package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.AppLogger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Entry point do Naviera Desktop.
 * Verifica se db.properties existe e esta valido:
 *   - Se nao: abre o SetupWizard para configurar
 *   - Se sim: abre o LoginApp normalmente
 */
public class Launch extends Application {

    @Override
    public void start(Stage primaryStage) {
        if (precisaSetup()) {
            abrirSetupWizard(primaryStage);
        } else {
            abrirLogin(primaryStage);
        }
    }

    /**
     * Verifica se db.properties existe e tem os campos minimos preenchidos.
     */
    private boolean precisaSetup() {
        File dbProps = dao.ConexaoBD.resolverDbProperties();
        if (!dbProps.exists()) return true;

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(dbProps)) {
                props.load(fis);
            }
            String url = props.getProperty("db.url");
            String usuario = props.getProperty("db.usuario");
            String senha = props.getProperty("db.senha");

            // Se algum campo obrigatorio esta vazio ou e placeholder, precisa setup
            if (url == null || url.isBlank()) return true;
            if (usuario == null || usuario.isBlank()) return true;
            if (senha == null || senha.isBlank() || "SUA_SENHA_AQUI".equals(senha)) return true;

            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private void abrirSetupWizard(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SetupWizard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Naviera - Configuracao Inicial");
            stage.setResizable(false);
            stage.show();

            // Quando o wizard fechar, abrir o login
            stage.setOnHidden(event -> {
                // Verifica se agora o db.properties esta ok
                if (!precisaSetup()) {
                    abrirLogin(new Stage());
                }
            });
        } catch (Exception e) {
            AppLogger.error("Launch", "Erro ao abrir Setup Wizard: " + e.getMessage(), e);
            // Fallback: tenta abrir login mesmo sem setup
            abrirLogin(stage);
        }
    }

    private void abrirLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Naviera - Login");
            stage.setResizable(false);
            stage.show();

            // Verificar atualizacao em background (silencioso se offline)
            gui.util.VersaoChecker.verificarAtualizacao();
        } catch (Exception e) {
            AppLogger.error("Launch", "Erro ao abrir Login: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
