package gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.time.LocalDate;

public class RelatorioPassagemController {

    @FXML private DatePicker dpInicial, dpFinal;
    @FXML private TextField txtTotalVendido, txtRecebido, txtDescontos;
    @FXML private Button btnGerar, btnImprimirRel;

    @FXML
    public void initialize() {
        dpInicial.setValue(LocalDate.now());
        dpFinal.setValue(LocalDate.now());
    }

    @FXML
    void handleGerarRelatorio(ActionEvent event) {
        LocalDate inicio = dpInicial.getValue();
        LocalDate fim = dpFinal.getValue();

        // Exemplo: faz soma no BD (DAO)
        double totalVendido = 5672.0;
        double recebido = 5672.0;
        double descontos = 363.0;

        txtTotalVendido.setText("R$ " + totalVendido);
        txtRecebido.setText("R$ " + recebido);
        txtDescontos.setText("R$ " + descontos);
    }

    @FXML
    void handleImprimirRel(ActionEvent event) {
        mostrarAlerta("Imprimir Relatório", "Função não implementada.");
    }

    private void mostrarAlerta(String titulo, String msg) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(msg);
        alerta.showAndWait();
    }
}
