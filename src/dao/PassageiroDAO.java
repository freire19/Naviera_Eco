package dao;

// Multi-tenant imports added automatically

import model.Passageiro;
import java.sql.Connection;
// tenant filter
import static dao.DAOUtils.empresaId;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import dao.AuxiliaresDAO;

public class PassageiroDAO {

    private final AuxiliaresDAO auxDAO = new AuxiliaresDAO();

    public PassageiroDAO() {}

    /**
     * Busca todos os passageiros cadastrados no banco.
     * @return Uma lista de objetos Passageiro.
     */
    public List<Passageiro> listarTodos() {
        List<Passageiro> passageiros = new ArrayList<>();
        // O mapResultSetToPassageiro agora não precisa mais da conexão como parâmetro
        String sql = "SELECT * FROM passageiros WHERE empresa_id = ? ORDER BY nome_passageiro";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                passageiros.add(mapResultSetToPassageiro(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em PassageiroDAO: " + e.getMessage());
        }
        return passageiros;
    }
    
    public Passageiro salvarOuAtualizar(Passageiro passageiro) {
        if (passageiro == null) {
            return null;
        }
        if (passageiro.getId() == null || passageiro.getId() == 0L) {
            return inserir(passageiro);
        } else {
            boolean sucesso = atualizar(passageiro);
            return sucesso ? passageiro : null;
        }
    }

    public Passageiro inserir(Passageiro passageiro) {
        String sql = "INSERT INTO passageiros (nome_passageiro, numero_documento, id_tipo_doc, data_nascimento, id_sexo, id_nacionalidade, empresa_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Usa campo auxDAO (instanciado uma unica vez)

            stmt.setString(1, passageiro.getNome());
            stmt.setString(2, passageiro.getNumeroDoc());
            // CORREÇÃO: Removido o parâmetro "conn" da chamada
            stmt.setObject(3, auxDAO.obterIdAuxiliar("aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", passageiro.getTipoDoc()));
            stmt.setObject(4, passageiro.getDataNascimento() != null ? java.sql.Date.valueOf(passageiro.getDataNascimento()) : null);
            stmt.setObject(5, auxDAO.obterIdAuxiliar("aux_sexo", "nome_sexo", "id_sexo", passageiro.getSexo()));
            stmt.setObject(6, auxDAO.obterIdAuxiliar("aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", passageiro.getNacionalidade()));
            stmt.setInt(7, empresaId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        passageiro.setId(rs.getLong(1));
                        return passageiro;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir passageiro: " + e.getMessage());
        }
        return null;
    }

    public boolean atualizar(Passageiro passageiro) {
        String sql = "UPDATE passageiros SET nome_passageiro = ?, numero_documento = ?, id_tipo_doc = ?, data_nascimento = ?, id_sexo = ?, id_nacionalidade = ?, data_ultima_atualizacao = CURRENT_TIMESTAMP WHERE id_passageiro = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Usa campo auxDAO (instanciado uma unica vez)

            stmt.setString(1, passageiro.getNome());
            stmt.setString(2, passageiro.getNumeroDoc());
            // CORREÇÃO: Removido o parâmetro "conn" da chamada
            stmt.setObject(3, auxDAO.obterIdAuxiliar("aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", passageiro.getTipoDoc()));
            stmt.setObject(4, passageiro.getDataNascimento() != null ? java.sql.Date.valueOf(passageiro.getDataNascimento()) : null);
            stmt.setObject(5, auxDAO.obterIdAuxiliar("aux_sexo", "nome_sexo", "id_sexo", passageiro.getSexo()));
            stmt.setObject(6, auxDAO.obterIdAuxiliar("aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", passageiro.getNacionalidade()));
            stmt.setLong(7, passageiro.getId());
            stmt.setInt(8, empresaId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar passageiro: " + e.getMessage());
        }
        return false;
    }
    
    public List<String> listarTodosNomesPassageiros() {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT nome_passageiro FROM passageiros WHERE empresa_id = ? ORDER BY nome_passageiro";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                nomes.add(rs.getString("nome_passageiro"));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em PassageiroDAO: " + e.getMessage());
        }
        return nomes;
    }
    
    public Passageiro buscarPorNome(String nome) {
        String sql = "SELECT * FROM passageiros WHERE empresa_id = ? AND nome_passageiro ILIKE ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPassageiro(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em PassageiroDAO: " + e.getMessage());
        }
        return null;
    }

    public Passageiro buscarPorDoc(String doc) {
        String sql = "SELECT * FROM passageiros WHERE empresa_id = ? AND numero_documento = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doc);
            // empresa_id param shifted - see below
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPassageiro(rs);
                }
            }
        } catch (SQLException e) {
                System.err.println("Erro SQL em PassageiroDAO: " + e.getMessage());
        }
        return null;
    }
    
    public Passageiro buscarPorId(long id) {
        String sql = "SELECT * FROM passageiros WHERE id_passageiro = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPassageiro(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em PassageiroDAO: " + e.getMessage());
        }
        return null;
    }
    
    // CORREÇÃO: Removido o parâmetro "conn", pois não é mais necessário
    private Passageiro mapResultSetToPassageiro(ResultSet rs) throws SQLException {
        Passageiro passageiro = new Passageiro();
        // Usa campo auxDAO (instanciado uma unica vez)

        passageiro.setId(rs.getLong("id_passageiro"));
        passageiro.setNome(rs.getString("nome_passageiro"));
        passageiro.setNumeroDoc(rs.getString("numero_documento"));
        passageiro.setDataNascimento(rs.getDate("data_nascimento") != null ? rs.getDate("data_nascimento").toLocalDate() : null);
        
        // CORREÇÃO: Removido o parâmetro "conn" da chamada
        passageiro.setTipoDoc(auxDAO.buscarNomeAuxiliarPorId("aux_tipos_documento", "nome_tipo_doc", "id_tipo_doc", rs.getInt("id_tipo_doc")));
        passageiro.setSexo(auxDAO.buscarNomeAuxiliarPorId("aux_sexo", "nome_sexo", "id_sexo", rs.getInt("id_sexo")));
        passageiro.setNacionalidade(auxDAO.buscarNomeAuxiliarPorId("aux_nacionalidades", "nome_nacionalidade", "id_nacionalidade", rs.getInt("id_nacionalidade")));
        
        return passageiro;
    }
}