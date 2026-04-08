package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    public static class ResumoBoleto {
        public LocalDate vencimento;
        public String descricao;
        public double valor;

        public ResumoBoleto(LocalDate v, String d, double val) {
            this.vencimento = v;
            this.descricao = d;
            this.valor = val;
        }
    }

    public void adicionarAnotacao(LocalDate data, String texto) {
        String sql = "INSERT INTO agenda_anotacoes (data_evento, descricao, concluida) VALUES (?, ?, false)";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(data));
            stmt.setString(2, texto);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }

    // Busca apenas as descrições (para as bolinhas do calendário)
    public List<String> buscarAnotacoesPorData(LocalDate data) {
        List<String> notas = new ArrayList<>();
        String sql = "SELECT descricao FROM agenda_anotacoes WHERE data_evento = ? AND concluida = false";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(data));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notas.add(rs.getString("descricao"));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return notas;
    }
    
    // --- NOVO MÉTODO: Busca Boletos Pendentes para exibir no Calendário ---
    public List<ResumoBoleto> buscarBoletosPendentesNoMes(int mes, int ano) {
        List<ResumoBoleto> boletos = new ArrayList<>();
        // Pega tudo que é BOLETO e está PENDENTE naquele mês/ano
        String sql = "SELECT data_vencimento, descricao, valor_total FROM financeiro_saidas " +
                     "WHERE forma_pagamento = 'BOLETO' AND status = 'PENDENTE' " +
                     "AND EXTRACT(MONTH FROM data_vencimento) = ? " +
                     "AND EXTRACT(YEAR FROM data_vencimento) = ?";
                     
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, mes);
            stmt.setInt(2, ano);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    java.sql.Date dt = rs.getDate("data_vencimento");
                    if (dt != null) {
                        boletos.add(new ResumoBoleto(
                            dt.toLocalDate(),
                            rs.getString("descricao"),
                            rs.getDouble("valor_total")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return boletos;
    }

    // Busca as tarefas de um dia específico (caso ainda usemos o clique no calendário)
    public List<TarefaAgenda> buscarTarefasCompletasPorData(LocalDate data) {
        List<TarefaAgenda> tarefas = new ArrayList<>();
        String sql = "SELECT id_anotacao, data_evento, descricao, concluida FROM agenda_anotacoes WHERE data_evento = ? ORDER BY id_anotacao";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(data));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tarefas.add(new TarefaAgenda(
                    rs.getInt("id_anotacao"),
                    rs.getDate("data_evento").toLocalDate(),
                    rs.getString("descricao"),
                    rs.getBoolean("concluida")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return tarefas;
    }

    // Busca TODAS as tarefas para a tela de gerenciamento
    public List<TarefaAgenda> buscarTodasTarefas() {
        List<TarefaAgenda> tarefas = new ArrayList<>();
        String sql = "SELECT id_anotacao, data_evento, descricao, concluida FROM agenda_anotacoes ORDER BY data_evento DESC, id_anotacao DESC";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tarefas.add(new TarefaAgenda(
                    rs.getInt("id_anotacao"),
                    rs.getDate("data_evento").toLocalDate(),
                    rs.getString("descricao"),
                    rs.getBoolean("concluida")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
        return tarefas;
    }

    public void atualizarStatus(int id, boolean concluida) {
        String sql = "UPDATE agenda_anotacoes SET concluida = ? WHERE id_anotacao = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, concluida);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }
    
    public void excluirTarefa(int id) {
        String sql = "DELETE FROM agenda_anotacoes WHERE id_anotacao = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro SQL em AgendaDAO: " + e.getMessage());
        }
    }
}