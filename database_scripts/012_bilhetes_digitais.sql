-- Migration 012: Bilhetes digitais (compra via app + anti-clone TOTP)
-- Referencia passagens existentes + clientes_app

BEGIN;

CREATE TABLE IF NOT EXISTS bilhetes_digitais (
    id                  BIGSERIAL PRIMARY KEY,
    id_passagem         BIGINT NOT NULL REFERENCES passagens(id_passagem),
    id_cliente_app      BIGINT NOT NULL REFERENCES clientes_app(id),
    totp_secret         VARCHAR(64) NOT NULL,
    qr_hash             VARCHAR(128) NOT NULL UNIQUE,
    status              VARCHAR(20) DEFAULT 'VALIDO'
                        CHECK (status IN ('VALIDO', 'EMBARCADO', 'CANCELADO', 'EXPIRADO')),
    data_embarque       TIMESTAMP,
    validado_por        VARCHAR(100),
    data_criacao        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_atualizacao  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bilhetes_cliente ON bilhetes_digitais(id_cliente_app);
CREATE INDEX IF NOT EXISTS idx_bilhetes_passagem ON bilhetes_digitais(id_passagem);
CREATE INDEX IF NOT EXISTS idx_bilhetes_qr ON bilhetes_digitais(qr_hash);
CREATE INDEX IF NOT EXISTS idx_bilhetes_status ON bilhetes_digitais(status) WHERE status = 'VALIDO';

DROP TRIGGER IF EXISTS trg_bilhetes_digitais_update ON bilhetes_digitais;
CREATE TRIGGER trg_bilhetes_digitais_update
    BEFORE UPDATE ON bilhetes_digitais
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ultima_atualizacao();

COMMIT;
