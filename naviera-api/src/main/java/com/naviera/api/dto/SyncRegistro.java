package com.naviera.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SyncRegistro(
    @NotBlank(message = "UUID e obrigatorio")
    String uuid,

    @NotBlank(message = "Acao e obrigatoria")
    String acao,

    String ultimaAtualizacao,

    String dadosJson
) {}
