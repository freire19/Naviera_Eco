package com.naviera.api.service;

import com.naviera.api.dto.ViagemDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class ViagemService {
    private final JdbcTemplate jdbc;
    public ViagemService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // DS4-008 fix: empresaId != null → filtrar (operador), null → cross-tenant (app)
    // #DL041: nao retornar viagens com data_chegada ja no passado
    public List<ViagemDTO> buscarAtivas(Integer empresaId) {
        String sql = """
            SELECT v.id_viagem as id, emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   COALESCE(hs.descricao_horario_saida, '') as horario_saida,
                   v.ativa, v.is_atual
            FROM viagens v
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            WHERE v.ativa = true AND v.data_chegada >= CURRENT_DATE""" + (empresaId != null ? " AND v.empresa_id = ?" : "") + """

            ORDER BY v.data_viagem DESC
            """;
        var mapper = new org.springframework.jdbc.core.RowMapper<ViagemDTO>() {
            public ViagemDTO mapRow(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
                return new ViagemDTO(
                    rs.getLong("id"), rs.getString("embarcacao"),
                    rs.getString("origem"), rs.getString("destino"),
                    rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
                    rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null,
                    rs.getString("horario_saida"),
                    rs.getBoolean("ativa"), rs.getBoolean("is_atual"));
            }
        };
        return empresaId != null ? jdbc.query(sql, mapper, empresaId) : jdbc.query(sql, mapper);
    }

    /** Viagens ativas de todas as empresas — cross-tenant para o app mobile */
    // #DL041: nao mostrar viagens com data_chegada passada (cliente CPF ve viagem expirada)
    public List<Map<String, Object>> buscarPublicas() {
        String sql = """
            SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao,
                   emb.nome AS nome_embarcacao, r.origem, r.destino,
                   emp.nome AS empresa_nome
            FROM viagens v
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN empresas emp ON emb.empresa_id = emp.id
            WHERE v.ativa = TRUE AND v.data_chegada >= CURRENT_DATE
            ORDER BY v.data_viagem ASC
            """;
        return jdbc.queryForList(sql);
    }

    // DS4-008 fix: empresaId nullable — operador filtrado, app cross-tenant
    public List<ViagemDTO> buscarPorEmbarcacao(Long embarcacaoId, Integer empresaId) {
        String where = "WHERE v.id_embarcacao = ? AND v.ativa = true AND v.data_chegada >= CURRENT_DATE";
        if (empresaId != null) where += " AND v.empresa_id = ?";
        String sql = """
            SELECT v.id_viagem as id, emb.nome as embarcacao, r.origem, r.destino,
                   v.data_viagem, v.data_chegada,
                   COALESCE(hs.descricao_horario_saida, '') as horario_saida,
                   v.ativa, v.is_atual
            FROM viagens v
            JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            JOIN rotas r ON v.id_rota = r.id
            LEFT JOIN aux_horarios_saida hs ON v.id_horario_saida = hs.id_horario_saida
            """ + where + " ORDER BY v.data_viagem ASC LIMIT 10";
        var mapper = new org.springframework.jdbc.core.RowMapper<ViagemDTO>() {
            public ViagemDTO mapRow(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
                return new ViagemDTO(
                    rs.getLong("id"), rs.getString("embarcacao"),
                    rs.getString("origem"), rs.getString("destino"),
                    rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
                    rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null,
                    rs.getString("horario_saida"),
                    rs.getBoolean("ativa"), rs.getBoolean("is_atual"));
            }
        };
        return empresaId != null ? jdbc.query(sql, mapper, embarcacaoId, empresaId) : jdbc.query(sql, mapper, embarcacaoId);
    }
}
