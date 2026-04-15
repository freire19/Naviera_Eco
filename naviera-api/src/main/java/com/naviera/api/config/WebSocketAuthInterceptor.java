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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Exigir JWT no header "Authorization" do STOMP CONNECT
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

        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // Restringir subscriptions por empresa_id
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/empresa/")) {
                var auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
                if (auth == null) {
                    throw new IllegalArgumentException("Autenticacao obrigatoria para subscription");
                }

                // Extrair empresa_id da destination: /topic/empresa/{id}/notifications
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
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
        }

        return message;
    }
}
