process.on('unhandledRejection', (reason, promise) => {
  console.error('[FATAL] Unhandled Rejection:', reason);
});
process.on('uncaughtException', (err) => {
  console.error('[FATAL] Uncaught Exception:', err);
  process.exit(1);
});

import 'dotenv/config'
import express from 'express'
import cors from 'cors'
import log from './logger.js'
import { rateLimit } from './middleware/rateLimit.js'
import { tenantMiddleware, tenantInfoRoute } from './middleware/tenant.js'
import authRoutes from './routes/auth.js'
import viagemRoutes from './routes/viagens.js'
import rotaRoutes from './routes/rotas.js'
import embarcacaoRoutes from './routes/embarcacoes.js'
import passagemRoutes from './routes/passagens.js'
import encomendaRoutes from './routes/encomendas.js'
import freteRoutes from './routes/fretes.js'
import cadastroRoutes from './routes/cadastros.js'
import financeiroRoutes from './routes/financeiro.js'
import dashboardRoutes from './routes/dashboard.js'
import adminRoutes from './routes/admin.js'
import agendaRoutes from './routes/agenda.js'
import estornoRoutes from './routes/estornos.js'
import ocrRoutes from './routes/ocr.js'
import documentosRoutes from './routes/documentos.js'
import errorHandler from './middleware/errorHandler.js'

const app = express()
// DS4-009 fix: confiar no X-Forwarded-For do proxy local (Nginx)
// Sem isso, req.ip = 127.0.0.1 para todos, rate limiter inoperante
app.set('trust proxy', 'loopback')
const PORT = process.env.SERVER_PORT || 3002

const corsOrigins = process.env.CORS_ORIGINS
  ? process.env.CORS_ORIGINS.split(',').map(o => o.trim())
  : ['http://localhost:5174', 'http://localhost:5173']

app.use(cors({
  origin: corsOrigins,
  credentials: true
}))
app.use(rateLimit({ windowMs: 60000, max: 200, message: 'Muitas requisicoes. Tente novamente em breve.' }))
app.use(express.json())

// Request logging
app.use((req, res, next) => {
  const start = Date.now()
  res.on('finish', () => {
    const ms = Date.now() - start
    const level = res.statusCode >= 500 ? 'error' : res.statusCode >= 400 ? 'warn' : 'info'
    log[level]('HTTP', `${req.method} ${req.originalUrl} ${res.statusCode} ${ms}ms`)
  })
  next()
})

// Tenant resolver (subdominio → empresa_id)
app.use(tenantMiddleware)

// Tenant info (publico, sem auth)
app.get('/api/tenant/info', tenantInfoRoute)

// Routes
app.use('/api/auth', authRoutes)
app.use('/api/viagens', viagemRoutes)
app.use('/api/rotas', rotaRoutes)
app.use('/api/embarcacoes', embarcacaoRoutes)
app.use('/api/passagens', passagemRoutes)
app.use('/api/encomendas', encomendaRoutes)
app.use('/api/fretes', freteRoutes)
app.use('/api/cadastros', cadastroRoutes)
app.use('/api/financeiro', financeiroRoutes)
app.use('/api/dashboard', dashboardRoutes)
app.use('/api/admin', adminRoutes)
app.use('/api/agenda', agendaRoutes)
app.use('/api/estornos', estornoRoutes)
app.use('/api/ocr', ocrRoutes)
app.use('/api/documentos', documentosRoutes)

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// Centralized error handler — must be LAST middleware
app.use(errorHandler)

const server = app.listen(PORT, () => {
  log.info('Server', `Naviera Web BFF running on http://localhost:${PORT}`)
})
server.timeout = 120_000

function shutdown(signal) {
  log.info('Server', `${signal} received — shutting down`)
  server.close(() => {
    log.info('Server', 'Connections closed — exiting')
    process.exit(0)
  })
  setTimeout(() => process.exit(1), 10000)
}

process.on('SIGTERM', () => shutdown('SIGTERM'))
process.on('SIGINT', () => shutdown('SIGINT'))
