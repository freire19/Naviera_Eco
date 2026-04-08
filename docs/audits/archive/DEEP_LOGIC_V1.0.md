# AUDITORIA PROFUNDA — LOGIC — SistemaEmbarcacaoProjeto_Novo
> **Versao:** V1.0
> **Data:** 2026-04-07
> **Categoria:** Logic (Regras de Negocio)
> **Base:** AUDIT_V1.0
> **Arquivos analisados:** 131 de 131 total (cobertura completa)

---

## RESUMO

| Status | Quantidade |
|--------|-----------|
| Novos problemas | 27 |
| Issues anteriores resolvidas | 0 |
| Issues anteriores parcialmente resolvidas | 0 |
| Issues anteriores pendentes | 9 |
| **Total de issues ativas** | **36** |

---

## ISSUES ANTERIORES — STATUS

### Resolvidas

Nenhuma. Nenhum fix foi aplicado desde o AUDIT_V1.0.

### Pendentes

| Issue | Titulo | Observacao |
|-------|--------|-----------|
| #027 | double para dinheiro (models) | Confirmado: Frete, ReciboAvulso, DadosBalancoViagem, ReciboQuitacaoPassageiro, FreteItem, ItemResumoBalanco |
| #028 | double para dinheiro (controllers) | Confirmado: BaixaPagamento, Financeiro*, Estorno, Boleto, Recibo, CadastroFrete |
| #029 | Transacao ausente Encomenda+Itens | Confirmado: conexoes separadas sem atomicidade |
| #030 | Tolerancia PAGO inconsistente | Confirmado: operadores diferentes em contextos distintos |
| #031 | Metodos viagem duplicados | Confirmado: fallback inutil (dead code) |
| #032 | Stubs de DAO (3 classes) | Confirmado: println em vez de INSERT |
| #033 | CidadeDAO lista hardcoded | Confirmado |
| #034 | Encomenda data como String | Confirmado |
| #035 | SyncClient recebimento nao implementado | Confirmado |

---

## NOVOS PROBLEMAS

### Integridade de Transacoes e Concorrencia

#### Issue #DL001 — Race condition na geracao de numero de bilhete (MAX+1)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 20-28
- **Problema:** Usa `SELECT MAX(CAST(numero_bilhete AS INTEGER)) + 1` em vez de sequence. Dois usuarios simultaneos recebem o mesmo numero. SELECT e INSERT em conexoes/transacoes separadas.
- **Impacto:** Bilhetes duplicados. Confusao no embarque e em relatorios.
- **Codigo problematico:**
```java
String sql = "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens";
```
- **Fix sugerido:** Usar sequence PostgreSQL: `SELECT nextval('seq_bilhete')` ou `SERIAL`.
- **Observacoes:**
> _Janela de race: entre o SELECT e o INSERT subsequente (linha 30), nao ha lock._

---

#### Issue #DL002 — Race condition na geracao de numero de encomenda (MAX+1)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 162-173
- **Problema:** Mesmo padrao MAX+1 para `numero_encomenda`. Adicionalmente, `CAST(numero_encomenda AS INTEGER)` falha se valor nao-numerico existir.
- **Impacto:** Encomendas duplicadas. Crash se numero_encomenda contem texto.
- **Codigo problematico:**
```java
String sql = "SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) FROM encomendas WHERE id_viagem = ? AND rota = ?";
```
- **Fix sugerido:** Sequence ou UNIQUE constraint em `(id_viagem, rota, numero_encomenda)`.
- **Observacoes:**
> _CAST falha com PSQLException se qualquer registro tiver texto no campo._

---

#### Issue #DL003 — Quitacao em massa sem transacao atomica
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 295-329
- **Problema:** `quitarDividaTotalPassageiro` atualiza multiplas passagens em auto-commit. Se conexao cair no meio, algumas ficam PAGO e outras PENDENTE.
- **Impacto:** Estado financeiro inconsistente — parcialmente quitado sem registro.
- **Fix sugerido:** Wrap em `setAutoCommit(false)` / `commit()` / `rollback()`.
- **Observacoes:**
> _Complementa issue #020 (LIKE wildcard) com problema adicional de atomicidade._

---

#### Issue #DL004 — TOCTOU em EmbarcacaoDAO.inserirOuBuscar
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EmbarcacaoDAO.java`
- **Linha(s):** 12-48
- **Problema:** Check-then-insert em conexoes separadas. Dois calls simultaneos com mesmo nome passam o null check e tentam inserir duplicata.
- **Impacto:** Embarcacao duplicada ou constraint violation silenciada.
- **Codigo problematico:**
```java
Embarcacao existente = buscarPorNome(embarcacao.getNome());
if (existente != null) return existente;
// ... INSERT em outra conexao
```
- **Fix sugerido:** `INSERT ... ON CONFLICT (nome) DO NOTHING RETURNING id_embarcacao`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Cascade e Integridade Referencial

#### Issue #DL005 — ViagemDAO.excluir nao deleta filhos (passagens, encomendas, fretes)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 328-335
- **Problema:** `DELETE FROM viagens WHERE id_viagem = ?` sem deletar passagens, encomendas, fretes, recibos, saidas financeiras que referenciam essa viagem.
- **Impacto:** FK violation (erro silenciado) ou registros orfaos com id_viagem invalido.
- **Codigo problematico:**
```java
String sql = "DELETE FROM viagens WHERE id_viagem = ?";
```
- **Fix sugerido:** Deletar filhos primeiro em transacao (padrao de EncomendaDAO.excluir) ou verificar `ON DELETE CASCADE` no schema.
- **Observacoes:**
> _Se FK nao tem CASCADE, o delete falha silenciosamente (catch retorna false)._

---

#### Issue #DL006 — EmbarcacaoDAO/RotaDAO.excluir sem verificar referencias
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EmbarcacaoDAO.java`:127-139, `src/dao/RotaDAO.java`:105-116
- **Linha(s):** Ver acima
- **Problema:** Delete de embarcacao/rota referenciada por viagens falha silenciosamente ou cria orfaos.
- **Impacto:** Viagens com embarcacao/rota inexistente.
- **Fix sugerido:** Verificar se existem viagens referenciando antes de permitir delecao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL007 — AuxiliaresDAO.excluir* sem verificar se auxiliar esta em uso
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 138-146, 201-209, 264-272, 329-337, 392-400, 455-463, 518-526
- **Problema:** 7 metodos excluir* deletam auxiliares (tipo_documento, sexo, nacionalidade, acomodacao, agente, etc.) sem verificar se estao em uso por passageiros/passagens.
- **Impacto:** FK violation silenciada ou passageiros com referencia para registro deletado.
- **Fix sugerido:** Check count de registros referenciando antes de deletar.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL008 — AuxiliaresDAO.inserir* sem prevencao de duplicatas
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 104-112, 167-175, 230-238, 293-301, 358-366, 421-429, 484-492
- **Problema:** 7 metodos inserir* fazem INSERT sem checar existencia. Se tabela nao tem UNIQUE constraint, duplicatas sao criadas.
- **Impacto:** ComboBoxes com itens duplicados. Confusao na selecao.
- **Fix sugerido:** `INSERT ... ON CONFLICT DO NOTHING` (padrao ja usado em ClienteEncomendaDAO).
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Calculos Financeiros

#### Issue #DL009 — Desconto pode exceder total em BaixaPagamentoController
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/BaixaPagamentoController.java`
- **Linha(s):** 64-80
- **Problema:** Nenhuma validacao de upper bound no desconto. Desconto de R$10.000 em conta de R$50 e aceito. `totalComDesconto` fica negativo, e qualquer pagamento marca como PAGO.
- **Impacto:** Status PAGO incorreto. Desconto maior que total gravado no banco.
- **Codigo problematico:**
```java
double totalComDesconto = dados.getValorTotalOriginal() - dados.getDesconto();
// Se desconto > total, totalComDesconto < 0, novoPago >= negativo → PAGO
```
- **Fix sugerido:** `if (desconto > restanteOriginal) { alert("Desconto excede total"); return; }`
- **Observacoes:**
> _confirmar() na linha 118 nao tem NENHUMA validacao — apenas `confirmado = true; fechar();`_

---

#### Issue #DL010 — Pagamento parcial de encomenda sobrescreve desconto anterior
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroEncomendasController.java`
- **Linha(s):** 238-255
- **Problema:** Cada pagamento parcial faz UPDATE no campo `desconto`, `tipo_pagamento` e `caixa`, sobrescrevendo valores do pagamento anterior.
- **Impacto:** 1o pagamento: desconto R$10 com Dinheiro. 2o pagamento: desconto R$0 com PIX. Banco fica com desconto=0 e tipo=PIX, perdendo historico.
- **Codigo problematico:**
```java
String sql = "UPDATE encomendas SET valor_pago = ?, desconto = ?, tipo_pagamento = ?, caixa = ?, status_pagamento = ? WHERE id_encomenda = ?";
```
- **Fix sugerido:** Acumular desconto (`desconto = desconto + ?`) ou usar tabela separada de pagamentos.
- **Observacoes:**
> _Tipo_pagamento e caixa tambem sobrescritos — 1o pagamento em Dinheiro, 2o em PIX, resultado: PIX._

---

#### Issue #DL011 — Frete pagamento nao grava forma_pagamento nem caixa
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 245-268
- **Problema:** UPDATE de frete atualiza apenas `valor_pago`, `valor_devedor`, `status_frete`. Forma de pagamento e caixa coletados pelo BaixaPagamentoController sao descartados.
- **Impacto:** Impossivel saber como frete foi pago. Relatorios por forma de pagamento incompletos.
- **Codigo problematico:**
```java
String sql = "UPDATE fretes SET valor_pago = ?, valor_devedor = ?, status_frete = ? WHERE id_frete = ?";
// dados.getFormaPagamento() e dados.getCaixa() NUNCA gravados
```
- **Fix sugerido:** Adicionar `tipo_pagamento = ?, nome_caixa = ?` no UPDATE.
- **Observacoes:**
> _BaixaPagamentoController coleta os dados, mas FinanceiroFretesController ignora._

---

#### Issue #DL012 — Frete pagamento nao considera desconto ja armazenado
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 245-250
- **Problema:** Calculo de `novoDevedor` usa desconto da sessao atual, ignorando desconto ja gravado no banco.
- **Impacto:** Desconto aplicado duas vezes ou ignorado em pagamento subsequente.
- **Fix sugerido:** Ler desconto atual do banco antes de calcular.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL013 — Estorno permite devolver R$0.01 a mais que o pago
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/EstornoPagamentoController.java`
- **Linha(s):** 67
- **Problema:** Tolerancia `v > pagoOriginal + 0.01` permite estorno de R$100.01 em pagamento de R$100.00.
- **Impacto:** Perda financeira de centavos por operacao. Acumula com volume.
- **Codigo problematico:**
```java
if (v <= 0.001 || v > pagoOriginal + 0.01) {
```
- **Fix sugerido:** `v > pagoOriginal + 0.001` ou comparacao exata com BigDecimal.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL014 — Parcela de boleto sem tratamento de resto (centavos perdidos)
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroBoletoController.java`
- **Linha(s):** 126
- **Problema:** `double valorParcela = total / parcelas;` — R$100/3 = R$33.33 × 3 = R$99.99. Um centavo perdido.
- **Impacto:** Soma das parcelas nao bate com total. Diferenca acumula.
- **Fix sugerido:** Ultima parcela absorve resto: `ultimaParcela = total - (valorParcela * (parcelas - 1))`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL015 — Encomenda.getSaldoDevedor() pode retornar valor negativo
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/model/Encomenda.java`
- **Linha(s):** 43-46
- **Problema:** Se `valorPago > (totalAPagar - desconto)`, retorna negativo. Usado em somas de totais em ListaEncomendaController e RelatorioEncomendaGeralController — deflaciona "total a receber".
- **Impacto:** Relatorio mostra total a receber menor que real. Overpayment de um cliente reduz total de todos.
- **Codigo problematico:**
```java
public BigDecimal getSaldoDevedor() {
    BigDecimal total = getTotalAPagar().subtract(getDesconto());
    return total.subtract(getValorPago());  // pode ser negativo
}
```
- **Fix sugerido:** `return total.subtract(getValorPago()).max(BigDecimal.ZERO);`
- **Observacoes:**
> _ListaEncomendaController:519 soma saldos negativos ao total, deflacionando footer._

---

#### Issue #DL016 — ExtratoClienteEncomenda usa ILIKE com wildcards para quitar dividas
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoClienteEncomendaController.java`
- **Linha(s):** 245-258
- **Problema:** Bulk UPDATE de quitacao usa `destinatario ILIKE '%nome%'`. "MARIA" matcha "ANA MARIA" e "MARIA SILVA".
- **Impacto:** Dividas de clientes errados quitadas. Perda financeira.
- **Codigo problematico:**
```java
stmt.setString(5, "%" + nomeClienteAtual.trim() + "%");
```
- **Fix sugerido:** Usar match por `id_encomenda` dos registros exibidos, nao por nome.
- **Observacoes:**
> _Mesmo padrao da issue #020 (PassagemDAO) mas em contexto de encomendas._

---

### Bugs de Logica em Fluxos Especificos

#### Issue #DL017 — PagamentoFreteController quebra com valores >= R$1.000 (parser de locale)
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/PagamentoFreteController.java`
- **Linha(s):** 50, 88-96
- **Problema:** `setTotalFrete` formata com `DecimalFormat("#,##0.00")` gerando "1.234,56". O parser `parseDoubleSafe` faz `.replace(",",".")` mas NAO remove o ponto de milhar, gerando "1.234.56" que `parseDouble` rejeita → retorna 0.0.
- **Impacto:** Qualquer frete >= R$1.000 mostra valor R$0,00 no pagamento. Pagamento registrado incorretamente.
- **Codigo problematico:**
```java
private double parseDoubleSafe(String t) {
    t = t.replace(",", ".");  // "1.234,56" → "1.234.56" → FALHA
    return Double.parseDouble(t);  // NumberFormatException → 0.0
}
```
- **Fix sugerido:**
```java
t = t.replace(".", "").replace(",", ".");  // "1.234,56" → "1234.56"
```
- **Observacoes:**
> _Bug reproduzivel com qualquer frete de valor >= R$1.000. Silenciosamente retorna zero._

---

#### Issue #DL018 — QuitarDividaEncomendaTotal carrega usuarios como "caixas"
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/QuitarDividaEncomendaTotalController.java`
- **Linha(s):** 67-76
- **Problema:** ComboBox "Caixa" carrega `nome_completo FROM usuarios` em vez de registros da tabela de caixas. Valor gravado sera nome de usuario, nao caixa real.
- **Impacto:** Dados de caixa inconsistentes com o resto do sistema. Relatorios por caixa incorretos.
- **Codigo problematico:**
```java
ResultSet rs = c.prepareStatement("SELECT nome_completo FROM usuarios").executeQuery()
```
- **Fix sugerido:** Usar `CaixaDAO.listarTodos()` ou query na tabela `cad_caixa`.
- **Observacoes:**
> _Todos os outros controllers usam tabela de caixas corretamente. Este e o unico errado._

---

#### Issue #DL019 — TabelaPrecosEncomenda: CRUD apenas em memoria, nunca persiste
- [ ] **Concluido**
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/TabelaPrecosEncomendaController.java`
- **Linha(s):** 293-296, 347-349
- **Problema:** Adicionar, editar e excluir itens da tabela de precos operam apenas em `masterData` (lista em memoria). DAO e carregado para leitura mas nunca chamado para escrita.
- **Impacto:** Usuario edita tabela de precos, parece funcionar, mas tudo e perdido ao fechar tela.
- **Codigo problematico:**
```java
masterData.add(novo);  // "Por enquanto, apenas em memória"
masterData.remove(selecionado);  // "Por enquanto, apenas em memória"
```
- **Fix sugerido:** Chamar `itemDAO.inserir(novo)`, `itemDAO.atualizar()`, `itemDAO.excluir()`.
- **Observacoes:**
> _Comentario no codigo confirma que e feature incompleta._

---

#### Issue #DL020 — Coluna de logo com nomes diferentes em queries do mesmo tabela
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** ListarPassageirosViagemController:87 vs ListaFretesController:301
- **Linha(s):** Ver acima
- **Problema:** Mesma tabela `configuracao_empresa` referenciada como `path_logo` em um controller e `caminho_foto` em outro. Uma das queries falha.
- **Impacto:** Logo nao aparece em relatorios de uma das telas (erro silenciado por catch).
- **Codigo problematico:**
```java
// ListarPassageirosViagemController:87
"SELECT nome_embarcacao, cnpj, endereco, telefone, path_logo FROM configuracao_empresa"
// ListaFretesController:301
"SELECT caminho_foto, nome_embarcacao, cnpj, telefone FROM configuracao_empresa"
```
- **Fix sugerido:** Verificar nome real da coluna e padronizar.
- **Observacoes:**
> _Uma das queries gera SQLException silenciada — logo nunca carrega nessa tela._

---

#### Issue #DL021 — BalancoViagemDAO retorna dados parciais em caso de erro sem indicacao
- [ ] **Concluido**
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 46-48, 67-69, 88-90
- **Problema:** Cada secao (passagens, encomendas, fretes, saidas) tem catch que imprime erro e continua. Se encomendas falhar, balanco mostra passagens+fretes mas zero encomendas — parecendo valido.
- **Impacto:** Relatorio de balanco financeiro incorreto sem alerta. Decisoes baseadas em dados incompletos.
- **Fix sugerido:** Propagar excecao ou marcar DadosBalancoViagem como incompleto.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL022 — Filtros de relatorio ignorados em PassagemDAO.filtrarRelatorio
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 239-269
- **Problema:** Parametros `viagemStr`, `rotaStr`, `tipoPagamento`, `caixa`, `agente`, `tipoPassagem` aceitos mas nunca usados na query. WHERE filtra apenas por data, nome e status.
- **Impacto:** Usuario seleciona filtros na UI, ve dados nao-filtrados. Relatorios incorretos.
- **Fix sugerido:** Implementar os filtros faltantes na construcao da query.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL023 — CAST(numero_encomenda AS INTEGER) crash com dados nao-numericos
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 48, 163
- **Problema:** `ORDER BY CAST(numero_encomenda AS INTEGER)` e `MAX(CAST(...))` falham se qualquer registro tiver texto.
- **Impacto:** Listagem e geracao de numero falham completamente (PSQLException).
- **Fix sugerido:** Armazenar como INTEGER ou usar `WHERE numero_encomenda ~ '^\d+$'`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

### Validacao de Input

#### Issue #DL024 — Viagem com data no passado permitida
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroViagemController.java`
- **Linha(s):** 279-329
- **Problema:** Valida `dataChegada >= dataPartida` mas nao impede data de partida no passado.
- **Impacto:** Viagem criada com data passada afeta dados financeiros retroativos.
- **Fix sugerido:** `if (dataPartida.isBefore(LocalDate.now())) alert("Data no passado");`
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL025 — BalancoViagemController re-parse de moeda formatada e fragil
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/BalancoViagemController.java`
- **Linha(s):** 416-419
- **Problema:** Re-parse de string "R$ 1.234,56" com `replace` manual. Se formato do NumberFormat mudar (ex: espaco nao-quebravel em Java moderno), parse retorna 0.0 silenciosamente.
- **Impacto:** Relatorio impresso com valores zerados.
- **Codigo problematico:**
```java
val = Double.parseDouble(p[1].replace("R$","").replace(".","").replace(",",".").trim());
```
- **Fix sugerido:** Manter valor numerico original separado da formatacao.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL026 — Calculo de dias comerciais com off-by-one em GestaoFuncionarios
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/GestaoFuncionariosController.java`
- **Linha(s):** 310, 346-359
- **Problema:** `calcularDiasComerciais` retorna `dias + 1`, fazendo 1-30 = 31 dias. Depois cap em 30. Salario proporcional calculado com `Math.round(salarioAcumulado * 100.0) / 100.0`.
- **Impacto:** Funcionarios que trabalham mes inteiro (30 dias) calculados como 31 → 30. Ok para mes cheio, mas meses parciais podem ter +1 dia a mais.
- **Fix sugerido:** Remover `+1` ou documentar regra de negocio.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

#### Issue #DL027 — JavaFX UI modificada de thread background em FinanceiroPassagensController
- [ ] **Concluido**
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/FinanceiroPassagensController.java`
- **Linha(s):** 219-229
- **Problema:** `Task.call()` modifica UI diretamente (`tabela.setItems()`, `lblTotal.setText()`). Viola regra de threading JavaFX.
- **Impacto:** Corrupcao intermitente de UI, exceptions aleatorias, freezes.
- **Fix sugerido:** Usar `Platform.runLater()` ou retornar dados do Task e aplicar no `succeeded`.
- **Observacoes:**
> _(sem observacoes adicionais)_

---

## COBERTURA

| Diretorio | Arquivos | Analisados | Issues novas |
|-----------|----------|-----------|-------------|
| src/dao/ | 28 | 28 (100%) | 11 |
| src/database/ | 2 | 2 (100%) | 0 |
| src/gui/ | 55 | 55 (100%) | 14 |
| src/gui/util/ | 5 | 5 (100%) | 0 |
| src/model/ | 26 | 26 (100%) | 1 |
| src/tests/ | 5 | 5 (100%) | 0 |
| database_scripts/ | 7 | 7 (100%) | 1 |
| Configs | 3 | 3 (100%) | 0 |
| **TOTAL** | **131** | **131 (100%)** | **27** |

---

## PLANO DE CORRECAO

### Urgente (CRITICO)

- [ ] #DL001 — Race condition bilhete (MAX+1) — **Esforco:** 30min (criar sequence)
- [ ] #DL002 — Race condition encomenda (MAX+1) — **Esforco:** 30min (criar sequence)
- [ ] #DL005 — Viagem excluir sem cascade — **Esforco:** 1h (transacao + deletes filhos)
- [ ] #DL009 — Desconto excede total — **Esforco:** 15min (validacao no confirmar)
- [ ] #DL017 — Parser quebra com >= R$1.000 — **Esforco:** 5min (fix no replace)
- [ ] #DL018 — Caixa carrega usuarios — **Esforco:** 15min (trocar query)
- [ ] #DL019 — Tabela precos nao persiste — **Esforco:** 1h (chamar DAO)
- **Notas:**
> _DL017 e fix de 1 linha. DL018 e DL009 sao fixes de 15min. Priorizar estes._

### Importante (ALTO)

- [ ] #DL003 — Quitacao sem transacao — **Esforco:** 30min
- [ ] #DL010 — Pagamento parcial sobrescreve desconto — **Esforco:** 2h (redesign)
- [ ] #DL011 — Frete nao grava forma pagamento — **Esforco:** 30min
- [ ] #DL012 — Frete ignora desconto anterior — **Esforco:** 30min
- [ ] #DL015 — getSaldoDevedor negativo — **Esforco:** 5min (max zero)
- [ ] #DL016 — ILIKE wildcard em quitacao encomendas — **Esforco:** 1h
- [ ] #DL020 — Coluna logo com nomes diferentes — **Esforco:** 15min
- [ ] #DL021 — Balanco retorna dados parciais — **Esforco:** 1h
- **Notas:**
> _DL015 e fix de 1 linha. DL010 requer redesign (tabela de pagamentos separada)._

### Importante (MEDIO)

- [ ] #DL004 — TOCTOU embarcacao — **Esforco:** 30min
- [ ] #DL006 — Excluir embarcacao/rota sem ref check — **Esforco:** 30min
- [ ] #DL007 — Excluir auxiliar sem ref check — **Esforco:** 1h (7 metodos)
- [ ] #DL008 — Insert auxiliar sem duplicate check — **Esforco:** 1h (7 metodos)
- [ ] #DL013 — Estorno +0.01 tolerancia — **Esforco:** 5min
- [ ] #DL014 — Parcela boleto sem resto — **Esforco:** 15min
- [ ] #DL022 — Filtros relatorio ignorados — **Esforco:** 1h
- [ ] #DL023 — CAST crash non-numeric — **Esforco:** 30min
- [ ] #DL024 — Viagem data passado — **Esforco:** 5min
- [ ] #DL025 — Parse moeda fragil — **Esforco:** 30min
- [ ] #DL026 — Off-by-one dias comerciais — **Esforco:** 15min
- [ ] #DL027 — JavaFX threading violation — **Esforco:** 30min
- **Notas:**
> _DL013 e DL024 sao fixes de 1 linha._

---

## NOTAS

> **Padrao dominante #1: Sem transacoes.** A maioria das operacoes multi-step roda em auto-commit. Apenas EncomendaDAO.excluir e ViagemDAO.definirViagemAtiva usam transacoes. Todo o resto esta vulneravel a falhas parciais.
>
> **Padrao dominante #2: Geracao de ID por MAX+1.** Passagens e encomendas usam MAX+1 em vez de sequences, criando window de race condition em qualquer uso concorrente.
>
> **Padrao dominante #3: Subsistema Frete inconsistente com Encomenda/Passagem.** Fretes usam double (Passagens usam BigDecimal), nao gravam forma de pagamento (Encomendas gravam), e tem o bug do ternario (#006). O subsistema de fretes precisa de atencao especial.
>
> **Bug mais impactante: DL017.** PagamentoFreteController silenciosamente zera qualquer frete >= R$1.000. Este e provavelmente o bug com maior impacto financeiro imediato se o sistema esta em uso.
>
> **Comparacao com scan:** O audit-scan encontrou 9 issues de logica. O deep audit encontrou 27 adicionais. Total ativo: **36 issues de logica**.

---
*Gerado por Claude Code (Deep Audit) — Revisao humana obrigatoria*
