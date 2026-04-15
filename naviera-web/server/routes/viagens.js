import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'

const router = Router()
router.use(authMiddleware)

// GET /api/viagens
router.get('/', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
             e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id
      WHERE v.empresa_id = $1
      ORDER BY v.data_viagem DESC
    `, [empresaId])
    res.json(result.rows)
  } catch (err) {
    console.error('[Viagens] Erro:', err.message)
    res.status(500).json({ error: 'Erro ao listar viagens' })
  }
})

// GET /api/viagens/ativa
router.get('/ativa', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
             e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id
      WHERE v.is_atual = TRUE AND v.empresa_id = $1
      ORDER BY v.data_viagem DESC
      LIMIT 1
    `, [empresaId])
    res.json(result.rows[0] || null)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar viagem ativa' })
  }
})

// GET /api/viagens/:id
router.get('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT v.*, e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id
      WHERE v.id_viagem = $1 AND v.empresa_id = $2
    `, [req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Viagem nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar viagem' })
  }
})

// POST /api/viagens
router.post('/', validate({ id_embarcacao: 'required|integer', id_rota: 'required|integer', data_viagem: 'required|string', data_chegada: 'required|string', descricao: 'required|string' }), async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida } = req.body
    if (!id_embarcacao || !id_rota || !data_viagem || !data_chegada || !descricao) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_embarcacao, id_rota, data_viagem, data_chegada, descricao' })
    }
    const result = await pool.query(`
      INSERT INTO viagens (id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida, ativa, is_atual, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, FALSE, FALSE, $7)
      RETURNING *
    `, [id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida || null, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Viagens] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar viagem' })
  }
})

// PUT /api/viagens/:id
router.put('/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_embarcacao, id_rota, data_viagem, data_chegada, descricao } = req.body
    const result = await pool.query(`
      UPDATE viagens SET data_viagem = COALESCE($1, data_viagem), data_chegada = COALESCE($2, data_chegada),
        descricao = COALESCE($3, descricao), id_embarcacao = COALESCE($4, id_embarcacao), id_rota = COALESCE($5, id_rota)
      WHERE id_viagem = $6 AND empresa_id = $7
      RETURNING *
    `, [data_viagem, data_chegada, descricao, id_embarcacao, id_rota, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Viagem nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Viagens] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar viagem' })
  }
})

// PUT /api/viagens/:id/ativar
router.put('/:id/ativar', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { ativa } = req.body
    await client.query('BEGIN')
    if (ativa) {
      await client.query('UPDATE viagens SET is_atual = FALSE WHERE empresa_id = $1', [empresaId])
    }
    const result = await client.query(
      'UPDATE viagens SET is_atual = $1 WHERE id_viagem = $2 AND empresa_id = $3 RETURNING *',
      [ativa !== false, req.params.id, empresaId]
    )
    if (result.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Viagem nao encontrada' })
    }
    await client.query('COMMIT')
    res.json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Viagens] Erro ao ativar:', err.message)
    res.status(500).json({ error: 'Erro ao ativar viagem' })
  } finally {
    client.release()
  }
})

// DELETE /api/viagens/:id
router.delete('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    await client.query('BEGIN')
    await client.query(
      'DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM passagens WHERE id_viagem = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM encomendas WHERE id_viagem = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM frete_itens WHERE id_frete IN (SELECT id_frete FROM fretes WHERE id_viagem = $1 AND empresa_id = $2)',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM fretes WHERE id_viagem = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    await client.query(
      'DELETE FROM financeiro_saidas WHERE id_viagem = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    const result = await client.query(
      'DELETE FROM viagens WHERE id_viagem = $1 AND empresa_id = $2 RETURNING id_viagem',
      [req.params.id, empresaId]
    )
    await client.query('COMMIT')
    if (result.rows.length === 0) return res.status(404).json({ error: 'Viagem nao encontrada' })
    res.json({ mensagem: 'Viagem excluida' })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Viagens] Erro ao excluir:', err.message)
    res.status(500).json({ error: 'Erro ao excluir viagem.' })
  } finally {
    client.release()
  }
})

export default router
