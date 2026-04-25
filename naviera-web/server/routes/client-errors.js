import { Router } from 'express'
import { rateLimit } from '../middleware/rateLimit.js'
import log from '../logger.js'
import pool from '../db.js'

// #DR285: endpoint publico (sem auth — ErrorBoundary roda mesmo deslogado).
//   Rate limit agressivo evita spam; payload truncado evita storage abuse.
//   Tabela client_errors e criada via migration on-demand (ALTER IF NOT EXISTS) para
//   nao depender de mais um script SQL externo.

const router = Router()

const errorLimiter = rateLimit({
  windowMs: 60_000,
  max: 30,
  message: 'Muitos erros reportados, aguarde.',
  keyFn: (req) => `client-error:${req.ip}`
})

let migrated = false
async function ensureTable() {
  if (migrated) return
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS client_errors (
        id BIGSERIAL PRIMARY KEY,
        message VARCHAR(500),
        stack TEXT,
        component_stack TEXT,
        user_agent VARCHAR(200),
        rota VARCHAR(200),
        ip VARCHAR(64),
        criado_em TIMESTAMP NOT NULL DEFAULT now()
      )
    `)
    migrated = true
  } catch (e) {
    log.warn('ClientErrors', `migracao falhou: ${e.code || e.name || 'unknown'}`)
  }
}

router.post('/', errorLimiter, async (req, res) => {
  await ensureTable()
  const b = req.body || {}
  const trunc = (s, n) => (typeof s === 'string' ? s.slice(0, n) : null)
  try {
    await pool.query(
      `INSERT INTO client_errors (message, stack, component_stack, user_agent, rota, ip)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        trunc(b.message, 500) || 'unknown',
        trunc(b.stack, 2000),
        trunc(b.componentStack, 2000),
        trunc(b.user_agent, 200),
        trunc(b.rota, 200),
        (req.ip || '').slice(0, 64)
      ]
    )
    res.json({ ok: true })
  } catch (e) {
    log.warn('ClientErrors', `falha ao salvar: ${e.code || 'unknown'}`)
    res.status(204).end()
  }
})

export default router
