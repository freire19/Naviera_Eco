package com.naviera.api.dto;

import jakarta.validation.constraints.Pattern;

public record PagarEncomendaRequest(
    @Pattern(regexp = "PIX|CARTAO|BARCO", message = "Forma de pagamento deve ser PIX, CARTAO ou BARCO")
    String formaPagamento
) {}
