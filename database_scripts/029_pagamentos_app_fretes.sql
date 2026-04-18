-- Migration 029: Pagamento pelo app (CNPJ) em fretes
--
-- Espelha a migration 028 para a tabela `fretes`, com um adicional:
-- CNPJ pode pagar via BOLETO (CPF nao tem essa opcao).
--
-- Regras:
--   - PIX aplica 10% desconto (desconto_app). BOLETO, CARTAO e BARCO sem desconto.
--   - Pagamento vai para a empresa dona do frete (fretes.empresa_id), mesma
--     logica das passagens/encomendas.
--   - BOLETO e PIX/CARTAO geram status PENDENTE_CONFIRMACAO ate o PSP ou
--     operador confirmar. BARCO mantem status PENDENTE (paga no embarque).

BEGIN;

ALTER TABLE fretes
    ADD COLUMN IF NOT EXISTS status_pagamento        VARCHAR(30),
    ADD COLUMN IF NOT EXISTS forma_pagamento_app     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS desconto_app            NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_pagamento_app      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_transacao_psp        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS qr_pix_payload          TEXT,
    ADD COLUMN IF NOT EXISTS boleto_url              TEXT,
    ADD COLUMN IF NOT EXISTS boleto_linha_digitavel  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS id_cliente_app_pagador  BIGINT REFERENCES clientes_app(id);

ALTER TABLE fretes
    ADD CONSTRAINT chk_fretes_forma_pagamento_app
    CHECK (forma_pagamento_app IS NULL OR forma_pagamento_app IN ('PIX','CARTAO','BOLETO','BARCO'));

CREATE INDEX IF NOT EXISTS idx_fretes_cliente_app_pagador
    ON fretes(id_cliente_app_pagador)
    WHERE id_cliente_app_pagador IS NOT NULL;

COMMIT;
