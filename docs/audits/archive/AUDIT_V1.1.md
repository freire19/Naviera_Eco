# AUDITORIA DE CODIGO — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V1.1
> **Data:** 2026-04-08
> **Auditor:** Claude Code (Dev Senior Audit)
> **Stack:** Java 17 + JavaFX 23.0.2 + PostgreSQL + JDBC (pool customizado)
> **Escopo:** Auditoria completa pos-merge (pool conexoes, background threads, batch queries)

---

## RESUMO EXECUTIVO

| Severidade | Quantidade |
|-----------|-----------|
| CRITICO | 6 |
| ALTO | 16 |
| MEDIO | 17 |
| BAIXO | 15 |
| **TOTAL** | **54** |

**Status geral:** REPROVADO PARA PRODUCAO

Criterio: 6 issues CRITICAS presentes (connection leaks, SQL injection, quitacao financeira insegura).

### Contexto
Esta auditoria V1.1 foca nas mudancas dos commits `377cd56` e `6a41c91` (pool de conexoes, background threads) e reavalia o estado geral. O audit V1.0 encontrou ~194 issues, das quais 140 foram corrigidas. Esta V1.1 encontra 54 issues adicionais/residuais, com foco no novo pool de conexoes e nos controllers financeiros.

---

## MAPEAMENTO ESTRUTURAL

| Metrica | Valor |
|---------|-------|
| Arquivos Java | 134 |
| Views FXML | 50 |
| DAOs | 27 (25 + ConexaoBD + PooledConnection) |
| Controllers | ~40 |
| Models | 26 |
| Testes | 4 |
| SQL scripts | 13 |
| Libs externas | 44 JARs |

**Entrypoints:** `gui.Launch` (producao), `gui.LaunchDireto` (dev)
**Banco:** PostgreSQL via pool customizado em ConexaoBD (db.properties)
**Fluxo:** FXML → Controller → DAO → ConexaoBD.getConnection() → PostgreSQL

---

## PROBLEMAS ENCONTRADOS

### 2.1 — Bugs Criticos e Runtime

#### Issue #001 — Pool de conexoes sem limite de criacao (crescimento ilimitado)
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 62-91
- **Problema:** `getConnection()` cria conexoes novas ilimitadamente quando pool vazio. Nao ha controle do total de conexoes abertas — so limita quantas ficam no pool apos devolucao. Sob carga, dezenas de conexoes simultaneas.
- **Impacto:** Exaustao de conexoes PostgreSQL (`too many connections`).
- **Codigo problematico:**
```java
Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
createdAt.put(real, System.currentTimeMillis());
return new PooledConnection(real);
```
- **Fix sugerido:**
```java
// Inicializar com capacidade: pool = new LinkedBlockingDeque<>(POOL_SIZE);
// Limitar criacao total com Semaphore:
private static final Semaphore semaphore = new Semaphore(POOL_SIZE * 3);
// Em getConnection(): semaphore.tryAcquire(timeout) antes de criar
// Em devolver/close: semaphore.release()
```
- **Observacoes:**
> _Fix original do scan usava AtomicInteger sem decrement — corrigido no review para Semaphore._

---

#### Issue #002 — PooledConnection duplicada (duas classes identicas)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ConexaoBD.java` (L129-199) + `src/dao/PooledConnection.java` (L12-86)
- **Linha(s):** Ambos arquivos inteiros
- **Problema:** Existem DUAS classes PooledConnection — inner class privada em ConexaoBD (usada) e arquivo separado (dead code).
- **Impacto:** Confusao; risco de importar a classe errada.
- **Fix sugerido:** Deletar `src/dao/PooledConnection.java`.
- **Observacoes:**
> __

---

#### Issue #003 — Race condition em pool.size() check no devolver()
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 108-112
- **Problema:** `pool.size() < POOL_SIZE` nao e atomico. N threads podem passar simultaneamente e ultrapassar limite.
- **Impacto:** Pool acumula conexoes acima do configurado.
- **Codigo problematico:**
```java
if (pool.size() < POOL_SIZE) {
    pool.offerFirst(realConn);
}
```
- **Fix sugerido:**
```java
// Usar LinkedBlockingDeque com capacidade fixa:
pool = new LinkedBlockingDeque<>(POOL_SIZE);
// devolver():
if (!pool.offerFirst(realConn)) {
    createdAt.remove(realConn);
    try { realConn.close(); } catch (SQLException ignored) {}
}
```
- **Observacoes:**
> _Adicionar `createdAt.remove()` antes do close (correcao do review)._

---

#### Issue #004 — ResultSet nao fechado em BalancoViagemDAO (4 ocorrencias)
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 40, 65, 87, 113
- **Problema:** 4 blocos de query usam `ResultSet rs = stmt.executeQuery()` SEM try-with-resources.
- **Impacto:** Vazamento de cursor no PostgreSQL.
- **Fix sugerido:** `try (ResultSet rs = stmt.executeQuery()) { ... }`
- **Observacoes:**
> __

---

#### Issue #005 — BalancoViagemDAO construtor default vaza conexao
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 20-22
- **Problema:** Construtor `BalancoViagemDAO()` pega conexao do pool e armazena no campo, mas classe nao implementa AutoCloseable. Conexao nunca devolvida.
- **Impacto:** Cada `new BalancoViagemDAO()` vaza 1 conexao permanentemente.
- **Codigo problematico:**
```java
public BalancoViagemDAO() throws java.sql.SQLException {
    this(ConexaoBD.getConnection());
}
```
- **Fix sugerido:**
```java
// Mudar para buscar conexao dentro do metodo:
public DadosBalancoViagem buscarBalancoDaViagem(int idViagem) {
    try (Connection connection = ConexaoBD.getConnection()) {
        // usar connection localmente
    }
}
```
- **Observacoes:**
> _Fix revisado: evitar armazenar conexao em campo — buscar por metodo._

---

#### Issue #006 — ResultSet nao fechado em CadastroFreteController
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** ~704
- **Problema:** `ResultSet rs = pst.executeQuery()` sem try-with-resources ao carregar frete.
- **Impacto:** Leak de recurso em operacao frequente.
- **Fix sugerido:** Envolver com `try (ResultSet rs = pst.executeQuery()) { ... }`
- **Observacoes:**
> __

---

#### Issue #007 — FileInputStream nao fechado em VenderPassagemController
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~1737-1748
- **Problema:** `new Image(new FileInputStream(f))` — stream passado ao Image mas nunca fechado.
- **Impacto:** File handle leak acumulativo.
- **Fix sugerido:** `new Image("file:" + f.getAbsolutePath())`
- **Observacoes:**
> __

---

#### Issue #008 — NPE quando recurso CSS nao encontrado
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** ~474
- **Problema:** `getClass().getResource(cssEscuro).toExternalForm()` — se recurso nao existe, NPE.
- **Impacto:** Crash ao abrir dialogo com tema escuro.
- **Fix sugerido:**
```java
java.net.URL cssUrl = getClass().getResource(cssEscuro);
if (cssUrl != null) dialogPane.getStylesheets().add(cssUrl.toExternalForm());
```
- **Observacoes:**
> __

---

#### Issue #009 — Race condition em static field (CadastroFreteController)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 78-82, 211-213
- **Problema:** `staticNumeroFreteParaAbrir` sem sincronizacao. Na pratica baixo risco (JavaFX single-threaded para UI).
- **Impacto:** Frete errado carregado em cenario improvavel.
- **Observacoes:**
> _Rebaixado de MEDIO para BAIXO no review — UI thread e sequencial._

---

#### Issue #010 — Long para int truncamento no balanco
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** ~877
- **Problema:** `viagemAtiva.getId().intValue()` trunca Long para int.
- **Impacto:** Bug latente se IDs excedem Integer.MAX_VALUE.
- **Observacoes:**
> __

---

#### Issue #011 — Excecao engolida em background thread
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 169-174
- **Problema:** `carregarDadosIniciaisComboBoxes()` falha silenciosamente na thread background. ComboBoxes ficam vazios sem explicacao.
- **Impacto:** Falha silenciosa; impossivel diagnosticar.
- **Observacoes:**
> __

---

### 2.2 — Seguranca

#### Issue #012 — SQL injection via concatenacao em FinanceiroEntradaController
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroEntradaController.java`
- **Linha(s):** 182, 189, 199
- **Problema:** 3 queries concatenam `idViagemSelecionada` diretamente no SQL em vez de `?` parametrizado.
- **Impacto:** SQL injection se source do valor mudar; viola defense-in-depth.
- **Codigo problematico:**
```java
if (idViagemSelecionada > 0) sql.append(" AND id_viagem = ").append(idViagemSelecionada);
```
- **Fix sugerido:**
```java
if (idViagemSelecionada > 0) { sql.append(" AND id_viagem = ?"); params.add(idViagemSelecionada); }
```
- **Observacoes:**
> __

---

#### Issue #013 — SQL injection via nome de tabela em SyncClient
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 272, 339
- **Problema:** `"SELECT * FROM " + tabela` sem whitelist formal. `getColunaId()` funciona como whitelist implicita mas nao valida formalmente.
- **Impacto:** SQL injection se tabela vier de fonte externa.
- **Fix sugerido:** Adicionar `Set<String> TABELAS_SYNC` com validacao antes de concatenar.
- **Observacoes:**
> _Rebaixado de CRITICO para ALTO no review — tabela vem de array hardcoded interno._

---

#### Issue #014 — Credenciais de banco hardcoded no backup
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 982-986
- **Problema:** `String usuario = "postgres"; String senha = "";` hardcoded. Codigo para ler de ConexaoBD esta comentado.
- **Impacto:** Credenciais expostas no source; backup falha se credenciais reais diferem.
- **Fix sugerido:** Ler de `db.properties` via ConexaoBD.
- **Observacoes:**
> __

---

#### Issue #015 — PGPASSWORD exposta em environment de processo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 1023-1025
- **Problema:** `pb.environment().put("PGPASSWORD", senha)`. Senha atualmente vazia.
- **Impacto:** Baixo — senha vazia e sistema desktop local.
- **Observacoes:**
> _Rebaixado de MEDIO para BAIXO — senha vazia, desktop local._

---

#### Issue #016 — SyncClient serverUrl padrao HTTP
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 26
- **Problema:** Default `http://localhost:8080` sem TLS. Configuravel via properties.
- **Impacto:** Dados interceptaveis se usado em producao sem alterar.
- **Observacoes:**
> _Rebaixado de ALTO para MEDIO — default localhost (desenvolvimento)._

---

#### Issue #017 — SyncClient parsing JSON manual sem validacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~515-586
- **Problema:** JSON parseado com indexOf/substring manual. Sem validacao de schema. Jackson ja esta no classpath.
- **Impacto:** Crash ou injecao de dados maliciosos via resposta do servidor.
- **Fix sugerido:** Usar `com.fasterxml.jackson.databind.ObjectMapper` (jackson-databind-2.15.3.jar).
- **Observacoes:**
> __

---

#### Issue #018 — Sem validacao de input em campos de cadastro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~660-665
- **Problema:** Dados de TextFields setados nos models sem validacao de comprimento/caracteres.
- **Impacto:** Dados inconsistentes; potencial XSS se exibidos no naviera-app web.
- **Observacoes:**
> __

---

#### Issue #019 — Falta auth/permissao em CadastroFreteController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** Inteiro
- **Problema:** Controller nao verifica permissoes antes de CRUD de fretes.
- **Impacto:** Qualquer usuario com acesso a UI pode operar fretes.
- **Observacoes:**
> __

---

### 2.3 — Logica de Negocio

#### Issue #020 — Quitacao por LIKE pode quitar passagens de outro passageiro
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 362-377
- **Problema:** Fallback usa `ILIKE '%nome%'` — pode afetar passageiros com nome parcial igual.
- **Impacto:** Dinheiro marcado como pago para passageiros errados; prejuizo financeiro.
- **Codigo problematico:**
```java
stmt2.setString(1, "%" + nomePassageiro + "%");
rows = stmt2.executeUpdate();
```
- **Fix sugerido:** Remover fallback LIKE. Metodo deveria receber `idPassageiro` (Long) em vez de nome.
- **Observacoes:**
> _Fix revisado no review: usar ID em vez de nome para operacoes financeiras._

---

#### Issue #021 — BalancoViagemDAO usa double para valores financeiros
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 48, 70, 92, 116
- **Problema:** `rs.getDouble("total")` e `double valor` para somas financeiras.
- **Impacto:** Erros de arredondamento acumulativos no balanco.
- **Fix sugerido:** Usar `rs.getBigDecimal("total")` e BigDecimal em toda a cadeia.
- **Observacoes:**
> __

---

#### Issue #023 — Viagem no passado impede atualizacao de campos nao-data
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 323-328
- **Problema:** `atualizar()` recusa viagens com data passada, impedindo editar descricao/embarcacao/rota.
- **Impacto:** Impossivel corrigir erros em viagens ja realizadas.
- **Fix sugerido:** Validar data somente se ela mudou (comparar com valor atual no banco).
- **Observacoes:**
> __

---

#### Issue #024 — SessaoUsuario.touch() nunca chamado automaticamente
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SessaoUsuario.java`
- **Linha(s):** 30-31
- **Problema:** `touch()` existe mas nenhum codigo o chama. Sessao expira 8h apos login, independente de atividade.
- **Impacto:** Usuarios ativos deslogados sem aviso.
- **Fix sugerido:** Chamar `touch()` em operacoes que mudam estado (DAO inserts/updates/deletes).
- **Observacoes:**
> _Fix revisado: nao chamar em getUsuarioLogado (renovaria com checks passivos)._

---

#### Issue #025 — Status "PENDENTE" vs "PENDENTE_PAGAMENTO" inconsistente
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java` + `src/gui/CadastroFreteController.java`
- **Linha(s):** VenderPassagem:682/780, CadastroFrete:~1529
- **Problema:** Status strings diferentes entre modulos. Sem enum centralizado para fretes.
- **Impacto:** Queries com filtro de status podem nao encontrar registros.
- **Observacoes:**
> __

---

#### Issue #026 — Validacao silenciosa falha em salvarOuAlterarFrete
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** ~1648-1661
- **Problema:** Parsing de quantidade/preco falha silenciosamente no catch — retorna sem alert.
- **Impacto:** Usuario pensa que salvou item mas nao salvou.
- **Observacoes:**
> __

---

#### Issue #027 — listarViagensParaComboBox sem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 51-86
- **Problema:** Retorna TODAS as viagens sem LIMIT.
- **Impacto:** Lentidao progressiva com anos de dados.
- **Observacoes:**
> __

---

### 2.4 — Resiliencia e Error Handling

#### Issue #028 — FinanceiroEncomendasController connection leak
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroEncomendasController.java`
- **Linha(s):** 328-362
- **Problema:** `Connection con = ConexaoBD.getConnection()` sem try-with-resources. Finally seta autoCommit mas NAO fecha conexao.
- **Impacto:** Cada operacao de estorno vaza 1 conexao.
- **Codigo problematico:**
```java
Connection con = null;
try {
    con = ConexaoBD.getConnection();
    con.setAutoCommit(false);
    // ...
} finally {
    if(con != null) con.setAutoCommit(true);
    // FALTA: con.close();
}
```
- **Fix sugerido:** `try (Connection con = ConexaoBD.getConnection()) { ... }`
- **Observacoes:**
> __

---

#### Issue #029 — ViagemDAO.definirViagemAtiva NPE + leak no finally
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 414-416
- **Problema:** `conn.close()` fora do `if(conn!=null)` — NPE mascarado por catch. Se setAutoCommit falha, close nao executa.
- **Impacto:** NPE silencioso + connection leak.
- **Codigo problematico:**
```java
try { if(conn!=null) conn.setAutoCommit(true); conn.close(); } catch(Exception ex){}
```
- **Fix sugerido:**
```java
if (conn != null) {
    try { conn.setAutoCommit(true); } catch (Exception ex) {}
    try { conn.close(); } catch (Exception ex) {}
}
```
- **Observacoes:**
> __

---

#### Issue #030 — Background tasks sem catch geral (VenderPassagemController)
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~257-351
- **Problema:** Thread background sem catch. Excecao trava UI (ComboBoxes vazios, btnNovo desabilitado).
- **Impacto:** Sistema inutilizavel se banco lento na inicializacao.
- **Observacoes:**
> __

---

#### Issue #031 — Swallowed exceptions em DAOs (sem stack trace)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplos DAOs (FreteDAO, CaixaDAO, EmbarcacaoDAO)
- **Problema:** `System.err.println(e.getMessage())` sem stack trace.
- **Impacto:** Impossivel diagnosticar erros em producao.
- **Observacoes:**
> __

---

#### Issue #032 — Scheduler SyncClient sem awaitTermination
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~147-150
- **Problema:** `shutdown()` sem esperar conclusao. Sync interrompido no meio.
- **Impacto:** Dados parcialmente sincronizados.
- **Observacoes:**
> __

---

#### Issue #033 — SyncClient sem retry em falhas de rede
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~155-196
- **Problema:** Falha em tabela N impede sync de tabelas N+1..M. Sem retry/backoff.
- **Impacto:** Sincronizacao incompleta.
- **Observacoes:**
> __

---

#### Issue #034 — ExtratoPassageiroController race condition
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** ~111-122
- **Problema:** Thread carrega dados empresa, mas UI pode referenciar campos antes de concluir.
- **Impacto:** NPE intermitente ao abrir extrato.
- **Observacoes:**
> __

---

#### Issue #035 — CadastroBoletoController connection leak
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 141, 186
- **Problema:** `Connection con = ConexaoBD.getConnection()` na L141. Finally na L186 faz `con.setAutoCommit(true)` mas NUNCA `con.close()`.
- **Impacto:** Cada geracao de boleto vaza 1 conexao do pool.
- **Codigo problematico:**
```java
Connection con = ConexaoBD.getConnection();
con.setAutoCommit(false);
// ...
} finally { con.setAutoCommit(true); }  // sem close!
```
- **Fix sugerido:** `try (Connection con = ConexaoBD.getConnection()) { ... }`
- **Observacoes:**
> _Promovida de ALTO para CRITICO no review — confirmado no codigo._

---

#### Issue #036 — LogService sem rotacao de log
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** 32-35
- **Problema:** `log_erros.txt` cresce indefinidamente.
- **Impacto:** Disco cheio apos meses/anos.
- **Observacoes:**
> __

---

### 2.5 — Performance

#### Issue #037 — Autocomplete filtra lista inteira a cada keystroke
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~861-937
- **Problema:** Reconstroi ObservableList inteira a cada caractere sem debounce.
- **Impacto:** Lag perceptivel com milhares de passageiros.
- **Fix sugerido:** `FilteredList` + `PauseTransition(200ms)`.
- **Observacoes:**
> __

---

#### Issue #038 — ContextMenu reconstruido a cada keystroke
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** ~1004-1012
- **Problema:** Menu items recriados a cada caractere digitado.
- **Impacto:** Memory churn e overhead de repainting.
- **Observacoes:**
> __

---

#### Issue #039 — listarTodasViagensResumido sem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 249-271
- **Problema:** Retorna TODAS as viagens sem LIMIT.
- **Impacto:** Lentidao progressiva; OOM em cenarios extremos.
- **Observacoes:**
> __

---

#### Issue #040 — NumberFormat criado dentro de metodo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** ~454
- **Problema:** `NumberFormat.getCurrencyInstance()` recriado em metodo chamado frequentemente.
- **Impacto:** Overhead de locale lookup repetido.
- **Observacoes:**
> __

---

#### Issue #041 — N+1 query em impressao de bilhete
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~1624-1635
- **Problema:** Query separada para passageiro por bilhete impresso.
- **Impacto:** Lentidao em lote.
- **Observacoes:**
> __

---

#### Issue #042 — ObservableList recriada em filtro de tabela
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** ~1481
- **Problema:** Copia inteira da lista a cada filtro. Deveria usar FilteredList.
- **Impacto:** Overhead de copia desnecessaria.
- **Observacoes:**
> __

---

#### Issue #043 — Sort completo apos cada insercao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** ~1167-1169
- **Problema:** `FXCollections.sort()` apos adicionar 1 item.
- **Impacto:** Imperceptivel para listas pequenas.
- **Observacoes:**
> __

---

### 2.6 — Manutenibilidade

#### Issue #044 — VenderPassagemController com 2170 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 1-2170
- **Problema:** 4.3x acima do limite de 500 linhas.
- **Fix sugerido:** Extrair `BilhetePrintService`, `TarifaCalculationService`, `PassagemFilterService`.
- **Observacoes:**
> __

---

#### Issue #045 — CadastroFreteController com 2239 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1-2239
- **Problema:** Arquivo mais longo do projeto. Mistura CRUD, OCR, XML, autocomplete, impressao.
- **Fix sugerido:** Extrair `FreteAIService`, `FreteTableManager`, `FretePrintService`.
- **Observacoes:**
> __

---

#### Issue #046 — InserirEncomendaController com 1798 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** 1-1798
- **Problema:** 1798 linhas com CRUD + impressao + autocomplete + financeiro.
- **Observacoes:**
> __

---

#### Issue #047 — TelaPrincipalController com 1428 linhas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 1-1428
- **Problema:** Hub com backup, agenda, lembretes, navegacao, sync tudo junto.
- **Observacoes:**
> __

---

#### Issue #048 — Metodos com mais de 100 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers
- **Problema:** 5 metodos entre 115 e 270 linhas. Impossiveis de testar unitariamente.
- **Observacoes:**
> __

---

#### Issue #049 — 3 stubs nao implementados
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1886-1906
- **Problema:** Botoes na UI que mostram "Funcionalidade Pendente".
- **Observacoes:**
> __

---

#### Issue #050 — AutoCompleteComboBoxListener e stub vazio
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/AutoCompleteComboBoxListener.java`
- **Linha(s):** 1-26
- **Problema:** Classe inteira e stub sem funcionalidade.
- **Observacoes:**
> __

---

#### Issue #051 — Padrao de error handling inconsistente
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Todo o projeto
- **Problema:** 4 padroes misturados: printStackTrace, System.err, LogService, AlertHelper.
- **Fix sugerido:** Padronizar em `LogService.registrarErro()`.
- **Observacoes:**
> __

---

#### Issue #052 — Magic strings de status (fretes)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Problema:** Strings "PENDENTE", cores "#2e7d32", sufixos hardcoded. StatusPagamento enum nao usado em fretes.
- **Observacoes:**
> __

---

#### Issue #053 — BalancoViagemDAO/model usa double para dinheiro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/BalancoViagemDAO.java` + `src/model/DadosBalancoViagem.java`
- **Problema:** Toda cadeia BalancoViagem usa double enquanto resto migrou para BigDecimal.
- **Observacoes:**
> __

---

#### Issue #054 — Duplicate error logging em DAOs
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** CaixaDAO, FreteDAO, UsuarioDAO, etc.
- **Problema:** Duas mensagens identicas por catch block.
- **Observacoes:**
> __

---

#### Issue #055 — Formatacao de moeda inconsistente
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** TelaPrincipalController, InserirEncomendaController
- **Problema:** 3+ formatos diferentes de moeda espalhados.
- **Observacoes:**
> __

---

## CONTRA-VERIFICACAO

### Falsos positivos descartados
| Issue | Motivo |
|-------|--------|
| #022 (Calculo edicao quantidade) | Codigo real mostra `setQuantidade(newValue)` ANTES de `setValorTotal()`. Ordem correta. |

### Severidades ajustadas
| Issue | De | Para | Motivo |
|-------|-----|------|--------|
| #013 | CRITICO | ALTO | tabela vem de array hardcoded; whitelist implicita via switch |
| #015 | MEDIO | BAIXO | Senha vazia; desktop local |
| #009 | MEDIO | BAIXO | JavaFX single-threaded para UI |
| #016 | ALTO | MEDIO | Default localhost (desenvolvimento) |
| #035 | ALTO | CRITICO | Connection leak confirmado — nunca chama close() |

### Pontos cegos declarados
| Area | Motivo |
|------|--------|
| 12 controllers nao lidos | AuditoriaExclusoesSaida, GestaoFuncionarios, CadastroTarifa, CadastroProduto, NotaFretePersonalizada, ExtratoClienteEncomenda, RelatorioEncomendaGeral, TabelaPrecosEncomenda, TabelaPrecoFrete, EstornoPagamento, QuitarDividaEncomendaTotal, ConfigurarApi |
| Testes (src/tests/) | Nao auditados — cobertura provavelmente baixa |
| SQL scripts | Verificados indiretamente via DAOs |
| 50 FXML views | Nao verificados |

---

## PLANO DE CORRECAO

### Sprint 1 — Criticos (fazer AGORA)
- [ ] Issue #001 — Pool sem limite de criacao (ConexaoBD)
- [ ] Issue #005 — BalancoViagemDAO construtor vaza conexao
- [ ] Issue #012 — SQL injection FinanceiroEntradaController
- [ ] Issue #020 — Quitacao LIKE afeta passageiros errados
- [ ] Issue #028 — Connection leak FinanceiroEncomendasController
- [ ] Issue #035 — Connection leak CadastroBoletoController
- **Notas:**
> _Prioridade maxima: #028 e #035 sao fixes de 1 linha (adicionar con.close()). #005 e #012 requerem refatoracao leve. #020 e #001 requerem mudanca de logica._

### Sprint 2 — Altos (esta semana)
- [ ] Issue #003 — Race condition pool.size()
- [ ] Issue #004 — ResultSet nao fechado BalancoViagemDAO
- [ ] Issue #006 — ResultSet nao fechado CadastroFreteController
- [ ] Issue #007 — FileInputStream leak VenderPassagemController
- [ ] Issue #008 — NPE CSS TelaPrincipalController
- [ ] Issue #013 — SQL injection SyncClient (whitelist)
- [ ] Issue #014 — Credenciais hardcoded backup
- [ ] Issue #017 — JSON parsing manual SyncClient
- [ ] Issue #021 — double para dinheiro BalancoViagemDAO
- [ ] Issue #023 — Viagem passado bloqueia update
- [ ] Issue #024 — SessaoUsuario.touch() nunca chamado
- [ ] Issue #029 — NPE finally ViagemDAO
- [ ] Issue #030 — Background sem catch VenderPassagem
- [ ] Issue #034 — Race condition ExtratoPassageiro
- [ ] Issue #037 — Autocomplete sem debounce
- [ ] Issue #044 — VenderPassagemController 2170 linhas
- **Notas:**
> _16 issues ALTAS. Agrupar: connection handling (#003,#004,#006,#029), security (#013,#014,#017), controllers grandes (#044,#045,#046)._

### Sprint 3 — Medios (este mes)
- [ ] Issue #002, #011, #016, #018, #019, #025, #026, #027
- [ ] Issue #031, #032, #033, #038, #039, #041
- [ ] Issue #047, #048, #051, #052, #053
- **Notas:**
> _17 issues MEDIAS. Melhorias incrementais de qualidade._

### Backlog — Baixos
- [ ] Issue #009, #010, #015, #036, #040, #042, #043
- [ ] Issue #045, #046, #049, #050, #054, #055
- **Notas:**
> _15 issues BAIXAS. Limpeza geral e polimento._

---

## HISTORICO DE AUDITORIAS

| Versao | Data | Total | Criticos | Status |
|--------|------|-------|----------|--------|
| V1.0 | 2026-04-07 | ~194 | 14 | REPROVADO → 140 fixados |
| V1.1 | 2026-04-08 | 54 | 6 | REPROVADO PARA PRODUCAO |

---

## NOTAS GERAIS
> - Esta V1.1 foca nas mudancas recentes (pool de conexoes) e controllers financeiros nao cobertos em profundidade na V1.0.
> - O pool customizado em ConexaoBD.java e uma melhoria significativa sobre o modelo anterior (sem pool), mas tem bugs de concorrencia (#001, #003) que precisam de atencao.
> - O padrao de connection leak (#028, #035) se repete em controllers que usam `setAutoCommit(false)` manualmente sem try-with-resources. Buscar e corrigir em TODOS os controllers.
> - 12 controllers nao foram auditados — recomenda-se rodar deep audit neles.
> - Cobertura de testes e minima (4 classes). Recomenda-se adicionar testes para DAOs criticos antes de corrigir issues.

---
*Gerado por Claude Code — Revisao humana obrigatoria*
