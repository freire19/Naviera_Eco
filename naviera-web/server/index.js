import 'dotenv/config'
import express from 'express'
import cors from 'cors'
import log from './logger.js'
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

const app = express()
const PORT = process.env.SERVER_PORT || 3001

app.use(cors())
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

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

app.listen(PORT, () => {
  log.info('Server', `Naviera Web BFF running on http://localhost:${PORT}`)
})
