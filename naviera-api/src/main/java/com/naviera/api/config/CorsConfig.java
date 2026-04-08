package com.naviera.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*; import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Value("${naviera.cors.allowed-origins}") private String[] origins;
    @Bean public CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.asList(origins));
        c.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(Arrays.asList("*")); c.setAllowCredentials(true);
        var s = new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**", c); return s;
    }
}
