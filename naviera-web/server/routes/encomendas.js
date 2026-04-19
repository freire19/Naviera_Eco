import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'
import { validarAutorizador } from '../utils/validarAutorizador.js'

const router = Router()
router.use(authMiddleware)

// GET /api/encomendas?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = `SELECT e.id_encomenda, e.id_viagem, e.numero_encomenda, e.remetente, e.destinatario,
      e.observacoes, e.total_volumes, e.total_a_pagar, e.valor_pago, e.desconto, e.status_pagamento,
      e.entregue, e.forma_pagamento, e.local_pagamento, e.doc_recebedor, e.nome_recebedor, e.rota,
      e.id_caixa, e.data_lancamento,
      TO_CHAR(v.data_viagem, 'DD/MM/YYYY') AS data_viagem,
      TO_CHAR(v.data_chegada, 'DD/MM/YYYY') AS data_chegada
      FROM encomendas e
      LEFT JOIN viagens v ON e.id_viagem = v.id_viagem
      WHERE e.empresa_id = $1`
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND e.id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY e.id_encomenda DESC'
    // DP052: LIMIT para evitar datasets ilimitados
    const limit = Math.min(parseInt(req.query.limit) || 500, 1000)
    const offset = parseInt(req.query.offset) || 0
    sql += ` LIMIT ${limit} OFFSET ${offset}`
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

// GET /api/encomendas/proximo-numero
router.get('/proximo-numero', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      `SELECT COALESCE(MAX(CASE WHEN numero_encomenda ~ '^[0-9]+$' THEN numero_encomenda::bigint ELSE 0 END), 0) + 1 AS next_num FROM encomendas WHERE empresa_id = $1`,
      [empresaId]
    )
    res.json({ numero: String(result.rows[0].next_num) })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar numero' })
  }
})

// GET /api/encomendas/:id/itens
router.get('/:id/itens', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM encomenda_itens WHERE id_encomenda = $1 AND (excluido = FALSE OR excluido IS NULL) ORDER BY id_item_encomenda',
      [req.params.id]
    )
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens' })
  }
})

// POST /api/encomendas
router.post('/', validate({ id_viagem: 'required|integer', destinatario: 'required|string' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      id_viagem, remetente, destinatario, observacoes, total_volumes,
      total_a_pagar, valor_pago, desconto, forma_pagamento, rota, id_caixa, local_pagamento, itens
    } = req.body
    if (!id_viagem || !destinatario) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, remetente, destinatario, total_a_pagar' })
    }

    await client.query('BEGIN')

    // #DB128: Advisory lock to prevent race condition on numero_encomenda MAX+1
    await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])

    const seqResult = await client.query(
      `SELECT COALESCE(MAX(CASE WHEN numero_encomenda ~ '^[0-9]+$' THEN numero_encomenda::bigint ELSE 0 END), 0) + 1 AS next_num FROM encomendas WHERE empresa_id = $1`,
      [empresaId]
    )
    const numEncomenda = String(seqResult.rows[0].next_num)

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
      id_viagem, numEncomenda, (remetente || '').toUpperCase() || null, (destinatario || '').toUpperCase() || null, observacoes || null,
      total_volumes || 0, vPagar, vPago, vDesconto, status,
      forma_pagamento || null, rota || null, local_pagamento || null, id_caixa ? parseInt(id_caixa) : null, empresaId
    ])

    const encomendaId = result.rows[0].id_encomenda
    // DP054: batch insert em vez de loop individual
    if (itens && Array.isArray(itens) && itens.length > 0) {
      const values = []
      const params = []
      itens.forEach((item, i) => {
        const off = i * 6
        values.push(`($${off+1}, $${off+2}, $${off+3}, $${off+4}, $${off+5}, $${off+6})`)
        params.push(encomendaId, item.quantidade || 1, (item.descricao || '').toUpperCase(), item.valor_unitario || 0, item.valor_total || 0, item.local_armazenamento || null)
      })
      await client.query(
        `INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total, local_armazenamento) VALUES ${values.join(', ')}`,
        params
      )
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

// PUT /api/encomendas/:id — atualizar encomenda completa com itens
router.put('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { remetente, destinatario, observacoes, rota, total_volumes, total_a_pagar, itens } = req.body

    await client.query('BEGIN')
    const result = await client.query(`
      UPDATE encomendas SET remetente = $1, destinatario = $2,
        observacoes = $3, rota = $4, total_volumes = $5, total_a_pagar = $6
      WHERE id_encomenda = $7 AND empresa_id = $8
      RETURNING *
    `, [(remetente || '').toUpperCase() || null, (destinatario || '').toUpperCase() || null, observacoes || null, rota || null,
        total_volumes || 0, parseFloat(total_a_pagar) || 0, req.params.id, empresaId])

    if (result.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Encomenda nao encontrada' })
    }

    // Substituir itens se fornecidos
    if (itens && Array.isArray(itens)) {
      await client.query('DELETE FROM encomenda_itens WHERE id_encomenda = $1', [req.params.id])
      if (itens.length > 0) {
        const values = []
        const params = []
        itens.forEach((item, i) => {
          const off = i * 6
          values.push(`($${off+1}, $${off+2}, $${off+3}, $${off+4}, $${off+5}, $${off+6})`)
          params.push(req.params.id, item.quantidade || 1, (item.descricao || '').toUpperCase(), item.valor_unitario || 0, item.valor_total || 0, item.local_armazenamento || null)
        })
        await client.query(`INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total, local_armazenamento) VALUES ${values.join(', ')}`, params)
      }
    }

    await client.query('COMMIT')
    res.json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Encomendas] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar encomenda' })
  } finally {
    client.release()
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
    // DS4-011 fix: guarda contra overpayment — so aceita se valor_devedor >= pagamento
    const result = await pool.query(`
      UPDATE encomendas SET valor_pago = valor_pago + $1,
        status_pagamento = CASE WHEN (valor_pago + $1) >= (total_a_pagar - COALESCE(desconto, 0)) THEN 'PAGO' ELSE 'PARCIAL' END
      WHERE id_encomenda = $2 AND empresa_id = $3
        AND (total_a_pagar - COALESCE(desconto, 0) - valor_pago) >= $1
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
// Exige senha de um usuario autorizador + motivo. Grava log em
// log_estornos_encomendas com tipo_operacao='EXCLUSAO' antes de deletar.
router.delete('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { login_autorizador, senha_autorizador, motivo } = req.body || {}
    if (!login_autorizador || !senha_autorizador) {
      return res.status(400).json({ error: 'Login e senha do autorizador obrigatorios' })
    }
    if (!motivo || !motivo.trim()) {
      return res.status(400).json({ error: 'Motivo da exclusao obrigatorio' })
    }
    const autorizador = await validarAutorizador(login_autorizador, senha_autorizador, empresaId)
    if (!autorizador) return res.status(403).json({ error: 'Senha do autorizador invalida' })

    await client.query('BEGIN')

    const check = await client.query(
      'SELECT id_encomenda, valor_pago FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (check.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Encomenda nao encontrada' })
    }
    const valorEstornado = parseFloat(check.rows[0].valor_pago) || 0

    await client.query(
      `INSERT INTO log_estornos_encomendas
       (id_encomenda, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador, data_hora, empresa_id, tipo_operacao)
       VALUES ($1, $2, $3, NULL, $4, $5, NOW(), $6, 'EXCLUSAO')`,
      [req.params.id, valorEstornado, motivo.trim(), autorizador.id, autorizador.nome, empresaId]
    )

    await client.query(
      'DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
    res.json({ mensagem: 'Encomenda excluida', autorizador: autorizador.nome })
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {})
    console.error('[Encomendas] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir encomenda' })
  } finally {
    client.release()
  }
})

export default router
