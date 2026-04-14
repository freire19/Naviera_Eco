package com.naviera.api.security;

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
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            if (jwtUtil.validar(token)) {
                String tipo = jwtUtil.getTipo(token);
                String role;
                if ("OPERADOR".equals(tipo)) {
                    role = "ROLE_OPERADOR";
                } else if ("CNPJ".equals(tipo)) {
                    role = "ROLE_CNPJ";
                } else {
                    role = "ROLE_CPF";
                }
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority(role));
                if ("OPERADOR".equals(tipo)) {
                    // Operadores com funcao ADMIN ganham ROLE_ADMIN adicional
                    io.jsonwebtoken.Claims claims = jwtUtil.parsear(token);
                    String funcao = claims.get("funcao", String.class);
                    if (funcao != null && ("ADMIN".equalsIgnoreCase(funcao) || "Administrador".equalsIgnoreCase(funcao))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                }
                var auth = new UsernamePasswordAuthenticationToken(jwtUtil.getClienteId(token), null, authorities);
                if ("OPERADOR".equals(tipo)) {
                    auth.setDetails(Map.of("empresa_id", jwtUtil.getEmpresaId(token)));
                }
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
