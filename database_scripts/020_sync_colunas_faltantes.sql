-- ============================================================================
-- 020_sync_colunas_faltantes.sql
-- Adiciona colunas de sincronizacao (uuid, ultima_atualizacao, sincronizado)
-- nas tabelas que faltavam: caixas, conferentes, embarcacoes,
-- financeiro_saidas, rotas, tarifas.
-- Padrao identico ao das tabelas que ja tinham (viagens, passagens, etc).
-- ============================================================================

-- ── caixas ──
ALTER TABLE caixas ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE caixas ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE caixas ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_caixas_sync ON caixas (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_caixas_uuid ON caixas (uuid);

-- ── conferentes ──
ALTER TABLE conferentes ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE conferentes ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE conferentes ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_conferentes_sync ON conferentes (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_conferentes_uuid ON conferentes (uuid);

-- ── embarcacoes ──
ALTER TABLE embarcacoes ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE embarcacoes ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE embarcacoes ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_embarcacoes_sync ON embarcacoes (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_embarcacoes_uuid ON embarcacoes (uuid);

-- ── financeiro_saidas ──
ALTER TABLE financeiro_saidas ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE financeiro_saidas ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE financeiro_saidas ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_sync ON financeiro_saidas (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_uuid ON financeiro_saidas (uuid);

-- ── rotas ──
ALTER TABLE rotas ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE rotas ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rotas ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_rotas_sync ON rotas (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_rotas_uuid ON rotas (uuid);

-- ── tarifas ──
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tarifas ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_tarifas_sync ON tarifas (sincronizado) WHERE sincronizado = false;
CREATE INDEX IF NOT EXISTS idx_tarifas_uuid ON tarifas (uuid);
