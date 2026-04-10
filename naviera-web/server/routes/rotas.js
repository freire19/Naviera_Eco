import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

router.get('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT id AS id_rota, origem, destino FROM rotas WHERE empresa_id = $1 ORDER BY origem, destino', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar rotas' })
  }
})

export default router
