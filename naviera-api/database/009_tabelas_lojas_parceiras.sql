-- Migration 009: Tabelas para Lojas Parceiras no Naviera
-- Extensão da migration 008 (clientes_app).
-- Permite que clientes CNPJ tenham vitrine, recebam pedidos e gerem rastreio.

BEGIN;

-- ============================================================================
-- TABELA: lojas_parceiras (vitrine do comerciante CNPJ)
-- Vinculada a clientes_app que tem tipo_documento = 'CNPJ'
-- ============================================================================
CREATE TABLE IF NOT EXISTS lojas_parceiras (
    id BIGSERIAL PRIMARY KEY,
    id_cliente_app BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    nome_fantasia VARCHAR(200) NOT NULL,
    segmento VARCHAR(100),
    descricao TEXT,
    telefone_comercial VARCHAR(20),
    email_comercial VARCHAR(200),
    -- Rotas atendidas (array de nomes de cidades destino)
    rotas_atendidas TEXT[] DEFAULT '{}',
    verificada BOOLEAN DEFAULT FALSE,
    ativa BOOLEAN DEFAULT TRUE,
    total_entregas INTEGER DEFAULT 0,
    nota_media NUMERIC(2,1) DEFAULT 0.0,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loja_cliente UNIQUE (id_cliente_app)
);

CREATE INDEX IF NOT EXISTS idx_lojas_ativa ON lojas_parceiras(ativa) WHERE ativa = TRUE;
CREATE INDEX IF NOT EXISTS idx_lojas_verificada ON lojas_parceiras(verificada) WHERE verificada = TRUE;

DROP TRIGGER IF EXISTS trg_lojas_parceiras_update ON lojas_parceiras;
CREATE TRIGGER trg_lojas_parceiras_update
    BEFORE UPDATE ON lojas_parceiras
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: pedidos_loja (pedidos feitos por clientes CPF a lojas CNPJ)
-- Pode ser vinculado a um frete existente para rastreio automático
-- ============================================================================
CREATE TABLE IF NOT EXISTS pedidos_loja (
    id BIGSERIAL PRIMARY KEY,
    numero_pedido VARCHAR(20) NOT NULL UNIQUE,
    id_loja BIGINT NOT NULL REFERENCES lojas_parceiras(id),
    id_cliente_comprador BIGINT NOT NULL REFERENCES clientes_app(id),
    cidade_destino VARCHAR(100) NOT NULL,
    descricao_itens TEXT NOT NULL,
    valor_total DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'AGUARDANDO_EMBARQUE',
    -- Vínculo com frete existente (sistema desktop)
    id_frete BIGINT,  -- FK manual → fretes.id_frete
    -- Rastreio
    codigo_rastreio VARCHAR(50),
    observacoes TEXT,
    data_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_embarque TIMESTAMP,
    data_entrega TIMESTAMP,
    ultima_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status_pedido CHECK (status IN (
        'AGUARDANDO_EMBARQUE', 'EM_TRANSITO', 'ENTREGUE', 'CANCELADO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_pedidos_loja ON pedidos_loja(id_loja);
CREATE INDEX IF NOT EXISTS idx_pedidos_comprador ON pedidos_loja(id_cliente_comprador);
CREATE INDEX IF NOT EXISTS idx_pedidos_frete ON pedidos_loja(id_frete) WHERE id_frete IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pedidos_status ON pedidos_loja(status);

-- Sequence para numero_pedido (PED-XXXX)
CREATE SEQUENCE IF NOT EXISTS seq_pedido_loja START WITH 1;

DROP TRIGGER IF EXISTS trg_pedidos_loja_update ON pedidos_loja;
CREATE TRIGGER trg_pedidos_loja_update
    BEFORE UPDATE ON pedidos_loja
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ultima_atualizacao();

-- ============================================================================
-- TABELA: avaliacoes_loja (avaliações de clientes CPF sobre lojas)
-- ============================================================================
CREATE TABLE IF NOT EXISTS avaliacoes_loja (
    id BIGSERIAL PRIMARY KEY,
    id_loja BIGINT NOT NULL REFERENCES lojas_parceiras(id) ON DELETE CASCADE,
    id_cliente BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    nota INTEGER NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario TEXT,
    id_pedido BIGINT REFERENCES pedidos_loja(id),
    data_avaliacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Evita avaliação duplicada por pedido
    CONSTRAINT uq_avaliacao_pedido UNIQUE (id_cliente, id_pedido)
);

CREATE INDEX IF NOT EXISTS idx_avaliacoes_loja ON avaliacoes_loja(id_loja);

-- ============================================================================
-- TABELA: amigos_app (rede de amigos entre usuários CPF)
-- ============================================================================
CREATE TABLE IF NOT EXISTS amigos_app (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    id_amigo BIGINT NOT NULL REFERENCES clientes_app(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'PENDENTE',
    data_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_aceite TIMESTAMP,
    CONSTRAINT uq_amizade UNIQUE (id_cliente, id_amigo),
    CONSTRAINT chk_amizade_diff CHECK (id_cliente != id_amigo),
    CONSTRAINT chk_status_amigo CHECK (status IN ('PENDENTE', 'ACEITO', 'BLOQUEADO'))
);

CREATE INDEX IF NOT EXISTS idx_amigos_cliente ON amigos_app(id_cliente);
CREATE INDEX IF NOT EXISTS idx_amigos_amigo ON amigos_app(id_amigo);

-- ============================================================================
-- FUNCTION: Atualizar nota_media e total_entregas da loja
-- Chamada por trigger quando avaliação é inserida ou pedido entregue
-- ============================================================================
CREATE OR REPLACE FUNCTION atualizar_stats_loja()
RETURNS TRIGGER AS $$
BEGIN
    -- Recalcula nota média
    UPDATE lojas_parceiras SET
        nota_media = COALESCE((
            SELECT ROUND(AVG(nota)::numeric, 1)
            FROM avaliacoes_loja WHERE id_loja = NEW.id_loja
        ), 0),
        total_entregas = (
            SELECT COUNT(*) FROM pedidos_loja
            WHERE id_loja = NEW.id_loja AND status = 'ENTREGUE'
        )
    WHERE id = NEW.id_loja;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_atualizar_stats_avaliacao ON avaliacoes_loja;
CREATE TRIGGER trg_atualizar_stats_avaliacao
    AFTER INSERT OR UPDATE ON avaliacoes_loja
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_stats_loja();

COMMIT;
