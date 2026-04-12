package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class GpsService {
    private final JdbcTemplate jdbc;

    public GpsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> registrarPosicao(Long idEmbarcacao, Long idViagem,
            double latitude, double longitude, Double velocidade, Double curso) {
        jdbc.update("""
            INSERT INTO embarcacao_gps (id_embarcacao, id_viagem, latitude, longitude, velocidade_nos, curso_graus)
            VALUES (?, ?, ?, ?, ?, ?)""",
            idEmbarcacao, idViagem, latitude, longitude, velocidade, curso);
        return Map.of("mensagem", "Posicao registrada");
    }

    public Map<String, Object> ultimaPosicao(Long idEmbarcacao) {
        var list = jdbc.queryForList("""
            SELECT g.*, e.nome AS nome_embarcacao
            FROM embarcacao_gps g
            JOIN embarcacoes e ON g.id_embarcacao = e.id_embarcacao
            WHERE g.id_embarcacao = ?
            ORDER BY g.timestamp DESC
            LIMIT 1""", idEmbarcacao);
        return list.isEmpty() ? Map.of("disponivel", false) : list.get(0);
    }

    public List<Map<String, Object>> historicoViagem(Long idViagem) {
        return jdbc.queryForList("""
            SELECT latitude, longitude, velocidade_nos, curso_graus, timestamp
            FROM embarcacao_gps
            WHERE id_viagem = ?
            ORDER BY timestamp""", idViagem);
    }

    /** Última posição de cada embarcação — para mapa público de tracking */
    public List<Map<String, Object>> todasUltimasPosicoes() {
        return jdbc.queryForList("""
            SELECT DISTINCT ON (g.id_embarcacao)
                   g.id_embarcacao AS embarcacao_id, e.nome,
                   g.latitude, g.longitude, g.timestamp AS ultima_atualizacao
            FROM embarcacao_gps g
            JOIN embarcacoes e ON g.id_embarcacao = e.id_embarcacao
            ORDER BY g.id_embarcacao, g.timestamp DESC
            """);
    }
}
