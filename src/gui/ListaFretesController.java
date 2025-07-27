package gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import dao.ConexaoBD;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller da tela ListaFretes.fxml.
 * Gerencia:
 *  - Exibição de todos os fretes cadastrados
 *  - Filtros (número, remetente, cliente, rota, status)
 *  - Totais no rodapé
 *  - Duplo-clique para editar (abre CadastroFrete.fxml em modal)
 */
public class ListaFretesController {

    // =====================================================
    // 1) INJEÇÕES DO FXML
    // =====================================================
    @FXML private TableView<FreteView> tabelaFretes;
    @FXML private TableColumn<FreteView, String> colNumFrete;
    @FXML private TableColumn<FreteView, String> colRemetente;
    @FXML private TableColumn<FreteView, String> colDestinatario;
    @FXML private TableColumn<FreteView, String> colViagem;
    @FXML private TableColumn<FreteView, String> colEmissao;
    @FXML private TableColumn<FreteView, String> colNominal;
    @FXML private TableColumn<FreteView, String> colDevedor;
    @FXML private TableColumn<FreteView, String> colBaixado;
    @FXML private TableColumn<FreteView, String> colConferente;

    @FXML private Label lblTotalLancado;
    @FXML private Label lblLancamentos;
    @FXML private Label lblTotalRecebido;
    @FXML private Label lblTotalAReceber;
    @FXML private Label lblTotalVolumes;
    @FXML private Label lblDescontos;
    @FXML private Label lblTotalFaturado;

    @FXML private RadioButton rbQuitados;
    @FXML private RadioButton rbAReceber;
    @FXML private RadioButton rbCancelado;
    @FXML private RadioButton rbTodos;
    @FXML private ToggleGroup statusPagamentoToggleGroup;

    @FXML private ComboBox<String> cbRotaFiltro;
    @FXML private TextField txtNumFreteFiltro;
    @FXML private ComboBox<String> cbRemetenteFiltro;
    @FXML private ComboBox<String> cbClienteFiltro;

    @FXML private Button btnFretesAReceber;
    @FXML private Button btnAtualizarLista;


    // =====================================================
    // 2) VARIÁVEIS AUXILIARES
    // =====================================================
    // Lista completa de fretes (antes de aplicar qualquer filtro)
    private final ObservableList<FreteView> listaCompletaFretes = FXCollections.observableArrayList();
    // Lista que efetivamente aparece na TableView (após filtros)
    private final ObservableList<FreteView> listaFretesVisivel = FXCollections.observableArrayList();

    // Para preencher ComboBoxes de filtro (listas originais, sem filtro)
    private final ObservableList<String> listaNomesRemetentesOriginal = FXCollections.observableArrayList();
    private final ObservableList<String> listaNomesClientesOriginal   = FXCollections.observableArrayList();
    private final ObservableList<String> listaNomesRotasOriginal      = FXCollections.observableArrayList();

    // Formatadores
    private final DecimalFormat df = new DecimalFormat("R$ #,##0.00",
            new DecimalFormatSymbols(new Locale("pt","BR")));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    // Quando o usuário dá duplo clique em um frete, guardamos aqui
    // para repassar ao CadastroFreteController (edição).
    private static String staticNumeroFreteParaAbrir = null;

    /**
     * Chamado de fora (por exemplo, TelaPrincipalController)
     * para dizer que, quando esta tela for exibida, já abra o frete
     * de número passado em edição.
     */
    public static void setNumeroFreteParaAbrir(String numFrete) {
        staticNumeroFreteParaAbrir = numFrete;
    }


    // =====================================================
    // 3) MÉTODO initialize() INVOCADO PELO FXML
    // =====================================================
    @FXML
    public void initialize() {
        // 1) Configura colunas da tabela
        configurarColunasTabela();
        if (tabelaFretes != null) {
            tabelaFretes.setItems(listaFretesVisivel);
        }

        // 2) Preenche combo boxes de filtros (remetentes, clientes, rotas)
        carregarDadosParaFiltros();

        // 3) Configura listeners (filtros, botões, duplo clique)
        configurarListeners();

        // 4) Carrega todos os fretes do banco (popula listaCompletaFretes e aplica filtro inicial)
        carregarTodosFretesDoBanco();

        // 5) Se alguém já havia definido staticNumeroFreteParaAbrir antes de abrir esta tela,
        //    então filtramos por aquele número e selecionamos na tabela.
        if (staticNumeroFreteParaAbrir != null && !staticNumeroFreteParaAbrir.isEmpty()) {
            if (txtNumFreteFiltro != null) {
                txtNumFreteFiltro.setText(staticNumeroFreteParaAbrir);
                filtrarLista();
                // Selecionar visualmente na tabela
                for (FreteView fv : listaFretesVisivel) {
                    if (fv.getNumFrete().equalsIgnoreCase(staticNumeroFreteParaAbrir)) {
                        tabelaFretes.getSelectionModel().select(fv);
                        tabelaFretes.scrollTo(fv);
                        break;
                    }
                }
            }
            // Zera para não reaplicar sempre que reentrar
            staticNumeroFreteParaAbrir = null;
        }
    }


    // =====================================================
    // 4) CONFIGURAÇÃO DAS COLUNAS DA TABLEVIEW
    // =====================================================
    private void configurarColunasTabela() {
        if (colNumFrete     != null) colNumFrete.setCellValueFactory(new PropertyValueFactory<>("numFrete"));
        if (colRemetente    != null) colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        if (colDestinatario != null) colDestinatario.setCellValueFactory(new PropertyValueFactory<>("destinatario"));
        if (colViagem       != null) colViagem.setCellValueFactory(new PropertyValueFactory<>("viagem"));
        if (colEmissao      != null) colEmissao.setCellValueFactory(new PropertyValueFactory<>("emissao"));
        if (colNominal      != null) colNominal.setCellValueFactory(new PropertyValueFactory<>("nominal"));
        if (colDevedor      != null) colDevedor.setCellValueFactory(new PropertyValueFactory<>("devedor"));
        if (colBaixado      != null) colBaixado.setCellValueFactory(new PropertyValueFactory<>("baixado"));
        if (colConferente   != null) colConferente.setCellValueFactory(new PropertyValueFactory<>("conferente"));
    }


    // =====================================================
    // 5) CONFIGURAÇÃO DE LISTENERS (FILTROS e DUPOLO CLIQUE)
    // =====================================================
    private void configurarListeners() {
        // -------------------------------------------------
        // a) Duplo clique em uma linha da TableView:
        //    abre CadastroFrete.fxml em modal, para edição daquele número.
        // -------------------------------------------------
        if (tabelaFretes != null) {
            tabelaFretes.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    FreteView sel = tabelaFretes.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        // 1) Informa ao CadastroFreteController qual número editar
                        CadastroFreteController.setNumeroFreteParaAbrir(sel.getNumFrete());
                        // 2) Carrega o FXML e abre em WINDOW_MODAL
                        abrirCadastroFreteEmModal("/gui/CadastroFrete.fxml",
                                "Edição do Frete: " + sel.getNumFrete());
                        // 3) Quando fechar a janela de edição, recarrega a lista:
                        carregarTodosFretesDoBanco();
                    }
                }
            });
        }

        // -------------------------------------------------
        // b) Botão “Fretes a Receber”:
        //    seleciona o RadioButton “A Receber” e filtra.
        // -------------------------------------------------
        if (btnFretesAReceber != null && rbAReceber != null) {
            btnFretesAReceber.setOnAction(e -> {
                if (statusPagamentoToggleGroup != null) {
                    statusPagamentoToggleGroup.selectToggle(rbAReceber);
                } else {
                    rbAReceber.setSelected(true);
                }
                filtrarLista();
            });
        }

        // -------------------------------------------------
        // c) Sempre que qualquer campo de filtro mudar, chamamos filtrarLista()
        // -------------------------------------------------
        if (txtNumFreteFiltro != null) {
            txtNumFreteFiltro.textProperty().addListener((obs, oldV, newV) -> filtrarLista());
        }

        if (cbRemetenteFiltro != null) {
            // Se o ComboBox for editável:
            if (cbRemetenteFiltro.getEditor() != null) {
                cbRemetenteFiltro.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                    filterComboBoxItems(cbRemetenteFiltro, listaNomesRemetentesOriginal, newText);
                    filtrarLista();
                });
            }
            cbRemetenteFiltro.valueProperty().addListener((obs, oldV, newV) -> filtrarLista());
        }

        if (cbClienteFiltro != null) {
            if (cbClienteFiltro.getEditor() != null) {
                cbClienteFiltro.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                    filterComboBoxItems(cbClienteFiltro, listaNomesClientesOriginal, newText);
                    filtrarLista();
                });
            }
            cbClienteFiltro.valueProperty().addListener((obs, oldV, newV) -> filtrarLista());
        }

        if (cbRotaFiltro != null) {
            cbRotaFiltro.valueProperty().addListener((obs, oldV, newV) -> filtrarLista());
        }

        if (rbQuitados   != null) rbQuitados.setOnAction(e -> filtrarLista());
        if (rbAReceber   != null) rbAReceber.setOnAction(e -> filtrarLista());
        if (rbCancelado  != null) rbCancelado.setOnAction(e -> filtrarLista());
        if (rbTodos      != null) rbTodos.setOnAction(e -> filtrarLista());

        if (btnAtualizarLista != null) {
            btnAtualizarLista.setOnAction(e -> carregarTodosFretesDoBanco());
        }
    }

    /**
     * Atualiza as opções visíveis do ComboBox com base em um texto de filtro,
     * mas mantém intacta a lista original (sem filtro) para pesquisas futuras.
     */
    private void filterComboBoxItems(ComboBox<String> comboBox,
                                     ObservableList<String> originalList,
                                     String filterText) {
        if (comboBox == null || originalList == null) return;

        ObservableList<String> itemsToShow;
        if (filterText == null || filterText.isEmpty()) {
            itemsToShow = originalList;
        } else {
            String lowerCaseFilter = filterText.toLowerCase();
            itemsToShow = originalList.filtered(s -> s != null && s.toLowerCase().contains(lowerCaseFilter));
        }

        if (!comboBox.getItems().equals(itemsToShow)) {
            comboBox.setItems(itemsToShow);
        }

        // Se o editor ou ComboBox estiver em foco, exibe a lista filtrada
        if ((comboBox.isFocused() || (comboBox.getEditor() != null && comboBox.getEditor().isFocused()))
                && !itemsToShow.isEmpty()) {
            comboBox.show();
        } else {
            comboBox.hide();
        }
    }


    // =====================================================
    // 6) CARREGAMENTO DOS DADOS (BANCO DE DADOS)
    // =====================================================
    /**
     * Carrega: remetentes, clientes e rotas (distintos) para preencher
     * os ComboBoxes de filtro. Não altera a tabela de fretes em si.
     */
    private void carregarDadosParaFiltros() {
        carregarRemetentesDistintos();
        carregarClientesDistintos();
        carregarRotasParaFiltro();

        if (cbRemetenteFiltro != null) {
            cbRemetenteFiltro.setItems(listaNomesRemetentesOriginal);
            cbRemetenteFiltro.setEditable(true);
        }
        if (cbClienteFiltro != null) {
            cbClienteFiltro.setItems(listaNomesClientesOriginal);
            cbClienteFiltro.setEditable(true);
        }
        if (cbRotaFiltro != null) {
            cbRotaFiltro.setItems(listaNomesRotasOriginal);
        }
    }

    private void carregarRemetentesDistintos() {
        listaNomesRemetentesOriginal.clear();
        listaNomesRemetentesOriginal.add(""); // opçãovazia
        String sql = "SELECT DISTINCT remetente_nome_temp FROM fretes " +
                     "WHERE remetente_nome_temp IS NOT NULL AND remetente_nome_temp <> '' " +
                     "ORDER BY remetente_nome_temp";
        try (Connection c = ConexaoBD.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                listaNomesRemetentesOriginal.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar remetentes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void carregarClientesDistintos() {
        listaNomesClientesOriginal.clear();
        listaNomesClientesOriginal.add("");
        String sql = "SELECT DISTINCT destinatario_nome_temp FROM fretes " +
                     "WHERE destinatario_nome_temp IS NOT NULL AND destinatario_nome_temp <> '' " +
                     "ORDER BY destinatario_nome_temp";
        try (Connection c = ConexaoBD.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                listaNomesClientesOriginal.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void carregarRotasParaFiltro() {
        listaNomesRotasOriginal.clear();
        listaNomesRotasOriginal.add("");

        String sql = "SELECT DISTINCT rota_temp FROM fretes " +
                     "WHERE rota_temp IS NOT NULL AND rota_temp <> '' ORDER BY rota_temp";
        try (Connection c = ConexaoBD.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String rotaTemp = rs.getString(1);
                if (!listaNomesRotasOriginal.contains(rotaTemp)) {
                    listaNomesRotasOriginal.add(rotaTemp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar rotas para filtro: " + e.getMessage());
            e.printStackTrace();
        }

        // Ordena (exceto o primeiro elemento vazio)
        if (listaNomesRotasOriginal.size() > 1) {
            List<String> sortedList = listaNomesRotasOriginal.subList(1, listaNomesRotasOriginal.size())
                                   .stream()
                                   .sorted()
                                   .collect(Collectors.toList());
            listaNomesRotasOriginal.remove(1, listaNomesRotasOriginal.size());
            listaNomesRotasOriginal.addAll(sortedList);
        }
    }

    /**
     * Carrega todos os fretes do banco, preenche listaCompletaFretes e
     * em seguida chama filtrarLista() para aplicar filtros iniciais.
     */
    private void carregarTodosFretesDoBanco() {
        listaCompletaFretes.clear();

        String sql = "SELECT f.id_frete, f.numero_frete, f.remetente_nome_temp AS remetente_nome, " +
                     "f.destinatario_nome_temp AS destinatario_nome, f.rota_temp AS rota, " +
                     "f.data_emissao, f.valor_total_itens AS valor_nominal, f.valor_devedor, f.valor_pago, " +
                     "f.conferente_temp AS conferente, f.status_frete " +
                     "FROM fretes f " +
                     "ORDER BY f.id_frete DESC";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql))
        {
            while (rs.next()) {
                String numeroFrete      = rs.getString("numero_frete");
                String remetenteNome    = rs.getString("remetente_nome");
                String destinatarioNome = rs.getString("destinatario_nome");
                String rotaTemp         = rs.getString("rota");
                java.sql.Date dataEmisDb = rs.getDate("data_emissao");
                String dataEmissaoStr   = (dataEmisDb != null)
                        ? dataEmisDb.toLocalDate().format(dateFormatter)
                        : "";

                String valorNominalStr  = df.format(rs.getDouble("valor_nominal"));
                String valorDevedorStr  = df.format(rs.getDouble("valor_devedor"));
                String valorPagoStr     = df.format(rs.getDouble("valor_pago"));
                String conferenteTemp   = rs.getString("conferente");
                String statusFrete      = rs.getString("status_frete");

                int totalVolumesDoFrete = 0; // Se quiser somar volumes, implemente aqui.

                FreteView fv = new FreteView(
                        (numeroFrete != null) ? numeroFrete : String.valueOf(rs.getLong("id_frete")),
                        (remetenteNome != null) ? remetenteNome : "",
                        (destinatarioNome != null) ? destinatarioNome : "",
                        (rotaTemp != null) ? rotaTemp : "",
                        dataEmissaoStr,
                        valorNominalStr,
                        valorDevedorStr,
                        valorPagoStr,
                        (conferenteTemp != null) ? conferenteTemp : "",
                        (statusFrete != null) ? statusFrete : "PENDENTE",
                        totalVolumesDoFrete
                );
                listaCompletaFretes.add(fv);
            }
        } catch (SQLException e) {
            System.err.println("Erro SQL ao carregar fretes: " + e.getMessage());
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro de Banco", "Falha ao carregar lista de fretes.");
        }
        filtrarLista();
    }


    // =====================================================
    // 7) FILTRAGEM E ATUALIZAÇÃO DO RODAPÉ
    // =====================================================
    /**
     * Aplica todos os filtros (número, remetente, cliente, rota, status) sobre
     * listaCompletaFretes e popula listaFretesVisivel. Em seguida, recalcula os totais.
     */
    private void filtrarLista() {
        final String numFiltroFinal = (txtNumFreteFiltro != null && txtNumFreteFiltro.getText() != null)
                ? txtNumFreteFiltro.getText().trim().toLowerCase()
                : "";

        String tempRemFiltro = "";
        if (cbRemetenteFiltro != null) {
            if (cbRemetenteFiltro.getEditor() != null &&
                cbRemetenteFiltro.getEditor().getText() != null &&
                !cbRemetenteFiltro.getEditor().getText().trim().isEmpty()) {
                tempRemFiltro = cbRemetenteFiltro.getEditor().getText().trim().toLowerCase();
            } else if (cbRemetenteFiltro.getValue() != null &&
                       !cbRemetenteFiltro.getValue().isEmpty()) {
                tempRemFiltro = cbRemetenteFiltro.getValue().trim().toLowerCase();
            }
        }
        final String remFiltroFinal = tempRemFiltro;

        String tempCliFiltro = "";
        if (cbClienteFiltro != null) {
            if (cbClienteFiltro.getEditor() != null &&
                cbClienteFiltro.getEditor().getText() != null &&
                !cbClienteFiltro.getEditor().getText().trim().isEmpty()) {
                tempCliFiltro = cbClienteFiltro.getEditor().getText().trim().toLowerCase();
            } else if (cbClienteFiltro.getValue() != null &&
                       !cbClienteFiltro.getValue().isEmpty()) {
                tempCliFiltro = cbClienteFiltro.getValue().trim().toLowerCase();
            }
        }
        final String cliFiltroFinal = tempCliFiltro;

        final String rotaFiltroFinal = (cbRotaFiltro != null &&
                                        cbRotaFiltro.getValue() != null &&
                                        !cbRotaFiltro.getValue().trim().isEmpty())
                                      ? cbRotaFiltro.getValue().trim().toLowerCase()
                                      : "";

        final boolean verQuitadosFinal  = rbQuitados   != null && rbQuitados.isSelected();
        final boolean verAReceberFinal  = rbAReceber   != null && rbAReceber.isSelected();
        final boolean verCanceladoFinal = rbCancelado  != null && rbCancelado.isSelected();
        final boolean verTodosFinal     = rbTodos      != null && rbTodos.isSelected();

        ObservableList<FreteView> filtrada = listaCompletaFretes.stream()
            .filter(f -> {
                boolean match = true;
                if (!numFiltroFinal.isEmpty() && (f.getNumFrete() == null || !f.getNumFrete().toLowerCase().contains(numFiltroFinal))) {
                    match = false;
                }
                if (match && !remFiltroFinal.isEmpty() && (f.getRemetente() == null || !f.getRemetente().toLowerCase().contains(remFiltroFinal))) {
                    match = false;
                }
                if (match && !cliFiltroFinal.isEmpty() && (f.getDestinatario() == null || !f.getDestinatario().toLowerCase().contains(cliFiltroFinal))) {
                    match = false;
                }
                if (match && !rotaFiltroFinal.isEmpty() && (f.getViagem() == null || !f.getViagem().toLowerCase().contains(rotaFiltroFinal))) {
                    match = false;
                }

                if (match && !verTodosFinal) {
                    double devedorDouble = parseDoubleFromMonetaryString(f.getDevedor());
                    double nominalDouble = parseDoubleFromMonetaryString(f.getNominal());
                    String statusFrete   = (f.getStatus() != null) ? f.getStatus().trim().toUpperCase() : "PENDENTE";

                    if (verQuitadosFinal) {
                        // quitado = valor_devedor == 0 && status != "CANCELADO"
                        if (Math.abs(devedorDouble) > 0.009 || statusFrete.equals("CANCELADO") || Math.abs(nominalDouble) < 0.009) {
                            match = false;
                        }
                    } else if (verAReceberFinal) {
                        // a receber = valor_devedor > 0 && status != "CANCELADO"
                        if (!(devedorDouble > 0.009) || statusFrete.equals("CANCELADO")) {
                            match = false;
                        }
                    } else if (verCanceladoFinal) {
                        // cancelado = status == "CANCELADO"
                        if (!statusFrete.equals("CANCELADO")) {
                            match = false;
                        }
                    }
                }
                return match;
            })
            .collect(Collectors.toCollection(FXCollections::observableArrayList));

        listaFretesVisivel.setAll(filtrada);
        recalcularRodape();
    }

    /**
     * Recalcula e exibe os totais (rodapé), baseado em listaFretesVisivel.
     */
    private void recalcularRodape() {
        double totalLancadoCalc   = 0;
        double totalRecebidoCalc  = 0;
        double totalAReceberCalc  = 0;
        int quantidadeLancamentos = 0;
        int totalVolumesCalc      = 0;

        for (FreteView f : listaFretesVisivel) {
            String statusFrete = f.getStatus() != null ? f.getStatus().trim().toUpperCase() : "PENDENTE";
            if (!statusFrete.equals("CANCELADO")) {
                quantidadeLancamentos++;
                totalLancadoCalc  += parseDoubleFromMonetaryString(f.getNominal());
                totalRecebidoCalc += parseDoubleFromMonetaryString(f.getBaixado());
                totalAReceberCalc += parseDoubleFromMonetaryString(f.getDevedor());
                totalVolumesCalc  += f.getTotalVolumes();
            }
        }

        if (lblTotalLancado    != null) lblTotalLancado.setText(df.format(totalLancadoCalc));
        if (lblLancamentos     != null) lblLancamentos.setText(String.valueOf(quantidadeLancamentos));
        if (lblTotalRecebido   != null) lblTotalRecebido.setText(df.format(totalRecebidoCalc));
        if (lblTotalAReceber   != null) lblTotalAReceber.setText(df.format(totalAReceberCalc));
        if (lblTotalVolumes    != null) lblTotalVolumes.setText(String.valueOf(totalVolumesCalc));
        if (lblTotalFaturado   != null) lblTotalFaturado.setText(df.format(totalLancadoCalc));
        if (lblDescontos       != null) lblDescontos.setText(df.format(0.0));
    }

    private double parseDoubleFromMonetaryString(String monetaryString) {
        if (monetaryString == null || monetaryString.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleaned = monetaryString.replace("R$", "").trim();
            if (cleaned.contains(",")) {
                cleaned = cleaned.replace(".", "");
                cleaned = cleaned.replace(",", ".");
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao parsear valor monetário para double: '" + monetaryString + "' - " + e.getMessage());
            return 0.0;
        }
    }


    // =====================================================
    // 8) ABRIR CadastroFrete.fxml EM MODO MODAL
    // =====================================================
    private void abrirCadastroFreteEmModal(String fxmlPathRelative, String title) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPathRelative);
            if (fxmlLocation == null) {
                showAlert(AlertType.ERROR, "Erro ao Abrir Tela",
                          "Arquivo FXML não pôde ser localizado: " + fxmlPathRelative);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);

            // Se tiver CSS global (por ex. /css/main.css), adicione aqui:
            URL cssLocation = getClass().getResource("/css/main.css");
            if (cssLocation != null) {
                stage.getScene().getStylesheets().add(cssLocation.toExternalForm());
            }

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Abrir Tela",
                      "Não foi possível carregar a tela: " + fxmlPathRelative + "\nDetalhes: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Crítico",
                      "Ocorreu um erro inesperado ao tentar abrir a tela '" + title + "'.");
        }
    }


    // =====================================================
    // 9) HELPER PARA EXIBIR ALERTS
    // =====================================================
    private void showAlert(Alert.AlertType aT, String t, String m) {
        Alert a = new Alert(aT);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }


    // =====================================================
    // 10) CLASSE INTERNA FreteView (modelo de cada linha da tabela)
    // =====================================================
    public static class FreteView {
        private final SimpleStringProperty numFrete;
        private final SimpleStringProperty remetente;
        private final SimpleStringProperty destinatario;
        private final SimpleStringProperty viagem;
        private final SimpleStringProperty emissao;
        private final SimpleStringProperty nominal;
        private final SimpleStringProperty devedor;
        private final SimpleStringProperty baixado;
        private final SimpleStringProperty conferente;
        private final SimpleStringProperty status;
        private final SimpleIntegerProperty totalVolumes;

        public FreteView(String nf, String rem, String des, String viaj,
                         String emi, String nom, String dev,
                         String bai, String conf, String stat, int volumes) {
            this.numFrete     = new SimpleStringProperty(nf);
            this.remetente    = new SimpleStringProperty(rem);
            this.destinatario = new SimpleStringProperty(des);
            this.viagem       = new SimpleStringProperty(viaj);
            this.emissao      = new SimpleStringProperty(emi);
            this.nominal      = new SimpleStringProperty(nom);
            this.devedor      = new SimpleStringProperty(dev);
            this.baixado      = new SimpleStringProperty(bai);
            this.conferente   = new SimpleStringProperty(conf);
            this.status       = new SimpleStringProperty(stat == null ? "PENDENTE" : stat);
            this.totalVolumes = new SimpleIntegerProperty(volumes);
        }

        public String getNumFrete()       { return numFrete.get(); }
        public String getRemetente()      { return remetente.get(); }
        public String getDestinatario()   { return destinatario.get(); }
        public String getViagem()         { return viagem.get(); }
        public String getEmissao()        { return emissao.get(); }
        public String getNominal()        { return nominal.get(); }
        public String getDevedor()        { return devedor.get(); }
        public String getBaixado()        { return baixado.get(); }
        public String getConferente()     { return conferente.get(); }
        public String getStatus()         { return status.get(); }
        public int    getTotalVolumes()   { return totalVolumes.get(); }

        public SimpleStringProperty numFreteProperty()      { return numFrete; }
        public SimpleStringProperty remetenteProperty()     { return remetente; }
        public SimpleStringProperty destinatarioProperty()  { return destinatario; }
        public SimpleStringProperty viagemProperty()        { return viagem; }
        public SimpleStringProperty emissaoProperty()       { return emissao; }
        public SimpleStringProperty nominalProperty()       { return nominal; }
        public SimpleStringProperty devedorProperty()       { return devedor; }
        public SimpleStringProperty baixadoProperty()       { return baixado; }
        public SimpleStringProperty conferenteProperty()    { return conferente; }
        public SimpleStringProperty statusProperty()        { return status; }
        public SimpleIntegerProperty totalVolumesProperty() { return totalVolumes; }
    }
}
