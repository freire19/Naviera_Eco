package model;

public class ApiConfig {
    private int id;
    private String nomeServico;
    private String provider;
    private String apiKey;
    private String endpointUrl;
    private boolean ativo;

    // Construtores, Getters e Setters padrão...
    public ApiConfig() {}
    
    public ApiConfig(String nomeServico, String apiKey) {
        this.nomeServico = nomeServico;
        this.apiKey = apiKey;
    }

    // Gere os Getters e Setters aqui pelo Eclipse (Source > Generate Getters and Setters)
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isAtivo() { return ativo; }
    // ... outros getters e setters
}