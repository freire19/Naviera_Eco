package com.naviera.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naviera.api.config.ApiException;
import com.naviera.api.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> TABELAS_PERMITIDAS = Set.of(
        "passageiros", "passagens", "viagens", "encomendas", "fretes", "cad_clientes_encomenda"
    );

    private static final Set<String> COLUNAS_SKIP_UPDATE = Set.of(
        "uuid", "sincronizado", "empresa_id"
    );

    public SyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public SyncResponse processar(SyncRequest request, Integer empresaId) {
        validarTabela(request.tabela());

        List<SyncRegistro> registros = request.registros() != null ? request.registros() : List.of();

        // 1. Processar upload (registros do Desktop → servidor)
        Set<String> uuidsRecebidos = new HashSet<>();
        int recebidos = 0;
        for (SyncRegistro reg : registros) {
            if (reg.uuid() != null) {
                uuidsRecebidos.add(reg.uuid());
                processarRegistro(request.tabela(), reg, empresaId);
                recebidos++;
            }
        }

        // 2. Buscar registros para download (servidor → Desktop)
        List<SyncRegistroDownload> paraDownload = buscarParaDownload(
            request.tabela(), request.ultimaSincronizacao(), empresaId, uuidsRecebidos);

        return new SyncResponse(true, "OK", recebidos, paraDownload.size(), paraDownload);
    }

    private void processarRegistro(String tabela, SyncRegistro reg, Integer empresaId) {
        String colunaId = getColunaId(tabela);

        // Verificar se existe no servidor
        List<Map<String, Object>> existente = jdbc.queryForList(
            "SELECT " + colunaId + ", ultima_atualizacao FROM " + tabela +
            " WHERE uuid = ?::uuid AND empresa_id = ?",
            reg.uuid(), empresaId);

        if ("DELETE".equals(reg.acao())) {
            if (!existente.isEmpty()) {
                jdbc.update("UPDATE " + tabela +
                    " SET excluido = TRUE, sincronizado = TRUE WHERE uuid = ?::uuid AND empresa_id = ?",
                    reg.uuid(), empresaId);
            }
            return;
        }

        // Parse dadosJson
        Map<String, Object> dados = parseDadosJson(reg.dadosJson());
        if (dados.isEmpty()) return;

        if (!existente.isEmpty()) {
            // UPDATE — last-write-wins
            Timestamp serverTime = (Timestamp) existente.get(0).get("ultima_atualizacao");
            if (reg.ultimaAtualizacao() != null && serverTime != null) {
                LocalDateTime uploadTime = LocalDateTime.parse(reg.ultimaAtualizacao());
                if (serverTime.toLocalDateTime().isAfter(uploadTime)) {
                    return; // servidor mais recente, skip
                }
            }

            // Build dynamic UPDATE
            List<String> setClauses = new ArrayList<>();
            List<Object> valores = new ArrayList<>();

            for (Map.Entry<String, Object> entry : dados.entrySet()) {
                String col = entry.getKey();
                if (COLUNAS_SKIP_UPDATE.contains(col) || col.equals(colunaId)) continue;
                setClauses.add(col + " = ?");
                valores.add(entry.getValue());
            }

            if (setClauses.isEmpty()) return;

            setClauses.add("sincronizado = TRUE");

            String sql = "UPDATE " + tabela + " SET " + String.join(", ", setClauses) +
                " WHERE uuid = ?::uuid AND empresa_id = ?";
            valores.add(reg.uuid());
            valores.add(empresaId);

            jdbc.update(sql, valores.toArray());

        } else {
            // INSERT
            List<String> colunas = new ArrayList<>();
            List<String> placeholders = new ArrayList<>();
            List<Object> valores = new ArrayList<>();

            for (Map.Entry<String, Object> entry : dados.entrySet()) {
                String col = entry.getKey();
                if (col.equals(colunaId)) continue; // skip auto-increment ID

                colunas.add(col);
                if ("uuid".equals(col)) {
                    placeholders.add("?::uuid");
                } else {
                    placeholders.add("?");
                }
                valores.add(entry.getValue());
            }

            // Garantir empresa_id e sincronizado
            if (!colunas.contains("empresa_id")) {
                colunas.add("empresa_id");
                placeholders.add("?");
                valores.add(empresaId);
            } else {
                // Sobrescrever empresa_id do upload com o do JWT
                int idx = colunas.indexOf("empresa_id");
                valores.set(idx, empresaId);
            }

            if (!colunas.contains("sincronizado")) {
                colunas.add("sincronizado");
                placeholders.add("?");
                valores.add(true);
            } else {
                int idx = colunas.indexOf("sincronizado");
                valores.set(idx, true);
            }

            String sql = "INSERT INTO " + tabela +
                " (" + String.join(", ", colunas) + ") VALUES (" +
                String.join(", ", placeholders) + ")";

            jdbc.update(sql, valores.toArray());
        }
    }

    private List<SyncRegistroDownload> buscarParaDownload(
            String tabela, String ultimaSincronizacao, Integer empresaId, Set<String> uuidsExcluir) {

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tabela + " WHERE empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);

        if (ultimaSincronizacao != null && !ultimaSincronizacao.isEmpty()) {
            sql.append(" AND ultima_atualizacao > ?::timestamp");
            params.add(Timestamp.valueOf(LocalDateTime.parse(ultimaSincronizacao)));
        }

        if (!uuidsExcluir.isEmpty()) {
            String placeholders = uuidsExcluir.stream().map(u -> "?::uuid").collect(Collectors.joining(","));
            sql.append(" AND uuid NOT IN (").append(placeholders).append(")");
            params.addAll(uuidsExcluir);
        }

        sql.append(" ORDER BY ultima_atualizacao LIMIT 1000");

        return jdbc.query(sql.toString(), (rs, i) -> {
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 1; c <= meta.getColumnCount(); c++) {
                row.put(meta.getColumnName(c), rs.getObject(c));
            }

            String uuid = row.get("uuid") != null ? row.get("uuid").toString() : null;
            Boolean excluido = (Boolean) row.get("excluido");
            String acao = Boolean.TRUE.equals(excluido) ? "DELETE" : "UPDATE";
            String ultimaAtt = row.get("ultima_atualizacao") != null ?
                row.get("ultima_atualizacao").toString() : null;

            String dadosJson;
            try {
                dadosJson = objectMapper.writeValueAsString(row);
            } catch (Exception e) {
                dadosJson = "{}";
            }

            return new SyncRegistroDownload(uuid, acao, ultimaAtt, dadosJson);
        }, params.toArray());
    }

    private Map<String, Object> parseDadosJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void validarTabela(String tabela) {
        if (!TABELAS_PERMITIDAS.contains(tabela)) {
            throw ApiException.badRequest("Tabela nao permitida para sync: " + tabela);
        }
    }

    private String getColunaId(String tabela) {
        return switch (tabela) {
            case "passageiros" -> "id_passageiro";
            case "passagens" -> "id_passagem";
            case "viagens" -> "id_viagem";
            case "encomendas" -> "id_encomenda";
            case "fretes" -> "id_frete";
            case "cad_clientes_encomenda" -> "id_cliente";
            default -> "id";
        };
    }
}
