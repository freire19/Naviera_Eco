import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'
import { criarFreteComItens } from '../helpers/criarFrete.js'

const router = Router()
router.use(authMiddleware)

// GET /api/fretes/contatos — lista contatos de frete (tabela separada de clientes encomenda)
router.get('/contatos', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM contatos ORDER BY nome_razao_social')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar contatos' })
  }
})

// POST /api/fretes/contatos — criar contato de frete
router.post('/contatos', async (req, res) => {
  try {
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO contatos (nome_razao_social) VALUES ($1) ON CONFLICT DO NOTHING RETURNING *',
      [nome.trim().toUpperCase()]
    )
    if (result.rows.length > 0) return res.status(201).json(result.rows[0])
    // Ja existe
    const existing = await pool.query('SELECT * FROM contatos WHERE UPPER(nome_razao_social) = UPPER($1)', [nome.trim()])
    res.json(existing.rows[0] || {})
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar contato' })
  }
})

// GET /api/fretes/proximo-numero
router.get('/proximo-numero', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num FROM fretes WHERE empresa_id = $1',
      [empresaId]
    )
    res.json({ numero: String(result.rows[0].next_num) })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar numero' })
  }
})

// GET /api/fretes/:id/itens
router.get('/:id/itens', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM frete_itens WHERE id_frete = $1 ORDER BY id_frete_item',
      [req.params.id]
    )
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens' })
  }
})

// GET /api/fretes?viagem_id=X
router.get('/', async (req, res) => {
  try {
    const { viagem_id } = req.query
    const empresaId = req.user.empresa_id
    let sql = 'SELECT id_frete, id_viagem, numero_frete, remetente, destinatario, observacoes, total_a_pagar, valor_pago, desconto, status_pagamento, forma_pagamento, local_pagamento, rota, data_lancamento, id_caixa FROM fretes WHERE empresa_id = $1'
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY id_frete DESC'
    // DP052: LIMIT para evitar datasets ilimitados
    const limit = Math.min(parseInt(req.query.limit) || 500, 1000)
    const offset = parseInt(req.query.offset) || 0
    sql += ` LIMIT ${limit} OFFSET ${offset}`
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
router.post('/', validate({ id_viagem: 'required|integer', valor_total_itens: 'required|number' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    if (!req.body.id_viagem || !req.body.valor_total_itens) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_viagem, valor_total_itens' })
    }

    await client.query('BEGIN')
    const frete = await criarFreteComItens(client, empresaId, req.body)
    await client.query('COMMIT')
    res.status(201).json(frete)
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
    // DS4-011 fix: guarda contra overpayment — so aceita se valor_devedor >= pagamento
    const result = await pool.query(`
      UPDATE fretes SET valor_pago = valor_pago + $1, valor_devedor = valor_devedor - $1
      WHERE id_frete = $2 AND empresa_id = $3
        AND valor_devedor >= $1
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
// DS4-032 fix: somente Administrador/Gerente pode deletar
router.delete('/:id', async (req, res) => {
  const funcao = (req.user.funcao || '').toLowerCase()
  if (funcao !== 'administrador' && funcao !== 'admin' && funcao !== 'gerente') {
    return res.status(403).json({ error: 'Somente Administrador ou Gerente pode excluir fretes' })
  }
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    await client.query('BEGIN')
    // Verify tenant ownership BEFORE deleting items
    const check = await client.query(
      'SELECT id_frete FROM fretes WHERE id_frete = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (check.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Frete nao encontrado' })
    }
    await client.query(
      'DELETE FROM frete_itens WHERE id_frete IN (SELECT id_frete FROM fretes WHERE id_frete = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    const result = await client.query(
      'DELETE FROM fretes WHERE id_frete = $1 AND empresa_id = $2 RETURNING id_frete',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
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
