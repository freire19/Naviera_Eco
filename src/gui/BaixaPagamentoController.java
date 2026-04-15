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
import gui.util.AppLogger;

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
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Baixa de Pagamento"); return; }
        txtDesconto.textProperty().addListener((obs, old, novo) -> calcularTotais());
        txtValorRecebido.textProperty().addListener((obs, old, novo) -> calcularTotais());

        // DR010+DR219: carrega combos em background com try-catch
        Thread bg = new Thread(() -> {
            try {
                carregarFormasPagamento();
                carregarUsuariosCaixa();
            } catch (Exception e) {
                AppLogger.warn("BaixaPagamentoController", "Erro ao carregar combos: " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("carregar formas de pagamento", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
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
            AppLogger.warn("BaixaPagamentoController", "Valor invalido para conversao monetaria: " + texto);
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
        javafx.application.Platform.runLater(() -> {
            cmbFormaPagamento.setItems(formas);
            cmbFormaPagamento.getSelectionModel().selectFirst();
        });
    }

    private void carregarUsuariosCaixa() {
        ObservableList<String> users = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement("SELECT nome_caixa FROM caixas WHERE empresa_id = ? ORDER BY nome_caixa")) {
            ps.setInt(1, dao.DAOUtils.empresaId());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) users.add(rs.getString(1));
        } catch (SQLException e) {
            AppLogger.warn("BaixaPagamentoController", "Erro ao carregar caixas: " + e.getMessage());
        }
        javafx.application.Platform.runLater(() -> {
            if (!users.isEmpty()) {
                cmbCaixa.setItems(users);
            } else {
                cmbCaixa.getItems().add("PADRAO");
            }
            cmbCaixa.getSelectionModel().selectFirst();
        });
    }

    @FXML void confirmar() {
        BigDecimal desc = converterMoeda(txtDesconto.getText());
        BigDecimal valorRecebido = converterMoeda(txtValorRecebido.getText());
        // D016: impede pagamento de R$0,00
        if (valorRecebido.signum() <= 0 && desc.signum() <= 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Informe um valor de pagamento ou desconto maior que zero.");
            alert.setHeaderText("Valor invalido");
            alert.showAndWait();
            return;
        }
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
