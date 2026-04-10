package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.TipoPassageiro;

public class TipoPassageiroDAO {

    public boolean inserir(TipoPassageiro tp) {
        String sql = "INSERT INTO tipo_passageiro (nome, idade_min, idade_max, deficiente, gratuito, empresa_id) "
                   + "VALUES (?,?,?,?,?)";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tp.getNome());
            ps.setInt(2, tp.getIdadeMin());
            ps.setInt(3, tp.getIdadeMax());
            ps.setBoolean(4, tp.isDeficiente());
            ps.setBoolean(5, tp.isGratuito());
            ps.executeUpdate();
            return true;

        } catch(SQLException e){
            System.err.println("Erro SQL em TipoPassageiroDAO: " + e.getMessage());
            return false;
        }
    }

    public List<TipoPassageiro> listarTodos() {
        List<TipoPassageiro> lista = new ArrayList<>();
        String sql = "SELECT id, nome, idade_min, idade_max, deficiente, gratuito "
                   + "FROM tipo_passageiro "
                   + "ORDER BY id";
        try(Connection conn = ConexaoBD.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql)) {

            while(rs.next()) {
                TipoPassageiro tp = new TipoPassageiro();
                tp.setId(rs.getInt("id"));
                tp.setNome(rs.getString("nome"));
                tp.setIdadeMin(rs.getInt("idade_min"));
                tp.setIdadeMax(rs.getInt("idade_max"));
                tp.setDeficiente(rs.getBoolean("deficiente"));
                tp.setGratuito(rs.getBoolean("gratuito"));

                lista.add(tp);
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em TipoPassageiroDAO: " + e.getMessage());
        }
        return lista;
    }

    // Retorna só os nomes
    public List<String> listarNomes() {
        List<String> nomes = new ArrayList<>();
        for(TipoPassageiro tp : listarTodos()) {
            nomes.add(tp.getNome());
        }
        return nomes;
    }

    // Busca ID pelo nome
    public int buscarIdPorNome(String nome) {
        String sql = "SELECT id FROM tipo_passageiro WHERE nome=? LIMIT 1";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) return rs.getInt("id");
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em TipoPassageiroDAO: " + e.getMessage());
        }
        return 0;
    }

    public String buscarNomePorId(int id) {
        String sql = "SELECT nome FROM tipo_passageiro WHERE id=?";
        try(Connection conn = ConexaoBD.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) return rs.getString("nome");
            }
        } catch(SQLException e){
            System.err.println("Erro SQL em TipoPassageiroDAO: " + e.getMessage());
        }
        return null;
    }
}
