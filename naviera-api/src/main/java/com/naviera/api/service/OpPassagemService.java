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
        if (viagemId != null) {
            return jdbc.queryForList("""
                SELECT p.*, pas.nome AS nome_passageiro, pas.numero_doc
                FROM passagens p
                LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
                WHERE p.empresa_id = ? AND p.id_viagem = ?
                ORDER BY p.num_bilhete DESC""", empresaId, viagemId);
        }
        return jdbc.queryForList("""
            SELECT p.*, pas.nome AS nome_passageiro, pas.numero_doc
            FROM passagens p
            LEFT JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro
            WHERE p.empresa_id = ?
            ORDER BY p.num_bilhete DESC""", empresaId);
    }

    public Map<String, Object> resumo(Integer empresaId, Long viagemId) {
        return jdbc.queryForMap("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(valor_total), 0) AS valor_total,
                   COALESCE(SUM(valor_pago), 0) AS valor_pago
            FROM passagens WHERE id_viagem = ? AND empresa_id = ?""", viagemId, empresaId);
    }
}
