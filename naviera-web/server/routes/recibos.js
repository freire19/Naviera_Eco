import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/recibos?id_viagem=:id  (ou sem filtro = todos da empresa, mais recentes primeiro)
router.get('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_viagem } = req.query
    let sql = `SELECT id_recibo, id_viagem, nome_pagador, referente_a, valor,
                      data_emissao, tipo_recibo
               FROM recibos_avulsos WHERE empresa_id = $1`
    const params = [empresaId]
    if (id_viagem) {
      sql += ' AND id_viagem = $2'
      params.push(parseInt(id_viagem))
    }
    sql += ' ORDER BY id_recibo DESC LIMIT 500'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Recibos] Erro ao listar:', err.message)
    res.status(500).json({ error: 'Erro ao listar recibos' })
  }
})

// POST /api/recibos
router.post('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo } = req.body
    if (!id_viagem || !nome_pagador || valor === undefined || valor === null) {
      return res.status(400).json({ error: 'id_viagem, nome_pagador e valor sao obrigatorios' })
    }
    const valorNum = Number(valor)
    if (!Number.isFinite(valorNum) || valorNum < 0) {
      return res.status(400).json({ error: 'valor invalido' })
    }
    const dataEmissao = data_emissao || new Date().toISOString().substring(0, 10)
    const result = await pool.query(`
      INSERT INTO recibos_avulsos (id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING id_recibo, id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo
    `, [
      parseInt(id_viagem),
      String(nome_pagador).trim(),
      referente_a ? String(referente_a).trim() : null,
      valorNum,
      dataEmissao,
      tipo_recibo || 'PADRAO',
      empresaId
    ])
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Recibos] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar recibo' })
  }
})

// DELETE /api/recibos/:id
router.delete('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'DELETE FROM recibos_avulsos WHERE id_recibo = $1 AND empresa_id = $2 RETURNING id_recibo',
      [parseInt(req.params.id), empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Recibo nao encontrado' })
    res.json({ mensagem: 'Recibo excluido' })
  } catch (err) {
    console.error('[Recibos] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir recibo' })
  }
})

export default router
