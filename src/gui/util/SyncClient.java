package gui.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import gui.util.AppLogger;

/**
 * Cliente de sincronizacao bidirecional.
 * Autentica via JWT (POST /api/auth/operador/login), depois sincroniza
 * cada tabela via POST /api/sync (upload + download numa unica chamada).
 * Conflitos resolvidos por last-write-wins (ultima_atualizacao) no servidor.
 */
public class SyncClient {

    private static final String TAG = "SyncClient";
    private static final String CONFIG_FILE = "sync_config.properties";

    // Tabelas permitidas para sync — ORDEM IMPORTA: tabelas referenciadas primeiro
    // (embarcacoes/rotas antes de viagens, passageiros antes de passagens)
    private static final List<String> TABELAS_SYNC = List.of(
        "embarcacoes", "rotas", "tarifas", "conferentes", "caixas",
        "passageiros", "viagens", "passagens", "encomendas",
        "fretes", "financeiro_saidas"
    );

    // Mapa tabela -> coluna PK (alinhado com SyncService)
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

    // Tabelas com coluna 'excluido' para soft-delete
    private static final Set<String> TABELAS_COM_EXCLUIDO = Set.of(
        "viagens", "passagens", "encomendas", "fretes",
        "passageiros", "conferentes", "caixas", "rotas",
        "embarcacoes", "tarifas"
    );

    // Tabelas que usam 'is_excluido' em vez de 'excluido'
    private static final Set<String> TABELAS_COM_IS_EXCLUIDO = Set.of(
        "financeiro_saidas"
    );

    // Colunas de controle que nao devem ir no dadosJson de upload
    private static final Set<String> COLUNAS_SKIP_DADOS = Set.of(
        "sincronizado"
    );

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 60_000;

    // Configuracoes
    private String serverUrl = "http://localhost:8081";
    private String login = "";
    private String senha = "";
    private volatile String jwtToken = "";
    private volatile boolean autoSyncEnabled = false;
    private volatile int syncIntervalMinutes = 5;

    // DR108: volatile para visibilidade entre threads (scheduler, FX, CompletableFuture)
    private volatile LocalDateTime ultimaSincronizacao;
    // #039/#032: nao-final para permitir recriar apos shutdown via pararSyncAutomatica()
    private ScheduledExecutorService scheduler;
    // DR108: CopyOnWriteArrayList para acesso thread-safe
    private final List<SyncListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Singleton
    private static SyncClient instance;

    public static synchronized SyncClient getInstance() {
        if (instance == null) {
            instance = new SyncClient();
        }
        return instance;
    }

    private SyncClient() {
        this.scheduler = criarScheduler();
        carregarConfiguracoes();
    }

    private ScheduledExecutorService criarScheduler() {
        return Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SyncClient-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // ========================================================================
    // Configuracao
    // ========================================================================

    private void carregarConfiguracoes() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);

            this.serverUrl = props.getProperty("server.url", serverUrl);

            // Credenciais para login JWT
            this.login = props.getProperty("operador.login", "");
            this.senha = props.getProperty("operador.senha", "");

            // Token JWT pre-existente (caso ja esteja autenticado)
            String tokenRaw = props.getProperty("api.token", "");
            if ("true".equals(props.getProperty("api.token.encoded")) && !tokenRaw.isEmpty()) {
                try {
                    tokenRaw = new String(Base64.getDecoder().decode(tokenRaw), StandardCharsets.UTF_8);
                } catch (Exception ignored) { /* fallback: usa valor raw */ }
            }
            this.jwtToken = tokenRaw;

            this.autoSyncEnabled = Boolean.parseBoolean(props.getProperty("sync.auto", "false"));
            this.syncIntervalMinutes = Integer.parseInt(props.getProperty("sync.interval.minutos",
                props.getProperty("sync.interval", "5")));

            String ultimaSync = props.getProperty("sync.ultima", "");
            if (!ultimaSync.isEmpty()) {
                try {
                    this.ultimaSincronizacao = LocalDateTime.parse(ultimaSync);
                } catch (Exception e) {
                    AppLogger.warn(TAG, "Formato invalido em sync.ultima: " + ultimaSync);
                }
            }
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao carregar configuracoes de sync: " + e.getMessage());
        }
    }

    public void salvarConfiguracoes() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("server.url", serverUrl);
            props.setProperty("operador.login", login != null ? login : "");
            props.setProperty("operador.senha", senha != null ? senha : "");

            if (jwtToken != null && !jwtToken.isEmpty()) {
                props.setProperty("api.token", Base64.getEncoder().encodeToString(
                    jwtToken.getBytes(StandardCharsets.UTF_8)));
                props.setProperty("api.token.encoded", "true");
            } else {
                props.setProperty("api.token", "");
                props.setProperty("api.token.encoded", "false");
            }

            props.setProperty("sync.auto", String.valueOf(autoSyncEnabled));
            props.setProperty("sync.interval.minutos", String.valueOf(syncIntervalMinutes));

            if (ultimaSincronizacao != null) {
                props.setProperty("sync.ultima", ultimaSincronizacao.toString());
            }
            props.store(fos, "Configuracoes de Sincronizacao - Naviera");
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao salvar configuracoes de sync: " + e.getMessage());
        }
    }

    /**
     * Configura o servidor e credenciais de sincronizacao.
     */
    public void configurar(String serverUrl, String login, String senha) {
        this.serverUrl = serverUrl;
        this.login = login;
        this.senha = senha;
        this.jwtToken = ""; // Forcar re-autenticacao
        salvarConfiguracoes();
    }

    /**
     * Configura com token JWT direto (retrocompatibilidade).
     */
    public void configurar(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.jwtToken = token;
        salvarConfiguracoes();
    }

    // ========================================================================
    // Autenticacao JWT
    // ========================================================================

    /**
     * Autentica no servidor via POST /api/auth/operador/login.
     * Armazena o JWT retornado para chamadas subsequentes.
     * @return true se autenticou com sucesso
     */
    public boolean autenticar() {
        if (login == null || login.isEmpty() || senha == null || senha.isEmpty()) {
            // Se nao tem credenciais mas tem token, tenta usar o token existente
            if (jwtToken != null && !jwtToken.isEmpty()) {
                return true;
            }
            AppLogger.warn(TAG, "Credenciais de operador nao configuradas para autenticacao.");
            return false;
        }

        String jsonBody = "{\"login\":\"" + escapeJson(login)
            + "\",\"senha\":\"" + escapeJson(senha) + "\"}";

        HttpURLConnection conn = null;
        try {
            conn = abrirConexao("/api/auth/operador/login", "POST");
            enviarBody(conn, jsonBody);

            int code = conn.getResponseCode();
            String resposta = lerResposta(conn);

            if (code == 200) {
                // Extrair token da resposta: {"token":"xxx", "usuario":{...}}
                String token = extrairStringJson(resposta, "token");
                if (token != null && !token.isEmpty()) {
                    this.jwtToken = token;
                    salvarConfiguracoes();
                    AppLogger.info(TAG, "Autenticacao JWT realizada com sucesso.");
                    return true;
                }
            }
            AppLogger.warn(TAG, "Falha na autenticacao HTTP " + code + ": " + resposta);
            return false;
        } catch (Exception e) {
            AppLogger.error(TAG, "Erro na autenticacao: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Garante que ha um token JWT valido. Tenta autenticar se necessario.
     */
    private boolean garantirAutenticacao() {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            return true;
        }
        return autenticar();
    }

    // ========================================================================
    // Teste de conexao
    // ========================================================================

    public CompletableFuture<Boolean> testarConexao() {
        return CompletableFuture.supplyAsync(() -> {
            // Garantir autenticacao antes de testar (ping requer JWT)
            if (!garantirAutenticacao()) {
                notificarListeners(SyncEvent.ERRO, "Falha na autenticacao para teste de conexao.");
                return false;
            }
            HttpURLConnection conn = null;
            try {
                conn = abrirConexao("/api/sync/ping", "GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                return conn.getResponseCode() == 200;
            } catch (Exception e) {
                notificarListeners(SyncEvent.ERRO, "Falha na conexao: " + e.getMessage());
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ========================================================================
    // Sync automatica (scheduler)
    // ========================================================================

    /**
     * Inicia sincronizacao automatica.
     * #039/#032: recria o scheduler se ele foi encerrado por pararSyncAutomatica().
     */
    public void iniciarSyncAutomatica() {
        if (!autoSyncEnabled) return;

        if (scheduler.isShutdown()) {
            scheduler = criarScheduler();
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                sincronizarTudo();
            } catch (Exception e) {
                AppLogger.error(TAG, "Erro na sync automatica: " + e.getMessage(), e);
                notificarListeners(SyncEvent.ERRO, "Erro na sync automatica: " + e.getMessage());
            }
        }, 1, syncIntervalMinutes, TimeUnit.MINUTES);

        notificarListeners(SyncEvent.INFO, "Sincronizacao automatica iniciada (intervalo: "
            + syncIntervalMinutes + " min)");
    }

    /**
     * Para sincronizacao automatica.
     */
    public void pararSyncAutomatica() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        notificarListeners(SyncEvent.INFO, "Sincronizacao automatica parada");
    }

    // ========================================================================
    // Sincronizacao principal
    // ========================================================================

    /**
     * Sincroniza todas as tabelas.
     * Para cada tabela: coleta registros locais com sincronizado=false,
     * envia POST /api/sync, processa downloads, marca como sincronizado.
     */
    public CompletableFuture<SyncResult> sincronizarTudo() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult resultadoGeral = new SyncResult();

            // Garantir autenticacao antes de iniciar
            if (!garantirAutenticacao()) {
                resultadoGeral.sucesso = false;
                resultadoGeral.mensagem = "Falha na autenticacao. Verifique credenciais.";
                resultadoGeral.erros.add("Autenticacao falhou");
                notificarListeners(SyncEvent.ERRO, resultadoGeral.mensagem);
                return resultadoGeral;
            }

            for (String tabela : TABELAS_SYNC) {
                try {
                    notificarListeners(SyncEvent.PROGRESSO, "Sincronizando " + tabela + "...");
                    SyncResult resultado = sincronizarTabela(tabela).get(60, TimeUnit.SECONDS);
                    resultadoGeral.registrosEnviados += resultado.registrosEnviados;
                    resultadoGeral.registrosRecebidos += resultado.registrosRecebidos;
                    if (!resultado.erros.isEmpty()) {
                        resultadoGeral.erros.addAll(resultado.erros);
                    }
                } catch (Exception e) {
                    AppLogger.error(TAG, "Erro em " + tabela + ": " + e.getMessage(), e);
                    resultadoGeral.erros.add(tabela + ": " + e.getMessage());
                }
            }

            ultimaSincronizacao = LocalDateTime.now();
            salvarConfiguracoes();

            resultadoGeral.sucesso = resultadoGeral.erros.isEmpty();
            resultadoGeral.mensagem = resultadoGeral.sucesso
                ? "Sincronizacao concluida com sucesso!"
                : "Sincronizacao concluida com " + resultadoGeral.erros.size() + " erro(s)";

            notificarListeners(
                resultadoGeral.sucesso ? SyncEvent.SUCESSO : SyncEvent.ERRO,
                resultadoGeral.mensagem
                    + " (enviados=" + resultadoGeral.registrosEnviados
                    + ", recebidos=" + resultadoGeral.registrosRecebidos + ")"
            );

            return resultadoGeral;
        });
    }

    /**
     * Sincroniza uma tabela especifica.
     * Fluxo: upload pendentes -> POST /api/sync -> download resposta -> mark synced.
     */
    public CompletableFuture<SyncResult> sincronizarTabela(String tabela) {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult resultado = new SyncResult();

            try {
                // 1. Buscar registros pendentes (sincronizado = false)
                List<RegistroPendente> pendentes = buscarRegistrosPendentes(tabela);

                // 2. Montar JSON de request conforme SyncRequest do servidor
                String jsonRequest = montarSyncRequest(tabela, pendentes);

                // 3. Enviar POST /api/sync com retry
                String response = enviarComRetry("/api/sync", jsonRequest);

                // 4. Parsear resposta (SyncResponse)
                Map<String, Object> syncResponse = parseJsonResponse(response);

                Object sucessoObj = syncResponse.getOrDefault("sucesso", false);
                resultado.sucesso = sucessoObj instanceof Boolean
                    ? (Boolean) sucessoObj
                    : Boolean.parseBoolean(String.valueOf(sucessoObj));
                resultado.mensagem = (String) syncResponse.getOrDefault("mensagem", "");
                resultado.registrosEnviados = pendentes.size();

                // 5. Processar registros de download (INSERT ON CONFLICT DO UPDATE)
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> registrosDownload =
                    (List<Map<String, Object>>) syncResponse.get("registrosParaDownload");

                if (registrosDownload != null) {
                    resultado.registrosRecebidos = registrosDownload.size();
                    for (Map<String, Object> registro : registrosDownload) {
                        aplicarRegistroRecebido(tabela, registro);
                    }
                }

                // 6. Marcar registros locais como sincronizados
                if (resultado.sucesso) {
                    marcarComoSincronizados(tabela, pendentes);
                }

            } catch (Exception e) {
                AppLogger.error(TAG, "Sync " + tabela + ": " + e.getMessage(), e);
                resultado.sucesso = false;
                resultado.mensagem = "Erro: " + e.getMessage();
                resultado.erros.add(tabela + ": " + e.getMessage());
            }
            return resultado;
        });
    }

    // ========================================================================
    // Upload: coletar registros pendentes
    // ========================================================================

    /**
     * Busca registros com sincronizado=false no banco local para upload.
     */
    private List<RegistroPendente> buscarRegistrosPendentes(String tabela) {
        List<RegistroPendente> pendentes = new ArrayList<>();
        String colunaId = getColunaId(tabela);

        // Construir WHERE: sincronizado = FALSE AND empresa_id = ?
        // Tabelas com 'excluido' podem ter registros excluidos para enviar como DELETE
        String sql = "SELECT * FROM " + tabela + " WHERE sincronizado = FALSE AND empresa_id = ?";

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, dao.DAOUtils.empresaId());
            java.sql.ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> dados = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = meta.getColumnName(i);
                    if (COLUNAS_SKIP_DADOS.contains(colName)) continue;
                    dados.put(colName, rs.getObject(i));
                }

                String uuid = dados.get("uuid") != null ? dados.get("uuid").toString() : null;
                if (uuid == null || uuid.isEmpty()) continue;

                Object idLocal = dados.get(colunaId);

                // Determinar acao: DELETE se excluido, senao INSERT/UPDATE
                String acao = determinarAcao(tabela, dados);

                String ultimaAtt = dados.get("ultima_atualizacao") != null
                    ? dados.get("ultima_atualizacao").toString()
                    : null;

                pendentes.add(new RegistroPendente(
                    uuid, acao, ultimaAtt, criarJsonSimples(dados), idLocal, colunaId
                ));
            }
        } catch (Exception e) {
            AppLogger.error(TAG, "Erro ao buscar pendentes de " + tabela + ": " + e.getMessage(), e);
        }

        return pendentes;
    }

    /**
     * Determina a acao para um registro baseado no seu estado.
     */
    private String determinarAcao(String tabela, Map<String, Object> dados) {
        // Verificar soft-delete
        if (TABELAS_COM_EXCLUIDO.contains(tabela)) {
            Object excluido = dados.get("excluido");
            if (Boolean.TRUE.equals(excluido)) return "DELETE";
        }
        if (TABELAS_COM_IS_EXCLUIDO.contains(tabela)) {
            Object excluido = dados.get("is_excluido");
            if (Boolean.TRUE.equals(excluido)) return "DELETE";
        }
        return "UPDATE"; // Servidor trata INSERT/UPDATE automaticamente (upsert)
    }

    // ========================================================================
    // Upload: montar JSON de request
    // ========================================================================

    /**
     * Monta o JSON no formato SyncRequest:
     * {"tabela":"...", "ultimaSincronizacao":"...", "registros":[{uuid, acao, ultimaAtualizacao, dadosJson}]}
     */
    private String montarSyncRequest(String tabela, List<RegistroPendente> pendentes) {
        StringBuilder json = new StringBuilder(4096);
        json.append("{\"tabela\":\"").append(escapeJson(tabela)).append("\"");

        if (ultimaSincronizacao != null) {
            json.append(",\"ultimaSincronizacao\":\"")
                .append(ultimaSincronizacao.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("\"");
        }

        json.append(",\"registros\":[");
        for (int i = 0; i < pendentes.size(); i++) {
            if (i > 0) json.append(",");
            RegistroPendente p = pendentes.get(i);
            json.append("{\"uuid\":\"").append(escapeJson(p.uuid)).append("\"");
            json.append(",\"acao\":\"").append(escapeJson(p.acao)).append("\"");
            if (p.ultimaAtualizacao != null) {
                json.append(",\"ultimaAtualizacao\":\"").append(escapeJson(p.ultimaAtualizacao)).append("\"");
            }
            json.append(",\"dadosJson\":\"").append(escapeJson(p.dadosJson)).append("\"");
            json.append("}");
        }
        json.append("]}");

        return json.toString();
    }

    // ========================================================================
    // Download: aplicar registros recebidos do servidor
    // ========================================================================

    /**
     * Aplica um registro recebido do servidor no banco local.
     * Usa INSERT ON CONFLICT (uuid) DO UPDATE para upsert.
     */
    private void aplicarRegistroRecebido(String tabela, Map<String, Object> registro) {
        String acao = (String) registro.get("acao");
        String uuid = (String) registro.get("uuid");
        String dadosJson = (String) registro.get("dadosJson");

        if (uuid == null || uuid.isEmpty()) return;

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection()) {
            String colunaId = getColunaId(tabela);
            int empresaId = dao.DAOUtils.empresaId();

            if ("DELETE".equals(acao)) {
                aplicarDelete(conn, tabela, uuid, empresaId);
            } else {
                aplicarUpsert(conn, tabela, uuid, empresaId, colunaId, dadosJson);
            }
        } catch (Exception e) {
            AppLogger.error(TAG, "Erro ao aplicar registro de " + tabela
                + " uuid=" + uuid + ": " + e.getMessage(), e);
        }
    }

    /**
     * Aplica soft-delete para registro recebido do servidor.
     */
    private void aplicarDelete(java.sql.Connection conn, String tabela,
                               String uuid, int empresaId) throws Exception {
        String excluirCol = null;
        if (TABELAS_COM_EXCLUIDO.contains(tabela)) excluirCol = "excluido";
        else if (TABELAS_COM_IS_EXCLUIDO.contains(tabela)) excluirCol = "is_excluido";

        if (excluirCol != null) {
            String sql = "UPDATE " + tabela + " SET " + excluirCol
                + " = TRUE, sincronizado = TRUE WHERE uuid = ?::uuid AND empresa_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                stmt.setInt(2, empresaId);
                stmt.executeUpdate();
            }
        } else {
            String sql = "DELETE FROM " + tabela + " WHERE uuid = ?::uuid AND empresa_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                stmt.setInt(2, empresaId);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Aplica INSERT ON CONFLICT (uuid) DO UPDATE para um registro do servidor.
     */
    private void aplicarUpsert(java.sql.Connection conn, String tabela, String uuid,
                               int empresaId, String colunaId, String dadosJson) throws Exception {
        Map<String, Object> dados = parseFullJson(dadosJson);
        if (dados.isEmpty()) return;

        // Colunas que nao devem ser atualizadas via download
        Set<String> skipCols = new HashSet<>(Arrays.asList(colunaId));

        List<String> colunas = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        List<String> updateClauses = new ArrayList<>();

        boolean hasUuid = false;
        boolean hasEmpresaId = false;

        for (Map.Entry<String, Object> entry : dados.entrySet()) {
            String col = entry.getKey();
            if (!isValidColumnName(col)) continue;
            if (skipCols.contains(col)) continue;

            if ("uuid".equals(col)) hasUuid = true;
            if ("empresa_id".equals(col)) hasEmpresaId = true;

            colunas.add(col);
            placeholders.add("uuid".equals(col) ? "?::uuid" : "?");
            valores.add(entry.getValue());

            // Para ON CONFLICT UPDATE, nao atualizar uuid nem empresa_id
            if (!"uuid".equals(col) && !"empresa_id".equals(col) && !"sincronizado".equals(col)) {
                updateClauses.add(col + " = EXCLUDED." + col);
            }
        }

        // Garantir uuid
        if (!hasUuid) {
            colunas.add("uuid");
            placeholders.add("?::uuid");
            valores.add(uuid);
        }

        // Garantir empresa_id
        if (!hasEmpresaId) {
            colunas.add("empresa_id");
            placeholders.add("?");
            valores.add(empresaId);
        }

        // Garantir sincronizado = TRUE (veio do servidor, ja esta sincronizado)
        int syncIdx = colunas.indexOf("sincronizado");
        if (syncIdx >= 0) {
            valores.set(syncIdx, true);
        } else {
            colunas.add("sincronizado");
            placeholders.add("?");
            valores.add(true);
        }
        updateClauses.add("sincronizado = TRUE");

        if (updateClauses.isEmpty()) return;

        String sql = "INSERT INTO " + tabela
            + " (" + String.join(", ", colunas) + ")"
            + " VALUES (" + String.join(", ", placeholders) + ")"
            + " ON CONFLICT (uuid) DO UPDATE SET " + String.join(", ", updateClauses);

        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < valores.size(); i++) {
                stmt.setObject(i + 1, valores.get(i));
            }
            stmt.executeUpdate();
        }
    }

    // ========================================================================
    // Marcar como sincronizado
    // ========================================================================

    /**
     * Marca registros locais como sincronizados apos upload bem-sucedido.
     */
    private void marcarComoSincronizados(String tabela, List<RegistroPendente> registros) {
        if (registros.isEmpty()) return;

        String sql = "UPDATE " + tabela + " SET sincronizado = TRUE WHERE "
            + registros.get(0).colunaId + " = ? AND empresa_id = ?";

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            int empresaId = dao.DAOUtils.empresaId();
            int count = 0;
            for (RegistroPendente reg : registros) {
                if (reg.idLocal != null) {
                    stmt.setObject(1, reg.idLocal);
                    stmt.setInt(2, empresaId);
                    stmt.addBatch();
                    count++;
                }
            }

            if (count > 0) {
                int[] results = stmt.executeBatch();
                AppLogger.info(TAG, "Marcados como sincronizados em " + tabela + ": " + results.length);
            }
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao marcar como sincronizados em " + tabela + ": " + e.getMessage());
        }
    }

    // ========================================================================
    // HTTP: envio com retry e autenticacao
    // ========================================================================

    /**
     * Envia POST com retry para falhas transientes (timeout, 5xx, 401 com re-auth).
     */
    private String enviarComRetry(String endpoint, String jsonBody) throws Exception {
        Exception ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= MAX_RETRIES; tentativa++) {
            HttpURLConnection conn = null;
            try {
                conn = abrirConexao(endpoint, "POST");

                // Adicionar token JWT
                if (jwtToken != null && !jwtToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                }

                enviarBody(conn, jsonBody);
                int code = conn.getResponseCode();

                // 401 = token expirado, re-autenticar e retry
                if (code == 401 && tentativa < MAX_RETRIES) {
                    AppLogger.warn(TAG, "Token expirado (HTTP 401), re-autenticando...");
                    jwtToken = "";
                    if (autenticar()) {
                        continue; // retry com novo token
                    }
                    throw new Exception("Re-autenticacao falhou apos HTTP 401");
                }

                String resposta = lerResposta(conn);

                if (code >= 200 && code < 300) {
                    return resposta;
                }

                // 5xx = erro do servidor, pode ser transiente
                if (code >= 500 && tentativa < MAX_RETRIES) {
                    AppLogger.warn(TAG, "Erro HTTP " + code + " (tentativa " + tentativa
                        + "/" + MAX_RETRIES + "), retentando...");
                    Thread.sleep(RETRY_DELAY_MS * tentativa);
                    continue;
                }

                throw new Exception("Erro HTTP " + code + ": " + resposta);

            } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
                ultimaExcecao = e;
                if (tentativa < MAX_RETRIES) {
                    AppLogger.warn(TAG, "Timeout/conexao (tentativa " + tentativa
                        + "/" + MAX_RETRIES + "): " + e.getMessage());
                    Thread.sleep(RETRY_DELAY_MS * tentativa);
                    continue;
                }
            } catch (Exception e) {
                ultimaExcecao = e;
                if (tentativa < MAX_RETRIES && isTransientError(e)) {
                    AppLogger.warn(TAG, "Erro transiente (tentativa " + tentativa
                        + "/" + MAX_RETRIES + "): " + e.getMessage());
                    Thread.sleep(RETRY_DELAY_MS * tentativa);
                    continue;
                }
                throw e;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        throw ultimaExcecao != null
            ? ultimaExcecao
            : new Exception("Falha apos " + MAX_RETRIES + " tentativas");
    }

    private boolean isTransientError(Exception e) {
        return e instanceof java.net.SocketTimeoutException
            || e instanceof java.net.ConnectException
            || (e.getMessage() != null && e.getMessage().contains("HTTP 5"));
    }

    /**
     * Abre uma HttpURLConnection configurada.
     */
    private HttpURLConnection abrirConexao(String endpoint, String method) throws Exception {
        // Aviso se usando HTTP em servidor remoto
        if (serverUrl != null && serverUrl.startsWith("http://")
            && !serverUrl.contains("localhost") && !serverUrl.contains("127.0.0.1")) {
            AppLogger.warn(TAG, "AVISO SEGURANCA: Sync usando HTTP sem TLS para servidor remoto: " + serverUrl);
        }

        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        if ("POST".equals(method) || "PUT".equals(method)) {
            conn.setDoOutput(true);
        }

        // Incluir JWT se disponivel
        if (jwtToken != null && !jwtToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        }

        return conn;
    }

    /**
     * Envia o body JSON numa conexao ja aberta.
     */
    private void enviarBody(HttpURLConnection conn, String jsonBody) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    /**
     * Le a resposta (sucesso ou erro) de uma conexao.
     */
    private String lerResposta(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    // ========================================================================
    // JSON helpers (sem dependencia externa)
    // ========================================================================

    /**
     * Cria JSON simples a partir de um Map (flat, sem objetos aninhados).
     */
    private String criarJsonSimples(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean primeiro = true;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!primeiro) json.append(",");
            primeiro = false;

            json.append("\"").append(escapeJson(entry.getKey())).append("\":");

            Object valor = entry.getValue();
            if (valor == null) {
                json.append("null");
            } else if (valor instanceof String) {
                json.append("\"").append(escapeJson((String) valor)).append("\"");
            } else if (valor instanceof Number || valor instanceof Boolean) {
                json.append(valor);
            } else {
                json.append("\"").append(escapeJson(valor.toString())).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    /**
     * Parse completo de um JSON flat — retorna TODOS os key-value pairs.
     */
    private Map<String, Object> parseFullJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return result;

        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            int keyStart = json.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = json.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart + 1, keyEnd);

            int colon = json.indexOf(':', keyEnd + 1);
            if (colon < 0) break;

            int valueStart = colon + 1;
            while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
            if (valueStart >= json.length()) break;

            char c = json.charAt(valueStart);
            Object value;

            if (c == '"') {
                int vEnd = valueStart + 1;
                boolean escape = false;
                StringBuilder sb = new StringBuilder();
                while (vEnd < json.length()) {
                    char ch = json.charAt(vEnd);
                    if (escape) {
                        if (ch == 'n') sb.append('\n');
                        else if (ch == 'r') sb.append('\r');
                        else if (ch == 't') sb.append('\t');
                        else sb.append(ch);
                        escape = false;
                    } else if (ch == '\\') {
                        escape = true;
                    } else if (ch == '"') {
                        break;
                    } else {
                        sb.append(ch);
                    }
                    vEnd++;
                }
                value = sb.toString();
                i = vEnd + 1;
            } else if (c == 'n' && json.startsWith("null", valueStart)) {
                value = null;
                i = valueStart + 4;
            } else if (c == 't' && json.startsWith("true", valueStart)) {
                value = true;
                i = valueStart + 4;
            } else if (c == 'f' && json.startsWith("false", valueStart)) {
                value = false;
                i = valueStart + 5;
            } else if (c == '{' || c == '[') {
                int nivel = 0;
                int vEnd = valueStart;
                char open = c, close = (c == '{') ? '}' : ']';
                while (vEnd < json.length()) {
                    if (json.charAt(vEnd) == open) nivel++;
                    else if (json.charAt(vEnd) == close) {
                        nivel--;
                        if (nivel == 0) { vEnd++; break; }
                    }
                    vEnd++;
                }
                value = json.substring(valueStart, vEnd);
                i = vEnd;
            } else {
                int vEnd = valueStart;
                while (vEnd < json.length() && json.charAt(vEnd) != ',' && json.charAt(vEnd) != '}') vEnd++;
                String numStr = json.substring(valueStart, vEnd).trim();
                try {
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        long l = Long.parseLong(numStr);
                        value = (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
                    }
                } catch (NumberFormatException e) {
                    value = numStr;
                }
                i = vEnd;
            }

            result.put(key, value);
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) == ' ')) i++;
        }

        return result;
    }

    /**
     * Parse simples de resposta JSON do servidor (SyncResponse).
     */
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();

        if (json == null || json.trim().isEmpty()) {
            result.put("sucesso", false);
            result.put("mensagem", "Resposta vazia do servidor");
            return result;
        }

        try {
            String j = json.trim();

            // Extrair campos primitivos
            result.put("sucesso", j.contains("\"sucesso\":true") || j.contains("\"sucesso\": true"));
            result.put("mensagem", extrairStringJson(j, "mensagem"));
            result.put("registrosRecebidos", extrairNumeroJson(j, "registrosRecebidos"));
            result.put("registrosEnviados", extrairNumeroJson(j, "registrosEnviados"));

            // Extrair registrosParaDownload como lista
            int downloadStart = j.indexOf("\"registrosParaDownload\":");
            if (downloadStart >= 0) {
                int arrayStart = j.indexOf("[", downloadStart);
                if (arrayStart >= 0) {
                    int arrayEnd = encontrarFechamento(j, arrayStart, '[', ']');
                    if (arrayEnd > arrayStart) {
                        String arrayJson = j.substring(arrayStart, arrayEnd);
                        result.put("registrosParaDownload", parseJsonArray(arrayJson));
                    }
                }
            }
        } catch (Exception e) {
            result.put("sucesso", false);
            result.put("mensagem", "Erro ao processar resposta: " + e.getMessage());
            AppLogger.error(TAG, "Erro ao parsear resposta JSON: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Extrai um valor string de um JSON pela chave.
     */
    private String extrairStringJson(String json, String chave) {
        String busca = "\"" + chave + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0) {
            busca = "\"" + chave + "\": ";
            idx = json.indexOf(busca);
        }
        if (idx < 0) return null;

        int valueStart = json.indexOf("\"", idx + busca.length());
        if (valueStart < 0) return null;
        valueStart++;

        int valueEnd = valueStart;
        boolean escape = false;
        while (valueEnd < json.length()) {
            char c = json.charAt(valueEnd);
            if (escape) { escape = false; }
            else if (c == '\\') { escape = true; }
            else if (c == '"') break;
            valueEnd++;
        }
        return valueEnd > valueStart ? json.substring(valueStart, valueEnd) : null;
    }

    /**
     * Extrai um valor numerico de um JSON pela chave.
     */
    private int extrairNumeroJson(String json, String chave) {
        String busca = "\"" + chave + "\":";
        int idx = json.indexOf(busca);
        if (idx < 0) return 0;

        int numStart = idx + busca.length();
        while (numStart < json.length() && json.charAt(numStart) == ' ') numStart++;

        StringBuilder numStr = new StringBuilder();
        for (int i = numStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '-') {
                numStr.append(c);
            } else if (numStr.length() > 0) {
                break;
            }
        }
        if (numStr.length() > 0) {
            try { return Integer.parseInt(numStr.toString()); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Encontra a posicao de fechamento de um delimitador (colchete ou chave).
     */
    private int encontrarFechamento(String json, int start, char open, char close) {
        int nivel = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == open) nivel++;
            else if (c == close) {
                nivel--;
                if (nivel == 0) return i + 1;
            }
        }
        return -1;
    }

    /**
     * Parse de array JSON com objetos.
     */
    private List<Map<String, Object>> parseJsonArray(String arrayJson) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (arrayJson == null || arrayJson.trim().isEmpty() || "[]".equals(arrayJson.trim())) {
            return lista;
        }

        try {
            arrayJson = arrayJson.trim();
            if (arrayJson.startsWith("[")) arrayJson = arrayJson.substring(1);
            if (arrayJson.endsWith("]")) arrayJson = arrayJson.substring(0, arrayJson.length() - 1);

            int nivel = 0;
            int objStart = -1;

            for (int i = 0; i < arrayJson.length(); i++) {
                char c = arrayJson.charAt(i);
                if (c == '{') {
                    if (nivel == 0) objStart = i;
                    nivel++;
                } else if (c == '}') {
                    nivel--;
                    if (nivel == 0 && objStart >= 0) {
                        String objJson = arrayJson.substring(objStart, i + 1);
                        lista.add(parseJsonObject(objJson));
                        objStart = -1;
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao parsear array JSON: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Parse de objeto JSON (SyncRegistroDownload).
     */
    private Map<String, Object> parseJsonObject(String objJson) {
        Map<String, Object> obj = new HashMap<>();
        try {
            obj.put("uuid", extrairStringJson(objJson, "uuid"));
            obj.put("acao", extrairStringJson(objJson, "acao"));
            obj.put("ultimaAtualizacao", extrairStringJson(objJson, "ultimaAtualizacao"));

            // dadosJson pode conter JSON aninhado — extrair com cuidado
            String busca = "\"dadosJson\":";
            int idx = objJson.indexOf(busca);
            if (idx < 0) {
                busca = "\"dadosJson\": ";
                idx = objJson.indexOf(busca);
            }
            if (idx >= 0) {
                int valueStart = idx + busca.length();
                while (valueStart < objJson.length() && objJson.charAt(valueStart) == ' ') valueStart++;

                if (valueStart < objJson.length()) {
                    char c = objJson.charAt(valueStart);
                    if (c == '"') {
                        // String value (JSON escapado dentro de string)
                        obj.put("dadosJson", extrairStringJson(objJson, "dadosJson"));
                    } else if (c == '{') {
                        // JSON object inline
                        int end = encontrarFechamento(objJson, valueStart, '{', '}');
                        if (end > valueStart) {
                            obj.put("dadosJson", objJson.substring(valueStart, end));
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao parsear objeto JSON: " + e.getMessage());
        }
        return obj;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private String getColunaId(String tabela) {
        return COLUNA_ID.getOrDefault(tabela, "id");
    }

    private boolean isValidColumnName(String col) {
        if (col == null || col.isEmpty()) return false;
        return col.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    // ========================================================================
    // Status e listeners
    // ========================================================================

    public String obterStatusSincronizacao() {
        StringBuilder status = new StringBuilder();
        status.append("URL do Servidor: ").append(serverUrl).append("\n");
        status.append("Sincronizacao Automatica: ").append(autoSyncEnabled ? "Ativada" : "Desativada").append("\n");

        if (autoSyncEnabled) {
            status.append("Intervalo: ").append(syncIntervalMinutes).append(" minutos\n");
        }

        status.append("Autenticacao: ").append(
            (jwtToken != null && !jwtToken.isEmpty()) ? "Token JWT presente" : "Nao autenticado"
        ).append("\n");

        if (ultimaSincronizacao != null) {
            status.append("Ultima Sincronizacao: ")
                  .append(ultimaSincronizacao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                  .append("\n");
        } else {
            status.append("Ultima Sincronizacao: Nunca sincronizado\n");
        }

        return status.toString();
    }

    /**
     * Recebe dados do servidor (retrocompatibilidade — agora integrado no sincronizarTudo).
     */
    public CompletableFuture<SyncResult> receberDadosDoServidor() {
        return sincronizarTudo();
    }

    public void addListener(SyncListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SyncListener listener) {
        listeners.remove(listener);
    }

    private void notificarListeners(SyncEvent evento, String mensagem) {
        for (SyncListener listener : listeners) {
            try {
                listener.onSyncEvent(evento, mensagem);
            } catch (Exception e) {
                AppLogger.error(TAG, "Erro em listener: " + e.getMessage(), e);
            }
        }
    }

    // ========================================================================
    // Getters / Setters
    // ========================================================================

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public boolean isAutoSyncEnabled() { return autoSyncEnabled; }
    public void setAutoSyncEnabled(boolean enabled) { this.autoSyncEnabled = enabled; }

    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public void setSyncIntervalMinutes(int minutes) { this.syncIntervalMinutes = minutes; }

    public LocalDateTime getUltimaSincronizacao() { return ultimaSincronizacao; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    // ========================================================================
    // Classes internas
    // ========================================================================

    public enum SyncEvent {
        INFO, PROGRESSO, SUCESSO, ERRO
    }

    public interface SyncListener {
        void onSyncEvent(SyncEvent evento, String mensagem);
    }

    /**
     * Registro pendente de upload com metadados para marcar como sincronizado depois.
     */
    private static class RegistroPendente {
        final String uuid;
        final String acao;
        final String ultimaAtualizacao;
        final String dadosJson;
        final Object idLocal;
        final String colunaId;

        RegistroPendente(String uuid, String acao, String ultimaAtualizacao,
                         String dadosJson, Object idLocal, String colunaId) {
            this.uuid = uuid;
            this.acao = acao;
            this.ultimaAtualizacao = ultimaAtualizacao;
            this.dadosJson = dadosJson;
            this.idLocal = idLocal;
            this.colunaId = colunaId;
        }
    }

    public static class SyncResult {
        public boolean sucesso = false;
        public String mensagem = "";
        public int registrosEnviados = 0;
        public int registrosRecebidos = 0;
        public List<String> erros = new ArrayList<>();

        public SyncResult() {}

        public SyncResult(boolean sucesso, String mensagem) {
            this.sucesso = sucesso;
            this.mensagem = mensagem;
        }

        @Override
        public String toString() {
            return String.format("SyncResult[sucesso=%s, enviados=%d, recebidos=%d, erros=%d]",
                sucesso, registrosEnviados, registrosRecebidos, erros.size());
        }
    }
}
