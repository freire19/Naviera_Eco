-- ============================================================================
-- SCHEMA COMPLETO: naviera_eco
-- Gerado a partir dos DAOs e Controllers do projeto Naviera Eco
-- PostgreSQL 12+
-- ============================================================================

-- ============================================================================
-- 1. TABELAS AUXILIARES (sem dependencias)
-- ============================================================================

CREATE TABLE IF NOT EXISTS aux_tipos_documento (
    id_tipo_doc        SERIAL PRIMARY KEY,
    nome_tipo_doc      VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_sexo (
    id_sexo            SERIAL PRIMARY KEY,
    nome_sexo          VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_nacionalidades (
    id_nacionalidade   SERIAL PRIMARY KEY,
    nome_nacionalidade VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_tipos_passagem (
    id_tipo_passagem   SERIAL PRIMARY KEY,
    nome_tipo_passagem VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_agentes (
    id_agente          SERIAL PRIMARY KEY,
    nome_agente        VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_horarios_saida (
    id_horario_saida       SERIAL PRIMARY KEY,
    descricao_horario_saida VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_acomodacoes (
    id_acomodacao      SERIAL PRIMARY KEY,
    nome_acomodacao    VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS aux_formas_pagamento (
    id_forma_pagamento SERIAL PRIMARY KEY,
    nome_forma_pagamento VARCHAR(100) NOT NULL UNIQUE
);

-- ============================================================================
-- 2. TABELAS DE CADASTRO (sem FK entre si)
-- ============================================================================

CREATE TABLE IF NOT EXISTS caixas (
    id_caixa           SERIAL PRIMARY KEY,
    nome_caixa         VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS rotas (
    id                 BIGSERIAL PRIMARY KEY,
    origem             VARCHAR(200) NOT NULL,
    destino            VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS embarcacoes (
    id_embarcacao              BIGSERIAL PRIMARY KEY,
    nome                       VARCHAR(200) NOT NULL UNIQUE,
    registro_capitania         VARCHAR(100),
    capacidade_passageiros     INTEGER,
    capacidade_carga_toneladas NUMERIC(12,2),
    observacoes                TEXT
);

CREATE TABLE IF NOT EXISTS conferentes (
    id_conferente      BIGSERIAL PRIMARY KEY,
    nome_conferente    VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS contatos (
    id                 BIGSERIAL PRIMARY KEY,
    nome_razao_social  VARCHAR(300) NOT NULL
);

CREATE TABLE IF NOT EXISTS cad_clientes_encomenda (
    id_cliente         BIGSERIAL PRIMARY KEY,
    nome_cliente       VARCHAR(300) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categorias_despesa (
    id                 SERIAL PRIMARY KEY,
    nome               VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS tipo_passageiro (
    id                 SERIAL PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    idade_min          INTEGER NOT NULL DEFAULT 0,
    idade_max          INTEGER NOT NULL DEFAULT 999,
    deficiente         BOOLEAN NOT NULL DEFAULT FALSE,
    gratuito           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS configuracao_empresa (
    id_config              INTEGER PRIMARY KEY,
    companhia              VARCHAR(200),
    nome_embarcacao        VARCHAR(200),
    comandante             VARCHAR(200),
    proprietario           VARCHAR(200),
    origem_padrao          VARCHAR(200),
    gerente                VARCHAR(200),
    linha_rio_padrao       VARCHAR(200),
    cnpj                   VARCHAR(20),
    ie                     VARCHAR(30),
    endereco               VARCHAR(300),
    cep                    VARCHAR(15),
    telefone               VARCHAR(30),
    frase_relatorio        TEXT,
    path_logo              VARCHAR(500),
    recomendacoes_bilhete  TEXT
);

-- ============================================================================
-- 3. TABELAS DE ITENS PADRAO (cadastro de produtos/servicos)
-- ============================================================================

CREATE TABLE IF NOT EXISTS itens_frete_padrao (
    id_item_frete          SERIAL PRIMARY KEY,
    nome_item              VARCHAR(200) NOT NULL,
    descricao              TEXT,
    unidade_medida         VARCHAR(20),
    preco_unitario_padrao  NUMERIC(12,2),
    preco_unitario_desconto NUMERIC(12,2),
    ativo                  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS itens_encomenda_padrao (
    id_item_encomenda          BIGSERIAL PRIMARY KEY,
    nome_item                  VARCHAR(200) NOT NULL,
    descricao                  TEXT,
    unidade_medida             VARCHAR(20),
    preco_unitario_padrao      NUMERIC(12,2),
    permite_valor_declarado    BOOLEAN NOT NULL DEFAULT FALSE,
    ativo                      BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================================
-- 4. PASSAGEIROS (referencia tabelas auxiliares)
-- ============================================================================

CREATE TABLE IF NOT EXISTS passageiros (
    id_passageiro          BIGSERIAL PRIMARY KEY,
    nome_passageiro        VARCHAR(300) NOT NULL,
    numero_documento       VARCHAR(50),
    id_tipo_doc            INTEGER REFERENCES aux_tipos_documento(id_tipo_doc),
    data_nascimento        DATE,
    id_sexo                INTEGER REFERENCES aux_sexo(id_sexo),
    id_nacionalidade       INTEGER REFERENCES aux_nacionalidades(id_nacionalidade),
    data_ultima_atualizacao TIMESTAMP
);

-- ============================================================================
-- 5. VIAGENS (referencia embarcacoes, rotas, aux_horarios_saida)
-- ============================================================================

CREATE TABLE IF NOT EXISTS viagens (
    id_viagem          BIGINT PRIMARY KEY,
    data_viagem        DATE NOT NULL,
    id_horario_saida   BIGINT REFERENCES aux_horarios_saida(id_horario_saida),
    data_chegada       DATE,
    descricao          TEXT,
    ativa              BOOLEAN NOT NULL DEFAULT TRUE,
    is_atual           BOOLEAN NOT NULL DEFAULT FALSE,
    id_embarcacao      BIGINT NOT NULL REFERENCES embarcacoes(id_embarcacao),
    id_rota            BIGINT NOT NULL REFERENCES rotas(id)
);

-- ============================================================================
-- 6. TARIFAS (referencia rotas, aux_tipos_passagem)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tarifas (
    id_tarifa          SERIAL PRIMARY KEY,
    id_rota            BIGINT NOT NULL REFERENCES rotas(id),
    id_tipo_passagem   INTEGER NOT NULL REFERENCES aux_tipos_passagem(id_tipo_passagem),
    valor_transporte   NUMERIC(12,2) DEFAULT 0,
    valor_alimentacao  NUMERIC(12,2) DEFAULT 0,
    valor_cargas       NUMERIC(12,2) DEFAULT 0,
    valor_desconto     NUMERIC(12,2) DEFAULT 0
);

-- ============================================================================
-- 7. PASSAGENS (referencia passageiros, viagens, rotas, auxiliares, caixas)
-- ============================================================================

CREATE TABLE IF NOT EXISTS passagens (
    id_passagem             BIGSERIAL PRIMARY KEY,
    numero_bilhete          VARCHAR(20) NOT NULL,
    id_passageiro           BIGINT NOT NULL REFERENCES passageiros(id_passageiro),
    id_viagem               BIGINT NOT NULL REFERENCES viagens(id_viagem),
    data_emissao            DATE,
    assento                 VARCHAR(20),
    id_acomodacao           INTEGER REFERENCES aux_acomodacoes(id_acomodacao),
    id_rota                 BIGINT REFERENCES rotas(id),
    id_tipo_passagem        INTEGER REFERENCES aux_tipos_passagem(id_tipo_passagem),
    id_agente               INTEGER REFERENCES aux_agentes(id_agente),
    numero_requisicao       VARCHAR(100),
    valor_alimentacao       NUMERIC(12,2) DEFAULT 0,
    valor_transporte        NUMERIC(12,2) DEFAULT 0,
    valor_cargas            NUMERIC(12,2) DEFAULT 0,
    valor_desconto_tarifa   NUMERIC(12,2) DEFAULT 0,
    valor_total             NUMERIC(12,2) DEFAULT 0,
    valor_desconto_geral    NUMERIC(12,2) DEFAULT 0,
    valor_a_pagar           NUMERIC(12,2) DEFAULT 0,
    valor_pago              NUMERIC(12,2) DEFAULT 0,
    troco                   NUMERIC(12,2) DEFAULT 0,
    valor_devedor           NUMERIC(12,2) DEFAULT 0,
    id_forma_pagamento      INTEGER REFERENCES aux_formas_pagamento(id_forma_pagamento),
    id_caixa                INTEGER REFERENCES caixas(id_caixa),
    id_usuario_emissor      INTEGER,
    status_passagem         VARCHAR(30),
    observacoes             TEXT,
    id_horario_saida        INTEGER REFERENCES aux_horarios_saida(id_horario_saida),
    valor_pagamento_dinheiro NUMERIC(12,2) DEFAULT 0,
    valor_pagamento_pix     NUMERIC(12,2) DEFAULT 0,
    valor_pagamento_cartao  NUMERIC(12,2) DEFAULT 0
);

-- ============================================================================
-- 8. ENCOMENDAS (referencia viagens)
-- ============================================================================

CREATE TABLE IF NOT EXISTS encomendas (
    id_encomenda       BIGSERIAL PRIMARY KEY,
    id_viagem          BIGINT NOT NULL REFERENCES viagens(id_viagem),
    numero_encomenda   VARCHAR(50),
    remetente          VARCHAR(300),
    destinatario       VARCHAR(300),
    observacoes        TEXT,
    total_volumes      INTEGER DEFAULT 0,
    total_a_pagar      NUMERIC(12,2) DEFAULT 0,
    valor_pago         NUMERIC(12,2) DEFAULT 0,
    desconto           NUMERIC(12,2) DEFAULT 0,
    status_pagamento   VARCHAR(30),
    forma_pagamento    VARCHAR(50),
    local_pagamento    VARCHAR(100),
    entregue           BOOLEAN NOT NULL DEFAULT FALSE,
    doc_recebedor      VARCHAR(100),
    nome_recebedor     VARCHAR(300),
    rota               VARCHAR(200),
    id_caixa           INTEGER REFERENCES caixas(id_caixa),
    data_lancamento    DATE
);

-- ============================================================================
-- 9. ENCOMENDA_ITENS (referencia encomendas)
-- ============================================================================

CREATE TABLE IF NOT EXISTS encomenda_itens (
    id_item_encomenda  BIGSERIAL PRIMARY KEY,
    id_encomenda       BIGINT NOT NULL REFERENCES encomendas(id_encomenda),
    quantidade         INTEGER NOT NULL DEFAULT 1,
    descricao          VARCHAR(300),
    valor_unitario     NUMERIC(12,2) DEFAULT 0,
    valor_total        NUMERIC(12,2) DEFAULT 0,
    local_armazenamento VARCHAR(200)
);

-- ============================================================================
-- 10. FRETES (referencia viagens)
-- ============================================================================

CREATE TABLE IF NOT EXISTS fretes (
    id_frete               BIGINT PRIMARY KEY,
    numero_frete           BIGINT,
    data_emissao           DATE,
    data_saida_viagem      DATE,
    local_transporte       VARCHAR(200),
    remetente_nome_temp    VARCHAR(300),
    destinatario_nome_temp VARCHAR(300),
    rota_temp              VARCHAR(200),
    conferente_temp        VARCHAR(200),
    cidade_cobranca        VARCHAR(200),
    observacoes            TEXT,
    num_notafiscal         VARCHAR(50),
    valor_notafiscal       NUMERIC(12,2) DEFAULT 0,
    peso_notafiscal        NUMERIC(12,2) DEFAULT 0,
    valor_total_itens      NUMERIC(12,2) DEFAULT 0,
    desconto               NUMERIC(12,2) DEFAULT 0,
    valor_frete_calculado  NUMERIC(12,2) DEFAULT 0,
    valor_pago             NUMERIC(12,2) DEFAULT 0,
    troco                  NUMERIC(12,2) DEFAULT 0,
    valor_devedor          NUMERIC(12,2) DEFAULT 0,
    tipo_pagamento         VARCHAR(50),
    nome_caixa             VARCHAR(100),
    status_frete           VARCHAR(30),
    id_viagem              BIGINT REFERENCES viagens(id_viagem),
    excluido               BOOLEAN DEFAULT FALSE
);

-- ============================================================================
-- 11. FRETE_ITENS (referencia fretes)
-- ============================================================================

CREATE TABLE IF NOT EXISTS frete_itens (
    id_item_frete          BIGSERIAL PRIMARY KEY,
    id_frete               BIGINT NOT NULL REFERENCES fretes(id_frete),
    nome_item_ou_id_produto VARCHAR(300),
    quantidade             INTEGER NOT NULL DEFAULT 1,
    preco_unitario         NUMERIC(12,2) DEFAULT 0,
    subtotal_item          NUMERIC(12,2) DEFAULT 0
);

-- ============================================================================
-- 12. FUNCIONARIOS
-- ============================================================================

CREATE TABLE IF NOT EXISTS funcionarios (
    id                     SERIAL PRIMARY KEY,
    nome                   VARCHAR(300) NOT NULL,
    cpf                    VARCHAR(20),
    rg                     VARCHAR(30),
    ctps                   VARCHAR(30),
    telefone               VARCHAR(30),
    endereco               VARCHAR(500),
    cargo                  VARCHAR(100),
    salario                NUMERIC(12,2) DEFAULT 0,
    data_admissao          DATE,
    data_nascimento        DATE,
    data_inicio_calculo    DATE,
    recebe_decimo_terceiro BOOLEAN NOT NULL DEFAULT FALSE,
    is_clt                 BOOLEAN NOT NULL DEFAULT FALSE,
    valor_inss             NUMERIC(12,2) DEFAULT 0,
    descontar_inss         BOOLEAN NOT NULL DEFAULT FALSE,
    ativo                  BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================================
-- 13. FINANCEIRO_SAIDAS (despesas/boletos — referencia categorias_despesa, viagens)
-- ============================================================================

CREATE TABLE IF NOT EXISTS financeiro_saidas (
    id                 SERIAL PRIMARY KEY,
    descricao          VARCHAR(500),
    valor_total        NUMERIC(12,2) DEFAULT 0,
    valor_pago         NUMERIC(12,2) DEFAULT 0,
    data_vencimento    DATE,
    data_pagamento     DATE,
    status             VARCHAR(30),
    forma_pagamento    VARCHAR(50),
    id_categoria       INTEGER REFERENCES categorias_despesa(id),
    id_viagem          BIGINT REFERENCES viagens(id_viagem),
    funcionario_id     INTEGER REFERENCES funcionarios(id),
    numero_parcela     INTEGER,
    total_parcelas     INTEGER,
    observacoes        TEXT,
    is_excluido        BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_exclusao    TEXT
);

-- ============================================================================
-- 14. EVENTOS_RH (referencia funcionarios)
-- ============================================================================

CREATE TABLE IF NOT EXISTS eventos_rh (
    id                 BIGSERIAL PRIMARY KEY,
    funcionario_id     INTEGER NOT NULL REFERENCES funcionarios(id),
    tipo               VARCHAR(50) NOT NULL,
    descricao          VARCHAR(500),
    valor              NUMERIC(12,2) DEFAULT 0,
    data_evento        DATE NOT NULL,
    data_referencia    DATE
);

-- ============================================================================
-- 15. RECIBOS AVULSOS (referencia viagens)
-- ============================================================================

CREATE TABLE IF NOT EXISTS recibos_avulsos (
    id_recibo          SERIAL PRIMARY KEY,
    id_viagem          INTEGER NOT NULL,
    nome_pagador       VARCHAR(300),
    referente_a        TEXT,
    valor              NUMERIC(12,2) DEFAULT 0,
    data_emissao       DATE,
    tipo_recibo        VARCHAR(50) DEFAULT 'PADRAO'
);

-- ============================================================================
-- 16. HISTORICO RECIBO QUITACAO PASSAGEIRO
-- ============================================================================

CREATE TABLE IF NOT EXISTS historico_recibo_quitacao_passageiro (
    id                 SERIAL PRIMARY KEY,
    nome_passageiro    VARCHAR(300),
    data_pagamento     TIMESTAMP,
    valor_total        NUMERIC(12,2) DEFAULT 0,
    forma_pagamento    VARCHAR(50),
    itens_pagos        TEXT
);

-- ============================================================================
-- 17. AGENDA / ANOTACOES
-- ============================================================================

CREATE TABLE IF NOT EXISTS agenda_anotacoes (
    id_anotacao        SERIAL PRIMARY KEY,
    data_evento        DATE NOT NULL,
    descricao          TEXT,
    concluida          BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================================
-- 18. AUDITORIA FINANCEIRO
-- ============================================================================

CREATE TABLE IF NOT EXISTS auditoria_financeiro (
    id                     SERIAL PRIMARY KEY,
    acao                   VARCHAR(100),
    tipo_operacao          VARCHAR(100),
    descricao              TEXT,
    usuario                VARCHAR(200),
    usuario_solicitante    VARCHAR(200),
    motivo                 TEXT,
    detalhe_valor          TEXT,
    id_viagem              INTEGER,
    data_hora              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 19. SEQUENCES
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS seq_numero_bilhete START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_numero_encomenda START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_viagem START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_rota START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_conferente START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_numero_frete START 1 INCREMENT 1;

-- ============================================================================
-- 20. INDICES DE PERFORMANCE
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_passagens_id_viagem ON passagens(id_viagem);
CREATE INDEX IF NOT EXISTS idx_passagens_id_passageiro ON passagens(id_passageiro);
CREATE INDEX IF NOT EXISTS idx_passagens_data_emissao ON passagens(data_emissao);
CREATE INDEX IF NOT EXISTS idx_passagens_numero_bilhete ON passagens(numero_bilhete);

CREATE INDEX IF NOT EXISTS idx_encomendas_id_viagem ON encomendas(id_viagem);
CREATE INDEX IF NOT EXISTS idx_encomenda_itens_id_encomenda ON encomenda_itens(id_encomenda);

CREATE INDEX IF NOT EXISTS idx_fretes_id_viagem ON fretes(id_viagem);
CREATE INDEX IF NOT EXISTS idx_fretes_numero_frete ON fretes(numero_frete);
CREATE INDEX IF NOT EXISTS idx_frete_itens_id_frete ON frete_itens(id_frete);

CREATE INDEX IF NOT EXISTS idx_viagens_data_viagem ON viagens(data_viagem);
CREATE INDEX IF NOT EXISTS idx_viagens_is_atual ON viagens(is_atual) WHERE is_atual = TRUE;

CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_id_viagem ON financeiro_saidas(id_viagem);
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_data_vencimento ON financeiro_saidas(data_vencimento);
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_funcionario_id ON financeiro_saidas(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_financeiro_saidas_forma_pagamento ON financeiro_saidas(forma_pagamento);

CREATE INDEX IF NOT EXISTS idx_eventos_rh_funcionario_id ON eventos_rh(funcionario_id);
CREATE INDEX IF NOT EXISTS idx_eventos_rh_data_referencia ON eventos_rh(data_referencia);

CREATE INDEX IF NOT EXISTS idx_agenda_anotacoes_data_evento ON agenda_anotacoes(data_evento);

CREATE INDEX IF NOT EXISTS idx_passageiros_nome ON passageiros(nome_passageiro);

CREATE INDEX IF NOT EXISTS idx_recibos_avulsos_id_viagem ON recibos_avulsos(id_viagem);

CREATE INDEX IF NOT EXISTS idx_tarifas_rota_tipo ON tarifas(id_rota, id_tipo_passagem);
