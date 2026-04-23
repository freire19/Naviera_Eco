package com.naviera.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    private final Environment env;

    public FirebaseConfig(Environment env) {
        this.env = env;
    }

    // #311: em prod, credenciais ausentes ou init com erro sao falha fatal — push e feature critica.
    @PostConstruct
    public void init() {
        boolean prod = env.acceptsProfiles(Profiles.of("prod"));
        if (credentialsPath == null || credentialsPath.isBlank()) {
            if (prod) throw new IllegalStateException("Firebase credentials.path ausente em prod");
            log.warn("[Firebase] Credenciais nao configuradas (dev). Push desativadas.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) return;

        try (FileInputStream fis = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(fis))
                .build();
            FirebaseApp.initializeApp(options);
            log.info("[Firebase] Inicializado com sucesso.");
        } catch (Exception e) {
            if (prod) throw new IllegalStateException("Firebase init falhou em prod: " + e.getMessage(), e);
            log.error("[Firebase] Erro ao inicializar: {}", e.getMessage(), e);
        }
    }
}
