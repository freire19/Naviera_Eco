-- ============================================================================
-- SCRIPT DE MIGRAÇÃO: Adicionar campos de sincronização
-- Sistema de Embarcação - Preparação para sincronização Desktop/Web
-- Data: 2024
-- ============================================================================

-- Este script adiciona os campos necessários para sincronização em todas as tabelas
-- Campos:
--   uuid: Identificador único global para sincronização entre bancos
--   ultima_atualizacao: Timestamp da última modificação
--   sincronizado: Flag indicando se o registro foi sincronizado com a nuvem
--   excluido: Flag para soft delete (não apaga, apenas marca)

-- ============================================================================
-- FUNÇÃO PARA ATUALIZAR TIMESTAMP AUTOMATICAMENTE
-- ============================================================================

CREATE OR REPLACE FUNCTION atualizar_ultima_atualizacao()
RETURNS TRIGGER AS $$
BEGIN
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    NEW.sincronizado = FALSE;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABELA: passageiros
-- ============================================================================
ALTER TABLE passageiros 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_passageiros_uuid ON passageiros(uuid);
CREATE INDEX IF NOT EXISTS idx_passageiros_sync ON passageiros(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_passageiros_update ON passageiros;
CREATE TRIGGER trg_passageiros_update
    BEFORE UPDATE ON passageiros
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: passagens
-- ============================================================================
ALTER TABLE passagens 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_passagens_uuid ON passagens(uuid);
CREATE INDEX IF NOT EXISTS idx_passagens_sync ON passagens(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_passagens_update ON passagens;
CREATE TRIGGER trg_passagens_update
    BEFORE UPDATE ON passagens
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: viagens
-- ============================================================================
ALTER TABLE viagens 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_viagens_uuid ON viagens(uuid);
CREATE INDEX IF NOT EXISTS idx_viagens_sync ON viagens(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_viagens_update ON viagens;
CREATE TRIGGER trg_viagens_update
    BEFORE UPDATE ON viagens
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: encomendas
-- ============================================================================
ALTER TABLE encomendas 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_encomendas_uuid ON encomendas(uuid);
CREATE INDEX IF NOT EXISTS idx_encomendas_sync ON encomendas(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_encomendas_update ON encomendas;
CREATE TRIGGER trg_encomendas_update
    BEFORE UPDATE ON encomendas
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: encomenda_itens
-- ============================================================================
ALTER TABLE encomenda_itens 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_encomenda_itens_uuid ON encomenda_itens(uuid);
CREATE INDEX IF NOT EXISTS idx_encomenda_itens_sync ON encomenda_itens(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_encomenda_itens_update ON encomenda_itens;
CREATE TRIGGER trg_encomenda_itens_update
    BEFORE UPDATE ON encomenda_itens
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: fretes
-- ============================================================================
ALTER TABLE fretes 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_fretes_uuid ON fretes(uuid);
CREATE INDEX IF NOT EXISTS idx_fretes_sync ON fretes(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_fretes_update ON fretes;
CREATE TRIGGER trg_fretes_update
    BEFORE UPDATE ON fretes
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: itens_frete
-- ============================================================================
ALTER TABLE itens_frete 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_itens_frete_uuid ON itens_frete(uuid);
CREATE INDEX IF NOT EXISTS idx_itens_frete_sync ON itens_frete(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_itens_frete_update ON itens_frete;
CREATE TRIGGER trg_itens_frete_update
    BEFORE UPDATE ON itens_frete
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_clientes_encomenda
-- ============================================================================
ALTER TABLE cad_clientes_encomenda 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_clientes_encomenda_uuid ON cad_clientes_encomenda(uuid);
CREATE INDEX IF NOT EXISTS idx_clientes_encomenda_sync ON cad_clientes_encomenda(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_clientes_encomenda_update ON cad_clientes_encomenda;
CREATE TRIGGER trg_clientes_encomenda_update
    BEFORE UPDATE ON cad_clientes_encomenda
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_caixa
-- ============================================================================
ALTER TABLE cad_caixa 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_caixa_uuid ON cad_caixa(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_caixa_sync ON cad_caixa(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_caixa_update ON cad_caixa;
CREATE TRIGGER trg_cad_caixa_update
    BEFORE UPDATE ON cad_caixa
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: usuarios
-- ============================================================================
ALTER TABLE usuarios 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_usuarios_uuid ON usuarios(uuid);
CREATE INDEX IF NOT EXISTS idx_usuarios_sync ON usuarios(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_usuarios_update ON usuarios;
CREATE TRIGGER trg_usuarios_update
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_empresa
-- ============================================================================
ALTER TABLE cad_empresa 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_empresa_uuid ON cad_empresa(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_empresa_sync ON cad_empresa(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_empresa_update ON cad_empresa;
CREATE TRIGGER trg_cad_empresa_update
    BEFORE UPDATE ON cad_empresa
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_rotas
-- ============================================================================
ALTER TABLE cad_rotas 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_rotas_uuid ON cad_rotas(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_rotas_sync ON cad_rotas(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_rotas_update ON cad_rotas;
CREATE TRIGGER trg_cad_rotas_update
    BEFORE UPDATE ON cad_rotas
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_embarcacao
-- ============================================================================
ALTER TABLE cad_embarcacao 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_embarcacao_uuid ON cad_embarcacao(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_embarcacao_sync ON cad_embarcacao(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_embarcacao_update ON cad_embarcacao;
CREATE TRIGGER trg_cad_embarcacao_update
    BEFORE UPDATE ON cad_embarcacao
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_tarifas
-- ============================================================================
ALTER TABLE cad_tarifas 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_tarifas_uuid ON cad_tarifas(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_tarifas_sync ON cad_tarifas(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_tarifas_update ON cad_tarifas;
CREATE TRIGGER trg_cad_tarifas_update
    BEFORE UPDATE ON cad_tarifas
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_produtos
-- ============================================================================
ALTER TABLE cad_produtos 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cad_produtos_uuid ON cad_produtos(uuid);
CREATE INDEX IF NOT EXISTS idx_cad_produtos_sync ON cad_produtos(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_cad_produtos_update ON cad_produtos;
CREATE TRIGGER trg_cad_produtos_update
    BEFORE UPDATE ON cad_produtos
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: cad_itens_encomenda_padrao
-- ============================================================================
ALTER TABLE cad_itens_encomenda_padrao 
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS sincronizado BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excluido BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_itens_encomenda_padrao_uuid ON cad_itens_encomenda_padrao(uuid);
CREATE INDEX IF NOT EXISTS idx_itens_encomenda_padrao_sync ON cad_itens_encomenda_padrao(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_itens_encomenda_padrao_update ON cad_itens_encomenda_padrao;
CREATE TRIGGER trg_itens_encomenda_padrao_update
    BEFORE UPDATE ON cad_itens_encomenda_padrao
    FOR EACH ROW EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA DE CONTROLE DE SINCRONIZAÇÃO
-- ============================================================================
CREATE TABLE IF NOT EXISTS sync_controle (
    id SERIAL PRIMARY KEY,
    tabela VARCHAR(100) NOT NULL,
    ultima_sincronizacao TIMESTAMP,
    direcao VARCHAR(20) DEFAULT 'AMBOS', -- 'UPLOAD', 'DOWNLOAD', 'AMBOS'
    registros_sincronizados INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDENTE',
    mensagem_erro TEXT,
    UNIQUE(tabela)
);

-- Inserir registros de controle para cada tabela
INSERT INTO sync_controle (tabela, direcao) VALUES 
    ('passageiros', 'AMBOS'),
    ('passagens', 'AMBOS'),
    ('viagens', 'AMBOS'),
    ('encomendas', 'AMBOS'),
    ('encomenda_itens', 'AMBOS'),
    ('fretes', 'AMBOS'),
    ('itens_frete', 'AMBOS'),
    ('cad_clientes_encomenda', 'AMBOS'),
    ('cad_caixa', 'AMBOS'),
    ('usuarios', 'AMBOS'),
    ('cad_empresa', 'AMBOS'),
    ('cad_rotas', 'AMBOS'),
    ('cad_embarcacao', 'AMBOS'),
    ('cad_tarifas', 'AMBOS'),
    ('cad_produtos', 'AMBOS'),
    ('cad_itens_encomenda_padrao', 'AMBOS')
ON CONFLICT (tabela) DO NOTHING;

-- ============================================================================
-- TABELA DE LOG DE SINCRONIZAÇÃO
-- ============================================================================
CREATE TABLE IF NOT EXISTS sync_log (
    id SERIAL PRIMARY KEY,
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(20), -- 'UPLOAD', 'DOWNLOAD', 'CONFLITO'
    tabela VARCHAR(100),
    uuid_registro UUID,
    acao VARCHAR(20), -- 'INSERT', 'UPDATE', 'DELETE'
    sucesso BOOLEAN DEFAULT TRUE,
    mensagem TEXT,
    dados_json JSONB
);

CREATE INDEX IF NOT EXISTS idx_sync_log_data ON sync_log(data_hora);
CREATE INDEX IF NOT EXISTS idx_sync_log_tabela ON sync_log(tabela);

-- ============================================================================
-- ATUALIZAR UUIDs PARA REGISTROS EXISTENTES (se estiverem nulos)
-- ============================================================================
UPDATE passageiros SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE passagens SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE viagens SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE encomendas SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE encomenda_itens SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE fretes SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE itens_frete SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_clientes_encomenda SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_caixa SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE usuarios SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_empresa SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_rotas SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_embarcacao SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_tarifas SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_produtos SET uuid = gen_random_uuid() WHERE uuid IS NULL;
UPDATE cad_itens_encomenda_padrao SET uuid = gen_random_uuid() WHERE uuid IS NULL;

-- Marcar todos como sincronizados inicialmente
UPDATE passageiros SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE passagens SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE viagens SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE encomendas SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE encomenda_itens SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE fretes SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE itens_frete SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_clientes_encomenda SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_caixa SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE usuarios SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_empresa SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_rotas SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_embarcacao SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_tarifas SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_produtos SET sincronizado = TRUE WHERE sincronizado IS NULL;
UPDATE cad_itens_encomenda_padrao SET sincronizado = TRUE WHERE sincronizado IS NULL;

-- ============================================================================
-- MENSAGEM FINAL
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '============================================================';
    RAISE NOTICE 'Campos de sincronização adicionados com sucesso!';
    RAISE NOTICE 'Tabelas modificadas: 16';
    RAISE NOTICE 'Campos adicionados: uuid, ultima_atualizacao, sincronizado, excluido';
    RAISE NOTICE 'Triggers criados para atualização automática';
    RAISE NOTICE '============================================================';
END $$;
