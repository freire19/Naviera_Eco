# AUDITORIA PROFUNDA — RESILIENCE — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V1.0
> **Data:** 2026-04-07
> **Categoria:** Resilience (Error Handling & Fault Tolerance)
> **Base:** AUDIT_V1.0
> **Arquivos analisados:** 131 de 131 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 30 |
| Issues anteriores resolvidas | 0 |
| Issues anteriores parcialmente resolvidas | 0 |
| Issues anteriores pendentes | 7 |
| **Total de issues ativas** | **37** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

Nenhuma.

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #036 | 68+ catch blocks vazios em controllers | Confirmado: pervasivo em todo o projeto |
| #037 | DAOs engolindo exceptions com return default | Confirmado: padrao sistematico |
| #038 | mapResultSet com catch vazios para colunas opcionais | Confirmado: ViagemDAO, PassagemDAO, EncomendaDAO |
| #039 | ScheduledExecutorService nao reinicia apos shutdown | Confirmado: campo final, sem recriacao |
| #040 | Log sem rotacao | Confirmado: log_erros.txt cresce indefinido |
| #041 | Debug println em producao | Confirmado: AuxiliaresDAO:304,314 |
| #042 | Rollback incompleto em EncomendaDAO.excluir | Confirmado: gap entre stmt1 e stmt2 |

---

## NOVOS PROBLEMAS

### Catch Blocks Vazios e Silenciamento de Erros

#### Issue #DR001 — QuitarDividaEncomendaTotal: catch vazio esconde falha ao carregar caixas
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/QuitarDividaEncomendaTotalController.java`
- **Linha(s):** 71
- **Problema:** Se DB falhar ao carregar caixas, ComboBox fica vazia sem feedback. Usuario pode confirmar pagamento com caixa nula.
- **Impacto:** Pagamento gravado sem caixa. Dados financeiros inconsistentes.
- **Codigo problematico:**
```java
} catch(Exception e) {}
```
- **Fix sugerido:** Logar erro + desabilitar botao Confirmar quando caixas nao carregam.
- **Observacoes:**
> _carregarFormas() tem fallback para "DINHEIRO","PIX" mas carregarCaixas() nao tem._

---

#### Issue #DR002 — QuitarDividaEncomendaTotal: catch vazio em calcular() mostra total stale
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/QuitarDividaEncomendaTotalController.java`
- **Linha(s):** 51
- **Problema:** Se calculo falhar, label mostra valor anterior. Usuario confirma com total errado exibido.
- **Impacto:** Pagamento baseado em valor incorreto exibido.
- **Codigo problematico:**
```java
private void calcular() {
    try { /* ... */ } catch (Exception e) {}  // label fica com valor stale
}
```
- **Fix sugerido:** No catch, setar `lblTotalFinal.setText("ERRO")` ou resetar para original.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR003 — BalancoViagemDAO retorna dados financeiros parciais sem indicacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 46-48, 67-69, 88-90
- **Problema:** Cada secao (passagens, encomendas, fretes, saidas) tem catch que imprime erro e continua. Se encomendas falhar, balanco mostra zero encomendas parecendo valido.
- **Impacto:** Relatorio financeiro incompleto apresentado como correto. Decisoes baseadas em dados errados.
- **Fix sugerido:** Propagar excecao ou adicionar flag `hasErrors()` em DadosBalancoViagem.
- **Observacoes:**
> _Overlap com DL021 (logica) — aqui foco e na resiliencia (erro silenciado)._

---

#### Issue #DR004 — PassagemDAO.inserir: fallback silencioso para userId=2
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 63
- **Problema:** Se SessaoUsuario falhar, passagem e atribuida silenciosamente ao usuario ID 2 (hardcoded).
- **Impacto:** Audit trail corrompido — todas as passagens de erro aparecem como do usuario #2.
- **Codigo problematico:**
```java
Integer sessaoUserId = 2;
try { if(SessaoUsuario.isUsuarioLogado()) sessaoUserId = SessaoUsuario.getUsuarioLogado().getId(); } catch(Exception e){}
```
- **Fix sugerido:** Logar quando fallback e usado. Ou falhar a operacao se sessao invalida.
- **Observacoes:**
> _Usuario #2 pode nao existir no banco, causando FK violation adicional._

---

#### Issue #DR005 — ReciboQuitacaoPassageiroDAO: Exception broad engole NPE em save financeiro
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ReciboQuitacaoPassageiroDAO.java`
- **Linha(s):** 26, 53
- **Problema:** Catch `Exception` (nao `SQLException`) engole NPE se `getDataPagamento()` retornar null. Recibo financeiro silenciosamente nao salvo.
- **Impacto:** Pagamento processado mas recibo nunca gravado. Inconsistencia financeira.
- **Fix sugerido:** Catch `SQLException` especifico. Deixar NPE propagar.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR006 — VenderPassagemController: 4 catch vazios em carregamento inicial
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 271, 303, 370, 428
- **Problema:** Falhas silenciosas ao carregar rotas (271), viagens (303), datas (370) e listener de viagem (428). Combos ficam vazios sem explicacao.
- **Impacto:** Tela de venda de passagens inutilizavel se BD falhar, sem feedback.
- **Fix sugerido:** Alertas contextuais ou retry com indicador de loading.
- **Observacoes:**
> _Se carregamento de rotas falha, usuario nao consegue vender mas nao sabe por que._

---

#### Issue #DR007 — VenderPassagemController: btnNovo fica desabilitado permanentemente em erro
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 259, 329-332
- **Problema:** `btnNovo.setDisable(true)` antes do background load. Se load falhar, catch re-habilita `rootPane` mas nao `btnNovo`.
- **Impacto:** Botao "Novo" travado permanentemente apos falha. Usuario nao pode vender passagens.
- **Codigo problematico:**
```java
btnNovo.setDisable(true);
// ... background load ...
} catch (Exception e) {
    e.printStackTrace();
    Platform.runLater(() -> { if(rootPane!=null) rootPane.setDisable(false); });
    // btnNovo NUNCA re-habilitado
}
```
- **Fix sugerido:** Adicionar `Platform.runLater(() -> btnNovo.setDisable(false))` no catch.
- **Observacoes:**
> _Bug reproduzivel: desligar BD, abrir tela de vendas, btnNovo fica cinza para sempre._

---

#### Issue #DR008 — RelatorioFretesController: 5 catches com apenas printStackTrace
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RelatorioFretesController.java`
- **Linha(s):** 146, 158, 208, 232, 284, 320
- **Problema:** Falhas em carregamento de dados de relatorio sem feedback ao usuario. Tabelas ficam vazias.
- **Impacto:** Relatorio financeiro aparece vazio como se nao houvesse dados.
- **Fix sugerido:** Alert contextual ou banner "Erro ao carregar dados".
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR009 — RelatorioEncomendaGeralController: 3 catches vazios/println
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RelatorioEncomendaGeralController.java`
- **Linha(s):** 141, 152, 574
- **Problema:** Viagens/rotas nao carregam silenciosamente. Nome de empresa usa fallback "SISTEMA DE ENCOMENDAS" sem indicar erro.
- **Impacto:** Relatorios com dados filtrados incompletamente e header errado.
- **Fix sugerido:** Feedback visual quando combos falham ao carregar.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### UI Thread Blocking (DB no JavaFX Thread)

#### Issue #DR010 — UI blocking sistematico: DB calls em initialize() de 8+ controllers
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** QuitarDividaEncomendaTotalController, RotasController, RelatorioPassagensController, RegistrarPagamentoEncomendaController, BalancoViagemController, TelaPrincipalController, FinanceiroEncomendasController, RelatorioEncomendaGeralController
- **Linha(s):** Todos os initialize() com ConexaoBD.getConnection()
- **Problema:** Queries SQL executadas diretamente no JavaFX Application Thread durante initialize(). Bloqueia UI ate query completar.
- **Impacto:** UI congela por 1-5s em cada abertura de tela. Se BD lento ou offline, congela indefinidamente (timeout JDBC).
- **Fix sugerido:** Usar `javafx.concurrent.Task` para DB calls, atualizar UI em `onSucceeded`.
- **Observacoes:**
> _TelaPrincipalController:566-574 faz 4 roundtrips de DB no FX thread em atualizarDashboard(). Chamado de initialize(), handleCarregarDados(), e ao fechar modais._

---

#### Issue #DR011 — FinanceiroPassagensController: UI update de background thread (threading violation)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroPassagensController.java`
- **Linha(s):** 219-229
- **Problema:** `Task.call()` modifica `tabela.setItems()` e `lblTotal.setText()` diretamente do background thread. Viola regra JavaFX.
- **Impacto:** IllegalStateException intermitente, corrupcao visual, freezes aleatorios.
- **Fix sugerido:** Retornar dados do `call()`, aplicar na UI em `onSucceeded`.
- **Observacoes:**
> _Overlap com DL027 (logica) — aqui foco e na instabilidade runtime._

---

### Null Safety e Type Safety

#### Issue #DR012 — NPE em groupingBy com chaves null (RelatorioPassagensController)
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/RelatorioPassagensController.java`
- **Linha(s):** 294-298, 322-326
- **Problema:** `Collectors.groupingBy(Passagem::getAgenteAux)` e `Passagem::getFormaPagamento` — se campo for null, `groupingBy` lanca NPE.
- **Impacto:** Grafico de vendas por agente/forma de pagamento crasha se qualquer passagem tiver campo null.
- **Codigo problematico:**
```java
Map<String, BigDecimal> vendasPorAgente = dados.stream()
    .collect(Collectors.groupingBy(Passagem::getAgenteAux, ...));
```
- **Fix sugerido:**
```java
.collect(Collectors.groupingBy(
    p -> p.getAgenteAux() != null ? p.getAgenteAux() : "N/A", ...))
```
- **Observacoes:**
> _getFormaPagamento() tem fallback "PENDENTE" (seguro), mas getAgenteAux() nao._

---

#### Issue #DR013 — NPE em AuditoriaExclusoesSaida ao formatar data null
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/AuditoriaExclusoesSaida.java`
- **Linha(s):** 210
- **Problema:** `sdf.format(rs.getTimestamp("data_hora"))` — se campo NULL, getTimestamp retorna null, format lanca NPE.
- **Impacto:** Tela de auditoria crasha ao encontrar registro sem data.
- **Fix sugerido:** `rs.getTimestamp("data_hora") != null ? sdf.format(...) : "--"`
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR014 — NumberFormatException nao tratada em BaixaPagamentoController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/BaixaPagamentoController.java`
- **Linha(s):** 83-86
- **Problema:** `Double.parseDouble(texto)` sem try-catch. Texto nao-numerico causa crash.
- **Impacto:** Tela de pagamento fecha com erro se usuario digitar letras.
- **Fix sugerido:** Wrap em try-catch retornando 0.0.
- **Observacoes:**
> _getDesconto() e getValorPago() chamam converterMoeda() que nao protege._

---

#### Issue #DR015 — NumberFormatException em VenderPassagemController.txtIdade
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 720
- **Problema:** `Integer.parseInt(txtIdade.getText())` sem try-catch. Texto nao-numerico causa "Erro inesperado ao preparar a venda".
- **Impacto:** Mensagem de erro confusa para campo de idade invalido.
- **Fix sugerido:** Try-catch com mensagem especifica: "Idade deve ser numero".
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR016 — TemaManager: NPE se CSS resource nao encontrado
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/TemaManager.java`
- **Linha(s):** 27
- **Problema:** `getResource(css).toExternalForm()` — se getResource retorna null, NPE. Scene ja teve stylesheets limpos na linha 23.
- **Impacto:** App fica sem estilo visual. Catch imprime erro mas nao recupera.
- **Fix sugerido:** Null check antes de toExternalForm().
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR017 — Passageiro.toString() retorna null se nome for null
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/model/Passageiro.java`
- **Linha(s):** 49
- **Problema:** `return nome;` — ComboBox chama toString() para exibir. Se null, exibe "null" ou causa NPE em chain.
- **Fix sugerido:** `return nome != null ? nome : "";`
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR018 — Tarifa getters retornam BigDecimal null
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/model/Tarifa.java`
- **Linha(s):** 64-94
- **Problema:** getValorTransporte/Alimentacao/Cargas/Desconto retornam null se nao setados. Caller usa `String.format("%,.2f", tarifa.getValorAlimentacao())` — exibe "null".
- **Fix sugerido:** Retornar `BigDecimal.ZERO` como default.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Infraestrutura e Configuracao

#### Issue #DR019 — ConexaoBD: ClassNotFoundException silenciada no static init
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ConexaoBD.java`
- **Linha(s):** 26-33
- **Problema:** Se driver PostgreSQL nao encontrado, imprime stderr e continua. Toda chamada subsequente falha com "No suitable driver" sem explicar a causa raiz.
- **Impacto:** Erro de driver impresso uma vez, depois centenas de "connection failed" sem contexto.
- **Fix sugerido:** Lancar RuntimeException ou armazenar erro para reportar em getConnection().
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR020 — DatabaseConnection.conectar() retorna null em vez de exception
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/database/DatabaseConnection.java`
- **Linha(s):** 12-28
- **Problema:** Retorna null em falha. Callers que nao checam null recebem NPE em vez de erro significativo.
- **Impacto:** NPE inexplicavel quando BD esta offline.
- **Fix sugerido:** Lancar SQLException como ConexaoBD faz, ou remover classe.
- **Observacoes:**
> _Classe parece ser versao antiga de ConexaoBD. Possivelmente nao usada._

---

#### Issue #DR021 — Reflection para cell value factory fragil
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RelatorioPassagensController.java`
- **Linha(s):** 151-158
- **Problema:** Usa Java reflection (`getMethod("get" + propertyName).invoke()`) para popular colunas de tabela. Se getter renomeado, coluna mostra null silenciosamente.
- **Impacto:** Relatorio com colunas vazias sem indicacao de erro.
- **Fix sugerido:** Substituir reflection por lambdas diretas: `cellData -> cellData.getValue().getValorTotal()`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR022 — SyncClient scheduler nao e daemon thread
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 46
- **Problema:** `Executors.newScheduledThreadPool(1)` cria thread non-daemon. Se app fechar, JVM nao termina.
- **Impacto:** Processo Java fica pendurado apos fechar a aplicacao.
- **Fix sugerido:** Usar ThreadFactory com `setDaemon(true)`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR023 — ReciboAvulsoDAO: ID coluna guessed por exception
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java`
- **Linha(s):** 63-65
- **Problema:** Tenta "id", se falhar tenta "id_recibo", se falhar ID fica 0. Operacoes subsequentes com ID=0 podem deletar/editar registro errado.
- **Impacto:** Integridade de dados comprometida por ID invalido.
- **Fix sugerido:** Determinar nome correto da coluna e usar diretamente.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR024 — LogService: path relativo para log file
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** 30
- **Problema:** `"log_erros.txt"` criado no diretorio de trabalho, que varia conforme lancamento.
- **Impacto:** Log pode ser criado em local inesperado ou inacessivel.
- **Fix sugerido:** Usar `System.getProperty("user.home") + "/SistemaEmbarcacao/logs/"`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR025 — Backup PGPASSWORD em environment de processo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/TelaPrincipalController.java`
- **Linha(s):** 946-948
- **Problema:** Senha do banco colocada em `PGPASSWORD` environment variable, visivel em listagem de processos.
- **Impacto:** Senha exposta em `ps aux` ou `/proc/<pid>/environ`.
- **Fix sugerido:** Usar arquivo `.pgpass` com permissoes restritas.
- **Observacoes:**
> _Atualmente senha e vazia (linha 909), mas padrao e inseguro._

---

### Return Value Ambiguity

#### Issue #DR026 — Convencao de erro inconsistente em geracao de IDs
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** RotaDAO:72, UsuarioDAO:53, ViagemDAO:291
- **Linha(s):** Ver acima
- **Problema:** Em erro de sequence: RotaDAO retorna -1, UsuarioDAO retorna 0, ViagemDAO retorna null. Sem convencao.
- **Impacto:** Callers podem inserir com ID=-1 ou ID=0 sem perceber erro.
- **Fix sugerido:** Padronizar: lancar excecao em todos.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DR027 — BaixaPagamentoController.carregarUsuariosCaixa: catch vazio em caixa
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/BaixaPagamentoController.java`
- **Linha(s):** 115
- **Problema:** `catch (SQLException e) {}` sem fallback ao carregar caixas. ComboBox fica vazia.
- **Impacto:** Pagamento pode ser gravado sem caixa associada.
- **Fix sugerido:** Fallback ou alerta ao usuario.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Testes

#### Issue #DR028 — Zero testes automatizados no projeto
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/tests/`
- **Linha(s):** Diretorio inteiro
- **Problema:** 4 arquivos de "teste": 2 testes manuais de conexao, 1 lanca tela FXML, 1 vazio. Zero assertions. Zero JUnit @Test.
- **Impacto:** Nenhuma protecao contra regressao. Qualquer fix pode quebrar outra coisa.
- **Fix sugerido:** Criar testes para: modelos financeiros, calculos de saldo, transicoes de status.
- **Observacoes:**
> _JUnit 4.12 ja esta no classpath (lib/). Basta usar._

---

## CENSO DE CATCH BLOCKS (Consolidado)

| Tipo | Quantidade | % |
|------|-----------|---|
| **Empty catch** `{}` | ~45 | 26% |
| **printStackTrace only** | ~55 | 32% |
| **Proper handling** (alert + log ou rethrow) | ~72 | 42% |
| **TOTAL catches no projeto** | **~172** | 100% |

> 58% dos catches no projeto sao inadequados (vazios ou apenas printStackTrace).

---

## COBERTURA

| Diretorio | Arquivos | Analisados | Issues novas |
|-----------|----------|-----------|-------------|
| src/dao/ | 28 | 28 (100%) | 7 |
| src/database/ | 2 | 2 (100%) | 2 |
| src/gui/ | 55 | 55 (100%) | 16 |
| src/gui/util/ | 5 | 5 (100%) | 2 |
| src/model/ | 26 | 26 (100%) | 2 |
| src/tests/ | 5 | 5 (100%) | 1 |
| **TOTAL** | **131** | **131 (100%)** | **30** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO + ALTO)

- [ ] #DR001 — Catch vazio em caixas (QuitarDivida) — **Esforco:** 15min
- [ ] #DR002 — Calculo mostra valor stale — **Esforco:** 10min
- [ ] #DR003 — Balanco retorna dados parciais — **Esforco:** 1h
- [ ] #DR004 — Fallback userId=2 hardcoded — **Esforco:** 15min
- [ ] #DR005 — Exception broad em recibo save — **Esforco:** 10min
- [ ] #DR006 — 4 catch vazios em VenderPassagem — **Esforco:** 30min
- [ ] #DR007 — btnNovo travado em erro — **Esforco:** 5min
- [ ] #DR010 — UI blocking em 8+ controllers — **Esforco:** 4h (sistematico)
- [ ] #DR011 — Threading violation em FinanceiroPassagens — **Esforco:** 30min
- [ ] #DR012 — NPE em groupingBy com null keys — **Esforco:** 15min
- [ ] #DR028 — Zero testes automatizados — **Esforco:** 4h+ (criar base)
- **Notas:**
> _DR007 e fix de 1 linha. DR010 e o mais trabalhoso mas com maior impacto em UX._

### Importante (MEDIO)

- [ ] #DR008 — RelatorioFretes catches silenciosos — **Esforco:** 30min
- [ ] #DR009 — RelatorioEncomendaGeral catches vazios — **Esforco:** 30min
- [ ] #DR013 — NPE em data null de auditoria — **Esforco:** 5min
- [ ] #DR014 — NumberFormatException em BaixaPagamento — **Esforco:** 5min
- [ ] #DR015 — NumberFormatException em txtIdade — **Esforco:** 5min
- [ ] #DR016 — NPE em TemaManager CSS null — **Esforco:** 5min
- [ ] #DR019 — ClassNotFoundException silenciada — **Esforco:** 10min
- [ ] #DR020 — DatabaseConnection retorna null — **Esforco:** 10min (ou remover)
- [ ] #DR021 — Reflection fragil em RelatorioPassagens — **Esforco:** 30min
- [ ] #DR022 — Scheduler non-daemon — **Esforco:** 5min
- [ ] #DR023 — ID column guessing em ReciboAvulso — **Esforco:** 10min
- **Notas:**
> _DR013-DR016 sao fixes de 1-2 linhas cada._

### Menor (BAIXO)

- [ ] #DR017 — Passageiro.toString null — **Esforco:** 1min
- [ ] #DR018 — Tarifa getters null — **Esforco:** 5min
- [ ] #DR024 — Log path relativo — **Esforco:** 10min
- [ ] #DR025 — PGPASSWORD em env — **Esforco:** 15min
- [ ] #DR026 — Convencao de ID erro inconsistente — **Esforco:** 15min
- [ ] #DR027 — Catch vazio caixa BaixaPagamento — **Esforco:** 5min
- **Notas:**
> _DR017 e DR018 sao fixes de 1 linha._

---

## NOTAS

> **Padrao dominante: 58% dos catches sao inadequados.** O projeto tem 172 catch blocks. 45 sao completamente vazios, 55 fazem apenas printStackTrace. Apenas 72 (42%) tem tratamento adequado (alert + log ou rethrow). As areas mais afetadas sao controllers financeiros e relatorios.
>
> **UI blocking e sistematico.** Pelo menos 8 controllers fazem queries SQL no JavaFX Application Thread. Isso afeta toda tela que o usuario abre. Com BD local, o delay e de ~100ms (toleravel). Com BD remoto ou lento, a UI congela visivelmente.
>
> **Sem testes = sem rede de seguranca.** JUnit 4.12 esta no classpath mas nunca foi usado. Criar testes para calculos financeiros (getSaldoDevedor, calcularTotais, status transitions) e a primeira defesa contra regressao.
>
> **Fix mais impactante: DR010 (UI blocking).** Migrar DB calls para Tasks com loading indicators transformaria a experiencia do usuario. Pode ser feito incrementalmente, um controller por vez.
>
> **Comparacao com scan:** O audit-scan encontrou 7 issues de resiliencia. O deep audit encontrou 30 adicionais. Total ativo: **37 issues de resiliencia**.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*
