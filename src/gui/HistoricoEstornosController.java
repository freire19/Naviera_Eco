package gui;

import dao.ConexaoBD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import gui.util.AppLogger;

public class HistoricoEstornosController {

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private TableView<EstornoLog> tabela;
    @FXML private TableColumn<EstornoLog, String> colData;
    @FXML private TableColumn<EstornoLog, String> colEnc;
    @FXML private TableColumn<EstornoLog, String> colValor;
    @FXML private TableColumn<EstornoLog, String> colForma;
    @FXML private TableColumn<EstornoLog, String> colMotivo;
    @FXML private TableColumn<EstornoLog, String> colAutorizador;

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Historico de Estornos"); return; }
        dpInicio.setValue(LocalDate.now().minusDays(30));
        dpFim.setValue(LocalDate.now());

        configurarTabela();
        // DR010: carrega dados em background
        Thread bg = new Thread(this::filtrar);
        bg.setDaemon(true);
        bg.start();
    }

    private void configurarTabela() {
        colData.setCellValueFactory(new PropertyValueFactory<>("dataHora"));
        colEnc.setCellValueFactory(new PropertyValueFactory<>("numeroEncomenda"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colForma.setCellValueFactory(new PropertyValueFactory<>("formaDevolucao"));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colAutorizador.setCellValueFactory(new PropertyValueFactory<>("autorizador"));
        
        // Estilo cabeçalho azul (caso tenha CSS)
        tabela.setStyle("-fx-font-size: 13px;");
    }

    @FXML
    public void filtrar() {
        if(dpInicio.getValue() == null || dpFim.getValue() == null) return;

        ObservableList<EstornoLog> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        String sql = "SELECT l.data_hora, e.numero_encomenda, l.valor_estornado, l.motivo, l.nome_autorizador, l.forma_devolucao " +
                     "FROM log_estornos_encomendas l " +
                     "JOIN encomendas e ON l.id_encomenda = e.id_encomenda " +
                     "WHERE l.data_hora::date BETWEEN ? AND ? " +
                     "AND e.empresa_id = ? " +
                     "ORDER BY l.data_hora DESC";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(dpInicio.getValue()));
            stmt.setDate(2, java.sql.Date.valueOf(dpFim.getValue()));
            stmt.setInt(3, dao.DAOUtils.empresaId());
            // DR113: ResultSet em try-with-resources
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    lista.add(new EstornoLog(
                        sdf.format(rs.getTimestamp("data_hora")),
                        rs.getString("numero_encomenda"),
                        rs.getDouble("valor_estornado"),
                        rs.getString("forma_devolucao"),
                        rs.getString("motivo"),
                        rs.getString("nome_autorizador")
                    ));
                }
            }
            javafx.application.Platform.runLater(() -> tabela.setItems(lista));

        } catch (Exception e) {
            AppLogger.error("HistoricoEstornosController", e.getMessage(), e);
            // DR113: Alert via Platform.runLater (filtrar() pode ser chamado de bg thread)
            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Erro ao buscar histórico: " + e.getMessage()).show());
        }
    }

    @FXML
    public void imprimir() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tabela.getScene().getWindow())) {
            boolean success = job.printPage(tabela);
            if (success) job.endJob();
        }
    }

    @FXML
    public void fechar() {
        ((Stage) tabela.getScene().getWindow()).close();
    }

    public static class EstornoLog {
        private String dataHora, numeroEncomenda, formaDevolucao, motivo, autorizador;
        private Double valor;

        public EstornoLog(String dt, String num, Double val, String forma, String mot, String auto) {
            this.dataHora = dt; this.numeroEncomenda = num; this.valor = val;
            this.formaDevolucao = forma; this.motivo = mot; this.autorizador = auto;
        }
        
        public String getDataHora() { return dataHora; }
        public String getNumeroEncomenda() { return numeroEncomenda; }
        public String getValorFormatado() { return String.format("R$ %.2f", valor); }
        public String getFormaDevolucao() { return formaDevolucao; }
        public String getMotivo() { return motivo; }
        public String getAutorizador() { return autorizador; }
    }
}