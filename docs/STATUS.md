# STATUS DO PROJETO — SistemaEmbarcacaoProjeto_Novo
> Ultima atualizacao: 2026-04-07 (2a revisao)
> Atualizado por: Claude Code (status-update)

---

## Estado Geral: EM PROGRESSO

### Resumo
Projeto com ~194 issues identificadas em 6 auditorias (scan geral + 5 deep audits). **73 issues corrigidas** e verificadas no codigo. Restam **4 issues CRITICAS pendentes** — todas em autenticacao legada (#014-#017). 8 fixes de performance aplicados (N+1 eliminados, indices criados, LIMIT adicionado). Novos utilitarios centralizados: AlertHelper, MoneyUtil, OcrAudioService, StatusPagamento enum. Dead code removido (3 DAOs + 1 modelo + metodos mortos).

---

## ISSUES CRITICAS ABERTAS

| # | Issue | Severidade | Status | Fonte | Arquivo |
|---|-------|-----------|--------|-------|---------|
| #014 | Senha BD hardcoded "123456" (3 arquivos) | CRITICO | Pendente | [AUDIT](audits/current/AUDIT_V1.0.md) | `ConexaoBD.java:23`, `DatabaseConnection.java:10` |
| #015 | Senha diferente hardcoded `5904` | CRITICO | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | `CadastroClienteController:75` |
| #016 | Login compara texto plano com hash via SQL | CRITICO | Pendente | [AUDIT](audits/current/AUDIT_V1.0.md) | `LoginController:77-83` |
| #017 | Fallback texto plano em estornos | CRITICO | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) | `EstornoPagamentoController:114` |

### Issues ALTAS pendentes (amostra)

| # | Issue | Status | Fonte |
|---|-------|--------|-------|
| #019 | SQL injection via tabela/coluna (AuxiliaresDAO, TarifaDAO) | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) |
| #022 | Token exibido em Alert | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) |
| #023 | URL producao em HTTP (sem TLS) | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) |
| #065 | Auth plaintext fallback FinanceiroSaidaController | Pendente | [SECURITY](audits/current/DEEP_SECURITY.md) |
| #027 | double para dinheiro (6 models) | Pendente | [LOGIC](audits/current/DEEP_LOGIC.md) |
| #028 | double para dinheiro (controllers) | Pendente | [LOGIC](audits/current/DEEP_LOGIC.md) |
| #045 | Sem connection pooling | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| DP001 | 5 conexoes por insert passagem | Pendente | [PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| DM005 | Autocomplete reimplementado 5+ vezes | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) |
| DM006 | AuxiliaresDAO 35 metodos duplicados | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) |
| DM007 | SQL direto em 11+ controllers | Pendente | [MAINT](audits/current/DEEP_MAINTAINABILITY.md) |
| DR010 | UI blocking DB em 15+ controllers | Parcial | [RESILIENCE](audits/current/DEEP_RESILIENCE.md) |
| DR028 | Zero testes automatizados | Pendente | [RESILIENCE](audits/current/DEEP_RESILIENCE.md) |

---

## ISSUES RESOLVIDAS RECENTEMENTE

### Security (13 fixados)

| # | Issue | Verificado |
|---|-------|-----------|
| D001 | SQL injection FinanceiroSaidaController | Codigo verificado — parametrizado |
| D006 | Senha String no model Usuario | Codigo verificado — senhaPlana transient |
| D009 | Nenhuma tela verificava permissoes | Codigo verificado — PermissaoService implementado |
| D011 | Audit trail deletavel | Codigo verificado — DELETE removido |
| D014 | Salario buscado por LIKE no nome | Codigo verificado — funcionario_id |
| D002 | SQL injection data CadastroBoletoController | Codigo verificado — parametrizado |
| D005 | Timing side-channel em verificacao de senha | Codigo verificado — BCrypt.checkpw() equalizado |
| D007 | Senha sem hash em CadastroUsuarioController | Codigo verificado — hashSenha() aplicado |
| D010 | Controllers financeiros sem autorizacao | Codigo verificado — PermissaoService |
| D012 | Delecao boletos sem auth nem audit | Codigo verificado — exigirAdmin() + auditoria |
| D013 | RH sem autorizacao | Codigo verificado — isAdmin() |
| D015 | Valores negativos aceitos em pagamentos | Codigo verificado — validacao |
| #021 | Hash logado em stderr (UsuarioDAO) | **NOVO FIX** — verificarSenha() sem logging de hash |

### Logic (15 fixados)

| # | Issue | Verificado |
|---|-------|-----------|
| DL001/DL002 | Race condition bilhete/encomenda (MAX+1) | Sequences implementadas |
| DL005 | Viagem excluir sem cascade | Transacao com deletes filhos |
| DL009 | Desconto pode exceder total | Validacao no confirmar |
| DL010/DL011/DL012 | Pagamento parcial/frete sem forma/desconto | Acumula desconto, grava forma |
| DL015 | getSaldoDevedor negativo | .max(BigDecimal.ZERO) |
| DL017 | Parser quebra com >= R$1.000 | Remove milhar antes de converter |
| DL019 | Tabela precos nao persiste | DAO chamado corretamente |
| DL020 | Coluna logo nome errado | path_logo (correto) |
| DL027 | JavaFX UI de thread background | Task.setOnSucceeded() |
| DL028 | BCrypt via SQL (auth gerente) | BCrypt.checkpw() |
| DL029 | Ternario dead code estorno frete | PENDENTE:NAO_PAGO |

### Resilience (22 fixados)

| # | Issue | Verificado |
|---|-------|-----------|
| DR001-DR009 | Catches vazios, fallback userId=2, btnNovo travado, RelatorioFretes/EncomendaGeral | Todos corrigidos com logging/fallback |
| DR012-DR016 | NPE groupingBy null, data null, NumberFormatException (2), TemaManager CSS | Null checks e try-catch |
| DR017-DR024 | Passageiro.toString, Tarifa getters, ClassNotFound, DB null, Reflection, Scheduler, ReciboAvulso, Log path | Corrigidos com fail-fast/daemon/path protegido |
| DR027, #041 | Catch vazio BaixaPgto, println debug | Logging + removido |

### Performance (8 fixados)

| # | Issue | Verificado |
|---|-------|-----------|
| DP004 | N+1 filtro encomendas (cada keystroke) | **Codigo verificado** — Cache `Map<Long, List<EncomendaItem>>` pre-carregado. 1 query em vez de N |
| DP005 | 30+ queries por calendario | **Codigo verificado** — `buscarAnotacoesPorMes()` retorna Map. 1 query |
| DP007 | EXTRACT previne uso de indice | **Codigo verificado** — Range queries `>= ? AND < ?` |
| DP008 | 16 indices criticos ausentes | **Codigo verificado** — Script `006_criar_indices_performance.sql` com 18 indices |
| DP011 | listarTodos sem LIMIT | **Codigo verificado** — LIMIT 500 em PassagemDAO e EncomendaDAO |
| DP015 | Pixel-by-pixel image copy (138k iteracoes) | **Codigo verificado** — `SwingFXUtils.fromFXImage()` |
| DP019 | Threads sem daemon flag | **Codigo verificado** — `setDaemon(true)` em 4 threads |
| DP022 | NumberFormat criado dentro de loop | **Codigo verificado** — Movido para fora do loop |

### Maintainability (15 fixados)

| # | Issue | Verificado |
|---|-------|-----------|
| DM001 | showAlert() copiado em 26 controllers | **Codigo verificado** — `AlertHelper.java` criado |
| DM002 | parseBigDecimal() duplicado em 4 controllers | **Codigo verificado** — `MoneyUtil.java` criado |
| DM003 | OCR/Audio code copiado (~200 linhas) | **Codigo verificado** — `OcrAudioService.java` criado |
| DM008 | 5 DAOs/metodos mortos | **Codigo verificado** — ClienteDAO, RemetenteDAO, PassagemAuxDAO deletados |
| DM009 | 4 modelos/metodos mortos | **Codigo verificado** — LinhaDespesaBalanco deletado |
| DM010 | VenderPassagem 2 metodos mortos | Deletados |
| DM011 | 76 magic strings de status | **Codigo verificado** — `StatusPagamento` enum |
| DM012 | Magic number 0.01 em 42 locais | `StatusPagamento.TOLERANCIA_PAGAMENTO` |
| DM013 | empresaDAO.buscarPorId(1) hardcoded | `EmpresaDAO.ID_EMPRESA_PRINCIPAL` |
| DM016 | printStackTrace em 18 DAOs (69 ocorrencias) | **Codigo verificado** — System.err.println com contexto |
| DM018 | CaixaDAO "alterar" em vez de "atualizar" | Parcial — metodo renomeado, log interno ainda diz "alterar" |
| DM019 | EmbarcacaoDAO mapping duplicado 3x | **Codigo verificado** — `mapResultSet()` extraido |
| DM026 | listarContatos duplicados nunca chamados | Deletados |
| DM027 | PassageiroDAO new AuxiliaresDAO por row | **Codigo verificado** — campo `final` |
| DM028 | RotaDAO wrapper getConnection() | **Codigo verificado** — removido |

**Total resolvido verificado: 73 issues**

---

## AUDITORIAS

| Tipo | Versao | Data | Issues ativas | Status | Doc |
|------|--------|------|--------------|--------|-----|
| Scan Geral | V1.0 | 2026-04-07 | base | Referencia | [AUDIT_V1.0](audits/current/AUDIT_V1.0.md) |
| Deep Security | V2.0 | 2026-04-07 | 28 | 13 fixados (+1 novo) | [DEEP_SECURITY](audits/current/DEEP_SECURITY.md) |
| Deep Logic | V3.0 | 2026-04-07 | 19 | 15 fixados + 5 parciais | [DEEP_LOGIC](audits/current/DEEP_LOGIC.md) |
| Deep Resilience | V3.0 | 2026-04-07 | 15 | 22 fixados + 2 parciais | [DEEP_RESILIENCE](audits/current/DEEP_RESILIENCE.md) |
| Deep Performance | V2.0 | 2026-04-07 | 23 | 8 fixados | [DEEP_PERFORMANCE](audits/current/DEEP_PERFORMANCE.md) |
| Deep Maintainability | V2.0 | 2026-04-07 | 31 | 15 fixados | [DEEP_MAINTAINABILITY](audits/current/DEEP_MAINTAINABILITY.md) |
| MVP Plan | — | — | — | Nao realizado | — |

---

## SPRINT ATUAL

Issues parciais em progresso:

- [ ] #001 — NPE datas nullable — **Status:** Parcial
- [ ] #018 — Admin validation (migrou para FinanceiroSaidaController, fallback persiste) — **Status:** Parcial
- [ ] DL003 — Quitacao sem transacao — **Status:** Parcial (estorno ok, salvar pendente)
- [ ] DL016 — ILIKE wildcard — **Status:** Parcial (QuitarDivida ok, ExtratoCliente pendente)
- [ ] DL024 — Viagem data passado — **Status:** Parcial (chegada>=partida ok, passado nao valida)
- [ ] DL025 — Parse moeda fragil — **Status:** Parcial (try/catch ok, retorna 0.0 silencioso)
- [ ] #029 — Encomenda inserir sem transacao — **Status:** Parcial (excluir ok, inserir pendente)
- [ ] DR010 — UI blocking — **Status:** Parcial (VenderPassagem ok, 15+ controllers pendentes)
- [ ] #042 — Rollback incompleto EncomendaDAO — **Status:** Parcial (inner ok, outer pendente)

**Progresso:** 0 de 9 concluidos

---

## PROXIMO SPRINT (sugerido)

Prioridade 1 — Autenticacao (bloqueia deploy):

- [ ] #016 — Login texto plano vs hash — **Severidade:** CRITICO
- [ ] #017 — Fallback texto plano estornos — **Severidade:** CRITICO
- [ ] #014 — Senha BD hardcoded — **Severidade:** CRITICO
- [ ] #015 — Senha diferente hardcoded — **Severidade:** CRITICO
- [ ] #065 — Auth plaintext fallback FinanceiroSaida — **Severidade:** ALTO

Prioridade 2 — Integridade financeira:

- [ ] #027 — double para dinheiro models — **Severidade:** ALTO
- [ ] #028 — double para dinheiro controllers — **Severidade:** ALTO

Prioridade 3 — Performance restante (maior impacto):

- [ ] DP001/DP002 — Cache tabelas auxiliares (elimina 5-6 conexoes/operacao) — **Severidade:** ALTO
- [ ] #045 — Connection pooling (HikariCP) — **Severidade:** ALTO
- [ ] DP006 — CAST previne indice — **Severidade:** ALTO

---

## METRICAS DE PROGRESSO

| Metrica | Valor |
|---------|-------|
| Total de issues encontradas (historico) | ~194 |
| Issues removidas (falso positivo) | 2 |
| Issues resolvidas (verificadas) | 73 |
| Issues parciais | 9 |
| Issues pendentes | ~110 |
| Taxa de resolucao | 38% |
| Issues CRITICAS pendentes | 4 |
| Issues CRITICAS resolvidas | ~23 |
| MVP bloqueadores restantes | N/A (MVP Plan nao gerado) |

### Progresso por categoria

| Categoria | Total | Resolvidas | Ativas | % Resolvido |
|-----------|-------|-----------|--------|-------------|
| Security | 41 | 13 | 28 | 32% |
| Logic | 39 | 15 | 19 | 38% |
| Resilience | 37 | 22 | 15 | 59% |
| Performance | 31 | 8 | 23 | 26% |
| Maintainability | 46 | 15 | 31 | 33% |

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
| 2026-04-07 | DEEP_SECURITY V2.0 — 28 novos + 12 D-series corrigidos |
| 2026-04-07 | DEEP_LOGIC V3.0 — 15 issues corrigidas (race conditions, parser, cascade) |
| 2026-04-07 | DEEP_RESILIENCE V3.0 — 22 issues corrigidas (NPEs, catches, threading) |
| 2026-04-07 | DEEP_PERFORMANCE V2.0 — 8 fixes aplicados (N+1, indices, LIMIT, SwingFXUtils) |
| 2026-04-07 | DEEP_MAINTAINABILITY V2.0 — 15 fixes (StatusPagamento, AlertHelper, OcrAudioService, dead code) |
| 2026-04-07 | Status V2 recompilado — 73 fixes verificados no codigo, 4 criticas restantes |

---
*Atualizado automaticamente — Revisao humana recomendada*
