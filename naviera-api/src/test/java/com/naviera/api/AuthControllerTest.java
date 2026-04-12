package com.naviera.api;

import com.naviera.api.config.RateLimitFilter;
import com.naviera.api.controller.AuthController;
import com.naviera.api.dto.AuthResponse;
import com.naviera.api.security.JwtFilter;
import com.naviera.api.security.JwtUtil;
import com.naviera.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private JwtFilter jwtFilter;
    @MockBean private RateLimitFilter rateLimitFilter;
    @MockBean private PasswordEncoder passwordEncoder;

    @Test
    void loginComCredenciaisValidas() throws Exception {
        when(authService.login(any())).thenReturn(
            new AuthResponse("token123", "CPF", "Joao", 1L));

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\",\"senha\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token123"))
            .andExpect(jsonPath("$.nome").value("Joao"))
            .andExpect(jsonPath("$.tipo").value("CPF"));
    }

    @Test
    void loginSemDocumentoRetorna400() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senha\":\"123456\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginSemSenhaRetorna400() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginBodyVazioRetorna400() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registrarComDadosValidos() throws Exception {
        when(authService.registrar(any())).thenReturn(
            new AuthResponse("token456", "CPF", "Maria", 2L));

        mvc.perform(post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\",\"nome\":\"Maria\",\"senha\":\"123456\",\"tipoDocumento\":\"CPF\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token456"))
            .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void registrarSemNomeRetorna400() throws Exception {
        mvc.perform(post("/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documento\":\"12345678901\",\"senha\":\"123456\"}"))
            .andExpect(status().isBadRequest());
    }
}
