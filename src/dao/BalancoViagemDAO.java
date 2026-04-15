package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.DadosBalancoViagem;
import model.ItemResumoBalanco;
import gui.util.AppLogger;

public class BalancoViagemDAO {

    private final Connection connectionExterna;

    /** Construtor com conexao externa (para compartilhar com caller). */
    public BalancoViagemDAO(Connection connection) {
        this.connectionExterna = connection;
    }

    /** #005: Construtor sem conexao — usa ConexaoBD internamente por metodo. */
    public BalancoViagemDAO() {
        this.connectionExterna = null;
    }

    private Connection obterConexao() throws SQLException {
        return connectionExterna != null ? connectionExterna : ConexaoBD.getConnection();
    }

    private void fecharSeLocal(Connection conn) {
        if (connectionExterna == null && conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }


    public DadosBalancoViagem buscarBalancoDaViagem(int idViagem) {
        DadosBalancoViagem dados = new DadosBalancoViagem();
        Connection connection = null;

        try {
            connection = obterConexao();

            // 1. PASSAGENS
            String sqlPassagem = "SELECT r.origem, r.destino, COUNT(*) as qtd, COALESCE(SUM(p.valor_total), 0) as total " +
                                 "FROM passagens p " +
                                 "LEFT JOIN rotas r ON p.id_rota = r.id " +
                                 "WHERE p.id_viagem = ? AND p.empresa_id = ? " +
                                 "GROUP BY r.origem, r.destino";

            try (PreparedStatement stmt = connection.prepareStatement(sqlPassagem)) {
                stmt.setInt(1, idViagem);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String origem = rs.getString("origem");
                        String destino = rs.getString("destino");
                        String rotaDesc = (origem != null ? origem : "?") + " / " + (destino != null ? destino : "?");
                        BigDecimal valor = DAOUtils.nvl(rs.getBigDecimal("total"));
                        dados.adicionarItem(new ItemResumoBalanco("Passagens", rotaDesc, rs.getInt("qtd"), valor));
                        dados.somarPassagens(valor);
                    }
                }
            } catch (SQLException e) {
                AppLogger.warn("BalancoViagemDAO", "Erro SQL Passagens: " + e.getMessage());
                dados.marcarIncompleto("Passagens", e.getMessage());
            }

            // 2. ENCOMENDAS
            String sqlEncomenda = "SELECT rota, COUNT(*) as qtd, COALESCE(SUM(total_a_pagar), 0) as total " +
                                  "FROM encomendas WHERE id_viagem = ? AND empresa_id = ? GROUP BY rota";

            try (PreparedStatement stmt = connection.prepareStatement(sqlEncomenda)) {
                stmt.setInt(1, idViagem);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String rota = rs.getString("rota");
                        if (rota == null || rota.isEmpty()) rota = "Geral";
                        BigDecimal valor = DAOUtils.nvl(rs.getBigDecimal("total"));
                        dados.adicionarItem(new ItemResumoBalanco("Encomendas", rota, rs.getInt("qtd"), valor));
                        dados.somarEncomendas(valor);
                    }
                }
            } catch (SQLException e) {
                AppLogger.warn("BalancoViagemDAO", "Erro SQL Encomendas: " + e.getMessage());
                dados.marcarIncompleto("Encomendas", e.getMessage());
            }

            // 3. FRETES
            String sqlFrete = "SELECT rota_temp, COUNT(*) as qtd, COALESCE(SUM(valor_frete_calculado), 0) as total " +
                              "FROM fretes WHERE id_viagem = ? AND empresa_id = ? GROUP BY rota_temp";

            try (PreparedStatement stmt = connection.prepareStatement(sqlFrete)) {
                stmt.setInt(1, idViagem);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String rota = rs.getString("rota_temp");
                        if (rota == null || rota.isEmpty()) rota = "Geral";
                        BigDecimal valor = DAOUtils.nvl(rs.getBigDecimal("total"));
                        dados.adicionarItem(new ItemResumoBalanco("Fretes", rota, rs.getInt("qtd"), valor));
                        dados.somarFretes(valor);
                    }
                }
            } catch (SQLException e) {
                AppLogger.warn("BalancoViagemDAO", "Erro SQL Fretes: " + e.getMessage());
                dados.marcarIncompleto("Fretes", e.getMessage());
            }

            // 4. SAIDAS
            String sqlSaidas = "SELECT c.nome, SUM(d.valor_total) as total " +
                               "FROM financeiro_saidas d " +
                               "JOIN categorias_despesa c ON d.id_categoria = c.id " +
                               "WHERE d.id_viagem = ? AND d.empresa_id = ? " +
                               "GROUP BY c.nome";

            // DR216: try-catch individual para query de Saidas (consistente com Passagens/Encomendas/Fretes)
            BigDecimal somaSaidas = BigDecimal.ZERO;
            try (PreparedStatement stmt = connection.prepareStatement(sqlSaidas)) {
                stmt.setInt(1, idViagem);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String categoria = rs.getString("nome");
                        BigDecimal valor = DAOUtils.nvl(rs.getBigDecimal("total"));
                        dados.getSaidasPorCategoria().put(categoria, valor);
                        somaSaidas = somaSaidas.add(valor);
                    }
                }
            } catch (SQLException e) {
                AppLogger.warn("BalancoViagemDAO", "Erro SQL Saidas: " + e.getMessage());
                dados.marcarIncompleto("Saidas", e.getMessage());
            }
            dados.setTotalSaidas(somaSaidas);

        } catch (SQLException e) {
            AppLogger.warn("BalancoViagemDAO", "Erro SQL em BalancoViagemDAO: " + e.getMessage());
        } finally {
            fecharSeLocal(connection);
        }

        return dados;
    }
}
