package com.naviera.api.config;

import java.math.BigDecimal;

public final class MoneyUtils {
    private MoneyUtils() {}

    public static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }
}
