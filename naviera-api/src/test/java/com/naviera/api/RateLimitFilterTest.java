package com.naviera.api;

import com.naviera.api.config.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void primeiraRequisicaoPermitida() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/viagens");
        req.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertNotEquals(429, res.getStatus());
    }

    @Test
    void loginBloqueadoApos10Tentativas() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertNotEquals(429, res.getStatus(), "Request " + (i + 1) + " nao deveria ser bloqueada");
        }

        // 11a tentativa deve ser bloqueada
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertEquals(429, res.getStatus());
    }

    @Test
    void ipsDistintosNaoInterferem() throws Exception {
        // Esgota limite do IP A
        for (int i = 0; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.2");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // IP B deve continuar funcionando
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void xForwardedForUsadoComoIp() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/viagens");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "200.100.50.1, 10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void retorna429ComBodyJson() throws Exception {
        for (int i = 0; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr("10.0.0.99");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(429, res.getStatus());
        assertEquals("application/json", res.getContentType());
        assertTrue(res.getContentAsString().contains("error"));
    }
}
