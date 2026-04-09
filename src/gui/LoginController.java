package gui;

import dao.ConexaoBD;
import dao.UsuarioDAO;
import gui.util.SessaoUsuario;
import model.Usuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.application.Platform;

public class LoginController implements Initializable {

    @FXML private ComboBox<String> cmbUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private Button btnSair;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        carregarUsuariosNoCombo();
    }

    private void carregarUsuariosNoCombo() {
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                ObservableList<String> logins = FXCollections.observableArrayList();
                String sql = "SELECT login_usuario FROM usuarios WHERE ativo = true ORDER BY login_usuario";

                try (Connection conn = ConexaoBD.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        logins.add(rs.getString("login_usuario"));
                    }
                }
                Platform.runLater(() -> cmbUsuario.setItems(logins));
            } catch (Exception e) {
                System.err.println("Erro ao carregar dados: " + e.getMessage());
                Platform.runLater(() -> showAlert(AlertType.ERROR, "Erro", "Erro ao carregar usuarios."));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    @FXML
    private void handleEntrar(ActionEvent event) {
        String login = cmbUsuario.getValue();
        if (login == null || login.isEmpty()) {
            login = cmbUsuario.getEditor().getText();
        }
        // DL064: nao aplicar trim() na senha — BCrypt compara string exata
        String senha = txtSenha.getText();

        if (login == null || login.isEmpty() || senha.isEmpty()) {
            showAlert(AlertType.WARNING, "Campos Vazios", "Selecione um usuario e digite a senha.");
            return;
        }

        if (realizarLogin(login, senha)) {
            Stage stageLogin = (Stage) btnEntrar.getScene().getWindow();
            stageLogin.close();
            abrirTelaPrincipal();
        } else {
            showAlert(AlertType.ERROR, "Acesso Negado", "Senha incorreta ou usuario inativo.");
        }
    }

    private boolean realizarLogin(String login, String senha) {
        // Usa UsuarioDAO com BCrypt — busca por login e verifica hash
        try {
            Usuario u = usuarioDAO.buscarPorUsuarioESenha(login, senha);
            if (u != null) {
                SessaoUsuario.setUsuarioLogado(u);
                System.out.println("Login realizado: " + u.getNomeCompleto());
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erro no login: " + e.getMessage());
            showAlert(AlertType.ERROR, "Erro de Banco", "Erro ao verificar credenciais.");
        }
        return false;
    }

    private void abrirTelaPrincipal() {
        try {
            new TelaPrincipalApp().start(new Stage());
        } catch (Exception e) {
            System.err.println("Erro ao abrir tela principal: " + e.getMessage());
            showAlert(AlertType.ERROR, "Erro Fatal", "Nao foi possivel abrir a tela principal.");
        }
    }

    @FXML
    private void handleSair(ActionEvent event) {
        System.exit(0);
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
