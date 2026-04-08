package com.naviera.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${naviera.jwt.secret}") private String secret;
    @Value("${naviera.jwt.expiration-ms}") private long expirationMs;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    public String gerarToken(Long clienteId, String documento, String tipo) {
        return Jwts.builder().subject(documento)
            .claim("id", clienteId).claim("tipo", tipo)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key()).compact();
    }
    public Claims parsear(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    public boolean validar(String token) { try { parsear(token); return true; } catch (JwtException e) { return false; } }
    public Long getClienteId(String token) { return parsear(token).get("id", Long.class); }
    public String getTipo(String token) { return parsear(token).get("tipo", String.class); }
}
