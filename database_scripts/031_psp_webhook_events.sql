-- Migration 031: Idempotencia de webhooks PSP (Asaas e futuros providers)
--
-- PSPs costumam reenviar webhooks em caso de falha (Asaas retenta ate receber 2xx).
-- Essa tabela guarda cada evento processado para:
--   - deduplicar reprocessamentos (UNIQUE em provider+event_id)
--   - auditar historico de eventos recebidos
--   - permitir reprocessamento manual com base no raw_payload

BEGIN;

CREATE TABLE IF NOT EXISTS psp_webhook_events (
    id                 BIGSERIAL PRIMARY KEY,
    provider           VARCHAR(30) NOT NULL,
    event_id           VARCHAR(100) NOT NULL,
    event_type         VARCHAR(50) NOT NULL,
    psp_cobranca_id    VARCHAR(100),
    novo_status        VARCHAR(30),
    received_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at       TIMESTAMP,
    processing_error   TEXT,
    raw_payload        JSONB NOT NULL
);

ALTER TABLE psp_webhook_events
    ADD CONSTRAINT uq_psp_webhook_events_provider_event
    UNIQUE (provider, event_id);

CREATE INDEX IF NOT EXISTS idx_psp_webhook_events_cobranca ON psp_webhook_events(psp_cobranca_id) WHERE psp_cobranca_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_psp_webhook_events_received ON psp_webhook_events(received_at DESC);

COMMIT;
