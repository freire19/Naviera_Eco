package gui;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import dao.ItemEncomendaPadraoDAO;
import gui.util.CompanyDataLoader;
import gui.util.PrintLayoutHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;          // <<< ADICIONADO
import javafx.stage.Window;
import model.ItemEncomendaPadrao;

public class TabelaPrecosEncomendaController implements Initializable {

    // ---------- FXML ----------

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField txtPesquisaItem;

    @FXML
    private Button btnAdicionar;

    @FXML
    private Button btnEditar;

    @FXML
    private Button btnExcluir;

    @FXML
    private Button btnImprimir;

    @FXML
    private TableView<ItemEncomendaPadrao> tablePrecos;

    @FXML
    private TableColumn<ItemEncomendaPadrao, String> colDescricao;

    @FXML
    private TableColumn<ItemEncomendaPadrao, String> colUnidade;

    @FXML
    private TableColumn<ItemEncomendaPadrao, String> colPreco;

    // ---------- DADOS ----------

    private final ItemEncomendaPadraoDAO itemDAO = new ItemEncomendaPadraoDAO();
    private final ObservableList<ItemEncomendaPadrao> masterData = FXCollections.observableArrayList();
    private FilteredList<ItemEncomendaPadrao> filteredData;
    private SortedList<ItemEncomendaPadrao> sortedData;

    private final NumberFormat moedaBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ============================================================
    //  INITIALIZE
    // ============================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Tabela de Precos Encomenda"); return; }
        configurarColunas();
        configurarTabela();
        configurarFiltroPesquisa();
        configurarSelecaoTabela();

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<ItemEncomendaPadrao> itens = itemDAO.listarTodos(false);
                Platform.runLater(() -> {
                    masterData.addAll(itens);
                    tablePrecos.sort();
                });
            } catch (Exception e) {
                System.err.println("Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();
    }

    // ------------------------------------------------------------
    //  CONFIGURAÇÃO DA TABELA
    // ------------------------------------------------------------

    private void configurarColunas() {
        colDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeTrim(cellData.getValue().getNomeItem())));

        colUnidade.setCellValueFactory(cellData ->
                new SimpleStringProperty(safeTrim(cellData.getValue().getUnidadeMedida())));

        colPreco.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMoeda(cellData.getValue().getPrecoUnit())));

        // ordenação padrão pela descrição
        colDescricao.setSortType(TableColumn.SortType.ASCENDING);
    }

    private void configurarTabela() {
        filteredData = new FilteredList<>(masterData, item -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePrecos.comparatorProperty());
        tablePrecos.setItems(sortedData);

        // Ocupa toda a largura da tabela
        tablePrecos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ordenação inicial
        tablePrecos.getSortOrder().setAll(colDescricao);
    }

    private void carregarItens() {
        masterData.clear();
        try {
            // false = apenas itens ativos (ajuste se precisar)
            List<ItemEncomendaPadrao> itens = itemDAO.listarTodos(false);
            masterData.addAll(itens);
            tablePrecos.sort();
        } catch (Exception e) { // pega qualquer erro do DAO
            e.printStackTrace();
            mostrarErro("Erro ao carregar itens da tabela de preços.", e.getMessage());
        }
    }

    private void configurarFiltroPesquisa() {
        txtPesquisaItem.textProperty().addListener((obs, oldValue, newValue) -> {
            String filtro = newValue == null ? "" : newValue.trim().toLowerCase();

            filteredData.setPredicate(item -> {
                if (filtro.isEmpty()) {
                    return true;
                }
                String desc = safeTrim(item.getNomeItem()).toLowerCase();
                return desc.contains(filtro);
            });
        });
    }

    private void configurarSelecaoTabela() {
        btnEditar.setDisable(true);
        btnExcluir.setDisable(true);

        tablePrecos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    boolean temSelecao = newSel != null;
                    btnEditar.setDisable(!temSelecao);
                    btnExcluir.setDisable(!temSelecao);
                });
    }

    // ------------------------------------------------------------
    //  UTILITÁRIOS
    // ------------------------------------------------------------

    private String safeTrim(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return moedaBR.format(valor);
    }

    private BigDecimal parsePrecoDigitado(String texto) {
        if (texto == null) {
            throw new NumberFormatException("Preço vazio");
        }
        String limpo = texto
                .replace("R", "")
                .replace("$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");
        if (limpo.isEmpty()) {
            throw new NumberFormatException("Preço vazio");
        }
        return new BigDecimal(limpo);
    }

    private void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(titulo);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    /**
     * Diálogo genérico para adicionar / editar item.
     * Retorna true se o usuário confirmou e os dados foram validados.
     */
    private boolean mostrarDialogoItem(ItemEncomendaPadrao item, boolean novo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(novo ? "Adicionar item padrão" : "Editar item padrão");
        dialog.setHeaderText(null);

        if (rootPane != null && rootPane.getScene() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }

        Label lblDesc = new Label("Descrição:");
        Label lblUnid = new Label("Unidade:");
        Label lblPreco = new Label("Preço padrão (R$):");

        TextField txtDesc = new TextField(item != null ? safeTrim(item.getNomeItem()) : "");
        TextField txtUnid = new TextField(item != null ? safeTrim(item.getUnidadeMedida()) : "");

        String precoInicial = "";
        if (item != null && item.getPrecoUnit() != null) {
            precoInicial = formatarMoeda(item.getPrecoUnit());
        }
        TextField txtPreco = new TextField(precoInicial);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(lblDesc, 0, 0);
        grid.add(txtDesc, 1, 0);
        grid.add(lblUnid, 0, 1);
        grid.add(txtUnid, 1, 1);
        grid.add(lblPreco, 0, 2);
        grid.add(txtPreco, 1, 2);

        GridPane.setHgrow(txtDesc, Priority.ALWAYS);
        GridPane.setHgrow(txtUnid, Priority.ALWAYS);
        GridPane.setHgrow(txtPreco, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String descricao = txtDesc.getText() == null ? "" : txtDesc.getText().trim();
                String unidade = txtUnid.getText() == null ? "" : txtUnid.getText().trim();
                BigDecimal preco = parsePrecoDigitado(txtPreco.getText());

                if (descricao.isEmpty() || unidade.isEmpty()) {
                    mostrarErro("Dados obrigatórios", "Preencha descrição e unidade.");
                    return false;
                }

                item.setNomeItem(descricao);
                item.setUnidadeMedida(unidade);
                item.setPrecoUnit(preco);
                return true;

            } catch (NumberFormatException ex) {
                mostrarErro("Valor inválido",
                        "Não foi possível entender o valor informado para o preço. Use algo como 10,00.");
                return false;
            }
        }
        return false;
    }

    // ============================================================
    //  BOTÕES (Adicionar / Editar / Excluir)
    // ============================================================

    @FXML
    private void handleAdicionar() {
        try {
            ItemEncomendaPadrao novo = new ItemEncomendaPadrao();
            boolean confirmado = mostrarDialogoItem(novo, true);
            if (!confirmado) {
                return;
            }

            if (itemDAO.inserir(novo)) {
                masterData.add(novo);
                tablePrecos.sort();
            } else {
                mostrarErro("Adicionar item", "Não foi possível salvar o item no banco de dados.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarErro("Adicionar item",
                    "Ocorreu um erro ao adicionar o item.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleEditar() {
        ItemEncomendaPadrao selecionado = tablePrecos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Editar item");
            alert.setHeaderText(null);
            alert.setContentText("Selecione um item na tabela para editar.");
            alert.showAndWait();
            return;
        }

        try {
            boolean confirmado = mostrarDialogoItem(selecionado, false);
            if (confirmado) {
                if (selecionado.getId() != null) {
                    itemDAO.atualizar(selecionado);
                }
                tablePrecos.refresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarErro("Editar item",
                    "Ocorreu um erro ao editar o item.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleExcluir() {
        ItemEncomendaPadrao selecionado = tablePrecos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Excluir item");
            alert.setHeaderText(null);
            alert.setContentText("Selecione um item na tabela para excluir.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir item");
        confirm.setHeaderText(null);
        confirm.setContentText("Tem certeza que deseja excluir o item selecionado?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (selecionado.getId() != null) {
                    itemDAO.excluir(selecionado.getId());
                }
                masterData.remove(selecionado);
                tablePrecos.getSelectionModel().clearSelection();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarErro("Excluir item",
                        "Ocorreu um erro ao excluir o item.\n" + e.getMessage());
            }
        }
    }

    // ============================================================
    //  IMPRESSÃO - TABELA DE PREÇOS
    // ============================================================

    @FXML
    private void handleImprimirTabela() {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job == null) {
                mostrarErro("Impressão", "Não foi possível criar o trabalho de impressão.");
                return;
            }

            Window owner = (rootPane != null && rootPane.getScene() != null)
                    ? rootPane.getScene().getWindow()
                    : null;

            // Abre diálogo de impressão
            if (owner != null && !job.showPrintDialog(owner)) {
                job.cancelJob();
                return;
            }

            // Layout de página escolhido pelo usuário (tamanho, margens etc.)
            PageLayout pageLayout = job.getJobSettings().getPageLayout();
            double printableWidth = pageLayout.getPrintableWidth();
            double printableHeight = pageLayout.getPrintableHeight();

            // Dados que realmente estão aparecendo na tela (já filtrados/ordenados)
            ObservableList<ItemEncomendaPadrao> dadosParaImpressao =
                    FXCollections.observableArrayList(tablePrecos.getItems());

            if (dadosParaImpressao.isEmpty()) {
                mostrarErro("Impressão", "Não há itens na tabela para imprimir.");
                job.cancelJob();
                return;
            }

            // ---------- ROOT DE IMPRESSÃO ----------
            VBox printRoot = new VBox(10);
            printRoot.setPadding(new Insets(30));
            printRoot.setFillWidth(true);
            printRoot.setPrefSize(printableWidth, printableHeight);
            printRoot.setMaxSize(printableWidth, printableHeight);
            printRoot.setStyle("-fx-background-color: white;");   // <<< FUNDO BRANCO

            // ---------- CABEÇALHO ----------
            CompanyDataLoader cdl = new CompanyDataLoader();
            VBox headerBox = PrintLayoutHelper.criarHeaderEmpresaA4(
                    cdl.getNomeEmpresa(), cdl.getCnpj(), cdl.getEndereco(), cdl.getCaminhoLogo());
            headerBox.setAlignment(Pos.TOP_LEFT);

            Label lblTitulo = PrintLayoutHelper.criarTitulo("Tabela de Preços de Encomenda");
            lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F2620;");
            headerBox.getChildren().add(lblTitulo);

            Separator sep = new Separator();
            sep.setPadding(new Insets(10, 0, 10, 0));

            // ---------- GRID COMO "TABELA" ----------
            GridPane grid = new GridPane();
            grid.setHgap(4);
            grid.setVgap(2);
            grid.setPadding(new Insets(5, 0, 5, 0));
            grid.setMaxWidth(Double.MAX_VALUE);

            // colunas ocupando 100% da largura disponível
            ColumnConstraints colDesc = new ColumnConstraints();
            colDesc.setPercentWidth(60);
            colDesc.setHgrow(Priority.ALWAYS);

            ColumnConstraints colUnid = new ColumnConstraints();
            colUnid.setPercentWidth(10);

            ColumnConstraints colPreco = new ColumnConstraints();
            colPreco.setPercentWidth(30);
            colPreco.setHgrow(Priority.ALWAYS);

            grid.getColumnConstraints().addAll(colDesc, colUnid, colPreco);

            // Cabeçalho da tabela
            Label hDesc = criarCelulaCabecalho("Descrição", Pos.CENTER_LEFT);
            Label hUnid = criarCelulaCabecalho("Unidade", Pos.CENTER);
            Label hPreco = criarCelulaCabecalho("Preço Padrão (R$)", Pos.CENTER_RIGHT);

            grid.add(hDesc, 0, 0);
            grid.add(hUnid, 1, 0);
            grid.add(hPreco, 2, 0);

            // Linhas de dados
            int row = 1;
            boolean linhaClara = true;

            for (ItemEncomendaPadrao item : dadosParaImpressao) {
                String desc = safeTrim(item.getNomeItem());
                String unid = safeTrim(item.getUnidadeMedida());
                String preco = formatarMoeda(item.getPrecoUnit());

                Label lDesc = criarCelulaCorpo(desc, Pos.CENTER_LEFT, linhaClara);
                Label lUnid = criarCelulaCorpo(unid, Pos.CENTER, linhaClara);
                Label lPreco = criarCelulaCorpo(preco, Pos.CENTER_RIGHT, linhaClara);

                grid.add(lDesc, 0, row);
                grid.add(lUnid, 1, row);
                grid.add(lPreco, 2, row);

                row++;
                linhaClara = !linhaClara; // segue alternando, mas sem mudar fundo
            }

            // Rodapé com data/hora
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Label lblRodapeData = new Label("Impresso em: " + LocalDateTime.now().format(fmt));
            lblRodapeData.setStyle("-fx-font-size: 10px;");
            lblRodapeData.setMaxWidth(Double.MAX_VALUE);
            lblRodapeData.setAlignment(Pos.CENTER_RIGHT);

            VBox.setVgrow(grid, Priority.ALWAYS);

            printRoot.getChildren().addAll(headerBox, sep, grid, lblRodapeData);

            // Garante que o layout seja calculado antes de imprimir
            Scene scene = new Scene(printRoot, printableWidth, printableHeight);
            scene.setFill(Color.WHITE);                     // <<< CENA TAMBÉM BRANCA
            printRoot.applyCss();
            printRoot.layout();

            boolean success = job.printPage(pageLayout, printRoot);
            if (success) {
                job.endJob();
            } else {
                job.cancelJob();
                mostrarErro("Impressão", "Falha ao enviar a página para a impressora.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarErro("Impressão",
                    "Ocorreu um erro ao tentar imprimir a tabela de preços:\n" + e.getMessage());
        }
    }

    // ============================================================
    //  HELPERS PARA CÉLULAS DO GRID (IMPRESSÃO)
    // ============================================================

    private Label criarCelulaCabecalho(String texto, Pos alinhamento) {
        Label lbl = new Label(texto);
        lbl.setAlignment(alinhamento);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #059669;" +
                "-fx-padding: 4 6 4 6;");
        return lbl;
    }

    private Label criarCelulaCorpo(String texto, Pos alinhamento, boolean linhaClara) {
        Label lbl = new Label(texto);
        lbl.setAlignment(alinhamento);
        lbl.setMaxWidth(Double.MAX_VALUE);
        // fundo branco puro
        lbl.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #333333;" +
                "-fx-padding: 3 6 3 6;");
        return lbl;
    }
}
