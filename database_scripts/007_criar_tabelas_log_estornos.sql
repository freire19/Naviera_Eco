-- Migration 007: Cria tabelas de log de estornos
-- D021: DDL removido do runtime da aplicacao

BEGIN;

CREATE TABLE IF NOT EXISTS log_estornos_fretes (
    id_log SERIAL PRIMARY KEY,
    id_frete INTEGER NOT NULL,
    valor_estornado DECIMAL(10,2) NOT NULL,
    motivo TEXT,
    forma_devolucao VARCHAR(50),
    id_usuario_autorizou INTEGER,
    nome_autorizador VARCHAR(100),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS log_estornos_encomendas (
    id_log SERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL,
    valor_estornado DECIMAL(10,2) NOT NULL,
    motivo TEXT,
    forma_devolucao VARCHAR(50),
    id_usuario_autorizou INTEGER,
    nome_autorizador VARCHAR(100),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS log_estornos_passagens (
    id_log SERIAL PRIMARY KEY,
    id_passagem INTEGER NOT NULL,
    valor_estornado DECIMAL(10,2) NOT NULL,
    motivo TEXT,
    forma_devolucao VARCHAR(50),
    id_usuario_autorizou INTEGER,
    nome_autorizador VARCHAR(100),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMIT;
