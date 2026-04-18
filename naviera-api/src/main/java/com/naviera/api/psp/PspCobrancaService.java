package com.naviera.api.psp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
     */
    @Transactional
    public PspCobranca criar(CobrancaRequest req) {
        CobrancaResponse resp = gateway.criarCobranca(req);

        PspCobranca c = new PspCobranca();
        c.setEmpresaId(req.empresaId());
        c.setTipoOrigem(req.tipoOrigem());
        c.setOrigemId(req.origemId());
        c.setPspProvider(resp.pspProvider());
        c.setPspCobrancaId(resp.pspCobrancaId());
        c.setPspStatus(resp.pspStatus());
        c.setFormaPagamento(req.formaPagamento());
        c.setValorBruto(req.valorBruto());
        c.setDescontoAplicado(req.descontoAplicado());
        c.setValorLiquido(resp.valorLiquido());
        c.setSplitNavieraPct(req.splitNavieraPct() != null ? req.splitNavieraPct() : props.getSplitNavieraPct());
        c.setSplitNavieraValor(resp.splitNavieraValor());
        c.setSplitEmpresaValor(resp.splitEmpresaValor());
        c.setQrCodePayload(resp.qrCodePayload());
        c.setQrCodeImageUrl(resp.qrCodeImageUrl());
        c.setLinhaDigitavel(resp.linhaDigitavel());
        c.setBoletoUrl(resp.boletoUrl());
        c.setCheckoutUrl(resp.checkoutUrl());
        c.setClienteAppId(req.clienteAppId());
        if (resp.dataVencimento() != null) {
            c.setDataVencimento(LocalDateTime.ofInstant(resp.dataVencimento(), ZoneOffset.UTC));
        }
        c.setRawResponse(resp.rawResponseJson());

        PspCobranca salvo = repo.save(c);
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
