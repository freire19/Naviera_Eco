package gui.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Permite digitação livre no ComboBox editável, filtra a lista e
 * reexibe as opções corrigindo problemas de “cliquei errado” ou “digitei errado”.
 *
 * - Se esvaziar o texto, restaura a lista original e reabre o combo.
 * - Se digitar algo, filtra e exibe as sugestões.
 * - Usa 'isFiltering' para evitar loop de eventos.
 *
 * Para usar:
 *   ComboBox<String> combo = new ComboBox<>();
 *   combo.setItems(FXCollections.observableArrayList(...));
 *   new AutoCompleteComboBoxListener<>(combo);
 *
 * Isso torna o ComboBox editável e, a cada letra digitada, filtra os itens
 * de acordo com o texto inserido.  
 */
public class AutoCompleteComboBoxListener<T> implements ChangeListener<String> {

    private final ComboBox<T> comboBox;
    private final List<T> originalData;    // guarda a lista completa original
    private String previousText = "";
    private boolean isFiltering = false;    // flag para evitar reentrância

    public AutoCompleteComboBoxListener(ComboBox<T> comboBox) {
        this.comboBox = comboBox;
        // O ComboBox deve estar em modo editável para permitir digitação
        this.comboBox.setEditable(true);

        // Guarda uma cópia da lista completa (State inicial)
        this.originalData = new ArrayList<>(comboBox.getItems());

        // Adiciona listener ao texto do editor interno do ComboBox
        this.comboBox.getEditor().textProperty().addListener(this);

        // Se teclar ENTER ou TAB, confirma a seleção atual (se existir)
        this.comboBox.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER || evt.getCode() == KeyCode.TAB) {
                commitSelection();
            }
        });
    }

    @Override
    public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
        // Se já estivermos filtrando, retorna para não gerar loop
        if (isFiltering) {
            return;
        }

        if (newVal == null) {
            return;
        }

        // Se limpou todo o texto, restaura a lista original
        if (newVal.isEmpty()) {
            restoreOriginalData();
            return;
        }

        // Se não houve mudança real de texto, ignora
        if (newVal.equals(previousText)) {
            return;
        }
        previousText = newVal;

        // Faz o filtro real
        doFilter(newVal);
    }

    /**
     * Filtra os itens originais com base no texto digitado (case‐insensitive).
     * Exibe somente os itens que contêm a substring digitada. Se não sobrar nada,
     * esconde a lista; caso contrário, mostra/atualiza automaticamente.
     */
    private void doFilter(String text) {
        isFiltering = true;
        try {
            String lower = text.toLowerCase();
            List<T> filtered = new ArrayList<>();
            for (T item : originalData) {
                if (item.toString().toLowerCase().contains(lower)) {
                    filtered.add(item);
                }
            }
            comboBox.getItems().setAll(filtered);

            // Se não tiver nada, fecha a lista
            if (filtered.isEmpty()) {
                if (comboBox.isShowing()) {
                    comboBox.hide();
                }
            } else {
                // Se tiver algo e estiver fechado, abre a lista
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
            }
        } finally {
            isFiltering = false;
        }
    }

    /**
     * Restaura a lista completa original e força reexibir o dropdown do ComboBox.
     */
    private void restoreOriginalData() {
        isFiltering = true;
        try {
            comboBox.getItems().setAll(originalData);
            previousText = "";
            // Força fechar e abrir para mostrar a lista completa
            comboBox.hide();
            comboBox.show();
        } finally {
            isFiltering = false;
        }
    }

    /**
     * Se nenhuma linha estiver selecionada mas houver itens disponíveis,
     * seleciona o primeiro da lista e fecha o dropdown.
     */
    private void commitSelection() {
        if (comboBox.getSelectionModel().getSelectedIndex() < 0
                && !comboBox.getItems().isEmpty()) {
            comboBox.getSelectionModel().select(0);
        }
        if (comboBox.isShowing()) {
            comboBox.hide();
        }
    }
}
