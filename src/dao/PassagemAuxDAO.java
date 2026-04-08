package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PassagemAuxDAO {

    // Se quiser inserir "Criança 0-4" etc.
    public boolean inserir(String nome) {
        String sql = "INSERT INTO passagem_aux (nome) VALUES (?)";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.executeUpdate();
            return true;
        } catch(SQLException e){
            System.err.println("Erro SQL em PassagemAuxDAO: " + e.getMessage());
            return false;
        }
    }

    // Listar todos os nomes
    public List<String> listarNomes() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nome FROM passagem_aux ORDER BY id";
        try(Connection conn = ConexaoBD.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql)) {

            while(rs.next()) {
                lista.add(rs.getString("nome"));
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em PassagemAuxDAO: " + e.getMessage());
        }
        return lista;
    }

    // Buscar ID dado o nome
    public int buscarIdPorNome(String nome) {
        String sql = "SELECT id FROM passagem_aux WHERE nome=? LIMIT 1";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getInt("id");
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em PassagemAuxDAO: " + e.getMessage());
        }
        return 0;
    }

    // Buscar nome dado o ID
    public String buscarNomePorId(int id) {
        String sql = "SELECT nome FROM passagem_aux WHERE id=?";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getString("nome");
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em PassagemAuxDAO: " + e.getMessage());
        }
        return null;
    }
}
