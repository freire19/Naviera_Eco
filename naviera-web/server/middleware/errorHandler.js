/**
 * Centralized error handler middleware for BFF.
 * Must be registered AFTER all routes in Express.
 */
export default function errorHandler(err, req, res, _next) {
  const statusCode = err.statusCode || err.status || 500
  const message = statusCode === 500
    ? 'Erro interno do servidor'
    : (err.message || 'Erro desconhecido')

  console.error(`[ErrorHandler] ${req.method} ${req.originalUrl} — ${statusCode}:`, err.message || err)

  if (!res.headersSent) {
    res.status(statusCode).json({
      error: true,
      message,
      code: statusCode,
      timestamp: new Date().toISOString()
    })
  }
}
