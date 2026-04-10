-- Migration 015: GPS Tracking para embarcações
-- Armazena posições em tempo real enviadas pela tripulação via app/desktop

CREATE TABLE IF NOT EXISTS embarcacao_gps (
    id BIGSERIAL PRIMARY KEY,
    id_embarcacao BIGINT NOT NULL REFERENCES embarcacoes(id_embarcacao),
    id_viagem BIGINT REFERENCES viagens(id_viagem),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    velocidade_nos DOUBLE PRECISION,
    curso_graus DOUBLE PRECISION,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indices para consultas frequentes
CREATE INDEX idx_embarcacao_gps_emb ON embarcacao_gps(id_embarcacao, timestamp DESC);
CREATE INDEX idx_embarcacao_gps_viagem ON embarcacao_gps(id_viagem, timestamp DESC);

-- Limpeza automatica: manter apenas 30 dias de historico (executar via cron ou pg_cron)
-- DELETE FROM embarcacao_gps WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '30 days';
