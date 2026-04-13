package gui.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Verifica se ha nova versao do sistema disponivel no servidor.
 * Usa o endpoint GET /api/public/versao/check?plataforma=desktop&versaoAtual=X.Y.Z
 *
 * Comportamento:
 * - Versao nova disponivel: mostra Alert com changelog e link de download
 * - Atualizacao obrigatoria: mostra Alert WARNING que bloqueia ate o usuario confirmar
 * - Servidor inacessivel: segue silenciosamente (app funciona offline)
 */
public class VersaoChecker {

    private static final String TAG = "VersaoChecker";
    public static final String VERSAO_ATUAL = "1.0.0";
    private static final String PLATAFORMA = "desktop";
    private static final String CONFIG_FILE = "sync_config.properties";
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 5_000;

    private VersaoChecker() {} // Utility class

    /**
     * Verifica atualizacao em background thread (daemon).
     * Seguro para chamar de qualquer lugar — nao bloqueia FX thread.
     */
    public static void verificarAtualizacao() {
        Thread t = new Thread(() -> {
            try {
                String serverUrl = lerServerUrl();
                String endpoint = serverUrl + "/api/public/versao/check?plataforma="
                        + PLATAFORMA + "&versaoAtual=" + VERSAO_ATUAL;

                HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setRequestProperty("Accept", "application/json");

                int status = conn.getResponseCode();
                if (status != 200) {
                    AppLogger.info(TAG, "Servidor retornou HTTP " + status + " ao verificar versao");
                    conn.disconnect();
                    return;
                }

                String response = lerResposta(conn);
                conn.disconnect();

                processarResposta(response);

            } catch (Exception e) {
                // Silencioso — app funciona offline
                AppLogger.info(TAG, "Nao foi possivel verificar atualizacao: " + e.getMessage());
            }
        }, "VersionCheck");
        t.setDaemon(true);
        t.start();
    }

    // ── Leitura de config ──

    private static String lerServerUrl() {
        String defaultUrl = "http://localhost:8081";
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) return defaultUrl;

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(fis);
            return props.getProperty("server.url", defaultUrl);
        } catch (Exception e) {
            AppLogger.info(TAG, "Erro ao ler sync_config.properties: " + e.getMessage());
            return defaultUrl;
        }
    }

    // ── HTTP ──

    private static String lerResposta(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ── Processamento da resposta ──

    private static void processarResposta(String json) {
        boolean atualizado = json.contains("\"atualizado\":true");
        if (atualizado) return; // Ja esta na versao mais recente

        boolean obrigatoria = json.contains("\"obrigatoria\":true");
        String versaoNova = extrairCampo(json, "versaoNova");
        String changelog = extrairCampo(json, "changelog");
        String urlDownload = extrairCampo(json, "urlDownload");

        Platform.runLater(() -> mostrarDialogoAtualizacao(versaoNova, changelog, urlDownload, obrigatoria));
    }

    /**
     * Extrai valor de um campo String do JSON (parse simples, sem dependencia externa).
     * Retorna null se nao encontrar.
     */
    private static String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\":\"";
        int idx = json.indexOf(chave);
        if (idx < 0) {
            // Pode ser null no JSON: "campo":null
            return null;
        }
        int start = idx + chave.length();
        int end = json.indexOf("\"", start);
        if (end <= start) return null;
        // Decodificar escapes basicos do JSON
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    // ── UI ──

    private static void mostrarDialogoAtualizacao(String versaoNova, String changelog,
                                                    String urlDownload, boolean obrigatoria) {
        Alert alert = new Alert(obrigatoria ? AlertType.WARNING : AlertType.INFORMATION);
        alert.setTitle("Atualizacao do Sistema");
        alert.setHeaderText(obrigatoria
                ? "Atualizacao Obrigatoria - v" + versaoNova
                : "Nova Versao Disponivel - v" + versaoNova);

        // Corpo do dialogo com changelog e link
        VBox content = new VBox(8);
        content.setPadding(new Insets(8));

        // Versao atual vs nova
        Label lblVersoes = new Label("Versao atual: " + VERSAO_ATUAL + "  →  Nova: " + versaoNova);
        lblVersoes.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(lblVersoes);

        // Changelog (se disponivel)
        if (changelog != null && !changelog.isBlank()) {
            Label lblChangelog = new Label("Novidades:");
            lblChangelog.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
            content.getChildren().add(lblChangelog);

            TextArea txtChangelog = new TextArea(changelog);
            txtChangelog.setEditable(false);
            txtChangelog.setWrapText(true);
            txtChangelog.setPrefRowCount(6);
            txtChangelog.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().add(txtChangelog);
        }

        // Link de download (se disponivel)
        if (urlDownload != null && !urlDownload.isBlank()) {
            Hyperlink link = new Hyperlink("Baixar atualizacao: " + urlDownload);
            link.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(urlDownload));
                } catch (Exception ex) {
                    AppLogger.warn(TAG, "Erro ao abrir link de download: " + ex.getMessage());
                }
            });
            content.getChildren().add(link);
        }

        if (obrigatoria) {
            Label lblAviso = new Label("Esta atualizacao e obrigatoria. O sistema pode nao funcionar corretamente sem ela.");
            lblAviso.setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold; -fx-wrap-text: true;");
            lblAviso.setWrapText(true);
            content.getChildren().add(lblAviso);
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(480);

        if (obrigatoria) {
            // Botoes: Baixar (fecha alert) e Sair (fecha app)
            alert.getButtonTypes().clear();
            ButtonType btnBaixar = new ButtonType("Baixar Atualizacao", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnSair = new ButtonType("Sair do Sistema", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().addAll(btnBaixar, btnSair);

            alert.showAndWait().ifPresent(response -> {
                if (response == btnBaixar && urlDownload != null && !urlDownload.isBlank()) {
                    try {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(urlDownload));
                    } catch (Exception ex) {
                        AppLogger.warn(TAG, "Erro ao abrir link de download: " + ex.getMessage());
                    }
                }
                // Atualizacao obrigatoria: fecha o app
                Platform.exit();
                System.exit(0);
            });
        } else {
            alert.showAndWait();
        }
    }
}
