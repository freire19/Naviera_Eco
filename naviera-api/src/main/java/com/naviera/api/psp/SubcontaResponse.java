package com.naviera.api.psp;

/**
 * Resposta do PSP ao criar uma subconta.
 * Status: PENDENTE_DOCS | EM_ANALISE | APROVADA | REJEITADA.
 */
public record SubcontaResponse(
    String pspProvider,
    String pspSubcontaId,
    String status,
    String onboardingUrl,
    String apiKeySubconta,
    String rawResponseJson
) {}
