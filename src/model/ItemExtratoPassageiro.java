package model;

/**
 * Item do extrato financeiro de um passageiro.
 */
public class ItemExtratoPassageiro {
    private String data, rota, descricao, valorTotal, valorPago, saldoDevedor, status;

    public ItemExtratoPassageiro(String data, String rota, String descricao, String valorTotal, String valorPago, String saldoDevedor, String status) {
        this.data = data; this.rota = rota; this.descricao = descricao;
        this.valorTotal = valorTotal; this.valorPago = valorPago; this.saldoDevedor = saldoDevedor; this.status = status;
    }

    public String getData() { return data; }
    public String getRota() { return rota; }
    public String getDescricao() { return descricao; }
    public String getValorTotal() { return valorTotal; }
    public String getValorPago() { return valorPago; }
    public String getSaldoDevedor() { return saldoDevedor; }
    public String getStatus() { return status; }
}
