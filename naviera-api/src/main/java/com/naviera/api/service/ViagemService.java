package com.naviera.api.service;

import com.naviera.api.dto.ViagemDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ViagemService {
    private final JdbcTemplate jdbc;
    public ViagemService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ViagemDTO> buscarAtivas() {
        String sql = """
            SELECT v.id_viagem as id, emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   COALESCE(hs.descricao_horario_saida, '') as horario_saida,
                   v.ativa, v.is_atual
            FROM viagens v
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            WHERE v.ativa = true
            ORDER BY v.data_viagem DESC
            """;
        return jdbc.query(sql, (rs, i) -> new ViagemDTO(
            rs.getLong("id"), rs.getString("embarcacao"),
            rs.getString("origem"), rs.getString("destino"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null,
            rs.getString("horario_saida"),
            rs.getBoolean("ativa"), rs.getBoolean("is_atual")
        ));
    }

    public List<ViagemDTO> buscarPorEmbarcacao(Long embarcacaoId) {
        String sql = """
            SELECT v.id_viagem as id, emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   COALESCE(hs.descricao_horario_saida, '') as horario_saida,
                   v.ativa, v.is_atual
            FROM viagens v
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            WHERE v.id_embarcacao = ? AND v.ativa = true AND v.data_chegada >= CURRENT_DATE
            ORDER BY v.data_viagem ASC LIMIT 10
            """;
        return jdbc.query(sql, (rs, i) -> new ViagemDTO(
            rs.getLong("id"), rs.getString("embarcacao"),
            rs.getString("origem"), rs.getString("destino"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null,
            rs.getString("horario_saida"),
            rs.getBoolean("ativa"), rs.getBoolean("is_atual")
        ), embarcacaoId);
    }
}
