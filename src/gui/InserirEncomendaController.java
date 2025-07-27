package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.net.URL;
import java.util.ResourceBundle;

// Caso usasse EncomendaDAO ou AuxiliaresDAO, deixe aqui.
// Apenas remova ControllerUtils:

public class InserirEncomendaController implements Initializable {

    // =============================
    // Campos @FXML
    // =============================
    @FXML private TextField txtNumeroEncomenda;
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbCidadeOrigem;
    @FXML private ComboBox<String> cmbCidadeDestino;
    @FXML private TextField txtPeso;
    @FXML private TextField txtVolume;
    @FXML private TextField txtValorNota;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnNovo;
    @FXML private Button btnEditar;
    @FXML private Button btnSair;
    // (adicione aqui quaisquer outros campos que seu FXML possuía)

    // =============================
    // DAOs ou variáveis de apoio
    // =============================
    // Exemplo:
    // private final EncomendaDAO encomendaDAO = new EncomendaDAO();
    // private final AuxiliaresDAO auxDao = new AuxiliaresDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // =============================
        // Cole todo o seu código de initialize() aqui,
        // mas remova chamadas a ControllerUtils.*.
        // Por exemplo, se fazia:
        // ControllerUtils.showAlert(AlertType.ERROR, "Erro", "Msg");
        // troque por new Alert(...).
        // =============================
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        // =============================
        // Cole handleNovo() original, mas converta showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        // =============================
        // Cole handleSalvar() original, convertendo showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleEditar(ActionEvent event) {
        // =============================
        // Cole handleEditar() original, convertendo showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        // =============================
        // Cole handleExcluir() original, convertendo showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // Outros métodos auxiliares (por ex. validação de campos)...
}
