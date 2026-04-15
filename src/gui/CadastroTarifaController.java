package gui; // CORRIGIDO: O pacote agora é 'gui'

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.Tarifa;
import model.Rota;
import dao.TarifaDAO;
import dao.AuxiliaresDAO; // Importado corretamente do pacote dao
import dao.RotaDAO;
import dao.ConexaoBD; // Importado ConexaoBD, caso ainda fosse usado diretamente

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.sql.SQLException;
import java.util.Objects;
import javafx.application.Platform;
import gui.util.AlertHelper;
import util.AppLogger;

public class CadastroTarifaController implements Initializable {

    // Campos @FXML
    @FXML private TableView<Tarifa> tableTarifas;
    @FXML private TableColumn<Tarifa, Integer> colId;
    @FXML private TableColumn<Tarifa, String> colRota;
    @FXML private TableColumn<Tarifa, String> colTipoPassageiro;
    @FXML private TableColumn<Tarifa, BigDecimal> colTransporte;
    @FXML private TableColumn<Tarifa, BigDecimal> colCargas;
    @FXML private TableColumn<Tarifa, BigDecimal> colAlimentacao;
    @FXML private TableColumn<Tarifa, BigDecimal> colDesconto;

    @FXML private TextField txtId;
    @FXML private ComboBox<Rota> cmbRota;
    @FXML private ComboBox<String> cmbTipoPassageiro;

    @FXML private TextField txtTransporte;
    @FXML private TextField txtCargas;
    @FXML private TextField txtAlimentacao;
    @FXML private TextField txtDesconto;

    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnFechar;

    private final TarifaDAO tarifaDAO = new TarifaDAO();
    private final AuxiliaresDAO auxDao = new AuxiliaresDAO(); // Inicializado
    private final RotaDAO rotaDAO = new RotaDAO();

    private ObservableList<Tarifa> observableListTarifas = FXCollections.observableArrayList();
    private ObservableList<String> observableListTiposPassageiro = FXCollections.observableArrayList();
    private ObservableList<Rota> observableListRotas = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Tarifas"); return; }
        configurarTabela();

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<String> tiposPassageiroNomes = auxDao.listarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem");
                List<Rota> rotas = rotaDAO.listarTodasAsRotasComoObjects();
                List<Tarifa> tarifas = tarifaDAO.listarTodos();
                Platform.runLater(() -> {
                    if (tiposPassageiroNomes != null && !tiposPassageiroNomes.isEmpty()) {
                        observableListTiposPassageiro.setAll(tiposPassageiroNomes);
                        cmbTipoPassageiro.setItems(observableListTiposPassageiro);
                    }
                    if (rotas != null && !rotas.isEmpty()) {
                        observableListRotas.setAll(rotas);
                        cmbRota.setItems(observableListRotas);
                    }
                    observableListTarifas.setAll(tarifas);
                });
            } catch (Exception e) {
                AppLogger.warn("CadastroTarifaController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        tableTarifas.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    preencherCamposComTarifaSelecionada(newValue);
                } else {
                    limparCampos();
                }
            }
        );
    }

    private void configurarTabela() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRota.setCellValueFactory(new PropertyValueFactory<>("nomeRota"));
        colTipoPassageiro.setCellValueFactory(new PropertyValueFactory<>("nomeTipoPassageiro"));
        colTransporte.setCellValueFactory(new PropertyValueFactory<>("valorTransporte"));
        colCargas.setCellValueFactory(new PropertyValueFactory<>("valorCargas"));
        colAlimentacao.setCellValueFactory(new PropertyValueFactory<>("valorAlimentacao"));
        colDesconto.setCellValueFactory(new PropertyValueFactory<>("valorDesconto"));
        tableTarifas.setItems(observableListTarifas);
    }

    private void carregarDadosTabela() {
        try {
            List<Tarifa> tarifas = tarifaDAO.listarTodos();
            observableListTarifas.setAll(tarifas);
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro ao Carregar Tarifas", "Não foi possível carregar os dados das tarifas: " + e.getMessage());
            AppLogger.error("CadastroTarifaController", e.getMessage(), e);
        }
    }

    private void popularComboBoxes() {
        // Popular cmbTipoPassageiro
        try {
            List<String> tiposPassageiroNomes = auxDao.listarAuxiliar("aux_tipos_passagem", "nome_tipo_passagem");

            if (tiposPassageiroNomes != null && !tiposPassageiroNomes.isEmpty()) {
                observableListTiposPassageiro.setAll(tiposPassageiroNomes);
                cmbTipoPassageiro.setItems(observableListTiposPassageiro);
            } else {
                AlertHelper.show(AlertType.WARNING, "Aviso: Tipos de Passageiro", "Nenhum tipo de passageiro encontrado para carregar no ComboBox.");
            }
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro ao Carregar Tipos de Passageiro", "Falha: " + e.getMessage());
            AppLogger.error("CadastroTarifaController", e.getMessage(), e);
        }

        // Popular cmbRota com objetos Rota
        try {
            List<Rota> rotas = rotaDAO.listarTodasAsRotasComoObjects();
            if (rotas != null && !rotas.isEmpty()) {
                observableListRotas.setAll(rotas);
                cmbRota.setItems(observableListRotas);
            } else {
                AlertHelper.show(AlertType.WARNING, "Aviso: Rotas", "Nenhuma rota encontrada para carregar no ComboBox.");
            }
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro ao Carregar Rotas", "Falha: " + e.getMessage());
            AppLogger.error("CadastroTarifaController", e.getMessage(), e);
        }
    }

    private void preencherCamposComTarifaSelecionada(Tarifa tarifa) {
        txtId.setText(String.valueOf(tarifa.getId()));

        Rota rotaParaSelecionar = null;
        if (cmbRota != null && cmbRota.getItems() != null) {
            for (Rota r : cmbRota.getItems()) {
                if (Objects.equals(r.getId(), tarifa.getRotaId())) {
                    rotaParaSelecionar = r;
                    break;
                }
            }
        }
        cmbRota.setValue(rotaParaSelecionar);

        cmbTipoPassageiro.setValue(tarifa.getNomeTipoPassageiro());

        txtTransporte.setText(tarifa.getValorTransporte() != null ? tarifa.getValorTransporte().toPlainString() : "0.00");
        txtAlimentacao.setText(tarifa.getValorAlimentacao() != null ? tarifa.getValorAlimentacao().toPlainString() : "0.00");
        txtCargas.setText(tarifa.getValorCargas() != null ? tarifa.getValorCargas().toPlainString() : "0.00");
        txtDesconto.setText(tarifa.getValorDesconto() != null ? tarifa.getValorDesconto().toPlainString() : "0.00");
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        limparCampos();
        tableTarifas.getSelectionModel().clearSelection();
        if (cmbRota != null) cmbRota.requestFocus();
    }

    private void limparCampos() {
        txtId.clear();
        if (cmbRota != null) {
            cmbRota.getSelectionModel().clearSelection();
            cmbRota.setValue(null);
        }
        if (cmbTipoPassageiro != null) {
            cmbTipoPassageiro.getSelectionModel().clearSelection();
            cmbTipoPassageiro.setValue(null);
        }
        txtTransporte.setText("0.00");
        txtAlimentacao.setText("0.00");
        txtCargas.setText("0.00");
        txtDesconto.setText("0.00");
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        Rota rotaSelecionada = cmbRota.getValue();
        String tipoPassageiroSelecionadoStr = cmbTipoPassageiro.getValue();

        if (rotaSelecionada == null) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Por favor, selecione uma Rota.");
            if (cmbRota != null) cmbRota.requestFocus();
            return;
        }
        if (tipoPassageiroSelecionadoStr == null || tipoPassageiroSelecionadoStr.trim().isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "Por favor, selecione um Tipo de Passageiro.");
            if (cmbTipoPassageiro != null) cmbTipoPassageiro.requestFocus();
            return;
        }

        Long rotaId = rotaSelecionada.getId();
        Integer tipoPassageiroId = null;

        try {
            tipoPassageiroId = auxDao.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", tipoPassageiroSelecionadoStr);
            if (tipoPassageiroId == null || tipoPassageiroId == -1) {
                AlertHelper.show(AlertType.ERROR, "Erro de Seleção", "Tipo de Passageiro '" + tipoPassageiroSelecionadoStr + "' não encontrado ou ID não pôde ser obtido. Verifique o cadastro de Tipos de Passageiro.");
                return;
            }
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro ao Buscar ID", "Erro ao buscar ID para o tipo de passageiro '" + tipoPassageiroSelecionadoStr + "': " + e.getMessage());
            AppLogger.error("CadastroTarifaController", e.getMessage(), e);
            return;
        }

        Tarifa tarifa = new Tarifa();
        tarifa.setRotaId(rotaId);
        tarifa.setTipoPassageiroId(tipoPassageiroId);

        try {
            tarifa.setValorTransporte(parseBigDecimal(txtTransporte.getText()));
            tarifa.setValorAlimentacao(parseBigDecimal(txtAlimentacao.getText()));
            tarifa.setValorCargas(parseBigDecimal(txtCargas.getText()));
            tarifa.setValorDesconto(parseBigDecimal(txtDesconto.getText()));
        } catch (NumberFormatException e) {
            AlertHelper.show(AlertType.ERROR, "Erro de Formato", "Os valores de Transporte, Alimentação, Cargas e Desconto devem ser números válidos (ex: 123.45). Use '.' como separador decimal.");
            return;
        }

        boolean sucesso;
        String operacaoMensagem;

        try {
            if (txtId.getText() == null || txtId.getText().isEmpty()) {
                Tarifa existente = tarifaDAO.buscarTarifaPorRotaETipo(rotaId, tipoPassageiroId);
                if (existente != null) {
                    AlertHelper.show(AlertType.ERROR, "Tarifa Duplicada", "Já existe uma tarifa cadastrada para esta Rota e Tipo de Passageiro (ID da tarifa existente: " + existente.getId() + ").");
                    return;
                }
                sucesso = tarifaDAO.inserir(tarifa);
                operacaoMensagem = "salva";
            } else {
                tarifa.setId(Integer.parseInt(txtId.getText()));
                Tarifa existenteParaNovaCombinacao = tarifaDAO.buscarTarifaPorRotaETipo(rotaId, tipoPassageiroId);
                if (existenteParaNovaCombinacao != null && !Objects.equals(existenteParaNovaCombinacao.getId(), tarifa.getId())) {
                    AlertHelper.show(AlertType.ERROR, "Conflito de Tarifa", "A combinação de Rota e Tipo de Passageiro selecionada já pertence a outra tarifa (ID: " + existenteParaNovaCombinacao.getId() + "). Não é possível atualizar para esta combinação.");
                    return;
                }
                sucesso = tarifaDAO.atualizar(tarifa);
                operacaoMensagem = "atualizada";
            }

            if (sucesso) {
                AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Tarifa " + operacaoMensagem + " com sucesso!");
                carregarDadosTabela();
                limparCampos();
                tableTarifas.getSelectionModel().clearSelection();
            } else {
                AlertHelper.show(AlertType.ERROR, "Erro na Operação", "Falha ao " + (operacaoMensagem.equals("salva") ? "salvar" : "atualizar") + " a tarifa. Verifique o console para mais detalhes e se os dados são válidos.");
            }
        } catch (NumberFormatException e) {
            AlertHelper.show(AlertType.ERROR, "Erro de ID", "ID da tarifa inválido para atualização.");
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro Inesperado", "Ocorreu um erro inesperado ao salvar a tarifa: " + e.getMessage());
            AppLogger.error("CadastroTarifaController", e.getMessage(), e);
        }
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        Tarifa selecionada = tableTarifas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.show(AlertType.WARNING, "Seleção Necessária", "Selecione uma tarifa para excluir.");
            return;
        }

        Alert confirmacao = new Alert(AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Exclusão");
        confirmacao.setHeaderText("Excluir Tarifa ID: " + selecionada.getId());
        confirmacao.setContentText("Rota: " + selecionada.getNomeRota() + "\nTipo Passageiro: " + selecionada.getNomeTipoPassageiro() + "\n\nVocê tem certeza que deseja excluir esta tarifa?");

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                if (tarifaDAO.excluir(selecionada.getId())) {
                    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Tarifa excluída com sucesso!");
                    carregarDadosTabela();
                    limparCampos();
                } else {
                    AlertHelper.show(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir a tarifa. Verifique se ela não está sendo usada em outros cadastros ou se há restrições no banco de dados.");
                }
            } catch (Exception e) {
                AlertHelper.show(AlertType.ERROR, "Erro Inesperado", "Ocorreu um erro inesperado ao excluir a tarifa: " + e.getMessage());
                AppLogger.error("CadastroTarifaController", e.getMessage(), e);
            }
        }
    }

    @FXML
    private void handleFechar(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }


    // Método auxiliar para parsear BigDecimal (duplicação de VenderPassagemController, pode ser movido para utilitário)
    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleanedText = text.replace("R$", "").replace(".", "").replace(",", ".").trim();
        try {
            return new BigDecimal(cleanedText);
        } catch (NumberFormatException e) {
            AppLogger.warn("CadastroTarifaController", "Erro de formato numérico ao parsear BigDecimal: " + text + " -> " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}