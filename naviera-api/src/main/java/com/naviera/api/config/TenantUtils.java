package com.naviera.api.config;

import org.springframework.security.core.Authentication;
import java.util.Map;

public class TenantUtils {

    @SuppressWarnings("unchecked")
    public static Integer getEmpresaId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof Map) {
            Object eid = ((Map<String, Object>) details).get("empresa_id");
            if (eid instanceof Integer) return (Integer) eid;
            if (eid != null) return Integer.valueOf(eid.toString());
        }
        throw ApiException.badRequest("empresa_id nao encontrado no token");
    }

    /**
     * Retorna empresa_id do operador ou null para usuarios app (CPF/CNPJ).
     * DS4-008: permite filtro condicional — operador ve so sua empresa, app user ve cross-tenant.
     */
    @SuppressWarnings("unchecked")
    public static Integer getEmpresaIdOrNull(Authentication auth) {
        if (auth == null || auth.getDetails() == null) return null;
        Object details = auth.getDetails();
        if (details instanceof Map) {
            Object eid = ((Map<String, Object>) details).get("empresa_id");
            if (eid instanceof Integer) return (Integer) eid;
            if (eid != null) {
                try { return Integer.valueOf(eid.toString()); } catch (NumberFormatException e) { return null; }
            }
        }
        return null;
    }

    /** Retorna id do operador/usuario logado (do JWT claim `id`). */
    public static Integer getOperadorId(Authentication auth) {
        if (auth == null) throw ApiException.unauthorized("Nao autenticado");
        Object principal = auth.getPrincipal();
        if (principal instanceof Number) return ((Number) principal).intValue();
        if (principal != null) {
            try { return Integer.valueOf(principal.toString()); } catch (NumberFormatException ignored) {}
        }
        throw ApiException.unauthorized("operador_id nao encontrado no token");
    }

    /** True se o operador logado tem ROLE_ADMIN (JwtFilter atribui quando funcao=ADMIN/Administrador). */
    public static boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** True apenas se usuarios.super_admin=TRUE no DB (flag cross-tenant para /admin/**). Fix #100/#114. */
    public static boolean isSuperAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority()));
    }
}
