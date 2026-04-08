package gui;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import dao.AuxiliaresDAO;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import model.Passagem;

public class FinalizarPagamentoPassagemController implements Initializable {

    @FXML private TextField txtAPagar;
    
    // Campos Individuais
    @FXML private TextField txtDinheiro;
    @FXML private TextField txtPix;
    @FXML private TextField txtCartao;
    
    @FXML private TextField txtTotalRecebido; // Soma dos 3 acima
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private TextField txtDevedor;
    @FXML private TextField txtTroco;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    private AuxiliaresDAO auxiliaresDAO;
    private Stage dialogStage;
    private Passagem passagem;
    private boolean confirmado = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.auxiliaresDAO = new AuxiliaresDAO();
        carregarComboBoxes();
        
        // Listeners para os 3 campos de pagamento
        txtDinheiro.textProperty().addListener((obs, oldVal, newVal) -> calcularTotais());
        txtPix.textProperty().addListener((obs, oldVal, newVal) -> calcularTotais());
        txtCartao.textProperty().addListener((obs, oldVal, newVal) -> calcularTotais());
        
        // Inicializa com zeros
        txtDinheiro.setText("0,00");
        txtPix.setText("0,00");
        txtCartao.setText("0,00");
    }

    // Método para receber os dados da tela principal
    public void setDadosPagamento(Stage dialogStage, Passagem passagem) {
        this.dialogStage = dialogStage;
        this.passagem = passagem;

        if (this.passagem != null && this.passagem.getValorAPagar() != null) {
            txtAPagar.setText(String.format("%,.2f", this.passagem.getValorAPagar()));
            
            // Sugestão Inteligente: Se já tiver valor pago (edição), tenta preencher
            // Senão, não preenche nada (deixa o usuário digitar onde vai entrar o dinheiro)
            calcularTotais();
            
            // Foca no campo Dinheiro por padrão
            javafx.application.Platform.runLater(() -> txtDinheiro.requestFocus());
        }
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Passagem getPassagemAtualizada() {
        return this.passagem;
    }

    @FXML
    private void handleConfirmar(ActionEvent event) {
        if (isInputValido()) {
            BigDecimal dinheiro = parseBigDecimal(txtDinheiro.getText());
            BigDecimal pix = parseBigDecimal(txtPix.getText());
            BigDecimal cartao = parseBigDecimal(txtCartao.getText());
            BigDecimal totalPago = dinheiro.add(pix).add(cartao);
            
            // Atualiza o objeto passagem com os dados detalhados
            passagem.setValorPagamentoDinheiro(dinheiro);
            passagem.setValorPagamentoPix(pix);
            passagem.setValorPagamentoCartao(cartao);
            passagem.setValorPago(totalPago); // Soma Total
            
            passagem.setDevedor(parseBigDecimal(txtDevedor.getText()));
            passagem.setTroco(parseBigDecimal(txtTroco.getText()));
            
            // Definimos "MISTO" ou o nome específico se for só um
            String formaPagtoStr = "MISTO";
            if(pix.compareTo(BigDecimal.ZERO) == 0 && cartao.compareTo(BigDecimal.ZERO) == 0) formaPagtoStr = "DINHEIRO";
            else if(dinheiro.compareTo(BigDecimal.ZERO) == 0 && cartao.compareTo(BigDecimal.ZERO) == 0) formaPagtoStr = "PIX";
            else if(dinheiro.compareTo(BigDecimal.ZERO) == 0 && pix.compareTo(BigDecimal.ZERO) == 0) formaPagtoStr = "CARTÃO";
            
            // Se tiver campo formaPagamento no Model (exibição), seta ele:
            // passagem.setFormaPagamento(formaPagtoStr); 

            passagem.setCaixa(cmbCaixa.getValue());

            confirmado = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancelar(ActionEvent event) {
        dialogStage.close();
    }
    
    private boolean isInputValido() {
        if (cmbCaixa.getValue() == null) {
            showAlert(AlertType.ERROR, "Campo Obrigatório", "Selecione o Caixa", "É necessário informar em qual caixa o valor será lançado.");
            return false;
        }
        BigDecimal totalInformado = parseBigDecimal(txtDinheiro.getText())
                .add(parseBigDecimal(txtPix.getText()))
                .add(parseBigDecimal(txtCartao.getText()));
        if (totalInformado.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(AlertType.ERROR, "Valor Inválido", "Pagamento zerado", "O valor total pago deve ser maior que zero.");
            return false;
        }
        return true;
    }

    private void carregarComboBoxes() {
        try {
            List<String> caixas = auxiliaresDAO.listarCaixas();
            cmbCaixa.setItems(FXCollections.observableArrayList(caixas));
            if(!caixas.isEmpty()) cmbCaixa.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void calcularTotais() {
        BigDecimal aPagar = parseBigDecimal(txtAPagar.getText());
        
        BigDecimal vDin = parseBigDecimal(txtDinheiro.getText());
        BigDecimal vPix = parseBigDecimal(txtPix.getText());
        BigDecimal vCar = parseBigDecimal(txtCartao.getText());
        
        BigDecimal totalPago = vDin.add(vPix).add(vCar);
        
        txtTotalRecebido.setText(String.format("%,.2f", totalPago));

        BigDecimal troco = BigDecimal.ZERO;
        BigDecimal devedor = BigDecimal.ZERO;

        if (totalPago.compareTo(aPagar) >= 0) {
            troco = totalPago.subtract(aPagar);
        } else {
            devedor = aPagar.subtract(totalPago);
        }

        txtTroco.setText(String.format("%,.2f", troco));
        txtDevedor.setText(String.format("%,.2f", devedor));
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleanedText = text.replace("R$", "").replace(".", "").replace(",", ".").trim();
        try {
            return new BigDecimal(cleanedText);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    private void showAlert(AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}