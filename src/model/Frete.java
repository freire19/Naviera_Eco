package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Frete {

    private long idFrete;
    private String numeroFrete;
    private long idViagem;
    private String nomeRemetente;
    private String nomeDestinatario;
    private String nomeRota;
    private LocalDate dataViagem;
    private LocalDate dataEmissao;
    private BigDecimal valorNominal = BigDecimal.ZERO;
    private BigDecimal valorDevedor = BigDecimal.ZERO;
    private BigDecimal valorPago = BigDecimal.ZERO;
    private String nomeConferente;
    private String status;
    private int totalVolumes;

    // Getters e Setters
    public long getIdFrete() { return idFrete; }
    public void setIdFrete(long idFrete) { this.idFrete = idFrete; }

    public String getNumeroFrete() { return numeroFrete; }
    public void setNumeroFrete(String numeroFrete) { this.numeroFrete = numeroFrete; }

    public long getIdViagem() { return idViagem; }
    public void setIdViagem(long idViagem) { this.idViagem = idViagem; }

    public String getNomeRemetente() { return nomeRemetente; }
    public void setNomeRemetente(String nomeRemetente) { this.nomeRemetente = nomeRemetente; }

    public String getNomeDestinatario() { return nomeDestinatario; }
    public void setNomeDestinatario(String nomeDestinatario) { this.nomeDestinatario = nomeDestinatario; }

    public String getNomeRota() { return nomeRota; }
    public void setNomeRota(String nomeRota) { this.nomeRota = nomeRota; }

    public LocalDate getDataViagem() { return dataViagem; }
    public void setDataViagem(LocalDate dataViagem) { this.dataViagem = dataViagem; }

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public BigDecimal getValorNominal() { return valorNominal; }
    public void setValorNominal(BigDecimal valorNominal) { this.valorNominal = valorNominal != null ? valorNominal : BigDecimal.ZERO; }

    public BigDecimal getValorDevedor() { return valorDevedor; }
    public void setValorDevedor(BigDecimal valorDevedor) { this.valorDevedor = valorDevedor != null ? valorDevedor : BigDecimal.ZERO; }

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago != null ? valorPago : BigDecimal.ZERO; }

    public String getNomeConferente() { return nomeConferente; }
    public void setNomeConferente(String nomeConferente) { this.nomeConferente = nomeConferente; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalVolumes() { return totalVolumes; }
    public void setTotalVolumes(int totalVolumes) { this.totalVolumes = totalVolumes; }

    // DP033: equals/hashCode para collection performance
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Frete that = (Frete) o;
        return idFrete == that.idFrete;
    }
    @Override
    public int hashCode() { return Long.hashCode(idFrete); }
}
