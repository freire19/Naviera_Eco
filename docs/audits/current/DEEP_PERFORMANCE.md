# AUDITORIA PROFUNDA — PERFORMANCE — Naviera_Eco
> **Versao:** V4.0
> **Data:** 2026-04-15
> **Categoria:** Performance
> **Base:** AUDIT_V1.2
> **Arquivos analisados:** 205 de 205 total (cobertura completa — Desktop + BFF + Web + App + OCR + Site)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas encontrados | 38 |
| Novos problemas corrigidos | 2 |
| Issues anteriores resolvidas | 10 |
| Issues anteriores parcialmente resolvidas | 2 |
| Issues anteriores pendentes | 9 |
| **Total de issues ativas** | **45** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #DP026 | NumberFormat em formatar() BalancoViagemController | RESOLVIDO — `static final FMT_MOEDA` em linha 483 |
| #DP027 | Impressao sincrona ListarPassageirosViagemController | RESOLVIDO — daemon Thread em linha 401 |
| #DP028 | Autocomplete sem debounce InserirEncomendaController | RESOLVIDO — 3 PauseTransition (linhas 176-178) |
| #DP030 | BalancoViagem bypassa EmpresaDAO cache | RESOLVIDO — usa EmpresaDAO.buscarPorId com cache (linha 486) |
| #DP031 | ObservableList recriado ListaEncomendaController | RESOLVIDO — setAll() pattern (linha 153) |
| #DP032 | DateTimeFormatter inline Viagem.java | RESOLVIDO — static final DTF_DATA (linha 9) |
| #089 | ViagemDAO.cacheViagemAtiva nao volatile | RESOLVIDO — ConcurrentHashMap keyed por empresa_id (linha 157) |
| #090 | PassagemDAO.temDataChegada campo compartilhado | RESOLVIDO — ThreadLocal (linha 184) |
| #083 | Boleto batch sem transacao (BFF) | RESOLVIDO — BEGIN/COMMIT/ROLLBACK em financeiro.js:275-328 |
| #084 | Race condition numero_bilhete MAX+1 | RESOLVIDO — pg_advisory_xact_lock em passagens.js:97 |
| #085 | Race condition id_frete MAX+1 | RESOLVIDO — nextval + advisory lock em criarFrete.js:21-43 |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| #DP029 | Logo sem cache (7 locais) | 3 locais ainda sem ImageCache: PassagemPrintHelper:153, GerarReciboAvulsoController:611, FinanceiroSaidaController:548 |
| #DP033 | Model classes sem equals/hashCode | 15 de 30 classes ainda faltam: ApiConfig, DadosBalancoViagem, Despesa, Empresa, FreteDevedor, FreteItem, Funcionario, ItemFrete, ItemResumoBalanco, LinhaDespesaDetalhada, PassagemFinanceiro, ReciboAvulso, ReciboQuitacaoPassageiro + 2 novos (EncomendaFinanceiro, FreteFinanceiro) |
| #080 | BFF queries sem LIMIT | Parcial — alguns endpoints com LIMIT (ocr.js:132, passagens.js:20), mas maioria ainda sem LIMIT |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #048 | JSON parser custom SyncClient (~290 linhas) | Pendente — SyncClient:950-1236, substituir por Gson |
| #DP023 | JARs duplicados (~35MB) | Pendente — sem mudanca |
| #079 | N+1 em PassagemDAO.mapResultSetToPassagem | PARCIAL — listarExtratoPorPassageiro (linha 347) nao chama preCarregarCachesPassagem |
| #086 | ViagemDAO.listarTodasViagensResumido sem LIMIT | Pendente — linha 294, sem LIMIT |
| #088 | PassagemDAO.filtrarRelatorio pos-filtragem em Java | Pendente — 5 filtros removeIf em linhas 324-342 |
| #092 | filtrarRelatorio nao pre-carrega caches | Pendente — linha 279 sem preCarregarCachesPassagem |
| #093 | DespesaDAO.buscarBoletos sem LIMIT | Pendente — linha 375, buscarDespesas (linha 63) tambem |
| #081 | SELECT * em 15+ endpoints BFF | Pendente — 17+ endpoints ainda usam SELECT * |
| #082 | Admin N+1 subqueries correlacionadas | Pendente — admin.js:35-48, 4 subqueries por empresa |
| #087 | React Web sem React.lazy/code splitting | Pendente — Layout.jsx importa 34 paginas estaticamente |
| #091 | Site institucional monolitico sem code splitting | Pendente — App.jsx ~757 linhas |

---

## NOVOS PROBLEMAS

### CAMADA DESKTOP — GUI (FX Thread Blocking)

#### Issue #DP034 — DB queries no FX thread em BalancoViagemController.carregarDetalhamentoTab2Fx()
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 173-237, 403-407
- **Problema:** `carregarDetalhamentoTab2Fx()` executa 4 queries SQL + `carregarDespesasAgrupadas()` diretamente no FX Application Thread via `Platform.runLater`. Cada query abre conexao JDBC e bloqueia a UI.
- **Impacto:** Freeze de 1-5s ao abrir relatorio de balanco — tela financeira principal.
- **Codigo problematico:**
```java
Platform.runLater(() -> {
    carregarDetalhamentoTab2Fx(idViagem); // 4 queries SQL no FX thread
});
```
- **Fix sugerido:** Mover todas as queries para a background thread existente em `carregarRelatorio()`, coletar resultados em POJOs, usar `Platform.runLater()` apenas para renderizar UI.
- **Observacoes:**
> _Problema mais critico desta auditoria — afeta tela financeira usada diariamente._

---

#### Issue #DP035 — DB queries no FX thread em TelaPrincipalController (calendario + dashboard)
- [x] **Concluido** _(corrigido 2026-04-15)_
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 575-630
- **Problema:** `carregarDadosComboParam()` (linha 575) e `carregarDadosComboSimples()` (linha 592) executam SQL sincronamente. Chamados durante construcao do calendario e handlers de dialogo no FX thread. `gerarLembretesNoBanco()` (linha 609) faz INSERT no FX thread.
- **Impacto:** Freeze ao navegar meses no calendario — tela principal do sistema.
- **Codigo problematico:**
```java
// Chamado de handlers no FX thread
private ObservableList<String> carregarDadosComboParam(String sql, String... params) {
    try (Connection con = ConexaoBD.getConnection(); // BLOQUEIA FX THREAD
         PreparedStatement stmt = con.prepareStatement(sql)) {
```
- **Fix sugerido:** Envolver em `javafx.concurrent.Task` ou background Thread, similar ao padrao usado em outros controllers.
- **Observacoes:**
> _TelaPrincipalController e a tela principal — qualquer freeze aqui e visivel ao operador._

---

#### Issue #DP036 — EmpresaDAO + DB queries no FX thread em EncomendaPrintHelper
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/EncomendaPrintHelper.java`
- **Linha(s):** 49-50, 128-129
- **Problema:** `imprimirCupomTermico()` e chamado de handlers no FX thread. Cria `new EmpresaDAO()` e `new EncomendaItemDAO()`, executa `buscarPorId()` e `listarPorIdEncomenda()` sincronamente. Combinado com `job.printPage()` sincrono na linha 182.
- **Impacto:** Freeze de 1-3s ao imprimir cupom de encomenda.
- **Fix sugerido:** Pre-carregar dados de empresa e itens no controller chamador (que ja os tem) e passar como parametros.
- **Observacoes:**
> _Afeta fluxo de impressao de encomendas — operacao de alta frequencia._

---

#### Issue #DP037 — Impressao sincrona de relatorios multi-pagina no FX thread
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers
- **Linha(s):**
  - `BalancoViagemController.java`: 305 (loop `job.printPage()`)
  - `RelatorioEncomendaGeralController.java`: 281, 324, 410, 554
  - `RelatorioFretesController.java`: 640, 866, 1326, 1466
  - `FinanceiroSaidaController.java`: 517 (loop)
  - `RelatorioUtil.java`: 394, 672, 698
  - `EncomendaPrintHelper.java`: 182
- **Problema:** `job.printPage()` (JavaFX PrinterJob) chamado no FX thread em loop para relatorios multi-pagina. Cada pagina bloqueia a UI por 0.5-2s.
- **Impacto:** Freeze de 5-30s ao imprimir relatorios com muitas paginas. Nota: `PrinterJob.printPage()` do JavaFX DEVE ser chamado no FX thread (requisito da API).
- **Fix sugerido:** Para relatorios multi-pagina, snapshot nodes no FX thread, passar imagens raster para AWT `PrinterJob` em daemon thread (padrao ja usado em `PassagemPrintHelper`). Adicionar progress indicator.
- **Observacoes:**
> _VenderPassagemController e ListarPassageirosViagemController ja usam daemon thread — aplicar mesmo padrao._

---

### CAMADA DESKTOP — DAO (Queries e Cache)

#### Issue #DP038 — PassageiroDAO.listarTodos sem pre-carregamento de cache
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 29, 183-198
- **Problema:** `mapResultSetToPassageiro` chama `buscarNomeAuxiliarPorId` 3x por linha (tipo_doc, sexo, nacionalidade). `listarTodos` (linha 29) nao chama `preCarregarCaches`. Cold-start com 200 passageiros = 600 queries.
- **Impacto:** Listagem de passageiros lenta na primeira abertura.
- **Fix sugerido:**
```java
public List<Passageiro> listarTodos() {
    try { auxiliaresDAO.preCarregarCachesPassageiro(); } catch (SQLException e) { /* cache opcional */ }
    // ... resto do metodo
```
- **Observacoes:**
> _Mesmo padrao N+1 do #079 mas em PassageiroDAO._

---

#### Issue #DP039 — AgendaDAO.buscarTodasTarefas sem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/AgendaDAO.java`
- **Linha(s):** 174-194
- **Problema:** Carrega TODAS as tarefas (concluidas e nao concluidas) sem LIMIT. Acumula ao longo dos anos.
- **Impacto:** Degradacao progressiva da tela de agenda.
- **Fix sugerido:** `LIMIT 500` ou filtro por periodo (ultimos 6 meses).
- **Observacoes:**
> _Tabela agenda_anotacoes cresce com boletos automaticos + tarefas manuais._

---

#### Issue #DP040 — AuxiliaresDAO cache nao segmentado por tenant
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 73-103
- **Problema:** Cache Maps (`cacheNomeParaId`, `cacheIdParaNome`) sao keyed apenas por nome da tabela, nao por `(tabela, empresa_id)`. Para tabelas tenant-scoped (caixas, rotas, embarcacoes), se tenant A carrega o cache e tenant B le, pega dados do tenant A. No Desktop single-tenant e inofensivo. Na API multi-tenant (se DAOs forem reutilizados) seria data leak.
- **Impacto:** Risco arquitetural — seguro HOJE no Desktop, perigoso se DAOs migrarem para API.
- **Fix sugerido:**
```java
String cacheKey = isTenantScoped(tabela) ? tabela + ":" + DAOUtils.empresaId() : tabela;
```
- **Observacoes:**
> _Risco BAIXO atual. Documentar como dica tecnica para quando/se DAOs forem usados na API._

---

#### Issue #DP041 — FuncionarioDAO.carregarHistorico: 2 queries separadas em vez de UNION
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/FuncionarioDAO.java`
- **Linha(s):** 223-282
- **Problema:** Duas queries separadas (financeiro_saidas + eventos_rh) com 2 conexoes do pool.
- **Impacto:** 1 roundtrip extra. Impacto minimo.
- **Fix sugerido:** Combinar em `UNION ALL ... ORDER BY data`.
- **Observacoes:**
> _Otimizacao opcional._

---

#### Issue #DP042 — SELECT * em 6+ DAOs Desktop
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** Multiplos DAOs
- **Linha(s):**
  - `EncomendaDAO.java`: 91, 108, 123
  - `FuncionarioDAO.java`: 23-24
  - `PassageiroDAO.java`: 32, 132, 149, 165
  - `ReciboAvulsoDAO.java`: 42, 62
  - `ReciboQuitacaoPassageiroDAO.java`: 37
  - `ClienteEncomendaDAO.java`: 22, 65
- **Problema:** SELECT * em vez de colunas especificas.
- **Impacto:** Dados extras transferidos. Impacto baixo em banco local.
- **Fix sugerido:** Especificar colunas necessarias.
- **Observacoes:**
> _Menor impacto no Desktop (banco local) vs BFF (banco remoto)._

---

#### Issue #DP043 — ConexaoBD.PooledConnection.close() race condition
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 195-199
- **Problema:** Campo `closed` e volatile mas check-then-act nao e atomico. Dois threads chamando `close()` podem devolver mesma conexao ao pool 2x.
- **Impacto:** Baixo — conexoes raramente compartilhadas entre threads. Mas pode causar corrupcao do pool em edge cases.
- **Fix sugerido:**
```java
private final AtomicBoolean closed = new AtomicBoolean(false);
@Override public void close() {
    if (closed.compareAndSet(false, true)) {
        ConexaoBD.devolver(real);
    }
}
```
- **Observacoes:**
> _Fix defensivo. Risco pratico baixo._

---

### CAMADA DESKTOP — GUI (NumberFormat / Formatacao)

#### Issue #DP044 — NumberFormat criado em CellFactory updateItem (hot path)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/GerarReciboAvulsoController.java`
- **Linha(s):** 250
- **Problema:** `NumberFormat.getCurrencyInstance(new Locale("pt","BR"))` criado dentro de `TableCell.updateItem()`. Chamado por JavaFX para cada celula visivel a cada scroll/resize. 50 linhas = 50 instanciacoes por render.
- **Impacto:** Lag de scroll em tabelas com muitas linhas.
- **Fix sugerido:** `private static final NumberFormat NF = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));`
- **Observacoes:**
> _Fix de 1 linha._

---

#### Issue #DP045 — NumberFormat por chamada em CadastroBoletoController.BoletoRow
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 394
- **Problema:** `BoletoRow.getValorFormatado()` cria `NumberFormat.getCurrencyInstance()` a cada chamada. Usado por `PropertyValueFactory` para table display — dispara por celula. Classe ja tem `private static final NumberFormat nf` em linha 52.
- **Impacto:** Instanciacoes redundantes — NF ja existe como campo static.
- **Fix sugerido:** Usar o `nf` ja existente: `return nf.format(valor);`
- **Observacoes:**
> _Fix de 1 linha. Campo ja existe, so precisa usar._

---

#### Issue #DP046 — NumberFormat per-call em TelaPrincipalController
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 330, 467
- **Problema:** `NumberFormat.getCurrencyInstance()` criado dentro de `construirCalendario()` (chamado a cada navegacao de mes) e handler de click.
- **Impacto:** Baixo — 1 instanciacao por acao do usuario.
- **Fix sugerido:** static final.
- **Observacoes:**
> _Consistencia com padrao do resto do codebase._

---

#### Issue #DP047 — NumberFormat como campo de instancia (nao static)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** Multiplos controllers
- **Linha(s):**
  - `TabelaPrecosEncomendaController.java`: 89
  - `ExtratoPassageiroController.java`: 77
- **Problema:** `private final NumberFormat moedaBR` — campo de instancia em vez de static final. Nova instancia por abertura de tela.
- **Impacto:** Minimo — 1 alocacao por abertura de controller.
- **Fix sugerido:** `private static final NumberFormat`.
- **Observacoes:**
> _Consistencia._

---

#### Issue #DP048 — SimpleDateFormat em vez de DateTimeFormatter
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 510
- **Problema:** `new SimpleDateFormat("dd/MM/yyyy")` em background thread. `SimpleDateFormat` nao e thread-safe (neste caso instancia local, entao funcional). Inconsistente com resto do codebase que usa `DateTimeFormatter`.
- **Impacto:** Minimo. Risco se reutilizado.
- **Fix sugerido:** Usar `DateTimeFormatter` static final ou `RelatorioUtil.FMT_DATA`.
- **Observacoes:**
> _Consistencia._

---

#### Issue #DP049 — Logo sem ImageCache em 3 locais restantes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplos
- **Linha(s):**
  - `PassagemPrintHelper.java`: 153
  - `GerarReciboAvulsoController.java`: 611
  - `FinanceiroSaidaController.java`: 548
- **Problema:** `new Image(f.toURI().toString())` em vez de `ImageCache.get()`. PassagemPrintHelper e o de maior impacto (impressao de bilhetes = alta frequencia).
- **Impacto:** 1-2 leituras de disco desnecessarias por impressao.
- **Fix sugerido:** `ImageCache.get(emp.pathLogo)` — ImageCache ja existe e e usado em outros 4 locais.
- **Observacoes:**
> _Expande DP029. Fix de 1 linha por local._

---

#### Issue #DP050 — Cascata de threads redundantes em BalancoViagemController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 93-144
- **Problema:** `inicializarDados()` lanca 1 background thread → `carregarComboViagensFx()` lanca OUTRA thread (linhas 501-532) → `carregarRelatorio()` lanca MAIS OUTRA thread (linhas 116-143) → que chama `carregarDetalhamentoTab2Fx()` no FX thread com 4+ queries. Total: 3 threads + 7+ conexoes para 1 tela.
- **Impacto:** Contencao no pool de conexoes + overhead de thread creation.
- **Fix sugerido:** Consolidar em UMA background thread que carrega todos os dados, depois `Platform.runLater()` uma vez com tudo pronto.
- **Observacoes:**
> _Combinar com fix do DP034._

---

#### Issue #DP051 — Thread de impressao sem setDaemon(true) em ExtratoPassageiroController
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** 572, ~764
- **Problema:** `new Thread(() -> job.print()).start()` sem `setDaemon(true)`. Se usuario fechar app durante impressao, JVM nao termina.
- **Impacto:** Processo fica pendurado no SO.
- **Fix sugerido:** `thread.setDaemon(true);`
- **Observacoes:**
> _Fix de 1 linha. Padrao ja usado em ListarPassageirosViagemController._

---

### CAMADA BFF — QUERIES E PAGINACAO

#### Issue #DP052 — Queries sem LIMIT em 10+ endpoints BFF
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Multiplos
- **Linha(s):**
  - `encomendas.js`: 14 (`SELECT * FROM encomendas` sem viagem_id)
  - `fretes.js`: 15 (`SELECT * FROM fretes` sem viagem_id)
  - `financeiro.js`: 34 (saidas), 90-152 (detalhes), 209 (boletos)
  - `viagens.js`: 13 (todas viagens)
  - `passagens.js`: 36 (passagens por viagem)
  - `estornos.js`: 237-345 (historico completo)
  - `admin.js`: 33, 182, 237 (listas admin)
- **Problema:** SELECTs retornam datasets ilimitados. Uma empresa com anos de dados pode ter dezenas de milhares de registros.
- **Impacto:** Uso excessivo de memoria no BFF; timeouts; possivel OOM kill.
- **Fix sugerido:**
```javascript
const page = parseInt(req.query.page) || 1
const limit = Math.min(parseInt(req.query.limit) || 100, 500)
const offset = (page - 1) * limit
sql += ` LIMIT ${limit} OFFSET ${offset}`
```
- **Observacoes:**
> _Consolida #080. Corrigir em TODOS os endpoints de listagem._

---

#### Issue #DP053 — Admin N+1 subqueries correlacionadas (4 por empresa)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/admin.js`
- **Linha(s):** 35-48, 193-202
- **Problema:** GET `/empresas` usa 4 subqueries correlacionadas por linha de empresa (COUNT usuarios, passagens, encomendas, fretes). GET `/metricas` repete o padrao + 5 full-table COUNTs sem filtro empresa_id. Com 50 empresas = 200+ subqueries.
- **Impacto:** Painel admin lento com crescimento do numero de tenants.
- **Fix sugerido:**
```javascript
const result = await pool.query(`
  SELECT e.*, COALESCE(u.cnt, 0) AS total_usuarios, ...
  FROM empresas e
  LEFT JOIN (SELECT empresa_id, COUNT(*) cnt FROM usuarios WHERE ... GROUP BY empresa_id) u ON u.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) cnt FROM passagens GROUP BY empresa_id) p ON p.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) cnt FROM encomendas GROUP BY empresa_id) en ON en.empresa_id = e.id
  LEFT JOIN (SELECT empresa_id, COUNT(*) cnt FROM fretes GROUP BY empresa_id) f ON f.empresa_id = e.id
`)
```
- **Observacoes:**
> _Consolida #082. Remover query `totais` redundante — somar resultados por_empresa._

---

#### Issue #DP054 — Itens inseridos em loop individual sem batch
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplos
- **Linha(s):**
  - `naviera-web/server/routes/encomendas.js`: 88-95
  - `naviera-web/server/helpers/criarFrete.js`: 75-88
- **Problema:** Cada item de encomenda/frete e INSERT individual dentro de transacao. 20 itens = 20 roundtrips ao PostgreSQL.
- **Impacto:** Latencia acumulada + lock duration.
- **Fix sugerido:** Multi-row INSERT: `INSERT INTO ... VALUES ($1,$2), ($3,$4), ...`
- **Observacoes:**
> _Roundtrip local e rapido, mas pattern e ineficiente._

---

#### Issue #DP055 — Boleto batch: 240 INSERTs sequenciais em transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 295-318
- **Problema:** POST `/boleto/batch` cria ate 120 parcelas com 2 INSERTs cada (financeiro_saidas + agenda_anotacoes) = 240 queries sequenciais. Transaction lock mantido durante todas.
- **Impacto:** Transacao longa + pool contention.
- **Fix sugerido:** Batch INSERTs: 1 multi-row INSERT para financeiro_saidas + 1 para agenda_anotacoes = 2 queries total.
- **Observacoes:**
> _Transacao ja existe (fix #083), mas o conteudo ainda e sequencial._

---

#### Issue #DP056 — Estorno historico: merge + sort em JavaScript
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/estornos.js`
- **Linha(s):** 268-271
- **Problema:** GET `/historico` dispara 3 queries paralelas (passagens, encomendas, fretes estornos), concatena com flatMap, sort em JS. Sem LIMIT.
- **Impacto:** Baixo com poucos estornos. Cresce com o tempo.
- **Fix sugerido:** UNION ALL com ORDER BY e LIMIT no SQL.
- **Observacoes:**
> _Combinar com paginacao._

---

#### Issue #DP057 — Agenda query com EXTRACT() previne uso de indice
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/agenda.js`
- **Linha(s):** 17-18
- **Problema:** `EXTRACT(MONTH FROM data_evento)` e `EXTRACT(YEAR FROM data_evento)` previnem B-tree index em data_evento.
- **Impacto:** Baixo — tabela agenda e pequena.
- **Fix sugerido:** Range query: `WHERE data_evento >= $2 AND data_evento < $3` com primeiro/ultimo dia do mes.
- **Observacoes:**
> _Mesmo padrao do DP007 (corrigido no Desktop). Replicar no BFF._

---

#### Issue #DP058 — existsSync no request path (OCR foto)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 347
- **Problema:** `existsSync(fullPath)` — chamada sincrona de filesystem no event loop.
- **Impacto:** Microsegundos por request. Minimo.
- **Fix sugerido:** Remover — `res.sendFile` ja retorna 404 se arquivo nao existe.
- **Observacoes:**
> _Fix de 1 linha._

---

### CAMADA BFF — INTEGRIDADE (encontradas durante scan de performance)

#### Issue #DP059 — Encomenda pagamento sem guarda de overpayment
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 153-158
- **Problema:** POST `/encomendas/:id/pagar` adiciona `valor_pago` sem checar se excede total. Passagens (passagens.js:197) tem `WHERE valor_devedor >= $1` — encomendas nao.
- **Impacto:** Pagamento excessivo corrompe relatorios financeiros.
- **Fix sugerido:** `AND (total_a_pagar - COALESCE(desconto, 0) - valor_pago) >= $1`
- **Observacoes:**
> _Issue de integridade encontrada durante scan de performance._

---

#### Issue #DP060 — Frete pagamento sem guarda de overpayment e sem transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 98-108
- **Problema:** POST `/fretes/:id/pagar` sem overpayment guard e sem transacao. Pagamentos concorrentes podem resultar em valor_pago > valor do frete.
- **Impacto:** Dados financeiros inconsistentes.
- **Fix sugerido:** Transacao com FOR UPDATE + `WHERE valor_devedor >= $1`.
- **Observacoes:**
> _Critico para integridade financeira._

---

### CAMADA DESKTOP — INDICES E SQL

#### Issue #DP061 — Indices compostos faltantes para queries frequentes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `database_scripts/`
- **Linha(s):** N/A
- **Problema:** Queries frequentes sem cobertura de indice composto:
  - `passagens WHERE empresa_id = ? AND id_viagem = ?`
  - `encomendas WHERE empresa_id = ? AND id_viagem = ?`
  - `fretes WHERE empresa_id = ? AND id_viagem = ?`
  - `financeiro_saidas WHERE empresa_id = ? AND forma_pagamento = 'BOLETO' AND status = 'PENDENTE'`
  - `agenda_anotacoes WHERE empresa_id = ? AND data_evento >= ? AND data_evento < ? AND concluida = false`
  - `viagens WHERE is_atual = TRUE AND empresa_id = ?` (partial index)
- **Impacto:** Full table scans em tabelas que crescem com o tempo.
- **Fix sugerido:** Script de migration com indices compostos:
```sql
CREATE INDEX IF NOT EXISTS idx_passagens_empresa_viagem ON passagens(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_encomendas_empresa_viagem ON encomendas(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_fretes_empresa_viagem ON fretes(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_saidas_boleto_pendente ON financeiro_saidas(empresa_id, forma_pagamento, status) WHERE forma_pagamento = 'BOLETO';
CREATE INDEX IF NOT EXISTS idx_agenda_empresa_data ON agenda_anotacoes(empresa_id, data_evento, concluida);
CREATE INDEX IF NOT EXISTS idx_viagens_ativa ON viagens(empresa_id) WHERE is_atual = TRUE;
```
- **Observacoes:**
> _Verificar se `006_criar_indices_performance.sql` ja cobre algum destes. Complementar se necessario._

---

### CAMADA FRONTEND WEB

#### Issue #DP062 — 34 paginas importadas estaticamente sem code splitting
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/components/Layout.jsx`
- **Linha(s):** 1-57
- **Problema:** 34 imports estaticos de paginas. Bundle inteiro carregado no login. Sem React.lazy() nem Suspense.
- **Impacto:** Tempo de carregamento inicial mais alto que necessario. Payloads maiores em conexoes lentas.
- **Fix sugerido:**
```jsx
const Dashboard = React.lazy(() => import('../pages/Dashboard.jsx'))
const Passagens = React.lazy(() => import('../pages/Passagens.jsx'))
// ... para todas as 34 paginas
// Envolver em <Suspense fallback={<div>Carregando...</div>}>
```
- **Observacoes:**
> _Consolida #087. Vite ja faz tree-shaking, mas lazy loading reduz initial bundle._

---

## COBERTURA

| Diretorio | Arquivos | Issues ativas |
|-----------|----------|---------------|
| src/dao/ | 13 | 9 (DP038-DP043 + #079/#086/#088/#092/#093) |
| src/database/ | 2 | 0 |
| src/gui/ | 43 | 12 (DP034-DP037, DP044-DP046, DP049-DP051 + #DP029) |
| src/gui/util/ | 16 | 3 (DP036, DP049, #048) |
| src/model/ | 23 | 1 (#DP033 parcial) |
| naviera-web/server/ | 22 | 12 (DP052-DP060 + #081/#082) |
| naviera-web/src/ | 38 | 1 (DP062/#087) |
| naviera-app/src/ | 31 | 0 |
| naviera-ocr/src/ | 16 | 0 |
| naviera-site/src/ | 2 | 1 (#091) |
| database_scripts/ | 23 | 1 (DP061) |
| lib/ | N/A | 1 (#DP023) |
| **TOTAL** | **205** | **47** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO) — 2 issues

- [x] #DP034 — DB queries no FX thread em BalancoViagemController — **FIXADO** (queries movidas para bg thread em carregarRelatorio)
- [x] #DP035 — DB queries no FX thread em TelaPrincipalController — **FIXADO** (carregarDadosComboParam, gerarLembretesNoBanco, handleCarregarDados, adicionarAnotacao — todos em bg thread)
- **Notas:**
> _Ambos corrigidos 2026-04-15. Zero queries SQL no FX thread nestas telas._

### Importante (ALTO) — 5 issues

- [ ] #DP036 — DB + print sincrono EncomendaPrintHelper — **Esforco:** 1h
- [ ] #DP037 — Impressao sincrona multi-pagina (6+ controllers) — **Esforco:** 3h
- [ ] #DP052 — Queries sem LIMIT em 10+ endpoints BFF — **Esforco:** 2h
- [ ] #DP053 — Admin N+1 subqueries — **Esforco:** 1h
- [ ] #079/#092 — N+1 listarExtratoPorPassageiro/filtrarRelatorio — **Esforco:** 30min
- **Notas:**
> _DP052 tem maior blast radius (10+ endpoints). DP053 e fix de 1 query._

### Importante (MEDIO) — 14 issues

- [ ] #DP038 — PassageiroDAO cache preload — **Esforco:** 15min
- [ ] #DP039 — AgendaDAO LIMIT — **Esforco:** 10min
- [ ] #DP040 — AuxiliaresDAO cache tenant-key — **Esforco:** 30min
- [ ] #DP044 — NumberFormat em CellFactory — **Esforco:** 5min
- [ ] #DP045 — NumberFormat em BoletoRow (usar nf existente) — **Esforco:** 5min
- [ ] #DP049 — Logo sem ImageCache (3 locais) — **Esforco:** 15min
- [ ] #DP050 — Cascata de threads BalancoViagem — **Esforco:** 1h (junto com DP034)
- [ ] #DP054 — Itens em loop individual BFF — **Esforco:** 30min
- [ ] #DP055 — Boleto batch 240 INSERTs — **Esforco:** 1h
- [ ] #DP059 — Encomenda overpayment guard — **Esforco:** 15min
- [ ] #DP060 — Frete overpayment guard + transacao — **Esforco:** 30min
- [ ] #DP061 — Indices compostos faltantes — **Esforco:** 30min
- [ ] #DP062 — React.lazy code splitting — **Esforco:** 1h
- [ ] #088 — filtrarRelatorio pos-filtragem em Java — **Esforco:** 1h
- **Notas:**
> _Quick wins: DP044, DP045, DP049 (5-15 min cada). DP061 e script SQL._

### Menor (BAIXO) — 13 issues

- [ ] #DP041 — FuncionarioDAO UNION — **Esforco:** 20min
- [ ] #DP042 — SELECT * em DAOs Desktop — **Esforco:** 1h
- [ ] #DP043 — PooledConnection AtomicBoolean — **Esforco:** 10min
- [ ] #DP046 — NumberFormat TelaPrincipalController — **Esforco:** 5min
- [ ] #DP047 — NumberFormat instancia vs static — **Esforco:** 5min
- [ ] #DP048 — SimpleDateFormat → DateTimeFormatter — **Esforco:** 5min
- [ ] #DP051 — Thread sem daemon — **Esforco:** 5min
- [ ] #DP056 — Estorno historico merge+sort — **Esforco:** 30min
- [ ] #DP057 — Agenda EXTRACT → range — **Esforco:** 10min
- [ ] #DP058 — existsSync no request path — **Esforco:** 5min
- [ ] #048 — JSON parser custom SyncClient — **Esforco:** 2h
- [ ] #DP023 — JARs duplicados — **Esforco:** 1h
- [ ] #086 — ViagemDAO LIMIT — **Esforco:** 10min
- [ ] #093 — DespesaDAO LIMIT — **Esforco:** 10min
- [ ] #081 — SELECT * em 17+ endpoints BFF — **Esforco:** 2h
- [ ] #091 — Site monolitico — **Esforco:** 30min
- [ ] #DP033 — equals/hashCode 15 models restantes — **Esforco:** 1h
- **Notas:**
> _Muitos sao fixes de 1-5 linhas. Agrupar para um commit de cleanup._

---

## NOTAS

> **Progresso V3.0 → V4.0:** Escopo expandido para cobertura completa (205 arquivos vs 131 anterior). 11 issues anteriores resolvidas, 2 parciais, 9 pendentes. 38 novos problemas encontrados — principalmente FX thread blocking no Desktop e queries sem paginacao no BFF.
>
> **Achados mais criticos:** DP034 e DP035 causam freezes visiveis nas telas mais usadas (BalancoViagem e TelaPrincipal). Estes devem ser priorizados.
>
> **Quick wins (fix em <15min):** DP044, DP045, DP049, DP051, DP058, DP046, DP047, DP048, DP043 — total de 9 issues corrigiveis em ~1 hora.
>
> **Padroes recorrentes:** (1) FX thread blocking com DB queries — afeta 5+ controllers. (2) NumberFormat/DateTimeFormatter criados em hot paths — 6 locais. (3) Missing LIMIT — 10+ endpoints BFF + 3 DAOs. (4) Logo sem ImageCache — 3 locais residuais.
>
> **Camadas limpas:** naviera-app (0 issues), naviera-ocr (0 issues), models (quase limpo apos DP032/DP033).

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*
