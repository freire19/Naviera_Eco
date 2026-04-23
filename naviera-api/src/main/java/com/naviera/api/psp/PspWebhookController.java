package com.naviera.api.psp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe webhooks do Asaas e propaga confirmacao/estorno para as origens (passagem/frete/encomenda).
 *
 * URL publica — configurar no painel Asaas apontando para https://<host>/webhooks/psp/asaas.
 * Assinatura validada via HMAC SHA256 (header asaas-access-token, hex lowercase do body bruto).
 *
 * Contrato minimo esperado:
 *   { "event": "PAYMENT_RECEIVED|PAYMENT_CONFIRMED|...", "id": "evt_xxx", "payment": { "id": "pay_xxx", ... } }
 *
 * Idempotencia por (provider, event_id) em psp_webhook_events — retries do Asaas sao silenciosos.
 */
@RestController
@RequestMapping("/webhooks/psp")
public class PspWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PspWebhookController.class);
    private static final String PROVIDER = "asaas";

    private final PspGateway gateway;
    private final PspCobrancaService cobrancaService;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PspWebhookController(PspGateway gateway, PspCobrancaService cobrancaService, JdbcTemplate jdbc, ObjectMapper mapper) {
        this.gateway = gateway;
        this.cobrancaService = cobrancaService;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @PostMapping("/asaas")
    public ResponseEntity<?> asaas(
            @RequestBody String rawBody,
            @RequestHeader(value = "asaas-access-token", required = false) String assinatura) {

        if (!gateway.validarAssinaturaWebhook(rawBody, assinatura)) {
            log.warn("[PspWebhook] Asaas — assinatura invalida");
            return ResponseEntity.status(401).body("invalid signature");
        }

        JsonNode root;
        try {
            root = mapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("[PspWebhook] Asaas — body invalido: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid body");
        }

        String eventType = root.path("event").asText(null);
        String eventId = root.path("id").asText(null);
        String pspCobrancaId = root.path("payment").path("id").asText(null);

        if (eventType == null || eventId == null || pspCobrancaId == null) {
            log.warn("[PspWebhook] Asaas — campos obrigatorios ausentes (event={}, id={}, payment.id={})",
                eventType, eventId, pspCobrancaId);
            return ResponseEntity.badRequest().body("missing fields");
        }

        int inserted = jdbc.update("""
            INSERT INTO psp_webhook_events (provider, event_id, event_type, psp_cobranca_id, raw_payload)
            VALUES (?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (provider, event_id) DO NOTHING""",
            PROVIDER, eventId, eventType, pspCobrancaId, rawBody);
        if (inserted == 0) {
            log.info("[PspWebhook] Asaas — evento {} ja processado (idempotente)", eventId);
            return ResponseEntity.ok("ok");
        }

        try {
            String novoStatus = cobrancaService.processarEvento(PROVIDER, eventType, pspCobrancaId);
            jdbc.update(
                "UPDATE psp_webhook_events SET processed_at = CURRENT_TIMESTAMP, novo_status = ? WHERE provider = ? AND event_id = ?",
                novoStatus, PROVIDER, eventId);
        } catch (RuntimeException e) {
            log.error("[PspWebhook] Asaas — falha processando evento {}: {}", eventId, e.getMessage(), e);
            jdbc.update(
                "UPDATE psp_webhook_events SET processing_error = ? WHERE provider = ? AND event_id = ?",
                e.getMessage() != null ? e.getMessage() : "erro", PROVIDER, eventId);
        }
        return ResponseEntity.ok("ok");
    }
}
