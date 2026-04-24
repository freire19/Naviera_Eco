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

// GET /api/financeiro/dashboard — agregados de passagens+encomendas+fretes com filtros.
// Agregacao no Postgres; filtros vao no WHERE, nao em Array.filter() pos-query.
router.get('/dashboard', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { viagem_id, categoria, forma_pagto, caixa } = req.query

    const params = [empresaId]
    const addParam = (v) => {
      params.push(v)
      return `$${params.length}`
    }

    // Pre-filtro por origem dentro de cada branch — permite que o planner elimine
    // branches inteiras quando categoria esta fixada (evita LEFT JOINs desnecessarios).
    const catFilter = (origem) =>
      categoria && categoria !== 'Todas' && origem !== categoria.toUpperCase() ? 'FALSE' : 'TRUE'

    let outerWhere = 'TRUE'
    if (viagem_id) outerWhere += ` AND id_viagem = ${addParam(viagem_id)}`
    if (caixa && caixa !== 'Todos') outerWhere += ` AND UPPER(usuario) = ${addParam(caixa.toUpperCase())}`
    if (forma_pagto && forma_pagto !== 'Todas') outerWhere += ` AND UPPER(pgto) LIKE ${addParam(`%${forma_pagto.toUpperCase()}%`)}`

    const sql = `
      WITH lancamentos AS (
        SELECT 'ENCOMENDA' AS origem, e.id_viagem, e.total_a_pagar AS total, e.valor_pago AS pago,
               COALESCE(e.forma_pagamento, 'PENDENTE') AS pgto, COALESCE(ue.nome, '') AS usuario
        FROM encomendas e
        LEFT JOIN usuarios ue ON e.id_caixa = ue.id
        WHERE e.empresa_id = $1 AND ${catFilter('ENCOMENDA')}
        UNION ALL
        SELECT 'FRETE', f.id_viagem, f.valor_frete_calculado, f.valor_pago,
               COALESCE(f.tipo_pagamento, 'PENDENTE'), COALESCE(f.nome_caixa, '')
        FROM fretes f WHERE f.empresa_id = $1 AND ${catFilter('FRETE')}
        UNION ALL
        SELECT 'PASSAGEM', p.id_viagem, p.valor_total, p.valor_pago,
               COALESCE(afp.nome_forma_pagamento, 'DINHEIRO'), COALESCE(up.nome, 'SISTEMA')
        FROM passagens p
        LEFT JOIN aux_formas_pagamento afp ON p.id_forma_pagamento = afp.id_forma_pagamento
        LEFT JOIN usuarios up ON p.id_usuario_emissor = up.id
        WHERE p.empresa_id = $1 AND ${catFilter('PASSAGEM')}
      )
      SELECT
        COUNT(*)::int AS registros,
        COALESCE(SUM(total), 0) AS total_geral,
        COALESCE(SUM(pago), 0) AS total_recebido,
        COALESCE(SUM(CASE WHEN origem = 'PASSAGEM' THEN total ELSE 0 END), 0) AS soma_passagem,
        COALESCE(SUM(CASE WHEN origem = 'ENCOMENDA' THEN total ELSE 0 END), 0) AS soma_encomenda,
        COALESCE(SUM(CASE WHEN origem = 'FRETE' THEN total ELSE 0 END), 0) AS soma_frete,
        -- #214 #653: buckets por match exato (CARTEIRA_DIGITAL nao vira CARTAO, e vice-versa)
        COALESCE(SUM(CASE WHEN pago > 0 AND UPPER(pgto) = 'PIX' THEN pago ELSE 0 END), 0) AS soma_pix,
        COALESCE(SUM(CASE WHEN pago > 0 AND UPPER(pgto) IN ('CARTAO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'CREDITO', 'DEBITO') THEN pago ELSE 0 END), 0) AS soma_cartao,
        COALESCE(SUM(CASE WHEN pago > 0 AND UPPER(pgto) NOT IN ('PIX', 'CARTAO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'CREDITO', 'DEBITO', 'CARTEIRA_DIGITAL', 'BOLETO') THEN pago ELSE 0 END), 0) AS soma_dinheiro
      FROM lancamentos
      WHERE ${outerWhere}
    `
    const result = await pool.query(sql, params)
    const r = result.rows[0]
    const n = (v) => Math.round(parseFloat(v || 0) * 100) / 100

    res.json({
      totalGeral: n(r.total_geral),
      totalRecebido: n(r.total_recebido),
      pendente: n(r.total_geral - r.total_recebido),
      categorias: { passagens: n(r.soma_passagem), encomendas: n(r.soma_encomenda), fretes: n(r.soma_frete) },
      formasPagamento: { dinheiro: n(r.soma_dinheiro), pix: n(r.soma_pix), cartao: n(r.soma_cartao) },
      registros: r.registros
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
    // #216: guards valores
    const vTotal = parseFloat(valor_total)
    const vPago = parseFloat(valor_pago) || 0
    if (!(vTotal > 0)) return res.status(400).json({ error: 'valor_total deve ser > 0' })
    if (vPago < 0) return res.status(400).json({ error: 'valor_pago nao pode ser negativo' })
    if (vPago > vTotal + 0.01) return res.status(400).json({ error: 'valor_pago nao pode exceder valor_total' })
    // #217: validar datas
    const parseDate = (s) => {
      if (!s) return null
      const d = new Date(s)
      if (isNaN(d.getTime())) throw new Error('data invalida')
      return s
    }
    let dVenc, dPag
    try {
      dVenc = parseDate(data_vencimento)
      dPag = parseDate(data_pagamento)
    } catch {
      return res.status(400).json({ error: 'data_vencimento ou data_pagamento invalida' })
    }
    const result = await pool.query(`
      INSERT INTO financeiro_saidas (id_viagem, descricao, valor_total, data_vencimento, id_categoria, funcionario_id, forma_pagamento,
        valor_pago, data_pagamento, status, numero_parcela, total_parcelas, observacoes, is_excluido, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, FALSE, $14)
      RETURNING *
    `, [
      id_viagem || null, descricao, vTotal, dVenc, id_categoria || null, id_funcionario || null, forma_pagamento || null,
      vPago, dPag, status || 'PENDENTE',
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
    // #218: valor_total deve ser positivo (evita boleto com valor 0 ou negativo)
    const valorTotalNum = parseFloat(valor_total)
    if (!(valorTotalNum > 0)) {
      return res.status(400).json({ error: 'valor_total deve ser maior que zero' })
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

// #204: endpoints legados /validar-admin e /estornar foram removidos.
//   Estornos agora passam por /api/estornos/{tipo}/:id (routes/estornos.js),
//   que valida login+senha por bcrypt no mesmo request, aceita valor parcial,
//   usa SELECT ... FOR UPDATE e grava em log_estornos_{passagens,encomendas,fretes}.

export default router
