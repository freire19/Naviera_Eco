package com.naviera.api.dto;
public record ViagemDTO(Long id, String embarcacao, String origem, String destino, String dataViagem,
    String dataChegada, String horarioSaida, boolean ativa, boolean atual) {}
