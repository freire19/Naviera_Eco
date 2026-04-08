-- ============================================================================
-- Script 006: Indices de performance criticos (fix DP008)
-- Cria indices para colunas usadas em WHERE, JOIN e ORDER BY
-- Executar apos scripts 001-005
-- ============================================================================

-- Passagens
CREATE INDEX IF NOT EXISTS idx_passagens_id_viagem ON passagens(id_viagem);
CREATE INDEX IF NOT EXISTS idx_passagens_id_passageiro ON passagens(id_passageiro);
CREATE INDEX IF NOT EXISTS idx_passagens_data_emissao ON passagens(data_emissao);
CREATE INDEX IF NOT EXISTS idx_passagens_status ON passagens(status_passagem);

-- Encomendas
CREATE INDEX IF NOT EXISTS idx_encomendas_id_viagem ON encomendas(id_viagem);

-- Fretes
CREATE INDEX IF NOT EXISTS idx_fretes_id_viagem ON fretes(id_viagem);
CREATE INDEX IF NOT EXISTS idx_fretes_data_emissao ON fretes(data_emissao);

-- Frete Itens
CREATE INDEX IF NOT EXISTS idx_frete_itens_id_frete ON frete_itens(id_frete);

-- Viagens
CREATE INDEX IF NOT EXISTS idx_viagens_data_viagem ON viagens(data_viagem);
CREATE INDEX IF NOT EXISTS idx_viagens_is_atual ON viagens(is_atual) WHERE is_atual = true;

-- Passageiros
CREATE INDEX IF NOT EXISTS idx_passageiros_numero_documento ON passageiros(numero_documento);

-- Financeiro Saidas
CREATE INDEX IF NOT EXISTS idx_fin_saidas_data_vencimento ON financeiro_saidas(data_vencimento);
CREATE INDEX IF NOT EXISTS idx_fin_saidas_id_viagem ON financeiro_saidas(id_viagem);
CREATE INDEX IF NOT EXISTS idx_fin_saidas_forma_pagamento ON financeiro_saidas(forma_pagamento);
CREATE INDEX IF NOT EXISTS idx_fin_saidas_funcionario_id ON financeiro_saidas(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_fin_saidas_status ON financeiro_saidas(status);

-- Encomenda Itens (para join no filtro)
CREATE INDEX IF NOT EXISTS idx_encomenda_itens_id_encomenda ON encomenda_itens(id_encomenda);

-- Agenda Anotacoes
CREATE INDEX IF NOT EXISTS idx_agenda_anotacoes_data ON agenda_anotacoes(data_evento);

-- Indice trigram para busca por nome (requer extensao pg_trgm)
-- Descomentar se a extensao estiver habilitada:
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_passageiros_nome_trgm ON passageiros USING gin (nome_passageiro gin_trgm_ops);
