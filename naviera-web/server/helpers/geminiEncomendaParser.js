import { fetchWithRetry } from './fetchWithRetry.js'
import { repairTruncatedJSON } from './geminiParser.js'

const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent'

/**
 * Chama Gemini com prompt e retorna JSON parseado.
 */
async function callGemini(prompt) {
  const apiKey = process.env.GEMINI_API_KEY
  if (!apiKey) throw new Error('GEMINI_API_KEY nao configurada no .env')

  const body = {
    contents: [{ parts: [{ text: prompt }] }],
    generationConfig: { temperature: 0.1, maxOutputTokens: 16384 }
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

  try {
    return JSON.parse(jsonMatch[0])
  } catch {
    const repaired = repairTruncatedJSON(jsonMatch[0])
    if (!repaired) throw new Error('JSON invalido do Gemini: ' + jsonMatch[0].substring(0, 200))
    return repaired
  }
}

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
function buildTabelaPrecos(itensPadrao) {
  return itensPadrao.length > 0
    ? `\nTabela de precos de encomenda da empresa (usar estes precos quando houver match):\n${itensPadrao.map(i => `- ${i.nome_item}: R$${i.preco_padrao}`).join('\n')}\n`
    : '\nNao ha tabela de precos cadastrada. Deixe preco_unitario como 0.\n'
}

function normalizeItem(i) {
  return {
    nome_item: i.nome_item || i.descricao || '',
    quantidade: i.quantidade || 1,
    preco_unitario: i.preco_unitario || 0,
    subtotal: i.subtotal || (i.quantidade || 1) * (i.preco_unitario || 0),
    confianca: 80,
    item_novo: true
  }
}

/**
 * Parse protocolo manuscrito como UMA encomenda com varios itens.
 */
export async function geminiParseEncomenda(ocrText, itensPadrao = []) {
  const prompt = `Voce e um assistente de logistica fluvial na Amazonia.
Analise este texto extraido por OCR de um PROTOCOLO DE ENCOMENDAS manuscrito.

REGRAS:
1. Cada linha numerada e um item da encomenda.
2. O nome_item deve ser descritivo com quantidade (ex: "2 Cx Margarina").
3. Corrija erros de OCR.
4. O preco_unitario e o PRECO DE FRETE, NAO o preco do produto.
${buildTabelaPrecos(itensPadrao)}
Texto OCR:
"""
${ocrText}
"""

Responda APENAS com JSON valido (sem markdown):
{
  "remetente": "", "destinatario": "", "rota": "",
  "itens": [{"nome_item": "Descricao", "quantidade": 1, "preco_unitario": 0, "subtotal": 0}],
  "valor_total": 0, "observacoes": ""
}`

  const parsed = await callGemini(prompt)
  return {
    remetente: parsed.remetente || '',
    destinatario: parsed.destinatario || '',
    rota: parsed.rota || '',
    itens: (parsed.itens || []).map(normalizeItem),
    valor_total: parsed.valor_total || 0,
    observacoes: parsed.observacoes || 'Extraido de protocolo manuscrito'
  }
}

/**
 * Parse protocolo manuscrito como LOTE — retorna N encomendas separadas.
 * Cada entrada numerada do protocolo e uma encomenda independente
 * com seu proprio remetente/destinatario.
 */
export async function geminiParseLote(ocrText, itensPadrao = []) {
  const prompt = `Voce e um assistente de logistica fluvial na Amazonia.
Analise este texto extraido por OCR de uma PAGINA DE PROTOCOLO manuscrito (caderno de bordo de barco).

CONTEXTO: Cada entrada numerada no protocolo e uma ENCOMENDA SEPARADA de um remetente para um destinatario diferente. Uma pagina tipica tem 3 a 8 encomendas.

REGRAS CRITICAS:
1. Cada entrada numerada (001, 002, etc.) e uma ENCOMENDA INDEPENDENTE — NAO agrupe tudo em uma so.
2. Cada encomenda tem seu PROPRIO remetente e destinatario.
3. Dentro de uma encomenda pode haver 1 ou mais itens (ex: "2 cx margarina + 1 saco arroz" = 2 itens na mesma encomenda).
4. Identifique o remetente (quem envia) e destinatario (quem recebe) de CADA encomenda.
5. Se o remetente/destinatario nao esta explicito, use o que vier antes/depois dos itens naquela entrada.
6. Corrija erros de OCR nos nomes.
7. O preco_unitario e o PRECO DE FRETE (transporte), NAO o preco do produto.
8. Se houver telefones/codigos, coloque nas observacoes da encomenda correspondente.
${buildTabelaPrecos(itensPadrao)}
Texto OCR do protocolo:
"""
${ocrText}
"""

Responda APENAS com JSON valido (sem markdown):
{
  "rota": "rota geral se identificada",
  "encomendas": [
    {
      "ordem": 1,
      "remetente": "quem envia esta encomenda",
      "destinatario": "quem recebe esta encomenda",
      "itens": [{"nome_item": "Descricao com quantidade", "quantidade": 1, "preco_unitario": 0, "subtotal": 0}],
      "valor_total": 0,
      "observacoes": ""
    }
  ]
}`

  const parsed = await callGemini(prompt)

  // Se Gemini retornou formato antigo (sem array encomendas), converter
  if (!parsed.encomendas && parsed.itens) {
    return {
      rota: parsed.rota || '',
      encomendas: [{
        ordem: 1,
        remetente: parsed.remetente || '',
        destinatario: parsed.destinatario || '',
        itens: (parsed.itens || []).map(normalizeItem),
        valor_total: parsed.valor_total || 0,
        observacoes: parsed.observacoes || ''
      }]
    }
  }

  return {
    rota: parsed.rota || '',
    encomendas: (parsed.encomendas || []).map((enc, idx) => ({
      ordem: enc.ordem || idx + 1,
      remetente: enc.remetente || '',
      destinatario: enc.destinatario || '',
      itens: (enc.itens || []).map(normalizeItem),
      valor_total: enc.valor_total || (enc.itens || []).reduce((s, i) => s + ((i.quantidade || 1) * (i.preco_unitario || 0)), 0),
      observacoes: enc.observacoes || ''
    }))
  }
}
