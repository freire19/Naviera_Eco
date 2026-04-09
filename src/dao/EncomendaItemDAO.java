package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

    /**
     * Resolve o nome da coluna PK de encomenda_itens uma vez, antes do loop de rows.
     * Evita o anti-padrao de usar try/catch por linha para detectar nome de coluna (DM036).
     */
    private String resolverColunaId(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i);
            if ("id_item_encomenda".equalsIgnoreCase(col) || "id_item".equalsIgnoreCase(col)) {
                return col;
            }
        }
        return "id"; // nome padrao da coluna PK
    }

    /**
     * Mapeia uma linha do ResultSet para EncomendaItem.
     * colId deve ser determinado uma unica vez antes do loop (via resolverColunaId).
     * local_armazenamento e lida com getObject para tratar NULL sem excecao.
     */
    private EncomendaItem mapEncomendaItem(ResultSet rs, String colId) throws SQLException {
        EncomendaItem item = new EncomendaItem();
        item.setId(rs.getLong(colId));
        item.setIdEncomenda(rs.getLong("id_encomenda"));
        item.setQuantidade(rs.getInt("quantidade"));
        item.setDescricao(rs.getString("descricao"));
        item.setValorUnitario(rs.getBigDecimal("valor_unitario"));
        item.setValorTotal(rs.getBigDecimal("valor_total"));
        Object localArm = rs.getObject("local_armazenamento");
        item.setLocalArmazenamento(localArm != null ? localArm.toString() : "");
        return item;
    }

    // Lista todos os itens de uma encomenda específica
    public List<EncomendaItem> listarPorIdEncomenda(Long idEncomenda) {
        List<EncomendaItem> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomenda_itens WHERE id_encomenda = ?";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idEncomenda);
            try (ResultSet rs = stmt.executeQuery()) {
                // DM036: resolve o nome da coluna PK uma unica vez, antes do loop
                String colId = resolverColunaId(rs);
                while (rs.next()) {
                    lista.add(mapEncomendaItem(rs, colId));
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
                // DM036: resolve o nome da coluna PK uma unica vez, antes do loop
                String colId = resolverColunaId(rs);
                while (rs.next()) {
                    long idEnc = rs.getLong("id_encomenda");
                    EncomendaItem item = mapEncomendaItem(rs, colId);
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