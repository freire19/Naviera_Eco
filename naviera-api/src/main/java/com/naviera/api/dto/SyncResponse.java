package com.naviera.api.dto;

import java.util.List;

public record SyncResponse(
    boolean sucesso,
    String mensagem,
    int registrosRecebidos,
    int registrosEnviados,
    List<SyncRegistroDownload> registrosParaDownload,
    // #309: lista de uuids cuja aplicacao falhou no servidor — Desktop NAO deve marcar
    //   esses registros como sincronizados (precisam ser reenviados na proxima sync).
    List<String> uuidsFalha
) {
    public SyncResponse(boolean sucesso, String mensagem, int registrosRecebidos,
                        int registrosEnviados, List<SyncRegistroDownload> registrosParaDownload) {
        this(sucesso, mensagem, registrosRecebidos, registrosEnviados, registrosParaDownload, List.of());
    }
}
