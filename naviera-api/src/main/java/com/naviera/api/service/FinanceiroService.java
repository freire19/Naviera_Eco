package com.naviera.api.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinanceiroService {
    private final JdbcTemplate jdbc;

    public FinanceiroService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarEntradas(Integer empresaId, Long viagemId) {
        if (viagemId != null) {
            return jdbc.queryForList(
                "SELECT * FROM financeiro_entradas WHERE empresa_id = ? AND id_viagem = ? ORDER BY data_entrada DESC",
                empresaId, viagemId);
        }
        return jdbc.queryForList(
            "SELECT * FROM financeiro_entradas WHERE empresa_id = ? ORDER BY data_entrada DESC", empresaId);
    }

    public List<Map<String, Object>> listarSaidas(Integer empresaId, Long viagemId) {
        if (viagemId != null) {
            return jdbc.queryForList(
                "SELECT * FROM financeiro_saidas WHERE excluido = FALSE AND empresa_id = ? AND id_viagem = ? ORDER BY data DESC",
                empresaId, viagemId);
        }
        return jdbc.queryForList(
            "SELECT * FROM financeiro_saidas WHERE excluido = FALSE AND empresa_id = ? ORDER BY data DESC", empresaId);
    }

    public Map<String, Object> balanco(Integer empresaId, Long viagemId) {
        // #238: receitas nao devem incluir registros soft-deleted.
        BigDecimal passagens = jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor_pago), 0) FROM passagens WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)",
            BigDecimal.class, viagemId, empresaId);
        BigDecimal encomendas = jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor_pago), 0) FROM encomendas WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)",
            BigDecimal.class, viagemId, empresaId);
        BigDecimal fretes = jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor_pago), 0) FROM fretes WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)",
            BigDecimal.class, viagemId, empresaId);
        BigDecimal despesas = jdbc.queryForObject(
            "SELECT COALESCE(SUM(valor), 0) FROM financeiro_saidas WHERE id_viagem = ? AND excluido = FALSE AND empresa_id = ?",
            BigDecimal.class, viagemId, empresaId);

        BigDecimal totalReceitas = passagens.add(encomendas).add(fretes);
        BigDecimal saldo = totalReceitas.subtract(despesas);

        var result = new LinkedHashMap<String, Object>();
        var receitas = new LinkedHashMap<String, Object>();
        receitas.put("passagens", passagens);
        receitas.put("encomendas", encomendas);
        receitas.put("fretes", fretes);
        result.put("receitas", receitas);
        result.put("totalReceitas", totalReceitas);
        result.put("totalDespesas", despesas);
        result.put("saldo", saldo);
        return result;
    }
}
