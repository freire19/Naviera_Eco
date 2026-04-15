package model;

/**
 * Registro de estorno de frete para exibicao em tabela.
 */
public class EstornoFreteLog {
    private String dataHora, numeroFrete, formaDevolucao, motivo, autorizador;
    private Double valor;

    public EstornoFreteLog(String dt, String num, Double val, String forma, String mot, String auto) {
        this.dataHora = dt;
        this.numeroFrete = num;
        this.valor = val;
        this.formaDevolucao = forma;
        this.motivo = mot;
        this.autorizador = auto;
    }

    public String getDataHora() { return dataHora; }
    public String getNumeroFrete() { return numeroFrete; }
    public String getValorFormatado() { return String.format("R$ %.2f", valor); }
    public String getFormaDevolucao() { return formaDevolucao; }
    public String getMotivo() { return motivo; }
    public String getAutorizador() { return autorizador; }
}
