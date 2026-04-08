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
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    // =======================================================================
    // <<< MÉTODO INITIALIZE MODIFICADO >>>
    // A lógica foi simplificada para chamar um novo método que carrega os dados.
    // =======================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        passagemDAO = new PassagemDAO();
        viagemDAO = new ViagemDAO();
        rotaDAO = new RotaDAO();
        auxiliaresDAO = new AuxiliaresDAO();

        configurarColunasTabela();
        carregarComboBoxesFiltro();
        configurarListenersDosFiltros();
        carregarDadosIniciais(); // Novo método para carregar os dados da viagem ativa
    }

    // =======================================================================
    // <<< NOVO MÉTODO >>>
    // Este método agora é responsável por carregar os dados iniciais.
    // Ele busca a viagem ativa e chama o `carregarDadosRelatorio` diretamente.
    // =======================================================================
    private void carregarDadosIniciais() {
        Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
        String viagemAtivaStr = null;

        if (viagemAtiva != null) {
            viagemAtivaStr = viagemAtiva.toString();
            // Apenas atualiza o ComboBox para o usuário ver, mas não depende dele
            if (cmbViagem.getItems().contains(viagemAtivaStr)) {
                cmbViagem.setValue(viagemAtivaStr);
            }
        } else {
            cmbViagem.setValue("Todas");
        }

        // Chama o carregamento dos dados diretamente com a string da viagem ativa
        // (ou null se não houver), ignorando os outros filtros.
        carregarDadosRelatorio(null, null, viagemAtivaStr, null, null, null, null, null, "Todos");
    }

    private void configurarColunasTabela() {
        colBilhete.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getNumBilhete()));
        colRota.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrigem() + " - " + cellData.getValue().getDestino()));
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

    private void carregarComboBoxesFiltro() {
        try {
            cmbStatusPagamento.setItems(FXCollections.observableArrayList("Todos", "Pagos", "Falta Pagar"));
            cmbStatusPagamento.getSelectionModel().select("Todos");

            List<String> viagens = new ArrayList<>();
            viagens.add("Todas");
            viagens.addAll(viagemDAO.listarViagensParaComboBox());
            cmbViagem.setItems(FXCollections.observableArrayList(viagens));
            cmbViagem.getSelectionModel().select("Todas"); // Inicia com "Todas"

            List<String> rotasStrings = new ArrayList<>();
            rotasStrings.add("Todas");
            rotasStrings.addAll(rotaDAO.listarTodasAsRotasComoObjects().stream().map(Rota::toString).collect(Collectors.toList()));
            cmbRota.setItems(FXCollections.observableArrayList(rotasStrings));
            cmbRota.getSelectionModel().select("Todas");

            List<String> tiposPagamento = new ArrayList<>();
            tiposPagamento.add("Todos");
            tiposPagamento.addAll(auxiliaresDAO.listarTiposPagamento());
            cmbTipoPagamento.setItems(FXCollections.observableArrayList(tiposPagamento));
            cmbTipoPagamento.getSelectionModel().select("Todos");

            List<String> caixas = new ArrayList<>();
            caixas.add("Todos");
            caixas.addAll(auxiliaresDAO.listarCaixas());
            cmbCaixa.setItems(FXCollections.observableArrayList(caixas));
            cmbCaixa.getSelectionModel().select("Todos");

            List<String> agentes = new ArrayList<>();
            agentes.add("Todos");
            agentes.addAll(auxiliaresDAO.listarAgenteAux());
            cmbAgente.setItems(FXCollections.observableArrayList(agentes));
            cmbAgente.getSelectionModel().select("Todos");

            List<String> tiposPassagemAux = new ArrayList<>();
            tiposPassagemAux.add("Todos");
            tiposPassagemAux.addAll(auxiliaresDAO.listarPassagemAux());
            cmbTipoPassagem.setItems(FXCollections.observableArrayList(tiposPassagemAux));
            cmbTipoPassagem.getSelectionModel().select("Todos");

        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro de Carregamento", "Falha ao carregar filtros: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void configurarListenersDosFiltros() {
        // Estes listeners são para QUANDO O USUÁRIO MUDA O FILTRO
        cmbStatusPagamento.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        dpDataInicio.valueProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        dpDataFim.valueProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbViagem.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbRota.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbAgente.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbTipoPassagem.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbTipoPagamento.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
        cmbCaixa.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> filtrarRelatorio());
    }
    
    // Este método foi removido de 'initialize' pois sua lógica agora está em 'carregarDadosIniciais'
    // private void definirFiltroPadraoViagemAtiva() { ... }
    
    private void filtrarRelatorio() {
        // Este método agora é usado apenas pelos listeners, lendo o que o USUÁRIO selecionou
        LocalDate dataInicio = dpDataInicio.getValue();
        LocalDate dataFim = dpDataFim.getValue();
        
        String viagemStr = (cmbViagem.getValue() != null && !cmbViagem.getValue().equals("Todas")) ? cmbViagem.getValue() : null;
        String rotaStr = (cmbRota.getValue() != null && !cmbRota.getValue().equals("Todas")) ? cmbRota.getValue() : null;
        String tipoPagamento = (cmbTipoPagamento.getValue() != null && !cmbTipoPagamento.getValue().equals("Todos")) ? cmbTipoPagamento.getValue() : null;
        String caixa = (cmbCaixa.getValue() != null && !cmbCaixa.getValue().equals("Todos")) ? cmbCaixa.getValue() : null;
        String agente = (cmbAgente.getValue() != null && !cmbAgente.getValue().equals("Todos")) ? cmbAgente.getValue() : null;
        String tipoPassagem = (cmbTipoPassagem.getValue() != null && !cmbTipoPassagem.getValue().equals("Todos")) ? cmbTipoPassagem.getValue() : null;
        String statusPagamento = (cmbStatusPagamento.getValue() != null && !cmbStatusPagamento.getValue().equals("Todos")) ? cmbStatusPagamento.getValue() : "Todos";
        
        carregarDadosRelatorio(dataInicio, dataFim, viagemStr, rotaStr, tipoPagamento, caixa, agente, tipoPassagem, statusPagamento);
    }
    
    private void carregarDadosRelatorio(LocalDate dataInicio, LocalDate dataFim, String viagemStr, String rotaStr, 
                                        String tipoPagamento, String caixa, String agente, String tipoPassagem, String statusPagamento) {
        try {
            List<Passagem> passagensFiltradas = passagemDAO.filtrarRelatorio(dataInicio, dataFim, viagemStr, rotaStr,
                                                                             tipoPagamento, caixa, agente, tipoPassagem,
                                                                             null, statusPagamento);
            
            passagensData.setAll(passagensFiltradas);
            
            BigDecimal totalVendido = BigDecimal.ZERO;
            BigDecimal totalRecebido = BigDecimal.ZERO;
            BigDecimal totalAReceber = BigDecimal.ZERO;

            for (Passagem p : passagensFiltradas) {
                if(p.getValorTotal() != null) totalVendido = totalVendido.add(p.getValorTotal());
                if(p.getValorPago() != null) totalRecebido = totalRecebido.add(p.getValorPago());
                if(p.getDevedor() != null) totalAReceber = totalAReceber.add(p.getDevedor());
            }

            lblTotalVendido.setText(String.format("R$ %,.2f", totalVendido));
            lblTotalRecebido.setText(String.format("R$ %,.2f", totalRecebido));
            lblTotalAReceber.setText(String.format("R$ %,.2f", totalAReceber));

            atualizarGraficos(passagensFiltradas);

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Relatório", "Falha ao carregar dados do relatório: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void atualizarGraficos(List<Passagem> dados) {
        criarGraficoPizzaPorAgente(dados);
        criarGraficoBarrasPorFormaPagamento(dados);
    }

    private void criarGraficoPizzaPorAgente(List<Passagem> dados) {
        if (chartContainer1 == null) return;
        chartContainer1.getChildren().clear();

        if (dados.isEmpty()) {
            chartContainer1.getChildren().add(new Label("Sem dados para o gráfico de agentes."));
            return;
        }

        Map<String, BigDecimal> vendasPorAgente = dados.stream()
            .filter(p -> p.getAgenteAux() != null)
            .collect(Collectors.groupingBy(
                Passagem::getAgenteAux,
                Collectors.mapping(Passagem::getValorTotal, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        vendasPorAgente.forEach((agente, total) -> {
            pieChartData.add(new PieChart.Data(agente + String.format(" (R$ %,.2f)", total), total.doubleValue()));
        });

        PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Vendas por Agente");
        chart.setLegendSide(Side.LEFT);
        chart.setLabelsVisible(false);

        chartContainer1.getChildren().add(chart);
    }

    private void criarGraficoBarrasPorFormaPagamento(List<Passagem> dados) {
        if (chartContainer2 == null) return;
        chartContainer2.getChildren().clear();

        if (dados.isEmpty()) {
            chartContainer2.getChildren().add(new Label("Sem dados para o gráfico de pagamentos."));
            return;
        }
        
        Map<String, BigDecimal> valorPorFormaPagamento = dados.stream()
            .filter(p -> p.getFormaPagamento() != null)
            .collect(Collectors.groupingBy(
                Passagem::getFormaPagamento,
                Collectors.mapping(Passagem::getValorPago, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor Recebido (R$)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Valores Recebidos por Forma de Pagamento");
        barChart.setLegendVisible(false);

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
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "A impressão será implementada futuramente.");
    }

    @FXML
    public void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        TelaPrincipalController.fecharTelaAtual(source);
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}