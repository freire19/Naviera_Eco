package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.FreteDTO;
import com.naviera.api.model.ClienteApp;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le fretes das tabelas do sistema desktop.
 * Para CNPJ: busca por nome_remetente vinculado ao nome da empresa.
 */
@Service
public class FreteService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public FreteService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
    }

    /**
     * Busca fretes do cliente cross-tenant (por nome em todas as empresas).
     * DS4-002 fix: empresaId nunca vem do request — busca e por identidade do cliente.
     */
    public List<FreteDTO> buscarPorRemetenteCrossTenant(Long clienteId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));

        String sql = """
            SELECT f.id_frete, f.numero_frete, f.remetente_nome_temp as nome_remetente,
                   f.destinatario_nome_temp as nome_destinatario,
                   f.rota_temp as nome_rota, COALESCE(emb.nome, '') as embarcacao,
                   f.valor_frete_calculado as valor_nominal, f.valor_pago, f.valor_devedor,
                   f.status_frete as status, f.status_pagamento, f.forma_pagamento_app, f.desconto_app,
                   f.data_saida_viagem as data_viagem
            FROM fretes f
            LEFT JOIN viagens v ON f.id_viagem = v.id_viagem
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            WHERE UPPER(f.remetente_nome_temp) LIKE UPPER(?)
               OR UPPER(f.destinatario_nome_temp) LIKE UPPER(?)
            ORDER BY f.id_frete DESC
            """;

        String termo = "%" + cliente.getNome() + "%";
        return jdbc.query(sql, (rs, i) -> new FreteDTO(
            rs.getLong("id_frete"),
            rs.getString("numero_frete"),
            rs.getString("nome_remetente"),
            rs.getString("nome_destinatario"),
            rs.getString("nome_rota"),
            rs.getString("embarcacao"),
            rs.getBigDecimal("valor_nominal"),
            rs.getBigDecimal("valor_pago"),
            rs.getBigDecimal("valor_devedor"),
            rs.getString("status"),
            rs.getString("status_pagamento"),
            rs.getString("forma_pagamento_app"),
            rs.getBigDecimal("desconto_app"),
            0,
            null,
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null
        ), termo, termo);
    }

    /**
     * Cliente CNPJ paga um frete vinculado a ele (remetente ou destinatario).
     * Regras:
     *  - PIX aplica 10% desconto; CARTAO, BOLETO e BARCO sem desconto.
     *  - BARCO mantem status_pagamento = PENDENTE (paga presencial na chegada).
     *  - PIX/CARTAO/BOLETO => PENDENTE_CONFIRMACAO ate operador/PSP confirmar.
     *  - Ownership por FK id_cliente_app_pagador quando existir, senao por
     *    match de nome (remetente ou destinatario) como fallback legado.
     */
    @Transactional
    public Map<String, Object> pagar(Long clienteId, Long idFrete, String formaPagamento) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        String forma = formaPagamento != null ? formaPagamento : "PIX";

        var rows = jdbc.queryForList(
            "SELECT id_frete, valor_frete_calculado, desconto, valor_pago, status_pagamento, " +
            "remetente_nome_temp, destinatario_nome_temp, id_cliente_app_pagador, empresa_id " +
            "FROM fretes WHERE id_frete = ?",
            idFrete);
        if (rows.isEmpty()) throw ApiException.notFound("Frete nao encontrado");
        var f = rows.get(0);

        Long donoFk = f.get("id_cliente_app_pagador") != null
            ? ((Number) f.get("id_cliente_app_pagador")).longValue() : null;
        if (donoFk != null) {
            if (!donoFk.equals(clienteId)) throw ApiException.forbidden("Frete nao pertence a este cliente");
        } else {
            String remetente = (String) f.get("remetente_nome_temp");
            String destinatario = (String) f.get("destinatario_nome_temp");
            String nomeUpper = cliente.getNome().toUpperCase();
            boolean match = (remetente != null && remetente.toUpperCase().contains(nomeUpper))
                         || (destinatario != null && destinatario.toUpperCase().contains(nomeUpper));
            if (!match) throw ApiException.forbidden("Frete nao pertence a este cliente");
        }

        String statusAtual = (String) f.get("status_pagamento");
        if ("PAGO".equalsIgnoreCase(statusAtual)) throw ApiException.conflict("Frete ja esta pago");
        if ("PENDENTE_CONFIRMACAO".equalsIgnoreCase(statusAtual))
            throw ApiException.conflict("Pagamento ja enviado, aguardando confirmacao");

        BigDecimal total = (BigDecimal) f.get("valor_frete_calculado");
        if (total == null) total = BigDecimal.ZERO;
        BigDecimal descontoBase = (BigDecimal) f.get("desconto");
        if (descontoBase == null) descontoBase = BigDecimal.ZERO;
        BigDecimal valorPago = (BigDecimal) f.get("valor_pago");
        if (valorPago == null) valorPago = BigDecimal.ZERO;

        BigDecimal saldo = total.subtract(descontoBase).subtract(valorPago).max(BigDecimal.ZERO);
        BigDecimal descontoApp = "PIX".equals(forma)
            ? saldo.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal valorAPagar = saldo.subtract(descontoApp);

        String novoStatus = "BARCO".equals(forma) ? "PENDENTE" : "PENDENTE_CONFIRMACAO";

        jdbc.update(
            "UPDATE fretes SET forma_pagamento_app = ?, desconto_app = ?, " +
            "status_pagamento = ?, tipo_pagamento = ?, " +
            "id_cliente_app_pagador = COALESCE(id_cliente_app_pagador, ?) " +
            "WHERE id_frete = ?",
            forma, descontoApp, novoStatus, forma, clienteId, idFrete);

        Map<String, Object> resp = new HashMap<>();
        resp.put("idFrete", idFrete);
        resp.put("saldoOriginal", saldo);
        resp.put("descontoApp", descontoApp);
        resp.put("valorAPagar", valorAPagar);
        resp.put("formaPagamento", forma);
        resp.put("status", novoStatus);
        String msg;
        if ("BARCO".equals(forma)) msg = "Reservado para pagamento no embarque.";
        else if ("BOLETO".equals(forma)) msg = "Boleto sera enviado. Aguardando confirmacao do pagamento.";
        else msg = "Pagamento enviado. Aguardando confirmacao.";
        resp.put("mensagem", msg);
        return resp;
    }
}
