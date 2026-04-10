package com.naviera.api.dto;

public record SyncRegistroDownload(
    String uuid,
    String acao,
    String ultimaAtualizacao,
    String dadosJson
) {}
