package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
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

    public void setTotalFrete(BigDecimal total) {
        txtTotalFrete.setText(df.format(total));
        recalcular();
    }

    private void recalcular() {
        BigDecimal total       = parseBigDecimalSafe(txtTotalFrete.getText());
        BigDecimal desconto    = parseBigDecimalSafe(txtDesconto.getText());
        BigDecimal valorPago   = parseBigDecimalSafe(txtValorPago.getText());
        BigDecimal aPagar      = total.subtract(desconto).max(BigDecimal.ZERO);
        txtAPagar.setText(df.format(aPagar));
        BigDecimal devedor = (aPagar.compareTo(valorPago) > 0) ? aPagar.subtract(valorPago) : BigDecimal.ZERO;
        txtDevedor.setText(df.format(devedor));
        BigDecimal troco = (valorPago.compareTo(aPagar) > 0) ? valorPago.subtract(aPagar) : BigDecimal.ZERO;
        txtTroco.setText(df.format(troco));
    }

    public boolean isConfirmado(){return confirmado;}

    public BigDecimal getDesconto() {
        return parseBigDecimalSafe(txtDesconto.getText());
    }
    public BigDecimal getValorPago() {
        return parseBigDecimalSafe(txtValorPago.getText());
    }
    public BigDecimal getTroco() {
        return parseBigDecimalSafe(txtTroco.getText());
    }
    public BigDecimal getDevedor() {
        return parseBigDecimalSafe(txtDevedor.getText());
    }
    public String getTipoPagamento() {
        return cbTipoPagamento.getValue();
    }
    public String getCaixa() {
        return cbCaixa.getValue();
    }

    private BigDecimal parseBigDecimalSafe(String t) {
        if(t==null||t.trim().isEmpty()) return BigDecimal.ZERO;
        t=t.replace(".", "").replace(",",".");
        try {
            return new BigDecimal(t);
        }catch(Exception e) {
            return BigDecimal.ZERO;
        }
    }
    private void fecharJanela() {
        Stage st=(Stage)btnOk.getScene().getWindow();
        st.close();
    }
}
