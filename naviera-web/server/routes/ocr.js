import { Router } from 'express'
import multer from 'multer'
import path from 'path'
import { readFile, unlink } from 'fs/promises'
import { existsSync, mkdirSync } from 'fs'
import jwt from 'jsonwebtoken'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { rateLimit } from '../middleware/rateLimit.js'
import { callVisionOCR } from '../helpers/visionApi.js'
import { parseOcrText } from '../helpers/parseOcrText.js'
import { criarFreteComItens } from '../helpers/criarFrete.js'
import { geminiParseOCR } from '../helpers/geminiParser.js'
import { geminiVisionAnalyze } from '../helpers/geminiVision.js'
import { geminiParseEncomenda, geminiParseLote } from '../helpers/geminiEncomendaParser.js'
import { OCR_STATUS, EDITAVEIS } from '../helpers/ocrStatus.js'
import log from '../logger.js'

const router = Router()

// DS4-015 fix: auth obrigatoria em TODAS as rotas (incluindo foto)
// Antes: foto bypassava auth e aceitava JWT via query param (expoe token em logs)
router.use(authMiddleware)

// Rate limiter por usuario para upload OCR (consome APIs pagas: Vision + Gemini)
const uploadLimiter = rateLimit({
  windowMs: 60000,
  max: 5,
  message: 'Limite de uploads atingido (max 5/min). Aguarde um momento.',
  keyFn: (req) => `ocr-upload:${req.user?.id || req.ip}`
})

// Rate limiter para re-analise IA (consome Gemini API paga)
const iaLimiter = rateLimit({
  windowMs: 60000,
  max: 10,
  message: 'Limite de analises IA atingido (max 10/min). Aguarde um momento.',
  keyFn: (req) => `ocr-ia:${req.user?.id || req.ip}`
})

// Configurar multer para upload de fotos
const UPLOAD_PATH = process.env.OCR_UPLOAD_PATH || path.resolve('uploads/ocr')

/** Verifica que o caminho resolvido esta dentro de UPLOAD_PATH (previne path traversal) */
function assertSafePath(relativePath) {
  const full = path.resolve(UPLOAD_PATH, relativePath)
  if (!full.startsWith(path.resolve(UPLOAD_PATH))) return null
  return full
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const dir = path.join(UPLOAD_PATH, String(req.user.empresa_id))
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
    cb(null, dir)
  },
  filename: (req, file, cb) => {
    // DS4-029 fix: forcar extensao por mimetype (antes: user-controlled originalname)
    const extMap = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' }
    const ext = extMap[file.mimetype] || '.jpg'
    const name = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}${ext}`
    cb(null, name)
  }
})

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB
  fileFilter: (req, file, cb) => {
    const allowed = ['image/jpeg', 'image/png', 'image/webp']
    cb(null, allowed.includes(file.mimetype))
  }
})

// Upload multiplo para fretes (ate 10 paginas) + single para encomenda/lote
const uploadMulti = upload.fields([
  { name: 'foto', maxCount: 1 },
  { name: 'fotos', maxCount: 10 }
])

// ============================================================================
// POST /api/ocr/upload — Upload foto(s), rodar OCR, salvar lancamento
// ============================================================================
router.post('/upload', uploadLimiter, uploadMulti, async (req, res) => {
  // Compatibilidade: aceita 'foto' (single) ou 'fotos' (multiplas)
  const allFiles = [
    ...(req.files?.fotos || []),
    ...(req.files?.foto || [])
  ]
  if (allFiles.length === 0) {
    return res.status(400).json({ error: 'Foto obrigatoria (campo "foto" ou "fotos", max 10MB, jpeg/png/webp)' })
  }

  const empresaId = req.user.empresa_id
  const { viagem_id, tipo, client_uuid, num_notafiscal } = req.body
  const tipoLanc = ['encomenda', 'lote'].includes(tipo) ? tipo : 'frete'

  // Para backward compat, req.file aponta para o primeiro arquivo
  const primaryFile = allFiles[0]

  try {
    let ocrResult = { text: '', confidence: 0, fullResponse: null }
    let dados

    if (tipoLanc === 'lote') {
      // LOTE: foto de protocolo com N encomendas separadas
      try {
        ocrResult = await callVisionOCR(primaryFile.path)
      } catch (err) {
        log.error('OCR', 'Vision OCR falhou para lote', { empresa_id: empresaId, erro: err.message })
      }

      if (!ocrResult.text || ocrResult.text.length < 30) {
        return res.status(400).json({ error: 'Nao foi possivel ler texto na foto. Use uma foto do protocolo manuscrito.' })
      }

      const padrao = await pool.query(
        'SELECT nome_item, preco_unitario_padrao AS preco_padrao FROM itens_encomenda_padrao WHERE empresa_id = $1 AND ativo = TRUE',
        [empresaId]
      )

      const loteResult = await geminiParseLote(ocrResult.text, padrao.rows)
      log.info('OCR', 'Lote parser OK', { empresa_id: empresaId, encomendas: loteResult.encomendas.length })

      // Criar N lancamentos — 1 por encomenda
      const fotoRelPath = path.relative(UPLOAD_PATH, primaryFile.path)
      const loteUuid = crypto.randomUUID()
      const lancamentos = []

      for (let i = 0; i < loteResult.encomendas.length; i++) {
        const enc = loteResult.encomendas[i]
        const encUuid = crypto.randomUUID()
        const encDados = { ...enc, rota: enc.rota || loteResult.rota || '' }

        const result = await pool.query(`
          INSERT INTO ocr_lancamentos (uuid, empresa_id, id_viagem, foto_path, foto_original_name,
            ocr_texto_bruto, ocr_confianca, dados_extraidos,
            status, id_usuario_criou, nome_usuario_criou, tipo, lote_uuid, lote_index)
          VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'pendente', $9, $10, 'lote', $11, $12)
          ON CONFLICT (uuid) DO UPDATE SET uuid = ocr_lancamentos.uuid
          RETURNING *
        `, [
          encUuid, empresaId, viagem_id || null, fotoRelPath, primaryFile.originalname,
          ocrResult.text, ocrResult.confidence, JSON.stringify(encDados),
          req.user.id, req.user.login || null, loteUuid, i
        ])
        lancamentos.push({ lancamento: result.rows[0], dados_extraidos: encDados })
      }

      return res.status(201).json({
        tipo: 'lote',
        lote_uuid: loteUuid,
        lancamentos,
        ocr_confianca: ocrResult.confidence
      })

    } else if (tipoLanc === 'encomenda') {
      // ENCOMENDA: primeiro tenta Vision OCR para detectar texto
      // Se detectar texto substancial → parser de encomenda
      // Se pouco/nenhum texto → Gemini Vision (foto de item fisico)
      try {
        ocrResult = await callVisionOCR(primaryFile.path)
      } catch (err) {
        log.warn('OCR', 'Vision OCR falhou para encomenda, tentando Gemini Vision', { empresa_id: empresaId, erro: err.message })
      }

      const padrao = await pool.query(
        'SELECT nome_item, preco_unitario_padrao AS preco_padrao FROM itens_encomenda_padrao WHERE empresa_id = $1 AND ativo = TRUE',
        [empresaId]
      )

      if (ocrResult.text && ocrResult.text.length >= 30) {
        try {
          dados = await geminiParseEncomenda(ocrResult.text, padrao.rows)
          log.info('OCR', 'Gemini encomenda parser OK', { empresa_id: empresaId, itens: dados.itens?.length || 0 })
        } catch (geminiErr) {
          log.warn('OCR', 'Gemini encomenda parser falhou, tentando Vision', { empresa_id: empresaId, erro: geminiErr.message })
          dados = await geminiVisionAnalyze(primaryFile.path, padrao.rows)
        }
      } else {
        try {
          dados = await geminiVisionAnalyze(primaryFile.path, padrao.rows)
          ocrResult.confidence = 85
          log.info('OCR', 'Gemini Vision (encomenda item) OK', { empresa_id: empresaId, itens: dados.itens?.length || 0 })
        } catch (err) {
          log.error('OCR', 'Gemini Vision falhou', { empresa_id: empresaId, erro: err.message })
          dados = { remetente: '', destinatario: '', itens: [], total_volumes: 0, total_a_pagar: 0, observacoes: 'Falha na analise — preencha manualmente' }
        }
      }
    } else {
      // FRETE: Google Vision OCR + Gemini multimodal
      // Suporte a multiplas paginas: OCR cada foto e combinar texto
      const ocrTexts = []
      let totalConfidence = 0
      let firstFullResponse = null

      for (const file of allFiles) {
        try {
          const pageResult = await callVisionOCR(file.path)
          if (pageResult.text) ocrTexts.push(pageResult.text)
          totalConfidence += pageResult.confidence || 0
          if (!firstFullResponse) firstFullResponse = pageResult.fullResponse
        } catch (err) {
          log.warn('OCR', `Vision API falhou para pagina ${file.originalname}`, { empresa_id: empresaId, erro: err.message })
        }
      }

      ocrResult = {
        text: ocrTexts.join('\n\n--- PAGINA ---\n\n'),
        confidence: ocrTexts.length > 0 ? Math.round(totalConfidence / allFiles.length) : 0,
        fullResponse: firstFullResponse
      }

      if (allFiles.length > 1) {
        log.info('OCR', `Frete multi-pagina: ${allFiles.length} fotos, ${ocrTexts.length} com texto`, { empresa_id: empresaId })
      }

      const padrao = await pool.query(
        'SELECT nome_item, preco_unitario_padrao, preco_unitario_desconto FROM itens_frete_padrao WHERE empresa_id = $1 AND ativo = TRUE',
        [empresaId]
      )

      if (ocrResult.text) {
        try {
          // Modo multimodal: envia primeira imagem + texto combinado para detectar numero_nota e marca-texto
          dados = await geminiParseOCR(ocrResult.text, padrao.rows, primaryFile.path)
          log.info('OCR', 'Gemini parser OK', {
            empresa_id: empresaId, itens: dados.itens?.length || 0,
            numero_nota: dados.numero_nota || null,
            modo_marcador: dados.modo_marcador || false
          })
        } catch (geminiErr) {
          log.warn('OCR', 'Gemini falhou, usando regex', { empresa_id: empresaId, erro: geminiErr.message })
          dados = parseOcrText(ocrResult.text, padrao.rows)
        }
      } else {
        dados = parseOcrText('', padrao.rows)
      }
    }

    // Numero da nota informado manualmente pelo usuario prevalece sobre o detectado via OCR
    if (tipoLanc === 'frete' && num_notafiscal && String(num_notafiscal).trim()) {
      if (!dados || typeof dados !== 'object') dados = {}
      dados.numero_nota = String(num_notafiscal).trim()
    }

    // Salvar no banco (com idempotencia via client_uuid)
    const fotoRelPath = path.relative(UPLOAD_PATH, primaryFile.path)
    const uuid = client_uuid || crypto.randomUUID()

    // ON CONFLICT: se o mesmo uuid ja existe, retorna o registro existente sem duplicar
    const result = await pool.query(`
      INSERT INTO ocr_lancamentos (uuid, empresa_id, id_viagem, foto_path, foto_original_name,
        ocr_texto_bruto, ocr_json, ocr_confianca, dados_extraidos,
        status, id_usuario_criou, nome_usuario_criou, tipo)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'pendente', $10, $11, $12)
      ON CONFLICT (uuid) DO UPDATE SET uuid = ocr_lancamentos.uuid
      RETURNING *
    `, [
      uuid,
      empresaId,
      viagem_id || null,
      fotoRelPath,
      primaryFile.originalname,
      ocrResult.text || null,
      JSON.stringify(ocrResult.fullResponse),
      ocrResult.confidence,
      JSON.stringify(dados),
      req.user.id,
      req.user.login || null,
      tipoLanc
    ])

    res.status(201).json({
      lancamento: result.rows[0],
      dados_extraidos: dados,
      ocr_confianca: ocrResult.confidence
    })
  } catch (err) {
    // Limpar todos os arquivos em caso de erro
    for (const f of allFiles) {
      if (f?.path) await unlink(f.path).catch(() => {})
    }
    log.error('OCR', 'Erro no upload', { empresa_id: req.user?.empresa_id, erro: err.message })
    res.status(500).json({ error: 'Erro ao processar foto' })
  }
})

// ============================================================================
// GET /api/ocr/lancamentos — Listar lancamentos OCR da empresa
// ============================================================================
router.get('/lancamentos', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { status, viagem_id } = req.query
    const limit = Math.min(parseInt(req.query.limit) || 30, 100)
    const offset = Math.max(parseInt(req.query.offset) || 0, 0)

    let sql = 'SELECT id, uuid, id_viagem, id_frete, id_encomenda, tipo, foto_original_name, ocr_confianca, dados_extraidos, dados_revisados, status, motivo_rejeicao, nome_usuario_criou, nome_usuario_revisou, data_revisao, criado_em FROM ocr_lancamentos WHERE empresa_id = $1'
    let countSql = 'SELECT COUNT(*) FROM ocr_lancamentos WHERE empresa_id = $1'
    const params = [empresaId]
    const countParams = [empresaId]
    let idx = 2

    if (status) {
      const clause = ` AND status = $${idx++}`
      sql += clause
      countSql += clause
      params.push(status)
      countParams.push(status)
    }
    if (viagem_id) {
      const clause = ` AND id_viagem = $${idx++}`
      sql += clause
      countSql += clause
      params.push(viagem_id)
      countParams.push(viagem_id)
    }

    sql += ` ORDER BY criado_em DESC LIMIT $${idx++} OFFSET $${idx++}`
    params.push(limit, offset)

    const [result, countResult] = await Promise.all([
      pool.query(sql, params),
      pool.query(countSql, countParams)
    ])
    const total = parseInt(countResult.rows[0].count)
    res.json({ data: result.rows, total, limit, offset })
  } catch (err) {
    log.error('OCR', 'Erro ao listar', { empresa_id: req.user?.empresa_id, erro: err.message })
    res.status(500).json({ error: 'Erro ao listar lancamentos OCR' })
  }
})

// ============================================================================
// GET /api/ocr/lancamentos/:id — Detalhe de um lancamento
// ============================================================================
router.get('/lancamentos/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT * FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Lancamento nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar lancamento' })
  }
})

// ============================================================================
// POST /api/ocr/lancamentos/:id/ia-review — Gemini AI reanalisa o texto OCR
// ============================================================================
router.post('/lancamentos/:id/ia-review', iaLimiter, async (req, res) => {
  try {
    const empresaId = req.user.empresa_id

    // Buscar lancamento
    const lancResult = await pool.query(
      'SELECT id, ocr_texto_bruto, tipo, foto_path FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (lancResult.rows.length === 0) {
      return res.status(404).json({ error: 'Lancamento nao encontrado' })
    }

    const { ocr_texto_bruto, tipo, foto_path } = lancResult.rows[0]
    let dados

    if (tipo === 'encomenda' || tipo === 'lote') {
      const padrao = await pool.query(
        'SELECT nome_item, preco_unitario_padrao AS preco_padrao FROM itens_encomenda_padrao WHERE empresa_id = $1 AND ativo = TRUE',
        [empresaId]
      )
      if (ocr_texto_bruto && ocr_texto_bruto.length >= 30) {
        // Protocolo manuscrito — re-parsear texto
        dados = await geminiParseEncomenda(ocr_texto_bruto, padrao.rows)
      } else {
        // Item fisico — re-analisar imagem
        const fullPath = path.resolve(UPLOAD_PATH, foto_path)
        if (!existsSync(fullPath)) return res.status(404).json({ error: 'Foto nao encontrada' })
        dados = await geminiVisionAnalyze(fullPath, padrao.rows)
      }
    } else {
      // Frete: Gemini multimodal (imagem + texto) para detectar numero_nota e marca-texto
      if (!ocr_texto_bruto) return res.status(400).json({ error: 'Sem texto OCR para analisar' })

      const padrao = await pool.query(
        'SELECT nome_item, preco_unitario_padrao, preco_unitario_desconto FROM itens_frete_padrao WHERE empresa_id = $1 AND ativo = TRUE',
        [empresaId]
      )

      // Tentar multimodal com imagem (detecta numero_nota + marca-texto)
      let imgPath = null
      if (foto_path) {
        const fullPath = path.resolve(UPLOAD_PATH, foto_path)
        if (existsSync(fullPath)) imgPath = fullPath
      }
      dados = await geminiParseOCR(ocr_texto_bruto, padrao.rows, imgPath)
    }

    // Atualizar dados extraidos no banco — #DB125: filtrar por empresa_id
    await pool.query(
      'UPDATE ocr_lancamentos SET dados_extraidos = $1 WHERE id = $2 AND empresa_id = $3',
      [JSON.stringify(dados), req.params.id, empresaId]
    )

    res.json({ dados_extraidos: dados })
  } catch (err) {
    log.error('OCR', 'Erro na revisao IA', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    // DS4-030 fix: mensagem generica (antes: err.message podia vazar API keys/paths)
    res.status(500).json({ error: 'Erro ao processar com IA. Tente novamente.' })
  }
})

// ============================================================================
// PUT /api/ocr/lancamentos/:id/revisar — Operador confirma/corrige dados
// ============================================================================
router.put('/lancamentos/:id/revisar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { dados_revisados } = req.body
    if (!dados_revisados) {
      return res.status(400).json({ error: 'dados_revisados obrigatorio' })
    }

    // Validacao de payload: prevenir JSON arbitrariamente grande
    const jsonStr = JSON.stringify(dados_revisados)
    if (jsonStr.length > 512_000) {
      return res.status(400).json({ error: 'Payload muito grande (max 500KB)' })
    }
    if (Array.isArray(dados_revisados.itens) && dados_revisados.itens.length > 200) {
      return res.status(400).json({ error: 'Limite de 200 itens por lancamento' })
    }

    const result = await pool.query(`
      UPDATE ocr_lancamentos SET dados_revisados = $1, status = 'revisado_operador'
      WHERE id = $2 AND empresa_id = $3 AND status IN ('pendente', 'revisado_operador')
      RETURNING *
    `, [JSON.stringify(dados_revisados), req.params.id, empresaId])

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Lancamento nao encontrado ou status invalido' })
    }
    res.json(result.rows[0])
  } catch (err) {
    log.error('OCR', 'Erro ao revisar', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    res.status(500).json({ error: 'Erro ao revisar lancamento' })
  }
})

// ============================================================================
// PUT /api/ocr/lancamentos/:id — Salvar edicoes dos dados extraidos antes de aprovar
// Atualiza AMBOS dados_extraidos e dados_revisados para que o aprovar use os dados corrigidos
router.put('/lancamentos/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { dados_extraidos } = req.body
    if (!dados_extraidos) return res.status(400).json({ error: 'dados_extraidos obrigatorio' })
    const jsonStr = JSON.stringify(dados_extraidos)
    const result = await pool.query(
      `UPDATE ocr_lancamentos
       SET dados_extraidos = $1, dados_revisados = $1,
           status = CASE WHEN status = 'pendente' THEN 'revisado_operador' ELSE status END
       WHERE id = $2 AND empresa_id = $3 AND status IN ('pendente', 'revisado_operador')
       RETURNING id`,
      [jsonStr, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Lancamento nao encontrado' })
    res.json({ ok: true })
  } catch (err) {
    console.error('[OCR] Erro ao salvar edicoes:', err.message)
    res.status(500).json({ error: 'Erro ao salvar edicoes' })
  }
})

// PUT /api/ocr/lancamentos/:id/aprovar — Conferente aprova → cria frete real
// ============================================================================
router.put('/lancamentos/:id/aprovar', async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id

    await client.query('BEGIN')

    // Buscar lancamento
    const lancResult = await client.query(
      'SELECT * FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2 AND status = $3 FOR UPDATE',
      [req.params.id, empresaId, 'revisado_operador']
    )
    if (lancResult.rows.length === 0) {
      await client.query('ROLLBACK')
      return res.status(404).json({ error: 'Lancamento nao encontrado ou nao esta revisado' })
    }

    const lanc = lancResult.rows[0]
    const dados = lanc.dados_revisados || lanc.dados_extraidos
    const { auto_cadastrar } = req.body || {}

    // Verificar se remetente/destinatario existem em cad_clientes_encomenda
    const clientesFaltantes = []
    for (const campo of ['remetente', 'destinatario']) {
      const nome = (dados[campo] || '').trim()
      if (!nome) continue
      const existe = await client.query(
        'SELECT id_cliente FROM cad_clientes_encomenda WHERE LOWER(nome_cliente) = LOWER($1) AND empresa_id = $2',
        [nome, empresaId]
      )
      if (existe.rows.length === 0) clientesFaltantes.push({ campo, nome })
    }

    if (clientesFaltantes.length > 0 && !auto_cadastrar) {
      await client.query('ROLLBACK')
      return res.status(409).json({
        error: 'Clientes nao cadastrados',
        clientes_faltantes: clientesFaltantes,
        mensagem: clientesFaltantes.map(c => `${c.campo === 'remetente' ? 'Remetente' : 'Destinatario'}: "${c.nome}"`).join(', ') + ' — nao cadastrado(s). Deseja cadastrar automaticamente?'
      })
    }

    // Auto-cadastrar clientes faltantes se solicitado
    for (const c of clientesFaltantes) {
      await client.query(
        'INSERT INTO cad_clientes_encomenda (nome_cliente, empresa_id) VALUES ($1, $2) ON CONFLICT (empresa_id, nome_cliente) DO NOTHING',
        [(c.nome || '').toUpperCase(), empresaId]
      )
    }

    let resultado

    if (lanc.tipo === 'encomenda' || lanc.tipo === 'lote') {
      // ENCOMENDA: criar registro em encomendas + encomenda_itens
      const totalAPagar = dados.total_a_pagar || dados.valor_total || (dados.itens || []).reduce((s, i) => s + ((i.subtotal || i.valor_total || 0) || (i.quantidade || 1) * (i.preco_unitario || i.valor_unitario || 0)), 0)
      const totalVolumes = dados.total_volumes || (dados.itens || []).reduce((s, i) => s + (i.quantidade || 1), 0)

      await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
      const seqResult = await client.query(
        `SELECT COALESCE(MAX(CASE WHEN numero_encomenda ~ '^[0-9]+$' THEN numero_encomenda::INTEGER END), 0) + 1 AS next_num FROM encomendas WHERE empresa_id = $1`,
        [empresaId]
      )
      const numEncomenda = seqResult.rows[0].next_num

      const encResult = await client.query(`
        INSERT INTO encomendas (id_viagem, numero_encomenda, remetente, destinatario, observacoes,
          total_volumes, total_a_pagar, valor_pago, desconto, status_pagamento,
          forma_pagamento, entregue, rota, data_lancamento, empresa_id)
        VALUES ($1,$2,$3,$4,$5,$6,$7,0,0,'PENDENTE',NULL,FALSE,$8,CURRENT_DATE,$9)
        RETURNING *
      `, [
        lanc.id_viagem, numEncomenda, (dados.remetente || '').toUpperCase() || null, (dados.destinatario || '').toUpperCase() || null,
        `[OCR #${lanc.id}] ${dados.observacoes || ''}`.trim(),
        totalVolumes, totalAPagar, dados.rota || null, empresaId
      ])

      const encId = encResult.rows[0].id_encomenda
      for (const item of (dados.itens || [])) {
        await client.query(`
          INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total)
          VALUES ($1, $2, $3, $4, $5)
        `, [encId, item.quantidade || 1, item.nome_item || item.descricao || '', item.preco_unitario || item.valor_unitario || 0, item.subtotal || item.valor_total || (item.quantidade || 1) * (item.preco_unitario || item.valor_unitario || 0)])
      }

      // Salvar itens novos no catalogo de encomendas
      for (const item of (dados.itens || [])) {
        const nomeItem = (item.nome_item || item.descricao || '').trim().toUpperCase()
        if (nomeItem) {
          await client.query(
            'INSERT INTO itens_encomenda_padrao (nome_item, preco_unitario_padrao, ativo, empresa_id) VALUES ($1, $2, TRUE, $3) ON CONFLICT DO NOTHING',
            [nomeItem, item.preco_unitario || item.valor_unitario || 0, empresaId]
          )
        }
      }

      await client.query(`
        UPDATE ocr_lancamentos SET status = 'aprovado', id_encomenda = $1,
          id_usuario_revisou = $2, nome_usuario_revisou = $3, data_revisao = CURRENT_TIMESTAMP
        WHERE id = $4 AND empresa_id = $5
      `, [encId, req.user.id, req.user.login, lanc.id, empresaId])

      resultado = { lancamento_id: lanc.id, encomenda: encResult.rows[0] }
    } else {
      // FRETE: fluxo original
      const valor_total_itens = dados.itens
        ? dados.itens.reduce((sum, i) => sum + (i.subtotal || i.preco_unitario * i.quantidade || 0), 0)
        : 0

      const fretePayload = {
        id_viagem: lanc.id_viagem,
        remetente_nome_temp: dados.remetente || null,
        destinatario_nome_temp: dados.destinatario || null,
        rota_temp: dados.rota || null,
        conferente_temp: dados.conferente || null,
        observacoes: `[OCR #${lanc.id}] ${dados.observacoes || ''}`.trim(),
        valor_total_itens,
        desconto: 0,
        valor_pago: 0,
        itens: (dados.itens || []).map(i => ({
          nome_item: i.nome_item,
          quantidade: i.quantidade,
          preco_unitario: i.preco_unitario,
          subtotal_item: i.subtotal || i.preco_unitario * i.quantidade
        }))
      }

      const frete = await criarFreteComItens(client, empresaId, fretePayload)

      // Salvar itens novos no catalogo de fretes
      // Regra desconto: estiva (3.80) e fardaria (4.50) tem R$0.20 de desconto, demais = preco normal
      for (const item of (dados.itens || [])) {
        const nomeItem = (item.nome_item || '').trim().toUpperCase()
        if (nomeItem) {
          const preco = parseFloat(item.preco_unitario) || 0
          const precoDesc = (preco === 3.80 || preco === 4.50) ? preco - 0.20 : preco
          await client.query(
            'INSERT INTO itens_frete_padrao (nome_item, preco_unitario_padrao, preco_unitario_desconto, ativo, empresa_id) VALUES ($1, $2, $3, TRUE, $4) ON CONFLICT DO NOTHING',
            [nomeItem, preco, precoDesc, empresaId]
          )
        }
      }

      await client.query(`
        UPDATE ocr_lancamentos SET status = 'aprovado', id_frete = $1,
          id_usuario_revisou = $2, nome_usuario_revisou = $3, data_revisao = CURRENT_TIMESTAMP
        WHERE id = $4 AND empresa_id = $5
      `, [frete.id_frete, req.user.id, req.user.login, lanc.id, empresaId])

      resultado = { lancamento_id: lanc.id, frete }
    }

    await client.query('COMMIT')

    res.json(resultado)
  } catch (err) {
    await client.query('ROLLBACK')
    log.error('OCR', 'Erro ao aprovar', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    res.status(500).json({ error: 'Erro ao aprovar lancamento' })
  } finally {
    client.release()
  }
})

// ============================================================================
// PUT /api/ocr/lancamentos/:id/rejeitar — Conferente rejeita
// ============================================================================
router.put('/lancamentos/:id/rejeitar', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { motivo } = req.body

    const result = await pool.query(`
      UPDATE ocr_lancamentos SET status = 'rejeitado', motivo_rejeicao = $1,
        id_usuario_revisou = $2, nome_usuario_revisou = $3, data_revisao = CURRENT_TIMESTAMP
      WHERE id = $4 AND empresa_id = $5 AND status IN ('pendente', 'revisado_operador')
      RETURNING *
    `, [motivo || null, req.user.id, req.user.login, req.params.id, empresaId])

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Lancamento nao encontrado ou status invalido' })
    }
    res.json(result.rows[0])
  } catch (err) {
    log.error('OCR', 'Erro ao rejeitar', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    res.status(500).json({ error: 'Erro ao rejeitar lancamento' })
  }
})

// ============================================================================
// POST /api/ocr/lancamentos/:id/adicionar-foto — Processa foto extra (item OU documento)
// Detecta automaticamente: se for RG/CNH/CPF extrai nome+numero do remetente
// Se for item fisico, extrai itens normalmente
// ============================================================================
// cpf e cnpj removidos: aparecem em NFC-e e causavam falsos positivos
const DOC_KEYWORDS = /\b(registro\s*geral|identidade|habilitac|rg[\s:.-]|cnh|cart\s*de\s*ident|orgao\s*emissor|data\s*de\s*nascimento|filiacao|naturalidade|doc\.?\s*de\s*identidade|republica\s*federativa)\b/i

router.post('/lancamentos/:id/adicionar-foto', upload.single('foto'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Foto obrigatoria' })

  try {
    const empresaId = req.user.empresa_id

    const lancResult = await pool.query(
      `SELECT id, tipo FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2 AND status IN ('pendente', 'revisado_operador')`,
      [req.params.id, empresaId]
    )
    if (lancResult.rows.length === 0) {
      await unlink(req.file.path).catch(() => {})
      return res.status(404).json({ error: 'Lancamento nao encontrado ou status invalido' })
    }

    // Vision OCR primeiro para detectar se e documento
    let ocrText = ''
    try {
      const ocrResult = await callVisionOCR(req.file.path)
      ocrText = ocrResult.text || ''
    } catch { /* sem texto — provavelmente item fisico */ }

    const isDocumento = DOC_KEYWORDS.test(ocrText)

    if (isDocumento) {
      // DOCUMENTO: extrair nome + numero via Gemini
      const docPrompt = `Analise este texto extraido de um DOCUMENTO DE IDENTIDADE brasileiro (RG, CNH, CPF, ou similar).

Extraia APENAS:
1. Nome completo da pessoa
2. Numero do CPF (formato 000.000.000-00) — PRIORIDADE MAXIMA
3. Se nao houver CPF, extraia o numero do RG
4. NUNCA use o numero de registro da CNH como documento. Na CNH, o CPF aparece no campo "CPF" e o RG no campo "Doc. Identidade / Org. Emissor". O numero grande no topo da CNH e o registro da CNH, NAO use esse.
5. Tipo do documento identificado

PRIORIDADE de numero: CPF > RG > outros

Texto OCR:
"""
${ocrText}
"""

Responda APENAS com JSON valido (sem markdown):
{"nome": "NOME COMPLETO", "cpf": "000.000.000-00", "rg": "0000000", "tipo_doc": "CNH"}`

      const { fetchWithRetry: fetchRetry } = await import('../helpers/fetchWithRetry.js')
      const apiKey = process.env.GEMINI_API_KEY
      const geminiRes = await fetchRetry(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=${apiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: docPrompt }] }],
            generationConfig: { temperature: 0.1, maxOutputTokens: 2048 }
          }),
          signal: AbortSignal.timeout(30000)
        }
      )
      const geminiData = await geminiRes.json()
      const parts = geminiData.candidates?.[0]?.content?.parts || []
      const jsonText = parts.find(p => p.text?.includes('{'))?.text || ''
      const jsonMatch = jsonText.match(/\{[\s\S]*\}/)
      let docInfo = { nome: '', cpf: '', rg: '', tipo_doc: '' }
      if (jsonMatch) {
        try { docInfo = JSON.parse(jsonMatch[0]) } catch {}
      }

      // Montar numero_doc priorizando CPF > RG
      const cpf = (docInfo.cpf || '').trim()
      const rg = (docInfo.rg || docInfo.numero_doc || '').trim()

      // Mover foto para subpasta segura docs/{empresa_id}/
      const docsDir = path.join(UPLOAD_PATH, 'docs', String(empresaId))
      if (!existsSync(docsDir)) mkdirSync(docsDir, { recursive: true })
      const docFilename = path.basename(req.file.path)
      const docDestPath = path.join(docsDir, docFilename)
      const { rename } = await import('fs/promises')
      await rename(req.file.path, docDestPath)
      const fotoRelPath = path.relative(UPLOAD_PATH, docDestPath)

      log.info('OCR', 'Documento identificado', {
        empresa_id: empresaId, lancamento_id: req.params.id,
        tipo_doc: docInfo.tipo_doc, nome: docInfo.nome, cpf, rg
      })

      return res.json({
        tipo: 'documento',
        nome: docInfo.nome || '',
        cpf,
        rg,
        tipo_doc: docInfo.tipo_doc || '',
        foto_doc_path: fotoRelPath
      })
    }

    // ITEM FISICO: fluxo normal
    const padrao = await pool.query(
      'SELECT nome_item, preco_unitario_padrao AS preco_padrao FROM itens_encomenda_padrao WHERE empresa_id = $1 AND ativo = TRUE',
      [empresaId]
    )
    const dados = await geminiVisionAnalyze(req.file.path, padrao.rows)
    log.info('OCR', 'Foto adicional (item)', { empresa_id: empresaId, lancamento_id: req.params.id, itens: dados.itens?.length || 0 })

    res.json({
      tipo: 'item',
      itens: dados.itens || [],
      observacoes: dados.observacoes || ''
    })
  } catch (err) {
    if (req.file?.path) await unlink(req.file.path).catch(() => {})
    log.error('OCR', 'Erro ao processar foto adicional', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    res.status(500).json({ error: 'Erro ao processar foto' })
  }
})

// ============================================================================
// GET /api/ocr/documentos — Listar todos documentos arquivados (ADMIN ONLY)
// Retorna lancamentos que possuem doc_remetente, agrupados por categoria
// ============================================================================
router.get('/documentos', async (req, res) => {
  try {
    const funcao = (req.user.funcao || '').toLowerCase()
    if (funcao !== 'administrador' && funcao !== 'admin' && funcao !== 'gerente') {
      return res.status(403).json({ error: 'Acesso restrito a administradores da empresa' })
    }
    const empresaId = req.user.empresa_id
    const { categoria } = req.query

    let sql = `SELECT id, tipo, status, dados_revisados, dados_extraidos,
      nome_usuario_criou, criado_em, id_frete, id_encomenda
      FROM ocr_lancamentos WHERE empresa_id = $1
      AND (dados_revisados ? 'doc_remetente' OR dados_extraidos ? 'doc_remetente')`
    const params = [empresaId]
    let idx = 2

    if (categoria) {
      sql += ` AND tipo = $${idx++}`
      params.push(categoria)
    }

    sql += ' ORDER BY criado_em DESC LIMIT 200'
    const result = await pool.query(sql, params)

    const docs = result.rows.map(r => {
      const dados = r.dados_revisados || r.dados_extraidos || {}
      return {
        id: r.id,
        tipo: r.tipo,
        status: r.status,
        remetente: dados.remetente || '',
        destinatario: dados.destinatario || '',
        doc_remetente: dados.doc_remetente || null,
        id_frete: r.id_frete,
        id_encomenda: r.id_encomenda,
        operador: r.nome_usuario_criou,
        criado_em: r.criado_em
      }
    }).filter(d => d.doc_remetente)

    res.json(docs)
  } catch (err) {
    log.error('OCR', 'Erro ao listar documentos', { erro: err.message })
    res.status(500).json({ error: 'Erro ao listar documentos' })
  }
})

// ============================================================================
// GET /api/ocr/lancamentos/:id/doc-foto — Servir foto do documento do remetente
// ADMIN ONLY — dados sensiveis, acesso restrito
// ============================================================================
router.get('/lancamentos/:id/doc-foto', async (req, res) => {
  try {
    const funcao = (req.user.funcao || '').toLowerCase()
    if (funcao !== 'administrador' && funcao !== 'admin' && funcao !== 'gerente') {
      return res.status(403).json({ error: 'Acesso restrito a administradores da empresa' })
    }

    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT dados_revisados, dados_extraidos FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })

    const dados = result.rows[0].dados_revisados || result.rows[0].dados_extraidos || {}
    const docPath = dados.doc_remetente?.foto_doc_path
    if (!docPath) return res.status(404).json({ error: 'Nenhum documento anexado' })

    const fullPath = assertSafePath(docPath)
    if (!fullPath) return res.status(403).json({ error: 'Acesso negado' })

    // DP058: res.sendFile handles not-found via callback (removed sync existsSync)
    res.sendFile(fullPath, (err) => {
      if (err && !res.headersSent) res.status(404).json({ error: 'Foto nao encontrada no disco' })
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao servir foto do documento' })
  }
})

// ============================================================================
// DELETE /api/ocr/lancamentos/:id — Excluir lancamento (apenas pendente/revisado)
// ============================================================================
router.delete('/lancamentos/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      "DELETE FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2 AND status IN ('pendente', 'revisado_operador') RETURNING id, foto_path",
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Lancamento nao encontrado ou ja aprovado/rejeitado' })

    // Limpar foto do disco — mas so se nenhum outro lancamento do lote usa a mesma foto
    const fotoPath = result.rows[0].foto_path
    if (fotoPath) {
      const others = await pool.query(
        'SELECT COUNT(*) FROM ocr_lancamentos WHERE foto_path = $1 AND id != $2',
        [fotoPath, req.params.id]
      )
      if (parseInt(others.rows[0].count) === 0) {
        const fullPath = assertSafePath(fotoPath)
        if (fullPath) {
          await unlink(fullPath).catch(() => {})
        }
      }
    }

    res.json({ ok: true })
  } catch (err) {
    log.error('OCR', 'Erro ao excluir', { empresa_id: req.user?.empresa_id, lancamento_id: req.params.id, erro: err.message })
    res.status(500).json({ error: 'Erro ao excluir lancamento' })
  }
})

// reanalisar removido — consolidado em POST /lancamentos/:id/ia-review

// ============================================================================
// GET /api/ocr/lancamentos/:id/foto — Servir foto do filesystem
// DS4-015 fix: auth via middleware padrao (antes aceitava JWT em query param — expoe token em logs)
// ============================================================================
router.get('/lancamentos/:id/foto', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    if (!empresaId) return res.status(401).json({ error: 'Nao autorizado' })

    const result = await pool.query(
      'SELECT foto_path FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })

    // DS4-043 fix: path.resolve em ambos para normalizar completamente (antes: path.join parcial)
    const fullPath = assertSafePath(result.rows[0].foto_path)
    if (!fullPath) return res.status(403).json({ error: 'Acesso negado' })

    // DP058: res.sendFile handles not-found via callback (removed sync existsSync)
    res.sendFile(fullPath, (err) => {
      if (err && !res.headersSent) res.status(404).json({ error: 'Foto nao encontrada no disco' })
    })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao servir foto' })
  }
})

export default router
