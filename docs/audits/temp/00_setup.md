# Audit Setup — SistemaEmbarcacaoProjeto_Novo
> Versao planejada: V1.0
> Data: 2026-04-07
> Stack: Java (JavaFX + FXML) / PostgreSQL / JasperReports / Eclipse IDE

## Arvore do Projeto

```
SistemaEmbarcacaoProjeto_Novo/
├── src/                          # Codigo-fonte principal
│   ├── dao/                      # Data Access Objects (28 classes)
│   │   ├── ConexaoBD.java        # Conexao JDBC principal
│   │   ├── PassagemDAO.java      # CRUD passagens
│   │   ├── PassageiroDAO.java    # CRUD passageiros
│   │   ├── ViagemDAO.java        # CRUD viagens
│   │   ├── EncomendaDAO.java     # CRUD encomendas
│   │   ├── FreteDAO.java         # CRUD fretes
│   │   ├── EmpresaDAO.java       # CRUD empresas
│   │   ├── UsuarioDAO.java       # CRUD usuarios
│   │   ├── CaixaDAO.java         # CRUD caixa financeiro
│   │   ├── RotaDAO.java          # CRUD rotas
│   │   ├── TarifaDAO.java        # CRUD tarifas
│   │   ├── BalancoViagemDAO.java  # Balanco financeiro por viagem
│   │   ├── ClienteEncomendaDAO.java
│   │   ├── EncomendaItemDAO.java
│   │   ├── ItemFreteDAO.java
│   │   ├── ReciboAvulsoDAO.java
│   │   ├── ReciboQuitacaoPassageiroDAO.java
│   │   └── ... (mais 10 DAOs)
│   ├── database/                 # Conexao alternativa ao banco
│   │   └── DatabaseConnection.java
│   ├── gui/                      # Controllers JavaFX (50+ classes)
│   │   ├── Launch.java           # ENTRYPOINT principal
│   │   ├── LoginApp.java         # Inicializador da tela de login
│   │   ├── LoginController.java  # Logica de autenticacao
│   │   ├── TelaPrincipalApp.java # Inicializador tela principal
│   │   ├── TelaPrincipalController.java # Menu principal
│   │   ├── VenderPassagemController.java # Venda de passagens
│   │   ├── CadastroViagemController.java # Gestao de viagens
│   │   ├── InserirEncomendaController.java # Cadastro encomendas
│   │   ├── CadastroFreteController.java   # Cadastro fretes
│   │   ├── FinanceiroPassagensController.java
│   │   ├── FinanceiroEncomendasController.java
│   │   ├── FinanceiroFretesController.java
│   │   ├── FinanceiroEntradaController.java
│   │   ├── FinanceiroSaidaController.java
│   │   ├── BalancoViagemController.java
│   │   ├── RelatorioPassagensController.java
│   │   ├── RelatorioEncomendaGeralController.java
│   │   ├── RelatorioFretesController.java
│   │   ├── HistoricoEstornosController.java
│   │   ├── EstornoPagamentoController.java
│   │   ├── ConfigurarSincronizacaoController.java
│   │   ├── ConfigurarApiController.java
│   │   ├── TemaManager.java      # Gerenciador de temas (claro/escuro)
│   │   ├── *.fxml                # 38 telas FXML
│   │   ├── icons/                # Icones internos
│   │   └── util/                 # Utilitarios
│   │       ├── SessaoUsuario.java     # Sessao do usuario logado
│   │       ├── SyncClient.java        # Cliente de sincronizacao
│   │       ├── RelatorioUtil.java     # Geracao de relatorios
│   │       ├── LogService.java        # Servico de logs
│   │       └── AutoCompleteComboBoxListener.java
│   ├── model/                    # Entidades do dominio (26 classes)
│   │   ├── Passagem.java
│   │   ├── Passageiro.java
│   │   ├── Viagem.java
│   │   ├── Encomenda.java
│   │   ├── Frete.java
│   │   ├── Embarcacao.java
│   │   ├── Empresa.java
│   │   ├── Usuario.java
│   │   ├── Caixa.java
│   │   ├── Rota.java
│   │   ├── Tarifa.java
│   │   ├── Produto.java
│   │   ├── ClienteEncomenda.java
│   │   ├── ApiConfig.java
│   │   ├── DadosBalancoViagem.java
│   │   └── ... (mais 11 modelos)
│   └── tests/                    # Testes manuais (5 classes)
│       ├── TesteConexao.java
│       ├── TesteConexaoPostgreSQL.java
│       ├── TesteController.java
│       └── TesteApp.java
├── bin/                          # Classes compiladas (.class + FXML duplicados)
├── lib/                          # Bibliotecas JAR (45 dependencias)
├── database_scripts/             # Scripts SQL de migracao (7 scripts)
├── resources/                    # Recursos estaticos
│   ├── css/                      # Estilos (main.css, dark.css)
│   └── icons/                    # Icones do sistema (27 imagens)
├── relatorios/                   # Templates JasperReports
├── .classpath                    # Config Eclipse
├── .project                      # Config Eclipse
├── .settings/                    # Config Eclipse
├── .metadata/                    # Workspace Eclipse
├── impressoras.config            # Config de impressoras (A4 + termica)
├── sync_config.properties        # Config de sincronizacao com servidor
└── RELATORIO GERAL DO PROJETO... # Documento de requisitos
```

## Pontos de Entrada

| Entrypoint | Arquivo | Descricao |
|------------|---------|-----------|
| **Principal** | `src/gui/Launch.java` | `main()` — chama `LoginApp.main()` |
| **Login** | `src/gui/LoginApp.java` | Inicializa JavaFX e carrega `Login.fxml` |
| **Direto** | `src/gui/LaunchDireto.java` | Entrypoint alternativo (bypass login?) |
| **Teste** | `src/tests/TesteApp.java` | Testes manuais |

## Dependencias Principais

| Dependencia | Versao | Proposito |
|------------|--------|-----------|
| JavaFX | (bundled/JDK) | Framework GUI — telas FXML |
| PostgreSQL JDBC | 42.7.5 | Driver de conexao ao banco |
| SQLite JDBC | 3.49.1.0 | Banco local alternativo |
| JasperReports | 6.21.3 | Geracao de relatorios PDF |
| iText | 2.1.7 | Manipulacao de PDFs |
| OpenPDF | 1.3.32 | Renderizacao PDF alternativa |
| Jackson | 2.15.3 | Serializacao JSON/XML |
| JBCrypt | 0.4 | Hash de senhas |
| JFreeChart | 1.0.19 | Graficos |
| Ghost4J | 1.0.1 | Processamento PostScript |
| Tess4J | 3.4.8 | OCR (reconhecimento de texto) |
| Vosk | 0.3.38 | Reconhecimento de voz |
| PDFBox | 2.0.9 | Leitura/extracao de PDF |
| Commons IO | 2.6 | Utilitarios I/O |
| SLF4J + Logback | 1.7.25 / 1.2.3 | Logging |
| Log4j | 1.2.17 | Logging legado |
| JUnit | 4.12 | Testes unitarios |

## Fluxo de Dados Principal

```
[Usuario] --> Login.fxml --> LoginController
    |                             |
    |                    ConexaoBD.getConnection()
    |                             |
    |                    PostgreSQL (sistema_embarcacao)
    |                             |
    |                    Valida credenciais
    |                             |
    v                             v
TelaPrincipal.fxml <-- SessaoUsuario (guarda usuario logado)
    |
    ├── Vender Passagem --> VenderPassagemController --> PassagemDAO --> BD
    ├── Cadastro Viagem --> CadastroViagemController --> ViagemDAO --> BD
    ├── Encomendas --> InserirEncomendaController --> EncomendaDAO --> BD
    ├── Fretes --> CadastroFreteController --> FreteDAO --> BD
    ├── Financeiro --> Financeiro*Controller --> CaixaDAO --> BD
    ├── Relatorios --> Relatorio*Controller --> JasperReports --> PDF
    ├── Balanco --> BalancoViagemController --> BalancoViagemDAO --> BD
    └── Config --> ConfigurarSincronizacaoController --> sync_config.properties
```

**Padrao:** GUI (JavaFX/FXML) → Controller → DAO → PostgreSQL (JDBC direto, sem ORM)

## Variaveis de Ambiente

O projeto NAO usa variaveis de ambiente. Configuracoes sao hardcoded ou em arquivos:

| Arquivo | Variavel | Valor | Descricao |
|---------|----------|-------|-----------|
| `ConexaoBD.java` | URL | `jdbc:postgresql://localhost:5432/sistema_embarcacao` | URL do banco |
| `ConexaoBD.java` | USUARIO | `postgres` | Usuario do banco |
| `ConexaoBD.java` | SENHA | `123456` | **SENHA HARDCODED** |
| `DatabaseConnection.java` | URL/USUARIO/SENHA | (mesmos valores) | Classe duplicada de conexao |
| `sync_config.properties` | server.url | `http://localhost:8080` | URL do servidor de sync |
| `sync_config.properties` | api.token | (vazio) | Token de API |
| `sync_config.properties` | sync.auto | `false` | Sync automatico |
| `impressoras.config` | impressora.a4 | `Microsoft Print to PDF` | Impressora A4 |
| `impressoras.config` | impressora.termica | `EPSON TM-T20 Receipt` | Impressora termica |

## Observacoes Relevantes

- **2 classes de conexao ao BD** — `ConexaoBD.java` (dao/) e `DatabaseConnection.java` (database/) com mesma config
- **Senha do banco hardcoded** em texto plano em ambas as classes
- **45 JARs** na pasta lib/ sem gerenciador de dependencias (sem Maven/Gradle)
- **bin/ contem metadata Eclipse** e FXMLs duplicados (compilacao inclui workspace)
- **38 telas FXML** no src/gui/ — sistema complexo com muitos modulos
- **Sem .gitignore** — bin/, .metadata/, .settings/ estao versionados
