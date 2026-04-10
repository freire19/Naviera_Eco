package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import gui.util.AppLogger;

public class CadastroFreteApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CadastroFrete.fxml"));
            Parent root = loader.load(); 
            Scene scene = new Scene(root);

            primaryStage.setTitle("Naviera - Cadastro de Frete");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1200);
            primaryStage.setHeight(700);

            primaryStage.show();

        } catch(Exception e) {
            AppLogger.error("CadastroFreteApp", e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
