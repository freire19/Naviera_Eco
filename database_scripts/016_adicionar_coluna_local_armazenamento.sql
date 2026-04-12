-- Script para adicionar a coluna local_armazenamento na tabela encomenda_itens
-- Execute este script no seu banco de dados PostgreSQL

-- Verifica se a coluna já existe antes de adicionar
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'encomenda_itens' 
        AND column_name = 'local_armazenamento'
    ) THEN
        ALTER TABLE encomenda_itens 
        ADD COLUMN local_armazenamento VARCHAR(100);
        
        RAISE NOTICE 'Coluna local_armazenamento adicionada com sucesso!';
    ELSE
        RAISE NOTICE 'Coluna local_armazenamento já existe.';
    END IF;
END $$;
