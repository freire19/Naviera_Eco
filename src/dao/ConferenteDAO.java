package dao;

// Multi-tenant imports added automatically

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para a tabela conferentes.
 * Centraliza CRUD que estava inline em CadastroConferenteController
 * e carregarConferentesDoBanco() em CadastroFreteController.
 */
public class ConferenteDAO {

    /**
     * Lista todos os conferentes ordenados por nome.
     * Retorna pares [id, nome] como array de Object.
     */
    public List<long[]> listarTodos() {
        List<long[]> lista = new ArrayList<>();
        String sql = "SELECT id_conferente, nome_conferente FROM conferentes WHERE empresa_id = ? ORDER BY nome_conferente";
        try (Connection con = ConexaoBD.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new long[]{rs.getLong("id_conferente"), 0});
                // Recria como lista de objetos abaixo — usado via listarNomes()
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.listarTodos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista apenas os nomes dos conferentes, para preencher ComboBox.
     */
    public List<String> listarNomes() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome_conferente FROM conferentes WHERE empresa_id = ? ORDER BY nome_conferente";
        try (Connection con = ConexaoBD.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String nome = rs.getString(1);
                if (nome != null) lista.add(nome);
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.listarNomes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Resultado de listar com id e nome (para a tabela do cadastro).
     */
    public static class ConferenteRow {
        public final long id;
        public final String nome;
        public ConferenteRow(long id, String nome) {
            this.id = id;
            this.nome = nome;
        }
    }

    /** Lista todos os conferentes como ConferenteRow (id + nome). */
    public List<ConferenteRow> listarComId() {
        List<ConferenteRow> lista = new ArrayList<>();
        String sql = "SELECT id_conferente, nome_conferente FROM conferentes WHERE empresa_id = ? ORDER BY nome_conferente";
        try (Connection con = ConexaoBD.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new ConferenteRow(rs.getLong("id_conferente"), rs.getString("nome_conferente")));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.listarComId: " + e.getMessage());
        }
        return lista;
    }

    /** Gera próximo id via sequence. */
    public long gerarProximoId() throws SQLException {
        String sql = "SELECT nextval('seq_conferente') as prox_id";
        try (Connection con = ConexaoBD.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong("prox_id");
        }
        throw new SQLException("Não foi possível gerar ID para conferente.");
    }

    /** Verifica se um conferente com dado id já existe. */
    public boolean existe(long id) {
        String sql = "SELECT 1 FROM conferentes WHERE id_conferente = ? AND empresa_id = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.existe: " + e.getMessage());
            return false;
        }
    }

    /** Insere novo conferente. */
    public boolean inserir(long id, String nome) {
        String sql = "INSERT INTO conferentes (id_conferente, nome_conferente, empresa_id) VALUES(?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setString(2, nome);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.inserir: " + e.getMessage());
            return false;
        }
    }

    /** Atualiza o nome de um conferente. */
    public boolean atualizar(long id, String novoNome) {
        String sql = "UPDATE conferentes SET nome_conferente = ? WHERE id_conferente = ? AND empresa_id = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.atualizar: " + e.getMessage());
            return false;
        }
    }

    /** Exclui um conferente pelo id. */
    public boolean excluir(long id) {
        String sql = "DELETE FROM conferentes WHERE id_conferente = ? AND empresa_id = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em ConferenteDAO.excluir: " + e.getMessage());
            return false;
        }
    }
}
