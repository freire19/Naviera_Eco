package dao;

import model.EncomendaItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para itens de encomenda padrão, operando sobre a tabela `itens_encomenda_padrao`.
 *
 * Colunas esperadas na tabela:
 *   - id_item_encomenda
 *   - nome_item
 *   - preco_unitario_padrao
 *   - unidade_medida
 *   - permite_valor_declarado
 *   - descricao
 *   - ativo
 */
public class EncomendaItemDAO {

    /**
     * Lista todos os itens de encomenda cadastrados em itens_encomenda_padrao.
     *
     * @param incluirInativos se true: traz também itens com ativo = false; 
     *                        se false: apenas ativo = true.
     * @return lista de EncomendaItem
     * @throws SQLException em caso de erro de acesso ao banco
     */
    public List<EncomendaItem> listarTodos(boolean incluirInativos) throws SQLException {
        List<EncomendaItem> lista = new ArrayList<>();
        String sql = "SELECT id_item_encomenda, nome_item, preco_unitario_padrao, unidade_medida, "
                   + "permite_valor_declarado, descricao, ativo "
                   + "FROM itens_encomenda_padrao";

        if (!incluirInativos) {
            sql += " WHERE ativo = TRUE";
        }
        sql += " ORDER BY nome_item";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                EncomendaItem item = new EncomendaItem();
                item.setId(rs.getInt("id_item_encomenda"));
                item.setNomeItem(rs.getString("nome_item"));
                item.setPrecoUnit(rs.getBigDecimal("preco_unitario_padrao"));
                item.setUnidadeMedida(rs.getString("unidade_medida"));
                item.setPermiteValorDeclarado(rs.getBoolean("permite_valor_declarado"));
                item.setDescricao(rs.getString("descricao"));
                item.setAtivo(rs.getBoolean("ativo"));
                lista.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar itens de encomenda no DAO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return lista;
    }

    /**
     * Insere um novo item na tabela itens_encomenda_padrao. 
     * Caso você queira cadastrar itens via interface, use este método.
     *
     * @param item objeto contendo dados a serem persistidos
     * @return o mesmo objeto com o ID preenchido, caso inserido com sucesso
     * @throws SQLException em caso de erro de banco
     */
    public EncomendaItem inserir(EncomendaItem item) throws SQLException {
        String sql = "INSERT INTO itens_encomenda_padrao "
                   + "(nome_item, preco_unitario_padrao, unidade_medida, "
                   + "permite_valor_declarado, descricao, ativo) "
                   + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_item_encomenda";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getNomeItem());
            ps.setBigDecimal(2, item.getPrecoUnit());
            ps.setString(3, item.getUnidadeMedida());
            ps.setBoolean(4, item.isPermiteValorDeclarado());
            ps.setString(5, item.getDescricao());
            ps.setBoolean(6, item.isAtivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.setId(rs.getInt("id_item_encomenda"));
                    return item;
                } else {
                    throw new SQLException("Falha ao inserir item de encomenda: nenhum ID retornado.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir item de encomenda no DAO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Atualiza um registro existente em itens_encomenda_padrao.
     *
     * @param item objeto contendo o ID e os novos valores
     * @return true se ao menos uma linha for afetada; false caso contrário
     * @throws SQLException em caso de erro de banco
     */
    public boolean atualizar(EncomendaItem item) throws SQLException {
        String sql = "UPDATE itens_encomenda_padrao SET "
                   + "nome_item = ?, preco_unitario_padrao = ?, unidade_medida = ?, "
                   + "permite_valor_declarado = ?, descricao = ?, ativo = ? "
                   + "WHERE id_item_encomenda = ?";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getNomeItem());
            ps.setBigDecimal(2, item.getPrecoUnit());
            ps.setString(3, item.getUnidadeMedida());
            ps.setBoolean(4, item.isPermiteValorDeclarado());
            ps.setString(5, item.getDescricao());
            ps.setBoolean(6, item.isAtivo());
            ps.setInt(7, item.getId());

            int afetadas = ps.executeUpdate();
            return afetadas > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar item de encomenda no DAO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Exclui (ou desativa) um item na tabela, definindo ativo = false.
     * Se você preferir deletar fisicamente, basta usar DELETE ao invés de UPDATE.
     *
     * @param id ID do item a ser “deletado”
     * @return true se ao menos uma linha for afetada; false caso contrário
     * @throws SQLException em caso de erro de banco
     */
    public boolean excluir(int id) throws SQLException {
        String sql = "UPDATE itens_encomenda_padrao SET ativo = FALSE WHERE id_item_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int afetadas = ps.executeUpdate();
            return afetadas > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao “excluir” (desativar) item de encomenda no DAO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
