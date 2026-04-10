package gui.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cliente de sincronizacao bidirecional.
 * Upload e download funcionam via POST /api/sync (ida-e-volta unica por tabela).
 * Conflitos resolvidos por last-write-wins (ultima_atualizacao).
 */
public class SyncClient {

    // Configurações - altere para seu servidor
    private static final String CONFIG_FILE = "sync_config.properties";
    private String serverUrl = "http://localhost:8080";
    private String apiToken = "";
    // DR108: volatile para visibilidade entre threads (scheduler, FX, CompletableFuture)
    private volatile boolean autoSyncEnabled = false;
    private volatile int syncIntervalMinutes = 5;

    // #039/#032: nao-final para permitir recriar apos shutdown via pararSyncAutomatica()
    private ScheduledExecutorService scheduler;
    private volatile LocalDateTime ultimaSincronizacao;
    // DR108: CopyOnWriteArrayList para acesso thread-safe (add/remove na FX thread, iteracao no scheduler)
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
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SyncClient-Scheduler");
            t.setDaemon(true);
            return t;
        });
        carregarConfiguracoes();
    }
    
    /**
     * Carrega configurações do arquivo.
     */
    private void carregarConfiguracoes() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                this.serverUrl = props.getProperty("server.url", serverUrl);
                // D022: decodificar token se encoded
                String tokenRaw = props.getProperty("api.token", "");
                if ("true".equals(props.getProperty("api.token.encoded")) && !tokenRaw.isEmpty()) {
                    try { tokenRaw = new String(java.util.Base64.getDecoder().decode(tokenRaw), java.nio.charset.StandardCharsets.UTF_8); }
                    catch (Exception ignored) { /* fallback: usa valor raw */ }
                }
                this.apiToken = tokenRaw;
                this.autoSyncEnabled = Boolean.parseBoolean(props.getProperty("sync.auto", "false"));
                this.syncIntervalMinutes = Integer.parseInt(props.getProperty("sync.interval", "5"));
                
                String ultimaSync = props.getProperty("sync.ultima", "");
                if (!ultimaSync.isEmpty()) {
                    this.ultimaSincronizacao = LocalDateTime.parse(ultimaSync);
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar configurações de sync: " + e.getMessage());
            }
        }
    }
    
    /**
     * Salva configurações no arquivo.
     */
    public void salvarConfiguracoes() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("server.url", serverUrl);
            // D022: ofuscar token com Base64 (nao e criptografia forte, mas evita leitura casual)
            if (apiToken != null && !apiToken.isEmpty()) {
                props.setProperty("api.token", java.util.Base64.getEncoder().encodeToString(apiToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                props.setProperty("api.token.encoded", "true");
            } else {
                props.setProperty("api.token", "");
                props.setProperty("api.token.encoded", "false");
            }
            props.setProperty("sync.auto", String.valueOf(autoSyncEnabled));
            props.setProperty("sync.interval", String.valueOf(syncIntervalMinutes));
            if (ultimaSincronizacao != null) {
                props.setProperty("sync.ultima", ultimaSincronizacao.toString());
            }
            props.store(fos, "Configurações de Sincronização - Naviera");
        } catch (Exception e) {
            System.err.println("Erro ao salvar configurações de sync: " + e.getMessage());
        }
    }
    
    /**
     * Configura o servidor de sincronização.
     */
    public void configurar(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.apiToken = token;
        salvarConfiguracoes();
    }
    
    /**
     * Testa conexão com o servidor.
     */
    // #DB019: HttpURLConnection com disconnect() em finally
    public CompletableFuture<Boolean> testarConexao() {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(serverUrl + "/api/sync/ping");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                notificarListeners(SyncEvent.ERRO, "Falha na conexão: " + e.getMessage());
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
    
    /**
     * Inicia sincronização automática.
     * #039/#032: recria o scheduler se ele foi encerrado por pararSyncAutomatica().
     */
    public void iniciarSyncAutomatica() {
        if (!autoSyncEnabled) return;

        // Se o scheduler foi encerrado, recria para evitar RejectedExecutionException
        if (scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "SyncClient-Scheduler");
                t.setDaemon(true);
                return t;
            });
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                sincronizarTudo();
            } catch (Exception e) {
                notificarListeners(SyncEvent.ERRO, "Erro na sync automática: " + e.getMessage());
            }
        }, 1, syncIntervalMinutes, TimeUnit.MINUTES);
        
        notificarListeners(SyncEvent.INFO, "Sincronização automática iniciada");
    }
    
    /**
     * Para sincronização automática.
     */
    // #DB028: awaitTermination para garantir conclusao de sync em andamento
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
        notificarListeners(SyncEvent.INFO, "Sincronização automática parada");
    }
    
    /**
     * Sincroniza todas as tabelas.
     */
    public CompletableFuture<SyncResult> sincronizarTudo() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult resultadoGeral = new SyncResult();
            
            String[] tabelas = {"viagens", "passageiros", "cad_clientes_encomenda", "passagens", "encomendas", "fretes"};
            
            for (String tabela : tabelas) {
                try {
                    notificarListeners(SyncEvent.PROGRESSO, "Sincronizando " + tabela + "...");
                    // DR109: timeout para evitar bloqueio indefinido do scheduler
                    SyncResult resultado = sincronizarTabela(tabela).get(60, TimeUnit.SECONDS);
                    resultadoGeral.registrosEnviados += resultado.registrosEnviados;
                    resultadoGeral.registrosRecebidos += resultado.registrosRecebidos;
                } catch (Exception e) {
                    System.err.println("[SYNC ERROR] Erro em " + tabela + ": " + e.getMessage());
                    e.printStackTrace();
                    resultadoGeral.erros.add(tabela + ": " + e.getMessage());
                }
            }
            
            ultimaSincronizacao = LocalDateTime.now();
            salvarConfiguracoes();
            
            resultadoGeral.sucesso = resultadoGeral.erros.isEmpty();
            resultadoGeral.mensagem = resultadoGeral.sucesso 
                ? "Sincronização concluída com sucesso!" 
                : "Sincronização concluída com erros";
            
            notificarListeners(
                resultadoGeral.sucesso ? SyncEvent.SUCESSO : SyncEvent.ERRO,
                resultadoGeral.mensagem
            );
            
            return resultadoGeral;
        });
    }
    
    /**
     * Sincroniza uma tabela específica.
     */
    public CompletableFuture<SyncResult> sincronizarTabela(String tabela) {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult resultado = new SyncResult();
            
            try {
                // 1. Buscar registros pendentes de upload
                List<Map<String, Object>> pendentes = buscarRegistrosPendentes(tabela);
                
                // 2. Criar requisição de sync
                Map<String, Object> request = new HashMap<>();
                request.put("tabela", tabela);
                request.put("ultimaSincronizacao", ultimaSincronizacao != null 
                    ? ultimaSincronizacao.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                    : null);
                request.put("registros", pendentes);
                
                // 3. Enviar para o servidor
                String jsonRequest = criarJsonRequest(tabela, ultimaSincronizacao, pendentes);
                String response = enviarPOST("/api/sync", jsonRequest);
                
                // 4. Processar resposta do servidor
                Map<String, Object> syncResponse = parseJsonResponse(response);
                
                // #DB021: cast seguro — aceita Boolean ou String "true"/"false"
                Object sucessoObj = syncResponse.getOrDefault("sucesso", false);
                resultado.sucesso = sucessoObj instanceof Boolean ? (Boolean) sucessoObj : Boolean.parseBoolean(String.valueOf(sucessoObj));
                resultado.mensagem = (String) syncResponse.getOrDefault("mensagem", "");
                resultado.registrosEnviados = pendentes.size(); // Quantidade que enviamos
                
                // Quantidade que o servidor nos enviou
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> registrosDownload = 
                    (List<Map<String, Object>>) syncResponse.get("registrosParaDownload");
                resultado.registrosRecebidos = registrosDownload != null ? registrosDownload.size() : 0;
                
                if (registrosDownload != null) {
                    for (Map<String, Object> registro : registrosDownload) {
                        processarRegistroRecebido(tabela, registro);
                    }
                }
                
                // 6. Marcar registros locais como sincronizados
                marcarComoSincronizados(tabela, pendentes);
                
            } catch (Exception e) {
                System.err.println("[SYNC ERROR] " + tabela + " - Exceção: " + e.getMessage());
                e.printStackTrace();
                resultado.sucesso = false;
                resultado.mensagem = "Erro: " + e.getMessage();
                resultado.erros.add(e.getMessage());
            }
            return resultado;
        });
    }
    
    /**
     * Busca registros pendentes de sincronização no banco local.
     */
    // #013/D004: Whitelist de tabelas permitidas para sync (previne SQL injection)
    private static final java.util.Set<String> TABELAS_SYNC = new java.util.HashSet<>(
        java.util.Arrays.asList("passageiros", "passagens", "viagens", "encomendas", "fretes", "cad_clientes_encomenda")
    );

    private static void validarTabelaSync(String tabela) {
        if (!TABELAS_SYNC.contains(tabela)) {
            throw new IllegalArgumentException("Tabela nao permitida para sync: " + tabela);
        }
    }

    private List<Map<String, Object>> buscarRegistrosPendentes(String tabela) {
        validarTabelaSync(tabela);
        List<Map<String, Object>> pendentes = new ArrayList<>();

        String colunaId = getColunaId(tabela);

        String sql = "SELECT * FROM " + tabela + " WHERE sincronizado = FALSE AND (excluido = FALSE OR excluido IS NULL) AND empresa_id = ?";

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dao.DAOUtils.empresaId());
            java.sql.ResultSet rs = stmt.executeQuery();
            
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int colunas = meta.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> registro = new HashMap<>();
                for (int i = 1; i <= colunas; i++) {
                    registro.put(meta.getColumnName(i), rs.getObject(i));
                }
                
                // Obter ID correto baseado na tabela
                Object idLocal = registro.get(colunaId);
                
                Map<String, Object> syncRegistro = new HashMap<>();
                syncRegistro.put("uuid", registro.get("uuid"));
                syncRegistro.put("idLocal", idLocal);
                syncRegistro.put("colunaId", colunaId);
                syncRegistro.put("acao", "UPDATE");
                syncRegistro.put("ultimaAtualizacao", registro.get("ultima_atualizacao"));
                syncRegistro.put("dadosJson", criarJsonSimples(registro));
                
                pendentes.add(syncRegistro);
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar pendentes de " + tabela + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return pendentes;
    }
    
    /**
     * Retorna o nome da coluna ID para cada tabela.
     */
    private String getColunaId(String tabela) {
        switch (tabela) {
            case "passageiros": return "id_passageiro";
            case "passagens": return "id_passagem";
            case "viagens": return "id_viagem";
            case "encomendas": return "id_encomenda";
            case "fretes": return "id_frete";
            case "cad_clientes_encomenda": return "id_cliente";
            default: return "id";
        }
    }
    
    /**
     * Processa um registro recebido do servidor.
     */
    private void processarRegistroRecebido(String tabela, Map<String, Object> registro) {
        validarTabelaSync(tabela);
        String acao = (String) registro.get("acao");
        String uuid = (String) registro.get("uuid");
        String dadosJson = (String) registro.get("dadosJson");

        if (uuid == null || uuid.isEmpty()) return;

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection()) {
            String colunaId = getColunaId(tabela);
            int empresaId = dao.DAOUtils.empresaId();

            // Verificar se já existe localmente
            String sqlCheck = "SELECT " + colunaId + " FROM " + tabela + " WHERE uuid = ?::uuid AND empresa_id = ?";
            boolean existe = false;

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sqlCheck)) {
                stmt.setString(1, uuid);
                stmt.setInt(2, empresaId);
                java.sql.ResultSet rs = stmt.executeQuery();
                existe = rs.next();
            }

            if ("DELETE".equals(acao)) {
                if (existe) {
                    String sqlDel = "UPDATE " + tabela + " SET excluido = TRUE, sincronizado = TRUE WHERE uuid = ?::uuid AND empresa_id = ?";
                    try (java.sql.PreparedStatement stmtDel = conn.prepareStatement(sqlDel)) {
                        stmtDel.setString(1, uuid);
                        stmtDel.setInt(2, empresaId);
                        stmtDel.executeUpdate();
                    }
                }
            } else if (existe) {
                // UPDATE — aplicar dados recebidos do servidor
                Map<String, Object> dados = parseFullJson(dadosJson);
                if (dados.isEmpty()) return;

                Set<String> skipCols = new HashSet<>(Arrays.asList("uuid", "sincronizado", "empresa_id", colunaId));
                List<String> setClauses = new ArrayList<>();
                List<Object> valores = new ArrayList<>();

                for (Map.Entry<String, Object> entry : dados.entrySet()) {
                    if (skipCols.contains(entry.getKey())) continue;
                    setClauses.add(entry.getKey() + " = ?");
                    valores.add(entry.getValue());
                }

                if (setClauses.isEmpty()) return;

                // sincronizado = TRUE → trigger bypass
                setClauses.add("sincronizado = TRUE");

                String sql = "UPDATE " + tabela + " SET " + String.join(", ", setClauses) +
                    " WHERE uuid = ?::uuid AND empresa_id = ?";
                valores.add(uuid);
                valores.add(empresaId);

                try (java.sql.PreparedStatement stmtUpd = conn.prepareStatement(sql)) {
                    for (int i = 0; i < valores.size(); i++) {
                        stmtUpd.setObject(i + 1, valores.get(i));
                    }
                    stmtUpd.executeUpdate();
                }
            } else {
                // INSERT — registro novo vindo do servidor
                Map<String, Object> dados = parseFullJson(dadosJson);
                if (dados.isEmpty()) return;

                List<String> colunas = new ArrayList<>();
                List<String> placeholders = new ArrayList<>();
                List<Object> valores = new ArrayList<>();

                for (Map.Entry<String, Object> entry : dados.entrySet()) {
                    String col = entry.getKey();
                    if (col.equals(colunaId)) continue; // skip auto-increment

                    colunas.add(col);
                    if ("uuid".equals(col)) {
                        placeholders.add("?::uuid");
                    } else {
                        placeholders.add("?");
                    }
                    valores.add(entry.getValue());
                }

                // Garantir sincronizado = TRUE
                if (!colunas.contains("sincronizado")) {
                    colunas.add("sincronizado");
                    placeholders.add("?");
                    valores.add(true);
                }

                // Garantir empresa_id
                if (!colunas.contains("empresa_id")) {
                    colunas.add("empresa_id");
                    placeholders.add("?");
                    valores.add(empresaId);
                }

                String sql = "INSERT INTO " + tabela +
                    " (" + String.join(", ", colunas) + ") VALUES (" +
                    String.join(", ", placeholders) + ")";

                try (java.sql.PreparedStatement stmtIns = conn.prepareStatement(sql)) {
                    for (int i = 0; i < valores.size(); i++) {
                        stmtIns.setObject(i + 1, valores.get(i));
                    }
                    stmtIns.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar registro de " + tabela + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Marca registros como sincronizados.
     */
    private void marcarComoSincronizados(String tabela, List<Map<String, Object>> registros) {
        if (registros.isEmpty()) {
            System.out.println("Nenhum registro para marcar como sincronizado em " + tabela);
            return;
        }
        
        String colunaId = getColunaId(tabela);
        
        // Usar ID em vez de UUID para evitar problemas de cast
        String sql = "UPDATE " + tabela + " SET sincronizado = TRUE WHERE " + colunaId + " = ? AND empresa_id = ?";

        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            int empresaId = dao.DAOUtils.empresaId();
            int count = 0;
            for (Map<String, Object> reg : registros) {
                Object idLocal = reg.get("idLocal");
                if (idLocal != null) {
                    stmt.setObject(1, idLocal);
                    stmt.setInt(2, empresaId);
                    stmt.addBatch();
                    count++;
                }
            }
            
            if (count > 0) {
                int[] results = stmt.executeBatch();
                System.out.println("Marcados como sincronizados em " + tabela + ": " + results.length + " registros");
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao marcar como sincronizados: " + e.getMessage());
        }
    }
    
    /**
     * Envia requisição POST para o servidor.
     */
    private String enviarPOST(String endpoint, String jsonBody) throws Exception {
        // D023: aviso se usando HTTP em servidor remoto (nao-localhost)
        if (serverUrl != null && serverUrl.startsWith("http://") && !serverUrl.contains("localhost") && !serverUrl.contains("127.0.0.1")) {
            System.err.println("AVISO SEGURANCA: Sync usando HTTP sem TLS para servidor remoto: " + serverUrl);
        }
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // DS011: desabilitar redirects para evitar vazamento de token
        conn.setInstanceFollowRedirects(false);
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        if (apiToken != null && !apiToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        }
        
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        
        // #DB020: null check em InputStream (getErrorStream pode retornar null)
        InputStream is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            throw new Exception("Erro HTTP " + responseCode + ": sem stream de resposta");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            
            if (responseCode >= 400) {
                throw new Exception("Erro HTTP " + responseCode + ": " + response);
            }
            
            return response.toString();
        }
    }
    
    /**
     * Cria JSON simples sem usar biblioteca externa.
     */
    private String criarJsonSimples(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean primeiro = true;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!primeiro) json.append(",");
            primeiro = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object valor = entry.getValue();
            if (valor == null) {
                json.append("null");
            } else if (valor instanceof String) {
                json.append("\"").append(escapeJson((String)valor)).append("\"");
            } else if (valor instanceof Number || valor instanceof Boolean) {
                json.append(valor);
            } else {
                json.append("\"").append(valor.toString()).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Cria JSON para requisição de sincronização.
     */
    private String criarJsonRequest(String tabela, LocalDateTime ultimaSync, List<Map<String, Object>> registros) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"tabela\":\"").append(tabela).append("\",");
        
        if (ultimaSync != null) {
            json.append("\"ultimaSincronizacao\":\"").append(ultimaSync.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",");
        }
        
        json.append("\"registros\":[");
        for (int i = 0; i < registros.size(); i++) {
            if (i > 0) json.append(",");
            json.append(criarJsonSimples(registros.get(i)));
        }
        json.append("]}");
        
        return json.toString();
    }
    
    /**
     * Escapa caracteres especiais para JSON.
     */
    private String escapeJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
    
    /**
     * Faz parse de uma resposta JSON simples.
     */
    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            result.put("sucesso", false);
            result.put("mensagem", "Resposta vazia do servidor");
            return result;
        }
        
        try {
            String jsonOriginal = json.trim();
            
            // Verificar sucesso
            if (jsonOriginal.contains("\"sucesso\":true") || jsonOriginal.contains("\"sucesso\": true")) {
                result.put("sucesso", true);
            } else {
                result.put("sucesso", false);
            }
            
            // Extrair mensagem
            int msgStart = jsonOriginal.indexOf("\"mensagem\":");
            if (msgStart >= 0) {
                int valueStart = jsonOriginal.indexOf("\"", msgStart + 11) + 1;
                int valueEnd = jsonOriginal.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    result.put("mensagem", jsonOriginal.substring(valueStart, valueEnd));
                }
            }
            
            // Extrair registrosEnviados (do servidor)
            result.put("registrosEnviados", extrairNumero(jsonOriginal, "\"registrosEnviados\":"));
            
            // Extrair registrosRecebidos (do servidor)
            result.put("registrosRecebidos", extrairNumero(jsonOriginal, "\"registrosRecebidos\":"));
            
            // Extrair registrosParaDownload como lista
            int downloadStart = jsonOriginal.indexOf("\"registrosParaDownload\":");
            if (downloadStart >= 0) {
                int arrayStart = jsonOriginal.indexOf("[", downloadStart);
                if (arrayStart >= 0) {
                    // Encontrar o final do array - contar colchetes
                    int nivel = 0;
                    int arrayEnd = -1;
                    for (int i = arrayStart; i < jsonOriginal.length(); i++) {
                        char c = jsonOriginal.charAt(i);
                        if (c == '[') nivel++;
                        else if (c == ']') {
                            nivel--;
                            if (nivel == 0) {
                                arrayEnd = i + 1;
                                break;
                            }
                        }
                    }
                    
                    if (arrayEnd > arrayStart) {
                        String arrayJson = jsonOriginal.substring(arrayStart, arrayEnd);
                        List<Map<String, Object>> registros = parseJsonArray(arrayJson);
                        result.put("registrosParaDownload", registros);
                    }
                }
            }
            
        } catch (Exception e) {
            result.put("sucesso", false);
            result.put("mensagem", "Erro ao processar resposta: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Extrai um número de uma string JSON.
     */
    private int extrairNumero(String json, String chave) {
        int idx = json.indexOf(chave);
        if (idx >= 0) {
            int numStart = idx + chave.length();
            StringBuilder numStr = new StringBuilder();
            for (int i = numStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (Character.isDigit(c)) {
                    numStr.append(c);
                } else if (numStr.length() > 0) {
                    break;
                }
            }
            if (numStr.length() > 0) {
                return Integer.parseInt(numStr.toString());
            }
        }
        return 0;
    }
    
    /**
     * Parse de array JSON simples.
     */
    private List<Map<String, Object>> parseJsonArray(String arrayJson) {
        List<Map<String, Object>> lista = new ArrayList<>();
        
        if (arrayJson == null || arrayJson.trim().isEmpty() || arrayJson.equals("[]")) {
            return lista;
        }
        
        try {
            // Remover colchetes externos
            arrayJson = arrayJson.trim();
            if (arrayJson.startsWith("[")) arrayJson = arrayJson.substring(1);
            if (arrayJson.endsWith("]")) arrayJson = arrayJson.substring(0, arrayJson.length() - 1);
            
            // Encontrar cada objeto no array
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
                        Map<String, Object> obj = parseJsonObject(objJson);
                        lista.add(obj);
                        objStart = -1;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear array JSON: " + e.getMessage());
        }
        
        return lista;
    }
    
    /**
     * Parse de objeto JSON simples.
     */
    private Map<String, Object> parseJsonObject(String objJson) {
        Map<String, Object> obj = new HashMap<>();
        
        try {
            // Remover chaves externas
            objJson = objJson.trim();
            if (objJson.startsWith("{")) objJson = objJson.substring(1);
            if (objJson.endsWith("}")) objJson = objJson.substring(0, objJson.length() - 1);
            
            // Extrair campos importantes: uuid, id, acao, dadosJson
            obj.put("uuid", extrairString(objJson, "\"uuid\":"));
            obj.put("id", extrairNumero(objJson, "\"id\":"));
            obj.put("acao", extrairString(objJson, "\"acao\":"));
            obj.put("dadosJson", extrairString(objJson, "\"dadosJson\":"));
            obj.put("ultimaAtualizacao", extrairString(objJson, "\"ultimaAtualizacao\":"));
            
        } catch (Exception e) {
            System.err.println("Erro ao parsear objeto JSON: " + e.getMessage());
        }
        
        return obj;
    }
    
    /**
     * Extrai uma string de JSON.
     */
    private String extrairString(String json, String chave) {
        int idx = json.indexOf(chave);
        if (idx >= 0) {
            int valueStart = json.indexOf("\"", idx + chave.length());
            if (valueStart >= 0) {
                valueStart++; // Pular a aspa de abertura
                // Encontrar aspa de fechamento, cuidando de escapes
                int valueEnd = valueStart;
                boolean escape = false;
                for (int i = valueStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escape) {
                        escape = false;
                    } else if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        valueEnd = i;
                        break;
                    }
                }
                if (valueEnd > valueStart) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        }
        return null;
    }
    
    /**
     * Parse completo de um JSON flat — retorna TODOS os key-value pairs.
     * Suporta string, number, boolean e null.
     */
    private Map<String, Object> parseFullJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) return result;

        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            // Procurar inicio da chave
            int keyStart = json.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = json.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart + 1, keyEnd);

            // Procurar ':'
            int colon = json.indexOf(':', keyEnd + 1);
            if (colon < 0) break;

            // Ler valor
            int valueStart = colon + 1;
            while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

            if (valueStart >= json.length()) break;

            char c = json.charAt(valueStart);
            Object value;

            if (c == '"') {
                // String value — encontrar aspa de fechamento (cuidando de escapes)
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
                // Objeto ou array aninhado — pular (nao suportado em flat parse)
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
                // Number
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

            // Avançar até próxima vírgula ou fim
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) == ' ')) i++;
        }

        return result;
    }

    /**
     * Obtém status de sincronização formatado.
     */
    public String obterStatusSincronizacao() {
        StringBuilder status = new StringBuilder();
        status.append("URL do Servidor: ").append(serverUrl).append("\n");
        status.append("Sincronização Automática: ").append(autoSyncEnabled ? "Ativada" : "Desativada").append("\n");
        
        if (autoSyncEnabled) {
            status.append("Intervalo: ").append(syncIntervalMinutes).append(" minutos\n");
        }
        
        if (ultimaSincronizacao != null) {
            status.append("Última Sincronização: ")
                  .append(ultimaSincronizacao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                  .append("\n");
        } else {
            status.append("Última Sincronização: Nunca sincronizado\n");
        }
        
        return status.toString();
    }
    
    // === Listeners ===
    
    /**
     * #035: Recebe dados do servidor e aplica no banco local.
     * TODO: implementar download de dados (pagamentos app, feedbacks, pedidos online).
     * Fluxo esperado: GET /api/sync/download?since={lastSync} → parse JSON → INSERT/UPDATE local.
     */
    public CompletableFuture<SyncResult> receberDadosDoServidor() {
        return CompletableFuture.supplyAsync(() -> {
            notificarListeners(SyncEvent.INFO, "Recebimento de dados nao implementado.");
            System.err.println("SyncClient.receberDadosDoServidor: fluxo de recebimento ainda nao implementado (issue #035).");
            notificarListeners(SyncEvent.ERRO, "Funcionalidade de recebimento pendente de implementacao.");
            return new SyncResult(false, "Recebimento de dados do servidor ainda nao implementado.");
        });
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
                e.printStackTrace();
            }
        }
    }
    
    // === Getters/Setters ===
    
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    
    public boolean isAutoSyncEnabled() { return autoSyncEnabled; }
    public void setAutoSyncEnabled(boolean enabled) { this.autoSyncEnabled = enabled; }
    
    public int getSyncIntervalMinutes() { return syncIntervalMinutes; }
    public void setSyncIntervalMinutes(int minutes) { this.syncIntervalMinutes = minutes; }
    
    public LocalDateTime getUltimaSincronizacao() { return ultimaSincronizacao; }
    
    // === Classes internas ===
    
    public enum SyncEvent {
        INFO, PROGRESSO, SUCESSO, ERRO
    }
    
    public interface SyncListener {
        void onSyncEvent(SyncEvent evento, String mensagem);
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
