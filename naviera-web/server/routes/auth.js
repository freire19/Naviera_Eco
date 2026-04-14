import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { generateToken, authMiddleware } from '../middleware/auth.js'
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

    const token = generateToken({
      id: user.id,
      login: user.nome,
      funcao: user.funcao,
      empresa_id: user.empresa_id || 1
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
    await pool.query(
      'UPDATE usuarios SET senha = $1, deve_trocar_senha = FALSE WHERE id = $2 AND empresa_id = $3',
      [novaHash, req.user.id, req.user.empresa_id]
    )

    res.json({ mensagem: 'Senha alterada com sucesso' })
  } catch (err) {
    console.error('[Auth] Erro ao trocar senha:', err.message)
    res.status(500).json({ error: 'Erro ao trocar senha' })
  }
})

export default router
