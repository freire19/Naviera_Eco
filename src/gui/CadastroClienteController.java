package gui;

import dao.ClienteFreteDAO;
import gui.util.AlertHelper;
import gui.util.PermissaoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.ClienteFrete;
import util.AppLogger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador da tela CadastroCliente.fxml
 * Cadastro de Clientes de Frete — tabela cad_clientes_frete (separada de encomenda/passagem)
 */
public class CadastroClienteController {

    @FXML private TextField txtBusca;
    @FXML private ListView<ClienteFrete> listaClientes;
    @FXML private Label lblContagem;
    @FXML private TextField txtNomeCliente;
    @FXML private TextField txtRazaoSocial;
    @FXML private TextField txtCpfCnpj;
    @FXML private TextField txtInscricaoEstadual;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefone;
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnFechar;

    private final ClienteFreteDAO dao = new ClienteFreteDAO();
    private final ObservableList<ClienteFrete> todosClientes = FXCollections.observableArrayList();
    private ClienteFrete selecionado = null;

    @FXML
    private void initialize() {
        if (!PermissaoService.isOperacional()) {
            PermissaoService.exigirOperacional("Cadastro de Cliente Frete");
            return;
        }

        // Configurar ListView
        listaClientes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ClienteFrete item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String txt = item.getNomeCliente();
                    if (item.getCpfCnpj() != null && !item.getCpfCnpj().isEmpty()) {
                        txt += "  |  " + item.getCpfCnpj();
                    }
                    if (item.getTelefone() != null && !item.getTelefone().isEmpty()) {
                        txt += "  |  " + item.getTelefone();
                    }
                    setText(txt);
                }
            }
        });

        // Selecao na lista
        listaClientes.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            if (novo != null) {
                selecionado = novo;
                preencherForm(novo);
            }
        });

        // Busca
        txtBusca.textProperty().addListener((obs, old, novo) -> filtrar(novo));

        // Botoes
        btnNovo.setOnAction(e -> limparForm());
        btnSalvar.setOnAction(e -> salvar());
        btnExcluir.setOnAction(e -> excluir());
        btnFechar.setOnAction(e -> fechar());

        // Carregar dados
        carregarDados();
    }

    private void carregarDados() {
        new Thread(() -> {
            try {
                List<ClienteFrete> lista = dao.listarTodos();
                Platform.runLater(() -> {
                    todosClientes.setAll(lista);
                    listaClientes.setItems(todosClientes);
                    lblContagem.setText(lista.size() + " clientes");
                });
            } catch (Exception e) {
                AppLogger.warn("CadastroClienteController", "Erro ao carregar: " + e.getMessage());
            }
        }).start();
    }

    private void filtrar(String busca) {
        if (busca == null || busca.trim().isEmpty()) {
            listaClientes.setItems(todosClientes);
            lblContagem.setText(todosClientes.size() + " clientes");
            return;
        }
        String q = busca.toLowerCase().trim();
        List<ClienteFrete> filtrados = todosClientes.stream()
                .filter(c -> (c.getNomeCliente() != null && c.getNomeCliente().toLowerCase().contains(q))
                        || (c.getCpfCnpj() != null && c.getCpfCnpj().contains(q)))
                .collect(Collectors.toList());
        listaClientes.setItems(FXCollections.observableArrayList(filtrados));
        lblContagem.setText(filtrados.size() + " clientes");
    }

    private void preencherForm(ClienteFrete c) {
        txtNomeCliente.setText(c.getNomeCliente() != null ? c.getNomeCliente() : "");
        txtRazaoSocial.setText(c.getRazaoSocial() != null ? c.getRazaoSocial() : "");
        txtCpfCnpj.setText(c.getCpfCnpj() != null ? c.getCpfCnpj() : "");
        txtInscricaoEstadual.setText(c.getInscricaoEstadual() != null ? c.getInscricaoEstadual() : "");
        txtEndereco.setText(c.getEndereco() != null ? c.getEndereco() : "");
        txtEmail.setText(c.getEmail() != null ? c.getEmail() : "");
        txtTelefone.setText(c.getTelefone() != null ? c.getTelefone() : "");
    }

    private void limparForm() {
        selecionado = null;
        listaClientes.getSelectionModel().clearSelection();
        txtNomeCliente.clear();
        txtRazaoSocial.clear();
        txtCpfCnpj.clear();
        txtInscricaoEstadual.clear();
        txtEndereco.clear();
        txtEmail.clear();
        txtTelefone.clear();
        txtNomeCliente.requestFocus();
    }

    private ClienteFrete montarDoForm() {
        ClienteFrete c = new ClienteFrete();
        if (selecionado != null) c.setIdCliente(selecionado.getIdCliente());
        c.setNomeCliente(txtNomeCliente.getText().trim());
        c.setRazaoSocial(txtRazaoSocial.getText().trim());
        c.setCpfCnpj(txtCpfCnpj.getText().trim());
        c.setInscricaoEstadual(txtInscricaoEstadual.getText().trim());
        c.setEndereco(txtEndereco.getText().trim());
        c.setEmail(txtEmail.getText().trim());
        c.setTelefone(txtTelefone.getText().trim());
        return c;
    }

    private void salvar() {
        String nome = txtNomeCliente.getText().trim();
        if (nome.isEmpty()) {
            AlertHelper.showWarning("Informe o nome do cliente.");
            return;
        }

        ClienteFrete c = montarDoForm();

        if (selecionado != null && selecionado.getIdCliente() != null) {
            // Atualizar
            boolean ok = dao.atualizar(c);
            if (ok) {
                AlertHelper.showInfo("Cliente atualizado com sucesso!");
                carregarDados();
                limparForm();
            } else {
                AlertHelper.showWarning("Erro ao atualizar cliente.");
            }
        } else {
            // Novo
            ClienteFrete salvo = dao.salvar(c);
            if (salvo != null) {
                AlertHelper.showInfo("Cliente cadastrado com sucesso!");
                carregarDados();
                limparForm();
            } else {
                AlertHelper.showWarning("Erro ao cadastrar. Verifique se o nome ja existe.");
            }
        }
    }

    private void excluir() {
        if (selecionado == null || selecionado.getIdCliente() == null) {
            AlertHelper.showWarning("Selecione um cliente para excluir.");
            return;
        }

        boolean confirma = AlertHelper.showConfirmation(
                "Excluir cliente \"" + selecionado.getNomeCliente() + "\"?\nEsta acao nao pode ser desfeita.");
        if (!confirma) return;

        boolean ok = dao.excluir(selecionado.getIdCliente());
        if (ok) {
            AlertHelper.showInfo("Cliente excluido.");
            carregarDados();
            limparForm();
        } else {
            AlertHelper.showWarning("Erro ao excluir cliente.");
        }
    }

    private void fechar() {
        Stage stage = (Stage) btnFechar.getScene().getWindow();
        stage.close();
    }
}
