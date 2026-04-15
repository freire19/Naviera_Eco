import { fetchWithRetry } from './fetchWithRetry.js'
import { repairTruncatedJSON } from './geminiParser.js'

const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent'

/**
 * Usa Gemini AI para extrair encomendas de texto OCR de protocolo manuscrito.
 * Protocolos tipicos de barco listam encomendas numeradas com:
 *   - Numero de ordem (001, 002...)
 *   - Descricao dos itens (ex: "2 cx margarina", "1 motor")
 *   - Remetente e destinatario por linha
 *   - Rota ou destino
 *
 * @param {string} ocrText - Texto bruto do Google Cloud Vision
 * @param {Array} itensPadrao - Itens da tabela de precos de encomenda
 * @returns {{ remetente, destinatario, rota, itens[], valor_total, observacoes }}
 */
export async function geminiParseEncomenda(ocrText, itensPadrao = []) {
  const apiKey = process.env.GEMINI_API_KEY
  if (!apiKey) throw new Error('GEMINI_API_KEY nao configurada no .env')

  const tabelaPrecos = itensPadrao.length > 0
    ? `\nTabela de precos de encomenda da empresa (usar estes precos quando houver match):\n${itensPadrao.map(i => `- ${i.nome_item}: R$${i.preco_padrao}`).join('\n')}\n`
    : '\nNao ha tabela de precos cadastrada. Deixe preco_unitario como 0.\n'

  const prompt = `Voce e um assistente de logistica fluvial na Amazonia.
Analise este texto extraido por OCR de um PROTOCOLO DE ENCOMENDAS manuscrito (caderno de bordo).

O protocolo lista encomendas que serao transportadas no barco. Cada entrada pode ter:
- Numero de ordem (001, 002, etc.)
- Descricao do item/mercadoria com quantidade (ex: "2 cx margarina", "1 motor pequeno", "3 sacas farinha")
- Nome do remetente (quem envia)
- Nome do destinatario (quem recebe)
- Rota ou destino (ex: "Manaus - Jutai", "p/ Parintins")
- Valor do frete ou preco

REGRAS:
1. Cada LINHA ou ENTRADA numerada do protocolo e uma encomenda separada.
2. Extraia TODOS os itens — nao pule nenhum. Cada item vira um registro.
3. O nome_item deve ser descritivo: inclua quantidade + nome do produto (ex: "2 Cx Margarina", "1 Motor Pequeno", "10 Sacas Farinha 50kg").
4. Se o mesmo remetente/destinatario aparece em varias linhas, repita em cada item.
5. Se houver um remetente/destinatario GERAL no topo da pagina, use para todas as linhas que nao tem um proprio.
6. Corrija erros de OCR nos nomes (letras trocadas, palavras cortadas).
7. Se houver numeros de telefone, CNPJ, ou codigos, coloque nas observacoes.
8. O preco_unitario e o PRECO DE FRETE (transporte), NAO o preco do produto.
${tabelaPrecos}
Texto OCR do protocolo:
"""
${ocrText}
"""

Responda APENAS com JSON valido neste formato (sem markdown, sem \`\`\`):
{
  "remetente": "remetente geral ou do primeiro item",
  "destinatario": "destinatario geral ou do primeiro item",
  "rota": "rota identificada ou vazio",
  "itens": [
    {"nome_item": "Descricao completa do item com quantidade", "quantidade": 1, "preco_unitario": 0, "subtotal": 0, "remetente": "remetente especifico se diferente", "destinatario": "destinatario especifico se diferente", "ordem": 1}
  ],
  "valor_total": 0,
  "observacoes": "informacoes adicionais (telefones, codigos, etc)"
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

  const parts = data.candidates?.[0]?.content?.parts || []
  let text = ''
  for (const part of parts) {
    if (part.text && part.text.includes('{')) { text = part.text; break }
  }
  if (!text) text = parts.find(p => p.text)?.text || ''

  const jsonMatch = text.match(/\{[\s\S]*\}/)
  if (!jsonMatch) throw new Error('Gemini nao retornou JSON valido')

  let parsed
  try {
    parsed = JSON.parse(jsonMatch[0])
  } catch {
    parsed = repairTruncatedJSON(jsonMatch[0])
    if (!parsed) throw new Error('JSON invalido do Gemini: ' + jsonMatch[0].substring(0, 200))
  }

  return {
    remetente: parsed.remetente || '',
    destinatario: parsed.destinatario || '',
    rota: parsed.rota || '',
    itens: (parsed.itens || []).map(i => ({
      nome_item: i.nome_item || i.descricao || '',
      quantidade: i.quantidade || 1,
      preco_unitario: i.preco_unitario || 0,
      subtotal: i.subtotal || (i.quantidade || 1) * (i.preco_unitario || 0),
      remetente: i.remetente || '',
      destinatario: i.destinatario || '',
      ordem: i.ordem || 0,
      confianca: 80,
      item_novo: true
    })),
    valor_total: parsed.valor_total || 0,
    observacoes: parsed.observacoes || 'Extraido de protocolo manuscrito'
  }
}
