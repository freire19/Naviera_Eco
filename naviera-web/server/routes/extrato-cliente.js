/**
 * Extrato de Cliente Consolidado — junta FRETE + ENCOMENDA + PASSAGEM
 * para um mesmo cliente (por nome), com filtros e baixa unificada.
 *
 * Espelha dao.ExtratoClienteGeralDAO do desktop.
 */
import express from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = express.Router()
router.use(authMiddleware)

/**
 * GET /api/extrato-cliente/clientes
 * Lista nomes unicos: destinatarios (frete + encomenda) + passageiros.
 * Acesso: qualquer usuario autenticado (mesmo padrao das outras rotas financeiras).
 */
router.get('/clientes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    // Junta: destinatarios com movimento + passageiros com passagem +
    //        cadastros (mesmo sem movimento ainda) pra facilitar autocomplete.
    const sql = `
      SELECT DISTINCT nome FROM (
        -- Com movimento:
        SELECT destinatario_nome_temp AS nome FROM fretes
          WHERE empresa_id = $1 AND destinatario_nome_temp IS NOT NULL AND destinatario_nome_temp <> ''
        UNION
        SELECT destinatario AS nome FROM encomendas
          WHERE empresa_id = $1 AND destinatario IS NOT NULL AND destinatario <> ''
        UNION
        SELECT p.nome_passageiro AS nome FROM passageiros p
          INNER JOIN passagens pg ON pg.id_passageiro = p.id_passageiro
          WHERE pg.empresa_id = $1 AND p.nome_passageiro IS NOT NULL AND p.nome_passageiro <> ''
        -- Cadastros (aparecem mesmo sem movimento):
        UNION
        SELECT nome_cliente AS nome FROM cad_clientes_frete
          WHERE empresa_id = $1 AND nome_cliente IS NOT NULL AND nome_cliente <> ''
        UNION
        SELECT nome_cliente AS nome FROM cad_clientes_encomenda
          WHERE empresa_id = $1 AND nome_cliente IS NOT NULL AND nome_cliente <> ''
        UNION
        SELECT nome_passageiro AS nome FROM passageiros
          WHERE empresa_id = $1 AND nome_passageiro IS NOT NULL AND nome_passageiro <> ''
      ) t ORDER BY nome`
    const { rows } = await pool.query(sql, [empresaId])
    res.json(rows.map(r => r.nome))
  } catch (err) {
    console.error('extrato-cliente/clientes:', err)
    res.status(500).json({ error: 'Erro ao listar clientes' })
  }
})

/**
 * GET /api/extrato-cliente/buscar
 * Query params:
 *   cliente (obrigatorio)
 *   viagem_id (opcional)
 *   tipos (csv: frete,encomenda,passagem — omitir = todos)
 *   status (todos | devedores | pagos — default: todos)
 */
router.get('/buscar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const cliente = (req.query.cliente || '').trim()
    if (!cliente) return res.status(400).json({ error: 'cliente obrigatorio' })
    const clienteNorm = cliente.toUpperCase()
    const viagemRaw = req.query.viagem_id
    // viagem_id pode ser: numero (id especifico) | 'agenda' (>= hoje) | vazio (todas)
    const agenda = viagemRaw === 'agenda'
    const viagemId = (!agenda && viagemRaw) ? Number(viagemRaw) : null
    const tiposCsv = (req.query.tipos || 'frete,encomenda,passagem').toLowerCase()
    const incluirFrete = tiposCsv.includes('frete')
    const incluirEnc = tiposCsv.includes('encomenda')
    const incluirPas = tiposCsv.includes('passagem')
    const status = (req.query.status || 'todos').toLowerCase()
    const apenasDevedores = status === 'devedores'
    const apenasPagos = status === 'pagos'

    const tarefas = []
    if (incluirFrete) tarefas.push(buscarFretes(empresaId, clienteNorm, viagemId, apenasDevedores, agenda))
    if (incluirEnc) tarefas.push(buscarEncomendas(empresaId, clienteNorm, viagemId, apenasDevedores, agenda))
    if (incluirPas) tarefas.push(buscarPassagens(empresaId, clienteNorm, viagemId, apenasDevedores, agenda))

    const partes = await Promise.all(tarefas)
    let itens = partes.flat()

    if (apenasPagos) {
      itens = itens.filter(i => Number(i.saldo_devedor || 0) <= 0.01)
    }

    // Ordena por data desc (mais recente primeiro)
    itens.sort((a, b) => {
      if (!a.data_viagem && !b.data_viagem) return 0
      if (!a.data_viagem) return 1
      if (!b.data_viagem) return -1
      return String(b.data_viagem).localeCompare(String(a.data_viagem))
    })

    res.json(itens)
  } catch (err) {
    console.error('extrato-cliente/buscar:', err)
    res.status(500).json({ error: 'Erro ao buscar extrato' })
  }
})

async function buscarFretes(empresaId, cliente, viagemId, apenasDevedores, agenda = false) {
  let sql = `
    SELECT f.id_frete AS id_original, f.numero_frete,
      f.id_viagem, v.data_viagem,
      COALESCE(f.rota_temp, CONCAT(COALESCE(r.origem,''), ' - ', COALESCE(r.destino,''))) AS rota,
      f.remetente_nome_temp AS remetente_ou_origem,
      f.destinatario_nome_temp AS cliente,
      COALESCE(f.valor_total_itens, 0) AS valor_total,
      COALESCE(f.valor_pago, 0) AS valor_pago
    FROM fretes f
    LEFT JOIN viagens v ON v.id_viagem = f.id_viagem
    LEFT JOIN rotas r ON r.id = v.id_rota
    WHERE f.empresa_id = $1 AND COALESCE(f.excluido, false) = false
      AND UPPER(f.destinatario_nome_temp) LIKE '%' || $2 || '%'`
  const params = [empresaId, cliente]
  if (viagemId) { params.push(viagemId); sql += ` AND f.id_viagem = $${params.length}` }
  if (agenda) sql += ` AND v.data_viagem >= CURRENT_DATE`
  if (apenasDevedores) sql += ` AND (COALESCE(f.valor_total_itens,0) - COALESCE(f.valor_pago,0)) > 0.01`
  const { rows } = await pool.query(sql, params)
  return rows.map(r => ({
    tipo: 'FRETE',
    tipo_label: 'Frete',
    id_original: Number(r.id_original),
    numero: r.numero_frete ? String(r.numero_frete) : null,
    id_viagem: r.id_viagem ? Number(r.id_viagem) : null,
    data_viagem: r.data_viagem,
    rota: r.rota,
    remetente_ou_origem: r.remetente_ou_origem,
    cliente: r.cliente,
    descricao: `Frete #${r.numero_frete || '-'}`,
    valor_total: Number(r.valor_total),
    valor_pago: Number(r.valor_pago),
    saldo_devedor: Math.max(0, Number(r.valor_total) - Number(r.valor_pago)),
    status: Math.max(0, Number(r.valor_total) - Number(r.valor_pago)) <= 0.01 ? 'PAGO' : 'PENDENTE'
  }))
}

async function buscarEncomendas(empresaId, cliente, viagemId, apenasDevedores, agenda = false) {
  let sql = `
    SELECT e.id_encomenda AS id_original, e.numero_encomenda,
      e.id_viagem, v.data_viagem,
      COALESCE(e.rota, CONCAT(COALESCE(r.origem,''), ' - ', COALESCE(r.destino,''))) AS rota,
      e.remetente AS remetente_ou_origem, e.destinatario AS cliente,
      e.total_volumes,
      COALESCE(e.total_a_pagar, 0) AS valor_total,
      COALESCE(e.valor_pago, 0) AS valor_pago
    FROM encomendas e
    LEFT JOIN viagens v ON v.id_viagem = e.id_viagem
    LEFT JOIN rotas r ON r.id = v.id_rota
    WHERE e.empresa_id = $1
      AND UPPER(e.destinatario) LIKE '%' || $2 || '%'`
  const params = [empresaId, cliente]
  if (viagemId) { params.push(viagemId); sql += ` AND e.id_viagem = $${params.length}` }
  if (agenda) sql += ` AND v.data_viagem >= CURRENT_DATE`
  if (apenasDevedores) sql += ` AND (COALESCE(e.total_a_pagar,0) - COALESCE(e.valor_pago,0)) > 0.01`
  const { rows } = await pool.query(sql, params)
  return rows.map(r => {
    const total = Number(r.valor_total)
    const pago = Number(r.valor_pago)
    const saldo = Math.max(0, total - pago)
    const vol = r.total_volumes ? ` (${r.total_volumes} vol)` : ''
    return {
      tipo: 'ENCOMENDA',
      tipo_label: 'Encomenda',
      id_original: Number(r.id_original),
      numero: r.numero_encomenda,
      id_viagem: r.id_viagem ? Number(r.id_viagem) : null,
      data_viagem: r.data_viagem,
      rota: r.rota,
      remetente_ou_origem: r.remetente_ou_origem,
      cliente: r.cliente,
      descricao: `Encomenda #${r.numero_encomenda || '-'}${vol}`,
      valor_total: total,
      valor_pago: pago,
      saldo_devedor: saldo,
      status: saldo <= 0.01 ? 'PAGO' : 'PENDENTE'
    }
  })
}

async function buscarPassagens(empresaId, cliente, viagemId, apenasDevedores, agenda = false) {
  let sql = `
    SELECT pg.id_passagem AS id_original, pg.numero_bilhete,
      pg.id_viagem, v.data_viagem,
      CONCAT(COALESCE(r.origem,''), ' - ', COALESCE(r.destino,'')) AS rota,
      p.nome_passageiro AS cliente,
      COALESCE(pg.valor_a_pagar, pg.valor_total, 0) AS valor_total,
      COALESCE(pg.valor_pago, 0) AS valor_pago,
      tp.nome_tipo_passagem, ac.nome_acomodacao
    FROM passagens pg
    INNER JOIN passageiros p ON p.id_passageiro = pg.id_passageiro
    LEFT JOIN viagens v ON v.id_viagem = pg.id_viagem
    LEFT JOIN rotas r ON r.id = pg.id_rota
    LEFT JOIN aux_tipos_passagem tp ON tp.id_tipo_passagem = pg.id_tipo_passagem
    LEFT JOIN aux_acomodacoes ac ON ac.id_acomodacao = pg.id_acomodacao
    WHERE pg.empresa_id = $1
      AND UPPER(p.nome_passageiro) LIKE '%' || $2 || '%'`
  const params = [empresaId, cliente]
  if (viagemId) { params.push(viagemId); sql += ` AND pg.id_viagem = $${params.length}` }
  if (agenda) sql += ` AND v.data_viagem >= CURRENT_DATE`
  if (apenasDevedores) sql += ` AND (COALESCE(pg.valor_a_pagar, pg.valor_total, 0) - COALESCE(pg.valor_pago,0)) > 0.01`
  const { rows } = await pool.query(sql, params)
  return rows.map(r => {
    const total = Number(r.valor_total)
    const pago = Number(r.valor_pago)
    const saldo = Math.max(0, total - pago)
    const extras = [r.nome_tipo_passagem, r.nome_acomodacao].filter(Boolean).join(' / ')
    return {
      tipo: 'PASSAGEM',
      tipo_label: 'Passagem',
      id_original: Number(r.id_original),
      numero: r.numero_bilhete,
      id_viagem: r.id_viagem ? Number(r.id_viagem) : null,
      data_viagem: r.data_viagem,
      rota: r.rota,
      remetente_ou_origem: null,
      cliente: r.cliente,
      descricao: `Bilhete #${r.numero_bilhete || '-'}${extras ? ' (' + extras + ')' : ''}`,
      valor_total: total,
      valor_pago: pago,
      saldo_devedor: saldo,
      status: saldo <= 0.01 ? 'PAGO' : 'PENDENTE'
    }
  })
}

/**
 * POST /api/extrato-cliente/baixa
 * Body: { tipo: 'FRETE'|'ENCOMENDA'|'PASSAGEM', id_original: number, valor: number }
 */
router.post('/baixa', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { tipo, id_original, valor, forma_pagamento } = req.body || {}
    if (!tipo || !id_original) return res.status(400).json({ error: 'tipo e id_original obrigatorios' })
    const valorNum = Number(valor)
    if (!(valorNum > 0)) return res.status(400).json({ error: 'valor deve ser > 0' })
    const forma = (forma_pagamento || 'DINHEIRO').toString().toUpperCase()

    const ok = await aplicarBaixa(pool, String(tipo).toUpperCase(), Number(id_original), valorNum, forma, empresaId)
    if (!ok) return res.status(404).json({ error: 'registro nao encontrado' })
    res.json({ ok: true, tipo, id_original, valor: valorNum, forma_pagamento: forma })
  } catch (err) {
    console.error('extrato-cliente/baixa:', err)
    res.status(500).json({ error: 'Erro ao dar baixa' })
  }
})

async function aplicarBaixa(db, tipo, idOriginal, valorNum, forma, empresaId) {
  let sql
  switch (tipo) {
    case 'FRETE':
      sql = `UPDATE fretes SET valor_pago = COALESCE(valor_pago,0) + $1,
               valor_devedor = GREATEST(COALESCE(valor_total_itens,0) - (COALESCE(valor_pago,0) + $1), 0),
               tipo_pagamento = COALESCE($4, tipo_pagamento)
             WHERE id_frete = $2 AND empresa_id = $3`
      break
    case 'ENCOMENDA':
      sql = `UPDATE encomendas SET valor_pago = COALESCE(valor_pago,0) + $1,
               status_pagamento = CASE WHEN (COALESCE(valor_pago,0) + $1) >= COALESCE(total_a_pagar,0) THEN 'PAGO' ELSE 'PARCIAL' END,
               forma_pagamento = COALESCE($4, forma_pagamento)
             WHERE id_encomenda = $2 AND empresa_id = $3`
      break
    case 'PASSAGEM':
      // passagens.id_forma_pagamento eh FK pra aux_formas_pagamento — nao atualiza
      // (deixa o que ja estava). Atualiza so o valor_pago + devedor.
      sql = `UPDATE passagens SET valor_pago = COALESCE(valor_pago,0) + $1,
               valor_devedor = GREATEST(COALESCE(valor_a_pagar, valor_total, 0) - (COALESCE(valor_pago,0) + $1), 0)
             WHERE id_passagem = $2 AND empresa_id = $3`
      // ignora $4 propositalmente
      const { rowCount: rcP } = await db.query(sql, [valorNum, idOriginal, empresaId])
      return rcP === 1
    default:
      return false
  }
  const { rowCount } = await db.query(sql, [valorNum, idOriginal, empresaId, forma])
  return rowCount === 1
}

/**
 * POST /api/extrato-cliente/quitar-tudo
 * Body: { itens: [{tipo, id_original, valor}, ...] }
 * Usado pra botao "Quitar Tudo em Aberto" no front.
 */
router.post('/quitar-tudo', async (req, res) => {
  const { itens, forma_pagamento } = req.body || {}
  if (!Array.isArray(itens) || itens.length === 0) return res.status(400).json({ error: 'itens vazio' })
  const forma = (forma_pagamento || 'DINHEIRO').toString().toUpperCase()
  const client = await pool.connect()
  try {
    await client.query('BEGIN')
    const empresaId = req.user.empresa_id
    let sucesso = 0
    for (const it of itens) {
      const valor = Number(it.valor)
      if (!(valor > 0)) continue
      const ok = await aplicarBaixa(client, String(it.tipo).toUpperCase(), Number(it.id_original), valor, forma, empresaId)
      if (ok) sucesso += 1
    }
    await client.query('COMMIT')
    res.json({ ok: true, sucesso, total_itens: itens.length, forma_pagamento: forma })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('extrato-cliente/quitar-tudo:', err)
    res.status(500).json({ error: 'Erro ao quitar lancamentos' })
  } finally {
    client.release()
  }
})

export default router
