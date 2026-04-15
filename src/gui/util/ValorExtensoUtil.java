package gui.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Converte valores monetarios (double) em texto por extenso em portugues.
 * Ex: 1234.56 → "Um Mil, Duzentos e Trinta e Quatro Reais e Cinquenta e Seis Centavos"
 *
 * Extraido de GerarReciboAvulsoController (inner class) para reuso.
 */
public class ValorExtensoUtil {
    private static final String[] UNIDADES = {"", "Um", "Dois", "Três", "Quatro", "Cinco", "Seis", "Sete", "Oito", "Nove", "Dez", "Onze", "Doze", "Treze", "Quatorze", "Quinze", "Dezesseis", "Dezessete", "Dezoito", "Dezenove"};
    private static final String[] DEZENAS = {"", "", "Vinte", "Trinta", "Quarenta", "Cinquenta", "Sessenta", "Setenta", "Oitenta", "Noventa"};
    private static final String[] CENTENAS = {"", "Cento", "Duzentos", "Trezentos", "Quatrocentos", "Quinhentos", "Seiscentos", "Setecentos", "Oitocentos", "Novecentos"};

    public static String valorPorExtenso(double v) {
        if (v == 0) return "Zero Reais";
        BigDecimal bd = new BigDecimal(v).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        BigInteger inteiro = bd.toBigInteger();
        BigInteger centavos = bd.subtract(new BigDecimal(inteiro)).multiply(new BigDecimal(100)).toBigInteger();
        String ret = "";
        if (inteiro.compareTo(BigInteger.ZERO) > 0) {
            ret = converter(inteiro) + (inteiro.compareTo(BigInteger.ONE) == 0 ? " Real" : " Reais");
        }
        if (centavos.compareTo(BigInteger.ZERO) > 0) {
            if (!ret.isEmpty()) ret += " e ";
            ret += converter(centavos) + (centavos.compareTo(BigInteger.ONE) == 0 ? " Centavo" : " Centavos");
        }
        return ret;
    }

    private static String converter(BigInteger n) {
        if (n.compareTo(new BigInteger("1000")) < 0) return converterAte999(n.intValue());
        if (n.compareTo(new BigInteger("1000000")) < 0) {
            int milhar = n.divide(new BigInteger("1000")).intValue();
            int resto = n.remainder(new BigInteger("1000")).intValue();
            String sMilhar = (milhar == 1 ? "Um Mil" : converterAte999(milhar) + " Mil");
            if (resto == 0) return sMilhar;
            if (resto <= 100 || resto % 100 == 0) return sMilhar + " e " + converterAte999(resto);
            return sMilhar + ", " + converterAte999(resto);
        }
        return n.toString();
    }

    private static String converterAte999(int n) {
        if (n == 0) return "";
        if (n == 100) return "Cem";
        if (n < 20) return UNIDADES[n];
        if (n < 100) return DEZENAS[n / 10] + (n % 10 != 0 ? " e " + UNIDADES[n % 10] : "");
        return CENTENAS[n / 100] + (n % 100 != 0 ? " e " + converterAte999(n % 100) : "");
    }
}
