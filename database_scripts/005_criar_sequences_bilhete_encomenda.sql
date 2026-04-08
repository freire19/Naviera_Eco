-- Script 005: Criar sequences para numero_bilhete e numero_encomenda
-- Resolve: DL001 (race condition bilhete) e DL002 (race condition encomenda)
-- Data: 2026-04-07

-- Sequence para numero_bilhete (global)
DO $$
DECLARE
    max_bilhete INTEGER;
BEGIN
    SELECT COALESCE(MAX(CAST(numero_bilhete AS INTEGER)), 0) INTO max_bilhete
    FROM passagens
    WHERE numero_bilhete ~ '^\d+$';

    EXECUTE 'CREATE SEQUENCE IF NOT EXISTS seq_numero_bilhete START WITH ' || (max_bilhete + 1);
END $$;

-- Sequence para numero_encomenda (global)
DO $$
DECLARE
    max_encomenda INTEGER;
BEGIN
    SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) INTO max_encomenda
    FROM encomendas
    WHERE numero_encomenda ~ '^\d+$';

    EXECUTE 'CREATE SEQUENCE IF NOT EXISTS seq_numero_encomenda START WITH ' || (max_encomenda + 1);
END $$;
