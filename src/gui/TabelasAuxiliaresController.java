package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TabPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import dao.AuxiliaresDAO;
// A classe AuxiliarItem agora é importada implicitamente pois está no mesmo pacote "gui"
// e definida em seu próprio arquivo AuxiliarItem.java

public class TabelasAuxiliaresController implements Initializable {

    private final AuxiliaresDAO auxDao = new AuxiliaresDAO();

    @FXML private TabPane tabPane;

    // --- ABA SEXO ---
    @FXML private TableView<AuxiliarItem> tableSexo;
    @FXML private TableColumn<AuxiliarItem, String> colSexoNome;
    @FXML private TextField txtSexoNome;
    @FXML private Button btnNovoSexo;
    @FXML private Button btnSalvarSexo;
    @FXML private Button btnExcluirSexo;
    private ObservableList<AuxiliarItem> listaSexo = FXCollections.observableArrayList();

    // --- ABA TIPO DOC ---
    @FXML private TableView<AuxiliarItem> tableTipoDoc;
    @FXML private TableColumn<AuxiliarItem, String> colTipoDocNome;
    @FXML private TextField txtTipoDocNome;
    @FXML private Button btnNovoTipoDoc;
    @FXML private Button btnSalvarTipoDoc;
    @FXML private Button btnExcluirTipoDoc;
    private ObservableList<AuxiliarItem> listaTipoDoc = FXCollections.observableArrayList();

    // --- ABA NACIONALIDADE ---
    @FXML private TableView<AuxiliarItem> tableNacionalidade;
    @FXML private TableColumn<AuxiliarItem, String> colNacionalidadeNome;
    @FXML private TextField txtNacionalidadeNome;
    @FXML private Button btnNovoNacionalidade;
    @FXML private Button btnSalvarNacionalidade;
    @FXML private Button btnExcluirNacionalidade;
    private ObservableList<AuxiliarItem> listaNacionalidade = FXCollections.observableArrayList();

    // --- ABA PASSAGEM AUX (Tipos de Passagem) ---
    @FXML private TableView<AuxiliarItem> tablePassagemAux;
    @FXML private TableColumn<AuxiliarItem, String> colPassagemAuxNome;
    @FXML private TextField txtPassagemAuxNome;
    @FXML private Button btnNovoPassagemAux;
    @FXML private Button btnSalvarPassagemAux;
    @FXML private Button btnExcluirPassagemAux;
    private ObservableList<AuxiliarItem> listaPassagemAux = FXCollections.observableArrayList();

    // --- ABA AGENTE AUX ---
    @FXML private TableView<AuxiliarItem> tableAgenteAux;
    @FXML private TableColumn<AuxiliarItem, String> colAgenteAuxNome;
    @FXML private TextField txtAgenteAuxNome;
    @FXML private Button btnNovoAgenteAux;
    @FXML private Button btnSalvarAgenteAux;
    @FXML private Button btnExcluirAgenteAux;
    private ObservableList<AuxiliarItem> listaAgenteAux = FXCollections.observableArrayList();
    
    // --- ABA HORÁRIO SAÍDA ---
    @FXML private TableView<AuxiliarItem> tableHorarioSaida;
    @FXML private TableColumn<AuxiliarItem, String> colHorarioSaidaNome;
    @FXML private TextField txtHorarioSaidaNome;
    @FXML private Button btnNovoHorarioSaida;
    @FXML private Button btnSalvarHorarioSaida;
    @FXML private Button btnExcluirHorarioSaida;
    private ObservableList<AuxiliarItem> listaHorarioSaida = FXCollections.observableArrayList();

    // --- ABA ACOMODAÇÃO ---
    @FXML private TableView<AuxiliarItem> tableAcomodacao;
    @FXML private TableColumn<AuxiliarItem, String> colAcomodacaoNome;
    @FXML private TextField txtAcomodacaoNome;
    @FXML private Button btnNovoAcomodacao;
    @FXML private Button btnSalvarAcomodacao;
    @FXML private Button btnExcluirAcomodacao;
    private ObservableList<AuxiliarItem> listaAcomodacao = FXCollections.observableArrayList();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sexo
        colSexoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableSexo.setItems(listaSexo);
        configurarSelecaoTabela(tableSexo, txtSexoNome);
        carregarDadosSexo();

        // Tipo Doc
        colTipoDocNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableTipoDoc.setItems(listaTipoDoc);
        configurarSelecaoTabela(tableTipoDoc, txtTipoDocNome);
        carregarDadosTipoDoc();

        // Nacionalidade
        colNacionalidadeNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableNacionalidade.setItems(listaNacionalidade);
        configurarSelecaoTabela(tableNacionalidade, txtNacionalidadeNome);
        carregarDadosNacionalidade();

        // Passagem Aux (Tipos de Passagem)
        colPassagemAuxNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tablePassagemAux.setItems(listaPassagemAux);
        configurarSelecaoTabela(tablePassagemAux, txtPassagemAuxNome);
        carregarDadosPassagemAux();

        // Agente Aux
        colAgenteAuxNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableAgenteAux.setItems(listaAgenteAux);
        configurarSelecaoTabela(tableAgenteAux, txtAgenteAuxNome);
        carregarDadosAgenteAux();
        
        // Horário Saída
        colHorarioSaidaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableHorarioSaida.setItems(listaHorarioSaida);
        configurarSelecaoTabela(tableHorarioSaida, txtHorarioSaidaNome);
        carregarDadosHorarioSaida();

        // Acomodação
        colAcomodacaoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tableAcomodacao.setItems(listaAcomodacao);
        configurarSelecaoTabela(tableAcomodacao, txtAcomodacaoNome);
        carregarDadosAcomodacao();
    }

    private void configurarSelecaoTabela(TableView<AuxiliarItem> tabela, TextField campoTexto) {
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                campoTexto.setText(newSelection.getNome());
            }
        });
    }

    // --- MÉTODOS SEXO ---
    private void carregarDadosSexo() {
        try {
            listaSexo.clear();
            List<String> nomes = auxDao.listarSexo();
            for (String nome : nomes) { listaSexo.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar os dados de Sexo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoSexo(ActionEvent event) {
        txtSexoNome.clear();
        txtSexoNome.requestFocus();
        tableSexo.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarSexo(ActionEvent event) {
        String nome = txtSexoNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Sexo não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableSexo.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdSexoPorNome(sel.getNome()); // Retorna Integer
                if (idOld != null && idOld != -1) { // Verifica se não é nulo e não é -1
                    if (auxDao.atualizarSexo(idOld, nome.trim())) { // atualizarSexo espera int
                        showAlert(AlertType.INFORMATION, "Sucesso", "Sexo atualizado com sucesso!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Sexo. Verifique se o novo nome já existe ou se há restrições no banco.");
                    }
                } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Não foi possível encontrar o ID do Sexo selecionado para atualização.");
                }
            } else { // Inserir
                if (auxDao.inserirSexo(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Sexo salvo com sucesso!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Sexo. Verifique se o nome já existe.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Ocorreu um erro ao salvar/atualizar Sexo: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosSexo();
        txtSexoNome.clear();
        tableSexo.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirSexo(ActionEvent event) {
        AuxiliarItem sel = tableSexo.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Por favor, selecione um item de Sexo para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdSexoPorNome(sel.getNome()); // Retorna Integer
            if (id != null && id != -1) { // Verifica se não é nulo e não é -1
                if (auxDao.excluirSexo(id)) { // excluirSexo espera int
                    showAlert(AlertType.INFORMATION, "Sucesso", "Sexo excluído!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Sexo.");
                }
            } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "ID do Sexo não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Ocorreu um erro ao excluir Sexo: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosSexo();
        txtSexoNome.clear();
        tableSexo.getSelectionModel().clearSelection();
    }

    // --- MÉTODOS TIPO DOC ---
    private void carregarDadosTipoDoc() {
        try {
            listaTipoDoc.clear();
            List<String> nomes = auxDao.listarTipoDoc();
            for (String nome : nomes) { listaTipoDoc.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar os Tipos de Documento: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoTipoDoc(ActionEvent event) {
        txtTipoDocNome.clear();
        txtTipoDocNome.requestFocus();
        tableTipoDoc.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarTipoDoc(ActionEvent event) {
        String nome = txtTipoDocNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Tipo de Documento não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableTipoDoc.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdTipoDocPorNome(sel.getNome());
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarTipoDoc(idOld, nome.trim())) {
                        showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Documento atualizado!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Tipo de Documento.");
                    }
                } else {
                    showAlert(AlertType.ERROR, "Erro na Atualização", "ID do Tipo de Documento não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirTipoDoc(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Documento salvo!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Tipo de Documento.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Tipo de Documento: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosTipoDoc();
        txtTipoDocNome.clear();
        tableTipoDoc.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirTipoDoc(ActionEvent event) {
        AuxiliarItem sel = tableTipoDoc.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione um Tipo de Documento para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdTipoDocPorNome(sel.getNome());
            if (id != null && id != -1) {
                if (auxDao.excluirTipoDoc(id)) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Documento excluído!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Tipo de Documento.");
                }
            } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "ID do Tipo de Documento não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Tipo de Documento: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosTipoDoc();
        txtTipoDocNome.clear();
        tableTipoDoc.getSelectionModel().clearSelection();
    }

    // --- MÉTODOS NACIONALIDADE ---
    private void carregarDadosNacionalidade() {
        try {
            listaNacionalidade.clear();
            List<String> nomes = auxDao.listarNacionalidade();
            for (String nome : nomes) { listaNacionalidade.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar as Nacionalidades: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoNacionalidade(ActionEvent event) {
        txtNacionalidadeNome.clear();
        txtNacionalidadeNome.requestFocus();
        tableNacionalidade.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarNacionalidade(ActionEvent event) {
        String nome = txtNacionalidadeNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Nacionalidade não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableNacionalidade.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdNacionalidadePorNome(sel.getNome());
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarNacionalidade(idOld, nome.trim())) {
                        showAlert(AlertType.INFORMATION, "Sucesso", "Nacionalidade atualizada!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Nacionalidade.");
                    }
                } else {
                    showAlert(AlertType.ERROR, "Erro na Atualização", "ID da Nacionalidade não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirNacionalidade(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Nacionalidade salva!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Nacionalidade.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Nacionalidade: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosNacionalidade();
        txtNacionalidadeNome.clear();
        tableNacionalidade.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirNacionalidade(ActionEvent event) {
        AuxiliarItem sel = tableNacionalidade.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione uma Nacionalidade para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdNacionalidadePorNome(sel.getNome());
            if (id != null && id != -1) {
                if (auxDao.excluirNacionalidade(id)) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Nacionalidade excluída!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Nacionalidade.");
                }
            } else {
                showAlert(AlertType.ERROR, "Erro ao Excluir", "ID da Nacionalidade não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Nacionalidade: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosNacionalidade();
        txtNacionalidadeNome.clear();
        tableNacionalidade.getSelectionModel().clearSelection();
    }

    // --- MÉTODOS PASSAGEM AUX (Tipos de Passagem) ---
    private void carregarDadosPassagemAux() {
        try {
            listaPassagemAux.clear();
            List<String> nomes = auxDao.listarPassagemAux(); // Este método foi mantido com este nome no DAO
            for (String nome : nomes) { listaPassagemAux.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar Tipos de Passagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoPassagemAux(ActionEvent event) {
        txtPassagemAuxNome.clear();
        txtPassagemAuxNome.requestFocus();
        tablePassagemAux.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarPassagemAux(ActionEvent event) {
        String nome = txtPassagemAuxNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Tipo de Passagem não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tablePassagemAux.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdTipoPassagemPorNome(sel.getNome());
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarTipoPassagem(idOld, nome.trim())) {
                        showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Passagem atualizado!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Tipo de Passagem.");
                    }
                } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "ID do Tipo de Passagem não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirTipoPassagem(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Passagem salvo!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Tipo de Passagem.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Tipo de Passagem: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosPassagemAux();
        txtPassagemAuxNome.clear();
        tablePassagemAux.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirPassagemAux(ActionEvent event) {
        AuxiliarItem sel = tablePassagemAux.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione um Tipo de Passagem para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdTipoPassagemPorNome(sel.getNome());
            if (id != null && id != -1) {
                if (auxDao.excluirTipoPassagem(id)) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Tipo de Passagem excluído!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Tipo de Passagem.");
                }
            } else {
                showAlert(AlertType.ERROR, "Erro ao Excluir", "ID do Tipo de Passagem não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Tipo de Passagem: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosPassagemAux();
        txtPassagemAuxNome.clear();
        tablePassagemAux.getSelectionModel().clearSelection();
    }

    // --- MÉTODOS AGENTE AUX ---
    private void carregarDadosAgenteAux() {
        try {
            listaAgenteAux.clear();
            List<String> nomes = auxDao.listarAgenteAux();
            for (String nome : nomes) { listaAgenteAux.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar Agentes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoAgenteAux(ActionEvent event) {
        txtAgenteAuxNome.clear();
        txtAgenteAuxNome.requestFocus();
        tableAgenteAux.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarAgenteAux(ActionEvent event) {
        String nome = txtAgenteAuxNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Agente não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableAgenteAux.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdAgenteAuxPorNome(sel.getNome());
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarAgenteAux(idOld, nome.trim())) {
                        showAlert(AlertType.INFORMATION, "Sucesso", "Agente atualizado!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Agente.");
                    }
                } else {
                    showAlert(AlertType.ERROR, "Erro na Atualização", "ID do Agente não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirAgenteAux(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Agente salvo!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Agente.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Agente: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosAgenteAux();
        txtAgenteAuxNome.clear();
        tableAgenteAux.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirAgenteAux(ActionEvent event) {
        AuxiliarItem sel = tableAgenteAux.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione um Agente para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdAgenteAuxPorNome(sel.getNome());
            if (id != null && id != -1) {
                if (auxDao.excluirAgenteAux(id)) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Agente excluído!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Agente.");
                }
            } else {
                showAlert(AlertType.ERROR, "Erro ao Excluir", "ID do Agente não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Agente: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosAgenteAux();
        txtAgenteAuxNome.clear();
        tableAgenteAux.getSelectionModel().clearSelection();
    }
    
    // --- MÉTODOS HORÁRIO SAÍDA ---
    private void carregarDadosHorarioSaida() {
        try {
            listaHorarioSaida.clear();
            List<String> nomes = auxDao.listarHorarioSaida();
            for (String nome : nomes) { listaHorarioSaida.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar Horários de Saída: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoHorarioSaida(ActionEvent event) {
        txtHorarioSaidaNome.clear();
        txtHorarioSaidaNome.requestFocus();
        tableHorarioSaida.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarHorarioSaida(ActionEvent event) {
        String nome = txtHorarioSaidaNome.getText(); // Assumindo que o horário é um texto simples
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O Horário de Saída não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableHorarioSaida.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                // auxDao.obterIdHorarioSaidaPorNome retorna Integer. A atualizarHorarioSaida espera int.
                Integer idOld = auxDao.obterIdHorarioSaidaPorNome(sel.getNome()); // CORRIGIDO: nome do método
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarHorarioSaida(idOld, nome.trim())) { // atualizarHorarioSaida espera int
                        showAlert(AlertType.INFORMATION, "Sucesso", "Horário de Saída atualizado!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Horário de Saída.");
                    }
                } else {
                    showAlert(AlertType.ERROR, "Erro na Atualização", "ID do Horário de Saída não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirHorarioSaida(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Horário de Saída salvo!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Horário de Saída.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Horário de Saída: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosHorarioSaida();
        txtHorarioSaidaNome.clear();
        tableHorarioSaida.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirHorarioSaida(ActionEvent event) {
        AuxiliarItem sel = tableHorarioSaida.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione um Horário de Saída para excluir.");
            return;
        }
        try {
            // auxDao.obterIdHorarioSaidaPorNome retorna Integer. A excluirHorarioSaida espera int.
            Integer id = auxDao.obterIdHorarioSaidaPorNome(sel.getNome()); // CORRIGIDO: nome do método
            if (id != null && id != -1) {
                if (auxDao.excluirHorarioSaida(id)) { // excluirHorarioSaida espera int
                    showAlert(AlertType.INFORMATION, "Sucesso", "Horário de Saída excluído!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Horário de Saída.");
                }
            } else {
                showAlert(AlertType.ERROR, "Erro ao Excluir", "ID do Horário de Saída não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Horário de Saída: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosHorarioSaida();
        txtHorarioSaidaNome.clear();
        tableHorarioSaida.getSelectionModel().clearSelection();
    }

    // --- MÉTODOS ACOMODAÇÃO ---
    private void carregarDadosAcomodacao() {
        try {
            listaAcomodacao.clear();
            List<String> nomes = auxDao.listarAcomodacao();
            for (String nome : nomes) { listaAcomodacao.add(new AuxiliarItem(nome)); }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Dados", "Não foi possível carregar Acomodações: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void handleNovoAcomodacao(ActionEvent event) {
        txtAcomodacaoNome.clear();
        txtAcomodacaoNome.requestFocus();
        tableAcomodacao.getSelectionModel().clearSelection();
    }
    @FXML private void handleSalvarAcomodacao(ActionEvent event) {
        String nome = txtAcomodacaoNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Campo Vazio", "O nome para Acomodação não pode ser vazio.");
            return;
        }
        AuxiliarItem sel = tableAcomodacao.getSelectionModel().getSelectedItem();
        try {
            if (sel != null) { // Atualizar
                Integer idOld = auxDao.buscarIdAcomodacaoPorNome(sel.getNome());
                if (idOld != null && idOld != -1) {
                    if (auxDao.atualizarAcomodacao(idOld, nome.trim())) {
                        showAlert(AlertType.INFORMATION, "Sucesso", "Acomodação atualizada!");
                    } else {
                        showAlert(AlertType.ERROR, "Erro na Atualização", "Falha ao atualizar Acomodação.");
                    }
                } else {
                    showAlert(AlertType.ERROR, "Erro na Atualização", "ID da Acomodação não encontrado.");
                }
            } else { // Inserir
                if (auxDao.inserirAcomodacao(nome.trim())) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Acomodação salva!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Salvar", "Falha ao salvar Acomodação.");
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao salvar/atualizar Acomodação: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosAcomodacao();
        txtAcomodacaoNome.clear();
        tableAcomodacao.getSelectionModel().clearSelection();
    }
    @FXML private void handleExcluirAcomodacao(ActionEvent event) {
        AuxiliarItem sel = tableAcomodacao.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(AlertType.WARNING, "Seleção Necessária", "Selecione uma Acomodação para excluir.");
            return;
        }
        try {
            Integer id = auxDao.buscarIdAcomodacaoPorNome(sel.getNome());
            if (id != null && id != -1) {
                if (auxDao.excluirAcomodacao(id)) {
                    showAlert(AlertType.INFORMATION, "Sucesso", "Acomodação excluída!");
                } else {
                    showAlert(AlertType.ERROR, "Erro ao Excluir", "Falha ao excluir Acomodação.");
                }
            } else {
                showAlert(AlertType.ERROR, "Erro ao Excluir", "ID da Acomodação não encontrado.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro na Operação", "Erro ao excluir Acomodação: " + e.getMessage());
            e.printStackTrace();
        }
        carregarDadosAcomodacao();
        txtAcomodacaoNome.clear();
        tableAcomodacao.getSelectionModel().clearSelection();
    }
    
    // --- Método Auxiliar para Alertas ---
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Para não mostrar um cabeçalho extra
        alert.setContentText(message);
        alert.showAndWait();
    }
}