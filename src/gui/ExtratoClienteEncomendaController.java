package gui;

import dao.ConexaoBD;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;

public class ExtratoClienteEncomendaController {

    @FXML private Label lblNomeCliente;
    @FXML private Label lblTotalGeral;
    @FXML private Label lblTotalPago;
    @FXML private Label lblTotalDivida;
    @FXML private Button btnFechar;
    @FXML private Button btnQuitarTudo;
    
    @FXML private ComboBox<String> cmbClientes;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<ItemExtrato> tabela;
    @FXML private TableColumn<ItemExtrato, String> colData;
    @FXML private TableColumn<ItemExtrato, String> colRota;
    @FXML private TableColumn<ItemExtrato, String> colDescricao;
    @FXML private TableColumn<ItemExtrato, String> colValor;
    @FXML private TableColumn<ItemExtrato, String> colPago;
    @FXML private TableColumn<ItemExtrato, String> colSaldo;
    @FXML private TableColumn<ItemExtrato, String> colStatus;

    private String nomeClienteAtual = "";
    private double dividaTotalAtual = 0;
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private ObservableList<String> listaTodosClientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabela();
        carregarListaClientesBanco(); 
        configurarAutocomplete();
        
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
        
        colStatus.setCellFactory(column -> new TableCell<ItemExtrato, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                }
            }
        });
        
        colValor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 14px;");
        colPago.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 14px;");
        colSaldo.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-font-size: 14px;");
        colDescricao.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold; -fx-font-size: 14px;");
        colRota.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-size: 14px;");
        colData.setStyle("-fx-alignment: CENTER; -fx-font-size: 14px;");
        
        try { tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); } catch(Exception e){}
    }
    
    private void configurarTabela() { configuringTabela(); } 

    private void carregarListaClientesBanco() {
        listaTodosClientes.clear();
        String sql = "SELECT DISTINCT nome FROM ( SELECT nome_cliente as nome FROM cad_clientes_encomenda UNION SELECT destinatario as nome FROM encomendas ) as todos WHERE nome IS NOT NULL ORDER BY nome";
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement(sql).executeQuery()) {
             while(rs.next()) listaTodosClientes.add(rs.getString("nome"));
        } catch (Exception e) { e.printStackTrace(); }
        cmbClientes.setItems(listaTodosClientes);
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

    private void buscarDados() {
        ObservableList<ItemExtrato> lista = FXCollections.observableArrayList();
        double totalGeral = 0;
        double totalPago = 0;
        this.dividaTotalAtual = 0;
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String statusFiltro = cmbStatus.getValue();

        String sql = "SELECT e.id_encomenda, e.numero_encomenda, e.remetente, e.total_a_pagar, e.valor_pago, " +
                     "v.data_viagem, v.data_chegada, r.origem, r.destino " +
                     "FROM encomendas e " +
                     "LEFT JOIN viagens v ON e.id_viagem = v.id_viagem " +
                     "LEFT JOIN rotas r ON v.id_rota = r.id " + 
                     "WHERE e.destinatario ILIKE ? " + 
                     "ORDER BY v.data_viagem DESC";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nomeClienteAtual.trim() + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                double val = rs.getDouble("total_a_pagar");
                double pag = rs.getDouble("valor_pago");
                double saldo = Math.max(0, val - pag);
                
                String status = (saldo <= 0.01) ? "PAGO" : "PENDENTE";
                
                if (statusFiltro != null) {
                    if (statusFiltro.equals("PENDENTES") && status.equals("PAGO")) continue;
                    if (statusFiltro.equals("PAGOS") && !status.equals("PAGO")) continue;
                }
                
                totalGeral += val;
                totalPago += pag;
                dividaTotalAtual += saldo;
                
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

                lista.add(new ItemExtrato(dataViagemStr, rotaStr, desc, val, pag, saldo));
            }
            
            tabela.setItems(lista);
            
            lblTotalGeral.setText(nf.format(totalGeral));
            lblTotalPago.setText(nf.format(totalPago));
            lblTotalDivida.setText(nf.format(dividaTotalAtual));
            
            btnQuitarTudo.setDisable(dividaTotalAtual <= 0.01);

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Erro ao buscar dados: " + e.getMessage());
            a.show();
        }
    }

    // --- FUNÇÃO ATUALIZADA: CHAMADA DA NOVA TELA DE QUITAÇÃO ---
    @FXML
    public void quitarDividaTotal() {
        if (dividaTotalAtual <= 0) return;

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
                double descontoTotal = controller.getDesconto();
                String forma = controller.getForma();
                String caixa = controller.getCaixa();

                // Cálculo Proporcional do Desconto
                double fatorPagamento = (dividaTotalAtual - descontoTotal) / dividaTotalAtual;
                if (fatorPagamento < 0) fatorPagamento = 0; 

                String sqlUpdate = "UPDATE encomendas SET " +
                                   "desconto = (total_a_pagar - valor_pago) * (1 - ?), " +
                                   "valor_pago = total_a_pagar - ((total_a_pagar - valor_pago) * (1 - ?)), " +
                                   "status_pagamento = 'PAGO', tipo_pagamento = ?, caixa = ? " +
                                   "WHERE TRIM(destinatario) ILIKE TRIM(?) AND valor_pago < total_a_pagar";

                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sqlUpdate)) {

                    stmt.setDouble(1, fatorPagamento);
                    stmt.setDouble(2, fatorPagamento);
                    stmt.setString(3, forma);
                    stmt.setString(4, caixa);
                    stmt.setString(5, nomeClienteAtual.trim());
                    
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
            e.printStackTrace();
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

    // Classe Modelo
    public static class ItemExtrato {
        private String dataViagem, rota, descricao;
        private Double valorTotal, valorPago, saldo;
        private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        public ItemExtrato(String data, String rota, String desc, Double total, Double pago, Double saldo) {
            this.dataViagem = data; this.rota = rota; this.descricao = desc;
            this.valorTotal = total; this.valorPago = pago; this.saldo = saldo;
        }
        public String getDataViagem() { return dataViagem; }
        public String getRota() { return rota; }
        public String getDescricao() { return descricao; }
        public String getValorTotal() { return nf.format(valorTotal); }
        public String getValorPago() { return nf.format(valorPago); }
        public String getSaldo() { return nf.format(saldo); }
        public String getStatus() {
            return model.StatusPagamento.calcularPorSaldo(saldo, valorPago).name();
        }
    }
}