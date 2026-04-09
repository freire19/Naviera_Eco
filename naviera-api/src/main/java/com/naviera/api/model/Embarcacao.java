package com.naviera.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "embarcacoes")
public class Embarcacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_embarcacao") private Long id;
    private String nome;
    @Column(name = "registro_capitania") private String registroCapitania;
    @Column(name = "capacidade_passageiros") private Integer capacidadePassageiros;
    @Column(name = "capacidade_carga_toneladas") private BigDecimal capacidadeCargaToneladas;
    private String observacoes;

    public Long getId() { return id; } public String getNome() { return nome; }
    public Integer getCapacidadePassageiros() { return capacidadePassageiros; }
    public BigDecimal getCapacidadeCargaToneladas() { return capacidadeCargaToneladas; }
}
