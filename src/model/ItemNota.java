package model;

/**
 * Item de nota de frete para exibicao em tabela e impressao.
 */
public class ItemNota {
    private int quant;
    private String nomeItem;
    private double preco;

    public ItemNota(int q, String nome, double p) {
        quant = q;
        nomeItem = nome;
        preco = p;
    }

    public int getQuant() { return quant; }
    public String getNomeItem() { return nomeItem; }
    public double getPreco() { return preco; }
    public double getTotal() { return quant * preco; }

    public String getQuantStr() { return String.valueOf(quant); }
    public String getPrecoStr() {
        return String.format("R$ %.2f", preco);
    }
    public String getTotalStr() {
        return String.format("R$ %.2f", getTotal());
    }
}
