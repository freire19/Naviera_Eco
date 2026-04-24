import { readFile } from 'fs/promises'
import { fetchWithRetry } from './fetchWithRetry.js'

const VISION_URL = 'https://vision.googleapis.com/v1/images:annotate'

/**
 * Chama Google Cloud Vision API para extrair texto de uma imagem.
 * Usa DOCUMENT_TEXT_DETECTION (melhor para texto manuscrito e documentos).
 *
 * @param {string} imagePath - Caminho absoluto da imagem no filesystem
 * @returns {{ text: string, confidence: number, fullResponse: object }}
 */
export async function callVisionOCR(imagePath) {
  const apiKey = process.env.GOOGLE_CLOUD_VISION_API_KEY
  if (!apiKey) {
    throw new Error('GOOGLE_CLOUD_VISION_API_KEY nao configurada')
  }

  const imageBuffer = await readFile(imagePath)
  const base64Image = imageBuffer.toString('base64')

  const body = {
    requests: [{
      image: { content: base64Image },
      features: [{ type: 'DOCUMENT_TEXT_DETECTION', maxResults: 1 }]
    }]
  }

  const res = await fetchWithRetry(`${VISION_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(30000)
  })

  if (!res.ok) {
    const errBody = await res.text()
    throw new Error(`Vision API erro ${res.status}: ${errBody}`)
  }

  const data = await res.json()
  const response = data.responses?.[0]

  if (response?.error) {
    throw new Error(`Vision API: ${response.error.message}`)
  }

  const fullText = response?.fullTextAnnotation?.text || ''
  const pages = response?.fullTextAnnotation?.pages || []

  // Calcular confianca media a partir dos blocos
  let totalConfidence = 0
  let blockCount = 0
  for (const page of pages) {
    for (const block of page.blocks || []) {
      if (block.confidence != null) {
        totalConfidence += block.confidence
        blockCount++
      }
    }
  }
  const avgConfidence = blockCount > 0 ? Math.round((totalConfidence / blockCount) * 100) : 0

  return {
    text: fullText,
    confidence: avgConfidence,
    fullResponse: response
  }
}

/**
 * #240: chama Vision OCR em multiplas imagens com concorrencia limitada.
 * Antes multi-pagina era loop sequencial — agora roda ate `concurrency` em paralelo.
 *
 * @param {string[]} imagePaths - Lista de caminhos absolutos
 * @param {number} concurrency - Limite simultaneo (default 3 — evita rate-limit Google)
 * @returns {Array<{text, confidence, fullResponse, error?}>}
 */
export async function callVisionOCRBatch(imagePaths, concurrency = 3) {
  if (!Array.isArray(imagePaths) || imagePaths.length === 0) return []
  const results = new Array(imagePaths.length)
  let cursor = 0
  const workers = Array.from({ length: Math.min(concurrency, imagePaths.length) }, async () => {
    while (true) {
      const idx = cursor++
      if (idx >= imagePaths.length) break
      try {
        results[idx] = await callVisionOCR(imagePaths[idx])
      } catch (e) {
        results[idx] = { text: '', confidence: 0, fullResponse: null, error: e.message }
      }
    }
  })
  await Promise.all(workers)
  return results
}
