-- Migration 008: Tabelas para o app NavegaAM (clientes mobile/web)
-- Estas tabelas NAO interferem nas tabelas existentes do sistema desktop.
-- Usa a funcao atualizar_ultima_atualizacao() criada na migration 001.

BEGIN;

-- ============================================================================
-- TABELA: clientes_app (login do app mobile/web)
-- ============================================================================
CREATE TABLE IF NOT EXISTS clientes_app (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,
    tipo_documento VARCHAR(4) NOT NULL DEFAULT 'CPF',
    nome VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    telefone VARCHAR(20),
    cidade VARCHAR(100),
    senha_hash VARCHAR(255) NOT NULL,
    foto_url VARCHAR(500),
    ativo BOOLEAN DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso TIMESTAMP,
    cnpj_matriz VARCHAR(20),
    responsavel_nome VARCHAR(200),
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sincronizado BOOLEAN DEFAULT FALSE,
    excluido BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_clientes_app_documento ON clientes_app(documento);
CREATE INDEX IF NOT EXISTS idx_clientes_app_uuid ON clientes_app(uuid);
CREATE INDEX IF NOT EXISTS idx_clientes_app_sync ON clientes_app(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_clientes_app_update ON clientes_app;
CREATE TRIGGER trg_clientes_app_update
    BEFORE UPDATE ON clientes_app
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: dispositivos_push (tokens FCM para push notifications)
-- ============================================================================
CREATE TABLE IF NOT EXISTS dispositivos_push (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    token_fcm VARCHAR(500) NOT NULL,
    plataforma VARCHAR(10),
    ativo BOOLEAN DEFAULT TRUE,
    data_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dispositivos_push_cliente ON dispositivos_push(id_cliente);

-- ============================================================================
-- TABELA: feedbacks (avaliacoes dos clientes)
-- ============================================================================
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    nota INTEGER CHECK (nota BETWEEN 1 AND 5),
    comentario TEXT,
    data_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_cliente ON feedbacks(id_cliente);

-- ============================================================================
-- TABELA: pagamentos_app (pagamentos feitos via app)
-- ============================================================================
CREATE TABLE IF NOT EXISTS pagamentos_app (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES clientes_app(id),
    tipo_referencia VARCHAR(20) NOT NULL,
    id_referencia BIGINT NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDENTE',
    id_transacao_externo VARCHAR(200),
    data_pagamento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sincronizado BOOLEAN DEFAULT FALSE,
    excluido BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_pagamentos_app_cliente ON pagamentos_app(id_cliente);
CREATE INDEX IF NOT EXISTS idx_pagamentos_app_referencia ON pagamentos_app(tipo_referencia, id_referencia);
CREATE INDEX IF NOT EXISTS idx_pagamentos_app_uuid ON pagamentos_app(uuid);
CREATE INDEX IF NOT EXISTS idx_pagamentos_app_sync ON pagamentos_app(sincronizado) WHERE sincronizado = FALSE;

DROP TRIGGER IF EXISTS trg_pagamentos_app_update ON pagamentos_app;
CREATE TRIGGER trg_pagamentos_app_update
    BEFORE UPDATE ON pagamentos_app
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ultima_atualizacao();

COMMIT;
