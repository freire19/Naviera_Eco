package model;

/**
 * Registro de estorno de encomenda para exibicao em tabela.
 */
public class EstornoLog {
    private String dataHora, numeroEncomenda, formaDevolucao, motivo, autorizador;
    private Double valor;

    public EstornoLog(String dt, String num, Double val, String forma, String mot, String auto) {
        this.dataHora = dt; this.numeroEncomenda = num; this.valor = val;
        this.formaDevolucao = forma; this.motivo = mot; this.autorizador = auto;
    }

    public String getDataHora() { return dataHora; }
    public String getNumeroEncomenda() { return numeroEncomenda; }
    public String getValorFormatado() { return String.format("R$ %.2f", valor); }
    public String getFormaDevolucao() { return formaDevolucao; }
    public String getMotivo() { return motivo; }
    public String getAutorizador() { return autorizador; }
}
