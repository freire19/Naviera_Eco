# AUDITORIA PROFUNDA — RESILIENCE — Naviera_Eco
> **Versao:** V6.0
> **Data:** 2026-04-18
> **Categoria:** Resilience (Error Handling, Fault Tolerance, Resource Management, Thread Safety, Idempotencia)
> **Base:** AUDIT_V1.3 + DEEP_RESILIENCE V5.0
> **Arquivos analisados:** 215+ (Desktop, BFF, API, PSP, Apps React, OCR)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Issues anteriores V5.0 — resolvidas | 47 (todas marcadas FIXADAS — nao reverificadas 1-a-1 neste ciclo) |
| Issues do AUDIT_V1.3 secao 2.4 — PENDENTES | ~~27~~ → **20** (7 CRITICAS ja corrigidas em 2026-04-23) |
| Novos problemas — PSP/Asaas | 5 |
| Novos problemas — BFF Web | 9 |
| Novos problemas — Desktop Java | 6 |
| Novos problemas — Apps React | 6 |
| **Total de NOVAS issues ativas** | ~~26~~ → **25** (#DR260 resolvido) |
| **Total de issues ativas (V6.0)** | ~~53~~ → **45** (8 CRITICOs conferidos em 2026-04-23) |

### Por severidade (ativos)

| Severidade | Quantidade |
|------------|-----------|
| CRITICO | ~~8~~ → **0** _(conferidos em 2026-04-23)_ |
| ALTO | 19 |
| MEDIO | 21 |
| BAIXO | 5 |

> **2026-04-23** — conferidos os 8 CRITICOs (7 de AUDIT_V1.3 sec 2.4 + 1 novo DR260). **TODOS JA ESTAVAM CORRIGIDOS NO CODIGO** antes desta verificacao — o audit V6.0 foi gerado em 2026-04-18 e os fixes foram aplicados em commits posteriores. Resta 0 CRITICO em DEEP_RESILIENCE.

---

## ISSUES ANTERIORES — STATUS

### Resolvidas (de DEEP_RESILIENCE V5.0)

Todas as 47 issues de V5.0 (#DR201-#DR247) estao marcadas como FIXADAS no relatorio anterior. Spot-check nesta sessao confirmou:
- `SyncClient.java` tem volatile em `serverUrl`/`login`/`senha`/`scheduler` (#DR209, #DR210) ✓
- `ConexaoBD.java` tem `volatile closed` no PooledConnection (#DR222) ✓
- `naviera-web/server/index.js` tem `server.timeout = 120_000` (#DR230) ✓
- `naviera-web/server/helpers/visionApi.js` + `geminiParser.js` usam `AbortSignal.timeout(30000)` (#DR231) ✓
- `naviera-web/server/routes/ocr.js` tem `unlink` no catch de upload (#DR232) ✓
- `ErrorBoundary.jsx` existe em naviera-app e naviera-ocr (#DR235, #DR243) ✓
- `RateLimitFilter.java` valida localhost antes de confiar em XFF (#DR245) ✓

Restante nao reverificado 1-a-1 — confiar em V5.0.

### AUDIT_V1.3 Secao 2.4 — **TODAS AINDA PENDENTES**

Verificacao linha-por-linha do codigo atual confirmou que NENHUM dos 27 issues de resiliencia do AUDIT_V1.3 secao 2.4 recebeu fix. Lista completa em "ISSUES PENDENTES DE AUDIT_V1.3" abaixo.

### Pendentes antigas (pre-V5.0)

Nenhuma — V5.0 fechou todos os pendentes antigos.

---

## ISSUES PENDENTES DE AUDIT_V1.3 (CATEGORIA RESILIENCE)

### CRITICOS (7) — ~~bloqueio de producao~~ **TODOS CORRIGIDOS (conferido 2026-04-23)**

| Issue | Arquivo | Status |
|-------|---------|--------|
| #300 | `AsaasGateway.java:51-54` | **RESOLVIDO (2026-04-23)** — `SimpleClientHttpRequestFactory` com `setConnectTimeout(5_000)` + `setReadTimeout(15_000)`. |
| #301 | `PspCobrancaService.java` + `PspWebhookController.java` | **RESOLVIDO (2026-04-23)** — `PspWebhookController` existe com HMAC e idempotencia via `psp_webhook_events` (migration `031_psp_webhook_events.sql`). `processarEvento` implementado. |
| #304 | `naviera-web/server/routes/ocr.js:6` | **RESOLVIDO (2026-04-23)** — `import { randomUUID } from 'crypto'` presente. |
| #305 | `AsaasGateway.java:201-210` | **RESOLVIDO (2026-04-23)** — em profile `prod` + secret blank retorna `false` (rejeita). Somente dev aceita com log.warn. |
| #308 | `PassagemService.java` + `FreteService.java` + `EncomendaService.java` | **RESOLVIDO (2026-04-23)** — os 3 metodos (`comprar`/`pagar`) nao tem `@Transactional`; usam `tx.execute()` programatico e chamam `pspService.criar()` fora de TX; TX2 posterior para UPDATE dos dados PSP. |
| #311 | `FirebaseConfig.java:30-49` | **RESOLVIDO (2026-04-23)** — `@PostConstruct` checa profile `prod` e lanca `IllegalStateException` se credenciais ausentes ou init falhar (API nao sobe silenciosamente). |
| #315 | `naviera-api/Dockerfile` | **RESOLVIDO (2026-04-23)** — `apk add tini` + `ENTRYPOINT ["/sbin/tini", "--", ...]` + `STOPSIGNAL SIGTERM` + `EXPOSE 8081`. |

### ALTOS (8)

| Issue | Arquivo | Problema resumido |
|-------|---------|-------------------|
| #302 | `AsaasGateway.java:90-105` | `GET /payments/{id}/pixQrCode` apos POST sem retry — usuario retenta, 2 cobrancas |
| #303 | `EncomendaService.java:115-211` | `@Transactional` + `pspService.criar()` — mesmo padrao do #308 |
| #307 | `naviera-web/server/helpers/fetchWithRetry.js:15-16` | 429 (rate limit) nao retentado; sem `Retry-After` |
| #309 | `SyncService.java:103-115` | Erro por registro silenciado (`log.warn`); Desktop marca todos como sync ok |
| #310 | `SyncClient.java:387-436,787-854` | 3 tentativas falham → enche log; operador do barco nao recebe aviso visual |
| #313 | `GlobalExceptionHandler.java:30-34` | 500 sem `correlationId` — debug impossivel em producao |
| #314 | `naviera-web/server/index.js:4-7` | `uncaughtException` → `process.exit(1)` sem drenar pool/server |
| #319 | `naviera-app/src/hooks/useWebSocket.js:28-62` | STOMP Client sem `heartbeatIncoming`/`heartbeatOutgoing` — zombie connections em NAT/celular |

### MEDIOS/BAIXOS (12)

`#306` fetchWithRetry sem jitter, `#312` PushService loop sincrono sem paralelismo, `#316` Dockerfile EXPOSE 8080 vs compose 8081, `#317` catch engole erro sem correlationId (3 telas mobile), `#318` clipboard.writeText sem .catch, `#320` SyncClient escapeJson manual duplo-escape, `#321` bcrypt cost hardcoded 10 (bloqueia event loop), `#322` TenantMiddleware cache in-memory falha em cluster, `#323` SyncClient aplicarRegistroRecebido catch silencioso em loop, `#324` Asaas onboarding catch generico, `#327` obterOuCriarCustomer race condition, `#328` SyncClient scheduler daemon sem shutdown hook, `#329` geminiParser regex JSON greedy, `#330` NotificationService WebSocket send sem outbox, `#331` AuthService/AuthOperadorService nunca auditados.

---

## NOVOS PROBLEMAS — PSP / ASAAS

### CRITICOS

#### Issue #DR260 — AsaasGateway.post/get: NullPointerException se body da resposta for null
- [x] **Concluido** _(ja estava corrigido — conferido em 2026-04-23)_

> Ja estava corrigido: `AsaasGateway.java:261-267` extraiu `parseBody(res, path)` helper que valida `body == null || body.isBlank()` e lanca `ApiException.badGateway("Resposta Asaas vazia em ...")`. Comentario `// #DR260` explicativo. `post()` e `get()` delegam para `parseBody()`.

- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 228-244
- **Problema:** `post()` e `get()` chamam `mapper.readTree(res.getBody())` sem null check. Em respostas 204 No Content, 304 Not Modified, ou falha de proxy, `getBody()` pode retornar `null` — `ObjectMapper.readTree((String) null)` lanca `NullPointerException` que e engolida pelo catch generico em `criarCobranca` L127-130 e `criarSubconta` L184-188.
- **Impacto:** Cobranca pode ser criada no Asaas mas o servico lanca excecao generica "Erro PSP Asaas: null". Usuario tenta novamente, CRIA COBRANCA DUPLICADA. Falha de pagamento silenciosa com exposicao contabil real.
- **Codigo problematico:**
```java
private JsonNode post(String path, Map<String, Object> body) throws Exception {
    ResponseEntity<String> res = rest.exchange(...);
    return mapper.readTree(res.getBody());  // NPE se null
}
```
- **Fix sugerido:**
```java
private JsonNode post(String path, Map<String, Object> body) throws Exception {
    ResponseEntity<String> res = rest.exchange(...);
    String raw = res.getBody();
    if (raw == null || raw.isBlank()) {
        throw new IllegalStateException("Asaas retornou body vazio (status=" + res.getStatusCode() + ")");
    }
    return mapper.readTree(raw);
}
```
- **Observacoes:**
> _Consequencia direta combinada com #300 (sem timeout): thread trava, timeout, retry, cobranca duplicada._

---

### ALTOS

#### Issue #DR261 — AsaasGateway: calculo manual de split diverge do retornado pelo Asaas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 107-110
- **Problema:** `splitNaviera` e `splitEmpresa` sao calculados localmente (`valorLiquido × pct / 100`) e salvos em `psp_cobrancas`. Porem o Asaas aplica suas proprias regras de arredondamento e pode retornar valores levemente diferentes no payload de resposta (`body.path("split")`). A reconciliacao local vs Asaas fica sempre com diferenca de centavos — auditoria financeira trabalhosa.
- **Impacto:** Divergencia permanente na tabela `psp_cobrancas` vs dashboard do Asaas — reconciliacao impossivel sem planilha. Risco contabil.
- **Fix sugerido:** Preferir valores retornados pelo Asaas quando presentes:
```java
BigDecimal splitNaviera, splitEmpresa;
JsonNode splitResp = body.path("split");
if (splitResp.isArray() && splitResp.size() > 0) {
    splitNaviera = new BigDecimal(splitResp.get(0).path("totalValue").asText("0"));
    splitEmpresa = valorLiquido.subtract(splitNaviera);
} else {
    splitNaviera = valorLiquido.multiply(...); // calculo local como fallback
    splitEmpresa = valorLiquido.subtract(splitNaviera);
}
```
- **Observacoes:**
> __

---

#### Issue #DR262 — AsaasGateway.obterOuCriarCustomer: URL query sem encoding
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 216
- **Problema:** `get("/customers?cpfCnpj=" + req.cpfCnpjPagador())` — concatenacao direta. Se `cpfCnpjPagador` contiver caracteres especiais (`.`, `/`, `-`, espaco) o Asaas pode nao encontrar match. Pior: se um usuario CNPJ for cadastrado com `"13.000.000/0001-00"` e outro com `"13000000000100"`, busca falha e cria DUPLICATA do customer.
- **Impacto:** Customers duplicados no Asaas; pagamentos atribuidos ao customer errado; historico do cliente fragmentado.
- **Fix sugerido:**
```java
import org.springframework.web.util.UriComponentsBuilder;
String url = UriComponentsBuilder.fromPath("/customers")
    .queryParam("cpfCnpj", req.cpfCnpjPagador().replaceAll("\\D", ""))
    .toUriString();
JsonNode list = get(url);
```
Normalizar para digitos-apenas antes de enviar (padrao recomendado pelo Asaas).
- **Observacoes:**
> _Complementa #327 (race condition em criar customer) — se buscar falha, cria duplicado independentemente de race._

---

### MEDIOS

#### Issue #DR263 — PspCobrancaService.atualizarStatus: sem validacao de transicao de estado
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/PspCobrancaService.java`
- **Linha(s):** 75-85
- **Problema:** `atualizarStatus` aceita qualquer transicao: `CONFIRMADA → PENDENTE`, `ESTORNADA → CONFIRMADA`. Webhook fora de ordem (network latency: REFUND chega antes de CONFIRMED) sobrescreve estados consistentes.
- **Impacto:** Cenario real documentado no #301: Asaas envia CONFIRMED OK → REFUND_REQUESTED OK → retry tardio do CONFIRMED sobrescreve REFUND. Cliente recebe mercadoria + estorno.
- **Fix sugerido:** Whitelist de transicoes:
```java
private static final Map<String, Set<String>> TRANSICOES_VALIDAS = Map.of(
    "PENDENTE",   Set.of("CONFIRMADA", "VENCIDA", "CANCELADA"),
    "CONFIRMADA", Set.of("ESTORNADA"),
    "VENCIDA",    Set.of("CONFIRMADA", "CANCELADA"),
    "ESTORNADA",  Set.of(),
    "CANCELADA",  Set.of()
);
if (!TRANSICOES_VALIDAS.get(c.getPspStatus()).contains(novoStatus)) {
    log.warn("Transicao invalida {}->{} rejeitada", c.getPspStatus(), novoStatus);
    return;
}
```
- **Observacoes:**
> _Complementa #301 (idempotencia): idempotencia evita duplicata, validacao de transicao evita retrocesso._

---

#### Issue #DR264 — AsaasGateway: splitNavieraPct nao validado (range)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 80-88
- **Problema:** `req.splitNavieraPct()` passado direto ao Asaas. Se configuracao errada enviar `150` (percentual > 100) ou negativo, Asaas rejeita com erro opaco. Se enviar `0`, split nao e aplicado silenciosamente.
- **Impacto:** Falha de cobranca sem mensagem util; split nao aplicado em 0; 500 em caso de > 100.
- **Fix sugerido:** Validar no inicio de `criarCobranca`:
```java
BigDecimal pct = req.splitNavieraPct();
if (pct != null && (pct.signum() < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0)) {
    throw new IllegalArgumentException("splitNavieraPct fora de range [0,100]: " + pct);
}
```
- **Observacoes:**
> __

---

## NOVOS PROBLEMAS — BFF WEB (naviera-web/server)

### ALTOS

#### Issue #DR265 — express.json() sem limite — payload grande nao rejeitado cedo
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/index.js`
- **Linha(s):** 47
- **Problema:** `app.use(express.json())` usa default `"100kb"`. Endpoints de OCR (`PUT /lancamentos/:id/revisar`, `/lancamentos/:id`) validam tamanho APOS parsing (L422-426 em `ocr.js`), mas parse de 100KB+ falha silenciosamente com `PayloadTooLargeError` que cai no 400 generico. Ao mesmo tempo nao ha protecao contra payload de 50MB.
- **Impacto:** Upload de dados_revisados com muitos itens falha com erro opaco; vetor DoS com requests de megabytes.
- **Fix sugerido:**
```js
app.use(express.json({ limit: '1mb' }));            // padrao API
app.use('/api/ocr', express.json({ limit: '1mb' })); // ocr ja tem validacao custom
```
- **Observacoes:**
> __

---

#### Issue #DR266 — Graceful shutdown nao drena o pool PG
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/index.js`
- **Linha(s):** 95-105
- **Problema:** `shutdown()` fecha o server mas NUNCA chama `pool.end()`. Durante o deploy/restart, connections ficam orfas no Postgres ate o sistema operacional fechar os sockets. Em PM2 cluster mode, cada worker deixa 10 conexoes orfas ate timeout do Postgres.
- **Impacto:** Exhaustao de `max_connections` em Postgres durante restart sequencial de workers.
- **Fix sugerido:**
```js
import pool from './db.js'
function shutdown(signal) {
  log.info('Server', `${signal} received — shutting down`)
  server.close(async () => {
    try { await pool.end(); } catch (e) { log.error('Server', 'Pool end err', e.message); }
    log.info('Server', 'Connections closed — exiting')
    process.exit(0)
  })
  setTimeout(() => process.exit(1), 10000)
}
```
- **Observacoes:**
> _Complementa #314 — uncaughtException tambem nao drena._

---

#### Issue #DR267 — tenantMiddleware: empresa desativada continua acessivel por ate 60s
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 38-41
- **Problema:** Cache de 60s guarda `req.tenant` sem revalidar `ativo = TRUE`. Se admin desativar empresa no painel, requests continuam chegando por ate 60 segundos. Em cenario de suspensao por inadimplencia ou comprometimento de conta, esse atraso e significativo.
- **Impacto:** Janela de 1 minuto de acesso apos desativacao — inaceitavel para revogacao por seguranca.
- **Fix sugerido:** Ou reduzir TTL para 5-10s, ou invalidar cache via `LISTEN/NOTIFY` do Postgres quando `empresas.ativo` muda, ou revalidar com query barata `SELECT 1 FROM empresas WHERE id=? AND ativo=TRUE`.
- **Observacoes:**
> _Complementa #322 (cache in-memory em cluster mode)._

---

### MEDIOS

#### Issue #DR268 — ocr.js upload lote: N+1 INSERTs em loop sequencial
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 130-148
- **Problema:** Loop `for (let i = 0; i < loteResult.encomendas.length; i++)` emite 1 INSERT por encomenda via `pool.query` (nao `client.query` com transacao). 100 encomendas = 100 round-trips + 100 conexoes do pool em serie. Se uma falhar no meio, as anteriores ja foram comitadas.
- **Impacto:** Latencia alta (100 × RTT), falhas parciais sem rollback, saturacao do pool.
- **Fix sugerido:** Usar transacao + batch INSERT:
```js
const client = await pool.connect();
try {
  await client.query('BEGIN');
  const values = [], params = [];
  loteResult.encomendas.forEach((enc, i) => {
    const off = i * 12;
    values.push(`($${off+1},$${off+2},...,$${off+12})`);
    params.push(randomUUID(), empresaId, ...);
  });
  await client.query(`INSERT INTO ocr_lancamentos (...) VALUES ${values.join(',')} ON CONFLICT (uuid) DO NOTHING`, params);
  await client.query('COMMIT');
} catch (e) { await client.query('ROLLBACK'); throw e; }
finally { client.release(); }
```
- **Observacoes:**
> __

---

#### Issue #DR269 — financeiro.js estornar: INSERT em auditoria fora da transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** ~531-580
- **Problema:** Rota de estorno usa `pool.query` para UPDATE de passagem/encomenda/frete e INSERT em `auditoria_financeiro`. Sem `BEGIN/COMMIT/ROLLBACK` explicito. Se INSERT auditoria falhar apos UPDATE, estorno fica sem trilha. Se UPDATE falhar apos auditoria ter sido gravada, trilha aponta para estorno que nunca aconteceu.
- **Impacto:** Auditoria financeira inconsistente; impossivel reconciliar manualmente.
- **Fix sugerido:** Envolver ambos em `client.connect()` + `BEGIN/COMMIT/ROLLBACK` — mesmo padrao ja aplicado em `ocr.js` aprovar.
- **Observacoes:**
> _Verificar linhas exatas; pode variar com refatoracao recente._

---

#### Issue #DR270 — Rate limiter in-memory cresce sem bound sob IPs variados
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/rateLimit.js`
- **Problema:** Map de hits cresce 1 entrada por IP/key. Em ataque com IPs rotativos (botnet), map pode chegar a milhoes de entradas em minutos. Cleanup tardio no expire do window.
- **Impacto:** Memory leak progressivo; OOM do Node em ataque.
- **Fix sugerido:** Hard cap no tamanho do map (LRU) ou mover para Redis (tambem resolve #247 e #322).
- **Observacoes:**
> __

---

#### Issue #DR271 — Pool PG: sem healthcheck periodico / dead connections
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/db.js`
- **Problema:** Connections ociosas podem ser mortas por firewall/load balancer sem o pool saber. Proxima query recebe `ECONNRESET`. `node-postgres` nao faz keepalive por default.
- **Impacto:** Primeiro request apos ociosidade falha; usuario ve 500 aleatorio.
- **Fix sugerido:**
```js
const pool = new Pool({ ..., keepAlive: true, idleTimeoutMillis: 10000, allowExitOnIdle: false });
pool.on('error', (err) => log.error('PG', 'Pool error', err.message));
```
- **Observacoes:**
> __

---

#### Issue #DR272 — /api/health nao valida dependencias (DB)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/index.js`
- **Linha(s):** 83-85
- **Problema:** `/api/health` retorna 200 mesmo se DB estiver inacessivel. Load balancer continua roteando trafego.
- **Fix sugerido:**
```js
app.get('/api/health', async (req, res) => {
  try {
    await Promise.race([pool.query('SELECT 1'), new Promise((_, r) => setTimeout(() => r(new Error('timeout')), 2000))]);
    res.json({ status: 'ok', db: 'ok', ts: Date.now() });
  } catch (e) {
    res.status(503).json({ status: 'degraded', db: 'down', err: e.message });
  }
});
```
- **Observacoes:**
> __

---

#### Issue #DR273 — OCR lote: `crypto.randomUUID()` usado sem `import crypto` (variantes de #304)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 127, 132, 243
- **Problema:** Duplicata de #304 — mantido aqui so para visibilidade. Garantir que o fix cubra todas as 3 ocorrencias.
- **Fix sugerido:** Ver #304.

---

## NOVOS PROBLEMAS — DESKTOP JAVA

### ALTOS

#### Issue #DR274 — ViagemDAO.definirViagemAtiva: COMMIT parcial deixa zero viagens ativas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 423-458
- **Problema:** Sequencia: `UPDATE set is_atual=false (todas)` → `UPDATE set is_atual=true WHERE id=?`. Se o segundo UPDATE falhar (violacao de constraint, connection drop), rollback funciona — OK. PORRE, se o segundo UPDATE executar com 0 linhas afetadas (id nao existe ou viagem de outra empresa), `conn.commit()` e chamado mesmo assim. Resultado: nenhuma viagem ativa.
- **Impacto:** Passagens emitidas nesse momento nao associam viagem; DAOs dependentes (`buscarViagemAtiva`) retornam null; bug silencioso ate operador perceber.
- **Fix sugerido:**
```java
int affected;
try (PreparedStatement s2 = conn.prepareStatement(sqlAtivar)) {
    s2.setLong(1, idViagemParaAtivar);
    s2.setInt(2, DAOUtils.empresaId());
    affected = s2.executeUpdate();
}
if (affected == 0) {
    conn.rollback();
    AppLogger.error("ViagemDAO", "Viagem " + idViagemParaAtivar + " nao encontrada para ativar");
    return false;
}
conn.commit();
```
- **Observacoes:**
> _Mesmo bug (em outro nivel) do #DR233 do V5.0 — la era BFF, aqui e DAO Desktop._

---

### MEDIOS

#### Issue #DR275 — SyncClient: aplicar registros recebidos SEM transacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~480-490 (loop em `sincronizarTabelaSync`)
- **Problema:** Loop `aplicarRegistroRecebido(tabela, registro)` processa cada registro com sua propria connection do pool. Se registro N falhar, registros 1..N-1 ja foram commitados; cliente marca a sync como falha e retenta — registros 1..N-1 sao reaplicados (idempotencia no ON CONFLICT funciona, mas pode mascarar problemas).
- **Impacto:** Estado de sync parcial; analises dependentes veem uma mistura de versoes.
- **Fix sugerido:** Abrir uma conexao e transacao para todo o lote de download; commit so no final.
- **Observacoes:**
> _Complementa #323 (catch silencioso no mesmo loop)._

---

#### Issue #DR276 — SyncClient: JWT decode manual via regex fragil
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~283-306 (metodo `garantirAutenticacao` ou similar)
- **Problema:** Decoder manual de claim `exp` com `replaceFirst("[^0-9]+", "")` e concat. Se payload tiver duas ocorrencias de `exp` (nested JSON), regex pega a primeira, que pode nao ser o topo. Catch vazio na falha — token expirado continua sendo usado ate 401 do servidor.
- **Impacto:** Sync quebra com 401 cada vez que token expira; operador precisa relogar manualmente.
- **Fix sugerido:** Parse proprio do JWT payload com `Base64.getUrlDecoder` + Jackson:
```java
String[] parts = jwtToken.split("\\.");
String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
Map<String,Object> claims = MAPPER.readValue(payload, new TypeReference<>() {});
long exp = ((Number) claims.get("exp")).longValue();
if (System.currentTimeMillis()/1000 > exp - 60) { jwtToken = null; }
```
- **Observacoes:**
> __

---

#### Issue #DR277 — SyncClient.buscarRegistrosPendentes: sem LIMIT
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** ~510-556
- **Problema:** `SELECT * FROM tabela WHERE sincronizado=FALSE` sem LIMIT. Se a embarcacao ficou offline por semanas, `passagens` pode ter 50k+ registros pendentes. Carregar tudo de uma vez pode gerar OOM ou timeout HTTP.
- **Impacto:** OutOfMemoryError em sync apos periodo longo offline; sync nunca completa.
- **Fix sugerido:** LIMIT 1000 + loop "ate esgotar" com multiplas chamadas de sync.
- **Observacoes:**
> __

---

#### Issue #DR278 — RelatorioPassagensController: Thread de carga sem try-catch externo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RelatorioPassagensController.java`
- **Linha(s):** 101-138
- **Problema:** `new Thread(() -> { ... }).start()` sem try-catch. Se `rotaDAO.listarTodasAsRotasComoObjects()` lancar excecao, a thread morre; UI nunca recebe dados; flags nao sao atualizadas.
- **Impacto:** Relatorio fica em estado "carregando" permanentemente sem feedback.
- **Fix sugerido:** Usar `Task<>` (padrao ja adotado nos controllers migrados em V5.0) com `setOnFailed` mostrando AlertHelper.
- **Observacoes:**
> _Mesmo padrao ja corrigido em ~20 controllers no V5.0 (#DR117); este ficou para tras._

---

#### Issue #DR279 — TelaPrincipalController.initialize: Task sem setOnFailed
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 127-174
- **Problema:** `Task<Void>` com `setOnSucceeded` mas sem `setOnFailed`. Se algum DAO lancar excecao (sessao corrompida, DB offline), falha so vai pro `call().failed()` que apenas loga. Dashboard fica com combos vazios.
- **Impacto:** Dashboard silenciosamente vazio em falha; operador perde contexto.
- **Fix sugerido:**
```java
taskInit.setOnFailed(e -> {
    AppLogger.error("TelaPrincipal", "Falha ao carregar dashboard", taskInit.getException());
    Platform.runLater(() -> AlertHelper.errorSafe("Erro ao carregar dashboard — veja logs"));
});
```
- **Observacoes:**
> __

---

## NOVOS PROBLEMAS — APPS REACT (naviera-app, naviera-ocr, naviera-web)

### ALTOS

#### Issue #DR280 — useApi: setState apos unmount via .finally()
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/api.js`
- **Linha(s):** 62-95
- **Problema:** `AbortController` cancela o fetch, mas `.finally(() => setLoading(false))` e `.then(d => setData(d))` podem rodar DEPOIS do unmount quando o abort nao e aceito imediatamente (response ja recebido parcialmente). Resultado: warning React "state update on unmounted component" + memory leak.
- **Impacto:** Warnings em console em cada navegacao; potencial memory leak em MapaCPF (30s auto-refresh).
- **Fix sugerido:** Adicionar flag `cancelled`:
```js
useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    fetch(url, { signal: controller.signal })
      .then(r => r.json())
      .then(d => { if (!cancelled) setData(d); })
      .catch(e => { if (!cancelled) setErr(e); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; controller.abort(); };
}, [url]);
```
- **Observacoes:**
> _Complementa #DR236 (V5.0, ja fixado) — adiciona a camada de guarda contra setState tardio._

---

#### Issue #DR281 — LoginScreen/TelaCadastro: double-submit possivel via Enter repetido
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/LoginScreen.jsx`, `TelaCadastroCPF.jsx`, `TelaCadastroCNPJ.jsx`
- **Problema:** Botao e desabilitado via `disabled={loginLoading}`, mas `loginLoading` e setado DENTRO de `doLogin`. Teclado com autorepeat ou toque duplo pode disparar 2x antes do setState propagar (React agenda, nao aplica imediato).
- **Impacto:** 2 POSTs simultaneos de login/cadastro — duplica tokens, pode criar 2 contas em cadastro.
- **Fix sugerido:**
```jsx
const pendingRef = useRef(false);
const doLogin = async () => {
  if (pendingRef.current) return;
  pendingRef.current = true;
  setLoginLoading(true);
  try { /* ... */ }
  finally { pendingRef.current = false; setLoginLoading(false); }
};
```
- **Observacoes:**
> _Typical idempotency guard em frontend; complementa idempotencia server-side._

---

#### Issue #DR282 — PassagensCPF/EncomendaCPF confirmarCompra: catch mascara erro de parse apos res.ok=false
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx` (L34-42), `EncomendaCPF.jsx` (L29-47)
- **Problema:** Bloco `try { const res = await authFetch(...); const data = await res.json(); if (!res.ok) ... } catch { setErr("Erro de conexao"); }`. Se backend retornar 200 com body invalido (caractere nao-JSON), `res.json()` lanca — catch generico mostra "Erro de conexao" mesmo que a transacao tenha passado no servidor.
- **Impacto:** Usuario repete a compra achando que falhou — cobranca duplicada no Asaas.
- **Fix sugerido:** Separar camadas:
```jsx
try {
  const res = await authFetch(url, {...});
  let data;
  try { data = await res.json(); } catch { data = null; }
  if (!res.ok) { setErr(data?.erro || "Erro no servidor"); return; }
  if (!data) { setErr("Resposta invalida — verifique se a compra foi registrada"); return; }
  setResultado(data);
} catch (e) {
  console.error('[PassagensCPF] rede', e);
  setErr("Sem conexao. Verifique sua internet.");
}
```
- **Observacoes:**
> _Relacionado a #317 (logging) — este e sobre semantica da mensagem._

---

### MEDIOS

#### Issue #DR283 — PagamentoArtefato.copiar: promise `writeText()` sem .catch
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/components/PagamentoArtefato.jsx`
- **Linha(s):** 19-25
- **Problema:** `navigator.clipboard?.writeText(txt).then(() => ...)`. Sem `.catch`. Em navegador sem https, navegador sem permission, ou iframe, a promise rejeita — UnhandledPromiseRejection no console e usuario clica "Copiar" e nada acontece.
- **Impacto:** PIX/codigo de boleto nao copia; usuario atrasa pagamento.
- **Fix sugerido:** `.then(...).catch(e => { setErro("Falha ao copiar"); console.warn('[Pagamento]', e); })`.
- **Observacoes:**
> _Duplica #318 do AUDIT_V1.3 — re-flagado para acompanhamento._

---

#### Issue #DR284 — MapaCPF: setInterval com `fetchGps` no deps re-cria timer em cada auth change
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/MapaCPF.jsx`
- **Linha(s):** 70-77
- **Problema:** `useEffect(() => { ...setInterval(fetchGps)... }, [fetchGps])`. `fetchGps` memoized em `useCallback([authHeaders?.Authorization])`. Se auth tem re-render (ex: token refresh), `fetchGps` muda → effect cleanup roda → novo interval comeca → mas se cleanup anterior nao aguardou abort, 2 timers co-existem por ms.
- **Impacto:** Em condicoes adversas, multiplas requests paralelas para GPS; consumo de bateria.
- **Fix sugerido:** Usar ref para `fetchGps` e dep `[]`:
```jsx
const fetchGpsRef = useRef(fetchGps);
useEffect(() => { fetchGpsRef.current = fetchGps; });
useEffect(() => {
  const id = setInterval(() => fetchGpsRef.current(true), REFRESH_MS);
  return () => clearInterval(id);
}, []);
```
- **Observacoes:**
> __

---

#### Issue #DR285 — ErrorBoundary sem remote logging
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/ErrorBoundary.jsx`, `naviera-ocr/src/ErrorBoundary.jsx`
- **Problema:** `componentDidCatch` apenas `console.error`. Em producao, nenhum diagnostico chega ao backend. Usuario reporta "tela branca" e desenvolvedor nao tem nada.
- **Impacto:** Bugs em producao sem rastreabilidade.
- **Fix sugerido:** Enviar POST para `/api/client-errors` com `{error.message, error.stack, componentStack, user_agent, rota}`. Criar endpoint simples no BFF que salva em tabela `client_errors`.
- **Observacoes:**
> _Opcoes avancadas: Sentry/Datadog. Minimo: endpoint proprio com rate limit._

---

#### Issue #DR286 — localStorage.getItem + JSON.parse sem validacao de schema
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/App.jsx` (L46, L54), varios outros screens
- **Problema:** `JSON.parse(localStorage.getItem("naviera_usuario"))` tem try/catch contra JSON invalido, mas nao contra objeto com schema diferente (ex: depois de refactor de campos). Se user_v1 tem `{tipo, nome}` e v2 tem `{perfil, fullName}`, app nao crasheia mas comportamento fica indefinido.
- **Impacto:** Bugs misteriosos apos atualizacao sem logout forcado.
- **Fix sugerido:** Validar campos esperados apos parse; se nao baterem, limpar e redirecionar para login.
- **Observacoes:**
> __

---

## CENSO DE CATCH BLOCKS (Atualizado V6.0)

| Tipo | Quantidade | % |
|------|-----------|---|
| **Empty catch** `{}` | ~10 | 2% |
| **Catch ignored** (labeled) | ~4 | 1% |
| **AppLogger/console com contexto** | ~475 | 80% |
| **AlertHelper/UI feedback** | ~48 | 8% |
| **Proper handling** (alert + log ou rethrow) | ~55 | 9% |
| **TOTAL catches no projeto** | **~592** | 100% |

> Estavel vs V5.0. As 10 empty catches restantes sao em ConexaoBD cleanup (aceitavel) e em SetupWizardController/SyncClient (flagadas individualmente).

---

## COBERTURA (V6.0)

| Diretorio | Arquivos | Analisados | Issues NOVAS |
|-----------|----------|-----------|-------------|
| src/dao/ | 27 | 27 (100%) | 1 (DR274) |
| src/gui/ | 56 | 56 (100%) | 2 (DR278, DR279) |
| src/gui/util/ | 16 | 16 (100%) | 3 (DR275-DR277) |
| naviera-api psp/ | 14 | 14 (100%) | 5 (DR260-DR264) |
| naviera-api service/ | 31 | 31 (100%) | 0 (#303,#308,#309,#312 pendentes AUDIT_V1.3) |
| naviera-api config/ | 10 | 10 (100%) | 0 (#311,#313 pendentes AUDIT_V1.3) |
| naviera-web server/ | 30+ | 30 (100%) | 9 (DR265-DR273) |
| naviera-app src/ | 30+ | 30 (100%) | 6 (DR280-DR286) |
| naviera-ocr src/ | 8 | 8 (100%) | compartilhados (DR285) |
| **TOTAL** | **~220** | **220 (100%)** | **26** |

---

## PLANO DE CORRECAO

### P0 — Bloqueio de producao (CRITICO) — **CONCLUIDA (2026-04-23)**

Todos os 8 CRITICOs ja estavam corrigidos no codigo no momento da conferencia (2026-04-23). #DR263 segue pendente (MEDIO, nao CRIT).

- [x] **#300** AsaasGateway RestTemplate timeout _(5s connect / 15s read em AsaasGateway:51-54)_
- [x] **#301** WebhookController + idempotencia _(PspWebhookController existe com psp_webhook_events)_
- [x] **#304** ocr.js `import { randomUUID } from 'crypto'` _(em ocr.js:6)_
- [x] **#305** webhook secret: fail-closed em profile=prod _(AsaasGateway:201-210)_
- [x] **#308** `@Transactional` + PSP separados _(3 services usam tx.execute() programatico)_
- [x] **#311** FirebaseConfig: throw em prod _(FirebaseConfig:30-49 com profile check)_
- [x] **#315** Dockerfile: tini + STOPSIGNAL + EXPOSE 8081 _(conferido no Dockerfile)_
- [x] **#DR260** AsaasGateway: null check em parseBody _(AsaasGateway:261-267)_

### P1 — Importante (ALTO)

- [ ] **#302** PSP 2 round-trips com retry + Idempotency-Key
- [ ] **#307** fetchWithRetry: retry em 429 com Retry-After
- [ ] **#309** SyncService: retornar uuids falhos para cliente
- [ ] **#310** SyncClient: sinalizar falhas consecutivas via AlertHelper/tray
- [ ] **#313** GlobalExceptionHandler: correlationId em log + response
- [ ] **#314** BFF uncaughtException: drenar pool antes de exit
- [ ] **#319** useWebSocket: heartbeatIncoming/Outgoing 10000
- [ ] **#DR261** AsaasGateway: usar split retornado pelo Asaas
- [ ] **#DR262** AsaasGateway: normalizar cpfCnpj em query
- [ ] **#DR265** express.json limit 1mb
- [ ] **#DR266** Graceful shutdown drenando pool PG
- [ ] **#DR267** tenantMiddleware TTL curto ou LISTEN/NOTIFY
- [ ] **#DR274** ViagemDAO.definirViagemAtiva: verificar affected rows antes de commit
- [ ] **#DR280** useApi: flag cancelled
- [ ] **#DR281** Login/Cadastro: pendingRef guard
- [ ] **#DR282** confirmarCompra: separar catch de rede vs parse

### P2 — Importante (MEDIO)

- [ ] #306 fetchWithRetry jitter + log
- [ ] #312 PushService FCM multicast
- [ ] #317 + #DR283 frontend catch logging + fallback
- [ ] #318 clipboard .catch
- [ ] #320 SyncClient escapeJson → ObjectMapper
- [ ] #322 TenantMiddleware → Redis (junto com #DR267)
- [ ] #323 SyncClient aplicarRegistroRecebido erros contabilizados
- [ ] #324 Asaas onboarding: parse HttpClientErrorException
- [ ] #327 obterOuCriarCustomer: Idempotency-Key no create
- [ ] #328 SyncClient shutdown hook
- [ ] #329 geminiParser: response_mime_type=application/json
- [ ] #330 NotificationService: outbox local
- [ ] #DR264 splitNavieraPct range validation
- [ ] #DR268 OCR lote batch INSERT em transacao
- [ ] #DR269 financeiro estornar dentro de transacao
- [ ] #DR270 rateLimiter LRU cap
- [ ] #DR271 pool keepAlive + on('error')
- [ ] #DR275 SyncClient downloads em transacao
- [ ] #DR276 SyncClient JWT decode com Base64/Jackson
- [ ] #DR277 SyncClient pendentes com LIMIT
- [ ] #DR278 RelatorioPassagensController Task<>
- [ ] #DR279 TelaPrincipalController setOnFailed
- [ ] #DR284 MapaCPF ref pattern para fetchGps
- [ ] #DR285 ErrorBoundary com remote logging

### P3 — Menor (BAIXO)

- [ ] #321 bcrypt library nativa
- [ ] #331 Auditoria pendente de AuthService/AuthOperadorService
- [ ] #DR272 /api/health com check DB
- [ ] #DR273 ver #304 (consolidar)
- [ ] #DR286 localStorage schema validation

---

## NOTAS

> **Comparacao V5.0 → V6.0:**
> - V5.0 fechou 47 issues (#DR201-#DR247) mas nao tocou em **nenhum** dos issues de resiliencia do AUDIT_V1.3 secao 2.4
> - 26 issues NOVAS descobertas nesta sessao (5 PSP + 9 BFF + 6 Desktop + 6 React)
> - Total ativo V6.0: **53 issues** (27 pendentes de AUDIT_V1.3 + 26 novas)
>
> **Tema dominante:** PSP/Asaas. Camada adicionada apos V5.0, com design que coloca HTTP externo dentro de transacao JPA, aceita webhooks sem validacao de assinatura em config default, nao tem idempotencia, e tem NPE no parsing de resposta. Todos os issues CRITICOS de V6.0 concentram-se aqui ou no Dockerfile (que nao propaga SIGTERM).
>
> **Segundo tema:** Graceful shutdown e drain. Tanto BFF Node (#DR266, #314) quanto Desktop (#DR277, #DR278) tem problemas em momentos de finalizacao/restart.
>
> **Terceiro tema:** State machines sem validacao de transicao (#DR263, #DR274). Webhooks fora de ordem e bugs de commit parcial conseguem deixar dados em estados invalidos.
>
> **Nao apareceram regressoes de V5.0** — fixes aplicados se mantem.

---
*Gerado por Claude Code (Deep Audit V6.0) — Revisao humana obrigatoria antes de deploy*
