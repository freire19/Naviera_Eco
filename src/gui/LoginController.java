package gui;

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
import java.util.ResourceBundle;
import javafx.application.Platform;
import gui.util.AlertHelper;
import gui.util.AppLogger;

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
                java.util.List<String> logins = usuarioDAO.listarLoginsAtivos();
                Platform.runLater(() -> cmbUsuario.setItems(FXCollections.observableArrayList(logins)));
            } catch (Exception e) {
                AppLogger.warn("LoginController", "Erro ao carregar dados: " + e.getMessage());
                Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao carregar usuarios."));
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
            AlertHelper.show(AlertType.WARNING, "Campos Vazios", "Selecione um usuario e digite a senha.");
            return;
        }

        if (realizarLogin(login, senha)) {
            Stage stageLogin = (Stage) btnEntrar.getScene().getWindow();
            stageLogin.close();
            abrirTelaPrincipal();
        } else {
            AlertHelper.show(AlertType.ERROR, "Acesso Negado", "Senha incorreta ou usuario inativo.");
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
            AppLogger.warn("LoginController", "Erro no login: " + e.getMessage());
            AlertHelper.show(AlertType.ERROR, "Erro de Banco", "Erro ao verificar credenciais.");
        }
        return false;
    }

    private void abrirTelaPrincipal() {
        try {
            new TelaPrincipalApp().start(new Stage());
        } catch (Exception e) {
            AppLogger.warn("LoginController", "Erro ao abrir tela principal: " + e.getMessage());
            AlertHelper.show(AlertType.ERROR, "Erro Fatal", "Nao foi possivel abrir a tela principal.");
        }
    }

    @FXML
    private void handleSair(ActionEvent event) {
        System.exit(0);
    }

}
