package dao;

// Multi-tenant imports added automatically

import java.sql.Connection;
// tenant filter
import static dao.DAOUtils.empresaId;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.ReciboAvulso;
import util.AppLogger;

public class ReciboAvulsoDAO {

    public boolean salvar(ReciboAvulso r) {
        String sql = "INSERT INTO recibos_avulsos (id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, r.getIdViagem());
            stmt.setString(2, r.getNomePagador());
            stmt.setString(3, r.getReferenteA());
            // DL063: usar setBigDecimal para valor financeiro
            stmt.setBigDecimal(4, r.getValor());
            stmt.setDate(5, Date.valueOf(r.getDataEmissao()));
            stmt.setString(6, r.getTipoRecibo());
            stmt.setInt(7, DAOUtils.empresaId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return false;
    }

    public List<ReciboAvulso> listarPorViagem(int idViagem) {
        List<ReciboAvulso> lista = new ArrayList<>();
        // Ordena do mais recente para o mais antigo
        String sql = "SELECT * FROM recibos_avulsos WHERE empresa_id = ? AND id_viagem = ? ORDER BY id_recibo DESC"; 
        
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setInt(2, idViagem);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    detectarColunas(rs);
                    lista.add(montarObjeto(rs));
                    while (rs.next()) lista.add(montarObjeto(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    public List<ReciboAvulso> listarPorData(LocalDate data) {
        List<ReciboAvulso> lista = new ArrayList<>();
        String sql = "SELECT * FROM recibos_avulsos WHERE empresa_id = ? AND data_emissao = ? ORDER BY id_recibo DESC";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    detectarColunas(rs);
                    lista.add(montarObjeto(rs));
                    while (rs.next()) lista.add(montarObjeto(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    // DR208: ThreadLocal em vez de campos de instancia (thread-safe)
    private final ThreadLocal<String> colIdTL = ThreadLocal.withInitial(() -> "id_recibo");
    private final ThreadLocal<Boolean> temTipoReciboTL = ThreadLocal.withInitial(() -> false);

    /** Detecta colunas uma vez por query (chamado antes do while loop). */
    private void detectarColunas(ResultSet rs) throws SQLException {
        java.sql.ResultSetMetaData meta = rs.getMetaData();
        colIdTL.set("id_recibo");
        temTipoReciboTL.set(false);
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i).toLowerCase();
            if (col.equals("id") || col.equals("id_recibo")) colIdTL.set(meta.getColumnName(i));
            if (col.equals("tipo_recibo")) temTipoReciboTL.set(true);
        }
    }

    private ReciboAvulso montarObjeto(ResultSet rs) throws SQLException {
        ReciboAvulso r = new ReciboAvulso();
        r.setId(rs.getInt(colIdTL.get()));
        r.setIdViagem(rs.getInt("id_viagem"));
        r.setNomePagador(rs.getString("nome_pagador"));
        r.setReferenteA(rs.getString("referente_a"));
        r.setValor(rs.getBigDecimal("valor"));
        java.sql.Date dtEmissao = rs.getDate("data_emissao");
        r.setDataEmissao(dtEmissao != null ? dtEmissao.toLocalDate() : null);
        r.setTipoRecibo(temTipoReciboTL.get() ? rs.getString("tipo_recibo") : "PADRAO");
        return r;
    }
}