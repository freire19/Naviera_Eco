package model;

import java.math.BigDecimal;

/**
 * Representa um item de encomenda padrão, armazenado na tabela `itens_encomenda_padrao`.
 *
 * Colunas na tabela `itens_encomenda_padrao` (PostgreSQL):
 *   - id_item_encomenda           (PK, serial)
 *   - nome_item                   (text)
 *   - preco_unitario_padrao       (numeric)
 *   - unidade_medida              (text)
 *   - permite_valor_declarado     (boolean)
 *   - descricao                   (text)
 *   - ativo                       (boolean)
 */
public class EncomendaItem {

    private int id;                          // id_item_encomenda
    private String nomeItem;                // nome_item
    private BigDecimal precoUnit;           // preco_unitario_padrao
    private String unidadeMedida;           // unidade_medida
    private boolean permiteValorDeclarado;  // permite_valor_declarado
    private String descricao;               // descricao
    private boolean ativo;                  // ativo

    public EncomendaItem() {
        // Construtor vazio para frameworks / DAO
    }

    public EncomendaItem(int id, String nomeItem, BigDecimal precoUnit,
                         String unidadeMedida, boolean permiteValorDeclarado,
                         String descricao, boolean ativo) {
        this.id = id;
        this.nomeItem = nomeItem;
        this.precoUnit = precoUnit;
        this.unidadeMedida = unidadeMedida;
        this.permiteValorDeclarado = permiteValorDeclarado;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    // ========================
    // Getters e Setters
    // ========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomeItem() {
        return nomeItem;
    }

    public void setNomeItem(String nomeItem) {
        this.nomeItem = nomeItem;
    }

    public BigDecimal getPrecoUnit() {
        return precoUnit;
    }

    public void setPrecoUnit(BigDecimal precoUnit) {
        this.precoUnit = precoUnit;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public boolean isPermiteValorDeclarado() {
        return permiteValorDeclarado;
    }

    public void setPermiteValorDeclarado(boolean permiteValorDeclarado) {
        this.permiteValorDeclarado = permiteValorDeclarado;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
