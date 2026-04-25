package dao;

import model.Viagem;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import util.AppLogger;

public class ViagemDAO {

    private final AuxiliaresDAO auxiliaresDAO;

    public ViagemDAO() {
        this.auxiliaresDAO = new AuxiliaresDAO();
    }

    // --- NOVO MÉTODO PARA O CALENDÁRIO ---
    public List<Viagem> listarViagensPorMesAno(int mes, int ano) {
        List<Viagem> viagens = new ArrayList<>();
        // SQL para Postgres: Extrai mês e ano da data
        String sql = "SELECT v.*, e.nome as nome_embarcacao, r.origem as nome_rota_origem, r.destino as nome_rota_destino, ahs.descricao_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.empresa_id = ? AND v.data_viagem >= ? AND v.data_viagem < ?";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            java.time.LocalDate inicio = java.time.LocalDate.of(ano, mes, 1);
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, java.sql.Date.valueOf(inicio));
            stmt.setDate(3, java.sql.Date.valueOf(inicio.plusMonths(1)));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    viagens.add(mapResultSetToViagem(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return viagens;
    }
    // -------------------------------------

    /**
     * Lista as N viagens mais recentes (por id_viagem DESC) com id, descricao,
     * data_viagem, data_chegada e is_atual.
     * Substitui a query inline duplicada em 6+ controllers financeiros.
     *
     * @param limit número máximo de viagens a retornar
     * @return lista de Viagem com campos básicos preenchidos
     */
    public List<Viagem> listarViagensRecentes(int limit) {
        List<Viagem> viagens = new ArrayList<>();
        String sql = "SELECT id_viagem, descricao, data_viagem, data_chegada, is_atual " +
                     "FROM viagens WHERE empresa_id = ? ORDER BY id_viagem DESC LIMIT ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Viagem v = new Viagem();
                    v.setId(rs.getLong("id_viagem"));
                    v.setDescricao(rs.getString("descricao"));
                    java.sql.Date dtViagem = rs.getDate("data_viagem");
                    if (dtViagem != null) v.setDataViagem(dtViagem.toLocalDate());
                    java.sql.Date dtChegada = rs.getDate("data_chegada");
                    if (dtChegada != null) v.setDataChegada(dtChegada.toLocalDate());
                    try { v.setIsAtual(rs.getBoolean("is_atual")); } catch (Exception e) { /* opcional */ }
                    viagens.add(v);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO.listarViagensRecentes: " + e.getMessage());
        }
        return viagens;
    }

    public List<String> listarViagensParaComboBox() {
        List<String> listaFormatada = new ArrayList<>();
        String sql = "SELECT v.id_viagem, v.data_viagem, ahs.descricao_horario_saida, r.origem, r.destino, e.nome as nome_embarcacao " +
                     "FROM viagens v " +
                     "JOIN rotas r ON v.id_rota = r.id " +
                     "JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.empresa_id = ? " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC, v.id_viagem DESC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // DR111: null check para evitar NPE se data_viagem for NULL
                    java.sql.Date dtViagem = rs.getDate("data_viagem");
                    LocalDate dataViagem = dtViagem != null ? dtViagem.toLocalDate() : LocalDate.now();
                    String origem = rs.getString("origem");
                    String destino = rs.getString("destino");
                    String nomeEmbarcacao = rs.getString("nome_embarcacao");
                    String horarioDescricao = rs.getString("descricao_horario_saida");
                    Long id = rs.getLong("id_viagem"); // Pega o ID

                    String nomeRotaConcatenado = origem + (destino != null && !destino.isEmpty() ? " - " + destino : "");

                    // Formato Padronizado: ID - DATA ...
                    listaFormatada.add(String.format("%d - %s - %s - %s - %s",
                                                     id,
                                                     dataViagem.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                                     (horarioDescricao != null ? horarioDescricao : "N/A"),
                                                     nomeRotaConcatenado,
                                                     nomeEmbarcacao));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return listaFormatada;
    }

    public Viagem buscarPorId(long id) {
        return buscarViagemPorId(id);
    }

    public Viagem buscarViagemPorId(Long id) {
        String sql = "SELECT v.*, e.nome as nome_embarcacao, r.origem as nome_rota_origem, r.destino as nome_rota_destino, ahs.descricao_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.id_viagem = ? AND v.empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToViagem(rs);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return null;
    }

    // DP013: cache da viagem ativa por empresa_id (muda raramente, evita 3-5 queries redundantes/ciclo)
    // DB101: ConcurrentHashMap keyed by empresa_id para isolar cache entre tenants
    // #210: TTL 60s — evita entrada cache stale se outra JVM mudou `is_atual`.
    private static final long CACHE_TTL_MS = 60_000L;
    private static final ConcurrentHashMap<Integer, Viagem> cacheViagemAtiva = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Long> cacheViagemAtivaExpira = new ConcurrentHashMap<>();

    public static void invalidarCacheViagem() { cacheViagemAtiva.clear(); cacheViagemAtivaExpira.clear(); }

    public static void invalidarCacheViagem(int empresaId) {
        cacheViagemAtiva.remove(empresaId);
        cacheViagemAtivaExpira.remove(empresaId);
    }

    public Viagem buscarViagemAtiva() {
        int empresaId = DAOUtils.empresaId();
        Long exp = cacheViagemAtivaExpira.get(empresaId);
        if (exp != null && System.currentTimeMillis() < exp) {
            Viagem cached = cacheViagemAtiva.get(empresaId);
            if (cached != null) return cached;
        } else if (exp != null) {
            // expirado — limpa
            invalidarCacheViagem(empresaId);
        }
        String sql = "SELECT v.*, e.nome as nome_embarcacao, r.origem as nome_rota_origem, r.destino as nome_rota_destino, ahs.descricao_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.is_atual = TRUE AND v.empresa_id = ? " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC LIMIT 1";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, empresaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Viagem v = mapResultSetToViagem(rs);
                    cacheViagemAtiva.put(empresaId, v);
                    cacheViagemAtivaExpira.put(empresaId, System.currentTimeMillis() + CACHE_TTL_MS);
                    return v;
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return null;
    }

    /** Delega para buscarViagemAtiva() — mantido para compatibilidade de chamadores existentes. */
    public Viagem buscarViagemMarcadaComoAtual() {
        return buscarViagemAtiva();
    }

    private Viagem mapResultSetToViagem(ResultSet rs) throws SQLException {
        Viagem viagem = new Viagem();
        viagem.setId(rs.getLong("id_viagem"));
        viagem.setDataViagem(rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toLocalDate() : null);
        viagem.setIdHorarioSaida(rs.getObject("id_horario_saida") != null ? rs.getLong("id_horario_saida") : null);
        viagem.setDescricaoHorarioSaida(rs.getString("descricao_horario_saida"));
        if (rs.getDate("data_chegada") != null) {
            viagem.setDataChegada(rs.getDate("data_chegada").toLocalDate());
        } else {
            viagem.setDataChegada(null);
        }
        viagem.setDescricao(rs.getString("descricao"));
        
        // Mapeia colunas booleanas com segurança
        // DR226: logging em vez de silenciar (colunas existem no schema atual)
        try { viagem.setAtiva(rs.getBoolean("ativa")); } catch(SQLException e) { AppLogger.warn("ViagemDAO", "Coluna ativa: " + e.getMessage()); }
        try { viagem.setIsAtual(rs.getBoolean("is_atual")); } catch(SQLException e) { AppLogger.warn("ViagemDAO", "Coluna is_atual: " + e.getMessage()); }
        
        viagem.setIdEmbarcacao(rs.getLong("id_embarcacao"));
        viagem.setNomeEmbarcacao(rs.getString("nome_embarcacao"));
        viagem.setIdRota(rs.getLong("id_rota"));

        String nomeRotaOrigem = rs.getString("nome_rota_origem");
        String nomeRotaDestino = rs.getString("nome_rota_destino");
        
        viagem.setOrigem(nomeRotaOrigem);
        viagem.setDestino(nomeRotaDestino);
        
        String origemSafe = nomeRotaOrigem != null ? nomeRotaOrigem : "";
        String destinoSafe = nomeRotaDestino != null ? nomeRotaDestino : "";
        viagem.setNomeRotaConcatenado(origemSafe + (!destinoSafe.isEmpty() ? " - " + destinoSafe : ""));
        return viagem;
    }

    public Long obterIdViagemPelaString(String strViagem) throws SQLException {
        if (strViagem == null || strViagem.trim().isEmpty()) return null;
        
        // Ajuste para pegar o ID se a string começar com "ID - "
        if (strViagem.matches("^\\d+ - .*")) {
            try {
                String idPart = strViagem.split(" - ")[0];
                return Long.parseLong(idPart);
            } catch (Exception e) {
                // Se falhar, continua para a lógica antiga
            }
        }
        
        List<String> parts = Arrays.stream(strViagem.split(" - ")).map(String::trim).collect(Collectors.toList());
        if (parts.size() < 4) return null;

        String dataViagemStr = parts.get(0);
        String nomeEmbarcacao = parts.get(parts.size() - 1);
        
        List<String> rotaPartsList = parts.subList(2, parts.size() - 1);
        String rotaCompleta = String.join(" - ", rotaPartsList);

        Integer idRotaInt = auxiliaresDAO.obterIdRotaPelaOrigemDestino(rotaCompleta, null);
        
        if(idRotaInt == null && rotaCompleta.contains(" - ")) {
            int ultimoHifen = rotaCompleta.lastIndexOf(" - ");
            idRotaInt = auxiliaresDAO.obterIdRotaPelaOrigemDestino(rotaCompleta.substring(0, ultimoHifen), rotaCompleta.substring(ultimoHifen + 3));
        }
        
        LocalDate dataViagem;
        try {
            dataViagem = LocalDate.parse(dataViagemStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) { AppLogger.warn("ViagemDAO", "ViagemDAO: formato de data invalido: " + dataViagemStr); return null; }

        Integer idEmbarcacaoInt = auxiliaresDAO.obterIdAuxiliar("embarcacoes", "nome", "id_embarcacao", nomeEmbarcacao);

        if (idRotaInt == null || idEmbarcacaoInt == null) return null;

        String sql = "SELECT id_viagem FROM viagens WHERE data_viagem = ? AND id_rota = ? AND id_embarcacao = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(dataViagem));
            stmt.setInt(2, idRotaInt);
            stmt.setInt(3, idEmbarcacaoInt);
            stmt.setInt(4, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id_viagem");
            }
        }
        return null;
    }

    public List<Viagem> listarTodasViagensResumido() {
        List<Viagem> viagens = new ArrayList<>();
        String sql = "SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa, v.is_atual, " +
                     "v.id_embarcacao, e.nome AS nome_embarcacao, " +
                     "v.id_rota, r.origem AS nome_rota_origem, r.destino AS nome_rota_destino, " +
                     "v.id_horario_saida, ahs.descricao_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.empresa_id = ? " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC " +
                     "LIMIT 200";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                viagens.add(mapResultSetToViagem(rs));
            }
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return viagens;
    }

    public List<Integer> listarIdsHorariosSaida() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id_horario_saida FROM aux_horarios_saida ORDER BY descricao_horario_saida";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id_horario_saida"));
            }
        } catch (SQLException e) { AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage()); }
        return ids;
    }

    public Long gerarProximoIdViagem() {
        String sql = "SELECT nextval('seq_viagem')";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
        }
        return null;
    }

    public boolean inserir(Viagem v) {
        // Validacao de data de partida removida do DAO — responsabilidade do controller.
        String sql = "INSERT INTO viagens (id_viagem, data_viagem, id_horario_saida, data_chegada, descricao, ativa, is_atual, id_embarcacao, id_rota, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, v.getId());
            stmt.setDate(2, Date.valueOf(v.getDataViagem()));
            stmt.setObject(3, v.getIdHorarioSaida());
            stmt.setDate(4, v.getDataChegada() != null ? Date.valueOf(v.getDataChegada()) : null);
            stmt.setString(5, v.getDescricao());
            stmt.setBoolean(6, v.isAtiva());
            stmt.setBoolean(7, v.getIsAtual());
            stmt.setLong(8, v.getIdEmbarcacao());
            stmt.setLong(9, v.getIdRota());
            stmt.setInt(10, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage()); return false; }
    }

    public boolean atualizar(Viagem v) {
        // #023: permite atualizar viagens passadas (corrigir descricao, embarcacao, rota)
        // DL024: impede apenas MUDAR data para o passado em viagens que ainda nao partiram
        String sql = "UPDATE viagens SET data_viagem = ?, id_horario_saida = ?, data_chegada = ?, descricao = ?, ativa = ?, is_atual = ?, id_embarcacao = ?, id_rota = ? WHERE id_viagem = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(v.getDataViagem()));
            stmt.setObject(2, v.getIdHorarioSaida());
            stmt.setDate(3, v.getDataChegada() != null ? Date.valueOf(v.getDataChegada()) : null);
            stmt.setString(4, v.getDescricao());
            stmt.setBoolean(5, v.isAtiva());
            stmt.setBoolean(6, v.getIsAtual());
            stmt.setLong(7, v.getIdEmbarcacao());
            stmt.setLong(8, v.getIdRota());
            stmt.setLong(9, v.getId());
            stmt.setInt(10, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage()); return false; }
    }

    public boolean excluir(long id) {
        // Exclui registros filhos antes da viagem porque o schema nao define ON DELETE CASCADE
        // nas FK que referenciam viagens. Enquanto o schema nao for alterado para adicionar
        // CASCADE, esta exclusao manual em transacao e necessaria para manter integridade.
        String[] sqlFilhos = {
            "DELETE FROM encomenda_itens WHERE id_encomenda IN (SELECT id_encomenda FROM encomendas WHERE id_viagem = ? AND empresa_id = ?)",
            "DELETE FROM passagens WHERE id_viagem = ? AND empresa_id = ?",
            "DELETE FROM encomendas WHERE id_viagem = ? AND empresa_id = ?",
            "DELETE FROM fretes WHERE id_viagem = ? AND empresa_id = ?",
            "DELETE FROM recibos_avulsos WHERE id_viagem = ? AND empresa_id = ?",
            "DELETE FROM financeiro_saidas WHERE id_viagem = ? AND empresa_id = ?"
        };
        String sqlViagem = "DELETE FROM viagens WHERE id_viagem = ? AND empresa_id = ?";

        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            for (String sql : sqlFilhos) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, id);
                    stmt.setInt(2, DAOUtils.empresaId());
                    stmt.executeUpdate();
                }
            }

            boolean resultado;
            try (PreparedStatement stmt = conn.prepareStatement(sqlViagem)) {
                stmt.setLong(1, id);
                stmt.setInt(2, DAOUtils.empresaId());
                resultado = stmt.executeUpdate() > 0;
            }

            conn.commit();
            return resultado;
        } catch (SQLException e) {
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { AppLogger.warn("ViagemDAO", "Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); } }
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { AppLogger.warn("ViagemDAO", "Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); } }
        }
    }

    // --- CORREÇÃO PRINCIPAL: DEFINIR VIAGEM ATIVA USANDO is_atual ---
    // #200: manter `ativa` e `is_atual` em sincronia (API e BFF ja fazem isso juntos)
    // #DR274: garantir atomicidade — se UPDATE de ativacao nao afeta nenhuma linha (alvo
    //   inexistente / cross-tenant), rollback do desativar para nao deixar empresa SEM
    //   viagem ativa. Tambem trava com pg_advisory_xact_lock para serializar concorrencia.
    public boolean definirViagemAtiva(long idViagemParaAtivar) {
        int empresaId = DAOUtils.empresaId();
        invalidarCacheViagem(empresaId);
        String sqlDesativar = "UPDATE viagens SET is_atual = false, ativa = false WHERE empresa_id = ?";
        String sqlAtivar = "UPDATE viagens SET is_atual = true, ativa = true WHERE id_viagem = ? AND empresa_id = ?";

        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement lock = conn.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)")) {
                lock.setInt(1, 730001); // namespace arbitrario para "definirViagemAtiva"
                lock.setInt(2, empresaId);
                lock.executeQuery().close();
            }

            try (PreparedStatement s1 = conn.prepareStatement(sqlDesativar)) {
                s1.setInt(1, empresaId);
                s1.executeUpdate();
            }

            int rowsAtivada;
            try (PreparedStatement s2 = conn.prepareStatement(sqlAtivar)) {
                s2.setLong(1, idViagemParaAtivar);
                s2.setInt(2, empresaId);
                rowsAtivada = s2.executeUpdate();
            }

            if (rowsAtivada == 0) {
                conn.rollback();
                AppLogger.warn("ViagemDAO",
                    "definirViagemAtiva abortado: id=" + idViagemParaAtivar + " nao existe na empresa " + empresaId);
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) { /* ignorado */ }
            AppLogger.warn("ViagemDAO", "Erro SQL em ViagemDAO: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ex) { /* ignorado */ }
                try { conn.close(); } catch (Exception ex) { /* ignorado */ }
            }
        }
    }
}