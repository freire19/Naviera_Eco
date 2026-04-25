package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class OpPassagemService {
    private final JdbcTemplate jdbc;

    public OpPassagemService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // #DL037: listar/resumo filtrar `excluido` (nao incluir soft-deleted)
    // #DS5-018: colunas explicitas — qr_pix_payload e id_transacao_psp ficam fora do listar
    //   default (leak de dados financeiros). Endpoints que precisam do payload usam consulta dedicada.
    private static final String COLUNAS_LISTAR = """
        p.id_passagem, p.numero_bilhete, p.id_passageiro, p.id_viagem,
        p.data_emissao, p.assento, p.id_acomodacao, p.id_rota,
        p.id_tipo_passagem, p.id_agente, p.numero_requisicao,
        p.valor_alimentacao, p.valor_transporte, p.valor_cargas,
        p.valor_desconto_tarifa, p.valor_total, p.valor_desconto_geral,
        p.valor_a_pagar, p.valor_pago, p.troco, p.valor_devedor,
        p.id_forma_pagamento, p.id_caixa, p.id_usuario_emissor,
        p.status_passagem, p.observacoes, p.id_horario_saida,
        p.valor_pagamento_dinheiro, p.valor_pagamento_pix, p.valor_pagamento_cartao,
        p.forma_pagamento_app, p.desconto_app, p.data_pagamento_app,
        p.empresa_id""";

    public List<Map<String, Object>> listar(Integer empresaId, Long viagemId) {
        String sql = "SELECT " + COLUNAS_LISTAR + ", pas.nome_passageiro, pas.numero_documento\n"
            + "FROM passagens p\n"
            + "LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro\n"
            + "WHERE p.empresa_id = ? AND (p.excluido IS NULL OR p.excluido = FALSE)"
            + (viagemId != null ? " AND p.id_viagem = ?" : "")
            + " ORDER BY p.numero_bilhete DESC";
        return viagemId != null
            ? jdbc.queryForList(sql, empresaId, viagemId)
            : jdbc.queryForList(sql, empresaId);
    }

    public Map<String, Object> resumo(Integer empresaId, Long viagemId) {
        return jdbc.queryForMap("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(valor_total), 0) AS valor_total,
                   COALESCE(SUM(valor_pago), 0) AS valor_pago
            FROM passagens WHERE id_viagem = ? AND empresa_id = ?
              AND (excluido IS NULL OR excluido = FALSE)""", viagemId, empresaId);
    }
}
