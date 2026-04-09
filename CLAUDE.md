# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Naviera** — sistema completo de 3 camadas para gestao de transporte fluvial (passageiros, fretes, encomendas e financeiro). Escrito em Portugues (BR).

| Camada | Tecnologia | Funcao | Porta |
|--------|-----------|--------|-------|
| **Desktop** | JavaFX 23 + Java 17 (Eclipse) | Console operacional (tripulacao/admin) | — |
| **API** | Spring Boot 3.3 + PostgreSQL | Backend REST servindo desktop e app | 8080 |
| **App** | React 19 + Vite | Frontend mobile-first (clientes) | 5173 dev / 80 prod |

As 3 camadas compartilham o **mesmo banco PostgreSQL** (`sistema_embarcacao`).

---

## Camada 1: Desktop (JavaFX)

**Eclipse IDE project** (no Maven/Gradle). JDK 17 required.

- **JavaFX SDK**: 23.0.2 — `/opt/javafx-sdk-23.0.2/lib/` (Linux) ou `C:/javafx-sdk-23.0.2/lib/` (Windows)
- **Dependencies**: JARs em `lib/` (PostgreSQL driver, jBCrypt, JasperReports, PDFBox, Tess4J, etc.)
- **Entry point**: `gui.Launch` → `gui.LoginApp.main()` (JavaFX Application)
- **Dev entry**: `gui.LaunchDireto` (bypasses login)
- **Database**: Conexao via `src/dao/ConexaoBD.java` + `db.properties`
- **Tests**: JUnit 4 em `src/tests/`
- **Output**: Compilado em `bin/`

**Pattern**: DAO + MVC (controllers chamam DAOs diretamente, sem service layer)

```
src/
├── dao/          # Data Access Objects (26 classes) + ConexaoBD (pool JDBC)
├── gui/          # JavaFX controllers + FXML views + Launch/LoginApp
│   └── util/     # UI helpers (AlertHelper, MascarasFX, PermissaoService)
├── model/        # POJOs/entities (~25 classes)
└── tests/        # JUnit 4 tests
```

**Key flow**: FXML view → Controller (`gui/`) → DAO (`dao/`) → PostgreSQL via `ConexaoBD.getConexao()`

---

## Camada 2: API REST (Spring Boot)

Diretorio: `naviera-api/`

- **Framework**: Spring Boot 3.3.5, Java 17, Maven
- **Auth**: JWT (io.jsonwebtoken) + Spring Security + BCrypt
- **DB**: Spring Data JPA + HikariCP, `ddl-auto=validate`
- **Context path**: `/api` (ex: `http://localhost:8080/api/auth/login`)
- **Health**: Spring Actuator em `/api/actuator/health`
- **CORS**: Permite `localhost:3000` e `localhost:5173` por padrao

```
naviera-api/src/main/java/com/naviera/api/
├── config/         # SecurityConfig, CorsConfig, GlobalExceptionHandler
├── controller/     # 8 REST controllers (Auth, Encomenda, Frete, Viagem, Embarcacao, Perfil, Loja, Tarifa)
├── service/        # 8 services (logica de negocio)
├── repository/     # JPA Repositories (Spring Data)
├── model/          # JPA Entities (ClienteApp, Embarcacao, Viagem, Rota, LojaParceira, PedidoLoja)
├── dto/            # DTOs (LoginRequest, EncomendaDTO, etc.)
└── security/       # JwtUtil, JwtFilter
```

**Run**:
```bash
cd naviera-api
DB_USER=postgres DB_PASSWORD=<senha> JWT_SECRET=<secret> SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5437/sistema_embarcacao mvn spring-boot:run
```

---

## Camada 3: App Mobile-First (React + Vite)

Diretorio: `naviera-app/`

- **Framework**: React 19.2.5 + Vite 5.4.21
- **Componente principal**: `src/App.jsx` (~53KB — app completo em um arquivo)
- **Funcionalidades**: Viagens, Encomendas, Fretes, Lojas Parceiras, Amigos, Chat
- **Temas**: Light/Dark mode
- **Producao**: Docker multi-stage + Nginx (`nginx.conf`)

```
naviera-app/
├── src/
│   ├── main.jsx    # Entry point
│   └── App.jsx     # Root component (toda a app)
├── dist/           # Build de producao
├── package.json
├── vite.config.js
├── Dockerfile
└── nginx.conf
```

**Run**:
```bash
cd naviera-app
npm install
npm run dev      # Dev em http://localhost:5173
npm run build    # Build producao → dist/
```

---

## Database

- **PostgreSQL**: `sistema_embarcacao` (44+ tabelas)
- **Migrations**: `database_scripts/` (numeradas 000-009)
- **Config desktop**: `db.properties` (criado a partir de `db.properties.example`)
- **Config API**: env vars `DB_USER`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`

Tabelas principais do app mobile (migration 008-009):
- `clientes_app` — usuarios do app (CPF/CNPJ)
- `lojas_parceiras` — vitrines de comerciantes CNPJ
- `pedidos_loja` — pedidos entre clientes e lojas
- `avaliacoes_loja` — avaliacoes de lojas
- `amigos_app` — rede de amigos

## Docker (producao)

```bash
cp .env.example .env  # preencher credenciais
docker compose up -d --build
# api: porta 8080 | app: porta 80 | db: externo
```

## Domain Terminology

- **Passagem** = passenger ticket; **Passageiro** = passenger
- **Encomenda** = parcel/package shipment; **ItemEncomendaPadrao** = standard parcel item type
- **Frete** = freight shipment
- **Viagem** = trip/voyage; **Rota** = route; **Embarcacao** = vessel/boat
- **Caixa** = cash register; **Boleto** = payment slip
- **Balanco Viagem** = trip financial balance/report
- **Estorno** = refund/reversal
- **Saida** = cash outflow/expense; **Entrada** = cash inflow

## Known Critical Issues

The project has extensive audit documentation in `docs/audits/current/` and a summary in `docs/STATUS.md` (32+ critical issues). Key architectural problems to be aware of when making changes:

- Race conditions in sequential numbering (MAX+1 pattern in PassagemDAO, EncomendaDAO)
- Some financial calculations still use `double` instead of `BigDecimal`
- Mixed authentication approaches (some plaintext comparison, some BCrypt)
- Connection leaks in several controllers (missing `finally` blocks)
- No permission checks on most screens (PermissaoService exists but is not wired into all views)
