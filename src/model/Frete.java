package model;

import java.time.LocalDate;

public class Frete {

    private long idFrete;
    private String numeroFrete;
    private long idViagem;
    private String nomeRemetente;
    private String nomeDestinatario;
    private String nomeRota;
    private LocalDate dataViagem; // <--- CAMPO NOVO ADICIONADO
    private LocalDate dataEmissao;
    private double valorNominal;
    private double valorDevedor;
    private double valorPago;
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

    // --- NOVOS MÉTODOS PARA DATA DA VIAGEM ---
    public LocalDate getDataViagem() { return dataViagem; }
    public void setDataViagem(LocalDate dataViagem) { this.dataViagem = dataViagem; }
    // -----------------------------------------

    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }

    public double getValorNominal() { return valorNominal; }
    public void setValorNominal(double valorNominal) { this.valorNominal = valorNominal; }

    public double getValorDevedor() { return valorDevedor; }
    public void setValorDevedor(double valorDevedor) { this.valorDevedor = valorDevedor; }

    public double getValorPago() { return valorPago; }
    public void setValorPago(double valorPago) { this.valorPago = valorPago; }

    public String getNomeConferente() { return nomeConferente; }
    public void setNomeConferente(String nomeConferente) { this.nomeConferente = nomeConferente; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getTotalVolumes() { return totalVolumes; }
    public void setTotalVolumes(int totalVolumes) { this.totalVolumes = totalVolumes; }
}