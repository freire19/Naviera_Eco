package com.naviera.api.psp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestra criacao de cobranca: chama o PSP + persiste log em psp_cobrancas.
 *
 * Integracao com passagens/encomendas/fretes acontece na Fase 3.3 — nesta fase
 * (3.1) o service ja esta pronto, mas nada chama ele ainda.
 */
@Service
public class PspCobrancaService {

    private static final Logger log = LoggerFactory.getLogger(PspCobrancaService.class);

    // #DR263: whitelist de transicoes — webhooks podem chegar fora de ordem ou repetidos
    //   (PAYMENT_RECEIVED depois de REFUNDED, etc). Sem validacao, status final ficaria
    //   incoerente com a realidade. Estados terminais (ESTORNADA/CANCELADA) sao absorventes.
    private static final Map<String, Set<String>> TRANSICOES_VALIDAS = Map.of(
        "INICIADA",   Set.of("PENDENTE", "CONFIRMADA", "VENCIDA", "CANCELADA", "FALHA"),
        "PENDENTE",   Set.of("CONFIRMADA", "VENCIDA", "CANCELADA"),
        "CONFIRMADA", Set.of("ESTORNADA"),
        "VENCIDA",    Set.of("CONFIRMADA", "CANCELADA"),
        "FALHA",      Set.of("PENDENTE", "CONFIRMADA", "CANCELADA"),
        "ESTORNADA",  Set.of(),
        "CANCELADA",  Set.of()
    );

    private final PspGateway gateway;
    private final PspCobrancaRepository repo;
    private final AsaasProperties props;
    private final JdbcTemplate jdbc;

    public PspCobrancaService(PspGateway gateway, PspCobrancaRepository repo, AsaasProperties props, JdbcTemplate jdbc) {
        this.gateway = gateway;
        this.repo = repo;
        this.props = props;
        this.jdbc = jdbc;
    }

    /**
     * Cria cobranca no PSP e grava log em psp_cobrancas.
     * Usado na Fase 3.3 pelos services de passagem/encomenda/frete.
     *
     * #DS5-003: idempotencia em 3 passos sem transacao global (a chamada HTTP nao entra em tx DB):
     *   1. Dedup — se ja existe cobranca PENDENTE/CONFIRMADA para a origem, retorna ela.
     *   2. Persiste row INICIADA com chave idempotente ANTES do POST ao PSP.
     *   3. Chama PSP; em sucesso, UPDATE com dados reais; em falha, marca FALHA (nao perde referencia).
     */
    public PspCobranca criar(CobrancaRequest req) {
        Optional<PspCobranca> existente = repo.findUltimaPorOrigem(req.tipoOrigem(), req.origemId());
        if (existente.isPresent()) {
            String st = existente.get().getPspStatus();
            if ("PENDENTE".equals(st) || "CONFIRMADA".equals(st)) {
                log.info("[PspCobrancaService] Idempotente: reutilizando cobranca {} para {}:{} (status={})",
                    existente.get().getPspCobrancaId(), req.tipoOrigem(), req.origemId(), st);
                return existente.get();
            }
        }

        PspCobranca inicial = new PspCobranca();
        inicial.setEmpresaId(req.empresaId());
        inicial.setTipoOrigem(req.tipoOrigem());
        inicial.setOrigemId(req.origemId());
        inicial.setPspProvider(gateway.providerName());
        inicial.setPspCobrancaId("IDEM_" + UUID.randomUUID().toString().replace("-", ""));
        inicial.setPspStatus("INICIADA");
        inicial.setFormaPagamento(req.formaPagamento());
        inicial.setValorBruto(req.valorBruto());
        BigDecimal desc = req.descontoAplicado() != null ? req.descontoAplicado() : BigDecimal.ZERO;
        inicial.setDescontoAplicado(desc);
        inicial.setValorLiquido(req.valorBruto().subtract(desc));
        inicial.setClienteAppId(req.clienteAppId());
        PspCobranca row = repo.save(inicial);

        CobrancaResponse resp;
        try {
            resp = gateway.criarCobranca(req);
        } catch (RuntimeException e) {
            row.setPspStatus("FALHA");
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "erro";
            row.setRawResponse("{\"erro\":\"" + msg + "\"}");
            repo.save(row);
            throw e;
        }

        row.setPspProvider(resp.pspProvider());
        row.setPspCobrancaId(resp.pspCobrancaId());
        row.setPspStatus(resp.pspStatus());
        row.setValorLiquido(resp.valorLiquido());
        row.setSplitNavieraPct(req.splitNavieraPct() != null ? req.splitNavieraPct() : props.getSplitNavieraPct());
        row.setSplitNavieraValor(resp.splitNavieraValor());
        row.setSplitEmpresaValor(resp.splitEmpresaValor());
        row.setQrCodePayload(resp.qrCodePayload());
        row.setQrCodeImageUrl(resp.qrCodeImageUrl());
        row.setLinhaDigitavel(resp.linhaDigitavel());
        row.setBoletoUrl(resp.boletoUrl());
        row.setCheckoutUrl(resp.checkoutUrl());
        if (resp.dataVencimento() != null) {
            row.setDataVencimento(LocalDateTime.ofInstant(resp.dataVencimento(), ZoneOffset.UTC));
        }
        row.setRawResponse(resp.rawResponseJson());

        PspCobranca salvo = repo.save(row);
        log.info("[PspCobrancaService] Cobranca {} criada no {} para {}:{} valor={}",
            salvo.getPspCobrancaId(), salvo.getPspProvider(),
            salvo.getTipoOrigem(), salvo.getOrigemId(), salvo.getValorLiquido());
        return salvo;
    }

    /**
     * Atualiza status local + propaga para tabelas de negocio (passagens/fretes/encomendas).
     * #DL033: CONFIRMADA aqui libera embarque/entrega na origem relacionada.
     */
    @Transactional
    public void atualizarStatus(String provider, String pspCobrancaId, String novoStatus) {
        repo.findByPspProviderAndPspCobrancaId(provider, pspCobrancaId).ifPresent(c -> {
            String atual = c.getPspStatus();
            if (atual != null && atual.equals(novoStatus)) {
                return;
            }
            Set<String> permitidos = TRANSICOES_VALIDAS.get(atual);
            if (permitidos == null || !permitidos.contains(novoStatus)) {
                log.warn("[PspCobrancaService] Transicao invalida {}->{} rejeitada (cobranca {})",
                    atual, novoStatus, pspCobrancaId);
                return;
            }
            c.setPspStatus(novoStatus);
            if ("CONFIRMADA".equals(novoStatus) && c.getDataConfirmacao() == null) {
                c.setDataConfirmacao(LocalDateTime.now());
            }
            repo.save(c);
            propagarStatusParaOrigem(c, novoStatus);
            log.info("[PspCobrancaService] Status cobranca {} atualizado para {} (origem {}:{})",
                pspCobrancaId, novoStatus, c.getTipoOrigem(), c.getOrigemId());
        });
    }

    /** Retorna o status aplicado ou null se o evento nao for mapeado. */
    public String processarEvento(String provider, String eventType, String pspCobrancaId) {
        String novoStatus = mapearEvento(eventType);
        if (novoStatus == null) {
            log.info("[PspCobrancaService] Evento {} ignorado (sem mapeamento)", eventType);
            return null;
        }
        atualizarStatus(provider, pspCobrancaId, novoStatus);
        return novoStatus;
    }

    private static String mapearEvento(String eventType) {
        if (eventType == null) return null;
        return switch (eventType) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> "CONFIRMADA";
            case "PAYMENT_OVERDUE" -> "VENCIDA";
            case "PAYMENT_REFUNDED", "PAYMENT_CHARGEBACK_REQUESTED", "PAYMENT_CHARGEBACK_DISPUTE" -> "ESTORNADA";
            case "PAYMENT_DELETED", "PAYMENT_RESTORED" -> "CANCELADA";
            default -> null;
        };
    }

    private void propagarStatusParaOrigem(PspCobranca c, String novoStatus) {
        if (!"CONFIRMADA".equals(novoStatus) && !"ESTORNADA".equals(novoStatus)) return;
        String tipo = c.getTipoOrigem();
        Long origemId = c.getOrigemId();
        if (tipo == null || origemId == null) return;

        if ("CONFIRMADA".equals(novoStatus)) {
            switch (tipo) {
                case "PASSAGEM" -> jdbc.update("""
                    UPDATE passagens
                    SET status_passagem = 'CONFIRMADA',
                        valor_pago = valor_a_pagar,
                        valor_devedor = 0
                    WHERE id_passagem = ? AND status_passagem = 'PENDENTE_CONFIRMACAO'""",
                    origemId);
                case "FRETE" -> jdbc.update("""
                    UPDATE fretes
                    SET status_pagamento = 'PAGO',
                        status_frete = 'PAGO',
                        valor_pago = valor_frete_calculado - COALESCE(desconto, 0),
                        valor_devedor = 0
                    WHERE id_frete = ? AND status_pagamento = 'PENDENTE_CONFIRMACAO'""",
                    origemId);
                case "ENCOMENDA" -> jdbc.update("""
                    UPDATE encomendas
                    SET status_pagamento = 'PAGO',
                        valor_pago = total_a_pagar - COALESCE(desconto, 0) - COALESCE(desconto_app, 0)
                    WHERE id_encomenda = ? AND status_pagamento = 'PENDENTE_CONFIRMACAO'""",
                    origemId);
            }
        } else {
            switch (tipo) {
                case "PASSAGEM" -> jdbc.update(
                    "UPDATE passagens SET status_passagem = 'CANCELADA' WHERE id_passagem = ? AND status_passagem IN ('PENDENTE_CONFIRMACAO','CONFIRMADA')",
                    origemId);
                case "FRETE" -> jdbc.update(
                    "UPDATE fretes SET status_pagamento = 'ESTORNADO' WHERE id_frete = ? AND status_pagamento IN ('PENDENTE_CONFIRMACAO','PAGO')",
                    origemId);
                case "ENCOMENDA" -> jdbc.update(
                    "UPDATE encomendas SET status_pagamento = 'ESTORNADO' WHERE id_encomenda = ? AND status_pagamento IN ('PENDENTE_CONFIRMACAO','PAGO')",
                    origemId);
            }
        }
    }
}
