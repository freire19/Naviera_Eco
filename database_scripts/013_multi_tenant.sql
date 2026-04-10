-- ============================================================================
-- MIGRATION 013: MULTI-TENANT
-- Adiciona suporte a multiplas empresas no mesmo banco de dados.
--
-- ESTRATEGIA: Coluna empresa_id em todas as tabelas de negocio.
--   - Tabelas auxiliares (aux_*) sao compartilhadas (dados de referencia).
--   - Tabelas de negocio recebem empresa_id com DEFAULT 1.
--   - Dados existentes ficam na empresa_id = 1 (migracao transparente).
--   - Row-Level Security (RLS) sera ativado em fase posterior.
--
-- IMPORTANTE: Executar com superuser ou owner do banco.
-- REVERSIVEL: Sim (DROP COLUMN empresa_id, DROP TABLE empresas).
-- ============================================================================

BEGIN;

-- ============================================================================
-- 1. TABELA CENTRAL: empresas
-- ============================================================================

CREATE TABLE IF NOT EXISTS empresas (
    id                 SERIAL PRIMARY KEY,
    nome               VARCHAR(300) NOT NULL,
    cnpj               VARCHAR(20) UNIQUE,
    ie                 VARCHAR(30),
    endereco           VARCHAR(500),
    cep                VARCHAR(15),
    telefone           VARCHAR(30),
    email              VARCHAR(200),
    path_logo          VARCHAR(500),
    plano              VARCHAR(50) DEFAULT 'basico',
    ativo              BOOLEAN NOT NULL DEFAULT TRUE,
    config_json        JSONB DEFAULT '{}',
    data_criacao       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inserir empresa padrao (migra dados existentes)
INSERT INTO empresas (id, nome, cnpj, ativo)
VALUES (1, 'Empresa Padrao (migrada)', NULL, TRUE)
ON CONFLICT (id) DO NOTHING;

-- Garantir que a sequence comece apos o id 1
SELECT setval('empresas_id_seq', GREATEST(1, (SELECT COALESCE(MAX(id), 0) FROM empresas)));

-- ============================================================================
-- 2. ADICIONAR empresa_id NAS TABELAS DE NEGOCIO
--    Todas com DEFAULT 1 para nao quebrar dados existentes.
--    FK para empresas(id) garante integridade.
-- ============================================================================

-- 2.1 Cadastros base
ALTER TABLE caixas
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE rotas
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE embarcacoes
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE conferentes
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE cad_clientes_encomenda
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE categorias_despesa
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE tipo_passageiro
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.2 Configuracao da empresa (agora multi-tenant)
ALTER TABLE configuracao_empresa
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.3 Itens padrao
ALTER TABLE itens_frete_padrao
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

ALTER TABLE itens_encomenda_padrao
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.4 Passageiros (compartilhados entre empresas? Nao — cada empresa tem seus registros)
ALTER TABLE passageiros
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.5 Viagens
ALTER TABLE viagens
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.6 Tarifas
ALTER TABLE tarifas
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.7 Passagens
ALTER TABLE passagens
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.8 Encomendas
ALTER TABLE encomendas
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.9 Fretes
ALTER TABLE fretes
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.10 Funcionarios
ALTER TABLE funcionarios
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.11 Financeiro saidas
ALTER TABLE financeiro_saidas
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.12 Eventos RH
ALTER TABLE eventos_rh
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.13 Recibos avulsos
ALTER TABLE recibos_avulsos
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.14 Historico recibo quitacao
ALTER TABLE historico_recibo_quitacao_passageiro
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.15 Agenda
ALTER TABLE agenda_anotacoes
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.16 Auditoria
ALTER TABLE auditoria_financeiro
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- 2.17 Usuarios (cada usuario pertence a uma empresa)
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS empresa_id INTEGER NOT NULL DEFAULT 1 REFERENCES empresas(id);

-- ============================================================================
-- 3. INDICES PARA PERFORMANCE (filtro por empresa_id)
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_passagens_empresa ON passagens(empresa_id);
CREATE INDEX IF NOT EXISTS idx_encomendas_empresa ON encomendas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_fretes_empresa ON fretes(empresa_id);
CREATE INDEX IF NOT EXISTS idx_viagens_empresa ON viagens(empresa_id);
CREATE INDEX IF NOT EXISTS idx_passageiros_empresa ON passageiros(empresa_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_empresa ON funcionarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_empresa ON financeiro_saidas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_empresa ON usuarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_tarifas_empresa ON tarifas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_rotas_empresa ON rotas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_embarcacoes_empresa ON embarcacoes(empresa_id);

-- Indices compostos para queries frequentes
CREATE INDEX IF NOT EXISTS idx_passagens_empresa_viagem ON passagens(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_encomendas_empresa_viagem ON encomendas(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_fretes_empresa_viagem ON fretes(empresa_id, id_viagem);
CREATE INDEX IF NOT EXISTS idx_viagens_empresa_data ON viagens(empresa_id, data_viagem);

-- ============================================================================
-- 4. TABELA CLIENTES_APP (global — clientes do app mobile, cross-tenant)
-- ============================================================================

CREATE TABLE IF NOT EXISTS clientes_app (
    id                 BIGSERIAL PRIMARY KEY,
    cpf                VARCHAR(14) UNIQUE,
    cnpj               VARCHAR(18) UNIQUE,
    nome               VARCHAR(300) NOT NULL,
    telefone           VARCHAR(30),
    email              VARCHAR(200) UNIQUE,
    senha_hash         VARCHAR(255) NOT NULL,
    tipo               VARCHAR(10) NOT NULL DEFAULT 'PF' CHECK (tipo IN ('PF', 'PJ')),
    ativo              BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 5. TABELA DE VERSAO (para auto-update do desktop)
-- ============================================================================

CREATE TABLE IF NOT EXISTS versao_sistema (
    id                 SERIAL PRIMARY KEY,
    versao             VARCHAR(20) NOT NULL,
    obrigatoria        BOOLEAN NOT NULL DEFAULT FALSE,
    url_download       VARCHAR(500),
    changelog          TEXT,
    data_publicacao    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO versao_sistema (versao, obrigatoria, changelog)
VALUES ('1.0.0', FALSE, 'Versao inicial multi-tenant')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 6. REMOVER CONSTRAINT UNIQUE de embarcacoes.nome (agora unico POR empresa)
-- ============================================================================

-- O nome da embarcacao so precisa ser unico dentro da mesma empresa
ALTER TABLE embarcacoes DROP CONSTRAINT IF EXISTS embarcacoes_nome_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_embarcacoes_nome_empresa
    ON embarcacoes(empresa_id, nome);

-- Mesmo para clientes encomenda
ALTER TABLE cad_clientes_encomenda DROP CONSTRAINT IF EXISTS cad_clientes_encomenda_nome_cliente_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_cad_clientes_nome_empresa
    ON cad_clientes_encomenda(empresa_id, nome_cliente);

COMMIT;

-- ============================================================================
-- NOTA: Tabelas auxiliares (aux_*) NAO recebem empresa_id.
-- Sao dados de referencia compartilhados: tipos de documento, sexo,
-- nacionalidades, tipos de passagem, agentes, horarios, acomodacoes,
-- formas de pagamento. Se uma empresa precisar customizar, usar
-- config_json na tabela empresas.
-- ============================================================================
