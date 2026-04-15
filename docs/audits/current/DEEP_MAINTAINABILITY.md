# AUDITORIA PROFUNDA — MAINTAINABILITY — Naviera Eco
> **Versao:** V4.0
> **Data:** 2026-04-15
> **Categoria:** Maintainability
> **Base:** AUDIT_V1.2 + DEEP_MAINTAINABILITY V3.0
> **Arquivos analisados:** 155 de 155 total (Desktop + Web + App + API + OCR + Site)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 15 |
| Issues anteriores resolvidas (V3->V4) | 0 |
| Issues anteriores parcialmente resolvidas | 1 |
| Issues anteriores pendentes | 14 |
| **Total de issues ativas** | **29** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| _(nenhuma nova resolucao nesta versao)_ | | |

*Nota: Todas as 27 issues fixadas no V3.0 (DM031-DM055 + DM001/DM002/etc.) continuam resolvidas. Nao houve regressoes.*

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| #DM032 | Inner classes em controllers | V3.0 estimou ~6 restantes. Contagem real: **19 inner classes** em 14 controllers (FreteItemCadastro, FreteCompleto, ItemFrete, FreteItemRelatorio, Boleto, ValorExtensoUtil, EstornoLog, ResultadoQueryPassagens, ItemExtrato x2, ItemNota, LogEstornoPassagem, Rota, Conferente, EstornoFreteLog, DadosEmpresa, ViagemFiltroWrapper, FreteView, TableCellMoney, PrecoItem) |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #049 | 8+ controllers > 500 linhas | Confirmado: CadastroFrete 2256L, VenderPassagem 1822L, RelatorioFretes 1778L, InserirEncomenda 1717L, TelaPrincipal 1499L, ListaEncomenda 979L, GestaoFuncionarios 759L, Financeiro.jsx 692L, GerarRecibo 665L, Passagens.jsx 581L |
| #050 | Funcoes > 50 linhas | Confirmado: configurarTabela ~292L, configurarAutoCompleteComboBox ~270L, imprimirRelatorioTermico ~229L, imprimirResumidoPorRemetente ~207L, salvarOuAlterarFrete ~195L, imprimirExtratoCliente ~195L, handleBackup ~175L, configurarAutoCompleteClienteGoogleStyle ~144L, configurarComboBoxItem ~130L |
| #056 | Passagem 48 campos | 273 linhas, 71 campos. God model confirmado |
| #058 | Sem gerenciador dependencias | Sem Maven/Gradle; 44 JARs em lib/ |
| #061 | Zero testes unitarios | TesteConexao e TesteConexaoPostgreSQL sao main(), nao @Test. SessaoUsuarioTest e StatusPagamentoTest sao reais mas cobrem apenas 2 classes |
| #DM004 | Print layout inline | 10+ controllers constroem layouts VBox/HBox inline. RelatorioFretes tem 31+ VBox/HBox instances. PrintLayoutHelper existe mas cobre apenas headers |
| #DM007 | SQL inline em controllers | CadastroFrete(40 refs), RelatorioFretes(40), CadastroBoleto(28), TelaPrincipal(15), VenderPassagem(4), GerarRecibo(4), InserirEncomenda(2). Total: ~133 SQL refs em controllers |
| #094 | CadastroFreteController 2256L | God class monolitico — UI + SQL + calculo + impressao + OCR |
| #095 | VenderPassagemController 1822L | Mesmo padrao — UI + SQL + calculo |
| #096 | RelatorioFretesController 1778L | 4 metodos de impressao (229L + 207L + 195L + 148L) |
| #097 | InserirEncomendaController 1717L | configurarTabela com 292L de cell factories inline |
| #098 | Financeiro.jsx 692L monolitico | 5 tabs, 2 modals, 14 useState, 28 inline styles |
| #100 | Sem Error Boundaries (naviera-web) | Confirmado: zero ErrorBoundary no naviera-web. naviera-app tem |
| #103 | cadastros.js 30+ endpoints repetitivos | 521 linhas, mesmo padrao CRUD copy-paste 30x |

---

## NOVOS PROBLEMAS

### Arquitetura / Cross-Cutting

#### Issue #DM056 — Dependencia circular: DAO/model importam gui.util.AppLogger
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** Todos os DAOs (20 arquivos) + `model/StatusPagamento.java:5`
- **Problema:** Camada model e DAO importam `gui.util.AppLogger` — violacao de layering. Models e DAOs nao devem depender de classes GUI. Impossibilita reutilizar DAOs em contexto nao-JavaFX (API, testes headless).
- **Impacto:** Acoplamento estrutural; impossivel extrair DAOs para modulo independente.
- **Codigo problematico:**
```java
// model/StatusPagamento.java:5
import gui.util.AppLogger;

// dao/PassagemDAO.java (e todos os outros DAOs)
import gui.util.AppLogger;
```
- **Fix sugerido:** Mover AppLogger para pacote `util/` (sem prefixo gui) ou usar `java.util.logging.Logger` nos DAOs/models.
- **Observacoes:**
> _21 arquivos afetados. Fix mecanico (rename de pacote) mas com alto impacto estrutural._

---

#### Issue #DM057 — Sem camada de servico: controllers gerenciam transacoes diretamente
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** CadastroFreteController:1499-1680, InserirEncomendaController, VenderPassagemController, CadastroBoletoController
- **Problema:** Controllers fazem `setAutoCommit(false)`, `commit()`, `rollback()` diretamente. Logica de negocio (calculo de pagamento, status, sequencias) misturada com UI. Nao existe camada Service entre Controller e DAO.
- **Impacto:** Impossivel reutilizar logica de negocio (Web BFF reimplementa tudo); impossivel testar sem JavaFX; duplicacao Desktop vs Web.
- **Codigo problematico:**
```java
// CadastroFreteController.java ~L1500
Connection con = ConexaoBD.getConnection();
con.setAutoCommit(false);
try {
    // 180 linhas de SQL + calculo + validacao
    con.commit();
} catch (Exception e) {
    con.rollback();
}
```
- **Fix sugerido:** Criar FreteService.salvarOuAlterarComItens(), EncomendaService, PassagemService. Controllers delegam para services.
- **Observacoes:**
> _Issue estrutural mais importante. Resolve tambem #DM007 (SQL inline) e #101 (duplicacao Desktop vs Web)._

---

#### Issue #DM058 — 3 padroes diferentes de API client (web/app/ocr)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/api.js`, `naviera-app/src/api.js`, `naviera-ocr/src/api.js`
- **Problema:** Cada camada reimplementa API client com padrao diferente: web usa `api.get/post()`, app usa `useApi()` hook + `authFetch()`, ocr usa funcoes `apiGet/Post/Put()`. Logica de 401 (token expirado) duplicada 3x.
- **Impacto:** Bug fix em auth handling precisa ser replicado em 3 arquivos.
- **Fix sugerido:** Criar pacote compartilhado `@naviera/api-client` ou copiar mesma implementacao de referencia.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### DAOs

#### Issue #DM059 — TipoPassageiroDAO.listarNomes anti-pattern (N+1)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/TipoPassageiroDAO.java:64-70`
- **Problema:** `listarNomes()` chama `listarTodos()` que faz SELECT de todos os campos, depois extrai apenas o nome em loop. Query desnecessariamente pesada.
- **Impacto:** Performance e legibilidade — metodo faz 2x o trabalho necessario.
- **Codigo problematico:**
```java
public List<String> listarNomes() {
    List<String> nomes = new ArrayList<>();
    for (TipoPassageiro tp : listarTodos()) {
        nomes.add(tp.getNome());
    }
    return nomes;
}
```
- **Fix sugerido:** `SELECT nome FROM tipo_passageiro WHERE empresa_id = ? ORDER BY nome` direto.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM060 — ConferenteDAO.listarTodos retorna List\<long[]\> com elemento morto
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ConferenteDAO.java:21-36`
- **Problema:** Retorna `List<long[]>` onde cada array e `{id, 0}` — segundo elemento sempre zero, nunca usado. API confusa e sem tipagem.
- **Impacto:** Callers precisam saber que index 0 e id e index 1 e lixo.
- **Fix sugerido:** Retornar `List<Conferente>` ou `Map<Long, String>` com id→nome.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM061 — DespesaDAO.buscarDespesas e buscarBoletos duplicam builder de WHERE
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/DespesaDAO.java:26-93, 356-402`
- **Problema:** Ambos metodos constroem WHERE clause com logica identica (StringBuilder + params list + condicional append). Pattern repetido sem helper.
- **Impacto:** Mudanca no filtro exige editar 2 metodos.
- **Fix sugerido:** Extrair `private void buildWhereClause(StringBuilder sql, List<Object> params, ...)`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM062 — Naming inconsistente entre DAOs (buscar/listar/obter)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Todos os DAOs
- **Problema:** 3 convencoes coexistem sem padrao: `UsuarioDAO.buscarPorId()`, `RotaDAO.listarTodasAsRotasComoObjects()`, `AuxiliaresDAO.obterIdAuxiliar()`. Alguns DAOs usam `get*` (getColumnNames) misturado com portugues.
- **Impacto:** Confusao sobre qual verbo usar ao criar novos metodos.
- **Fix sugerido:** Padronizar: `buscarPorId()` (single), `listar*()` (collection), `obter*()` (derived/computed). Documentar convencao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Controllers GUI

#### Issue #DM063 — ValorExtensoUtil: utility class dentro de controller
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/GerarReciboAvulsoController.java:626`
- **Problema:** Classe `ValorExtensoUtil` (converte numero para extenso em portugues) definida como inner class do controller. E uma utility pura sem relacao com UI.
- **Impacto:** Impossivel reutilizar em outros controllers ou na API. Infla o controller.
- **Fix sugerido:** Mover para `gui/util/ValorExtensoUtil.java` ou `util/ValorExtensoUtil.java`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM064 — TelaPrincipalController.handleBackup: logica de infra em controller
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/TelaPrincipalController.java:1005-1179`
- **Linha(s):** 1005-1179 (175 linhas)
- **Problema:** Metodo de backup dentro do controller principal: selecao de arquivo, geracao SQL de export, criacao de ZIP, recovery de erro. Logica de infraestrutura que nao pertence a UI.
- **Impacto:** Impossivel executar backup headless (cron, CLI); logica acoplada a FileChooser.
- **Fix sugerido:** Extrair `BackupService.executarBackupCompleto(Path destino)`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Models

#### Issue #DM065 — Encomenda: campos de data duplicados (String + LocalDate)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Encomenda.java:28-101`
- **Problema:** Tem `String dataLancamento` E `LocalDate dataLancamentoDate` para o mesmo conceito. Setter sincroniza ambos mas getter retorna o String. Dual representation cria bugs de sincronizacao.
- **Impacto:** Se alguem seta apenas um campo, o outro fica stale.
- **Fix sugerido:** Deprecar `dataLancamento` (String); usar apenas `dataLancamentoDate` (LocalDate). Controllers formatam com DTF.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM066 — 7+ DTOs financeiros sem equals/hashCode
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Despesa.java, ReciboQuitacaoPassageiro.java, ReciboAvulso.java, EncomendaFinanceiro.java, FreteFinanceiro.java, FreteDevedor.java, PassagemFinanceiro.java, ItemResumoBalanco.java
- **Problema:** DTOs financeiros usados em TableView e colecoes sem equals/hashCode. Passagem.java tem implementacao correta (L262-272) mas os demais nao seguem o padrao.
- **Impacto:** Comportamento incorreto em HashSet, HashMap, removeIf, contains.
- **Fix sugerido:** Adicionar equals/hashCode baseado em id (ou campos primarios).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Web / BFF

#### Issue #DM067 — Passagens.jsx 581L com autocomplete inline e 2 modals
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/src/pages/Passagens.jsx:1-581`
- **Problema:** Componente monolitico com autocomplete construido manualmente (div + event handlers + position absolute), 2 modals (criar + pagar), 10+ inline styles. Autocomplete deveria ser componente reutilizavel.
- **Impacto:** Logica de autocomplete impossivel de reutilizar; inline styles dificultam theming.
- **Codigo problematico:**
```jsx
<div style={{
  position: 'absolute', top: '100%', left: 0, right: 0,
  background: 'var(--bg-card)', border: '1px solid var(--border)',
  borderRadius: 6, maxHeight: 200, overflowY: 'auto',
  zIndex: 100, boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
}}>
```
- **Fix sugerido:** Extrair `<Autocomplete>` component, `<ModalCriarPassagem>`, `<ModalPagarPassagem>`.
- **Observacoes:**
> _Complementa #099 do AUDIT V1.2 com detalhes adicionais._

---

#### Issue #DM068 — BFF: formato de erro inconsistente entre rotas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/` (todas as rotas)
- **Problema:** Alguns endpoints retornam `{ error: 'msg' }`, outros `{ message: 'msg' }`, outros apenas status 500 sem body. Nao ha middleware central de erro.
- **Impacto:** Frontend precisa tratar multiplos formatos; dificil debugging.
- **Fix sugerido:** Criar errorHandler middleware: `{ error: true, message, code, timestamp }` padrao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### API (Spring Boot)

#### Issue #DM069 — Services misturam Map e DTO como retorno
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java:50-85`
- **Problema:** Alguns services retornam `List<Map<String, Object>>` (PassagemService), outros retornam DTOs tipados. Inconsistencia dificulta consumo pelos controllers.
- **Impacto:** Callers precisam fazer cast manual; sem type-safety; fragil a mudancas de schema.
- **Fix sugerido:** Padronizar: todos services retornam DTOs tipados.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM070 — VersaoChecker: JSON parsing fragil com indexOf/substring
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/VersaoChecker.java:104-135`
- **Problema:** Parsing manual de JSON da API com `indexOf()` e `substring()` em vez de usar biblioteca JSON (Gson/Jackson ja no classpath via JARs).
- **Impacto:** Quebra silenciosamente se formato do JSON mudar (ex: campo com aspas no valor).
- **Fix sugerido:** Usar `com.google.gson.JsonParser` ou `org.json.JSONObject`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

## COBERTURA

| Camada | Diretorio | Arquivos | Issues novas | Issues anteriores |
|--------|-----------|----------|-------------|-------------------|
| Desktop | src/dao/ | 20 | 4 | 2 |
| Desktop | src/gui/ (controllers) | 40 | 3 | 10 |
| Desktop | src/gui/util/ | 9 | 1 | 0 |
| Desktop | src/model/ | 26 | 2 | 2 |
| Desktop | src/tests/ | 5 | 0 | 1 |
| Web | naviera-web/src/ | 15 | 1 | 2 |
| Web | naviera-web/server/ | 12 | 1 | 1 |
| App | naviera-app/src/ | 27 | 0 | 0 |
| API | naviera-api/src/ | 18 | 1 | 0 |
| OCR | naviera-ocr/src/ | 6 | 0 | 0 |
| Site | naviera-site/src/ | 3 | 0 | 0 |
| Cross | Multiplos | - | 2 | 0 |
| **TOTAL** | | **155** | **15** | **15** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO)

- [ ] #DM057 — Criar camada de servico (FreteService, EncomendaService, PassagemService) — **Esforco:** 3-5 dias
- **Notas:**
> _Issue estrutural mais importante. Desbloqueia: eliminacao de SQL inline (#DM007), reducao de god classes (#049, #094-097), eliminacao de duplicacao Desktop vs Web (#101). Recomendado comecar por FreteService (CadastroFreteController e o maior)._

### Importante (ALTO)

- [ ] #DM056 — Mover AppLogger para pacote nao-GUI (21 arquivos) — **Esforco:** 30 min
- [ ] #DM059 — Reescrever TipoPassageiroDAO.listarNomes com SELECT direto — **Esforco:** 10 min
- [ ] #DM063 — Mover ValorExtensoUtil para gui/util/ — **Esforco:** 10 min
- [ ] #DM064 — Extrair BackupService de TelaPrincipalController — **Esforco:** 1-2 horas
- [ ] #DM067 — Extrair Autocomplete + Modals de Passagens.jsx — **Esforco:** 2-3 horas
- [ ] #DM032 — Mover 19 inner classes restantes para model/ — **Esforco:** 2-3 horas
- [ ] #DM004 — Expandir PrintLayoutHelper (corpo + itens, nao so headers) — **Esforco:** 1-2 dias
- **Notas:**
> _DM056 e DM059/DM063 sao quick wins mecanicos. DM064 e DM067 reduzem god classes. DM032 e trabalho repetitivo mas de baixo risco._

### Moderado (MEDIO)

- [ ] #DM058 — Unificar API client web/app/ocr — **Esforco:** 2-3 horas
- [ ] #DM060 — Extrair ConferenteDAO para retornar objetos tipados — **Esforco:** 30 min
- [ ] #DM061 — DespesaDAO extrair buildWhereClause — **Esforco:** 30 min
- [ ] #DM062 — Documentar convencao naming DAOs — **Esforco:** 30 min
- [ ] #DM065 — Deprecar Encomenda.dataLancamento String — **Esforco:** 1 hora
- [ ] #DM066 — Adicionar equals/hashCode em 7+ DTOs — **Esforco:** 1 hora
- [ ] #DM068 — Criar errorHandler middleware no BFF — **Esforco:** 1-2 horas
- [ ] #DM069 — Padronizar retorno de services Spring Boot para DTOs — **Esforco:** 2-3 horas
- [ ] #098 — Extrair tabs/modals de Financeiro.jsx — **Esforco:** 2-3 horas
- [ ] #100 — Adicionar ErrorBoundary no naviera-web — **Esforco:** 30 min
- [ ] #103 — Criar crudRouter factory no BFF — **Esforco:** 2-3 horas

### Menor (BAIXO)

- [ ] #DM070 — VersaoChecker usar biblioteca JSON — **Esforco:** 30 min

---

## NOTAS

> **Progresso V3.0 → V4.0:** Nenhuma issue anterior foi resolvida nesta iteracao (sessao de auditoria, nao de correcao). Porem, a auditoria revelou que:
>
> 1. **V3.0 subestimou issues restantes:** Reportou "~5 ativas" mas ha **14 pendentes** + **15 novas** = **29 ativas**. A diferenca vem de: (a) inner classes contadas como ~6, na verdade sao 19; (b) issues estruturais do AUDIT (#094-103) nao foram recontadas no V3.0; (c) camadas Web/App/API/OCR/Site nao foram auditadas no V3.0.
>
> 2. **Issue mais critica: falta de camada Service (#DM057).** E a raiz de multiplos problemas: SQL inline em controllers (#DM007), god classes (#049, #094-097), duplicacao Desktop vs Web (#101-102), impossibilidade de testes unitarios (#061). Resolver DM057 desbloqueia ~5 outras issues.
>
> 3. **Quick wins identificados (30 min cada):** DM056 (AppLogger rename), DM059 (TipoPassageiroDAO), DM063 (ValorExtensoUtil), DM060 (ConferenteDAO), #100 (ErrorBoundary web).
>
> 4. **naviera-app refatorado com sucesso:** App.jsx foi de 1144L para 179L (refactoring exemplar). Nenhuma issue nova encontrada no app.
>
> 5. **Cobertura ampliada:** V3.0 cobriu apenas Desktop (131 arquivos). V4.0 cobriu todas as 6 camadas (155 arquivos). Camadas Web/BFF contribuiram 4 novas issues; API Spring Boot 1.

---
*Gerado por Claude Code (Deep Audit V4.0) — Revisao humana obrigatoria*
