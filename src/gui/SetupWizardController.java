package gui;

import gui.util.AlertHelper;
import gui.util.AppLogger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Wizard de primeira configuracao — fluxo simplificado com codigo de ativacao.
 *
 * 3 telas:
 *   1. Codigo de ativacao (unico campo)
 *   2. Preparando sistema (auto-setup completo em background)
 *   3. Pronto (empresa configurada, pronto para login)
 *
 * O operador NAO precisa saber o que e PostgreSQL, host, porta, banco ou empresa_id.
 * Tudo e detectado, instalado e configurado automaticamente.
 */
public class SetupWizardController implements Initializable {

    // -- Sidebar --
    @FXML private HBox stepIndicator1, stepIndicator2, stepIndicator3;

    // -- Header --
    @FXML private Label lblStepTitle;
    @FXML private StackPane stepContent;
    @FXML private VBox step1, step2, step3;

    // -- Tela 1: Ativacao --
    @FXML private TextField txtCodigoAtivacao;
    @FXML private Label lblStatusAtivacao;
    @FXML private Button btnAtivar;
    @FXML private Hyperlink lnkCadastrar;

    // -- Tela 2: Preparando --
    @FXML private Label lblNomeEmpresa;
    @FXML private Label lblSetupMsg;
    @FXML private ProgressBar progressSetup;
    @FXML private Label lblSetupDetail;
    @FXML private VBox boxErro;
    @FXML private Label lblErroDetalhe;
    @FXML private Button btnTentarNovamente, btnCopiarLog;

    // -- Tela 3: Pronto --
    @FXML private Label lblProntoEmpresa;
    @FXML private Label lblProntoLogin;
    @FXML private Button btnIniciar;

    private int currentStep = 1;

    // Dados recebidos da API apos ativacao
    private long empresaId;
    private String nomeEmpresa;
    private String slugEmpresa;
    private String operadorNome;
    private String operadorEmail;

    // Log de setup para suporte
    private final StringBuilder logCompleto = new StringBuilder();

    // URL da API central
    private static final String API_URL = "https://api.naviera.com.br";

    // Credenciais do PG local (geradas automaticamente, operador nunca ve)
    private String pgSenhaLocal;
    private String pgPortaLocal = "5432";

    private static final String[] STEP_TITLES = {
        "Ativar sua empresa",
        "Preparando seu sistema",
        "Tudo pronto!"
    };

    // Migrations na ordem correta
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
        "019_sync_trigger_bypass.sql",
        "020_sync_colunas_faltantes.sql",
        "021_estornos_empresa_id.sql",
        "022_ocr_lancamentos.sql",
        "023_onboarding_self_service.sql"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Gerar senha aleatoria para o PostgreSQL local
        pgSenhaLocal = "nav_" + UUID.randomUUID().toString().substring(0, 12);
        updateStepView();
    }

    // ========================================================================
    // Tela 1: Ativacao
    // ========================================================================

    @FXML
    private void handleAtivar() {
        String codigo = txtCodigoAtivacao.getText().trim().toUpperCase();
        if (codigo.isEmpty()) {
            lblStatusAtivacao.setText("Digite o codigo de ativacao.");
            return;
        }

        btnAtivar.setDisable(true);
        lblStatusAtivacao.setText("");
        lblStatusAtivacao.setTextFill(javafx.scene.paint.Color.web("#546e7a"));
        lblStatusAtivacao.setText("Verificando codigo...");

        Thread bg = new Thread(() -> {
            try {
                // Chamar API: GET /public/ativar/{codigo}
                String urlStr = API_URL + "/api/public/ativar/" + codigo;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                String body = readResponse(conn);

                if (status == 200) {
                    // Parse JSON simples (sem lib externa)
                    empresaId = parseLong(body, "empresa_id");
                    nomeEmpresa = parseString(body, "nome");
                    slugEmpresa = parseString(body, "slug");
                    operadorNome = parseString(body, "operador_nome");
                    operadorEmail = parseString(body, "operador_email");

                    log("Ativacao OK: empresa_id=" + empresaId + ", nome=" + nomeEmpresa);

                    Platform.runLater(() -> {
                        // Ir para tela 2 e iniciar setup automatico
                        currentStep = 2;
                        updateStepView();
                        lblNomeEmpresa.setText(nomeEmpresa);
                        iniciarSetupAutomatico();
                    });
                } else {
                    String erro = parseString(body, "error");
                    if (erro == null || erro.isEmpty()) erro = parseString(body, "message");
                    if (erro == null || erro.isEmpty()) erro = "Codigo invalido. Verifique e tente novamente.";
                    String msg = erro;
                    Platform.runLater(() -> {
                        lblStatusAtivacao.setTextFill(javafx.scene.paint.Color.web("#DC2626"));
                        lblStatusAtivacao.setText(msg);
                        btnAtivar.setDisable(false);
                    });
                }
            } catch (Exception e) {
                log("Erro na ativacao: " + e.getMessage());
                Platform.runLater(() -> {
                    lblStatusAtivacao.setTextFill(javafx.scene.paint.Color.web("#DC2626"));
                    lblStatusAtivacao.setText("Sem conexao com a internet. Conecte e tente novamente.");
                    btnAtivar.setDisable(false);
                });
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    @FXML
    private void handleAbrirSite() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://naviera.com.br"));
        } catch (Exception e) {
            AppLogger.warn("SetupWizard", "Nao foi possivel abrir o navegador: " + e.getMessage());
        }
    }

    // ========================================================================
    // Tela 2: Setup automatico completo
    // ========================================================================

    private void iniciarSetupAutomatico() {
        boxErro.setVisible(false);
        boxErro.setManaged(false);
        progressSetup.setProgress(-1);

        Thread bg = new Thread(() -> {
            try {
                // ---- Passo 1: Verificar/instalar PostgreSQL ----
                updateSetup("Verificando seu computador...", "Procurando banco de dados local...", -1);

                if (!isPostgresRunning()) {
                    updateSetup("Instalando componentes necessarios...", "Isso pode levar alguns minutos...", -1);
                    log("PostgreSQL nao detectado, iniciando instalacao...");
                    boolean instalado = instalarPostgresSilencioso();

                    if (!instalado) {
                        throw new Exception("Nao foi possivel instalar o banco de dados automaticamente. " +
                            "Ligue para o suporte: (92) 00000-0000");
                    }

                    // Aguardar PG subir
                    updateSetup("Aguardando componentes iniciarem...", "Quase la...", -1);
                    for (int i = 0; i < 15; i++) {
                        Thread.sleep(2000);
                        if (isPostgresRunning()) break;
                    }
                    if (!isPostgresRunning()) {
                        throw new Exception("O banco de dados foi instalado mas nao iniciou. " +
                            "Reinicie o computador e abra o Naviera novamente.");
                    }
                    log("PostgreSQL instalado e rodando.");
                } else {
                    log("PostgreSQL detectado na porta " + pgPortaLocal);
                }

                // ---- Passo 2: Criar banco ----
                updateSetup("Criando banco de dados...", "Preparando estrutura...", 0.2);

                String urlPostgres = "jdbc:postgresql://localhost:" + pgPortaLocal + "/postgres";
                Class.forName("org.postgresql.Driver");
                DriverManager.setLoginTimeout(5);

                // Tentar com senha gerada, fallback para senhas comuns
                String senhaFuncional = encontrarSenhaPg();
                if (senhaFuncional == null) {
                    throw new Exception("Nao foi possivel conectar ao banco de dados local. " +
                        "Ligue para o suporte: (92) 00000-0000");
                }

                // Criar banco se nao existir
                try (Connection conn = DriverManager.getConnection(urlPostgres, "postgres", senhaFuncional)) {
                    boolean bancoExiste = false;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                        ps.setString(1, "naviera_eco");
                        try (ResultSet rs = ps.executeQuery()) { bancoExiste = rs.next(); }
                    }
                    if (!bancoExiste) {
                        log("Criando banco naviera_eco...");
                        try (Statement st = conn.createStatement()) {
                            st.execute("CREATE DATABASE naviera_eco ENCODING 'UTF8'");
                        }
                        log("Banco criado.");
                    } else {
                        log("Banco naviera_eco ja existe.");
                    }
                }

                // ---- Passo 3: Rodar migrations ----
                updateSetup("Configurando estrutura de dados...", "Criando tabelas...", 0.35);
                String urlBanco = "jdbc:postgresql://localhost:" + pgPortaLocal + "/naviera_eco";

                try (Connection conn = DriverManager.getConnection(urlBanco, "postgres", senhaFuncional)) {
                    // Verificar se ja tem tabelas
                    boolean temTabelas = false;
                    try (ResultSet rs = conn.getMetaData().getTables(null, "public", "viagens", null)) {
                        temTabelas = rs.next();
                    }

                    if (!temTabelas) {
                        String migrationsDir = localizarDiretorioMigrations();
                        log("Rodando migrations de: " + migrationsDir);

                        for (int i = 0; i < MIGRATION_FILES.length; i++) {
                            String fileName = MIGRATION_FILES[i];
                            File sqlFile = new File(migrationsDir, fileName);
                            if (!sqlFile.exists()) {
                                log("Migration nao encontrada (pulando): " + fileName);
                                continue;
                            }

                            double progress = 0.35 + (0.5 * ((double)(i + 1) / MIGRATION_FILES.length));
                            updateSetup("Configurando estrutura de dados...", fileName, progress);

                            String sql = Files.readString(sqlFile.toPath(), StandardCharsets.UTF_8);
                            executarSql(conn, sql, fileName);
                            log("Migration OK: " + fileName);
                        }
                    } else {
                        log("Banco ja possui tabelas — migrations puladas.");
                    }
                }

                // ---- Passo 4: Gerar db.properties ----
                updateSetup("Salvando configuracao...", "Finalizando...", 0.9);
                salvarDbProperties(senhaFuncional);
                salvarSyncConfig();
                log("db.properties e sync_config.properties gerados.");

                // ---- Concluido ----
                updateSetup("Tudo configurado!", "", 1.0);
                Thread.sleep(500);

                Platform.runLater(() -> {
                    currentStep = 3;
                    updateStepView();
                    lblProntoEmpresa.setText(nomeEmpresa);
                    lblProntoLogin.setText("Seu login: " + operadorEmail);
                });

            } catch (Exception e) {
                log("ERRO FATAL: " + e.getMessage());
                AppLogger.error("SetupWizard", "Erro no auto-setup: " + e.getMessage(), e);
                Platform.runLater(() -> mostrarErro(e.getMessage()));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // ========================================================================
    // PostgreSQL: Detectar e instalar silenciosamente
    // ========================================================================

    private boolean isPostgresRunning() {
        String[] portas = {"5432", "5433"};
        for (String porta : portas) {
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress("localhost", Integer.parseInt(porta)), 2000);
                pgPortaLocal = porta;
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Tenta encontrar uma senha que funcione para o PostgreSQL local.
     * Testa: senha do instalador silencioso, senhas comuns, sem senha.
     */
    private String encontrarSenhaPg() {
        String[] senhasCandidatas = {
            pgSenhaLocal,           // senha gerada pelo nosso instalador silencioso
            "NavieraDB@2026",       // senha usada pelo instalador anterior
            "postgres",             // default comum
            "",                     // sem senha (trust auth)
            "123456"                // comum em dev
        };

        String url = "jdbc:postgresql://localhost:" + pgPortaLocal + "/postgres";
        for (String senha : senhasCandidatas) {
            try {
                DriverManager.setLoginTimeout(3);
                try (Connection conn = DriverManager.getConnection(url, "postgres", senha)) {
                    if (conn.isValid(2)) {
                        log("Conexao PG OK com senha: " + (senha.isEmpty() ? "(vazia)" : "****"));
                        return senha;
                    }
                }
            } catch (SQLException ignored) {}
        }
        return null;
    }

    private boolean instalarPostgresSilencioso() {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("linux")) {
            return instalarPostgresLinux();
        } else if (os.contains("win")) {
            return instalarPostgresWindows();
        }
        return false;
    }

    private boolean instalarPostgresLinux() {
        try {
            log("Instalando PostgreSQL via apt (Linux)...");
            ProcessBuilder pb = new ProcessBuilder(
                "pkexec", "bash", "-c",
                "apt-get update -qq && apt-get install -y postgresql postgresql-client && " +
                "sudo -u postgres psql -c \"ALTER USER postgres PASSWORD '" + pgSenhaLocal + "';\""
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log("[apt] " + line);
                }
            }
            int exitCode = proc.waitFor();
            log("apt install exit code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log("Erro ao instalar PostgreSQL Linux: " + e.getMessage());
            return false;
        }
    }

    private boolean instalarPostgresWindows() {
        try {
            log("Tentando instalar PostgreSQL via winget (Windows)...");
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c",
                "winget install -e --id PostgreSQL.PostgreSQL.16 " +
                "--accept-package-agreements --accept-source-agreements --silent"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log("[winget] " + line);
                }
            }
            int exitCode = proc.waitFor();
            log("winget exit code: " + exitCode);

            if (exitCode == 0) return true;

            // Fallback: download direto
            log("winget falhou, tentando download direto...");
            return tentarDownloadDiretoPostgres();
        } catch (Exception e) {
            log("Erro ao instalar PostgreSQL Windows: " + e.getMessage());
            return false;
        }
    }

    private boolean tentarDownloadDiretoPostgres() {
        try {
            String[] urls = {
                "https://get.enterprisedb.com/postgresql/postgresql-16.8-1-windows-x64.exe",
                "https://get.enterprisedb.com/postgresql/postgresql-16.7-1-windows-x64.exe",
                "https://get.enterprisedb.com/postgresql/postgresql-16.6-1-windows-x64.exe"
            };

            Path tempInstaller = Files.createTempFile("postgresql-installer-", ".exe");

            for (String pgUrl : urls) {
                try {
                    log("Baixando de " + pgUrl + "...");
                    HttpURLConnection conn = (HttpURLConnection) new URL(pgUrl).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(300000);
                    conn.setInstanceFollowRedirects(true);
                    if (conn.getResponseCode() == 200) {
                        try (InputStream in = conn.getInputStream()) {
                            Files.copy(in, tempInstaller, StandardCopyOption.REPLACE_EXISTING);
                        }
                        break;
                    }
                } catch (Exception e) {
                    log("URL falhou: " + pgUrl + " - " + e.getMessage());
                }
            }

            if (!Files.exists(tempInstaller) || Files.size(tempInstaller) < 1000000) return false;

            log("Executando instalador silencioso...");
            ProcessBuilder pb = new ProcessBuilder(
                tempInstaller.toString(), "--mode", "unattended",
                "--superpassword", pgSenhaLocal,
                "--serverport", "5432", "--install_runtimes", "0"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) { log("[installer] " + line); }
            }
            int exitCode = proc.waitFor();
            Files.deleteIfExists(tempInstaller);
            log("Installer exit code: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log("Download direto falhou: " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // Migrations
    // ========================================================================

    private void executarSql(Connection conn, String sql, String fileName) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("already exists") || msg.contains("ja existe")
                    || msg.contains("duplicate") || (msg.contains("relation") && msg.contains("exists"))) {
                log("  (ja existe, continuando)");
            } else if (msg.contains("multiple")) {
                executarStatementPorStatement(conn, sql);
            } else {
                throw new SQLException("Erro em " + fileName + ": " + e.getMessage(), e);
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void executarStatementPorStatement(Connection conn, String sql) throws SQLException {
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
                    if (msg.contains("already exists") || msg.contains("duplicate")) continue;
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

    private String localizarDiretorioMigrations() {
        String[] candidatos = { "database_scripts", "../database_scripts",
            System.getProperty("user.dir") + "/database_scripts" };
        for (String path : candidatos) {
            File dir = new File(path);
            if (dir.isDirectory() && new File(dir, "000_schema_completo.sql").exists()) return dir.getAbsolutePath();
        }
        try {
            File jarDir = new File(SetupWizardController.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParentFile();
            File dir = new File(jarDir, "database_scripts");
            if (dir.isDirectory()) return dir.getAbsolutePath();
        } catch (Exception ignored) {}
        return "database_scripts";
    }

    // ========================================================================
    // Salvar configuracoes
    // ========================================================================

    private void salvarDbProperties(String senhaPg) throws IOException {
        String jdbcUrl = "jdbc:postgresql://localhost:" + pgPortaLocal + "/naviera_eco";

        StringBuilder sb = new StringBuilder();
        sb.append("# Configuracao gerada automaticamente pelo Naviera Setup\n");
        sb.append("db.url=").append(jdbcUrl).append("\n");
        sb.append("db.usuario=postgres\n");
        sb.append("db.senha=").append(senhaPg).append("\n");
        sb.append("db.pool.tamanho=5\n\n");
        sb.append("# Empresa desta instalacao\n");
        sb.append("empresa.id=").append(empresaId).append("\n\n");
        sb.append("# Versao\n");
        sb.append("app.versao=1.0.0\n");

        Files.writeString(Path.of("db.properties"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void salvarSyncConfig() throws IOException {
        StringBuilder sc = new StringBuilder();
        sc.append("# Configuracao de sincronizacao — gerado pelo Setup\n");
        sc.append("server.url=").append(API_URL).append("\n");
        sc.append("operador.login=").append(operadorEmail != null ? operadorEmail : "").append("\n");
        sc.append("operador.senha=\n");
        sc.append("api.token=\n");
        sc.append("api.token.encoded=false\n");
        sc.append("sync.auto=true\n");
        sc.append("sync.interval.minutos=5\n");
        sc.append("sync.ultima=\n");

        Files.writeString(Path.of("sync_config.properties"), sc.toString(), StandardCharsets.UTF_8);
    }

    // ========================================================================
    // Tela 3: Pronto — iniciar sistema
    // ========================================================================

    @FXML
    private void handleIniciar() {
        Platform.runLater(() -> {
            Stage stage = (Stage) btnIniciar.getScene().getWindow();
            stage.close();
        });
    }

    // ========================================================================
    // Erro e retry
    // ========================================================================

    private void mostrarErro(String mensagem) {
        progressSetup.setProgress(0);
        lblSetupMsg.setText("Ocorreu um problema");
        lblSetupDetail.setText("");
        boxErro.setVisible(true);
        boxErro.setManaged(true);
        lblErroDetalhe.setText(mensagem);
    }

    @FXML
    private void handleTentarNovamente() {
        iniciarSetupAutomatico();
    }

    @FXML
    private void handleCopiarLog() {
        ClipboardContent cc = new ClipboardContent();
        cc.putString("=== Naviera Setup Log ===\n" +
            "Empresa: " + nomeEmpresa + " (ID: " + empresaId + ")\n" +
            "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "\n" +
            "Java: " + System.getProperty("java.version") + "\n\n" +
            logCompleto.toString());
        Clipboard.getSystemClipboard().setContent(cc);
        AlertHelper.info("Log copiado! Cole no WhatsApp e envie para o suporte.");
    }

    // ========================================================================
    // Navegacao visual
    // ========================================================================

    private void updateStepView() {
        step1.setVisible(currentStep == 1); step1.setManaged(currentStep == 1);
        step2.setVisible(currentStep == 2); step2.setManaged(currentStep == 2);
        step3.setVisible(currentStep == 3); step3.setManaged(currentStep == 3);

        lblStepTitle.setText(STEP_TITLES[currentStep - 1]);

        HBox[] indicators = { stepIndicator1, stepIndicator2, stepIndicator3 };
        for (int i = 0; i < indicators.length; i++) {
            if (i + 1 == currentStep) {
                indicators[i].setStyle("-fx-padding: 10; -fx-background-color: #059669; -fx-background-radius: 5;");
                setIndicatorColors(indicators[i], true);
            } else if (i + 1 < currentStep) {
                indicators[i].setStyle("-fx-padding: 10; -fx-background-color: #047857; -fx-background-radius: 5;");
                setIndicatorColors(indicators[i], true);
            } else {
                indicators[i].setStyle("-fx-padding: 10; -fx-background-radius: 5;");
                setIndicatorColors(indicators[i], false);
            }
        }
    }

    private void setIndicatorColors(HBox indicator, boolean active) {
        indicator.getChildren().forEach(node -> {
            if (node instanceof Label lbl) {
                lbl.setTextFill(active
                    ? javafx.scene.paint.Color.WHITE
                    : javafx.scene.paint.Color.web("#7BA393"));
            }
        });
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void updateSetup(String msg, String detail, double progress) {
        Platform.runLater(() -> {
            lblSetupMsg.setText(msg);
            lblSetupDetail.setText(detail);
            if (progress >= 0) progressSetup.setProgress(progress);
        });
    }

    private void log(String msg) {
        logCompleto.append("[").append(java.time.LocalTime.now().toString().substring(0, 8))
            .append("] ").append(msg).append("\n");
        AppLogger.info("SetupWizard", msg);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "{}";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // JSON parsing simples (sem dependencia de Jackson no Desktop)
    private String parseString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;
        int start = json.indexOf("\"", colon + 1);
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }

    private long parseLong(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return 0;
        StringBuilder num = new StringBuilder();
        for (int i = colon + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (num.length() > 0) break;
        }
        return num.length() > 0 ? Long.parseLong(num.toString()) : 0;
    }
}
