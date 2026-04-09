package com.naviera.api.dto;

public record CompraPassagemRequest(
    Long idViagem,
    Integer idTipoPassagem,
    String formaPagamento
) {}
