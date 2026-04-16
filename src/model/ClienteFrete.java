package model;

import java.util.Objects;

/**
 * Representa um Cliente de Frete.
 * Corresponde à tabela 'cad_clientes_frete'.
 * Separado de ClienteEncomenda — cada módulo tem sua própria tabela de clientes.
 */
public class ClienteFrete {

    private Long idCliente;
    private String nomeCliente;
    private String razaoSocial;
    private String cpfCnpj;
    private String endereco;
    private String inscricaoEstadual;
    private String email;
    private String telefone;

    public ClienteFrete() {}

    public ClienteFrete(Long idCliente, String nomeCliente) {
        this.idCliente = idCliente;
        this.nomeCliente = nomeCliente;
    }

    // Getters e Setters
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    @Override
    public String toString() {
        return nomeCliente != null ? nomeCliente : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClienteFrete that = (ClienteFrete) o;
        return Objects.equals(idCliente, that.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }
}
