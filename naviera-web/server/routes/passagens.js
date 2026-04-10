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
      SELECT p.*, pas.nome_passageiro, pas.numero_documento AS numero_doc
      FROM passagens p
      LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
      WHERE p.empresa_id = $1
    `
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND p.id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY p.numero_bilhete DESC'

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
      id_passageiro, nome_passageiro, documento, id_viagem, assento, id_rota, id_tipo_passagem, id_acomodacao,
      id_caixa, valor_total, valor_pago, observacoes,
      valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao,
      id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas,
      valor_desconto_tarifa, valor_desconto_geral, troco, id_horario_saida
    } = req.body
    if (!id_viagem || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, valor_total' })
    }
    if (!id_passageiro && !nome_passageiro) {
      return res.status(400).json({ error: 'Informe id_passageiro ou nome_passageiro' })
    }

    await client.query('BEGIN')

    // Se nao tem id_passageiro, busca ou cria pelo nome
    let passageiroId = id_passageiro
    if (!passageiroId && nome_passageiro) {
      const busca = await client.query(
        'SELECT id_passageiro FROM passageiros WHERE LOWER(nome_passageiro) = LOWER($1) AND empresa_id = $2 LIMIT 1',
        [nome_passageiro.trim(), empresaId]
      )
      if (busca.rows.length > 0) {
        passageiroId = busca.rows[0].id_passageiro
      } else {
        const novo = await client.query(
          'INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id) VALUES ($1, $2, $3) RETURNING id_passageiro',
          [nome_passageiro.trim(), documento || null, empresaId]
        )
        passageiroId = novo.rows[0].id_passageiro
      }
    }

    // Auto-sequence numero_bilhete
    const seqResult = await client.query(
      'SELECT COALESCE(MAX(numero_bilhete::bigint), 0) + 1 AS next_num FROM passagens WHERE empresa_id = $1',
      [empresaId]
    )
    const numBilhete = seqResult.rows[0].next_num

    const vTotal = parseFloat(valor_total) || 0
    const vPago = parseFloat(valor_pago) || 0
    const vDevedor = vTotal - vPago
    const status = vDevedor <= 0.01 ? 'PAGO' : 'PENDENTE'

    const params = [
      String(numBilhete), parseInt(passageiroId), parseInt(id_viagem), assento || null,
      id_rota ? parseInt(id_rota) : null, id_tipo_passagem ? parseInt(id_tipo_passagem) : null, id_acomodacao ? parseInt(id_acomodacao) : null,
      id_caixa ? parseInt(id_caixa) : null, parseInt(req.user.id),
      vTotal, vTotal, vPago, vDevedor, status,
      observacoes || null,
      parseFloat(valor_pagamento_dinheiro) || 0, parseFloat(valor_pagamento_pix) || 0, parseFloat(valor_pagamento_cartao) || 0,
      parseInt(empresaId),
      id_agente ? parseInt(id_agente) : null, numero_requisicao || null,
      parseFloat(valor_alimentacao) || 0, parseFloat(valor_transporte) || 0, parseFloat(valor_cargas) || 0,
      parseFloat(valor_desconto_tarifa) || 0, parseFloat(valor_desconto_geral) || 0, parseFloat(troco) || 0,
      id_horario_saida ? parseInt(id_horario_saida) : null
    ]
    const result = await client.query(`
      INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao, assento,
        id_rota, id_tipo_passagem, id_acomodacao, id_caixa, id_usuario_emissor,
        valor_total, valor_a_pagar, valor_pago, valor_devedor, status_passagem,
        observacoes, valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao, empresa_id,
        id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas,
        valor_desconto_tarifa, valor_desconto_geral, troco, id_horario_saida)
      VALUES ($1,$2,$3,CURRENT_DATE,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22,$23,$24,$25,$26,$27,$28)
      RETURNING *
    `, params)

    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Passagens] Erro ao criar:', err.message, err.stack?.split('\n')[1])
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
    `, [assento || null, observacoes || null, id_acomodacao ? parseInt(id_acomodacao) : null, id_rota ? parseInt(id_rota) : null, parseInt(req.params.id), parseInt(empresaId)])
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
