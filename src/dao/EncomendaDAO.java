package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.Encomenda;

// DR026: Convencao de retorno de erro neste DAO — metodos que retornam Encomenda retornam
// null em caso de falha; metodos de busca retornam lista vazia ou null. Veja DR026 em STATUS.md.
public class EncomendaDAO {

    /** Insere encomenda sem itens (legado — callers que inserem itens separadamente). */
    public Encomenda inserir(Encomenda encomenda) {
        return inserirComItens(encomenda, null);
    }

    /**
     * #029: Insere encomenda + itens em transacao atomica.
     * Se itens for null/vazio, insere apenas a encomenda.
     */
    public Encomenda inserirComItens(Encomenda encomenda, java.util.List<model.EncomendaItem> itens) {
        String sqlEnc = "INSERT INTO encomendas (id_viagem, numero_encomenda, remetente, destinatario, observacoes, total_volumes, total_a_pagar, valor_pago, desconto, status_pagamento, forma_pagamento, local_pagamento, entregue, doc_recebedor, nome_recebedor, rota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO encomenda_itens (id_encomenda, quantidade, descricao, valor_unitario, valor_total, local_armazenamento) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insere encomenda
                try (PreparedStatement stmt = conn.prepareStatement(sqlEnc, PreparedStatement.RETURN_GENERATED_KEYS)) {
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
                    stmt.setString(15, encomenda.getNomeRecebedor());
                    stmt.setString(16, encomenda.getNomeRota());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) encomenda.setId(generatedKeys.getLong(1));
                        }
                    }
                }

                // 2. Insere itens na mesma transacao
                if (itens != null && !itens.isEmpty()) {
                    try (PreparedStatement stmtItem = conn.prepareStatement(sqlItem)) {
                        for (model.EncomendaItem item : itens) {
                            stmtItem.setLong(1, encomenda.getId());
                            stmtItem.setInt(2, item.getQuantidade());
                            stmtItem.setString(3, item.getDescricao());
                            stmtItem.setBigDecimal(4, item.getValorUnitario());
                            stmtItem.setBigDecimal(5, item.getValorTotal());
                            stmtItem.setString(6, item.getLocalArmazenamento());
                            stmtItem.addBatch();
                        }
                        stmtItem.executeBatch();
                    }
                }

                conn.commit();
                return encomenda;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO.inserirComItens: " + e.getMessage()); return null; }
    }

    public List<Encomenda> listarPorViagem(Long idViagem) {
        List<Encomenda> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomendas WHERE id_viagem = ? ORDER BY id_encomenda";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idViagem);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearEncomenda(rs));
                }
            }
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO: " + e.getMessage()); }
        return lista;
    }

    public List<Encomenda> listarTodos() {
        List<Encomenda> lista = new ArrayList<>();
        String sql = "SELECT * FROM encomendas ORDER BY id_encomenda DESC LIMIT 500"; 
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEncomenda(rs));
            }
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO: " + e.getMessage()); }
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
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO: " + e.getMessage()); }
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
        } catch (SQLException ex) { System.err.println("Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); return false; }
    }
    
    public boolean atualizarFinanceiro(Long idEncomenda, java.math.BigDecimal valorPago, String status) {
        String sql = "UPDATE encomendas SET valor_pago = ?, status_pagamento = ? WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, valorPago);
            stmt.setString(2, status);
            stmt.setLong(3, idEncomenda);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO: " + e.getMessage()); return false; }
    }

    public boolean excluir(Long id) {
        String sqlItens = "DELETE FROM encomenda_itens WHERE id_encomenda = ?";
        String sqlEnc = "DELETE FROM encomendas WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt1 = conn.prepareStatement(sqlItens)) {
                    stmt1.setLong(1, id);
                    stmt1.executeUpdate();
                }
                try (PreparedStatement stmt2 = conn.prepareStatement(sqlEnc)) {
                    stmt2.setLong(1, id);
                    int rows = stmt2.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO.excluir: " + e.getMessage()); return false; }
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
        } catch (SQLException e) { System.err.println("Erro SQL em EncomendaDAO: " + e.getMessage()); return false; }
    }

    public int obterProximoNumero(Long idViagem, String nomeRota) {
        // Usa sequence para evitar race condition (DL002)
        String sql = "SELECT nextval('seq_numero_encomenda')";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // Fallback se sequence não existir ainda (rodar script 005)
            System.err.println("Sequence seq_numero_encomenda não encontrada. Usando fallback MAX+1. Execute o script 005.");
            // DL023: filtrar apenas registros numericos para evitar CAST crash
            String fallback = "SELECT COALESCE(MAX(CAST(numero_encomenda AS INTEGER)), 0) FROM encomendas WHERE id_viagem = ? AND rota = ? AND numero_encomenda ~ '^[0-9]+$'";
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmt2 = conn.prepareStatement(fallback)) {
                stmt2.setLong(1, idViagem);
                stmt2.setString(2, nomeRota);
                try (ResultSet rs = stmt2.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) + 1;
                }
            } catch (SQLException ex) { System.err.println("Erro: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
        }
        return 1;
    }
    
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
        
        // DR119: colunas opcionais — log minimo para diagnostico de schema changes
        try { e.setFormaPagamento(rs.getString("forma_pagamento")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional forma_pagamento ausente"); }
        try { e.setLocalPagamento(rs.getString("local_pagamento")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional local_pagamento ausente"); }
        try { e.setDocRecebedor(rs.getString("doc_recebedor")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional doc_recebedor ausente"); }
        try { e.setNomeRecebedor(rs.getString("nome_recebedor")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional nome_recebedor ausente"); }
        try { e.setNomeRota(rs.getString("rota")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional rota ausente"); }
        try { e.setIdCaixa(rs.getInt("id_caixa")); } catch (Exception ex) { System.err.println("EncomendaDAO: coluna opcional id_caixa ausente"); }

        return e;
    }
}