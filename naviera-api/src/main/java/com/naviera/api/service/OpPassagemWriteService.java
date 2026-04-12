package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import static com.naviera.api.config.MoneyUtils.toBigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class OpPassagemWriteService {
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public OpPassagemWriteService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> criar(Integer empresaId, Map<String, Object> dados) {
        // Gerar numero do bilhete
        String numBilhete = jdbc.queryForObject(
            "SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) + 1 FROM passagens WHERE empresa_id = ?",
            String.class, empresaId);

        Long idPassageiro = ((Number) dados.get("id_passageiro")).longValue();
        Long idViagem = ((Number) dados.get("id_viagem")).longValue();
        Integer idRota = dados.get("id_rota") != null ? ((Number) dados.get("id_rota")).intValue() : null;
        Integer idTipoPassagem = dados.get("id_tipo_passagem") != null ? ((Number) dados.get("id_tipo_passagem")).intValue() : null;
        Integer idAcomodacao = dados.get("id_acomodacao") != null ? ((Number) dados.get("id_acomodacao")).intValue() : null;
        Integer idFormaPagamento = dados.get("id_forma_pagamento") != null ? ((Number) dados.get("id_forma_pagamento")).intValue() : null;
        Integer idCaixa = dados.get("id_caixa") != null ? ((Number) dados.get("id_caixa")).intValue() : null;
        String assento = (String) dados.get("assento");
        BigDecimal valorTotal = toBigDecimal(dados.get("valor_total"));
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));
        BigDecimal valorDevedor = valorTotal.subtract(valorPago);

        jdbc.update("""
            INSERT INTO passagens (numero_bilhete, id_passageiro, id_viagem, data_emissao, assento,
                id_acomodacao, id_rota, id_tipo_passagem, valor_total, valor_a_pagar, valor_pago,
                valor_devedor, id_forma_pagamento, id_caixa, status_passagem, observacoes, empresa_id)
            VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            numBilhete, idPassageiro, idViagem, assento,
            idAcomodacao, idRota, idTipoPassagem, valorTotal, valorTotal, valorPago,
            valorDevedor, idFormaPagamento, idCaixa,
            valorDevedor.compareTo(BigDecimal.ZERO) <= 0 ? "PAGO" : "PENDENTE",
            dados.get("observacoes"), empresaId);

        notificationService.passagemCriada(empresaId, numBilhete, numBilhete);
        return Map.of("mensagem", "Passagem criada", "numero_bilhete", numBilhete);
    }

    @Transactional
    public Map<String, Object> atualizar(Integer empresaId, Long id, Map<String, Object> dados) {
        int rows = jdbc.update("""
            UPDATE passagens SET assento = ?, observacoes = ?, id_acomodacao = ?, id_rota = ?
            WHERE id_passagem = ? AND empresa_id = ?""",
            dados.get("assento"), dados.get("observacoes"),
            dados.get("id_acomodacao"), dados.get("id_rota"), id, empresaId);
        if (rows == 0) throw ApiException.notFound("Passagem nao encontrada");
        notificationService.passagemAtualizada(empresaId, id);
        return Map.of("mensagem", "Passagem atualizada");
    }

    @Transactional
    public Map<String, Object> excluir(Integer empresaId, Long id) {
        int rows = jdbc.update(
            "UPDATE passagens SET excluido = TRUE WHERE id_passagem = ? AND empresa_id = ?", id, empresaId);
        if (rows == 0) throw ApiException.notFound("Passagem nao encontrada");
        return Map.of("mensagem", "Passagem excluida");
    }

    @Transactional
    public Map<String, Object> pagar(Integer empresaId, Long id, Map<String, Object> dados) {
        BigDecimal valorPago = toBigDecimal(dados.get("valor_pago"));

        int rows = jdbc.update("""
            UPDATE passagens SET valor_pago = valor_pago + ?, valor_devedor = valor_devedor - ?,
                status_passagem = CASE WHEN valor_devedor - ? <= 0.01 THEN 'PAGO' ELSE 'PARCIAL' END
            WHERE id_passagem = ? AND empresa_id = ?""",
            valorPago, valorPago, valorPago, id, empresaId);
        if (rows == 0) throw ApiException.notFound("Passagem nao encontrada");
        return Map.of("mensagem", "Pagamento registrado");
    }

}
