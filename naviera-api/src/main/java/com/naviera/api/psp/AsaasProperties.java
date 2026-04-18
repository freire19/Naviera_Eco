package com.naviera.api.psp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "naviera.psp")
public class AsaasProperties {
    private String provider = "asaas";
    private BigDecimal splitNavieraPct = new BigDecimal("1.50");
    private Asaas asaas = new Asaas();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public BigDecimal getSplitNavieraPct() { return splitNavieraPct; }
    public void setSplitNavieraPct(BigDecimal splitNavieraPct) { this.splitNavieraPct = splitNavieraPct; }
    public Asaas getAsaas() { return asaas; }
    public void setAsaas(Asaas asaas) { this.asaas = asaas; }

    public static class Asaas {
        private String baseUrl = "https://sandbox.asaas.com/api/v3";
        private String apiKey = "";
        private String webhookSecret = "";
        private String navieraSubcontaId = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
        public String getNavieraSubcontaId() { return navieraSubcontaId; }
        public void setNavieraSubcontaId(String navieraSubcontaId) { this.navieraSubcontaId = navieraSubcontaId; }
    }
}
