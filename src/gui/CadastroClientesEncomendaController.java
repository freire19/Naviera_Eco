package gui;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import dao.ClienteEncomendaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import model.ClienteEncomenda;
import gui.util.AlertHelper;
import gui.util.AppLogger;

public class CadastroClientesEncomendaController implements Initializable {

    @FXML private ListView<ClienteEncomenda> listViewClientes;
    @FXML private TextField txtNomeCliente;
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;

    private ClienteEncomendaDAO clienteDAO;
    private ObservableList<ClienteEncomenda> obsListaClientes;
    private ClienteEncomenda clienteSelecionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.clienteDAO = new ClienteEncomendaDAO();
        this.obsListaClientes = FXCollections.observableArrayList();
        
        listViewClientes.setItems(obsListaClientes);
        
        // Listener para quando um cliente é selecionado na lista
        listViewClientes.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    clienteSelecionado = newSelection;
                    txtNomeCliente.setText(clienteSelecionado.getNomeCliente());
                    btnExcluir.setDisable(false);
                } else {
                    limparSelecao();
                }
            }
        );
        
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                java.util.List<ClienteEncomenda> dados = clienteDAO.listarTodos();
                Platform.runLater(() -> obsListaClientes.setAll(dados));
            } catch (Exception e) {
                AppLogger.warn("CadastroClientesEncomendaController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        limparSelecao();
    }

    private void carregarClientes() {
        obsListaClientes.setAll(clienteDAO.listarTodos());
    }
    
    private void limparSelecao() {
        clienteSelecionado = null;
        listViewClientes.getSelectionModel().clearSelection();
        txtNomeCliente.clear();
        txtNomeCliente.requestFocus();
        btnExcluir.setDisable(true);
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        limparSelecao();
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        String nome = txtNomeCliente.getText();
        if (nome == null || nome.trim().isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "O nome do cliente não pode estar vazio.");
            return;
        }

        if (clienteSelecionado == null) { // Criando um novo cliente
            ClienteEncomenda novoCliente = new ClienteEncomenda();
            novoCliente.setNomeCliente(nome.trim().toUpperCase());
            
            ClienteEncomenda clienteSalvo = clienteDAO.salvar(novoCliente);
            if (clienteSalvo != null) {
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Novo cliente salvo com sucesso!");
                carregarClientes();
                limparSelecao();
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível salvar o novo cliente. O nome pode já existir.");
            }
            
        } else { // Atualizando um cliente existente
            clienteSelecionado.setNomeCliente(nome.trim().toUpperCase());
            if (clienteDAO.atualizar(clienteSelecionado)) {
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Cliente atualizado com sucesso!");
                carregarClientes();
                limparSelecao();
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível atualizar o cliente.");
            }
        }
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        if (clienteSelecionado == null) {
            AlertHelper.show(AlertType.WARNING, "Nenhuma Seleção", "Por favor, selecione um cliente na lista para excluir.");
            return;
        }
        
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText("Excluir Cliente");
        alert.setContentText("Você tem certeza que deseja excluir o cliente '" + clienteSelecionado.getNomeCliente() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (clienteDAO.excluir(clienteSelecionado.getIdCliente())) {
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Cliente excluído com sucesso!");
                carregarClientes();
                limparSelecao();
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível excluir o cliente.");
            }
        }
    }
    
}