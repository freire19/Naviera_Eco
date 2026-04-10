package gui;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import dao.ConexaoBD;
import dao.EmpresaDAO;
import dao.ReciboAvulsoDAO;
import dao.ViagemDAO;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.application.Platform;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import model.Empresa;
import model.ReciboAvulso;
import model.Viagem;

public class GerarReciboAvulsoController implements Initializable {

    @FXML private TextField txtNome;
    @FXML private TextField txtValor;
    @FXML private DatePicker dtData;
    @FXML private TextArea txtReferente;
    
    // FILTROS
    @FXML private ComboBox<String> cmbFiltroViagem; 
    @FXML private Button btnLimparFiltro;
    
    @FXML private TableView<ReciboAvulso> tabelaRecibos;
    @FXML private TableColumn<ReciboAvulso, LocalDate> colData;
    @FXML private TableColumn<ReciboAvulso, String> colNome;
    @FXML private TableColumn<ReciboAvulso, String> colReferente;
    @FXML private TableColumn<ReciboAvulso, BigDecimal> colValor;
    @FXML private TableColumn<ReciboAvulso, ReciboAvulso> colAcoes;

    private ReciboAvulsoDAO dao = new ReciboAvulsoDAO();
    private ViagemDAO viagemDao = new ViagemDAO();
    private Viagem viagemAtiva;
    
    // Dados Empresa
    private String empresaNome = "EMPRESA MODELO";
    private String empresaCnpj = "";
    private String empresaEndereco = "";
    private String empresaTelefone = "";
    private String empresaLogoPath = ""; 

    // CORES
    private final String COR_PRIMARIA_BRAND = "#059669";
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Gerar Recibo Avulso"); return; }
        // 2. Data padrão = Hoje (UI pura, fica na FX thread)
        dtData.setValue(LocalDate.now());
        // 3. Configurações Iniciais (UI pura)
        configurarTabela();
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                carregarDadosEmpresa();
                // 1. Identifica Viagem Ativa
                Viagem va = viagemDao.buscarViagemAtiva();
                // Carrega combo de viagens (DB)
                ObservableList<String> itensViagens = carregarListaViagensParaFiltroAsync();
                Platform.runLater(() -> {
                    this.viagemAtiva = va;
                    // Preenche combo
                    if (cmbFiltroViagem != null) {
                        cmbFiltroViagem.setItems(itensViagens);
                        cmbFiltroViagem.setOnAction(e -> {
                            String selecionada = cmbFiltroViagem.getValue();
                            if (selecionada != null && !selecionada.isEmpty()) {
                                buscarViagemEAtualizarTabela(selecionada);
                            }
                        });
                    }
                    // 4. Carrega dados iniciais (Da viagem ativa)
                    if (this.viagemAtiva != null) {
                        carregarHistorico(this.viagemAtiva.getId().intValue() /* #010: IDs < Integer.MAX_VALUE */);
                        if (cmbFiltroViagem != null) {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            String saida = "";
                            if (this.viagemAtiva.getDataViagem() != null) {
                                saida = this.viagemAtiva.getDataViagem().format(dtf);
                            } else {
                                saida = "--";
                            }
                            String chegada = "--";
                            // Alterado de getPrevisaoChegada() para getDataChegada() conforme banco
                            try {
                                if (this.viagemAtiva.getDataChegada() != null) {
                                    chegada = this.viagemAtiva.getDataChegada().format(dtf);
                                }
                            } catch (Exception e) {
                                // Se ainda der erro, deixa --
                            }
                            // Formato Exigido: ID - Saida até Chegada
                            cmbFiltroViagem.setValue(this.viagemAtiva.getId() + " - " + saida + " até " + chegada);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Erro ao carregar dados iniciais GerarReciboAvulso: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // =========================================================================
    //  MÉTODOS DE CONTROLE (FILTRO E SAIR)
    // =========================================================================

    @FXML
    private void handleSair(ActionEvent event) {
        if (txtNome.getScene() != null && txtNome.getScene().getWindow() != null) {
            ((Stage) txtNome.getScene().getWindow()).close();
        }
    }

    // DR117: versao que retorna dados para ser chamada do background thread
    private ObservableList<String> carregarListaViagensParaFiltroAsync() {
        ObservableList<String> itens = FXCollections.observableArrayList();
        // Alterado nome da coluna no SQL para 'data_chegada'
        String sql = "SELECT id_viagem, data_viagem, data_chegada FROM viagens WHERE empresa_id = ? ORDER BY id_viagem DESC";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, dao.DAOUtils.empresaId());
            ResultSet rs = stmt.executeQuery();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            while (rs.next()) {
                long id = rs.getLong("id_viagem");
                java.sql.Date dtS = rs.getDate("data_viagem");
                java.sql.Date dtC = rs.getDate("data_chegada");
                String saida = (dtS != null) ? dtS.toLocalDate().format(dtf) : "--";
                String chegada = (dtC != null) ? dtC.toLocalDate().format(dtf) : "--";
                itens.add(id + " - " + saida + " até " + chegada);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return itens;
    }

    private void buscarViagemEAtualizarTabela(String viagemString) {
        try {
            // Pega o ID antes do primeiro traço
            if (viagemString.contains(" - ")) {
                String idStr = viagemString.split(" - ")[0].trim();
                int id = Integer.parseInt(idStr);
                carregarHistorico(id);
                if (btnLimparFiltro != null) btnLimparFiltro.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLimparFiltro(ActionEvent event) {
        if (viagemAtiva != null) {
            carregarHistorico(viagemAtiva.getId().intValue() /* #010: IDs < Integer.MAX_VALUE */);
            if(cmbFiltroViagem != null) cmbFiltroViagem.getSelectionModel().clearSelection();
        }
        if (btnLimparFiltro != null) btnLimparFiltro.setVisible(false);
    }

    private void carregarHistorico(int idViagem) {
        List<ReciboAvulso> lista = dao.listarPorViagem(idViagem);
        tabelaRecibos.setItems(FXCollections.observableArrayList(lista));
    }

    // =========================================================================
    //  CONFIGURAÇÃO DA TABELA (BOTÃO IMPRIMIR FUNCIONAL)
    // =========================================================================
    private void configurarTabela() {
        colData.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getDataEmissao()));
        colData.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });
        
        colNome.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNomePagador()));
        colReferente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReferenteA()));
        colValor.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getValor()));
        
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item));
            }
        });
        
        colAcoes.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnPrint = new Button("🖨");
            {
                btnPrint.setStyle("-fx-background-color: " + COR_PRIMARIA_BRAND + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                btnPrint.setOnAction(e -> {
                    ReciboAvulso r = getTableView().getItems().get(getIndex());
                    // Chama o método da classe principal corretamente
                    GerarReciboAvulsoController.this.abrirDetalhesRecibo(r);
                });
            }
            @Override protected void updateItem(ReciboAvulso item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPrint);
            }
        });

        tabelaRecibos.setRowFactory(tv -> {
            TableRow<ReciboAvulso> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    abrirDetalhesRecibo(row.getItem());
                }
            });
            return row ;
        });
    }

    private void abrirDetalhesRecibo(ReciboAvulso r) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reimpressão");
        dialog.setHeaderText("Opções de Impressão para Recibo Nº " + r.getId());

        ButtonType btnFechar = new ButtonType("Fechar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnFechar);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        
        Button btnA4 = new Button("🖨 A4 Econômico (Padrão)");
        btnA4.setStyle("-fx-background-color: " + COR_PRIMARIA_BRAND + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 250;");
        btnA4.setOnAction(e -> imprimirA4Preenchido(r));

        Button btnBranco = new Button("📄 A4 em Branco (2 vias)");
        btnBranco.setStyle("-fx-background-color: #7BA393; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 250;");
        btnBranco.setOnAction(e -> imprimirA4DuploEconomico(r, false));

        Button btnTermica = new Button("🧾 Cupom Térmico");
        btnTermica.setStyle("-fx-background-color: #3D6B56; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 250;");
        btnTermica.setOnAction(e -> imprimirTermica(r));
        
        layout.getChildren().addAll(new Label("Selecione o modelo:"), btnA4, btnBranco, btnTermica);
        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    // =========================================================================
    //  SALVAMENTO (VINCULADO À VIAGEM ATIVA)
    // =========================================================================

    private ReciboAvulso criarObjetoRecibo() {
        double val = 0;
        try { val = Double.parseDouble(txtValor.getText().replace("R$", "").replace(".", "").replace(",", ".").trim()); } catch (Exception e) { System.err.println("Valor invalido no recibo: " + txtValor.getText()); }
        
        // ID VIAGEM ATIVA
        int idViagemParaSalvar = (viagemAtiva != null) ? viagemAtiva.getId().intValue() /* #010: IDs < Integer.MAX_VALUE */ : 0;

        return new ReciboAvulso(
            idViagemParaSalvar,
            txtNome.getText(),
            txtReferente.getText(),
            BigDecimal.valueOf(val),
            dtData.getValue()
        );
    }

    private void salvarEImprimir(ReciboAvulso r, Runnable acaoImprimir) {
        boolean salvo = dao.salvar(r);
        if (!salvo) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText("Não foi possível salvar o recibo. Verifique o log para detalhes.");
            alert.showAndWait();
            return;
        }

        // ATUALIZA A TABELA COM O FILTRO ATUAL (OU VIAGEM ATIVA)
        String filtroAtual = (cmbFiltroViagem != null) ? cmbFiltroViagem.getValue() : null;
        if (filtroAtual != null && !filtroAtual.isEmpty()) {
            buscarViagemEAtualizarTabela(filtroAtual);
        } else if (viagemAtiva != null) {
            carregarHistorico(viagemAtiva.getId().intValue() /* #010: IDs < Integer.MAX_VALUE */);
        }

        acaoImprimir.run();
        txtNome.clear(); txtValor.clear(); txtReferente.clear(); txtValor.setText("");
    }

    @FXML private void handleImprimirBranco(ActionEvent event) {
        ReciboAvulso rVazio = new ReciboAvulso(0, "", "", BigDecimal.ZERO, LocalDate.now());
        imprimirA4DuploEconomico(rVazio, false); 
    }

    @FXML private void handleImprimirPreenchido(ActionEvent event) {
        if (!validarCampos()) return;
        ReciboAvulso r = criarObjetoRecibo();
        r.setTipoRecibo("A4_PREENCHIDO");
        salvarEImprimir(r, () -> imprimirA4Preenchido(r));
    }

    @FXML private void handleImprimirTermica(ActionEvent event) {
        if (!validarCampos()) return;
        ReciboAvulso r = criarObjetoRecibo();
        r.setTipoRecibo("TERMICA");
        salvarEImprimir(r, () -> imprimirTermica(r));
    }

    private boolean validarCampos() {
        if (txtNome.getText().isEmpty() || txtValor.getText().isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Atenção");
            alert.setHeaderText(null);
            alert.setContentText("Preencha Nome e Valor.");
            alert.showAndWait();
            return false;
        }
        if (viagemAtiva == null) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Atenção");
            alert.setHeaderText(null);
            alert.setContentText("Nenhuma viagem ativa encontrada.\nAtive uma viagem na tela principal.");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    // =========================================================================
    //  IMPRESSÃO TÉRMICA (COM MARGEM INFERIOR SEGURA)
    // =========================================================================
    private void imprimirTermica(ReciboAvulso r) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(txtNome.getScene().getWindow())) {
            
            Printer printer = job.getPrinter();
            PageLayout pageLayout = null;
            
            for (Paper paper : printer.getPrinterAttributes().getSupportedPapers()) {
                if (paper.getWidth() <= 300) { 
                     pageLayout = printer.createPageLayout(paper, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                     break;
                }
            }
            if (pageLayout == null) {
                 pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
            }

            double larguraConteudo = 200; 
            
            VBox cupom = new VBox(5);
            // MARGEM INFERIOR DE 80px (SEGURANÇA CONTRA CORTE)
            cupom.setPadding(new Insets(0, 0, 80, 0)); 
            cupom.setPrefWidth(larguraConteudo); 
            cupom.setMaxWidth(larguraConteudo);
            cupom.setAlignment(Pos.TOP_CENTER);

            ImageView logo = carregarLogo(45);
            if(logo != null) cupom.getChildren().add(logo);
            
            Text tEmp = new Text(empresaNome); tEmp.setFont(Font.font("System", FontWeight.BOLD, 9)); tEmp.setTextAlignment(TextAlignment.CENTER); tEmp.setWrappingWidth(larguraConteudo);
            Text tCnpj = new Text("CNPJ: " + empresaCnpj); tCnpj.setFont(Font.font("System", 8)); tCnpj.setTextAlignment(TextAlignment.CENTER);
            Text tEnd = new Text(empresaEndereco); tEnd.setFont(Font.font("System", 8)); tEnd.setTextAlignment(TextAlignment.CENTER); tEnd.setWrappingWidth(larguraConteudo);
            Text tTel = new Text("Tel: " + empresaTelefone); tTel.setFont(Font.font("System", 8)); tTel.setTextAlignment(TextAlignment.CENTER);

            cupom.getChildren().addAll(tEmp, tCnpj, tEnd, tTel, new Text("--------------------------------"));
            
            Text tTitulo = new Text("RECIBO Nº " + r.getId()); tTitulo.setFont(Font.font("System", FontWeight.BOLD, 11)); cupom.getChildren().add(tTitulo);

            VBox body = new VBox(4); body.setAlignment(Pos.TOP_LEFT); body.setPadding(new Insets(0, 5, 0, 5));
            body.getChildren().add(criarLinhaTermica("PAGADOR:", r.getNomePagador(), larguraConteudo - 10));
            body.getChildren().add(new Text("- - - - - - - - - - - - - - - -"));
            Text tValor = new Text("VALOR: " + NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(r.getValor())); tValor.setFont(Font.font("System", FontWeight.BOLD, 12)); body.getChildren().add(tValor);
            Text tExtenso = new Text("(" + ValorExtensoUtil.valorPorExtenso(r.getValor().doubleValue()) + ")"); tExtenso.setFont(Font.font("System", 8)); tExtenso.setWrappingWidth(larguraConteudo - 10); body.getChildren().add(tExtenso);
            body.getChildren().add(new Text("- - - - - - - - - - - - - - - -"));
            Text tRefLabel = new Text("REFERENTE:"); tRefLabel.setFont(Font.font("System", FontWeight.BOLD, 9));
            Text tRef = new Text(r.getReferenteA()); tRef.setWrappingWidth(larguraConteudo - 10); tRef.setFont(Font.font("System", 9)); body.getChildren().addAll(tRefLabel, tRef);
            
            cupom.getChildren().add(body);

            VBox footer = new VBox(5); footer.setAlignment(Pos.CENTER); footer.setPadding(new Insets(15, 0, 0, 0));
            DateTimeFormatter dtfData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Text tDataRecibo = new Text(empresaEndereco + ", " + r.getDataEmissao().format(dtfData)); tDataRecibo.setFont(Font.font("System", 8));
            Line lineAss = new Line(0, 0, 140, 0); lineAss.getStrokeDashArray().addAll(2d);
            Text tAss = new Text("Assinatura"); tAss.setFont(Font.font("System", 8));
            footer.getChildren().addAll(new Text("\n"), tDataRecibo, new Text("\n"), lineAss, tAss);
            
            DateTimeFormatter dtfImpressao = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            Text tImpresso = new Text("Impresso em: " + LocalDateTime.now().format(dtfImpressao));
            tImpresso.setFont(Font.font("System", 7));
            footer.getChildren().add(new Text("\n"));
            footer.getChildren().add(tImpresso);

            cupom.getChildren().add(footer);

            VBox pageContainer = new VBox(cupom); pageContainer.setAlignment(Pos.TOP_CENTER); pageContainer.setPadding(new Insets(0));
            boolean success = job.printPage(pageLayout, pageContainer);
            if (success) job.endJob();
        }
    }
    
    private VBox criarLinhaTermica(String label, String val, double width) {
        VBox v = new VBox(0);
        Text l = new Text(label); l.setFont(Font.font("System", FontWeight.BOLD, 8));
        Text value = new Text(val); value.setFont(Font.font("System", 9)); value.setWrappingWidth(width);
        v.getChildren().addAll(l, value);
        return v;
    }

    // =========================================================================
    //  IMPRESSÃO A4 - DESIGN ECONÔMICO
    // =========================================================================

    private void imprimirA4Preenchido(ReciboAvulso r) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(txtNome.getScene().getWindow())) {
            VBox reciboNode = criarLayoutReciboEconomico(r, true);
            VBox pageRoot = new VBox(reciboNode);
            pageRoot.setPadding(new Insets(40, 0, 0, 0)); 
            pageRoot.setAlignment(Pos.TOP_CENTER);
            PageLayout pageLayout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double scale = pageLayout.getPrintableWidth() / 620; 
            pageRoot.getTransforms().add(new Scale(scale, scale));
            if (job.printPage(pageLayout, pageRoot)) job.endJob();
        }
    }

    private void imprimirA4DuploEconomico(ReciboAvulso r, boolean preenchido) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(txtNome.getScene().getWindow())) {
            VBox recibo1 = criarLayoutReciboEconomico(r, preenchido);
            HBox linhaCorte = new HBox(10);
            linhaCorte.setAlignment(Pos.CENTER);
            linhaCorte.setPadding(new Insets(50, 0, 50, 0)); 
            Line l1 = new Line(0,0,250,0); l1.getStrokeDashArray().addAll(10d); l1.setStroke(Color.GRAY);
            Label ico = new Label("✂ Corte Aqui"); ico.setTextFill(Color.GRAY);
            Line l2 = new Line(0,0,250,0); l2.getStrokeDashArray().addAll(10d); l2.setStroke(Color.GRAY);
            linhaCorte.getChildren().addAll(l1, ico, l2);
            VBox recibo2 = criarLayoutReciboEconomico(r, preenchido);
            VBox pageRoot = new VBox();
            pageRoot.getChildren().addAll(recibo1, linhaCorte, recibo2);
            pageRoot.setPadding(new Insets(30, 0, 0, 0)); 
            pageRoot.setAlignment(Pos.TOP_CENTER);
            PageLayout pageLayout = job.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double scale = (pageLayout.getPrintableWidth() / 620) * 0.95; 
            pageRoot.getTransforms().add(new Scale(scale, scale));
            if (job.printPage(pageLayout, pageRoot)) job.endJob();
        }
    }

    private VBox criarLayoutReciboEconomico(ReciboAvulso r, boolean preenchido) {
        VBox mainContainer = new VBox(15);
        mainContainer.setPrefWidth(600); 
        mainContainer.setMaxWidth(600);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: " + COR_PRIMARIA_BRAND + "; -fx-border-width: 2;");
        mainContainer.setPadding(new Insets(20));

        HBox header = new HBox(20); header.setAlignment(Pos.TOP_CENTER);
        
        VBox leftSide = new VBox(5); leftSide.setPrefWidth(350); leftSide.setAlignment(Pos.TOP_LEFT);
        HBox logoTitle = new HBox(10); logoTitle.setAlignment(Pos.CENTER_LEFT); 
        ImageView logoView = carregarLogo(50);
        Label lblEmp = new Label(empresaNome); lblEmp.setFont(Font.font("Arial", FontWeight.BOLD, 14)); lblEmp.setTextFill(Color.web(COR_PRIMARIA_BRAND)); lblEmp.setWrapText(true); lblEmp.setMaxWidth(280);
        logoTitle.getChildren().addAll(logoView, lblEmp);
        Label lblEnd = new Label(empresaEndereco); lblEnd.setFont(Font.font("Arial", 10));
        Label lblContatos = new Label("CNPJ: " + empresaCnpj + " | Tel: " + empresaTelefone); lblContatos.setFont(Font.font("Arial", 10));
        leftSide.getChildren().addAll(logoTitle, lblEnd, lblContatos);
        
        VBox rightSide = new VBox(10); rightSide.setPrefWidth(200); rightSide.setAlignment(Pos.TOP_RIGHT);
        HBox boxNum = new HBox(5); boxNum.setAlignment(Pos.CENTER_RIGHT);
        Label lblN = new Label("RECIBO Nº"); lblN.setFont(Font.font("Arial", FontWeight.BOLD, 14)); lblN.setTextFill(Color.web(COR_PRIMARIA_BRAND));
        Label valN = new Label(preenchido ? String.valueOf(r.getId()) : "____"); valN.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        boxNum.getChildren().addAll(lblN, valN);
        
        VBox valorBox = new VBox(5); valorBox.setStyle("-fx-border-color: " + COR_PRIMARIA_BRAND + "; -fx-border-width: 1; -fx-padding: 5;"); valorBox.setAlignment(Pos.CENTER);
        Label lblV = new Label("VALOR R$"); lblV.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        String valTxt = preenchido ? NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(r.getValor()) : "";
        Label txtV = new Label(valTxt); txtV.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        valorBox.getChildren().addAll(lblV, txtV);
        rightSide.getChildren().addAll(boxNum, valorBox);
        
        header.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(header);
        
        Line div = new Line(0,0,560,0); div.setStroke(Color.web(COR_PRIMARIA_BRAND)); div.setStrokeWidth(1);
        mainContainer.getChildren().add(div);

        VBox body = new VBox(15); body.setPadding(new Insets(10, 0, 10, 0));
        body.getChildren().add(criarLinhaEconomica("Recebi(emos) de:", preenchido ? r.getNomePagador() : ""));
        String extenso = preenchido ? ValorExtensoUtil.valorPorExtenso(r.getValor().doubleValue()) : "";
        body.getChildren().add(criarLinhaEconomica("A importância de:", extenso));
        body.getChildren().add(criarLinhaEconomica("Referente a:", preenchido ? r.getReferenteA() : ""));
        body.getChildren().add(criarLinhaEconomica("", ""));
        mainContainer.getChildren().add(body);

        HBox footer = new HBox(30); footer.setAlignment(Pos.BOTTOM_CENTER); footer.setPadding(new Insets(20, 0, 0, 0));
        String dataH = preenchido ? r.getDataEmissao().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt","BR"))) : "";
        
        VBox cityBox = new VBox(2); cityBox.setAlignment(Pos.CENTER);
        Label cityVal = new Label(empresaEndereco + ", " + dataH); cityVal.setFont(Font.font("Arial", 11));
        Line l1 = new Line(0,0,220,0); l1.setStroke(Color.BLACK);
        Label cityLbl = new Label("Cidade e Data"); cityLbl.setFont(Font.font("Arial", 9));
        cityBox.getChildren().addAll(cityVal, l1, cityLbl);
        
        VBox signBox = new VBox(2); signBox.setAlignment(Pos.CENTER);
        Label signVal = new Label(" "); 
        Line l2 = new Line(0,0,220,0); l2.setStroke(Color.BLACK);
        Label signLbl = new Label("Assinatura"); signLbl.setFont(Font.font("Arial", 9));
        signBox.getChildren().addAll(signVal, l2, signLbl);
        
        footer.getChildren().addAll(cityBox, signBox);
        mainContainer.getChildren().add(footer);
        
        return mainContainer;
    }

    private VBox criarLinhaEconomica(String label, String valor) {
        VBox v = new VBox(2);
        HBox content = new HBox(5); content.setAlignment(Pos.BOTTOM_LEFT);
        Label lbl = new Label(label); lbl.setFont(Font.font("Arial", FontWeight.BOLD, 10)); lbl.setTextFill(Color.web(COR_PRIMARIA_BRAND)); lbl.setMinWidth(Region.USE_PREF_SIZE);
        Label val = new Label(valor); val.setFont(Font.font("Arial", 11)); val.setTextFill(Color.BLACK);
        content.getChildren().addAll(lbl, val);
        Line line = new Line(0, 0, 560, 0); line.setStroke(Color.web("#7BA393")); // Cinza bem claro
        v.getChildren().addAll(content, line);
        return v;
    }

    private void carregarDadosEmpresa() {
        try {
            EmpresaDAO empresaDAO = new EmpresaDAO();
            Empresa empresa = empresaDAO.buscarPorId(EmpresaDAO.ID_EMPRESA_PRINCIPAL);
            if (empresa != null) {
                if (empresa.getEmbarcacao() != null) empresaNome = empresa.getEmbarcacao().toUpperCase();
                if (empresa.getCnpj() != null) empresaCnpj = empresa.getCnpj();
                if (empresa.getCaminhoFoto() != null) empresaLogoPath = empresa.getCaminhoFoto();
                if (empresa.getEndereco() != null) empresaEndereco = empresa.getEndereco();
                if (empresa.getTelefone() != null) empresaTelefone = empresa.getTelefone();
            }
        } catch (Exception e) { System.err.println("Erro em GerarReciboAvulsoController.carregarDadosEmpresa: " + e.getMessage()); }
    }

    private ImageView carregarLogo(double h) {
        try {
            if (empresaLogoPath != null && !empresaLogoPath.isEmpty()) {
                File f = new File(empresaLogoPath);
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitHeight(h); iv.setPreserveRatio(true);
                    return iv;
                }
            }
            URL url = getClass().getResource("/gui/icons/logo_login.png");
            if (url != null) {
                ImageView iv = new ImageView(new Image(url.toExternalForm()));
                iv.setFitHeight(h); iv.setPreserveRatio(true);
                return iv;
            }
        } catch(Exception e) { System.err.println("Erro em GerarReciboAvulsoController.carregarLogo: " + e.getMessage()); }
        return new ImageView();
    }

    public static class ValorExtensoUtil {
        private static final String[] UNIDADES = {"", "Um", "Dois", "Três", "Quatro", "Cinco", "Seis", "Sete", "Oito", "Nove", "Dez", "Onze", "Doze", "Treze", "Quatorze", "Quinze", "Dezesseis", "Dezessete", "Dezoito", "Dezenove"};
        private static final String[] DEZENAS = {"", "", "Vinte", "Trinta", "Quarenta", "Cinquenta", "Sessenta", "Setenta", "Oitenta", "Noventa"};
        private static final String[] CENTENAS = {"", "Cento", "Duzentos", "Trezentos", "Quatrocentos", "Quinhentos", "Seiscentos", "Setecentos", "Oitocentos", "Novecentos"};

        public static String valorPorExtenso(double v) {
            if (v == 0) return "Zero Reais";
            BigDecimal bd = new BigDecimal(v).setScale(2, BigDecimal.ROUND_HALF_EVEN);
            BigInteger inteiro = bd.toBigInteger();
            BigInteger centavos = bd.subtract(new BigDecimal(inteiro)).multiply(new BigDecimal(100)).toBigInteger();
            String ret = "";
            if (inteiro.compareTo(BigInteger.ZERO) > 0) {
                ret = converter(inteiro) + (inteiro.compareTo(BigInteger.ONE) == 0 ? " Real" : " Reais");
            }
            if (centavos.compareTo(BigInteger.ZERO) > 0) {
                if (!ret.isEmpty()) ret += " e ";
                ret += converter(centavos) + (centavos.compareTo(BigInteger.ONE) == 0 ? " Centavo" : " Centavos");
            }
            return ret;
        }
        private static String converter(BigInteger n) {
            if (n.compareTo(new BigInteger("1000")) < 0) return converterAte999(n.intValue());
            if (n.compareTo(new BigInteger("1000000")) < 0) {
                int milhar = n.divide(new BigInteger("1000")).intValue();
                int resto = n.remainder(new BigInteger("1000")).intValue();
                String sMilhar = (milhar == 1 ? "Um Mil" : converterAte999(milhar) + " Mil"); 
                if (resto == 0) return sMilhar;
                if (resto <= 100 || resto % 100 == 0) return sMilhar + " e " + converterAte999(resto);
                return sMilhar + ", " + converterAte999(resto);
            }
            return n.toString(); 
        }
        private static String converterAte999(int n) {
            if (n == 0) return "";
            if (n == 100) return "Cem";
            if (n < 20) return UNIDADES[n];
            if (n < 100) return DEZENAS[n / 10] + (n % 10 != 0 ? " e " + UNIDADES[n % 10] : "");
            return CENTENAS[n / 100] + (n % 100 != 0 ? " e " + converterAte999(n % 100) : "");
        }
    }
}