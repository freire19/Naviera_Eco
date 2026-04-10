import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/financeiro/entradas?viagem_id=X
router.get('/entradas', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = 'SELECT * FROM financeiro_entradas WHERE empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
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
    const empresaId = req.user.empresa_id
    let sql = 'SELECT * FROM financeiro_saidas WHERE excluido = FALSE AND empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
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

    const empresaId = req.user.empresa_id
    const [passagens, encomendas, fretes, saidas] = await Promise.all([
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM passagens WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(valor_pago), 0) AS total FROM fretes WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(valor), 0) AS total FROM financeiro_saidas WHERE id_viagem = $1 AND excluido = FALSE AND empresa_id = $2', [viagem_id, empresaId])
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

// POST /api/financeiro/saida
router.post('/saida', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_viagem, descricao, valor, data, id_categoria, id_funcionario, tipo } = req.body
    if (!descricao || !valor || !data) {
      return res.status(400).json({ error: 'Campos obrigatorios: descricao, valor, data' })
    }
    const result = await pool.query(`
      INSERT INTO financeiro_saidas (id_viagem, descricao, valor, data, id_categoria, funcionario_id, tipo, excluido, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, FALSE, $8)
      RETURNING *
    `, [id_viagem || null, descricao, valor, data, id_categoria || null, id_funcionario || null, tipo || null, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Financeiro] Erro ao criar saida:', err.message)
    res.status(500).json({ error: 'Erro ao criar saida' })
  }
})

// DELETE /api/financeiro/saida/:id (soft delete)
router.delete('/saida/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { motivo } = req.body
    const result = await pool.query(
      'UPDATE financeiro_saidas SET excluido = TRUE, motivo_exclusao = $1 WHERE id = $2 AND empresa_id = $3 RETURNING id',
      [motivo || null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Saida nao encontrada' })
    res.json({ mensagem: 'Saida excluida' })
  } catch (err) {
    console.error('[Financeiro] Erro ao excluir saida:', err.message)
    res.status(500).json({ error: 'Erro ao excluir saida' })
  }
})

export default router
