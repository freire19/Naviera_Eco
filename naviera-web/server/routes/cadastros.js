import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'

const router = Router()
router.use(authMiddleware)

// --- Usuarios ---
router.get('/usuarios', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT id, nome, email, funcao, permissao, excluido FROM usuarios WHERE (excluido = FALSE OR excluido IS NULL) AND empresa_id = $1 ORDER BY nome',
      [empresaId]
    )
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar usuarios' })
  }
})

// --- Conferentes ---
router.get('/conferentes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM conferentes WHERE empresa_id = $1 ORDER BY nome', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar conferentes' })
  }
})

// --- Caixas ---
router.get('/caixas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM caixas WHERE empresa_id = $1 ORDER BY nome', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar caixas' })
  }
})

// --- Tarifas ---
router.get('/tarifas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT t.*, r.origem, r.destino, tp.nome AS nome_tipo_passageiro
      FROM tarifas t
      LEFT JOIN rotas r ON t.id_rota = r.id_rota
      LEFT JOIN tipo_passageiro tp ON t.id_tipo_passageiro = tp.id_tipo_passageiro
      WHERE t.empresa_id = $1
      ORDER BY r.origem, tp.nome
    `, [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar tarifas' })
  }
})

// --- Tipo Passageiro ---
router.get('/tipos-passageiro', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM tipo_passageiro WHERE empresa_id = $1 ORDER BY nome', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar tipos' })
  }
})

// --- Empresa ---
router.get('/empresa', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM configuracao_empresa WHERE empresa_id = $1 LIMIT 1', [empresaId])
    res.json(result.rows[0] || {})
  } catch (err) {
    res.status(500).json({ error: 'Erro ao buscar empresa' })
  }
})

// --- Clientes Encomenda ---
router.get('/clientes-encomenda', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM cad_clientes_encomenda WHERE empresa_id = $1 ORDER BY nome_cliente', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar clientes' })
  }
})

// --- Itens Encomenda Padrao ---
router.get('/itens-encomenda', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM itens_encomenda_padrao WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome_item', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens' })
  }
})

// --- Itens Frete ---
router.get('/itens-frete', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM itens_frete_padrao WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome_item', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar itens frete' })
  }
})

// ============================================================
// WRITE ENDPOINTS
// ============================================================

// --- Rotas CRUD ---
router.post('/rotas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { origem, destino } = req.body
    if (!origem || !destino) return res.status(400).json({ error: 'origem e destino obrigatorios' })
    const result = await pool.query(
      'INSERT INTO rotas (origem, destino, empresa_id) VALUES ($1, $2, $3) RETURNING *',
      [origem, destino, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar rota' })
  }
})

router.put('/rotas/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { origem, destino } = req.body
    const result = await pool.query(
      'UPDATE rotas SET origem = COALESCE($1, origem), destino = COALESCE($2, destino) WHERE id_rota = $3 AND empresa_id = $4 RETURNING *',
      [origem, destino, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Rota nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar rota' })
  }
})

// --- Embarcacoes CRUD ---
router.post('/embarcacoes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, registro_capitania, capacidade_passageiros, observacoes } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO embarcacoes (nome, registro_capitania, capacidade_passageiros, observacoes, empresa_id) VALUES ($1,$2,$3,$4,$5) RETURNING *',
      [nome, registro_capitania || null, capacidade_passageiros || null, observacoes || null, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar embarcacao' })
  }
})

router.put('/embarcacoes/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, registro_capitania, capacidade_passageiros, observacoes } = req.body
    const result = await pool.query(`
      UPDATE embarcacoes SET nome = COALESCE($1, nome), registro_capitania = COALESCE($2, registro_capitania),
        capacidade_passageiros = COALESCE($3, capacidade_passageiros), observacoes = COALESCE($4, observacoes)
      WHERE id_embarcacao = $5 AND empresa_id = $6 RETURNING *
    `, [nome, registro_capitania, capacidade_passageiros, observacoes, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Embarcacao nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar embarcacao' })
  }
})

// --- Conferentes CRUD ---
router.post('/conferentes', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO conferentes (nome, empresa_id) VALUES ($1, $2) RETURNING *',
      [nome, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar conferente' })
  }
})

router.put('/conferentes/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    const result = await pool.query(
      'UPDATE conferentes SET nome = $1 WHERE id_conferente = $2 AND empresa_id = $3 RETURNING *',
      [nome, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Conferente nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar conferente' })
  }
})

// --- Caixas CRUD ---
router.post('/caixas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    if (!nome) return res.status(400).json({ error: 'nome obrigatorio' })
    const result = await pool.query(
      'INSERT INTO caixas (nome, empresa_id) VALUES ($1, $2) RETURNING *',
      [nome, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar caixa' })
  }
})

router.put('/caixas/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome } = req.body
    const result = await pool.query(
      'UPDATE caixas SET nome = $1 WHERE id_caixa = $2 AND empresa_id = $3 RETURNING *',
      [nome, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Caixa nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar caixa' })
  }
})

// --- Clientes Encomenda CRUD ---
router.post('/clientes-encomenda', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_cliente, telefone, endereco } = req.body
    if (!nome_cliente) return res.status(400).json({ error: 'nome_cliente obrigatorio' })
    const result = await pool.query(
      'INSERT INTO cad_clientes_encomenda (nome_cliente, telefone, endereco, empresa_id) VALUES ($1,$2,$3,$4) RETURNING *',
      [nome_cliente, telefone || null, endereco || null, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar cliente' })
  }
})

router.put('/clientes-encomenda/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_cliente, telefone, endereco } = req.body
    const result = await pool.query(`
      UPDATE cad_clientes_encomenda SET nome_cliente = COALESCE($1, nome_cliente),
        telefone = COALESCE($2, telefone), endereco = COALESCE($3, endereco)
      WHERE id_cliente = $4 AND empresa_id = $5 RETURNING *
    `, [nome_cliente, telefone, endereco, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Cliente nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar cliente' })
  }
})

// --- Usuarios CRUD ---
router.post('/usuarios', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, email, senha, funcao, permissao } = req.body
    if (!nome || !senha) return res.status(400).json({ error: 'nome e senha obrigatorios' })
    const senhaHash = await bcrypt.hash(senha, 10)
    const result = await pool.query(
      'INSERT INTO usuarios (nome, email, senha, funcao, permissao, excluido, empresa_id) VALUES ($1,$2,$3,$4,$5,FALSE,$6) RETURNING id, nome, email, funcao, permissao',
      [nome, email || null, senhaHash, funcao || 'OPERADOR', permissao || null, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao criar usuario:', err.message)
    res.status(500).json({ error: 'Erro ao criar usuario' })
  }
})

router.put('/usuarios/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, email, funcao, permissao, senha } = req.body
    let sql, params
    if (senha) {
      const senhaHash = await bcrypt.hash(senha, 10)
      sql = 'UPDATE usuarios SET nome = COALESCE($1, nome), email = COALESCE($2, email), funcao = COALESCE($3, funcao), permissao = COALESCE($4, permissao), senha = $5 WHERE id = $6 AND empresa_id = $7 RETURNING id, nome, email, funcao, permissao'
      params = [nome, email, funcao, permissao, senhaHash, req.params.id, empresaId]
    } else {
      sql = 'UPDATE usuarios SET nome = COALESCE($1, nome), email = COALESCE($2, email), funcao = COALESCE($3, funcao), permissao = COALESCE($4, permissao) WHERE id = $5 AND empresa_id = $6 RETURNING id, nome, email, funcao, permissao'
      params = [nome, email, funcao, permissao, req.params.id, empresaId]
    }
    const result = await pool.query(sql, params)
    if (result.rows.length === 0) return res.status(404).json({ error: 'Usuario nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar usuario:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar usuario' })
  }
})

// --- Tarifas CRUD ---
router.post('/tarifas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_rota, id_tipo_passageiro, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto } = req.body
    if (!id_rota || !id_tipo_passageiro) return res.status(400).json({ error: 'id_rota e id_tipo_passageiro obrigatorios' })
    const result = await pool.query(`
      INSERT INTO tarifas (id_rota, id_tipo_passageiro, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto, empresa_id)
      VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *
    `, [id_rota, id_tipo_passageiro, valor_transporte || 0, valor_alimentacao || 0, valor_cargas || 0, valor_desconto || 0, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar tarifa' })
  }
})

router.put('/tarifas/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { valor_transporte, valor_alimentacao, valor_cargas, valor_desconto } = req.body
    const result = await pool.query(`
      UPDATE tarifas SET valor_transporte = COALESCE($1, valor_transporte), valor_alimentacao = COALESCE($2, valor_alimentacao),
        valor_cargas = COALESCE($3, valor_cargas), valor_desconto = COALESCE($4, valor_desconto)
      WHERE id_tarifa = $5 AND empresa_id = $6 RETURNING *
    `, [valor_transporte, valor_alimentacao, valor_cargas, valor_desconto, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Tarifa nao encontrada' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar tarifa' })
  }
})

export default router
