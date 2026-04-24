-- Migration 033 — #221: unique constraint composto (uuid, empresa_id)
--   Motivo: tenants diferentes podem gerar UUIDs identicos (baixa probabilidade,
--   mas um dump restore ou bug no gerador pode causar colisao global). Unique
--   composto permite ON CONFLICT (uuid, empresa_id) em INSERT multi-tenant.
--
-- AVISO: cada CREATE UNIQUE INDEX sem CONCURRENTLY pega SHARE lock e bloqueia
-- writes na tabela. Em tabelas grandes (passagens, viagens, fretes) isso trava
-- operacoes por segundos. `CONCURRENTLY` nao e permitido dentro de bloco DO $$
-- ou transaction. Se executar em producao com trafego ativo, rodar cada
-- statement fora do DO$$, um por vez, com CREATE UNIQUE INDEX CONCURRENTLY
-- (manualmente ou via migration-runner que auto-commit por statement).

DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN
        SELECT unnest(ARRAY[
            'passageiros','passagens','viagens','encomendas','encomenda_itens',
            'fretes','itens_frete','cad_clientes_encomenda','caixas','conferentes',
            'embarcacoes','financeiro_saidas','rotas','tarifas'
        ])
    LOOP
        EXECUTE format(
            'CREATE UNIQUE INDEX IF NOT EXISTS uq_%I_uuid_empresa ON %I (uuid, empresa_id) WHERE uuid IS NOT NULL',
            t, t
        );
    END LOOP;
END$$;
