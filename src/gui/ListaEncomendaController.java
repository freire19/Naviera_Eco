package gui;

import java.io.File;
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
import java.util.stream.Collectors;

import dao.CaixaDAO;
import dao.ConexaoBD;
import dao.EncomendaDAO;
import dao.EncomendaItemDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import model.Caixa;
import model.Encomenda;
import model.EncomendaItem;
import model.Rota;
import model.Viagem;

public class ListaEncomendaController implements Initializable {

    @FXML private BorderPane rootPane;

    // Filtros
    @FXML private ComboBox<Viagem> cmbViagem;
    @FXML private ComboBox<Rota> cmbFiltroRota;
    @FXML private ComboBox<Caixa> cmbFiltroCaixa;
    @FXML private TextField txtFiltroCliente;
    @FXML private TextField txtFiltroNumeroEncomenda;
    @FXML private TextField txtFiltroItem;

    @FXML private RadioButton rbPagoTodas, rbPagoQuitados, rbPagoAbertos;
    @FXML private RadioButton rbEntregueTodas, rbEntregueSim, rbEntregueNao;

    @FXML private Button btnLimparFiltros;
    @FXML private Button btnSair;
    @FXML private Button btnImprimir;

    // Totais (tela)
    @FXML private Label lblQtdTotal;
    @FXML private Label lblTotalRecebido;
    @FXML private Label lblTotalReceber;
    @FXML private Label lblValorTotal;

    // Tabela
    @FXML private TableView<Encomenda> tabelaEncomendas;
    @FXML private TableColumn<Encomenda, String> colNumero;
    @FXML private TableColumn<Encomenda, String> colRemetente;
    @FXML private TableColumn<Encomenda, String> colDestinatario;
    @FXML private TableColumn<Encomenda, BigDecimal> colValorNominal;
    @FXML private TableColumn<Encomenda, BigDecimal> colValorPago;
    @FXML private TableColumn<Encomenda, BigDecimal> colDevedor;
    @FXML private TableColumn<Encomenda, String> colStatus;
    @FXML private TableColumn<Encomenda, String> colRotaNome;
    @FXML private TableColumn<Encomenda, String> colDocRecebedor;

    private EncomendaDAO encomendaDAO;
    private EncomendaItemDAO encomendaItemDAO;
    private ViagemDAO viagemDAO;
    private RotaDAO rotaDAO;
    private CaixaDAO caixaDAO;

    private ObservableList<Encomenda> listaMestraEncomendas;
    private ObservableList<Viagem> obsListaViagens;
    private ObservableList<Rota> obsListaRotas;
    private ObservableList<Caixa> obsListaCaixas;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        encomendaDAO = new EncomendaDAO();
        encomendaItemDAO = new EncomendaItemDAO();
        viagemDAO = new ViagemDAO();
        rotaDAO = new RotaDAO();
        caixaDAO = new CaixaDAO();

        listaMestraEncomendas = FXCollections.observableArrayList();
        obsListaViagens = FXCollections.observableArrayList();
        obsListaRotas = FXCollections.observableArrayList();
        obsListaCaixas = FXCollections.observableArrayList();

        configurarTabela();
        carregarCombos();
        agruparRadioButtons();

        if (txtFiltroCliente != null) {
            txtFiltroCliente.textProperty().addListener((o, ov, nv) -> aplicarFiltros());
        }
        if (txtFiltroNumeroEncomenda != null) {
            txtFiltroNumeroEncomenda.textProperty().addListener((o, ov, nv) -> aplicarFiltros());
        }
        if (txtFiltroItem != null) {
            txtFiltroItem.textProperty().addListener((o, ov, nv) -> aplicarFiltros());
        }
        if (cmbFiltroRota != null) {
            cmbFiltroRota.valueProperty().addListener((o, ov, nv) -> aplicarFiltros());
        }
        if (cmbFiltroCaixa != null) {
            cmbFiltroCaixa.valueProperty().addListener((o, ov, nv) -> aplicarFiltros());
        }

        if (btnLimparFiltros != null) {
            btnLimparFiltros.setOnAction(e -> limparFiltros());
        }
        if (btnSair != null) {
            btnSair.setOnAction(this::handleSair);
        }
        if (btnImprimir != null) {
            btnImprimir.setOnAction(this::handleImprimirLista);
        }

        if (cmbViagem != null) {
            cmbViagem.setCellFactory(param -> new ListCell<Viagem>() {
                @Override
                protected void updateItem(Viagem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item.getId() == null) {
                        setText("--- TODAS AS VIAGENS ---");
                        setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
                    } else {
                        setText(item.toString());
                        setStyle("");
                    }
                }
            });

            cmbViagem.setButtonCell(new ListCell<Viagem>() {
                @Override
                protected void updateItem(Viagem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item.getId() == null) {
                        setText("--- TODAS AS VIAGENS ---");
                    } else {
                        setText(item.toString());
                    }
                }
            });

            cmbViagem.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    carregarEncomendasDaViagem(newV);
                }
            });
        }

        tabelaEncomendas.setRowFactory(tv -> {
            TableRow<Encomenda> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Encomenda rowData = row.getItem();
                    abrirTelaEdicao(rowData);
                }
            });
            return row;
        });
    }

    private void agruparRadioButtons() {
        ToggleGroup groupPago = new ToggleGroup();
        if (rbPagoTodas != null) {
            rbPagoTodas.setToggleGroup(groupPago);
            rbPagoTodas.setOnAction(e -> aplicarFiltros());
            rbPagoTodas.setSelected(true);
            aplicarEstiloRadioButton(rbPagoTodas);
        }
        if (rbPagoQuitados != null) {
            rbPagoQuitados.setToggleGroup(groupPago);
            rbPagoQuitados.setOnAction(e -> aplicarFiltros());
            aplicarEstiloRadioButton(rbPagoQuitados);
        }
        if (rbPagoAbertos != null) {
            rbPagoAbertos.setToggleGroup(groupPago);
            rbPagoAbertos.setOnAction(e -> aplicarFiltros());
            aplicarEstiloRadioButton(rbPagoAbertos);
        }

        ToggleGroup groupEntregue = new ToggleGroup();
        if (rbEntregueTodas != null) {
            rbEntregueTodas.setToggleGroup(groupEntregue);
            rbEntregueTodas.setOnAction(e -> aplicarFiltros());
            rbEntregueTodas.setSelected(true);
            aplicarEstiloRadioButton(rbEntregueTodas);
        }
        if (rbEntregueSim != null) {
            rbEntregueSim.setToggleGroup(groupEntregue);
            rbEntregueSim.setOnAction(e -> aplicarFiltros());
            aplicarEstiloRadioButton(rbEntregueSim);
        }
        if (rbEntregueNao != null) {
            rbEntregueNao.setToggleGroup(groupEntregue);
            rbEntregueNao.setOnAction(e -> aplicarFiltros());
            aplicarEstiloRadioButton(rbEntregueNao);
        }
    }
    
    /**
     * Aplica estilo visual destacado nos RadioButtons para melhor visibilidade
     */
    private void aplicarEstiloRadioButton(RadioButton rb) {
        // Listener para atualizar visual quando selecionado/deselecionado
        rb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            atualizarEstiloRadioButton(rb, isSelected);
        });
        // Aplicar estilo inicial
        atualizarEstiloRadioButton(rb, rb.isSelected());
    }
    
    private void atualizarEstiloRadioButton(RadioButton rb, boolean selecionado) {
        boolean escuro = TemaManager.isModoEscuro();
        
        if (selecionado) {
            // Quando SELECIONADO: fundo azul, texto branco, borda destacada
            String corFundo = escuro ? "#1565c0" : "#0056b3";
            String corBorda = escuro ? "#1976d2" : "#003d82";
            rb.setStyle(
                "-fx-background-color: " + corFundo + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 5 10 5 10; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: " + corBorda + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
            );
        } else {
            // Quando NÃO selecionado: fundo claro/escuro, texto normal
            String corFundo = escuro ? "#2a2a2a" : "#f0f0f0";
            String corTexto = escuro ? "#cccccc" : "#333333";
            String corBorda = escuro ? "#555555" : "#cccccc";
            rb.setStyle(
                "-fx-background-color: " + corFundo + "; " +
                "-fx-text-fill: " + corTexto + "; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 5 10 5 10; " +
                "-fx-border-color: " + corBorda + "; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
            );
        }
    }

    private void configurarTabela() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroEncomenda()));
        colNumero.setCellFactory(tc -> new TableCell<Encomenda, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #1565C0; -fx-font-weight: bold; -fx-font-size: 15px; -fx-alignment: CENTER;");
                }
            }
        });

        colRemetente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRemetente()));
        colDestinatario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDestinatario()));

        if (colDocRecebedor != null) {
            colDocRecebedor.setCellValueFactory(c -> {
                String doc = c.getValue().getDocRecebedor();
                return new SimpleStringProperty(doc != null ? doc : "-");
            });
        }

        colValorNominal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalAPagar()));
        colValorNominal.setCellFactory(tc -> new TableCellMoney());

        colValorPago.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getValorPago()));
        colValorPago.setCellFactory(tc -> new TableCellMoney());

        colDevedor.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getSaldoDevedor()));
        colDevedor.setCellFactory(tc -> new TableCellMoney(true));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0 ? "FALTA" : "PAGO") + " / "
                        + (c.getValue().isEntregue() ? "ENTREGUE" : "PENDENTE")));

        colStatus.setCellFactory(tc -> new TableCell<Encomenda, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Encomenda enc = getTableView().getItems().get(getIndex());
                    if (enc.isEntregue()) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: 800;");
                    } else {
                        setStyle("-fx-text-fill: #d84315; -fx-font-weight: 800;");
                    }
                }
            }
        });

        if (colRotaNome != null) {
            colRotaNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeRota()));
        }
    }

    private static class TableCellMoney extends TableCell<Encomenda, BigDecimal> {
        private final boolean destacarDevedor;

        public TableCellMoney() {
            this(false);
        }

        public TableCellMoney(boolean destacarDevedor) {
            this.destacarDevedor = destacarDevedor;
        }

        @Override
        protected void updateItem(BigDecimal item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(String.format("R$ %,.2f", item));
                if (destacarDevedor && item.compareTo(BigDecimal.ZERO) > 0) {
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                } else if (!destacarDevedor && item.compareTo(BigDecimal.ZERO) > 0) {
                    setStyle("-fx-text-fill: green;");
                } else {
                    setStyle("-fx-text-fill: black;");
                }
            }
        }
    }

    private void carregarCombos() {
        try {
            List<Viagem> listaViagens = new ArrayList<>();
            Viagem todas = new Viagem();
            todas.setId(null);
            todas.setDescricao("TODAS");
            listaViagens.add(todas);

            listaViagens.addAll(viagemDAO.listarTodasViagensResumido());
            obsListaViagens.setAll(listaViagens);

            if (cmbViagem != null) {
                cmbViagem.setItems(obsListaViagens);
                if (obsListaViagens.size() > 1) {
                    Viagem viagemAtual = obsListaViagens.get(1);
                    cmbViagem.setValue(viagemAtual);
                    carregarEncomendasDaViagem(viagemAtual);
                } else {
                    cmbViagem.getSelectionModel().selectFirst();
                    carregarEncomendasDaViagem(todas);
                }
            }

            obsListaRotas.setAll(rotaDAO.listarTodasAsRotasComoObjects());
            if (cmbFiltroRota != null) {
                cmbFiltroRota.setItems(obsListaRotas);
            }

            obsListaCaixas.setAll(caixaDAO.listarTodos());
            if (cmbFiltroCaixa != null) {
                cmbFiltroCaixa.setItems(obsListaCaixas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarEncomendasDaViagem(Viagem viagem) {
        try {
            List<Encomenda> lista;
            if (viagem == null || viagem.getId() == null) {
                lista = encomendaDAO.listarTodos();
            } else {
                lista = encomendaDAO.listarPorViagem(viagem.getId());
            }
            listaMestraEncomendas.setAll(lista);
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void aplicarFiltros() {
        if (listaMestraEncomendas.isEmpty()) {
            tabelaEncomendas.setItems(FXCollections.observableArrayList());
            calcularTotais(listaMestraEncomendas);
            return;
        }

        String nome = txtFiltroCliente != null ? txtFiltroCliente.getText().toUpperCase().trim() : "";
        String numero = txtFiltroNumeroEncomenda != null ? txtFiltroNumeroEncomenda.getText().trim() : "";
        String itemBusca = txtFiltroItem != null ? txtFiltroItem.getText().toUpperCase().trim() : "";
        Rota rota = cmbFiltroRota != null ? cmbFiltroRota.getValue() : null;
        Caixa caixa = cmbFiltroCaixa != null ? cmbFiltroCaixa.getValue() : null;
        boolean mostrarQuitados = rbPagoQuitados != null && rbPagoQuitados.isSelected();
        boolean mostrarAbertos = rbPagoAbertos != null && rbPagoAbertos.isSelected();
        boolean mostrarEntregues = rbEntregueSim != null && rbEntregueSim.isSelected();
        boolean mostrarNaoEntregues = rbEntregueNao != null && rbEntregueNao.isSelected();

        List<Encomenda> filtrados = listaMestraEncomendas.stream().filter(e -> {
            boolean matchNome = nome.isEmpty()
                    || (e.getRemetente() != null && e.getRemetente().toUpperCase().contains(nome))
                    || (e.getDestinatario() != null && e.getDestinatario().toUpperCase().contains(nome));
            boolean matchNumero = numero.isEmpty() || e.getNumeroEncomenda().equals(numero);
            boolean matchCaixa = caixa == null || (e.getIdCaixa() != null && e.getIdCaixa().equals(caixa.getId()));

            boolean matchRota = true;
            if (rota != null && e.getNomeRota() != null) {
                matchRota = e.getNomeRota().toUpperCase().contains(rota.toString().toUpperCase());
            } else if (rota != null) {
                matchRota = false;
            }

            boolean matchPagamento = true;
            if (mostrarQuitados) {
                matchPagamento = e.getSaldoDevedor().compareTo(BigDecimal.ZERO) <= 0;
            }
            if (mostrarAbertos) {
                matchPagamento = e.getSaldoDevedor().compareTo(BigDecimal.ZERO) > 0;
            }

            boolean matchEntrega = true;
            if (mostrarEntregues) {
                matchEntrega = e.isEntregue();
            }
            if (mostrarNaoEntregues) {
                matchEntrega = !e.isEntregue();
            }

            boolean matchItem = true;
            if (!itemBusca.isEmpty()) {
                List<EncomendaItem> itens = encomendaItemDAO.listarPorIdEncomenda(e.getId());
                matchItem = itens.stream().anyMatch(i -> i.getDescricao().toUpperCase().contains(itemBusca));
            }

            return matchNome && matchNumero && matchCaixa && matchRota && matchPagamento && matchEntrega && matchItem;
        }).collect(Collectors.toList());

        tabelaEncomendas.setItems(FXCollections.observableArrayList(filtrados));
        calcularTotais(filtrados);
    }

    private void calcularTotais(List<Encomenda> lista) {
        BigDecimal totalLancado = BigDecimal.ZERO;
        BigDecimal totalRecebido = BigDecimal.ZERO;
        BigDecimal totalReceber = BigDecimal.ZERO;

        for (Encomenda e : lista) {
            if (e.getTotalAPagar() != null) {
                totalLancado = totalLancado.add(e.getTotalAPagar());
            }
            if (e.getValorPago() != null) {
                totalRecebido = totalRecebido.add(e.getValorPago());
            }
            if (e.getSaldoDevedor() != null) {
                totalReceber = totalReceber.add(e.getSaldoDevedor());
            }
        }

        if (lblQtdTotal != null) {
            lblQtdTotal.setText(String.valueOf(lista.size()));
        }
        if (lblValorTotal != null) {
            lblValorTotal.setText(String.format("R$ %,.2f", totalLancado));
        }
        if (lblTotalRecebido != null) {
            lblTotalRecebido.setText(String.format("R$ %,.2f", totalRecebido));
        }
        if (lblTotalReceber != null) {
            lblTotalReceber.setText(String.format("R$ %,.2f", totalReceber));
        }
    }

    private void limparFiltros() {
        if (txtFiltroCliente != null) {
            txtFiltroCliente.clear();
        }
        if (txtFiltroNumeroEncomenda != null) {
            txtFiltroNumeroEncomenda.clear();
        }
        if (txtFiltroItem != null) {
            txtFiltroItem.clear();
        }
        if (cmbFiltroRota != null) {
            cmbFiltroRota.setValue(null);
        }
        if (cmbFiltroCaixa != null) {
            cmbFiltroCaixa.setValue(null);
        }
        if (rbPagoTodas != null) {
            rbPagoTodas.setSelected(true);
        }
        if (rbEntregueTodas != null) {
            rbEntregueTodas.setSelected(true);
        }
        aplicarFiltros();
    }

    // ==================================================================================
    // IMPRESSÃO
    // ==================================================================================
    @FXML
    private void handleImprimirLista(ActionEvent event) {
        if (tabelaEncomendas == null || tabelaEncomendas.getItems().isEmpty()) {
            Alert a = new Alert(AlertType.INFORMATION);
            a.setHeaderText(null);
            a.setContentText("Nenhuma encomenda para imprimir.");
            a.showAndWait();
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Configuração de Impressão");
        alert.setHeaderText("Escolha a orientação do papel");
        ButtonType btnPaisagem = new ButtonType("Paisagem (Deitado)");
        ButtonType btnRetrato = new ButtonType("Retrato (Em Pé)");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnPaisagem, btnRetrato, btnCancelar);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() == btnCancelar) {
            return;
        }

        PageOrientation orientation =
                (result.get() == btnPaisagem) ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return;
        }

        if (!job.showPrintDialog(rootPane.getScene().getWindow())) {
            return;
        }

        Printer printer = job.getPrinter();
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, orientation, Printer.MarginType.DEFAULT);

        List<Encomenda> itensParaImprimir = new ArrayList<>(tabelaEncomendas.getItems());
        itensParaImprimir.sort(Comparator.comparingInt(e -> {
            try {
                return Integer.parseInt(e.getNumeroEncomenda());
            } catch (Exception ex) {
                return 0;
            }
        }));

        int itensPorPagina = 18;

        int totalPaginas = (int) Math.ceil((double) itensParaImprimir.size() / itensPorPagina);
        if (totalPaginas == 0) {
            totalPaginas = 1;
        }

        String nomeViagem =
                (cmbViagem != null && cmbViagem.getValue() != null) ? cmbViagem.getValue().toString() : "TODAS AS VIAGENS";
        String dataViagem = extrairDataViagem(nomeViagem);
        String dataImpressao =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String filtrosTexto = gerarTextoFiltros();

        BigDecimal somaLancado = BigDecimal.ZERO;
        BigDecimal somaDesconto = BigDecimal.ZERO;
        BigDecimal somaRecebido = BigDecimal.ZERO;
        BigDecimal somaReceber = BigDecimal.ZERO;

        for (Encomenda e : itensParaImprimir) {
            if (e.getTotalAPagar() != null) {
                somaLancado = somaLancado.add(e.getTotalAPagar());
            }
            if (e.getDesconto() != null) {
                somaDesconto = somaDesconto.add(e.getDesconto());
            }
            if (e.getValorPago() != null) {
                somaRecebido = somaRecebido.add(e.getValorPago());
            }
            if (e.getSaldoDevedor() != null) {
                somaReceber = somaReceber.add(e.getSaldoDevedor());
            }
        }

        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        for (int paginaIndex = 0; paginaIndex < totalPaginas; paginaIndex++) {

            int fromIndex = paginaIndex * itensPorPagina;
            int toIndex = Math.min(fromIndex + itensPorPagina, itensParaImprimir.size());
            List<Encomenda> paginaItens = itensParaImprimir.subList(fromIndex, toIndex);

            VBox pagina = new VBox(5);
            pagina.setPadding(new Insets(15, 25, 15, 25));
            pagina.setStyle("-fx-background-color: white;");
            pagina.setPrefWidth(printableWidth);
            pagina.setMinWidth(printableWidth);
            pagina.setMaxWidth(printableWidth);

            // CABEÇALHO
            if (paginaIndex == 0) {
                pagina.getChildren().add(criarCabecalhoRelatorio(printableWidth - 50));
            } else {
                Label lblContinuacao = new Label("RELATÓRIO DE ENCOMENDAS (CONTINUAÇÃO)");
                lblContinuacao.setStyle(
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 0 8 0;");
                lblContinuacao.setAlignment(Pos.CENTER);
                lblContinuacao.setPrefWidth(printableWidth - 50);
                pagina.getChildren().add(lblContinuacao);
            }

            // Informações da viagem / filtros / paginação (usando Text)
            VBox infoBox = new VBox(4);
            infoBox.setPadding(new Insets(6, 0, 6, 0));
            infoBox.setAlignment(Pos.CENTER);
            infoBox.setPrefWidth(printableWidth - 50);
            infoBox.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

            double infoWidth = printableWidth - 60;

            Text txtViagem = new Text("DATA DA VIAGEM: " + dataViagem);
            txtViagem.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            txtViagem.setWrappingWidth(infoWidth);

            Text txtInfos = new Text(
                    "IMPRESSO EM: " + dataImpressao + "  |  FILTROS: "
                            + (filtrosTexto.isEmpty() ? "Nenhum" : filtrosTexto));
            txtInfos.setStyle("-fx-font-size: 11px;");
            txtInfos.setWrappingWidth(infoWidth);

            Text txtPag = new Text("PÁGINA: " + (paginaIndex + 1) + "/" + totalPaginas);
            txtPag.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            txtPag.setWrappingWidth(infoWidth);

            infoBox.getChildren().addAll(txtViagem, txtInfos, txtPag);
            pagina.getChildren().add(infoBox);

            // ============================ TABELA ============================
            GridPane grid = new GridPane();
            grid.setPrefWidth(printableWidth - 50);
            grid.setMinWidth(printableWidth - 50);
            grid.setAlignment(Pos.CENTER);

            // Ajuste das larguras das colunas (VALOR com mais espaço)
            ColumnConstraints colNum = new ColumnConstraints();
            colNum.setPercentWidth(5);
            ColumnConstraints colRem = new ColumnConstraints();
            colRem.setPercentWidth(20);
            ColumnConstraints colDest = new ColumnConstraints();
            colDest.setPercentWidth(20);
            ColumnConstraints colRota = new ColumnConstraints();
            colRota.setPercentWidth(13);
            ColumnConstraints colVal = new ColumnConstraints();
            colVal.setPercentWidth(17); // mais espaço para o valor
            ColumnConstraints colStatus = new ColumnConstraints();
            colStatus.setPercentWidth(13);
            ColumnConstraints colRec = new ColumnConstraints();
            colRec.setPercentWidth(12);

            grid.getColumnConstraints().addAll(colNum, colRem, colDest, colRota, colVal, colStatus, colRec);

            // Cabeçalho da tabela
            adicionarLinhaGridCabecalho(grid, 0,
                    "Nº", "REMETENTE", "DESTINATÁRIO", "ROTA", "VALOR", "STATUS", "RECEBEDOR");

            int row = 1;
            for (Encomenda e : paginaItens) {
                boolean isPago = e.getSaldoDevedor().compareTo(BigDecimal.ZERO) <= 0;
                String statusStr =
                        (isPago ? "PAGO" : "FALTA") + " / " + (e.isEntregue() ? "ENTR." : "PEND.");
                boolean linhaPar = (row % 2 == 0);

                adicionarLinhaGridEstilizada(
                        grid,
                        row++,
                        linhaPar,
                        isPago,
                        e.getNumeroEncomenda(),
                        e.getRemetente(),
                        e.getDestinatario(),
                        e.getNomeRota(),
                        String.format("R$ %,.2f", e.getTotalAPagar()),
                        statusStr,
                        (e.getDocRecebedor() != null && !e.getDocRecebedor().isEmpty()) ? "OK" : ""
                );
            }

            // RESUMO FINAL SÓ NA ÚLTIMA PÁGINA
            if (paginaIndex == totalPaginas - 1) {
                adicionarResumoTotaisGrid(grid, row, somaLancado, somaDesconto, somaRecebido, somaReceber);
            }

            pagina.getChildren().add(grid);

            // layout + escala
            Scene scene = new Scene(pagina);
            pagina.applyCss();
            pagina.layout();

            double contentWidth = pagina.getBoundsInParent().getWidth();
            double contentHeight = pagina.getBoundsInParent().getHeight();

            double scaleX = printableWidth / contentWidth;
            double scaleY = printableHeight / contentHeight;
            double scale = Math.min(scaleX, scaleY);

            // Agora escala tanto para diminuir quanto para AUMENTAR, se couber
            if (Math.abs(scale - 1.0) > 0.01) {
                pagina.getTransforms().add(new Scale(scale, scale));
            }

            job.printPage(pageLayout, pagina);
        }

        job.endJob();
    }

    private void adicionarResumoTotaisGrid(
            GridPane grid,
            int row,
            BigDecimal somaLancado,
            BigDecimal somaDesconto,
            BigDecimal somaRecebido,
            BigDecimal somaReceber
    ) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
                "-fx-background-color: #f9f9f9; -fx-border-color: #ddd; "
                        + "-fx-border-width: 1 0 0 0; -fx-padding: 8 10 8 10;");

        Label l1 = new Label("TOTAL LANÇADO:   R$ " + String.format("%,.2f", somaLancado));
        Label l2 = new Label("TOTAL DESCONTO:  R$ " + String.format("%,.2f", somaDesconto));
        Label l3 = new Label("TOTAL RECEBIDO:  R$ " + String.format("%,.2f", somaRecebido));
        Label l4 = new Label("TOTAL A RECEBER: R$ " + String.format("%,.2f", somaReceber));

        String estiloLinha = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;";

        l1.setStyle(estiloLinha);
        l2.setStyle(estiloLinha);
        l3.setStyle(estiloLinha);
        l4.setStyle(estiloLinha + " -fx-text-fill: #c62828;");

        box.getChildren().addAll(l1, l2, l3, l4);

        grid.add(box, 0, row);
        GridPane.setColumnSpan(box, 7);
    }

    private void adicionarLinhaGridEstilizada(
            GridPane grid,
            int row,
            boolean isPar,
            boolean isPago,
            String... valores
    ) {
        String bg = isPar ? "#f2f2f2" : "#ffffff";
        String fontSize = "11px";

        for (int i = 0; i < valores.length; i++) {
            String valor = valores[i] == null ? "" : valores[i];
            Label lbl = new Label(valor);
            lbl.setWrapText(true);
            lbl.setTextOverrun(OverrunStyle.CLIP); // sem "..."
            lbl.setMaxWidth(Double.MAX_VALUE);

            String textColor = "black";
            String weight = "normal";

            if (i == 5) { // coluna "STATUS"
                textColor = isPago ? "#2e7d32" : "#c62828";
                weight = "bold";
            }

            String style =
                    String.format(
                            "-fx-background-color: %s; -fx-font-size: %s; -fx-text-fill: %s; "
                                    + "-fx-font-weight: %s; -fx-border-color: #e0e0e0; "
                                    + "-fx-border-width: 0 0 1 0; -fx-padding: 2 2 2 2;",
                            bg,
                            fontSize,
                            textColor,
                            weight);

            lbl.setStyle(style);

            // alinhamentos por coluna
            if (i == 0) {
                lbl.setAlignment(Pos.CENTER);          // Nº
            } else if (i == 4) {
                lbl.setAlignment(Pos.CENTER_RIGHT);    // VALOR
            } else if (i >= 5) {
                lbl.setAlignment(Pos.CENTER);          // STATUS / RECEBEDOR
            } else {
                lbl.setAlignment(Pos.CENTER_LEFT);     // REMETENTE / DESTINATÁRIO / ROTA
            }

            grid.add(lbl, i, row);
        }
    }

    private void adicionarLinhaGridCabecalho(GridPane grid, int row, String... valores) {
        String style =
                "-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; "
                        + "-fx-font-size: 11px; -fx-padding: 6;";
        for (int i = 0; i < valores.length; i++) {
            Label lbl = new Label(valores[i]);
            lbl.setStyle(style);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setTextOverrun(OverrunStyle.CLIP);
            if (i == 4) {
                lbl.setAlignment(Pos.CENTER_RIGHT);
            } else {
                lbl.setAlignment(Pos.CENTER);
            }
            grid.add(lbl, i, row);
        }
    }

    private VBox criarCabecalhoRelatorio(double width) {
        VBox contentBox = new VBox(4);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPrefWidth(width);
        contentBox.setMinWidth(width);
        contentBox.setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: black;");
        contentBox.setPadding(new Insets(0, 0, 5, 0));

        String sql = "SELECT path_logo, nome_embarcacao FROM configuracao_empresa LIMIT 1";

        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String path = rs.getString("path_logo");
                String emb = rs.getString("nome_embarcacao");

                if (path != null && !path.isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) {
                        Image img = new Image(file.toURI().toString());
                        ImageView imgView = new ImageView(img);
                        imgView.setFitHeight(60);
                        imgView.setPreserveRatio(true);
                        contentBox.getChildren().add(imgView);
                    }
                }

                Label lblEmb = new Label(emb != null ? emb : "EMBARCAÇÃO");
                lblEmb.setStyle(
                        "-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: black;");
                contentBox.getChildren().add(lblEmb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Label lblTitulo = new Label("RELATÓRIO DE ENCOMENDAS");
        lblTitulo.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 15px; -fx-underline: true; -fx-text-fill: black;");
        contentBox.getChildren().add(lblTitulo);

        return contentBox;
    }

    private String gerarTextoFiltros() {
        List<String> filtros = new ArrayList<>();

        if (rbPagoQuitados != null && rbPagoQuitados.isSelected()) {
            filtros.add("Somente Quitados");
        }
        if (rbPagoAbertos != null && rbPagoAbertos.isSelected()) {
            filtros.add("Somente Abertos");
        }
        if (rbEntregueSim != null && rbEntregueSim.isSelected()) {
            filtros.add("Somente Entregues");
        }
        if (rbEntregueNao != null && rbEntregueNao.isSelected()) {
            filtros.add("Somente Pendentes");
        }
        if (cmbFiltroRota != null && cmbFiltroRota.getValue() != null) {
            filtros.add("Rota: " + cmbFiltroRota.getValue());
        }
        if (cmbFiltroCaixa != null && cmbFiltroCaixa.getValue() != null) {
            filtros.add("Caixa: " + cmbFiltroCaixa.getValue());
        }

        return String.join(", ", filtros);
    }

    // pega só a data antes do primeiro "-"
    private String extrairDataViagem(String nomeViagem) {
        if (nomeViagem == null || nomeViagem.isBlank()) {
            return "-";
        }
        int idx = nomeViagem.indexOf('-');
        if (idx > 0) {
            return nomeViagem.substring(0, idx).trim();
        }
        return nomeViagem.trim();
    }

    private void abrirTelaEdicao(Encomenda encomenda) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/InserirEncomenda.fxml"));
            Parent root = loader.load();

            InserirEncomendaController controller = loader.getController();
            controller.setEncomendaParaEdicao(encomenda);

            Stage stage = new Stage();
            stage.setTitle("Editar Encomenda " + encomenda.getNumeroEncomenda());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.showAndWait();

            if (cmbViagem != null && cmbViagem.getValue() != null) {
                carregarEncomendasDaViagem(cmbViagem.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSair(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }
}
