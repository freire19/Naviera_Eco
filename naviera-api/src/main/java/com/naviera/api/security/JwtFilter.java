package com.naviera.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.ArrayList; import java.util.Map;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    public JwtFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                String token = h.substring(7);
                Claims claims = parseOrNull(token);
                if (claims != null) {
                    String tipo = claims.get("tipo", String.class);
                    var authorities = new ArrayList<SimpleGrantedAuthority>();
                    if ("OPERADOR".equals(tipo)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_OPERADOR"));
                    } else if ("CNPJ".equals(tipo)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_CNPJ"));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_CPF"));
                    }
                    boolean superAdmin = false;
                    if ("OPERADOR".equals(tipo)) {
                        // ROLE_ADMIN = admin-empresa (/op/**, /psp/**); ROLE_SUPERADMIN = admin-plataforma
                        // (/admin/** cross-tenant), so quando usuarios.super_admin=TRUE no DB (fix #100/#114).
                        // #DS5-213: comparar a forma canonica (trim + uppercase) contra um set fechado;
                        //   evita aceitar "adm", "ADMINISTRATOR", "admin " ou variacoes que devem ser rejeitadas.
                        String funcao = claims.get("funcao", String.class);
                        if (funcao != null) {
                            String norm = funcao.trim().toUpperCase();
                            if ("ADMIN".equals(norm) || "ADMINISTRADOR".equals(norm)) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            }
                        }
                        superAdmin = Boolean.TRUE.equals(claims.get("super_admin", Boolean.class));
                        if (superAdmin) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
                        }
                    }
                    Long clienteId = claims.get("id", Long.class);
                    var auth = new UsernamePasswordAuthenticationToken(clienteId, null, authorities);
                    if ("OPERADOR".equals(tipo)) {
                        auth.setDetails(Map.of("empresa_id", claims.get("empresa_id", Integer.class), "super_admin", superAdmin));
                    }
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Claims parseOrNull(String token) {
        try { return jwtUtil.parsear(token); } catch (JwtException e) { return null; }
    }
}
