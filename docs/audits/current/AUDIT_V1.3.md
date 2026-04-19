# AUDITORIA DE CODIGO — Naviera Eco
> **Versao:** V1.3
> **Data:** 2026-04-18
> **Auditor:** Claude Code (Dev Senior Audit)
> **Stack:** JavaFX 23 Desktop + Spring Boot 3.3.5 API + React 18/Vite + Express BFF + React 19/Vite App + PostgreSQL
> **Escopo:** Auditoria completa (scan 6 categorias + contra-verificacao)

---

## RESUMO EXECUTIVO

| Severidade | Quantidade |
|-----------|-----------|
| CRITICO | 30 |
| ALTO | 77 |
| MEDIO | 84 |
| BAIXO | 34 |
| **TOTAL** | **225** |

**Status geral:** REPROVADO PARA PRODUCAO (30 CRITICOs)

**Top 5 bloqueadores:**
1. #201 — Webhook Asaas NAO existe — pagamentos PIX/boleto ficam eternamente PENDENTE
2. #411 — PSP inline em `@Transactional` + HikariCP=10 — incidente Asaas derruba API inteira
3. #650 — `X-Tenant-Slug` aceito do cliente sem validar trusted proxy — bypass multi-tenant
4. #100/#114 — Admin de qualquer empresa pode modificar/desativar outras via `/admin/empresas`
5. #107/#105/#106 — Cross-tenant data leak em rotas e pagamentos (`WHERE id = ?` sem empresa_id)

---

## 1. MAPEAMENTO ESTRUTURAL

### Arvore do Projeto

```
Naviera_Eco/
├── src/                          # Desktop JavaFX (Eclipse project, sem build tool)
│   ├── dao/                      # 29 DAOs tenant-aware + ConexaoBD + TenantContext + DAOUtils
│   ├── gui/                      # 67 controllers JavaFX + FXML + CSS + util/ (AlertHelper, PermissaoService, SyncClient, AppLogger)
│   ├── model/                    # ~25 POJOs
│   ├── service/                  # Servicos de apoio do desktop
│   ├── util/                     # Utils gerais
│   └── tests/                    # JUnit 4
├── lib/                          # 44 JARs (JavaFX, Postgres JDBC, Jackson, iText, JasperReports, BCrypt)
├── resources/                    # CSS, icones, imagens do desktop
├── bin/                          # Classes compiladas (Eclipse)
├── certs/                        # Certificados (TLS, etc.)
├── database_scripts/             # 30+ migrations SQL (000-028, sequenciais)
├── docker-compose.yml            # Orquestracao API + App + Postgres
├── nginx/                        # Configs Nginx (wildcard subdominio para multi-tenant)
├── naviera-api/                  # Spring Boot 3.3.5 (Java 17, Maven)
│   └── src/main/java/com/naviera/api/
│       ├── NavieraApiApplication.java  # Entrypoint Spring Boot
│       ├── controller/           # 28 controllers REST
│       ├── service/              # Logica de negocio (Auth, Financeiro, Passagem, Frete, etc.)
│       ├── repository/           # Spring Data JPA
│       ├── model/                # Entidades JPA
│       ├── dto/                  # DTOs
│       ├── security/             # JwtFilter, JwtUtil
│       ├── config/               # CORS, Security, WebSocket, TenantUtils, RateLimitFilter
│       └── psp/                  # Integracao PSP Asaas (gateway, cobranca, subconta, onboarding)
├── naviera-web/                  # React 18 + Vite + Express BFF
│   ├── src/
│   │   ├── pages/                # ~40 paginas (Dashboard, Financeiro, Passagens, Fretes, Encomendas, OCR, Admin...)
│   │   ├── components/           # Layout, Sidebar, TopBar
│   │   ├── styles/
│   │   └── utils/
│   └── server/                   # BFF Express (porta 3002)
│       ├── index.js              # Entrypoint
│       ├── db.js                 # Pool PostgreSQL
│       ├── logger.js
│       ├── routes/               # 15 arquivos de rotas (auth, viagens, rotas, embarcacoes, passagens, encomendas, fretes, cadastros, financeiro, dashboard, admin, agenda, estornos, ocr, documentos)
│       ├── middleware/           # auth, rateLimit, tenant, validate, errorHandler
│       ├── helpers/
│       ├── utils/
│       └── tests/
├── naviera-app/                  # React 19 + Vite (mobile/PWA)
│   └── src/
│       ├── main.jsx + App.jsx + api.js
│       ├── screens/              # 14 telas (Login, Cadastro, Perfil; CPF: Home, Amigos, Mapa, Passagens, Encomenda, Bilhete; CNPJ: Home, Pedidos, Financeiro, Loja, LojasParceiras)
│       ├── components/           # Badge, Card, Header, TabBar, Toast, Skeleton, Avatar + PagamentoArtefato
│       ├── hooks/                # useNotifications, usePWA, useWebSocket
│       ├── ErrorBoundary.jsx
│       ├── helpers.js
│       ├── icons.jsx
│       └── theme.js
├── naviera-ocr/                  # Modulo OCR (Google Vision + Gemini)
├── naviera-site/                 # Site institucional
└── docs/                         # Documentacao (audits/, mvp/, decisions/, runbooks/, specs/, STATUS.md)
```

### Pontos de Entrada

| Camada | Entrypoint | Como inicia |
|--------|-----------|-------------|
| Desktop (prod) | `src/gui/LoginLauncher.java` | `java ... gui.LoginLauncher` (VS Code F5 config "Naviera Desktop") |
| Desktop (dev) | `src/gui/LaunchDireto.java` | Atalho sem login |
| API | `naviera-api/src/main/java/com/naviera/api/NavieraApiApplication.java` | `mvn spring-boot:run` (porta 8081) |
| Web BFF | `naviera-web/server/index.js` | `node server/index.js` (porta 3002) |
| Web Frontend | `naviera-web/src/main.jsx` (via Vite) | `npm run dev` (porta 5174, proxy /api -> :3002) |
| App Mobile | `naviera-app/src/main.jsx` (via Vite) | `npm run dev` (porta 3000) |
| Docker | `docker-compose.yml` | `docker compose up -d --build` (API :8081, App :8180, DB :5432) |

### Dependencias Principais

#### Desktop (lib/ — 44 JARs, sem pom.xml)
| Dependencia | Versao | Proposito |
|-------------|--------|-----------|
| JavaFX | 23.0.2 | UI framework |
| PostgreSQL JDBC | — | Driver JDBC |
| Jackson | — | JSON (integracao sync com API) |
| iText | — | Geracao PDF (boletos, holerites) |
| JasperReports | — | Relatorios |
| jBCrypt | — | Hash de senhas |

#### API (naviera-api/pom.xml)
| Dependencia | Versao | Proposito |
|-------------|--------|-----------|
| Spring Boot Starter Parent | 3.3.5 | Parent POM |
| spring-boot-starter-web | 3.3.5 | REST |
| spring-boot-starter-security | 3.3.5 | JWT + BCrypt |
| spring-boot-starter-data-jpa | 3.3.5 | Persistencia |
| spring-boot-starter-validation | 3.3.5 | Bean validation |
| spring-boot-starter-websocket | 3.3.5 | STOMP + SockJS |
| spring-boot-starter-actuator | 3.3.5 | Healthcheck |
| postgresql | (Boot managed) | Driver JDBC |
| jjwt (api/impl/jackson) | 0.12.6 | JWT |
| jbcrypt | 0.4 | Hash senhas |
| lombok | — | Boilerplate |
| firebase-admin | 9.3.0 | Push notifications (FCM) |
| Java | 17 | Runtime |

#### Web (naviera-web/package.json)
| Dependencia | Versao | Proposito |
|-------------|--------|-----------|
| react + react-dom | 18.2.0 | UI |
| vite | 5.0.0 | Bundler/dev server |
| @vitejs/plugin-react | 4.2.0 | Plugin React |
| express | 5.2.1 | BFF HTTP server |
| pg | 8.20.0 | Pool PostgreSQL |
| jsonwebtoken | 9.0.3 | JWT server-side |
| bcryptjs | 3.0.3 | Hash senhas |
| multer | 1.4.5-lts.1 | Upload de arquivos (OCR, docs) |
| cors | 2.8.6 | CORS |
| dotenv | 17.4.1 | .env loader |

#### App Mobile (naviera-app/package.json)
| Dependencia | Versao | Proposito |
|-------------|--------|-----------|
| react + react-dom | 19.2.5 | UI (React 19) |
| vite | 5.4.21 | Bundler |
| @vitejs/plugin-react | 4.7.0 | Plugin React |
| firebase | 12.12.0 | FCM push notifications |
| @stomp/stompjs | 7.3.0 | STOMP client (WebSocket) |
| sockjs-client | 1.6.1 | SockJS fallback |

### Fluxo de Dados Principal

#### 1) Fluxo Web (BFF) — dominante
```
Browser (React/Vite :5174)
  -> Vite dev proxy  /api/*
  -> Express BFF (:3002)
       tenantMiddleware (resolve slug do subdominio -> empresa_id)
       rateLimit (200 req/min)
       auth middleware (JWT Bearer -> req.user)
  -> rota (routes/*.js) executa SQL via pg Pool (server/db.js)
  -> PostgreSQL (porta 5432/5437 dev)
  -> resposta JSON
```
Observacao: o Web BFF vai **direto** ao Postgres, nao passa pela API Spring.

#### 2) Fluxo App Mobile
```
PWA/Web (:3000) -> Axios (src/api.js, VITE_API_URL) -> API Spring (:8081)
  -> JwtFilter extrai empresa_id do token -> TenantUtils
  -> Controller -> Service -> Repository (JPA) -> PostgreSQL
  -> WebSocket STOMP/SockJS para notificacoes em tempo real
```

#### 3) Fluxo Desktop (OFFLINE-FIRST)
```
JavaFX Controller -> DAO -> ConexaoBD.getConnection() (pool JDBC custom)
  -> PostgreSQL LOCAL (db.properties: jdbc.url + empresa.id)
  -> SyncClient (quando online) envia deltas para API Spring
```
Regra critica: desktop SEMPRE usa banco local. Sync imediato quando online. Nunca apontar pro banco da VPS.

#### 4) Multi-tenant
- Toda tabela de negocio tem coluna `empresa_id`
- Desktop: `empresa.id` fixo em `db.properties`
- API: extraido do JWT no `JwtFilter`
- Web: extraido do JWT + slug do subdominio (middleware `tenant.js`)
- `aux_*` sao compartilhadas (NAO filtrar)

### Variaveis de Ambiente

#### Raiz / Docker (.env.example)
| Variavel | Obrigatoria | Default | Descricao |
|----------|-------------|---------|-----------|
| DB_HOST | Sim | host.docker.internal | Host do Postgres |
| DB_PORT | Sim | 5432 | Porta do Postgres |
| DB_NAME | Sim | naviera_eco | Nome do banco |
| DB_USER | Sim | postgres | Usuario |
| DB_PASSWORD | Sim | — | Senha |
| JWT_SECRET | Sim | — | Segredo JWT (min 32 chars) |
| SERVER_PORT | Nao | 8081 | Porta da API Spring |
| CORS_ORIGINS | Sim | http://localhost:3000,5173,8180 | Lista separada por virgula |

#### naviera-api/.env.example
| Variavel | Obrigatoria | Default | Descricao |
|----------|-------------|---------|-----------|
| DB_USER | Sim | postgres | Usuario DB |
| DB_PASSWORD | Sim | — | Senha DB |
| JWT_SECRET | Sim | — | Segredo JWT |

#### naviera-web/.env.example
| Variavel | Obrigatoria | Default | Descricao |
|----------|-------------|---------|-----------|
| DB_HOST | Sim | localhost | Host Postgres |
| DB_PORT | Sim | 5437 | Porta Postgres (dev) |
| DB_NAME | Sim | naviera_eco | Nome do banco |
| DB_USER | Sim | postgres | Usuario |
| DB_PASSWORD | Sim | — | Senha |
| JWT_SECRET | Sim | — | Segredo JWT (deve igualar ao da API) |
| SERVER_PORT | Nao | 3002 | Porta do BFF Express |
| CORS_ORIGINS | Sim | localhost:5174,5173,5175 | Origens permitidas |
| GOOGLE_CLOUD_VISION_API_KEY | Sim (para OCR) | — | API key Google Vision |
| GEMINI_API_KEY | Sim (para OCR) | — | API key Gemini (modelo gemini-3-flash-preview) |
| OCR_UPLOAD_PATH | Nao | uploads/ocr | Path local para uploads OCR |

#### naviera-app/.env.example
| Variavel | Obrigatoria | Default | Descricao |
|----------|-------------|---------|-----------|
| VITE_API_URL | Sim | http://localhost:8081/api | URL da API Spring |
| VITE_FIREBASE_API_KEY | Nao | — | FCM (fallback: Notification API) |
| VITE_FIREBASE_AUTH_DOMAIN | Nao | — | FCM |
| VITE_FIREBASE_PROJECT_ID | Nao | — | FCM |
| VITE_FIREBASE_MESSAGING_SENDER_ID | Nao | — | FCM |
| VITE_FIREBASE_APP_ID | Nao | — | FCM |
| VITE_FIREBASE_VAPID_KEY | Nao | — | FCM Web Push |

#### Desktop (db.properties)
| Variavel | Obrigatoria | Descricao |
|----------|-------------|-----------|
| jdbc.url | Sim | URL JDBC do Postgres local |
| db.user | Sim | Usuario |
| db.password | Sim | Senha |
| empresa.id | Sim | Tenant fixo deste desktop |
| app.versao | Sim | Versao para checagem de update |

---

## 2. PROBLEMAS ENCONTRADOS

### 2.1 — Bugs Criticos e Runtime

#### Issue #003 — NullPointerException em EncomendaService.pagar quando empresa_id vem null
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 177
- **Problema:** `Integer empresaId = ((Number) enc.get("empresa_id")).intValue();` — se o registro existir no banco com `empresa_id IS NULL` (legado pre-multi-tenant), lanca NPE dentro de transacao @Transactional, rollback ocorre mas o cliente recebe 500 sem contexto util.
- **Impacto:** Encomendas legadas sem tenant causam 500 em producao; usuario nao consegue pagar e nao sabe por que.
- **Codigo problematico:**
```java
Integer empresaId = ((Number) enc.get("empresa_id")).intValue();
String subcontaId = (String) jdbc.queryForMap(
    "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId).get("psp_subconta_id");
```
- **Fix sugerido:**
```java
Object empresaIdRaw = enc.get("empresa_id");
if (empresaIdRaw == null) {
    throw ApiException.badRequest("Encomenda sem empresa vinculada - contate o suporte");
}
Integer empresaId = ((Number) empresaIdRaw).intValue();
// queryForMap pode lancar EmptyResultDataAccessException — usar queryForList + check empty:
var rows = jdbc.queryForList("SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId);
if (rows.isEmpty()) throw ApiException.badRequest("Empresa nao existe");
String subcontaId = (String) rows.get(0).get("psp_subconta_id");
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #004 — NPE identico em FreteService.pagar (empresa_id null)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 156
- **Problema:** `Integer empresaId = ((Number) f.get("empresa_id")).intValue();` sem null-check. Fretes importados do desktop ou legacy rows podem ter empresa_id null.
- **Impacto:** 500 em producao quando CNPJ tenta pagar frete legado.
- **Codigo problematico:**
```java
Integer empresaId = ((Number) f.get("empresa_id")).intValue();
```
- **Fix sugerido:**
```java
Object empresaIdRaw = f.get("empresa_id");
if (empresaIdRaw == null) throw ApiException.badRequest("Frete sem empresa vinculada");
Integer empresaId = ((Number) empresaIdRaw).intValue();
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #005 — NPE em PassagemService.comprar quando viagem.empresa_id null
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 76-77
- **Problema:** `((Number) viagem.get(0).get("empresa_id")).intValue()` e `(Long) viagem.get(0).get("id_rota")` — sem null-check. Viagens legadas sem tenant causam NPE. `id_rota` e NOT NULL no schema — fix reduz para apenas guard de `empresa_id`.
- **Impacto:** 500 quando cliente tenta comprar passagem de viagem legada; transacao cai em rollback mas sem mensagem util.
- **Codigo problematico:**
```java
Integer empresaId = ((Number) viagem.get(0).get("empresa_id")).intValue();
Long idRota = (Long) viagem.get(0).get("id_rota");
```
- **Fix sugerido:**
```java
Object empresaIdRaw = viagem.get(0).get("empresa_id");
if (empresaIdRaw == null) throw ApiException.badRequest("Viagem sem empresa vinculada");
Integer empresaId = ((Number) empresaIdRaw).intValue();
Long idRota = ((Number) viagem.get(0).get("id_rota")).longValue();
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #006 — NPE em PassagemService.comprar quando tarifa retorna null em valor_transporte/alimentacao/desconto
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 86-89
- **Problema:** `transporte.add(alimentacao).subtract(desconto)` — qualquer um dos tres sendo null (coluna nullable no banco) lanca NPE. O schema permite NULL em valor_alimentacao e valor_desconto.
- **Impacto:** Compra falha com 500 para qualquer tarifa mal cadastrada. Cliente nao compra a passagem.
- **Codigo problematico:**
```java
var transporte = (BigDecimal) tarifa.get("valor_transporte");
var alimentacao = (BigDecimal) tarifa.get("valor_alimentacao");
var desconto = (BigDecimal) tarifa.get("valor_desconto");
var total = transporte.add(alimentacao).subtract(desconto);
```
- **Fix sugerido:**
```java
BigDecimal transporte = (BigDecimal) tarifa.get("valor_transporte");
BigDecimal alimentacao = (BigDecimal) tarifa.get("valor_alimentacao");
BigDecimal desconto = (BigDecimal) tarifa.get("valor_desconto");
if (transporte == null) transporte = BigDecimal.ZERO;
if (alimentacao == null) alimentacao = BigDecimal.ZERO;
if (desconto == null) desconto = BigDecimal.ZERO;
BigDecimal total = transporte.add(alimentacao).subtract(desconto);
if (total.signum() < 0) total = BigDecimal.ZERO;
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #007 — ClassCastException em PassagemService.comprar ao ler id_passageiro como Long
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 98-101
- **Problema:** `jdbc.queryForObject("SELECT id_passageiro FROM passageiros...", Long.class, ...)` — ha uma RACE CONDITION obvia entre o INSERT e o SELECT subsequente. Se dois CPFs identicos chegarem em paralelo, ambos fazem INSERT, resultando em registros duplicados ou UniqueConstraint violation. Nao ha UNIQUE(documento, empresa_id) garantido.
- **Impacto:** Passageiro duplicado ou transacao abortada ao concorrer com outra compra.
- **Codigo problematico:**
```java
if (passageiros.isEmpty()) {
    jdbc.update("INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id) VALUES (?, ?, ?)",
        cliente.getNome(), cliente.getDocumento(), empresaId);
    idPassageiro = jdbc.queryForObject("SELECT id_passageiro FROM passageiros WHERE numero_documento = ? AND empresa_id = ?",
        Long.class, cliente.getDocumento(), empresaId);
}
```
- **Fix sugerido:**
```sql
-- Migration obrigatoria ANTES do INSERT ON CONFLICT:
-- 019_passageiros_unique_doc.sql
-- 1) Dedup passageiros por (numero_documento, empresa_id), mantendo o menor id
-- 2) CREATE UNIQUE INDEX CONCURRENTLY passageiros_doc_empresa_uidx
--    ON passageiros(numero_documento, empresa_id)
--    WHERE numero_documento IS NOT NULL;
```
```java
// Use INSERT ... ON CONFLICT com RETURNING — atomico
idPassageiro = jdbc.queryForObject(
    "INSERT INTO passageiros (nome_passageiro, numero_documento, empresa_id) VALUES (?, ?, ?) " +
    "ON CONFLICT (numero_documento, empresa_id) DO UPDATE SET nome_passageiro = EXCLUDED.nome_passageiro " +
    "RETURNING id_passageiro",
    Long.class, cliente.getNome(), cliente.getDocumento(), empresaId);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #008 — Numero de bilhete colisivel (mod 1e6) em PassagemService
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 105
- **Problema:** `"APP-" + String.format("%06d", System.currentTimeMillis() % 1000000)` — o mod 1.000.000 reinicia a cada ~16 minutos. Duas compras simultaneas no mesmo ms geram identico bilhete. A sequence `seq_numero_bilhete` JA existe no schema (L434).
- **Impacto:** Compras simultaneas falhando com 500, ou bilhetes duplicados causando `consultarParaEmbarque()` a retornar passagem errada (cross-tenant se combinado com outro bug).
- **Codigo problematico:**
```java
String numBilhete = "APP-" + String.format("%06d", System.currentTimeMillis() % 1000000);
```
- **Fix sugerido:**
```java
// Usar sequence existente do banco (idempotente, sem colisao) — %08d para nao estourar em 1M bilhetes
Long seq = jdbc.queryForObject("SELECT nextval('seq_numero_bilhete')", Long.class);
String numBilhete = String.format("APP-%08d", seq);
// Alternativa: UUID prefixado
// String numBilhete = "APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #009 — Auth.js: resultado de Promise rejeitada em dinamic import de bcryptjs esconde erro
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 517-522
- **Problema:** `const bcrypt = await import('bcryptjs').catch(() => import('bcrypt'))` — se ambos os pacotes falharem o import, `bcrypt` fica undefined e `bcrypt.default.compare(...)` lanca TypeError ao acessar `.default` de undefined.
- **Impacto:** Fluxo de estorno bloqueado sem mensagem util; operador nao sabe se e senha errada ou bug.
- **Codigo problematico:**
```js
const bcrypt = await import('bcryptjs').catch(() => import('bcrypt'))
for (const user of result.rows) {
  if (user.senha && await bcrypt.default.compare(senha, user.senha)) {
```
- **Fix sugerido:**
```js
// Import estatico no topo do arquivo — nao faca lazy/conditional
import bcrypt from 'bcryptjs'
// ...
for (const user of result.rows) {
  if (user.senha && await bcrypt.compare(senha, user.senha)) {
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #011 — ThreadLocal temDataChegadaTL sem cleanup em PassagemDAO
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 184, 256, 272, 343, 377
- **Problema:** `temDataChegadaTL` eh setado em multiplos pontos mas nunca chamado `.remove()`. Em desktop JavaFX isto e menos critico (threads dedicadas), mas no contexto do API Spring (se algum dia este DAO for reusado) pode vazar.
- **Impacto:** Inconsistencias sutis de mapeamento ResultSet quando thread e reciclada com valor antigo.
- **Codigo problematico:**
```java
private final ThreadLocal<Boolean> temDataChegadaTL = ThreadLocal.withInitial(() -> false);
temDataChegadaTL.set(detectarTemDataChegada(rs));
// nunca removido
```
- **Fix sugerido:**
```java
// Melhor: passar como parametro local ao metodo
private Passagem mapResultSetToPassagem(ResultSet rs, boolean temDataChegada) throws SQLException {...}
// ou garantir cleanup em try/finally
try {
    temDataChegadaTL.set(detectarTemDataChegada(rs));
} finally {
    temDataChegadaTL.remove();
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #012 — Validacao assincrona do pagamento ignora erro de rede e deixa estado inconsistente
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 161-196
- **Problema:** Dentro do `@Transactional`, primeiro faz `UPDATE encomendas SET status_pagamento = 'PENDENTE_CONFIRMACAO'` e depois chama `pspService.criar(pspReq)` que faz chamada HTTP externa para Asaas. Se o Asaas retorna timeout/erro, a exception roll-backa o UPDATE — mas se a requisicao PSP FOI BEM-SUCEDIDA e a resposta HTTP nao chegou (timeout de leitura), a cobranca foi criada no Asaas mas o rollback apagou o registro local.
- **Impacto:** Inconsistencia de dados — cobranca criada no PSP sem corresponder a registro local. Em escala vira disputa financeira com cliente.
- **Codigo problematico:**
```java
@Transactional
public Map<String, Object> pagar(Long clienteId, Long idEncomenda, String formaPagamento) {
    // UPDATE local ...
    PspCobranca cob = pspService.criar(pspReq);  // HTTP externo dentro da transacao
    jdbc.update("UPDATE encomendas SET id_transacao_psp = ?, qr_pix_payload = ? ...", ...);
```
- **Fix sugerido:**
```java
// Padrao Outbox ou 2-fase:
// 1. Commit do UPDATE local (status = 'AGUARDANDO_PSP')
// 2. Fora da transacao, chamar PSP
// 3. Segundo UPDATE (transacao curta) gravando psp_id ou revertendo para 'ERRO_PSP'
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #013 — Mesmo bug de outbox em FreteService.pagar
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 139-176
- **Problema:** Identico ao #012 — UPDATE local + chamada PSP externa dentro de `@Transactional` causa dessincronia se resposta HTTP perder.
- **Impacto:** Cobranca Asaas orfa para fretes CNPJ.
- **Codigo problematico:**
```java
@Transactional
public Map<String, Object> pagar(Long clienteId, Long idFrete, String formaPagamento) {
    jdbc.update("UPDATE fretes SET forma_pagamento_app = ?, ...", ...);
    PspCobranca cob = pspService.criar(pspReq);
    jdbc.update("UPDATE fretes SET id_transacao_psp = ?, ...", ...);
```
- **Fix sugerido:** Mesmo padrao outbox do #012.
- **Observacoes:**
> _[espaco]_

---

#### Issue #014 — Mesmo bug em PassagemService.comprar
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 118-166
- **Problema:** INSERT passagem + chamada PSP externa dentro de `@Transactional`. Se PSP retornar com timeout apos sucesso, a transacao da rollback e a passagem local nao existe, porem cobranca Asaas existe.
- **Impacto:** Cliente paga uma cobranca que nao corresponde a passagem alguma.
- **Codigo problematico:**
```java
Long idPassagem = jdbc.queryForObject("INSERT INTO passagens ... RETURNING id_passagem", ...);
PspCobranca cob = pspService.criar(pspReq);
```
- **Fix sugerido:** Outbox pattern.
- **Observacoes:**
> _[espaco]_

---

#### Issue #016 — FreteService.buscarPorRemetenteCrossTenant sem limite nem cache
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 42-80
- **Problema:** Nao filtra empresa_id (comportamento intencional) e usa LIKE em ambas colunas com ORDER BY sem LIMIT.
- **Impacto:** DoS potencial em alto volume.
- **Codigo problematico:**
```java
WHERE UPPER(f.remetente_nome_temp) LIKE UPPER(?)
   OR UPPER(f.destinatario_nome_temp) LIKE UPPER(?)
ORDER BY f.id_frete DESC
```
- **Fix sugerido:** Adicionar `LIMIT 200` e indice funcional sobre UPPER(remetente_nome_temp)/UPPER(destinatario_nome_temp). Mover matching para `id_cliente_app_pagador` quando possivel.
- **Observacoes:**
> _[espaco]_

---

#### Issue #017 — NPE em ClienteApp.getNome() no matching fallback por nome
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 136
- **Problema:** `!destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())` — se `cliente.getNome()` for null, lanca NPE. Mesmo caso em FreteService.pagar linha 113.
- **Impacto:** 500 ao tentar pagar quando nome do cliente esta em branco.
- **Codigo problematico:**
```java
if (destinatario == null || !destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())) {
    throw ApiException.forbidden("Encomenda nao pertence a este cliente");
}
```
- **Fix sugerido:**
```java
String nomeCli = cliente.getNome();
if (nomeCli == null || nomeCli.isBlank()) {
    throw ApiException.forbidden("Conta sem nome cadastrado - complete seu perfil");
}
if (destinatario == null || !destinatario.toUpperCase().contains(nomeCli.toUpperCase())) {
    throw ApiException.forbidden("Encomenda nao pertence a este cliente");
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #019 — setTimeout com setState sem cleanup pode bater em componente unmounted
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/components/PagamentoArtefato.jsx`
- **Linha(s):** 22-23
- **Problema:** `setTimeout(() => setCopiado(""), 2000)` — se usuario clica copiar e navega antes dos 2s, o setTimeout dispara setState em componente desmontado.
- **Impacto:** Warnings em dev; em modo strict possivelmente "Can't perform a React state update on an unmounted component".
- **Codigo problematico:**
```js
navigator.clipboard?.writeText(txt).then(() => {
  setCopiado(label);
  setTimeout(() => setCopiado(""), 2000);
});
```
- **Fix sugerido:**
```js
const timerRef = useRef(null);
useEffect(() => () => { if (timerRef.current) clearTimeout(timerRef.current); }, []);
const copiar = async (...) => {
  setCopiado(label);
  if (timerRef.current) clearTimeout(timerRef.current);
  timerRef.current = setTimeout(() => setCopiado(""), 2000);
};
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #020 — PassagensCPF: confirmarCompra nao trata body nao-JSON no erro
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 34-43
- **Problema:** Se o POST for bem-sucedido no server mas a resposta `.json()` for interrompida (network flaky), o `catch` dispara, o usuario ve "Erro de conexao" E a passagem FOI CRIADA. Clicar novamente cria segunda passagem.
- **Impacto:** Dupla compra quando conexao flaky. Cliente paga duas passagens para a mesma viagem.
- **Codigo problematico:**
```js
const res = await authFetch(`${API}/passagens/comprar`, { method: "POST", ..., body: JSON.stringify({...}) });
const data = await res.json();
if (!res.ok) { setErro(data.erro || "Erro ao comprar."); return; }
```
- **Fix sugerido:**
```js
// Client: gerar idempotency-key e enviar no header
const idempotencyKey = crypto.randomUUID();
const res = await authFetch(`${API}/passagens/comprar`, {
  method: "POST",
  headers: { ...authHeaders, 'Idempotency-Key': idempotencyKey },
  body: JSON.stringify({...})
});
let data;
try { data = await res.json(); } catch { data = {}; }
if (!res.ok) { setErro(data.erro || "Erro ao comprar."); return; }
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #021 — Mesmo problema de dupla chamada em EncomendaCPF.confirmarPagamento
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/EncomendaCPF.jsx`
- **Linha(s):** 29-47
- **Problema:** Idem #020. `authFetch` pode timeout apos server processar com sucesso; usuario retenta e gera segunda cobranca.
- **Impacto:** Cobranca duplicada Asaas em PIX/CARTAO.
- **Fix sugerido:** Idempotency-Key e disable defensivo.
- **Observacoes:**
> _[espaco]_

---

#### Issue #022 — FinanceiroCNPJ.confirmarPagamento sem idempotencia
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/FinanceiroCNPJ.jsx`
- **Linha(s):** 20-38
- **Problema:** Identico a #020/#021.
- **Impacto:** Cobranca duplicada de frete.
- **Fix sugerido:** Idempotency-Key.
- **Observacoes:**
> _[espaco]_

---

#### Issue #023 — `catch {}` generico em confirmarPagamento engole erro util
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/EncomendaCPF.jsx`, `FinanceiroCNPJ.jsx`, `PassagensCPF.jsx`
- **Linha(s):** 46, 37, 42
- **Problema:** `catch { setErrPag("Erro de conexao."); }` sem parametro de erro — impossivel distinguir timeout de CORS de JSON parse error.
- **Impacto:** Depuracao em producao complicada; bugs reais invisiveis.
- **Codigo problematico:**
```js
} catch { setErrPag("Erro de conexao."); }
```
- **Fix sugerido:**
```js
} catch (e) {
  console.error('[EncomendaCPF.pagar]', e);
  setErrPag(e?.message?.includes('Failed to fetch') ? 'Sem conexao.' : 'Erro ao processar.');
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #600 — PassagensCPF: setSelBilhete nao inclui id_passagem — TOTP fetch falha silenciosamente no bilhete digital apos compra
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx` + `naviera-api/.../service/PassagemService.java`
- **Linha(s):** PassagensCPF:69 / PassagemService:167-176
- **Problema:** Apos comprar, o codigo monta `selBilhete` sem `id`. `BilheteScreen.jsx:17-18` verifica `if (!bilhete?.id || !authHeaders?.Authorization) return;` — `fetchTotp` retorna cedo sem gerar TOTP. A response do servidor `PassagemService.comprar` tambem NAO devolve `idPassagem` — apenas numeroBilhete.
- **Impacto:** Cliente que acabou de comprar via app nao consegue embarcar com o bilhete que acabou de emitir (QR/TOTP ficam invalidos no embarque).
- **Codigo problematico:**
```js
setSelBilhete({
  numero_bilhete: resultado.numeroBilhete,
  valor_total: resultado.valorTotal,
  totp_secret: resultado.numeroBilhete // inutil — BilheteScreen nao usa este campo
});
```
```java
Map<String, Object> resp = new java.util.HashMap<>();
resp.put("numeroBilhete", numBilhete);
// falta: resp.put("idPassagem", idPassagem);
```
- **Fix sugerido:**
```java
// server: incluir idPassagem na resposta
resp.put("idPassagem", idPassagem);
```
```js
// client:
setSelBilhete({
  id: resultado.idPassagem,
  id_passagem: resultado.idPassagem,
  numero_bilhete: resultado.numeroBilhete,
});
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #001 — Connection leak em POST /api/financeiro/boleto quando descricao/valor_total ausentes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 374-415
- **Problema:** `pool.connect()` e chamado ANTES da validacao manual. O `client.release()` no `finally` SEMPRE executa, mas em alta concorrencia com queries lentas, cada 400 consome um slot de conexao ate o release (ineficiencia, nao leak real).
- **Impacto:** Ineficiencia sob burst; impacto real pequeno porque o finally libera sempre.
- **Codigo problematico:**
```js
router.post('/boleto', validate({...}), async (req, res) => {
  const client = await pool.connect()
  try {
    const empresaId = req.user.empresa_id
    const { descricao, valor_total, ... } = req.body
    if (!descricao || !valor_total) {
      return res.status(400).json({ error: 'Campos obrigatorios: descricao, valor_total' })
    }
    await client.query('BEGIN')
```
- **Fix sugerido:**
```js
router.post('/boleto', validate({...}), async (req, res) => {
  const empresaId = req.user.empresa_id
  const { descricao, valor_total, ... } = req.body
  if (!descricao || !valor_total) {
    return res.status(400).json({ error: 'Campos obrigatorios: descricao, valor_total' })
  }
  const client = await pool.connect()
  try {
    await client.query('BEGIN')
  } finally { client.release() }
})
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #002 — Connection leak em POST /api/financeiro/boleto/batch com parcelas invalidas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 418-434
- **Problema:** Mesmo padrao do #001. Finally libera, entao ineficiencia ao inves de leak.
- **Impacto:** Ineficiencia sob payloads invalidos em burst.
- **Fix sugerido:** Mover `pool.connect()` depois das checagens de validacao.
- **Observacoes:**
> _[espaco]_

---

#### Issue #015 — rastreioCrossTenant executa LIKE sem limite, vulneravel a full-table scan
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 86-104
- **Problema:** Query roda LIKE cross-tenant em toda a tabela `encomendas` (sem filtro de empresa_id, ORDER BY sem LIMIT).
- **Impacto:** DoS potencial em volume alto (~1M linhas). MVP atual nao atinge.
- **Codigo problematico:**
```java
String sql = """
    SELECT e.id_encomenda, ...
    FROM encomendas e ...
    WHERE UPPER(e.destinatario) LIKE UPPER(?)
       OR UPPER(e.remetente) LIKE UPPER(?)
    ORDER BY e.id_encomenda DESC
    """;
```
- **Fix sugerido:**
```java
sql += " LIMIT 200";
// E idealmente criar index funcional: CREATE INDEX ... ON encomendas (UPPER(destinatario))
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #018 — Promises sem catch em PagamentoArtefato ao copiar clipboard
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/components/PagamentoArtefato.jsx`
- **Linha(s):** 21-25
- **Problema:** `navigator.clipboard?.writeText(txt).then(() => {...})` — sem `.catch()`. Em browsers que bloqueiam clipboard (HTTP, iframe, permission denied), a Promise rejeita e vira unhandledrejection.
- **Impacto:** Console errors; mobile antigo trava o botao.
- **Codigo problematico:**
```js
const copiar = (txt, label) => {
  if (!txt) return;
  navigator.clipboard?.writeText(txt).then(() => {
    setCopiado(label);
    setTimeout(() => setCopiado(""), 2000);
  });
};
```
- **Fix sugerido:**
```js
const copiar = async (txt, label) => {
  if (!txt) return;
  try {
    if (!navigator.clipboard?.writeText) {
      const ta = document.createElement('textarea');
      ta.value = txt; document.body.appendChild(ta);
      ta.select(); document.execCommand('copy'); document.body.removeChild(ta);
    } else {
      await navigator.clipboard.writeText(txt);
    }
    setCopiado(label);
    setTimeout(() => setCopiado(""), 2000);
  } catch (e) {
    console.warn('[PagamentoArtefato] clipboard indisponivel', e);
  }
};
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #024 — Cache de tenant no middleware nunca expira por invalidacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 4-58
- **Problema:** Cache `Map` de slug->empresa tem TTL 60s mas nao tem cap em tamanho. Empresa desativada fica acessivel por ate 60s.
- **Impacto:** Empresa desativada fica acessivel por ate 60s.
- **Fix sugerido:**
```js
const MAX_CACHE = 500
if (cache.size >= MAX_CACHE) {
  const oldestKey = cache.keys().next().value;
  cache.delete(oldestKey);
}
cache.set(cacheKey, { data: empresa, ts: Date.now() })
// E endpoint admin para invalidar: DELETE /api/admin/tenant-cache/:slug
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #025 — `parseInt(req.query.limit)` pode retornar NaN e virar 500
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 42-44
- **Problema:** `parseInt('-10')` da -10 que NAO e falsy, entao o LIMIT vira -10 (erro SQL).
- **Impacto:** SQL error 500 quando client envia negatives.
- **Codigo problematico:**
```js
const limit = Math.min(parseInt(req.query.limit) || 500, 1000)
const offset = parseInt(req.query.offset) || 0
sql += ` LIMIT ${limit} OFFSET ${offset}`
```
- **Fix sugerido:**
```js
const limit = Math.min(Math.max(parseInt(req.query.limit) || 500, 1), 1000)
const offset = Math.max(parseInt(req.query.offset) || 0, 0)
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #026 — useWebSocket cria cliente novo a cada change de reconnectDelay
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/hooks/useWebSocket.js`
- **Linha(s):** 15, 31, 56, 73
- **Problema:** O valor `reconnectDelay` e lido UMA VEZ na criacao. Apos isso, o backoff setado pelo ref nao afeta o comportamento do STOMP client.
- **Impacto:** Reconnect continua a cada 2s mesmo em falhas repetidas.
- **Fix sugerido:**
```js
onStompError: (frame) => {
  reconnectDelay.current = Math.min(reconnectDelay.current * 2, 30000);
  if (client.reconnectDelay !== undefined) {
    client.reconnectDelay = reconnectDelay.current;
  }
},
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #027 — Timer do rateLimit em memoria cresce sem bound por keyFn
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/rateLimit.js`
- **Linha(s):** 7-37
- **Problema:** `Map hits` armazena 1 entry por `req.ip`. Com 100k IPs distintos em 60s, o Map tem 100k entries.
- **Impacto:** Memoria cresce em spikes de trafego.
- **Fix sugerido:** Impor cap `if (hits.size > 10000) hits.clear()` ou migrar para Redis.
- **Observacoes:**
> _[espaco]_

---

#### Issue #029 — EncomendaService.pagar: possivel duplicacao de psp_cobrancas em retry
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 142-166
- **Problema:** A primeira chamada rollbackou = status volta a "PENDENTE" = segunda chamada passa no check E chama criar() de novo no PSP. Cria 2 cobrancas.
- **Impacto:** Duplicacao de cobranca Asaas quando PSP lenta.
- **Fix sugerido:** Criar tabela `psp_requests_idempotency(cliente_id, tipo_origem, origem_id, created_at)` com UNIQUE constraint.
- **Observacoes:**
> _[espaco]_

---

#### Issue #030 — AsaasGateway.post()/get() nao valida status HTTP, trata 4xx/5xx como sucesso
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 228-244
- **Problema:** `rest.exchange(...)` com RestTemplate sem timeout configurado — em producao pode bloquear por default. `mapper.readTree(res.getBody())` se body for null lanca NPE.
- **Impacto:** Asaas lento deixa threads do Tomcat bloqueadas indefinidamente.
- **Fix sugerido:**
```java
public AsaasGateway(AsaasProperties props, RestTemplateBuilder builder) {
    this.rest = builder
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(30))
        .build();
}
String body = res.getBody();
if (body == null || body.isBlank()) throw new IllegalStateException("Asaas retornou corpo vazio");
return mapper.readTree(body);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #031 — AsaasGateway.obterOuCriarCustomer concatena cpfCnpj em URL sem encoding
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 216-218
- **Problema:** `get("/customers?cpfCnpj=" + req.cpfCnpjPagador())` — se contem `/` ou `.`, quebra URL.
- **Impacto:** Cliente existente nao encontrado; duplicacao de customer.
- **Fix sugerido:**
```java
String cpfCnpj = req.cpfCnpjPagador() != null
    ? req.cpfCnpjPagador().replaceAll("\\D", "")
    : "";
JsonNode list = get("/customers?cpfCnpj=" + URLEncoder.encode(cpfCnpj, StandardCharsets.UTF_8));
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #033 — FreteService/EncomendaService nao valida forma_pagamento contra whitelist
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java` + `EncomendaService.java`
- **Linha(s):** FreteService 96, EncomendaService 120
- **Problema:** `String forma = formaPagamento != null ? formaPagamento : "PIX"` — aceita qualquer string do cliente.
- **Impacto:** Input invalido causa 500 com mensagem ruim.
- **Fix sugerido:**
```java
String forma = formaPagamento != null ? formaPagamento.toUpperCase() : "PIX";
Set<String> validas = Set.of("PIX", "CARTAO", "BOLETO", "BARCO");
if (!validas.contains(forma)) throw ApiException.badRequest("Forma de pagamento invalida: " + forma);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #036 — useNotifications: exception de SW register nao tratada por branch
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/hooks/useNotifications.js`
- **Linha(s):** 76
- **Problema:** `const swReg = await navigator.serviceWorker.register(...)` — se o SW nao existir, lanca TypeError que vai para o catch de fora.
- **Impacto:** Mensagem de erro confusa.
- **Fix sugerido:**
```js
let swReg;
try {
  swReg = await navigator.serviceWorker.register("/firebase-messaging-sw.js");
} catch (e) {
  console.warn("[Notificacoes] SW register falhou:", e);
  return null;
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #034 — Session storage em usePWA pode lancar em Safari private mode
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/hooks/usePWA.js`
- **Linha(s):** 6-9, 52
- **Problema:** `sessionStorage.getItem/setItem` ja esta em try/catch, mas em Safari Private Mode muitas versoes antigas nao lancam — apenas falham silenciosamente.
- **Impacto:** UX degradada em Safari Private.
- **Fix sugerido:** Aceitavel. Marcado como baixo.
- **Observacoes:**
> _[espaco]_

---

#### Issue #035 — PassagensCPF: selBilhete construido com totp_secret igual ao numeroBilhete
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 69
- **Problema:** `totp_secret: resultado.numeroBilhete` — o secret TOTP esta sendo igualado ao numero do bilhete, que e publico.
- **Impacto:** Qualquer um com o numero pode forjar TOTP.
- **Fix sugerido:** Backend deve retornar um `totp_secret` real (nao o numeroBilhete) e o front usa esse.
- **Observacoes:**
> _[espaco]_

---

#### Issue #037 — BilheteScreen faz destructuring sem fallback em selBilhete
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 48
- **Problema:** Passa `bilhete={selBilhete}` para `BilheteScreen` — se o componente filho faz destructuring sem defaults, o usuario pode ver crash.
- **Impacto:** Baixo.
- **Fix sugerido:** Verificar BilheteScreen.
- **Observacoes:**
> _[espaco]_

---

#### Issue #038 — ConexaoBD: Thread.sleep sem interrupt handling em loop de retry
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 132-163
- **Problema:** Loop `while` com `System.currentTimeMillis()` nao e interrumpivel.
- **Impacto:** Muito baixo.
- **Fix sugerido:**
```java
while (System.currentTimeMillis() < deadline) {
    if (Thread.currentThread().isInterrupted()) throw new SQLException("Interrompido");
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #601 — HashMap com chaves null em responses PSP
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 167-176
- **Problema:** Quando `cob.getQrCodePayload()` retorna null, `resp.put("qrCodePayload", null)` grava chave com valor null. Jackson serializa como `"qrCodePayload":null`. Em clientes React strict-typed, pode falhar. Aplicavel tambem ao `EncomendaService.pagar` e `FreteService.pagar`.
- **Impacto:** Baixo; fragilidade contra mudanca de frontend.
- **Fix sugerido:** Adotar DTO tipado conforme TODO DM069 ja marcado no codigo.
- **Observacoes:**
> _[espaco]_

---

### 2.2 — Seguranca

#### Issue #100 — Qualquer admin de empresa pode modificar/criar/ativar qualquer outra empresa
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/AdminController.java` + `naviera-api/src/main/java/com/naviera/api/config/SecurityConfig.java`
- **Linha(s):** AdminController 17-42; SecurityConfig 29
- **Problema:** Em `SecurityConfig` o rotulo `ROLE_ADMIN` e concedido pelo `JwtFilter` a TODO operador cuja claim `funcao == Administrador/ADMIN` — ou seja, cada empresa tem seu proprio admin e todos tem a mesma role. O `AdminController` executa operacoes sobre `Long id` sem qualquer verificacao de que o admin logado pertence aquela empresa.
- **Impacto:** Sequestro total do SaaS. Admin cliente X ativa uma empresa concorrente como suya; desativa empresa concorrente; rouba `codigo_ativacao`; muda slug.
- **Codigo problematico:**
```java
.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

if (funcao != null && ("ADMIN".equalsIgnoreCase(funcao) || "Administrador".equalsIgnoreCase(funcao))) {
    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
}

@PutMapping("/empresas/{id}")
public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
    return ResponseEntity.ok(service.atualizarEmpresa(id, dados));
}
```
- **Fix sugerido:**
```sql
-- Migration:
ALTER TABLE usuarios ADD COLUMN super_admin BOOLEAN DEFAULT FALSE;
-- Seed manual do 1o super-admin:
UPDATE usuarios SET super_admin = TRUE WHERE id = <id_do_dev>;
```
```java
// JwtFilter: conceder ROLE_SUPERADMIN apenas se super_admin=TRUE no DB (nao usar claim funcao)
// SecurityConfig:
.requestMatchers("/admin/**").hasAuthority("ROLE_SUPERADMIN")
// Manter ROLE_ADMIN para admin-empresa (escopo /op/* e /psp/* da propria empresa)
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #102 — Qualquer operador autenticado pode se auto-promover a Administrador (BFF)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/cadastros.js`
- **Linha(s):** 369-390
- **Problema:** A rota `PUT /api/cadastros/usuarios/:id` aceita um campo `funcao` no body e o aplica diretamente via UPDATE. Nao ha verificacao de role — basta que o usuario esteja autenticado e o alvo pertenca a mesma empresa. Um operador comum pode enviar `PUT /api/cadastros/usuarios/<meuId>` com `{"funcao":"Administrador"}` e ganhar `ROLE_ADMIN` no proximo login.
- **Impacto:** Escalacao de privilegio imediata + tomada de conta de admin legitimo via senha reset.
- **Codigo problematico:**
```javascript
router.put('/usuarios/:id', async (req, res, next) => {
  try {
    const empresaId = req.user.empresa_id
    const { nome, email, funcao, permissao, senha } = req.body
    // NENHUMA checagem de role/ownership
    sql = 'UPDATE usuarios SET ... funcao = COALESCE($3, funcao), ... senha = $5 WHERE id = $6 AND empresa_id = $7'
```
- **Fix sugerido:**
```javascript
router.put('/usuarios/:id', async (req, res, next) => {
  const funcaoLogada = (req.user.funcao || '').toLowerCase()
  if (!['administrador', 'admin'].includes(funcaoLogada)) {
    return res.status(403).json({ error: 'Apenas Administrador pode editar usuarios' })
  }
  if (parseInt(req.params.id) === req.user.id && req.body.funcao) {
    return res.status(400).json({ error: 'Nao pode alterar propria funcao' })
  }
})
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #103 — CadastrosWriteService (API) permite escalacao de privilegios via `PUT /op/cadastros/usuarios/{id}`
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/CadastrosWriteService.java`
- **Linha(s):** 112-130 + `controller/CadastrosController.java` 118-125
- **Problema:** Mesma falha que #102 agora no backend Spring. Qualquer operador cria um usuario Administrador.
- **Impacto:** Escalacao de privilegio; acesso a modulos sensiveis.
- **Codigo problematico:**
```java
@Transactional
public Map<String, Object> criarUsuario(Integer empresaId, Map<String, Object> dados) {
    jdbc.update("""
        INSERT INTO usuarios (nome, email, senha, funcao, permissao, empresa_id)
        VALUES (?, ?, ?, ?, ?, ?)""",
        dados.get("nome"), dados.get("email"), passwordEncoder.encode((String) dados.get("senha")),
        dados.get("funcao"), dados.get("permissao"), empresaId);
```
- **Fix sugerido:**
```java
public Map<String, Object> criarUsuario(Integer empresaId, Map<String, Object> dados, Authentication auth) {
    if (!isAdmin(auth)) throw ApiException.forbidden("Apenas administradores podem criar usuarios");
    String funcaoAlvo = (String) dados.getOrDefault("funcao", "OPERADOR");
    if ("Administrador".equalsIgnoreCase(funcaoAlvo) && !isSuperAdmin(auth)) {
        // bloquear ou exigir 2FA
    }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #105 — Endpoint /rotas vaza todas as rotas de todas empresas (cross-tenant)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/RotaController.java`
- **Linha(s):** 12-14
- **Problema:** Controller faz `repo.findAll()` sem filtrar por empresa_id. Endpoint sob `anyRequest().authenticated()`, entao qualquer cliente le rotas de concorrentes.
- **Impacto:** Concorrentes mapeiam rotas operadas por cada empresa apenas autenticando-se. Violacao direta da Regra #1 do CLAUDE.md.
- **Codigo problematico:**
```java
@RestController @RequestMapping("/rotas")
public class RotaController {
    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(repo.findAll()); }
}
```
- **Fix sugerido:**
```java
@GetMapping
public ResponseEntity<?> listar(Authentication auth) {
    Integer empresaId = TenantUtils.getEmpresaIdOrNull(auth);
    if (empresaId != null) return ResponseEntity.ok(repo.findByEmpresaId(empresaId));
    throw ApiException.forbidden("Endpoint restrito a operadores");
}
// RotaRepository: adicionar List<Rota> findByEmpresaId(Integer empresaId);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #106 — GET /api/encomendas/:id/itens e GET /api/fretes/:id/itens no BFF vazam itens cross-tenant
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/encomendas.js` (65-75); `naviera-web/server/routes/fretes.js` (141-151)
- **Problema:** Ambos endpoints fazem `SELECT * FROM encomenda_itens WHERE id_encomenda = $1` / `SELECT * FROM frete_itens WHERE id_frete = $1` sem JOIN/check de `empresa_id`.
- **Impacto:** Cross-tenant data leak — todos os itens de todas as empresas estao expostos.
- **Codigo problematico:**
```javascript
router.get('/:id/itens', async (req, res) => {
  const result = await pool.query(
    'SELECT * FROM encomenda_itens WHERE id_encomenda = $1 ...',
    [req.params.id]   // SEM empresa_id
  )
})
```
- **Fix sugerido:**
```javascript
router.get('/:id/itens', async (req, res) => {
  const empresaId = req.user.empresa_id
  const result = await pool.query(
    `SELECT ei.* FROM encomenda_itens ei
     JOIN encomendas e ON ei.id_encomenda = e.id_encomenda
     WHERE ei.id_encomenda = $1 AND e.empresa_id = $2
       AND (ei.excluido = FALSE OR ei.excluido IS NULL)
     ORDER BY ei.id_item_encomenda`,
    [req.params.id, empresaId]
  )
})
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #107 — Ownership weak em pagamento de encomenda/frete — match por nome permite pagar encomenda alheia
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java` (116-139); `FreteService.java` (92-117)
- **Problema:** Em `pagar()`, quando `id_cliente_app_destinatario` e null, o fallback valida ownership comparando se `destinatario` contem o nome do cliente (case-insensitive, `contains`). Query nao filtra por empresa_id. Atacante cadastra cliente_app com nome "Silva" e paga/sequestra TODAS as encomendas com destinatario contendo "Silva" no SaaS inteiro.
- **Impacto:** Atacante sequestra encomendas de homonimos. Pode pagar por BARCO (sem cobranca do PSP) para so vincular a conta.
- **Codigo problematico:**
```java
var rows = jdbc.queryForList(
    "SELECT id_encomenda, total_a_pagar, desconto, valor_pago, status_pagamento, " +
    "destinatario, id_cliente_app_destinatario, empresa_id " +
    "FROM encomendas WHERE id_encomenda = ?",    // SEM empresa_id filter
    idEncomenda);
} else {
    String destinatario = (String) enc.get("destinatario");
    if (destinatario == null || !destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())) {
        throw ApiException.forbidden("Encomenda nao pertence a este cliente");
    }
}
```
- **Fix sugerido:**
```java
// Exigir MATCH EXATO de id_cliente_app_destinatario (sem fallback por nome)
if (donoFk == null) throw ApiException.forbidden(
    "Encomenda nao vinculada ao seu cadastro. Pecam ao remetente para vincular ou retire no balcao com documento.");
if (!donoFk.equals(clienteId)) throw ApiException.forbidden("Encomenda nao pertence a este cliente");
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #108 — Login BFF aceita usuario de qualquer empresa em dev — risco de deploy mal configurado em producao
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 47-58
- **Problema:** Se `NODE_ENV !== 'production'` e nao ha `req.tenant`, o endpoint `/api/auth/login` autentica usuario de QUALQUER empresa apenas pelo `login`. Basta subir o BFF sem `NODE_ENV=production`. Tambem o header `X-Tenant-Slug` pode ser forjado.
- **Impacto:** Credential stuffing cross-tenant.
- **Codigo problematico:**
```javascript
const isOcrApp = /ocr\.naviera\.com\.br/.test(origin) || req.headers['x-tenant-slug'] === 'ocr'
const isAdminApp = /admin\.naviera\.com\.br/.test(origin) || req.headers['x-tenant-slug'] === 'admin'
} else if (process.env.NODE_ENV === 'production') {
  return res.status(400).json({ error: 'Subdominio da empresa obrigatorio' })
} else {
  sql = `SELECT ... FROM usuarios WHERE ...`
}
```
- **Fix sugerido:**
```javascript
// 1. Deletar header X-Tenant-Slug quando req.ip != loopback (tenant.js)
// 2. Exigir ALLOW_DEV_LOGIN=1 em env para aceitar login sem tenant
// 3. Abortar boot se NODE_ENV=production && !CORS_ORIGINS.startsWith('https://')
const fromTrustedProxy = req.ip === '127.0.0.1' || req.ip === '::1' || req.ip?.startsWith('::ffff:127.')
if (req.headers['x-tenant-slug'] && !fromTrustedProxy) {
  delete req.headers['x-tenant-slug']
}
if (!req.tenant && !process.env.ALLOW_DEV_LOGIN) {
  return res.status(400).json({ error: 'Subdominio da empresa obrigatorio' })
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #114 — AdminController (Spring) nao valida role — ativa/edita empresa com qualquer operador
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/AdminController.java`
- **Linha(s):** 17-42
- **Problema:** Embora a rota este sob `/admin/**` com `hasAuthority("ROLE_ADMIN")`, a flag `ROLE_ADMIN` e atribuida a qualquer operador com `funcao=Administrador`. Nao ha check contra admin-super vs admin-empresa. `PutMapping("/empresas/{id}/ativar")` permite qualquer admin desativar empresa concorrente.
- **Impacto:** Desativar concorrente em massa. Mesma raiz do #100 com vetor extra de impacto imediato em producao.
- **Codigo problematico:**
```java
@PutMapping("/empresas/{id}/ativar")
public ResponseEntity<?> ativar(@PathVariable Long id, @RequestBody Map<String, Object> dados) {
    boolean ativo = Boolean.TRUE.equals(dados.get("ativo"));
    return ResponseEntity.ok(service.ativarEmpresa(id, ativo));
}
```
- **Fix sugerido:** Mesma solucao do #100 — coluna `usuarios.super_admin` + JwtFilter conceder `ROLE_SUPERADMIN` so se flag=TRUE + `SecurityConfig` usar `hasAuthority("ROLE_SUPERADMIN")` em `/admin/**`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #650 — tenantMiddleware aceita header X-Tenant-Slug diretamente do cliente sem validar origem trusted-proxy
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 17-18
- **Problema:** O middleware le `req.headers['x-tenant-slug']` sem validar se a request veio do Nginx. `trust proxy = 'loopback'` so afeta `req.ip`; nao bloqueia headers. Um atacante que consiga falar direto com o BFF envia `curl -H "X-Tenant-Slug: empresa-alvo"` e o middleware aceita.
- **Impacto:** Bypass de isolamento multi-tenant na camada de rede antes mesmo de auth. Combina com #108 para credential stuffing direcionado.
- **Codigo problematico:**
```javascript
let slug = req.headers['x-tenant-slug']
```
- **Fix sugerido:**
```javascript
const fromTrustedProxy = req.ip === '127.0.0.1' || req.ip === '::1' || req.ip?.startsWith('::ffff:127.')
let slug = fromTrustedProxy ? req.headers['x-tenant-slug'] : null
// Nginx continua setando via proxy_set_header X-Tenant-Slug
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #101 — Segredos sensiveis em arquivos .env fora de controle (plain text no disco)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/.env`, `naviera-api/.env`
- **Problema:** Os arquivos `.env` estao no `.gitignore` mas existem no disco com valores reais e fracos: `DB_PASSWORD=123456`, `JWT_SECRET=naviera-jwt-secret-dev-2026`, chaves REAIS Google (`GOOGLE_CLOUD_VISION_API_KEY`, `GEMINI_API_KEY`). .env NAO commitado no git (verificado).
- **Impacto:** JWT forjavel; credit card draining; senha DB trivial.
- **Codigo problematico:**
```
DB_PASSWORD=123456
JWT_SECRET=naviera-jwt-secret-dev-2026
GOOGLE_CLOUD_VISION_API_KEY=AIzaSyAoqbEWTtv5DaVrGK2RAnm4M8xDbqKDwTg
GEMINI_API_KEY=AIzaSyAKOc7s7VRXgwy9J9CZ-tyjTMlEi1KPNXw
```
- **Fix sugerido:**
```
1. Rotacionar JA as duas chaves Google no GCP Console (revogar + emitir novas)
2. Gerar JWT_SECRET novo: `openssl rand -hex 64`
3. Trocar DB_PASSWORD para senha forte (>= 32 chars random)
4. Verificar `git log --all -- naviera-web/.env` para confirmar que nunca foi commitado
5. Mover segredos para vault (AWS Secrets Manager, Doppler)
6. Adicionar pre-commit hook para bloquear `AIzaSy*` e padrao `JWT_SECRET=`
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #104 — PSP onboarding permite que operador nao-admin cadastre subconta Asaas com dados arbitrarios
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/PspController.java` + `psp/EmpresaPspService.java`
- **Linha(s):** PspController 31-35; EmpresaPspService 48-97
- **Problema:** Endpoint `POST /psp/onboarding` so exige `Authentication` (qualquer operador autenticado). Nao ha check de role admin. AdminPspController JA valida ROLE_ADMIN, porem PspController opera sobre `empresaId = TenantUtils.getEmpresaId(auth)` — operador so consegue onboardar a propria empresa, o que e diferente do cenario original descrito. Risco residual: operador NAO-admin cria subconta em nome da sua propria empresa, desviando pagamentos.
- **Impacto:** Fraude pontual dentro da propria empresa.
- **Codigo problematico:**
```java
@PostMapping("/onboarding")
public ResponseEntity<?> onboarding(@RequestBody @Valid OnboardingRequest req, Authentication auth) {
    Integer empresaId = TenantUtils.getEmpresaId(auth);  // qualquer operador
    return ResponseEntity.ok(service.onboarding(empresaId, req));
}
```
- **Fix sugerido:**
```java
@PostMapping("/onboarding")
public ResponseEntity<?> onboarding(@RequestBody @Valid OnboardingRequest req, Authentication auth) {
    requireAdmin(auth);  // mesmo helper usado no AdminPspController
    Integer empresaId = TenantUtils.getEmpresaId(auth);
    // Validar no EmpresaPspService que empresa.ativo = TRUE antes do onboarding
    var empresa = jdbc.queryForMap("SELECT cnpj, ativo FROM empresas WHERE id = ?", empresaId);
    if (!Boolean.TRUE.equals(empresa.get("ativo"))) throw ApiException.badRequest("Empresa inativa");
    if (!Objects.equals(empresa.get("cnpj"), req.cnpj()))
        throw ApiException.badRequest("CNPJ nao bate com cadastro da empresa");
    return ResponseEntity.ok(service.onboarding(empresaId, req));
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #109 — JWT_SECRET fraco (dev secret "naviera-jwt-secret-dev-2026") em uso
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/.env`, `naviera-api/.env`
- **Problema:** Secret tem baixa entropia (ASCII previsivel, 34 chars, sem aleatoriedade) e diferentes entre API e web. Atacante pode fazer HMAC-SHA256 cracking.
- **Impacto:** Forja de JWT para qualquer usuario/empresa.
- **Fix sugerido:**
```bash
openssl rand -hex 64
# Usar >= 32 bytes (256 bits) e mesmo valor para API e BFF
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #110 — Rate limit no /auth/login usa IP fraco — sem protecao contra credential stuffing distribuido
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/RateLimitFilter.java` + `naviera-web/server/middleware/rateLimit.js`
- **Problema:** Rate limit e por IP (10/min). Credential stuffing moderno usa milhares de IPs proxy. Nao ha rate-limit por conta/login, CAPTCHA/MFA, account lockout, detection de "muitos logins falhando com usuarios diferentes".
- **Impacto:** Atacante com botnet testa dumps de senhas publicas sem impedimento pratico.
- **Fix sugerido:**
```java
// Adicionar bucket por conta-tentada (ler body)
// Usar Redis compartilhado
// Implementar account lockout: apos 5 senhas invalidas, bloquear login por 15min
// Adicionar hCaptcha/reCAPTCHA v3 no login
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #111 — CORS permite credentials com origin fixo mas sem garantia de que lista nao contem *
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/CorsConfig.java`
- **Linha(s):** 10-15
- **Problema:** `setAllowedOrigins` combinado com `setAllowCredentials(true)` e vulneravel se CORS_ORIGINS=*. Nao usa `setAllowedOriginPatterns` para hostnames dinamicos.
- **Impacto:** Atacante faz fetch com credentials include se env mal configurada.
- **Fix sugerido:**
```java
for (String o : origins) {
    if ("*".equals(o)) throw new IllegalStateException("CORS_ORIGINS=* com allowCredentials=true e inseguro");
}
c.setAllowedOriginPatterns(Arrays.asList("https://*.naviera.com.br"));
c.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Tenant-Slug"));
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #112 — `AsaasGateway.validarAssinaturaWebhook` retorna `true` quando secret nao configurado
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 191-209
- **Problema:** Quando `webhookSecret` esta em branco, retorna `true` — aceita qualquer payload sem validacao. Atacante envia POST dizendo "pagamento CONFIRMADO" e marca passagens como PAGO sem ter pago.
- **Impacto:** Fraude financeira — tickets/encomendas/fretes PAGOS sem transacao real.
- **Codigo problematico:**
```java
public boolean validarAssinaturaWebhook(String payload, String assinatura) {
    String secret = props.getAsaas().getWebhookSecret();
    if (isBlank(secret)) {
        log.warn("[AsaasGateway] webhook-secret nao configurado — aceitando webhook sem validacao");
        return true;  // CRITICAL FAIL-OPEN
    }
```
- **Fix sugerido:**
```java
public boolean validarAssinaturaWebhook(String payload, String assinatura) {
    String secret = props.getAsaas().getWebhookSecret();
    String profile = System.getProperty("spring.profiles.active", "");
    if (isBlank(secret)) {
        if (profile.contains("prod")) {
            log.error("[AsaasGateway] webhook-secret NAO CONFIGURADO em profile de producao — rejeitando todos os webhooks");
            return false;   // FAIL-CLOSED em prod
        }
        log.warn("[AsaasGateway] webhook-secret nao configurado (profile={}) — aceitando sem validacao", profile);
        return true;
    }
    // HMAC check
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #113 — Endpoint publico /public/ativar/{codigo} sujeito a brute-force (4-6 hex = 65k-16M possibilidades)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PublicController.java` + `service/AdminService.java`
- **Linha(s):** PublicController 47-50; AdminService 46
- **Problema:** `AdminService` gera `codigo_ativacao` com apenas 4 hex (65535 combinacoes — brute-forcable em minutos). Endpoint publico sem auth, retorna `{empresa_id, nome, slug, operador_nome}`.
- **Impacto:** Atacante brute-forca codigo e ativa copia pirata ou enumera empresas.
- **Codigo problematico:**
```java
String codigo = "NAV-" + String.format("%04X", RANDOM.nextInt(0xFFFF));
```
- **Fix sugerido:**
```java
// Extrair para CodigoAtivacaoGenerator utility (compartilhado com OnboardingService.gerarCodigoAtivacao)
String codigo = "NAV-" + String.format("%08X", RANDOM.nextInt());
// + bucket rate-limit dedicado "ativar:" em RateLimitFilter (max=5/min/IP)
// + captcha no primeiro hit
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #115 — `POST /api/financeiro/estornar` nao exige senha do autorizador (rota paralela insegura)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 530-580
- **Problema:** O BFF expoe DUAS rotas para estorno: `/api/estornos/passagem/:id` EXIGE senha_autorizador; `/api/financeiro/estornar` aceita apenas `{tipo, id, motivo, autorizador}` sem senha — `autorizador` e string livre.
- **Impacto:** Fraude — operador zera pagamento e fica com dinheiro em especie.
- **Codigo problematico:**
```javascript
router.post('/estornar', async (req, res) => {
  const { tipo, id, motivo, autorizador } = req.body
  if (!tipo || !id || !motivo || !autorizador) return res.status(400)...
  // SEM validacao de senha
  await client.query('UPDATE passagens SET valor_pago = 0 ...')
})
```
- **Fix sugerido:** Remover esta rota em favor de `/api/estornos/*` que ja valida senha. Ou adicionar a mesma validacao aqui e restringir a Administrador/Gerente.
- **Observacoes:**
> _[espaco]_

---

#### Issue #118 — JWT nao tem revogacao (stateless) — troca de senha nao invalida tokens emitidos
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/auth.js` + `naviera-api/.../service/AuthService.java`
- **Problema:** Apos `POST /api/auth/trocar-senha`, tokens anteriores continuam validos ate expiracao natural (8h).
- **Impacto:** Window de 8h apos vazamento de token.
- **Fix sugerido:**
```javascript
// 1. Adicionar coluna usuarios.password_changed_at
await pool.query('UPDATE usuarios SET senha = $1, password_changed_at = NOW() WHERE id = $2', ...)
// 2. authMiddleware rejeita tokens com iat < password_changed_at:
const user = await pool.query('SELECT password_changed_at FROM usuarios WHERE id = $1', [decoded.id])
if (user.rows[0]?.password_changed_at && new Date(decoded.iat * 1000) < user.rows[0].password_changed_at) {
  return res.status(401).json({ error: 'Token invalidado por troca de senha' })
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #119 — Gemini API key logada/potencialmente vazavel em erros
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/ocr.js` (716); `naviera-web/server/routes/documentos.js` (73)
- **Problema:** A URL enviada ao `fetchWithRetry` contem a key como query string. Se `fetchWithRetry` logar URLs em falha, a key vaza em logs.
- **Impacto:** Chave Google Gemini em logs = credit card draining.
- **Codigo problematico:**
```javascript
const geminiRes = await fetchRetry(
  `https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=${apiKey}`,
  { ... }
)
```
- **Fix sugerido:**
```javascript
const urlSafe = url.replace(/([?&])key=[^&]+/g, '$1key=REDACTED')
log.error('Gemini', urlSafe, { erro: err.message })
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #120 — Verificacao de `funcao` baseada em string (case-insensitive) mas grafia variavel — bypass por mutacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../security/JwtFilter.java`; `naviera-web/server/routes/admin.js`; `naviera-web/server/routes/ocr.js`
- **Problema:** Multiple checks aceitam variacoes de string (`"ADMIN"`, `"Administrador"`, etc). `funcao="Admin "` com espaco ou `funcao="ADMINISTRADOR"` podem passar em uma camada mas nao em outra.
- **Impacto:** Bypass de checagem em alguma camada.
- **Fix sugerido:** Criar enum central (Java + JS), validar em cada layer, adicionar CHECK constraint no PostgreSQL em `funcao IN ('Administrador','Gerente','Caixa','Vendedor','Operador')`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #121 — Auth BFF nao remove/strip JWT de logs de erro — req.body pode conter senha em `/trocar-senha`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/middleware/errorHandler.js`; `naviera-web/server/index.js` (50-58)
- **Problema:** Request logging loga method, url e status. `unhandledRejection` handler loga o reason completo — pode incluir bcrypt stack trace com buffer de senha.
- **Impacto:** Senhas em plaintext em arquivos de log.
- **Fix sugerido:** Redactar body/query de rotas sensiveis antes de qualquer log. Pino/winston com serializers.
- **Observacoes:**
> _[espaco]_

---

#### Issue #651 — CORS default do Spring Boot inclui `localhost` em producao se env CORS_ORIGINS nao for setada
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/resources/application.properties`
- **Linha(s):** 44
- **Problema:** Se Docker/systemd nao propagar `CORS_ORIGINS`, Spring assume defaults localhost, combinado com `setAllowCredentials(true)` em producao aceita cookies de localhost.
- **Impacto:** Em producao com env var vazia, CORS aceita chamadas de localhost com credenciais.
- **Fix sugerido:**
```java
@PostConstruct
void validate() {
    if (origins.length == 3 && origins[0].startsWith("http://localhost")) {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if ("prod".equals(env) || "production".equals(env)) {
            throw new IllegalStateException("CORS_ORIGINS nao configurado em producao");
        }
    }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #655 — Desativacao de empresa nao invalida tokens emitidos dos usuarios dessa empresa
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AdminService.java` + `naviera-web/server/middleware/tenant.js`
- **Problema:** Admin desativa empresa — `empresas.ativo = FALSE` e gravado. Porem: tokens JWT continuam validos por ate 24h, cache tenant por ate 60s, JwtFilter nao consulta `empresas.ativo`.
- **Impacto:** Controle de SaaS inutil — desativar empresa nao para ela.
- **Fix sugerido:** Adicionar check `empresas.ativo = TRUE` no `JwtFilter` (cache curto, 30s) OU usar `empresa_desativada_em` para invalidar tokens com `iat < empresa_desativada_em`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #656 — `CadastrosWriteService.criarUsuario` nao valida se email ja existe na empresa antes do INSERT
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/CadastrosWriteService.java`
- **Linha(s):** 111-119
- **Problema:** INSERT sem checar duplicata. Se ha constraint UNIQUE(email), estoura SQLException com mensagem quebrada. Se nao ha, email duplica e login fica ambiguo.
- **Fix sugerido:** Pre-check `SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?)` antes do INSERT, lancar ApiException.conflict.
- **Observacoes:**
> _[espaco]_

---

#### Issue #658 — `psp/onboarding` concorrente cria DUAS subcontas se 2 operadores clicarem simultaneamente
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/EmpresaPspService.java`
- **Linha(s):** 48-97
- **Problema:** O metodo verifica `existing != null` e depois chama `gateway.criarSubconta`. Sem advisory lock, duas threads paralelas passam pela checagem e criam 2 subcontas no Asaas; o UPDATE final sobrescreve a primeira.
- **Impacto:** Empresa tem 1 subconta no banco mas 2 no Asaas, com uma orfa recebendo cobrancas sem controle.
- **Fix sugerido:** `pg_advisory_xact_lock(hash('empresa_psp', empresaId))` ou UNIQUE(psp_subconta_id) + ON CONFLICT, ou `SELECT ... FOR UPDATE` da linha empresas.
- **Observacoes:**
> _[espaco]_

---

#### Issue #661 — `encomendas.js /:id/itens` sem paginacao + enumeravel por id sequencial
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 65-75
- **Problema:** Alem do cross-tenant do #106, o endpoint e GET enumerable por id sequencial. Sem rate-limit especifico. Atacante com conta operadora faz scrape de todos os itens em minutos.
- **Fix sugerido:** Alem do fix #106 (JOIN empresa_id), adicionar bucket "encomenda-itens:" no rate limiter (max 30/min/user).
- **Observacoes:**
> _[espaco]_

---

#### Issue #117 — Desktop UsuarioDAO nao filtra por empresa_id (cross-tenant em banco compartilhado)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Problema:** Todas as queries SELECT/UPDATE de usuarios nao incluem `empresa_id = ?`. Risco depende de `db.properties` apontar errado para banco multi-tenant. Desktop e OFFLINE-FIRST com banco local single-tenant (CLAUDE.md regra #8).
- **Impacto:** Se db.properties apontar para banco central por erro, cross-tenant data access.
- **Fix sugerido:**
```java
String sql = "SELECT ... FROM usuarios WHERE (login_usuario = ? OR LOWER(email) = LOWER(?)) "
           + "AND excluido IS NOT TRUE AND " + DAOUtils.TENANT_FILTER;
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #122 — PerfilController upload de foto permite extensoes como .gif
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PerfilController.java`
- **Linha(s):** 74
- **Problema:** Whitelist `.gif` inclusa mas `TIPOS_PERMITIDOS` so aceita jpeg/png/webp. Inconsistencia. Nao valida content (magic bytes).
- **Impacto:** Armazenamento de arquivos maliciosos.
- **Fix sugerido:**
```java
Set<String> allowedExts = Set.of(".jpg", ".jpeg", ".png", ".webp");
// Validar magic bytes com Tika ou ImageIO antes de salvar
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #123 — Stack trace e mensagens de erro SQL potencialmente expostas via res.status(500)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** multiplos `naviera-web/server/routes/*.js` e API
- **Problema:** errorHandler central para statusCode != 500 retorna `err.message` — se uma ApiException wrapped carregar SQL error, o cliente ve detalhes do schema.
- **Impacto:** Information disclosure sobre estrutura do DB.
- **Fix sugerido:**
```javascript
const isSafe = err.isApiError && typeof err.safeMessage === 'string'
const message = isSafe ? err.safeMessage : 'Erro interno'
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #124 — Tenant cache em memoria nao e invalidado entre workers (PM2 cluster)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Problema:** Cache map em memoria por processo. Em PM2 cluster mode, se admin desativa empresa X, workers que ja cachearam continuam aceitando login por ate 60s.
- **Impacto:** Janela 60s apos desativacao.
- **Fix sugerido:** Usar Redis com TTL compartilhado ou channel de pub/sub.
- **Observacoes:**
> _[espaco]_

---

#### Issue #125 — PublicController.servirFoto usa filename sem validacao de content-type vs extensao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PublicController.java`
- **Linha(s):** 56-68
- **Problema:** Content-type deduzido da extensao sem validar conteudo. Arquivo .jpg contendo HTML serve como image/jpeg mas browser pode renderizar com sniff.
- **Impacto:** XSS via upload + download direto.
- **Fix sugerido:** Adicionar header `X-Content-Type-Options: nosniff` no ResponseEntity.
- **Observacoes:**
> _[espaco]_

---

#### Issue #126 — Timing attack em `AuthService.login`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthService.java`
- **Linha(s):** 25-28
- **Problema:** Mensagens distintas `"Documento nao encontrado"` e `"Senha incorreta"` permitem enumerar CPFs/CNPJs cadastrados.
- **Codigo problematico:**
```java
var cliente = repo.findByDocumentoAndAtivoTrue(req.documento())
    .orElseThrow(() -> ApiException.unauthorized("Documento nao encontrado"));
if (!encoder.matches(req.senha(), cliente.getSenhaHash()))
    throw ApiException.unauthorized("Senha incorreta");
```
- **Fix sugerido:**
```java
var clienteOpt = repo.findByDocumentoAndAtivoTrue(req.documento());
String hashAlvo = clienteOpt.map(ClienteApp::getSenhaHash)
    .orElse("$2a$10$dummyForTimingEqualization.........................");
if (!encoder.matches(req.senha(), hashAlvo) || clienteOpt.isEmpty()) {
    throw ApiException.unauthorized("Credenciais invalidas");
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #127 — Valor de compra calculado no server OK mas `comprar` cria passagem antes de PSP
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`; `EncomendaService.java`; `FreteService.java`
- **Problema:** (1) INSERT passagem com status `PENDENTE_CONFIRMACAO`, (2) chama PSP, (3) UPDATE. Se PSP falhar em (2), a passagem fica `PENDENTE_CONFIRMACAO` sem transacao PSP. Cliente pensa que pagou mas nao tem nada.
- **Fix sugerido:** Saga pattern — registrar intento primeiro em tabela `payment_intents`, chamar PSP, atualizar tabela origem so depois do callback/confirm.
- **Observacoes:**
> _[espaco]_

---

#### Issue #129 — Documentos pessoais (CPF/RG/CNH) via OCR armazenados em JSON sem criptografia at-rest
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/documentos.js` (122-130); `naviera-web/server/routes/ocr.js`
- **Problema:** CPF, RG, nome completo gravados em colunas plain text. Para producao brasileira (LGPD/ANPD), dados pessoais devem ter criptografia at-rest ou pseudonimizacao.
- **Fix sugerido:** Criptografar colunas sensiveis (pgcrypto / coluna `cpf_encrypted`); campo CPF nunca em resposta JSON para roles que nao precisam.
- **Observacoes:**
> _[espaco]_

---

#### Issue #130 — `buscarPorRemetenteCrossTenant` retorna fretes cross-tenant sem paginacao — enumeracao do grafo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 42-80
- **Problema:** Um cliente app com nome comum recebe lista GIGANTE de fretes de todas as empresas. Sem LIMIT.
- **Fix sugerido:** Remover match por nome — so retornar fretes com `id_cliente_app_pagador = ?` (FK forte). Adicionar LIMIT 200 sempre.
- **Observacoes:**
> _[espaco]_

---

#### Issue #131 — Endpoint GET /viagens/publicas sem autenticacao
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/ViagemController.java`
- **Linha(s):** 29-30 (e SecurityConfig 28)
- **Problema:** `SecurityConfig` libera `/viagens/publicas` para `permitAll()`. Pode expor dados competitivos. Sem rate-limit especifico.
- **Fix sugerido:** Reduzir payload (so origem, destino, data — sem empresa_id, sem embarcacao nome). Rate-limit mais restrito.
- **Observacoes:**
> _[espaco]_

---

#### Issue #654 — `PerfilController.upload_foto` sem bucket de rate-limit especifico (DoS disco)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PerfilController.java`
- **Linha(s):** 76
- **Problema:** Nenhuma verificacao de quota por empresa — atacante faz upload em loop (rate limit generico 200 req/min). Apos milhares de uploads, disco enche.
- **Impacto:** DoS por exaustao de disco.
- **Fix sugerido:** (a) Adicionar bucket especifico "upload:" em RateLimitFilter, (b) quota total por cliente, (c) monitor de tamanho do diretorio.
- **Observacoes:**
> _[espaco]_

---

#### Issue #128 — CSRF desabilitado mas cookies nao usados — SameSite/HttpOnly nao aplicados ao token
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/SecurityConfig.java`
- **Linha(s):** 24
- **Problema:** `csrf(c -> c.disable())` — aceitavel se token JWT for em header Authorization. Cliente armazena em localStorage. App e PWA sob HTTPS com CSP adequada.
- **Fix sugerido:** Avaliar migrar para HttpOnly cookie com SameSite=Lax ou Strict + CSRF double-submit token.
- **Observacoes:**
> _[espaco]_

---

#### Issue #132 — `console.error` em BFF mistura com structured logger; erro stack em stdout
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** multiplos routes/*.js
- **Problema:** Codigo ora usa `console.error` ora usa `log.error`.
- **Fix sugerido:** Padronizar sempre via `logger.js`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #133 — `db.properties` nao criptografa senha; plaintext on disk of every desktop
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `db.properties.example`
- **Problema:** Desktop tem `db.password` em claro.
- **Fix sugerido:** Usar JKS/OS-level credential store; minimo, encriptar file at-rest.
- **Observacoes:**
> _[espaco]_

---

#### Issue #134 — Versao de Spring Boot 3.3.5 e jjwt 0.12.6 — verificar CVEs
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/pom.xml`
- **Problema:** Tendencia de CVEs periodicos em Spring Security/Boot.
- **Fix sugerido:** Rodar `mvn dependency:analyze` + `npm audit` em CI; atualizar para 3.3.latest antes do prod.
- **Observacoes:**
> _[espaco]_

---

#### Issue #135 — GpsController aceita id_embarcacao do body sem validar ownership
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/GpsController.java`
- **Linha(s):** 21-33
- **Problema:** `POST /gps/posicao` aceita `id_embarcacao` do body. Extrai empresaId do JWT mas service deve validar que embarcacao pertence a essa empresa.
- **Fix sugerido:** Verificar em GpsService `embarcacoes.empresa_id = ?` antes de inserir em `gps_posicoes`.
- **Observacoes:**
> _[espaco]_

---

### 2.3 — Logica de Negocio

#### Issue #200 — Desktop e API divergem no conceito de "viagem ativa"
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ViagemDAO.java` vs `naviera-api/.../service/OpViagemWriteService.java` vs `naviera-web/server/routes/viagens.js`
- **Linha(s):** ViagemDAO 423-458 | OpViagemWriteService 61-74 | viagens.js 114-142
- **Problema:** Desktop so toca `is_atual` (deixa `ativa` intocado); API/BFF tocam `is_atual` E `ativa` juntos. Apos sync ou uso misto, uma viagem pode ter `ativa=TRUE` e `is_atual=FALSE`, e vice-versa.
- **Impacto:** Passagens compradas via app podem ser emitidas para viagem "aparentemente ativa" que ja nao e considerada atual pelo operador no desktop. Risco de double-booking.
- **Codigo problematico:**
```java
// Desktop
String sqlDesativar = "UPDATE viagens SET is_atual = false WHERE empresa_id = ?";
String sqlAtivar    = "UPDATE viagens SET is_atual = true WHERE id_viagem = ? AND empresa_id = ?";
```
```java
// API
if (ativa) {
    jdbc.update("UPDATE viagens SET ativa = FALSE, is_atual = FALSE WHERE empresa_id = ?", empresaId);
}
jdbc.update("UPDATE viagens SET ativa = ?, is_atual = ? WHERE id_viagem = ? AND empresa_id = ?",
    ativa, ativa, id, empresaId);
```
- **Fix sugerido:** Padronizar as 3 camadas para atualizar ambas as colunas na mesma transacao. Decidir em migration qual coluna e canonica (`is_atual`) e derivar `ativa` via generated column, ou remover `ativa` completamente.
- **Observacoes:**
> _[espaco]_

---

#### Issue #201 — Webhook Asaas inexistente: pagamentos PSP ficam eternamente PENDENTE_CONFIRMACAO
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/` (diretorio inteiro — falta controller)
- **Problema:** `PspCobrancaService.atualizarStatus()` e `AsaasGateway.validarAssinaturaWebhook()` existem mas NENHUM `@RestController` os invoca. Nao ha endpoint `/webhook/asaas` cadastrado. Resultado: Asaas envia callback mas nao ha quem receba. Esses registros ficam em `PENDENTE_CONFIRMACAO` para sempre.
- **Impacto:** CRITICO financeiro. Cliente paga, vendedor nunca sabe.
- **Codigo problematico:**
```java
@Transactional
public void atualizarStatus(String provider, String pspCobrancaId, String novoStatus) {
    repo.findByPspProviderAndPspCobrancaId(provider, pspCobrancaId).ifPresent(c -> {
        c.setPspStatus(novoStatus);
    });
}
```
- **Fix sugerido:**
```sql
-- Tabela para idempotencia
CREATE TABLE webhook_events (
  id BIGSERIAL PRIMARY KEY,
  provider VARCHAR(20),
  evento_id VARCHAR(100) UNIQUE NOT NULL,
  payload JSONB,
  processado_em TIMESTAMP DEFAULT NOW()
);
```
```java
@RestController
@RequestMapping("/psp/webhook")
public class PspWebhookController {
    @PostMapping("/asaas")
    public ResponseEntity<?> asaas(@RequestBody String body,
                                   @RequestHeader("asaas-access-token") String sig) {
        if (!gateway.validarAssinaturaWebhook(body, sig))
            return ResponseEntity.status(401).build();
        JsonNode evento = mapper.readTree(body);
        String eventId = evento.path("id").asText();
        // Idempotencia: ON CONFLICT DO NOTHING
        int inserted = webhookEventRepo.insertIfAbsent(eventId, body);
        if (inserted == 0) return ResponseEntity.ok().build();  // ja processado
        String pspId = evento.path("payment").path("id").asText();
        String status = evento.path("payment").path("status").asText();
        cobrancaService.atualizarStatus("asaas", pspId, mapStatus(status));
        // Propagar para passagens/fretes/encomendas via externalReference
        propagarStatus(pspId, status);
        return ResponseEntity.ok().build();
    }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #202 — `ocr.js` usa `crypto.randomUUID()` sem importar `crypto` → ReferenceError em runtime
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 127, 132, 243
- **Problema:** O arquivo nao tem `import crypto from 'crypto'`. Em Node 18+ existe `globalThis.crypto`, mas nao e garantido. Em Node 16 ou em scripts sem `--experimental-global-webcrypto`, quebra.
- **Impacto:** Upload OCR falha com `ReferenceError: crypto is not defined`.
- **Codigo problematico:**
```javascript
const uuid = client_uuid || crypto.randomUUID()   // crypto nao importado
```
- **Fix sugerido:**
```javascript
import { randomUUID } from 'crypto'
const uuid = client_uuid || randomUUID()
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #203 — `OpPassagemService` faz query em colunas que nao existem na tabela `passageiros`
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpPassagemService.java`
- **Linha(s):** 19, 23, 26, 30
- **Problema:** A query referencia `pas.nome AS nome_passageiro`, `pas.numero_doc` e `p.num_bilhete`. Nomes reais sao `pas.nome_passageiro`, `pas.numero_documento` e `p.numero_bilhete`. Gera SQLException.
- **Impacto:** Qualquer tela do app mobile que bata em `/op/passagens` quebra imediatamente.
- **Codigo problematico:**
```java
return jdbc.queryForList("""
    SELECT p.*, pas.nome AS nome_passageiro, pas.numero_doc
    FROM passagens p
    LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
    WHERE p.empresa_id = ? AND p.id_viagem = ?
    ORDER BY p.num_bilhete DESC""", empresaId, viagemId);
```
- **Fix sugerido:**
```java
return jdbc.queryForList("""
    SELECT p.*, pas.nome_passageiro, pas.numero_documento
    FROM passagens p
    LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
    WHERE p.empresa_id = ? AND p.id_viagem = ?
    ORDER BY p.numero_bilhete::bigint DESC""", empresaId, viagemId);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #204 — `POST /financeiro/estornar` zera `valor_pago` completamente e nao registra em `log_estornos_*`
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 531-580
- **Problema:** Endpoint recebe `tipo`, `id`, `motivo`, `autorizador` mas NAO recebe `valor`. Faz `valor_pago = 0` cego — estorna TUDO mesmo que a intencao fosse estornar parcial. Grava em `auditoria_financeiro` mas NAO em `log_estornos_passagens/encomendas/fretes`, produzindo divergencia de relatorios.
- **Impacto:** Dois caminhos de estorno incompativeis coexistem. Este destroi o pagamento integral.
- **Codigo problematico:**
```javascript
await client.query('UPDATE passagens SET valor_pago = 0, valor_devedor = valor_total, status_passagem = $1, valor_pagamento_dinheiro = 0, valor_pagamento_pix = 0, valor_pagamento_cartao = 0 WHERE id_passagem = $2 AND empresa_id = $3', ['PENDENTE', id, empresaId])
```
- **Fix sugerido:** Antes de deletar, grep se frontend ainda chama `/api/financeiro/estornar`. Se houver uso, migrar chamada para `/api/estornos/{tipo}/:id` (ja existente e com valor parcial + log em `log_estornos_*`). Remover completamente este endpoint.
- **Observacoes:**
> _[espaco]_

---

#### Issue #205 — PSP cria cobranca ANTES de confirmar, sem rollback caso gateway falhe apos UPDATE
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/.../service/PassagemService.java`, `FreteService.java`, `EncomendaService.java`
- **Linha(s):** PassagemService 115-180 | FreteService 91-193 | EncomendaService 115-211
- **Problema:** O metodo `pagar()`/`comprar()` e `@Transactional`, mas faz chamada externa a `pspService.criar()` DENTRO da transacao. Se o Asaas aceitar a cobranca mas o subsequente `jdbc.update` falhar, o Spring reverte as alteracoes locais mas a cobranca permanece no Asaas. Cliente pode ser cobrado sem ter passagem/frete/encomenda valida.
- **Impacto:** Risco de dupla cobranca, cobranca orfa, cliente pagando por algo que o sistema nao reconhece.
- **Codigo problematico:**
```java
// PassagemService.comprar() — @Transactional
Long idPassagem = jdbc.queryForObject("INSERT INTO passagens ... RETURNING id_passagem", ...);
PspCobranca cob = pspService.criar(pspReq);    // HTTP call INSIDE @Transactional
jdbc.update("UPDATE passagens SET id_transacao_psp = ?, qr_pix_payload = ? WHERE id_passagem = ?",
    cob.getPspCobrancaId(), cob.getQrCodePayload(), idPassagem);
```
- **Fix sugerido:** Usar padrao **outbox**. Commit local muda status para `PENDENTE_COBRANCA` com job agendado. Worker assincrono faz chamada Asaas com retry e atualiza ou **reverte** status. Minimo: extrair `pspService.criar()` para FORA do `@Transactional` e reverter status em falha (nao deixar `PROCESSANDO` orfao).
```java
// 1. transactional: reserva (UPDATE status='PENDENTE_COBRANCA') — commit
// 2. NAO-transactional: chama PSP — se falhar, abrir nova tx e reverter status para PENDENTE
// 3. transactional: grava resultado (UPDATE id_transacao_psp, qr_pix_payload)
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #206 — Definicao de "saldo" divergente entre PassagemService.comprar() e FreteService.pagar()/EncomendaService.pagar()
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/.../service/PassagemService.java` vs `FreteService.java`/`EncomendaService.java`
- **Linha(s):** PassagemService 89-111 | FreteService 124-135 | EncomendaService 146-157
- **Problema:** Em passagem, saldo = `transporte + alimentacao - desconto`. Em encomenda/frete, saldo = `total - desconto - valor_pago` e aplica-se 10% PIX. Frontend para frete usa `valorNominal - valorPago` (ignora desconto).
- **Impacto:** Valores cobrados diferentes dos exibidos.
- **Codigo problematico:**
```javascript
const saldo = Math.max(0, (Number(pagando.valorNominal) || 0) - (Number(pagando.valorPago) || 0));
```
- **Fix sugerido:** Unificar formula no backend e enviar `saldoOriginal` ao frontend.
- **Observacoes:**
> _[espaco]_

---

#### Issue #207 — `PassagemService.comprar()` usa `valorTotal` em vez de `saldo` como base do desconto PIX
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 89, 109-112
- **Problema:** Inconsistencia: `FreteService.pagar` e `EncomendaService.pagar` calculam saldo. `PassagemService.comprar` aplica 10% sobre `valorTotal` diretamente.
- **Fix sugerido:** Usar mesma formula saldo-based mesmo em comprar().
- **Observacoes:**
> _[espaco]_

---

#### Issue #208 — Data de viagem passada aceita no INSERT sem validacao server-side
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/viagens.js`
- **Linha(s):** 74-92
- **Problema:** `POST /api/viagens` aceita `data_viagem` como string sem validar se e >= CURRENT_DATE.
- **Impacto:** Dashboards podem exibir viagens em datas retroativas.
- **Fix sugerido:**
```javascript
const dv = new Date(data_viagem)
const dc = new Date(data_chegada)
if (isNaN(dv.getTime()) || isNaN(dc.getTime())) return res.status(400).json({ error: 'Data invalida' })
if (dc < dv) return res.status(400).json({ error: 'data_chegada deve ser >= data_viagem' })
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #209 — `PassagensCPF.jsx` filtra viagens por `v.dataViagem >= hoje` comparando string, formato nao normalizado
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 29-30
- **Problema:** API pode retornar `v.dataViagem` como `18/04/2026` ou `2026-04-18T00:00:00`. A comparacao lexicografica falha.
- **Impacto:** App mobile CPF mostra "nenhuma viagem futura" mesmo tendo viagens.
- **Fix sugerido:** Parsear `v.dataViagem` para Date object ou garantir que a API sempre retorne ISO-8601.
- **Observacoes:**
> _[espaco]_

---

#### Issue #210 — `definirViagemAtiva` nao invalida cache em outras JVMs/threads do desktop
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 157-161, 423-458
- **Problema:** `cacheViagemAtiva` e estatico em nivel de JVM. Funciona numa unica instancia. Se operador usar BFF/API para trocar viagem atual, o desktop continua servindo a anterior ate reinicio.
- **Fix sugerido:** Adicionar TTL ao cache (30s) ou fazer invalidacao via sync/WebSocket event.
- **Observacoes:**
> _[espaco]_

---

#### Issue #212 — `comprar()` mobile nao valida se `req.idTipoPassagem` existe em `aux_tipos_passagem`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 79-83
- **Problema:** Se ID nao bate, retorna erro generico; conversao `Integer.valueOf(req.idTipoPassagem())` pode lancar NumberFormatException sem tratamento amigavel.
- **Fix sugerido:**
```java
if (req.idTipoPassagem() == null)
    throw ApiException.badRequest("idTipoPassagem obrigatorio");
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #213 — `encomendas.status_pagamento = 'PAGO'` sem subtrair desconto no WHERE
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/encomendas.js`
- **Linha(s):** 211-232
- **Problema:** Se cliente paga R$ 99,99 em uma divida de R$ 100, status fica PARCIAL para sempre, nunca PAGO.
- **Impacto:** Encomenda quase-paga fica `PARCIAL` indefinidamente.
- **Fix sugerido:**
```sql
status_pagamento = CASE
  WHEN (valor_pago + $1) >= (total_a_pagar - COALESCE(desconto, 0)) - 0.01
  THEN 'PAGO' ELSE 'PARCIAL' END
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #214 — Dashboard agrupa pagamento por substring de string → falso positivo
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 141-146
- **Problema:** `pgto.includes('CART')` ou `pgto.includes('PIX')`. `'CARTEIRA DIGITAL'` cai em `CART`. `'BOLETO'`, `'TRANSFERENCIA'`, `'CHEQUE'` caem em `DINHEIRO` por default.
- **Impacto:** Dashboard financeiro mostra totais errados.
- **Codigo problematico:**
```javascript
if (pgto.includes('PIX')) somaPix += p
else if (pgto.includes('CART') || pgto.includes('CREDITO') || pgto.includes('DEBITO')) somaCartao += p
else somaDinheiro += p
```
- **Fix sugerido:**
```javascript
const p = (pgto || '').trim().toUpperCase()
if (p === 'PIX') somaPix += v
else if (p === 'CARTAO' || p === 'CARTAO CREDITO' || p === 'CARTAO DEBITO') somaCartao += v
else if (p === 'DINHEIRO') somaDinheiro += v
else somaOutros += v
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #216 — `POST /financeiro/saida` nao valida que `valor_pago <= valor_total`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 274-300
- **Problema:** Aceita `valor_pago` arbitrario.
- **Fix sugerido:**
```javascript
if (parseFloat(valor_pago) > parseFloat(valor_total)) return res.status(400).json({ error: 'valor_pago nao pode exceder valor_total' })
if (numero_parcela && total_parcelas && numero_parcela > total_parcelas) return res.status(400).json({ error: 'numero_parcela > total_parcelas' })
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #217 — `data_vencimento` e `data_pagamento` aceitam qualquer string (sem parser)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 290-294, 333-335
- **Problema:** As datas sao inseridas direto sem casting nem validacao. Postgres aceita '`2025-02-31`' e converte.
- **Fix sugerido:**
```javascript
function parseDate(s) {
  if (!s) return null
  const d = new Date(s)
  if (isNaN(d.getTime())) throw new Error('Data invalida: ' + s)
  return d.toISOString().split('T')[0]
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #218 — `POST /financeiro/boleto/batch` pode criar parcelas com valor total diferente de `valor_total`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 430-432
- **Problema:** O codigo usa o input `valor_total` sem validar que e `> 0`.
- **Impacto:** Negativo passa silenciosamente.
- **Fix sugerido:** Validar `valor_total > 0` antes do calculo.
- **Observacoes:**
> _[espaco]_

---

#### Issue #222 — Sync upload tolera `ultimaAtualizacao` ausente e assume cliente mais recente
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/SyncService.java`
- **Linha(s):** 200-222
- **Problema:** `isClienteNewer` retorna `true` quando timestamp cliente e vazio. Last-write-wins degenera para "cliente sempre ganha".
- **Impacto:** Perda de dados recentes quando desktop fica dessync e volta por dias.
- **Fix sugerido:** Se `clienteTimestamp == null`, forcar status "needs-conflict-resolution" ou retornar `false` (servidor ganha).
- **Observacoes:**
> _[espaco]_

---

#### Issue #226 — `bilhetes_digitais.totp_secret` gerado com 32 bytes random mas armazenado em plain na tabela
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`
- **Linha(s):** 117-119, 264-268
- **Problema:** Se alguem dump do banco, todos os bilhetes viram falsificaveis. TOTP aceita janela de +-1 perido (90s total).
- **Impacto:** Falsificacao de bilhete se DB vazar.
- **Fix sugerido:** Cifrar `totp_secret` no banco com KMS/chave em env var.
- **Observacoes:**
> _[espaco]_

---

#### Issue #233 — Estornos bloqueiam `valor > valor_pago` mas nao validam estorno de passagem EMBARCADO
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/estornos.js`
- **Linha(s):** 43-58
- **Problema:** Permite estornar pagamento de passagem `EMBARCADO`. Nao ha verificacao de status.
- **Impacto:** Fraude interna possivel.
- **Fix sugerido:**
```javascript
if (passagem.status_passagem === 'EMBARCADO' || passagem.status_passagem === 'CANCELADA')
    return res.status(400).json({ error: 'Nao e possivel estornar passagem EMBARCADO/CANCELADA. Use cancelamento.' })
// Exigir admin + motivo >= 20 chars para estorno pos-embarque
const funcao = (req.user.funcao || '').toLowerCase();
if (!['administrador','admin'].includes(funcao)) return res.status(403)...
if (!motivo || motivo.length < 20) return res.status(400).json({ error: 'Motivo obrigatorio, minimo 20 caracteres' });
// Log separado em log_estornos_pos_embarque
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #237 — `EmpresaPspService.onboarding` usa `@Transactional` mas faz chamada Asaas dentro dela
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/EmpresaPspService.java`
- **Linha(s):** 48-97
- **Problema:** Mesmo pattern de #205 — HTTP externo dentro de transacao. Se Asaas criar subconta mas DB falhar no UPDATE, subconta fica orfa no Asaas.
- **Fix sugerido:** Idem ao #205 — mover chamada PSP para fora da transacao, usar fluxo de compensacao.
- **Observacoes:**
> _[espaco]_

---

#### Issue #238 — `FinanceiroService.balanco` nao exclui registros com `excluido=TRUE`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FinanceiroService.java`
- **Linha(s):** 38-65
- **Problema:** As queries de passagens/encomendas/fretes somam `valor_pago` sem filtrar `excluido = FALSE`.
- **Impacto:** Balanco superestima receitas.
- **Fix sugerido:** Adicionar `AND (excluido IS NULL OR excluido = FALSE)` em todas as 3 queries.
- **Observacoes:**
> _[espaco]_

---

#### Issue #652 — `ViagemDAO.excluir` cascade pode deletar passageiros compartilhados em outras viagens
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Problema:** Passageiros sao entidades reutilizaveis (mesmo documento pode ter multiplas passagens em multiplas viagens). Se a exclusao de viagem deletar passageiros referenciados por essa viagem, perde-se historico. Se deleta passageiro que ainda tem passagem em outra viagem ativa, FK quebra.
- **Impacto:** Perda de dados ou FK orfa no desktop.
- **Fix sugerido:** Revisar ViagemDAO.excluir completo e confirmar que passageiros so sao deletados se NAO tiverem passagens em outras viagens (`NOT EXISTS`).
- **Observacoes:**
> _[espaco]_

---

#### Issue #653 — `financeiro.js` dashboard: `'CARTEIRA_DIGITAL'.includes('CART') === true` → carteira digital contabilizada como cartao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 144
- **Problema:** Variante do #214 com impacto concreto: `'CARTEIRA_DIGITAL'.includes('CART')` retorna true. `'TRANSPORTADOR'.includes('CART')` tambem.
- **Impacto:** Balanco financeiro errado, mascara origem real de receita.
- **Fix sugerido:**
```javascript
const FORMAS = { PIX: 'pix', CARTAO_CREDITO: 'cartao', CARTAO_DEBITO: 'cartao', BOLETO: 'boleto', DINHEIRO: 'dinheiro' }
const bucket = FORMAS[pgto?.toUpperCase().trim()] || 'outros'
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #711 — Drift financeiro: desktop soma `cargas`, API `PassagemService` NAO soma
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivos:**
  - `src/gui/VenderPassagemController.java:1291` — `alimentacao.add(transporte).add(cargas).subtract(descontoTarifa)`
  - `naviera-api/.../service/PassagemService.java:89` — `transporte.add(alimentacao).subtract(desconto)` (**SEM cargas**)
  - `naviera-api/.../service/BilheteService.java:65-69` — inclui cargas (correto)
- **Problema:** Calculo de total da passagem DIFERE entre desktop e `PassagemService.comprar` do app. Cliente paga menos pelo app que pelo operador no desktop. Bug de cobranca real entre canais.
- **Fix sugerido:**
```java
// PassagemService.java:89
var cargas = (BigDecimal) tarifa.get("valor_cargas");
if (cargas == null) cargas = BigDecimal.ZERO;
var total = transporte.add(alimentacao).add(cargas).subtract(desconto);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #215 — Estorno de frete ignora `desconto` no calculo de devedor
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/estornos.js`
- **Linha(s):** 184-205
- **Problema:** `novoValorDevedor = parseFloat(frete.valor_frete_calculado || 0) - novoValorPago` nao considera desconto.
- **Impacto:** Frete pode ficar como `PAGO` com saldo devedor ou vice-versa.
- **Fix sugerido:** Calcular devedor = `valor_total_itens - desconto - valor_pago`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #219 — `comprar()` mobile permite comprar passagem pra viagem `is_atual = TRUE` mesmo com `data_viagem < hoje`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 71-74
- **Problema:** `BilheteService.comprar()` filtra apenas `v.is_atual = TRUE` — aceita viagens com data passada.
- **Fix sugerido:** Mesma condicao em ambos: `v.is_atual = TRUE AND v.data_viagem >= CURRENT_DATE`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #220 — `PassagensCPF.jsx` usa `toISOString()` que converte pra UTC
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 29
- **Problema:** Para usuarios em Brasilia (UTC-3) as 22h retorna o dia seguinte.
- **Fix sugerido:**
```javascript
const hoje = new Date().toLocaleDateString('sv-SE') // '2026-04-18' local
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #221 — Sync `executarInsert` aceita `empresa_id` e `sincronizado` do cliente
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/SyncService.java`
- **Linha(s):** 262-322
- **Problema:** ON CONFLICT assume `uuid` unico global, nao por `empresa_id`. Se dois tenants tiverem mesma UUID, o segundo upsert sobrescreve.
- **Impacto:** Sync cross-tenant com UUID colidindo causa sobrescrita.
- **Fix sugerido:**
```sql
-- Migration: 
-- (1) adicionar indice composto (uuid, empresa_id)
-- (2) remover unique em uuid sozinho
-- (3) alterar ON CONFLICT em SyncService.executarInsert de ON CONFLICT (uuid) para ON CONFLICT (uuid, empresa_id)
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #223 — `aplicarUpsert` (SyncClient desktop) nao converte timestamps recebidos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 686-702, 736-741
- **Problema:** `stmt.setObject(i+1, valor)` sem casting — formato nao-padrao pode causar erro.
- **Fix sugerido:** Normalizar strings para `Timestamp.valueOf(...)` antes do setObject, ou usar `::timestamptz` cast no placeholder.
- **Observacoes:**
> _[espaco]_

---

#### Issue #224 — `geminiParseOCR` e `geminiParseEncomenda` nao tem fallback robusto para resposta malformada
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js` e helpers invocados
- **Linha(s):** 174-188, 226-238
- **Problema:** Quando Gemini retorna 429 ou 500, `dados` recebe fallback vazio e o operador aprova algo sem dados, criando encomenda/frete com valores zerados.
- **Fix sugerido:** Bloquear aprovacao de lancamento OCR com `total_a_pagar <= 0` ou `itens.length == 0`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #225 — `confirmarEmbarque` nao verifica pagamento antes de aceitar embarque BARCO
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 222-242
- **Problema:** Para passagens BARCO, fluxo e: status_passagem = PENDENTE. `/confirmar-embarque` nao bloqueia `PENDENTE`. Passagem pode embarcar sem pagar.
- **Fix sugerido:**
```java
if ("PENDENTE".equals(dados.get("status_passagem")) && BigDecimal.ZERO.compareTo((BigDecimal) dados.get("valor_pago")) == 0) {
    throw ApiException.badRequest("Passagem BARCO: registre o pagamento antes de embarcar.");
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #227 — Web BFF usa `parseFloat` ao ler mas `Math.round((v * 100)) / 100` ao escrever
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/passagens.js`, `encomendas.js`, `fretes.js`
- **Problema:** Mistura float IEEE 754 com arredondamento. Desktop Java usa BigDecimal, BFF usa number. Fechamento de caixa pode divergir.
- **Fix sugerido:** Usar `decimal.js` no BFF, ou converter todos os valores monetarios para inteiros (centavos).
- **Observacoes:**
> _[espaco]_

---

#### Issue #228 — `OpEncomendaWriteService.criar()` nao valida `total_volumes >= 0` e `valor_pago <= total_a_pagar - desconto`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OpEncomendaWriteService.java`
- **Linha(s):** 36-48
- **Problema:** `total_volumes` pode vir negativo; `valor_pago > total_a_pagar` gera `status_pagamento = PAGO` com valor_devedor negativo.
- **Fix sugerido:**
```java
if (valorPago.compareTo(totalAPagar.subtract(desconto)) > 0)
    throw ApiException.badRequest("valor_pago nao pode exceder total - desconto");
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #229 — Frete pagar PIX aplica 10% mas `status_frete` fica intocado
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 139-144
- **Problema:** UPDATE nao atualiza `valor_pago`, `valor_devedor` ou `status_frete`. Falta logica para reconciliar.
- **Fix sugerido:** Quando status_pagamento virar 'PAGO', UPDATE tambem em `valor_pago = valor_frete_calculado - desconto_app, valor_devedor = 0, status_frete = 'PAGO'`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #231 — App mobile `PassagensCPF.jsx` passa `compra.id` mas API espera `id_viagem`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 38
- **Problema:** Nao ha garantia de que API sempre retorna campo `id` — pode ser `id_viagem`.
- **Fix sugerido:** Padronizar DTO ou usar `compra.id ?? compra.id_viagem`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #232 — `FinanceiroCNPJ.jsx` agrupa fretes por `embarcacao` com bucket "Sem embarcacao"
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/FinanceiroCNPJ.jsx`
- **Linha(s):** 108-116
- **Problema:** Cliente CNPJ com fretes em empresas diferentes ve todos agrupados. Se houver rateio, fica enganoso.
- **Fix sugerido:** Agrupar por `empresa_nome` em vez de embarcacao.
- **Observacoes:**
> _[espaco]_

---

#### Issue #234 — `trocar-senha` nao invalida tokens JWT antigos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 120-158
- **Problema:** Tokens emitidos antes continuam validos ate expiracao.
- **Fix sugerido:** Adicionar campo `senha_atualizada_em` em `usuarios` e comparar `jwt.iat < usuario.senha_atualizada_em`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #235 — `admin/empresas/{id}/psp/onboarding` nao verifica se empresa esta ativa
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/EmpresaPspService.java`
- **Linha(s):** 48-97
- **Problema:** Pode onboardar subconta de empresa desativada. Cria obrigacao contratual com Asaas e cobranca em nome de empresa inexistente.
- **Fix sugerido:** `SELECT ativo FROM empresas WHERE id = ?` — validar antes.
- **Observacoes:**
> _[espaco]_

---

#### Issue #657 — `PassagemService.comprar` valida tarifa com `empresa_id`, mas INSERT usa `id_viagem` sem revalidar
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 71-74, 118-129
- **Problema:** Entre o SELECT inicial e o INSERT, alguem pode ter desativado a viagem (race).
- **Fix sugerido:** Usar `SELECT ... FOR UPDATE` no SELECT inicial, ou WHERE `ativa=true` no INSERT.
- **Observacoes:**
> _[espaco]_

---

#### Issue #659 — `BilheteService` TOTP aceita janela de +-1 periodo = 90s total
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`
- **Problema:** Com 6 digitos e 3 janelas temporais, brute-force = 3/10^6 por tentativa. Sem rate-limit de tentativas.
- **Fix sugerido:** TOTP window = 1 (30s total) + rate-limit "totp:" com max=5/30s por numero_bilhete.
- **Observacoes:**
> _[espaco]_

---

#### Issue #660 — `financeiro.js /estornar` sobrescreve `forma_pagamento = NULL` — perde rastreabilidade
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 552, 558
- **Problema:** UPDATE zera `forma_pagamento` e `tipo_pagamento`. Historico some.
- **Fix sugerido:** Incluido na recomendacao do #204 (deletar endpoint). Se mantido, NAO sobrescrever forma_pagamento; marcar `estornado_em` / `valor_estornado`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #662 — Desktop ViagemDAO.definirViagemAtiva nao propaga evento para API/BFF
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 423-458
- **Problema:** Mesmo quando desktop atualizar ambas as colunas, API/BFF nao sabem da troca ate a proxima sync. App mobile CPF pode ver viagem antiga.
- **Fix sugerido:** Apos `definirViagemAtiva`, disparar evento sync imediato + broadcast WebSocket.
- **Observacoes:**
> _[espaco]_

---

#### Issue #714 — `EncomendaService.pagar` tem validacao de ownership por `contains` (substring), nao exata
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../service/EncomendaService.java:134-138`
- **Problema:** `destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())`. "Ana" ∈ "Mariana". Cliente "curto" paga encomendas de terceiros com nome parcialmente igual.
- **Fix sugerido:** Usar `equalsIgnoreCase` ou comparacao normalizada por tokens. Melhor: exigir FK sempre. Padrao FreteService:113-116 tem o mesmo problema.
- **Observacoes:**
> _[espaco]_

---

#### Issue #716 — `BilheteService.comprar` e `PassagemService.comprar` parecem ambas ativas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../service/BilheteService.java:92-109`
- **Problema:** O INSERT em `passagens` injeta valores, mas calculo difere de `PassagemService.comprar`. Parece haver duas implementacoes de "comprar passagem".
- **Fix sugerido:** Rever se `BilheteService` e ainda usado ou foi substituido por `PassagemService`. Se ambas ativas, consolidar.
- **Observacoes:**
> _[espaco]_

---

#### Issue #236 — Onboarding `gerarCodigoAtivacao` usa `RANDOM.nextInt()` que retorna signed int
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/OnboardingService.java`
- **Linha(s):** 32-41
- **Problema:** O unico "bug" seria codigo `"NAV-00000000"` se nextInt retornar 0 exatamente (1 em 4Bi).
- **Fix sugerido:** Baixa prioridade.
- **Observacoes:**
> _[espaco]_

---

#### Issue #239 — `POST /fretes/contatos` usa ON CONFLICT mas nao fala o que aconteceu
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/fretes.js`
- **Linha(s):** 26-45
- **Problema:** Cliente nao sabe se foi criado novo ou reaproveitado.
- **Fix sugerido:** Retornar campo `criado: true/false` no JSON.
- **Observacoes:**
> _[espaco]_

---

#### Issue #240 — `callVisionOCR` em `ocr.js` nao tem timeout definido
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/helpers/visionApi.js` + `ocr.js:108`
- **Problema:** Upload multiplas paginas itera sequencialmente. 10 paginas * 5s/pagina = 50s.
- **Fix sugerido:** Paralelizar com `Promise.all(allFiles.map(callVisionOCR))` + timeout global de 30s.
- **Observacoes:**
> _[espaco]_

---

### 2.4 — Resiliencia e Error Handling

#### Issue #300 — AsaasGateway: RestTemplate sem timeout trava thread HTTP indefinidamente
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 42-45, 228-244
- **Problema:** `new RestTemplate()` sem builder — nao configura timeouts. Qualquer TCP hang na Asaas segura a thread HTTP do Tomcat.
- **Impacto:** Pool Tomcat esgota. API inteira para de responder sem a JVM morrer.
- **Codigo problematico:**
```java
public AsaasGateway(AsaasProperties props) {
    this.props = props;
    this.rest = new RestTemplate();
}
```
- **Fix sugerido:**
```java
public AsaasGateway(AsaasProperties props, RestTemplateBuilder builder) {
    this.props = props;
    this.rest = builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(15))
        .build();
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #301 — Webhook Asaas sem idempotencia nem WebhookController implementado
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/PspCobrancaService.java`
- **Linha(s):** 75-85
- **Problema:** `atualizarStatus` e chamado por um `WebhookController` que NAO EXISTE. Sem controle de idempotencia — Asaas retry aplicaria status repetidamente.
- **Impacto:** Cenario real — Asaas envia webhook CONFIRMADA, processa OK. Asaas envia REFUND_REQUESTED. Webhook CONFIRMADA original retry tardio sobrescreve. Cliente recebe mercadoria + estorno.
- **Fix sugerido:**
```java
@Transactional
public void atualizarStatus(String provider, String pspCobrancaId, String novoStatus, String eventId) {
    if (webhookEventRepo.existsByProviderAndEventId(provider, eventId)) {
        log.info("Webhook {} ja processado", eventId);
        return;
    }
    webhookEventRepo.save(new WebhookEvent(provider, eventId, pspCobrancaId, novoStatus));
    repo.findByPspProviderAndPspCobrancaId(provider, pspCobrancaId).ifPresent(c -> {
        if (!isValidTransition(c.getPspStatus(), novoStatus)) {
            log.warn("Transicao invalida {}->{} para cobranca {}",
                c.getPspStatus(), novoStatus, pspCobrancaId);
            return;
        }
        c.setPspStatus(novoStatus);
        repo.save(c);
    });
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #304 — ocr.js usa `crypto.randomUUID()` sem importar crypto
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 127, 132, 243
- **Problema:** `crypto.randomUUID()` chamado 3 vezes sem import. Depende de `globalThis.crypto` (Node 19+).
- **Impacto:** Upload OCR quebra em Node <19 ou setups nao-compatives.
- **Fix sugerido:**
```javascript
import { randomUUID } from 'crypto'
const loteUuid = randomUUID()
const encUuid = randomUUID()
const uuid = client_uuid || randomUUID()
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #305 — Webhook secret vazio retorna `true` — aceita webhooks forjados
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 191-196
- **Problema:** Se `asaas.webhook-secret` vazio, `validarAssinaturaWebhook` retorna `true`. Atacante envia POST `PAYMENT_CONFIRMED` e sistema marca cobranca como paga sem pagamento real.
- **Impacto:** Fraude trivial.
- **Codigo problematico:**
```java
public boolean validarAssinaturaWebhook(String payload, String assinatura) {
    String secret = props.getAsaas().getWebhookSecret();
    if (isBlank(secret)) {
        log.warn("[AsaasGateway] webhook-secret nao configurado — aceitando webhook sem validacao");
        return true;  // FALHA ABERTA
    }
}
```
- **Fix sugerido:**
```java
public boolean validarAssinaturaWebhook(String payload, String assinatura) {
    String secret = props.getAsaas().getWebhookSecret();
    String profile = System.getProperty("spring.profiles.active", "");
    if (isBlank(secret)) {
        if (profile.contains("prod")) {
            log.error("[AsaasGateway] webhook-secret NAO CONFIGURADO em profile de producao — rejeitando");
            return false;  // FAIL-CLOSED em prod
        }
        log.warn("[AsaasGateway] webhook-secret nao configurado (profile={}) — aceitando sem validacao", profile);
        return true;
    }
    // HMAC check
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #308 — PassagemService.comprar: PSP call dentro de @Transactional com 4 queries + INSERT
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 65-181
- **Problema:** Bloco inteiro `@Transactional`: SELECT viagem + SELECT tarifa + SELECT passageiro + INSERT passageiro + INSERT passagem + SELECT subconta + HTTP Asaas (ate 2 round-trips) + UPDATE passagens. Se Asaas travar 30s, conexao Hikari (pool 10) fica presa 30s.
- **Impacto:** Pior que EncomendaService pois tem INSERT novo. Se PSP falhar, rollback desfaz INSERT mas Asaas pode ter criado a cobranca remota. Proxima tentativa cria OUTRA cobranca.
- **Fix sugerido:** Separar em 2 metodos:
```java
@Transactional
public Long criarPassagemReservada(...) {
    // Todos os INSERTs ate passagem com status = RESERVADA_AGUARDANDO_PSP
    return idPassagem;
}

public Map<String, Object> comprar(Long clienteId, CompraPassagemRequest req) {
    Long idPassagem = criarPassagemReservada(...);  // commit imediato
    PspCobranca cob = pspService.criar(pspReq);
    atualizarPspInfo(idPassagem, cob);
    return resp;
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #311 — FirebaseConfig.init falha silenciosa, sem healthcheck
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/FirebaseConfig.java`
- **Linha(s):** 17-34
- **Problema:** Inicializacao falha com `System.err.println` e a API CONTINUA SUBINDO sem FCM. Nao lanca exception, nao falha o boot.
- **Impacto:** Deploy muda credenciais Firebase por acidente. Push notifications param sem qualquer alerta.
- **Codigo problematico:**
```java
} catch (Exception e) {
    System.err.println("[Firebase] Erro ao inicializar: " + e.getMessage());
}
```
- **Fix sugerido:**
```java
} catch (Exception e) {
    log.error("[Firebase] Erro critico ao inicializar", e);
    if ("prod".equals(System.getProperty("spring.profiles.active"))) {
        throw new IllegalStateException("Firebase obrigatorio em prod", e);
    }
}
// + HealthIndicator customizado
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #315 — Dockerfile API: sem STOPSIGNAL / tini — graceful shutdown nao funciona
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/Dockerfile`
- **Linha(s):** 10-14
- **Problema:** Java roda como PID 1 diretamente. Sem `tini`, SIGTERM nao e propagado corretamente. Docker envia SIGKILL — conexoes cortadas.
- **Impacto:** Em deploy, requests em pipeline quebram.
- **Codigo problematico:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- **Fix sugerido:** Consolidar com #316 — um unico patch no Dockerfile (EXPOSE + STOPSIGNAL + tini):
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tini
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
STOPSIGNAL SIGTERM
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]
```
E em application.properties: `server.shutdown=graceful` e `spring.lifecycle.timeout-per-shutdown-phase=20s`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #302 — PSP cobrancas: 2 round-trips sequenciais sem timeout e sem retry
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 90-105
- **Problema:** Apos `POST /payments`, chama `GET /payments/{id}/pixQrCode` em sequencia sem retry. Usuario retenta e cria segunda cobranca.
- **Fix sugerido:**
```java
headers.set("Idempotency-Key", req.tipoOrigem() + ":" + req.origemId() + ":" + req.formaPagamento());
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        JsonNode qr = get("/payments/" + pspCobrancaId + "/pixQrCode");
        return qr;
    } catch (Exception e) {
        if (attempt == 3) throw e;
        Thread.sleep(1000L * attempt);
    }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #303 — EncomendaService.pagar: transacao segura pool HikariCP durante I/O externo
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 115-211
- **Problema:** `@Transactional` + `pspService.criar()`. Enquanto HTTP calls rolam, conexao Hikari fica segurada. Pool esgota.
- **Fix sugerido:** Split em 2 etapas; chamar PSP fora do `@Transactional`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #307 — `fetchWithRetry` em Gemini/Vision nao retenta em 429 Too Many Requests
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/helpers/fetchWithRetry.js`
- **Linha(s):** 15-16
- **Problema:** `res.status >= 400 && res.status < 500` retorna 429 imediatamente sem retry.
- **Fix sugerido:**
```javascript
if (res.ok || (res.status >= 400 && res.status < 500 && res.status !== 429 && res.status !== 408)) {
    return res
}
const retryAfter = res.headers.get('Retry-After')
if (retryAfter && attempt < retries) {
    const secs = parseInt(retryAfter, 10) || 5
    await new Promise(r => setTimeout(r, secs * 1000))
    continue
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #309 — SyncService.processar: engole erros por registro silenciosamente
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/SyncService.java`
- **Linha(s):** 103-115
- **Problema:** Se um registro falhar, apenas loga warn e continua. Nao retorna quais UUIDs falharam. Desktop marca todos como sincronizados. Dado silenciosamente perdido.
- **Impacto:** 50 passagens criadas offline, 3 falham por constraint, todos marcados como sync ok. 3 passagens desaparecem.
- **Fix sugerido:**
```java
List<String> uuidsFalhos = new ArrayList<>();
for (SyncRegistro reg : registros) {
    try {
        processarRegistro(tabela, reg, empresaId);
        recebidos++;
    } catch (Exception e) {
        uuidsFalhos.add(reg.uuid());
    }
}
return new SyncResponse(uuidsFalhos.isEmpty(), mensagem, recebidos, paraDownload.size(), paraDownload, uuidsFalhos);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #310 — SyncClient (Desktop): enviaComRetry nao persiste failure state
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 387-436, 787-854
- **Problema:** Se `enviarComRetry` falha apos 3 tentativas, log enche de "Falha na sync" mas nenhum aviso visual chega ao operador do barco.
- **Fix sugerido:** Adicionar contador `falhas_sync_consecutivas` no DB local; se > 3, notificar via AlertHelper/tray icon.
- **Observacoes:**
> _[espaco]_

---

#### Issue #313 — GlobalExceptionHandler: exception generica retorna 500 sem correlationId
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/GlobalExceptionHandler.java`
- **Linha(s):** 30-34
- **Problema:** Sem `correlationId`/`requestId` retornado ao cliente nem embutido no log. Debugging impossivel.
- **Fix sugerido:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<?> handleGeneric(Exception e, HttpServletRequest req) {
    String correlationId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put("correlationId", correlationId);
    try {
        log.error("Erro nao tratado em {} {} [correlationId={}]",
            req.getMethod(), req.getRequestURI(), correlationId, e);
    } finally {
        MDC.remove("correlationId");
    }
    return ResponseEntity.internalServerError().body(Map.of(
        "erro", "Erro interno do servidor",
        "correlationId", correlationId
    ));
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #314 — index.js BFF: uncaughtException faz process.exit(1) sem drenar conexoes
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/index.js`
- **Linha(s):** 4-7
- **Problema:** Falha em 1 request derruba o servidor inteiro.
- **Fix sugerido:**
```javascript
process.on('uncaughtException', (err) => {
  log.error('Server', 'Uncaught Exception', { message: err.message, stack: err.stack });
  server.close(() => process.exit(1));
  setTimeout(() => process.exit(1), 5000).unref();
});
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #319 — useWebSocket: sem heartbeat configurado — conexoes mortas nao detectadas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/hooks/useWebSocket.js`
- **Linha(s):** 28-62
- **Problema:** STOMP Client criado sem `heartbeatIncoming`/`heartbeatOutgoing`. Em NAT/celular, TCP fica "zombie".
- **Fix sugerido:**
```jsx
const client = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: reconnectDelay.current,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
});
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #306 — fetchWithRetry nao distingue timeout de erro de rede; nao loga tentativa
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/helpers/fetchWithRetry.js`
- **Linha(s):** 10-32
- **Problema:** Sem jitter no backoff (thundering herd), sem log estruturado de tentativas.
- **Fix sugerido:**
```javascript
if (attempt < retries) {
  const base = baseDelay * Math.pow(2, attempt)
  const jitter = Math.random() * base * 0.3
  const delay = base + jitter
  console.warn(`[fetchWithRetry] ${url} retry ${attempt+1}/${retries+1} em ${Math.round(delay)}ms`)
  await new Promise(r => setTimeout(r, delay))
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #312 — PushService.enviarNotificacao: loop sincrono sem limite de paralelismo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PushService.java`
- **Linha(s):** 41-66
- **Problema:** For-loop sequencial sincrono. Cliente com 5 dispositivos = 5 chamadas HTTP em serie.
- **Fix sugerido:**
```java
ApiFuture<BatchResponse> future = FirebaseMessaging.getInstance().sendEachForMulticastAsync(
    MulticastMessage.builder()
        .addAllTokens(tokens)
        .setNotification(Notification.builder().setTitle(titulo).setBody(corpo).build())
        .build()
);
future.addListener(() -> { /* log result */ }, Runnable::run);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #316 — docker-compose.yml API expoe 8081 mas Dockerfile EXPOSE 8080
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/Dockerfile` / `docker-compose.yml`
- **Linha(s):** Dockerfile:13, compose:25, compose:36
- **Problema:** EXPOSE cosmetico mas nao bate com porta real.
- **Fix sugerido:** Consolidar com #315 num unico patch Dockerfile (EXPOSE 8081 + STOPSIGNAL + tini).
- **Observacoes:**
> _[espaco]_

---

#### Issue #317 — PagamentoArtefato / FinanceiroCNPJ / EncomendaCPF: `catch` engole erro sem correlationId
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/EncomendaCPF.jsx` / `FinanceiroCNPJ.jsx` / `PassagensCPF.jsx`
- **Linha(s):** EncomendaCPF:46, FinanceiroCNPJ:37, PassagensCPF:42
- **Problema:** `} catch { setErrPag("Erro de conexao."); }` — stack trace descartado.
- **Fix sugerido:**
```jsx
} catch (e) {
  console.error('[EncomendaCPF] pagar falhou', e);
  setErrPag("Erro de conexao. Tente novamente.");
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #320 — SyncClient.escapeJson: implementacao manual, nao lida com \u0000-\u001F
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 939-946, 582-607
- **Problema:** Duplo escape + caracteres de controle causam JSON invalido.
- **Fix sugerido:**
```java
Map<String, Object> body = Map.of("tabela", tabela, ...);
String jsonRequest = MAPPER.writeValueAsString(body);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #322 — TenantMiddleware cache in-memory com TTL de 60s — falha em scale horizontal
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 3-4, 36-54
- **Problema:** Em PM2 cluster mode, cache por processo causa inconsistencia.
- **Fix sugerido:** Redis como cache compartilhado ou LISTEN/NOTIFY do Postgres.
- **Observacoes:**
> _[espaco]_

---

#### Issue #323 — SyncClient.aplicarRegistroRecebido: catch silencioso em loop
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 618-638, 480-487
- **Problema:** UI diz "sync ok: 50 recebidos" mas 5 nao aplicaram.
- **Fix sugerido:** Contar `registrosComFalhaAplicacao` separadamente.
- **Observacoes:**
> _[espaco]_

---

#### Issue #324 — Asaas onboarding: exception generica engole detalhe
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 184-188
- **Problema:** `catch (Exception e) { throw new RuntimeException("Erro Asaas onboarding: " + e.getMessage(), e); }` — detalhe do Asaas nao propagado.
- **Fix sugerido:** Capturar `HttpClientErrorException`, parsar body JSON e retornar `ApiException.badRequest(asaasError.message)`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #327 — AsaasGateway.obterOuCriarCustomer: race condition pode criar customer duplicado
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/psp/AsaasGateway.java`
- **Linha(s):** 213-226
- **Problema:** Check-then-act sem atomicidade. 2 requests paralelos criam 2 customers.
- **Fix sugerido:** Usar `externalReference` unico no customer + idempotencia do Asaas via header.
- **Observacoes:**
> _[espaco]_

---

#### Issue #328 — SyncClient (Desktop): scheduler com thread daemon
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 94-98, 117-123
- **Problema:** Threads daemon abortadas imediatamente no SIGTERM.
- **Fix sugerido:** `Runtime.getRuntime().addShutdownHook(new Thread(() -> syncClient.pararSyncAutomatica()))`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #329 — geminiParser: JSON parse via regex `\{[\s\S]*\}` gulos ao erro
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/helpers/geminiParser.js`
- **Linha(s):** 122-125, 164-187
- **Problema:** Regex greedy pode pegar texto nao-JSON.
- **Fix sugerido:** Tentar `JSON.parse(text)` direto primeiro, depois lazy regex. Ou configurar response_mime_type: "application/json".
- **Observacoes:**
> _[espaco]_

---

#### Issue #330 — NotificationService.notify: catch silencia WebSocket send failures
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/NotificationService.java`
- **Linha(s):** 27-33
- **Problema:** `messaging.convertAndSend(...)` pode lancar exception; handler so loga `warn`.
- **Fix sugerido:** Manter fila local (tabela `notification_outbox`) — worker thread consome.
- **Observacoes:**
> _[espaco]_

---

#### Issue #318 — PagamentoArtefato: `navigator.clipboard?.writeText().then()` sem `.catch`
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-app/src/components/PagamentoArtefato.jsx`
- **Linha(s):** 19-25
- **Problema:** Promessa rejeitada vira UnhandledPromiseRejection.
- **Fix sugerido:**
```jsx
const copiar = async (txt, label) => {
    if (!txt) return;
    try {
      await navigator.clipboard?.writeText(txt);
      setCopiado(label);
      setTimeout(() => setCopiado(""), 2000);
    } catch (e) {
      console.warn('[Pagamento] clipboard falhou', e);
    }
};
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #321 — Auth.js trocar-senha: bcrypt.hash com cost factor 10 hardcoded
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/routes/auth.js`
- **Linha(s):** 147
- **Problema:** Cost factor hardcoded. Em massa, bloqueia event loop.
- **Fix sugerido:** Usar `bcrypt` nativo (nao bcryptjs) que roda em libuv worker pool.
- **Observacoes:**
> _[espaco]_

---

#### Issue #331 — AuthOperadorService / AuthService: NUNCA checados no audit
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AuthService.java` / `AuthOperadorService.java`
- **Problema:** Nao varridos. JWT expira em 8h. App mobile nao parece ter refresh token.
- **Fix sugerido:** Considerar refresh token OU aumentar expiration para operador em campo.
- **Observacoes:**
> _[espaco]_

---

### 2.5 — Performance

#### Issue #403 — `financeiro/dashboard`: UNION ALL full-scan da empresa + filtros em JS
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-web/server/routes/financeiro.js`
- **Linha(s):** 95-161
- **Problema:** Endpoint faz `UNION ALL` de passagens + encomendas + fretes da empresa. Sem viagem_id, retorna tudo. Filtra em JS. Sem LIMIT no UNION.
- **Impacto:** Payload de 50MB+, BFF Node cai em OOM. Em tenant com 1-2 anos de uso derruba o BFF.
- **Codigo problematico:**
```js
let sql = `
  SELECT 'ENCOMENDA' AS origem, e.total_a_pagar AS total, ...
  FROM encomendas e
  LEFT JOIN usuarios ue ON e.id_caixa = ue.id
  WHERE e.empresa_id = $1 ${viagem_id ? 'AND e.id_viagem = $2' : ''}
  UNION ALL ... UNION ALL ...
`
if (categoria && categoria !== 'Todas') rows = rows.filter(r => r.origem === categoria.toUpperCase())
if (forma_pagto && forma_pagto !== 'Todas') rows = rows.filter(r => ...)
```
- **Fix sugerido:**
```js
// 1. Exigir viagem_id ou janela temporal obrigatoria
if (!viagem_id && !data_inicio) return res.status(400).json({ error: 'viagem_id ou data_inicio obrigatorio' })
// 2. Mover filtros para SQL
// 3. Agregar no proprio SQL
const sql = `
  WITH movimentos AS (
    SELECT 'ENCOMENDA' AS origem, total_a_pagar AS total, valor_pago AS pago, forma_pagamento AS pgto
    FROM encomendas WHERE empresa_id = $1 AND id_viagem = $2
    UNION ALL ...
  )
  SELECT origem, SUM(total) AS total, SUM(pago) AS pago, pgto
  FROM movimentos GROUP BY origem, pgto`
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #411 — PSP inline em POST `/encomendas/:id/pagar`: chamada sincrona ao Asaas dentro de `@Transactional`
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `naviera-api/.../service/EncomendaService.java` (L116-211), `FreteService.java` (L91-193), `PassagemService.java` (L65-181)
- **Linha(s):** EncomendaService L176-196, FreteService L155-175, PassagemService L140-166
- **Problema:** `pspService.criar(pspReq)` (HTTP para Asaas) roda **dentro** do `@Transactional`. Com HikariCP=10, incidente Asaas derruba Spring Boot inteiro.
- **Impacto:** Sob incidente Asaas: pool vira. Todos usuarios sem conseguir fazer qualquer INSERT/UPDATE.
- **Codigo problematico:**
```java
@Transactional
public Map<String, Object> pagar(...) {
    jdbc.update("UPDATE encomendas SET ... WHERE id_encomenda = ?", ...);
    PspCobranca cob = pspService.criar(pspReq); // <-- HTTP sincrono dentro da tx
    jdbc.update("UPDATE encomendas SET id_transacao_psp = ?, qr_pix_payload = ? WHERE id_encomenda = ?", ...);
}
```
- **Fix sugerido:** Usar padrao **outbox**. Commit local muda status para `PENDENTE_COBRANCA`. Worker assincrono faz chamada Asaas e atualiza ou **reverte** status. Minimo viavel:
```java
// 1. @Transactional curto: UPDATE status = 'PENDENTE_COBRANCA', COMMIT
// 2. FORA da tx: chamar PSP
// 3. Em caso de sucesso, nova tx: UPDATE id_transacao_psp, qr_pix_payload
// 4. Em caso de falha: nova tx: UPDATE status = 'PENDENTE' (reverter!)
// Sem deixar 'PROCESSANDO' orfao.
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #400 — Cross-tenant LIKE full-scan sem indice trigram nem LIMIT
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EncomendaService.java`
- **Linha(s):** 82-104
- **Problema:** `rastreioCrossTenant` faz LIKE sem filtro empresa_id, 3 LEFT JOINs, sem LIMIT, sem indice trigram.
- **Impacto:** Em 50 tenants × 1M encomendas derruba. MVP atual nao atinge.
- **Fix sugerido:**
```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_encomendas_destinatario_trgm
  ON encomendas USING gin (UPPER(destinatario) gin_trgm_ops);
CREATE INDEX idx_encomendas_remetente_trgm
  ON encomendas USING gin (UPPER(remetente) gin_trgm_ops);
```
```java
sql += " WHERE (e.id_cliente_app_destinatario = ? " +
       "      OR UPPER(e.destinatario) LIKE UPPER(?) " +
       "      OR UPPER(e.remetente) LIKE UPPER(?))" +
       " AND v.data_viagem >= CURRENT_DATE - INTERVAL '6 months'" +
       " ORDER BY e.id_encomenda DESC LIMIT 50";
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #401 — `viagens/buscarPublicas` sem LIMIT, sem cache, cross-tenant
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/ViagemService.java`
- **Linha(s):** 44-57
- **Problema:** Retorna todas as viagens ativas de todas as empresas sem LIMIT. Payload ~200KB em 3G.
- **Fix sugerido:**
```java
@Cacheable(value = "viagens-publicas", key = "'all'")
public List<Map<String, Object>> buscarPublicas() {
    return jdbc.queryForList("""
        SELECT v.id_viagem, ... FROM viagens v ...
        WHERE v.ativa = TRUE
          AND v.data_viagem BETWEEN CURRENT_DATE - 1 AND CURRENT_DATE + INTERVAL '60 days'
        ORDER BY v.data_viagem ASC
        LIMIT 500
        """);
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #402 — Pool de conexoes subdimensionado (HikariCP + pg Pool)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/resources/application.properties` (L22-23) + `naviera-web/server/db.js` (L10)
- **Problema:** HikariCP max=10, pg Pool max=10. Mesmo Postgres atende ambos + desktop sync + webhooks. Dashboard faz Promise.all com 4 queries.
- **Impacto:** Sob 20+ RPS, timeouts aleatorios.
- **Fix sugerido:** **pgbouncer primeiro, ajustar pool depois** — pgbouncer resolve sem tocar em cada instancia. Documentar: `postgresql.conf max_connections >= hikari_max * N_api + pg_pool * N_bff + 20`.
```properties
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=3000
```
```js
const pool = new Pool({ ..., max: 30, idleTimeoutMillis: 10000, allowExitOnIdle: false })
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #404 — `FreteService.buscarPorRemetenteCrossTenant`: LIKE cross-tenant sem LIMIT
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/FreteService.java`
- **Linha(s):** 42-80
- **Problema:** Mesmo padrao do #400.
- **Fix sugerido:**
```sql
CREATE INDEX idx_fretes_remetente_trgm ON fretes USING gin (UPPER(remetente_nome_temp) gin_trgm_ops);
```
```java
sql += " WHERE (f.id_cliente_app_pagador = ? OR UPPER(f.remetente_nome_temp) LIKE UPPER(?))" +
       " AND f.data_emissao >= CURRENT_DATE - INTERVAL '12 months'" +
       " ORDER BY f.id_frete DESC LIMIT 50";
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #405 — `AmigoService.buscarPorNome/sugestoes`: subquery NOT IN + UNION sem indice
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/AmigoService.java`
- **Linha(s):** 60-90
- **Problema:** `NOT IN` com subquery correlacionada. Sem indice em `amigos_app(id_cliente, status)`.
- **Fix sugerido:**
```sql
CREATE INDEX idx_amigos_cliente ON amigos_app(id_cliente, status);
CREATE INDEX idx_amigos_amigo ON amigos_app(id_amigo, status);
CREATE INDEX idx_clientes_app_nome_trgm ON clientes_app USING gin (UPPER(nome) gin_trgm_ops);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #406 — `LojaService.stats`: 3 sub-SELECTs + N+1 em toPedidoDTO
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/LojaService.java`
- **Linha(s):** 100-109, 111-118
- **Problema:** `clienteRepo.findById` chamado por pedido — classic N+1.
- **Fix sugerido:**
```java
// Batch fetch
Set<Long> ids = pedidos.stream().map(PedidoLoja::getIdClienteComprador).collect(toSet());
Map<Long, String> nomes = clienteRepo.findAllById(ids).stream()
    .collect(toMap(ClienteApp::getId, ClienteApp::getNome));
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #407 — `EmbarcacaoService.listarComStatus`: 2 LATERAL JOINs por embarcacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/EmbarcacaoService.java`
- **Linha(s):** 14-55
- **Problema:** Para cada embarcacao, 2 scans. Indice composto nao existe.
- **Fix sugerido:**
```sql
CREATE INDEX idx_viagens_embarcacao_ativa
  ON viagens (id_embarcacao, ativa, data_viagem, data_chegada)
  WHERE ativa = TRUE;
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #409 — `PassagemService.minhasPassagens`: query cross-tenant sem limite
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 36-61
- **Problema:** CPF veterano com 500 viagens = 3000 lookups.
- **Fix sugerido:**
```java
sql += " AND v.data_viagem >= CURRENT_DATE - INTERVAL '24 months' ORDER BY v.data_viagem DESC LIMIT 100";
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #412 — QR Code PIX em base64 dentro de JSON response
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/components/PagamentoArtefato.jsx`
- **Linha(s):** 27-48
- **Problema:** `qrCodeImageUrl` recebido como string base64 completa. Payload JSON cresce 200-500KB.
- **Fix sugerido:**
```jsx
// Cliente gera QR a partir de qrCodePayload (copia-e-cola PIX)
import QRCode from 'qrcode'
useEffect(() => {
  if (qrCodePayload) QRCode.toDataURL(qrCodePayload).then(setImgSrc)
}, [qrCodePayload])
// Backend remove qrCodeImageUrl do response. React.memo no componente.
export default React.memo(PagamentoArtefato);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #413 — `PassagensCPF.jsx`: 4 `useApi` + filtros O(n) em todo render
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-app/src/screens/PassagensCPF.jsx`
- **Linha(s):** 14-32
- **Problema:** `embFiltradas`, `viagensEmb`, `tarifasDaViagem` recomputados em cada keystroke sem `useMemo`.
- **Fix sugerido:**
```jsx
const viagensEmb = useMemo(() =>
  selEmb ? (viagens || []).filter(v => v.embarcacao === selEmb.nome && v.dataViagem >= hoje && !v.atual) : [],
  [selEmb?.nome, viagens, hoje]
);
const embFiltradas = useMemo(() => {
  const q = busca.trim().toLowerCase();
  if (!q) return embarcacoes;
  return (embarcacoes || []).filter(e => e.nome.toLowerCase().includes(q));
}, [busca, embarcacoes]);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #415 — OCR `callVisionOCR`/`geminiParseOCR`: base64 de imagem inteira em memoria
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/helpers/{visionApi.js,geminiParser.js}`
- **Problema:** `readFile + toString('base64')` duplica em heap. 10 paginas ~140MB. BFF com heap 512MB-1.5GB.
- **Fix sugerido:**
```js
// p-limit(2) mais leve que sharp para comecar
import pLimit from 'p-limit'
const limit = pLimit(2)
const results = await Promise.all(allFiles.map(f => limit(() => callVisionOCR(f.path))))
// Se volume OCR for alto, adicionar sharp para resize antes
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #429 — Ausencia de indice trigram em colunas de busca cross-tenant
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `database_scripts/*.sql`
- **Problema:** Nenhum script tem trigram para `encomendas`, `fretes`, `clientes_app.nome`.
- **Fix sugerido:**
```sql
-- Novo script 031_trigram_busca_cross_tenant.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_encomendas_destinatario_trgm ON encomendas USING gin (UPPER(destinatario) gin_trgm_ops);
CREATE INDEX idx_encomendas_remetente_trgm ON encomendas USING gin (UPPER(remetente) gin_trgm_ops);
CREATE INDEX idx_fretes_remetente_trgm ON fretes USING gin (UPPER(remetente_nome_temp) gin_trgm_ops);
CREATE INDEX idx_fretes_destinatario_trgm ON fretes USING gin (UPPER(destinatario_nome_temp) gin_trgm_ops);
CREATE INDEX idx_clientes_app_nome_trgm ON clientes_app USING gin (UPPER(nome) gin_trgm_ops);
CREATE INDEX idx_passagens_empresa_status ON passagens(empresa_id, status_passagem) WHERE status_passagem IN ('PENDENTE', 'PENDENTE_CONFIRMACAO');
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #703 — `financeiro.js /dashboard` nao valida tamanho antes do UNION ALL
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `naviera-web/server/routes/financeiro.js:95-120`
- **Problema:** Relacionado ao #403. Usuario que pressione F5 rapidamente dispara N queries UNION paralelas.
- **Fix sugerido:** Ja coberto pelo #403 ao exigir viagem_id. Adicional: middleware de dedup por (user, endpoint, query string) por 2s.
- **Observacoes:**
> _[espaco]_

---

#### Issue #408 — Dashboard/resumo: 3 aggregate queries sequenciais
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/DashboardService.java`
- **Linha(s):** 16-32
- **Problema:** Controller faz 3 `jdbc.queryForMap` em sequencia (serial).
- **Fix sugerido:**
```java
return jdbc.queryForMap("""
    SELECT
      (SELECT JSONB_BUILD_OBJECT('total', COUNT(*), 'valor', COALESCE(SUM(valor_total), 0))
         FROM passagens WHERE id_viagem = ? AND empresa_id = ?) AS passagens,
      (SELECT JSONB_BUILD_OBJECT('total', COUNT(*), 'valor', COALESCE(SUM(total_a_pagar), 0))
         FROM encomendas WHERE id_viagem = ? AND empresa_id = ?) AS encomendas,
      (SELECT JSONB_BUILD_OBJECT('total', COUNT(*), 'valor', COALESCE(SUM(valor_nominal), 0))
         FROM fretes WHERE id_viagem = ? AND empresa_id = ?) AS fretes
""", viagemId, empresaId, viagemId, empresaId, viagemId, empresaId);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #410 — `BilheteService.comprar`: MAX+1 com advisory lock por viagem
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`
- **Linha(s):** 84-89
- **Problema:** Advisory lock usa empresaId — serializa todas as compras da empresa.
- **Fix sugerido:**
```sql
CREATE SEQUENCE IF NOT EXISTS seq_numero_bilhete;
```
```java
String numBilhete = jdbc.queryForObject(
    "SELECT LPAD(nextval('seq_numero_bilhete')::text, 5, '0')", String.class);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #414 — `FinanceiroCNPJ.jsx`: `reduce` sobre fretes re-executado em todo render
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/src/screens/FinanceiroCNPJ.jsx`
- **Linha(s):** 108-119
- **Problema:** `grupos`, `listaGrupos`, `totalPendente`, `totalPago` sem `useMemo`.
- **Fix sugerido:**
```jsx
const { listaGrupos, totalPendente, totalPago } = useMemo(() => {
  const grupos = (fretes || []).reduce((acc, f) => { ... }, {});
  const lista = Object.values(grupos).sort((a, b) => b.pendente - a.pendente);
  return {
    listaGrupos: lista,
    totalPendente: lista.reduce((s, g) => s + g.pendente, 0),
    totalPago: lista.reduce((s, g) => s + g.pago, 0)
  };
}, [fretes]);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #416 — Loop sequencial de INSERT em OCR aprovar (N+1 insert)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 551-567, 604-614
- **Problema:** Aprovar com N itens faz N `INSERT` sequenciais.
- **Fix sugerido:**
```js
const values = [], params = [];
dados.itens.forEach((item, i) => {
  const off = i * 5;
  values.push(`($${off+1}, $${off+2}, $${off+3}, $${off+4}, $${off+5})`);
  params.push(encId, item.quantidade || 1, ...);
});
await client.query(
  `INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total)
   VALUES ${values.join(',')}`, params);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #417 — `listarAvaliacoes` sem LIMIT + `criarAvaliacao` recalcula AVG(nota)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/LojaService.java`
- **Linha(s):** 76-98
- **Fix sugerido:**
```sql
CREATE INDEX idx_avaliacoes_loja ON avaliacoes_loja(id_loja);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #418 — `CrudFactory.list`: sem LIMIT default
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/utils/crudFactory.js`
- **Linha(s):** 20-30, 85-93
- **Problema:** Handlers CRUD listam tabela inteira sem paginacao.
- **Fix sugerido:**
```js
`SELECT ${select} FROM ${table} WHERE empresa_id = $1${extraWhere} ORDER BY ${order} LIMIT 500`
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #419 — Rate limit em memoria no filter Java
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/RateLimitFilter.java`
- **Linha(s):** 30, 40-43
- **Problema:** `ConcurrentHashMap` acumula entries por IP. Limpeza so 1/min. OOM latente.
- **Fix sugerido:**
```java
Cache<String, RateEntry> hits = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(2))
    .maximumSize(100_000)
    .build();
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #421 — `viagens.delete`: cascade em 6 steps sequenciais
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/viagens.js`
- **Linha(s):** 155-178
- **Problema:** 6 DELETEs sequenciais. Viagem com 1000 passagens + 500 encomendas + 300 fretes = 5-15s.
- **Fix sugerido:**
```sql
ALTER TABLE encomenda_itens
  ADD CONSTRAINT fk_encomenda_itens FOREIGN KEY (id_encomenda)
  REFERENCES encomendas(id_encomenda) ON DELETE CASCADE;
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #424 — `PushService.enviarNotificacao`: N requests sincronos ao FCM
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PushService.java`
- **Linha(s):** 48-65
- **Problema:** Loop sincrono por token.
- **Fix sugerido:**
```java
MulticastMessage multi = MulticastMessage.builder()
    .addAllTokens(tokens)
    .setNotification(notification)
    .build();
BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multi);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #430 — `passageiros(numero_documento)` indice nao-composto com empresa
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `database_scripts/006_criar_indices_performance.sql`
- **Linha(s):** 28
- **Problema:** Queries filtram por `(numero_documento, empresa_id)` mas indice e so em `numero_documento`.
- **Fix sugerido:**
```sql
CREATE INDEX idx_passageiros_doc_empresa ON passageiros(empresa_id, numero_documento);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #431 — `OCR upload` roda Vision OCR em serie
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-web/server/routes/ocr.js`
- **Linha(s):** 197-206
- **Problema:** Loop `for` com `await` serial. 10 paginas = 10-20s.
- **Fix sugerido:**
```js
import pLimit from 'p-limit'
const limit = pLimit(3)
const results = await Promise.all(allFiles.map(f => limit(() => callVisionOCR(f.path))))
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #700 — `PassagemService.confirmarEmbarque` executa query pesada dentro de `@Transactional`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 222-241
- **Problema:** `confirmarEmbarque` e `@Transactional` e executa `consultarParaEmbarque` (7 JOINs). Lock transacional prolongado.
- **Fix sugerido:**
```java
public Map<String, Object> confirmarEmbarque(Integer empresaId, String numeroBilhete, String operador) {
    var dados = consultarParaEmbarque(empresaId, numeroBilhete); // sem @Transactional
    return updateEmbarque(empresaId, numeroBilhete, operador, dados); // tx curta
}
@Transactional
protected Map<String, Object> updateEmbarque(...) { ... }
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #702 — `SELECT MAX(CAST(SUBSTRING(...)))` para numero_bilhete em hot path
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/BilheteService.java`
- **Linha(s):** 86-89
- **Problema:** Regex + cast em todas as linhas, sem indice funcional.
- **Fix sugerido:** Mesma solucao de #410 — sequence dedicada ou indice funcional.
- **Observacoes:**
> _[espaco]_

---

#### Issue #420 — Tenant cache sem max size nem LRU eviction
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/middleware/tenant.js`
- **Linha(s):** 4, 52
- **Problema:** `Map` JS cresce indefinidamente. Em 3 anos com 5000 empresas acumula sem limpar.
- **Fix sugerido:**
```js
import { LRUCache } from 'lru-cache'
// Cachear tambem null (slug nao encontrado) 30s pra evitar brute-force
const cache = new LRUCache({ max: 500, ttl: 60_000, ttlAutopurge: true })
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #422 — `EncomendaService.pagar`/`FreteService.pagar`: 3 queries sequenciais para metadados
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `EncomendaService.java`/`FreteService.java`
- **Problema:** 3 round-trips ao DB por pagamento quando poderia ser 1.
- **Fix sugerido:**
```java
var row = jdbc.queryForMap("""
    SELECT e.*, emp.psp_subconta_id
    FROM encomendas e JOIN empresas emp ON emp.id = e.empresa_id
    WHERE e.id_encomenda = ?""", idEncomenda);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #425 — Dashboard `useEffect` sem cleanup + fetch nao cancelado (apenas pagina web)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/src/pages/Dashboard.jsx`
- **Linha(s):** 28-33
- **Problema:** `useEffect` com `api.get()` sem AbortController. Nota: o hook `useApi` do app mobile ja usa AbortController — issue apenas para a pagina web.
- **Fix sugerido:**
```jsx
useEffect(() => {
  if (!viagemAtiva) return;
  const ac = new AbortController();
  api.get(`/dashboard/resumo?viagem_id=${viagemAtiva.id_viagem}`, { signal: ac.signal })
    .then(setResumo).catch(() => {});
  return () => ac.abort();
}, [viagemAtiva]);
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #427 — Console.log/System.err em hot paths (sync I/O)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** multiplos (ex.: `PushService.java:61`, `routes/*.js`)
- **Problema:** `System.err.println` e `console.error` sao sincronos.
- **Fix sugerido:**
```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
  <appender-ref ref="FILE"/>
  <queueSize>1024</queueSize>
</appender>
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #428 — `jdbc.queryForMap` em hot-path retorna `LinkedHashMap` — GC pressure
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** muitos — ex.: `FinanceiroService.java`, `DashboardService.java`, `OpViagemService.java`
- **Problema:** `Map<String, Object>` gera muitas alocacoes curtas.
- **Fix sugerido:** Este issue ja esta rastreado como TODO DM069 em PassagemService (5 ocorrencias). Migrar para records DTO (Java 17).
- **Observacoes:**
> _[espaco]_

---

#### Issue #701 — `OpEmbarcacaoController.listar` retorna `SELECT *` sem projecao
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/.../controller/OpEmbarcacaoController.java:19-23`
- **Problema:** `SELECT *` puxa todas as colunas (incluindo `foto_url`, `descricao`, `link_externo`).
- **Fix sugerido:** Listar colunas explicitamente. Se precisar de `foto_url` so em tela detalhe, separar endpoint.
- **Observacoes:**
> _[espaco]_

---

### 2.6 — Manutenibilidade

#### Issue #500 — Triplicacao do fluxo `pagar()` nos services PSP da API
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivos:** `EncomendaService.java`, `FreteService.java`, `PassagemService.java`
- **Linha(s):** Encomenda 115-211 (`pagar`), Frete 92-193 (`pagar`), Passagem 66-181 (`comprar`)
- **Problema:** ~300 linhas de logica praticamente identica: carregar cliente, validar ownership, calcular saldo, aplicar 10% PIX, setar status, criar CobrancaRequest, atualizar entidade. Variacoes sutis (plusDays 1 vs 3) sem clareza se intencional ou bug.
- **Fix sugerido:**
```java
@Service
class PagamentoProcessor {
    record Resultado(BigDecimal saldo, BigDecimal desconto, BigDecimal aPagar, String status) {}
    Resultado calcular(BigDecimal total, BigDecimal descontoBase, BigDecimal pago, String forma) { ... }
    Map<String,Object> gerarCobrancaOuReservar(TipoCobranca tipo, long idEntidade,
        Integer empresaId, ClienteApp cliente, Resultado r, String forma, String descricao) { ... }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #501 — Triplicacao do modal de pagamento nas 3 telas do App
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivos:** `EncomendaCPF.jsx`, `FinanceiroCNPJ.jsx`, `PassagensCPF.jsx`
- **Linha(s):** EncomendaCPF 68-111, FinanceiroCNPJ 61-105, PassagensCPF 99-130
- **Problema:** ~150 linhas JSX quase identicas. Calculo saldo/desconto10/aPagar + opcoes + `<Cd>` + card de resumo.
- **Fix sugerido:**
```jsx
// naviera-app/src/components/PagamentoModal.jsx
export default function PagamentoModal({
  t, titulo, resumo, saldo, opcoes, formaPag, setFormaPag,
  onConfirm, enviando, erro, percentualDescontoPix = 0.10,
}) { ... }
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #502 — Magic number `0.10` (desconto PIX) hardcoded em 6 lugares
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivos:** 3 services Java + 3 telas React
- **Problema:** Regra de negocio "PIX da 10% de desconto" espalhada em 3 linguagens, sem fonte unica.
- **Impacto:** Front pode calcular 10% e back aplicar 5% (mostrar um preco, cobrar outro).
- **Fix sugerido:**
```properties
naviera.psp.desconto-pix-pct=${PSP_DESCONTO_PIX_PCT:10.00}
```
```java
BigDecimal pct = pspProps.getDescontoPixPct().divide(HUNDRED, 4, HALF_UP);
desconto = "PIX".equals(forma) ? saldo.multiply(pct).setScale(2, HALF_UP) : ZERO;
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #506 — `CadastroFreteController.java` com 2.081 linhas e responsabilidades misturadas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Problema:** Controller mistura autocomplete, OCR, audio, XML parser, SQL inline, tabela de precos.
- **Fix sugerido:** Extrair AutocompleteHelperJFX, FreteOcrService, FreteAudioService, FreteXmlParser. Usar FreteService existente. Meta: Controller <600 linhas.
- **Observacoes:**
> _[espaco]_

---

#### Issue #507 — `VenderPassagemController.java` 1.824 linhas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Problema:** Mesma patologia do #506. Logica de calculo de tarifa duplicada com `PassagemService`.
- **Fix sugerido:** Extrair `VendaPassagemService`, mover impressao para `PassagemPrintHelper`. Meta: <500 linhas.
- **Observacoes:**
> _[espaco]_

---

#### Issue #527 — Services de negocio criticos sem nenhum teste unitario
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivos sem testes:** `EncomendaService`, `FreteService`, `PassagemService`, `OnboardingService`, `EmpresaPspService`, `PspCobrancaService` e 22 outros.
- **Problema:** `pagar()` e `comprar()` fazem calculo financeiro + ownership + PSP — refatorar sem test suite e radicalmente arriscado.
- **Fix sugerido:** Comecar por testes de `calcularDesconto`, `validarOwnership`, `statusAposForma`. Se extrair `PagamentoProcessor` (#500), fica trivial.
- **Observacoes:**
> _[espaco]_

---

#### Issue #710 — DELETE de `frete_itens` sem filtro `empresa_id` em `CadastroFreteController.handleExcluirFrete`
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroFreteController.java:1563-1567`
- **Problema:** `DELETE FROM frete_itens WHERE id_frete = ?` sem `AND empresa_id = ?`. Quebra regra #6 do CLAUDE.md.
- **Impacto:** Hoje baixo (desktop empresa_id fixo); drift com regra oficial.
- **Fix sugerido:**
```sql
DELETE FROM frete_itens
WHERE id_frete IN (SELECT id_frete FROM fretes WHERE id_frete = ? AND empresa_id = ?)
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #503 — `AdminPspController` e `PspController` duplicam endpoints PSP
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `AdminPspController.java` + `PspController.java`
- **Problema:** Ambos expoem `/status` e `/onboarding`.
- **Fix sugerido:** Manter os dois controllers (split por roles e bom). Garantir que compartilhem as mesmas DTOs e cubra ambas com o mesmo teste.
- **Observacoes:**
> _[espaco]_

---

#### Issue #504 — BCrypt com custos diferentes entre Desktop e BFF para o mesmo banco
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `src/dao/UsuarioDAO.java:25` (cost=12), `naviera-web/server/routes/{auth,admin,cadastros}.js` (cost=10)
- **Problema:** Desktop cost 12, BFF cost 10. Bcrypt detecta cost do hash — verificacao funciona. Cost 10 aceitavel em 2026 mas divergencia e inconsistente.
- **Fix sugerido:**
```js
export const BCRYPT_COST = Number(process.env.BCRYPT_COST || 12)
export async function hashSenha(senha) { return bcrypt.hash(senha, BCRYPT_COST) }
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #508 — Metodos de >100 linhas no Desktop
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `CadastroFreteController.java`
- **Linha(s):** 284-428, 1650-1734, 1546-1597
- **Problema:** Funcoes com >50 linhas e multiplas responsabilidades.
- **Fix sugerido:** Extrair metodos privados por responsabilidade.
- **Observacoes:**
> _[espaco]_

---

#### Issue #509 — Pages React grandes: ReviewOCR 755, Fretes 710, GestaoFuncionarios 683, Encomendas 676, Passagens 561
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `naviera-web/src/pages/{ReviewOCR,Fretes,GestaoFuncionarios,Encomendas,Passagens}.jsx`
- **Problema:** >500 linhas JSX com logica de fetch, state, validacao e UI.
- **Fix sugerido:** Extrair custom hooks, subcomponentes por secao. Meta: <400 linhas.
- **Observacoes:**
> _[espaco]_

---

#### Issue #510 — Route BFF `ocr.js` 935 linhas, `cadastros.js` 862
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `naviera-web/server/routes/{ocr,cadastros}.js`
- **Problema:** Arquivo unico com multiplas entidades.
- **Fix sugerido:** Quebrar por entidade (`routes/cadastros/cliente.js`, `conferente.js`).
- **Observacoes:**
> _[espaco]_

---

#### Issue #511 — Controllers da API chamando `JdbcTemplate` direto (sem Service)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `OpEmbarcacaoController.java`, `OpRotaController.java`
- **Problema:** Controllers pulando a camada de service.
- **Fix sugerido:** Rotear para `EmbarcacaoService` ja existente.
- **Observacoes:**
> _[espaco]_

---

#### Issue #512 — Desktop GUI: controllers fazem SQL JDBC direto
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** transversal, >27 arquivos em `src/gui/*.java` (87 ocorrencias de ConexaoBD)
- **Problema:** Controllers conhecendo detalhes de transacao e conexao.
- **Fix sugerido:** Mover para DAOs/Services. Meta: zero `ConexaoBD.getConnection()` em `src/gui/*Controller.java`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #513 — `RotaController` expoe `findAll()` sem filtro de tenant
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/RotaController.java:13`
- **Problema:** `return ResponseEntity.ok(repo.findAll());` sem filtrar por empresa_id. Dois controllers fazendo "a mesma coisa" com semanticas diferentes.
- **Fix sugerido:** Unificar ou remover um dos dois. Se `/rotas` existe por algum motivo legado, renomear para `/public/rotas`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #515 — 4 arquivos `.env.example` com esquemas divergentes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `/.env.example`, `naviera-api/.env.example`, `naviera-web/.env.example`, `naviera-app/.env.example`
- **Problema:** DB_PORT 5432 vs 5437, CORS_ORIGINS conflitantes, JWT_SECRET em 3 arquivos, NODE_ENV nao documentado.
- **Fix sugerido:** Manter envs separados por modulo (motivo legitimo). Adicionar header `# IMPORTANTE: JWT_SECRET deve ser identico em naviera-api/.env e naviera-web/.env` e `# Porta 5437 so dev local (5432 em Docker)`. Documentar em CLAUDE.md.
- **Observacoes:**
> _[espaco]_

---

#### Issue #517 — CLAUDE.md diz "JWT expira em 24h" mas codigo usa 8h
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `CLAUDE.md:120`, `application.properties:33`, `server/middleware/auth.js:13`
- **Fix sugerido:** Atualizar CLAUDE.md para "8h" ou alinhar codigo a 24h.
- **Observacoes:**
> _[espaco]_

---

#### Issue #528 — Middlewares BFF sem testes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos sem testes:** `naviera-web/server/middleware/{auth,tenant,rateLimit,validate}.js`
- **Fix sugerido:** Criar `server/tests/tenant.test.js` com mock de pool.
- **Observacoes:**
> _[espaco]_

---

#### Issue #529 — App mobile sem testes
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-app/` inteiro
- **Fix sugerido:** Adicionar vitest. Comecar por `helpers.test.js` e `hooks/useApi.test.js`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #712 — `PassagemService.comprar` gera `numeroBilhete` com `System.currentTimeMillis() % 1000000`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `naviera-api/.../service/PassagemService.java:105`
- **Problema:** Colisao em ~16min; mesmo ms gera bilhetes identicos.
- **Fix sugerido:** Usar sequence dedicada `seq_numero_bilhete` ou seguir padrao `BilheteService.comprar`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #713 — Duplicacao estrutural de rota `/rotas` e `/op/rotas`
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivos:** `RotaController.java` (sem tenant), `OpRotaController.java` (com tenant)
- **Problema:** Dois endpoints sem semantica clara.
- **Fix sugerido:** Unir ou renomear. Se `/rotas` e publico, prefixar como `/public/rotas`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #717 — `CadastroFreteController` encoding corrompido em strings de UI visiveis
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java:1548-1555`
- **Problema:** Strings `"NÃ£o hÃ¡ frete selecionado para exclusÃ£o."` aparecem em AlertHelper na UI.
- **Fix sugerido:** Converter arquivo para UTF-8 limpo via `iconv`. Adicionar `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`. Rodar em todos os `src/gui/*Controller.java` afetados.
- **Observacoes:**
> _[espaco]_

---

#### Issue #505 — `crudFactory.js` usa `.then/.catch` enquanto todo resto do BFF usa async/await
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/utils/crudFactory.js`
- **Problema:** Mistura de estilos.
- **Fix sugerido:** Converter para async/await.
- **Observacoes:**
> _[espaco]_

---

#### Issue #514 — `tenant.js` acopla roteamento a chamadas diretas de DB
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/server/middleware/tenant.js:43-54`
- **Problema:** Middleware faz `pool.query` direto em vez de delegar a um `empresaService`.
- **Fix sugerido:** Extrair `empresaService.findBySlug(slug)`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #516 — `vite.config.js` hardcoda portas
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-web/vite.config.js:7,10`
- **Fix sugerido:**
```js
server: {
  port: Number(process.env.VITE_PORT || 5174),
  proxy: { '/api': { target: process.env.VITE_BFF_URL || 'http://localhost:3002', changeOrigin: true } }
}
```
- **Observacoes:**
> _[espaco]_

---

#### Issue #518 — Drift de contagens em CLAUDE.md
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `CLAUDE.md`
- **Problema:** 42 pages (CLAUDE 29), 15 rotas BFF (10), 29 DAOs (27).
- **Fix sugerido:** Atualizar CLAUDE.md.
- **Observacoes:**
> _[espaco]_

---

#### Issue #519 — CLAUDE.md nao menciona `/psp` endpoints nem PSP modulo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `CLAUDE.md`
- **Fix sugerido:** Adicionar secao "Integracao PSP (Asaas)".
- **Observacoes:**
> _[espaco]_

---

#### Issue #522 — TODOs antigos em `PassagemService` aguardando DTO tipado
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/service/PassagemService.java`
- **Linha(s):** 33, 63, 183, 219, 244
- **Problema:** 5 ocorrencias de `// TODO DM069: Replace List<Map<String, Object>> with typed DTO`.
- **Fix sugerido:** Criar DTOs sugeridos e fechar DM069.
- **Observacoes:**
> _[espaco]_

---

#### Issue #523 — TODO em `PublicController` sobre bucket de rate limit
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/src/main/java/com/naviera/api/controller/PublicController.java:44`
- **Fix sugerido:** Implementar ou fechar.
- **Observacoes:**
> _[espaco]_

---

#### Issue #524 — Arquivo `src/gui/util/A.txt` e anotacao pessoal commitada
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/A.txt`
- **Fix sugerido:** Remover ou mover para um issue tracker.
- **Observacoes:**
> _[espaco]_

---

#### Issue #525 — `db.properties.bak2` e `log_erros.txt` no tree
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivos:** `db.properties.bak2`, `log_erros.txt`, `RELATÓRIO GERAL DO PROJETO SISTEMA 19.02.25.txt`
- **Fix sugerido:** `git rm --cached` nos 3.
- **Observacoes:**
> _[espaco]_

---

#### Issue #526 — Comentarios com encoding corrompido no Desktop
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivos:** `VenderPassagemController.java`, `CadastroFreteController.java`
- **Fix sugerido:** Converter arquivos para UTF-8 limpo com iconv. Adicionar `.editorconfig` com `charset = utf-8`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #530 — `EncomendaService.buscarPorCliente` faz LIKE por NOME, nao por cliente
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `naviera-api/.../service/EncomendaService.java:40`
- **Problema:** Assinatura sugere busca por ID, mas internamente faz LIKE por nome.
- **Fix sugerido:** Renomear para `buscarPorNomeDoDestinatario` ou migrar para FK `id_cliente_app_destinatario`.
- **Observacoes:**
> _[espaco]_

---

#### Issue #715 — `RELATÓRIO GERAL DO PROJETO SISTEMA 19.02.25.txt` commitado no root
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `RELATÓRIO GERAL DO PROJETO SISTEMA 19.02.25.txt` (5.9KB)
- **Problema:** Documento antigo no root, nome com acentos e espacos.
- **Fix sugerido:** `git rm --cached "RELATÓRIO GERAL..."` + mover para `docs/archive/`.
- **Observacoes:**
> _[espaco]_

---

---

## 3. CONTRA-VERIFICACAO

### 3.1 — Falsos Positivos Descartados (13)

| Issue | Motivo (resumido) |
|-------|-------------------|
| #010 | API Spring nao usa `TenantContext` ThreadLocal — usa `TenantUtils` stateless. Classe ThreadLocal so existe no Desktop. |
| #028 | Logica de auth.js esta correta: ramo OCR/Admin captura ANTES do fallback dev. Risco descrito e configuracao de deploy. |
| #032 | `TenantUtils.getEmpresaId()` sempre retorna `Integer`. ClassCastException descrita e cenario hipotetico. |
| #116 | Nginx tem `proxy_set_header Host $host` + `server_name` explicito. Bypass so possivel se BFF exposto direto. |
| #211 | Autor ja descartou durante o scan. |
| #230 | DELETE `encomenda_itens` ja inclui `empresa_id` no subquery. |
| #241 | `Number("0E-2") === 0` + comparacao `> 0` e comportamento correto. |
| #325 | Vite 5 exige Node 18+; `AbortSignal.timeout` esta disponivel. |
| #326 | Imports nao-usados sao lint warnings, nao bugs. |
| #423 | Entidades JPA nao tem `@OneToMany`/`@ManyToOne`. Risco inexistente. |
| #425 (parcial) | Hook `useApi` do mobile JA usa `AbortController`. Issue mantido apenas para pagina Dashboard web. |
| #426 | Fundido em #402. |
| #520/#521 | Comentario descreve comportamento condicional futuro, nao obsoleto. Estilo subjetivo. |

### 3.2 — Severidades Ajustadas (31)

**Elevadas (5):**
| Issue | De → Para | Motivo |
|-------|-----------|--------|
| **#114** | ALTO → **CRITICO** | Mesma raiz do #100; permite admin de empresa A desativar empresa B. |
| **#411** | ALTO → **CRITICO** | PSP chamado dentro de `@Transactional` + HikariCP=10 — derruba Spring Boot. |
| #222 | MEDIO → ALTO | Sync "cliente sempre vence" causa perda real de dados. |
| #226 | MEDIO → ALTO | totp_secret em plain text = risco concreto com backup leak. |
| #235 | BAIXO → MEDIO | Subconta Asaas para empresa desativada gera obrigacao contratual. |

**Rebaixadas (26):**
| Issue | De → Para | Motivo |
|-------|-----------|--------|
| #001, #002 | CRITICO → MEDIO | `client.release()` no `finally` SEMPRE executa. Nao ha leak. |
| #009 | CRITICO → ALTO | `bcryptjs` ESTA no package.json. Import primario sempre sucede. |
| #015, #018 | ALTO → MEDIO | Volume atual do MVP nao justifica impacto descrito. |
| #101 | CRITICO → ALTO | .env NAO esta commitado. Rotacao necessaria mas sem evidencia de leak. |
| #104 | CRITICO → ALTO | AdminPspController JA valida ROLE_ADMIN. Vetor residual apenas. |
| #117 | ALTO → MEDIO | Desktop e single-tenant por design. Risco so em configuracao errada. |
| #128 | MEDIO → BAIXO | App e PWA sob HTTPS com CSP. XSS nao foi encontrado. |
| #215 | ALTO → MEDIO | Impacto e de relatorio, nao corrupcao financeira. |
| #326 | BAIXO → INFORMACIONAL | Lint, nao bug. |
| #400 | CRITICO → ALTO | Derruba em 50 tenants × 1M encomendas. MVP atual nao tem esse volume. |
| #401 | CRITICO → ALTO | Payload ~200KB irritante em 3G, nao derruba. |
| #402 | CRITICO → ALTO | 10 conexoes atende MVP atual. Fix e 2 linhas. |
| #504 | ALTO → MEDIO | BCrypt detecta cost do hash. Cost 10 ainda aceitavel em 2026. |
| #515 | ALTO → MEDIO | Envs separados por modulo tem razao legitima. |

### 3.3 — Fixes Revisados (22)

| Issue | Motivo |
|-------|--------|
| #003 | `queryForMap` pode lancar `EmptyResultDataAccessException`. Usar `queryForList` + check empty. |
| #005 | `id_rota` e NOT NULL no schema — fix reduz para apenas guard de `empresa_id`. |
| #007 | Migration de deduplicacao OBRIGATORIA antes do UNIQUE INDEX. |
| #008 | `seq_numero_bilhete` JA existe (schema L434); usar `%08d`. |
| #100/#114 | Bootstrap: coluna `usuarios.super_admin BOOL` + seed SQL + `JwtFilter` usando flag. |
| #104 | Adicionar `requireAdmin(auth)` tambem no PspController + validar `empresa.ativo=TRUE`. |
| #108 | Deletar header `X-Tenant-Slug` quando `req.ip != loopback`; exigir `ALLOW_DEV_LOGIN=1`. |
| #112 | `@PostConstruct` fail-closed em prod, warn loud em dev. |
| #113 | Extrair `CodigoAtivacaoGenerator` utility compartilhada. |
| #201 | Tabela `webhook_events(evento_id UNIQUE)` para idempotencia. |
| #202 | Fix confirmado OK — `import { randomUUID } from 'crypto'`. |
| #204 | Antes de deletar endpoint, grep se frontend ainda chama. |
| #205/#237/#411 | Saga/outbox — se falhar PSP, **reverter** status local. |
| #221 | Migration: indice composto `(uuid, empresa_id)` + ON CONFLICT ajustado. |
| #233 | Bloquear EMBARCADO+CANCELADA + exigir admin + motivo >=20 chars. |
| #316 | Consolidar com #315 num unico patch Dockerfile. |
| #402 | pgbouncer primeiro, ajustar pool depois. |
| #412 | Gerar QR no cliente com lib `qrcode` a partir de `qrCodePayload`. |
| #415 | `p-limit(2)` mais leve que `sharp`. |
| #420 | Cachear `null` (slug nao encontrado) 30s pra evitar brute-force. |
| #428 | Citar TODO DM069 ja rastreado. |
| #515 | Manter envs separados; adicionar header documentando sincronizacao. |

### 3.4 — Pontos Cegos Declarados

1. `naviera-api/.../service/*WriteService.java` — 12 arquivos nao lidos integralmente. `SyncService.java` (466 LOC) nao auditado.
2. `src/dao/*.java` Desktop (29 DAOs) — sample pequeno.
3. `src/gui/*.java` (67 controllers JavaFX) — cobertura parcial.
4. `naviera-api/.../controller/*.java` — 26 de 28 controllers nao lidos linha-a-linha.
5. BFF routes nao inspecionados a fundo: viagens, rotas, embarcacoes, passagens, encomendas, cadastros, admin, agenda, estornos, documentos, dashboard.
6. `naviera-web/src/pages/*.jsx` — 42 paginas, so Dashboard e Financeiro amostrados.
7. `database_scripts/*.sql` — 30 migrations nao auditadas para constraints UNIQUE/CHECK.
8. Teste de carga — avaliacoes de CRITICO/ALTO em performance sao teoricas.
9. `nginx/*.conf` — timeouts upstream, keepalive nao inspecionados.
10. `naviera-api/.../security/JwtFilter.java` + `JwtUtil.java` — nao lidos em detalhe.
11. `naviera-ocr/`, `naviera-site/` — fora de escopo declarado.
12. PSP webhook — confirmado ausente (grep 0 matches). Issue #201 REAL.
13. `certs/` nao inspecionado.
14. BFF `trust proxy` quando Nginx em container separado pode precisar range de IPs.

---

## 4. PLANO DE CORRECAO

### Sprint 1 — Criticos (AGORA) — 30 issues

- [ ] **#003** — NullPointerException em EncomendaService.pagar — `naviera-api/.../service/EncomendaService.java`
- [ ] **#004** — NPE identico em FreteService.pagar — `naviera-api/.../service/FreteService.java`
- [ ] **#005** — NPE em PassagemService.comprar — `naviera-api/.../service/PassagemService.java`
- [ ] **#006** — NPE em tarifa.valor_transporte/alimentacao/desconto — `naviera-api/.../service/PassagemService.java`
- [ ] **#007** — ClassCastException + race em passageiros — `naviera-api/.../service/PassagemService.java`
- [ ] **#008** — Numero de bilhete colisivel (mod 1e6) — `naviera-api/.../service/PassagemService.java`
- [ ] **#100** — Admin cross-empresa — `naviera-api/.../controller/AdminController.java`
- [ ] **#102** — Auto-promocao Administrador BFF — `naviera-web/server/routes/cadastros.js`
- [ ] **#103** — Escalacao de privilegios via /op/cadastros/usuarios — `naviera-api/.../service/CadastrosWriteService.java`
- [ ] **#105** — /rotas vaza cross-tenant — `naviera-api/.../controller/RotaController.java`
- [ ] **#106** — /encomendas/:id/itens cross-tenant — `naviera-web/server/routes/encomendas.js`, `fretes.js`
- [ ] **#107** — Ownership weak em pagamento — `naviera-api/.../service/EncomendaService.java`, `FreteService.java`
- [ ] **#108** — Login BFF aceita qualquer empresa em dev — `naviera-web/server/routes/auth.js`
- [ ] **#114** — AdminController nao valida role — `naviera-api/.../controller/AdminController.java`
- [ ] **#200** — Desktop e API divergem no conceito "viagem ativa" — `src/dao/ViagemDAO.java` vs API/BFF
- [ ] **#201** — Webhook Asaas inexistente — `naviera-api/.../psp/`
- [ ] **#202** — ocr.js usa `crypto.randomUUID()` sem importar — `naviera-web/server/routes/ocr.js`
- [ ] **#203** — OpPassagemService colunas inexistentes — `naviera-api/.../service/OpPassagemService.java`
- [ ] **#204** — /financeiro/estornar zera valor_pago — `naviera-web/server/routes/financeiro.js`
- [ ] **#205** — PSP cria cobranca antes de confirmar — `naviera-api/.../service/*.java`
- [ ] **#300** — AsaasGateway sem timeout — `naviera-api/.../psp/AsaasGateway.java`
- [ ] **#301** — Webhook sem idempotencia — `naviera-api/.../psp/PspCobrancaService.java`
- [ ] **#304** — ocr.js crypto sem import (duplicado #202 mas referenciado em Cat 4) — `naviera-web/server/routes/ocr.js`
- [ ] **#305** — Webhook secret vazio retorna true — `naviera-api/.../psp/AsaasGateway.java`
- [ ] **#308** — PassagemService.comprar PSP call em @Transactional — `naviera-api/.../service/PassagemService.java`
- [ ] **#311** — FirebaseConfig falha silenciosa — `naviera-api/.../config/FirebaseConfig.java`
- [ ] **#315** — Dockerfile sem tini/STOPSIGNAL — `naviera-api/Dockerfile`
- [ ] **#403** — financeiro/dashboard UNION ALL sem LIMIT — `naviera-web/server/routes/financeiro.js`
- [ ] **#411** — PSP inline em @Transactional — `naviera-api/.../service/{Encomenda,Frete,Passagem}Service.java`
- [ ] **#650** — X-Tenant-Slug sem validar trusted proxy — `naviera-web/server/middleware/tenant.js`

### Sprint 2 — Altos (esta semana) — 77 issues

**Cat 1 Bugs (12):** #009, #011, #012, #013, #014, #016, #017, #019, #020, #021, #022, #023, #600

**Cat 2 Security (16):** #101, #104, #109, #110, #111, #112, #113, #115, #118, #119, #120, #121, #651, #655, #656, #658, #661

**Cat 3 Logic (19):** #206, #207, #208, #209, #210, #212, #213, #214, #216, #217, #218, #222, #226, #233, #237, #238, #652, #653, #711

**Cat 4 Resilience (8):** #302, #303, #307, #309, #310, #313, #314, #319

**Cat 5 Performance (13):** #400, #401, #402, #404, #405, #406, #407, #409, #412, #413, #415, #429, #703

**Cat 6 Maintainability (7):** #500, #501, #502, #506, #507, #527, #710

### Sprint 3 — Medios (este mes) — 84 issues

**Cat 1 Bugs (14):** #001, #002, #015, #018, #024, #025, #026, #027, #029, #030, #031, #033, #036

**Cat 2 Security (11):** #117, #122, #123, #124, #125, #126, #127, #129, #130, #131, #654

**Cat 3 Logic (20):** #215, #219, #220, #221, #223, #224, #225, #227, #228, #229, #231, #232, #234, #235, #657, #659, #660, #662, #714, #716

**Cat 4 Resilience (12):** #306, #312, #316, #317, #320, #322, #323, #324, #327, #328, #329, #330

**Cat 5 Performance (13):** #408, #410, #414, #416, #417, #418, #419, #421, #424, #430, #431, #700, #702

**Cat 6 Maintainability (15):** #503, #504, #508, #509, #510, #511, #512, #513, #515, #517, #528, #529, #712, #713, #717

### Backlog — Baixos — 34 issues

**Cat 1 Bugs (5):** #034, #035, #037, #038, #601

**Cat 2 Security (5):** #128, #132, #133, #134, #135

**Cat 3 Logic (3):** #236, #239, #240

**Cat 4 Resilience (3):** #318, #321, #331

**Cat 5 Performance (6):** #420, #422, #425, #427, #428, #701

**Cat 6 Maintainability (12):** #505, #514, #516, #518, #519, #522, #523, #524, #525, #526, #530, #715

---

## 5. HISTORICO DE AUDITORIAS

| Versao | Data | Total | Criticos | Status |
|--------|------|-------|----------|--------|
| V1.0 | 2026-04-07 | ~194 | ? | Archive |
| V1.1 | 2026-04-08 | ? | ? | Archive |
| V1.2 | 2026-04-14 | 112 | 12 | Archive (todas resolvidas em 895adc9) |
| **V1.3** | **2026-04-18** | **225** | **30** | **Atual — REPROVADO** |

---

## 6. NOTAS GERAIS

- Auditoria V1.3 regrediu em relacao a V1.2 porque NOVOS modulos foram adicionados: integracao PSP Asaas, OCR Gemini, webhook, onboarding self-service, ativacao/desativacao de empresas. Esses modulos ainda nao passaram por hardening.
- O desktop **continua limpo** em multi-tenancy apos os fixes do V1.2 — nenhuma nova regressao detectada nas DAOs auditadas.
- A maior parte dos 30 CRITICOs estao concentrados em 3 areas: **tenant isolation** (9), **PSP Asaas/webhook** (5), **Pagamento saga** (4).
- Recomenda-se NAO fazer deploy de producao ate completar Sprint 1.
- Executar `audit-5-deep security` antes do deploy para garantir que nao ha mais bypass de tenant cross-empresa.

---

*Gerado por Claude Code — Revisao humana obrigatoria*
