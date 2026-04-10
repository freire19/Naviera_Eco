package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> resumo(Integer empresaId, Long viagemId) {
        var passagens = jdbc.queryForMap(
            "SELECT COUNT(*) AS total, COALESCE(SUM(valor_total), 0) AS valor FROM passagens WHERE id_viagem = ? AND empresa_id = ?",
            viagemId, empresaId);
        var encomendas = jdbc.queryForMap(
            "SELECT COUNT(*) AS total, COALESCE(SUM(total_a_pagar), 0) AS valor FROM encomendas WHERE id_viagem = ? AND empresa_id = ?",
            viagemId, empresaId);
        var fretes = jdbc.queryForMap(
            "SELECT COUNT(*) AS total, COALESCE(SUM(valor_nominal), 0) AS valor FROM fretes WHERE id_viagem = ? AND empresa_id = ?",
            viagemId, empresaId);

        var result = new LinkedHashMap<String, Object>();
        result.put("passagens", passagens);
        result.put("encomendas", encomendas);
        result.put("fretes", fretes);
        return result;
    }
}
