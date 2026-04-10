package gui;

import dao.ConexaoBD;
import dao.ItemFreteDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty; // Para BigDecimal
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import model.ItemFrete;

import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import gui.util.AlertHelper;
import gui.util.AppLogger;

public class CadastroItensController implements Initializable {

    // --- FXML Components ---
    @FXML private TableView<ItemFrete> tabelaFrete; // Nome da variável é tabelaFrete
    @FXML private TableColumn<ItemFrete, String> colNomeFrete;
    @FXML private TableColumn<ItemFrete, BigDecimal> colValorNormal;
    @FXML private TableColumn<ItemFrete, BigDecimal> colValorDesconto;
    @FXML private TableColumn<ItemFrete, Boolean> colAtivoFrete;

    // Campos de entrada para edição/novo item
    @FXML private TextField txtNomeItem;
    @FXML private TextField txtValorNormal;
    @FXML private TextField txtValorDesconto;
    @FXML private CheckBox chkAtivo;

    // Botões
    @FXML private Button btnNovoFrete;
    @FXML private Button btnEditarFrete;
    @FXML private Button btnExcluirFrete;
    @FXML private Button btnSalvarFrete;
    @FXML private Button btnCancelarFrete;

    // --- Internal State ---
    private ObservableList<ItemFrete> listaItensFrete = FXCollections.observableArrayList();
    private ItemFrete itemSelecionadoParaEdicao; // O item atualmente selecionado/sendo editado

    private final DecimalFormat df = new DecimalFormat("#,##0.00",
            new DecimalFormatSymbols(new Locale("pt","BR")));

    private boolean programmaticamenteAtualizando = false; // Flag para evitar loops em listeners

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Itens"); return; }
        configurarTabela();
        configurarCamposDeEntrada(); // Novo método para configurar listeners dos TextFields

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                ItemFreteDAO dao = new ItemFreteDAO();
                List<ItemFrete> dados = dao.listarTodos(true);
                Platform.runLater(() -> listaItensFrete.setAll(dados));
            } catch (Exception e) {
                AppLogger.warn("CadastroItensController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        atualizarEstadoUI(EstadoUI.VISUALIZACAO); // Inicia no modo de visualização
    }

    private void configurarTabela() {
        // Vincula as colunas aos atributos do modelo ItemFrete
        colNomeFrete.setCellValueFactory(new PropertyValueFactory<>("nomeItem"));
        colValorNormal.setCellValueFactory(new PropertyValueFactory<>("precoUnitarioPadrao"));
        colValorDesconto.setCellValueFactory(new PropertyValueFactory<>("precoUnitarioDesconto"));
        
        // CORREÇÃO: Para CheckBoxTableCell, usar PropertyValueFactory para o atributo e não SimpleObjectProperty
        colAtivoFrete.setCellValueFactory(new PropertyValueFactory<>("ativo"));
        colAtivoFrete.setCellFactory(CheckBoxTableCell.forTableColumn(colAtivoFrete));


        // Formatação de valores monetários e booleanos para exibição na tabela
        colValorNormal.setCellFactory(tc -> new TableCell<ItemFrete, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText("R$ " + df.format(price));
                }
            }
        });
        colValorDesconto.setCellFactory(tc -> new TableCell<ItemFrete, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText("R$ " + df.format(price));
                }
            }
        });
        // A formatação de colAtivoFrete já é feita pelo CheckBoxTableCell.forTableColumn()

        // Listener para a seleção de item na tabela
        tabelaFrete.getSelectionModel().selectedItemProperty().addListener( // USANDO tabelaFrete aqui
            (observable, oldValue, newValue) -> {
                if (programmaticamenteAtualizando) {
                    return; // Ignora se a seleção foi programática
                }
                itemSelecionadoParaEdicao = newValue;
                if (newValue != null) {
                    preencherCamposComItem(newValue);
                    // Chamar o estado correto
                    atualizarEstadoUI(EstadoUI.ITEM_SELECIONADO); 
                } else {
                    limparCamposDeEntrada();
                    atualizarEstadoUI(EstadoUI.VISUALIZACAO);
                }
            });

        tabelaFrete.setItems(listaItensFrete); // USANDO tabelaFrete aqui
    }

    private void configurarCamposDeEntrada() {
        // Listeners para formatação de valores monetários ao perder o foco
        txtValorNormal.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !txtValorNormal.getText().isEmpty()) { // Perdeu o foco
                try {
                    double value = parseValorMonetario(txtValorNormal.getText());
                    txtValorNormal.setText(df.format(value));
                } catch (ParseException e) {
                    AlertHelper.show(AlertType.ERROR, "Erro de Formato", "Valor Normal inválido.");
                    txtValorNormal.setText("0,00"); // Ou limpar ou manter o último válido
                }
            }
        });

        txtValorDesconto.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !txtValorDesconto.getText().isEmpty()) { // Perdeu o foco
                try {
                    double value = parseValorMonetario(txtValorDesconto.getText());
                    txtValorDesconto.setText(df.format(value));
                } catch (ParseException e) {
                    AlertHelper.show(AlertType.ERROR, "Erro de Formato", "Valor com Desconto inválido.");
                    txtValorDesconto.setText("0,00"); // Ou limpar ou manter o último válido
                }
            }
        });
    }

    private void carregarItensFrete() {
        ItemFreteDAO dao = new ItemFreteDAO();
        // Passar true para listarTodos para trazer todos os itens (ativos e inativos)
        listaItensFrete.setAll(dao.listarTodos(true));
    }

    private void preencherCamposComItem(ItemFrete item) {
        programmaticamenteAtualizando = true;
        try {
            txtNomeItem.setText(item.getNomeItem());
            txtValorNormal.setText(df.format(item.getPrecoUnitarioPadrao()));
            txtValorDesconto.setText(df.format(item.getPrecoUnitarioDesconto()));
            chkAtivo.setSelected(item.isAtivo());
        } finally {
            programmaticamenteAtualizando = false;
        }
    }

    private void limparCamposDeEntrada() {
        programmaticamenteAtualizando = true;
        try {
            txtNomeItem.clear();
            txtValorNormal.clear();
            txtValorDesconto.clear();
            chkAtivo.setSelected(false);
        } finally {
            programmaticamenteAtualizando = false;
        }
    }

    // --- UI State Management ---
    private enum EstadoUI {
        VISUALIZACAO,        // Campos desabilitados, apenas "Novo" habilitado
        ITEM_SELECIONADO,    // Um item está na tabela selecionado, "Novo", "Editar", "Excluir" habilitados
        NOVO_ITEM,           // Campos habilitados para um novo item, "Salvar", "Cancelar" habilitados
        EDITANDO_ITEM        // Campos habilitados para edição, "Salvar", "Cancelar" habilitados
    }

    private void atualizarEstadoUI(EstadoUI estado) {
        // Desabilita tudo por padrão e depois habilita o que for necessário
        txtNomeItem.setDisable(true);
        txtValorNormal.setDisable(true);
        txtValorDesconto.setDisable(true);
        chkAtivo.setDisable(true);
        tabelaFrete.setDisable(false); // Tabela habilitada por padrão

        btnNovoFrete.setDisable(false);
        btnEditarFrete.setDisable(true);
        btnExcluirFrete.setDisable(true);
        btnSalvarFrete.setDisable(true);
        btnCancelarFrete.setDisable(true);

        switch (estado) {
            case VISUALIZACAO:
                limparCamposDeEntrada(); // Garante campos vazios se nada selecionado
                tabelaFrete.getSelectionModel().clearSelection(); // Desseleciona
                // Apenas btnNovoFrete está habilitado inicialmente
                break;
            case ITEM_SELECIONADO:
                // Campos preenchidos (feito por preencherCamposComItem)
                btnEditarFrete.setDisable(false);
                btnExcluirFrete.setDisable(false);
                break;
            case NOVO_ITEM:
                limparCamposDeEntrada(); // Garante campos vazios para novo item
                tabelaFrete.getSelectionModel().clearSelection(); // Desseleciona qualquer item
                txtNomeItem.requestFocus(); // Coloca foco no primeiro campo
                // Habilita campos e botões para salvar
                txtNomeItem.setDisable(false);
                txtValorNormal.setDisable(false);
                txtValorDesconto.setDisable(false);
                chkAtivo.setDisable(false);
                tabelaFrete.setDisable(true); // Desabilita tabela enquanto cria/edita

                btnNovoFrete.setDisable(true);
                btnSalvarFrete.setDisable(false);
                btnCancelarFrete.setDisable(false);
                break;
            case EDITANDO_ITEM:
                // Campos já preenchidos (feito por preencherCamposComItem)
                txtNomeItem.setDisable(false);
                txtValorNormal.setDisable(false);
                txtValorDesconto.setDisable(false);
                chkAtivo.setDisable(false);
                tabelaFrete.setDisable(true); // Desabilita tabela enquanto cria/edita

                btnNovoFrete.setDisable(true);
                btnSalvarFrete.setDisable(false);
                btnCancelarFrete.setDisable(false);
                break;
        }
    }

    // --- FXML Event Handlers ---

    @FXML
    private void handleNovoFrete(ActionEvent event) {
        itemSelecionadoParaEdicao = null; // Garante que é um novo item
        atualizarEstadoUI(EstadoUI.NOVO_ITEM);
    }

    @FXML
    private void handleEditarFrete(ActionEvent event) {
        if (itemSelecionadoParaEdicao == null) {
            AlertHelper.show(AlertType.WARNING, "Edição", "Por favor, selecione um item para editar.");
            return;
        }
        atualizarEstadoUI(EstadoUI.EDITANDO_ITEM);
    }

    @FXML
    private void handleExcluirFrete(ActionEvent event) {
        if (itemSelecionadoParaEdicao == null) {
            AlertHelper.show(AlertType.WARNING, "Exclusão", "Por favor, selecione um item para excluir.");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText("Excluir Item: " + itemSelecionadoParaEdicao.getNomeItem() + "?");
        alert.setContentText("Esta ação não pode ser desfeita. Deseja realmente excluir este item?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ItemFreteDAO dao = new ItemFreteDAO();
            boolean excluido = dao.excluir(itemSelecionadoParaEdicao.getIdItemFrete());
            if (excluido) {
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Item excluído com sucesso!");
                carregarItensFrete(); // Recarrega a tabela
                atualizarEstadoUI(EstadoUI.VISUALIZACAO);
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro no Banco de Dados", "Não foi possível excluir o item.");
            }
        }
    }

    @FXML
    private void handleSalvarFrete(ActionEvent event) {
        String nome = txtNomeItem.getText().trim();
        String valorNormalStr = txtValorNormal.getText().trim();
        String valorDescontoStr = txtValorDesconto.getText().trim();
        boolean ativo = chkAtivo.isSelected();

        if (nome.isEmpty() || valorNormalStr.isEmpty() || valorDescontoStr.isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Campos Obrigatórios", "Nome do item, Valor Normal e Valor com Desconto são obrigatórios.");
            return;
        }

        BigDecimal precoPadrao;
        BigDecimal precoDesconto;
        try {
            precoPadrao = BigDecimal.valueOf(parseValorMonetario(valorNormalStr));
            precoDesconto = BigDecimal.valueOf(parseValorMonetario(valorDescontoStr));
        } catch (ParseException e) {
            AlertHelper.show(AlertType.ERROR, "Formato Inválido", "Valores monetários inválidos.");
            return;
        }

        ItemFreteDAO dao = new ItemFreteDAO();
        boolean sucesso;
        if (itemSelecionadoParaEdicao == null) { // Novo item
            // idItemFrete 0 para um novo item, o banco de dados deve gerar o ID real
            ItemFrete novoItem = new ItemFrete(0, nome, "", "", precoPadrao, precoDesconto, ativo);
            sucesso = dao.inserir(novoItem);
            if (sucesso) AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Item '" + nome + "' cadastrado com sucesso!");
        } else { // Editar item existente
            itemSelecionadoParaEdicao.setNomeItem(nome);
            itemSelecionadoParaEdicao.setPrecoUnitarioPadrao(precoPadrao);
            itemSelecionadoParaEdicao.setPrecoUnitarioDesconto(precoDesconto);
            itemSelecionadoParaEdicao.setAtivo(ativo);

            // Mantenha descrição e unidade de medida vazias se não houver campos para elas
            itemSelecionadoParaEdicao.setDescricao("");
            itemSelecionadoParaEdicao.setUnidadeMedida("");

            sucesso = dao.atualizar(itemSelecionadoParaEdicao);
            if (sucesso) AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Item '" + nome + "' atualizado com sucesso!");
        }
        if (!sucesso) {
            AlertHelper.show(AlertType.ERROR, "Erro no Banco de Dados", "Não foi possível salvar o item.");
            return;
        }
        carregarItensFrete(); // Recarrega a tabela para mostrar as mudanças
        atualizarEstadoUI(EstadoUI.VISUALIZACAO); // Volta ao estado de visualização
    }

    @FXML
    private void handleCancelarFrete(ActionEvent event) {
        // Se havia um item selecionado antes de cancelar a edição,
        // ele será re-selecionado implicitamente pelo setAll() em carregarItensFrete(),
        // ou você pode re-selecioná-lo explicitamente se quiser.
        // Por enquanto, apenas volta ao estado de visualização.
        atualizarEstadoUI(EstadoUI.VISUALIZACAO); 
        tabelaFrete.getSelectionModel().clearSelection(); // Garante que não haja seleção ativa
    }

    // --- Helper Methods ---

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

}