import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/financeiro/entradas?viagem_id=X
router.get('/entradas', async (req, res) => {
  try {
    const { viagem_id } = req.query
    let sql = 'SELECT * FROM financeiro_entradas'
    const params = []
    if (viagem_id) {
      sql += ' WHERE id_viagem = $1'
      params.push(viagem_id)
    }
    sql += ' ORDER BY data_entrada DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar entradas' })
  }
})

// GET /api/financeiro/saidas?viagem_id=X
router.get('/saidas', async (req, res) => {
  try {
    const { viagem_id } = req.query
    let sql = 'SELECT * FROM financeiro_saidas WHERE excluido = FALSE'
    const params = []
    if (viagem_id) {
      sql += ' AND id_viagem = $1'
      params.push(viagem_id)
    }
    sql += ' ORDER BY data DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar saidas' })
  }
})

// GET /api/financeiro/balanco?viagem_id=X
router.get('/balanco', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })

    const [passagens, encomendas, fretes, saidas] = await Promise.all([
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM passagens WHERE id_viagem = $1', [viagem_id]),
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM encomendas WHERE id_viagem = $1', [viagem_id]),
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM fretes WHERE id_viagem = $1', [viagem_id]),
      pool.query('SELECT COALESCE(SUM(valor), 0) AS total FROM financeiro_saidas WHERE id_viagem = $1 AND excluido = FALSE', [viagem_id])
    ])

    const receitas = {
      passagens: parseFloat(passagens.rows[0].total),
      encomendas: parseFloat(encomendas.rows[0].total),
      fretes: parseFloat(fretes.rows[0].total)
    }
    const totalReceitas = receitas.passagens + receitas.encomendas + receitas.fretes
    const totalDespesas = parseFloat(saidas.rows[0].total)

    res.json({
      receitas,
      totalReceitas,
      totalDespesas,
      saldo: totalReceitas - totalDespesas
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao calcular balanco' })
  }
})

export default router
