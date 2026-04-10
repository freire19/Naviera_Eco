package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Caixa;
import gui.util.AppLogger;

public class CaixaDAO {

    public List<Caixa> listarTodos() {
        List<Caixa> lista = new ArrayList<>();
        String sql = "SELECT * FROM caixas WHERE empresa_id = ? ORDER BY id_caixa";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Caixa c = new Caixa();
                    c.setId(rs.getInt("id_caixa"));
                    c.setNome(rs.getString("nome_caixa"));
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("CaixaDAO", "Erro SQL em CaixaDAO: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean inserir(Caixa caixa) {
        String sql = "INSERT INTO caixas (nome_caixa, empresa_id) VALUES (?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, caixa.getNome());
            stmt.setInt(2, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("CaixaDAO", "Erro SQL em CaixaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Caixa caixa) {
        String sql = "UPDATE caixas SET nome_caixa = ? WHERE id_caixa = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, caixa.getNome());
            stmt.setInt(2, caixa.getId());
            stmt.setInt(3, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("CaixaDAO", "Erro SQL em CaixaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(int id) {
        String sql = "DELETE FROM caixas WHERE id_caixa = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("CaixaDAO", "Erro SQL em CaixaDAO: " + e.getMessage());
            return false;
        }
    }
}
