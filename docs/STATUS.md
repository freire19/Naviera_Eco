# STATUS DO PROJETO — SistemaEmbarcacaoProjeto_Novo
> Ultima atualizacao: 2026-04-07 (recompilado)
> Atualizado por: Claude Code (status-update)

---

## Estado Geral: EM PROGRESSO

### Resumo
Projeto com 204 issues identificadas em 6 auditorias. **~50 issues ja corrigidas** (todas CRITICAS de security, logic e resilience). Restam **8 issues criticas pendentes** — principalmente autenticacao legada (#014-#018), double para dinheiro (#027/#028), e N+1 em filtro de encomendas (DP004). Controle de acesso (PermissaoService) implementado. Race conditions corrigidas com sequences. Performance nao iniciada.

---

## ISSUES CRITICAS ABERTAS

| # | Issue | Severidade | Status | Fonte | Arquivo |
|---|-------|-----------|--------|-------|---------|
| #001 | NPE em datas nullable (ReciboAvulsoDAO, PassagemDAO) | CRITICO | Parcial | [AUDIT](audits/current/AUDIT_V1.0.md) | ReciboAvulsoDAO:76, PassagemDAO:173 |
| #014 | Senha BD hardcoded "123456" (3 arquivos) | CRITICO | Pendente | [AUDIT](audits/current/AUDIT_V1.0.md) | `ConexaoBD.java:23` |
| #015 | Senha diferente hardcoded `5904` | CRITICO | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | `CadastroClienteController` |
| #016 | Login compara texto plano com hash via SQL | CRITICO | Pendente | [AUDIT](audits/current/AUDIT_V1.0.md) | `LoginController:77-83` |
| #017 | Fallback texto plano em estornos | CRITICO | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | `EstornoPagamentoController` |
| #018 | Admin validation texto plano | CRITICO | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | `AuditoriaExclusoesSaida` |
| #027 | double para dinheiro (6 models) | CRITICO | Pendente | [LOGIC](audits/current/DEEP_LOGIC.md) | Frete, ReciboAvulso, DadosBalancoViagem |
| #028 | double para dinheiro (controllers) | CRITICO | Pendente | [LOGIC](audits/current/DEEP_LOGIC.md) | BaixaPagamento, Financeiro*, Estorno |
| DP004 | N+1 por encomenda em filtro (cada keystroke) | CRITICO | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | `ListaEncomendaController:496` |

### Issues ALTAS pendentes (amostra — ver docs para lista completa)

| # | Issue | Status | Fonte | Arquivo |
|---|-------|--------|-------|---------|
| #019 | SQL injection via tabela/coluna | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | AuxiliaresDAO, TarifaDAO |
| #045 | Sem connection pooling | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | ConexaoBD.java |
| DL003 | Quitacao sem transacao atomica (salvarPagamento) | Parcial | [LOGIC](audits/current/DEEP_LOGIC.md) | QuitarDividaEncomendaTotal |
| DL016 | ILIKE wildcard em ExtratoClienteEncomenda | Parcial | [LOGIC](audits/current/DEEP_LOGIC.md) | ExtratoClienteEncomendaController |
| DR010 | UI blocking DB em initialize() (15+ controllers) | Parcial | [RESILIENCE](audits/current/DEEP_RESILIENCE.md) | 15+ controllers |
| DR028 | Zero testes automatizados | Pendente | [RESILIENCE](audits/current/DEEP_RESILIENCE.md) | src/tests/ |
| DP001 | 5 conexoes por insert passagem | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | PassagemDAO |
| DP006 | CAST() previne indice em bilhete/encomenda | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | PassagemDAO, EncomendaDAO |
| DP008 | 16 indices criticos ausentes | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | database_scripts/ |
| DP011 | listarTodos() sem LIMIT (4 DAOs) | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) | PassagemDAO, EncomendaDAO, ViagemDAO |
| DM001 | showAlert() copiado em 26 controllers | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) | Todos controllers |
| DM005 | Autocomplete reimplementado 5+ vezes | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) | 5+ controllers |
| DM007 | SQL direto em controllers com DAOs existentes | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) | 11+ locais |
| DM011 | 76 comparacoes status sem enum | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) | 17 arquivos |

---

## ISSUES RESOLVIDAS RECENTEMENTE

| # | Issue | Resolvido em | Verificado |
|---|-------|-------------|-----------|
| #003 | Connection leak ViagemDAO finally | 2026-04-07 | Codigo verificado — conn.close() em finally |
| #004 | Connection leak 4 controllers estorno | 2026-04-07 | Codigo verificado — try-with-resources |
| #006 / DL029 | Ternario dead code PENDENTE:PENDENTE | 2026-04-07 | Codigo verificado — agora PENDENTE:NAO_PAGO |
| #065 / DL028 | Auth gerente via BCrypt SQL | 2026-04-07 | Codigo verificado — BCrypt.checkpw() |
| D001 | SQL injection FinanceiroSaidaController | 2026-04-07 | Codigo verificado — parametrizado |
| D006 | Senha String no model Usuario | 2026-04-07 | Codigo verificado — senhaPlana transient |
| D009 | Nenhuma tela verifica permissoes | 2026-04-07 | Codigo verificado — PermissaoService implementado |
| D011 | Audit trail deletavel | 2026-04-07 | Codigo verificado — DELETE removido |
| D014 | Salario por LIKE no nome | 2026-04-07 | Codigo verificado — funcionario_id |
| DL001 | Race condition bilhete MAX+1 | 2026-04-07 | Codigo verificado — sequence |
| DL002 | Race condition encomenda MAX+1 | 2026-04-07 | Codigo verificado — sequence |
| DL005 | Viagem excluir sem cascade | 2026-04-07 | Codigo verificado — transacao + filhos |
| DL009 | Desconto excede total | 2026-04-07 | Codigo verificado — validacao |
| DL017 | Parser R$1.000 | 2026-04-07 | Codigo verificado — remove milhar |
| DL019 | Tabela precos nao persiste | 2026-04-07 | Codigo verificado — DAO chamado |
| DR001 | Catch vazio caixas QuitarDivida | 2026-04-07 | Codigo verificado — logging + fallback |
| DR002 | Calculo stale QuitarDivida | 2026-04-07 | Codigo verificado — mostra ERRO |
| DR004-DR009 | Catches vazios, fallback hardcoded, NPEs | 2026-04-07 | Codigo verificado |
| DR012-DR024 | NPEs, NumberFormat, threading, scheduler | 2026-04-07 | Codigo verificado |
| D002, D005, D007, D010, D012, D013, D015 | SQL param, BCrypt, auth, audit | 2026-04-07 | Codigo verificado |
| DL010-DL012, DL015, DL020 | Pagamento parcial, frete, saldo, logo | 2026-04-07 | Codigo verificado |

**Total resolvido: ~50 issues** (12 security D-series + 14 logic + 22 resilience + 2 falsos positivos removidos)

---

## AUDITORIAS

| Tipo | Versao | Data | Issues ativas | Status | Doc |
|------|--------|------|--------------|--------|-----|
| Scan Geral | V1.0 | 2026-04-07 | ~30 (muitas corrigidas via deep) | Base | [AUDIT_V1.0](audits/current/AUDIT_V1.0.md) |
| Deep Security | V2.0 | 2026-04-07 | 29 (12 D-series fixados) | Concluido | [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) |
| Deep Logic | V3.0 | 2026-04-07 | 19 (14 fixados + 5 parciais) | Concluido | [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) |
| Deep Resilience | V3.0 | 2026-04-07 | 15 (22 fixados + 2 parciais) | Concluido | [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md) |
| Deep Performance | V1.0 | 2026-04-07 | 31 (0 fixados) | Concluido | [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| Deep Maintainability | V1.0 | 2026-04-07 | 46 (0 fixados) | Concluido | [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md) |
| MVP Plan | — | — | — | Nao realizado | — |

---

## SPRINT ATUAL

Nenhum sprint em andamento. Issues parciais indicam trabalho em progresso:

- [ ] #001 — NPE datas nullable — **Status:** Parcial (ViagemDAO ok, ReciboAvulsoDAO e PassagemDAO pendentes)
- [ ] DL003 — Quitacao sem transacao — **Status:** Parcial (estorno ok, salvar pendente)
- [ ] DL016 — ILIKE wildcard — **Status:** Parcial (QuitarDivida ok, ExtratoCliente pendente)
- [ ] DL018 — Caixa carrega usuarios — **Status:** Parcial (QuitarDivida ok, BaixaPagamento pendente)
- [ ] DR010 — UI blocking — **Status:** Parcial (VenderPassagem ok, 15+ controllers pendentes)

**Progresso:** 0 de 5 concluidos

---

## PROXIMO SPRINT (sugerido)

Prioridade 1 — Autenticacao (corrigir antes de qualquer deploy):

- [ ] #016 — Login texto plano vs hash — **Severidade:** CRITICO
- [ ] #017 — Fallback texto plano estornos — **Severidade:** CRITICO
- [ ] #018 — Admin validation texto plano — **Severidade:** CRITICO
- [ ] #014 — Senha BD hardcoded — **Severidade:** CRITICO
- [ ] #015 — Senha diferente hardcoded — **Severidade:** CRITICO

Prioridade 2 — Integridade financeira:

- [ ] #027 — double para dinheiro models — **Severidade:** CRITICO
- [ ] #028 — double para dinheiro controllers — **Severidade:** CRITICO
- [ ] #001 — NPE datas (completar) — **Severidade:** CRITICO

Prioridade 3 — Performance critica:

- [ ] DP004 — N+1 filtro encomendas — **Severidade:** CRITICO
- [ ] DP008 — 16 indices ausentes — **Severidade:** ALTO
- [ ] #045 — Connection pooling (HikariCP) — **Severidade:** ALTO

---

## METRICAS DE PROGRESSO

| Metrica | Valor |
|---------|-------|
| Total de issues encontradas (historico) | 204 |
| Issues resolvidas | ~50 |
| Issues pendentes | ~154 |
| Taxa de resolucao | ~25% |
| Issues criticas pendentes | 9 |
| Issues criticas resolvidas | 21 |
| MVP bloqueadores restantes | N/A (MVP Plan nao gerado) |

---

## DECISOES RECENTES

Nenhuma ADR registrada. Considere documentar decisoes arquiteturais importantes em `docs/decisions/`.

---

## LINKS RAPIDOS

- **Audit geral:** [AUDIT_V1.0](audits/current/AUDIT_V1.0.md)
- **Deep Security:** [DEEP_SECURITY](audits/current/DEEP_SECURITY.md)
- **Deep Logic:** [DEEP_LOGIC](audits/current/DEEP_LOGIC.md)
- **Deep Resilience:** [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md)
- **Deep Performance:** [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md)
- **Deep Maintainability:** [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md)

---

## TIMELINE

| Data | Evento |
|------|--------|
| 2026-04-07 | Initial commit + sincronizacao completa |
| 2026-04-07 | AUDIT_V1.0 — 64 issues encontradas (14 criticas) |
| 2026-04-07 | DEEP_SECURITY V1.0 → V2.0 — 28 novos, 12 D-series corrigidos |
| 2026-04-07 | DEEP_LOGIC V1.0 → V3.0 — 14 issues corrigidas (race conditions, parser, cascade) |
| 2026-04-07 | DEEP_RESILIENCE V1.0 → V3.0 — 22 issues corrigidas (NPEs, catches, threading) |
| 2026-04-07 | DEEP_PERFORMANCE V1.0 — 31 issues identificadas, 0 corrigidas |
| 2026-04-07 | DEEP_MAINTAINABILITY V1.0 — 46 issues identificadas, 0 corrigidas |
| 2026-04-07 | Status recompilado — 50 fixes verificados no codigo, 9 criticas restantes |

---
*Atualizado automaticamente — Revisao humana recomendada*
