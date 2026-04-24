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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import util.AppLogger;

/**
 * Cliente de sincronizacao bidirecional.
 * Autentica via JWT (POST /api/auth/operador/login), depois sincroniza
 * cada tabela via POST /api/sync (upload + download numa unica chamada).
 * Conflitos resolvidos por last-write-wins (ultima_atualizacao) no servidor.
 */
public class SyncClient {

    private static final String TAG = "SyncClient";
    private static final String CONFIG_FILE = "sync_config.properties";
    // #223: Patterns precompilados — chamados por coluna × registro × tabela em loops de sync.
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}.*");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    // DR209: volatile para visibilidade entre threads (FX thread escreve, scheduler lê)
    // DS4-012 fix: default HTTPS (antes HTTP)
    private volatile String serverUrl = "https://localhost:8081";
    private volatile String login = "";
    private volatile String senha = "";
    private volatile String jwtToken = "";
    private volatile boolean autoSyncEnabled = false;
    private volatile int syncIntervalMinutes = 5;

    // DR108: volatile para visibilidade entre threads (scheduler, FX, CompletableFuture)
    private volatile LocalDateTime ultimaSincronizacao;
    // DR210: volatile + nao-final para permitir recriar apos shutdown via pararSyncAutomatica()
    private volatile ScheduledExecutorService scheduler;
    // DB112: executor dedicado para operacoes de sync que fazem I/O bloqueante.
    // Evita bloquear o ForkJoinPool common (usado por CompletableFuture sem executor).
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "SyncClient-Worker");
        t.setDaemon(true);
        return t;
    });
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

        // #DS5-204: se o arquivo antigo contem senha/token em Base64, migra (deleta dos campos)
        // sem reutilizar valores — exige re-login imediato. Base64 nao e criptografia.
        boolean precisaMigrar = false;

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);

            this.serverUrl = props.getProperty("server.url", serverUrl);

            // Mantem apenas o username para pre-preencher a tela de login (nao e credencial).
            this.login = props.getProperty("operador.login", "");

            // #DS5-204: senha nunca mais carregada do disco — forca re-autenticacao.
            this.senha = "";
            if (props.containsKey("operador.senha") || props.containsKey("operador.senha.encoded")) {
                precisaMigrar = true;
            }

            // #DS5-204: JWT nunca mais carregado do disco — re-autentica a cada execucao.
            this.jwtToken = "";
            if (props.containsKey("api.token") || props.containsKey("api.token.encoded")) {
                precisaMigrar = true;
            }

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

        // #DS5-204: se detectou campos sensiveis no arquivo antigo, reescreve sem eles.
        if (precisaMigrar) {
            AppLogger.warn(TAG, "sync_config.properties tinha credenciais persistidas — removidas (DS5-204). Re-autentique.");
            salvarConfiguracoes();
        }
    }

    public void salvarConfiguracoes() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("server.url", serverUrl);
            // #DS5-204: persiste apenas o username (identificador, nao credencial).
            // Senha e JWT NUNCA sao gravados — sao mantidos so em memoria.
            props.setProperty("operador.login", login != null ? login : "");

            props.setProperty("sync.auto", String.valueOf(autoSyncEnabled));
            props.setProperty("sync.interval.minutos", String.valueOf(syncIntervalMinutes));

            if (ultimaSincronizacao != null) {
                props.setProperty("sync.ultima", ultimaSincronizacao.toString());
            }
            props.store(fos, "Configuracoes de Sincronizacao - Naviera (sem credenciais — DS5-204)");
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

        // API exige empresa_id: lido do db.properties via TenantContext/DAOUtils
        int empresaIdAuth;
        try {
            empresaIdAuth = dao.DAOUtils.empresaId();
        } catch (Exception e) {
            AppLogger.warn(TAG, "empresa_id indisponivel no contexto — login sem empresa_id pode falhar.");
            empresaIdAuth = 0;
        }
        String jsonBody = "{\"login\":\"" + escapeJson(login)
            + "\",\"senha\":\"" + escapeJson(senha) + "\""
            + (empresaIdAuth > 0 ? ",\"empresa_id\":" + empresaIdAuth : "")
            + "}";

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
            // Decode JWT payload and check exp
            try {
                String[] parts = jwtToken.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                    // Simple JSON parse for "exp" field
                    int expIdx = payload.indexOf("\"exp\"");
                    if (expIdx >= 0) {
                        String after = payload.substring(expIdx + 5).replaceFirst("[^0-9]+", "");
                        long exp = Long.parseLong(after.replaceFirst("[^0-9].*", ""));
                        if (System.currentTimeMillis() / 1000 > exp - 60) { // 60s buffer
                            jwtToken = null; // Force re-auth
                        }
                    }
                }
            } catch (Exception ignored) { /* If decode fails, let server reject */ }
        }
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
        // DB112: passa syncExecutor (threads dedicadas a I/O) para nao bloquear ForkJoinPool common.
        // O loop interno chama sincronizarTabelaSync() diretamente (metodo sincrono) em vez de
        // .get() sobre outro CompletableFuture, eliminando o aninhamento bloqueante.
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
                    // DB112: chama versao sincrona — ja estamos num thread do syncExecutor
                    SyncResult resultado = sincronizarTabelaSync(tabela);
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
        }, syncExecutor);
    }

    /**
     * Sincroniza uma tabela especifica (async).
     * DB112: usa syncExecutor para nao bloquear o ForkJoinPool common.
     */
    public CompletableFuture<SyncResult> sincronizarTabela(String tabela) {
        return CompletableFuture.supplyAsync(() -> sincronizarTabelaSync(tabela), syncExecutor);
    }

    /**
     * Implementacao sincrona de sincronizarTabela.
     * Chamada diretamente por sincronizarTudo (que ja roda no syncExecutor)
     * para evitar bloqueio aninhado de CompletableFuture.
     * Fluxo: upload pendentes -> POST /api/sync -> download resposta -> mark synced.
     */
    private SyncResult sincronizarTabelaSync(String tabela) {
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
                try (java.sql.Connection connDownload = dao.ConexaoBD.getConnection()) {
                    for (Map<String, Object> registro : registrosDownload) {
                        aplicarRegistroRecebido(connDownload, tabela, registro);
                    }
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
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
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
     * Recebe a conexao compartilhada para evitar N+1 conexoes por registro.
     */
    private void aplicarRegistroRecebido(java.sql.Connection conn, String tabela, Map<String, Object> registro) {
        String acao = (String) registro.get("acao");
        String uuid = (String) registro.get("uuid");
        String dadosJson = (String) registro.get("dadosJson");

        if (uuid == null || uuid.isEmpty()) return;

        try {
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
                // #223: normalizar timestamps (String ISO -> Timestamp) para evitar mismatch
                //   entre postgres (sem TZ) e java.util.Date quando vem do JSON.
                Object val = valores.get(i);
                if (val instanceof String s && s.length() >= 10 && s.length() <= 30) {
                    boolean ts = TIMESTAMP_PATTERN.matcher(s).matches();
                    boolean dt = !ts && DATE_PATTERN.matcher(s).matches();
                    if (ts || dt) {
                        try {
                            if (ts) {
                                String norm = s.replace('T', ' ');
                                int dot = norm.indexOf('.');
                                if (dot > 0) norm = norm.substring(0, dot);
                                stmt.setTimestamp(i + 1, java.sql.Timestamp.valueOf(norm));
                            } else {
                                stmt.setDate(i + 1, java.sql.Date.valueOf(s));
                            }
                            continue;
                        } catch (Exception ignored) { /* fallback setObject */ }
                    }
                }
                stmt.setObject(i + 1, val);
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
        // DS4-012 fix: bloquear HTTP para servidores remotos (antes: so warning)
        if (serverUrl != null && serverUrl.startsWith("http://")
            && !serverUrl.contains("localhost") && !serverUrl.contains("127.0.0.1")) {
            throw new SecurityException("Sync bloqueado: HTTP sem TLS para servidor remoto (" + serverUrl + "). Use HTTPS.");
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
    // JSON helpers (Jackson ObjectMapper)
    // ========================================================================

    /**
     * Cria JSON a partir de um Map usando Jackson.
     */
    private String criarJsonSimples(Map<String, Object> data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            AppLogger.error(TAG, "Erro ao serializar JSON: " + e.getMessage(), e);
            return "{}";
        }
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
     * Parse completo de um JSON — retorna TODOS os key-value pairs via Jackson.
     */
    private Map<String, Object> parseFullJson(String json) {
        if (json == null || json.trim().isEmpty()) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            AppLogger.warn(TAG, "Erro ao parsear JSON completo: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Parse de resposta JSON do servidor (SyncResponse) via Jackson.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();

        if (json == null || json.trim().isEmpty()) {
            result.put("sucesso", false);
            result.put("mensagem", "Resposta vazia do servidor");
            return result;
        }

        try {
            Map<String, Object> parsed = MAPPER.readValue(json,
                new TypeReference<LinkedHashMap<String, Object>>() {});

            result.put("sucesso", Boolean.TRUE.equals(parsed.get("sucesso")));
            result.put("mensagem", parsed.get("mensagem"));
            result.put("registrosRecebidos", parsed.getOrDefault("registrosRecebidos", 0));
            result.put("registrosEnviados", parsed.getOrDefault("registrosEnviados", 0));

            Object download = parsed.get("registrosParaDownload");
            if (download instanceof List) {
                List<Map<String, Object>> registros = new ArrayList<>();
                for (Object item : (List<?>) download) {
                    if (item instanceof Map) {
                        Map<String, Object> registro = (Map<String, Object>) item;
                        // Converter dadosJson de Map para String se necessario
                        Object dadosJson = registro.get("dadosJson");
                        if (dadosJson != null && !(dadosJson instanceof String)) {
                            registro.put("dadosJson", MAPPER.writeValueAsString(dadosJson));
                        }
                        registros.add(registro);
                    }
                }
                result.put("registrosParaDownload", registros);
            }
        } catch (Exception e) {
            result.put("sucesso", false);
            result.put("mensagem", "Erro ao processar resposta: " + e.getMessage());
            AppLogger.error(TAG, "Erro ao parsear resposta JSON: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Extrai um valor string de um JSON pela chave via Jackson.
     */
    private String extrairStringJson(String json, String chave) {
        try {
            Map<String, Object> parsed = MAPPER.readValue(json,
                new TypeReference<Map<String, Object>>() {});
            Object val = parsed.get(chave);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            return null;
        }
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
