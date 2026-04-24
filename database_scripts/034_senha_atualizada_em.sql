-- Migration 034 — #234: coluna senha_atualizada_em para invalidar JWTs apos troca de senha.
--   authMiddleware rejeita tokens com iat < senha_atualizada_em.

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS senha_atualizada_em TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuarios_id_senha_at
    ON usuarios(id, senha_atualizada_em);
