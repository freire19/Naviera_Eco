package model;

import java.math.BigDecimal;

public class ItemFrete {
    
    // Suas variáveis originais
    private int idItemFrete; 
    private String nomeItem;
    private BigDecimal precoUnitarioPadrao;
    private BigDecimal precoUnitarioDesconto;
    private boolean ativo;
    private String descricao;
    private String unidadeMedida;

    // Construtor vazio
    public ItemFrete() {
    }

    // CONSTRUTOR COMPLETO
    public ItemFrete(int idItemFrete, String nomeItem, String descricao,
                     String unidadeMedida, BigDecimal precoUnitarioPadrao,
                     BigDecimal precoUnitarioDesconto, boolean ativo) {
        this.idItemFrete = idItemFrete;
        this.nomeItem = nomeItem;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.precoUnitarioPadrao = precoUnitarioPadrao;
        this.precoUnitarioDesconto = precoUnitarioDesconto;
        this.ativo = ativo;
    }

    // --- GETTERS E SETTERS ---

    public int getIdItemFrete() { return idItemFrete; }
    public void setIdItemFrete(int idItemFrete) { this.idItemFrete = idItemFrete; }

    // *** MÉTODO DE CORREÇÃO (A PONTE) ***
    // Adicionei estes dois métodos para que o sistema funcione 
    // tanto se chamar getId() quanto getIdItemFrete()
    public int getId() { return idItemFrete; }
    public void setId(int id) { this.idItemFrete = id; }
    // ************************************

    public String getNomeItem() { return nomeItem; }
    public void setNomeItem(String nomeItem) { this.nomeItem = nomeItem; }

    public BigDecimal getPrecoUnitarioPadrao() { return precoUnitarioPadrao; }
    public void setPrecoUnitarioPadrao(BigDecimal precoUnitarioPadrao) { this.precoUnitarioPadrao = precoUnitarioPadrao; }

    public BigDecimal getPrecoUnitarioDesconto() { return precoUnitarioDesconto; }
    public void setPrecoUnitarioDesconto(BigDecimal precoUnitarioDesconto) { this.precoUnitarioDesconto = precoUnitarioDesconto; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    
    @Override
    public String toString() {
        // DR128: retorna string vazia se nomeItem for null (evita NPE em ComboBox/ListView)
        return nomeItem != null ? nomeItem : "";
    }
}