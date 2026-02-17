package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.ItemEncomendaPadrao;

public class ItemEncomendaPadraoDAO {

    // --- ESTE É O MÉTODO QUE FALTAVA PARA CORRIGIR O ERRO ---
    public void salvar(ItemEncomendaPadrao item) {
        if (item.getDescricao() == null) item.setDescricao(""); 
        if (item.getUnidadeMedida() == null) item.setUnidadeMedida("UN"); 
        item.setAtivo(true); 
        
        inserir(item);
    }
    // -------------------------------------------------------

    public List<ItemEncomendaPadrao> listarTodos(boolean apenasAtivos) {
        List<ItemEncomendaPadrao> lista = new ArrayList<>();
        String sql = "SELECT * FROM itens_encomenda_padrao ";
        if (apenasAtivos) {
            sql += " WHERE ativo = true ";
        }
        sql += " ORDER BY nome_item";

        try (Connection conn = ConexaoBD.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ItemEncomendaPadrao item = new ItemEncomendaPadrao();
                item.setId(rs.getLong("id_item_encomenda")); 
                item.setNomeItem(rs.getString("nome_item"));
                item.setDescricao(rs.getString("descricao"));
                item.setPrecoUnit(rs.getBigDecimal("preco_unitario_padrao")); 
                item.setUnidadeMedida(rs.getString("unidade_medida"));
                item.setPermiteValorDeclarado(rs.getBoolean("permite_valor_declarado"));
                item.setAtivo(rs.getBoolean("ativo"));
                
                lista.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    public boolean inserir(ItemEncomendaPadrao item) {
        String sql = "INSERT INTO itens_encomenda_padrao (nome_item, descricao, unidade_medida, preco_unitario_padrao, permite_valor_declarado, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getNomeItem());
            stmt.setString(2, item.getDescricao());
            stmt.setString(3, item.getUnidadeMedida());
            stmt.setBigDecimal(4, item.getPrecoUnit());
            stmt.setBoolean(5, item.isPermiteValorDeclarado());
            stmt.setBoolean(6, item.isAtivo());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}