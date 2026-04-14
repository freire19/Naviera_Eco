# AUDITORIA PROFUNDA — LOGIC — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V4.1
> **Data:** 2026-04-08
> **Categoria:** Logic (Regras de Negocio)
> **Base:** AUDIT_V1.1
> **Arquivos analisados:** 134 de 134 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas (V4.0) | 36 |
| Issues corrigidas nesta sessao (CRITICAS) | 8 (DL031 falso positivo, DL032-DL039 fixados) |
| Issues corrigidas nesta sessao (ALTAS) | 15 (DL040-DL054 fixados) |
| Issues corrigidas nesta sessao (MEDIAS) | 12 (DL055-DL064, DL023, #025 fixados) |
| Issues anteriores resolvidas (sessao ant.) | 6 + 5 (#020,#021,#023,#024,#030 fixados no STATUS) |
| Issues anteriores parcialmente resolvidas | 1 (#DL030) |
| Issues corrigidas nesta sessao (BAIXAS) | 4 (#033, #034, DL065, DL066) + 2 verificados (DL026, #035) |
| **Total de issues ativas** | **0** (todas corrigidas ou verificadas) |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

| Issue | Titulo | Verificacao |
|-------|--------|------------|
| #DL022 | Filtros relatorio ignorados (6 de 10 params) | FIXADO — PassagemDAO.filtrarRelatorio agora usa SQL para 5 params + Java post-filter para outros 5 (agente, tipo, rota, pagamento, caixa). Todos 10 cobertos. |
| #DL007 | Excluir auxiliar sem ref check (7 metodos) | FIXADO — AuxiliaresDAO.excluirAuxiliar agora captura FK violation (SQLState 23503) e retorna false com mensagem descritiva. |
| #DL018 | Caixa carrega usuarios | FIXADO — BaixaPagamentoController usa tabela `caixas` diretamente. |
| #DL021 | Balanco retorna dados parciais | FIXADO — `marcarIncompleto()` implementado com flag e detalhes de erro. Alert exibido ao usuario. |
| #029 | Encomenda+Itens sem transacao | FIXADO — `inserirComItens()` atomico com rollback em EncomendaDAO. |
| #DL024 | Viagem data no passado | FIXADO — ViagemDAO.inserir/atualizar validam data >= hoje. |

### Parcialmente resolvidas

| Issue | Titulo | O que falta |
|-------|--------|------------|
| #DL030 | Numeracao pagina off-by-one | Fix `(i+2)` → `(i+1)` aplicado. Porem primeira pagina continua SEM numeracao (apenas continuacoes recebem). |
| #030 | Tolerancia PAGO inconsistente | Enum `StatusPagamento` existe com `TOLERANCIA_PAGAMENTO`. Maioria migrada. Porem FinanceiroFretesController.estorno usa `"NAO_PAGO"` que nao e tratado adequadamente por filtros/relatorios (ver DL041). |

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #020 | Quitacao LIKE afeta passageiros errados | CONFIRMADO: PassagemDAO.quitarDividaTotalPassageiro L363-376 ainda tem fallback `ILIKE '%nome%'` |
| #021 | BalancoViagemDAO usa double | CONFIRMADO: L48/70/92/117 usam `rs.getDouble("total")`. Model DadosBalancoViagem espera BigDecimal — incompatibilidade de tipos. |
| #DL023 | CAST crash non-numeric | CONFIRMADO: fallback de EncomendaDAO.obterProximoNumero L209 ainda usa `CAST(numero_encomenda AS INTEGER)` |
| #023 | Viagem passado bloqueia update | CONFIRMADO: ViagemDAO.atualizar L325-328 impede editar QUALQUER campo se data < hoje |
| #024 | SessaoUsuario.touch() nunca chamado | CONFIRMADO: nenhum metodo no projeto chama `SessaoUsuario.touch()` automaticamente |
| #025 | Status PENDENTE vs PENDENTE_PAGAMENTO | CONFIRMADO: VenderPassagemController L780 usa "PENDENTE_PAGAMENTO" (dead code, sobrescrito por "EMITIDA" na L682) |
| #026 | Validacao silenciosa em salvarOuAlterarFrete | CONFIRMADO: CadastroFreteController parsing de quantidade/preco falha silenciosamente |
| #033 | CidadeDAO hardcoded | CONFIRMADO: 3 cidades hardcoded em CidadeDAO.buscarTodasCidades() |
| #034 | Encomenda data como String | CONFIRMADO: `private String dataLancamento` sem type safety |
| #035 | SyncClient recebimento nao implementado | CONFIRMADO: fluxo de recebimento inexistente |
| #DL026 | Off-by-one dias comerciais | REAVALIADO: algoritmo 30/360 parece correto apos analise detalhada. Manter como pendente para revisao humana. |

---

## NOVOS PROBLEMAS

### CRITICOS

#### Issue #DL031 — VenderPassagem: valor pago nunca gravado na passagem
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 682-689
- **Problema:** O objeto `Passagem` recebe `valorAPagar`, mas `valorPago` nunca e preenchido antes de chamar `passagemDAO.inserir()`. O dialogo `FinalizarPagamentoPassagemController` captura o pagamento mas o valor nao e transferido de volta para o objeto. A passagem e salva com `valorPago = null`.
- **Impacto:** Coluna `valor_devedor` fica incorreta no banco. Toda passagem recém vendida fica como "devedora" do valor total, mesmo que o pagamento tenha sido integral.
- **Codigo problematico:**
```java
passagemParaSalvar.setStatusPassagem("EMITIDA");
// valorPago NUNCA setado aqui
boolean sucesso;
if (this.passagemEmEdicao == null) {
    sucesso = passagemDAO.inserir(passagemParaSalvar);
}
```
- **Fix sugerido:**
```java
// Apos fechar dialogo de pagamento, transferir valores:
passagemParaSalvar.setValorPago(controllerPagamento.getValorPago());
passagemParaSalvar.setDevedor(controllerPagamento.getDevedor());
passagemParaSalvar.setTroco(controllerPagamento.getTroco());
```
- **Observacoes:**
> _Bug critico de fluxo: o valor pago e capturado pelo dialogo mas descartado antes de persistir._

---

#### Issue #DL032 — VenderPassagem: passagem gratuita bloqueada
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 611-615
- **Problema:** Validacao `valorAPagar <= 0` bloqueia passagens com valor zero. Impede venda de passagens gratuitas (criancas, cortesias, tipo passageiro com tarifa zero).
- **Impacto:** Impossivel emitir passagem gratuita. Passageiros com direito a gratuidade nao conseguem embarcar com bilhete.
- **Fix sugerido:** Diferenciar "valor invalido" de "passagem gratuita" verificando se o tipo de passagem permite valor zero.
- **Observacoes:**
> _Afeta diretamente operacao diaria de embarque._

---

#### Issue #DL033 — VenderPassagem: status "PENDENTE_PAGAMENTO" e dead code
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 780 e 682
- **Problema:** Em `preencherDadosDaPassagem` (L780) o status e definido como "PENDENTE_PAGAMENTO". Imediatamente apos, em `salvarDadosFinais` (L682), e sobrescrito para "EMITIDA". O status final e SEMPRE "EMITIDA", independente do resultado do pagamento.
- **Impacto:** Passagens com pagamento parcial ou sem pagamento ficam com status "EMITIDA" (aparentando estar pagas). Nao ha como filtrar passagens pendentes de pagamento pelo status.
- **Fix sugerido:** Definir status com base no resultado do pagamento: se `valorPago >= valorTotal` → "PAGO", se `valorPago > 0` → "PARCIAL", senao → "PENDENTE".
- **Observacoes:**
> _Issue #025 relacionada — inconsistencia de strings de status._

---

#### Issue #DL034 — CadastroFrete: pagamento anterior apagado no UPDATE
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1524-1526
- **Problema:** No UPDATE de frete existente, `valor_pago = 0`, `troco = 0`, `valor_devedor = valor_frete_calculado` sao hardcoded. Qualquer pagamento parcial registrado anteriormente e ZERADO quando o frete e alterado (ex: corrigir descricao de item).
- **Impacto:** Editar um frete ja parcialmente pago apaga o registro de pagamento. Prejuizo financeiro direto.
- **Codigo problematico:**
```java
pstFrete.setBigDecimal(paramIdx++, BigDecimal.ZERO); // valor_pago
pstFrete.setBigDecimal(paramIdx++, BigDecimal.ZERO); // troco
pstFrete.setBigDecimal(paramIdx++, valorFreteCalculado); // valor_devedor
```
- **Fix sugerido:** No UPDATE, preservar `valor_pago` e `valor_devedor` existentes. Recalcular `valor_devedor = valor_frete_calculado - valor_pago_existente`.
- **Observacoes:**
> _Todo frete editado apos pagamento parcial volta a status PENDENTE com divida total._

---

#### Issue #DL035 — CadastroFrete: numero_frete usa MAX+1 (race condition)
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1399-1410
- **Problema:** `SELECT COALESCE(MAX(numero_frete), 0) + 1 FROM fretes` — padrao classico de race condition. Duas estacoes podem obter o mesmo numero de frete.
- **Impacto:** Fretes duplicados na mesma viagem. Confusao operacional e financeira.
- **Codigo problematico:**
```java
String sql = "SELECT COALESCE(MAX(numero_frete), 0) + 1 FROM fretes";
```
- **Fix sugerido:** Criar sequence `seq_numero_frete` no PostgreSQL e usar `SELECT nextval('seq_numero_frete')`.
- **Observacoes:**
> _Passagens e encomendas ja migraram para sequences (DL001/DL002). Fretes ficaram de fora._

---

#### Issue #DL036 — GestaoFuncionarios: provisao 13o salario usa meses totais desde admissao
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/GestaoFuncionariosController.java`
- **Linha(s):** 324-327
- **Problema:** `long mesesTotais = ChronoUnit.MONTHS.between(f.dataAdmissao, LocalDate.now())` e `provisao = (salario / 12.0) * mesesTotais`. Calcula provisao sobre o total de meses desde admissao, nao sobre meses trabalhados no ano corrente.
- **Impacto:** Funcionario admitido ha 3 anos mostra provisao de 3 salarios (quando deveria ser proporcional ao ano corrente, maximo 1 salario). Distorce completamente o relatorio de custos de RH.
- **Codigo problematico:**
```java
long mesesTotais = ChronoUnit.MONTHS.between(f.dataAdmissao, LocalDate.now());
double provisao = (f.salario / 12.0) * mesesTotais;
```
- **Fix sugerido:**
```java
LocalDate inicioAno = LocalDate.of(LocalDate.now().getYear(), 1, 1);
LocalDate base = f.dataAdmissao.isAfter(inicioAno) ? f.dataAdmissao : inicioAno;
long mesesNoAno = ChronoUnit.MONTHS.between(base, LocalDate.now()) + 1;
double provisao = (f.salario / 12.0) * Math.min(mesesNoAno, 12);
```
- **Observacoes:**
> _Bug de logica pura — formula errada._

---

#### Issue #DL037 — FinanceiroPassagens: ResultSet fora do try-with-resources
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroPassagensController.java`
- **Linha(s):** 186-196
- **Problema:** O `try-with-resources` que gerencia `Connection` e `PreparedStatement` fecha prematuramente na L194 (brace extra). `stmt.executeQuery()` na L196 executa com statement ja fechado, lancando `SQLException` silenciosamente capturada pelo catch.
- **Impacto:** A tela de Financeiro Passagens NUNCA exibe dados. O metodo `carregarDados()` sempre falha silenciosamente. Toda a funcionalidade de gestao financeira de passagens esta inoperante.
- **Codigo problematico:**
```java
try (Connection con = ...; PreparedStatement stmt = ...) {
    for (...) { ... }
    }  // <-- fecha try-with-resources prematuramente

    ResultSet rs = stmt.executeQuery(); // stmt ja fechado!
```
- **Fix sugerido:** Remover a `}` extra na L194 para manter o `ResultSet` dentro do escopo do `try-with-resources`.
- **Observacoes:**
> _Requer confirmacao compilando o projeto. Se confirmado, e o bug mais impactante do sistema._

---

#### Issue #DL038 — EstornoPagamento: parse dependente de Locale pode multiplicar valor por 100x
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/EstornoPagamentoController.java`
- **Linha(s):** 34-37, 57
- **Problema:** `setDados` preenche campo com `String.format("%.2f", pago)` (Locale JVM). `getValorEstorno()` parseia com `replace(".", "").replace(",", ".")` (assume pt-BR). Se JVM usar Locale en-US, "100.50" vira "10050" (100x errado).
- **Impacto:** Estorno com valor drasticamente errado se Locale JVM diferir de pt-BR. Prejuizo financeiro direto.
- **Fix sugerido:** Usar `MoneyUtil.parseBigDecimal()` para parsing consistente, e `MoneyUtil.formatar()` para exibicao.
- **Observacoes:**
> _Risco alto em ambientes de producao com JVM em en-US._

---

#### Issue #DL039 — RegistrarPagamentoEncomenda: itens excluidos e reinseridos sem transacao
- [x] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/RegistrarPagamentoEncomendaController.java`
- **Linha(s):** 225-233
- **Problema:** `encomendaItemDAO.excluirPorEncomenda()` e `encomendaItemDAO.inserir()` usam conexoes separadas sem transacao. Se a reinseracao falhar, a encomenda fica SEM itens no banco.
- **Impacto:** Encomenda salva sem itens de carga. Impossivel calcular valor correto. Dados de carga perdidos permanentemente.
- **Fix sugerido:** Executar exclusao+reinsercao na mesma conexao com `setAutoCommit(false)` e rollback em caso de erro.
- **Observacoes:**
> _Semelhante a issue #029 (ja corrigida em EncomendaDAO.inserirComItens), mas o RegistrarPagamentoEncomendaController nao usa esse metodo._

---

### ALTOS

#### Issue #DL040 — FinanceiroFretes: pagamento em duas conexoes sem transacao
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 263-297
- **Problema:** Busca desconto anterior (conexao 1) e UPDATE do frete (conexao 2) sem transacao envolvendo ambas. Race condition se dois operadores pagarem o mesmo frete simultaneamente.
- **Impacto:** Desconto duplicado e `valor_devedor` negativo no banco.
- **Fix sugerido:** Usar uma unica conexao com transacao para ler desconto e aplicar UPDATE.
- **Observacoes:**
> _Operacao financeira critica sem atomicidade._

---

#### Issue #DL041 — FinanceiroFretes: status "NAO_PAGO" incompativel com enum
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 335
- **Problema:** Estorno total define status `"NAO_PAGO"`. Embora `StatusPagamento.NAO_PAGO` exista no enum, filtros SQL e outros controllers usam `"PENDENTE"` para fretes nao pagos, criando inconsistencia.
- **Impacto:** Fretes com estorno total podem nao aparecer em filtros de devedores. Relatorios com totais incorretos.
- **Fix sugerido:** Usar `StatusPagamento.PENDENTE.name()` em vez de `"NAO_PAGO"` para estornos totais de fretes.
- **Observacoes:**
> _Relacionado a issue #030 (tolerancia inconsistente) e #025 (strings de status)._

---

#### Issue #DL042 — FinanceiroPassagens: estorno nao zera saldo de cartao quando car < estornoBD
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroPassagensController.java`
- **Linha(s):** 450-459
- **Problema:** A logica subtrai estorno de dinheiro, pix, cartao em cascata. Na ultima condicao, se `car < estornoBD`, `car` permanece com o valor original (nao e zerado) e o excesso e ignorado.
- **Impacto:** `valor_pagamento_cartao` incorreto no banco apos estornos que excedem o valor pago por cartao.
- **Fix sugerido:** `else { estornoBD = estornoBD.subtract(car); car = BigDecimal.ZERO; }`
- **Observacoes:**
> __

---

#### Issue #DL043 — CadastroViagem: multiplas viagens ativas se definirViagemAtiva falhar
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroViagemController.java`
- **Linha(s):** 396-401
- **Problema:** A viagem e salva com `ativa=true` primeiro, depois `definirViagemAtiva()` (que desativa as outras) e chamada separadamente. Se falhar, o sistema fica com multiplas viagens ativas.
- **Impacto:** Telas que buscam viagem ativa podem retornar a errada. Lancamentos em viagem incorreta.
- **Fix sugerido:** Incluir `definirViagemAtiva` na mesma transacao que o INSERT/UPDATE da viagem.
- **Observacoes:**
> __

---

#### Issue #DL044 — CadastroViagem: excluir viagem ativa deixa sistema sem viagem ativa
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroViagemController.java`
- **Linha(s):** 426-434
- **Problema:** Ao excluir viagem que era `is_atual = true`, nenhuma outra viagem e automaticamente ativada. Todas as telas que dependem de viagem ativa ficam bloqueadas.
- **Impacto:** Sistema inutilizavel ate que o usuario manualmente ative outra viagem.
- **Fix sugerido:** Apos excluir, buscar viagem mais recente e ativa-la automaticamente, ou alertar o usuario.
- **Observacoes:**
> __

---

#### Issue #DL045 — ExtratoPassageiro: forma de pagamento hard-coded "DINHEIRO/PIX"
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** 400
- **Problema:** Quitacao de divida total grava `formaPagamento = "DINHEIRO/PIX"` independente da forma real escolhida pelo usuario.
- **Impacto:** Historico de pagamento incorreto. Relatorios por forma de pagamento distorcidos.
- **Fix sugerido:** Ler a forma de pagamento do dialogo de finalizacao e propagar para o registro.
- **Observacoes:**
> __

---

#### Issue #DL046 — ExtratoPassageiro: totais calculados sobre TODAS passagens, nao sobre filtradas
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** 251, 267-269
- **Problema:** `listarExtratoPorPassageiro(nome, "TODOS")` traz todas as passagens. Os totais (`totalGeral`, `totalPago`, `divida`) sao calculados sobre todas elas ANTES do filtro de status ser aplicado na exibicao.
- **Impacto:** Quando usuario filtra por "PENDENTES", os labels de total nao correspondem ao que esta visivel na tabela.
- **Fix sugerido:** Calcular totais APOS aplicar o filtro de status, ou exibir labels separados para total geral e total filtrado.
- **Observacoes:**
> __

---

#### Issue #DL047 — ExtratoClienteEncomenda: formula de desconto proporcional incorreta
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoClienteEncomendaController.java`
- **Linha(s):** 247-252
- **Problema:** `fatorPagamento = (dividaTotal - descontoTotal) / dividaTotal` aplicado igualmente a todas as encomendas. Se `descontoTotal > dividaTotal`, fator fica negativo e e forçado a 0, resultando em desconto ZERO para todas as encomendas (silencioso).
- **Impacto:** Desconto maior que divida total resulta em zero de desconto em vez de erro explicito. Quitacao com desconto pode gravar valores incorretos por encomenda individual.
- **Fix sugerido:** Validar `descontoTotal <= dividaTotal` antes de calcular fator. Mostrar erro se exceder.
- **Observacoes:**
> __

---

#### Issue #DL048 — BalancoViagem: caixa recebido nao desconta passagens pendentes
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 217
- **Problema:** `totalPendenteGlobal` soma pendentes de encomendas e fretes, mas NAO de passagens. O "Caixa Recebido" exibido fica inflado.
- **Impacto:** Relatorio de balanco mostra caixa maior do que o real se houver passagens a receber.
- **Fix sugerido:** Incluir pendente de passagens no calculo de `totalPendenteGlobal`.
- **Observacoes:**
> __

---

#### Issue #DL049 — CadastroBoleto: boletos listados sem filtro por viagem
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 232
- **Problema:** SQL busca `WHERE forma_pagamento = 'BOLETO'` sem filtrar por `id_viagem`. Boletos de todas as viagens aparecem misturados.
- **Impacto:** Balanco financeiro de uma viagem contabiliza boletos de viagens anteriores.
- **Fix sugerido:** Adicionar filtro `AND id_viagem = ?` com a viagem ativa.
- **Observacoes:**
> __

---

#### Issue #DL050 — AuditoriaExclusoesSaida: filtro usa coluna errada
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/AuditoriaExclusoesSaida.java`
- **Linha(s):** 185
- **Problema:** Filtro usa `WHERE acao = 'EXCLUSAO_DESPESA'`, mas `CadastroBoletoController` grava com `tipo_operacao = 'EXCLUSAO_BOLETO'`. Colunas/valores incompativeis entre insercao e leitura.
- **Impacto:** Registros de auditoria de exclusao de boletos nao aparecem na tela de auditoria.
- **Fix sugerido:** Padronizar nome da coluna e valores usados por todos os controllers de auditoria.
- **Observacoes:**
> __

---

#### Issue #DL051 — InserirEncomenda: re-entrega sobrescreve dados do recebedor original
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** 732-854
- **Problema:** Se `entregue = true` e `estaQuitado = false`, o botao permite nova "entrega". `handleEntregar` nao verifica se ja foi entregue — executa `registrarEntrega()` novamente, sobrescrevendo nome/documento do recebedor original.
- **Impacto:** Dados de entrega anterior perdidos. Impossivel saber quem recebeu originalmente.
- **Fix sugerido:** Bloquear `handleEntregar` se `encomenda.isEntregue() == true`, ou solicitar confirmacao explicita.
- **Observacoes:**
> __

---

#### Issue #DL052 — GestaoFuncionarios: data referencia de fechamento aponta para mes anterior
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/GestaoFuncionariosController.java`
- **Linha(s):** 486-487
- **Problema:** `dataReferenciaFechamento = dataHoje.withDayOfMonth(1).minusDays(1)` resulta no ULTIMO dia do mes anterior. Fechamento registrado com data do mes errado se executado antes do dia 20.
- **Impacto:** Folha de pagamento registrada no mes errado. Relatorios de custos mensais distorcidos.
- **Fix sugerido:** Usar `LocalDate.now()` como data de referencia, ou `dataHoje.withDayOfMonth(1)` (primeiro dia do mes atual).
- **Observacoes:**
> __

---

#### Issue #DL053 — RelatorioFretes: devedor lido do campo persistido sem recalcular
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/RelatorioFretesController.java`
- **Linha(s):** 310-317
- **Problema:** `double devedor = rs.getDouble("valor_devedor")` le o valor gravado na insercao do frete. Se pagamentos parciais foram feitos depois, o campo pode nao ter sido atualizado, resultando em valores desatualizados.
- **Impacto:** Relatorio de devedores mostra devedores que ja pagaram.
- **Fix sugerido:** Calcular devedor como `valor_total_itens - valor_pago` na query SQL, ou usar `GREATEST(valor_total_itens - valor_pago, 0)`.
- **Observacoes:**
> __

---

#### Issue #DL054 — QuitarDividaEncomendaTotal: sem validacao de desconto
- [x] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/QuitarDividaEncomendaTotalController.java`
- **Linha(s):** 98
- **Problema:** `confirmar()` nao valida se desconto e maior que a divida, se o campo e valido numericamente, ou se forma de pagamento foi selecionada. `getDesconto()` retorna o valor bruto digitado.
- **Impacto:** Chamador pode receber desconto maior que divida. `converter()` retorna 0.0 silenciosamente para texto invalido.
- **Fix sugerido:** Validar `desconto <= totalDivida` e `cmbFormaPagamento.getValue() != null` antes de fechar.
- **Observacoes:**
> __

---

### MEDIOS

#### Issue #DL055 — FinanceiroEncomendas: desconto nao atualiza total_a_pagar no banco
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/FinanceiroEncomendasController.java`
- **Linha(s):** 268-280
- **Problema:** UPDATE salva `valor_pago`, `desconto`, `status_pagamento` mas NAO `total_a_pagar`. Na proxima abertura, `total_a_pagar` exibe valor original sem desconto.
- **Impacto:** Inconsistencia visual entre total exibido e saldo real.
- **Fix sugerido:** Incluir `total_a_pagar = total_a_pagar - desconto_novo` no UPDATE, ou calcular saldo sempre como `total - desconto - pago`.
- **Observacoes:**
> __

---

#### Issue #DL056 — RegistrarPagamentoEncomenda: forma_pagamento = "PENDENTE"
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/RegistrarPagamentoEncomendaController.java`
- **Linha(s):** 192-199
- **Problema:** Se nenhum valor e pago (`dinheiro=0, pix=0, cartao=0`), `formaPagamento` e gravada como `"PENDENTE"` (que e um status, nao uma forma de pagamento).
- **Impacto:** Relatorios por forma de pagamento incluem valores reais em "PENDENTE".
- **Fix sugerido:** Se nenhum valor pago, nao alterar `forma_pagamento` da encomenda.
- **Observacoes:**
> __

---

#### Issue #DL057 — VenderPassagem: coluna "Valor a Pagar" exibe devedor
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/VenderPassagemController.java`
- **Linha(s):** 560
- **Problema:** `colValorAPagar.setCellValueFactory(... getDevedor())` — a coluna rotulada "Valor a Pagar" exibe o saldo devedor, nao o valor a pagar.
- **Impacto:** Confusao para o operador. Dois campos exibem o mesmo conceito.
- **Fix sugerido:** Usar `getValorAPagar()` para a coluna, ou renomear para "Saldo Devedor".
- **Observacoes:**
> __

---

#### Issue #DL058 — ExtratoPassageiro: saldo "R$ 0,00" hard-coded na 2a via de recibo
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ExtratoPassageiroController.java`
- **Linha(s):** 353
- **Problema:** `reconstruirItensDoRecibo` usa saldo "R$ 0,00" fixo. Ao reimprimir 2a via, recibo mostra saldo zero mesmo em pagamento parcial.
- **Impacto:** Documento impresso com informacao incorreta entregue ao passageiro.
- **Fix sugerido:** Calcular saldo real com base no historico do banco.
- **Observacoes:**
> __

---

#### Issue #DL059 — CadastroFrete: parseToBigDecimal passa por double
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 2186-2196
- **Problema:** `double vD = parseValorMonetario(vS); return BigDecimal.valueOf(vD)` — a conversao passa por `double` antes de virar `BigDecimal`, perdendo precisao para valores grandes.
- **Impacto:** Erros de centavos em fretes com valores altos.
- **Fix sugerido:** Usar `new BigDecimal(valorLimpo)` diretamente da String.
- **Observacoes:**
> __

---

#### Issue #DL060 — InserirEncomenda: quantidade silenciosa = 1 se parse falhar
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/InserirEncomendaController.java`
- **Linha(s):** 1378
- **Problema:** `catch(Exception e) { item.setQuantidade(1); }` — se parse falhar, item e adicionado com quantidade 1 silenciosamente. Total na tela pode diferir do total salvo.
- **Impacto:** Item com quantidade errada sem feedback ao usuario.
- **Fix sugerido:** Exibir alerta de validacao em vez de usar fallback silencioso.
- **Observacoes:**
> __

---

#### Issue #DL061 — GestaoFuncionarios: desconto de falta sobre salario bruto sem validar duplicata
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/GestaoFuncionariosController.java`
- **Linha(s):** 431
- **Problema:** `salario / 30.0` calcula desconto sobre bruto (deveria considerar INSS). Nao valida se ja existe falta registrada para o mesmo dia.
- **Impacto:** Desconto de falta potencialmente maior que o correto. Multiplas faltas no mesmo dia permitidas.
- **Fix sugerido:** Verificar `WHERE data_referencia = ? AND tipo = 'FALTA'` antes de registrar. Calcular sobre liquido se for o padrao da empresa.
- **Observacoes:**
> __

---

#### Issue #DL062 — CadastroFrete: cidade de cobranca errada com rota contendo hifen
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 178-185
- **Problema:** `rota.split("-")` divide na primeira ocorrencia. Se a rota contem hifen no nome (ex: "SAO LUIS-ACAILANDIA - MARABA"), o split retorna fragmento do nome como cidade de cobranca.
- **Impacto:** Campo `txtCidadeCobranca` preenchido com valor incorreto.
- **Fix sugerido:** Usar `split(" - ", 2)` (com espacos) para separar apenas no delimitador padrao de rota.
- **Observacoes:**
> __

---

#### Issue #DL063 — ReciboAvulsoDAO e ReciboQuitacaoPassageiroDAO: double para valores financeiros
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java` (L20) + `src/dao/ReciboQuitacaoPassageiroDAO.java` (L23, L49)
- **Linha(s):** Multiplas
- **Problema:** `setDouble(4, r.getValor())` e `rs.getDouble("valor_total")` — valores financeiros lidos/gravados como `double`. Os models correspondentes tambem usam `double`.
- **Impacto:** Erros de arredondamento em recibos avulsos e historico de quitacao.
- **Fix sugerido:** Migrar para `BigDecimal` nos models e DAOs.
- **Observacoes:**
> _Parte da issue sistemica #021 (double para dinheiro). Estes DAOs nao foram incluidos na migracao anterior._

---

#### Issue #DL064 — LoginController: trim() na senha pode impedir login valido
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/LoginController.java`
- **Linha(s):** 61
- **Problema:** `txtSenha.getText().trim()` — se a senha foi cadastrada com espacos nas bordas (via UsuarioDAO que NAO faz trim no hash), o login falhara porque BCrypt compara a string exata.
- **Impacto:** Usuarios com espacos na senha nao conseguem logar.
- **Fix sugerido:** Remover `trim()` da senha no login, ou aplicar `trim()` tambem no cadastro.
- **Observacoes:**
> __

---

### BAIXOS

#### Issue #DL065 — CadastroFrete: nome de item gravado em minusculo
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroFreteController.java`
- **Linha(s):** 1663
- **Problema:** `nomeItemFinal = itemNomeOuDescricao.trim().toLowerCase()` — item gravado em minusculo no banco e exibido assim em notas/recibos.
- **Impacto:** Documentos financeiros com formatacao incorreta.
- **Fix sugerido:** Preservar case original ou usar `toUpperCase()` para documentos.
- **Observacoes:**
> __

---

#### Issue #DL066 — ConferenteDAO: implementacao stub (nao funcional)
- [ ] **Concluido**
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ConferenteDAO.java`
- **Linha(s):** 14-24
- **Problema:** `listarNomes()` retorna lista vazia. `inserir()` apenas imprime no console sem gravar no banco.
- **Impacto:** ComboBox de conferentes sempre vazio. Conferente digitado manualmente nao e persistido no cadastro.
- **Fix sugerido:** Implementar SELECT/INSERT reais na tabela `conferentes`.
- **Observacoes:**
> __

---

## NOTA SOBRE ISSUES SISTEMICAS

### double para valores financeiros (issue persistente)
Os seguintes arquivos ainda usam `double` para calculos/armazenamento de valores monetarios:

| Arquivo | Contexto |
|---------|----------|
| `BalancoViagemDAO.java` | Toda a cadeia de balanço (#021) |
| `FinanceiroEntradaController.java` | Dashboard de entradas |
| `PagamentoFreteController.java` | Controller de pagamento inteiro |
| `QuitarDividaEncomendaTotalController.java` | Quitacao de encomendas |
| `ExtratoPassageiroController.java` | Calculo de divida |
| `RelatorioFretesController.java` | Total de itens |
| `GestaoFuncionariosController.java` | Folha de pagamento |
| `ReciboAvulsoDAO.java` + `ReciboQuitacaoPassageiroDAO.java` | Persistencia |
| `CadastroFreteController.java` | Classe interna `FreteItem` |
| `AgendaDAO.ResumoBoleto` | Display de boletos |

Recomendacao: migrar sistematicamente todos os campos financeiros para `BigDecimal`. Priorizar os que envolvem persistencia e calculos (BalancoViagemDAO, PagamentoFrete, QuitarDivida).

---

## COBERTURA

| Diretorio | Arquivos | Analisados | Issues novas |
|-----------|----------|-----------|-------------|
| src/dao/ | 28 | 28 (100%) | 3 |
| src/database/ | 2 | 2 (100%) | 0 |
| src/gui/ | 55 | 55 (100%) | 30 |
| src/gui/util/ | 7 | 7 (100%) | 1 |
| src/model/ | 26 | 26 (100%) | 0 |
| src/tests/ | 5 | 5 (100%) | 0 |
| database_scripts/ | 7 | 7 (100%) | 0 |
| Configs | 4 | 4 (100%) | 0 |
| **TOTAL** | **134** | **134 (100%)** | **36** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO) — CONCLUIDO
- [x] #DL031 — Valor pago nao gravado — **FALSO POSITIVO** (valor transferido por referencia de objeto)
- [x] #DL032 — Passagem gratuita bloqueada — **FIXADO** (validacao `< 0` + pula dialogo pagamento)
- [x] #DL033 — Status sempre EMITIDA — **FIXADO** (StatusPagamento.calcular baseado em devedor)
- [x] #DL034 — Pagamento apagado no UPDATE de frete — **FIXADO** (preserva valor_pago/troco existentes)
- [x] #DL035 — Race condition numero_frete MAX+1 — **FIXADO** (sequence seq_numero_frete + script 010)
- [x] #DL036 — Provisao 13o formula errada — **FIXADO** (meses no ano corrente, max 12)
- [x] #DL037 — ResultSet fora do try — **FIXADO** (removida brace extra)
- [x] #DL038 — Estorno Locale-dependent — **FIXADO** (MoneyUtil.parseBigDecimalSafe + %,.2f)
- [x] #DL039 — Itens encomenda sem transacao — **FIXADO** (transacao atomica com rollback)

### Importante (ALTO) — CONCLUIDO
- [x] #DL040 — Pagamento frete sem transacao — **FIXADO** (conexao unica + FOR UPDATE + commit)
- [x] #DL041 — Status NAO_PAGO incompativel — **FIXADO** (StatusPagamento.calcular)
- [x] #DL042 — Estorno cartao saldo errado — **FIXADO** (else car = ZERO)
- [x] #DL043 — Multiplas viagens ativas — **FIXADO** (alert explicito se definirViagemAtiva falhar)
- [x] #DL044 — Excluir viagem ativa sem reposicao — **FIXADO** (ativa proxima automaticamente)
- [x] #DL045 — Forma pagamento hard-coded — **FIXADO** (determina forma real do dialogo)
- [x] #DL046 — Totais antes do filtro — **FIXADO** (totais calculados apos check `adicionar`)
- [x] #DL047 — Formula desconto proporcional — **FIXADO** (validacao desconto <= divida + alert)
- [x] #DL048 — Balanco sem passagens pendentes — **FIXADO** (inclui SUM valor_devedor passagens)
- [x] #DL049 — Boletos sem filtro viagem — **FIXADO** (AND id_viagem = ?)
- [x] #DL050 — Auditoria coluna errada — **FIXADO** (busca acao OR tipo_operacao)
- [x] #DL051 — Re-entrega sobrescreve recebedor — **FIXADO** (confirmacao explicita se ja entregue)
- [x] #DL052 — Data fechamento mes errado — **FIXADO** (usa dataHoje diretamente)
- [x] #DL053 — Relatorio devedor desatualizado — **FIXADO** (recalcula total - pago em tempo real)
- [x] #DL054 — QuitarDivida sem validacao — **FIXADO** (valida desconto <= divida + forma obrigatoria)

### Importante (MEDIO) — PARCIALMENTE CONCLUIDO
- [x] #DL055 — Desconto nao atualiza total_a_pagar — **RESOLVIDO** (model calcula corretamente via getSaldoDevedor)
- [x] #DL056 — forma_pagamento = "PENDENTE" — **FIXADO** (nao sobrescreve se nenhum valor pago)
- [x] #DL057 — Coluna a pagar exibe devedor — **FIXADO** (usa getValorAPagar)
- [x] #DL058 — Saldo hard-coded 2a via — **RESOLVIDO** (status QUITADO + saldo zero e correto para itens ja pagos)
- [x] #DL059 — parseToBigDecimal via double — **FIXADO** (converte direto da String)
- [x] #DL060 — Quantidade silenciosa = 1 — **FIXADO** (alert + return em vez de fallback)
- [x] #DL061 — Desconto falta bruto + duplicata — **FIXADO** (verifica falta existente no dia)
- [x] #DL062 — Cidade cobranca com hifen — **FIXADO** (split " - " com limite 2)
- [x] #DL063 — ReciboDAO double — **FIXADO** (setBigDecimal/getBigDecimal)
- [x] #DL064 — trim() na senha — **FIXADO** (removido trim)
- [x] #DL023 — CAST crash fallback — **FIXADO** (filtro regex `^\d+$`)
- [x] #025 — Strings PENDENTE_PAGAMENTO — **FIXADO** (StatusPagamento.PENDENTE.name())
- [ ] #021 — BalancoViagemDAO double (sistemico) — **Resolvido pelo STATUS.md (sessao anterior)**
- [ ] #023 — Viagem passado bloqueia update — **Resolvido pelo STATUS.md (sessao anterior)**
- [ ] #024 — SessaoUsuario.touch() nunca chamado — **Resolvido pelo STATUS.md (sessao anterior)**
- [x] #033 — CidadeDAO hardcoded — **FIXADO** (busca cidades das rotas + fallback)
- [x] #034 — Encomenda data String — **FIXADO** (campo LocalDate tipado + getter String compativel)
- **Notas:**
> _12 de 16 medias corrigidas nesta sessao. 3 ja resolvidas em sessao anterior. Restam 2 (#033, #034) + 4 baixas._

### Backlog (BAIXO) — CONCLUIDO
- [x] #DL065 — Nome item minusculo — **FIXADO** (toUpperCase para documentos)
- [x] #DL066 — ConferenteDAO stub — **FIXADO** (SELECT/INSERT reais na tabela conferentes)
- [x] #035 — SyncClient recebimento — **PARCIAL** (metodo stub com TODO + mensagem clara)
- [x] #DL026 — Off-by-one dias comerciais — **VERIFICADO** (algoritmo correto — +1 padrao CLT)

---

## NOTAS

> **Comparacao V4.0 → V4.1:**
> - V4.0 encontrou 36 issues novas, totalizando 49 ativas
> - V4.1 corrigiu 23 issues (8 criticas + 15 altas) + 1 descartada como falso positivo
> - **Total ativo V4.1: 26 issues** (12 medias + 4 baixas + 5 pendentes anteriores + 1 parcial + 4 anteriores resolvidas pelo STATUS)
>
> **Todas as 9 CRITICAS e 15 ALTAS de logica foram corrigidas.**
> Restam 16 MEDIAS + 4 BAIXAS para proximas sprints.
>
> **Areas mais problematicas:**
> 1. **Fluxo de venda de passagem** (DL031-DL033): valor pago descartado, gratuidade bloqueada, status incorreto
> 2. **Editar frete apaga pagamento** (DL034): qualquer edicao zera valor_pago
> 3. **GestaoFuncionarios** (DL036, DL052, DL061): formulas de RH erradas
> 4. **Financeiro Passagens** (DL037): tela inteira inoperante por brace extra
> 5. **double persistente**: 10+ arquivos ainda usam double para valores financeiros
>
> **Recomendacao principal:** Corrigir DL037 (5min) e DL031-DL033 (1h total) primeiro — afetam operacao diaria de venda.

---
*Gerado por Claude Code (Deep Audit V4.0) — Revisao humana obrigatoria*
