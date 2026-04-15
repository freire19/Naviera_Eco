package model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Item do extrato financeiro de um cliente de encomenda.
 */
public class ItemExtratoEncomenda {
    private String dataViagem, rota, descricao;
    private Double valorTotal, valorPago, saldo;
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public ItemExtratoEncomenda(String data, String rota, String desc, Double total, Double pago, Double saldo) {
        this.dataViagem = data; this.rota = rota; this.descricao = desc;
        this.valorTotal = total; this.valorPago = pago; this.saldo = saldo;
    }

    public String getDataViagem() { return dataViagem; }
    public String getRota() { return rota; }
    public String getDescricao() { return descricao; }
    public String getValorTotal() { return nf.format(valorTotal); }
    public String getValorPago() { return nf.format(valorPago); }
    public String getSaldo() { return nf.format(saldo); }
    public String getStatus() {
        return StatusPagamento.calcularPorSaldo(saldo, valorPago).name();
    }
}
