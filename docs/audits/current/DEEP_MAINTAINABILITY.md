# AUDITORIA PROFUNDA — MAINTAINABILITY — Naviera Eco
> **Versao:** V5.0
> **Data:** 2026-04-18
> **Categoria:** Maintainability
> **Base:** AUDIT_V1.3 + DEEP_MAINTAINABILITY V4.0
> **Arquivos analisados:** 168 de 168 total (Desktop + Web + App + API + OCR + Site)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Issues V4.0 resolvidas (re-verificadas) | 22 |
| Issues V4.0 pendentes | 7 |
| Issues V1.3 maintainability ja conhecidas (consolidadas) | 27 |
| **Novos problemas (V5.0)** | **15** |
| Falsos positivos descartados | 2 |
| **Total de issues ativas** | **49** |

**Distribuicao por severidade (ativas):**

| CRITICO | ALTO | MEDIO | BAIXO |
|---------|------|-------|-------|
| 0 | 11 | 22 | 16 |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas (re-verificadas)

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #DM056 | AppLogger movido para `util/` | `src/util/AppLogger.java` existe; **0** imports `gui.util.AppLogger`; 96 usos via `util.AppLogger` |
| #DM057 | Camada de servico Desktop (PARCIAL) | `src/service/FreteService.java` + `BackupService.java` criados; CadastroFrete delega; falta EncomendaService/PassagemService |
| #DM058 | API client unificado (PARCIAL) | Padrao `request/api.{get,post,put,delete}` em web/app/ocr; **mas** consts divergem (`BASE` vs `API`) — ver DM072 |
| #DM059 | TipoPassageiroDAO.listarNomes | Confirmado SELECT direto (sem listarTodos) |
| #DM060 | ConferenteDAO tipado | Confirmado |
| #DM061 | DespesaDAO addFiltrosComuns | Confirmado |
| #DM062 | Convencao naming DAOs | `src/dao/package-info.java` documenta `buscarPor/listar/obter/inserir/atualizar/excluir/salvar` |
| #DM063 | ValorExtensoUtil em gui/util | Confirmado em `src/gui/util/ValorExtensoUtil.java` |
| #DM064 | BackupService extraido | Confirmado em `src/service/BackupService.java` (nao em `gui/util/` como sugerido — package mais apropriado) |
| #DM065 | Encomenda.dataLancamento @Deprecated | `src/model/Encomenda.java:92` confirma `@Deprecated` |
| #DM066 | equals/hashCode em DTOs financeiros | 30 modelos com equals/hashCode |
| #DM067 | Autocomplete + Modals extraidos | `naviera-web/src/components/{Autocomplete,ModalCriarPassagem,ModalPagarPassagem}.jsx` confirmados |
| #DM068 | errorHandler middleware BFF | `naviera-web/server/middleware/errorHandler.js` confirmado |
| #DM069 | Map→DTO documentado | TODOs presentes (DM069 referenciado por #522 — pendente fechar) |
| #DM070 | VersaoChecker → Jackson | `import com.fasterxml.jackson.databind.{JsonNode,ObjectMapper}` confirmado |
| #DM032 | Inner classes movidas | 16/19 extraidos (3 intencionais) |
| #098 | Financeiro.jsx refatorado | Reduzido + componentes em `src/components/financeiro/` |
| #100 | ErrorBoundary naviera-web | `naviera-web/src/components/ErrorBoundary.jsx` confirmado |
| #103 | crudFactory BFF | `naviera-web/server/utils/crudFactory.js` confirmado (mas ver DM081) |
| Re-verificadas tambem | DM031-DM055, etc. | Sem regressoes detectadas |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| #DM057 | Camada Service Desktop | Falta EncomendaService, PassagemService, OCRService. CadastroFrete chama FreteService apenas em alguns paths |
| #DM058 | API client unificado | Padrao request unificado, mas constants/exports divergem — ver DM072 |
| #DM004 | PrintLayoutHelper expandido | Helpers adicionados (`criarLinhaInfo`, `criarTabelaItens`, `criarSeparadorFino`, `criarRodape`) **mas** controllers ainda nao migraram. Ainda ha layout inline em RelatorioFretes (~600 linhas de impressao). |
| #056 | Passagem 48 campos | Organizado em secoes; split nao foi feito |
| #061 | Zero testes unitarios | Apenas 5 classes desktop, 3 API, 1 BFF — cobertura minima. Consolidado em #527/#528/#529 |
| #103 | crudFactory aplicado | Apenas 4 entidades migraram para `tenantCrud`, 6 para `auxCrud`. Resto continua inline em `cadastros.js` (39 endpoints, 862L) — ver DM081 |

### Pendentes (de V4.0 e AUDIT_V1.3)

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #049/#506/#507/#094/#095 | Controllers >500L | CadastroFrete 2081L, VenderPassagem 1824L, RelatorioFretes 1748L, InserirEncomenda 1719L, TelaPrincipal 1433L, ListaEncomenda 953L, GestaoFuncionarios 759L. Sem mudanca |
| #050/#508 | Funcoes >100L | configurarTabela ~292L, configurarAutoCompleteComboBox ~270L, imprimirRelatorioTermico ~229L. Sem mudanca |
| #058 | Sem gerenciador dependencias Desktop | Sem Maven/Gradle; 44 JARs em lib/ |
| #DM007/#512 | SQL inline em controllers | RelatorioFretes(13 ConexaoBD), CadastroBoleto(9), FinanceiroSaida(6), BalancoViagem(6), TabelaPrecoFrete(5), FinanceiroFretes(5), TelaPrincipal(4), 87 ocorrencias totais |
| #096 | RelatorioFretesController 1748L | 4 metodos de impressao monoliticos |
| #097 | InserirEncomendaController 1719L | configurarTabela com 292L de cell factories |
| #101 | Duplicacao Desktop vs Web | Web BFF reimplementa logica em SQL (financeiro/dashboard, balanco, baixa). FreteService Desktop nao tem espelho no BFF |
| #500 | Triplicacao `pagar()` PSP services | EncomendaService.pagar (96L) ≈ FreteService.pagar (102L) ≈ PassagemService.comprar (115L). Confirmado linha por linha |
| #501 | Triplicacao modal pagamento App | EncomendaCPF L68-111, FinanceiroCNPJ L52-105, PassagensCPF L99-130. Confirmado |
| #502 | Magic 0.10 PIX em 6 lugares | 3× Java services + 3× JSX screens. Confirmado |
| #503 | AdminPspController + PspController | Dois controllers com `/status` e `/onboarding` (intencional split admin/operador) — falta compartilhar DTOs/testes |
| #504 | BCrypt cost 12 vs 10 | UsuarioDAO.java cost=12, BFF auth.js/cadastros.js cost=10 |
| #505 | crudFactory `.then/.catch` | Confirmado — destoa do resto (async/await) |
| #506 | CadastroFreteController 2081L | Confirmado |
| #507 | VenderPassagemController 1824L | Confirmado |
| #508 | Metodos >100L Desktop | Confirmado — CadastroFrete L284-428 (145L), L1650-1734 (85L), L1546-1597 (52L) |
| #509 | Pages React >500L | ReviewOCR 755L, Fretes 710L, GestaoFuncionarios 683L, Encomendas 676L, Passagens 561L, RelatorioFretes 505L. Confirmado |
| #510 | ocr.js 935L, cadastros.js 862L | Confirmado (cadastros.js apesar do crudFactory permanece em 862L) |
| #511 | Controllers API com JdbcTemplate direto | OpEmbarcacaoController.java (24L) e OpRotaController.java (24L) confirmados — bypass de service |
| #513 | RotaController.findAll() sem tenant | `repo.findAll()` em 14 LOC, sem filtro de empresa_id confirmado |
| #515 | .env.example divergentes | DB_PORT(.env=6, api=1, web=3, app=0); JWT_SECRET em 3 arquivos sem header de sincronizacao |
| #516 | vite.config.js portas hardcoded | Confirmado linhas 7,10 (5174, http://localhost:3002) |
| #517 | CLAUDE.md 24h vs codigo 8h | CLAUDE.md L120 = "24h"; application.properties L33 = `28800000ms` (8h); auth.js L13 = `'8h'` |
| #518 | CLAUDE.md drift de contagens | Confirmado |
| #519 | CLAUDE.md sem PSP | Confirmado |
| #522 | TODOs DM069 PassagemService | 5 TODOs L33, 63, 183, 219, 244 confirmados |
| #523 | TODO PublicController L44 | Confirmado |
| #524 | A.txt commitado | Conteudo: "ajeitar os relatorios de frete, pois estao todos errados..." — anotacao pessoal |
| #525 | db.properties.bak2 + log_erros.txt + RELATÓRIO | db.properties.bak2 (1.6KB) **paradoxalmente esta com pattern em .gitignore mas continua tracked**; log_erros.txt (3495 linhas, ~150KB) sem pattern; "RELATÓRIO GERAL DO PROJETO SISTEMA 19.02.25.txt" (5.9KB) — ver DM082 |
| #526 | Encoding corrompido Desktop | Confirmado em 20+ pontos: "MÃ‰TODO", "ExclusÃ£o", "MÉTODO" misturados |
| #527 | Services API sem testes | Apenas AuthControllerTest, RateLimitFilterTest, SecurityTest. Faltam EncomendaService, FreteService, PassagemService, OnboardingService, EmpresaPspService, PspCobrancaService + 22 outros |
| #528 | Middlewares BFF sem testes | Apenas `ocr.test.js`. Faltam tenant/auth/rateLimit/validate/errorHandler |
| #529 | App mobile sem testes | Confirmado — sem `tests/` e sem framework |
| #530 | EncomendaService.buscarPorCliente faz LIKE por NOME | Confirmado L40-76 |
| #710 | DELETE frete_itens sem empresa_id | CadastroFreteController.java:1563 confirmado |
| #712 | numeroBilhete colisivo | PassagemService.java:105 `currentTimeMillis() % 1000000` confirmado. BilheteService usa `seq_numero_bilhete` corretamente |
| #713 | /rotas e /op/rotas | Confirmado. **Adicionalmente**: ha 3a implementation em `naviera-web/server/routes/cadastros.js:270-313` (POST/PUT/DELETE `/rotas`) — ver DM085 |
| #715 | RELATÓRIO GERAL PROJETO 19.02.25.txt | Confirmado no root |
| #717 | CadastroFreteController encoding em UI | L1548-1555 confirmado: "NÃ£o hÃ¡ frete...", "ExclusÃ£o" em AlertHelper |

---

## NOVOS PROBLEMAS

### Cross-cutting / Arquitetura

#### Issue #DM071 — `naviera-app/src/api.js` mistura padrao `request()` com legacy `authFetch()`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/api.js:43-59`
- **Problema:** Apos DM058 (unificacao), o arquivo expoe DOIS padroes:
  - `api.{get,post,put,delete}` → `request()` injeta Bearer + trata 401 — **forma correta**
  - `authFetch(url, options)` → wrapper de `fetch()` que NAO injeta Bearer (so trata 401) — caller precisa montar `Authorization` manual
  Apesar do comentario dizer "backward-compatible", chamadores das tres telas (`EncomendaCPF`, `PassagensCPF`, `FinanceiroCNPJ`) usam `authFetch(${API}/encomendas/${id}/pagar, { headers: authHeaders })` em vez de `api.post('/encomendas/' + id + '/pagar', { formaPagamento: forma })`. Resultado: padrao "unificado" coexiste com padrao antigo.
- **Impacto:** Uma sessao expirada num caminho `authFetch` pode ser tratada diferente de `api.*`. Aumenta superficie de bug.
- **Codigo problematico:**
```js
export const api = { get: (path) => request(path), post: ..., put: ..., delete: ... }

export function authFetch(url, options = {}) {  // legacy — sem Bearer
  return fetch(url, options).then(res => {
    if (res.status === 401 || res.status === 403) clearSession()
    return res
  })
}
```
- **Fix sugerido:** Migrar `EncomendaCPF`, `PassagensCPF`, `FinanceiroCNPJ` para `api.post()`. Remover `authFetch` ou marcar `@deprecated`.
- **Observacoes:**
> _Fix complementar de DM058 + #501._

---

#### Issue #DM072 — Constants `BASE` vs `API` divergem entre web/app/ocr api.js
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivos:** `naviera-web/src/api.js:1`, `naviera-app/src/api.js:3`, `naviera-ocr/src/api.js:1`
- **Problema:** Mesmo apos "unificacao" (DM058):
  - web: `const BASE = import.meta.env.VITE_API_URL || '/api'`
  - app: `export const API = import.meta.env.VITE_API_URL || "http://localhost:8081/api"`
  - ocr: `export const API = import.meta.env.VITE_API_URL || '/api'`
  Nome do export divergente (`BASE` no web, `API` nos outros) e fallback divergente (`/api` vs `http://localhost:8081/api`).
- **Impacto:** Refactoring fica fragil — `import { BASE } from './api'` no web precisa virar `API` no app. Fallback diferente esconde bug em prod (web atrasa Nginx 404 vs app falha conexao 8081).
- **Fix sugerido:** Padronizar export `API` em todos; mover fallback para `process.env.VITE_API_URL` obrigatorio em prod (fail-fast).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### API (Spring Boot)

#### Issue #DM073 — Sub-pattern N+1 em `EncomendaService.pagar` / `FreteService.pagar`: query separada de `psp_subconta_id` apos query principal
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `naviera-api/.../service/EncomendaService.java:178-183`, `FreteService.java:157-162`, `PassagemService.java:141-146`
- **Problema:** Apos a query principal (carrega entidade + empresa_id), os 3 services fazem segunda viagem ao DB para buscar `psp_subconta_id`:
```java
// EncomendaService L178
String subcontaId = (String) jdbc.queryForMap(
    "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId).get("psp_subconta_id");
```
Em FreteService L163-164 chega a haver tambem terceira query (`SELECT numero_frete...`).
- **Impacto:** Maintenance — 3 SELECTs separados onde 1 JOIN bastaria. Cada novo campo da empresa precisa replicar em 3 lugares.
- **Fix sugerido:** Adicionar `psp_subconta_id` (e numero_xxx) ao `queryForList` inicial via JOIN.
- **Observacoes:**
> _Vai junto com #500 quando for refatorado para `PagamentoProcessor`._

---

#### Issue #DM074 — `AsaasGateway.criarCobranca` 80 linhas com 5 responsabilidades
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java:51-131`
- **Problema:** Um unico metodo faz: (1) validar config, (2) resolver/criar customer, (3) montar payload Asaas, (4) POST + parse, (5) fetch QR/boleto adicional, (6) calcular split valor. Sem extracao de helpers privados.
- **Impacto:** Dificil testar isoladamente cada etapa; mudanca em um aspecto exige reler 80 linhas.
- **Fix sugerido:**
```java
public CobrancaResponse criarCobranca(CobrancaRequest req) {
    validarConfiguracao(req);
    String customerId = obterOuCriarCustomer(req);
    Map<String, Object> payload = montarPayload(req, customerId);
    JsonNode body = post("/payments", payload);
    return mapearResposta(req, body);
}
```
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM075 — `AsaasGateway` instancia `RestTemplate` inline (sem injecao + sem timeout)
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java:39, 44`
- **Problema:**
```java
private final RestTemplate rest;
public AsaasGateway(AsaasProperties props) {
    this.props = props;
    this.rest = new RestTemplate();   // <-- inline, sem timeout
}
```
`RestTemplate` puro tem timeout infinito. Soma com #300 do AUDIT (AsaasGateway sem timeout) — esta e a face de manutenibilidade do mesmo bug.
- **Impacto:** Impossivel adicionar timeout/interceptor/retry centralmente. Cada metodo HTTP do Asaas perde customizacao.
- **Fix sugerido:**
```java
public AsaasGateway(AsaasProperties props, RestTemplateBuilder builder) {
    this.props = props;
    this.rest = builder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(15))
        .build();
}
```
- **Observacoes:**
> _Acoplado com #300 do AUDIT (resilience). Fix unico atende ambos._

---

### Desktop / Util

#### Issue #DM076 — `src/gui/util/SyncClient.java` god class de 1163 LOC com 37 metodos publicos e 6 mapas de configuracao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 1-1163
- **Problema:** Singleton com 37 `public` methods misturando: persistencia config (`carregarConfiguracoes`/`salvarConfiguracoes`), agendamento (`scheduler`, `criarScheduler`), HTTP (timeouts, retries), parser de tabelas (TABELAS_SYNC, COLUNA_ID, TABELAS_COM_EXCLUIDO, TABELAS_COM_IS_EXCLUIDO, COLUNAS_SKIP_DADOS), threadpool, listener pattern. 6 colecoes estaticas hardcoded com nomes de tabela/coluna do schema.
- **Impacto:** Adicionar nova tabela exige editar 6 mapas estaticos sem coordenacao. Bug em retry duplica em 4 entry points. Impossivel testar sem efeito colateral em filesystem (sync_config.properties).
- **Fix sugerido:** Quebrar em `SyncConfig` (load/save), `SyncScheduler`, `SyncHttpClient`, `SyncTableMetadata` (mapas), `SyncEngine`. Singleton apenas para `SyncEngine`.
- **Observacoes:**
> _Mesmo padrao de god class do CadastroFreteController, mas em util/. Justifica tambem por SyncService.java (API) ter 466 LOC (#DM057-pontos-cegos)._

---

#### Issue #DM077 — `src/gui/util/RelatorioUtil.java` 807 LOC sem split por dominio
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/RelatorioUtil.java`
- **Problema:** Utility crescente que mistura helpers de relatorio para passagem, encomenda, frete, balanco. Sem separacao por dominio (`RelatorioPassagemUtil`, `RelatorioFreteUtil`, etc.).
- **Impacto:** Mudanca em relatorio de frete pode quebrar rendering de passagem. Builds Eclipse sem incremental compilam o arquivo todo.
- **Fix sugerido:** Quebrar por dominio + manter `RelatorioCommonUtil` para realmente compartilhado (formatacao monetaria, datas).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Web / React

#### Issue #DM078 — `Fretes.jsx` com 43 useState + 94 inline styles (pior pagina React)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/src/pages/Fretes.jsx` (710 LOC)
- **Problema:** **43 chamadas `useState`** (top da liga; comparativo: Encomendas 27, Passagens 23, GestaoFuncionarios 19, ReviewOCR ~15) e **94 inline styles**. Mais hostil para manutencao do que ReviewOCR (755L).
- **Impacto:** Cada interacao reabre 43 estados; impossivel raciocinar sobre fluxo sem ler todo o arquivo. React DevTools fica ilegivel.
- **Fix sugerido:** Extrair custom hooks (`useFretesData`, `useFreteForm`, `useFreteOcrUpload`); subcomponentes por secao (header, filtros, tabela, modal). Migrar inline styles para CSS classes ja existentes em theme.
- **Observacoes:**
> _Eleva prioridade de #509 — Fretes.jsx merece atencao maior do que ReviewOCR.jsx (apesar de menor)._

---

#### Issue #DM079 — `docs/audits/temp/` commitado com 350KB de scan dumps
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `docs/audits/temp/` (12 arquivos, 350KB)
- **Problema:** Diretorio explicitamente marcado como "temp" pelo workflow audit-2-scan, contem dumps intermediarios (`00_setup.md`, `01_bugs.md` ... `07_review.md`). Esta tracked pelo git e foi reaparecendo a cada audit.
- **Impacto:** Repositorio incha com cada audit (~350KB por scan); reviewers veem diff de dumps que serao re-gerados.
- **Fix sugerido:** Adicionar `docs/audits/temp/` ao `.gitignore`; rodar `git rm -r --cached docs/audits/temp/`. Skill `audit-2-scan` ja escreve la — apenas garantir que nao seja versionado.
- **Observacoes:**
> _Vi os arquivos com timestamp 18-04 (hoje). Skill mantem a pasta entre runs sem cleanup._

---

#### Issue #DM080 — `crudFactory.js` interpola nomes de tabela/coluna sem validacao defensiva
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/utils/crudFactory.js:25, 37, 48, 61, 90, 99, 109, 121`
- **Problema:**
```js
`SELECT ${select} FROM ${table} WHERE empresa_id = $1${extraWhere} ORDER BY ${order}`
`INSERT INTO ${table} (${nameColumn}, empresa_id) VALUES ($1, $2) ...`
```
Nao e SQL injection (input vem de configuracao server-side, nao de request). **Mas** typo no `table` ou `nameColumn` no call site quebra silenciosamente em runtime no primeiro acesso. Sem fail-fast no boot.
- **Impacto:** Erro digitacao so aparece quando alguem clica num CRUD; teste de smoke nao cobre todas entidades.
- **Fix sugerido:** No factory, validar contra regex `^[a-z_][a-z0-9_]*$` E (idealmente) consultar `information_schema.columns` no boot pra confirmar existencia.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM081 — `cadastros.js` continua com 39 endpoints/862L apesar do crudFactory (DM103)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Problema:** Apos refactoring DM103 (`#103` em V4.0), apenas 4 entidades migraram para `tenantCrud` (conferentes, caixas, tipo_passageiro, clientes-encomenda) e 6 para `auxCrud`. Resto continua inline: `/usuarios` GET/POST/PUT, `/tarifas`, `/empresa`, `/recebimento`, `/funcionarios`, `/itens-encomenda`, `/itens-frete`, `/rotas` GET/POST/PUT/DELETE, `/embarcacoes`, etc. Total: 39 router calls em 862 linhas.
- **Impacto:** Migration nao concluiu — beneficio do factory esta diluido. Continua sendo o pior arquivo de routes (depois de ocr.js 935L).
- **Fix sugerido:** Quebrar `cadastros.js` em `routes/cadastros/{usuarios,funcionarios,tarifas,empresa,rotas,embarcacoes,itens}.js`. Ainda usar `tenantCrud` onde fizer sentido.
- **Observacoes:**
> _Complementa #103 (parcial) e #510 com escopo cirurgico._

---

### Cross-cutting

#### Issue #DM082 — `log_erros.txt` (3495 linhas) commitado no root + `db.properties.bak2` apesar de pattern em .gitignore
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivos:** `log_erros.txt` (~150KB, 3495L), `db.properties.bak2` (1.6KB)
- **Problema:**
  - `log_erros.txt` — sem pattern em .gitignore; arquivo de log de erros do desktop crescente, agora com 3495 linhas.
  - `db.properties.bak2` — `.gitignore` tem `db.properties.bak*` (linha 26) **mas** o arquivo continua tracked porque foi commitado antes da regra. `.gitignore` so ignora arquivos NOVOS; nao remove os ja rastreados.
- **Impacto:** Vazamento potencial de schema/credenciais em logs/backup historicos. Inflando repo.
- **Fix sugerido:**
```bash
git rm --cached log_erros.txt db.properties.bak2
echo "log_erros.txt" >> .gitignore
git commit -m "remove log_erros.txt and db.properties.bak2 from tracking"
```
Adicionalmente: redirecionar AppLogger para `~/.naviera/logs/` em vez do CWD.
- **Observacoes:**
> _Inclui parte do #525 mas eleva severidade — `db.properties.bak2` foi confirmado tracked **mesmo com pattern em .gitignore** (foi commitado antes do pattern)._

---

#### Issue #DM083 — `MoneyUtils.java` (API) duplica logica de `MoneyUtil.java` (Desktop)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivos:** `naviera-api/src/main/java/com/naviera/api/config/MoneyUtils.java` (13L), `src/gui/util/MoneyUtil.java` (Desktop)
- **Problema:** Duas implementations independentes para a mesma operacao (formatacao monetaria BRL). Singular vs plural no nome (`MoneyUtil` vs `MoneyUtils`) — confunde grep.
- **Impacto:** Mudanca de regra (ex: passar de "R$ 1.234,56" para "BRL 1234.56") exige editar dois lugares. Bug em arredondamento divergente entre camadas.
- **Fix sugerido:** Manter os dois (camadas diferentes, nao podem compartilhar codigo) **mas** rebatizar para nome igual (`MoneyUtil`) e documentar que devem espelhar uma a outra. Adicionar teste de "round-trip" cruzado se possivel.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DM084 — Calculo `desconto10 = saldo * 0.10` triplicado nas telas do app
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `naviera-app/src/screens/EncomendaCPF.jsx:71`, `PassagensCPF.jsx:102`, `FinanceiroCNPJ.jsx:64`
- **Problema:** Mesma expressao literal `formaPag === "PIX" ? saldo * 0.10 : 0` em 3 telas. Sub-issue de #501/#502 (que tratam JSX e magic number) — esta foca no calculo em si.
- **Impacto:** Mudar regra do desconto exige editar 3 telas + 3 services Java + 1 properties. **6 lugares** ja contados em #502 + **3 lugares de calculo** aqui = ainda mais espalhado do que #502 sugere.
- **Fix sugerido:** Apos resolver #502 (`PSP_DESCONTO_PIX_PCT` no backend), criar `helpers.js: calcularDescontoApp(saldo, formaPag, pct)` no app que recebe `pct` injetado por bootstrap. Telas chamam `calcularDescontoApp(saldo, formaPag, percentualDescontoPix)`.
- **Observacoes:**
> _Reforca urgencia de #502._

---

#### Issue #DM085 — Entidade `rotas` tem 3 implementations divergentes (RotaController + OpRotaController + cadastros.js)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `naviera-api/.../controller/RotaController.java`, `OpRotaController.java`, `naviera-web/server/routes/cadastros.js:270-313`
- **Problema:** AUDIT_V1.3 #713 ja noticiou dois lugares (`/rotas` e `/op/rotas` em Spring). Mas tambem ha `POST /api/cadastros/rotas`, `PUT /api/cadastros/rotas/:id`, `DELETE /api/cadastros/rotas/:id` no BFF Express que duplicam CRUD direto no Postgres. **3 endpoints distintos** servem rotas, com semantica e validacao diferentes (RotaController nao filtra tenant, OpRotaController filtra, BFF/cadastros filtra mas faz check de viagens associadas no DELETE).
- **Impacto:** Frontend confuso sobre qual endpoint usar; mudanca de schema exige editar 3 lugares; surge bug onde DELETE em /api/cadastros/rotas funciona mas DELETE em /op/rotas/:id retorna erro inconsistente.
- **Fix sugerido:** Eleger um responsavel:
  - **Opcao A:** Spring `OpRotaController` (com tenant) e a fonte. BFF passa proxy.
  - **Opcao B:** BFF `cadastros.js` e a fonte. Spring nao expoe `/op/rotas` (so leitura via /rotas publica, sem tenant).
  Documentar decisao em ADR.
- **Observacoes:**
> _Estende escopo do #713 (que cobria so 2 dos 3 lugares)._

---

## FALSOS POSITIVOS DESCARTADOS

| Issue | Motivo |
|-------|--------|
| #DM057 | Marcada PARCIAL no V4.0 — re-verificada aqui como majoritariamente RESOLVIDA. FreteService funcionando, BackupService criado, CadastroFreteController reduzido. Falta apenas EncomendaService/PassagemService — registrado como "PARCIAL" (sem nova issue) |
| #094 | Re-listado em V4.0 como "god class" mas DUPLICATED com #506 (mesmo arquivo, mesma queixa). Consolidado em #506 |

---

## COBERTURA

| Camada | Diretorio | Arquivos | Issues novas | Issues existentes verificadas |
|--------|-----------|----------|-------------|-------------------|
| Desktop | src/dao/ | 29 | 0 | 6 (DM056, DM059, DM060, DM061, DM062, DM007) |
| Desktop | src/gui/ controllers | 60+ | 1 (DM076 indireto) | 11 (#049, #050, #094-097, #506-508, #512, #717) |
| Desktop | src/gui/util/ | 21 | 2 (DM076, DM077) | 4 (DM004, #524, #526, DM063) |
| Desktop | src/model/ | 47 | 0 | 2 (DM065, DM066, #056) |
| Desktop | src/service/ | 2 | 0 | 1 (DM057, DM064) |
| Desktop | src/util/ | 1 | 0 | 1 (DM056) |
| Desktop | src/tests/ | 8 | 0 | 1 (#061) |
| API | controller/ | 28 | 0 | 4 (#511, #513, #523) |
| API | service/ | 31 | 1 (DM073) | 5 (#500, #522, #527, #530, #712) |
| API | psp/ | 14 | 3 (DM073, DM074, DM075) | 2 (#503, #500) |
| API | dto/, model/, security/, config/ | ~40 | 0 | 1 (#517) |
| Web | server/routes/ | 15 | 1 (DM081) | 4 (#510, #505, #103, #503) |
| Web | server/middleware/ | 5 | 0 | 2 (DM068, #528, #514) |
| Web | server/utils/ | 1 | 1 (DM080) | 2 (#103, #505) |
| Web | src/pages/ | 42 | 1 (DM078) | 6 (#509, #098, #100) |
| Web | src/components/ | 9 | 0 | 3 (DM067, #100) |
| App | src/api.js + screens | 14 | 2 (DM071, DM072, DM084) | 3 (#501, #502, #529) |
| App | components/, hooks/ | ~15 | 0 | 0 |
| Cross-cutting | docs/, .env, .gitignore, root | - | 4 (DM079, DM082, DM083, DM085) | 6 (#515-519, #524, #525, #715) |
| **TOTAL** | | **168** | **15** | **49** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO)

_(nenhuma issue CRITICA — categoria nao tem bloqueador de deploy)_

### Importante (ALTO) — 11

- [ ] #500 — Extrair `PagamentoProcessor` para deduplicar `pagar()` PSP — **Esforco:** 1-2 dias
- [ ] #501 — Extrair `PagamentoModal` no app — **Esforco:** 4h
- [ ] #502 — Mover `0.10` para config + injetar — **Esforco:** 2h
- [ ] #506 — Quebrar `CadastroFreteController` (extrair Autocomplete/OcrService/AudioService/XmlParser) — **Esforco:** 3-5 dias
- [ ] #507 — Quebrar `VenderPassagemController` (`VendaPassagemService` + `PassagemPrintHelper`) — **Esforco:** 2-3 dias
- [ ] #527 — Adicionar testes para 6 services PSP/financeiros criticos — **Esforco:** 3-5 dias
- [ ] #710 — Adicionar `empresa_id` ao DELETE de `frete_itens` — **Esforco:** 30 min
- [ ] #DM075 — `RestTemplate` injetado com timeout (resolve tambem #300) — **Esforco:** 1h
- [ ] #DM076 — Quebrar `SyncClient.java` em 5 classes — **Esforco:** 2-3 dias
- [ ] #DM078 — Refatorar `Fretes.jsx` (43 useState → custom hooks + componentes) — **Esforco:** 1-2 dias
- [ ] #DM082 — `git rm --cached log_erros.txt db.properties.bak2` + .gitignore — **Esforco:** 15 min
- **Notas:**
> _#500/#501/#502 sao bundle natural — uma sessao resolve todos. #DM075 e quick win com efeito em resilience._

### Importante (MEDIO) — 22

- [ ] #503 — Compartilhar DTOs entre AdminPspController e PspController — **Esforco:** 2h
- [ ] #504 — Padronizar BCRYPT_COST via env — **Esforco:** 1h
- [ ] #508 — Extrair metodos privados em CadastroFreteController — **Esforco:** 4h
- [ ] #509 — Refatorar 5 pages React >500L (ReviewOCR, GestaoFuncionarios, Encomendas, Passagens) — **Esforco:** 5 dias
- [ ] #510 — Quebrar ocr.js e cadastros.js por entidade — **Esforco:** 2 dias
- [ ] #511 — Mover OpEmbarcacaoController/OpRotaController para Service — **Esforco:** 2h
- [ ] #513 — `RotaController` com tenant ou prefixar `/public/rotas` — **Esforco:** 1h
- [ ] #515 — Header de sincronizacao em .env.example — **Esforco:** 30 min
- [ ] #516 — `vite.config.js` ler portas de env — **Esforco:** 15 min
- [ ] #517 — Atualizar CLAUDE.md de "24h" para "8h" — **Esforco:** 5 min
- [ ] #518 — Atualizar contagens em CLAUDE.md — **Esforco:** 15 min
- [ ] #519 — Adicionar secao PSP em CLAUDE.md — **Esforco:** 30 min
- [ ] #522 — Fechar TODOs DM069 (criar 5 DTOs em PassagemService) — **Esforco:** 4h
- [ ] #528 — Tests para middlewares BFF — **Esforco:** 1 dia
- [ ] #530 — Renomear `EncomendaService.buscarPorCliente` ou usar FK — **Esforco:** 2h
- [ ] #712 — Trocar `currentTimeMillis() % 1e6` por `seq_numero_bilhete` — **Esforco:** 30 min
- [ ] #713/#DM085 — Eliminar duplicacao /rotas em 3 lugares (decisao + ADR) — **Esforco:** 4h
- [ ] #717 — `iconv` em CadastroFreteController + `<sourceEncoding>UTF-8</sourceEncoding>` — **Esforco:** 1h
- [ ] #DM071 — Migrar app screens de `authFetch` para `api.post` — **Esforco:** 2h
- [ ] #DM073 — JOIN para `psp_subconta_id` (junto com #500) — **Esforco:** included em #500
- [ ] #DM074 — Refatorar `AsaasGateway.criarCobranca` em 5 helpers — **Esforco:** 2h
- [ ] #DM077 — Quebrar `RelatorioUtil.java` por dominio — **Esforco:** 4h
- [ ] #DM081 — Quebrar `cadastros.js` por entidade — **Esforco:** 4h (junto com #510)
- [ ] #DM084 — Helper `calcularDescontoApp` (junto com #502) — **Esforco:** included em #502
- [ ] #526 — `iconv` em todos os Controllers desktop — **Esforco:** 2h

### Menor (BAIXO) — 16

- [ ] #505 — `crudFactory.js` para async/await — **Esforco:** 30 min
- [ ] #523 — Implementar bucket dedicado em PublicController L44 — **Esforco:** 1h
- [ ] #524 — Remover `src/gui/util/A.txt` — **Esforco:** 5 min
- [ ] #525 — `git rm --cached "RELATÓRIO GERAL..."` (consolidado em DM082 mas listado a parte) — **Esforco:** 5 min
- [ ] #529 — Adicionar vitest no naviera-app — **Esforco:** 1 dia
- [ ] #715 — Mover RELATÓRIO para docs/archive/ — **Esforco:** 5 min
- [ ] #514 — `tenant.js` extrair `empresaService.findBySlug` — **Esforco:** 1h
- [ ] #DM072 — Padronizar export `API` em web/app/ocr — **Esforco:** 30 min
- [ ] #DM079 — Adicionar `docs/audits/temp/` ao .gitignore — **Esforco:** 5 min
- [ ] #DM080 — Validacao defensiva de table/column em crudFactory — **Esforco:** 1h
- [ ] #DM083 — Renomear MoneyUtils → MoneyUtil + documentar — **Esforco:** 30 min
- [ ] #094-097 (consolidado em #506/#507) — _ja contado_ — —
- [ ] #098 — _RESOLVIDO em V4.0_ — —
- [ ] #100 — _RESOLVIDO em V4.0_ — —
- [ ] #103 — _PARCIAL — ver DM081_ — —

---

## NOTAS

> **Progresso V4.0 → V5.0:**
>
> 1. **22 issues V4.0 estao confirmadas resolvidas.** Nenhuma regressao detectada.
>
> 2. **Categoria estavel quanto a CRITICOs (0).** Maintainability nao introduz bloqueadores de deploy — mas concentra a divida tecnica que vai sangrar nos proximos meses.
>
> 3. **27 issues V1.3 maintainability todas pendentes.** Nada foi corrigido entre V1.3 (hoje cedo) e V5.0 (este audit). E esperado — a versao V1.3 saiu hoje.
>
> 4. **15 novos problemas (DM071-DM085).** Concentracao em:
>    - **API/PSP (4 novas):** DM073-DM075 evidenciam que `AsaasGateway` + 3 services PSP precisam de refactor estrutural antes da Fase 4 do PSP entrar em producao.
>    - **Cross-cutting (4 novas):** DM079, DM082, DM083, DM085 — sujeira commitada + duplicacoes nao mapeadas.
>    - **Web (3 novas):** DM078 (Fretes.jsx), DM080 (crudFactory), DM081 (cadastros.js incompleto).
>    - **App (2 novas):** DM071 (api.js misturado), DM084 (calculo PIX 3x duplicado).
>    - **Desktop (2 novas):** DM076 (SyncClient god util), DM077 (RelatorioUtil grande).
>
> 5. **Padroes recorrentes detectados:**
>    - **God class continua spreading:** SyncClient.java (1163L) e RelatorioUtil.java (807L) na pasta `gui/util/` — categoria nova de god class fora dos controllers.
>    - **Triplicacao PSP-related:** Encomenda/Frete/Passagem tem 3 services Java + 3 telas app + 3 entries no AsaasGateway. Sem `PagamentoProcessor` central, qualquer feature nova entra em 9 lugares.
>    - **Duplicacao de entidade nao mapeada antes:** `rotas` tem 3 implementations (RotaController, OpRotaController, BFF cadastros.js) — DM085 estende o que #713 viu como 2.
>    - **`docs/audits/temp/` commitado**: skill `audit-2-scan` continua escrevendo la mas .gitignore nao cobre.
>
> 6. **Top 5 prioridades para esta categoria:**
>    1. **#500 + #501 + #502** (bundle PSP) — refatorar `PagamentoProcessor` antes da Fase 4 PSP. Aborta 9 pontos de duplicacao.
>    2. **#DM082** (1h) — sair do .gitignore lag: `log_erros.txt` e `db.properties.bak2` precisam sair YA.
>    3. **#DM075 + #300 do AUDIT** (1h, mesma fix) — RestTemplate com timeout. Resolve resilience tambem.
>    4. **#DM078** (Fretes.jsx) + **#509** (5 pages) — semana inteira mas zera dores cronicas do frontend.
>    5. **#506 + #507** (god controllers desktop) — projeto grande mas desbloqueia testes.

---
*Gerado por Claude Code (Deep Audit V5.0) — Revisao humana obrigatoria*
