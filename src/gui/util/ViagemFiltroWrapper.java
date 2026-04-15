package gui.util;

/**
 * Wrapper para exibir viagens em ComboBox de filtro.
 */
public class ViagemFiltroWrapper {
    private final Long id;
    private final String displayText;

    public ViagemFiltroWrapper(Long id, String displayText) {
        this.id = id;
        this.displayText = displayText;
    }

    public Long getId() { return id; }

    @Override
    public String toString() { return displayText; }
}
