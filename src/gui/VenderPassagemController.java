package gui;

import dao.PassageiroDAO;
import dao.PassagemDAO;
import dao.TarifaDAO;
import dao.ViagemDAO;
import dao.ConexaoBD;
import dao.AuxiliaresDAO;

import java.util.Arrays;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderStroke;       
import javafx.scene.layout.BorderStrokeStyle;  
import javafx.scene.layout.BorderWidths;       
import javafx.scene.layout.CornerRadii;        
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture; 
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import gui.util.MoneyUtil;
import model.Passageiro;
import model.Passagem;
import model.Tarifa;
import model.Viagem;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

// Imports para Impressao AWT (Termica) e Rotacao de Imagem
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable; 
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob; 
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import gui.util.AlertHelper;
import gui.util.AppLogger;
import gui.util.PassagemPrintHelper;
import gui.util.ValidationHelper;


public class VenderPassagemController implements Initializable {

    // =======================================================================
    // <<< COMPONENTES FXML >>>
    // =======================================================================
    @FXML private BorderPane rootPane;
    @FXML private TextField txtNumeroDoc;
    @FXML private TextField txtIdade;
    @FXML private DatePicker dpDataNascimento;
    @FXML private TextField txtNascimentoMask;
    @FXML private TextField txtAlimentacao;
    @FXML private TextField txtTransporte;
    @FXML private TextField txtCargas;
    @FXML private TextField txtDescontoTarifa;
    @FXML private TextField txtTotal;
    @FXML private TextField txtDesconto;
    @FXML private TextField txtAPagar;
    @FXML private TextField txtRequisicao;
    @FXML private TextField txtHorario;
    @FXML private ComboBox<String> cmbPassageiroAuto;
    @FXML private ComboBox<String> cmbRota;
    @FXML private ComboBox<String> cmbTipoPassagemAux;
    @FXML private ComboBox<String> cmbViagem;
    @FXML private ComboBox<String> cmbSexo;
    @FXML private ComboBox<String> cmbTipoDoc;
    @FXML private ComboBox<String> cmbNacionalidade;
    @FXML private ComboBox<String> cmbAgenteAux;
    @FXML private ComboBox<String> cmbAcomodacao;
    @FXML private ComboBox<String> cmbPesquisarModo;
    @FXML private TextField txtPesquisar;
    @FXML private TextField txtNumBilhete;
    @FXML private TextField txtDataViagemMask;
    @FXML private DatePicker dpDataViagem;
    @FXML private TextField txtTotalPassageiros;
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;
    @FXML private Button btnNovo;
    @FXML private Button btnFiltrar;
    @FXML private Button btnEditar;
    @FXML private Button btnExcluir;
    @FXML private Button btnImprimirBilhete;
    @FXML private Button btnImprimirLista;
    @FXML private Button btnRelatorio;
    @FXML private Button btnSair;

    // Tabela
    @FXML private TableView<Passagem> tablePassagens;
    @FXML private TableColumn<Passagem, String> colNumBilhete;
    @FXML private TableColumn<Passagem, String> colPassageiro;
    @FXML private TableColumn<Passagem, LocalDate> colDataNascimento;
    @FXML private TableColumn<Passagem, String> colNumeroDoc;
    @FXML private TableColumn<Passagem, String> colNacionalidade;
    @FXML private TableColumn<Passagem, String> colOrigem;
    @FXML private TableColumn<Passagem, String> colDestino;
    @FXML private TableColumn<Passagem, BigDecimal> colValorTotal;
    @FXML private TableColumn<Passagem, BigDecimal> colValorDesconto;
    @FXML private TableColumn<Passagem, BigDecimal> colValorPago;
    @FXML private TableColumn<Passagem, BigDecimal> colValorAPagar;
    @FXML private TableColumn<Passagem, BigDecimal> colDevedor;

    // DAOs e Variaveis de Controle
    private PassageiroDAO passageiroDAO;
    private PassagemDAO passagemDAO;
    private TarifaDAO tarifaDAO;
    private ViagemDAO viagemDAO;
    private AuxiliaresDAO auxiliaresDAO;

    private Passagem passagemEmEdicao = null;
    private Passageiro passageiroEmEdicao = null;
    private Viagem viagemSelecionada = null;

    private ObservableList<String> nomesOrigemPassageiros;
    private FilteredList<String> passageirosNomesFiltrados;
    private ObservableList<Passageiro> todosOsPassageiros;
    
    // Lista auxiliar para guardar viagens carregadas
    private List<Viagem> listaViagensCarregadas = new ArrayList<>();

    private ObservableList<Passagem> passagensDaViagemAtual = FXCollections.observableArrayList();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean isUpdatingFromCode = false;

    // Variaveis para Dados da Empresa
    private String empPathLogo = "";
    private String empCompanhia = "EMPRESA DE NAVEGA\u00c7\u00c3O";
    private String empEmbarcacao = "F/B DEUS DE ALIAN\u00c7A V";
    private String empProprietario = "Francisco Cintra"; 
    private String empCnpj = "";
    private String empTelefone = "";
    private String empEndereco = "";
    private String empFrase = "Jesus n\u00e3o desiste de voc\u00ea";
    private String empRecomendacoes = "Sem recomenda\u00e7\u00f5es cadastradas.";

    // =======================================================================
    // <<< INITIALIZE E CONFIGURAÃ‡Ã•ES >>>
    // =======================================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passageiroDAO = new PassageiroDAO();
        passagemDAO = new PassagemDAO();
        tarifaDAO = new TarifaDAO();
        viagemDAO = new ViagemDAO();
        auxiliaresDAO = new AuxiliaresDAO();

        todosOsPassageiros = FXCollections.observableArrayList();
        nomesOrigemPassageiros = FXCollections.observableArrayList();
        passageirosNomesFiltrados = new FilteredList<>(nomesOrigemPassageiros, p -> true);

        if (dpDataNascimento != null && txtNascimentoMask != null) {
            configurarDataNascimentoFlexivel();
        }

        carregarDadosEmBackground();

        limparCamposTarifa();
        configurarTabelaPassagens();

        adicionarListenerDeSelecaoNaTabela();
        adicionarListenerAoCampoPesquisar();
        configurarNavegacaoEntreCampos();
        configurarEstadoInicialDaTela();

        if (cmbRota != null) cmbRota.setOnAction(event -> {
            if (!isUpdatingFromCode) carregarValoresTarifaAutomatica();
        });

        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.setOnAction(event -> {
            if (!isUpdatingFromCode) carregarValoresTarifaAutomatica();
        });

        setupCalculoTotalPassagem();

        // >>> CORREÃ‡ÃƒO CRÃTICA DO CAMPO DOCUMENTO <<<
        // Se sair do campo e nao encontrar o Doc, NÃƒO LIMPA O NOME, apenas aceita como novo Doc.
        if (txtNumeroDoc != null) {
            txtNumeroDoc.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && txtNumeroDoc.getText() != null && !txtNumeroDoc.getText().isEmpty()) {
                    preencherPassageiroPorDoc(txtNumeroDoc.getText());
                }
            });
            txtNumeroDoc.setOnAction(event -> {
                if (txtNumeroDoc.getText() != null && !txtNumeroDoc.getText().isEmpty()) {
                    preencherPassageiroPorDoc(txtNumeroDoc.getText());
                }
            });
        }

        configurarListenerComboViagem();
        configurarAtalhosTeclado();

        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                configurarTamanhoMinimoJanela();
            }
        });
    }

    // =======================================================================
    // <<< CARREGAMENTO TURBO (THREAD BACKGROUND) >>>
    // =======================================================================
    private void carregarDadosEmBackground() {
        if (btnNovo != null) btnNovo.setDisable(true);

        Thread bgThread = new Thread(() -> {
            try {
                // =============================================================
                // FASE 1: Carregar VIAGEM primeiro para habilitar NOVO rapido
                // =============================================================
                carregarConfiguracaoEmpresa();

                listaViagensCarregadas = new ArrayList<>();
                List<String> viagensFormatadas = new ArrayList<>();

                String sql = "SELECT id_viagem, data_viagem, data_chegada, descricao, id_horario_saida, is_atual FROM viagens WHERE empresa_id = ? ORDER BY data_viagem DESC";
                try (Connection conn = ConexaoBD.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, dao.DAOUtils.empresaId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Viagem v = new Viagem();
                            v.setId(rs.getLong("id_viagem"));
                            v.setDataViagem(rs.getDate("data_viagem") != null ? rs.getDate("data_viagem").toLocalDate() : null);
                            v.setDataChegada(rs.getDate("data_chegada") != null ? rs.getDate("data_chegada").toLocalDate() : null);
                            v.setDescricao(rs.getString("descricao"));
                            v.setIdHorarioSaida(rs.getLong("id_horario_saida"));
                            v.setIsAtual(rs.getBoolean("is_atual"));

                            listaViagensCarregadas.add(v);

                            String dt = (v.getDataViagem() != null) ? dateFormatter.format(v.getDataViagem()) : "--";
                            String prev = (v.getDataChegada() != null) ? dateFormatter.format(v.getDataChegada()) : "??";
                            viagensFormatadas.add(v.getId() + " - " + dt + " - Prev: " + prev);
                        }
                    }
                } catch (Exception e) {
                    AppLogger.warn("VenderPassagemController", "Erro ao carregar viagens: " + e.getMessage());
                }

                // Habilitar NOVO e Pesquisar IMEDIATAMENTE (antes de carregar passageiros/rotas)
                final List<String> viagensF1 = new ArrayList<>(viagensFormatadas);
                Platform.runLater(() -> {
                    configurarAutoCompleteComboBox(cmbViagem, FXCollections.observableArrayList(viagensF1), false);
                    if (cmbPesquisarModo != null) {
                        cmbPesquisarModo.setItems(FXCollections.observableArrayList("N\u00famero Bilhete", "Passageiro", "N\u00ba Documento", "Data Partida"));
                        cmbPesquisarModo.getSelectionModel().selectFirst();
                    }
                    carregarViagemAtualAoIniciar(); // <<< Habilita NOVO aqui
                });

                // =============================================================
                // FASE 2: Carregar demais dados (passageiros, rotas, auxiliares)
                // =============================================================
                List<Passageiro> listaP = passageiroDAO.listarTodos();

                List<String> rotasStrings = new ArrayList<>();
                try {
                    List<model.Rota> rotasObjects = new dao.RotaDAO().listarTodasAsRotasComoObjects();
                    if (rotasObjects != null) {
                        for (model.Rota r : rotasObjects) rotasStrings.add(r.toString());
                    }
                } catch (Exception e) {
                    AppLogger.warn("VenderPassagemController", "Erro ao carregar rotas: " + e.getMessage());
                }

                List<String> tipoPassagem = auxiliaresDAO.listarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem");
                List<String> sexos = auxiliaresDAO.listarAuxiliar("aux_sexo", "nome_sexo");
                List<String> tiposDoc = auxiliaresDAO.listarAuxiliar("aux_tipos_documento", "nome_tipo_doc");
                List<String> nac = auxiliaresDAO.listarAuxiliar("aux_nacionalidades", "nome_nacionalidade");
                List<String> agentes = auxiliaresDAO.listarAuxiliar("aux_agentes", "nome_agente");
                List<String> acomodacoes = auxiliaresDAO.listarAuxiliar("aux_acomodacoes", "nome_acomodacao");

                Platform.runLater(() -> {
                    todosOsPassageiros.setAll(listaP);
                    nomesOrigemPassageiros.setAll(todosOsPassageiros.stream().map(Passageiro::getNome).collect(Collectors.toList()));

                    configurarAutoCompleteComboBox(cmbPassageiroAuto, nomesOrigemPassageiros, true);
                    configurarAutoCompleteComboBox(cmbRota, FXCollections.observableArrayList(rotasStrings), true);
                    configurarAutoCompleteComboBox(cmbTipoPassagemAux, FXCollections.observableArrayList(tipoPassagem), false);
                    configurarAutoCompleteComboBox(cmbSexo, FXCollections.observableArrayList(sexos), false);
                    configurarAutoCompleteComboBox(cmbTipoDoc, FXCollections.observableArrayList(tiposDoc), false);
                    configurarAutoCompleteComboBox(cmbNacionalidade, FXCollections.observableArrayList(nac), false);
                    configurarAutoCompleteComboBox(cmbAgenteAux, FXCollections.observableArrayList(agentes), false);
                    configurarAutoCompleteComboBox(cmbAcomodacao, FXCollections.observableArrayList(acomodacoes), false);
                });

            } catch (Exception e) {
                AppLogger.warn("VenderPassagemController", "Erro ao carregar dados em background: " + e.getMessage());
                AppLogger.error("VenderPassagemController", e.getMessage(), e);
                Platform.runLater(() -> {
                    if (rootPane != null) rootPane.setDisable(false);
                    if (btnNovo != null) btnNovo.setDisable(false);
                    gui.util.AlertHelper.errorSafe("carregar dados iniciais", e);
                });
            }
        });
        bgThread.setDaemon(true);
        bgThread.start();
    }

    private void configurarDataNascimentoFlexivel() {
        dpDataNascimento.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate date) { return (date != null) ? dateFormatter.format(date) : ""; }
            @Override public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try { return LocalDate.parse(string, dateFormatter); } catch (Exception e) { return null; }
                }
                return null;
            }
        });

        dpDataNascimento.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                txtNascimentoMask.setText(dateFormatter.format(newDate));
                calcularIdade();
            } else {
                if(!txtNascimentoMask.isFocused()) {
                    txtNascimentoMask.clear();
                    if (txtIdade != null) txtIdade.clear();
                }
            }
        });

        txtNascimentoMask.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                String nums = newVal.replaceAll("[^0-9]", "");
                if (nums.length() == 8) {
                     try {
                        String fmt = nums.substring(0, 2) + "/" + nums.substring(2, 4) + "/" + nums.substring(4, 8);
                        if (!newVal.equals(fmt)) {
                             Platform.runLater(() -> txtNascimentoMask.setText(fmt));
                        }
                        LocalDate d = LocalDate.parse(fmt, dateFormatter);
                        dpDataNascimento.setValue(d);
                    } catch (Exception e) {
                        AppLogger.warn("VenderPassagemController", "Erro ao parsear data de nascimento: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void carregarConfiguracaoEmpresa() {
        try {
            dao.EmpresaDAO empresaDAO = new dao.EmpresaDAO();
            model.Empresa empresa = empresaDAO.buscarPorId(dao.EmpresaDAO.ID_EMPRESA_PRINCIPAL);
            if (empresa != null) {
                if (empresa.getCaminhoFoto() != null) this.empPathLogo = empresa.getCaminhoFoto();
                if (empresa.getCompanhia() != null) this.empCompanhia = empresa.getCompanhia();
                if (empresa.getEmbarcacao() != null) this.empEmbarcacao = empresa.getEmbarcacao();
                if (empresa.getCnpj() != null) this.empCnpj = empresa.getCnpj();
                if (empresa.getTelefone() != null) this.empTelefone = empresa.getTelefone();
                if (empresa.getFrase() != null) this.empFrase = empresa.getFrase();
                if (empresa.getRecomendacoesBilhete() != null) this.empRecomendacoes = empresa.getRecomendacoesBilhete().replace("\\n", "\n");
                this.empProprietario = this.empCompanhia;
            }
        } catch (Exception e) {
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
        }
    }

    private void carregarViagemAtualAoIniciar() {
        if (this.viagemSelecionada != null) {
            configurarTelaComViagemAtiva();
            return;
        }
        if (listaViagensCarregadas != null) {
            for(Viagem v : listaViagensCarregadas) {
                if(v.getIsAtual()) { setViagemAtiva(v); return; }
            }
        }
    }

    private void configurarListenerComboViagem() {
        if (cmbViagem != null) {
            cmbViagem.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdatingFromCode) return;
                if (newVal != null && !newVal.isEmpty()) {
                    try {
                        String[] parts = newVal.split(" - ");
                        if (parts.length > 0) {
                            long id = Long.parseLong(parts[0].trim());
                            Optional<Viagem> vOpt = listaViagensCarregadas.stream().filter(v -> v.getId() != null && v.getId().equals(id)).findFirst();
                            if (vOpt.isPresent()) {
                                this.viagemSelecionada = vOpt.get();
                                configurarTelaComViagemAtiva();
                            } else {
                                Viagem v = viagemDAO.buscarPorId(id);
                                if (v != null) { this.viagemSelecionada = v; configurarTelaComViagemAtiva(); }
                            }
                        }
                    } catch (Exception e) {
                        AppLogger.warn("VenderPassagemController", "Erro ao parsear selecao de viagem: " + e.getMessage());
                    }
                }
            });
        }
    }

    public void setViagemAtiva(Viagem viagem) {
        this.viagemSelecionada = viagem;
        if (this.viagemSelecionada == null) {
            if (btnNovo != null) btnNovo.setDisable(true);
            return;
        }
        if (cmbViagem != null) {
            isUpdatingFromCode = true;
            for (String item : cmbViagem.getItems()) {
                if (item.startsWith(viagem.getId() + " -")) {
                    cmbViagem.getSelectionModel().select(item);
                    break;
                }
            }
            isUpdatingFromCode = false;
        }
        configurarTelaComViagemAtiva();
    }

    private void configurarTelaComViagemAtiva() {
        if (viagemSelecionada != null) {
            if (dpDataViagem != null) dpDataViagem.setValue(viagemSelecionada.getDataViagem());
            if (txtDataViagemMask != null && viagemSelecionada.getDataViagem() != null) {
                txtDataViagemMask.setText(dateFormatter.format(viagemSelecionada.getDataViagem()));
            }

            String descHorario = viagemSelecionada.getDescricaoHorarioSaida();
            if (descHorario == null || descHorario.isEmpty()) {
                try {
                    descHorario = new dao.AuxiliaresDAO().obterDescricaoHorario(viagemSelecionada.getIdHorarioSaida());
                } catch (Exception e) { AppLogger.warn("VenderPassagemController", "Erro em VenderPassagemController.selecionarViagem (horario): " + e.getMessage()); }
            }
            if (txtHorario != null) txtHorario.setText(descHorario);

            if (btnNovo != null) btnNovo.setDisable(false);
            carregarPassagensNaTabela();
        } else {
            if (btnNovo != null) btnNovo.setDisable(true);
        }
    }

    private void carregarPassagensNaTabela() {
        if (this.viagemSelecionada == null || this.viagemSelecionada.getId() == null) {
            if (tablePassagens != null) tablePassagens.getItems().clear();
            if (txtTotalPassageiros != null) txtTotalPassageiros.setText("0");
            return;
        }
        
        Thread loadThread = new Thread(() -> {
            try {
                List<Passagem> passagensParaExibir = passagemDAO.listarPorViagem(viagemSelecionada.getId());
                
                Platform.runLater(() -> {
                    passagensDaViagemAtual.setAll(passagensParaExibir);
                    if (tablePassagens != null) tablePassagens.setItems(passagensDaViagemAtual);
                    if (txtTotalPassageiros != null) txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));
                });
            } catch (Exception e) {
                AppLogger.warn("VenderPassagemController", "VenderPassagemController.carregarPassagensDaViagem: erro ao carregar passagens — " + e.getMessage());
                AppLogger.error("VenderPassagemController", e.getMessage(), e);
                Platform.runLater(() -> {
                    if (tablePassagens != null) tablePassagens.getItems().clear();
                    if (txtTotalPassageiros != null) txtTotalPassageiros.setText("0");
                });
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void configurarTabelaPassagens() {
        if (tablePassagens == null) return;

        if (colNumBilhete != null) colNumBilhete.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getNumBilhete())));
        if (colPassageiro != null) colPassageiro.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomePassageiro()));
        if (colNumeroDoc != null) colNumeroDoc.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumeroDoc()));
        if (colNacionalidade != null) colNacionalidade.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNacionalidade()));
        if (colOrigem != null) colOrigem.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrigem()));
        if (colDestino != null) colDestino.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDestino()));

        if (colDataNascimento != null) {
            colDataNascimento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataNascimento()));
            colDataNascimento.setCellFactory(column -> new TableCell<>() {
                private final DateTimeFormatter cf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                @Override protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : cf.format(item));
                }
            });
        }

        Callback<TableColumn<Passagem, BigDecimal>, TableCell<Passagem, BigDecimal>> cellFactoryMoeda = param -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("R$ %,.2f", item));
                }
            }
        };

        if (colValorTotal != null) { colValorTotal.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorTotal())); colValorTotal.setCellFactory(cellFactoryMoeda); }
        if (colValorDesconto != null) { colValorDesconto.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorDesconto())); colValorDesconto.setCellFactory(cellFactoryMoeda); }
        if (colValorPago != null) { colValorPago.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorPago())); colValorPago.setCellFactory(cellFactoryMoeda); }
        // DL057: coluna "Valor a Pagar" exibe valorAPagar (nao devedor)
        if (colValorAPagar != null) { colValorAPagar.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorAPagar())); colValorAPagar.setCellFactory(cellFactoryMoeda); }

        if (colDevedor != null) {
            colDevedor.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDevedor()));
            colDevedor.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        if (item.compareTo(BigDecimal.ZERO) <= 0) {
                            setText("PAGO");
                            setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        } else {
                            setText("PENDENTE");
                            setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                    }
                }
            });
        }
    }

    // =======================================================================
    // <<< MÃ‰TODOS DE AÃ‡ÃƒO >>>
    // =======================================================================

    private String obterValorCombo(ComboBox<String> combo) {
        if (combo == null) return null;
        String val = combo.getValue();
        if (val != null && !val.trim().isEmpty()) return val.trim();
        // Fallback: ler direto do editor (para ComboBoxes editaveis)
        if (combo.isEditable() && combo.getEditor() != null) {
            String editorText = combo.getEditor().getText();
            if (editorText != null && !editorText.trim().isEmpty()) return editorText.trim();
        }
        return null;
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        String nomePassageiroDigitado = (cmbPassageiroAuto != null && cmbPassageiroAuto.getEditor() != null) ? cmbPassageiroAuto.getEditor().getText() : "";
        String selectedRotaStr = obterValorCombo(cmbRota);
        String tipoPassagem = obterValorCombo(cmbTipoPassagemAux);
        String acomodacao = obterValorCombo(cmbAcomodacao);
        String agente = obterValorCombo(cmbAgenteAux);

        // Validacao campo a campo com mensagem especifica
        if (!ValidationHelper.requiredCombo(cmbPassageiroAuto, "Passageiro")) return;
        if (!ValidationHelper.requiredDate(dpDataNascimento, "Data de Nascimento")) return;
        if (tipoPassagem == null) { AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Tipo de Passagem deve ser informado."); if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.requestFocus(); return; }
        if (acomodacao == null) { AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Acomodação deve ser informada."); if (cmbAcomodacao != null) cmbAcomodacao.requestFocus(); return; }
        if (selectedRotaStr == null) { AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Rota deve ser informada."); if (cmbRota != null) cmbRota.requestFocus(); return; }
        if (agente == null) { AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Agente deve ser informado."); if (cmbAgenteAux != null) cmbAgenteAux.requestFocus(); return; }
        // DL032: permite valor zero (passagem gratuita) — bloqueia apenas negativo
        if (!ValidationHelper.nonNegativeMoney(txtAPagar, "Valor a Pagar")) return;

        if (this.viagemSelecionada == null || this.viagemSelecionada.getId() == null) {
            AlertHelper.show(AlertType.ERROR, "Erro de Viagem", "Nenhuma viagem selecionada.");
            return;
        }

        try {
            Passagem passagemParaSalvar = (this.passagemEmEdicao != null) ? this.passagemEmEdicao : new Passagem();
            preencherDadosDaPassagem(passagemParaSalvar, nomePassageiroDigitado, selectedRotaStr);

            // DL032: passagem gratuita (valor zero) — pula dialogo de pagamento
            boolean pagamentoConfirmado;
            if (passagemParaSalvar.getValorAPagar() != null &&
                passagemParaSalvar.getValorAPagar().compareTo(BigDecimal.ZERO) == 0) {
                passagemParaSalvar.setValorPago(BigDecimal.ZERO);
                passagemParaSalvar.setDevedor(BigDecimal.ZERO);
                passagemParaSalvar.setTroco(BigDecimal.ZERO);
                passagemParaSalvar.setValorPagamentoDinheiro(BigDecimal.ZERO);
                passagemParaSalvar.setValorPagamentoPix(BigDecimal.ZERO);
                passagemParaSalvar.setValorPagamentoCartao(BigDecimal.ZERO);
                pagamentoConfirmado = true;
            } else {
                pagamentoConfirmado = showFinalizarPagamentoDialog(passagemParaSalvar);
            }

            if (pagamentoConfirmado) {
                salvarDadosFinais(passagemParaSalvar);
            }

        } catch (Exception e) {
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro Cr\u00edtico", "Ocorreu um erro inesperado ao preparar a venda: " + e.getMessage());
        }
    }

    private boolean showFinalizarPagamentoDialog(Passagem passagem) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/FinalizarPagamentoPassagem.fxml"));
        Parent pane = loader.load();

        Stage dialogStage = new Stage();
        dialogStage.setTitle("Finalizar Pagamento da Passagem");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(rootPane.getScene().getWindow());
        Scene scene = new Scene(pane);
        dialogStage.setScene(scene);

        FinalizarPagamentoPassagemController controller = loader.getController();
        controller.setDadosPagamento(dialogStage, passagem);

        dialogStage.showAndWait();

        return controller.isConfirmado();
    }

    private void salvarDadosFinais(Passagem passagemParaSalvar) throws SQLException {
        Passageiro passageiroParaSalvar = (passageiroEmEdicao != null) ? passageiroEmEdicao : new Passageiro();
        passageiroParaSalvar.setNome(cmbPassageiroAuto.getEditor().getText());
        passageiroParaSalvar.setNumeroDoc(txtNumeroDoc.getText());
        passageiroParaSalvar.setDataNascimento(dpDataNascimento.getValue());
        passageiroParaSalvar.setTipoDoc(cmbTipoDoc.getValue());
        passageiroParaSalvar.setSexo(cmbSexo.getValue());
        passageiroParaSalvar.setNacionalidade(cmbNacionalidade.getValue());

        boolean isNovoPassageiro = (passageiroParaSalvar.getId() == null);
        Passageiro passageiroSalvo = passageiroDAO.salvarOuAtualizar(passageiroParaSalvar);

        if (passageiroSalvo == null) {
            AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao salvar os dados do passageiro.");
            return;
        }

        if (isNovoPassageiro) {
            todosOsPassageiros.add(passageiroSalvo);
            todosOsPassageiros.sort(Comparator.comparing(Passageiro::getNome));
            nomesOrigemPassageiros.setAll(todosOsPassageiros.stream().map(Passageiro::getNome).collect(Collectors.toList()));
        }

        passagemParaSalvar.setIdPassageiro(passageiroSalvo.getId());
        // DL033: definir status baseado no resultado real do pagamento
        BigDecimal devedor = passagemParaSalvar.getDevedor();
        BigDecimal valorPago = passagemParaSalvar.getValorPago();
        if (devedor == null || devedor.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) {
            passagemParaSalvar.setStatusPassagem(model.StatusPagamento.PAGO.name());
        } else if (valorPago != null && valorPago.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) {
            passagemParaSalvar.setStatusPassagem(model.StatusPagamento.PARCIAL.name());
        } else {
            passagemParaSalvar.setStatusPassagem(model.StatusPagamento.PENDENTE.name());
        }

        boolean sucesso;
        if (this.passagemEmEdicao == null) {
            sucesso = passagemDAO.inserir(passagemParaSalvar);
        } else {
            sucesso = passagemDAO.atualizar(passagemParaSalvar);
        }

        if (sucesso) {
            Alert alertPrint = new Alert(AlertType.CONFIRMATION);
            alertPrint.setTitle("Venda Conclu\u00edda");
            alertPrint.setHeaderText("Passagem salva com sucesso!");
            alertPrint.setContentText("Deseja imprimir o bilhete agora?");

            ButtonType btnSim = new ButtonType("Sim", ButtonBar.ButtonData.YES);
            ButtonType btnNao = new ButtonType("N\u00e3o", ButtonBar.ButtonData.NO);
            alertPrint.getButtonTypes().setAll(btnSim, btnNao);

            Optional<ButtonType> result = alertPrint.showAndWait();
            if (result.isPresent() && result.get() == btnSim) {
                passagemParaSalvar.setDataNascimento(passageiroSalvo.getDataNascimento());
                passagemParaSalvar.setSexo(passageiroSalvo.getSexo());
                passagemParaSalvar.setNacionalidade(passageiroSalvo.getNacionalidade()); 
                PassagemPrintHelper.EmpresaInfo empInfoSalva = new PassagemPrintHelper.EmpresaInfo(
                        empEmbarcacao, empProprietario, empCnpj, empTelefone, empFrase, empRecomendacoes, empPathLogo);
                java.time.LocalDate dataChegadaSalva = (this.viagemSelecionada != null) ? this.viagemSelecionada.getDataChegada() : null;
                PassagemPrintHelper.mostrarPreviewImpressao(passagemParaSalvar, empInfoSalva, dataChegadaSalva, rootPane.getScene().getWindow());
            }

            Viagem vAtual = this.viagemSelecionada;
            
            Platform.runLater(() -> {
                fecharTodosOsDropdowns();
                limparCamposParaNovaVendaAutomatica();
                this.viagemSelecionada = vAtual;
                configurarTelaComViagemAtiva();

                if(tablePassagens != null) tablePassagens.setDisable(false);
                // Habilitar campos automaticamente para a proxima venda â€” sem necessidade de clicar NOVO
                habilitarCamposParaNovaVenda(true);
                if(btnNovo != null) btnNovo.setDisable(true);

                if (cmbPassageiroAuto != null) {
                    cmbPassageiroAuto.requestFocus();
                }
            });

        } else {
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao salvar passagem.");
        }
    }

    private void preencherDadosDaPassagem(Passagem passagem, String nomePassageiro, String rotaStr) throws SQLException {
        if (this.passagemEmEdicao == null) {
            try {
                passagem.setNumBilhete(Integer.parseInt(txtNumBilhete.getText()));
            } catch (NumberFormatException e) {
                throw new SQLException("N\u00famero de bilhete inv\u00e1lido.");
            }
        }

        String[] rotaParts = rotaStr.split(" - ");
        String origem = rotaParts[0].trim();
        String destino = (rotaParts.length > 1) ? rotaParts[1].trim() : "";
        Integer idRotaInteger = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origem, destino);
        if (idRotaInteger == null) {
            throw new SQLException("ID da Rota n\u00e3o encontrado.");
        }

        passagem.setIdRota(idRotaInteger.longValue());
        passagem.setIdViagem(viagemSelecionada.getId());

        if (viagemSelecionada.getIdHorarioSaida() != null) {
            passagem.setIdHorarioSaida(viagemSelecionada.getIdHorarioSaida().intValue());
        }
        passagem.setDescricaoHorarioSaida(viagemSelecionada.getDescricaoHorarioSaida());

        if (txtIdade.getText() != null && !txtIdade.getText().trim().isEmpty()) {
            try {
                passagem.setIdade(Integer.parseInt(txtIdade.getText().trim()));
            } catch (NumberFormatException e) {
                AppLogger.warn("VenderPassagemController", "Idade invalida: " + txtIdade.getText());
            }
        }
        passagem.setNomePassageiro(nomePassageiro);
        passagem.setTipoPassagemAux(obterValorCombo(cmbTipoPassagemAux));
        passagem.setAgenteAux(obterValorCombo(cmbAgenteAux));
        passagem.setAcomodacao(obterValorCombo(cmbAcomodacao));
        passagem.setOrigem(origem);
        passagem.setDestino(destino);
        passagem.setDataViagem(viagemSelecionada.getDataViagem());
        passagem.setStrViagem(viagemSelecionada.toString());
        passagem.setRequisicao(txtRequisicao.getText());
        passagem.setValorAlimentacao(MoneyUtil.parseBigDecimal(txtAlimentacao.getText()));
        passagem.setValorTransporte(MoneyUtil.parseBigDecimal(txtTransporte.getText()));
        passagem.setValorCargas(MoneyUtil.parseBigDecimal(txtCargas.getText()));
        passagem.setValorDescontoTarifa(MoneyUtil.parseBigDecimal(txtDescontoTarifa.getText()));
        passagem.setValorDesconto(MoneyUtil.parseBigDecimal(txtDesconto.getText()));
        passagem.setValorTotal(MoneyUtil.parseBigDecimal(txtTotal.getText()));
        passagem.setValorAPagar(MoneyUtil.parseBigDecimal(txtAPagar.getText()));
        // #025: usar enum padrao em vez de string (status real definido em salvarDadosFinais baseado no pagamento)
        passagem.setStatusPassagem(model.StatusPagamento.PENDENTE.name());
    }

    // =======================================================================
    // <<< OUTROS MÃ‰TODOS AUXILIARES >>>
    // =======================================================================

    private void configurarAutoCompleteComboBox(ComboBox<String> comboBox, ObservableList<String> items, boolean customAction) {
        if (comboBox == null || items == null) return;

        comboBox.setEditable(true);
        comboBox.setItems(items);

        final ObservableList<String> itemsOriginal = FXCollections.observableArrayList(items);
        final boolean[] isFiltering = {false};
        final boolean[] userNavigated = {false};

        // =====================================================================
        // NAVEGACAO COM SETAS — para TODOS os ComboBoxes editaveis
        // Intercepta UP/DOWN antes do editor (TextField) mover o cursor.
        // =====================================================================
        comboBox.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, arrowEvent -> {
            if (arrowEvent.getCode() == KeyCode.DOWN || arrowEvent.getCode() == KeyCode.UP) {
                arrowEvent.consume(); // Impede o editor de mover o cursor
                userNavigated[0] = true;
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
                if (!comboBox.getItems().isEmpty()) {
                    int currentIndex = comboBox.getSelectionModel().getSelectedIndex();
                    int newIndex;
                    if (arrowEvent.getCode() == KeyCode.DOWN) {
                        newIndex = (currentIndex < comboBox.getItems().size() - 1) ? currentIndex + 1 : 0;
                    } else {
                        newIndex = (currentIndex > 0) ? currentIndex - 1 : comboBox.getItems().size() - 1;
                    }
                    isUpdatingFromCode = true;
                    comboBox.getSelectionModel().select(newIndex);
                    String selected = comboBox.getItems().get(newIndex);
                    comboBox.getEditor().setText(selected);
                    comboBox.getEditor().positionCaret(selected.length());
                    isUpdatingFromCode = false;
                }
            }
        });

        // =====================================================================
        // INTERCEPTORES DE ESPAÇO — SOMENTE para o campo de passageiro
        // O skin do ComboBox ao receber SPACE confirma o item destacado.
        // Para os outros combos (rota, sexo, etc.) isso não é problema.
        // =====================================================================
        if (comboBox == cmbPassageiroAuto) {
            // KEY_PRESSED: consumir SPACE + inserir manualmente (unica insercao).
            comboBox.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, spaceEvent -> {
                if (spaceEvent.getCode() == KeyCode.SPACE && !spaceEvent.isControlDown() && !spaceEvent.isAltDown()) {
                    spaceEvent.consume();
                    TextField editor = comboBox.getEditor();
                    String textoAtual = editor.getText();
                    int pos = editor.getCaretPosition();
                    // Evitar espacos consecutivos
                    if (pos > 0 && textoAtual != null && pos <= textoAtual.length()
                            && textoAtual.charAt(pos - 1) == ' ') {
                        return;
                    }
                    isUpdatingFromCode = true;
                    editor.insertText(pos, " ");
                    isUpdatingFromCode = false;
                }
            });

            // KEY_TYPED: apenas consumir para impedir processamento nativo (sem insercao)
            comboBox.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, keyEvent -> {
                if (" ".equals(keyEvent.getCharacter())) {
                    keyEvent.consume();
                }
            });
        }

        // =====================================================================
        // LISTENER DE TEXTO: filtra com debounce (#037)
        // =====================================================================
        final javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdatingFromCode || isFiltering[0]) return;
            if (userNavigated[0]) return;

            final String textoDigitado;

            // DETECÇÃO ANTI-SKIN (só para passageiro):
            // Se o texto pulou de "JOAO" para "JOAO GUILHERME" (>1 char e é item da lista),
            // o skin interferiu. Reverter para oldValue + espaço.
            if (comboBox == cmbPassageiroAuto && oldValue != null && newValue != null
                    && newValue.length() > oldValue.length() + 1) {
                boolean skinInterferiu = false;
                for (String item : itemsOriginal) {
                    if (item.equalsIgnoreCase(newValue)) {
                        skinInterferiu = true;
                        break;
                    }
                }
                if (skinInterferiu) {
                    String textoCorreto = oldValue.endsWith(" ") ? oldValue : oldValue + " ";
                    textoDigitado = textoCorreto;
                    isUpdatingFromCode = true;
                    comboBox.getEditor().setText(textoCorreto);
                    comboBox.getEditor().positionCaret(textoCorreto.length());
                    isUpdatingFromCode = false;
                } else {
                    textoDigitado = newValue;
                }
            } else {
                textoDigitado = (newValue != null) ? newValue : "";
            }

            // #037: debounce — adia filtragem 150ms para evitar lag com listas grandes
            debounce.setOnFinished(evt -> {
                if (isUpdatingFromCode || isFiltering[0]) return;

                isFiltering[0] = true;
                try {
                    comboBox.getSelectionModel().clearSelection();

                    if (textoDigitado.isEmpty()) {
                        comboBox.setItems(FXCollections.observableArrayList(itemsOriginal));
                    } else {
                        String filtro = textoDigitado.toLowerCase();
                        ObservableList<String> filtrados = FXCollections.observableArrayList();
                        for (String item : itemsOriginal) {
                            if (item.toLowerCase().contains(filtro)) {
                                filtrados.add(item);
                            }
                        }
                        comboBox.setItems(filtrados);
                    }

                    if (!textoDigitado.equals(comboBox.getEditor().getText())) {
                        isUpdatingFromCode = true;
                        comboBox.getEditor().setText(textoDigitado);
                        comboBox.getEditor().positionCaret(textoDigitado.length());
                        isUpdatingFromCode = false;
                    } else {
                        comboBox.getEditor().positionCaret(textoDigitado.length());
                    }

                } finally {
                    isFiltering[0] = false;
                }

                if (comboBox.isFocused() || comboBox.getEditor().isFocused()) {
                    if (!comboBox.getItems().isEmpty()) {
                        if (!comboBox.isShowing()) {
                            comboBox.show();
                        }
                    } else {
                        comboBox.hide();
                    }
                }
            });
            debounce.playFromStart();
        });

        // =====================================================================
        // ENTER/TAB
        // =====================================================================
        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                String selectedItem = comboBox.getSelectionModel().getSelectedItem();

                // Para o passageiro: só preencher se o usuário navegou com setas
                // Para os outros combos: comportamento normal (qualquer seleção vale)
                boolean deveUsarSelecao;
                if (comboBox == cmbPassageiroAuto) {
                    deveUsarSelecao = userNavigated[0] && selectedItem != null && !selectedItem.isEmpty();
                } else {
                    deveUsarSelecao = selectedItem != null && !selectedItem.isEmpty();
                }

                if (deveUsarSelecao) {
                    isUpdatingFromCode = true;
                    comboBox.getEditor().setText(selectedItem);
                    comboBox.setValue(selectedItem);
                    isUpdatingFromCode = false;
                    if (customAction && comboBox == cmbPassageiroAuto) {
                        preencherPassageiroPorNome(selectedItem);
                    }
                } else if (comboBox != cmbPassageiroAuto) {
                    // Para combos nao-passageiro: commitar o texto digitado como valor
                    String editorText = comboBox.getEditor().getText();
                    if (editorText != null && !editorText.trim().isEmpty()) {
                        isUpdatingFromCode = true;
                        comboBox.setValue(editorText.trim());
                        isUpdatingFromCode = false;
                    }
                } else if (customAction && comboBox == cmbPassageiroAuto) {
                    String textoDigitado = comboBox.getEditor().getText();
                    if (textoDigitado != null && !textoDigitado.trim().isEmpty()) {
                        String nomeTrimmed = textoDigitado.trim();
                        boolean existe = todosOsPassageiros.stream()
                            .anyMatch(p -> p.getNome().equalsIgnoreCase(nomeTrimmed));
                        if (existe) {
                            preencherPassageiroPorNome(nomeTrimmed);
                        } else {
                            Alert alertNovo = new Alert(AlertType.CONFIRMATION);
                            alertNovo.setTitle("Passageiro não encontrado");
                            alertNovo.setHeaderText("\"" + nomeTrimmed + "\" não está cadastrado.");
                            alertNovo.setContentText("Deseja continuar com este nome como novo passageiro?");
                            ButtonType btnSimNovo = new ButtonType("Sim, continuar", ButtonBar.ButtonData.YES);
                            ButtonType btnNaoNovo = new ButtonType("Não, redigitar", ButtonBar.ButtonData.NO);
                            alertNovo.getButtonTypes().setAll(btnSimNovo, btnNaoNovo);
                            java.util.Optional<ButtonType> resposta = alertNovo.showAndWait();
                            if (resposta.isEmpty() || resposta.get() != btnSimNovo) {
                                isUpdatingFromCode = true;
                                comboBox.getEditor().clear();
                                comboBox.getSelectionModel().clearSelection();
                                isUpdatingFromCode = false;
                                comboBox.requestFocus();
                            }
                        }
                    }
                }
                comboBox.hide();
                userNavigated[0] = false;
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                comboBox.hide();
                userNavigated[0] = false;
                event.consume();
            } else if (!event.getCode().isModifierKey() && !event.getCode().isFunctionKey()
                    && event.getCode() != KeyCode.DOWN && event.getCode() != KeyCode.UP) {
                // Qualquer tecla que nao seja seta/modificador/funcao: resetar navegacao
                userNavigated[0] = false;
            }
        });

        // =====================================================================
        // CLIQUE no dropdown: preencher dados do passageiro
        // =====================================================================
        if (customAction && comboBox == cmbPassageiroAuto) {
            comboBox.skinProperty().addListener((obsSkin, oldSkin, newSkin) -> {
                if (newSkin instanceof javafx.scene.control.skin.ComboBoxListViewSkin) {
                    @SuppressWarnings("unchecked")
                    javafx.scene.control.ListView<String> listView =
                        (javafx.scene.control.ListView<String>)
                        ((javafx.scene.control.skin.ComboBoxListViewSkin<?>) newSkin).getPopupContent();
                    listView.setOnMouseClicked(mouseEvent -> {
                        String selecionado = comboBox.getSelectionModel().getSelectedItem();
                        if (selecionado != null && !selecionado.isEmpty() && !isUpdatingFromCode) {
                            isUpdatingFromCode = true;
                            comboBox.getEditor().setText(selecionado);
                            comboBox.getEditor().positionCaret(selecionado.length());
                            isUpdatingFromCode = false;
                            comboBox.hide();
                            preencherPassageiroPorNome(selecionado);
                        }
                    });
                }
            });
        }

        // Ao perder foco: fechar dropdown e commitar valor
        comboBox.getEditor().focusedProperty().addListener((obs4, wasFocused, isFocused) -> {
            if (!isFocused) {
                comboBox.hide();
                // Commitar o texto do editor como valor do ComboBox
                String editorText = comboBox.getEditor().getText();
                if (editorText != null && !editorText.trim().isEmpty() && !isUpdatingFromCode) {
                    isUpdatingFromCode = true;
                    comboBox.setValue(editorText.trim());
                    isUpdatingFromCode = false;
                }
            }
        });
        comboBox.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
            if (!isFocused && !comboBox.getEditor().isFocused()) {
                comboBox.hide();
            }
        });
    }

    private void preencherPassageiroPorNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            passageiroEmEdicao = null;
            limparCamposPassageiroGUI(false);
            return;
        }
        Optional<Passageiro> passageiro = todosOsPassageiros.stream().filter(p -> p.getNome().equalsIgnoreCase(nome.trim())).findFirst();
        passageiro.ifPresent(p -> {
            passageiroEmEdicao = p;
            txtNumeroDoc.setText(p.getNumeroDoc());
            if (p.getDataNascimento() != null) {
                txtNascimentoMask.setText(dateFormatter.format(p.getDataNascimento()));
                txtIdade.setText(String.valueOf(Period.between(p.getDataNascimento(), LocalDate.now()).getYears()));
            }
            cmbSexo.getSelectionModel().select(p.getSexo());
            cmbNacionalidade.getSelectionModel().select(p.getNacionalidade());
            cmbTipoDoc.getSelectionModel().select(p.getTipoDoc());
        });
    }

    private void configurarEstadoInicialDaTela() {
        limparTodosOsCamposGUI();
        habilitarCamposParaNovaVenda(false);
        if (btnNovo != null) btnNovo.setDisable(true);
        configurarEstadoBotoesGerais();

        if (viagemSelecionada != null) {
            btnNovo.setDisable(false);
            carregarPassagensNaTabela();
        } else {
            if (tablePassagens != null) tablePassagens.getItems().clear();
            if (txtTotalPassageiros != null) txtTotalPassageiros.setText("0");
        }
    }

    private void configurarEstadoBotoesGerais() {
        if (btnFiltrar != null) btnFiltrar.setDisable(false);
        if (btnImprimirLista != null) btnImprimirLista.setDisable(false);
        if (btnRelatorio != null) btnRelatorio.setDisable(false);
        if (btnSair != null) btnSair.setDisable(false);

        if (btnEditar != null) btnEditar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
        if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(true);

        if (tablePassagens != null && tablePassagens.getSelectionModel() != null) {
            tablePassagens.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                boolean haSelecao = (newV != null);
                // >>> CORREÃ‡ÃƒO DE EDIÃ‡ÃƒO <<<
                // Se houver selecao, SEMPRE habilita os botoes de edicao, independente do estado anterior
                if (haSelecao) {
                    if (btnEditar != null) btnEditar.setDisable(false);
                    if (btnExcluir != null) btnExcluir.setDisable(false);
                    if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(false);
                } else {
                    if (btnEditar != null) btnEditar.setDisable(true);
                    if (btnExcluir != null) btnExcluir.setDisable(true);
                    if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(true);
                }
            });
        }
    }

    private void preencherPassageiroPorDoc(String doc) {
        if (doc == null || doc.trim().isEmpty()) {
            passageiroEmEdicao = null;
            return;
        }
        try {
            Passageiro p = passageiroDAO.buscarPorDoc(doc.trim());
            if (p != null) {
                preencherCamposPassageiro(p);
                if (cmbPassageiroAuto != null) {
                    isUpdatingFromCode = true;
                    cmbPassageiroAuto.getEditor().setText(p.getNome());
                    isUpdatingFromCode = false;
                }
            } else {
                // >>> CORREÃ‡ÃƒO: DOC NÃƒO ENCONTRADO <<<
                // Se nao achou o doc, NÃƒO limpa o nome. Apenas assume que e um novo doc para o passageiro atual (ou novo).
                // Mantem passageiroEmEdicao como null se estava null (novo), ou mantem o passageiro se estava editando e mudou o doc.
                // Na duvida, para novo cadastro, passageiroEmEdicao fica null.
            }
        } catch (Exception e) {
            AppLogger.warn("VenderPassagemController", "VenderPassagemController.preencherPassageiroPorDoc: erro ao buscar passageiro por documento — " + e.getMessage());
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
        }
    }

    private void preencherCamposPassageiro(Passageiro p) {
        if (p == null) return;
        passageiroEmEdicao = p;
        if (cmbPassageiroAuto != null) {
            isUpdatingFromCode = true;
            cmbPassageiroAuto.getEditor().setText(p.getNome());
            isUpdatingFromCode = false;
        }
        if (txtNumeroDoc != null) txtNumeroDoc.setText(p.getNumeroDoc());
        if (dpDataNascimento != null) dpDataNascimento.setValue(p.getDataNascimento());
        if (cmbTipoDoc != null) cmbTipoDoc.setValue(p.getTipoDoc());
        if (cmbNacionalidade != null) cmbNacionalidade.setValue(p.getNacionalidade());
        if (cmbSexo != null) cmbSexo.setValue(p.getSexo());
        calcularIdade();
    }

    private void limparCamposPassageiroGUI(boolean limparNome) {
        boolean wasUpdating = isUpdatingFromCode;
        isUpdatingFromCode = true;
        try {
            if (limparNome && cmbPassageiroAuto != null) {
                if (cmbPassageiroAuto.isShowing()) cmbPassageiroAuto.hide();
                cmbPassageiroAuto.getSelectionModel().clearSelection();
                if (cmbPassageiroAuto.getEditor() != null) cmbPassageiroAuto.getEditor().clear();
            }
            if (txtNumeroDoc != null) txtNumeroDoc.clear();
            if (dpDataNascimento != null) dpDataNascimento.setValue(null);
            if (txtNascimentoMask != null) txtNascimentoMask.clear();
            if (txtIdade != null) txtIdade.clear();
            if (cmbTipoDoc != null) {
                if (cmbTipoDoc.isShowing()) cmbTipoDoc.hide();
                cmbTipoDoc.getSelectionModel().clearSelection();
                if (cmbTipoDoc.getEditor() != null) cmbTipoDoc.getEditor().clear();
            }
            if (cmbNacionalidade != null) {
                if (cmbNacionalidade.isShowing()) cmbNacionalidade.hide();
                cmbNacionalidade.getSelectionModel().clearSelection();
                if (cmbNacionalidade.getEditor() != null) cmbNacionalidade.getEditor().clear();
            }
            if (cmbSexo != null) {
                if (cmbSexo.isShowing()) cmbSexo.hide();
                cmbSexo.getSelectionModel().clearSelection();
                if (cmbSexo.getEditor() != null) cmbSexo.getEditor().clear();
            }
        } finally {
            isUpdatingFromCode = wasUpdating;
        }
    }

    private void calcularIdade() {
        if (dpDataNascimento != null && dpDataNascimento.getValue() != null) {
            int idade = Period.between(dpDataNascimento.getValue(), LocalDate.now()).getYears();
            if (txtIdade != null) txtIdade.setText(String.valueOf(idade));
        } else {
            if (txtIdade != null) txtIdade.clear();
        }
    }

    private void carregarValoresTarifaAutomatica() {
        String selectedRotaStr = cmbRota != null ? cmbRota.getSelectionModel().getSelectedItem() : null;
        String selectedTipoPassagemStr = cmbTipoPassagemAux != null ? cmbTipoPassagemAux.getSelectionModel().getSelectedItem() : null;
        if (selectedRotaStr == null || selectedRotaStr.trim().isEmpty() || selectedTipoPassagemStr == null || selectedTipoPassagemStr.trim().isEmpty()) {
            limparCamposTarifa();
            return;
        }
        try {
            String[] rotaParts = selectedRotaStr.split(" - ");
            String origem = rotaParts[0].trim();
            String destino = (rotaParts.length > 1) ? rotaParts[1].trim() : "";
            Integer idRotaInteger = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origem, destino);
            long idRota = (idRotaInteger != null) ? idRotaInteger.longValue() : 0;
            Integer idTipoPassagem = auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", selectedTipoPassagemStr);
            if (idRota == 0 || idTipoPassagem == null) {
                limparCamposTarifa();
                return;
            }
            Tarifa tarifaEncontrada = tarifaDAO.buscarTarifaPorRotaETipo(idRota, idTipoPassagem);
            if (tarifaEncontrada != null) {
                if (txtAlimentacao != null) txtAlimentacao.setText(String.format("%,.2f", tarifaEncontrada.getValorAlimentacao()));
                if (txtTransporte != null) txtTransporte.setText(String.format("%,.2f", tarifaEncontrada.getValorTransporte()));
                if (txtCargas != null) txtCargas.setText(String.format("%,.2f", tarifaEncontrada.getValorCargas()));
                if (txtDescontoTarifa != null) txtDescontoTarifa.setText(String.format("%,.2f", tarifaEncontrada.getValorDesconto()));
            } else {
                limparCamposTarifa();
            }
        } catch (Exception e) {
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
            limparCamposTarifa();
        } finally {
            calcularValoresPassagem();
        }
    }

    private void setupCalculoTotalPassagem() {
        List.of(txtAlimentacao, txtTransporte, txtCargas, txtDescontoTarifa, txtDesconto).forEach(field -> {
            if (field != null) field.textProperty().addListener((obs, oldVal, newVal) -> calcularValoresPassagem());
        });
    }

    private void calcularValoresPassagem() {
        BigDecimal alimentacao = MoneyUtil.parseBigDecimal(txtAlimentacao.getText());
        BigDecimal transporte = MoneyUtil.parseBigDecimal(txtTransporte.getText());
        BigDecimal cargas = MoneyUtil.parseBigDecimal(txtCargas.getText());
        BigDecimal descontoTarifa = MoneyUtil.parseBigDecimal(txtDescontoTarifa.getText());
        BigDecimal descontoGeral = MoneyUtil.parseBigDecimal(txtDesconto.getText());

        BigDecimal valorTotalCalculado = alimentacao.add(transporte).add(cargas).subtract(descontoTarifa);
        if (valorTotalCalculado.compareTo(BigDecimal.ZERO) < 0) valorTotalCalculado = BigDecimal.ZERO;

        BigDecimal valorAPagarCalculado = valorTotalCalculado.subtract(descontoGeral);
        if (valorAPagarCalculado.compareTo(BigDecimal.ZERO) < 0) valorAPagarCalculado = BigDecimal.ZERO;

        txtTotal.setText(String.format("%,.2f", valorTotalCalculado));
        txtAPagar.setText(String.format("%,.2f", valorAPagarCalculado));
    }

    private void adicionarListenerAoCampoPesquisar() {
        if (txtPesquisar != null) {
            txtPesquisar.textProperty().addListener((obs, oldText, newText) -> {
                if (newText == null || newText.trim().isEmpty()) {
                    if (tablePassagens != null) tablePassagens.setItems(passagensDaViagemAtual);
                    if (txtTotalPassageiros != null) txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));
                } else {
                    handleFiltrar(null);
                }
            });

            txtPesquisar.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleFiltrar(null);
                    event.consume();
                }
            });
        }
    }

    private void adicionarListenerDeSelecaoNaTabela() {
        if (tablePassagens != null) {
            tablePassagens.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    if (passagemEmEdicao == null && (btnNovo != null && !btnNovo.isDisabled())) {
                        if (btnEditar != null) btnEditar.setDisable(false);
                        if (btnExcluir != null) btnExcluir.setDisable(false);
                        if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(false);
                    }
                } else {
                    if (passagemEmEdicao == null && (btnNovo != null && !btnNovo.isDisabled())) {
                        if (btnEditar != null) btnEditar.setDisable(true);
                        if (btnExcluir != null) btnExcluir.setDisable(true);
                        if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(true);
                    }
                }
            });
        }
    }

    private void configurarNavegacaoEntreCampos() {
        List<Node> focusableNodes = new ArrayList<>();
        
        if (cmbPassageiroAuto != null) focusableNodes.add(cmbPassageiroAuto);
        if (txtNumeroDoc != null) focusableNodes.add(txtNumeroDoc);
        if (cmbTipoDoc != null) focusableNodes.add(cmbTipoDoc);
        if (cmbNacionalidade != null) focusableNodes.add(cmbNacionalidade);
        if (txtNascimentoMask != null) focusableNodes.add(txtNascimentoMask);
        if (cmbSexo != null) focusableNodes.add(cmbSexo);
        if (cmbViagem != null) focusableNodes.add(cmbViagem);
        if (cmbRota != null) focusableNodes.add(cmbRota);
        if (cmbAcomodacao != null) focusableNodes.add(cmbAcomodacao);
        if (cmbTipoPassagemAux != null) focusableNodes.add(cmbTipoPassagemAux);
        if (cmbAgenteAux != null) focusableNodes.add(cmbAgenteAux);
        if (txtRequisicao != null) focusableNodes.add(txtRequisicao);
        if (txtAlimentacao != null) focusableNodes.add(txtAlimentacao);
        if (txtTransporte != null) focusableNodes.add(txtTransporte);
        if (txtCargas != null) focusableNodes.add(txtCargas);
        if (txtDescontoTarifa != null) focusableNodes.add(txtDescontoTarifa);
        if (txtDesconto != null) focusableNodes.add(txtDesconto);
        if (btnSalvar != null) focusableNodes.add(btnSalvar);

        for (int i = 0; i < focusableNodes.size(); i++) {
            final int currentIndex = i;
            final int nextIndex = (i + 1) % focusableNodes.size();
            Node currentNode = focusableNodes.get(i);

            if (currentNode instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> combo = (ComboBox<String>) currentNode;
                if (combo != cmbPassageiroAuto) {
                    combo.getEditor().setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ENTER && !combo.isShowing()) {
                            // Commitar valor do editor antes de mover foco
                            String editorText = combo.getEditor().getText();
                            if (editorText != null && !editorText.trim().isEmpty()) {
                                isUpdatingFromCode = true;
                                combo.setValue(editorText.trim());
                                isUpdatingFromCode = false;
                            }
                            event.consume();
                            Platform.runLater(() -> focusableNodes.get(nextIndex).requestFocus());
                        }
                    });
                }
            } else if (currentNode instanceof TextField) {
                currentNode.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        event.consume();
                        Platform.runLater(() -> focusableNodes.get(nextIndex).requestFocus());
                    }
                });
            }
        }
        
        if (cmbPassageiroAuto != null) {
            Platform.runLater(() -> cmbPassageiroAuto.requestFocus());
        }
    }

    private void configurarAtalhosTeclado() {
        if (rootPane == null) return;

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    switch (event.getCode()) {
                        case F1:
                            if (btnSalvar != null && !btnSalvar.isDisabled()) { handleSalvar(null); event.consume(); }
                            break;
                        case F2:
                            if (btnNovo != null && !btnNovo.isDisabled()) { handleNovo(null); event.consume(); }
                            break;
                        case F3:
                            if (btnEditar != null && !btnEditar.isDisabled()) { handleEditar(null); event.consume(); }
                            break;
                        case F4:
                            if (btnExcluir != null && !btnExcluir.isDisabled()) { handleExcluir(null); event.consume(); }
                            break;
                        case F5:
                            if (btnCancelar != null && !btnCancelar.isDisabled()) { handleCancelar(null); event.consume(); }
                            break;
                        case F6:
                            if (btnImprimirBilhete != null && !btnImprimirBilhete.isDisabled()) { handleImprimirBilhete(null); event.consume(); }
                            break;
                        case F7:
                            if (btnImprimirLista != null && !btnImprimirLista.isDisabled()) { handleImprimirLista(null); event.consume(); }
                            break;
                        case F8:
                            if (btnRelatorio != null && !btnRelatorio.isDisabled()) { handleRelatorio(null); event.consume(); }
                            break;
                        case ESCAPE:
                            if (btnSair != null && !btnSair.isDisabled()) { handleSair(null); event.consume(); }
                            break;
                        default: break;
                    }
                });
            }
        });
    }

    private void configurarTamanhoMinimoJanela() {
        if (rootPane == null) return;

        rootPane.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWindow, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage) {
                        Stage stage = (Stage) newWindow;
                        stage.setMinWidth(1024);
                        stage.setMinHeight(650);
                    }
                });
            }
        });
    }

    @FXML private void handleNovo(ActionEvent event) {
        if (viagemSelecionada == null) {
            AlertHelper.show(AlertType.INFORMATION, "Viagem Necess\u00e1ria", "Nenhuma viagem foi informada. Verifique se existe uma viagem 'Atual'.");
            return;
        }
        limparCamposParaNovaVendaAutomatica();
        habilitarCamposParaNovaVenda(true);
        if (btnNovo != null) btnNovo.setDisable(true);
        if (cmbPassageiroAuto != null) cmbPassageiroAuto.requestFocus();
    }

    @FXML private void handleCancelar(ActionEvent event) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Cancelamento");
        alert.setHeaderText("Cancelar opera\u00e7\u00e3o?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            configurarEstadoInicialDaTela();
            configurarTelaComViagemAtiva();
            if (tablePassagens != null) tablePassagens.setDisable(false);
        }
    }

    @FXML
    private void handleFiltrar(ActionEvent event) {
        String modo = (cmbPesquisarModo != null) ? cmbPesquisarModo.getSelectionModel().getSelectedItem() : null;
        String texto = (txtPesquisar != null) ? txtPesquisar.getText() : "";

        if (modo == null || modo.isEmpty()) return;

        if (texto == null || texto.trim().isEmpty()) {
            if (tablePassagens != null) tablePassagens.setItems(passagensDaViagemAtual);
            if (txtTotalPassageiros != null) txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));
            return;
        }

        List<Passagem> filtradas = filtrarPassagensLocalmente(modo, texto);

        if (tablePassagens != null) {
            tablePassagens.setItems(FXCollections.observableArrayList(filtradas));
        }
        if (txtTotalPassageiros != null) {
            txtTotalPassageiros.setText(String.valueOf(filtradas.size()));
        }
    }

    private List<Passagem> filtrarPassagensLocalmente(String modo, String texto) {
        String termo = texto.trim();
        String termoUpper = termo.toUpperCase();

        return passagensDaViagemAtual.stream().filter(p -> {
            if (p == null) return false;

            switch (modo) {
                case "N\u00famero Bilhete":
                    String num = String.valueOf(p.getNumBilhete());
                    return num != null && num.contains(termo);

                case "Passageiro":
                    String nome = p.getNomePassageiro();
                    return nome != null && nome.toUpperCase().contains(termoUpper);

                case "N\u00ba Documento":
                    String doc = p.getNumeroDoc();
                    return doc != null && doc.toUpperCase().contains(termoUpper);

                case "Data Partida":
                    LocalDate data = p.getDataViagem();
                    if (data == null) return false;
                    String dataStr = dateFormatter.format(data);
                    return dataStr.contains(termo);

                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }

    // =======================================================================
    // <<< MÃ‰TODOS DE EDIÃ‡ÃƒO, EXCLUSÃƒO E IMPRESSÃƒO >>>
    // =======================================================================

    @FXML
    private void handleEditar(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.show(AlertType.WARNING, "Sele\u00e7\u00e3o", "Selecione uma passagem na tabela para editar.");
            return;
        }

        this.passagemEmEdicao = selected;
        isUpdatingFromCode = true;

        try {
            habilitarCamposParaNovaVenda(true);
            btnNovo.setDisable(true);

            txtNumBilhete.setText(String.valueOf(selected.getNumBilhete()));

            if (selected.getNomePassageiro() != null) {
                cmbPassageiroAuto.getEditor().setText(selected.getNomePassageiro());
                preencherPassageiroPorNome(selected.getNomePassageiro());
            }

            String rotaDesejada = selected.getOrigem() + " - " + selected.getDestino();
            boolean rotaAchada = false;
            for (String item : cmbRota.getItems()) {
                if (item.equalsIgnoreCase(rotaDesejada)) {
                    cmbRota.getSelectionModel().select(item);
                    rotaAchada = true;
                    break;
                }
            }
            if (!rotaAchada && selected.getOrigem() != null) {
                cmbRota.getEditor().setText(rotaDesejada);
            }

            if (selected.getTipoPassagemAux() != null) cmbTipoPassagemAux.getSelectionModel().select(selected.getTipoPassagemAux());
            if (selected.getAcomodacao() != null) cmbAcomodacao.getSelectionModel().select(selected.getAcomodacao());
            if (selected.getAgenteAux() != null) cmbAgenteAux.getSelectionModel().select(selected.getAgenteAux());

            txtRequisicao.setText(selected.getRequisicao() == null ? "" : selected.getRequisicao());
            txtAlimentacao.setText(String.format("%,.2f", selected.getValorAlimentacao()));
            txtTransporte.setText(String.format("%,.2f", selected.getValorTransporte()));
            txtCargas.setText(String.format("%,.2f", selected.getValorCargas()));
            txtDescontoTarifa.setText(String.format("%,.2f", selected.getValorDescontoTarifa()));
            txtDesconto.setText(String.format("%,.2f", selected.getValorDesconto()));

            calcularValoresPassagem();

            cmbPassageiroAuto.requestFocus();

        } catch (Exception e) {
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao carregar dados para edi\u00e7\u00e3o: " + e.getMessage());
            configurarEstadoInicialDaTela();
        } finally {
            isUpdatingFromCode = false;
        }
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.show(AlertType.WARNING, "Sele\u00e7\u00e3o", "Selecione uma passagem para excluir.");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Excluir Passagem");
        alert.setHeaderText("Deseja realmente excluir a passagem do bilhete " + selected.getNumBilhete() + "?");
        alert.setContentText("Essa a\u00e7\u00e3o n\u00e3o poder\u00e1 ser desfeita.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean excluido = passagemDAO.excluir(selected.getId());
                if (excluido) {
                    passagensDaViagemAtual.remove(selected);
                    tablePassagens.getItems().remove(selected);
                    txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));

                    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Passagem exclu\u00edda com sucesso.");
                    limparTodosOsCamposGUI();
                } else {
                    AlertHelper.show(AlertType.ERROR, "Erro", "N\u00e3o foi poss\u00edvel excluir a passagem.");
                }
            } catch (Exception e) {
                AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImprimirBilhete(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.show(AlertType.WARNING, "Sele\u00e7\u00e3o", "Selecione uma passagem para imprimir o bilhete.");
            return;
        }

        if (selected.getIdPassageiro() != null) {
            try {
                Passageiro pCompleto = passageiroDAO.buscarPorId(selected.getIdPassageiro());
                if(pCompleto != null) {
                    selected.setDataNascimento(pCompleto.getDataNascimento());
                    selected.setSexo(pCompleto.getSexo());
                    selected.setNacionalidade(pCompleto.getNacionalidade());
                    if (pCompleto.getDataNascimento() != null) {
                        int idade = Period.between(pCompleto.getDataNascimento(), LocalDate.now()).getYears();
                        selected.setIdade(idade);
                    }
                }
            } catch(Exception e) {
                AppLogger.warn("VenderPassagemController", "Erro ao buscar dados complementares do passageiro: " + e.getMessage());
            }
        }

        PassagemPrintHelper.EmpresaInfo empInfo = new PassagemPrintHelper.EmpresaInfo(
                empEmbarcacao, empProprietario, empCnpj, empTelefone, empFrase, empRecomendacoes, empPathLogo);
        java.time.LocalDate dataChegada = (this.viagemSelecionada != null) ? this.viagemSelecionada.getDataChegada() : null;
        PassagemPrintHelper.mostrarPreviewImpressao(selected, empInfo, dataChegada, rootPane.getScene().getWindow());
    }

    @FXML private void handleImprimirLista(ActionEvent event) {
        abrirNovaJanela("/gui/ListarPassageirosViagem.fxml", "Lista de Passageiros da Viagem");
    }

    @FXML private void handleRelatorio(ActionEvent event) {
        abrirNovaJanela("/gui/RelatorioPassagens.fxml", "Relatorio de Passagens");
    }

    @FXML private void handleSair(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }

    private void abrirNovaJanela(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent pane = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(pane));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());

            if (loader.getController() instanceof VenderPassagemController) {
                ((VenderPassagemController) loader.getController()).setViagemAtiva(this.viagemSelecionada);
            }
            stage.show();
        } catch (IOException e) {
            AppLogger.error("VenderPassagemController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro ao Abrir Tela", "Nao foi possivel carregar a tela: " + fxmlPath);
        }
    }

    private void limparCamposTarifa() {
        if (txtAlimentacao != null) txtAlimentacao.setText("0,00");
        if (txtTransporte != null) txtTransporte.setText("0,00");
        if (txtCargas != null) txtCargas.setText("0,00");
        if (txtDescontoTarifa != null) txtDescontoTarifa.setText("0,00");
        if (txtTotal != null) txtTotal.setText("0,00");
        if (txtDesconto != null) txtDesconto.setText("0,00");
        if (txtAPagar != null) txtAPagar.setText("0,00");
    }

    private void limparCamposParaNovaVendaAutomatica() {
        isUpdatingFromCode = true;
        try {
            fecharTodosOsDropdowns();
            limparCamposPassageiroGUI(true);
            limparCamposTarifa();
            if (txtRequisicao != null) txtRequisicao.clear();
            if (cmbTipoPassagemAux != null) {
                cmbTipoPassagemAux.getSelectionModel().clearSelection();
                cmbTipoPassagemAux.getEditor().clear();
            }
            if (cmbAgenteAux != null) {
                cmbAgenteAux.getSelectionModel().clearSelection();
                cmbAgenteAux.getEditor().clear();
            }
            if (cmbAcomodacao != null) {
                cmbAcomodacao.getSelectionModel().clearSelection();
                cmbAcomodacao.getEditor().clear();
            }
            if (cmbRota != null) {
                cmbRota.getSelectionModel().clearSelection();
                cmbRota.getEditor().clear();
            }
            passagemEmEdicao = null;
            passageiroEmEdicao = null;
            try {
                if (txtNumBilhete != null) txtNumBilhete.setText(String.valueOf(passagemDAO.obterProximoBilhete()));
            } catch (Exception e) {
                if (txtNumBilhete != null) txtNumBilhete.setText("Erro");
            }
            if (viagemSelecionada != null) {
                if (dpDataViagem != null) dpDataViagem.setValue(viagemSelecionada.getDataViagem());
                if (txtHorario != null) txtHorario.setText(viagemSelecionada.getDescricaoHorarioSaida());
            }
            if (tablePassagens != null) tablePassagens.getSelectionModel().clearSelection();
            if (btnEditar != null) btnEditar.setDisable(true);
            if (btnExcluir != null) btnExcluir.setDisable(true);
            if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(true);
        } finally {
            isUpdatingFromCode = false;
        }
    }
    
    private void fecharTodosOsDropdowns() {
        List<ComboBox<String>> comboBoxes = List.of(
            cmbPassageiroAuto, cmbRota, cmbTipoPassagemAux, cmbViagem,
            cmbSexo, cmbTipoDoc, cmbNacionalidade, cmbAgenteAux, cmbAcomodacao
        );
        
        for (ComboBox<String> cmb : comboBoxes) {
            if (cmb != null && cmb.isShowing()) {
                cmb.hide();
            }
        }
    }

    private void limparTodosOsCamposGUI() {
        isUpdatingFromCode = true;
        try {
            limparCamposPassageiroGUI(true);
            limparCamposTarifa();
            if (txtNumBilhete != null) {
                txtNumBilhete.clear();
                txtNumBilhete.setDisable(true);
            }
            if (txtRequisicao != null) txtRequisicao.clear();
            if (txtPesquisar != null) txtPesquisar.clear();
            passagemEmEdicao = null;
            passageiroEmEdicao = null;
            if (tablePassagens != null) tablePassagens.getSelectionModel().clearSelection();
            if (btnEditar != null) btnEditar.setDisable(true);
            if (btnExcluir != null) btnExcluir.setDisable(true);
            if (btnImprimirBilhete != null) btnImprimirBilhete.setDisable(true);

            List<ComboBox<?>> comboBoxesExistentes = List.of(cmbPassageiroAuto, cmbRota, cmbTipoPassagemAux, cmbSexo, cmbTipoDoc, cmbNacionalidade, cmbAgenteAux, cmbAcomodacao);
            comboBoxesExistentes.forEach(cmb -> {
                if (cmb != null) {
                    cmb.getSelectionModel().clearSelection();
                }
            });
        } finally {
            isUpdatingFromCode = false;
        }
    }

    private void habilitarCamposParaNovaVenda(boolean enable) {
        cmbPassageiroAuto.setDisable(!enable);
        txtNumeroDoc.setDisable(!enable);
        dpDataNascimento.setDisable(!enable);
        txtNascimentoMask.setDisable(!enable);
        cmbTipoDoc.setDisable(!enable);
        cmbNacionalidade.setDisable(!enable);
        cmbSexo.setDisable(!enable);
        cmbTipoPassagemAux.setDisable(!enable);
        cmbAcomodacao.setDisable(!enable);
        cmbAgenteAux.setDisable(!enable);
        txtRequisicao.setDisable(!enable);
        txtAlimentacao.setDisable(!enable);
        txtTransporte.setDisable(!enable);
        txtCargas.setDisable(!enable);
        txtDescontoTarifa.setDisable(!enable);
        txtDesconto.setDisable(!enable);

        if (cmbViagem != null) cmbViagem.setDisable(true);
        if (dpDataViagem != null) dpDataViagem.setDisable(true);
        if (txtDataViagemMask != null) txtDataViagemMask.setDisable(true);
        if (txtHorario != null) txtHorario.setDisable(true);

        cmbRota.setDisable(!enable);
        btnSalvar.setDisable(!enable);
        btnCancelar.setDisable(!enable);
        if (cmbPesquisarModo != null) cmbPesquisarModo.setDisable(enable);
        if (txtPesquisar != null) txtPesquisar.setDisable(enable);
        if (btnFiltrar != null) btnFiltrar.setDisable(enable);
        
        // >>> TABELA SEMPRE ATIVA <<<
        if (tablePassagens != null) tablePassagens.setDisable(false);
    }


}