package gui;

import dao.ItemFreteDAO;
import dao.ConexaoBD;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import model.ItemFrete;

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

/**
 * Controller da tela CadastroFrete.fxml.
 * Implementa:
 * - Carregamento inicial de combo boxes
 * - Edição de frete (quando staticNumeroFreteParaAbrir != null)
 * - Inserção/atualização de frete e itens
 */
public class CadastroFreteController implements Initializable {

    // =====================================================
    // 1) VARIÁVEL ESTÁTICA PARA RECEBER O NÚMERO DO FRETE
    //    QUE SERÁ ABERTO PELA LISTA (EDIÇÃO)
    // =====================================================
    private static String staticNumeroFreteParaAbrir = null;

    /**
     * Chamado por ListaFretesController antes de abrir esta tela,
     * para indicar qual número de frete deve ser carregado para edição.
     */
    public static void setNumeroFreteParaAbrir(String numFrete) {
        staticNumeroFreteParaAbrir = numFrete;
    }

    //<editor-fold desc="FXML Injections">
    @FXML private ComboBox<String> cbRemetente;
    @FXML private ComboBox<String> cbRota;
    @FXML private ComboBox<String> cbConferente;
    @FXML private ComboBox<String> cbCliente;
    @FXML private ComboBox<String> cbCidadeDeCobranca;
    // CORREÇÃO: ComboBox de item agora será ComboBox<ItemFrete> para exibir mais dados
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
    @FXML private Button btnSalvar; // Este botão será Salvar (para novo) ou Confirmar (para alteração)
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
    private ObservableList<String> listaCidadesOriginal = FXCollections.observableArrayList();
    // CORREÇÃO: listaItensDisplayOriginal agora conterá objetos ItemFrete
    private ObservableList<ItemFrete> listaItensDisplayOriginal = FXCollections.observableArrayList();

    private ObservableList<FreteItem> listaTabelaItensFrete = FXCollections.observableArrayList();
    private Map<String, ItemFrete> mapItensCadastrados = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("'R$ '#,##0.00",
            new DecimalFormatSymbols(new Locale("pt","BR")));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private boolean programmaticamenteAtualizando = false;
    private boolean processandoItemCbItem = false;
    private boolean processandoContatoRemetente= false;
    private boolean processandoContatoCliente = false;
    private String ultimoItemProcessadoCbItem = null;

    private long freteAtualId = -1; // -1 indica que é um novo frete, não um existente

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("CadastroFreteController: Iniciando initialize()...");

        setComponentProperties();
        configurarComboBoxItem(); // Nova configuração para cbitem
        carregarDadosIniciaisComboBoxes();
        setComboBoxItems();
        configurarTabela();
        configurarListenersDeCamposEEventos();

        System.out.println("CadastroFreteController: initialize() pré-checagem de edição concluído.");

        if (staticNumeroFreteParaAbrir != null) {
            System.out.println("CadastroFreteController: Detected request to open Frete para edição: " + staticNumeroFreteParaAbrir);
            carregarFreteParaEdicao(staticNumeroFreteParaAbrir);
            staticNumeroFreteParaAbrir = null;
        } else {
            limparCamposFrete();
            habilitarCamposParaVisualizacao(false);
            if (btnNovo != null) btnNovo.setDisable(false);
            if (btnSalvar != null) btnSalvar.setDisable(true);
            if (btnAlterar != null) btnAlterar.setDisable(true);
            if (btnExcluir != null) btnExcluir.setDisable(true);
        }

        System.out.println("CadastroFreteController: initialize() concluído.");
    }

    // NOVO MÉTODO: Configurações específicas para cbitem
    private void configurarComboBoxItem() {
        if (cbitem == null) {
            System.err.println("cbitem é NULL em configurarComboBoxItem.");
            return;
        }
        cbitem.setEditable(true);

        // Personaliza como cada ItemFrete é exibido na lista suspensa
        cbitem.setCellFactory(lv -> new ListCell<ItemFrete>() {
            @Override
            protected void updateItem(ItemFrete item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNomeItem() + " (R$ " + df.format(item.getPrecoUnitarioPadrao()) + " / R$ " + df.format(item.getPrecoUnitarioDesconto()) + ")");
                }
            }
        });

        // Personaliza como o ItemFrete selecionado é exibido no TextField do ComboBox
        cbitem.setConverter(new StringConverter<ItemFrete>() {
            @Override
            public String toString(ItemFrete item) {
                if (item == null) {
                    return null;
                }
                // Quando o item é selecionado, exibe apenas o nome no TextField
                return item.getNomeItem();
            }

            @Override
            public ItemFrete fromString(String string) {
                // Ao digitar, tenta encontrar um ItemFrete correspondente
                // Esta lógica já é tratada em processarItemDigitadoOuSelecionado,
                // então aqui podemos apenas retornar null ou um ItemFrete básico
                // que será refinado pelo listener do editor.
                String normalizedString = string.toLowerCase();
                for (ItemFrete item : listaItensDisplayOriginal) {
                    if (item.getNomeItem().toLowerCase().equals(normalizedString)) {
                        return item;
                    }
                }
                // Se não encontrou, retorna um ItemFrete "fantasma" que será tratado como novo
                // ou apenas a string para o editor. O ideal é deixar o listener do editor tratar.
                return null; 
            }
        });

        // Listener para o texto digitado no editor do ComboBox (para filtragem)
        cbitem.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (programmaticamenteAtualizando) return;

            if (newV == null || newV.isEmpty()) {
                cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
                cbitem.hide();
            } else {
                ObservableList<ItemFrete> filteredList = listaItensDisplayOriginal.filtered(
                    item -> item != null && item.getNomeItem().toLowerCase().contains(newV.toLowerCase())
                );
                cbitem.setItems(filteredList);
                if (!filteredList.isEmpty() && cbitem.getEditor().isFocused() && !cbitem.isShowing()) {
                    cbitem.show();
                } else if (filteredList.isEmpty() && cbitem.isShowing()) {
                    cbitem.hide();
                }
            }
        });

        // Listener para quando um item é selecionado ou quando ENTER é pressionado no editor
        cbitem.setOnAction(e -> {
            if (programmaticamenteAtualizando) return;
            ItemFrete selectedItem = cbitem.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                processarItemDigitadoOuSelecionado(selectedItem.getNomeItem()); // Usa o nome do item selecionado
                // Move o foco para txtpreco, se aplicável
                if (txtpreco != null && txtpreco.isFocusTraversable()) {
                    txtpreco.requestFocus();
                }
            } else {
                // Se o usuário digitou e não selecionou, trata como texto
                processarItemDigitadoOuSelecionado(cbitem.getEditor().getText());
            }
        });

        // Se o foco sair do ComboBox do item, processa o que foi digitado
        cbitem.focusedProperty().addListener((obs, oldV, newV) -> {
            if (programmaticamenteAtualizando) return;
            if (!newV) { // Perdeu o foco
                String text = cbitem.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    // Tenta encontrar um item existente que corresponda ao texto digitado
                    ItemFrete matchedItem = mapItensCadastrados.get(text.trim().toLowerCase());
                    if (matchedItem != null) {
                        programmaticamenteAtualizando = true;
                        try {
                            cbitem.setValue(matchedItem); // Define o item como o valor selecionado
                        } finally {
                            programmaticamenteAtualizando = false;
                        }
                        processarItemDigitadoOuSelecionado(matchedItem.getNomeItem()); // Processa o item encontrado
                    } else {
                        // Se não encontrou, trata como um novo item/texto livre
                        processarItemDigitadoOuSelecionado(text.trim());
                    }
                }
            } else { // Ganhou foco
                cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
            }
        });
    }

    private void carregarFreteParaEdicao(String numFrete) {
        System.out.println("carregarFreteParaEdicao: carregando frete " + numFrete + " do banco...");

        long numeroFreteLong;
        try {
            numeroFreteLong = Long.parseLong(numFrete);
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Erro de Dados", "O número do frete '" + numFrete + "' é inválido.");
            return;
        }

        try (Connection conn = ConexaoBD.getConnection()) {
            String sqlFrete = "SELECT * FROM fretes WHERE numero_frete = ?";
            try (PreparedStatement pst = conn.prepareStatement(sqlFrete)) {
                pst.setLong(1, numeroFreteLong);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    programmaticamenteAtualizando = true;
                    try {
                        freteAtualId = rs.getLong("id_frete");
                        String remetenteNome = rs.getString("remetente_nome_temp");
                        String destinatarioNome = rs.getString("destinatario_nome_temp");
                        String rotaTemp = rs.getString("rota_temp");
                        java.sql.Date dataSaidaDb = rs.getDate("data_saida_viagem");
                        String localTransporte = rs.getString("local_transporte");
                        String conferenteTemp = rs.getString("conferente_temp");
                        String cidadeCobrancaTemp = rs.getString("cidade_cobranca");
                        String obsTexto = rs.getString("observacoes");
                        String numNotaFiscalTemp = rs.getString("num_notafiscal");
                        BigDecimal valorNotaDb = rs.getBigDecimal("valor_notafiscal");
                        BigDecimal pesoNotaDb = rs.getBigDecimal("peso_notafiscal");
                        BigDecimal valorTotalItens = rs.getBigDecimal("valor_total_itens");
                        BigDecimal valorFreteCalculado = rs.getBigDecimal("valor_frete_calculado");

                        if (txtNumFrete != null)        txtNumFrete.setText(numFrete);
                        if (cbRemetente != null)        cbRemetente.setValue(remetenteNome);
                        if (cbCliente != null)          cbCliente.setValue(destinatarioNome);
                        if (cbRota != null)             cbRota.setValue(rotaTemp);
                        if (txtSaida != null && dataSaidaDb != null) {
                            txtSaida.setText(dataSaidaDb.toLocalDate().format(dateFormatter));
                        } else if (txtSaida != null) {
                            txtSaida.clear();
                        }
                        if (txtLocalTransporte != null) txtLocalTransporte.setText(localTransporte);
                        if (cbConferente != null)       cbConferente.setValue(conferenteTemp);
                        if (cbCidadeDeCobranca != null) cbCidadeDeCobranca.setValue(cidadeCobrancaTemp);
                        if (txtObs != null)             txtObs.setText(obsTexto);

                        if (numNotaFiscalTemp != null && !numNotaFiscalTemp.isEmpty()) {
                            if (rbSim != null) rbSim.setSelected(true);
                            if (txtNumNota != null)    txtNumNota.setText(numNotaFiscalTemp);
                            if (txtValorNota != null && valorNotaDb != null) {
                                txtValorNota.setText(df.format(valorNotaDb.doubleValue()));
                            } else if (txtValorNota != null) {
                                txtValorNota.clear();
                            }
                            if (txtPesoNota != null && pesoNotaDb != null) {
                                txtPesoNota.setText(pesoNotaDb.toString());
                            } else if (txtPesoNota != null) {
                                txtPesoNota.clear();
                            }
                        } else {
                            if (Rbnao != null) Rbnao.setSelected(true);
                        }
                        if (txtValorTotalNota != null && valorFreteCalculado != null) {
                            txtValorTotalNota.setText(df.format(valorFreteCalculado.doubleValue()));
                        } else if (txtValorTotalNota != null) {
                            txtValorTotalNota.clear();
                        }
                    } finally {
                        programmaticamenteAtualizando = false;
                    }
                } else {
                    showAlert(AlertType.WARNING, "Aviso",
                            "Nenhum frete encontrado com número: " + numFrete);
                    freteAtualId = -1;
                    limparCamposFrete();
                    habilitarCamposParaVisualizacao(false);
                    if (btnNovo != null) btnNovo.setDisable(false);
                    if (btnSalvar != null) btnSalvar.setDisable(true);
                    if (btnAlterar != null) btnAlterar.setDisable(true);
                    if (btnExcluir != null) btnExcluir.setDisable(true);
                    return;
                }
            }

            String sqlItens = "SELECT nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item " +
                    "FROM frete_itens WHERE id_frete = ?";
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

            habilitarCamposParaVisualizacao(true);
            if (btnNovo != null)    btnNovo.setDisable(false);
            if (btnSalvar != null)  btnSalvar.setDisable(true);
            if (btnAlterar != null) btnAlterar.setDisable(false);
            if (btnExcluir != null) btnExcluir.setDisable(false);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Carregar Frete",
                    "Não foi possível carregar o frete para edição:\n" + e.getMessage());
            freteAtualId = -1;
            limparCamposFrete();
            habilitarCamposParaVisualizacao(false);
            if (btnNovo != null) btnNovo.setDisable(false);
            if (btnSalvar != null) btnSalvar.setDisable(true);
            if (btnAlterar != null) btnAlterar.setDisable(true);
            if (btnExcluir != null) btnExcluir.setDisable(true);
        }
    }


    private void setComponentProperties() {
        if (cbRemetente != null)   cbRemetente.setEditable(true);
        if (cbCliente != null)     cbCliente.setEditable(true);
        // cbitem agora é configurado em configurarComboBoxItem()
        if (cbRota != null)        cbRota.setEditable(false);
        if (cbConferente != null)  cbConferente.setEditable(false);
        if (cbCidadeDeCobranca != null) cbCidadeDeCobranca.setEditable(false);

        if (txtNumFrete != null) {
            txtNumFrete.setEditable(false);
            txtNumFrete.setText("Automático");
        }
        if (txtSaida != null)       txtSaida.setPromptText("dd/MM/yyyy");
        if (txttotal != null) {
            txttotal.setEditable(false);
            txttotal.setFocusTraversable(false);
        }
        if (txtTotalVol != null)      txtTotalVol.setEditable(false);
        if (txtValorTotalNota != null) txtValorTotalNota.setEditable(false);
        if (txtViagemAtual != null)   txtViagemAtual.setEditable(false);
    }

    private void carregarDadosIniciaisComboBoxes() {
        System.out.println("CadastroFreteController: Iniciando carregarDadosIniciaisComboBoxes()...");
        carregarContatosParaComboBoxes("Remetente", listaRemetentesOriginal);
        carregarContatosParaComboBoxes("Cliente", listaClientesOriginal);
        carregarRotas();
        carregarConferentesDoBanco();
        carregarCidadesDoBanco();
        carregarItensCadastradosParaComboBox(); // Este método precisa ser atualizado
        System.out.println("CadastroFreteController: carregarDadosIniciaisComboBoxes() concluído.");
    }

    private void setComboBoxItems() {
        System.out.println("CadastroFreteController: Iniciando setComboBoxItems()...");
        programmaticamenteAtualizando = true;
        try {
            if (cbRemetente != null && listaRemetentesOriginal != null)
                cbRemetente.setItems(FXCollections.observableArrayList(listaRemetentesOriginal));
            if (cbCliente != null && listaClientesOriginal != null)
                cbCliente.setItems(FXCollections.observableArrayList(listaClientesOriginal));
            if (cbRota != null && listaRotasOriginal != null)
                cbRota.setItems(FXCollections.observableArrayList(listaRotasOriginal));
            if (cbConferente != null && listaConferentesOriginal != null)
                cbConferente.setItems(FXCollections.observableArrayList(listaConferentesOriginal));
            if (cbCidadeDeCobranca != null && listaCidadesOriginal != null)
                cbCidadeDeCobranca.setItems(FXCollections.observableArrayList(listaCidadesOriginal));
            // CORREÇÃO: cbitem agora é do tipo ItemFrete e usa listaItensDisplayOriginal (que agora é ItemFrete)
            if (cbitem != null && listaItensDisplayOriginal != null)
                cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
        } finally {
            programmaticamenteAtualizando = false;
        }
        System.out.println("CadastroFreteController: setComboBoxItems() concluído.");
    }

    public static class FreteItem {
        private final SimpleIntegerProperty quantidade;
        private final SimpleStringProperty  item;
        private final SimpleDoubleProperty preco;

        public FreteItem(int qtd, String it, double pr) {
            this.quantidade = new SimpleIntegerProperty(qtd);
            this.item       = new SimpleStringProperty(it);
            this.preco      = new SimpleDoubleProperty(pr);
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
        if (tabelaItens == null) {
            System.err.println("Erro Crítico: tabelaItens é null em configurarTabela.");
            return;
        }
        tabelaItens.setEditable(true);
        if (listaTabelaItensFrete != null) {
            tabelaItens.setItems(listaTabelaItensFrete);
        } else {
            System.err.println("listaTabelaItensFrete é NULL em configurarTabela");
        }

        if (colQuantidade != null) {
            colQuantidade.setCellValueFactory(cd -> cd.getValue().quantidadeProperty().asObject());
            colQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            colQuantidade.setOnEditCommit(e -> {
                FreteItem it = e.getRowValue();
                Integer nV   = e.getNewValue();
                Integer oV   = e.getOldValue();
                if (nV != null && nV > 0) {
                    it.setQuantidade(nV);
                } else {
                    it.setQuantidade(oV != null ? oV : 1);
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        } else System.err.println("colQuantidade é NULL em configurarTabela");

        if (colItem != null) {
            colItem.setCellValueFactory(cd -> cd.getValue().itemProperty());
            colItem.setCellFactory(TextFieldTableCell.forTableColumn());
            colItem.setOnEditCommit(e -> {
                FreteItem it = e.getRowValue();
                String nV    = e.getNewValue();
                if (nV != null && !nV.trim().isEmpty()) {
                    it.setItem(nV.trim());
                } else {
                    it.setItem(e.getOldValue());
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        } else System.err.println("colItem é NULL em configurarTabela");

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
                        showAlert(AlertType.ERROR, "Valor Inválido",
                                "Preço digitado na tabela ('" + string + "') é inválido.");
                        return null;
                    }
                }
            }));
            colPreco.setOnEditCommit(event -> {
                FreteItem itemEditado = event.getRowValue();
                Double novoValor      = event.getNewValue();
                Double valorAntigo    = event.getOldValue();
                if (novoValor != null && novoValor >= 0) {
                    itemEditado.setPreco(novoValor);
                } else {
                    itemEditado.setPreco(valorAntigo != null ? valorAntigo : 0.0);
                }
                tabelaItens.refresh();
                atualizarTotaisAgregados();
            });
        } else System.err.println("colPreco é NULL em configurarTabela");

        if (colTotal != null) {
            colTotal.setCellValueFactory(cd ->
                    new SimpleStringProperty(df.format(cd.getValue().getTotal()))
            );
        } else System.err.println("colTotal é NULL em configurarTabela");
    }

    private void configurarListenersDeCamposEEventos() {
        System.out.println("CadastroFreteController: Iniciando configurarListenersDeCamposEEventos()...");

        if (txtquantidade != null) {
            txtquantidade.textProperty().addListener((o, old, n) -> calcularTotalItemEmTempoReal());
        } else System.err.println("txtquantidade é NULL");

        if (txtpreco != null) {
            txtpreco.textProperty().addListener((o, old, n) -> calcularTotalItemEmTempoReal());
            txtpreco.focusedProperty().addListener((o, oldV, newV) -> {
                if (!newV && txtpreco.getText() != null && !txtpreco.getText().isEmpty()) {
                    try {
                        double v = parseValorMonetario(txtpreco.getText());
                        txtpreco.setText(df.format(v));
                    } catch (ParseException e) {
                        // mantém como está se for inválido
                    }
                }
            });
        } else System.err.println("txtpreco é NULL");

        if (notaFiscalToggleGroup != null) {
            notaFiscalToggleGroup.selectedToggleProperty().addListener((o, old, n) -> {
                if (programmaticamenteAtualizando) return;
                boolean isFormEditable = (btnSalvar != null && !btnSalvar.isDisabled()) || (btnAlterar != null && !btnAlterar.isDisabled());
                habilitarCamposDaNotaFiscal(isFormEditable);
            });
        } else System.err.println("notaFiscalToggleGroup é NULL");
        
        if (precoToggleGroup != null) {
            precoToggleGroup.selectedToggleProperty().addListener((o, old, n) -> {
                if (programmaticamenteAtualizando) return;
                // CORREÇÃO: Usar cbitem.getValue() para obter o ItemFrete selecionado
                if (cbitem != null && cbitem.getValue() != null) {
                    processarItemDigitadoOuSelecionado(cbitem.getValue().getNomeItem());
                } else {
                    if (txtpreco != null) txtpreco.clear();
                    if (txttotal != null) txttotal.clear();
                }
            });
        } else System.err.println("precoToggleGroup é NULL");

        // CORREÇÃO: cbitem agora é do tipo ItemFrete, então configureDynamicComboBox precisa ser ajustado
        // ou criar um novo método para ele. Já temos configurarComboBoxItem() para isso.
        // As chamadas para cbRemetente e cbCliente permanecem as mesmas.
        configureDynamicComboBox(cbRemetente, listaRemetentesOriginal,
                (nome) -> processarContatoDigitado(cbRemetente, nome, listaRemetentesOriginal, "Remetente"), cbCliente);
        configureDynamicComboBox(cbCliente, listaClientesOriginal,
                (nome) -> processarContatoDigitado(cbCliente, nome, listaClientesOriginal, "Cliente"), cbRota);

        if (listaTabelaItensFrete != null) {
            listaTabelaItensFrete.addListener((ListChangeListener<FreteItem>) c -> {
                if (programmaticamenteAtualizando) return;
                atualizarTotaisAgregados();
                boolean itensPresentes = !listaTabelaItensFrete.isEmpty();
                boolean podeImprimir = freteAtualId != -1 || (btnSalvar != null && !btnSalvar.isDisabled());

                if (BtnImprimirNota != null)     BtnImprimirNota.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!(podeImprimir && itensPresentes));
                if (btnImprimirRecibo != null)   btnImprimirRecibo.setDisable(!(podeImprimir && itensPresentes));
            });
        } else System.err.println("listaTabelaItensFrete é NULL");

        if (cbRemetente != null && cbRemetente.getEditor() != null
                && cbCliente != null && cbCliente.getEditor() != null) {
            setEnterNavigation(cbRemetente.getEditor(), cbCliente.getEditor());
        }
        if (cbCliente != null && cbCliente.getEditor() != null && cbRota != null) {
            setEnterNavigation(cbCliente.getEditor(), cbRota);
        }
        if (cbRota != null && txtSaida != null) {
            setEnterNavigation(cbRota, txtSaida);
        }
        if (txtSaida != null && txtLocalTransporte != null) {
            setEnterNavigation(txtSaida, txtLocalTransporte);
        }
        if (txtLocalTransporte != null && cbConferente != null) {
            setEnterNavigation(txtLocalTransporte, cbConferente);
        }
        if (cbConferente != null && cbCidadeDeCobranca != null) {
            setEnterNavigation(cbConferente, cbCidadeDeCobranca);
        }
        if (cbCidadeDeCobranca != null && rbSim != null) {
            setEnterNavigation(cbCidadeDeCobranca, rbSim);
        }

        if (rbSim != null && txtNumNota != null && txtObs != null) {
            rbSim.setOnKeyPressed(createEnterKeyHandlerForRadioButton(txtNumNota, txtObs, true));
        }
        if (Rbnao != null && txtNumNota != null && txtObs != null) {
            Rbnao.setOnKeyPressed(createEnterKeyHandlerForRadioButton(txtNumNota, txtObs, false));
        }

        if (txtNumNota != null && txtValorNota != null) {
            setEnterNavigation(txtNumNota, txtValorNota);
        }
        if (txtValorNota != null && txtPesoNota != null) {
            setEnterNavigation(txtValorNota, txtPesoNota);
        }
        if (txtPesoNota != null && txtObs != null) {
            setEnterNavigation(txtPesoNota, txtObs);
        }
        if (txtObs != null && txtquantidade != null) {
            setEnterNavigation(txtObs, txtquantidade);
        }
        // CORREÇÃO: Navegação do txtquantidade para o editor do cbitem
        if (txtquantidade != null && cbitem != null && cbitem.getEditor() != null) {
            setEnterNavigation(txtquantidade, cbitem.getEditor());
        }

        // CORREÇÃO: Editor do cbitem agora deve chamar processarItemDigitadoOuSelecionado
        if (cbitem != null && cbitem.getEditor() != null) {
            cbitem.getEditor().setOnAction(e -> {
                if (cbitem.getEditor() != null) {
                    String itemTexto = cbitem.getEditor().getText();
                    if (itemTexto != null && !itemTexto.trim().isEmpty()) {
                        processarItemDigitadoOuSelecionado(itemTexto.trim());
                    } else {
                        if (txtpreco != null && txtpreco.isFocusTraversable()) {
                            txtpreco.requestFocus();
                        }
                    }
                }
            });
        }


        if (txtpreco != null && btnInserir != null) {
            txtpreco.setOnAction(e -> {
                if (btnInserir != null && !btnInserir.isDisable()) {
                    btnInserir.fire();
                    if (txtquantidade != null && txtquantidade.isFocusTraversable()) {
                        txtquantidade.requestFocus();
                    }
                }
            });
        }

        // DESABILITANDO OS BOTÕES DE IA POR ENQUANTO
        if (btnFotoNota != null) {
            btnFotoNota.setDisable(true);
            btnFotoNota.setOnAction(null);
        }
        if (btnCodXml != null) {
            btnCodXml.setDisable(true);
            btnCodXml.setOnAction(null);
        }
        if (btnAudio != null) {
            btnAudio.setDisable(true);
            btnAudio.setOnAction(null);
        }

        System.out.println("CadastroFreteController: configurarListenersDeCamposEEventos() concluído.");
    }

    // Método original `configureDynamicComboBox` foi renomeado para `configurarComboBoxRemetenteCliente` ou similar,
    // já que `cbitem` tem uma configuração especial agora.
    // Manterei este método para cbRemetente e cbCliente que ainda são tipo String.
    private void configureDynamicComboBox(
            ComboBox<String> comboBox,
            ObservableList<String> originalListInput,
            java.util.function.Consumer<String> processFunction,
            Node nextNodeToFocusOnSelectionOrEnter
    ) {
        if (comboBox == null || comboBox.getEditor() == null) {
            System.err.println("configureDynamicComboBox: ComboBox ou seu editor é null. Abortando configuração.");
            return;
        }
        final ObservableList<String> actualOriginalList =
                (originalListInput != null) ? originalListInput : FXCollections.observableArrayList();

        comboBox.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (programmaticamenteAtualizando) {
                return;
            }

            if (actualOriginalList == null) {
                comboBox.setItems(FXCollections.observableArrayList());
                return;
            }
            if (newV == null || newV.isEmpty()) {
                programmaticamenteAtualizando = true;
                try {
                    comboBox.setItems(FXCollections.observableArrayList(actualOriginalList));
                } finally {
                    programmaticamenteAtualizando = false;
                }
                if (comboBox.isShowing()) comboBox.hide();
            } else {
                ObservableList<String> filteredList = actualOriginalList.filtered(
                        item -> item != null && item.toLowerCase().contains(newV.toLowerCase())
                );
                programmaticamenteAtualizando = true;
                try {
                    comboBox.setItems(filteredList);
                } finally {
                    programmaticamenteAtualizando = false;
                }
                if (!filteredList.isEmpty() && comboBox.getEditor().isFocused() && !comboBox.isShowing()) {
                    comboBox.show();
                } else if (filteredList.isEmpty() && comboBox.isShowing()) {
                    comboBox.hide();
                }
            }
        });

        comboBox.focusedProperty().addListener((obs, oldV, newV) -> {
            if (programmaticamenteAtualizando) return;

            if (actualOriginalList == null) return;

            if (!newV) {
                String text = comboBox.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    String matched = null;
                    for (String item : actualOriginalList) {
                        if (item != null && item.equalsIgnoreCase(text.trim())) {
                            matched = item;
                            break;
                        }
                    }
                    final String finalMatched = matched;
                    final String finalText = text.trim();
                    javafx.application.Platform.runLater(() -> {
                        programmaticamenteAtualizando = true;
                        try {
                            if (finalMatched != null) {
                                if (comboBox.getValue() == null || !comboBox.getValue().equalsIgnoreCase(finalMatched)) {
                                     comboBox.setValue(finalMatched);
                                }
                            } else {
                                processFunction.accept(finalText);
                            }
                        } finally {
                            programmaticamenteAtualizando = false;
                        }
                        if (nextNodeToFocusOnSelectionOrEnter != null && nextNodeToFocusOnSelectionOrEnter.isFocusTraversable()
                            && !nextNodeToFocusOnSelectionOrEnter.isFocused()) {
                            nextNodeToFocusOnSelectionOrEnter.requestFocus();
                        }
                    });
                }
            } else {
                programmaticamenteAtualizando = true;
                try {
                    comboBox.setItems(FXCollections.observableArrayList(actualOriginalList));
                } finally {
                    programmaticamenteAtualizando = false;
                }
            }
        });

        comboBox.setOnAction(e -> {
            if (programmaticamenteAtualizando) {
                return;
            }
            String selectedItem = comboBox.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                processFunction.accept(selectedItem);
                if (nextNodeToFocusOnSelectionOrEnter != null && nextNodeToFocusOnSelectionOrEnter.isFocusTraversable()) {
                    if (nextNodeToFocusOnSelectionOrEnter != comboBox && !nextNodeToFocusOnSelectionOrEnter.isFocused()) {
                        nextNodeToFocusOnSelectionOrEnter.requestFocus();
                    }
                }
            }
        });
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
            // CORREÇÃO: A navegação no ComboBox do Item precisa ser no editor
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

        final ObservableList<String> listaContatos =
                (listaContatosInput != null) ? listaContatosInput : FXCollections.observableArrayList();
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
                confirmacao.setHeaderText(tipoContatoContexto + " não encontrado: '" + nomeContato + "'");
                confirmacao.setContentText("Deseja cadastrar este novo " + tipoContatoContexto.toLowerCase() + "?");
                ButtonType btnSim = new ButtonType("Sim, Cadastrar");
                ButtonType btnNao = new ButtonType("Não");
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
                            showAlert(AlertType.INFORMATION, "Sucesso",
                                    novoNomeContato + " cadastrado como " + tipoContatoContexto + "!");
                            if (!listaContatos.contains(novoNomeContato)) {
                                listaContatos.add(novoNomeContato);
                                FXCollections.sort(listaContatos);
                            }
                            final String finalNome = novoNomeContato;
                            javafx.application.Platform.runLater(() -> {
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
                        if ("23505".equals(e.getSQLState())) {
                            showAlert(AlertType.ERROR, "Erro de Duplicidade",
                                    "O " + tipoContatoContexto.toLowerCase() + " '" + novoNomeContato + "' já existe.");
                        } else {
                            showAlert(AlertType.ERROR, "Erro ao Salvar",
                                    "Não foi possível salvar o novo " +
                                            tipoContatoContexto.toLowerCase() + ": " + e.getMessage());
                        }
                    }
                } else {
                    if (comboBox.isEditable() && comboBox.getEditor() != null) {
                        comboBox.getEditor().clear();
                    }
                    comboBox.setValue(null);
                }
            }
        } finally {
            if (tipoContatoContexto.equals("Remetente")) {
                processandoContatoRemetente = false;
            } else if (tipoContatoContexto.equals("Cliente")) {
                processandoContatoCliente = false;
            }
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
                c.setHeaderText("Item não encontrado: '" + nomeItemInput.trim() + "'");
                c.setContentText("Deseja cadastrar este novo item?");
                ButtonType bS = new ButtonType("Sim, Cadastrar");
                ButtonType bN = new ButtonType("Não");
                c.getButtonTypes().setAll(bS, bN, ButtonType.CANCEL);
                Optional<ButtonType> res = c.showAndWait();
                if (res.isPresent() && res.get() == bS) {
                    abrirDialogCadastroNovoItem(nomeItemInput.trim());
                } else {
                    if (txtpreco != null) txtpreco.clear();
                    if (txttotal != null) txttotal.clear();
                    // CORREÇÃO: Limpar o cbitem corretamente, já que agora é tipo ItemFrete
                    if (cbitem != null) {
                        programmaticamenteAtualizando = true;
                        try {
                            cbitem.getSelectionModel().clearSelection();
                            if (cbitem.isEditable() && cbitem.getEditor() != null) {
                                cbitem.getEditor().clear();
                            }
                            cbitem.setValue(null); // Define o valor como null
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
        if (lista == null) {
            System.err.println("carregarContatosParaComboBoxes: Lista fornecida para " + tipo + " é null.");
            return;
        }
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
            System.err.println("Erro SQL ao carregar " + tipo + "s: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro GERAL ao carregar " + tipo + "s: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(tipo + "s carregados na lista: " + lista.size());
    }

    private void carregarRotas() {
        if (listaRotasOriginal == null) {
            listaRotasOriginal = FXCollections.observableArrayList();
        }
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
            System.err.println("Falha SQL ao carregar rotas: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro de Banco de Dados",
                    "Falha ao carregar lista de rotas: " + e.getMessage());
        }
        System.out.println("Rotas carregadas: " + listaRotasOriginal.size());
    }

    private void carregarConferentesDoBanco() {
        if (listaConferentesOriginal == null) {
            listaConferentesOriginal = FXCollections.observableArrayList();
        }
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
            System.err.println("Erro SQL ao carregar conferentes: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Conferentes carregados: " + listaConferentesOriginal.size());
    }

    private void carregarCidadesDoBanco() {
        if (listaCidadesOriginal == null) {
            listaCidadesOriginal = FXCollections.observableArrayList();
        }
        listaCidadesOriginal.clear();
        listaCidadesOriginal.addAll("Manaus (AM)", "Belém (PA)", "Santarém (PA)", "Porto Velho (RO)");
        System.out.println("Cidades carregadas (mock): " + listaCidadesOriginal.size());
    }

    private void carregarItensCadastradosParaComboBox() {
        if (listaItensDisplayOriginal == null) {
            listaItensDisplayOriginal = FXCollections.observableArrayList();
        }
        if (mapItensCadastrados == null) {
            mapItensCadastrados = new HashMap<>();
        }
        listaItensDisplayOriginal.clear();
        mapItensCadastrados.clear();

        try {
            ItemFreteDAO dao = new ItemFreteDAO();
            // CORREÇÃO: Chamar listarTodos(false) para obter apenas itens ATIVOS para a combobox
            List<ItemFrete> todosAtivos = dao.listarTodos(false); 

            for (ItemFrete item : todosAtivos) { // Itera sobre itens ativos
                if (item != null && item.getNomeItem() != null) {
                    String desc = item.getNomeItem().toLowerCase();
                    if (!listaItensDisplayOriginal.contains(item)) { // Adiciona o objeto ItemFrete
                        listaItensDisplayOriginal.add(item);
                    }
                    mapItensCadastrados.put(desc, item); // Mapeia pelo nome em minúsculas
                }
            }
            // Não é necessário ordenar listaItensDisplayOriginal se o DAO já ordena,
            // mas FXCollections.sort(listaItensDisplayOriginal, Comparator.comparing(ItemFrete::getNomeItem));
            // pode ser usado se precisar ordenar na UI por nome.

            System.out.println("Itens cadastrados (DAO.listarTodos) carregados: "
                    + listaItensDisplayOriginal.size());
        } catch (SQLException e) {
            System.err.println("Falha SQL ao carregar itens de frete (DAO): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirDialogCadastroNovoItem(String descSugerida) {
        try {
            // Usar a classe TripleCustom
            Dialog<TripleCustom<String, String, String>> d = new Dialog<>();
            d.setTitle("Novo Item de Frete");
            d.setHeaderText("Forneça a descrição (minúsculas) e os preços do novo item.");
            ButtonType sBT = ButtonType.OK;
            d.getDialogPane().getButtonTypes().addAll(sBT, ButtonType.CANCEL);

            GridPane g = new GridPane();
            g.setHgap(10);
            g.setVgap(10);
            g.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField tDI = new TextField();
            tDI.setPromptText("Descrição do Item");
            tDI.setText(descSugerida);

            TextField tPN = new TextField(); // Preço Normal
            tPN.setPromptText("Ex: 12.50 ou 12,50");

            TextField tPD = new TextField(); // Preço com Desconto
            tPD.setPromptText("Ex: 10.00 ou 10,00");

            g.add(new Label("Descrição:"), 0, 0);
            g.add(tDI, 1, 0);
            g.add(new Label("Preço Normal (R$):"), 0, 1);
            g.add(tPN, 1, 1);
            g.add(new Label("Preço c/ Desconto (R$):"), 0, 2);
            g.add(tPD, 1, 2);

            Node bSD = d.getDialogPane().lookupButton(sBT);
            Runnable uSBDS = () -> bSD.setDisable(
                    tDI.getText().trim().isEmpty() ||
                            tPN.getText().trim().isEmpty() ||
                            !isPrecoValido(tPN.getText()) ||
                            tPD.getText().trim().isEmpty() ||
                            !isPrecoValido(tPD.getText())
            );
            tDI.textProperty().addListener((o, oV, nV) -> uSBDS.run());
            tPN.textProperty().addListener((o, oV, nV) -> uSBDS.run());
            tPD.textProperty().addListener((o, oV, nV) -> uSBDS.run());
            uSBDS.run();

            d.getDialogPane().setContent(g);
            d.setResultConverter(dB -> {
                if (dB == sBT) {
                    try {
                        parseValorMonetario(tPN.getText());
                        parseValorMonetario(tPD.getText());
                        return new TripleCustom<>(tDI.getText().trim(), tPN.getText(), tPD.getText());
                    } catch (ParseException e) {
                        showAlert(AlertType.ERROR, "Preço Inválido",
                                "Um dos valores de preço digitados ('" + tPN.getText() + "' ou '" + tPD.getText() + "') não é um preço válido.");
                        return null;
                    }
                }
                return null;
            });

            Optional<TripleCustom<String, String, String>> res = d.showAndWait();
            ultimoItemProcessadoCbItem = null;
            res.ifPresent(pDP -> {
                String nDO = pDP.getFirstValue();
                String nD  = nDO.toLowerCase();
                String nPSNormal = pDP.getSecondValue();
                String nPSDesconto = pDP.getThirdValue();
                double nPNormal, nPDesconto;
                try {
                    nPNormal = parseValorMonetario(nPSNormal);
                    nPDesconto = parseValorMonetario(nPSDesconto);
                } catch (ParseException e) {
                    showAlert(AlertType.ERROR, "Erro Interno de Conversão",
                            "Um dos preços se tornou inválido.");
                    return;
                }
                String sqlI = "INSERT INTO itens_frete_padrao (nome_item, descricao, unidade_medida, preco_unitario_padrao, preco_unitario_desconto, ativo) VALUES (?, ?, ?, ?, ?, TRUE)";
                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement p = con.prepareStatement(sqlI, Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, nD);
                    p.setString(2, "");
                    p.setString(3, "");
                    p.setBigDecimal(4, BigDecimal.valueOf(nPNormal).setScale(2, RoundingMode.HALF_UP));
                    p.setBigDecimal(5, BigDecimal.valueOf(nPDesconto).setScale(2, RoundingMode.HALF_UP));
                    int aR = p.executeUpdate();
                    if (aR > 0) {
                        showAlert(AlertType.INFORMATION, "Sucesso",
                                "Item '" + nDO + "' (salvo como '" + nD + "') com preços " + df.format(nPNormal) + " / " + df.format(nPDesconto) + " cadastrado!");
                        
                        // Recarrega os itens no ComboBox da tela de Frete para refletir o novo item
                        carregarItensCadastradosParaComboBox(); 
                        setComboBoxItems(); // Atualiza os items do ComboBox

                        ItemFrete newItem = new ItemFrete((int)0L, nD, "", "", BigDecimal.valueOf(nPNormal), BigDecimal.valueOf(nPDesconto), true);
                        mapItensCadastrados.put(nD, newItem);

                        if (cbitem != null) {
                            programmaticamenteAtualizando = true;
                            try {
                                // CORREÇÃO: Setar o valor do ComboBox com o objeto ItemFrete recém-criado
                                cbitem.setValue(newItem); 
                            } finally {
                                programmaticamenteAtualizando = false;
                            }
                        }
                        processarItemDigitadoOuSelecionado(nD);
                        ultimoItemProcessadoCbItem = nD;
                        if (txtpreco != null && txtpreco.isFocusTraversable()) {
                            txtpreco.requestFocus();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    if ("23505".equals(e.getSQLState())) {
                        showAlert(AlertType.ERROR, "Erro de Duplicidade", "Item '" + nD + "' já existe no banco de dados.");
                    } else {
                        showAlert(AlertType.ERROR, "Erro ao Salvar", "Não foi possível salvar o novo item: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Inesperado", "Não foi possível abrir o diálogo de novo item.");
            ultimoItemProcessadoCbItem = null;
        }
    }

    private boolean isPrecoValido(String precoStr) {
        if (precoStr == null || precoStr.trim().isEmpty()) return false;
        try {
            parseValorMonetario(precoStr);
            return true;
        } catch (ParseException e) {
            return false;
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

    @FXML
    private void handleNovoFrete(ActionEvent event) {
        limparCamposFrete();
        habilitarCamposParaEdicao(true);
        freteAtualId = -1;
        if (txtNumFrete != null) txtNumFrete.setText("Automático");
        if (cbRemetente != null) cbRemetente.requestFocus();
        if (btnNovo != null) btnNovo.setDisable(true);
        if (btnSalvar != null) btnSalvar.setDisable(false);
        if (btnAlterar != null) btnAlterar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }

    @FXML
    private void handleSalvarFrete(ActionEvent event) {
        salvarOuAlterarFrete();
    }

    @FXML
    private void handleAlterarFrete(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "Não há frete selecionado para alteração.");
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
            showAlert(AlertType.WARNING, "Campo Obrigatório", "Remetente deve ser informado.");
            if (cbRemetente != null) cbRemetente.requestFocus();
            return;
        }
        if (cbCliente == null || cbCliente.getValue() == null || cbCliente.getValue().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Obrigatório", "Cliente (Destinatário) deve ser informado.");
            if (cbCliente != null) cbCliente.requestFocus();
            return;
        }
        if (cbRota == null || cbRota.getValue() == null || cbRota.getValue().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Obrigatório", "Rota deve ser informada.");
            if (cbRota != null) cbRota.requestFocus();
            return;
        }
        if (listaTabelaItensFrete == null || listaTabelaItensFrete.isEmpty()) {
            showAlert(AlertType.WARNING, "Nenhum Item na Nota", "É necessário adicionar pelo menos um item ao frete.");
            if (txtquantidade != null) txtquantidade.requestFocus();
            return;
        }

        Connection conn = null;
        try {
            conn = ConexaoBD.getConnection();
            conn.setAutoCommit(false);

            long numeroFreteParaOperacao = freteAtualId;
            boolean isNewFrete = (freteAtualId == -1);

            if (isNewFrete) {
                numeroFreteParaOperacao = gerarNumeroFreteNoBanco();
            }

            String sqlFrete;
            if (isNewFrete) {
                sqlFrete = "INSERT INTO fretes (" +
                        "id_frete, numero_frete, data_emissao, data_saida_viagem, local_transporte, " +
                        "remetente_nome_temp, destinatario_nome_temp, rota_temp, conferente_temp, cidade_cobranca, " +
                        "observacoes, num_notafiscal, valor_notafiscal, peso_notafiscal, valor_total_itens, " +
                        "desconto, valor_frete_calculado, valor_pago, troco, valor_devedor, tipo_pagamento, nome_caixa, status_frete" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            } else {
                sqlFrete = "UPDATE fretes SET " +
                        "data_emissao = ?, data_saida_viagem = ?, local_transporte = ?, " +
                        "remetente_nome_temp = ?, destinatario_nome_temp = ?, rota_temp = ?, conferente_temp = ?, cidade_cobranca = ?, " +
                        "observacoes = ?, num_notafiscal = ?, valor_notafiscal = ?, peso_notafiscal = ?, valor_total_itens = ?, " +
                        "desconto = ?, valor_frete_calculado = ?, valor_pago = ?, troco = ?, valor_devedor = ?, tipo_pagamento = ?, nome_caixa = ?, status_frete = ? " +
                        "WHERE id_frete = ?";
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
                pstFrete.setString(paramIdx++, cbCidadeDeCobranca.getValue());
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

            String sqlItem = "INSERT INTO frete_itens (" +
                    "id_frete, nome_item_ou_id_produto, quantidade, preco_unitario, subtotal_item) VALUES (?,?,?,?,?)";
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
                mensagemSucesso = "Frete número " + numeroFreteParaOperacao + " salvo com sucesso!";
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
                mensagemSucesso = "Frete número " + numeroFreteParaOperacao + " alterado com sucesso!";
            }
            showAlert(AlertType.INFORMATION, "Sucesso", mensagemSucesso);

            habilitarCamposParaVisualizacao(true);
            if (btnNovo != null) btnNovo.setDisable(false);
            if (btnSalvar != null) btnSalvar.setDisable(true);
            if (btnAlterar != null) btnAlterar.setDisable(false);
            if (btnExcluir != null) btnExcluir.setDisable(false);

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro na Operação do Frete",
                    "Ocorreu um erro no banco de dados:\n" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Inesperado", "Ocorreu um erro geral na operação:\n" + e.getMessage());
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
            showAlert(AlertType.ERROR, "Erro Interno de Componente",
                    "Componentes para adicionar itens não foram inicializados.");
            return;
        }
        if (btnSalvar.isDisabled() && btnAlterar.isDisabled()) {
             showAlert(AlertType.WARNING, "Aviso", "Habilite a edição do frete (Botão 'Novo' ou 'Alterar') antes de adicionar itens.");
             return;
        }

        String qtdStr = txtquantidade.getText().trim();
        String itemNomeOuDescricao = null;
        // CORREÇÃO: cbitem.getValue() já retorna um ItemFrete. Se for nulo, pega do editor.
        if (cbitem.getValue() != null) {
            itemNomeOuDescricao = cbitem.getValue().getNomeItem();
        } else if (cbitem.getEditor() != null) {
            itemNomeOuDescricao = cbitem.getEditor().getText();
        }
        
        String precoStr = txtpreco.getText().trim();

        if (qtdStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Obrigatório",
                    "Quantidade do item deve ser informada.");
            txtquantidade.requestFocus();
            return;
        }
        if (itemNomeOuDescricao == null || itemNomeOuDescricao.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Obrigatório",
                    "Descrição/Nome do item deve ser informado.");
            cbitem.requestFocus();
            return;
        }
        if (precoStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Obrigatório",
                    "Preço unitário do item deve ser informado.");
            txtpreco.requestFocus();
            return;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(qtdStr);
            if (quantidade <= 0) throw new NumberFormatException("Qtd > 0");
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Formato Inválido",
                    "Quantidade ('" + qtdStr + "') inválida. Deve ser inteiro > 0.");
            txtquantidade.requestFocus();
            return;
        }
        double precoUnitario;
        try {
            precoUnitario = parseValorMonetario(precoStr);
            if (precoUnitario < 0) throw new ParseException("Preço não negativo", 0);
        } catch (ParseException e) {
            showAlert(AlertType.ERROR, "Formato Inválido",
                    "Preço unitário ('" + precoStr + "') inválido.");
            txtpreco.requestFocus();
            return;
        }

        String nomeItemFinal = itemNomeOuDescricao.trim().toLowerCase();
        if (listaTabelaItensFrete != null) {
            listaTabelaItensFrete.add(new FreteItem(quantidade, nomeItemFinal, precoUnitario));
        } else {
            System.err.println("listaTabelaItensFrete é NULL ao adicionar item!");
        }
        limparCamposItem();
        if (txtquantidade != null && txtquantidade.isFocusTraversable()) {
            txtquantidade.requestFocus();
        }
    }

    @FXML private void handleExcluirFrete(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "Não há frete selecionado para exclusão.");
            return;
        }

        Alert confirmacao = new Alert(AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Exclusão");
        confirmacao.setHeaderText("Excluir Frete " + txtNumFrete.getText() + "?");
        confirmacao.setContentText("Esta ação não pode ser desfeita. Deseja realmente excluir este frete e todos os seus itens?");
        
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
                showAlert(AlertType.INFORMATION, "Sucesso", "Frete " + txtNumFrete.getText() + " excluído com sucesso!");
                limparCamposFrete();
                habilitarCamposParaVisualizacao(false);
                if (btnNovo != null) btnNovo.setDisable(false);
                if (btnSalvar != null) btnSalvar.setDisable(true);
                if (btnAlterar != null) btnAlterar.setDisable(true);
                if (btnExcluir != null) btnExcluir.setDisable(true);
                freteAtualId = -1;
            } catch (SQLException e) {
                try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Erro ao Excluir Frete",
                        "Não foi possível excluir o frete:\n" + e.getMessage());
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


    @FXML private void imprimirNotaFretePersonalizada(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "É necessário ter um frete salvo ou carregado para imprimir a nota.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente",
                "Imprimir Nota Fiscal do Frete - Não implementado.");
    }
    @FXML private void imprimirReciboPersonalizado(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "É necessário ter um frete salvo ou carregado para imprimir o recibo.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente",
                "Imprimir Recibo do Frete - Não implementado.");
    }
    @FXML private void abrirListaFretes(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente",
                "Abrir Lista de Fretes - Esta funcionalidade deve ser implementada para abrir a tela de listagem.");
    }
    @FXML private void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        if (source != null && source.getScene() != null) {
            Stage s = (Stage) source.getScene().getWindow();
            if (s != null) s.close();
            else System.err.println("Erro ao fechar janela: stage é null.");
        } else {
            System.err.println("Erro ao fechar janela: source ou scene é null.");
        }
    }
    @FXML private void handleFotoNota(ActionEvent event) {
        // Nada acontece, botão desabilitado por enquanto
    }
    @FXML private void handleCodXml(ActionEvent event) {
        // Nada acontece, botão desabilitado por enquanto
    }
    @FXML private void handleAudio(ActionEvent event) {
        // Nada acontece, botão desabilitado por enquanto
    }
    @FXML private void handleImprimirEtiqueta(ActionEvent event) {
        if (freteAtualId == -1) {
            showAlert(AlertType.WARNING, "Aviso", "É necessário ter um frete salvo ou carregado para imprimir etiquetas.");
            return;
        }
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente",
                "Imprimir Etiqueta(s) - Não implementado.");
    }

    private void habilitarCamposDaNotaFiscal(boolean formularioHabilitado) {
        boolean notaFiscalSelecionada = rbSim != null && rbSim.isSelected();
        boolean habilitarCamposNF = formularioHabilitado && notaFiscalSelecionada;

        // Esses botões estarão sempre desabilitados por enquanto, conforme solicitado
        if (btnFotoNota != null)       btnFotoNota.setDisable(true);
        if (btnCodXml != null)         btnCodXml.setDisable(true);
        if (btnAudio != null)          btnAudio.setDisable(true);

        if (txtNumNota != null)        txtNumNota.setDisable(!habilitarCamposNF);
        if (txtValorNota != null)      txtValorNota.setDisable(!habilitarCamposNF);
        if (txtPesoNota != null)       txtPesoNota.setDisable(!habilitarCamposNF);

        if (!habilitarCamposNF) {
            if (txtNumNota != null)    txtNumNota.clear();
            if (txtValorNota != null)  txtValorNota.clear();
            if (txtPesoNota != null)   txtPesoNota.clear();
        }
    }

    private void habilitarCamposParaEdicao(boolean habilitar) {
        if (cbRemetente != null)         cbRemetente.setDisable(!habilitar);
        if (cbCliente != null)           cbCliente.setDisable(!habilitar);
        if (cbRota != null)              cbRota.setDisable(!habilitar);
        if (txtSaida != null)            txtSaida.setDisable(!habilitar);
        if (txtLocalTransporte != null)  txtLocalTransporte.setDisable(!habilitar);
        if (cbConferente != null)        cbConferente.setDisable(!habilitar);
        if (cbCidadeDeCobranca != null)  cbCidadeDeCobranca.setDisable(!habilitar);
        if (rbSim != null)               rbSim.setDisable(!habilitar);
        if (Rbnao != null)               Rbnao.setDisable(!habilitar);
        
        habilitarCamposDaNotaFiscal(habilitar); 

        if (txtObs != null)              txtObs.setDisable(!habilitar);
        if (rbComDesconto != null)       rbComDesconto.setDisable(!habilitar);
        if (rbNormal != null)            rbNormal.setDisable(!habilitar);
        if (txtquantidade != null)       txtquantidade.setDisable(!habilitar);
        if (cbitem != null)              cbitem.setDisable(!habilitar); // CORREÇÃO: cbitem habilitado aqui
        if (txtpreco != null)            txtpreco.setDisable(!habilitar);
        if (btnInserir != null)          btnInserir.setDisable(!habilitar);
        
        if (tabelaItens != null) {
            tabelaItens.setEditable(habilitar);
            if (colQuantidade != null) colQuantidade.setEditable(habilitar);
            if (colItem != null) colItem.setEditable(habilitar);
            if (colPreco != null) colPreco.setEditable(habilitar);
            tabelaItens.setDisable(!habilitar);
        }
    }

    private void habilitarCamposParaVisualizacao(boolean habilitar) {
        // Desabilita campos de entrada para edição (modo visualização)
        habilitarCamposParaEdicao(false);

        // Habilita os botões de IA novamente, se for o caso. (Mantendo desabilitados por enquanto)
        if (btnFotoNota != null) btnFotoNota.setDisable(true);
        if (btnCodXml != null)   btnCodXml.setDisable(true);
        if (btnAudio != null)    btnAudio.setDisable(true);


        boolean itensPresentes = listaTabelaItensFrete != null && !listaTabelaItensFrete.isEmpty();
        boolean podeImprimir = habilitar && itensPresentes && freteAtualId != -1;

        if (BtnImprimirNota != null)     BtnImprimirNota.setDisable(!podeImprimir);
        if (btnImprimirEtiqueta != null) btnImprimirEtiqueta.setDisable(!podeImprimir);
        if (btnImprimirRecibo != null)   btnImprimirRecibo.setDisable(!podeImprimir);
    }

    private void limparCamposFrete() {
        programmaticamenteAtualizando = true;
        try {
            if (txtNumFrete != null) txtNumFrete.setText("Automático");
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
            if (cbCidadeDeCobranca != null) {
                cbCidadeDeCobranca.getSelectionModel().clearSelection();
                cbCidadeDeCobranca.setValue(null);
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
            programmaticamenteAtualizando = true;
            try {
                cbitem.getSelectionModel().clearSelection();
                if (cbitem.isEditable() && cbitem.getEditor() != null) {
                    cbitem.getEditor().clear();
                }
                cbitem.setValue(null);
                // CORREÇÃO: Garante que os itens da combobox sejam recarregados para o estado original
                if (listaItensDisplayOriginal != null) {
                    cbitem.setItems(FXCollections.observableArrayList(listaItensDisplayOriginal));
                }
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
        String valorLimpo = valorStr.replace("R$", "")
                                    .trim()
                                    .replace(".", "")
                                    .replace(",", ".");
        try {
            return Double.parseDouble(valorLimpo);
        } catch (NumberFormatException e) {
            throw new ParseException("Valor monetário '" + valorStr + "' inválido.", 0);
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
            System.err.println("Erro ao converter '" + vS + "' para BigDecimal: " + e.getMessage());
            throw new IllegalArgumentException("Valor monetário inválido: '" + vS + "'.", e);
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
            if (fxmlFileRelative == null || fxmlFileRelative.trim().isEmpty()) {
                showAlert(AlertType.ERROR, "Erro Interno", "Nome do arquivo FXML não pode ser vazio.");
                return;
            }
            String fxmlPath = fxmlFileRelative.startsWith("/")
                    ? fxmlFileRelative
                    : "/gui/" + fxmlFileRelative;

            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                showAlert(AlertType.ERROR, "Erro ao Abrir Tela",
                        "Arquivo FXML não pôde ser localizado: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setResizable(resizable);

            URL cssLocation = getClass().getResource("/css/main.css");
            if (cssLocation != null) {
                stage.getScene().getStylesheets().add(cssLocation.toExternalForm());
            }
            if (modality != null && modality != Modality.NONE) {
                stage.initModality(modality);
                stage.showAndWait();
            } else {
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Abrir Tela",
                    "Não foi possível carregar a tela: " + fxmlFileRelative + "\nDetalhes: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Crítico",
                    "Ocorreu um erro inesperado ao tentar abrir a tela '" + title + "'.");
        }
    }

    public void atualizarListaItensDoComboBox() {
        carregarItensCadastradosParaComboBox();
        setComboBoxItems();
    }

    // CORREÇÃO: Nova classe auxiliar TripleCustom para passar 3 valores
    // Mantenho aqui por simplicidade da entrega, mas o ideal é que esteja em 'util' ou similar.
    private static class TripleCustom<A, B, C> {
        private final A firstValue;
        private final B secondValue;
        private final C thirdValue;

        public TripleCustom(A firstValue, B secondValue, C thirdValue) {
            this.firstValue = firstValue;
            this.secondValue = secondValue;
            this.thirdValue = thirdValue;
        }

        public A getFirstValue() {
            return firstValue;
        }

        public B getSecondValue() {
            return secondValue;
        }

        public C getThirdValue() {
            return thirdValue;
        }
    }
}