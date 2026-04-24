package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.EncomendaDTO;
import com.naviera.api.model.ClienteApp;
import com.naviera.api.psp.AsaasProperties;
import com.naviera.api.psp.CobrancaRequest;
import com.naviera.api.psp.PspCobranca;
import com.naviera.api.psp.PspCobrancaService;
import com.naviera.api.repository.ClienteAppRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final PspCobrancaService pspService;
    private final AsaasProperties pspProps;
    private final TransactionTemplate tx;

    public EncomendaService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo,
                            PspCobrancaService pspService, AsaasProperties pspProps,
                            TransactionTemplate tx) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
        this.pspService = pspService; this.pspProps = pspProps;
        this.tx = tx;
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
    // #205: chamada HTTP ao PSP nunca dentro de @Transactional.
    public Map<String, Object> pagar(Long clienteId, Long idEncomenda, String formaPagamento) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        String forma = formaPagamento != null ? formaPagamento : "PIX";

        Map<String, Object> resp = tx.execute(status -> {
            var rows = jdbc.queryForList(
                "SELECT id_encomenda, total_a_pagar, desconto, valor_pago, status_pagamento, " +
                "destinatario, id_cliente_app_destinatario, empresa_id, numero_encomenda " +
                "FROM encomendas WHERE id_encomenda = ?",
                idEncomenda);
            if (rows.isEmpty()) throw ApiException.notFound("Encomenda nao encontrada");
            var enc = rows.get(0);

            Long donoFk = enc.get("id_cliente_app_destinatario") != null
                ? ((Number) enc.get("id_cliente_app_destinatario")).longValue() : null;
            if (donoFk != null) {
                if (!donoFk.equals(clienteId)) throw ApiException.forbidden("Encomenda nao pertence a este cliente");
            } else {
                // #DB201/#714: fallback por nome exige match exato (trim+lower) com destinatario.
                if (!com.naviera.api.config.MoneyUtils.nomeCasaComAlgum(
                        cliente.getNome(), (String) enc.get("destinatario"))) {
                    throw ApiException.forbidden("Encomenda nao pertence a este cliente");
                }
            }

            String statusAtual = (String) enc.get("status_pagamento");
            if ("PAGO".equalsIgnoreCase(statusAtual)) throw ApiException.conflict("Encomenda ja esta paga");
            if ("PENDENTE_CONFIRMACAO".equalsIgnoreCase(statusAtual))
                throw ApiException.conflict("Pagamento ja enviado, aguardando confirmacao");

            BigDecimal total = com.naviera.api.config.MoneyUtils.toBigDecimal(enc.get("total_a_pagar"));
            BigDecimal descontoBase = com.naviera.api.config.MoneyUtils.toBigDecimal(enc.get("desconto"));
            BigDecimal valorPago = com.naviera.api.config.MoneyUtils.toBigDecimal(enc.get("valor_pago"));

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

            String subcontaId = null;
            if (!"BARCO".equals(forma)) {
                Integer empresaId = ((Number) enc.get("empresa_id")).intValue();
                subcontaId = (String) jdbc.queryForMap(
                    "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId).get("psp_subconta_id");
                if (subcontaId == null || subcontaId.isBlank()) {
                    throw ApiException.badRequest(
                        "Empresa nao possui subconta Asaas. Pagamento online indisponivel — use 'Pagar no barco'.");
                }
            }

            Map<String, Object> r = new HashMap<>();
            r.put("idEncomenda", idEncomenda);
            r.put("empresaId", enc.get("empresa_id"));
            r.put("subcontaId", subcontaId);
            r.put("numeroEncomenda", enc.get("numero_encomenda"));
            r.put("saldoOriginal", saldo);
            r.put("descontoApp", descontoApp);
            r.put("valorAPagar", valorAPagar);
            r.put("formaPagamento", forma);
            r.put("status", novoStatus);
            return r;
        });

        if ("BARCO".equals(forma)) {
            resp.remove("empresaId");
            resp.remove("subcontaId");
            resp.remove("numeroEncomenda");
            resp.put("mensagem", "Reservado para pagamento no embarque.");
            return resp;
        }

        Integer empresaId = ((Number) resp.remove("empresaId")).intValue();
        String subcontaId = (String) resp.remove("subcontaId");
        String numEncomenda = (String) resp.remove("numeroEncomenda");
        BigDecimal valorAPagar = (BigDecimal) resp.get("valorAPagar");

        CobrancaRequest pspReq = new CobrancaRequest(
            empresaId, subcontaId, "ENCOMENDA", idEncomenda, clienteId, forma,
            valorAPagar, BigDecimal.ZERO, pspProps.getSplitNavieraPct(),
            "Encomenda " + (numEncomenda != null ? numEncomenda : idEncomenda),
            LocalDate.now(com.naviera.api.config.MoneyUtils.ZONE_BR).plusDays(3),
            cliente.getDocumento(), cliente.getNome(), cliente.getEmail()
        );
        PspCobranca cob = pspService.criar(pspReq);

        tx.executeWithoutResult(s -> jdbc.update(
            "UPDATE encomendas SET id_transacao_psp = ?, qr_pix_payload = ? WHERE id_encomenda = ?",
            cob.getPspCobrancaId(), cob.getQrCodePayload(), idEncomenda));

        resp.put("pspCobrancaId", cob.getPspCobrancaId());
        resp.put("qrCodePayload", cob.getQrCodePayload());
        resp.put("qrCodeImageUrl", cob.getQrCodeImageUrl());
        resp.put("boletoUrl", cob.getBoletoUrl());
        resp.put("linhaDigitavel", cob.getLinhaDigitavel());
        resp.put("checkoutUrl", cob.getCheckoutUrl());
        resp.put("mensagem", "PIX".equals(forma)
            ? "Escaneie o QR Code ou copie o codigo para pagar."
            : "Conclua o pagamento no checkout.");
        return resp;
    }
}
