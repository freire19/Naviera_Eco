package tests;

import gui.util.MoneyUtil;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

/**
 * DR028: Testes unitarios para MoneyUtil.
 * Verifica parsing de valores monetarios brasileiros e edge cases.
 */
public class MoneyUtilTest {

    @Test
    public void parseBigDecimal_formatoBrasileiro() {
        assertEquals(new BigDecimal("1234.56"), MoneyUtil.parseBigDecimal("1.234,56"));
    }

    @Test
    public void parseBigDecimal_comPrefixoRS() {
        assertEquals(new BigDecimal("1234.56"), MoneyUtil.parseBigDecimal("R$ 1.234,56"));
    }

    @Test
    public void parseBigDecimal_semSeparadorMilhar() {
        assertEquals(new BigDecimal("234.56"), MoneyUtil.parseBigDecimal("234,56"));
    }

    @Test
    public void parseBigDecimal_pontoComoDecimal() {
        // Sem virgula — ponto isolado e tratado como milhar (removido)
        // "1234.56" → remove ponto → "123456"
        assertEquals(new BigDecimal("123456"), MoneyUtil.parseBigDecimal("1234.56"));
    }

    @Test
    public void parseBigDecimal_inteiro() {
        assertEquals(new BigDecimal("100"), MoneyUtil.parseBigDecimal("100"));
    }

    @Test
    public void parseBigDecimal_nulo_retornaZero() {
        assertEquals(BigDecimal.ZERO, MoneyUtil.parseBigDecimal(null));
    }

    @Test
    public void parseBigDecimal_vazio_retornaZero() {
        assertEquals(BigDecimal.ZERO, MoneyUtil.parseBigDecimal(""));
    }

    @Test
    public void parseBigDecimal_apenasEspacos_retornaZero() {
        assertEquals(BigDecimal.ZERO, MoneyUtil.parseBigDecimal("   "));
    }

    @Test(expected = NumberFormatException.class)
    public void parseBigDecimal_textoInvalido_lancaExcecao() {
        MoneyUtil.parseBigDecimal("abc");
    }

    @Test
    public void parseBigDecimalSafe_textoInvalido_retornaZero() {
        assertEquals(BigDecimal.ZERO, MoneyUtil.parseBigDecimalSafe("abc"));
    }

    @Test
    public void parseBigDecimalSafe_valorValido() {
        assertEquals(new BigDecimal("50.00"), MoneyUtil.parseBigDecimalSafe("50,00"));
    }

    @Test
    public void formatar_valorPositivo() {
        String result = MoneyUtil.formatar(new BigDecimal("1234.56"));
        assertTrue("Deve conter R$", result.contains("R$"));
        assertTrue("Deve conter valor formatado", result.contains("1.234,56") || result.contains("1,234.56"));
    }

    @Test
    public void formatar_nulo_retornaZero() {
        assertEquals("R$ 0,00", MoneyUtil.formatar((BigDecimal) null));
    }

    @Test
    public void formatar_zero() {
        String result = MoneyUtil.formatar(BigDecimal.ZERO);
        assertTrue(result.contains("0,00") || result.contains("0.00"));
    }

    @Test
    public void parseDouble_formatoBrasileiro() {
        assertEquals(50.0, MoneyUtil.parseDouble("50,00"), 0.001);
    }

    @Test
    public void parseBigDecimal_valorGrande() {
        BigDecimal result = MoneyUtil.parseBigDecimal("R$ 999.999.999,99");
        assertEquals(new BigDecimal("999999999.99"), result);
    }
}
