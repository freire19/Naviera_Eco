package model;

import java.math.BigDecimal;

/**
 * Enum centralizado para status de pagamento.
 * Substitui as magic strings "PAGO", "PENDENTE", "PARCIAL" etc espalhadas pelo projeto.
 */
public enum StatusPagamento {
    PAGO,
    PENDENTE,
    PARCIAL,
    QUITADO,
    NAO_PAGO,
    EMITIDA,
    CANCELADA;

    /** Tolerancia padrao para comparacoes de pagamento (centavo). */
    public static final BigDecimal TOLERANCIA_PAGAMENTO = new BigDecimal("0.01");

    /**
     * Determina o status baseado em valor pago e total (BigDecimal).
     */
    public static StatusPagamento calcular(BigDecimal valorPago, BigDecimal valorTotal) {
        if (valorPago == null) valorPago = BigDecimal.ZERO;
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
        if (valorTotal.subtract(valorPago).compareTo(TOLERANCIA_PAGAMENTO) <= 0) return PAGO;
        if (valorPago.compareTo(TOLERANCIA_PAGAMENTO) > 0) return PARCIAL;
        return PENDENTE;
    }

    /**
     * Determina o status baseado no saldo devedor (BigDecimal).
     */
    public static StatusPagamento calcularPorSaldo(BigDecimal saldoDevedor, BigDecimal valorPago) {
        if (saldoDevedor == null) saldoDevedor = BigDecimal.ZERO;
        if (valorPago == null) valorPago = BigDecimal.ZERO;
        if (saldoDevedor.compareTo(TOLERANCIA_PAGAMENTO) <= 0) return PAGO;
        if (valorPago.compareTo(TOLERANCIA_PAGAMENTO) > 0) return PARCIAL;
        return PENDENTE;
    }

    /**
     * Overloads com double para compatibilidade com codigo legado.
     * @deprecated Usar versao BigDecimal.
     */
    @Deprecated
    public static StatusPagamento calcular(double valorPago, double valorTotal) {
        return calcular(BigDecimal.valueOf(valorPago), BigDecimal.valueOf(valorTotal));
    }

    @Deprecated
    public static StatusPagamento calcularPorSaldo(double saldoDevedor, double valorPago) {
        return calcularPorSaldo(BigDecimal.valueOf(saldoDevedor), BigDecimal.valueOf(valorPago));
    }

    /**
     * Converte string do banco para enum (case-insensitive).
     * Retorna PENDENTE como default se nao reconhecer.
     */
    public static StatusPagamento fromString(String status) {
        if (status == null || status.trim().isEmpty()) return PENDENTE;
        try {
            return valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("StatusPagamento.fromString: valor desconhecido '" + status + "' — retornando PENDENTE");
            return PENDENTE;
        }
    }

}
