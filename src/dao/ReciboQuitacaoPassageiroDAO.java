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
        String sql = "INSERT INTO historico_recibo_quitacao_passageiro (nome_passageiro, data_pagamento, valor_total, forma_pagamento, itens_pagos, empresa_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, recibo.getNomePassageiro());
            stmt.setTimestamp(2, Timestamp.valueOf(recibo.getDataPagamento()));
            // DL063: usar setBigDecimal para valor financeiro
            stmt.setBigDecimal(3, recibo.getValorTotal());
            stmt.setString(4, recibo.getFormaPagamento());
            stmt.setString(5, recibo.getItensPagos());
            stmt.setInt(6, DAOUtils.empresaId());
            
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Erro SQL em ReciboQuitacaoPassageiroDAO: " + e.getMessage());
        }
    }

    public List<ReciboQuitacaoPassageiro> listarPorPassageiro(String nome) {
        List<ReciboQuitacaoPassageiro> lista = new ArrayList<>();
        String sql = "SELECT * FROM historico_recibo_quitacao_passageiro WHERE empresa_id = ? AND nome_passageiro = ? ORDER BY data_pagamento DESC";
        
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setString(2, nome);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReciboQuitacaoPassageiro r = new ReciboQuitacaoPassageiro();
                    r.setId(rs.getInt("id"));
                    r.setNomePassageiro(rs.getString("nome_passageiro"));
                    // DR110: null check para evitar NPE se data_pagamento for NULL
                    java.sql.Timestamp tsPagamento = rs.getTimestamp("data_pagamento");
                    if (tsPagamento != null) r.setDataPagamento(tsPagamento.toLocalDateTime());
                    r.setValorTotal(rs.getBigDecimal("valor_total"));
                    r.setFormaPagamento(rs.getString("forma_pagamento"));
                    r.setItensPagos(rs.getString("itens_pagos"));
                    lista.add(r);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro SQL em ReciboQuitacaoPassageiroDAO: " + e.getMessage());
        }
        return lista;
    }
}