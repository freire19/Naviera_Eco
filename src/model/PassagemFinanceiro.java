package model;

import java.math.BigDecimal;

public class PassagemFinanceiro {
    private int id;
    private String bilhete, dataViagem, passageiro, destino, status;
    private BigDecimal total, pago;

    public PassagemFinanceiro(int id, String bilhete, String data, String passageiro, String destino, BigDecimal total, BigDecimal pago, String status) {
        this.id = id;
        this.bilhete = bilhete;
        this.dataViagem = data;
        this.passageiro = passageiro;
        this.destino = destino;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.pago = pago != null ? pago : BigDecimal.ZERO;
        this.status = status;
    }

    public int getId() { return id; }
    public String getBilhete() { return bilhete; }
    public String getDataViagem() { return dataViagem; }
    public String getPassageiro() { return passageiro; }
    public String getDestino() { return destino; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getPago() { return pago; }
    public BigDecimal getRestante() { return total.subtract(pago).max(BigDecimal.ZERO); }
    public String getTotalFormatado() { return String.format("R$ %,.2f", total); }
    public String getPagoFormatado() { return String.format("R$ %,.2f", pago); }
    public String getRestanteFormatado() { return String.format("R$ %,.2f", getRestante()); }
    public String getStatus() { return status; }
}
