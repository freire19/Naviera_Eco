package com.naviera.api.model;

import jakarta.persistence.*;

@Entity @Table(name = "rotas")
public class Rota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String origem;
    private String destino;

    @Column(name = "empresa_id")
    private Integer empresaId;

    public Long getId() { return id; }
    public String getOrigem() { return origem; }
    public String getDestino() { return destino; }
    public Integer getEmpresaId() { return empresaId; }
}
