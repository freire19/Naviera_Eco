# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Naviera Eco** — Plataforma SaaS multi-tenant de gestao de transporte fluvial (passageiros, fretes, encomendas e financeiro).

| Camada | Tecnologia | Porta | Status |
|--------|-----------|-------|--------|
| **Desktop** | JavaFX 23 + Java 17 | — | Funcional — multi-tenant aware |
| **API** | Spring Boot 3.3 + Java 17 + Maven | 8081 | Funcional — ~28 controllers, 108+ endpoints |
| **Web** | React 18 + Vite 5 + Express BFF | 5174 / 3002 | Funcional — 29 paginas, CRUD completo |
| **App Mobile** | React 19 + Vite | 3000 | 15+ telas, 2 perfis (CPF/CNPJ) |

Todas as camadas acessam o **mesmo banco PostgreSQL**. Multi-tenancy via coluna `empresa_id`.

---

## Build & Run Commands

### Desktop (JavaFX — sem Maven/Gradle)

```bash
# Compilar (requer JavaFX SDK 23 instalado)
JAVAFX_PATH="/path/to/javafx-sdk-23.0.2/lib"
CP="src"; for jar in lib/*.jar "$JAVAFX_PATH"/*.jar; do CP="$CP;$jar"; done
javac --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media -cp "$CP" -d bin $(find src -name "*.java")

# Copiar recursos
cp -r src/gui/*.fxml bin/gui/ && cp -r src/gui/*.css bin/gui/ && cp -r src/gui/icons bin/gui/ && cp -r resources/css bin/

# Rodar
CP="bin"; for jar in lib/*.jar; do CP="$CP;$jar"; done
java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media -cp "$CP" gui.LoginLauncher

# VS Code: F5 com .vscode/launch.json configurado (config "Naviera Desktop")
# Eclipse: Run gui.Launch (com login) ou gui.LaunchDireto (dev sem login)
```

### API (Spring Boot)

```bash
cd naviera-api
mvn spring-boot:run                # Dev (porta 8081)
mvn clean package -DskipTests      # Build JAR
mvn test                           # Rodar testes
```

### Web (React + Express BFF)

```bash
cd naviera-web
npm install
npm run dev                        # Frontend Vite (porta 5174, proxy /api → 3002)
# Em outro terminal:
cd naviera-web && node server/index.js  # BFF Express (porta 3002)
```

### App Mobile

```bash
cd naviera-app
npm install
npm run dev                        # Dev web (porta 3000)
npm run build                      # Build producao
```

### Docker (producao)

```bash
cp .env.example .env               # Preencher credenciais
docker compose up -d --build       # API :8081, App :8180, DB :5432
```

### Database

```bash
# Criar banco e rodar migrations
createdb -U postgres naviera_eco
psql -U postgres -d naviera_eco -f database_scripts/000_schema_completo.sql
# Rodar 001-018 em ordem
```

---

## Architecture

### Desktop (Eclipse project, sem build tool)

```
src/
├── dao/          # 27 DAOs tenant-aware + ConexaoBD (pool JDBC) + TenantContext
├── gui/          # 70+ controllers JavaFX + 40+ FXML + CSS
│   └── util/     # AlertHelper, PermissaoService, SyncClient, AppLogger
├── model/        # POJOs (~25 classes)
└── tests/        # JUnit 4
lib/              # 44 JARs (JavaFX, PostgreSQL JDBC, Jackson, iText, JasperReports, BCrypt)
```

**Fluxo:** FXML → Controller (`gui/`) → DAO (`dao/`) → PostgreSQL via `ConexaoBD.getConnection()`

**Entry points:** `gui.LoginLauncher` → `gui.LoginApp` (producao), `gui.LaunchDireto` (dev)

**Config:** `db.properties` (JDBC URL, credenciais, `empresa.id`, `app.versao`)

### API (Spring Boot 3.3.5)

```
naviera-api/src/main/java/com/naviera/api/
├── controller/    # 28 controllers REST
├── service/       # Logica de negocio
├── repository/    # Spring Data JPA
├── model/         # Entidades JPA
├── dto/           # Data Transfer Objects
├── security/      # JwtFilter, JwtUtil
└── config/        # CORS, Security, WebSocket, TenantUtils, RateLimitFilter
```

**Auth:** JWT + Spring Security + BCrypt. Token expira em 24h.

### Web (BFF pattern)

```
naviera-web/
├── src/pages/     # 29 paginas React (CRUD completo)
├── src/components/# Layout, Sidebar, TopBar
├── server/        # Express BFF
│   ├── routes/    # 10 arquivos de rotas (~50 endpoints)
│   ├── middleware/ # auth, rateLimit, tenant, validate
│   └── db.js      # Pool PostgreSQL direto
```

**Proxy:** Vite proxy `/api` → Express BFF (`:3002`) → PostgreSQL direto (sem passar pela API Spring)

### App Mobile

```
naviera-app/src/
├── screens/       # 15+ telas (CPF: Home, Amigos, Mapa, Passagens, Bilhete; CNPJ: Painel, Pedidos, Financeiro, Loja)
├── components/    # Badge, Card, Header, TabBar, Toast, Skeleton, Avatar
├── hooks/         # useNotifications, usePWA, useWebSocket
└── api.js         # Axios → Spring Boot API
```

---

## Multi-Tenant

- Coluna `empresa_id` em TODAS as tabelas de negocio (exceto `aux_*`)
- `TenantContext` (ThreadLocal) armazena empresa_id da thread atual
- Desktop: empresa_id fixo de `db.properties` (`empresa.id`)
- API: empresa_id extraido do JWT
- Web BFF: empresa_id extraido do JWT + slug do subdominio

**Arquivos-chave:**
- `src/dao/TenantContext.java` — ThreadLocal
- `src/dao/DAOUtils.java` — `empresaId()`, `setEmpresa()`, `TENANT_FILTER`
- `database_scripts/013_multi_tenant.sql` — Migration principal
- `naviera-web/server/middleware/tenant.js` — Resolve slug → empresa_id
- `nginx/naviera.conf` — Wildcard subdominio

---

## Regras importantes

1. **NUNCA** fazer query sem filtrar por `empresa_id` em tabelas de negocio
2. **SEMPRE** usar `DAOUtils.empresaId()` para obter o tenant atual
3. **NUNCA** hardcodar empresa_id nos DAOs (usar TenantContext)
4. Tabelas `aux_*` sao compartilhadas — NAO filtrar por empresa_id
5. `definirViagemAtiva` DEVE filtrar por empresa_id
6. DELETEs em cascata DEVEM incluir `AND empresa_id = ?` em cada subquery
7. O desktop funciona OFFLINE — toda logica de negocio deve funcionar sem API
8. Desktop usa `ConexaoBD` (pool JDBC custom) — NAO usar DriverManager direto
9. Banco real de desenvolvimento pode ser `sistema_embarcacao` (schema legado com colunas como `id_usuario`, `login_usuario`, `nome_completo`, `senha_hash`) — o schema do codigo (migrations) usa nomes diferentes (`id`, `nome`, `senha`)

---

## Database Migrations

20 scripts em `database_scripts/`, executar em ordem numerica (000-018). Script principal: `000_schema_completo.sql`. Migration multi-tenant: `013_multi_tenant.sql`.

**Nota:** O banco de dev (`sistema_embarcacao`) pode ter schema diferente das migrations — as migrations criam o schema "ideal" mas o banco existente usa nomes de colunas legados.

---

*Atualizado: 2026-04-12*
