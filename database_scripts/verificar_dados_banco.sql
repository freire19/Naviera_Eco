-- ================================================================
-- Script para Verificar Dados no Banco PostgreSQL
-- Execute linha por linha no pgAdmin para diagnosticar
-- ================================================================

-- 1. VERIFICAR SE AS COLUNAS DE SINCRONIZAÇÃO EXISTEM
SELECT 
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name IN ('passageiros', 'passagens', 'viagens', 'encomendas', 'fretes')
  AND column_name IN ('uuid', 'ultima_atualizacao', 'sincronizado', 'excluido')
ORDER BY table_name, column_name;

-- 2. VERIFICAR SE HÁ DADOS NAS TABELAS
SELECT 
    'passageiros' as tabela, COUNT(*) as total_registros 
FROM passageiros
UNION ALL
SELECT 
    'passagens' as tabela, COUNT(*) as total_registros 
FROM passagens
UNION ALL
SELECT 
    'viagens' as tabela, COUNT(*) as total_registros 
FROM viagens
UNION ALL
SELECT 
    'encomendas' as tabela, COUNT(*) as total_registros 
FROM encomendas
UNION ALL
SELECT 
    'fretes' as tabela, COUNT(*) as total_registros 
FROM fretes;

-- 3. VERIFICAR REGISTROS SINCRONIZADOS VS NÃO SINCRONIZADOS
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

-- 4. VER ALGUNS REGISTROS DE EXEMPLO
-- Passageiros
SELECT id, nome, cpf, sincronizado, excluido, ultima_atualizacao
FROM passageiros
LIMIT 5;

-- Passagens
SELECT id, id_passageiro, id_viagem, sincronizado, excluido, ultima_atualizacao
FROM passagens
LIMIT 5;

-- Viagens
SELECT id, data_viagem, sincronizado, excluido, ultima_atualizacao
FROM viagens
LIMIT 5;

-- 5. VERIFICAR TABELA SYNC_LOG (se existir)
SELECT * FROM sync_log 
ORDER BY data_hora DESC 
LIMIT 10;

-- 6. VERIFICAR TABELA SYNC_CONTROLE (se existir)
SELECT * FROM sync_controle 
ORDER BY ultima_sincronizacao DESC;
