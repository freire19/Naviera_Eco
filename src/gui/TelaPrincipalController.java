package gui;

import gui.util.LogService;
import gui.util.PermissaoService;
import gui.util.RelatorioUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import dao.AgendaDAO;
import dao.ConexaoBD;
import dao.ViagemDAO;
import model.Viagem;
import gui.BalancoViagemController;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.Collections;
import java.util.Map;
import gui.util.AlertHelper;
import util.AppLogger;
import gui.util.VersaoChecker;

public class TelaPrincipalController implements Initializable {

    // DP046: static final NumberFormat avoids per-call instantiation
    private static final NumberFormat NF_MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @FXML private BorderPane rootPane;
    @FXML private ComboBox<String> cmbViagemAtiva;
    @FXML private Button btnCarregarDadosDaViagem;
    @FXML private GridPane dashboardGrid;
    
    // Menu Superior
    @FXML private HBox hboxMenuSuperior;

    // Botão de Modo Escuro
    @FXML private ToggleButton btnModoNoturno;

    // Badge de atualizacao disponivel (estilo VS Code)
    @FXML private Button btnAtualizar;
    private VersaoChecker.VersaoInfo versaoInfoPendente;
    
    // Calendário
    @FXML private Label lblMesAnoCalendario;
    @FXML private GridPane calendarioGrid;
    
    // Legenda 
    @FXML private HBox hboxLegenda;
    
    // Sistema de Abas
    @FXML private TabPane tabPanePrincipal;
    @FXML private Tab tabInicio;

    private Text txtTotalVolumesFrete;
    private Text txtQtdEncomendas;
    private Text txtTotalPassageiros;

    private final ViagemDAO viagemDAO = new ViagemDAO();
    private final AgendaDAO agendaDAO = new AgendaDAO();
    
    private YearMonth mesAtualCalendario;
    
    // Controle de Tema
    private boolean isModoEscuro = false;
    private String cssClaro = "/css/main.css";
    private String cssEscuro = "/css/dark.css";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDashboardCards();
        mesAtualCalendario = YearMonth.now();
        atualizarEstiloMenuSuperior();
        configurarLegendaDinamica();

        if (btnModoNoturno != null) {
            btnModoNoturno.setText("🌙 Modo Escuro");
        }

        // Carrega dados do banco em background para nao bloquear a FX thread (DR010)
        // Carrega viagens, dashboard e calendario em uma unica passagem (performance)
        javafx.concurrent.Task<Void> taskInit = new javafx.concurrent.Task<Void>() {
            private java.util.List<String> listaViagens;
            private model.Viagem viagemAtiva;
            private int[] counts;
            private List<Viagem> viagensDoMes;
            private List<AgendaDAO.ResumoBoleto> boletosDoMes;
            private Map<LocalDate, List<String>> notasDoMes;

            @Override
            protected Void call() throws Exception {
                listaViagens = viagemDAO.listarViagensParaComboBox();
                viagemAtiva = viagemDAO.buscarViagemAtiva();

                // Dados do dashboard
                long idViagem = viagemAtiva != null ? viagemAtiva.getId() : -1;
                counts = carregarCountsDashboard(idViagem);

                // Dados do calendario (batch)
                viagensDoMes = viagemDAO.listarViagensPorMesAno(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                boletosDoMes = agendaDAO.buscarBoletosPendentesNoMes(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                notasDoMes = agendaDAO.buscarAnotacoesDoMes(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                return null;
            }

            @Override
            protected void succeeded() {
                // Volta para FX thread para atualizar UI
                cmbViagemAtiva.setItems(FXCollections.observableArrayList(listaViagens));
                if (viagemAtiva != null) cmbViagemAtiva.setValue(viagemAtiva.toString());
                else if (!cmbViagemAtiva.getItems().isEmpty()) cmbViagemAtiva.getSelectionModel().selectFirst();

                // Atualizar dashboard com dados ja carregados
                txtTotalVolumesFrete.setText(String.valueOf(counts[0]));
                txtQtdEncomendas.setText(String.valueOf(counts[1]));
                txtTotalPassageiros.setText(String.valueOf(counts[2]));

                // Construir calendario com dados ja carregados
                construirCalendarioComDados(viagensDoMes, boletosDoMes, notasDoMes);
            }

            @Override
            protected void failed() {
                AppLogger.warn("TelaPrincipalController", "Erro ao carregar dados iniciais: " + getException().getMessage());
            }
        };
        Thread t = new Thread(taskInit);
        t.setDaemon(true);
        t.start();

        // Check de versao em background (nao bloqueia startup)
        // Mostra badge "Atualizar" na TopBar quando ha nova versao (estilo VS Code)
        VersaoChecker.verificarAtualizacao(info -> {
            versaoInfoPendente = info;
            if (btnAtualizar != null) {
                btnAtualizar.setVisible(true);
                btnAtualizar.setManaged(true);
                if (info.obrigatoria) {
                    btnAtualizar.setText("Atualizacao Obrigatoria");
                    btnAtualizar.setStyle("-fx-background-color: #cc0000; -fx-text-fill: white; -fx-font-weight: bold;");
                    // Update obrigatorio: abre popup automaticamente
                    handleAbrirAtualizacao(null);
                } else {
                    btnAtualizar.setText("Atualizar \u2022 v" + info.versaoNova);
                    btnAtualizar.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
                }
            }
        });
    }

    @FXML
    private void handleAbrirAtualizacao(ActionEvent event) {
        if (versaoInfoPendente == null) return;
        javafx.stage.Window owner = (rootPane != null && rootPane.getScene() != null)
            ? rootPane.getScene().getWindow() : null;
        VersaoChecker.mostrarPopupAtualizacao(versaoInfoPendente, owner);
    }

    // ================================================================================
    // CONFIGURAÇÃO AUTOMÁTICA DA LEGENDA
    // ================================================================================
    private void configurarLegendaDinamica() {
        HBox containerLegenda = null;

        if (hboxLegenda != null) {
            containerLegenda = hboxLegenda;
        } 
        else if (rootPane.getBottom() instanceof HBox) {
            containerLegenda = (HBox) rootPane.getBottom();
        }

        if (containerLegenda != null) {
            boolean jaExiste = containerLegenda.getChildren().stream()
                .filter(n -> n instanceof Label)
                .map(n -> ((Label)n).getText())
                .anyMatch(t -> t.contains("Contas a Pagar"));

            if (!jaExiste) {
                Label lblBoleto = new Label("📄 Contas a Pagar");
                lblBoleto.setStyle("-fx-font-size: 12px; -fx-padding: 0 0 0 15;");
                
                if (isModoEscuro) lblBoleto.setTextFill(Color.WHITE);
                else lblBoleto.setTextFill(Color.BLACK);
                
                lblBoleto.getProperties().put("tipo", "legendaBoleto");

                containerLegenda.getChildren().add(lblBoleto);
            }
        }
    }

    // ================================================================================
    // LÓGICA DO MODO ESCURO / CLARO
    // ================================================================================
    @FXML
    private void handleAlternarTema(ActionEvent event) {
        isModoEscuro = btnModoNoturno.isSelected();
        TemaManager.setModoEscuro(isModoEscuro); // Sincronizar com TemaManager
        
        if (isModoEscuro) {
            btnModoNoturno.setText("☀️ Modo Claro");
        } else {
            btnModoNoturno.setText("🌙 Modo Escuro");
        }
        
        aplicarTema(rootPane.getScene());
        atualizarEstiloMenuSuperior(); 
        construirCalendario(); 
        atualizarCorLegendaBoleto();
    }
    
    private void atualizarCorLegendaBoleto() {
        HBox container = (hboxLegenda != null) ? hboxLegenda : (rootPane.getBottom() instanceof HBox ? (HBox) rootPane.getBottom() : null);
        if (container != null) {
            for (Node n : container.getChildren()) {
                if (n instanceof Label && "legendaBoleto".equals(n.getProperties().get("tipo"))) {
                    ((Label)n).setTextFill(isModoEscuro ? Color.WHITE : Color.BLACK);
                }
            }
        }
    }

    private void aplicarTema(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();

        String cssParaCarregar = isModoEscuro ? cssEscuro : cssClaro;
        URL url = getClass().getResource(cssParaCarregar);

        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        } else {
            AppLogger.warn("TelaPrincipalController", "Erro crítico: CSS não encontrado em " + cssParaCarregar);
        }
    }

    public void aplicarTemaEmNovaJanela(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            aplicarTema(stage.getScene());
        }
    }
    
    private void atualizarEstiloMenuSuperior() {
        if (hboxMenuSuperior == null) return;

        if (isModoEscuro) {
            hboxMenuSuperior.setStyle("-fx-background-color: #333333; -fx-padding: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 2);");
            for (Node node : hboxMenuSuperior.getChildren()) {
                if (node instanceof MenuButton || node instanceof Button) {
                    node.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-cursor: hand; -fx-text-fill: white;");
                }
            }
        } else {
            hboxMenuSuperior.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            for (Node node : hboxMenuSuperior.getChildren()) {
                if (node instanceof MenuButton || node instanceof Button) {
                    node.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-cursor: hand; -fx-text-fill: black;");
                }
            }
        }
    }

    // ================================================================================
    // LÓGICA DO CALENDÁRIO (COM BOLETOS E LEGENDA)
    // ================================================================================
    private void construirCalendario() {
        // Carrega dados em background e depois constrói na UI thread
        // DR114: daemon + try-catch
        Thread bgCal = new Thread(() -> {
            try {
                List<Viagem> viagensDoMes = viagemDAO.listarViagensPorMesAno(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                List<AgendaDAO.ResumoBoleto> boletosDoMes = agendaDAO.buscarBoletosPendentesNoMes(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                Map<LocalDate, List<String>> notasDoMes = agendaDAO.buscarAnotacoesDoMes(mesAtualCalendario.getMonthValue(), mesAtualCalendario.getYear());
                Platform.runLater(() -> construirCalendarioComDados(viagensDoMes, boletosDoMes, notasDoMes));
            } catch (Exception e) {
                AppLogger.warn("TelaPrincipalController", "Erro ao carregar calendario: " + e.getMessage());
            }
        });
        bgCal.setDaemon(true);
        bgCal.start();
    }

    /** Constrói o calendário na UI thread usando dados já carregados */
    private void construirCalendarioComDados(List<Viagem> viagensDoMes, List<AgendaDAO.ResumoBoleto> boletosDoMes, Map<LocalDate, List<String>> notasDoMes) {
        calendarioGrid.getChildren().clear();

        String corBordaNormal = isModoEscuro ? "#333333" : "#cccccc";

        String corBordaDestaque = isModoEscuro ? "#34D399" : "#0d9668";
        String corFundoHover = isModoEscuro ? "#0A1F18" : "#E6F5ED";
        String corFundoPadrao = isModoEscuro ? "#333333" : "white";
        String corFeriado = isModoEscuro ? "#DC2626" : "#fff9c4";
        String corHojeFundo = isModoEscuro ? "#34D399" : "#E6F5ED";

        calendarioGrid.setStyle("-fx-border-color: " + corBordaNormal + "; -fx-border-width: 1px;");

        String nomeMes = mesAtualCalendario.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        String titulo = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1) + " " + mesAtualCalendario.getYear();
        lblMesAnoCalendario.setText(titulo);

        String[] diasSemana = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        for (int i = 0; i < diasSemana.length; i++) {
            Label lblDia = new Label(diasSemana[i]);
            lblDia.setStyle("-fx-font-weight: bold; -fx-alignment: center;");
            lblDia.setMaxWidth(Double.MAX_VALUE);
            calendarioGrid.add(lblDia, i, 0);
        }

        LocalDate dataInicio = mesAtualCalendario.atDay(1);
        int diaDaSemanaInicio = dataInicio.getDayOfWeek().getValue();
        if (diaDaSemanaInicio == 7) diaDaSemanaInicio = 0;

        int diasNoMes = mesAtualCalendario.lengthOfMonth();

        // DP046: use static final NF_MOEDA
        NumberFormat nfCalendario = NF_MOEDA;

        int row = 1;
        int col = diaDaSemanaInicio;

        for (int dia = 1; dia <= diasNoMes; dia++) {
            LocalDate dataAtual = mesAtualCalendario.atDay(dia);
            VBox cell = new VBox(2);
            cell.getStyleClass().add("vbox-calendario"); 
            cell.setAlignment(Pos.TOP_LEFT);
            
            String styleBase = "-fx-background-color: " + corFundoPadrao + "; -fx-border-color: " + corBordaNormal + "; -fx-padding: 2; -fx-cursor: hand;";
            
            String feriado = getFeriado(dataAtual);
            if (feriado != null) {
                styleBase = "-fx-background-color: " + corFeriado + "; -fx-border-color: " + corBordaNormal + "; -fx-padding: 2; -fx-cursor: hand;";
            }
            if (dataAtual.equals(LocalDate.now())) {
                styleBase = "-fx-border-color: " + corBordaDestaque + "; -fx-border-width: 2; -fx-background-color: " + corHojeFundo + "; -fx-padding: 1; -fx-cursor: hand;";
            }
            
            cell.setStyle(styleBase);
            final String finalStyle = styleBase;
            
            cell.setOnMouseEntered(e -> cell.setStyle("-fx-border-color: " + corBordaDestaque + "; -fx-background-color: " + corFundoHover + "; -fx-padding: 2; -fx-cursor: hand;"));
            cell.setOnMouseExited(e -> cell.setStyle(finalStyle));
            
            Label lblNumeroDia = new Label(String.valueOf(dia));
            lblNumeroDia.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0 0 0 2;");
            
            if (dataAtual.equals(LocalDate.now())) lblNumeroDia.setTextFill(Color.WHITE);
            else if (feriado != null && isModoEscuro) lblNumeroDia.setTextFill(Color.WHITE);
            else if (feriado != null) lblNumeroDia.setTextFill(Color.web("#F59E0B"));
            
            cell.getChildren().add(lblNumeroDia);
            
            if (feriado != null) {
                Label lblFer = new Label("★ " + feriado);
                String corTextoFeriado = isModoEscuro ? "#FEE2E2" : "#B45309";
                lblFer.setStyle("-fx-text-fill: " + corTextoFeriado + "; -fx-font-size: 9px; -fx-padding: 0 0 0 2;");
                cell.getChildren().add(lblFer);
            }
            
            // DM014: secoes extraidas em metodos auxiliares
            adicionarViagensNaCelula(cell, viagensDoMes, dataAtual);
            adicionarBoletosNaCelula(cell, boletosDoMes, dataAtual, nfCalendario);
            List<String> notas = notasDoMes.getOrDefault(dataAtual, java.util.Collections.emptyList());
            adicionarNotasNaCelula(cell, notas);
            
            cell.setOnMouseClicked(e -> gerenciarAgendaDoDia(dataAtual, viagensDoMes, notas, feriado, boletosDoMes));
            calendarioGrid.add(cell, col, row);
            col++;
            if (col > 6) { col = 0; row++; }
        }
    }
    
    // DM014: metodos auxiliares extraidos de construirCalendario()
    private void adicionarViagensNaCelula(VBox cell, List<Viagem> viagens, LocalDate data) {
        for (Viagem v : viagens) {
            if (v.getDataViagem().equals(data)) {
                String destino = v.getDestino() != null ? v.getDestino() : "Viagem";
                Label lbl = new Label("\uD83D\uDEA2 " + destino);
                String bg = isModoEscuro ? "#0F2D24" : "#FEE2E2";
                String tx = isModoEscuro ? "#ffffff" : "#DC2626";
                lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + tx + "; -fx-font-size: 9px; -fx-padding: 1 3 1 3; -fx-background-radius: 3;");
                lbl.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(lbl);
            }
        }
    }

    private void adicionarBoletosNaCelula(VBox cell, List<AgendaDAO.ResumoBoleto> boletos, LocalDate data, NumberFormat nf) {
        for (AgendaDAO.ResumoBoleto b : boletos) {
            if (b.vencimento.equals(data)) {
                Label lbl = new Label("\uD83D\uDCC4 " + nf.format(b.valor));
                String bg = isModoEscuro ? "#DC2626" : "#FEE2E2";
                String tx = isModoEscuro ? "#ffffff" : "#DC2626";
                lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + tx + "; -fx-font-size: 9px; -fx-padding: 1 3 1 3; -fx-background-radius: 3; -fx-border-color: #ef5350; -fx-border-width: 0 0 0 2;");
                lbl.setMaxWidth(Double.MAX_VALUE);
                Tooltip.install(lbl, new Tooltip("Vencimento: " + b.descricao + "\nValor: " + nf.format(b.valor)));
                cell.getChildren().add(lbl);
            }
        }
    }

    private void adicionarNotasNaCelula(VBox cell, List<String> notas) {
        for (String nota : notas) {
            Label lbl = new Label("\u270E " + nota);
            String bg = isModoEscuro ? "#0F2D24" : "#A7F3D0";
            String tx = isModoEscuro ? "#D1FAE5" : "#047857";
            String notaLower = nota.toLowerCase();
            if (notaLower.contains("manaus") && notaLower.contains("juta")) {
                if (notaLower.indexOf("manaus") < notaLower.indexOf("juta")) {
                    bg = isModoEscuro ? "#047857" : "#A7F3D0"; tx = isModoEscuro ? "#D1FAE5" : "#047857";
                } else {
                    bg = isModoEscuro ? "#DC2626" : "#FEE2E2"; tx = isModoEscuro ? "#FEE2E2" : "#DC2626";
                }
            }
            lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + tx + "; -fx-font-size: 9px; -fx-padding: 1 3 1 3; -fx-background-radius: 3;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(lbl);
        }
    }

    private String getFeriado(LocalDate date) {
        int d = date.getDayOfMonth();
        Month m = date.getMonth();
        if (d == 1 && m == Month.JANUARY) return "Ano Novo";
        if (d == 21 && m == Month.APRIL) return "Tiradentes";
        if (d == 1 && m == Month.MAY) return "Trabalho";
        if (d == 7 && m == Month.SEPTEMBER) return "Independência";
        if (d == 12 && m == Month.OCTOBER) return "N. Sra. Aparecida";
        if (d == 2 && m == Month.NOVEMBER) return "Finados";
        if (d == 15 && m == Month.NOVEMBER) return "Proclamação Rep.";
        if (d == 25 && m == Month.DECEMBER) return "Natal";
        if (d == 5 && m == Month.SEPTEMBER) return "Elev. Amazonas";
        if (d == 20 && m == Month.NOVEMBER) return "Consciência Negra";
        return null;
    }
    
    private void gerenciarAgendaDoDia(LocalDate data, List<Viagem> viagens, List<String> notas, String feriado, List<AgendaDAO.ResumoBoleto> boletos) {
        StringBuilder resumo = new StringBuilder("Agenda de " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ":\n\n");
        if (feriado != null) resumo.append("★ FERIADO: ").append(feriado).append("\n\n");
        
        // Exibe Viagens
        boolean temViagem = false;
        for (Viagem v : viagens) {
            if (v.getDataViagem().equals(data)) {
                resumo.append("🚢 VIAGEM: ").append(v.getNomeRotaConcatenado()).append("\n");
                resumo.append("   Barco: ").append(v.getNomeEmbarcacao()).append("\n");
                resumo.append("   Saída: ").append(v.getDescricaoHorarioSaida()).append("\n\n");
                temViagem = true;
            }
        }
        
        // Exibe Boletos
        boolean temBoleto = false;
        NumberFormat nf = NF_MOEDA;
        for (AgendaDAO.ResumoBoleto b : boletos) {
            if (b.vencimento.equals(data)) {
                if (!temBoleto) resumo.append("--- CONTAS A PAGAR ---\n");
                resumo.append("📄 ").append(b.descricao).append(" - ").append(nf.format(b.valor)).append("\n");
                temBoleto = true;
            }
        }
        if (temBoleto) resumo.append("\n");
        
        // Exibe Notas
        if (!notas.isEmpty()) resumo.append("--- ANOTAÇÕES ---\n");
        for (String nota : notas) resumo.append("✎ ").append(nota).append("\n");
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Detalhes do Dia");
        dialog.setHeaderText(resumo.toString());
        dialog.setContentText("Nova anotação rápida:");
        if (isModoEscuro) {
            java.net.URL cssUrl = getClass().getResource(cssEscuro);
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        }
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            // DP035: INSERT em background thread
            final String nota = result.get().trim();
            Thread bg = new Thread(() -> {
                agendaDAO.adicionarAnotacao(data, nota);
                Platform.runLater(() -> construirCalendario());
            });
            bg.setDaemon(true);
            bg.start();
        }
    }

    @FXML private void mesAnterior(ActionEvent event) { mesAtualCalendario = mesAtualCalendario.minusMonths(1); construirCalendario(); }
    @FXML private void mesProximo(ActionEvent event) { mesAtualCalendario = mesAtualCalendario.plusMonths(1); construirCalendario(); }
    
    @FXML 
    private void handleGerenciarAgenda(ActionEvent event) { 
        abrirTelaModal("/gui/TelaGerenciarAgenda.fxml", "Gerenciar Tarefas e Agenda", false); 
    }

    @FXML
    private void handleGerarEscala(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agendar Saídas Recorrentes");
        dialog.setHeaderText("Marcar dias de saída na Agenda (Lembretes):");

        if (isModoEscuro) {
            java.net.URL cssUrl = getClass().getResource(cssEscuro);
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
        }

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> cmbBarcos = new ComboBox<>();
        ComboBox<String> cmbRotas = new ComboBox<>();
        DatePicker dtInicio = new DatePicker(LocalDate.now());
        DatePicker dtFim = new DatePicker(LocalDate.now().plusMonths(6)); 
        ComboBox<String> cmbFrequencia = new ComboBox<>();
        cmbFrequencia.getItems().addAll("Semanal (7 dias)", "Quinzenal (14 dias)", "Mensal (30 dias)");
        cmbFrequencia.getSelectionModel().select(1); 

        // CARREGA BARCOS
        carregarDadosComboParam(cmbBarcos, "SELECT nome FROM embarcacoes WHERE empresa_id = ? ORDER BY nome");

        // CORREÇÃO AQUI: Usa 'origem' e 'destino' em vez de 'nome'
        carregarDadosComboParam(cmbRotas, "SELECT origem || ' / ' || destino FROM rotas WHERE empresa_id = ? ORDER BY origem");

        grid.add(new Label("Embarcação:"), 0, 0);
        grid.add(cmbBarcos, 1, 0);
        grid.add(new Label("Rota:"), 0, 1);
        grid.add(cmbRotas, 1, 1);
        grid.add(new Label("Data Início:"), 0, 2);
        grid.add(dtInicio, 1, 2);
        grid.add(new Label("Marcar até:"), 0, 3);
        grid.add(dtFim, 1, 3);
        grid.add(new Label("Frequência:"), 0, 4);
        grid.add(cmbFrequencia, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String barcoNome = cmbBarcos.getValue();
            String rotaNome = cmbRotas.getValue();
            LocalDate inicio = dtInicio.getValue();
            LocalDate fim = dtFim.getValue();
            String freq = cmbFrequencia.getValue();

            if (barcoNome == null || rotaNome == null || inicio == null || fim == null) {
                AlertHelper.show(AlertType.WARNING, "Dados Incompletos", "Preencha todos os campos.");
                return;
            }

            int diasIntervalo = 14;
            if (freq.contains("7")) diasIntervalo = 7;
            else if (freq.contains("30")) diasIntervalo = 30;

            gerarLembretesNoBanco(barcoNome, rotaNome, inicio, fim, diasIntervalo);
            construirCalendario();
        }
    }

    // DP035: carrega dados do combo em background thread para nao bloquear FX thread
    private void carregarDadosComboParam(ComboBox<String> combo, String sql) {
        Thread bg = new Thread(() -> {
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, dao.DAOUtils.empresaId());
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<String> itens = FXCollections.observableArrayList();
                    while (rs.next()) {
                        itens.add(rs.getString(1));
                    }
                    Platform.runLater(() -> {
                        combo.setItems(itens);
                        if(!itens.isEmpty()) combo.getSelectionModel().selectFirst();
                    });
                }
            } catch (SQLException e) {
                AppLogger.error("TelaPrincipalController", e.getMessage(), e);
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // DP035: carrega dados do combo simples em background thread
    private void carregarDadosComboSimples(ComboBox<String> combo, String sql) {
        Thread bg = new Thread(() -> {
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                ObservableList<String> itens = FXCollections.observableArrayList();
                while (rs.next()) {
                    itens.add(rs.getString(1));
                }
                Platform.runLater(() -> {
                    combo.setItems(itens);
                    if(!itens.isEmpty()) combo.getSelectionModel().selectFirst();
                });
            } catch (SQLException e) {
                AppLogger.error("TelaPrincipalController", e.getMessage(), e);
                Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro ao carregar lista",
                    "Não foi possível carregar os dados. Verifique o banco.\nErro: " + e.getMessage()));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // DP035: gera lembretes em background thread para nao bloquear FX thread
    private void gerarLembretesNoBanco(String nomeBarco, String nomeRota, LocalDate inicio, LocalDate fim, int intervalo) {
        Thread bg = new Thread(() -> {
            String sqlInsert = "INSERT INTO agenda_anotacoes (data_evento, descricao, concluida, empresa_id) VALUES (?, ?, false, ?)";
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert)) {

                LocalDate dataAtual = inicio;
                int contador = 0;
                String textoLembrete = "SAÍDA: " + nomeRota;

                while (!dataAtual.isAfter(fim)) {
                    stmtInsert.setDate(1, Date.valueOf(dataAtual));
                    stmtInsert.setString(2, textoLembrete);
                    stmtInsert.setInt(3, dao.DAOUtils.empresaId());
                    stmtInsert.executeUpdate();
                    dataAtual = dataAtual.plusDays(intervalo);
                    contador++;
                }
                final int total = contador;
                Platform.runLater(() -> AlertHelper.show(AlertType.INFORMATION, "Sucesso", total + " lembretes de saída agendados!"));
            } catch (SQLException e) {
                AppLogger.error("TelaPrincipalController", e.getMessage(), e);
                Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro SQL", e.getMessage()));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // --- Outros Métodos ---
    private void setupDashboardCards() {
        txtTotalVolumesFrete = new Text("0"); txtTotalVolumesFrete.getStyleClass().add("dashboard-card-value");
        VBox cardFrete = createDashboardCard("Fretes (Volumes)", txtTotalVolumesFrete); dashboardGrid.add(cardFrete, 0, 0);

        txtQtdEncomendas = new Text("0"); txtQtdEncomendas.getStyleClass().add("dashboard-card-value");
        VBox cardEncomendas = createDashboardCard("Encomendas", txtQtdEncomendas); dashboardGrid.add(cardEncomendas, 1, 0);

        txtTotalPassageiros = new Text("0"); txtTotalPassageiros.getStyleClass().add("dashboard-card-value");
        VBox cardPassageiros = createDashboardCard("Passageiros", txtTotalPassageiros); dashboardGrid.add(cardPassageiros, 2, 0);
    }

    private VBox createDashboardCard(String title, Node valueNode) {
        Label titleLabel = new Label(title); titleLabel.getStyleClass().add("dashboard-card-title");
        VBox card = new VBox(5, titleLabel, valueNode);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private void atualizarDashboard() {
        // DR114: daemon + try-catch
        Thread bgDash = new Thread(() -> {
            try {
                Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
                long idViagem = viagemAtiva != null ? viagemAtiva.getId() : -1;
                int[] counts = carregarCountsDashboard(idViagem);
                Platform.runLater(() -> {
                    txtTotalVolumesFrete.setText(String.valueOf(counts[0]));
                    txtQtdEncomendas.setText(String.valueOf(counts[1]));
                    txtTotalPassageiros.setText(String.valueOf(counts[2]));
                });
            } catch (Exception e) {
                AppLogger.warn("TelaPrincipalController", "Erro ao atualizar dashboard: " + e.getMessage());
            }
        });
        bgDash.setDaemon(true);
        bgDash.start();
    }

    /** Executa as 3 queries do dashboard numa única conexão. Pode ser chamado de qualquer thread. */
    private int[] carregarCountsDashboard(long idViagem) {
        int[] counts = {0, 0, 0};
        if (idViagem < 0) return counts;
        String[] sqls = {
            "SELECT COALESCE(SUM(fi.quantidade), 0) FROM frete_itens fi JOIN fretes f ON fi.id_frete = f.id_frete WHERE f.id_viagem = ? AND f.empresa_id = ? AND (f.excluido = FALSE OR f.excluido IS NULL)",
            "SELECT COUNT(*) FROM encomendas WHERE id_viagem = ? AND empresa_id = ? AND (excluido = FALSE OR excluido IS NULL)",
            "SELECT COUNT(*) FROM passagens WHERE id_viagem = ? AND empresa_id = ? AND (excluido = FALSE OR excluido IS NULL)"
        };
        try (Connection conn = ConexaoBD.getConnection()) {
            for (int i = 0; i < sqls.length; i++) {
                try (PreparedStatement stmt = conn.prepareStatement(sqls[i])) {
                    stmt.setLong(1, idViagem);
                    stmt.setInt(2, dao.DAOUtils.empresaId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) counts[i] = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) { AppLogger.error("TelaPrincipalController", e.getMessage(), e); }
        return counts;
    }

    private void carregarViagensNoCombo() {
        // DR114: daemon
        Thread bgViagens = new Thread(() -> {
            try {
                List<String> listaViagens = viagemDAO.listarViagensParaComboBox();
                Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
                Platform.runLater(() -> {
                    cmbViagemAtiva.setItems(FXCollections.observableArrayList(listaViagens));
                    if (viagemAtiva != null) cmbViagemAtiva.setValue(viagemAtiva.toString());
                    else if (!cmbViagemAtiva.getItems().isEmpty()) cmbViagemAtiva.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro", e.getMessage()));
            }
        });
        bgViagens.setDaemon(true);
        bgViagens.start();
    }
    
    // DM007: delega para ViagemDAO.definirViagemAtiva() em vez de SQL inline
    private boolean salvarViagemAtivaNoBanco(long idViagemSelecionada) {
        try {
            return viagemDAO.definirViagemAtiva(idViagemSelecionada);
        } catch (Exception e) {
            AppLogger.error("TelaPrincipalController", e.getMessage(), e);
            return false;
        }
    }

    // =========================================================================
    //  MÉTODO DO BOTÃO ATUALIZADO
    // =========================================================================
    // DP035: DB operations em background thread
    @FXML private void handleCarregarDadosDaViagem(ActionEvent event) {
        String selecionada = cmbViagemAtiva.getValue();
        if (selecionada == null || selecionada.isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Atenção", "Selecione uma viagem.");
            return;
        }
        btnCarregarDadosDaViagem.setDisable(true);
        Thread bg = new Thread(() -> {
            try {
                Long idViagem = viagemDAO.obterIdViagemPelaString(selecionada);
                boolean sucesso = idViagem != null && salvarViagemAtivaNoBanco(idViagem);
                Platform.runLater(() -> {
                    btnCarregarDadosDaViagem.setDisable(false);
                    if (sucesso) {
                        AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Viagem definida como ativa no sistema!");
                        atualizarDashboard();
                        construirCalendario();
                    } else {
                        AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível definir a viagem como ativa.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnCarregarDadosDaViagem.setDisable(false);
                    AlertHelper.show(AlertType.ERROR, "Erro", e.getMessage());
                });
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // --- MÉTODOS DE ABERTURA DE TELA ---
    
    // 1. Abertura MODAL (Bloqueia a tela de trás - Padrão para cadastros)
    private void abrirTelaModal(String fxml, String titulo, boolean max) {
        try {
            URL url = getClass().getResource(fxml);
            if (url == null) { AlertHelper.show(AlertType.ERROR, "Erro", "Arquivo não encontrado: " + fxml); return; }
            Parent pane = new FXMLLoader(url).load();
            Stage stage = new Stage(); stage.setTitle(titulo); 
            Scene scene = new Scene(pane);
            aplicarTema(scene); 
            stage.setScene(scene);
            
            stage.initOwner((Stage) rootPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            if (max) stage.setMaximized(true);
            stage.showAndWait();
            
            atualizarDashboard(); construirCalendario();
        } catch (IOException e) { AppLogger.error("TelaPrincipalController", e.getMessage(), e); AlertHelper.show(AlertType.ERROR, "Erro", e.getMessage()); }
    }

    // 2. Abertura em ABA (Sistema de abas dentro da janela principal)
    private void abrirTelaLivre(String fxml, String titulo) {
        // Verificar se já existe uma aba com esse título
        for (Tab tab : tabPanePrincipal.getTabs()) {
            if (tab.getText().equals(titulo)) {
                tabPanePrincipal.getSelectionModel().select(tab);
                return;
            }
        }

        // Criar aba com indicador de carregamento
        Tab novaAba = new Tab(titulo);
        Label lblCarregando = new Label("Carregando...");
        lblCarregando.setStyle("-fx-font-size: 14px; -fx-padding: 20;");
        novaAba.setContent(lblCarregando);
        novaAba.setClosable(true);
        tabPanePrincipal.getTabs().add(novaAba);
        tabPanePrincipal.getSelectionModel().select(novaAba);

        // DR114: daemon para carregar FXML em background
        Thread bgFxml = new Thread(() -> {
            try {
                URL url = getClass().getResource(fxml);
                if (url == null) {
                    Platform.runLater(() -> {
                        tabPanePrincipal.getTabs().remove(novaAba);
                        AlertHelper.show(AlertType.ERROR, "Erro", "Arquivo não encontrado: " + fxml);
                    });
                    return;
                }
                FXMLLoader loader = new FXMLLoader(url);
                Parent pane = loader.load();

                Platform.runLater(() -> {
                    novaAba.setContent(pane);
                    pane.getProperties().put("parentTab", novaAba);
                    pane.getProperties().put("parentTabPane", tabPanePrincipal);

                    novaAba.setOnClosed(event -> {
                        atualizarDashboard();
                        construirCalendario();
                    });
                });
            } catch (IOException e) {
                AppLogger.error("TelaPrincipalController", e.getMessage(), e);
                Platform.runLater(() -> {
                    tabPanePrincipal.getTabs().remove(novaAba);
                    AlertHelper.show(AlertType.ERROR, "Erro", e.getMessage());
                });
            }
        });
        bgFxml.setDaemon(true);
        bgFxml.start();
    }
    
    /**
     * Método utilitário estático para fechar a aba que contém um determinado Node.
     * Telas filhas podem chamar este método no lugar de ((Stage) node.getScene().getWindow()).close()
     * para funcionar tanto em modo de aba quanto em janela separada.
     */
    public static void fecharTelaAtual(Node node) {
        if (node == null) return;
        
        // Procurar a propriedade parentTab nos pais do node
        Node current = node;
        while (current != null) {
            Object tab = current.getProperties().get("parentTab");
            Object tabPane = current.getProperties().get("parentTabPane");
            if (tab instanceof Tab && tabPane instanceof TabPane) {
                Tab parentTab = (Tab) tab;
                TabPane parentTabPane = (TabPane) tabPane;
                parentTabPane.getTabs().remove(parentTab);
                return;
            }
            current = current.getParent();
        }
        
        // Fallback: se não encontrou aba, tenta fechar como Stage (modo legado)
        try {
            if (node.getScene() != null && node.getScene().getWindow() != null) {
                Stage stage = (Stage) node.getScene().getWindow();
                // Verificar se é a janela principal (não devemos fechá-la)
                // Se tiver TabPane, é a janela principal
                if (stage.getScene().getRoot().lookup(".main-tab-pane") != null) {
                    // É a janela principal - não fechar!
                    System.out.println("Tentativa de fechar janela principal ignorada. Use fechar aba.");
                    return;
                }
                stage.close();
            }
        } catch (Exception ex) {
            AppLogger.error("TelaPrincipalController", ex.getMessage(), ex);
        }
    }
    
    // 3. Abertura em JANELA SEPARADA (para casos especiais que realmente precisam)
    private void abrirTelaJanelaSeparada(String fxml, String titulo) {
        try {
            URL url = getClass().getResource(fxml);
            if (url == null) { AlertHelper.show(AlertType.ERROR, "Erro", "Arquivo não encontrado: " + fxml); return; }
            Parent pane = new FXMLLoader(url).load();
            Stage stage = new Stage(); stage.setTitle(titulo); 
            Scene scene = new Scene(pane);
            aplicarTema(scene); 
            stage.setScene(scene);
            
            stage.initModality(Modality.NONE); 
            stage.initStyle(StageStyle.DECORATED);
            
            stage.setOnHidden(event -> {
                atualizarDashboard();
                construirCalendario();
            });
            
            stage.setMaximized(true); 
            stage.show(); 
            
        } catch (IOException e) { AppLogger.error("TelaPrincipalController", e.getMessage(), e); AlertHelper.show(AlertType.ERROR, "Erro", e.getMessage()); }
    }
    
    // Método específico para telas que precisam da viagem ativa (Venda de passagem, encomenda...)
    // Verifica viagem ativa em thread de background para nao travar a UI
    private void abrirTelaComViagem(String fxml, String titulo) {
         new Thread(() -> {
             Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
             Platform.runLater(() -> {
                 if (viagemAtiva == null) {
                     AlertHelper.show(AlertType.WARNING, "Atenção", "Ative uma viagem.");
                     return;
                 }
                 abrirTelaLivre(fxml, titulo);
             });
         }).start();
    }
    
    // =========================================================================
    //  MÉTODOS DE RELATÓRIO / BALANÇO (MODIFICADO COM CORREÇÃO)
    // =========================================================================
    
    // Método ATUALIZADO para abrir o Balanço Financeiro passando o ID
    @FXML 
    private void handleRelatorioGeralViagem(ActionEvent e) { 
        Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
        
        if (viagemAtiva == null) {
            AlertHelper.show(AlertType.WARNING, "Atenção", "Nenhuma viagem ativa encontrada.\nSelecione e ative uma viagem no painel superior.");
            return;
        }

        // #010: cast seguro Long->int (IDs de viagem nunca excedem Integer.MAX_VALUE)
        abrirTelaBalancoFinanceiro(Math.toIntExact(viagemAtiva.getId()));
    }
    
    // Método que faz a injeção do ID no BalancoViagemController
    private void abrirTelaBalancoFinanceiro(int idViagem) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BalancoViagem.fxml"));
            Parent root = loader.load();

            // Pega o controlador e passa o ID
            BalancoViagemController controller = loader.getController();
            controller.inicializarDados(idViagem);

            Stage stage = new Stage();
            stage.setTitle("Balanço Financeiro Detalhado");
            Scene scene = new Scene(root);
            aplicarTema(scene); 
            
            stage.setScene(scene);
            stage.initModality(Modality.NONE);
            stage.setMaximized(true);
            
            // Atualiza o dashboard quando a janela for fechada
            stage.setOnHidden(event -> {
                atualizarDashboard();
                construirCalendario();
            });
            
            stage.show();

        } catch (IOException ex) {
            AppLogger.error("TelaPrincipalController", ex.getMessage(), ex);
            AlertHelper.show(AlertType.ERROR, "Erro ao abrir Relatório", "Não foi possível carregar a tela de Balanço.\nErro: " + ex.getMessage());
        }
    }
    
    // =========================================================================
    //  NOVO MÉTODO PARA O BOTÃO DE RECIBOS AVULSOS
    // =========================================================================
    @FXML 
    private void handleReciboAvulso(ActionEvent e) { 
        // Usa 'abrirTelaComViagem' para garantir que existe uma viagem ativa vinculada ao recibo
        abrirTelaComViagem("/gui/GerarReciboAvulso.fxml", "Emissão e Histórico de Recibos"); 
    }

    // --- HANDLERS DO MENU (Outros) ---

    // Financeiro e Movimentações (protegidos por permissao)
    @FXML private void handleInserirEntrada(ActionEvent e) { if (PermissaoService.exigirFinanceiro("Lançar Entrada Financeira")) abrirTelaLivre("/gui/FinanceiroEntrada.fxml", "Lançar Entrada Financeira"); }
    @FXML private void handleInserirSaida(ActionEvent e) { if (PermissaoService.exigirFinanceiro("Lançar Despesa")) abrirTelaLivre("/gui/FinanceiroSaida.fxml", "Lançamento de Despesas"); }
    @FXML private void handleVenderPassagem(ActionEvent e) { if (PermissaoService.exigirOperacional("Vender Passagem")) abrirTelaComViagem("/gui/VenderPassagem.fxml", "Vender Passagem"); }
    @FXML private void handleInserirEncomenda(ActionEvent e) { if (PermissaoService.exigirOperacional("Nova Encomenda")) abrirTelaComViagem("/gui/InserirEncomenda.fxml", "Nova Encomenda"); }
    @FXML private void handleCadastrarFrete(ActionEvent e) { if (PermissaoService.exigirOperacional("Lançar Frete")) abrirTelaLivre("/gui/CadastroFrete.fxml", "Lançar Novo Frete"); }
    @FXML private void handleListaPassagensNovo(ActionEvent e) { if (PermissaoService.exigirOperacional("Listar Passageiros")) abrirTelaLivre("/gui/ListarPassageirosViagem.fxml", "Passageiros"); }
    @FXML private void handleListaFrete(ActionEvent e) { if (PermissaoService.exigirOperacional("Listar Fretes")) abrirTelaLivre("/gui/ListaFretes.fxml", "Fretes"); }
    @FXML private void handleListaEncomenda(ActionEvent e) { if (PermissaoService.exigirOperacional("Listar Encomendas")) abrirTelaLivre("/gui/ListaEncomenda.fxml", "Encomendas"); }

    // Relatórios (protegidos por permissao financeira)
    @FXML private void handleRelatorioPassagem(ActionEvent e) { if (PermissaoService.exigirFinanceiro("Relatório Passagens")) abrirTelaLivre("/gui/RelatorioPassagens.fxml", "Relatório Passagens"); }
    @FXML private void handleRelatorioFrete(ActionEvent e) { if (PermissaoService.exigirFinanceiro("Relatório Fretes")) abrirTelaLivre("/gui/RelatorioFretes.fxml", "Relatório Fretes"); }
    @FXML private void handleRelatorioEncomenda(ActionEvent e) { if (PermissaoService.exigirFinanceiro("Relatório Encomendas")) abrirTelaLivre("/gui/RelatorioEncomendaGeral.fxml", "Central de Relatórios de Encomendas"); }

    // Cadastros Administrativos (protegidos por permissao admin)
    @FXML private void handleCadastrarViagem(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Viagem")) { abrirTelaModal("/gui/CadastroViagem.fxml", "Cadastro de Viagem", true); carregarViagensNoCombo(); construirCalendario(); } }
    @FXML private void handleCadastrarEmpresa(ActionEvent e) { if (PermissaoService.exigirAdmin("Configurações da Empresa")) abrirTelaModal("/gui/CadastroEmpresa.fxml", "Configurações", false); }
    @FXML private void handleCadastrarUsuario(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Usuários")) abrirTelaModal("/gui/CadastroUsuario.fxml", "Usuários", false); }
    @FXML private void handleCadastrarRotas(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Rotas")) abrirTelaModal("/gui/Rotas.fxml", "Rotas", false); }
    @FXML private void handleCadastroTarifa(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Tarifas")) abrirTelaModal("/gui/CadastroTarifa.fxml", "Tarifas", false); }
    @FXML private void handleCadastrarConferente(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Conferentes")) abrirTelaModal("/gui/CadastroConferente.fxml", "Conferentes", false); }
    @FXML private void handleCadastrarCaixa(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Caixas")) abrirTelaModal("/gui/CadastroCaixa.fxml", "Caixas", false); }
    @FXML private void handleProductos(ActionEvent e) { if (PermissaoService.exigirAdmin("Cadastro de Itens")) abrirTelaModal("/gui/CadastroItem.fxml", "Itens", false); }
    @FXML private void handleTabelasAuxiliares(ActionEvent e) { if (PermissaoService.exigirAdmin("Tabelas Auxiliares")) abrirTelaModal("/gui/TabelasAuxiliares.fxml", "Auxiliares", false); }
    @FXML private void handleClientesEncomenda(ActionEvent e) { if (PermissaoService.exigirOperacional("Cadastro de Clientes")) abrirTelaModal("/gui/CadastroClientesEncomenda.fxml", "Clientes", false); }
    @FXML private void handleTabelaPrecoFrete(ActionEvent e) { if (PermissaoService.exigirAdmin("Tabela de Preços Frete")) abrirTelaModal("/gui/TabelaPrecoFrete.fxml", "Tabela de Preços", false); }
    @FXML private void handlePrecoEncomenda(ActionEvent e) { if (PermissaoService.exigirAdmin("Tabela de Preços Encomenda")) abrirTelaModal("/gui/TabelaPrecosEncomenda.fxml", "Tabela de Preços", false); }
    
    // =========================================================================
    // FUNCIONALIDADE 1: BACKUP PROFISSIONAL COM PG_DUMP
    // =========================================================================
    @FXML
    private void handleBackup(ActionEvent e) {
        if (!PermissaoService.exigirAdmin("Backup do Banco de Dados")) return;
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Backup do Banco de Dados");
            fileChooser.setInitialFileName("backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Arquivo SQL", "*.sql"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
            );
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

            Stage stage = (Stage) rootPane.getScene().getWindow();
            File arquivoDestino = fileChooser.showSaveDialog(stage);
            if (arquivoDestino == null) return;

            Alert alertProgresso = new Alert(AlertType.INFORMATION);
            alertProgresso.setTitle("Backup em Andamento");
            alertProgresso.setHeaderText("Aguarde...");
            alertProgresso.setContentText("Gerando backup do banco de dados.\nIsso pode levar alguns segundos.");
            alertProgresso.show();

            service.BackupService backupService = new service.BackupService();

            Thread backupThread = new Thread(() -> {
                service.BackupService.BackupResult resultado = backupService.executarBackup(arquivoDestino);
                Platform.runLater(() -> {
                    alertProgresso.close();
                    if (resultado.isSucesso()) {
                        LogService.registrarInfo("Backup realizado com sucesso: " + arquivoDestino.getAbsolutePath());
                        Alert sucesso = new Alert(AlertType.INFORMATION);
                        sucesso.setTitle("Backup Concluído");
                        sucesso.setHeaderText("Backup realizado com sucesso!");
                        sucesso.setContentText(resultado.getMensagem() + "\n\nTamanho: " + resultado.getTamanhoFormatado());
                        aplicarEstiloAlerta(sucesso);
                        sucesso.showAndWait();
                    } else {
                        LogService.registrarErro("Falha no backup: " + resultado.getMensagem());
                        Alert erro = new Alert(AlertType.ERROR);
                        erro.setTitle("Erro no Backup");
                        erro.setHeaderText("Não foi possível gerar o backup");
                        erro.setContentText(resultado.getMensagem());
                        aplicarEstiloAlerta(erro);
                        erro.showAndWait();
                    }
                });
            });
            backupThread.setDaemon(true);
            backupThread.start();

        } catch (Exception ex) {
            LogService.registrarErro("Erro ao iniciar backup", ex);
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao iniciar o backup: " + ex.getMessage());
        }
    }
    
    // =========================================================================
    // FUNCIONALIDADE 2: CONFIGURAR IMPRESSORAS
    // =========================================================================
    @FXML 
    private void handleConfigurarImpressoras(ActionEvent e) {
        RelatorioUtil.configurarImpressoras();
    }
    
    // =========================================================================
    // FUNCIONALIDADE 2.5: CONFIGURAR API WEB
    // =========================================================================
    @FXML
    private void handleConfigurarApi(ActionEvent e) {
        if (!PermissaoService.exigirAdmin("Configurar API")) return;
        ConfigurarApiController.abrir();
    }
    
    // =========================================================================
    // FUNCIONALIDADE 3: RELATÓRIO DE ERROS (LOGS)
    // =========================================================================
    @FXML 
    private void handleRelatorioErros(ActionEvent e) {
        if (LogService.arquivoExiste()) {
            // Perguntar se quer abrir ou limpar
            Alert opcoes = new Alert(AlertType.CONFIRMATION);
            opcoes.setTitle("Relatório de Erros");
            opcoes.setHeaderText("O que você deseja fazer?");
            opcoes.setContentText("Escolha uma opção para o arquivo de log de erros:");
            
            ButtonType btnAbrir = new ButtonType("Abrir Log", ButtonBar.ButtonData.LEFT);
            ButtonType btnLimpar = new ButtonType("Limpar Log", ButtonBar.ButtonData.LEFT);
            ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            opcoes.getButtonTypes().setAll(btnAbrir, btnLimpar, btnCancelar);
            aplicarEstiloAlerta(opcoes);
            
            Optional<ButtonType> result = opcoes.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == btnAbrir) {
                    boolean abriu = LogService.abrirArquivoLog();
                    if (!abriu) {
                        AlertHelper.show(AlertType.WARNING, "Aviso", "Não foi possível abrir o arquivo de log.");
                    }
                } else if (result.get() == btnLimpar) {
                    // Confirmar limpeza
                    Alert confirma = new Alert(AlertType.WARNING);
                    confirma.setTitle("Confirmar Limpeza");
                    confirma.setHeaderText("Tem certeza?");
                    confirma.setContentText("Isso irá apagar todo o histórico de erros registrados.");
                    confirma.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    aplicarEstiloAlerta(confirma);
                    
                    if (confirma.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        LogService.limparLog();
                        AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Log de erros limpo com sucesso!");
                    }
                }
            }
        } else {
            // Arquivo não existe
            Alert info = new Alert(AlertType.INFORMATION);
            info.setTitle("Relatório de Erros");
            info.setHeaderText("Nenhum erro registrado");
            info.setContentText(
                "O sistema não registrou nenhum erro até o momento.\n\n" +
                "Isso é uma boa notícia! Significa que a aplicação está funcionando corretamente.\n\n" +
                "Quando ocorrer algum erro, ele será automaticamente registrado no arquivo:\n" +
                LogService.getCaminhoArquivo()
            );
            aplicarEstiloAlerta(info);
            info.showAndWait();
        }
    }
    
    // =========================================================================
    // FUNCIONALIDADE 4: AJUDA / SOBRE
    // =========================================================================
    @FXML 
    private void handleAjuda(ActionEvent e) {
        Alert sobre = new Alert(AlertType.INFORMATION);
        sobre.setTitle("Sobre o Naviera");
        sobre.setHeaderText(null);
        
        // Criar conteúdo personalizado
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // Logo/Ícone do sistema (se existir)
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/gui/icons/logo_icon.png")));
            logo.setFitWidth(64);
            logo.setFitHeight(64);
            content.getChildren().add(logo);
        } catch (Exception ex) {
            // Sem logo, usar texto
        }
        
        // Nome do Sistema
        Label lblNome = new Label("Naviera - Navegação Fluvial");
        lblNome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #047857;");
        
        // Versão
        Label lblVersao = new Label("Versão 1.0");
        lblVersao.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #666;");
        
        // Separador
        Separator sep = new Separator();
        sep.setPrefWidth(250);
        
        // Créditos
        Label lblCreditos = new Label("Desenvolvido por Jessica");
        lblCreditos.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
        
        // Ano
        Label lblAno = new Label("© " + LocalDate.now().getYear() + " - Todos os direitos reservados");
        lblAno.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        
        // Separador
        Separator sep2 = new Separator();
        sep2.setPrefWidth(250);
        
        // Suporte
        Label lblSuporte = new Label("📧 Suporte Técnico");
        lblSuporte.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label lblContatoSuporte = new Label("Para dúvidas ou problemas, entre em\ncontato com o suporte técnico.");
        lblContatoSuporte.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-text-alignment: center;");
        lblContatoSuporte.setWrapText(true);
        
        // Informações do ambiente
        Label lblAmbiente = new Label(
            "Java: " + System.getProperty("java.version") + 
            " | JavaFX | PostgreSQL 17"
        );
        lblAmbiente.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");
        
        content.getChildren().addAll(
            lblNome, lblVersao, sep, 
            lblCreditos, lblAno, sep2,
            lblSuporte, lblContatoSuporte,
            lblAmbiente
        );
        
        sobre.getDialogPane().setContent(content);
        sobre.getDialogPane().setPrefWidth(350);
        sobre.getDialogPane().setPrefHeight(400);
        
        aplicarEstiloAlerta(sobre);
        sobre.showAndWait();
    }
    
    // =========================================================================
    // SINCRONIZAÇÃO COM API
    // =========================================================================
    
    @FXML
    private void handleSincronizarAgora(ActionEvent event) {
        if (!PermissaoService.exigirAdmin("Sincronização")) return;
        try {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Sincronização");
            alert.setHeaderText("Iniciando sincronização...");
            alert.setContentText("Aguarde enquanto os dados são sincronizados com o servidor.");
            aplicarEstiloAlerta(alert);
            alert.show();
            
            gui.util.SyncClient syncClient = gui.util.SyncClient.getInstance();
            
            // Executar sincronizacao em daemon thread (fix DP019)
            Thread syncThread = new Thread(() -> {
                try {
                    syncClient.sincronizarTudo();
                    Platform.runLater(() -> {
                        alert.close();
                        AlertHelper.show(AlertType.INFORMATION, "Sincronização Concluída", 
                            "Dados sincronizados com sucesso!");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        alert.close();
                        AlertHelper.show(AlertType.ERROR, "Erro na Sincronização", 
                            "Não foi possível sincronizar os dados:\n" + e.getMessage());
                    });
                    LogService.registrarErro("Erro ao sincronizar", e);
                }
            });
            syncThread.setDaemon(true);
            syncThread.start();
            
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao iniciar sincronização: " + e.getMessage());
            LogService.registrarErro("Erro ao iniciar sincronização", e);
        }
    }
    
    @FXML
    private void handleConfigurarSincronizacao(ActionEvent event) {
        abrirTelaSincronizacao();
    }
    
    private void abrirTelaSincronizacao() {
        try {
            URL url = getClass().getResource("/gui/ConfigurarSincronizacao.fxml");
            if (url == null) { 
                AlertHelper.show(AlertType.ERROR, "Erro", "Arquivo não encontrado: /gui/ConfigurarSincronizacao.fxml"); 
                return; 
            }
            
            Parent pane = new FXMLLoader(url).load();
            Stage stage = new Stage();
            stage.setTitle("Configurar Sincronização");
            
            Scene scene = new Scene(pane, 850, 700);
            
            // Aplicar CSS específico da tela de sincronização
            URL cssUrl = getClass().getResource("/gui/ConfigurarSincronizacao.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Aplicar tema atual (claro ou escuro)
            aplicarTema(scene);
            
            // Se estiver em modo escuro, adicionar classe dark-mode
            if (isModoEscuro) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
            
            stage.setScene(scene);
            stage.initOwner((Stage) rootPane.getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(650);
            
            stage.showAndWait();
            
            // Atualiza o dashboard após fechar a tela de sincronização
            atualizarDashboard();
            
        } catch (Exception e) { 
            AppLogger.error("TelaPrincipalController", e.getMessage(), e); 
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao abrir tela: " + e.getMessage()); 
        }
    }
    
    @FXML
    private void handleStatusSincronizacao(ActionEvent event) {
        try {
            gui.util.SyncClient syncClient = gui.util.SyncClient.getInstance();
            String status = syncClient.obterStatusSincronizacao();
            
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Status da Sincronização");
            alert.setHeaderText("Informações de Sincronização");
            alert.setContentText(status);
            aplicarEstiloAlerta(alert);
            alert.showAndWait();
            
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao obter status: " + e.getMessage());
            LogService.registrarErro("Erro ao obter status de sincronização", e);
        }
    }
    
    // =========================================================================
    // FUNCIONALIDADE 5: SAIR COM SEGURANÇA
    // =========================================================================
    @FXML 
    private void handleSair(ActionEvent e) {
        Alert confirma = new Alert(AlertType.CONFIRMATION);
        confirma.setTitle("Confirmar Saída");
        confirma.setHeaderText("Deseja realmente sair?");
        confirma.setContentText("Tem certeza que deseja fechar o sistema?\n\nTodas as janelas abertas serão fechadas.");
        
        // Personalizar botões
        ButtonType btnSim = new ButtonType("Sim, Sair", ButtonBar.ButtonData.YES);
        ButtonType btnNao = new ButtonType("Não, Continuar", ButtonBar.ButtonData.NO);
        confirma.getButtonTypes().setAll(btnSim, btnNao);
        
        aplicarEstiloAlerta(confirma);
        
        Optional<ButtonType> result = confirma.showAndWait();
        
        if (result.isPresent() && result.get() == btnSim) {
            LogService.registrarInfo("Sistema encerrado pelo usuário.");
            Platform.exit();
            System.exit(0);
        }
    }
    
    /**
     * Aplica o estilo do tema atual ao Alert
     */
    private void aplicarEstiloAlerta(Alert alert) {
        if (isModoEscuro) {
            DialogPane dialogPane = alert.getDialogPane();
            java.net.URL url = getClass().getResource(cssEscuro);
            if (url != null) {
                dialogPane.getStylesheets().add(url.toExternalForm());
            }
        }
    }

}
