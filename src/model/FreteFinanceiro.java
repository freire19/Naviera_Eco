package model;

import java.math.BigDecimal;

public class FreteFinanceiro {
    private long id;
    private String numero, dataViagem, remetente, destinatario;
    private int volumes;
    private BigDecimal total, pago;

    public FreteFinanceiro(long id, String num, String data, String rem, String dest, int volumes, BigDecimal total, BigDecimal pago) {
        this.id = id;
        this.numero = num;
        this.dataViagem = data;
        this.remetente = rem;
        this.destinatario = dest;
        this.volumes = volumes;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.pago = pago != null ? pago : BigDecimal.ZERO;
    }

    public long getId() { return id; }
    public String getNumero() { return numero; }
    public String getDataViagem() { return dataViagem; }
    public String getRemetente() { return remetente; }
    public String getDestinatario() { return destinatario; }
    public int getVolumes() { return volumes; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getPago() { return pago; }
    public BigDecimal getRestante() { return total.subtract(pago).max(BigDecimal.ZERO); }
    public String getTotalFormatado() { return String.format("R$ %,.2f", total); }
    public String getPagoFormatado() { return String.format("R$ %,.2f", pago); }
    public String getRestanteFormatado() { return String.format("R$ %,.2f", getRestante()); }

    public String getStatus() {
        return StatusPagamento.calcularPorSaldo(getRestante().doubleValue(), getPago().doubleValue()).name();
    }
}
