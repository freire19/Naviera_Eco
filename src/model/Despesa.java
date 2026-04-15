package model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class Despesa {
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private int id;
    private String data, descricao, categoria, forma, status;
    private BigDecimal valor;
    private boolean excluido;

    public Despesa(int id, String d, String desc, String cat, String forma, BigDecimal val, String st, boolean excluido) {
        this.id = id;
        this.data = d;
        this.descricao = desc;
        this.categoria = cat;
        this.forma = forma;
        this.valor = val != null ? val : BigDecimal.ZERO;
        this.status = st;
        this.excluido = excluido;
    }

    public int getId() { return id; }
    public String getData() { return data; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public String getForma() { return forma; }
    public BigDecimal getValor() { return valor; }
    public String getStatus() { return status; }
    public boolean isExcluido() { return excluido; }
    public String getValorFormatado() { return nf.format(valor); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Despesa that = (Despesa) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
