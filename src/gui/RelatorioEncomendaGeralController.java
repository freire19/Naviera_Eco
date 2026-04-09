package gui;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

import javafx.application.Platform;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dao.ConexaoBD;
import dao.EncomendaDAO;
import dao.EncomendaItemDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Encomenda;
import model.EncomendaItem;
import model.Rota;
import model.Viagem;
import gui.util.AlertHelper;

public class RelatorioEncomendaGeralController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private ComboBox<Viagem> cmbViagem;
    @FXML private ComboBox<Rota> cmbRota;
    
    @FXML private RadioButton rbCompleto;
    @FXML private RadioButton rbSimples;
    @FXML private RadioButton rbListaTabular; 
    @FXML private RadioButton rbTabelaPrecos; 
    
    @FXML private ToggleGroup grupoRelatorio;
    @FXML private Button btnSair;
    @FXML private Button btnGerarRelatorio;

    private ViagemDAO viagemDAO;
    private EncomendaDAO encomendaDAO;
    private EncomendaItemDAO encomendaItemDAO;
    private RotaDAO rotaDAO;
    private ObservableList<Viagem> obsListaViagens;
    private ObservableList<Rota> obsListaRotas;

    // Cores e Fontes
    private final String COR_AZUL_ESCURO = "#0d47a1";
    private final String COR_CINZA_CLARO = "#f5f5f5"; // VOLTANDO PARA CINZA PARA SEGURANÇA
    
    private final Font FONT_EMPRESA = Font.font("Arial", FontWeight.BLACK, 16);
    private final Font FONT_ROTA_DESTAQUE = Font.font("Arial", FontWeight.BLACK, 14); 
    private final Font FONT_DATAS = Font.font("Arial", FontWeight.BOLD, 11);
    
    private final Font FONT_TITULO_ENCOMENDA = Font.font("Arial", FontWeight.BLACK, 14);
    private final Font FONT_NORMAL = Font.font("Arial", FontWeight.NORMAL, 10);
    private final Font FONT_NEGRITO = Font.font("Arial", FontWeight.BOLD, 10);
    private final Font FONT_HEADER_ITENS = Font.font("Arial", FontWeight.BOLD, 9); 
    private final Font FONT_TOTAIS_TITULO = Font.font("Arial", FontWeight.BOLD, 12);
    private final Font FONT_TOTAIS_VALOR = Font.font("Arial", FontWeight.BOLD, 13);
    
    private final Font FONT_SIMPLES_NUMERO = Font.font("Arial", FontWeight.BLACK, 12);
    private final Font FONT_SIMPLES_NOME = Font.font("Arial", FontWeight.BOLD, 11);
    
    private final DateTimeFormatter dtfDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dtfApenasData = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Relatorio de Encomendas"); return; }
        viagemDAO = new ViagemDAO();
        encomendaDAO = new EncomendaDAO();
        encomendaItemDAO = new EncomendaItemDAO();
        rotaDAO = new RotaDAO();
        obsListaViagens = FXCollections.observableArrayList();
        obsListaRotas = FXCollections.observableArrayList();
        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<model.Viagem> viagens = viagemDAO.listarTodasViagensResumido();
                List<model.Rota> rotas = rotaDAO.listarTodasAsRotasComoObjects();
                Platform.runLater(() -> {
                    carregarViagensComDados(viagens);
                    carregarRotasComDados(rotas);
                });
            } catch (Exception e) {
                System.err.println("Erro ao carregar dados iniciais RelatorioEncomendaGeral: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    public void setViagemInicial(Viagem viagem) {
        if (viagem != null && viagem.getId() != null) {
            for (Viagem v : obsListaViagens) {
                if (v.getId() != null && v.getId().equals(viagem.getId())) {
                    cmbViagem.setValue(v);
                    break;
                }
            }
        }
    }

    // DR117: aceita dados pre-carregados para ser chamado do Platform.runLater
    private void carregarViagensComDados(List<Viagem> viagens) {
        obsListaViagens.setAll(viagens);
        cmbViagem.setItems(obsListaViagens);
        cmbViagem.setCellFactory(p -> new ListCell<Viagem>() {
            @Override protected void updateItem(Viagem item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.toString()); }
        });
        cmbViagem.setButtonCell(new ListCell<Viagem>() {
            @Override protected void updateItem(Viagem item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.toString()); }
        });
        if (!obsListaViagens.isEmpty()) cmbViagem.getSelectionModel().selectFirst();
    }

    // DR117: aceita dados pre-carregados para ser chamado do Platform.runLater
    private void carregarRotasComDados(List<Rota> rotas) {
        Rota todas = new Rota(); todas.setId(null); todas.setOrigem("TODAS"); todas.setDestino("AS ROTAS");
        obsListaRotas.add(todas);
        obsListaRotas.addAll(rotas);
        cmbRota.setItems(obsListaRotas);
        cmbRota.getSelectionModel().selectFirst();
    }

    @FXML
    void handleGerarRelatorio(ActionEvent event) {
        if (rbTabelaPrecos.isSelected()) {
            executarRelatorioTabelaPrecosGeral();
            return;
        }

        Viagem viagemSelecionada = cmbViagem.getValue();
        if (viagemSelecionada == null) { AlertHelper.warn("Selecione uma Viagem."); return; }

        if (rbCompleto.isSelected()) {
            executarRelatorioCompleto(viagemSelecionada);
        } else if (rbSimples.isSelected()) {
            executarRelatorioSimples(viagemSelecionada);
        } else if (rbListaTabular.isSelected()) {
            executarRelatorioTabularViagem(viagemSelecionada);
        }
    }

    // =========================================================================================
    // 1. RELATÓRIO COMPLETO (CONFERÊNCIA) - CORRIGIDO O CORTE (LAYOUT VERTICAL)
    // =========================================================================================
    private void executarRelatorioCompleto(Viagem viagemSelecionada) {
        Alert alertFiltro = new Alert(AlertType.CONFIRMATION);
        alertFiltro.setTitle("Filtros");
        alertFiltro.setHeaderText("O que deseja imprimir?");
        ButtonType btnTudo = new ButtonType("Tudo");
        ButtonType btnPendentes = new ButtonType("Só Pendentes");
        ButtonType btnFinanceiro = new ButtonType("Falta Pagar");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        alertFiltro.getButtonTypes().setAll(btnTudo, btnPendentes, btnFinanceiro, btnCancelar);
        Optional<ButtonType> res = alertFiltro.showAndWait();
        if (!res.isPresent() || res.get() == btnCancelar) return;

        int tipo = 0;
        String titulo = "RELATÓRIO GERAL";
        if (res.get() == btnPendentes) { tipo = 1; titulo = "RELATÓRIO DE PENDÊNCIAS DE ENTREGA"; }
        else if (res.get() == btnFinanceiro) { tipo = 2; titulo = "RELATÓRIO DE PENDÊNCIAS FINANCEIRAS"; }

        String nomeRota = "TODAS AS ROTAS";
        if (cmbRota.getValue() != null && cmbRota.getValue().getId() != null) nomeRota = cmbRota.getValue().toString().toUpperCase();

        Alert alertPapel = new Alert(AlertType.CONFIRMATION);
        alertPapel.setTitle("Papel");
        alertPapel.setHeaderText("Escolha a orientação:");
        ButtonType btnRetrato = new ButtonType("Retrato (Em Pé)");
        ButtonType btnPaisagem = new ButtonType("Paisagem (Deitado)");
        alertPapel.getButtonTypes().setAll(btnRetrato, btnPaisagem, btnCancelar);
        Optional<ButtonType> resPapel = alertPapel.showAndWait();
        if (!resPapel.isPresent() || resPapel.get() == btnCancelar) return;
        
        PageOrientation orientation = (resPapel.get() == btnPaisagem) ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;

        gerarRelatorioProfissional(viagemSelecionada, cmbRota.getValue(), tipo, titulo, orientation, nomeRota);
    }

    private void gerarRelatorioProfissional(Viagem viagem, Rota rotaFiltro, int tipoFiltro, String titulo, PageOrientation orientation, String nomeRota) {
        List<Encomenda> listaOriginal = encomendaDAO.listarPorViagem(viagem.getId());
        List<Encomenda> listaFiltrada = filtrarLista(listaOriginal, rotaFiltro, tipoFiltro);

        if (listaFiltrada.isEmpty()) { AlertHelper.info("Nada encontrado."); return; }
        
        listaFiltrada.sort(Comparator.comparingInt(e -> { try { return Integer.parseInt(e.getNumeroEncomenda()); } catch (Exception ex) { return 0; } }));

        BigDecimal tTotal = BigDecimal.ZERO, tDesc = BigDecimal.ZERO, tPago = BigDecimal.ZERO, tFalta = BigDecimal.ZERO;
        for(Encomenda e : listaFiltrada) {
            tTotal = tTotal.add(e.getTotalAPagar());
            tDesc = tDesc.add(e.getDesconto() != null ? e.getDesconto() : BigDecimal.ZERO);
            tPago = tPago.add(e.getValorPago() != null ? e.getValorPago() : BigDecimal.ZERO);
            tFalta = tFalta.add(e.getSaldoDevedor());
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(rootPane.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, orientation, Printer.MarginType.DEFAULT);
            double larguraUtil = pageLayout.getPrintableWidth() - 40;
            double alturaUtil = pageLayout.getPrintableHeight();

            List<VBox> paginas = new ArrayList<>();
            int numPagina = 1;
            // isSimples = false (para manter o estilo padrão)
            VBox paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, titulo, true, nomeRota, false); 
            double alturaAtual = 150; 

            for (Encomenda enc : listaFiltrada) {
                List<EncomendaItem> itens = encomendaItemDAO.listarPorIdEncomenda(enc.getId());
                
                // AQUI ESTÁ O BLOCO CORRIGIDO
                VBox bloco = criarBlocoEncomenda(enc, itens, larguraUtil);
                
                Scene tempScene = new Scene(bloco); bloco.applyCss(); bloco.layout();
                double alturaBloco = bloco.getBoundsInLocal().getHeight();

                if (alturaAtual + alturaBloco > alturaUtil - 100) {
                    adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual); numPagina++;
                    paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, titulo, false, nomeRota, false);
                    alturaAtual = 60; 
                }
                paginaAtual.getChildren().add(bloco); alturaAtual += alturaBloco;
            }

            VBox blocoTotais = criarBlocoTotaisGeraisVertical(tTotal, tDesc, tPago, tFalta, larguraUtil);
            Scene tempT = new Scene(blocoTotais); blocoTotais.applyCss(); blocoTotais.layout();
            if (alturaAtual + blocoTotais.getBoundsInLocal().getHeight() > alturaUtil - 100) {
                adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual); numPagina++;
                paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, titulo, false, nomeRota, false);
            }
            paginaAtual.getChildren().add(blocoTotais);
            adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual);

            for (VBox p : paginas) { job.printPage(pageLayout, p); }
            job.endJob();
        }
    }

    // =========================================================================================
    // 2. RELATÓRIO SIMPLES (LISTA RÁPIDA)
    // =========================================================================================
    private void executarRelatorioSimples(Viagem viagem) {
        String nomeRota = "TODAS AS ROTAS";
        if (cmbRota.getValue() != null && cmbRota.getValue().getId() != null) nomeRota = cmbRota.getValue().toString().toUpperCase();
        
        List<Encomenda> listaOriginal = encomendaDAO.listarPorViagem(viagem.getId());
        List<Encomenda> listaFiltrada = filtrarLista(listaOriginal, cmbRota.getValue(), 0);

        if (listaFiltrada.isEmpty()) { AlertHelper.info("Nada encontrado."); return; }
        listaFiltrada.sort(Comparator.comparingInt(e -> { try { return Integer.parseInt(e.getNumeroEncomenda()); } catch (Exception ex) { return 0; } }));

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(rootPane.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double larguraUtil = pageLayout.getPrintableWidth() - 20;
            double alturaUtil = pageLayout.getPrintableHeight();

            List<VBox> paginas = new ArrayList<>();
            int numPagina = 1;
            VBox paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, "LISTA RÁPIDA DE ENTREGA", true, nomeRota, true); 
            double alturaAtual = 150;

            for (Encomenda enc : listaFiltrada) {
                HBox linha = criarLinhaSimples(enc, larguraUtil);
                Scene s = new Scene(linha); linha.applyCss(); linha.layout();
                double altLinha = linha.getBoundsInLocal().getHeight();

                if (alturaAtual + altLinha > alturaUtil - 50) {
                    adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual); numPagina++;
                    paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, "LISTA RÁPIDA DE ENTREGA", false, nomeRota, true);
                    alturaAtual = 60;
                }
                paginaAtual.getChildren().add(linha); alturaAtual += altLinha;
            }
            adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual);
            for (VBox p : paginas) { job.printPage(pageLayout, p); }
            job.endJob();
        }
    }

    private HBox criarLinhaSimples(Encomenda enc, double w) {
        HBox box = new HBox(10);
        box.setPrefWidth(w); 
        box.setPadding(new Insets(8, 5, 8, 5));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #e0e0e0 transparent; -fx-border-width: 0 0 1 0;"); 
        
        Label lNum = new Label("ENC. " + enc.getNumeroEncomenda()); 
        lNum.setFont(FONT_SIMPLES_NUMERO); 
        lNum.setMinWidth(80); 
        lNum.setTextFill(Color.web(COR_AZUL_ESCURO));
        lNum.setStyle("-fx-background-color: transparent;");
        
        Label lNome = new Label(enc.getDestinatario()); 
        lNome.setFont(FONT_SIMPLES_NOME); 
        lNome.setWrapText(true);
        lNome.setStyle("-fx-background-color: transparent;");
        
        box.getChildren().addAll(lNum, lNome);
        return box;
    }

    // =========================================================================================
    // 3. LISTA TABULAR DA VIAGEM
    // =========================================================================================
    private void executarRelatorioTabularViagem(Viagem viagem) {
        String nomeRota = "TODAS AS ROTAS";
        if (cmbRota.getValue() != null && cmbRota.getValue().getId() != null) nomeRota = cmbRota.getValue().toString().toUpperCase();
        List<Encomenda> listaOriginal = encomendaDAO.listarPorViagem(viagem.getId());
        List<Encomenda> listaFiltrada = filtrarLista(listaOriginal, cmbRota.getValue(), 0);

        if (listaFiltrada.isEmpty()) { AlertHelper.info("Nada encontrado."); return; }
        listaFiltrada.sort(Comparator.comparingInt(e -> { try { return Integer.parseInt(e.getNumeroEncomenda()); } catch (Exception ex) { return 0; } }));

        BigDecimal tTotal = BigDecimal.ZERO, tDesc = BigDecimal.ZERO, tPago = BigDecimal.ZERO, tFalta = BigDecimal.ZERO;
        for(Encomenda e : listaFiltrada) {
            tTotal = tTotal.add(e.getTotalAPagar());
            tDesc = tDesc.add(e.getDesconto() != null ? e.getDesconto() : BigDecimal.ZERO);
            tPago = tPago.add(e.getValorPago() != null ? e.getValorPago() : BigDecimal.ZERO);
            tFalta = tFalta.add(e.getSaldoDevedor());
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(rootPane.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
            double larguraUtil = pageLayout.getPrintableWidth() - 20;
            double alturaUtil = pageLayout.getPrintableHeight();

            List<VBox> paginas = new ArrayList<>();
            int numPagina = 1;
            VBox paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, "LISTA DE ENCOMENDAS (TABELA)", true, nomeRota, true);
            HBox headerTabela = criarHeaderTabela(larguraUtil);
            paginaAtual.getChildren().add(headerTabela);
            double alturaAtual = 180;

            int i = 0;
            for (Encomenda enc : listaFiltrada) {
                HBox linha = criarLinhaTabela(enc, larguraUtil, i % 2 == 0);
                Scene s = new Scene(linha); linha.applyCss(); linha.layout();
                double alt = linha.getBoundsInLocal().getHeight();

                if (alturaAtual + alt > alturaUtil - 50) {
                    adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual); numPagina++;
                    paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, "LISTA DE ENCOMENDAS", false, nomeRota, true);
                    paginaAtual.getChildren().add(headerTabela);
                    alturaAtual = 90;
                }
                paginaAtual.getChildren().add(linha); alturaAtual += alt;
                i++;
            }

            VBox blocoTotais = criarBlocoTotaisGeraisVertical(tTotal, tDesc, tPago, tFalta, larguraUtil);
            Scene tempT = new Scene(blocoTotais); blocoTotais.applyCss(); blocoTotais.layout();
            if (alturaAtual + blocoTotais.getBoundsInLocal().getHeight() > alturaUtil - 100) {
                adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual); numPagina++;
                paginaAtual = criarPaginaBase(larguraUtil, alturaUtil, numPagina, viagem, "LISTA DE ENCOMENDAS", false, nomeRota, true);
            }
            paginaAtual.getChildren().add(blocoTotais);

            adicionarRodape(paginaAtual, larguraUtil); paginas.add(paginaAtual);
            for (VBox p : paginas) { job.printPage(pageLayout, p); }
            job.endJob();
        }
    }

    private HBox criarHeaderTabela(double w) {
        HBox box = new HBox(5); box.setPrefWidth(w); box.setPadding(new Insets(5));
        box.setStyle("-fx-background-color: " + COR_AZUL_ESCURO + ";"); 
        box.getChildren().addAll(
            criarCelTabela("Nº", 40, true, true), 
            criarCelTabela("REMETENTE", 150, true, true), 
            criarCelTabela("DESTINATÁRIO", 150, true, true),
            criarCelTabela("TOTAL", 80, true, true), 
            criarCelTabela("PAGO", 80, true, true), 
            criarCelTabela("A RECEBER", 80, true, true), 
            criarCelTabela("STATUS", 100, true, true)
        );
        return box;
    }
    private HBox criarLinhaTabela(Encomenda e, double w, boolean par) {
        HBox box = new HBox(5); box.setPrefWidth(w); box.setPadding(new Insets(3, 5, 3, 5));
        box.setStyle(par ? "-fx-background-color: white;" : "-fx-background-color: #f9f9f9;");
        String st = (e.getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0) ? "PENDENTE" : "PAGO";
        Color corSt = st.equals("PENDENTE") ? Color.RED : Color.GREEN;
        box.getChildren().addAll(
            criarCelTabela(e.getNumeroEncomenda(), 40, false, false), 
            criarCelTabela(e.getRemetente(), 150, false, false), 
            criarCelTabela(e.getDestinatario(), 150, false, false),
            criarCelTabela(fmtMoeda(e.getTotalAPagar()), 80, false, false), 
            criarCelTabela(fmtMoeda(e.getValorPago()), 80, false, false), 
            criarCelTabela(fmtMoeda(e.getSaldoDevedor()), 80, false, false), 
            criarCelTabelaColorida(st, 100, corSt)
        );
        return box;
    }
    
    private Label criarCelTabela(String txt, double w, boolean header, boolean isWhite) {
        Label l = new Label(txt); l.setPrefWidth(w); l.setMinWidth(w); l.setMaxWidth(w);
        l.setFont(header ? FONT_NEGRITO : FONT_NORMAL);
        l.setWrapText(true);
        if(isWhite) l.setTextFill(Color.WHITE);
        return l;
    }
    private Label criarCelTabelaColorida(String txt, double w, Color c) {
        Label l = new Label(txt); l.setPrefWidth(w); l.setFont(FONT_NEGRITO); l.setTextFill(c); return l;
    }

    // =========================================================================================
    // 4. TABELA DE PREÇOS GERAL - HEADER SIMPLIFICADO
    // =========================================================================================
    private void executarRelatorioTabelaPrecosGeral() {
        class PrecoItem { String desc; String un; BigDecimal val; PrecoItem(String d, String u, BigDecimal v){desc=d;un=u;val=v;}}
        List<PrecoItem> itens = new ArrayList<>();

        // BUSCANDO DADOS DA TABELA CORRETA
        String sql = "SELECT nome_item, unidade_medida, preco_unitario_padrao FROM itens_encomenda_padrao ORDER BY nome_item"; 
        
        try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                String nome = rs.getString("nome_item");
                String un = rs.getString("unidade_medida");
                if(un == null || un.trim().isEmpty()) un = "UN";
                itens.add(new PrecoItem(nome, un, rs.getBigDecimal("preco_unitario_padrao")));
            }
        } catch (Exception e) {
            try (Connection conn = ConexaoBD.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT nome_item, preco_unitario_padrao FROM itens_encomenda_padrao ORDER BY nome_item")) {
                ResultSet rs = stmt.executeQuery();
                while(rs.next()) itens.add(new PrecoItem(rs.getString("nome_item"), "UN", rs.getBigDecimal("preco_unitario_padrao")));
            } catch(Exception ex2) {
                AlertHelper.error("Erro ao buscar na tabela 'itens_encomenda_padrao'.");
                return;
            }
        }

        if (itens.isEmpty()) { AlertHelper.info("Tabela de preços está vazia."); return; }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(rootPane.getScene().getWindow())) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double w = pageLayout.getPrintableWidth() - 20;
            double h = pageLayout.getPrintableHeight();

            List<VBox> paginas = new ArrayList<>();
            
            // CABEÇALHO SIMPLIFICADO PARA TABELA DE PREÇOS
            VBox pagina = new VBox(5);
            pagina.setPadding(new Insets(20));
            pagina.setPrefSize(w, h);
            pagina.setStyle("-fx-background-color: white;");
            
            String nomeEmp = "SISTEMA DE ENCOMENDAS";
            try {
                dao.EmpresaDAO empresaDAO = new dao.EmpresaDAO();
                model.Empresa empresa = empresaDAO.buscarPorId(dao.EmpresaDAO.ID_EMPRESA_PRINCIPAL);
                if (empresa != null && empresa.getEmbarcacao() != null) nomeEmp = empresa.getEmbarcacao();
            } catch(Exception e) { System.err.println("Erro ao carregar nome empresa: " + e.getMessage()); }

            Label lEmp = new Label(nomeEmp.toUpperCase()); lEmp.setFont(FONT_EMPRESA); lEmp.setAlignment(Pos.CENTER); lEmp.setMaxWidth(Double.MAX_VALUE);
            Label lTit = new Label("TABELA DE PREÇOS DE ENCOMENDA"); lTit.setFont(Font.font("Arial", FontWeight.BLACK, 14)); lTit.setAlignment(Pos.CENTER); lTit.setMaxWidth(Double.MAX_VALUE);
            
            VBox headerFull = new VBox(5, lEmp, lTit);
            headerFull.setAlignment(Pos.CENTER);
            headerFull.setStyle("-fx-padding: 0 0 15 0;");
            pagina.getChildren().add(headerFull);
            
            HBox headerCol = new HBox(10);
            headerCol.setStyle("-fx-background-color: " + COR_AZUL_ESCURO + "; -fx-padding: 5;");
            Label h1 = new Label("DESCRIÇÃO"); h1.setTextFill(Color.WHITE); h1.setPrefWidth(w * 0.6); h1.setFont(FONT_NEGRITO);
            Label h2 = new Label("UND"); h2.setTextFill(Color.WHITE); h2.setPrefWidth(w * 0.15); h2.setFont(FONT_NEGRITO);
            Label h3 = new Label("PREÇO (R$)"); h3.setTextFill(Color.WHITE); h3.setPrefWidth(w * 0.25); h3.setFont(FONT_NEGRITO); h3.setAlignment(Pos.CENTER_RIGHT);
            headerCol.getChildren().addAll(h1, h2, h3);
            pagina.getChildren().add(headerCol);

            double y = 100;
            int i = 0;
            for(PrecoItem pi : itens) {
                HBox linha = new HBox(10);
                linha.setPadding(new Insets(4));
                linha.setStyle(i%2==0 ? "-fx-background-color: white;" : "-fx-background-color: #f0f0f0;");
                
                Label c1 = new Label(pi.desc); c1.setPrefWidth(w * 0.6); c1.setFont(FONT_NORMAL);
                Label c2 = new Label(pi.un); c2.setPrefWidth(w * 0.15); c2.setFont(FONT_NORMAL);
                Label c3 = new Label(fmtMoeda(pi.val)); c3.setPrefWidth(w * 0.25); c3.setFont(FONT_NORMAL); c3.setAlignment(Pos.CENTER_RIGHT);
                
                linha.getChildren().addAll(c1, c2, c3);
                
                Scene s = new Scene(linha); linha.applyCss(); linha.layout();
                double alt = linha.getBoundsInLocal().getHeight();
                
                if (y + alt > h - 50) {
                    adicionarRodape(pagina, w); paginas.add(pagina);
                    pagina = new VBox(5); pagina.setPadding(new Insets(20)); pagina.setPrefSize(w, h); 
                    pagina.setStyle("-fx-background-color: white;");
                    pagina.getChildren().add(headerCol); 
                    y = 50;
                }
                pagina.getChildren().add(linha); y += alt;
                i++;
            }
            adicionarRodape(pagina, w); paginas.add(pagina);
            for (VBox pg : paginas) job.printPage(pageLayout, pg);
            job.endJob();
        }
    }

    // =========================================================================================
    // UTILITÁRIOS
    // =========================================================================================
    private List<Encomenda> filtrarLista(List<Encomenda> original, Rota rota, int tipo) {
        List<Encomenda> filtrada = new ArrayList<>();
        if (rota != null && rota.getId() != null) {
            String r = rota.toString().toUpperCase();
            original = original.stream().filter(e -> e.getNomeRota() != null && e.getNomeRota().toUpperCase().contains(r)).collect(Collectors.toList());
        }
        if (tipo == 1) filtrada = original.stream().filter(e -> !e.isEntregue()).collect(Collectors.toList());
        else if (tipo == 2) filtrada = original.stream().filter(e -> e.getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
        else filtrada = new ArrayList<>(original);
        return filtrada;
    }

    private VBox criarPaginaBase(double w, double h, int pag, Viagem viagem, String titulo, boolean isPrimeiraPagina, String nomeRota, boolean isSimples) {
        VBox p = new VBox(5);
        p.setPadding(new Insets(5)); 
        p.setPrefSize(w, h);
        p.setMaxWidth(w);
        // Fundo branco
        p.setStyle("-fx-background-color: white;"); 

        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPrefWidth(w);
        header.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
        
        if (isPrimeiraPagina) {
            String nomeEmp = "SISTEMA DE ENCOMENDAS";
            try {
                dao.EmpresaDAO empresaDAO = new dao.EmpresaDAO();
                model.Empresa empresa = empresaDAO.buscarPorId(dao.EmpresaDAO.ID_EMPRESA_PRINCIPAL);
                if (empresa != null && empresa.getEmbarcacao() != null) nomeEmp = empresa.getEmbarcacao();
            } catch(Exception e) { System.err.println("Erro ao carregar nome empresa: " + e.getMessage()); }
            Label lEmp = new Label(nomeEmp.toUpperCase()); lEmp.setFont(FONT_EMPRESA);
            
            Label lRota = new Label("ROTA: " + nomeRota);
            lRota.setFont(FONT_ROTA_DESTAQUE);
            
            if (!isSimples) { 
                 lRota.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 3 10 3 10; -fx-background-radius: 3;");
            } else {
                 lRota.setStyle("-fx-background-color: transparent;");
            }

            String dSaida = "N/D"; String dChegada = "EM ABERTO";
            if(viagem.getDataViagem() != null) dSaida = viagem.getDataViagem().format(dtfApenasData);
            if(viagem.getDataChegada() != null) dChegada = viagem.getDataChegada().format(dtfApenasData);
            
            Label lDatas = new Label("SAÍDA: " + dSaida + "  |  PREV. CHEGADA: " + dChegada); lDatas.setFont(FONT_DATAS);
            Label lTit = new Label(titulo); lTit.setFont(FONT_NORMAL); lTit.setTextFill(Color.DARKGRAY);

            header.getChildren().addAll(lEmp, lRota, lDatas, lTit);
        } else {
            Label lSimples = new Label(titulo + " (Continuação) - Página " + pag);
            lSimples.setFont(FONT_NEGRITO);
            header.getChildren().add(lSimples);
        }
        p.getChildren().add(header);
        return p;
    }

    private void adicionarRodape(VBox p, double w) {
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); p.getChildren().add(spacer);
        Label l = new Label("Impresso em: " + LocalDateTime.now().format(dtfDataHora));
        l.setFont(FONT_NORMAL); l.setAlignment(Pos.CENTER_RIGHT); l.setPrefWidth(w);
        l.setStyle("-fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        p.getChildren().add(l);
    }

    // BLOCO COM VALORES EMPILHADOS (CORRIGIDO PARA NÃO CORTAR) E FUNDO CINZA NO RODAPÉ
    private VBox criarBlocoEncomenda(Encomenda enc, List<EncomendaItem> itens, double larguraTotal) {
        VBox box = new VBox(0); box.setPrefWidth(larguraTotal); box.setMaxWidth(larguraTotal);
        box.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0; -fx-padding: 5 0 5 0;");

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(3, 5, 3, 5));
        header.setStyle("-fx-background-color: " + COR_AZUL_ESCURO + ";"); header.setPrefWidth(larguraTotal);

        Label lNum = new Label("ENC. " + enc.getNumeroEncomenda()); lNum.setFont(FONT_TITULO_ENCOMENDA); lNum.setTextFill(Color.WHITE); lNum.setMinWidth(60);
        VBox boxNomes = new VBox(2); boxNomes.setAlignment(Pos.CENTER_LEFT);
        Label lRem = new Label("REMETENTE:    " + enc.getRemetente()); lRem.setFont(FONT_NEGRITO); lRem.setTextFill(Color.WHITE);
        Label lDest = new Label("DESTINATÁRIO: " + enc.getDestinatario()); lDest.setFont(FONT_NEGRITO); lDest.setTextFill(Color.WHITE);
        boxNomes.getChildren().addAll(lRem, lDest);
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        String stTxt = (enc.getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0) ? "FALTA PAGAR" : "PAGO";
        Label lSt = new Label(stTxt); lSt.setFont(FONT_NEGRITO); lSt.setTextFill(stTxt.equals("PAGO") ? Color.LIGHTGREEN : Color.ORANGE);

        header.getChildren().addAll(lNum, boxNomes, spacer, lSt); box.getChildren().add(header);

        if (itens != null && !itens.isEmpty()) {
            GridPane grid = new GridPane(); grid.setPadding(new Insets(12, 5, 5, 5)); grid.setHgap(10); grid.setVgap(8); grid.setPrefWidth(larguraTotal); 
            ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(8); ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(57); 
            ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(17); ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(18); 
            grid.getColumnConstraints().addAll(c1, c2, c3, c4);

            Label hQtd = new Label("QTD"); hQtd.setFont(FONT_HEADER_ITENS); hQtd.setTextFill(Color.DARKGRAY);
            Label hDesc = new Label("DESCRIÇÃO"); hDesc.setFont(FONT_HEADER_ITENS); hDesc.setTextFill(Color.DARKGRAY);
            Label hUnit = new Label("VL UNIT"); hUnit.setFont(FONT_HEADER_ITENS); hUnit.setTextFill(Color.DARKGRAY); hUnit.setMaxWidth(Double.MAX_VALUE); hUnit.setAlignment(Pos.CENTER_RIGHT);
            Label hTotal = new Label("VL TOTAL"); hTotal.setFont(FONT_HEADER_ITENS); hTotal.setTextFill(Color.DARKGRAY); hTotal.setMaxWidth(Double.MAX_VALUE); hTotal.setAlignment(Pos.CENTER_RIGHT);

            grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);

            int row = 1;
            for(EncomendaItem it : itens) {
                Label q = new Label(it.getQuantidade() + "x"); q.setFont(FONT_NORMAL);
                Label d = new Label(it.getDescricao()); d.setFont(FONT_NORMAL); d.setWrapText(true);
                Label u = new Label(fmtMoeda(it.getValorUnitario())); u.setFont(FONT_NORMAL); u.setAlignment(Pos.CENTER_RIGHT); u.setMaxWidth(Double.MAX_VALUE);
                Label t = new Label(fmtMoeda(it.getValorTotal())); t.setFont(FONT_NORMAL); t.setAlignment(Pos.CENTER_RIGHT); t.setMaxWidth(Double.MAX_VALUE);
                grid.add(q, 0, row); grid.add(d, 1, row); grid.add(u, 2, row); grid.add(t, 3, row); row++;
            }
            box.getChildren().add(grid);
        }

        // RODAPÉ CINZA (#f5f5f5)
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(5, 5, 5, 5));
        footer.setStyle("-fx-background-color: " + COR_CINZA_CLARO + ";");

        Label lAss = new Label("Assinatura: __________________________");
        lAss.setFont(FONT_NORMAL);
        Region r2 = new Region(); HBox.setHgrow(r2, Priority.ALWAYS);

        // VBOX PARA EMPILHAR E NÃO CORTAR
        VBox boxValores = new VBox(2);
        boxValores.setAlignment(Pos.CENTER_RIGHT);

        if (enc.getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0) {
            Label lTot = new Label("Total: " + fmtMoeda(enc.getTotalAPagar())); 
            lTot.setFont(FONT_NEGRITO);
            boxValores.getChildren().add(lTot);

            if (enc.getValorPago() != null && enc.getValorPago().compareTo(BigDecimal.ZERO) > 0) {
                Label lPago = new Label("Pago: " + fmtMoeda(enc.getValorPago()));
                lPago.setFont(FONT_NEGRITO);
                lPago.setTextFill(Color.FORESTGREEN);
                boxValores.getChildren().add(lPago);
            }
            
            Label lFalta = new Label("A Pagar: " + fmtMoeda(enc.getSaldoDevedor())); 
            lFalta.setFont(FONT_NEGRITO); 
            lFalta.setTextFill(Color.RED);
            boxValores.getChildren().add(lFalta);
            
        } else {
            Label lTot = new Label("TOTAL: " + fmtMoeda(enc.getTotalAPagar()) + " (PAGO)");
            lTot.setFont(FONT_NEGRITO);
            lTot.setTextFill(Color.FORESTGREEN);
            boxValores.getChildren().add(lTot);
        }

        footer.getChildren().addAll(lAss, r2, boxValores);
        box.getChildren().add(footer);
        return box;
    }

    private VBox criarBlocoTotaisGeraisVertical(BigDecimal tLancado, BigDecimal tDesc, BigDecimal tRecebido, BigDecimal tAReceber, double w) {
        VBox box = new VBox(8); box.setPrefWidth(w); box.setAlignment(Pos.CENTER_RIGHT); box.setPadding(new Insets(0, 5, 0, 0)); box.setStyle("-fx-background-color: white;"); 
        VBox.setMargin(box, new Insets(40, 0, 10, 0));
        Label lblTit = new Label("RESUMO FINANCEIRO GERAL"); lblTit.setFont(FONT_TITULO_ENCOMENDA);
        box.getChildren().add(lblTit);
        box.getChildren().add(criarLinhaTotal("TOTAL ENCOMENDA (LANÇADO):", tLancado, Color.web(COR_AZUL_ESCURO)));
        box.getChildren().add(criarLinhaTotal("TOTAL DESCONTO:", tDesc, Color.DIMGRAY));
        box.getChildren().add(criarLinhaTotal("TOTAL RECEBIDO (PAGO):", tRecebido, Color.FORESTGREEN));
        Color corFiado = (tAReceber.compareTo(BigDecimal.ZERO) > 0) ? Color.RED : Color.BLACK;
        box.getChildren().add(criarLinhaTotal("TOTAL A RECEBER (FIADO):", tAReceber, corFiado));
        return box;
    }

    private HBox criarLinhaTotal(String titulo, BigDecimal valor, Color corValor) {
        HBox linha = new HBox(10); linha.setAlignment(Pos.CENTER_RIGHT);
        Label lTit = new Label(titulo); lTit.setFont(FONT_TOTAIS_TITULO);
        Label lVal = new Label(fmtMoeda(valor)); lVal.setFont(FONT_TOTAIS_VALOR); lVal.setTextFill(corValor); 
        if (corValor.equals(Color.RED)) lTit.setTextFill(Color.RED);
        linha.getChildren().addAll(lTit, lVal);
        return linha;
    }

    private String fmtMoeda(BigDecimal v) { return "R$ " + String.format("%,.2f", v != null ? v : BigDecimal.ZERO); }
    @FXML void handleSair(ActionEvent event) { TelaPrincipalController.fecharTelaAtual(rootPane); }
}