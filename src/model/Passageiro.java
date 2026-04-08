package model;

import java.time.LocalDate;

public class Passageiro {
    private Long id; // id_passageiro (PK)
    private String nome; // nome_passageiro
    private String numeroDoc; // numero_documento
    private LocalDate dataNascimento; // data_nascimento
    private String sexo; // nome_sexo (FK)
    private String tipoDoc; // nome_tipo_doc (FK)
    private String nacionalidade; // nome_nacionalidade (FK)

    public Passageiro() {}

    public Passageiro(Long id, String nome, String numeroDoc, LocalDate dataNascimento, String sexo, String tipoDoc, String nacionalidade) {
        this.id = id;
        this.nome = nome;
        this.numeroDoc = numeroDoc;
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.tipoDoc = tipoDoc;
        this.nacionalidade = nacionalidade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNumeroDoc() { return numeroDoc; }
    public void setNumeroDoc(String numeroDoc) { this.numeroDoc = numeroDoc; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    @Override
    public String toString() {
        return nome != null ? nome : "";
    }
}