-- 015_gps_tracking.sql
-- Tabela dedicada para histórico de posições GPS das embarcações
-- Complementa embarcacao_gps existente com suporte multi-tenant explícito

CREATE TABLE IF NOT EXISTS gps_posicoes (
  id SERIAL PRIMARY KEY,
  id_embarcacao BIGINT REFERENCES embarcacoes(id_embarcacao),
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  velocidade DOUBLE PRECISION,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  empresa_id INTEGER REFERENCES empresas(id)
);
CREATE INDEX IF NOT EXISTS idx_gps_embarcacao ON gps_posicoes(id_embarcacao);
