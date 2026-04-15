import { Router } from 'express'
import multer from 'multer'
import path from 'path'
import { readFile, unlink } from 'fs/promises'
import { existsSync, mkdirSync } from 'fs'
import jwt from 'jsonwebtoken'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { callVisionOCR } from '../helpers/visionApi.js'
import { parseOcrText } from '../helpers/parseOcrText.js'
import { criarFreteComItens } from '../helpers/criarFrete.js'
import { geminiParseOCR } from '../helpers/geminiParser.js'

const router = Router()

// Auth para todas as rotas EXCETO foto (que aceita token via query param)
router.use((req, res, next) => {
  if (req.path.match(/\/lancamentos\/\d+\/foto/)) return next()
  return authMiddleware(req, res, next)
})

// Configurar multer para upload de fotos
const UPLOAD_PATH = process.env.OCR_UPLOAD_PATH || path.resolve('uploads/ocr')

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const dir = path.join(UPLOAD_PATH, String(req.user.empresa_id))
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
    cb(null, dir)
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg'
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

// ============================================================================
// POST /api/ocr/upload — Upload foto, rodar OCR, salvar lancamento
// ============================================================================
router.post('/upload', upload.single('foto'), async (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'Foto obrigatoria (campo "foto", max 10MB, jpeg/png/webp)' })
  }

  const empresaId = req.user.empresa_id
  const { viagem_id } = req.body

  try {
    // 1. Chamar Google Cloud Vision
    let ocrResult = { text: '', confidence: 0, fullResponse: null }
    try {
      ocrResult = await callVisionOCR(req.file.path)
    } catch (err) {
      console.error('[OCR] Erro na Vision API:', err.message)
      // Continua sem OCR — operador digita manualmente
    }

    // 2. Buscar itens padrao da empresa para comparacao de precos
    const padrao = await pool.query(
      'SELECT nome_item, preco_unitario_padrao, preco_unitario_desconto FROM itens_frete_padrao WHERE empresa_id = $1 AND ativo = TRUE',
      [empresaId]
    )

    // 3. Parsear texto OCR → itens estruturados
    const dados = parseOcrText(ocrResult.text, padrao.rows)

    // 4. Salvar no banco
    const fotoRelPath = path.relative(UPLOAD_PATH, req.file.path)
    const result = await pool.query(`
      INSERT INTO ocr_lancamentos (empresa_id, id_viagem, foto_path, foto_original_name,
        ocr_texto_bruto, ocr_json, ocr_confianca, dados_extraidos,
        status, id_usuario_criou, nome_usuario_criou)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'pendente', $9, $10)
      RETURNING *
    `, [
      empresaId,
      viagem_id || null,
      fotoRelPath,
      req.file.originalname,
      ocrResult.text,
      JSON.stringify(ocrResult.fullResponse),
      ocrResult.confidence,
      JSON.stringify(dados),
      req.user.id,
      req.user.login || null
    ])

    res.status(201).json({
      lancamento: result.rows[0],
      dados_extraidos: dados,
      ocr_confianca: ocrResult.confidence
    })
  } catch (err) {
    if (req.file?.path) {
      await unlink(req.file.path).catch(() => {})
    }
    console.error('[OCR] Erro no upload:', err.message)
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
    let sql = 'SELECT id, uuid, id_viagem, id_frete, foto_original_name, ocr_confianca, dados_extraidos, dados_revisados, status, motivo_rejeicao, nome_usuario_criou, nome_usuario_revisou, data_revisao, criado_em FROM ocr_lancamentos WHERE empresa_id = $1'
    const params = [empresaId]
    let idx = 2

    if (status) {
      sql += ` AND status = $${idx++}`
      params.push(status)
    }
    if (viagem_id) {
      sql += ` AND id_viagem = $${idx++}`
      params.push(viagem_id)
    }

    sql += ' ORDER BY criado_em DESC LIMIT 100'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    console.error('[OCR] Erro ao listar:', err.message)
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
router.post('/lancamentos/:id/ia-review', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id

    // Buscar lancamento
    const lancResult = await pool.query(
      'SELECT id, ocr_texto_bruto FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (lancResult.rows.length === 0) {
      return res.status(404).json({ error: 'Lancamento nao encontrado' })
    }

    const { ocr_texto_bruto } = lancResult.rows[0]
    if (!ocr_texto_bruto) {
      return res.status(400).json({ error: 'Sem texto OCR para analisar' })
    }

    // Buscar itens padrao da empresa
    const padrao = await pool.query(
      'SELECT nome_item, preco_unitario_padrao, preco_unitario_desconto FROM itens_frete_padrao WHERE empresa_id = $1 AND ativo = TRUE',
      [empresaId]
    )

    // Chamar Gemini AI
    const dados = await geminiParseOCR(ocr_texto_bruto, padrao.rows)

    // Atualizar dados extraidos no banco
    await pool.query(
      'UPDATE ocr_lancamentos SET dados_extraidos = $1 WHERE id = $2',
      [JSON.stringify(dados), req.params.id]
    )

    res.json({ dados_extraidos: dados })
  } catch (err) {
    console.error('[OCR] Erro na revisao IA:', err.message)
    res.status(500).json({ error: 'Erro ao processar com IA: ' + err.message })
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
    console.error('[OCR] Erro ao revisar:', err.message)
    res.status(500).json({ error: 'Erro ao revisar lancamento' })
  }
})

// ============================================================================
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

    // Montar payload para criarFreteComItens
    const valor_total_itens = dados.itens
      ? dados.itens.reduce((sum, i) => sum + (i.subtotal || i.preco_unitario * i.quantidade || 0), 0)
      : 0

    const fretePayload = {
      id_viagem: lanc.id_viagem,
      remetente_nome_temp: dados.remetente || null,
      destinatario_nome_temp: dados.destinatario || null,
      rota_temp: dados.rota || null,
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

    // Criar frete real
    const frete = await criarFreteComItens(client, empresaId, fretePayload)

    // Atualizar lancamento OCR
    await client.query(`
      UPDATE ocr_lancamentos SET status = 'aprovado', id_frete = $1,
        id_usuario_revisou = $2, nome_usuario_revisou = $3, data_revisao = CURRENT_TIMESTAMP
      WHERE id = $4 AND empresa_id = $5
    `, [frete.id_frete, req.user.id, req.user.login, lanc.id, empresaId])

    await client.query('COMMIT')

    res.json({ lancamento_id: lanc.id, frete })
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('[OCR] Erro ao aprovar:', err.message)
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
    console.error('[OCR] Erro ao rejeitar:', err.message)
    res.status(500).json({ error: 'Erro ao rejeitar lancamento' })
  }
})

// ============================================================================
// GET /api/ocr/lancamentos/:id/foto — Servir foto do filesystem
// Aceita auth via query param ?token=... (necessario para <img src>)
// ============================================================================
router.get('/lancamentos/:id/foto', async (req, res) => {
  try {
    // Auth: tentar query param primeiro (para <img src>), senao usa req.user do middleware
    let empresaId = req.user?.empresa_id
    if (!empresaId && req.query.token) {
      try {
        const decoded = jwt.verify(req.query.token, process.env.JWT_SECRET)
        empresaId = decoded.empresa_id
      } catch {
        return res.status(401).json({ error: 'Token invalido' })
      }
    }
    if (!empresaId) return res.status(401).json({ error: 'Nao autorizado' })

    const result = await pool.query(
      'SELECT foto_path FROM ocr_lancamentos WHERE id = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })

    const fullPath = path.join(UPLOAD_PATH, result.rows[0].foto_path)
    if (!existsSync(fullPath)) return res.status(404).json({ error: 'Foto nao encontrada no disco' })

    res.sendFile(fullPath)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao servir foto' })
  }
})

export default router
