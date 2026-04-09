package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.EncomendaItem;

public class EncomendaItemDAO {

    // Salva um novo item no banco
    public boolean inserir(EncomendaItem item) {
        String sql = "INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total, local_armazenamento) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, item.getIdEncomenda());
            stmt.setInt(2, item.getQuantidade());
            stmt.setString(3, item.getDescricao());
            stmt.setBigDecimal(4, item.getValorUnitario());
            stmt.setBigDecimal(5, item.getValorTotal());
            stmt.setString(6, item.getLocalArmazenamento());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em EncomendaItemDAO: " + e.getMessage());
            return false;
        }
    }

    // Lista todos os itens de uma encomenda específica
    public List<EncomendaItem> listarPorIdEncomenda(Long idEncomenda) {
        List<EncomendaItem> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomenda_itens WHERE id_encomenda = ?";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, idEncomenda);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EncomendaItem item = new EncomendaItem();
                    // DR121: log em catch de coluna ID
                    try {
                        item.setId(rs.getLong("id"));
                    } catch (SQLException e) {
                        try { item.setId(rs.getLong("id_item")); } catch (SQLException ex) { System.err.println("EncomendaItemDAO: nem 'id' nem 'id_item' encontrados"); }
                    }

                    item.setIdEncomenda(rs.getLong("id_encomenda"));
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setDescricao(rs.getString("descricao"));
                    item.setValorUnitario(rs.getBigDecimal("valor_unitario"));
                    item.setValorTotal(rs.getBigDecimal("valor_total"));
                    try {
                        item.setLocalArmazenamento(rs.getString("local_armazenamento"));
                    } catch (SQLException e) {
                        item.setLocalArmazenamento("");
                    }
                    lista.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em EncomendaItemDAO: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Carrega todos os itens de uma viagem agrupados por id_encomenda.
     * Uma unica query em vez de N queries (fix DP004).
     */
    public Map<Long, List<EncomendaItem>> listarItensPorViagem(long idViagem) {
        Map<Long, List<EncomendaItem>> mapa = new HashMap<>();
        String sql = "SELECT ei.* FROM encomenda_itens ei " +
                     "JOIN encomendas e ON ei.id_encomenda = e.id_encomenda " +
                     "WHERE e.id_viagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idViagem);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long idEnc = rs.getLong("id_encomenda");
                    EncomendaItem item = new EncomendaItem();
                    try { item.setId(rs.getLong("id")); } catch (SQLException e) {
                        try { item.setId(rs.getLong("id_item")); } catch (SQLException ex) { System.err.println("EncomendaItemDAO: nem 'id' nem 'id_item' encontrados no ResultSet — " + ex.getMessage()); }
                    }
                    item.setIdEncomenda(idEnc);
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setDescricao(rs.getString("descricao"));
                    item.setValorUnitario(rs.getBigDecimal("valor_unitario"));
                    item.setValorTotal(rs.getBigDecimal("valor_total"));
                    try { item.setLocalArmazenamento(rs.getString("local_armazenamento")); }
                    catch (SQLException e) { item.setLocalArmazenamento(""); }
                    mapa.computeIfAbsent(idEnc, k -> new ArrayList<>()).add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em EncomendaItemDAO.listarItensPorViagem: " + e.getMessage());
        }
        return mapa;
    }

    // === CORREÇÃO: Renomeado para excluirPorEncomenda para bater com o Controller ===
    public boolean excluirPorEncomenda(Long idEncomenda) {
        String sql = "DELETE FROM encomenda_itens WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, idEncomenda);
            stmt.executeUpdate();
            return true; 
            
        } catch (SQLException e) {
            System.err.println("Erro SQL em EncomendaItemDAO: " + e.getMessage());
            return false;
        }
    }
}