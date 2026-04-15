package model;

/**
 * Item de frete para exibicao em relatorio tabelado.
 */
public class FreteItemRelatorio {
    private String codFrete, dataViagem, remetente, item, quantidade, preco, total;

    public FreteItemRelatorio(String codFrete, String dataViagem, String remetente, String item, double quantidade, double preco, double total) {
        this.codFrete = codFrete;
        this.dataViagem = dataViagem;
        this.remetente = remetente != null ? remetente : "";
        this.item = item != null ? item : "";
        this.quantidade = String.format("%.0f", quantidade);
        this.preco = String.format("R$ %.2f", preco);
        this.total = String.format("R$ %.2f", total);
    }

    public String getCodFrete() { return codFrete; }
    public String getDataViagem() { return dataViagem; }
    public String getRemetente() { return remetente; }
    public String getItem() { return item; }
    public String getQuantidade() { return quantidade; }
    public String getPreco() { return preco; }
    public String getTotal() { return total; }
}
