package gui;

import dao.ConexaoBD;
import gui.util.PermissaoService;
import dao.PassagemDAO;
import dao.AuxiliaresDAO;
import model.Passagem;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class FinanceiroPassagensController {

    @FXML private ComboBox<OpcaoViagem> cmbViagem;
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkApenasDevedores;
    @FXML private Label lblTotalPendente;
    @FXML private Button btnSair;

    @FXML private TableView<PassagemFinanceiro> tabela;
    @FXML private TableColumn<PassagemFinanceiro, String> colBilhete;
    @FXML private TableColumn<PassagemFinanceiro, String> colData; 
    @FXML private TableColumn<PassagemFinanceiro, String> colPassageiro;
    @FXML private TableColumn<PassagemFinanceiro, String> colDestino;
    @FXML private TableColumn<PassagemFinanceiro, String> colTotal;
    @FXML private TableColumn<PassagemFinanceiro, String> colPago;
    @FXML private TableColumn<PassagemFinanceiro, String> colRestante;
    @FXML private TableColumn<PassagemFinanceiro, String> colStatus;

    @FXML
    public void initialize() {
        if (!PermissaoService.isFinanceiro()) { PermissaoService.exigirFinanceiro("Financeiro Passagens"); return; }
        configurarTabela();
        cmbViagem.valueProperty().addListener((obs, oldVal, newVal) -> carregarDadosEmBackground());

        // DR010: carrega viagens em background
        Thread bg = new Thread(() -> carregarComboViagens());
        bg.setDaemon(true);
        bg.start();
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> carregarDadosEmBackground());
        chkApenasDevedores.selectedProperty().addListener((obs, oldVal, newVal) -> carregarDadosEmBackground());
        
        try {
            tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception e) { System.err.println("Erro em FinanceiroPassagensController.initialize (CSS): " + e.getMessage()); }
    }

    @FXML
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void verHistoricoEstornos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HistoricoEstornosPassagens.fxml")); 
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Histórico de Auditoria - Estornos (Passagens)");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.showAndWait();
        } catch(Exception e) { 
            e.printStackTrace(); 
            alert("Erro ao abrir histórico: " + e.getMessage()); 
        }
    }

    public void setViagemInicial(int idViagem) {
        for (OpcaoViagem op : cmbViagem.getItems()) {
            if (op.id == idViagem) {
                cmbViagem.setValue(op);
                break;
            }
        }
    }

    private void configurarTabela() {
        colBilhete.setCellValueFactory(new PropertyValueFactory<>("bilhete"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataViagem"));
        colPassageiro.setCellValueFactory(new PropertyValueFactory<>("passageiro"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatado"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("pagoFormatado"));
        colRestante.setCellValueFactory(new PropertyValueFactory<>("restanteFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(column -> new TableCell<PassagemFinanceiro, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                }
            }
        });
    }

    private void carregarComboViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        String sql = "SELECT id_viagem, descricao, data_viagem FROM viagens ORDER BY id_viagem DESC LIMIT 30";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            lista.add(new OpcaoViagem(0, "Todas as Viagens"));

            while (rs.next()) {
                String label = rs.getString("descricao");
                java.sql.Date dtSaida = rs.getDate("data_viagem");
                
                if (label == null || label.trim().isEmpty()) {
                    label = "Viagem";
                }

                if (dtSaida != null) {
                    label += " (" + sdf.format(dtSaida) + ")";
                }
                lista.add(new OpcaoViagem(rs.getInt("id_viagem"), label));
            }
            ObservableList<OpcaoViagem> finalLista = lista;
            javafx.application.Platform.runLater(() -> cmbViagem.setItems(finalLista));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void carregarDados() {
        if (cmbViagem.getValue() == null) return;
        int idViagem = cmbViagem.getValue().id;
        
        ObservableList<PassagemFinanceiro> lista = FXCollections.observableArrayList();
        java.math.BigDecimal somaPendente = java.math.BigDecimal.ZERO;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id_passagem, p.numero_bilhete, v.data_viagem, ");
        sql.append("pas.nome_passageiro, ");
        sql.append("COALESCE(r.destino, 'N/A') as destino_nome, ");
        sql.append("p.valor_total, p.valor_pago, p.status_passagem ");
        sql.append("FROM passagens p ");
        sql.append("JOIN viagens v ON p.id_viagem = v.id_viagem ");
        sql.append("JOIN passageiros pas ON p.id_passageiro = pas.id_passageiro ");
        sql.append("LEFT JOIN rotas r ON p.id_rota = r.id ");
        sql.append("WHERE 1=1 ");

        // D003: parametriza idViagem
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (idViagem > 0) { sql.append(" AND p.id_viagem = ?"); params.add(idViagem); }

        if (chkApenasDevedores.isSelected()) {
            sql.append(" AND (p.valor_devedor > 0 OR p.valor_pago < p.valor_total) ");
        }

        String busca = txtBusca.getText().toLowerCase();
        if (!busca.isEmpty()) {
            sql.append(" AND (LOWER(pas.nome_passageiro) LIKE ? OR CAST(p.numero_bilhete AS TEXT) LIKE ?) ");
            params.add("%" + busca + "%");
            params.add("%" + busca + "%");
        }

        sql.append(" ORDER BY p.id_passagem DESC");

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                else stmt.setString(i + 1, p.toString());
            }
            }

            ResultSet rs = stmt.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            
            while (rs.next()) {
                java.math.BigDecimal total = rs.getBigDecimal("valor_total");
                java.math.BigDecimal pago = rs.getBigDecimal("valor_pago");
                if (total == null) total = java.math.BigDecimal.ZERO;
                if (pago == null) pago = java.math.BigDecimal.ZERO;
                java.math.BigDecimal devendo = total.subtract(pago);

                String dataFmt = "";
                if(rs.getDate("data_viagem") != null) dataFmt = sdf.format(rs.getDate("data_viagem"));

                String statusBD = rs.getString("status_passagem");
                if (statusBD == null) statusBD = "PENDENTE";

                lista.add(new PassagemFinanceiro(
                    rs.getInt("id_passagem"),
                    rs.getString("numero_bilhete"),
                    dataFmt,
                    rs.getString("nome_passageiro"),
                    rs.getString("destino_nome"),
                    total, pago, statusBD
                ));

                if(devendo.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) somaPendente = somaPendente.add(devendo);
            }
            tabela.setItems(lista);
            lblTotalPendente.setText(String.format("R$ %,.2f", somaPendente));

        } catch (SQLException e) { 
            e.printStackTrace(); 
            alert("Erro ao buscar dados: " + e.getMessage());
        }
    }

    private void carregarDadosEmBackground() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                carregarDados();
                return null;
            }
        };
        task.setOnSucceeded(event -> tabela.refresh());
        new Thread(task).start();
    }

    // =================================================================================
    // >>> LÓGICA DE DAR BAIXA (MODERNA - MÚLTIPLOS PAGAMENTOS) <<<
    // =================================================================================
    @FXML
    public void darBaixa() {
        PassagemFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alert("Selecione uma passagem na tabela para dar baixa.");
            return;
        }
        if (selecionada.getRestante().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) {
            alert("Esta passagem já está quitada!");
            return;
        }

        try {
            // 1. Busca os dados COMPLETOS da passagem no banco (incluindo o que já foi pago de Pix/Dinheiro/Cartão)
            Passagem passagemCompleta = buscarPassagemCompletaPorId(selecionada.getId());
            
            if (passagemCompleta == null) {
                alert("Erro ao carregar dados originais da passagem.");
                return;
            }

            // 2. Guarda os valores ANTIGOS (para somar depois)
            BigDecimal antigoDinheiro = passagemCompleta.getValorPagamentoDinheiro();
            BigDecimal antigoPix = passagemCompleta.getValorPagamentoPix();
            BigDecimal antigoCartao = passagemCompleta.getValorPagamentoCartao();
            
            // 3. Abre a janela de Pagamento Misto
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/FinalizarPagamentoPassagem.fxml"));
            Parent root = loader.load();
            
            FinalizarPagamentoPassagemController controller = loader.getController();
            
            // ATENÇÃO: Passamos a passagem para a tela, mas zeramos os campos de pagamento individuais na tela
            // para que o usuário digite APENAS o que está pagando AGORA.
            controller.setDadosPagamento(null, passagemCompleta); // null stage, setaremos depois se precisar
            
            Stage stage = new Stage();
            stage.setTitle("Quitar Passagem - Múltiplos Pagamentos");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Re-seta o stage no controller para ele poder fechar
            controller.setDadosPagamento(stage, passagemCompleta);
            
            stage.showAndWait();
            
            // 4. Se confirmou, processa a SOMA dos pagamentos
            if (controller.isConfirmado()) {
                Passagem passagemRetorno = controller.getPassagemAtualizada();
                
                // Pega o que foi digitado na tela (o pagamento NOVO)
                BigDecimal novoDinheiro = passagemRetorno.getValorPagamentoDinheiro();
                BigDecimal novoPix = passagemRetorno.getValorPagamentoPix();
                BigDecimal novoCartao = passagemRetorno.getValorPagamentoCartao();
                
                // Soma com o ANTIGO
                passagemRetorno.setValorPagamentoDinheiro(antigoDinheiro.add(novoDinheiro));
                passagemRetorno.setValorPagamentoPix(antigoPix.add(novoPix));
                passagemRetorno.setValorPagamentoCartao(antigoCartao.add(novoCartao));
                
                // Recalcula totais gerais
                BigDecimal totalPagoAtualizado = passagemRetorno.getValorPagamentoDinheiro()
                        .add(passagemRetorno.getValorPagamentoPix())
                        .add(passagemRetorno.getValorPagamentoCartao());
                
                passagemRetorno.setValorPago(totalPagoAtualizado);
                
                // Recalcula o Devedor
                BigDecimal totalDaPassagem = passagemRetorno.getValorTotal().subtract(passagemRetorno.getValorDesconto());
                BigDecimal novoDevedor = totalDaPassagem.subtract(totalPagoAtualizado);
                if (novoDevedor.compareTo(BigDecimal.ZERO) < 0) novoDevedor = BigDecimal.ZERO;
                
                passagemRetorno.setValorAPagar(novoDevedor); // Atualiza campo valor_a_pagar/devedor
                passagemRetorno.setDevedor(novoDevedor);
                
                // Atualiza Status
                if (novoDevedor.compareTo(BigDecimal.ZERO) <= 0) {
                    passagemRetorno.setStatusPassagem("PAGO");
                } else {
                    passagemRetorno.setStatusPassagem("PARCIAL");
                }
                
                // 5. Salva no Banco usando o DAO atualizado
                PassagemDAO dao = new PassagemDAO();
                boolean sucesso = dao.atualizar(passagemRetorno);
                
                if (sucesso) {
                    alert("Pagamento registrado com sucesso!");
                    carregarDados(); // Atualiza a tabela
                } else {
                    alert("Erro ao salvar o pagamento no banco.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro: " + e.getMessage());
        }
    }
    
    // Método auxiliar para buscar a passagem completa (necessário pois a tabela usa modelo simplificado)
    private Passagem buscarPassagemCompletaPorId(long id) {
        PassagemDAO dao = new PassagemDAO();
        // Como o DAO pode não ter o buscarPorId implementado no seu código antigo,
        // vou implementar uma busca rápida aqui mesmo para garantir.
        String sql = "SELECT * FROM passagens WHERE id_passagem = ?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Passagem p = new Passagem();
                p.setId(rs.getLong("id_passagem"));
                p.setNumBilhete(rs.getInt("numero_bilhete"));
                p.setValorTotal(rs.getBigDecimal("valor_total"));
                p.setValorDesconto(rs.getBigDecimal("valor_desconto_geral"));
                // Importante: Pegar o valor "A Pagar" correto (que é a dívida atual)
                BigDecimal total = rs.getBigDecimal("valor_total");
                BigDecimal pago = rs.getBigDecimal("valor_pago");
                BigDecimal desc = rs.getBigDecimal("valor_desconto_geral");
                if (pago == null) pago = BigDecimal.ZERO;
                if (desc == null) desc = BigDecimal.ZERO;
                
                p.setValorAPagar(total.subtract(desc).subtract(pago)); 
                p.setValorPago(pago);
                
                p.setValorPagamentoDinheiro(rs.getBigDecimal("valor_pagamento_dinheiro"));
                p.setValorPagamentoPix(rs.getBigDecimal("valor_pagamento_pix"));
                p.setValorPagamentoCartao(rs.getBigDecimal("valor_pagamento_cartao"));
                
                // Campos obrigatórios para o update funcionar sem quebrar FKs
                p.setIdPassageiro(rs.getLong("id_passageiro"));
                p.setIdViagem(rs.getLong("id_viagem"));
                p.setIdRota(rs.getLong("id_rota"));
                // ... preencher outros FKs essenciais como null safe se o update pedir
                p.setAssento(rs.getString("assento"));
                // Se o seu método atualizar() valida muitos campos, precisaríamos carregar tudo.
                // Mas geralmente para financeiro, focamos nos valores.
                
                // Recarrega dados auxiliares minimos para o update não falhar
                p.setIdAcomodacao(getObjectInt(rs, "id_acomodacao"));
                p.setIdTipoPassagem(getObjectInt(rs, "id_tipo_passagem"));
                p.setIdAgente(getObjectInt(rs, "id_agente"));
                p.setIdCaixa(getObjectInt(rs, "id_caixa"));
                p.setIdHorarioSaida(getObjectInt(rs, "id_horario_saida"));
                p.setObservacoes(rs.getString("observacoes"));
                
                return p;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Integer getObjectInt(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        return (o != null) ? (Integer) o : null;
    }
    
    @FXML
    public void estornarPagamento() {
        PassagemFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) { alert("Selecione um item para estornar."); return; }
        
        if (selecionada.getPago().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) { alert("Este item não tem pagamento para estornar."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/EstornoPagamento.fxml"));
            Parent root = loader.load();
            
            EstornoPagamentoController controller = loader.getController();
            controller.setDados(selecionada.getTotal(), selecionada.getPago());
            
            Stage stage = new Stage();
            stage.setTitle("Estornar Pagamento - Área Restrita");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            
            if (controller.isConfirmado()) {
                java.math.BigDecimal vEstorno = controller.getValorEstorno();
                String motivo = controller.getMotivo();
                String forma = controller.getFormaDevolucao();
                int idAutorizador = controller.getIdAutorizador();
                String nomeAutorizador = controller.getNomeAutorizador();

                java.math.BigDecimal novoPago = selecionada.getPago().subtract(vEstorno);
                String novoStatus = (novoPago.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) ? "PARCIAL" : "PENDENTE";

                Connection con = null;
                try {
                    con = ConexaoBD.getConnection();
                    con.setAutoCommit(false);

                    Passagem p = buscarPassagemCompletaPorId(selecionada.getId());
                    BigDecimal estornoBD = vEstorno;
                    BigDecimal din = p.getValorPagamentoDinheiro();
                    BigDecimal pix = p.getValorPagamentoPix();
                    BigDecimal car = p.getValorPagamentoCartao();
                    
                    if (din.compareTo(estornoBD) >= 0) {
                        din = din.subtract(estornoBD); estornoBD = BigDecimal.ZERO;
                    } else {
                        estornoBD = estornoBD.subtract(din); din = BigDecimal.ZERO;
                        if (pix.compareTo(estornoBD) >= 0) {
                            pix = pix.subtract(estornoBD); estornoBD = BigDecimal.ZERO;
                        } else {
                            estornoBD = estornoBD.subtract(pix); pix = BigDecimal.ZERO;
                            if (car.compareTo(estornoBD) >= 0) car = car.subtract(estornoBD);
                        }
                    }

                    String sqlUp = "UPDATE passagens SET valor_pago = ?, status_passagem = ?, valor_pagamento_dinheiro = ?, valor_pagamento_pix = ?, valor_pagamento_cartao = ? WHERE id_passagem = ?";
                    try (PreparedStatement stmt = con.prepareStatement(sqlUp)) {
                        stmt.setBigDecimal(1, novoPago);
                        stmt.setString(2, novoStatus);
                        stmt.setBigDecimal(3, din);
                        stmt.setBigDecimal(4, pix);
                        stmt.setBigDecimal(5, car);
                        stmt.setInt(6, selecionada.getId());
                        stmt.executeUpdate();
                    }

                    // 2. Grava Log
                    String sqlLog = "INSERT INTO log_estornos_passagens (id_passagem, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = con.prepareStatement(sqlLog)) {
                        stmt.setInt(1, selecionada.getId());
                        stmt.setBigDecimal(2, vEstorno);
                        stmt.setString(3, motivo);
                        stmt.setString(4, forma);
                        stmt.setInt(5, idAutorizador);
                        stmt.setString(6, nomeAutorizador);
                        stmt.executeUpdate();
                    }
                    
                    con.commit();
                    alert("Estorno realizado com sucesso!");
                    carregarDados();

                } catch (Exception ex) {
                    if(con != null) con.rollback();
                    ex.printStackTrace();
                    alert("Erro ao gravar estorno: " + ex.getMessage());
                } finally {
                    if(con != null) con.setAutoCommit(true);
                }
            }
        } catch (Exception e) { 
            e.printStackTrace();
            alert("Erro ao abrir tela de estorno: " + e.getMessage());
        }
    }

    @FXML
    public void gerarRelatorioCliente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ExtratoPassageiro.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Extrato Financeiro - Passageiro");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            alert("Tela de Extrato ainda não criada ou com erro: " + e.getMessage());
        }
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    public static class OpcaoViagem {
        int id; String label;
        public OpcaoViagem(int id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    // --- MODELO INTERNO PARA TABELA ---
    public static class PassagemFinanceiro {
        private int id;
        private String bilhete, dataViagem, passageiro, destino, status;
        private java.math.BigDecimal total, pago;

        public PassagemFinanceiro(int id, String bilhete, String data, String passageiro, String destino, java.math.BigDecimal total, java.math.BigDecimal pago, String status) {
            this.id = id; this.bilhete = bilhete; this.dataViagem = data;
            this.passageiro = passageiro; this.destino = destino;
            this.total = total != null ? total : java.math.BigDecimal.ZERO;
            this.pago = pago != null ? pago : java.math.BigDecimal.ZERO;
            this.status = status;
        }
        public int getId() { return id; }
        public String getBilhete() { return bilhete; }
        public String getDataViagem() { return dataViagem; }
        public String getPassageiro() { return passageiro; }
        public String getDestino() { return destino; }
        public java.math.BigDecimal getTotal() { return total; }
        public java.math.BigDecimal getPago() { return pago; }
        public java.math.BigDecimal getRestante() { return total.subtract(pago).max(java.math.BigDecimal.ZERO); }
        public String getTotalFormatado() { return String.format("R$ %,.2f", total); }
        public String getPagoFormatado() { return String.format("R$ %,.2f", pago); }
        public String getRestanteFormatado() { return String.format("R$ %,.2f", getRestante()); }
        public String getStatus() { return status; }
    }
}