package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class TarifaService {
    private final JdbcTemplate jdbc;
    public TarifaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> listarPorRota() {
        String sql = """
            SELECT r.origem, r.destino,
                   tp.nome as tipo_passageiro,
                   t.valor_transporte, t.valor_alimentacao, t.valor_cargas, t.valor_desconto
            FROM tarifas t
            JOIN rotas r ON t.id_rota = r.id
            JOIN tipos_passageiro tp ON t.id_tipo_passageiro = tp.id
            ORDER BY r.origem, r.destino, tp.nome
            """;
        return jdbc.queryForList(sql);
    }
}
