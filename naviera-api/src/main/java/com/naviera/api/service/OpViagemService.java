package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class OpViagemService {
    private final JdbcTemplate jdbc;

    public OpViagemService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // #DS5-042: limit fixo evita resposta ilimitada (50k+ linhas) que estoura memoria do BFF.
    //   Frontend ja pagina; clientes que precisam de mais devem usar query com filtro de data.
    private static final int LISTAR_TODAS_LIMIT = 1000;

    public List<Map<String, Object>> listarTodas(Integer empresaId) {
        return jdbc.queryForList("""
            SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
                   e.nome AS nome_embarcacao, r.origem, r.destino
            FROM viagens v
            LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
            LEFT JOIN rotas r ON v.id_rota = r.id_rota
            WHERE v.empresa_id = ?
            ORDER BY v.data_viagem DESC
            LIMIT ?""", empresaId, LISTAR_TODAS_LIMIT);
    }

    public Map<String, Object> buscarAtiva(Integer empresaId) {
        var list = jdbc.queryForList("""
            SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa,
                   e.nome AS nome_embarcacao, r.origem, r.destino
            FROM viagens v
            LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
            LEFT JOIN rotas r ON v.id_rota = r.id_rota
            WHERE v.ativa = TRUE AND v.empresa_id = ?
            ORDER BY v.data_viagem DESC
            LIMIT 1""", empresaId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> buscarPorId(Integer empresaId, Long id) {
        var list = jdbc.queryForList("""
            SELECT v.*, e.nome AS nome_embarcacao, r.origem, r.destino
            FROM viagens v
            LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao
            LEFT JOIN rotas r ON v.id_rota = r.id_rota
            WHERE v.id_viagem = ? AND v.empresa_id = ?""", id, empresaId);
        if (list.isEmpty()) throw ApiException.notFound("Viagem nao encontrada");
        return list.get(0);
    }
}
