import { createWriteStream, mkdirSync, existsSync, readdirSync, unlinkSync, statSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const LEVELS = { error: 0, warn: 1, info: 2, debug: 3 }
const LEVEL = LEVELS[process.env.LOG_LEVEL || 'info'] ?? 2

// Log file config
const __dirname = dirname(fileURLToPath(import.meta.url))
const LOG_DIR = process.env.LOG_DIR || join(__dirname, '..', 'logs')
const LOG_MAX_DAYS = 30
const LOG_ENABLED = process.env.LOG_TO_FILE !== 'false'

let fileStream = null

function initFileLog() {
  if (!LOG_ENABLED) return
  try {
    if (!existsSync(LOG_DIR)) mkdirSync(LOG_DIR, { recursive: true })
    const filename = `bff-${new Date().toISOString().slice(0, 10)}.log`
    fileStream = createWriteStream(join(LOG_DIR, filename), { flags: 'a' })
    cleanOldLogs()
  } catch (e) {
    console.error(`[logger] Falha ao abrir arquivo de log: ${e.message}`)
  }
}

function cleanOldLogs() {
  try {
    const cutoff = Date.now() - LOG_MAX_DAYS * 86400000
    for (const file of readdirSync(LOG_DIR)) {
      if (!file.startsWith('bff-') || !file.endsWith('.log')) continue
      const filepath = join(LOG_DIR, file)
      if (statSync(filepath).mtimeMs < cutoff) unlinkSync(filepath)
    }
  } catch (_) { /* best effort */ }
}

function ts() {
  return new Date().toISOString().replace('T', ' ').slice(0, 23)
}

// #DS5-231: log injection via newline — escapar CR/LF na msg do caller (input usuario).
//   tag e literal de codigo, JSON.stringify ja escapa \n dentro de strings.
function safe(s) {
  if (s == null) return ''
  return String(s).replace(/[\r\n]+/g, ' \\n ')
}

function log(level, tag, msg, extra) {
  if (LEVELS[level] > LEVEL) return
  const prefix = `${ts()} ${level.toUpperCase().padEnd(5)} [${tag}]`
  const line = extra
    ? `${prefix} ${safe(msg)} ${JSON.stringify(extra)}`
    : `${prefix} ${safe(msg)}`

  console[level === 'error' ? 'error' : 'log'](line)

  if (fileStream && !fileStream.destroyed) {
    fileStream.write(line + '\n')
  }
}

initFileLog()

export default {
  info: (tag, msg, extra) => log('info', tag, msg, extra),
  warn: (tag, msg, extra) => log('warn', tag, msg, extra),
  error: (tag, msg, extra) => log('error', tag, msg, extra),
  debug: (tag, msg, extra) => log('debug', tag, msg, extra)
}
