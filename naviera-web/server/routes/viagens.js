import { Router } from 'express'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// GET /api/viagens
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
             e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id_rota
      ORDER BY v.data_viagem DESC
    `)
    res.json(result.rows)
  } catch (err) {
    console.error('[Viagens] Erro:', err.message)
    res.status(500).json({ error: 'Erro ao listar viagens' })
  }
})

// GET /api/viagens/ativa
router.get('/ativa', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
             e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id_rota
      WHERE v.ativa = TRUE
      ORDER BY v.data_viagem DESC
      LIMIT 1
    `)
    res.json(result.rows[0] || null)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar viagem ativa' })
  }
})

// GET /api/viagens/:id
router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT v.*, e.nome AS nome_embarcacao, r.origem, r.destino
      FROM viagens v
      LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
      LEFT JOIN rotas r ON v.id_rota = r.id_rota
      WHERE v.id_viagem = $1
    `, [req.params.id])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Viagem nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar viagem' })
  }
})

export default router
