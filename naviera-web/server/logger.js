const LEVELS = { error: 0, warn: 1, info: 2, debug: 3 }
const LEVEL = LEVELS[process.env.LOG_LEVEL || 'info'] ?? 2

function ts() {
  return new Date().toISOString().replace('T', ' ').slice(0, 23)
}

function log(level, tag, msg, extra) {
  if (LEVELS[level] > LEVEL) return
  const prefix = `${ts()} ${level.toUpperCase().padEnd(5)} [${tag}]`
  if (extra) {
    console[level === 'error' ? 'error' : 'log'](`${prefix} ${msg}`, extra)
  } else {
    console[level === 'error' ? 'error' : 'log'](`${prefix} ${msg}`)
  }
}

export default {
  info: (tag, msg, extra) => log('info', tag, msg, extra),
  warn: (tag, msg, extra) => log('warn', tag, msg, extra),
  error: (tag, msg, extra) => log('error', tag, msg, extra),
  debug: (tag, msg, extra) => log('debug', tag, msg, extra)
}
