package com.naviera.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SyncRequest(
    @NotBlank(message = "Tabela e obrigatoria")
    String tabela,

    String ultimaSincronizacao,

    @Valid
    List<SyncRegistro> registros
) {}
