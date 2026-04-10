import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/passagens?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = `
      SELECT p.*, pas.nome AS nome_passageiro, pas.numero_doc
      FROM passagens p
      LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
      WHERE p.empresa_id = $1
    `
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND p.id_viagem = $2'
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

    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT COUNT(*) AS total,
             COALESCE(SUM(valor_total), 0) AS valor_total,
             COALESCE(SUM(valor_pago), 0) AS valor_pago
      FROM passagens WHERE id_viagem = $1 AND empresa_id = $2
    `, [viagem_id, empresaId])
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar resumo' })
  }
})

// POST /api/passagens
router.post('/', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      id_passageiro, id_viagem, assento, id_rota, id_tipo_passagem, id_acomodacao,
      id_caixa, valor_total, valor_pago, observacoes,
      valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao
    } = req.body
    if (!id_passageiro || !id_viagem || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_passageiro, id_viagem, valor_total' })
    }

    await client.query('BEGIN')

    // Auto-sequence numero_bilhete
    const seqResult = await client.query(
      'SELECT COALESCE(MAX(num_bilhete), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
      [empresaId]
    )
    const numBilhete = seqResult.rows[0].next_num

    const vTotal = parseFloat(valor_total) || 0
    const vPago = parseFloat(valor_pago) || 0
    const vDevedor = vTotal - vPago
    const status = vDevedor <= 0.01 ? 'PAGO' : 'PENDENTE'

    const result = await client.query(`
      INSERT INTO passagens (num_bilhete, id_passageiro, id_viagem, data_emissao, assento,
        id_rota, id_tipo_passagem, id_acomodacao, id_caixa, id_usuario_emissor,
        valor_total, valor_a_pagar, valor_pago, valor_devedor, status_passagem,
        observacoes, valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao, empresa_id)
      VALUES ($1,$2,$3,CURRENT_DATE,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19)
      RETURNING *
    `, [
      numBilhete, id_passageiro, id_viagem, assento || null,
      id_rota || null, id_tipo_passagem || null, id_acomodacao || null,
      id_caixa || null, req.user.id,
      vTotal, vTotal, vPago, vDevedor, status,
      observacoes || null,
      valor_pagamento_dinheiro || 0, valor_pagamento_pix || 0, valor_pagamento_cartao || 0,
      empresaId
    ])

    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Passagens] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar passagem' })
  } finally {
    client.release()
  }
})

// PUT /api/passagens/:id
router.put('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { assento, observacoes, id_acomodacao, id_rota } = req.body
    const result = await pool.query(`
      UPDATE passagens SET assento = COALESCE($1, assento), observacoes = COALESCE($2, observacoes),
        id_acomodacao = COALESCE($3, id_acomodacao), id_rota = COALESCE($4, id_rota)
      WHERE id_passagem = $5 AND empresa_id = $6
      RETURNING *
    `, [assento, observacoes, id_acomodacao, id_rota, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Passagem nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Passagens] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar passagem' })
  }
})

// POST /api/passagens/:id/pagar
router.post('/:id/pagar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { valor_pago } = req.body
    if (!valor_pago || valor_pago <= 0) {
      return res.status(400).json({ error: 'valor_pago obrigatorio e deve ser positivo' })
    }
    const result = await pool.query(`
      UPDATE passagens SET valor_pago = valor_pago + $1,
        valor_devedor = valor_devedor - $1,
        status_passagem = CASE WHEN (valor_devedor - $1) <= 0.01 THEN 'PAGO' ELSE 'PARCIAL' END
      WHERE id_passagem = $2 AND empresa_id = $3
      RETURNING *
    `, [valor_pago, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Passagem nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Passagens] Erro ao pagar:', err.message)
    res.status(500).json({ error: 'Erro ao registrar pagamento' })
  }
})

// DELETE /api/passagens/:id
router.delete('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'DELETE FROM passagens WHERE id_passagem = $1 AND empresa_id = $2 RETURNING id_passagem',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Passagem nao encontrada' })
    res.json({ mensagem: 'Passagem excluida' })
  } catch (err) {
    console.error('[Passagens] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir passagem' })
  }
})

export default router
