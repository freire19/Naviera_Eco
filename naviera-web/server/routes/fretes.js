import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'
import { criarFreteComItens } from '../helpers/criarFrete.js'
import { validarAutorizador } from '../utils/validarAutorizador.js'

const router = Router()
router.use(authMiddleware)

// GET /api/fretes/contatos — lista clientes de frete (tabela propria, separada de encomenda/passagem)
router.get('/contatos', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      `SELECT id_cliente AS id, nome_cliente AS nome_razao_social, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone
       FROM cad_clientes_frete WHERE empresa_id = $1 ORDER BY nome_cliente`,
      [empresaId]
    )
    res.json(result.rows)
  } catch (err) {
    console.error('[Fretes] Erro ao listar contatos:', err.message)
    res.status(500).json({ error: 'Erro ao listar contatos' })
  }
})

// POST /api/fretes/contatos — criar cliente de frete (tabela cad_clientes_frete)
router.post('/contatos', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const nomeUpper = nome.trim().toUpperCase()
    const result = await pool.query(
      `INSERT INTO cad_clientes_frete (nome_cliente, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone, empresa_id)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8) ON CONFLICT (empresa_id, nome_cliente) DO NOTHING RETURNING *`,
      [nomeUpper, (razao_social || '').toUpperCase() || null, cpf_cnpj || null, (endereco || '').toUpperCase() || null, inscricao_estadual || null, email || null, telefone || null, empresaId]
    )
    // #239: sinalizar se foi criado (201) ou se ja existia (200 com criado:false)
    if (result.rows.length > 0) return res.status(201).json({ criado: true, id: result.rows[0].id_cliente, nome_razao_social: result.rows[0].nome_cliente, ...result.rows[0] })
    const existing = await pool.query('SELECT * FROM cad_clientes_frete WHERE LOWER(nome_cliente) = LOWER($1) AND empresa_id = $2', [nomeUpper, empresaId])
    res.json({ criado: false, ...(existing.rows[0] || { nome_razao_social: nomeUpper }) })
  } catch (err) {
    console.error('[Fretes] Erro ao criar contato:', err.message)
    res.status(500).json({ error: 'Erro ao criar contato' })
  }
})

// PUT /api/fretes/contatos/:id — atualizar cliente de frete
router.put('/contatos/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      `UPDATE cad_clientes_frete SET nome_cliente = $1, razao_social = $2, cpf_cnpj = $3, endereco = $4, inscricao_estadual = $5, email = $6, telefone = $7
       WHERE id_cliente = $8 AND empresa_id = $9 RETURNING *`,
      [nome.trim().toUpperCase(), (razao_social || '').toUpperCase() || null, cpf_cnpj || null, (endereco || '').toUpperCase() || null, inscricao_estadual || null, email || null, telefone || null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Cliente nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Fretes] Erro ao atualizar contato:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar contato' })
  }
})

// DELETE /api/fretes/contatos/:id — excluir cliente de frete
router.delete('/contatos/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'DELETE FROM cad_clientes_frete WHERE id_cliente = $1 AND empresa_id = $2 RETURNING id_cliente',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Cliente nao encontrado' })
    res.json({ ok: true })
  } catch (err) {
    console.error('[Fretes] Erro ao excluir contato:', err.message)
    res.status(500).json({ error: 'Erro ao excluir contato' })
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

// GET /api/fretes/relatorio/itens — Itens detalhados para relatorio
router.get('/relatorio/itens', async (req, res) => {
  try {
    const { viagem_id, cliente, rota } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })
    const empresaId = req.user.empresa_id
    let sql = `SELECT f.numero_frete, v.data_viagem, f.remetente_nome_temp AS remetente,
      f.destinatario_nome_temp AS destinatario, f.local_transporte, f.rota_temp AS rota,
      fi.nome_item_ou_id_produto AS item, fi.quantidade, fi.preco_unitario,
      (fi.quantidade * fi.preco_unitario) AS total_item
      FROM fretes f
      JOIN frete_itens fi ON f.id_frete = fi.id_frete
      LEFT JOIN viagens v ON f.id_viagem = v.id_viagem
      WHERE f.id_viagem = $1 AND f.empresa_id = $2`
    const params = [viagem_id, empresaId]
    if (cliente) { sql += ` AND f.destinatario_nome_temp = $${params.length + 1}`; params.push(cliente) }
    if (rota) { sql += ` AND f.rota_temp = $${params.length + 1}`; params.push(rota) }
    sql += ' ORDER BY f.numero_frete, fi.nome_item_ou_id_produto'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar itens relatorio' })
  }
})

// GET /api/fretes/relatorio/financeiro — Situacao financeira por frete
router.get('/relatorio/financeiro', async (req, res) => {
  try {
    const { viagem_id, cliente, rota } = req.query
    if (!viagem_id) return res.status(400).json({ error: 'viagem_id obrigatorio' })
    const empresaId = req.user.empresa_id
    let sql = `SELECT f.numero_frete, f.valor_total_itens, f.valor_pago, f.valor_devedor,
      f.remetente_nome_temp AS remetente, f.destinatario_nome_temp AS destinatario
      FROM fretes f WHERE f.id_viagem = $1 AND f.empresa_id = $2`
    const params = [viagem_id, empresaId]
    if (cliente) { sql += ` AND f.destinatario_nome_temp = $${params.length + 1}`; params.push(cliente) }
    if (rota) { sql += ` AND f.rota_temp = $${params.length + 1}`; params.push(rota) }
    sql += ' ORDER BY f.numero_frete'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar financeiro relatorio' })
  }
})

// GET /api/fretes/:id/itens
router.get('/:id/itens', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM frete_itens WHERE id_frete = $1 ORDER BY id_item_frete',
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
    let sql = `SELECT f.id_frete, f.id_viagem, f.numero_frete,
      f.remetente_nome_temp AS remetente, f.destinatario_nome_temp AS destinatario,
      f.remetente_nome_temp, f.destinatario_nome_temp, f.rota_temp, f.conferente_temp,
      f.observacoes, f.valor_total_itens, f.valor_frete_calculado, f.valor_pago, f.valor_devedor,
      f.desconto, f.status_frete, f.tipo_pagamento, f.nome_caixa,
      f.rota_temp AS rota, f.conferente_temp AS conferente,
      f.data_emissao, f.local_transporte, f.cidade_cobranca,
      f.num_notafiscal, f.valor_notafiscal, f.peso_notafiscal,
      TO_CHAR(v.data_viagem, 'DD/MM/YYYY') AS data_viagem,
      TO_CHAR(v.data_chegada, 'DD/MM/YYYY') AS data_chegada,
      COALESCE((SELECT SUM(fi.quantidade) FROM frete_itens fi WHERE fi.id_frete = f.id_frete), 0) AS total_volumes
      FROM fretes f
      LEFT JOIN viagens v ON f.id_viagem = v.id_viagem
      WHERE f.empresa_id = $1`
    const params = [empresaId]
    if (viagem_id) {
      sql += ' AND f.id_viagem = $2'
      params.push(viagem_id)
    }
    sql += ' ORDER BY f.id_frete DESC'
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

// PUT /api/fretes/:id — atualizar frete completo com itens
router.put('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const {
      remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp,
      observacoes, valor_total_itens, local_transporte, cidade_cobranca,
      num_notafiscal, valor_notafiscal, peso_notafiscal, itens
    } = req.body

    const vItens = parseFloat(valor_total_itens) || 0

    await client.query('BEGIN')
    const result = await client.query(`
      UPDATE fretes SET remetente_nome_temp = $1, destinatario_nome_temp = $2,
        rota_temp = $3, conferente_temp = $4, observacoes = $5,
        valor_total_itens = $6, valor_frete_calculado = $6,
        local_transporte = $7, cidade_cobranca = $8,
        num_notafiscal = $9, valor_notafiscal = $10, peso_notafiscal = $11,
        valor_devedor = GREATEST(0, $6 - COALESCE(valor_pago, 0))
      WHERE id_frete = $12 AND empresa_id = $13
      RETURNING *
    `, [(remetente_nome_temp || '').toUpperCase() || null, (destinatario_nome_temp || '').toUpperCase() || null,
        rota_temp || null, conferente_temp || null, observacoes || null,
        vItens, local_transporte || null, cidade_cobranca || null,
        num_notafiscal || null, parseFloat(valor_notafiscal) || 0, parseFloat(peso_notafiscal) || 0,
        req.params.id, empresaId])

    if (result.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Frete nao encontrado' })
    }

    // Substituir itens
    if (itens && Array.isArray(itens)) {
      await client.query('DELETE FROM frete_itens WHERE id_frete = $1', [req.params.id])
      if (itens.length > 0) {
        const values = []
        const params = []
        itens.forEach((item, i) => {
          const off = i * 5
          values.push(`($${off+1}, $${off+2}, $${off+3}, $${off+4}, $${off+5})`)
          params.push(req.params.id, (item.nome_item || item.descricao || '').toUpperCase() || null, item.quantidade || 1, item.preco_unitario || item.valor_unitario || 0, item.subtotal_item || item.subtotal || 0)
        })
        await client.query(`INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item) VALUES ${values.join(', ')}`, params)
      }
    }

    await client.query('COMMIT')
    res.json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Fretes] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar frete' })
  } finally {
    client.release()
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
    // #229: atualizar status_pagamento e status_frete junto para nao desalinhar UI/relatorios
    const result = await pool.query(`
      UPDATE fretes SET
        valor_pago = valor_pago + $1,
        valor_devedor = valor_devedor - $1,
        status_pagamento = CASE WHEN (valor_devedor - $1) <= 0.01 THEN 'PAGO' ELSE 'PARCIAL' END,
        status_frete = CASE WHEN (valor_devedor - $1) <= 0.01 THEN 'PAGO' ELSE status_frete END
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
      'SELECT id_frete, valor_pago FROM fretes WHERE id_frete = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (check.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Frete nao encontrado' })
    }
    const valorEstornado = parseFloat(check.rows[0].valor_pago) || 0

    await client.query(
      `INSERT INTO log_estornos_fretes
       (id_frete, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador, data_hora, empresa_id, tipo_operacao)
       VALUES ($1, $2, $3, NULL, $4, $5, NOW(), $6, 'EXCLUSAO')`,
      [req.params.id, valorEstornado, motivo.trim(), autorizador.id, autorizador.nome, empresaId]
    )

    await client.query(
      'DELETE FROM frete_itens WHERE id_frete IN (SELECT id_frete FROM fretes WHERE id_frete = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM fretes WHERE id_frete = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
    res.json({ mensagem: 'Frete excluido', autorizador: autorizador.nome })
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {})
    console.error('[Fretes] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir frete' })
  } finally {
    client.release()
  }
})

export default router
