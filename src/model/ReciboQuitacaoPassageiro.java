package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReciboQuitacaoPassageiro {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private String nomePassageiro;
    private LocalDateTime dataPagamento;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String formaPagamento;
    private String itensPagos;

    public ReciboQuitacaoPassageiro() {}

    public ReciboQuitacaoPassageiro(String nomePassageiro, BigDecimal valorTotal, String formaPagamento, String itensPagos) {
        this.nomePassageiro = nomePassageiro;
        this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO;
        this.formaPagamento = formaPagamento;
        this.itensPagos = itensPagos;
        this.dataPagamento = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomePassageiro() { return nomePassageiro; }
    public void setNomePassageiro(String nomePassageiro) { this.nomePassageiro = nomePassageiro; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getItensPagos() { return itensPagos; }
    public void setItensPagos(String itensPagos) { this.itensPagos = itensPagos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReciboQuitacaoPassageiro that = (ReciboQuitacaoPassageiro) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }

    // #DB025: null check para evitar NPE em ListView/ComboBox
    @Override
    public String toString() {
        String dataStr = dataPagamento != null ? DTF.format(dataPagamento) : "N/A";
        String formaStr = formaPagamento != null ? formaPagamento : "N/A";
        return dataStr + " - Valor: R$ " + String.format("%,.2f", valorTotal) + " (" + formaStr + ")";
    }
}
