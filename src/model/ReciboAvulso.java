package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReciboAvulso {
    private int id;
    private int idViagem;
    private String nomePagador;
    private String referenteA;
    private BigDecimal valor = BigDecimal.ZERO;
    private LocalDate dataEmissao;
    private String tipoRecibo;

    public ReciboAvulso() {}

    public ReciboAvulso(int idViagem, String nomePagador, String referenteA, BigDecimal valor, LocalDate dataEmissao) {
        this.idViagem = idViagem;
        this.nomePagador = nomePagador;
        this.referenteA = referenteA;
        this.valor = valor != null ? valor : BigDecimal.ZERO;
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
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor != null ? valor : BigDecimal.ZERO; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public String getTipoRecibo() { return tipoRecibo; }
    public void setTipoRecibo(String tipoRecibo) { this.tipoRecibo = tipoRecibo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReciboAvulso that = (ReciboAvulso) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
