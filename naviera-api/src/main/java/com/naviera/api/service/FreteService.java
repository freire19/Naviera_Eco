package com.naviera.api.service;

import com.naviera.api.config.ApiException;
import com.naviera.api.dto.FreteDTO;
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
 * Le fretes das tabelas do sistema desktop.
 * Para CNPJ: busca por nome_remetente vinculado ao nome da empresa.
 */
@Service
public class FreteService {
    private final JdbcTemplate jdbc;
    private final ClienteAppRepository clienteRepo;
    private final PspCobrancaService pspService;
    private final AsaasProperties pspProps;
    private final TransactionTemplate tx;

    public FreteService(JdbcTemplate jdbc, ClienteAppRepository clienteRepo,
                        PspCobrancaService pspService, AsaasProperties pspProps,
                        TransactionTemplate tx) {
        this.jdbc = jdbc; this.clienteRepo = clienteRepo;
        this.pspService = pspService; this.pspProps = pspProps;
        this.tx = tx;
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
    // #205: chamada HTTP ao PSP nunca dentro de @Transactional.
    public Map<String, Object> pagar(Long clienteId, Long idFrete, String formaPagamento) {
        ClienteApp cliente = clienteRepo.findById(clienteId)
            .orElseThrow(() -> ApiException.notFound("Cliente nao encontrado"));

        String forma = formaPagamento != null ? formaPagamento : "PIX";

        Map<String, Object> resp = tx.execute(status -> {
            var rows = jdbc.queryForList(
                "SELECT id_frete, valor_frete_calculado, desconto, valor_pago, status_pagamento, " +
                "remetente_nome_temp, destinatario_nome_temp, id_cliente_app_pagador, empresa_id, numero_frete " +
                "FROM fretes WHERE id_frete = ?",
                idFrete);
            if (rows.isEmpty()) throw ApiException.notFound("Frete nao encontrado");
            var f = rows.get(0);

            Long donoFk = f.get("id_cliente_app_pagador") != null
                ? ((Number) f.get("id_cliente_app_pagador")).longValue() : null;
            if (donoFk != null) {
                if (!donoFk.equals(clienteId)) throw ApiException.forbidden("Frete nao pertence a este cliente");
            } else {
                // #DB201/#714: fallback por nome exige match exato (trim+lower) em remetente ou destinatario.
                if (!com.naviera.api.config.MoneyUtils.nomeCasaComAlgum(
                        cliente.getNome(),
                        (String) f.get("remetente_nome_temp"),
                        (String) f.get("destinatario_nome_temp"))) {
                    throw ApiException.forbidden("Frete nao pertence a este cliente");
                }
            }

            String statusAtual = (String) f.get("status_pagamento");
            if ("PAGO".equalsIgnoreCase(statusAtual)) throw ApiException.conflict("Frete ja esta pago");
            if ("PENDENTE_CONFIRMACAO".equalsIgnoreCase(statusAtual))
                throw ApiException.conflict("Pagamento ja enviado, aguardando confirmacao");

            BigDecimal total = com.naviera.api.config.MoneyUtils.toBigDecimal(f.get("valor_frete_calculado"));
            BigDecimal descontoBase = com.naviera.api.config.MoneyUtils.toBigDecimal(f.get("desconto"));
            BigDecimal valorPago = com.naviera.api.config.MoneyUtils.toBigDecimal(f.get("valor_pago"));

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

            String subcontaId = null;
            if (!"BARCO".equals(forma)) {
                Integer empresaId = ((Number) f.get("empresa_id")).intValue();
                subcontaId = (String) jdbc.queryForMap(
                    "SELECT psp_subconta_id FROM empresas WHERE id = ?", empresaId).get("psp_subconta_id");
                if (subcontaId == null || subcontaId.isBlank()) {
                    throw ApiException.badRequest(
                        "Empresa nao possui subconta Asaas. Pagamento online indisponivel — use 'Pagar no barco'.");
                }
            }

            Map<String, Object> r = new HashMap<>();
            r.put("idFrete", idFrete);
            r.put("empresaId", f.get("empresa_id"));
            r.put("subcontaId", subcontaId);
            r.put("numeroFrete", f.get("numero_frete"));
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
            resp.remove("numeroFrete");
            resp.put("mensagem", "Reservado para pagamento no embarque.");
            return resp;
        }

        Integer empresaId = ((Number) resp.remove("empresaId")).intValue();
        String subcontaId = (String) resp.remove("subcontaId");
        String numeroFrete = (String) resp.remove("numeroFrete");
        BigDecimal valorAPagar = (BigDecimal) resp.get("valorAPagar");

        LocalDate venc = "BOLETO".equals(forma) ? LocalDate.now().plusDays(3) : LocalDate.now().plusDays(1);
        CobrancaRequest pspReq = new CobrancaRequest(
            empresaId, subcontaId, "FRETE", idFrete, clienteId, forma,
            valorAPagar, BigDecimal.ZERO, pspProps.getSplitNavieraPct(),
            "Frete " + (numeroFrete != null ? numeroFrete : idFrete),
            venc, cliente.getDocumento(), cliente.getNome(), cliente.getEmail()
        );
        PspCobranca cob = pspService.criar(pspReq);

        tx.executeWithoutResult(s -> jdbc.update(
            "UPDATE fretes SET id_transacao_psp = ?, qr_pix_payload = ?, boleto_url = ?, boleto_linha_digitavel = ? WHERE id_frete = ?",
            cob.getPspCobrancaId(), cob.getQrCodePayload(), cob.getBoletoUrl(), cob.getLinhaDigitavel(), idFrete));

        resp.put("pspCobrancaId", cob.getPspCobrancaId());
        resp.put("qrCodePayload", cob.getQrCodePayload());
        resp.put("qrCodeImageUrl", cob.getQrCodeImageUrl());
        resp.put("boletoUrl", cob.getBoletoUrl());
        resp.put("linhaDigitavel", cob.getLinhaDigitavel());
        resp.put("checkoutUrl", cob.getCheckoutUrl());
        String msg;
        if ("PIX".equals(forma)) msg = "Escaneie o QR Code ou copie o codigo para pagar.";
        else if ("BOLETO".equals(forma)) msg = "Boleto gerado. Pague no seu banco pela linha digitavel ou pelo link.";
        else msg = "Conclua o pagamento no checkout.";
        resp.put("mensagem", msg);
        return resp;
    }
}
