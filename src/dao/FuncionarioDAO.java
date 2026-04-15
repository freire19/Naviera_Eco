package dao;

import model.Funcionario;
import model.PagamentoHistorico;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import util.AppLogger;

/**
 * DAO para funcionarios e operacoes financeiras de folha.
 * Criado a partir do DM033 — extrai SQL de GestaoFuncionariosController.
 */
public class FuncionarioDAO {

    // ========================= CRUD FUNCIONARIOS =========================

    public List<Funcionario> listarTodos(boolean incluirInativos) {
        List<Funcionario> lista = new ArrayList<>();
        String sql = incluirInativos
            ? "SELECT * FROM funcionarios WHERE empresa_id = ? ORDER BY nome"
            : "SELECT * FROM funcionarios WHERE empresa_id = ? AND ativo = true ORDER BY nome";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapResultSet(rs));
            }
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.listarTodos: " + e.getMessage());
        }
        return lista;
    }

    public boolean inserir(Funcionario f) {
        String sql = "INSERT INTO funcionarios (nome, cpf, rg, ctps, telefone, endereco, cargo, salario, " +
                     "data_admissao, data_nascimento, data_inicio_calculo, recebe_decimo_terceiro, " +
                     "is_clt, valor_inss, descontar_inss, ativo, empresa_id) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, true, ?)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindFuncionario(stmt, f, false);
            stmt.setInt(16, DAOUtils.empresaId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.inserir: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Funcionario f) {
        String sql = "UPDATE funcionarios SET nome=?, cpf=?, rg=?, ctps=?, telefone=?, endereco=?, cargo=?, " +
                     "salario=?, data_admissao=?, data_nascimento=?, data_inicio_calculo=?, " +
                     "recebe_decimo_terceiro=?, is_clt=?, valor_inss=?, descontar_inss=? WHERE id=? AND empresa_id=?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindFuncionario(stmt, f, true);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.atualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarDataInicioCalculo(int idFunc, LocalDate novaData) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET data_inicio_calculo = ? WHERE id = ? AND empresa_id = ?")) {
            stmt.setDate(1, Date.valueOf(novaData));
            stmt.setInt(2, idFunc);
            stmt.setInt(3, DAOUtils.empresaId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.atualizarDataInicioCalculo: " + e.getMessage());
            return false;
        }
    }

    public boolean demitir(int idFunc) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET ativo = false WHERE id = ? AND empresa_id = ?")) {
            stmt.setInt(1, idFunc);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.demitir: " + e.getMessage());
            return false;
        }
    }

    // ========================= QUERIES FINANCEIRAS =========================

    public double buscarTotalPagamentosReais(int idFuncionario, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas " +
                     "WHERE funcionario_id = ? AND empresa_id = ? AND is_excluido = false AND data_pagamento >= ? " +
                     "AND (forma_pagamento IS NULL OR forma_pagamento != 'DESCONTO' AND forma_pagamento != 'RETIDO')";
        return queryDouble(sql, idFuncionario, inicio);
    }

    public double buscarTotalEventosRH(int idFuncionario, LocalDate dataReferencia) {
        String sql = "SELECT COALESCE(SUM(valor), 0) as total FROM eventos_rh " +
                     "WHERE funcionario_id = ? AND empresa_id = ? AND data_referencia >= ?";
        return queryDouble(sql, idFuncionario, dataReferencia);
    }

    public double buscarTotalDescontosLegado(int idFuncionario, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas " +
                     "WHERE funcionario_id = ? AND empresa_id = ? AND data_pagamento >= ? " +
                     "AND (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO')";
        return queryDouble(sql, idFuncionario, inicio);
    }

    public boolean existeEventoRH(int idFuncionario, LocalDate dataReferencia, String tipo) {
        String sql = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND empresa_id = ? AND data_referencia >= ? AND tipo = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setDate(3, Date.valueOf(dataReferencia));
            stmt.setString(4, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.existeEventoRH: " + e.getMessage());
        }
        return false;
    }

    public boolean existeDescontoLegado(LocalDate dataReferencia, String termo) {
        String sql = "SELECT COUNT(*) FROM financeiro_saidas WHERE UPPER(descricao) LIKE ? " +
                     "AND empresa_id = ? AND data_pagamento >= ? AND forma_pagamento = 'DESCONTO'";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "%" + termo.toUpperCase() + "%");
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setDate(3, Date.valueOf(dataReferencia));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.existeDescontoLegado: " + e.getMessage());
        }
        return false;
    }

    public boolean existeFaltaNoDia(int idFuncionario, LocalDate data) {
        String sql = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND empresa_id = ? AND data_referencia = ? AND tipo = 'FALTA'";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setDate(3, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.existeFaltaNoDia: " + e.getMessage());
        }
        return false;
    }

    // ========================= LANCAMENTOS =========================

    public boolean lancarEventoRH(int idFuncionario, String tipo, String descricao, double valor,
                                  LocalDate dataEvento, LocalDate dataReferencia) {
        String sql = "INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia, empresa_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setString(2, tipo);
            stmt.setString(3, descricao);
            stmt.setDouble(4, valor);
            stmt.setDate(5, Date.valueOf(dataEvento));
            stmt.setDate(6, Date.valueOf(dataReferencia != null ? dataReferencia : dataEvento));
            stmt.setInt(7, DAOUtils.empresaId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.lancarEventoRH: " + e.getMessage());
            return false;
        }
    }

    public boolean lancarDebito(int idFuncionario, String descricao, double valor,
                                LocalDate dataRef, String formaPagamento) {
        try (Connection con = ConexaoBD.getConnection()) {
            int idViagem = buscarViagemAtiva(con);
            int idCategoria = buscarIdCategoriaFuncionarios(con);

            String sql = "INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, " +
                         "data_pagamento, status, forma_pagamento, id_categoria, id_viagem, funcionario_id, is_excluido, empresa_id) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, descricao.toUpperCase());
                stmt.setDouble(2, valor);
                stmt.setDouble(3, valor);
                stmt.setDate(4, Date.valueOf(dataRef));
                stmt.setDate(5, Date.valueOf(dataRef));
                stmt.setString(6, "PAGO");
                stmt.setString(7, formaPagamento);
                stmt.setInt(8, idCategoria);
                stmt.setInt(9, idViagem);
                stmt.setInt(10, idFuncionario);
                stmt.setInt(11, DAOUtils.empresaId());
                stmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.lancarDebito: " + e.getMessage());
            return false;
        }
    }

    public List<PagamentoHistorico> carregarHistorico(int idFuncionario, int mes, int ano) {
        List<PagamentoHistorico> historico = new ArrayList<>();

        String sqlFin = "SELECT data_pagamento, descricao, valor_pago, forma_pagamento FROM financeiro_saidas " +
                        "WHERE funcionario_id = ? AND empresa_id = ? " +
                        "AND ( (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO') OR is_excluido = false ) " +
                        "AND EXTRACT(MONTH FROM data_pagamento) = ? AND EXTRACT(YEAR FROM data_pagamento) = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlFin)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setInt(3, mes);
            stmt.setInt(4, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String forma = rs.getString("forma_pagamento");
                    String tipo = (forma != null && (forma.equals("DESCONTO") || forma.equals("RETIDO"))) ? "DESCONTO" : "DINHEIRO";
                    // DR202: null check em data_pagamento (pode ser NULL para despesas pendentes)
                    java.sql.Date dpDate = rs.getDate("data_pagamento");
                    if (dpDate == null) continue; // pular registros sem data
                    historico.add(new PagamentoHistorico(
                        dpDate.toLocalDate(),
                        rs.getString("descricao"),
                        rs.getDouble("valor_pago"),
                        tipo
                    ));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.carregarHistorico (saidas): " + e.getMessage());
        }

        String sqlRH = "SELECT data_evento, descricao, valor, tipo FROM eventos_rh " +
                        "WHERE funcionario_id = ? AND empresa_id = ? " +
                        "AND EXTRACT(MONTH FROM data_evento) = ? AND EXTRACT(YEAR FROM data_evento) = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlRH)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setInt(3, mes);
            stmt.setInt(4, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // DR202: null check em data_evento (pode ser NULL em eventos importados)
                    java.sql.Date deDate = rs.getDate("data_evento");
                    if (deDate == null) continue; // pular registros sem data
                    historico.add(new PagamentoHistorico(
                        deDate.toLocalDate(),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        "DESCONTO"
                    ));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.carregarHistorico (eventos): " + e.getMessage());
        }

        return historico;
    }

    // ========================= HELPERS =========================

    private Funcionario mapResultSet(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getInt("id"));
        f.setNome(rs.getString("nome"));
        f.setCargo(rs.getString("cargo"));
        f.setSalario(rs.getBigDecimal("salario"));
        if (rs.getDate("data_admissao") != null) f.setDataAdmissao(rs.getDate("data_admissao").toLocalDate());
        f.setCpf(rs.getString("cpf"));
        f.setRg(rs.getString("rg"));
        f.setCtps(rs.getString("ctps"));
        f.setTelefone(rs.getString("telefone"));
        f.setEndereco(rs.getString("endereco"));
        if (rs.getDate("data_nascimento") != null) f.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());

        // DR214: catch com logging (colunas existem no schema atual — erros nao devem ser silenciados)
        try { if (rs.getDate("data_inicio_calculo") != null) f.setDataInicioCalculo(rs.getDate("data_inicio_calculo").toLocalDate()); }
        catch (SQLException e) { AppLogger.warn("FuncionarioDAO", "Coluna data_inicio_calculo: " + e.getMessage()); }
        try { f.setRecebe13(rs.getBoolean("recebe_decimo_terceiro")); }
        catch (SQLException e) { f.setRecebe13(false); AppLogger.warn("FuncionarioDAO", "Coluna recebe_decimo_terceiro: " + e.getMessage()); }
        try { f.setClt(rs.getBoolean("is_clt")); }
        catch (SQLException e) { f.setClt(false); AppLogger.warn("FuncionarioDAO", "Coluna is_clt: " + e.getMessage()); }
        try { f.setValorInss(rs.getBigDecimal("valor_inss")); }
        catch (SQLException e) { f.setValorInss(java.math.BigDecimal.ZERO); AppLogger.warn("FuncionarioDAO", "Coluna valor_inss: " + e.getMessage()); }
        try { f.setDescontarInss(rs.getBoolean("descontar_inss")); }
        catch (SQLException e) { f.setDescontarInss(false); AppLogger.warn("FuncionarioDAO", "Coluna descontar_inss: " + e.getMessage()); }
        try { f.setAtivo(rs.getBoolean("ativo")); }
        catch (SQLException e) { f.setAtivo(true); AppLogger.warn("FuncionarioDAO", "Coluna ativo: " + e.getMessage()); }

        return f;
    }

    private void bindFuncionario(PreparedStatement stmt, Funcionario f, boolean isUpdate) throws SQLException {
        stmt.setString(1, f.getNome());
        stmt.setString(2, f.getCpf());
        stmt.setString(3, f.getRg());
        stmt.setString(4, f.getCtps());
        stmt.setString(5, f.getTelefone());
        stmt.setString(6, f.getEndereco());
        stmt.setString(7, f.getCargo());
        stmt.setBigDecimal(8, f.getSalario());
        stmt.setDate(9, f.getDataAdmissao() != null ? Date.valueOf(f.getDataAdmissao()) : null);
        stmt.setDate(10, f.getDataNascimento() != null ? Date.valueOf(f.getDataNascimento()) : null);
        stmt.setDate(11, f.getDataInicioCalculo() != null ? Date.valueOf(f.getDataInicioCalculo()) : null);
        stmt.setBoolean(12, f.isRecebe13());
        stmt.setBoolean(13, f.isClt());
        stmt.setBigDecimal(14, f.getValorInss());
        stmt.setBoolean(15, f.isDescontarInss());
        if (isUpdate) {
            stmt.setInt(16, f.getId());
            stmt.setInt(17, DAOUtils.empresaId());
        }
    }

    private double queryDouble(String sql, int idFuncionario, LocalDate data) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.setDate(3, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal v = rs.getBigDecimal("total");
                    return v != null ? v.doubleValue() : 0.0;
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO: " + e.getMessage());
        }
        return 0.0;
    }

    private int buscarViagemAtiva(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("SELECT id_viagem FROM viagens WHERE is_atual = true AND empresa_id = ? LIMIT 1")) {
            ps.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.buscarViagemAtiva: " + e.getMessage());
        }
        return 1;
    }

    private int buscarIdCategoriaFuncionarios(Connection con) {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT id FROM categorias_despesa WHERE (UPPER(nome) LIKE '%FUNCIONARIO%' OR UPPER(nome) LIKE '%FOLHA%' " +
                "OR UPPER(nome) LIKE '%RH%' OR UPPER(nome) LIKE '%PAGAMENTO%') AND empresa_id = ? LIMIT 1")) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            AppLogger.warn("FuncionarioDAO", "Erro SQL em FuncionarioDAO.buscarIdCategoriaFuncionarios: " + e.getMessage());
        }
        return 1;
    }
}
