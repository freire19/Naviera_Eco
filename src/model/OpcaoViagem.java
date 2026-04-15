package model;

/**
 * Representa uma opcao de viagem para ComboBoxes financeiros.
 * Centraliza classe que antes era duplicada em 6 controllers.
 */
public class OpcaoViagem {
    public final int id;
    public final String label;

    public OpcaoViagem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }

    @Override
    public String toString() { return label != null ? label : ""; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OpcaoViagem that = (OpcaoViagem) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
