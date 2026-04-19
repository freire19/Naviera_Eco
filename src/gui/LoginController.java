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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import gui.util.AlertHelper;
import util.AppLogger;

public class LoginController implements Initializable {

    @FXML private ComboBox<String> cmbUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private Button btnSair;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // #DS5-203: rate-limit local por login (memoria do processo — sobrevive a janelas de login
    // re-abertas no mesmo processo, mas nao entre reinicios da JVM). Lockout temporario apos 5 erros.
    private static final int MAX_TENTATIVAS = 5;
    private static final long LOCKOUT_MS = 15L * 60L * 1000L; // 15 min
    private static final Map<String, AtomicInteger> TENTATIVAS = new ConcurrentHashMap<>();
    private static final Map<String, Long> LOCKED_UNTIL = new ConcurrentHashMap<>();

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

        // #DS5-203: checa lockout
        String chave = login.toLowerCase();
        Long until = LOCKED_UNTIL.get(chave);
        if (until != null && System.currentTimeMillis() < until) {
            long minutos = (until - System.currentTimeMillis()) / 60_000L + 1;
            AlertHelper.show(AlertType.ERROR, "Conta Bloqueada",
                "Muitas tentativas. Tente novamente em " + minutos + " minuto(s).");
            return;
        }

        if (realizarLogin(login, senha)) {
            // reset contadores em login bem-sucedido
            TENTATIVAS.remove(chave);
            LOCKED_UNTIL.remove(chave);
            Usuario u = SessaoUsuario.getUsuarioLogado();

            // Verificar se precisa trocar a senha no primeiro login
            if (u != null && u.isDeveTrocarSenha()) {
                boolean senhaTrocada = exibirDialogTrocaSenha(u);
                if (!senhaTrocada) {
                    // Nao trocou — nao deixa entrar
                    SessaoUsuario.setUsuarioLogado(null);
                    AlertHelper.show(AlertType.WARNING, "Troca Obrigatoria",
                        "Voce precisa criar uma nova senha para continuar.");
                    return;
                }
            }

            Stage stageLogin = (Stage) btnEntrar.getScene().getWindow();
            stageLogin.close();
            abrirTelaPrincipal();
        } else {
            // #DS5-203: incrementa tentativas + backoff progressivo + lockout apos N falhas
            int n = TENTATIVAS.computeIfAbsent(chave, k -> new AtomicInteger()).incrementAndGet();
            if (n >= MAX_TENTATIVAS) {
                LOCKED_UNTIL.put(chave, System.currentTimeMillis() + LOCKOUT_MS);
                TENTATIVAS.remove(chave);
                AlertHelper.show(AlertType.ERROR, "Conta Bloqueada",
                    "Limite de tentativas excedido. Conta bloqueada por 15 minutos.");
            } else {
                // backoff progressivo: 500ms, 1s, 1.5s, 2s (aplicado na thread FX — bloqueia UI intencionalmente)
                try { Thread.sleep(Math.min(n * 500L, 3000L)); } catch (InterruptedException ignored) {}
                AlertHelper.show(AlertType.ERROR, "Acesso Negado",
                    "Senha incorreta ou usuario inativo. Tentativa " + n + " de " + MAX_TENTATIVAS + ".");
            }
        }
    }

    /**
     * Exibe dialog de troca de senha obrigatoria (primeiro login).
     * Retorna true se a senha foi trocada com sucesso.
     */
    private boolean exibirDialogTrocaSenha(Usuario usuario) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Criar Nova Senha");
        dialog.setHeaderText("Bem-vindo, " + usuario.getNomeCompleto() + "!\nCrie uma nova senha para continuar.");

        javafx.scene.control.ButtonType btnTrocar = new javafx.scene.control.ButtonType("Salvar Nova Senha", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTrocar, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        PasswordField novaSenha = new PasswordField();
        novaSenha.setPromptText("Nova senha (minimo 6 caracteres)");
        novaSenha.setPrefHeight(35);
        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Confirmar nova senha");
        confirmar.setPrefHeight(35);
        content.getChildren().addAll(
            new javafx.scene.control.Label("Nova senha:"), novaSenha,
            new javafx.scene.control.Label("Confirmar:"), confirmar
        );
        content.setPadding(new javafx.geometry.Insets(10, 20, 10, 20));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(380);

        Platform.runLater(novaSenha::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == btnTrocar) return novaSenha.getText();
            return null;
        });

        while (true) {
            java.util.Optional<String> resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() == null) return false;

            String nova = resultado.get();
            String conf = confirmar.getText();

            if (nova.length() < 6) {
                AlertHelper.show(AlertType.WARNING, "Senha Curta", "A senha deve ter no minimo 6 caracteres.");
                continue;
            }
            if (!nova.equals(conf)) {
                AlertHelper.show(AlertType.WARNING, "Senhas Diferentes", "As senhas nao conferem.");
                continue;
            }

            boolean ok = usuarioDAO.trocarSenhaELimparFlag(usuario.getId(), nova);
            if (ok) {
                AlertHelper.show(AlertType.INFORMATION, "Senha Atualizada", "Sua senha foi alterada com sucesso!");
                return true;
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro", "Nao foi possivel alterar a senha. Tente novamente.");
                return false;
            }
        }
    }

    private boolean realizarLogin(String login, String senha) {
        // Usa UsuarioDAO com BCrypt — busca por login e verifica hash
        try {
            Usuario u = usuarioDAO.buscarPorUsuarioESenha(login, senha);
            if (u != null) {
                SessaoUsuario.setUsuarioLogado(u);
                // DS4-038 fix: sem PII no console (antes: nome completo)
                AppLogger.info("LoginController", "Login realizado com sucesso");
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
