package gui.util;

import java.math.BigDecimal;

/**
 * Utilitario centralizado para parsing e formatacao de valores monetarios.
 * Substitui as 4+ copias de parseBigDecimal() espalhadas pelos controllers.
 */
public class MoneyUtil {

    /**
     * Converte texto monetario brasileiro para BigDecimal.
     * Aceita formatos: "R$ 1.234,56", "1234,56", "1234.56", "R$1.234,56"
     * Retorna BigDecimal.ZERO se texto for nulo ou vazio.
     * Lanca NumberFormatException se texto nao for um valor valido (DL025).
     */
    public static BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        String cleaned = text
                .replace("R$", "")
                .replace(" ", "")
                .replace(".", "")  // remove separador de milhar
                .replace(",", ".") // virgula decimal → ponto
                .trim();
        return new BigDecimal(cleaned); // propaga excecao para o caller tratar
    }

    /**
     * Versao segura que retorna BigDecimal.ZERO em caso de erro.
     * Usar apenas em contextos onde 0 e um fallback aceitavel (ex: filtros).
     */
    public static BigDecimal parseBigDecimalSafe(String text) {
        try {
            return parseBigDecimal(text);
        } catch (NumberFormatException e) {
            System.err.println("Valor monetario invalido: " + text);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Converte texto monetario para double (para contextos legados que ainda usam double).
     */
    public static double parseDouble(String text) {
        return parseBigDecimal(text).doubleValue();
    }

    /**
     * Formata valor como moeda brasileira: R$ 1.234,56
     */
    public static String formatar(BigDecimal valor) {
        if (valor == null) return "R$ 0,00";
        return String.format("R$ %,.2f", valor);
    }

    /**
     * Formata valor double como moeda brasileira.
     */
    public static String formatar(double valor) {
        return String.format("R$ %,.2f", valor);
    }
}
