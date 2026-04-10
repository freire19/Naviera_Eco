package gui;

import gui.util.LogService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane; 
import javafx.stage.Stage;
import java.net.URL;
import gui.util.AlertHelper;
import gui.util.AppLogger;

public class TelaPrincipalApp extends Application {

    @Override
    public void init() throws Exception {
        super.init();
        
        // =========================================================================
        // CONFIGURAR HANDLER GLOBAL DE EXCEÇÕES NÃO CAPTURADAS
        // Captura erros fatais e grava no log automaticamente
        // =========================================================================
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Ignorar IndexOutOfBoundsException interno do JavaFX (bug conhecido do framework)
            // que ocorre no ListViewBehavior ao manipular seleção em ComboBox
            if (throwable instanceof IndexOutOfBoundsException) {
                String stackTrace = throwable.getStackTrace().length > 0
                    ? throwable.getStackTrace()[0].getClassName() : "";
                if (stackTrace.contains("javafx") || stackTrace.contains("com.sun.javafx")) {
                    AppLogger.warn("TelaPrincipalApp", "Ignorando IndexOutOfBoundsException interno do JavaFX: " + throwable.getMessage());
                    return;
                }
            }

            // Registrar erro no arquivo de log
            LogService.registrarErroFatal(thread, throwable);

            // Exibir alerta para o usuário (na thread do JavaFX)
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro Crítico");
                alert.setHeaderText("Ocorreu um erro inesperado no sistema");
                alert.setContentText(
                    "O erro foi registrado no arquivo de log.\n\n" +
                    "Tipo: " + throwable.getClass().getSimpleName() + "\n" +
                    "Mensagem: " + throwable.getMessage() + "\n\n" +
                    "Por favor, entre em contato com o suporte técnico."
                );
                alert.showAndWait();
            });

            // Imprimir no console também
            AppLogger.warn("TelaPrincipalApp", "ERRO FATAL CAPTURADO:");
            AppLogger.error("TelaPrincipalApp", throwable.getMessage(), throwable);
        });
        
        LogService.registrarInfo("Sistema inicializado com sucesso.");
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlLocation = getClass().getResource("TelaPrincipal.fxml");
            if (fxmlLocation == null) {
                // Tenta carregar de uma forma alternativa se estiver em pastas diferentes
                // durante o desenvolvimento vs. empacotado.
                // Mas para estrutura src/gui/TelaPrincipal.fxml e resources como source folder,
                // getClass().getResource("TelaPrincipal.fxml") deve funcionar.
                AppLogger.warn("TelaPrincipalApp", "Erro Crítico: Arquivo TelaPrincipal.fxml não encontrado no caminho esperado (gui/TelaPrincipal.fxml).");
                AppLogger.warn("TelaPrincipalApp", "Verifique se o FXML está no pacote 'gui'.");
                AlertHelper.show(Alert.AlertType.ERROR, "Erro Crítico", "Não foi possível encontrar o arquivo de interface principal (TelaPrincipal.fxml).");
                return; // Encerra se não encontrar o FXML
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            BorderPane root = loader.load(); 

            Scene scene = new Scene(root); 

            // Carrega o CSS
            URL cssLocation = getClass().getResource("/css/main.css"); // Caminho a partir da raiz do classpath
            if (cssLocation != null) {
                scene.getStylesheets().add(cssLocation.toExternalForm());
                System.out.println("CSS carregado de: " + cssLocation.toExternalForm());
            } else {
                System.out.println("AVISO: Arquivo CSS não encontrado em /css/main.css. Verifique se 'resources' é uma pasta de origem.");
            }

            primaryStage.setTitle("Naviera - Navegação Fluvial");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true); 
            // primaryStage.setMaximized(true); // Descomente para iniciar maximizado
            primaryStage.show();

        } catch (Exception e) {
            AppLogger.warn("TelaPrincipalApp", "Erro ao carregar a tela principal:");
            AppLogger.error("TelaPrincipalApp", e.getMessage(), e);
            AlertHelper.show(Alert.AlertType.ERROR, "Erro Crítico de Inicialização", 
                      "Não foi possível iniciar a aplicação devido a um erro interno.\nDetalhes: " + e.getMessage());
        }
    }
    

    public static void main(String[] args) {
        launch(args);
    }
}