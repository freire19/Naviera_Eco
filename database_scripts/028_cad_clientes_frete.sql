-- Migration 028: Tabela separada de clientes de frete + normalizacao uppercase
-- Data: 2026-04-16
-- Contexto: Cada modulo (frete, encomenda, passagem) tem tabela de clientes separada.

-- 1. Criar tabela cad_clientes_frete (separada de cad_clientes_encomenda)
CREATE TABLE IF NOT EXISTS cad_clientes_frete (
    id_cliente BIGSERIAL PRIMARY KEY,
    nome_cliente VARCHAR(300) NOT NULL,
    razao_social VARCHAR(300),
    cpf_cnpj VARCHAR(20),
    endereco VARCHAR(500),
    inscricao_estadual VARCHAR(30),
    email VARCHAR(200),
    telefone VARCHAR(30),
    empresa_id BIGINT NOT NULL,
    UNIQUE(empresa_id, nome_cliente)
);

-- 2. Popular com remetentes e destinatarios unicos dos fretes existentes
INSERT INTO cad_clientes_frete (nome_cliente, empresa_id)
SELECT DISTINCT nome, empresa_id FROM (
    SELECT UPPER(TRIM(remetente_nome_temp)) AS nome, empresa_id FROM fretes WHERE remetente_nome_temp IS NOT NULL AND remetente_nome_temp != ''
    UNION
    SELECT UPPER(TRIM(destinatario_nome_temp)) AS nome, empresa_id FROM fretes WHERE destinatario_nome_temp IS NOT NULL AND destinatario_nome_temp != ''
) sub
WHERE nome IS NOT NULL AND nome != ''
ON CONFLICT (empresa_id, nome_cliente) DO NOTHING;

-- 3. Normalizar todos os dados existentes para UPPERCASE
UPDATE itens_frete_padrao SET nome_item = UPPER(nome_item) WHERE nome_item != UPPER(nome_item);
UPDATE itens_encomenda_padrao SET nome_item = UPPER(nome_item) WHERE nome_item != UPPER(nome_item);
UPDATE frete_itens SET nome_item_ou_id_produto = UPPER(nome_item_ou_id_produto) WHERE nome_item_ou_id_produto != UPPER(nome_item_ou_id_produto);
UPDATE encomenda_itens SET descricao = UPPER(descricao) WHERE descricao != UPPER(descricao);
UPDATE fretes SET remetente_nome_temp = UPPER(remetente_nome_temp) WHERE remetente_nome_temp != UPPER(remetente_nome_temp);
UPDATE fretes SET destinatario_nome_temp = UPPER(destinatario_nome_temp) WHERE destinatario_nome_temp != UPPER(destinatario_nome_temp);
UPDATE encomendas SET remetente = UPPER(remetente) WHERE remetente != UPPER(remetente);
UPDATE encomendas SET destinatario = UPPER(destinatario) WHERE destinatario != UPPER(destinatario);
UPDATE passageiros SET nome_passageiro = UPPER(nome_passageiro) WHERE nome_passageiro != UPPER(nome_passageiro);
UPDATE cad_clientes_encomenda SET nome_cliente = UPPER(nome_cliente) WHERE nome_cliente != UPPER(nome_cliente);

-- 4. Regra desconto estiva/fardaria: R$3.80 -> R$3.60, R$4.50 -> R$4.30
UPDATE itens_frete_padrao SET preco_unitario_desconto = 3.60 WHERE preco_unitario_padrao = 3.80 AND (preco_unitario_desconto = 3.80 OR preco_unitario_desconto IS NULL OR preco_unitario_desconto = 0);
UPDATE itens_frete_padrao SET preco_unitario_desconto = 4.30 WHERE preco_unitario_padrao = 4.50 AND (preco_unitario_desconto = 4.50 OR preco_unitario_desconto IS NULL OR preco_unitario_desconto = 0);
-- Demais itens sem desconto definido: desconto = normal
UPDATE itens_frete_padrao SET preco_unitario_desconto = preco_unitario_padrao WHERE preco_unitario_padrao NOT IN (3.80, 4.50) AND (preco_unitario_desconto IS NULL OR preco_unitario_desconto = 0);
