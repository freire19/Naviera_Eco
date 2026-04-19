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
      SELECT v.id_viagem, v.id_embarcacao, v.id_rota, v.is_atual,
             TO_CHAR(v.data_viagem, 'DD/MM/YYYY') AS data_viagem,
             TO_CHAR(v.data_chegada, 'DD/MM/YYYY') AS data_chegada,
             v.descricao, v.ativa, v.id_horario_saida,
             e.nome AS nome_embarcacao,
             CASE WHEN r.origem IS NOT NULL THEN r.origem || ' - ' || COALESCE(r.destino, '') ELSE NULL END AS nome_rota,
             hs.descricao_horario_saida AS horario
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id
      LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
      WHERE v.empresa_id = $1
      ORDER BY v.data_viagem DESC
      LIMIT 200
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
router.post('/', validate({ id_embarcacao: 'required|integer', id_rota: 'required|integer', data_viagem: 'required|string', data_chegada: 'required|string' }), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida, ativa } = req.body
    if (!id_embarcacao || !id_rota || !data_viagem || !data_chegada) {
      return res.status(400).json({ error: 'Campos obrigatorios: id_embarcacao, id_rota, data_viagem, data_chegada' })
    }
    const marcarAtiva = ativa === true || ativa === 'true'

    await client.query('BEGIN')
    // Se vai criar ja ativa, desativa todas as outras primeiro (apenas 1 ativa por empresa)
    if (marcarAtiva) {
      await client.query('UPDATE viagens SET is_atual = FALSE, ativa = FALSE WHERE empresa_id = $1', [empresaId])
    }
    const result = await client.query(`
      INSERT INTO viagens (id_viagem, id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida, ativa, is_atual, empresa_id)
      VALUES (nextval('seq_viagem'), $1, $2, $3, $4, $5, $6, $7, $7, $8)
      RETURNING *
    `, [id_embarcacao, id_rota, data_viagem, data_chegada, descricao || null, id_horario_saida || null, marcarAtiva, empresaId])
    await client.query('COMMIT')
    res.status(201).json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {})
    console.error('[Viagens] Erro ao criar:', err.message)
    res.status(500).json({ error: 'Erro ao criar viagem' })
  } finally {
    client.release()
  }
})

// PUT /api/viagens/:id
router.put('/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { id_embarcacao, id_rota, data_viagem, data_chegada, descricao, id_horario_saida, ativa } = req.body
    const marcarAtiva = ativa === true || ativa === 'true'
    const desmarcarAtiva = ativa === false || ativa === 'false'

    await client.query('BEGIN')
    // Se vai marcar essa como ativa, desativa todas as outras primeiro
    if (marcarAtiva) {
      await client.query('UPDATE viagens SET is_atual = FALSE, ativa = FALSE WHERE empresa_id = $1 AND id_viagem != $2', [empresaId, req.params.id])
    }
    // descricao usa COALESCE com string vazia tratada como null (permite limpar)
    const descParam = descricao === undefined ? null : (descricao || null)
    const result = await client.query(`
      UPDATE viagens SET
        data_viagem = COALESCE($1, data_viagem),
        data_chegada = COALESCE($2, data_chegada),
        descricao = CASE WHEN $9::boolean THEN $3 ELSE COALESCE($3, descricao) END,
        id_embarcacao = COALESCE($4, id_embarcacao),
        id_rota = COALESCE($5, id_rota),
        id_horario_saida = COALESCE($6, id_horario_saida),
        ativa = CASE WHEN $10::boolean THEN $11 ELSE ativa END,
        is_atual = CASE WHEN $10::boolean THEN $11 ELSE is_atual END
      WHERE id_viagem = $7 AND empresa_id = $8
      RETURNING *
    `, [
      data_viagem, data_chegada, descParam, id_embarcacao, id_rota, id_horario_saida || null,
      req.params.id, empresaId,
      descricao !== undefined, // $9: permite limpar descricao
      marcarAtiva || desmarcarAtiva, // $10: atualizar ativa
      marcarAtiva // $11: novo valor (true se marcar, false se desmarcar)
    ])
    if (result.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Viagem nao encontrada' })
    }
    await client.query('COMMIT')
    res.json(result.rows[0])
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {})
    console.error('[Viagens] Erro ao atualizar:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar viagem' })
  } finally {
    client.release()
  }
})

// PUT /api/viagens/:id/ativar
router.put('/:id/ativar', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { ativa } = req.body
    await client.query('BEGIN')
    const ativar = ativa !== false
    if (ativar) {
      await client.query('UPDATE viagens SET is_atual = FALSE, ativa = FALSE WHERE empresa_id = $1', [empresaId])
    }
    const result = await client.query(
      'UPDATE viagens SET is_atual = $1, ativa = $1 WHERE id_viagem = $2 AND empresa_id = $3 RETURNING *',
      [ativar, req.params.id, empresaId]
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
// DS4-032 fix: somente Administrador/Gerente pode deletar viagens (cascade completo)
router.delete('/:id', async (req, res) => {
  const funcao = (req.user.funcao || '').toLowerCase()
  if (funcao !== 'administrador' && funcao !== 'admin' && funcao !== 'gerente') {
    return res.status(403).json({ error: 'Somente Administrador ou Gerente pode excluir viagens' })
  }
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
    if (result.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Viagem nao encontrada' })
    }
    await client.query('COMMIT')
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
