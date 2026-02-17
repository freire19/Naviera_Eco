package model;

public class LinhaDespesaBalanco {
    private final String categoria;
    private final Double valor;

    public LinhaDespesaBalanco(String categoria, Double valor) {
        this.categoria = categoria;
        this.valor = valor;
    }

    public String getCategoria() { return categoria; }
    public Double getValor() { return valor; }
}