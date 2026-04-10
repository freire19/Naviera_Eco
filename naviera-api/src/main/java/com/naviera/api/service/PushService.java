package com.naviera.api.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class PushService {
    private final JdbcTemplate jdbc;

    public PushService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> registrarDispositivo(Long clienteId, String tokenFcm, String plataforma) {
        // Remover tokens antigos do mesmo dispositivo
        jdbc.update("DELETE FROM dispositivos_push WHERE token_fcm = ?", tokenFcm);

        jdbc.update("""
            INSERT INTO dispositivos_push (id_cliente, token_fcm, plataforma, ativo)
            VALUES (?, ?, ?, TRUE)""",
            clienteId, tokenFcm, plataforma);

        return Map.of("mensagem", "Dispositivo registrado");
    }

    @Transactional
    public Map<String, Object> desregistrar(Long clienteId, String tokenFcm) {
        jdbc.update("DELETE FROM dispositivos_push WHERE id_cliente = ? AND token_fcm = ?",
            clienteId, tokenFcm);
        return Map.of("mensagem", "Dispositivo removido");
    }

    public void enviarNotificacao(Long clienteId, String titulo, String corpo) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<String> tokens = jdbc.queryForList(
            "SELECT token_fcm FROM dispositivos_push WHERE id_cliente = ? AND ativo = TRUE",
            String.class, clienteId);

        for (String token : tokens) {
            try {
                Message msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(corpo)
                        .build())
                    .build();
                FirebaseMessaging.getInstance().send(msg);
            } catch (Exception e) {
                System.err.println("[Push] Erro ao enviar para " + token + ": " + e.getMessage());
                // Token inválido — desativar
                jdbc.update("UPDATE dispositivos_push SET ativo = FALSE WHERE token_fcm = ?", token);
            }
        }
    }
}
