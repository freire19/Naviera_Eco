package model;

import java.math.BigDecimal;

// #DB022: valorNota e valorFreteItem migrados para BigDecimal
// DM049: decoupled from JavaFX Observable properties — plain POJO
public class FreteItem {
    private String descricao;
    private int quantidade;
    private double pesoBalanca;
    private double pesoCubado;
    private BigDecimal valorNota;
    private BigDecimal valorFreteItem;

    public FreteItem(String descricao, int quantidade, double pesoBalanca, double pesoCubado, BigDecimal valorNota, BigDecimal valorFreteItem) {
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.pesoBalanca = pesoBalanca;
        this.pesoCubado = pesoCubado;
        this.valorNota = valorNota != null ? valorNota : BigDecimal.ZERO;
        this.valorFreteItem = valorFreteItem != null ? valorFreteItem : BigDecimal.ZERO;
    }

    public FreteItem() {
        this.descricao = "";
        this.quantidade = 0;
        this.pesoBalanca = 0.0;
        this.pesoCubado = 0.0;
        this.valorNota = BigDecimal.ZERO;
        this.valorFreteItem = BigDecimal.ZERO;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getQuantidade() {
        return quantidade;
    }
    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPesoBalanca() {
        return pesoBalanca;
    }
    public void setPesoBalanca(double pesoBalanca) {
        this.pesoBalanca = pesoBalanca;
    }

    public double getPesoCubado() {
        return pesoCubado;
    }
    public void setPesoCubado(double pesoCubado) {
        this.pesoCubado = pesoCubado;
    }

    public BigDecimal getValorNota() {
        return valorNota;
    }
    public void setValorNota(BigDecimal valorNota) {
        this.valorNota = valorNota != null ? valorNota : BigDecimal.ZERO;
    }

    public BigDecimal getValorFreteItem() {
        return valorFreteItem;
    }
    public void setValorFreteItem(BigDecimal valorFreteItem) {
        this.valorFreteItem = valorFreteItem != null ? valorFreteItem : BigDecimal.ZERO;
    }
}
