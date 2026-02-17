package gui;

import dao.ConexaoBD;
import dao.EmpresaDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
import gui.util.RelatorioUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import model.Empresa;
import model.Rota;
import model.Viagem;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class RelatorioFretesController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private ComboBox<Viagem> cmbViagem;
    @FXML private ComboBox<String> cmbRota;
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbDevedores;
    
    @FXML private TableView<FreteItem> tabelaFretes;
    @FXML private TableColumn<FreteItem, String> colCodFrete;
    @FXML private TableColumn<FreteItem, String> colDataViagem;
    @FXML private TableColumn<FreteItem, String> colRemetente;
    @FXML private TableColumn<FreteItem, String> colItem;
    @FXML private TableColumn<FreteItem, String> colQuant;
    @FXML private TableColumn<FreteItem, String> colPreco;
    @FXML private TableColumn<FreteItem, String> colTotal;
    
    @FXML private TableView<FreteDevedor> tabelaDevedores;
    @FXML private TableColumn<FreteDevedor, String> colDevTotal;
    @FXML private TableColumn<FreteDevedor, String> colDevBaixado;
    @FXML private TableColumn<FreteDevedor, String> colDevDevedor;
    @FXML private TableColumn<FreteDevedor, String> colDevNumFrete;
    
    @FXML private Label lblTotalItens;
    @FXML private Label lblEmAberto;
    
    private ViagemDAO viagemDAO = new ViagemDAO();
    private RotaDAO rotaDAO = new RotaDAO();
    private ObservableList<FreteItem> listaFreteItens = FXCollections.observableArrayList();
    private ObservableList<FreteDevedor> listaDevedores = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Cores e Fontes - PADRÃO DO SISTEMA
    private final String COR_AZUL_ESCURO = "#0d47a1";
    private final String COR_CINZA_CLARO = "#f5f5f5";
    
    private final Font FONT_EMPRESA = Font.font("Arial", FontWeight.BLACK, 16);
    private final Font FONT_ROTA_DESTAQUE = Font.font("Arial", FontWeight.BLACK, 14);
    private final Font FONT_DATAS = Font.font("Arial", FontWeight.BOLD, 11);
    private final Font FONT_TITULO_FRETE = Font.font("Arial", FontWeight.BLACK, 14);
    private final Font FONT_NORMAL = Font.font("Arial", FontWeight.NORMAL, 10);
    private final Font FONT_NEGRITO = Font.font("Arial", FontWeight.BOLD, 10);
    private final Font FONT_HEADER_ITENS = Font.font("Arial", FontWeight.BOLD, 9);
    private final Font FONT_TOTAIS_TITULO = Font.font("Arial", FontWeight.BOLD, 12);
    private final Font FONT_TOTAIS_VALOR = Font.font("Arial", FontWeight.BOLD, 13);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunasTabela();
        carregarViagens();
        carregarRotas();
        configurarListeners();
    }

    private void configurarColunasTabela() {
        // Tabela de Fretes/Itens
        colCodFrete.setCellValueFactory(new PropertyValueFactory<>("codFrete"));
        colDataViagem.setCellValueFactory(new PropertyValueFactory<>("dataViagem"));
        colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("item"));
        colQuant.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        tabelaFretes.setItems(listaFreteItens);
        
        // Tabela de Devedores
        colDevTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colDevBaixado.setCellValueFactory(new PropertyValueFactory<>("baixado"));
        colDevDevedor.setCellValueFactory(new PropertyValueFactory<>("devedor"));
        colDevNumFrete.setCellValueFactory(new PropertyValueFactory<>("numFrete"));
        tabelaDevedores.setItems(listaDevedores);
    }

    private void carregarViagens() {
        try {
            List<Viagem> viagens = viagemDAO.listarTodasViagensResumido();
            cmbViagem.setItems(FXCollections.observableArrayList(viagens));
            
            Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
            if (viagemAtiva != null) {
                for (Viagem v : viagens) {
                    if (v.getId().equals(viagemAtiva.getId())) {
                        cmbViagem.setValue(v);
                        break;
                    }
                }
            } else if (!viagens.isEmpty()) {
                cmbViagem.setValue(viagens.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarRotas() {
        List<String> rotas = new ArrayList<>();
        rotas.add("Todas as Rotas");
        try {
            for (Rota r : rotaDAO.listarTodasAsRotasComoObjects()) {
                rotas.add(r.getOrigem() + " - " + r.getDestino());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cmbRota.setItems(FXCollections.observableArrayList(rotas));
        cmbRota.setValue("Todas as Rotas");
    }

    private void configurarListeners() {
        cmbViagem.valueProperty().addListener((obs, oldV, newV) -> {
            carregarClientes();
            carregarDevedores();
            carregarDados();
        });
        cmbRota.valueProperty().addListener((obs, oldV, newV) -> {
            carregarClientes();
            carregarDados();
        });
        cmbCliente.valueProperty().addListener((obs, oldV, newV) -> carregarDados());
        cmbDevedores.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isEmpty()) {
                cmbCliente.setValue(newV);
            }
        });
        
        // Carregar dados iniciais
        if (cmbViagem.getValue() != null) {
            carregarClientes();
            carregarDevedores();
            carregarDados();
        }
    }

    private void carregarClientes() {
        Viagem viagem = cmbViagem.getValue();
        if (viagem == null) return;
        
        List<String> clientes = new ArrayList<>();
        clientes.add("Todos os Clientes");
        
        String sql = "SELECT DISTINCT f.destinatario_nome_temp FROM fretes f WHERE f.id_viagem = ? AND f.destinatario_nome_temp IS NOT NULL ORDER BY f.destinatario_nome_temp";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String cliente = rs.getString("destinatario_nome_temp");
                if (cliente != null && !cliente.trim().isEmpty()) {
                    clientes.add(cliente);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cmbCliente.setItems(FXCollections.observableArrayList(clientes));
        cmbCliente.setValue("Todos os Clientes");
    }

    private void carregarDevedores() {
        Viagem viagem = cmbViagem.getValue();
        if (viagem == null) return;
        
        List<String> devedores = new ArrayList<>();
        
        String sql = "SELECT DISTINCT f.destinatario_nome_temp FROM fretes f WHERE f.id_viagem = ? AND f.valor_devedor > 0.01 AND f.destinatario_nome_temp IS NOT NULL ORDER BY f.destinatario_nome_temp";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String devedor = rs.getString("destinatario_nome_temp");
                if (devedor != null && !devedor.trim().isEmpty()) {
                    devedores.add(devedor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cmbDevedores.setItems(FXCollections.observableArrayList(devedores));
    }

    private void carregarDados() {
        Viagem viagem = cmbViagem.getValue();
        if (viagem == null) return;
        
        listaFreteItens.clear();
        listaDevedores.clear();
        
        String cliente = cmbCliente.getValue();
        boolean filtrarCliente = cliente != null && !cliente.equals("Todos os Clientes");
        
        // Carregar itens dos fretes
        StringBuilder sql = new StringBuilder(
            "SELECT f.numero_frete, v.data_viagem, f.remetente_nome_temp, " +
            "fi.descricao, fi.quantidade, fi.valor_unitario, (fi.quantidade * fi.valor_unitario) as total_item " +
            "FROM fretes f " +
            "JOIN frete_itens fi ON f.id_frete = fi.id_frete " +
            "LEFT JOIN viagens v ON f.id_viagem = v.id_viagem " +
            "WHERE f.id_viagem = ? "
        );
        
        if (filtrarCliente) {
            sql.append(" AND f.destinatario_nome_temp = ? ");
        }
        sql.append(" ORDER BY f.numero_frete, fi.descricao");
        
        double totalItens = 0;
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            stmt.setLong(1, viagem.getId());
            if (filtrarCliente) {
                stmt.setString(2, cliente);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String codFrete = rs.getString("numero_frete");
                LocalDate dataViagem = rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toLocalDate() : null;
                String dataStr = dataViagem != null ? dataViagem.format(dateFormatter) : "";
                String remetente = rs.getString("remetente_nome_temp");
                String item = rs.getString("descricao");
                double quantidade = rs.getDouble("quantidade");
                double preco = rs.getDouble("valor_unitario");
                double total = rs.getDouble("total_item");
                
                listaFreteItens.add(new FreteItem(codFrete, dataStr, remetente, item, quantidade, preco, total));
                totalItens += total;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        lblTotalItens.setText(String.format("R$ %.2f", totalItens));
        
        // Carregar situação financeira
        StringBuilder sqlFin = new StringBuilder(
            "SELECT f.numero_frete, f.valor_total_itens, f.valor_pago, f.valor_devedor " +
            "FROM fretes f " +
            "WHERE f.id_viagem = ? "
        );
        if (filtrarCliente) {
            sqlFin.append(" AND f.destinatario_nome_temp = ? ");
        }
        sqlFin.append(" ORDER BY f.numero_frete");
        
        double totalEmAberto = 0;
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sqlFin.toString())) {
            stmt.setLong(1, viagem.getId());
            if (filtrarCliente) {
                stmt.setString(2, cliente);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String numFrete = rs.getString("numero_frete");
                double total = rs.getDouble("valor_total_itens");
                double pago = rs.getDouble("valor_pago");
                double devedor = rs.getDouble("valor_devedor");
                
                listaDevedores.add(new FreteDevedor(total, pago, devedor, numFrete));
                totalEmAberto += devedor;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        lblEmAberto.setText(String.format("R$ %.2f", totalEmAberto));
    }

    // ============================================================================
    // IMPRESSÕES
    // ============================================================================

    @FXML
    private void imprimirRelatorio(ActionEvent event) {
        // Relatório simples para impressora térmica: destinatário, data viagem, rota
        // Itens: apenas quantidade e descrição, total de volumes no final
        Viagem viagem = cmbViagem.getValue();
        String cliente = cmbCliente.getValue();
        
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        if (cliente == null || cliente.equals("Todos os Clientes")) {
            showAlert("Selecione um cliente específico para imprimir o relatório.");
            return;
        }
        
        imprimirRelatorioTermico(viagem, cliente, false);
    }

    @FXML
    private void imprimirCobranca(ActionEvent event) {
        // Igual relatório mas com valores unitários, total e soma
        Viagem viagem = cmbViagem.getValue();
        String cliente = cmbCliente.getValue();
        
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        if (cliente == null || cliente.equals("Todos os Clientes")) {
            showAlert("Selecione um cliente específico para imprimir a cobrança.");
            return;
        }
        
        imprimirRelatorioTermico(viagem, cliente, true);
    }

    private void imprimirRelatorioTermico(Viagem viagem, String cliente, boolean comValores) {
        // Usar impressora térmica configurada
        Printer printer = RelatorioUtil.getImpressoraTermica();
        if (printer == null) { showAlert("Nenhuma impressora térmica selecionada."); return; }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return;
        
        javafx.print.PageLayout pageLayout = printer.createPageLayout(
            printer.getDefaultPageLayout().getPaper(), 
            javafx.print.PageOrientation.PORTRAIT, 
            Printer.MarginType.HARDWARE_MINIMUM
        );
        job.getJobSettings().setPageLayout(pageLayout);
        
        EmpresaDAO empresaDAO = new EmpresaDAO();
        Empresa empresa = empresaDAO.buscarPorId(1);
        double larguraBase = 270;
        
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(larguraBase);
        root.setMaxWidth(larguraBase);
        root.setAlignment(Pos.TOP_LEFT);
        
        // ============ CABEÇALHO COM LOGO E DADOS DA EMPRESA ============
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(larguraBase);
        
        if (empresa != null) {
            if (empresa.getCaminhoFoto() != null && !empresa.getCaminhoFoto().isEmpty()) {
                try {
                    ImageView logo = new ImageView(new Image("file:" + empresa.getCaminhoFoto()));
                    logo.setFitWidth(50);
                    logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                } catch (Exception e) { }
            }
            Label lblEmpresa = new Label(empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "EMBARCAÇÃO");
            lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: black;");
            
            String dados = "";
            if (empresa.getCnpj() != null) dados += "CNPJ: " + empresa.getCnpj() + "\n";
            if (empresa.getTelefone() != null) dados += "Tel: " + empresa.getTelefone() + "\n";
            if (empresa.getEndereco() != null && !empresa.getEndereco().isEmpty()) dados += empresa.getEndereco();
            
            Label lblDados = new Label(dados);
            lblDados.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().addAll(lblEmpresa, lblDados);
        }
        root.getChildren().add(headerBox);
        
        // ============ TÍTULO E NÚMERO DO FRETE ============
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(10, 0, 5, 0));
        
        Label lblTitulo = new Label(comValores ? "COBRANÇA DE FRETE" : "RECIBO DE FRETE");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        
        // Buscar número do frete
        String numeroFrete = buscarNumeroFrete(viagem, cliente);
        Label lblNum = new Label("Nº " + numeroFrete);
        lblNum.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-border-color: black; -fx-border-width: 2px; -fx-padding: 3px 15px 3px 15px; -fx-text-fill: black;");
        
        infoBox.getChildren().addAll(lblTitulo, lblNum);
        root.getChildren().add(infoBox);
        
        // ============ DADOS DO CLIENTE ============
        VBox boxClientes = new VBox(2);
        boxClientes.setAlignment(Pos.CENTER_LEFT);
        
        String remetente = buscarRemetenteFrete(viagem, cliente);
        Label lblRem = new Label("REM: " + (remetente != null ? remetente : ""));
        lblRem.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        
        Label lblDest = new Label("DEST: " + cliente);
        lblDest.setStyle("-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;");
        
        String rotaStr = cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas as Rotas") ? cmbRota.getValue() : buscarRotaFrete(viagem);
        Label lblRota = new Label("ROTA: " + rotaStr);
        lblRota.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        
        lblRem.setWrapText(true);
        lblDest.setWrapText(true);
        boxClientes.getChildren().addAll(lblRem, lblDest, lblRota);
        boxClientes.setPadding(new Insets(5, 0, 5, 0));
        root.getChildren().add(boxClientes);
        
        // ============ TABELA DE ITENS COM BORDAS ============
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        
        String styleHeaderBase = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
        String styleHeaderFirst = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 1;";
        String styleHeaderLast = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
        String styleCellFirst = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 1;";
        String styleCellNormal = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
        String styleCellLast = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
        
        double wQtd = 30, wDesc = 110, wUnit = 60, wTotal = 70;
        
        // Headers
        Label hQtd = new Label("QTD"); hQtd.setStyle(styleHeaderFirst); hQtd.setPrefWidth(wQtd); hQtd.setAlignment(Pos.CENTER);
        Label hDesc = new Label("DESC."); hDesc.setStyle(styleHeaderBase); hDesc.setPrefWidth(wDesc); hDesc.setAlignment(Pos.CENTER_LEFT);
        
        if (comValores) {
            Label hUnit = new Label("V.UN"); hUnit.setStyle(styleHeaderBase); hUnit.setPrefWidth(wUnit); hUnit.setAlignment(Pos.CENTER_RIGHT);
            Label hTotal = new Label("TOTAL"); hTotal.setStyle(styleHeaderLast); hTotal.setPrefWidth(wTotal); hTotal.setAlignment(Pos.CENTER_RIGHT);
            grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);
        } else {
            hDesc.setPrefWidth(wDesc + wUnit + wTotal);
            grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0);
        }
        
        // Buscar itens
        double totalGeral = 0;
        int totalVolumes = 0;
        int linha = 1;
        
        String sql = "SELECT fi.quantidade, fi.descricao, fi.valor_unitario, (fi.quantidade * fi.valor_unitario) as total_item " +
                     "FROM fretes f " +
                     "JOIN frete_itens fi ON f.id_frete = fi.id_frete " +
                     "WHERE f.id_viagem = ? AND f.destinatario_nome_temp = ? " +
                     "ORDER BY f.numero_frete, fi.descricao";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                double qtd = rs.getDouble("quantidade");
                String desc = rs.getString("descricao");
                double unit = rs.getDouble("valor_unitario");
                double total = rs.getDouble("total_item");
                
                totalVolumes += (int) qtd;
                totalGeral += total;
                
                Label q = new Label(String.valueOf((int) qtd));
                q.setStyle(styleCellFirst); q.setPrefWidth(wQtd); q.setAlignment(Pos.CENTER);
                
                Label d = new Label(desc);
                d.setStyle(styleCellNormal); d.setPrefWidth(wDesc); d.setWrapText(true); d.setAlignment(Pos.CENTER_LEFT);
                
                if (comValores) {
                    Label vu = new Label(String.format("%,.2f", unit));
                    vu.setStyle(styleCellNormal); vu.setPrefWidth(wUnit); vu.setAlignment(Pos.CENTER_RIGHT);
                    
                    Label vt = new Label(String.format("%,.2f", total));
                    vt.setStyle(styleCellLast); vt.setPrefWidth(wTotal); vt.setAlignment(Pos.CENTER_RIGHT);
                    
                    grid.add(q, 0, linha); grid.add(d, 1, linha); grid.add(vu, 2, linha); grid.add(vt, 3, linha);
                } else {
                    d.setPrefWidth(wDesc + wUnit + wTotal);
                    grid.add(q, 0, linha); grid.add(d, 1, linha);
                }
                linha++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        root.getChildren().add(grid);
        
        // ============ VOLUMES ============
        Label lblVol = new Label("VOLUMES: " + totalVolumes);
        lblVol.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        
        Label espaco = new Label(" ");
        espaco.setMinHeight(25);
        root.getChildren().addAll(lblVol, espaco);
        
        // ============ VALORES E STATUS ============
        VBox boxValores = new VBox(3);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        boxValores.setPadding(new Insets(0, 5, 0, 0));
        
        // Buscar valores financeiros
        double[] valoresFin = buscarValoresFinanceiros(viagem, cliente);
        double valorTotal = valoresFin[0];
        double valorPago = valoresFin[1];
        double valorDevedor = valoresFin[2];
        
        if (comValores) {
            Label lblTotal = new Label("TOTAL: R$ " + String.format("%,.2f", valorTotal > 0 ? valorTotal : totalGeral));
            lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: black;");
            
            Label lblPago = new Label("PAGO: R$ " + String.format("%,.2f", valorPago));
            lblPago.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
            
            String statusTxt = (valorDevedor <= 0.01) ? "QUITADO" : "PENDENTE";
            Label lblStatus = new Label("STATUS: " + statusTxt);
            lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;");
            
            boxValores.getChildren().addAll(lblTotal, lblPago, lblStatus);
        }
        
        root.getChildren().add(boxValores);
        
        // ============ RODAPÉ COM ASSINATURA ============
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle("-fx-font-size: 9px; -fx-text-fill: black;");
        lblData.setTextAlignment(TextAlignment.CENTER);
        
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);
        
        // Ajustar escala se necessário
        double largImp = pageLayout.getPrintableWidth();
        if (largImp > 0 && largImp < larguraBase) {
            double sc = largImp / larguraBase;
            root.getTransforms().add(new Scale(sc, sc));
        }
        
        if (job.printPage(root)) job.endJob();
        else showAlert("Falha na impressão.");
    }
    
    private String buscarNumeroFrete(Viagem viagem, String cliente) {
        String sql = "SELECT numero_frete FROM fretes WHERE id_viagem = ? AND destinatario_nome_temp = ? LIMIT 1";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("numero_frete");
        } catch (Exception e) { e.printStackTrace(); }
        return "---";
    }
    
    private String buscarRemetenteFrete(Viagem viagem, String cliente) {
        String sql = "SELECT remetente_nome_temp FROM fretes WHERE id_viagem = ? AND destinatario_nome_temp = ? LIMIT 1";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("remetente_nome_temp");
        } catch (Exception e) { e.printStackTrace(); }
        return "";
    }
    
    private String buscarRotaFrete(Viagem viagem) {
        String sql = "SELECT r.origem, r.destino FROM viagens v JOIN rotas r ON v.id_rota = r.id WHERE v.id_viagem = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("origem") + " - " + rs.getString("destino");
        } catch (Exception e) { e.printStackTrace(); }
        return "--";
    }
    
    private double[] buscarValoresFinanceiros(Viagem viagem, String cliente) {
        double total = 0, pago = 0, devedor = 0;
        String sql = "SELECT SUM(valor_total_itens) as total, SUM(valor_pago) as pago, SUM(valor_devedor) as devedor " +
                     "FROM fretes WHERE id_viagem = ? AND destinatario_nome_temp = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
                pago = rs.getDouble("pago");
                devedor = rs.getDouble("devedor");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new double[]{total, pago, devedor};
    }

    @FXML
    private void imprimirGeral(ActionEvent event) {
        // Papel A4, cabeçalho com logo, todos os clientes, todos os remetentes
        Viagem viagem = cmbViagem.getValue();
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        
        imprimirRelatorioA4Geral(viagem);
    }

    private void imprimirRelatorioA4Geral(Viagem viagem) {
        // Perguntar filtros
        Alert alertFiltro = new Alert(Alert.AlertType.CONFIRMATION);
        alertFiltro.setTitle("Filtros");
        alertFiltro.setHeaderText("O que deseja imprimir?");
        ButtonType btnTudo = new ButtonType("Tudo");
        ButtonType btnPendentes = new ButtonType("Só Pendentes");
        ButtonType btnFinanceiro = new ButtonType("Falta Pagar");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alertFiltro.getButtonTypes().setAll(btnTudo, btnPendentes, btnFinanceiro, btnCancelar);
        Optional<ButtonType> res = alertFiltro.showAndWait();
        if (!res.isPresent() || res.get() == btnCancelar) return;

        int tipoFiltro = 0;
        String tituloRelatorio = "RELATÓRIO GERAL";
        if (res.get() == btnPendentes) { tipoFiltro = 1; tituloRelatorio = "RELATÓRIO DE PENDÊNCIAS DE ENTREGA"; }
        else if (res.get() == btnFinanceiro) { tipoFiltro = 2; tituloRelatorio = "RELATÓRIO DE PENDÊNCIAS FINANCEIRAS"; }

        String nomeRota = "TODAS AS ROTAS";
        if (cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas as Rotas")) {
            nomeRota = cmbRota.getValue().toUpperCase();
        }

        // Usar impressora A4 configurada
        Printer printer = RelatorioUtil.getImpressoraA4();
        if (printer == null) { showAlert("Nenhuma impressora A4 selecionada."); return; }
        
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(rootPane.getScene().getWindow())) return;
        
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        double larguraUtil = pageLayout.getPrintableWidth() - 40;
        double alturaUtil = pageLayout.getPrintableHeight();
        
        // Buscar fretes agrupados por número de frete (destinatário)
        Map<String, FreteCompleto> fretesMap = new LinkedHashMap<>();
        
        String sql = "SELECT f.id_frete, f.numero_frete, f.destinatario_nome_temp, f.remetente_nome_temp, " +
                     "f.valor_total_itens, f.valor_pago, f.valor_devedor, " +
                     "fi.descricao, fi.quantidade, fi.valor_unitario, (fi.quantidade * fi.valor_unitario) as total_item " +
                     "FROM fretes f " +
                     "JOIN frete_itens fi ON f.id_frete = fi.id_frete " +
                     "WHERE f.id_viagem = ? " +
                     "ORDER BY f.numero_frete, fi.descricao";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String numFrete = rs.getString("numero_frete");
                
                FreteCompleto frete = fretesMap.get(numFrete);
                if (frete == null) {
                    frete = new FreteCompleto();
                    frete.numeroFrete = numFrete;
                    frete.remetente = rs.getString("remetente_nome_temp");
                    frete.destinatario = rs.getString("destinatario_nome_temp");
                    frete.valorTotal = rs.getDouble("valor_total_itens");
                    frete.valorPago = rs.getDouble("valor_pago");
                    frete.valorDevedor = rs.getDouble("valor_devedor");
                    frete.itens = new ArrayList<>();
                    fretesMap.put(numFrete, frete);
                }
                
                ItemFrete item = new ItemFrete();
                item.descricao = rs.getString("descricao");
                item.quantidade = rs.getDouble("quantidade");
                item.valorUnitario = rs.getDouble("valor_unitario");
                item.valorTotal = rs.getDouble("total_item");
                frete.itens.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Aplicar filtros
        List<FreteCompleto> listaFiltrada = new ArrayList<>();
        for (FreteCompleto f : fretesMap.values()) {
            if (tipoFiltro == 1) { /* Pendentes - não implementado para fretes */ listaFiltrada.add(f); }
            else if (tipoFiltro == 2) { if (f.valorDevedor > 0.01) listaFiltrada.add(f); }
            else { listaFiltrada.add(f); }
        }
        
        if (listaFiltrada.isEmpty()) { showAlert("Nenhum frete encontrado com os filtros selecionados."); return; }
        
        // Calcular totais
        double tTotal = 0, tPago = 0, tDevedor = 0;
        for (FreteCompleto f : listaFiltrada) {
            tTotal += f.valorTotal;
            tPago += f.valorPago;
            tDevedor += f.valorDevedor;
        }
        
        // Criar páginas
        List<VBox> paginas = new ArrayList<>();
        int numPagina = 1;
        VBox paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, tituloRelatorio, true, nomeRota);
        double alturaAtual = 150;
        
        for (FreteCompleto frete : listaFiltrada) {
            VBox bloco = criarBlocoFrete(frete, larguraUtil);
            
            Scene tempScene = new Scene(bloco);
            bloco.applyCss();
            bloco.layout();
            double alturaBloco = bloco.getBoundsInLocal().getHeight();
            
            if (alturaAtual + alturaBloco > alturaUtil - 100) {
                adicionarRodapeA4(paginaAtual, larguraUtil);
                paginas.add(paginaAtual);
                numPagina++;
                paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, tituloRelatorio, false, nomeRota);
                alturaAtual = 60;
            }
            
            paginaAtual.getChildren().add(bloco);
            alturaAtual += alturaBloco;
        }
        
        // Bloco de totais
        VBox blocoTotais = criarBlocoTotaisGerais(tTotal, tPago, tDevedor, larguraUtil);
        Scene tempT = new Scene(blocoTotais);
        blocoTotais.applyCss();
        blocoTotais.layout();
        
        if (alturaAtual + blocoTotais.getBoundsInLocal().getHeight() > alturaUtil - 100) {
            adicionarRodapeA4(paginaAtual, larguraUtil);
            paginas.add(paginaAtual);
            numPagina++;
            paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, tituloRelatorio, false, nomeRota);
        }
        paginaAtual.getChildren().add(blocoTotais);
        
        adicionarRodapeA4(paginaAtual, larguraUtil);
        paginas.add(paginaAtual);
        
        // Imprimir todas as páginas
        for (VBox p : paginas) {
            job.printPage(pageLayout, p);
        }
        job.endJob();
    }
    
    // Classe auxiliar para frete completo
    private static class FreteCompleto {
        String numeroFrete, remetente, destinatario;
        double valorTotal, valorPago, valorDevedor;
        List<ItemFrete> itens;
    }
    
    private static class ItemFrete {
        String descricao;
        double quantidade, valorUnitario, valorTotal;
    }
    
    private VBox criarBlocoFrete(FreteCompleto frete, double larguraTotal) {
        VBox box = new VBox(0);
        box.setPrefWidth(larguraTotal);
        box.setMaxWidth(larguraTotal);
        box.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0; -fx-padding: 5 0 5 0;");
        
        // Header azul com número do frete, remetente, destinatário e status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(3, 5, 3, 5));
        header.setStyle("-fx-background-color: " + COR_AZUL_ESCURO + ";");
        header.setPrefWidth(larguraTotal);
        
        Label lNum = new Label("FRETE " + frete.numeroFrete);
        lNum.setFont(FONT_TITULO_FRETE);
        lNum.setTextFill(Color.WHITE);
        lNum.setMinWidth(80);
        
        VBox boxNomes = new VBox(2);
        boxNomes.setAlignment(Pos.CENTER_LEFT);
        
        Label lRem = new Label("REMETENTE:    " + (frete.remetente != null ? frete.remetente : ""));
        lRem.setFont(FONT_NEGRITO);
        lRem.setTextFill(Color.WHITE);
        
        Label lDest = new Label("DESTINATÁRIO: " + (frete.destinatario != null ? frete.destinatario : ""));
        lDest.setFont(FONT_NEGRITO);
        lDest.setTextFill(Color.WHITE);
        
        boxNomes.getChildren().addAll(lRem, lDest);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String stTxt = (frete.valorDevedor > 0.01) ? "FALTA PAGAR" : "PAGO";
        Label lSt = new Label(stTxt);
        lSt.setFont(FONT_NEGRITO);
        lSt.setTextFill(stTxt.equals("PAGO") ? Color.LIGHTGREEN : Color.ORANGE);
        
        header.getChildren().addAll(lNum, boxNomes, spacer, lSt);
        box.getChildren().add(header);
        
        // Grid de itens
        if (frete.itens != null && !frete.itens.isEmpty()) {
            GridPane grid = new GridPane();
            grid.setPadding(new Insets(12, 5, 5, 5));
            grid.setHgap(10);
            grid.setVgap(8);
            grid.setPrefWidth(larguraTotal);
            
            ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(8);
            ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(57);
            ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(17);
            ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(18);
            grid.getColumnConstraints().addAll(c1, c2, c3, c4);
            
            // Headers
            Label hQtd = new Label("QTD"); hQtd.setFont(FONT_HEADER_ITENS); hQtd.setTextFill(Color.DARKGRAY);
            Label hDesc = new Label("DESCRIÇÃO"); hDesc.setFont(FONT_HEADER_ITENS); hDesc.setTextFill(Color.DARKGRAY);
            Label hUnit = new Label("VL UNIT"); hUnit.setFont(FONT_HEADER_ITENS); hUnit.setTextFill(Color.DARKGRAY); hUnit.setMaxWidth(Double.MAX_VALUE); hUnit.setAlignment(Pos.CENTER_RIGHT);
            Label hTotal = new Label("VL TOTAL"); hTotal.setFont(FONT_HEADER_ITENS); hTotal.setTextFill(Color.DARKGRAY); hTotal.setMaxWidth(Double.MAX_VALUE); hTotal.setAlignment(Pos.CENTER_RIGHT);
            
            grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);
            
            int row = 1;
            for (ItemFrete it : frete.itens) {
                Label q = new Label(String.format("%.0fx", it.quantidade)); q.setFont(FONT_NORMAL);
                Label d = new Label(it.descricao); d.setFont(FONT_NORMAL); d.setWrapText(true);
                Label u = new Label(fmtMoeda(it.valorUnitario)); u.setFont(FONT_NORMAL); u.setAlignment(Pos.CENTER_RIGHT); u.setMaxWidth(Double.MAX_VALUE);
                Label t = new Label(fmtMoeda(it.valorTotal)); t.setFont(FONT_NORMAL); t.setAlignment(Pos.CENTER_RIGHT); t.setMaxWidth(Double.MAX_VALUE);
                grid.add(q, 0, row); grid.add(d, 1, row); grid.add(u, 2, row); grid.add(t, 3, row);
                row++;
            }
            box.getChildren().add(grid);
        }
        
        // Rodapé com assinatura e valores
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(5, 5, 5, 5));
        footer.setStyle("-fx-background-color: " + COR_CINZA_CLARO + ";");
        
        Label lAss = new Label("Assinatura: __________________________");
        lAss.setFont(FONT_NORMAL);
        
        Region r2 = new Region();
        HBox.setHgrow(r2, Priority.ALWAYS);
        
        VBox boxValores = new VBox(2);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        
        if (frete.valorDevedor > 0.01) {
            Label lTot = new Label("Total: " + fmtMoeda(frete.valorTotal));
            lTot.setFont(FONT_NEGRITO);
            boxValores.getChildren().add(lTot);
            
            if (frete.valorPago > 0.01) {
                Label lPago = new Label("Pago: " + fmtMoeda(frete.valorPago));
                lPago.setFont(FONT_NEGRITO);
                lPago.setTextFill(Color.FORESTGREEN);
                boxValores.getChildren().add(lPago);
            }
            
            Label lFalta = new Label("A Pagar: " + fmtMoeda(frete.valorDevedor));
            lFalta.setFont(FONT_NEGRITO);
            lFalta.setTextFill(Color.RED);
            boxValores.getChildren().add(lFalta);
        } else {
            Label lTot = new Label("TOTAL: " + fmtMoeda(frete.valorTotal) + " (PAGO)");
            lTot.setFont(FONT_NEGRITO);
            lTot.setTextFill(Color.FORESTGREEN);
            boxValores.getChildren().add(lTot);
        }
        
        footer.getChildren().addAll(lAss, r2, boxValores);
        box.getChildren().add(footer);
        
        return box;
    }
    
    private VBox criarPaginaBaseA4(double w, double h, int pag, Viagem viagem, String titulo, boolean isPrimeiraPagina, String nomeRota) {
        VBox p = new VBox(5);
        p.setPadding(new Insets(5));
        p.setPrefSize(w, h);
        p.setMaxWidth(w);
        p.setStyle("-fx-background-color: white;");
        
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPrefWidth(w);
        header.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
        
        if (isPrimeiraPagina) {
            String nomeEmp = obterNomeEmpresa();
            Label lEmp = new Label(nomeEmp.toUpperCase());
            lEmp.setFont(FONT_EMPRESA);
            
            Label lRota = new Label("ROTA: " + nomeRota);
            lRota.setFont(FONT_ROTA_DESTAQUE);
            lRota.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 3 10 3 10; -fx-background-radius: 3;");
            
            String dSaida = viagem.getDataViagem() != null ? viagem.getDataViagem().format(dateFormatter) : "N/D";
            String dChegada = viagem.getDataChegada() != null ? viagem.getDataChegada().format(dateFormatter) : "EM ABERTO";
            Label lDatas = new Label("SAÍDA: " + dSaida + "  |  PREV. CHEGADA: " + dChegada);
            lDatas.setFont(FONT_DATAS);
            
            Label lTit = new Label(titulo);
            lTit.setFont(FONT_NORMAL);
            lTit.setTextFill(Color.DARKGRAY);
            
            header.getChildren().addAll(lEmp, lRota, lDatas, lTit);
        } else {
            Label lSimples = new Label(titulo + " (Continuação) - Página " + pag);
            lSimples.setFont(FONT_NEGRITO);
            header.getChildren().add(lSimples);
        }
        
        p.getChildren().add(header);
        return p;
    }
    
    private void adicionarRodapeA4(VBox p, double w) {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        p.getChildren().add(spacer);
        
        Label l = new Label("Impresso em: " + LocalDateTime.now().format(dateTimeFormatter));
        l.setFont(FONT_NORMAL);
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setPrefWidth(w);
        l.setStyle("-fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        p.getChildren().add(l);
    }
    
    private VBox criarBlocoTotaisGerais(double tTotal, double tPago, double tDevedor, double w) {
        VBox box = new VBox(8);
        box.setPrefWidth(w);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 5, 0, 0));
        box.setStyle("-fx-background-color: white;");
        VBox.setMargin(box, new Insets(40, 0, 10, 0));
        
        Label lblTit = new Label("RESUMO FINANCEIRO GERAL");
        lblTit.setFont(FONT_TITULO_FRETE);
        box.getChildren().add(lblTit);
        
        box.getChildren().add(criarLinhaTotalA4("TOTAL FRETES (LANÇADO):", tTotal, Color.web(COR_AZUL_ESCURO)));
        box.getChildren().add(criarLinhaTotalA4("TOTAL RECEBIDO (PAGO):", tPago, Color.FORESTGREEN));
        
        Color corDevedor = (tDevedor > 0.01) ? Color.RED : Color.BLACK;
        box.getChildren().add(criarLinhaTotalA4("TOTAL A RECEBER (FIADO):", tDevedor, corDevedor));
        
        return box;
    }
    
    private HBox criarLinhaTotalA4(String titulo, double valor, Color corValor) {
        HBox linha = new HBox(10);
        linha.setAlignment(Pos.CENTER_RIGHT);
        
        Label lTit = new Label(titulo);
        lTit.setFont(FONT_TOTAIS_TITULO);
        
        Label lVal = new Label(fmtMoeda(valor));
        lVal.setFont(FONT_TOTAIS_VALOR);
        lVal.setTextFill(corValor);
        
        if (corValor.equals(Color.RED)) lTit.setTextFill(Color.RED);
        
        linha.getChildren().addAll(lTit, lVal);
        return linha;
    }
    
    private String fmtMoeda(double v) {
        return "R$ " + String.format("%,.2f", v);
    }

    @FXML
    private void imprimirResumido(ActionEvent event) {
        // Igual cobrança mas separado por remetente
        Viagem viagem = cmbViagem.getValue();
        String cliente = cmbCliente.getValue();
        
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        if (cliente == null || cliente.equals("Todos os Clientes")) {
            showAlert("Selecione um cliente específico para imprimir o resumido.");
            return;
        }
        
        imprimirResumidoPorRemetente(viagem, cliente);
    }

    private void imprimirResumidoPorRemetente(Viagem viagem, String cliente) {
        // Usar impressora térmica configurada
        Printer printer = RelatorioUtil.getImpressoraTermica();
        if (printer == null) { showAlert("Nenhuma impressora térmica selecionada."); return; }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return;
        
        javafx.print.PageLayout pageLayout = printer.createPageLayout(
            printer.getDefaultPageLayout().getPaper(), 
            javafx.print.PageOrientation.PORTRAIT, 
            Printer.MarginType.HARDWARE_MINIMUM
        );
        job.getJobSettings().setPageLayout(pageLayout);
        
        EmpresaDAO empresaDAO = new EmpresaDAO();
        Empresa empresa = empresaDAO.buscarPorId(1);
        double larguraBase = 270;
        
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(larguraBase);
        root.setMaxWidth(larguraBase);
        root.setAlignment(Pos.TOP_LEFT);
        
        // ============ CABEÇALHO COM LOGO E DADOS DA EMPRESA ============
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(larguraBase);
        
        if (empresa != null) {
            if (empresa.getCaminhoFoto() != null && !empresa.getCaminhoFoto().isEmpty()) {
                try {
                    ImageView logo = new ImageView(new Image("file:" + empresa.getCaminhoFoto()));
                    logo.setFitWidth(50);
                    logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                } catch (Exception e) { }
            }
            Label lblEmpresa = new Label(empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "EMBARCAÇÃO");
            lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: black;");
            
            String dados = "";
            if (empresa.getCnpj() != null) dados += "CNPJ: " + empresa.getCnpj() + "\n";
            if (empresa.getTelefone() != null) dados += "Tel: " + empresa.getTelefone() + "\n";
            
            Label lblDados = new Label(dados);
            lblDados.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().addAll(lblEmpresa, lblDados);
        }
        root.getChildren().add(headerBox);
        
        // ============ TÍTULO ============
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(10, 0, 5, 0));
        
        Label lblTitulo = new Label("RESUMIDO POR REMETENTE");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        
        infoBox.getChildren().add(lblTitulo);
        root.getChildren().add(infoBox);
        
        // ============ DADOS DO CLIENTE ============
        VBox boxClientes = new VBox(2);
        boxClientes.setAlignment(Pos.CENTER_LEFT);
        
        Label lblDest = new Label("DEST: " + cliente);
        lblDest.setStyle("-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;");
        
        String rotaStr = cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas as Rotas") ? cmbRota.getValue() : buscarRotaFrete(viagem);
        Label lblRota = new Label("ROTA: " + rotaStr);
        lblRota.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        
        lblDest.setWrapText(true);
        boxClientes.getChildren().addAll(lblDest, lblRota);
        boxClientes.setPadding(new Insets(5, 0, 5, 0));
        root.getChildren().add(boxClientes);
        
        // Separador
        Separator sep = new Separator();
        root.getChildren().add(sep);
        
        // Buscar itens agrupados por remetente
        Map<String, List<Map<String, Object>>> itensPorRemetente = new LinkedHashMap<>();
        
        String sql = "SELECT f.remetente_nome_temp, fi.descricao, fi.quantidade, fi.valor_unitario, " +
                     "(fi.quantidade * fi.valor_unitario) as total_item " +
                     "FROM fretes f " +
                     "JOIN frete_itens fi ON f.id_frete = fi.id_frete " +
                     "WHERE f.id_viagem = ? AND f.destinatario_nome_temp = ? " +
                     "ORDER BY f.remetente_nome_temp, fi.descricao";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String remetente = rs.getString("remetente_nome_temp");
                if (remetente == null) remetente = "SEM REMETENTE";
                
                Map<String, Object> item = new HashMap<>();
                item.put("descricao", rs.getString("descricao"));
                item.put("quantidade", rs.getDouble("quantidade"));
                item.put("valorUnit", rs.getDouble("valor_unitario"));
                item.put("total", rs.getDouble("total_item"));
                
                itensPorRemetente.computeIfAbsent(remetente, k -> new ArrayList<>()).add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        double totalGeral = 0;
        int totalVolumes = 0;
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : itensPorRemetente.entrySet()) {
            String remetente = entry.getKey();
            List<Map<String, Object>> itens = entry.getValue();
            
            // Nome do remetente
            Label lblRem = new Label("REM: " + remetente);
            lblRem.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-background-color: #e0e0e0; -fx-padding: 2;");
            lblRem.setWrapText(true);
            root.getChildren().add(lblRem);
            
            double subtotal = 0;
            int subVolumes = 0;
            
            for (Map<String, Object> item : itens) {
                double qtd = (Double) item.get("quantidade");
                String desc = (String) item.get("descricao");
                double unit = (Double) item.get("valorUnit");
                double tot = (Double) item.get("total");
                subtotal += tot;
                subVolumes += (int) qtd;
                
                String linha = String.format("  %.0fx %s R$%.2f", qtd, desc, tot);
                Label lblItem = new Label(linha);
                lblItem.setStyle("-fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
                lblItem.setWrapText(true);
                root.getChildren().add(lblItem);
            }
            
            Label lblSub = new Label(String.format("  Subtotal: R$ %.2f (%d vol)", subtotal, subVolumes));
            lblSub.setStyle("-fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;");
            root.getChildren().add(lblSub);
            root.getChildren().add(new Separator());
            
            totalGeral += subtotal;
            totalVolumes += subVolumes;
        }
        
        // ============ VOLUMES E TOTAL ============
        Label lblVol = new Label("VOLUMES: " + totalVolumes);
        lblVol.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        root.getChildren().add(lblVol);
        
        // ============ VALORES E STATUS ============
        VBox boxValores = new VBox(3);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        boxValores.setPadding(new Insets(10, 5, 0, 0));
        
        double[] valoresFin = buscarValoresFinanceiros(viagem, cliente);
        double valorTotal = valoresFin[0] > 0 ? valoresFin[0] : totalGeral;
        double valorPago = valoresFin[1];
        double valorDevedor = valoresFin[2];
        
        Label lblTotal = new Label("TOTAL: R$ " + String.format("%,.2f", valorTotal));
        lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: black;");
        
        Label lblPago = new Label("PAGO: R$ " + String.format("%,.2f", valorPago));
        lblPago.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        
        String statusTxt = (valorDevedor <= 0.01) ? "QUITADO" : "PENDENTE";
        Label lblStatus = new Label("STATUS: " + statusTxt);
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;");
        
        boxValores.getChildren().addAll(lblTotal, lblPago, lblStatus);
        root.getChildren().add(boxValores);
        
        // ============ RODAPÉ COM ASSINATURA ============
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle("-fx-font-size: 9px; -fx-text-fill: black;");
        lblData.setTextAlignment(TextAlignment.CENTER);
        
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);
        
        // Ajustar escala se necessário
        double largImp = pageLayout.getPrintableWidth();
        if (largImp > 0 && largImp < larguraBase) {
            double sc = largImp / larguraBase;
            root.getTransforms().add(new Scale(sc, sc));
        }
        
        if (job.printPage(root)) job.endJob();
        else showAlert("Falha na impressão.");
    }

    @FXML
    private void imprimirConfereViagem(ActionEvent event) {
        // Papel A4, todos os itens por cliente, separado por remetente, local para assinatura
        Viagem viagem = cmbViagem.getValue();
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        
        imprimirConfereViagemA4(viagem);
    }

    private void imprimirConfereViagemA4(Viagem viagem) {
        String nomeRota = "TODAS AS ROTAS";
        if (cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas as Rotas")) {
            nomeRota = cmbRota.getValue().toUpperCase();
        }
        
        // Usar impressora A4 configurada
        Printer printer = RelatorioUtil.getImpressoraA4();
        if (printer == null) { showAlert("Nenhuma impressora A4 selecionada."); return; }
        
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null || !job.showPrintDialog(rootPane.getScene().getWindow())) return;
        
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        double larguraUtil = pageLayout.getPrintableWidth() - 40;
        double alturaUtil = pageLayout.getPrintableHeight();
        
        // Buscar fretes agrupados por número de frete
        Map<String, FreteCompleto> fretesMap = new LinkedHashMap<>();
        
        String sql = "SELECT f.id_frete, f.numero_frete, f.destinatario_nome_temp, f.remetente_nome_temp, " +
                     "f.valor_total_itens, f.valor_pago, f.valor_devedor, " +
                     "fi.descricao, fi.quantidade, fi.valor_unitario, (fi.quantidade * fi.valor_unitario) as total_item " +
                     "FROM fretes f " +
                     "JOIN frete_itens fi ON f.id_frete = fi.id_frete " +
                     "WHERE f.id_viagem = ? " +
                     "ORDER BY f.numero_frete, fi.descricao";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String numFrete = rs.getString("numero_frete");
                
                FreteCompleto frete = fretesMap.get(numFrete);
                if (frete == null) {
                    frete = new FreteCompleto();
                    frete.numeroFrete = numFrete;
                    frete.remetente = rs.getString("remetente_nome_temp");
                    frete.destinatario = rs.getString("destinatario_nome_temp");
                    frete.valorTotal = rs.getDouble("valor_total_itens");
                    frete.valorPago = rs.getDouble("valor_pago");
                    frete.valorDevedor = rs.getDouble("valor_devedor");
                    frete.itens = new ArrayList<>();
                    fretesMap.put(numFrete, frete);
                }
                
                ItemFrete item = new ItemFrete();
                item.descricao = rs.getString("descricao");
                item.quantidade = rs.getDouble("quantidade");
                item.valorUnitario = rs.getDouble("valor_unitario");
                item.valorTotal = rs.getDouble("total_item");
                frete.itens.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (fretesMap.isEmpty()) { showAlert("Nenhum frete encontrado para esta viagem."); return; }
        
        // Calcular totais
        double tTotal = 0, tPago = 0, tDevedor = 0;
        for (FreteCompleto f : fretesMap.values()) {
            tTotal += f.valorTotal;
            tPago += f.valorPago;
            tDevedor += f.valorDevedor;
        }
        
        // Criar páginas
        List<VBox> paginas = new ArrayList<>();
        int numPagina = 1;
        VBox paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, "CONFERÊNCIA DE VIAGEM - FRETES", true, nomeRota);
        double alturaAtual = 150;
        
        for (FreteCompleto frete : fretesMap.values()) {
            VBox bloco = criarBlocoFrete(frete, larguraUtil);
            
            Scene tempScene = new Scene(bloco);
            bloco.applyCss();
            bloco.layout();
            double alturaBloco = bloco.getBoundsInLocal().getHeight();
            
            if (alturaAtual + alturaBloco > alturaUtil - 100) {
                adicionarRodapeA4(paginaAtual, larguraUtil);
                paginas.add(paginaAtual);
                numPagina++;
                paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, "CONFERÊNCIA DE VIAGEM - FRETES", false, nomeRota);
                alturaAtual = 60;
            }
            
            paginaAtual.getChildren().add(bloco);
            alturaAtual += alturaBloco;
        }
        
        // Bloco de totais
        VBox blocoTotais = criarBlocoTotaisGerais(tTotal, tPago, tDevedor, larguraUtil);
        Scene tempT = new Scene(blocoTotais);
        blocoTotais.applyCss();
        blocoTotais.layout();
        
        if (alturaAtual + blocoTotais.getBoundsInLocal().getHeight() > alturaUtil - 100) {
            adicionarRodapeA4(paginaAtual, larguraUtil);
            paginas.add(paginaAtual);
            numPagina++;
            paginaAtual = criarPaginaBaseA4(larguraUtil, alturaUtil, numPagina, viagem, "CONFERÊNCIA DE VIAGEM - FRETES", false, nomeRota);
        }
        paginaAtual.getChildren().add(blocoTotais);
        
        adicionarRodapeA4(paginaAtual, larguraUtil);
        paginas.add(paginaAtual);
        
        // Imprimir todas as páginas
        for (VBox p : paginas) {
            job.printPage(pageLayout, p);
        }
        job.endJob();
    }

    private String obterNomeEmpresa() {
        String nome = "SISTEMA DE FRETES";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT nome_embarcacao FROM configuracao_empresa LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nome = rs.getString("nome_embarcacao");
            }
        } catch (Exception e) {
            // Usar nome padrão
        }
        return nome;
    }

    @FXML
    private void imprimirExtrato(ActionEvent event) {
        // Extrato do cliente: histórico de todos os fretes com pagamentos
        Viagem viagem = cmbViagem.getValue();
        String cliente = cmbCliente.getValue();
        
        if (viagem == null) {
            showAlert("Selecione uma viagem.");
            return;
        }
        if (cliente == null || cliente.equals("Todos os Clientes")) {
            showAlert("Selecione um cliente específico para imprimir o extrato.");
            return;
        }
        
        imprimirExtratoCliente(viagem, cliente);
    }

    private void imprimirExtratoCliente(Viagem viagem, String cliente) {
        // Usar impressora térmica configurada
        Printer printer = RelatorioUtil.getImpressoraTermica();
        if (printer == null) { showAlert("Nenhuma impressora térmica selecionada."); return; }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return;
        
        javafx.print.PageLayout pageLayout = printer.createPageLayout(
            printer.getDefaultPageLayout().getPaper(), 
            javafx.print.PageOrientation.PORTRAIT, 
            Printer.MarginType.HARDWARE_MINIMUM
        );
        job.getJobSettings().setPageLayout(pageLayout);
        
        EmpresaDAO empresaDAO = new EmpresaDAO();
        Empresa empresa = empresaDAO.buscarPorId(1);
        double larguraBase = 270;
        
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(larguraBase);
        root.setMaxWidth(larguraBase);
        root.setAlignment(Pos.TOP_LEFT);
        
        // ============ CABEÇALHO COM LOGO E DADOS DA EMPRESA ============
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(larguraBase);
        
        if (empresa != null) {
            if (empresa.getCaminhoFoto() != null && !empresa.getCaminhoFoto().isEmpty()) {
                try {
                    ImageView logo = new ImageView(new Image("file:" + empresa.getCaminhoFoto()));
                    logo.setFitWidth(50);
                    logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                } catch (Exception e) { }
            }
            Label lblEmpresa = new Label(empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "EMBARCAÇÃO");
            lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: black;");
            
            String dados = "";
            if (empresa.getCnpj() != null) dados += "CNPJ: " + empresa.getCnpj() + "\n";
            if (empresa.getTelefone() != null) dados += "Tel: " + empresa.getTelefone() + "\n";
            if (empresa.getEndereco() != null && !empresa.getEndereco().isEmpty()) dados += empresa.getEndereco();
            
            Label lblDados = new Label(dados);
            lblDados.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().addAll(lblEmpresa, lblDados);
        }
        root.getChildren().add(headerBox);
        
        // ============ TÍTULO ============
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(10, 0, 5, 0));
        
        Label lblTitulo = new Label("EXTRATO DE FRETES");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        
        infoBox.getChildren().add(lblTitulo);
        root.getChildren().add(infoBox);
        
        // ============ DADOS DO CLIENTE ============
        VBox boxClientes = new VBox(2);
        boxClientes.setAlignment(Pos.CENTER_LEFT);
        
        Label lblDest = new Label("CLIENTE: " + cliente);
        lblDest.setStyle("-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;");
        
        String dataViagem = viagem.getDataViagem() != null ? viagem.getDataViagem().format(dateFormatter) : "N/D";
        Label lblViagem = new Label("VIAGEM: " + dataViagem);
        lblViagem.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        
        lblDest.setWrapText(true);
        boxClientes.getChildren().addAll(lblDest, lblViagem);
        boxClientes.setPadding(new Insets(5, 0, 5, 0));
        root.getChildren().add(boxClientes);
        
        // ============ TABELA DE FRETES COM BORDAS ============
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        
        String styleHeaderFirst = "-fx-padding: 2px; -fx-font-size: 8px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 1;";
        String styleHeaderBase = "-fx-padding: 2px; -fx-font-size: 8px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
        String styleCellFirst = "-fx-padding: 2px; -fx-font-size: 8px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 1;";
        String styleCellNormal = "-fx-padding: 2px; -fx-font-size: 8px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
        
        double wFrete = 50, wTotal = 65, wPago = 65, wSaldo = 65;
        
        // Headers
        Label hFrete = new Label("FRETE"); hFrete.setStyle(styleHeaderFirst); hFrete.setPrefWidth(wFrete); hFrete.setAlignment(Pos.CENTER);
        Label hTotal = new Label("TOTAL"); hTotal.setStyle(styleHeaderBase); hTotal.setPrefWidth(wTotal); hTotal.setAlignment(Pos.CENTER_RIGHT);
        Label hPago = new Label("PAGO"); hPago.setStyle(styleHeaderBase); hPago.setPrefWidth(wPago); hPago.setAlignment(Pos.CENTER_RIGHT);
        Label hSaldo = new Label("SALDO"); hSaldo.setStyle(styleHeaderBase); hSaldo.setPrefWidth(wSaldo); hSaldo.setAlignment(Pos.CENTER_RIGHT);
        
        grid.add(hFrete, 0, 0); grid.add(hTotal, 1, 0); grid.add(hPago, 2, 0); grid.add(hSaldo, 3, 0);
        
        // Buscar fretes do cliente
        double totalGeral = 0;
        double totalPago = 0;
        double totalSaldo = 0;
        int linha = 1;
        
        String sql = "SELECT f.numero_frete, f.valor_total_itens, f.valor_pago, f.valor_devedor " +
                     "FROM fretes f " +
                     "WHERE f.id_viagem = ? AND f.destinatario_nome_temp = ? " +
                     "ORDER BY f.numero_frete";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, viagem.getId());
            stmt.setString(2, cliente);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String numFrete = rs.getString("numero_frete");
                double total = rs.getDouble("valor_total_itens");
                double pago = rs.getDouble("valor_pago");
                double devedor = rs.getDouble("valor_devedor");
                
                totalGeral += total;
                totalPago += pago;
                totalSaldo += devedor;
                
                Label lFrete = new Label(numFrete != null ? numFrete : ""); 
                lFrete.setStyle(styleCellFirst); lFrete.setPrefWidth(wFrete); lFrete.setAlignment(Pos.CENTER);
                
                Label lTotal = new Label(String.format("%,.2f", total)); 
                lTotal.setStyle(styleCellNormal); lTotal.setPrefWidth(wTotal); lTotal.setAlignment(Pos.CENTER_RIGHT);
                
                Label lPago = new Label(String.format("%,.2f", pago)); 
                lPago.setStyle(styleCellNormal); lPago.setPrefWidth(wPago); lPago.setAlignment(Pos.CENTER_RIGHT);
                
                Label lSaldo = new Label(String.format("%,.2f", devedor)); 
                lSaldo.setStyle(styleCellNormal); lSaldo.setPrefWidth(wSaldo); lSaldo.setAlignment(Pos.CENTER_RIGHT);
                
                grid.add(lFrete, 0, linha); grid.add(lTotal, 1, linha); grid.add(lPago, 2, linha); grid.add(lSaldo, 3, linha);
                linha++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        root.getChildren().add(grid);
        
        // ============ ESPAÇO ============
        Label espaco = new Label(" ");
        espaco.setMinHeight(15);
        root.getChildren().add(espaco);
        
        // ============ VALORES E STATUS ============
        VBox boxValores = new VBox(3);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        boxValores.setPadding(new Insets(0, 5, 0, 0));
        
        Label lblTotalGeral = new Label("TOTAL: R$ " + String.format("%,.2f", totalGeral));
        lblTotalGeral.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: black;");
        
        Label lblTotalPago = new Label("PAGO: R$ " + String.format("%,.2f", totalPago));
        lblTotalPago.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        
        String statusTxt = (totalSaldo <= 0.01) ? "QUITADO" : "PENDENTE";
        Label lblStatus = new Label("STATUS: " + statusTxt);
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;");
        
        boxValores.getChildren().addAll(lblTotalGeral, lblTotalPago, lblStatus);
        root.getChildren().add(boxValores);
        
        // ============ RODAPÉ COM ASSINATURA ============
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle("-fx-font-size: 9px; -fx-text-fill: black;");
        lblData.setTextAlignment(TextAlignment.CENTER);
        
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);
        
        // Ajustar escala se necessário
        double largImp = pageLayout.getPrintableWidth();
        if (largImp > 0 && largImp < larguraBase) {
            double sc = largImp / larguraBase;
            root.getTransforms().add(new Scale(sc, sc));
        }
        
        if (job.printPage(root)) job.endJob();
        else showAlert("Falha na impressão.");
    }

    @FXML
    private void handleFechar(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    // ============================================================================
    // CLASSES INTERNAS
    // ============================================================================

    public static class FreteItem {
        private String codFrete, dataViagem, remetente, item, quantidade, preco, total;

        public FreteItem(String codFrete, String dataViagem, String remetente, String item, double quantidade, double preco, double total) {
            this.codFrete = codFrete;
            this.dataViagem = dataViagem;
            this.remetente = remetente != null ? remetente : "";
            this.item = item != null ? item : "";
            this.quantidade = String.format("%.0f", quantidade);
            this.preco = String.format("R$ %.2f", preco);
            this.total = String.format("R$ %.2f", total);
        }

        public String getCodFrete() { return codFrete; }
        public String getDataViagem() { return dataViagem; }
        public String getRemetente() { return remetente; }
        public String getItem() { return item; }
        public String getQuantidade() { return quantidade; }
        public String getPreco() { return preco; }
        public String getTotal() { return total; }
    }

    public static class FreteDevedor {
        private String total, baixado, devedor, numFrete;

        public FreteDevedor(double total, double baixado, double devedor, String numFrete) {
            this.total = String.format("R$ %.2f", total);
            this.baixado = String.format("R$ %.2f", baixado);
            this.devedor = String.format("R$ %.2f", devedor);
            this.numFrete = numFrete;
        }

        public String getTotal() { return total; }
        public String getBaixado() { return baixado; }
        public String getDevedor() { return devedor; }
        public String getNumFrete() { return numFrete; }
    }
}
