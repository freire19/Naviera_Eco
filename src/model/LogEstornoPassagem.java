package model;

/**
 * Registro de estorno de passagem para exibicao em tabela.
 */
public class LogEstornoPassagem {
    private String dataHora, bilhete, valor, forma, motivo, autorizador;

    public LogEstornoPassagem(String dataHora, String bilhete, String valor, String forma, String motivo, String autorizador) {
        this.dataHora = dataHora;
        this.bilhete = bilhete;
        this.valor = valor;
        this.forma = forma;
        this.motivo = motivo;
        this.autorizador = autorizador;
    }

    public String getDataHora() { return dataHora; }
    public String getBilhete() { return bilhete; }
    public String getValor() { return valor; }
    public String getForma() { return forma; }
    public String getMotivo() { return motivo; }
    public String getAutorizador() { return autorizador; }
}
