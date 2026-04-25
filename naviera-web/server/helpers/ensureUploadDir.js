import { existsSync, mkdirSync } from 'fs'

// #DP073/#DP074: cache de dirs ja criados — evita existsSync+mkdirSync sincronos a cada
//   upload. Multer destination handler chama em todo request; sem cache, syscalls bloqueavam
//   o event loop sob upload concorrente.
const cache = new Set()

export function ensureUploadDirCached(dir, cb) {
  if (cache.has(dir)) return cb(null, dir)
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
  cache.add(dir)
  cb(null, dir)
}
