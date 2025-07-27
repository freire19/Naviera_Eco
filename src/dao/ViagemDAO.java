package dao;

import model.Viagem;
import model.Embarcacao;
import model.Rota;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Importar AuxiliaresDAO
import dao.AuxiliaresDAO;

public class ViagemDAO {

    private final AuxiliaresDAO auxiliaresDAO; // Adicionar instância de AuxiliaresDAO

    public ViagemDAO() {
        this.auxiliaresDAO = new AuxiliaresDAO(); // Inicializar
    }

    /**
     * Lista viagens em um formato de string para ComboBoxes.
     * Formato: "DD/MM/YYYY - HorárioCadastrado - Origem - Destino - Nome Embarcação"
     * Agora usa a descrição do horário auxiliar.
     * @return Uma lista de strings formatadas representando as viagens.
     */
    public List<String> listarViagensParaComboBox() {
        List<String> listaFormatada = new ArrayList<>();
        String sql = "SELECT v.id_viagem, v.data_viagem, ahs.descricao_horario_saida, r.origem, r.destino, e.nome as nome_embarcacao " +
                     "FROM viagens v " +
                     "JOIN rotas r ON v.id_rota = r.id " +
                     "JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC, v.id_viagem DESC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LocalDate dataViagem = rs.getDate("data_viagem").toLocalDate();
                String origem = rs.getString("origem");
                String destino = rs.getString("destino");
                String nomeEmbarcacao = rs.getString("nome_embarcacao");
                String horarioDescricao = rs.getString("descricao_horario_saida");

                String nomeRotaConcatenado = origem + " - " + destino;

                listaFormatada.add(String.format("%s - %s - %s - %s",
                                                 dataViagem.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                                 (horarioDescricao != null ? horarioDescricao : "N/A"),
                                                 nomeRotaConcatenado,
                                                 nomeEmbarcacao));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar viagens para ComboBox: " + e.getMessage());
            e.printStackTrace();
        }
        return listaFormatada;
    }

    /**
     * Busca uma viagem por ID, incluindo nomes de embarcação, rota e descrição do horário.
     * @param id O ID da viagem a ser buscada.
     * @return O objeto Viagem encontrado ou null se não for encontrada.
     */
    public Viagem buscarPorId(long id) {
        String sql = "SELECT v.id_viagem, v.data_viagem, ahs.descricao_horario_saida, v.data_chegada, v.descricao, v.ativa, " +
                     "v.id_embarcacao, e.nome as nome_embarcacao, " +
                     "v.id_rota, r.origem as nome_rota_origem, r.destino as nome_rota_destino, v.id_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.id_viagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToViagem(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar viagem por ID com nomes: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca uma viagem ativa no banco de dados.
     * @return O objeto Viagem ativa encontrado, ou null se nenhuma for encontrada.
     */
    public Viagem buscarViagemAtiva() {
        String sql = "SELECT v.id_viagem, v.data_viagem, ahs.descricao_horario_saida, v.data_chegada, v.descricao, v.ativa, " +
                     "v.id_embarcacao, e.nome as nome_embarcacao, " +
                     "v.id_rota, r.origem as nome_rota_origem, r.destino as nome_rota_destino, v.id_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "WHERE v.ativa = TRUE " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC LIMIT 1";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return mapResultSetToViagem(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar viagem ativa: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mapeia um ResultSet para um objeto Viagem.
     */
    private Viagem mapResultSetToViagem(ResultSet rs) throws SQLException {
        Viagem viagem = new Viagem();

        // Correção aqui: Conversão segura de Integer para Long
        Object idViagemObj = rs.getObject("id_viagem");
        viagem.setId(idViagemObj != null ? ((Number) idViagemObj).longValue() : null);

        viagem.setDataViagem(rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toLocalDate() : null);

        // Correção aqui: Conversão segura de Integer para Long
        Object idHorarioSaidaObj = rs.getObject("id_horario_saida");
        viagem.setIdHorarioSaida(idHorarioSaidaObj != null ? ((Number) idHorarioSaidaObj).longValue() : null);
        viagem.setDescricaoHorarioSaida(rs.getString("descricao_horario_saida"));

        if (rs.getDate("data_chegada") != null) {
            viagem.setDataChegada(rs.getDate("data_chegada").toLocalDate());
        } else {
            viagem.setDataChegada(null);
        }
        viagem.setDescricao(rs.getString("descricao"));
        viagem.setAtiva(rs.getBoolean("ativa"));

        // Correção aqui: Conversão segura de Integer para Long
        Object idEmbarcacaoObj = rs.getObject("id_embarcacao");
        viagem.setIdEmbarcacao(idEmbarcacaoObj != null ? ((Number) idEmbarcacaoObj).longValue() : null);
        viagem.setNomeEmbarcacao(rs.getString("nome_embarcacao"));

        // Correção aqui: Conversão segura de Integer para Long
        Object idRotaObj = rs.getObject("id_rota");
        viagem.setIdRota(idRotaObj != null ? ((Number) idRotaObj).longValue() : null);

        String nomeRotaOrigem = rs.getString("nome_rota_origem");
        String nomeRotaDestino = rs.getString("nome_rota_destino");
        if (nomeRotaOrigem != null && nomeRotaDestino != null) {
            viagem.setNomeRotaConcatenado(nomeRotaOrigem + " - " + nomeRotaDestino);
        } else if (nomeRotaOrigem != null) {
            viagem.setNomeRotaConcatenado(nomeRotaOrigem);
        } else if (nomeRotaDestino != null) {
            viagem.setNomeRotaConcatenado(nomeRotaDestino);
        } else {
            viagem.setNomeRotaConcatenado(null);
        }
        return viagem;
    }


    /**
     * Obtém o ID de uma viagem a partir de sua representação em string.
     * @param strViagem A string formatada da viagem.
     * @return O ID da viagem ou null se não for encontrada.
     * @throws SQLException Se ocorrer um erro de SQL.
     */
    public Long obterIdViagemPelaString(String strViagem) throws SQLException { // Removido 'Connection conn'
        System.out.println("Tentando obter ID para a string de viagem: '" + strViagem + "'");
        if (strViagem == null || strViagem.trim().isEmpty()) {
            System.out.println("String de viagem nula ou vazia.");
            return null;
        }

        String[] parts = strViagem.split(" - ");

        if (parts.length < 4) {
            System.out.println("Formato de string de viagem inválido (menos de 4 segmentos esperados): " + strViagem + " (Partes: " + parts.length + ")");
            for (int i = 0; i < parts.length; i++) {
                System.out.println("Part " + i + ": '" + parts[i] + "'");
            }
            return null;
        }

        String dataViagemStr = parts[0].trim();
        String horarioDescricaoStr = parts[1].trim();
        String rotaOuOrigemDestinoStr = parts[2].trim();
        String nomeEmbarcacao = parts[parts.length - 1].trim();

        String origemRota = "";
        String destinoRota = "";

        String[] rotaPartsInterna = rotaOuOrigemDestinoStr.split(" - ");
        if (rotaPartsInterna.length >= 2) {
            origemRota = rotaPartsInterna[0].trim();
            destinoRota = rotaPartsInterna[1].trim();
        } else {
            origemRota = rotaOuOrigemDestinoStr;
            destinoRota = "";
        }

        System.out.println("Debug - Data: " + dataViagemStr + ", Horário Desc: " + horarioDescricaoStr +
                           ", Origem Rota: " + origemRota + ", Destino Rota: " + destinoRota +
                           ", Nome Embarcação: " + nomeEmbarcacao);

        LocalDate dataViagem = null;
        try {
            dataViagem = LocalDate.parse(dataViagemStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            System.out.println("Debug - Data parseada: " + dataViagem);
        } catch (java.time.format.DateTimeParseException e) {
            System.err.println("Erro ao parsear data da string da viagem: " + e.getMessage());
            return null;
        }

        // Chamar AuxiliaresDAO
        Integer idHorarioSaidaInt = auxiliaresDAO.obterIdHorarioSaidaPorNome(horarioDescricaoStr); // Chamada corrigida
        Long idHorarioSaida = (idHorarioSaidaInt != null) ? idHorarioSaidaInt.longValue() : null;

        Integer idRotaInt = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origemRota, destinoRota); // Chamada corrigida
        Long idRota = (idRotaInt != null) ? idRotaInt.longValue() : null;

        Integer idEmbarcacaoInt = auxiliaresDAO.obterIdAuxiliar("embarcacoes", "nome", "id_embarcacao", nomeEmbarcacao); // Chamada corrigida
        Long idEmbarcacao = (idEmbarcacaoInt != null) ? idEmbarcacaoInt.longValue() : null;

        System.out.println("Debug - IDs buscados: Horario Saida=" + idHorarioSaida + ", Rota=" + idRota + ", Embarcação=" + idEmbarcacao);

        if (idRota == null || idEmbarcacao == null) {
            System.err.println("ID de rota ou embarcação não encontrado para a string de viagem. Verifique se os dados correspondem aos cadastros.");
            return null;
        }

        String sql = "SELECT id_viagem FROM viagens WHERE data_viagem = ? AND id_rota = ? AND id_embarcacao = ?";
        if (idHorarioSaida != null) {
            sql += " AND id_horario_saida = ?";
        } else {
            sql += " AND id_horario_saida IS NULL";
        }

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(dataViagem));
            stmt.setLong(2, idRota);
            stmt.setLong(3, idEmbarcacao);
            if (idHorarioSaida != null) {
                stmt.setLong(4, idHorarioSaida);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long foundId = rs.getLong("id_viagem");
                    System.out.println("Debug - ID da viagem encontrado no DB: " + foundId);
                    return foundId;
                }
            }
        }
        System.out.println("Debug - Nenhuma viagem encontrada com os critérios fornecidos no DB.");
        return null;
    }

    /**
     * Lista todas as viagens de forma resumida para exibição em tabelas.
     * Retorna objetos Viagem com campos essenciais preenchidos.
     * @return Uma lista de objetos Viagem.
     */
    public List<Viagem> listarTodasViagensResumido() {
        List<Viagem> viagens = new ArrayList<>();
        String sql = "SELECT v.id_viagem, v.data_viagem, v.data_chegada, v.descricao, v.ativa, " +
                     "v.id_embarcacao, e.nome AS nome_embarcacao, " +
                     "v.id_rota, r.origem AS nome_rota_origem, r.destino AS nome_rota_destino, " +
                     "v.id_horario_saida, ahs.descricao_horario_saida " +
                     "FROM viagens v " +
                     "LEFT JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " +
                     "LEFT JOIN aux_horarios_saida ahs ON v.id_horario_saida = ahs.id_horario_saida " +
                     "ORDER BY v.data_viagem DESC, ahs.descricao_horario_saida DESC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                viagens.add(mapResultSetToViagem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todas as viagens resumidas: " + e.getMessage());
            e.printStackTrace();
        }
        return viagens;
    }

    /**
     * Lista os IDs de todos os horários de saída cadastrados.
     * Usado para popular o ComboBox de horários.
     * @return Uma lista de Integer contendo os IDs dos horários. Pode ser vazia, mas não null.
     */
    public List<Integer> listarIdsHorariosSaida() { // Método mantido aqui, embora AuxiliaresDAO possa ter algo parecido
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id_horario_saida FROM aux_horarios_saida ORDER BY descricao_horario_saida";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id_horario_saida"));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar IDs de horários de saída: " + e.getMessage());
            e.printStackTrace();
        }
        return ids;
    }

    // Remover os métodos obterIdAuxiliar, obterIdRotaPelaOrigemDestino, buscarNomeAuxiliar (já estão em AuxiliaresDAO)
    /*
    public Integer obterIdAuxiliar(Connection conn, String tabela, String colunaNome, String colunaId, String valorNome) throws SQLException { ... }
    public Integer obterIdRotaPelaOrigemDestino(Connection conn, String origem, String destino) throws SQLException { ... }
    public String buscarNomeAuxiliar(Connection conn, String tabela, String colunaNome, Integer id) throws SQLException { ... }
    */

    public long gerarProximoIdViagem() {
        String sql = "SELECT nextval('seq_viagem')";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gerar próximo ID de viagem: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public boolean inserir(Viagem v) {
        String sql = "INSERT INTO viagens (id_viagem, data_viagem, id_horario_saida, data_chegada, descricao, ativa, id_embarcacao, id_rota) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, v.getId());
            stmt.setDate(2, Date.valueOf(v.getDataViagem()));
            stmt.setObject(3, v.getIdHorarioSaida());
            stmt.setDate(4, v.getDataChegada() != null ? Date.valueOf(v.getDataChegada()) : null);
            stmt.setString(5, v.getDescricao());
            stmt.setBoolean(6, v.isAtiva());
            stmt.setObject(7, v.getIdEmbarcacao());
            stmt.setObject(8, v.getIdRota());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir viagem: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean atualizar(Viagem v) {
        String sql = "UPDATE viagens SET data_viagem = ?, id_horario_saida = ?, data_chegada = ?, descricao = ?, ativa = ?, id_embarcacao = ?, id_rota = ? WHERE id_viagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(v.getDataViagem()));
            stmt.setObject(2, v.getIdHorarioSaida());
            stmt.setDate(3, v.getDataChegada() != null ? Date.valueOf(v.getDataChegada()) : null);
            stmt.setString(4, v.getDescricao());
            stmt.setBoolean(5, v.isAtiva());
            stmt.setObject(6, v.getIdEmbarcacao());
            stmt.setObject(7, v.getIdRota());
            stmt.setObject(8, v.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar viagem: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean excluir(long id) {
        String sql = "DELETE FROM viagens WHERE id_viagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir viagem: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean definirViagemAtiva(long idViagemParaAtivar) {
        String sqlDesativarTodas = "UPDATE viagens SET ativa = false";
        String sqlAtivarEspecifica = "UPDATE viagens SET ativa = true WHERE id_viagem = ?";
        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtDesativar = conn.prepareStatement(sqlDesativarTodas)) {
                stmtDesativar.executeUpdate();
            }

            try (PreparedStatement stmtAtivar = conn.prepareStatement(sqlAtivarEspecifica)) {
                stmtAtivar.setLong(1, idViagemParaAtivar);
                stmtAtivar.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao definir viagem ativa: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Erro ao fazer rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar conexão: " + e.getMessage());
                }
            }
        }
    }
}