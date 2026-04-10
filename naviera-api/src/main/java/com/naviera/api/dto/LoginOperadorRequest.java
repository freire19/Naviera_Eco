package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginOperadorRequest(
    @NotBlank(message = "Login e obrigatorio")
    String login,

    @NotBlank(message = "Senha e obrigatoria")
    String senha
) {}
