# Contra-Verificacao — Audit V1.0
> Data: 2026-04-07
> Revisao de 64 issues em 6 categorias
> Metodo: Leitura direta do codigo-fonte nas linhas exatas referenciadas

## Resumo da Revisao

- **Issues verificadas:** 64
- **Confirmadas sem alteracao:** 55
- **Falsos positivos descartados:** 1
- **Severidades ajustadas:** 7
- **Fixes corrigidos:** 0
- **Novos problemas encontrados:** 1

---

## Falsos Positivos Descartados

| Issue | Motivo do descarte |
|-------|-------------------|
| **#008** — Divisao por zero em ExtratoClienteEncomendaController:242 | Guard `if (dividaTotalAtual <= 0) return;` na linha 218 garante que `dividaTotalAtual > 0` antes da divisao. Alem disso, linha 243 tem `if (fatorPagamento < 0) fatorPagamento = 0;` como protecao adicional. Nao ha race condition possivel pois e thread JavaFX (single-thread UI). **Nao e bug.** |

---

## Severidades Ajustadas

| Issue | De | Para | Motivo |
|-------|-----|------|--------|
| **#002** — ResultSet nao fechado (13 locais) | CRITICO | BAIXO | Per spec JDBC, fechar PreparedStatement fecha automaticamente seu ResultSet. Todos os locais referenciados tem o PS em try-with-resources. E code smell (estilo inconsistente), nao leak real. |
| **#007** — DDL dentro de transacao | ALTO | MEDIO | PostgreSQL suporta DDL transacional nativamente. `CREATE TABLE IF NOT EXISTS` e efetivamente no-op apos primeira execucao. Problema real e design (DDL nao pertence a runtime), nao risco de crash. |
| **#019** — SQL injection via nomes de tabela | ALTO | MEDIO | Todos os callers verificados passam strings hardcoded internas (constantes do codigo), nao input de usuario. Risco e futuro (se caller mudar), nao presente. Boa pratica e validar, mas nao e exploravel hoje. |
| **#022** — Token exibido em Alert | MEDIO | BAIXO | Acao iniciada pelo proprio usuario (clica botao "mostrar token"). Risco e shoulder-surfing, nao vulnerabilidade de codigo. |
| **#025** — Command injection em LogService | MEDIO | BAIXO | Path vem de `File.getAbsolutePath()` construido internamente, nao de input de usuario. Problema real e robustez com espacos no path, nao injection exploravel. |
| **#030** — Tolerancia inconsistente para PAGO | ALTO | MEDIO | Operadores diferentes (`>=`, `<=`, `>`) servem propositos distintos em contextos diferentes (pagamento vs estorno vs exibicao). Sao matematicamente equivalentes nos respectivos contextos. Problema real e falta de centralizacao, nao inconsistencia logica. |
| **#031** — Metodos duplicados de viagem | MEDIO | BAIXO | Queries NAO sao identicas — `buscarViagemAtiva` tem ORDER BY, `buscarViagemMarcadaComoAtual` nao tem. Porem, como ambas filtram `is_atual = TRUE`, o fallback e efetivamente dead code (se primeiro nao acha, segundo tambem nao). Reclassificar como dead code/confusao, nao bug logico. |

---

## Fixes Corrigidos

| Issue | Problema no fix original | Fix revisado |
|-------|------------------------|-------------|
| *(nenhum)* | Todos os fixes sugeridos sao tecnicamente corretos e nao introduzem bugs novos. | — |

---

## Novos Problemas Encontrados

#### Issue #065 — validarPermissaoGerente em FinanceiroSaidaController usa texto plano
- **Severidade:** CRITICO
- **Arquivo:** `src/gui/FinanceiroSaidaController.java`
- **Linha(s):** 413, 458
- **Problema:** O unico controller com verificacao de autorizacao (`validarPermissaoGerente`) compara senha digitada diretamente com `senha_hash` via SQL `=`, mesmo padrao quebrado das issues #016/#018. A unica funcao de auth no sistema inteiro esta implementada de forma insegura.
- **Impacto:** Se senhas forem BCrypt, autorizacao de exclusao de saida NUNCA funciona. Se senhas forem texto plano, a "protecao" e ilusoria.
- **Codigo problematico:**
```java
stmt.setString(1, senha);  // texto plano comparado com senha_hash via SQL =
```
- **Fix sugerido:** Buscar hash por funcao/login e comparar com `BCrypt.checkpw()`.

---

## Pontos Cegos Declarados

1. **Arquivos FXML nao auditados em profundidade** — 38 arquivos .fxml foram identificados mas nao lidos linha por linha. Podem conter bindings problematicos, event handlers inline, ou referencias a controllers inexistentes. Risco baixo (FXML e declarativo).

2. **Relatorios JasperReports** — Pasta `relatorios/` nao foi inspecionada. Templates .jrxml podem conter SQL inline com injection, ou formulas de calculo incorretas.

3. **Arquivos binarios em lib/** — 45 JARs nao foram verificados contra CVE databases. Notavelmente, `log4j-1.2.17.jar` e conhecidamente vulneravel (CVE-2019-17571, CVE-2021-4104), embora Log4j 1.x nao seja afetado pelo Log4Shell (CVE-2021-44228, que afeta 2.x).

4. **CSS files** — Verificados, sem problemas encontrados.

5. **Conteudo do banco de dados em producao** — Nao e possivel verificar se as senhas estao realmente em texto plano ou BCrypt sem acesso ao banco. A auditoria assume o pior caso baseado no codigo.

---

## Tabela Final Consolidada — Pos-Revisao

### Contagem por severidade (atualizada)

| Severidade | Scan original | Pos-revisao | Delta |
|------------|--------------|-------------|-------|
| CRITICO | 14 | 13 | -1 (descartado #008) |
| ALTO | 23 | 20 | -3 (rebaixados #019, #030 para MEDIO; #002 para BAIXO) |
| MEDIO | 18 | 21 | +3 (recebeu #007, #019, #030) |
| BAIXO | 9 | 11 | +2 (recebeu #002, #022, #025, #031; perdeu 0) |
| **TOTAL** | **64** | **63 + 1 novo = 64** | 1 descartado, 1 novo |

### Issues CRITICAS confirmadas (13):

| # | Issue | Arquivo principal |
|---|-------|-------------------|
| 001 | NPE em datas nullable | Multiplos DAOs |
| 003 | Connection leak ViagemDAO finally | ViagemDAO.java |
| 004 | Connection leak estornos (4 controllers) | Financeiro*Controller |
| 006 | Ternario "PENDENTE":"PENDENTE" | FinanceiroFretesController |
| 012 | Schema SQL diverge do modelo Java | criar_tabela_usuarios.sql |
| 014 | Senha BD hardcoded (3 arquivos) | ConexaoBD, DatabaseConnection |
| 015 | Senha diferente hardcoded | CadastroClienteController |
| 016 | Login compara texto plano com hash | LoginController |
| 017 | Fallback texto plano em estornos | EstornoPagamentoController |
| 018 | Admin validation texto plano | AuditoriaExclusoesSaida |
| 027 | double para dinheiro (models) | Frete, ReciboAvulso, DadosBalanco |
| 028 | double para dinheiro (controllers) | BaixaPagamento, Financeiro* |
| 036 | 68+ catch blocks vazios | Multiplos controllers |
| 065 | Auth unica do sistema usa texto plano | FinanceiroSaidaController |

> Nota: #065 foi adicionado durante a revisao.
