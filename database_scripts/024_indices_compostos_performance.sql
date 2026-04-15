-- DP061: Indices compostos para queries frequentes
-- Executar apos 013_multi_tenant.sql

-- Passagens por viagem (usado em listagens, relatorios, balanco)
CREATE INDEX IF NOT EXISTS idx_passagens_empresa_viagem ON passagens(empresa_id, id_viagem);

-- Encomendas por viagem
CREATE INDEX IF NOT EXISTS idx_encomendas_empresa_viagem ON encomendas(empresa_id, id_viagem);

-- Fretes por viagem
CREATE INDEX IF NOT EXISTS idx_fretes_empresa_viagem ON fretes(empresa_id, id_viagem);

-- Boletos pendentes (financeiro_saidas filtrado por forma_pagamento e status)
CREATE INDEX IF NOT EXISTS idx_saidas_boleto_pendente ON financeiro_saidas(empresa_id, forma_pagamento, status)
  WHERE forma_pagamento = 'BOLETO';

-- Agenda por data (usado no calendario)
CREATE INDEX IF NOT EXISTS idx_agenda_empresa_data ON agenda_anotacoes(empresa_id, data_evento, concluida);

-- Viagem ativa (partial index — so indexa as ativas)
CREATE INDEX IF NOT EXISTS idx_viagens_ativa ON viagens(empresa_id) WHERE is_atual = TRUE;
