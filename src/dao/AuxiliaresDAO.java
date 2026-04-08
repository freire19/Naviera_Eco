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
        "aux_agentes", "aux_horarios_saida", "aux_acomodacoes", "aux_formas_pagamento", "caixas", "rotas"
    );

    private static void validarTabela(String tabela) {
        if (!TABELAS_PERMITIDAS.contains(tabela)) {
            throw new IllegalArgumentException("Tabela nao permitida: " + tabela);
        }
    }

    // ==================== CACHE EM MEMORIA ====================

    // Cache: tabela -> (nome_lower -> id)
    private static final Map<String, Map<String, Integer>> cacheNomeParaId = new ConcurrentHashMap<>();
    // Cache: tabela -> (id -> nome)
    private static final Map<String, Map<Integer, String>> cacheIdParaNome = new ConcurrentHashMap<>();

    /** Carrega todos os registros de uma tabela auxiliar no cache. */
    private static void carregarCache(String tabela, String colunaNome, String colunaId) throws SQLException {
        Map<String, Integer> nomeId = new ConcurrentHashMap<>();
        Map<Integer, String> idNome = new ConcurrentHashMap<>();
        String sql = "SELECT " + colunaId + ", " + colunaNome + " FROM " + tabela;
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt(colunaId);
                String nome = rs.getString(colunaNome);
                if (nome != null) {
                    nomeId.put(nome.toLowerCase().trim(), id);
                    idNome.put(id, nome);
                }
            }
        }
        cacheNomeParaId.put(tabela, nomeId);
        cacheIdParaNome.put(tabela, idNome);
    }

    /** Invalida o cache de uma tabela (apos insert/update/delete). */
    private static void invalidarCache(String tabela) {
        cacheNomeParaId.remove(tabela);
        cacheIdParaNome.remove(tabela);
    }

    /** Invalida todo o cache (para uso externo se necessario). */
    public static void invalidarTodoCache() {
        cacheNomeParaId.clear();
        cacheIdParaNome.clear();
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

        // Tenta cache primeiro
        Map<String, Integer> cache = cacheNomeParaId.get(tabela);
        if (cache == null) {
            carregarCache(tabela, colunaNome, colunaId);
            cache = cacheNomeParaId.get(tabela);
        }
        if (cache != null) {
            Integer id = cache.get(valorNome.toLowerCase().trim());
            if (id != null) return id;
        }

        // Fallback: busca direta (ILIKE para acentos/case)
        String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valorNome);
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

        Map<Integer, String> cache = cacheIdParaNome.get(tabela);
        if (cache == null) {
            carregarCache(tabela, colunaNome, colunaId);
            cache = cacheIdParaNome.get(tabela);
        }
        if (cache != null) {
            String nome = cache.get(id);
            if (nome != null) return nome;
        }

        // Fallback
        String sql = "SELECT " + colunaNome + " FROM " + tabela + " WHERE " + colunaId + " = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
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
    public boolean inserirAuxiliar(String tabela, String colunaNome, String valor) throws SQLException {
        if (valor == null || valor.trim().isEmpty()) return false;
        validarTabela(tabela);
        // Verifica duplicata case-insensitive antes de inserir
        String sqlCheck = "SELECT 1 FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
        try (Connection conn = ConexaoBD.getConnection()) {
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setString(1, valor.trim());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) return false; // ja existe
                }
            }
            String sql = "INSERT INTO " + tabela + " (" + colunaNome + ") VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, valor.trim());
                boolean ok = ps.executeUpdate() > 0;
                if (ok) invalidarCache(tabela);
                return ok;
            }
        }
    }

    /**
     * Lista todos os nomes de qualquer tabela auxiliar.
     */
    public List<String> listarAuxiliar(String tabela, String colunaNome) throws SQLException {
        validarTabela(tabela);
        List<String> lista = new ArrayList<>();
        String sql = "SELECT " + colunaNome + " FROM " + tabela + " ORDER BY " + colunaNome;
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString(colunaNome));
        }
        return lista;
    }

    /**
     * Atualiza um registro em qualquer tabela auxiliar.
     */
    public boolean atualizarAuxiliar(String tabela, String colunaNome, String colunaId, int id, String novoNome) throws SQLException {
        validarTabela(tabela);
        String sql = "UPDATE " + tabela + " SET " + colunaNome + "=? WHERE " + colunaId + "=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novoNome);
            ps.setInt(2, id);
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
        String sql = "DELETE FROM " + tabela + " WHERE " + colunaId + "=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) invalidarCache(tabela);
            return ok;
        } catch (SQLException e) {
            // FK violation (23503) = registro em uso por outra tabela
            if ("23503".equals(e.getSQLState())) {
                System.err.println("Registro id=" + id + " da tabela " + tabela + " nao pode ser excluido: em uso por outra tabela.");
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
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND (destino IS NULL OR destino = '')";
        } else {
            sql = "SELECT id FROM rotas WHERE origem ILIKE ? AND destino ILIKE ?";
        }
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, origem);
            if (destino != null && !destino.trim().isEmpty()) stmt.setString(2, destino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }

    // ==================== METODOS LEGADOS (delegam para genericos) ====================
    // DM006: Mantidos temporariamente — usar os 5 genericos acima em novos codigos.
    // Caller unico: TabelasAuxiliaresController.

    // --- TipoDoc ---
    public boolean inserirTipoDoc(String nome) throws SQLException { return inserirAuxiliar("aux_tipos_documento", "nome_tipo_doc", nome); }
    public List<String> listarTipoDoc() throws SQLException { return listarAuxiliar("aux_tipos_documento", "nome_tipo_doc"); }
    public boolean atualizarTipoDoc(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", id, novoNome); }
    public boolean excluirTipoDoc(int id) throws SQLException { return excluirAuxiliar("aux_tipos_documento", "id_tipo_doc", id); }
    public Integer buscarIdTipoDocPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", nome); }

    // --- Sexo ---
    public boolean inserirSexo(String nome) throws SQLException { return inserirAuxiliar("aux_sexo", "nome_sexo", nome); }
    public List<String> listarSexo() throws SQLException { return listarAuxiliar("aux_sexo", "nome_sexo"); }
    public boolean atualizarSexo(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_sexo", "nome_sexo", "id_sexo", id, novoNome); }
    public boolean excluirSexo(int id) throws SQLException { return excluirAuxiliar("aux_sexo", "id_sexo", id); }
    public Integer buscarIdSexoPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_sexo", "nome_sexo", "id_sexo", nome); }

    // --- Nacionalidade ---
    public boolean inserirNacionalidade(String nome) throws SQLException { return inserirAuxiliar("aux_nacionalidades", "nome_nacionalidade", nome); }
    public List<String> listarNacionalidade() throws SQLException { return listarAuxiliar("aux_nacionalidades", "nome_nacionalidade"); }
    public boolean atualizarNacionalidade(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", id, novoNome); }
    public boolean excluirNacionalidade(int id) throws SQLException { return excluirAuxiliar("aux_nacionalidades", "id_nacionalidade", id); }
    public Integer buscarIdNacionalidadePorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", nome); }

    // --- TipoPassagem ---
    public boolean inserirTipoPassagem(String nome) throws SQLException { return inserirAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", nome); }
    public List<String> listarPassagemAux() throws SQLException { return listarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem"); }
    public boolean atualizarTipoPassagem(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", id, novoNome); }
    public boolean excluirTipoPassagem(int id) throws SQLException { return excluirAuxiliar("aux_tipos_passagem", "id_tipo_passagem", id); }
    public Integer buscarIdTipoPassagemPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", nome); }

    // --- AgenteAux ---
    public boolean inserirAgenteAux(String nome) throws SQLException { return inserirAuxiliar("aux_agentes", "nome_agente", nome); }
    public List<String> listarAgenteAux() throws SQLException { return listarAuxiliar("aux_agentes", "nome_agente"); }
    public boolean atualizarAgenteAux(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_agentes", "nome_agente", "id_agente", id, novoNome); }
    public boolean excluirAgenteAux(int id) throws SQLException { return excluirAuxiliar("aux_agentes", "id_agente", id); }
    public Integer buscarIdAgenteAuxPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_agentes", "nome_agente", "id_agente", nome); }

    // --- HorarioSaida ---
    public boolean inserirHorarioSaida(String descricao) throws SQLException { return inserirAuxiliar("aux_horarios_saida", "descricao_horario_saida", descricao); }
    public List<String> listarHorarioSaida() throws SQLException { return listarAuxiliar("aux_horarios_saida", "descricao_horario_saida"); }
    public boolean atualizarHorarioSaida(int id, String novaDescricao) throws SQLException { return atualizarAuxiliar("aux_horarios_saida", "descricao_horario_saida", "id_horario_saida", id, novaDescricao); }
    public boolean excluirHorarioSaida(int id) throws SQLException { return excluirAuxiliar("aux_horarios_saida", "id_horario_saida", id); }
    public Integer obterIdHorarioSaidaPorNome(String descricao) throws SQLException { return obterIdAuxiliar("aux_horarios_saida", "descricao_horario_saida", "id_horario_saida", descricao); }

    // --- Acomodacao ---
    public boolean inserirAcomodacao(String nome) throws SQLException { return inserirAuxiliar("aux_acomodacoes", "nome_acomodacao", nome); }
    public List<String> listarAcomodacao() throws SQLException { return listarAuxiliar("aux_acomodacoes", "nome_acomodacao"); }
    public boolean atualizarAcomodacao(int id, String novoNome) throws SQLException { return atualizarAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", id, novoNome); }
    public boolean excluirAcomodacao(int id) throws SQLException { return excluirAuxiliar("aux_acomodacoes", "id_acomodacao", id); }
    public Integer buscarIdAcomodacaoPorNome(String nome) throws SQLException { return obterIdAuxiliar("aux_acomodacoes", "nome_acomodacao", "id_acomodacao", nome); }

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
