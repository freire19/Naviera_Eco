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
}
