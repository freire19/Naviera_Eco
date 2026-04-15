package com.naviera.api.service;

import com.naviera.api.dto.EmbarcacaoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmbarcacaoService {
    private final JdbcTemplate jdbc;
    public EmbarcacaoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // #DB143: empresaId parameter added to prevent cross-tenant data leak
    public List<EmbarcacaoDTO> listarComStatus(Integer empresaId) {
        String sql = """
            SELECT emb.id_embarcacao as id, emb.nome, emb.capacidade_passageiros,
                   CASE WHEN v_atual.id_viagem IS NOT NULL THEN 'EM_VIAGEM'
                        ELSE 'NO_PORTO' END as status,
                   COALESCE(r_atual.origem || ' → ' || r_atual.destino,
                            r_prox.origem || ' → ' || r_prox.destino,
                            COALESCE(emb.rota_principal, '')) as rota_atual,
                   COALESCE(v_prox.data_viagem, v_atual.data_viagem) as data_viagem,
                   COALESCE(v_prox.data_chegada, v_atual.data_chegada) as data_chegada,
                   emb.foto_url, emb.link_externo, emb.descricao,
                   emb.rota_principal, emb.horario_saida_padrao, emb.telefone
            FROM embarcacoes emb
            LEFT JOIN LATERAL (
                SELECT vi.id_viagem, vi.data_viagem, vi.data_chegada, vi.id_rota
                FROM viagens vi
                WHERE vi.id_embarcacao = emb.id_embarcacao AND vi.ativa = true
                  AND CURRENT_DATE >= vi.data_viagem AND CURRENT_DATE <= vi.data_chegada
                LIMIT 1
            ) v_atual ON true
            LEFT JOIN rotas r_atual ON v_atual.id_rota = r_atual.id
            LEFT JOIN LATERAL (
                SELECT vi.id_viagem, vi.data_viagem, vi.data_chegada, vi.id_rota
                FROM viagens vi
                WHERE vi.id_embarcacao = emb.id_embarcacao AND vi.ativa = true
                  AND vi.data_viagem > CURRENT_DATE
                ORDER BY vi.data_viagem ASC LIMIT 1
            ) v_prox ON true
            LEFT JOIN rotas r_prox ON v_prox.id_rota = r_prox.id
            WHERE emb.empresa_id = ?
            ORDER BY emb.nome
            """;
        return jdbc.query(sql, (rs, i) -> new EmbarcacaoDTO(
            rs.getLong("id"), rs.getString("nome"),
            rs.getInt("capacidade_passageiros"),
            rs.getString("status"), rs.getString("rota_atual"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null,
            rs.getString("foto_url"), rs.getString("link_externo"), rs.getString("descricao"),
            rs.getString("rota_principal"), rs.getString("horario_saida_padrao"), rs.getString("telefone")
        ), empresaId);
    }
}
