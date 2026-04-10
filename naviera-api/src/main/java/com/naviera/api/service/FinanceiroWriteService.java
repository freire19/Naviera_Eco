package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import static com.naviera.api.config.MoneyUtils.toBigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class FinanceiroWriteService {
    private final JdbcTemplate jdbc;

    public FinanceiroWriteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> criarSaida(Integer empresaId, Map<String, Object> dados) {
        jdbc.update("""
            INSERT INTO financeiro_saidas (id_viagem, descricao, valor, valor_total, data, id_categoria,
                id_funcionario, tipo, excluido, empresa_id)
            VALUES (?, ?, ?, ?, ?::date, ?, ?, ?, FALSE, ?)""",
            dados.get("id_viagem"), dados.get("descricao"),
            toBigDecimal(dados.get("valor")), toBigDecimal(dados.get("valor_total")),
            dados.get("data"), dados.get("id_categoria"), dados.get("id_funcionario"),
            dados.get("tipo"), empresaId);
        return Map.of("mensagem", "Saida registrada");
    }

    @Transactional
    public Map<String, Object> excluirSaida(Integer empresaId, Long id) {
        int rows = jdbc.update(
            "UPDATE financeiro_saidas SET excluido = TRUE WHERE id = ? AND empresa_id = ?", id, empresaId);
        if (rows == 0) throw ApiException.notFound("Saida nao encontrada");
        return Map.of("mensagem", "Saida excluida");
    }

}
