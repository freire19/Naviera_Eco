package gui;

import dao.AgendaDAO;
import dao.AgendaDAO.TarefaAgenda;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class TelaGerenciarAgendaController implements Initializable {

    @FXML private CheckBox chkOcultarConcluidas;
    @FXML private TableView<TarefaAgenda> tabelaTarefas;
    @FXML private TableColumn<TarefaAgenda, LocalDate> colData;
    @FXML private TableColumn<TarefaAgenda, String> colDescricao;
    @FXML private TableColumn<TarefaAgenda, Boolean> colConcluida;
    @FXML private TableColumn<TarefaAgenda, Void> colAcoes;

    private final AgendaDAO agendaDAO = new AgendaDAO();
    private ObservableList<TarefaAgenda> listaMaster = FXCollections.observableArrayList();
    private FilteredList<TarefaAgenda> listaFiltrada;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColunas();
        carregarDados();
    }

    private void configurarColunas() {
        // Formatar Data
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colData.setCellFactory(column -> new TableCell<TarefaAgenda, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        // Descrição
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        // CheckBox de Concluído (A mágica acontece aqui)
        colConcluida.setCellValueFactory(param -> {
            TarefaAgenda tarefa = param.getValue();
            SimpleObjectProperty<Boolean> booleanProp = new SimpleObjectProperty<>(tarefa.isConcluida());
            // DR127: mover DB call para bg thread (evita bloquear FX thread no render de celula)
            booleanProp.addListener((observable, oldValue, newValue) -> {
                tarefa.setConcluida(newValue);
                Thread bgUpdate = new Thread(() -> agendaDAO.atualizarStatus(tarefa.getId(), newValue));
                bgUpdate.setDaemon(true);
                bgUpdate.start();
            });
            return booleanProp;
        });
        colConcluida.setCellFactory(CheckBoxTableCell.forTableColumn(colConcluida));

        // Botão Excluir
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnExcluir = new Button("Excluir");
            {
                btnExcluir.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: red;");
                btnExcluir.setOnAction(event -> {
                    TarefaAgenda tarefa = getTableView().getItems().get(getIndex());
                    excluirTarefa(tarefa);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnExcluir);
            }
        });
    }

    private void carregarDados() {
        listaMaster.clear();
        listaMaster.addAll(agendaDAO.buscarTodasTarefas());
        
        listaFiltrada = new FilteredList<>(listaMaster, p -> true);
        tabelaTarefas.setItems(listaFiltrada);
        aplicarFiltro();
    }
    
    @FXML
    private void handleAtualizar() {
        carregarDados();
    }

    @FXML
    private void handleFiltrar() {
        aplicarFiltro();
    }
    
    private void aplicarFiltro() {
        if (listaFiltrada == null) return;
        
        boolean ocultar = chkOcultarConcluidas.isSelected();
        listaFiltrada.setPredicate(tarefa -> {
            if (ocultar) {
                return !tarefa.isConcluida(); // Só mostra se NÃO estiver concluída
            }
            return true; // Mostra tudo
        });
    }

    private void excluirTarefa(TarefaAgenda tarefa) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Excluir Tarefa");
        alert.setHeaderText("Tem certeza que deseja excluir?");
        alert.setContentText(tarefa.getDescricao());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            agendaDAO.excluirTarefa(tarefa.getId());
            listaMaster.remove(tarefa); // Remove da tela sem precisar recarregar tudo do banco
        }
    }
}