-- Migration 032 — #659: contador de tentativas de TOTP para rate-limit
--   Bloqueia bilhete apos 10 falhas. Reset no embarque (sucesso).

ALTER TABLE bilhetes_digitais
    ADD COLUMN IF NOT EXISTS totp_attempts INTEGER NOT NULL DEFAULT 0;

-- Indice parcial para monitoramento (bilhetes com tentativas altas)
CREATE INDEX IF NOT EXISTS idx_bilhetes_totp_attempts
    ON bilhetes_digitais(totp_attempts) WHERE totp_attempts > 0;
