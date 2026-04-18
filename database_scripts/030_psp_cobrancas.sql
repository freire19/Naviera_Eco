-- Migration 030: Log de cobrancas no PSP (Payment Service Provider)
--
-- Cada chamada ao PSP (criar cobranca PIX/Cartao/Boleto) gera um registro.
-- Fonte de verdade pra:
--   - rastrear ciclo de vida da cobranca (PENDENTE → CONFIRMADA/VENCIDA/CANCELADA)
--   - reconciliar com passagens/encomendas/fretes via (tipo_origem, origem_id)
--   - registrar split (parcela Naviera vs parcela empresa)
--   - guardar artefatos (QR PIX, linha digitavel, checkout URL)
--   - webhook de conciliacao busca por psp_cobranca_id

BEGIN;

CREATE TABLE IF NOT EXISTS psp_cobrancas (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            INTEGER NOT NULL REFERENCES empresas(id),

    -- Referencia polimorfica pro que esta sendo cobrado
    tipo_origem           VARCHAR(20) NOT NULL,
    origem_id             BIGINT NOT NULL,

    -- Dados do PSP
    psp_provider          VARCHAR(30) NOT NULL,
    psp_cobranca_id       VARCHAR(100) NOT NULL,
    psp_status            VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    forma_pagamento       VARCHAR(20) NOT NULL,

    -- Valores e split
    valor_bruto           NUMERIC(12,2) NOT NULL,
    desconto_aplicado     NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_liquido         NUMERIC(12,2) NOT NULL,
    split_naviera_pct     NUMERIC(5,2) NOT NULL DEFAULT 1.50,
    split_naviera_valor   NUMERIC(12,2) NOT NULL DEFAULT 0,
    split_empresa_valor   NUMERIC(12,2) NOT NULL DEFAULT 0,

    -- Artefatos para exibir ao pagador
    qr_code_payload       TEXT,
    qr_code_image_url     TEXT,
    linha_digitavel       VARCHAR(100),
    boleto_url            TEXT,
    checkout_url          TEXT,

    -- Metadata
    cliente_app_id        BIGINT REFERENCES clientes_app(id),
    data_criacao          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_confirmacao      TIMESTAMP,
    data_vencimento       TIMESTAMP,
    raw_response          JSONB
);

ALTER TABLE psp_cobrancas
    ADD CONSTRAINT chk_psp_cobrancas_tipo_origem
    CHECK (tipo_origem IN ('PASSAGEM','ENCOMENDA','FRETE'));

ALTER TABLE psp_cobrancas
    ADD CONSTRAINT chk_psp_cobrancas_forma_pagamento
    CHECK (forma_pagamento IN ('PIX','CARTAO','BOLETO'));

ALTER TABLE psp_cobrancas
    ADD CONSTRAINT chk_psp_cobrancas_psp_status
    CHECK (psp_status IN ('PENDENTE','CONFIRMADA','VENCIDA','CANCELADA','ESTORNADA'));

ALTER TABLE psp_cobrancas
    ADD CONSTRAINT uq_psp_cobrancas_provider_id
    UNIQUE (psp_provider, psp_cobranca_id);

CREATE INDEX IF NOT EXISTS idx_psp_cobrancas_empresa ON psp_cobrancas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_psp_cobrancas_origem ON psp_cobrancas(tipo_origem, origem_id);
CREATE INDEX IF NOT EXISTS idx_psp_cobrancas_status ON psp_cobrancas(psp_status) WHERE psp_status = 'PENDENTE';
CREATE INDEX IF NOT EXISTS idx_psp_cobrancas_cliente ON psp_cobrancas(cliente_app_id) WHERE cliente_app_id IS NOT NULL;

COMMIT;
