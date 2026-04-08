# Cat 2 — Seguranca
> Audit V1.0 | 2026-04-07
> **NOTA:** Rascunho intermediario. Issues #014, #015, #016, #017 foram RESOLVIDAS. Ver `docs/audits/current/DEEP_SECURITY.md` para status atualizado.

---

#### Issue #014 — Senha do banco hardcoded em 3 arquivos
- **Severidade:** CRITICO
- **Arquivo:** `src/dao/ConexaoBD.java`, `src/database/DatabaseConnection.java`, `src/tests/TesteConexaoPostgreSQL.java`
- **Linha(s):** ConexaoBD:21-23 | DatabaseConnection:8-10 | TesteConexaoPostgreSQL:10-12
- **Problema:** Credenciais do PostgreSQL (`postgres` / `123456`) hardcoded em codigo-fonte versionado.
- **Impacto:** Qualquer pessoa com acesso ao repositorio tem acesso total ao banco de dados.
- **Codigo problematico:**
```java
private static final String URL     = "jdbc:postgresql://localhost:5432/sistema_embarcacao"; 
private static final String USUARIO = "postgres";
private static final String SENHA   = "123456";
```
- **Fix sugerido:**
```java
private static final String URL     = System.getenv("DB_URL");
private static final String USUARIO = System.getenv("DB_USER");
private static final String SENHA   = System.getenv("DB_PASS");
```

---

#### Issue #015 — Senha DIFERENTE hardcoded em CadastroClienteController
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/CadastroClienteController.java`
- **Linha(s):** 73-75
- **Problema:** Bypassa ConexaoBD e usa DriverManager direto com senha diferente (`5904`). Indica que existem/existiram multiplas senhas para o banco.
- **Impacto:** Credencial adicional exposta. Conexao fora do padrao pode causar inconsistencias.
- **Codigo problematico:**
```java
String url = "jdbc:postgresql://localhost:5432/sistema_embarcacao";
String user = "postgres";
String pass = "5904";
```
- **Fix sugerido:** Usar `ConexaoBD.getConnection()` centralizado.

---

#### Issue #016 — Login compara senha em texto plano contra coluna senha_hash
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/LoginController.java`
- **Linha(s):** 77-83
- **Problema:** Query SQL compara senha digitada diretamente com `senha_hash` via `=`. Se senhas estiverem com BCrypt, login NUNCA funciona. Se funciona, senhas estao em texto plano no banco.
- **Impacto:** Autenticacao fundamentalmente quebrada ou insegura.
- **Codigo problematico:**
```java
String sql = "SELECT * FROM usuarios WHERE login_usuario = ? AND senha_hash = ? AND ativo = true";
stmt.setString(1, login);
stmt.setString(2, senha);  // compara texto plano com hash
```
- **Fix sugerido:**
```java
String sql = "SELECT * FROM usuarios WHERE login_usuario = ? AND ativo = true";
stmt.setString(1, login);
ResultSet rs = stmt.executeQuery();
if (rs.next()) {
    String hash = rs.getString("senha_hash");
    if (BCrypt.checkpw(senha, hash)) { /* login ok */ }
}
```

---

#### Issue #017 — Fallback de senha em texto plano para autorizacao de estornos
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/EstornoPagamentoController.java`
- **Linha(s):** 114
- **Problema:** Se o hash armazenado nao comeca com `$2a$`, aceita comparacao direta texto plano. Backdoor intencional.
- **Impacto:** Qualquer gerente com senha nao-hasheada pode ser impersonado para autorizar estornos financeiros.
- **Codigo problematico:**
```java
} else if (hashDoBanco != null && hashDoBanco.equals(senhaDigitada)) {
    senhaConfere = true;  // fallback texto plano
}
```
- **Fix sugerido:** Remover fallback. Forcar migracao de todas as senhas para BCrypt.

---

#### Issue #018 — Validacao de admin compara texto plano com senha_hash
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/AuditoriaExclusoesSaida.java`
- **Linha(s):** 250-257
- **Problema:** Mesmo padrao do login — compara senha digitada diretamente com `senha_hash` via SQL `=`.
- **Impacto:** Funcionalidade de auditoria de exclusoes quebrada se senhas forem BCrypt.
- **Codigo problematico:**
```java
String sql = "SELECT count(*) FROM usuarios WHERE senha_hash = ? AND (funcao = 'Gerente' OR funcao = 'Administrador')";
stmt.setString(1, senha);  // texto plano
```
- **Fix sugerido:** Buscar hash do admin e comparar com BCrypt.checkpw().

---

#### Issue #019 — SQL Injection via nomes de tabela/coluna concatenados
- **Severidade:** ALTO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`, `src/dao/TarifaDAO.java`, `src/gui/util/SyncClient.java`, `src/gui/ConfigurarSincronizacaoController.java`
- **Linha(s):** AuxiliaresDAO:29,89 | TarifaDAO:136 | SyncClient:268,335,346,378 | ConfigurarSincronizacaoController:130
- **Problema:** Nomes de tabela e coluna concatenados diretamente no SQL. Nao podem ser parametrizados com `?`. Atualmente chamados com strings hardcoded, mas a API e insegura.
- **Impacto:** Se qualquer caller futuro passar input do usuario, SQL injection completo.
- **Codigo problematico:**
```java
String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
```
- **Fix sugerido:** Validar contra whitelist de tabelas/colunas permitidas:
```java
private static final Set<String> TABELAS_PERMITIDAS = Set.of("cidades", "nacionalidades", ...);
if (!TABELAS_PERMITIDAS.contains(tabela)) throw new IllegalArgumentException("Tabela invalida");
```

---

#### Issue #020 — LIKE wildcard injection em quitacao de divida por nome
- **Severidade:** ALTO
- **Arquivo:** `src/dao/PassagemDAO.java`
- **Linha(s):** 312-325
- **Problema:** Busca com `ILIKE '%' + nomePassageiro + '%'` para quitar dividas. Nome parcial pode match multiplos passageiros.
- **Impacto:** Operacao financeira (quitar divida) pode afetar passageiros errados.
- **Codigo problematico:**
```java
stmt2.setString(1, "%" + nomePassageiro + "%");
rows = stmt2.executeUpdate();
```
- **Fix sugerido:** Usar busca por ID do passageiro em vez de nome com LIKE.

---

#### Issue #021 — Hash de senha logado em stderr
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/UsuarioDAO.java`
- **Linha(s):** 29
- **Problema:** Hash da senha impresso em stderr quando formato nao e BCrypt.
- **Impacto:** Se logs forem expostos, facilita ataques de cracking offline.
- **Codigo problematico:**
```java
System.err.println("AVISO: Tentativa de verificar senha com hash em formato não-BCrypt: " + hashArmazenado);
```
- **Fix sugerido:** Remover o hash da mensagem de log.

---

#### Issue #022 — Token de API exibido em Alert dialog
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 176
- **Problema:** Token de API mostrado em popup. Visivel em tela compartilhada ou gravacao.
- **Impacto:** Exposicao de credencial de API.
- **Codigo problematico:**
```java
mostrarAlerta(Alert.AlertType.INFORMATION, "Token", "Token atual: " + token);
```
- **Fix sugerido:** Mascarar token (mostrar apenas ultimos 4 caracteres).

---

#### Issue #023 — URL de producao em HTTP (nao HTTPS)
- **Severidade:** ALTO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 84, 105, 231, 332, 334
- **Problema:** URL de producao `http://sistemabarco.navdeusdealianca.com.br/api` usa HTTP puro. Token de API trafega sem criptografia.
- **Impacto:** Token e dados podem ser interceptados em rede via MITM.
- **Codigo problematico:**
```java
"http://sistemabarco.navdeusdealianca.com.br/api"
```
- **Fix sugerido:** Usar HTTPS: `https://sistemabarco.navdeusdealianca.com.br/api`

---

#### Issue #024 — Token de API armazenado em texto plano em properties
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/ConfigurarApiController.java`
- **Linha(s):** 257-258, 273-274
- **Problema:** Token e chave de nuvem salvos como texto plano em `api_config.properties`.
- **Impacto:** Qualquer usuario com acesso ao filesystem le as credenciais.
- **Codigo problematico:**
```java
props.setProperty("token", token);
props.setProperty("chave.nuvem", chaveNuvem);
```
- **Fix sugerido:** Usar criptografia ou keychain do OS.

---

#### Issue #025 — Command injection potencial em LogService
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** 130
- **Problema:** `Runtime.exec()` com path concatenado. Espacos ou caracteres especiais no path podem causar execucao inesperada.
- **Impacto:** Em cenario de path controlado por usuario, pode executar comandos arbitrarios.
- **Codigo problematico:**
```java
Runtime.getRuntime().exec("notepad.exe " + arquivo.getAbsolutePath());
```
- **Fix sugerido:**
```java
new ProcessBuilder("notepad.exe", arquivo.getAbsolutePath()).start();
```

---

#### Issue #026 — Sem .gitignore — credenciais e binarios versionados
- **Severidade:** ALTO
- **Arquivo:** Raiz do projeto
- **Linha(s):** N/A
- **Problema:** Projeto nao tem `.gitignore`. Pastas `bin/`, `.metadata/`, `.settings/`, e arquivos com credenciais estao no historico git.
- **Impacto:** Binarios compilados e metadata desnecessaria inflam o repo. Credenciais no historico mesmo que removidas dos fontes.
- **Fix sugerido:** Criar `.gitignore` e usar `git filter-branch` ou BFG para limpar historico.

---

## Arquivos nao cobertos
Nenhum — cobertura completa do codigo-fonte.
