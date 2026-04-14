-- ============================================================================
-- MIGRATION 023: ONBOARDING SELF-SERVICE
-- Adiciona codigo de ativacao para registro de empresas pelo site,
-- e flag de troca de senha obrigatoria no primeiro login.
-- ============================================================================

BEGIN;

-- 1. Codigo de ativacao (usado pelo Desktop no setup simplificado)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS codigo_ativacao VARCHAR(20) UNIQUE;
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS ativado_em TIMESTAMP;

-- 2. Troca de senha obrigatoria no primeiro login
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS deve_trocar_senha BOOLEAN DEFAULT FALSE;

-- 3. Indice para busca rapida por codigo
CREATE INDEX IF NOT EXISTS idx_empresas_codigo_ativacao ON empresas(codigo_ativacao);

COMMIT;
