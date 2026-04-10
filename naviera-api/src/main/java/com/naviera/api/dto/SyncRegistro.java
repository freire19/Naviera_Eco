package com.naviera.api.dto;

public record SyncRegistro(
    String uuid,
    String acao,
    String ultimaAtualizacao,
    String dadosJson
) {}
