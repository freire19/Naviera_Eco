package com.naviera.api.dto;

public record AmigoDTO(
    Long id,
    Long idAmigo,
    String nome,
    String cidade,
    String fotoUrl,
    String status,
    String dataSolicitacao
) {}
