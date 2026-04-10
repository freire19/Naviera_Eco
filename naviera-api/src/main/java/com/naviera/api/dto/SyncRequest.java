package com.naviera.api.dto;

import java.util.List;

public record SyncRequest(
    String tabela,
    String ultimaSincronizacao,
    List<SyncRegistro> registros
) {}
