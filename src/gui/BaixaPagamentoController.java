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

    private double totalOriginal;
    private double jaPago;
    private double restanteOriginal; // Sem desconto
    
    private boolean confirmado = false;

    // Getters
    public double getDesconto() { return converterMoeda(txtDesconto.getText()); }
    public double getValorPago() { return converterMoeda(txtValorRecebido.getText()); }
    public double getValorTotalOriginal() { return totalOriginal; }
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

    public void setDadosIniciais(double total, double pago, double rest) {
        this.totalOriginal = total;
        this.jaPago = pago;
        this.restanteOriginal = rest;
        
        lblValorTotal.setText(String.format("R$ %.2f", total));
        lblJaPago.setText(String.format("R$ %.2f", pago));
        lblRestante.setText(String.format("R$ %.2f", rest));
        
        // Sugere o valor restante cheio inicialmente
        txtValorRecebido.setText(String.format("%.2f", rest));
        calcularTotais();
    }

    private void calcularTotais() {
        try {
            double desc = converterMoeda(txtDesconto.getText());
            double recebido = converterMoeda(txtValorRecebido.getText());
            
            // O que a pessoa precisa pagar REALMENTE?
            // (Dívida Original - Desconto)
            double novoRestante = Math.max(0, restanteOriginal - desc);
            
            // Troco = O que ela deu - O que precisava pagar
            double troco = Math.max(0, recebido - novoRestante);

            // Atualiza visualmente o restante a pagar considerando o desconto
            lblRestante.setText(String.format("R$ %.2f", novoRestante));
            lblTroco.setText(String.format("R$ %.2f", troco));
            
        } catch (Exception e) {
            lblTroco.setText("R$ 0,00");
        }
    }

    private double converterMoeda(String texto) {
        if (texto == null || texto.trim().isEmpty()) return 0.0;
        try {
            double valor = Double.parseDouble(texto.trim().replace(",", "."));
            if (valor < 0) throw new IllegalArgumentException("Valor nao pode ser negativo");
            return valor;
        } catch (NumberFormatException e) {
            System.err.println("Valor invalido para conversao monetaria: " + texto);
            return 0.0;
        }
    }
    
    private void carregarFormasPagamento() {
        ObservableList<String> formas = FXCollections.observableArrayList();
        // Busca do banco
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_forma_pagamento FROM aux_formas_pagamento").executeQuery()) {
            while(rs.next()) formas.add(rs.getString(1));
        } catch (SQLException e) { 
            // Fallback se não tiver tabela
            formas.addAll("DINHEIRO", "PIX", "CARTAO"); 
        }
        cmbFormaPagamento.setItems(formas);
        cmbFormaPagamento.getSelectionModel().selectFirst();
    }
    
    private void carregarUsuariosCaixa() {
        ObservableList<String> users = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement("SELECT nome_completo FROM usuarios ORDER BY nome_completo").executeQuery()) {
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
        double desc = converterMoeda(txtDesconto.getText());
        if (desc > restanteOriginal) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "O desconto não pode ser maior que o valor restante (R$ " + String.format("%.2f", restanteOriginal) + ").");
            alert.setHeaderText("Desconto inválido");
            alert.showAndWait();
            return;
        }
        confirmado = true;
        fechar();
    }
    @FXML void cancelar() { confirmado = false; fechar(); }
    private void fechar() { ((Stage) btnConfirmar.getScene().getWindow()).close(); }
}