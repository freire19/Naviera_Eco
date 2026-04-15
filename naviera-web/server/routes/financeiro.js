import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'

const router = Router()
router.use(authMiddleware)

// GET /api/financeiro/entradas?viagem_id=X
// Nota: entradas sao calculadas a partir de passagens+encomendas+fretes (nao tem tabela propria)
router.get('/entradas', async (req, res) => {
  try {
    const { viagem_id } = req.query
    if (!viagem_id) return res.json([])
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT 'Passagens' AS tipo, COUNT(*) AS qtd, COALESCE(SUM(valor_pago), 0) AS valor FROM passagens WHERE id_viagem = $1 AND empresa_id = $2
      UNION ALL
      SELECT 'Encomendas', COUNT(*), COALESCE(SUM(valor_pago), 0) FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2
      UNION ALL
      SELECT 'Fretes', COUNT(*), COALESCE(SUM(valor_pago), 0) FROM fretes WHERE id_viagem = $1 AND empresa_id = $2
    `, [viagem_id, empresaId])
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
    let sql = 'SELECT id, id_viagem, descricao, valor_total, valor_pago, data_vencimento, data_pagamento, status, forma_pagamento, id_categoria, is_excluido, motivo_exclusao, funcionario_id, numero_parcela, total_parcelas, observacoes FROM financeiro_saidas WHERE (is_excluido = FALSE OR is_excluido IS NULL) AND empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY data_vencimento DESC'
    // DP052: LIMIT para evitar datasets ilimitados
    const limit = Math.min(parseInt(req.query.limit) || 500, 1000)
    const offset = parseInt(req.query.offset) || 0
    sql += ` LIMIT ${limit} OFFSET ${offset}`
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
      pool.query('SELECT COALESCE(SUM(valor_total), 0) AS total FROM financeiro_saidas WHERE id_viagem = $1 AND (is_excluido = FALSE OR is_excluido IS NULL) AND empresa_id = $2', [viagem_id, empresaId])
    ])

    const receitas = {
      passagens: Number(passagens.rows[0].total) || 0,
      encomendas: Number(encomendas.rows[0].total) || 0,
      fretes: Number(fretes.rows[0].total) || 0
    }
    // Aritmetica em centavos para evitar erros IEEE 754
    const totalReceitas = Math.round((receitas.passagens + receitas.encomendas + receitas.fretes) * 100) / 100
    // #DB131: apply same rounding to totalDespesas and saldo
    const totalDespesas = Math.round((Number(saidas.rows[0].total) || 0) * 100) / 100
    const saldo = Math.round((totalReceitas - totalDespesas) * 100) / 100

    res.json({
      receitas,
      totalReceitas,
      totalDespesas,
      saldo
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao calcular balanco' })
  }
})

// GET /api/financeiro/passagens?viagem_id=X&data_inicio=...&data_fim=...
router.get('/passagens', async (req, res) => {
  try {
    const { viagem_id, data_inicio, data_fim } = req.query
    if (!viagem_id) return res.json([])
    const empresaId = req.user.empresa_id
    let sql = `
      SELECT p.id_passagem, p.numero_bilhete, pas.nome_passageiro, p.valor_total, p.valor_pago,
             p.valor_devedor, p.status_passagem, p.data_emissao
      FROM passagens p
      LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
      WHERE p.id_viagem = $1 AND p.empresa_id = $2
    `
    const params = [viagem_id, empresaId]
    if (data_inicio) { params.push(data_inicio); sql += ` AND p.data_emissao >= $${params.length}` }
    if (data_fim) { params.push(data_fim); sql += ` AND p.data_emissao <= $${params.length}` }
    sql += ' ORDER BY p.data_emissao DESC, p.numero_bilhete DESC LIMIT 1000'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar passagens financeiro' })
  }
})

// GET /api/financeiro/encomendas?viagem_id=X&data_inicio=...&data_fim=...
router.get('/encomendas', async (req, res) => {
  try {
    const { viagem_id, data_inicio, data_fim } = req.query
    if (!viagem_id) return res.json([])
    const empresaId = req.user.empresa_id
    let sql = `
      SELECT id_encomenda, numero_encomenda, remetente, destinatario,
             total_a_pagar, valor_pago, status_pagamento, data_emissao, entregue
      FROM encomendas
      WHERE id_viagem = $1 AND empresa_id = $2
    `
    const params = [viagem_id, empresaId]
    if (data_inicio) { params.push(data_inicio); sql += ` AND data_emissao >= $${params.length}` }
    if (data_fim) { params.push(data_fim); sql += ` AND data_emissao <= $${params.length}` }
    sql += ' ORDER BY data_emissao DESC LIMIT 1000'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar encomendas financeiro' })
  }
})

// GET /api/financeiro/fretes?viagem_id=X&data_inicio=...&data_fim=...
router.get('/fretes', async (req, res) => {
  try {
    const { viagem_id, data_inicio, data_fim } = req.query
    if (!viagem_id) return res.json([])
    const empresaId = req.user.empresa_id
    let sql = `
      SELECT id_frete, numero_frete, remetente_nome_temp, destinatario_nome_temp,
             valor_nominal, valor_pago, status, data_emissao
      FROM fretes
      WHERE id_viagem = $1 AND empresa_id = $2
    `
    const params = [viagem_id, empresaId]
    if (data_inicio) { params.push(data_inicio); sql += ` AND data_emissao >= $${params.length}` }
    if (data_fim) { params.push(data_fim); sql += ` AND data_emissao <= $${params.length}` }
    sql += ' ORDER BY data_emissao DESC LIMIT 1000'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar fretes financeiro' })
  }
})

// POST /api/financeiro/saida
router.post('/saida', validate({ descricao: 'required|string', valor_total: 'required|number' }), async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const {
      id_viagem, descricao, valor_total, data_vencimento, id_categoria, id_funcionario, forma_pagamento,
      valor_pago, data_pagamento, status, numero_parcela, total_parcelas, observacoes
    } = req.body
    if (!descricao || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: descricao, valor_total' })
    }
    const result = await pool.query(`
      INSERT INTO financeiro_saidas (id_viagem, descricao, valor_total, data_vencimento, id_categoria, funcionario_id, forma_pagamento,
        valor_pago, data_pagamento, status, numero_parcela, total_parcelas, observacoes, is_excluido, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, FALSE, $14)
      RETURNING *
    `, [
      id_viagem || null, descricao, valor_total, data_vencimento || null, id_categoria || null, id_funcionario || null, forma_pagamento || null,
      parseFloat(valor_pago) || 0, data_pagamento || null, status || 'PENDENTE',
      numero_parcela ? parseInt(numero_parcela) : null, total_parcelas ? parseInt(total_parcelas) : null, observacoes || null,
      empresaId
    ])
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
      'UPDATE financeiro_saidas SET is_excluido = TRUE, motivo_exclusao = $1 WHERE id = $2 AND empresa_id = $3 RETURNING id',
      [motivo || null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Saida nao encontrada' })
    res.json({ mensagem: 'Saida excluida' })
  } catch (err) {
    console.error('[Financeiro] Erro ao excluir saida:', err.message)
    res.status(500).json({ error: 'Erro ao excluir saida' })
  }
})

// ============================================================
// BOLETOS
// ============================================================

// GET /api/financeiro/boletos?viagem_id=X&status=PENDENTE
router.get('/boletos', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { viagem_id, status } = req.query
    let sql = `SELECT * FROM financeiro_saidas WHERE (is_excluido = FALSE OR is_excluido IS NULL) AND forma_pagamento = 'BOLETO' AND empresa_id = $1`
    const params = [empresaId]
    let idx = 2
    if (viagem_id) {
      sql += ` AND id_viagem = $${idx++}`
      params.push(viagem_id)
    }
    if (status && status !== 'TODOS') {
      sql += ` AND status = $${idx++}`
      params.push(status)
    }
    sql += ' ORDER BY data_vencimento ASC LIMIT 500'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Financeiro] Erro ao listar boletos:', err.message)
    res.status(500).json({ error: 'Erro ao listar boletos' })
  }
})

// POST /api/financeiro/boleto — Create single boleto
router.post('/boleto', validate({ descricao: 'required|string', valor_total: 'required|number' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      descricao, valor_total, data_vencimento, id_categoria, id_viagem,
      numero_parcela, total_parcelas, observacoes
    } = req.body
    if (!descricao || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: descricao, valor_total' })
    }

    await client.query('BEGIN')

    const result = await client.query(`
      INSERT INTO financeiro_saidas (id_viagem, descricao, valor_total, data_vencimento, id_categoria, forma_pagamento,
        valor_pago, status, numero_parcela, total_parcelas, observacoes, is_excluido, empresa_id)
      VALUES ($1, $2, $3, $4, $5, 'BOLETO', 0, 'PENDENTE', $6, $7, $8, FALSE, $9)
      RETURNING *
    `, [
      id_viagem || null, descricao, valor_total, data_vencimento || null, id_categoria || null,
      numero_parcela ? parseInt(numero_parcela) : null, total_parcelas ? parseInt(total_parcelas) : null,
      observacoes || null, empresaId
    ])

    // Create agenda entry
    const dataEvento = data_vencimento || new Date().toISOString().split('T')[0]
    await client.query(
      'INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES ($1, $2, FALSE, $3)',
      [dataEvento, `Boleto: ${descricao} - R$ ${parseFloat(valor_total).toFixed(2)}`, empresaId]
    )

    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Financeiro] Erro ao criar boleto:', err.message)
    res.status(500).json({ error: 'Erro ao criar boleto' })
  } finally {
    client.release()
  }
})

// POST /api/financeiro/boleto/batch — Create multiple boletos (parcelas)
router.post('/boleto/batch', validate({ descricao_base: 'required|string', valor_total: 'required|number', parcelas: 'required|integer' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      descricao_base, valor_total, parcelas, data_primeira_vencimento, intervalo_dias,
      id_categoria, id_viagem
    } = req.body
    if (!descricao_base || !valor_total || !parcelas || parcelas < 1) {
      return res.status(400).json({ error: 'Campos obrigatorios: descricao_base, valor_total, parcelas (>= 1)' })
    }
    if (parcelas > 120) return res.status(400).json({ error: 'Maximo de 120 parcelas permitido' })
    const valorParcela = Math.floor(valor_total * 100 / parcelas) / 100
    const valorUltimaParcela = Math.round((valor_total - valorParcela * (parcelas - 1)) * 100) / 100
    const intervalo = parseInt(intervalo_dias) || 30
    const dataBase = data_primeira_vencimento ? new Date(data_primeira_vencimento) : new Date()
    if (isNaN(dataBase.getTime())) return res.status(400).json({ error: 'Data de vencimento invalida' })
    const boletos = []

    await client.query('BEGIN')

    // DP055: batch inserts em vez de loop sequencial (era 2*N queries, agora 2)
    const saidasValues = []
    const saidasParams = []
    const agendaValues = []
    const agendaParams = []

    for (let i = 0; i < parcelas; i++) {
      const dataVenc = new Date(dataBase)
      dataVenc.setDate(dataVenc.getDate() + (i * intervalo))
      const dataStr = dataVenc.toISOString().split('T')[0]
      const descricao = `${descricao_base} (${i + 1}/${parcelas})`
      const valorEsta = (i === parcelas - 1) ? valorUltimaParcela : valorParcela

      const sOff = i * 8
      saidasValues.push(`($${sOff+1}, $${sOff+2}, $${sOff+3}, $${sOff+4}, $${sOff+5}, 'BOLETO', 0, 'PENDENTE', $${sOff+6}, $${sOff+7}, NULL, FALSE, $${sOff+8})`)
      saidasParams.push(id_viagem || null, descricao, valorEsta, dataStr, id_categoria || null, i + 1, parseInt(parcelas), empresaId)

      const aOff = i * 3
      agendaValues.push(`($${aOff+1}, $${aOff+2}, FALSE, $${aOff+3})`)
      agendaParams.push(dataStr, `Boleto: ${descricao} - R$ ${valorEsta.toFixed(2)}`, empresaId)
    }

    const saidasResult = await client.query(
      `INSERT INTO financeiro_saidas (id_viagem, descricao, valor_total, data_vencimento, id_categoria, forma_pagamento,
        valor_pago, status, numero_parcela, total_parcelas, observacoes, is_excluido, empresa_id)
      VALUES ${saidasValues.join(', ')} RETURNING *`,
      saidasParams
    )
    boletos.push(...saidasResult.rows)

    await client.query(
      `INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES ${agendaValues.join(', ')}`,
      agendaParams
    )

    await client.query('COMMIT')
    res.status(201).json(boletos)
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Financeiro] Erro ao criar boletos em lote:', err.message)
    res.status(500).json({ error: 'Erro ao criar boletos em lote' })
  } finally {
    client.release()
  }
})

// PUT /api/financeiro/boleto/:id/baixa — Mark boleto as paid
router.put('/boleto/:id/baixa', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { forma_pagamento } = req.body
    const result = await pool.query(`
      UPDATE financeiro_saidas
      SET status = 'PAGO', data_pagamento = CURRENT_DATE, valor_pago = valor_total,
          forma_pagamento = COALESCE($1, forma_pagamento)
      WHERE id = $2 AND empresa_id = $3 AND forma_pagamento = 'BOLETO'
      RETURNING *
    `, [forma_pagamento || 'BOLETO', req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Boleto nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Financeiro] Erro ao dar baixa no boleto:', err.message)
    res.status(500).json({ error: 'Erro ao dar baixa no boleto' })
  }
})

export default router
