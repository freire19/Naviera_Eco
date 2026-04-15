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
1. Identifique o que e o item na foto com o MAXIMO de detalhe util para transporte.
2. Inclua no nome: tipo + tamanho/porte estimado + marca (se visivel).
   Exemplos de bons nomes:
   - "Polia mecanica media 6-8 polegadas"
   - "Caixa de cerveja Skol 24 latas"
   - "Saco de cimento 50kg"
   - "Motor eletrico pequeno ~5kg"
   - "Geladeira Consul grande"
   - "Caixa papelao media ~40x30x30cm"
3. Para pecas mecanicas/industriais: estime o diametro ou porte (pequena/media/grande) em polegadas ou cm.
4. Estime a QUANTIDADE visivel (ex: 1 peca, 3 sacos, 2 caixas).
5. Se houver texto visivel (rotulo, marca, fabricante), inclua no nome do item ou remetente.
6. Se houver mais de um item diferente na foto, liste cada um separadamente.
7. O valor_unitario e o PRECO DE FRETE/TRANSPORTE por volume, NAO o preco do produto.
${tabelaPrecos}
Responda APENAS com JSON valido neste formato (sem markdown, sem \`\`\`):
{
  "remetente": "nome visivel na embalagem ou vazio",
  "destinatario": "",
  "itens": [
    {"nome_item": "Nome Detalhado Do Item Com Tamanho", "quantidade": 1, "valor_unitario": 0, "valor_total": 0}
  ],
  "total_volumes": 1,
  "total_a_pagar": 0,
  "observacoes": "detalhes relevantes da foto (material, estado, cor)"
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
      nome_item: i.nome_item || i.descricao || '',
      quantidade: i.quantidade || 1,
      preco_unitario: i.valor_unitario || i.preco_unitario || 0,
      subtotal: i.valor_total || (i.quantidade || 1) * (i.valor_unitario || i.preco_unitario || 0),
      confianca: 85,
      item_novo: true
    })),
    total_volumes: parsed.total_volumes || (parsed.itens || []).reduce((s, i) => s + (i.quantidade || 1), 0),
    total_a_pagar: parsed.total_a_pagar || 0,
    observacoes: parsed.observacoes || 'Identificado por Gemini Vision'
  }
}
