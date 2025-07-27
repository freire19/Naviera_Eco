package gui;

import dao.EmbarcacaoDAO;
import dao.RotaDAO;
import dao.ViagemDAO;
import dao.AuxiliaresDAO;
import model.Embarcacao;
import model.Rota;
import model.Viagem;
import dao.ConexaoBD;

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
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class CadastroViagemController implements Initializable {

    @FXML private TextField txtIdViagem;
    @FXML private DatePicker dpDataViagem;
    @FXML private DatePicker dpDataChegada;
    @FXML private TextField txtDescricaoViagem;
    @FXML private ComboBox<Embarcacao> cmbEmbarcacao;
    @FXML private ComboBox<Rota> cmbRotaViagem;
    @FXML private ComboBox<String> cmbHorarioSaida;
    @FXML private CheckBox chkAtivaViagem;

    @FXML private Button btnNovaViagem;
    @FXML private Button btnSalvarViagem;
    @FXML private Button btnExcluirViagem;
    @FXML private Button btnEditarViagem;


    @FXML private TableView<Viagem> tabelaViagens;
    @FXML private TableColumn<Viagem, Long> colIdViagem;
    @FXML private TableColumn<Viagem, String> colDataViagem;
    @FXML private TableColumn<Viagem, String> colHorarioSaida;
    @FXML private TableColumn<Viagem, String> colDataChegada;
    @FXML private TableColumn<Viagem, String> colDescricaoViagem;
    @FXML private TableColumn<Viagem, String> colEmbarcacaoViagem;
    @FXML private TableColumn<Viagem, String> colRotaViagem;
    @FXML private TableColumn<Viagem, Boolean> colAtivaViagem;

    private ViagemDAO viagemDAO;
    private EmbarcacaoDAO embarcacaoDAO;
    private RotaDAO rotaDAO;
    private AuxiliaresDAO auxiliaresDAO;


    private ObservableList<Viagem> listaObservableViagens;
    private Viagem viagemSelecionada = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viagemDAO = new ViagemDAO();
        embarcacaoDAO = new EmbarcacaoDAO();
        rotaDAO = new RotaDAO();
        auxiliaresDAO = new AuxiliaresDAO();

        txtIdViagem.setDisable(true);
        txtIdViagem.setPromptText("Automático");

        configurarTabela();
        configurarComboBoxes();

        carregarViagensNaTabela();
        carregarEmbarcacoesNoComboBox();
        carregarRotasNoComboBox();
        carregarHorariosSaidaNoComboBox();

        tabelaViagens.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                viagemSelecionada = newSelection;
                preencherCamposComViagem(viagemSelecionada);
            }
        });

    }

    private void configurarTabela() {
        colIdViagem.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDataViagem.setCellValueFactory(new PropertyValueFactory<>("dataViagemStr"));
        colHorarioSaida.setCellValueFactory(new PropertyValueFactory<>("horarioSaidaStr"));
        colDataChegada.setCellValueFactory(new PropertyValueFactory<>("dataChegadaStr"));
        colDescricaoViagem.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colEmbarcacaoViagem.setCellValueFactory(new PropertyValueFactory<>("nomeEmbarcacao"));
        colRotaViagem.setCellValueFactory(new PropertyValueFactory<>("nomeRotaConcatenado"));
        colAtivaViagem.setCellValueFactory(new PropertyValueFactory<>("ativa"));

        colAtivaViagem.setCellFactory(column -> new TableCell<Viagem, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Sim" : "Não"));
            }
        });

        listaObservableViagens = FXCollections.observableArrayList();
        tabelaViagens.setItems(listaObservableViagens);
    }

    private void configurarComboBoxes() {
        if (cmbEmbarcacao != null) {
            cmbEmbarcacao.setConverter(new StringConverter<Embarcacao>() {
                @Override public String toString(Embarcacao embarcacao) { return embarcacao == null ? "Selecione..." : embarcacao.getNome(); }
                @Override public Embarcacao fromString(String string) { return null; }
            });
            cmbEmbarcacao.setPlaceholder(new Label("Nenhuma embarcação cadastrada"));
        }

        if (cmbRotaViagem != null) {
            cmbRotaViagem.setConverter(new StringConverter<Rota>() {
                @Override public String toString(Rota rota) { return rota == null ? "Selecione..." : rota.toString(); }
                @Override public Rota fromString(String string) { return null; }
            });
            cmbRotaViagem.setPlaceholder(new Label("Nenhuma rota cadastrada"));
        }

        if (cmbHorarioSaida != null) {
            cmbHorarioSaida.setPlaceholder(new Label("Nenhum horário cadastrado"));
        }
    }

    private void carregarEmbarcacoesNoComboBox() {
        List<Embarcacao> embarcacoes = embarcacaoDAO.listarTodas();
        if (cmbEmbarcacao != null) {
            if (embarcacoes != null && !embarcacoes.isEmpty()) {
                cmbEmbarcacao.setItems(FXCollections.observableArrayList(embarcacoes));
            } else {
                cmbEmbarcacao.getItems().clear();
            }
        }
    }

    private void carregarRotasNoComboBox() {
        List<Rota> rotas = rotaDAO.listarTodasAsRotasComoObjects();
        if (cmbRotaViagem != null) {
            if (rotas != null && !rotas.isEmpty()) {
                cmbRotaViagem.setItems(FXCollections.observableArrayList(rotas));
            } else {
                cmbRotaViagem.getItems().clear();
            }
        }
    }

    private void carregarHorariosSaidaNoComboBox() {
        if (cmbHorarioSaida == null) return;
        try {
            List<String> horarios = auxiliaresDAO.listarHorarioSaida();
            if (horarios != null && !horarios.isEmpty()) {
                cmbHorarioSaida.setItems(FXCollections.observableArrayList(horarios));
            } else {
                cmbHorarioSaida.getItems().clear();
                showAlert(Alert.AlertType.INFORMATION, "Aviso", "Não há horários de saída cadastrados. Cadastre-os em 'Outros Cadastros Auxiliares'.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erro de Carregamento", "Falha ao carregar horários de saída: " + e.getMessage());
            e.printStackTrace();
            cmbHorarioSaida.getItems().clear();
        }
    }


    private void carregarViagensNaTabela() {
        List<Viagem> viagensDoBanco = viagemDAO.listarTodasViagensResumido();
        if (viagensDoBanco != null) {
            listaObservableViagens.setAll(viagensDoBanco);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erro", "Não foi possível carregar a lista de viagens.");
            listaObservableViagens.clear();
        }
        tabelaViagens.refresh();
    }

    private void preencherCamposComViagem(Viagem v) {
        if (v == null) {
            limparCampos();
            return;
        }
        txtIdViagem.setText(String.valueOf(v.getId()));
        dpDataViagem.setValue(v.getDataViagem());
        dpDataChegada.setValue(v.getDataChegada());
        txtDescricaoViagem.setText(v.getDescricao());
        chkAtivaViagem.setSelected(v.isAtiva());

        if (v.getIdEmbarcacao() != null && cmbEmbarcacao.getItems() != null) {
            boolean found = false;
            for (Embarcacao emb : cmbEmbarcacao.getItems()) {
                if (Objects.equals(emb.getId(), v.getIdEmbarcacao())) {
                    cmbEmbarcacao.setValue(emb);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cmbEmbarcacao.setValue(null);
                showAlert(Alert.AlertType.WARNING, "Embarcação Ausente", "A embarcação associada a esta viagem não foi encontrada na lista. Pode ter sido excluída.");
            }
        } else {
            cmbEmbarcacao.setValue(null);
        }

        if (v.getIdRota() != null && cmbRotaViagem.getItems() != null) {
            boolean found = false;
            for (Rota rota : cmbRotaViagem.getItems()) {
                if (Objects.equals(rota.getId(), v.getIdRota())) {
                    cmbRotaViagem.setValue(rota);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cmbRotaViagem.setValue(null);
                showAlert(Alert.AlertType.WARNING, "Rota Ausente", "A rota associada a esta viagem não foi encontrada na lista. Pode ter sido excluída.");
            }
        } else {
            cmbRotaViagem.setValue(null);
        }

        if (v.getDescricaoHorarioSaida() != null && cmbHorarioSaida.getItems() != null) {
            if (cmbHorarioSaida.getItems().contains(v.getDescricaoHorarioSaida())) {
                cmbHorarioSaida.setValue(v.getDescricaoHorarioSaida());
            } else {
                cmbHorarioSaida.setValue(null);
                System.err.println("Aviso: Horário de saída '" + v.getDescricaoHorarioSaida() + "' da viagem ID " + v.getId() + " não encontrado nos cadastros auxiliares.");
            }
        } else {
            cmbHorarioSaida.setValue(null);
        }
    }

    @FXML
    private void handleNovaViagem(ActionEvent event) {
        limparCampos();
        viagemSelecionada = null;
        Long proximoId = viagemDAO.gerarProximoIdViagem();
        if (proximoId != null && proximoId > 0) {
            txtIdViagem.setText(String.valueOf(proximoId));
        } else {
            txtIdViagem.clear();
            showAlert(Alert.AlertType.ERROR, "Erro ao Gerar ID", "Não foi possível obter um novo ID para a viagem. Verifique a sequence 'seq_viagem'.");
        }
        dpDataViagem.requestFocus();
    }

    @FXML
    private void handleSalvarViagem(ActionEvent event) {
        String idStr = txtIdViagem.getText();
        LocalDate dataPartida = dpDataViagem.getValue();
        LocalDate dataChegada = dpDataChegada.getValue();
        String descricao = txtDescricaoViagem.getText() == null ? "" : txtDescricaoViagem.getText().trim();
        Embarcacao embarcacaoSelecionada = cmbEmbarcacao.getValue();
        Rota rotaSelecionada = cmbRotaViagem.getValue();
        String horarioSaidaSelecionado = cmbHorarioSaida.getValue();
        boolean ativa = chkAtivaViagem.isSelected();

        if (idStr.isEmpty() || dataPartida == null || dataChegada == null || embarcacaoSelecionada == null || rotaSelecionada == null || horarioSaidaSelecionado == null || horarioSaidaSelecionado.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos Obrigatórios", "ID (clique em Nova Viagem), Data de Viagem, Data de Chegada, Embarcação, Rota e HORÁRIO DE SAÍDA são obrigatórios.");
            return;
        }
        if (dataChegada.isBefore(dataPartida)) {
            showAlert(Alert.AlertType.WARNING, "Data Inválida", "A Data de Chegada não pode ser anterior à Data de Viagem.");
            return;
        }

        Long idViagemLong;
        try {
            idViagemLong = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erro de ID", "ID da viagem inválido.");
            return;
        }

        Long idHorarioSaidaDB;
        try {
            Integer idAux = auxiliaresDAO.obterIdHorarioSaidaPorNome(horarioSaidaSelecionado);
            idHorarioSaidaDB = (idAux != null) ? idAux.longValue() : null; // CORRIGIDO AQUI
            if (idHorarioSaidaDB == null || idHorarioSaidaDB == -1L) {
                showAlert(Alert.AlertType.ERROR, "Erro de Horário", "Não foi possível encontrar o ID para o horário de saída selecionado. Verifique os cadastros auxiliares.");
                return;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao obter ID do horário de saída: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Viagem v;
        boolean isInsert = (viagemSelecionada == null);

        if (isInsert) {
            v = new Viagem();
            v.setId(idViagemLong);
        } else {
            v = viagemSelecionada;
            if (!Objects.equals(v.getId(), idViagemLong)) {
                showAlert(Alert.AlertType.ERROR, "Erro de ID", "O ID da viagem foi alterado. Selecione na tabela para editar ou clique em Nova Viagem.");
                return;
            }
        }

        v.setDataViagem(dataPartida);
        v.setDataChegada(dataChegada);
        v.setDescricao(descricao);
        v.setAtiva(ativa);
        v.setIdEmbarcacao(embarcacaoSelecionada.getId());
        v.setIdRota(rotaSelecionada.getId());
        v.setIdHorarioSaida(idHorarioSaidaDB);
        v.setDescricaoHorarioSaida(horarioSaidaSelecionado);

        boolean sucesso;
        String mensagem;

        if (isInsert) {
            sucesso = viagemDAO.inserir(v);
            mensagem = "Viagem ID " + v.getId() + " cadastrada com sucesso!";
        } else {
            sucesso = viagemDAO.atualizar(v);
            mensagem = "Viagem ID " + v.getId() + " atualizada com sucesso!";
        }

        if (sucesso) {
            if (ativa) {
                if (!viagemDAO.definirViagemAtiva(v.getId())) {
                    System.err.println("Aviso: Falha ao tentar definir a viagem ID " + v.getId() + " como a única ativa.");
                }
            }
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", mensagem);
            carregarViagensNaTabela();
            handleNovaViagem(null);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erro", "Falha ao salvar viagem no banco de dados.");
        }
    }

    @FXML
    private void handleExcluirViagem(ActionEvent event) {
        viagemSelecionada = tabelaViagens.getSelectionModel().getSelectedItem();
        if (viagemSelecionada == null) {
            showAlert(Alert.AlertType.WARNING, "Atenção", "Nenhuma viagem selecionada para excluir.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir a Viagem ID: " + viagemSelecionada.getId() + "?\n" +
                "Data: " + viagemSelecionada.getDataViagemStr() + " - " + viagemSelecionada.getHorarioSaidaStr() + " - " + viagemSelecionada.getDescricao(),
                ButtonType.YES, ButtonType.NO);
        confirmacao.setTitle("Confirmar Exclusão");
        confirmacao.setHeaderText(null);

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.YES) {
            if (viagemDAO.excluir(viagemSelecionada.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Viagem excluída com sucesso!");
                carregarViagensNaTabela();
                handleNovaViagem(null);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erro", "Falha ao excluir viagem. Verifique se há dados dependentes (passagens, encomendas, etc).");
            }
        }
    }

    @FXML
    private void handleEditarViagem(ActionEvent event) {
        viagemSelecionada = tabelaViagens.getSelectionModel().getSelectedItem();
        if (viagemSelecionada == null) {
            showAlert(Alert.AlertType.WARNING, "Atenção", "Nenhuma viagem selecionada para edição.");
            return;
        }
        preencherCamposComViagem(viagemSelecionada);
        dpDataViagem.requestFocus();
    }


    private void limparCampos() {
        txtIdViagem.clear();
        dpDataViagem.setValue(null);
        dpDataChegada.setValue(null);
        txtDescricaoViagem.clear();
        if (cmbEmbarcacao != null) {
            cmbEmbarcacao.getSelectionModel().clearSelection();
            cmbEmbarcacao.setValue(null);
        }
        if (cmbRotaViagem != null) {
            cmbRotaViagem.getSelectionModel().clearSelection();
            cmbRotaViagem.setValue(null);
        }
        if (cmbHorarioSaida != null) {
            cmbHorarioSaida.getSelectionModel().clearSelection();
            cmbHorarioSaida.setValue(null);
        }
        chkAtivaViagem.setSelected(false);
        if (tabelaViagens != null) tabelaViagens.getSelectionModel().clearSelection();
        viagemSelecionada = null;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}