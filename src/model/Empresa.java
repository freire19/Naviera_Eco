package model;

public class Empresa {
    private int id;
    private String companhia;
    private String embarcacao;
    private String comandante;
    private String proprietario;
    private String origem;
    private String gerente;
    private String linhaDoRio;
    private String cnpj;
    private String ie;
    private String endereco;
    private String cep;
    private String telefone;
    private String frase;
    private String caminhoFoto;
    private String recomendacoesBilhete;

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompanhia() { return companhia; }
    public void setCompanhia(String companhia) { this.companhia = companhia; }

    public String getEmbarcacao() { return embarcacao; }
    public void setEmbarcacao(String embarcacao) { this.embarcacao = embarcacao; }

    public String getComandante() { return comandante; }
    public void setComandante(String comandante) { this.comandante = comandante; }

    public String getProprietario() { return proprietario; }
    public void setProprietario(String proprietario) { this.proprietario = proprietario; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getGerente() { return gerente; }
    public void setGerente(String gerente) { this.gerente = gerente; }

    public String getLinhaDoRio() { return linhaDoRio; }
    public void setLinhaDoRio(String linhaDoRio) { this.linhaDoRio = linhaDoRio; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getIe() { return ie; }
    public void setIe(String ie) { this.ie = ie; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getFrase() { return frase; }
    public void setFrase(String frase) { this.frase = frase; }

    public String getCaminhoFoto() { return caminhoFoto; }
    public void setCaminhoFoto(String caminhoFoto) { this.caminhoFoto = caminhoFoto; }

    public String getRecomendacoesBilhete() { return recomendacoesBilhete; }
    public void setRecomendacoesBilhete(String recomendacoesBilhete) { this.recomendacoesBilhete = recomendacoesBilhete; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Empresa that = (Empresa) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}