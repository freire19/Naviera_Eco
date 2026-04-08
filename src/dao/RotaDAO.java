package dao;

import model.Rota; // Certifique-se que model.Rota.java existe e está correto
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RotaDAO {

    public List<Rota> listarTodasAsRotasComoObjects() { // Método renomeado para corresponder ao Controller
        List<Rota> rotas = new ArrayList<>();
        String sql = "SELECT id, origem, destino FROM rotas ORDER BY origem, destino";

        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Rota rota = new Rota();
                // Usar getObject e cast para Long para IDs que podem ser null no DB
                rota.setId((Long) rs.getObject("id")); // Agora id é Long
                rota.setOrigem(rs.getString("origem"));
                rota.setDestino(rs.getString("destino"));
                rotas.add(rota);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todas as rotas como objetos: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return rotas;
    }

    public Rota buscarPorId(long idRota) { // Entrada ainda pode ser long primitivo
        String sql = "SELECT id, origem, destino FROM rotas WHERE id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idRota);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Rota rota = new Rota();
                    rota.setId((Long) rs.getObject("id")); // Agora id é Long
                    rota.setOrigem(rs.getString("origem"));
                    rota.setDestino(rs.getString("destino"));
                    return rota;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar rota por ID: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return null;
    }

    // Se você tem uma tela de CRUD para Rotas, adicione os métodos de inserir, atualizar, excluir aqui
    public long gerarProximoIdRota() {
        String sql = "SELECT nextval('seq_rota')"; // Assumindo uma sequence 'seq_rota'
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gerar próximo ID de rota: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
        }
        return -1;
    }

    public boolean inserir(Rota rota) {
        String sql = "INSERT INTO rotas (id, origem, destino) VALUES (?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, rota.getId()); // ID é Long
            stmt.setString(2, rota.getOrigem());
            stmt.setString(3, rota.getDestino());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao inserir rota: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Rota rota) {
        String sql = "UPDATE rotas SET origem = ?, destino = ? WHERE id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rota.getOrigem());
            stmt.setString(2, rota.getDestino());
            stmt.setObject(3, rota.getId()); // ID é Long
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar rota: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(long id) { // Entrada pode ser long primitivo
        String sql = "DELETE FROM rotas WHERE id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir rota: " + e.getMessage());
            System.err.println("Erro SQL em RotaDAO: " + e.getMessage());
            return false;
        }
    }
}