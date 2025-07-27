// src/gui/AuxiliarItem.java
package gui;

public class AuxiliarItem {
    private String nome;
    // Se AuxiliarItem tiver um ID, adicione aqui (ex: private Integer id;)

    public AuxiliarItem(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}