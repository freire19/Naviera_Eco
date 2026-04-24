# STATUS DO PROJETO — Naviera Eco
> Ultima atualizacao: 2026-04-23
> Atualizado por: Claude Code (conferencia CRITICOS DEEP_LOGIC V6.0 — todos ja corrigidos)

---

## Estado Geral: REPROVADO PARA PRODUCAO

### Resumo
Auditoria V1.3 (scan + review) identificou **30 issues CRITICAS** concentradas em novos modulos adicionados apos V1.2: integracao PSP Asaas, webhook, onboarding self-service, ativacao/desativacao de empresas. Status piorou em relacao a V1.2 (0 CRITICOs) porque novos vetores foram introduzidos sem hardening. Desktop continua limpo apos fixes do V1.2. **Nao fazer deploy de producao ate concluir Sprint 1.**

---

## ISSUES CRITICAS ABERTAS (30)

**Bloqueadores de deploy** — detalhes completos em [AUDIT_V1.3](audits/current/AUDIT_V1.3.md).

### Top 5 bloqueadores
1. **#201** — Webhook Asaas NAO existe: pagamentos PIX/boleto ficam eternamente PENDENTE
2. **#411** — PSP chamado dentro de `@Transactional` + HikariCP=10: incidente Asaas derruba API
3. **#650** — `X-Tenant-Slug` aceito do cliente sem validar trusted proxy: bypass multi-tenant
4. **#100/#114** — Admin de qualquer empresa pode modificar/desativar outras via `/admin/empresas`
5. **#107/#105/#106** — Cross-tenant data leak em rotas e pagamentos

### CRITICOs por categoria
| Categoria | CRITICOs | Detalhe |
|-----------|----------|---------|
| Security | 9 | #100, #102, #103, #105, #106, #107, #108, #114, #650 |
| Resiliencia | 7 | #300, #301, #304, #305, #308, #311, #315 |
| Bugs | 6 | #003, #004, #005, #006, #007, #008 |
| Logic | 6 | #200, #201, #202, #203, #204, #205 |
| Performance | 2 | #403, #411 |
| Maintainability | 0 | — |

### Areas concentradoras de risco
- **Tenant isolation** (9 CRITICOs) — queries sem `empresa_id`, escalacao de privilegio, header X-Tenant-Slug confiado
- **PSP Asaas / webhook** (5 CRITICOs) — webhook ausente, fail-open HMAC, HTTP em transacao
- **Pagamento saga** (4 CRITICOs) — estados `PROCESSANDO` orfaos, estorno destrutivo

---

## AUDITORIAS

| Tipo | Versao | Data | Status | Doc |
|------|--------|------|--------|-----|
| **Scan Geral** | **V1.3** | **2026-04-18** | **30 CRITICOs (REPROVADO)** | [AUDIT_V1.3](audits/current/AUDIT_V1.3.md) |
| Deep Security | V5.0 | 2026-04-19 | 132 ativas (0 CRIT, 44 ALTO, 42 MEDIO, 23 BAIXO + 23 V1.3) — **16 CRITICOs fixados em 2026-04-19**; 5 CVEs ainda ativos (multer, spring-boot) | [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) |
| Deep Logic | V6.0 | 2026-04-23 | **1 ativa** (apenas #662 WebSocket Desktop→API deferido) — 8 CRIT ja estavam corrigidos + F1-F7 aplicadas em 2026-04-23 fechando ALTO/MEDIO/BAIXO | [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) |
| Deep Bugs | V3.0 | 2026-04-23 | 21 ativas (**0 CRIT**, 6 ALTO, 10 MEDIO, 3 BAIXO + 1 parcial legado) — 5 CRITICOs conferidos em 2026-04-23, todos ja estavam corrigidos no codigo | [DEEP_BUGS](audits/current/DEEP_BUGS.md) |
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
| Bugs | 6 | 13 | 14 | 4 | 37 | Regressao (novos modulos) |
| Security | 9 | 15 | 14 | 4 | 42 | Regressao critica |
| Logic | 6 | 18 | 17 | 7 | 48 | Regressao critica |
| Resilience | 7 | 11 | 9 | 3 | 30 | Regressao |
| Performance | 2 | 11 | 15 | 6 | 34 | Quase limpa |
| Maintainability | 0 | 7 | 18 | 9 | 34 | Duplicacao crescente |
| **TOTAL** | **30** | **75** | **87** | **33** | **225** | **REPROVADO** |

---

## PROXIMO SPRINT

### Sprint 1 — CRITICOS (bloqueia deploy — fazer AGORA)
30 issues listadas em [AUDIT_V1.3.md secao 4](audits/current/AUDIT_V1.3.md). Prioridade:

1. **P0 Tenant isolation** — #100, #102, #103, #105, #106, #107, #108, #114, #650 (9 issues)
2. **P0 PSP/Webhook** — #201, #112, #301, #305, #411 (5 issues)
3. **P0 Runtime bugs** — #003, #004, #005, #006, #007, #008, #202, #203, #204, #205 (10 issues)
4. **P0 Resiliencia** — #300, #304, #308, #311, #315 (5 issues)
5. **P0 Performance** — #403 (1 issue)

### Sprint 2 — ALTOs (~75 issues, 2 semanas)
Destaques: #222 (sync perda de dados), #226 (TOTP plain text), #655 (desativacao ineficaz), #658 (race PSP), #711 (drift financeiro desktop/API), #502 (magic number PIX), #506/#507 (god classes >1800L), #717 (strings UI corrompidas).

### Sprint 3 — MEDIOs (~87 issues, 1 mes)

### Backlog — BAIXOs (~33 issues)

---

## BLOQUEADORES MVP

**Status: 5 novos bloqueadores adicionados apos V1.2.**

| Bloqueador | Issue | Area |
|-----------|-------|------|
| Webhook Asaas ausente | #201 | PSP |
| Escalacao de privilegio operador→admin | #102/#103 | Auth |
| Admin cross-empresa (multi-tenant bleed) | #100/#114 | Tenant |
| X-Tenant-Slug spoofing | #650 | Tenant |
| Pool exhaustion em incidente Asaas | #411 | Resilience/Perf |

---

## METRICAS DE PROGRESSO

| Metrica | Valor |
|---------|-------|
| Issues CRITICAS pendentes | **30** |
| Issues ALTAS pendentes | **75** |
| Issues totais abertas (V1.3) | **225** |
| Bloqueadores MVP reais | **5** |
| Categorias 100% limpas | **nenhuma** |
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

---
*Atualizado automaticamente por Claude Code (audit-4-report) — Revisao humana recomendada*
