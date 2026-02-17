package gui;

import javafx.application.Application;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class TesteImpressaoSimples extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Um retângulo preto bem grande + texto grande
        Rectangle rect = new Rectangle(0, 0, 200, 200);
        rect.setFill(Color.BLACK);

        Label label = new Label("TESTE IMPRESSAO");
        label.setStyle("-fx-font-size: 24px; -fx-text-fill: black; -fx-font-family: 'Arial';");
        label.setLayoutX(210);
        label.setLayoutY(80);

        Group root = new Group(rect, label);
        Scene scene = new Scene(root, 500, 250, Color.WHITE);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Tela de Teste");
        primaryStage.show();

        // Manda imprimir esse desenho simples
        imprimir(root);
    }

    private void imprimir(Group root) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            System.out.println("Nenhuma impressora.");
            return;
        }

        Printer printer = job.getPrinter();
        PageLayout layout = printer.getDefaultPageLayout();

        // Ajusta layout, CSS etc.
        root.applyCss();
        root.layout();

        boolean ok = job.printPage(layout, root);
        if (ok) {
            job.endJob();
            System.out.println("Job enviado.");
        } else {
            System.out.println("Falha na impressão.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
