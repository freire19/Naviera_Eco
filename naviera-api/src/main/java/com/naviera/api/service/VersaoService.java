package com.naviera.api.service;

import com.naviera.api.dto.VersaoCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VersaoService {
    private final JdbcTemplate jdbc;

    // Fallback: versoes configuradas em application.properties (usadas se tabela vazia ou sem registro para plataforma)
    @Value("${naviera.versao.desktop:1.0.0}")
    private String versaoDesktopFallback;

    @Value("${naviera.versao.web:1.0.0}")
    private String versaoWebFallback;

    @Value("${naviera.versao.app:1.0.0}")
    private String versaoAppFallback;

    @Value("${naviera.versao.desktop.url-download:}")
    private String urlDownloadDesktopFallback;

    @Value("${naviera.versao.desktop.obrigatoria:false}")
    private boolean obrigatoriaDesktopFallback;

    public VersaoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Verifica se ha atualizacao para a plataforma informada.
     * Busca primeiro na tabela versao_sistema filtrando por plataforma.
     * Se a tabela nao tiver coluna plataforma ou nao houver registro, usa fallback sem filtro.
     */
    public VersaoCheckResponse check(String plataforma, String versaoCliente) {
        // Tentar busca com filtro de plataforma
        List<Map<String, Object>> rows = buscarVersaoComPlataforma(plataforma);

        // Fallback: busca sem filtro de plataforma (compatibilidade com tabela antiga)
        if (rows.isEmpty()) {
            rows = buscarVersaoSemPlataforma();
        }

        if (rows.isEmpty()) {
            // Nenhum registro na tabela — usar valores do application.properties
            return checkComFallback(plataforma, versaoCliente);
        }

        Map<String, Object> ultima = rows.get(0);
        String versaoNova = (String) ultima.get("versao");
        Boolean obrigatoria = (Boolean) ultima.getOrDefault("obrigatoria", false);
        String urlDownload = (String) ultima.get("url_download");
        String changelog = (String) ultima.get("changelog");

        boolean atualizado = compararVersoes(versaoCliente, versaoNova) >= 0;

        return new VersaoCheckResponse(atualizado, versaoCliente, versaoNova,
            Boolean.TRUE.equals(obrigatoria), urlDownload, changelog);
    }

    /**
     * Compatibilidade: check sem plataforma (usa "desktop" como padrao).
     */
    public VersaoCheckResponse check(String versaoCliente) {
        return check("desktop", versaoCliente);
    }

    /**
     * Retorna informacoes da versao atual do servidor para todas as plataformas.
     */
    public Map<String, Object> versaoAtual() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String plat : List.of("desktop", "web", "app")) {
            List<Map<String, Object>> rows = buscarVersaoComPlataforma(plat);
            if (rows.isEmpty()) rows = buscarVersaoSemPlataforma();

            Map<String, Object> info = new LinkedHashMap<>();
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                info.put("versao", row.get("versao"));
                info.put("obrigatoria", row.getOrDefault("obrigatoria", false));
                info.put("urlDownload", row.get("url_download"));
                info.put("changelog", row.get("changelog"));
            } else {
                // Fallback do application.properties
                info.put("versao", versaoFallback(plat));
                info.put("obrigatoria", false);
                info.put("urlDownload", "desktop".equals(plat) ? urlDownloadDesktopFallback : null);
                info.put("changelog", null);
            }
            result.put(plat, info);
        }

        return result;
    }

    // ── Metodos internos ──

    private List<Map<String, Object>> buscarVersaoComPlataforma(String plataforma) {
        try {
            return jdbc.queryForList(
                "SELECT versao, obrigatoria, url_download, changelog " +
                "FROM versao_sistema WHERE plataforma = ? ORDER BY data_publicacao DESC LIMIT 1",
                plataforma);
        } catch (Exception e) {
            // Coluna plataforma pode nao existir ainda — fallback silencioso
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> buscarVersaoSemPlataforma() {
        try {
            return jdbc.queryForList(
                "SELECT versao, obrigatoria, url_download, changelog " +
                "FROM versao_sistema ORDER BY data_publicacao DESC LIMIT 1");
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private VersaoCheckResponse checkComFallback(String plataforma, String versaoCliente) {
        String versaoServer = versaoFallback(plataforma);
        boolean atualizado = compararVersoes(versaoCliente, versaoServer) >= 0;
        boolean obrigatoria = "desktop".equals(plataforma) && obrigatoriaDesktopFallback;
        String urlDownload = "desktop".equals(plataforma) ? urlDownloadDesktopFallback : null;
        return new VersaoCheckResponse(atualizado, versaoCliente, versaoServer, obrigatoria, urlDownload, null);
    }

    private String versaoFallback(String plataforma) {
        return switch (plataforma) {
            case "web" -> versaoWebFallback;
            case "app" -> versaoAppFallback;
            default -> versaoDesktopFallback;
        };
    }

    /** Compara versoes semanticas (ex: "1.2.3"). Retorna negativo se a < b, 0 se igual, positivo se a > b. */
    private int compararVersoes(String a, String b) {
        if (a == null || b == null) return 0;
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            // #DS5-027: parts nao numericas (ex: "1.2.x", "1.2-rc") sao tratadas como 0 em vez de crash.
            int va = parseSegmento(i < partsA.length ? partsA[i] : "0");
            int vb = parseSegmento(i < partsB.length ? partsB[i] : "0");
            if (va != vb) return va - vb;
        }
        return 0;
    }

    private int parseSegmento(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D.*$", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
