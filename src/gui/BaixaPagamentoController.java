package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BaixaPagamentoController {

    @FXML private Label lblValorTotal;
    @FXML private Label lblJaPago;
    @FXML private Label lblRestante;

    @FXML private TextField txtDesconto;
    @FXML private TextField txtValorRecebido;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private Label lblTroco;
    @FXML private Button btnConfirmar;

    private BigDecimal totalOriginal = BigDecimal.ZERO;
    private BigDecimal jaPago = BigDecimal.ZERO;
    private BigDecimal restanteOriginal = BigDecimal.ZERO;

    private boolean confirmado = false;

    // Getters
    public BigDecimal getDesconto() { return converterMoeda(txtDesconto.getText()); }
    public BigDecimal getValorPago() { return converterMoeda(txtValorRecebido.getText()); }
    public BigDecimal getValorTotalOriginal() { return totalOriginal; }
    public String getFormaPagamento() { return cmbFormaPagamento.getValue(); }
    public String getCaixa() { return cmbCaixa.getValue(); }
    public boolean isConfirmado() { return confirmado; }

    @FXML
    public void initialize() {
        carregarFormasPagamento();
        carregarUsuariosCaixa();

        txtDesconto.textProperty().addListener((obs, old, novo) -> calcularTotais());
        txtValorRecebido.textProperty().addListener((obs, old, novo) -> calcularTotais());
    }

    public void setDadosIniciais(BigDecimal total, BigDecimal pago, BigDecimal rest) {
        this.totalOriginal = total;
        this.jaPago = pago;
        this.restanteOriginal = rest;

        lblValorTotal.setText(String.format("R$ %,.2f", total));
        lblJaPago.setText(String.format("R$ %,.2f", pago));
        lblRestante.setText(String.format("R$ %,.2f", rest));

        txtValorRecebido.setText(String.format("%.2f", rest));
        calcularTotais();
    }

    /** Overload para compatibilidade com callers legados que ainda usam double. */
    @Deprecated
    public void setDadosIniciais(double total, double pago, double rest) {
        setDadosIniciais(BigDecimal.valueOf(total), BigDecimal.valueOf(pago), BigDecimal.valueOf(rest));
    }

    private void calcularTotais() {
        try {
            BigDecimal desc = converterMoeda(txtDesconto.getText());
            BigDecimal recebido = converterMoeda(txtValorRecebido.getText());

            BigDecimal novoRestante = restanteOriginal.subtract(desc).max(BigDecimal.ZERO);
            BigDecimal troco = recebido.subtract(novoRestante).max(BigDecimal.ZERO);

            lblRestante.setText(String.format("R$ %,.2f", novoRestante));
            lblTroco.setText(String.format("R$ %,.2f", troco));

        } catch (Exception e) {
            lblTroco.setText("R$ 0,00");
        }
    }

    private BigDecimal converterMoeda(String texto) {
        if (texto == null || texto.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String limpo = texto.trim().replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".");
            BigDecimal valor = new BigDecimal(limpo);
            if (valor.signum() < 0) throw new IllegalArgumentException("Valor nao pode ser negativo");
            return valor;
        } catch (IllegalArgumentException e) {
            System.err.println("Valor invalido para conversao monetaria: " + texto);
            return BigDecimal.ZERO;
        }
    }

    private void carregarFormasPagamento() {
        ObservableList<String> formas = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()) {
            while(rs.next()) formas.add(rs.getString(1));
        } catch (SQLException e) {
            formas.addAll("DINHEIRO", "PIX", "CARTAO");
        }
        cmbFormaPagamento.setItems(formas);
        cmbFormaPagamento.getSelectionModel().selectFirst();
    }

    private void carregarUsuariosCaixa() {
        ObservableList<String> users = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_caixa FROM caixas ORDER BY nome_caixa").executeQuery()) {
            while(rs.next()) users.add(rs.getString(1));

            if(!users.isEmpty()) {
                cmbCaixa.setItems(users);
                cmbCaixa.getSelectionModel().selectFirst();
            } else {
                cmbCaixa.getItems().add("PADRAO");
                cmbCaixa.getSelectionModel().selectFirst();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar caixas: " + e.getMessage());
            cmbCaixa.getItems().add("PADRAO");
            cmbCaixa.getSelectionModel().selectFirst();
        }
    }

    @FXML void confirmar() {
        BigDecimal desc = converterMoeda(txtDesconto.getText());
        if (desc.compareTo(restanteOriginal) > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "O desconto nao pode ser maior que o valor restante (R$ " + String.format("%,.2f", restanteOriginal) + ").");
            alert.setHeaderText("Desconto invalido");
            alert.showAndWait();
            return;
        }
        confirmado = true;
        fechar();
    }
    @FXML void cancelar() { confirmado = false; fechar(); }
    private void fechar() { ((Stage) btnConfirmar.getScene().getWindow()).close(); }
}
