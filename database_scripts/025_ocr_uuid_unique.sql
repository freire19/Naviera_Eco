-- Migration 025: Tornar uuid UNIQUE em ocr_lancamentos para idempotencia
-- Permite que o frontend envie client_uuid no upload e o BFF rejeite duplicatas

-- Dropar indice antigo (nao-unique)
DROP INDEX IF EXISTS idx_ocr_lancamentos_uuid;

-- Criar constraint UNIQUE
ALTER TABLE ocr_lancamentos
    ADD CONSTRAINT uq_ocr_lancamentos_uuid UNIQUE (uuid);
