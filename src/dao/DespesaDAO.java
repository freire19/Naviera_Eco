package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import gui.util.AppLogger;

/**
 * DAO para financeiro_saidas e categorias_despesa.
 * Centraliza SQL que estava duplicado em FinanceiroSaidaController e CadastroBoletoController.
 */
public class DespesaDAO {

    // -------------------------------------------------------------------------
    // financeiro_saidas
    // -------------------------------------------------------------------------

    /**
     * Busca despesas com filtros opcionais.
     * Retorna colunas: id, data_vencimento, descricao, cat_nome, forma_pagamento,
     * valor_total, status, is_excluido.
     */
    public List<Map<String, Object>> buscarDespesas(int idViagem, String categoriaFiltro,
                                                     String formaFiltro, LocalDate dataFiltro,
                                                     boolean apenasNaoBoletos) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT s.id, s.data_vencimento, s.descricao, c.nome AS cat_nome, " +
            "s.forma_pagamento, s.valor_total, s.status, s.is_excluido " +
            "FROM financeiro_saidas s " +
            "LEFT JOIN categorias_despesa c ON s.id_categoria = c.id " +
            "WHERE s.empresa_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(DAOUtils.empresaId());

        if (idViagem > 0) {
            sql.append(" AND s.id_viagem = ?");
            params.add(idViagem);
        }
        if (apenasNaoBoletos) {
            sql.append(" AND (s.forma_pagamento != 'BOLETO' OR s.status = 'PAGO')");
        }
        if (dataFiltro != null) {
            sql.append(" AND s.data_vencimento = ?");
            params.add(Date.valueOf(dataFiltro));
        }
        if (categoriaFiltro != null && !categoriaFiltro.isEmpty()
                && !categoriaFiltro.equalsIgnoreCase("Todas")
                && !categoriaFiltro.equalsIgnoreCase("Todas as Categorias")) {
            sql.append(" AND c.nome = ?");
            params.add(categoriaFiltro);
        }
        if (formaFiltro != null && !formaFiltro.isEmpty()
                && !formaFiltro.equalsIgnoreCase("Todas")) {
            sql.append(" AND s.forma_pagamento = ?");
            params.add(formaFiltro);
        }
        sql.append(" ORDER BY s.data_vencimento DESC");

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                else if (p instanceof Date) stmt.setDate(i + 1, (Date) p);
                else stmt.setString(i + 1, p.toString());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("data_vencimento", rs.getDate("data_vencimento"));
                    row.put("descricao", rs.getString("descricao"));
                    row.put("cat_nome", rs.getString("cat_nome"));
                    row.put("forma_pagamento", rs.getString("forma_pagamento"));
                    row.put("valor_total", rs.getBigDecimal("valor_total"));
                    row.put("status", rs.getString("status"));
                    row.put("is_excluido", rs.getBoolean("is_excluido"));
                    resultado.add(row);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.buscarDespesas: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Insere uma nova despesa em financeiro_saidas.
     */
    public boolean inserirDespesa(String descricao, java.math.BigDecimal valorTotal,
                                   java.math.BigDecimal valorPago, LocalDate vencimento,
                                   LocalDate dataPagamento, String status, String formaPagamento,
                                   int idCategoria, int idViagem, Integer funcionarioId) {
        String sql = "INSERT INTO financeiro_saidas " +
                "(descricao, valor_total, valor_pago, data_vencimento, data_pagamento, " +
                "status, forma_pagamento, id_categoria, id_viagem, empresa_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, descricao);
            stmt.setBigDecimal(2, valorTotal);
            stmt.setBigDecimal(3, valorPago != null ? valorPago : java.math.BigDecimal.ZERO);
            stmt.setDate(4, vencimento != null ? Date.valueOf(vencimento) : null);
            stmt.setDate(5, dataPagamento != null ? Date.valueOf(dataPagamento) : null);
            stmt.setString(6, status);
            stmt.setString(7, formaPagamento);
            stmt.setInt(8, idCategoria);
            stmt.setInt(9, idViagem);
            stmt.setInt(10, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.inserirDespesa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza o status de uma despesa (dar baixa).
     */
    public boolean atualizarStatus(int id, String status, LocalDate dataPagamento) {
        String sql = "UPDATE financeiro_saidas SET status = ?, data_pagamento = ? WHERE id = ? AND empresa_id = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setDate(2, dataPagamento != null ? Date.valueOf(dataPagamento) : null);
            stmt.setInt(3, id);
            stmt.setInt(4, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.atualizarStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * Dar baixa em boleto: atualiza status, forma_pagamento e valor_pago.
     */
    public boolean darBaixaBoleto(int id, String formaPagamento) {
        String sql = "UPDATE financeiro_saidas SET status='PAGO', forma_pagamento=?, " +
                     "data_pagamento=CURRENT_DATE, valor_pago=valor_total WHERE id=? AND empresa_id=?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, formaPagamento);
            stmt.setInt(2, id);
            stmt.setInt(3, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.darBaixaBoleto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft delete com auditoria (para FinanceiroSaidaController).
     * Usa a conexão fornecida (dentro de transação existente).
     */
    public void excluirComAuditoria(Connection con, int id, String motivo,
                                     String responsaveis, int idViagem,
                                     String detalheValor) throws SQLException {
        String sqlUpdate = "UPDATE financeiro_saidas SET is_excluido = true, motivo_exclusao = ? WHERE id = ? AND empresa_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sqlUpdate)) {
            stmt.setString(1, motivo.toUpperCase());
            stmt.setInt(2, id);
            stmt.setInt(3, DAOUtils.empresaId());
            stmt.executeUpdate();
        }

        String sqlAudit = "INSERT INTO auditoria_financeiro (acao, usuario, motivo, detalhe_valor, id_viagem, empresa_id) " +
                          "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sqlAudit)) {
            stmt.setString(1, "EXCLUSAO_DESPESA");
            stmt.setString(2, responsaveis);
            stmt.setString(3, motivo.toUpperCase());
            stmt.setString(4, detalheValor);
            stmt.setInt(5, idViagem);
            stmt.setInt(6, DAOUtils.empresaId());
            stmt.executeUpdate();
        }
    }

    /**
     * Exclusão de boleto com registro de auditoria (hard delete, usa tabela auditoria_financeiro).
     */
    public boolean excluirBoleto(int id, String descricao, String valorFormatado,
                                  String vencimento, String nomeUsuario) {
        try (Connection con = ConexaoBD.getConnection()) {
            try (PreparedStatement audit = con.prepareStatement(
                    "INSERT INTO auditoria_financeiro (tipo_operacao, descricao, usuario_solicitante, data_hora, detalhe_valor, empresa_id) " +
                    "VALUES (?, ?, ?, NOW(), ?, ?)")) {
                audit.setString(1, "EXCLUSAO_BOLETO");
                audit.setString(2, "Exclusao de boleto: " + descricao);
                audit.setString(3, nomeUsuario);
                audit.setString(4, "Valor: " + valorFormatado + " | Vencimento: " + vencimento);
                audit.setInt(5, DAOUtils.empresaId());
                audit.executeUpdate();
            }
            try (PreparedStatement s = con.prepareStatement("DELETE FROM financeiro_saidas WHERE id=? AND empresa_id=?")) {
                s.setInt(1, id);
                s.setInt(2, DAOUtils.empresaId());
                return s.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.excluirBoleto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna o id_viagem de uma despesa específica.
     */
    public int buscarIdViagemDaDespesa(int idDespesa, Connection con) {
        String sql = "SELECT id_viagem FROM financeiro_saidas WHERE id = ? AND empresa_id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idDespesa);
            stmt.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id_viagem");
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.buscarIdViagemDaDespesa: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Retorna info de viagem (datas) associada a uma despesa (para auditoria).
     */
    public String buscarInfoViagem(int idDespesa, Connection con) {
        String info = "VIAGEM N/D";
        String sql = "SELECT v.data_viagem, v.data_chegada FROM financeiro_saidas s " +
                     "JOIN viagens v ON s.id_viagem = v.id_viagem WHERE s.id = ? AND s.empresa_id = ?";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM");
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idDespesa);
            stmt.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Date dtIda = rs.getDate("data_viagem");
                    Date dtVolta = rs.getDate("data_chegada");
                    String sIda = (dtIda != null) ? sdf.format(dtIda) : "?";
                    String sVolta = (dtVolta != null) ? sdf.format(dtVolta) : "?";
                    info = "REF. VIAGEM: " + sIda + " A " + sVolta;
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.buscarInfoViagem: " + e.getMessage());
        }
        return info;
    }

    // -------------------------------------------------------------------------
    // categorias_despesa
    // -------------------------------------------------------------------------

    /** Lista todos os nomes de categorias ordenados alfabeticamente. */
    public List<String> listarCategorias() {
        List<String> lista = new ArrayList<>();
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT nome FROM categorias_despesa WHERE empresa_id = ? ORDER BY nome")) {
            pstmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) lista.add(rs.getString(1));
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.listarCategorias: " + e.getMessage());
        }
        return lista;
    }

    /** Retorna o id de uma categoria pelo nome (case-insensitive). */
    public int buscarIdCategoria(String nome) {
        if (nome == null || nome.isEmpty()) return 1;
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT id FROM categorias_despesa WHERE nome = ? AND empresa_id = ?")) {
            stmt.setString(1, nome);
            stmt.setInt(2, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.buscarIdCategoria: " + e.getMessage());
        }
        return 1;
    }

    /**
     * Busca ou cria categoria. Retorna o id.
     */
    public int buscarOuCriarCategoria(String nome) throws SQLException {
        if (nome == null || nome.isEmpty()) return 1;
        String nomeUpper = nome.toUpperCase();
        try (Connection con = ConexaoBD.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(
                    "SELECT id FROM categorias_despesa WHERE nome = ? AND empresa_id = ?")) {
                stmt.setString(1, nomeUpper);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO categorias_despesa (nome, empresa_id) VALUES (?, ?) RETURNING id")) {
                stmt.setString(1, nomeUpper);
                stmt.setInt(2, DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    /** Insere nova categoria. */
    public boolean inserirCategoria(String nome) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO categorias_despesa (nome, empresa_id) VALUES (?, ?)")) {
            stmt.setString(1, nome.toUpperCase());
            stmt.setInt(2, DAOUtils.empresaId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.inserirCategoria: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Boletos (CadastroBoletoController)
    // -------------------------------------------------------------------------

    /**
     * Busca boletos de financeiro_saidas (forma_pagamento = 'BOLETO').
     */
    public List<Map<String, Object>> buscarBoletos(int idViagem, LocalDate dataFiltro) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT id, data_vencimento, descricao, numero_parcela, total_parcelas, " +
            "valor_total, status FROM financeiro_saidas " +
            "WHERE forma_pagamento = 'BOLETO' AND empresa_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(DAOUtils.empresaId());

        if (idViagem > 0) {
            sql.append(" AND id_viagem = ?");
            params.add(idViagem);
        }
        if (dataFiltro != null) {
            sql.append(" AND data_vencimento = ?");
            params.add(Date.valueOf(dataFiltro));
        }
        sql.append(" ORDER BY data_vencimento ASC");

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                else if (p instanceof Date) stmt.setDate(i + 1, (Date) p);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("data_vencimento", rs.getDate("data_vencimento"));
                    row.put("descricao", rs.getString("descricao"));
                    row.put("numero_parcela", rs.getInt("numero_parcela"));
                    row.put("total_parcelas", rs.getInt("total_parcelas"));
                    row.put("valor_total", rs.getDouble("valor_total"));
                    row.put("status", rs.getString("status"));
                    resultado.add(row);
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("DespesaDAO", "Erro SQL em DespesaDAO.buscarBoletos: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Insere parcelas de boleto em batch com agenda.
     * Recebe listas paralelas de vencimentos, valores e numeros de parcela.
     */
    public boolean inserirBoletoEmBatch(String descricao, List<LocalDate> vencimentos,
                                         List<java.math.BigDecimal> valores,
                                         int totalParcelas, String observacoes,
                                         int idCategoria, int idViagem) throws SQLException {
        String sqlFin = "INSERT INTO financeiro_saidas (descricao, valor_total, data_vencimento, status, " +
                        "forma_pagamento, id_categoria, numero_parcela, total_parcelas, observacoes, id_viagem, empresa_id) " +
                        "VALUES (?, ?, ?, 'PENDENTE', 'BOLETO', ?, ?, ?, ?, ?, ?)";
        String sqlAgenda = "INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES (?, ?, false, ?)";

        try (Connection con = ConexaoBD.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmtFin = con.prepareStatement(sqlFin);
                 PreparedStatement stmtAgenda = con.prepareStatement(sqlAgenda)) {

                for (int i = 0; i < vencimentos.size(); i++) {
                    LocalDate venc = vencimentos.get(i);
                    java.math.BigDecimal valor = valores.get(i);

                    stmtFin.setString(1, descricao);
                    stmtFin.setBigDecimal(2, valor);
                    stmtFin.setDate(3, Date.valueOf(venc));
                    stmtFin.setInt(4, idCategoria);
                    stmtFin.setInt(5, i + 1);
                    stmtFin.setInt(6, totalParcelas);
                    stmtFin.setString(7, observacoes);
                    stmtFin.setInt(8, idViagem);
                    stmtFin.setInt(9, DAOUtils.empresaId());
                    stmtFin.addBatch();

                    stmtAgenda.setDate(1, Date.valueOf(venc));
                    stmtAgenda.setString(2, "VENCIMENTO BOLETO: " + descricao +
                            " (" + (i + 1) + "/" + totalParcelas + ") - R$ " +
                            String.format("%.2f", valor));
                    stmtAgenda.setInt(3, DAOUtils.empresaId());
                    stmtAgenda.addBatch();
                }

                stmtFin.executeBatch();
                stmtAgenda.executeBatch();
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
