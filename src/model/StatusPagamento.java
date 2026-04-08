package model;

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

    /**
     * Determina o status baseado em valor pago e total.
     * Usa tolerancia de 0.01 para comparacoes de double.
     */
    public static StatusPagamento calcular(double valorPago, double valorTotal) {
        if (valorTotal - valorPago <= 0.01) return PAGO;
        if (valorPago > 0.01) return PARCIAL;
        return PENDENTE;
    }

    /**
     * Determina o status baseado no saldo devedor.
     */
    public static StatusPagamento calcularPorSaldo(double saldoDevedor, double valorPago) {
        if (saldoDevedor <= 0.01) return PAGO;
        if (valorPago > 0.01) return PARCIAL;
        return PENDENTE;
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
            return PENDENTE;
        }
    }

    /**
     * Retorna a cor CSS para uso em estilos JavaFX.
     */
    public String getCorCSS() {
        switch (this) {
            case PAGO: case QUITADO: return "#2e7d32";
            case PARCIAL: return "#ef6c00";
            case PENDENTE: case NAO_PAGO: return "#c62828";
            case EMITIDA: return "#1565c0";
            case CANCELADA: return "#757575";
            default: return "#000000";
        }
    }

    /**
     * Retorna o estilo CSS completo para celulas de tabela.
     */
    public String getEstiloCelula() {
        return "-fx-text-fill: " + getCorCSS() + "; -fx-font-weight: bold; -fx-alignment: CENTER;";
    }
}
