import { Router } from 'express'
import multer from 'multer'
import path from 'path'
import { existsSync, mkdirSync } from 'fs'
import { unlink } from 'fs/promises'
import { randomUUID } from 'crypto'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { callVisionOCR } from '../helpers/visionApi.js'
import { fetchWithRetry } from '../helpers/fetchWithRetry.js'
import log from '../logger.js'

const router = Router()
router.use(authMiddleware)

const UPLOAD_PATH = process.env.OCR_UPLOAD_PATH || path.resolve('uploads/ocr')
const FUNCOES_ADMIN = ['administrador', 'admin', 'gerente']

function isAdmin(req) {
  return FUNCOES_ADMIN.includes((req.user.funcao || '').toLowerCase())
}

// Upload config — salva direto em docs/{empresa_id}/
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const dir = path.join(UPLOAD_PATH, 'docs', String(req.user.empresa_id))
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
    cb(null, dir)
  },
  filename: (req, file, cb) => {
    const extMap = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' }
    const ext = extMap[file.mimetype] || '.jpg'
    // #DS5-222: CSPRNG (era Math.random).
    cb(null, `${Date.now()}-${randomUUID().slice(0, 8)}${ext}`)
  }
})
const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (req, file, cb) => cb(null, ['image/jpeg', 'image/png', 'image/webp'].includes(file.mimetype))
})

const DOC_KEYWORDS = /\b(registro\s*geral|identidade|habilitac|cpf|cnpj|rg[\s:.-]|cnh|orgao\s*emissor|data\s*de\s*nascimento|filiacao|republica\s*federativa)\b/i

// ============================================================================
// POST /api/documentos/upload — Upload foto de documento + extrai nome/CPF/RG
// ============================================================================
router.post('/upload', upload.single('foto'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Foto obrigatoria' })
  if (!isAdmin(req)) {
    await unlink(req.file.path).catch(() => {})
    return res.status(403).json({ error: 'Acesso restrito a administradores' })
  }

  const { categoria, referencia_id, referencia_nome } = req.body
  if (!categoria) {
    await unlink(req.file.path).catch(() => {})
    return res.status(400).json({ error: 'Categoria obrigatoria (passageiro, encomenda, frete, empresa)' })
  }

  const empresaId = req.user.empresa_id

  try {
    // Vision OCR para extrair texto
    let ocrText = ''
    try {
      const ocrResult = await callVisionOCR(req.file.path)
      ocrText = ocrResult.text || ''
    } catch { /* sem texto */ }

    let nome = '', cpf = '', rg = '', tipo_doc = ''

    if (ocrText && DOC_KEYWORDS.test(ocrText)) {
      // Gemini extrai dados do documento
      const apiKey = process.env.GEMINI_API_KEY
      if (apiKey) {
        const prompt = `Extraia do texto de documento brasileiro:
1. Nome completo
2. CPF (formato 000.000.000-00) — PRIORIDADE
3. RG (se nao houver CPF)
4. Tipo do documento (RG, CNH, CPF)
NUNCA use numero de registro da CNH. Na CNH, CPF esta no campo "CPF".

Texto: """${ocrText}"""

JSON (sem markdown): {"nome":"","cpf":"","rg":"","tipo_doc":""}`

        try {
          const geminiRes = await fetchWithRetry(
            `https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=${apiKey}`,
            {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: { temperature: 0.1, maxOutputTokens: 2048 }
              }),
              signal: AbortSignal.timeout(30000)
            }
          )
          const data = await geminiRes.json()
          const parts = data.candidates?.[0]?.content?.parts || []
          const jsonText = parts.find(p => p.text?.includes('{'))?.text || ''
          const match = jsonText.match(/\{[\s\S]*\}/)
          if (match) {
            const parsed = JSON.parse(match[0])
            nome = parsed.nome || ''
            cpf = parsed.cpf || ''
            rg = parsed.rg || ''
            tipo_doc = parsed.tipo_doc || ''
          }
        } catch (err) {
          log.warn('Documentos', 'Gemini falhou na extracao', { erro: err.message })
        }
      }
    }

    // Se nao extraiu nome, usar referencia_nome
    if (!nome) nome = referencia_nome || ''

    const fotoRelPath = path.relative(UPLOAD_PATH, req.file.path)

    const result = await pool.query(`
      INSERT INTO documentos_arquivados (empresa_id, categoria, referencia_id, referencia_nome,
        nome_pessoa, cpf, rg, tipo_doc, foto_path, id_usuario_criou, nome_usuario_criou)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING *
    `, [
      empresaId, categoria, referencia_id || null, referencia_nome || null,
      nome, cpf, rg, tipo_doc, fotoRelPath,
      req.user.id, req.user.login || null
    ])

    log.info('Documentos', 'Documento arquivado', { empresa_id: empresaId, categoria, nome, tipo_doc })

    res.status(201).json(result.rows[0])
  } catch (err) {
    if (req.file?.path) await unlink(req.file.path).catch(() => {})
    log.error('Documentos', 'Erro no upload', { erro: err.message })
    res.status(500).json({ error: 'Erro ao processar documento' })
  }
})

// ============================================================================
// GET /api/documentos — Listar documentos arquivados (admin da empresa)
// ============================================================================
router.get('/', async (req, res) => {
  if (!isAdmin(req)) return res.status(403).json({ error: 'Acesso restrito' })

  try {
    const empresaId = req.user.empresa_id
    const { categoria } = req.query

    let sql = 'SELECT * FROM documentos_arquivados WHERE empresa_id = $1'
    const params = [empresaId]

    if (categoria) {
      sql += ' AND categoria = $2'
      params.push(categoria)
    }

    sql += ' ORDER BY criado_em DESC LIMIT 200'
    const result = await pool.query(sql, params)
    res.json(result.rows)
  } catch (err) {
    log.error('Documentos', 'Erro ao listar', { erro: err.message })
    res.status(500).json({ error: 'Erro ao listar documentos' })
  }
})

// ============================================================================
// GET /api/documentos/:id/foto — Servir foto (admin only)
// ============================================================================
router.get('/:id/foto', async (req, res) => {
  if (!isAdmin(req)) return res.status(403).json({ error: 'Acesso restrito' })

  try {
    const result = await pool.query(
      'SELECT foto_path FROM documentos_arquivados WHERE id = $1 AND empresa_id = $2',
      [req.params.id, req.user.empresa_id]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })

    const fullPath = path.resolve(UPLOAD_PATH, result.rows[0].foto_path)
    if (!fullPath.startsWith(path.resolve(UPLOAD_PATH))) return res.status(403).json({ error: 'Acesso negado' })
    if (!existsSync(fullPath)) return res.status(404).json({ error: 'Foto nao encontrada' })

    // #DS5-207: Content-Disposition impede browser de tratar arquivo como navegacao.
    //   nosniff vem do helmet global em server/index.js.
    res.setHeader('Content-Disposition', `inline; filename="documento-${req.params.id}.jpg"`)
    res.sendFile(fullPath)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao servir foto' })
  }
})

// ============================================================================
// DELETE /api/documentos/:id — Excluir documento (admin only)
// ============================================================================
router.delete('/:id', async (req, res) => {
  if (!isAdmin(req)) return res.status(403).json({ error: 'Acesso restrito' })

  try {
    const result = await pool.query(
      'DELETE FROM documentos_arquivados WHERE id = $1 AND empresa_id = $2 RETURNING foto_path',
      [req.params.id, req.user.empresa_id]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Nao encontrado' })

    const fullPath = path.resolve(UPLOAD_PATH, result.rows[0].foto_path)
    if (fullPath.startsWith(path.resolve(UPLOAD_PATH))) {
      await unlink(fullPath).catch(() => {})
    }

    res.json({ ok: true })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao excluir' })
  }
})

export default router
