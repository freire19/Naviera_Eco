package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompraPassagemRequest(
    @NotNull(message = "ID da viagem e obrigatorio")
    Long idViagem,

    @NotNull(message = "Tipo de passagem e obrigatorio")
    Integer idTipoPassagem,

    @NotBlank(message = "Forma de pagamento e obrigatoria")
    String formaPagamento
) {}
