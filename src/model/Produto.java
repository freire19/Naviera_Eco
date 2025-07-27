package model;

import java.math.BigDecimal;

/**
 * Modelo mínimo para Produto (só para que o ProdutoDAO e os Controllers compilem).
 */
public class Produto {
    private int id;
    private String nome;
    private BigDecimal preco;

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

    public BigDecimal getPreco() {
        return preco;
    }
    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }
}
