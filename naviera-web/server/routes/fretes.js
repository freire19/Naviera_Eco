import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'
import { criarFreteComItens } from '../helpers/criarFrete.js'

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
