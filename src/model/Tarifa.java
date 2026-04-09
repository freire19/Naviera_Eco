package model;

import java.math.BigDecimal;

public class Tarifa {
    private int id; // ID da tarifa (PK, int)
    private long rotaId; // ID da rota (FK, long)
    private int tipoPassageiroId; // ID do tipo de passagem (FK, int)

    // Campos para exibição na tabela e preenchimento dos ComboBoxes (se necessário)
    private String nomeRota; // Ex: "Origem - Destino"
    private String nomeTipoPassageiro;

    private BigDecimal valorTransporte;
    private BigDecimal valorAlimentacao;
    private BigDecimal valorCargas;
    private BigDecimal valorDesconto; // Renomeado de valorDescontoTarifa para corresponder ao DB

    // Construtor padrão
    public Tarifa() {
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) { // Setter para int
        this.id = id;
    }

    public long getRotaId() {
        return rotaId;
    }

    public void setRotaId(long rotaId) { // Setter para long
        this.rotaId = rotaId;
    }

    public int getTipoPassageiroId() {
        return tipoPassageiroId;
    }

    public void setTipoPassageiroId(int tipoPassageiroId) { // Setter para int
        this.tipoPassageiroId = tipoPassageiroId;
    }

    public String getNomeRota() {
        return nomeRota;
    }

    public void setNomeRota(String nomeRota) {
        this.nomeRota = nomeRota;
    }

    public String getNomeTipoPassageiro() {
        return nomeTipoPassageiro;
    }

    public void setNomeTipoPassageiro(String nomeTipoPassageiro) {
        this.nomeTipoPassageiro = nomeTipoPassageiro;
    }

    public BigDecimal getValorTransporte() {
        return valorTransporte != null ? valorTransporte : BigDecimal.ZERO;
    }

    public void setValorTransporte(BigDecimal valorTransporte) {
        this.valorTransporte = valorTransporte;
    }

    public BigDecimal getValorAlimentacao() {
        return valorAlimentacao != null ? valorAlimentacao : BigDecimal.ZERO;
    }

    public void setValorAlimentacao(BigDecimal valorAlimentacao) {
        this.valorAlimentacao = valorAlimentacao;
    }

    public BigDecimal getValorCargas() {
        return valorCargas != null ? valorCargas : BigDecimal.ZERO;
    }

    public void setValorCargas(BigDecimal valorCargas) {
        this.valorCargas = valorCargas;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto != null ? valorDesconto : BigDecimal.ZERO;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    // toString() pode ser útil para debugging ou para ComboBoxes simples
    @Override
    public String toString() {
        return "Tarifa [id=" + id + ", rota=" + nomeRota + ", tipoPassageiro=" + nomeTipoPassageiro + "]";
    }

    // DP033: equals/hashCode para collection performance
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tarifa that = (Tarifa) o;
        return id == that.id;
    }
    @Override
    public int hashCode() { return Integer.hashCode(id); }
}