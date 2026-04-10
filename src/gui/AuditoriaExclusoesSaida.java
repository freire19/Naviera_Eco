package gui;

import dao.ConexaoBD;
import gui.util.CompanyDataLoader;
import gui.util.PrintLayoutHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import model.OpcaoViagem;
import gui.util.AppLogger;

public class AuditoriaExclusoesSaida {

    private TableView<RegistroAuditoria> tabela;
    private Stage stage;
    private ComboBox<OpcaoViagem> cmbSelecaoViagem;
    
    private int idViagemAtual;
    private String nomeViagemAtual;

    public void abrir(int idViagemInicial, String descricaoViagemInicial) {
        this.idViagemAtual = idViagemInicial;
        this.nomeViagemAtual = descricaoViagemInicial;
        
        stage = new Stage();
        stage.setTitle("Auditoria de Exclusões");
        stage.initModality(Modality.APPLICATION_MODAL);

        // --- CORREÇÃO DO ERRO CRÍTICO: CRIA A TABELA PRIMEIRO ---
        tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<RegistroAuditoria, String> colData = new TableColumn<>("DATA/HORA");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataHora"));
        colData.setPrefWidth(140);

        TableColumn<RegistroAuditoria, String> colSolicitante = new TableColumn<>("SOLICITANTE");
        colSolicitante.setCellValueFactory(new PropertyValueFactory<>("solicitante"));
        colSolicitante.setPrefWidth(130);
        
        TableColumn<RegistroAuditoria, String> colAutorizador = new TableColumn<>("AUTORIZADOR");
        colAutorizador.setCellValueFactory(new PropertyValueFactory<>("autorizador"));
        colAutorizador.setPrefWidth(130);
        
        TableColumn<RegistroAuditoria, String> colMotivo = new TableColumn<>("MOTIVO");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivo.setPrefWidth(220);

        TableColumn<RegistroAuditoria, String> colDetalhe = new TableColumn<>("ITEM EXCLUÍDO (DESCRIÇÃO | VALOR)");
        colDetalhe.setCellValueFactory(new PropertyValueFactory<>("detalhe"));
        colDetalhe.setPrefWidth(300);

        tabela.getColumns().addAll(colData, colSolicitante, colAutorizador, colMotivo, colDetalhe);
        tabela.setStyle("-fx-font-size: 11px;");

        // --- CABEÇALHO ---
        VBox header = new VBox(5);
        header.setStyle("-fx-background-color: #059669; -fx-padding: 15;");
        header.setAlignment(Pos.CENTER);

        Label lblTitulo = new Label("REGISTRO DE EXCLUSÕES - SAÍDAS");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        HBox boxSelecao = new HBox(10);
        boxSelecao.setAlignment(Pos.CENTER);
        
        Label lblRef = new Label("REFERENTE À VIAGEM:");
        lblRef.setTextFill(Color.WHITE);
        lblRef.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        cmbSelecaoViagem = new ComboBox<>();
        cmbSelecaoViagem.setPrefWidth(300);
        cmbSelecaoViagem.setStyle("-fx-font-weight: bold;");
        
        // Listener: Quando mudar a viagem no combo, recarrega a tabela
        cmbSelecaoViagem.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.idViagemAtual = newVal.id;
                this.nomeViagemAtual = newVal.label;
                carregarDados(); 
                stage.setTitle("Auditoria de Exclusões - " + newVal.label);
            }
        });

        // DR117: background thread para nao bloquear FX thread ao carregar viagens
        Thread bg = new Thread(() -> {
            try {
                ObservableList<OpcaoViagem> lista = carregarViagensBg(idViagemInicial);
                Platform.runLater(() -> {
                    cmbSelecaoViagem.setItems(lista);
                    OpcaoViagem paraSelecionar = null;
                    for (OpcaoViagem ov : lista) {
                        if (ov.id == idViagemInicial) { paraSelecionar = ov; break; }
                    }
                    if (paraSelecionar != null) cmbSelecaoViagem.getSelectionModel().select(paraSelecionar);
                    else if (!lista.isEmpty()) cmbSelecaoViagem.getSelectionModel().selectFirst();
                    // Carrega os dados da viagem selecionada
                    carregarDados();
                });
            } catch (Exception e) {
                AppLogger.warn("AuditoriaExclusoesSaida", "Erro ao carregar viagens AuditoriaExclusoesSaida: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        boxSelecao.getChildren().addAll(lblRef, cmbSelecaoViagem);
        header.getChildren().addAll(lblTitulo, boxSelecao);

        // --- RODAPÉ ---
        HBox boxBotoes = new HBox(10);
        boxBotoes.setPadding(new Insets(10));
        boxBotoes.setAlignment(Pos.CENTER_RIGHT);
        boxBotoes.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        Button btnImprimir = new Button("IMPRIMIR RELATÓRIO");
        btnImprimir.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold;");
        btnImprimir.setOnAction(e -> imprimirRelatorioAuditoria());

        Button btnLimparHistorico = new Button("APAGAR REGISTRO");
        btnLimparHistorico.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLimparHistorico.setOnAction(e -> apagarRegistroSelecionado());

        Button btnFechar = new Button("FECHAR");
        btnFechar.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold;");
        btnFechar.setOnAction(e -> stage.close());

        boxBotoes.getChildren().addAll(btnImprimir, btnLimparHistorico, btnFechar);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabela);
        root.setBottom(boxBotoes);

        Scene scene = new Scene(root, 1100, 550);
        stage.setScene(scene);
        stage.showAndWait();
    }
    
    // DR117: versao que retorna dados para ser chamada do background thread
    private ObservableList<OpcaoViagem> carregarViagensBg(int idInicial) {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String sql = "SELECT id_viagem, descricao, data_viagem FROM viagens WHERE empresa_id = ? ORDER BY data_viagem DESC";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, dao.DAOUtils.empresaId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id_viagem");
                String desc = rs.getString("descricao");
                java.sql.Date dt = rs.getDate("data_viagem");
                if (dt != null) desc += " (" + sdf.format(dt) + ")";
                lista.add(new OpcaoViagem(id, desc));
            }
        } catch (Exception e) { AppLogger.error("AuditoriaExclusoesSaida", e.getMessage(), e); }
        return lista;
    }

    private void carregarViagens(int idInicial) {
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                ObservableList<OpcaoViagem> lista = carregarViagensBg(idInicial);
                Platform.runLater(() -> {
                    cmbSelecaoViagem.setItems(lista);
                    OpcaoViagem paraSelecionar = null;
                    for (OpcaoViagem ov : lista) {
                        if (ov.id == idInicial) { paraSelecionar = ov; break; }
                    }
                    if (paraSelecionar != null) cmbSelecaoViagem.getSelectionModel().select(paraSelecionar);
                    else if (!lista.isEmpty()) cmbSelecaoViagem.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                AppLogger.warn("AuditoriaExclusoesSaida", "Erro ao carregar viagens AuditoriaExclusoesSaida: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // DR117: background thread para nao bloquear FX thread
    private void carregarDados() {
        if (tabela == null) return;
        final int idViagem = this.idViagemAtual;
        Thread bg = new Thread(() -> {
            try {
                ObservableList<RegistroAuditoria> lista = FXCollections.observableArrayList();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                // DL050: buscar tanto por coluna 'acao' quanto 'tipo_operacao' (padronizacao pendente)
                String sql = "SELECT * FROM auditoria_financeiro WHERE (acao = 'EXCLUSAO_DESPESA' OR tipo_operacao = 'EXCLUSAO_BOLETO') AND id_viagem = ? AND empresa_id = ? ORDER BY data_hora DESC";
                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, idViagem);
                    stmt.setInt(2, dao.DAOUtils.empresaId());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String responsaveis = rs.getString("usuario");
                        String solicitante = "N/D";
                        String autorizador = responsaveis;
                        if (responsaveis != null && responsaveis.contains("/")) {
                            String[] partes = responsaveis.split("/");
                            if (partes.length >= 2) { solicitante = partes[0].trim(); autorizador = partes[1].trim(); }
                        }
                        String detalheBruto = rs.getString("detalhe_valor");
                        String detalheLimpo = detalheBruto;
                        if (detalheBruto != null && detalheBruto.contains("| REF. VIAGEM")) {
                            detalheLimpo = detalheBruto.split("\\| REF. VIAGEM")[0].trim();
                        }
                        java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                        String dataFormatada = (ts != null) ? sdf.format(ts) : "—";
                        lista.add(new RegistroAuditoria(rs.getInt("id"), dataFormatada, solicitante, autorizador, rs.getString("motivo"), detalheLimpo));
                    }
                }
                Platform.runLater(() -> tabela.setItems(lista));
            } catch (Exception e) {
                AppLogger.warn("AuditoriaExclusoesSaida", "Erro ao carregar auditoria: " + e.getMessage());
                AppLogger.error("AuditoriaExclusoesSaida", e.getMessage(), e);
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    private void apagarRegistroSelecionado() {
        new Alert(Alert.AlertType.WARNING,
            "Registros de auditoria sao imutaveis e nao podem ser excluidos.\n" +
            "Esta restricao existe para garantir a integridade do historico financeiro."
        ).show();
    }

    private void imprimirRelatorioAuditoria() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(stage)) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(javafx.print.Paper.A4, javafx.print.PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double alturaUtilPagina = pageLayout.getPrintableHeight() - 80; 
            double larguraPagina = pageLayout.getPrintableWidth();

            java.util.List<VBox> paginas = new java.util.ArrayList<>();
            java.util.List<Label> labelsNumeracao = new java.util.ArrayList<>(); 

            VBox paginaAtual = new VBox(5);
            paginaAtual.setPrefWidth(larguraPagina);
            paginaAtual.setStyle("-fx-background-color: white; -fx-padding: 0;");
            paginaAtual.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            
            double alturaAtual = 0;

            VBox cabecalho = criarCabecalhoImpressao();
            paginaAtual.getChildren().add(cabecalho);
            alturaAtual += 100; 

            Label lblSub = new Label("AUDITORIA DE ITENS EXCLUÍDOS");
            lblSub.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 0 0 0;");
            
            Label lblViagem = new Label("VIAGEM: " + nomeViagemAtual);
            lblViagem.setStyle("-fx-font-size: 10px; -fx-padding: 0 0 10 0;");
            
            paginaAtual.getChildren().addAll(lblSub, lblViagem);
            alturaAtual += 50;

            GridPane gridTabela = new GridPane();
            configurarGridImpressao(gridTabela, larguraPagina);
            adicionarCabecalhoColunasImpressao(gridTabela);
            paginaAtual.getChildren().add(gridTabela);
            alturaAtual += 30; 

            ObservableList<RegistroAuditoria> itens = tabela.getItems();
            int rowGrid = 1; 

            for (RegistroAuditoria reg : itens) {
                double alturaLinha = 30; 
                if (reg.getMotivo().length() > 50 || reg.getDetalhe().length() > 50) alturaLinha = 50; 

                if (alturaAtual + alturaLinha > alturaUtilPagina) {
                    paginas.add(paginaAtual); 
                    paginaAtual = new VBox(5);
                    paginaAtual.setPrefWidth(larguraPagina);
                    paginaAtual.setStyle("-fx-background-color: white; -fx-padding: 20 0 0 0;");
                    paginaAtual.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                    Label lblNumPag = new Label("Página ?/?");
                    lblNumPag.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                    labelsNumeracao.add(lblNumPag);
                    paginaAtual.getChildren().add(lblNumPag);
                    gridTabela = new GridPane();
                    configurarGridImpressao(gridTabela, larguraPagina);
                    adicionarCabecalhoColunasImpressao(gridTabela);
                    paginaAtual.getChildren().add(gridTabela);
                    alturaAtual += 50; 
                    rowGrid = 1; 
                }

                String corFundo = (rowGrid % 2 == 0) ? "#f2f2f2" : "#ffffff";
                adicionarLinhaGridImpressao(gridTabela, reg, rowGrid, corFundo);
                alturaAtual += alturaLinha;
                rowGrid++;
            }
            paginas.add(paginaAtual);
            int totalPaginas = paginas.size();
            for (int i = 0; i < labelsNumeracao.size(); i++) {
                labelsNumeracao.get(i).setText("Página " + (i + 1) + "/" + totalPaginas);
            }
            for (VBox p : paginas) { job.printPage(pageLayout, p); }
            job.endJob();
        }
    }

    private VBox criarCabecalhoImpressao() {
        CompanyDataLoader cdl = new CompanyDataLoader();
        VBox cabecalho = PrintLayoutHelper.criarHeaderEmpresaA4(
                cdl.getNomeEmpresa(), cdl.getCnpj(), cdl.getEndereco(), cdl.getCaminhoLogo());

        Label lblData = new Label("EMITIDO EM: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblData.setStyle("-fx-font-size: 9px;");
        cabecalho.getChildren().add(lblData);
        return cabecalho;
    }

    private void configurarGridImpressao(GridPane grid, double largura) {
        grid.setPrefWidth(largura);
        grid.setMaxWidth(largura);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(12); // Data
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(15); // Solicitante
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(15); // Autorizador
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(20); // Motivo
        ColumnConstraints col5 = new ColumnConstraints(); col5.setPercentWidth(38); // Detalhes
        grid.getColumnConstraints().addAll(col1, col2, col3, col4, col5);
    }

    private void adicionarCabecalhoColunasImpressao(GridPane grid) {
        adicionarCelulaCabecalho(grid, "DATA", 0);
        adicionarCelulaCabecalho(grid, "SOLICITANTE", 1);
        adicionarCelulaCabecalho(grid, "AUTORIZADOR", 2);
        adicionarCelulaCabecalho(grid, "MOTIVO", 3);
        adicionarCelulaCabecalho(grid, "ITEM EXCLUÍDO", 4);
    }

    private void adicionarCelulaCabecalho(GridPane grid, String texto, int col) {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: #059669; -fx-padding: 6;"); 
        Label label = new Label(texto);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 9px;");
        pane.getChildren().add(label);
        grid.add(pane, col, 0);
    }

    private void adicionarLinhaGridImpressao(GridPane grid, RegistroAuditoria reg, int row, String corFundo) {
        adicionarCelulaDado(grid, reg.getDataHora(), 0, row, corFundo);
        adicionarCelulaDado(grid, reg.getSolicitante(), 1, row, corFundo);
        adicionarCelulaDado(grid, reg.getAutorizador(), 2, row, corFundo);
        adicionarCelulaDado(grid, reg.getMotivo(), 3, row, corFundo);
        adicionarCelulaDado(grid, reg.getDetalhe(), 4, row, corFundo);
    }

    private void adicionarCelulaDado(GridPane grid, String texto, int col, int row, String corFundo) {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: " + corFundo + "; -fx-padding: 5;");
        Label label = new Label(texto);
        label.setStyle("-fx-text-fill: black; -fx-font-size: 8px;");
        label.setWrapText(true);
        pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        pane.getChildren().add(label);
        grid.add(pane, col, row);
    }

    public static class RegistroAuditoria {
        private int id;
        private String dataHora, solicitante, autorizador, motivo, detalhe;

        public RegistroAuditoria(int id, String dh, String sol, String aut, String mot, String det) {
            this.id = id; this.dataHora = dh; this.solicitante = sol; this.autorizador = aut; this.motivo = mot; this.detalhe = det;
        }
        
        public int getId() { return id; }
        public String getDataHora() { return dataHora; }
        public String getSolicitante() { return solicitante; }
        public String getAutorizador() { return autorizador; }
        public String getMotivo() { return motivo; }
        public String getDetalhe() { return detalhe; }
    }
    
}