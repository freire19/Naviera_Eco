package com.naviera.api.config;

import com.naviera.api.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import java.net.InetAddress;

@Configuration @EnableWebSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;
    public SecurityConfig(JwtFilter jwtFilter, RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(c -> {}).csrf(c -> c.disable())
            // #DS5-011: HSTS + frame-ancestors none + CSP minima + Referrer-Policy.
            //   API serve JSON puro, entao default-src 'none' e seguro. Se algum endpoint passar
            //   a renderizar HTML (admin embed), abrir CSP por filtro especifico.
            .headers(h -> h
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31_536_000).includeSubDomains(true).preload(true))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .permissionsPolicy(p -> p.policy("camera=(), geolocation=(), microphone=()"))
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // #DS5-022: 401/403 retornam JSON ({"error": "..."}) em vez do HTML default do Spring.
            .exceptionHandling(e -> e.authenticationEntryPoint(jsonAuthEntryPoint()))
            .authorizeHttpRequests(a -> a
                // #DS5-044: bloquear TRACE/CONNECT (Tomcat ja barra; este matcher e defesa em profundidade).
                .requestMatchers(req -> {
                    String m = req.getMethod();
                    return "TRACE".equalsIgnoreCase(m) || "CONNECT".equalsIgnoreCase(m);
                }).denyAll()
                .requestMatchers("/auth/**", "/public/**", "/ws/**", "/webhooks/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/embarcacoes/*/gps", "/viagens/*/rastreio", "/viagens/publicas", "/gps/embarcacoes").permitAll()
                // #DS5-441: actuator so para loopback verdadeiro (rejeita XFF spoof).
                .requestMatchers("/actuator/**").access((auth, ctx) -> {
                    HttpServletRequest req = ctx.getRequest();
                    boolean local = isLoopback(req.getRemoteAddr())
                        && req.getHeader("X-Forwarded-For") == null;
                    return new org.springframework.security.authorization.AuthorizationDecision(local);
                })
                .requestMatchers("/admin/**").hasAuthority("ROLE_SUPERADMIN")
                // #104: /psp/onboarding cria subconta Asaas com dados financeiros — restringir a ADMIN
                //   da empresa (operador comum nao deve poder cadastrar conta de recebimento).
                .requestMatchers("/psp/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/gps/**").hasAuthority("ROLE_OPERADOR")
                .requestMatchers("/op/**", "/sync/**").hasAuthority("ROLE_OPERADOR")
                .anyRequest().authenticated())
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (HttpServletRequest req, HttpServletResponse res, org.springframework.security.core.AuthenticationException ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"error\":\"Nao autorizado\"}");
        };
    }

    private static boolean isLoopback(String ip) {
        if (ip == null) return false;
        try { return InetAddress.getByName(ip).isLoopbackAddress(); }
        catch (Exception e) { return false; }
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    // Evita registro duplo do filter (Spring auto-registra @Component como servlet filter)
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
