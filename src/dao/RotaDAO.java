package dao;

import model.Rota;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RotaDAO {

    public List<Rota> listarTodasAsRotasComoObjects() {
        List<Rota> rotas = new ArrayList<>();
        String sql = "SELECT id, origem, destino FROM rotas WHERE empresa_id = ? ORDER BY origem, destino";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Rota rota = new Rota();
                    rota.setId((Long) rs.getObject("id"));
                    rota.setOrigem(rs.getString("origem"));
                    rota.setDestino(rs.getString("destino"));
                    rotas.add(rota);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return rotas;
    }

    public Rota buscarPorId(long idRota) {
        String sql = "SELECT id, origem, destino FROM rotas WHERE id = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idRota);
            stmt.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Rota rota = new Rota();
                    rota.setId((Long) rs.getObject("id"));
                    rota.setOrigem(rs.getString("origem"));
                    rota.setDestino(rs.getString("destino"));
                    return rota;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return null;
    }

    public long gerarProximoIdRota() {
        String sql = "SELECT nextval('seq_rota')";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return -1;
    }

    public boolean inserir(Rota rota) {
        String sql = "INSERT INTO rotas (id, origem, destino, empresa_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, rota.getId());
            stmt.setString(2, rota.getOrigem());
            stmt.setString(3, rota.getDestino());
            stmt.setInt(4, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Rota rota) {
        String sql = "UPDATE rotas SET origem = ?, destino = ? WHERE id = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rota.getOrigem());
            stmt.setString(2, rota.getDestino());
            stmt.setObject(3, rota.getId());
            stmt.setInt(4, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
            return false;
        }
    }

    /**
     * DL006: FK constraint do banco protege contra exclusao com viagens vinculadas.
     */
    public boolean excluir(long id) {
        String sql = "DELETE FROM rotas WHERE id = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setInt(2, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                System.err.println("Rota id=" + id + " nao pode ser excluida: possui viagens vinculadas.");
                return false;
            }
            System.err.println("Erro ao excluir rota: " + e.getMessage());
            return false;
        }
    }
}
