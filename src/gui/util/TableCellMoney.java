package gui.util;

import javafx.scene.control.TableCell;
import model.Encomenda;
import java.math.BigDecimal;

/**
 * TableCell customizado para exibir valores monetarios (BigDecimal) com formatacao BRL
 * e destaque visual para valores devedores.
 */
public class TableCellMoney extends TableCell<Encomenda, BigDecimal> {
    private final boolean destacarDevedor;

    public TableCellMoney() {
        this(false);
    }

    public TableCellMoney(boolean destacarDevedor) {
        this.destacarDevedor = destacarDevedor;
    }

    @Override
    protected void updateItem(BigDecimal item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setStyle("");
        } else {
            setText(String.format("R$ %,.2f", item));
            if (destacarDevedor && item.compareTo(BigDecimal.ZERO) > 0) {
                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            } else if (!destacarDevedor && item.compareTo(BigDecimal.ZERO) > 0) {
                setStyle("-fx-text-fill: green;");
            } else {
                setStyle("-fx-text-fill: black;");
            }
        }
    }
}
