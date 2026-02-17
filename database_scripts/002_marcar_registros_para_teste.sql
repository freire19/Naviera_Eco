-- ================================================================
-- Script para Marcar Registros como Não Sincronizados (TESTE)
-- Execute este script para testar a sincronização
-- ================================================================

-- 1. Marcar alguns passageiros como não sincronizados
UPDATE passageiros 
SET sincronizado = FALSE 
WHERE id IN (
    SELECT id FROM passageiros 
    ORDER BY id 
    LIMIT 5
);

-- 2. Marcar algumas passagens como não sincronizadas
UPDATE passagens 
SET sincronizado = FALSE 
WHERE id IN (
    SELECT id FROM passagens 
    ORDER BY id 
    LIMIT 10
);

-- 3. Marcar algumas viagens como não sincronizadas
UPDATE viagens 
SET sincronizado = FALSE 
WHERE id IN (
    SELECT id FROM viagens 
    ORDER BY id 
    LIMIT 3
);

-- 4. Marcar algumas encomendas como não sincronizadas
UPDATE encomendas 
SET sincronizado = FALSE 
WHERE id IN (
    SELECT id FROM encomendas 
    ORDER BY id 
    LIMIT 8
);

-- 5. Marcar alguns fretes como não sincronizados
UPDATE fretes 
SET sincronizado = FALSE 
WHERE id IN (
    SELECT id FROM fretes 
    ORDER BY id 
    LIMIT 5
);

-- Verificar resultados
SELECT 
    'passageiros' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados,
    COUNT(*) as total
FROM passageiros
UNION ALL
SELECT 
    'passagens' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados,
    COUNT(*) as total
FROM passagens
UNION ALL
SELECT 
    'viagens' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados,
    COUNT(*) as total
FROM viagens
UNION ALL
SELECT 
    'encomendas' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados,
    COUNT(*) as total
FROM encomendas
UNION ALL
SELECT 
    'fretes' as tabela,
    COUNT(*) FILTER (WHERE sincronizado = TRUE) as sincronizados,
    COUNT(*) FILTER (WHERE sincronizado = FALSE) as nao_sincronizados,
    COUNT(*) as total
FROM fretes;
