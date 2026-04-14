# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Naviera Eco** — Plataforma SaaS multi-tenant de gestao de transporte fluvial (passageiros, fretes, encomendas e financeiro). Evolucao do SistemaEmbarcacaoProjeto_Novo para suportar multiplas empresas.

| Camada | Tecnologia | Funcao | Status |
|--------|-----------|--------|--------|
| **Desktop** | JavaFX 23 + Java 17 | Console operacional offline (barco) | FUNCIONAL — migrando para multi-tenant |
| **Web** | React + Vite + Express (BFF) | Espelho do Desktop online (escritorio) | PARCIAL — 6 telas funcionais, 20 placeholder, somente leitura |
| **API** | Spring Boot 3.3 + PostgreSQL | Backend REST multi-tenant | PARCIAL — precisa tenant-awareness |
| **App Mobile** | React + Vite (dev) → mobile futuro | App para clientes finais (CPF/CNPJ) | PARCIAL — 11 telas (2 perfis CPF/CNPJ), UI em web para dev |
| **Site** | React + Vite (estatico) | Site institucional (naviera.com.br) | FUNCIONAL — 7 paginas, deploy estatico |

---

## Arquitetura Multi-Tenant

### Modelo de operacao

**Principio fundamental: o Desktop SEMPRE opera com banco local e NUNCA depende de internet para funcionar.** O barco fica online a maior parte do tempo, mas a internet pode cair a qualquer momento. Quando isso acontece, o operador continua lancando passagens, fretes e encomendas normalmente no banco local. Quando a internet volta, o SyncClient sincroniza imediatamente com o banco central da VPS. O offline nao e o modo principal — e a rede de seguranca que garante operacao ininterrupta.

```
VPS (72.62.166.247)
┌──────────────────────────────────────────────────────────┐
│  Docker: naviera_postgres (porta 5435)                   │
│  Banco CENTRAL: naviera_eco                              │
│  User: naviera / Nav13r4DB@2026                          │
│                                                          │
│  naviera-api (Spring Boot, porta 8081, systemd)          │
│  naviera-web (Express BFF, porta 3003, PM2)              │
│  naviera-app (build estatico, Nginx)                     │
│  naviera-site (build estatico, Nginx)                    │
│  Todos acessam o MESMO banco central                     │
└──────────────────────┬───────────────────────────────────┘
                       │ sync bidirecional (quando online)
┌──────────────────────┴───────────────────────────────────┐
│  BARCO (Desktop JavaFX)                                  │
│  PostgreSQL LOCAL (porta 5437 dev / variavel prod)       │
│  Banco LOCAL: naviera_eco                                │
│  Opera OFFLINE — lanca passagens, fretes, encomendas     │
│  SyncClient sincroniza com API quando tem internet       │
└──────────────────────────────────────────────────────────┘
```

- **Desktop** = JavaFX + PostgreSQL LOCAL = opera offline, sync quando online
- **Web** = React + Express BFF = acessa banco CENTRAL da VPS (escritorio com internet estavel)
- **App Mobile** = React → API Spring Boot = acessa banco CENTRAL da VPS
- **Sync** = SyncClient no Desktop: bidirecional, automatico quando online, via API REST

### Estrategia de isolamento

- Coluna `empresa_id` em TODAS as tabelas de negocio (exceto aux_*)
- `TenantContext` (ThreadLocal) armazena empresa_id da thread atual
- Desktop: empresa_id fixo, lido de `db.properties` (chave `empresa.id`)
- API REST: empresa_id extraido do JWT a cada request
- Web BFF: empresa_id extraido do JWT (login valida pelo slug do subdominio)
- Tabelas auxiliares (aux_*) sao compartilhadas entre empresas

### Estrategia de subdominios (producao)

**Dominio:** `naviera.com.br`

| Subdominio | Funcao | Quem usa |
|-----------|--------|----------|
| `naviera.com.br` | Site institucional | Publico geral (protegido por auth_basic temporariamente) |
| `{slug}.naviera.com.br` | Console web da empresa | Operadores (ex: `saofrancisco.naviera.com.br`) |
| `app.naviera.com.br` | App mobile (clientes) | Passageiros e lojas |
| `admin.naviera.com.br` | Painel admin Naviera | Gestor da plataforma |
| `api.naviera.com.br` | API REST | Todos os frontends |

**Fluxo:**
1. Operador acessa `{slug}.naviera.com.br`
2. Nginx extrai subdominio → header `X-Tenant-Slug`
3. BFF middleware resolve slug → `empresa_id` (tabela `empresas.slug`)
4. Login valida usuario filtrando por `empresa_id`
5. JWT gerado com `empresa_id` embutido
6. Todas as queries filtram por `empresa_id` do JWT

**Wildcard DNS:** `*.naviera.com.br` → IP do servidor
**Wildcard SSL:** Let's Encrypt com certbot `--domains *.naviera.com.br`

### Arquivos-chave do multi-tenant

| Arquivo | Funcao |
|---------|--------|
| `database_scripts/013_multi_tenant.sql` | Migration: cria tabela `empresas`, adiciona `empresa_id` em todas as tabelas |
| `database_scripts/014_tenant_slug.sql` | Migration: adiciona `slug` unico na tabela `empresas` |
| `src/dao/TenantContext.java` | ThreadLocal com empresa_id — usado por todos os DAOs |
| `src/dao/DAOUtils.java` | Helpers: `empresaId()`, `setEmpresa()`, `TENANT_FILTER` |
| `naviera-web/server/middleware/tenant.js` | Middleware BFF: resolve slug → empresa_id |
| `nginx/naviera.conf` | Nginx: site institucional + wildcard subdominio + proxy (deploy como `naviera.com.br.conf`) |
| `db.properties.example` | Config com `empresa.id=1` |

---

## Camada Desktop (JavaFX)

**Eclipse IDE project** (no Maven/Gradle). JDK 17 required.

- **JavaFX SDK**: 23.0.2
- **Dependencies**: JARs em `lib/`
- **Entry point**: `gui.Launch` → `gui.LoginApp.main()`
- **Dev entry**: `gui.LaunchDireto` (bypasses login)
- **Database**: `src/dao/ConexaoBD.java` + `db.properties`
- **Tests**: JUnit 4 em `src/tests/`
- **Pattern**: DAO + MVC (controllers chamam DAOs diretamente)

```
src/
├── dao/          # Data Access Objects + ConexaoBD + TenantContext
├── gui/          # JavaFX controllers + FXML views
│   └── util/     # UI helpers (AlertHelper, PermissaoService, SyncClient)
├── model/        # POJOs/entities (~25 classes)
└── tests/        # JUnit 4 tests
```

**Key flow**: FXML → Controller (`gui/`) → DAO (`dao/`) → PostgreSQL via `ConexaoBD.getConnection()`

---

## Camada Web (React + Express BFF)

Diretorio: `naviera-web/`

Espelho online do Desktop para uso no escritorio. Arquitetura com BFF (Backend For Frontend) proprio.

### Arquitetura

```
Browser (React, porta 5174) ──proxy /api──▶ Express BFF (porta 3002) ──SQL──▶ PostgreSQL
```

O Vite faz proxy de `/api` para o Express (configurado em `vite.config.js`). O React nao chama a API Spring Boot diretamente — o Express BFF e o intermediario.

### Frontend React — 14 arquivos

- **Framework**: React 18 + Vite 5
- **Auth**: AuthContext + JWT em localStorage
- **Tema**: light/dark via `data-theme` attribute
- **Layout**: Sidebar (7 secoes) + TopBar (seletor viagem ativa)

| Pagina | Arquivo | Status |
|--------|---------|--------|
| Login | `pages/Login.jsx` | Funcional — login operador |
| Dashboard | `pages/Dashboard.jsx` | Funcional — cards resumo por viagem |
| Passagens | `pages/Passagens.jsx` | Funcional — tabela listagem |
| Encomendas | `pages/Encomendas.jsx` | Funcional — tabela listagem |
| Fretes | `pages/Fretes.jsx` | Funcional — tabela listagem |
| Financeiro | `pages/Financeiro.jsx` | Funcional — balanco receitas/despesas |
| **20 paginas** | `pages/Placeholder.jsx` | "Em Construcao" (cadastros, relatorios, estornos, etc.) |

### Backend Express (BFF) — 10 rotas, ~25 endpoints

| Rota | Endpoints | Funcao |
|------|-----------|--------|
| `/api/auth` | POST `/login`, GET `/me` | Login nome/email + bcrypt, JWT 8h |
| `/api/viagens` | GET `/`, `/ativa`, `/:id` | Listar, buscar ativa, detalhe |
| `/api/passagens` | GET `/`, `/resumo` | Listar por viagem, totais |
| `/api/encomendas` | GET `/`, `/resumo` | Listar por viagem, totais |
| `/api/fretes` | GET `/`, `/resumo` | Listar por viagem, totais |
| `/api/financeiro` | GET `/entradas`, `/saidas`, `/balanco` | Receitas, despesas, saldo |
| `/api/dashboard` | GET `/resumo` | Cards resumo (passagens+encomendas+fretes) |
| `/api/cadastros` | GET x8 | Usuarios, conferentes, caixas, tarifas, tipos, empresa, clientes, itens |
| `/api/rotas` | GET `/` | Listar rotas |
| `/api/embarcacoes` | GET `/` | Listar embarcacoes |

**Estado**: Todos endpoints sao somente leitura (GET). Falta: operacoes de escrita (criar/editar passagem, encomenda, frete, etc.)

### Comandos

```bash
cd naviera-web && npm run dev          # Frontend (porta 5174)
cd naviera-web/server && node index.js # BFF Express (porta 3002)
```

---

## Camada App Mobile

Diretorio: `naviera-app/`

App para clientes finais (CPF pessoa fisica, CNPJ loja parceira). **Desenvolvido em React + Vite (web) para agilizar iteracao da UI — destino final e mobile.**

### Stack atual (dev)

- **Framework**: React 19 + Vite (web, nao React Native ainda)
- **Estrutura**: Monolitico em `App.jsx` (1144 linhas) + `icons.jsx` + `App.css`
- **API**: `VITE_API_URL` → Spring Boot API (`localhost:8080/api`)
- **Auth**: JWT via localStorage, login por CPF/CNPJ + senha

### Telas implementadas

**Perfil CPF (pessoa fisica) — 4 tabs + 2 telas:**

| Tela | Componente | Funcao |
|------|-----------|--------|
| Home | `HomeCPF` | Dashboard do passageiro |
| Amigos | `AmigosCPF` | Gerenciar lista de amigos |
| Mapa | `MapaCPF` | Rastreio de barcos (GPS) |
| Passagens | `PassagensCPF` | Listar/comprar passagens |
| Bilhete | `BilheteScreen` | Visualizar bilhete digital |
| Perfil | `PerfilScreen` | Editar dados, foto, senha |

**Perfil CNPJ (loja parceira) — 5 tabs:**

| Tela | Componente | Funcao |
|------|-----------|--------|
| Painel | `HomeCNPJ` | Dashboard da loja |
| Pedidos | `PedidosCNPJ` | Gerenciar pedidos |
| Parceiros | `LojasParceiras` | Lojas parceiras |
| Financeiro | `FinanceiroCNPJ` | Extrato financeiro |
| Loja | `LojaCNPJ` | Config da loja |

### Infraestrutura pronta

- Design system Naviera V4 completo (light/dark)
- Componentes base: Badge, Skeleton, ErrorRetry, Toast, Avatar, Card
- Hook `useApi` com auth headers e refresh
- Mascaras CPF/CNPJ, formatadores data/dinheiro
- Header com navegacao back, TabBar, Logo SVG
- Cadastro completo (`TelaCadastro`)

### Comandos

```bash
cd naviera-app && npm run dev  # Dev web (sera mobile no futuro)
```

---

## Camada Site Institucional

Diretorio: `naviera-site/`

Site institucional da Naviera em `naviera.com.br`. React SPA com build estatico servido pelo Nginx.

### Stack

- **Framework**: React 18 + Vite 5
- **Estrutura**: Monolitico em `App.jsx` (~600 linhas) — 7 paginas + componentes compartilhados
- **Estilos**: CSS inline via template string (sem arquivo CSS separado)
- **Fontes**: Sora (titulos) + Space Mono (monospace)
- **Deploy**: `npm run build` → `dist/` copiado para `/var/www/naviera-site/` na VPS

### Paginas

| Pagina | Componente | Conteudo |
|--------|-----------|----------|
| Home | `HomePage` | Hero + features + arquitetura + dual B2B/B2C + CTA |
| Empresas | `EmpresasPage` | Landing para operadores + 3 passos |
| Passageiros | `PassageirosPage` | Landing para passageiros + lojas CNPJ |
| Funcionalidades | `FuncionalidadesPage` | Grid operacional + financeiro + tech |
| Precos | `PrecosPage` | 3 planos (Gratis / R$299 / Consulta) + FAQ |
| Download | `DownloadPage` | Desktop (.deb/.msi) + App (PWA/stores) |
| Contato | `ContatoPage` | Canais + WhatsApp CTA |

### Downloads hospedados

- `naviera.com.br/downloads/naviera-desktop.deb` — Instalador Linux (97 MB)
- `naviera.com.br/downloads/naviera-desktop.msi` — Futuro instalador Windows
- Arquivos em `/var/www/naviera-site/downloads/` na VPS

### Protecao temporaria

Site protegido por `auth_basic` no Nginx (htpasswd em `/etc/nginx/.htpasswd_naviera`). Remover as 3 linhas de `auth_basic` no bloco `naviera.com.br` quando for publicar.

### Comandos

```bash
cd naviera-site && npm run dev    # Dev local (porta 5175)
cd naviera-site && npm run build  # Build producao → dist/
```

---

## Camada API (Spring Boot)

Diretorio: `naviera-api/`

- **Framework**: Spring Boot 3.3.5, Java 17, Maven
- **Auth**: JWT + Spring Security + BCrypt
- **DB**: Spring Data JPA + HikariCP
- **Context path**: `/api`
- **Usada por**: App mobile (direto), Desktop (via SyncClient)

---

## MIGRACAO MULTI-TENANT — STATUS E PLANO

### Fase 0: Preparacao (CONCLUIDA)

| Item | Status | Detalhe |
|------|--------|---------|
| Migration SQL 013 | FEITO | `013_multi_tenant.sql` — tabela `empresas`, `empresa_id` em 24+ tabelas, indices |
| TenantContext | FEITO | `dao/TenantContext.java` — ThreadLocal + default via db.properties |
| DAOUtils helpers | FEITO | `empresaId()`, `setEmpresa()`, `TENANT_FILTER` |
| ConexaoBD atualizado | FEITO | Le `empresa.id` do db.properties e inicializa TenantContext |
| DAOs tenant-aware | FEITO | 24/24 DAOs migrados — todos filtram por empresa_id |
| Controllers GUI | FEITO | 22 controllers com SQL inline migrados para empresa_id |

### DAOs — Status Final

| DAO | Status |
|-----|--------|
| ViagemDAO | FEITO |
| PassagemDAO | FEITO |
| EncomendaDAO | FEITO |
| FreteDAO | FEITO |
| DespesaDAO | FEITO |
| FuncionarioDAO | FEITO |
| RotaDAO | FEITO |
| EmbarcacaoDAO | FEITO |
| TarifaDAO | FEITO |
| CaixaDAO | FEITO |
| ConferenteDAO | FEITO |
| ClienteEncomendaDAO | FEITO |
| UsuarioDAO | FEITO |
| AgendaDAO | FEITO |
| ReciboAvulsoDAO | FEITO |
| ReciboQuitacaoPassageiroDAO | FEITO |
| TipoPassageiroDAO | FEITO |
| ItemFreteDAO | FEITO |
| ItemEncomendaPadraoDAO | FEITO |
| PassageiroDAO | FEITO |
| EncomendaItemDAO | N/A (tabela filha via FK) |
| EmpresaDAO | FEITO |
| BalancoViagemDAO | FEITO |

### Controllers GUI — SQL Inline Migrado

22 controllers com SQL inline agora filtram por empresa_id:
BalancoViagemController, FinanceiroPassagensController, FinanceiroEncomendasController,
FinanceiroFretesController, FinanceiroEntradaController, FinanceiroSaidaController,
VenderPassagemController, CadastroFreteController, CadastroBoletoController,
TabelaPrecoFreteController, TelaPrincipalController, AuditoriaExclusoesSaida,
RelatorioEncomendaGeralController, EstornoPagamentoController, GerarReciboAvulsoController,
RelatorioFretesController, ExtratoClienteEncomendaController, BaixaPagamentoController,
QuitarDividaEncomendaTotalController, HistoricoEstornosController,
HistoricoEstornosPassagensController, ConfigurarSincronizacaoController,
CompanyDataLoader

**Padrao usado:** `dao.DAOUtils.empresaId()` em todo SQL inline.

**Tabelas que NAO recebem empresa_id (dados globais/compartilhados):**
aux_*, contatos, frete_itens, encomenda_itens, log_estornos_*, clientes_app

### Fase 1: Banco multi-tenant — PENDENTE

- [ ] Executar migration 013 no banco de producao
- [x] Completar todos os DAOs da lista acima (24/24 feitos)
- [x] EmpresaDAO e BalancoViagemDAO ja filtram por empresa_id corretamente
- [ ] Testar com empresa_id = 1 (deve funcionar identico ao sistema atual)

### Fase 2: API tenant-aware + sync — CONCLUIDA

- [x] API: JWT com empresa_id, filtro em todos endpoints (TenantUtils)
- [x] SyncService reescrito (11 tabelas, last-write-wins, ON CONFLICT, sanitizacao)
- [x] SyncClient Desktop reescrito (auth JWT, upload/download, retry com backoff, mark synced)
- [x] WebSocket STOMP/SockJS (`/ws`) com NotificationService tenant-aware (`/topic/empresa/{id}/notifications`)
- [x] Endpoints mobile: viagens publicas cross-tenant, rastreio encomenda, GPS embarcacoes
- [x] Migration 015: tabela gps_posicoes para rastreamento
- [x] Multi-tenant por subdominio: tenant middleware, slug na tabela empresas, Nginx wildcard

### Fase 3: Desktop auto-update + Web — CONCLUIDA

- [x] Sistema de versao: VersaoController (API) + VersaoChecker.java (Desktop) — check no startup, dialog com changelog
- [x] Instalador nativo: scripts build.sh (Linux .deb) + build.bat (Windows .msi/.exe) via jpackage com JRE embutido
- [x] naviera-web: 29+ paginas funcionais (CRUD completo), Express BFF com ~50 endpoints
- [x] naviera-web: multi-tenant por subdominio (tenant middleware + login por empresa)
- [x] naviera-web: responsivo (3 breakpoints), logging, rate limiting, query timeout
- [x] Painel admin Naviera: AdminEmpresas (CRUD + stats + ativar) + AdminMetricas (dashboard plataforma)

### Fase 4: App mobile + GPS — CONCLUIDA

- [x] UI do app: 15+ telas, 2 perfis CPF/CNPJ, refatorado em 27+ modulos
- [x] Cadastro CPF/CNPJ + login + auth JWT
- [x] Design system Naviera V4 (light/dark)
- [x] Tela EncomendaCPF (rastreio com busca)
- [x] API GPS: POST /gps/posicao (tripulacao) + GET /gps/embarcacoes (publico)
- [x] PWA: manifest.json, service worker (cache-first + offline), usePWA hook, install banner
- [x] MapaCPF integrado com GPS real: mapa SVG rio Amazonas, barcos coloridos por freshness, auto-refresh 30s
- [x] Push notifications: Firebase FCM com graceful degradation, useNotifications hook, NotificationBanner
- [x] WebSocket STOMP/SockJS no app: useWebSocket hook, NotificationList com badge, reconexao automatica

### Fase 5: Site institucional + Instalador — CONCLUIDA

- [x] Site institucional React SPA (`naviera-site/`) com 7 paginas
- [x] Deploy estatico em `naviera.com.br` (antes redirecionava para app)
- [x] Nginx atualizado: bloco `naviera.com.br` serve site, bloco deve vir ANTES do wildcard `*.naviera.com.br`
- [x] Downloads hospedados: `/downloads/naviera-desktop.deb` (97 MB) servido com Content-Disposition attachment
- [x] Protecao temporaria com auth_basic (htpasswd)
- [x] Instalador Desktop Linux (.deb) via jpackage com JRE embutido

---

## Comandos uteis

```bash
# Executar migration multi-tenant
psql -U postgres -d naviera_eco -f database_scripts/013_multi_tenant.sql

# Verificar se empresa_id foi adicionado
psql -U postgres -d naviera_eco -c "\d passagens" | grep empresa_id

# API (Spring Boot — porta 8080)
cd naviera-api && mvn spring-boot:run

# Web — Frontend (porta 5174, proxy /api → 3002)
cd naviera-web && npm run dev

# Web — BFF Express (porta 3002)
cd naviera-web/server && node index.js

# App Mobile dev (web, porta padrao Vite)
cd naviera-app && npm run dev

# Site institucional dev (porta 5175)
cd naviera-site && npm run dev

# Site institucional — build + deploy
cd naviera-site && npm run build
scp -r dist/* root@72.62.166.247:/var/www/naviera-site/
```

---

## Camada OCR (naviera-ocr)

Diretorio: `naviera-ocr/`

App PWA standalone para operadores lancarem fretes por foto (nota fiscal, cupom, caderno). Deploy em `ocr.naviera.com.br`.

### Stack

- **Frontend**: React 18 + Vite (PWA standalone, porta 5175 dev)
- **Backend**: Rotas OCR no BFF existente (`naviera-web/server/routes/ocr.js`)
- **OCR**: Google Cloud Vision API (`DOCUMENT_TEXT_DETECTION`)
- **IA**: Google Gemini (revisao inteligente dos itens extraidos)
- **Offline**: IndexedDB para fila de fotos quando sem internet
- **Fotos**: Filesystem da VPS (`uploads/ocr/{empresa_id}/`)

### Fluxo

```
Operador tira foto → Google Vision extrai texto → Parser regex extrai itens
→ Operador revisa (pode clicar "Revisar com IA" para Gemini reprocessar)
→ Operador confirma → Conferente aprova no naviera-web → Frete criado
```

### APIs externas e modelos

**IMPORTANTE: Sempre ler esta secao antes de alterar modelos ou keys de API.**

| Servico | Modelo | Variavel .env | Uso |
|---------|--------|---------------|-----|
| Google Cloud Vision | DOCUMENT_TEXT_DETECTION | `GOOGLE_CLOUD_VISION_API_KEY` | OCR de imagens (extrair texto) |
| Google Gemini | `gemini-3-flash-preview` | `GEMINI_API_KEY` | Revisao inteligente dos itens OCR |

- **NUNCA** trocar o modelo do Gemini sem consultar https://ai.google.dev/gemini-api/docs/models e verificar disponibilidade
- A key do Vision e diferente da key do Gemini — podem ser keys separadas
- Ambas as keys estao no `.env` do BFF (`naviera-web/.env`) — NUNCA commitar

### Arquivos-chave

| Arquivo | Funcao |
|---------|--------|
| `naviera-ocr/src/App.jsx` | App principal PWA (auth + navegacao + theme) |
| `naviera-ocr/src/screens/CapturaScreen.jsx` | Tela de camera/galeria + upload |
| `naviera-ocr/src/screens/RevisaoScreen.jsx` | Revisao de itens + botao "Revisar com IA" |
| `naviera-web/server/routes/ocr.js` | 8 endpoints OCR (upload, listar, revisar, aprovar, rejeitar, foto, ia-review) |
| `naviera-web/server/helpers/visionApi.js` | Chamada Google Cloud Vision |
| `naviera-web/server/helpers/geminiParser.js` | Chamada Gemini para reprocessar itens |
| `naviera-web/server/helpers/parseOcrText.js` | Parser regex (fallback sem IA) |
| `naviera-web/server/helpers/criarFrete.js` | Logica compartilhada de criacao de frete |
| `naviera-web/src/pages/ReviewOCR.jsx` | Tela de conferente no naviera-web |
| `database_scripts/022_ocr_lancamentos.sql` | Tabela ocr_lancamentos |

### Precos de frete vs precos do produto

O preco exibido nos itens OCR e o **preco de FRETE (transporte)**, NAO o preco do produto na nota fiscal. O preco vem da tabela `itens_frete_padrao` da empresa. Se o item nao esta cadastrado, preco = 0 e o operador preenche manualmente.

### Comandos

```bash
cd naviera-ocr && npm run dev                    # Dev local (porta 5175)
cd naviera-ocr && npm run build                  # Build producao
cd naviera-web/server && node index.js           # BFF (inclui rotas OCR)
```

---

## Regras importantes

1. **NUNCA** fazer query sem filtrar por `empresa_id` em tabelas de negocio
2. **SEMPRE** usar `DAOUtils.empresaId()` para obter o tenant atual
3. **NUNCA** hardcodar empresa_id = 1 nos DAOs (usar TenantContext)
4. Tabelas `aux_*` sao compartilhadas — NAO filtrar por empresa_id
5. `definirViagemAtiva` DEVE filtrar por empresa_id (senao desativa viagens de outras empresas)
6. DELETEs em cascata DEVEM incluir `AND empresa_id = ?` em cada subquery
7. O desktop funciona OFFLINE — toda logica de negocio deve funcionar sem API

---

### Fase 6: OCR + Lancamento por Foto — CONCLUIDA

- [x] naviera-ocr: PWA standalone com 5 telas (login, captura, revisao, confirmado, historico)
- [x] Google Cloud Vision: OCR de notas fiscais, cupons e cadernos
- [x] Google Gemini (gemini-3-flash-preview): revisao inteligente de itens com correcao de OCR
- [x] Parser regex como fallback (detecta NFC-e e notas manuais)
- [x] Validacao de precos contra itens_frete_padrao
- [x] Offline queue com IndexedDB + auto-sync
- [x] Tela ReviewOCR no naviera-web para conferente aprovar/rejeitar
- [x] Nginx: ocr.naviera.com.br com client_max_body_size 15M
- [x] Migration 022: tabela ocr_lancamentos com workflow de status

*Atualizado: 2026-04-14 — Adicionado naviera-ocr (lancamento de fretes por foto com OCR + Gemini AI)*

### Fase 7: Onboarding Self-Service — EM ANDAMENTO

- [x] Migration 023: codigo_ativacao + ativado_em em empresas, deve_trocar_senha em usuarios
- [x] API: POST /public/registrar-empresa (OnboardingService — cria empresa + usuario + codigo)
- [x] API: GET /public/ativar/{codigo} (retorna empresa_id, nome, slug, operador)
- [x] Desktop: SetupWizard reescrito — 3 telas (ativacao, preparando, pronto), zero config tecnica
- [ ] Site: pagina de cadastro self-service (CadastroEmpresaPublico.jsx no naviera-site)
- [ ] Correcao: JwtFilter + BFF adminOnly — padronizar funcao 'Administrador'
- [ ] Correcao: UsuarioDAO.buscarPorUsuarioESenha aceitar email (Desktop login)
- [ ] Correcao: Troca de senha obrigatoria no primeiro login

**Fluxo do operador:**
1. Operador acessa naviera.com.br/cadastro
2. Preenche: nome empresa, CNPJ, embarcacao, email, senha
3. Recebe codigo de ativacao (ex: NAV-7F3A) + botao download
4. Instala Desktop, abre → digita codigo → tudo automatico
5. Tela "Pronto! Login: email. Use a senha do site." → Iniciar
