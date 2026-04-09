package dao;

import model.Funcionario;
import model.PagamentoHistorico;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para funcionarios e operacoes financeiras de folha.
 * Criado a partir do DM033 — extrai SQL de GestaoFuncionariosController.
 */
public class FuncionarioDAO {

    // ========================= CRUD FUNCIONARIOS =========================

    public List<Funcionario> listarTodos(boolean incluirInativos) {
        List<Funcionario> lista = new ArrayList<>();
        String sql = incluirInativos
            ? "SELECT * FROM funcionarios ORDER BY nome"
            : "SELECT * FROM funcionarios WHERE ativo = true ORDER BY nome";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.listarTodos: " + e.getMessage());
        }
        return lista;
    }

    public boolean inserir(Funcionario f) {
        String sql = "INSERT INTO funcionarios (nome, cpf, rg, ctps, telefone, endereco, cargo, salario, " +
                     "data_admissao, data_nascimento, data_inicio_calculo, recebe_decimo_terceiro, " +
                     "is_clt, valor_inss, descontar_inss, ativo) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, true)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindFuncionario(stmt, f, false);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.inserir: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Funcionario f) {
        String sql = "UPDATE funcionarios SET nome=?, cpf=?, rg=?, ctps=?, telefone=?, endereco=?, cargo=?, " +
                     "salario=?, data_admissao=?, data_nascimento=?, data_inicio_calculo=?, " +
                     "recebe_decimo_terceiro=?, is_clt=?, valor_inss=?, descontar_inss=? WHERE id=?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            bindFuncionario(stmt, f, true);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.atualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarDataInicioCalculo(int idFunc, LocalDate novaData) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET data_inicio_calculo = ? WHERE id = ?")) {
            stmt.setDate(1, Date.valueOf(novaData));
            stmt.setInt(2, idFunc);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.atualizarDataInicioCalculo: " + e.getMessage());
            return false;
        }
    }

    public boolean demitir(int idFunc) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET ativo = false WHERE id = ?")) {
            stmt.setInt(1, idFunc);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.demitir: " + e.getMessage());
            return false;
        }
    }

    // ========================= QUERIES FINANCEIRAS =========================

    public double buscarTotalPagamentosReais(int idFuncionario, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas " +
                     "WHERE funcionario_id = ? AND is_excluido = false AND data_pagamento >= ? " +
                     "AND (forma_pagamento IS NULL OR forma_pagamento != 'DESCONTO' AND forma_pagamento != 'RETIDO')";
        return queryDouble(sql, idFuncionario, inicio);
    }

    public double buscarTotalEventosRH(int idFuncionario, LocalDate dataReferencia) {
        String sql = "SELECT COALESCE(SUM(valor), 0) as total FROM eventos_rh " +
                     "WHERE funcionario_id = ? AND data_referencia >= ?";
        return queryDouble(sql, idFuncionario, dataReferencia);
    }

    public double buscarTotalDescontosLegado(int idFuncionario, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas " +
                     "WHERE funcionario_id = ? AND data_pagamento >= ? " +
                     "AND (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO')";
        return queryDouble(sql, idFuncionario, inicio);
    }

    public boolean existeEventoRH(int idFuncionario, LocalDate dataReferencia, String tipo) {
        String sql = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND data_referencia >= ? AND tipo = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setDate(2, Date.valueOf(dataReferencia));
            stmt.setString(3, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.existeEventoRH: " + e.getMessage());
        }
        return false;
    }

    public boolean existeDescontoLegado(LocalDate dataReferencia, String termo) {
        String sql = "SELECT COUNT(*) FROM financeiro_saidas WHERE UPPER(descricao) LIKE ? " +
                     "AND data_pagamento >= ? AND forma_pagamento = 'DESCONTO'";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "%" + termo.toUpperCase() + "%");
            stmt.setDate(2, Date.valueOf(dataReferencia));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.existeDescontoLegado: " + e.getMessage());
        }
        return false;
    }

    public boolean existeFaltaNoDia(int idFuncionario, LocalDate data) {
        String sql = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND data_referencia = ? AND tipo = 'FALTA'";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.existeFaltaNoDia: " + e.getMessage());
        }
        return false;
    }

    // ========================= LANCAMENTOS =========================

    public boolean lancarEventoRH(int idFuncionario, String tipo, String descricao, double valor,
                                  LocalDate dataEvento, LocalDate dataReferencia) {
        String sql = "INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setString(2, tipo);
            stmt.setString(3, descricao);
            stmt.setDouble(4, valor);
            stmt.setDate(5, Date.valueOf(dataEvento));
            stmt.setDate(6, Date.valueOf(dataReferencia != null ? dataReferencia : dataEvento));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.lancarEventoRH: " + e.getMessage());
            return false;
        }
    }

    public boolean lancarDebito(int idFuncionario, String descricao, double valor,
                                LocalDate dataRef, String formaPagamento) {
        try (Connection con = ConexaoBD.getConnection()) {
            int idViagem = buscarViagemAtiva(con);
            int idCategoria = buscarIdCategoriaFuncionarios(con);

            String sql = "INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, " +
                         "data_pagamento, status, forma_pagamento, id_categoria, id_viagem, funcionario_id, is_excluido) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false)";
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
                stmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.lancarDebito: " + e.getMessage());
            return false;
        }
    }

    public List<PagamentoHistorico> carregarHistorico(int idFuncionario, int mes, int ano) {
        List<PagamentoHistorico> historico = new ArrayList<>();

        String sqlFin = "SELECT data_pagamento, descricao, valor_pago, forma_pagamento FROM financeiro_saidas " +
                        "WHERE funcionario_id = ? " +
                        "AND ( (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO') OR is_excluido = false ) " +
                        "AND EXTRACT(MONTH FROM data_pagamento) = ? AND EXTRACT(YEAR FROM data_pagamento) = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlFin)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, mes);
            stmt.setInt(3, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String forma = rs.getString("forma_pagamento");
                    String tipo = (forma != null && (forma.equals("DESCONTO") || forma.equals("RETIDO"))) ? "DESCONTO" : "DINHEIRO";
                    historico.add(new PagamentoHistorico(
                        rs.getDate("data_pagamento").toLocalDate(),
                        rs.getString("descricao"),
                        rs.getDouble("valor_pago"),
                        tipo
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.carregarHistorico (saidas): " + e.getMessage());
        }

        String sqlRH = "SELECT data_evento, descricao, valor, tipo FROM eventos_rh " +
                        "WHERE funcionario_id = ? " +
                        "AND EXTRACT(MONTH FROM data_evento) = ? AND EXTRACT(YEAR FROM data_evento) = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlRH)) {
            stmt.setInt(1, idFuncionario);
            stmt.setInt(2, mes);
            stmt.setInt(3, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historico.add(new PagamentoHistorico(
                        rs.getDate("data_evento").toLocalDate(),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        "DESCONTO"
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.carregarHistorico (eventos): " + e.getMessage());
        }

        return historico;
    }

    // ========================= HELPERS =========================

    private Funcionario mapResultSet(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario();
        f.setId(rs.getInt("id"));
        f.setNome(rs.getString("nome"));
        f.setCargo(rs.getString("cargo"));
        f.setSalario(rs.getDouble("salario"));
        if (rs.getDate("data_admissao") != null) f.setDataAdmissao(rs.getDate("data_admissao").toLocalDate());
        f.setCpf(rs.getString("cpf"));
        f.setRg(rs.getString("rg"));
        f.setCtps(rs.getString("ctps"));
        f.setTelefone(rs.getString("telefone"));
        f.setEndereco(rs.getString("endereco"));
        if (rs.getDate("data_nascimento") != null) f.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());

        try { if (rs.getDate("data_inicio_calculo") != null) f.setDataInicioCalculo(rs.getDate("data_inicio_calculo").toLocalDate()); }
        catch (Exception e) { /* coluna opcional */ }
        try { f.setRecebe13(rs.getBoolean("recebe_decimo_terceiro")); } catch (Exception e) { f.setRecebe13(false); }
        try { f.setClt(rs.getBoolean("is_clt")); } catch (Exception e) { f.setClt(false); }
        try { f.setValorInss(rs.getDouble("valor_inss")); } catch (Exception e) { f.setValorInss(0.0); }
        try { f.setDescontarInss(rs.getBoolean("descontar_inss")); } catch (Exception e) { f.setDescontarInss(false); }
        try { f.setAtivo(rs.getBoolean("ativo")); } catch (Exception e) { f.setAtivo(true); }

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
        stmt.setDouble(8, f.getSalario());
        stmt.setDate(9, f.getDataAdmissao() != null ? Date.valueOf(f.getDataAdmissao()) : null);
        stmt.setDate(10, f.getDataNascimento() != null ? Date.valueOf(f.getDataNascimento()) : null);
        stmt.setDate(11, f.getDataInicioCalculo() != null ? Date.valueOf(f.getDataInicioCalculo()) : null);
        stmt.setBoolean(12, f.isRecebe13());
        stmt.setBoolean(13, f.isClt());
        stmt.setDouble(14, f.getValorInss());
        stmt.setBoolean(15, f.isDescontarInss());
        if (isUpdate) stmt.setInt(16, f.getId());
    }

    private double queryDouble(String sql, int idFuncionario, LocalDate data) {
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idFuncionario);
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal v = rs.getBigDecimal("total");
                    return v != null ? v.doubleValue() : 0.0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO: " + e.getMessage());
        }
        return 0.0;
    }

    private int buscarViagemAtiva(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("SELECT id_viagem FROM viagens WHERE is_atual = true LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.buscarViagemAtiva: " + e.getMessage());
        }
        return 1;
    }

    private int buscarIdCategoriaFuncionarios(Connection con) {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT id FROM categorias WHERE UPPER(nome) LIKE '%FUNCIONARIO%' OR UPPER(nome) LIKE '%FOLHA%' " +
                "OR UPPER(nome) LIKE '%RH%' OR UPPER(nome) LIKE '%PAGAMENTO%' LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("Erro SQL em FuncionarioDAO.buscarIdCategoriaFuncionarios: " + e.getMessage());
        }
        return 1;
    }
}
