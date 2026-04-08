# Naviera — Sistema de Navegacao Fluvial

Sistema completo para gestao de transporte fluvial: passageiros, encomendas, fretes e financeiro.

## Componentes

| Componente | Tecnologia | Porta |
|-----------|------------|-------|
| **Desktop** | JavaFX 23 + JDK 17 | — |
| **API** | Spring Boot 3.3 + PostgreSQL | 8080 |
| **App** | React 19 + Vite | 3000 (dev) / 80 (prod) |

## Requisitos

- Java 17+
- Node 20+
- PostgreSQL 14+
- Docker + Docker Compose (para deploy)

## Setup rapido (desenvolvimento)

### 1. Banco de dados

```bash
# Criar banco
createdb sistema_embarcacao

# Rodar migrations (em ordem)
psql -d sistema_embarcacao -f database_scripts/001_criar_tabelas.sql
# ... ate o ultimo script
```

### 2. API (Spring Boot)

```bash
cd naviera-api

# Configurar variaveis de ambiente
export DB_USER=postgres
export DB_PASSWORD=sua_senha
export JWT_SECRET=$(openssl rand -base64 32)

# Rodar
mvn spring-boot:run
# API disponivel em http://localhost:8080/api
# Health check: http://localhost:8080/api/actuator/health
```

### 3. App (React)

```bash
cd naviera-app
npm install
npm run dev
# App disponivel em http://localhost:3000
```

### 4. Desktop (JavaFX)

Abrir projeto no Eclipse IDE. JavaFX SDK 23.0.2 necessario (ajustar paths no `.classpath`).

Entry point: `gui.Launch` (com login) ou `gui.LaunchDireto` (dev).

## Deploy com Docker

```bash
# Copiar e preencher .env
cp .env.example .env
# Editar .env com suas credenciais

# Subir
docker compose up -d --build

# API: http://localhost:8080/api
# App: http://localhost
```

## Estrutura

```
├── naviera-api/       # Spring Boot REST API
├── naviera-app/       # React frontend (mobile-first)
├── src/               # Desktop JavaFX (Eclipse project)
├── database_scripts/  # SQL migrations
├── docs/              # Auditorias e MVP plans
└── docker-compose.yml
```
