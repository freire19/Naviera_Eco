package model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Boleto para exibicao em tabela de cadastro.
 */
public class Boleto {
    private int id;
    private String vencimento, descricao, parcelaStr, status;
    private Double valor;

    public Boleto(int id, String v, String d, String p, Double val, String s) {
        this.id = id; this.vencimento = v; this.descricao = d; this.parcelaStr = p; this.valor = val; this.status = s;
    }

    public int getId() { return id; }
    public String getVencimento() { return vencimento; }
    public String getDescricao() { return descricao; }
    public String getParcelaStr() { return parcelaStr; }
    public Double getValor() { return valor; }
    public String getStatus() { return status; }
    public String getValorFormatado() { return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor); }
}
