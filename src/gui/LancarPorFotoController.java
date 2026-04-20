package gui;

import gui.util.AlertHelper;
import gui.util.BffClient;
import gui.util.SessaoUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import util.AppLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Fase 1 do OCR no desktop: seleciona foto, envia para o BFF (/api/ocr/upload)
 * e exibe o JSON retornado (texto extraido + dados parseados). Nao cria
 * encomenda/frete ainda — isso entra na Fase 2/3.
 */
public class LancarPorFotoController implements Initializable {

    private static final String TAG = "LancarPorFoto";

    @FXML private Label lblTitulo;
    @FXML private ComboBox<String> cbTipo;
    @FXML private TextField txtNumNf;
    @FXML private Button btnEscolher;
    @FXML private Button btnProcessar;
    @FXML private Label lblArquivo;
    @FXML private Label lblStatus;
    @FXML private ImageView preview;
    @FXML private ProgressIndicator progress;
    @FXML private TextArea txtResultado;

    private File fotoSelecionada;
    private int idViagemAtiva = 0;
    private String tipoInicial = "encomenda";

    private final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbTipo.getItems().addAll("encomenda", "frete");
        cbTipo.getSelectionModel().select(tipoInicial);
        // Mostra campo NF so quando tipo = frete
        cbTipo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean mostra = "frete".equals(newV);
            txtNumNf.setVisible(mostra);
            txtNumNf.setManaged(mostra);
        });
        txtNumNf.setVisible("frete".equals(tipoInicial));
        txtNumNf.setManaged("frete".equals(tipoInicial));
    }

    /** Define qual tipo sera pre-selecionado (encomenda ou frete). */
    public void setTipoInicial(String tipo) {
        this.tipoInicial = (tipo != null && ("encomenda".equals(tipo) || "frete".equals(tipo))) ? tipo : "encomenda";
        if (cbTipo != null) cbTipo.getSelectionModel().select(this.tipoInicial);
        if (lblTitulo != null) {
            lblTitulo.setText("Lançar " + ("frete".equals(this.tipoInicial) ? "Frete" : "Encomenda") + " por Foto (OCR)");
        }
    }

    public void setIdViagemAtiva(int idViagem) { this.idViagemAtiva = idViagem; }

    @FXML
    private void handleEscolherFoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar foto do documento");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        Stage stage = (Stage) btnEscolher.getScene().getWindow();
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        fotoSelecionada = f;
        lblArquivo.setText(f.getName() + " (" + (f.length() / 1024) + " KB)");
        try {
            preview.setImage(new Image(f.toURI().toString(), 380, 260, true, true, true));
        } catch (Exception e) {
            AppLogger.warn(TAG, "Nao foi possivel renderizar preview: " + e.getMessage());
        }
        btnProcessar.setDisable(false);
        lblStatus.setText("");
        txtResultado.clear();
    }

    @FXML
    private void handleProcessar() {
        if (fotoSelecionada == null) {
            AlertHelper.warn("Selecione uma foto primeiro.");
            return;
        }
        if (!garantirAuth()) return;

        String tipo = cbTipo.getValue();
        String numNf = "frete".equals(tipo) ? txtNumNf.getText() : null;
        btnProcessar.setDisable(true);
        btnEscolher.setDisable(true);
        progress.setVisible(true);
        lblStatus.setText("Enviando foto e processando OCR... pode levar 10-30 segundos.");
        lblStatus.setStyle("-fx-text-fill: #F59E0B;");

        // Thread separada para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                Map<String, Object> resp = BffClient.getInstance().uploadOcr(
                        fotoSelecionada, tipo, idViagemAtiva, numNf);
                String json = MAPPER.writeValueAsString(resp);
                Platform.runLater(() -> {
                    txtResultado.setText(json);
                    lblStatus.setText("✓ OCR concluido!");
                    lblStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                });
            } catch (Exception ex) {
                AppLogger.error(TAG, "Erro no OCR: " + ex.getMessage(), ex);
                Platform.runLater(() -> {
                    txtResultado.setText("ERRO: " + ex.getMessage());
                    lblStatus.setText("✗ Falha no OCR");
                    lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                });
            } finally {
                Platform.runLater(() -> {
                    btnProcessar.setDisable(false);
                    btnEscolher.setDisable(false);
                    progress.setVisible(false);
                });
            }
        }, "OcrUpload-Worker");
        bg.setDaemon(true);
        bg.start();
    }

    @FXML
    private void handleFechar() {
        Stage stage = (Stage) btnProcessar.getScene().getWindow();
        stage.close();
    }

    /**
     * Garante que o BffClient tem um JWT valido. Se nao tiver, abre um
     * dialog pedindo a senha do usuario logado para fazer login no BFF.
     */
    private boolean garantirAuth() {
        BffClient bff = BffClient.getInstance();
        if (bff.isAuthenticated()) return true;

        model.Usuario u = SessaoUsuario.getUsuarioLogado();
        String login = (u != null && u.getEmail() != null && !u.getEmail().isEmpty())
                ? u.getEmail() : (u != null ? u.getLoginUsuario() : "");

        Dialog<String> d = new Dialog<>();
        d.setTitle("Autenticacao OCR");
        d.setHeaderText("Digite sua senha para acessar o OCR");
        d.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        PasswordField pf = new PasswordField();
        pf.setPromptText("senha");
        TextField loginField = new TextField(login);
        loginField.setPromptText("email / login");
        VBox box = new VBox(8, new Label("Login:"), loginField, new Label("Senha:"), pf);
        box.setPadding(new javafx.geometry.Insets(10));
        d.getDialogPane().setContent(box);
        Platform.runLater(pf::requestFocus);

        d.setResultConverter(bt -> bt == javafx.scene.control.ButtonType.OK ? pf.getText() : null);
        Optional<String> resp = d.showAndWait();
        if (!resp.isPresent()) return false;
        String senha = resp.get();
        if (senha == null || senha.isEmpty()) {
            AlertHelper.warn("Senha em branco.");
            return false;
        }
        String loginFinal = loginField.getText();
        if (loginFinal == null || loginFinal.isEmpty()) {
            AlertHelper.warn("Login em branco.");
            return false;
        }
        boolean ok = bff.login(loginFinal, senha);
        if (!ok) {
            AlertHelper.error("Falha no login do BFF. Verifique as credenciais.");
            return false;
        }
        return true;
    }
}
