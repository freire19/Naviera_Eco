package model;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Historico de pagamento/desconto de funcionario.
 * Extraido de GestaoFuncionariosController (DM033).
 */
public class PagamentoHistorico {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final LocalDate data;
    private final String descricao;
    public final double valor;
    public final String tipo;

    public PagamentoHistorico(LocalDate data, String descricao, double valor, String tipo) {
        this.data = data;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
    }

    public String getData() { return data.format(dtf); }
    public LocalDate getDataLocal() { return data; }
    public String getDescricao() { return descricao; }
    public String getValorFormatado() {
        if ("DESCONTO".equals(tipo)) return "(-) " + nf.format(valor);
        return nf.format(valor);
    }
}
