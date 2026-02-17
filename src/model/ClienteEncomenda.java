package model;

import java.util.Objects;

/**
 * Representa o "molde" para um Cliente de Encomenda.
 * Corresponde à tabela 'cad_clientes_encomenda'.
 */
public class ClienteEncomenda {

    private Long idCliente;
    private String nomeCliente;

    // Construtor vazio
    public ClienteEncomenda() {
    }

    // Construtor com parâmetros
    public ClienteEncomenda(Long idCliente, String nomeCliente) {
        this.idCliente = idCliente;
        this.nomeCliente = nomeCliente;
    }

    // Getters e Setters
    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    // Método toString para fácil visualização em ComboBoxes
    @Override
    public String toString() {
        return nomeCliente;
    }

    // Métodos equals e hashCode para comparações corretas
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClienteEncomenda that = (ClienteEncomenda) o;
        return Objects.equals(idCliente, that.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }
}