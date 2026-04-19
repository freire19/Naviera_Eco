package com.naviera.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    @Value("${naviera.cors.allowed-origins}") private String[] origins;

    @Bean public CorsConfigurationSource corsConfigurationSource() {
        // #DS5-406: rejeita "*" com allowCredentials=true (nao e valido em CORS) e origins vazios.
        List<String> list = Arrays.stream(origins)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        if (list.isEmpty()) {
            throw new IllegalStateException(
                "CORS_ORIGINS nao configurado — defina ex: https://app.naviera.com.br,https://admin.naviera.com.br");
        }
        for (String o : list) {
            if ("*".equals(o)) {
                throw new IllegalStateException(
                    "CORS_ORIGINS='*' e invalido com allowCredentials=true — liste dominios especificos");
            }
        }

        var c = new CorsConfiguration();
        c.setAllowedOrigins(list);
        c.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","X-Tenant-Slug","X-Requested-With"));
        c.setAllowCredentials(true);
        var s = new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**", c); return s;
    }
}
