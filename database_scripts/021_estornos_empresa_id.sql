-- Migration 021: Adiciona empresa_id nas tabelas de log de estornos para multi-tenant

BEGIN;

ALTER TABLE log_estornos_passagens ADD COLUMN IF NOT EXISTS empresa_id INTEGER;
ALTER TABLE log_estornos_encomendas ADD COLUMN IF NOT EXISTS empresa_id INTEGER;
ALTER TABLE log_estornos_fretes ADD COLUMN IF NOT EXISTS empresa_id INTEGER;

CREATE INDEX IF NOT EXISTS idx_log_estornos_passagens_empresa ON log_estornos_passagens(empresa_id);
CREATE INDEX IF NOT EXISTS idx_log_estornos_encomendas_empresa ON log_estornos_encomendas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_log_estornos_fretes_empresa ON log_estornos_fretes(empresa_id);

CREATE INDEX IF NOT EXISTS idx_log_estornos_passagens_data ON log_estornos_passagens(data_hora);
CREATE INDEX IF NOT EXISTS idx_log_estornos_encomendas_data ON log_estornos_encomendas(data_hora);

COMMIT;
