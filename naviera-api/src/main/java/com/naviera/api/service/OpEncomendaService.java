package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class OpEncomendaService {
    private final JdbcTemplate jdbc;

    public OpEncomendaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listar(Integer empresaId, Long viagemId) {
        if (viagemId != null) {
            return jdbc.queryForList(
                "SELECT * FROM encomendas WHERE empresa_id = ? AND id_viagem = ? ORDER BY id_encomenda DESC",
                empresaId, viagemId);
        }
        return jdbc.queryForList(
            "SELECT * FROM encomendas WHERE empresa_id = ? ORDER BY id_encomenda DESC", empresaId);
    }

    public Map<String, Object> resumo(Integer empresaId, Long viagemId) {
        return jdbc.queryForMap("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(total_a_pagar), 0) AS valor_total,
                   COALESCE(SUM(valor_pago), 0) AS valor_pago
            FROM encomendas WHERE id_viagem = ? AND empresa_id = ?""", viagemId, empresaId);
    }
}
