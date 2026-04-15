import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'

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
    const result = await pool.query('SELECT * FROM conferentes WHERE empresa_id = $1 ORDER BY nome_conferente', [empresaId])
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar conferentes' })
  }
})

// --- Caixas ---
router.get('/caixas', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM caixas WHERE empresa_id = $1 ORDER BY nome_caixa', [empresaId])
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
      LEFT JOIN rotas r ON t.id_rota = r.id
      LEFT JOIN tipo_passageiro tp ON t.id_tipo_passagem = tp.id
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

router.put('/empresa', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { companhia, nome_embarcacao, comandante, proprietario, origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, recomendacoes_bilhete } = req.body
    // Upsert: update if exists, insert if not
    const exists = await pool.query('SELECT id_config FROM configuracao_empresa WHERE empresa_id = $1', [empresaId])
    let result
    if (exists.rows.length > 0) {
      result = await pool.query(`
        UPDATE configuracao_empresa SET companhia=$1, nome_embarcacao=$2, comandante=$3, proprietario=$4,
          origem_padrao=$5, gerente=$6, linha_rio_padrao=$7, cnpj=$8, ie=$9, endereco=$10, cep=$11,
          telefone=$12, frase_relatorio=$13, recomendacoes_bilhete=$14
        WHERE empresa_id = $15 RETURNING *
      `, [companhia||null, nome_embarcacao||null, comandante||null, proprietario||null,
          origem_padrao||null, gerente||null, linha_rio_padrao||null, cnpj||null, ie||null,
          endereco||null, cep||null, telefone||null, frase_relatorio||null, recomendacoes_bilhete||null, empresaId])
    } else {
      result = await pool.query(`
        INSERT INTO configuracao_empresa (id_config, companhia, nome_embarcacao, comandante, proprietario,
          origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone,
          frase_relatorio, recomendacoes_bilhete, empresa_id)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16) RETURNING *
      `, [empresaId, companhia||null, nome_embarcacao||null, comandante||null, proprietario||null,
          origem_padrao||null, gerente||null, linha_rio_padrao||null, cnpj||null, ie||null,
          endereco||null, cep||null, telefone||null, frase_relatorio||null, recomendacoes_bilhete||null, empresaId])
    }
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar empresa:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar dados da empresa' })
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

// --- Funcionarios ---
router.get('/funcionarios', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT * FROM funcionarios WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome',
      [empresaId]
    )
    res.json(result.rows)
  } catch (err) {
    res.status(500).json({ error: 'Erro ao listar funcionarios' })
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
      'UPDATE rotas SET origem = COALESCE($1, origem), destino = COALESCE($2, destino) WHERE id = $3 AND empresa_id = $4 RETURNING *',
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
      'INSERT INTO embarcacoes (nome, registro_capitania, capacidade_passageiros, observacoes, empresa_id) VALUES ($1,$2,$3,$4,$5) ON CONFLICT (empresa_id, nome) DO NOTHING RETURNING *',
      [nome, registro_capitania || null, capacidade_passageiros || null, observacoes || null, empresaId]
    )
    if (result.rows.length > 0) {
      return res.status(201).json(result.rows[0])
    }
    // Conflict: record already exists, fetch it
    const existing = await pool.query(
      'SELECT * FROM embarcacoes WHERE empresa_id = $1 AND nome = $2',
      [empresaId, nome]
    )
    res.status(200).json(existing.rows[0])
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
      'INSERT INTO conferentes (nome_conferente, empresa_id) VALUES ($1, $2) RETURNING *',
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
      'UPDATE conferentes SET nome_conferente = $1 WHERE id_conferente = $2 AND empresa_id = $3 RETURNING *',
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
      'INSERT INTO caixas (nome_caixa, empresa_id) VALUES ($1, $2) RETURNING *',
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
      'UPDATE caixas SET nome_caixa = $1 WHERE id_caixa = $2 AND empresa_id = $3 RETURNING *',
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
    const { nome_cliente } = req.body
    if (!nome_cliente) return res.status(400).json({ error: 'nome_cliente obrigatorio' })
    const result = await pool.query(
      'INSERT INTO cad_clientes_encomenda (nome_cliente, empresa_id) VALUES ($1,$2) RETURNING *',
      [nome_cliente, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao criar cliente' })
  }
})

router.put('/clientes-encomenda/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_cliente } = req.body
    const result = await pool.query(
      'UPDATE cad_clientes_encomenda SET nome_cliente = COALESCE($1, nome_cliente) WHERE id_cliente = $2 AND empresa_id = $3 RETURNING *',
      [nome_cliente, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Cliente nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar cliente' })
  }
})

// --- Usuarios CRUD ---
router.post('/usuarios', validate({ nome: 'required|string|min:2', senha: 'required|string|min:4' }), async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, email, senha, funcao, permissao } = req.body
    if (!nome || !senha) return res.status(400).json({ error: 'nome e senha obrigatorios' })
    if (senha.length > 128) return res.status(400).json({ error: 'Senha deve ter no maximo 128 caracteres' })
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
      if (senha.length > 128) return res.status(400).json({ error: 'Senha deve ter no maximo 128 caracteres' })
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
router.post('/tarifas', validate({ id_rota: 'required|integer', id_tipo_passagem: 'required|integer' }), async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto } = req.body
    if (!id_rota || !id_tipo_passagem) return res.status(400).json({ error: 'id_rota e id_tipo_passagem obrigatorios' })
    const result = await pool.query(`
      INSERT INTO tarifas (id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto, empresa_id)
      VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *
    `, [id_rota, id_tipo_passagem, valor_transporte || 0, valor_alimentacao || 0, valor_cargas || 0, valor_desconto || 0, empresaId])
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

// --- Funcionarios CRUD ---
router.post('/funcionarios', validate({ nome: 'required|string|min:2' }), async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento, is_clt, recebe_decimo_terceiro } = req.body
    const result = await pool.query(`
      INSERT INTO funcionarios (nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento, is_clt, recebe_decimo_terceiro, ativo, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, TRUE, $13) RETURNING *
    `, [nome, cpf || null, rg || null, ctps || null, telefone || null, endereco || null, cargo || null,
        parseFloat(salario) || 0, data_admissao || null, data_nascimento || null,
        is_clt || false, recebe_decimo_terceiro || false, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao criar funcionario:', err.message)
    res.status(500).json({ error: 'Erro ao criar funcionario' })
  }
})

router.put('/funcionarios/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento, is_clt, recebe_decimo_terceiro } = req.body
    const result = await pool.query(`
      UPDATE funcionarios SET nome = $1, cpf = $2, rg = $3, ctps = $4, telefone = $5, endereco = $6,
        cargo = $7, salario = $8, data_admissao = $9, data_nascimento = $10, is_clt = $11, recebe_decimo_terceiro = $12
      WHERE id = $13 AND empresa_id = $14 RETURNING *
    `, [nome, cpf || null, rg || null, ctps || null, telefone || null, endereco || null, cargo || null,
        parseFloat(salario) || 0, data_admissao || null, data_nascimento || null,
        is_clt || false, recebe_decimo_terceiro || false, req.params.id, empresaId])
    if (result.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    res.status(500).json({ error: 'Erro ao atualizar funcionario' })
  }
})

router.delete('/funcionarios/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE funcionarios SET ativo = FALSE WHERE id = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    res.json({ ok: true })
  } catch (err) {
    res.status(500).json({ error: 'Erro ao desativar funcionario' })
  }
})

// --- Itens Frete Padrao CRUD ---
router.post('/itens-frete', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    if (!nome_item) return res.status(400).json({ error: 'nome_item obrigatorio' })
    const result = await pool.query(
      'INSERT INTO itens_frete_padrao (nome_item, preco_padrao, ativo, empresa_id) VALUES ($1, $2, TRUE, $3) RETURNING *',
      [nome_item, parseFloat(preco_padrao) || 0, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao criar item frete:', err.message)
    res.status(500).json({ error: 'Erro ao criar item frete' })
  }
})

router.put('/itens-frete/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    const result = await pool.query(
      'UPDATE itens_frete_padrao SET nome_item = COALESCE($1, nome_item), preco_padrao = COALESCE($2, preco_padrao) WHERE id = $3 AND empresa_id = $4 RETURNING *',
      [nome_item, preco_padrao != null ? parseFloat(preco_padrao) : null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item frete nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar item frete:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar item frete' })
  }
})

router.delete('/itens-frete/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE itens_frete_padrao SET ativo = FALSE WHERE id = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item frete nao encontrado' })
    res.json({ ok: true })
  } catch (err) {
    console.error('[Cadastros] Erro ao desativar item frete:', err.message)
    res.status(500).json({ error: 'Erro ao desativar item frete' })
  }
})

// --- Itens Encomenda Padrao CRUD ---
router.post('/itens-encomenda', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    if (!nome_item) return res.status(400).json({ error: 'nome_item obrigatorio' })
    const result = await pool.query(
      'INSERT INTO itens_encomenda_padrao (nome_item, preco_padrao, ativo, empresa_id) VALUES ($1, $2, TRUE, $3) RETURNING *',
      [nome_item, parseFloat(preco_padrao) || 0, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao criar item encomenda:', err.message)
    res.status(500).json({ error: 'Erro ao criar item encomenda' })
  }
})

router.put('/itens-encomenda/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    const result = await pool.query(
      'UPDATE itens_encomenda_padrao SET nome_item = COALESCE($1, nome_item), preco_padrao = COALESCE($2, preco_padrao) WHERE id = $3 AND empresa_id = $4 RETURNING *',
      [nome_item, preco_padrao != null ? parseFloat(preco_padrao) : null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item encomenda nao encontrado' })
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar item encomenda:', err.message)
    res.status(500).json({ error: 'Erro ao atualizar item encomenda' })
  }
})

router.delete('/itens-encomenda/:id', async (req, res) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE itens_encomenda_padrao SET ativo = FALSE WHERE id = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item encomenda nao encontrado' })
    res.json({ ok: true })
  } catch (err) {
    console.error('[Cadastros] Erro ao desativar item encomenda:', err.message)
    res.status(500).json({ error: 'Erro ao desativar item encomenda' })
  }
})

export default router
