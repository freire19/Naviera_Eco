package model;

import java.math.BigDecimal;

public class ItemResumoBalanco {
    private String tipo;      // Ex: "Passagem"
    private String rota;      // Ex: "Manaus / Jutai"
    private int quantidade;   // Ex: 15
    private BigDecimal valorTotal; // Ex: 500.00

    public ItemResumoBalanco(String tipo, String rota, int quantidade, BigDecimal valorTotal) {
        this.tipo = tipo;
        this.rota = rota;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO;
    }

    public String getDescricaoFormatada() {
        return String.format("%02d %s (%s) = R$ %,.2f", quantidade, tipo, rota, valorTotal);
    }

    public String getTipo() { return tipo; }
    public String getRota() { return rota; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getValorTotal() { return valorTotal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemResumoBalanco that = (ItemResumoBalanco) o;
        return quantidade == that.quantidade
            && java.util.Objects.equals(tipo, that.tipo)
            && java.util.Objects.equals(rota, that.rota);
    }
    @Override
    public int hashCode() { return java.util.Objects.hash(tipo, rota, quantidade); }
}
