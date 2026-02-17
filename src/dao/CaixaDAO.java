package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Caixa;

public class CaixaDAO {

    // LISTAR (Lê os dados da tabela 'caixas')
    public List<Caixa> listarTodos() {
        List<Caixa> lista = new ArrayList<>();
        // Ajustado para sua tabela 'caixas'
        String sql = "SELECT * FROM caixas ORDER BY id_caixa"; 

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Caixa c = new Caixa();
                // Mapeando as colunas da sua foto: id_caixa e nome_caixa
                c.setId(rs.getInt("id_caixa")); 
                c.setNome(rs.getString("nome_caixa"));
                lista.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar caixas: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
    
    // INSERIR (Salva na tabela 'caixas')
    public boolean inserir(Caixa caixa) {
        String sql = "INSERT INTO caixas (nome_caixa) VALUES (?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, caixa.getNome());
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erro ao inserir caixa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ALTERAR (Atualiza usando 'id_caixa')
    public boolean alterar(Caixa caixa) {
        String sql = "UPDATE caixas SET nome_caixa = ? WHERE id_caixa = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, caixa.getNome());
            stmt.setInt(2, caixa.getId());
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erro ao alterar caixa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // EXCLUIR (Remove usando 'id_caixa')
    public boolean excluir(int id) {
        String sql = "DELETE FROM caixas WHERE id_caixa = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erro ao excluir caixa: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}