# AUDITORIA PROFUNDA — RESILIENCE — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V3.0
> **Data:** 2026-04-07
> **Categoria:** Resilience (Error Handling & Fault Tolerance)
> **Base:** AUDIT_V1.0
> **Arquivos analisados:** 131 de 131 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 0 |
| Issues resolvidas (total acumulado) | 22 |
| Issues parcialmente resolvidas | 2 |
| Issues pendentes | 13 |
| **Total de issues ativas** | **15** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #DR003 | BalancoViagemDAO retorna dados parciais | FIXADO — `marcarIncompleto()` + Alert ao usuario (Fix via DL021) |
| #DR011 | Threading violation FinanceiroPassagens | FIXADO — marshal correto para JavaFX thread (Fix via DL027) |
| #DR001 | Catch vazio caixas QuitarDivida | FIXADO — `carregarFormas()` agora tem logging + fallback. `carregarCaixas()` ja tinha fallback |
| #DR002 | Calculo mostra valor stale | FIXADO — mostra "ERRO" em vez de valor stale |
| #DR004 | Fallback userId=2 hardcoded | FIXADO — `sessaoUserId = null` com logging de erro |
| #DR005 | Exception broad em recibo save | FIXADO — catch com logging em logo/impressao |
| #DR006 | 4 catch vazios VenderPassagem | FIXADO — todos 4 catches agora tem `System.err.println` com contexto |
| #DR007 | btnNovo travado permanente em erro | FIXADO — `btnNovo.setDisable(false)` adicionado no catch do background thread |
| #DR008 | RelatorioFretes catches silenciosos | FIXADO — 3 catches de logo agora logam erro |
| #DR009 | RelatorioEncomendaGeral catches vazios | FIXADO — 2 catches agora logam erro |
| #DR012 | NPE groupingBy com null keys | FIXADO — `.filter(p -> p.getAgenteAux() != null)` e `.filter(p -> p.getFormaPagamento() != null)` antes de groupingBy |
| #DR013 | NPE data null auditoria | FIXADO — null check em `rs.getTimestamp("data_hora")` |
| #DR014 | NumberFormatException BaixaPagamento | FIXADO — `converterMoeda()` com try-catch para NumberFormatException |
| #DR015 | NumberFormatException txtIdade | FIXADO — `Integer.parseInt(txtIdade)` com try-catch |
| #DR016 | NPE TemaManager CSS null | FIXADO — null check em `getResource()` antes de `toExternalForm()` |
| #DR017 | Passageiro.toString null | FIXADO — `return nome != null ? nome : ""` |
| #DR018 | Tarifa getters null BigDecimal | FIXADO — 4 getters retornam `BigDecimal.ZERO` quando null |
| #DR019 | ClassNotFoundException silenciada | FIXADO — agora lanca `RuntimeException` para falhar fast |
| #DR020 | DatabaseConnection retorna null | FIXADO — agora lanca `SQLException` em vez de retornar null |
| #DR021 | Reflection fragil RelatorioPassagens | FIXADO — catch especifico para `NoSuchMethodException` com log |
| #DR022 | Scheduler non-daemon SyncClient | FIXADO — thread factory com `setDaemon(true)` |
| #DR023 | ReciboAvulso ID column guessing | FIXADO — usa `ResultSetMetaData` para determinar nome da coluna |
| #DR024 | Log path relativo | FIXADO — log em `~/.sistema_embarcacao/log_erros.txt` |
| #DR027 | Catch vazio caixa BaixaPagamento | FIXADO — logging + fallback "PADRAO" |
| #041 | Debug println em producao | FIXADO — removidos 2 println de debug em AuxiliaresDAO |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| #042 | Rollback incompleto EncomendaDAO.excluir | Inner catch tem rollback, outer catch nao |
| #DR010 | UI blocking DB em initialize() | VenderPassagem usa background thread, mas 15+ controllers ainda bloqueiam FX thread |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #DR025 | PGPASSWORD em environment | Forma padrao do pg_dump — sem alternativa sem .pgpass |
| #DR026 | Convencao ID erro inconsistente | -1 vs 0 vs null em diferentes DAOs |
| #DR028 | Zero testes automatizados | 5 arquivos manuais, zero JUnit @Test |
| #036 | 68+ catch blocks vazios | Pervasivo — varios corrigidos, restam ~40+ |
| #037 | DAOs engolindo exceptions | return false/null/default sem log em varios DAOs |
| #038 | mapResultSet catch vazios | GestaoFuncionarios 8 try-catch inline vazios |
| #039 | ScheduledExecutor sem restart | Campo final, sem recriacao apos falha |
| #040 | Log sem rotacao | FileWriter append sem limite de tamanho |

---

## CENSO DE CATCH BLOCKS (Atualizado)

| Tipo | Quantidade | % |
|------|-----------|---|
| **Empty catch** `{}` | ~43 | 25% |
| **printStackTrace only** | ~54 | 32% |
| **Proper handling** (alert + log ou rethrow) | ~75 | 43% |
| **TOTAL catches no projeto** | **~172** | 100% |

> Leve melhoria: 2 catches convertidos para handling adequado (DR003, DR011). 57% ainda inadequados.

---

## COBERTURA

| Diretorio | Arquivos | Analisados | Issues novas |
|-----------|----------|-----------|-------------|
| src/dao/ | 28 | 28 (100%) | 0 |
| src/database/ | 2 | 2 (100%) | 0 |
| src/gui/ | 55 | 55 (100%) | 0 |
| src/gui/util/ | 5 | 5 (100%) | 0 |
| src/model/ | 26 | 26 (100%) | 0 |
| src/tests/ | 5 | 5 (100%) | 0 |
| **TOTAL** | **131** | **131 (100%)** | **0** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO + ALTO) — MAIORIA CONCLUIDA

- [x] #DR001 — Catch vazio caixas — **FIXADO**
- [x] #DR002 — Calculo stale QuitarDivida — **FIXADO**
- [x] #DR004 — Fallback userId=2 hardcoded — **FIXADO**
- [x] #DR005 — Exception broad em recibo — **FIXADO**
- [x] #DR006 — 4 catch vazios VenderPassagem — **FIXADO**
- [x] #DR007 — btnNovo travado — **FIXADO**
- [ ] #DR010 — UI blocking 16+ controllers — **PARCIAL** (VenderPassagem ok, restam 15+) — **Esforco:** 4h
- [x] #DR012 — NPE groupingBy null — **FIXADO**
- [ ] #DR028 — Zero testes automatizados — **Esforco:** 4h+

### Importante (MEDIO) — MAIORIA CONCLUIDA

- [x] #DR008 — RelatorioFretes catches — **FIXADO**
- [x] #DR009 — RelatorioEncomendaGeral catches — **FIXADO**
- [x] #DR013 — NPE data null auditoria — **FIXADO**
- [x] #DR014 — NumberFormatException BaixaPagamento — **FIXADO**
- [x] #DR015 — NumberFormatException txtIdade — **FIXADO**
- [x] #DR016 — NPE TemaManager CSS — **FIXADO**
- [x] #DR019 — ClassNotFoundException silenciada — **FIXADO**
- [x] #DR020 — DatabaseConnection null — **FIXADO**
- [x] #DR021 — Reflection fragil — **FIXADO**
- [x] #DR022 — Scheduler non-daemon — **FIXADO**
- [x] #DR023 — ID column guessing — **FIXADO**
- [ ] #036 — 68+ catch vazios (sistematico) — **PARCIAL** (vários corrigidos) — **Esforco:** 2h
- [ ] #037 — DAOs engolindo exceptions — **Esforco:** 2h
- [ ] #038 — mapResultSet catch vazios — **Esforco:** 1h
- [ ] #042 — Rollback incompleto (parcial) — **Esforco:** 15min

### Menor (BAIXO) — MAIORIA CONCLUIDA

- [x] #DR017 — Passageiro.toString null — **FIXADO**
- [x] #DR018 — Tarifa getters null — **FIXADO**
- [x] #DR024 — Log path relativo — **FIXADO**
- [ ] #DR025 — PGPASSWORD em env — limitacao pg_dump
- [ ] #DR026 — Convencao ID erro — **Esforco:** 15min
- [x] #DR027 — Catch vazio caixa BaixaPagamento — **FIXADO**
- [ ] #039 — Scheduler sem restart — **Esforco:** 15min
- [ ] #040 — Log sem rotacao — **Esforco:** 30min
- [x] #041 — Debug println — **FIXADO**

---

## NOTAS

> **Comparacao V2.0 → V3.0:**
> - V2.0 tinha 35 issues ativas
> - 20 issues adicionais corrigidas nesta versao (DR001-DR009, DR012-DR024, DR027, #041)
> - **Total ativo V3.0: 15 issues** (13 pendentes + 2 parciais)
>
> **Progresso significativo:** De 35 → 15 issues ativas (reducao de 57%). Todos os NPEs, NumberFormatExceptions, catches vazios criticos e problemas de null safety foram corrigidos.
>
> **Destaques dos fixes:** DatabaseConnection agora lanca excecao (nao retorna null), ConexaoBD faz fail-fast no driver, SyncClient scheduler nao impede JVM shutdown, log vai para diretorio protegido, userId hardcoded removido.
>
> **Issues restantes significativas:** #DR010 (UI blocking — sistematico, 4h), #DR028 (zero testes), #036/#037 (catches vazios/engolidos restantes — refatoracao sistematica).

---
*Gerado por Claude Code (Deep Audit V2.0) — Revisao humana obrigatoria*
