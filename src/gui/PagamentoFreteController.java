package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PagamentoFreteController {

    @FXML public TextField txtTotalFrete;
    @FXML public TextField txtDesconto;
    @FXML public TextField txtAPagar;
    @FXML public TextField txtValorPago;
    @FXML public TextField txtDevedor;
    @FXML public TextField txtTroco;
    @FXML public ComboBox<String> cbTipoPagamento;
    @FXML public ComboBox<String> cbCaixa;
    @FXML public Button btnOk;
    @FXML public Button btnCancelar;

    private boolean confirmado = false;

    private final DecimalFormat df = new DecimalFormat("#,##0.00",
        new DecimalFormatSymbols(new Locale("pt", "BR")));

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Pagamento de Frete"); return; }
        cbTipoPagamento.getItems().addAll("Dinheiro", "Cartão", "PIX");
        cbCaixa.getItems().addAll("Caixa 1", "Caixa 2");

        txtDesconto.textProperty().addListener((obs, oldVal, newVal) -> recalcular());
        txtValorPago.textProperty().addListener((obs, oldVal, newVal) -> recalcular());

        btnOk.setOnAction(e-> {
            confirmado=true;
            fecharJanela();
        });
        btnCancelar.setOnAction(e-> {
            confirmado=false;
            fecharJanela();
        });
    }

    public void setTotalFrete(double total) {
        txtTotalFrete.setText(df.format(total));
        recalcular();
    }

    private void recalcular() {
        double total       = parseDoubleSafe(txtTotalFrete.getText());
        double desconto    = parseDoubleSafe(txtDesconto.getText());
        double valorPago   = parseDoubleSafe(txtValorPago.getText());
        double aPagar      = total - desconto;
        if(aPagar<0) aPagar=0;
        txtAPagar.setText(df.format(aPagar));
        double devedor=(aPagar>valorPago)?(aPagar-valorPago):0;
        txtDevedor.setText(df.format(devedor));
        double troco=(valorPago>aPagar)?(valorPago-aPagar):0;
        txtTroco.setText(df.format(troco));
    }

    public boolean isConfirmado(){return confirmado;}

    public double getDesconto() {
        return parseDoubleSafe(txtDesconto.getText());
    }
    public double getValorPago() {
        return parseDoubleSafe(txtValorPago.getText());
    }
    public double getTroco() {
        return parseDoubleSafe(txtTroco.getText());
    }
    public double getDevedor() {
        return parseDoubleSafe(txtDevedor.getText());
    }
    public String getTipoPagamento() {
        return cbTipoPagamento.getValue();
    }
    public String getCaixa() {
        return cbCaixa.getValue();
    }

    private double parseDoubleSafe(String t) {
        if(t==null||t.trim().isEmpty()) return 0.0;
        t=t.replace(".", "").replace(",",".");
        try {
            return Double.parseDouble(t);
        }catch(Exception e) {
            return 0.0;
        }
    }
    private void fecharJanela() {
        Stage st=(Stage)btnOk.getScene().getWindow();
        st.close();
    }
}
