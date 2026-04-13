package gui;

import gui.util.AlertHelper;
import gui.util.AppLogger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Controller do wizard de primeira configuracao.
 * Guia o tecnico pelos passos: conexao PG, criar banco/migrations, empresa, salvar db.properties.
 */
public class SetupWizardController implements Initializable {

    // -- Sidebar step indicators --
    @FXML private HBox stepIndicator1;
    @FXML private HBox stepIndicator2;
    @FXML private HBox stepIndicator3;
    @FXML private HBox stepIndicator4;

    // -- Content --
    @FXML private Label lblStepTitle;
    @FXML private StackPane stepContent;
    @FXML private VBox step1, step2, step3, step4;

    // -- Step 1: Conexao --
    @FXML private TextField txtHost;
    @FXML private TextField txtPorta;
    @FXML private TextField txtNomeBanco;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnTestarConexao;
    @FXML private Label lblStatusConexao;

    // -- Step 2: Migrations --
    @FXML private CheckBox chkCriarBanco;
    @FXML private CheckBox chkRodarMigrations;
    @FXML private ProgressBar progressMigrations;
    @FXML private Label lblProgressDetail;
    @FXML private TextArea txtLog;
    @FXML private Button btnExecutarSetup;

    // -- Step 3: Empresa --
    @FXML private TextField txtEmpresaId;
    @FXML private TextField txtApiUrl;
    @FXML private TextField txtPoolSize;
    @FXML private TextField txtSyncLogin;
    @FXML private PasswordField txtSyncSenha;

    // -- Step 4: Resumo --
    @FXML private Label lblResumoConexao;
    @FXML private Label lblResumoBanco;
    @FXML private Label lblResumoEmpresa;
    @FXML private Label lblResumoApi;
    @FXML private Label lblResumoPool;

    // -- Footer --
    @FXML private Button btnVoltar;
    @FXML private Button btnProximo;
    @FXML private Button btnConcluir;

    private int currentStep = 1;
    private boolean conexaoTestadaOk = false;
    private boolean setupExecutado = false;

    private static final String STYLE_STEP_ACTIVE = "-fx-padding: 10; -fx-background-color: #059669; -fx-background-radius: 5;";
    private static final String STYLE_STEP_DONE = "-fx-padding: 10; -fx-background-color: #047857; -fx-background-radius: 5;";
    private static final String STYLE_STEP_INACTIVE = "-fx-padding: 10; -fx-background-radius: 5;";

    private static final String[] STEP_TITLES = {
        "Conexao com o Banco de Dados",
        "Criar Estrutura do Banco",
        "Configurar Empresa",
        "Revisar e Concluir"
    };

    // Migrations na ordem correta (000 e obrigatorio, demais sao incrementais)
    private static final String[] MIGRATION_FILES = {
        "000_schema_completo.sql",
        "001_adicionar_campos_sincronizacao.sql",
        "003_corrigir_sequence_viagem.sql",
        "004_adicionar_funcionario_id_saidas.sql",
        "005_criar_sequences_bilhete_encomenda.sql",
        "006_criar_indices_performance.sql",
        "007_criar_tabelas_log_estornos.sql",
        "008_tabelas_app.sql",
        "009_tabelas_lojas_parceiras.sql",
        "010_criar_sequence_frete.sql",
        "011_indice_trigram_frete_itens.sql",
        "012_bilhetes_digitais.sql",
        "013_multi_tenant.sql",
        "014_tenant_slug.sql",
        "015_gps_tracking.sql",
        "016_adicionar_coluna_local_armazenamento.sql",
        "017_criar_tabela_log_estornos_fretes.sql",
        "018_criar_tabela_usuarios.sql",
        "019_sync_trigger_bypass.sql"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateStepView();

        // Carrega valores existentes do db.properties se houver (setup parcial)
        File dbProps = new File("db.properties");
        if (dbProps.exists()) {
            try {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(dbProps)) {
                    props.load(fis);
                }
                String url = props.getProperty("db.url", "");
                if (url.startsWith("jdbc:postgresql://")) {
                    // Parse: jdbc:postgresql://host:porta/banco
                    String after = url.substring("jdbc:postgresql://".length());
                    String hostPort = after.contains("/") ? after.substring(0, after.indexOf('/')) : after;
                    String banco = after.contains("/") ? after.substring(after.indexOf('/') + 1) : "naviera_eco";
                    String host = hostPort.contains(":") ? hostPort.substring(0, hostPort.indexOf(':')) : hostPort;
                    String porta = hostPort.contains(":") ? hostPort.substring(hostPort.indexOf(':') + 1) : "5432";
                    txtHost.setText(host);
                    txtPorta.setText(porta);
                    txtNomeBanco.setText(banco);
                }
                if (props.containsKey("db.usuario")) txtUsuario.setText(props.getProperty("db.usuario"));
                if (props.containsKey("empresa.id")) txtEmpresaId.setText(props.getProperty("empresa.id"));
                if (props.containsKey("db.pool.tamanho")) txtPoolSize.setText(props.getProperty("db.pool.tamanho"));
            } catch (Exception e) {
                // Ignora — valores default ja estao nos campos
            }
        }
    }

    // ========================================================================
    // Navegacao
    // ========================================================================

    @FXML
    private void handleVoltar() {
        if (currentStep > 1) {
            currentStep--;
            updateStepView();
        }
    }

    @FXML
    private void handleProximo() {
        if (!validarPassoAtual()) return;
        if (currentStep < 4) {
            currentStep++;
            if (currentStep == 4) preencherResumo();
            updateStepView();
        }
    }

    @FXML
    private void handleConcluir() {
        salvarDbProperties();
    }

    private void updateStepView() {
        // Mostrar/esconder paineis
        step1.setVisible(currentStep == 1); step1.setManaged(currentStep == 1);
        step2.setVisible(currentStep == 2); step2.setManaged(currentStep == 2);
        step3.setVisible(currentStep == 3); step3.setManaged(currentStep == 3);
        step4.setVisible(currentStep == 4); step4.setManaged(currentStep == 4);

        // Titulo
        lblStepTitle.setText(STEP_TITLES[currentStep - 1]);

        // Botoes
        btnVoltar.setVisible(currentStep > 1);
        btnProximo.setVisible(currentStep < 4);
        btnConcluir.setVisible(currentStep == 4);

        // Sidebar indicators
        HBox[] indicators = { stepIndicator1, stepIndicator2, stepIndicator3, stepIndicator4 };
        for (int i = 0; i < indicators.length; i++) {
            if (i + 1 == currentStep) {
                indicators[i].setStyle(STYLE_STEP_ACTIVE);
                setIndicatorColors(indicators[i], true, true);
            } else if (i + 1 < currentStep) {
                indicators[i].setStyle(STYLE_STEP_DONE);
                setIndicatorColors(indicators[i], true, false);
            } else {
                indicators[i].setStyle(STYLE_STEP_INACTIVE);
                setIndicatorColors(indicators[i], false, false);
            }
        }
    }

    private void setIndicatorColors(HBox indicator, boolean active, boolean current) {
        indicator.getChildren().forEach(node -> {
            if (node instanceof javafx.scene.control.Label lbl) {
                if (active) {
                    lbl.setTextFill(javafx.scene.paint.Color.WHITE);
                    if (lbl.getText().length() == 1) { // numero
                        lbl.setStyle(current
                            ? "-fx-background-color: #047857; -fx-background-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-alignment: center;"
                            : "-fx-background-color: #059669; -fx-background-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-alignment: center;");
                    }
                } else {
                    lbl.setTextFill(javafx.scene.paint.Color.web("#7BA393"));
                    if (lbl.getText().length() == 1) {
                        lbl.setStyle("-fx-background-color: #1a3d30; -fx-background-radius: 12; -fx-min-width: 24; -fx-min-height: 24; -fx-alignment: center;");
                    }
                }
            }
        });
    }

    // ========================================================================
    // Validacao por passo
    // ========================================================================

    private boolean validarPassoAtual() {
        switch (currentStep) {
            case 1:
                if (txtHost.getText().isBlank() || txtPorta.getText().isBlank()
                        || txtUsuario.getText().isBlank() || txtSenha.getText().isBlank()) {
                    AlertHelper.warn("Preencha todos os campos de conexao.");
                    return false;
                }
                if (!conexaoTestadaOk) {
                    AlertHelper.warn("Teste a conexao antes de continuar.");
                    return false;
                }
                return true;

            case 2:
                if (!setupExecutado) {
                    AlertHelper.warn("Execute a criacao do banco antes de continuar.");
                    return false;
                }
                return true;

            case 3:
                String empresaIdStr = txtEmpresaId.getText().trim();
                if (empresaIdStr.isBlank()) {
                    AlertHelper.warn("Informe o ID da empresa.");
                    return false;
                }
                try {
                    int id = Integer.parseInt(empresaIdStr);
                    if (id < 1) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    AlertHelper.warn("ID da empresa deve ser um numero positivo.");
                    return false;
                }
                return true;

            default:
                return true;
        }
    }

    // ========================================================================
    // Passo 1: Testar conexao
    // ========================================================================

    @FXML
    private void handleTestarConexao() {
        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = txtSenha.getText();

        if (host.isBlank() || porta.isBlank() || usuario.isBlank() || senha.isBlank()) {
            AlertHelper.warn("Preencha todos os campos primeiro.");
            return;
        }

        btnTestarConexao.setDisable(true);
        lblStatusConexao.setText("Testando...");
        lblStatusConexao.setTextFill(javafx.scene.paint.Color.web("#546e7a"));

        Thread bg = new Thread(() -> {
            // Testa conexao no banco postgres (existe sempre)
            String url = "jdbc:postgresql://" + host + ":" + porta + "/postgres";
            try {
                Class.forName("org.postgresql.Driver");
                DriverManager.setLoginTimeout(5);
                try (Connection conn = DriverManager.getConnection(url, usuario, senha)) {
                    if (conn.isValid(3)) {
                        Platform.runLater(() -> {
                            lblStatusConexao.setText("Conexao OK");
                            lblStatusConexao.setTextFill(javafx.scene.paint.Color.web("#059669"));
                            conexaoTestadaOk = true;
                            btnTestarConexao.setDisable(false);
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && msg.length() > 80) msg = msg.substring(0, 80) + "...";
                String finalMsg = msg;
                Platform.runLater(() -> {
                    lblStatusConexao.setText("Falha: " + finalMsg);
                    lblStatusConexao.setTextFill(javafx.scene.paint.Color.web("#DC2626"));
                    conexaoTestadaOk = false;
                    btnTestarConexao.setDisable(false);
                });
                return;
            }
            Platform.runLater(() -> {
                lblStatusConexao.setText("Falha ao conectar");
                lblStatusConexao.setTextFill(javafx.scene.paint.Color.web("#DC2626"));
                conexaoTestadaOk = false;
                btnTestarConexao.setDisable(false);
            });
        });
        bg.setDaemon(true);
        bg.start();
    }

    // ========================================================================
    // Passo 2: Criar banco + rodar migrations
    // ========================================================================

    @FXML
    private void handleExecutarSetup() {
        btnExecutarSetup.setDisable(true);
        txtLog.clear();
        progressMigrations.setProgress(0);

        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String nomeBanco = txtNomeBanco.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = txtSenha.getText();

        Thread bg = new Thread(() -> {
            try {
                Class.forName("org.postgresql.Driver");
                DriverManager.setLoginTimeout(10);

                // 1. Verificar se banco existe / criar
                appendLog("Verificando banco '" + nomeBanco + "'...");
                String urlPostgres = "jdbc:postgresql://" + host + ":" + porta + "/postgres";
                boolean bancoExiste = false;

                try (Connection conn = DriverManager.getConnection(urlPostgres, usuario, senha)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT 1 FROM pg_database WHERE datname = ?")) {
                        ps.setString(1, nomeBanco);
                        try (ResultSet rs = ps.executeQuery()) {
                            bancoExiste = rs.next();
                        }
                    }

                    if (!bancoExiste) {
                        appendLog("Criando banco '" + nomeBanco + "'...");
                        try (Statement st = conn.createStatement()) {
                            st.execute("CREATE DATABASE " + nomeBanco
                                + " ENCODING 'UTF8' LC_COLLATE 'pt_BR.UTF-8' LC_CTYPE 'pt_BR.UTF-8' TEMPLATE template0");
                        } catch (SQLException e) {
                            // Se falhar com locale pt_BR, tenta sem especificar
                            if (e.getMessage().contains("locale") || e.getMessage().contains("collation")) {
                                appendLog("Locale pt_BR nao disponivel, criando com locale padrao...");
                                try (Statement st = conn.createStatement()) {
                                    st.execute("CREATE DATABASE " + nomeBanco + " ENCODING 'UTF8'");
                                }
                            } else {
                                throw e;
                            }
                        }
                        appendLog("Banco criado com sucesso.");
                    } else {
                        appendLog("Banco ja existe.");
                    }
                }

                // 2. Conectar no banco alvo e rodar migrations
                String urlBanco = "jdbc:postgresql://" + host + ":" + porta + "/" + nomeBanco;

                try (Connection conn = DriverManager.getConnection(urlBanco, usuario, senha)) {
                    // Verificar se ja tem tabelas (banco ja configurado)
                    boolean temTabelas = false;
                    try (ResultSet rs = conn.getMetaData().getTables(null, "public", "viagens", null)) {
                        temTabelas = rs.next();
                    }

                    if (temTabelas) {
                        appendLog("Banco ja possui tabelas — pulando migrations.");
                        updateProgress(1.0, "Banco ja configurado.");
                        Platform.runLater(() -> {
                            setupExecutado = true;
                            btnExecutarSetup.setDisable(false);
                        });
                        return;
                    }

                    // Rodar cada migration
                    appendLog("Executando " + MIGRATION_FILES.length + " scripts...");
                    String migrationsDir = localizarDiretorioMigrations();

                    for (int i = 0; i < MIGRATION_FILES.length; i++) {
                        String fileName = MIGRATION_FILES[i];
                        File sqlFile = new File(migrationsDir, fileName);

                        if (!sqlFile.exists()) {
                            appendLog("AVISO: " + fileName + " nao encontrado, pulando.");
                            continue;
                        }

                        appendLog("Executando " + fileName + "...");
                        double progress = (double)(i + 1) / MIGRATION_FILES.length;
                        updateProgress(progress, fileName);

                        String sql = Files.readString(sqlFile.toPath(), StandardCharsets.UTF_8);
                        executarSql(conn, sql, fileName);
                    }

                    appendLog("Todas as migrations executadas com sucesso!");
                    updateProgress(1.0, "Concluido.");
                }

                Platform.runLater(() -> {
                    setupExecutado = true;
                    btnExecutarSetup.setDisable(false);
                });

            } catch (Exception e) {
                AppLogger.error("SetupWizard", "Erro no setup: " + e.getMessage(), e);
                appendLog("ERRO: " + e.getMessage());
                Platform.runLater(() -> {
                    btnExecutarSetup.setDisable(false);
                    AlertHelper.error("Erro no Setup", e.getMessage());
                });
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    /**
     * Executa um script SQL completo. Trata blocos PL/pgSQL (funcoes, DO blocks).
     */
    private void executarSql(Connection conn, String sql, String fileName) throws SQLException {
        // Remove comentarios de linha e divide por ';' respeitando blocos $$ (PL/pgSQL)
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            // Estrategia: executar o script inteiro de uma vez
            // Funciona para a maioria dos scripts PostgreSQL
            st.execute(sql);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            // Se falhar executando tudo junto, tenta statement por statement
            if (e.getMessage() != null && e.getMessage().contains("multiple")) {
                executarStatementPorStatement(conn, sql);
            } else {
                // Alguns erros sao aceitaveis (objeto ja existe, etc)
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("already exists") || msg.contains("ja existe")
                        || msg.contains("duplicate") || msg.contains("relation") && msg.contains("exists")) {
                    appendLog("  (ja existe, continuando)");
                } else {
                    throw new SQLException("Erro em " + fileName + ": " + e.getMessage(), e);
                }
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void executarStatementPorStatement(Connection conn, String sql) throws SQLException {
        // Split simples por ';' no final de linha, ignorando blocos $$
        String[] statements = sql.split("(?m);\\s*$");
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                try {
                    st.execute(trimmed);
                } catch (SQLException e) {
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("already exists") || msg.contains("duplicate")) {
                        // Toleravel
                        continue;
                    }
                    throw e;
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Localiza o diretorio database_scripts/ — pode estar no diretorio do projeto
     * ou no diretorio do JAR.
     */
    private String localizarDiretorioMigrations() {
        // Tentar caminhos relativos comuns
        String[] candidatos = {
            "database_scripts",
            "../database_scripts",
            System.getProperty("user.dir") + "/database_scripts",
        };
        for (String path : candidatos) {
            File dir = new File(path);
            if (dir.isDirectory() && new File(dir, "000_schema_completo.sql").exists()) {
                return dir.getAbsolutePath();
            }
        }
        // Fallback: diretorio onde o JAR esta
        try {
            File jarDir = new File(SetupWizardController.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParentFile();
            File dir = new File(jarDir, "database_scripts");
            if (dir.isDirectory()) return dir.getAbsolutePath();
        } catch (Exception ignored) {}

        return "database_scripts"; // default
    }

    // ========================================================================
    // Passo 4: Resumo
    // ========================================================================

    private void preencherResumo() {
        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String banco = txtNomeBanco.getText().trim();

        lblResumoConexao.setText("Conexao: " + host + ":" + porta);
        lblResumoBanco.setText("Banco: " + banco);
        lblResumoEmpresa.setText("Empresa ID: " + txtEmpresaId.getText().trim());
        lblResumoApi.setText("API Sync: " + (txtApiUrl.getText().isBlank() ? "https://api.naviera.com.br" : txtApiUrl.getText().trim()));
        lblResumoPool.setText("Pool: " + txtPoolSize.getText().trim() + " conexoes | Sync: automatico (5 min)");
    }

    // ========================================================================
    // Salvar db.properties e fechar
    // ========================================================================

    private void salvarDbProperties() {
        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String banco = txtNomeBanco.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = txtSenha.getText();
        String empresaId = txtEmpresaId.getText().trim();
        String poolSize = txtPoolSize.getText().trim();
        String apiUrl = txtApiUrl.getText().trim();

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;

        StringBuilder sb = new StringBuilder();
        sb.append("# Configuracao do banco de dados — gerado pelo Setup Wizard\n");
        sb.append("# NAO commitar com credenciais de producao\n");
        sb.append("db.url=").append(jdbcUrl).append("\n");
        sb.append("db.usuario=").append(usuario).append("\n");
        sb.append("db.senha=").append(senha).append("\n");
        sb.append("db.pool.tamanho=").append(poolSize).append("\n");
        sb.append("\n");
        sb.append("# Multi-tenant: ID da empresa desta instalacao\n");
        sb.append("empresa.id=").append(empresaId).append("\n");
        sb.append("\n");
        sb.append("# Versao do aplicativo (usado para auto-update check)\n");
        sb.append("app.versao=1.0.0\n");

        if (!apiUrl.isBlank()) {
            sb.append("\n");
            sb.append("# URL da API central (sync)\n");
            sb.append("api.url=").append(apiUrl).append("\n");
        }

        try {
            Files.writeString(Path.of("db.properties"), sb.toString(), StandardCharsets.UTF_8);
            AppLogger.info("SetupWizard", "db.properties salvo com sucesso.");

            // Gerar sync_config.properties para o SyncClient
            salvarSyncConfig(apiUrl);

            AlertHelper.info("Configuracao salva com sucesso! O sistema vai iniciar.");

            // Fechar wizard — Launch.java vai abrir o LoginApp
            Platform.runLater(() -> {
                Stage stage = (Stage) btnConcluir.getScene().getWindow();
                stage.close();
            });

        } catch (IOException e) {
            AppLogger.error("SetupWizard", "Erro ao salvar db.properties: " + e.getMessage(), e);
            AlertHelper.error("Erro", "Nao foi possivel salvar db.properties: " + e.getMessage());
        }
    }

    // ========================================================================
    // Gerar sync_config.properties
    // ========================================================================

    private void salvarSyncConfig(String apiUrl) {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.naviera.com.br";
        }
        // Garante que a URL nao termina com /
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }

        StringBuilder sc = new StringBuilder();
        sc.append("#Configuracoes de Sincronizacao - Naviera Eco\n");
        sc.append("server.url=").append(apiUrl).append("\n");
        sc.append("operador.login=").append(txtSyncLogin.getText().trim()).append("\n");
        sc.append("operador.senha=").append(txtSyncSenha.getText().trim()).append("\n");
        sc.append("api.token=\n");
        sc.append("api.token.encoded=false\n");
        sc.append("sync.auto=true\n");
        sc.append("sync.interval.minutos=5\n");
        sc.append("sync.ultima=\n");

        try {
            Files.writeString(Path.of("sync_config.properties"), sc.toString(), StandardCharsets.UTF_8);
            AppLogger.info("SetupWizard", "sync_config.properties salvo com sucesso.");
        } catch (IOException e) {
            AppLogger.warn("SetupWizard", "Erro ao salvar sync_config.properties: " + e.getMessage());
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void appendLog(String msg) {
        Platform.runLater(() -> txtLog.appendText(msg + "\n"));
    }

    private void updateProgress(double value, String detail) {
        Platform.runLater(() -> {
            progressMigrations.setProgress(value);
            lblProgressDetail.setText(detail);
        });
    }
}
