package com.naviera.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naviera.api.config.ApiException;
import com.naviera.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---- Tabelas permitidas para sync bidirecional ----
    private static final Set<String> TABELAS_PERMITIDAS = Set.of(
        "viagens",
        "passagens",
        "encomendas",
        "fretes",
        "financeiro_saidas",
        "passageiros",
        "conferentes",
        "caixas",
        "rotas",
        "embarcacoes",
        "tarifas"
    );

    // Colunas que nunca sao atualizadas via upload (gerenciadas pelo servidor)
    private static final Set<String> COLUNAS_SKIP_UPDATE = Set.of(
        "uuid", "sincronizado", "empresa_id"
    );

    // Colunas de controle de sync (nao enviar no dadosJson de download se nao forem uteis)
    private static final Set<String> COLUNAS_CONTROLE = Set.of(
        "sincronizado"
    );

    // Mapa: tabela -> coluna PK auto-increment (para skip no INSERT)
    private static final Map<String, String> COLUNA_ID = Map.ofEntries(
        Map.entry("viagens", "id_viagem"),
        Map.entry("passagens", "id_passagem"),
        Map.entry("encomendas", "id_encomenda"),
        Map.entry("fretes", "id_frete"),
        Map.entry("financeiro_saidas", "id"),
        Map.entry("passageiros", "id_passageiro"),
        Map.entry("conferentes", "id_conferente"),
        Map.entry("caixas", "id_caixa"),
        Map.entry("rotas", "id"),
        Map.entry("embarcacoes", "id_embarcacao"),
        Map.entry("tarifas", "id_tarifa")
    );

    // Tabelas que usam 'excluido' para soft-delete
    private static final Set<String> TABELAS_COM_EXCLUIDO = Set.of(
        "viagens", "passagens", "encomendas", "fretes",
        "passageiros", "conferentes", "caixas", "rotas",
        "embarcacoes", "tarifas"
    );

    // Tabelas que usam 'is_excluido' em vez de 'excluido' (financeiro_saidas)
    private static final Set<String> TABELAS_COM_IS_EXCLUIDO = Set.of(
        "financeiro_saidas"
    );

    public SyncService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Processa sync bidirecional para uma tabela:
     * 1) Upload: recebe registros do Desktop, aplica INSERT/UPDATE/DELETE
     * 2) Download: retorna registros alterados no servidor desde ultima sync
     */
    @Transactional
    public SyncResponse processar(SyncRequest request, Integer empresaId) {
        validarTabela(request.tabela());
        String tabela = request.tabela();

        List<SyncRegistro> registros = request.registros() != null ? request.registros() : List.of();

        // ---- 1. UPLOAD: processar registros vindos do Desktop ----
        Set<String> uuidsRecebidos = new HashSet<>();
        int recebidos = 0;
        int erros = 0;

        for (SyncRegistro reg : registros) {
            if (reg.uuid() == null || reg.uuid().isBlank()) continue;

            try {
                uuidsRecebidos.add(reg.uuid());
                processarRegistro(tabela, reg, empresaId);
                recebidos++;
            } catch (Exception e) {
                erros++;
                log.warn("Sync upload erro [tabela={}, uuid={}, acao={}]: {}",
                    tabela, reg.uuid(), reg.acao(), e.getMessage());
            }
        }

        // ---- 2. DOWNLOAD: buscar registros alterados no servidor ----
        List<SyncRegistroDownload> paraDownload = buscarParaDownload(
            tabela, request.ultimaSincronizacao(), empresaId, uuidsRecebidos);

        String mensagem = erros > 0
            ? String.format("OK com %d erro(s)", erros)
            : "OK";

        return new SyncResponse(erros == 0, mensagem, recebidos, paraDownload.size(), paraDownload);
    }

    // ========================================================================
    // UPLOAD: processar um registro individual
    // ========================================================================

    private void processarRegistro(String tabela, SyncRegistro reg, Integer empresaId) {
        String colunaId = getColunaId(tabela);

        // Verificar se o registro ja existe no servidor (por uuid + empresa_id)
        List<Map<String, Object>> existente = jdbc.queryForList(
            "SELECT " + colunaId + ", ultima_atualizacao FROM " + tabela
                + " WHERE uuid = ?::uuid AND empresa_id = ?",
            reg.uuid(), empresaId);

        String acao = reg.acao() != null ? reg.acao().toUpperCase() : "UPDATE";

        switch (acao) {
            case "DELETE" -> processarDelete(tabela, reg, empresaId, existente);
            case "INSERT" -> processarInsertOuUpdate(tabela, reg, empresaId, colunaId, existente);
            case "UPDATE" -> processarInsertOuUpdate(tabela, reg, empresaId, colunaId, existente);
            default -> log.warn("Acao desconhecida: {} para uuid {}", acao, reg.uuid());
        }
    }

    private void processarDelete(String tabela, SyncRegistro reg, Integer empresaId,
                                  List<Map<String, Object>> existente) {
        if (existente.isEmpty()) return; // nada para deletar

        if (TABELAS_COM_EXCLUIDO.contains(tabela)) {
            jdbc.update(
                "UPDATE " + tabela
                    + " SET excluido = TRUE, sincronizado = TRUE, ultima_atualizacao = CURRENT_TIMESTAMP"
                    + " WHERE uuid = ?::uuid AND empresa_id = ?",
                reg.uuid(), empresaId);
        } else if (TABELAS_COM_IS_EXCLUIDO.contains(tabela)) {
            jdbc.update(
                "UPDATE " + tabela
                    + " SET is_excluido = TRUE, sincronizado = TRUE, ultima_atualizacao = CURRENT_TIMESTAMP"
                    + " WHERE uuid = ?::uuid AND empresa_id = ?",
                reg.uuid(), empresaId);
        } else {
            // Hard delete para tabelas sem flag de exclusao
            jdbc.update(
                "DELETE FROM " + tabela + " WHERE uuid = ?::uuid AND empresa_id = ?",
                reg.uuid(), empresaId);
        }
    }

    private void processarInsertOuUpdate(String tabela, SyncRegistro reg, Integer empresaId,
                                          String colunaId, List<Map<String, Object>> existente) {
        Map<String, Object> dados = parseDadosJson(reg.dadosJson());
        if (dados.isEmpty()) {
            log.warn("dadosJson vazio para uuid {} na tabela {}", reg.uuid(), tabela);
            return;
        }

        if (!existente.isEmpty()) {
            // ---- UPDATE (last-write-wins) ----
            if (!isClienteNewer(reg.ultimaAtualizacao(), existente.get(0))) {
                return; // servidor tem versao mais recente, skip
            }
            executarUpdate(tabela, reg.uuid(), empresaId, colunaId, dados);
        } else {
            // ---- INSERT ----
            executarInsert(tabela, reg.uuid(), empresaId, colunaId, dados);
        }
    }

    /**
     * Retorna true se o timestamp do cliente e mais recente que o do servidor.
     * Se nao for possivel comparar, assume que o cliente e mais recente (aceita o upload).
     */
    private boolean isClienteNewer(String clienteTimestamp, Map<String, Object> serverRow) {
        if (clienteTimestamp == null || clienteTimestamp.isBlank()) return true;

        Object serverObj = serverRow.get("ultima_atualizacao");
        if (serverObj == null) return true;

        try {
            LocalDateTime clienteTime = parseTimestamp(clienteTimestamp);
            LocalDateTime serverTime;

            if (serverObj instanceof Timestamp ts) {
                serverTime = ts.toLocalDateTime();
            } else {
                serverTime = parseTimestamp(serverObj.toString());
            }

            return !serverTime.isAfter(clienteTime); // cliente >= servidor => aceita
        } catch (DateTimeParseException e) {
            log.warn("Erro ao comparar timestamps (cliente={}, servidor={}): {}",
                clienteTimestamp, serverObj, e.getMessage());
            return true; // na duvida, aceita
        }
    }

    private LocalDateTime parseTimestamp(String ts) {
        // Suporta tanto "2026-04-10T14:30:00" quanto "2026-04-10 14:30:00"
        String normalized = ts.replace(" ", "T");
        // Remove microssegundos extras se houver (PostgreSQL pode enviar 6 decimais)
        if (normalized.contains(".") && normalized.indexOf('.') < normalized.length() - 7) {
            normalized = normalized.substring(0, normalized.indexOf('.') + 7);
        }
        return LocalDateTime.parse(normalized);
    }

    private void executarUpdate(String tabela, String uuid, Integer empresaId,
                                 String colunaId, Map<String, Object> dados) {
        List<String> setClauses = new ArrayList<>();
        List<Object> valores = new ArrayList<>();

        for (Map.Entry<String, Object> entry : dados.entrySet()) {
            String col = sanitizeColumnName(entry.getKey());
            if (col == null) continue;
            if (COLUNAS_SKIP_UPDATE.contains(col) || col.equals(colunaId)) continue;

            setClauses.add(col + " = ?");
            valores.add(convertValue(col, entry.getValue()));
        }

        if (setClauses.isEmpty()) return;

        // Marcar como sincronizado e atualizar timestamp
        setClauses.add("sincronizado = TRUE");
        setClauses.add("ultima_atualizacao = CURRENT_TIMESTAMP");

        String sql = "UPDATE " + tabela + " SET " + String.join(", ", setClauses)
            + " WHERE uuid = ?::uuid AND empresa_id = ?";
        valores.add(uuid);
        valores.add(empresaId);

        jdbc.update(sql, valores.toArray());
    }

    private void executarInsert(String tabela, String uuid, Integer empresaId,
                                 String colunaId, Map<String, Object> dados) {
        List<String> colunas = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> valores = new ArrayList<>();

        boolean hasUuid = false;

        for (Map.Entry<String, Object> entry : dados.entrySet()) {
            String col = sanitizeColumnName(entry.getKey());
            if (col == null) continue;
            if (col.equals(colunaId)) continue; // skip auto-increment PK

            if ("uuid".equals(col)) hasUuid = true;

            colunas.add(col);
            placeholders.add("uuid".equals(col) ? "?::uuid" : "?");
            valores.add(convertValue(col, entry.getValue()));
        }

        // Garantir que uuid esta presente
        if (!hasUuid) {
            colunas.add("uuid");
            placeholders.add("?::uuid");
            valores.add(uuid);
        }

        // Garantir empresa_id (sempre do JWT, nunca do upload)
        int empIdx = colunas.indexOf("empresa_id");
        if (empIdx >= 0) {
            valores.set(empIdx, empresaId);
        } else {
            colunas.add("empresa_id");
            placeholders.add("?");
            valores.add(empresaId);
        }

        // Garantir sincronizado = true
        int syncIdx = colunas.indexOf("sincronizado");
        if (syncIdx >= 0) {
            valores.set(syncIdx, true);
        } else {
            colunas.add("sincronizado");
            placeholders.add("?");
            valores.add(true);
        }

        String sql = "INSERT INTO " + tabela
            + " (" + String.join(", ", colunas) + ")"
            + " VALUES (" + String.join(", ", placeholders) + ")"
            + " ON CONFLICT (uuid) DO NOTHING"; // evita duplicatas se uuid ja existe

        jdbc.update(sql, valores.toArray());
    }

    // ========================================================================
    // DOWNLOAD: buscar registros alterados no servidor
    // ========================================================================

    private List<SyncRegistroDownload> buscarParaDownload(
            String tabela, String ultimaSincronizacao, Integer empresaId, Set<String> uuidsExcluir) {

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tabela)
            .append(" WHERE empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);

        // Filtrar por timestamp: so registros alterados desde a ultima sync do cliente
        if (ultimaSincronizacao != null && !ultimaSincronizacao.isBlank()) {
            try {
                LocalDateTime ts = parseTimestamp(ultimaSincronizacao);
                sql.append(" AND ultima_atualizacao > ?::timestamp");
                params.add(Timestamp.valueOf(ts));
            } catch (DateTimeParseException e) {
                log.warn("ultimaSincronizacao invalida: {}", ultimaSincronizacao);
                // Sem filtro de data = retorna tudo (full sync)
            }
        }

        // Excluir UUIDs que acabaram de ser recebidos (evita eco)
        if (!uuidsExcluir.isEmpty()) {
            String ph = uuidsExcluir.stream().map(u -> "?::uuid").collect(Collectors.joining(","));
            sql.append(" AND uuid NOT IN (").append(ph).append(")");
            params.addAll(uuidsExcluir);
        }

        sql.append(" ORDER BY ultima_atualizacao ASC LIMIT 1000");

        String excluirCol = getExcluidoColumn(tabela);

        return jdbc.query(sql.toString(), (rs, i) -> {
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 1; c <= meta.getColumnCount(); c++) {
                String colName = meta.getColumnName(c);
                // Nao incluir colunas de controle interno
                if (COLUNAS_CONTROLE.contains(colName)) continue;
                row.put(colName, rs.getObject(c));
            }

            String rowUuid = row.get("uuid") != null ? row.get("uuid").toString() : null;

            // Determinar acao: DELETE se soft-deleted, UPDATE caso contrario
            boolean isExcluido = false;
            if (excluirCol != null) {
                Object exc = row.get(excluirCol);
                isExcluido = Boolean.TRUE.equals(exc);
            }
            String acao = isExcluido ? "DELETE" : "UPDATE";

            String ultimaAtt = row.get("ultima_atualizacao") != null
                ? row.get("ultima_atualizacao").toString()
                : null;

            String dadosJson;
            try {
                dadosJson = objectMapper.writeValueAsString(row);
            } catch (Exception e) {
                dadosJson = "{}";
            }

            return new SyncRegistroDownload(rowUuid, acao, ultimaAtt, dadosJson);
        }, params.toArray());
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private Map<String, Object> parseDadosJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Erro ao parsear dadosJson: {}", e.getMessage());
            return Map.of();
        }
    }

    private void validarTabela(String tabela) {
        if (tabela == null || !TABELAS_PERMITIDAS.contains(tabela)) {
            throw ApiException.badRequest("Tabela nao permitida para sync: " + tabela);
        }
    }

    private String getColunaId(String tabela) {
        return COLUNA_ID.getOrDefault(tabela, "id");
    }

    /**
     * Retorna o nome da coluna de soft-delete para a tabela, ou null se nao tem.
     */
    private String getExcluidoColumn(String tabela) {
        if (TABELAS_COM_EXCLUIDO.contains(tabela)) return "excluido";
        if (TABELAS_COM_IS_EXCLUIDO.contains(tabela)) return "is_excluido";
        return null;
    }

    /**
     * Sanitiza nome de coluna para evitar SQL injection.
     * Aceita apenas letras, numeros e underscore.
     */
    private String sanitizeColumnName(String col) {
        if (col == null || col.isBlank()) return null;
        if (!col.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            log.warn("Nome de coluna rejeitado: {}", col);
            return null;
        }
        return col.toLowerCase();
    }

    /**
     * Converte valores do JSON para tipos adequados ao JDBC.
     * O Jackson deserializa numeros como Integer/Long/Double; strings ficam String.
     */
    private Object convertValue(String col, Object value) {
        if (value == null) return null;

        // Booleanos
        if (value instanceof Boolean) return value;

        // Numeros: deixar como esta (JDBC faz cast automatico)
        if (value instanceof Number) return value;

        // Strings: verificar se e data/timestamp
        if (value instanceof String str) {
            if (str.isBlank()) return null;
            return str;
        }

        // Para mapas/listas (JSON aninhado), converter de volta para string
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
