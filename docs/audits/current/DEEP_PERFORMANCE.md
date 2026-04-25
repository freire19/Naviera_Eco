# AUDITORIA PROFUNDA — PERFORMANCE — Naviera_Eco
> **Versao:** V5.0
> **Data:** 2026-04-18
> **Categoria:** Performance
> **Base:** AUDIT_V1.3
> **Arquivos analisados:** 240+ (Desktop + API Spring + BFF + Web + App Mobile + OCR helpers)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Issues AUDIT_V1.3 confirmadas (secao 2.5) | 33 |
| Issues anteriores (V4.0) resolvidas | 30 |
| Issues anteriores (V4.0) ainda pendentes | 2 |
| Novos problemas encontrados neste deep | 23 |
| **Total de issues ativas de performance** | ~~58~~ → **55** (3 CRITICOs conferidos em 2026-04-23) |

Distribuicao por severidade das 55 ativas:

| Severidade | Qtd |
|------------|-----|
| CRITICO | ~~3~~ → **0** _(conferidos em 2026-04-23)_ |
| ALTO | 17 |
| MEDIO | 28 |
| BAIXO | 10 |

> **2026-04-23** — conferidos os 3 CRITICOs (#403, #411, #DP071). **TODOS JA ESTAVAM CORRIGIDOS NO CODIGO** — commit `06f2460 perf(web): dashboard financeiro agrega no Postgres em vez de JS` resolveu #403 + #DP071; #411 foi resolvido junto com #205/#DB203/#308 (3 services usam `tx.execute()` programatico). Resta 0 CRITICO em DEEP_PERFORMANCE.

---

## ISSUES ANTERIORES — STATUS

### Resolvidas (V4.0 → hoje)

Todas as issues DP034-DP062 e carryovers foram verificadas e mantem-se corrigidas. Nao ha regressoes. Lista-sintese:

| Issue | Titulo | Verificacao |
|-------|--------|-------------|
| #DP034 | DB queries no FX thread (BalancoViagem) | RESOLVIDO — bg thread mantida |
| #DP035 | DB queries no FX thread (TelaPrincipal) | RESOLVIDO |
| #DP036 | EmpresaDAO no FX (EncomendaPrintHelper) | RESOLVIDO |
| #DP037 | Impressao multi-pagina FX (6 controllers) | RESOLVIDO |
| #DP038 | PassageiroDAO cache preload | RESOLVIDO |
| #DP039 | AgendaDAO LIMIT | RESOLVIDO — LIMIT 500 |
| #DP040 | AuxiliaresDAO cache por tenant | RESOLVIDO |
| #DP041 | FuncionarioDAO UNION | RESOLVIDO |
| #DP042 | SELECT * em 6 DAOs Desktop | RESOLVIDO |
| #DP043 | PooledConnection AtomicBoolean | RESOLVIDO |
| #DP044-#DP048 | NumberFormat/DateTimeFormatter hot path (5) | RESOLVIDO |
| #DP049 | Logo sem ImageCache | RESOLVIDO |
| #DP050 | Cascata de threads BalancoViagem | RESOLVIDO |
| #DP051 | Thread sem daemon | RESOLVIDO |
| #DP052 | Queries BFF sem LIMIT (parcial) | REGREDIU — ver novos abaixo |
| #DP053 | Admin N+1 | RESOLVIDO |
| #DP054 | Itens em loop sem batch | RESOLVIDO |
| #DP055 | Boleto 240 INSERTs | RESOLVIDO |
| #DP056 | Estorno merge+sort JS | ACEITAVEL |
| #DP057 | Agenda EXTRACT range | RESOLVIDO |
| #DP058 | existsSync OCR foto | RESOLVIDO |
| #DP059-#DP060 | Overpayment guard | RESOLVIDO (DS4-011) |
| #DP061 | Indices compostos | RESOLVIDO (script 024) |
| #DP062 | React.lazy 34 paginas | RESOLVIDO |
| #079, #088, #092, #093 | N+1/LIMIT Desktop | RESOLVIDO |
| #086 | ViagemDAO LIMIT | RESOLVIDO |
| #091 | Site monolitico | ACEITAVEL |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|-------------|
| #DP033 | equals/hashCode models | Fixado em 4 classes (ApiConfig, Empresa, Funcionario, ItemFrete). 11 classes ainda sem: DadosBalancoViagem, Despesa, FreteDevedor, FreteItem, ItemResumoBalanco, LinhaDespesaDetalhada, PassagemFinanceiro, ReciboAvulso, ReciboQuitacaoPassageiro, EncomendaFinanceiro, FreteFinanceiro |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #DP023 | JARs duplicados (~35MB lib/) | Infra — sem mudanca |

---

## ISSUES DO AUDIT_V1.3 (SECAO 2.5 — CONFIRMADAS VIA DEEP DIVE)

Todas as 33 issues listadas na secao 2.5 do AUDIT_V1.3 foram re-verificadas linha-por-linha e confirmadas. Nao duplico aqui — referenciar diretamente o AUDIT_V1.3 para descricao e fix. Consolidando por severidade para o plano:

- **CRITICO (3):** #403, #411 (PSP/@Transactional), #400 (se release expandir base)
- **ALTO (14):** #400, #401, #402, #404, #405, #406, #407, #409, #412, #413, #415, #429, #431, #703
- **MEDIO (13):** #408, #410, #414, #416, #417, #418, #419, #420, #421, #422, #424, #425, #430, #700
- **BAIXO (3):** #427, #428, #701, #702

---

## NOVOS PROBLEMAS (descobertos neste deep)

### API SPRING — SERVICES

#### Issue #DP063 — SyncService.processar loop N+1 — processarRegistro por registro
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/SyncService.java`
- **Linha(s):** 103-115
- **Problema:** Para cada SyncRegistro recebido, `processarRegistro(tabela, reg, empresaId)` executa 1 SELECT por UUID + 1 INSERT/UPDATE. Desktop envia batches de ate 1000 registros (LIMIT 1000 em buscarParaDownload). 1000 registros = 2000 queries sequenciais dentro de `@Transactional`.
- **Impacto:** Sync do Desktop mais lenta que o necessario; lock de transacao prolongado; se pool=10, 1 sync ocupa 1 conn por minutos.
- **Codigo problematico:**
```java
for (SyncRegistro reg : registros) {
    if (reg.uuid() == null || reg.uuid().isBlank()) continue;
    try {
        uuidsRecebidos.add(reg.uuid());
        processarRegistro(tabela, reg, empresaId); // 1-2 queries dentro
        recebidos++;
    } catch (Exception e) { ... }
}
```
- **Fix sugerido:** (1) Batch SELECT dos UUIDs existentes em uma query; (2) Classificar registros em INSERT-novos vs UPDATE-existentes; (3) Usar `jdbc.batchUpdate(...)` para cada grupo. Reduz para 3 queries no total.
- **Observacoes:**
> _Sync e hot path — executa a cada 30s em cada Desktop conectado._

---

#### Issue #DP064 — GpsService.historicoViagem: SELECT sem LIMIT, sem filtro empresa_id
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/GpsService.java`
- **Linha(s):** 45-51
- **Problema:** `SELECT ... FROM embarcacao_gps WHERE id_viagem = ? ORDER BY timestamp` sem LIMIT e sem `AND empresa_id = ?`. Viagem ativa com GPS ligado 24h coleta ~8640 pontos/dia. Viagem de 5 dias = 43200 pontos. Payload JSON 1-3 MB.
- **Impacto:** Timeout no mobile sob 3G; memoria alta no Node BFF ao proxiar.
- **Fix sugerido:**
```java
return jdbc.queryForList("""
    SELECT latitude, longitude, velocidade_nos, curso_graus, timestamp
    FROM embarcacao_gps
    WHERE id_viagem = ?
      AND empresa_id = ?        -- DEFESA MULTI-TENANT
    ORDER BY timestamp
    LIMIT 5000                  -- cap razoavel
    """, idViagem, empresaId);
```
- **Observacoes:**
> _Considerar tambem downsampling (1 ponto a cada X min) no lado cliente._

---

#### Issue #DP065 — GpsService.todasUltimasPosicoes: cross-tenant sem cache nem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/GpsService.java`
- **Linha(s):** 54-62
- **Problema:** `DISTINCT ON (id_embarcacao)` sem LIMIT e sem cache. Endpoint do mapa publico de tracking — pode ser chamado a cada segundo por clientes. DISTINCT ON em tabela grande (`embarcacao_gps`) sem indice `(id_embarcacao, timestamp DESC)` vira seq scan.
- **Impacto:** DB load proporcional ao numero de conexoes no mapa publico.
- **Fix sugerido:** (1) `@Cacheable(value="gps-atual", key="'all'")` com TTL de 30s; (2) Indice `CREATE INDEX idx_gps_emb_ts_desc ON embarcacao_gps(id_embarcacao, timestamp DESC);`.
- **Observacoes:**
> _Ligado ao #DP064 — mesma tabela, mesmo gap de indice._

---

#### Issue #DP066 — FinanceiroService.balanco: 4 queries sequenciais por viagem
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FinanceiroService.java`
- **Linha(s):** 38-65
- **Problema:** Dashboard de balanco faz 4 `queryForObject` sequenciais (passagens, encomendas, fretes, despesas). Cada roundtrip ~2-5ms. Total 10-20ms para 1 balanco — 4x o necessario.
- **Impacto:** Dashboard financeiro mais lento; ocupa 4 conexoes do pool sequencialmente.
- **Fix sugerido:**
```java
Map<String, Object> row = jdbc.queryForMap("""
    SELECT
      (SELECT COALESCE(SUM(valor_pago), 0) FROM passagens WHERE id_viagem = ? AND empresa_id = ?) AS passagens,
      (SELECT COALESCE(SUM(valor_pago), 0) FROM encomendas WHERE id_viagem = ? AND empresa_id = ?) AS encomendas,
      (SELECT COALESCE(SUM(valor_pago), 0) FROM fretes WHERE id_viagem = ? AND empresa_id = ?) AS fretes,
      (SELECT COALESCE(SUM(valor), 0) FROM financeiro_saidas WHERE id_viagem = ? AND excluido = FALSE AND empresa_id = ?) AS despesas
    """, viagemId, empresaId, viagemId, empresaId, viagemId, empresaId, viagemId, empresaId);
```
- **Observacoes:**
> _Mesmo padrao corrigido em #408 (DashboardService) — replicar aqui._

---

#### Issue #DP067 — FinanceiroService.listarEntradas/listarSaidas: SELECT * sem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FinanceiroService.java`
- **Linha(s):** 18-36
- **Problema:** `SELECT * FROM financeiro_entradas/saidas WHERE empresa_id = ? ORDER BY data DESC` sem LIMIT. Tenant com 2 anos de dados pode ter 10k+ linhas de cada tabela. SELECT * sem projecao.
- **Impacto:** Payload crescente ao longo do tempo; pode estourar timeout.
- **Fix sugerido:** Projecao explicita + paginacao (`LIMIT ? OFFSET ?`). Ou filtro obrigatorio por periodo.
- **Observacoes:**
> _Alinhado com #418 (crudFactory) mas estes endpoints nao usam crudFactory._

---

#### Issue #DP068 — OnboardingService.gerarSlug: while loop com query por tentativa
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OnboardingService.java`
- **Linha(s):** 66-71
- **Problema:** `while (!jdbc.queryForList("SELECT 1 FROM empresas WHERE slug = ?", slug).isEmpty())` — 1 roundtrip por tentativa. Race condition: se 2 operadores se cadastrarem com nome similar simultaneamente, ambos passam no check e um falha no INSERT (se UNIQUE) ou cria duplicata (se nao).
- **Impacto:** Onboarding mais lento; possivel inconsistencia em pico.
- **Fix sugerido:**
```sql
ALTER TABLE empresas ADD CONSTRAINT empresas_slug_key UNIQUE (slug);
```
```java
// Tentar INSERT direto; em conflito, append sufixo aleatorio e retentar
try {
    insertEmpresa(slug, ...);
} catch (DuplicateKeyException e) {
    insertEmpresa(slug + "-" + RandomStringUtils.random(4), ...);
}
```
- **Observacoes:**
> _Mesmo risco em AdminService.gerarCodigoAtivacaoUnico (linha 45-52) — mas com cap de 10 tentativas._

---

#### Issue #DP069 — SELECT * em services de listagem do operador
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** Multiplos
- **Linha(s):**
  - `OpEncomendaService.java`: 19, 23
  - `OpFreteService.java`: 19, 23
  - `OpViagemService.java`: 18-25
  - `CadastrosService.java`: listarTarifas linha 31-37
  - `SyncService.java`: buscarParaDownload linha 331
- **Problema:** `SELECT *` ou `SELECT t.*` em endpoints de listagem. Tabelas com colunas grandes (descricao, observacoes, dados_extraidos JSON) transmitem payload desnecessario.
- **Impacto:** Payloads 2-3x maiores que o necessario.
- **Fix sugerido:** Projecao explicita por endpoint.
- **Observacoes:**
> _Expansao de #701 (OpEmbarcacaoController) — mesmo padrao generalizado._

---

#### Issue #DP070 — AuthService.login: @Transactional desnecessario
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthService.java`
- **Linha(s):** 23-33
- **Problema:** Login faz 1 SELECT + 1 UPDATE (`ultimo_acesso`). Metodo marcado com `@Transactional`. Overhead de transacao para operacao idempotente sem requisito de ACID.
- **Impacto:** ~0.5ms extra por login. Menor.
- **Fix sugerido:** Remover `@Transactional` — UPDATE de ultimo_acesso e fire-and-forget.
- **Observacoes:**
> _Consistencia com padrao de read-heavy services._

---

### BFF — ROUTES + HELPERS

#### Issue #DP071 — Financeiro /dashboard: filtros categoria/forma_pagto em JS pos-UNION
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: `financeiro.js:95-148` — commit `06f2460 perf(web): dashboard financeiro agrega no Postgres em vez de JS`. Filtros `categoria`, `forma_pagto`, `caixa`, `viagem_id` estao no SQL (`catFilter` + `outerWhere`); endpoint retorna apenas agregados (SUM/COUNT), sem `.filter()` pos-query em JS.

- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 100-147
- **Problema:** Extensao do #403. Mesmo apos fix do `viagem_id` obrigatorio, os filtros `categoria` e `forma_pagto` sao aplicados com `.filter()` em JS sobre o dataset ja trafegado. Uma viagem pequena tem ~500 linhas; com filtro forma_pagto='PIX', retornamos as 500 e filtramos 50.
- **Impacto:** Payload 10x maior que necessario; CPU de node wasted.
- **Fix sugerido:** Mover filtros para o WHERE de cada subquery do UNION:
```sql
WHERE e.empresa_id = $1 AND e.id_viagem = $2
  ${categoria === 'ENCOMENDA' || categoria === 'Todas' ? '' : 'AND false'}
  ${forma_pagto && forma_pagto !== 'Todas' ? `AND e.forma_pagamento = $${n}` : ''}
```
- **Observacoes:**
> _Ampliacao do #403 — completa o fix._

---

#### Issue #DP072 — ListaFretes.jsx: Promise.all de /itens sem concurrency limit
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/src/pages/ListaFretes.jsx`
- **Linha(s):** 72-86
- **Problema:** `Promise.all(idsParaCarregar.map(id => api.get(/fretes/${id}/itens)))` sem limite. Usuario com 500 fretes filtrados dispara 500 requests paralelos. BFF e DB ficam sob burst de queries.
- **Impacto:** Rate limit hits; pool DB saturado; UI trava por 5-30s.
- **Fix sugerido:**
```jsx
import pLimit from 'p-limit'
const limit = pLimit(5) // 5 requests paralelos max
const results = await Promise.all(
  idsParaCarregar.map(id => limit(() => api.get(`/fretes/${id}/itens`)...))
)
```
Melhor ainda: endpoint batch `POST /fretes/itens?ids=1,2,3` que retorna `{1:[...], 2:[...], 3:[...]}`.
- **Observacoes:**
> _Pattern tipico de "busca por item" lazy-load — precisa throttle._

---

#### Issue #DP073 — ocr.js upload storage: existsSync + mkdirSync no request path
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 52-55
- **Problema:** `multer.diskStorage.destination` chama `existsSync(dir)` + `mkdirSync(dir, {recursive:true})` em cada upload. fs sincrono bloqueia event loop por milissegundos — sob upload concorrente, agrava contention.
- **Impacto:** Latencia perceptivel em uploads concorrentes de OCR (ocr.js e frequentemente usado).
- **Fix sugerido:** Pre-criar o diretorio root uma vez na inicializacao do modulo; usar `fs.promises.mkdir({recursive:true}).catch(()=>{})` dentro de `destination`.
```js
// topo do arquivo
await fs.promises.mkdir(UPLOAD_PATH, { recursive: true }).catch(() => {})

const storage = multer.diskStorage({
  destination: async (req, file, cb) => {
    const dir = path.join(UPLOAD_PATH, String(req.user.empresa_id))
    await fs.promises.mkdir(dir, { recursive: true }).catch(() => {})
    cb(null, dir)
  },
  ...
})
```
- **Observacoes:**
> _Similar ao #428 scan mas na rota principal de OCR, maior frequencia._

---

#### Issue #DP074 — documentos.js /upload: existsSync sincrono no request path
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/documentos.js`
- **Linha(s):** 4 (importar existsSync), destination handler
- **Problema:** Mesma pattern do #DP073 mas em rota de upload de documentos.
- **Impacto:** Uploads de documentos sao menos frequentes, impacto menor.
- **Fix sugerido:** Identico ao #DP073.
- **Observacoes:**
> _Pair com #DP073._

---

#### Issue #DP075 — criarFrete.js: dual fallback com SAVEPOINT adiciona 2 queries
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/helpers/criarFrete.js`
- **Linha(s):** 22-49
- **Problema:** `SAVEPOINT` + `pg_advisory_xact_lock` + fallback MAX query para numero_frete. Se sequence nao estiver inicializado, faz 2 queries extras + 1 savepoint.
- **Impacto:** Minimo na pratica (sequence esta sempre inicializado apos migration).
- **Fix sugerido:** Simplificar: `SELECT nextval('seq_numero_frete')` unica query; garantir em migration que sequence e criada antes do primeiro INSERT.
- **Observacoes:**
> _Fallback e defensivo — avaliar se realmente necessario._

---

### WEB FRONTEND (React)

#### Issue #DP076 — Layout.jsx: useEffect ESC handler sem cleanup
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/components/Layout.jsx`
- **Linha(s):** 140-150
- **Problema:** `useEffect(() => { document.addEventListener('keydown', handler) })` sem `return () => document.removeEventListener(...)`. Cada re-render do Layout adiciona um listener — acumulam indefinidamente.
- **Impacto:** Memory leak progressivo + ESC dispara handler N vezes.
- **Fix sugerido:**
```jsx
useEffect(() => {
  const handler = (e) => { if (e.key === 'Escape') ... }
  document.addEventListener('keydown', handler)
  return () => document.removeEventListener('keydown', handler)
}, [/* deps */])
```
- **Observacoes:**
> _Classico bug de lifecycle em React._

---

#### Issue #DP077 — ListaFretes/RelatorioFretes: filter + reduce sem useMemo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** Multiplos
- **Linha(s):**
  - `naviera-web/src/pages/ListaFretes.jsx`: 89-114 (filter), 116-119 (4 reduces)
  - `naviera-web/src/pages/RelatorioFretes.jsx`: 115-118 (3 reduces)
- **Problema:** Filtros e agregacoes executados em cada render. Para 500 fretes, cada digitacao em campo de busca re-executa ~2000 operacoes.
- **Impacto:** Input lag perceptivel com >200 items.
- **Fix sugerido:**
```jsx
const filtrados = useMemo(() => fretes.filter(...), [fretes, filtroNumero, filtroRemetente, ...])
const totais = useMemo(() => {
  return filtrados.reduce((acc, f) => ({
    totalLancado: acc.totalLancado + (parseFloat(f.valor_total_itens || 0)),
    totalRecebido: acc.totalRecebido + (parseFloat(f.valor_pago || 0)),
    totalVolumes: acc.totalVolumes + (parseInt(f.total_volumes) || 0),
  }), {totalLancado:0, totalRecebido:0, totalVolumes:0})
}, [filtrados])
```
- **Observacoes:**
> _Expansao do padrao #413 e #414 (app mobile) para web._

---

#### Issue #DP078 — Boletos.jsx: loop await de POST parcelas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/src/pages/Boletos.jsx`
- **Linha(s):** ~14 (funcao gerar)
- **Problema:** `for (let i=0; i<qtdParcelas; i++) { await api.post(...) }` — serializa POSTs de parcelas. 12 parcelas = 12 roundtrips sequenciais.
- **Impacto:** Geracao de carne anual demora 2-5s; UX ruim.
- **Fix sugerido:** Usar o endpoint batch ja existente (`POST /financeiro/boleto/batch` — ver #DP055 fix) em 1 chamada.
- **Observacoes:**
> _Endpoint batch ja existe no BFF; frontend nao esta usando._

---

#### Issue #DP079 — Agenda.jsx: forEach montando objeto contagem sem useMemo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/src/pages/Agenda.jsx`
- **Linha(s):** ~72
- **Problema:** `tarefas.forEach(...)` construindo `contagem{}` a cada render. Se tarefas tem 300 items e agenda re-renderiza com frequencia, desperdicio.
- **Impacto:** Baixo.
- **Fix sugerido:** `const contagem = useMemo(() => { ... }, [tarefas])`.
- **Observacoes:**
> _Quick win._

---

#### Issue #DP080 — App.jsx: localStorage+JSON.parse em useState initializer sem try/catch
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/src/App.jsx`
- **Linha(s):** 12-25
- **Problema:** `useState(() => JSON.parse(localStorage.getItem('xxx')))` sem try/catch. Se localStorage corrompido, app quebra no boot.
- **Impacto:** Bug latente raro.
- **Fix sugerido:**
```jsx
const initial = (() => { try { return JSON.parse(localStorage.getItem(k)) ?? fallback } catch { return fallback } })()
```
- **Observacoes:**
> _Resiliencia + correcao de perf (evita boot re-tentativa)._

---

#### Issue #DP081 — Financeiro.jsx: new Date().toLocaleDateString no render
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/src/pages/Financeiro.jsx`
- **Linha(s):** ~93
- **Problema:** `new Date().toLocaleDateString('pt-BR')` avaliado a cada render. Cria Date object + locale lookup desnecessariamente.
- **Impacto:** Minimo.
- **Fix sugerido:** `useMemo(() => new Date().toLocaleDateString('pt-BR'), [])` ou constante fora do componente.
- **Observacoes:**
> _Minimo._

---

### APP MOBILE

#### Issue #DP082 — FinanceiroCNPJ.jsx: array opts literal dentro do render
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/screens/FinanceiroCNPJ.jsx`
- **Linha(s):** 83-92
- **Problema:** `const opts = [{...}, {...}]` dentro do corpo do componente — nova referencia a cada render. Componentes filhos que recebem `opts` como prop quebram `React.memo` por mudanca de referencia.
- **Impacto:** Re-renders desnecessarios de filhos.
- **Fix sugerido:** Mover `PAYMENT_OPTS` para fora do componente como constante modular.
- **Observacoes:**
> _Padrao recorrente em componentes React — mover arrays/objetos literais estaticos para fora._

---

#### Issue #DP083 — Header.jsx: `<img>` sem loading=lazy nem thumbnail
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/components/Header.jsx`
- **Linha(s):** ~17
- **Problema:** `<img src={foto}/>` sem `loading="lazy"`. Mobile em listas com avatares baixa imagens fora do viewport.
- **Impacto:** Dados moveis desperdicados; LCP pior.
- **Fix sugerido:** `<img loading="lazy" src={foto}/>` + backend servir thumbnail (100x100) em endpoint separado.
- **Observacoes:**
> _Padrao pode existir em outros 2-3 componentes — scan opcional._

---

### DESKTOP JAVAFX

#### Issue #DP084 — CadastroFreteController: Files.readAllBytes no FX thread
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1795
- **Problema:** Handler `handleCodXml` le XML da NF-e com `Files.readAllBytes(file.toPath())` no FX Application Thread. NF-e pode ter >500KB. File I/O sincrono bloqueia a UI.
- **Impacto:** Freeze de 200-800ms ao importar XML.
- **Codigo problematico:**
```java
String conteudoXml = new String(
    java.nio.file.Files.readAllBytes(file.toPath()),
    java.nio.charset.StandardCharsets.UTF_8);
```
- **Fix sugerido:** Envolver em Task, parsear em bg thread, `Platform.runLater` para popular campos.
```java
new Thread(() -> {
    try {
        String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String numNota = extrairTagXml(xml, "nNF");
        ...
        Platform.runLater(() -> {
            if (!numNota.isEmpty()) txtNumNota.setText(numNota);
            ...
        });
    } catch (IOException e) { /* alert no FX */ }
}, "xml-parse").start();
```
- **Observacoes:**
> _Padrao ja usado em outros controllers Desktop._

---

#### Issue #DP085 — ConfigurarApiController: FileInputStream em getters estaticos sem cache
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 352-386
- **Problema:** `getUrlApi()`, `getToken()`, `getPastaArquivos()` — 3 metodos estaticos que abrem `FileInputStream(CONFIG_FILE)` + `Properties.load()` a cada chamada. Nenhum cache. Cada chamada do `SyncClient` ou `ApiClient` re-le o arquivo.
- **Impacto:** File I/O por cada request HTTP saindo do Desktop. Impacto acumula em sync continuo.
- **Fix sugerido:**
```java
private static volatile Properties cachedProps = null;
private static Properties loadProps() {
    Properties p = cachedProps;
    if (p != null) return p;
    synchronized (ConfigurarApiController.class) {
        if (cachedProps != null) return cachedProps;
        Properties np = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            np.load(fis);
        } catch (IOException ignored) {}
        cachedProps = np;
        return np;
    }
}
public static String getUrlApi() {
    return loadProps().getProperty("url.api", DEFAULT_URL);
}
// adicionar metodo invalidateCache() chamado apos salvar
```
- **Observacoes:**
> _Padrao singleton com double-checked locking._

---

#### Issue #DP086 — ListaFretesController: clear() + loop .add() em ObservableList
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ListaFretesController.java`
- **Linha(s):** 428-429
- **Problema:** `listaCompletaFretes.clear()` seguido de `for (Frete f : fretesDoBanco) listaCompletaFretes.add(f)`. Cada `.add` dispara change listeners da TableView — N updates de UI.
- **Impacto:** Flicker + render lento ao recarregar lista.
- **Fix sugerido:** `listaCompletaFretes.setAll(fretesDoBanco)` — 1 notificacao apenas.
- **Observacoes:**
> _Mesmo padrao ja corrigido em ListaEncomendaController (#DP031) — faltou aqui._

---

#### Issue #DP087 — VenderPassagemController: 15+ addListener sem remove no destruct
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 239, 374, 386, 438, 899, 1055, 1077, 1089, 1142, 1280, 1303, 1323, 1404, 1445, 1447
- **Problema:** 15+ `.addListener(...)` em `initialize()`. Se controller for aberto/fechado varias vezes na mesma sessao, listeners acumulam (memory leak + eventos disparando multiplas vezes). Linha 1280 em loop `List.of(...).forEach(field -> field.textProperty().addListener(...))`.
- **Impacto:** Memory growth + UI glitches sob uso prolongado.
- **Fix sugerido:**
  - Opcao 1: Usar `WeakChangeListener` para auto-cleanup.
  - Opcao 2: Armazenar as `ChangeListener` como campos e remover em `handleClose()` / `onCloseRequest`.
  - Opcao 3: Garantir que o controller so e instanciado 1x via FXML loader com cache.
- **Observacoes:**
> _VenderPassagem e a tela mais usada — efeito mais visivel._

---

#### Issue #DP088 — ExtratoPassageiroController: clear() + loop .add() em ObservableList
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** 176-177
- **Problema:** Mesmo padrao do #DP086.
- **Fix sugerido:** `todosNomesPassageiros.setAll(lista.stream().map(Passageiro::getNome).toList())`.
- **Observacoes:**
> _Quick win._

---

## COBERTURA

| Diretorio | Arquivos | Issues ativas (V5.0) |
|-----------|----------|---------------------|
| src/dao/ | 13 | 0 |
| src/database/ | 2 | 0 |
| src/gui/ | 43 | 5 (#DP084-#DP088) |
| src/gui/util/ | 16 | 0 |
| src/model/ | 25 | 1 (#DP033 parcial) |
| naviera-api/service/ | 31 | 20 (AUDIT#400-#430 + #700-#702 + #DP063-#DP070) |
| naviera-api/config/ | 8 | 2 (#419, #420) |
| naviera-api/controller/ | 28 | 1 (#701) |
| naviera-web/server/routes/ | 15 | 11 (AUDIT#403,#416,#418,#421,#425,#703 + #DP071-#DP075) |
| naviera-web/server/helpers/ | 8 | 2 (#415, #431) |
| naviera-web/src/ | 38 | 7 (#413, #425 + #DP076-#DP081) |
| naviera-app/src/ | 31 | 5 (#412, #413, #414 + #DP082, #DP083) |
| naviera-ocr/ | 16 | 0 |
| naviera-site/ | 2 | 1 (#091 aceitavel) |
| database_scripts/ | 23 | 2 (#429 trigram, #430 indice) |
| lib/ | N/A | 1 (#DP023 infra) |
| **TOTAL** | **300+** | **58** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO) — 3 issues — **CONCLUIDA (2026-04-23)**

- [x] #403 — Financeiro /dashboard UNION ALL _(agrega no Postgres com SUM/COUNT; commit `06f2460`; conferido 2026-04-23)_
- [x] #411 — PSP chamado dentro de @Transactional _(3 services usam `tx.execute()` programatico; mesmo fix de #205/#DB203/#308)_
- [x] #DP071 — Filtros categoria/forma_pagto no SQL _(catFilter + outerWhere em `financeiro.js:109-115`; conferido 2026-04-23)_
- **Notas:**
> Todos os 3 CRITICOs ja estavam corrigidos no codigo (inclusive `06f2460 perf(web): dashboard financeiro agrega no Postgres em vez de JS`). Nenhum arquivo de codigo modificado nesta sessao.

### Importante (ALTO) — 17 issues

- [ ] #400 — rastreioCrossTenant LIKE sem LIMIT — **Esforco:** 1h (indices + LIMIT)
- [ ] #401 — viagens/buscarPublicas sem LIMIT/cache — **Esforco:** 30min
- [ ] #402 — Pool conexoes subdimensionado — **Esforco:** 2h (pgbouncer recomendado)
- [ ] #404 — FreteService buscarPorRemetenteCrossTenant LIKE — **Esforco:** 1h
- [ ] #405 — AmigoService NOT IN subquery — **Esforco:** 30min (indices)
- [ ] #406 — LojaService.stats N+1 toPedidoDTO — **Esforco:** 1h
- [ ] #407 — EmbarcacaoService LATERAL JOIN — **Esforco:** 30min (indice)
- [ ] #409 — PassagemService.minhasPassagens sem limite — **Esforco:** 30min
- [ ] #412 — QR PIX base64 em JSON — **Esforco:** 2h (gerar no cliente)
- [ ] #413 — PassagensCPF.jsx sem useMemo — **Esforco:** 30min
- [ ] #415 — OCR base64 em heap — **Esforco:** 1h (streaming)
- [ ] #429 — Indices trigram faltantes — **Esforco:** 1h (migration)
- [ ] #431 — OCR upload serial — **Esforco:** 1h (p-limit)
- [ ] #703 — /dashboard F5 rapido — **Esforco:** 30min (dedup middleware)
- [ ] #DP063 — SyncService.processar loop N+1 — **Esforco:** 3h (batch refactor)
- [ ] #DP064 — GpsService.historicoViagem sem LIMIT/empresa_id — **Esforco:** 30min
- [ ] #DP072 — ListaFretes Promise.all sem limit — **Esforco:** 30min (p-limit ou endpoint batch)
- **Notas:**
> _#402 exige decisao de infra (pgbouncer vs pool resize)._

### Importante (MEDIO) — 28 issues

- [ ] #408 — DashboardService 3 queries sequenciais — **Esforco:** 30min
- [ ] #410 — BilheteService advisory lock por empresa — **Esforco:** 30min
- [ ] #414 — FinanceiroCNPJ.jsx sem useMemo — **Esforco:** 30min
- [ ] #416 — OCR aprovar INSERT sequencial — **Esforco:** 1h
- [ ] #417 — listarAvaliacoes sem LIMIT — **Esforco:** 15min
- [ ] #418 — crudFactory sem LIMIT default — **Esforco:** 30min (muda default)
- [ ] #419 — RateLimitFilter sem LRU — **Esforco:** 1h (Caffeine)
- [ ] #420 — TenantCache sem max size — **Esforco:** 30min
- [ ] #421 — viagens.delete cascade 6 DELETEs — **Esforco:** 1h (FK CASCADE)
- [ ] #422 — Encomenda/Frete pagar 3 queries metadata — **Esforco:** 1h
- [ ] #424 — PushService loop sincrono FCM — **Esforco:** 1h (multicast)
- [ ] #425 — useEffect sem cleanup (Dashboard) — **Esforco:** 15min
- [ ] #430 — Indice compound passageiros(documento,empresa) — **Esforco:** 15min
- [ ] #700 — confirmarEmbarque query pesada em tx — **Esforco:** 30min
- [ ] #DP065 — GpsService.todasUltimasPosicoes sem cache — **Esforco:** 1h
- [ ] #DP066 — FinanceiroService.balanco 4 queries — **Esforco:** 30min
- [ ] #DP067 — FinanceiroService listar sem LIMIT — **Esforco:** 30min
- [ ] #DP068 — OnboardingService gerarSlug loop — **Esforco:** 30min (UNIQUE + try/catch)
- [ ] #DP073 — OCR existsSync no request — **Esforco:** 30min
- [ ] #DP074 — documentos existsSync — **Esforco:** 15min
- [ ] #DP076 — Layout useEffect sem cleanup — **Esforco:** 15min
- [ ] #DP077 — Lista/Relatorio Fretes sem useMemo — **Esforco:** 30min
- [ ] #DP078 — Boletos.jsx loop await — **Esforco:** 30min (usar endpoint batch)
- [ ] #DP084 — CadastroFrete readAllBytes FX thread — **Esforco:** 30min
- [ ] #DP085 — ConfigurarApi getters sem cache — **Esforco:** 30min
- [ ] #DP086 — ListaFretes clear+add ObservableList — **Esforco:** 15min
- [ ] #DP087 — VenderPassagem listeners sem cleanup — **Esforco:** 2h (WeakListener refactor)
- [ ] #DP033 | equals/hashCode models (11 restantes) — **Esforco:** 1h

### Menor (BAIXO) — 10 issues

- [ ] #427 — console.log/System.err hot paths — **Esforco:** 1h (substituir por logger)
- [ ] #428 — LinkedHashMap pressure — **Esforco:** 2h (DTO records)
- [ ] #701 — SELECT * OpEmbarcacaoController — **Esforco:** 15min
- [ ] #702 — MAX(CAST()) numero_bilhete — **Esforco:** 30min (sequence)
- [ ] #DP023 — JARs duplicados — **Esforco:** 1h (audit lib/)
- [ ] #DP069 — SELECT * servicos operador — **Esforco:** 1h
- [ ] #DP070 — AuthService @Transactional desnecessario — **Esforco:** 15min
- [ ] #DP075 — criarFrete dual fallback — **Esforco:** 30min
- [ ] #DP079 — Agenda.jsx forEach sem useMemo — **Esforco:** 15min
- [ ] #DP080 — App.jsx localStorage sem try/catch — **Esforco:** 15min
- [ ] #DP081 — Financeiro.jsx new Date() render — **Esforco:** 15min
- [ ] #DP082 — FinanceiroCNPJ opts no render — **Esforco:** 15min
- [ ] #DP083 — Header.jsx img sem lazy — **Esforco:** 15min
- [ ] #DP088 — ExtratoPassageiro clear+add — **Esforco:** 15min

---

## NOTAS

> **Progresso V4.0 → V5.0:** V4.0 fechou 30+ issues em 2026-04-15. AUDIT_V1.3 (2026-04-18) trouxe 33 issues NOVAS de performance (secao 2.5) — a maioria concentrada na API Spring e no BFF, camadas que receberam mais codigo no sprint de PSP/onboarding. Deep dive adicionou 23 issues residuais.
>
> **Padroes recorrentes nesta V5.0:**
> 1. **Queries cross-tenant sem LIMIT** (#400, #401, #404, #409, #DP064) — pattern de endpoints publicos do app.
> 2. **HTTP sincrono dentro de @Transactional** (#411) — padrao critico que derruba API sob incidente do PSP.
> 3. **Loops com await/query sequencial** (#416, #421, #424, #DP063, #DP078) — oportunidade de batch.
> 4. **React sem useMemo** (#413, #414, #425, #DP076-#DP081) — migrado do padrao encontrado em V3/V4.
> 5. **fs sincrono no event loop** (#DP073, #DP074) — pattern recorrente em uploads.
> 6. **Indices compound/trigram faltantes** (#429, #430, #DP065) — cobertura parcial do script 024.
>
> **Quick wins (<30min cada, 10 issues):** #401, #405, #407, #409, #417, #430, #DP067, #DP076, #DP079, #DP080, #DP081, #DP088.
>
> **Criticos de infra:** #402 (pool/pgbouncer) e #411 (outbox PSP) sao os 2 unicos que exigem decisoes arquiteturais antes de codar.
>
> **Camadas mais limpas:** `src/dao/`, `src/gui/util/`, `naviera-ocr/` permanecem zeradas. `naviera-api/service/` concentra 20 das 58 issues (34%).
>
> **Carryover:** #DP023 (JARs) e #091 (site monolitico) seguem como aceitaveis/infra — sem acao recomendada no MVP.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*
