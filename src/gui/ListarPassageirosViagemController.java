package gui;

import dao.PassagemDAO;
import dao.ViagemDAO;
import model.Passagem; // Passagem contém os dados do passageiro para esta lista
import model.Viagem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.Node;
// IMPORTAÇÃO QUE ESTAVA FALTANDO:
import javafx.scene.control.TableCell; 

import java.net.URL;
import java.time.LocalDate;
import java.time.Period; // Para calcular idade
import java.time.format.DateTimeFormatter;
import java.util.Comparator; // Para ordenar a lista por nome
import java.util.List;
import java.util.ResourceBundle;

public class ListarPassageirosViagemController implements Initializable {

    @FXML private Label lblViagemAtivaInfo;
    @FXML private TableView<Passagem> tablePassageirosViagem;
    @FXML private TableColumn<Passagem, Integer> colOrdem; // Agora para o número sequencial
    @FXML private TableColumn<Passagem, String> colNomeCompleto;
    @FXML private TableColumn<Passagem, LocalDate> colDataNascimento;
    @FXML private TableColumn<Passagem, Integer> colIdade;    
    @FXML private TableColumn<Passagem, String> colRG;
    @FXML private TableColumn<Passagem, String> colOrigem;
    @FXML private TableColumn<Passagem, String> colDestino;
    @FXML private Button btnImprimirLista;
    @FXML private Button btnSair;

    private ViagemDAO viagemDAO;
    private PassagemDAO passagemDAO;
    private ObservableList<Passagem> obsListPassageiros;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        viagemDAO = new ViagemDAO();
        passagemDAO = new PassagemDAO();

        configurarTabela();
        carregarListaPassageirosDaViagemAtiva();
    }

    private void configurarTabela() {
        colOrdem.setCellValueFactory(new PropertyValueFactory<>("ordem")); // 'ordem' será uma propriedade que vamos adicionar ou simular
        colNomeCompleto.setCellValueFactory(new PropertyValueFactory<>("nomePassageiro"));
        colDataNascimento.setCellValueFactory(new PropertyValueFactory<>("dataNascimento"));    
        colIdade.setCellValueFactory(new PropertyValueFactory<>("idade"));    
        colRG.setCellValueFactory(new PropertyValueFactory<>("numeroDoc"));
        colOrigem.setCellValueFactory(new PropertyValueFactory<>("origem"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));

        // Formatação da coluna de data de nascimento
        colDataNascimento.setCellFactory(column -> new TableCell<Passagem, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        obsListPassageiros = FXCollections.observableArrayList();
        tablePassageirosViagem.setItems(obsListPassageiros);
    }

    private void carregarListaPassageirosDaViagemAtiva() {
        Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();

        if (viagemAtiva == null) {
            showAlert(AlertType.INFORMATION, "Nenhuma Viagem Ativa", "Não há uma viagem ativa definida. Por favor, ative uma viagem na Tela Principal.");
            lblViagemAtivaInfo.setText("Nenhuma viagem ativa.");
            obsListPassageiros.clear();
            return;
        }

        lblViagemAtivaInfo.setText(
            "Embarcação: " + (viagemAtiva.getNomeEmbarcacao() != null ? viagemAtiva.getNomeEmbarcacao() : "N/D") +
            " - Data de Saída: " + (viagemAtiva.getDataViagem() != null ? viagemAtiva.getDataViagem().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/D") +
            " - Horário: " + (viagemAtiva.getHorarioSaidaStr() != null ? viagemAtiva.getHorarioSaidaStr() : "N/D")
        );

        try {
            // Buscando passagens apenas para a viagem ativa
            List<Passagem> todasPassagens = passagemDAO.listarTodos(); // OU passagemDAO.buscarPassagensPorViagem(viagemAtiva.getId()); se existir
            List<Passagem> passageirosDaViagem = new java.util.ArrayList<>();
            int ordem = 1;

            for (Passagem p : todasPassagens) {
                // Verificar se a passagem pertence à viagem ativa
                if (p.getIdViagem() != null && p.getIdViagem().equals(viagemAtiva.getId())) {
                    // Calcula a idade
                    if (p.getDataNascimento() != null) {
                        p.setIdade(Period.between(p.getDataNascimento(), LocalDate.now()).getYears());
                    } else {
                        p.setIdade(0); // Idade desconhecida
                    }
                    // Adiciona um número de ordem sequencial
                    p.setOrdem(ordem++); // Define o número de ordem
                    passageirosDaViagem.add(p);
                }
            }

            // Opcional: Ordenar a lista por Nome Completo para a apresentação
            passageirosDaViagem.sort(Comparator.comparing(Passagem::getNomePassageiro, Comparator.nullsLast(String::compareToIgnoreCase)));

            obsListPassageiros.setAll(passageirosDaViagem);

            if (passageirosDaViagem.isEmpty()) {
                showAlert(AlertType.INFORMATION, "Lista Vazia", "Não há passageiros cadastrados para esta viagem.");
            }

        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Lista", "Ocorreu um erro ao carregar a lista de passageiros: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleImprimirLista(ActionEvent event) {
        showAlert(AlertType.INFORMATION, "Funcionalidade Pendente", "Imprimir Lista de Passageiros não implementado.");
    }

    @FXML
    private void handleSair(ActionEvent event) {
        Node source = (Node) event.getSource();
        if (source != null && source.getScene() != null) {
            Stage stage = (Stage) source.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}