package model;

import java.time.LocalDate;

public class ReciboAvulso {
    private int id;
    private int idViagem;
    private String nomePagador;
    private String referenteA;
    private double valor;
    private LocalDate dataEmissao;
    private String tipoRecibo;

    public ReciboAvulso() {}

    public ReciboAvulso(int idViagem, String nomePagador, String referenteA, double valor, LocalDate dataEmissao) {
        this.idViagem = idViagem;
        this.nomePagador = nomePagador;
        this.referenteA = referenteA;
        this.valor = valor;
        this.dataEmissao = dataEmissao;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdViagem() { return idViagem; }
    public void setIdViagem(int idViagem) { this.idViagem = idViagem; }
    public String getNomePagador() { return nomePagador; }
    public void setNomePagador(String nomePagador) { this.nomePagador = nomePagador; }
    public String getReferenteA() { return referenteA; }
    public void setReferenteA(String referenteA) { this.referenteA = referenteA; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public String getTipoRecibo() { return tipoRecibo; }
    public void setTipoRecibo(String tipoRecibo) { this.tipoRecibo = tipoRecibo; }
}