package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QuitarDividaEncomendaTotalController {

    @FXML private Label lblValorOriginal;
    @FXML private TextField txtDesconto;
    @FXML private Label lblTotalFinal;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private Button btnConfirmar;

    private double totalDivida;
    private boolean confirmado = false;

    // Getters
    public double getDesconto() { return converter(txtDesconto.getText()); }
    public double getTotalFinal() { return converter(lblTotalFinal.getText().replace("R$", "")); }
    public String getForma() { return cmbFormaPagamento.getValue(); }
    public String getCaixa() { return cmbCaixa.getValue(); }
    public boolean isConfirmado() { return confirmado; }

    @FXML
    public void initialize() {
        txtDesconto.textProperty().addListener((o,old,nw) -> calcular());

        // DR010: carrega combos em background
        Thread bg = new Thread(() -> {
            carregarFormas();
            carregarCaixas();
        });
        bg.setDaemon(true);
        bg.start();
    }

    public void setValorTotal(double total) {
        this.totalDivida = total;
        lblValorOriginal.setText(String.format("R$ %.2f", total));
        calcular();
    }

    private void calcular() {
        try {
            double desc = converter(txtDesconto.getText());
            double finalVal = Math.max(0, totalDivida - desc);
            lblTotalFinal.setText(String.format("R$ %.2f", finalVal));
        } catch (Exception e) {
            lblTotalFinal.setText("ERRO");
            System.err.println("Erro ao calcular total: " + e.getMessage());
        }
    }

    private double converter(String t) {
        try { return Double.parseDouble(t.replace(",", ".").trim()); } catch(Exception e) { return 0.0; }
    }

    private void carregarFormas() {
        ObservableList<String> l = FXCollections.observableArrayList();
        try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()){
            while(rs.next()) l.add(rs.getString(1));
        } catch(Exception e) {
            System.err.println("Erro ao carregar formas de pagamento: " + e.getMessage());
            l.addAll("DINHEIRO","PIX");
        }
        javafx.application.Platform.runLater(() -> {
            cmbFormaPagamento.setItems(l);
            cmbFormaPagamento.getSelectionModel().selectFirst();
        });
    }

    private void carregarCaixas() {
        ObservableList<String> l = FXCollections.observableArrayList();
        try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT nome_caixa FROM caixas ORDER BY nome_caixa").executeQuery()){
            while(rs.next()) l.add(rs.getString(1));
        } catch(Exception e) {
            System.err.println("Erro ao carregar caixas: " + e.getMessage());
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

    @FXML void confirmar() { confirmado = true; ((Stage)btnConfirmar.getScene().getWindow()).close(); }
    @FXML void cancelar() { ((Stage)btnConfirmar.getScene().getWindow()).close(); }
}