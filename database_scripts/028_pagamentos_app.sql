-- Migration 028: Dados de recebimento por empresa + pagamento pelo app (CPF)
--
-- Passagens e encomendas pagas pelo app sao creditadas para a empresa dona
-- da viagem/encomenda. Esta migration adiciona:
--   1. Dados de recebimento (chave PIX, conta bancaria, ID PSP) em `empresas`.
--   2. Campos de pagamento app em `passagens` e `encomendas`:
--      - forma_pagamento_app: PIX | CARTAO | BARCO
--      - desconto_app: valor do desconto (10% se PIX)
--      - data_pagamento_app, id_transacao_psp, qr_pix_payload
--
-- Regra: desconto de 10% SO se aplica quando forma_pagamento_app = 'PIX'.
-- Pagamento no barco mantem status pendente ate operador confirmar presencialmente.

BEGIN;

-- ============================================================================
-- 1. DADOS DE RECEBIMENTO POR EMPRESA
-- ============================================================================

ALTER TABLE empresas
    ADD COLUMN IF NOT EXISTS chave_pix             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS tipo_chave_pix        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS titular_conta         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS cpf_cnpj_recebedor    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS banco                 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS agencia               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS conta_numero          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS conta_tipo            VARCHAR(20),
    ADD COLUMN IF NOT EXISTS psp_provider          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS psp_subconta_id       VARCHAR(100);

ALTER TABLE empresas
    ADD CONSTRAINT chk_empresas_tipo_chave_pix
    CHECK (tipo_chave_pix IS NULL OR tipo_chave_pix IN ('CPF','CNPJ','EMAIL','TELEFONE','ALEATORIA'));

ALTER TABLE empresas
    ADD CONSTRAINT chk_empresas_conta_tipo
    CHECK (conta_tipo IS NULL OR conta_tipo IN ('CORRENTE','POUPANCA'));

-- ============================================================================
-- 2. PAGAMENTO APP EM PASSAGENS
-- ============================================================================

ALTER TABLE passagens
    ADD COLUMN IF NOT EXISTS forma_pagamento_app   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS desconto_app          NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_pagamento_app    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_transacao_psp      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS qr_pix_payload        TEXT;

ALTER TABLE passagens
    ADD CONSTRAINT chk_passagens_forma_pagamento_app
    CHECK (forma_pagamento_app IS NULL OR forma_pagamento_app IN ('PIX','CARTAO','BARCO'));

-- ============================================================================
-- 3. PAGAMENTO APP EM ENCOMENDAS
-- ============================================================================

ALTER TABLE encomendas
    ADD COLUMN IF NOT EXISTS id_cliente_app_destinatario BIGINT REFERENCES clientes_app(id),
    ADD COLUMN IF NOT EXISTS forma_pagamento_app         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS desconto_app                NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_pagamento_app          TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_transacao_psp            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS qr_pix_payload              TEXT;

ALTER TABLE encomendas
    ADD CONSTRAINT chk_encomendas_forma_pagamento_app
    CHECK (forma_pagamento_app IS NULL OR forma_pagamento_app IN ('PIX','CARTAO','BARCO'));

CREATE INDEX IF NOT EXISTS idx_encomendas_cliente_app_destinatario
    ON encomendas(id_cliente_app_destinatario)
    WHERE id_cliente_app_destinatario IS NOT NULL;

COMMIT;
