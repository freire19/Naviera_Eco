package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.ReciboAvulso;

public class ReciboAvulsoDAO {

    public void salvar(ReciboAvulso r) throws SQLException {
        String sql = "INSERT INTO recibos_avulsos (id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, r.getIdViagem());
            stmt.setString(2, r.getNomePagador());
            stmt.setString(3, r.getReferenteA());
            stmt.setDouble(4, r.getValor());
            stmt.setDate(5, Date.valueOf(r.getDataEmissao()));
            stmt.setString(6, r.getTipoRecibo());
            stmt.executeUpdate();
        }
    }

    public List<ReciboAvulso> listarPorViagem(int idViagem) {
        List<ReciboAvulso> lista = new ArrayList<>();
        // Ordena do mais recente para o mais antigo
        String sql = "SELECT * FROM recibos_avulsos WHERE id_viagem = ? ORDER BY 1 DESC"; 
        
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idViagem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(montarObjeto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    public List<ReciboAvulso> listarPorData(LocalDate data) {
        List<ReciboAvulso> lista = new ArrayList<>();
        String sql = "SELECT * FROM recibos_avulsos WHERE data_emissao = ? ORDER BY 1 DESC";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(data));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lista.add(montarObjeto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    private ReciboAvulso montarObjeto(ResultSet rs) throws SQLException {
        ReciboAvulso r = new ReciboAvulso();
        // Determina nome da coluna ID no ResultSet
        java.sql.ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i);
            if ("id".equals(col) || "id_recibo".equals(col)) {
                r.setId(rs.getInt(col));
                break;
            }
        }
        
        r.setIdViagem(rs.getInt("id_viagem"));
        r.setNomePagador(rs.getString("nome_pagador"));
        r.setReferenteA(rs.getString("referente_a"));
        r.setValor(rs.getDouble("valor"));
        r.setDataEmissao(rs.getDate("data_emissao").toLocalDate());
        try { r.setTipoRecibo(rs.getString("tipo_recibo")); } catch (Exception e) { r.setTipoRecibo("PADRAO"); }
        return r;
    }
}