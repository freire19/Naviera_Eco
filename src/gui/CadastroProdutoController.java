package gui;

import dao.ItemFreteDAO;
import dao.EncomendaItemDAO;
import model.ItemFrete;
import model.EncomendaItem;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CadastroProdutoController {

    // === ABA “Itens de Frete” ===
    @FXML private TextField        txtNomeFrete;
    @FXML private TextField        txtDescFrete;
    @FXML private TextField        txtUnidFrete;
    @FXML private TextField        txtPrecoNormalFrete;
    @FXML private TextField        txtPrecoDescFrete;
    @FXML private Button           btnSalvarFrete;

    @FXML private TableView<ItemFrete>                tableFrete;
    @FXML private TableColumn<ItemFrete,String>      colNomeFrete;
    @FXML private TableColumn<ItemFrete,String>      colDescFrete;
    @FXML private TableColumn<ItemFrete,String>      colUnidFrete;
    @FXML private TableColumn<ItemFrete,BigDecimal>  colPrecoNormFrete;
    @FXML private TableColumn<ItemFrete,BigDecimal>  colPrecoDescFrete;
    @FXML private TableColumn<ItemFrete,Boolean>     colAtivoFrete;

    private final ObservableList<ItemFrete> listaFrete = FXCollections.observableArrayList();

    // === ABA “Itens de Encomenda” ===
    @FXML private TextField            txtNomeEnc;
    @FXML private TextField            txtDescEnc;
    @FXML private TextField            txtUnidEnc;
    @FXML private TextField            txtPrecoEnc;
    @FXML private CheckBox             chkPermiteValorDeclaradoEnc;
    @FXML private Button               btnSalvarEnc;

    @FXML private TableView<EncomendaItem>              tableEncomenda;
    @FXML private TableColumn<EncomendaItem,String>     colNomeEnc;
    @FXML private TableColumn<EncomendaItem,String>     colDescEnc;
    @FXML private TableColumn<EncomendaItem,String>     colUnidEnc;
    @FXML private TableColumn<EncomendaItem,BigDecimal> colPrecoEnc;
    @FXML private TableColumn<EncomendaItem,Boolean>    colPermiteValorDecEnc;
    @FXML private TableColumn<EncomendaItem,Boolean>    colAtivoEnc;

    private final ObservableList<EncomendaItem> listaEncomenda = FXCollections.observableArrayList();

    // === BOTÃO “Fechar” NA JANELA ===
    @FXML private Button btnFecharProduto;

    @FXML
    public void initialize() {
        // Configura colunas
        configurarTabelaFrete();
        configurarTabelaEncomenda();

        // Carrega dados
        carregarFrete();
        carregarEncomenda();

        // Liga botões
        btnSalvarFrete.setOnAction(e -> onSalvarFrete());
        btnSalvarEnc .setOnAction(e -> onSalvarEncomenda());
        btnFecharProduto.setOnAction(e -> fecharJanela());
    }

    // Configura colunas da tabela de Frete
    private void configurarTabelaFrete() {
        tableFrete.setItems(listaFrete);
        colNomeFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeItem()));
        colDescFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colUnidFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidadeMedida()));
        colPrecoNormFrete.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnitarioPadrao()));
        colPrecoDescFrete.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnitarioDesconto()));
        colAtivoFrete    .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isAtivo()));
    }

    // Configura colunas da tabela de Encomenda
    private void configurarTabelaEncomenda() {
        tableEncomenda.setItems(listaEncomenda);
        colNomeEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeItem()));
        colDescEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colUnidEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidadeMedida()));
        colPrecoEnc          .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnit()));
        colPermiteValorDecEnc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isPermiteValorDeclarado()));
        colAtivoEnc          .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isAtivo()));
    }

    // Carrega todos os itens de frete ativos
    private void carregarFrete() {
        listaFrete.clear();
        try {
            List<ItemFrete> itens = new ItemFreteDAO().listarTodos(false);
            if (itens != null) listaFrete.addAll(itens);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Erro ao carregar Itens de Frete:\n" + ex.getMessage());
        }
    }

    // Carrega todos os itens de encomenda-padrão ativos
    private void carregarEncomenda() {
        listaEncomenda.clear();
        try {
            List<EncomendaItem> itens = new EncomendaItemDAO().listarTodos(false);
            if (itens != null) listaEncomenda.addAll(itens);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Erro ao carregar Itens de Encomenda:\n" + ex.getMessage());
        }
    }

    // Salva um novo Item de Frete no banco e atualiza a tabela
    private void onSalvarFrete() {
        try {
            ItemFrete it = new ItemFrete();
            it.setNomeItem(txtNomeFrete.getText().trim());
            it.setDescricao(txtDescFrete.getText().trim());
            it.setUnidadeMedida(txtUnidFrete.getText().trim());
            // não existe setPermiteValorDeclarado em ItemFrete
            it.setPrecoUnitarioPadrao(new BigDecimal(txtPrecoNormalFrete.getText().replace(",", ".")));
            it.setPrecoUnitarioDesconto(new BigDecimal(txtPrecoDescFrete.getText().replace(",", ".")));
            it.setAtivo(true);

            new ItemFreteDAO().inserir(it);
            showAlert(Alert.AlertType.INFORMATION, "Item de Frete cadastrado com sucesso!");
            limparCamposFrete();
            carregarFrete();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro ao salvar Frete:\n" + ex.getMessage());
        }
    }

    // Salva um novo Item de Encomenda no banco e atualiza a tabela
    private void onSalvarEncomenda() {
        try {
            EncomendaItem it = new EncomendaItem();
            it.setNomeItem(txtNomeEnc.getText().trim());
            it.setDescricao(txtDescEnc.getText().trim());
            it.setUnidadeMedida(txtUnidEnc.getText().trim());
            it.setPrecoUnit(new BigDecimal(txtPrecoEnc.getText().replace(",", ".")));
            it.setPermiteValorDeclarado(chkPermiteValorDeclaradoEnc.isSelected());
            it.setAtivo(true);

            new EncomendaItemDAO().inserir(it);
            showAlert(Alert.AlertType.INFORMATION, "Item de Encomenda cadastrado com sucesso!");
            limparCamposEncomenda();
            carregarEncomenda();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro ao salvar Encomenda:\n" + ex.getMessage());
        }
    }

    // Limpa os campos da aba de Frete
    private void limparCamposFrete() {
        txtNomeFrete.clear();
        txtDescFrete.clear();
        txtUnidFrete.clear();
        txtPrecoNormalFrete.clear();
        txtPrecoDescFrete.clear();
    }

    // Limpa os campos da aba de Encomenda
    private void limparCamposEncomenda() {
        txtNomeEnc.clear();
        txtDescEnc.clear();
        txtUnidEnc.clear();
        chkPermiteValorDeclaradoEnc.setSelected(false);
        txtPrecoEnc.clear();
    }

    // Fecha a janela
    private void fecharJanela() {
        Stage stage = (Stage) btnFecharProduto.getScene().getWindow();
        stage.close();
    }

    // Helper para mostrar alertas
    private void showAlert(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}

