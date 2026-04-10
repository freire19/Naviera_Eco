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

---

## Arquitetura Multi-Tenant

### Modelo de operacao

- **Desktop** = JavaFX + PostgreSQL LOCAL = funciona OFFLINE no barco
- **Web** = React + Express BFF (acesso direto ao PostgreSQL) = mesmo que Desktop, para escritorio com internet
- **App Mobile** = React web (dev) → mobile futuro = clientes finais CPF/CNPJ (compra passagem, rastreia encomenda, GPS) = consome API Spring Boot
- **Sync** = quando barco chega em area com internet, sync bidirecional automatico

### Estrategia de isolamento

- Coluna `empresa_id` em TODAS as tabelas de negocio (exceto aux_*)
- `TenantContext` (ThreadLocal) armazena empresa_id da thread atual
- Desktop: empresa_id fixo, lido de `db.properties` (chave `empresa.id`)
- API REST: empresa_id extraido do JWT a cada request
- Tabelas auxiliares (aux_*) sao compartilhadas entre empresas

### Arquivos-chave do multi-tenant

| Arquivo | Funcao |
|---------|--------|
| `database_scripts/013_multi_tenant.sql` | Migration: cria tabela `empresas`, adiciona `empresa_id` em todas as tabelas |
| `src/dao/TenantContext.java` | ThreadLocal com empresa_id — usado por todos os DAOs |
| `src/dao/DAOUtils.java` | Helpers: `empresaId()`, `setEmpresa()`, `TENANT_FILTER` |
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
| DAOs tenant-aware | FEITO | 22/24 DAOs migrados (faltam EmpresaDAO e BalancoViagemDAO) |
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
| EmpresaDAO | PENDENTE |
| BalancoViagemDAO | PENDENTE (queries complexas) |

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
- [x] Completar todos os DAOs da lista acima (22/24 feitos — faltam EmpresaDAO e BalancoViagemDAO)
- [ ] Atualizar EmpresaDAO para usar empresa_id ao inves de id_config fixo
- [ ] Testar com empresa_id = 1 (deve funcionar identico ao sistema atual)

### Fase 2: API tenant-aware + sync — PENDENTE

- [ ] API: JWT com empresa_id, filtro em todos endpoints
- [ ] Reescrever SyncClient (atual e deprecated/incompleto)
- [ ] WebSocket para notificacoes real-time
- [ ] Endpoints mobile: viagens publicas (cross-tenant), compra passagem, rastreio

### Fase 3: Desktop auto-update + Web — EM ANDAMENTO

- [ ] Sistema de versao: GET /api/versao/check no startup do desktop
- [ ] Instalador nativo via jpackage (JRE embutido)
- [x] naviera-web criado: React + Express BFF, 6 telas funcionais (leitura), 20 placeholder
- [ ] naviera-web: implementar operacoes de escrita (criar/editar passagem, encomenda, frete)
- [ ] naviera-web: implementar as 20 telas placeholder
- [ ] Painel admin Naviera (gestao de empresas/planos)

### Fase 4: App mobile + GPS — EM ANDAMENTO

- [x] UI do app criada em React web (11 telas, 2 perfis CPF/CNPJ) — dev em web, destino mobile
- [x] Cadastro CPF/CNPJ + login + auth JWT
- [x] Design system Naviera V4 (light/dark)
- [ ] Migrar de React web para mobile (React Native, PWA ou Capacitor)
- [ ] Rastreamento GPS (app tripulacao envia lat/lon)
- [ ] Notificacoes push (Firebase)
- [ ] Separar App.jsx monolitico em modulos

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

*Atualizado: 2026-04-10 — Correcao estado real das camadas Web, App e API*
