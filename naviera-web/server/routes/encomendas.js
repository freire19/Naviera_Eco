import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'

const router = Router()
router.use(authMiddleware)

// GET /api/encomendas?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = 'SELECT * FROM encomendas WHERE empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY id_encomenda DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar encomendas' })
  }
})

// GET /api/encomendas/resumo?viagem_id=X
router.get('/resumo', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT COUNT(*) AS total,
             COALESCE(SUM(total_a_pagar), 0) AS valor_total,
             COALESCE(SUM(valor_pago), 0) AS valor_pago
      FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2
    `, [viagem_id, empresaId])
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar resumo' })
  }
})

// POST /api/encomendas
router.post('/', validate({ id_viagem: 'required|integer', remetente: 'required|string', destinatario: 'required|string', total_a_pagar: 'required|number' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      id_viagem, remetente, destinatario, observacoes, total_volumes,
      total_a_pagar, valor_pago, desconto, forma_pagamento, rota, id_caixa, local_pagamento, itens
    } = req.body
    if (!id_viagem || !remetente || !destinatario || !total_a_pagar) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, remetente, destinatario, total_a_pagar' })
    }

    await client.query('BEGIN')

    // #DB128: Advisory lock to prevent race condition on numero_encomenda MAX+1
    await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])

    const seqResult = await client.query(
      'SELECT COALESCE(MAX(numero_encomenda), 0) + 1 AS next_num FROM encomendas WHERE empresa_id = $1',
      [empresaId]
    )
    const numEncomenda = seqResult.rows[0].next_num

    const vPagar = parseFloat(total_a_pagar) || 0
    const vPago = parseFloat(valor_pago) || 0
    const vDesconto = parseFloat(desconto) || 0
    const status = vPago >= (vPagar - vDesconto) ? 'PAGO' : 'PENDENTE'

    const result = await client.query(`
      INSERT INTO encomendas (id_viagem, numero_encomenda, remetente, destinatario, observacoes,
        total_volumes, total_a_pagar, valor_pago, desconto, status_pagamento,
        forma_pagamento, entregue, rota, data_lancamento, local_pagamento, id_caixa, empresa_id)
      VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,FALSE,$12,CURRENT_DATE,$13,$14,$15)
      RETURNING *
    `, [
      id_viagem, numEncomenda, remetente, destinatario, observacoes || null,
      total_volumes || 0, vPagar, vPago, vDesconto, status,
      forma_pagamento || null, rota || null, local_pagamento || null, id_caixa ? parseInt(id_caixa) : null, empresaId
    ])

    const encomendaId = result.rows[0].id_encomenda
    if (itens && Array.isArray(itens)) {
      for (const item of itens) {
        await client.query(`
          INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total, local_armazenamento)
          VALUES ($1, $2, $3, $4, $5, $6)
        `, [encomendaId, item.quantidade || 1, item.descricao, item.valor_unitario || 0, item.valor_total || 0, item.local_armazenamento || null])
      }
    }

    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Encomendas] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar encomenda' })
  } finally {
    client.release()
  }
})

// PUT /api/encomendas/:id
router.put('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { remetente, destinatario, observacoes, rota, total_volumes } = req.body
    const result = await pool.query(`
      UPDATE encomendas SET remetente = COALESCE($1, remetente), destinatario = COALESCE($2, destinatario),
        observacoes = COALESCE($3, observacoes), rota = COALESCE($4, rota), total_volumes = COALESCE($5, total_volumes)
      WHERE id_encomenda = $6 AND empresa_id = $7
      RETURNING *
    `, [remetente, destinatario, observacoes, rota, total_volumes, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Encomenda nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Encomendas] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar encomenda' })
  }
})

// PUT /api/encomendas/:id/entregar
router.put('/:id/entregar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { doc_recebedor, nome_recebedor } = req.body
    const result = await pool.query(`
      UPDATE encomendas SET entregue = TRUE, doc_recebedor = $1, nome_recebedor = $2
      WHERE id_encomenda = $3 AND empresa_id = $4
      RETURNING *
    `, [doc_recebedor || null, nome_recebedor || null, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Encomenda nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Encomendas] Erro ao entregar:', err.message)
    res.status(500).json({ error: 'Erro ao marcar entrega' })
  }
})

// POST /api/encomendas/:id/pagar
router.post('/:id/pagar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { valor_pago } = req.body
    if (!valor_pago || valor_pago <= 0) {
      return res.status(400).json({ error: 'valor_pago obrigatorio e deve ser positivo' })
    }
    const result = await pool.query(`
      UPDATE encomendas SET valor_pago = valor_pago + $1,
        status_pagamento = CASE WHEN (valor_pago + $1) >= (total_a_pagar - COALESCE(desconto, 0)) THEN 'PAGO' ELSE 'PARCIAL' END
      WHERE id_encomenda = $2 AND empresa_id = $3
      RETURNING *
    `, [valor_pago, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Encomenda nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Encomendas] Erro ao pagar:', err.message)
    res.status(500).json({ error: 'Erro ao registrar pagamento' })
  }
})

// DELETE /api/encomendas/:id
router.delete('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    await client.query('BEGIN')
    // Verify tenant ownership BEFORE deleting items
    const check = await client.query(
      'SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (check.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Encomenda nao encontrada' })
    }
    await client.query(
      'DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    const result = await client.query(
      'DELETE FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2 RETURNING id_encomenda',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
    res.json({ mensagem: 'Encomenda excluida' })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Encomendas] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir encomenda' })
  } finally {
    client.release()
  }
})

export default router
