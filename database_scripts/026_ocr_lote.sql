-- Migration 026: Suporte a lote de encomendas no OCR
-- Permite que 1 foto de protocolo gere N lancamentos agrupados

ALTER TABLE ocr_lancamentos
    ADD COLUMN IF NOT EXISTS lote_uuid UUID,
    ADD COLUMN IF NOT EXISTS lote_index SMALLINT;

CREATE INDEX IF NOT EXISTS idx_ocr_lancamentos_lote ON ocr_lancamentos(lote_uuid) WHERE lote_uuid IS NOT NULL;
