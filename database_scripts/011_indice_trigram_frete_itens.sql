-- DP009: Indice trigram para busca ILIKE '%termo%' em frete_itens.nome_item_ou_id_produto
-- Requer extensao pg_trgm (padrao no PostgreSQL)
-- Sem este indice, toda busca por item de frete faz sequential scan na tabela inteira.

-- 1. Habilitar extensao trigram (idempotente)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Criar indice GIN trigram
CREATE INDEX IF NOT EXISTS idx_frete_itens_nome_trgm
    ON frete_itens USING gin (nome_item_ou_id_produto gin_trgm_ops);

-- Verificar
-- EXPLAIN ANALYZE SELECT 1 FROM frete_itens WHERE nome_item_ou_id_produto ILIKE '%teste%';
