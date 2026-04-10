-- Migration 014: Trigger inteligente para sync bypass
-- Problema: o trigger atualizar_ultima_atualizacao() reseta sincronizado = FALSE em todo UPDATE.
-- Quando o sync aplica um registro recebido, ele seta sincronizado = TRUE, mas o trigger desfaz.
-- Solucao: se o UPDATE explicitamente muda sincronizado de FALSE para TRUE, o trigger nao interfere.

CREATE OR REPLACE FUNCTION atualizar_ultima_atualizacao()
RETURNS TRIGGER AS $$
BEGIN
    -- Sync bypass: se sincronizado esta sendo mudado para TRUE explicitamente,
    -- e uma operacao de sync. Preservar timestamp e flag.
    IF NEW.sincronizado IS DISTINCT FROM OLD.sincronizado AND NEW.sincronizado = TRUE THEN
        RETURN NEW;
    END IF;
    -- Edicao normal do usuario: resetar flag e atualizar timestamp
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    NEW.sincronizado = FALSE;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Esta funcao e referenciada por triggers em 16 tabelas.
-- Substituir a funcao com CREATE OR REPLACE atualiza todos os triggers automaticamente.
