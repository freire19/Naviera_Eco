package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import util.AppLogger;

/**
 * DAO para tabelas auxiliares com cache em memoria.
 * Cache elimina N+1 queries — tabelas auxiliares sao pequenas e raramente mudam.
 *
 * Refatorado: 35 metodos especificos agora delegam para 5 metodos genericos.
 */
public class AuxiliaresDAO {

    // ==================== CONFIGURACAO DAS TABELAS ====================

    /** Whitelist de tabelas permitidas (previne SQL injection via nome de tabela). */
    private static final List<String> TABELAS_PERMITIDAS = Arrays.asList(
        "aux_tipos_documento", "aux_sexo", "aux_nacionalidades", "aux_tipos_passagem",
        "aux_agentes", "aux_horarios_saida", "aux_acomodacoes", "aux_formas_pagamento", "caixas", "rotas", "embarcacoes"
    );

    /** Tabelas que possuem empresa_id (tenant-scoped, nao compartilhadas). */
    private static final List<String> TABELAS_COM_TENANT = Arrays.asList("caixas", "rotas", "embarcacoes");

    private static boolean isTenantScoped(String tabela) {
        return TABELAS_COM_TENANT.contains(tabela);
    }

    /** DS002: Whitelist de colunas por tabela (previne SQL injection via nome de coluna). */
    private static final java.util.Map<String, java.util.Set<String>> COLUNAS_PERMITIDAS = new java.util.HashMap<>();
    static {
        COLUNAS_PERMITIDAS.put("aux_tipos_documento", new java.util.HashSet<>(Arrays.asList("id_tipo_doc", "nome_tipo_doc")));
        COLUNAS_PERMITIDAS.put("aux_sexo", new java.util.HashSet<>(Arrays.asList("id_sexo", "nome_sexo")));
        COLUNAS_PERMITIDAS.put("aux_nacionalidades", new java.util.HashSet<>(Arrays.asList("id_nacionalidade", "nome_nacionalidade")));
        COLUNAS_PERMITIDAS.put("aux_tipos_passagem", new java.util.HashSet<>(Arrays.asList("id_tipo_passagem", "nome_tipo_passagem")));
        COLUNAS_PERMITIDAS.put("aux_agentes", new java.util.HashSet<>(Arrays.asList("id_agente", "nome_agente")));
        COLUNAS_PERMITIDAS.put("aux_horarios_saida", new java.util.HashSet<>(Arrays.asList("id_horario_saida", "descricao_horario_saida")));
        COLUNAS_PERMITIDAS.put("aux_acomodacoes", new java.util.HashSet<>(Arrays.asList("id_acomodacao", "nome_acomodacao")));
        COLUNAS_PERMITIDAS.put("aux_formas_pagamento", new java.util.HashSet<>(Arrays.asList("id_forma_pagamento", "nome_forma_pagamento")));
        COLUNAS_PERMITIDAS.put("caixas", new java.util.HashSet<>(Arrays.asList("id_caixa", "nome_caixa")));
        COLUNAS_PERMITIDAS.put("rotas", new java.util.HashSet<>(Arrays.asList("id", "origem", "destino")));
        COLUNAS_PERMITIDAS.put("embarcacoes", new java.util.HashSet<>(Arrays.asList("id_embarcacao", "nome")));
    }

    private static void validarTabela(String tabela) {
        if (!TABELAS_PERMITIDAS.contains(tabela)) {
            throw new IllegalArgumentException("Tabela nao permitida: " + tabela);
        }
    }

    private static void validarColuna(String tabela, String... colunas) {
        java.util.Set<String> permitidas = COLUNAS_PERMITIDAS.get(tabela);
        if (permitidas == null) return; // tabela validada separadamente
        for (String col : colunas) {
            if (!permitidas.contains(col)) {
                throw new IllegalArgumentException("Coluna nao permitida: " + col + " na tabela " + tabela);
            }
        }
    }

    // ==================== CACHE EM MEMORIA ====================

    // Cache: tabela -> (nome_lower -> id)
    private static final Map<String, Map<String, Integer>> cacheNomeParaId = new ConcurrentHashMap<>();
    // Cache: tabela -> (id -> nome)
    private static final Map<String, Map<Integer, String>> cacheIdParaNome = new ConcurrentHashMap<>();

    /** Carrega todos os registros de uma tabela auxiliar no cache. Filtra por empresa_id se tenant-scoped. */
    private static void carregarCache(String tabela, String colunaNome, String colunaId) throws SQLException {
        Map<String, Integer> nomeId = new ConcurrentHashMap<>();
        Map<Integer, String> idNome = new ConcurrentHashMap<>();
        String sql = "SELECT " + colunaId + ", " + colunaNome + " FROM " + tabela;
        if (isTenantScoped(tabela)) {
            sql += " WHERE empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (isTenantScoped(tabela)) {
                stmt.setInt(1, DAOUtils.empresaId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(colunaId);
                    String nome = rs.getString(colunaNome);
                    if (nome != null) {
                        nomeId.put(nome.toLowerCase().trim(), id);
                        idNome.put(id, nome);
                    }
                }
            }
        }
        // DP040: segmentar cache por tenant para tabelas tenant-scoped
        String key = cacheKey(tabela);
        cacheNomeParaId.put(key, nomeId);
        cacheIdParaNome.put(key, idNome);
    }

    /** DP040: gera chave de cache segmentada por tenant para tabelas tenant-scoped */
    private static String cacheKey(String tabela) {
        return isTenantScoped(tabela) ? tabela + ":" + DAOUtils.empresaId() : tabela;
    }

    /** Invalida o cache de uma tabela (apos insert/update/delete). */
    private static void invalidarCache(String tabela) {
        String key = cacheKey(tabela);
        cacheNomeParaId.remove(key);
        cacheIdParaNome.remove(key);
    }

    /** Invalida todo o cache (para uso externo se necessario). */
    public static void invalidarTodoCache() {
        cacheNomeParaId.clear();
        cacheIdParaNome.clear();
    }

    /**
     * #DB030: Pre-carrega caches das tabelas usadas em mapeamento de passagens.
     * Chamar antes de iterar listas grandes para evitar N+1 no cold-start.
     */
    public void preCarregarCachesPassagem() throws SQLException {
        String[][] tabelas = {
            {"aux_acomodacoes", "nome_acomodacao", "id_acomodacao"},
            {"aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem"},
            {"aux_agentes", "nome_agente", "id_agente"},
            {"aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento"},
            {"caixas", "nome_caixa", "id_caixa"}
        };
        for (String[] t : tabelas) {
            if (!cacheIdParaNome.containsKey(cacheKey(t[0]))) {
                carregarCache(t[0], t[1], t[2]);
            }
        }
    }

    // ==================== 5 METODOS GENERICOS (core) ====================

    /**
     * Busca ID pelo nome em qualquer tabela auxiliar. Usa cache.
     */
    public Integer obterIdAuxiliar(String tabela, String colunaNome, String colunaId, String valorNome) throws SQLException {
        if (valorNome == null || valorNome.trim().isEmpty() || "N/A".equalsIgnoreCase(valorNome)) {
            return null;
        }
        validarTabela(tabela);
        validarColuna(tabela, colunaNome, colunaId);

        // Tenta cache primeiro (DP040: chave segmentada por tenant)
        String key = cacheKey(tabela);
        Map<String, Integer> cache = cacheNomeParaId.get(key);
        if (cache == null) {
            carregarCache(tabela, colunaNome, colunaId);
            cache = cacheNomeParaId.get(key);
        }
        if (cache != null) {
            Integer id = cache.get(valorNome.toLowerCase().trim());
            if (id != null) return id;
        }

        // Fallback: busca direta (ILIKE para acentos/case)
        String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
        if (isTenantScoped(tabela)) {
            sql += " AND empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valorNome);
            if (isTenantScoped(tabela)) {
                stmt.setInt(2, DAOUtils.empresaId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(colunaId);
                    // Atualiza cache
                    if (cache != null) cache.put(valorNome.toLowerCase().trim(), id);
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * Busca nome pelo ID em qualquer tabela auxiliar. Usa cache.
     */
    public String buscarNomeAuxiliarPorId(String tabela, String colunaNome, String colunaId, Integer id) throws SQLException {
        if (id == null || id == 0) return null;
        validarTabela(tabela);
        validarColuna(tabela, colunaNome, colunaId);

        // DP040: chave segmentada por tenant
        String key = cacheKey(tabela);
        Map<Integer, String> cache = cacheIdParaNome.get(key);
        if (cache == null) {
            carregarCache(tabela, colunaNome, colunaId);
            cache = cacheIdParaNome.get(key);
        }
        if (cache != null) {
            String nome = cache.get(id);
            if (nome != null) return nome;
        }

        // Fallback
        String sql = "SELECT " + colunaNome + " FROM " + tabela + " WHERE " + colunaId + " = ?";
        if (isTenantScoped(tabela)) {
            sql += " AND empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            if (isTenantScoped(tabela)) {
                stmt.setInt(2, DAOUtils.empresaId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(colunaNome);
            }
        }
        return null;
    }

    /**
     * Insere um valor em qualquer tabela auxiliar (generico).
     * DL008: Verifica duplicata antes de inserir (case-insensitive).
     */
    // #DB004: INSERT ON CONFLICT DO NOTHING — atomico, sem TOCTOU race condition
    public boolean inserirAuxiliar(String tabela, String colunaNome, String valor) throws SQLException {
        if (valor == null || valor.trim().isEmpty()) return false;
        validarTabela(tabela);
        validarColuna(tabela, colunaNome);
        String sql;
        if (isTenantScoped(tabela)) {
            sql = "INSERT INTO " + tabela + " (" + colunaNome + ", empresa_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        } else {
            sql = "INSERT INTO " + tabela + " (" + colunaNome + ") VALUES (?) ON CONFLICT DO NOTHING";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor.trim());
            if (isTenantScoped(tabela)) {
                ps.setInt(2, DAOUtils.empresaId());
            }
            int rows = ps.executeUpdate();
            if (rows > 0) {
                invalidarCache(tabela);
                return true;
            }
            return false; // ja existia (ON CONFLICT)
        }
    }

    /**
     * Lista todos os nomes de qualquer tabela auxiliar.
     */
    public List<String> listarAuxiliar(String tabela, String colunaNome) throws SQLException {
        validarTabela(tabela);
        validarColuna(tabela, colunaNome);
        List<String> lista = new ArrayList<>();
        String sql = "SELECT " + colunaNome + " FROM " + tabela;
        if (isTenantScoped(tabela)) {
            sql += " WHERE empresa_id = ?";
        }
        sql += " ORDER BY " + colunaNome;
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (isTenantScoped(tabela)) {
                ps.setInt(1, DAOUtils.empresaId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getString(colunaNome));
            }
        }
        return lista;
    }

    /**
     * Atualiza um registro em qualquer tabela auxiliar.
     */
    public boolean atualizarAuxiliar(String tabela, String colunaNome, String colunaId, int id, String novoNome) throws SQLException {
        validarTabela(tabela);
        validarColuna(tabela, colunaNome, colunaId);
        String sql = "UPDATE " + tabela + " SET " + colunaNome + "=? WHERE " + colunaId + "=?";
        if (isTenantScoped(tabela)) {
            sql += " AND empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
            if (isTenantScoped(tabela)) {
                ps.setInt(3, DAOUtils.empresaId());
            }
            boolean ok = ps.executeUpdate() > 0;
            if (ok) invalidarCache(tabela);
            return ok;
        }
    }

    /**
     * Exclui um registro de qualquer tabela auxiliar.
     * DL007: Tenta DELETE e captura FK violation em vez de check manual
     * (cada tabela auxiliar pode ser referenciada por tabelas diferentes).
     */
    public boolean excluirAuxiliar(String tabela, String colunaId, int id) throws SQLException {
        validarTabela(tabela);
        validarColuna(tabela, colunaId);
        String sql = "DELETE FROM " + tabela + " WHERE " + colunaId + "=?";
        if (isTenantScoped(tabela)) {
            sql += " AND empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (isTenantScoped(tabela)) {
                ps.setInt(2, DAOUtils.empresaId());
            }
            boolean ok = ps.executeUpdate() > 0;
            if (ok) invalidarCache(tabela);
            return ok;
        } catch (SQLException e) {
            // FK violation (23503) = registro em uso por outra tabela
            if ("23503".equals(e.getSQLState())) {
                AppLogger.warn("AuxiliaresDAO", "Registro id=" + id + " da tabela " + tabela + " nao pode ser excluido: em uso por outra tabela.");
                return false;
            }
            throw e;
        }
    }

    // ==================== METODO ESPECIAL: Rota ====================

    public Integer obterIdRotaPelaOrigemDestino(String origem, String destino) throws SQLException {
        if (origem == null || origem.trim().isEmpty()) return null;
        String sql;
        if (destino == null || destino.trim().isEmpty()) {
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND (destino IS NULL OR destino = '') AND empresa_id = ?";
        } else {
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND destino ILIKE ? AND empresa_id = ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, origem);
            if (destino != null && !destino.trim().isEmpty()) {
                stmt.setString(2, destino);
                stmt.setInt(3, DAOUtils.empresaId());
            } else {
                stmt.setInt(2, DAOUtils.empresaId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }

    // ==================== METODOS DE CONVENIENCIA (somente leitura) ====================

    // --- FormasPagamento / Caixas (somente leitura) ---
    public List<String> listarTiposPagamento() throws SQLException { return listarAuxiliar("aux_formas_pagamento", "nome_forma_pagamento"); }
    public Integer buscarIdTipoPagamentoPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_formas_pagamento", "nome_forma_pagamento", "id_forma_pagamento", nome); }
    public List<String> listarCaixas() throws SQLException { return listarAuxiliar("caixas", "nome_caixa"); }

    // --- Descricao Horario ---
    public String obterDescricaoHorario(Long idHorario) throws SQLException {
        if (idHorario == null || idHorario == 0) return "";
        String nome = buscarNomeAuxiliarPorId("aux_horarios_saida", "descricao_horario_saida", "id_horario_saida", idHorario.intValue());
        return nome != null ? nome : "";
    }
}
