package model;

import java.math.BigDecimal;
import java.util.Objects;

public class ItemEncomendaPadrao {

    private Long id;
    private String nomeItem;
    private String descricao;
    private String unidadeMedida;
    private BigDecimal precoUnit;
    private boolean permiteValorDeclarado;
    private boolean ativo;

    // Construtor vazio
    public ItemEncomendaPadrao() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeItem() { return nomeItem; }
    public void setNomeItem(String nomeItem) { this.nomeItem = nomeItem; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }

    public BigDecimal getPrecoUnit() { return precoUnit != null ? precoUnit : BigDecimal.ZERO; }
    public void setPrecoUnit(BigDecimal precoUnit) { this.precoUnit = precoUnit; }

    public boolean isPermiteValorDeclarado() { return permiteValorDeclarado; }
    public void setPermiteValorDeclarado(boolean permiteValorDeclarado) { this.permiteValorDeclarado = permiteValorDeclarado; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public String toString() {
        // DR128: retorna string vazia se nomeItem for null (evita NPE em ComboBox/ListView)
        return nomeItem != null ? nomeItem : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemEncomendaPadrao that = (ItemEncomendaPadrao) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}