package model;

import java.math.BigDecimal;
import java.util.Objects;

public class EncomendaItem {

    // Campos para a tela InserirEncomenda
    private Long id;
    private Long idEncomenda;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
    private String localArmazenamento;
    
    // Campos para a tela CadastroProdutoController
    private String nomeItem;
    private String descricao;
    private String unidadeMedida;
    private BigDecimal precoUnit;
    private boolean permiteValorDeclarado;
    private boolean ativo;

    // Getters e Setters para TODOS os campos
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdEncomenda() { return idEncomenda; }
    public void setIdEncomenda(Long idEncomenda) { this.idEncomenda = idEncomenda; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getNomeItem() { return nomeItem; }
    public void setNomeItem(String nomeItem) { this.nomeItem = nomeItem; }
    
    public BigDecimal getPrecoUnit() { return precoUnit; }
    public void setPrecoUnit(BigDecimal precoUnit) { this.precoUnit = precoUnit; }
    
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    
    public boolean isPermiteValorDeclarado() { return permiteValorDeclarado; }
    public void setPermiteValorDeclarado(boolean permiteValorDeclarado) { this.permiteValorDeclarado = permiteValorDeclarado; }
    
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    
    public String getLocalArmazenamento() { return localArmazenamento; }
    public void setLocalArmazenamento(String localArmazenamento) { this.localArmazenamento = localArmazenamento; }

    @Override
    public String toString() {
        // DR128: retorna string vazia se nomeItem for null (evita NPE em ComboBox/ListView)
        return nomeItem != null ? nomeItem : "";
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