package model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import java.math.BigDecimal;

// #DB022: valorNota e valorFreteItem migrados para BigDecimal
public class FreteItem {
    private final SimpleStringProperty descricao;
    private final SimpleIntegerProperty quantidade;
    private final SimpleDoubleProperty pesoBalanca;
    private final SimpleDoubleProperty pesoCubado;
    private final SimpleObjectProperty<BigDecimal> valorNota;
    private final SimpleObjectProperty<BigDecimal> valorFreteItem;
    // Adicione outros campos que você precisa para um item de frete
    // Ex: String unidade, Double altura, Double largura, Double comprimento, etc.

    public FreteItem(String descricao, int quantidade, double pesoBalanca, double pesoCubado, BigDecimal valorNota, BigDecimal valorFreteItem) {
        this.descricao = new SimpleStringProperty(descricao);
        this.quantidade = new SimpleIntegerProperty(quantidade);
        this.pesoBalanca = new SimpleDoubleProperty(pesoBalanca);
        this.pesoCubado = new SimpleDoubleProperty(pesoCubado);
        this.valorNota = new SimpleObjectProperty<>(valorNota != null ? valorNota : BigDecimal.ZERO);
        this.valorFreteItem = new SimpleObjectProperty<>(valorFreteItem != null ? valorFreteItem : BigDecimal.ZERO);
    }

    public FreteItem() {
        this.descricao = new SimpleStringProperty("");
        this.quantidade = new SimpleIntegerProperty(0);
        this.pesoBalanca = new SimpleDoubleProperty(0.0);
        this.pesoCubado = new SimpleDoubleProperty(0.0);
        this.valorNota = new SimpleObjectProperty<>(BigDecimal.ZERO);
        this.valorFreteItem = new SimpleObjectProperty<>(BigDecimal.ZERO);
    }

    // Getters e Setters para todas as propriedades
    // Exemplo para descricao:
    public String getDescricao() {
        return descricao.get();
    }
    public SimpleStringProperty descricaoProperty() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao.set(descricao);
    }

    public int getQuantidade() {
        return quantidade.get();
    }
    public SimpleIntegerProperty quantidadeProperty() {
        return quantidade;
    }
    public void setQuantidade(int quantidade) {
        this.quantidade.set(quantidade);
    }

    public double getPesoBalanca() {
        return pesoBalanca.get();
    }
    public SimpleDoubleProperty pesoBalancaProperty() {
        return pesoBalanca;
    }
    public void setPesoBalanca(double pesoBalanca) {
        this.pesoBalanca.set(pesoBalanca);
    }

    public double getPesoCubado() {
        return pesoCubado.get();
    }
    public SimpleDoubleProperty pesoCubadoProperty() {
        return pesoCubado;
    }
    public void setPesoCubado(double pesoCubado) {
        this.pesoCubado.set(pesoCubado);
    }

    public BigDecimal getValorNota() {
        return valorNota.get();
    }
    public SimpleObjectProperty<BigDecimal> valorNotaProperty() {
        return valorNota;
    }
    public void setValorNota(BigDecimal valorNota) {
        this.valorNota.set(valorNota != null ? valorNota : BigDecimal.ZERO);
    }

    public BigDecimal getValorFreteItem() {
        return valorFreteItem.get();
    }
    public SimpleObjectProperty<BigDecimal> valorFreteItemProperty() {
        return valorFreteItem;
    }
    public void setValorFreteItem(BigDecimal valorFreteItem) {
        this.valorFreteItem.set(valorFreteItem != null ? valorFreteItem : BigDecimal.ZERO);
    }

    // Adapte ou adicione mais getters/setters conforme necessário
}