// #DS5-220: cache em memoria com cap maximo — sem isso atacante pode crescer cache
//   indefinidamente enviando chaves arbitrarias (slugs, ids), exaurindo RAM do BFF.
//   Insertion-order eviction (FIFO) — suficiente quando o objetivo e cap de RAM,
//   nao maximizar hit-rate.
export function createBoundedMap(maxSize = 1000) {
  const map = new Map()
  return {
    get: (k) => map.get(k),
    delete: (k) => map.delete(k),
    has: (k) => map.has(k),
    size: () => map.size,
    set: (k, v) => {
      if (!map.has(k) && map.size >= maxSize) {
        const oldest = map.keys().next().value
        if (oldest != null) map.delete(oldest)
      }
      map.set(k, v)
    },
    clear: () => map.clear(),
    raw: () => map
  }
}
