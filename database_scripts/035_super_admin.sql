-- Migration 035 — super_admin flag (fix #100/#114)
-- AUDIT_V1.3: AdminController / SecurityConfig concediam ROLE_ADMIN a qualquer funcao='Administrador',
-- permitindo que admin de uma empresa editasse/ativasse empresas concorrentes via /admin/empresas.
-- Fix: flag super_admin so pode ser TRUE via UPDATE manual (seed). JwtFilter passa a emitir
-- ROLE_SUPERADMIN apenas quando super_admin=TRUE no DB; /admin/** exige ROLE_SUPERADMIN.
-- ROLE_ADMIN continua valido para admin-empresa no escopo /op/** e /psp/**.

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS super_admin BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN usuarios.super_admin IS
    'Super-admin plataforma (nao admin-empresa). Habilita /admin/** cross-tenant. Seed manual via UPDATE.';

-- Indice parcial para lookup rapido de super-admins (populacao esperada: 1-3 usuarios).
CREATE INDEX IF NOT EXISTS idx_usuarios_super_admin
    ON usuarios (id) WHERE super_admin = TRUE;

-- Seed do primeiro super-admin deve ser manual pos-deploy:
--   UPDATE usuarios SET super_admin = TRUE WHERE email = '<email-do-fundador>' AND funcao = 'Administrador';
