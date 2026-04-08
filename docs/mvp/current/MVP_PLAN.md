# MVP PLAN — Naviera (API + App)
> **Versao:** V1.0
> **Data:** 2026-04-08
> **Status:** PRECISA DE TRABALHO

---

## RESUMO

| Status | Itens |
|--------|-------|
| PRONTO | 38 |
| INCOMPLETO | 20 |
| FALTANDO | 22 |
| POS-MVP | 1 |

**Bloqueadores:** 3 itens impedem MVP
**Estimativa total:** ~10 horas

---

## FUNCIONALIDADES CORE

### Login CPF/CNPJ
- **Status:** PRONTO
- **Estado atual:** AuthService com JWT + BCrypt, frontend integrado, loading state, erro exibido
- **Observacoes:**
> _Login funcional de ponta a ponta. JWT com expiracao 24h._

### Cadastro de novo cliente
- **Status:** FALTANDO
- **Estado atual:** API POST /auth/cadastro existe e funciona. Frontend NAO tem tela.
- **O que falta:** Tela de cadastro no React (campos: documento, nome, email, telefone, cidade, senha)
- **Observacoes:**
> _BLOQUEADOR. O texto "Cadastre-se" na tela de login nao e clicavel. Sem isso, nenhum usuario novo entra._

### Listar encomendas/fretes do cliente
- **Status:** PRONTO
- **Estado atual:** CPF busca encomendas, CNPJ busca fretes. Frontend diferencia automaticamente.
- **Observacoes:**
> _Vinculacao por nome (ILIKE). Funciona para MVP mas e fragil a longo prazo._

### Detalhe de encomenda/frete
- **Status:** PRONTO
- **Estado atual:** Tela de detalhe com rastreio (timeline 4 etapas), valores, status
- **Observacoes:**
> _Completo._

### Resumo financeiro
- **Status:** PRONTO
- **Estado atual:** GET /financeiro/resumo retorna totalDevido, situacao, temRestricao. Frontend exibe.
- **Observacoes:**
> _Diferencia CPF (encomendas) vs CNPJ (fretes). Calcula vencidos (>30 dias)._

### Pagamento via app
- **Status:** INCOMPLETO
- **Estado atual:** Registra em pagamentos_app como PENDENTE. Frontend chama API antes de mostrar sucesso.
- **O que falta:** Gateway real (Mercado Pago/PIX). Hoje pagamento fica PENDENTE sem confirmacao automatica.
- **Observacoes:**
> _Aceitavel para MVP se operador confirma manualmente no desktop. Erros usam alert() nativo._

### Ver embarcacoes
- **Status:** PRONTO
- **Estado atual:** Lista publica, busca por nome, detalhe com mapa SVG, status EM_VIAGEM/NO_PORTO
- **Observacoes:**
> _GPS real nao implementado (retorna 501). Posicao no mapa e estimada._

### Chat IA
- **Status:** INCOMPLETO
- **Estado atual:** Frontend tem pattern matching local. Backend nao tem endpoint.
- **O que falta:** POST /api/chat com Claude API
- **Observacoes:**
> _POS-MVP. Chat local funciona como FAQ basico._

### Feedback/avaliacao
- **Status:** PRONTO
- **Estado atual:** POST /feedback funciona. Frontend com estrelas + comentario + confirmacao visual.
- **Observacoes:**
> _Erro usa alert(). Menor prioridade._

### Edicao de perfil
- **Status:** INCOMPLETO
- **Estado atual:** GET/PUT /perfil funciona no backend. Frontend tem estados declarados mas tela nao renderizada.
- **O que falta:** Renderizar formulario de edicao no JSX da tab Perfil
- **Observacoes:**
> _Codigo estava no prototipo original (naviera-v4.jsx) mas foi perdido na integracao._

### Recuperacao de senha
- **Status:** FALTANDO
- **Estado atual:** Nenhuma implementacao
- **O que falta:** Endpoint + tela. Pode ser "Esqueci senha → contate suporte" para MVP.
- **Observacoes:**
> _Nao e bloqueador para MVP se houver canal alternativo (WhatsApp do suporte)._

### Reserva de passagem
- **Status:** FALTANDO
- **Estado atual:** Nenhum endpoint nem tela funcional
- **O que falta:** Backend + frontend completos
- **Observacoes:**
> _POS-MVP. Passagens continuam sendo vendidas no desktop._

### Historico de pagamentos
- **Status:** PRONTO
- **Estado atual:** GET /financeiro/historico + frontend lista com status e valores
- **Observacoes:**
> _Completo._

---

## FLUXOS CRITICOS

### Fluxo: Primeiro uso (onboarding)
- **Status:** FALTANDO
- **Etapas:**
  - [ ] Tela de cadastro — FALTANDO
  - [x] Validacao de documento unico — PRONTO (backend)
  - [x] Hash de senha — PRONTO (BCrypt)
  - [x] Gerar JWT apos cadastro — PRONTO (retorna token)
  - [ ] Redirecionar para home — FALTANDO (sem tela)
- **Gaps:** Tela de cadastro no frontend
- **Observacoes:**
> _BLOQUEADOR. Sem cadastro, zero usuarios novos._

### Fluxo: Login → consultar dados
- **Status:** PRONTO
- **Etapas:**
  - [x] Digitar CPF/CNPJ + senha
  - [x] Chamar API de login
  - [x] Armazenar JWT
  - [x] Carregar encomendas/fretes
  - [x] Exibir com loading state
- **Gaps:** Nenhum
- **Observacoes:**
> _Happy path completo._

### Fluxo: Pagamento completo
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] Selecionar debito pendente
  - [x] Escolher forma de pagamento (PIX/cartao/boleto)
  - [x] Chamar POST /financeiro/pagar
  - [x] Exibir confirmacao de registro
  - [ ] Confirmacao automatica via gateway — FALTANDO
- **Gaps:** Sem gateway real. Pagamento fica PENDENTE.
- **Observacoes:**
> _Aceitavel para MVP com confirmacao manual._

### Fluxo: Erro e recuperacao
- **Status:** INCOMPLETO
- **Etapas:**
  - [x] Erro de login — mensagem exibida
  - [x] Sessao expirada (401) — redirect automatico
  - [ ] Erro de rede/servidor — silenciado (.catch(()=>[]))
  - [ ] Erro de pagamento — usa alert() nativo
- **Gaps:** Erros de fetch silenciados, alert() em vez de modal
- **Observacoes:**
> _Usuario ve tela vazia sem saber o que houve._

---

## INFRAESTRUTURA

| Item | Status | Detalhe |
|------|--------|---------|
| Migracao SQL 008 | PRONTO | 4 tabelas novas, triggers, indices |
| pom.xml | PRONTO | Spring Boot 3.3.0, todas deps |
| package.json | PRONTO | React 18.3.1, react-scripts 5.0.1 |
| application.yml | INCOMPLETO | Env vars com fallback inseguro |
| application-prod.yml | FALTANDO | Sem perfil de producao |
| .gitignore (backend) | FALTANDO | Vai commitar target/ |
| .gitignore (frontend) | FALTANDO | Vai commitar node_modules/ |
| npm install | FALTANDO | node_modules/ nao existe |
| Dockerfile backend | FALTANDO | — |
| Dockerfile frontend | FALTANDO | — |
| docker-compose.yml | FALTANDO | — |
| .env.example | FALTANDO | — |
| README.md | FALTANDO | — |
| Health check | FALTANDO | Sem Actuator |
| CI/CD | FALTANDO | — |

- **Observacoes:**
> _Infra e o maior gap. Projeto nao e deployavel sem Dockerfiles e configuracao de producao._

---

## SEGURANCA MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| Autenticacao JWT | PRONTO | BCrypt + JWT 24h |
| Senha BCrypt | PRONTO | encode/matches corretos |
| JWT secret via env var | INCOMPLETO | Fallback fraco hardcoded |
| DB credentials via env var | INCOMPLETO | Fallback "123456" hardcoded |
| Input validation backend | INCOMPLETO | Auth e Financeiro OK, Perfil e Feedback sem validacao |
| Input validation frontend | FALTANDO | Nenhuma validacao local |
| Dados sensiveis | INCOMPLETO | setSenhaHash(null) fragil — deveria usar DTO |
| CORS | INCOMPLETO | Sem origin de producao |
| HTTPS | FALTANDO | — |
| Rate limiting | FALTANDO | Login vulneravel a brute force |
| Protecao enumeracao | FALTANDO | "Cliente nao encontrado" vs "Senha incorreta" |
| Soft delete | PRONTO | @Where em todos entities |
| Endpoints publicos | PRONTO | Corretos |

---

## ESTABILIDADE

| Item | Status | Detalhe |
|------|--------|---------|
| GlobalExceptionHandler | INCOMPLETO | Cobre RuntimeException, falta DataAccessException etc |
| Custom exceptions | FALTANDO | Usa RuntimeException para tudo |
| Connection pool | PRONTO | HikariCP 5 conns, 30min lifetime |
| Graceful shutdown | PRONTO | Spring Boot default |
| Transacoes | INCOMPLETO | Apenas 1 @Transactional em todo projeto |
| Lazy loading safety | PRONTO | Null checks em todos toDTO() |
| Null safety financeiro | PRONTO | BigDecimal null checks completos |
| Paginacao | FALTANDO | Listas retornam tudo |
| Testes | FALTANDO | Zero testes |

---

## UX MINIMA

| Item | Status | Detalhe |
|------|--------|---------|
| Loading indicators | PRONTO | Todas as tabs + botoes |
| Sucesso visual | PRONTO | Modal pagamento, feedback |
| Erro login | PRONTO | Mensagem no formulario |
| Erro pagamento | INCOMPLETO | alert() nativo |
| Erro fetch dados | FALTANDO | Silenciado |
| Fluxo intuitivo | PRONTO | Bottom nav, cards, back button |
| Responsividade | PRONTO | Mobile-first 420px |
| Tela cadastro | FALTANDO | BLOQUEADOR |
| Edicao perfil | INCOMPLETO | Estado existe, renderizacao falta |
| Empty states | INCOMPLETO | Parcial |

---

## DEPENDENCIAS

| Servico/API | Status | Configurado | Fallback |
|------------|--------|------------|----------|
| PostgreSQL | PRONTO | Sim (env var) | HikariCP retry |
| Mercado Pago (PIX) | FALTANDO | Nao | Pagamento manual |
| Firebase (push) | FALTANDO | Nao | Sem notificacao |
| Claude API (chat) | FALTANDO | Nao | Pattern matching local |

---

## PLANO DE ACAO POR FASES

### Fase 1 — Bloqueadores (AGORA)
- [ ] Criar tela de cadastro no frontend — **Arquivo:** `naviera-app/src/App.jsx` — **Esforco:** 2h
- [ ] Executar npm install — **Arquivo:** `naviera-app/` — **Esforco:** 5min
- [ ] Criar .gitignore backend — **Arquivo:** `naviera-api/.gitignore` — **Esforco:** 10min
- [ ] Criar .gitignore frontend — **Arquivo:** `naviera-app/.gitignore` — **Esforco:** 10min
- [ ] Remover fallback de credenciais inseguros — **Arquivo:** `naviera-api/src/main/resources/application.yml` — **Esforco:** 15min
- **Notas:**
> _Sem estes itens o projeto nao pode ser usado por ninguem. Prioridade maxima._
- **Esforco total:** ~3h

### Fase 2 — Incompletos Criticos (esta semana)
- [ ] Adicionar error states no frontend (substituir .catch(()=>[]) por mensagem visual) — **Arquivo:** `naviera-app/src/App.jsx` — **Esforco:** 1h
- [ ] Substituir alert() por modal de erro no pagamento e feedback — **Arquivo:** `naviera-app/src/App.jsx` — **Esforco:** 30min
- [ ] Criar DTOs com validacao para PerfilController e FeedbackController — **Arquivo:** `naviera-api/src/main/java/com/naviera/api/dto/` — **Esforco:** 1h
- [ ] Renderizar formulario de edicao de perfil no frontend — **Arquivo:** `naviera-app/src/App.jsx` — **Esforco:** 1h
- [ ] Adicionar CORS com origin via env var — **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/CorsConfig.java` — **Esforco:** 20min
- [ ] Adicionar @Transactional em write operations — **Arquivo:** `AuthService.java`, `PerfilController.java` — **Esforco:** 15min
- **Notas:**
> _Estes itens melhoram significativamente a qualidade e seguranca do MVP._
- **Esforco total:** ~4h

### Fase 3 — Estabilidade (antes do lancamento)
- [ ] Criar Dockerfile backend (multi-stage build) — **Arquivo:** `naviera-api/Dockerfile` — **Esforco:** 30min
- [ ] Criar Dockerfile frontend — **Arquivo:** `naviera-app/Dockerfile` — **Esforco:** 30min
- [ ] Criar docker-compose.yml — **Arquivo:** raiz — **Esforco:** 30min
- [ ] Criar .env.example — **Arquivo:** raiz — **Esforco:** 15min
- [ ] Adicionar Spring Actuator (health check) — **Arquivo:** `pom.xml` + `application.yml` — **Esforco:** 15min
- [ ] Criar custom exceptions em vez de RuntimeException — **Arquivo:** `naviera-api/src/main/java/com/naviera/api/config/` — **Esforco:** 1h
- [ ] Criar README.md com instrucoes de setup — **Arquivo:** `naviera-api/README.md`, `naviera-app/README.md` — **Esforco:** 30min
- **Notas:**
> _Deploy sem Docker e possivel mas fragil. Com Docker, reproducivel._
- **Esforco total:** ~3.5h

### Fase 4 — Polish (pos-lancamento)
- [ ] Testes unitarios (auth + financeiro services) — **Esforco:** 3h
- [ ] Paginacao nas listas de encomendas/fretes — **Esforco:** 1h
- [ ] Validacao de CPF/CNPJ no frontend — **Esforco:** 30min
- [ ] Mensagem unificada de erro login (nao revelar se documento existe) — **Esforco:** 15min
- [ ] Rate limiting no login — **Esforco:** 1h
- [ ] Structured logging com logback — **Esforco:** 30min

### Backlog — Pos-MVP
- [ ] Gateway de pagamento Mercado Pago (PIX real) — **Prioridade:** alta
- [ ] Push notifications com Firebase — **Prioridade:** alta
- [ ] Chat IA com Claude API — **Prioridade:** media
- [ ] Reserva de passagem via app — **Prioridade:** media
- [ ] Recuperacao de senha — **Prioridade:** media
- [ ] Campo cpf_destinatario em encomendas/fretes (vinculacao precisa) — **Prioridade:** media
- [ ] Localizacao GPS de embarcacoes — **Prioridade:** baixa
- [ ] Tabela de parceiros no banco + CRUD — **Prioridade:** baixa
- [ ] Service worker / offline mode — **Prioridade:** baixa
- **Notas:**
> _O gateway de pagamento e o upgrade mais impactante — transforma PENDENTE em confirmacao instantanea._

---

## HISTORICO

| Versao | Data | Prontos | Incompletos | Faltando | Status |
|--------|------|---------|-------------|----------|--------|
| V1.0 | 2026-04-08 | 38 | 20 | 22 | PRECISA DE TRABALHO |

---

## NOTAS GERAIS
> - **Trade-off vinculacao por nome:** Encomendas sao vinculadas ao cliente por ILIKE no nome (remetente/destinatario). Funciona para MVP mas vai gerar falsos positivos ("Maria" match "Ana Maria"). Solucao definitiva: adicionar cpf_destinatario nas tabelas do desktop.
> - **Trade-off pagamento PENDENTE:** Sem gateway real, todo pagamento fica PENDENTE e precisa de confirmacao manual do operador no desktop. Aceitavel para MVP porque o volume e baixo (<50 pagamentos/dia).
> - **Divida tecnica:** Zero testes, RuntimeException generico, string matching no exception handler. Aceitavel para MVP mas precisa ser resolvido antes de escalar.
> - **Risco de concorrencia:** Desktop e API escrevem no mesmo banco. Para MVP, a API so cria registros em pagamentos_app (tabela nova) e nao atualiza encomendas/fretes diretamente, eliminando conflito.

---
*Gerado por Claude Code — Revisao humana obrigatoria*
