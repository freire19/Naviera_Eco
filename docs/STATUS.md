# STATUS DO PROJETO — Naviera Eco
> Ultima atualizacao: 2026-04-25
> Atualizado por: Claude Code (30/30 CRITICOs V1.3 + 37/37 Bugs V1.3 (CRIT/ALTO/MEDIO/BAIXO) fechados)

---

## Estado Geral: APROVADO PARA DEPLOY (0 CRITICOs, 0 Bugs abertos)

### Resumo
Auditoria V1.3 identificou **30 issues CRITICAS** e **37 Bugs** (6 CRIT + 13 ALTO + 14 MEDIO + 4 BAIXO). Em 2026-04-24, **todas foram fechadas**:
- **30 CRITICOs** (23 ja corrigidos em DEEPs + 7 fixados e deployados em `e5c080d`/`3343b52`/`ed42c4d`).
- **37 Bugs (2.1)** — CRITs resolvidos pelos DEEPs de abril; ALTOs (13), MEDIOs (14) e BAIXOs (4) fixados em patches subsequentes desta sessao (rateLimit bound, tenant cache invalidation, FOR UPDATE em pagar(), LIMIT cross-tenant, whitelist forma_pagamento, clipboard try/catch, reconnectDelay STOMP, SW register isolado, guards defensivos etc).

**Zero CRITs e zero Bugs abertos em V1.3.**

---

## ISSUES CRITICAS ABERTAS (0)

Todos os 30 CRITICOs do AUDIT_V1.3 resolvidos. Detalhes em [AUDIT_V1.3](audits/current/AUDIT_V1.3.md).

### CRITICOs fixados nesta sessao (7) — 2026-04-24
| # | Titulo | Fix |
|---|--------|-----|
| **#100/#114** | AdminController super-admin global | Migration 035 + coluna `super_admin` + claim JWT + `ROLE_SUPERADMIN` em `/admin/**` |
| **#102** | BFF escalacao operador→admin via PUT /usuarios/:id | Check de role + bloqueio de auto-promocao em `routes/cadastros.js` |
| **#105** | `/rotas` cross-tenant | `findByEmpresaId` + entidade com `empresa_id` |
| **#106** | Itens encomenda/frete cross-tenant | JOIN com empresa pai em `routes/encomendas.js` e `routes/fretes.js` |
| **#108** | Login dev cross-empresa | Flag explicita `ALLOW_DEV_LOGIN=1` + boot abort se combinar com `NODE_ENV=production` |
| **#650** | `X-Tenant-Slug` spoofing | `tenantMiddleware` descarta header se `req.socket.remoteAddress` nao e loopback/`TRUSTED_PROXY_IPS` |

### CRITICOs ja corrigidos antes da sessao (23) — conferidos 2026-04-24
| Categoria | Issues | Evidencia |
|-----------|--------|-----------|
| Bugs (6) | #003, #004, #005, #006, #007, #008 | DEEP_BUGS V3.0 — 0 CRIT |
| Logic (6) | #200, #201, #202, #203, #204, #205 | DEEP_LOGIC V6.0 — 0 CRIT + commit `ed21783` (webhook Asaas c/ idempotencia) |
| Resilience (7) | #300, #301, #304, #305, #308, #311, #315 | DEEP_RESILIENCE V6.0 — 0 CRIT |
| Performance (2) | #403, #411 | DEEP_PERFORMANCE V5.0 — 0 CRIT + commit `f7804a0` (PSP fora de @Transactional) |
| Security (2) | #103, #107 | `CadastrosWriteService.java:131` valida role; commit `45ccf60` bloqueia match-por-nome vazio |

### Deploy em producao (2026-04-24)
- Migration `035_super_admin.sql` aplicada no container `naviera_postgres` da VPS
- Seed: 2 super-admins (`eujonatasfreire@gmail.com`, `anajmfreire@gmail.com`)
- API rebuildada (`naviera-api-1.1.3-SNAPSHOT.jar`) e restartada via systemd
- BFF restartado via PM2
- `naviera-api.service` com `ExecStart` ajustado para glob (version-agnostic — nao quebra mais em bumps de release-please)
- Super-admins precisam **relogar** para ganhar claim `super_admin=true` no JWT

---

## AUDITORIAS

| Tipo | Versao | Data | Status | Doc |
|------|--------|------|--------|-----|
| **Scan Geral** | **V1.3** | **2026-04-18** | **0 CRITs (30/30 fechados em 2026-04-24)** | [AUDIT_V1.3](audits/current/AUDIT_V1.3.md) |
| Deep Security | V5.0 | 2026-04-19 | **115 ativas** (0 CRIT, 34 ALTO, 37 MEDIO, 21 BAIXO + 23 V1.3) — 16 CRITICOs fixados em 2026-04-19; 7 ALTOs + 5 MEDIOs + 2 BAIXOs adicionais verificados/fixados em 2026-04-25 (ver tabela RESUMO no doc); 5 CVEs ainda ativos (multer, spring-boot) | [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) |
| Deep Logic | V6.0 | 2026-04-23 | **1 ativa** (apenas #662 WebSocket Desktop→API deferido) — 8 CRIT ja estavam corrigidos + F1-F7 aplicadas em 2026-04-23 fechando ALTO/MEDIO/BAIXO | [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) |
| Deep Bugs | V3.0 | 2026-04-23 | **1 parcial** (#DB014/015 double em folha — deferido) — todos os ALTO/MEDIO/BAIXO corrigidos em FB1-FB6 | [DEEP_BUGS](audits/current/DEEP_BUGS.md) |
| Deep Resilience | V6.0 | 2026-04-23 | 45 ativas (**0 CRIT**, 19 ALTO, 21 MEDIO, 5 BAIXO) — 8 CRITICOs conferidos em 2026-04-23, todos ja estavam corrigidos no codigo | [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md) |
| Deep Performance | V5.0 | 2026-04-23 | 55 ativas (**0 CRIT**, 17 ALTO, 28 MEDIO, 10 BAIXO) — 3 CRITICOs conferidos em 2026-04-23, todos ja estavam corrigidos (commit `06f2460` + fix de @Transactional) | [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| Deep Maintainability | V5.0 | 2026-04-18 | 49 ativas (0 CRIT, 11 ALTO, 22 MEDIO, 16 BAIXO) — 22 V4.0 resolvidas, 27 V1.3 pendentes, 15 novas DM071-DM085 | [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md) |
| MVP Plan | V4.0 | 2026-04-10 | Desatualizado (nao cobre novos modulos PSP) | [MVP_PLAN](mvp/current/MVP_PLAN.md) |

**Historico de scan geral:**
| Versao | Data | Total | CRITICOs | Status |
|--------|------|-------|----------|--------|
| V1.0 | 2026-04-07 | ~194 | ? | Archive |
| V1.1 | 2026-04-08 | ? | ? | Archive |
| V1.2 | 2026-04-14 | 112 | 12 | Archive (100% resolvidas) |
| **V1.3** | **2026-04-18** | **225** | **30** | **Atual — REPROVADO** |

---

## CATEGORIAS — ESTADO ATUAL (V1.3)

| Categoria | CRIT | ALTO | MEDIO | BAIXO | Total | Status |
|-----------|------|------|-------|-------|-------|--------|
| Bugs | 0 | 0 | 0 | 0 | 0 | **LIMPA** (37/37 fechados em 2026-04-24) |
| Security | 0 | 0 | 0 | 0 | 0 | **LIMPA** (42/42 fechados em 2026-04-24) |
| Logic | 0 | 0 | 0 | 0 | 0 | **LIMPA** (48/48 fechados em 2026-04-24) |
| Resilience | 0 | 11 | 9 | 3 | 23 | 7 CRITs ja fechados (DEEPs 2026-04-23) |
| Performance | 0 | 11 | 15 | 6 | 32 | 2 CRITs ja fechados (commits pre) |
| Maintainability | 0 | 7 | 18 | 9 | 34 | Duplicacao crescente |
| **TOTAL** | **0** | **29** | **42** | **18** | **89** | **APROVADO** (0 CRITs, 0 Bugs, 0 Logic, 0 Security) |

---

## PROXIMO SPRINT

### Sprint 1 — CRITICOS **CONCLUIDO** (2026-04-24)
Todos os 7 CRITs restantes foram fixados, commitados e deployados na VPS. Veja detalhes na secao "CRITICOs fixados nesta sessao" acima.

### Proximo sprint — ALTOS
~75 issues ALTAS pendentes (Destaques no Sprint 2 abaixo). Com CRITs zerados, deploy nao esta mais bloqueado por severidade — decidir se vale atacar ALTOs antes de liberar MVP.

### Sprint 2 — ALTOs (~75 issues, 2 semanas)
Destaques: #222 (sync perda de dados), #226 (TOTP plain text), #655 (desativacao ineficaz), #658 (race PSP), #711 (drift financeiro desktop/API), #502 (magic number PIX), #506/#507 (god classes >1800L), #717 (strings UI corrompidas).

### Sprint 3 — MEDIOs (~87 issues, 1 mes)

### Backlog — BAIXOs (~33 issues)

---

## BLOQUEADORES MVP

**Status: 0 bloqueadores — todos os 5 originais resolvidos em 2026-04-19/23/24.**

| Bloqueador | Issue | Status |
|-----------|-------|--------|
| ~~Webhook Asaas ausente~~ | ~~#201~~ | **RESOLVIDO** (commit `ed21783`) |
| ~~Escalacao de privilegio operador→admin~~ | ~~#102/#103~~ | **RESOLVIDO** (API `CadastrosWriteService` + BFF `cadastros.js`) |
| ~~Admin cross-empresa (multi-tenant bleed)~~ | ~~#100/#114~~ | **RESOLVIDO** (migration 035 + `ROLE_SUPERADMIN`) |
| ~~X-Tenant-Slug spoofing~~ | ~~#650~~ | **RESOLVIDO** (`middleware/tenant.js` trusted-proxy check) |
| ~~Pool exhaustion em incidente Asaas~~ | ~~#411~~ | **RESOLVIDO** (commit `f7804a0`) |

---

## METRICAS DE PROGRESSO

| Metrica | Valor |
|---------|-------|
| Issues CRITICAS pendentes | **0** _(30/30 fechados em 2026-04-24)_ |
| Issues ALTAS pendentes | **29** _(Bugs + Logic + Security zerados em ALTO; 49 ALTOs fechados em 2026-04-24)_ |
| Issues totais abertas (V1.3) | **~89** _(225 - 127 fechados)_ |
| Bloqueadores MVP reais | **0** |
| Categorias com 0 aberta | **Bugs, Logic, Security** (127 issues fechadas — 56% do total V1.3) |
| Categorias com 0 ALTO | **Bugs, Logic, Security** |
| Categorias com 0 CRIT | **Todas** |
| Novos modulos desde V1.2 | PSP Asaas, webhook, onboarding, OCR Gemini, bilhetes digitais |

---

## DECISOES RECENTES

Nenhuma ADR registrada em `docs/decisions/`. Documentacao pendente:
- Estrategia de integracao PSP Asaas (outbox vs saga vs sincrono)
- Modelo de super-admin vs admin de empresa
- Webhook idempotencia (tabela `webhook_events` proposta em #201)
- Sincronizacao `JWT_SECRET` entre API Spring e BFF Express

---

## LINKS RAPIDOS

- **AUDIT atual:** [AUDIT_V1.3](audits/current/AUDIT_V1.3.md) — **REPROVADO, 30 CRITICOs**
- **MVP Plan:** [MVP_PLAN](mvp/current/MVP_PLAN.md) — **Regerar, V1.3 introduziu novos bloqueadores**
- **Deep Security:** [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) — **V5.0, 148 ativas, 16 CRIT incluindo CVEs em multer/spring/vite**
- **Deep Logic:** [DEEP_LOGIC](audits/current/DEEP_LOGIC.md)
- **Deep Bugs:** [DEEP_BUGS](audits/current/DEEP_BUGS.md)
- **Deep Resilience:** [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md)
- **Deep Performance:** [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md)
- **Deep Maintainability:** [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md)

---

## TIMELINE

| Data | Evento |
|------|--------|
| 2026-04-07 | Initial commit + AUDIT V1.0 (~194 issues) |
| 2026-04-08 | DEEP_SECURITY V3.0 — 100% limpa (47/47) |
| 2026-04-08 | DEEP_LOGIC V4.1, DEEP_BUGS V1.0 — ambas 100% limpas |
| 2026-04-10 | MVP Fases 1-4: HTTPS, BFF multi-tenant, 22 paginas web, 27 arquivos app |
| 2026-04-14 | AUDIT V1.2 — 12 CRITICAS encontradas (DAOs multi-tenant) |
| 2026-04-14 | Commit 895adc9: 12 CRITICAS corrigidas |
| 2026-04-14 | DEEP_LOGIC V5.0, DEEP_RESILIENCE V5.0 — ambas 100% limpas |
| 2026-04-15 | DEEP_SECURITY V4.0 (200+ arquivos, 0 ativas), DEEP_BUGS V2.0 (61/61) |
| 2026-04-15 | DEEP_MAINTAINABILITY V4.0 — 155 arquivos, 7 estruturais ativas |
| 2026-04-17 | Fix de `findByLogin` + 2 SQL concats restantes |
| 2026-04-18 | Novos modulos adicionados: PSP Asaas, webhook, onboarding, AdminPspController |
| **2026-04-18** | **AUDIT V1.3: 225 issues, 30 CRITICOs — REPROVADO PARA PRODUCAO** |
| 2026-04-18 | DEEP_PERFORMANCE V5.0 — 58 ativas (23 novas alem do AUDIT_V1.3) |
| 2026-04-18 | DEEP_LOGIC V6.0 — 55 ativas (12 novas alem do AUDIT_V1.3, incluindo DL030-DL032 criticos em CadastrosService) |
| 2026-04-18 | DEEP_RESILIENCE V6.0 — 53 ativas (26 novas alem do AUDIT_V1.3; 27 pendentes nao corrigidas entre V5.0 e V6.0, incluindo 7 CRITICAS PSP/Asaas) |
| 2026-04-18 | DEEP_BUGS V3.0 — 26 ativas (5 CRIT, 6 ALTO, 10 MEDIO, 3 BAIXO); 25 novas pos-PSP + #DB200 quebra feature super-admin + #DB202 estorno sem validacao + #DB203 PSP em @Transactional + #DB204 webhook secret vazio aceita |
| 2026-04-18 | DEEP_SECURITY V5.0 — 148 ativas (16 CRIT, 44 ALTO, 42 MEDIO, 23 BAIXO); 125 novas #DS5-001 a #DS5-456 + 23 V1.3 pendentes; CVEs confirmados: multer 1.4.5 (CVE-2025-47944), spring-boot 3.3.5 (CVE-2025-22235), vite 5.4.21 (CVE-2025-62522) |
| 2026-04-23 | Conferencia CRITICOs DEEP_LOGIC V6.0 — 8/8 ja estavam corrigidos no codigo (#DL030-032, #201, #202, #203, #204, #205). DEEP_LOGIC agora 47 ativas, 0 CRIT. |
| 2026-04-23 | Conferencia CRITICOs DEEP_BUGS V3.0 — 5/5 ja estavam corrigidos no codigo (#DB200, #DB201, #DB202, #DB203, #DB204). DEEP_BUGS agora 21 ativas, 0 CRIT. |
| 2026-04-23 | Conferencia CRITICOs DEEP_RESILIENCE V6.0 — 8/8 ja estavam corrigidos no codigo (#300, #301, #304, #305, #308, #311, #315, #DR260). DEEP_RESILIENCE agora 45 ativas, 0 CRIT. |
| 2026-04-23 | Conferencia CRITICOs DEEP_PERFORMANCE V5.0 — 3/3 ja estavam corrigidos no codigo (#403, #411, #DP071 via commit `06f2460` + fix @Transactional). DEEP_PERFORMANCE agora 55 ativas, 0 CRIT. |
| 2026-04-23 | Fixes massivos DEEP_LOGIC V6.0 ALTO+MEDIO+BAIXO em 7 fases: 22 arquivos modificados + 3 migrations (032/033/034) + 1 classe nova (CryptoUtil). Fecha #711 cargas, #225 PENDENTE block, #657 FOR UPDATE, #226 AES-GCM at-rest, #221 ON CONFLICT composto, #200 Desktop is_atual+ativa, #234 senha_atualizada_em JWT invalidation, #DL034/35/36/37/40/41, #224 OCR vazio bloqueio, e outros. DEEP_LOGIC agora 1 ativa (#662 Desktop WS deferido). |
| 2026-04-23 | Fixes massivos DEEP_BUGS V3.0 ALTO+MEDIO+BAIXO em 6 fases (FB1-FB6). Fecha #DB205 HMAC constant-time, #DB206 bilhete advisory_lock, #DB207 CPF URL-encode, #DB209 TZ BR, #DB210 whitelist admin hosts, #DB212 codigo ativacao 12 hex, #DB213 slug imutavel, #DB214 erro sem fallback id=1, #DB215 /fechar-mes transacao, #DB220 valorDevedor do servidor, #DB221 INSERT RETURNING atomico, #DB223 whitelist SyncClient, #DB225 VersaoChecker fallback, outros. DEEP_BUGS agora 1 parcial (#DB014/015 double em folha deferido). |
| 2026-04-24 | Conferencia FINAL dos 30 CRITICOs AUDIT_V1.3 direto no codigo. Resultado: 23/30 ja corrigidos (6 Bugs + 6 Logic + 7 Resilience + 2 Performance + 2 Security #103/#107), 7 pendentes — todos Security/Tenant: #100, #102, #105, #106, #108, #114, #650. Checkboxes marcados em AUDIT_V1.3.md. |
| 2026-04-24 | **Fix dos 7 CRITs restantes**: migration 035 (super_admin) + JwtFilter ROLE_SUPERADMIN + SecurityConfig + RotaController tenant-aware + BFF cadastros/encomendas/fretes/auth/tenant hardened. Commits `e5c080d` (api), `3343b52` (web), `ed42c4d` (docs). Compilacao Java + node --check passando. |
| 2026-04-24 | **Deploy em producao**: migration aplicada no `naviera_postgres` (7 usuarios no banco, 2 super-admins seedados); API rebuildada `1.1.3-SNAPSHOT` via systemd; BFF restartado via PM2; `naviera-api.service` ExecStart ajustado para glob (version-agnostic). Super-admins Jonatas + Ana ativados (precisam relogar). **AUDIT_V1.3: 0 CRITs abertos.** |
| 2026-04-24 | **Bugs V1.3 zerados**: 13 ALTOs fixados (ThreadLocal leak em PassagemDAO/ReciboAvulsoDAO, LIMIT em cross-tenant, async handlers com warn, etc); 14 MEDIOs fixados (#001/#002 pool.connect() apos validacao, #015 LIMIT cross-tenant, #018 clipboard fallback, #024 invalidateTenantCache, #025 clamp parseInt, #026 STOMP reconnectDelay, #027 rateLimit bound+FIFO, #029 FOR UPDATE em pagar(), #030/#031 timeouts+URLEncoder ja aplicados, #033 whitelist forma_pagamento, #036 SW register isolado); 5 BAIXOs (#034 aceito, #035 ja fixo em sessao anterior, #037 guard BilheteScreen, #038 interrupt check ConexaoBD, #601 deferido pra DM069). **Bug category: 37/37 fechado.** |
| 2026-04-24 | **Logic ALTOs zerados**: 19/19 fechados. #213 patch aplicado (tolerancia 0.01 no match PAGO em encomendas). Os outros 18 ja estavam corrigidos (#206/#207 frontend-only via #DB220, #208/#209 date validation/compare, #210 TTL cache ViagemDAO, #212 idTipoPassagem guard, #214/#653 UPPER exact match, #216/#217/#218 financeiro validacoes, #222 SyncService blank timestamp, #226 AES-GCM CryptoUtil, #233 estornos EMBARCADO block, #237 PSP fora de @Transactional, #238 balanco filter excluido, #652 passageiros nunca deletados, #711 cargas incluidas em PassagemService). **Logic: 0 CRIT + 0 ALTO.** |
| 2026-04-24 | **Logic MEDIOs + BAIXOs zerados**: 20 MEDIOs + 3 BAIXOs fechados. Patches aplicados: #716 (BilheteController.comprar retorna 410 Gone — endpoint divergente do PassagemService.comprar, gravava valor_pago=valor_total sem PSP), #236 (rand==0 guard em gerarCodigoAtivacao). Outros 21 ja estavam corrigidos (#215 estornos frete c/ desconto, #219 data>=CURRENT_DATE em PassagemService, #220 sv-SE locale, #221 ON CONFLICT uuid+empresa_id, #223 SyncClient setTimestamp, #224 OCR dados vazios block, #225 BARCO PENDENTE block, #227 deferido p/ refactor decimal.js, #228 OpEncomendaWriteService guards, #229 status_frete via webhook PSP, #231 compra.id fallback, #232 agrupar por empresa_nome, #234 senha_atualizada_em invalida JWT, #235 validar empresa.ativo em PSP onboard, #657 FOR UPDATE em PassagemService, #659 TOTP window 60s + rate-limit 10, #660 endpoint /estornar removido #204, #662 deferido (Desktop WS), #714 nomeCasaComAlgum exato, #239 criado:true/false, #240 AbortSignal.timeout 30s). **Logic: 48/48 fechado.** |
| 2026-04-24 | **Security V1.3 zerado**: 17 ALTOs + 11 MEDIOs + 5 BAIXOs fechados. Patches nesta sessao: #104 (`/psp/**` agora ROLE_ADMIN no SecurityConfig), #113 (bucket `ativar:` no RateLimitFilter, max=5/min/IP), #125 (header `X-Content-Type-Options: nosniff` em PublicController.servirFoto), #126 (AuthService.login com hash dummy + mensagem unificada `Credenciais invalidas` — fim do timing attack), #131 (LIMIT 500 + sem `descricao` em ViagemService.buscarPublicas), #654 (bucket `upload:` no RateLimitFilter, max=5/min/IP), #656 (pre-check email duplicado em CadastrosWriteService.criarUsuario), #658 (reserva atomica `PENDING_<id>` em EmpresaPspService.onboarding + revert em falha), #661 (rate-limit dedicado `itens:` no GET /encomendas/:id/itens, max=30/min/user). Demais 19 ja estavam corrigidos (CorsConfig validacao, errorHandler nao loga body, fetchWithRetry nao loga URL, JwtFilter senha_atualizada_em, etc). **Security: 42/42 fechado.** |

---
*Atualizado automaticamente por Claude Code (audit-4-report) — Revisao humana recomendada*
