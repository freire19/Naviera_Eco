package com.naviera.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    @Value("${naviera.jwt.secret}") private String secret;
    @Value("${naviera.jwt.expiration-ms}") private long expirationMs;

    // #DS5-405: rejeitar JWT_SECRET fraco no boot — HS256 exige >= 256 bits de entropia.
    private static final List<String> PADROES_FRACOS = List.of(
        "dev", "local", "naviera", "secret", "changeme", "default", "test", "123", "password"
    );

    private SecretKey cachedKey;

    @PostConstruct
    void validarSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET ausente — defina antes de iniciar a API");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET muito curto (< 32 bytes) — use 'openssl rand -base64 48'");
        }
        String lower = secret.toLowerCase();
        for (String padrao : PADROES_FRACOS) {
            if (lower.contains(padrao)) {
                throw new IllegalStateException(
                    "JWT_SECRET contem padrao previsivel ('" + padrao + "') — gere um secret aleatorio real");
            }
        }
        // SecretKey e imutavel e thread-safe — pre-derivada uma vez evita alocacao por parsear()/sign().
        cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey key() { return cachedKey; }

    public String gerarToken(Long clienteId, String documento, String tipo) {
        return Jwts.builder().subject(documento)
            .claim("id", clienteId).claim("tipo", tipo)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key()).compact();
    }

    public String gerarTokenOperador(Integer usuarioId, String login, String funcao, Integer empresaId, boolean superAdmin) {
        return Jwts.builder().subject(login)
            .claim("id", usuarioId).claim("tipo", "OPERADOR").claim("funcao", funcao)
            .claim("empresa_id", empresaId).claim("super_admin", superAdmin)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key()).compact();
    }
    public Claims parsear(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    public boolean validar(String token) { try { parsear(token); return true; } catch (JwtException e) { return false; } }
    public Long getClienteId(String token) { return parsear(token).get("id", Long.class); }
    public String getTipo(String token) { return parsear(token).get("tipo", String.class); }
    public Integer getEmpresaId(String token) { return parsear(token).get("empresa_id", Integer.class); }
}
