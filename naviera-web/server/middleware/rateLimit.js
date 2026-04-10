/**
 * Rate limiter simples em memoria.
 * Limita requisicoes por IP em uma janela de tempo.
 */
export function rateLimit({ windowMs = 60000, max = 10, message = 'Muitas tentativas. Tente novamente em breve.' } = {}) {
  const hits = new Map()

  // Limpa entradas expiradas a cada minuto
  setInterval(() => {
    const now = Date.now()
    for (const [key, entry] of hits) {
      if (now - entry.start > windowMs) hits.delete(key)
    }
  }, windowMs)

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
