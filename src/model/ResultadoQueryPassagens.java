package model;

import javafx.collections.ObservableList;
import java.math.BigDecimal;

/**
 * Resultado da query de passagens financeiras.
 */
public class ResultadoQueryPassagens {
    public final ObservableList<PassagemFinanceiro> lista;
    public final BigDecimal somaPendente;

    public ResultadoQueryPassagens(ObservableList<PassagemFinanceiro> lista, BigDecimal somaPendente) {
        this.lista = lista;
        this.somaPendente = somaPendente;
    }
}
