-- Migration 027: Tabela generica de documentos arquivados
-- Armazena docs de identidade (RG, CNH, CPF) de passageiros, remetentes, etc.
-- Exigencia de compliance policia/marinha

CREATE TABLE IF NOT EXISTS documentos_arquivados (
    id              SERIAL PRIMARY KEY,
    empresa_id      INTEGER NOT NULL REFERENCES empresas(id),
    categoria       VARCHAR(30) NOT NULL,  -- passageiro, encomenda, frete, empresa
    referencia_id   BIGINT,                -- id do passageiro, encomenda, frete, etc.
    referencia_nome VARCHAR(300),           -- nome para busca rapida (ex: nome passageiro)
    nome_pessoa     VARCHAR(300) NOT NULL,
    cpf             VARCHAR(20),
    rg              VARCHAR(30),
    tipo_doc        VARCHAR(20),           -- RG, CNH, CPF, CTPS
    foto_path       VARCHAR(500) NOT NULL,
    id_usuario_criou INTEGER,
    nome_usuario_criou VARCHAR(200),
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_docs_arquivados_empresa ON documentos_arquivados(empresa_id);
CREATE INDEX idx_docs_arquivados_categoria ON documentos_arquivados(empresa_id, categoria);
