package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import gui.util.AppLogger;

public class CadastroViagemApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CadastroViagem.fxml"));
            AnchorPane root = loader.load();
            Scene scene = new Scene(root, 800, 600); // Definição de tamanho inicial
            primaryStage.setTitle("Cadastro de Viagem");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true); // Abre a tela maximizada
            primaryStage.show();
        } catch (Exception e) {
            AppLogger.error("CadastroViagemApp", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
