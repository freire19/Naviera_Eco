import { fetchWithRetry } from './fetchWithRetry.js'

const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent'

/**
 * Usa Google Gemini AI para extrair itens estruturados do texto OCR.
 * Mais inteligente que regex — entende contexto, corrige erros de OCR,
 * e separa corretamente nome, quantidade e preco.
 *
 * @param {string} ocrText - Texto bruto do Google Cloud Vision
 * @param {Array} itensPadrao - Itens da tabela de precos de frete da empresa
 * @returns {{ remetente, destinatario, rota, itens[], valor_total, observacoes }}
 */
export async function geminiParseOCR(ocrText, itensPadrao = []) {
  const apiKey = process.env.GEMINI_API_KEY
  if (!apiKey) throw new Error('GEMINI_API_KEY nao configurada no .env')

  const tabelaPrecos = itensPadrao.length > 0
    ? `\nTabela de precos de frete da empresa (usar estes precos quando houver match):\n${itensPadrao.map(i => `- ${i.nome_item}: R$${i.preco_unitario_padrao}`).join('\n')}\n`
    : '\nNao ha tabela de precos de frete cadastrada. Deixe preco_unitario como 0.\n'

  const prompt = `Voce e um assistente especializado em logistica fluvial na Amazonia.
Analise o texto extraido por OCR de uma nota fiscal, cupom ou caderno e extraia os PRODUTOS para lancamento de frete.

REGRAS IMPORTANTES:
1. Extraia APENAS produtos/mercadorias. Ignore cabecalhos, CNPJs, enderecos, rodapes, chaves de acesso.
2. O "preco_unitario" deve ser o PRECO DE FRETE (transporte), NAO o preco do produto na nota.
3. Se existir tabela de precos de frete, use o preco da tabela. Senao, deixe preco_unitario = 0.
4. Agrupe itens duplicados (some quantidades).
5. Corrija erros de OCR nos nomes (ex: "REQUEI JAO" → "Requeijao", "MACARRAD" → "Macarrao").
6. Para items em KG com peso fracionado (ex: 0,440 KG), considere quantidade = 1 volume.
7. Extraia o nome do estabelecimento/remetente se visivel.
${tabelaPrecos}
Texto OCR:
"""
${ocrText}
"""

Responda APENAS com JSON valido neste formato (sem markdown, sem \`\`\`):
{
  "remetente": "nome do estabelecimento ou remetente",
  "destinatario": "",
  "rota": "",
  "itens": [
    {"nome_item": "Nome Correto Do Produto", "quantidade": 1, "preco_unitario": 0, "subtotal": 0}
  ],
  "valor_total": 0,
  "observacoes": "qualquer observacao relevante"
}`

  const body = {
    contents: [{ parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.1,
      maxOutputTokens: 16384
    }
  }

  const res = await fetchWithRetry(`${GEMINI_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(60000)
  })

  if (!res.ok) {
    const err = await res.text()
    throw new Error(`Gemini API erro ${res.status}: ${err.substring(0, 200)}`)
  }

  const data = await res.json()

  // Gemini 3 Flash retorna multiplas parts (thoughtSignature + text)
  // Precisamos encontrar a part que contem o JSON de resposta
  const parts = data.candidates?.[0]?.content?.parts || []
  let text = ''
  for (const part of parts) {
    if (part.text && part.text.includes('{')) {
      text = part.text
      break
    }
  }
  if (!text) {
    // Fallback: pegar qualquer text
    text = parts.find(p => p.text)?.text || ''
  }

  // Extrair JSON da resposta (pode vir com markdown ```json ... ```)
  const jsonMatch = text.match(/\{[\s\S]*\}/)
  if (!jsonMatch) {
    throw new Error('Gemini nao retornou JSON valido. Resposta: ' + text.substring(0, 200))
  }

  let parsed
  try {
    parsed = JSON.parse(jsonMatch[0])
  } catch (e) {
    // Tentar reparar JSON truncado (Gemini cortou no meio)
    parsed = repairTruncatedJSON(jsonMatch[0])
    if (!parsed) throw new Error('JSON invalido do Gemini: ' + jsonMatch[0].substring(0, 200))
  }

  // Garantir estrutura correta
  return {
    remetente: parsed.remetente || '',
    destinatario: parsed.destinatario || '',
    rota: parsed.rota || '',
    itens: (parsed.itens || []).map(i => ({
      nome_item: i.nome_item || '',
      quantidade: i.quantidade || 1,
      preco_unitario: i.preco_unitario || 0,
      subtotal: i.subtotal || (i.quantidade || 1) * (i.preco_unitario || 0),
      confianca: 90,
      item_novo: true
    })),
    valor_total: parsed.valor_total || 0,
    observacoes: parsed.observacoes || 'Revisado por IA (Gemini)'
  }
}

/**
 * Tenta reparar JSON truncado pelo Gemini (cortou no maxOutputTokens).
 * Estrategia: fechar arrays/objetos abertos e tentar parse.
 */
export function repairTruncatedJSON(text) {
  // Remover item incompleto no final (cortado no meio de um objeto)
  let cleaned = text.replace(/,\s*\{[^}]*$/, '')  // remove ultimo objeto incompleto de array
  cleaned = cleaned.replace(/,\s*"[^"]*$/, '')     // remove ultima chave incompleta

  // Contar brackets abertos e fechar
  const opens = { '{': 0, '[': 0 }
  const closes = { '}': '{', ']': '[' }
  for (const ch of cleaned) {
    if (ch in opens) opens[ch]++
    if (ch in closes) opens[closes[ch]]--
  }

  // Fechar na ordem inversa
  let suffix = ''
  for (let i = 0; i < opens['[']; i++) suffix += ']'
  for (let i = 0; i < opens['{']; i++) suffix += '}'

  try {
    return JSON.parse(cleaned + suffix)
  } catch {
    return null
  }
}
