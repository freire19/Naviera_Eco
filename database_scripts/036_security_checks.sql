-- ============================================================================
-- 036 — SECURITY CHECKS (DEEP_SECURITY V5.0)
-- ============================================================================
-- Adiciona CHECKs declarativos para fechar issues:
--   #DS5-434 — usuarios.funcao deve estar em set conhecido
--   #DS5-435 — usuarios.senha deve ter formato bcrypt $2a/$2b/$2y$<custo>$<22 char>
--   #DS5-440 — pagamentos_app.tipo_referencia restringido a tipos validos
-- ============================================================================

DO $$ BEGIN
    -- #DS5-434: funcao em conjunto fechado (case-insensitive ja tratado em codigo).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_usuarios_funcao') THEN
        ALTER TABLE usuarios
            ADD CONSTRAINT chk_usuarios_funcao
            CHECK (funcao IS NULL OR UPPER(TRIM(funcao)) IN
                ('ADMIN','ADMINISTRADOR','OPERADOR','CONFERENTE','CAIXA','GERENTE','SUPER_ADMIN'));
    END IF;
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'chk_usuarios_funcao: dados existentes violam o CHECK; pular';
END $$;

DO $$ BEGIN
    -- #DS5-435: senha em formato bcrypt valido.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_usuarios_senha_bcrypt') THEN
        ALTER TABLE usuarios
            ADD CONSTRAINT chk_usuarios_senha_bcrypt
            CHECK (senha IS NULL OR senha ~ '^\$2[aby]\$[0-9]{2}\$[A-Za-z0-9./]{53}$');
    END IF;
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'chk_usuarios_senha_bcrypt: senhas existentes nao bcrypt; rotacionar antes';
END $$;

DO $$ BEGIN
    -- #DS5-440: tipo_referencia em set fechado.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_pagamentos_app_tipo_ref') THEN
        ALTER TABLE pagamentos_app
            ADD CONSTRAINT chk_pagamentos_app_tipo_ref
            CHECK (tipo_referencia IN ('PASSAGEM','ENCOMENDA','FRETE','RECARGA','ASSINATURA'));
    END IF;
EXCEPTION WHEN check_violation THEN
    RAISE NOTICE 'chk_pagamentos_app_tipo_ref: pular se houver legado';
END $$;
