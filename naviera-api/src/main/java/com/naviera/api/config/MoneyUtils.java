package com.naviera.api.config;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Set;

public final class MoneyUtils {
    private MoneyUtils() {}

    /** #033: whitelist de forma de pagamento aceita pelos services de pagar(). BARCO = presencial. */
    public static final Set<String> FORMAS_PAGAMENTO_VALIDAS = Set.of("PIX", "CARTAO", "BOLETO", "BARCO");

    /** Valida forma de pagamento contra whitelist. Lanca ApiException.badRequest se invalida. */
    public static String validarFormaPagamento(String forma) {
        String f = forma != null ? forma.trim().toUpperCase() : "PIX";
        if (!FORMAS_PAGAMENTO_VALIDAS.contains(f)) {
            throw ApiException.badRequest("Forma de pagamento invalida: " + forma);
        }
        return f;
    }

    /** #DB209: TZ de negocio — usar em LocalDate.now()/LocalDateTime para evitar drift UTC. */
    public static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

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
