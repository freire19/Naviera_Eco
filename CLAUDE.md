# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Naviera Eco** — Plataforma SaaS multi-tenant de gestao de transporte fluvial (passageiros, fretes, encomendas e financeiro). Evolucao do SistemaEmbarcacaoProjeto_Novo para suportar multiplas empresas.

| Camada | Tecnologia | Funcao | Status |
|--------|-----------|--------|--------|
| **Desktop** | JavaFX 23 + Java 17 | Console operacional offline (barco) | FUNCIONAL — migrando para multi-tenant |
| **Web** | React + Vite | Espelho do Desktop online (escritorio) | A CRIAR |
| **API** | Spring Boot 3.3 + PostgreSQL | Backend REST multi-tenant | PARCIAL — precisa tenant-awareness |
| **App Mobile** | React Native | App para clientes finais (CPF/CNPJ) | A CRIAR |

---

## Arquitetura Multi-Tenant

### Modelo de operacao

- **Desktop** = JavaFX + PostgreSQL LOCAL = funciona OFFLINE no barco
- **Web** = React online = mesmo que Desktop, para escritorio com internet
- **App Mobile** = online = clientes finais (compra passagem, rastreia encomenda, GPS)
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

## Camada API (Spring Boot)

Diretorio: `naviera-api/`

- **Framework**: Spring Boot 3.3.5, Java 17, Maven
- **Auth**: JWT + Spring Security + BCrypt
- **DB**: Spring Data JPA + HikariCP
- **Context path**: `/api`

---

## MIGRACAO MULTI-TENANT — STATUS E PLANO

### Fase 0: Preparacao (EM ANDAMENTO)

| Item | Status | Detalhe |
|------|--------|---------|
| Migration SQL 013 | FEITO | `013_multi_tenant.sql` — tabela `empresas`, `empresa_id` em 17 tabelas, indices, `clientes_app`, `versao_sistema` |
| TenantContext | FEITO | `dao/TenantContext.java` — ThreadLocal + default via db.properties |
| DAOUtils helpers | FEITO | `empresaId()`, `setEmpresa()`, `TENANT_FILTER` |
| ConexaoBD atualizado | FEITO | Le `empresa.id` do db.properties e inicializa TenantContext |
| ViagemDAO | FEITO | 100% tenant-aware (SELECT, INSERT, UPDATE, DELETE) |
| PassagemDAO INSERT | FEITO | INSERT inclui empresa_id |
| EncomendaDAO INSERT | FEITO | INSERT inclui empresa_id |

### DAOs PENDENTES — Padrao a seguir

**REGRA: Toda query que toca tabela de negocio DEVE filtrar por empresa_id.**

Padrao para cada tipo de operacao:

```java
// SELECT — adicionar WHERE empresa_id = ? (ou AND empresa_id = ?)
String sql = "SELECT * FROM passagens WHERE empresa_id = ? AND id_viagem = ?";
stmt.setInt(1, DAOUtils.empresaId());
stmt.setLong(2, idViagem);

// INSERT — adicionar empresa_id na lista de colunas e valores
String sql = "INSERT INTO fretes (..., empresa_id) VALUES (..., ?)";
stmt.setInt(N, DAOUtils.empresaId());

// UPDATE — adicionar AND empresa_id = ? no WHERE
String sql = "UPDATE passagens SET ... WHERE id_passagem = ? AND empresa_id = ?";
stmt.setInt(N, DAOUtils.empresaId());

// DELETE — adicionar AND empresa_id = ? no WHERE
String sql = "DELETE FROM passagens WHERE id_passagem = ? AND empresa_id = ?";
stmt.setInt(N, DAOUtils.empresaId());
```

**DAOs que PRECISAM de empresa_id (tenant-scoped):**

| DAO | Prioridade | Queries |
|-----|-----------|---------|
| PassagemDAO | FEITO | 100% tenant-aware |
| EncomendaDAO | FEITO | 100% tenant-aware |
| FreteDAO | FEITO | 100% tenant-aware |
| DespesaDAO | FEITO | 100% tenant-aware |
| FuncionarioDAO | FEITO | 100% tenant-aware |
| PassageiroDAO | PARCIAL | SQL atualizado, falta params |
| RotaDAO | FEITO | 100% tenant-aware |
| EmbarcacaoDAO | FEITO | 100% tenant-aware + ON CONFLICT fix |
| TarifaDAO | MEDIA | 1 INSERT + 3 SELECTs |
| CaixaDAO | FEITO | 100% tenant-aware |
| ConferenteDAO | PARCIAL | SQL atualizado, falta params |
| ClienteEncomendaDAO | PARCIAL | SQL atualizado, falta params |
| UsuarioDAO | BAIXA | 1 INSERT + 6 SELECTs |
| AgendaDAO | PARCIAL | SQL atualizado, falta params |
| ReciboAvulsoDAO | PARCIAL | SQL atualizado, falta params |
| ReciboQuitacaoPassageiroDAO | FEITO | 100% tenant-aware |
| TipoPassageiroDAO | PARCIAL | SQL atualizado, falta params |
| ItemFreteDAO | BAIXA | 1 INSERT + 1 SELECT |
| ItemEncomendaPadraoDAO | BAIXA | 1 INSERT + 1 SELECT |
| EncomendaItemDAO | BAIXA | 1 INSERT + 2 SELECTs |
| EmpresaDAO | ESPECIAL | Mudar para filtrar por empresa_id ao inves de id_config fixo |
| BalancoViagemDAO | MEDIA | Queries complexas de relatorio — verificar cada uma |

**DAOs que NAO precisam de empresa_id (dados globais):**

- AuxiliaresDAO (tabelas aux_* sao compartilhadas)

### Fase 1: Banco multi-tenant — PENDENTE

- [ ] Executar migration 013 no banco de producao
- [ ] Completar todos os DAOs da lista acima
- [ ] Atualizar EmpresaDAO para usar empresa_id ao inves de id_config fixo
- [ ] Testar com empresa_id = 1 (deve funcionar identico ao sistema atual)

### Fase 2: API tenant-aware + sync — PENDENTE

- [ ] API: JWT com empresa_id, filtro em todos endpoints
- [ ] Reescrever SyncClient (atual e deprecated/incompleto)
- [ ] WebSocket para notificacoes real-time
- [ ] Endpoints mobile: viagens publicas (cross-tenant), compra passagem, rastreio

### Fase 3: Desktop auto-update + Web — PENDENTE

- [ ] Sistema de versao: GET /api/versao/check no startup do desktop
- [ ] Instalador nativo via jpackage (JRE embutido)
- [ ] naviera-web em React (espelho do desktop)
- [ ] Painel admin Naviera (gestao de empresas/planos)

### Fase 4: App mobile + GPS — PENDENTE

- [ ] App React Native com cadastro CPF/CNPJ
- [ ] Rastreamento GPS (app tripulacao envia lat/lon)
- [ ] Notificacoes push (Firebase)
- [ ] Dashboard lojas parceiras (CNPJ)

---

## Comandos uteis

```bash
# Executar migration multi-tenant
psql -U postgres -d sistema_embarcacao -f database_scripts/013_multi_tenant.sql

# Verificar se empresa_id foi adicionado
psql -U postgres -d sistema_embarcacao -c "\d passagens" | grep empresa_id

# API (Spring Boot)
cd naviera-api && mvn spring-boot:run

# App Mobile (dev)
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

*Atualizado: 2026-04-09 — Inicio da migracao multi-tenant*
