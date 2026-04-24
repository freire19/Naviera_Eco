import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { generateToken, authMiddleware, invalidateSenhaCache } from '../middleware/auth.js'
import { rateLimit } from '../middleware/rateLimit.js'

const router = Router()

const loginLimiter = rateLimit({ windowMs: 60000, max: 10, message: 'Muitas tentativas de login. Aguarde 1 minuto.' })

// POST /api/auth/login
router.post('/login', loginLimiter, async (req, res) => {
  const { login, senha } = req.body
  if (!login || !senha) {
    return res.status(400).json({ error: 'Login e senha obrigatorios' })
  }

  try {
    // Se o tenant foi resolvido pelo subdominio, filtrar por empresa_id
    const tenantId = req.tenant?.id || null

    // OCR app e Admin console nao tem tenant — resolvem empresa_id pelo proprio usuario.
    // adminOnly middleware valida funcao=Administrador nas rotas /api/admin.
    // #DB211: x-tenant-slug=admin/ocr so vale quando origin/host tambem bate no dominio
    //   (evita credential-stuffing cross-tenant via header forjado).
    const origin = req.headers.origin || req.headers.referer || ''
    const host = (req.headers.host || '').toLowerCase()
    const isDev = process.env.NODE_ENV === 'development'
    const originOk = (re) => re.test(origin) || re.test(host)
    const isOcrApp = originOk(/ocr\.naviera\.com\.br/) || (isDev && req.headers['x-tenant-slug'] === 'ocr')
    const isAdminApp = originOk(/admin\.naviera\.com\.br/) || (isDev && req.headers['x-tenant-slug'] === 'admin')

    let sql, params
    if (tenantId) {
      // Producao: filtrar usuario pela empresa do subdominio
      sql = `SELECT id, nome, email, senha, funcao, permissao, empresa_id,
                    COALESCE(deve_trocar_senha, FALSE) AS deve_trocar_senha
             FROM usuarios
             WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1))
               AND (excluido = FALSE OR excluido IS NULL)
               AND empresa_id = $2`
      params = [login, tenantId]
    } else if (isOcrApp || isAdminApp) {
      // OCR/Admin: usuario resolve empresa_id pelo banco (sem filtro de tenant)
      sql = `SELECT id, nome, email, senha, funcao, permissao, empresa_id,
                    COALESCE(deve_trocar_senha, FALSE) AS deve_trocar_senha
             FROM usuarios
             WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1))
               AND (excluido = FALSE OR excluido IS NULL)`
      params = [login]
    } else if (process.env.NODE_ENV === 'production') {
      // Producao: obrigar subdominio de empresa
      return res.status(400).json({ error: 'Subdominio da empresa obrigatorio' })
    } else {
      // Dev (localhost): aceitar qualquer empresa
      sql = `SELECT id, nome, email, senha, funcao, permissao, empresa_id,
                    COALESCE(deve_trocar_senha, FALSE) AS deve_trocar_senha
             FROM usuarios
             WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1))
               AND (excluido = FALSE OR excluido IS NULL)`
      params = [login]
    }

    const result = await pool.query(sql, params)

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Credenciais invalidas' })
    }

    const user = result.rows[0]
    const senhaValida = await bcrypt.compare(senha, user.senha)
    if (!senhaValida) {
      return res.status(401).json({ error: 'Credenciais invalidas' })
    }

    if (!user.empresa_id) {
      return res.status(401).json({ error: 'Usuario sem empresa vinculada' })
    }

    const token = generateToken({
      id: user.id,
      login: user.nome,
      funcao: user.funcao,
      empresa_id: user.empresa_id
    })

    res.json({
      token,
      deve_trocar_senha: user.deve_trocar_senha === true,
      usuario: {
        id: user.id,
        nome: user.nome,
        login: user.nome,
        email: user.email,
        funcao: user.funcao,
        permissoes: user.permissao,
        empresa_id: user.empresa_id
      },
      empresa: req.tenant || null
    })
  } catch (err) {
    console.error('[Auth] Erro no login:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

// GET /api/auth/me
router.get('/me', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, nome, email, funcao, permissao, empresa_id FROM usuarios WHERE id = $1 AND empresa_id = $2',
      [req.user.id, req.user.empresa_id]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Usuario nao encontrado' })
    }
    const u = result.rows[0]
    res.json({ id: u.id, nome: u.nome, login: u.nome, email: u.email, funcao: u.funcao, permissoes: u.permissao, empresa_id: u.empresa_id })
  } catch (err) {
    res.status(500).json({ error: 'Erro interno' })
  }
})

// POST /api/auth/trocar-senha — troca de senha (obrigatoria ou voluntaria)
router.post('/trocar-senha', authMiddleware, async (req, res) => {
  const { senha_atual, nova_senha } = req.body
  if (!senha_atual || !nova_senha) {
    return res.status(400).json({ error: 'Senha atual e nova senha obrigatorias' })
  }
  if (nova_senha.length < 6) {
    return res.status(400).json({ error: 'Nova senha deve ter no minimo 6 caracteres' })
  }
  if (nova_senha.length > 128) {
    return res.status(400).json({ error: 'Senha deve ter no maximo 128 caracteres' })
  }

  try {
    const result = await pool.query(
      'SELECT senha FROM usuarios WHERE id = $1 AND empresa_id = $2',
      [req.user.id, req.user.empresa_id]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Usuario nao encontrado' })
    }

    const senhaValida = await bcrypt.compare(senha_atual, result.rows[0].senha)
    if (!senhaValida) {
      return res.status(401).json({ error: 'Senha atual incorreta' })
    }

    const novaHash = await bcrypt.hash(nova_senha, 10)
    // #234: senha_atualizada_em invalida JWTs antigos via authMiddleware
    await pool.query(
      'UPDATE usuarios SET senha = $1, deve_trocar_senha = FALSE, senha_atualizada_em = NOW() WHERE id = $2 AND empresa_id = $3',
      [novaHash, req.user.id, req.user.empresa_id]
    )
    invalidateSenhaCache(req.user.id)

    res.json({ mensagem: 'Senha alterada com sucesso. Refaca login nos outros dispositivos.' })
  } catch (err) {
    console.error('[Auth] Erro ao trocar senha:', err.message)
    res.status(500).json({ error: 'Erro ao trocar senha' })
  }
})

export default router
