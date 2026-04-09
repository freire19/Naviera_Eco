package gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;

import dao.ConexaoBD;
import dao.FreteDAO;
import dao.ViagemDAO;
import model.Frete;
import model.Viagem;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ListaFretesController {

    @FXML private TableView<FreteView> tabelaFretes;
    @FXML private TableColumn<FreteView, String> colNumFrete, colRemetente, colDestinatario, colViagem, colDataViagem, colEmissao, colNominal, colDevedor, colBaixado, colConferente;
    @FXML private Label lblTotalLancado, lblTotalRecebido, lblTotalAReceber, lblLancamentos, lblTotalVolumes;
    @FXML private RadioButton rbQuitados, rbAReceber, rbCancelado, rbTodos;
    @FXML private TextField txtNumFreteFiltro;
    @FXML private ComboBox<String> cbRemetenteFiltro, cbClienteFiltro;
    @FXML private Button btnAtualizarLista;
    @FXML private Button btnFechar; 
    @FXML private ComboBox<ViagemFiltroWrapper> cbViagemFiltro;
    @FXML private TextField txtItemFiltro;
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    
    private final FreteDAO freteDAO = new FreteDAO();
    private final ViagemDAO viagemDAO = new ViagemDAO();

    private final ObservableList<FreteView> listaCompletaFretes = FXCollections.observableArrayList();
    private final ObservableList<FreteView> listaFretesVisivel = FXCollections.observableArrayList();

    private final DecimalFormat df = new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt","BR")));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarColunasTabela();
        configurarEstiloLinhas();
        tabelaFretes.setItems(listaFretesVisivel);
        configurarListeners();
        aplicarEstiloCabecalho();

        // DR010+DR103: carrega dados em background, atualiza UI via Platform.runLater
        Thread bg = new Thread(() -> {
            try {
                // 1. Buscar viagens em bg thread (sem tocar UI)
                List<Viagem> todasAsViagens = viagemDAO.listarTodasViagensResumido();
                Viagem viagemAtiva = null;
                List<ViagemFiltroWrapper> wrappers = new java.util.ArrayList<>();
                wrappers.add(new ViagemFiltroWrapper(0L, "Todas as Viagens"));
                for (Viagem v : todasAsViagens) {
                    wrappers.add(new ViagemFiltroWrapper(v.getId(), formatarTextoViagem(v)));
                    if (v.getIsAtual()) viagemAtiva = v;
                }
                final Viagem finalViagem = viagemAtiva;

                // 2. Atualizar ComboBox na FX thread, depois carregar dados
                javafx.application.Platform.runLater(() -> {
                    cbViagemFiltro.getItems().setAll(wrappers);
                    if (finalViagem != null) {
                        for (ViagemFiltroWrapper w : cbViagemFiltro.getItems()) {
                            if (w.getId().equals(finalViagem.getId())) { cbViagemFiltro.getSelectionModel().select(w); break; }
                        }
                    } else { cbViagemFiltro.getSelectionModel().selectFirst(); }
                    recarregarDadosDoBanco();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> showAlert(AlertType.ERROR, "Erro", "Falha ao carregar dados iniciais."));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    private void configurarColunasTabela() {
        colNumFrete.setCellValueFactory(new PropertyValueFactory<>("numFrete"));
        colNumFrete.setCellFactory(column -> new TableCell<FreteView, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); } 
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0078D7; -fx-alignment: CENTER;"); }
            }
        });
        colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        colDestinatario.setCellValueFactory(new PropertyValueFactory<>("destinatario"));
        colViagem.setCellValueFactory(new PropertyValueFactory<>("viagem"));
        colDataViagem.setCellValueFactory(new PropertyValueFactory<>("dataViagem"));
        colDataViagem.setStyle("-fx-alignment: CENTER;");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("emissao"));
        colNominal.setCellValueFactory(new PropertyValueFactory<>("nominal"));
        colNominal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #0078D7;");
        colDevedor.setCellValueFactory(new PropertyValueFactory<>("devedor"));
        colDevedor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        colBaixado.setCellValueFactory(new PropertyValueFactory<>("baixado"));
        colBaixado.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        colConferente.setCellValueFactory(new PropertyValueFactory<>("conferente"));
    }
    
    private void configurarEstiloLinhas() {
        tabelaFretes.setRowFactory(tv -> new TableRow<FreteView>() {
            @Override protected void updateItem(FreteView item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); } else {
                    double aReceber = parseDoubleFromMonetaryString(item.getDevedor());
                    String status = item.getStatus();
                    if ("CANCELADO".equalsIgnoreCase(status)) { setStyle("-fx-background-color: #f5f5f5; -fx-opacity: 0.5;"); } 
                    else if (aReceber <= 0.01) { setStyle("-fx-background-color: #e8f5e9;"); } 
                    else { setStyle(""); }
                }
            }
        });
    }
    
    private void aplicarEstiloCabecalho() {
        Platform.runLater(() -> {
            Set<Node> headers = tabelaFretes.lookupAll(".column-header");
            for (Node header : headers) { header.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cfd8dc; -fx-border-width: 0 1 1 0;"); }
            Node headerBackground = tabelaFretes.lookup(".column-header-background");
            if (headerBackground != null) { headerBackground.setStyle("-fx-background-color: #f0f0f0;"); }
            Set<Node> labels = tabelaFretes.lookupAll(".column-header .label");
            for (Node label : labels) { label.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 12px;"); }
        });
    }
    
    private void configurarFiltrosIniciais() {
        List<Viagem> todasAsViagens = viagemDAO.listarTodasViagensResumido();
        cbViagemFiltro.getItems().clear();
        cbViagemFiltro.getItems().add(new ViagemFiltroWrapper(0L, "Todas as Viagens"));
        Viagem viagemAtiva = null;
        for (Viagem v : todasAsViagens) {
            String textoFormatado = formatarTextoViagem(v);
            cbViagemFiltro.getItems().add(new ViagemFiltroWrapper(v.getId(), textoFormatado));
            if (v.getIsAtual()) viagemAtiva = v;
        }
        if (viagemAtiva != null) {
            for (ViagemFiltroWrapper wrapper : cbViagemFiltro.getItems()) {
                if (wrapper.getId().equals(viagemAtiva.getId())) { cbViagemFiltro.getSelectionModel().select(wrapper); break; }
            }
        } else { cbViagemFiltro.getSelectionModel().selectFirst(); }
    }
    
    private String formatarTextoViagem(Viagem v) {
        if (v == null) return "";
        String inicio = (v.getDataViagem() != null) ? v.getDataViagem().format(dateFormatter) : "??";
        String fim = (v.getDataChegada() != null) ? v.getDataChegada().format(dateFormatter) : "??";
        return inicio + " até " + fim;
    }
    
    private void configurarListeners() {
        tabelaFretes.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tabelaFretes.getSelectionModel().getSelectedItem() != null) {
                abrirEdicaoFrete(tabelaFretes.getSelectionModel().getSelectedItem().getNumFrete());
            }
        });
        cbViagemFiltro.valueProperty().addListener((obs, oldV, newV) -> recarregarDadosDoBanco());
        txtItemFiltro.textProperty().addListener((obs, oldV, newV) -> recarregarDadosDoBanco());
        dpDataInicio.valueProperty().addListener((obs, oldV, newV) -> recarregarDadosDoBanco());
        dpDataFim.valueProperty().addListener((obs, oldV, newV) -> recarregarDadosDoBanco());
        btnAtualizarLista.setOnAction(e -> recarregarDadosDoBanco());
        txtNumFreteFiltro.textProperty().addListener((obs, oldV, newV) -> filtrarListaLocalmente());
        cbRemetenteFiltro.valueProperty().addListener((obs, oldV, newV) -> filtrarListaLocalmente());
        cbClienteFiltro.valueProperty().addListener((obs, oldV, newV) -> filtrarListaLocalmente());
        rbQuitados.setOnAction(e -> filtrarListaLocalmente());
        rbAReceber.setOnAction(e -> filtrarListaLocalmente());
        rbCancelado.setOnAction(e -> filtrarListaLocalmente());
        rbTodos.setOnAction(e -> filtrarListaLocalmente());
    }
    
    @FXML void handleFechar(ActionEvent event) { TelaPrincipalController.fecharTelaAtual(btnFechar); }

    private void abrirEdicaoFrete(String numeroFrete) {
        try {
            // CORREÇÃO: Usamos o método estático para passar o ID ANTES de carregar a tela.
            // Isso evita o erro de tentar acessar um método privado diretamente.
            CadastroFreteController.setNumeroFreteParaAbrir(numeroFrete);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CadastroFrete.fxml"));
            Parent root = loader.load();
            
            // O initialize() do CadastroFreteController vai ler a variável estática automaticamente.

            Stage stage = new Stage();
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setTitle("Editar Frete " + numeroFrete);
            stage.setMaximized(true);
            stage.showAndWait(); 
            
            recarregarDadosDoBanco();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro", "Erro ao abrir tela de frete: " + e.getMessage());
        }
    }

    // =========================================================================================
    // IMPRESSÃO DE LISTA DE FRETES (A4 PAISAGEM + ZEBRADO + LOGO)
    // =========================================================================================
    @FXML
    public void handleImprimirLista(ActionEvent event) {
        if (listaFretesVisivel.isEmpty()) {
            showAlert(AlertType.WARNING, "Aviso", "A lista está vazia. Nada para imprimir.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tabelaFretes.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
            
            double larguraUtil = pageLayout.getPrintableWidth() - 20;
            double alturaUtil = pageLayout.getPrintableHeight();

            List<VBox> paginas = new ArrayList<>();
            ViagemFiltroWrapper vSelect = cbViagemFiltro.getValue();
            String textoViagem = (vSelect != null) ? vSelect.toString() : "Todas as Viagens";

            VBox paginaAtual = criarPaginaTemplate(larguraUtil, alturaUtil, 1, textoViagem);
            double alturaAtual = 160; 

            HBox headerTabela = criarHeaderTabelaImpressao(larguraUtil);
            paginaAtual.getChildren().add(headerTabela);
            alturaAtual += 30;

            int index = 0;
            int numPagina = 1;

            for (FreteView f : listaFretesVisivel) {
                HBox linha = criarLinhaTabelaImpressao(f, larguraUtil, index % 2 == 0);
                new Scene(linha); linha.applyCss(); linha.layout();
                double alturaLinha = linha.getLayoutBounds().getHeight();
                if (alturaLinha < 20) alturaLinha = 25; 

                if (alturaAtual + alturaLinha > alturaUtil - 50) { 
                    adicionarRodapePagina(paginaAtual, larguraUtil, numPagina);
                    paginas.add(paginaAtual);
                    
                    numPagina++;
                    paginaAtual = criarPaginaTemplate(larguraUtil, alturaUtil, numPagina, textoViagem);
                    paginaAtual.getChildren().add(headerTabela); 
                    alturaAtual = 160 + 30;
                }

                paginaAtual.getChildren().add(linha);
                alturaAtual += alturaLinha;
                index++;
            }
            
            VBox totais = criarBlocoTotaisImpressao(larguraUtil);
            new Scene(totais); totais.applyCss(); totais.layout();
            if (alturaAtual + totais.getLayoutBounds().getHeight() > alturaUtil - 50) {
                 adicionarRodapePagina(paginaAtual, larguraUtil, numPagina);
                 paginas.add(paginaAtual);
                 numPagina++;
                 paginaAtual = criarPaginaTemplate(larguraUtil, alturaUtil, numPagina, textoViagem);
            }
            paginaAtual.getChildren().add(totais);
            adicionarRodapePagina(paginaAtual, larguraUtil, numPagina);
            paginas.add(paginaAtual);

            for (VBox p : paginas) {
                boolean sucesso = job.printPage(pageLayout, p);
                if (!sucesso) {
                    showAlert(AlertType.ERROR, "Erro", "Falha ao enviar página para impressora.");
                    break;
                }
            }
            job.endJob();
        }
    }

    private VBox criarPaginaTemplate(double w, double h, int pag, String filtroViagem) {
        VBox p = new VBox(5);
        p.setPadding(new Insets(20));
        p.setPrefSize(w, h);
        p.setMaxSize(w, h);
        p.setStyle("-fx-background-color: white;");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefWidth(w);
        header.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");

        try (Connection conn = ConexaoBD.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement("SELECT path_logo, nome_embarcacao, cnpj, telefone FROM configuracao_empresa LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String caminhoFoto = rs.getString("path_logo");
                if (caminhoFoto != null && !caminhoFoto.isEmpty()) {
                    ImageView logo = new ImageView(new Image("file:" + caminhoFoto));
                    logo.setFitHeight(60); logo.setPreserveRatio(true);
                    header.getChildren().add(logo);
                }
                
                VBox infoEmpresa = new VBox(2);
                Label lNome = new Label(rs.getString("nome_embarcacao")); lNome.setFont(Font.font("Arial", FontWeight.BOLD, 18));
                Label lDados = new Label("CNPJ: " + rs.getString("cnpj") + "  |  Tel: " + rs.getString("telefone")); 
                lDados.setFont(Font.font("Arial", 10));
                Label lTitulo = new Label("RELATÓRIO DE FRETES");
                lTitulo.setFont(Font.font("Arial", FontWeight.BLACK, 14));
                lTitulo.setTextFill(Color.web("#0d47a1"));
                Label lFiltro = new Label("Filtro: " + filtroViagem + " | Emissão: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                lFiltro.setFont(Font.font("Arial", 10));

                infoEmpresa.getChildren().addAll(lNome, lDados, lTitulo, lFiltro);
                header.getChildren().add(infoEmpresa);
            }
        } catch (Exception e) { e.printStackTrace(); }

        p.getChildren().add(header);
        return p;
    }

    private HBox criarHeaderTabelaImpressao(double w) {
        HBox box = new HBox(5);
        box.setPrefWidth(w);
        box.setPadding(new Insets(5));
        box.setStyle("-fx-background-color: #0d47a1;");

        box.getChildren().addAll(
            criarCelHeader("Nº", w * 0.08),
            criarCelHeader("REMETENTE", w * 0.20),
            criarCelHeader("DESTINATÁRIO", w * 0.20),
            criarCelHeader("ROTA", w * 0.15),
            criarCelHeader("VALOR", w * 0.10),
            criarCelHeader("PAGO", w * 0.10),
            criarCelHeader("A RECEBER", w * 0.10),
            criarCelHeader("STATUS", w * 0.07)
        );
        return box;
    }

    private HBox criarLinhaTabelaImpressao(FreteView f, double w, boolean par) {
        HBox box = new HBox(5);
        box.setPrefWidth(w);
        box.setPadding(new Insets(3, 5, 3, 5));
        box.setStyle(par ? "-fx-background-color: white;" : "-fx-background-color: #e3f2fd;"); 

        box.getChildren().addAll(
            criarCelNormal(f.getNumFrete(), w * 0.08, Pos.CENTER),
            criarCelNormal(f.getRemetente(), w * 0.20, Pos.CENTER_LEFT),
            criarCelNormal(f.getDestinatario(), w * 0.20, Pos.CENTER_LEFT),
            criarCelNormal(f.getViagem(), w * 0.15, Pos.CENTER_LEFT),
            criarCelNormal(f.getNominal(), w * 0.10, Pos.CENTER_RIGHT),
            criarCelNormal(f.getBaixado(), w * 0.10, Pos.CENTER_RIGHT),
            criarCelNormal(f.getDevedor(), w * 0.10, Pos.CENTER_RIGHT),
            criarCelNormal(f.getStatus(), w * 0.07, Pos.CENTER)
        );
        return box;
    }

    private Label criarCelHeader(String txt, double w) {
        Label l = new Label(txt);
        l.setPrefWidth(w); l.setMaxWidth(w);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private Label criarCelNormal(String txt, double w, Pos align) {
        Label l = new Label(txt);
        l.setPrefWidth(w); l.setMaxWidth(w);
        l.setTextFill(Color.BLACK);
        l.setFont(Font.font("Arial", 9));
        l.setAlignment(align);
        l.setWrapText(true);
        return l;
    }

    private VBox criarBlocoTotaisImpressao(double w) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10, 0, 0, 0));
        box.setAlignment(Pos.CENTER_RIGHT);
        
        box.getChildren().add(new Label("RESUMO GERAL"));
        box.getChildren().add(criarLinhaTotalImp("Total Lançado:", lblTotalLancado.getText(), Color.BLUE));
        box.getChildren().add(criarLinhaTotalImp("Total Recebido:", lblTotalRecebido.getText(), Color.GREEN));
        box.getChildren().add(criarLinhaTotalImp("Total A Receber:", lblTotalAReceber.getText(), Color.RED));
        
        return box;
    }
    
    private HBox criarLinhaTotalImp(String tit, String val, Color c) {
        HBox h = new HBox(10); h.setAlignment(Pos.CENTER_RIGHT);
        Label lT = new Label(tit); lT.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        Label lV = new Label(val); lV.setFont(Font.font("Arial", FontWeight.BOLD, 11)); lV.setTextFill(c);
        h.getChildren().addAll(lT, lV);
        return h;
    }

    private void adicionarRodapePagina(VBox p, double w, int num) {
        Region r = new Region(); VBox.setVgrow(r, Priority.ALWAYS);
        p.getChildren().add(r);
        Label l = new Label("Página " + num);
        l.setPrefWidth(w); l.setAlignment(Pos.CENTER_RIGHT);
        l.setFont(Font.font("Arial", 8));
        p.getChildren().add(l);
    }

    private void recarregarDadosDoBanco() {
        ViagemFiltroWrapper viagemSelecionada = cbViagemFiltro.getValue();
        Long idViagemFiltro = null;
        if (viagemSelecionada != null && viagemSelecionada.getId() > 0) {
            idViagemFiltro = viagemSelecionada.getId();
        }
        String termoBuscaItem = txtItemFiltro.getText();
        LocalDate dataInicio = dpDataInicio.getValue();
        LocalDate dataFim = dpDataFim.getValue();
        List<Frete> fretesDoBanco = freteDAO.buscarFretes(idViagemFiltro, termoBuscaItem, dataInicio, dataFim);
        listaCompletaFretes.clear();
        for (Frete f : fretesDoBanco) {
            String dataEmissaoStr = (f.getDataEmissao() != null) ? f.getDataEmissao().format(dateFormatter) : "";
            String dataViagemStr = "";
            if (f.getDataViagem() != null) {
                dataViagemStr = f.getDataViagem().format(dateFormatter);
            }
            listaCompletaFretes.add(new FreteView(
                f.getNumeroFrete() != null ? f.getNumeroFrete() : String.valueOf(f.getIdFrete()),
                f.getNomeRemetente(), f.getNomeDestinatario(), f.getNomeRota(), 
                dataViagemStr, dataEmissaoStr,
                df.format(f.getValorNominal()), df.format(f.getValorDevedor()), df.format(f.getValorPago()),
                f.getNomeConferente(), f.getStatus(), f.getTotalVolumes()
            ));
        }
        filtrarListaLocalmente();
    }

    private void filtrarListaLocalmente() {
        final String numFiltro = txtNumFreteFiltro.getText() != null ? txtNumFreteFiltro.getText().trim().toLowerCase() : "";
        final String remFiltro = cbRemetenteFiltro.getValue() != null ? cbRemetenteFiltro.getValue().trim().toLowerCase() : "";
        final String cliFiltro = cbClienteFiltro.getValue() != null ? cbClienteFiltro.getValue().trim().toLowerCase() : "";
        
        ObservableList<FreteView> filtrada = listaCompletaFretes.stream()
            .filter(f -> {
                boolean match = true;
                if (!numFiltro.isEmpty() && !f.getNumFrete().toLowerCase().contains(numFiltro)) match = false;
                if (match && !remFiltro.isEmpty() && (f.getRemetente() == null || !f.getRemetente().toLowerCase().contains(remFiltro))) match = false;
                if (match && !cliFiltro.isEmpty() && (f.getDestinatario() == null || !f.getDestinatario().toLowerCase().contains(cliFiltro))) match = false;
                if (match && !rbTodos.isSelected()) {
                    double devedor = parseDoubleFromMonetaryString(f.getDevedor());
                    String status = (f.getStatus() != null) ? f.getStatus().trim().toUpperCase() : "PENDENTE";
                    if (rbQuitados.isSelected() && (devedor > 0.009 || "CANCELADO".equals(status))) match = false;
                    if (rbAReceber.isSelected() && (devedor <= 0.009 || "CANCELADO".equals(status))) match = false;
                    if (rbCancelado.isSelected() && !"CANCELADO".equals(status)) match = false;
                }
                return match;
            })
            .collect(Collectors.toCollection(FXCollections::observableArrayList));

        listaFretesVisivel.setAll(filtrada);
        recalcularRodape();
    }
    
    private void recalcularRodape() {
        double totalLancado = 0, totalRecebido = 0, totalAReceber = 0;
        int lancamentos = 0, volumes = 0;
        for (FreteView f : listaFretesVisivel) {
            if (!"CANCELADO".equalsIgnoreCase(f.getStatus())) {
                lancamentos++;
                volumes += f.getTotalVolumes();
                totalLancado += parseDoubleFromMonetaryString(f.getNominal());
                totalRecebido += parseDoubleFromMonetaryString(f.getBaixado());
                totalAReceber += parseDoubleFromMonetaryString(f.getDevedor());
            }
        }
        lblTotalLancado.setText(df.format(totalLancado));
        lblTotalRecebido.setText(df.format(totalRecebido));
        lblTotalAReceber.setText(df.format(totalAReceber));
        lblLancamentos.setText(String.valueOf(lancamentos));
        lblTotalVolumes.setText(String.valueOf(volumes));
    }

    private double parseDoubleFromMonetaryString(String monetaryString) {
        if (monetaryString == null || monetaryString.trim().isEmpty()) return 0.0;
        try {
            String cleaned = monetaryString.replace("R$", "").trim().replace(".", "").replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) { return 0.0; }
    }

    private void showAlert(Alert.AlertType aT, String t, String m) {
        Alert a = new Alert(aT); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private static class ViagemFiltroWrapper {
        private final Long id;
        private final String displayText;
        public ViagemFiltroWrapper(Long id, String displayText) { this.id = id; this.displayText = displayText; }
        public Long getId() { return id; }
        @Override public String toString() { return displayText; }
    }

    public static class FreteView {
        private final SimpleStringProperty numFrete, remetente, destinatario, viagem, dataViagem, emissao, nominal, devedor, baixado, conferente, status;
        private final SimpleIntegerProperty totalVolumes;
        
        public FreteView(String nf, String rem, String des, String viaj, String dtViagem, String emi, String nom, String dev, String bai, String conf, String stat, int volumes) {
            this.numFrete = new SimpleStringProperty(nf);
            this.remetente = new SimpleStringProperty(rem);
            this.destinatario = new SimpleStringProperty(des);
            this.viagem = new SimpleStringProperty(viaj);
            this.dataViagem = new SimpleStringProperty(dtViagem); 
            this.emissao = new SimpleStringProperty(emi);
            this.nominal = new SimpleStringProperty(nom);
            this.devedor = new SimpleStringProperty(dev);
            this.baixado = new SimpleStringProperty(bai);
            this.conferente = new SimpleStringProperty(conf);
            this.status = new SimpleStringProperty(stat == null ? "PENDENTE" : stat);
            this.totalVolumes = new SimpleIntegerProperty(volumes);
        }
        public String getNumFrete() { return numFrete.get(); }
        public String getRemetente() { return remetente.get(); }
        public String getDestinatario() { return destinatario.get(); }
        public String getViagem() { return viagem.get(); }
        public String getDataViagem() { return dataViagem.get(); } 
        public String getEmissao() { return emissao.get(); }
        public String getNominal() { return nominal.get(); }
        public String getDevedor() { return devedor.get(); }
        public String getBaixado() { return baixado.get(); }
        public String getConferente() { return conferente.get(); }
        public String getStatus() { return status.get(); }
        public int    getTotalVolumes() { return totalVolumes.get(); }
    }
}