package model;

public class ApiConfig {
    private int id;
    private String nomeServico;
    private String provider;
    // D020: transient evita serializacao acidental da chave
    private transient String apiKey;
    private String endpointUrl;
    private boolean ativo;

    public ApiConfig() {}

    public ApiConfig(String nomeServico, String apiKey) {
        this.nomeServico = nomeServico;
        this.apiKey = apiKey;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomeServico() { return nomeServico; }
    public void setNomeServico(String nomeServico) { this.nomeServico = nomeServico; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    // D020: toString NAO expoe apiKey
    @Override
    public String toString() {
        return "ApiConfig{servico=" + nomeServico + ", provider=" + provider + ", ativo=" + ativo + "}";
    }
}
