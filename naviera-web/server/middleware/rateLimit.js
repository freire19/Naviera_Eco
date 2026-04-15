/**
 * Rate limiter simples em memoria.
 * Limita requisicoes por IP em uma janela de tempo.
 * DR247: NOTA — em PM2 cluster mode cada worker tem seu proprio Map.
 * Para rate limiting real em cluster, usar Redis. Em fork mode (atual), funciona OK.
 */
export function rateLimit({ windowMs = 60000, max = 10, message = 'Muitas tentativas. Tente novamente em breve.' } = {}) {
  const hits = new Map()

  // DR247: guardar ref do interval para cleanup em graceful shutdown
  const cleanupInterval = setInterval(() => {
    const now = Date.now()
    for (const [key, entry] of hits) {
      if (now - entry.start > windowMs) hits.delete(key)
    }
  }, windowMs)
  cleanupInterval.unref() // nao impede o processo de encerrar

  return (req, res, next) => {
    const key = req.ip || req.connection.remoteAddress
    const now = Date.now()
    let entry = hits.get(key)

    if (!entry || now - entry.start > windowMs) {
      entry = { count: 0, start: now }
      hits.set(key, entry)
    }

    entry.count++

    if (entry.count > max) {
      return res.status(429).json({ error: message })
    }

    next()
  }
}
