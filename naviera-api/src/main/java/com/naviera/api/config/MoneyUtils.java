package com.naviera.api.config;

import java.math.BigDecimal;

public final class MoneyUtils {
    private MoneyUtils() {}

    public static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    /**
     * #714: match exato de nome de cliente (trim+lowercase) contra uma lista de candidatos.
     * Usado em fallback legado de ownership (frete/encomenda sem FK id_cliente_app_*).
     * Retorna false se clienteNome for null/blank — caller deve lancar forbidden.
     */
    public static boolean nomeCasaComAlgum(String clienteNome, String... candidatos) {
        if (clienteNome == null || clienteNome.isBlank()) return false;
        String norm = clienteNome.trim().toLowerCase();
        for (String c : candidatos) {
            if (c != null && norm.equals(c.trim().toLowerCase())) return true;
        }
        return false;
    }
}
