package com.naviera.api.dto;

import java.util.List;

public record SyncResponse(
    boolean sucesso,
    String mensagem,
    int registrosRecebidos,
    int registrosEnviados,
    List<SyncRegistroDownload> registrosParaDownload
) {}
