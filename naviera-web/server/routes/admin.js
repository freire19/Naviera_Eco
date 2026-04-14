import { Router } from 'express'
import crypto from 'crypto'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()

// All admin routes require auth
router.use(authMiddleware)

// Admin-only middleware: must be Administrador AND on admin subdomain
function adminOnly(req, res, next) {
  const host = req.hostname || req.headers.host || ''
  const isAdminSubdomain = host.startsWith('admin.') || host === 'localhost'
  if (!isAdminSubdomain) {
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
    const result = await pool.query(`
      SELECT e.*,
        (SELECT COUNT(*) FROM usuarios u WHERE u.empresa_id = e.id AND (u.excluido = FALSE OR u.excluido IS NULL)) AS total_usuarios,
        (SELECT COUNT(*) FROM passagens p WHERE p.empresa_id = e.id) AS total_passagens,
        (SELECT COUNT(*) FROM encomendas en WHERE en.empresa_id = e.id) AS total_encomendas,
        (SELECT COUNT(*) FROM fretes f WHERE f.empresa_id = e.id) AS total_fretes
      FROM empresas e
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
    const codigoAtivacao = 'NAV-' + crypto.randomBytes(2).toString('hex').toUpperCase()

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
router.put('/empresas/:id', async (req, res) => {
  try {
    const { nome, slug, cor_primaria, logo_url } = req.body
    const result = await pool.query(
      `UPDATE empresas SET
        nome = COALESCE($1, nome),
        slug = COALESCE($2, slug),
        cor_primaria = COALESCE($3, cor_primaria),
        logo_url = COALESCE($4, logo_url)
      WHERE id = $5 RETURNING *`,
      [nome, slug ? slug.toLowerCase() : null, cor_primaria, logo_url, req.params.id]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Empresa nao encontrada' })
    }
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Admin] Erro ao atualizar empresa:', err.message)
    if (err.code === '23505') {
      return res.status(409).json({ error: 'Slug ja existe' })
    }
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
// METRICAS
// ============================================================

// GET /api/admin/metricas — platform-wide metrics
router.get('/metricas', async (req, res) => {
  try {
    const [totais, porEmpresa] = await Promise.all([
      pool.query(`
        SELECT
          (SELECT COUNT(*) FROM empresas) AS total_empresas,
          (SELECT COUNT(*) FROM usuarios WHERE excluido = FALSE OR excluido IS NULL) AS total_usuarios,
          (SELECT COUNT(*) FROM passagens) AS total_passagens,
          (SELECT COUNT(*) FROM encomendas) AS total_encomendas,
          (SELECT COUNT(*) FROM fretes) AS total_fretes
      `),
      pool.query(`
        SELECT
          e.id, e.nome, e.slug, e.ativo,
          (SELECT COUNT(*) FROM usuarios u WHERE u.empresa_id = e.id AND (u.excluido = FALSE OR u.excluido IS NULL)) AS total_usuarios,
          (SELECT COUNT(*) FROM passagens p WHERE p.empresa_id = e.id) AS total_passagens,
          (SELECT COUNT(*) FROM encomendas en WHERE en.empresa_id = e.id) AS total_encomendas,
          (SELECT COUNT(*) FROM fretes f WHERE f.empresa_id = e.id) AS total_fretes
        FROM empresas e
        ORDER BY e.nome
      `)
    ])

    const t = totais.rows[0]
    res.json({
      totais: {
        empresas: parseInt(t.total_empresas),
        usuarios: parseInt(t.total_usuarios),
        passagens: parseInt(t.total_passagens),
        encomendas: parseInt(t.total_encomendas),
        fretes: parseInt(t.total_fretes)
      },
      por_empresa: porEmpresa.rows.map(r => ({
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
