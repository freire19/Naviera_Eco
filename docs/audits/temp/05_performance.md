# Cat 5 — Performance
> Audit V1.0 | 2026-04-07

---

#### Issue #043 — Query N+1: 3 conexoes extras por passageiro no mapResultSet
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassageiroDAO.java`
- **Linha(s):** 171-186
- **Problema:** `mapResultSetToPassageiro()` abre 3 novas conexoes (via AuxiliaresDAO) para cada linha do ResultSet. Para N passageiros, sao 3N+1 conexoes.
- **Impacto:** 100 passageiros = 301 conexoes ao banco. Tela de listagem lenta. Pode exaurir conexoes do PostgreSQL.
- **Codigo problematico:**
```java
private Passageiro mapResultSetToPassageiro(ResultSet rs) throws SQLException {
    AuxiliaresDAO auxDAO = new AuxiliaresDAO();
    passageiro.setTipoDoc(auxDAO.buscarNomeAuxiliarPorId(...));   // +1 conexao
    passageiro.setSexo(auxDAO.buscarNomeAuxiliarPorId(...));       // +1 conexao
    passageiro.setNacionalidade(auxDAO.buscarNomeAuxiliarPorId(...)); // +1 conexao
}
```
- **Fix sugerido:** Usar JOINs na query SQL principal para trazer os nomes auxiliares na mesma consulta.

---

#### Issue #044 — Query N+1: 5 conexoes extras por passagem no mapResultSet
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 207-211
- **Problema:** Mesmo padrao — 5 lookups auxiliares por passagem. Lista de 200 passagens = 1001 conexoes.
- **Impacto:** Listagem de passagens extremamente lenta. Conexoes esgotam rapidamente.
- **Fix sugerido:** JOIN na query SQL.

---

#### Issue #045 — Sem connection pooling — nova conexao fisica por operacao
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ConexaoBD.java`, `src/database/DatabaseConnection.java`
- **Linha(s):** ConexaoBD:39-41 | DatabaseConnection:12-18
- **Problema:** Cada chamada a `getConnection()` / `conectar()` cria nova conexao JDBC via DriverManager. Nenhum pool (HikariCP, etc.).
- **Impacto:** Overhead de TCP handshake + autenticacao a cada operacao. Combinado com N+1, pode abrir centenas de conexoes por segundo.
- **Codigo problematico:**
```java
public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USUARIO, SENHA);
}
```
- **Fix sugerido:** Usar HikariCP (ja disponivel como JAR) com pool de 10-20 conexoes.

---

#### Issue #046 — System.out.println em cada conexao bem-sucedida
- **Severidade:** BAIXO
- **Arquivo:** `src/database/DatabaseConnection.java`
- **Linha(s):** 18
- **Problema:** Print no stdout a cada conexao. Com N+1, pode gerar centenas de linhas por tela.
- **Impacto:** I/O sincronizado de stdout desacelera hot paths.
- **Codigo problematico:**
```java
System.out.println("Conexao com o PostgreSQL realizada com sucesso! (DatabaseConnection.java)");
```
- **Fix sugerido:** Remover ou migrar para logger com nivel DEBUG.

---

#### Issue #047 — ORDER BY 1 fragil em queries
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/ReciboAvulsoDAO.java`
- **Linha(s):** 31, 47
- **Problema:** `ORDER BY 1` depende da posicao da coluna no SELECT *. Mudanca de schema altera ordenacao silenciosamente.
- **Impacto:** Resultados podem ficar desordenados apos alteracao de tabela.
- **Codigo problematico:**
```java
String sql = "SELECT * FROM recibos_avulsos WHERE id_viagem = ? ORDER BY 1 DESC";
```
- **Fix sugerido:** Usar nome explicito: `ORDER BY id DESC`.

---

#### Issue #048 — JSON parser customizado (250+ linhas) em vez de Jackson
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 449-704
- **Problema:** Parser JSON feito a mao (criarJsonSimples, parseJsonResponse, parseJsonArray, parseJsonObject, etc.) quando Jackson 2.15.3 ja esta no classpath (lib/).
- **Impacto:** Fragil para edge cases (unicode, nested objects, arrays de strings). Mais lento que Jackson.
- **Fix sugerido:** Substituir por `ObjectMapper` do Jackson.

---

## Arquivos nao cobertos
Nenhum — cobertura completa.
