package gui;

import dao.PassageiroDAO;
import dao.PassagemDAO;
import dao.TarifaDAO;
import dao.ViagemDAO;
import dao.ConexaoBD;

import gui.util.SessaoUsuario;
import gui.util.AutoCompleteComboBoxListener;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.Passageiro;
import model.Passagem;
import model.Tarifa;
import model.Viagem;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

// Importar AuxiliaresDAO
import dao.AuxiliaresDAO;

public class VenderPassagemController implements Initializable {

    @FXML private AnchorPane rootPane;

    @FXML private TextField txtNumeroDoc;
    @FXML private TextField txtIdade;
    @FXML private DatePicker dpDataNascimento;
    @FXML private TextField txtNascimentoMask;
    @FXML private TextField txtAlimentacao;
    @FXML private TextField txtTransporte;
    @FXML private TextField txtCargas;
    @FXML private TextField txtDescontoTarifa;
    @FXML private TextField txtTotal;
    @FXML private TextField txtDesconto;
    @FXML private TextField txtAPagar;
    @FXML private TextField txtValorPago;
    @FXML private TextField txtDevedor;
    @FXML private TextField txtTroco;
    @FXML private TextField txtRequisicao;
    @FXML private TextField txtHorario;
    @FXML private ComboBox<String> cmbPassageiroAuto;
    @FXML private ComboBox<String> cmbRota;
    @FXML private ComboBox<String> cmbTipoPassagemAux;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private ComboBox<String> cmbCaixa;
    @FXML private ComboBox<String> cmbViagem;
    @FXML private ComboBox<String> cmbSexo;
    @FXML private ComboBox<String> cmbTipoDoc;
    @FXML private ComboBox<String> cmbNacionalidade;
    @FXML private ComboBox<String> cmbAgenteAux;
    @FXML private ComboBox<String> cmbAcomodacao;
    @FXML private ComboBox<String> cmbPesquisarModo;
    @FXML private TextField txtPesquisar;
    @FXML private TextField txtNumBilhete;
    @FXML private TextField txtDataViagemMask;
    @FXML private DatePicker dpDataViagem;
    @FXML private TextField txtTotalPassageiros;

    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;
    @FXML private Button btnNovo;
    @FXML private Button btnFiltrar;
    @FXML private Button btnEditar;
    @FXML private Button btnExcluir;
    @FXML private Button btnImprimirBilhete;
    @FXML private Button btnImprimirLista;
    @FXML private Button btnRelatorio;
    @FXML private Button btnSair;

    @FXML private TableView<Passagem> tablePassagens;
    @FXML private TableColumn<Passagem, String> colNumBilhete;
    @FXML private TableColumn<Passagem, String> colPassageiro;
    @FXML private TableColumn<Passagem, LocalDate> colDataNascimento;
    @FXML private TableColumn<Passagem, String> colNumeroDoc;
    @FXML private TableColumn<Passagem, String> colNacionalidade;
    @FXML private TableColumn<Passagem, String> colOrigem;
    @FXML private TableColumn<Passagem, String> colDestino;
    @FXML private TableColumn<Passagem, BigDecimal> colValor;
    @FXML private TableColumn<Passagem, BigDecimal> colValorDesconto;
    @FXML private TableColumn<Passagem, BigDecimal> colValorAPagar;
    @FXML private TableColumn<Passagem, BigDecimal> colValorPago;
    @FXML private TableColumn<Passagem, BigDecimal> colDevedor;


    private PassageiroDAO passageiroDAO;
    private PassagemDAO passagemDAO;
    private TarifaDAO tarifaDAO;
    private ViagemDAO viagemDAO;
    private AuxiliaresDAO auxiliaresDAO;

    private Passagem passagemEmEdicao = null;
    private Passageiro passageiroEmEdicao = null;
    private Viagem viagemSelecionada = null; // Esta é a viagem ativa carregada na inicialização

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passageiroDAO = new PassageiroDAO();
        passagemDAO = new PassagemDAO();
        tarifaDAO = new TarifaDAO();
        viagemDAO = new ViagemDAO();
        auxiliaresDAO = new AuxiliaresDAO();

        if (dpDataNascimento != null && txtNascimentoMask != null) {
            setupDatePickerAndMaskedTextField(dpDataNascimento, txtNascimentoMask);
        }
        if (dpDataViagem != null && txtDataViagemMask != null) {
            setupDatePickerAndMaskedTextField(dpDataViagem, txtDataViagemMask);
        }

        carregarComboBoxes();
        aplicarAutoCompleteEmComboBoxes();

        limparCamposTarifa();

        configurarTabelaPassagens();
        adicionarListenerDeSelecaoNaTabela();
        adicionarListenerAoCampoPesquisar();
        configurarNavegacaoEntreCampos();

        // ** FLUXO REVISADO: ESTADO INICIAL DA TELA **
        configurarEstadoInicialDaTela(); 
        
        // Listeners para cálculo de tarifa e totais (permanecem os mesmos)
        if (cmbRota != null) {
            cmbRota.setOnAction(event -> carregarValoresTarifaAutomatica());
        }
        if (cmbTipoPassagemAux != null) {
            cmbTipoPassagemAux.setOnAction(event -> carregarValoresTarifaAutomatica());
        }

        // Listener para seleção da viagem no ComboBox (permanece o mesmo)
        // Permite que o usuário mude a viagem SE a venda não estiver em andamento
        if (cmbViagem != null) {
            cmbViagem.setOnAction(event -> {
                String selectedViagemStr = cmbViagem.getSelectionModel().getSelectedItem();
                if (selectedViagemStr != null && !selectedViagemStr.isEmpty()) {
                    try {
                        Long idViagem = viagemDAO.obterIdViagemPelaString(selectedViagemStr);
                        if (idViagem != null) {
                            Viagem viagem = viagemDAO.buscarPorId(idViagem);
                            if (viagem != null) {
                                // Atualiza a viagem selecionada apenas se não estivermos no meio de uma venda
                                if (passagemEmEdicao == null && passageiroEmEdicao == null) {
                                    viagemSelecionada = viagem;
                                    if (dpDataViagem != null) dpDataViagem.setValue(viagem.getDataViagem());
                                    if (txtHorario != null) txtHorario.setText(viagem.getHorarioSaidaStr());
                                } else {
                                    showAlert(AlertType.WARNING, "Operação em Andamento", "Não é possível trocar de viagem durante uma venda ou edição. Cancele primeiro.");
                                    // Volta para a viagem previamente selecionada no ComboBox se houver
                                    if(viagemSelecionada != null) cmbViagem.setValue(viagemSelecionada.toString());
                                    else cmbViagem.getSelectionModel().clearSelection(); // Limpa se não havia viagem selecionada
                                }
                            } else {
                                showAlert(AlertType.WARNING, "Viagem Não Encontrada", "Detalhes da viagem selecionada não puderam ser carregados.");
                                dpDataViagem.setValue(null);
                                txtHorario.clear();
                                if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
                                // Não zera viagemSelecionada aqui se já estiver no meio de uma venda
                                if(passagemEmEdicao == null) viagemSelecionada = null;
                            }
                        } else {
                            showAlert(AlertType.WARNING, "Viagem Não Encontrada", "Não foi possível identificar o ID da viagem selecionada.");
                            dpDataViagem.setValue(null);
                            txtHorario.clear();
                            if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
                            if(passagemEmEdicao == null) viagemSelecionada = null;
                        }
                    } catch (SQLException e) {
                        showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Falha ao carregar detalhes da viagem: " + e.getMessage());
                        e.printStackTrace();
                        dpDataViagem.setValue(null);
                        txtHorario.clear();
                        if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
                        if(passagemEmEdicao == null) viagemSelecionada = null;
                    }
                } else {
                    dpDataViagem.setValue(null);
                    txtHorario.clear();
                    if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
                    if(passagemEmEdicao == null) viagemSelecionada = null;
                }
                carregarValoresTarifaAutomatica(); // Recalcula tarifa baseada na rota da viagem selecionada
            });
        }

        setupCalculoTotalPassagem();
        
        // Listeners para auto-preenchimento de passageiro (permanecem os mesmos)
        if (cmbPassageiroAuto != null) {
            cmbPassageiroAuto.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && cmbPassageiroAuto.getEditor().getText() != null && !cmbPassageiroAuto.getEditor().getText().isEmpty()) {
                    preencherPassageiroPorNome(cmbPassageiroAuto.getEditor().getText());
                }
            });
            cmbPassageiroAuto.setOnAction(event -> {
                if (cmbPassageiroAuto.getSelectionModel().getSelectedItem() != null) {
                    try {
                        Passageiro p = passageiroDAO.buscarPorNome(cmbPassageiroAuto.getSelectionModel().getSelectedItem());
                        if (p != null) {
                            preencherCamposPassageiro(p);
                        } else {
                            limparCamposPassageiroGUI();
                            if(cmbPassageiroAuto.getEditor() != null) cmbPassageiroAuto.getEditor().setText(cmbPassageiroAuto.getSelectionModel().getSelectedItem());
                            passageiroEmEdicao = null;
                        }
                    } catch (Exception e) {
                        showAlert(AlertType.ERROR, "Erro", "Erro ao carregar dados do passageiro selecionado: " + e.getMessage());
                        e.printStackTrace();
                        limparCamposPassageiroGUI();
                        passageiroEmEdicao = null;
                    }
                }
            });
        }
        if (txtNumeroDoc != null) {
            txtNumeroDoc.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && txtNumeroDoc.getText() != null && !txtNumeroDoc.getText().isEmpty()) {
                    preencherPassageiroPorDoc(txtNumeroDoc.getText());
                }
            });
            txtNumeroDoc.setOnAction(event -> {
                if (txtNumeroDoc.getText() != null && !txtNumeroDoc.getText().isEmpty()) {
                    preencherPassageiroPorDoc(txtNumeroDoc.getText());
                }
            });
        }
    }

    // NOVO MÉTODO PARA CONFIGURAR O ESTADO INICIAL DA TELA
    private void configurarEstadoInicialDaTela() {
        limparTodosOsCamposGUI(); // Limpa tudo
        habilitarCamposParaNovaVenda(false); // Desabilita todos os campos de entrada inicialmente

        // Busca a viagem ativa
        Viagem viagemAtivaNoMomento = viagemDAO.buscarViagemAtiva(); 
        if (viagemAtivaNoMomento != null) {
            this.viagemSelecionada = viagemAtivaNoMomento; // Atualiza a viagemSelecionada da classe
            String activeViagemStr = viagemAtivaNoMomento.toString();
            
            // Preenche o ComboBox de Viagem e seleciona a viagem ativa
            if (cmbViagem != null) {
                // Adiciona a viagem ativa se não estiver na lista (para garantir)
                if (!cmbViagem.getItems().contains(activeViagemStr)) {
                    cmbViagem.getItems().add(0, activeViagemStr);
                }
                cmbViagem.getSelectionModel().select(activeViagemStr);
                cmbViagem.setDisable(true); // Garante que o usuário não mude a viagem selecionada por padrão
            }
            // Preenche os campos de data e horário da viagem
            if (dpDataViagem != null) dpDataViagem.setValue(viagemAtivaNoMomento.getDataViagem());
            if (txtHorario != null) txtHorario.setText(viagemAtivaNoMomento.getHorarioSaidaStr());

            // Habilita o botão "Novo" se uma viagem ativa foi encontrada
            if (btnNovo != null) btnNovo.setDisable(false);
            
            // O alerta da viagem ativa é dado pela Tela Principal, não repetimos aqui
            // showAlert(AlertType.INFORMATION, "Viagem Ativa", "Uma viagem ativa foi carregada. Clique em 'Novo' para iniciar uma nova venda.");

        } else {
            // Se nenhuma viagem ativa for encontrada, desabilita o botão "Novo" também
            if (btnNovo != null) btnNovo.setDisable(true);
            showAlert(AlertType.INFORMATION, "Viagem Ativa Necessária", "Nenhuma viagem ativa encontrada no sistema. Cadastre ou ative uma viagem na Tela Principal. As vendas estão desabilitadas.");
        }
        // Configura os botões de ação geral
        configurarEstadoBotoesGerais();
        carregarPassagensNaTabela(); // Carrega a tabela de passagens existente
    }
    
    private void configurarEstadoBotoesGerais() {
        // Estes botões devem sempre estar habilitados (ou sua habilitação depende da seleção na tabela)
        if (btnFiltrar != null) btnFiltrar.setDisable(false);
        if (btnImprimirLista != null) btnImprimirLista.setDisable(false);
        if (btnRelatorio != null) btnRelatorio.setDisable(false);
        if (btnSair != null) btnSair.setDisable(false);
        // Editar e Excluir dependem da seleção na tabela, o listener de seleção cuida disso
        if (btnEditar != null) btnEditar.setDisable(tablePassagens.getSelectionModel().getSelectedItem() == null);
        if (btnExcluir != null) btnExcluir.setDisable(tablePassagens.getSelectionModel().getSelectedItem() == null);
    }


    private void setupDatePickerAndMaskedTextField(DatePicker datePicker, TextField maskedTextField) {
        UnaryOperator<TextFormatter.Change> dateFilter = change -> {
            String newText = change.getControlNewText();
            String cleanedText = newText.replaceAll("[^\\d]", "");

            if (cleanedText.length() > 8) {
                cleanedText = cleanedText.substring(0, 8);
            }

            StringBuilder formattedText = new StringBuilder();
            for (int i = 0; i < cleanedText.length(); i++) {
                formattedText.append(cleanedText.charAt(i));
                if (i == 1 || i == 3) {
                    formattedText.append("/");
                }
            }

            if (formattedText.length() > 10) {
                formattedText = new StringBuilder(formattedText.substring(0, 10));
            }

            change.setText(formattedText.toString());
            change.setCaretPosition(formattedText.length());
            change.setAnchor(formattedText.length());
            change.setRange(0, change.getControlText().length());

            return change;
        };

        maskedTextField.setTextFormatter(new TextFormatter<>(dateFilter));

        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, dateFormatter);
                    } catch (java.time.format.DateTimeParseException e) {
                        showAlert(AlertType.ERROR, "Erro de Formato", "Formato de data inválido. Use dd/MM/yyyy.");
                        return null;
                    }
                }
                return null;
            }
        });

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                maskedTextField.setText(dateFormatter.format(newDate));
                if (datePicker == dpDataNascimento) {
                    calcularIdade();
                }
            } else {
                maskedTextField.clear();
                if (datePicker == dpDataNascimento) {
                    if (txtIdade != null) txtIdade.clear();
                }
            }
        });

        maskedTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && maskedTextField.getText() != null && !maskedTextField.getText().isEmpty()) {
                try {
                    LocalDate parsedDate = LocalDate.parse(maskedTextField.getText(), dateFormatter);
                    datePicker.setValue(parsedDate);
                }
                catch (java.time.format.DateTimeParseException e) {
                    showAlert(AlertType.ERROR, "Erro de Formato", "Formato de data inválido. Use dd/MM/yyyy.");
                    datePicker.setValue(null);
                    maskedTextField.clear();
                }
            }
        });
        maskedTextField.setOnAction(event -> {
            if (maskedTextField.getText() != null && !maskedTextField.getText().isEmpty()) {
                try {
                    LocalDate parsedDate = LocalDate.parse(maskedTextField.getText(), dateFormatter);
                    datePicker.setValue(parsedDate);
                } catch (java.time.format.DateTimeParseException e) {
                    showAlert(AlertType.ERROR, "Erro de Formato", "Formato de data inválido. Use dd/MM/yyyy.");
                    datePicker.setValue(null);
                    maskedTextField.clear();
                }
            }
        });
    }

    private void carregarComboBoxes() {
        try (Connection conn = ConexaoBD.getConnection()) {
            List<String> nomesPass = passageiroDAO.listarTodosNomesPassageiros();
            if (cmbPassageiroAuto != null) {
                cmbPassageiroAuto.setItems(FXCollections.observableArrayList(nomesPass));
                cmbPassageiroAuto.setEditable(true);
            }

            List<String> rotasStrings = new java.util.ArrayList<>();
            try {
                List<model.Rota> rotasObjects = new dao.RotaDAO().listarTodasAsRotasComoObjects();
                if (rotasObjects != null) {
                    for (model.Rota r : rotasObjects) {
                        rotasStrings.add(r.toString());
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar rotas para combobox: " + e.getMessage());
            }

            if (cmbRota != null) cmbRota.setItems(FXCollections.observableArrayList(rotasStrings));

            List<String> tiposPassagem = auxiliaresDAO.listarPassagemAux(); // Chamar AuxiliaresDAO
            if (cmbTipoPassagemAux != null) {
                // Adicionado depuração para verificar se a lista não está vazia aqui
                if(tiposPassagem.isEmpty()) {
                    System.err.println("DEBUG: ListarPassagemAux retornou lista vazia. Verifique o DB ou o método AuxiliaresDAO.listarPassagemAux().");
                }
                cmbTipoPassagemAux.setItems(FXCollections.observableArrayList(tiposPassagem));
            }

            List<String> tiposPagamento = auxiliaresDAO.listarTiposPagamento(); // Chamar AuxiliaresDAO
            if (cmbFormaPagamento != null) cmbFormaPagamento.setItems(FXCollections.observableArrayList(tiposPagamento));

            List<String> caixas = carregarDadosDoBanco(conn, "SELECT nome_caixa FROM caixas ORDER BY nome_caixa");
            if (cmbCaixa != null) cmbCaixa.setItems(FXCollections.observableArrayList(caixas));

            List<String> viagens = viagemDAO.listarViagensParaComboBox();
            if (cmbViagem != null) cmbViagem.setItems(FXCollections.observableArrayList(viagens));

            List<String> sexos = auxiliaresDAO.listarSexo(); // Chamar AuxiliaresDAO
            if (cmbSexo != null) cmbSexo.setItems(FXCollections.observableArrayList(sexos));

            List<String> tiposDoc = auxiliaresDAO.listarTipoDoc(); // Chamar AuxiliaresDAO
            if (cmbTipoDoc != null) cmbTipoDoc.setItems(FXCollections.observableArrayList(tiposDoc));

            List<String> nacionalidades = auxiliaresDAO.listarNacionalidade(); // Chamar AuxiliaresDAO
            if (cmbNacionalidade != null) cmbNacionalidade.setItems(FXCollections.observableArrayList(nacionalidades));

            List<String> agentes = auxiliaresDAO.listarAgenteAux(); // Chamar AuxiliaresDAO
            if (cmbAgenteAux != null) cmbAgenteAux.setItems(FXCollections.observableArrayList(agentes));

            List<String> acomodacoes = auxiliaresDAO.listarAcomodacao(); // Chamar AuxiliaresDAO
            if (cmbAcomodacao != null) cmbAcomodacao.setItems(FXCollections.observableArrayList(acomodacoes));

            if (cmbPesquisarModo != null) {
                cmbPesquisarModo.setItems(FXCollections.observableArrayList("Número Bilhete", "Passageiro", "Nº Documento", "Data Partida"));
                cmbPesquisarModo.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erro de Carregamento", "Falha ao carregar dados do banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> carregarDadosDoBanco(Connection conn, String sql) throws SQLException {
        List<String> data = FXCollections.observableArrayList();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                data.add(rs.getString(1));
            }
        }
        return data;
    }

    private void preencherPassageiroPorNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            limparCamposPassageiroGUI();
            passageiroEmEdicao = null;
            return;
        }

        try {
            Passageiro p = passageiroDAO.buscarPorNome(nome.trim());
            if (p != null) {
                preencherCamposPassageiro(p);
            } else {
                passageiroEmEdicao = null;
                if (cmbPassageiroAuto != null && !cmbPassageiroAuto.isShowing()) {
                    showAlert(AlertType.INFORMATION, "Passageiro Não Encontrado", "Passageiro não encontrado para o nome digitado. Por favor, complete os dados do passageiro.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro ao buscar passageiro por nome: " + e.getMessage());
            e.printStackTrace();
            limparCamposPassageiroGUI();
            passageiroEmEdicao = null;
        }
    }

    private void preencherPassageiroPorDoc(String doc) {
        if (doc == null || doc.trim().isEmpty()) {
            limparCamposPassageiroGUI();
            passageiroEmEdicao = null;
            return;
        }

        try {
            Passageiro p = passageiroDAO.buscarPorDoc(doc.trim());
            if (p != null) {
                preencherCamposPassageiro(p);
            } else {
                passageiroEmEdicao = null;
                showAlert(AlertType.INFORMATION, "Passageiro Não Encontrado", "Passageiro não encontrado para o documento digitado. Por favor, complete os dados do passageiro.");
            }
        } catch (Exception e) {
        	showAlert(AlertType.ERROR, "Erro", "Erro ao buscar passageiro por documento: " + e.getMessage());
        	e.printStackTrace(); // Chama printStackTrace separadamente para o console
            e.printStackTrace(); // Added this back
            limparCamposPassageiroGUI();
            passageiroEmEdicao = null;
        }
    }

    private void preencherCamposPassageiro(Passageiro p) {
        if (p == null) {
            limparCamposPassageiroGUI();
            passageiroEmEdicao = null;
            return;
        }
        passageiroEmEdicao = p;

        if (cmbPassageiroAuto != null) {
            cmbPassageiroAuto.getEditor().setText(p.getNome());
            cmbPassageiroAuto.setValue(p.getNome());
        }
        if (txtNumeroDoc != null) txtNumeroDoc.setText(p.getNumeroDoc());
        if (dpDataNascimento != null) dpDataNascimento.setValue(p.getDataNascimento());
        if (cmbTipoDoc != null) cmbTipoDoc.setValue(p.getTipoDoc());
        if (cmbNacionalidade != null) cmbNacionalidade.setValue(p.getNacionalidade());
        if (cmbSexo != null) cmbSexo.setValue(p.getSexo());

        calcularIdade();
    }

    private void limparCamposPassageiroGUI() {
        if (cmbPassageiroAuto != null) {
            cmbPassageiroAuto.getSelectionModel().clearSelection();
            cmbPassageiroAuto.getEditor().clear();
        }
        if (txtNumeroDoc != null) txtNumeroDoc.clear();
        if (dpDataNascimento != null) dpDataNascimento.setValue(null);
        if (txtNascimentoMask != null) txtNascimentoMask.clear();
        if (txtIdade != null) txtIdade.clear();
        if (cmbTipoDoc != null) cmbTipoDoc.getSelectionModel().clearSelection();
        if (cmbNacionalidade != null) cmbNacionalidade.getSelectionModel().clearSelection();
        if (cmbSexo != null) cmbSexo.getSelectionModel().clearSelection();
    }


    private void calcularIdade() {
        if (dpDataNascimento != null && dpDataNascimento.getValue() != null) {
            LocalDate dataNasc = dpDataNascimento.getValue();
            LocalDate hoje = LocalDate.now();
            int idade = Period.between(dataNasc, hoje).getYears();
            if (txtIdade != null) {
                txtIdade.setText(String.valueOf(idade));
            }
        } else {
            if (txtIdade != null) txtIdade.clear();
        }
    }

    private void carregarValoresTarifaAutomatica() {
        String selectedRotaStr = cmbRota != null ? cmbRota.getSelectionModel().getSelectedItem() : null;
        String selectedTipoPassagemStr = cmbTipoPassagemAux != null ? cmbTipoPassagemAux.getSelectionModel().getSelectedItem() : null;

        if (selectedRotaStr == null || selectedRotaStr.trim().isEmpty() || selectedTipoPassagemStr == null || selectedTipoPassagemStr.trim().isEmpty()) {
            limparCamposTarifa();
            return;
        }

        try {
            String[] rotaParts = selectedRotaStr.split(" - ");
            String origem = rotaParts[0].trim();
            String destino = (rotaParts.length > 1) ? rotaParts[1].trim() : "";

            Integer idRotaInteger = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origem, destino);
            long idRota = (idRotaInteger != null) ? idRotaInteger.longValue() : 0;

            Integer idTipoPassagem = auxiliaresDAO.obterIdAuxiliar("aux_tipos_passagem", "nome_tipo_passagem", "id_tipo_passagem", selectedTipoPassagemStr);

            if (idRota == 0 || idTipoPassagem == null) {
                showAlert(AlertType.WARNING, "IDs Não Encontrados", "Não foi possível identificar a Rota ou o Tipo de Passagem selecionados no banco de dados.");
                limparCamposTarifa();
                return;
            }

            Tarifa tarifaEncontrada = tarifaDAO.buscarTarifaPorRotaETipo(idRota, idTipoPassagem);

            if (tarifaEncontrada != null) {
                if (txtAlimentacao != null) txtAlimentacao.setText(String.format("%,.2f", tarifaEncontrada.getValorAlimentacao()));
                if (txtTransporte != null) txtTransporte.setText(String.format("%,.2f", tarifaEncontrada.getValorTransporte()));
                if (txtCargas != null) txtCargas.setText(String.format("%,.2f", tarifaEncontrada.getValorCargas()));
                if (txtDescontoTarifa != null) txtDescontoTarifa.setText(String.format("%,.2f", tarifaEncontrada.getValorDesconto()));
            } else {
                showAlert(AlertType.INFORMATION, "Tarifa Não Encontrada", "Não foi encontrada uma tarifa para a Rota e Tipo de Passagem selecionados. Preencha manualmente ou cadastre a tarifa.");
                limparCamposTarifa();
            }
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Falha ao carregar tarifa: " + e.getMessage());
            e.printStackTrace();
            limparCamposTarifa();
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro inesperado ao carregar tarifa: " + e.getMessage());
            e.printStackTrace();
            limparCamposTarifa();
        } finally {
            calcularValoresPassagem();
        }
    }


    private void setupCalculoTotalPassagem() {
        List.of(txtAlimentacao, txtTransporte, txtCargas, txtDescontoTarifa, txtDesconto, txtValorPago).forEach(field -> {
            if (field != null) {
                field.textProperty().addListener((obs, oldVal, newVal) -> calcularValoresPassagem());
                field.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        try {
                            BigDecimal value = parseBigDecimal(field.getText());
                            field.setText(String.format("%,.2f", value));
                        }
                        catch (NumberFormatException e) {
                            if (!field.getText().isEmpty()) {
                                showAlert(AlertType.WARNING, "Formato Inválido", "Por favor, insira um valor numérico válido.");
                                field.clear();
                            }
                        }
                    }
                });
            }
        });
    }

    private void calcularValoresPassagem() {
        BigDecimal alimentacao = parseBigDecimal(txtAlimentacao != null ? txtAlimentacao.getText() : null);
        BigDecimal transporte = parseBigDecimal(txtTransporte != null ? txtTransporte.getText() : null);
        BigDecimal cargas = parseBigDecimal(txtCargas != null ? txtCargas.getText() : null);
        BigDecimal descontoTarifa = parseBigDecimal(txtDescontoTarifa != null ? txtDescontoTarifa.getText() : null);
        BigDecimal descontoGeral = parseBigDecimal(txtDesconto != null ? txtDesconto.getText() : null);
        BigDecimal valorPago = parseBigDecimal(txtValorPago != null ? txtValorPago.getText() : null);


        BigDecimal valorTotalCalculado = alimentacao
                                         .add(transporte)
                                         .add(cargas)
                                         .subtract(descontoTarifa);

        if (valorTotalCalculado.compareTo(BigDecimal.ZERO) < 0) {
            valorTotalCalculado = BigDecimal.ZERO;
        }

        BigDecimal valorAPagarCalculado = valorTotalCalculado.subtract(descontoGeral);
        if (valorAPagarCalculado.compareTo(BigDecimal.ZERO) < 0) {
            valorAPagarCalculado = BigDecimal.ZERO;
        }

        BigDecimal trocoCalculado = BigDecimal.ZERO;
        BigDecimal devedorCalculado = BigDecimal.ZERO;

        if (valorPago.compareTo(valorAPagarCalculado) > 0) {
            trocoCalculado = valorPago.subtract(valorAPagarCalculado);
        } else {
            devedorCalculado = valorAPagarCalculado.subtract(valorPago);
        }

        if (txtTotal != null) txtTotal.setText(String.format("%,.2f", valorTotalCalculado));
        if (txtAPagar != null) txtAPagar.setText(String.format("%,.2f", valorAPagarCalculado));
        if (txtTroco != null) txtTroco.setText(String.format("%,.2f", trocoCalculado));
        if (txtDevedor != null) txtDevedor.setText(String.format("%,.2f", devedorCalculado));
    }


    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String cleanedText = text.replace("R$", "").replace(".", "").replace(",", ".").trim();
        try {
            return new BigDecimal(cleanedText);
        } catch (NumberFormatException e) {
            System.err.println("Erro de formato numérico ao parsear BigDecimal: " + text + " -> " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }


    private void configurarTabelaPassagens() {
        if (tablePassagens == null) {
            System.err.println("tablePassagens é NULL em configurarTabelaPassagens.");
            return;
        }
        if (colNumBilhete != null) {
            colNumBilhete.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getNumBilhete())));
        }
        if (colPassageiro != null) {
            colPassageiro.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNomePassageiro()));
        }
        if (colDataNascimento != null) {
            colDataNascimento.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDataNascimento()));
            colDataNascimento.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, LocalDate>() {
                private final DateTimeFormatter cellFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                @Override
                protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(cellFormatter.format(item));
                    }
                }
            });
        }
        if (colNumeroDoc != null) {
            colNumeroDoc.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumeroDoc()));
        }
        if (colNacionalidade != null) {
            colNacionalidade.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNacionalidade()));
        }
        if (colOrigem != null) {
            colOrigem.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOrigem()));
        }
        if (colDestino != null) {
            colDestino.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDestino()));
        }
        if (colValor != null) {
            colValor.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorTotal()));
            colValor.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("R$ %,.2f", item));
                    }
                }
            });
        }
        if (colValorDesconto != null) {
            colValorDesconto.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorDesconto()));
            colValorDesconto.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("R$ %,.2f", item));
                    }
                }
            });
        }
        if (colValorAPagar != null) {
            colValorAPagar.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorAPagar()));
            colValorAPagar.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("R$ %,.2f", item));
                    }
                }
            });
        }
        if (colValorPago != null) {
            colValorPago.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getValorPago()));
            colValorPago.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("R$ %,.2f", item));
                    }
                }
            });
        }
        if (colDevedor != null) {
            colDevedor.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDevedor()));
            colDevedor.setCellFactory(column -> new javafx.scene.control.TableCell<Passagem, BigDecimal>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("R$ %,.2f", item));
                    }
                }
            });
        }
        carregarPassagensNaTabela();
    }

    private void carregarPassagensNaTabela() {
        try {
            List<Passagem> passagens = passagemDAO.listarTodos();
            if (tablePassagens != null) tablePassagens.setItems(FXCollections.observableArrayList(passagens));
            if (txtTotalPassageiros != null) {
                txtTotalPassageiros.setText(String.valueOf(passagens.size()));
            }
            // A habilitação/desabilitação de Editar/Excluir agora é manipulada pelo listener da tabela
            // e pelo estado geral da tela (handleNovo, handleSalvar, handleCancelar)
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro ao carregar passagens na tabela: " + e.getMessage());
        }
    }

    private void adicionarListenerAoCampoPesquisar() {
        if (txtPesquisar != null) {
            txtPesquisar.textProperty().addListener((obs, oldText, newText) -> {
                if (newText == null || newText.trim().isEmpty()) {
                    carregarPassagensNaTabela();
                } else {
                    handleFiltrar(null);
                }
            });
        }
    }

    private void adicionarListenerDeSelecaoNaTabela() {
        if (tablePassagens != null) {
            tablePassagens.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    // Se não estiver em modo de nova venda/edição, habilita editar/excluir
                    if (passagemEmEdicao == null && !btnNovo.isDisabled()) { // Adicionado !btnNovo.isDisabled()
                        if (btnEditar != null) btnEditar.setDisable(false);
                        if (btnExcluir != null) btnExcluir.setDisable(false);
                    }
                } else {
                    // Se nada estiver selecionado, desabilita editar/excluir, a menos que estejamos em nova venda
                    if (passagemEmEdicao == null && !btnNovo.isDisabled()) { // Adicionado !btnNovo.isDisabled()
                        if (btnEditar != null) btnEditar.setDisable(true);
                        if (btnExcluir != null) btnExcluir.setDisable(true);
                    }
                }

                // Auto-carrega para edição se uma nova seleção for feita E o sistema não estiver em modo de nova venda
                if (newSelection != null && oldSelection != newSelection && passagemEmEdicao == null && !btnNovo.isDisabled()) {
                    handleEditar(null);
                }
            });
        }
    }

    private void aplicarAutoCompleteEmComboBoxes() {
        if (cmbPassageiroAuto != null) new AutoCompleteComboBoxListener<>(cmbPassageiroAuto);
        if (cmbRota != null) new AutoCompleteComboBoxListener<>(cmbRota);
        if (cmbTipoPassagemAux != null) new AutoCompleteComboBoxListener<>(cmbTipoPassagemAux); // Linha a ser comentada temporariamente para teste
        if (cmbFormaPagamento != null) new AutoCompleteComboBoxListener<>(cmbFormaPagamento);
        if (cmbCaixa != null) new AutoCompleteComboBoxListener<>(cmbCaixa);
        if (cmbSexo != null) new AutoCompleteComboBoxListener<>(cmbSexo);
        if (cmbTipoDoc != null) new AutoCompleteComboBoxListener<>(cmbTipoDoc);
        if (cmbNacionalidade != null) new AutoCompleteComboBoxListener<>(cmbNacionalidade);
        if (cmbAgenteAux != null) new AutoCompleteComboBoxListener<>(cmbAgenteAux);
        if (cmbAcomodacao != null) new AutoCompleteComboBoxListener<>(cmbAcomodacao);
    }

    private void configurarNavegacaoEntreCampos() {
        Node cmbPassageiroEditor = (cmbPassageiroAuto != null) ? cmbPassageiroAuto.getEditor() : null;
        Node dpDataNascimentoEditor = (dpDataNascimento != null) ? dpDataNascimento.getEditor() : null;

        List<Node> focusableNodes = new java.util.ArrayList<>();
        if (cmbPassageiroEditor != null) focusableNodes.add(cmbPassageiroEditor);
        if (txtNumeroDoc != null) focusableNodes.add(txtNumeroDoc);
        if (dpDataNascimentoEditor != null) focusableNodes.add(dpDataNascimentoEditor);
        if (txtNascimentoMask != null) focusableNodes.add(txtNascimentoMask);
        if (txtIdade != null) focusableNodes.add(txtIdade);
        if (cmbSexo != null) focusableNodes.add(cmbSexo);
        if (cmbTipoDoc != null) focusableNodes.add(cmbTipoDoc);
        if (cmbNacionalidade != null) focusableNodes.add(cmbNacionalidade);
        if (cmbTipoPassagemAux != null) focusableNodes.add(cmbTipoPassagemAux);
        if (cmbRota != null) focusableNodes.add(cmbRota);
        if (cmbAcomodacao != null) focusableNodes.add(cmbAcomodacao);
        if (cmbAgenteAux != null) focusableNodes.add(cmbAgenteAux);
        if (txtRequisicao != null) focusableNodes.add(txtRequisicao);
        if (txtAlimentacao != null) focusableNodes.add(txtAlimentacao);
        if (txtTransporte != null) focusableNodes.add(txtTransporte);
        if (txtCargas != null) focusableNodes.add(txtCargas);
        if (txtDescontoTarifa != null) focusableNodes.add(txtDescontoTarifa);
        if (txtDesconto != null) focusableNodes.add(txtDesconto);
        if (txtValorPago != null) focusableNodes.add(txtValorPago);
        if (cmbFormaPagamento != null) focusableNodes.add(cmbFormaPagamento);
        if (cmbCaixa != null) focusableNodes.add(cmbCaixa);
        if (btnSalvar != null) focusableNodes.add(btnSalvar);


        for (int i = 0; i < focusableNodes.size(); i++) {
            final int nextIndex = (i + 1) % focusableNodes.size();
            Node currentNode = focusableNodes.get(i);

            currentNode.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();

                    Node nextNode = focusableNodes.get(nextIndex);

                    if (nextNode instanceof TextField) {
                        ((TextField) nextNode).requestFocus();
                        ((TextField) nextNode).selectAll();
                    } else if (nextNode instanceof DatePicker) {
                        ((DatePicker) nextNode).getEditor().requestFocus();
                        ((DatePicker) nextNode).getEditor().selectAll();
                    } else if (nextNode instanceof ComboBox) {
                        ((ComboBox) nextNode).requestFocus();
                        ((ComboBox) nextNode).show();
                        if (((ComboBox) nextNode).isEditable()) {
                            ((ComboBox) nextNode).getEditor().selectAll();
                        }
                    } else if (nextNode instanceof Button) {
                        ((Button) nextNode).fire();
                    }
                }
            });
        }
    }


    @FXML
    private void handleNovo(ActionEvent event) {
        // Fluxo para iniciar uma NOVA venda
        // Só é chamado quando o botão "Novo" é clicado, após a inicialização da tela
        if (viagemSelecionada == null) {
            showAlert(AlertType.INFORMATION, "Viagem Necessária", "Nenhuma viagem ativa encontrada. Por favor, selecione uma viagem na lista (na tela principal).");
            return; // Impede continuar se não há viagem ativa
        }
        
        limparCamposParaNovaVendaAutomatica(); // Limpa e prepara para a próxima venda
        habilitarCamposParaNovaVenda(true); // Habilita os campos de entrada
        if (btnNovo != null) btnNovo.setDisable(true); // Desabilita o botão Novo, pois já estamos no modo de nova venda
        
        // Foca no primeiro campo de entrada
        if (cmbPassageiroAuto != null) cmbPassageiroAuto.requestFocus();
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        String nomePassageiroDigitado = (cmbPassageiroAuto != null && cmbPassageiroAuto.getEditor() != null) ? cmbPassageiroAuto.getEditor().getText() : "";
        String selectedRotaStr = (cmbRota != null) ? cmbRota.getSelectionModel().getSelectedItem() : null;

        if (nomePassageiroDigitado.isEmpty() || cmbViagem.getValue() == null ||
            dpDataNascimento.getValue() == null || cmbTipoPassagemAux.getValue() == null ||
            cmbAcomodacao.getValue() == null || selectedRotaStr == null || selectedRotaStr.trim().isEmpty() ||
            cmbAgenteAux.getValue() == null || cmbFormaPagamento.getValue() == null ||
            cmbCaixa.getValue() == null || (txtTotal != null && parseBigDecimal(txtTotal.getText()).compareTo(BigDecimal.ZERO) <= 0)) {
            showAlert(AlertType.WARNING, "Campos Obrigatórios", "Por favor, preencha todos os campos obrigatórios e verifique o valor total da passagem.");
            return;
        }

        if (this.viagemSelecionada == null || this.viagemSelecionada.getId() == null || this.viagemSelecionada.getId() == 0L) {
            showAlert(AlertType.ERROR, "Erro de Viagem", "Nenhuma viagem ativa selecionada ou carregada. Por favor, selecione uma viagem válida ou ative uma.");
            return;
        }

        Passageiro passageiroASalvar;

        try {
            Passageiro passageiroPorDoc = null;
            if (txtNumeroDoc != null && txtNumeroDoc.getText() != null && !txtNumeroDoc.getText().trim().isEmpty()) {
                passageiroPorDoc = passageiroDAO.buscarPorDoc(txtNumeroDoc.getText().trim());
            }

            if (passagemEmEdicao == null) { // Se é uma nova passagem
                if (passageiroPorDoc != null) { // E o passageiro já existe
                    passageiroASalvar = passageiroPorDoc;
                    showAlert(AlertType.INFORMATION, "Passageiro Existente", "Passageiro com este documento já cadastrado. Usaremos o cadastro existente.");
                } else { // Passageiro novo
                    passageiroASalvar = new Passageiro();
                }
            } else { // Se estamos editando uma passagem existente
                passageiroASalvar = passageiroEmEdicao;
                if (passageiroPorDoc != null && !Objects.equals(passageiroPorDoc.getId(), passageiroASalvar.getId())) {
                    showAlert(AlertType.WARNING, "Documento Duplicado", "O número de documento informado já pertence a outro passageiro (ID: " + passageiroPorDoc.getId() + "). Por favor, corrija.");
                    return;
                }
            }

            passageiroASalvar.setNome(nomePassageiroDigitado);
            if (txtNumeroDoc != null) passageiroASalvar.setNumeroDoc(txtNumeroDoc.getText());
            if (dpDataNascimento != null) passageiroASalvar.setDataNascimento(dpDataNascimento.getValue());
            if (cmbTipoDoc != null) passageiroASalvar.setTipoDoc(cmbTipoDoc.getValue());
            if (cmbSexo != null) passageiroASalvar.setSexo(cmbSexo.getValue());
            if (cmbNacionalidade != null) passageiroASalvar.setNacionalidade(cmbNacionalidade.getValue());

            boolean passageiroSalvoComSucesso = false;
            if (passageiroASalvar.getId() == null || passageiroASalvar.getId() == 0L) {
                passageiroSalvoComSucesso = passageiroDAO.inserir(passageiroASalvar);
            } else {
                passageiroSalvoComSucesso = passageiroDAO.atualizar(passageiroASalvar);
            }

            if (!passageiroSalvoComSucesso) {
                showAlert(AlertType.ERROR, "Erro", "Falha ao cadastrar/atualizar o passageiro.");
                return;
            }
            passageiroEmEdicao = passageiroASalvar; // Atualiza a referência do passageiro em edição
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Erro ao processar dados do passageiro: " + e.getMessage()); // Corrigido aqui
            e.printStackTrace();
            return;
        }

        Passagem passagemParaSalvar;

        if (this.passagemEmEdicao == null) { // Se é uma nova passagem
            passagemParaSalvar = new Passagem();
            try {
                if (txtNumBilhete != null) passagemParaSalvar.setNumBilhete(Integer.parseInt(txtNumBilhete.getText()));
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Erro", "Número do bilhete inválido.");
                return;
            }
        } else { // Se estamos editando uma passagem existente
            passagemParaSalvar = this.passagemEmEdicao;
        }

        if (passageiroEmEdicao == null || passageiroEmEdicao.getId() == null || passageiroEmEdicao.getId() == 0L) {
            showAlert(AlertType.ERROR, "Erro Interno", "O passageiro não possui um ID válido. Não foi possível salvar a passagem.");
            return;
        }
        passagemParaSalvar.setIdPassageiro(passageiroEmEdicao.getId());

        try {
            String[] rotaParts = selectedRotaStr.split(" - ");
            String origem = rotaParts[0].trim();
            String destino = (rotaParts.length > 1) ? rotaParts[1].trim() : "";

            Integer idRotaInteger = auxiliaresDAO.obterIdRotaPelaOrigemDestino(origem, destino);
            if (idRotaInteger == null) {
                showAlert(AlertType.ERROR, "Erro de Rota", "Não foi possível identificar a Rota selecionada no banco de dados.");
                return;
            }
            passagemParaSalvar.setIdRota(idRotaInteger.longValue());
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Falha ao obter ID da rota para a passagem: " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro inesperado ao obter ID da rota para a passagem: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (viagemSelecionada.getId() == null) {
            showAlert(AlertType.ERROR, "Erro de Viagem", "A viagem selecionada não possui um ID válido.");
            return;
        }
        passagemParaSalvar.setIdViagem(viagemSelecionada.getId());
        passagemParaSalvar.setIdHorarioSaida(viagemSelecionada.getIdHorarioSaida() != null ? viagemSelecionada.getIdHorarioSaida().intValue() : null); 
        passagemParaSalvar.setDescricaoHorarioSaida(viagemSelecionada.getHorarioSaidaStr());

        if (txtIdade != null && txtIdade.getText() != null && !txtIdade.getText().isEmpty()) {
            try {
                passagemParaSalvar.setIdade(Integer.parseInt(txtIdade.getText()));
            } catch (NumberFormatException e) { /* Ignorar */ }
        }

        if (cmbTipoPassagemAux != null) passagemParaSalvar.setTipoPassagemAux(cmbTipoPassagemAux.getValue());
        if (cmbAgenteAux != null) passagemParaSalvar.setAgenteAux(cmbAgenteAux.getValue());
        if (cmbAcomodacao != null) passagemParaSalvar.setAcomodacao(cmbAcomodacao.getValue());
        if (cmbFormaPagamento != null) passagemParaSalvar.setFormaPagamento(cmbFormaPagamento.getValue());
        if (cmbCaixa != null) passagemParaSalvar.setCaixa(cmbCaixa.getValue());

        String[] rotaSplit = selectedRotaStr.split(" - ");
        if (rotaSplit.length >= 2) {
            passagemParaSalvar.setOrigem(rotaSplit[0].trim());
            passagemParaSalvar.setDestino(rotaSplit[1].trim());
        } else {
            passagemParaSalvar.setOrigem(selectedRotaStr);
            passagemParaSalvar.setDestino("");
        }

        passagemParaSalvar.setDataViagem(viagemSelecionada.getDataViagem());
        passagemParaSalvar.setStrViagem(viagemSelecionada.toString());

        if (txtRequisicao != null) passagemParaSalvar.setRequisicao(txtRequisicao.getText());

        if (txtAlimentacao != null) passagemParaSalvar.setValorAlimentacao(parseBigDecimal(txtAlimentacao.getText()));
        if (txtTransporte != null) passagemParaSalvar.setValorTransporte(parseBigDecimal(txtTransporte.getText()));
        if (txtCargas != null) passagemParaSalvar.setValorCargas(parseBigDecimal(txtCargas.getText()));
        if (txtDescontoTarifa != null) passagemParaSalvar.setValorDescontoTarifa(parseBigDecimal(txtDescontoTarifa.getText()));
        if (txtDesconto != null) passagemParaSalvar.setValorDesconto(parseBigDecimal(txtDesconto.getText()));
        if (txtTotal != null) passagemParaSalvar.setValorTotal(parseBigDecimal(txtTotal.getText()));
        if (txtAPagar != null) passagemParaSalvar.setValorAPagar(parseBigDecimal(txtAPagar.getText()));
        if (txtValorPago != null) passagemParaSalvar.setValorPago(parseBigDecimal(txtValorPago.getText()));
        if (txtDevedor != null) passagemParaSalvar.setDevedor(parseBigDecimal(txtDevedor.getText()));
        if (txtTroco != null) passagemParaSalvar.setTroco(parseBigDecimal(txtTroco.getText()));


        passagemParaSalvar.setStatusPassagem("EMITIDA");

        boolean sucesso = false;
        try {
            if (this.passagemEmEdicao == null) {
                sucesso = passagemDAO.inserir(passagemParaSalvar);
            } else {
                sucesso = passagemDAO.atualizar(passagemParaSalvar);
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro no DAO", "Erro ao interagir com o banco de dados: " + e.getMessage());
            e.printStackTrace();
        }

        if (sucesso) {
            showAlert(AlertType.INFORMATION, "Sucesso", "Passagem salva com sucesso!");
            carregarPassagensNaTabela();
            // ** FLUXO REVISADO: APÓS SALVAR, LIMPA PARA NOVA VENDA E MANTEM CAMPOS HABILITADOS **
            limparCamposParaNovaVendaAutomatica(); // Chama o novo método de limpeza
            // O btnNovo permanece desabilitado, pois já estamos no fluxo de "nova venda"
        } else {
            showAlert(AlertType.ERROR, "Erro", "Erro ao salvar passagem. Verifique os dados e o console.");
        }
    }

    @FXML
    private void handleCancelar(ActionEvent event) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Cancelamento");
        alert.setHeaderText("Você tem certeza que deseja cancelar a operação?");
        alert.setContentText("Todas as informações não salvas serão perdidas.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            configurarEstadoInicialDaTela(); // Volta ao estado inicial (Novo habilitado, campos desabilitados)
        }
    }

    @FXML
    private void handleFiltrar(ActionEvent event) {
        String modo = (cmbPesquisarModo != null) ? cmbPesquisarModo.getSelectionModel().getSelectedItem() : null;
        String texto = (txtPesquisar != null) ? txtPesquisar.getText() : "";

        if (modo == null || modo.isEmpty()) {
            showAlert(AlertType.WARNING, "Pesquisa", "Selecione um modo de pesquisa.");
            return;
        }
        if (texto.trim().isEmpty()) {
            carregarPassagensNaTabela();
            return;
        }

        try {
            List<Passagem> passagensFiltradas = passagemDAO.filtrar(modo, texto);
            if (tablePassagens != null) tablePassagens.setItems(FXCollections.observableArrayList(passagensFiltradas));
            if (txtTotalPassageiros != null) {
                txtTotalPassageiros.setText(String.valueOf(passagensFiltradas.size()));
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Pesquisa", "Ocorreu um erro ao filtrar as passagens: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditar(ActionEvent event) {
        Passagem selectedPassagem = (tablePassagens != null) ? tablePassagens.getSelectionModel().getSelectedItem() : null;
        if (selectedPassagem == null) {
            showAlert(AlertType.WARNING, "Nenhuma Seleção", "Selecione uma passagem na tabela para editar.");
            return;
        }

        // Se já houver uma passagem em edição/criação, impede a edição de outra
        if (passagemEmEdicao != null && !Objects.equals(passagemEmEdicao.getId(), selectedPassagem.getId())) {
            showAlert(AlertType.INFORMATION, "Edição em Andamento", "Há uma passagem sendo editada ou criada. Cancele ou salve antes de editar outra.");
            return;
        }
        // Se o botão Novo está habilitado, significa que a tela está no estado inicial.
        // Se clicarmos em editar de uma passagem na tabela, devemos desabilitar o Novo
        if (btnNovo != null && !btnNovo.isDisabled()) {
             btnNovo.setDisable(true);
        }

        limparTodosOsCamposGUI(); // Limpa antes de preencher
        habilitarCamposParaNovaVenda(true); // Habilita campos para edição

        passagemEmEdicao = selectedPassagem; // Define a passagem que está sendo editada

        if (txtNumBilhete != null) txtNumBilhete.setText(String.valueOf(selectedPassagem.getNumBilhete()));
        if (txtNumBilhete != null) txtNumBilhete.setDisable(true); // Número do bilhete não deve ser editável

        try {
            Passageiro p = passageiroDAO.buscarPorId(selectedPassagem.getIdPassageiro());
            if (p != null) {
                preencherCamposPassageiro(p);
            } else {
                limparCamposPassageiroGUI();
                showAlert(AlertType.WARNING, "Passageiro Não Encontrado", "Os dados do passageiro desta passagem (ID: " + selectedPassagem.getIdPassageiro() + ") não foram encontrados no sistema. Por favor, preencha manualmente.");
                passageiroEmEdicao = null;
            }

            Viagem v = viagemDAO.buscarPorId(selectedPassagem.getIdViagem());
            if (v != null) {
                this.viagemSelecionada = v; // Define a viagem selecionada
                if (cmbViagem != null && cmbViagem.getItems().contains(v.toString())) {
                    cmbViagem.setValue(v.toString());
                } else {
                    showAlert(AlertType.WARNING, "Viagem Não Disponível", "A viagem associada a esta passagem não está na lista de viagens disponíveis. Verifique o cadastro da viagem.");
                    if (cmbViagem != null) cmbViagem.getSelectionModel().clearSelection();
                }
                if (dpDataViagem != null) dpDataViagem.setValue(selectedPassagem.getDataViagem());
                if (txtHorario != null) txtHorario.setText(selectedPassagem.getDescricaoHorarioSaida());
            } else {
                showAlert(AlertType.WARNING, "Viagem Não Encontrada", "A viagem associada a esta passagem não foi encontrada no sistema. Os campos de viagem não serão preenchidos.");
                if (dpDataViagem != null) dpDataViagem.setValue(null);
                if (txtHorario != null) txtHorario.clear();
                if (cmbViagem != null) cmbViagem.getSelectionModel().clearSelection();
                this.viagemSelecionada = null;
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Erro ao carregar dados para edição: " + e.getMessage());
            e.printStackTrace();
            limparCamposPassageiroGUI();
            if (cmbViagem != null) cmbViagem.getSelectionModel().clearSelection();
            if (dpDataViagem != null) dpDataViagem.setValue(null);
            if (txtHorario != null) txtHorario.clear();
            if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
            this.viagemSelecionada = null;
            passageiroEmEdicao = null;
        }

        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.setValue(selectedPassagem.getTipoPassagemAux());
        if (cmbAgenteAux != null) cmbAgenteAux.setValue(selectedPassagem.getAgenteAux());
        if (cmbAcomodacao != null) cmbAcomodacao.setValue(selectedPassagem.getAcomodacao());
        if (txtRequisicao != null) txtRequisicao.setText(selectedPassagem.getRequisicao());

        if (txtAlimentacao != null) txtAlimentacao.setText(String.format("%,.2f", selectedPassagem.getValorAlimentacao()));
        if (txtTransporte != null) txtTransporte.setText(String.format("%,.2f", selectedPassagem.getValorTransporte()));
        if (txtCargas != null) txtCargas.setText(String.format("%,.2f", selectedPassagem.getValorCargas()));
        if (txtDescontoTarifa != null) txtDescontoTarifa.setText(String.format("%,.2f", selectedPassagem.getValorDescontoTarifa()));
        if (txtTotal != null) txtTotal.setText(String.format("%,.2f", selectedPassagem.getValorTotal()));
        if (txtDesconto != null) txtDesconto.setText(String.format("%,.2f", selectedPassagem.getValorDesconto()));
        if (txtAPagar != null) txtAPagar.setText(String.format("%,.2f", selectedPassagem.getValorAPagar()));
        if (txtValorPago != null) txtValorPago.setText(String.format("%,.2f", selectedPassagem.getValorPago()));
        if (txtDevedor != null) txtDevedor.setText(String.format("%,.2f", selectedPassagem.getDevedor()));
        if (txtTroco != null) txtTroco.setText(String.format("%,.2f", selectedPassagem.getTroco()));

        if (cmbFormaPagamento != null) cmbFormaPagamento.setValue(selectedPassagem.getFormaPagamento());
        if (cmbCaixa != null) cmbCaixa.setValue(selectedPassagem.getCaixa());

        if (txtHorario != null) txtHorario.setText(selectedPassagem.getDescricaoHorarioSaida());
        
        // Assegura que a rota esteja selecionada se os campos de origem/destino existirem
        if (cmbRota != null) {
            String rotaConcatenada = selectedPassagem.getOrigem() + " - " + selectedPassagem.getDestino();
            if (cmbRota.getItems().contains(rotaConcatenada)) {
                cmbRota.setValue(rotaConcatenada);
            } else {
                // Se a rota não estiver na lista, adicione-a temporariamente
                cmbRota.getItems().add(0, rotaConcatenada);
                cmbRota.setValue(rotaConcatenada);
            }
        }

        // Habilita Salvar/Cancelar e desabilita Novo/Editar/Excluir
        if (btnSalvar != null) btnSalvar.setDisable(false);
        if (btnCancelar != null) btnCancelar.setDisable(false);
        if (btnNovo != null) btnNovo.setDisable(true); 
        if (btnEditar != null) btnEditar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }

    @FXML
    private void handleExcluir(ActionEvent event) {
        Passagem selectedPassagem = (tablePassagens != null) ? tablePassagens.getSelectionModel().getSelectedItem() : null;
        if (selectedPassagem == null) {
            showAlert(AlertType.WARNING, "Nenhuma Seleção", "Selecione uma passagem na tabela para excluir.");
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Exclusão");
        alert.setHeaderText("Tem certeza que deseja excluir a passagem " + selectedPassagem.getNumBilhete() + "?");
        alert.setContentText("Esta ação não pode ser desfeita.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean sucesso = passagemDAO.excluir(selectedPassagem.getId());
                if (sucesso) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Passagem excluída com sucesso!");
                    carregarPassagensNaTabela(); // Recarrega a tabela
                    configurarEstadoInicialDaTela(); // Volta ao estado inicial
                } else {
                    showAlert(AlertType.ERROR, "Erro", "Falha ao excluir passagem.");
                }
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Erro ao excluir passagem: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleImprimirBilhete(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Bilhete não implementado.");
    }

    @FXML
    private void handleImprimirLista(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Lista não implementado.");
    }

    @FXML
    private void handleRelatorio(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Relatório não implementado.");
    }

    @FXML
    private void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        if (source != null && source.getScene() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }

    private void limparCamposTarifa() {
        if (txtAlimentacao != null) txtAlimentacao.setText("0,00");
        if (txtTransporte != null) txtTransporte.setText("0,00");
        if (txtCargas != null) txtCargas.setText("0,00");
        if (txtDescontoTarifa != null) txtDescontoTarifa.setText("0,00");
        if (txtTotal != null) txtTotal.setText("0,00");
        if (txtDesconto != null) txtDesconto.setText("0,00");
        if (txtAPagar != null) txtAPagar.setText("0,00");
        if (txtValorPago != null) txtValorPago.setText("0,00");
        if (txtDevedor != null) txtDevedor.setText("0,00");
        if (txtTroco != null) txtTroco.setText("0,00");
        calcularValoresPassagem();
    }

    // NOVO MÉTODO DE LIMPEZA PARA O FLUXO AUTOMÁTICO APÓS SALVAR
    private void limparCamposParaNovaVendaAutomatica() {
        limparCamposPassageiroGUI(); // Limpa os campos do passageiro
        limparCamposTarifa(); // Limpa os valores de tarifa/totais
        
        if (txtRequisicao != null) txtRequisicao.clear();

        // Limpa a seleção dos ComboBoxes, mas não as opções
        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.getSelectionModel().clearSelection();
        if (cmbAgenteAux != null) cmbAgenteAux.getSelectionModel().clearSelection();
        if (cmbAcomodacao != null) cmbAcomodacao.getSelectionModel().clearSelection();
        if (cmbFormaPagamento != null) cmbFormaPagamento.getSelectionModel().clearSelection();
        if (cmbCaixa != null) cmbCaixa.getSelectionModel().clearSelection();
        if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();

        // Redefine passagemEmEdicao e passageiroEmEdicao para null para indicar nova venda
        passagemEmEdicao = null;
        passageiroEmEdicao = null;

        // Gera o próximo número de bilhete para a nova venda
        try {
            if (txtNumBilhete != null) txtNumBilhete.setText(String.valueOf(passagemDAO.obterProximoBilhete()));
        } catch (Exception e) {
            if (txtNumBilhete != null) txtNumBilhete.setText("Erro");
        }
        
        // Garante que a viagem ativa continua selecionada e seus dados visíveis
        // (cmbViagem, dpDataViagem, txtHorario já estão desabilitados para edição manual)
        if (viagemSelecionada != null) {
            if (cmbViagem != null) cmbViagem.setValue(viagemSelecionada.toString());
            if (dpDataViagem != null) dpDataViagem.setValue(viagemSelecionada.getDataViagem());
            if (txtHorario != null) txtHorario.setText(viagemSelecionada.getHorarioSaidaStr());
        } else {
            // Se por algum motivo a viagem ativa sumiu, volta para o estado inicial desabilitado
            // Isso fará o alert de "Viagem Ativa Necessária"
            configurarEstadoInicialDaTela(); 
            return;
        }
        
        // Os campos de entrada já devem estar habilitados por causa do fluxo do "Novo"
        // Foco no primeiro campo de entrada
        if (cmbPassageiroAuto != null) cmbPassageiroAuto.requestFocus();

        // Limpa a seleção da tabela, pois agora estamos inserindo uma nova passagem
        if (tablePassagens != null) tablePassagens.getSelectionModel().clearSelection();
        // Desabilita Editar/Excluir, pois não há item selecionado para edição
        if (btnEditar != null) btnEditar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);
    }
    
    // Método de limpeza geral para quando a tela inicia ou cancela uma operação
    private void limparTodosOsCamposGUI() {
        limparCamposPassageiroGUI();
        limparCamposTarifa();

        if (txtNumBilhete != null) txtNumBilhete.clear();
        if (txtNumBilhete != null) txtNumBilhete.setDisable(true); // Desabilitar inicialmente

        if (txtRequisicao != null) txtRequisicao.clear();

        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.getSelectionModel().clearSelection();
        if (cmbAgenteAux != null) cmbAgenteAux.getSelectionModel().clearSelection();
        if (cmbAcomodacao != null) cmbAcomodacao.getSelectionModel().clearSelection();
        if (cmbFormaPagamento != null) cmbFormaPagamento.getSelectionModel().clearSelection();
        if (cmbCaixa != null) cmbCaixa.getSelectionModel().clearSelection();
        if (cmbRota != null) cmbRota.getSelectionModel().clearSelection();
        
        // NOVO: Limpa a seleção do cmbViagem e zera os campos de data/horário de viagem
        if (cmbViagem != null) cmbViagem.getSelectionModel().clearSelection(); 
        if (dpDataViagem != null) dpDataViagem.setValue(null);
        if (txtDataViagemMask != null) txtDataViagemMask.clear();
        if (txtHorario != null) txtHorario.clear();

        if (txtPesquisar != null) txtPesquisar.clear();

        passagemEmEdicao = null;
        passageiroEmEdicao = null;
        viagemSelecionada = null; // Zera a viagem selecionada também
        
        if (tablePassagens != null) tablePassagens.getSelectionModel().clearSelection();
        if (btnEditar != null) btnEditar.setDisable(true);
        if (btnExcluir != null) btnExcluir.setDisable(true);

        // Esconde os ComboBoxes de auto-completar que podem estar abertos
        if (cmbPassageiroAuto != null) cmbPassageiroAuto.hide();
        if (cmbRota != null) cmbRota.hide();
        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.hide();
        if (cmbFormaPagamento != null) cmbFormaPagamento.hide();
        if (cmbCaixa != null) cmbCaixa.hide();
        if (cmbSexo != null) cmbSexo.hide();
        if (cmbTipoDoc != null) cmbTipoDoc.hide();
        if (cmbNacionalidade != null) cmbNacionalidade.hide();
        if (cmbAgenteAux != null) cmbAgenteAux.hide();
        if (cmbAcomodacao != null) cmbAcomodacao.hide();
    }


    private void habilitarCamposParaNovaVenda(boolean enable) {
        if (cmbPassageiroAuto != null) cmbPassageiroAuto.setDisable(!enable);
        if (txtNumeroDoc != null) txtNumeroDoc.setDisable(!enable);
        if (dpDataNascimento != null) dpDataNascimento.setDisable(!enable);
        if (txtNascimentoMask != null) txtNascimentoMask.setDisable(!enable);
        if (cmbTipoDoc != null) cmbTipoDoc.setDisable(!enable);
        if (cmbNacionalidade != null) cmbNacionalidade.setDisable(!enable);
        if (cmbSexo != null) cmbSexo.setDisable(!enable);

        if (cmbTipoPassagemAux != null) cmbTipoPassagemAux.setDisable(!enable);
        if (cmbAcomodacao != null) cmbAcomodacao.setDisable(!enable);
        if (cmbAgenteAux != null) cmbAgenteAux.setDisable(!enable);
        if (txtRequisicao != null) txtRequisicao.setDisable(!enable);
        if (cmbFormaPagamento != null) cmbFormaPagamento.setDisable(!enable);
        if (cmbCaixa != null) cmbCaixa.setDisable(!enable);

        if (txtAlimentacao != null) txtAlimentacao.setDisable(!enable);
        if (txtTransporte != null) txtTransporte.setDisable(!enable);
        if (txtCargas != null) txtCargas.setDisable(!enable);
        if (txtDescontoTarifa != null) txtDescontoTarifa.setDisable(!enable);
        if (txtDesconto != null) txtDesconto.setDisable(!enable);
        if (txtValorPago != null) txtValorPago.setDisable(!enable);

        // Campos de Viagem devem ser somente leitura (display)
        if (cmbViagem != null) cmbViagem.setDisable(true); // Sempre desabilitado para edição manual
        if (dpDataViagem != null) dpDataViagem.setDisable(true);
        if (txtDataViagemMask != null) txtDataViagemMask.setDisable(true);
        if (txtHorario != null) txtHorario.setDisable(true);

        if (cmbRota != null) cmbRota.setDisable(!enable);

        if (btnSalvar != null) btnSalvar.setDisable(!enable);
        if (btnCancelar != null) btnCancelar.setDisable(!enable);
        
        // Pesquisa e Tabela devem ser desabilitadas quando estivermos em modo de Nova Venda/Edição
        if (cmbPesquisarModo != null) cmbPesquisarModo.setDisable(enable);
        if (txtPesquisar != null) txtPesquisar.setDisable(enable);
        if (btnFiltrar != null) btnFiltrar.setDisable(enable);
        if (tablePassagens != null) tablePassagens.setDisable(enable); // Desabilita a tabela
        
        // Habilitação do Novo/Editar/Excluir é mais granular, tratada nos seus próprios handlers/configurações
        // Não manipular btnNovo aqui, ele é tratado em configurarEstadoInicialDaTela e handleNovo
        // btnEditar e btnExcluir são manipulados pelo listener da tabela e no handleEditar/Excluir
    }


    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}