package model;

import java.math.BigDecimal;

public class EncomendaFinanceiro {
    private int id;
    private String numero, dataLancamento, remetente, destinatario;
    private BigDecimal total, pago;

    public EncomendaFinanceiro(int id, String num, String data, String rem, String dest, BigDecimal total, BigDecimal pago) {
        this.id = id;
        this.numero = num;
        this.dataLancamento = data;
        this.remetente = rem;
        this.destinatario = dest;
        this.total = total != null ? total : BigDecimal.ZERO;
        this.pago = pago != null ? pago : BigDecimal.ZERO;
    }

    public int getId() { return id; }
    public String getNumero() { return numero; }
    public String getDataLancamento() { return dataLancamento; }
    public String getRemetente() { return remetente; }
    public String getDestinatario() { return destinatario; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getPago() { return pago; }
    public BigDecimal getRestante() { return total.subtract(pago).max(BigDecimal.ZERO); }
    public String getTotalFormatado() { return String.format("R$ %,.2f", total); }
    public String getPagoFormatado() { return String.format("R$ %,.2f", pago); }
    public String getRestanteFormatado() { return String.format("R$ %,.2f", getRestante()); }

    public String getStatus() {
        return StatusPagamento.calcularPorSaldo(getRestante(), getPago()).name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncomendaFinanceiro that = (EncomendaFinanceiro) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
