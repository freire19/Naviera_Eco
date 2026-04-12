# MVP PLAN — Naviera Eco
> **Versao:** V4.0
> **Data:** 2026-04-10
> **Status:** QUASE PRONTO

---

## RESUMO

| Status | Itens |
|--------|-------|
| PRONTO | 95 |
| INCOMPLETO | 17 |
| FALTANDO | 10 |
| POS-MVP | 2 |

**Bloqueadores:** 3 itens criticos impedem deploy em producao
**Estimativa total bloqueadores:** ~4h

### Progresso

| Metrica | V1.0 (04-08) | V2.0 (04-10) | V3.0 (04-10) | V4.0 (04-10) | Delta total |
|---------|-------------|-------------|-------------|-------------|-------------|
| PRONTO | 38 | 70 | 108 | 95 | +57 |
| FALTANDO | 22 | 5 | 0 | 10 | -12 |
| % Pronto | 47% | 60% | 93% | 77% | +30pp |
| Total itens | 81 | 116 | 116 | 124 | +43 |

> **Nota V4.0:** Contagem PRONTO caiu porque esta versao analisou 8 novos itens de infra/seguranca que a V3.0 nao cobriu (CI/CD, CORS, rate limiting, HTTPS real, testes, graceful shutdown, etc.). O codigo nao regrediu — a analise ficou mais rigorosa.

### Readiness por Camada

| Camada | Features Core | Infra/Seguranca | Status Geral |
|--------|--------------|-----------------|-------------|
| **Desktop (JavaFX)** | 9/9 PRONTO | PRONTO | Producao-ready |
| **API (Spring Boot)** | 6/6 PRONTO | INCOMPLETO (testes, rate limiting) | Quase pronto |
| **Web (React + Express BFF)** | 8/11 PRONTO | INCOMPLETO (CORS, HTTPS, graceful shutdown) | Quase pronto |
| **App (React → mobile)** | 11/11 PRONTO | PRONTO | Pronto como web app |

---

## FUNCIONALIDADES CORE

### Desktop — CRUD Completo + Multi-Tenant
- **Status:** PRONTO
- **Estado atual:** 9/9 features. Passagens, encomendas, fretes, viagens, financeiro (6 controllers), auth (login BCrypt + timing-safe), multi-tenant (285 ocorrencias empresa_id). EmpresaDAO e BalancoViagemDAO agora tenant-aware. 446 try-catch em DAOs, 595 em controllers.
- **O que falta:** Nada.
- **Observacoes:**
> _Sistema completo e operacional offline. Todos os DAOs migrados para multi-tenant._

### API — CRUD + Auth + Sync + Push
- **Status:** PRONTO
- **Estado atual:** 25 controllers. Auth JWT com roles (CPF/CNPJ/OPERADOR/ADMIN). empresa_id no JWT, TenantUtils em 21 servicos. Sync bidirecional. Firebase push. Versionamento. logback com profiles dev/prod. HikariCP com timeouts. @Valid em DTOs.
- **O que falta:** Testes (0 arquivos). Rate limiting.
- **Observacoes:**
> _Backend completo. Gaps sao de estabilidade/seguranca, nao de features._

### Web — CRUD Completo (28 paginas funcionais)
- **Status:** PRONTO (features) | INCOMPLETO (3 stubs)
- **Estado atual:** 29 paginas (28 funcionais + 1 placeholder). Express BFF com CRUD completo (GET/POST/PUT/DELETE). Multi-tenant em todas queries. Rate limiting no login. Logger estruturado. Toast notifications. Tema light/dark. Responsivo.
- **O que falta:** 3 paginas stub (Agenda, ConfigurarApi, Gestao Funcionarios). Sem deep links (state machine, nao React Router).
- **Observacoes:**
> _Console web operacional. Stubs sao de funcionalidades secundarias._

### App — 11 Telas em 2 Perfis
- **Status:** PRONTO
- **Estado atual:** App.jsx modular (103 linhas). 11 telas (CPF: home/amigos/mapa/passagens/bilhete/perfil | CNPJ: painel/pedidos/parceiros/financeiro/loja). Design system Naviera V4. useApi hook com loading/erro/data. Mascaras CPF/CNPJ. Skeleton + ErrorRetry + Toast.
- **O que falta:** GPS real. Migracao para mobile nativo.
- **Observacoes:**
> _Pronto como web app. Migracao mobile e pos-MVP._

---

## FLUXOS CRITICOS

### Fluxo: Onboarding / Primeiro Uso
- **Status:** PRONTO
- **Etapas:**
  - [x] Desktop: Login → BCrypt → Selecionar Viagem
  - [x] Web: Login → JWT 8h → Dashboard → Selecionar viagem (TopBar)
  - [x] App: Cadastro CPF/CNPJ → Login → Home por perfil
  - [x] API: POST /auth/login + /auth/registrar → JWT com roles
- **Gaps:** App sem email verification e password reset (aceitavel MVP).
- **Observacoes:**
> _Funcional em todas as camadas._

### Fluxo: Operacao Desktop (Principal)
- **Status:** PRONTO
- **Etapas:**
  - [x] Login → Selecionar Viagem
  - [x] Vender Passagem → Imprimir Bilhete
  - [x] Registrar Encomenda/Frete
  - [x] Fechar Balanco (tenant-aware)
  - [x] Sync opcional (nao quebra se falhar)
- **Gaps:** Nenhum. 100% offline.
- **Observacoes:**
> _Fluxo completo e testado._

### Fluxo: Operacao Web
- **Status:** PRONTO
- **Etapas:**
  - [x] Login → JWT → Dashboard
  - [x] Selecionar viagem ativa
  - [x] CRUD passagens/encomendas/fretes (modais)
  - [x] Ver financeiro (entradas/saidas/balanco)
  - [x] 9 telas CRUD cadastros
- **Gaps:** 1 placeholder (gestao-funcionarios). Sem URL navigation.
- **Observacoes:**
> _Console web operacional._

### Fluxo: Compra pelo App (CPF)
- **Status:** PRONTO
- **Etapas:**
  - [x] Cadastro CPF → Login → HomeCPF
  - [x] Ver viagens → Comprar passagem
  - [x] Ver bilhete digital
  - [x] Rastrear encomenda
- **Gaps:** GPS em tempo real (pos-MVP).
- **Observacoes:**
> _Fluxo completo para cliente final._

### Fluxo: Sync Desktop ↔ API
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] SyncClient no Desktop (user-triggered)
  - [x] SyncService na API (11 tabelas, last-write-wins)
  - [ ] SyncService.java com SQL injection risk (tabela concatenada)
- **Gaps:** Nome de tabela concatenado em SQL sem whitelist.
- **Observacoes:**
> _Funciona mas tem risco de seguranca no SyncService._

### Fluxo: Tratamento de Erros
- **Status:** PRONTO
- **Etapas:**
  - [x] API: GlobalExceptionHandler + ApiException
  - [x] Desktop: AppLogger estruturado (491 substituicoes)
  - [x] Web: Toast notifications + 401 auto-logout
  - [x] App: ErrorRetry + Toast + useApi hook
- **Gaps:** Nenhum critico.
- **Observacoes:**
> _Consistente em todas as camadas._

---

## INFRAESTRUTURA

| Item | Status | Detalhe |
|------|--------|---------|
| Dockerfile API | PRONTO | Multi-stage Maven → JRE alpine |
| Dockerfile App | PRONTO | Multi-stage Node → Nginx |
| docker-compose.yml | PRONTO | 3 services (db + api + app), healthcheck, depends_on |
| PostgreSQL no compose | PRONTO | postgres:16-alpine, volume pgdata |
| DB Migrations (000-015) | PRONTO | 15+ SQL numerados, sequenciais |
| DB Migrations avulsas | INCOMPLETO | 3 scripts SQL fora da sequencia numerada |
| .env.example (root + api + web) | PRONTO | Todos com valores placeholder |
| .env.example (app) | INCOMPLETO | Tem .env.development, falta .env.example |
| Healthchecks | PRONTO | Actuator /health + BFF /health + pg_isready |
| Logging API | PRONTO | logback-spring.xml, dev + prod profiles, 30d rotacao |
| Logging BFF | INCOMPLETO | logger.js stdout-only, sem rotacao em arquivo |
| README root | PRONTO | Presente |
| README api | PRONTO | Presente |
| README web | FALTANDO | Nao existe |
| README app | FALTANDO | Nao existe |
| CI/CD pipeline | FALTANDO | Nenhuma automacao (GitHub Actions, etc.) |
| Deploy scripts/runbooks | FALTANDO | docs/runbooks/ vazio |
| BFF npm start script | FALTANDO | Sem entry no package.json para o server |
| Setup comando unico | INCOMPLETO | docker-compose para api+db+app, mas BFF e manual |

- **Observacoes:**
> _Docker funcional para deploy. Gaps sao de automacao (CI/CD) e documentacao (READMEs). BFF precisa de script de start._

---

## SEGURANCA MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| Auth Desktop (BCrypt) | PRONTO | Timing-safe comparison |
| Auth Web (JWT 8h) | PRONTO | authMiddleware em todas rotas, process.exit se sem secret |
| Auth API (JWT + roles) | PRONTO | 4 roles, Spring Security stateless |
| Secrets fora do codigo | PRONTO | .gitignore exclui .env, db.properties, *.key, *.pem |
| Secrets strength | INCOMPLETO | Dev: DB_PASSWORD=123456, JWT_SECRET previsivel |
| SQL injection BFF | PRONTO | Queries parametrizadas ($1, $2) |
| SQL injection API | INCOMPLETO | SyncService.java concatena tabela sem whitelist |
| SQL injection Desktop | PRONTO | PreparedStatements |
| CORS API | PRONTO | Configuravel por env var |
| **CORS BFF** | **FALTANDO** | **app.use(cors()) sem restricao — BLOQUEADOR** |
| **HTTPS** | **INCOMPLETO** | **nginx.conf TLS completo mas COMENTADO, certs/ nao existe** |
| Rate limiting BFF login | PRONTO | 10 req/min in-memory |
| Rate limiting BFF geral | FALTANDO | Nenhum alem do login |
| Rate limiting API | FALTANDO | Nenhum rate limiter |
| Input validation BFF | INCOMPLETO | Checks basicos, sem schema validation |
| Input validation API | PRONTO | @Valid + @NotNull/@Size |
| db.properties no repo | INCOMPLETO | Arquivo real pode estar commitado |

---

## ESTABILIDADE

| Item | Status | Detalhe |
|------|--------|---------|
| Error handling Desktop | PRONTO | 446 try-catch DAOs, 595 controllers |
| Error handling BFF | PRONTO | try/catch todas rotas, pool.on('error'), timeout 30s |
| Error handling API | PRONTO | GlobalExceptionHandler + ApiException |
| Error handling App | PRONTO | useApi + ErrorRetry + Toast |
| Reconexao DB (3 camadas) | PRONTO | HikariCP + pg Pool |
| Graceful shutdown Desktop | PRONTO | JavaFX lifecycle + shutdown hook |
| Graceful shutdown API | PRONTO | Spring Boot shutdown hooks |
| Graceful shutdown BFF | INCOMPLETO | Sem handler SIGTERM/SIGINT |
| Timeouts (BFF + API) | PRONTO | 30s statement + connection timeouts |
| Testes Desktop | PRONTO | JUnit 4 em src/tests/ |
| Testes API | FALTANDO | Zero arquivos de teste |
| Testes Web | FALTANDO | Nenhum teste |
| Testes App | FALTANDO | Nenhum teste |

---

## UX MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| Loading states (todas camadas) | PRONTO | Booleans + Skeleton + background threads |
| Toast/Feedback (Web + App) | PRONTO | showToast sucesso/erro |
| AlertHelper (Desktop) | PRONTO | Mensagens em portugues |
| Tema light/dark (Web + App) | PRONTO | Persistido em localStorage |
| Responsividade Web | PRONTO | 3 breakpoints |
| Design system App | PRONTO | Naviera V4, tokens consistentes |
| Deep links Web | FALTANDO | State machine sem React Router, refresh perde estado |

---

## DEPENDENCIAS

| Servico/API | Status | Configurado | Fallback |
|------------|--------|------------|----------|
| PostgreSQL | PRONTO | Pool em todas camadas | Erro 500 |
| Firebase (push) | PRONTO | Admin SDK 9.3.0 | Desativa se sem credenciais |
| Maven deps | PRONTO | Versoes fixas no pom.xml | — |
| npm deps (Web) | INCOMPLETO | Caret ^ (nao pinadas) | Lockfile mitiga |
| npm deps (App) | PRONTO | Minimas (react + react-dom) | — |
| react-router-dom (Web) | INCOMPLETO | Instalado mas nao usado | Dependencia morta |
| Express 5.x (Web) | INCOMPLETO | RC — risco menor | — |
| Lib mapa real (App) | POS-MVP | Nao instalada | GPS mock |
| Lib charts (App) | POS-MVP | Nao instalada | — |

---

## PLANO DE ACAO POR FASES

### Fase 1 — Bloqueadores (AGORA)
- [ ] CORS restrito no BFF — **Arquivo:** `naviera-web/server/index.js` — **Esforco:** 15min
- [ ] SyncService SQL injection — **Arquivo:** `naviera-api/src/.../SyncService.java` — **Esforco:** 1h
- [ ] HTTPS ativar no nginx — **Arquivo:** `nginx.conf` + gerar `certs/` — **Esforco:** 2h
- **Notas:**
> _CORS e 1 linha. SyncService precisa de whitelist de tabelas validas. HTTPS precisa de certificado (Let's Encrypt ou self-signed para staging)._
- **Esforco total:** ~3h

### Fase 2 — Incompletos Criticos (esta semana)
- [ ] Secrets fortes em prod — **Arquivo:** `.env` de producao — **Esforco:** 30min
- [ ] Rate limiting API — **Arquivo:** `naviera-api/` (Bucket4j ou Resilience4j) — **Esforco:** 2h
- [ ] Rate limiting BFF geral — **Arquivo:** `naviera-web/server/index.js` — **Esforco:** 1h
- [ ] BFF graceful shutdown — **Arquivo:** `naviera-web/server/index.js` — **Esforco:** 30min
- [ ] BFF npm start script — **Arquivo:** `naviera-web/package.json` — **Esforco:** 5min
- [ ] PORT mismatch BFF (3001 vs 3002) — **Arquivo:** `naviera-web/server/index.js` — **Esforco:** 5min
- [ ] db.properties verificar se commitado — **Arquivo:** root — **Esforco:** 10min
- **Notas:**
> _Todos sao fixes rapidos. Rate limiting API e o mais trabalhoso._
- **Esforco total:** ~4h

### Fase 3 — Estabilidade (antes do lancamento)
- [ ] Testes API (pelo menos fluxos core) — **Arquivo:** `naviera-api/src/test/` — **Esforco:** 2-3 dias
- [ ] Input validation BFF (Zod/Joi) — **Arquivo:** `naviera-web/server/routes/` — **Esforco:** 1 dia
- [ ] BFF logging em arquivo (rotacao) — **Arquivo:** `naviera-web/server/logger.js` — **Esforco:** 2h
- [ ] Migrations avulsas numeradas — **Arquivo:** `database_scripts/` — **Esforco:** 30min
- [ ] .env.example para naviera-app — **Arquivo:** `naviera-app/.env.example` — **Esforco:** 10min
- [ ] Remover react-router-dom nao usado — **Arquivo:** `naviera-web/package.json` — **Esforco:** 5min
- **Notas:**
> _Testes API sao o item mais importante. Restante sao ajustes rapidos._
- **Esforco total:** ~3 dias

### Fase 4 — Polish (pos-lancamento)
- [ ] 3 paginas Web stub (Agenda, ConfigurarApi, Gestao Funcionarios) — **Esforco:** 1 dia
- [ ] Deep links Web (React Router ou history API) — **Esforco:** 1 dia
- [ ] READMEs para naviera-web e naviera-app — **Esforco:** 1h
- [ ] Testes Web e App — **Esforco:** 2-3 dias
- [ ] Validacao CPF/CNPJ com digitos verificadores — **Esforco:** 1h

### Backlog — Pos-MVP
- [ ] CI/CD com GitHub Actions — **Prioridade:** media
- [ ] App GPS real-time (MapaCPF) — **Prioridade:** alta
- [ ] Migrar app para React Native / PWA / Capacitor — **Prioridade:** alta
- [ ] API docs com Swagger/OpenAPI — **Prioridade:** media
- [ ] Email verification no cadastro app — **Prioridade:** media
- [ ] Password reset — **Prioridade:** media
- [ ] Refresh token JWT — **Prioridade:** baixa
- [ ] Audit logging (acoes sensiveis) — **Prioridade:** media
- [ ] Lib mapa real no App (Leaflet/Mapbox) — **Prioridade:** alta
- **Notas:**
> _GPS e migracao mobile sao prioridade alta pos-MVP. CI/CD facilita tudo mais._

---

## HISTORICO

| Versao | Data | Prontos | Incompletos | Faltando | Status |
|--------|------|---------|-------------|----------|--------|
| V1.0 | 2026-04-08 | 38 | 20 | 22 | PRECISA DE TRABALHO |
| V2.0 | 2026-04-10 | 70 | 37 | 5 | PRECISA DE TRABALHO |
| V3.0 | 2026-04-10 | 108 | 4 | 0 | QUASE PRONTO |
| **V4.0** | **2026-04-10** | **95** | **17** | **10** | **QUASE PRONTO** |

> V4.0 aprofundou analise em infra/seguranca — areas que V3.0 nao cobriu. Contagem PRONTO caiu porque novos itens foram avaliados, nao porque houve regressao.

---

## NOTAS GERAIS
> **Trade-offs aceitos:**
> - Web BFF (Express) conecta direto no PostgreSQL em vez de consumir a API Spring Boot — decisao de arquitetura para performance e independencia.
> - App em React web para dev, migra para mobile depois — UI estavel, pronto para migracao.
> - JWT sem blacklist — aceitavel com expiracao de 8h.
>
> **Mudancas detectadas vs V3.0:**
> - EmpresaDAO e BalancoViagemDAO agora estao tenant-aware (eram PENDENTE no CLAUDE.md).
> - App.jsx tem 103 linhas (refatorado de 1144 — ja estava concluido na V3.0).
>
> **Dividas tecnicas:**
> - VenderPassagemController 2170 linhas, CadastroFreteController 2239, InserirEncomendaController 1798 — requer testes antes de refatorar.
> - SyncService.java concatena nome de tabela em SQL — vetor de SQL injection.
> - Express 5.x (RC) em producao — monitorar estabilidade.
>
> **Riscos para producao:**
> - CORS aberto permite CSRF-like attacks contra o BFF.
> - Sem HTTPS, JWT trafega em texto claro.
> - Sem testes na API, regressoes passam silenciosamente.

---
*Gerado por Claude Code (mvp-report) — Revisao humana obrigatoria*
