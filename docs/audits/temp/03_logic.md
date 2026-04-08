# Cat 3 — Logica de Negocio
> Audit V1.0 | 2026-04-07

---

#### Issue #027 — Valores monetarios em double em vez de BigDecimal (6+ modelos)
- **Severidade:** CRITICO
- **Arquivo:** `src/model/Frete.java`, `src/model/ReciboAvulso.java`, `src/model/ReciboQuitacaoPassageiro.java`, `src/model/FreteItem.java`, `src/model/DadosBalancoViagem.java`, `src/model/ItemResumoBalanco.java`, `src/model/LinhaDespesaBalanco.java`
- **Linha(s):** Frete:15-17 | ReciboAvulso:10 | ReciboQuitacaoPassageiro:9 | FreteItem:10-13 | DadosBalancoViagem:10-13,19 | ItemResumoBalanco:8 | LinhaDespesaBalanco:6
- **Problema:** Campos financeiros usam `double` em vez de `BigDecimal`. Aritmetica de ponto flutuante causa erros de arredondamento (ex: 0.1 + 0.2 != 0.3). Inconsistente — Passagem, Encomenda e Tarifa usam BigDecimal corretamente.
- **Impacto:** Totais financeiros divergem ao longo do tempo. Relatorios nao batem. Clientes podem ser cobrados a mais ou a menos.
- **Codigo problematico:**
```java
// Frete.java
private double valorNominal;
private double valorDevedor;
private double valorPago;
// DadosBalancoViagem.java
private double totalPassagens = 0;
private double totalEncomendas = 0;
private double totalFretes = 0;
```
- **Fix sugerido:** Migrar todos os campos monetarios para `BigDecimal`.

---

#### Issue #028 — Valores monetarios em double nos controllers (pervasivo)
- **Severidade:** CRITICO
- **Arquivo:** BaixaPagamentoController, FinanceiroEncomendasController, ExtratoClienteEncomendaController, BalancoViagemController, EstornoPagamentoController, CadastroBoletoController, GerarReciboAvulsoController, CadastroFreteController
- **Linha(s):** BaixaPagamento:27-29,34-36 | FinanceiroEncomendas:238-240 | ExtratoCliente:47,142-143 | Balanco:72,173,203 | Estorno:27,33-34 | Boleto:124,126 | ReciboAvulso:313 | CadastroFrete:564,997-1001
- **Problema:** Todos os controllers financeiros usam `double` para calculos monetarios.
- **Impacto:** Complementa Issue #027 — erros se propagam do model ao controller e de volta ao banco.
- **Codigo problematico:**
```java
double totalOriginal, jaPago, restanteOriginal, desconto, valorPago, troco;
```
- **Fix sugerido:** Usar BigDecimal em toda a cadeia: model → controller → DAO → banco.

---

#### Issue #029 — Transacao ausente em insercao de Encomenda + Itens
- **Severidade:** ALTO
- **Arquivo:** `src/dao/EncomendaDAO.java` + `src/dao/EncomendaItemDAO.java`
- **Linha(s):** EncomendaDAO:14, EncomendaItemDAO:14
- **Problema:** Inserir encomenda e seus itens sao chamadas DAO separadas em conexoes diferentes. Se encomenda inserir mas itens falharem, encomenda orfã persiste.
- **Impacto:** Dados inconsistentes — encomendas sem itens no banco. Afeta relatorios e financeiro.
- **Codigo problematico:**
```java
// EncomendaDAO.inserir() — conexao 1
// EncomendaItemDAO.inserir() — conexao 2 (separada)
```
- **Fix sugerido:** Usar mesma Connection com transacao atomica para ambas as operacoes.

---

#### Issue #030 — Tolerancia inconsistente para status "PAGO"
- **Severidade:** ALTO
- **Arquivo:** FinanceiroEncomendasController, ExtratoClienteEncomendaController
- **Linha(s):** FinanceiroEncomendas:241,270,294 | ExtratoCliente:168,307
- **Problema:** Threshold de 0.01 usado com operadores diferentes (`<=`, `>=`, `>`) em pontos diferentes do codigo. Com `double`, 0.01 nao e representavel exatamente.
- **Impacto:** Mesmo pagamento pode ser classificado como "PAGO" em um ponto e "PENDENTE" em outro.
- **Codigo problematico:**
```java
// FinanceiroEncomendasController:241
novoPago >= totalComDesconto - 0.01  // usa >=
// ExtratoClienteEncomendaController:307
saldo <= 0.01  // usa <=
// FinanceiroEncomendasController:294
novoPago > 0.01  // usa >
```
- **Fix sugerido:** Centralizar constante e logica de comparacao. Usar BigDecimal com `compareTo`.

---

#### Issue #031 — buscarViagemAtiva e buscarViagemMarcadaComoAtual sao identicas
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 114, 140
- **Problema:** Ambos os metodos fazem `WHERE v.is_atual = TRUE LIMIT 1`. O fallback nunca retornara algo diferente do metodo original.
- **Impacto:** Dead code que confunde manutencao. Fallback inutil.
- **Codigo problematico:**
```java
// buscarViagemAtiva() — WHERE v.is_atual = TRUE ORDER BY ... LIMIT 1
// buscarViagemMarcadaComoAtual() — WHERE v.is_atual = TRUE LIMIT 1
```
- **Fix sugerido:** Remover metodo duplicado.

---

#### Issue #032 — Stubs de DAO que nao salvam no banco
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ClienteDAO.java`, `src/dao/ConferenteDAO.java`, `src/dao/RemetenteDAO.java`
- **Linha(s):** ClienteDAO:35 | ConferenteDAO:22 | RemetenteDAO:22
- **Problema:** Metodo `inserir()` apenas imprime no stdout — nao persiste no banco.
- **Impacto:** Usuario cadastra cliente/conferente/remetente, ve mensagem de sucesso, mas dado nao e salvo.
- **Codigo problematico:**
```java
public void inserir(Cliente c) {
    System.out.println("Cliente: " + c.getNome());  // NAO salva no banco
}
```
- **Fix sugerido:** Implementar INSERT real com PreparedStatement.

---

#### Issue #033 — CidadeDAO retorna lista hardcoded
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/CidadeDAO.java`
- **Linha(s):** 11-15
- **Problema:** Retorna lista fixa de cidades em vez de consultar o banco.
- **Impacto:** Novas cidades so podem ser adicionadas via alteracao de codigo-fonte.
- **Fix sugerido:** Criar tabela `cidades` e buscar do banco.

---

#### Issue #034 — Encomenda armazena data como String
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Encomenda.java`
- **Linha(s):** 25
- **Problema:** `dataLancamento` e `String` enquanto todos os outros modelos usam `LocalDate`.
- **Impacto:** Comparacoes de data, ordenacao e formatacao inconsistentes.
- **Codigo problematico:**
```java
private String dataLancamento;
```
- **Fix sugerido:** Mudar para `LocalDate`.

---

#### Issue #035 — SyncClient.processarRegistroRecebido nao implementado
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 353-358
- **Problema:** Branches de UPDATE e INSERT tem apenas comentarios — nao executam nada.
- **Impacto:** Sincronizacao recebe dados do servidor mas nunca os grava localmente.
- **Codigo problematico:**
```java
// UPDATE e INSERT branches — apenas comentarios, sem implementacao
```
- **Fix sugerido:** Implementar a logica de sincronizacao ou remover feature.

---

## Arquivos nao cobertos
Nenhum — cobertura completa.
