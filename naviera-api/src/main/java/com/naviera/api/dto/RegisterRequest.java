package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank(message = "Documento e obrigatorio")
    String documento,

    @NotBlank(message = "Tipo de documento e obrigatorio")
    String tipoDocumento,

    @NotBlank(message = "Nome e obrigatorio")
    String nome,

    String email,

    String telefone,

    String cidade,

    @NotBlank(message = "Senha e obrigatoria")
    String senha
) {}
