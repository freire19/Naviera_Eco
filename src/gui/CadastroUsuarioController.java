package gui;

// import dao.CidadeDAO; // Removido para simplificar, adicione se for usar cbCidadeDeCobranca
import dao.UsuarioDAO;
import model.Usuario;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CadastroUsuarioController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtNomeCompleto;
    @FXML private TextField txtLoginUsuario;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtSenha;
    @FXML private PasswordField txtConfirmarSenha;
    @FXML private ComboBox<String> cmbFuncao;
    @FXML private ComboBox<String> cmbPermissao;
    @FXML private CheckBox chkAtivo;

    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colIdUsuario;
    @FXML private TableColumn<Usuario, String> colNomeCompleto;
    @FXML private TableColumn<Usuario, String> colLogin;
    @FXML private TableColumn<Usuario, String> colFuncao;
    @FXML private TableColumn<Usuario, Boolean> colAtivo;

    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnSair; 
    // Se você adicionar fx:id="btnEditar" e fx:id="btnPesquisar" ao FXML, descomente aqui:
    // @FXML private Button btnEditar;
    // @FXML private Button btnPesquisar;

    @FXML private Label lblMensagem;

    private UsuarioDAO usuarioDAO;
    private Usuario usuarioSelecionado = null;
    private ObservableList<Usuario> listaObservableUsuarios;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usuarioDAO = new UsuarioDAO(); // Instancia o DAO
        txtId.setDisable(true);
        txtId.setPromptText("Automático");

        // Populando ComboBoxes - você pode querer carregar isso do banco no futuro
        cmbFuncao.getItems().addAll("Administrador", "Gerente", "Operador de Caixa", "Conferente", "Atendente", "Outro");
        cmbPermissao.getItems().addAll("TOTAL", "ADMINISTRATIVO", "OPERACIONAL_COMPLETO", "OPERACIONAL_RESTRITO", "FINANCEIRO", "CONSULTA_APENAS");
        
        chkAtivo.setSelected(true); 

        configurarTabela();
        carregarUsuariosNaTabela();
        carregarIcones(); 

        tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                usuarioSelecionado = newSelection;
                preencherCamposComUsuario(usuarioSelecionado);
                lblMensagem.setText("Editando usuário: " + usuarioSelecionado.getNomeCompleto());
                txtSenha.setPromptText("Deixe em branco para não alterar");
                txtConfirmarSenha.setPromptText("Deixe em branco para não alterar");
            } else {
                // Não limpar tudo aqui, handleNovo faz isso de forma mais controlada
            }
        });
    }

    private void configurarTabela() {
        colIdUsuario.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNomeCompleto.setCellValueFactory(new PropertyValueFactory<>("nomeCompleto"));
        colLogin.setCellValueFactory(new PropertyValueFactory<>("loginUsuario"));
        colFuncao.setCellValueFactory(new PropertyValueFactory<>("funcao"));
        colAtivo.setCellValueFactory(new PropertyValueFactory<>("ativo"));
        
        colAtivo.setCellFactory(column -> new TableCell<Usuario, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Sim" : "Não"));
            }
        });
        listaObservableUsuarios = FXCollections.observableArrayList();
        tabelaUsuarios.setItems(listaObservableUsuarios);
    }

    private void carregarUsuariosNaTabela() {
        List<Usuario> usuariosDoBanco = usuarioDAO.listarTodos();
        if (usuariosDoBanco != null) {
            listaObservableUsuarios.setAll(usuariosDoBanco);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erro de Carregamento", "Não foi possível carregar a lista de usuários do banco de dados.");
            listaObservableUsuarios.clear();
        }
        tabelaUsuarios.refresh(); // Garante que a tabela seja redesenhada
    }

    private void preencherCamposComUsuario(Usuario u) {
        if (u == null) {
            limparCampos(true); 
            return;
        }
        txtId.setText(String.valueOf(u.getId()));
        txtNomeCompleto.setText(u.getNomeCompleto());
        txtLoginUsuario.setText(u.getLoginUsuario());
        txtEmail.setText(u.getEmail());
        txtSenha.clear(); 
        txtConfirmarSenha.clear();
        cmbFuncao.setValue(u.getFuncao());
        cmbPermissao.setValue(u.getPermissoes());
        chkAtivo.setSelected(u.isAtivo());
    }

    @FXML
    void handleNovo(ActionEvent event) {
        limparCampos(true); 
        usuarioSelecionado = null; 
        int proximoId = usuarioDAO.gerarProximoId();
        if (proximoId > 0) {
            txtId.setText(String.valueOf(proximoId)); // Apenas para UI, DAO ignora no insert SERIAL
            lblMensagem.setText("Preencha os dados para um novo usuário. O ID é para referência.");
        } else {
            txtId.clear(); 
            showAlert(Alert.AlertType.ERROR, "Erro ao Gerar ID", "Não foi possível obter um novo ID. Verifique a sequence 'usuarios_id_usuario_seq'.");
            lblMensagem.setText("Erro ao gerar ID. Verifique o console.");
        }
        txtNomeCompleto.requestFocus();
    }

    @FXML
    void handleSalvar(ActionEvent event) {
        String nome = txtNomeCompleto.getText().trim();
        String login = txtLoginUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = txtSenha.getText(); // Não trim(), senha pode ter espaços
        String confirmarSenha = txtConfirmarSenha.getText();
        String funcao = cmbFuncao.getValue();
        String permissao = cmbPermissao.getValue();
        boolean ativo = chkAtivo.isSelected();

        if (nome.isEmpty() || login.isEmpty() || funcao == null || permissao == null) {
            showAlert(Alert.AlertType.WARNING, "Campos Obrigatórios", "Nome Completo, Login de Usuário, Função e Permissão são obrigatórios!");
            return;
        }

        boolean isInsert = (usuarioSelecionado == null || txtId.getText().isEmpty() || txtId.getText().equals("0"));

        if (isInsert && senha.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Senha Obrigatória", "Para novos usuários, a senha é obrigatória.");
            return;
        }

        if (!senha.isEmpty() && !senha.equals(confirmarSenha)) {
            showAlert(Alert.AlertType.WARNING, "Senhas Divergentes", "A senha e a confirmação de senha não correspondem.");
            return;
        }

        // Verifica se o login já existe para outro usuário (em caso de insert ou update de login)
        Usuario usuarioExistenteComLogin = usuarioDAO.buscarPorLogin(login);
        if (usuarioExistenteComLogin != null) {
            if (isInsert || (usuarioSelecionado != null && usuarioExistenteComLogin.getId() != usuarioSelecionado.getId())) {
                showAlert(Alert.AlertType.ERROR, "Login em Uso", "O Login de Usuário '" + login + "' já está em uso.");
                return;
            }
        }
        // Verifica se o email já existe para outro usuário (se email for preenchido)
        if (email != null && !email.isEmpty()) {
            Usuario usuarioExistenteComEmail = null; // Você precisaria de um usuarioDAO.buscarPorEmail(email);
            // Se implementar buscarPorEmail:
            // if (usuarioExistenteComEmail != null && (isInsert || (usuarioSelecionado != null && usuarioExistenteComEmail.getId() != usuarioSelecionado.getId()))) {
            //     showAlert(Alert.AlertType.ERROR, "Email em Uso", "O Email '" + email + "' já está em uso.");
            //     return;
            // }
        }


        Usuario u;
        if (isInsert) {
            u = new Usuario();
        } else {
            u = usuarioSelecionado; 
        }

        u.setNomeCompleto(nome);
        u.setLoginUsuario(login);
        u.setEmail(email.isEmpty() ? null : email); 
        
        if (!senha.isEmpty()) { // Só define a senha no objeto se uma nova foi digitada
            u.setSenhaPlana(senha);
        } else if (isInsert) {
            // Esta validação já foi feita acima, mas é uma dupla checagem.
            System.err.println("Tentativa de inserir novo usuário sem senha, embora já validado.");
            return; 
        }
        // Se for atualização e a senha estiver vazia, o DAO (na minha sugestão) não atualizará o hash.

        u.setFuncao(funcao);
        u.setPermissoes(permissao);
        u.setAtivo(ativo);

        boolean sucesso;
        String mensagemSucesso;

        if (isInsert) {
            sucesso = usuarioDAO.inserir(u);
            mensagemSucesso = "Usuário ID " + u.getId() + " cadastrado com sucesso!"; // Mostra ID retornado pelo DAO
        } else {
            sucesso = usuarioDAO.atualizar(u);
            mensagemSucesso = "Usuário ID " + u.getId() + " atualizado com sucesso!";
        }

        if (sucesso) {
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", mensagemSucesso);
            carregarUsuariosNaTabela();
            handleNovo(null); 
        } else {
            showAlert(Alert.AlertType.ERROR, "Erro", "Falha ao salvar usuário. Verifique o console para mais detalhes.");
        }
    }
    
    @FXML
    void handleExcluir(ActionEvent event) {
        usuarioSelecionado = tabelaUsuarios.getSelectionModel().getSelectedItem(); // Garante que temos o último selecionado
        if (usuarioSelecionado == null) { // Verifica se txtId está vazio também se não usar tabela
            showAlert(Alert.AlertType.WARNING, "Atenção", "Nenhum usuário selecionado para exclusão.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir o usuário: " + usuarioSelecionado.getNomeCompleto() + " (Login: " + usuarioSelecionado.getLoginUsuario() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirmacao.setTitle("Confirmar Exclusão");
        confirmacao.setHeaderText("Excluir Usuário");

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.YES) {
            if (usuarioDAO.excluir(usuarioSelecionado.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Usuário excluído com sucesso!");
                carregarUsuariosNaTabela();
                handleNovo(null);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erro", "Falha ao excluir usuário.");
            }
        }
    }

    @FXML
    void handleSair(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void limparCampos(boolean paraNovoModo) {
        if (paraNovoModo) {
            txtId.clear();
            usuarioSelecionado = null;
            tabelaUsuarios.getSelectionModel().clearSelection();
        }
        txtNomeCompleto.clear();
        txtLoginUsuario.clear();
        txtEmail.clear();
        txtSenha.clear();
        txtConfirmarSenha.clear();
        cmbFuncao.getSelectionModel().clearSelection();
        cmbFuncao.setValue(null); 
        cmbPermissao.getSelectionModel().clearSelection();
        cmbPermissao.setValue(null);
        chkAtivo.setSelected(true); 
        lblMensagem.setText("");
    }

    private void showAlert(Alert.AlertType alertType, String titulo, String mensagem) {
        Alert alerta = new Alert(alertType);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
    
    private void carregarIcones() {
        adicionarIcone(btnNovo, "/gui/icons/novo.png");
        adicionarIcone(btnSalvar, "/gui/icons/salvar.png");
        adicionarIcone(btnExcluir, "/gui/icons/excluir.png");
        if (btnSair != null) { // Adicionar verificação se o botão existir no FXML
             adicionarIcone(btnSair, "/gui/icons/sair.png");
        }
        // Se adicionar btnEditar e btnPesquisar ao FXML e ao controller:
        // adicionarIcone(btnEditar, "/gui/icons/editar.png");
        // adicionarIcone(btnPesquisar, "/gui/icons/pesquisar.png");
    }

    private void adicionarIcone(Button button, String iconPath) {
        if (button == null) { // Verificação para evitar NullPointerException
            System.err.println("Tentativa de adicionar ícone a um botão nulo. Verifique o fx:id no FXML para o caminho: " + iconPath);
            return;
        }
        try {
            URL res = getClass().getResource(iconPath);
            if (res == null) {
                System.err.println("Ícone não encontrado no classpath: " + iconPath);
                return;
            }
            Image img = new Image(res.toExternalForm());
            ImageView icon = new ImageView(img);
            icon.setFitWidth(16); 
            icon.setFitHeight(16);
            button.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("Erro ao carregar o ícone: " + iconPath);
            // e.printStackTrace(); // Pode ser muito verboso, System.err já é suficiente
        }
    }
}