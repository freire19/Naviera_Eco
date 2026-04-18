package com.naviera.api.dto;

import jakarta.validation.constraints.Pattern;

public record PagarFreteRequest(
    @Pattern(regexp = "PIX|CARTAO|BOLETO|BARCO", message = "Forma de pagamento deve ser PIX, CARTAO, BOLETO ou BARCO")
    String formaPagamento
) {}
