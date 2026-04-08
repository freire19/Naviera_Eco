# Cat 1 — Bugs Criticos e Runtime
> Audit V1.0 | 2026-04-07

---

#### Issue #001 — NullPointerException em datas nullable nos DAOs
- **Severidade:** CRITICO
- **Arquivo:** Multiplos DAOs
- **Linha(s):** ReciboAvulsoDAO:71, AgendaDAO:120,142, PassagemDAO:161, ReciboQuitacaoPassageiroDAO:46, ViagemDAO:64
- **Problema:** Chamadas `.toLocalDate()` / `.toLocalDateTime()` em resultados de `rs.getDate()` / `rs.getTimestamp()` sem verificar null. Se o campo for NULL no banco, ocorre NPE.
- **Impacto:** Crash da aplicacao ao carregar registros com datas nulas.
- **Codigo problematico:**
```java
// ReciboAvulsoDAO.java:71
r.setDataEmissao(rs.getDate("data_emissao").toLocalDate());
// AgendaDAO.java:120
rs.getDate("data_evento").toLocalDate()
// PassagemDAO.java:161
rs.getDate("data_emissao").toLocalDate()
```
- **Fix sugerido:**
```java
Date dt = rs.getDate("data_emissao");
r.setDataEmissao(dt != null ? dt.toLocalDate() : null);
```

---

#### Issue #002 — ResultSet nao fechado em 13+ locais
- **Severidade:** CRITICO
- **Arquivo:** PassagemAuxDAO, TipoPassageiroDAO, EncomendaItemDAO, EncomendaDAO, ReciboAvulsoDAO, ReciboQuitacaoPassageiroDAO, BalancoViagemDAO, AgendaDAO, PassagemDAO
- **Linha(s):** PassagemAuxDAO:46,62 | TipoPassageiroDAO:71,87 | EncomendaItemDAO:40 | EncomendaDAO:52 | ReciboAvulsoDAO:35,50 | ReciboQuitacaoPassageiroDAO:40 | BalancoViagemDAO:34,58,79,104 | AgendaDAO:66,116 | PassagemDAO:289
- **Problema:** `ResultSet` criado via `ps.executeQuery()` sem try-with-resources. Se excecao ocorrer durante iteracao, o ResultSet nunca e fechado.
- **Impacto:** Leak de cursores do banco. Sob uso continuo, exaure cursores disponiveis do PostgreSQL.
- **Codigo problematico:**
```java
// PassagemAuxDAO.java:46
ResultSet rs = ps.executeQuery();
if(rs.next()) {
    return rs.getInt("id");
}
// rs nunca e fechado
```
- **Fix sugerido:**
```java
try (ResultSet rs = ps.executeQuery()) {
    if (rs.next()) {
        return rs.getInt("id");
    }
}
```

---

#### Issue #003 — Connection leak no finally de ViagemDAO.definirViagemAtiva
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ViagemDAO.java`
- **Linha(s):** 365
- **Problema:** No bloco finally, `conn.setAutoCommit(true)` e executado antes do null check, e `conn.close()` nao e chamado se setAutoCommit falhar. Alem disso, se conn for null, o acesso apos o `;` causa NPE engolido pelo catch vazio.
- **Impacto:** Conexao com banco nunca e liberada. Acumula conexoes orfas ate exaustao.
- **Codigo problematico:**
```java
finally {
    try { if(conn!=null) conn.setAutoCommit(true); conn.close(); } catch(Exception ex){}
}
```
- **Fix sugerido:**
```java
finally {
    if (conn != null) {
        try { conn.setAutoCommit(true); } catch (Exception ex) { }
        try { conn.close(); } catch (Exception ex) { }
    }
}
```

---

#### Issue #004 — Connection leak em 4 controllers de estorno/boleto (nunca chama close)
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroBoletoController.java`, `src/gui/FinanceiroEncomendasController.java`, `src/gui/FinanceiroFretesController.java`, `src/gui/FinanceiroPassagensController.java`
- **Linha(s):** CadastroBoletoController:131,180 | FinanceiroEncomendasController:296-329 | FinanceiroFretesController:307-355 | FinanceiroPassagensController:431-491
- **Problema:** Connection aberta com `ConexaoBD.getConnection()` fora de try-with-resources. O `finally` faz `con.setAutoCommit(true)` mas NUNCA chama `con.close()`.
- **Impacto:** Cada operacao de estorno ou boleto vaza uma conexao. Sob uso normal, exaure pool em horas.
- **Codigo problematico:**
```java
Connection con = ConexaoBD.getConnection();  // NAO em try-with-resources
con.setAutoCommit(false);
// ...
} finally { con.setAutoCommit(true); }  // NUNCA chama con.close()
```
- **Fix sugerido:**
```java
try (Connection con = ConexaoBD.getConnection()) {
    con.setAutoCommit(false);
    // ... operacoes
    con.commit();
} // close() automatico
```

---

#### Issue #005 — Connection leak em BalancoViagemDAO (campo de instancia)
- **Severidade:** ALTO
- **Arquivo:** `src/dao/BalancoViagemDAO.java`
- **Linha(s):** 11-14
- **Problema:** DAO armazena Connection como campo de instancia. Se o caller nao fechar a conexao (nao ha contrato), ela vaza. Usada para multiplas queries sem transacao.
- **Impacto:** Leak silencioso de conexao em cada consulta de balanco.
- **Codigo problematico:**
```java
private Connection connection;
public BalancoViagemDAO(Connection connection) {
    this.connection = connection;
}
```
- **Fix sugerido:**
```java
// Cada metodo deve abrir e fechar sua propria conexao, ou usar try-with-resources no caller
```

---

#### Issue #006 — Ternario com mesmo resultado nos dois branches (frete estorno)
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 305
- **Problema:** Ambos os branches do ternario retornam "PENDENTE". Bug de copy-paste — o branch true deveria retornar "PARCIAL".
- **Impacto:** Fretes parcialmente pagos ficam com status "PENDENTE" em vez de "PARCIAL". Relatorios financeiros mostram dados incorretos.
- **Codigo problematico:**
```java
String novoStatus = (novoPago > 0.01) ? "PENDENTE" : "PENDENTE";
```
- **Fix sugerido:**
```java
String novoStatus = (novoPago > 0.01) ? "PARCIAL" : "PENDENTE";
```

---

#### Issue #007 — DDL (CREATE TABLE) dentro de transacao em runtime
- **Severidade:** ALTO
- **Arquivo:** `src/gui/FinanceiroFretesController.java`
- **Linha(s):** 312-324
- **Problema:** `CREATE TABLE IF NOT EXISTS` executado dentro de transacao de estorno. Em PostgreSQL, DDL e transacional — se UPDATE subsequente falhar e rollback ocorrer, a tabela tambem e removida.
- **Impacto:** Tabela de log de estornos pode nao existir apos rollback, causando falhas em cascata.
- **Codigo problematico:**
```java
con.setAutoCommit(false);
String sqlCriarTabela = "CREATE TABLE IF NOT EXISTS log_estornos_fretes (...)";
try (PreparedStatement stmtCriar = con.prepareStatement(sqlCriarTabela)) {
    stmtCriar.executeUpdate();
}
```
- **Fix sugerido:** Mover DDL para scripts de migracao em `database_scripts/`.

---

#### Issue #008 — Divisao por zero potencial em calculo financeiro
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ExtratoClienteEncomendaController.java`
- **Linha(s):** 242
- **Problema:** Guard `if (dividaTotalAtual <= 0) return;` na linha 218, mas entre o check e a divisao pode haver race condition (outro usuario pagando concorrentemente).
- **Impacto:** `NaN` ou `Infinity` gravado no banco via calculo financeiro.
- **Codigo problematico:**
```java
double fatorPagamento = (dividaTotalAtual - descontoTotal) / dividaTotalAtual;
```
- **Fix sugerido:**
```java
if (dividaTotalAtual <= 0) return;
double fatorPagamento = (dividaTotalAtual - descontoTotal) / dividaTotalAtual;
// Ou adicionar check defensivo imediatamente antes da divisao
```

---

#### Issue #009 — Unsafe cast Long em RotaDAO
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/RotaDAO.java`
- **Linha(s):** 26
- **Problema:** `rs.getObject("id")` retorna Integer se coluna for INTEGER, mas cast e para Long — causa ClassCastException.
- **Impacto:** Crash ao carregar rotas.
- **Codigo problematico:**
```java
rota.setId((Long) rs.getObject("id"));
```
- **Fix sugerido:**
```java
rota.setId(rs.getLong("id"));
```

---

#### Issue #010 — gerarProximoId retorna 0 em caso de erro
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Linha(s):** 53
- **Problema:** Se query de sequence falhar, retorna 0. Caller pode inserir usuario com ID 0.
- **Impacto:** Violacao de constraint ou sobrescrita de dados.
- **Codigo problematico:**
```java
return 0;
```
- **Fix sugerido:** Lancar excecao em vez de retornar valor sentinela.

---

#### Issue #011 — NullPointerException em campos BigDecimal nao inicializados de Passagem
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Passagem.java`
- **Linha(s):** 29-35
- **Problema:** Alguns campos BigDecimal inicializados com ZERO (linhas 39-41), outros ficam null (valorAlimentacao, valorTransporte etc). Chamadas `.compareTo()` no getter causam NPE.
- **Impacto:** Crash ao exibir passagens com campos financeiros nulos.
- **Codigo problematico:**
```java
private BigDecimal valorAlimentacao;  // null
private BigDecimal valorPagamentoDinheiro = BigDecimal.ZERO;  // ok
```
- **Fix sugerido:** Inicializar todos os campos BigDecimal com `BigDecimal.ZERO`.

---

#### Issue #012 — Schema SQL diverge do modelo Java (tabela usuarios)
- **Severidade:** CRITICO
- **Arquivo:** `database_scripts/criar_tabela_usuarios.sql` vs `src/model/Usuario.java`
- **Linha(s):** SQL inteiro vs Usuario.java:5-11
- **Problema:** SQL cria colunas `id`, `nome`, `senha`, `permissao`. Java espera `id_usuario`, `nome_completo`, `login_usuario`, `senha_hash`, `permissoes`, `ativo`. Mismatch total.
- **Impacto:** Script SQL cria tabela incompativel. DAOs falham com "column not found" em runtime.
- **Codigo problematico:**
```sql
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    permissao VARCHAR(50)
);
```
- **Fix sugerido:**
```sql
CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome_completo VARCHAR(100) NOT NULL,
    login_usuario VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    senha_hash VARCHAR(255) NOT NULL,
    funcao VARCHAR(50),
    permissoes VARCHAR(50),
    ativo BOOLEAN DEFAULT true
);
```

---

#### Issue #013 — EncomendaItemDAO usa Exception para descobrir nome de coluna
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaItemDAO.java`
- **Linha(s):** 44-48
- **Problema:** Tenta ler coluna "id", se falhar tenta "id_item". Usa exception para flow control.
- **Impacto:** Performance degradada e mascaramento de erros reais.
- **Codigo problematico:**
```java
try {
    item.setId(rs.getLong("id")); 
} catch (SQLException e) {
    try { item.setId(rs.getLong("id_item")); } catch (SQLException ex) {}
}
```
- **Fix sugerido:** Usar nome de coluna correto baseado na query SQL.

---

## Arquivos nao cobertos
Nenhum — todos os DAOs, models, controllers e scripts foram analisados.
