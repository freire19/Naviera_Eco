/**
 * Wrapper fetch com retry e backoff exponencial.
 * Retenta em erros de rede, timeout e 5xx. NAO retenta em 4xx (erro do cliente).
 *
 * @param {string} url
 * @param {RequestInit} options
 * @param {{ retries?: number, baseDelay?: number }} config
 * @returns {Promise<Response>}
 */
export async function fetchWithRetry(url, options = {}, { retries = 2, baseDelay = 1000 } = {}) {
  let lastError
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const res = await fetch(url, options)
      // Nao retentar em 4xx (erro do cliente — request invalido)
      if (res.ok || (res.status >= 400 && res.status < 500)) {
        return res
      }
      // 5xx — servidor/API com problema, retentar
      lastError = new Error(`HTTP ${res.status}`)
      lastError.status = res.status
    } catch (err) {
      // Timeout, rede fora, DNS fail — retentar
      lastError = err
    }
    if (attempt < retries) {
      const delay = baseDelay * Math.pow(2, attempt) // 1s, 2s
      await new Promise(r => setTimeout(r, delay))
    }
  }
  throw lastError
}
