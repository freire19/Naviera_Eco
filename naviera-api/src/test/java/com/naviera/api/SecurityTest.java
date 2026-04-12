package com.naviera.api;

import com.naviera.api.config.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de seguranca que nao dependem do contexto Spring.
 * Valida rate limiting, IP parsing, e respostas 429.
 */
class SecurityTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void requisicaoGeralPermitidaDentroDolimite() throws Exception {
        for (int i = 0; i < 200; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/viagens");
            req.setRemoteAddr("172.16.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertNotEquals(429, res.getStatus(), "Request " + (i + 1) + " bloqueada indevidamente");
        }
    }

    @Test
    void requisicaoGeralBloqueadaApos200() throws Exception {
        for (int i = 0; i < 200; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/viagens");
            req.setRemoteAddr("172.16.0.2");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/viagens");
        req.setRemoteAddr("172.16.0.2");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(429, res.getStatus());
    }

    @Test
    void loginTemLimiteSeparadoDe10() throws Exception {
        // Login: limite 10
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("172.16.0.3");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertNotEquals(429, res.getStatus());
        }

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("172.16.0.3");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(429, res.getStatus());

        // GET geral do mesmo IP ainda funciona (contador separado)
        req = new MockHttpServletRequest("GET", "/api/viagens");
        req.setRemoteAddr("172.16.0.3");
        res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void respostaRateLimitTemFormatoCorreto() throws Exception {
        for (int i = 0; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("172.16.0.4");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("172.16.0.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(429, res.getStatus());
        assertEquals("application/json", res.getContentType());
        assertTrue(res.getContentAsString().contains("\"error\""));
    }
}
