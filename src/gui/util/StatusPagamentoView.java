package gui.util;

import model.StatusPagamento;

public class StatusPagamentoView {

    public static String getCorCSS(StatusPagamento status) {
        switch (status) {
            case PAGO: case QUITADO: return "#059669";
            case PARCIAL: return "#B45309";
            case PENDENTE: case NAO_PAGO: return "#DC2626";
            case EMITIDA: return "#059669";
            case CANCELADA: return "#757575";
            default: return "#000000";
        }
    }

    public static String getEstiloCelula(StatusPagamento status) {
        return "-fx-text-fill: " + getCorCSS(status) + "; -fx-font-weight: bold; -fx-alignment: CENTER;";
    }
}
