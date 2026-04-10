package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AmigoRequest(
    @NotBlank(message = "Documento e obrigatorio")
    String documento
) {}
