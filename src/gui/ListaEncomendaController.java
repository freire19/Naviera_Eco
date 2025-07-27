package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

// Caso usasse EncomendaDAO, deixe o import; só remova ControllerUtils:

public class ListaEncomendaController implements Initializable {

    // =============================
    // Campos @FXML
    // =============================
    @FXML private ComboBox<String> cmbFiltroCliente;
    @FXML private ComboBox<String> cmbFiltroCidadeOrigem;
    @FXML private ComboBox<String> cmbFiltroCidadeDestino;
    @FXML private TextField txtFiltroNumeroEncomenda;
    @FXML private TableView<?> tabelaEncomendas;
    @FXML private Button btnFiltrar;
    @FXML private Button btnSair;
    // (adicione aqui quaisquer outros campos que constassem no seu FXML)

    // =============================
    // DAOs ou variáveis de apoio
    // =============================
    // Exemplo:
    // private final EncomendaDAO encomendaDAO = new EncomendaDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // =============================
        // Cole aqui todo o initialize() original,
        // substituindo cada ControllerUtils.showAlert por new Alert.
        // =============================
    }

    @FXML
    private void handleFiltrar(ActionEvent event) {
        // =============================
        // Cole o corpo de handleFiltrar() original,
        // trocando showAlert → new Alert.
        // =============================
    }

    @FXML
    private void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // Outros métodos auxiliares (por ex. carregar filtros, preencher tabelas)...
}
//Testando o funcionamento do GitHub Desktop