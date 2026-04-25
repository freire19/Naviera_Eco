import log from '../logger.js'

/**
 * Centralized error handler middleware for BFF.
 * Must be registered AFTER all routes in Express.
 */
export default function errorHandler(err, req, res, _next) {
  const statusCode = err.statusCode || err.status || 500
  const isServerError = statusCode === 500
  // #DS5-212: nunca devolver `err.message` em 5xx (pode vazar caminho/SQL/IP do PG).
  //   Em 4xx so devolve se vier explicito do nosso codigo (whitelist via err.expose).
  const safeMessage = isServerError
    ? 'Erro interno do servidor'
    : (err.expose && err.message ? err.message : 'Requisicao invalida')

  // #DS5-212: log estruturado com codigo do erro (e stack so em DEBUG); err.message fica fora do log padrao.
  log.error('errorHandler', `${req.method} ${req.originalUrl} — ${statusCode}`, {
    code: err.code,
    name: err.name,
  })
  if (process.env.LOG_LEVEL === 'debug' && err.stack) {
    log.debug('errorHandler', err.stack)
  }

  if (!res.headersSent) {
    res.status(statusCode).json({
      error: true,
      message: safeMessage,
      code: statusCode,
    })
  }
}
