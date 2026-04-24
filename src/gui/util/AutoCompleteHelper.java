package gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

/**
 * Autocomplete unificado para ComboBox editaveis.
 *
 * Espelha o componente Autocomplete.jsx da web: filtro local case-insensitive
 * com debounce, dropdown com setas/Enter/Esc, maximo 10 sugestoes.
 *
 * O dropdown nativo do ComboBox e desativado (evita conflito com o ContextMenu
 * customizado). Chame AutoCompleteHelper.install(...) no initialize() do
 * controller — a lista e consultada a cada filtragem (Supplier), entao
 * carregamento em background funciona sem precisar reinstalar.
 */
public final class AutoCompleteHelper {

    private static final int MAX_SUGESTOES = 10;
    private static final int DEBOUNCE_MS = 150;

    private AutoCompleteHelper() {}

    public static void install(ComboBox<String> cmb, Supplier<List<String>> source) {
        installImpl(cmb, source, s -> s, null, null);
    }

    public static void install(ComboBox<String> cmb, Supplier<List<String>> source, Runnable onSelect) {
        installImpl(cmb, source, s -> s, (onSelect == null) ? null : s -> onSelect.run(), null);
    }

    public static void install(ComboBox<String> cmb, Supplier<List<String>> source, Consumer<String> onSelect) {
        installImpl(cmb, source, s -> s, onSelect, null);
    }

    public static <T> void installGeneric(
            ComboBox<T> cmb,
            Supplier<List<T>> source,
            Function<T, String> toDisplay,
            Consumer<T> onSelect) {
        installImpl(cmb, source, toDisplay, onSelect, null);
    }

    /**
     * Versao com renderer customizado: o Node retornado é exibido no dropdown
     * (ex: HBox com nome + preco). toDisplay continua sendo usado para o texto
     * do editor após selecionar e para filtragem.
     */
    public static <T> void installGeneric(
            ComboBox<T> cmb,
            Supplier<List<T>> source,
            Function<T, String> toDisplay,
            Consumer<T> onSelect,
            Function<T, Node> renderer) {
        installImpl(cmb, source, toDisplay, onSelect, renderer);
    }

    private static <T> void installImpl(
            ComboBox<T> cmb,
            Supplier<List<T>> source,
            Function<T, String> toDisplay,
            Consumer<T> onSelect,
            Function<T, Node> renderer) {

        if (cmb == null || source == null || toDisplay == null) return;

        cmb.setEditable(true);

        final ContextMenu menu = new ContextMenu();
        menu.setAutoHide(true);
        menu.setHideOnEscape(true);
        menu.getStyleClass().add("autocomplete-menu");

        final int[] highlight = {-1};
        final List<T> sugestoes = new ArrayList<>();
        final boolean[] programmatic = {false};

        final PauseTransition debounce = new PauseTransition(Duration.millis(DEBOUNCE_MS));

        final Runnable aplicarDestaque = () -> {
            for (int i = 0; i < menu.getItems().size(); i++) {
                MenuItem mi = menu.getItems().get(i);
                if (!(mi instanceof CustomMenuItem)) continue;
                Node content = ((CustomMenuItem) mi).getContent();
                if (content == null) continue;
                boolean selected = i == highlight[0];
                if (content instanceof Label) {
                    Label lbl = (Label) content;
                    lbl.setStyle(selected
                        ? "-fx-padding: 5 10 5 10; -fx-background-color: #047857; -fx-text-fill: white; -fx-font-weight: bold;"
                        : "-fx-padding: 5 10 5 10;");
                } else {
                    // Node customizado (renderer): aplica apenas o background verde no wrapper
                    content.setStyle(selected ? "-fx-background-color: #047857;" : "");
                    aplicarCorTextoRecursivo(content, selected);
                }
            }
        };

        final Consumer<T> selecionar = item -> {
            programmatic[0] = true;
            try {
                cmb.setValue(item);
                String t = toDisplay.apply(item);
                if (t != null) {
                    cmb.getEditor().setText(t);
                    cmb.getEditor().positionCaret(t.length());
                }
            } finally {
                programmatic[0] = false;
            }
            menu.hide();
            highlight[0] = -1;
            if (onSelect != null) onSelect.accept(item);
        };

        final Runnable filtrar = () -> {
            if (programmatic[0]) return;
            String text = cmb.getEditor().getText();
            String upper = (text == null) ? "" : text.toUpperCase();
            List<T> all = source.get();
            sugestoes.clear();
            menu.getItems().clear();
            if (all != null) {
                for (T t : all) {
                    if (t == null) continue;
                    String disp = toDisplay.apply(t);
                    if (disp == null) continue;
                    if (upper.isEmpty() || disp.toUpperCase().contains(upper)) {
                        sugestoes.add(t);
                        if (sugestoes.size() >= MAX_SUGESTOES) break;
                    }
                }
            }
            double larguraCmb = cmb.getWidth() > 0 ? cmb.getWidth() : 250;
            for (T item : sugestoes) {
                CustomMenuItem mi = new CustomMenuItem();
                Node content;
                if (renderer != null) {
                    Node custom = renderer.apply(item);
                    javafx.scene.layout.HBox wrapper = new javafx.scene.layout.HBox(custom);
                    wrapper.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                    wrapper.setPrefWidth(larguraCmb - 8);
                    javafx.scene.layout.HBox.setHgrow(custom, javafx.scene.layout.Priority.ALWAYS);
                    if (custom instanceof javafx.scene.layout.Region) {
                        ((javafx.scene.layout.Region) custom).setMaxWidth(Double.MAX_VALUE);
                    }
                    content = wrapper;
                } else {
                    Label lbl = new Label(toDisplay.apply(item));
                    lbl.setStyle("-fx-padding: 5 10 5 10;");
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setPrefWidth(larguraCmb - 8);
                    content = lbl;
                }
                mi.setContent(content);
                mi.setHideOnClick(true);
                mi.setOnAction(ev -> selecionar.accept(item));
                menu.getItems().add(mi);
            }
            menu.setMinWidth(larguraCmb);
            menu.setPrefWidth(larguraCmb);
            highlight[0] = sugestoes.isEmpty() ? -1 : 0;
            aplicarDestaque.run();
            if (sugestoes.isEmpty()) {
                menu.hide();
            } else if (cmb.getEditor().isFocused() || cmb.isFocused()) {
                if (!menu.isShowing()) menu.show(cmb, Side.BOTTOM, 0, 0);
            }
        };

        // Clique na setinha do ComboBox: cancela dropdown nativo e abre nosso menu
        cmb.setOnShowing(e -> Platform.runLater(() -> {
            cmb.hide();
            cmb.getEditor().requestFocus();
            filtrar.run();
            if (!sugestoes.isEmpty() && !menu.isShowing()) {
                menu.show(cmb, Side.BOTTOM, 0, 0);
            }
        }));

        cmb.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (programmatic[0]) return;
            debounce.setOnFinished(e -> filtrar.run());
            debounce.playFromStart();
        });

        // Filter no ComboBox (nao no editor) — captura setas/Enter ANTES da skin nativa
        cmb.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.DOWN) {
                if (!menu.isShowing()) {
                    filtrar.run();
                } else if (!sugestoes.isEmpty()) {
                    highlight[0] = Math.min(highlight[0] + 1, sugestoes.size() - 1);
                    aplicarDestaque.run();
                }
                event.consume();
            } else if (code == KeyCode.UP) {
                if (menu.isShowing() && !sugestoes.isEmpty()) {
                    highlight[0] = Math.max(highlight[0] - 1, 0);
                    aplicarDestaque.run();
                }
                event.consume();
            } else if (code == KeyCode.ENTER) {
                if (menu.isShowing() && highlight[0] >= 0 && highlight[0] < sugestoes.size()) {
                    selecionar.accept(sugestoes.get(highlight[0]));
                    event.consume();
                } else if (menu.isShowing()) {
                    menu.hide();
                }
            } else if (code == KeyCode.ESCAPE) {
                if (menu.isShowing()) {
                    menu.hide();
                    event.consume();
                }
            } else if (code == KeyCode.TAB) {
                menu.hide();
            }
        });

        cmb.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                menu.hide();
                highlight[0] = -1;
            }
        });
    }

    /** Percorre a arvore de Labels do item destacado e troca a cor do texto. */
    private static void aplicarCorTextoRecursivo(Node node, boolean destacado) {
        if (node instanceof Label) {
            Label lbl = (Label) node;
            String baseStyle = lbl.getProperties().getOrDefault("naviera.base-style", "").toString();
            if (baseStyle.isEmpty()) {
                baseStyle = lbl.getStyle();
                lbl.getProperties().put("naviera.base-style", baseStyle);
            }
            if (destacado) {
                lbl.setStyle(baseStyle + "; -fx-text-fill: white;");
            } else {
                lbl.setStyle(baseStyle);
            }
        }
        if (node instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                aplicarCorTextoRecursivo(child, destacado);
            }
        }
    }
}
