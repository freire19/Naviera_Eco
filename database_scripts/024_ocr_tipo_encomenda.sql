-- Migration 024: Adicionar tipo (frete/encomenda) em ocr_lancamentos
-- Permite que o mesmo fluxo OCR crie fretes ou encomendas

ALTER TABLE ocr_lancamentos
    ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'frete';

-- Lancamentos existentes sao todos fretes
UPDATE ocr_lancamentos SET tipo = 'frete' WHERE tipo IS NULL;

-- Adicionar id_encomenda para linkagem quando aprovado como encomenda
ALTER TABLE ocr_lancamentos
    ADD COLUMN IF NOT EXISTS id_encomenda BIGINT REFERENCES encomendas(id_encomenda);
