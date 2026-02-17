package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Encomenda;

public class EncomendaDAO {

    // CORREÇÃO: Adicionado 'nome_recebedor' que faltava no INSERT
    public Encomenda inserir(Encomenda encomenda) {
        String sql = "INSERT INTO encomendas (id_viagem, numero_encomenda, remetente, destinatario, observacoes, total_volumes, total_a_pagar, valor_pago, desconto, status_pagamento, forma_pagamento, local_pagamento, entregue, doc_recebedor, nome_recebedor, rota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, encomenda.getIdViagem());
            stmt.setString(2, encomenda.getNumeroEncomenda());
            stmt.setString(3, encomenda.getRemetente());
            stmt.setString(4, encomenda.getDestinatario());
            stmt.setString(5, encomenda.getObservacoes());
            stmt.setInt(6, encomenda.getTotalVolumes());
            stmt.setBigDecimal(7, encomenda.getTotalAPagar());
            stmt.setBigDecimal(8, encomenda.getValorPago());
            stmt.setBigDecimal(9, encomenda.getDesconto());
            stmt.setString(10, encomenda.getStatusPagamento());
            stmt.setString(11, encomenda.getFormaPagamento()); 
            stmt.setString(12, encomenda.getLocalPagamento()); 
            stmt.setBoolean(13, encomenda.isEntregue());
            stmt.setString(14, encomenda.getDocRecebedor());
            stmt.setString(15, encomenda.getNomeRecebedor()); // ADICIONADO AQUI
            stmt.setString(16, encomenda.getNomeRota());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) encomenda.setId(generatedKeys.getLong(1));
                }
            }
            return encomenda;
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    public List<Encomenda> listarPorViagem(Long idViagem) {
        List<Encomenda> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomendas WHERE id_viagem = ? ORDER BY CAST(numero_encomenda AS INTEGER)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idViagem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(mapearEncomenda(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public List<Encomenda> listarTodos() {
        List<Encomenda> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomendas ORDER BY id_encomenda DESC"; 
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEncomenda(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public Encomenda buscarPorId(Long id) {
        String sql = "SELECT * FROM encomendas WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearEncomenda(rs);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean atualizar(Encomenda e) {
        // CORREÇÃO: Agora atualiza também os dados de entrega caso sejam editados na tela
        String sql = "UPDATE encomendas SET remetente=?, destinatario=?, observacoes=?, total_volumes=?, total_a_pagar=?, valor_pago=?, status_pagamento=?, forma_pagamento=?, local_pagamento=?, rota=?, numero_encomenda=?, nome_recebedor=?, doc_recebedor=?, entregue=? WHERE id_encomenda=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getRemetente());
            stmt.setString(2, e.getDestinatario());
            stmt.setString(3, e.getObservacoes());
            stmt.setInt(4, e.getTotalVolumes());
            stmt.setBigDecimal(5, e.getTotalAPagar());
            stmt.setBigDecimal(6, e.getValorPago());        
            stmt.setString(7, e.getStatusPagamento());    
            stmt.setString(8, e.getFormaPagamento());      
            stmt.setString(9, e.getLocalPagamento());      
            stmt.setString(10, e.getNomeRota());
            stmt.setString(11, e.getNumeroEncomenda());
            // Novos campos para garantir atualização
            stmt.setString(12, e.getNomeRecebedor());
            stmt.setString(13, e.getDocRecebedor());
            stmt.setBoolean(14, e.isEntregue());
            stmt.setLong(15, e.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }
    
    public boolean atualizarFinanceiro(Long idEncomenda, java.math.BigDecimal valorPago, String status) {
        String sql = "UPDATE encomendas SET valor_pago = ?, status_pagamento = ? WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, valorPago);
            stmt.setString(2, status);
            stmt.setLong(3, idEncomenda);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean excluir(Long id) {
        String sqlItens = "DELETE FROM encomenda_itens WHERE id_encomenda = ?";
        String sqlEnc = "DELETE FROM encomendas WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt1 = conn.prepareStatement(sqlItens)) {
                stmt1.setLong(1, id);
                stmt1.executeUpdate();
            }
            try (PreparedStatement stmt2 = conn.prepareStatement(sqlEnc)) {
                stmt2.setLong(1, id);
                int rows = stmt2.executeUpdate();
                conn.commit();
                return rows > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean registrarEntrega(Long idEncomenda, String docRecebedor, String nomeRecebedor, String statusPagto) {
        // CORREÇÃO: Garante que os nomes das colunas estão certos
        String sql = "UPDATE encomendas SET entregue = TRUE, doc_recebedor = ?, nome_recebedor = ?, status_pagamento = ? WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Normaliza/trima valores e trata strings vazias como NULL
            docRecebedor = (docRecebedor == null || docRecebedor.trim().isEmpty()) ? null : docRecebedor.trim();
            nomeRecebedor = (nomeRecebedor == null || nomeRecebedor.trim().isEmpty()) ? null : nomeRecebedor.trim();
            statusPagto = (statusPagto == null || statusPagto.trim().isEmpty()) ? null : statusPagto.trim();

            stmt.setString(1, docRecebedor);
            stmt.setString(2, nomeRecebedor);
            stmt.setString(3, statusPagto);
            stmt.setLong(4, idEncomenda);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public int obterProximoNumero(Long idViagem, String nomeRota) {
        String sql = "SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) FROM encomendas WHERE id_viagem = ? AND rota = ?";
        try (Connection conn = ConexaoBD.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idViagem);
            stmt.setString(2, nomeRota);
            try (ResultSet rs = stmt.executeQuery()) { 
                if (rs.next()) return rs.getInt(1) + 1; 
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 1;
    }
    
    public int obterProximaEncomendaNum() { return 1; }
    public int obterProximoNumeroPorRota(Long idRota) { return 1; }

    private Encomenda mapearEncomenda(ResultSet rs) throws SQLException {
        Encomenda e = new Encomenda();
        e.setId(rs.getLong("id_encomenda"));
        e.setIdViagem(rs.getLong("id_viagem"));
        e.setNumeroEncomenda(rs.getString("numero_encomenda"));
        e.setRemetente(rs.getString("remetente"));
        e.setDestinatario(rs.getString("destinatario"));
        e.setObservacoes(rs.getString("observacoes"));
        e.setTotalVolumes(rs.getInt("total_volumes"));
        e.setTotalAPagar(rs.getBigDecimal("total_a_pagar"));
        e.setValorPago(rs.getBigDecimal("valor_pago"));
        e.setDesconto(rs.getBigDecimal("desconto"));
        e.setStatusPagamento(rs.getString("status_pagamento"));
        
        // CORREÇÃO: Leitura segura do booleano
        e.setEntregue(rs.getBoolean("entregue"));
        
        try { e.setFormaPagamento(rs.getString("forma_pagamento")); } catch (Exception ex) {}
        try { e.setLocalPagamento(rs.getString("local_pagamento")); } catch (Exception ex) {}
        
        // CORREÇÃO: Tenta ler o nome_recebedor, se a coluna não existir, ele ignora sem travar
        try { e.setDocRecebedor(rs.getString("doc_recebedor")); } catch (Exception ex) {}
        try { e.setNomeRecebedor(rs.getString("nome_recebedor")); } catch (Exception ex) {}
        
        try { e.setNomeRota(rs.getString("rota")); } catch (Exception ex) {}
        try { e.setIdCaixa(rs.getInt("id_caixa")); } catch (Exception ex) {}

        return e;
    }
}