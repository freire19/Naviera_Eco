package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class OpViagemWriteService {
    private final JdbcTemplate jdbc;

    public OpViagemWriteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> criar(Integer empresaId, Map<String, Object> dados) {
        Long idViagem = ((Number) dados.get("id_viagem")).longValue();
        Long idEmbarcacao = ((Number) dados.get("id_embarcacao")).longValue();
        Long idRota = ((Number) dados.get("id_rota")).longValue();
        String dataViagem = (String) dados.get("data_viagem");
        String dataChegada = (String) dados.get("data_chegada");
        String descricao = (String) dados.get("descricao");
        Integer idHorarioSaida = dados.get("id_horario_saida") != null ? ((Number) dados.get("id_horario_saida")).intValue() : null;

        jdbc.update("""
            INSERT INTO viagens (id_viagem, data_viagem, data_chegada, descricao, ativa, is_atual, id_embarcacao, id_rota, id_horario_saida, empresa_id)
            VALUES (?, ?::date, ?::date, ?, TRUE, FALSE, ?, ?, ?, ?)""",
            idViagem, dataViagem, dataChegada, descricao, idEmbarcacao, idRota, idHorarioSaida, empresaId);

        return Map.of("mensagem", "Viagem criada", "id_viagem", idViagem);
    }

    @Transactional
    public Map<String, Object> atualizar(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE viagens SET data_viagem = ?::date, data_chegada = ?::date, descricao = ?,
                   id_embarcacao = ?, id_rota = ?
            WHERE id_viagem = ? AND empresa_id = ?""",
            dados.get("data_viagem"), dados.get("data_chegada"), dados.get("descricao"),
            dados.get("id_embarcacao"), dados.get("id_rota"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Viagem nao encontrada");
        return Map.of("mensagem", "Viagem atualizada");
    }

    @Transactional
    public Map<String, Object> ativar(Integer empresaId, Long id, boolean ativa) {
        // Desativar todas as viagens da empresa antes de ativar a nova
        if (ativa) {
            jdbc.update("UPDATE viagens SET ativa = FALSE WHERE empresa_id = ?", empresaId);
        }
        int rows = jdbc.update("UPDATE viagens SET ativa = ? WHERE id_viagem = ? AND empresa_id = ?",
            ativa, id, empresaId);
        if (rows == 0) throw ApiException.notFound("Viagem nao encontrada");
        return Map.of("mensagem", ativa ? "Viagem ativada" : "Viagem desativada");
    }
}
