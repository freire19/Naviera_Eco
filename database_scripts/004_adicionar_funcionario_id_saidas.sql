-- Migration: Adicionar funcionario_id em financeiro_saidas
-- Issue #D014: Busca de salario por LIKE no nome e insegura, deve usar FK

ALTER TABLE financeiro_saidas ADD COLUMN IF NOT EXISTS funcionario_id INTEGER;

-- Criar indice para performance
CREATE INDEX IF NOT EXISTS idx_fin_saidas_funcionario ON financeiro_saidas(funcionario_id);

-- Tentar popular retroativamente (match exato por nome no padrao "PAGTO NOME -")
UPDATE financeiro_saidas fs
SET funcionario_id = f.id
FROM funcionarios f
WHERE fs.funcionario_id IS NULL
  AND UPPER(fs.descricao) LIKE 'PAGTO ' || UPPER(f.nome) || ' -%';
