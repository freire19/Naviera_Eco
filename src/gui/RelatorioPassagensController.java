package gui;

import dao.AuxiliaresDAO;
import dao.PassagemDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;
import model.Passagem;
import model.Rota;
import model.Viagem;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import gui.util.AlertHelper;

public class RelatorioPassagensController implements Initializable {

    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private ComboBox<String> cmbViagem;
    @FXML private ComboBox<String> cmbRota;
    @FXML private ComboBox<String> cmbTipoPagamento;
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private ComboBox<String> cmbAgente;
    @FXML private ComboBox<String> cmbTipoPassagem;
    @FXML private ComboBox<String> cmbStatusPagamento;
    @FXML private Label lblTotalVendido;
    @FXML private Label lblTotalRecebido;
    @FXML private Label lblTotalAReceber;
    @FXML private VBox chartContainer1;
    @FXML private VBox chartContainer2;
    @FXML private TableView<Passagem> tableRelatorio;
    @FXML private TableColumn<Passagem, Integer> colBilhete;
    @FXML private TableColumn<Passagem, String> colRota;
    @FXML private TableColumn<Passagem, String> colTipoPassagem;
    @FXML private TableColumn<Passagem, String> colAgente;
    @FXML private TableColumn<Passagem, LocalDate> colDataViagem;
    @FXML private TableColumn<Passagem, BigDecimal> colValorTotal;
    @FXML private TableColumn<Passagem, BigDecimal> colValorPago;
    @FXML private TableColumn<Passagem, BigDecimal> colDevedor;
    @FXML private TableColumn<Passagem, String> colFormaPagamento;
    @FXML private TableColumn<Passagem, String> colCaixa;
    @FXML private Button btnImprimirRelatorio;

    private PassagemDAO passagemDAO;
    private ViagemDAO viagemDAO;
    private RotaDAO rotaDAO;
    private AuxiliaresDAO auxiliaresDAO;
    private ObservableList<Passagem> passagensData = FXCollections.observableArrayList();

    // Flag para evitar que listeners disparem durante carregamento inicial
    private boolean inicializando = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Relatorio de Passagens"); return; }
        passagemDAO = new PassagemDAO();
        viagemDAO = new ViagemDAO();
        rotaDAO = new RotaDAO();
        auxiliaresDAO = new AuxiliaresDAO();

        configurarColunasTabela();

        // Carregar TUDO em background para nao travar a UI
        carregarTudoEmBackground();
    }

    private void carregarTudoEmBackground() {
        new Thread(() -> {
            try {
                // --- Fase 1: Carregar filtros (queries leves) ---
                Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();

                List<String> viagens = new ArrayList<>();
                viagens.add("Todas");
                viagens.addAll(viagemDAO.listarViagensParaComboBox());

                List<String> rotasStrings = new ArrayList<>();
                rotasStrings.add("Todas");
                try {
                    List<Rota> rotasObjects = rotaDAO.listarTodasAsRotasComoObjects();
                    if (rotasObjects != null) {
                        for (Rota r : rotasObjects) rotasStrings.add(r.toString());
                    }
                } catch (Exception e) { System.err.println("RelatorioPassagensController.initialize: erro ao listar rotas — " + e.getMessage()); }

                List<String> tiposPagamento = new ArrayList<>();
                tiposPagamento.add("Todos");
                tiposPagamento.addAll(auxiliaresDAO.listarTiposPagamento());

                List<String> caixas = new ArrayList<>();
                caixas.add("Todos");
                caixas.addAll(auxiliaresDAO.listarCaixas());

                List<String> agentes = new ArrayList<>();
                agentes.add("Todos");
                agentes.addAll(auxiliaresDAO.listarAuxiliar("aux_agentes", "nome_agente"));

                List<String> tiposPassagemAux = new ArrayList<>();
                tiposPassagemAux.add("Todos");
                tiposPassagemAux.addAll(auxiliaresDAO.listarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem"));

                // --- Fase 2: Encontrar a string da viagem ativa no formato do ComboBox ---
                String viagemAtivaComboStr = null;
                if (viagemAtiva != null) {
                    String prefixo = viagemAtiva.getId() + " - ";
                    for (String v : viagens) {
                        if (v.startsWith(prefixo)) {
                            viagemAtivaComboStr = v;
                            break;
                        }
                    }
                }

                // Carregar dados iniciais filtrando pela viagem ativa
                List<Passagem> dadosIniciais;
                try {
                    dadosIniciais = passagemDAO.filtrarRelatorio(
                        null, null, viagemAtivaComboStr, null, null, null, null, null, null, "Todos");
                } catch (SQLException e) {
                    dadosIniciais = new ArrayList<>();
                    e.printStackTrace();
                }

                // --- Fase 3: Atualizar UI no thread do JavaFX ---
                final List<Passagem> dadosFinais = dadosIniciais;
                final String viagemAtivaNome = viagemAtivaComboStr;

                Platform.runLater(() -> {
                    // Preencher ComboBoxes SEM listeners ativos
                    cmbStatusPagamento.setItems(FXCollections.observableArrayList("Todos", "Pagos", "Falta Pagar"));
                    cmbStatusPagamento.getSelectionModel().select("Todos");

                    cmbViagem.setItems(FXCollections.observableArrayList(viagens));
                    cmbRota.setItems(FXCollections.observableArrayList(rotasStrings));
                    cmbRota.getSelectionModel().select("Todas");
                    cmbTipoPagamento.setItems(FXCollections.observableArrayList(tiposPagamento));
                    cmbTipoPagamento.getSelectionModel().select("Todos");
                    cmbCaixa.setItems(FXCollections.observableArrayList(caixas));
                    cmbCaixa.getSelectionModel().select("Todos");
                    cmbAgente.setItems(FXCollections.observableArrayList(agentes));
                    cmbAgente.getSelectionModel().select("Todos");
                    cmbTipoPassagem.setItems(FXCollections.observableArrayList(tiposPassagemAux));
                    cmbTipoPassagem.getSelectionModel().select("Todos");

                    // Selecionar viagem ativa no ComboBox (match por ID)
                    if (viagemAtivaNome != null) {
                        cmbViagem.setValue(viagemAtivaNome);
                    } else {
                        cmbViagem.setValue("Todas");
                    }

                    // Mostrar dados iniciais
                    exibirDados(dadosFinais);

                    // AGORA ativar listeners (apos tudo preenchido)
                    inicializando = false;
                    configurarListenersDosFiltros();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    inicializando = false;
                    AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao carregar dados do relat\u00f3rio: " + e.getMessage());
                });
            }
        }).start();
    }

    private void configurarColunasTabela() {
        colBilhete.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getNumBilhete()));
        colRota.setCellValueFactory(cellData -> {
            Passagem p = cellData.getValue();
            String origem = p.getOrigem() != null ? p.getOrigem() : "";
            String destino = p.getDestino() != null ? p.getDestino() : "";
            return new SimpleStringProperty(origem + " - " + destino);
        });
        colTipoPassagem.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipoPassagemAux()));
        colAgente.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAgenteAux()));

        colDataViagem.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataViagem()));
        colDataViagem.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.format(item));
            }
        });

        formatarColunaDecimal(colValorTotal, "ValorTotal");
        formatarColunaDecimal(colValorPago, "ValorPago");
        formatarColunaDecimal(colDevedor, "Devedor");

        colFormaPagamento.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormaPagamento()));
        colCaixa.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCaixa()));

        tableRelatorio.setItems(passagensData);
    }

    private void formatarColunaDecimal(TableColumn<Passagem, BigDecimal> column, String propertyName) {
        column.setCellValueFactory(cellData -> {
            try {
                if (cellData.getValue() == null) return new SimpleObjectProperty<>(null);
                java.lang.reflect.Method m = Passagem.class.getMethod("get" + propertyName);
                return new SimpleObjectProperty<>((BigDecimal) m.invoke(cellData.getValue()));
            } catch (NoSuchMethodException e) {
                System.err.println("Metodo get" + propertyName + " nao encontrado em Passagem: " + e.getMessage());
                return new SimpleObjectProperty<>(null);
            } catch (Exception e) {
                System.err.println("RelatorioPassagensController.formatarColunaDecimal: erro ao invocar get" + propertyName + " — " + e.getMessage());
                return new SimpleObjectProperty<>(null);
            }
        });

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %,.2f", item));
            }
        });
    }

    private void configurarListenersDosFiltros() {
        cmbStatusPagamento.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        dpDataInicio.valueProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        dpDataFim.valueProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbViagem.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbRota.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbAgente.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbTipoPassagem.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbTipoPagamento.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
        cmbCaixa.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> { if (!inicializando) filtrarRelatorio(); });
    }

    private void filtrarRelatorio() {
        LocalDate dataInicio = dpDataInicio.getValue();
        LocalDate dataFim = dpDataFim.getValue();

        String viagemStr = (cmbViagem.getValue() != null && !cmbViagem.getValue().equals("Todas")) ? cmbViagem.getValue() : null;
        String rotaStr = (cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas")) ? cmbRota.getValue() : null;
        String tipoPagamento = (cmbTipoPagamento.getValue() != null && !cmbTipoPagamento.getValue().equals("Todos")) ? cmbTipoPagamento.getValue() : null;
        String caixa = (cmbCaixa.getValue() != null && !cmbCaixa.getValue().equals("Todos")) ? cmbCaixa.getValue() : null;
        String agente = (cmbAgente.getValue() != null && !cmbAgente.getValue().equals("Todos")) ? cmbAgente.getValue() : null;
        String tipoPassagem = (cmbTipoPassagem.getValue() != null && !cmbTipoPassagem.getValue().equals("Todos")) ? cmbTipoPassagem.getValue() : null;
        String statusPagamento = cmbStatusPagamento.getValue() != null ? cmbStatusPagamento.getValue() : "Todos";

        // Executar query em background
        new Thread(() -> {
            try {
                List<Passagem> resultado = passagemDAO.filtrarRelatorio(
                    dataInicio, dataFim, viagemStr, rotaStr, tipoPagamento, caixa, agente, tipoPassagem, null, statusPagamento);
                Platform.runLater(() -> exibirDados(resultado));
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao filtrar: " + e.getMessage()));
            }
        }).start();
    }

    private void exibirDados(List<Passagem> dados) {
        passagensData.setAll(dados);

        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal totalRecebido = BigDecimal.ZERO;
        BigDecimal totalAReceber = BigDecimal.ZERO;

        for (Passagem p : dados) {
            if (p.getValorTotal() != null) totalVendido = totalVendido.add(p.getValorTotal());
            if (p.getValorPago() != null) totalRecebido = totalRecebido.add(p.getValorPago());
            if (p.getDevedor() != null) totalAReceber = totalAReceber.add(p.getDevedor());
        }

        lblTotalVendido.setText(String.format("R$ %,.2f", totalVendido));
        lblTotalRecebido.setText(String.format("R$ %,.2f", totalRecebido));
        lblTotalAReceber.setText(String.format("R$ %,.2f", totalAReceber));

        atualizarGraficos(dados);
    }

    private void atualizarGraficos(List<Passagem> dados) {
        criarGraficoPizzaPorAgente(dados);
        criarGraficoBarrasPorFormaPagamento(dados);
    }

    private void criarGraficoPizzaPorAgente(List<Passagem> dados) {
        if (chartContainer1 == null) return;
        chartContainer1.getChildren().clear();

        if (dados.isEmpty()) {
            chartContainer1.getChildren().add(new Label("Sem dados para o gr\u00e1fico de agentes."));
            return;
        }

        Map<String, BigDecimal> vendasPorAgente = dados.stream()
            .filter(p -> p.getAgenteAux() != null)
            .collect(Collectors.groupingBy(
                p -> p.getAgenteAux() != null ? p.getAgenteAux() : "Sem Agente",
                Collectors.mapping(p -> p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        vendasPorAgente.forEach((agente, total) -> {
            pieChartData.add(new PieChart.Data(agente + String.format(" (R$ %,.2f)", total), total.doubleValue()));
        });

        PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Vendas por Agente");
        chart.setLegendSide(Side.LEFT);
        chart.setLabelsVisible(false);
        chart.setMaxHeight(220);

        chartContainer1.getChildren().add(chart);
    }

    private void criarGraficoBarrasPorFormaPagamento(List<Passagem> dados) {
        if (chartContainer2 == null) return;
        chartContainer2.getChildren().clear();

        if (dados.isEmpty()) {
            chartContainer2.getChildren().add(new Label("Sem dados para o gr\u00e1fico de pagamentos."));
            return;
        }

        Map<String, BigDecimal> valorPorFormaPagamento = dados.stream()
            .filter(p -> p.getFormaPagamento() != null)
            .collect(Collectors.groupingBy(
                p -> p.getFormaPagamento() != null ? p.getFormaPagamento() : "N/A",
                Collectors.mapping(p -> p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor Recebido (R$)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Valores Recebidos por Forma de Pagamento");
        barChart.setLegendVisible(false);
        barChart.setMaxHeight(220);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        valorPorFormaPagamento.forEach((forma, total) -> {
            series.getData().add(new XYChart.Data<>(forma, total));
        });

        barChart.getData().add(series);

        Platform.runLater(() -> {
            String[] colors = {"#007bff", "#28a745", "#ffc107", "#dc3545", "#17a2b8", "#6c757d"};
            int i = 0;
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + colors[i % colors.length] + ";");
                }
                i++;
            }
        });

        chartContainer2.getChildren().add(barChart);
    }

    @FXML
    private void handleImprimirRelatorio(ActionEvent event) {
        AlertHelper.show(AlertType.INFORMATION, "Funcionalidade Pendente", "A impress\u00e3o ser\u00e1 implementada futuramente.");
    }

    @FXML
    public void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        TelaPrincipalController.fecharTelaAtual(source);
    }

}
