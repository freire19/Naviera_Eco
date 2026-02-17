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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return lista;
    }

    private ReciboAvulso montarObjeto(ResultSet rs) throws SQLException {
        ReciboAvulso r = new ReciboAvulso();
        // Tenta ler ID (seja 'id' ou 'id_recibo' para evitar erros de banco)
        try { r.setId(rs.getInt("id")); } catch (Exception e) { 
            try { r.setId(rs.getInt("id_recibo")); } catch(Exception ex) {} 
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