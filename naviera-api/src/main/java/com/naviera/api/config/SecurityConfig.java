package com.naviera.api.config;

import com.naviera.api.security.JwtFilter;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

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
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/auth/**", "/public/**", "/ws/**", "/webhooks/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/embarcacoes/*/gps", "/viagens/*/rastreio", "/viagens/publicas", "/gps/embarcacoes").permitAll()
                .requestMatchers("/admin/**").hasAuthority("ROLE_SUPERADMIN")
                .requestMatchers("/gps/**").hasAuthority("ROLE_OPERADOR")
                .requestMatchers("/op/**", "/sync/**").hasAuthority("ROLE_OPERADOR")
                .anyRequest().authenticated())
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
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
