import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/dashboard/resumo?viagem_id=X
router.get('/resumo', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })

    const empresaId = req.user.empresa_id
    const [passagens, encomendas, fretes] = await Promise.all([
      pool.query('SELECT COUNT(*) AS total, COALESCE(SUM(valor_total), 0) AS valor FROM passagens WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COUNT(*) AS total, COALESCE(SUM(total_a_pagar), 0) AS valor FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COUNT(*) AS total, COALESCE(SUM(valor_frete_calculado), 0) AS valor FROM fretes WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId])
    ])

    res.json({
      passagens: { total: parseInt(passagens.rows[0].total), valor: parseFloat(passagens.rows[0].valor) },
      encomendas: { total: parseInt(encomendas.rows[0].total), valor: parseFloat(encomendas.rows[0].valor) },
      fretes: { total: parseInt(fretes.rows[0].total), valor: parseFloat(fretes.rows[0].valor) }
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar resumo' })
  }
})

export default router
