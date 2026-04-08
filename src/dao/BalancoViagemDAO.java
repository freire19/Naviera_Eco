package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.DadosBalancoViagem;
import model.ItemResumoBalanco;

public class BalancoViagemDAO {

    private Connection connection;

    public BalancoViagemDAO(Connection connection) {
        this.connection = connection;
    }

    public DadosBalancoViagem buscarBalancoDaViagem(int idViagem) {
        DadosBalancoViagem dados = new DadosBalancoViagem();
        
        try {
            // =================================================================================
            // 1. PASSAGENS (Join com ROTAS para pegar nomes)
            // =================================================================================
            // Atenção: Assumindo que a tabela de rotas tem colunas 'id', 'origem' e 'destino'
            String sqlPassagem = "SELECT r.origem, r.destino, COUNT(*) as qtd, COALESCE(SUM(p.valor_total), 0) as total " +
                                 "FROM passagens p " +
                                 "LEFT JOIN rotas r ON p.id_rota = r.id " + // Cruzando dados
                                 "WHERE p.id_viagem = ? " +
                                 "GROUP BY r.origem, r.destino";
            
            try (PreparedStatement stmt = connection.prepareStatement(sqlPassagem)) {
                stmt.setInt(1, idViagem);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String origem = rs.getString("origem");
                    String destino = rs.getString("destino");
                    
                    // Se não achar a rota (null), coloca "?"
                    String rotaDesc = (origem != null ? origem : "?") + " / " + (destino != null ? destino : "?");
                    
                    double valor = rs.getDouble("total");
                    dados.adicionarItem(new ItemResumoBalanco("Passagens", rotaDesc, rs.getInt("qtd"), valor));
                    dados.somarPassagens(valor);
                }
            } catch (SQLException e) {
                System.err.println("Erro SQL Passagens: " + e.getMessage());
                dados.marcarIncompleto("Passagens", e.getMessage());
            }

            // =================================================================================
            // 2. ENCOMENDAS (Coluna 'total_a_pagar' e 'rota')
            // =================================================================================
            String sqlEncomenda = "SELECT rota, COUNT(*) as qtd, COALESCE(SUM(total_a_pagar), 0) as total " +
                                  "FROM encomendas WHERE id_viagem = ? GROUP BY rota";
            
            try (PreparedStatement stmt = connection.prepareStatement(sqlEncomenda)) {
                stmt.setInt(1, idViagem);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String rota = rs.getString("rota");
                    if (rota == null || rota.isEmpty()) rota = "Geral";
                    
                    double valor = rs.getDouble("total");
                    dados.adicionarItem(new ItemResumoBalanco("Encomendas", rota, rs.getInt("qtd"), valor));
                    dados.somarEncomendas(valor);
                }
            } catch (SQLException e) {
                System.err.println("Erro SQL Encomendas: " + e.getMessage());
                dados.marcarIncompleto("Encomendas", e.getMessage());
            }
            
            // =================================================================================
            // 3. FRETES (Coluna 'valor_frete_calculado' e 'rota_temp')
            // =================================================================================
            String sqlFrete = "SELECT rota_temp, COUNT(*) as qtd, COALESCE(SUM(valor_frete_calculado), 0) as total " +
                              "FROM fretes WHERE id_viagem = ? GROUP BY rota_temp";
            
            try (PreparedStatement stmt = connection.prepareStatement(sqlFrete)) {
                stmt.setInt(1, idViagem);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String rota = rs.getString("rota_temp");
                    if (rota == null || rota.isEmpty()) rota = "Geral";
                    
                    double valor = rs.getDouble("total");
                    dados.adicionarItem(new ItemResumoBalanco("Fretes", rota, rs.getInt("qtd"), valor));
                    dados.somarFretes(valor);
                }
            } catch (SQLException e) {
                System.err.println("Erro SQL Fretes: " + e.getMessage());
                dados.marcarIncompleto("Fretes", e.getMessage());
            }

            // =================================================================================
            // 4. SAÍDAS (GASTOS) - Mantido igual (valor_total já estava certo)
            // =================================================================================
            String sqlSaidas = "SELECT c.nome, SUM(d.valor_total) as total " +
                               "FROM financeiro_saidas d " +
                               "JOIN categorias_despesa c ON d.id_categoria = c.id " +
                               "WHERE d.id_viagem = ? " +
                               "GROUP BY c.nome";
            
            double somaSaidas = 0;
            try (PreparedStatement stmt = connection.prepareStatement(sqlSaidas)) {
                stmt.setInt(1, idViagem);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String categoria = rs.getString("nome");
                    double valor = rs.getDouble("total");
                    dados.getSaidasPorCategoria().put(categoria, valor);
                    somaSaidas += valor;
                }
            }
            dados.setTotalSaidas(somaSaidas);

        } catch (SQLException e) {
            System.err.println("Erro SQL em BalancoViagemDAO: " + e.getMessage());
        }
        
        return dados;
    }
}