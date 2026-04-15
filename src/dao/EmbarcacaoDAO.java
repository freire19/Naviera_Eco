package dao;

import model.Embarcacao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

public class EmbarcacaoDAO {

    /**
     * DL004: INSERT ON CONFLICT para atomicidade.
     * NOTA: UNIQUE constraint agora e (empresa_id, nome) — ON CONFLICT adaptado.
     */
    public Embarcacao inserirOuBuscar(Embarcacao embarcacao) {
        String sql = "INSERT INTO embarcacoes (nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes, empresa_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (empresa_id, nome) DO NOTHING " +
                     "RETURNING id_embarcacao";
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
            ps.setInt(6, DAOUtils.empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    embarcacao.setId(rs.getLong("id_embarcacao"));
                    return embarcacao;
                }
            }
            return buscarPorNome(embarcacao.getNome());
        } catch (SQLException e) {
            AppLogger.warn("EmbarcacaoDAO", "Erro ao inserir/buscar embarcacao: " + e.getMessage());
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
                     "FROM embarcacoes WHERE nome = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            AppLogger.warn("EmbarcacaoDAO", "Erro SQL em EmbarcacaoDAO.buscarPorNome: " + e.getMessage());
        }
        return null;
    }

    public List<Embarcacao> listarTodas() {
        List<Embarcacao> lista = new ArrayList<>();
        String sql = "SELECT id_embarcacao, nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes " +
                     "FROM embarcacoes WHERE empresa_id = ? ORDER BY nome";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            AppLogger.warn("EmbarcacaoDAO", "Erro SQL em EmbarcacaoDAO.listarTodas: " + e.getMessage());
        }
        return lista;
    }

    public boolean atualizar(Embarcacao embarcacao) {
        String sql = "UPDATE embarcacoes SET nome = ?, registro_capitania = ?, capacidade_passageiros = ?, capacidade_carga_toneladas = ?, observacoes = ? WHERE id_embarcacao = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, embarcacao.getNome());
            ps.setString(2, embarcacao.getRegistroCapitania());
            if (embarcacao.getCapacidadePassageiros() != null) ps.setInt(3, embarcacao.getCapacidadePassageiros());
            else ps.setNull(3, Types.INTEGER);
            if (embarcacao.getCapacidadeCargaToneladas() != null) ps.setBigDecimal(4, embarcacao.getCapacidadeCargaToneladas());
            else ps.setNull(4, Types.NUMERIC);
            ps.setString(5, embarcacao.getObservacoes());
            ps.setLong(6, embarcacao.getId());
            ps.setInt(7, DAOUtils.empresaId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("EmbarcacaoDAO", "Erro SQL em EmbarcacaoDAO: " + e.getMessage());
        }
        return false;
    }

    public boolean excluir(Long id) {
        String sql = "DELETE FROM embarcacoes WHERE id_embarcacao = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setInt(2, DAOUtils.empresaId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                AppLogger.warn("EmbarcacaoDAO", "Embarcacao id=" + id + " nao pode ser excluida: possui viagens vinculadas.");
                return false;
            }
            AppLogger.warn("EmbarcacaoDAO", "Erro ao excluir embarcacao: " + e.getMessage());
            return false;
        }
    }
}
