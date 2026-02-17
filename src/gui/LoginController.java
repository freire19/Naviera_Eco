package gui;

import dao.ConexaoBD;
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

public class LoginController implements Initializable {

    @FXML private ComboBox<String> cmbUsuario; 
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private Button btnSair;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        carregarUsuariosNoCombo();
    }

    private void carregarUsuariosNoCombo() {
        ObservableList<String> logins = FXCollections.observableArrayList();
        String sql = "SELECT login_usuario FROM usuarios WHERE ativo = true ORDER BY login_usuario";
        
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                logins.add(rs.getString("login_usuario"));
            }
            cmbUsuario.setItems(logins);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro", "Erro ao carregar usuários: " + e.getMessage());
        }
    }

    @FXML
    private void handleEntrar(ActionEvent event) {
        String login = cmbUsuario.getValue();
        if (login == null || login.isEmpty()) {
            login = cmbUsuario.getEditor().getText();
        }
        String senha = txtSenha.getText().trim();

        if (login == null || login.isEmpty() || senha.isEmpty()) {
            showAlert(AlertType.WARNING, "Campos Vazios", "Selecione um usuário e digite a senha.");
            return;
        }

        if (realizarLogin(login, senha)) {
            Stage stageLogin = (Stage) btnEntrar.getScene().getWindow();
            stageLogin.close();
            
            // Abre a Tela Principal após o login
            abrirTelaPrincipal();
        } else {
            showAlert(AlertType.ERROR, "Acesso Negado", "Senha incorreta ou usuário inativo.");
        }
    }

    private boolean realizarLogin(String login, String senha) {
        String sql = "SELECT * FROM usuarios WHERE login_usuario = ? AND senha_hash = ? AND ativo = true";
        
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, login);
            stmt.setString(2, senha); 
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    
                    // --- CORREÇÃO AQUI: mudado de "id" para "id_usuario" ---
                    u.setId(rs.getInt("id_usuario")); 
                    // -------------------------------------------------------

                    u.setNomeCompleto(rs.getString("nome_completo")); 
                    u.setLoginUsuario(rs.getString("login_usuario")); 
                    u.setEmail(rs.getString("email"));
                    u.setFuncao(rs.getString("funcao"));
                    u.setPermissoes(rs.getString("permissoes"));
                    u.setAtivo(rs.getBoolean("ativo"));
                    
                    // Salva na memória
                    SessaoUsuario.setUsuarioLogado(u);
                    System.out.println("Login realizado: " + u.getNomeCompleto());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Mostra o erro exato na tela se acontecer de novo
            showAlert(AlertType.ERROR, "Erro de Banco", "Erro ao ler dados do usuário.\n" + e.getMessage());
        }
        return false;
    }

    private void abrirTelaPrincipal() {
        try {
            // Tenta abrir a tela principal
            new TelaPrincipalApp().start(new Stage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Fatal", "Não foi possível abrir a tela principal.\n" + e.getMessage());
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