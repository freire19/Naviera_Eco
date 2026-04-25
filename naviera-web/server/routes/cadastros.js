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
    const { companhia, nome_embarcacao, comandante, proprietario, origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, cidade, telefone, frase_relatorio, recomendacoes_bilhete } = req.body
    const exists = await pool.query('SELECT id_config FROM configuracao_empresa WHERE empresa_id = $1', [empresaId])
    let result
    if (exists.rows.length > 0) {
      result = await pool.query(`
        UPDATE configuracao_empresa SET companhia=$1, nome_embarcacao=$2, comandante=$3, proprietario=$4,
          origem_padrao=$5, gerente=$6, linha_rio_padrao=$7, cnpj=$8, ie=$9, endereco=$10, cep=$11,
          telefone=$12, frase_relatorio=$13, recomendacoes_bilhete=$14, cidade=$15
        WHERE empresa_id = $16 RETURNING *
      `, [companhia||null, nome_embarcacao||null, comandante||null, proprietario||null,
          origem_padrao||null, gerente||null, linha_rio_padrao||null, cnpj||null, ie||null,
          endereco||null, cep||null, telefone||null, frase_relatorio||null, recomendacoes_bilhete||null, cidade||null, empresaId])
    } else {
      result = await pool.query(`
        INSERT INTO configuracao_empresa (id_config, companhia, nome_embarcacao, comandante, proprietario,
          origem_padrao, gerente, linha_rio_padrao, cnpj, ie, endereco, cep, cidade, telefone,
          frase_relatorio, recomendacoes_bilhete, empresa_id)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17) RETURNING *
      `, [empresaId, companhia||null, nome_embarcacao||null, comandante||null, proprietario||null,
          origem_padrao||null, gerente||null, linha_rio_padrao||null, cnpj||null, ie||null,
          endereco||null, cep||null, cidade||null, telefone||null, frase_relatorio||null, recomendacoes_bilhete||null, empresaId])
    }
    res.json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar empresa:', err.message)
    next(err)
  }
})

// --- Dados de recebimento (conta bancaria / PIX da empresa) ---
// Usados para creditar pagamentos do app (passagens e encomendas) direto na empresa dona da viagem/encomenda.
router.get('/recebimento', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const r = await pool.query(
      `SELECT chave_pix, tipo_chave_pix, titular_conta, cpf_cnpj_recebedor,
              banco, agencia, conta_numero, conta_tipo, psp_provider, psp_subconta_id
       FROM empresas WHERE id = $1`,
      [empresaId]
    )
    res.json(r.rows[0] || {})
  } catch (err) { next(err) }
})

router.put('/recebimento', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const {
      chave_pix, tipo_chave_pix, titular_conta, cpf_cnpj_recebedor,
      banco, agencia, conta_numero, conta_tipo, psp_provider, psp_subconta_id
    } = req.body

    const tiposChave = ['CPF','CNPJ','EMAIL','TELEFONE','ALEATORIA']
    if (tipo_chave_pix && !tiposChave.includes(tipo_chave_pix)) {
      return res.status(400).json({ error: 'tipo_chave_pix invalido' })
    }
    const tiposConta = ['CORRENTE','POUPANCA']
    if (conta_tipo && !tiposConta.includes(conta_tipo)) {
      return res.status(400).json({ error: 'conta_tipo invalido' })
    }

    const r = await pool.query(
      `UPDATE empresas SET
         chave_pix = $1, tipo_chave_pix = $2, titular_conta = $3, cpf_cnpj_recebedor = $4,
         banco = $5, agencia = $6, conta_numero = $7, conta_tipo = $8,
         psp_provider = $9, psp_subconta_id = $10
       WHERE id = $11
       RETURNING chave_pix, tipo_chave_pix, titular_conta, cpf_cnpj_recebedor,
                 banco, agencia, conta_numero, conta_tipo, psp_provider, psp_subconta_id`,
      [chave_pix || null, tipo_chave_pix || null, titular_conta || null, cpf_cnpj_recebedor || null,
       banco || null, agencia || null, conta_numero || null, conta_tipo || null,
       psp_provider || null, psp_subconta_id || null, empresaId]
    )
    res.json(r.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao atualizar recebimento:', err.message)
    next(err)
  }
})

// --- PSP onboarding (proxy para Spring API) ---
// BFF repassa JWT do usuario; Spring API (:8081) consolida logica e chama Asaas.
const SPRING_API_BASE = process.env.SPRING_API_BASE || 'http://localhost:8081/api'

router.get('/recebimento/psp/status', async (req, res) => {
  try {
    const upstream = await fetch(`${SPRING_API_BASE}/psp/status`, {
      headers: { Authorization: req.headers.authorization }
    })
    const body = await upstream.text()
    res.status(upstream.status).type('application/json').send(body)
  } catch (err) {
    console.error('[Cadastros] Erro proxy /psp/status:', err.message)
    res.status(502).json({ error: 'Backend PSP indisponivel' })
  }
})

router.post('/recebimento/onboarding', async (req, res) => {
  try {
    const upstream = await fetch(`${SPRING_API_BASE}/psp/onboarding`, {
      method: 'POST',
      headers: {
        Authorization: req.headers.authorization,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(req.body || {})
    })
    const body = await upstream.text()
    res.status(upstream.status).type('application/json').send(body)
  } catch (err) {
    console.error('[Cadastros] Erro proxy /psp/onboarding:', err.message)
    res.status(502).json({ error: 'Backend PSP indisponivel' })
  }
})

// --- Funcionarios (list) ---
router.get('/funcionarios', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const incluirInativos = req.query.incluir_inativos === 'true'
    const where = incluirInativos
      ? 'WHERE empresa_id = $1'
      : 'WHERE ativo = TRUE AND empresa_id = $1'
    const result = await pool.query(
      `SELECT id, nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao,
              data_nascimento, data_inicio_calculo, recebe_decimo_terceiro, is_clt,
              valor_inss, descontar_inss, ativo
       FROM funcionarios ${where} ORDER BY nome`,
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
// #102: alteracao de funcao/permissao so por Administrador; operador comum so edita proprio perfil
// (nome/email/senha) sem tocar em funcao/permissao. Bloqueia auto-promocao e criacao de novo admin
// por nao-admin.
function isAdmin(user) {
  const f = (user?.funcao || '').toLowerCase()
  return f === 'administrador' || f === 'admin'
}

router.post('/usuarios', validate({ nome: 'required|string|min:2', senha: 'required|string|min:4' }), async (req, res, next) => {
  try {
    if (!isAdmin(req.user)) {
      return res.status(403).json({ error: 'Apenas Administrador pode criar usuarios' })
    }
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
    const alvoId = parseInt(req.params.id, 10)
    if (!Number.isInteger(alvoId) || alvoId <= 0) {
      return res.status(400).json({ error: 'id invalido' })
    }
    const { nome, email, funcao, permissao, senha } = req.body
    const mudaRole = funcao !== undefined || permissao !== undefined
    const admin = isAdmin(req.user)

    // #102: somente admin muda funcao/permissao; nao-admin so edita a si mesmo (sem tocar em role)
    if (!admin) {
      if (mudaRole) {
        return res.status(403).json({ error: 'Apenas Administrador pode alterar funcao/permissao' })
      }
      if (alvoId !== req.user.id) {
        return res.status(403).json({ error: 'Operador nao-admin so pode editar proprio usuario' })
      }
    }
    // #102: admin nao pode alterar propria funcao/permissao (prevenir perda de controle e auto-locking)
    if (admin && alvoId === req.user.id && mudaRole) {
      return res.status(400).json({ error: 'Nao e possivel alterar a propria funcao/permissao' })
    }

    let sql, params
    if (senha) {
      if (senha.length > 128) return res.status(400).json({ error: 'Senha deve ter no maximo 128 caracteres' })
      const senhaHash = await bcrypt.hash(senha, 10)
      sql = 'UPDATE usuarios SET nome = COALESCE($1, nome), email = COALESCE($2, email), funcao = COALESCE($3, funcao), permissao = COALESCE($4, permissao), senha = $5 WHERE id = $6 AND empresa_id = $7 RETURNING id, nome, email, funcao, permissao'
      params = [nome, email, funcao, permissao, senhaHash, alvoId, empresaId]
    } else {
      sql = 'UPDATE usuarios SET nome = COALESCE($1, nome), email = COALESCE($2, email), funcao = COALESCE($3, funcao), permissao = COALESCE($4, permissao) WHERE id = $5 AND empresa_id = $6 RETURNING id, nome, email, funcao, permissao'
      params = [nome, email, funcao, permissao, alvoId, empresaId]
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
    const { nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento,
            is_clt, recebe_decimo_terceiro, valor_inss, descontar_inss } = req.body
    const result = await pool.query(`
      INSERT INTO funcionarios (nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao,
        data_nascimento, is_clt, recebe_decimo_terceiro, valor_inss, descontar_inss, ativo, empresa_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, TRUE, $15) RETURNING *
    `, [nome, cpf || null, rg || null, ctps || null, telefone || null, endereco || null, cargo || null,
        parseFloat(salario) || 0, data_admissao || null, data_nascimento || null,
        is_clt || false, recebe_decimo_terceiro || false,
        parseFloat(valor_inss) || 0, descontar_inss || false, empresaId])
    res.status(201).json(result.rows[0])
  } catch (err) {
    console.error('[Cadastros] Erro ao criar funcionario:', err.message)
    next(err)
  }
})

router.put('/funcionarios/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento,
            is_clt, recebe_decimo_terceiro, valor_inss, descontar_inss, data_inicio_calculo } = req.body
    const result = await pool.query(`
      UPDATE funcionarios SET nome = $1, cpf = $2, rg = $3, ctps = $4, telefone = $5, endereco = $6,
        cargo = $7, salario = $8, data_admissao = $9, data_nascimento = $10, is_clt = $11,
        recebe_decimo_terceiro = $12, valor_inss = $13, descontar_inss = $14, data_inicio_calculo = $15
      WHERE id = $16 AND empresa_id = $17 RETURNING *
    `, [nome, cpf || null, rg || null, ctps || null, telefone || null, endereco || null, cargo || null,
        parseFloat(salario) || 0, data_admissao || null, data_nascimento || null,
        is_clt || false, recebe_decimo_terceiro || false,
        parseFloat(valor_inss) || 0, descontar_inss || false, data_inicio_calculo || null,
        req.params.id, empresaId])
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

// --- Helper: calculo financeiro do funcionario (reutilizado por /financeiro e /fechar-mes) ---
function calcDiasComerciais(dataInicio) {
  const hoje = new Date()
  const inicio = new Date(dataInicio)
  if (inicio > hoje) return { dias: 0, hoje, inicio }
  let dI = Math.min(inicio.getUTCDate(), 30)
  let dF = Math.min(hoje.getUTCDate(), 30)
  const mI = inicio.getUTCMonth() + 1
  const mF = hoje.getUTCMonth() + 1
  if (mI === 2 && dI >= 28) dI = 30
  if (mF === 2 && dF >= 28) dF = 30
  let dias = (hoje.getUTCFullYear() - inicio.getUTCFullYear()) * 360
           + (mF - mI) * 30 + (dF - dI) + 1
  if (dias > 30) dias = 30
  if (dias < 0) dias = 0
  return { dias, hoje, inicio }
}

async function calcFinanceiroFuncionario(pool, idFunc, empresaId, f) {
  const dataInicio = f.data_inicio_calculo || f.data_admissao
  if (!dataInicio) return { dias_trabalhados: 0, acumulado: 0, pago: 0, descontos_rh: 0, saldo: 0 }

  const { dias, hoje, inicio } = calcDiasComerciais(dataInicio)
  if (inicio > hoje) return { dias_trabalhados: 0, acumulado: 0, pago: 0, descontos_rh: 0, saldo: 0, data_inicio: dataInicio }

  const salario = parseFloat(f.salario) || 0
  const salarioDiario = salario / 30
  const acumulado = Math.round(dias * salarioDiario * 100) / 100

  // Data do ultimo FECHAMENTO MENSAL — pagamentos/descontos anteriores ou iguais
  // a essa data ficam "presos" no ciclo anterior e nao contam no atual.
  const fechRes = await pool.query(
    `SELECT MAX(data_pagamento) AS ultimo FROM financeiro_saidas
     WHERE funcionario_id = $1 AND empresa_id = $2 AND is_excluido = false
     AND UPPER(descricao) LIKE 'FECHAMENTO MENSAL%'`,
    [idFunc, empresaId]
  )
  const ultimoFech = fechRes.rows[0].ultimo || '1900-01-01'

  // 5 queries independentes em paralelo (filtradas pelo ultimo fechamento)
  const [pgRes, rhRes, lgRes, inssRH, inssLeg] = await Promise.all([
    pool.query(
      `SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas
       WHERE funcionario_id = $1 AND empresa_id = $2 AND is_excluido = false
       AND data_pagamento >= $3 AND data_pagamento > $4
       AND (forma_pagamento IS NULL OR (forma_pagamento != 'DESCONTO' AND forma_pagamento != 'RETIDO'))`,
      [idFunc, empresaId, dataInicio, ultimoFech]),
    pool.query(
      `SELECT COALESCE(SUM(valor), 0) as total FROM eventos_rh
       WHERE funcionario_id = $1 AND empresa_id = $2 AND data_referencia >= $3 AND data_evento > $4`,
      [idFunc, empresaId, dataInicio, ultimoFech]),
    pool.query(
      `SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas
       WHERE funcionario_id = $1 AND empresa_id = $2 AND data_pagamento >= $3 AND data_pagamento > $4
       AND (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO')`,
      [idFunc, empresaId, dataInicio, ultimoFech]),
    pool.query(
      `SELECT COUNT(*) as cnt FROM eventos_rh WHERE funcionario_id = $1 AND empresa_id = $2
       AND data_referencia >= $3 AND data_evento > $4 AND tipo = 'INSS'`,
      [idFunc, empresaId, dataInicio, ultimoFech]),
    pool.query(
      `SELECT COUNT(*) as cnt FROM financeiro_saidas WHERE funcionario_id = $1 AND UPPER(descricao) LIKE '%ENCARGOS%'
       AND empresa_id = $2 AND data_pagamento >= $3 AND data_pagamento > $4 AND forma_pagamento = 'DESCONTO'`,
      [idFunc, empresaId, dataInicio, ultimoFech])
  ])

  const pago = parseFloat(pgRes.rows[0].total) || 0
  const descontosRH = parseFloat(rhRes.rows[0].total) || 0
  const descontosLegado = parseFloat(lgRes.rows[0].total) || 0
  const inssJaLancado = parseInt(inssRH.rows[0].cnt) > 0 || parseInt(inssLeg.rows[0].cnt) > 0
  let descontoInssAuto = 0
  if (!inssJaLancado && f.descontar_inss && parseFloat(f.valor_inss) > 0) {
    descontoInssAuto = parseFloat(f.valor_inss)
  }
  const saldo = Math.round((acumulado - pago - descontosRH - descontosLegado - descontoInssAuto) * 100) / 100

  // 13o provisao
  let provisao13 = 0
  if (f.recebe_decimo_terceiro && f.data_admissao) {
    const anoAtual = hoje.getFullYear()
    const inicioAno = new Date(anoAtual, 0, 1)
    const base = new Date(f.data_admissao) > inicioAno ? new Date(f.data_admissao) : inicioAno
    let meses = (hoje.getFullYear() - base.getFullYear()) * 12 + (hoje.getMonth() - base.getMonth()) + 1
    if (meses > 12) meses = 12
    if (meses < 0) meses = 0
    provisao13 = Math.round((salario / 12) * meses * 100) / 100
  }

  const isCicloAtual = inicio.getUTCMonth() === hoje.getUTCMonth() && inicio.getUTCFullYear() === hoje.getUTCFullYear()

  return {
    dias_trabalhados: dias, valor_diaria: Math.round(salarioDiario * 100) / 100,
    acumulado, pago, descontos_rh: descontosRH + descontosLegado,
    desconto_inss_auto: descontoInssAuto, inss_ja_lancado: inssJaLancado,
    saldo, provisao_13: provisao13, data_inicio: dataInicio, ciclo_atual: isCicloAtual
  }
}

async function getViagemAtivaCategoriaRH(db, empresaId, idViagemCliente) {
  // #DB214: nunca retornar fallback id=1 (pode pertencer a outra empresa ou inexistir —
  //   FK violation ou cross-tenant bleed). Lanca erro explicito se nao houver viagem/categoria.
  let viagemId = idViagemCliente ? parseInt(idViagemCliente) : null
  const queries = [
    db.query(`SELECT id FROM categorias_despesa WHERE empresa_id = $1 AND UPPER(nome) LIKE '%FUNCIONARIO%' LIMIT 1`, [empresaId])
  ]
  if (!viagemId) {
    queries.push(db.query(`SELECT id_viagem FROM viagens WHERE empresa_id = $1 AND is_atual = true LIMIT 1`, [empresaId]))
  }
  const results = await Promise.all(queries)
  const cRes = results[0]
  if (cRes.rows.length === 0) {
    const err = new Error('Empresa sem categoria de RH (FUNCIONARIO) cadastrada — configure antes de lancar folha')
    err.status = 400
    throw err
  }
  if (!viagemId) {
    const vRes = results[1]
    if (vRes.rows.length === 0) {
      const err = new Error('Empresa sem viagem ativa (is_atual=true) — defina uma antes de lancar folha')
      err.status = 400
      throw err
    }
    viagemId = vRes.rows[0].id_viagem
  }
  return { viagemId, categoriaId: cRes.rows[0].id }
}

// --- Funcionarios: Financeiro (summary) ---
router.get('/funcionarios/:id/financeiro', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    const fRes = await pool.query('SELECT * FROM funcionarios WHERE id = $1 AND empresa_id = $2', [idFunc, empresaId])
    if (fRes.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    res.json(await calcFinanceiroFuncionario(pool, idFunc, empresaId, fRes.rows[0]))
  } catch (err) { next(err) }
})

// --- Funcionarios: Historico mensal ---
router.get('/funcionarios/:id/historico', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    // #DS5-228: parseInt sem fallback retorna NaN — EXTRACT(MONTH FROM ...) = NaN nunca
    //   bate, mas pg-driver pode emitir warn. Validar e clamp em range.
    const mesRaw = parseInt(req.query.mes, 10)
    const anoRaw = parseInt(req.query.ano, 10)
    const mes = Number.isFinite(mesRaw) && mesRaw >= 1 && mesRaw <= 12 ? mesRaw : (new Date().getMonth() + 1)
    const ano = Number.isFinite(anoRaw) && anoRaw >= 2000 && anoRaw <= 2100 ? anoRaw : new Date().getFullYear()

    // Filtro por mes do CICLO do funcionario:
    //   - financeiro_saidas: usa mes_referencia (coluna dedicada), fallback data_pagamento
    //   - eventos_rh: usa data_referencia (ja existe)
    const result = await pool.query(`
      SELECT data_pagamento AS data, descricao, valor_pago AS valor, forma_pagamento, 'FIN' AS origem
      FROM financeiro_saidas
      WHERE funcionario_id = $1 AND empresa_id = $2
      AND ((forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO') OR is_excluido = false)
      AND EXTRACT(MONTH FROM COALESCE(mes_referencia, data_pagamento)) = $3
      AND EXTRACT(YEAR FROM COALESCE(mes_referencia, data_pagamento)) = $4
      UNION ALL
      SELECT data_evento AS data, descricao, valor, NULL AS forma_pagamento, 'RH' AS origem
      FROM eventos_rh
      WHERE funcionario_id = $5 AND empresa_id = $6
      AND EXTRACT(MONTH FROM COALESCE(data_referencia, data_evento)) = $7
      AND EXTRACT(YEAR FROM COALESCE(data_referencia, data_evento)) = $8
      ORDER BY data
    `, [idFunc, empresaId, mes, ano, idFunc, empresaId, mes, ano])

    const historico = result.rows.map(r => {
      let tipo
      if (r.origem === 'RH') {
        tipo = 'DESCONTO'
      } else {
        tipo = (r.forma_pagamento === 'DESCONTO' || r.forma_pagamento === 'RETIDO') ? 'DESCONTO' : 'DINHEIRO'
      }
      return { data: r.data, descricao: r.descricao, valor: parseFloat(r.valor), tipo }
    })

    res.json(historico)
  } catch (err) { next(err) }
})

// --- Funcionarios: Lancar pagamento ---
router.post('/funcionarios/:id/pagamento', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    const { descricao, valor, id_viagem } = req.body
    if (!descricao || !valor) return res.status(400).json({ error: 'descricao e valor obrigatorios' })
    // #DB218: rejeitar valores <= 0 (parseFloat("-50") passa o falsy check antes)
    const valorNum = Number(valor)
    if (!(valorNum > 0)) return res.status(400).json({ error: 'valor deve ser maior que zero' })

    const { viagemId, categoriaId } = await getViagemAtivaCategoriaRH(pool, empresaId, id_viagem)

    // mes_referencia = inicio do ciclo atual (data_inicio_calculo ou data_admissao)
    const fRes = await pool.query('SELECT data_inicio_calculo, data_admissao FROM funcionarios WHERE id = $1 AND empresa_id = $2', [idFunc, empresaId])
    const mesRef = fRes.rows[0]?.data_inicio_calculo || fRes.rows[0]?.data_admissao || null

    const hoje = new Date().toISOString().split('T')[0]
    await pool.query(`
      INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, data_pagamento,
        status, forma_pagamento, id_categoria, id_viagem, funcionario_id, is_excluido, mes_referencia, empresa_id)
      VALUES ($1, $2, $3, $4, $5, 'PAGO', 'DINHEIRO', $6, $7, $8, false, $9, $10)
    `, [descricao.toUpperCase(), parseFloat(valor), parseFloat(valor), hoje, hoje,
        categoriaId, viagemId, idFunc, mesRef, empresaId])

    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Funcionarios: Registrar falta ---
router.post('/funcionarios/:id/falta', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    const { data_falta } = req.body
    const dataFalta = data_falta || new Date().toISOString().split('T')[0]

    // verificar duplicata
    const dup = await pool.query(
      `SELECT COUNT(*) as cnt FROM eventos_rh WHERE funcionario_id = $1 AND empresa_id = $2 AND data_referencia = $3 AND tipo = 'FALTA'`,
      [idFunc, empresaId, dataFalta]
    )
    if (parseInt(dup.rows[0].cnt) > 0) return res.status(400).json({ error: 'Ja existe falta registrada para esta data' })

    const fRes = await pool.query('SELECT salario, nome, data_inicio_calculo, data_admissao FROM funcionarios WHERE id = $1 AND empresa_id = $2', [idFunc, empresaId])
    if (fRes.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    const salario = parseFloat(fRes.rows[0].salario) || 0
    const nome = fRes.rows[0].nome
    const valorDesconto = Math.round((salario / 30) * 100) / 100
    const descricao = `FALTA - ${nome.toUpperCase()} - ${dataFalta}`
    // data_referencia = inicio do ciclo atual, pra agrupar a falta no mes do ciclo
    const dataRef = fRes.rows[0].data_inicio_calculo || fRes.rows[0].data_admissao || dataFalta

    await pool.query(
      `INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia, empresa_id)
       VALUES ($1, 'FALTA', $2, $3, $4, $5, $6)`,
      [idFunc, descricao, valorDesconto, dataFalta, dataRef, empresaId]
    )

    res.json({ ok: true, valor: valorDesconto })
  } catch (err) { next(err) }
})

// --- Funcionarios: Lancar desconto manual ---
router.post('/funcionarios/:id/desconto', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    const { descricao, valor } = req.body
    if (!descricao || !valor) return res.status(400).json({ error: 'descricao e valor obrigatorios' })
    // #DB218: valor deve ser > 0; negativo corromperia folha
    const valorNum = Number(valor)
    if (!(valorNum > 0)) return res.status(400).json({ error: 'valor deve ser maior que zero' })

    const fRes = await pool.query('SELECT data_inicio_calculo, data_admissao FROM funcionarios WHERE id = $1 AND empresa_id = $2', [idFunc, empresaId])
    if (fRes.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    const hoje = new Date().toISOString().split('T')[0]
    // data_referencia = inicio do ciclo atual, pra agrupar no mes do ciclo
    const dataRef = fRes.rows[0].data_inicio_calculo || fRes.rows[0].data_admissao || hoje

    await pool.query(
      `INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia, empresa_id)
       VALUES ($1, 'DESCONTO_MANUAL', $2, $3, $4, $5, $6)`,
      [idFunc, ('DESC. ' + descricao).toUpperCase(), parseFloat(valor), hoje, dataRef, empresaId]
    )

    res.json({ ok: true })
  } catch (err) { next(err) }
})

// --- Funcionarios: Fechar mes ---
// #DB215: transacao envolvendo 3 operacoes (INSS + FECHAMENTO + UPDATE data_inicio_calculo).
//   Sem tx, falha parcial duplica INSS ou re-paga ciclo seguinte.
// #DB219: transicao de mes usa TZ BR (nao UTC) — evita drift no fim do dia.
router.post('/funcionarios/:id/fechar-mes', async (req, res, next) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const idFunc = req.params.id
    const { id_viagem } = req.body || {}

    const fRes = await client.query('SELECT * FROM funcionarios WHERE id = $1 AND empresa_id = $2', [idFunc, empresaId])
    if (fRes.rows.length === 0) return res.status(404).json({ error: 'Funcionario nao encontrado' })
    const f = fRes.rows[0]

    const dataInicio = f.data_inicio_calculo || f.data_admissao
    if (!dataInicio) return res.status(400).json({ error: 'Funcionario sem data de admissao' })

    const financeiro = await calcFinanceiroFuncionario(client, idFunc, empresaId, f)
    const saldoParaPagar = financeiro.saldo
    // hoje em TZ BR (sv-SE retorna YYYY-MM-DD local)
    const hojeISO = new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' })

    await client.query('BEGIN')

    const descontoInss = financeiro.desconto_inss_auto || 0
    if (descontoInss > 0) {
      await client.query(
        `INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia, empresa_id)
         VALUES ($1, 'INSS', 'DESC. ENCARGOS (INSS/FOLHA)', $2, $3, $4, $5)`,
        [idFunc, descontoInss, hojeISO, dataInicio, empresaId]
      )
    }

    if (saldoParaPagar > 0) {
      const { viagemId, categoriaId } = await getViagemAtivaCategoriaRH(client, empresaId, id_viagem)
      const desc = `FECHAMENTO MENSAL ${(f.nome || '').toUpperCase()}`
      await client.query(`
        INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, data_pagamento,
          status, forma_pagamento, id_categoria, id_viagem, funcionario_id, is_excluido, mes_referencia, empresa_id)
        VALUES ($1, $2, $3, $4, $5, 'PAGO', 'DINHEIRO', $6, $7, $8, false, $9, $10)
      `, [desc, saldoParaPagar, saldoParaPagar, hojeISO, hojeISO, categoriaId, viagemId, idFunc, dataInicio, empresaId])
    }

    // proxima data_inicio_calculo = hojeISO + 1 dia (calculado em string pra evitar UTC/BR drift)
    const [yy, mm, dd] = hojeISO.split('-').map(Number)
    const proxima = new Date(yy, mm - 1, dd + 1)
    const novaDataISO = proxima.toLocaleDateString('sv-SE')

    await client.query(
      'UPDATE funcionarios SET data_inicio_calculo = $1 WHERE id = $2 AND empresa_id = $3',
      [novaDataISO, idFunc, empresaId]
    )

    await client.query('COMMIT')
    res.json({ ok: true, saldo_pago: saldoParaPagar, nova_data_inicio: novaDataISO, inss_gravado: descontoInss })
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {})
    next(err)
  } finally {
    client.release()
  }
})

// --- Funcionarios: Demitir ---
router.post('/funcionarios/:id/demitir', async (req, res, next) => {
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
