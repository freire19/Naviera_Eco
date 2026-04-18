package com.naviera.api.psp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resposta do PSP apos criar uma cobranca.
 * Contem artefatos que o app mostra ao cliente (QR PIX, linha digitavel, checkout URL).
 */
public record CobrancaResponse(
    String pspProvider,
    String pspCobrancaId,
    String pspStatus,
    BigDecimal valorLiquido,
    BigDecimal splitNavieraValor,
    BigDecimal splitEmpresaValor,
    String qrCodePayload,
    String qrCodeImageUrl,
    String linhaDigitavel,
    String boletoUrl,
    String checkoutUrl,
    Instant dataVencimento,
    String rawResponseJson
) {}
