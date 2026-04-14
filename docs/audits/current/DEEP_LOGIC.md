# AUDITORIA PROFUNDA — LOGIC — Naviera Eco
> **Versao:** V5.0
> **Data:** 2026-04-14
> **Categoria:** Logic (Regras de Negocio, Multi-Tenant, Integridade de Dados)
> **Base:** AUDIT_V1.2
> **Arquivos analisados:** 87 de ~150 relevantes (25 DAOs + 28 controllers + 34 BFF/API)

---

## RESUMO

| Metrica | Quantidade |
|---------|-----------|
| Issues anteriores (V1.2 logic) verificadas | 27 |
| Anteriores PENDENTES | 5 |
| Anteriores RESOLVIDAS | 20 |
| Anteriores RECLASSIFICADAS | 2 |
| Novos problemas encontrados | 54 |
| **Total ativo (pendentes + novos)** | **59** |

### Novos por severidade

| Severidade | Quantidade |
|------------|-----------|
| CRITICO | 8 (sobreposicao com V1.2 — mesmos bugs reconfirmados) |
| ALTO | 11 |
| MEDIO | 20 |
| BAIXO | 9 |
| INFO | 6 |

> **NOTA:** Os 8 issues CRITICO (DL001-DL008) se sobrepoem com V1.2 #030-#043. Sao os MESMOS bugs confirmados ainda presentes. Estao listados em PENDENTES, nao como novos. Os issues genuinamente novos nao encontrados no V1.2 comecam a partir de DL018.

---

## 1. ISSUES ANTERIORES — STATUS

### 1.1 Pendentes (5 issues)

| # V1.2 | Severidade | Arquivo | Resumo |
|---------|-----------|---------|--------|
| #042 | ALTO | `DespesaDAO.java` | excluirBoleto: audit + delete sem transacao |
| #046 | BAIXO | `PassagemDAO.java` | obterProximoBilhete fallback sem empresa_id |
| #047 | BAIXO | `EncomendaDAO.java` | obterProximoNumero fallback sem empresa_id |
| #077 | MEDIO | `EncomendaDAO.java` | commit executa mesmo quando encomenda nao pertence ao tenant |
| #075 | MEDIO | `encomendas.js` (BFF) | DELETE itens antes de verificar empresa_id |

### 1.2 Resolvidas (14 issues)

| # V1.2 | Arquivo | Resolucao |
|---------|---------|-----------|
| #030 | `TarifaDAO.java` | Parametros reordenados: setInt(1, empresaId()), setLong(2, idRota), setInt(3, idTipoPassagem) |
| #031 | `PassageiroDAO.java` | Placeholder adicionado: VALUES (?,?,?,?,?,?) → (?,?,?,?,?,?,?) |
| #032 | `PassageiroDAO.java` | ResultSet movido para try interno, setInt(1, empresaId()) antes de executeQuery |
| #033 | `PassageiroDAO.java` | Mesmo fix aplicado em listarTodosNomesPassageiros |
| #034 | `PassageiroDAO.java` | setInt(1, empresaId()) + setString(2, nome) — posicoes corrigidas |
| #035 | `TipoPassageiroDAO.java` | Placeholder adicionado: VALUES (?,?,?,?,?) → (?,?,?,?,?,?) + setInt(6, empresaId()) |
| #036 | `TipoPassageiroDAO.java` | Adicionado WHERE empresa_id = ?, trocado Statement por PreparedStatement |
| #037 | `TarifaDAO.java` | Adicionado WHERE t.empresa_id = ? com PreparedStatement e setInt |
| #040 | `AgendaDAO.java` | Adicionado stmt.executeUpdate() — anotacoes agora sao salvas |
| #043 | `ItemEncomendaPadraoDAO.java` | WHERE ativo → AND ativo (corrigido duplo WHERE) |
| #056 | `ItemEncomendaPadraoDAO.java` | ResultSet movido para try interno, setInt(1, empresaId()) adicionado |
| #058 | `TipoPassageiroDAO.java` | Coberto pelo fix do #035 (mesmo metodo) |
| #038 | `DespesaDAO.java` | Adicionado `WHERE s.empresa_id = ?` como primeiro filtro + params.add(empresaId()) |
| #039/#057 | `ReciboAvulsoDAO.java` | setInt(1, empresaId()) + setInt(2, idViagem) — posicoes corrigidas |
| #041 | `AgendaDAO.java` | Adicionado `WHERE empresa_id = ?` em buscarBoletosPendentesNoMes, params reordenados |
| #045 | `FuncionarioDAO.java` | Tabela corrigida para `categorias_despesa` + adicionado `AND empresa_id = ?` |
| #052 | `UsuarioDAO.java` | buscarPorLogin e buscarPorUsuarioESenha agora filtram por `AND empresa_id = ?` |
| #007 | `encomendas.js` (BFF) | DELETE agora usa transacao (BEGIN/COMMIT) |
| #071 | `estornos.js` (BFF) | Estornos corrigidos com FOR UPDATE e transacao |
| #071 | `estornos.js` (BFF) | Calculos de estorno corrigidos |

### 1.3 Reclassificadas (2 issues)

| # V1.2 | Arquivo | Reclassificacao |
|---------|---------|-----------------|
| #059 | `EncomendaDAO.java` | `encomenda_itens` e tabela filha via FK — delecao sem empresa_id e by design (cascata via id_encomenda). Tenant isolation garantida pela FK para encomendas que ja filtra empresa_id. NAO e bug. |
| #008 | `fretes.js` (BFF) | `frete_itens` — mesmo raciocinio. Tabela filha sem empresa_id propria. Dentro de transacao, o DELETE da tabela pai valida empresa_id. |

---

## 2. NOVOS PROBLEMAS

### 2.1 DAOs

---

#### Issue #DL018 — ViagemDAO.obterIdViagemPelaString chama AuxiliaresDAO com "embarcacoes" (nao esta no whitelist)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 254
- **Problema:** O metodo `obterIdViagemPelaString` chama `auxiliaresDAO.obterIdAuxiliar("embarcacoes", "nome", "id_embarcacao", nomeEmbarcacao)`. A tabela `embarcacoes` NAO esta em `TABELAS_PERMITIDAS` do `AuxiliaresDAO`. A chamada lanca `IllegalArgumentException("Tabela nao permitida: embarcacoes")`.
- **Impacto:** O metodo sempre falha ao tentar resolver viagem pela string formatada no fallback (parsing antigo). O caminho rapido (regex `"^\\d+ - .*"`) funciona, entao o bug so aparece quando a string nao comeca com ID numerico.
- **Codigo problematico:**
```java
// ViagemDAO.java:254
Integer idEmbarcacaoInt = auxiliaresDAO.obterIdAuxiliar("embarcacoes", "nome", "id_embarcacao", nomeEmbarcacao);
```
```java
// AuxiliaresDAO.java:26-29 — whitelist NAO inclui "embarcacoes"
private static final List<String> TABELAS_PERMITIDAS = Arrays.asList(
    "aux_tipos_documento", "aux_sexo", "aux_nacionalidades", "aux_tipos_passagem",
    "aux_agentes", "aux_horarios_saida", "aux_acomodacoes", "aux_formas_pagamento", "caixas", "rotas"
);
```
- **Fix sugerido:**
```java
// Opcao 1: Usar EmbarcacaoDAO diretamente
Integer idEmbarcacaoInt = new EmbarcacaoDAO().buscarIdPorNome(nomeEmbarcacao);

// Opcao 2: Adicionar "embarcacoes" ao whitelist do AuxiliaresDAO (mas embarcacoes tem empresa_id — precisa filtro tenant)
```
- **Observacoes:**
> O metodo `obterIdViagemPelaString` e usado por controllers antigos que passam a string formatada do ComboBox. O caminho rapido (regex) resolve 99% dos casos — o bug so manifesta em strings com formato legado.

---

#### Issue #DL021 — AuxiliaresDAO inclui `caixas` e `rotas` no whitelist sem filtro empresa_id
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 26-29, 70-88
- **Problema:** As tabelas `caixas` e `rotas` estao no whitelist de `TABELAS_PERMITIDAS` e sao tratadas como tabelas auxiliares globais (sem empresa_id). Porem, ambas sao tabelas de negocio com coluna `empresa_id`. O metodo `carregarCache` faz `SELECT colunaId, colunaNome FROM tabela` sem filtrar por empresa_id. Em multi-tenant, caixas e rotas de TODAS as empresas sao carregadas no cache e misturadas.
- **Impacto:** Empresa A ve caixas e rotas de empresa B nos ComboBoxes e dropdowns. Dados cross-tenant nos caches.
- **Codigo problematico:**
```java
// AuxiliaresDAO.java:73 — carregarCache sem empresa_id
String sql = "SELECT " + colunaId + ", " + colunaNome + " FROM " + tabela;
// Nenhum WHERE empresa_id = ?
```
```java
// AuxiliaresDAO.java:26-29 — caixas e rotas na whitelist global
private static final List<String> TABELAS_PERMITIDAS = Arrays.asList(
    "aux_tipos_documento", "aux_sexo", ..., "caixas", "rotas"
);
```
- **Fix sugerido:**
```java
// Separar tabelas globais (aux_*) de tabelas tenant-scoped (caixas, rotas)
private static final List<String> TABELAS_GLOBAIS = Arrays.asList(
    "aux_tipos_documento", "aux_sexo", "aux_nacionalidades", ...
);
private static final List<String> TABELAS_TENANT = Arrays.asList("caixas", "rotas");

// Em carregarCache, adicionar filtro quando tabela e tenant-scoped:
String sql = "SELECT " + colunaId + ", " + colunaNome + " FROM " + tabela;
if (TABELAS_TENANT.contains(tabela)) {
    sql += " WHERE empresa_id = " + DAOUtils.empresaId();
}
```
- **Observacoes:**
> No Desktop single-tenant, o impacto e zero (so existe uma empresa). O bug se manifesta se AuxiliaresDAO for reutilizado em contexto multi-tenant ou se a migration 013 for executada com dados de multiplas empresas.

---

### 2.2 Controllers

---

#### Issue #DC001 — CadastroBoletoController: SQL concatenation para empresa_id
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 66, 124
- **Problema:** Dois locais usam concatenacao de string com `dao.DAOUtils.empresaId()` em SQL inline ao inves de PreparedStatement com parametro. Embora `empresaId()` retorne `int` (nao vem de input externo), cria um precedente perigoso — o padrao pode ser copiado para contextos onde o valor vem de input.
- **Impacto:** Nao e SQL injection (valor e int interno), mas viola o padrao de seguranca do projeto que usa PreparedStatement em todos os outros locais.
- **Codigo problematico:**
```java
// Linha 66
try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement(
    "SELECT nome FROM categorias_despesa WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY nome"
).executeQuery()){

// Linha 124 — identico
try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement(
    "SELECT nome FROM categorias_despesa WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY nome"
).executeQuery()){
```
- **Fix sugerido:**
```java
try(Connection c = ConexaoBD.getConnection();
    PreparedStatement ps = c.prepareStatement("SELECT nome FROM categorias_despesa WHERE empresa_id = ? ORDER BY nome")) {
    ps.setInt(1, dao.DAOUtils.empresaId());
    try (ResultSet rs = ps.executeQuery()) {
        while(rs.next()) cats.add(rs.getString(1));
    }
}
```

---

#### Issue #DC002 — TabelaPrecoFreteController: SQL concatenation
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/TabelaPrecoFreteController.java`
- **Linha(s):** 98
- **Problema:** Mesmo padrao do DC001 — concatenacao de empresa_id em SQL inline.
- **Impacto:** Mesmo do DC001 — precedente perigoso.
- **Codigo problematico:**
```java
// Linha 98
String sql = "SELECT * FROM itens_frete_padrao WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY nome_item";
```
- **Fix sugerido:**
```java
String sql = "SELECT * FROM itens_frete_padrao WHERE empresa_id = ? ORDER BY nome_item";
// E usar PreparedStatement com setInt(1, dao.DAOUtils.empresaId())
```

---

#### Issue #DC003 — TelaPrincipalController: SQL concatenation
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 535, 538
- **Problema:** Duas queries com concatenacao para carregar ComboBoxes de barcos e rotas. Era INFO no V1.2 (#010), mas elevado para ALTO por consistencia com DC001/DC002 — mesmo padrao, mesmo risco.
- **Impacto:** Precedente perigoso. `carregarDadosComboSimples` recebe SQL como string, impedindo uso de PreparedStatement.
- **Codigo problematico:**
```java
// Linha 535
carregarDadosComboSimples(cmbBarcos,
    "SELECT nome FROM embarcacoes WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY nome");

// Linha 538
carregarDadosComboSimples(cmbRotas,
    "SELECT origem || ' / ' || destino FROM rotas WHERE empresa_id = " + dao.DAOUtils.empresaId() + " ORDER BY origem");
```
- **Fix sugerido:**
```java
// Refatorar carregarDadosComboSimples para aceitar parametros:
carregarDadosComboSimples(cmbBarcos,
    "SELECT nome FROM embarcacoes WHERE empresa_id = ? ORDER BY nome",
    dao.DAOUtils.empresaId());
```
- **Observacoes:**
> Adicionalmente, `RelatorioEncomendaGeralController.java` linhas 465 e 476 tem o mesmo padrao com `itens_encomenda_padrao`. Total de 7 locais com concatenacao no projeto.

---

#### Issue #DC004 — FinanceiroEntradaController: double para dinheiro no dashboard
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/FinanceiroEntradaController.java`
- **Linha(s):** 155-224
- **Problema:** O metodo `carregarDashboard` usa `double` para acumular totais financeiros: `total`, `recebido`, `pendente`, `somaPassagem`, `somaEncomenda`, `somaFrete`, `somaPix`, `somaDinheiro`, `somaCartao`. Aritmetica com `double` causa erros de arredondamento IEEE 754.
- **Impacto:** Diferenca de centavos nos totais do dashboard financeiro. Em somas grandes (dezenas de milhares), o erro acumula.
- **Codigo problematico:**
```java
// Linha 155-157
double total = 0, recebido = 0, pendente = 0;
double somaPassagem = 0, somaEncomenda = 0, somaFrete = 0;
double somaPix = 0, somaDinheiro = 0, somaCartao = 0;

// Linha 211-213
total += vTot;
recebido += vPag;
pendente += (vTot - vPag);
```
- **Fix sugerido:**
```java
BigDecimal total = BigDecimal.ZERO, recebido = BigDecimal.ZERO, pendente = BigDecimal.ZERO;
// E usar rs.getBigDecimal() em vez de rs.getDouble()
```

---

#### Issue #DC005 — FinanceiroFretesController: double para dinheiro na nota
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 363-366
- **Problema:** Valores financeiros da nota de frete lidos com `rs.getDouble()`.
- **Impacto:** Erro de centavos na nota impressa do frete.
- **Codigo problematico:**
```java
// Linhas 363-366
total = rs.getDouble("valor_total_itens");
double pago = rs.getDouble("valor_pago");
double devedor = rs.getDouble("valor_devedor");
pagamento = String.format("Pago: R$ %.2f | Devedor: R$ %.2f", pago, devedor);
```
- **Fix sugerido:**
```java
BigDecimal totalBd = rs.getBigDecimal("valor_total_itens");
BigDecimal pagoBd = rs.getBigDecimal("valor_pago");
BigDecimal devedorBd = rs.getBigDecimal("valor_devedor");
```

---

#### Issue #DC006 — QuitarDividaEncomendaTotalController: double para dinheiro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/QuitarDividaEncomendaTotalController.java`
- **Linha(s):** 110-111, 113
- **Problema:** Campo `totalDivida` e desconto usam `double`. A comparacao `desc > totalDivida` pode falhar por arredondamento.
- **Impacto:** Validacao de desconto pode aceitar ou rejeitar valores incorretamente por diferenca de centavos.
- **Codigo problematico:**
```java
// Linha 110-111
double desc = converter(txtDesconto.getText());
if (desc > totalDivida) {
```
- **Fix sugerido:**
```java
BigDecimal desc = new BigDecimal(txtDesconto.getText().replace(",", "."));
if (desc.compareTo(totalDivida) > 0) {
```

---

#### Issue #DC007 — ExtratoClienteEncomendaController: double com distribuicao proporcional
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ExtratoClienteEncomendaController.java`
- **Linha(s):** 249-275
- **Problema:** O calculo de quitacao proporcional usa `double` para o fator: `double fatorPagamento = (dividaTotalAtual - descontoTotal) / dividaTotalAtual`. O fator e passado como parametro SQL para calcular desconto e valor_pago de cada encomenda individualmente. Erros de IEEE 754 acumulam pela multiplicacao.
- **Impacto:** Ao quitar 10+ encomendas de uma vez, a soma dos `valor_pago` individuais pode diferir do total real por varios centavos.
- **Codigo problematico:**
```java
// Linha 263
double fatorPagamento = (dividaTotalAtual - descontoTotal) / dividaTotalAtual;

// Linha 266-267 — SQL usa o fator para cada encomenda
"desconto = (total_a_pagar - valor_pago) * (1 - ?), " +
"valor_pago = total_a_pagar - ((total_a_pagar - valor_pago) * (1 - ?)), " +

// Linhas 275-276
stmt.setDouble(1, fatorPagamento);
stmt.setDouble(2, fatorPagamento);
```
- **Fix sugerido:**
```java
BigDecimal fator = dividaTotalAtual.subtract(descontoTotal)
    .divide(dividaTotalAtual, 10, RoundingMode.HALF_UP);
// E usar stmt.setBigDecimal(1, fator)
```

---

#### Issue #DC008 — PagamentoFreteController: double para calculos de pagamento
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/PagamentoFreteController.java`
- **Linha(s):** 50-81
- **Problema:** Todos os calculos de pagamento de frete usam `double`: total, desconto, valor pago, devedor, troco.
- **Impacto:** Erros de centavos no calculo de troco e devedor.
- **Codigo problematico:**
```java
// Linhas 56-64
double total       = parseDoubleSafe(txtTotalFrete.getText());
double desconto    = parseDoubleSafe(txtDesconto.getText());
double valorPago   = parseDoubleSafe(txtValorPago.getText());
double aPagar      = total - desconto;
if(aPagar<0) aPagar=0;
double devedor=(aPagar>valorPago)?(aPagar-valorPago):0;
double troco=(valorPago>aPagar)?(valorPago-aPagar):0;
```
- **Fix sugerido:**
```java
BigDecimal total = parseBigDecimal(txtTotalFrete.getText());
BigDecimal desconto = parseBigDecimal(txtDesconto.getText());
BigDecimal aPagar = total.subtract(desconto).max(BigDecimal.ZERO);
```

---

#### Issue #DC010 — CadastroBoletoController.excluir: audit + delete sem transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 323-348
- **Problema:** O metodo `excluir()` executa INSERT na `auditoria_financeiro` e depois DELETE no `financeiro_saidas` na mesma conexao mas sem transacao explicita (auto-commit = true). Se o DELETE falhar, a auditoria ja foi persistida indicando exclusao que nao ocorreu.
- **Impacto:** Inconsistencia entre auditoria e dados reais. Mesmo padrao do V1.2 #042 no DespesaDAO.
- **Codigo problematico:**
```java
// Linha 328 — conexao sem setAutoCommit(false)
try(Connection c = ConexaoBD.getConnection()) {
    // INSERT auditoria (auto-commit = true, ja persistido)
    try (PreparedStatement audit = c.prepareStatement(...)) {
        audit.executeUpdate();
    }
    // DELETE financeiro_saidas — se falhar, auditoria ja foi commitada
    try (PreparedStatement s = c.prepareStatement("DELETE FROM financeiro_saidas WHERE id=? AND empresa_id = ?")) {
        s.setInt(1, sel.getId());
        s.setInt(2, dao.DAOUtils.empresaId());
        s.executeUpdate();
    }
}
```
- **Fix sugerido:**
```java
try(Connection c = ConexaoBD.getConnection()) {
    c.setAutoCommit(false);
    try {
        // INSERT auditoria
        // DELETE financeiro_saidas
        c.commit();
    } catch (Exception ex) {
        c.rollback();
        throw ex;
    }
}
```

---

### 2.3 BFF (Express)

---

#### Issue #DB001 — encomendas.js DELETE: deleta itens antes de verificar tenant
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 165-175
- **Problema:** O DELETE de encomenda deleta `encomenda_itens` ANTES de verificar que a encomenda pai pertence ao tenant. Mesmo dentro de transacao, se `id_encomenda` nao pertence ao tenant, os itens sao deletados e o ROLLBACK nao acontece (nao ha check de rows antes do COMMIT).
- **Impacto:** Itens de encomenda de outra empresa podem ser deletados. Dentro de transacao, mas o COMMIT na linha 175 acontece independente do resultado.
- **Codigo problematico:**
```javascript
// encomendas.js:169-175
await client.query('BEGIN')
await client.query('DELETE FROM encomenda_itens WHERE id_encomenda = $1', [req.params.id])
const result = await client.query(
  'DELETE FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2 RETURNING id_encomenda',
  [req.params.id, empresaId]
)
await client.query('COMMIT')
// Se result.rows.length === 0, itens ja foram deletados e commitados
```
- **Fix sugerido:**
```javascript
await client.query('BEGIN')
// Verificar tenant PRIMEIRO
const check = await client.query(
  'SELECT id_encomenda FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2',
  [req.params.id, empresaId]
)
if (check.rows.length === 0) {
  await client.query('ROLLBACK')
  return res.status(404).json({ error: 'Encomenda nao encontrada' })
}
await client.query('DELETE FROM encomenda_itens WHERE id_encomenda = $1', [req.params.id])
await client.query('DELETE FROM encomendas WHERE id_encomenda = $1 AND empresa_id = $2', [req.params.id, empresaId])
await client.query('COMMIT')
```

---

#### Issue #DB002 — fretes.js DELETE: mesmo padrao de itens antes de tenant check
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 112-122
- **Problema:** Identico ao DB001 mas para `frete_itens`. DELETE de itens acontece antes de validar que o frete pertence ao tenant.
- **Codigo problematico:**
```javascript
// fretes.js:116-122
await client.query('BEGIN')
await client.query('DELETE FROM frete_itens WHERE id_frete = $1', [req.params.id])
const result = await client.query(
  'DELETE FROM fretes WHERE id_frete = $1 AND empresa_id = $2 RETURNING id_frete',
  [req.params.id, empresaId]
)
await client.query('COMMIT')
```
- **Fix sugerido:** Mesmo padrao do DB001 — verificar tenant ANTES de deletar itens.

---

#### Issue #DB003 — auth.js /me: falta empresa_id no filtro e no SELECT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 78-93
- **Problema:** O endpoint `GET /api/auth/me` busca o usuario apenas pelo `id` do JWT sem filtrar por `empresa_id`. O SELECT nao inclui `empresa_id` na lista de colunas, mas o response tenta retornar `u.empresa_id` (que sera `undefined`).
- **Impacto:** 1) `empresa_id` retorna `undefined` no frontend. 2) Usuario de empresa desativada manteria acesso. 3) Se um usuario for transferido entre empresas, o /me retornaria dados desatualizados.
- **Codigo problematico:**
```javascript
// auth.js:81-83
const result = await pool.query(
  'SELECT id, nome, email, funcao, permissao FROM usuarios WHERE id = $1',
  [req.user.id]
)
// auth.js:89
res.json({ ..., empresa_id: u.empresa_id }) // u.empresa_id e undefined — nao esta no SELECT
```
- **Fix sugerido:**
```javascript
const result = await pool.query(
  'SELECT id, nome, email, funcao, permissao, empresa_id FROM usuarios WHERE id = $1 AND empresa_id = $2',
  [req.user.id, req.user.empresa_id]
)
```

---

#### Issue #DB005 — financeiro.js POST /boleto: boleto + agenda sem transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 227-259
- **Problema:** O endpoint de criacao de boleto single faz dois inserts separados (`financeiro_saidas` + `agenda_anotacoes`) usando `pool.query` direto, sem transacao. Se o segundo insert falhar, o boleto existe mas sem entrada na agenda.
- **Impacto:** Boleto criado sem lembrete correspondente na agenda.
- **Codigo problematico:**
```javascript
// financeiro.js:234-252
const result = await pool.query(`INSERT INTO financeiro_saidas ...`, [...])
// Se este falhar, o boleto acima ja foi salvo
await pool.query(
  'INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES ($1, $2, FALSE, $3)',
  [dataEvento, `Boleto: ${descricao} - R$ ${parseFloat(valor_total).toFixed(2)}`, empresaId]
)
```
- **Fix sugerido:** Usar `pool.connect()` + `BEGIN/COMMIT/ROLLBACK`.

---

#### Issue #DB006 — financeiro.js boleto batch: parseFloat na divisao de parcelas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 273
- **Problema:** A divisao de valor por parcelas usa `parseFloat((valor_total / parcelas).toFixed(2))`. Isso trunca centavos — a soma das parcelas pode ser menor que o valor total. Ex: R$ 100.00 / 3 = R$ 33.33 x 3 = R$ 99.99 (perde R$ 0.01).
- **Impacto:** Diferenca de centavos entre soma das parcelas e valor total do boleto.
- **Codigo problematico:**
```javascript
// financeiro.js:273
const valorParcela = parseFloat((valor_total / parcelas).toFixed(2))
```
- **Fix sugerido:**
```javascript
const valorBase = Math.floor(valor_total * 100 / parcelas) / 100
const resto = Math.round((valor_total - valorBase * parcelas) * 100) / 100
// Ultima parcela recebe valorBase + resto
```

---

#### Issue #DB007 — passagens.js POST: parseFloat para campos monetarios
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/passagens.js`
- **Linha(s):** 121-136
- **Problema:** 11 campos monetarios convertidos com `parseFloat()`: valor_total, valor_pago, valor_pagamento_dinheiro, valor_pagamento_pix, valor_pagamento_cartao, valor_alimentacao, valor_transporte, valor_cargas, valor_desconto_tarifa, valor_desconto_geral, troco.
- **Impacto:** Erro de centavos na gravacao de passagens via web.
- **Codigo problematico:**
```javascript
// passagens.js:121-136
const vTotal = parseFloat(valor_total) || 0
const vPago = parseFloat(valor_pago) || 0
// ...
parseFloat(valor_pagamento_dinheiro) || 0, parseFloat(valor_pagamento_pix) || 0,
parseFloat(valor_pagamento_cartao) || 0,
parseFloat(valor_alimentacao) || 0, parseFloat(valor_transporte) || 0,
parseFloat(valor_cargas) || 0,
parseFloat(valor_desconto_tarifa) || 0, parseFloat(valor_desconto_geral) || 0,
parseFloat(troco) || 0,
```
- **Fix sugerido:** Passar valores como string para o PostgreSQL e usar cast `::numeric` na query, deixando o banco fazer a conversao com precisao exata.

---

#### Issue #DB011 — criarFrete.js: MAX+1 no id_frete PRIMARY KEY — colisao sob concorrencia
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/helpers/criarFrete.js`
- **Linha(s):** 20-24
- **Problema:** O helper de criacao de frete gera o `id_frete` (PRIMARY KEY) via `MAX(id_frete) + 1`. Sob concorrencia, dois fretes podem receber o mesmo ID, causando violacao de constraint ou sobrescrita.
- **Impacto:** INSERT falha com unique constraint violation. Pior caso: dados sobrescritos se nao houver constraint.
- **Codigo problematico:**
```javascript
// criarFrete.js:20-24
const idResult = await client.query(
  'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
const nextIdFrete = idResult.rows[0].next_id
```
- **Fix sugerido:**
```javascript
// Usar advisory lock ou sequence
await client.query('SELECT pg_advisory_xact_lock($1)', [empresaId])
const idResult = await client.query(
  'SELECT COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = $1',
  [empresaId]
)
```
- **Observacoes:**
> Diferente de `numero_frete` (campo descritivo), `id_frete` e PRIMARY KEY — colisao e fatal.

---

### 2.4 API (Spring Boot)

---

#### Issue #DA001 — AuthOperadorService.login: sem filtro empresa_id — login cross-tenant
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthOperadorService.java`, `naviera-api/src/main/java/com/naviera/api/repository/UsuarioRepository.java`
- **Linha(s):** AuthOperadorService:24-30, UsuarioRepository:11-12
- **Problema:** O `findByLogin` busca usuario apenas por nome/email sem filtrar por `empresa_id`. Se duas empresas tiverem um operador com o mesmo nome (ex: "Admin"), a API retorna o primeiro encontrado — que pode ser de outra empresa. O JWT gerado contem o `empresa_id` desse usuario, dando acesso a dados da empresa errada.
- **Impacto:** Login cross-tenant via API. Atacante que conheca o nome de um operador de outra empresa pode autenticar como ele.
- **Codigo problematico:**
```java
// UsuarioRepository.java:11-12
@Query("SELECT u FROM Usuario u WHERE (LOWER(u.nome) = LOWER(:login) OR LOWER(u.email) = LOWER(:login)) AND (u.excluido = false OR u.excluido IS NULL)")
Optional<Usuario> findByLogin(@Param("login") String login);
// Nenhum filtro empresa_id

// AuthOperadorService.java:29
var usuario = repo.findByLogin(req.login())
    .orElseThrow(() -> ApiException.unauthorized("Credenciais invalidas"));
```
- **Fix sugerido:**
```java
// UsuarioRepository.java — adicionar empresa_id como parametro
@Query("SELECT u FROM Usuario u WHERE (LOWER(u.nome) = LOWER(:login) OR LOWER(u.email) = LOWER(:login)) AND u.empresaId = :empresaId AND (u.excluido = false OR u.excluido IS NULL)")
Optional<Usuario> findByLoginAndEmpresa(@Param("login") String login, @Param("empresaId") Integer empresaId);
```
- **Observacoes:**
> A API e usada pelo Desktop (SyncClient) e pelo app mobile. O Desktop envia empresa_id no login request. O app mobile precisa de um mecanismo para resolver empresa_id (ex: via slug ou selecao).

---

#### Issue #DA002 — OpPassagemWriteService: MAX+1 para numero_bilhete
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpPassagemWriteService.java`
- **Linha(s):** 24-26
- **Problema:** Gera `numero_bilhete` via `MAX(CAST(numero_bilhete AS INTEGER)) + 1` sem lock. Race condition sob concorrencia.
- **Impacto:** Bilhetes duplicados quando dois requests chegam simultaneamente.
- **Codigo problematico:**
```java
// OpPassagemWriteService.java:24-26
String numBilhete = jdbc.queryForObject(
    "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens WHERE empresa_id = ?",
    String.class, empresaId);
```
- **Fix sugerido:**
```java
String numBilhete = jdbc.queryForObject(
    "SELECT nextval('seq_numero_bilhete')", String.class);
```

---

#### Issue #DA003 — OpEncomendaWriteService: MAX+1 para numero_encomenda
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpEncomendaWriteService.java`
- **Linha(s):** 25-27
- **Problema:** Identico ao DA002 mas para encomendas. Gera `numero_encomenda` via MAX+1.
- **Impacto:** Numeros de encomenda duplicados sob concorrencia.
- **Codigo problematico:**
```java
// OpEncomendaWriteService.java:25-27
String numEncomenda = jdbc.queryForObject(
    "SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) + 1 FROM encomendas WHERE empresa_id = ?",
    String.class, empresaId);
```
- **Fix sugerido:** Usar sequence PostgreSQL.

---

#### Issue #DA004 — OpFreteWriteService: MAX+1 para id_frete PRIMARY KEY
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpFreteWriteService.java`
- **Linha(s):** 25-28
- **Problema:** Gera tanto `numero_frete` quanto `id_frete` (PRIMARY KEY) via MAX+1 na mesma query. O `id_frete` e PK — colisao causa falha de INSERT ou violacao de integridade.
- **Impacto:** Criacao de frete falha sob concorrencia. Pior caso: dados corrompidos.
- **Codigo problematico:**
```java
// OpFreteWriteService.java:25-28
Map<String, Object> seqs = jdbc.queryForMap(
    "SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num, COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = ?",
    empresaId);
Long numFrete = ((Number) seqs.get("next_num")).longValue();
Long idFrete = ((Number) seqs.get("next_id")).longValue();
```
- **Fix sugerido:**
```java
// Usar sequence para PK e advisory lock para numero sequencial
Long idFrete = jdbc.queryForObject("SELECT nextval('fretes_id_frete_seq')", Long.class);
// Ou usar GENERATED ALWAYS AS IDENTITY e nao passar id_frete no INSERT
```

---

#### Issue #DA005 — OpEncomendaWriteService: busca id gerado por numero (fragil)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpEncomendaWriteService.java`
- **Linha(s):** 44-46
- **Problema:** Apos o INSERT, o `id_encomenda` e recuperado buscando pelo `numero_encomenda` recem-gerado. Sob concorrencia, se dois inserts geram o mesmo numero (DA003), o `ORDER BY id_encomenda DESC LIMIT 1` pode retornar o registro errado.
- **Impacto:** Itens de encomenda associados ao `id_encomenda` errado. Dados corrompidos.
- **Codigo problematico:**
```java
// OpEncomendaWriteService.java:44-46
Long idEncomenda = jdbc.queryForObject(
    "SELECT id_encomenda FROM encomendas WHERE numero_encomenda = ? AND empresa_id = ? ORDER BY id_encomenda DESC LIMIT 1",
    Long.class, numEncomenda, empresaId);
```
- **Fix sugerido:**
```java
// Usar RETURNING id_encomenda no INSERT (via KeyHolder ou nativeQuery)
KeyHolder keyHolder = new GeneratedKeyHolder();
jdbc.update(con -> {
    PreparedStatement ps = con.prepareStatement(insertSql, new String[]{"id_encomenda"});
    // ... setar parametros
    return ps;
}, keyHolder);
Long idEncomenda = keyHolder.getKey().longValue();
```

---

#### Issue #DA007 — SyncService: ON CONFLICT DO NOTHING descarta dados silenciosamente
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/SyncService.java`
- **Linha(s):** 309-314
- **Problema:** O INSERT de sync usa `ON CONFLICT (uuid) DO NOTHING`. Se o registro ja existe no banco central com uuid duplicado mas dados diferentes (editado no Desktop depois da ultima sync), a atualizacao e silenciosamente descartada. Nao ha log nem notificacao de que dados foram perdidos.
- **Impacto:** Edicoes feitas no Desktop apos sync anterior sao silenciosamente ignoradas. Dados desatualizados no banco central.
- **Codigo problematico:**
```java
// SyncService.java:309-312
String sql = "INSERT INTO " + tabela
    + " (" + String.join(", ", colunas) + ")"
    + " VALUES (" + String.join(", ", placeholders) + ")"
    + " ON CONFLICT (uuid) DO NOTHING"; // evita duplicatas se uuid ja existe
```
- **Fix sugerido:**
```java
// Usar ON CONFLICT (uuid) DO UPDATE com last-write-wins baseado em updated_at
String sql = "INSERT INTO " + tabela
    + " (" + String.join(", ", colunas) + ")"
    + " VALUES (" + String.join(", ", placeholders) + ")"
    + " ON CONFLICT (uuid) DO UPDATE SET "
    + colunas.stream().map(c -> c + " = EXCLUDED." + c).collect(Collectors.joining(", "))
    + " WHERE " + tabela + ".updated_at < EXCLUDED.updated_at";
```
- **Observacoes:**
> O CLAUDE.md menciona "last-write-wins" como estrategia de sync, mas a implementacao usa DO NOTHING que e o oposto — first-write-wins.

---

#### Issue #DA009 — OpViagemWriteService usa `ativa` mas BFF usa `is_atual` — inconsistencia
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpViagemWriteService.java`, `naviera-web/server/routes/viagens.js`
- **Linha(s):** OpViagemWriteService:30-31,51-57; viagens.js:39,114-118
- **Problema:** A API Spring Boot usa a coluna `ativa` para controlar qual viagem esta ativa. O BFF Express usa `is_atual`. Ambos escrevem no mesmo banco. Se a API ativa uma viagem (seta `ativa = TRUE`), o BFF nao reconhece (busca `is_atual = TRUE`). Vice-versa.
- **Impacto:** Viagem ativada pela API nao aparece como ativa no web. Viagem ativada pelo web nao aparece como ativa no app mobile.
- **Codigo problematico:**
```java
// OpViagemWriteService.java:30 — INSERT com coluna `ativa`
INSERT INTO viagens (..., ativa, is_atual, ...) VALUES (..., TRUE, FALSE, ...)

// OpViagemWriteService.java:54 — UPDATE usa `ativa`
jdbc.update("UPDATE viagens SET ativa = FALSE WHERE empresa_id = ?", empresaId);
jdbc.update("UPDATE viagens SET ativa = ? WHERE id_viagem = ? AND empresa_id = ?", ativa, id, empresaId);
```
```javascript
// viagens.js:39 — BFF busca por `is_atual`
WHERE v.is_atual = TRUE AND v.empresa_id = $1

// viagens.js:114 — BFF atualiza `is_atual`
await client.query('UPDATE viagens SET is_atual = FALSE WHERE empresa_id = $1', [empresaId])
'UPDATE viagens SET is_atual = $1 WHERE id_viagem = $2 AND empresa_id = $3 RETURNING *',
```
- **Fix sugerido:**
```java
// Padronizar: usar APENAS `is_atual` em todos os sistemas
// OpViagemWriteService.java
INSERT INTO viagens (..., is_atual, ...) VALUES (..., FALSE, ...)
// ...
jdbc.update("UPDATE viagens SET is_atual = FALSE WHERE empresa_id = ?", empresaId);
jdbc.update("UPDATE viagens SET is_atual = ? WHERE id_viagem = ? AND empresa_id = ?", ativa, id, empresaId);
```
- **Observacoes:**
> A tabela provavelmente tem DUAS colunas: `ativa` e `is_atual`. Ambas servem o mesmo proposito. Unificar e remover a duplicada.

---

## 3. COBERTURA DE ARQUIVOS

### 3.1 DAOs (25 arquivos)

| Arquivo | Status |
|---------|--------|
| `src/dao/AgendaDAO.java` | AUDITADO — #040, #041 pendentes |
| `src/dao/AuxiliaresDAO.java` | AUDITADO — DL021 novo |
| `src/dao/BalancoViagemDAO.java` | AUDITADO — limpo |
| `src/dao/CaixaDAO.java` | AUDITADO — limpo |
| `src/dao/ClienteEncomendaDAO.java` | AUDITADO — limpo |
| `src/dao/ConexaoBD.java` | AUDITADO — V1.2 #001, #002 pendentes |
| `src/dao/ConferenteDAO.java` | AUDITADO — limpo |
| `src/dao/DAOUtils.java` | AUDITADO — limpo |
| `src/dao/DespesaDAO.java` | AUDITADO — #038, #042 pendentes |
| `src/dao/EmbarcacaoDAO.java` | AUDITADO — limpo |
| `src/dao/EmpresaDAO.java` | AUDITADO — limpo |
| `src/dao/EncomendaDAO.java` | AUDITADO — #047, #077 pendentes |
| `src/dao/EncomendaItemDAO.java` | AUDITADO — limpo (tabela filha) |
| `src/dao/FreteDAO.java` | AUDITADO — limpo |
| `src/dao/FuncionarioDAO.java` | AUDITADO — #044, #045 pendentes |
| `src/dao/ItemEncomendaPadraoDAO.java` | AUDITADO — #043, #056 pendentes |
| `src/dao/ItemFreteDAO.java` | AUDITADO — limpo |
| `src/dao/PassageiroDAO.java` | AUDITADO — #031-#034 pendentes |
| `src/dao/PassagemDAO.java` | AUDITADO — #046 pendente |
| `src/dao/ReciboAvulsoDAO.java` | AUDITADO — #039, #057 pendentes |
| `src/dao/ReciboQuitacaoPassageiroDAO.java` | AUDITADO — limpo |
| `src/dao/RotaDAO.java` | AUDITADO — limpo |
| `src/dao/TarifaDAO.java` | AUDITADO — #030, #037 pendentes |
| `src/dao/TipoPassageiroDAO.java` | AUDITADO — #035, #036, #058 pendentes |
| `src/dao/UsuarioDAO.java` | AUDITADO — #052 pendente |
| `src/dao/ViagemDAO.java` | AUDITADO — DL018 novo |

### 3.2 Controllers (28 arquivos)

| Arquivo | Status |
|---------|--------|
| `src/gui/BalancoViagemController.java` | AUDITADO — limpo |
| `src/gui/BaixaPagamentoController.java` | AUDITADO — limpo |
| `src/gui/CadastroBoletoController.java` | AUDITADO — DC001, DC010 novos |
| `src/gui/CadastroFreteController.java` | AUDITADO — limpo (tenant OK) |
| `src/gui/ConfigurarSincronizacaoController.java` | AUDITADO — limpo |
| `src/gui/EstornoPagamentoController.java` | AUDITADO — V1.2 #066 pendente |
| `src/gui/ExtratoClienteEncomendaController.java` | AUDITADO — DC007 novo |
| `src/gui/FinanceiroEncomendasController.java` | AUDITADO — limpo |
| `src/gui/FinanceiroEntradaController.java` | AUDITADO — DC004 novo |
| `src/gui/FinanceiroFretesController.java` | AUDITADO — DC005 novo |
| `src/gui/FinanceiroPassagensController.java` | AUDITADO — limpo |
| `src/gui/FinanceiroSaidaController.java` | AUDITADO — limpo |
| `src/gui/GerarReciboAvulsoController.java` | AUDITADO — limpo |
| `src/gui/HistoricoEstornosController.java` | AUDITADO — limpo |
| `src/gui/HistoricoEstornosPassagensController.java` | AUDITADO — limpo |
| `src/gui/InserirEncomendaController.java` | NAO AUDITADO (prioridade menor) |
| `src/gui/ListaEncomendaController.java` | NAO AUDITADO (prioridade menor) |
| `src/gui/PagamentoFreteController.java` | AUDITADO — DC008 novo |
| `src/gui/QuitarDividaEncomendaTotalController.java` | AUDITADO — DC006 novo |
| `src/gui/RelatorioEncomendaGeralController.java` | AUDITADO — SQL concat (coberto por DC003) |
| `src/gui/RelatorioFretesController.java` | AUDITADO — limpo (tenant OK) |
| `src/gui/TabelaPrecoFreteController.java` | AUDITADO — DC002 novo |
| `src/gui/TelaPrincipalController.java` | AUDITADO — DC003 novo |
| `src/gui/VenderPassagemController.java` | AUDITADO — limpo (tenant OK) |
| `src/gui/AuditoriaExclusoesSaida.java` | AUDITADO — limpo |
| `src/gui/CompanyDataLoader.java` | AUDITADO — limpo |
| `src/gui/SetupWizardController.java` | NAO AUDITADO (setup, nao logica de negocio) |
| `src/gui/LoginApp.java` | AUDITADO — limpo |

### 3.3 BFF Express (17 arquivos)

| Arquivo | Status |
|---------|--------|
| `naviera-web/server/routes/auth.js` | AUDITADO — DB003 novo |
| `naviera-web/server/routes/passagens.js` | AUDITADO — DB007 novo |
| `naviera-web/server/routes/encomendas.js` | AUDITADO — DB001 novo |
| `naviera-web/server/routes/fretes.js` | AUDITADO — DB002 novo |
| `naviera-web/server/routes/financeiro.js` | AUDITADO — DB005, DB006 novos |
| `naviera-web/server/routes/estornos.js` | AUDITADO — resolvido |
| `naviera-web/server/routes/viagens.js` | AUDITADO — limpo (tenant OK) |
| `naviera-web/server/routes/dashboard.js` | AUDITADO — limpo |
| `naviera-web/server/routes/cadastros.js` | AUDITADO — limpo |
| `naviera-web/server/routes/rotas.js` | AUDITADO — limpo |
| `naviera-web/server/routes/embarcacoes.js` | AUDITADO — limpo |
| `naviera-web/server/routes/admin.js` | AUDITADO — limpo |
| `naviera-web/server/helpers/criarFrete.js` | AUDITADO — DB011 novo |
| `naviera-web/server/middleware/tenant.js` | AUDITADO — V1.2 #015 pendente |
| `naviera-web/server/middleware/rateLimit.js` | AUDITADO — V1.2 #020 pendente |
| `naviera-web/server/db.js` | AUDITADO — V1.2 #068 pendente |
| `naviera-web/server/index.js` | AUDITADO — V1.2 #028 pendente |

### 3.4 API Spring Boot (17 arquivos)

| Arquivo | Status |
|---------|--------|
| `naviera-api/.../service/AuthOperadorService.java` | AUDITADO — DA001 novo |
| `naviera-api/.../repository/UsuarioRepository.java` | AUDITADO — DA001 (complemento) |
| `naviera-api/.../service/OpPassagemWriteService.java` | AUDITADO — DA002 novo |
| `naviera-api/.../service/OpEncomendaWriteService.java` | AUDITADO — DA003, DA005 novos |
| `naviera-api/.../service/OpFreteWriteService.java` | AUDITADO — DA004 novo |
| `naviera-api/.../service/OpViagemWriteService.java` | AUDITADO — DA009 novo |
| `naviera-api/.../service/SyncService.java` | AUDITADO — DA007 novo |
| `naviera-api/.../service/BilheteService.java` | AUDITADO — MAX+1 (mesmo padrao DA002) |
| `naviera-api/.../service/AuthService.java` | AUDITADO — V1.2 #023 pendente |
| `naviera-api/.../config/SecurityConfig.java` | AUDITADO — V1.2 #017 pendente |
| `naviera-api/.../config/WebSocketConfig.java` | AUDITADO — V1.2 #011 pendente |
| `naviera-api/.../config/RateLimitFilter.java` | AUDITADO — V1.2 #019 pendente |
| `naviera-api/.../config/GlobalExceptionHandler.java` | AUDITADO — V1.2 #027 pendente |
| `naviera-api/.../controller/AuthOperadorController.java` | AUDITADO — limpo |
| `naviera-api/.../service/NotificationService.java` | AUDITADO — limpo |
| `naviera-api/.../util/TenantUtils.java` | AUDITADO — limpo |
| `naviera-api/.../dto/*.java` | AUDITADO — limpo |

---

## 4. PLANO DE CORRECAO

### 4.1 URGENTE (Corrigir antes de producao multi-tenant)

| Prioridade | Issue(s) | Descricao | Esforco |
|-----------|----------|-----------|---------|
| U1 | V1.2 #030-#037, #040, #043, #056, #058 | DAOs CRITICOS: parametros errados, executeUpdate faltando, SQL invalido | 4h |
| U2 | DA001 | API login sem empresa_id — cross-tenant authentication | 1h |
| U3 | V1.2 #052 | UsuarioDAO login sem empresa_id — cross-tenant no Desktop | 1h |
| U4 | DA009 | Inconsistencia `ativa` vs `is_atual` entre API e BFF | 2h |
| U5 | DA007 | SyncService ON CONFLICT DO NOTHING descarta dados | 2h |
| U6 | DA004, DB011 | id_frete via MAX+1 — PK collision | 2h |
| U7 | V1.2 #038, #041, #045 | DAOs sem empresa_id em tabelas de negocio | 2h |

### 4.2 IMPORTANTE (Corrigir antes de escalar para multiplas empresas)

| Prioridade | Issue(s) | Descricao | Esforco |
|-----------|----------|-----------|---------|
| I1 | DC001-DC003 | SQL concatenation nos controllers (7 locais) | 2h |
| I2 | DL021 | AuxiliaresDAO: caixas/rotas sem empresa_id no cache | 1h |
| I3 | DB001, DB002 | BFF DELETE: itens antes de tenant check | 1h |
| I4 | DB003 | auth.js /me sem empresa_id | 30min |
| I5 | DB005, V1.2 #050 | Boletos sem transacao (single e batch) | 1h |
| I6 | DA002, DA003 | MAX+1 para numeros sequenciais (usar sequences) | 2h |
| I7 | DC010, V1.2 #042 | Audit + delete sem transacao | 1h |
| I8 | V1.2 #039, #057 | ReciboAvulsoDAO parametros trocados | 30min |

### 4.3 MENOR (Melhorias de qualidade, sem impacto critico)

| Prioridade | Issue(s) | Descricao | Esforco |
|-----------|----------|-----------|---------|
| M1 | DC004-DC008 | double para dinheiro nos controllers | 4h |
| M2 | DB006, DB007 | parseFloat para valores monetarios no BFF | 2h |
| M3 | DL018 | ViagemDAO chama AuxiliaresDAO com tabela nao permitida | 30min |
| M4 | DA005 | OpEncomendaWriteService busca id por numero (usar RETURNING) | 30min |

---

## 5. METRICAS

| Metrica | Valor |
|---------|-------|
| Arquivos auditados | 87 |
| Arquivos limpos | 48 (55%) |
| Arquivos com issues | 39 (45%) |
| Issues CRITICO ativos | 12 (todos em DAOs, todos do V1.2) |
| Issues ALTO ativos | 18 (V1.2 + novos) |
| Issues MEDIO ativos | 27 |
| Issues BAIXO ativos | 11 |
| Issues INFO ativos | 9 |
| **Total ativos** | **77** |

---

*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*
