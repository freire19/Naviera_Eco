package gui.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import util.AppLogger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente HTTP para o BFF web (Node/Express) em api.naviera.com.br (porta 3003 atras do nginx).
 * Usado primariamente para OCR, que exige endpoints do BFF (nao da API Spring).
 *
 * Autenticacao: login/senha do usuario logado -> JWT do BFF, mantido apenas em memoria.
 */
public class BffClient {

    private static final String TAG = "BffClient";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // URL padrao. Pode ser alterada por configuracao futura.
    private volatile String baseUrl = "https://api.naviera.com.br";
    // Tenant slug do subdominio; lido de db.properties se existir, senao default.
    private volatile String tenantSlug = "deus-de-alianca";
    private volatile String jwtToken = "";

    private static BffClient instance;

    public static synchronized BffClient getInstance() {
        if (instance == null) instance = new BffClient();
        return instance;
    }

    private BffClient() {
        // Le tenant slug do db.properties (empresa.slug) se existir
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("db.properties")) {
                props.load(fis);
            }
            String slug = props.getProperty("empresa.slug");
            if (slug != null && !slug.isEmpty()) this.tenantSlug = slug;
            String url = props.getProperty("bff.url");
            if (url != null && !url.isEmpty()) this.baseUrl = url;
        } catch (Exception e) {
            // defaults ok
        }
    }

    public String getBaseUrl() { return baseUrl; }
    public String getTenantSlug() { return tenantSlug; }
    public boolean isAuthenticated() { return jwtToken != null && !jwtToken.isEmpty(); }

    /**
     * Faz login no BFF. Retorna true se autenticou.
     */
    public boolean login(String loginOuEmail, String senha) {
        String jsonBody = "{\"login\":\"" + escapeJson(loginOuEmail) + "\",\"senha\":\"" + escapeJson(senha) + "\"}";
        HttpURLConnection conn = null;
        try {
            conn = abrirConexao("/api/auth/login", "POST", "application/json");
            conn.setRequestProperty("X-Tenant-Slug", tenantSlug);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String resp = lerResposta(conn);
            if (code == 200) {
                Map<String, Object> j = MAPPER.readValue(resp, new TypeReference<LinkedHashMap<String, Object>>() {});
                Object tok = j.get("token");
                if (tok != null) {
                    this.jwtToken = tok.toString();
                    AppLogger.info(TAG, "Login BFF OK");
                    return true;
                }
            }
            AppLogger.warn(TAG, "Login BFF falhou HTTP " + code + ": " + resp);
            return false;
        } catch (Exception e) {
            AppLogger.error(TAG, "Erro no login BFF: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * POST multipart para /api/ocr/upload.
     *
     * @param foto arquivo de imagem
     * @param tipo 'encomenda', 'frete' ou 'lote'
     * @param idViagem id_viagem ou 0 para null
     * @param numNotaFiscal opcional (apenas frete)
     * @return resposta JSON como Map
     */
    public Map<String, Object> uploadOcr(File foto, String tipo, int idViagem, String numNotaFiscal) throws Exception {
        if (!isAuthenticated()) throw new IllegalStateException("Nao autenticado no BFF. Chame login() primeiro.");
        if (foto == null || !foto.exists()) throw new IllegalArgumentException("Foto invalida");

        String boundary = "----NavieraDesktop" + System.currentTimeMillis();
        HttpURLConnection conn = abrirConexao("/api/ocr/upload", "POST", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        conn.setRequestProperty("X-Tenant-Slug", tenantSlug);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(120_000); // OCR pode demorar

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            // Campo tipo
            escreverCampoTexto(out, boundary, "tipo", tipo != null ? tipo : "encomenda");
            // viagem_id
            if (idViagem > 0) escreverCampoTexto(out, boundary, "viagem_id", String.valueOf(idViagem));
            // num_notafiscal (opcional)
            if (numNotaFiscal != null && !numNotaFiscal.isEmpty()) {
                escreverCampoTexto(out, boundary, "num_notafiscal", numNotaFiscal);
            }
            // client_uuid para idempotencia
            escreverCampoTexto(out, boundary, "client_uuid", java.util.UUID.randomUUID().toString());

            // Arquivo
            String mime = Files.probeContentType(foto.toPath());
            if (mime == null) mime = "application/octet-stream";
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"foto\"; filename=\"" + foto.getName() + "\"\r\n");
            out.writeBytes("Content-Type: " + mime + "\r\n\r\n");
            try (FileInputStream fis = new FileInputStream(foto)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) out.write(buf, 0, n);
            }
            out.writeBytes("\r\n--" + boundary + "--\r\n");
        }

        int code = conn.getResponseCode();
        String resp = lerResposta(conn);
        conn.disconnect();

        if (code == 401) {
            jwtToken = "";
            throw new Exception("Sessao BFF expirada. Faca login novamente.");
        }
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + resp);
        }
        return MAPPER.readValue(resp, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    // ===== helpers =====

    private HttpURLConnection abrirConexao(String endpoint, String method, String contentType) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setInstanceFollowRedirects(false);
        conn.setDoOutput("POST".equals(method) || "PUT".equals(method));
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private void escreverCampoTexto(DataOutputStream out, String boundary, String nome, String valor) throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + nome + "\"\r\n\r\n");
        out.write(valor.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private String lerResposta(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
