import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// ── Helper: validar senha do autorizador ──
async function validarAutorizador(login, senha, empresaId) {
  const result = await pool.query(
    `SELECT id, nome, senha FROM usuarios
     WHERE (LOWER(nome) = LOWER($1) OR LOWER(email) = LOWER($1))
       AND (excluido = FALSE OR excluido IS NULL)
       AND empresa_id = $2`,
    [login, empresaId]
  )
  if (result.rows.length === 0) return null
  const user = result.rows[0]
  const valida = await bcrypt.compare(senha, user.senha)
  if (!valida) return null
  return { id: user.id, nome: user.nome }
}

// ══════════════════════════════════════════
// POST /api/estornos/passagem/:id
// ══════════════════════════════════════════
router.post('/passagem/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { valor, motivo, forma_devolucao, senha_autorizador, login_autorizador } = req.body

    if (!valor || valor <= 0) return res.status(400).json({ error: 'Valor de estorno obrigatorio e positivo' })
    if (!motivo) return res.status(400).json({ error: 'Motivo obrigatorio' })
    if (!login_autorizador || !senha_autorizador) return res.status(400).json({ error: 'Login e senha do autorizador obrigatorios' })

    const autorizador = await validarAutorizador(login_autorizador, senha_autorizador, empresaId)
    if (!autorizador) return res.status(403).json({ error: 'Senha do autorizador invalida' })

    await client.query('BEGIN')

    // Buscar passagem
    const passResult = await client.query(
      'SELECT * FROM passagens WHERE id_passagem = $1 AND empresa_id = $2 FOR UPDATE',
      [req.params.id, empresaId]
    )
    if (passResult.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Passagem nao encontrada' })
    }
    const passagem = passResult.rows[0]
    const valorPago = parseFloat(passagem.valor_pago) || 0

    if (parseFloat(valor) > valorPago + 0.01) {
      await client.query('ROLLBACK')
      return res.status(400).json({ error: `Valor de estorno (${valor}) excede o valor pago (${valorPago})` })
    }

    const novoValorPago = Math.max(0, valorPago - parseFloat(valor))
    const novoValorDevedor = parseFloat(passagem.valor_a_pagar || passagem.valor_total || 0) - novoValorPago
    const novoStatus = novoValorDevedor <= 0.01 ? 'PAGO' : novoValorPago > 0 ? 'PARCIAL' : 'PENDENTE'

    const updated = await client.query(
      `UPDATE passagens SET valor_pago = $1, valor_devedor = $2, status_passagem = $3
       WHERE id_passagem = $4 AND empresa_id = $5 RETURNING *`,
      [novoValorPago, Math.max(0, novoValorDevedor), novoStatus, req.params.id, empresaId]
    )

    // Log no historico
    await client.query(
      `INSERT INTO log_estornos_passagens (id_passagem, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador, data_hora, empresa_id)
       VALUES ($1, $2, $3, $4, $5, $6, NOW(), $7)`,
      [req.params.id, valor, motivo, forma_devolucao || null, autorizador.id, autorizador.nome, empresaId]
    )

    await client.query('COMMIT')
    res.json(updated.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Estornos] Erro passagem:', err.message)
    res.status(500).json({ error: 'Erro ao processar estorno de passagem' })
  } finally {
    client.release()
  }
})

// ══════════════════════════════════════════
// POST /api/estornos/encomenda/:id
// ══════════════════════════════════════════
router.post('/encomenda/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { valor, motivo, forma_devolucao, senha_autorizador, login_autorizador } = req.body

    if (!valor || valor <= 0) return res.status(400).json({ error: 'Valor de estorno obrigatorio e positivo' })
    if (!motivo) return res.status(400).json({ error: 'Motivo obrigatorio' })
    if (!login_autorizador || !senha_autorizador) return res.status(400).json({ error: 'Login e senha do autorizador obrigatorios' })

    const autorizador = await validarAutorizador(login_autorizador, senha_autorizador, empresaId)
    if (!autorizador) return res.status(403).json({ error: 'Senha do autorizador invalida' })

    await client.query('BEGIN')

    const encResult = await client.query(
      'SELECT * FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2 FOR UPDATE',
      [req.params.id, empresaId]
    )
    if (encResult.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Encomenda nao encontrada' })
    }
    const encomenda = encResult.rows[0]
    const valorPago = parseFloat(encomenda.valor_pago) || 0

    if (parseFloat(valor) > valorPago + 0.01) {
      await client.query('ROLLBACK')
      return res.status(400).json({ error: `Valor de estorno (${valor}) excede o valor pago (${valorPago})` })
    }

    const novoValorPago = Math.max(0, valorPago - parseFloat(valor))
    const totalAPagar = parseFloat(encomenda.total_a_pagar) || 0
    const desconto = parseFloat(encomenda.desconto) || 0
    const novoStatus = novoValorPago >= (totalAPagar - desconto - 0.01) ? 'PAGO' : novoValorPago > 0 ? 'PARCIAL' : 'PENDENTE'

    const updated = await client.query(
      `UPDATE encomendas SET valor_pago = $1, status_pagamento = $2
       WHERE id_encomenda = $3 AND empresa_id = $4 RETURNING *`,
      [novoValorPago, novoStatus, req.params.id, empresaId]
    )

    await client.query(
      `INSERT INTO log_estornos_encomendas (id_encomenda, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador, data_hora, empresa_id)
       VALUES ($1, $2, $3, $4, $5, $6, NOW(), $7)`,
      [req.params.id, valor, motivo, forma_devolucao || null, autorizador.id, autorizador.nome, empresaId]
    )

    await client.query('COMMIT')
    res.json(updated.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Estornos] Erro encomenda:', err.message)
    res.status(500).json({ error: 'Erro ao processar estorno de encomenda' })
  } finally {
    client.release()
  }
})

// ══════════════════════════════════════════
// POST /api/estornos/frete/:id
// ══════════════════════════════════════════
router.post('/frete/:id', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { valor, motivo, forma_devolucao, senha_autorizador, login_autorizador } = req.body

    if (!valor || valor <= 0) return res.status(400).json({ error: 'Valor de estorno obrigatorio e positivo' })
    if (!motivo) return res.status(400).json({ error: 'Motivo obrigatorio' })
    if (!login_autorizador || !senha_autorizador) return res.status(400).json({ error: 'Login e senha do autorizador obrigatorios' })

    const autorizador = await validarAutorizador(login_autorizador, senha_autorizador, empresaId)
    if (!autorizador) return res.status(403).json({ error: 'Senha do autorizador invalida' })

    await client.query('BEGIN')

    const freteResult = await client.query(
      'SELECT * FROM fretes WHERE id_frete = $1 AND empresa_id = $2 FOR UPDATE',
      [req.params.id, empresaId]
    )
    if (freteResult.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Frete nao encontrado' })
    }
    const frete = freteResult.rows[0]
    const valorPago = parseFloat(frete.valor_pago) || 0

    if (parseFloat(valor) > valorPago + 0.01) {
      await client.query('ROLLBACK')
      return res.status(400).json({ error: `Valor de estorno (${valor}) excede o valor pago (${valorPago})` })
    }

    const novoValorPago = Math.max(0, valorPago - parseFloat(valor))
    const novoValorDevedor = parseFloat(frete.valor_frete_calculado || 0) - novoValorPago

    const updated = await client.query(
      `UPDATE fretes SET valor_pago = $1, valor_devedor = $2
       WHERE id_frete = $3 AND empresa_id = $4 RETURNING *`,
      [novoValorPago, Math.max(0, novoValorDevedor), req.params.id, empresaId]
    )

    await client.query(
      `INSERT INTO log_estornos_fretes (id_frete, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador, data_hora, empresa_id)
       VALUES ($1, $2, $3, $4, $5, $6, NOW(), $7)`,
      [req.params.id, valor, motivo, forma_devolucao || null, autorizador.id, autorizador.nome, empresaId]
    )

    // #DB130: Update frete status_pagamento after estorno
    await client.query(`
      UPDATE fretes SET status_pagamento = CASE
        WHEN valor_pago <= 0.01 THEN 'NAO_PAGO'
        WHEN valor_pago < valor_frete_calculado - 0.01 THEN 'PARCIAL'
        ELSE 'PAGO' END
      WHERE id_frete = $1 AND empresa_id = $2`, [req.params.id, empresaId])

    await client.query('COMMIT')
    res.json(updated.rows[0])
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[Estornos] Erro frete:', err.message)
    res.status(500).json({ error: 'Erro ao processar estorno de frete' })
  } finally {
    client.release()
  }
})

// ══════════════════════════════════════════
// GET /api/estornos/historico — Todos os estornos
// ══════════════════════════════════════════
router.get('/historico', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { tipo, data_inicio, data_fim, autorizador } = req.query

    const queries = []
    const buildWhere = (baseParams) => {
      let where = ''
      const params = [...baseParams]
      let idx = params.length + 1
      if (data_inicio) { where += ` AND l.data_hora >= $${idx}`; params.push(data_inicio); idx++ }
      if (data_fim) { where += ` AND l.data_hora <= ($${idx}::date + INTERVAL '1 day')`; params.push(data_fim); idx++ }
      if (autorizador) { where += ` AND LOWER(l.nome_autorizador) LIKE LOWER($${idx})`; params.push(`%${autorizador}%`); idx++ }
      return { where, params }
    }

    if (!tipo || tipo === 'passagem') {
      const { where, params } = buildWhere([empresaId])
      queries.push(pool.query(
        `SELECT l.*, 'passagem' AS tipo, p.numero_bilhete AS numero
         FROM log_estornos_passagens l
         LEFT JOIN passagens p ON l.id_passagem = p.id_passagem
         WHERE l.empresa_id = $1 ${where}
         ORDER BY l.data_hora DESC`, params
      ))
    }
    if (!tipo || tipo === 'encomenda') {
      const { where, params } = buildWhere([empresaId])
      queries.push(pool.query(
        `SELECT l.*, 'encomenda' AS tipo, e.numero_encomenda AS numero
         FROM log_estornos_encomendas l
         LEFT JOIN encomendas e ON l.id_encomenda = e.id_encomenda
         WHERE l.empresa_id = $1 ${where}
         ORDER BY l.data_hora DESC`, params
      ))
    }
    if (!tipo || tipo === 'frete') {
      const { where, params } = buildWhere([empresaId])
      queries.push(pool.query(
        `SELECT l.*, 'frete' AS tipo, f.numero_frete AS numero
         FROM log_estornos_fretes l
         LEFT JOIN fretes f ON l.id_frete = f.id_frete
         WHERE l.empresa_id = $1 ${where}
         ORDER BY l.data_hora DESC`, params
      ))
    }

    const results = await Promise.all(queries)
    const rows = results.flatMap(r => r.rows)
    rows.sort((a, b) => new Date(b.data_hora) - new Date(a.data_hora))
    res.json(rows)
  } catch (err) {
    console.error('[Estornos] Erro historico:', err.message)
    res.status(500).json({ error: 'Erro ao buscar historico de estornos' })
  }
})

// GET /api/estornos/historico/passagens
router.get('/historico/passagens', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { data_inicio, data_fim, autorizador } = req.query
    let sql = `SELECT l.*, 'passagem' AS tipo, p.numero_bilhete AS numero
               FROM log_estornos_passagens l
               LEFT JOIN passagens p ON l.id_passagem = p.id_passagem
               WHERE l.empresa_id = $1`
    const params = [empresaId]
    let idx = 2
    if (data_inicio) { sql += ` AND l.data_hora >= $${idx}`; params.push(data_inicio); idx++ }
    if (data_fim) { sql += ` AND l.data_hora <= ($${idx}::date + INTERVAL '1 day')`; params.push(data_fim); idx++ }
    if (autorizador) { sql += ` AND LOWER(l.nome_autorizador) LIKE LOWER($${idx})`; params.push(`%${autorizador}%`); idx++ }
    sql += ' ORDER BY l.data_hora DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Estornos] Erro historico passagens:', err.message)
    res.status(500).json({ error: 'Erro ao buscar historico de estornos de passagens' })
  }
})

// GET /api/estornos/historico/encomendas
router.get('/historico/encomendas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { data_inicio, data_fim, autorizador } = req.query
    let sql = `SELECT l.*, 'encomenda' AS tipo, e.numero_encomenda AS numero
               FROM log_estornos_encomendas l
               LEFT JOIN encomendas e ON l.id_encomenda = e.id_encomenda
               WHERE l.empresa_id = $1`
    const params = [empresaId]
    let idx = 2
    if (data_inicio) { sql += ` AND l.data_hora >= $${idx}`; params.push(data_inicio); idx++ }
    if (data_fim) { sql += ` AND l.data_hora <= ($${idx}::date + INTERVAL '1 day')`; params.push(data_fim); idx++ }
    if (autorizador) { sql += ` AND LOWER(l.nome_autorizador) LIKE LOWER($${idx})`; params.push(`%${autorizador}%`); idx++ }
    sql += ' ORDER BY l.data_hora DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Estornos] Erro historico encomendas:', err.message)
    res.status(500).json({ error: 'Erro ao buscar historico de estornos de encomendas' })
  }
})

// GET /api/estornos/historico/fretes
router.get('/historico/fretes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { data_inicio, data_fim, autorizador } = req.query
    let sql = `SELECT l.*, 'frete' AS tipo, f.numero_frete AS numero
               FROM log_estornos_fretes l
               LEFT JOIN fretes f ON l.id_frete = f.id_frete
               WHERE l.empresa_id = $1`
    const params = [empresaId]
    let idx = 2
    if (data_inicio) { sql += ` AND l.data_hora >= $${idx}`; params.push(data_inicio); idx++ }
    if (data_fim) { sql += ` AND l.data_hora <= ($${idx}::date + INTERVAL '1 day')`; params.push(data_fim); idx++ }
    if (autorizador) { sql += ` AND LOWER(l.nome_autorizador) LIKE LOWER($${idx})`; params.push(`%${autorizador}%`); idx++ }
    sql += ' ORDER BY l.data_hora DESC'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[Estornos] Erro historico fretes:', err.message)
    res.status(500).json({ error: 'Erro ao buscar historico de estornos de fretes' })
  }
})

export default router
