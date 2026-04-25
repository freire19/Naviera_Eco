package com.naviera.api.service;

import com.naviera.api.config.ApiException;
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
    public Map<String, Object> registrarPosicao(Integer empresaId, Long idEmbarcacao, Long idViagem,
            double latitude, double longitude, Double velocidade, Double curso) {
        // Validar que a embarcacao pertence a esta empresa
        int count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM embarcacoes WHERE id_embarcacao = ? AND empresa_id = ?",
            Integer.class, idEmbarcacao, empresaId);
        if (count == 0) throw ApiException.badRequest("Embarcacao nao pertence a esta empresa");

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

    // #DP064: viagem de 5 dias com GPS ativo coleta ~43k pontos — payload de 1-3MB. Limitar
    //   aos 5000 pontos mais recentes (subquery DESC + LIMIT) e devolver em ordem cronologica.
    //   Endpoint /viagens/{id}/rastreio e publico por design (id_viagem e globalmente unico).
    public List<Map<String, Object>> historicoViagem(Long idViagem) {
        return jdbc.queryForList("""
            SELECT latitude, longitude, velocidade_nos, curso_graus, timestamp FROM (
                SELECT latitude, longitude, velocidade_nos, curso_graus, timestamp
                FROM embarcacao_gps
                WHERE id_viagem = ?
                ORDER BY timestamp DESC
                LIMIT 5000
            ) sub
            ORDER BY timestamp ASC""", idViagem);
    }

    // #DP065: cache TTL 30s para mapa publico — endpoint pode ser chamado a cada segundo
    //   por N clientes. DISTINCT ON e custoso; cachear evita seq scan em embarcacao_gps.
    //   Cache em memoria simples; em multi-instance trocaria por Redis.
    private static final long CACHE_TTL_MS = 30_000L;
    private volatile List<Map<String, Object>> cacheTodasUltimas;
    private volatile long cacheTodasUltimasExpiresAt;

    public List<Map<String, Object>> todasUltimasPosicoes() {
        long now = System.currentTimeMillis();
        List<Map<String, Object>> snap = cacheTodasUltimas;
        if (snap != null && now < cacheTodasUltimasExpiresAt) return snap;

        List<Map<String, Object>> fresh = jdbc.queryForList("""
            SELECT DISTINCT ON (g.id_embarcacao)
                   g.id_embarcacao AS embarcacao_id, e.nome,
                   g.latitude, g.longitude, g.timestamp AS ultima_atualizacao
            FROM embarcacao_gps g
            JOIN embarcacoes e ON g.id_embarcacao = e.id_embarcacao
            ORDER BY g.id_embarcacao, g.timestamp DESC
            LIMIT 1000
            """);
        cacheTodasUltimas = fresh;
        cacheTodasUltimasExpiresAt = now + CACHE_TTL_MS;
        return fresh;
    }
}
