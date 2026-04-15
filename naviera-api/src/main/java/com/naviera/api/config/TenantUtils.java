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
}
