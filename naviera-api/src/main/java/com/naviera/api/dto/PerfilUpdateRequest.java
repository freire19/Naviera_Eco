package com.naviera.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PerfilUpdateRequest(
    @Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    String nome,

    @Email(message = "Email invalido")
    @Size(max = 200, message = "Email muito longo")
    String email,

    @Size(max = 20, message = "Telefone muito longo")
    String telefone,

    @Size(max = 100, message = "Cidade muito longa")
    String cidade
) {}
