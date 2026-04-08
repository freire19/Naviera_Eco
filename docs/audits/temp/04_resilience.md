# Cat 4 — Resiliencia e Error Handling
> Audit V1.0 | 2026-04-07

---

#### Issue #036 — 68+ catch blocks com corpo vazio em controllers
- **Severidade:** CRITICO
- **Arquivo:** Multiplos controllers (BalancoViagemController, FinanceiroEncomendasController, FinanceiroPassagensController, InserirEncomendaController, GerarReciboAvulsoController, GestaoFuncionariosController, etc.)
- **Linha(s):** BalancoViagem:165,205,354,418,430,443,446 | FinanceiroEncomendas:51 | FinanceiroPassagens:54 | InserirEncomenda:400,871,1207 | ReciboAvulso:313 | GestaoFuncionarios:184-191,527,539,551,564,576,658
- **Problema:** Catch blocks vazios `catch (Exception e) {}` que descartam erros silenciosamente. Nenhum log, nenhum alerta ao usuario.
- **Impacto:** Erros de banco, I/O, e logica acontecem sem que ninguem saiba. Debugging em producao impossivel. Dados podem ficar inconsistentes sem alerta.
- **Codigo problematico:**
```java
try {
    // operacao financeira critica
} catch (Exception e) {
    // NADA — erro engolido
}
```
- **Fix sugerido:**
```java
try {
    // operacao
} catch (Exception e) {
    LogService.registrarErro("Contexto", e);
    mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
}
```

---

#### Issue #037 — DAOs engolindo exceptions com printStackTrace + retorno default
- **Severidade:** ALTO
- **Arquivo:** Maioria dos DAOs — EncomendaDAO, ReciboAvulsoDAO, EncomendaItemDAO, ViagemDAO, PassagemDAO, etc.
- **Linha(s):** EncomendaDAO:43 | ViagemDAO:174-175 | PassagemDAO:196-201 | EncomendaDAO:195-203
- **Problema:** `catch (SQLException e) { e.printStackTrace(); return null; }` — caller nao distingue "sem dados" de "erro de banco".
- **Impacto:** Telas mostram listas vazias quando o banco esta fora do ar, sem feedback.
- **Codigo problematico:**
```java
} catch (SQLException e) {
    e.printStackTrace();
    return null;  // ou return new ArrayList<>()
}
```
- **Fix sugerido:** Propagar excecao ao caller ou retornar Optional para indicar ausencia vs erro.

---

#### Issue #038 — mapResultSet com catch vazios para colunas opcionais
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/ViagemDAO.java`, `src/dao/PassagemDAO.java`, `src/dao/EncomendaDAO.java`
- **Linha(s):** ViagemDAO:174-175 | PassagemDAO:196-201 | EncomendaDAO:195-203
- **Problema:** Campos opcionais lidos com `try { campo = rs.getX("col"); } catch (Exception) {}`. Se coluna for renomeada, o campo fica silenciosamente com valor default.
- **Impacto:** Dados incompletos exibidos sem indicacao de erro.
- **Codigo problematico:**
```java
try { viagem.setAtiva(rs.getBoolean("ativa")); } catch(Exception e) {}
```
- **Fix sugerido:** Verificar existencia da coluna com ResultSetMetaData ou usar query explicita.

---

#### Issue #039 — ScheduledExecutorService nao pode ser reutilizado apos shutdown
- **Severidade:** ALTO
- **Arquivo:** `src/gui/util/SyncClient.java`
- **Linha(s):** 143-146
- **Problema:** `scheduler.shutdown()` torna o executor inutilizavel. Re-iniciar sync automatica falha com RejectedExecutionException.
- **Impacto:** Funcionalidade de sync automatica quebra permanentemente apos primeiro stop.
- **Codigo problematico:**
```java
public void pararSyncAutomatica() {
    scheduler.shutdown();
}
```
- **Fix sugerido:**
```java
public void pararSyncAutomatica() {
    scheduler.shutdown();
    scheduler = Executors.newSingleThreadScheduledExecutor();
}
```

---

#### Issue #040 — Log de erros sem rotacao — cresce indefinidamente
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/util/LogService.java`
- **Linha(s):** Classe inteira
- **Problema:** Appends em `log_erros.txt` sem limite de tamanho, sem rotacao, sem cleanup.
- **Impacto:** Disco pode encher em producao. Arquivo de log fica ilegivel com o tempo.
- **Fix sugerido:** Implementar rotacao (ex: max 10MB, manter ultimos 5 arquivos) ou usar SLF4J/Logback ja presente no classpath.

---

#### Issue #041 — Debug println em codigo de producao
- **Severidade:** BAIXO
- **Arquivo:** `src/dao/AuxiliaresDAO.java`
- **Linha(s):** 304, 314
- **Problema:** `System.out.println` de debug deixado em producao.
- **Impacto:** Poluicao de stdout em producao.
- **Codigo problematico:**
```java
System.out.println("--- DAO: Entrando em listarPassagemAux ---");
System.out.println("--- DAO: Saindo de listarPassagemAux. Itens encontrados: " + lista.size() + " ---");
```
- **Fix sugerido:** Remover ou migrar para logger.

---

#### Issue #042 — Rollback incompleto em EncomendaDAO.excluir
- **Severidade:** MEDIO
- **Arquivo:** `src/dao/EncomendaDAO.java`
- **Linha(s):** 126-141
- **Problema:** Se o primeiro DELETE (encomenda_itens) falhar, o catch externo nao faz rollback — a conexao e fechada com autoCommit=false e comportamento e driver-dependent.
- **Impacto:** Possivel delete parcial sem rollback — itens deletados mas encomenda nao.
- **Fix sugerido:** Mover todo o bloco transacional para dentro do try-with-resources e garantir rollback em qualquer falha.

---

## Arquivos nao cobertos
Nenhum — cobertura completa.
