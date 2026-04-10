package dao;

import model.Tarifa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TarifaDAO {

    public Tarifa buscarTarifaPorRotaETipo(long idRota, int idTipoPassagem) {
        Tarifa tarifa = null;
        String sql = "SELECT id_tarifa, id_rota, id_tipo_passagem, valor_transporte, valor_cargas, valor_alimentacao, valor_desconto FROM tarifas WHERE empresa_id = ? AND id_rota = ? AND id_tipo_passagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idRota);
            stmt.setInt(2, idTipoPassagem);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tarifa = new Tarifa();
                    tarifa.setId(rs.getInt("id_tarifa"));
                    tarifa.setRotaId(rs.getLong("id_rota"));
                    tarifa.setTipoPassageiroId(rs.getInt("id_tipo_passagem"));
                    tarifa.setValorTransporte(rs.getBigDecimal("valor_transporte"));
                    tarifa.setValorCargas(rs.getBigDecimal("valor_cargas"));
                    tarifa.setValorAlimentacao(rs.getBigDecimal("valor_alimentacao"));
                    tarifa.setValorDesconto(rs.getBigDecimal("valor_desconto"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em TarifaDAO: " + e.getMessage());
        }
        return tarifa;
    }

    public boolean inserir(Tarifa tarifa) {
        String sql = "INSERT INTO tarifas (id_rota, id_tipo_passagem, valor_transporte, valor_alimentacao, valor_cargas, valor_desconto) VALUES (?,?,?,?,?,?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, tarifa.getRotaId());
            ps.setInt(2, tarifa.getTipoPassageiroId());
            ps.setBigDecimal(3, DAOUtils.nvl(tarifa.getValorTransporte()));
            ps.setBigDecimal(4, DAOUtils.nvl(tarifa.getValorAlimentacao()));
            ps.setBigDecimal(5, DAOUtils.nvl(tarifa.getValorCargas()));
            ps.setBigDecimal(6, DAOUtils.nvl(tarifa.getValorDesconto()));
            if (ps.executeUpdate() > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tarifa.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em TarifaDAO: " + e.getMessage());
        }
        return false;
    }
    
    public boolean atualizar(Tarifa tarifa) {
        String sql = "UPDATE tarifas SET valor_transporte = ?, valor_alimentacao = ?, valor_cargas = ?, valor_desconto = ? WHERE id_tarifa = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, DAOUtils.nvl(tarifa.getValorTransporte()));
            ps.setBigDecimal(2, DAOUtils.nvl(tarifa.getValorAlimentacao()));
            ps.setBigDecimal(3, DAOUtils.nvl(tarifa.getValorCargas()));
            ps.setBigDecimal(4, DAOUtils.nvl(tarifa.getValorDesconto()));
            ps.setInt(5, tarifa.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em TarifaDAO: " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(int idTarifa) {
        String sql = "DELETE FROM tarifas WHERE id_tarifa = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTarifa);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erro SQL em TarifaDAO: " + e.getMessage());
            return false;
        }
    }

    public List<Tarifa> listarTodos() {
        List<Tarifa> tarifas = new ArrayList<>();
        String sql = "SELECT t.id_tarifa, t.id_rota, t.id_tipo_passagem, t.valor_transporte, t.valor_cargas, t.valor_alimentacao, t.valor_desconto, " +
                     "r.origem || ' - ' || r.destino AS nome_rota, " +
                     "atp.nome_tipo_passagem AS nome_tipo_passageiro " +
                     "FROM tarifas t " +
                     "JOIN rotas r ON t.id_rota = r.id " +
                     "JOIN aux_tipos_passagem atp ON t.id_tipo_passagem = atp.id_tipo_passagem " +
                     "ORDER BY r.origem, atp.nome_tipo_passagem";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Tarifa tarifa = new Tarifa();
                tarifa.setId(rs.getInt("id_tarifa"));
                tarifa.setRotaId(rs.getLong("id_rota"));
                tarifa.setTipoPassageiroId(rs.getInt("id_tipo_passagem"));
                tarifa.setValorTransporte(rs.getBigDecimal("valor_transporte"));
                tarifa.setValorCargas(rs.getBigDecimal("valor_cargas"));
                tarifa.setValorAlimentacao(rs.getBigDecimal("valor_alimentacao"));
                tarifa.setValorDesconto(rs.getBigDecimal("valor_desconto"));
                tarifa.setNomeRota(rs.getString("nome_rota"));
                tarifa.setNomeTipoPassageiro(rs.getString("nome_tipo_passageiro"));
                tarifas.add(tarifa);
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em TarifaDAO: " + e.getMessage());
        }
        return tarifas;
    }

    // #019: whitelist de tabelas permitidas (previne SQL injection via nome de tabela)
    private static final List<String> TABELAS_PERMITIDAS = List.of(
        "aux_tipos_passagem", "aux_agentes", "aux_acomodacoes", "aux_horarios_saida",
        "aux_formas_pagamento", "caixas", "rotas"
    );

    public Integer obterIdAuxiliar(Connection conn, String tabela, String colunaNome, String colunaId, String valorNome) throws SQLException {
        if (valorNome == null || valorNome.trim().isEmpty()) return null;
        if (!TABELAS_PERMITIDAS.contains(tabela)) {
            throw new IllegalArgumentException("Tabela nao permitida em TarifaDAO: " + tabela);
        }
        String sql = "SELECT " + colunaId + " FROM " + tabela + " WHERE " + colunaNome + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valorNome);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(colunaId);
                }
            }
        }
        return null;
    }

    public Integer obterIdRotaPelaOrigemDestino(Connection conn, String origem, String destino) throws SQLException {
        if (origem == null || origem.trim().isEmpty() || destino == null || destino.trim().isEmpty()) return null;
        String sql = "SELECT id FROM rotas WHERE origem = ? AND destino = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, origem);
            stmt.setString(2, destino);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }
}