-- ============================================================================
-- MIGRATION 014: Adicionar slug na tabela empresas
-- Slug e o subdominio usado para identificar a empresa na URL
-- Ex: slug = 'saofrancisco' → saofrancisco.naviera.com.br
-- ============================================================================

BEGIN;

-- Adicionar coluna slug (unico, lowercase, sem espacos)
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS slug VARCHAR(50) UNIQUE;

-- Adicionar colunas uteis para o painel admin
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS cor_primaria VARCHAR(7) DEFAULT '#059669';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Atualizar empresa padrao com slug
UPDATE empresas SET slug = 'padrao' WHERE id = 1 AND slug IS NULL;

-- Indice para busca rapida por slug
CREATE INDEX IF NOT EXISTS idx_empresas_slug ON empresas(slug);

COMMIT;
