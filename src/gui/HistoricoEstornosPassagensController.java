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
import java.time.format.DateTimeFormatter;

public class HistoricoEstornosPassagensController {

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private Button btnFechar;

    @FXML private TableView<LogEstornoPassagem> tabela;
    @FXML private TableColumn<LogEstornoPassagem, String> colData;
    @FXML private TableColumn<LogEstornoPassagem, String> colBilhete;
    @FXML private TableColumn<LogEstornoPassagem, String> colValor;
    @FXML private TableColumn<LogEstornoPassagem, String> colForma;
    @FXML private TableColumn<LogEstornoPassagem, String> colMotivo;
    @FXML private TableColumn<LogEstornoPassagem, String> colAutorizador;

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isFinanceiro()) { gui.util.PermissaoService.exigirFinanceiro("Historico Estornos Passagens"); return; }
        // Configura as colunas
        colData.setCellValueFactory(new PropertyValueFactory<>("dataHora"));
        colBilhete.setCellValueFactory(new PropertyValueFactory<>("bilhete"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colForma.setCellValueFactory(new PropertyValueFactory<>("forma"));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colAutorizador.setCellValueFactory(new PropertyValueFactory<>("autorizador"));

        // Define data padrão (últimos 30 dias)
        dpInicio.setValue(LocalDate.now().minusDays(30));
        dpFim.setValue(LocalDate.now());

        // DR010: carrega dados em background
        Thread bg = new Thread(this::carregarDados);
        bg.setDaemon(true);
        bg.start();
    }

    @FXML
    public void carregarDados() {
        ObservableList<LogEstornoPassagem> lista = FXCollections.observableArrayList();
        
        // SQL QUE BUSCA NA TABELA NOVA DE PASSAGENS
        // Faz JOIN com a tabela passagens para pegar o número do bilhete
        String sql = "SELECT l.data_hora, p.numero_bilhete, l.valor_estornado, l.forma_devolucao, l.motivo, l.nome_autorizador " +
                     "FROM log_estornos_passagens l " +
                     "JOIN passagens p ON l.id_passagem = p.id_passagem " +
                     "WHERE l.data_hora BETWEEN ? AND ? " +
                     "ORDER BY l.data_hora DESC";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            // Converte LocalDate para Timestamp/Date do SQL
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(dpInicio.getValue().atStartOfDay()));
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(dpFim.getValue().atTime(23, 59, 59)));

            // DR113: ResultSet em try-with-resources
            try (ResultSet rs = stmt.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    String dataFmt = sdf.format(rs.getTimestamp("data_hora"));
                    String bilhete = rs.getString("numero_bilhete");
                    if (bilhete == null) bilhete = "N/A";
                    lista.add(new LogEstornoPassagem(
                        dataFmt, bilhete,
                        String.format("R$ %.2f", rs.getDouble("valor_estornado")),
                        rs.getString("forma_devolucao"),
                        rs.getString("motivo"),
                        rs.getString("nome_autorizador")
                    ));
                }
            }
            javafx.application.Platform.runLater(() -> tabela.setItems(lista));

        } catch (Exception e) {
            e.printStackTrace();
            // DR113: Alert via Platform.runLater (carregarDados() pode ser chamado de bg thread)
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Erro ao buscar histórico: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    @FXML
    public void fechar() {
        Stage stage = (Stage) btnFechar.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    public void imprimir() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tabela.getScene().getWindow())) {
            boolean success = job.printPage(tabela);
            if (success) {
                job.endJob();
            }
        }
    }

    // --- CLASSE INTERNA PARA MODELAR A TABELA ---
    public static class LogEstornoPassagem {
        private String dataHora, bilhete, valor, forma, motivo, autorizador;

        public LogEstornoPassagem(String dataHora, String bilhete, String valor, String forma, String motivo, String autorizador) {
            this.dataHora = dataHora;
            this.bilhete = bilhete;
            this.valor = valor;
            this.forma = forma;
            this.motivo = motivo;
            this.autorizador = autorizador;
        }

        public String getDataHora() { return dataHora; }
        public String getBilhete() { return bilhete; }
        public String getValor() { return valor; }
        public String getForma() { return forma; }
        public String getMotivo() { return motivo; }
        public String getAutorizador() { return autorizador; }
    }
}