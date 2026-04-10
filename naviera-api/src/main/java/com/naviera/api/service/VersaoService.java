package com.naviera.api.service;

import com.naviera.api.dto.VersaoCheckResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class VersaoService {
    private final JdbcTemplate jdbc;

    public VersaoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VersaoCheckResponse check(String versaoCliente) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT versao, obrigatoria, url_download, changelog FROM versao_sistema ORDER BY data_publicacao DESC LIMIT 1");

        if (rows.isEmpty()) {
            return new VersaoCheckResponse(true, versaoCliente, versaoCliente, false, null, null);
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

    /** Compara versões semânticas (ex: "1.2.3"). Retorna negativo se a < b, 0 se igual, positivo se a > b. */
    private int compararVersoes(String a, String b) {
        if (a == null || b == null) return 0;
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            int va = i < partsA.length ? Integer.parseInt(partsA[i]) : 0;
            int vb = i < partsB.length ? Integer.parseInt(partsB[i]) : 0;
            if (va != vb) return va - vb;
        }
        return 0;
    }
}
