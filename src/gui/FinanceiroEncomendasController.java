package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;

public class FinanceiroEncomendasController {

    @FXML private ComboBox<OpcaoViagem> cmbViagem;
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkApenasDevedores;
    @FXML private Label lblTotalPendente;
    @FXML private Button btnSair;

    @FXML private TableView<EncomendaFinanceiro> tabela;
    @FXML private TableColumn<EncomendaFinanceiro, String> colNumero;
    @FXML private TableColumn<EncomendaFinanceiro, String> colData; 
    @FXML private TableColumn<EncomendaFinanceiro, String> colRemetente;
    @FXML private TableColumn<EncomendaFinanceiro, String> colDestinatario;
    @FXML private TableColumn<EncomendaFinanceiro, String> colTotal;
    @FXML private TableColumn<EncomendaFinanceiro, String> colPago;
    @FXML private TableColumn<EncomendaFinanceiro, String> colRestante;
    @FXML private TableColumn<EncomendaFinanceiro, String> colStatus;

    @FXML
    public void initialize() {
        configurarTabela();
        carregarComboViagens();
        
        cmbViagem.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        chkApenasDevedores.selectedProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        
        try {
            tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception e) { }
    }

    @FXML
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    }

    // --- Abre a nova tela de Histórico ---
    @FXML
    public void verHistoricoEstornos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HistoricoEstornos.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Histórico de Auditoria - Estornos");
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
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataLancamento"));
        colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        colDestinatario.setCellValueFactory(new PropertyValueFactory<>("destinatario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatado"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("pagoFormatado"));
        colRestante.setCellValueFactory(new PropertyValueFactory<>("restanteFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(column -> new TableCell<EncomendaFinanceiro, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    if (item.equals("PENDENTE")) setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    else if (item.equals("PARCIAL")) setStyle("-fx-text-fill: #ef6c00; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    else setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });
    }

    private void carregarComboViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        String sql = "SELECT id_viagem, descricao, data_viagem, data_chegada FROM viagens ORDER BY id_viagem DESC LIMIT 20";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            lista.add(new OpcaoViagem(0, "Todas as Viagens"));

            while (rs.next()) {
                String label = rs.getString("descricao");
                java.sql.Date dtSaida = rs.getDate("data_viagem");
                java.sql.Date dtChegada = rs.getDate("data_chegada");
                
                if (dtSaida != null) {
                    label += " (" + sdf.format(dtSaida);
                    if (dtChegada != null) label += " - " + sdf.format(dtChegada);
                    label += ")";
                }
                lista.add(new OpcaoViagem(rs.getInt("id_viagem"), label));
            }
            cmbViagem.setItems(lista);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void carregarDados() {
        if (cmbViagem.getValue() == null) return;
        int idViagem = cmbViagem.getValue().id;
        
        ObservableList<EncomendaFinanceiro> lista = FXCollections.observableArrayList();
        double somaPendente = 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.id_encomenda, e.numero_encomenda, v.data_viagem, e.remetente, e.destinatario, e.total_a_pagar, e.valor_pago ");
        sql.append("FROM encomendas e JOIN viagens v ON e.id_viagem = v.id_viagem WHERE 1=1 ");

        if (idViagem > 0) sql.append(" AND e.id_viagem = ").append(idViagem);
        if (chkApenasDevedores.isSelected()) sql.append(" AND (e.valor_pago < e.total_a_pagar OR e.valor_pago IS NULL) ");

        String busca = txtBusca.getText().toLowerCase();
        if (!busca.isEmpty()) {
            sql.append(" AND (LOWER(e.remetente) LIKE ? OR LOWER(e.destinatario) LIKE ?) ");
        }
        
        sql.append(" ORDER BY e.id_encomenda DESC");

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            
            if (!busca.isEmpty()) {
                stmt.setString(1, "%" + busca + "%");
                stmt.setString(2, "%" + busca + "%");
            }

            ResultSet rs = stmt.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            
            while (rs.next()) {
                double total = rs.getDouble("total_a_pagar");
                double pago = rs.getDouble("valor_pago");
                double devendo = total - pago;
                
                String dataFmt = "";
                if(rs.getDate("data_viagem") != null) dataFmt = sdf.format(rs.getDate("data_viagem"));

                lista.add(new EncomendaFinanceiro(
                    rs.getInt("id_encomenda"),
                    rs.getString("numero_encomenda"),
                    dataFmt,
                    rs.getString("remetente"),
                    rs.getString("destinatario"),
                    total, pago
                ));
                if(devendo > 0.01) somaPendente += devendo;
            }
            tabela.setItems(lista);
            lblTotalPendente.setText(String.format("R$ %.2f", somaPendente));

        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void darBaixa() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alert("Selecione uma encomenda na tabela para dar baixa.");
            return;
        }
        if (selecionada.getRestante() <= 0.01) {
            alert("Esta encomenda já está quitada!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BaixaPagamento.fxml"));
            Parent root = loader.load();
            BaixaPagamentoController controller = loader.getController();
            
            controller.setDadosIniciais(
                selecionada.getTotal(), 
                selecionada.getPago(),
                selecionada.getRestante()
            );
            
            Stage stage = new Stage();
            stage.setTitle("Realizar Pagamento");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            
            if (controller.isConfirmado()) {
                salvarPagamento(selecionada.getId(), controller, selecionada.getPago());
            }

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro ao abrir tela de pagamento: " + e.getMessage());
        }
    }
    
    private void salvarPagamento(int idEncomenda, BaixaPagamentoController dados, double jaPago) {
        double novoPago = jaPago + dados.getValorPago();
        double totalComDesconto = dados.getValorTotalOriginal() - dados.getDesconto();
        String novoStatus = (novoPago >= totalComDesconto - 0.01) ? "PAGO" : "PARCIAL";

        String sql = "UPDATE encomendas SET valor_pago = ?, desconto = ?, tipo_pagamento = ?, caixa = ?, status_pagamento = ? WHERE id_encomenda = ?";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setDouble(1, novoPago);
            stmt.setDouble(2, dados.getDesconto());
            stmt.setString(3, dados.getFormaPagamento());
            stmt.setString(4, dados.getCaixa());
            stmt.setString(5, novoStatus);
            stmt.setInt(6, idEncomenda);
            stmt.executeUpdate();
            
            alert("Pagamento registrado com sucesso!");
            carregarDados();

        } catch (SQLException e) {
            alert("Erro ao salvar no banco: " + e.getMessage());
        }
    }
    
    // --- MÉTODO ESTORNAR (AGORA PRESENTE E CORRIGIDO) ---
    @FXML
    public void estornarPagamento() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) { alert("Selecione um item para estornar."); return; }
        
        if (selecionada.getPago() <= 0.01) { alert("Este item não tem pagamento para estornar."); return; }

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
                double vEstorno = controller.getValorEstorno();
                String motivo = controller.getMotivo();
                String forma = controller.getFormaDevolucao();
                int idAutorizador = controller.getIdAutorizador();
                String nomeAutorizador = controller.getNomeAutorizador();

                double novoPago = selecionada.getPago() - vEstorno;
                String novoStatus = (novoPago > 0.01) ? "PARCIAL" : "PENDENTE";
                
                Connection con = null;
                try {
                    con = ConexaoBD.getConnection();
                    con.setAutoCommit(false); 

                    String sqlUp = "UPDATE encomendas SET valor_pago = ?, status_pagamento = ? WHERE id_encomenda = ?";
                    try (PreparedStatement stmt = con.prepareStatement(sqlUp)) {
                        stmt.setDouble(1, novoPago);
                        stmt.setString(2, novoStatus);
                        stmt.setInt(3, selecionada.getId());
                        stmt.executeUpdate();
                    }

                    String sqlLog = "INSERT INTO log_estornos_encomendas (id_encomenda, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = con.prepareStatement(sqlLog)) {
                        stmt.setInt(1, selecionada.getId());
                        stmt.setDouble(2, vEstorno);
                        stmt.setString(3, motivo);
                        stmt.setString(4, forma);
                        stmt.setInt(5, idAutorizador);
                        stmt.setString(6, nomeAutorizador);
                        stmt.executeUpdate();
                    }
                    
                    con.commit();
                    alert("Estorno realizado com sucesso!\nAutorizado por: " + nomeAutorizador);
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

    // --- GERA EXTRATO (AGORA ABRE MESMO SEM SELEÇÃO) ---
    @FXML
    public void gerarRelatorioCliente() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        String nomeCliente = "";

        if (selecionada != null) {
            nomeCliente = selecionada.getRemetente();
        } else {
            nomeCliente = txtBusca.getText();
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ExtratoClienteEncomenda.fxml"));
            Parent root = loader.load();
            
            ExtratoClienteEncomendaController controller = loader.getController();
            
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                controller.carregarExtrato(nomeCliente);
            } else {
                controller.carregarExtrato(""); // Abre vazio para pesquisar lá
            }
            
            Stage stage = new Stage();
            stage.setTitle("Extrato Financeiro");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro ao abrir extrato: " + e.getMessage());
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

    public static class EncomendaFinanceiro {
        private int id;
        private String numero, dataLancamento, remetente, destinatario;
        private Double total, pago;

        public EncomendaFinanceiro(int id, String num, String data, String rem, String dest, Double total, Double pago) {
            this.id = id; this.numero = num; this.dataLancamento = data; 
            this.remetente = rem; this.destinatario = dest;
            this.total = total; this.pago = pago;
        }
        public int getId() { return id; }
        public String getNumero() { return numero; }
        public String getDataLancamento() { return dataLancamento; }
        public String getRemetente() { return remetente; }
        public String getDestinatario() { return destinatario; }
        public Double getTotal() { return total; }
        public Double getPago() { return pago; }
        public Double getRestante() { return Math.max(0, total - pago); }
        public String getTotalFormatado() { return String.format("R$ %.2f", total); }
        public String getPagoFormatado() { return String.format("R$ %.2f", pago); }
        public String getRestanteFormatado() { return String.format("R$ %.2f", getRestante()); }
        public String getStatus() { 
            if (getRestante() <= 0.01) return "PAGO";
            if (getPago() > 0.01) return "PARCIAL";
            return "PENDENTE";
        }
    }
}