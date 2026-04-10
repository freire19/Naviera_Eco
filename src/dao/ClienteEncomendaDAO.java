package dao;

import model.ClienteEncomenda;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteEncomendaDAO {

    public List<ClienteEncomenda> listarTodos() {
        List<ClienteEncomenda> clientes = new ArrayList<>();
        String sql = "SELECT * FROM cad_clientes_encomenda WHERE empresa_id = ? ORDER BY nome_cliente ASC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ClienteEncomenda cliente = new ClienteEncomenda();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNomeCliente(rs.getString("nome_cliente"));
                clientes.add(cliente);
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
        }
        return clientes;
    }

    public ClienteEncomenda salvar(ClienteEncomenda cliente) {
        String sql = "INSERT INTO cad_clientes_encomenda (nome_cliente, empresa_id) VALUES (?, ?) ON CONFLICT (nome_cliente) DO NOTHING RETURNING id_cliente";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cliente.getNomeCliente());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    return cliente;
                } else {
                    return buscarPorNomeExato(cliente.getNomeCliente());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
            return null;
        }
    }
    
    public ClienteEncomenda buscarPorNomeExato(String nome) {
        String sql = "SELECT * FROM cad_clientes_encomenda WHERE empresa_id = ? AND nome_cliente = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ClienteEncomenda cliente = new ClienteEncomenda();
                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    cliente.setNomeCliente(rs.getString("nome_cliente"));
                    return cliente;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar cliente por nome: " + e.getMessage());
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
        String sql = "UPDATE cad_clientes_encomenda SET nome_cliente = ? WHERE id_cliente = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cliente.getNomeCliente());
            stmt.setLong(2, cliente.getIdCliente());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
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
        String sql = "DELETE FROM cad_clientes_encomenda WHERE id_cliente = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idCliente);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em ClienteEncomendaDAO: " + e.getMessage());
            return false;
        }
    }
}