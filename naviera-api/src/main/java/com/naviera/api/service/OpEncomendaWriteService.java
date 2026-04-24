package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import static com.naviera.api.config.MoneyUtils.toBigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OpEncomendaWriteService {
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public OpEncomendaWriteService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> criar(Integer empresaId, Map<String, Object> dados) {
        String numEncomenda;
        try {
            numEncomenda = jdbc.queryForObject("SELECT nextval('seq_numero_encomenda')", String.class);
        } catch (Exception e) {
            // Fallback: advisory lock + MAX+1 filtrado por empresa_id
            jdbc.query("SELECT pg_advisory_xact_lock(?)", rs -> null, empresaId);
            numEncomenda = jdbc.queryForObject(
                "SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) + 1 FROM encomendas WHERE empresa_id = ?",
                String.class, empresaId);
        }

        BigDecimal totalAPagar = toBigDecimal(dados.get("total_a_pagar"));
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));
        // #228: guards de valores
        if (totalAPagar == null || totalAPagar.signum() < 0) {
            throw ApiException.badRequest("total_a_pagar invalido");
        }
        if (valorPago == null) valorPago = BigDecimal.ZERO;
        if (valorPago.signum() < 0) throw ApiException.badRequest("valor_pago nao pode ser negativo");
        if (valorPago.compareTo(totalAPagar) > 0) {
            throw ApiException.badRequest("valor_pago nao pode exceder total_a_pagar");
        }
        Object totVol = dados.get("total_volumes");
        if (totVol != null) {
            int tv = ((Number) totVol).intValue();
            if (tv < 0) throw ApiException.badRequest("total_volumes nao pode ser negativo");
        }

        jdbc.update("""
            INSERT INTO encomendas (numero_encomenda, id_viagem, remetente, destinatario, observacoes,
                total_volumes, total_a_pagar, valor_pago, desconto, status_pagamento, forma_pagamento,
                rota, id_caixa, data_lancamento, entregue, empresa_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, FALSE, ?)""",
            numEncomenda, dados.get("id_viagem"), dados.get("remetente"), dados.get("destinatario"),
            dados.get("observacoes"), dados.get("total_volumes"), totalAPagar, valorPago,
            toBigDecimal(dados.get("desconto")),
            valorPago.compareTo(totalAPagar) >= 0 ? "PAGO" : "PENDENTE",
            dados.get("forma_pagamento"), dados.get("rota"), dados.get("id_caixa"), empresaId);

        // Buscar id gerado via currval (mesma transacao, seguro)
        Long idEncomenda;
        try {
            idEncomenda = jdbc.queryForObject("SELECT currval('encomendas_id_encomenda_seq')", Long.class);
        } catch (Exception e) {
            idEncomenda = jdbc.queryForObject(
                "SELECT id_encomenda FROM encomendas WHERE numero_encomenda = ? AND empresa_id = ? ORDER BY id_encomenda DESC LIMIT 1",
                Long.class, numEncomenda, empresaId);
        }

        // Inserir itens se presentes
        List<Map<String, Object>> itens = (List<Map<String, Object>>) dados.get("itens");
        if (itens != null) {
            for (Map<String, Object> item : itens) {
                jdbc.update("""
                    INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total)
                    VALUES (?, ?, ?, ?, ?)""",
                    idEncomenda, item.get("quantidade"), item.get("descricao"),
                    toBigDecimal(item.get("valor_unitario")), toBigDecimal(item.get("valor_total")));
            }
        }

        notificationService.encomendaCriada(empresaId, idEncomenda, numEncomenda);
        return Map.of("mensagem", "Encomenda criada", "id_encomenda", idEncomenda, "numero_encomenda", numEncomenda);
    }

    @Transactional
    public Map<String, Object> atualizar(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE encomendas SET remetente = ?, destinatario = ?, observacoes = ?, rota = ?, total_volumes = ?
            WHERE id_encomenda = ? AND empresa_id = ?""",
            dados.get("remetente"), dados.get("destinatario"), dados.get("observacoes"),
            dados.get("rota"), dados.get("total_volumes"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Encomenda nao encontrada");
        return Map.of("mensagem", "Encomenda atualizada");
    }

    @Transactional
    public Map<String, Object> excluir(Integer empresaId, Long id) {
        int rows = jdbc.update(
            "UPDATE encomendas SET excluido = TRUE WHERE id_encomenda = ? AND empresa_id = ?", id, empresaId);
        if (rows == 0) throw ApiException.notFound("Encomenda nao encontrada");
        return Map.of("mensagem", "Encomenda excluida");
    }

    @Transactional
    public Map<String, Object> pagar(Integer empresaId, Long id, Map<String, Object> dados) {
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));
        int rows = jdbc.update("""
            UPDATE encomendas SET valor_pago = valor_pago + ?,
                status_pagamento = CASE WHEN valor_pago + ? >= total_a_pagar - desconto THEN 'PAGO' ELSE 'PARCIAL' END
            WHERE id_encomenda = ? AND empresa_id = ?""",
            valorPago, valorPago, id, empresaId);
        if (rows == 0) throw ApiException.notFound("Encomenda nao encontrada");
        return Map.of("mensagem", "Pagamento registrado");
    }

    @Transactional
    public Map<String, Object> entregar(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE encomendas SET entregue = TRUE, doc_recebedor = ?, nome_recebedor = ?
            WHERE id_encomenda = ? AND empresa_id = ?""",
            dados.get("doc_recebedor"), dados.get("nome_recebedor"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Encomenda nao encontrada");
        notificationService.encomendaEntregue(empresaId, id);
        return Map.of("mensagem", "Encomenda entregue");
    }

}
