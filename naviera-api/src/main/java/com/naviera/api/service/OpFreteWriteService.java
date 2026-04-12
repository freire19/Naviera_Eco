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
public class OpFreteWriteService {
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public OpFreteWriteService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> criar(Integer empresaId, Map<String, Object> dados) {
        Map<String, Object> seqs = jdbc.queryForMap(
            "SELECT COALESCE(MAX(numero_frete), 0) + 1 AS next_num, COALESCE(MAX(id_frete), 0) + 1 AS next_id FROM fretes WHERE empresa_id = ?", empresaId);
        Long numFrete = ((Number) seqs.get("next_num")).longValue();
        Long idFrete = ((Number) seqs.get("next_id")).longValue();

        BigDecimal valorTotal = toBigDecimal(dados.get("valor_total_itens"));
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));
        BigDecimal desconto = toBigDecimal(dados.get("desconto"));

        jdbc.update("""
            INSERT INTO fretes (id_frete, numero_frete, data_emissao, id_viagem, remetente_nome_temp,
                destinatario_nome_temp, rota_temp, conferente_temp, observacoes,
                valor_total_itens, desconto, valor_frete_calculado, valor_pago, valor_devedor,
                tipo_pagamento, nome_caixa, empresa_id)
            VALUES (?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            idFrete, numFrete, dados.get("id_viagem"),
            dados.get("remetente_nome_temp"), dados.get("destinatario_nome_temp"),
            dados.get("rota_temp"), dados.get("conferente_temp"), dados.get("observacoes"),
            valorTotal, desconto, valorTotal.subtract(desconto), valorPago,
            valorTotal.subtract(desconto).subtract(valorPago),
            dados.get("tipo_pagamento"), dados.get("nome_caixa"), empresaId);

        // Inserir itens se presentes
        List<Map<String, Object>> itens = (List<Map<String, Object>>) dados.get("itens");
        if (itens != null) {
            for (Map<String, Object> item : itens) {
                jdbc.update("""
                    INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade,
                        valor_unitario, valor_total, observacao)
                    VALUES (?, ?, ?, ?, ?, ?)""",
                    idFrete, item.get("nome_item"), item.get("quantidade"),
                    toBigDecimal(item.get("valor_unitario")), toBigDecimal(item.get("valor_total")),
                    item.get("observacao"));
            }
        }

        notificationService.freteCriado(empresaId, idFrete, numFrete);
        return Map.of("mensagem", "Frete criado", "id_frete", idFrete, "numero_frete", numFrete);
    }

    @Transactional
    public Map<String, Object> atualizar(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE fretes SET remetente_nome_temp = ?, destinatario_nome_temp = ?,
                rota_temp = ?, conferente_temp = ?, observacoes = ?
            WHERE id_frete = ? AND empresa_id = ?""",
            dados.get("remetente_nome_temp"), dados.get("destinatario_nome_temp"),
            dados.get("rota_temp"), dados.get("conferente_temp"), dados.get("observacoes"),
            id, empresaId);
        if (rows == 0) throw ApiException.notFound("Frete nao encontrado");
        return Map.of("mensagem", "Frete atualizado");
    }

    @Transactional
    public Map<String, Object> excluir(Integer empresaId, Long id) {
        int rows = jdbc.update(
            "UPDATE fretes SET excluido = TRUE WHERE id_frete = ? AND empresa_id = ?", id, empresaId);
        if (rows == 0) throw ApiException.notFound("Frete nao encontrado");
        return Map.of("mensagem", "Frete excluido");
    }

    @Transactional
    public Map<String, Object> pagar(Integer empresaId, Long id, Map<String, Object> dados) {
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));
        int rows = jdbc.update("""
            UPDATE fretes SET valor_pago = valor_pago + ?, valor_devedor = valor_devedor - ?
            WHERE id_frete = ? AND empresa_id = ?""",
            valorPago, valorPago, id, empresaId);
        if (rows == 0) throw ApiException.notFound("Frete nao encontrado");
        return Map.of("mensagem", "Pagamento registrado");
    }

}
