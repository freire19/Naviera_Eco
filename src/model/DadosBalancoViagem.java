package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DadosBalancoViagem {
    // Totais Gerais
    private double totalPassagens = 0;
    private double totalEncomendas = 0;
    private double totalFretes = 0;
    private double totalSaidas = 0;
    
    // Lista detalhada para a Visão Geral
    private List<ItemResumoBalanco> itensReceita = new ArrayList<>();
    
    // Mapa para o Gráfico de Despesas
    private Map<String, Double> saidasPorCategoria = new HashMap<>();

    // Flag para indicar que alguma seção falhou ao carregar
    private boolean dadosIncompletos = false;
    private String erroDetalhes = "";

    // --- Métodos ---
    public void adicionarItem(ItemResumoBalanco item) {
        this.itensReceita.add(item);
    }
    
    public void somarPassagens(double v) { totalPassagens += v; }
    public void somarEncomendas(double v) { totalEncomendas += v; }
    public void somarFretes(double v) { totalFretes += v; }

    public double getTotalEntradas() {
        return totalPassagens + totalEncomendas + totalFretes;
    }
    
    public double getLucroLiquido() {
        return getTotalEntradas() - totalSaidas;
    }

    // Getters e Setters
    public double getTotalPassagens() { return totalPassagens; }
    public void setTotalPassagens(double t) { this.totalPassagens = t; }

    public double getTotalEncomendas() { return totalEncomendas; }
    public void setTotalEncomendas(double t) { this.totalEncomendas = t; }
    
    public double getTotalFretes() { return totalFretes; }
    public void setTotalFretes(double t) { this.totalFretes = t; }

    public double getTotalSaidas() { return totalSaidas; }
    public void setTotalSaidas(double t) { this.totalSaidas = t; }

    public Map<String, Double> getSaidasPorCategoria() { return saidasPorCategoria; }
    public void setSaidasPorCategoria(Map<String, Double> m) { this.saidasPorCategoria = m; }
    
    public List<ItemResumoBalanco> getItensReceita() { return itensReceita; }

    public boolean isDadosIncompletos() { return dadosIncompletos; }
    public void marcarIncompleto(String secao, String erro) {
        this.dadosIncompletos = true;
        this.erroDetalhes += secao + ": " + erro + "; ";
    }
    public String getErroDetalhes() { return erroDetalhes; }
}