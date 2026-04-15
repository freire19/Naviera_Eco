package model;

import java.math.BigDecimal;
import java.util.Objects;

public class EncomendaItem {

    // Campos da tela InserirEncomenda (linha do item dentro de uma encomenda)
    private Long id;
    private Long idEncomenda;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private String localArmazenamento;

    // Descrição do item nesta encomenda (pode diferir do nome no catálogo)
    private String descricao;

    // Referência ao item do catálogo que originou este item (pode ser null para itens avulsos)
    private Long idItemPadrao;

    // Getters e Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdEncomenda() { return idEncomenda; }
    public void setIdEncomenda(Long idEncomenda) { this.idEncomenda = idEncomenda; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValorUnitario() { return valorUnitario != null ? valorUnitario : BigDecimal.ZERO; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public BigDecimal getValorTotal() { return valorTotal != null ? valorTotal : BigDecimal.ZERO; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getLocalArmazenamento() { return localArmazenamento; }
    public void setLocalArmazenamento(String localArmazenamento) { this.localArmazenamento = localArmazenamento; }

    public Long getIdItemPadrao() { return idItemPadrao; }
    public void setIdItemPadrao(Long idItemPadrao) { this.idItemPadrao = idItemPadrao; }

    @Override
    public String toString() {
        // DR128: retorna string vazia se descricao for null (evita NPE em ComboBox/ListView)
        return descricao != null ? descricao : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncomendaItem that = (EncomendaItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}