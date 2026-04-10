import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/passagens?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    let sql = `
      SELECT p.*, pas.nome AS nome_passageiro, pas.numero_doc
      FROM passagens p
      LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
    `
    const params = []
    if (viagem_id) {
      sql += ' WHERE p.id_viagem = $1'
      params.push(viagem_id)
    }
    sql += ' ORDER BY p.num_bilhete DESC'

    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Passagens] Erro:', err.message)
    res.status(500).json({ error: 'Erro ao listar passagens' })
  }
})

// GET /api/passagens/resumo?viagem_id=X
router.get('/resumo', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })

    const result = await pool.query(`
      SELECT COUNT(*) AS total,
             COALESCE(SUM(valor_total), 0) AS valor_total,
             COALESCE(SUM(valor_pago), 0) AS valor_pago
      FROM passagens WHERE id_viagem = $1
    `, [viagem_id])
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar resumo' })
  }
})

export default router
