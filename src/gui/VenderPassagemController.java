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

// Imports para Impressão AWT (Térmica) e Rotação de Imagem
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

    // DAOs e Variáveis de Controle
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

    // Variáveis para Dados da Empresa
    private String empPathLogo = "";
    private String empCompanhia = "EMPRESA DE NAVEGAÇÃO";
    private String empEmbarcacao = "F/B DEUS DE ALIANÇA V"; 
    private String empProprietario = "Francisco Cintra"; 
    private String empCnpj = "";
    private String empTelefone = "";
    private String empEndereco = "";
    private String empFrase = "Jesus não desiste de você"; 
    private String empRecomendacoes = "Sem recomendações cadastradas.";

    // =======================================================================
    // <<< INITIALIZE E CONFIGURAÇÕES >>>
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
        carregarConfiguracaoEmpresa();

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

        // >>> CORREÇÃO CRÍTICA DO CAMPO DOCUMENTO <<<
        // Se sair do campo e não encontrar o Doc, NÃO LIMPA O NOME, apenas aceita como novo Doc.
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

        new Thread(() -> {
            try {
                List<Passageiro> listaP = passageiroDAO.listarTodos();
                
                List<String> rotasStrings = new ArrayList<>();
                try {
                    List<model.Rota> rotasObjects = new dao.RotaDAO().listarTodasAsRotasComoObjects();
                    if (rotasObjects != null) {
                        for (model.Rota r : rotasObjects) rotasStrings.add(r.toString());
                    }
                } catch (Exception e) {}
                
                List<String> tipoPassagem = auxiliaresDAO.listarPassagemAux();
                List<String> sexos = auxiliaresDAO.listarSexo();
                List<String> tiposDoc = auxiliaresDAO.listarTipoDoc();
                List<String> nac = auxiliaresDAO.listarNacionalidade();
                List<String> agentes = auxiliaresDAO.listarAgenteAux();
                List<String> acomodacoes = auxiliaresDAO.listarAcomodacao();

                listaViagensCarregadas = new ArrayList<>();
                List<String> viagensFormatadas = new ArrayList<>();
                
                String sql = "SELECT id_viagem, data_viagem, data_chegada, descricao, id_horario_saida, is_atual FROM viagens ORDER BY data_viagem DESC";
                try (Connection conn = ConexaoBD.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
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
                } catch(Exception e) {
                    System.err.println("Erro ao carregar viagens: " + e.getMessage());
                }

                Platform.runLater(() -> {
                    todosOsPassageiros.setAll(listaP);
                    nomesOrigemPassageiros.setAll(todosOsPassageiros.stream().map(Passageiro::getNome).collect(Collectors.toList()));
                    
                    configurarAutoCompleteComboBox(cmbPassageiroAuto, nomesOrigemPassageiros, true);
                    configurarAutoCompleteComboBox(cmbRota, FXCollections.observableArrayList(rotasStrings), true);
                    configurarAutoCompleteComboBox(cmbTipoPassagemAux, FXCollections.observableArrayList(tipoPassagem), false);
                    configurarAutoCompleteComboBox(cmbViagem, FXCollections.observableArrayList(viagensFormatadas), false);
                    configurarAutoCompleteComboBox(cmbSexo, FXCollections.observableArrayList(sexos), false);
                    configurarAutoCompleteComboBox(cmbTipoDoc, FXCollections.observableArrayList(tiposDoc), false);
                    configurarAutoCompleteComboBox(cmbNacionalidade, FXCollections.observableArrayList(nac), false);
                    configurarAutoCompleteComboBox(cmbAgenteAux, FXCollections.observableArrayList(agentes), false);
                    configurarAutoCompleteComboBox(cmbAcomodacao, FXCollections.observableArrayList(acomodacoes), false);
                    
                    if (cmbPesquisarModo != null) {
                        cmbPesquisarModo.setItems(FXCollections.observableArrayList("Número Bilhete", "Passageiro", "Nº Documento", "Data Partida"));
                        cmbPesquisarModo.getSelectionModel().selectFirst();
                    }
                    
                    carregarViagemAtualAoIniciar();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if(rootPane!=null) rootPane.setDisable(false); });
            }
        }).start();
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
                    } catch (Exception e) {}
                }
            }
        });
    }

    private void carregarConfiguracaoEmpresa() {
        String sql = "SELECT path_logo, companhia, nome_embarcacao, cnpj, telefone, endereco, frase_relatorio, recomendacoes_bilhete FROM configuracao_empresa LIMIT 1";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                if (rs.getString("path_logo") != null) this.empPathLogo = rs.getString("path_logo");
                if (rs.getString("companhia") != null) this.empCompanhia = rs.getString("companhia");
                if (rs.getString("nome_embarcacao") != null) this.empEmbarcacao = rs.getString("nome_embarcacao");
                if (rs.getString("cnpj") != null) this.empCnpj = rs.getString("cnpj");
                if (rs.getString("telefone") != null) this.empTelefone = rs.getString("telefone");
                if (rs.getString("frase_relatorio") != null) this.empFrase = rs.getString("frase_relatorio");
                if (rs.getString("recomendacoes_bilhete") != null) this.empRecomendacoes = rs.getString("recomendacoes_bilhete").replace("\\n", "\n");
                
                this.empProprietario = this.empCompanhia; 
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                            Optional<Viagem> vOpt = listaViagensCarregadas.stream().filter(v -> v.getId() == id).findFirst();
                            if (vOpt.isPresent()) {
                                this.viagemSelecionada = vOpt.get();
                                configurarTelaComViagemAtiva();
                            } else {
                                Viagem v = viagemDAO.buscarPorId(id);
                                if (v != null) { this.viagemSelecionada = v; configurarTelaComViagemAtiva(); }
                            }
                        }
                    } catch (Exception e) {}
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
                } catch (Exception e) {}
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
        
        new Thread(() -> {
            try {
                List<Passagem> passagensParaExibir = passagemDAO.listarPorViagem(viagemSelecionada.getId());
                
                Platform.runLater(() -> {
                    passagensDaViagemAtual.setAll(passagensParaExibir);
                    if (tablePassagens != null) tablePassagens.setItems(passagensDaViagemAtual);
                    if (txtTotalPassageiros != null) txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (tablePassagens != null) tablePassagens.getItems().clear();
                    if (txtTotalPassageiros != null) txtTotalPassageiros.setText("0");
                });
            }
        }).start();
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
        if (colValorAPagar != null) { colValorAPagar.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDevedor())); colValorAPagar.setCellFactory(cellFactoryMoeda); }

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
                            setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        } else {
                            setText("PENDENTE");
                            setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-alignment: CENTER;");
                        }
                    }
                }
            });
        }
    }

    // =======================================================================
    // <<< MÉTODOS DE AÇÃO >>>
    // =======================================================================

    @FXML
    private void handleSalvar(ActionEvent event) {
        String nomePassageiroDigitado = (cmbPassageiroAuto != null && cmbPassageiroAuto.getEditor() != null) ? cmbPassageiroAuto.getEditor().getText() : "";
        String selectedRotaStr = (cmbRota != null) ? cmbRota.getSelectionModel().getSelectedItem() : null;

        if (nomePassageiroDigitado.isEmpty() ||
            dpDataNascimento.getValue() == null || cmbTipoPassagemAux.getValue() == null ||
            cmbAcomodacao.getValue() == null || selectedRotaStr == null || selectedRotaStr.trim().isEmpty() ||
            cmbAgenteAux.getValue() == null || (txtAPagar != null && parseBigDecimal(txtAPagar.getText()).compareTo(BigDecimal.ZERO) <= 0)) {
            showAlert(AlertType.WARNING, "Campos Obrigatórios", "Por favor, preencha todos os campos obrigatórios e verifique se o valor a pagar é maior que zero.");
            return;
        }

        if (this.viagemSelecionada == null || this.viagemSelecionada.getId() == null) {
            showAlert(AlertType.ERROR, "Erro de Viagem", "Nenhuma viagem selecionada.");
            return;
        }

        try {
            Passagem passagemParaSalvar = (this.passagemEmEdicao != null) ? this.passagemEmEdicao : new Passagem();
            preencherDadosDaPassagem(passagemParaSalvar, nomePassageiroDigitado, selectedRotaStr);

            boolean pagamentoConfirmado = showFinalizarPagamentoDialog(passagemParaSalvar);

            if (pagamentoConfirmado) {
                salvarDadosFinais(passagemParaSalvar);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Crítico", "Ocorreu um erro inesperado ao preparar a venda: " + e.getMessage());
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
            showAlert(AlertType.ERROR, "Erro", "Falha ao salvar os dados do passageiro.");
            return;
        }

        if (isNovoPassageiro) {
            todosOsPassageiros.add(passageiroSalvo);
            todosOsPassageiros.sort(Comparator.comparing(Passageiro::getNome));
            nomesOrigemPassageiros.setAll(todosOsPassageiros.stream().map(Passageiro::getNome).collect(Collectors.toList()));
        }

        passagemParaSalvar.setIdPassageiro(passageiroSalvo.getId());
        passagemParaSalvar.setStatusPassagem("EMITIDA");

        boolean sucesso;
        if (this.passagemEmEdicao == null) {
            sucesso = passagemDAO.inserir(passagemParaSalvar);
        } else {
            sucesso = passagemDAO.atualizar(passagemParaSalvar);
        }

        if (sucesso) {
            Alert alertPrint = new Alert(AlertType.CONFIRMATION);
            alertPrint.setTitle("Venda Concluída");
            alertPrint.setHeaderText("Passagem salva com sucesso!");
            alertPrint.setContentText("Deseja imprimir o bilhete agora?");

            ButtonType btnSim = new ButtonType("Sim", ButtonBar.ButtonData.YES);
            ButtonType btnNao = new ButtonType("Não", ButtonBar.ButtonData.NO);
            alertPrint.getButtonTypes().setAll(btnSim, btnNao);

            Optional<ButtonType> result = alertPrint.showAndWait();
            if (result.isPresent() && result.get() == btnSim) {
                passagemParaSalvar.setDataNascimento(passageiroSalvo.getDataNascimento());
                passagemParaSalvar.setSexo(passageiroSalvo.getSexo());
                passagemParaSalvar.setNacionalidade(passageiroSalvo.getNacionalidade()); 
                mostrarPreviewImpressao(passagemParaSalvar);
            }

            Viagem vAtual = this.viagemSelecionada;
            
            Platform.runLater(() -> {
                fecharTodosOsDropdowns();
                limparCamposParaNovaVendaAutomatica();
                this.viagemSelecionada = vAtual;
                configurarTelaComViagemAtiva();

                if(tablePassagens != null) tablePassagens.setDisable(false);
                // Habilitar campos automaticamente para a próxima venda — sem necessidade de clicar NOVO
                habilitarCamposParaNovaVenda(true);
                if(btnNovo != null) btnNovo.setDisable(true);

                if (cmbPassageiroAuto != null) {
                    cmbPassageiroAuto.requestFocus();
                }
            });

        } else {
            showAlert(AlertType.ERROR, "Erro", "Erro ao salvar passagem.");
        }
    }

    private void preencherDadosDaPassagem(Passagem passagem, String nomePassageiro, String rotaStr) throws SQLException {
        if (this.passagemEmEdicao == null) {
            try {
                passagem.setNumBilhete(Integer.parseInt(txtNumBilhete.getText()));
            } catch (NumberFormatException e) {
                throw new SQLException("Número de bilhete inválido.");
            }
        }

        String[] rotaParts = rotaStr.split(" - ");
        String origem = rotaParts[0].trim();
        String destino = (rotaParts.length > 1) ? rotaParts[1].trim() : "";
        Integer idRotaInteger = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origem, destino);
        if (idRotaInteger == null) {
            throw new SQLException("ID da Rota não encontrado.");
        }

        passagem.setIdRota(idRotaInteger.longValue());
        passagem.setIdViagem(viagemSelecionada.getId());

        if (viagemSelecionada.getIdHorarioSaida() != null) {
            passagem.setIdHorarioSaida(viagemSelecionada.getIdHorarioSaida().intValue());
        }
        passagem.setDescricaoHorarioSaida(viagemSelecionada.getDescricaoHorarioSaida());

        if (txtIdade.getText() != null && !txtIdade.getText().isEmpty()) {
            passagem.setIdade(Integer.parseInt(txtIdade.getText()));
        }
        passagem.setNomePassageiro(nomePassageiro);
        passagem.setTipoPassagemAux(cmbTipoPassagemAux.getValue());
        passagem.setAgenteAux(cmbAgenteAux.getValue());
        passagem.setAcomodacao(cmbAcomodacao.getValue());
        passagem.setOrigem(origem);
        passagem.setDestino(destino);
        passagem.setDataViagem(viagemSelecionada.getDataViagem());
        passagem.setStrViagem(viagemSelecionada.toString());
        passagem.setRequisicao(txtRequisicao.getText());
        passagem.setValorAlimentacao(parseBigDecimal(txtAlimentacao.getText()));
        passagem.setValorTransporte(parseBigDecimal(txtTransporte.getText()));
        passagem.setValorCargas(parseBigDecimal(txtCargas.getText()));
        passagem.setValorDescontoTarifa(parseBigDecimal(txtDescontoTarifa.getText()));
        passagem.setValorDesconto(parseBigDecimal(txtDesconto.getText()));
        passagem.setValorTotal(parseBigDecimal(txtTotal.getText()));
        passagem.setValorAPagar(parseBigDecimal(txtAPagar.getText()));
        passagem.setStatusPassagem("PENDENTE_PAGAMENTO");
    }

    // =======================================================================
    // <<< OUTROS MÉTODOS AUXILIARES >>>
    // =======================================================================

    private void configurarAutoCompleteComboBox(ComboBox<String> comboBox, ObservableList<String> items, boolean customAction) {
        if (comboBox == null || items == null) return;

        comboBox.setEditable(true);
        comboBox.setItems(items);

        // Cópia fixa dos itens originais para restaurar ao filtrar
        final ObservableList<String> itemsOriginal = FXCollections.observableArrayList(items);
        // Flag local para evitar loop infinito durante filtragem
        final boolean[] isFiltering = {false};

        // Interceptar KEY_TYPED de ESPAÇO antes que o skin nativo do ComboBox processe.
        // O ComboBoxListViewSkin do JavaFX ao receber ESPAÇO confirma o item destacado
        // no dropdown e muda o texto do editor — isso é o que causava o preenchimento
        // automático ao digitar "JOAO " com "JOAO GUILHERME" destacado.
        // Consumindo o evento e reinserindo o espaço manualmente, o textProperty listener
        // continua funcionando normalmente SEM que o skin interfira.
        comboBox.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, keyEvent -> {
            if (" ".equals(keyEvent.getCharacter()) && comboBox.isShowing()) {
                keyEvent.consume(); // Impede o skin de tratar espaço como confirmação
                int pos = comboBox.getEditor().getCaretPosition();
                comboBox.getEditor().insertText(pos, " "); // Insere o espaço normalmente
            }
        });

        // Listener de texto: filtra letra por letra (estilo Google)
        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdatingFromCode || isFiltering[0]) return;

            // *** CORREÇÃO CRÍTICA ***
            // Captura o texto AGORA (síncrono), antes do Platform.runLater.
            // O skin nativo do ComboBox pode alterar o texto do editor no intervalo entre
            // o listener disparar e o Platform.runLater executar (race condition).
            // Exemplo: o usuário digita "JOAO " → o skin muda o editor para "JOAO GUILHERME"
            // antes do Platform.runLater rodar. Capturando newValue aqui, garantimos que
            // usamos o texto que o usuário REALMENTE digitou.
            final String textoDigitado = (newValue != null) ? newValue : "";

            Platform.runLater(() -> {
                if (isUpdatingFromCode || isFiltering[0]) return;

                isFiltering[0] = true;
                try {
                    // Limpa seleção para impedir que o skin use item auto-selecionado
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

                    // Força o editor de volta ao texto exato que o usuário digitou.
                    // O skin pode ter alterado para o item selecionado (ex: "JOAO GUILHERME").
                    // O isUpdatingFromCode=true evita loop infinito neste setText.
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

                // SÓ abre o dropdown se este combo tiver o foco
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
        });

        // ENTER/TAB: se navegou com setas no dropdown, confirma a seleção
        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                String selectedItem = comboBox.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.isEmpty()) {
                    isUpdatingFromCode = true;
                    comboBox.getEditor().setText(selectedItem);
                    isUpdatingFromCode = false;
                    if (customAction && comboBox == cmbPassageiroAuto) {
                        preencherPassageiroPorNome(selectedItem);
                    }
                } else if (customAction && comboBox == cmbPassageiroAuto) {
                    // Sem item selecionado no dropdown — verificar se o nome digitado existe no banco
                    String textoDigitado = comboBox.getEditor().getText();
                    if (textoDigitado != null && !textoDigitado.trim().isEmpty()) {
                        String nomeTrimmed = textoDigitado.trim();
                        boolean existe = todosOsPassageiros.stream()
                            .anyMatch(p -> p.getNome().equalsIgnoreCase(nomeTrimmed));
                        if (existe) {
                            preencherPassageiroPorNome(nomeTrimmed);
                        } else {
                            // Passageiro não encontrado — perguntar se deseja continuar como novo
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
                            // Se confirmou, mantém o nome digitado sem auto-preencher os demais campos
                        }
                    }
                }
                comboBox.hide();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                comboBox.hide();
                event.consume();
            }
        });

        // O preenchimento dos dados do passageiro acontece SOMENTE quando:
        // 1. ENTER/TAB (acima) com item selecionado no dropdown
        // 2. Clique explícito do mouse num item do dropdown (via skinProperty abaixo)

        if (customAction && comboBox == cmbPassageiroAuto) {
            // Detectar clique explícito do usuário no ListView interno do dropdown
            // Isso evita que o skin nativo do ComboBox acione o preenchimento ao digitar espaço
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

        // Ao perder o foco: apenas fecha o dropdown — não preenche automaticamente
        // O usuário deve confirmar explicitamente via ENTER ou clique no dropdown
        comboBox.getEditor().focusedProperty().addListener((obs4, wasFocused, isFocused) -> {
            if (!isFocused) {
                comboBox.hide();
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
                // >>> CORREÇÃO DE EDIÇÃO <<<
                // Se houver seleção, SEMPRE habilita os botões de edição, independente do estado anterior
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
                // >>> CORREÇÃO: DOC NÃO ENCONTRADO <<<
                // Se não achou o doc, NÃO limpa o nome. Apenas assume que é um novo doc para o passageiro atual (ou novo).
                // Mantém passageiroEmEdicao como null se estava null (novo), ou mantém o passageiro se estava editando e mudou o doc.
                // Na dúvida, para novo cadastro, passageiroEmEdicao fica null.
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
            limparCamposTarifa();
        } finally {
            calcularValoresPassagem();
        }
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        String cleanedText = text.replace("R$", "").replace(".", "").replace(",", ".").trim();
        try { return new BigDecimal(cleanedText); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void setupCalculoTotalPassagem() {
        List.of(txtAlimentacao, txtTransporte, txtCargas, txtDescontoTarifa, txtDesconto).forEach(field -> {
            if (field != null) field.textProperty().addListener((obs, oldVal, newVal) -> calcularValoresPassagem());
        });
    }

    private void calcularValoresPassagem() {
        BigDecimal alimentacao = parseBigDecimal(txtAlimentacao.getText());
        BigDecimal transporte = parseBigDecimal(txtTransporte.getText());
        BigDecimal cargas = parseBigDecimal(txtCargas.getText());
        BigDecimal descontoTarifa = parseBigDecimal(txtDescontoTarifa.getText());
        BigDecimal descontoGeral = parseBigDecimal(txtDesconto.getText());

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
                ComboBox<?> combo = (ComboBox<?>) currentNode;
                combo.getEditor().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER && !combo.isShowing()) {
                        event.consume();
                        Platform.runLater(() -> focusableNodes.get(nextIndex).requestFocus());
                    }
                });
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

                    if (event.getCode() == KeyCode.F1) {
                        if (btnSalvar != null && !btnSalvar.isDisabled()) {
                            handleSalvar(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F2) {
                        if (btnNovo != null && !btnNovo.isDisabled()) {
                            handleNovo(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F3) {
                        if (btnEditar != null && !btnEditar.isDisabled()) {
                            handleEditar(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F4) {
                        if (btnExcluir != null && !btnExcluir.isDisabled()) {
                            handleExcluir(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F5) {
                        if (btnCancelar != null && !btnCancelar.isDisabled()) {
                            handleCancelar(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F6) {
                        if (btnImprimirBilhete != null && !btnImprimirBilhete.isDisabled()) {
                            handleImprimirBilhete(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F7) {
                        if (btnImprimirLista != null && !btnImprimirLista.isDisabled()) {
                            handleImprimirLista(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.F8) {
                        if (btnRelatorio != null && !btnRelatorio.isDisabled()) {
                            handleRelatorio(null);
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        if (btnSair != null && !btnSair.isDisabled()) {
                            handleSair(null);
                            event.consume();
                        }
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
            showAlert(AlertType.INFORMATION, "Viagem Necessária", "Nenhuma viagem foi informada. Verifique se existe uma viagem 'Atual'.");
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
        alert.setHeaderText("Cancelar operação?");
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
                case "Número Bilhete":
                    String num = String.valueOf(p.getNumBilhete());
                    return num != null && num.contains(termo);

                case "Passageiro":
                    String nome = p.getNomePassageiro();
                    return nome != null && nome.toUpperCase().contains(termoUpper);

                case "Nº Documento":
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
    // <<< MÉTODOS DE EDIÇÃO, EXCLUSÃO E IMPRESSÃO >>>
    // =======================================================================

    @FXML
    private void handleEditar(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "Seleção", "Selecione uma passagem na tabela para editar.");
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
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro", "Erro ao carregar dados para edição: " + e.getMessage());
            configurarEstadoInicialDaTela();
        } finally {
            isUpdatingFromCode = false;
        }
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "Seleção", "Selecione uma passagem para excluir.");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Excluir Passagem");
        alert.setHeaderText("Deseja realmente excluir a passagem do bilhete " + selected.getNumBilhete() + "?");
        alert.setContentText("Essa ação não poderá ser desfeita.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean excluido = passagemDAO.excluir(selected.getId());
                if (excluido) {
                    passagensDaViagemAtual.remove(selected);
                    tablePassagens.getItems().remove(selected);
                    txtTotalPassageiros.setText(String.valueOf(passagensDaViagemAtual.size()));

                    showAlert(AlertType.INFORMATION, "Sucesso", "Passagem excluída com sucesso.");
                    limparTodosOsCamposGUI();
                } else {
                    showAlert(AlertType.ERROR, "Erro", "Não foi possível excluir a passagem.");
                }
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Erro", "Erro ao excluir: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImprimirBilhete(ActionEvent event) {
        Passagem selected = tablePassagens.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(AlertType.WARNING, "Seleção", "Selecione uma passagem para imprimir o bilhete.");
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
                System.err.println("Erro ao buscar dados complementares do passageiro: " + e.getMessage());
            }
        }

        mostrarPreviewImpressao(selected);
    }

    // =================================================================================================
    // >>> PREVIEW DE IMPRESSÃO + IMPRESSÃO CORRIGIDA (LAYOUT TÉRMICO) <<<
    // =================================================================================================

    private void mostrarPreviewImpressao(Passagem p) {
        Stage previewStage = new Stage();
        previewStage.setTitle("Visualização do Bilhete (Impressora Térmica)");
        previewStage.initModality(Modality.WINDOW_MODAL);
        previewStage.initOwner(rootPane.getScene().getWindow());

        Node bilheteNode = criarLayoutBilheteVisual(p);

        Button btnConfirmar = new Button("🖨️ IMPRIMIR AGORA");
        btnConfirmar.setStyle("-fx-font-size: 16px; -fx-background-color: #2e7d32; -fx-text-fill: white; -fx-padding: 10px 20px;");
        btnConfirmar.setOnAction(e -> {
            Node nodeParaImpressora = criarLayoutBilheteVisual(p);
            imprimirNodeReal(nodeParaImpressora);
            previewStage.close();
        });

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-font-size: 14px;");
        btnCancelar.setOnAction(e -> previewStage.close());

        HBox buttonBox = new HBox(20, btnConfirmar, btnCancelar);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f0f0;");
        root.getChildren().addAll(new Text("Confira os dados abaixo antes de imprimir:"), bilheteNode, buttonBox);

        Scene scene = new Scene(root, 700, 400);
        previewStage.setScene(scene);
        previewStage.show();
    }

    private Node criarLayoutBilheteVisual(Passagem p) {
        double logicHeight = 230; 
        double logicWidth = 600;

        // FONTES E ESTILOS - AJUSTADOS
        Font fontHeaderBarco = Font.font("Arial", FontWeight.BOLD, 22);
        Font fontHeaderDono = Font.font("Arial", FontWeight.NORMAL, 14);
        Font fontHeaderCNPJ = Font.font("Arial", FontWeight.NORMAL, 12);
        Font fontHeaderPhrase = Font.font("Arial", FontWeight.NORMAL, 11);
        
                // CORRIGIDO: Variável adicionada
        Font fontHeaderSmall = Font.font("Arial", FontWeight.NORMAL, 10); 

        Font fontSectionTitle = Font.font("Arial", FontWeight.BOLD, 11);
        Font fontData = Font.font("Arial", FontWeight.BOLD, 12);
        Font fontLabel = Font.font("Arial", FontWeight.NORMAL, 11);
        Font fontValores = Font.font("Courier New", FontWeight.BOLD, 12);

        Font fontBilheteNum = Font.font("Arial", FontWeight.BOLD, 26);
        Font fontSituacao = Font.font("Arial", FontWeight.BOLD, 20);
        Font fontTotalBig = Font.font("Arial", FontWeight.BOLD, 16);

        HBox mainLayout = new HBox(5);
        mainLayout.setPadding(new Insets(5));
        mainLayout.setPrefSize(logicWidth, logicHeight);
        mainLayout.setMaxSize(logicWidth, logicHeight);
        mainLayout.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2;");

        VBox leftPanel = new VBox(2);
        leftPanel.setPrefWidth(logicWidth * 0.65);

        // --- CABEÇALHO ---
        VBox headerContent = new VBox(0);
        headerContent.setAlignment(Pos.CENTER_LEFT);
        
        Text txtBarco = new Text(this.empEmbarcacao);
        txtBarco.setFont(fontHeaderBarco);

        Text txtDono = new Text(this.empProprietario);
        txtDono.setFont(fontHeaderDono);
        txtDono.setFill(Color.BLACK);

        Text txtCnpjTel = new Text("CNPJ: " + empCnpj + " | " + empTelefone);
        txtCnpjTel.setFont(fontHeaderCNPJ);

        Text txtFrase = new Text(this.empFrase);
        txtFrase.setFont(fontHeaderPhrase);
        txtFrase.setFill(Color.BLACK); 

        headerContent.getChildren().addAll(txtBarco, txtDono, txtCnpjTel, txtFrase);

        HBox headerBox = new HBox(5);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        if (this.empPathLogo != null && !this.empPathLogo.isEmpty()) {
            try {
                File f = new File(this.empPathLogo);
                if (f.exists()) {
                    ImageView logo = new ImageView(new Image(new FileInputStream(f)));
                    logo.setFitWidth(70); logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                }
            } catch(Exception e) {}
        }
        headerBox.getChildren().add(headerContent);
        
        VBox headerContainer = new VBox(headerBox);
        headerContainer.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(
                Color.BLACK, 
                BorderStrokeStyle.SOLID, 
                CornerRadii.EMPTY, 
                new BorderWidths(0, 0, 2.5, 0) 
        )));
        headerContainer.setPadding(new Insets(0, 0, 4, 0));
        leftPanel.getChildren().add(headerContainer);

        // --- VIAGEM ---
        VBox tripBox = createSectionBox("VIAGEM", fontSectionTitle, leftPanel.getPrefWidth());
        
        String prevChegada = (this.viagemSelecionada != null && this.viagemSelecionada.getDataChegada() != null) 
                            ? dateFormatter.format(this.viagemSelecionada.getDataChegada()) : "??";
        
        tripBox.getChildren().addAll(
            criarLinhaDupla("De:", p.getOrigem(), fontLabel, fontData),
            criarLinhaDupla("Para:", p.getDestino(), fontLabel, fontData),
            new HBox(10,
                criarLinhaDupla("Data:", dateFormatter.format(p.getDataViagem()), fontLabel, fontData),
                criarLinhaDupla("Prev:", prevChegada, fontLabel, fontData)
            ),
            new HBox(10,
                criarLinhaDupla("Acom.:", p.getAcomodacao(), fontLabel, fontData),
                criarLinhaDupla("Agente:", (p.getAgenteAux() != null ? p.getAgenteAux() : "--"), fontLabel, fontData)
            )
        );
        leftPanel.getChildren().add(tripBox);

        // --- PASSAGEIRO ---
        VBox passBox = createSectionBox("PASSAGEIRO", fontSectionTitle, leftPanel.getPrefWidth());
        String idade = (p.getIdade() > 0 ? p.getIdade() + "a" : "--");
        String nascimento = (p.getDataNascimento() != null) ? dateFormatter.format(p.getDataNascimento()) : "--";
        String nacionalidade = (p.getNacionalidade() != null) ? p.getNacionalidade() : "BR";

        passBox.getChildren().addAll(
            criarLinhaDupla("Nome:", p.getNomePassageiro(), fontLabel, fontData),
            new HBox(10,
                criarLinhaDupla("Doc:", p.getNumeroDoc(), fontLabel, fontData),
                criarLinhaDupla("Nac:", nacionalidade, fontLabel, fontData) 
            ),
            new HBox(10,
                criarLinhaDupla("DN:", nascimento, fontLabel, fontData), 
                criarLinhaDupla("Id:", idade, fontLabel, fontData),
                criarLinhaDupla("Sx:", (p.getSexo()!=null?p.getSexo():"--"), fontLabel, fontData)
            )
        );
        leftPanel.getChildren().add(passBox);

        // --- PAGAMENTO ---
        HBox finBox = new HBox(5);
        VBox tarBox = createSectionBox("TARIFAS", fontSectionTitle, leftPanel.getPrefWidth()/2);
        tarBox.getChildren().addAll(
            criarLinhaValor("Alim:", p.getValorAlimentacao(), fontValores),
            criarLinhaValor("Transp:", p.getValorTransporte(), fontValores),
            criarLinhaValor("Carga:", p.getValorCargas(), fontValores)
        );
        
        VBox payBox = createSectionBox("PAGAMENTO", fontSectionTitle, leftPanel.getPrefWidth()/2);
        
        BigDecimal total = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        BigDecimal desconto = p.getValorDesconto() != null ? p.getValorDesconto() : BigDecimal.ZERO;
        BigDecimal pago = p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO;
        BigDecimal troco = BigDecimal.ZERO;
        BigDecimal falta = BigDecimal.ZERO;
        
        BigDecimal aPagar = total.subtract(desconto);
        if (pago.compareTo(aPagar) > 0) troco = pago.subtract(aPagar);
        else if (pago.compareTo(aPagar) < 0) falta = aPagar.subtract(pago);

        payBox.getChildren().add(criarLinhaValor("TOTAL:", total, fontTotalBig));
        if (desconto.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Desc.:", desconto, fontValores));
        payBox.getChildren().add(criarLinhaValor("Pago:", pago, fontValores));
        
        if (troco.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Troco:", troco, fontValores));
        if (falta.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Falta:", falta, fontValores)); 
        
        String formaPagtoTexto = (falta.compareTo(BigDecimal.ZERO) <= 0) ? "A VISTA" : "PENDENTE";
        payBox.getChildren().add(criarLinhaDupla("Situação:", formaPagtoTexto, fontLabel, fontData));

        finBox.getChildren().addAll(tarBox, payBox);
        leftPanel.getChildren().add(finBox);
        
        leftPanel.getChildren().add(new Text("Emissão: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))) {{ setFont(fontHeaderSmall); }});

        // Right Panel
        VBox rightPanel = new VBox(5);
        rightPanel.setPrefWidth(logicWidth * 0.35 - 10);

        VBox bilBox = new VBox(2);
        bilBox.setAlignment(Pos.CENTER);
        bilBox.setStyle("-fx-border-color: black; -fx-background-color: #eeeeee; -fx-padding: 5; -fx-border-width: 2;");
        Text txtBil = new Text("BILHETE\n" + p.getNumBilhete());
        txtBil.setFont(fontBilheteNum);
        txtBil.setTextAlignment(TextAlignment.CENTER);

        String situacao = "PENDENTE"; Color cor = Color.RED;
        if(p.getDevedor() != null && p.getDevedor().compareTo(BigDecimal.ZERO) <= 0) { situacao = "PAGO"; cor = Color.GREEN; }
        Text txtSit = new Text(situacao); txtSit.setFont(fontSituacao); txtSit.setFill(cor);

        bilBox.getChildren().addAll(txtBil, txtSit);
        rightPanel.getChildren().add(bilBox);

        VBox recBox = createSectionBox("AVISOS", Font.font("Arial", FontWeight.BOLD, 11), rightPanel.getPrefWidth());
        VBox.setVgrow(recBox, Priority.ALWAYS);
        Text txtRec = new Text(this.empRecomendacoes != null ? this.empRecomendacoes : "");
        txtRec.setFont(Font.font("Arial", 10));
        txtRec.setWrappingWidth(rightPanel.getPrefWidth() - 5);
        recBox.getChildren().add(txtRec);
        rightPanel.getChildren().add(recBox);

        mainLayout.getChildren().addAll(leftPanel, rightPanel);
        return mainLayout;
    }

    // CRIA CAIXAS COM LINHA FINA (PADRÃO 0.5)
    private VBox createSectionBox(String title, Font fontTitle, double width) {
        VBox box = new VBox(1);
        box.setPrefWidth(width);
        box.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(
                Color.web("#999999"), 
                BorderStrokeStyle.SOLID, 
                CornerRadii.EMPTY, 
                new BorderWidths(0, 0, 0.5, 0) // Linha Fina
        )));
        box.setPadding(new Insets(2));
        Text txtTitle = new Text(title);
        txtTitle.setFont(fontTitle);
        box.getChildren().add(txtTitle);
        return box;
    }

    private HBox criarLinhaDupla(String label, String valor, Font fontLbl, Font fontVal) {
        HBox line = new HBox(5);
        Text txtLbl = new Text(label); txtLbl.setFont(fontLbl);
        Text txtVal = new Text(valor != null ? valor : ""); txtVal.setFont(fontVal);
        line.getChildren().addAll(txtLbl, txtVal);
        return line;
    }

    private HBox criarLinhaValor(String label, BigDecimal val, Font fontVal) {
        HBox line = new HBox(5);
        Text txtLbl = new Text(label); txtLbl.setFont(Font.font("Arial", 12));
        Text txtVal = new Text(val != null ? String.format("R$ %,.2f", val) : "R$ 0,00");
        txtVal.setFont(fontVal);
        line.getChildren().addAll(txtLbl, txtVal);
        return line;
    }

    // IMPRESSÃO (COM ROTAÇÃO)
    private BufferedImage rotateImage90(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage newImage = new BufferedImage(height, width, img.getType());
        Graphics2D g = newImage.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((height - width) / 2.0, (width - height) / 2.0);
        at.rotate(Math.toRadians(90), width / 2.0, height / 2.0);
        g.setTransform(at);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return newImage;
    }

    private void imprimirNodeReal(Node nodeParaImpressora) {
        try {
            // 1. Preparar o Node e Snapshot
            nodeParaImpressora.setStyle("-fx-background-color: white; -fx-font-family: 'Arial'; -fx-font-size: 11px;");
            Group root = new Group(nodeParaImpressora);
            Scene tempScene = new Scene(root);
            root.applyCss();
            root.layout();

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            WritableImage fxImage = nodeParaImpressora.snapshot(params, null);

            int imgWidth = (int) fxImage.getWidth();
            int imgHeight = (int) fxImage.getHeight();

            BufferedImage bufferedImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < imgWidth; x++) {
                for (int y = 0; y < imgHeight; y++) {
                    bufferedImage.setRGB(x, y, fxImage.getPixelReader().getArgb(x, y));
                }
            }

            if (bufferedImage.getWidth() > bufferedImage.getHeight()) {
                bufferedImage = rotateImage90(bufferedImage);
            }

            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            PrintService service = PrintServiceLookup.lookupDefaultPrintService();
            if (service != null) {
                job.setPrintService(service);
            }

            java.awt.print.PageFormat pf = job.defaultPage();
            java.awt.print.Paper paper = pf.getPaper();
            double finalWidth = bufferedImage.getWidth();
            double finalHeight = bufferedImage.getHeight();
            paper.setSize(finalWidth + 10, finalHeight + 50);
            paper.setImageableArea(0, 0, finalWidth + 10, finalHeight + 50);
            pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);
            pf.setPaper(paper);

            final BufferedImage imageToPrint = bufferedImage;

            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                double pageWidth = pageFormat.getImageableWidth();
                double scale = 1.0;
                if (imageToPrint.getWidth() > pageWidth) scale = pageWidth / imageToPrint.getWidth();
                g2d.scale(scale, scale);
                g2d.drawImage(imageToPrint, 0, 0, null);

                return Printable.PAGE_EXISTS;
            }, pf);

            job.print();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro de Impressão", "Falha ao imprimir: " + e.getMessage());
        }
    }

    @FXML private void handleImprimirLista(ActionEvent event) {
        abrirNovaJanela("/gui/ListarPassageirosViagem.fxml", "Lista de Passageiros da Viagem");
    }

    @FXML private void handleRelatorio(ActionEvent event) {
        abrirNovaJanela("/gui/RelatorioPassagens.fxml", "Relatório de Passagens");
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
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Abrir Tela", "Não foi possível carregar a tela: " + fxmlPath);
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

    private void showAlert(AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void fecharComboBoxesAbertos() {
        List<ComboBox<?>> comboBoxes = Arrays.asList(cmbPassageiroAuto, cmbRota, cmbTipoPassagemAux, cmbViagem, cmbSexo, cmbTipoDoc);
        comboBoxes.forEach(comboBox -> {
            if (comboBox != null && comboBox.isShowing()) {
                comboBox.hide();
            }
        });
    }

    @FXML
    private void salvarPassagem() {
        fecharComboBoxesAbertos();
        // ... código existente para salvar passagem ...
    }
}