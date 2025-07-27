package model;

public class TipoPassageiro {

    private int id;
    private String nome;
    private int idadeMin;
    private int idadeMax;
    private boolean deficiente;
    private boolean gratuito;

    // GETTERS / SETTERS
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getIdadeMin() {
        return idadeMin;
    }
    public void setIdadeMin(int idadeMin) {
        this.idadeMin = idadeMin;
    }

    public int getIdadeMax() {
        return idadeMax;
    }
    public void setIdadeMax(int idadeMax) {
        this.idadeMax = idadeMax;
    }

    public boolean isDeficiente() {
        return deficiente;
    }
    public void setDeficiente(boolean deficiente) {
        this.deficiente = deficiente;
    }

    public boolean isGratuito() {
        return gratuito;
    }
    public void setGratuito(boolean gratuito) {
        this.gratuito = gratuito;
    }
}
