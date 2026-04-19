package com.naviera.api.psp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
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

    private final PspGateway gateway;
    private final PspCobrancaRepository repo;
    private final AsaasProperties props;

    public PspCobrancaService(PspGateway gateway, PspCobrancaRepository repo, AsaasProperties props) {
        this.gateway = gateway;
        this.repo = repo;
        this.props = props;
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
     * Atualiza status local apos recebimento de webhook do PSP.
     * Usado na Fase 3.4 pelo WebhookController.
     */
    @Transactional
    public void atualizarStatus(String provider, String pspCobrancaId, String novoStatus) {
        repo.findByPspProviderAndPspCobrancaId(provider, pspCobrancaId).ifPresent(c -> {
            c.setPspStatus(novoStatus);
            if ("CONFIRMADA".equals(novoStatus) && c.getDataConfirmacao() == null) {
                c.setDataConfirmacao(LocalDateTime.now());
            }
            repo.save(c);
            log.info("[PspCobrancaService] Status cobranca {} atualizado para {}", pspCobrancaId, novoStatus);
        });
    }
}
