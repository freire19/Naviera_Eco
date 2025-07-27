package dao;

import model.Embarcacao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // Adicionado para Objects.equals se for usar aqui, não estritamente necessário para os erros atuais.

/**
 * DAO para CRUD de embarcações.
 * Usa a tabela 'embarcacoes' com colunas:
 * id_embarcacao, nome, registro_capitania, capacidade_passageiros,
 * capacidade_carga_toneladas, observacoes
 */
public class EmbarcacaoDAO {

    /**
     * Se existir embarcação com o mesmo nome, retorna o objeto existente.
     * Caso contrário, insere e retorna o próprio objeto com o ID preenchido.
     */
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
                    embarcacao.setId((Long) rs.getObject("id_embarcacao")); // CORRIGIDO AQUI: getObject e casting para Long
                    return embarcacao;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir/buscar embarcação: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca embarcação pelo nome (igual exato). Retorna null se não encontrar.
     */
    public Embarcacao buscarPorNome(String nome) {
        String sql = "SELECT id_embarcacao, nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes " +
                     "FROM embarcacoes WHERE nome = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Embarcacao e = new Embarcacao();
                    e.setId((Long) rs.getObject("id_embarcacao")); // CORRIGIDO AQUI
                    e.setNome(rs.getString("nome"));
                    e.setRegistroCapitania(rs.getString("registro_capitania"));
                    e.setCapacidadePassageiros(rs.getObject("capacidade_passageiros", Integer.class));
                    e.setCapacidadeCargaToneladas(rs.getBigDecimal("capacidade_carga_toneladas"));
                    e.setObservacoes(rs.getString("observacoes"));
                    return e;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar embarcação por nome: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lista todas as embarcações ordenadas pelo nome.
     */
    public List<Embarcacao> listarTodas() {
        List<Embarcacao> lista = new ArrayList<>();
        String sql = "SELECT id_embarcacao, nome, registro_capitania, capacidade_passageiros, capacidade_carga_toneladas, observacoes " +
                     "FROM embarcacoes ORDER BY nome";
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Embarcacao e = new Embarcacao();
                e.setId((Long) rs.getObject("id_embarcacao")); // CORRIGIDO AQUI
                e.setNome(rs.getString("nome"));
                e.setRegistroCapitania(rs.getString("registro_capitania"));
                e.setCapacidadePassageiros(rs.getObject("capacidade_passageiros", Integer.class));
                e.setCapacidadeCargaToneladas(rs.getBigDecimal("capacidade_carga_toneladas"));
                e.setObservacoes(rs.getString("observacoes"));
                lista.add(e);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todas as embarcações: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    // Se desejar implementar atualizar/excluir, basta criar métodos update e delete aqui.
    // Exemplo de atualizar, assumindo que id não é alterável:
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
            ps.setObject(6, embarcacao.getId()); // ID é Long
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar embarcação: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean excluir(Long id) { // Agora espera Long
        String sql = "DELETE FROM embarcacoes WHERE id_embarcacao = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id); // Usa setObject para Long/null
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao excluir embarcação: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}