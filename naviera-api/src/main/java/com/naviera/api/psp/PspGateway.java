package com.naviera.api.psp;

/**
 * Abstracao do PSP (Asaas, Mercado Pago, Stripe, etc).
 *
 * Implementacoes:
 *   - {@link AsaasGateway} (Asaas sandbox/producao)
 *
 * Troca de PSP futura: basta escrever outra implementacao e configurar
 * application.properties {@code naviera.psp.provider=<nome>}.
 */
public interface PspGateway {

    /** Nome do provider para logar em psp_cobrancas.psp_provider. */
    String providerName();

    /**
     * Cria cobranca PIX/Cartao/Boleto com split automatico.
     * O PSP fica responsavel por direcionar o valor liquido (menos split Naviera)
     * para a subconta da empresa.
     */
    CobrancaResponse criarCobranca(CobrancaRequest req);

    /**
     * Cria subconta marketplace pra uma empresa (onboarding).
     * Geralmente devolve {@code onboardingUrl} onde a empresa completa KYC.
     */
    SubcontaResponse criarSubconta(SubcontaRequest req);

    /**
     * Valida a assinatura do webhook recebido.
     * Usado em {@code /api/webhooks/psp} antes de processar o payload.
     */
    boolean validarAssinaturaWebhook(String payload, String assinatura);
}
