package model;

import java.math.BigDecimal;

public class LinhaDespesaDetalhada {
    private final String data;
    private final String descricao;
    private final String categoria;
    private final BigDecimal valor;

    public LinhaDespesaDetalhada(String d, String de, String c, BigDecimal v) {
        this.data = d;
        this.descricao = de;
        this.categoria = c;
        this.valor = v != null ? v : BigDecimal.ZERO;
    }

    public String getData() { return data; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinhaDespesaDetalhada that = (LinhaDespesaDetalhada) o;
        return java.util.Objects.equals(data, that.data)
            && java.util.Objects.equals(descricao, that.descricao)
            && java.util.Objects.equals(categoria, that.categoria);
    }
    @Override
    public int hashCode() { return java.util.Objects.hash(data, descricao, categoria); }
}
