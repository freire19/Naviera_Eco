-- ============================================================================
-- 022_ocr_lancamentos.sql
-- Tabela para rastrear lancamentos de frete via OCR (foto de nota/cupom/caderno)
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS ocr_lancamentos (
    id                      SERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL DEFAULT gen_random_uuid(),
    empresa_id              INTEGER NOT NULL REFERENCES empresas(id),
    id_viagem               BIGINT REFERENCES viagens(id_viagem),
    id_frete                BIGINT REFERENCES fretes(id_frete),  -- preenchido apos aprovacao

    -- Foto
    foto_path               VARCHAR(500) NOT NULL,
    foto_original_name      VARCHAR(300),

    -- OCR raw
    ocr_texto_bruto         TEXT,
    ocr_json                JSONB,
    ocr_confianca           NUMERIC(5,2),  -- media de confianca 0-100

    -- Dados extraidos pelo OCR (parseados)
    dados_extraidos         JSONB NOT NULL DEFAULT '{}',
    -- Estrutura esperada:
    -- {
    --   "remetente": "Joao Silva",
    --   "destinatario": "Maria Costa",
    --   "rota": "Manaus - Parintins",
    --   "itens": [
    --     { "nome_item": "Cx Margarina", "quantidade": 1, "preco_unitario": 5.00, "subtotal": 5.00, "confianca": 92 },
    --     { "nome_item": "Saco Farinha", "quantidade": 2, "preco_unitario": 12.00, "subtotal": 24.00, "confianca": 85 }
    --   ],
    --   "valor_total": 29.00,
    --   "observacoes": ""
    -- }

    -- Dados revisados pelo operador (versao corrigida)
    dados_revisados         JSONB,

    -- Workflow: pendente -> revisado_operador -> aprovado | rejeitado
    status                  VARCHAR(30) NOT NULL DEFAULT 'pendente',
    motivo_rejeicao         TEXT,

    -- Quem criou (operador que tirou a foto)
    id_usuario_criou        INTEGER,
    nome_usuario_criou      VARCHAR(200),

    -- Quem revisou (conferente que aprovou/rejeitou)
    id_usuario_revisou      INTEGER,
    nome_usuario_revisou    VARCHAR(200),
    data_revisao            TIMESTAMP,

    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices
CREATE INDEX idx_ocr_lancamentos_empresa ON ocr_lancamentos(empresa_id);
CREATE INDEX idx_ocr_lancamentos_status ON ocr_lancamentos(empresa_id, status);
CREATE INDEX idx_ocr_lancamentos_viagem ON ocr_lancamentos(id_viagem);
CREATE INDEX idx_ocr_lancamentos_uuid ON ocr_lancamentos(uuid);

-- Trigger para atualizar atualizado_em automaticamente
CREATE OR REPLACE FUNCTION atualizar_ocr_lancamentos_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ocr_lancamentos_atualizado
    BEFORE UPDATE ON ocr_lancamentos
    FOR EACH ROW
    EXECUTE FUNCTION atualizar_ocr_lancamentos_timestamp();

COMMIT;
