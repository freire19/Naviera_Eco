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

    // #DP067: cap defensivo para evitar payload ilimitado em tenants antigos com 10k+ linhas.
    //   Listas exibidas no app/web sao paginadas pelo cliente; 1000 itens cobre uso normal.
    private static final int MAX_LINHAS_LISTAGEM = 1000;

    public List<Map<String, Object>> listarEntradas(Integer empresaId, Long viagemId) {
        if (viagemId != null) {
            return jdbc.queryForList(
                "SELECT * FROM financeiro_entradas WHERE empresa_id = ? AND id_viagem = ? ORDER BY data_entrada DESC LIMIT " + MAX_LINHAS_LISTAGEM,
                empresaId, viagemId);
        }
        return jdbc.queryForList(
            "SELECT * FROM financeiro_entradas WHERE empresa_id = ? ORDER BY data_entrada DESC LIMIT " + MAX_LINHAS_LISTAGEM, empresaId);
    }

    public List<Map<String, Object>> listarSaidas(Integer empresaId, Long viagemId) {
        if (viagemId != null) {
            return jdbc.queryForList(
                "SELECT * FROM financeiro_saidas WHERE excluido = FALSE AND empresa_id = ? AND id_viagem = ? ORDER BY data DESC LIMIT " + MAX_LINHAS_LISTAGEM,
                empresaId, viagemId);
        }
        return jdbc.queryForList(
            "SELECT * FROM financeiro_saidas WHERE excluido = FALSE AND empresa_id = ? ORDER BY data DESC LIMIT " + MAX_LINHAS_LISTAGEM, empresaId);
    }

    // #DP066: 4 SUMs em uma unica query — era 4 roundtrips sequenciais (10-20ms).
    //   Tenant filter em cada subquery permite o planner usar indice (id_viagem, empresa_id).
    public Map<String, Object> balanco(Integer empresaId, Long viagemId) {
        Map<String, Object> row = jdbc.queryForMap("""
            SELECT
              (SELECT COALESCE(SUM(valor_pago), 0) FROM passagens
                 WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)) AS passagens,
              (SELECT COALESCE(SUM(valor_pago), 0) FROM encomendas
                 WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)) AS encomendas,
              (SELECT COALESCE(SUM(valor_pago), 0) FROM fretes
                 WHERE id_viagem = ? AND empresa_id = ? AND (excluido IS NULL OR excluido = FALSE)) AS fretes,
              (SELECT COALESCE(SUM(valor), 0) FROM financeiro_saidas
                 WHERE id_viagem = ? AND excluido = FALSE AND empresa_id = ?) AS despesas
            """, viagemId, empresaId, viagemId, empresaId, viagemId, empresaId, viagemId, empresaId);

        BigDecimal passagens = (BigDecimal) row.get("passagens");
        BigDecimal encomendas = (BigDecimal) row.get("encomendas");
        BigDecimal fretes = (BigDecimal) row.get("fretes");
        BigDecimal despesas = (BigDecimal) row.get("despesas");

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
