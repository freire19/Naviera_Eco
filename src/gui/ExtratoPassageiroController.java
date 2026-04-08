package gui;

import dao.ConexaoBD;
import dao.PassageiroDAO;
import dao.PassagemDAO;
import dao.ReciboQuitacaoPassageiroDAO;
import model.Passageiro;
import model.Passagem;
import model.ReciboQuitacaoPassageiro;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.math.BigDecimal;

// Imports Impressão
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import javax.imageio.ImageIO;

public class ExtratoPassageiroController implements Initializable {

    @FXML private Label lblNomeClienteTopo;
    @FXML private ComboBox<String> cmbPassageiro;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private Label lblTotalPassagens;
    @FXML private Label lblTotalPago;
    @FXML private Label lblDivida;
    @FXML private Button btnQuitarTudo;

    @FXML private TableView<ItemExtrato> tabela;
    @FXML private TableColumn<ItemExtrato, String> colData;
    @FXML private TableColumn<ItemExtrato, String> colRota;
    @FXML private TableColumn<ItemExtrato, String> colDescricao;
    @FXML private TableColumn<ItemExtrato, String> colValor;
    @FXML private TableColumn<ItemExtrato, String> colPago;
    @FXML private TableColumn<ItemExtrato, String> colSaldo;
    @FXML private TableColumn<ItemExtrato, String> colStatus;

    private PassagemDAO passagemDAO;
    private PassageiroDAO passageiroDAO;
    private ReciboQuitacaoPassageiroDAO reciboDAO;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private List<String> todosNomesPassageiros = new ArrayList<>();
    private ObservableList<ItemExtrato> listaImprimir;

    private double totalDividaCalculada = 0.0;

    // Dados da Empresa
    private String empNome = "EMPRESA DE NAVEGAÇÃO";
    private String empCnpj = "";
    private String empTelefone = "";
    private String empPathLogo = "";

    private boolean ignoreEvents = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passagemDAO = new PassagemDAO();
        passageiroDAO = new PassageiroDAO();
        reciboDAO = new ReciboQuitacaoPassageiroDAO();

        carregarDadosEmpresa();
        configurarTabela();
        configurarCombos();
        carregarNomesPassageiros();

        try {
            tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception e) { System.err.println("Erro em ExtratoPassageiroController.initialize (CSS): " + e.getMessage()); }
    }

    private void carregarDadosEmpresa() {
        new Thread(() -> {
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT nome_embarcacao, cnpj, telefone, path_logo FROM configuracao_empresa LIMIT 1")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    if(rs.getString("nome_embarcacao") != null) empNome = rs.getString("nome_embarcacao");
                    if(rs.getString("cnpj") != null) empCnpj = rs.getString("cnpj");
                    if(rs.getString("telefone") != null) empTelefone = rs.getString("telefone");
                    if(rs.getString("path_logo") != null) empPathLogo = rs.getString("path_logo");
                }
            } catch (Exception e) { System.err.println("Erro em ExtratoPassageiroController.carregarDadosEmpresa: " + e.getMessage()); }
        }).start();
    }

    private void configuringAutocomplete(ComboBox<String> comboBox, List<String> data) {
        comboBox.setEditable(true);
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (ignoreEvents) return;
            if (newText == null || newText.isEmpty()) return;

            List<String> filtered = data.stream()
                    .filter(s -> s.toUpperCase().contains(newText.toUpperCase()))
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                Platform.runLater(() -> {
                    String currentText = comboBox.getEditor().getText();
                    if (!currentText.equals(newText)) return;
                    try {
                        ignoreEvents = true;
                        if(!comboBox.isShowing()) comboBox.show();
                        comboBox.setItems(FXCollections.observableArrayList(filtered));
                        comboBox.getEditor().setText(currentText);
                        comboBox.getEditor().positionCaret(currentText.length());
                    } finally {
                        ignoreEvents = false;
                    }
                });
            } else { comboBox.hide(); }
        });

        comboBox.setOnAction(e -> {
            if (ignoreEvents) return;
            String sel = comboBox.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ignoreEvents = true;
                comboBox.getEditor().setText(sel);
                ignoreEvents = false;
                buscar(null);
            }
        });
    }

    private void configurarCombos() {
        cmbStatus.setItems(FXCollections.observableArrayList("TODOS", "PENDENTES", "PAGOS"));
        cmbStatus.getSelectionModel().select("TODOS");
        cmbStatus.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) buscar(null);
        });
    }

    private void carregarNomesPassageiros() {
        new Thread(() -> {
            try {
                List<Passageiro> lista = passageiroDAO.listarTodos();
                todosNomesPassageiros.clear();
                for (Passageiro p : lista) todosNomesPassageiros.add(p.getNome());

                Platform.runLater(() -> {
                    cmbPassageiro.setItems(FXCollections.observableArrayList(todosNomesPassageiros));
                    configuringAutocomplete(cmbPassageiro, todosNomesPassageiros);
                });
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void configurarTabela() {
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colRota.setCellValueFactory(new PropertyValueFactory<>("rota"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("valorPago"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoDevedor"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        String centerStyle = "-fx-alignment: CENTER;";
        String centerRightStyle = "-fx-alignment: CENTER-RIGHT;";
        colValor.setStyle(centerRightStyle);
        colPago.setStyle(centerRightStyle);
        colSaldo.setStyle(centerRightStyle);
        colData.setStyle(centerStyle);
        colRota.setStyle(centerStyle);
        colStatus.setStyle(centerStyle);

        colStatus.setCellFactory(column -> new TableCell<ItemExtrato, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                }
            }
        });
        colSaldo.setCellFactory(column -> new TableCell<ItemExtrato, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if (item.contains("0,00")) setStyle("-fx-text-fill: #2e7d32; -fx-alignment: CENTER-RIGHT;");
                    else setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }

    public void carregarExtrato(String nomeCliente) {
        if (nomeCliente != null) {
            ignoreEvents = true;
            cmbPassageiro.setValue(nomeCliente);
            cmbPassageiro.getEditor().setText(nomeCliente);
            ignoreEvents = false;
            Platform.runLater(() -> buscar(null));
        }
    }

    @FXML
    public void buscar(ActionEvent event) {
        String nome = cmbPassageiro.getEditor().getText();
        if (nome == null || nome.trim().isEmpty()) {
            lblNomeClienteTopo.setText("SELECIONE UM PASSAGEIRO");
            return;
        }
        lblNomeClienteTopo.setText(nome.toUpperCase());

        String statusSelecionado = cmbStatus.getValue();
        final String statusFiltro = (statusSelecionado == null) ? "TODOS" : statusSelecionado;

        new Thread(() -> {
            List<Passagem> listaBD = passagemDAO.listarExtratoPorPassageiro(nome, "TODOS");
            ObservableList<ItemExtrato> itens = FXCollections.observableArrayList();

            double totalGeral = 0;
            double totalPago = 0;
            double dividaTemp = 0;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (Passagem p : listaBD) {
                double vTotal = p.getValorTotal() != null ? p.getValorTotal().doubleValue() : 0;
                double vPago = p.getValorPago() != null ? p.getValorPago().doubleValue() : 0;
                double vSaldo = vTotal - vPago;
                if (vSaldo < model.StatusPagamento.TOLERANCIA_PAGAMENTO.doubleValue()) vSaldo = 0.0;

                String stCalculado = model.StatusPagamento.calcularPorSaldo(vSaldo, vPago).name();

                totalGeral += vTotal;
                totalPago += vPago;
                dividaTemp += vSaldo;

                boolean adicionar = true;
                if (statusFiltro.equals("PENDENTES") && stCalculado.equals("PAGO")) adicionar = false;
                if (statusFiltro.equals("PAGOS") && !stCalculado.equals("PAGO")) adicionar = false;

                if (adicionar) {
                    String rota = (p.getOrigem() != null ? p.getOrigem() : "?") + " > " + (p.getDestino() != null ? p.getDestino() : "?");
                    String desc = "Bil. " + p.getNumBilhete();
                    // Aqui ajustamos a data para o formato correto (dd/MM/yyyy)
                    itens.add(new ItemExtrato(dataDisplay(p, dtf), rota, desc, nf.format(vTotal), nf.format(vPago), nf.format(vSaldo), stCalculado));
                }
            }

            double finalGeral = totalGeral;
            double finalPago = totalPago;
            double finalDivida = dividaTemp;

            Platform.runLater(() -> {
                tabela.setItems(itens);
                listaImprimir = itens;

                totalDividaCalculada = finalDivida;

                lblTotalPassagens.setText(nf.format(finalGeral));
                lblTotalPago.setText(nf.format(finalPago));
                lblDivida.setText(nf.format(finalDivida));

                btnQuitarTudo.setDisable(finalDivida <= model.StatusPagamento.TOLERANCIA_PAGAMENTO.doubleValue());
            });
        }).start();
    }

    private String dataDisplay(Passagem p, DateTimeFormatter dtf) {
        if (p.getDataViagem() != null) {
            // Se você tiver as outras datas no objeto Passagem, pode concatenar aqui:
            // return dtf.format(p.getDataViagem()) + " - " + dtf.format(p.getDataSaida());
            return dtf.format(p.getDataViagem()); 
        }
        return "--/--/----";
    }

    @FXML
    public void imprimirSegundaVia(ActionEvent event) {
        String nome = cmbPassageiro.getEditor().getText();
        if (nome == null || nome.isEmpty()) return;

        List<ReciboQuitacaoPassageiro> historico = reciboDAO.listarPorPassageiro(nome);

        if (historico.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Nenhum histórico de pagamento encontrado para este passageiro.").showAndWait();
            return;
        }

        ChoiceDialog<ReciboQuitacaoPassageiro> dialog = new ChoiceDialog<>(historico.get(0), historico);
        dialog.setTitle("2ª Via de Recibo");
        dialog.setHeaderText("Selecione o pagamento que deseja reimprimir:");
        dialog.setContentText("Pagamentos:");

        Optional<ReciboQuitacaoPassageiro> result = dialog.showAndWait();
        if (result.isPresent()) {
            ReciboQuitacaoPassageiro reciboSelecionado = result.get();
            List<ItemExtrato> itensReconstruidos = reconstruirItensDoRecibo(reciboSelecionado.getItensPagos());

            // Passa "2ª VIA" no título para cair na lógica correta de impressão
            imprimirReciboQuitacaoTermica(
                reciboSelecionado.getNomePassageiro(),
                reciboSelecionado.getValorTotal(),
                itensReconstruidos,
                "2ª VIA (" + reciboSelecionado.getFormaPagamento() + ")",
                reciboSelecionado.getDataPagamento()
            );
        }
    }

    private List<ItemExtrato> reconstruirItensDoRecibo(String itensTexto) {
        List<ItemExtrato> lista = new ArrayList<>();
        if (itensTexto == null || itensTexto.isEmpty()) return lista;

        String[] itens = itensTexto.split(";");
        for (String s : itens) {
            try {
                String[] partes = s.split("\\|");
                if (partes.length >= 3) {
                    lista.add(new ItemExtrato(partes[1], "--", partes[0], partes[2], partes[2], "R$ 0,00", "PAGO"));
                }
            } catch (Exception e) { System.err.println("Erro em ExtratoPassageiroController.reconstruirItensDoRecibo: " + e.getMessage()); }
        }
        return lista;
    }

    @FXML
    public void quitarDividaTotal(ActionEvent event) {
        String nome = cmbPassageiro.getEditor().getText();
        if (nome == null || nome.isEmpty()) return;
        if (totalDividaCalculada <= model.StatusPagamento.TOLERANCIA_PAGAMENTO.doubleValue()) return;

        try {
            List<ItemExtrato> itensSendoPagos = new ArrayList<>();
            StringBuilder itensParaSalvar = new StringBuilder();

            if (listaImprimir != null) {
                for (ItemExtrato item : listaImprimir) {
                    if (!item.getStatus().equals("PAGO")) {
                        itensSendoPagos.add(item);
                        String dataCurta = item.getData().length() >= 5 ? item.getData().substring(0, 5) : "--";
                        itensParaSalvar.append(item.getDescricao()).append("|")
                                       .append(dataCurta).append("|")
                                       .append(item.getSaldoDevedor()).append(";");
                    }
                }
            }

            Passagem pDivida = new Passagem();
            pDivida.setValorAPagar(new BigDecimal(totalDividaCalculada));
            pDivida.setValorTotal(new BigDecimal(totalDividaCalculada));
            pDivida.setNomePassageiro(nome);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/FinalizarPagamentoPassagem.fxml"));
            Parent root = loader.load();
            FinalizarPagamentoPassagemController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Quitar Dívida Total - " + nome);
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            controller.setDadosPagamento(stage, pDivida);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                String formaPagamento = "DINHEIRO/PIX"; 

                boolean sucesso = passagemDAO.quitarDividaTotalPassageiro(nome);
                if (sucesso) {
                    ReciboQuitacaoPassageiro novoRecibo = new ReciboQuitacaoPassageiro(nome, totalDividaCalculada, formaPagamento, itensParaSalvar.toString());
                    reciboDAO.salvar(novoRecibo);

                    new Alert(Alert.AlertType.INFORMATION, "Todas as dívidas foram quitadas e o recibo salvo!").showAndWait();
                    // Impressão normal (Recibo de Quitação)
                    imprimirReciboQuitacaoTermica(nome, totalDividaCalculada, itensSendoPagos, formaPagamento, LocalDateTime.now());
                    buscar(null);
                } else {
                    new Alert(Alert.AlertType.ERROR, "Erro ao atualizar status.").showAndWait();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ===========================================================================================
    // IMPRESSÃO TÉRMICA (AJUSTADA: DATAS, N° BILHETE, TÍTULO 2 VIA E CORREÇÃO DO R$ DUPLICADO)
    // ===========================================================================================
    private void imprimirReciboQuitacaoTermica(String nome, double valorPago, List<ItemExtrato> itensPagos, String infoPagamento, LocalDateTime dataEmissao) {
        new Thread(() -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Recibo - " + nome);

            double alturaNecessaria = 160 + (itensPagos.size() * 15) + 180;
            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();
            double width80mm = 226; 
            paper.setSize(width80mm, alturaNecessaria);
            paper.setImageableArea(2, 2, width80mm - 4, alturaNecessaria - 4);
            pf.setPaper(paper);

            job.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    if (pageIndex > 0) return NO_SUCH_PAGE;
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    double width = 190;
                    int y = 10;
                    int x = 5;

                    // Logo
                    if (empPathLogo != null && !empPathLogo.isEmpty()) {
                        try {
                            File f = new File(empPathLogo);
                            if (f.exists()) {
                                Image logo = ImageIO.read(f);
                                int targetH = 40;
                                int targetW = (int) ((double)logo.getWidth(null) / logo.getHeight(null) * targetH);
                                int centerX = (int) (width / 2) - (targetW / 2);
                                g2d.drawImage(logo, centerX, y, targetW, targetH, null);
                                y += targetH + 10;
                            }
                        } catch (Exception e) {}
                    }
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    drawCenteredString(g2d, empNome.length() > 25 ? empNome.substring(0,25) : empNome, width, y);
                    y += 12;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    drawCenteredString(g2d, "CNPJ: " + empCnpj, width, y);
                    y += 15;
                    g2d.drawLine(x, y, (int)width, y);
                    y += 15;

                    // LÓGICA DO TÍTULO (2ª VIA ou NORMAL)
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    if (infoPagamento.toUpperCase().contains("VIA")) {
                        drawCenteredString(g2d, "2ª VIA", width, y);
                        y += 14;
                        drawCenteredString(g2d, "RECIBO DE QUITAÇÃO DE PASSAGEM", width, y);
                    } else {
                        drawCenteredString(g2d, "RECIBO DE QUITAÇÃO DE PASSAGEM", width, y);
                    }
                    y += 20;

                    g2d.setFont(new Font("Arial", Font.BOLD, 9));
                    g2d.drawString("CLIENTE: " + (nome.length() > 25 ? nome.substring(0,25) : nome), x, y);
                    y += 20;

                    // Cabeçalho das colunas
                    int xBilhete = x;
                    int xData = x + 70;
                    int xValor = x + 140;
                    g2d.setFont(new Font("Arial", Font.BOLD, 8));
                    g2d.drawString("BILHETE", xBilhete, y);
                    g2d.drawString("DATA", xData, y);
                    g2d.drawString("VALOR", xValor, y);
                    y += 5;
                    g2d.drawLine(x, y, (int)width, y);
                    y += 12;

                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    for (ItemExtrato item : itensPagos) {
                        // N° no bilhete
                        String bilheteResumido = item.getDescricao().replace("Bil. ", "");
                        g2d.drawString("N° " + bilheteResumido, xBilhete, y);
                        
                        // Data completa (dd/MM/yyyy)
                        g2d.drawString(item.getData(), xData, y);
                        
                        // Valor sem duplicar R$
                        String valorMostrar = item.getValorTotal(); // Já vem "R$ 50,00" do sistema
                        g2d.drawString(valorMostrar, xValor, y);
                        
                        y += 12;
                    }
                    y += 5;
                    g2d.drawLine(x, y, (int)width, y);
                    y += 15;

                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("TOTAL: " + nf.format(valorPago), x, y);
                    y += 20;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    
                    String textoLegal = "Declaro para devidos fins que os bilhetes citados acima foram quitados. Forma: " + infoPagamento + ".";
                    y = drawWrappedText(g2d, textoLegal, x, y, (int)width - 10);
                    
                    y += 15;
                    g2d.drawString("Data Pagto: " + dataEmissao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), x, y);
                    y += 30;
                    g2d.drawLine(x, y, (int)width, y);
                    y += 12;
                    drawCenteredString(g2d, "Assinatura Responsável", width, y);
                    return PAGE_EXISTS;
                }
            }, pf);

            boolean doPrint = job.printDialog();
            if (doPrint) {
                try { job.print(); } catch (PrinterException e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erro");
                        alert.setContentText("Erro ao imprimir. Verifique permissões.");
                        alert.showAndWait();
                    });
                }
            }
        }).start();
    }

    // ===========================================================================================
    // IMPRESSÃO A4 (EXTRATO) - CORRIGIDA: FORÇA O TAMANHO A4
    // ===========================================================================================
    @FXML
    public void imprimirExtrato(ActionEvent event) {
        if (listaImprimir == null || listaImprimir.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Sem dados para imprimir.").showAndWait();
            return;
        }

        new Thread(() -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Extrato - " + cmbPassageiro.getEditor().getText());
            
            // CORREÇÃO PRINCIPAL DO A4 BAGUNÇADO:
            // Força um novo PageFormat com tamanho A4, ignorando o padrão anterior da térmica
            PageFormat pf = job.defaultPage();
            Paper paper = new Paper();
            // Tamanho A4 padrão em pontos (points) ~ 595 x 842
            paper.setSize(595, 842); 
            // Margens seguras
            paper.setImageableArea(20, 20, 555, 802);
            pf.setPaper(paper);

            job.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    if (pageIndex > 0) return NO_SUCH_PAGE;

                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    double width = pageFormat.getImageableWidth();
                    double height = pageFormat.getImageableHeight();

                    int marginX = 30; 
                    int y = 40;
                    int tableWidth = (int)width - (marginX * 2);

                    // --- LOGO ---
                    if (empPathLogo != null && !empPathLogo.isEmpty()) {
                        try {
                            File f = new File(empPathLogo);
                            if (f.exists()) {
                                Image logo = ImageIO.read(f);
                                int targetH = 50;
                                int targetW = (int) ((double)logo.getWidth(null) / logo.getHeight(null) * targetH);
                                int centerX = (int) width / 2;
                                g2d.drawImage(logo, centerX - (targetW/2), y, targetW, targetH, null);
                                y += targetH + 10;
                            }
                        } catch (Exception e) {}
                    } else { y += 60; }

                    g2d.setColor(new Color(0, 51, 102));
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    drawCenteredString(g2d, empNome, width, y);
                    y += 20;
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    drawCenteredString(g2d, "EXTRATO DE PASSAGENS", width, y);
                    y += 30;

                    // Passageiro
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    g2d.drawString("PASSAGEIRO: " + cmbPassageiro.getEditor().getText().toUpperCase(), marginX, y);
                    y += 20;

                    // --- QUADROS DE TOTAIS ---
                    int boxWidth = (tableWidth - 20) / 3;
                    int boxHeight = 40;
                    int xBox = marginX;

                    // Box 1: TOTAL
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRect(xBox, y, boxWidth, boxHeight);
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Arial", Font.BOLD, 9));
                    g2d.drawString("TOTAL", xBox + 5, y + 12);
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString(lblTotalPassagens.getText(), xBox + 5, y + 30);

                    // Box 2: PAGO
                    xBox += boxWidth + 10;
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawRect(xBox, y, boxWidth, boxHeight);
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Arial", Font.BOLD, 9));
                    g2d.drawString("PAGO", xBox + 5, y + 12);
                    g2d.setColor(new Color(46, 125, 50));
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString(lblTotalPago.getText(), xBox + 5, y + 30);

                    // Box 3: DÍVIDA
                    xBox += boxWidth + 10;
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawRect(xBox, y, boxWidth, boxHeight);
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Arial", Font.BOLD, 9));
                    g2d.drawString("DÍVIDA", xBox + 5, y + 12);
                    g2d.setColor(new Color(198, 40, 40));
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString(lblDivida.getText(), xBox + 5, y + 30);

                    y += boxHeight + 25;

                    // --- COLUNAS ---
                    double[] colPercents = {0.12, 0.20, 0.25, 0.11, 0.11, 0.11, 0.10};
                    String[] colNames = {"DATA", "ROTA", "DESCRIÇÃO", "VALOR", "PAGO", "SALDO", "STATUS"};
                    int[] colX = new int[7];
                    int[] colWidths = new int[7];
                    int currentX = marginX;

                    for(int i=0; i<7; i++) {
                        colX[i] = currentX;
                        colWidths[i] = (int)(tableWidth * colPercents[i]);
                        currentX += colWidths[i];
                    }

                    // CABEÇALHO
                    int rowHeight = 18;
                    g2d.setColor(new Color(21, 101, 192));
                    g2d.fillRect(marginX, y, tableWidth, rowHeight);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 8));

                    for(int i=0; i<colNames.length; i++) {
                        if(i >= 3 && i <= 5) {
                            drawRightAlignedString(g2d, colNames[i], colX[i] + colWidths[i] - 5, y + 12);
                        } else {
                            g2d.drawString(colNames[i], colX[i] + 5, y + 12);
                        }
                    }
                    y += rowHeight;

                    // DADOS
                    g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                    for (int i = 0; i < listaImprimir.size(); i++) {
                        if (y > height - 50) break;
                        ItemExtrato item = listaImprimir.get(i);

                        if (i % 2 != 0) {
                            g2d.setColor(new Color(245, 245, 245));
                            g2d.fillRect(marginX, y, tableWidth, rowHeight);
                        } else {
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(marginX, y, tableWidth, rowHeight);
                        }

                        g2d.setColor(Color.BLACK);
                        g2d.drawString(item.getData().length() > 10 ? item.getData().substring(0,10) : item.getData(), colX[0] + 5, y + 12);
                        g2d.drawString(clipString(g2d, item.getRota(), colWidths[1]-10), colX[1] + 5, y + 12);
                        g2d.drawString(clipString(g2d, item.getDescricao(), colWidths[2]-10), colX[2] + 5, y + 12);

                        drawRightAlignedString(g2d, item.getValorTotal(), colX[3] + colWidths[3] - 5, y + 12);
                        drawRightAlignedString(g2d, item.getValorPago(), colX[4] + colWidths[4] - 5, y + 12);
                        if (!item.getSaldoDevedor().contains("0,00")) g2d.setColor(new Color(198, 40, 40));
                        else g2d.setColor(new Color(46, 125, 50));
                        drawRightAlignedString(g2d, item.getSaldoDevedor(), colX[5] + colWidths[5] - 5, y + 12);
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(item.getStatus(), colX[6] + 5, y + 12);

                        y += rowHeight;
                    }

                    // RODAPÉ
                    y = (int)height - 30;
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawLine(marginX, y, (int)width - marginX, y);
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.ITALIC, 8));
                    g2d.drawString("Sistema de Gestão Embarcação - Emissão: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), marginX, y + 15);
                    g2d.drawString("Página 1", (int)width - marginX - 30, y + 15);

                    return PAGE_EXISTS;
                }
            }, pf);

            boolean doPrint = job.printDialog();
            if (doPrint) {
                try { job.print(); } catch (PrinterException e) {
                     Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erro");
                        alert.setContentText("Acesso Negado à Impressora. Execute como Admin.");
                        alert.showAndWait();
                    });
                }
            }
        }).start();
    }

    private void drawRightAlignedString(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, x - textWidth, y);
    }

    private String clipString(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String res = text;
        while (fm.stringWidth(res + "...") > maxWidth && res.length() > 0) {
            res = res.substring(0, res.length() - 1);
        }
        return res + "...";
    }

    private void drawCenteredString(Graphics2D g, String text, double pageWidth, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = (int) ((pageWidth - metrics.stringWidth(text)) / 2);
        g.drawString(text, x, y);
    }

    private int drawWrappedText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split(" ");
        String line = "";
        for (String word : words) {
            if (fm.stringWidth(line + word) < maxWidth) {
                line += word + " ";
            } else {
                g2d.drawString(line, x, y);
                y += fm.getHeight();
                line = word + " ";
            }
        }
        g2d.drawString(line, x, y);
        return y + fm.getHeight();
    }

    @FXML
    public void fechar(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public static class ItemExtrato {
        private String data, rota, descricao, valorTotal, valorPago, saldoDevedor, status;
        public ItemExtrato(String data, String rota, String descricao, String valorTotal, String valorPago, String saldoDevedor, String status) {
            this.data = data; this.rota = rota; this.descricao = descricao;
            this.valorTotal = valorTotal; this.valorPago = valorPago; this.saldoDevedor = saldoDevedor; this.status = status;
        }
        public String getData() { return data; }
        public String getRota() { return rota; }
        public String getDescricao() { return descricao; }
        public String getValorTotal() { return valorTotal; }
        public String getValorPago() { return valorPago; }
        public String getSaldoDevedor() { return saldoDevedor; }
        public String getStatus() { return status; }
    }
}