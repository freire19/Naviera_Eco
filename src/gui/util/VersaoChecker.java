package gui.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import util.AppLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Verifica se ha nova versao do sistema disponivel no servidor.
 * Usa o endpoint GET /api/public/versao/check?plataforma=desktop&versaoAtual=X.Y.Z
 *
 * Uso:
 *   VersaoChecker.verificarAtualizacao(info -> mostrarBadgeNaTopBar(info));
 *
 * Comportamento:
 * - Se ha versao nova: invoca callback com VersaoInfo (na FX thread)
 * - Se esta atualizado ou servidor inacessivel: nao invoca nada (silencioso)
 * - UI de notificacao e responsabilidade do chamador — ver {@link #mostrarPopupAtualizacao}
 */
public class VersaoChecker {

    private static final String TAG = "VersaoChecker";
    public static final String VERSAO_ATUAL = "1.0.0";
    private static final String PLATAFORMA = "desktop";
    private static final String CONFIG_FILE = "sync_config.properties";
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT = 5_000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VersaoChecker() {} // Utility class

    /** Dados retornados pelo check de versao (quando ha atualizacao disponivel). */
    public static class VersaoInfo {
        public final String versaoAtual;
        public final String versaoNova;
        public final String changelog;
        public final String urlDownload;
        public final boolean obrigatoria;

        public VersaoInfo(String versaoAtual, String versaoNova, String changelog,
                          String urlDownload, boolean obrigatoria) {
            this.versaoAtual = versaoAtual;
            this.versaoNova = versaoNova;
            this.changelog = changelog;
            this.urlDownload = urlDownload;
            this.obrigatoria = obrigatoria;
        }
    }

    /**
     * Verifica atualizacao em background thread (daemon).
     * Callback e invocado na FX thread apenas se houver versao nova.
     */
    public static void verificarAtualizacao(Consumer<VersaoInfo> onUpdateAvailable) {
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

                VersaoInfo info = parseResposta(response);
                if (info != null && onUpdateAvailable != null) {
                    Platform.runLater(() -> onUpdateAvailable.accept(info));
                }

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

    private static VersaoInfo parseResposta(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            boolean atualizado = root.path("atualizado").asBoolean(false);
            if (atualizado) return null;

            return new VersaoInfo(
                VERSAO_ATUAL,
                textOrNull(root, "versaoNova"),
                textOrNull(root, "changelog"),
                textOrNull(root, "urlDownload"),
                root.path("obrigatoria").asBoolean(false)
            );
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao processar resposta de versao: " + e.getMessage());
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }

    // ── UI: popup nao-modal estilo VS Code ──

    /**
     * Abre popup nao-modal com detalhes da atualizacao, posicionado proximo ao botao de origem.
     * Para atualizacao obrigatoria, popup fica modal e app fecha se usuario nao baixar.
     */
    public static void mostrarPopupAtualizacao(VersaoInfo info, Window owner) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UTILITY);
        popup.setTitle("Atualizacao Disponivel");
        if (info.obrigatoria && owner != null) {
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(owner);
        } else if (owner != null) {
            popup.initOwner(owner);
        }

        // Cabecalho
        Label titulo = new Label(info.obrigatoria
            ? "Atualizacao Obrigatoria"
            : "Nova Versao Disponivel");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label subtitulo = new Label("Naviera Desktop");
        subtitulo.setStyle("-fx-text-fill: #888;");

        Label versoes = new Label("Versao atual: " + info.versaoAtual
            + "    →    Nova: " + info.versaoNova);
        versoes.setStyle("-fx-padding: 8 0 4 0;");

        VBox header = new VBox(2, titulo, subtitulo, versoes);

        // Changelog
        VBox body = new VBox(4);
        if (info.changelog != null && !info.changelog.isBlank()) {
            Label lblCh = new Label("Novidades:");
            lblCh.setStyle("-fx-font-weight: bold;");
            TextArea txtCh = new TextArea(info.changelog);
            txtCh.setEditable(false);
            txtCh.setWrapText(true);
            txtCh.setPrefRowCount(6);
            txtCh.setMaxWidth(Double.MAX_VALUE);
            body.getChildren().addAll(lblCh, txtCh);
        }

        if (info.obrigatoria) {
            Label aviso = new Label("Esta atualizacao e obrigatoria. O sistema pode nao funcionar corretamente sem ela.");
            aviso.setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold;");
            aviso.setWrapText(true);
            body.getChildren().add(aviso);
        }

        // Botoes
        Button btnNotas = new Button("Notas da Versao");
        btnNotas.setDisable(info.changelog == null || info.changelog.isBlank());
        btnNotas.setOnAction(e -> {
            Alert notas = new Alert(Alert.AlertType.INFORMATION);
            notas.setTitle("Notas da Versao " + info.versaoNova);
            notas.setHeaderText(null);
            TextArea ta = new TextArea(info.changelog != null ? info.changelog : "");
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(12);
            notas.getDialogPane().setContent(ta);
            notas.getDialogPane().setPrefWidth(520);
            notas.showAndWait();
        });

        Button btnDownload = new Button("Baixar");
        btnDownload.setDefaultButton(true);
        btnDownload.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        btnDownload.setDisable(info.urlDownload == null || info.urlDownload.isBlank());
        btnDownload.setOnAction(e -> {
            abrirLinkDownload(info.urlDownload);
            if (info.obrigatoria) {
                popup.close();
                Platform.exit();
                System.exit(0);
            } else {
                popup.close();
            }
        });

        Button btnDepois = new Button(info.obrigatoria ? "Sair do Sistema" : "Depois");
        btnDepois.setOnAction(e -> {
            if (info.obrigatoria) {
                popup.close();
                Platform.exit();
                System.exit(0);
            } else {
                popup.close();
            }
        });

        HBox acoes = new HBox(8, btnNotas, new javafx.scene.layout.Region(), btnDepois, btnDownload);
        HBox.setHgrow(acoes.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        acoes.setAlignment(Pos.CENTER_LEFT);
        acoes.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(8, header, body, acoes);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 480, 360);
        popup.setScene(scene);
        popup.setResizable(false);
        popup.show();
    }

    private static void abrirLinkDownload(String url) {
        if (url == null || url.isBlank()) return;
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            AppLogger.warn(TAG, "Erro ao abrir link de download: " + ex.getMessage());
        }
    }
}
