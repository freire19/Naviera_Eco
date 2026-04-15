package gui;

import gui.util.PermissaoService;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReciboPersonalizadoController {

    @FXML
    private ImageView imgLogo;
    @FXML
    private Label lblCnpj, lblDataEmissao, lblRecebemosDe;
    @FXML
    private TextArea txtDescricao, txtFormaPagamento;
    @FXML
    private TextField txtValor, txtTotal;
    @FXML
    private Button btnImprimirRecibo;
    @FXML
    private AnchorPane rootPane;

    @FXML
    public void initialize() {
        if (!PermissaoService.isOperacional()) { PermissaoService.exigirOperacional("Recibo Personalizado"); return; }
        // Exemplo: define data atual
        lblDataEmissao.setText("Data: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        btnImprimirRecibo.setOnAction(e-> imprimirRecibo());
    }

    public void setDadosRecibo(String cnpj, String cliente, String descricao, double valor, double total, String formaPgto) {
        lblCnpj.setText("CNPJ: " + cnpj);
        lblRecebemosDe.setText(cliente);
        txtDescricao.setText(descricao);
        txtValor.setText(String.format("R$ %.2f", valor));
        txtTotal.setText(String.format("R$ %.2f", total));
        txtFormaPagamento.setText(formaPgto);
    }

    private void imprimirRecibo() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean ok = job.showPrintDialog(rootPane.getScene().getWindow());
            if (ok) {
                boolean success = job.printPage(rootPane);
                if (success) {
                    job.endJob();
                }
            }
        }
    }
}
