package gui;

import dao.ItemFreteDAO;
import dao.ItemEncomendaPadraoDAO; 
import model.ItemFrete;
import model.ItemEncomendaPadrao; 

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
// import java.sql.SQLException; // Não é mais necessário importar explicitamente se não for lançado
import java.util.List;
import gui.util.AlertHelper;
import util.AppLogger;

public class CadastroProdutoController {

    // === ABA “Itens de Frete” ===
    @FXML private TextField         txtNomeFrete;
    @FXML private TextField         txtDescFrete;
    @FXML private TextField         txtUnidFrete;
    @FXML private TextField         txtPrecoNormalFrete;
    @FXML private TextField         txtPrecoDescFrete;
    @FXML private Button            btnSalvarFrete;

    @FXML private TableView<ItemFrete>                tableFrete;
    @FXML private TableColumn<ItemFrete,String>       colNomeFrete;
    @FXML private TableColumn<ItemFrete,String>       colDescFrete;
    @FXML private TableColumn<ItemFrete,String>       colUnidFrete;
    @FXML private TableColumn<ItemFrete,BigDecimal>   colPrecoNormFrete;
    @FXML private TableColumn<ItemFrete,BigDecimal>   colPrecoDescFrete;
    @FXML private TableColumn<ItemFrete,Boolean>      colAtivoFrete;

    private final ObservableList<ItemFrete> listaFrete = FXCollections.observableArrayList();

    // === ABA “Itens de Encomenda” ===
    @FXML private TextField             txtNomeEnc;
    @FXML private TextField             txtDescEnc;
    @FXML private TextField             txtUnidEnc;
    @FXML private TextField             txtPrecoEnc;
    @FXML private CheckBox              chkPermiteValorDeclaradoEnc;
    @FXML private Button                btnSalvarEnc;

    @FXML private TableView<ItemEncomendaPadrao>              tableEncomenda;
    @FXML private TableColumn<ItemEncomendaPadrao,String>     colNomeEnc;
    @FXML private TableColumn<ItemEncomendaPadrao,String>     colDescEnc;
    @FXML private TableColumn<ItemEncomendaPadrao,String>     colUnidEnc;
    @FXML private TableColumn<ItemEncomendaPadrao,BigDecimal> colPrecoEnc;
    @FXML private TableColumn<ItemEncomendaPadrao,Boolean>    colPermiteValorDecEnc;
    @FXML private TableColumn<ItemEncomendaPadrao,Boolean>    colAtivoEnc;

    private final ObservableList<ItemEncomendaPadrao> listaEncomenda = FXCollections.observableArrayList();

    // === BOTÃO “Fechar” ===
    @FXML private Button btnFecharProduto;

    @FXML
    public void initialize() {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Produtos"); return; }
        configurarTabelaFrete();
        configurarTabelaEncomenda();

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<ItemFrete> itensF = new ItemFreteDAO().listarTodos(false);
                List<ItemEncomendaPadrao> itensE = new ItemEncomendaPadraoDAO().listarTodos(false);
                Platform.runLater(() -> {
                    if (itensF != null) listaFrete.addAll(itensF);
                    if (itensE != null) listaEncomenda.addAll(itensE);
                });
            } catch (Exception e) {
                AppLogger.warn("CadastroProdutoController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        btnSalvarFrete.setOnAction(e -> onSalvarFrete());
        btnSalvarEnc .setOnAction(e -> onSalvarEncomenda());
        btnFecharProduto.setOnAction(e -> fecharJanela());
    }

    private void configurarTabelaFrete() {
        tableFrete.setItems(listaFrete);
        colNomeFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeItem()));
        colDescFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colUnidFrete     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidadeMedida()));
        colPrecoNormFrete.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnitarioPadrao()));
        colPrecoDescFrete.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnitarioDesconto()));
        colAtivoFrete    .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isAtivo()));
    }

    private void configurarTabelaEncomenda() {
        tableEncomenda.setItems(listaEncomenda);
        colNomeEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomeItem()));
        colDescEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        colUnidEnc           .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidadeMedida()));
        colPrecoEnc          .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnit()));
        colPermiteValorDecEnc.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isPermiteValorDeclarado()));
        colAtivoEnc          .setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().isAtivo()));
    }

    private void carregarFrete() {
        listaFrete.clear();
        try {
            List<ItemFrete> itens = new ItemFreteDAO().listarTodos(false);
            if (itens != null) listaFrete.addAll(itens);
        } catch (Exception ex) { // CORREÇÃO: Mudado de SQLException para Exception
            AppLogger.error("CadastroProdutoController", ex.getMessage(), ex);
            AlertHelper.warn("Erro ao carregar Itens de Frete:\n" + ex.getMessage());
        }
    }

    private void carregarEncomenda() {
        listaEncomenda.clear();
        try {
            List<ItemEncomendaPadrao> itens = new ItemEncomendaPadraoDAO().listarTodos(false);
            if (itens != null) listaEncomenda.addAll(itens);
        } catch (Exception ex) { // CORREÇÃO: Mudado de SQLException para Exception
            AppLogger.error("CadastroProdutoController", ex.getMessage(), ex);
            AlertHelper.warn("Erro ao carregar Itens de Encomenda:\n" + ex.getMessage());
        }
    }

    private void onSalvarFrete() {
        try {
            ItemFrete it = new ItemFrete();
            it.setNomeItem(txtNomeFrete.getText().trim());
            it.setDescricao(txtDescFrete.getText().trim());
            it.setUnidadeMedida(txtUnidFrete.getText().trim());
            
            // Tratamento simples para troca de vírgula por ponto
            String precoNorm = txtPrecoNormalFrete.getText().replace(",", ".");
            if(precoNorm.isEmpty()) precoNorm = "0.00";
            it.setPrecoUnitarioPadrao(new BigDecimal(precoNorm));

            String precoDesc = txtPrecoDescFrete.getText().replace(",", ".");
            if(precoDesc.isEmpty()) precoDesc = "0.00";
            it.setPrecoUnitarioDesconto(new BigDecimal(precoDesc));
            
            it.setAtivo(true);

            new ItemFreteDAO().inserir(it);
            AlertHelper.info("Item de Frete cadastrado com sucesso!");
            limparCamposFrete();
            carregarFrete();
        } catch (Exception ex) {
            AppLogger.error("CadastroProdutoController", ex.getMessage(), ex);
            AlertHelper.error("Erro ao salvar Frete:\n" + ex.getMessage());
        }
    }

    private void onSalvarEncomenda() {
        try {
            ItemEncomendaPadrao it = new ItemEncomendaPadrao();
            it.setNomeItem(txtNomeEnc.getText().trim());
            it.setDescricao(txtDescEnc.getText().trim());
            it.setUnidadeMedida(txtUnidEnc.getText().trim());
            
            String preco = txtPrecoEnc.getText().replace(",", ".");
            if(preco.isEmpty()) preco = "0.00";
            it.setPrecoUnit(new BigDecimal(preco));
            
            it.setPermiteValorDeclarado(chkPermiteValorDeclaradoEnc.isSelected());
            it.setAtivo(true);

            new ItemEncomendaPadraoDAO().inserir(it);
            
            AlertHelper.info("Item de Encomenda cadastrado com sucesso!");
            limparCamposEncomenda();
            carregarEncomenda();
        } catch (Exception ex) {
            AppLogger.error("CadastroProdutoController", ex.getMessage(), ex);
            AlertHelper.error("Erro ao salvar Encomenda:\n" + ex.getMessage());
        }
    }

    private void limparCamposFrete() {
        txtNomeFrete.clear();
        txtDescFrete.clear();
        txtUnidFrete.clear();
        txtPrecoNormalFrete.clear();
        txtPrecoDescFrete.clear();
    }

    private void limparCamposEncomenda() {
        txtNomeEnc.clear();
        txtDescEnc.clear();
        txtUnidEnc.clear();
        chkPermiteValorDeclaradoEnc.setSelected(false);
        txtPrecoEnc.clear();
    }

    private void fecharJanela() {
        Stage stage = (Stage) btnFecharProduto.getScene().getWindow();
        stage.close();
    }

}