package gui.util;

import java.math.BigDecimal;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

/**
 * Validacao unificada para controllers JavaFX.
 * Cada metodo valida um campo, mostra alerta especifico e foca no campo com erro.
 * Retorna true se valido, false se invalido.
 *
 * Uso tipico:
 *   if (!ValidationHelper.requiredText(txtNome, "Nome do Passageiro")) return;
 *   if (!ValidationHelper.positiveInt(txtQtd, "Quantidade")) return;
 */
public class ValidationHelper {

    /**
     * Valida que um TextField nao esta vazio.
     */
    public static boolean requiredText(TextField field, String fieldName) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            showAndFocus(field, fieldName + " deve ser informado(a).");
            return false;
        }
        return true;
    }

    /**
     * Valida que um ComboBox editavel tem valor selecionado ou digitado.
     */
    public static boolean requiredCombo(ComboBox<?> combo, String fieldName) {
        if (combo == null) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", fieldName + " deve ser informado(a).");
            return false;
        }
        String value = getComboText(combo);
        if (value == null || value.isEmpty()) {
            showAndFocus(combo, fieldName + " deve ser informado(a).");
            return false;
        }
        return true;
    }

    /**
     * Valida que um DatePicker tem data selecionada.
     */
    public static boolean requiredDate(DatePicker picker, String fieldName) {
        if (picker == null || picker.getValue() == null) {
            showAndFocus(picker, fieldName + " deve ser informado(a).");
            return false;
        }
        return true;
    }

    /**
     * Valida que uma lista tem pelo menos um item.
     */
    public static boolean requiredList(ObservableList<?> list, Control focusTarget, String fieldName) {
        if (list == null || list.isEmpty()) {
            showAndFocus(focusTarget, "É necessário adicionar pelo menos um item em " + fieldName + ".");
            return false;
        }
        return true;
    }

    /**
     * Valida que o texto e um inteiro positivo (> 0).
     */
    public static boolean positiveInt(TextField field, String fieldName) {
        if (!requiredText(field, fieldName)) return false;
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value <= 0) {
                showAndFocus(field, fieldName + " deve ser um número maior que zero.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showAndFocus(field, fieldName + " deve ser um número inteiro válido.");
            return false;
        }
    }

    /**
     * Valida que o texto e um BigDecimal >= 0 (permite zero, bloqueia negativo).
     */
    public static boolean nonNegativeMoney(TextField field, String fieldName) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            return true; // campo vazio e tratado como zero pelo MoneyUtil
        }
        try {
            BigDecimal value = MoneyUtil.parseBigDecimal(field.getText());
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                showAndFocus(field, fieldName + " não pode ser negativo.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showAndFocus(field, fieldName + " contém um valor monetário inválido.");
            return false;
        }
    }

    /**
     * Valida que o texto e um double >= 0 (para precos em contextos legados).
     */
    public static boolean nonNegativeDouble(TextField field, String fieldName) {
        if (!requiredText(field, fieldName)) return false;
        try {
            double value = MoneyUtil.parseDouble(field.getText());
            if (value < 0) {
                showAndFocus(field, fieldName + " não pode ser negativo.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showAndFocus(field, fieldName + " contém um valor numérico inválido.");
            return false;
        }
    }

    // --- Helpers internos ---

    private static void showAndFocus(Control field, String message) {
        AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", message);
        if (field != null) field.requestFocus();
    }

    private static String getComboText(ComboBox<?> combo) {
        if (combo.getValue() != null) {
            String val = combo.getValue().toString().trim();
            if (!val.isEmpty()) return val;
        }
        if (combo.isEditable() && combo.getEditor() != null) {
            String editorText = combo.getEditor().getText();
            if (editorText != null && !editorText.trim().isEmpty()) return editorText.trim();
        }
        return null;
    }
}
