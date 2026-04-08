# AUDITORIA DE CODIGO — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V1.0
> **Data:** 2026-04-07
> **Auditor:** Claude Code (Dev Senior Audit)
> **Stack:** Java (JavaFX + FXML) / PostgreSQL / JasperReports / Eclipse IDE
> **Escopo:** Auditoria completa — src/, database_scripts/, resources/, configs

---

## RESUMO EXECUTIVO

| Severidade | Quantidade |
|-----------|-----------|
| CRITICO | 14 |
| ALTO | 19 |
| MEDIO | 21 |
| BAIXO | 10 |
| **TOTAL** | **64** |

**Status geral:** REPROVADO PARA PRODUCAO

Criterio: 14 issues CRITICAS = REPROVADO. Destaque para autenticacao fundamentalmente quebrada (5 pontos), valores monetarios em double no sistema financeiro inteiro, e connection leaks em operacoes recorrentes.

---

## MAPEAMENTO ESTRUTURAL

### Arvore do Projeto

```
SistemaEmbarcacaoProjeto_Novo/
├── src/
│   ├── dao/                      # Data Access Objects (28 classes)
│   ├── database/                 # Conexao alternativa ao banco
│   ├── gui/                      # Controllers JavaFX (50+ classes, 38 FXMLs)
│   │   ├── util/                 # SessaoUsuario, SyncClient, RelatorioUtil, LogService
│   │   └── icons/
│   ├── model/                    # Entidades do dominio (26 classes)
│   └── tests/                    # Testes manuais (5 classes, zero automatizados)
├── bin/                          # Classes compiladas + metadata Eclipse
├── lib/                          # 45 JARs manuais (sem Maven/Gradle)
├── database_scripts/             # 7 scripts SQL de migracao
├── resources/                    # CSS (main + dark) e 27 icones
├── relatorios/                   # Templates JasperReports
├── impressoras.config            # Config impressoras A4 + termica
└── sync_config.properties        # Config sincronizacao com servidor
```

### Pontos de Entrada

| Entrypoint | Arquivo | Descricao |
|------------|---------|-----------|
| Principal | `src/gui/Launch.java` | `main()` → `LoginApp.main()` |
| Login | `src/gui/LoginApp.java` | JavaFX → `Login.fxml` |
| Direto | `src/gui/LaunchDireto.java` | Bypass de login |

### Dependencias Principais

| Dependencia | Versao | Proposito |
|------------|--------|-----------|
| PostgreSQL JDBC | 42.7.5 | Driver banco |
| SQLite JDBC | 3.49.1.0 | Banco local |
| JasperReports | 6.21.3 | Relatorios PDF |
| Jackson | 2.15.3 | JSON/XML |
| JBCrypt | 0.4 | Hash senhas |
| Log4j | 1.2.17 | Logging legado |
| JUnit | 4.12 | Testes |

### Fluxo de Dados Principal

```
Login.fxml → LoginController → ConexaoBD → PostgreSQL → SessaoUsuario
    → TelaPrincipal → Controller → DAO → PostgreSQL
    → JasperReports → PDF
```

**Padrao:** GUI (JavaFX/FXML) → Controller → DAO → PostgreSQL (JDBC direto, sem ORM)

---

## PROBLEMAS ENCONTRADOS

### 2.1 — Bugs Criticos e Runtime

#### Issue #001 — NullPointerException em datas nullable nos DAOs
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** ReciboAvulsoDAO:71, AgendaDAO:120,142, PassagemDAO:161, ReciboQuitacaoPassageiroDAO:46, ViagemDAO:64
- **Linha(s):** Multiplas (ver acima)
- **Problema:** Chamadas `.toLocalDate()` / `.toLocalDateTime()` em `rs.getDate()` / `rs.getTimestamp()` sem null check. Se campo NULL no banco, NPE.
- **Impacto:** Crash ao carregar registros com datas nulas.
- **Codigo problematico:**
```java
// ReciboAvulsoDAO.java:71
r.setDataEmissao(rs.getDate("data_emissao").toLocalDate());
// PassagemDAO.java:161
p.setDataEmissao(rs.getDate("data_emissao").toLocalDate());
```
- **Fix sugerido:**
```java
Date dt = rs.getDate("data_emissao");
r.setDataEmissao(dt != null ? dt.toLocalDate() : null);
```
- **Observacoes:**
> _Nota: PassagemDAO protege data_nascimento (linha 192) e data_viagem (linha 194) com null check, mas esquece data_emissao. ViagemDAO:163 tambem protege data_viagem em mapResultSetToViagem mas nao em listarViagensParaComboBox. Inconsistencia indica oversight._

---

#### Issue #002 — ResultSet nao fechado em 13+ locais (code smell)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** PassagemAuxDAO, TipoPassageiroDAO, EncomendaItemDAO, EncomendaDAO, ReciboAvulsoDAO, ReciboQuitacaoPassageiroDAO, BalancoViagemDAO, AgendaDAO, PassagemDAO
- **Linha(s):** PassagemAuxDAO:46,62 | TipoPassageiroDAO:71,87 | EncomendaItemDAO:40 | BalancoViagemDAO:34,58,79,104 | AgendaDAO:66,116 | PassagemDAO:289
- **Problema:** ResultSet criado via `ps.executeQuery()` sem try-with-resources. Per spec JDBC, fechar PreparedStatement fecha seu ResultSet, entao nao ha leak real — mas e estilo inconsistente.
- **Impacto:** Baixo — code smell, nao leak real.
- **Codigo problematico:**
```java
ResultSet rs = ps.executeQuery();
if(rs.next()) { return rs.getInt("id"); }
// rs nao fechado explicitamente (mas PS fecha via try-with-resources)
```
- **Fix sugerido:**
```java
try (ResultSet rs = ps.executeQuery()) {
    if (rs.next()) { return rs.getInt("id"); }
}
```
- **Observacoes:**
> _Rebaixado de CRITICO para BAIXO na contra-verificacao. PS em try-with-resources garante fechamento cascata._

---

#### Issue #003 — Connection leak no finally de ViagemDAO.definirViagemAtiva
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 365
- **Problema:** No finally, se `setAutoCommit(true)` lancar excecao, `conn.close()` e pulado. Catch vazio engole o erro. Se conn for null, NPE tambem engolido.
- **Impacto:** Conexao vaza a cada chamada que falha. Acumula ate exaustao.
- **Codigo problematico:**
```java
finally {
    try { if(conn!=null) conn.setAutoCommit(true); conn.close(); } catch(Exception ex){}
}
```
- **Fix sugerido:**
```java
finally {
    if (conn != null) {
        try { conn.setAutoCommit(true); } catch (Exception ex) { }
        try { conn.close(); } catch (Exception ex) { }
    }
}
```
- **Observacoes:**
> _conn.close() esta fora do if-guard — apos o `;` do setAutoCommit. Bug sutil._

---

#### Issue #004 — Connection leak em 4 controllers de estorno/boleto
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** CadastroBoletoController:131,180, FinanceiroEncomendasController:296-329, FinanceiroFretesController:307-355, FinanceiroPassagensController:431-491
- **Linha(s):** Ver acima
- **Problema:** Connection aberta fora de try-with-resources. Finally faz `con.setAutoCommit(true)` mas NUNCA chama `con.close()`.
- **Impacto:** Cada estorno ou boleto vaza uma conexao. Exaure pool em horas de uso.
- **Codigo problematico:**
```java
Connection con = ConexaoBD.getConnection();
con.setAutoCommit(false);
// ... operacoes ...
} finally { con.setAutoCommit(true); }  // NUNCA chama con.close()
```
- **Fix sugerido:**
```java
try (Connection con = ConexaoBD.getConnection()) {
    con.setAutoCommit(false);
    // ... operacoes ...
    con.commit();
} // close() automatico
```
- **Observacoes:**
> _Confirmado em todos os 4 controllers. Padrao identico repetido._

---

#### Issue #005 — Connection leak em BalancoViagemDAO (campo de instancia)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 11-14
- **Problema:** DAO armazena Connection como campo. Caller responsavel por fechar — sem contrato.
- **Impacto:** Leak silencioso em cada consulta de balanco.
- **Codigo problematico:**
```java
private Connection connection;
public BalancoViagemDAO(Connection connection) {
    this.connection = connection;
}
```
- **Fix sugerido:** Cada metodo abre e fecha sua propria conexao, ou caller usa try-with-resources.
- **Observacoes:**
> _Padrao arquitetural ruim — DAO nao deveria guardar estado de conexao._

---

#### Issue #006 — Ternario retorna mesmo valor nos dois branches
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 305
- **Problema:** `(novoPago > 0.01) ? "PENDENTE" : "PENDENTE"` — bug de copy-paste. Deveria ser "PARCIAL" no branch true (confirmado comparando com FinanceiroEncomendasController:294).
- **Impacto:** Fretes parcialmente pagos ficam PENDENTE em vez de PARCIAL. Relatorios financeiros incorretos.
- **Codigo problematico:**
```java
String novoStatus = (novoPago > 0.01) ? "PENDENTE" : "PENDENTE";
```
- **Fix sugerido:**
```java
String novoStatus = (novoPago > 0.01) ? "PARCIAL" : "PENDENTE";
```
- **Observacoes:**
> _Bug claro de copy-paste. FinanceiroEncomendasController tem a versao correta._

---

#### Issue #007 — DDL (CREATE TABLE) dentro de transacao em runtime
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 312-324
- **Problema:** `CREATE TABLE IF NOT EXISTS` executado dentro de transacao de estorno. Em PostgreSQL e suportado, mas e design ruim.
- **Impacto:** Se rollback ocorrer, tabela de log tambem e removida.
- **Codigo problematico:**
```java
con.setAutoCommit(false);
String sqlCriarTabela = "CREATE TABLE IF NOT EXISTS log_estornos_fretes (...)";
stmtCriar.executeUpdate();
```
- **Fix sugerido:** Mover DDL para `database_scripts/`.
- **Observacoes:**
> _Rebaixado de ALTO para MEDIO. PostgreSQL lida com DDL transacional, e IF NOT EXISTS e no-op apos primeira vez._

---

#### Issue #009 — Unsafe cast Long em RotaDAO
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/RotaDAO.java`
- **Linha(s):** 26, 46
- **Problema:** `(Long) rs.getObject("id")` — se coluna for INTEGER, retorna Integer, cast para Long causa ClassCastException.
- **Impacto:** Crash ao carregar rotas.
- **Codigo problematico:**
```java
rota.setId((Long) rs.getObject("id"));
```
- **Fix sugerido:**
```java
rota.setId(rs.getLong("id"));
```
- **Observacoes:**
> _Mesmo padrao na linha 46. Se coluna for bigint, nao ha problema — mas e fragil._

---

#### Issue #010 — gerarProximoId retorna 0 em caso de erro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Linha(s):** 53
- **Problema:** Falha de sequence retorna 0. Caller pode inserir com ID 0.
- **Impacto:** Violacao de constraint ou sobrescrita de dados.
- **Codigo problematico:**
```java
return 0;
```
- **Fix sugerido:** Lancar excecao em vez de retornar valor sentinela.
- **Observacoes:**
> _Mesmo padrao em TipoPassageiroDAO:78 e PassagemAuxDAO:53._

---

#### Issue #011 — NullPointerException em campos BigDecimal nao inicializados
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Passagem.java`
- **Linha(s):** 29-35 vs 39-41
- **Problema:** Alguns BigDecimal inicializados com ZERO, outros ficam null. Chamadas `.compareTo()` no null causam NPE.
- **Impacto:** Crash ao exibir passagens com campos financeiros nulos.
- **Codigo problematico:**
```java
private BigDecimal valorAlimentacao;       // null
private BigDecimal valorPagamentoDinheiro = BigDecimal.ZERO;  // ok
```
- **Fix sugerido:** Inicializar todos os campos BigDecimal com `BigDecimal.ZERO`.
- **Observacoes:**
> _Inconsistencia no mesmo arquivo indica oversight._

---

#### Issue #012 — Schema SQL diverge completamente do modelo Java
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `database_scripts/criar_tabela_usuarios.sql` vs `src/model/Usuario.java` e `src/gui/LoginController.java`
- **Linha(s):** SQL inteiro
- **Problema:** SQL cria `id`, `nome`, `senha`, `permissao`. Java espera `id_usuario`, `nome_completo`, `login_usuario`, `senha_hash`, `permissoes`, `ativo`. 6 colunas divergentes.
- **Impacto:** Script cria tabela incompativel. Aplicacao falha com "column not found".
- **Codigo problematico:**
```sql
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    permissao VARCHAR(50)
);
-- Java espera: id_usuario, nome_completo, login_usuario, senha_hash, permissoes, ativo
```
- **Fix sugerido:**
```sql
CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome_completo VARCHAR(100) NOT NULL,
    login_usuario VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    senha_hash VARCHAR(255) NOT NULL,
    funcao VARCHAR(50),
    permissoes VARCHAR(50),
    ativo BOOLEAN DEFAULT true
);
```
- **Observacoes:**
> _Script provavelmente e versao antiga nunca atualizada. Banco de producao tem schema diferente._

---

#### Issue #013 — EncomendaItemDAO usa Exception para descobrir coluna
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaItemDAO.java`
- **Linha(s):** 44-48
- **Problema:** Tenta ler "id", se falha tenta "id_item". Usa excecao para flow control.
- **Impacto:** Performance degradada e mascaramento de erros reais.
- **Codigo problematico:**
```java
try {
    item.setId(rs.getLong("id")); 
} catch (SQLException e) {
    try { item.setId(rs.getLong("id_item")); } catch (SQLException ex) {}
}
```
- **Fix sugerido:** Usar nome de coluna correto baseado na query SQL. Evitar `SELECT *`.
- **Observacoes:**
> _Mesmo padrao em ReciboAvulsoDAO:63-64._

---

### 2.2 — Seguranca

#### Issue #014 — Senha do banco hardcoded em 3 arquivos
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ConexaoBD.java`:21-23, `src/database/DatabaseConnection.java`:8-10, `src/tests/TesteConexaoPostgreSQL.java`:10-12
- **Linha(s):** Ver acima
- **Problema:** Credenciais do PostgreSQL (`postgres` / `123456`) em texto plano no codigo versionado.
- **Impacto:** Qualquer pessoa com acesso ao repo tem acesso total ao banco.
- **Codigo problematico:**
```java
private static final String USUARIO = "postgres";
private static final String SENHA   = "123456";
```
- **Fix sugerido:**
```java
private static final String URL     = System.getenv("DB_URL");
private static final String USUARIO = System.getenv("DB_USER");
private static final String SENHA   = System.getenv("DB_PASS");
```
- **Observacoes:**
> _Senha fraca (123456) para usuario superuser (postgres)._

---

#### Issue #015 — Senha DIFERENTE hardcoded em CadastroClienteController
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroClienteController.java`
- **Linha(s):** 73-75
- **Problema:** Bypassa ConexaoBD, usa DriverManager direto com senha `5904` (diferente de `123456` nos outros).
- **Impacto:** Credencial adicional exposta. Indica possivel senha pessoal de dev.
- **Codigo problematico:**
```java
String pass = "5904"; // <=== AQUI ESTA SUA SENHA!
```
- **Fix sugerido:** Usar `ConexaoBD.getConnection()` centralizado.
- **Observacoes:**
> _Comentario no codigo ("AQUI ESTA SUA SENHA!") sugere que dev sabia que era problematico._

---

#### Issue #016 — Login compara texto plano contra coluna senha_hash
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/LoginController.java`
- **Linha(s):** 77-83
- **Problema:** SQL `WHERE senha_hash = ?` com senha digitada em texto plano. Se BCrypt, login nunca funciona. Se funciona, senhas estao em texto plano.
- **Impacto:** Autenticacao fundamentalmente quebrada ou insegura.
- **Codigo problematico:**
```java
String sql = "SELECT * FROM usuarios WHERE login_usuario = ? AND senha_hash = ? AND ativo = true";
stmt.setString(2, senha);  // texto plano comparado com "hash"
```
- **Fix sugerido:**
```java
String sql = "SELECT * FROM usuarios WHERE login_usuario = ? AND ativo = true";
stmt.setString(1, login);
ResultSet rs = stmt.executeQuery();
if (rs.next()) {
    String hash = rs.getString("senha_hash");
    if (BCrypt.checkpw(senha, hash)) { /* login ok */ }
}
```
- **Observacoes:**
> _JBCrypt 0.4 ja esta no classpath (lib/jbcrypt-0.4.jar). Basta usar._

---

#### Issue #017 — Fallback de senha em texto plano para estornos
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/EstornoPagamentoController.java`
- **Linha(s):** 109-117
- **Problema:** Se hash nao comeca com `$2a$`, aceita comparacao texto plano. Backdoor intencional.
- **Impacto:** Qualquer gerente com senha nao-hasheada pode ser impersonado.
- **Codigo problematico:**
```java
if(hashDoBanco.startsWith("$2a$")) {
    if(BCrypt.checkpw(senhaDigitada, hashDoBanco)) { senhaConfere = true; }
} else if (hashDoBanco != null && hashDoBanco.equals(senhaDigitada)) {
    senhaConfere = true;  // fallback texto plano
}
```
- **Fix sugerido:** Remover fallback. Forcar migracao de todas as senhas para BCrypt.
- **Observacoes:**
> _Este e o UNICO lugar que usa BCrypt corretamente (primeira branch). Padrao a seguir._

---

#### Issue #018 — Admin validation compara texto plano com senha_hash
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/AuditoriaExclusoesSaida.java`
- **Linha(s):** 250-257
- **Problema:** Mesmo padrao do #016 — SQL `WHERE senha_hash = ?` com texto plano.
- **Impacto:** Auditoria de exclusoes quebrada se senhas forem BCrypt.
- **Codigo problematico:**
```java
String sql = "SELECT count(*) FROM usuarios WHERE senha_hash = ? AND (funcao = 'Gerente' OR funcao = 'Administrador')";
stmt.setString(1, senha);  // texto plano
```
- **Fix sugerido:** Buscar hash do admin e comparar com `BCrypt.checkpw()`.
- **Observacoes:**
> _Catch block na linha 256 e vazio — erro de banco e invisivel._

---

#### Issue #019 — SQL injection via nomes de tabela/coluna concatenados
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** AuxiliaresDAO:29,89, TarifaDAO:136, SyncClient:268,335,346,378, ConfigurarSincronizacaoController:130
- **Linha(s):** Ver acima
- **Problema:** Nomes de tabela/coluna concatenados no SQL. Callers atuais usam constantes internas (nao user input).
- **Impacto:** Nao exploravel hoje, mas API insegura para uso futuro.
- **Codigo problematico:**
```java
String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
```
- **Fix sugerido:** Validar contra whitelist de tabelas/colunas permitidas.
- **Observacoes:**
> _Rebaixado de ALTO para MEDIO. Callers verificados — todos passam constantes hardcoded._

---

#### Issue #020 — LIKE wildcard injection em quitacao de divida
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 312-325
- **Problema:** Fallback com `ILIKE '%' + nome + '%'` para quitar dividas. Nome parcial pode match multiplos passageiros.
- **Impacto:** Operacao financeira afeta passageiros errados.
- **Codigo problematico:**
```java
stmt2.setString(1, "%" + nomePassageiro + "%");
rows = stmt2.executeUpdate();  // UPDATE financeiro com match fuzzy
```
- **Fix sugerido:** Usar busca por ID do passageiro, nao por nome com LIKE.
- **Observacoes:**
> _Primeiro tenta match exato, fallback e o perigoso. Remover fallback._

---

#### Issue #021 — Hash de senha logado em stderr
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Linha(s):** 29
- **Problema:** Hash (ou senha plana se nao migrada) impresso em stderr.
- **Impacto:** Logs expostos facilitam cracking.
- **Codigo problematico:**
```java
System.err.println("AVISO: ... hash em formato não-BCrypt: " + hashArmazenado);
```
- **Fix sugerido:** Remover o hash da mensagem.
- **Observacoes:**
> _Se senha for texto plano (per #017), este log expoe a senha diretamente._

---

#### Issue #022 — Token de API exibido em Alert
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 176
- **Problema:** Token mostrado em popup ao clicar botao.
- **Impacto:** Shoulder-surfing em tela compartilhada.
- **Codigo problematico:**
```java
mostrarAlerta(Alert.AlertType.INFORMATION, "Token", "Token atual: " + token);
```
- **Fix sugerido:** Mascarar token (ultimos 4 caracteres).
- **Observacoes:**
> _Rebaixado de MEDIO para BAIXO. Acao iniciada pelo proprio usuario._

---

#### Issue #023 — URL de producao em HTTP (nao HTTPS)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 84, 105, 231
- **Problema:** URL `http://sistemabarco.navdeusdealianca.com.br/api` sem TLS. Token e dados trafegam sem criptografia.
- **Impacto:** MITM intercepta credenciais e dados financeiros.
- **Codigo problematico:**
```java
"http://sistemabarco.navdeusdealianca.com.br/api"
```
- **Fix sugerido:** Usar `https://`.
- **Observacoes:**
> _Verificar se servidor suporta HTTPS antes de mudar._

---

#### Issue #024 — Token de API em texto plano em properties
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 257-258, 273-274
- **Problema:** Token e chave de nuvem salvos em texto plano em `api_config.properties`.
- **Impacto:** Qualquer acesso ao filesystem le credenciais.
- **Fix sugerido:** Criptografia ou keychain do OS.
- **Observacoes:**
> _Para app desktop, aceitavel como risco baixo se disco nao e compartilhado._

---

#### Issue #025 — Runtime.exec com path concatenado em LogService
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** 130
- **Problema:** `Runtime.exec("notepad.exe " + path)` — espacos no path quebram comando.
- **Impacto:** Funcionalidade de abrir log falha com paths com espaco. Nao e injection exploravel (path interno).
- **Codigo problematico:**
```java
Runtime.getRuntime().exec("notepad.exe " + arquivo.getAbsolutePath());
```
- **Fix sugerido:**
```java
new ProcessBuilder("notepad.exe", arquivo.getAbsolutePath()).start();
```
- **Observacoes:**
> _Rebaixado de MEDIO para BAIXO. Path vem de File interno, nao de user input._

---

#### Issue #026 — Sem .gitignore — credenciais e binarios versionados
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Raiz do projeto
- **Linha(s):** N/A
- **Problema:** Nenhum .gitignore na raiz. bin/, .metadata/, .settings/ e arquivos com credenciais no historico.
- **Impacto:** Binarios inflam repo. Credenciais no historico permanentemente.
- **Fix sugerido:** Criar `.gitignore` completo. Usar BFG para limpar historico.
- **Observacoes:**
> _Unico .gitignore e em bin/.gitignore (ignora .class apenas)._

---

### 2.3 — Logica de Negocio

#### Issue #027 — Valores monetarios em double nos modelos (6+ classes)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** Frete:15-17, ReciboAvulso:10, ReciboQuitacaoPassageiro:9, FreteItem:10-13, DadosBalancoViagem:10-13,19, ItemResumoBalanco:8, LinhaDespesaBalanco:6
- **Linha(s):** Ver acima
- **Problema:** Campos financeiros usam `double`. Aritmetica de ponto flutuante causa erros de arredondamento. Inconsistente — Passagem, Encomenda e Tarifa usam BigDecimal corretamente.
- **Impacto:** Totais divergem. Relatorios nao batem. Clientes cobrados incorretamente.
- **Codigo problematico:**
```java
// Frete.java
private double valorNominal;
private double valorDevedor;
private double valorPago;
// DadosBalancoViagem.java
private double totalPassagens = 0;
private double totalEncomendas = 0;
```
- **Fix sugerido:** Migrar todos para `BigDecimal`.
- **Observacoes:**
> _Passagem.java usa BigDecimal corretamente — padrao a seguir._

---

#### Issue #028 — Valores monetarios em double nos controllers (pervasivo)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** BaixaPagamentoController, FinanceiroEncomendasController, ExtratoClienteEncomendaController, BalancoViagemController, EstornoPagamentoController, CadastroBoletoController, GerarReciboAvulsoController, CadastroFreteController
- **Linha(s):** BaixaPagamento:27-36 | FinanceiroEncomendas:238-240 | ExtratoCliente:47,142 | Balanco:72,173 | Estorno:27,33 | Boleto:124 | Recibo:313 | Frete:564,997
- **Problema:** Controllers financeiros usam `double` para calculos monetarios.
- **Impacto:** Erros propagam model → controller → banco.
- **Fix sugerido:** Usar BigDecimal em toda a cadeia.
- **Observacoes:**
> _VenderPassagemController.java usa BigDecimal (linha 1109) — prova que equipe sabe fazer._

---

#### Issue #029 — Transacao ausente em insercao de Encomenda + Itens
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/EncomendaDAO.java` + `src/dao/EncomendaItemDAO.java`
- **Linha(s):** EncomendaDAO:14-16, EncomendaItemDAO:14-16
- **Problema:** Insert de encomenda e itens em conexoes separadas sem transacao. Encomenda orfa se itens falharem.
- **Impacto:** Dados inconsistentes no banco.
- **Fix sugerido:** Mesma Connection com transacao atomica.
- **Observacoes:**
> _EncomendaDAO.excluir() (linha 126) USA transacao corretamente — padrao a seguir._

---

#### Issue #030 — Tolerancia inconsistente para status PAGO
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** FinanceiroEncomendasController:241,270,294, ExtratoClienteEncomendaController:168,307
- **Linha(s):** Ver acima
- **Problema:** Threshold 0.01 com operadores diferentes (`>=`, `<=`, `>`) em contextos diferentes. Com double, 0.01 nao e exato.
- **Impacto:** Falta de centralizacao, mas logica matematicamente equivalente nos respectivos contextos.
- **Fix sugerido:** Centralizar constante e logica. Usar BigDecimal com `compareTo`.
- **Observacoes:**
> _Rebaixado de ALTO para MEDIO. Operadores servem propositos distintos (pagamento vs estorno vs display)._

---

#### Issue #031 — Metodos de viagem ativa sao redundantes
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 114, 140
- **Problema:** `buscarViagemAtiva` (com ORDER BY) chama `buscarViagemMarcadaComoAtual` (sem ORDER BY) como fallback. Ambas filtram `is_atual = TRUE` — se primeira nao acha, segunda tambem nao.
- **Impacto:** Dead code confuso.
- **Fix sugerido:** Remover metodo duplicado.
- **Observacoes:**
> _Rebaixado de MEDIO para BAIXO. Queries NAO sao identicas (ORDER BY diferente), mas fallback e inutil._

---

#### Issue #032 — Stubs de DAO que nao salvam no banco
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** ClienteDAO:35, ConferenteDAO:22, RemetenteDAO:22
- **Linha(s):** Ver acima
- **Problema:** Metodo `inserir()` apenas faz `System.out.println("Simulando inserção...")`. Nao persiste.
- **Impacto:** Usuario cadastra, ve sucesso, dado nao e salvo.
- **Codigo problematico:**
```java
System.out.println("Simulando inserção de Cliente: " + nome);
```
- **Fix sugerido:** Implementar INSERT real.
- **Observacoes:**
> _listarNomes() tambem retorna lista vazia com SQL comentado._

---

#### Issue #033 — CidadeDAO retorna lista hardcoded
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/CidadeDAO.java`
- **Linha(s):** 11-15
- **Problema:** Retorna lista fixa em vez de consultar banco.
- **Impacto:** Novas cidades requerem mudanca de codigo.
- **Fix sugerido:** Criar tabela `cidades` e buscar do banco.
- **Observacoes:**
> _Aceitavel para MVP se cidades sao poucas e estáveis._

---

#### Issue #034 — Encomenda armazena data como String
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Encomenda.java`
- **Linha(s):** 25
- **Problema:** `dataLancamento` e `String` enquanto outros modelos usam `LocalDate`.
- **Impacto:** Comparacoes e ordenacao de data inconsistentes.
- **Fix sugerido:** Mudar para `LocalDate`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #035 — SyncClient.processarRegistroRecebido nao implementado
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 353-358
- **Problema:** Branches de UPDATE e INSERT tem apenas comentarios — nao executam nada.
- **Impacto:** Sincronizacao recebe dados do servidor mas nunca os grava localmente.
- **Fix sugerido:** Implementar ou remover feature.
- **Observacoes:**
> _Feature de sync e fundamentalmente incompleta._

---

### 2.4 — Resiliencia e Error Handling

#### Issue #036 — 68+ catch blocks com corpo vazio em controllers
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** BalancoViagemController, FinanceiroEncomendasController, FinanceiroPassagensController, InserirEncomendaController, GerarReciboAvulsoController, GestaoFuncionariosController (e outros)
- **Linha(s):** Balanco:165,205,354,418,430,443,446 | FinEncomendas:51 | FinPassagens:54 | InserirEncomenda:400,871,1207 | ReciboAvulso:313 | GestaoFunc:184-191,527,539,551,564,576,658
- **Problema:** `catch (Exception e) {}` silencia erros em operacoes financeiras, carregamento de dados, inicializacao.
- **Impacto:** Erros de banco e logica invisíveis. Debugging em producao impossivel. Dados podem ficar inconsistentes.
- **Codigo problematico:**
```java
try { /* operacao financeira */ } catch (Exception e) { /* NADA */ }
```
- **Fix sugerido:**
```java
catch (Exception e) {
    LogService.registrarErro("Contexto", e);
    mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
}
```
- **Observacoes:**
> _Pior caso: GerarReciboAvulsoController:313 — parse de valor falha silenciosamente, gera recibo de R$0,00._

---

#### Issue #037 — DAOs engolindo exceptions com return default
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** EncomendaDAO, ViagemDAO, PassagemDAO, ReciboAvulsoDAO, EncomendaItemDAO
- **Linha(s):** EncomendaDAO:43,195-203 | ViagemDAO:174-175 | PassagemDAO:196-201
- **Problema:** `catch (SQLException) { printStackTrace(); return null; }` — caller nao distingue "sem dados" de "erro de banco".
- **Impacto:** Telas mostram listas vazias quando banco esta fora do ar.
- **Fix sugerido:** Propagar excecao ou usar Optional.
- **Observacoes:**
> _EncomendaDAO:195-203 tem catch blocks completamente vazios — pior que printStackTrace._

---

#### Issue #038 — mapResultSet com catch vazios para colunas opcionais
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** ViagemDAO:174-175, PassagemDAO:196-201, EncomendaDAO:195-203
- **Linha(s):** Ver acima
- **Problema:** `try { campo = rs.getX("col"); } catch (Exception) {}` para campos opcionais.
- **Impacto:** Dados incompletos sem indicacao de erro.
- **Fix sugerido:** ResultSetMetaData ou query explicita.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #039 — ScheduledExecutorService nao pode ser reutilizado apos shutdown
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 31, 46, 129, 144
- **Problema:** `scheduler.shutdown()` torna executor inutilizavel. Campo e `final`, nao pode ser reatribuido.
- **Impacto:** Sync automatica quebra permanentemente apos primeiro stop.
- **Codigo problematico:**
```java
private final ScheduledExecutorService scheduler;
// ...
public void pararSyncAutomatica() { scheduler.shutdown(); }
```
- **Fix sugerido:** Remover `final` e recriar executor no stop, ou usar `shutdownNow()` + recriacao.
- **Observacoes:**
> _Confirmado: iniciarSyncAutomatica() apos pararSyncAutomatica() lanca RejectedExecutionException._

---

#### Issue #040 — Log de erros sem rotacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** Classe inteira
- **Problema:** Append em `log_erros.txt` sem limite, rotacao ou cleanup.
- **Impacto:** Disco enche em producao.
- **Fix sugerido:** Rotacao (max 10MB, 5 arquivos) ou usar SLF4J/Logback ja no classpath.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #041 — Debug println em producao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 304, 314
- **Problema:** `System.out.println` de debug deixado.
- **Impacto:** Poluicao de stdout.
- **Fix sugerido:** Remover ou migrar para logger.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #042 — Rollback incompleto em EncomendaDAO.excluir
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 126-141
- **Problema:** Se primeiro DELETE falhar, catch externo nao faz rollback.
- **Impacto:** Delete parcial possivel.
- **Fix sugerido:** Rollback em qualquer falha dentro da transacao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### 2.5 — Performance

#### Issue #043 — Query N+1: 3 conexoes extras por passageiro
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 171-186
- **Problema:** `mapResultSetToPassageiro()` abre 3 conexoes via AuxiliaresDAO para cada linha. 100 passageiros = 301 conexoes.
- **Impacto:** Tela lenta. Pode exaurir conexoes do PostgreSQL.
- **Codigo problematico:**
```java
AuxiliaresDAO auxDAO = new AuxiliaresDAO();
passageiro.setTipoDoc(auxDAO.buscarNomeAuxiliarPorId(...));   // +1 conn
passageiro.setSexo(auxDAO.buscarNomeAuxiliarPorId(...));       // +1 conn
passageiro.setNacionalidade(auxDAO.buscarNomeAuxiliarPorId(...)); // +1 conn
```
- **Fix sugerido:** JOIN na query SQL principal.
- **Observacoes:**
> _Combinado com falta de connection pool (#045), impacto e severo._

---

#### Issue #044 — Query N+1: 5 conexoes extras por passagem
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 207-211
- **Problema:** 5 lookups auxiliares por passagem. 200 passagens = 1001 conexoes.
- **Impacto:** Listagem extremamente lenta.
- **Codigo problematico:**
```java
p.setAcomodacao(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_acomodacoes", ...));
p.setTipoPassagemAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_tipos_passagem", ...));
p.setAgenteAux(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_agentes", ...));
p.setFormaPagamento(auxiliaresDAO.buscarNomeAuxiliarPorId("aux_formas_pagamento", ...));
p.setCaixa(auxiliaresDAO.buscarNomeAuxiliarPorId("caixas", ...));
```
- **Fix sugerido:** JOIN na query SQL.
- **Observacoes:**
> _5 conexoes/linha e o pior N+1 do projeto._

---

#### Issue #045 — Sem connection pooling
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ConexaoBD.java`, `src/database/DatabaseConnection.java`
- **Linha(s):** ConexaoBD:39-41 | DatabaseConnection:12-18
- **Problema:** `DriverManager.getConnection()` cria conexao fisica nova a cada chamada.
- **Impacto:** Overhead TCP + auth por operacao. Combinado com N+1, centenas de conexoes/segundo.
- **Fix sugerido:** Usar HikariCP com pool de 10-20 conexoes.
- **Observacoes:**
> _HikariCP nao esta no classpath, mas SQLite JDBC e PostgreSQL JDBC estao._

---

#### Issue #046 — println em cada conexao bem-sucedida
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/database/DatabaseConnection.java`
- **Linha(s):** 18
- **Problema:** Print no stdout a cada conexao. Com N+1, centenas de prints por tela.
- **Impacto:** I/O sincronizado desacelera hot paths.
- **Fix sugerido:** Remover ou logger com nivel DEBUG.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #047 — ORDER BY 1 fragil
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java`
- **Linha(s):** 31, 47
- **Problema:** `ORDER BY 1` depende da posicao da coluna em `SELECT *`.
- **Impacto:** Mudanca de schema altera ordenacao silenciosamente.
- **Fix sugerido:** Usar nome explicito: `ORDER BY id DESC`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #048 — JSON parser customizado (250+ linhas) com Jackson no classpath
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 449-704
- **Problema:** Parser JSON feito a mao quando Jackson 2.15.3 esta disponivel.
- **Impacto:** Fragil para edge cases. Mais lento que Jackson.
- **Fix sugerido:** Substituir por `ObjectMapper`.
- **Observacoes:**
> _250 linhas de codigo que podem ser substituidas por 5 linhas com Jackson._

---

### 2.6 — Manutenibilidade

#### Issue #049 — 18 arquivos com mais de 500 linhas (God Classes)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers
- **Linha(s):** N/A
- **Problema:** Controllers massivos misturando GUI, logica, banco e relatorios.

| Linhas | Arquivo |
|--------|---------|
| 2026 | VenderPassagemController.java |
| 2002 | CadastroFreteController.java |
| 1778 | InserirEncomendaController.java |
| 1661 | RelatorioFretesController.java |
| 1345 | TelaPrincipalController.java |
| 1005 | util/RelatorioUtil.java |
| 997 | ListaEncomendaController.java |
| 911 | GestaoFuncionariosController.java |
| 891 | FinanceiroSaidaController.java |
| 803 | ExtratoPassageiroController.java |
| 785 | util/SyncClient.java |
| 722 | RelatorioEncomendaGeralController.java |
| 689 | TabelasAuxiliaresController.java |
| 661 | GerarReciboAvulsoController.java |
| 548 | FinanceiroPassagensController, CadastroConferenteController |
| 541 | ListaFretesController.java |
| 537 | TabelaPrecosEncomendaController.java |

- **Fix sugerido:** Extrair camada de servico (Service Layer).
- **Observacoes:**
> _Top 3 controllers tem mais de 1700 linhas cada._

---

#### Issue #050 — Funcoes com mais de 50 linhas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** CadastroFreteController, SyncClient, FinanceiroPassagensController
- **Linha(s):** CadastroFrete:1240-1393 (~153 linhas) | SyncClient:197-256,511-582 | FinPassagens:~400-498
- **Problema:** Metodos monoliticos.
- **Fix sugerido:** Extrair sub-metodos.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #051 — Duas classes de conexao ao banco duplicadas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ConexaoBD.java` e `src/database/DatabaseConnection.java`
- **Linha(s):** Ambos completos
- **Problema:** Duas implementacoes identicas. Configuracao ja divergiu (senha diferente em CadastroClienteController).
- **Fix sugerido:** Remover `DatabaseConnection.java` e padronizar em `ConexaoBD.java`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #052 — Classe Auxiliares.java vazia
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/model/Auxiliares.java`
- **Problema:** Corpo completamente comentado.
- **Fix sugerido:** Remover ou implementar.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #053 — ApiConfig.java com getters/setters faltando
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/ApiConfig.java`
- **Linha(s):** 19-23
- **Problema:** Campos `id`, `nomeServico`, `provider`, `endpointUrl` sem acesso.
- **Fix sugerido:** Adicionar getters/setters.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #054 — IDs com tipos inconsistentes entre modelos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplos modelos
- **Problema:** `int`, `Long`, `long` (primitivo) misturados.
- **Fix sugerido:** Padronizar em `Long` (boxed).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #055 — EncomendaItem mistura dois conceitos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/EncomendaItem.java`
- **Linha(s):** 8-22
- **Problema:** Mesma classe serve para InserirEncomenda e CadastroProduto.
- **Fix sugerido:** Separar em duas classes ou DTO.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #056 — Passagem.java com 40+ campos (God Object)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Passagem.java`
- **Linha(s):** 9-71
- **Problema:** Combina colunas do banco com campos de exibicao.
- **Fix sugerido:** Separar Model de DTO/ViewModel.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #057 — .classpath com paths absolutos do Windows
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `.classpath`
- **Linha(s):** 12-19
- **Problema:** `C:/javafx-sdk-23.0.2/lib/` hardcoded. Projeto roda em Linux.
- **Fix sugerido:** Migrar para Maven/Gradle.
- **Observacoes:**
> _Projeto nao compila sem JavaFX naquele path exato._

---

#### Issue #058 — Sem gerenciador de dependencias (45 JARs manuais)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `lib/`
- **Problema:** 45 JARs sem controle. commons-beanutils duplicado (1.9.2 E 1.9.4). Log4j 1.2.17 com CVEs.
- **Fix sugerido:** Migrar para Maven ou Gradle.
- **Observacoes:**
> _Log4j 1.2.17: CVE-2019-17571, CVE-2021-4104 (nao Log4Shell, que e 2.x)._

---

#### Issue #059 — Mistura de Swing e JavaFX
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroClienteController.java`
- **Linha(s):** 8, 94, 105
- **Problema:** Usa `JOptionPane` (Swing) enquanto tudo mais usa `Alert` (JavaFX).
- **Fix sugerido:** Substituir por Alert.
- **Observacoes:**
> _Thread safety issues ao misturar toolkits._

---

#### Issue #060 — Duplicacao de campos entre EncomendaItem e ItemEncomendaPadrao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/model/EncomendaItem.java`, `src/model/ItemEncomendaPadrao.java`
- **Problema:** 6 campos duplicados.
- **Fix sugerido:** EncomendaItem referenciar ou estender ItemEncomendaPadrao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #061 — Nenhum teste unitario real
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/tests/`
- **Problema:** 5 arquivos: 3 testes manuais de conexao, 1 lanca tela, 1 vazio. Zero automatizados.
- **Fix sugerido:** Criar testes unitarios para modelos e logica de negocio.
- **Observacoes:**
> _Nenhuma protecao contra regressao._

---

#### Issue #062 — Email duplicate check comentado
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroUsuarioController.java`
- **Linha(s):** 185
- **Problema:** Verificacao de email duplicado comentada.
- **Fix sugerido:** Descomentar ou implementar.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #063 — Sem verificacao de autorizacao na maioria dos controllers
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Todos exceto FinanceiroSaidaController
- **Problema:** Nenhuma tela verifica permissoes do usuario logado. TelaPrincipalController abre qualquer tela sem checar.
- **Fix sugerido:** Interceptor de autorizacao baseado em SessaoUsuario.getPermissoes().
- **Observacoes:**
> _Unico check (FinanceiroSaidaController:358) e apenas para excluir, e usa texto plano (ver #065)._

---

#### Issue #064 — Scripts SQL sem transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `database_scripts/001_adicionar_campos_sincronizacao.sql`
- **Problema:** Migracao altera 16 tabelas sem BEGIN/COMMIT. Falha parcial = banco inconsistente.
- **Fix sugerido:** Envolver em `BEGIN; ... COMMIT;`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #065 — Unica auth do sistema usa texto plano (encontrado na revisao)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroSaidaController.java`
- **Linha(s):** 413, 458
- **Problema:** `validarPermissaoGerente` — o unico controller com auth — compara senha digitada com `senha_hash` via SQL `=`. Mesmo padrao quebrado de #016/#018.
- **Impacto:** Unica funcao de autorizacao do sistema nao funciona com BCrypt.
- **Codigo problematico:**
```java
stmt.setString(1, senha);  // texto plano vs senha_hash
```
- **Fix sugerido:** Buscar hash e comparar com `BCrypt.checkpw()`.
- **Observacoes:**
> _Issue encontrada durante contra-verificacao. Padrao identico a #016, #018._

---

## CONTRA-VERIFICACAO

### Falsos positivos descartados

| Issue | Motivo do descarte |
|-------|-------------------|
| #008 — Divisao por zero em ExtratoClienteEncomendaController:242 | Guard `if (dividaTotalAtual <= 0) return;` na linha 218 garante divisor > 0. Linha 243 tem protecao adicional para fatorPagamento negativo. Nao ha race condition (single-thread JavaFX). |

### Severidades ajustadas

| Issue | De | Para | Motivo |
|-------|-----|------|--------|
| #002 | CRITICO | BAIXO | PS em try-with-resources fecha RS automaticamente (spec JDBC). Code smell, nao leak. |
| #007 | ALTO | MEDIO | PostgreSQL suporta DDL transacional. IF NOT EXISTS e no-op apos primeira vez. |
| #019 | ALTO | MEDIO | Callers verificados passam constantes internas, nao user input. |
| #022 | MEDIO | BAIXO | Acao iniciada pelo usuario (clica botao). Risco e shoulder-surfing. |
| #025 | MEDIO | BAIXO | Path vem de File interno. Robustez, nao injection. |
| #030 | ALTO | MEDIO | Operadores servem propositos distintos nos respectivos contextos. |
| #031 | MEDIO | BAIXO | Queries nao sao identicas (ORDER BY difere). Dead code, nao bug logico. |

### Pontos cegos declarados

1. **Arquivos FXML** — 38 .fxml nao lidos linha por linha. Podem ter bindings problematicos. Risco baixo (declarativo).
2. **Relatorios JasperReports** — Pasta `relatorios/` nao inspecionada. Templates .jrxml podem ter SQL inline.
3. **CVEs em lib/** — 45 JARs nao verificados contra bases de CVE. Log4j 1.2.17 tem CVEs conhecidas.
4. **Banco de producao** — Impossivel verificar se senhas estao em texto plano ou BCrypt sem acesso.

---

## PLANO DE CORRECAO

### Sprint 1 — Criticos (fazer AGORA)

- [ ] Issue #016 — Login: implementar BCrypt.checkpw
- [ ] Issue #017 — Remover fallback texto plano em estornos
- [ ] Issue #018 — Corrigir validacao admin AuditoriaExclusoesSaida
- [ ] Issue #065 — Corrigir validarPermissaoGerente em FinanceiroSaida
- [ ] Issue #014 — Mover credenciais BD para env vars ou properties externo
- [ ] Issue #015 — Remover senha diferente de CadastroClienteController
- [ ] Issue #006 — Corrigir ternario "PENDENTE":"PENDENTE" para "PARCIAL":"PENDENTE"
- [ ] Issue #003 — Corrigir finally em ViagemDAO.definirViagemAtiva
- [ ] Issue #004 — Adicionar con.close() em 4 controllers de estorno
- [ ] Issue #012 — Atualizar script SQL da tabela usuarios
- [ ] Issue #001 — Adicionar null checks em datas dos DAOs
- [ ] Issue #009 — Corrigir cast Long em RotaDAO
- [ ] Issue #027 — Migrar Frete, ReciboAvulso, DadosBalancoViagem para BigDecimal
- [ ] Issue #028 — Migrar controllers financeiros para BigDecimal
- **Notas:**
> _Priorizar autenticacao (#016-#018, #065) — sistema inteiro e inseguro sem isso._

### Sprint 2 — Altos (esta semana)

- [ ] Issue #026 — Criar .gitignore completo
- [ ] Issue #023 — Mudar HTTP para HTTPS na URL de producao
- [ ] Issue #020 — Remover fallback LIKE em quitacao de divida
- [ ] Issue #029 — Transacao atomica para Encomenda + Itens
- [ ] Issue #032 — Implementar INSERT real em ClienteDAO, ConferenteDAO, RemetenteDAO
- [ ] Issue #035 — Implementar ou remover sync recebimento
- [ ] Issue #036 — Substituir catch vazios por logging + alert nos controllers criticos
- [ ] Issue #037 — Propagar exceptions nos DAOs em vez de return null
- [ ] Issue #039 — Recriar scheduler apos shutdown no SyncClient
- [ ] Issue #043 — Resolver N+1 em PassageiroDAO com JOINs
- [ ] Issue #044 — Resolver N+1 em PassagemDAO com JOINs
- [ ] Issue #045 — Implementar connection pool (HikariCP)
- [ ] Issue #049 — Iniciar extracao de Service Layer nos top 3 controllers
- [ ] Issue #051 — Remover DatabaseConnection.java, padronizar ConexaoBD
- [ ] Issue #057 — Corrigir .classpath ou migrar para Maven/Gradle
- [ ] Issue #058 — Migrar para gerenciador de dependencias
- [ ] Issue #061 — Criar testes unitarios para modelos e logica financeira
- [ ] Issue #063 — Implementar autorizacao por funcao nos controllers
- [ ] Issue #005 — Refatorar BalancoViagemDAO para nao guardar Connection
- **Notas:**
> _#043 + #044 + #045 sao interdependentes — resolver juntos._

### Sprint 3 — Medios (este mes)

- [ ] Issue #007 — Mover DDL para migration scripts
- [ ] Issue #010 — Lancar excecao em gerarProximoId
- [ ] Issue #011 — Inicializar BigDecimal com ZERO em Passagem
- [ ] Issue #013 — Corrigir EncomendaItemDAO para nao usar exception para flow control
- [ ] Issue #019 — Adicionar whitelist para tabelas/colunas em SQL dinamico
- [ ] Issue #021 — Remover hash da mensagem de log
- [ ] Issue #024 — Criptografar token em properties
- [ ] Issue #030 — Centralizar logica de comparacao de status PAGO
- [ ] Issue #033 — Criar tabela cidades
- [ ] Issue #034 — Mudar Encomenda.dataLancamento para LocalDate
- [ ] Issue #038 — Substituir catch vazios em mapResultSet por ResultSetMetaData
- [ ] Issue #040 — Implementar rotacao de log
- [ ] Issue #042 — Corrigir rollback em EncomendaDAO.excluir
- [ ] Issue #048 — Substituir JSON parser manual por Jackson
- [ ] Issue #050 — Extrair sub-metodos de funcoes longas
- [ ] Issue #053 — Adicionar getters/setters em ApiConfig
- [ ] Issue #054 — Padronizar tipos de ID para Long
- [ ] Issue #055 — Separar EncomendaItem
- [ ] Issue #056 — Separar Passagem model de DTO
- [ ] Issue #059 — Substituir JOptionPane por Alert
- [ ] Issue #064 — Adicionar transacao em scripts SQL
- **Notas:**
> _#011 e fix rapido. #048 e substituicao direta por Jackson (ja no classpath)._

### Backlog — Baixos

- [ ] Issue #002 — Padronizar try-with-resources para ResultSet
- [ ] Issue #022 — Mascarar token em Alert
- [ ] Issue #025 — Usar ProcessBuilder em vez de Runtime.exec
- [ ] Issue #031 — Remover metodo duplicado buscarViagemMarcadaComoAtual
- [ ] Issue #041 — Remover println de debug
- [ ] Issue #046 — Remover println de conexao
- [ ] Issue #047 — Substituir ORDER BY 1 por nome de coluna
- [ ] Issue #052 — Remover Auxiliares.java vazia
- [ ] Issue #060 — Refatorar duplicacao EncomendaItem/ItemEncomendaPadrao
- [ ] Issue #062 — Descomentar validacao de email duplicado
- **Notas:**
> _Todos sao fixes rapidos que podem ser feitos incrementalmente._

---

## HISTORICO DE AUDITORIAS

| Versao | Data | Total | Criticos | Status |
|--------|------|-------|----------|--------|
| V1.0 | 2026-04-07 | 64 | 14 | REPROVADO |

---

## NOTAS GERAIS

> **Divida tecnica principal:** O sistema foi construido sem camada de servico — controllers acessam DAOs diretamente, misturam logica de negocio com GUI. Refatoracao profunda necessaria para qualquer evolucao sustentavel.
>
> **Autenticacao e a prioridade #1:** 5 pontos do codigo comparam texto plano com `senha_hash`. JBCrypt ja esta no classpath e um unico ponto (EstornoPagamentoController:109) mostra o uso correto. Padronizar a partir dele.
>
> **double → BigDecimal:** Migracao complexa pois afeta models, DAOs, controllers e banco. Recomendado fazer por modulo (Frete primeiro, depois Recibos, depois Balanco).
>
> **Risco de producao:** Se o sistema esta em uso com as senhas em texto plano, ha risco imediato de acesso nao autorizado. Priorizar migracao de senhas e correcao do login ANTES de qualquer outra issue.

---
*Gerado por Claude Code — Revisao humana obrigatoria*
