package com.naviera.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "viagens")
public class Viagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "data_viagem") private LocalDate dataViagem;
    @Column(name = "data_chegada") private LocalDate dataChegada;
    private String descricao;
    private Boolean ativa;
    @Column(name = "is_atual") private Boolean isAtual;
    @Column(name = "id_embarcacao") private Long idEmbarcacao;
    @Column(name = "id_rota") private Long idRota;
    // Transient: populados via JOIN
    @Transient private String nomeEmbarcacao;
    @Transient private String origem;
    @Transient private String destino;
    @Transient private String descricaoHorarioSaida;

    public Long getId() { return id; } public LocalDate getDataViagem() { return dataViagem; }
    public LocalDate getDataChegada() { return dataChegada; } public Boolean getAtiva() { return ativa; }
    public Boolean getIsAtual() { return isAtual; } public Long getIdEmbarcacao() { return idEmbarcacao; }
    public Long getIdRota() { return idRota; } public String getDescricao() { return descricao; }
    public String getNomeEmbarcacao() { return nomeEmbarcacao; } public void setNomeEmbarcacao(String n) { this.nomeEmbarcacao = n; }
    public String getOrigem() { return origem; } public void setOrigem(String o) { this.origem = o; }
    public String getDestino() { return destino; } public void setDestino(String d) { this.destino = d; }
    public String getDescricaoHorarioSaida() { return descricaoHorarioSaida; } public void setDescricaoHorarioSaida(String h) { this.descricaoHorarioSaida = h; }
}
