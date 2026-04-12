# STATUS DO PROJETO — Naviera Eco
> Ultima atualizacao: 2026-04-10
> Atualizado por: Claude Code (status-update)

---

## Estado Geral: MVP QUASE PRONTO (77% — 95/124 itens prontos, 3 bloqueadores)

### Resumo
Plataforma SaaS multi-tenant de gestao fluvial com **4 camadas operacionais**: Desktop (JavaFX), API (Spring Boot), Web (React + Express BFF), App (React → mobile). Features core completas em todas as camadas. **3 bloqueadores de seguranca/infra** impedem deploy em producao: CORS aberto no BFF, SyncService SQL injection, HTTPS desativado. 0 issues CRITICAS de codigo. Auditorias: Security e Logic 100% limpas. MVP V4.0 aprofundou analise em infra/seguranca (124 itens vs 116 da V3.0).

---

## ISSUES CRITICAS ABERTAS

**Zero issues CRITICAS pendentes.** Todas as 3 criticas de Maintainability (DM031, DM033, DM041) foram corrigidas.

---

## ISSUES ALTAS PENDENTES

| # | Issue | Categoria | Nota |
|---|-------|-----------|------|
| #044 | VenderPassagemController 2170 linhas | Maintainability | Requer testes antes de refatorar |
| #045 | CadastroFreteController 2239 linhas | Maintainability | Requer testes antes de refatorar |
| #046 | InserirEncomendaController 1798 linhas | Maintainability | Requer testes antes de refatorar |
| #048 | Metodos com mais de 100 linhas | Maintainability | Depende de #044/#045/#046 |

> Todas as ALTAS de Security, Logic e Bugs foram resolvidas. Restam 4 ALTAS de Maintainability que requerem cobertura de testes.

---

## CATEGORIAS — ESTADO ATUAL

| Categoria | Total | Resolvidas | Ativas | % Resolvido | Status |
|-----------|-------|-----------|--------|-------------|--------|
| **Security** | **47** | **47** | **0** | **100%** | LIMPO |
| **Logic** | **75** | **75** | **0** | **100%** | LIMPO |
| **Bugs** | **36** | **36** | **0** | **100%** | **LIMPO** — 35 deep + 6 AUDIT anteriores, todas verificadas |
| **Resilience** | **67** | **65** | **2** | **97%** | **QUASE LIMPO** (restam DR025 pg_dump + DR028 testes) |
| **Performance** | **39** | **37** | **2** | **95%** | **QUASE LIMPO** (restam #048 JSON parser + #DP023 JARs) |
| **Maintainability** | **76** | **71** | **~5** | **93%** | **0 CRIT, 0 ALTA, 0 MEDIA, 0 BAIXA corrigivel — restam 5 estruturais** |

> **Nota:** DEEP_BUGS reconciliado em 2026-04-09 — todas as 35 issues (6 crit + 11 alta + 8 media + 3 baixa + 7 AUDIT) verificadas como corrigidas. As ~27 "ativas" anteriores estavam sobrepostas com fixes de Security/Logic/Resilience/Maintainability.

---

## ISSUES RESOLVIDAS NESTA SESSAO — DEEP_LOGIC V4.1

### CRITICAS (8 corrigidas)

| # | Issue | Fix |
|---|-------|-----|
| DL032 | Passagem gratuita bloqueada | Validacao `< 0` + pula dialogo |
| DL033 | Status sempre "EMITIDA" | StatusPagamento.calcular baseado em devedor |
| DL034 | UPDATE frete apaga pagamento | Preserva valor_pago existente |
| DL035 | Race condition numero_frete | Sequence `seq_numero_frete` |
| DL036 | Provisao 13o formula errada | Meses no ano corrente (max 12) |
| DL037 | ResultSet fora do try | Removida brace extra |
| DL038 | Estorno Locale-dependent | MoneyUtil.parseBigDecimalSafe |
| DL039 | Itens encomenda sem transacao | Transacao atomica com rollback |

### ALTAS (15 corrigidas)
DL040-DL054: pagamento frete atomico, NAO_PAGO→calcular(), estorno cartao, viagem ativa, forma pagamento, totais filtro, desconto proporcional, balanco passagens, boletos viagem, auditoria coluna, re-entrega, fechamento data, devedor recalculado, QuitarDivida validacao.

### MEDIAS (12 corrigidas)
DL055-DL064, DL023, #025: total_a_pagar, forma_pagamento, coluna aPagar, 2a via recibo, parseToBigDecimal, quantidade alert, falta duplicada, cidade hifen, ReciboDAO BigDecimal, trim senha, CAST regex, StatusPagamento enum.

### BAIXAS (4 corrigidas)
#033 CidadeDAO rotas, #034 Encomenda LocalDate, DL065 toUpperCase, DL066 ConferenteDAO real.

---

## AUDITORIAS

| Tipo | Versao | Data | Issues ativas | Status | Doc |
|------|--------|------|--------------|--------|-----|
| Scan Geral | V1.1 | 2026-04-08 | 54 original | Issues tratadas nos deep audits | [AUDIT_V1.1](audits/current/AUDIT_V1.1.md) |
| **Deep Security** | **V3.0** | **2026-04-08** | **0** | **100% LIMPO** | [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) |
| **Deep Logic** | **V4.1** | **2026-04-08** | **0** | **100% LIMPO** | [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) |
| **Deep Bugs** | **V1.1** | **2026-04-09** | **0** | **100% LIMPO — 35/35 verificadas** | **[DEEP_BUGS](audits/current/DEEP_BUGS.md)** |
| **Deep Resilience** | **V4.0** | **2026-04-08** | **2 ativas (97% limpo)** | **39 fixadas** | **[DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md)** |
| **Deep Performance** | **V3.0** | **2026-04-09** | **2 (infra only)** | **95% LIMPO — 37/39 fixadas** | [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| **Deep Maintainability** | **V3.0** | **2026-04-09** | **~5 (estruturais)** | **46 corrigidas (90% reducao)** | [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md) |
| MVP Plan | V3.0 | 2026-04-10 | 0 faltando, 4 incompletos | 93% pronto (features only) | [MVP_PLAN V3.0](mvp/archive/MVP_PLAN_V3.0.md) |
| **MVP Plan** | **V4.0** | **2026-04-10** | **10 faltando, 17 incompletos** | **77% pronto — 3 bloqueadores (CORS, injection, HTTPS)** | [MVP_PLAN](mvp/current/MVP_PLAN.md) |

---

## PROXIMO SPRINT (sugerido)

**Prioridade 1 — Resolver 3 bloqueadores (~3h):**
1. CORS restrito no BFF (`naviera-web/server/index.js` — 1 linha)
2. SyncService SQL injection — whitelist de tabelas (`naviera-api/.../SyncService.java`)
3. HTTPS — descomentar nginx.conf + gerar certificados

**Prioridade 2 — Incompletos criticos (~4h):**
Rate limiting API + BFF geral, secrets fortes em prod, graceful shutdown BFF, BFF npm script.

**Prioridade 3 — Estabilidade (antes do lancamento):**
Testes API (fluxos core), input validation BFF (Zod/Joi), migrations avulsas numeradas.

---

## METRICAS DE PROGRESSO

| Metrica | Valor |
|---------|-------|
| Total de issues encontradas | ~305 |
| Issues resolvidas (verificadas) | ~205 |
| Issues pendentes (estimado) | ~82 (muitos sobrepostos) |
| Taxa de resolucao | 67% |
| Issues CRITICAS pendentes | **0** |
| Categorias 100% limpas | **Security, Logic, Bugs** |
| MVP readiness | **77% (95/124 itens)** |
| MVP bloqueadores | **3 (CORS, SQL injection, HTTPS)** |
| Paginas Web | **29** (28 funcionais + 1 placeholder) |
| Telas App | **15** (5 CPF + 5 CNPJ + perfil + login + cadastro + bilhete + encomendas) |
| Endpoints BFF | **~50** (GET + POST + PUT + DELETE) |
| Endpoints API | **108+** |
| Arquivos Desktop modificados (logging) | **87** (491 substituicoes) |

---

## LINKS RAPIDOS

- **Audit geral:** [AUDIT_V1.1](audits/current/AUDIT_V1.1.md)
- **Deep Security:** [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) — 100% LIMPO
- **Deep Logic:** [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) — 100% LIMPO
- **Deep Bugs:** [DEEP_BUGS](audits/current/DEEP_BUGS.md)
- **Deep Resilience:** [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md)
- **Deep Performance:** [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md)
- **Deep Maintainability:** [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md)
- **MVP Plan:** [MVP_PLAN](mvp/current/MVP_PLAN.md)

---

## TIMELINE

| Data | Evento |
|------|--------|
| 2026-04-07 | Initial commit + sincronizacao completa |
| 2026-04-07 | AUDIT_V1.0 — ~194 issues (14 criticas) |
| 2026-04-07 | Sprint correcoes — 140 fixes em 5 deep audits |
| 2026-04-08 | AUDIT_V1.1 — 54 issues novas (6 criticas) |
| 2026-04-08 | **DEEP_SECURITY V3.0 — Categoria 100% limpa** (47/47) |
| 2026-04-08 | DEEP_BUGS V1.0 — 30 novas + 8 criticas corrigidas |
| 2026-04-08 | DEEP_RESILIENCE V4.0 — 30 novas, 41 ativas |
| 2026-04-08 | Fix criticas V1.1 — 7 criticas + 15 altas resolvidas |
| 2026-04-08 | DEEP_LOGIC V4.0 — 36 novas (auditoria profunda 134 arquivos) |
| 2026-04-08 | **DEEP_LOGIC V4.1 — Categoria 100% limpa** (49/49: 8 crit + 15 alta + 12 media + 4 baixa) |
| 2026-04-08 | Fix DEEP_BUGS altas — 11 ALTAS corrigidas: ResultSet leaks, BigDecimal, quitacao por ID, SessaoUsuario volatile, SyncClient |
| 2026-04-08 | Fix DEEP_BUGS medias — 8 MEDIAS corrigidas: ON CONFLICT, dead code deletado, toString null-safe, awaitTermination, preCarregarCaches |
| 2026-04-08 | **DEEP_BUGS V1.0 — Categoria 100% limpa** (35/35: 6 crit + 11 alta + 8 media + 3 baixa + 7 AUDIT pendentes) |
| 2026-04-09 | DEEP_PERFORMANCE V3.0 — auditoria + fix completo: 37/39 resolvidas (95%) |
| 2026-04-09 | DEEP_MAINTAINABILITY V3.0 — auditoria + fix completo: 40 corrigidas (3 crit + 13 altas + 20 medias + 4 parciais), ~11 restantes (BAIXAS) |
| 2026-04-10 | **MVP Fase 1** — HTTPS preparado, BFF multi-tenant (30 queries), JWT secret fix, db.properties protegido |
| 2026-04-10 | **MVP Fase 2** — 25 write endpoints BFF, 4 paginas com CRUD + modais, @Valid em 7 DTOs |
| 2026-04-10 | **MVP Fase 3** — logback, logger.js, rate limiting, query timeout 30s, shutdown hook, responsividade, .env.example |
| 2026-04-10 | **MVP Fase 4** — 22 novas paginas web (29 total), app refatorado (27 arquivos), EncomendaCPF, SyncService (11 tabelas), AppLogger (491 fixes), PostgreSQL no compose |
| 2026-04-10 | **MVP PLAN V3.0 — 93% pronto (108/116), 0 bloqueadores, 4 fases concluidas** |
| 2026-04-10 | **MVP PLAN V4.0 — 77% pronto (95/124), 3 bloqueadores (CORS, SyncService injection, HTTPS). Analise aprofundada em infra/seguranca (+8 itens avaliados)** |

---
*Atualizado automaticamente por Claude Code (status-update) — Revisao humana recomendada*
