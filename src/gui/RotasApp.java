package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import gui.util.AppLogger;

public class RotasApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Rotas.fxml"));
            AnchorPane root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setTitle("Cadastro de Rotas");
            primaryStage.setScene(scene);
         // primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            AppLogger.error("RotasApp", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
