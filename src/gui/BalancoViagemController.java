package gui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dao.BalancoViagemDAO;
import dao.ConexaoBD;
import javafx.scene.control.Alert;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import model.DadosBalancoViagem;
import model.ItemResumoBalanco;

public class BalancoViagemController {

    @FXML private AnchorPane mainContainer;
    @FXML private ComboBox<String> cmbViagens;

    // ABA 1
    @FXML private VBox vboxListaEntradas; 
    @FXML private Label lblTotalGeralEntradas, lblTotalGeralSaidas, lblSaldoFinal;

    // ABA 2
    @FXML private VBox vboxReceitasDetalhadasTela;
    @FXML private VBox vboxDespesasDetalhadasTela;
    @FXML private BarChart<String, Number> graficoBalanco;

    // CARDS
    @FXML private Label lblCardEntradas, lblCardSaidas, lblCardSaldo, lblCardPendente, lblCardRecebido;

    private DadosBalancoViagem dadosAtuais;
    private int idViagemAtual;
    private String textoViagemSelecionada = "Nenhuma Viagem Selecionada";
    private double totalPendenteGlobal = 0.0;
    
    // Listas internas para impressão
    private List<String> dadosPassagensPrint = new ArrayList<>();
    private List<String> dadosCargasPrint = new ArrayList<>();
    
    private String empresaNome = "F/B DEUS DE ALIANÇA V";
    private String empresaCnpj = "00.000.000/0000-00";
    private String empresaEndereco = "Porto de Manaus/AM";
    private String empresaTelefone = "";
    private String empresaLogoPath = "";

    private static final double PAGE_HEIGHT = 930; 
    private static final double HEADER_HEIGHT = 160;
    private static final double ROW_HEIGHT = 20;

    public void inicializarDados(int idViagem) {
        this.idViagemAtual = idViagem;
        carregarDadosEmpresa();
        carregarComboViagens();
        carregarRelatorio(idViagem);
    }
    
    private void carregarRelatorio(int id) {
        try (Connection con = ConexaoBD.getConnection()) {
            BalancoViagemDAO dao = new BalancoViagemDAO(con);
            this.dadosAtuais = dao.buscarBalancoDaViagem(id);
            if (dadosAtuais.isDadosIncompletos()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Dados Incompletos");
                alert.setHeaderText("O balanço pode estar incompleto");
                alert.setContentText("Algumas seções falharam ao carregar: " + dadosAtuais.getErroDetalhes());
                alert.showAndWait();
            }
            preencherAbaSimplificada();
            carregarDetalhamentoTab2(con, id);
            carregarGraficos();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- ABA 1 ---
    private void preencherAbaSimplificada() {
        vboxListaEntradas.getChildren().clear();
        if (dadosAtuais.getItensReceita().isEmpty()) vboxListaEntradas.getChildren().add(new Label("Nenhuma receita."));
        else {
            for (ItemResumoBalanco item : dadosAtuais.getItensReceita()) {
                HBox ln = new HBox(); ln.setAlignment(Pos.CENTER_LEFT); ln.setPadding(new Insets(3,0,3,0));
                String txt = item.getDescricaoFormatada();
                String d = txt.contains("=") ? txt.split("=")[0].trim() : txt;
                String v = txt.contains("=") ? txt.split("=")[1].trim() : "";
                String c = "#333333";
                if(d.toUpperCase().contains("PASSAGE")) c="#2E7D32"; else if(d.toUpperCase().contains("ENCOMENDA")) c="#EF6C00"; else if(d.toUpperCase().contains("FRETE")) c="#1565C0";
                
                Label ld = new Label(d); ld.setStyle("-fx-text-fill:"+c+"; -fx-font-weight:bold; -fx-font-size:14px;");
                Label lv = new Label(v); lv.setStyle("-fx-text-fill:"+c+"; -fx-font-weight:bold; -fx-font-size:15px;");
                Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
                ln.getChildren().addAll(ld, r, lv); vboxListaEntradas.getChildren().add(ln);
            }
        }
        lblTotalGeralEntradas.setText(formatar(dadosAtuais.getTotalEntradas()));
        lblTotalGeralSaidas.setText(formatar(dadosAtuais.getTotalSaidas()));
        lblSaldoFinal.setText(formatar(dadosAtuais.getLucroLiquido()));
        lblSaldoFinal.setStyle(dadosAtuais.getLucroLiquido()>=0 ? "-fx-text-fill:#2e7d32;" : "-fx-text-fill:#c62828;");
    }

    // --- ABA 2 (VISUAL NA TELA) ---
    private void carregarDetalhamentoTab2(Connection con, int idViagem) {
        vboxReceitasDetalhadasTela.getChildren().clear();
        vboxDespesasDetalhadasTela.getChildren().clear();
        dadosPassagensPrint.clear();
        dadosCargasPrint.clear();

        // 1. Receitas - PASSAGENS (CORRIGIDO: USA TABELA ROTAS)
        // SQL: Agrupa por Origem/Destino
        String sqlP = "SELECT r.origem, r.destino, COUNT(*), SUM(p.valor_total) " +
                      "FROM passagens p " +
                      "LEFT JOIN rotas r ON p.id_rota = r.id " +
                      "WHERE p.id_viagem=? GROUP BY r.origem, r.destino ORDER BY r.origem";
        try (PreparedStatement s = con.prepareStatement(sqlP)) {
            s.setInt(1, idViagem); ResultSet rs = s.executeQuery();
            while(rs.next()) {
                String orig = rs.getString(1) == null ? "ORIGEM" : rs.getString(1).toUpperCase();
                String dest = rs.getString(2) == null ? "DESTINO" : rs.getString(2).toUpperCase();
                // FORMATO: 10 Passagens MANAUS/JUTAI = R$ ...
                String txt = String.format("%02d Passagens %s/%s = %s", rs.getInt(3), orig, dest, formatar(rs.getDouble(4)));
                dadosPassagensPrint.add(txt);
                adicionarLinhaReceitaTela(txt);
            }
        } catch(Exception e) { e.printStackTrace(); }
        
        // 2. Receitas - CARGAS
        try (PreparedStatement s = con.prepareStatement("SELECT 'ENCOMENDA', rota, COUNT(*), SUM(total_a_pagar) FROM encomendas WHERE id_viagem=? GROUP BY rota UNION ALL SELECT 'FRETE', rota_temp, COUNT(*), SUM(valor_total_itens) FROM fretes WHERE id_viagem=? GROUP BY rota_temp")) {
            s.setInt(1,idViagem); s.setInt(2,idViagem); ResultSet rs = s.executeQuery();
            while(rs.next()) {
                String tipo = rs.getString(1);
                String rota = rs.getString(2) == null ? "N/D" : rs.getString(2).toUpperCase();
                String txt = String.format("%02d %ss %s = %s", rs.getInt(3), tipo, rota, formatar(rs.getDouble(4)));
                dadosCargasPrint.add(txt);
                adicionarLinhaReceitaTela(txt);
            }
        } catch(Exception e){ System.err.println("Erro em BalancoViagemController.carregarDetalhamentoTab2 (receitas cargas): " + e.getMessage()); }

        // 3. Despesas Agrupadas
        Map<String, List<LinhaDespesaDetalhada>> despesasMap = new TreeMap<>();
        carregarDespesasAgrupadas(despesasMap);
        
        for (Map.Entry<String, List<LinhaDespesaDetalhada>> entry : despesasMap.entrySet()) {
            String cat = entry.getKey();
            double sub = entry.getValue().stream().mapToDouble(LinhaDespesaDetalhada::getValor).sum();
            
            Label lCat = new Label("CATEGORIA: " + cat); 
            lCat.setStyle("-fx-font-weight:bold; -fx-text-fill:#1565C0; -fx-font-size:12px; -fx-padding: 10 0 2 0;");
            vboxDespesasDetalhadasTela.getChildren().add(lCat);
            vboxDespesasDetalhadasTela.getChildren().add(new Separator());
            
            int i=0;
            for(LinhaDespesaDetalhada d : entry.getValue()) {
                HBox row = new HBox(); row.setPadding(new Insets(3)); 
                if(i++%2!=0) row.setStyle("-fx-background-color:#f9f9f9;");
                Label ld = new Label(d.getDescricao()); ld.setPrefWidth(300); ld.setStyle("-fx-font-size:11px;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                Label lv = new Label(formatar(d.getValor())); lv.setStyle("-fx-font-size:11px;");
                row.getChildren().addAll(ld, sp, lv); vboxDespesasDetalhadasTela.getChildren().add(row);
            }
            HBox subBox = new HBox(); subBox.setAlignment(Pos.CENTER_RIGHT); subBox.setPadding(new Insets(0,0,10,0));
            Label lSub = new Label("TOTAL " + cat + ": " + formatar(sub)); 
            lSub.setStyle("-fx-font-weight:bold; -fx-font-size:11px; -fx-text-fill:#555;");
            subBox.getChildren().add(lSub); vboxDespesasDetalhadasTela.getChildren().add(subBox);
        }

        // 4. Cards e Pendentes
        lblCardEntradas.setText(formatar(dadosAtuais.getTotalEntradas()));
        lblCardSaidas.setText(formatar(dadosAtuais.getTotalSaidas()));
        lblCardSaldo.setText(formatar(dadosAtuais.getLucroLiquido()));
        lblCardSaldo.setStyle(dadosAtuais.getLucroLiquido()>=0?"-fx-text-fill:#1565C0;":"-fx-text-fill:#c62828;");
        
        totalPendenteGlobal = 0.0;
        try {
            PreparedStatement st1 = con.prepareStatement("SELECT SUM(total_a_pagar) FROM encomendas WHERE id_viagem=? AND status_pagamento='PENDENTE'"); st1.setInt(1,idViagem); ResultSet r1=st1.executeQuery(); if(r1.next()) totalPendenteGlobal+=r1.getDouble(1);
            PreparedStatement st2 = con.prepareStatement("SELECT SUM(valor_total_itens) FROM fretes WHERE id_viagem=? AND status_frete='PENDENTE'"); st2.setInt(1,idViagem); ResultSet r2=st2.executeQuery(); if(r2.next()) totalPendenteGlobal+=r2.getDouble(1);
        } catch(Exception e){ System.err.println("Erro em BalancoViagemController.carregarDetalhamentoTab2 (pendentes): " + e.getMessage()); }
        lblCardPendente.setText(formatar(totalPendenteGlobal));
        lblCardRecebido.setText("Caixa: "+formatar(Math.max(0, dadosAtuais.getTotalEntradas()-totalPendenteGlobal)));
    }
    
    private void adicionarLinhaReceitaTela(String txt) {
        HBox row = new HBox(); row.setPadding(new Insets(5)); row.setStyle("-fx-border-color: #eee; -fx-border-width: 0 0 1 0;");
        String[] p = txt.split("=");
        Label desc = new Label(p[0].trim()); desc.setStyle("-fx-font-weight:bold; -fx-text-fill:#333;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(p.length>1?p[1].trim():""); val.setStyle("-fx-font-weight:bold; -fx-text-fill:#2E7D32;");
        row.getChildren().addAll(desc, sp, val); vboxReceitasDetalhadasTela.getChildren().add(row);
    }

    private void carregarGraficos() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        XYChart.Data<String, Number> dEntrada = new XYChart.Data<>("Entradas", dadosAtuais.getTotalEntradas());
        XYChart.Data<String, Number> dSaida = new XYChart.Data<>("Saídas", dadosAtuais.getTotalSaidas());
        series.getData().addAll(dEntrada, dSaida);
        
        graficoBalanco.getData().clear();
        graficoBalanco.getData().add(series);
        
        // CORRIGIDO: FORÇA A COR AZUL NA ENTRADA E VERMELHO NA SAÍDA
        // Usando style diretamente no node e runLater para garantir que o node existe
        javafx.application.Platform.runLater(() -> {
            if(dEntrada.getNode() != null) dEntrada.getNode().setStyle("-fx-bar-fill: #1565C0;"); // AZUL
            if(dSaida.getNode() != null) dSaida.getNode().setStyle("-fx-bar-fill: #C62828;");   // VERMELHO
        });
    }

    // =========================================================================
    //  IMPRESSÃO PAGINADA
    // =========================================================================

    @FXML private void handleImprimirDetalhado() { if(dadosAtuais!=null) imprimirPaginas(true); }
    @FXML private void handleImprimirResumido() { if(dadosAtuais!=null) imprimirPaginas(false); }

    private void imprimirPaginas(boolean detalhado) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if(job!=null && job.showPrintDialog(mainContainer.getScene().getWindow())) {
            List<VBox> paginas = gerarPaginas(detalhado);
            Printer printer = job.getPrinter();
            PageLayout layout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            for(VBox p : paginas) {
                double scale = layout.getPrintableWidth() / 595;
                p.getTransforms().add(new Scale(scale, scale));
                if(!job.printPage(layout, p)) break;
            }
            job.endJob();
        }
    }

    private List<VBox> gerarPaginas(boolean detalhado) {
        List<VBox> paginas = new ArrayList<>();
        Map<String, List<LinhaDespesaDetalhada>> despesasMap = new TreeMap<>();
        if(detalhado) carregarDespesasAgrupadas(despesasMap);

        VBox pagAtual = novaPagina();
        double y = HEADER_HEIGHT;
        addCabecalho(pagAtual, detalhado);

        // 1. RECEITAS
        if(detalhado) {
            y = addBlocoReceitas(pagAtual, y, paginas);
        } else {
            y = addResumoReceitas(pagAtual, y, paginas);
        }

        // 2. DESPESAS AGRUPADAS (AZUL, SEM FUNDO, ZEBRA)
        if(detalhado && !despesasMap.isEmpty()) {
            if(y + 40 > PAGE_HEIGHT) { paginas.add(pagAtual); pagAtual=novaPagina(); y=HEADER_HEIGHT; addCabecalho(pagAtual, detalhado); }
            addTitulo(pagAtual, "DETALHAMENTO DE DESPESAS POR CATEGORIA");
            pagAtual.getChildren().add(new Line(0,0,515,0)); y+=30;

            for(Map.Entry<String, List<LinhaDespesaDetalhada>> entry : despesasMap.entrySet()) {
                String cat = entry.getKey();
                List<LinhaDespesaDetalhada> itens = entry.getValue();
                double sub = itens.stream().mapToDouble(LinhaDespesaDetalhada::getValor).sum();

                if(y + 40 > PAGE_HEIGHT) { paginas.add(pagAtual); pagAtual=novaPagina(); y=HEADER_HEIGHT; addCabecalho(pagAtual, detalhado); }
                
                Label lCat = new Label("CATEGORIA: " + cat.toUpperCase()); 
                lCat.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                lCat.setTextFill(Color.web("#1565C0")); 
                lCat.setPrefWidth(515);
                pagAtual.getChildren().addAll(new Region(){{setPrefHeight(10);}}, lCat); y+=35;

                int cnt=0;
                for(LinhaDespesaDetalhada item : itens) {
                    if(y + ROW_HEIGHT > PAGE_HEIGHT) {
                        paginas.add(pagAtual); pagAtual=novaPagina(); y=HEADER_HEIGHT; addCabecalho(pagAtual, detalhado);
                        pagAtual.getChildren().add(new Label("Continuação ("+cat+")..."){{setFont(Font.font(10));}}); y+=15;
                    }
                    pagAtual.getChildren().add(criarLinhaTabelaDetalhada(item.getDescricao(), item.getValor(), cnt++%2!=0, false));
                    y+=ROW_HEIGHT;
                }

                if(y + 25 > PAGE_HEIGHT) { paginas.add(pagAtual); pagAtual=novaPagina(); y=HEADER_HEIGHT; addCabecalho(pagAtual, detalhado); }
                HBox subBox = new HBox(new Text("TOTAL "+cat.toUpperCase()+": "+formatar(sub)){{setFont(Font.font("Arial",FontWeight.BOLD,11));}});
                subBox.setAlignment(Pos.CENTER_RIGHT); subBox.setPadding(new Insets(5,0,0,0));
                pagAtual.getChildren().addAll(subBox, new Region(){{setPrefHeight(5);}}); y+=25;
            }
        } else if (!detalhado) {
            y = addResumoSaidas(pagAtual, y, paginas);
        }

        // 3. CARDS FINAIS (AGORA LOGO APÓS O CONTEÚDO)
        // Se não cabe os cards (aprox 80px)
        if (y + 100 > PAGE_HEIGHT) { 
             paginas.add(pagAtual); pagAtual = novaPagina(); addCabecalho(pagAtual, detalhado);
        }
        
        pagAtual.getChildren().add(new Region(){{setPrefHeight(20);}}); // Espaço antes dos cards
        
        HBox cards = new HBox(15); cards.setAlignment(Pos.CENTER);
        cards.getChildren().add(cardBox("ENTRADAS", dadosAtuais.getTotalEntradas(), Color.GREEN));
        cards.getChildren().add(cardBox("SAÍDAS", dadosAtuais.getTotalSaidas(), Color.BLACK));
        cards.getChildren().add(cardBox("SALDO", dadosAtuais.getLucroLiquido(), dadosAtuais.getLucroLiquido()>=0?Color.BLUE:Color.RED));
        cards.getChildren().add(cardBox("A RECEBER", totalPendenteGlobal, Color.ORANGE));
        pagAtual.getChildren().add(cards);
        
        // 4. ASSINATURA (SEMPRE NO RODAPÉ ABSOLUTO)
        VBox footerBox = new VBox(20); footerBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox ass = new HBox(80); ass.setAlignment(Pos.CENTER);
        ass.getChildren().addAll(assLine("Responsável Financeiro"), assLine("Comandante / Gerente"));
        footerBox.getChildren().addAll(new Region(){{setPrefHeight(40);}}, ass); 

        // Mola (Spring) que empurra tudo para baixo
        Region spring = new Region(); VBox.setVgrow(spring, Priority.ALWAYS);
        pagAtual.getChildren().add(spring);
        pagAtual.getChildren().add(footerBox);
        
        paginas.add(pagAtual);

        // Numeração
        int total = paginas.size();
        for(int i=0; i<total; i++) {
            VBox p = paginas.get(i);
            HBox num = new HBox(new Text("Página " + (i+1) + " / " + total)); num.setAlignment(Pos.CENTER_RIGHT); num.setPadding(new Insets(10,0,0,0));
            p.getChildren().add(num);
        }
        return paginas;
    }

    private void carregarDespesasAgrupadas(Map<String, List<LinhaDespesaDetalhada>> map) {
        String sql = "SELECT s.descricao, c.nome, s.valor_total FROM financeiro_saidas s LEFT JOIN categorias_despesa c ON s.id_categoria = c.id WHERE s.id_viagem=? ORDER BY c.nome, s.descricao";
        try(Connection c=ConexaoBD.getConnection(); PreparedStatement s=c.prepareStatement(sql)){ s.setInt(1,idViagemAtual); ResultSet rs=s.executeQuery();
            while(rs.next()){ String cat=rs.getString(2)==null?"OUTROS":rs.getString(2); map.computeIfAbsent(cat,k->new ArrayList<>()).add(new LinhaDespesaDetalhada("-",rs.getString(1),cat,rs.getDouble(3))); }
        }catch(Exception e){ System.err.println("Erro em BalancoViagemController.carregarDespesasAgrupadas: " + e.getMessage()); }
    }

    // --- MONTAGEM LAYOUT ---
    private VBox novaPagina() { VBox p=new VBox(0); p.setPadding(new Insets(40)); p.setPrefWidth(595); p.setMinHeight(842); p.setStyle("-fx-background-color:white;"); return p; }
    private void addTitulo(VBox p, String t) { p.getChildren().add(new Text(t){{setFont(Font.font("Arial",FontWeight.BOLD,12));}}); }
    
    private void addCabecalho(VBox p, boolean det) {
        VBox h = new VBox(5); h.setAlignment(Pos.CENTER);
        if(empresaLogoPath!=null && !empresaLogoPath.isEmpty()) { try{ImageView iv=new ImageView(new Image("file:"+empresaLogoPath)); iv.setFitHeight(50); iv.setPreserveRatio(true); h.getChildren().add(iv);}catch(Exception e){} }
        else { try{ImageView iv=new ImageView(new Image(getClass().getResourceAsStream("/gui/icons/logo_login.png"))); iv.setFitHeight(50); iv.setPreserveRatio(true); h.getChildren().add(iv);}catch(Exception e){} }
        Text t1 = new Text(empresaNome.toUpperCase()); t1.setFont(Font.font("Arial",FontWeight.BOLD,20)); t1.setFill(Color.web("#1565C0")); // Azul
        h.getChildren().addAll(t1, new Text(empresaEndereco){{setFont(Font.font("Arial",10));}}, new Text("CNPJ: "+empresaCnpj){{setFont(Font.font("Arial",10));}});
        p.getChildren().addAll(h, new Region(){{setPrefHeight(10);}}, new Line(0,0,515,0), new Region(){{setPrefHeight(10);}});
        Text t2 = new Text(det?"RELATÓRIO DETALHADO":"RESUMO GERAL"); t2.setFont(Font.font("Arial",FontWeight.BOLD,14));
        VBox tb = new VBox(3, t2, new Text(textoViagemSelecionada){{setFont(Font.font(10));}}); tb.setAlignment(Pos.CENTER);
        p.getChildren().addAll(tb, new Region(){{setPrefHeight(15);}});
    }

    private double addBlocoReceitas(VBox p, double y, List<VBox> pg) {
        addTitulo(p, "RECEITAS DETALHADAS"); p.getChildren().add(new Line(0,0,515,0)); y+=20;
        int i=0;
        for(String s : dadosPassagensPrint) { p.getChildren().add(criarLinhaTabelaDetalhada(s,0,i++%2!=0,true)); y+=ROW_HEIGHT; }
        for(String s : dadosCargasPrint) { p.getChildren().add(criarLinhaTabelaDetalhada(s,0,i++%2!=0,true)); y+=ROW_HEIGHT; }
        HBox tot = new HBox(new Text("TOTAL RECEITAS: "+formatar(dadosAtuais.getTotalEntradas())){{setFont(Font.font("Arial",FontWeight.BOLD,12));setFill(Color.GREEN);}});
        tot.setAlignment(Pos.CENTER_RIGHT); p.getChildren().addAll(new Region(){{setPrefHeight(5);}}, tot, new Region(){{setPrefHeight(15);}});
        return y+30;
    }
    
    private double addResumoReceitas(VBox p, double y, List<VBox> pg) {
        p.getChildren().addAll(criarLinhaResumoColorida("TOTAL PASSAGENS", dadosAtuais.getTotalPassagens(), false), criarLinhaResumoColorida("TOTAL ENCOMENDAS", dadosAtuais.getTotalEncomendas(), false), criarLinhaResumoColorida("TOTAL FRETES", dadosAtuais.getTotalFretes(), false));
        p.getChildren().addAll(new Region(){{setPrefHeight(10);}}, new Line(0,0,490,0), new Region(){{setPrefHeight(10);}});
        p.getChildren().add(criarLinhaResumoColorida("TOTAL ENTRADAS:", dadosAtuais.getTotalEntradas(), true));
        return y+100;
    }
    
    private double addResumoSaidas(VBox p, double y, List<VBox> pg) {
        p.getChildren().add(criarLinhaResumoColorida("TOTAL SAÍDAS:", dadosAtuais.getTotalSaidas(), true));
        return y+50;
    }

    private VBox cardBox(String t, double v, Color c) {
        VBox b=new VBox(5); b.setPrefWidth(120); b.setPadding(new Insets(10)); b.setAlignment(Pos.CENTER);
        b.setStyle("-fx-border-color: black; -fx-border-width:1;"); 
        if(c==Color.GREEN) b.setStyle("-fx-border-color: green; -fx-border-width:2;");
        if(c==Color.RED) b.setStyle("-fx-border-color: red; -fx-border-width:2;");
        if(c==Color.BLUE) b.setStyle("-fx-border-color: blue; -fx-border-width:2;");
        if(c==Color.ORANGE) b.setStyle("-fx-border-color: orange; -fx-border-width:2;");
        b.getChildren().addAll(new Text(t){{setFont(Font.font("Arial",FontWeight.BOLD,10));}}, new Text(formatar(v)){{setFont(Font.font("Arial",FontWeight.BOLD,13));setFill(c);}}); return b;
    }
    private VBox assLine(String t) { VBox v=new VBox(5); v.setAlignment(Pos.CENTER); v.getChildren().addAll(new Line(0,0,150,0), new Text(t){{setFont(Font.font(9));}}); return v; }
    
    private HBox criarLinhaResumoColorida(String l, double v, boolean t) { 
        HBox h = new HBox(); h.setAlignment(Pos.CENTER_LEFT); Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS); 
        Color cv = l.contains("ENTRADA") || l.contains("RECEITAS") ? Color.GREEN : Color.BLACK;
        h.getChildren().addAll(new Label(l){{setFont(Font.font("Arial",FontWeight.BOLD,t?12:11));}}, s, new Label(formatar(v)){{setFont(Font.font("Arial",FontWeight.BOLD,t?14:12));setTextFill(cv);}}); return h; 
    }
    private HBox criarLinhaTabelaDetalhada(String txt, double v, boolean zeb, boolean receita) { 
        HBox h = new HBox(); h.setAlignment(Pos.CENTER_LEFT); h.setPadding(new Insets(3)); 
        if(zeb) h.setStyle("-fx-background-color: #f5f5f5;"); 
        
        String d = txt; double val = v;
        if(receita && txt.contains("=")) {
             String[] p = txt.split("="); d = p[0];
             try { val = Double.parseDouble(p[1].replace("R$", "").replace(".", "").replace(",", ".").trim()); } catch(Exception e){}
        }
        
        h.getChildren().add(new Text(d){{setFont(Font.font("Arial",11));}});
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Text tVal = new Text(formatar(val)); tVal.setFont(Font.font("Arial",11));
        if(receita) tVal.setFill(Color.GREEN); 
        
        h.getChildren().addAll(s, tVal); return h; 
    }

    private String formatar(double v) { return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(v); }
    private void carregarDadosEmpresa() { String sql = "SELECT nome_embarcacao, cnpj, endereco, telefone, path_logo FROM configuracao_empresa LIMIT 1"; try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) { if (rs.next()) { if (rs.getString(1) != null) this.empresaNome = rs.getString(1); if (rs.getString(2) != null) this.empresaCnpj = rs.getString(2); if (rs.getString(3) != null) this.empresaEndereco = rs.getString(3); if (rs.getString(4) != null) this.empresaTelefone = rs.getString(4); if (rs.getString(5) != null) this.empresaLogoPath = rs.getString(5); } } catch (Exception e) { System.err.println("Erro em BalancoViagemController.carregarDadosEmpresa: " + e.getMessage()); } }
    
    // Filtro ComboBox ajustado
    private void carregarComboViagens() { 
        String sql = "SELECT v.id_viagem, e.nome, v.data_viagem, v.data_chegada FROM viagens v JOIN embarcacoes e ON v.id_embarcacao = e.id_embarcacao ORDER BY v.data_viagem DESC"; 
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) { 
            ObservableList<String> l = FXCollections.observableArrayList(); ResultSet rs = stmt.executeQuery(); SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); 
            while(rs.next()) { 
                String saida = rs.getDate(3)!=null ? sdf.format(rs.getDate(3)) : "--/--/----";
                String chegada = rs.getDate(4)!=null ? sdf.format(rs.getDate(4)) : "--/--/----";
                l.add(String.format("Saída: %s - Chegada: %s (ID: %d)", saida, chegada, rs.getInt(1))); 
                if(rs.getInt(1)==idViagemAtual) { cmbViagens.setValue(l.get(l.size()-1)); textoViagemSelecionada=l.get(l.size()-1); } 
            } cmbViagens.setItems(l); 
        } catch(Exception e) { System.err.println("Erro em BalancoViagemController.carregarComboViagens: " + e.getMessage()); }
    }

    @FXML private void handleFiltrarViagem(ActionEvent event) { String sel = cmbViagens.getValue(); if (sel != null) { try { int id = Integer.parseInt(sel.substring(sel.lastIndexOf("(ID: ") + 5, sel.lastIndexOf(")"))); this.idViagemAtual = id; this.textoViagemSelecionada = sel; carregarRelatorio(id); } catch (Exception e) { System.err.println("Erro em BalancoViagemController.handleFiltrarViagem: " + e.getMessage()); } } }
    
    public static class LinhaDespesaDetalhada { private final String data; private final String descricao; private final String categoria; private final double valor; public LinhaDespesaDetalhada(String d, String de, String c, double v) { this.data=d; this.descricao=de; this.categoria=c; this.valor=v; } public String getData() { return data; } public String getDescricao() { return descricao; } public String getCategoria() { return categoria; } public double getValor() { return valor; } }
}