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
}
