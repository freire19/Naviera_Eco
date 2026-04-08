package gui;

import dao.ItemFreteDAO;
import dao.ConexaoBD;
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

/**
 * Controller da tela CadastroFrete.fxml.
 * VERSÃƒO CORRIGIDA: MÃ©todo PUBLIC e EdiÃ§Ã£o Direta.
 */
public class CadastroFreteController implements Initializable {

    // VARIÃVEL ESTÃTICA PARA RECEBER O NÃšMERO DO FRETE
    private static String staticNumeroFreteParaAbrir = null;

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
    @FXML private TextField txtNumNota;
    @FXML private TextField txtValorNota;
    @FXML private TextField txtPesoNota;
    @FXML private TextArea txtObs;
    @FXML private TextField txtquantidade;
    @FXML private TextField txtpreco;
    @FXML private TextField txttotal;
    @FXML private TextField txtTotalVol;
    @FXML private TextField txtValorTotalNota;
    @FXML private RadioButton rbSim;
    @FXML private RadioButton Rbnao;
    @FXML private RadioButton rbComDesconto;
    @FXML private RadioButton rbNormal;
    @FXML private Button btnFotoNota;
    @FXML private Button btnCodXml;
    @FXML private Button btnAudio;
    @FXML private Button btnInserir;
    @FXML private Button btnNovo;
    @FXML private Button btnAlterar;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button BtnSair;
    @FXML private Button BtnImprimirNota;
    @FXML private Button btnImprimirEtiqueta;
    @FXML private Button btnListaDeFrete;
    @FXML private Button btnImprimirRecibo;
    @FXML private TableView<FreteItem> tabelaItens;
    @FXML private TableColumn<FreteItem, Integer> colQuantidade;
    @FXML private TableColumn<FreteItem, String> colItem;
    @FXML private TableColumn<FreteItem, Double> colPreco;
    @FXML private TableColumn<FreteItem, String> colTotal;
    @FXML private ToggleGroup notaFiscalToggleGroup;
    @FXML private ToggleGroup precoToggleGroup;
    //</editor-fold>

    private ObservableList<String> listaRemetentesOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaClientesOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaRotasOriginal = FXCollections.observableArrayList();
    private ObservableList<String> listaConferentesOriginal = FXCollections.observableArrayList();
    private ObservableList<ItemFrete> listaItensDisplayOriginal = FXCollections.observableArrayList();

    private ObservableList<FreteItem> listaTabelaItensFrete = FXCollections.observableArrayList();
    private Map<String, ItemFrete> mapItensCadastrados = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("'R$ '#,##0.00", new DecimalFormatSymbols(new Locale("pt","BR")));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private boolean programmaticamenteAtualizando = false;
    private boolean processandoItemCbItem = false;
    private boolean processandoContatoRemetente= false;
    private boolean processandoContatoCliente = false;
    private String ultimoItemProcessadoCbItem = null;

    private long freteAtualId = -1; 
    private ViagemDAO viagemDAO;
    private Viagem viagemAtiva;

    private ContextMenu menuSugestoesRemetente;
    private ContextMenu menuSugestoesCliente;
    private ContextMenu menuSugestoesCbItem;
    private int indiceSugestaoCbItem = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.viagemDAO = new ViagemDAO();
        this.menuSugestoesRemetente = new ContextMenu();
        this.menuSugestoesCliente = new ContextMenu();
        this.menuSugestoesCbItem = new ContextMenu();

        setComponentProperties();
        configurarComboBoxItem();
        configurarTabela();

        // DR010: carrega dados do banco em background para nao bloquear UI
        Thread bgThread = new Thread(() -> {
            carregarDadosIniciaisComboBoxes();
            javafx.application.Platform.runLater(this::setComboBoxItems);
        });
        bgThread.setDaemon(true);
        bgThread.start();
        configurarListenersDeCamposEEventos();

        if (cbRota != null) {
            cbRota.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.contains("-")) {
                    String[] partes = newVal.split("-");
                    if (partes.length > 1 && txtCidadeCobranca != null) {
                        txtCidadeCobranca.setText(partes[1].trim());
                    }
                }
            });
        }

        configurarAutoCompleteClienteGoogleStyle(cbRemetente, menuSugestoesRemetente, listaRemetentesOriginal);
        configurarAutoCompleteClienteGoogleStyle(cbCliente, menuSugestoesCliente, listaClientesOriginal);
        configurarValidacaoFocoClientesGoogle();

        configurarComboboxParaAbrirAoFocar(cbRota);
        configurarComboboxParaAbrirAoFocar(cbConferente);

        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F1) { if(btnNovo != null && !btnNovo.isDisabled()) handleNovoFrete(null); event.consume(); }
                    if (event.getCode() == KeyCode.F2) { if(btnAlterar != null && !btnAlterar.isDisabled()) handleAlterarFrete(null); event.consume(); }
                    if (event.getCode() == KeyCode.F3) { if(btnSalvar != null && !btnSalvar.isDisabled()) handleSalvarFrete(null); event.consume(); }
                    if (event.getCode() == KeyCode.F4) { if(btnExcluir != null && !btnExcluir.isDisabled()) handleExcluirFrete(null); event.consume(); }
                    if (event.getCode() == KeyCode.F5) { if(btnListaDeFrete != null && !btnListaDeFrete.isDisabled()) abrirListaFretes(null); event.consume(); }
                    if (event.getCode() == KeyCode.F6) { if(BtnImprimirNota != null && !BtnImprimirNota.isDisabled()) imprimirNotaFretePersonalizada(null); event.consume(); }
                    if (event.getCode() == KeyCode.ESCAPE) { handleSair(null); event.consume(); }
                });
            }
        });

        aplicarEstiloBotoesIA();

        if (staticNumeroFreteParaAbrir != null) {
            carregarFreteParaEdicao(staticNumeroFreteParaAbrir);
            staticNumeroFreteParaAbrir = null;
        } else {
            configurarParaNovoFrete();
        }
    }

    private void aplicarEstiloBotoesIA() {
        String estiloAzulForte = "-fx-background-color: #0d47a1; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;";
        if(btnAudio != null) {
            btnAudio.setStyle(estiloAzulForte);
            btnAudio.setText("Voz");
        }
        if(btnFotoNota != null) {
            btnFotoNota.setStyle(estiloAzulForte);
        }
    }

    private void configurarComboboxParaAbrirAoFocar(ComboBox<?> comboBox) {
        if (comboBox == null) return;
        comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                PauseTransition delay = new PauseTransition(Duration.millis(100));
                delay.setOnFinished(event -> {
                    try {
                        if (comboBox.isFocused() && !comboBox.isShowing() && comboBox.getItems() != null && !comboBox.getItems().isEmpty()) {
                            comboBox.show();
                        }
                    } catch (Exception e) { /* UI timing — seguro ignorar */ }
                });
                delay.play();
            }
        });
    }

    private void configurarAutoCompleteClienteGoogleStyle(ComboBox<String> cmb, ContextMenu menuContexto, ObservableList<String> listaFonte) {
        cmb.setOnShowing(e -> {}); 

        // Índice de navegação para setas - usa variável local por referência
        final int[] idxNavegacao = {-1};

        // Filtro no ComboBox (não no editor) para interceptar ANTES do skin nativo
        cmb.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();

            if (code == KeyCode.DOWN) {
                if (menuContexto.isShowing() && !menuContexto.getItems().isEmpty()) {
                    idxNavegacao[0] = Math.min(idxNavegacao[0] + 1, menuContexto.getItems().size() - 1);
                    destacarItemMenu(menuContexto, idxNavegacao[0]);
                } else if (!menuContexto.isShowing()) {
                    // Abre o menu com sugestões filtradas ou todas
                    String digitado = cmb.getEditor().getText();
                    if (digitado == null) digitado = "";
                    digitado = digitado.toUpperCase();
                    if (cmb.isShowing()) cmb.hide();
                    menuContexto.getItems().clear();
                    idxNavegacao[0] = -1;
                    final String filtroUp = digitado;
                    List<String> sugestoes = filtroUp.isEmpty()
                            ? new ArrayList<>(listaFonte)
                            : listaFonte.stream().filter(c -> c.toUpperCase().contains(filtroUp)).collect(Collectors.toList());
                    if (!sugestoes.isEmpty()) {
                        for (String cliente : sugestoes) {
                            CustomMenuItem mi = new CustomMenuItem();
                            Label lblN = new Label(cliente);
                            lblN.setStyle("-fx-text-fill: black; -fx-padding: 5 10 5 10;");
                            HBox ctn = new HBox(lblN);
                            ctn.setAlignment(Pos.CENTER_LEFT);
                            ctn.setPrefWidth(cmb.getWidth() > 0 ? cmb.getWidth() : 300);
                            mi.setContent(ctn);
                            mi.setOnAction(ev -> {
                                programmaticamenteAtualizando = true;
                                try {
                                    cmb.setValue(cliente);
                                    cmb.getEditor().positionCaret(cliente.length());
                                } finally {
                                    programmaticamenteAtualizando = false;
                                }
                                menuContexto.hide();
                                idxNavegacao[0] = -1;
                                if (cmb == cbRemetente && cbCliente != null) cbCliente.requestFocus();
                                if (cmb == cbCliente && cbRota != null) cbRota.requestFocus();
                            });
                            menuContexto.getItems().add(mi);
                        }
                        menuContexto.show(cmb, Side.BOTTOM, 0, 0);
                    }
                }
                event.consume();
                return;
            }
            if (code == KeyCode.UP) {
                if (menuContexto.isShowing() && !menuContexto.getItems().isEmpty()) {
                    idxNavegacao[0] = Math.max(idxNavegacao[0] - 1, 0);
                    destacarItemMenu(menuContexto, idxNavegacao[0]);
                }
                event.consume(); // Sempre consumir para evitar o skin nativo abrir o popup
                return;
            }
            if (code == KeyCode.ENTER) {
                if (menuContexto.isShowing() && idxNavegacao[0] >= 0 && idxNavegacao[0] < menuContexto.getItems().size()) {
                    menuContexto.getItems().get(idxNavegacao[0]).fire();
                    idxNavegacao[0] = -1;
                    event.consume();
                } else if (menuContexto.isShowing()) {
                    menuContexto.hide();
                    idxNavegacao[0] = -1;
                    event.consume();
                }
                // Se menu não está mostrando, deixa o ENTER ser processado normalmente (navegação para próximo campo)
                return;
            }
            if (code == KeyCode.ESCAPE) {
                menuContexto.hide();
                idxNavegacao[0] = -1;
                event.consume();
                return;
            }
        });

        cmb.getEditor().setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            // Ignorar teclas de controle — só processar digitação real
            if (code == KeyCode.ENTER || code == KeyCode.TAB || code == KeyCode.ESCAPE 
                || code == KeyCode.UP || code == KeyCode.DOWN
                || code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT) {
                return;
            }

            String digitado = cmb.getEditor().getText().toUpperCase();
            if (cmb.isShowing()) cmb.hide();
            menuContexto.getItems().clear(); 
            idxNavegacao[0] = -1;

            if (digitado.isEmpty()) {
                menuContexto.hide();
                return;
            }

            List<String> sugestoes = listaFonte.stream().filter(c -> c.toUpperCase().contains(digitado)).collect(Collectors.toList());

            if (!sugestoes.isEmpty()) {
                for (String cliente : sugestoes) {
                    CustomMenuItem item = new CustomMenuItem();
                    Label lblNome = new Label(cliente);
                    lblNome.setStyle("-fx-text-fill: black; -fx-padding: 5 10 5 10;");
                    HBox container = new HBox(lblNome);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setPrefWidth(cmb.getWidth() > 0 ? cmb.getWidth() : 300);
                    item.setContent(container);
                    item.setOnAction(e -> {
                        programmaticamenteAtualizando = true;
                        try {
                            cmb.setValue(cliente); 
                            cmb.getEditor().positionCaret(cliente.length()); 
                        } finally {
                            programmaticamenteAtualizando = false;
                        }
                        menuContexto.hide();
                        idxNavegacao[0] = -1;
                        if (cmb == cbRemetente && cbCliente != null) cbCliente.requestFocus();
                        if (cmb == cbCliente && cbRota != null) cbRota.requestFocus();
                    });
                    menuContexto.getItems().add(item);
                }
                if (!menuContexto.isShowing()) {
                    menuContexto.show(cmb, Side.BOTTOM, 0, 0);
                }
            } else {
                menuContexto.hide();
            }
        });
        
        cmb.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                menuContexto.hide();
                idxNavegacao[0] = -1;
            }
        });
    }

    /**
     * Destaca visualmente o item na posição idx do ContextMenu.
     */
    private void destacarItemMenu(ContextMenu menu, int idx) {
        for (int i = 0; i < menu.getItems().size(); i++) {
            MenuItem mi = menu.getItems().get(i);
            if (mi instanceof CustomMenuItem) {
                Node content = ((CustomMenuItem) mi).getContent();
                if (content instanceof HBox) {
                    if (i == idx) {
                        content.setStyle("-fx-background-color: #1565C0;");
                        // Muda cor do texto para branco
                        for (Node child : ((HBox) content).getChildren()) {
                            if (child instanceof Label) {
                                ((Label) child).setStyle("-fx-text-fill: white; -fx-padding: 5 10 5 10;");
                            }
                        }
                    } else {
                        content.setStyle("-fx-background-color: transparent;");
                        for (Node child : ((HBox) content).getChildren()) {
                            if (child instanceof Label) {
                                ((Label) child).setStyle("-fx-text-fill: black; -fx-padding: 5 10 5 10;");
                            }
                        }
                    }
                }
            }
        }
    }

    private void configurarValidacaoFocoClientesGoogle() {
        cbRemetente.getEditor().focusedProperty().addListener((obs, foiFocado, estaFocado) -> {
            if (!estaFocado) {
                menuSugestoesRemetente.hide();
                String texto = cbRemetente.getEditor().getText().trim().toUpperCase();
                if (!texto.isEmpty()) {
                    processarContatoDigitado(cbRemetente, texto, listaRemetentesOriginal, "Remetente");
                }
            }
        });

        cbCliente.getEditor().focusedProperty().addListener((obs, foiFocado, estaFocado) -> {
            if (!estaFocado) {
                menuSugestoesCliente.hide();
                String texto = cbCliente.getEditor().getText().trim().toUpperCase();
                if (!texto.isEmpty()) {
                    processarContatoDigitado(cbCliente, texto, listaClientesOriginal, "Cliente");
                }
            }
        });
    }

    private void configurarComboBoxItem() {
        if (cbitem == null) return;
        cbitem.setEditable(true);

        // Prevenir o popup nativo do ComboBox — usamos ContextMenu no lugar
        cbitem.setOnShowing(e -> {});

        cbitem.setConverter(new StringConverter<ItemFrete>() {
            @Override
            public String toString(ItemFrete item) {
                if (item == null) return null;
                return item.getNomeItem();
            }
            @Override
            public ItemFrete fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                return mapItensCadastrados.get(string.trim().toLowerCase());
            }
        });

        cbitem.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (programmaticamenteAtualizando) return;
            try {
                cbitem.getSelectionModel().clearSelection();
                cbitem.setValue(null);
            } catch (Exception e) { /* UI cleanup — seguro ignorar */ }
        });

        // =====================================================================
        // INTERCEPTOR DE TECLAS — DOWN/UP/ENTER/ESCAPE no ContextMenu
        // Filtro no ComboBox (não no editor) para interceptar ANTES do skin nativo
        // =====================================================================
        cbitem.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();

            if (code == KeyCode.DOWN) {
                if (menuSugestoesCbItem.isShowing() && !menuSugestoesCbItem.getItems().isEmpty()) {
                    indiceSugestaoCbItem = Math.min(indiceSugestaoCbItem + 1, menuSugestoesCbItem.getItems().size() - 1);
                    destacarItemMenu(menuSugestoesCbItem, indiceSugestaoCbItem);
                } else if (!menuSugestoesCbItem.isShowing()) {
                    mostrarSugestoesCbItem(cbitem.getEditor().getText());
                }
                event.consume();
                return;
            }
            if (code == KeyCode.UP) {
                if (menuSugestoesCbItem.isShowing() && !menuSugestoesCbItem.getItems().isEmpty()) {
                    indiceSugestaoCbItem = Math.max(indiceSugestaoCbItem - 1, 0);
                    destacarItemMenu(menuSugestoesCbItem, indiceSugestaoCbItem);
                }
                event.consume();
                return;
            }
            if (code == KeyCode.ENTER) {
                if (menuSugestoesCbItem.isShowing() && indiceSugestaoCbItem >= 0
                        && indiceSugestaoCbItem < menuSugestoesCbItem.getItems().size()) {
                    menuSugestoesCbItem.getItems().get(indiceSugestaoCbItem).fire();
                    indiceSugestaoCbItem = -1;
                } else {
                    menuSugestoesCbItem.hide();
                    indiceSugestaoCbItem = -1;
                    String texto = cbitem.getEditor().getText();
                    if (texto != null && !texto.trim().isEmpty()) {
                        processarItemDigitadoOuSelecionado(texto.trim());
                    } else if (txtpreco != null) {
                        txtpreco.requestFocus();
                    }
                }
                event.consume();
                return;
            }
            if (code == KeyCode.ESCAPE) {
                menuSugestoesCbItem.hide();
                indiceSugestaoCbItem = -1;
                event.consume();
                return;
            }
            if (code == KeyCode.TAB) {
                menuSugestoesCbItem.hide();
                indiceSugestaoCbItem = -1;
                // Não consome — deixa TAB navegar normalmente
            }
        });

        // =====================================================================
        // KEY RELEASED — filtra e mostra sugestões quando o usuário digita
        // =====================================================================
        cbitem.getEditor().setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.ENTER || code == KeyCode.TAB || code == KeyCode.ESCAPE
                    || code == KeyCode.UP || code == KeyCode.DOWN
                    || code == KeyCode.SHIFT || code == KeyCode.CONTROL || code == KeyCode.ALT) {
                return;
            }

            String texto = cbitem.getEditor().getText();
            if (texto == null || texto.isEmpty()) {
                menuSugestoesCbItem.hide();
                indiceSugestaoCbItem = -1;
                return;
            }
            mostrarSugestoesCbItem(texto);
        });

        // =====================================================================
        // FOCUS LISTENER — ao perder foco, processa o texto digitado
        // =====================================================================
        cbitem.getEditor().focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                menuSugestoesCbItem.hide();
                indiceSugestaoCbItem = -1;
                if (!programmaticamenteAtualizando) {
                    String text = cbitem.getEditor().getText();
                    if (text != null && !text.trim().isEmpty()) {
                        ItemFrete matchedItem = mapItensCadastrados.get(text.trim().toLowerCase());
                        if (matchedItem != null) {
                            programmaticamenteAtualizando = true;
                            try {
                                cbitem.setValue(matchedItem);
                            } finally {
                                programmaticamenteAtualizando = false;
                            }
                            processarItemDigitadoOuSelecionado(matchedItem.getNomeItem());
                        } else {
                            processarItemDigitadoOuSelecionado(text.trim());
                        }
                    }
                }
            }
        });
    }

    /**
     * Monta e exibe o ContextMenu de sugestões para o campo Item/Produto.
     */
    private void mostrarSugestoesCbItem(String texto) {
        menuSugestoesCbItem.getItems().clear();
        indiceSugestaoCbItem = -1;

        List<ItemFrete> sugestoes;
        if (texto == null || texto.isEmpty()) {
            sugestoes = new ArrayList<>(listaItensDisplayOriginal);
        } else {
            String filtro = texto.toLowerCase();
            sugestoes = listaItensDisplayOriginal.stream()
                    .filter(item -> item != null && item.getNomeItem().toLowerCase().contains(filtro))
                    .collect(Collectors.toList());
        }

        if (sugestoes.isEmpty()) {
            menuSugestoesCbItem.hide();
            return;
        }

        for (ItemFrete itemFrete : sugestoes) {
            CustomMenuItem menuItem = new CustomMenuItem();
            HBox root = new HBox();
            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(10);

            Label lblNome = new Label(itemFrete.getNomeItem());
            lblNome.setStyle("-fx-text-fill: black; -fx-padding: 5 10 5 10;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            BigDecimal precoExibir;
            String corTexto;
            String sufixo;
            if (rbComDesconto != null && rbComDesconto.isSelected()) {
                precoExibir = itemFrete.getPrecoUnitarioDesconto();
                corTexto = "#2e7d32";
                sufixo = " (Desc.)";
            } else {
                precoExibir = itemFrete.getPrecoUnitarioPadrao();
                corTexto = "#0d47a1";
                sufixo = " (Normal)";
            }

            Label lblPreco = new Label(df.format(precoExibir) + sufixo);
            lblPreco.setStyle("-fx-font-weight: bold; -fx-text-fill: " + corTexto + "; -fx-padding: 5 10 5 10;");

            root.getChildren().addAll(lblNome, spacer, lblPreco);
            root.setPrefWidth(cbitem.getWidth() > 0 ? cbitem.getWidth() : 300);
            menuItem.setContent(root);

            menuItem.setOnAction(ev -> {
                programmaticamenteAtualizando = true;
                try {
                    cbitem.getEditor().setText(itemFrete.getNomeItem());
                    cbitem.getEditor().positionCaret(itemFrete.getNomeItem().length());
                    cbitem.setValue(itemFrete);
                } finally {
                    programmaticamenteAtualizando = false;
                }
                menuSugestoesCbItem.hide();
                indiceSugestaoCbItem = -1;
                processarItemDigitadoOuSelecionado(itemFrete.getNomeItem());
            });

            menuSugestoesCbItem.getItems().add(menuItem);
        }

        if (!menuSugestoesCbItem.isShowing()) {
            Bounds bounds = cbitem.localToScreen(cbitem.getBoundsInLocal());
            if (bounds != null) {
                menuSugestoesCbItem.show(cbitem, bounds.getMinX(), bounds.getMaxY());
            } else {
                menuSugestoesCbItem.show(cbitem, Side.BOTTOM, 0, 0);
            }
        }
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
            showAlert(AlertType.INFORMATION, "Nenhuma Viagem Ativa",
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
    public void carregarFreteParaEdicao(String numFrete) {
        System.out.println("carregarFreteParaEdicao: carregando frete " + numFrete + " do banco...");
        long numeroFreteLong;
        try {
            numeroFreteLong = Long.parseLong(numFrete);
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Erro de Dados", "O nÃºmero do frete '" + numFrete + "' Ã© invÃ¡lido.");
            return;
        }

        try (Connection conn = ConexaoBD.getConnection()) {
            String sqlFrete = "SELECT * FROM fretes WHERE numero_frete = ?";
            try (PreparedStatement pst = conn.prepareStatement(sqlFrete)) {
                pst.setLong(1, numeroFreteLong);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    // DM015: preenchimento extraido em metodo auxiliar
                    preencherCamposDoFrete(rs, numFrete);
                } else {
                    showAlert(AlertType.WARNING, "Aviso", "Nenhum frete encontrado com nÃºmero: " + numFrete);
                    return;
                }
            }

            String sqlItens = "SELECT nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item FROM frete_itens WHERE id_frete = ?";
            try (PreparedStatement pst2 = conn.prepareStatement(sqlItens)) {
                pst2.setLong(1, freteAtualId);
                ResultSet rs2 = pst2.executeQuery();
                listaTabelaItensFrete.clear();
                while (rs2.next()) {
                    String descricaoItem = rs2.getString("nome_item_ou_id_produto");
                    int qtd = rs2.getInt("quantidade");
                    double precoUnit = rs2.getDouble("preco_unitario");
                    FreteItem item = new FreteItem(qtd, descricaoItem, precoUnit);
                    listaTabelaItensFrete.add(item);
                }
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
            if (BtnImprimirNota != null) BtnImprimirNota.setDisable(!itensPresentes);
            if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!itensPresentes);
            if (btnImprimirRecibo != null) btnImprimirRecibo.setDisable(!itensPresentes);
            // --------------------------------------------------

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Carregar Frete", "NÃ£o foi possÃ­vel carregar o frete:\n" + e.getMessage());
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

            String numNotaFiscalTemp = rs.getString("num_notafiscal");
            BigDecimal valorNotaDb = rs.getBigDecimal("valor_notafiscal");
            BigDecimal pesoNotaDb = rs.getBigDecimal("peso_notafiscal");
            BigDecimal valorFreteCalculado = rs.getBigDecimal("valor_frete_calculado");

            if (numNotaFiscalTemp != null && !numNotaFiscalTemp.isEmpty()) {
                if (rbSim != null) rbSim.setSelected(true);
                if (txtNumNota != null) txtNumNota.setText(numNotaFiscalTemp);
                if (txtValorNota != null) txtValorNota.setText(valorNotaDb != null ? df.format(valorNotaDb.doubleValue()) : "");
                if (txtPesoNota != null) txtPesoNota.setText(pesoNotaDb != null ? pesoNotaDb.toString() : "");
            } else {
                if (Rbnao != null) Rbnao.setSelected(true);
            }
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
        carregarContatosParaComboBoxes("Remetente", listaRemetentesOriginal);
        carregarContatosParaComboBoxes("Cliente", listaClientesOriginal);
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

    public static class FreteItem {
        private final SimpleIntegerProperty quantidade;
        private final SimpleStringProperty item;
        private final SimpleDoubleProperty preco;

        public FreteItem(int qtd, String it, double pr) {
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

        public double getTotal() { return getQuantidade() * getPreco(); }
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
                FreteItem it = e.getRowValue();
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
                FreteItem it = e.getRowValue();
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
                        return parseValorMonetario(string);
                    } catch (ParseException parseException) {
                        return null;
                    }
                }
            }));
            colPreco.setOnEditCommit(event -> {
                FreteItem itemEditado = event.getRowValue();
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
                        double v = parseValorMonetario(txtpreco.getText());
                        txtpreco.setText(df.format(v));
                    } catch (ParseException e) {
                    }
                }
            });
        }

        if (notaFiscalToggleGroup != null) {
            notaFiscalToggleGroup.selectedToggleProperty().addListener((o, old, n) -> {
                if (programmaticamenteAtualizando) return;
                boolean isFormEditable = (btnSalvar != null && !btnSalvar.isDisabled()) || (btnAlterar != null && !btnAlterar.isDisabled());
                habilitarCamposDaNotaFiscal(isFormEditable);
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
                if (cbitem != null) {
                    List<ItemFrete> items = new ArrayList<>(cbitem.getItems());
                    cbitem.setItems(FXCollections.observableArrayList(items));
                }
            });
        }
        
        if (cbRemetente != null && cbCliente != null) {
            cbRemetente.getEditor().setOnKeyPressed(e -> {
                 if(e.getCode() == KeyCode.ENTER) { menuSugestoesRemetente.hide(); cbCliente.requestFocus(); }
            });
        }
        if (cbCliente != null && cbRota != null) {
            cbCliente.getEditor().setOnKeyPressed(e -> {
                 if(e.getCode() == KeyCode.ENTER) { menuSugestoesCliente.hide(); cbRota.requestFocus(); }
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
        
        if (txtCidadeCobranca != null && rbSim != null) setEnterNavigation(txtCidadeCobranca, rbSim);

        if (rbSim != null && txtNumNota != null && txtObs != null) {
            rbSim.setOnKeyPressed(createEnterKeyHandlerForRadioButton(txtNumNota, txtObs, true));
        }
        if (Rbnao != null && txtNumNota != null && txtObs != null) {
            Rbnao.setOnKeyPressed(createEnterKeyHandlerForRadioButton(txtNumNota, txtObs, false));
        }

        if (txtNumNota != null && txtValorNota != null) setEnterNavigation(txtNumNota, txtValorNota);
        if (txtValorNota != null && txtPesoNota != null) setEnterNavigation(txtValorNota, txtPesoNota);
        if (txtPesoNota != null && txtObs != null) setEnterNavigation(txtPesoNota, txtObs);
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
            listaTabelaItensFrete.addListener((ListChangeListener<FreteItem>) c -> {
                if (programmaticamenteAtualizando) return;
                atualizarTotaisAgregados();
                boolean itensPresentes = !listaTabelaItensFrete.isEmpty();
                boolean podeImprimir = freteAtualId != -1 || (btnSalvar != null && !btnSalvar.isDisabled());

                if (BtnImprimirNota != null) BtnImprimirNota.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirRecibo != null) btnImprimirRecibo.setDisable(!(podeImprimir && itensPresentes));
            });
        }
    }

    private javafx.event.EventHandler<KeyEvent> createEnterKeyHandlerForRadioButton(Node nextSim, Node nextNao, boolean isSim) {
        return e -> {
            if (e.getCode() == KeyCode.ENTER) {
                Toggle selectedToggle = notaFiscalToggleGroup.getSelectedToggle();
                if (selectedToggle == rbSim && nextSim != null && nextSim.isFocusTraversable()) {
                    nextSim.requestFocus();
                } else if (selectedToggle == Rbnao && nextNao != null && nextNao.isFocusTraversable()) {
                    nextNao.requestFocus();
                }
                e.consume();
            }
        };
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
                    String novoNomeContato = nomeContato.toUpperCase();
                    String sqlInsert = "INSERT INTO contatos (nome_razao_social) VALUES (?)";
                    try (Connection conn = ConexaoBD.getConnection();
                         PreparedStatement pst = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        pst.setString(1, novoNomeContato);
                        int affectedRows = pst.executeUpdate();
                        if (affectedRows > 0) {
                            if (!listaContatos.contains(novoNomeContato)) {
                                listaContatos.add(novoNomeContato);
                                FXCollections.sort(listaContatos);
                            }
                            final String finalNome = novoNomeContato;
                            javafx.application.Platform.runLater(() -> {
                                // Verificar se o editor ainda tem texto (pode ter sido limpo por limparCamposFrete)
                                String editorText = comboBox.getEditor() != null ? comboBox.getEditor().getText() : null;
                                if (editorText == null || editorText.trim().isEmpty()) return;
                                programmaticamenteAtualizando = true;
                                try {
                                    comboBox.setValue(finalNome);
                                } finally {
                                    programmaticamenteAtualizando = false;
                                }
                            });
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
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

    private void carregarContatosParaComboBoxes(String tipo, ObservableList<String> lista) {
        if (lista == null) return;
        lista.clear();
        String sql = "SELECT nome_razao_social FROM contatos ORDER BY nome_razao_social";
        try (Connection c = ConexaoBD.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                String nome = r.getString(1);
                if (nome != null) lista.add(nome);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarRotas() {
        if (listaRotasOriginal == null) listaRotasOriginal = FXCollections.observableArrayList();
        listaRotasOriginal.clear();
        String sql = "SELECT origem, destino FROM rotas ORDER BY origem, destino";
        try (Connection c = ConexaoBD.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                String o = r.getString("origem");
                String d = r.getString("destino");
                String rd = "";
                if (o != null && !o.trim().isEmpty()) rd += o.trim();
                if (d != null && !d.trim().isEmpty()) {
                    if (!rd.isEmpty()) rd += " - ";
                    rd += d.trim();
                }
                if (!rd.isEmpty()) listaRotasOriginal.add(rd);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarConferentesDoBanco() {
        if (listaConferentesOriginal == null) listaConferentesOriginal = FXCollections.observableArrayList();
        listaConferentesOriginal.clear();
        String sql = "SELECT nome_conferente FROM conferentes ORDER BY nome_conferente";
        try (Connection c = ConexaoBD.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                String nome = r.getString(1);
                if (nome != null) listaConferentesOriginal.add(nome);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void carregarItensCadastradosParaComboBox() {
        if (listaItensDisplayOriginal == null) listaItensDisplayOriginal = FXCollections.observableArrayList();
        if (mapItensCadastrados == null) mapItensCadastrados = new HashMap<>();
        listaItensDisplayOriginal.clear();
        mapItensCadastrados.clear();

        try {
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
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private long gerarNumeroFreteNoBanco() throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero_frete), 0) + 1 FROM fretes";
        long proximoNumero = 1;
        try (Connection conn = ConexaoBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                proximoNumero = rs.getLong(1);
            }
        }
        return proximoNumero;
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
            showAlert(AlertType.WARNING, "Aviso", "NÃ£o hÃ¡ frete selecionado para alteraÃ§Ã£o.");
            return;
        }
        habilitarCamposParaEdicao(true);
        if (btnNovo != null) btnNovo.setDisable(true);
        if (btnSalvar != null) btnSalvar.setDisable(false);
        if (btnAlterar != null) btnAlterar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }

    private void salvarOuAlterarFrete() {
        if (cbRemetente == null || cbRemetente.getValue() == null || cbRemetente.getValue().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo ObrigatÃ³rio", "Remetente deve ser informado.");
            if (cbRemetente != null) cbRemetente.requestFocus();
            return;
        }
        if (cbCliente == null || cbCliente.getValue() == null || cbCliente.getValue().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo ObrigatÃ³rio", "Cliente (DestinatÃ¡rio) deve ser informado.");
            if (cbCliente != null) cbCliente.requestFocus();
            return;
        }
        if (cbRota == null || cbRota.getValue() == null || cbRota.getValue().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo ObrigatÃ³rio", "Rota deve ser informada.");
            if (cbRota != null) cbRota.requestFocus();
            return;
        }
        if (listaTabelaItensFrete == null || listaTabelaItensFrete.isEmpty()) {
            showAlert(AlertType.WARNING, "Nenhum Item na Nota", "Ã‰ necessÃ¡rio adicionar pelo menos um item ao frete.");
            if (txtquantidade != null) txtquantidade.requestFocus();
            return;
        }

        boolean isNewFrete = (freteAtualId == -1);
        if (isNewFrete && this.viagemAtiva == null) {
            showAlert(AlertType.ERROR, "Erro CrÃ­tico", "NÃ£o Ã© possÃ­vel salvar um novo frete sem uma Viagem Ativa definida no sistema. Por favor, ative uma viagem na tela principal.");
            return;
        }

        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            long numeroFreteParaOperacao = freteAtualId;

            if (isNewFrete) {
                numeroFreteParaOperacao = gerarNumeroFreteNoBanco();
            }

            String sqlFrete;
            if (isNewFrete) {
                sqlFrete = "INSERT INTO fretes (id_frete, numero_frete, data_emissao, data_saida_viagem, local_transporte, remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp, cidade_cobranca, observacoes, num_notafiscal, valor_notafiscal, peso_notafiscal, valor_total_itens, desconto, valor_frete_calculado, valor_pago, troco, valor_devedor, tipo_pagamento, nome_caixa, status_frete, id_viagem) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            } else {
                sqlFrete = "UPDATE fretes SET data_emissao = ?, data_saida_viagem = ?, local_transporte = ?, remetente_nome_temp = ?, destinatario_nome_temp = ?, rota_temp = ?, conferente_temp = ?, cidade_cobranca = ?, observacoes = ?, num_notafiscal = ?, valor_notafiscal = ?, peso_notafiscal = ?, valor_total_itens = ?, desconto = ?, valor_frete_calculado = ?, valor_pago = ?, troco = ?, valor_devedor = ?, tipo_pagamento = ?, nome_caixa = ?, status_frete = ?, id_viagem = ? WHERE id_frete = ?";
            }

            try (PreparedStatement pstFrete = conn.prepareStatement(sqlFrete)) {
                int paramIdx = 1;

                if (isNewFrete) {
                    pstFrete.setLong(paramIdx++, numeroFreteParaOperacao);
                    pstFrete.setLong(paramIdx++, numeroFreteParaOperacao);
                }

                pstFrete.setDate(paramIdx++, java.sql.Date.valueOf(LocalDate.now()));
                if (txtSaida != null && !txtSaida.getText().trim().isEmpty()) {
                    LocalDate d = LocalDate.parse(txtSaida.getText().trim(), dateFormatter);
                    pstFrete.setDate(paramIdx++, java.sql.Date.valueOf(d));
                } else {
                    pstFrete.setNull(paramIdx++, Types.DATE);
                }
                pstFrete.setString(paramIdx++, txtLocalTransporte != null ? txtLocalTransporte.getText() : null);
                pstFrete.setString(paramIdx++, cbRemetente.getValue());
                pstFrete.setString(paramIdx++, cbCliente.getValue());
                pstFrete.setString(paramIdx++, cbRota.getValue());
                pstFrete.setString(paramIdx++, cbConferente.getValue());
                pstFrete.setString(paramIdx++, txtCidadeCobranca.getText()); 
                pstFrete.setString(paramIdx++, txtObs != null ? txtObs.getText() : null);

                boolean temNF = rbSim != null && rbSim.isSelected();
                pstFrete.setString(paramIdx++, temNF && txtNumNota != null ? txtNumNota.getText() : null);
                pstFrete.setBigDecimal(paramIdx++, temNF && txtValorNota != null ? parseToBigDecimal(txtValorNota.getText()) : BigDecimal.ZERO);
                pstFrete.setBigDecimal(paramIdx++, temNF && txtPesoNota != null ? parseToBigDecimal(txtPesoNota.getText()) : BigDecimal.ZERO);

                BigDecimal totalItens = listaTabelaItensFrete.stream()
                        .map(i -> BigDecimal.valueOf(i.getTotal()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                pstFrete.setBigDecimal(paramIdx++, totalItens);
                pstFrete.setBigDecimal(paramIdx++, BigDecimal.ZERO);

                BigDecimal valorFreteCalculado = (txtValorTotalNota != null && !txtValorTotalNota.getText().isEmpty())
                        ? parseToBigDecimal(txtValorTotalNota.getText())
                        : totalItens;
                pstFrete.setBigDecimal(paramIdx++, valorFreteCalculado);
                pstFrete.setBigDecimal(paramIdx++, BigDecimal.ZERO);
                pstFrete.setBigDecimal(paramIdx++, BigDecimal.ZERO);
                pstFrete.setBigDecimal(paramIdx++, valorFreteCalculado);
                pstFrete.setString(paramIdx++, null);
                pstFrete.setString(paramIdx++, null);
                pstFrete.setString(paramIdx++, "PENDENTE");

                if (this.viagemAtiva != null) {
                    pstFrete.setLong(paramIdx++, this.viagemAtiva.getId());
                } else {
                    pstFrete.setNull(paramIdx++, Types.BIGINT);
                }

                if (!isNewFrete) {
                    pstFrete.setLong(paramIdx++, numeroFreteParaOperacao);
                }

                pstFrete.executeUpdate();
            }

            if (!isNewFrete) {
                String sqlDeleteItems = "DELETE FROM frete_itens WHERE id_frete = ?";
                try (PreparedStatement pstDelete = conn.prepareStatement(sqlDeleteItems)) {
                    pstDelete.setLong(1, freteAtualId);
                    pstDelete.executeUpdate();
                }
            }

            String sqlItem = "INSERT INTO frete_itens (id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item) VALUES (?,?,?,?,?)";
            try (PreparedStatement pstItem = conn.prepareStatement(sqlItem)) {
                for (FreteItem it : listaTabelaItensFrete) {
                    pstItem.setLong(1, numeroFreteParaOperacao);
                    pstItem.setString(2, it.getItem());
                    pstItem.setInt(3, it.getQuantidade());
                    pstItem.setBigDecimal(4, BigDecimal.valueOf(it.getPreco()));
                    pstItem.setBigDecimal(5, BigDecimal.valueOf(it.getTotal()));
                    pstItem.addBatch();
                }
                pstItem.executeBatch();
            }

            conn.commit();

            String mensagemSucesso;
            if (isNewFrete) {
                mensagemSucesso = "Frete numero " + numeroFreteParaOperacao + " salvo com sucesso!";
                freteAtualId = numeroFreteParaOperacao;
                if(txtNumFrete != null) {
                    programmaticamenteAtualizando = true;
                    try {
                        txtNumFrete.setText(String.valueOf(numeroFreteParaOperacao));
                    } finally {
                        programmaticamenteAtualizando = false;
                    }
                }
            } else {
                mensagemSucesso = "Frete numero " + numeroFreteParaOperacao + " alterado com sucesso!";
            }
            showAlert(AlertType.INFORMATION, "Sucesso", mensagemSucesso);

            if (isNewFrete) {
                configurarParaNovoFrete();
                habilitarCamposParaEdicao(true);
                freteAtualId = -1;
                Platform.runLater(() -> {
                    if (cbRemetente != null) cbRemetente.requestFocus();
                });
            } else {
                habilitarCamposParaVisualizacao(true);
                if (btnNovo != null) btnNovo.setDisable(false);
                if (btnSalvar != null) btnSalvar.setDisable(true);
                if (btnAlterar != null) btnAlterar.setDisable(false);
                if (btnExcluir != null) btnExcluir.setDisable(false);
            }

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro na OperaÃ§Ã£o do Frete", "Ocorreu um erro no banco de dados:\n" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Inesperado", "Ocorreu um erro geral na operaÃ§Ã£o:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
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

        if (qtdStr.isEmpty()) {
            txtquantidade.requestFocus();
            return;
        }
        if (itemNomeOuDescricao == null || itemNomeOuDescricao.trim().isEmpty()) {
            cbitem.requestFocus();
            return;
        }
        if (precoStr.isEmpty()) {
            txtpreco.requestFocus();
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(qtdStr);
            if (quantidade <= 0) throw new NumberFormatException("Qtd > 0");
        } catch (NumberFormatException e) {
            txtquantidade.requestFocus();
            return;
        }
        double precoUnitario;
        try {
            precoUnitario = parseValorMonetario(precoStr);
            if (precoUnitario < 0) throw new ParseException("PreÃ§o nÃ£o negativo", 0);
        } catch (ParseException e) {
            txtpreco.requestFocus();
            return;
        }

        String nomeItemFinal = itemNomeOuDescricao.trim().toLowerCase();
        if (listaTabelaItensFrete != null) {
            listaTabelaItensFrete.add(new FreteItem(quantidade, nomeItemFinal, precoUnitario));
        }
        limparCamposItem();
        if (txtquantidade != null && txtquantidade.isFocusTraversable()) {
            txtquantidade.requestFocus();
        }
    }

    @FXML private void handleExcluirFrete(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "NÃ£o hÃ¡ frete selecionado para exclusÃ£o.");
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

                String sqlDeleteFrete = "DELETE FROM fretes WHERE id_frete = ?";
                try (PreparedStatement pstFrete = conn.prepareStatement(sqlDeleteFrete)) {
                    pstFrete.setLong(1, freteAtualId);
                    pstFrete.executeUpdate();
                }

                conn.commit();
                showAlert(AlertType.INFORMATION, "Sucesso", "Frete " + txtNumFrete.getText() + " excluÃ­do com sucesso!");
                limparCamposFrete();
                habilitarCamposParaVisualizacao(false);
                if (btnNovo != null) btnNovo.setDisable(false);
                if (btnSalvar != null) btnSalvar.setDisable(true);
                if (btnAlterar != null) btnAlterar.setDisable(true);
                if (btnExcluir != null) btnExcluir.setDisable(true);
                freteAtualId = -1;
            } catch (SQLException e) {
                try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                showAlert(AlertType.ERROR, "Erro ao Excluir Frete", "NÃ£o foi possÃ­vel excluir o frete:\n" + e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    @FXML
    public void handleFotoNota(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecione a Foto da Nota Fiscal/Pedido");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
            File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

            if (file != null) {
                if (freteAtualId == -1 || btnSalvar.isDisable()) {
                     handleNovoFrete(null);
                }
                if (rbSim != null) rbSim.setSelected(true);

                gui.util.OcrAudioService.executarOCRAsync(file,
                    resultado -> interpretarTextoFreteEPreencher(resultado),
                    e -> Platform.runLater(() -> showAlert(AlertType.ERROR, "Erro OCR", "Erro ao ler imagem: " + e.getMessage()))
                );
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro ao abrir seletor de foto:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAudio(ActionEvent event) {
        if(btnAudio.getText().contains("Ouvindo")) return;
        if (freteAtualId == -1 || btnSalvar.isDisable()) {
             handleNovoFrete(null);
        }

        btnAudio.setText("Ouvindo... (Fale)");
        btnAudio.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");

        gui.util.OcrAudioService.executarVozAsync(
            texto -> {
                interpretarTextoFreteEPreencher(texto);
                Platform.runLater(() -> {
                    btnAudio.setText("Voz");
                    btnAudio.setStyle("-fx-background-color: #0d47a1; -fx-text-fill: white;");
                });
            },
            e -> Platform.runLater(() -> {
                showAlert(AlertType.ERROR, "Erro Audio", "Erro no microfone ou modelo: " + e.getMessage());
                btnAudio.setText("Voz");
                btnAudio.setStyle("-fx-background-color: #0d47a1; -fx-text-fill: white;");
            })
        );
    }

    private void interpretarTextoFreteEPreencher(String texto) {
        if (texto == null || texto.isEmpty()) {
            Platform.runLater(() -> showAlert(AlertType.WARNING, "IA", "Nenhum texto identificado."));
            return;
        }

        String[] linhas = texto.split("\n");

        Platform.runLater(() -> {
            for (String linha : linhas) {
                linha = linha.trim().toUpperCase();
                if (linha.isEmpty()) continue;

                if (linha.contains("DANFE") || linha.contains("NF-E") || linha.contains("NOTA FISCAL") || linha.contains("NÃšMERO")) {
                     String numeros = linha.replaceAll("[^0-9]", "");
                     if(numeros.length() > 3 && numeros.length() < 10) {
                         if(txtNumNota != null) txtNumNota.setText(numeros);
                     }
                }

                if (linha.contains("VALOR") || linha.contains("TOTAL NOTA") || linha.contains("V. TOTAL")) {
                     if (txtValorNota != null && txtValorNota.getText().isEmpty()) {
                         String valor = extrairValorMonetarioStr(linha);
                         if(!valor.isEmpty()) txtValorNota.setText(valor);
                     }
                }

                if (linha.contains("PESO") || linha.contains("KG") || linha.contains("P.LIQ")) {
                    String peso = extrairApenasNumerosVirgula(linha);
                    if (!peso.isEmpty() && txtPesoNota != null) txtPesoNota.setText(peso);
                }

                if (linha.startsWith("REM:") || linha.startsWith("EMITENTE") || linha.startsWith("DE:")) {
                    String valor = extrairTextoAposDoisPontos(linha);
                    if (!valor.isEmpty() && cbRemetente != null) cbRemetente.setValue(valor);
                }

                else if (linha.startsWith("DEST:") || linha.startsWith("DESTINATARIO") || linha.startsWith("PARA:")) {
                    String valor = extrairTextoAposDoisPontos(linha);
                    if (!valor.isEmpty() && cbCliente != null) cbCliente.setValue(valor);
                }
                
                else if (linha.startsWith("OBS:") || linha.startsWith("OBSERVACOES")) {
                    String valor = extrairTextoAposDoisPontos(linha);
                    if (!valor.isEmpty() && txtObs != null) txtObs.setText(valor);
                }

                else if (Character.isDigit(linha.charAt(0)) && linha.contains(" ")) {
                    try {
                        String[] partes = linha.split(" ", 2);
                        if (partes.length >= 2) {
                            String qtdStr = partes[0].replaceAll("[^0-9]", "");
                            String descLida = partes[1].trim();

                            if (!qtdStr.isEmpty() && descLida.length() > 2) {
                                int qtd = Integer.parseInt(qtdStr);
                                
                                double precoEncontrado = 0.0;
                                for (Map.Entry<String, ItemFrete> entry : mapItensCadastrados.entrySet()) {
                                    if (descLida.contains(entry.getKey().toUpperCase()) || entry.getKey().toUpperCase().contains(descLida)) {
                                        ItemFrete itemBanco = entry.getValue();
                                        precoEncontrado = itemBanco.getPrecoUnitarioPadrao().doubleValue();
                                        descLida = itemBanco.getNomeItem(); 
                                        break;
                                    }
                                }

                                if(listaTabelaItensFrete != null) {
                                    listaTabelaItensFrete.add(new FreteItem(qtd, descLida, precoEncontrado));
                                }
                            }
                        }
                    } catch (Exception e) { System.err.println("Erro em CadastroFreteController.processarIA (item): " + e.getMessage()); }
                }
            }

            atualizarTotaisAgregados();
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("IA Processada");
            alert.setHeaderText("Dados extraÃ­dos com sucesso");
            alert.setContentText("Verifique os campos preenchidos e os itens na tabela.");
            alert.show();
        });
    }

    private String extrairTextoAposDoisPontos(String linha) {
        if (linha.contains(":")) {
            return linha.substring(linha.indexOf(":") + 1).trim();
        }
        String[] keywords = {"REM", "DEST", "PARA", "DE", "EMITENTE", "DESTINATARIO"};
        for(String k : keywords) {
             if(linha.startsWith(k)) return linha.replace(k, "").trim();
        }
        return linha;
    }

    private String extrairApenasNumerosVirgula(String texto) {
        return texto.replaceAll("[^0-9,.]", "");
    }
    
    private String extrairValorMonetarioStr(String linha) {
         String[] tokens = linha.split(" ");
         for(String t : tokens) {
             if(t.contains(",") || t.contains(".")) {
                 if(t.matches(".*\\d.*")) return t.replace("R$", "").trim();
             }
         }
         return "";
    }

    @FXML private void imprimirNotaFretePersonalizada(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir a nota.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Nota Fiscal do Frete - NÃ£o implementado.");
    }
    @FXML private void imprimirReciboPersonalizado(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir o recibo.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Recibo do Frete - NÃ£o implementado.");
    }

    @FXML private void abrirListaFretes(ActionEvent event) {
        abrirJanelaGenerica("ListaFretes.fxml", "Lista de Fretes", true, Modality.APPLICATION_MODAL);
    }
    @FXML private void handleSair(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }

    @FXML private void handleCodXml(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecione o Arquivo XML da Nota Fiscal");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos XML", "*.xml"));
            File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

            if (file != null) {
                if (freteAtualId == -1 || btnSalvar.isDisable()) {
                    handleNovoFrete(null);
                }
                if (rbSim != null) rbSim.setSelected(true);

                String conteudoXml = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                
                // Tenta extrair dados do XML da NF-e
                String numNota = extrairTagXml(conteudoXml, "nNF");
                String valorNota = extrairTagXml(conteudoXml, "vNF");
                String pesoNota = extrairTagXml(conteudoXml, "pesoB");
                if (pesoNota.isEmpty()) pesoNota = extrairTagXml(conteudoXml, "pesoL");

                if (!numNota.isEmpty() && txtNumNota != null) txtNumNota.setText(numNota);
                if (!valorNota.isEmpty() && txtValorNota != null) txtValorNota.setText(valorNota);
                if (!pesoNota.isEmpty() && txtPesoNota != null) txtPesoNota.setText(pesoNota);

                if (numNota.isEmpty() && valorNota.isEmpty()) {
                    showAlert(AlertType.WARNING, "Aviso", "Não foi possível extrair dados da nota fiscal do XML.\nVerifique se o arquivo é uma NF-e válida.");
                } else {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Dados da NF-e importados do XML com sucesso!");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro XML", "Erro ao processar o arquivo XML:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extrairTagXml(String xml, String tag) {
        try {
            String abertura = "<" + tag + ">";
            String fechamento = "</" + tag + ">";
            int inicio = xml.indexOf(abertura);
            if (inicio == -1) return "";
            inicio += abertura.length();
            int fim = xml.indexOf(fechamento, inicio);
            if (fim == -1) return "";
            return xml.substring(inicio, fim).trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    @FXML private void handleImprimirEtiqueta(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "Ã‰ necessÃ¡rio ter um frete salvo ou carregado para imprimir etiquetas.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Etiqueta(s) - NÃ£o implementado.");
    }

    private void habilitarCamposDaNotaFiscal(boolean formularioHabilitado) {
        boolean notaFiscalSelecionada = rbSim != null && rbSim.isSelected();
        boolean habilitarCamposNF = formularioHabilitado && notaFiscalSelecionada;

        if (btnFotoNota != null) btnFotoNota.setDisable(!formularioHabilitado);
        if (btnCodXml != null) btnCodXml.setDisable(!formularioHabilitado);
        if (btnAudio != null) btnAudio.setDisable(!formularioHabilitado);

        if (txtNumNota != null) txtNumNota.setDisable(!habilitarCamposNF);
        if (txtValorNota != null) txtValorNota.setDisable(!habilitarCamposNF);
        if (txtPesoNota != null) txtPesoNota.setDisable(!habilitarCamposNF);

        if (!habilitarCamposNF) {
            if (txtNumNota != null) txtNumNota.clear();
            if (txtValorNota != null) txtValorNota.clear();
            if (txtPesoNota != null) txtPesoNota.clear();
        }
    }

    private void habilitarCamposParaEdicao(boolean habilitar) {
        if (cbRemetente != null) cbRemetente.setDisable(!habilitar);
        if (cbCliente != null) cbCliente.setDisable(!habilitar);
        if (cbRota != null) cbRota.setDisable(!habilitar);
        if (txtSaida != null) txtSaida.setDisable(!habilitar);
        if (txtLocalTransporte != null) txtLocalTransporte.setDisable(!habilitar);
        if (cbConferente != null) cbConferente.setDisable(!habilitar);
        if (txtCidadeCobranca != null) txtCidadeCobranca.setDisable(!habilitar); 
        if (rbSim != null) rbSim.setDisable(!habilitar);
        if (Rbnao != null) Rbnao.setDisable(!habilitar);
        habilitarCamposDaNotaFiscal(habilitar);

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

        if (btnFotoNota != null) btnFotoNota.setDisable(true);
        if (btnCodXml != null) btnCodXml.setDisable(true);
        if (btnAudio != null) btnAudio.setDisable(true);

        boolean itensPresentes = listaTabelaItensFrete != null && !listaTabelaItensFrete.isEmpty();
        boolean podeImprimir = habilitar && itensPresentes && freteAtualId != -1;

        if (BtnImprimirNota != null) BtnImprimirNota.setDisable(!podeImprimir);
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
            if (Rbnao != null && notaFiscalToggleGroup != null) Rbnao.setSelected(true);
            if (txtNumNota != null) txtNumNota.clear();
            if (txtValorNota != null) txtValorNota.clear();
            if (txtPesoNota != null) txtPesoNota.clear();
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
            menuSugestoesCbItem.hide();
            indiceSugestaoCbItem = -1;
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
        double valorTotalAgregado = 0;
        if (listaTabelaItensFrete != null) {
            for (FreteItem item : listaTabelaItensFrete) {
                totalDeVolumes += item.getQuantidade();
                valorTotalAgregado += item.getTotal();
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
            double p = parseValorMonetario(sP.trim());
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
        } catch (NumberFormatException | ParseException e) {
            programmaticamenteAtualizando = true;
            try {
                txttotal.clear();
            } finally {
                programmaticamenteAtualizando = false;
            }
        }
    }

    private double parseValorMonetario(String valorStr) throws ParseException {
        if (valorStr == null || valorStr.trim().isEmpty()) {
            return 0.0;
        }
        String valorLimpo = valorStr.replace("R$", "").trim().replace(".", "").replace(",", ".");
        try {
            return Double.parseDouble(valorLimpo);
        } catch (NumberFormatException e) {
            throw new ParseException("Valor monetÃ¡rio '" + valorStr + "' invÃ¡lido.", 0);
        }
    }

    private BigDecimal parseToBigDecimal(String vS) throws IllegalArgumentException {
        if (vS == null || vS.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            double vD = parseValorMonetario(vS);
            return BigDecimal.valueOf(vD).setScale(2, RoundingMode.HALF_UP);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Valor monetÃ¡rio invÃ¡lido: '" + vS + "'.", e);
        }
    }

    private void showAlert(AlertType aT, String t, String m) {
        Alert a = new Alert(aT);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }

    private void abrirJanelaGenerica(String fxmlFileRelative, String title, boolean resizable, Modality modality) {
        try {
            String fxmlPath = fxmlFileRelative.startsWith("/") ? fxmlFileRelative : "/gui/" + fxmlFileRelative;
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                showAlert(AlertType.ERROR, "Erro ao Abrir Tela", "Arquivo FXML nÃ£o pÃ´de ser localizado: " + fxmlPath);
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
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Abrir Tela", "NÃ£o foi possÃ­vel carregar a tela: " + fxmlFileRelative + "\nDetalhes: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro CrÃ­tico", "Ocorreu um erro inesperado ao tentar abrir a tela '" + title + "'.");
        }
    }

    public void atualizarListaItensDoComboBox() {
        carregarItensCadastradosParaComboBox();
        setComboBoxItems();
    }
}