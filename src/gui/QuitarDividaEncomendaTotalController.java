package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import gui.util.AppLogger;

public class QuitarDividaEncomendaTotalController {

    @FXML private Label lblValorOriginal;
    @FXML private TextField txtDesconto;
    @FXML private Label lblTotalFinal;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private Button btnConfirmar;

    private BigDecimal totalDivida = BigDecimal.ZERO;
    private boolean confirmado = false;

    // Getters
    public BigDecimal getDesconto() { return converterBD(txtDesconto.getText()); }
    public BigDecimal getTotalFinal() { return converterBD(lblTotalFinal.getText().replace("R$", "")); }
    public String getForma() { return cmbFormaPagamento.getValue(); }
    public String getCaixa() { return cmbCaixa.getValue(); }
    public boolean isConfirmado() { return confirmado; }

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Quitar Divida"); return; }
        txtDesconto.textProperty().addListener((o,old,nw) -> calcular());

        // DR010: carrega combos em background
        Thread bg = new Thread(() -> {
            try {
                carregarFormas();
                carregarCaixas();
            } catch (Exception e) {
                AppLogger.warn("QuitarDividaEncomendaTotalController", "Erro em QuitarDividaEncomendaTotalController (bg init): " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("QuitarDividaEncomendaTotalController", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    public void setValorTotal(BigDecimal total) {
        this.totalDivida = total;
        lblValorOriginal.setText(String.format("R$ %.2f", total));
        calcular();
    }

    private void calcular() {
        try {
            BigDecimal desc = converterBD(txtDesconto.getText());
            BigDecimal finalVal = totalDivida.subtract(desc).max(BigDecimal.ZERO);
            lblTotalFinal.setText(String.format("R$ %.2f", finalVal));
        } catch (Exception e) {
            lblTotalFinal.setText("ERRO");
            AppLogger.warn("QuitarDividaEncomendaTotalController", "Erro ao calcular total: " + e.getMessage());
        }
    }

    private BigDecimal converterBD(String t) {
        try { return new BigDecimal(t.replace(",", ".").trim()); } catch(Exception e) { return BigDecimal.ZERO; }
    }

    private void carregarFormas() {
        ObservableList<String> l = FXCollections.observableArrayList();
        try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()){
            while(rs.next()) l.add(rs.getString(1));
        } catch(Exception e) {
            AppLogger.warn("QuitarDividaEncomendaTotalController", "Erro ao carregar formas de pagamento: " + e.getMessage());
            l.addAll("DINHEIRO","PIX");
        }
        javafx.application.Platform.runLater(() -> {
            cmbFormaPagamento.setItems(l);
            cmbFormaPagamento.getSelectionModel().selectFirst();
        });
    }

    private void carregarCaixas() {
        ObservableList<String> l = FXCollections.observableArrayList();
        try(Connection c = ConexaoBD.getConnection();
            java.sql.PreparedStatement ps = c.prepareStatement("SELECT nome_caixa FROM caixas WHERE empresa_id = ? ORDER BY nome_caixa")) {
            ps.setInt(1, dao.DAOUtils.empresaId());
            // DR213: try-with-resources para ResultSet
            try (ResultSet rs = ps.executeQuery()) {
            while(rs.next()) l.add(rs.getString(1));
            }
        } catch(Exception e) {
            AppLogger.warn("QuitarDividaEncomendaTotalController", "Erro ao carregar caixas: " + e.getMessage());
            l.add("CAIXA PRINCIPAL");
        }
        javafx.application.Platform.runLater(() -> {
            if (!l.isEmpty()) {
                cmbCaixa.setItems(l);
            } else {
                cmbCaixa.getItems().add("CAIXA PRINCIPAL");
            }
            cmbCaixa.getSelectionModel().selectFirst();
        });
    }

    @FXML void confirmar() {
        // DL054: validar desconto e forma de pagamento antes de confirmar
        BigDecimal desc = converterBD(txtDesconto.getText());
        if (desc.compareTo(totalDivida) > 0) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            a.setTitle("Desconto Invalido");
            a.setHeaderText(null);
            a.setContentText("O desconto (R$ " + String.format("%.2f", desc) + ") nao pode ser maior que a divida total (R$ " + String.format("%.2f", totalDivida) + ").");
            a.showAndWait();
            return;
        }
        if (cmbFormaPagamento.getValue() == null || cmbFormaPagamento.getValue().trim().isEmpty()) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            a.setTitle("Campo Obrigatorio");
            a.setHeaderText(null);
            a.setContentText("Selecione a forma de pagamento.");
            a.showAndWait();
            return;
        }
        confirmado = true;
        ((Stage)btnConfirmar.getScene().getWindow()).close();
    }
    @FXML void cancelar() { ((Stage)btnConfirmar.getScene().getWindow()).close(); }
}