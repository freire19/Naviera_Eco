package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DadosBalancoViagem {
    // Totais Gerais
    private BigDecimal totalPassagens = BigDecimal.ZERO;
    private BigDecimal totalEncomendas = BigDecimal.ZERO;
    private BigDecimal totalFretes = BigDecimal.ZERO;
    private BigDecimal totalSaidas = BigDecimal.ZERO;

    // Lista detalhada para a Visao Geral
    private List<ItemResumoBalanco> itensReceita = new ArrayList<>();

    // Mapa para o Grafico de Despesas
    private Map<String, BigDecimal> saidasPorCategoria = new HashMap<>();

    // Flag para indicar que alguma secao falhou ao carregar
    private boolean dadosIncompletos = false;
    private String erroDetalhes = "";

    // --- Metodos ---
    public void adicionarItem(ItemResumoBalanco item) {
        this.itensReceita.add(item);
    }

    public void somarPassagens(BigDecimal v) { totalPassagens = totalPassagens.add(v); }
    public void somarEncomendas(BigDecimal v) { totalEncomendas = totalEncomendas.add(v); }
    public void somarFretes(BigDecimal v) { totalFretes = totalFretes.add(v); }

    public BigDecimal getTotalEntradas() {
        return totalPassagens.add(totalEncomendas).add(totalFretes);
    }

    public BigDecimal getLucroLiquido() {
        return getTotalEntradas().subtract(totalSaidas);
    }

    // Getters e Setters
    public BigDecimal getTotalPassagens() { return totalPassagens; }
    public void setTotalPassagens(BigDecimal t) { this.totalPassagens = t != null ? t : BigDecimal.ZERO; }

    public BigDecimal getTotalEncomendas() { return totalEncomendas; }
    public void setTotalEncomendas(BigDecimal t) { this.totalEncomendas = t != null ? t : BigDecimal.ZERO; }

    public BigDecimal getTotalFretes() { return totalFretes; }
    public void setTotalFretes(BigDecimal t) { this.totalFretes = t != null ? t : BigDecimal.ZERO; }

    public BigDecimal getTotalSaidas() { return totalSaidas; }
    public void setTotalSaidas(BigDecimal t) { this.totalSaidas = t != null ? t : BigDecimal.ZERO; }

    public Map<String, BigDecimal> getSaidasPorCategoria() { return saidasPorCategoria; }
    public void setSaidasPorCategoria(Map<String, BigDecimal> m) { this.saidasPorCategoria = m; }

    public List<ItemResumoBalanco> getItensReceita() { return itensReceita; }

    public boolean isDadosIncompletos() { return dadosIncompletos; }
    public void marcarIncompleto(String secao, String erro) {
        this.dadosIncompletos = true;
        this.erroDetalhes += secao + ": " + erro + "; ";
    }
    public String getErroDetalhes() { return erroDetalhes; }
}
