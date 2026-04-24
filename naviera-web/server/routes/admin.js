import { Router } from 'express'
import crypto from 'crypto'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()

// All admin routes require auth
router.use(authMiddleware)

// Admin-only middleware: must be Administrador AND on admin subdomain
// #DB210: whitelist estrita (nao basta prefix 'admin.' — atacante pode forjar Host: admin.evil.com)
//   Fail-closed fora de dev: se ADMIN_HOSTS ausente, staging/prod derruba todo /api/admin.
const ADMIN_HOSTS = (process.env.ADMIN_HOSTS || '')
  .split(',').map(s => s.trim().toLowerCase()).filter(Boolean)
if (ADMIN_HOSTS.length === 0 && process.env.NODE_ENV !== 'development') {
  console.warn('[Admin] ADMIN_HOSTS nao configurado — /api/admin so aceitara em dev. Defina ADMIN_HOSTS=admin.seu-dominio.com em prod.')
}
function adminOnly(req, res, next) {
  const host = (req.hostname || req.headers.host || '').toLowerCase().split(':')[0]
  const isDev = process.env.NODE_ENV === 'development' && host === 'localhost'
  const isAdminHost = ADMIN_HOSTS.includes(host) || isDev
  if (!isAdminHost) {
    return res.status(403).json({ error: 'Acesso restrito ao painel admin' })
  }
  const funcao = (req.user.funcao || '').toLowerCase()
  if (funcao !== 'administrador' && funcao !== 'admin') {
    return res.status(403).json({ error: 'Acesso restrito a administradores' })
  }
  next()
}

router.use(adminOnly)

// ============================================================
// EMPRESAS
// ============================================================

// GET /api/admin/empresas — list all empresas with stats
router.get('/empresas', async (req, res) => {
  try {
    // DP053: LEFT JOIN + GROUP BY em vez de subqueries correlacionadas (evita N+1)
    const result = await pool.query(`
      SELECT e.*,
        COALESCE(u.cnt, 0) AS total_usuarios,
        COALESCE(p.cnt, 0) AS total_passagens,
        COALESCE(en.cnt, 0) AS total_encomendas,
        COALESCE(f.cnt, 0) AS total_fretes
      FROM empresas e
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM usuarios WHERE excluido = FALSE OR excluido IS NULL GROUP BY empresa_id) u ON u.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM passagens GROUP BY empresa_id) p ON p.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM encomendas GROUP BY empresa_id) en ON en.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM fretes GROUP BY empresa_id) f ON f.empresa_id = e.id
      ORDER BY e.nome
    `)
    res.json(result.rows)
  } catch (err) {
    console.error('[Admin] Erro ao listar empresas:', err.message)
    res.status(500).json({ error: 'Erro ao listar empresas' })
  }
})

// POST /api/admin/empresas — create new empresa + first admin user + activation code
router.post('/empresas', async (req, res) => {
  const client = await pool.connect()
  try {
    const { nome, slug, cor_primaria, logo_url, operador_nome, operador_email } = req.body
    if (!nome || !slug) {
      return res.status(400).json({ error: 'nome e slug obrigatorios' })
    }
    if (!operador_nome || !operador_email) {
      return res.status(400).json({ error: 'Nome e email do operador obrigatorios' })
    }

    await client.query('BEGIN')

    // Generate activation code
    // #DB212: 6 bytes = 12 hex (~10^14 combinacoes); 2 bytes tinham 65K e eram enumeraveis
    const codigoAtivacao = 'NAV-' + crypto.randomBytes(6).toString('hex').toUpperCase()

    // Create empresa with activation code
    const empresaResult = await client.query(
      `INSERT INTO empresas (nome, slug, cor_primaria, logo_url, ativo, codigo_ativacao)
       VALUES ($1, $2, $3, $4, TRUE, $5) RETURNING *`,
      [nome, slug.toLowerCase(), cor_primaria || '#059669', logo_url || null, codigoAtivacao]
    )
    const empresa = empresaResult.rows[0]

    // Generate temporary password
    const senhaTemp = crypto.randomBytes(4).toString('hex')
    const senhaHash = await bcrypt.hash(senhaTemp, 10)

    // Create first admin user with deve_trocar_senha = TRUE
    await client.query(
      `INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id, deve_trocar_senha)
       VALUES ($1, $2, $3, 'Administrador', 'ADMIN', $4, TRUE)`,
      [operador_nome, operador_email.toLowerCase(), senhaHash, empresa.id]
    )

    await client.query('COMMIT')

    res.status(201).json({
      ...empresa,
      operador: {
        nome: operador_nome,
        email: operador_email.toLowerCase(),
        senha_temporaria: senhaTemp
      }
    })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Admin] Erro ao criar empresa:', err.message)
    if (err.code === '23505') {
      const detail = err.detail || ''
      if (detail.includes('slug')) return res.status(409).json({ error: 'Slug ja existe' })
      if (detail.includes('email')) return res.status(409).json({ error: 'Email do operador ja existe' })
      return res.status(409).json({ error: 'Registro duplicado' })
    }
    res.status(500).json({ error: 'Erro ao criar empresa' })
  } finally {
    client.release()
  }
})

// PUT /api/admin/empresas/:id — update empresa
// #DB213: slug e imutavel — tokens JWT e cache tenant referenciam o slug.
//   Mudar slug vivo causa token hijacking se slug antigo for reusado por outra empresa.
router.put('/empresas/:id', async (req, res) => {
  try {
    const { nome, slug, cor_primaria, logo_url } = req.body
    if (slug != null) {
      const atual = await pool.query('SELECT slug FROM empresas WHERE id = $1', [req.params.id])
      const slugAtual = atual.rows[0]?.slug
      if (slugAtual && slug.toLowerCase() !== slugAtual.toLowerCase()) {
        return res.status(400).json({
          error: 'slug imutavel apos criacao — tokens e cache dependem do valor atual'
        })
      }
    }
    const result = await pool.query(
      `UPDATE empresas SET
        nome = COALESCE($1, nome),
        cor_primaria = COALESCE($2, cor_primaria),
        logo_url = COALESCE($3, logo_url)
      WHERE id = $4 RETURNING *`,
      [nome, cor_primaria, logo_url, req.params.id]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Empresa nao encontrada' })
    }
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Admin] Erro ao atualizar empresa:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar empresa' })
  }
})

// PUT /api/admin/empresas/:id/ativar — toggle ativo
router.put('/empresas/:id/ativar', async (req, res) => {
  try {
    const result = await pool.query(
      'UPDATE empresas SET ativo = NOT ativo WHERE id = $1 RETURNING *',
      [req.params.id]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Empresa nao encontrada' })
    }
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Admin] Erro ao toggle ativo empresa:', err.message)
    res.status(500).json({ error: 'Erro ao alterar status da empresa' })
  }
})

// GET /api/admin/empresas/:id/stats — empresa stats
router.get('/empresas/:id/stats', async (req, res) => {
  try {
    const id = req.params.id
    const [usuarios, passagens, encomendas, fretes] = await Promise.all([
      pool.query('SELECT COUNT(*) AS total FROM usuarios WHERE empresa_id = $1 AND (excluido = FALSE OR excluido IS NULL)', [id]),
      pool.query('SELECT COUNT(*) AS total FROM passagens WHERE empresa_id = $1', [id]),
      pool.query('SELECT COUNT(*) AS total FROM encomendas WHERE empresa_id = $1', [id]),
      pool.query('SELECT COUNT(*) AS total FROM fretes WHERE empresa_id = $1', [id])
    ])
    res.json({
      usuarios: parseInt(usuarios.rows[0].total),
      passagens: parseInt(passagens.rows[0].total),
      encomendas: parseInt(encomendas.rows[0].total),
      fretes: parseInt(fretes.rows[0].total)
    })
  } catch (err) {
    console.error('[Admin] Erro ao buscar stats empresa:', err.message)
    res.status(500).json({ error: 'Erro ao buscar estatisticas' })
  }
})

// ============================================================
// PSP — onboarding de subconta Asaas em nome de qualquer empresa
// ============================================================
// Proxy para Spring API /api/admin/empresas/{id}/psp/...
// adminOnly middleware ja garante ROLE_ADMIN.
const SPRING_API_BASE_PSP = process.env.SPRING_API_BASE || 'http://localhost:8081/api'

// #DS5-201: valida id como inteiro positivo antes de interpolar na URL upstream
function parseEmpresaId(req, res) {
  const raw = req.params.id
  const id = Number.parseInt(raw, 10)
  if (!Number.isInteger(id) || id <= 0 || id > 2_147_483_647 || String(id) !== raw) {
    res.status(400).json({ error: 'ID de empresa invalido' })
    return null
  }
  return id
}

router.get('/empresas/:id/psp/status', async (req, res) => {
  const id = parseEmpresaId(req, res)
  if (id == null) return
  try {
    const upstream = await fetch(`${SPRING_API_BASE_PSP}/admin/empresas/${id}/psp/status`, {
      headers: { Authorization: req.headers.authorization }
    })
    const body = await upstream.text()
    res.status(upstream.status).type('application/json').send(body)
  } catch (err) {
    console.error('[Admin] Erro proxy /psp/status:', err.message)
    res.status(502).json({ error: 'Backend PSP indisponivel' })
  }
})

router.post('/empresas/:id/psp/onboarding', async (req, res) => {
  const id = parseEmpresaId(req, res)
  if (id == null) return
  try {
    const upstream = await fetch(`${SPRING_API_BASE_PSP}/admin/empresas/${id}/psp/onboarding`, {
      method: 'POST',
      headers: {
        Authorization: req.headers.authorization,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(req.body || {})
    })
    const body = await upstream.text()
    res.status(upstream.status).type('application/json').send(body)
  } catch (err) {
    console.error('[Admin] Erro proxy /psp/onboarding:', err.message)
    res.status(502).json({ error: 'Backend PSP indisponivel' })
  }
})

// ============================================================
// METRICAS
// ============================================================

// GET /api/admin/metricas — platform-wide metrics
router.get('/metricas', async (req, res) => {
  try {
    // DP053: 1 query com LEFT JOIN (era 2 queries com 4 subqueries correlacionadas cada)
    const porEmpresa = await pool.query(`
      SELECT
        e.id, e.nome, e.slug, e.ativo,
        COALESCE(u.cnt, 0) AS total_usuarios,
        COALESCE(p.cnt, 0) AS total_passagens,
        COALESCE(en.cnt, 0) AS total_encomendas,
        COALESCE(f.cnt, 0) AS total_fretes
      FROM empresas e
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM usuarios WHERE excluido = FALSE OR excluido IS NULL GROUP BY empresa_id) u ON u.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM passagens GROUP BY empresa_id) p ON p.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM encomendas GROUP BY empresa_id) en ON en.empresa_id = e.id
      LEFT JOIN (SELECT empresa_id, COUNT(*) AS cnt FROM fretes GROUP BY empresa_id) f ON f.empresa_id = e.id
      ORDER BY e.nome
    `)

    // Totais derivados da soma por empresa (evita 5 full-table scans extras)
    const rows = porEmpresa.rows
    const totais = {
      empresas: rows.length,
      usuarios: rows.reduce((s, r) => s + parseInt(r.total_usuarios), 0),
      passagens: rows.reduce((s, r) => s + parseInt(r.total_passagens), 0),
      encomendas: rows.reduce((s, r) => s + parseInt(r.total_encomendas), 0),
      fretes: rows.reduce((s, r) => s + parseInt(r.total_fretes), 0)
    }

    res.json({
      totais,
      por_empresa: rows.map(r => ({
        id: r.id,
        nome: r.nome,
        slug: r.slug,
        ativo: r.ativo,
        total_usuarios: parseInt(r.total_usuarios),
        total_passagens: parseInt(r.total_passagens),
        total_encomendas: parseInt(r.total_encomendas),
        total_fretes: parseInt(r.total_fretes)
      }))
    })
  } catch (err) {
    console.error('[Admin] Erro ao buscar metricas:', err.message)
    res.status(500).json({ error: 'Erro ao buscar metricas' })
  }
})

// ============================================================
// USUARIOS
// ============================================================

// GET /api/admin/usuarios — list all users across all empresas
router.get('/usuarios', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT u.id, u.nome, u.email, u.funcao, u.permissao, u.empresa_id,
        e.nome AS empresa_nome, e.slug AS empresa_slug
      FROM usuarios u
      LEFT JOIN empresas e ON u.empresa_id = e.id
      WHERE u.excluido = FALSE OR u.excluido IS NULL
      ORDER BY e.nome, u.nome
    `)
    res.json(result.rows)
  } catch (err) {
    console.error('[Admin] Erro ao listar usuarios:', err.message)
    res.status(500).json({ error: 'Erro ao listar usuarios' })
  }
})

export default router
