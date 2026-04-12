package gui;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.vosk.Model;
import org.vosk.Recognizer;

import dao.ClienteEncomendaDAO;
import dao.EmpresaDAO;
import dao.EncomendaDAO;
import dao.EncomendaItemDAO;
import dao.ItemEncomendaPadraoDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
import model.ClienteEncomenda;
import model.Empresa;
import model.Encomenda;
import model.EncomendaItem;
import model.ItemEncomendaPadrao;
import model.Rota;
import model.Viagem;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog; 
import javafx.scene.control.Tooltip; 
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;
import javafx.util.StringConverter;
import gui.util.AlertHelper;
import gui.util.AppLogger;
import gui.util.ValidationHelper;

public class InserirEncomendaController implements Initializable {

    @FXML private BorderPane rootPane;

    @FXML private Button btnAdicionarItem;
    @FXML private Button btnEntregar;
    @FXML private Button btnExcluir;
    @FXML private Button btnImprimir;
    @FXML private Button btnIniciar;
    @FXML private Button btnSalvar;
    @FXML private Button btnSair;
    @FXML private Button btnEditar;
    @FXML private Button btnAbrirLista;
    
    @FXML private Button btnAudioInput;
    @FXML private Button btnImageInput;

    @FXML private ComboBox<String> cmbDestinatario;
    @FXML private ComboBox<String> cmbRemetente;
    @FXML private ComboBox<Rota> cmbRota;
    @FXML private ComboBox<String> cmbDescricao;
    
    @FXML private TextField txtViagemAtual;
    
    @FXML private TableColumn<EncomendaItem, String> colDesc;
    @FXML private TableColumn<EncomendaItem, Integer> colQuant;
    @FXML private TableColumn<EncomendaItem, BigDecimal> colValTotal;
    @FXML private TableColumn<EncomendaItem, BigDecimal> colValUnit;
    @FXML private TableColumn<EncomendaItem, String> colLocalArmazenamento;
    @FXML private TableView<EncomendaItem> tableItens;
    
    // CAMPOS DO RECEBEDOR
    @FXML private TextField txtNomeRecebedor;      
    @FXML private TextField txtNDocumentoRecebedor; 
    
    @FXML private TextField txtNumeroEncomenda;
    @FXML private TextField txtObs;
    
    @FXML private TextField txtQuantidade;
    @FXML private TextField txtTotalAPagar;
    @FXML private TextField txtTotalVol;
    @FXML private TextField txtValorUnit;
    @FXML private TextField txtValorTotal;
    
    @FXML private TextField txtStatusEntrega;

    private ViagemDAO viagemDAO;
    private RotaDAO rotaDAO;
    private ClienteEncomendaDAO clienteEncomendaDAO;
    private EncomendaDAO encomendaDAO;
    private EncomendaItemDAO encomendaItemDAO;
    private ItemEncomendaPadraoDAO itemPadraoDAO;

    private ObservableList<Viagem> obsListaViagens;
    private ObservableList<Rota> obsListaRotas;
    private ObservableList<String> listaMestraClientes;
    private List<ItemEncomendaPadrao> listaMestraProdutosObjetos;
    private ObservableList<EncomendaItem> obsListaItens;
    
    private Viagem viagemAtiva;
    private Encomenda encomendaEmEdicao;
    
    private ContextMenu menuSugestoesProdutos;
    private ContextMenu menuSugestoesRemetente;
    private ContextMenu menuSugestoesDestinatario;
    private ContextMenu menuSugestoesRota;

    private List<Rota> sugestoesRotaAtuais = new ArrayList<>();
    private List<String> sugestoesRemetenteAtuais = new ArrayList<>();
    private List<String> sugestoesDestinatarioAtuais = new ArrayList<>();
    private List<ItemEncomendaPadrao> sugestoesProdutoAtuais = new ArrayList<>();

    private int indexRemetenteSelecionado = -1;
    private int indexDestinatarioSelecionado = -1;
    private int indexRotaSelecionado = -1;
    private int indexProdutoSelecionado = -1;

    // DP028: debounce para autocomplete (evita stream filter a cada keystroke)
    private PauseTransition debounceClientes;
    private PauseTransition debounceRotas;
    private PauseTransition debounceProdutos;
    
    private boolean isSelecionandoViaEnter = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.viagemDAO = new ViagemDAO();
        this.rotaDAO = new RotaDAO();
        this.clienteEncomendaDAO = new ClienteEncomendaDAO();
        this.encomendaDAO = new EncomendaDAO();
        this.encomendaItemDAO = new EncomendaItemDAO();
        this.itemPadraoDAO = new ItemEncomendaPadraoDAO();

        this.obsListaViagens = FXCollections.observableArrayList();
        this.obsListaRotas = FXCollections.observableArrayList();
        this.obsListaItens = FXCollections.observableArrayList();
        this.listaMestraClientes = FXCollections.observableArrayList();
        this.listaMestraProdutosObjetos = new ArrayList<>();
        
        this.menuSugestoesProdutos = criarMenuConfigurado();
        this.menuSugestoesRemetente = criarMenuConfigurado();
        this.menuSugestoesDestinatario = criarMenuConfigurado();
        this.menuSugestoesRota = criarMenuConfigurado();

        configurarTabela();
        configurarListenersDeCampos();
        configurarValidacaoFocoClientes();

        configurarAutocompleteGenerico(cmbRemetente, menuSugestoesRemetente, "R");
        configurarAutocompleteGenerico(cmbDestinatario, menuSugestoesDestinatario, "D");
        configurarAutoCompleteRota(cmbRota);
        configurarAutocompleteProdutosNoTextField();

        aplicarEstiloBotoes();

        // Carrega dados do banco em background (DR010)
        javafx.concurrent.Task<model.Viagem> taskInit = new javafx.concurrent.Task<model.Viagem>() {
            @Override protected model.Viagem call() throws Exception {
                carregarComboBoxes();
                carregarCatalogoProdutos();
                return viagemDAO.buscarViagemAtiva();
            }
        };
        taskInit.setOnSucceeded(ev -> {
            viagemAtiva = taskInit.getValue();
            atualizarLabelViagem();
        });
        taskInit.setOnFailed(ev -> {
            AppLogger.warn("InserirEncomendaController", "Erro ao carregar dados iniciais: " + taskInit.getException().getMessage());
        });
        Thread tInit = new Thread(taskInit);
        tInit.setDaemon(true);
        tInit.start();

        cmbRota.getSelectionModel().selectedItemProperty().addListener((obs, oldRota, newRota) -> {
            if (newRota != null && encomendaEmEdicao == null) {
                atualizarNumeroEncomenda();
            }
        });
        
        btnImprimir.setDisable(true);
        btnExcluir.setDisable(true);
        btnEntregar.setDisable(true);

        tableItens.setItems(obsListaItens);
        handleIniciar(null);
        
        cmbRemetente.setEditable(true);
        cmbDestinatario.setEditable(true);
        cmbRota.setEditable(true);
        
        cmbRemetente.setMaxWidth(Double.MAX_VALUE);
        cmbDestinatario.setMaxWidth(Double.MAX_VALUE);
        
        cmbRota.setConverter(new StringConverter<Rota>() {
            @Override public String toString(Rota object) { return (object == null) ? "" : object.toString(); }
            @Override public Rota fromString(String string) {
                if (string == null) return null;
                String s = string.trim();
                if (s.isEmpty()) return null;
                String upper = s.toUpperCase();
                for (Rota r : obsListaRotas) { if (r.toString().equalsIgnoreCase(s)) return r; }
                for (Rota r : obsListaRotas) { if (r.toString().toUpperCase().contains(upper)) return r; }
                return cmbRota.getValue();
            }
        });
        
        btnSalvar.setDefaultButton(false);
        
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    switch (event.getCode()) {
                        case F2:
                            handleIniciar(null); event.consume(); break;
                        case F3:
                            if (!btnSalvar.isDisabled()) handleSalvar(null);
                            event.consume(); break;
                        case ESCAPE:
                            handleSair(null); event.consume(); break;
                        default: break;
                    }
                });
            }
        });
        
        Platform.runLater(() -> {
            if (btnSalvar != null && btnSalvar.getParent() != null) {
                btnSalvar.getParent().setStyle("-fx-background-color: white; -fx-padding: 10px; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
            }
        });
    }

    @FXML
    public void handleImageInput(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione a Foto");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            gui.util.OcrAudioService.executarOCRAsync(file,
                resultado -> interpretarTextoEPreencher(resultado),
                e -> Platform.runLater(() -> AlertHelper.show(AlertType.ERROR, "Erro OCR", "Erro: " + e.getMessage()))
            );
        }
    }

    @FXML
    public void handleAudioInput(ActionEvent event) {
        if(btnAudioInput.getText().contains("Ouvindo")) return;
        btnAudioInput.setText("Ouvindo... (Fale agora)");
        btnAudioInput.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white;"); 
        gui.util.OcrAudioService.executarVozAsync(
            texto -> {
                interpretarTextoEPreencher(texto);
                Platform.runLater(() -> {
                    btnAudioInput.setText("Microfone");
                    btnAudioInput.setStyle("-fx-background-color: #047857; -fx-text-fill: white;");
                });
            },
            e -> Platform.runLater(() -> {
                AlertHelper.show(AlertType.ERROR, "Erro Audio", "Erro: " + e.getMessage());
                btnAudioInput.setText("Microfone");
                btnAudioInput.setStyle("-fx-background-color: #047857; -fx-text-fill: white;");
            })
        );
    }

    private void interpretarTextoEPreencher(String texto) {
        if (texto == null || texto.isEmpty()) {
            Platform.runLater(() -> AlertHelper.show(AlertType.WARNING, "Aviso", "Nenhum texto identificado."));
            return;
        }
        String[] linhas = texto.split("\n");
        Platform.runLater(() -> {
            for (String linha : linhas) {
                linha = linha.trim().toUpperCase();
                if (linha.isEmpty()) continue;
                if (linha.contains("Nº") || linha.contains("NUMERO")) {
                    String numero = extrairApenasNumeros(linha);
                    if (!numero.isEmpty()) txtNumeroEncomenda.setText(numero);
                }
                else if (linha.startsWith("REM:") || linha.startsWith("REMETENTE")) {
                    String valor = extrairValor(linha);
                    if (!valor.isEmpty()) cmbRemetente.getEditor().setText(valor);
                }
                else if (linha.startsWith("DEST:") || linha.startsWith("DESTINATARIO") || linha.startsWith("PARA:")) {
                    String valor = extrairValor(linha);
                    if (!valor.isEmpty()) cmbDestinatario.getEditor().setText(valor);
                }
                else if (linha.startsWith("ROTA:")) {
                    String valor = extrairValor(linha);
                    for (Rota r : obsListaRotas) {
                        if (r.toString().toUpperCase().contains(valor)) { cmbRota.setValue(r); break; }
                    }
                }
                else if (Character.isDigit(linha.charAt(0)) && linha.contains(" ")) {
                    try {
                        String[] partes = linha.split(" ", 2);
                        if (partes.length >= 2) {
                            String qtdStr = partes[0].replaceAll("[^0-9]", "");
                            String descLida = partes[1].trim(); 
                            if (!qtdStr.isEmpty() && descLida.length() > 2 && !descLida.contains("/")) {
                                int qtd = Integer.parseInt(qtdStr);
                                BigDecimal precoEncontrado = BigDecimal.ZERO;
                                boolean achou = false;
                                for (ItemEncomendaPadrao prod : listaMestraProdutosObjetos) {
                                    if (prod.getNomeItem().equals(descLida) || descLida.contains(prod.getNomeItem())) {
                                        precoEncontrado = prod.getPrecoUnit();
                                        descLida = prod.getNomeItem(); 
                                        achou = true;
                                        break;
                                    }
                                }
                                if (!achou) {
                                    TextInputDialog dialog = new TextInputDialog();
                                    dialog.setTitle("Novo Item Detectado");
                                    dialog.setHeaderText("Item não cadastrado: " + descLida);
                                    dialog.setContentText("Informe o valor unitário:");
                                    Optional<String> result = dialog.showAndWait();
                                    if (result.isPresent()) {
                                        try {
                                            String valStr = result.get().replace(",", ".");
                                            precoEncontrado = new BigDecimal(valStr);
                                            ItemEncomendaPadrao novo = new ItemEncomendaPadrao();
                                            novo.setNomeItem(descLida); novo.setPrecoUnit(precoEncontrado);
                                            itemPadraoDAO.salvar(novo);
                                            listaMestraProdutosObjetos.add(novo);
                                        } catch (Exception ex) { precoEncontrado = BigDecimal.ZERO; }
                                    }
                                }
                                EncomendaItem item = new EncomendaItem();
                                item.setQuantidade(qtd); item.setDescricao(descLida);
                                item.setValorUnitario(precoEncontrado);
                                item.setValorTotal(precoEncontrado.multiply(new BigDecimal(qtd)));
                                obsListaItens.add(item);
                            }
                        }
                    } catch (Exception e) { AppLogger.warn("InserirEncomendaController", "Erro em InserirEncomendaController.processarIA (item): " + e.getMessage()); }
                }
            }
            atualizarTotaisEncomenda(); 
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Processo Concluído");
            alert.setHeaderText("Encomenda Pré-preenchida");
            alert.setContentText("Confira os itens.");
            alert.show();
        });
    }

    private String extrairValor(String linha) {
        if (linha.contains(":")) { return linha.substring(linha.indexOf(":") + 1).trim(); }
        return linha;
    }
    private String extrairApenasNumeros(String texto) { return texto.replaceAll("[^0-9]", ""); }

    private void aplicarEstiloBotoes() {
        estilizarBotao(btnSalvar, "#059669"); 
        estilizarBotao(btnIniciar, "#059669"); 
        estilizarBotao(btnEntregar, "#0369A1");
        estilizarBotao(btnImprimir, "#7BA393"); 
        estilizarBotao(btnExcluir, "#DC2626"); 
        estilizarBotao(btnEditar, "#F59E0B"); 
        estilizarBotao(btnSair, "#424242"); 
        if(btnAdicionarItem != null) {
            btnAdicionarItem.setStyle("-fx-background-color: #FBBF24; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
            btnAdicionarItem.setOnMousePressed(e -> btnAdicionarItem.setStyle("-fx-background-color: #B45309; -fx-text-fill: black; -fx-font-weight: bold;"));
            btnAdicionarItem.setOnMouseReleased(e -> btnAdicionarItem.setStyle("-fx-background-color: #FBBF24; -fx-text-fill: black; -fx-font-weight: bold;"));
        }
        String estiloAzulForte = "-fx-background-color: #047857; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;";
        if(btnAudioInput != null) btnAudioInput.setStyle(estiloAzulForte);
        if(btnImageInput != null) btnImageInput.setStyle(estiloAzulForte);
    }

    private void estilizarBotao(Button btn, String corHex) {
        if (btn == null) return;
        String estiloNormal = "-fx-background-color: " + corHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4;";
        String estiloPressionado = "-fx-background-color: derive(" + corHex + ", -20%); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;";
        btn.setStyle(estiloNormal);
        btn.setOnMousePressed(e -> btn.setStyle(estiloPressionado));
        btn.setOnMouseReleased(e -> btn.setStyle(estiloNormal));
    }
    
    private void configurarTabela() {
        tableItens.setEditable(true);
        tableItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        colQuant.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQuant.setMinWidth(50); colQuant.setMaxWidth(70);
        colQuant.setEditable(true);
        
        // Coluna Descrição com ComboBox para seleção de produtos
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDesc.setCellFactory(tc -> new TableCell<EncomendaItem, String>() {
            private ComboBox<String> comboBox;
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(null);
                }
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                if (comboBox == null) {
                    comboBox = new ComboBox<>();
                    comboBox.setEditable(true);
                    comboBox.setMaxWidth(Double.MAX_VALUE);
                    // Carregar lista de produtos
                    List<String> nomesProdutos = listaMestraProdutosObjetos.stream()
                        .map(p -> p.getNomeItem())
                        .collect(Collectors.toList());
                    comboBox.getItems().addAll(nomesProdutos);
                    comboBox.getEditor().setText(getItem() == null ? "" : getItem());
                    
                    // Ao selecionar um produto, preencher valor automaticamente
                    comboBox.setOnAction(e -> {
                        String selected = comboBox.getValue();
                        if (selected != null) {
                            commitEdit(selected.toUpperCase());
                            // Buscar valor do produto
                            ItemEncomendaPadrao prod = listaMestraProdutosObjetos.stream()
                                .filter(p -> p.getNomeItem().equalsIgnoreCase(selected))
                                .findFirst().orElse(null);
                            if (prod != null && getTableRow() != null && getTableRow().getItem() != null) {
                                EncomendaItem itemEnc = getTableRow().getItem();
                                if (itemEnc.getValorUnitario() == null || itemEnc.getValorUnitario().compareTo(BigDecimal.ZERO) == 0) {
                                    itemEnc.setValorUnitario(prod.getPrecoUnit());
                                    itemEnc.setValorTotal(prod.getPrecoUnit().multiply(new BigDecimal(itemEnc.getQuantidade())));
                                    atualizarTotaisEncomenda();
                                }
                            }
                        }
                    });
                    
                    comboBox.getEditor().setOnKeyPressed(ev -> {
                        if (ev.getCode() == KeyCode.ENTER) {
                            commitEdit(comboBox.getEditor().getText().toUpperCase());
                        } else if (ev.getCode() == KeyCode.ESCAPE) {
                            cancelEdit();
                        } else if (ev.getCode() == KeyCode.TAB) {
                            commitEdit(comboBox.getEditor().getText().toUpperCase());
                            // Mover para próxima coluna
                            int row = getIndex();
                            Platform.runLater(() -> tableItens.edit(row, colValUnit));
                        }
                    });
                    
                    comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused && isEditing()) {
                            commitEdit(comboBox.getEditor().getText().toUpperCase());
                        }
                    });
                }
                comboBox.getEditor().setText(getItem() == null ? "" : getItem());
                setText(null);
                setGraphic(comboBox);
                comboBox.getEditor().selectAll();
                comboBox.requestFocus();
                comboBox.getEditor().requestFocus();
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }
            
            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().setDescricao(newValue);
                }
                setText(newValue);
                setGraphic(null);
            }
        });
        colDesc.setMinWidth(150);
        colDesc.setEditable(true);
        
        // Coluna Valor Unitário
        colValUnit.setCellValueFactory(new PropertyValueFactory<>("valorUnitario"));
        colValUnit.setCellFactory(tc -> new TableCell<EncomendaItem, BigDecimal>() {
            private TextField textField;
            
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item == null ? "" : String.format("%.2f", item));
                    setGraphic(null);
                }
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(e -> commitarValor());
                    textField.setOnKeyPressed(ev -> {
                        if (ev.getCode() == KeyCode.ESCAPE) cancelEdit();
                        else if (ev.getCode() == KeyCode.TAB) {
                            commitarValor();
                            int row = getIndex();
                            Platform.runLater(() -> tableItens.edit(row, colLocalArmazenamento));
                        }
                    });
                    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused && isEditing()) commitarValor();
                    });
                }
                textField.setText(getItem() == null ? "" : String.format("%.2f", getItem()));
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
            
            private void commitarValor() {
                BigDecimal valor = parseBigDecimal(textField.getText());
                commitEdit(valor);
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() == null ? "" : String.format("%.2f", getItem()));
                setGraphic(null);
            }
            
            @Override
            public void commitEdit(BigDecimal newValue) {
                super.commitEdit(newValue);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    EncomendaItem item = getTableRow().getItem();
                    item.setValorUnitario(newValue);
                    item.setValorTotal(newValue.multiply(new BigDecimal(item.getQuantidade())));
                    atualizarTotaisEncomenda();
                }
                setText(newValue == null ? "" : String.format("%.2f", newValue));
                setGraphic(null);
            }
        });
        colValUnit.setEditable(true);
        colValUnit.setMinWidth(65); colValUnit.setMaxWidth(95);
        colValTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));
        colValTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("R$ %,.2f", item));
            }
        });
        colValTotal.setMinWidth(80); colValTotal.setMaxWidth(110);
        
        colLocalArmazenamento.setCellValueFactory(new PropertyValueFactory<>("localArmazenamento"));
        colLocalArmazenamento.setCellFactory(TextFieldTableCell.forTableColumn());
        colLocalArmazenamento.setOnEditCommit(event -> {
            EncomendaItem item = event.getRowValue();
            item.setLocalArmazenamento(event.getNewValue());
        });
        colLocalArmazenamento.setMinWidth(80); colLocalArmazenamento.setMaxWidth(200);
        colLocalArmazenamento.setEditable(true);
        
        // Configurar edição da coluna Quantidade
        colQuant.setCellFactory(tc -> {
            TableCell<EncomendaItem, Integer> cell = new TableCell<>() {
                private TextField textField;
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setText(null); setGraphic(null); }
                    else { setText(item == null ? "" : item.toString()); setGraphic(null); }
                }
                @Override
                public void startEdit() {
                    super.startEdit();
                    if (textField == null) {
                        textField = new TextField(getItem() == null ? "" : getItem().toString());
                        textField.setOnAction(e -> {
                            try { commitEdit(Integer.parseInt(textField.getText())); } 
                            catch (NumberFormatException ex) { commitEdit(getItem()); }
                        });
                        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                            if (!isNowFocused) {
                                try { commitEdit(Integer.parseInt(textField.getText())); } 
                                catch (NumberFormatException ex) { commitEdit(getItem()); }
                            }
                        });
                    }
                    textField.setText(getItem() == null ? "" : getItem().toString());
                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    textField.requestFocus();
                }
                @Override
                public void cancelEdit() { super.cancelEdit(); setText(getItem() == null ? "" : getItem().toString()); setGraphic(null); }
                @Override
                public void commitEdit(Integer newValue) {
                    super.commitEdit(newValue);
                    EncomendaItem item = getTableView().getItems().get(getIndex());
                    item.setQuantidade(newValue);
                    item.setValorTotal(item.getValorUnitario().multiply(new BigDecimal(newValue)));
                    // Forçar atualização visual sem perder dados
                    getTableView().getColumns().get(0).setVisible(false);
                    getTableView().getColumns().get(0).setVisible(true);
                    atualizarTotaisEncomenda();
                }
            };
            cell.setStyle("-fx-alignment: CENTER;");
            return cell;
        });
        colQuant.setEditable(true);
        
        // Menu de contexto para adicionar/remover itens na tabela
        ContextMenu menuTabela = new ContextMenu();
        MenuItem menuAddItem = new MenuItem("Adicionar Novo Item");
        menuAddItem.setOnAction(e -> adicionarNovoItemNaTabela());
        MenuItem menuRemoveItem = new MenuItem("Remover Item Selecionado");
        menuRemoveItem.setOnAction(e -> {
            EncomendaItem selecionado = tableItens.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                obsListaItens.remove(selecionado);
                atualizarTotaisEncomenda();
            }
        });
        menuTabela.getItems().addAll(menuAddItem, menuRemoveItem);
        tableItens.setContextMenu(menuTabela);
        
        // Clique único: editar item existente OU adicionar novo em linha vazia
        tableItens.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1 && tableItens.isEditable()) {
                EncomendaItem selecionado = tableItens.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    // Editar item existente
                    int row = tableItens.getSelectionModel().getSelectedIndex();
                    Platform.runLater(() -> {
                        if (tableItens.getSelectionModel().getSelectedItem() != null) {
                            tableItens.edit(row, colDesc);
                        }
                    });
                } else {
                    // Clique em linha vazia - adicionar novo item imediatamente
                    adicionarNovoItemNaTabela();
                }
            }
        });
        
        // Permitir adicionar item com tecla INSERT
        tableItens.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.INSERT && tableItens.isEditable()) {
                adicionarNovoItemNaTabela();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE && tableItens.isEditable()) {
                EncomendaItem selecionado = tableItens.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    obsListaItens.remove(selecionado);
                    atualizarTotaisEncomenda();
                }
                event.consume();
            }
        });
    }
    
    private void adicionarNovoItemNaTabela() {
        EncomendaItem novoItem = new EncomendaItem();
        novoItem.setQuantidade(1);
        novoItem.setDescricao("");
        novoItem.setValorUnitario(BigDecimal.ZERO);
        novoItem.setValorTotal(BigDecimal.ZERO);
        novoItem.setLocalArmazenamento("");
        obsListaItens.add(novoItem);
        atualizarTotaisEncomenda();
        // Selecionar e iniciar edição do novo item
        Platform.runLater(() -> {
            tableItens.getSelectionModel().select(novoItem);
            tableItens.scrollTo(novoItem);
            tableItens.edit(obsListaItens.indexOf(novoItem), colDesc);
        });
    }

    @FXML
    public void handleEntregar(ActionEvent event) {
        if (encomendaEmEdicao == null || encomendaEmEdicao.getId() == null) {
            AlertHelper.show(AlertType.WARNING, "Operação Não Permitida", "A encomenda não foi localizada corretamente.\nTente salvar (F3) ou recarregar a tela antes de entregar.");
            return;
        }
        // DL051: impedir re-entrega que sobrescreveria dados do recebedor original
        if (encomendaEmEdicao.isEntregue()) {
            String recebedorOriginal = encomendaEmEdicao.getNomeRecebedor();
            Alert confirmReentrega = new Alert(AlertType.CONFIRMATION,
                "Esta encomenda já foi entregue" + (recebedorOriginal != null ? " para " + recebedorOriginal : "") + ".\nDeseja realmente registrar nova entrega? Os dados do recebedor anterior serão substituídos.",
                ButtonType.YES, ButtonType.NO);
            confirmReentrega.setTitle("Encomenda Já Entregue");
            Optional<ButtonType> res = confirmReentrega.showAndWait();
            if (!res.isPresent() || res.get() != ButtonType.YES) return;
        }
        try {
            // Salvar o ID antes de qualquer operação que possa limpar encomendaEmEdicao
            final Long idEncomenda = encomendaEmEdicao.getId();
            final String destinatarioOriginal = encomendaEmEdicao.getDestinatario();

            recarregarDadosFinanceiros(idEncomenda);
            BigDecimal total = encomendaEmEdicao.getTotalAPagar();
            BigDecimal pagoInicial = (encomendaEmEdicao.getValorPago() != null) ? encomendaEmEdicao.getValorPago() : BigDecimal.ZERO;

            if (pagoInicial.compareTo(total) < 0) {
                abrirTelaPagamento();
                // Após pagamento, finalizarSalvamento pode ter limpado encomendaEmEdicao
                // Recarregar a encomenda do banco usando o ID salvo
                recarregarDadosFinanceiros(idEncomenda);
                if (encomendaEmEdicao == null) {
                    // finalizarSalvamento limpou - recarregar manualmente
                    encomendaEmEdicao = encomendaDAO.buscarPorId(idEncomenda);
                    if (encomendaEmEdicao == null) {
                        AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível recarregar a encomenda após o pagamento.");
                        return;
                    }
                }
                // Atualizar total após pagamento
                total = encomendaEmEdicao.getTotalAPagar();
            }

            // Se o usuário já digitou o nome/documento no formulário principal, usar sem abrir diálogo
            String mainNome = (txtNomeRecebedor != null) ? txtNomeRecebedor.getText().trim() : "";
            String mainDoc  = (txtNDocumentoRecebedor != null) ? txtNDocumentoRecebedor.getText().trim() : "";
            if (!mainNome.isEmpty() && !mainNome.equalsIgnoreCase("Pendente de Entrega")) {
                finalizarEntrega(encomendaEmEdicao, mainNome, mainDoc, total, event);
                return;
            }

            // Capturar referências finais para uso no lambda
            final BigDecimal totalFinal = total;

            Dialog<Pair<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Finalizar Entrega");
            dialog.setHeaderText("Informe os dados de quem está recebendo a encomenda:");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setOnShowing(e -> {
                Stage stg = (Stage) dialog.getDialogPane().getScene().getWindow();
                stg.setAlwaysOnTop(true);
                stg.toFront();
            });
            ButtonType btnConfirmar = new ButtonType("Confirmar e Imprimir", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
            TextField txtNomeRecebedorDlg = new TextField();
            txtNomeRecebedorDlg.setPromptText("Nome do Recebedor");
            TextField txtDocRecebedorDlg = new TextField();
            txtDocRecebedorDlg.setPromptText("Documento (Opcional)");
            if (destinatarioOriginal != null) {
                txtNomeRecebedorDlg.setText(destinatarioOriginal);
            }
            grid.add(new Label("Nome Recebedor:"), 0, 0); grid.add(txtNomeRecebedorDlg, 1, 0);
            grid.add(new Label("Documento:"), 0, 1); grid.add(txtDocRecebedorDlg, 1, 1);
            dialog.getDialogPane().setContent(grid);
            Platform.runLater(txtNomeRecebedorDlg::requestFocus);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnConfirmar) return new Pair<>(txtNomeRecebedorDlg.getText(), txtDocRecebedorDlg.getText());
                return null;
            });
            Optional<Pair<String, String>> result = dialog.showAndWait();
            result.ifPresent(dadosRecebedor -> {
                String nome = dadosRecebedor.getKey().trim();
                String doc = dadosRecebedor.getValue().trim();
                if (nome.isEmpty()) {
                    AlertHelper.show(AlertType.WARNING, "Atenção", "O Nome do recebedor é obrigatório para finalizar.");
                    return;
                }
                // Usar encomendaEmEdicao se ainda disponível, ou recarregar do banco
                Encomenda enc = (encomendaEmEdicao != null) ? encomendaEmEdicao : encomendaDAO.buscarPorId(idEncomenda);
                if (enc == null) {
                    AlertHelper.show(AlertType.ERROR, "Erro", "Não foi possível localizar a encomenda.");
                    return;
                }
                finalizarEntrega(enc, nome, doc, totalFinal, event);
            });
        } catch (Exception e) {
            AppLogger.error("InserirEncomendaController", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro Crítico", "Erro ao processar entrega: " + e.getMessage());
        }
    }

    /**
     * Executa os passos comuns de finalização de entrega:
     * calcula status financeiro, registra no banco, atualiza o modelo e imprime o cupom.
     *
     * @param enc   encomenda a ser entregue
     * @param nome  nome do recebedor (será convertido para maiúsculas)
     * @param doc   documento do recebedor (será convertido para maiúsculas)
     * @param total valor total da encomenda (usado para calcular o status financeiro)
     * @param event evento de origem (repassado para handleSair)
     */
    private void finalizarEntrega(Encomenda enc, String nome, String doc, BigDecimal total, ActionEvent event) {
        BigDecimal valorPagoAtualizado = (enc.getValorPago() != null) ? enc.getValorPago() : BigDecimal.ZERO;
        String statusFinanceiroFinal = (valorPagoAtualizado.compareTo(total) >= 0) ? "PAGO" : "PENDENTE";
        String nomeFormatado = nome.toUpperCase();
        String docFormatado = doc.toUpperCase();

        boolean sucesso = encomendaDAO.registrarEntrega(enc.getId(), docFormatado, nomeFormatado, statusFinanceiroFinal);
        if (sucesso) {
            enc.setEntregue(true);
            enc.setNomeRecebedor(nomeFormatado);
            enc.setDocRecebedor(docFormatado);
            encomendaEmEdicao = enc;
            setEncomendaParaEdicao(enc);
            imprimirCupomTermico(enc);
            handleSair(event);
        } else {
            AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao registrar entrega no banco de dados.");
        }
    }

    private void imprimirCupomTermico(Encomenda encomenda) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) { AlertHelper.show(AlertType.ERROR, "Erro", "Nenhuma impressora encontrada."); return; }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;
        javafx.print.PageLayout pageLayout = printer.createPageLayout(printer.getDefaultPageLayout().getPaper(), javafx.print.PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(pageLayout);
        EmpresaDAO empresaDAO = new EmpresaDAO();
        Empresa empresa = empresaDAO.buscarPorId(dao.EmpresaDAO.ID_EMPRESA_PRINCIPAL);
        double larguraBase = 270; 
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(larguraBase); root.setMaxWidth(larguraBase); root.setAlignment(Pos.TOP_LEFT);
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER); headerBox.setPrefWidth(larguraBase);
        if (empresa != null) {
            if (empresa.getCaminhoFoto() != null && !empresa.getCaminhoFoto().isEmpty()) {
                try {
                    ImageView logo = new ImageView(gui.util.ImageCache.get(empresa.getCaminhoFoto()));
                    logo.setFitWidth(50); logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                } catch (Exception e) { /* logo opcional */ }
            }
            Label lblEmpresa = new Label(empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "EMBARCAÇÃO");
            lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: black;");
            String dados = "";
            if(empresa.getCnpj() != null) dados += "CNPJ: " + empresa.getCnpj() + "\n";
            if(empresa.getTelefone() != null) dados += "Tel: " + empresa.getTelefone() + "\n";
            if(empresa.getEndereco() != null && !empresa.getEndereco().isEmpty()) dados += empresa.getEndereco(); 
            Label lblDados = new Label(dados);
            lblDados.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().addAll(lblEmpresa, lblDados);
        }
        root.getChildren().add(headerBox);
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER); infoBox.setPadding(new Insets(10, 0, 5, 0)); 
        Label lblTitulo = new Label("RECIBO DE ENCOMENDA");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        Label lblNum = new Label("Nº " + encomenda.getNumeroEncomenda());
        lblNum.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-border-color: black; -fx-border-width: 2px; -fx-padding: 3px 15px 3px 15px; -fx-text-fill: black;");
        infoBox.getChildren().addAll(lblTitulo, lblNum);
        root.getChildren().add(infoBox);
        VBox boxClientes = new VBox(2);
        boxClientes.setAlignment(Pos.CENTER_LEFT);
        Label lblRem = new Label("REM: " + encomenda.getRemetente());
        lblRem.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        Label lblDest = new Label("DEST: " + encomenda.getDestinatario());
        lblDest.setStyle("-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;"); 
        Label lblRota = new Label("ROTA: " + (encomenda.getNomeRota() != null ? encomenda.getNomeRota() : "--"));
        lblRota.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        lblRem.setWrapText(true); lblDest.setWrapText(true);
        boxClientes.getChildren().addAll(lblRem, lblDest, lblRota);
        boxClientes.setPadding(new Insets(5, 0, 5, 0));
        root.getChildren().addAll(boxClientes);
        GridPane grid = new GridPane();
        grid.setHgap(0); grid.setVgap(0);
        String styleHeaderBase = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
        String styleHeaderLast = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 0 1 0;";
        String styleCellNormal = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
        String styleCellLast   = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 0 1 0;"; 
        double wQtd = 30; double wDesc = 110; double wUnit = 60; double wTotal = 70;
        Label hQtd = new Label("QTD"); hQtd.setStyle(styleHeaderBase); hQtd.setPrefWidth(wQtd); hQtd.setAlignment(Pos.CENTER);
        Label hDesc = new Label("DESC."); hDesc.setStyle(styleHeaderBase); hDesc.setPrefWidth(wDesc); hDesc.setAlignment(Pos.CENTER_LEFT);
        Label hUnit = new Label("V.UN"); hUnit.setStyle(styleHeaderBase); hUnit.setPrefWidth(wUnit); hUnit.setAlignment(Pos.CENTER_RIGHT);
        Label hTotal = new Label("TOTAL"); hTotal.setStyle(styleHeaderLast); hTotal.setPrefWidth(wTotal); hTotal.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);
        List<EncomendaItem> itens = encomendaItemDAO.listarPorIdEncomenda(encomenda.getId());
        int l = 1;
        for(EncomendaItem i : itens){
            Label q = new Label(String.valueOf(i.getQuantidade()));
            q.setStyle(styleCellNormal); q.setPrefWidth(wQtd); q.setAlignment(Pos.CENTER);
            Label d = new Label(i.getDescricao());
            d.setStyle(styleCellNormal); d.setPrefWidth(wDesc); d.setWrapText(true); d.setAlignment(Pos.CENTER_LEFT);
            Label vu = new Label(String.format("%,.2f", i.getValorUnitario()));
            vu.setStyle(styleCellNormal); vu.setPrefWidth(wUnit); vu.setAlignment(Pos.CENTER_RIGHT);
            Label vt = new Label(String.format("%,.2f", i.getValorTotal()));
            vt.setStyle(styleCellLast); vt.setPrefWidth(wTotal); vt.setAlignment(Pos.CENTER_RIGHT);
            grid.add(q, 0, l); grid.add(d, 1, l); grid.add(vu, 2, l); grid.add(vt, 3, l);
            l++;
        }
        root.getChildren().add(grid);
        Label lblVol = new Label("VOLUMES: " + encomenda.getTotalVolumes());
        lblVol.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        Label espaco = new Label(" ");
        espaco.setMinHeight(25); 
        root.getChildren().addAll(lblVol, espaco);
        VBox boxValores = new VBox(3);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        boxValores.setPadding(new Insets(0, 5, 0, 0));
        Label lblTotal = new Label("TOTAL: R$ " + String.format("%,.2f", encomenda.getTotalAPagar()));
        lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: black;"); 
        BigDecimal pg = encomenda.getValorPago()!=null?encomenda.getValorPago():BigDecimal.ZERO;
        Label lblPago = new Label("PAGO: R$ " + String.format("%,.2f", pg));
        lblPago.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        String statusTxt = (pg.compareTo(encomenda.getTotalAPagar())>=0?"QUITADO":"PENDENTE");
        Label lblStatus = new Label("STATUS: " + statusTxt);
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;");
        boxValores.getChildren().addAll(lblTotal, lblPago, lblStatus);
        root.getChildren().add(boxValores);
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle("-fx-font-size: 9px; -fx-text-fill: black;");
        lblData.setTextAlignment(TextAlignment.CENTER);
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);
        double largImp = pageLayout.getPrintableWidth();
        if(largImp > 0 && largImp < larguraBase) { 
            double sc = largImp/larguraBase; 
            root.getTransforms().add(new Scale(sc,sc)); 
        }
        if(job.printPage(root)) job.endJob();
        else AlertHelper.show(AlertType.ERROR, "Erro", "Falha na impressão.");
    }

    private ContextMenu criarMenuConfigurado() {
        ContextMenu menu = new ContextMenu();
        menu.setAutoHide(true); menu.setHideOnEscape(true);
        return menu;
    }
    
    private void configurarAutocompleteGenerico(ComboBox<String> cmb, ContextMenu menu, String tipo) {
         cmb.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (menu.isShowing()) {
                    List<String> lista = (tipo.equals("R")) ? sugestoesRemetenteAtuais : sugestoesDestinatarioAtuais;
                    int idx = (tipo.equals("R")) ? indexRemetenteSelecionado : indexDestinatarioSelecionado;
                    if(!lista.isEmpty() && idx >= 0 && idx < lista.size()){
                        cmb.setValue(lista.get(idx));
                        menu.hide();
                    }
                    event.consume();
                    Platform.runLater(() -> { if(tipo.equals("R")) cmbDestinatario.requestFocus(); else cmbRota.requestFocus(); });
                } else {
                    event.consume();
                    Platform.runLater(() -> { if(tipo.equals("R")) cmbDestinatario.requestFocus(); else cmbRota.requestFocus(); });
                }
            } else if (event.getCode() == KeyCode.DOWN) { navegarMenu(menu, 1, tipo); event.consume(); }
              else if (event.getCode() == KeyCode.UP) { navegarMenu(menu, -1, tipo); event.consume(); }
              else if (event.getCode() == KeyCode.ESCAPE) { menu.hide(); }
         });
         // DP028: debounce 250ms para evitar stream filter a cada keystroke
         if (debounceClientes == null) debounceClientes = new PauseTransition(Duration.millis(250));
         final PauseTransition db = debounceClientes;
         cmb.getEditor().setOnKeyReleased(e -> {
             if(isNavegacaoKey(e.getCode())) return;
             db.setOnFinished(ev -> {
                 String txt = cmb.getEditor().getText().toUpperCase();
                 List<String> achados = listaMestraClientes.stream().filter(c -> c.contains(txt)).collect(Collectors.toList());
                 if(!achados.isEmpty()) {
                     menu.getItems().clear();
                     if(tipo.equals("R")) sugestoesRemetenteAtuais = achados; else sugestoesDestinatarioAtuais = achados;
                     for(String s : achados) {
                         MenuItem mi = new MenuItem(s);
                         mi.setOnAction(ev2 -> { cmb.setValue(s); menu.hide(); });
                         menu.getItems().add(mi);
                     }
                     menu.show(cmb, Side.BOTTOM, 0,0);
                 } else menu.hide();
             });
             db.playFromStart();
         });
    }

    private void configurarAutoCompleteRota(ComboBox<Rota> cmb) {
         cmb.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (menuSugestoesRota.isShowing() && !sugestoesRotaAtuais.isEmpty()) {
                    if (indexRotaSelecionado >= 0 && indexRotaSelecionado < sugestoesRotaAtuais.size()) {
                        cmb.setValue(sugestoesRotaAtuais.get(indexRotaSelecionado));
                        menuSugestoesRota.hide();
                    }
                    event.consume();
                    Platform.runLater(() -> txtQuantidade.requestFocus());
                } else {
                    event.consume();
                    Platform.runLater(() -> txtQuantidade.requestFocus());
                }
            } else if (event.getCode() == KeyCode.DOWN) { navegarMenu(menuSugestoesRota, 1, "ROTA"); event.consume(); }
              else if (event.getCode() == KeyCode.UP) { navegarMenu(menuSugestoesRota, -1, "ROTA"); event.consume(); }
              else if (event.getCode() == KeyCode.ESCAPE) { menuSugestoesRota.hide(); }
         });
         // DP028: debounce 250ms para rotas
         if (debounceRotas == null) debounceRotas = new PauseTransition(Duration.millis(250));
         cmb.getEditor().setOnKeyReleased(e -> {
             if(isNavegacaoKey(e.getCode())) return;
             debounceRotas.setOnFinished(ev -> {
                 String txt = cmb.getEditor().getText().toUpperCase();
                 List<Rota> achados = obsListaRotas.stream().filter(r -> r.toString().toUpperCase().contains(txt)).collect(Collectors.toList());
                 if(!achados.isEmpty()) {
                     menuSugestoesRota.getItems().clear();
                     sugestoesRotaAtuais = achados;
                     for(Rota r : achados) {
                         MenuItem mi = new MenuItem(r.toString());
                         mi.setOnAction(ev2 -> { cmb.setValue(r); menuSugestoesRota.hide(); txtQuantidade.requestFocus(); });
                         menuSugestoesRota.getItems().add(mi);
                     }
                     menuSugestoesRota.show(cmb, Side.BOTTOM, 0,0);
                 } else menuSugestoesRota.hide();
             });
             debounceRotas.playFromStart();
         });
    }
    
    private void configurarAutocompleteProdutosNoTextField() {
        // Popular ComboBox com itens padrão formatados com preço
        ObservableList<String> itensDescricao = FXCollections.observableArrayList();
        for(ItemEncomendaPadrao item : listaMestraProdutosObjetos) {
            String itemFormatado = String.format("%-50s R$ %8.2f", item.getNomeItem(), item.getPrecoUnit());
            itensDescricao.add(itemFormatado);
        }
        cmbDescricao.setItems(itensDescricao);
        
        // Configurar estilo da fonte para monoespaçado no dropdown com linhas zebradas
        cmbDescricao.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Linhas zebradas - alternar cores entre linhas pares e ímpares
                    String corFundo = (getIndex() % 2 == 0) ? "#f0f0f0" : "#ffffff";
                    setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                             "-fx-font-size: 14px; " +
                             "-fx-font-weight: bold; " +
                             "-fx-background-color: " + corFundo + "; " +
                             "-fx-padding: 8px;");
                }
            }
        });
        
        cmbDescricao.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (menuSugestoesProdutos.isShowing() && !sugestoesProdutoAtuais.isEmpty()) {
                    if (indexProdutoSelecionado >= 0 && indexProdutoSelecionado < sugestoesProdutoAtuais.size()) {
                         ItemEncomendaPadrao p = sugestoesProdutoAtuais.get(indexProdutoSelecionado);
                         cmbDescricao.getEditor().setText(p.getNomeItem());
                         txtValorUnit.setText(p.getPrecoUnit().toString().replace(".", ","));
                         menuSugestoesProdutos.hide();
                    }
                    event.consume();
                    Platform.runLater(() -> { txtValorUnit.requestFocus(); txtValorUnit.selectAll(); });
                } else {
                    event.consume();
                    Platform.runLater(() -> { txtValorUnit.requestFocus(); txtValorUnit.selectAll(); });
                }
            } else if (event.getCode() == KeyCode.DOWN) { navegarMenu(menuSugestoesProdutos, 1, "PROD"); event.consume(); }
              else if (event.getCode() == KeyCode.UP) { navegarMenu(menuSugestoesProdutos, -1, "PROD"); event.consume(); }
              else if (event.getCode() == KeyCode.ESCAPE) { menuSugestoesProdutos.hide(); }
        });
         // DP028: debounce 250ms para produtos
         if (debounceProdutos == null) debounceProdutos = new PauseTransition(Duration.millis(250));
         cmbDescricao.getEditor().setOnKeyReleased(e -> {
             if(isNavegacaoKey(e.getCode())) return;
             debounceProdutos.setOnFinished(ev -> {
                 String txt = cmbDescricao.getEditor().getText().toUpperCase();
                 if(txt.isEmpty()) { menuSugestoesProdutos.hide(); return; }
                 List<ItemEncomendaPadrao> achados = listaMestraProdutosObjetos.stream().filter(p -> p.getNomeItem().contains(txt)).collect(Collectors.toList());
                 if(!achados.isEmpty()) {
                     menuSugestoesProdutos.getItems().clear();
                     sugestoesProdutoAtuais = achados;
                     for(ItemEncomendaPadrao p : achados) {
                         MenuItem mi = new MenuItem(p.getNomeItem() + " - " + p.getPrecoUnit());
                         mi.setOnAction(ev2 -> {
                             cmbDescricao.getEditor().setText(p.getNomeItem());
                             txtValorUnit.setText(p.getPrecoUnit().toString().replace(".", ","));
                             menuSugestoesProdutos.hide();
                             txtValorUnit.requestFocus();
                         });
                         menuSugestoesProdutos.getItems().add(mi);
                     }
                     menuSugestoesProdutos.show(cmbDescricao, Side.BOTTOM, 0,0);
                 } else menuSugestoesProdutos.hide();
             });
             debounceProdutos.playFromStart();
         });
         
         // Listener para quando selecionar item do ComboBox
         cmbDescricao.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
             if(newVal != null && !newVal.trim().isEmpty()) {
                 // Extrair apenas o nome do item (antes do R$)
                 String nomeItem = newVal.split("R\\$")[0].trim();
                 cmbDescricao.getEditor().setText(nomeItem);
                 
                 for(ItemEncomendaPadrao p : listaMestraProdutosObjetos) {
                     if(p.getNomeItem().equals(nomeItem)) {
                         txtValorUnit.setText(p.getPrecoUnit().toString().replace(".", ","));
                         Platform.runLater(() -> { txtValorUnit.requestFocus(); txtValorUnit.selectAll(); });
                         break;
                     }
                 }
             }
         });
    }

    private void navegarMenu(ContextMenu menu, int dir, String tipo) {
        int size = menu.getItems().size();
        if(size == 0) return;
        int idx = -1;
        if (tipo.equals("R")) idx = indexRemetenteSelecionado;
        else if (tipo.equals("D")) idx = indexDestinatarioSelecionado;
        else if (tipo.equals("ROTA")) idx = indexRotaSelecionado;
        else idx = indexProdutoSelecionado;
        if (idx < 0) idx = 0;
        idx += dir;
        if (idx < 0) idx = 0;
        if (idx >= size) idx = size - 1;
        if (tipo.equals("R")) indexRemetenteSelecionado = idx;
        else if (tipo.equals("D")) indexDestinatarioSelecionado = idx;
        else if (tipo.equals("ROTA")) indexRotaSelecionado = idx;
        else indexProdutoSelecionado = idx;
        atualizarVisualMenu(menu, idx);
    }

    private void atualizarVisualMenu(ContextMenu menu, int selectedIndex) {
        for (int i = 0; i < menu.getItems().size(); i++) {
            MenuItem item = menu.getItems().get(i);
            if (item instanceof CustomMenuItem) {
                Node content = ((CustomMenuItem) item).getContent();
                if (content instanceof HBox) {
                    HBox box = (HBox) content;
                    Label lbl = (Label) box.getChildren().get(0);
                    estilizarItem((CustomMenuItem) item, lbl, i == selectedIndex);
                }
            }
        }
    }

    private void estilizarItem(CustomMenuItem item, Label label, boolean isSelected) {
        HBox box = new HBox(label);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setPrefWidth(400); 
        HBox.setHgrow(box, Priority.ALWAYS);
        if (isSelected) {
            box.setStyle("-fx-background-color: #059669;");
            label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            box.setStyle("-fx-background-color: white;");
            label.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
        }
        item.setContent(box);
    }

    private boolean isNavegacaoKey(KeyCode code) {
        return code == KeyCode.ENTER || code == KeyCode.TAB || code == KeyCode.ESCAPE || 
               code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT;
    }

    @FXML
    public void handleAbrirLista(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ListaEncomenda.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Lista de Encomendas");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { AppLogger.error("InserirEncomendaController", e.getMessage(), e); }
    }

    private void carregarCatalogoProdutos() {
        try { this.listaMestraProdutosObjetos = itemPadraoDAO.listarTodos(true); } catch (Exception e) { AppLogger.warn("InserirEncomendaController", "Erro em InserirEncomendaController.carregarCatalogoProdutos: " + e.getMessage()); }
    }

    private void configurarValidacaoFocoClientes() {
        cmbRemetente.getEditor().focusedProperty().addListener((obs, foi, esta) -> {
            if (!esta && !isSelecionandoViaEnter && !cmbRemetente.getEditor().getText().isEmpty()) {
                 verificarEProporCadastroRapidoCliente(cmbRemetente.getEditor().getText(), "Remetente");
            }
        });
        cmbDestinatario.getEditor().focusedProperty().addListener((obs, foi, esta) -> {
            if (!esta && !isSelecionandoViaEnter && !cmbDestinatario.getEditor().getText().isEmpty()) {
                 verificarEProporCadastroRapidoCliente(cmbDestinatario.getEditor().getText(), "Destinatário");
            }
        });
    }

    private void atualizarLabelViagem() {
        if (viagemAtiva != null) {
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dataStr = viagemAtiva.getDataViagem() != null ? viagemAtiva.getDataViagem().format(dtf) : "--";
            String chegadaStr = "--";
            if (viagemAtiva.getDataChegada() != null) chegadaStr = viagemAtiva.getDataChegada().format(dtf);
            txtViagemAtual.setText(viagemAtiva.getId() + " - " + dataStr + " ate " + chegadaStr);
        } else {
            txtViagemAtual.setText("NENHUMA VIAGEM ATIVA");
            btnSalvar.setDisable(true);
        }
    }

    private void carregarComboBoxes() {
        String tR = cmbRemetente.getEditor().getText();
        String tD = cmbDestinatario.getEditor().getText();
        obsListaViagens.setAll(viagemDAO.listarTodasViagensResumido());
        obsListaRotas.setAll(rotaDAO.listarTodasAsRotasComoObjects());
        List<ClienteEncomenda> clientes = clienteEncomendaDAO.listarTodos();
        List<String> nomes = clientes.stream().map(ClienteEncomenda::getNomeCliente).collect(Collectors.toList());
        listaMestraClientes.setAll(nomes);
        cmbRemetente.setItems(listaMestraClientes);
        cmbDestinatario.setItems(listaMestraClientes);
        cmbRota.setItems(obsListaRotas);
        if(!tR.isEmpty()) cmbRemetente.getEditor().setText(tR);
        if(!tD.isEmpty()) cmbDestinatario.getEditor().setText(tD);
    }

    private void configurarListenersDeCampos() {
        txtQuantidade.textProperty().addListener((o, old, n) -> calcularValorTotalItem());
        txtValorUnit.textProperty().addListener((o, old, n) -> calcularValorTotalItem());
        
        // Configurar navegação com ENTER nos campos
        configurarNavegacaoComEnter();
    }
    
    private void configurarNavegacaoComEnter() {
        // Campo Remetente - ENTER vai para Destinatário
        cmbRemetente.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cmbDestinatario.requestFocus();
                event.consume();
            }
        });
        
        // Campo Destinatário - ENTER vai para Rota
        cmbDestinatario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cmbRota.requestFocus();
                event.consume();
            }
        });
        
        // Campo Rota - ENTER vai para Descrição do Item
        cmbRota.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                cmbDescricao.requestFocus();
                event.consume();
            }
        });
        
        // Campo Descrição - ENTER vai para Valor Unitário
        cmbDescricao.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtValorUnit.requestFocus();
                event.consume();
            }
        });
        
        // Campo Quantidade - ENTER vai para Valor Unitário
        txtQuantidade.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtValorUnit.requestFocus();
                event.consume();
            }
        });
        
        // Campo Valor Unitário - ENTER adiciona o item OU vai para Quantidade se precisar alterar
        txtValorUnit.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Se quantidade for diferente de 1, o usuário pode ter alterado, adicionar item direto
                if (!txtQuantidade.getText().isEmpty() && 
                    !cmbDescricao.getEditor().getText().isEmpty() && 
                    !txtValorUnit.getText().isEmpty()) {
                    handleAdicionarItem(null);
                } else {
                    AlertHelper.show(AlertType.WARNING, "Atenção", "Preencha todos os campos do item antes de adicionar.");
                }
                event.consume();
            } else if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                // TAB vai para quantidade para ajuste se necessário
                txtQuantidade.requestFocus();
                event.consume();
            }
        });
        
        // Campos opcionais de recebedor
        if (txtNomeRecebedor != null) {
            txtNomeRecebedor.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if (txtNDocumentoRecebedor != null) {
                        txtNDocumentoRecebedor.requestFocus();
                    }
                    event.consume();
                }
            });
        }
        
        if (txtNDocumentoRecebedor != null) {
            txtNDocumentoRecebedor.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if (txtObs != null) {
                        txtObs.requestFocus();
                    } else {
                        cmbRemetente.requestFocus();
                    }
                    event.consume();
                }
            });
        }
        
        if (txtObs != null) {
            txtObs.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    cmbRemetente.requestFocus();
                    event.consume();
                }
            });
        }
    }

    @FXML
    public void handleAdicionarItem(ActionEvent event) {
        if (!ValidationHelper.positiveInt(txtQuantidade, "Quantidade")) return;
        if (!ValidationHelper.requiredCombo(cmbDescricao, "Descrição do Item")) return;
        if (!ValidationHelper.requiredText(txtValorUnit, "Valor Unitário")) return;

        String descricao = cmbDescricao.getEditor().getText().toUpperCase().trim();
        BigDecimal valorUnitario = parseBigDecimal(txtValorUnit.getText());

        // Opcional: Sugerir cadastro de produto, mas não bloquear a adição
        boolean produtoExiste = listaMestraProdutosObjetos.stream().anyMatch(p -> p.getNomeItem().equalsIgnoreCase(descricao));

        // ATIVADO: Verificar e propor cadastro rápido de produto
        if (!produtoExiste) {
            verificarEProporCadastroRapidoProduto(descricao, valorUnitario);
        }

        EncomendaItem item = new EncomendaItem();
        // DL060: quantidade ja validada pelo positiveInt acima
        item.setQuantidade(Integer.parseInt(txtQuantidade.getText().trim()));
        item.setDescricao(descricao);
        item.setValorUnitario(valorUnitario);
        item.setValorTotal(item.getValorUnitario().multiply(new BigDecimal(item.getQuantidade())));
        obsListaItens.add(item);

        limparCamposItem();
        atualizarTotaisEncomenda();
        
        // Focar na descrição para adicionar próximo item rapidamente
        cmbDescricao.requestFocus();
    }

    private void calcularValorTotalItem() {
        try {
            int qtd = txtQuantidade.getText().isEmpty() ? 1 : Integer.parseInt(txtQuantidade.getText());
            BigDecimal valorUnit = txtValorUnit.getText().isEmpty() ? BigDecimal.ZERO : parseBigDecimal(txtValorUnit.getText());
            txtValorTotal.setText(String.format("%.2f", valorUnit.multiply(new BigDecimal(qtd))).replace(".", ","));
        } catch (NumberFormatException e) { txtValorTotal.setText("0,00"); }
    }

    // ========================================================
    // CORREÇÃO: VISUALIZAR VALORES - FONTE MAIOR (24px)
    // ========================================================
    private void atualizarTotaisEncomenda() {
        int totalVolumes = 0;
        BigDecimal totalAPagar = BigDecimal.ZERO;
        for (EncomendaItem item : obsListaItens) {
            totalVolumes += item.getQuantidade();
            totalAPagar = totalAPagar.add(item.getValorTotal());
        }
        txtTotalVol.setText(String.valueOf(totalVolumes));
        
        BigDecimal pago = (encomendaEmEdicao != null && encomendaEmEdicao.getValorPago() != null) ? encomendaEmEdicao.getValorPago() : BigDecimal.ZERO;
        BigDecimal falta = totalAPagar.subtract(pago);
        
        String texto = String.format("R$ %,.2f", totalAPagar);
        
        if (encomendaEmEdicao != null && encomendaEmEdicao.getId() != null) {
            texto += String.format(" (Pg: %,.2f | Rest: %,.2f)", pago, falta);
            
            // FONTE AUMENTADA PARA 24px E LARGURA FORÇADA
            if (falta.compareTo(BigDecimal.ZERO) > 0) {
                 txtTotalAPagar.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 24px;");
            } else {
                 txtTotalAPagar.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 24px;");
            }
            txtTotalAPagar.setPrefWidth(600); 
            txtTotalAPagar.setTooltip(new Tooltip("Total: " + totalAPagar + "\nPago: " + pago + "\nFalta: " + falta));
            
        } else {
            txtTotalAPagar.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-size: 24px;");
            txtTotalAPagar.setTooltip(null);
        }
        
        txtTotalAPagar.setText(texto);
    }

    @FXML
    public void handleSalvar(ActionEvent event) {
        if (viagemAtiva == null) { AlertHelper.show(AlertType.WARNING, "Aviso", "Não há viagem ativa."); return; }
        if (!ValidationHelper.requiredCombo(cmbDestinatario, "Destinatário")) return;
        if (!ValidationHelper.requiredList(obsListaItens, cmbDescricao, "Itens da Encomenda")) return;
        abrirTelaPagamento();
    }

    public void abrirTelaPagamento() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RegistrarPagamentoEncomenda.fxml"));
            Parent root = loader.load();
            RegistrarPagamentoEncomendaController controller = loader.getController();
            
            Encomenda enc = (encomendaEmEdicao != null) ? encomendaEmEdicao : new Encomenda();
            if (encomendaEmEdicao == null) {
                enc.setIdViagem(viagemAtiva.getId());
                enc.setNumeroEncomenda(txtNumeroEncomenda.getText());
            }
            enc.setRemetente(cmbRemetente.getEditor().getText().toUpperCase());
            enc.setDestinatario(cmbDestinatario.getEditor().getText().toUpperCase());
            if (txtObs != null) enc.setObservacoes(txtObs.getText());
            enc.setTotalVolumes(Integer.parseInt(txtTotalVol.getText()));
            
            BigDecimal valorReal = parseBigDecimal(txtTotalAPagar.getText());
            if (valorReal.compareTo(BigDecimal.ZERO) == 0 && !obsListaItens.isEmpty()) {
                BigDecimal soma = BigDecimal.ZERO;
                for(EncomendaItem i : obsListaItens) soma = soma.add(i.getValorTotal());
                valorReal = soma;
            }
            enc.setTotalAPagar(valorReal);

            if(cmbRota.getValue() != null) enc.setNomeRota(cmbRota.getValue().toString());

            controller.setDados(enc, obsListaItens, this);

            Stage stage = new Stage();
            stage.setTitle("Pagamento da Encomenda");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.showAndWait();
        } catch (IOException e) { AppLogger.error("InserirEncomendaController", e.getMessage(), e); }
    }
    
    public void finalizarSalvamento(Encomenda encomendaFinal) {
        if (encomendaFinal != null && encomendaFinal.getId() != null) {
            // Se o usuário já preencheu os campos de recebedor na tela principal,
            // garantir que sejam copiados para o objeto que veio do fluxo de pagamento
            if (txtNomeRecebedor != null) {
                String n = txtNomeRecebedor.getText() != null ? txtNomeRecebedor.getText().trim() : "";
                if (!n.isEmpty()) encomendaFinal.setNomeRecebedor(n.toUpperCase());
            }
            if (txtNDocumentoRecebedor != null) {
                String d = txtNDocumentoRecebedor.getText() != null ? txtNDocumentoRecebedor.getText().trim() : "";
                if (!d.isEmpty()) encomendaFinal.setDocRecebedor(d.toUpperCase());
            }

            // Mostrar mensagem de sucesso
            AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Encomenda #" + encomendaFinal.getNumeroEncomenda() + " salva com sucesso!");
            
            // LIMPAR CAMPOS AUTOMATICAMENTE para facilitar lançamento rápido
            Platform.runLater(() -> {
                limparFormularioCompleto();
                // Focar no primeiro campo para continuar lançando
                cmbRemetente.requestFocus();
            });
            
        } else {
            handleIniciar(null); 
        }
    }
    
    private void recarregarDadosFinanceiros(Long idEncomenda) {
        if (idEncomenda == null) return;
        Encomenda doBanco = encomendaDAO.buscarPorId(idEncomenda);
        if (doBanco != null) {
            this.encomendaEmEdicao = doBanco;
            atualizarTotaisEncomenda();
            verificarStatusBotoesEntrega();
        }
    }
    
    private void atualizarNumeroEncomenda() {
        Rota rota = cmbRota.getValue();
        if (rota != null && viagemAtiva != null && encomendaDAO != null) {
            int proximoNumero = encomendaDAO.obterProximoNumero(viagemAtiva.getId(), rota.toString());
            txtNumeroEncomenda.setText(String.valueOf(proximoNumero));
        } else { txtNumeroEncomenda.setText("1"); }
    }
    
    private void controlarEstadoDosCampos(boolean permitirEdicao) {
        boolean bloquear = !permitirEdicao;
        cmbRemetente.setDisable(bloquear);
        cmbDestinatario.setDisable(bloquear);
        cmbRota.setDisable(bloquear);
        txtNumeroEncomenda.setDisable(bloquear);
        if (txtObs != null) txtObs.setDisable(bloquear);
        txtQuantidade.setDisable(bloquear);
        cmbDescricao.setDisable(bloquear);
        txtValorUnit.setDisable(bloquear);
        btnAdicionarItem.setDisable(bloquear);
        btnSalvar.setDisable(bloquear);
        btnEditar.setDisable(!bloquear);
        
        // Controlar edição da tabela e menu de contexto
        tableItens.setEditable(permitirEdicao);
        if (tableItens.getContextMenu() != null) {
            tableItens.getContextMenu().getItems().forEach(item -> item.setDisable(bloquear));
        }
        
        if (txtNDocumentoRecebedor != null) txtNDocumentoRecebedor.setEditable(permitirEdicao);
        if (txtNomeRecebedor != null) txtNomeRecebedor.setEditable(permitirEdicao);
        
        if(btnAudioInput != null) btnAudioInput.setDisable(bloquear);
        if(btnImageInput != null) btnImageInput.setDisable(bloquear);
    }
    
    private void verificarStatusBotoesEntrega() {
        if (encomendaEmEdicao == null) return;
        BigDecimal total = encomendaEmEdicao.getTotalAPagar();
        BigDecimal pago = (encomendaEmEdicao.getValorPago() != null) ? encomendaEmEdicao.getValorPago() : BigDecimal.ZERO;
        boolean entregue = encomendaEmEdicao.isEntregue();
        boolean estaQuitado = pago.compareTo(total) >= 0;

        if (entregue) {
            if (!estaQuitado) {
                btnEntregar.setDisable(false);
                btnEntregar.setText("Receber Restante");
                estilizarBotao(btnEntregar, "#F59E0B"); 
            } else {
                btnEntregar.setDisable(true);
                btnEntregar.setText("Concluído");
                btnEntregar.setStyle("");
            }
        } else {
            btnEntregar.setDisable(false);
            btnEntregar.setText("Entregar");
            estilizarBotao(btnEntregar, "#0369A1");
        }
    }
    
    // =============================================================
    // AQUI ESTÁ A LÓGICA QUE RESOLVE O PROBLEMA DO NOME
    // =============================================================
    public void setEncomendaParaEdicao(Encomenda encomenda) {
        if (encomenda != null && encomenda.getId() != null) {
            this.encomendaDAO = new EncomendaDAO();
            this.encomendaEmEdicao = encomendaDAO.buscarPorId(encomenda.getId());
        } else {
            this.encomendaEmEdicao = encomenda;
        }
        
        if (this.encomendaEmEdicao == null) return;

        txtNumeroEncomenda.setText(this.encomendaEmEdicao.getNumeroEncomenda());
        cmbRemetente.setValue(this.encomendaEmEdicao.getRemetente());
        cmbDestinatario.setValue(this.encomendaEmEdicao.getDestinatario());
        if (this.encomendaEmEdicao.getObservacoes() != null && txtObs != null) txtObs.setText(this.encomendaEmEdicao.getObservacoes());
        
        // Atualiza o nome do recebedor corretamente
        String nomeBanco = this.encomendaEmEdicao.getNomeRecebedor();
        if (nomeBanco != null && !nomeBanco.trim().isEmpty()) {
            txtNomeRecebedor.setText(nomeBanco);
        } else {
            txtNomeRecebedor.setText("Pendente de Entrega");
        }

        if (txtNDocumentoRecebedor != null) {
            txtNDocumentoRecebedor.setText(this.encomendaEmEdicao.getDocRecebedor() != null ? this.encomendaEmEdicao.getDocRecebedor() : "");
        }

        if (this.encomendaEmEdicao.getNomeRota() != null) {
            for (Rota r : obsListaRotas) {
                if (r.toString().equalsIgnoreCase(this.encomendaEmEdicao.getNomeRota())) {
                    cmbRota.setValue(r);
                    break;
                }
            }
        }
        
        if (encomendaItemDAO != null) {
            List<EncomendaItem> itensBanco = encomendaItemDAO.listarPorIdEncomenda(this.encomendaEmEdicao.getId());
            obsListaItens.setAll(itensBanco);
        }
        
        // Atualiza o status corretamente
        String statusEntrega = (nomeBanco != null && !nomeBanco.trim().isEmpty()) ? "Encomenda já entregue" : "Pendente de Entrega";
        txtStatusEntrega.setText(statusEntrega);
        
        atualizarTotaisEncomenda(); 
        btnSalvar.setText("F3 - Atualizar");
        btnImprimir.setDisable(false);
        btnExcluir.setDisable(false);
        btnEntregar.setDisable(false);
        controlarEstadoDosCampos(false);
        verificarStatusBotoesEntrega();
        
        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setResizable(true);
                stage.setMaximized(true);
                stage.setMinWidth(1100);
                stage.setMinHeight(700);
            }
        });
    }
    
    @FXML
    public void handleEditar(ActionEvent event) {
        if (encomendaEmEdicao != null) {
            controlarEstadoDosCampos(true);
            btnSalvar.setText("F3 - Salvar Alterações");
            cmbRemetente.requestFocus();
        }
    }

    private void verificarEProporCadastroRapidoCliente(String nome, String tipo) {
        if (listaMestraClientes.stream().anyMatch(c -> c.equalsIgnoreCase(nome))) return;
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Novo Cliente");
            alert.setHeaderText(tipo + " não cadastrado: " + nome);
            alert.setContentText("Deseja cadastrar automaticamente?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    ClienteEncomenda novo = new ClienteEncomenda();
                    novo.setNomeCliente(nome.toUpperCase());
                    clienteEncomendaDAO.salvar(novo);
                    carregarComboBoxes();
                    if(tipo.equals("Remetente")) cmbRemetente.setValue(nome.toUpperCase());
                    else cmbDestinatario.setValue(nome.toUpperCase());
                } catch (Exception e) { AppLogger.warn("InserirEncomendaController", "Erro em InserirEncomendaController.verificarEProporCadastroRapidoCliente: " + e.getMessage()); }
            }
        });
    }

    private boolean verificarEProporCadastroRapidoProduto(String descricao, BigDecimal valorSugerido) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Novo Item");
        alert.setHeaderText("Item '" + descricao + "' não está cadastrado");
        alert.setContentText("Deseja salvar '" + descricao + "' (R$ " + valorSugerido + ") como novo item padrão?\n\nPressione ENTER para Sim ou ESC para Não.");
        
        // Configurar botões personalizados para melhor usabilidade
        ButtonType simButton = new ButtonType("Sim (ENTER)", ButtonData.OK_DONE);
        ButtonType naoButton = new ButtonType("Não (ESC)", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(simButton, naoButton);
        
        // Permitir navegação por teclado
        alert.getDialogPane().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                alert.setResult(simButton);
                keyEvent.consume();
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                alert.setResult(naoButton);
                keyEvent.consume();
            }
        });
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == simButton) {
            try {
                ItemEncomendaPadrao novo = new ItemEncomendaPadrao();
                novo.setNomeItem(descricao.toUpperCase());
                novo.setPrecoUnit(valorSugerido);
                
                // Salvar o novo item
                itemPadraoDAO.salvar(novo);
                
                // Recarregar catálogo para incluir o novo item
                carregarCatalogoProdutos();
                
                // Mostrar confirmação rápida
                Platform.runLater(() -> {
                    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Item '" + descricao + "' cadastrado com sucesso!");
                });
                
                return true;
            } catch (Exception e) {
                AlertHelper.show(AlertType.ERROR, "Erro", "Erro ao cadastrar item: " + e.getMessage());
                AppLogger.error("InserirEncomendaController", e.getMessage(), e);
                return false;
            }
        }
        return false;
    }

    private void limparCamposItem() {
        txtQuantidade.setText("1");
        cmbDescricao.getEditor().clear();
        cmbDescricao.getSelectionModel().clearSelection();
        txtValorUnit.clear();
        txtValorTotal.clear();
    }

    private void limparFormularioCompleto() {
        encomendaEmEdicao = null;
        btnSalvar.setText("F3 - FINALIZAR");
        btnSalvar.setDisable(false);
        btnImprimir.setDisable(true);
        btnExcluir.setDisable(true);
        btnEntregar.setDisable(true);
        btnEntregar.setText("Entregar");
        btnEntregar.setStyle("");
        controlarEstadoDosCampos(true);
        if (encomendaDAO != null && cmbRota.getValue() != null && viagemAtiva != null) atualizarNumeroEncomenda();
        else if (encomendaDAO != null) txtNumeroEncomenda.setText("1");
        cmbRemetente.getEditor().clear();
        cmbDestinatario.getEditor().clear();
        if (txtObs != null) txtObs.clear();
        obsListaItens.clear();
        txtTotalVol.setText("0");
        txtTotalAPagar.setText("0,00");
        txtTotalAPagar.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-size: 24px;");
        
        // Definir quantidade padrão como "1" para agilizar lançamento
        txtQuantidade.setText("1");
        
        cmbRemetente.requestFocus();
        if (txtNDocumentoRecebedor != null) txtNDocumentoRecebedor.clear();
        if (txtNomeRecebedor != null) txtNomeRecebedor.setText("Pendente de Entrega");
        aplicarEstiloBotoes();
    }

    @FXML public void handleIniciar(ActionEvent event) { limparFormularioCompleto(); }
    @FXML public void handleSair(ActionEvent event) { 
        TelaPrincipalController.fecharTelaAtual(rootPane);
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        if (text.contains("(")) {
            text = text.substring(0, text.indexOf("("));
        }
        try { return new BigDecimal(text.replace("R$", "").replace(".", "").replace(",", ".").trim()); } 
        catch (Exception e) { return BigDecimal.ZERO; }
    }


    @FXML
    public void handleImprimir(ActionEvent event) {
        if (encomendaEmEdicao != null) imprimirCupomTermico(encomendaEmEdicao);
    }
    
    @FXML public void handleExcluir(ActionEvent event) {
        if (encomendaEmEdicao == null) return;
        if (encomendaDAO.excluir(encomendaEmEdicao.getId())) {
            AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Excluída.");
            handleSair(event);
        }
    }
}