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
 * Cliente de sincronização para comunicação com a API do servidor.
 * Gerencia upload/download de dados entre o desktop e a nuvem.
 * 
 * @author Sistema Embarcação
 * @version 1.0.0
 */
public class SyncClient {

    // Configurações - altere para seu servidor
    private static final String CONFIG_FILE = "sync_config.properties";
    private String serverUrl = "http://localhost:8080";
    private String apiToken = "";
    private boolean autoSyncEnabled = false;
    private int syncIntervalMinutes = 5;
    
    private final ScheduledExecutorService scheduler;
    private LocalDateTime ultimaSincronizacao;
    private final List<SyncListener> listeners = new ArrayList<>();
    
    // Singleton
    private static SyncClient instance;
    
    public static synchronized SyncClient getInstance() {
        if (instance == null) {
            instance = new SyncClient();
        }
        return instance;
    }
    
    private SyncClient() {
        this.scheduler = Executors.newScheduledThreadPool(1);
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
                this.apiToken = props.getProperty("api.token", "");
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
            props.setProperty("api.token", apiToken);
            props.setProperty("sync.auto", String.valueOf(autoSyncEnabled));
            props.setProperty("sync.interval", String.valueOf(syncIntervalMinutes));
            if (ultimaSincronizacao != null) {
                props.setProperty("sync.ultima", ultimaSincronizacao.toString());
            }
            props.store(fos, "Configurações de Sincronização - Sistema Embarcação");
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
    public CompletableFuture<Boolean> testarConexao() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(serverUrl + "/api/sync/ping");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                return responseCode == 200;
            } catch (Exception e) {
                notificarListeners(SyncEvent.ERRO, "Falha na conexão: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Inicia sincronização automática.
     */
    public void iniciarSyncAutomatica() {
        if (!autoSyncEnabled) return;
        
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
    public void pararSyncAutomatica() {
        scheduler.shutdown();
        notificarListeners(SyncEvent.INFO, "Sincronização automática parada");
    }
    
    /**
     * Sincroniza todas as tabelas.
     */
    public CompletableFuture<SyncResult> sincronizarTudo() {
        return CompletableFuture.supplyAsync(() -> {
            SyncResult resultadoGeral = new SyncResult();
            
            String[] tabelas = {"passageiros", "passagens", "viagens", "encomendas", "fretes"};
            
            System.out.println("[SYNC DEBUG] ===== INICIANDO SINCRONIZAÇÃO GERAL =====");
            
            for (String tabela : tabelas) {
                try {
                    notificarListeners(SyncEvent.PROGRESSO, "Sincronizando " + tabela + "...");
                    SyncResult resultado = sincronizarTabela(tabela).get();
                    System.out.println("[SYNC DEBUG] Tabela " + tabela + " retornou: enviados=" + resultado.registrosEnviados + ", recebidos=" + resultado.registrosRecebidos);
                    resultadoGeral.registrosEnviados += resultado.registrosEnviados;
                    resultadoGeral.registrosRecebidos += resultado.registrosRecebidos;
                    System.out.println("[SYNC DEBUG] Total acumulado: enviados=" + resultadoGeral.registrosEnviados + ", recebidos=" + resultadoGeral.registrosRecebidos);
                } catch (Exception e) {
                    System.err.println("[SYNC ERROR] Erro em " + tabela + ": " + e.getMessage());
                    e.printStackTrace();
                    resultadoGeral.erros.add(tabela + ": " + e.getMessage());
                }
            }
            
            System.out.println("[SYNC DEBUG] ===== SINCRONIZAÇÃO FINALIZADA =====");
            System.out.println("[SYNC DEBUG] TOTAL FINAL: enviados=" + resultadoGeral.registrosEnviados + ", recebidos=" + resultadoGeral.registrosRecebidos);
            
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
                System.out.println("[SYNC DEBUG] " + tabela + " - Pendentes para enviar: " + pendentes.size());
                
                // 2. Criar requisição de sync
                Map<String, Object> request = new HashMap<>();
                request.put("tabela", tabela);
                request.put("ultimaSincronizacao", ultimaSincronizacao != null 
                    ? ultimaSincronizacao.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                    : null);
                request.put("registros", pendentes);
                
                // 3. Enviar para o servidor
                String jsonRequest = criarJsonRequest(tabela, ultimaSincronizacao, pendentes);
                System.out.println("[SYNC DEBUG] " + tabela + " - Enviando requisição para servidor...");
                String response = enviarPOST("/api/sync", jsonRequest);
                System.out.println("[SYNC DEBUG] " + tabela + " - Resposta recebida: " + (response != null ? response.substring(0, Math.min(200, response.length())) + "..." : "NULL"));
                
                // 4. Processar resposta do servidor
                Map<String, Object> syncResponse = parseJsonResponse(response);
                System.out.println("[SYNC DEBUG] " + tabela + " - Resposta parseada: " + syncResponse);
                
                resultado.sucesso = (Boolean) syncResponse.getOrDefault("sucesso", false);
                resultado.mensagem = (String) syncResponse.getOrDefault("mensagem", "");
                resultado.registrosEnviados = pendentes.size(); // Quantidade que enviamos
                System.out.println("[SYNC DEBUG] " + tabela + " - registrosEnviados definido: " + resultado.registrosEnviados);
                
                // Quantidade que o servidor nos enviou
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> registrosDownload = 
                    (List<Map<String, Object>>) syncResponse.get("registrosParaDownload");
                resultado.registrosRecebidos = registrosDownload != null ? registrosDownload.size() : 0;
                System.out.println("[SYNC DEBUG] " + tabela + " - registrosRecebidos definido: " + resultado.registrosRecebidos);
                
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
            
            System.out.println("[SYNC DEBUG] " + tabela + " - Resultado final: enviados=" + resultado.registrosEnviados + ", recebidos=" + resultado.registrosRecebidos);
            return resultado;
        });
    }
    
    /**
     * Busca registros pendentes de sincronização no banco local.
     */
    private List<Map<String, Object>> buscarRegistrosPendentes(String tabela) {
        List<Map<String, Object>> pendentes = new ArrayList<>();
        
        // Mapeamento de coluna ID por tabela
        String colunaId = getColunaId(tabela);
        
        // Query que funciona mesmo se excluido for NULL
        String sql = "SELECT * FROM " + tabela + " WHERE sincronizado = FALSE AND (excluido = FALSE OR excluido IS NULL)";
        
        System.out.println("[SYNC DEBUG] Buscando pendentes de " + tabela);
        System.out.println("[SYNC DEBUG] SQL: " + sql);
        
        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            
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
            
            System.out.println("[SYNC DEBUG] Encontrados " + pendentes.size() + " registros pendentes em " + tabela);
            
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
        String acao = (String) registro.get("acao");
        String uuid = (String) registro.get("uuid");
        String dadosJson = (String) registro.get("dadosJson");
        
        try {
            // Verificar se já existe localmente
            String sqlCheck = "SELECT id FROM " + tabela + " WHERE uuid = ?::uuid";
            
            try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sqlCheck)) {
                
                stmt.setString(1, uuid);
                java.sql.ResultSet rs = stmt.executeQuery();
                
                if ("DELETE".equals(acao)) {
                    if (rs.next()) {
                        // Marcar como excluído
                        String sqlDel = "UPDATE " + tabela + " SET excluido = TRUE, sincronizado = TRUE WHERE uuid = ?::uuid";
                        try (java.sql.PreparedStatement stmtDel = conn.prepareStatement(sqlDel)) {
                            stmtDel.setString(1, uuid);
                            stmtDel.executeUpdate();
                        }
                    }
                } else if (rs.next()) {
                    // UPDATE - atualizar registro existente
                    // Implementar lógica de atualização específica por tabela
                } else {
                    // INSERT - criar novo registro
                    // Implementar lógica de inserção específica por tabela
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao processar registro de " + tabela + ": " + e.getMessage());
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
        String sql = "UPDATE " + tabela + " SET sincronizado = TRUE WHERE " + colunaId + " = ?";
        
        try (java.sql.Connection conn = dao.ConexaoBD.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int count = 0;
            for (Map<String, Object> reg : registros) {
                Object idLocal = reg.get("idLocal");
                if (idLocal != null) {
                    stmt.setObject(1, idLocal);
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
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
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
        
        InputStream is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        
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
                        System.out.println("[SYNC DEBUG] Parseados " + registros.size() + " registros para download");
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
        
        @Override
        public String toString() {
            return String.format("SyncResult[sucesso=%s, enviados=%d, recebidos=%d, erros=%d]",
                sucesso, registrosEnviados, registrosRecebidos, erros.size());
        }
    }
}
