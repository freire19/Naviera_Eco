package com.naviera.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            System.out.println("[Firebase] Credenciais nao configuradas. Push notifications desativadas.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) return;

        try (FileInputStream fis = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(fis))
                .build();
            FirebaseApp.initializeApp(options);
            System.out.println("[Firebase] Inicializado com sucesso.");
        } catch (Exception e) {
            System.err.println("[Firebase] Erro ao inicializar: " + e.getMessage());
        }
    }
}
