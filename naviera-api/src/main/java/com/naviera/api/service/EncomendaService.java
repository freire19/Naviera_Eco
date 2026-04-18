package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.EncomendaDTO;
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
 * Lê encomendas diretamente das tabelas do sistema desktop.
 * Busca por nome do destinatário vinculado ao CPF do cliente logado.
 * Quando o campo cpf_destinatario existir no banco, usará esse campo.
 */
@Service
public class EncomendaService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;

    public EncomendaService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
    }

    // #DB144: empresaId parameter prevents cross-tenant LIKE scan
    public List<EncomendaDTO> buscarPorCliente(Long clienteId, Integer empresaId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String sql = """
            SELECT e.id_encomenda as id, e.numero_encomenda, e.remetente, e.destinatario, e.rota,
                   COALESCE(emb.nome, '') as embarcacao,
                   e.total_a_pagar, e.valor_pago, e.desconto,
                   e.status_pagamento, e.entregue, e.total_volumes,
                   v.data_viagem, v.data_chegada
            FROM encomendas e
            LEFT JOIN viagens v ON e.id_viagem = v.id_viagem
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            WHERE UPPER(e.destinatario) LIKE UPPER(?) AND e.empresa_id = ?
            ORDER BY e.id_encomenda DESC
            """;

        return jdbc.query(sql, (rs, i) -> new EncomendaDTO(
            rs.getLong("id"),
            rs.getString("numero_encomenda"),
            rs.getString("remetente"),
            rs.getString("destinatario"),
            rs.getString("rota"),
            rs.getString("embarcacao"),
            rs.getBigDecimal("total_a_pagar"),
            rs.getBigDecimal("valor_pago"),
            rs.getBigDecimal("total_a_pagar")
                .subtract(rs.getBigDecimal("desconto") != null ? rs.getBigDecimal("desconto") : java.math.BigDecimal.ZERO)
                .subtract(rs.getBigDecimal("valor_pago") != null ? rs.getBigDecimal("valor_pago") : java.math.BigDecimal.ZERO)
                .max(java.math.BigDecimal.ZERO),
            rs.getString("status_pagamento"),
            rs.getBoolean("entregue"),
            rs.getInt("total_volumes"),
            rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toString() : null,
            rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toString() : null
        ), "%" + cliente.getNome() + "%", empresaId);
    }

    /**
     * Rastreio cross-tenant — busca encomendas do cliente (por nome e documento)
     * em todas as empresas. Retorna Maps para incluir empresa_nome.
     */
    public List<Map<String, Object>> rastreioCrossTenant(Long clienteId) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        String sql = """
            SELECT e.id_encomenda, e.numero_encomenda, e.remetente, e.destinatario, e.rota,
                   COALESCE(emb.nome, '') AS embarcacao,
                   e.total_a_pagar, e.valor_pago, e.desconto,
                   e.status_pagamento, e.entregue, e.total_volumes,
                   v.data_viagem, v.data_chegada,
                   COALESCE(emp.nome, '') AS empresa_nome
            FROM encomendas e
            LEFT JOIN viagens v ON e.id_viagem = v.id_viagem
            LEFT JOIN embarcacoes emb ON v.id_embarcacao = emb.id_embarcacao
            LEFT JOIN empresas emp ON emb.empresa_id = emp.id
            WHERE UPPER(e.destinatario) LIKE UPPER(?)
               OR UPPER(e.remetente) LIKE UPPER(?)
            ORDER BY e.id_encomenda DESC
            """;

        String termo = "%" + cliente.getNome() + "%";
        return jdbc.queryForList(sql, termo, termo);
    }

    /**
     * Cliente CPF paga uma encomenda destinada a ele.
     * Regras:
     *  - PIX aplica 10% desconto; CARTAO e BARCO sem desconto.
     *  - BARCO mantem status PENDENTE (paga presencial na chegada).
     *  - PIX/CARTAO vao para PENDENTE_CONFIRMACAO ate operador/PSP confirmar.
     * Valida que o cliente logado e destinatario (por id_cliente_app_destinatario
     * quando existir, ou por match de nome como fallback legado).
     */
    @Transactional
    public Map<String, Object> pagar(Long clienteId, Long idEncomenda, String formaPagamento) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        String forma = formaPagamento != null ? formaPagamento : "PIX";

        var rows = jdbc.queryForList(
            "SELECT id_encomenda, total_a_pagar, desconto, valor_pago, status_pagamento, " +
            "destinatario, id_cliente_app_destinatario, empresa_id " +
            "FROM encomendas WHERE id_encomenda = ?",
            idEncomenda);
        if (rows.isEmpty()) throw ApiException.notFound("Encomenda nao encontrada");
        var enc = rows.get(0);

        Long donoFk = enc.get("id_cliente_app_destinatario") != null
            ? ((Number) enc.get("id_cliente_app_destinatario")).longValue() : null;
        if (donoFk != null) {
            if (!donoFk.equals(clienteId)) throw ApiException.forbidden("Encomenda nao pertence a este cliente");
        } else {
            String destinatario = (String) enc.get("destinatario");
            if (destinatario == null || !destinatario.toUpperCase().contains(cliente.getNome().toUpperCase())) {
                throw ApiException.forbidden("Encomenda nao pertence a este cliente");
            }
        }

        String statusAtual = (String) enc.get("status_pagamento");
        if ("PAGO".equalsIgnoreCase(statusAtual)) throw ApiException.conflict("Encomenda ja esta paga");
        if ("PENDENTE_CONFIRMACAO".equalsIgnoreCase(statusAtual))
            throw ApiException.conflict("Pagamento ja enviado, aguardando confirmacao");

        BigDecimal total = (BigDecimal) enc.get("total_a_pagar");
        if (total == null) total = BigDecimal.ZERO;
        BigDecimal descontoBase = (BigDecimal) enc.get("desconto");
        if (descontoBase == null) descontoBase = BigDecimal.ZERO;
        BigDecimal valorPago = (BigDecimal) enc.get("valor_pago");
        if (valorPago == null) valorPago = BigDecimal.ZERO;

        BigDecimal saldo = total.subtract(descontoBase).subtract(valorPago).max(BigDecimal.ZERO);
        BigDecimal descontoApp = "PIX".equals(forma)
            ? saldo.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal valorAPagar = saldo.subtract(descontoApp);

        String novoStatus = "BARCO".equals(forma) ? "PENDENTE" : "PENDENTE_CONFIRMACAO";

        jdbc.update(
            "UPDATE encomendas SET forma_pagamento_app = ?, desconto_app = ?, " +
            "status_pagamento = ?, forma_pagamento = ?, id_cliente_app_destinatario = COALESCE(id_cliente_app_destinatario, ?) " +
            "WHERE id_encomenda = ?",
            forma, descontoApp, novoStatus, forma, clienteId, idEncomenda);

        Map<String, Object> resp = new HashMap<>();
        resp.put("idEncomenda", idEncomenda);
        resp.put("saldoOriginal", saldo);
        resp.put("descontoApp", descontoApp);
        resp.put("valorAPagar", valorAPagar);
        resp.put("formaPagamento", forma);
        resp.put("status", novoStatus);
        resp.put("mensagem", "BARCO".equals(forma)
            ? "Reservado para pagamento no embarque."
            : "Pagamento enviado. Aguardando confirmacao.");
        return resp;
    }
}
