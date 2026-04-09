# Dim 3 — Infraestrutura

## Dockerfile
- **PRONTO** — naviera-app: Multi-stage (Node 20 build + Nginx serve)
- **PRONTO** — docker-compose.yml com api + app, depends_on com healthcheck

## Deploy
- **INCOMPLETO** — Sem CI/CD (sem GitHub Actions, sem pipeline)
- **PRONTO** — Deploy manual via docker compose up -d --build

## .env.example
- **PRONTO** — .env.example na raiz com DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, JWT_SECRET, CORS_ORIGINS
- **PRONTO** — naviera-api/.env.example existe

## Migrations
- **PRONTO** — database_scripts/ com migrations numeradas 000-009
- **INCOMPLETO** — Sem ferramenta de migration automatica (scripts manuais SQL)

## Setup
- **INCOMPLETO** — Precisa: cp .env.example .env + preencher + docker compose up
- Banco e externo (nao containerizado), precisa estar rodando

## README
- **PRONTO** — CLAUDE.md documenta as 3 camadas
- **INCOMPLETO** — Sem README.md dedicado para o app

## Logs
- **INCOMPLETO** — Spring Boot loga no stdout, sem log file/rotation config

## Healthcheck
- **PRONTO** — Spring Actuator em /api/actuator/health
- **PRONTO** — docker-compose usa healthcheck da API

## Contagem
| Status | Qtd |
|--------|-----|
| PRONTO | 7 |
| INCOMPLETO | 5 |
| FALTANDO | 0 |
| POS-MVP | 0 |
