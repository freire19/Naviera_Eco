package dao;

import java.math.BigDecimal;

public final class DAOUtils {
    private DAOUtils() {}

    public static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
