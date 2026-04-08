package gui.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;

/**
 * STUB INERTE — esta classe existia para fazer autocomplete nos ComboBoxes,
 * mas causava conflitos com a lógica do VenderPassagemController
 * (forçava select(0) ao apertar ENTER, causando auto-seleção indesejada).
 *
 * A lógica de autocomplete agora é feita diretamente nos Controllers.
 * Esta classe é mantida apenas para não quebrar compilação caso seja
 * referenciada em algum lugar.
 */
public class AutoCompleteComboBoxListener<T> implements ChangeListener<String> {

    public AutoCompleteComboBoxListener(ComboBox<T> comboBox) {
        // NÃO FAZ NADA — a lógica de autocomplete é controlada pelo Controller
    }

    @Override
    public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
        // NÃO FAZ NADA
    }
}