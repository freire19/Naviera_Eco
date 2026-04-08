package com.naviera.api.service;

import com.naviera.api.dto.EmbarcacaoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmbarcacaoService {
    private final JdbcTemplate jdbc;
    public EmbarcacaoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<EmbarcacaoDTO> listarComStatus() {
        String sql = """
            SELECT emb.id, emb.nome, emb.capacidade_passageiros,
                   CASE WHEN v.id IS NOT NULL AND v.is_atual = true THEN 'EM_VIAGEM'
                        ELSE 'NO_PORTO' END as status,
                   COALESCE(r.origem || ' → ' || r.destino, '') as rota_atual,
                   v.data_viagem, v.data_chegada
            FROM embarcacoes emb
            LEFT JOIN LATERAL (
                SELECT vi.id, vi.is_atual, vi.data_viagem, vi.data_chegada, vi.id_rota
                FROM viagens vi WHERE vi.id_embarcacao = emb.id AND vi.ativa = true
                ORDER BY vi.data_viagem DESC LIMIT 1
            ) v ON true
            LEFT JOIN rotas r ON v.id_rota = r.id
            ORDER BY emb.nome
            """;
        return jdbc.query(sql, (rs, i) -> new EmbarcacaoDTO(
            rs.getLong("id"), rs.getString("nome"),
            rs.getInt("capacidade_passageiros"),
            rs.getString("status"), rs.getString("rota_atual"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null
        ));
    }
}
