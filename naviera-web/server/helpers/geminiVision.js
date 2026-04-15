import { readFile } from 'fs/promises'
import { repairTruncatedJSON } from './geminiParser.js'
import { fetchWithRetry } from './fetchWithRetry.js'

const GEMINI_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent'

/**
 * Usa Gemini Vision para analisar uma FOTO de item fisico e extrair dados de encomenda.
 * Diferente do geminiParser (que recebe texto OCR), este recebe a IMAGEM diretamente.
 *
 * @param {string} imagePath - Caminho absoluto da imagem no filesystem
 * @param {Array} itensPadrao - Itens da tabela de precos de encomenda da empresa
 * @returns {{ remetente, destinatario, itens[], valor_total, observacoes }}
 */
export async function geminiVisionAnalyze(imagePath, itensPadrao = []) {
  const apiKey = process.env.GEMINI_API_KEY
  if (!apiKey) throw new Error('GEMINI_API_KEY nao configurada no .env')

  // Ler imagem como base64
  const imageBuffer = await readFile(imagePath)
  const base64 = imageBuffer.toString('base64')

  // Detectar mime type pela extensao
  const ext = imagePath.toLowerCase().split('.').pop()
  const mimeMap = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', webp: 'image/webp' }
  const mimeType = mimeMap[ext] || 'image/jpeg'

  const tabelaPrecos = itensPadrao.length > 0
    ? `\nTabela de precos de encomenda da empresa (usar estes precos quando houver match):\n${itensPadrao.map(i => `- ${i.nome_item}: R$${i.preco_padrao}`).join('\n')}\n`
    : '\nNao ha tabela de precos cadastrada. Deixe valor_unitario como 0.\n'

  const prompt = `Voce e um assistente de logistica fluvial na Amazonia.
Analise esta FOTO de um item/mercadoria/pacote que sera enviado como ENCOMENDA em um barco.

REGRAS:
1. Identifique o que e o item na foto (ex: "Caixa de banana", "Polia mecanica", "Saco de cimento", "Caixa de cerveja").
2. Estime a QUANTIDADE visivel (ex: 1 caixa, 3 sacos, 1 peca).
3. Se houver texto visivel na foto (rotulo, marca, nome de empresa), extraia como remetente ou observacao.
4. Se houver mais de um item diferente na foto, liste cada um separadamente.
5. Use nomes simples e descritivos em portugues.
6. O valor_unitario e o PRECO DE FRETE/TRANSPORTE por volume, NAO o preco do produto.
${tabelaPrecos}
Responda APENAS com JSON valido neste formato (sem markdown, sem \`\`\`):
{
  "remetente": "nome visivel na embalagem ou vazio",
  "destinatario": "",
  "itens": [
    {"descricao": "Nome Do Item", "quantidade": 1, "valor_unitario": 0, "valor_total": 0}
  ],
  "total_volumes": 1,
  "total_a_pagar": 0,
  "observacoes": "detalhes relevantes da foto"
}`

  const body = {
    contents: [{
      parts: [
        { text: prompt },
        { inlineData: { mimeType, data: base64 } }
      ]
    }],
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
    throw new Error(`Gemini Vision erro ${res.status}: ${err.substring(0, 200)}`)
  }

  const data = await res.json()

  // Extrair JSON da resposta
  const parts = data.candidates?.[0]?.content?.parts || []
  let text = ''
  for (const part of parts) {
    if (part.text && part.text.includes('{')) { text = part.text; break }
  }
  if (!text) text = parts.find(p => p.text)?.text || ''

  const jsonMatch = text.match(/\{[\s\S]*\}/)
  if (!jsonMatch) throw new Error('Gemini Vision nao retornou JSON valido')

  let parsed
  try {
    parsed = JSON.parse(jsonMatch[0])
  } catch {
    parsed = repairTruncatedJSON(jsonMatch[0])
    if (!parsed) throw new Error('JSON truncado do Gemini Vision')
  }

  return {
    remetente: parsed.remetente || '',
    destinatario: parsed.destinatario || '',
    itens: (parsed.itens || []).map(i => ({
      descricao: i.descricao || i.nome_item || '',
      quantidade: i.quantidade || 1,
      valor_unitario: i.valor_unitario || 0,
      valor_total: i.valor_total || (i.quantidade || 1) * (i.valor_unitario || 0)
    })),
    total_volumes: parsed.total_volumes || (parsed.itens || []).reduce((s, i) => s + (i.quantidade || 1), 0),
    total_a_pagar: parsed.total_a_pagar || 0,
    observacoes: parsed.observacoes || 'Identificado por Gemini Vision'
  }
}
