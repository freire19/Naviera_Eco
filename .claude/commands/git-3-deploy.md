---
name: git-3-deploy
description: Deploy generico para VPS via git pull + build + restart. Faz perguntas antes de iniciar para configurar o deploy. Trigger quando o usuario mencionar "deploy", "subir pra vps", "atualizar vps", "deploy vps", "deploy producao", "vps update", ou qualquer variacao de enviar para producao (exceto "deploy ed" que usa git-deploy-ed).
---

# Deploy VPS — Generico

Deploy de qualquer projeto para VPS via SSH. Coleta informacoes antes de executar.

## Credenciais Naviera

```
IP: 72.62.166.247
User: root
Password: Fr31r3VPS@2026#Sec
Repo na VPS: /var/www/naviera.com.br
Branch: main
```

### Estrutura na VPS

| Camada | Diretorio build | Process manager | Porta |
|--------|----------------|-----------------|-------|
| naviera-api | `/var/www/naviera.com.br/naviera-api` | systemd (`naviera-api.service`) | 8081 |
| naviera-web (BFF) | `/var/www/naviera.com.br/naviera-web/server` | PM2 (`naviera-web`) | 3003 |
| naviera-web (front) | Build → `/var/www/naviera-web/` | Nginx (estatico) | — |
| naviera-site | Build → `/var/www/naviera-site/` | Nginx (estatico) | — |
| naviera-app | Build → `/var/www/naviera.com.br/naviera-app/dist` | Nginx (estatico) | — |
| naviera-ocr | Build → `/var/www/naviera-ocr/` | Nginx (estatico) | — |

## Etapa 0 — Questionario (OBRIGATORIO antes de tudo)

Use AskUserQuestion para cada pergunta. Nao pule nenhuma. Adapte as perguntas seguintes com base nas respostas anteriores.

**Se o projeto for Naviera (detectado pelo diretorio de trabalho), pule direto para a pergunta "Quais camadas deployar?" usando as credenciais acima.**

### Perguntas obrigatorias:

1. **Tipo de deploy:**
   > E um deploy de projeto ja configurado na VPS ou primeira vez (setup novo)?

2. **Acesso SSH:**
   > Qual o IP, usuario e senha (ou chave SSH) da VPS?

3. **Dominio/URL:**
   > Qual o dominio ou URL onde o projeto sera acessivel? (ex: meusite.com)

4. **Caminho na VPS:**
   > Qual o diretorio do projeto na VPS? (ex: /var/www/meusite.com)
   - Se for setup novo, pergunte onde deseja clonar

5. **Stack do projeto:**
   > Qual a stack? (ex: Node/Next.js, Spring Boot, React+Express, HTML estatico, etc.)

6. **Process manager:**
   > Usa PM2, systemd, Docker, ou outro para manter o app rodando?
   - Se o usuario nao souber, sugira PM2 para Node ou systemd para Java

7. **Branch:**
   > Qual branch deployar? (default: main)

### Perguntas condicionais (se aplicavel):

- Se Node.js: "Tem migrations (Prisma, Knex, etc.)?"
- Se Java/Spring: "Usa Maven ou Gradle? Tem migrations Flyway/Liquibase?"
- Se monorepo: "Quais apps/packages precisam de build?"
- Se primeira vez: "Precisa configurar nginx/reverse proxy?"

**Guarde todas as respostas em variaveis mentais antes de prosseguir.**

---

## Etapa 1 — Validacao local

Antes de tocar na VPS:

```bash
# Verificar se ha commits nao pushados
git log origin/$(git branch --show-current)..HEAD --oneline
```

Se houver commits pendentes: avise "Ha commits nao pushados. Rode `/git-2-push` primeiro." e **pare**.

Analise o que mudou nos ultimos commits:
```bash
git diff --name-only HEAD~3..HEAD
```

---

## Etapa 2 — Conexao SSH + Git Pull

```bash
sshpass -p '<SENHA>' ssh -o StrictHostKeyChecking=no <USER>@<IP> "
  cd <CAMINHO> && git pull origin <BRANCH>
"
```

Se der conflito: **PARE** e avise o usuario. Nunca force.

Se for setup novo (primeira vez):
```bash
sshpass -p '<SENHA>' ssh -o StrictHostKeyChecking=no <USER>@<IP> "
  mkdir -p <CAMINHO_PAI> && cd <CAMINHO_PAI> && git clone <REPO_URL> <PASTA>
"
```

---

## Etapa 3 — Install + Build (adaptar a stack)

### Node.js (Next.js, Express, Vite, etc.)
```bash
cd <CAMINHO> && npm install && npm run build
```

### Java (Spring Boot — Maven)
```bash
cd <CAMINHO> && ./mvnw clean package -DskipTests
```

### Java (Spring Boot — Gradle)
```bash
cd <CAMINHO> && ./gradlew clean build -x test
```

### HTML estatico
Nenhum build necessario.

### Monorepo
Build apenas os packages/apps que mudaram (detectado na Etapa 1).

---

## Etapa 4 — Migrations (se aplicavel)

### Prisma
```bash
cd <CAMINHO> && npx prisma migrate deploy
```

### Flyway
```bash
cd <CAMINHO> && ./mvnw flyway:migrate
```

**NUNCA** use comandos de migration "dev" em producao (ex: `prisma migrate dev`, `flyway repair`).

---

## Etapa 5 — Restart do servico

### PM2
```bash
pm2 restart <APP_NAME> --update-env
```

### systemd
```bash
systemctl restart <SERVICE_NAME>
```

### Docker
```bash
cd <CAMINHO> && docker compose up -d --build
```

Aguarde 5-10 segundos para warmup.

---

## Etapa 6 — Health Check

Tente verificar se o app esta respondendo:

```bash
curl -sf -o /dev/null -w '%{http_code}' http://localhost:<PORTA>
```

Ou, se tiver endpoint de health:
```bash
curl -sf http://localhost:<PORTA>/health
```

Se falhar: cheque logs antes de reportar.
- PM2: `pm2 logs <APP> --lines 30 --nostream`
- systemd: `journalctl -u <SERVICE> -n 30 --no-pager`
- Docker: `docker logs <CONTAINER> --tail 30`

---

## Etapa 7 — Relatorio final

Mostre resumo compacto:

```
Deploy concluido!
  Projeto: <nome>
  VPS: <IP> (<dominio>)
  Branch: <branch>
  Apps atualizados: <lista>
  Migrations: <sim/nao>
  Health check: <status>
```

---

## Regras

- **NUNCA** sobrescreva `.env` na VPS
- **NUNCA** use migrations "dev" em producao
- **NUNCA** faca `git reset --hard` na VPS sem autorizacao
- **NUNCA** faca force push ou force pull
- Se o build falhar, **NAO** reinicie o servico — o app antigo continua rodando
- Sempre faca health check apos restart
- Se algo der errado, mostre os logs e pergunte ao usuario antes de agir
