# MVP PLAN — Naviera Eco
> **Versao:** V2.0
> **Data:** 2026-04-10
> **Status:** PRECISA DE TRABALHO

---

## RESUMO

| Status | Itens |
|--------|-------|
| PRONTO | 70 |
| INCOMPLETO | 37 |
| FALTANDO | 5 |
| POS-MVP | 4 |

**Bloqueadores:** 3 itens criticos impedem MVP
**Estimativa total bloqueadores:** ~12-16 horas

### Progresso desde V1.0

| Metrica | V1.0 (04-08) | V2.0 (04-10) | Delta |
|---------|-------------|-------------|-------|
| PRONTO | 38 | 70 | +32 |
| FALTANDO | 22 | 5 | -17 |
| % Pronto | 47% | 60% | +13pp |
| Total itens | 81 | 116 | +35 |

### Readiness por Camada

| Camada | Features Core | Status Geral |
|--------|--------------|-------------|
| **Desktop (JavaFX)** | 7/7 PRONTO | Producao-ready |
| **API (Spring Boot)** | 7/7 PRONTO | Pronto (falta sync + HTTPS) |
| **Web (React + Express BFF)** | 1/7 PRONTO | Somente leitura — sem escrita |
| **App (React → mobile)** | ~3/7 PRONTO | Compra passagem OK, falta encomendas |

---

## FUNCIONALIDADES CORE

### Desktop — CRUD Completo
- **Status:** PRONTO
- **Estado atual:** 7/7 features funcionais. 143 arquivos Java, 49 telas FXML. Passagens, encomendas, fretes, viagens, financeiro (5 controllers), auth (login BCrypt + roles), 11 controllers de cadastro. Multi-tenant: 22/24 DAOs migrados.
- **O que falta:** EmpresaDAO e BalancoViagemDAO (queries complexas) ainda pendentes.
- **Observacoes:**
> _Sistema completo e operacional offline. Principal ferramenta de trabalho._

### API — CRUD Completo + Auth
- **Status:** PRONTO
- **Estado atual:** 7/7 features. 28 controllers, 108+ endpoints REST. CRUD completo para passagens, encomendas, fretes, viagens, financeiro. Auth operador + cliente (JWT + BCrypt). Cadastros com read + write. Extras: BilheteController (TOTP/QR), PerfilController (foto). CQRS pattern (Read/Write separados). TenantUtils em todos endpoints.
- **O que falta:** SyncService e stub (logica bidirecional nao implementada).
- **Observacoes:**
> _Backend mais completo do projeto. Pronto para consumo pelo web e app._

### Web — Somente Leitura
- **Status:** INCOMPLETO
- **Estado atual:** Auth (login operador) PRONTO. 6 telas de leitura: Dashboard, Passagens, Encomendas, Fretes, Financeiro + Login. Express BFF com 10 rotas, ~25 endpoints GET. Sidebar com 7 secoes, TopBar com seletor viagem, tema light/dark.
- **O que falta:** TODOS endpoints de escrita (POST/PUT/DELETE) — passagens, encomendas, fretes, viagens, financeiro, cadastros. 20 paginas placeholder. Filtro multi-tenant nos endpoints.
- **Observacoes:**
> _Console web nao permite operar — apenas visualizar. Bloqueador critico para MVP._

### App — Parcialmente Funcional
- **Status:** INCOMPLETO
- **Estado atual:** Auth PRONTO (login CPF/CNPJ + cadastro + JWT + perfil com foto). 11 telas, 2 perfis (CPF: home/amigos/mapa/passagens/bilhete/perfil | CNPJ: painel/pedidos/parceiros/financeiro/loja). Design system V4 completo (light/dark). Compra passagem chama API.
- **O que falta:** Tela de rastreio de encomendas. GPS real no MapaCPF. Separar App.jsx monolitico (1144 linhas).
- **Observacoes:**
> _Desenvolvido em React web para iteracao rapida — destino final e mobile. Escopo correto para cliente final._

---

## FLUXOS CRITICOS

### Fluxo: Onboarding / Primeiro Uso
- **Status:** PRONTO (Desktop, Web, API) | INCOMPLETO (App)
- **Etapas:**
  - [x] Desktop: Login → Combo usuarios → BCrypt → Selecionar Viagem
  - [x] Web: Login → JWT → Dashboard → Selecionar viagem (TopBar)
  - [x] App: Cadastro CPF/CNPJ → Login → Home
  - [x] API: POST /auth/login + /auth/registrar → JWT
- **Gaps:** App falta email verification e password reset.
- **Observacoes:**
> _Fluxo funcional em todas as camadas. Gaps do app sao aceitaveis para MVP._

### Fluxo: Operacao Principal (Desktop)
- **Status:** PRONTO
- **Etapas:**
  - [x] Login → Selecionar Viagem
  - [x] Vender Passagem (VenderPassagemController, 400+ linhas)
  - [x] Imprimir Bilhete (PrinterJob AWT)
  - [x] Registrar Encomenda/Frete
  - [x] Fechar Balanco (BalancoViagemController)
- **Gaps:** Nenhum. Funciona 100% offline.
- **Observacoes:**
> _Fluxo completo e testado em producao._

### Fluxo: Visualizacao Web
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] Login → JWT → Dashboard
  - [x] Selecionar viagem ativa
  - [x] Ver passagens/encomendas/fretes/financeiro (tabelas)
  - [ ] Criar passagem/encomenda/frete — **FALTANDO**
  - [ ] Editar/Deletar registros — **FALTANDO**
  - [ ] 20 paginas placeholder — **FALTANDO**
- **Gaps:** Web e somente leitura. Nao permite operacoes.
- **Observacoes:**
> _Bloqueador principal. Sem escrita, web serve apenas como dashboard de consulta._

### Fluxo: Compra pelo App
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] Login/Cadastro CPF/CNPJ
  - [x] Ver viagens disponiveis (HomeCPF)
  - [x] Comprar passagem (confirmarCompra → POST /bilhetes/comprar)
  - [x] Ver bilhete digital (BilheteScreen)
  - [ ] Rastrear encomenda — **FALTANDO**
  - [ ] GPS em tempo real — **FALTANDO**
- **Gaps:** Encomendas e GPS nao implementados.
- **Observacoes:**
> _Fluxo de compra de passagem funciona. Encomendas seria util mas nao bloqueia MVP._

### Fluxo: Logout / Saida
- **Status:** PRONTO
- **Etapas:**
  - [x] Desktop: Confirma → Fecha janelas → salva estado
  - [x] Web: localStorage.clear() → redirect Login
  - [x] App: localStorage.clear() → tela login
- **Gaps:** JWT stateless sem blacklist (aceitavel).
- **Observacoes:**
> _Funcional em todas as camadas._

### Fluxo: Tratamento de Erros
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] API: GlobalExceptionHandler + ApiException (nao expoe stack traces)
  - [ ] Desktop: AlertHelper funciona mas muitos e.printStackTrace()
  - [ ] Web: setErro() basico, sem retry, sem toast estruturado
  - [ ] App: ErrorRetry component, sem retry automatico
- **Gaps:** Erro handling inconsistente entre camadas.
- **Observacoes:**
> _API e a melhor. Desktop/Web/App precisam padronizar mensagens._

---

## INFRAESTRUTURA

| Item | Status | Detalhe |
|------|--------|---------|
| Dockerfile API | PRONTO | Multi-stage Maven → JRE alpine |
| Dockerfile App | PRONTO | Multi-stage Node → Nginx |
| docker-compose.yml | PRONTO | 2 services, healthcheck, restart, depends_on |
| Deploy scripts | FALTANDO | Sem script .sh ou Makefile |
| CI/CD | POS-MVP | Sem GitHub Actions |
| .env.example (root + api) | PRONTO | Variaveis documentadas |
| .env (web) | INCOMPLETO | Hardcoded localhost — criar .env.example |
| db.properties | INCOMPLETO | Tracked em git com placeholder |
| DB Migrations | PRONTO | 15+ SQL numerados, multi-tenant |
| Setup unico | INCOMPLETO | Docker funciona mas PostgreSQL fora do compose |
| README.md | PRONTO | Requisitos, setup, deploy |
| Logging | INCOMPLETO | Sem logback, sem Winston, apenas console |
| Healthchecks | PRONTO | /actuator/health (API) + /health (BFF) |

- **Observacoes:**
> _Docker e migrations solidos. Logging e o gap mais importante para operacao._

---

## SEGURANCA MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| JWT (API + BFF) | PRONTO | JJWT 0.12.6 + jsonwebtoken, HMAC-SHA256, BCrypt |
| Spring Security | PRONTO | Stateless, CSRF off, CORS, multi-role |
| Secrets via env | PRONTO | JWT_SECRET, DB_PASSWORD em .env |
| SQL injection | PRONTO | Queries parametrizadas em todas as camadas |
| XSS | PRONTO | API JSON-only, Nginx SPA |
| BFF secret fallback | INCOMPLETO | Fallback hardcoded — deveria dar erro |
| db.properties em git | INCOMPLETO | Mover para .gitignore |
| Input validation | INCOMPLETO | Faltam @NotBlank/@Valid nos DTOs |
| HTTPS | FALTANDO | Nenhuma config SSL/TLS — **BLOQUEADOR** |
| Rate limiting | FALTANDO | Sem protecao brute force |

- **Observacoes:**
> _Auth e protecao SQL sao solidos. HTTPS e o unico bloqueador de seguranca._

---

## ESTABILIDADE

| Item | Status | Detalhe |
|------|--------|---------|
| Error handling API | PRONTO | GlobalExceptionHandler + ApiException |
| Reconexao BD (3 camadas) | PRONTO | Pool + timeout + reciclagem em Desktop/API/BFF |
| Transacoes Desktop | PRONTO | setAutoCommit(false) + commit/rollback |
| Transacoes API | PRONTO | @Transactional em 15+ services |
| Error handling Desktop | INCOMPLETO | Muitos e.printStackTrace() |
| Error handling BFF | INCOMPLETO | Sem retry, sem validacao |
| Graceful shutdown | INCOMPLETO | Desktop/Express sem hooks |
| Timeouts HTTP | INCOMPLETO | Fetch e Spring sem timeout explicito |
| Transacoes BFF | INCOMPLETO | Queries independentes |
| Query timeout | FALTANDO | Nao configurado em nenhuma camada |

- **Observacoes:**
> _Reconexao BD e transacoes sao pontos fortes. Timeouts e shutdown sao os gaps._

---

## UX MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| Loading states | PRONTO | Todas as paginas em Web/App/Desktop |
| Mensagens erro (API/Web/App) | PRONTO | Amigaveis, setErro(), ErrorRetry |
| Navegacao (3 camadas) | PRONTO | Sidebar/TabBar/Menu logicos |
| Responsividade App | PRONTO | Mobile-first, max-width 420px |
| Toast/Feedback App | PRONTO | Auto-close 3s |
| Mensagens erro Desktop | INCOMPLETO | e.printStackTrace() internos |
| Responsividade Web | INCOMPLETO | Sem @media queries — quebra em <800px |
| Feedback sucesso Web | INCOMPLETO | Sem escrita = sem feedback |

- **Observacoes:**
> _UX e a dimensao mais forte. Responsividade web e unico gap significativo._

---

## DEPENDENCIAS

| Servico/API | Status | Configurado | Fallback |
|------------|--------|------------|----------|
| PostgreSQL | PRONTO | Pool em todas as camadas | Erro 500 (sem cache) |
| Firebase (push) | PRONTO | FirebaseConfig.java | Desativa se sem credenciais |
| pom.xml versions | PRONTO | Todas fixas (3.3.5, 0.12.6, 9.3.0) | — |
| package.json | INCOMPLETO | Ranges (^19, ^5) | Lockfile mitiga |
| Fallback BD | INCOMPLETO | — | Sem retry automatico |

- **Observacoes:**
> _Dependencias minimas e bem gerenciadas. Firebase com fallback correto._

---

## PLANO DE ACAO POR FASES

### Fase 1 — Bloqueadores (AGORA)
- [ ] **HTTPS**: Configurar SSL no Nginx ou reverse proxy — **Arquivo:** `naviera-app/nginx.conf` + docker-compose — **Esforco:** ~2h
- [ ] **Web BFF multi-tenant**: Adicionar WHERE empresa_id = ? em todos os endpoints Express — **Arquivo:** `naviera-web/server/routes/*.js` — **Esforco:** ~2h
- [ ] **BFF secret fallback**: Remover fallback, exigir JWT_SECRET — **Arquivo:** `naviera-web/server/middleware/auth.js` — **Esforco:** ~15min
- [ ] **db.properties**: git rm --cached + .gitignore — **Esforco:** ~15min
- **Notas:**
> _HTTPS e multi-tenant sao os unicos bloqueadores reais. Os demais sao fixes rapidos._
- **Esforco total:** ~4.5h

### Fase 2 — Incompletos Criticos (esta semana)
- [ ] **Web BFF write endpoints — Passagens**: POST /passagens (criar), PUT /passagens/:id, DELETE /passagens/:id — **Arquivo:** `naviera-web/server/routes/passagens.js` — **Esforco:** ~2h
- [ ] **Web BFF write endpoints — Encomendas**: POST, PUT, DELETE + entregar — **Arquivo:** `naviera-web/server/routes/encomendas.js` — **Esforco:** ~2h
- [ ] **Web BFF write endpoints — Fretes**: POST, PUT, DELETE + pagar — **Arquivo:** `naviera-web/server/routes/fretes.js` — **Esforco:** ~2h
- [ ] **Web BFF write endpoints — Viagens**: POST, PUT, DELETE, ativar/desativar — **Arquivo:** `naviera-web/server/routes/viagens.js` — **Esforco:** ~1.5h
- [ ] **Web BFF write endpoints — Financeiro**: POST saida, DELETE saida — **Arquivo:** `naviera-web/server/routes/financeiro.js` — **Esforco:** ~1h
- [ ] **Web BFF write endpoints — Cadastros**: POST/PUT/DELETE para usuarios, conferentes, caixas, tarifas, rotas, embarcacoes — **Arquivo:** `naviera-web/server/routes/cadastros.js` — **Esforco:** ~3h
- [ ] **Web frontend — Forms de escrita**: Adicionar formularios de criacao/edicao nas 6 paginas funcionais — **Arquivo:** `naviera-web/src/pages/*.jsx` — **Esforco:** ~6h
- [ ] **Input validation API**: Adicionar @NotBlank @NotNull @Valid nos DTOs — **Arquivo:** `naviera-api/src/.../dto/*.java` — **Esforco:** ~2h
- **Notas:**
> _Web BFF write e o maior esforco. Frontend precisa de forms alem das tabelas._
- **Esforco total:** ~19.5h

### Fase 3 — Estabilidade (antes do lancamento)
- [ ] **Logging API**: Criar logback-spring.xml com niveis INFO/WARN/ERROR — **Arquivo:** `naviera-api/src/main/resources/logback-spring.xml` — **Esforco:** ~1h
- [ ] **Logging BFF**: Substituir console.error por winston ou pino — **Arquivo:** `naviera-web/server/*.js` — **Esforco:** ~1h
- [ ] **Rate limiting login**: Bucket4j na API + express-rate-limit no BFF — **Arquivo:** `naviera-api/pom.xml` + `naviera-web/server/routes/auth.js` — **Esforco:** ~2h
- [ ] **Query timeout**: Configurar em HikariCP + pg pool — **Arquivo:** `naviera-api/application.properties` + `naviera-web/server/db.js` — **Esforco:** ~30min
- [ ] **Graceful shutdown Desktop**: Adicionar Runtime.addShutdownHook em ConexaoBD — **Arquivo:** `src/dao/ConexaoBD.java` — **Esforco:** ~30min
- [ ] **Responsividade Web**: Adicionar @media queries para <800px — **Arquivo:** `naviera-web/src/styles/global.css` — **Esforco:** ~2h
- [ ] **Web .env.example**: Renomear .env → .env.example — **Arquivo:** `naviera-web/.env` — **Esforco:** ~15min
- **Notas:**
> _Logging e rate limiting sao os mais importantes para operacao em producao._
- **Esforco total:** ~7h

### Fase 4 — Polish (pos-lancamento)
- [ ] **Web 20 paginas placeholder**: Implementar telas de cadastros, relatorios, estornos, agenda — **Esforco:** ~20h
- [ ] **App refatorar monolito**: Separar App.jsx em modulos/componentes — **Esforco:** ~4h
- [ ] **App tela encomendas**: Rastreio de encomendas para perfil CPF — **Esforco:** ~3h
- [ ] **API Sync bidirecional**: Implementar logica no SyncService — **Esforco:** ~8h
- [ ] **Desktop cleanup e.printStackTrace**: Substituir por logging estruturado — **Esforco:** ~3h
- [ ] **PostgreSQL no compose**: Adicionar servico DB ao docker-compose.yml — **Esforco:** ~1h

### Backlog — Pos-MVP
- [ ] CI/CD com GitHub Actions — **Prioridade:** media
- [ ] API docs com Swagger/OpenAPI — **Prioridade:** media
- [ ] Rollback migrations — **Prioridade:** baixa
- [ ] Email verification no cadastro app — **Prioridade:** media
- [ ] Password reset — **Prioridade:** media
- [ ] Refresh token JWT — **Prioridade:** baixa
- [ ] App GPS real-time (MapaCPF) — **Prioridade:** alta
- [ ] Push notifications (Firebase) — **Prioridade:** media
- [ ] Migrar app para React Native / PWA / Capacitor — **Prioridade:** alta
- [ ] EmpresaDAO + BalancoViagemDAO multi-tenant — **Prioridade:** alta
- [ ] Audit logging (acoes sensiveis) — **Prioridade:** media
- [ ] Exponential backoff em retries — **Prioridade:** baixa
- [ ] Transacoes no BFF Express — **Prioridade:** media
- **Notas:**
> _GPS e migracao mobile sao prioridade alta pos-MVP. Sync e fundamental para operacao multi-barco._

---

## HISTORICO

| Versao | Data | Prontos | Incompletos | Faltando | Status |
|--------|------|---------|-------------|----------|--------|
| V1.0 | 2026-04-08 | 38 | 20 | 22 | PRECISA DE TRABALHO |
| **V2.0** | **2026-04-10** | **70** | **37** | **5** | **PRECISA DE TRABALHO** |

---

## NOTAS GERAIS
> **Trade-offs aceitos:**
> - Web BFF (Express) conecta direto no PostgreSQL em vez de consumir a API Spring Boot — decisao de arquitetura para performance e independencia. Significa manter 2 backends com queries similares.
> - App em React web para dev, migra para mobile depois — aceitavel se UI estabilizar antes da migracao.
> - JWT sem blacklist — aceitavel com expiracao de 8-24h para MVP.
> - Rate limiting pos-MVP — aceitavel se deploy for privado/interno inicialmente.
>
> **Dividas tecnicas:**
> - VenderPassagemController tem 2170 linhas, CadastroFreteController 2239, InserirEncomendaController 1798 — refatoracao requer testes primeiro.
> - App.jsx monolitico (1144 linhas) — separar antes de migrar para mobile.
> - Logging inexistente em producao — adicionar antes de escalar.
>
> **Riscos:**
> - Sync Desktop ↔ API e stub — operacao multi-barco depende disso.
> - Web sem escrita torna o console inutil para operadores — prioridade maxima.
> - Sem HTTPS, JWT trafega em plain text — nao pode ir para producao assim.

---
*Gerado por Claude Code (mvp-report) — Revisao humana obrigatoria*
