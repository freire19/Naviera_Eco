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

    /**
     * Verifica se ja existe um recibo identico (mesma viagem, nome, valor, data) salvo
     * nos ultimos minutosAtras minutos. Usado para prevenir duplicatas acidentais
     * por duplo-clique ou re-clique no botao Salvar e Imprimir.
     */
    public ReciboAvulso buscarIdenticoRecente(int idViagem, String nomePagador, java.math.BigDecimal valor, LocalDate dataEmissao) {
        String sql = "SELECT id_recibo, id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo "
                   + "FROM recibos_avulsos "
                   + "WHERE empresa_id = ? AND id_viagem = ? AND nome_pagador = ? "
                   + "AND valor = ? AND data_emissao = ? "
                   + "ORDER BY id_recibo DESC LIMIT 1";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setInt(2, idViagem);
            stmt.setString(3, nomePagador);
            stmt.setBigDecimal(4, valor);
            stmt.setDate(5, Date.valueOf(dataEmissao));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ColunasRecibo cols = detectarColunas(rs);
                    return montarObjeto(rs, cols);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro ao buscar identico: " + e.getMessage());
        }
        return null;
    }

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
        String sql = "SELECT id_recibo, id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo FROM recibos_avulsos WHERE empresa_id = ? AND id_viagem = ? ORDER BY id_recibo DESC";
        
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setInt(2, idViagem);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ColunasRecibo cols = detectarColunas(rs);
                    lista.add(montarObjeto(rs, cols));
                    while (rs.next()) lista.add(montarObjeto(rs, cols));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    public List<ReciboAvulso> listarPorData(LocalDate data) {
        List<ReciboAvulso> lista = new ArrayList<>();
        String sql = "SELECT id_recibo, id_viagem, nome_pagador, referente_a, valor, data_emissao, tipo_recibo FROM recibos_avulsos WHERE empresa_id = ? AND data_emissao = ? ORDER BY id_recibo DESC";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ColunasRecibo cols = detectarColunas(rs);
                    lista.add(montarObjeto(rs, cols));
                    while (rs.next()) lista.add(montarObjeto(rs, cols));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("ReciboAvulsoDAO", "Erro SQL em ReciboAvulsoDAO: " + e.getMessage());
        }
        return lista;
    }

    // #011 gemeo: ColunasRecibo passado como parametro (antes era 2 ThreadLocals sem cleanup,
    // vazava entre requests no pool JavaFX — mesmo anti-padrao do PassagemDAO ja corrigido).
    private record ColunasRecibo(String colId, boolean temTipoRecibo) {}

    private ColunasRecibo detectarColunas(ResultSet rs) throws SQLException {
        java.sql.ResultSetMetaData meta = rs.getMetaData();
        String colId = "id_recibo";
        boolean temTipo = false;
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i).toLowerCase();
            if (col.equals("id") || col.equals("id_recibo")) colId = meta.getColumnName(i);
            if (col.equals("tipo_recibo")) temTipo = true;
        }
        return new ColunasRecibo(colId, temTipo);
    }

    private ReciboAvulso montarObjeto(ResultSet rs, ColunasRecibo cols) throws SQLException {
        ReciboAvulso r = new ReciboAvulso();
        r.setId(rs.getInt(cols.colId()));
        r.setIdViagem(rs.getInt("id_viagem"));
        r.setNomePagador(rs.getString("nome_pagador"));
        r.setReferenteA(rs.getString("referente_a"));
        r.setValor(rs.getBigDecimal("valor"));
        java.sql.Date dtEmissao = rs.getDate("data_emissao");
        r.setDataEmissao(dtEmissao != null ? dtEmissao.toLocalDate() : null);
        r.setTipoRecibo(cols.temTipoRecibo() ? rs.getString("tipo_recibo") : "PADRAO");
        return r;
    }
}