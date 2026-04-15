import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'

const router = Router()
router.use(authMiddleware)

// GET /api/passagens/busca-passageiro?q=jon — Search passageiros by name (ILIKE)
router.get('/busca-passageiro', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { q } = req.query
    if (!q || q.trim().length < 2) return res.json([])
    const result = await pool.query(
      `SELECT p.id_passageiro, p.nome_passageiro, p.numero_documento,
              p.data_nascimento, p.id_tipo_doc, p.id_sexo, p.id_nacionalidade
       FROM passageiros p
       WHERE p.empresa_id = $1 AND p.nome_passageiro ILIKE $2
       ORDER BY p.nome_passageiro
       LIMIT 15`,
      // DS4-041 fix: escapar wildcards LIKE do input
      [empresaId, `%${q.trim().replace(/%/g, '\\%').replace(/_/g, '\\_')}%`]
    )
    res.json(result.rows)
  } catch (err) {
    console.error('[Passagens] Erro busca passageiro:', err.message)
    res.status(500).json({ error: 'Erro ao buscar passageiros' })
  }
})

// GET /api/passagens?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = `
      SELECT p.*,
             pas.nome_passageiro, pas.numero_documento AS numero_doc,
             pas.data_nascimento, pas.id_nacionalidade AS pas_id_nacionalidade,
             nac.nome_nacionalidade,
             r.origem, r.destino,
             hs.descricao_horario_saida,
             ac.nome_acomodacao,
             ag.nome_agente,
             COALESCE(tpas.nome, tp.nome_tipo_passagem) AS nome_tipo_passagem
      FROM passagens p
      LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro AND pas.empresa_id = p.empresa_id
      LEFT JOIN aux_nacionalidades nac ON pas.id_nacionalidade = nac.id_nacionalidade
      LEFT JOIN rotas r ON p.id_rota = r.id
      LEFT JOIN aux_horarios_saida hs ON p.id_horario_saida = hs.id_horario_saida
      LEFT JOIN aux_acomodacoes ac ON p.id_acomodacao = ac.id_acomodacao
      LEFT JOIN aux_agentes ag ON p.id_agente = ag.id_agente
      LEFT JOIN tipo_passageiro tpas ON p.id_tipo_passagem = tpas.id AND tpas.empresa_id = p.empresa_id
      LEFT JOIN aux_tipos_passagem tp ON p.id_tipo_passagem = tp.id_tipo_passagem
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
router.post('/', validate({ id_viagem: 'required|integer', valor_total: 'required|number' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      id_passageiro, nome_passageiro, documento, id_viagem, assento, id_rota, id_tipo_passagem, id_acomodacao,
      id_caixa, valor_total, valor_pago, observacoes,
      valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao,
      id_agente, numero_requisicao, valor_alimentacao, valor_transporte, valor_cargas,
      valor_desconto_tarifa, valor_desconto_geral, troco, id_horario_saida,
      data_nascimento, id_tipo_doc, id_sexo, id_nacionalidade
    } = req.body
    if (!id_viagem || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, valor_total' })
    }
    if (!id_passageiro && !nome_passageiro) {
      return res.status(400).json({ error: 'Informe id_passageiro ou nome_passageiro' })
    }

    await client.query('BEGIN')

    // #DB127: Advisory lock to prevent race condition on numero_bilhete MAX+1
    await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])

    // Se nao tem id_passageiro, busca ou cria pelo nome
    let passageiroId = id_passageiro
    if (!passageiroId && nome_passageiro) {
      const busca = await client.query(
        'SELECT id_passageiro FROM passageiros WHERE LOWER(nome_passageiro) = LOWER($1) AND empresa_id = $2 LIMIT 1',
        [nome_passageiro.trim(), empresaId]
      )
      if (busca.rows.length > 0) {
        passageiroId = busca.rows[0].id_passageiro
        // Atualizar dados do passageiro existente se informados
        if (data_nascimento || id_tipo_doc || id_sexo || id_nacionalidade || documento) {
          await client.query(
            `UPDATE passageiros SET
              numero_documento = COALESCE($1, numero_documento),
              data_nascimento = COALESCE($2, data_nascimento),
              id_tipo_doc = COALESCE($3, id_tipo_doc),
              id_sexo = COALESCE($4, id_sexo),
              id_nacionalidade = COALESCE($5, id_nacionalidade),
              data_ultima_atualizacao = NOW()
            WHERE id_passageiro = $6 AND empresa_id = $7`,
            [documento || null, data_nascimento || null, id_tipo_doc ? parseInt(id_tipo_doc) : null,
             id_sexo ? parseInt(id_sexo) : null, id_nacionalidade ? parseInt(id_nacionalidade) : null,
             passageiroId, empresaId]
          )
        }
      } else {
        const novo = await client.query(
          `INSERT INTO passageiros (nome_passageiro, numero_documento, data_nascimento, id_tipo_doc, id_sexo, id_nacionalidade, empresa_id)
           VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id_passageiro`,
          [nome_passageiro.trim(), documento || null, data_nascimento || null,
           id_tipo_doc ? parseInt(id_tipo_doc) : null, id_sexo ? parseInt(id_sexo) : null,
           id_nacionalidade ? parseInt(id_nacionalidade) : null, empresaId]
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
    // Aritmetica em centavos para evitar erros de IEEE 754
    const vDevedor = Math.round((vTotal - vPago) * 100) / 100
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
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { valor_pago, valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao, id_caixa } = req.body
    if (!valor_pago || valor_pago <= 0) {
      return res.status(400).json({ error: 'valor_pago obrigatorio e deve ser positivo' })
    }

    await client.query('BEGIN')

    // #DB129: WHERE valor_devedor >= $1 prevents overpayment (negative devedor)
    const result = await client.query(`
      UPDATE passagens SET valor_pago = valor_pago + $1,
        valor_devedor = valor_devedor - $1,
        status_passagem = CASE WHEN (valor_devedor - $1) <= 0.01 THEN 'PAGO' ELSE 'PARCIAL' END,
        valor_pagamento_dinheiro = COALESCE(valor_pagamento_dinheiro, 0) + $4,
        valor_pagamento_pix = COALESCE(valor_pagamento_pix, 0) + $5,
        valor_pagamento_cartao = COALESCE(valor_pagamento_cartao, 0) + $6,
        id_caixa = COALESCE($7, id_caixa)
      WHERE id_passagem = $2 AND empresa_id = $3 AND valor_devedor >= $1
      RETURNING *
    `, [valor_pago, req.params.id, empresaId,
        parseFloat(valor_pagamento_dinheiro) || 0,
        parseFloat(valor_pagamento_pix) || 0,
        parseFloat(valor_pagamento_cartao) || 0,
        id_caixa ? parseInt(id_caixa) : null])

    if (result.rows.length === 0) {
      // Distinguish between not-found and overpayment
      const check = await client.query(
        'SELECT id_passagem, valor_devedor FROM passagens WHERE id_passagem = $1 AND empresa_id = $2',
        [req.params.id, empresaId]
      )
      await client.query('ROLLBACK')
      if (check.rows.length === 0) return res.status(404).json({ error: 'Passagem nao encontrada' })
      return res.status(400).json({ error: `Valor de pagamento (${valor_pago}) excede o valor devedor (${check.rows[0].valor_devedor})` })
    }

    await client.query('COMMIT')
    res.json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Passagens] Erro ao pagar:', err.message)
    res.status(500).json({ error: 'Erro ao registrar pagamento' })
  } finally {
    client.release()
  }
})

// DELETE /api/passagens/:id
// DS4-032 fix: somente Administrador/Gerente pode deletar
router.delete('/:id', async (req, res) => {
  const funcao = (req.user.funcao || '').toLowerCase()
  if (funcao !== 'administrador' && funcao !== 'admin' && funcao !== 'gerente') {
    return res.status(403).json({ error: 'Somente Administrador ou Gerente pode excluir passagens' })
  }
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
