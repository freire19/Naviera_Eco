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
    const empresaId = req.user.empresa_id
    const { viagem_id, categoria, forma_pagto, data_especifica } = req.query
    let sql = `SELECT s.id, s.id_viagem, s.descricao, s.valor_total, s.valor_pago, s.data_vencimento, s.data_pagamento, s.status, s.forma_pagamento, s.id_categoria, s.is_excluido, s.motivo_exclusao, s.funcionario_id, s.numero_parcela, s.total_parcelas, s.observacoes, COALESCE(c.nome, '') AS categoria_nome FROM financeiro_saidas s LEFT JOIN categorias_despesa c ON s.id_categoria = c.id WHERE (s.is_excluido = FALSE OR s.is_excluido IS NULL) AND s.empresa_id = $1`
    const params = [empresaId]
    if (viagem_id) { sql += ` AND s.id_viagem = $${params.length + 1}`; params.push(viagem_id) }
    if (categoria && categoria !== 'Todas') { sql += ` AND c.nome = $${params.length + 1}`; params.push(categoria) }
    if (forma_pagto && forma_pagto !== 'Todas') { sql += ` AND s.forma_pagamento = $${params.length + 1}`; params.push(forma_pagto) }
    if (data_especifica) { sql += ` AND s.data_vencimento = $${params.length + 1}`; params.push(data_especifica) }
    sql += ' ORDER BY s.data_vencimento DESC'
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
      pool.query('SELECT COALESCE(SUM(valor_total), 0) AS lancado, COALESCE(SUM(valor_pago), 0) AS recebido FROM passagens WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(total_a_pagar), 0) AS lancado, COALESCE(SUM(valor_pago), 0) AS recebido FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(valor_frete_calculado), 0) AS lancado, COALESCE(SUM(valor_pago), 0) AS recebido FROM fretes WHERE id_viagem = $1 AND empresa_id = $2', [viagem_id, empresaId]),
      pool.query('SELECT COALESCE(SUM(valor_total), 0) AS total FROM financeiro_saidas WHERE id_viagem = $1 AND (is_excluido = FALSE OR is_excluido IS NULL) AND empresa_id = $2', [viagem_id, empresaId])
    ])

    const receitas = {
      passagens: Number(passagens.rows[0].lancado) || 0,
      encomendas: Number(encomendas.rows[0].lancado) || 0,
      fretes: Number(fretes.rows[0].lancado) || 0
    }
    const recebido = {
      passagens: Number(passagens.rows[0].recebido) || 0,
      encomendas: Number(encomendas.rows[0].recebido) || 0,
      fretes: Number(fretes.rows[0].recebido) || 0
    }
    const totalReceitas = Math.round((receitas.passagens + receitas.encomendas + receitas.fretes) * 100) / 100
    const totalRecebido = Math.round((recebido.passagens + recebido.encomendas + recebido.fretes) * 100) / 100
    const totalDespesas = Math.round((Number(saidas.rows[0].total) || 0) * 100) / 100
    const saldo = Math.round((totalReceitas - totalDespesas) * 100) / 100

    res.json({
      receitas,
      recebido,
      totalReceitas,
      totalRecebido,
      totalDespesas,
      saldo
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao calcular balanco' })
  }
})

// GET /api/financeiro/dashboard — UNION de passagens+encomendas+fretes com filtros (igual desktop)
router.get('/dashboard', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { viagem_id, categoria, forma_pagto, caixa } = req.query

    let sql = `
      SELECT 'ENCOMENDA' AS origem, e.total_a_pagar AS total, e.valor_pago AS pago,
             COALESCE(e.forma_pagamento, 'PENDENTE') AS pgto, COALESCE(ue.nome, '') AS usuario
      FROM encomendas e
      LEFT JOIN usuarios ue ON e.id_caixa = ue.id
      WHERE e.empresa_id = $1 ${viagem_id ? 'AND e.id_viagem = $2' : ''}
      UNION ALL
      SELECT 'FRETE' AS origem, f.valor_frete_calculado AS total, f.valor_pago AS pago,
             COALESCE(f.tipo_pagamento, 'PENDENTE') AS pgto, COALESCE(f.nome_caixa, '') AS usuario
      FROM fretes f WHERE f.empresa_id = $1 ${viagem_id ? 'AND f.id_viagem = $2' : ''}
      UNION ALL
      SELECT 'PASSAGEM' AS origem, p.valor_total AS total, p.valor_pago AS pago,
             COALESCE(afp.nome_forma_pagamento, 'DINHEIRO') AS pgto, COALESCE(up.nome, 'SISTEMA') AS usuario
      FROM passagens p
      LEFT JOIN aux_formas_pagamento afp ON p.id_forma_pagamento = afp.id_forma_pagamento
      LEFT JOIN usuarios up ON p.id_usuario_emissor = up.id
      WHERE p.empresa_id = $1 ${viagem_id ? 'AND p.id_viagem = $2' : ''}
    `
    const params = viagem_id ? [empresaId, viagem_id] : [empresaId]
    const result = await pool.query(sql, params)

    // Filtrar em JS (como o desktop faz)
    let rows = result.rows
    if (categoria && categoria !== 'Todas') rows = rows.filter(r => r.origem === categoria.toUpperCase())
    if (forma_pagto && forma_pagto !== 'Todas') rows = rows.filter(r => (r.pgto || '').toUpperCase().includes(forma_pagto.toUpperCase()))
    if (caixa && caixa !== 'Todos') rows = rows.filter(r => (r.usuario || '').toUpperCase() === caixa.toUpperCase())

    // Agregar
    let totalGeral = 0, totalRecebido = 0
    let somaPassagem = 0, somaEncomenda = 0, somaFrete = 0
    let somaDinheiro = 0, somaPix = 0, somaCartao = 0

    for (const r of rows) {
      const t = parseFloat(r.total) || 0
      const p = parseFloat(r.pago) || 0
      totalGeral += t
      totalRecebido += p
      if (r.origem === 'PASSAGEM') somaPassagem += t
      if (r.origem === 'ENCOMENDA') somaEncomenda += t
      if (r.origem === 'FRETE') somaFrete += t
      // Formas de pagamento (so do recebido)
      if (p > 0) {
        const pgto = (r.pgto || '').toUpperCase()
        if (pgto.includes('PIX')) somaPix += p
        else if (pgto.includes('CART') || pgto.includes('CREDITO') || pgto.includes('DEBITO')) somaCartao += p
        else somaDinheiro += p
      }
    }

    res.json({
      totalGeral: Math.round(totalGeral * 100) / 100,
      totalRecebido: Math.round(totalRecebido * 100) / 100,
      pendente: Math.round((totalGeral - totalRecebido) * 100) / 100,
      categorias: { passagens: Math.round(somaPassagem * 100) / 100, encomendas: Math.round(somaEncomenda * 100) / 100, fretes: Math.round(somaFrete * 100) / 100 },
      formasPagamento: { dinheiro: Math.round(somaDinheiro * 100) / 100, pix: Math.round(somaPix * 100) / 100, cartao: Math.round(somaCartao * 100) / 100 },
      registros: rows.length
    })
  } catch (err) {
    console.error('[Financeiro] Erro dashboard:', err.message)
    res.status(500).json({ error: 'Erro ao calcular dashboard' })
  }
})

// GET /api/financeiro/caixas — lista usuarios/caixas
router.get('/caixas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT DISTINCT nome FROM usuarios WHERE empresa_id = $1 ORDER BY nome', [empresaId])
    res.json(result.rows.map(r => r.nome))
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar caixas' })
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
             total_a_pagar, valor_pago, status_pagamento, data_lancamento, entregue
      FROM encomendas
      WHERE id_viagem = $1 AND empresa_id = $2
    `
    const params = [viagem_id, empresaId]
    if (data_inicio) { params.push(data_inicio); sql += ` AND data_lancamento >= $${params.length}` }
    if (data_fim) { params.push(data_fim); sql += ` AND data_lancamento <= $${params.length}` }
    sql += ' ORDER BY numero_encomenda DESC LIMIT 1000'
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
      SELECT id_frete, numero_frete, remetente_nome_temp AS remetente, destinatario_nome_temp AS destinatario,
             valor_total_itens, valor_frete_calculado, valor_pago, valor_devedor, status_frete, data_emissao
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
// GET /api/financeiro/categorias — listar categorias de despesa
router.get('/categorias', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT id, nome FROM categorias_despesa WHERE empresa_id = $1 ORDER BY nome', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar categorias' })
  }
})

// POST /api/financeiro/categorias — criar nova categoria
router.post('/categorias', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO categorias_despesa (nome, empresa_id) VALUES ($1, $2) ON CONFLICT DO NOTHING RETURNING *',
      [nome.trim().toUpperCase(), empresaId]
    )
    if (result.rows.length > 0) return res.status(201).json(result.rows[0])
    const existing = await pool.query('SELECT * FROM categorias_despesa WHERE LOWER(nome) = LOWER($1) AND empresa_id = $2', [nome.trim(), empresaId])
    res.json(existing.rows[0] || {})
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar categoria' })
  }
})

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

// PUT /api/financeiro/saida/:id (editar boleto/saida)
router.put('/saida/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { descricao, valor_total, data_vencimento, observacoes, id_categoria } = req.body
    const result = await pool.query(`
      UPDATE financeiro_saidas SET
        descricao = COALESCE($1, descricao),
        valor_total = COALESCE($2, valor_total),
        data_vencimento = COALESCE($3, data_vencimento),
        observacoes = COALESCE($4, observacoes),
        id_categoria = COALESCE($5, id_categoria)
      WHERE id = $6 AND empresa_id = $7 AND (is_excluido = FALSE OR is_excluido IS NULL)
      RETURNING *
    `, [descricao || null, valor_total != null ? parseFloat(valor_total) : null,
        data_vencimento || null, observacoes !== undefined ? observacoes : null,
        id_categoria || null, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Saida nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Financeiro] Erro ao editar saida:', err.message)
    res.status(500).json({ error: 'Erro ao editar saida' })
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

// POST /api/financeiro/validar-admin — valida senha de administrador/gerente
router.post('/validar-admin', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { senha } = req.body
    if (!senha) return res.status(400).json({ error: 'Senha obrigatoria' })
    // Buscar admins/gerentes da empresa
    const result = await pool.query(
      "SELECT id, nome, senha FROM usuarios WHERE empresa_id = $1 AND (LOWER(funcao) IN ('administrador', 'admin', 'gerente')) AND (excluido = FALSE OR excluido IS NULL)",
      [empresaId]
    )
    // Verificar senha com bcrypt
    const bcrypt = await import('bcryptjs').catch(() => import('bcrypt'))
    for (const user of result.rows) {
      if (user.senha && await bcrypt.default.compare(senha, user.senha)) {
        return res.json({ valido: true, autorizador: user.nome })
      }
    }
    res.json({ valido: false })
  } catch (err) {
    console.error('[Financeiro] Erro validar admin:', err.message)
    res.status(500).json({ error: 'Erro ao validar' })
  }
})

// POST /api/financeiro/estornar — estorna pagamento com auditoria
router.post('/estornar', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { tipo, id, motivo, autorizador } = req.body
    if (!tipo || !id || !motivo || !autorizador) return res.status(400).json({ error: 'Campos obrigatorios: tipo, id, motivo, autorizador' })

    await client.query('BEGIN')

    let descricaoItem = ''
    if (tipo === 'passagem') {
      const r = await client.query('SELECT numero_bilhete, valor_pago, valor_total FROM passagens WHERE id_passagem = $1 AND empresa_id = $2', [id, empresaId])
      if (r.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ error: 'Passagem nao encontrada' }) }
      const p = r.rows[0]
      descricaoItem = `Passagem #${p.numero_bilhete} | Pago: R$${p.valor_pago} | Total: R$${p.valor_total}`
      await client.query('UPDATE passagens SET valor_pago = 0, valor_devedor = valor_total, status_passagem = $1, valor_pagamento_dinheiro = 0, valor_pagamento_pix = 0, valor_pagamento_cartao = 0 WHERE id_passagem = $2 AND empresa_id = $3', ['PENDENTE', id, empresaId])
    } else if (tipo === 'encomenda') {
      const r = await client.query('SELECT numero_encomenda, valor_pago, total_a_pagar FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2', [id, empresaId])
      if (r.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ error: 'Encomenda nao encontrada' }) }
      const e = r.rows[0]
      descricaoItem = `Encomenda #${e.numero_encomenda} | Pago: R$${e.valor_pago} | Total: R$${e.total_a_pagar}`
      await client.query('UPDATE encomendas SET valor_pago = 0, status_pagamento = $1, forma_pagamento = NULL WHERE id_encomenda = $2 AND empresa_id = $3', ['PENDENTE', id, empresaId])
    } else if (tipo === 'frete') {
      const r = await client.query('SELECT numero_frete, valor_pago, valor_total_itens FROM fretes WHERE id_frete = $1 AND empresa_id = $2', [id, empresaId])
      if (r.rows.length === 0) { await client.query('ROLLBACK'); return res.status(404).json({ error: 'Frete nao encontrado' }) }
      const f = r.rows[0]
      descricaoItem = `Frete #${f.numero_frete} | Pago: R$${f.valor_pago} | Total: R$${f.valor_total_itens}`
      await client.query('UPDATE fretes SET valor_pago = 0, valor_devedor = valor_frete_calculado, tipo_pagamento = NULL, status_frete = NULL WHERE id_frete = $1 AND empresa_id = $2', [id, empresaId])
    } else {
      await client.query('ROLLBACK')
      return res.status(400).json({ error: 'Tipo invalido' })
    }

    // Registrar auditoria
    await client.query(
      `INSERT INTO auditoria_financeiro (acao, tipo_operacao, usuario, usuario_solicitante, motivo, detalhe_valor, empresa_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      ['ESTORNO_PAGAMENTO', `ESTORNO_${tipo.toUpperCase()}`, `${req.user.login} / ${autorizador}`, req.user.login, motivo, descricaoItem, empresaId]
    )

    await client.query('COMMIT')
    res.json({ ok: true, descricao: descricaoItem })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Financeiro] Erro estorno:', err.message)
    res.status(500).json({ error: 'Erro ao estornar' })
  } finally {
    client.release()
  }
})

export default router
