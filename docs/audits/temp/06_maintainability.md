# Cat 6 — Manutenibilidade
> Audit V1.0 | 2026-04-07

---

#### Issue #049 — 18 arquivos com mais de 500 linhas (God Classes)
- **Severidade:** ALTO
- **Arquivo:** Multiplos controllers
- **Linha(s):** N/A
- **Problema:** Controllers massivos misturando GUI, logica de negocio, acesso a banco e geracao de relatorios.

| Linhas | Arquivo |
|--------|---------|
| 2026 | VenderPassagemController.java |
| 2002 | CadastroFreteController.java |
| 1778 | InserirEncomendaController.java |
| 1661 | RelatorioFretesController.java |
| 1345 | TelaPrincipalController.java |
| 1005 | util/RelatorioUtil.java |
| 997 | ListaEncomendaController.java |
| 911 | GestaoFuncionariosController.java |
| 891 | FinanceiroSaidaController.java |
| 803 | ExtratoPassageiroController.java |
| 785 | util/SyncClient.java |
| 722 | RelatorioEncomendaGeralController.java |
| 689 | TabelasAuxiliaresController.java |
| 661 | GerarReciboAvulsoController.java |
| 548 | FinanceiroPassagensController.java |
| 548 | CadastroConferenteController.java |
| 541 | ListaFretesController.java |
| 537 | TabelaPrecosEncomendaController.java |

- **Impacto:** Dificuldade extrema de manutencao, testes e refatoracao.
- **Fix sugerido:** Extrair camada de servico (Service Layer) entre controllers e DAOs.

---

#### Issue #050 — Funcoes com mais de 50 linhas
- **Severidade:** MEDIO
- **Arquivo:** CadastroFreteController, SyncClient, FinanceiroPassagensController
- **Linha(s):** CadastroFrete:1240-1393 (~153 linhas) | SyncClient:197-256,511-582 | FinanceiroPassagens:~400-498 (~98 linhas)
- **Problema:** Metodos monoliticos que fazem validacao, acesso a banco, logica de negocio e UI update tudo junto.
- **Impacto:** Impossivel testar unitariamente. Bugs dificeis de isolar.
- **Fix sugerido:** Extrair sub-metodos com responsabilidade unica.

---

#### Issue #051 — Duas classes de conexao ao banco duplicadas
- **Severidade:** ALTO
- **Arquivo:** `src/dao/ConexaoBD.java` e `src/database/DatabaseConnection.java`
- **Linha(s):** Ambos completos
- **Problema:** Duas implementacoes identicas de conexao ao banco. Qual usar? Alteracoes precisam ser feitas em ambas.
- **Impacto:** Configuracao divergente (ja divergiu: senha diferente em CadastroClienteController). Confusao para novos devs.
- **Fix sugerido:** Remover `DatabaseConnection.java` e padronizar em `ConexaoBD.java`.

---

#### Issue #052 — Classe Auxiliares.java vazia (dead code)
- **Severidade:** BAIXO
- **Arquivo:** `src/model/Auxiliares.java`
- **Linha(s):** Classe inteira
- **Problema:** Corpo da classe completamente comentado. Compila mas nao faz nada.
- **Impacto:** Confusao — parece que deveria ter conteudo.
- **Fix sugerido:** Remover ou implementar.

---

#### Issue #053 — ApiConfig.java com getters/setters faltando
- **Severidade:** MEDIO
- **Arquivo:** `src/model/ApiConfig.java`
- **Linha(s):** 19-23
- **Problema:** Campos `id`, `nomeServico`, `provider`, `endpointUrl` sem getters/setters. Apenas `apiKey` e `ativo` tem acesso.
- **Impacto:** DAO nao consegue popular objeto completamente.
- **Fix sugerido:** Adicionar getters/setters para todos os campos.

---

#### Issue #054 — IDs com tipos inconsistentes entre modelos
- **Severidade:** MEDIO
- **Arquivo:** Multiplos modelos
- **Linha(s):** N/A
- **Problema:** Alguns usam `int`, outros `Long`, outros `long` (primitivo).

| Classe | Tipo ID |
|--------|---------|
| Passagem, Viagem, Embarcacao, Rota, Encomenda | Long |
| Usuario, Empresa, Caixa, Produto, TipoPassageiro | int |
| Frete | long (primitivo) |
| Tarifa | int (id) + long (rotaId) |

- **Impacto:** Conversoes implicitas, incompatibilidade em comparacoes, long primitivo nao pode ser null.
- **Fix sugerido:** Padronizar em `Long` (boxed) para todos os IDs.

---

#### Issue #055 — EncomendaItem mistura dois conceitos diferentes
- **Severidade:** MEDIO
- **Arquivo:** `src/model/EncomendaItem.java`
- **Linha(s):** 8-22
- **Problema:** Mesma classe serve para tela InserirEncomenda e CadastroProdutoController com subsets de campos diferentes.
- **Impacto:** Campos irrelevantes em cada contexto. Fragilidade na manutencao.
- **Fix sugerido:** Separar em duas classes ou usar DTO pattern.

---

#### Issue #056 — Passagem.java com 40+ campos misturando persistencia e display
- **Severidade:** MEDIO
- **Arquivo:** `src/model/Passagem.java`
- **Linha(s):** 9-71
- **Problema:** God Object — combina colunas do banco (linhas 9-46) com campos de exibicao (48-71).
- **Impacto:** Qualquer mudanca no banco ou na UI afeta a mesma classe.
- **Fix sugerido:** Separar Model (persistencia) de DTO/ViewModel (exibicao).

---

#### Issue #057 — .classpath com paths absolutos do Windows
- **Severidade:** ALTO
- **Arquivo:** `.classpath`
- **Linha(s):** 12-19
- **Problema:** Paths como `C:/javafx-sdk-23.0.2/lib/javafx-swt.jar` hardcoded. Projeto roda em Linux.
- **Impacto:** Projeto nao compila em nenhuma maquina sem JavaFX exatamente naquele path Windows.
- **Codigo problematico:**
```xml
<classpathentry kind="lib" path="C:/javafx-sdk-23.0.2/lib/javafx-swt.jar"/>
```
- **Fix sugerido:** Migrar para Maven/Gradle com dependencia JavaFX declarativa.

---

#### Issue #058 — Sem gerenciador de dependencias (45 JARs manuais)
- **Severidade:** ALTO
- **Arquivo:** `lib/`
- **Linha(s):** N/A
- **Problema:** 45 JARs copiados manualmente sem controle de versao, sem declaracao de dependencias transitivas.
- **Impacto:** Impossivel atualizar dependencias com seguranca. Possivel conflito de versoes (ex: commons-beanutils 1.9.2 E 1.9.4 coexistem). Log4j 1.2.17 tem CVEs conhecidas.
- **Fix sugerido:** Migrar para Maven ou Gradle.

---

#### Issue #059 — Mistura de Swing e JavaFX
- **Severidade:** MEDIO
- **Arquivo:** `src/gui/CadastroClienteController.java`
- **Linha(s):** 8, 94, 105
- **Problema:** Usa `javax.swing.JOptionPane` enquanto todo o resto do projeto usa JavaFX `Alert`.
- **Impacto:** Thread safety issues ao misturar toolkits. Inconsistencia visual.
- **Fix sugerido:** Substituir JOptionPane por Alert do JavaFX.

---

#### Issue #060 — Duplicacao de campos entre EncomendaItem e ItemEncomendaPadrao
- **Severidade:** BAIXO
- **Arquivo:** `src/model/EncomendaItem.java`, `src/model/ItemEncomendaPadrao.java`
- **Linha(s):** N/A
- **Problema:** Ambas tem: nomeItem, descricao, unidadeMedida, precoUnit, permiteValorDeclarado, ativo.
- **Impacto:** Alteracoes precisam ser replicadas em ambas.
- **Fix sugerido:** EncomendaItem deveria referenciar ou estender ItemEncomendaPadrao.

---

#### Issue #061 — Nenhum teste unitario real
- **Severidade:** ALTO
- **Arquivo:** `src/tests/`
- **Linha(s):** Diretorio inteiro
- **Problema:** 5 arquivos de teste: 3 sao testes manuais de conexao, 1 lanca tela JavaFX, 1 e classe vazia. Zero testes automatizados.
- **Impacto:** Nenhuma protecao contra regressao. Impossivel refatorar com seguranca.
- **Fix sugerido:** Criar testes unitarios para modelos e logica de negocio. Testes de integracao para DAOs.

---

#### Issue #062 — Email duplicate check comentado em CadastroUsuarioController
- **Severidade:** BAIXO
- **Arquivo:** `src/gui/CadastroUsuarioController.java`
- **Linha(s):** 185
- **Problema:** Verificacao de email duplicado completamente comentada.
- **Impacto:** Usuarios duplicados podem ser criados com mesmo email.
- **Fix sugerido:** Descomentar ou implementar a validacao.

---

#### Issue #063 — Sem verificacao de autorizacao na maioria dos controllers
- **Severidade:** ALTO
- **Arquivo:** Todos os controllers exceto FinanceiroSaidaController
- **Linha(s):** N/A
- **Problema:** Apenas FinanceiroSaidaController (linha 358) verifica SessaoUsuario. Todos os outros operam sem checar permissoes do usuario logado.
- **Impacto:** Qualquer usuario que acessar uma tela pode executar qualquer operacao, independente de funcao/permissao.
- **Fix sugerido:** Implementar interceptor de autorizacao baseado em SessaoUsuario.getUsuarioLogado().getPermissoes().

---

#### Issue #064 — Scripts SQL sem transacao
- **Severidade:** MEDIO
- **Arquivo:** `database_scripts/001_adicionar_campos_sincronizacao.sql`
- **Linha(s):** Script inteiro
- **Problema:** Migracao altera 16 tabelas sem BEGIN/COMMIT. Se falhar na tabela 8, banco fica parcialmente migrado.
- **Impacto:** Estado de banco inconsistente sem rollback possivel.
- **Fix sugerido:** Envolver em `BEGIN; ... COMMIT;`.

---

## Arquivos nao cobertos
Nenhum — cobertura completa de src/, database_scripts/, resources/, e configs.
