package model;

import java.time.LocalDateTime;

public class ReciboQuitacaoPassageiro {
    private int id;
    private String nomePassageiro;
    private LocalDateTime dataPagamento;
    private double valorTotal;
    private String formaPagamento;
    private String itensPagos; // Ex: "Bil. 20 (R$ 100,00); Bil. 21 (R$ 50,00)"

    public ReciboQuitacaoPassageiro() {}

    public ReciboQuitacaoPassageiro(String nomePassageiro, double valorTotal, String formaPagamento, String itensPagos) {
        this.nomePassageiro = nomePassageiro;
        this.valorTotal = valorTotal;
        this.formaPagamento = formaPagamento;
        this.itensPagos = itensPagos;
        this.dataPagamento = LocalDateTime.now();
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNomePassageiro() { return nomePassageiro; }
    public void setNomePassageiro(String nomePassageiro) { this.nomePassageiro = nomePassageiro; }
    
    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }
    
    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }
    
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    
    public String getItensPagos() { return itensPagos; }
    public void setItensPagos(String itensPagos) { this.itensPagos = itensPagos; }

    @Override
    public String toString() {
        // Formata para aparecer bonito na lista de seleção da 2ª via
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dtf.format(dataPagamento) + " - Valor: R$ " + String.format("%.2f", valorTotal) + " (" + formaPagamento + ")";
    }
}