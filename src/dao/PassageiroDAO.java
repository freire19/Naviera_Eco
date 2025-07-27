package dao;

import model.Passageiro;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PassageiroDAO {

    public PassageiroDAO() {
        // Construtor vazio
    }

    /**
     * Lista todos os nomes de passageiros para preencher o ComboBox.
     * @return Lista de nomes de passageiros.
     */
    public List<String> listarTodosNomesPassageiros() {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT nome_passageiro FROM passageiros ORDER BY nome_passageiro";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                nomes.add(rs.getString("nome_passageiro"));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar nomes de passageiros: " + e.getMessage());
            e.printStackTrace();
        }
        return nomes;
    }

    /**
     * Busca um passageiro pelo nome.
     * @param nome O nome completo do passageiro.
     * @return O objeto Passageiro encontrado, ou null se não encontrar.
     */
    public Passageiro buscarPorNome(String nome) {
        Passageiro passageiro = null;
        String sql = "SELECT id_passageiro, nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade FROM passageiros WHERE nome_passageiro ILIKE ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    passageiro = new Passageiro();
                    passageiro.setId(rs.getLong("id_passageiro"));
                    passageiro.setNome(rs.getString("nome_passageiro"));
                    passageiro.setNumeroDoc(rs.getString("numero_documento"));
                    passageiro.setDataNascimento(rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);

                    // Reutiliza a conexão para buscar nomes auxiliares
                    passageiro.setTipoDoc(buscarNomeAuxiliar(conn, "aux_tipos_documento", "nome_tipo_doc", rs.getInt("id_tipo_doc")));
                    passageiro.setSexo(buscarNomeAuxiliar(conn, "aux_sexo", "nome_sexo", rs.getInt("id_sexo")));
                    passageiro.setNacionalidade(buscarNomeAuxiliar(conn, "aux_nacionalidades", "nome_nacionalidade", rs.getInt("id_nacionalidade")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar passageiro por nome: " + e.getMessage());
            e.printStackTrace();
        }
        return passageiro;
    }

    /**
     * Busca um passageiro pelo número do documento.
     * @param doc O número do documento do passageiro.
     * @return O objeto Passageiro encontrado, ou null se não encontrar.
     */
    public Passageiro buscarPorDoc(String doc) {
        Passageiro passageiro = null;
        String sql = "SELECT id_passageiro, nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade FROM passageiros WHERE numero_documento = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doc);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    passageiro = new Passageiro();
                    passageiro.setId(rs.getLong("id_passageiro"));
                    passageiro.setNome(rs.getString("nome_passageiro"));
                    passageiro.setNumeroDoc(rs.getString("numero_documento"));
                    passageiro.setDataNascimento(rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);

                    // Reutiliza a conexão para buscar nomes auxiliares
                    passageiro.setTipoDoc(buscarNomeAuxiliar(conn, "aux_tipos_documento", "nome_tipo_doc", rs.getInt("id_tipo_doc")));
                    passageiro.setSexo(buscarNomeAuxiliar(conn, "aux_sexo", "nome_sexo", rs.getInt("id_sexo")));
                    passageiro.setNacionalidade(buscarNomeAuxiliar(conn, "aux_nacionalidades", "nome_nacionalidade", rs.getInt("id_nacionalidade")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar passageiro por documento: " + e.getMessage());
            e.printStackTrace();
        }
        return passageiro;
    }

    /**
     * Busca um passageiro pelo ID.
     * @param id O ID do passageiro.
     * @return O objeto Passageiro encontrado, ou null se não encontrar.
     */
    public Passageiro buscarPorId(long id) {
        Passageiro passageiro = null;
        String sql = "SELECT id_passageiro, nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade FROM passageiros WHERE id_passageiro = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    passageiro = new Passageiro();
                    passageiro.setId(rs.getLong("id_passageiro"));
                    passageiro.setNome(rs.getString("nome_passageiro"));
                    passageiro.setNumeroDoc(rs.getString("numero_documento"));
                    passageiro.setDataNascimento(rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);

                    // Reutiliza a conexão para buscar nomes auxiliares
                    passageiro.setTipoDoc(buscarNomeAuxiliar(conn, "aux_tipos_documento", "nome_tipo_doc", rs.getInt("id_tipo_doc")));
                    passageiro.setSexo(buscarNomeAuxiliar(conn, "aux_sexo", "nome_sexo", rs.getInt("id_sexo")));
                    passageiro.setNacionalidade(buscarNomeAuxiliar(conn, "aux_nacionalidades", "nome_nacionalidade", rs.getInt("id_nacionalidade")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar passageiro por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return passageiro;
    }

    /**
     * Insere um novo passageiro no banco de dados.
     * @param passageiro O objeto Passageiro a ser inserido.
     * @return true se a inserção for bem-sucedida, false caso contrário.
     */
    public boolean inserir(Passageiro passageiro) {
        String sql = "INSERT INTO passageiros (nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, passageiro.getNome());
            stmt.setString(2, passageiro.getNumeroDoc());
            stmt.setObject(3, obterIdAuxiliar(conn, "aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", passageiro.getTipoDoc()));
            stmt.setObject(4, passageiro.getDataNascimento() != null ? java.sql.Date.valueOf(passageiro.getDataNascimento()) : null);
            stmt.setObject(5, obterIdAuxiliar(conn, "aux_sexo", "nome_sexo", "id_sexo", passageiro.getSexo()));
            stmt.setObject(6, obterIdAuxiliar(conn, "aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", passageiro.getNacionalidade()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        passageiro.setId(rs.getLong(1)); // Define o ID gerado no objeto Passageiro
                        System.out.println("DEBUG PassageiroDAO: ID gerado para novo passageiro: " + passageiro.getId());
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) { // 23505 é para violação de PK/UK
                System.err.println("Erro: Passageiro com este documento já existe (violação de chave única).");
            } else {
                System.err.println("Erro ao inserir passageiro: " + e.getMessage());
            }
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Atualiza um passageiro existente no banco de dados.
     * @param passageiro O objeto Passageiro a ser atualizado.
     * @return true se a atualização for bem-sucedida, false caso contrário.
     */
    public boolean atualizar(Passageiro passageiro) {
        String sql = "UPDATE passageiros SET nome_passageiro = ?, numero_documento = ?, id_tipo_doc = ?, data_nascimento = ?, id_sexo = ?, id_nacionalidade = ?, data_ultima_atualizacao = CURRENT_TIMESTAMP WHERE id_passageiro = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passageiro.getNome());
            stmt.setString(2, passageiro.getNumeroDoc());
            stmt.setObject(3, obterIdAuxiliar(conn, "aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", passageiro.getTipoDoc()));
            stmt.setObject(4, passageiro.getDataNascimento() != null ? java.sql.Date.valueOf(passageiro.getDataNascimento()) : null);
            stmt.setObject(5, obterIdAuxiliar(conn, "aux_sexo", "nome_sexo", "id_sexo", passageiro.getSexo()));
            stmt.setObject(6, obterIdAuxiliar(conn, "aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", passageiro.getNacionalidade()));
            stmt.setLong(7, passageiro.getId()); // WHERE clause

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                System.err.println("Erro: Atualização falhou. Passageiro com este documento já existe (violação de chave única).");
            } else {
                System.err.println("Erro ao atualizar passageiro: " + e.getMessage());
            }
            e.printStackTrace();
        }
        return false;
    }

    // Métodos auxiliares para converter nome para ID e ID para nome em tabelas auxiliares
    // Refatorado para usar try-with-resources para Connection.
    private String buscarNomeAuxiliar(Connection conn, String tabela, String colunaNome, int id) throws SQLException {
        if (id == 0) {
            // System.out.println("DEBUG PassageiroDAO.buscarNomeAuxiliar: ID é 0 para tabela " + tabela + ", retornando null.");
            return null; // ID 0 geralmente significa "não definido" ou nulo em muitos bancos.
        }
        // Nota: Assumimos que a coluna de ID é nomeada como id_colunaNome
        String idColuna = colunaNome.replace("nome_", "id_");
        String sql = "SELECT " + colunaNome + " FROM " + tabela + " WHERE " + idColuna + " = ?";
        // System.out.println("DEBUG PassageiroDAO.buscarNomeAuxiliar: Buscando nome para tabela: " + tabela + ", ID: " + id + ", SQL: " + sql);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString(colunaNome);
                    // System.out.println("DEBUG PassageiroDAO.buscarNomeAuxiliar: Encontrado: " + nome);
                    return nome;
                }
            }
        }
        // System.out.println("DEBUG PassageiroDAO.buscarNomeAuxiliar: Nenhum nome encontrado para tabela: " + tabela + ", ID: " + id);
        return null;
    }

    // Refatorado para usar try-with-resources para Connection.
    private Integer obterIdAuxiliar(Connection conn, String tabela, String colunaNome, String colunaId, String valorNome) throws SQLException {
        if (valorNome == null || valorNome.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " ILIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valorNome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(colunaId);
                }
            }
        }
        return null;
    }
}