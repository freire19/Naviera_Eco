package com.naviera.api.dto;

public record BilheteDTO(
    Long id,
    Long idPassagem,
    String numeroBilhete,
    String nomePassageiro,
    String cpf,
    String origem,
    String destino,
    String embarcacao,
    String dataViagem,
    String horarioSaida,
    String acomodacao,
    String tipoPassagem,
    String valorTotal,
    String status,
    String totpSecret,
    String qrHash,
    String idViagem
) {}
