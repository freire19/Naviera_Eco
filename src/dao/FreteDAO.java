package dao;

import model.Frete;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import gui.util.AppLogger;

public class FreteDAO {

    /**
     * Busca fretes no banco de dados com base em múltiplos filtros.
     * @param idViagem O ID da viagem para filtrar. Se for null, busca de todas as viagens.
     * @param termoBuscaItem Um texto para pesquisar na descrição dos itens do frete.
     * @param dataInicio Data de emissão inicial para o filtro de período.
     * @param dataFim Data de emissão final para o filtro de período.
     * @return Uma lista de objetos Frete.
     */
    public List<Frete> buscarFretes(Long idViagem, String termoBuscaItem, LocalDate dataInicio, LocalDate dataFim) {
        List<Frete> fretes = new ArrayList<>();
        
        // Alterei o SQL para fazer JOIN com a tabela de viagens e pegar a data_viagem
        StringBuilder sql = new StringBuilder(
            // DP010: subquery correlacionada substituida por LEFT JOIN agrupado
            "SELECT f.id_frete, f.numero_frete, f.id_viagem, f.remetente_nome_temp AS remetente_nome, " +
            "f.destinatario_nome_temp AS destinatario_nome, f.rota_temp AS rota, " +
            "f.data_emissao, f.valor_total_itens AS valor_nominal, f.valor_devedor, f.valor_pago, " +
            "f.conferente_temp AS conferente, f.status_frete, " +
            "v.data_viagem, " +
            "COALESCE(fiv.total_volumes, 0) AS total_volumes " +
            "FROM fretes f " +
            "LEFT JOIN viagens v ON f.id_viagem = v.id_viagem " +
            "LEFT JOIN (SELECT id_frete, SUM(quantidade) AS total_volumes FROM frete_itens GROUP BY id_frete) fiv ON fiv.id_frete = f.id_frete "
        );

        List<Object> parametros = new ArrayList<>();
        List<String> condicoes = new ArrayList<>();

        // Multi-tenant: sempre filtrar por empresa
        condicoes.add("f.empresa_id = ?");
        parametros.add(DAOUtils.empresaId());

        if (idViagem != null && idViagem > 0) {
            condicoes.add("f.id_viagem = ?");
            parametros.add(idViagem);
        }

        if (termoBuscaItem != null && !termoBuscaItem.trim().isEmpty()) {
            condicoes.add("EXISTS (SELECT 1 FROM frete_itens fi WHERE fi.id_frete = f.id_frete AND fi.nome_item_ou_id_produto ILIKE ?)");
            parametros.add("%" + termoBuscaItem.trim() + "%");
        }
        
        if (dataInicio != null) {
            condicoes.add("f.data_emissao >= ?");
            parametros.add(Date.valueOf(dataInicio));
        }
        
        if (dataFim != null) {
            condicoes.add("f.data_emissao <= ?");
            parametros.add(Date.valueOf(dataFim));
        }

        if (!condicoes.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", condicoes));
        }

        sql.append(" ORDER BY f.id_frete DESC");

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Frete frete = new Frete();
                    frete.setIdFrete(rs.getLong("id_frete"));
                    frete.setNumeroFrete(rs.getString("numero_frete"));
                    frete.setIdViagem(rs.getLong("id_viagem"));
                    frete.setNomeRemetente(rs.getString("remetente_nome"));
                    frete.setNomeDestinatario(rs.getString("destinatario_nome"));
                    frete.setNomeRota(rs.getString("rota"));
                    
                    // Preenchendo a data da viagem
                    Date dataViagemSql = rs.getDate("data_viagem");
                    if (dataViagemSql != null) {
                        frete.setDataViagem(dataViagemSql.toLocalDate());
                    }

                    Date dataEmissaoSql = rs.getDate("data_emissao");
                    if (dataEmissaoSql != null) {
                        frete.setDataEmissao(dataEmissaoSql.toLocalDate());
                    }
                    
                    frete.setValorNominal(rs.getBigDecimal("valor_nominal"));
                    frete.setValorDevedor(rs.getBigDecimal("valor_devedor"));
                    frete.setValorPago(rs.getBigDecimal("valor_pago"));
                    frete.setNomeConferente(rs.getString("conferente"));
                    frete.setStatus(rs.getString("status_frete"));
                    frete.setTotalVolumes(rs.getInt("total_volumes"));
                    
                    fretes.add(frete);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FreteDAO", "Erro SQL em FreteDAO: " + e.getMessage());
        }
        return fretes;
    }
    
    /**
     * Lista todos os fretes de uma viagem específica.
     * @param idViagem O ID da viagem para a qual os fretes serão listados.
     * @return Uma lista de objetos Frete da viagem especificada.
     */
    public List<Frete> listarPorViagem(long idViagem) {
        return buscarFretes(idViagem, null, null, null);
    }

    /**
     * Exclui um frete e seus itens em transação.
     * @param idFrete o id_frete a excluir
     * @return true se excluído com sucesso
     */
    public boolean excluir(long idFrete) {
        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstItems = conn.prepareStatement(
                    "DELETE FROM frete_itens WHERE id_frete = ?")) {
                pstItems.setLong(1, idFrete);
                pstItems.executeUpdate();
            }

            boolean ok;
            try (PreparedStatement pstFrete = conn.prepareStatement(
                    "DELETE FROM fretes WHERE id_frete = ? AND empresa_id = ?")) {
                pstFrete.setLong(1, idFrete);
                pstFrete.setInt(2, DAOUtils.empresaId());
                ok = pstFrete.executeUpdate() > 0;
            }

            conn.commit();
            return ok;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { /* ignorado */ } }
            AppLogger.warn("FreteDAO", "Erro SQL em FreteDAO.excluir: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignorado */ }
            }
        }
    }
}