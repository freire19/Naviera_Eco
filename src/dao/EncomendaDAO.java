package dao;

import model.Encomenda;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;

/**
 * DAO para CRUD de encomendas. Tabela: encomendas,
 * colunas:
 *   id_encomenda (PK),
 *   numero_encomenda (string),
 *   remetente, destinatario, doc_recebedor, observacao, viagem,
 *   valor_nominal, desconto, valor_pago, valor_a_pagar, devedor,
 *   tipo_pagamento, caixa, data_encomenda, entregue (bool), data_entrega (date)
 */
public class EncomendaDAO {

    /**
     * Atualiza apenas o status de entrega de uma encomenda.
     * Retorna true se a atualização ocorreu bem.
     */
    public boolean atualizarStatusEntrega(int idEncomenda, boolean entregue, String docRecebedor, LocalDate dataEntrega) {
        String sql = "UPDATE encomendas SET entregue = ?, doc_recebedor = ?, data_entrega = ? WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, entregue);
            pstmt.setString(2, docRecebedor);
            if (dataEntrega != null) {
                pstmt.setDate(3, java.sql.Date.valueOf(dataEntrega));
            } else {
                pstmt.setNull(3, Types.DATE);
            }
            pstmt.setInt(4, idEncomenda);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar status de entrega da encomenda ID " + idEncomenda + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retorna o próximo número de encomenda (tentando extrair somente numéricos do campo numero_encomenda).
     * Se não der certo, faz fallback pela PK id_encomenda.
     */
    public int obterProximaEncomendaNum() {
        // Atenção: REGEXP_REPLACE e operador ~ funcionam nativamente no PostgreSQL ≥ 10.
        String sql = "SELECT COALESCE(MAX(CAST(REGEXP_REPLACE(numero_encomenda, '[^0-9]', '', 'g') AS INTEGER)), 0) + 1 " +
                     "FROM encomendas WHERE numero_encomenda ~ '^[0-9]+$'";
        String sqlFallback = "SELECT COALESCE(MAX(id_encomenda), 0) + 1 FROM encomendas";

        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int nextNum = rs.getInt(1);
                return (nextNum > 0) ? nextNum : 1;
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter próximo número de encomenda (tentativa 1): " + e.getMessage() + ". Tentando fallback.");
            try (Connection conn = ConexaoBD.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlFallback)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e2) {
                System.err.println("Erro ao obter próximo número de encomenda (fallback): " + e2.getMessage());
                e2.printStackTrace();
            }
        }
        return 1;
    }

    /**
     * Insere nova encomenda (sem itens). Retorna true se inserido.
     * Caso bem-sucedido, preenche o campo id_encomenda no próprio objeto Encomenda.
     */
    public boolean inserir(Encomenda encomenda) {
        String sql = "INSERT INTO encomendas (" +
                     "numero_encomenda, remetente, destinatario, doc_recebedor, observacao, viagem, " +
                     "valor_nominal, desconto, valor_pago, valor_a_pagar, devedor, tipo_pagamento, caixa, data_encomenda, entregue, data_entrega" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, encomenda.getNumeroEncomenda());
            pstmt.setString(2, encomenda.getRemetente());
            pstmt.setString(3, encomenda.getDestinatario());
            pstmt.setString(4, encomenda.getDocRecebedor());
            pstmt.setString(5, encomenda.getObservacao());
            pstmt.setString(6, encomenda.getViagem());
            pstmt.setBigDecimal(7, encomenda.getValorNominal());
            pstmt.setBigDecimal(8, encomenda.getDesconto());
            pstmt.setBigDecimal(9, encomenda.getValorPago());
            pstmt.setBigDecimal(10, encomenda.getValorAPagar());
            pstmt.setBigDecimal(11, encomenda.getDevedor());
            pstmt.setString(12, encomenda.getTipoPagamento());
            pstmt.setString(13, encomenda.getCaixa());
            pstmt.setDate(14, java.sql.Date.valueOf(encomenda.getData()));
            pstmt.setBoolean(15, encomenda.isEntregue());
            if (encomenda.getDataEntrega() != null) {
                pstmt.setDate(16, java.sql.Date.valueOf(encomenda.getDataEntrega()));
            } else {
                pstmt.setNull(16, Types.DATE);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        encomenda.setId(generatedKeys.getInt(1));
                        return true;
                    } else {
                        System.err.println("Falha ao obter ID gerado para nova encomenda.");
                        return false;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Erro ao inserir encomenda: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza dados completos de uma encomenda já existente (incluindo seus itens de encomenda, que devem ser tratados separadamente).
     */
    public boolean atualizar(Encomenda encomenda) {
        String sql = "UPDATE encomendas SET " +
                     "numero_encomenda = ?, remetente = ?, destinatario = ?, doc_recebedor = ?, observacao = ?, viagem = ?, " +
                     "valor_nominal = ?, desconto = ?, valor_pago = ?, valor_a_pagar = ?, devedor = ?, tipo_pagamento = ?, caixa = ?, " +
                     "data_encomenda = ?, entregue = ?, data_entrega = ? " +
                     "WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, encomenda.getNumeroEncomenda());
            pstmt.setString(2, encomenda.getRemetente());
            pstmt.setString(3, encomenda.getDestinatario());
            pstmt.setString(4, encomenda.getDocRecebedor());
            pstmt.setString(5, encomenda.getObservacao());
            pstmt.setString(6, encomenda.getViagem());
            pstmt.setBigDecimal(7, encomenda.getValorNominal());
            pstmt.setBigDecimal(8, encomenda.getDesconto());
            pstmt.setBigDecimal(9, encomenda.getValorPago());
            pstmt.setBigDecimal(10, encomenda.getValorAPagar());
            pstmt.setBigDecimal(11, encomenda.getDevedor());
            pstmt.setString(12, encomenda.getTipoPagamento());
            pstmt.setString(13, encomenda.getCaixa());
            pstmt.setDate(14, java.sql.Date.valueOf(encomenda.getData()));
            pstmt.setBoolean(15, encomenda.isEntregue());
            if (encomenda.getDataEntrega() != null) {
                pstmt.setDate(16, java.sql.Date.valueOf(encomenda.getDataEntrega()));
            } else {
                pstmt.setNull(16, Types.DATE);
            }
            pstmt.setInt(17, encomenda.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar encomenda: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exclui uma encomenda (e normalmente, antes de chamar isto, deve-se apagar os itens relacionados em encomenda_itens).
     */
    public boolean excluir(int idEncomenda) {
        String sql = "DELETE FROM encomendas WHERE id_encomenda = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idEncomenda);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir encomenda: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca encomenda pelo campo numero_encomenda (string exata).
     * Retorna null se não encontrar.
     */
    public Encomenda buscarPorNumero(String numeroEncomenda) {
        String sql = "SELECT * FROM encomendas WHERE numero_encomenda = ?";
        Encomenda encomenda = null;
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroEncomenda);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    encomenda = new Encomenda();
                    encomenda.setId(rs.getInt("id_encomenda"));
                    encomenda.setNumeroEncomenda(rs.getString("numero_encomenda"));
                    encomenda.setRemetente(rs.getString("remetente"));
                    encomenda.setDestinatario(rs.getString("destinatario"));
                    encomenda.setDocRecebedor(rs.getString("doc_recebedor"));
                    encomenda.setObservacao(rs.getString("observacao"));
                    encomenda.setViagem(rs.getString("viagem"));
                    encomenda.setValorNominal(rs.getBigDecimal("valor_nominal"));
                    encomenda.setDesconto(rs.getBigDecimal("desconto"));
                    encomenda.setValorPago(rs.getBigDecimal("valor_pago"));
                    encomenda.setValorAPagar(rs.getBigDecimal("valor_a_pagar"));
                    encomenda.setDevedor(rs.getBigDecimal("devedor"));
                    encomenda.setTipoPagamento(rs.getString("tipo_pagamento"));
                    encomenda.setCaixa(rs.getString("caixa"));
                    if (rs.getDate("data_encomenda") != null) {
                        encomenda.setData(rs.getDate("data_encomenda").toLocalDate());
                    }
                    encomenda.setEntregue(rs.getBoolean("entregue"));
                    if (rs.getDate("data_entrega") != null) {
                        encomenda.setDataEntrega(rs.getDate("data_entrega").toLocalDate());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar encomenda por número: " + e.getMessage());
            e.printStackTrace();
        }
        return encomenda;
    }
}
