import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/agenda?mes=4&ano=2026
router.get('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { mes, ano } = req.query
    let sql = 'SELECT id_anotacao AS id, data_evento, descricao, concluida, empresa_id FROM agenda_anotacoes WHERE empresa_id = $1'
    const params = [empresaId]

    if (mes && ano) {
      // DP057: range query instead of EXTRACT() to allow index usage
      const m = parseInt(mes)
      const y = parseInt(ano)
      const firstDay = `${y}-${String(m).padStart(2, '0')}-01`
      const nextMonth = m === 12 ? `${y + 1}-01-01` : `${y}-${String(m + 1).padStart(2, '0')}-01`
      sql += ' AND data_evento >= $2 AND data_evento < $3'
      params.push(firstDay, nextMonth)
    }
    sql += ' ORDER BY data_evento, id_anotacao'

    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Agenda] Erro ao listar:', err.message)
    res.status(500).json({ error: 'Erro ao listar anotacoes' })
  }
})

// POST /api/agenda
router.post('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { data_evento, descricao } = req.body
    if (!data_evento || !descricao) {
      return res.status(400).json({ error: 'data_evento e descricao obrigatorios' })
    }
    const result = await pool.query(
      `INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id)
       VALUES ($1, $2, FALSE, $3)
       RETURNING id_anotacao AS id, data_evento, descricao, concluida, empresa_id`,
      [data_evento, descricao.trim(), empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Agenda] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar anotacao' })
  }
})

// PUT /api/agenda/:id
router.put('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { descricao, concluida } = req.body
    const result = await pool.query(`
      UPDATE agenda_anotacoes
      SET descricao = COALESCE($1, descricao),
          concluida = COALESCE($2, concluida)
      WHERE id_anotacao = $3 AND empresa_id = $4
      RETURNING id_anotacao AS id, data_evento, descricao, concluida, empresa_id
    `, [descricao || null, concluida !== undefined ? concluida : null, req.params.id, empresaId])

    if (result.rows.length === 0) return res.status(404).json({ error: 'Anotacao nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Agenda] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar anotacao' })
  }
})

// DELETE /api/agenda/:id
router.delete('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'DELETE FROM agenda_anotacoes WHERE id_anotacao = $1 AND empresa_id = $2 RETURNING id_anotacao',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Anotacao nao encontrada' })
    res.json({ mensagem: 'Anotacao excluida' })
  } catch (err) {
    console.error('[Agenda] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir anotacao' })
  }
})

export default router
