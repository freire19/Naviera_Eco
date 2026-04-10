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
    // Login by nome or email
    const result = await pool.query(
      'SELECT id, nome, email, senha, funcao, permissao, empresa_id FROM usuarios WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1)) AND (excluido = FALSE OR excluido IS NULL)',
      [login]
    )

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
      empresa_id: user.empresa_id
    })

    res.json({
      token,
      usuario: {
        id: user.id,
        nome: user.nome,
        login: user.nome,
        email: user.email,
        funcao: user.funcao,
        permissoes: user.permissao
      }
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
      'SELECT id, nome, email, funcao, permissao, empresa_id FROM usuarios WHERE id = $1',
      [req.user.id]
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

export default router
