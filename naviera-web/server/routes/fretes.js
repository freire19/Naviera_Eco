import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/fretes?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = 'SELECT * FROM fretes WHERE empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY id_frete DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar fretes' })
  }
})

// GET /api/fretes/resumo?viagem_id=X
router.get('/resumo', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT COUNT(*) AS total,
             COALESCE(SUM(valor_frete_calculado), 0) AS valor_total,
             COALESCE(SUM(valor_pago), 0) AS valor_pago
      FROM fretes WHERE id_viagem = $1 AND empresa_id = $2
    `, [viagem_id, empresaId])
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar resumo' })
  }
})

// POST /api/fretes
router.post('/', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      id_viagem, remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp,
      observacoes, valor_total_itens, desconto, valor_pago, tipo_pagamento, nome_caixa, itens,
      data_saida_viagem, local_transporte, cidade_cobranca, num_notafiscal,
      valor_notafiscal, peso_notafiscal, troco, status_frete
    } = req.body
    if (!id_viagem || !valor_total_itens) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, valor_total_itens' })
    }

    await client.query('BEGIN')

    const idResult = await client.query(
      'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
      [empresaId]
    )
    const nextIdFrete = idResult.rows[0].next_id

    const seqResult = await client.query(
      'SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num FROM fretes WHERE empresa_id = $1',
      [empresaId]
    )
    const numFrete = seqResult.rows[0].next_num

    const vItens = parseFloat(valor_total_itens) || 0
    const vDesconto = parseFloat(desconto) || 0
    const vCalculado = vItens - vDesconto
    const vPago = parseFloat(valor_pago) || 0
    const vDevedor = vCalculado - vPago

    const result = await client.query(`
      INSERT INTO fretes (id_frete, numero_frete, data_emissao, id_viagem, remetente_nome_temp, destinatario_nome_temp,
        rota_temp, conferente_temp, observacoes, valor_total_itens, desconto,
        valor_frete_calculado, valor_pago, valor_devedor, tipo_pagamento, nome_caixa,
        data_saida_viagem, local_transporte, cidade_cobranca, num_notafiscal,
        valor_notafiscal, peso_notafiscal, troco, status_frete, empresa_id)
      VALUES ($1,$2,CURRENT_DATE,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24)
      RETURNING *
    `, [
      nextIdFrete, numFrete, id_viagem, remetente_nome_temp || null, destinatario_nome_temp || null,
      rota_temp || null, conferente_temp || null, observacoes || null,
      vItens, vDesconto, vCalculado, vPago, vDevedor,
      tipo_pagamento || null, nome_caixa || null,
      data_saida_viagem || null, local_transporte || null, cidade_cobranca || null, num_notafiscal || null,
      parseFloat(valor_notafiscal) || 0, parseFloat(peso_notafiscal) || 0, parseFloat(troco) || 0,
      status_frete || null, empresaId
    ])

    const freteId = result.rows[0].id_frete
    if (itens && Array.isArray(itens)) {
      for (const item of itens) {
        await client.query(`
          INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item)
          VALUES ($1, $2, $3, $4, $5)
        `, [freteId, item.nome_item || null, item.quantidade || 1, item.preco_unitario || item.valor_unitario || 0, item.subtotal_item || item.valor_total || 0])
      }
    }

    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Fretes] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar frete' })
  } finally {
    client.release()
  }
})

// PUT /api/fretes/:id
router.put('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp, observacoes } = req.body
    const result = await pool.query(`
      UPDATE fretes SET remetente_nome_temp = COALESCE($1, remetente_nome_temp),
        destinatario_nome_temp = COALESCE($2, destinatario_nome_temp),
        rota_temp = COALESCE($3, rota_temp), conferente_temp = COALESCE($4, conferente_temp),
        observacoes = COALESCE($5, observacoes)
      WHERE id_frete = $6 AND empresa_id = $7
      RETURNING *
    `, [remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp, observacoes, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Frete nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Fretes] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar frete' })
  }
})

// POST /api/fretes/:id/pagar
router.post('/:id/pagar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { valor_pago } = req.body
    if (!valor_pago || valor_pago <= 0) {
      return res.status(400).json({ error: 'valor_pago obrigatorio e deve ser positivo' })
    }
    const result = await pool.query(`
      UPDATE fretes SET valor_pago = valor_pago + $1, valor_devedor = valor_devedor - $1
      WHERE id_frete = $2 AND empresa_id = $3
      RETURNING *
    `, [valor_pago, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Frete nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Fretes] Erro ao pagar:', err.message)
    res.status(500).json({ error: 'Erro ao registrar pagamento' })
  }
})

// DELETE /api/fretes/:id
router.delete('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    await client.query('BEGIN')
    await client.query('DELETE FROM frete_itens WHERE id_frete = $1', [req.params.id])
    const result = await client.query(
      'DELETE FROM fretes WHERE id_frete = $1 AND empresa_id = $2 RETURNING id_frete',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
    if (result.rows.length === 0) return res.status(404).json({ error: 'Frete nao encontrado' })
    res.json({ mensagem: 'Frete excluido' })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Fretes] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir frete' })
  } finally {
    client.release()
  }
})

export default router
