package dao;

// Multi-tenant imports added automatically

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import gui.util.AppLogger;

public class AgendaDAO {

    // Classe interna para representar a Tarefa (Anotação simples)
    public static class TarefaAgenda {
        private int id;
        private LocalDate data;
        private String descricao;
        private boolean concluida;

        public TarefaAgenda(int id, LocalDate data, String descricao, boolean concluida) {
            this.id = id;
            this.data = data;
            this.descricao = descricao;
            this.concluida = concluida;
        }

        public int getId() { return id; }
        public LocalDate getData() { return data; }
        public String getDescricao() { return descricao; }
        public boolean isConcluida() { return concluida; }
        public void setConcluida(boolean concluida) { this.concluida = concluida; }
        
        @Override
        public String toString() { return descricao; }
    }
    
    // --- NOVO: Classe simples para representar Boletos no Calendário ---
    // #DB027/#DB003: BigDecimal para valor financeiro
    public static class ResumoBoleto {
        public LocalDate vencimento;
        public String descricao;
        public java.math.BigDecimal valor;

        public ResumoBoleto(LocalDate v, String d, java.math.BigDecimal val) {
            this.vencimento = v;
            this.descricao = d;
            this.valor = val != null ? val : java.math.BigDecimal.ZERO;
        }
    }

    public void adicionarAnotacao(LocalDate data, String texto) {
        String sql = "INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES (?, ?, false, ?)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(data));
            stmt.setString(2, texto);
            stmt.setInt(3, DAOUtils.empresaId());
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }

    // Busca apenas as descrições (para as bolinhas do calendário)
    public List<String> buscarAnotacoesPorData(LocalDate data) {
        List<String> notas = new ArrayList<>();
        String sql = "SELECT descricao FROM agenda_anotacoes WHERE empresa_id = ? AND data_evento = ? AND concluida = false";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notas.add(rs.getString("descricao"));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return notas;
    }
    
    /**
     * Busca TODAS as anotacoes nao concluidas do mes inteiro em 1 query (fix DP005).
     * Usa range query em vez de EXTRACT para permitir uso de indice.
     * Retorna Map(data -> lista de descricoes) para uso direto no calendario.
     */
    public Map<LocalDate, List<String>> buscarAnotacoesDoMes(int mes, int ano) {
        Map<LocalDate, List<String>> mapa = new HashMap<>();
        String sql = "SELECT data_evento, descricao FROM agenda_anotacoes " +
                     "WHERE empresa_id = ? AND data_evento >= ? AND data_evento < ? AND concluida = false";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDate inicio = LocalDate.of(ano, mes, 1);
            LocalDate fim = inicio.plusMonths(1);
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, Date.valueOf(inicio));
            stmt.setDate(3, Date.valueOf(fim));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date dtEvento = rs.getDate("data_evento");
                    LocalDate data = dtEvento != null ? dtEvento.toLocalDate() : null;
                    if (data == null) continue;
                    mapa.computeIfAbsent(data, k -> new ArrayList<>()).add(rs.getString("descricao"));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO.buscarAnotacoesDoMes: " + e.getMessage());
        }
        return mapa;
    }

    // --- NOVO MÉTODO: Busca Boletos Pendentes para exibir no Calendário ---
    public List<ResumoBoleto> buscarBoletosPendentesNoMes(int mes, int ano) {
        List<ResumoBoleto> boletos = new ArrayList<>();
        // Range query em vez de EXTRACT para permitir uso de indice (fix DP007)
        String sql = "SELECT data_vencimento, descricao, valor_total FROM financeiro_saidas " +
                     "WHERE forma_pagamento = 'BOLETO' AND status = 'PENDENTE' " +
                     "AND data_vencimento >= ? AND data_vencimento < ?";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocalDate inicio = LocalDate.of(ano, mes, 1);
            stmt.setDate(1, Date.valueOf(inicio));
            stmt.setDate(2, Date.valueOf(inicio.plusMonths(1)));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    java.sql.Date dt = rs.getDate("data_vencimento");
                    if (dt != null) {
                        boletos.add(new ResumoBoleto(
                            dt.toLocalDate(),
                            rs.getString("descricao"),
                            rs.getBigDecimal("valor_total")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return boletos;
    }

    // Busca as tarefas de um dia específico (caso ainda usemos o clique no calendário)
    public List<TarefaAgenda> buscarTarefasCompletasPorData(LocalDate data) {
        List<TarefaAgenda> tarefas = new ArrayList<>();
        String sql = "SELECT id_anotacao, data_evento, descricao, concluida FROM agenda_anotacoes WHERE empresa_id = ? AND data_evento = ? ORDER BY id_anotacao";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            stmt.setDate(2, Date.valueOf(data));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tarefas.add(new TarefaAgenda(
                        rs.getInt("id_anotacao"),
                        rs.getDate("data_evento") != null ? rs.getDate("data_evento").toLocalDate() : null,
                        rs.getString("descricao"),
                        rs.getBoolean("concluida")
                    ));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return tarefas;
    }

    // Busca TODAS as tarefas para a tela de gerenciamento
    public List<TarefaAgenda> buscarTodasTarefas() {
        List<TarefaAgenda> tarefas = new ArrayList<>();
        String sql = "SELECT id_anotacao, data_evento, descricao, concluida FROM agenda_anotacoes WHERE empresa_id = ? ORDER BY data_evento DESC, id_anotacao DESC";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, DAOUtils.empresaId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tarefas.add(new TarefaAgenda(
                        rs.getInt("id_anotacao"),
                        rs.getDate("data_evento") != null ? rs.getDate("data_evento").toLocalDate() : null,
                        rs.getString("descricao"),
                        rs.getBoolean("concluida")
                    ));
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return tarefas;
    }

    public void atualizarStatus(int id, boolean concluida) {
        String sql = "UPDATE agenda_anotacoes SET concluida = ? WHERE id_anotacao = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, concluida);
            stmt.setInt(2, id);
            stmt.setInt(3, DAOUtils.empresaId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }
    
    public void excluirTarefa(int id) {
        String sql = "DELETE FROM agenda_anotacoes WHERE id_anotacao = ? AND empresa_id = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, DAOUtils.empresaId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            AppLogger.warn("AgendaDAO", "Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }
}