import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// --- Usuarios ---
router.get('/usuarios', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, nome, email, funcao, permissao, excluido FROM usuarios WHERE excluido = FALSE OR excluido IS NULL ORDER BY nome'
    )
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar usuarios' })
  }
})

// --- Conferentes ---
router.get('/conferentes', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM conferentes ORDER BY nome')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar conferentes' })
  }
})

// --- Caixas ---
router.get('/caixas', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM caixas ORDER BY nome')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar caixas' })
  }
})

// --- Tarifas ---
router.get('/tarifas', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT t.*, r.origem, r.destino, tp.nome AS nome_tipo_passageiro
      FROM tarifas t
      LEFT JOIN rotas r ON t.id_rota = r.id_rota
      LEFT JOIN tipo_passageiro tp ON t.id_tipo_passageiro = tp.id_tipo_passageiro
      ORDER BY r.origem, tp.nome
    `)
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar tarifas' })
  }
})

// --- Tipo Passageiro ---
router.get('/tipos-passageiro', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM tipo_passageiro ORDER BY nome')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar tipos' })
  }
})

// --- Empresa ---
router.get('/empresa', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM configuracao_empresa LIMIT 1')
    res.json(result.rows[0] || {})
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar empresa' })
  }
})

// --- Clientes Encomenda ---
router.get('/clientes-encomenda', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM cad_clientes_encomenda ORDER BY nome_cliente')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar clientes' })
  }
})

// --- Itens Encomenda Padrao ---
router.get('/itens-encomenda', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM itens_encomenda_padrao WHERE ativo = TRUE ORDER BY nome_item')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens' })
  }
})

// --- Itens Frete ---
router.get('/itens-frete', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM itens_frete_padrao WHERE ativo = TRUE ORDER BY nome_item')
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens frete' })
  }
})

export default router
