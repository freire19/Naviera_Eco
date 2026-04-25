/**
 * Wrapper fetch com retry e backoff exponencial.
 * Retenta em erros de rede, timeout, 5xx e 429 (com Retry-After). NAO retenta em outros 4xx.
 *
 * @param {string} url
 * @param {RequestInit} options
 * @param {{ retries?: number, baseDelay?: number, maxDelay?: number }} config
 * @returns {Promise<Response>}
 */
export async function fetchWithRetry(url, options = {}, { retries = 2, baseDelay = 1000, maxDelay = 30000 } = {}) {
  let lastError
  for (let attempt = 0; attempt <= retries; attempt++) {
    let res
    try {
      res = await fetch(url, options)
      // 2xx/3xx — sucesso
      if (res.ok) return res
      // 4xx exceto 429 — erro do cliente, nao retentar
      if (res.status >= 400 && res.status < 500 && res.status !== 429) {
        return res
      }
      // 429 ou 5xx — retentavel
      lastError = new Error(`HTTP ${res.status}`)
      lastError.status = res.status
      // #307: honrar Retry-After (segundos absolutos ou data HTTP) quando 429.
      if (res.status === 429 && attempt < retries) {
        const retryAfter = res.headers.get('Retry-After')
        const wait = parseRetryAfter(retryAfter)
        if (wait != null) {
          await sleep(Math.min(wait, maxDelay))
          continue
        }
      }
    } catch (err) {
      // Timeout, rede fora, DNS fail — retentar
      lastError = err
    }
    if (attempt < retries) {
      // Backoff exponencial com jitter (evita thundering herd em incidente Asaas/upstream).
      const expo = baseDelay * Math.pow(2, attempt)
      const jitter = Math.random() * baseDelay
      await sleep(Math.min(expo + jitter, maxDelay))
    }
  }
  throw lastError
}

function parseRetryAfter(header) {
  if (!header) return null
  const seconds = Number(header)
  if (Number.isFinite(seconds) && seconds >= 0) return seconds * 1000
  const date = Date.parse(header)
  if (!Number.isNaN(date)) return Math.max(0, date - Date.now())
  return null
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }
