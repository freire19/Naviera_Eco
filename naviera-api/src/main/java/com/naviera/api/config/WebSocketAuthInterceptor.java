package com.naviera.api.config;

import com.naviera.api.security.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DS4-003 fix: Valida JWT no STOMP CONNECT e restringe subscriptions por empresa_id.
 * Sem token valido, a conexao e rejeitada.
 * Subscriptions a /topic/empresa/{id}/notifications so permitidas se o empresa_id do token bater.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // #DS5-013: token guardado na sessao STOMP no CONNECT, revalidado em cada SUBSCRIBE/SEND.
    private static final String SESSION_TOKEN = "naviera.ws.token";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand cmd = accessor.getCommand();
        if (StompCommand.CONNECT.equals(cmd)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(cmd) || StompCommand.SEND.equals(cmd)) {
            revalidarSessao(accessor);
            if (StompCommand.SUBSCRIBE.equals(cmd)) {
                validarTenantSubscription(accessor);
            }
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token de autenticacao obrigatorio para WebSocket");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validar(token)) {
            throw new IllegalArgumentException("Token invalido ou expirado");
        }

        Integer empresaId = jwtUtil.getEmpresaId(token);
        Long userId = jwtUtil.getClienteId(token);
        String tipo = jwtUtil.getTipo(token);

        var auth = new UsernamePasswordAuthenticationToken(
            userId, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + (tipo != null ? tipo : "CPF")))
        );
        auth.setDetails(Map.of("empresa_id", empresaId != null ? empresaId : 0));
        accessor.setUser(auth);

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) sessionAttrs.put(SESSION_TOKEN, token);
    }

    private void revalidarSessao(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        String token = sessionAttrs != null ? (String) sessionAttrs.get(SESSION_TOKEN) : null;
        if (token == null || !jwtUtil.validar(token)) {
            throw new IllegalArgumentException("Sessao expirada — reconecte");
        }
    }

    private void validarTenantSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/empresa/")) return;

        var auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        if (auth == null) {
            throw new IllegalArgumentException("Autenticacao obrigatoria para subscription");
        }

        String[] parts = destination.split("/");
        if (parts.length < 4) return;

        try {
            int requestedEmpresa = Integer.parseInt(parts[3]);
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            int userEmpresa = details != null ? ((Number) details.get("empresa_id")).intValue() : 0;
            if (requestedEmpresa != userEmpresa) {
                throw new IllegalArgumentException(
                    "Acesso negado: nao pode subscrever notificacoes de outra empresa");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("empresa_id invalido na destination");
        }
    }
}
