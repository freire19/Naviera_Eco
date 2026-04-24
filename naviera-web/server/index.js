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
import pool from './db.js'
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
import reciboRoutes from './routes/recibos.js'
import extratoClienteRoutes from './routes/extrato-cliente.js'
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
// #DS5-206: body size limit + strict JSON. Rotas que precisam mais (ex: /api/ocr/upload) usam multer proprio.
app.use(express.json({ limit: '200kb', strict: true }))
app.use(express.urlencoded({ limit: '200kb', extended: false }))

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
app.use('/api/recibos', reciboRoutes)
app.use('/api/extrato-cliente', extratoClienteRoutes)

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// Centralized error handler — must be LAST middleware
app.use(errorHandler)

// Migracoes idempotentes executadas no startup
async function runMigrations() {
  try {
    // 1. Adiciona coluna se nao existir
    await pool.query(`ALTER TABLE financeiro_saidas ADD COLUMN IF NOT EXISTS mes_referencia DATE`)

    // 2. Backfill: lancamentos de funcionarios sem mes_referencia
    //    usam data_admissao como raiz do primeiro ciclo (so toca NULLs = idempotente)
    await pool.query(`
      UPDATE financeiro_saidas s
      SET mes_referencia = f.data_admissao
      FROM funcionarios f
      WHERE s.funcionario_id IS NOT NULL
        AND s.mes_referencia IS NULL
        AND s.funcionario_id = f.id
        AND f.data_admissao IS NOT NULL
    `)

    // 3. Backfill eventos_rh: faltas/descontos que usaram data_evento como referencia
    //    (fallback antigo incorreto) — so toca registros nessa condicao = idempotente
    await pool.query(`
      UPDATE eventos_rh e
      SET data_referencia = f.data_admissao
      FROM funcionarios f
      WHERE e.tipo IN ('FALTA', 'DESCONTO_MANUAL')
        AND e.data_referencia = e.data_evento
        AND e.funcionario_id = f.id
        AND f.data_admissao IS NOT NULL
        AND f.data_admissao < e.data_evento
    `)

    log.info('Migrations', 'mes_referencia + backfill funcionarios ok')

    // tipo_operacao em log_estornos_* (ESTORNO | EXCLUSAO)
    await pool.query(`ALTER TABLE log_estornos_passagens ADD COLUMN IF NOT EXISTS tipo_operacao VARCHAR(20) DEFAULT 'ESTORNO'`)
    await pool.query(`ALTER TABLE log_estornos_encomendas ADD COLUMN IF NOT EXISTS tipo_operacao VARCHAR(20) DEFAULT 'ESTORNO'`)
    await pool.query(`ALTER TABLE log_estornos_fretes ADD COLUMN IF NOT EXISTS tipo_operacao VARCHAR(20) DEFAULT 'ESTORNO'`)
    log.info('Migrations', 'tipo_operacao em log_estornos_* ok')
  } catch (err) {
    log.warn('Migrations', `falha ao aplicar migration: ${err.message}`)
  }
}
runMigrations()

const server = app.listen(PORT, () => {
  log.info('Server', `Naviera Web BFF running on http://localhost:${PORT}`)
})
// #DS5-206: fecha conexoes lentas (slowloris) — headersTimeout e body (requestTimeout) ocioso
server.headersTimeout = 15_000
server.requestTimeout = 30_000
server.keepAliveTimeout = 65_000
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
