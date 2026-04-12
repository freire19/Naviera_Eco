package com.naviera.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter em memoria por IP.
 * Limite geral: 200 req/min por IP.
 * Limite login: 10 req/min por IP.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int GENERAL_MAX = 200;
    private static final int LOGIN_MAX = 10;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateEntry> hits = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleaner;

    @PostConstruct
    void startCleaner() {
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            hits.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS);
        }, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    void stopCleaner() {
        if (cleaner != null) cleaner.shutdownNow();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String ip = getClientIp(req);
        String path = req.getRequestURI();

        boolean isLogin = path.endsWith("/auth/login") && "POST".equalsIgnoreCase(req.getMethod());
        int max = isLogin ? LOGIN_MAX : GENERAL_MAX;
        String key = isLogin ? "login:" + ip : "general:" + ip;

        RateEntry entry = hits.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (entry.count.get() > max) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Muitas requisicoes. Tente novamente em breve.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static class RateEntry {
        final long windowStart;
        final AtomicInteger count;

        RateEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

}
