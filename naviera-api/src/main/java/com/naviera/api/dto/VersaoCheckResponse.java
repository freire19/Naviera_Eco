package com.naviera.api.dto;

public record VersaoCheckResponse(
    boolean atualizado,
    String versaoAtual,
    String versaoNova,
    boolean obrigatoria,
    String urlDownload,
    String changelog
) {}
