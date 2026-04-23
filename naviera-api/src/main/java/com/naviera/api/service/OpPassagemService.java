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

    public List<Map<String, Object>> listar(Integer empresaId, Long viagemId) {
        String sql = """
            SELECT p.*, pas.nome_passageiro, pas.numero_documento
            FROM passagens p
            LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
            WHERE p.empresa_id = ?"""
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
            FROM passagens WHERE id_viagem = ? AND empresa_id = ?""", viagemId, empresaId);
    }
}
