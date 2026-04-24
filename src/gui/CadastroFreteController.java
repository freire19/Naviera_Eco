package gui;

import dao.ItemFreteDAO;
import dao.ConexaoBD;
import gui.util.AutoCompleteHelper;
import gui.util.PermissaoService;
import dao.DAOUtils;
import dao.ViagemDAO;
import model.Viagem;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser; 
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import model.ItemFrete;

// --- IMPORTS DAS NOVAS BIBLIOTECAS (Audio e Imagem) ---
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.vosk.Model;
import org.vosk.Recognizer;
// -----------------------------------------------------

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.File; 
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import gui.util.AlertHelper;
import gui.util.MoneyUtil;
import util.AppLogger;
import gui.util.ValidationHelper;
import service.FreteService;
import service.FreteService.ResultadoFrete;
import dao.FreteDAO.FreteData;
import dao.FreteDAO.FreteItemData;

/**
 * Controller da tela CadastroFrete.fxml.
 * VERSÃƒO CORRIGIDA: MÃ©todo PUBLIC e EdiÃ§Ã£o Direta.
 */
public class CadastroFreteController implements Initializable {

    // VARIÃVEL ESTÃTICA PARA RECEBER O NÃšMERO DO FRETE
    // #009: volatile para visibilidade entre chamadas de diferentes controllers
    private static volatile String staticNumeroFreteParaAbrir = null;

    public static void setNumeroFreteParaAbrir(String numFrete) {
        staticNumeroFreteParaAbrir = numFrete;
    }

    //<editor-fold desc="FXML Injections">
    @FXML private AnchorPane rootPane;
    @FXML private ComboBox<String> cbRemetente;
    @FXML private ComboBox<String> cbRota;
    @FXML private ComboBox<String> cbConferente;
    @FXML private ComboBox<String> cbCliente;
    @FXML private TextField txtCidadeCobranca;
    @FXML private ComboBox<ItemFrete> cbitem;
    @FXML private TextField txtNumFrete;
    @FXML private TextField txtSaida;
    @FXML private TextField txtLocalTransporte;
    @FXML private TextField txtViagemAtual;
    @FXML private TextArea txtObs;
    @FXML private TextField txtquantidade;
    @FXML private TextField txtpreco;
    @FXML private TextField txttotal;
    @FXML private TextField txtTotalVol;
    @FXML private TextField txtValorTotalNota;
    @FXML private RadioButton rbComDesconto;
    @FXML private RadioButton rbNormal;
    @FXML private Button btnInserir;
    @FXML private Button btnNovo;
    @FXML private Button btnAlterar;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnSair;
    @FXML private Button btnImprimirNota;
    @FXML private Button btnImprimirEtiqueta;
    @FXML private Button btnListaDeFrete;
    @FXML private Button btnImprimirRecibo;
    @FXML private TableView<FreteItemCadastro> tabelaItens;
    @FXML private TableColumn<FreteItemCadastro, Integer> colQuantidade;
    @FXML private TableColumn<FreteItemCadastro, String> colItem;
    @FXML private TableColumn<FreteItemCadastro, Double> colPreco;
    @FXML private TableColumn<FreteItemCadastro, String> colTotal;
    @FXML private ToggleGroup precoToggleGroup;
    //</editor-fold>

    private ObservableList<String> listaRemetentesOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaClientesOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaRotasOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaConferentesOriginal = FXCollections.observableArrayList();
    private ObservableList<ItemFrete> listaItensDisplayOriginal = FXCollections.observableArrayList();

    private ObservableList<FreteItemCadastro> listaTabelaItensFrete = FXCollections.observableArrayList();
    private Map<String, ItemFrete> mapItensCadastrados = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("'R$ '#,##0.00", new DecimalFormatSymbols(new Locale("pt","BR")));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Nota fiscal é gerenciada pelo modo web (OCR/XML) e replicada via sync.
    // Preservamos os valores carregados na edição para não sobrescrever com null ao salvar.
    private String preservadoNumNotaFiscal = null;
    private BigDecimal preservadoValorNotaFiscal = null;
    private BigDecimal preservadoPesoNotaFiscal = null;

    private boolean programmaticamenteAtualizando = false;
    private boolean processandoItemCbItem = false;
    private boolean processandoContatoRemetente= false;
    private boolean processandoContatoCliente = false;
    private String ultimoItemProcessadoCbItem = null;

    private long freteAtualId = -1;
    private ViagemDAO viagemDAO;
    private Viagem viagemAtiva;
    // DM057: camada de servico para logica de negocio
    private final FreteService freteService = new FreteService();
    private final dao.FreteDAO freteDAO = new dao.FreteDAO();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!PermissaoService.isOperacional()) { PermissaoService.exigirOperacional("Cadastro de Frete"); return; }
        this.viagemDAO = new ViagemDAO();

        setComponentProperties();
        configurarComboBoxItem();
        configurarTabela();

        // DR010: carrega dados do banco em background para nao bloquear UI
        Thread bgThread = new Thread(() -> {
            try {
                carregarDadosIniciaisComboBoxes();
                javafx.application.Platform.runLater(this::setComboBoxItems);
            } catch (Exception e) {
                AppLogger.warn("CadastroFreteController", "Erro em CadastroFreteController (bg init): " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("CadastroFreteController", e));
            }
        });
        bgThread.setDaemon(true);
        bgThread.start();
        configurarListenersDeCamposEEventos();

        if (cbRota != null) {
            cbRota.valueProperty().addListener((obs, oldVal, newVal) -> {
                // DL062: usar " - " (com espacos) para evitar split em nomes com hifen
                if (newVal != null && newVal.contains(" - ")) {
                    String[] partes = newVal.split(" - ", 2);
                    if (partes.length > 1 && txtCidadeCobranca != null) {
                        txtCidadeCobranca.setText(partes[1].trim());
                    }
                }
            });
        }

        AutoCompleteHelper.install(cbRemetente, () -> listaRemetentesOriginal,
            () -> { if (cbCliente != null) cbCliente.requestFocus(); });
        AutoCompleteHelper.install(cbCliente, () -> listaClientesOriginal,
            () -> { if (cbRota != null) cbRota.requestFocus(); });
        configurarValidacaoFocoClientesGoogle();

        AutoCompleteHelper.install(cbRota, () -> listaRotasOriginal,
            () -> { if (txtSaida != null) txtSaida.requestFocus(); });
        AutoCompleteHelper.install(cbConferente, () -> listaConferentesOriginal,
            () -> { if (txtquantidade != null) Platform.runLater(txtquantidade::requestFocus); });

        // Enter/Tab em cbConferente pula direto para Qtd (skip nota fiscal/observacoes/cidade cobranca)
        if (cbConferente != null && txtquantidade != null) {
            cbConferente.getEditor().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER || (e.getCode() == KeyCode.TAB && !e.isShiftDown())) {
                    e.consume();
                    Platform.runLater(txtquantidade::requestFocus);
                }
            });
        }

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    switch (event.getCode()) {
                        case F1:
                            if (btnNovo != null && !btnNovo.isDisabled()) handleNovoFrete(null);
                            event.consume(); break;
                        case F2:
                            if (btnAlterar != null && !btnAlterar.isDisabled()) handleAlterarFrete(null);
                            event.consume(); break;
                        case F3:
                            if (btnSalvar != null && !btnSalvar.isDisabled()) handleSalvarFrete(null);
                            event.consume(); break;
                        case F4:
                            if (btnExcluir != null && !btnExcluir.isDisabled()) handleExcluirFrete(null);
                            event.consume(); break;
                        case F5:
                            if (btnListaDeFrete != null && !btnListaDeFrete.isDisabled()) abrirListaFretes(null);
                            event.consume(); break;
                        case F6:
                            if (btnImprimirNota != null && !btnImprimirNota.isDisabled()) imprimirNotaFretePersonalizada(null);
                            event.consume(); break;
                        case ESCAPE:
                            handleSair(null); event.consume(); break;
                        default: break;
                    }
                });
            }
        });


        if (staticNumeroFreteParaAbrir != null) {
            carregarFreteParaEdicao(staticNumeroFreteParaAbrir);
            staticNumeroFreteParaAbrir = null;
        } else {
            configurarParaNovoFrete();
        }
    }

    private void configurarValidacaoFocoClientesGoogle() {
        cbRemetente.getEditor().focusedProperty().addListener((obs, foiFocado, estaFocado) -> {
            if (!estaFocado) {
                String texto = cbRemetente.getEditor().getText().trim().toUpperCase();
                if (!texto.isEmpty()) {
                    processarContatoDigitado(cbRemetente, texto, listaRemetentesOriginal, "Remetente");
                }
            }
        });

        cbCliente.getEditor().focusedProperty().addListener((obs, foiFocado, estaFocado) -> {
            if (!estaFocado) {
                String texto = cbCliente.getEditor().getText().trim().toUpperCase();
                if (!texto.isEmpty()) {
                    processarContatoDigitado(cbCliente, texto, listaClientesOriginal, "Cliente");
                }
            }
        });
    }

    private void configurarComboBoxItem() {
        if (cbitem == null) return;

        cbitem.setConverter(new StringConverter<ItemFrete>() {
            @Override
            public String toString(ItemFrete item) { return item == null ? null : item.getNomeItem(); }
            @Override
            public ItemFrete fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                return mapItensCadastrados.get(string.trim().toLowerCase());
            }
        });

        AutoCompleteHelper.installGeneric(cbitem,
            () -> new ArrayList<>(listaItensDisplayOriginal),
            it -> (it == null) ? "" : it.getNomeItem(),
            itemFrete -> {
                processarItemDigitadoOuSelecionado(itemFrete.getNomeItem());
                if (txtpreco != null) Platform.runLater(txtpreco::requestFocus);
            },
            itemFrete -> {
                // Renderer customizado: nome à esquerda, preço à direita (com cor e sufixo)
                boolean temDesc = rbComDesconto != null && rbComDesconto.isSelected();
                BigDecimal preco = temDesc ? itemFrete.getPrecoUnitarioDesconto() : itemFrete.getPrecoUnitarioPadrao();
                String corPreco = temDesc ? "#059669" : "#047857";
                String sufixo = temDesc ? " (Desc.)" : " (Normal)";

                Label lblNome = new Label(itemFrete.getNomeItem());
                lblNome.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label lblPreco = new Label(df.format(preco) + sufixo);
                lblPreco.setStyle("-fx-font-weight: bold; -fx-text-fill: " + corPreco + ";");

                HBox row = new HBox(lblNome, spacer, lblPreco);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setSpacing(10);
                return row;
            });

        // Ao perder foco, processar texto digitado mesmo sem seleção no menu
        cbitem.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV && !programmaticamenteAtualizando) {
                String text = cbitem.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    ItemFrete matched = mapItensCadastrados.get(text.trim().toLowerCase());
                    if (matched != null) {
                        programmaticamenteAtualizando = true;
                        try { cbitem.setValue(matched); } finally { programmaticamenteAtualizando = false; }
                        processarItemDigitadoOuSelecionado(matched.getNomeItem());
                    } else {
                        processarItemDigitadoOuSelecionado(text.trim());
                    }
                }
            }
        });
    }

    private void configurarParaNovoFrete() {
        limparCamposFrete();
        habilitarCamposParaVisualizacao(false);

        this.viagemAtiva = viagemDAO.buscarViagemAtiva();

        if (this.viagemAtiva != null) {
            if (txtSaida != null) {
                txtSaida.setText(this.viagemAtiva.getDataViagem().format(dateFormatter));
            }
            if (txtViagemAtual != null) {
                txtViagemAtual.setText(this.viagemAtiva.getDataViagem().format(dateFormatter));
            }
            if (btnNovo != null) btnNovo.setDisable(false);
            if (btnSalvar != null) btnSalvar.setDisable(false);
        } else {
            AlertHelper.show(AlertType.INFORMATION, "Nenhuma Viagem Ativa",
                    "NÃ£o hÃ¡ nenhuma viagem ativa no sistema. Por favor, ative uma viagem na tela principal para lanÃ§ar um novo frete.");
            if (btnNovo != null) btnNovo.setDisable(true);
            if (btnSalvar != null) btnSalvar.setDisable(true);
        }
        if (btnAlterar != null) btnAlterar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
        
        Platform.runLater(() -> {
            if (cbRemetente != null) cbRemetente.requestFocus();
        });
    }

    // =================================================================================
    // MÃ‰TODO AGORA Ã‰ PUBLICO PARA SER ACESSADO PELO LISTA FRETES
    // E CONFIGURA A TELA JÃ EM MODO DE EDIÃ‡ÃƒO (COM BOTÃ•ES HABILITADOS)
    // =================================================================================
    // DM057: queries de busca movidas para FreteDAO
    public void carregarFreteParaEdicao(String numFrete) {
        long numeroFreteLong;
        try {
            numeroFreteLong = Long.parseLong(numFrete);
        } catch (NumberFormatException e) {
            AlertHelper.show(AlertType.ERROR, "Erro de Dados", "O numero do frete '" + numFrete + "' e invalido.");
            return;
        }

        try {
            // Buscar frete via DAO (em vez de SQL inline)
            model.Frete freteDb = freteDAO.buscarPorNumero(numeroFreteLong);
            if (freteDb == null) {
                AlertHelper.show(AlertType.WARNING, "Aviso", "Nenhum frete encontrado com numero: " + numFrete);
                return;
            }

            // Buscar via SELECT * para preencherCamposDoFrete (precisa de todos os campos raw)
            try (Connection conn = ConexaoBD.getConnection()) {
                String sqlFrete = "SELECT * FROM fretes WHERE numero_frete = ? AND empresa_id = ?";
                try (PreparedStatement pst = conn.prepareStatement(sqlFrete)) {
                    pst.setLong(1, numeroFreteLong);
                    pst.setInt(2, dao.DAOUtils.empresaId());
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            preencherCamposDoFrete(rs, numFrete);
                        }
                    }
                }
            }

            // Buscar itens via DAO
            List<dao.FreteDAO.FreteItemData> itensDb = freteDAO.buscarItens(freteDb.getIdFrete());
            listaTabelaItensFrete.clear();
            for (dao.FreteDAO.FreteItemData it : itensDb) {
                FreteItemCadastro item = new FreteItemCadastro(
                        it.quantidade, it.nomeItem,
                        it.precoUnitario != null ? it.precoUnitario.doubleValue() : 0.0);
                listaTabelaItensFrete.add(item);
            }
            if (tabelaItens != null) tabelaItens.refresh();
            atualizarTotaisAgregados();

            // --- LÃ“GICA DE ESTADO DA TELA (IGUAL AO NOVO) ---
            habilitarCamposParaEdicao(true); // TUDO DESTRAVADO
            
            if (btnNovo != null) btnNovo.setDisable(false); // Permite limpar e comeÃ§ar um novo
            if (btnSalvar != null) btnSalvar.setDisable(false); // JÃ¡ pode salvar alteraÃ§Ãµes
            if (btnAlterar != null) btnAlterar.setDisable(true); // JÃ¡ estÃ¡ editando
            if (btnExcluir != null) btnExcluir.setDisable(false); // Pode excluir pois existe
            
            // Habilita impressÃµes se tiver itens
            boolean itensPresentes = listaTabelaItensFrete != null && !listaTabelaItensFrete.isEmpty();
            if (btnImprimirNota != null) btnImprimirNota.setDisable(!itensPresentes);
            if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!itensPresentes);
            if (btnImprimirRecibo != null) btnImprimirRecibo.setDisable(!itensPresentes);
            // --------------------------------------------------

        } catch (SQLException e) {
            AppLogger.error("CadastroFreteController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro ao Carregar Frete", "NÃ£o foi possÃ­vel carregar o frete:\n" + e.getMessage());
            configurarParaNovoFrete();
        }
    }

    private void setComponentProperties() {
        if (cbRemetente != null) cbRemetente.setEditable(true);
        if (cbCliente != null) cbCliente.setEditable(true);
        if (cbRota != null) cbRota.setEditable(false);
        if (cbConferente != null) cbConferente.setEditable(false);
        if (txtCidadeCobranca != null) txtCidadeCobranca.setEditable(true);
        if (txtNumFrete != null) {
            txtNumFrete.setEditable(false);
            txtNumFrete.setText("AutomÃ¡tico");
        }
        if (txtSaida != null) txtSaida.setPromptText("dd/MM/yyyy");
        if (txttotal != null) {
            txttotal.setEditable(false);
            txttotal.setFocusTraversable(false);
        }
        if (txtTotalVol != null) txtTotalVol.setEditable(false);
        if (txtValorTotalNota != null) txtValorTotalNota.setEditable(false);
        if (txtViagemAtual != null) txtViagemAtual.setEditable(false);
    }

    // DM015: metodo extraido de carregarFreteParaEdicao() para separar DB mapping de UI binding
    private void preencherCamposDoFrete(ResultSet rs, String numFrete) throws SQLException {
        programmaticamenteAtualizando = true;
        try {
            freteAtualId = rs.getLong("id_frete");
            if (txtNumFrete != null) txtNumFrete.setText(numFrete);
            if (cbRemetente != null) cbRemetente.setValue(rs.getString("remetente_nome_temp"));
            if (cbCliente != null) cbCliente.setValue(rs.getString("destinatario_nome_temp"));
            if (cbRota != null) cbRota.setValue(rs.getString("rota_temp"));
            java.sql.Date dataSaidaDb = rs.getDate("data_saida_viagem");
            if (txtSaida != null && dataSaidaDb != null) txtSaida.setText(dataSaidaDb.toLocalDate().format(dateFormatter));
            else if (txtSaida != null) txtSaida.clear();
            if (txtLocalTransporte != null) txtLocalTransporte.setText(rs.getString("local_transporte"));
            if (cbConferente != null) cbConferente.setValue(rs.getString("conferente_temp"));
            if (txtCidadeCobranca != null) txtCidadeCobranca.setText(rs.getString("cidade_cobranca"));
            if (txtObs != null) txtObs.setText(rs.getString("observacoes"));

            // Nota fiscal: preserva valores do banco (vieram do OCR web via sync)
            preservadoNumNotaFiscal = rs.getString("num_notafiscal");
            preservadoValorNotaFiscal = rs.getBigDecimal("valor_notafiscal");
            preservadoPesoNotaFiscal = rs.getBigDecimal("peso_notafiscal");
            BigDecimal valorFreteCalculado = rs.getBigDecimal("valor_frete_calculado");

            if (txtValorTotalNota != null) txtValorTotalNota.setText(valorFreteCalculado != null ? df.format(valorFreteCalculado.doubleValue()) : "");

            Long idViagem = rs.getObject("id_viagem", Long.class);
            if (idViagem != null) {
                Viagem viagemAssociada = viagemDAO.buscarViagemPorId(idViagem);
                if (viagemAssociada != null) {
                    if (txtViagemAtual != null) txtViagemAtual.setText(viagemAssociada.getDataViagem().format(dateFormatter));
                    this.viagemAtiva = viagemAssociada;
                } else {
                    if (txtViagemAtual != null) txtViagemAtual.setText("Viagem Não Encontrada");
                    this.viagemAtiva = null;
                }
            } else {
                if (txtViagemAtual != null) txtViagemAtual.clear();
                this.viagemAtiva = null;
            }
        } finally {
            programmaticamenteAtualizando = false;
        }
    }

    private void carregarDadosIniciaisComboBoxes() {
        // DP014: carregar contatos 1x e copiar para ambas as listas (mesma query)
        carregarContatosParaComboBoxes("Remetente", listaRemetentesOriginal);
        listaClientesOriginal.setAll(listaRemetentesOriginal);
        carregarRotas();
        carregarConferentesDoBanco();
        carregarItensCadastradosParaComboBox();
    }

    private void setComboBoxItems() {
        programmaticamenteAtualizando = true;
        try {
            if (cbRemetente != null) cbRemetente.setItems(FXCollections.observableArrayList(listaRemetentesOriginal));
            if (cbCliente != null) cbCliente.setItems(FXCollections.observableArrayList(listaClientesOriginal));
            if (cbRota != null) cbRota.setItems(FXCollections.observableArrayList(listaRotasOriginal));
            if (cbConferente != null) cbConferente.setItems(FXCollections.observableArrayList(listaConferentesOriginal));
            if (cbitem != null) cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
        } finally {
            programmaticamenteAtualizando = false;
        }
    }

    public static class FreteItemCadastro {
        private final SimpleIntegerProperty quantidade;
        private final SimpleStringProperty item;
        private final SimpleDoubleProperty preco;

        public FreteItemCadastro(int qtd, String it, double pr) {
            this.quantidade = new SimpleIntegerProperty(qtd);
            this.item = new SimpleStringProperty(it);
            this.preco = new SimpleDoubleProperty(pr);
        }

        public int getQuantidade() { return quantidade.get(); }
        public void setQuantidade(int q) { quantidade.set(q); }
        public SimpleIntegerProperty quantidadeProperty() { return quantidade; }

        public String getItem() { return item.get(); }
        public void setItem(String s) { item.set(s); }
        public SimpleStringProperty itemProperty() { return item; }

        public double getPreco() { return preco.get(); }
        public void setPreco(double p) { preco.set(p); }
        public SimpleDoubleProperty precoProperty() { return preco; }

        // #DB022: calculo de total via BigDecimal para evitar erros de arredondamento
        public double getTotal() { return getTotalBD().doubleValue(); }
        public java.math.BigDecimal getTotalBD() {
            return java.math.BigDecimal.valueOf(getQuantidade()).multiply(java.math.BigDecimal.valueOf(getPreco()));
        }
    }

    private void configurarTabela() {
        if (tabelaItens == null) return;
        tabelaItens.setEditable(true);
        if (listaTabelaItensFrete != null) {
            tabelaItens.setItems(listaTabelaItensFrete);
        }

        if (colQuantidade != null) {
            colQuantidade.setCellValueFactory(cd -> cd.getValue().quantidadeProperty().asObject());
            colQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            colQuantidade.setOnEditCommit(e -> {
                FreteItemCadastro it = e.getRowValue();
                Integer nV = e.getNewValue();
                Integer oV = e.getOldValue();
                if (nV != null && nV > 0) {
                    it.setQuantidade(nV);
                } else {
                    it.setQuantidade(oV != null ? oV : 1);
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        }

        if (colItem != null) {
            colItem.setCellValueFactory(cd -> cd.getValue().itemProperty());
            colItem.setCellFactory(TextFieldTableCell.forTableColumn());
            colItem.setOnEditCommit(e -> {
                FreteItemCadastro it = e.getRowValue();
                String nV = e.getNewValue();
                if (nV != null && !nV.trim().isEmpty()) {
                    it.setItem(nV.trim());
                } else {
                    it.setItem(e.getOldValue());
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        }

        if (colPreco != null) {
            colPreco.setCellValueFactory(cd -> cd.getValue().precoProperty().asObject());
            colPreco.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
                @Override
                public String toString(Double object) {
                    return (object == null) ? "" : df.format(object);
                }
                @Override
                public Double fromString(String string) {
                    try {
                        return MoneyUtil.parseDouble(string);
                    } catch (NumberFormatException parseException) {
                        return null;
                    }
                }
            }));
            colPreco.setOnEditCommit(event -> {
                FreteItemCadastro itemEditado = event.getRowValue();
                Double novoValor = event.getNewValue();
                Double valorAntigo = event.getOldValue();
                if (novoValor != null && novoValor >= 0) {
                    itemEditado.setPreco(novoValor);
                } else {
                    itemEditado.setPreco(valorAntigo != null ? valorAntigo : 0.0);
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        }

        if (colTotal != null) {
            colTotal.setCellValueFactory(cd ->
                    new SimpleStringProperty(df.format(cd.getValue().getTotal()))
            );
        }
    }

    private void configurarListenersDeCamposEEventos() {
        if (txtquantidade != null) {
            txtquantidade.textProperty().addListener((o, old, n) -> calcularTotalItemEmTempoReal());
        }

        if (txtpreco != null) {
            txtpreco.textProperty().addListener((o, old, n) -> calcularTotalItemEmTempoReal());
            txtpreco.focusedProperty().addListener((o, oldV, newV) -> {
                if (!newV && txtpreco.getText() != null && !txtpreco.getText().isEmpty()) {
                    try {
                        double v = MoneyUtil.parseDouble(txtpreco.getText());
                        txtpreco.setText(df.format(v));
                    } catch (NumberFormatException e) {
                        AppLogger.warn("CadastroFreteController", "CadastroFreteController: erro ao formatar valor monetario '" + txtpreco.getText() + "' — " + e.getMessage());
                    }
                }
            });
        }

        if (precoToggleGroup != null) {
            precoToggleGroup.selectedToggleProperty().addListener((o, old, n) -> {
                if (programmaticamenteAtualizando) return;
                if (cbitem != null && cbitem.getValue() != null) {
                    processarItemDigitadoOuSelecionado(cbitem.getValue().getNomeItem());
                } else {
                    if (txtpreco != null) txtpreco.clear();
                    if (txttotal != null) txttotal.clear();
                }
            });
        }
        
        if (cbRemetente != null && cbCliente != null) {
            cbRemetente.getEditor().setOnKeyPressed(e -> {
                 if(e.getCode() == KeyCode.ENTER) cbCliente.requestFocus();
            });
        }
        if (cbCliente != null && cbRota != null) {
            cbCliente.getEditor().setOnKeyPressed(e -> {
                 if(e.getCode() == KeyCode.ENTER) cbRota.requestFocus();
            });
        }
        
        if (cbRota != null && txtSaida != null) {
            cbRota.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    if (cbRota.isShowing()) {
                        cbRota.hide();
                        e.consume();
                    } else {
                        Platform.runLater(() -> txtSaida.requestFocus());
                        e.consume(); 
                    }
                }
            });
        }
        
        if (txtSaida != null && txtLocalTransporte != null) setEnterNavigation(txtSaida, txtLocalTransporte);
        if (txtLocalTransporte != null && cbConferente != null) setEnterNavigation(txtLocalTransporte, cbConferente);
        
        if (cbConferente != null && txtCidadeCobranca != null) {
            cbConferente.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    if (cbConferente.isShowing()) {
                        cbConferente.hide();
                        e.consume();
                    } else {
                        Platform.runLater(() -> txtCidadeCobranca.requestFocus());
                        e.consume(); 
                    }
                }
            });
        }
        
        if (txtCidadeCobranca != null && txtObs != null) setEnterNavigation(txtCidadeCobranca, txtObs);
        if (txtObs != null && txtquantidade != null) setEnterNavigation(txtObs, txtquantidade);
        if (txtquantidade != null && cbitem != null) setEnterNavigation(txtquantidade, cbitem.getEditor());
        // ENTER no cbitem é tratado pelo addEventFilter em configurarComboBoxItem()

        if (txtpreco != null && btnInserir != null) {
            txtpreco.setOnAction(e -> {
                if (!btnInserir.isDisable()) {
                    btnInserir.fire();
                    if (txtquantidade != null && txtquantidade.isFocusTraversable()) {
                        txtquantidade.requestFocus();
                    }
                }
            });
        }

        if (listaTabelaItensFrete != null) {
            listaTabelaItensFrete.addListener((ListChangeListener<FreteItemCadastro>) c -> {
                if (programmaticamenteAtualizando) return;
                atualizarTotaisAgregados();
                boolean itensPresentes = !listaTabelaItensFrete.isEmpty();
                boolean podeImprimir = freteAtualId != -1 || (btnSalvar != null && !btnSalvar.isDisabled());

                if (btnImprimirNota != null) btnImprimirNota.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirRecibo != null) btnImprimirRecibo.setDisable(!(podeImprimir && itensPresentes));
            });
        }
    }

    private void setEnterNavigation(Node sourceNode, Node targetNode) {
        if (sourceNode == null || targetNode == null) return;
        if (sourceNode instanceof TextField) {
            ((TextField) sourceNode).setOnAction(e -> {
                if (targetNode.isFocusTraversable()) targetNode.requestFocus();
            });
        } else if (sourceNode instanceof ComboBox) {
            ComboBox<?> comboBox = (ComboBox<?>) sourceNode;
            if (comboBox.isEditable() && comboBox.getEditor() != null) {
                comboBox.getEditor().setOnAction(e -> {
                    if (targetNode.isFocusTraversable()) targetNode.requestFocus();
                });
            } else {
                sourceNode.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        if (targetNode.isFocusTraversable()) targetNode.requestFocus();
                        e.consume();
                    }
                });
            }
        }
    }

    private void processarContatoDigitado(ComboBox<String> comboBox, String nomeContatoInput,
                                          ObservableList<String> listaContatosInput, String tipoContatoContexto) {
        if (programmaticamenteAtualizando) return;
        final ObservableList<String> listaContatos = (listaContatosInput != null) ? listaContatosInput : FXCollections.observableArrayList();
        if (nomeContatoInput == null || nomeContatoInput.trim().isEmpty()) return;
        final String nomeContato = nomeContatoInput.trim();

        if (tipoContatoContexto.equals("Remetente")) {
            if (processandoContatoRemetente) return;
            processandoContatoRemetente = true;
        } else if (tipoContatoContexto.equals("Cliente")) {
            if (processandoContatoCliente) return;
            processandoContatoCliente = true;
        }

        try {
            boolean encontrado = false;
            String contatoEncontradoNaLista = null;
            for (String contatoExistente : listaContatos) {
                if (contatoExistente != null && contatoExistente.equalsIgnoreCase(nomeContato)) {
                    encontrado = true;
                    contatoEncontradoNaLista = contatoExistente;
                    break;
                }
            }

            if (encontrado) {
                final String finalContato = contatoEncontradoNaLista;
                javafx.application.Platform.runLater(() -> {
                    // Verificar se o editor ainda tem texto (pode ter sido limpo por limparCamposFrete)
                    String editorText = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
                    if (editorText == null || editorText.trim().isEmpty()) return;
                    programmaticamenteAtualizando = true;
                    try {
                        if (comboBox.getValue() == null || !comboBox.getValue().equalsIgnoreCase(finalContato)) {
                            comboBox.setValue(finalContato);
                        }
                    } finally {
                        programmaticamenteAtualizando = false;
                    }
                });
            } else {
                Alert confirmacao = new Alert(AlertType.CONFIRMATION);
                confirmacao.setTitle("Cadastrar Novo " + tipoContatoContexto);
                confirmacao.setHeaderText(tipoContatoContexto + " nÃ£o encontrado: '" + nomeContato + "'");
                confirmacao.setContentText("Deseja cadastrar este novo " + tipoContatoContexto.toLowerCase() + "?");
                ButtonType btnSim = new ButtonType("Sim, Cadastrar");
                ButtonType btnNao = new ButtonType("NÃ£o");
                confirmacao.getButtonTypes().setAll(btnSim, btnNao, ButtonType.CANCEL);

                Optional<ButtonType> resultado = confirmacao.showAndWait();
                if (resultado.isPresent() && resultado.get() == btnSim) {
                    // DM057: SQL movido para FreteDAO.inserirContato()
                    try {
                        String novoNomeContato = freteDAO.inserirContato(nomeContato);
                        if (!listaContatos.contains(novoNomeContato)) {
                            listaContatos.add(novoNomeContato);
                            FXCollections.sort(listaContatos);
                        }
                        final String finalNome = novoNomeContato;
                        javafx.application.Platform.runLater(() -> {
                            String editorText = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
                            if (editorText == null || editorText.trim().isEmpty()) return;
                            programmaticamenteAtualizando = true;
                            try {
                                comboBox.setValue(finalNome);
                            } finally {
                                programmaticamenteAtualizando = false;
                            }
                        });
                    } catch (SQLException e) {
                        AppLogger.error("CadastroFreteController", e.getMessage(), e);
                    }
                } else {
                    if (comboBox.isEditable() && comboBox.getEditor() != null) {
                        comboBox.getEditor().clear();
                    }
                    comboBox.setValue(null);
                }
            }
        } finally {
            if (tipoContatoContexto.equals("Remetente")) processandoContatoRemetente = false;
            else if (tipoContatoContexto.equals("Cliente")) processandoContatoCliente = false;
        }
    }

    private void processarItemDigitadoOuSelecionado(String nomeItemInput) {
        if (nomeItemInput == null || nomeItemInput.trim().isEmpty()) {
            if (txtpreco != null) txtpreco.clear();
            if (txttotal != null) txttotal.clear();
            return;
        }
        String nomeItem = nomeItemInput.trim().toLowerCase();

        if (programmaticamenteAtualizando || processandoItemCbItem) {
            if (mapItensCadastrados.containsKey(nomeItem) && txtpreco != null) {
                ItemFrete itemCadastrado = mapItensCadastrados.get(nomeItem);
                double precoASerUsado = 0.0;
                if (rbNormal != null && rbNormal.isSelected()) {
                    precoASerUsado = itemCadastrado.getPrecoUnitarioPadrao().doubleValue();
                } else if (rbComDesconto != null && rbComDesconto.isSelected()) {
                    precoASerUsado = itemCadastrado.getPrecoUnitarioDesconto().doubleValue();
                }
                txtpreco.setText(df.format(precoASerUsado));
                calcularTotalItemEmTempoReal();
            }
            return;
        }
        processandoItemCbItem = true;
        ultimoItemProcessadoCbItem = nomeItem;

        try {
            if (mapItensCadastrados.containsKey(nomeItem)) {
                ItemFrete itemCadastrado = mapItensCadastrados.get(nomeItem);
                double precoASerUsado = 0.0;
                if (rbNormal != null && rbNormal.isSelected()) {
                    precoASerUsado = itemCadastrado.getPrecoUnitarioPadrao().doubleValue();
                } else if (rbComDesconto != null && rbComDesconto.isSelected()) {
                    precoASerUsado = itemCadastrado.getPrecoUnitarioDesconto().doubleValue();
                }
                if (txtpreco != null) txtpreco.setText(df.format(precoASerUsado));
                calcularTotalItemEmTempoReal();
                if (txtpreco != null && txtpreco.isFocusTraversable()) {
                    txtpreco.requestFocus();
                }
            } else {
                Alert c = new Alert(AlertType.CONFIRMATION);
                c.setTitle("Cadastrar Novo Item de Frete");
                c.setHeaderText("Item nÃ£o encontrado: '" + nomeItemInput.trim() + "'");
                c.setContentText("Deseja cadastrar este novo item?");
                ButtonType bS = new ButtonType("Sim, Cadastrar");
                ButtonType bN = new ButtonType("NÃ£o");
                c.getButtonTypes().setAll(bS, bN, ButtonType.CANCEL);
                Optional<ButtonType> res = c.showAndWait();
                if (res.isPresent() && res.get() == bS) {
                    abrirDialogCadastroNovoItem(nomeItemInput.trim());
                } else {
                    if (txtpreco != null) txtpreco.clear();
                    if (txttotal != null) txttotal.clear();
                    if (cbitem != null) {
                        programmaticamenteAtualizando = true;
                        try {
                            cbitem.getSelectionModel().clearSelection();
                            if (cbitem.isEditable() && cbitem.getEditor() != null) {
                                cbitem.getEditor().clear();
                            }
                            cbitem.setValue(null);
                        } finally {
                            programmaticamenteAtualizando = false;
                        }
                    }
                    ultimoItemProcessadoCbItem = null;
                }
            }
        } finally {
            processandoItemCbItem = false;
        }
    }

    // DM057: queries movidas para FreteDAO
    private void carregarContatosParaComboBoxes(String tipo, ObservableList<String> lista) {
        if (lista == null) return;
        lista.clear();
        lista.addAll(freteDAO.listarContatos());
    }

    private void carregarRotas() {
        if (listaRotasOriginal == null) listaRotasOriginal = FXCollections.observableArrayList();
        listaRotasOriginal.clear();
        listaRotasOriginal.addAll(freteDAO.listarRotasFormatadas());
    }

    private void carregarConferentesDoBanco() {
        if (listaConferentesOriginal == null) listaConferentesOriginal = FXCollections.observableArrayList();
        listaConferentesOriginal.clear();
        listaConferentesOriginal.addAll(freteDAO.listarNomesConferentes());
    }

    private void carregarItensCadastradosParaComboBox() {
        if (listaItensDisplayOriginal == null) listaItensDisplayOriginal = FXCollections.observableArrayList();
        if (mapItensCadastrados == null) mapItensCadastrados = new HashMap<>();
        listaItensDisplayOriginal.clear();
        mapItensCadastrados.clear();

        ItemFreteDAO dao = new ItemFreteDAO();
        List<ItemFrete> todosAtivos = dao.listarTodos(false);
        for (ItemFrete item : todosAtivos) {
            if (item != null && item.getNomeItem() != null) {
                String desc = item.getNomeItem().toLowerCase();
                if (!listaItensDisplayOriginal.contains(item)) {
                    listaItensDisplayOriginal.add(item);
                }
                mapItensCadastrados.put(desc, item);
            }
        }
    }

    private void abrirDialogCadastroNovoItem(String descSugerida) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TabelaPrecoFrete.fxml"));
            Parent root = loader.load();
            TabelaPrecoFreteController controller = loader.getController();
            if (controller != null) {
                controller.setDescricaoTexto(descSugerida);
            }
            Stage stage = new Stage();
            stage.setTitle("Cadastrar Novo Item");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait(); 
            
            programmaticamenteAtualizando = true;
            try {
                carregarItensCadastradosParaComboBox();
                if (cbitem != null) {
                    cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
                }
            } finally {
                programmaticamenteAtualizando = false;
            }

            if (cbitem != null) {
                String nomeItemProcurado = descSugerida.trim().toLowerCase();
                for(ItemFrete item : listaItensDisplayOriginal) {
                    if(item != null && item.getNomeItem().trim().equalsIgnoreCase(nomeItemProcurado)) {
                        programmaticamenteAtualizando = true;
                        try {
                            cbitem.setValue(item);
                            processarItemDigitadoOuSelecionado(item.getNomeItem());
                        } finally {
                            programmaticamenteAtualizando = false;
                        }
                        break;
                    }
                }
                if(txtquantidade != null) {
                    txtquantidade.requestFocus();
                    txtquantidade.selectAll();
                }
            }
        } catch (IOException e) {
            AppLogger.error("CadastroFreteController", e.getMessage(), e);
        }
    }

    // DM057: movido para FreteDAO.gerarNumeroFrete()
    private long gerarNumeroFreteNoBanco() throws SQLException {
        return freteDAO.gerarNumeroFrete();
    }

    @FXML private void handleNovoFrete(ActionEvent event) {
        configurarParaNovoFrete();
        habilitarCamposParaEdicao(true);
        freteAtualId = -1;
        if (txtNumFrete != null) txtNumFrete.setText("AutomÃ¡tico");
        if (cbRemetente != null) cbRemetente.requestFocus();
        if (btnNovo != null) btnNovo.setDisable(true);
        if (btnSalvar != null) btnSalvar.setDisable(false);
        if (btnAlterar != null) btnAlterar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }

    @FXML private void handleSalvarFrete(ActionEvent event) {
        salvarOuAlterarFrete();
    }

    @FXML private void handleAlterarFrete(ActionEvent event) {
        if (freteAtualId == -1) {
            AlertHelper.show(AlertType.WARNING, "Aviso", "NÃ£o hÃ¡ frete selecionado para alteraÃ§Ã£o.");
            return;
        }
        habilitarCamposParaEdicao(true);
        if (btnNovo != null) btnNovo.setDisable(true);
        if (btnSalvar != null) btnSalvar.setDisable(false);
        if (btnAlterar != null) btnAlterar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }

    // DM057: refatorado — logica de negocio extraida para FreteService + FreteDAO
    private void salvarOuAlterarFrete() {
        if (!ValidationHelper.requiredCombo(cbRemetente, "Remetente")) return;
        if (!ValidationHelper.requiredCombo(cbCliente, "Cliente (Destinatário)")) return;
        if (!ValidationHelper.requiredCombo(cbRota, "Rota")) return;
        if (!ValidationHelper.requiredList(listaTabelaItensFrete, txtquantidade, "Itens do Frete")) return;

        boolean isNovo = (freteAtualId == -1);

        // 1. Coletar dados do formulario (UI → DTO)
        FreteData data = new FreteData();
        data.idFrete = freteAtualId;
        data.numeroFrete = freteAtualId;
        if (txtSaida != null && !txtSaida.getText().trim().isEmpty()) {
            data.dataSaida = LocalDate.parse(txtSaida.getText().trim(), dateFormatter);
        }
        data.localTransporte = txtLocalTransporte != null ? txtLocalTransporte.getText() : null;
        data.remetente = cbRemetente.getValue();
        data.destinatario = cbCliente.getValue();
        data.rota = cbRota.getValue();
        data.conferente = cbConferente.getValue();
        data.cidadeCobranca = txtCidadeCobranca.getText();
        data.observacoes = txtObs != null ? txtObs.getText() : null;

        // Nota fiscal não é editada no desktop — preserva o que veio do banco (populado via sync do OCR web).
        data.numNotaFiscal = preservadoNumNotaFiscal;
        data.valorNotaFiscal = preservadoValorNotaFiscal != null ? preservadoValorNotaFiscal : BigDecimal.ZERO;
        data.pesoNotaFiscal = preservadoPesoNotaFiscal != null ? preservadoPesoNotaFiscal : BigDecimal.ZERO;

        if (txtValorTotalNota != null && !txtValorTotalNota.getText().isEmpty()) {
            data.valorFreteCalculado = MoneyUtil.parseBigDecimal(txtValorTotalNota.getText());
        }

        // 2. Coletar itens (UI → DTO)
        List<FreteItemData> itens = new ArrayList<>();
        for (FreteItemCadastro it : listaTabelaItensFrete) {
            FreteItemData itemData = new FreteItemData();
            itemData.nomeItem = it.getItem();
            itemData.quantidade = it.getQuantidade();
            itemData.precoUnitario = BigDecimal.valueOf(it.getPreco());
            itemData.subtotal = it.getTotalBD();
            itens.add(itemData);
        }

        // 3. Delegar para o service (logica de negocio + transacao)
        Long idViagem = this.viagemAtiva != null ? this.viagemAtiva.getId() : null;
        ResultadoFrete resultado = freteService.salvarOuAlterar(data, itens, isNovo, idViagem);

        // 4. Atualizar UI com resultado
        if (resultado.sucesso) {
            AlertHelper.show(AlertType.INFORMATION, "Sucesso", resultado.mensagem);
            if (isNovo) {
                freteAtualId = resultado.numeroFrete;
                if (txtNumFrete != null) {
                    programmaticamenteAtualizando = true;
                    try { txtNumFrete.setText(String.valueOf(resultado.numeroFrete)); }
                    finally { programmaticamenteAtualizando = false; }
                }
                configurarParaNovoFrete();
                habilitarCamposParaEdicao(true);
                freteAtualId = -1;
                Platform.runLater(() -> { if (cbRemetente != null) cbRemetente.requestFocus(); });
            } else {
                habilitarCamposParaVisualizacao(true);
                if (btnNovo != null) btnNovo.setDisable(false);
                if (btnSalvar != null) btnSalvar.setDisable(true);
                if (btnAlterar != null) btnAlterar.setDisable(false);
                if (btnExcluir != null) btnExcluir.setDisable(false);
            }
        } else {
            AlertHelper.show(AlertType.ERROR, "Erro na Operacao do Frete", resultado.mensagem);
        }
    }

    @FXML private void adicionarItemTabela(ActionEvent event) {
        if (txtquantidade == null || cbitem == null || txtpreco == null || tabelaItens == null) {
            return;
        }
        if (btnSalvar.isDisabled() && btnAlterar.isDisabled()) {
            return;
        }

        String qtdStr = txtquantidade.getText().trim();
        String itemNomeOuDescricao = null;
        if (cbitem.getValue() != null) {
            itemNomeOuDescricao = cbitem.getValue().getNomeItem();
        } else if (cbitem.getEditor() != null) {
            itemNomeOuDescricao = cbitem.getEditor().getText();
        }
        String precoStr = txtpreco.getText().trim();

        if (!ValidationHelper.positiveInt(txtquantidade, "Quantidade")) return;
        if (itemNomeOuDescricao == null || itemNomeOuDescricao.trim().isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Item deve ser informado.");
            cbitem.requestFocus();
            return;
        }
        if (!ValidationHelper.nonNegativeDouble(txtpreco, "Preço Unitário")) return;

        int quantidade = Integer.parseInt(qtdStr);
        double precoUnitario = MoneyUtil.parseDouble(precoStr);

        // DL065: preservar case original (toUpperCase para documentos fiscais)
        String nomeItemFinal = itemNomeOuDescricao.trim().toUpperCase();
        if (listaTabelaItensFrete != null) {
            listaTabelaItensFrete.add(new FreteItemCadastro(quantidade, nomeItemFinal, precoUnitario));
        }
        limparCamposItem();
        if (txtquantidade != null && txtquantidade.isFocusTraversable()) {
            txtquantidade.requestFocus();
        }
    }

    @FXML private void handleExcluirFrete(ActionEvent event) {
        if (freteAtualId == -1) {
            AlertHelper.show(AlertType.WARNING, "Aviso", "NÃ£o hÃ¡ frete selecionado para exclusÃ£o.");
            return;
        }

        Alert confirmacao = new Alert(AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar ExclusÃ£o");
        confirmacao.setHeaderText("Excluir Frete " + txtNumFrete.getText() + "?");
        confirmacao.setContentText("Esta aÃ§Ã£o nÃ£o pode ser desfeita. Deseja realmente excluir este frete e todos os seus itens?");
        Optional<ButtonType> result = confirmacao.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Connection conn = null;
            try {
                conn = ConexaoBD.getConnection();
                conn.setAutoCommit(false);

                String sqlDeleteItems = "DELETE FROM frete_itens WHERE id_frete = ?";
                try (PreparedStatement pstItems = conn.prepareStatement(sqlDeleteItems)) {
                    pstItems.setLong(1, freteAtualId);
                    pstItems.executeUpdate();
                }

                String sqlDeleteFrete = "DELETE FROM fretes WHERE id_frete = ? AND empresa_id = ?";
                try (PreparedStatement pstFrete = conn.prepareStatement(sqlDeleteFrete)) {
                    pstFrete.setLong(1, freteAtualId);
                    pstFrete.setInt(2, DAOUtils.empresaId());
                    pstFrete.executeUpdate();
                }

                conn.commit();
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Frete " + txtNumFrete.getText() + " excluÃ­do com sucesso!");
                limparCamposFrete();
                habilitarCamposParaVisualizacao(false);
                if (btnNovo != null) btnNovo.setDisable(false);
                if (btnSalvar != null) btnSalvar.setDisable(true);
                if (btnAlterar != null) btnAlterar.setDisable(true);
                if (btnExcluir != null) btnExcluir.setDisable(true);
                freteAtualId = -1;
            } catch (SQLException e) {
                try { if (conn != null) conn.rollback(); } catch (SQLException ex) { AppLogger.error("CadastroFreteController", ex.getMessage(), ex); }
                AlertHelper.show(AlertType.ERROR, "Erro ao Excluir Frete", "NÃ£o foi possÃ­vel excluir o frete:\n" + e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException ex) { AppLogger.error("CadastroFreteController", ex.getMessage(), ex); }
            }
        }
    }

    @FXML private void imprimirNotaFretePersonalizada(ActionEvent event) {
        if (freteAtualId == -1) {
            AlertHelper.show(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir a nota.");
            return;
        }
        AlertHelper.show(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Nota Fiscal do Frete - NÃ£o implementado.");
    }
    @FXML private void imprimirReciboPersonalizado(ActionEvent event) {
        if (freteAtualId == -1) {
            AlertHelper.show(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir o recibo.");
            return;
        }
        AlertHelper.show(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Recibo do Frete - NÃ£o implementado.");
    }

    @FXML private void abrirListaFretes(ActionEvent event) {
        abrirJanelaGenerica("ListaFretes.fxml", "Lista de Fretes", true, Modality.APPLICATION_MODAL);
    }
    @FXML private void handleSair(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }

    @FXML private void handleImprimirEtiqueta(ActionEvent event) {
        if (freteAtualId == -1) {
            AlertHelper.show(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir etiquetas.");
            return;
        }
        AlertHelper.show(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Etiqueta(s) - NÃ£o implementado.");
    }

    private void habilitarCamposParaEdicao(boolean habilitar) {
        if (cbRemetente != null) cbRemetente.setDisable(!habilitar);
        if (cbCliente != null) cbCliente.setDisable(!habilitar);
        if (cbRota != null) cbRota.setDisable(!habilitar);
        if (txtSaida != null) txtSaida.setDisable(!habilitar);
        if (txtLocalTransporte != null) txtLocalTransporte.setDisable(!habilitar);
        if (cbConferente != null) cbConferente.setDisable(!habilitar);
        if (txtCidadeCobranca != null) txtCidadeCobranca.setDisable(!habilitar);

        if (txtObs != null) txtObs.setDisable(!habilitar);
        if (rbComDesconto != null) rbComDesconto.setDisable(!habilitar);
        if (rbNormal != null) rbNormal.setDisable(!habilitar);
        if (txtquantidade != null) txtquantidade.setDisable(!habilitar);
        if (cbitem != null) cbitem.setDisable(!habilitar); 
        if (txtpreco != null) txtpreco.setDisable(!habilitar);
        if (btnInserir != null) btnInserir.setDisable(!habilitar);
        if (tabelaItens != null) {
            tabelaItens.setEditable(habilitar);
            if (colQuantidade != null) colQuantidade.setEditable(habilitar);
            if (colItem != null) colItem.setEditable(habilitar);
            if (colPreco != null) colPreco.setEditable(habilitar);
            tabelaItens.setDisable(!habilitar);
        }
    }

    private void habilitarCamposParaVisualizacao(boolean habilitar) {
        habilitarCamposParaEdicao(false);

        boolean itensPresentes = listaTabelaItensFrete != null && !listaTabelaItensFrete.isEmpty();
        boolean podeImprimir = habilitar && itensPresentes && freteAtualId != -1;

        if (btnImprimirNota != null) btnImprimirNota.setDisable(!podeImprimir);
        if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!podeImprimir);
        if (btnImprimirRecibo != null) btnImprimirRecibo.setDisable(!podeImprimir);
    }

    private void limparCamposFrete() {
        programmaticamenteAtualizando = true;
        try {
            if (txtNumFrete != null) txtNumFrete.setText("AutomÃ¡tico");
            if (cbRemetente != null) {
                cbRemetente.getSelectionModel().clearSelection();
                if (cbRemetente.isEditable() && cbRemetente.getEditor() != null) {
                    cbRemetente.getEditor().clear();
                }
                cbRemetente.setValue(null);
                if (listaRemetentesOriginal != null) {
                    cbRemetente.setItems(FXCollections.observableArrayList(listaRemetentesOriginal));
                }
            }
            if (cbCliente != null) {
                cbCliente.getSelectionModel().clearSelection();
                if (cbCliente.isEditable() && cbCliente.getEditor() != null) {
                    cbCliente.getEditor().clear();
                }
                cbCliente.setValue(null);
                if (listaClientesOriginal != null) {
                    cbCliente.setItems(FXCollections.observableArrayList(listaClientesOriginal));
                }
            }
            if (cbRota != null) {
                cbRota.getSelectionModel().clearSelection();
                cbRota.setValue(null);
            }
            if (txtSaida != null) txtSaida.clear();
            if (txtLocalTransporte != null) txtLocalTransporte.clear();
            if (txtViagemAtual != null) txtViagemAtual.clear();
            if (cbConferente != null) {
                cbConferente.getSelectionModel().clearSelection();
                cbConferente.setValue(null);
            }
            if (txtCidadeCobranca != null) {
                txtCidadeCobranca.clear(); 
            }
            preservadoNumNotaFiscal = null;
            preservadoValorNotaFiscal = null;
            preservadoPesoNotaFiscal = null;
            if (txtObs != null) txtObs.clear();
            if (rbNormal != null && precoToggleGroup != null) rbNormal.setSelected(true);
            limparCamposItem();
            if (listaTabelaItensFrete != null) {
                listaTabelaItensFrete.clear();
            }
            atualizarTotaisAgregados();
        } finally {
            programmaticamenteAtualizando = false;
        }
    }

    private void limparCamposItem() {
        if (txtquantidade != null) txtquantidade.clear();
        if (cbitem != null) {
            programmaticamenteAtualizando = true;
            try {
                cbitem.getSelectionModel().clearSelection();
                if (cbitem.isEditable() && cbitem.getEditor() != null) {
                    cbitem.getEditor().clear();
                }
                cbitem.setValue(null);
            } finally {
                programmaticamenteAtualizando = false;
            }
        }
        if (txtpreco != null) txtpreco.clear();
        if (txttotal != null) txttotal.clear();
        ultimoItemProcessadoCbItem = null;
    }

    private void atualizarTotaisAgregados() {
        int totalDeVolumes = 0;
        // #DB022: BigDecimal para soma de valores financeiros
        java.math.BigDecimal valorTotalAgregado = java.math.BigDecimal.ZERO;
        if (listaTabelaItensFrete != null) {
            for (FreteItemCadastro item : listaTabelaItensFrete) {
                totalDeVolumes += item.getQuantidade();
                valorTotalAgregado = valorTotalAgregado.add(item.getTotalBD());
            }
        }
        if (txtTotalVol != null) {
            programmaticamenteAtualizando = true;
            try {
                txtTotalVol.setText(String.valueOf(totalDeVolumes));
            } finally {
                programmaticamenteAtualizando = false;
            }
        }
        if (txtValorTotalNota != null) {
            programmaticamenteAtualizando = true;
            try {
                txtValorTotalNota.setText(df.format(valorTotalAgregado));
            } finally {
                programmaticamenteAtualizando = false;
            }
        }
    }

    private void calcularTotalItemEmTempoReal() {
        if (programmaticamenteAtualizando) return;
        if (txtquantidade == null || txtpreco == null || txttotal == null) return;
        String sQ = txtquantidade.getText();
        String sP = txtpreco.getText();
        if (sQ == null || sP == null || sQ.trim().isEmpty() || sP.trim().isEmpty()) {
            programmaticamenteAtualizando = true;
            try {
                txttotal.clear();
            } finally {
                programmaticamenteAtualizando = false;
            }
            return;
        }
        try {
            int q = Integer.parseInt(sQ.trim());
            double p = MoneyUtil.parseDouble(sP.trim());
            if (q > 0 && p >= 0) {
                programmaticamenteAtualizando = true;
                try {
                    txttotal.setText(df.format(q * p));
                } finally {
                    programmaticamenteAtualizando = false;
                }
            } else {
                programmaticamenteAtualizando = true;
                try {
                    txttotal.clear();
                } finally {
                    programmaticamenteAtualizando = false;
                }
            }
        } catch (NumberFormatException e) {
            programmaticamenteAtualizando = true;
            try {
                txttotal.clear();
            } finally {
                programmaticamenteAtualizando = false;
            }
        }
    }

    private void abrirJanelaGenerica(String fxmlFileRelative, String title, boolean resizable, Modality modality) {
        try {
            String fxmlPath = fxmlFileRelative.startsWith("/") ? fxmlFileRelative : "/gui/" + fxmlFileRelative;
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                AlertHelper.show(AlertType.ERROR, "Erro ao Abrir Tela", "Arquivo FXML nÃ£o pÃ´de ser localizado: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setResizable(resizable);
            if (modality != null && modality != Modality.NONE) {
                stage.initModality(modality);
                stage.showAndWait();
            } else {
                stage.show();
            }
        } catch (IOException e) {
            AppLogger.error("CadastroFreteController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro ao Abrir Tela", "NÃ£o foi possÃ­vel carregar a tela: " + fxmlFileRelative + "\nDetalhes: " + e.getMessage());
        } catch (Exception e) {
            AppLogger.error("CadastroFreteController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro CrÃ­tico", "Ocorreu um erro inesperado ao tentar abrir a tela '" + title + "'.");
        }
    }

    public void atualizarListaItensDoComboBox() {
        carregarItensCadastradosParaComboBox();
        setComboBoxItems();
    }
}