package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    // DS4-021 fix: validar formato CPF (11 digitos) ou CNPJ (14 digitos)
    @NotBlank(message = "Documento e obrigatorio")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "Documento deve ter 11 digitos (CPF) ou 14 digitos (CNPJ)")
    String documento,

    @NotBlank(message = "Tipo de documento e obrigatorio")
    @Pattern(regexp = "CPF|CNPJ", message = "Tipo de documento deve ser CPF ou CNPJ")
    String tipoDocumento,

    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    String nome,

    String email,

    String telefone,

    String cidade,

    // DS4-021 fix: senha minima de 6 caracteres
    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    String senha
) {}
