package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Documento e obrigatorio")
    String documento,

    @NotBlank(message = "Senha e obrigatoria")
    String senha
) {}
