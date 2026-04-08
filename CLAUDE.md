# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Desktop JavaFX application for managing river/boat transportation operations — passengers, freight, parcels (encomendas), and finances. Called "Sistema Embarcacao". Written in Portuguese (BR).

## Build & Run

This is an **Eclipse IDE project** (no Maven/Gradle). JDK 17 required.

- **JavaFX SDK**: 23.0.2 — paths in `.classpath` point to `C:/javafx-sdk-23.0.2/lib/` (adjust for your OS)
- **Dependencies**: All JARs in `lib/` directory (PostgreSQL driver, jBCrypt, JasperReports, PDFBox, Tess4J, etc.)
- **Entry point**: `gui.Launch` → calls `gui.LoginApp.main()` (JavaFX Application)
- **Alternative entry**: `gui.LaunchDireto` (bypasses login, for dev testing)
- **Database**: PostgreSQL on `localhost:5432/sistema_embarcacao` — credentials in `src/dao/ConexaoBD.java`
- **SQL scripts**: `database_scripts/` contains schema creation and migration scripts (numbered 001-005 + standalone scripts)
- **Tests**: JUnit 4 tests in `src/tests/` — run via Eclipse test runner
- **Output**: Compiled to `bin/`

## Architecture

**Pattern**: DAO + MVC (no service layer — controllers call DAOs directly)

```
src/
├── dao/          # Data Access Objects (26 classes) + ConexaoBD (JDBC connection provider)
├── gui/          # JavaFX controllers + FXML views + Launch/LoginApp entry points
│   └── util/     # UI helpers (AlertHelper, MascarasFX, PermissaoService)
├── model/        # POJOs/entities (~25 classes: Passagem, Passageiro, Encomenda, Frete, etc.)
└── tests/        # JUnit 4 tests
```

**Key flow**: FXML view → Controller (in `gui/`) → DAO (in `dao/`) → PostgreSQL via `ConexaoBD.getConexao()`

**Database connection**: `ConexaoBD.java` provides static `getConexao()` returning raw JDBC `Connection`. No connection pooling. Every caller must close connections manually.

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
