package dao;

import model.Embarcacao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

public class EmbarcacaoDAO {

    public Embarcacao inserirOuBuscar(Embarcacao embarcacao) {
        Embarcacao existente = buscarPorNome(embarcacao.getNome());
        if (existente != null) {
            return existente;
        }

        String sql = "INSERT INTO embarcacoes (nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id_embarcacao";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, embarcacao.getNome());
            ps.setString(2, embarcacao.getRegistroCapitania());
            if (embarcacao.getCapacidadePassageiros() != null) {
                ps.setInt(3, embarcacao.getCapacidadePassageiros());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (embarcacao.getCapacidadeCargaToneladas() != null) {
                ps.setBigDecimal(4, embarcacao.getCapacidadeCargaToneladas());
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.setString(5, embarcacao.getObservacoes());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    embarcacao.setId(rs.getLong("id_embarcacao"));
                    return embarcacao;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir/buscar embarcação: " + e.getMessage());
            System.err.println("Erro SQL em EmbarcacaoDAO: " + e.getMessage());
        }
        return null;
    }

    private Embarcacao mapResultSet(ResultSet rs) throws SQLException {
        Embarcacao e = new Embarcacao();
        e.setId(rs.getLong("id_embarcacao"));
        e.setNome(rs.getString("nome"));
        e.setRegistroCapitania(rs.getString("registro_capitania"));
        e.setCapacidadePassageiros(rs.getObject("capacidade_passageiros", Integer.class));
        e.setCapacidadeCargaToneladas(rs.getBigDecimal("capacidade_carga_toneladas"));
        e.setObservacoes(rs.getString("observacoes"));
        return e;
    }

    public Embarcacao buscarPorNome(String nome) {
        String sql = "SELECT id_embarcacao, nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes " +
                     "FROM embarcacoes WHERE nome = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em EmbarcacaoDAO.buscarPorNome: " + e.getMessage());
        }
        return null;
    }

    public List<Embarcacao> listarTodas() {
        List<Embarcacao> lista = new ArrayList<>();
        String sql = "SELECT id_embarcacao, nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes " +
                     "FROM embarcacoes ORDER BY nome";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Erro SQL em EmbarcacaoDAO.listarTodas: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizar(Embarcacao embarcacao) {
        String sql = "UPDATE embarcacoes SET nome = ?, registro_capitania = ?, capacidade_passageiros = ?, capacidade_carga_toneladas = ?, observacoes = ? WHERE id_embarcacao = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, embarcacao.getNome());
            ps.setString(2, embarcacao.getRegistroCapitania());
            if (embarcacao.getCapacidadePassageiros() != null) {
                ps.setInt(3, embarcacao.getCapacidadePassageiros());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            if (embarcacao.getCapacidadeCargaToneladas() != null) {
                ps.setBigDecimal(4, embarcacao.getCapacidadeCargaToneladas());
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.setString(5, embarcacao.getObservacoes());
            ps.setLong(6, embarcacao.getId());
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar embarcação: " + e.getMessage());
            System.err.println("Erro SQL em EmbarcacaoDAO: " + e.getMessage());
        }
        return false;
    }

    public boolean excluir(Long id) {
        String sql = "DELETE FROM embarcacoes WHERE id_embarcacao = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir embarcação: " + e.getMessage());
            System.err.println("Erro SQL em EmbarcacaoDAO: " + e.getMessage());
        }
        return false;
    }
}