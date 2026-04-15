package gui;

import dao.ConexaoBD;
import gui.util.PermissaoService;
import gui.util.StatusPagamentoView;
import model.ItemExtratoEncomenda;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import util.AppLogger;

public class ExtratoClienteEncomendaController {

    @FXML private Label lblNomeCliente;
    @FXML private Label lblTotalGeral;
    @FXML private Label lblTotalPago;
    @FXML private Label lblTotalDivida;
    @FXML private Button btnFechar;
    @FXML private Button btnQuitarTudo;
    
    @FXML private ComboBox<String> cmbClientes;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<ItemExtratoEncomenda> tabela;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colData;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colRota;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colDescricao;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colValor;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colPago;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colSaldo;
    @FXML private TableColumn<ItemExtratoEncomenda, String> colStatus;

    private String nomeClienteAtual = "";
    private BigDecimal dividaTotalAtual = BigDecimal.ZERO;
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private ObservableList<String> listaTodosClientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!PermissaoService.isAdmin()) { PermissaoService.exigirAdmin("Extrato Cliente Encomenda"); return; }
        configurarTabela();
        configurarAutocomplete();

        // DR010: carrega lista de clientes em background
        Thread bg = new Thread(this::carregarListaClientesBanco);
        bg.setDaemon(true);
        bg.start();
        
        cmbStatus.setItems(FXCollections.observableArrayList("TODOS", "PENDENTES", "PAGOS"));
        cmbStatus.getSelectionModel().selectFirst();
    }

    private void configuringTabela() {
        colData.setCellValueFactory(new PropertyValueFactory<>("dataViagem"));
        colRota.setCellValueFactory(new PropertyValueFactory<>("rota"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("valorPago"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(column -> new TableCell<ItemExtratoEncomenda, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(StatusPagamentoView.getEstiloCelula(model.StatusPagamento.fromString(item)));
                }
            }
        });
        
        colValor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 14px;");
        colPago.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 14px;");
        colSaldo.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-font-size: 14px;");
        colDescricao.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold; -fx-font-size: 14px;");
        colRota.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-size: 14px;");
        colData.setStyle("-fx-alignment: CENTER; -fx-font-size: 14px;");
        
        try { tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); } catch(Exception e){ AppLogger.warn("ExtratoClienteEncomendaController", "Erro em ExtratoClienteEncomendaController.configuringTabela (CSS): " + e.getMessage()); }
    }
    
    private void configurarTabela() { configuringTabela(); } 

    private void carregarListaClientesBanco() {
        ObservableList<String> nomes = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT nome FROM ( SELECT nome_cliente as nome FROM cad_clientes_encomenda WHERE empresa_id = ? UNION SELECT destinatario as nome FROM encomendas WHERE empresa_id = ? ) as todos WHERE nome IS NOT NULL ORDER BY nome";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
             stmt.setInt(1, dao.DAOUtils.empresaId());
             stmt.setInt(2, dao.DAOUtils.empresaId());
             // DR213: try-with-resources para ResultSet
             try (ResultSet rs = stmt.executeQuery()) {
             while(rs.next()) nomes.add(rs.getString("nome"));
             }
        } catch (Exception e) { AppLogger.error("ExtratoClienteEncomendaController", e.getMessage(), e); }
        javafx.application.Platform.runLater(() -> {
            listaTodosClientes.setAll(nomes);
            cmbClientes.setItems(listaTodosClientes);
        });
    }

    private void configurarAutocomplete() {
        cmbClientes.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ObservableList<String> filtrados = FXCollections.observableArrayList();
            for (String nome : listaTodosClientes) {
                if (nome.toUpperCase().contains(newVal.toUpperCase())) filtrados.add(nome);
            }
            cmbClientes.setItems(filtrados);
            cmbClientes.show(); 
        });
    }

    public void carregarExtrato(String nome) {
        this.nomeClienteAtual = nome;
        cmbClientes.setValue(nome); 
        if(nome != null && !nome.isEmpty()) {
            lblNomeCliente.setText(nome.toUpperCase());
            buscarDados();
        }
    }
    
    @FXML
    public void pesquisarCliente() {
        String novoNome = cmbClientes.getValue();
        if (novoNome == null || novoNome.trim().isEmpty()) {
             novoNome = cmbClientes.getEditor().getText(); 
        }
        if (novoNome != null && !novoNome.trim().isEmpty()) {
            this.nomeClienteAtual = novoNome;
            lblNomeCliente.setText(novoNome.toUpperCase());
            buscarDados();
        }
    }

    // DR211: buscar dados em background thread para nao bloquear FX thread
    private void buscarDados() {
        String statusFiltro = cmbStatus.getValue();
        String clienteNome = nomeClienteAtual != null ? nomeClienteAtual.trim() : "";
        if (clienteNome.isEmpty()) return;

        Thread bg = new Thread(() -> {
            try {
                ObservableList<ItemExtratoEncomenda> lista = FXCollections.observableArrayList();
                BigDecimal totalGeral = BigDecimal.ZERO;
                BigDecimal totalPago = BigDecimal.ZERO;
                BigDecimal dividaTotal = BigDecimal.ZERO;

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                String sql = "SELECT e.id_encomenda, e.numero_encomenda, e.remetente, e.total_a_pagar, e.valor_pago, " +
                             "v.data_viagem, v.data_chegada, r.origem, r.destino " +
                             "FROM encomendas e " +
                             "LEFT JOIN viagens v ON e.id_viagem = v.id_viagem " +
                             "LEFT JOIN rotas r ON v.id_rota = r.id " +
                             "WHERE UPPER(TRIM(e.destinatario)) = UPPER(TRIM(?)) " +
                             "AND e.empresa_id = ? " +
                             "ORDER BY v.data_viagem DESC";

                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {

                    stmt.setString(1, clienteNome);
                    stmt.setInt(2, dao.DAOUtils.empresaId());
                    try (ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        BigDecimal val = rs.getBigDecimal("total_a_pagar");
                        BigDecimal pag = rs.getBigDecimal("valor_pago");
                        if (val == null) val = BigDecimal.ZERO;
                        if (pag == null) pag = BigDecimal.ZERO;
                        BigDecimal saldo = val.subtract(pag).max(BigDecimal.ZERO);

                        String status = (saldo.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) ? "PAGO" : "PENDENTE";

                        if (statusFiltro != null) {
                            if (statusFiltro.equals("PENDENTES") && status.equals("PAGO")) continue;
                            if (statusFiltro.equals("PAGOS") && !status.equals("PAGO")) continue;
                        }

                        totalGeral = totalGeral.add(val);
                        totalPago = totalPago.add(pag);
                        dividaTotal = dividaTotal.add(saldo);

                        String dataViagemStr = "--";
                        if(rs.getDate("data_viagem") != null) {
                            dataViagemStr = sdf.format(rs.getDate("data_viagem"));
                            if(rs.getDate("data_chegada") != null) {
                                dataViagemStr += " - " + sdf.format(rs.getDate("data_chegada"));
                            }
                        }

                        String rotaStr = "Geral";
                        String orig = rs.getString("origem");
                        String dest = rs.getString("destino");
                        if(orig != null && dest != null) rotaStr = orig + " -> " + dest;

                        String num = rs.getString("numero_encomenda");
                        String rem = rs.getString("remetente");
                        String desc = "N° Enc. " + num + " (De: " + rem + ")";

                        lista.add(new ItemExtratoEncomenda(dataViagemStr, rotaStr, desc, val.doubleValue(), pag.doubleValue(), saldo.doubleValue()));
                    }
                    }
                }

                final BigDecimal fTotalGeral = totalGeral;
                final BigDecimal fTotalPago = totalPago;
                final BigDecimal fDividaTotal = dividaTotal;
                javafx.application.Platform.runLater(() -> {
                    dividaTotalAtual = fDividaTotal;
                    tabela.setItems(lista);
                    lblTotalGeral.setText(nf.format(fTotalGeral));
                    lblTotalPago.setText(nf.format(fTotalPago));
                    lblTotalDivida.setText(nf.format(fDividaTotal));
                    btnQuitarTudo.setDisable(fDividaTotal.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0);
                });

            } catch (Exception e) {
                AppLogger.error("ExtratoClienteEncomendaController", e.getMessage(), e);
                javafx.application.Platform.runLater(() -> AlertHelper.errorSafe("buscar extrato encomendas", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // --- FUNÇÃO ATUALIZADA: CHAMADA DA NOVA TELA DE QUITAÇÃO ---
    @FXML
    public void quitarDividaTotal() {
        if (dividaTotalAtual.compareTo(BigDecimal.ZERO) <= 0) return;

        try {
            // CHAMA O FXML ESPECÍFICO: QuitarDividaEncomendaTotal.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/QuitarDividaEncomendaTotal.fxml"));
            Parent root = loader.load();
            
            // CHAMA O CONTROLLER ESPECÍFICO
            QuitarDividaEncomendaTotalController controller = loader.getController();
            controller.setValorTotal(dividaTotalAtual);
            
            Stage stage = new Stage();
            stage.setTitle("Quitar Dívida Total (Encomendas)");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            
            if (controller.isConfirmado()) {
                BigDecimal descontoTotal = controller.getDesconto();
                String forma = controller.getForma();
                String caixa = controller.getCaixa();

                // DL047: validar desconto antes de calcular fator proporcional
                if (descontoTotal.compareTo(dividaTotalAtual) > 0) {
                    Alert aErro = new Alert(Alert.AlertType.ERROR);
                    aErro.setTitle("Erro");
                    aErro.setHeaderText("Desconto invalido");
                    aErro.setContentText("O desconto (R$ " + String.format("%,.2f", descontoTotal) + ") nao pode ser maior que a divida total (R$ " + String.format("%,.2f", dividaTotalAtual) + ").");
                    aErro.showAndWait();
                    return;
                }

                BigDecimal fatorPagamento = dividaTotalAtual.subtract(descontoTotal).divide(dividaTotalAtual, 10, RoundingMode.HALF_UP);

                String sqlUpdate = "UPDATE encomendas SET " +
                                   "desconto = (total_a_pagar - valor_pago) * (1 - ?), " +
                                   "valor_pago = total_a_pagar - ((total_a_pagar - valor_pago) * (1 - ?)), " +
                                   "status_pagamento = 'PAGO', tipo_pagamento = ?, caixa = ? " +
                                   "WHERE UPPER(TRIM(destinatario)) = UPPER(TRIM(?)) AND valor_pago < total_a_pagar " +
                                   "AND empresa_id = ?";

                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sqlUpdate)) {

                    stmt.setBigDecimal(1, fatorPagamento);
                    stmt.setBigDecimal(2, fatorPagamento);
                    stmt.setString(3, forma);
                    stmt.setString(4, caixa);
                    stmt.setString(5, nomeClienteAtual.trim());
                    stmt.setInt(6, dao.DAOUtils.empresaId());
                    
                    int qtd = stmt.executeUpdate();
                    
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Sucesso");
                    a.setHeaderText("Dívida Quitada!");
                    a.setContentText(qtd + " encomendas foram baixadas com sucesso.");
                    a.showAndWait();
                    
                    buscarDados(); 

                }
            }
        } catch (Exception e) {
            AppLogger.error("ExtratoClienteEncomendaController", e.getMessage(), e);
            Alert a = new Alert(Alert.AlertType.ERROR, "Erro ao quitar: " + e.getMessage());
            a.show();
        }
    }

    @FXML
    public void imprimir() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tabela.getScene().getWindow())) {
            boolean success = job.printPage(tabela.getScene().getRoot());
            if (success) job.endJob();
        }
    }

    @FXML public void fechar() { ((Stage) btnFechar.getScene().getWindow()).close(); }

}