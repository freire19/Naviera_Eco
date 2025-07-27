package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.net.URL;
import java.util.ResourceBundle;

// Caso usasse UsuarioDAO, mantenha-o aqui; apenas remova ControllerUtils:

public class LoginController implements Initializable {

    // =============================
    // Campos @FXML
    // =============================
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private Button btnCancelar;
    @FXML private Label lblMensagem;
    // (adicione aqui quaisquer outros campos do seu FXML)

    // =============================
    // DAOs ou variáveis de apoio
    // =============================
    // Exemplo:
    // private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // =============================
        // Cole aqui todo o código de initialize() original,
        // mas troque ControllerUtils.showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleEntrar(ActionEvent event) {
        // =============================
        // Cole o corpo de handleEntrar() original,
        // substituindo showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleCancelar(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // Outros métodos auxiliares (validações, etc.)...
}
