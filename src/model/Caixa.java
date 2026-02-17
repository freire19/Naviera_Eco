package model;

import java.util.Objects;

public class Caixa {
    
    private int id;
    private String nome;

    public Caixa() {}

    public Caixa(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    // O segredo para o ComboBox mostrar o nome bonito:
    @Override
    public String toString() {
        return nome; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Caixa caixa = (Caixa) o;
        return id == caixa.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}