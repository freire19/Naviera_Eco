import { Router } from 'express'
import bcrypt from 'bcryptjs'
import pool from '../db.js'
import { authMiddleware } from '../middleware/auth.js'
import { validate } from '../middleware/validate.js'
import { tenantCrud, auxCrud, registerCrud } from '../utils/crudFactory.js'

const router = Router()
router.use(authMiddleware)

// ============================================================
// FACTORY-GENERATED CRUD (tenant-scoped)
// ============================================================

const conferentes = tenantCrud({ table: 'conferentes', idColumn: 'id_conferente', nameColumn: 'nome_conferente' })
registerCrud(router, '/conferentes', conferentes)

const caixas = tenantCrud({ table: 'caixas', idColumn: 'id_caixa', nameColumn: 'nome_caixa' })
registerCrud(router, '/caixas', caixas)

const tiposPassageiro = tenantCrud({ table: 'tipo_passageiro', idColumn: 'id', nameColumn: 'nome' })
registerCrud(router, '/tipos-passageiro', tiposPassageiro)

const clientesEncomenda = tenantCrud({ table: 'cad_clientes_encomenda', idColumn: 'id_cliente', nameColumn: 'nome_cliente', nameField: 'nome_cliente' })
registerCrud(router, '/clientes-encomenda', clientesEncomenda)

// ============================================================
// FACTORY-GENERATED CRUD (aux shared tables — no empresa_id)
// ============================================================

const sexos = auxCrud({ table: 'aux_sexo', idColumn: 'id_sexo', nameColumn: 'nome_sexo' })
registerCrud(router, '/sexos', sexos)

const tiposDocumento = auxCrud({ table: 'aux_tipos_documento', idColumn: 'id_tipo_doc', nameColumn: 'nome_tipo_doc' })
registerCrud(router, '/tipos-documento', tiposDocumento)

const nacionalidades = auxCrud({ table: 'aux_nacionalidades', idColumn: 'id_nacionalidade', nameColumn: 'nome_nacionalidade' })
registerCrud(router, '/nacionalidades', nacionalidades)

const tiposPassagemAux = auxCrud({ table: 'aux_tipos_passagem', idColumn: 'id_tipo_passagem', nameColumn: 'nome_tipo_passagem' })
registerCrud(router, '/tipos-passagem-aux', tiposPassagemAux)

const agentes = auxCrud({ table: 'aux_agentes', idColumn: 'id_agente', nameColumn: 'nome_agente' })
registerCrud(router, '/agentes', agentes)

const horariosSaida = auxCrud({ table: 'aux_horarios_saida', idColumn: 'id_horario_saida', nameColumn: 'descricao_horario_saida' })
registerCrud(router, '/horarios-saida', horariosSaida)

const acomodacoes = auxCrud({ table: 'aux_acomodacoes', idColumn: 'id_acomodacao', nameColumn: 'nome_acomodacao' })
registerCrud(router, '/acomodacoes', acomodacoes)

// ============================================================
// CUSTOM ENDPOINTS (not suitable for factory)
// ============================================================

// --- Usuarios (list) ---
router.get('/usuarios', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT id, nome, email, funcao, permissao, excluido FROM usuarios WHERE (excluido = FALSE OR excluido IS NULL) AND empresa_id = $1 ORDER BY nome',
      [empresaId]
    )
    res.json(result.rows)
  } catch (err) { next(err) }
})

// --- Tarifas (custom join) ---
router.get('/tarifas', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(`
      SELECT t.*, r.origem, r.destino,
             COALESCE(tp.nome, atp.nome_tipo_passagem) AS nome_tipo_passageiro
      FROM tarifas t
      LEFT JOIN rotas r ON t.id_rota = r.id
      LEFT JOIN tipo_passageiro tp ON t.id_tipo_passagem = tp.id AND tp.empresa_id = t.empresa_id
      LEFT JOIN aux_tipos_passagem atp ON t.id_tipo_passagem = atp.id_tipo_passagem
      WHERE t.empresa_id = $1
      ORDER BY r.origem, COALESCE(tp.nome, atp.nome_tipo_passagem)
    `, [empresaId])
    res.json(result.rows)
  } catch (err) { next(err) }
})

// --- Tarifa lookup por rota + tipo passagem ---
router.get('/tarifas/busca', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_rota, id_tipo_passagem } = req.query
    if (!id_rota || !id_tipo_passagem) return res.json(null)
    const result = await pool.query(
      `SELECT valor_transporte, valor_alimentacao, valor_cargas, valor_desconto
       FROM tarifas WHERE id_rota = $1 AND id_tipo_passagem = $2 AND empresa_id = $3 LIMIT 1`,
      [id_rota, id_tipo_passagem, empresaId]
    )
    res.json(result.rows[0] || null)
  } catch (err) { next(err) }
})

// --- Empresa ---
router.get('/empresa', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT * FROM configuracao_empresa WHERE empresa_id = $1 LIMIT 1', [empresaId])
    res.json(result.rows[0] || {})
  } catch (err) { next(err) }
})

router.put('/empresa', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { companhia, nome_embarcacao, comandante, proprietario, origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, telefone, frase_relatorio, recomendacoes_bilhete } = req.body
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
    next(err)
  }
})

// --- Funcionarios (list) ---
router.get('/funcionarios', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'SELECT * FROM funcionarios WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome',
      [empresaId]
    )
    res.json(result.rows)
  } catch (err) { next(err) }
})

// --- Itens Encomenda Padrao (list) ---
router.get('/itens-encomenda', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT *, id_item_encomenda AS id, preco_unitario_padrao AS preco_padrao FROM itens_encomenda_padrao WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome_item', [empresaId])
    res.json(result.rows)
  } catch (err) { next(err) }
})

// --- Itens Frete (list) ---
router.get('/itens-frete', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query('SELECT *, id_item_frete AS id, preco_unitario_padrao AS preco_padrao FROM itens_frete_padrao WHERE ativo = TRUE AND empresa_id = $1 ORDER BY nome_item', [empresaId])
    res.json(result.rows)
  } catch (err) { next(err) }
})

// ============================================================
// WRITE ENDPOINTS (custom logic — not factory-suitable)
// ============================================================

// --- Rotas CRUD ---
router.post('/rotas', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { origem, destino } = req.body
    if (!origem || !destino) return res.status(400).json({ error: 'origem e destino obrigatorios' })
    const result = await pool.query(
      'INSERT INTO rotas (origem, destino, empresa_id) VALUES ($1, $2, $3) RETURNING *',
      [origem, destino, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) { next(err) }
})

router.put('/rotas/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { origem, destino } = req.body
    const result = await pool.query(
      'UPDATE rotas SET origem = COALESCE($1, origem), destino = COALESCE($2, destino) WHERE id = $3 AND empresa_id = $4 RETURNING *',
      [origem, destino, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Rota nao encontrada' })
    res.json(result.rows[0])
  } catch (err) { next(err) }
})

router.delete('/rotas/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const check = await pool.query(
      'SELECT COUNT(*) AS cnt FROM viagens WHERE id_rota = $1 AND empresa_id = $2',
      [req.params.id, empresaId]
    )
    if (parseInt(check.rows[0].cnt) > 0) {
      return res.status(400).json({ error: 'Nao e possivel excluir rota com viagens associadas' })
    }
    const result = await pool.query(
      'DELETE FROM rotas WHERE id = $1 AND empresa_id = $2 RETURNING id',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Rota nao encontrada' })
    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Embarcacoes CRUD ---
router.post('/embarcacoes', async (req, res, next) => {
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
    const existing = await pool.query(
      'SELECT * FROM embarcacoes WHERE empresa_id = $1 AND nome = $2',
      [empresaId, nome]
    )
    res.status(200).json(existing.rows[0])
  } catch (err) { next(err) }
})

router.put('/embarcacoes/:id', async (req, res, next) => {
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
  } catch (err) { next(err) }
})

// --- Usuarios CRUD ---
router.post('/usuarios', validate({ nome: 'required|string|min:2', senha: 'required|string|min:4' }), async (req, res, next) => {
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
    next(err)
  }
})

router.put('/usuarios/:id', async (req, res, next) => {
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
    next(err)
  }
})

// --- Tarifas CRUD ---
router.post('/tarifas', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { id_rota, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto } = req.body
    const id_tipo_passagem = req.body.id_tipo_passagem || req.body.id_tipo_passageiro
    if (!id_rota || !id_tipo_passagem) return res.status(400).json({ error: 'id_rota e id_tipo_passagem obrigatorios' })
    const result = await pool.query(`
      INSERT INTO tarifas (id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto, empresa_id)
      VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *
    `, [id_rota, id_tipo_passagem, valor_transporte || 0, valor_alimentacao || 0, valor_cargas || 0, valor_desconto || 0, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) { next(err) }
})

router.put('/tarifas/:id', async (req, res, next) => {
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
  } catch (err) { next(err) }
})

router.delete('/tarifas/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'DELETE FROM tarifas WHERE id_tarifa = $1 AND empresa_id = $2 RETURNING id_tarifa',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Tarifa nao encontrada' })
    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Funcionarios CRUD ---
router.post('/funcionarios', validate({ nome: 'required|string|min:2' }), async (req, res, next) => {
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
    next(err)
  }
})

router.put('/funcionarios/:id', async (req, res, next) => {
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
  } catch (err) { next(err) }
})

router.delete('/funcionarios/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE funcionarios SET ativo = FALSE WHERE id = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Itens Frete Padrao CRUD ---
router.post('/itens-frete', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao, preco_desconto } = req.body
    if (!nome_item) return res.status(400).json({ error: 'nome_item obrigatorio' })
    const result = await pool.query(
      'INSERT INTO itens_frete_padrao (nome_item, preco_unitario_padrao, preco_unitario_desconto, ativo, empresa_id) VALUES ($1, $2, $3, TRUE, $4) RETURNING *, preco_unitario_padrao AS preco_padrao, preco_unitario_desconto',
      [(nome_item || '').toUpperCase(), parseFloat(preco_padrao) || 0, parseFloat(preco_desconto) || 0, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) { next(err) }
})

router.put('/itens-frete/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao, preco_desconto } = req.body
    const result = await pool.query(
      `UPDATE itens_frete_padrao SET nome_item = COALESCE($1, nome_item),
        preco_unitario_padrao = COALESCE($2, preco_unitario_padrao),
        preco_unitario_desconto = COALESCE($3, preco_unitario_desconto)
       WHERE id_item_frete = $4 AND empresa_id = $5
       RETURNING *, preco_unitario_padrao AS preco_padrao`,
      [nome_item ? nome_item.toUpperCase() : null, preco_padrao != null ? parseFloat(preco_padrao) : null,
       preco_desconto != null ? parseFloat(preco_desconto) : null,
       req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item frete nao encontrado' })
    res.json(result.rows[0])
  } catch (err) { next(err) }
})

router.delete('/itens-frete/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE itens_frete_padrao SET ativo = FALSE WHERE id_item_frete = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item frete nao encontrado' })
    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Itens Encomenda Padrao CRUD ---
router.post('/itens-encomenda', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    if (!nome_item) return res.status(400).json({ error: 'nome_item obrigatorio' })
    const result = await pool.query(
      'INSERT INTO itens_encomenda_padrao (nome_item, preco_unitario_padrao, ativo, empresa_id) VALUES ($1, $2, TRUE, $3) RETURNING *, preco_unitario_padrao AS preco_padrao',
      [(nome_item || '').toUpperCase(), parseFloat(preco_padrao) || 0, empresaId]
    )
    res.status(201).json(result.rows[0])
  } catch (err) { next(err) }
})

router.put('/itens-encomenda/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome_item, preco_padrao } = req.body
    const result = await pool.query(
      'UPDATE itens_encomenda_padrao SET nome_item = COALESCE($1, nome_item), preco_unitario_padrao = COALESCE($2, preco_unitario_padrao) WHERE id_item_encomenda = $3 AND empresa_id = $4 RETURNING *, preco_unitario_padrao AS preco_padrao',
      [nome_item ? nome_item.toUpperCase() : null, preco_padrao != null ? parseFloat(preco_padrao) : null, req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item encomenda nao encontrado' })
    res.json(result.rows[0])
  } catch (err) { next(err) }
})

router.delete('/itens-encomenda/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const result = await pool.query(
      'UPDATE itens_encomenda_padrao SET ativo = FALSE WHERE id_item_encomenda = $1 AND empresa_id = $2 RETURNING *',
      [req.params.id, empresaId]
    )
    if (result.rows.length === 0) return res.status(404).json({ error: 'Item encomenda nao encontrado' })
    res.json({ ok: true })
  } catch (err) { next(err) }
})

export default router
