-- Script 010: Criar sequence para numero_frete
-- Resolve: DL035 (race condition numero_frete MAX+1)
-- Data: 2026-04-08

DO $$
DECLARE
    max_frete BIGINT;
BEGIN
    SELECT COALESCE(MAX(numero_frete), 0) INTO max_frete FROM fretes;

    EXECUTE 'CREATE SEQUENCE IF NOT EXISTS seq_numero_frete START WITH ' || (max_frete + 1);
END $$;
