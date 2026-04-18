package com.naviera.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompraPassagemRequest(
    @NotNull(message = "ID da viagem e obrigatorio")
    Long idViagem,

    @NotNull(message = "Tipo de passagem e obrigatorio")
    Integer idTipoPassagem,

    @Pattern(regexp = "PIX|CARTAO|BARCO", message = "Forma de pagamento deve ser PIX, CARTAO ou BARCO")
    String formaPagamento
) {}
