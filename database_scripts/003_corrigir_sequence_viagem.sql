-- Script para verificar e criar a sequence seq_viagem se necessário
-- Execute este script no PostgreSQL para resolver o problema de cadastro de viagens

-- Verificar se a sequence existe
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'seq_viagem') THEN
        -- Se não existe, criar a sequence
        EXECUTE 'CREATE SEQUENCE seq_viagem START WITH ' || 
                COALESCE((SELECT MAX(id_viagem) + 1 FROM viagens), 1);
        RAISE NOTICE 'Sequence seq_viagem criada com sucesso!';
    ELSE
        -- Se existe, sincronizar com o maior ID da tabela
        EXECUTE 'SELECT setval(''seq_viagem'', COALESCE((SELECT MAX(id_viagem) FROM viagens), 1))';
        RAISE NOTICE 'Sequence seq_viagem sincronizada com sucesso!';
    END IF;
END $$;

-- Verificar o valor atual da sequence
SELECT 'seq_viagem' as sequence_name, last_value, is_called FROM seq_viagem;

-- Verificar o maior ID atual na tabela viagens
SELECT 'viagens' as tabela, COALESCE(MAX(id_viagem), 0) as maior_id FROM viagens;