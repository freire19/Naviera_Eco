package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.ReciboQuitacaoPassageiro;

public class ReciboQuitacaoPassageiroDAO {

    public void salvar(ReciboQuitacaoPassageiro recibo) {
        // Atualizei o nome da tabela aqui
        String sql = "INSERT INTO historico_recibo_quitacao_passageiro (nome_passageiro, data_pagamento, valor_total, forma_pagamento, itens_pagos) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, recibo.getNomePassageiro());
            stmt.setTimestamp(2, Timestamp.valueOf(recibo.getDataPagamento()));
            stmt.setDouble(3, recibo.getValorTotal());
            stmt.setString(4, recibo.getFormaPagamento());
            stmt.setString(5, recibo.getItensPagos());
            
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro SQL em ReciboQuitacaoPassageiroDAO: " + e.getMessage());
        }
    }

    public List<ReciboQuitacaoPassageiro> listarPorPassageiro(String nome) {
        List<ReciboQuitacaoPassageiro> lista = new ArrayList<>();
        // Atualizei o nome da tabela aqui
        String sql = "SELECT * FROM historico_recibo_quitacao_passageiro WHERE nome_passageiro = ? ORDER BY data_pagamento DESC";
        
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ReciboQuitacaoPassageiro r = new ReciboQuitacaoPassageiro();
                r.setId(rs.getInt("id"));
                r.setNomePassageiro(rs.getString("nome_passageiro"));
                r.setDataPagamento(rs.getTimestamp("data_pagamento").toLocalDateTime());
                r.setValorTotal(rs.getDouble("valor_total"));
                r.setFormaPagamento(rs.getString("forma_pagamento"));
                r.setItensPagos(rs.getString("itens_pagos"));
                lista.add(r);
            }
        } catch (Exception e) {
            System.err.println("Erro SQL em ReciboQuitacaoPassageiroDAO: " + e.getMessage());
        }
        return lista;
    }
}