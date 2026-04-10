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
import javafx.application.Platform;
import gui.util.AlertHelper;
import gui.util.AppLogger;

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
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Usuarios"); return; }
        usuarioDAO = new UsuarioDAO(); // Instancia o DAO
        txtId.setDisable(true);
        txtId.setPromptText("Automático");

        // Populando ComboBoxes - você pode querer carregar isso do banco no futuro
        cmbFuncao.getItems().addAll("Administrador", "Gerente", "Operador de Caixa", "Conferente", "Atendente", "Outro");
        cmbPermissao.getItems().addAll("TOTAL", "ADMINISTRATIVO", "OPERACIONAL_COMPLETO", "OPERACIONAL_RESTRITO", "FINANCEIRO", "CONSULTA_APENAS");
        
        chkAtivo.setSelected(true); 

        configurarTabela();

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<Usuario> dados = usuarioDAO.listarTodos();
                Platform.runLater(() -> {
                    if (dados != null) {
                        listaObservableUsuarios.setAll(dados);
                    } else {
                        AlertHelper.show(Alert.AlertType.ERROR, "Erro de Carregamento", "Não foi possível carregar a lista de usuários do banco de dados.");
                        listaObservableUsuarios.clear();
                    }
                    tabelaUsuarios.refresh();
                });
            } catch (Exception e) {
                AppLogger.warn("CadastroUsuarioController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

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
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Carregamento", "Não foi possível carregar a lista de usuários do banco de dados.");
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
            AlertHelper.show(Alert.AlertType.ERROR, "Erro ao Gerar ID", "Não foi possível obter um novo ID. Verifique a sequence 'usuarios_id_seq'.");
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
            AlertHelper.show(Alert.AlertType.WARNING, "Campos Obrigatórios", "Nome Completo, Login de Usuário, Função e Permissão são obrigatórios!");
            return;
        }

        boolean isInsert = (usuarioSelecionado == null || txtId.getText().isEmpty() || txtId.getText().equals("0"));

        if (isInsert && senha.isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Senha Obrigatória", "Para novos usuários, a senha é obrigatória.");
            return;
        }

        if (!senha.isEmpty() && !senha.equals(confirmarSenha)) {
            AlertHelper.show(Alert.AlertType.WARNING, "Senhas Divergentes", "A senha e a confirmação de senha não correspondem.");
            return;
        }

        // Verifica se o login já existe para outro usuário (em caso de insert ou update de login)
        Usuario usuarioExistenteComLogin = usuarioDAO.buscarPorLogin(login);
        if (usuarioExistenteComLogin != null) {
            if (isInsert || (usuarioSelecionado != null && usuarioExistenteComLogin.getId() != usuarioSelecionado.getId())) {
                AlertHelper.show(Alert.AlertType.ERROR, "Login em Uso", "O Login de Usuário '" + login + "' já está em uso.");
                return;
            }
        }
        // Verifica se o email já existe para outro usuário (se email for preenchido)
        if (email != null && !email.isEmpty()) {
            Usuario usuarioExistenteComEmail = null; // Você precisaria de um usuarioDAO.buscarPorEmail(email);
            // Se implementar buscarPorEmail:
            // if (usuarioExistenteComEmail != null && (isInsert || (usuarioSelecionado != null && usuarioExistenteComEmail.getId() != usuarioSelecionado.getId()))) {
            //     AlertHelper.show(Alert.AlertType.ERROR, "Email em Uso", "O Email '" + email + "' já está em uso.");
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
            AppLogger.warn("CadastroUsuarioController", "Tentativa de inserir novo usuário sem senha, embora já validado.");
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
            AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", mensagemSucesso);
            carregarUsuariosNaTabela();
            handleNovo(null); 
        } else {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro", "Falha ao salvar usuário. Verifique o console para mais detalhes.");
        }
    }
    
    @FXML
    void handleExcluir(ActionEvent event) {
        usuarioSelecionado = tabelaUsuarios.getSelectionModel().getSelectedItem(); // Garante que temos o último selecionado
        if (usuarioSelecionado == null) { // Verifica se txtId está vazio também se não usar tabela
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Nenhum usuário selecionado para exclusão.");
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
                AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Usuário excluído com sucesso!");
                carregarUsuariosNaTabela();
                handleNovo(null);
            } else {
                AlertHelper.show(Alert.AlertType.ERROR, "Erro", "Falha ao excluir usuário.");
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
            AppLogger.warn("CadastroUsuarioController", "Tentativa de adicionar ícone a um botão nulo. Verifique o fx:id no FXML para o caminho: " + iconPath);
            return;
        }
        try {
            URL res = getClass().getResource(iconPath);
            if (res == null) {
                AppLogger.warn("CadastroUsuarioController", "Ícone não encontrado no classpath: " + iconPath);
                return;
            }
            Image img = new Image(res.toExternalForm());
            ImageView icon = new ImageView(img);
            icon.setFitWidth(16); 
            icon.setFitHeight(16);
            button.setGraphic(icon);
        } catch (Exception e) {
            AppLogger.warn("CadastroUsuarioController", "Erro ao carregar o ícone: " + iconPath);
            // AppLogger.error("CadastroUsuarioController", e.getMessage(), e); // Pode ser muito verboso, System.err já é suficiente
        }
    }
}