package tests;

import model.StatusPagamento;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

/**
 * DR028: Testes unitarios para StatusPagamento.
 * Verifica logica de calculo de status de pagamento.
 */
public class StatusPagamentoTest {

    @Test
    public void calcular_pagamentoCompleto_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(new BigDecimal("100.00"), new BigDecimal("100.00")));
    }

    @Test
    public void calcular_pagamentoParcial_PARCIAL() {
        assertEquals(StatusPagamento.PARCIAL,
            StatusPagamento.calcular(new BigDecimal("50.00"), new BigDecimal("100.00")));
    }

    @Test
    public void calcular_semPagamento_PENDENTE() {
        assertEquals(StatusPagamento.PENDENTE,
            StatusPagamento.calcular(BigDecimal.ZERO, new BigDecimal("100.00")));
    }

    @Test
    public void calcular_pagamentoNulo_PENDENTE() {
        assertEquals(StatusPagamento.PENDENTE,
            StatusPagamento.calcular(null, new BigDecimal("100.00")));
    }

    @Test
    public void calcular_totalNulo_PAGO() {
        // Se total e null (zero), qualquer pagamento cobre
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(BigDecimal.ZERO, null));
    }

    @Test
    public void calcular_diferençaDentroTolerancia_PAGO() {
        // Diferenca de 0.005 < tolerancia 0.01 = PAGO
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(new BigDecimal("99.995"), new BigDecimal("100.00")));
    }

    @Test
    public void calcular_diferençaForaTolerancia_PARCIAL() {
        // Diferenca de 0.02 > tolerancia 0.01 = PARCIAL (ja pagou algo)
        assertEquals(StatusPagamento.PARCIAL,
            StatusPagamento.calcular(new BigDecimal("99.98"), new BigDecimal("100.00")));
    }

    @Test
    public void calcularPorSaldo_saldoZero_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcularPorSaldo(BigDecimal.ZERO, new BigDecimal("100.00")));
    }

    @Test
    public void calcularPorSaldo_saldoPositivoComPagamento_PARCIAL() {
        assertEquals(StatusPagamento.PARCIAL,
            StatusPagamento.calcularPorSaldo(new BigDecimal("50.00"), new BigDecimal("50.00")));
    }

    @Test
    public void calcularPorSaldo_saldoPositivoSemPagamento_PENDENTE() {
        assertEquals(StatusPagamento.PENDENTE,
            StatusPagamento.calcularPorSaldo(new BigDecimal("100.00"), BigDecimal.ZERO));
    }

    @Test
    public void fromString_valoresValidos() {
        assertEquals(StatusPagamento.PAGO, StatusPagamento.fromString("PAGO"));
        assertEquals(StatusPagamento.PENDENTE, StatusPagamento.fromString("PENDENTE"));
        assertEquals(StatusPagamento.PARCIAL, StatusPagamento.fromString("PARCIAL"));
        assertEquals(StatusPagamento.CANCELADA, StatusPagamento.fromString("CANCELADA"));
    }

    @Test
    public void fromString_caseInsensitive() {
        assertEquals(StatusPagamento.PAGO, StatusPagamento.fromString("pago"));
        assertEquals(StatusPagamento.PARCIAL, StatusPagamento.fromString("Parcial"));
    }

    @Test
    public void fromString_nulo_retornaPENDENTE() {
        assertEquals(StatusPagamento.PENDENTE, StatusPagamento.fromString(null));
    }

    @Test
    public void fromString_vazio_retornaPENDENTE() {
        assertEquals(StatusPagamento.PENDENTE, StatusPagamento.fromString(""));
    }

    @Test
    public void fromString_valorDesconhecido_retornaPENDENTE() {
        assertEquals(StatusPagamento.PENDENTE, StatusPagamento.fromString("INVALIDO"));
    }

    @Test
    public void calcular_ambosZero_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    public void calcular_ambosNull_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(null, null));
    }

    @Test
    public void calcularPorSaldo_saldoNull_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcularPorSaldo(null, new BigDecimal("50.00")));
    }

    @Test
    public void calcularPorSaldo_ambosNull_PAGO() {
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcularPorSaldo(null, null));
    }

    @Test
    public void calcular_pagamentoExcedente_PAGO() {
        // Pagou mais que o total — deve ser PAGO
        assertEquals(StatusPagamento.PAGO,
            StatusPagamento.calcular(new BigDecimal("150.00"), new BigDecimal("100.00")));
    }

    @Test
    public void calcularDouble_compatibilidade() {
        assertEquals(StatusPagamento.PAGO, StatusPagamento.calcular(100.0, 100.0));
        assertEquals(StatusPagamento.PARCIAL, StatusPagamento.calcular(50.0, 100.0));
        assertEquals(StatusPagamento.PENDENTE, StatusPagamento.calcular(0.0, 100.0));
    }
}
