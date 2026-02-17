-- Script para criar a tabela de log de estornos de fretes
-- Execute este script no banco de dados para habilitar o histórico de estornos de fretes

CREATE TABLE IF NOT EXISTS log_estornos_fretes (
    id_log SERIAL PRIMARY KEY,
    id_frete INTEGER NOT NULL,
    valor_estornado DECIMAL(10,2) NOT NULL,
    motivo TEXT,
    forma_devolucao VARCHAR(50),
    id_usuario_autorizou INTEGER,
    nome_autorizador VARCHAR(100),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_frete) REFERENCES fretes(id_frete)
);

-- Índices para melhorar performance das consultas
CREATE INDEX IF NOT EXISTS idx_log_estornos_fretes_data ON log_estornos_fretes(data_hora);
CREATE INDEX IF NOT EXISTS idx_log_estornos_fretes_frete ON log_estornos_fretes(id_frete);

-- Comentários
COMMENT ON TABLE log_estornos_fretes IS 'Tabela de auditoria para registrar todos os estornos de pagamentos de fretes';
COMMENT ON COLUMN log_estornos_fretes.id_log IS 'Identificador único do registro de log';
COMMENT ON COLUMN log_estornos_fretes.id_frete IS 'ID do frete que teve pagamento estornado';
COMMENT ON COLUMN log_estornos_fretes.valor_estornado IS 'Valor que foi estornado';
COMMENT ON COLUMN log_estornos_fretes.motivo IS 'Motivo do estorno';
COMMENT ON COLUMN log_estornos_fretes.forma_devolucao IS 'Forma de devolução do valor (Dinheiro, Crédito, etc)';
COMMENT ON COLUMN log_estornos_fretes.id_usuario_autorizou IS 'ID do usuário que autorizou o estorno';
COMMENT ON COLUMN log_estornos_fretes.nome_autorizador IS 'Nome do usuário que autorizou o estorno';
COMMENT ON COLUMN log_estornos_fretes.data_hora IS 'Data e hora do estorno';
