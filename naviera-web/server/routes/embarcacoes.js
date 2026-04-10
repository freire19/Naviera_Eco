import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

router.get('/', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM embarcacoes ORDER BY nome')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar embarcacoes' })
  }
})

export default router
