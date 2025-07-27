package dao;

import model.ItemFrete;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ItemFreteDAO {

    // Método para inserir um novo ItemFrete
    public boolean inserir(ItemFrete item) throws SQLException {
        String sql = "INSERT INTO itens_frete_padrao (nome_item, descricao, unidade_medida, preco_unitario_padrao, preco_unitario_desconto, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, item.getNomeItem());
            stmt.setString(2, item.getDescricao());
            stmt.setString(3, item.getUnidadeMedida());
            stmt.setBigDecimal(4, item.getPrecoUnitarioPadrao());
            stmt.setBigDecimal(5, item.getPrecoUnitarioDesconto());
            stmt.setBoolean(6, item.isAtivo());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setIdItemFrete(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    // Método para atualizar um ItemFrete existente
    public boolean atualizar(ItemFrete item) throws SQLException {
        String sql = "UPDATE itens_frete_padrao SET nome_item = ?, descricao = ?, unidade_medida = ?, preco_unitario_padrao = ?, preco_unitario_desconto = ?, ativo = ? WHERE id_item_frete = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getNomeItem());
            stmt.setString(2, item.getDescricao());
            stmt.setString(3, item.getUnidadeMedida());
            stmt.setBigDecimal(4, item.getPrecoUnitarioPadrao());
            stmt.setBigDecimal(5, item.getPrecoUnitarioDesconto());
            stmt.setBoolean(6, item.isAtivo());
            stmt.setInt(7, item.getIdItemFrete());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Método para excluir um ItemFrete por ID
    public boolean excluir(int id) throws SQLException {
        String sql = "DELETE FROM itens_frete_padrao WHERE id_item_frete = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Método para listar ItemFrete:
    // se incluirInativos for TRUE, retorna todos (ativos e inativos).
    // se incluirInativos for FALSE, retorna APENAS os ativos.
    public List<ItemFrete> listarTodos(boolean incluirInativos) throws SQLException {
        List<ItemFrete> itens = new ArrayList<>();
        String sql = "SELECT id_item_frete, nome_item, descricao, unidade_medida, preco_unitario_padrao, preco_unitario_desconto, ativo FROM itens_frete_padrao";
        if (!incluirInativos) { // Se não é para incluir inativos, filtra por ativo = TRUE
            sql += " WHERE ativo = TRUE";
        }
        sql += " ORDER BY nome_item";

        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ItemFrete item = new ItemFrete();
                item.setIdItemFrete(rs.getInt("id_item_frete"));
                item.setNomeItem(rs.getString("nome_item"));
                item.setDescricao(rs.getString("descricao"));
                item.setUnidadeMedida(rs.getString("unidade_medida"));
                item.setPrecoUnitarioPadrao(rs.getBigDecimal("preco_unitario_padrao"));
                item.setPrecoUnitarioDesconto(rs.getBigDecimal("preco_unitario_desconto"));
                item.setAtivo(rs.getBoolean("ativo"));
                itens.add(item);
            }
        }
        return itens;
    }
    
    // Método para listar TODOS os ItemFrete (ativos e inativos), pode ser útil para outras partes do sistema.
    // Mantenho este como uma sobrecarga se você precisar de uma forma de listar todos sem especificar um boolean.
    public List<ItemFrete> listarTodos() throws SQLException {
        return listarTodos(true); // Chama o método com true para incluir inativos por padrão (listar todos)
    }
}