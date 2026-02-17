package model;

public class ItemResumoBalanco {
    private String tipo;      // Ex: "Passagem"
    private String rota;      // Ex: "Manaus / Jutaí"
    private int quantidade;   // Ex: 15
    private double valorTotal; // Ex: 500.00

    public ItemResumoBalanco(String tipo, String rota, int quantidade, double valorTotal) {
        this.tipo = tipo;
        this.rota = rota;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal;
    }

    public String getDescricaoFormatada() {
        // Formato: "15 Passagens (Manaus / Jutaí) = R$ 500,00"
        return String.format("%02d %s (%s) = R$ %.2f", quantidade, tipo, rota, valorTotal);
    }
}