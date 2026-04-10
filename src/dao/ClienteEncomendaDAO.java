package dao;

// Multi-tenant imports added automatically

import model.ClienteEncomenda;
import java.sql.Connection;
// tenant filter
import static dao.DAOUtils.empresaId;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import gui.util.AppLogger;

public class ClienteEncomendaDAO {

    public List<ClienteEncomenda> listarTodos() {
        List<ClienteEncomenda> clientes = new ArrayList<>();
        String sql = "SELECT * FROM cad_clientes_encomenda WHERE empresa_id = ? ORDER BY nome_cliente ASC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empresaId());
            try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ClienteEncomenda cliente = new ClienteEncomenda();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNomeCliente(rs.getString("nome_cliente"));
                clientes.add(cliente);
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteEncomendaDAO", "Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
        }
        return clientes;
    }

    public ClienteEncomenda salvar(ClienteEncomenda cliente) {
        String sql = "INSERT INTO cad_clientes_encomenda (nome_cliente, empresa_id) VALUES (?, ?) ON CONFLICT (nome_cliente) DO NOTHING RETURNING id_cliente";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cliente.getNomeCliente());
            stmt.setInt(2, empresaId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    return cliente;
                } else {
                    return buscarPorNomeExato(cliente.getNomeCliente());
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteEncomendaDAO", "Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
            return null;
        }
    }
    
    public ClienteEncomenda buscarPorNomeExato(String nome) {
        String sql = "SELECT * FROM cad_clientes_encomenda WHERE empresa_id = ? AND nome_cliente = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, empresaId());
            stmt.setString(2, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ClienteEncomenda cliente = new ClienteEncomenda();
                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    cliente.setNomeCliente(rs.getString("nome_cliente"));
                    return cliente;
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteEncomendaDAO", "Erro ao buscar cliente por nome: " + e.getMessage());
        }
        return null;
    }

    // =======================================================================
    // <<< NOVO MÉTODO >>>
    // =======================================================================
    /**
     * Atualiza o nome de um cliente existente no banco de dados.
     * @param cliente O objeto ClienteEncomenda com o ID e o novo nome.
     * @return true se a atualização foi bem-sucedida, false caso contrário.
     */
    public boolean atualizar(ClienteEncomenda cliente) {
        String sql = "UPDATE cad_clientes_encomenda SET nome_cliente = ? WHERE id_cliente = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNomeCliente());
            stmt.setLong(2, cliente.getIdCliente());
            stmt.setInt(3, empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("ClienteEncomendaDAO", "Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
            return false;
        }
    }

    // =======================================================================
    // <<< NOVO MÉTODO >>>
    // =======================================================================
    /**
     * Exclui um cliente do banco de dados pelo seu ID.
     * @param idCliente O ID do cliente a ser excluído.
     * @return true se a exclusão foi bem-sucedida, false caso contrário.
     */
    public boolean excluir(Long idCliente) {
        String sql = "DELETE FROM cad_clientes_encomenda WHERE id_cliente = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idCliente);
            stmt.setInt(2, empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("ClienteEncomendaDAO", "Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
            return false;
        }
    }
}