/**
 * Rate limiter simples em memoria.
 * Limita requisicoes por IP em uma janela de tempo.
 * DR247: NOTA — em PM2 cluster mode cada worker tem seu proprio Map.
 * Para rate limiting real em cluster, usar Redis. Em fork mode (atual), funciona OK.
 */
export function rateLimit({ windowMs = 60000, max = 10, message = 'Muitas tentativas. Tente novamente em breve.', keyFn, maxKeys = 10000 } = {}) {
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
    const key = keyFn ? keyFn(req) : (req.ip || req.connection.remoteAddress)
    const now = Date.now()
    let entry = hits.get(key)

    if (!entry || now - entry.start > windowMs) {
      // #027: cap em maxKeys evita Map sem bound em keyFn de alta cardinalidade.
      //   Sweep limitado a 100 entradas por request — caso contrario varriamos 10k a cada
      //   POST sob saturacao. FIFO drop garante progresso mesmo se sweep nao liberar nada.
      if (hits.size >= maxKeys) {
        let scanned = 0
        for (const [k, e] of hits) {
          if (++scanned > 100) break
          if (now - e.start > windowMs) hits.delete(k)
          if (hits.size < maxKeys) break
        }
        if (hits.size >= maxKeys) {
          const firstKey = hits.keys().next().value
          if (firstKey !== undefined) hits.delete(firstKey)
        }
      }
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
