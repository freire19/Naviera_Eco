package dao;

import model.ClienteFrete;
import java.sql.Connection;
import static dao.DAOUtils.empresaId;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

/**
 * DAO para clientes de frete — tabela cad_clientes_frete.
 * Separado de ClienteEncomendaDAO (cada módulo tem tabela própria).
 */
public class ClienteFreteDAO {

    public List<ClienteFrete> listarTodos() {
        List<ClienteFrete> clientes = new ArrayList<>();
        String sql = "SELECT id_cliente, nome_cliente, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone FROM cad_clientes_frete WHERE empresa_id = ? ORDER BY nome_cliente ASC";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao listar: " + e.getMessage());
        }
        return clientes;
    }

    public List<String> listarNomes() {
        List<String> nomes = new ArrayList<>();
        String sql = "SELECT nome_cliente FROM cad_clientes_frete WHERE empresa_id = ? ORDER BY nome_cliente ASC";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nome = rs.getString("nome_cliente");
                    if (nome != null) nomes.add(nome);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao listar nomes: " + e.getMessage());
        }
        return nomes;
    }

    public ClienteFrete salvar(ClienteFrete cliente) {
        String sql = "INSERT INTO cad_clientes_frete (nome_cliente, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone, empresa_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (empresa_id, nome_cliente) DO NOTHING RETURNING id_cliente";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, upper(cliente.getNomeCliente()));
            stmt.setString(2, upper(cliente.getRazaoSocial()));
            stmt.setString(3, cliente.getCpfCnpj());
            stmt.setString(4, upper(cliente.getEndereco()));
            stmt.setString(5, cliente.getInscricaoEstadual());
            stmt.setString(6, cliente.getEmail());
            stmt.setString(7, cliente.getTelefone());
            stmt.setInt(8, empresaId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente.setIdCliente(rs.getLong("id_cliente"));
                    return cliente;
                } else {
                    return buscarPorNome(cliente.getNomeCliente());
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao salvar: " + e.getMessage());
            return null;
        }
    }

    public boolean atualizar(ClienteFrete cliente) {
        String sql = "UPDATE cad_clientes_frete SET nome_cliente = ?, razao_social = ?, cpf_cnpj = ?, endereco = ?, inscricao_estadual = ?, email = ?, telefone = ? " +
                     "WHERE id_cliente = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, upper(cliente.getNomeCliente()));
            stmt.setString(2, upper(cliente.getRazaoSocial()));
            stmt.setString(3, cliente.getCpfCnpj());
            stmt.setString(4, upper(cliente.getEndereco()));
            stmt.setString(5, cliente.getInscricaoEstadual());
            stmt.setString(6, cliente.getEmail());
            stmt.setString(7, cliente.getTelefone());
            stmt.setLong(8, cliente.getIdCliente());
            stmt.setInt(9, empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao atualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(Long idCliente) {
        String sql = "DELETE FROM cad_clientes_frete WHERE id_cliente = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idCliente);
            stmt.setInt(2, empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao excluir: " + e.getMessage());
            return false;
        }
    }

    public ClienteFrete buscarPorNome(String nome) {
        String sql = "SELECT id_cliente, nome_cliente, razao_social, cpf_cnpj, endereco, inscricao_estadual, email, telefone FROM cad_clientes_frete WHERE empresa_id = ? AND LOWER(nome_cliente) = LOWER(?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empresaId());
            stmt.setString(2, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            AppLogger.warn("ClienteFreteDAO", "Erro ao buscar por nome: " + e.getMessage());
        }
        return null;
    }

    /**
     * Insere contato rápido só com nome (compatibilidade com fluxo antigo).
     */
    public String inserirContato(String nome) throws SQLException {
        String nomeUpper = nome.trim().toUpperCase();
        String sql = "INSERT INTO cad_clientes_frete (nome_cliente, empresa_id) VALUES (?, ?) ON CONFLICT (empresa_id, nome_cliente) DO NOTHING";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nomeUpper);
            pst.setInt(2, empresaId());
            pst.executeUpdate();
        }
        return nomeUpper;
    }

    private ClienteFrete mapear(ResultSet rs) throws SQLException {
        ClienteFrete c = new ClienteFrete();
        c.setIdCliente(rs.getLong("id_cliente"));
        c.setNomeCliente(rs.getString("nome_cliente"));
        c.setRazaoSocial(rs.getString("razao_social"));
        c.setCpfCnpj(rs.getString("cpf_cnpj"));
        c.setEndereco(rs.getString("endereco"));
        c.setInscricaoEstadual(rs.getString("inscricao_estadual"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        return c;
    }

    private String upper(String s) {
        return s != null ? s.trim().toUpperCase() : null;
    }
}
