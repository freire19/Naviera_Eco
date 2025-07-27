package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType; // Importar AlertType
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.sql.Connection;
import java.sql.SQLException;

import dao.ViagemDAO;
import dao.ConexaoBD;
import model.Viagem;
import dao.AuxiliaresDAO;

/**
 * Controller da tela principal (menu).
 */
public class TelaPrincipalController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private ComboBox<String> cmbViagemAtiva;

    @FXML
    private Button btnCarregarDadosDaViagem;

    @FXML
    private Text txtProximasViagens;
    @FXML
    private Text txtTotalEncomendas;
    @FXML
    private Text txtSaldoCaixa;

    private final ViagemDAO viagemDAO = new ViagemDAO();
    private final AuxiliaresDAO auxiliaresDAO = new AuxiliaresDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        carregarViagensNoCombo();
        txtProximasViagens.setText("N/D");
        txtTotalEncomendas.setText("0");
        txtSaldoCaixa.setText("R$ 0,00");
    }

    private void carregarViagensNoCombo() {
        try {
            List<String> listaViagens = viagemDAO.listarViagensParaComboBox();
            ObservableList<String> obs = FXCollections.observableArrayList(listaViagens);
            if (cmbViagemAtiva != null) { // Adicionado null check para cmbViagemAtiva
                cmbViagemAtiva.setItems(obs);

                Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();
                if (viagemAtiva != null) {
                    String activeViagemStr = viagemAtiva.toString();
                    if (cmbViagemAtiva.getItems().contains(activeViagemStr)) {
                        cmbViagemAtiva.getSelectionModel().select(activeViagemStr);
                    } else {
                        System.err.println("Aviso: Viagem ativa não encontrada no ComboBox da Tela Principal: " + activeViagemStr);
                        if (!obs.isEmpty()) {
                            cmbViagemAtiva.getSelectionModel().selectFirst();
                        }
                    }
                } else {
                    if (!obs.isEmpty()) {
                        cmbViagemAtiva.getSelectionModel().selectFirst();
                    }
                }
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro de Carregamento", "Não foi possível carregar as viagens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCarregarDadosDaViagem(ActionEvent event) {
        String selecionada = cmbViagemAtiva.getValue();
        if (selecionada == null || selecionada.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Atenção", "Selecione uma viagem antes de continuar.");
            return;
        }

        Long idViagem = -1L; // Usar Long para consistência e -1L para erro
        try {
            // CORRIGIDO AQUI: Usando o valor do cmbViagemAtiva
        	Long resolvedId = viagemDAO.obterIdViagemPelaString(selecionada);
            if(resolvedId != null) {
                idViagem = resolvedId;
            } else {
                showAlert(AlertType.ERROR, "Erro", "Não foi possível identificar o ID da viagem selecionada.");
                return;
            }

        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Erro de Banco de Dados", "Erro de conexão ao obter ID da viagem: " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro", "Erro inesperado ao processar seleção da viagem: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (idViagem != -1L) { // Comparar com -1L
            try {
                if (viagemDAO.definirViagemAtiva(idViagem)) { // definirViagemAtiva espera long
                    showAlert(AlertType.INFORMATION, "Viagem Selecionada", "Viagem ID " + idViagem + " definida como ativa com sucesso!");
                } else {
                    showAlert(AlertType.ERROR, "Erro", "Não foi possível definir a viagem como ativa.");
                }
            } catch (Exception e) {
                showAlert(AlertType.ERROR, "Erro", "Erro ao definir viagem ativa: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showAlert(AlertType.WARNING, "ID Inválido", "Não foi possível identificar um ID de viagem válido para definir como ativa.");
        }
    }

    // --------- Menu “Cadastro” ---------

    @FXML
    private void handleCadastrarEmpresa(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroEmpresa.fxml", "Cadastro de Empresa/Configurações");
    }

    @FXML
    private void handleCadastrarUsuario(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroUsuario.fxml", "Cadastro de Usuários");
    }

    @FXML
    private void handleCadastrarViagem(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroViagem.fxml", "Cadastro de Viagem");
    }

    @FXML
    private void handleCadastrarRotas(ActionEvent event) {
        abrirTelaOUAlerta("/gui/Rotas.fxml", "Cadastro de Rotas");
    }

    @FXML
    private void handleCadastroTarifa(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroTarifa.fxml", "Cadastro de Tarifas (Passagem/Frete)");
    }

    @FXML
    private void handleCadastrarConferente(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroConferente.fxml", "Cadastro de Conferentes");
    }

    @FXML
    private void handleCadastrarCaixa(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroCaixa.fxml", "Cadastro de Tipos de Caixa");
    }

    @FXML
    private void handleProductos(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroItem.fxml", "Cadastro de Itens");
    }

    @FXML
    private void handleTabelasAuxiliares(ActionEvent event) {
        abrirTelaOUAlerta("/gui/TabelasAuxiliares.fxml", "Outros Cadastros Auxiliares");
    }

    // --------- Menu “Frete” ---------

    @FXML
    private void handleCadastrarFrete(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroFrete.fxml", "Lançar Novo Frete");
    }

    @FXML
    private void handleListaFrete(ActionEvent event) {
        abrirTelaOUAlerta("/gui/ListaFretes.fxml", "Listar Fretes da Viagem");
    }

    @FXML
    private void handleRelatorioFrete(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioFretes.fxml", "Relatório de Fretes");
    }

    @FXML
    private void handleEditarMercadoria(ActionEvent event) {
        abrirTelaOUAlerta("/gui/EditarMercadoriaFrete.fxml", "Editar Mercadoria (Frete)");
    }

    // --------- Menu “Passagens” ---------

    @FXML
    private void handleVenderPassagem(ActionEvent event) {
        abrirTelaOUAlerta("/gui/VenderPassagem.fxml", "Vender Nova Passagem");
    }

    @FXML
    private void handleListaPassagensNovo(ActionEvent event) {
        abrirTelaOUAlerta("/gui/ListarPassageirosViagem.fxml", "Listar Passageiros da Viagem");
    }

    @FXML
    private void handleRelatorioPassagem(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioPassagens.fxml", "Relatório de Passagens");
    }

    // --------- Menu “Encomendas” ---------

    @FXML
    private void handleInserirEncomenda(ActionEvent event) {
        abrirTelaOUAlerta("/gui/InserirEncomenda.fxml", "Registrar Nova Encomenda");
    }

    @FXML
    private void handleListaEncomenda(ActionEvent event) {
        abrirTelaOUAlerta("/gui/ListaEncomenda.fxml", "Listar Encomendas da Viagem");
    }

    @FXML
    private void handleRelatorioEncomenda(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioEncomendas.fxml", "Relatório de Encomendas");
    }

    @FXML
    private void handlePrecoEncomenda(ActionEvent event) {
        abrirTelaOUAlerta("/gui/TabelaPrecoEncomenda.fxml", "Tabela de Preços de Encomenda");
    }

    @FXML
    private void handleClientesEncomenda(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroClientesEncomenda.fxml", "Cadastro de Clientes (Encomendas)");
    }

    // --------- Menu “Financeiro” ---------

    @FXML
    private void handleInserirEntrada(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroEntrada.fxml", "Lançar Entrada (Avulsa)");
    }

    @FXML
    private void handleInserirSaida(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroSaida.fxml", "Lançar Saída (Despesa)");
    }

    @FXML
    private void handleRelatorioEntrada(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioEntradas.fxml", "Relatório de Entradas");
    }

    @FXML
    private void handleRelatorioSaida(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioSaidas.fxml", "Relatório de Saídas");
    }

    @FXML
    private void handleRelatorioGeralViagem(ActionEvent event) {
        abrirTelaOUAlerta("/gui/RelatorioGeralViagem.fxml", "Relatório Geral Financeiro da Viagem");
    }

    @FXML
    private void handleVentas(ActionEvent event) {
        abrirTelaOUAlerta("/gui/CadastroVendasBar.fxml", "Registrar Vendas (Bar/Serviços)");
    }

    // --------- Menu “Manutenção” ---------

    @FXML
    private void handleBackup(ActionEvent event) {
        abrirTelaOUAlerta("/gui/Backup.fxml", "Backup do Sistema");
    }

    // --------- Menu “Ajuda” ---------

    @FXML
    private void handleDuvidasFrequentes(ActionEvent event) {
        abrirTelaOUAlerta("/gui/DuvidasFrequentes.fxml", "Dúvidas Frequentes");
    }

    // --------- Menu “Sair” ---------

    @FXML
    private void handleLogout(ActionEvent event) {
        Node source = (Node) event.getSource();
        if (source != null && source.getScene() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) source.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }

    private void abrirTelaOUAlerta(String recursoFXML, String titulo) {
        try {
            URL fxmlUrl = getClass().getResource(recursoFXML);

            if (fxmlUrl == null) {
                System.err.println("Erro Crítico: Não foi possível encontrar o arquivo FXML no caminho: " + recursoFXML);
                showAlert(AlertType.ERROR, "Erro ao Carregar Tela",
                    "O arquivo FXML especificado não foi encontrado no classpath:\n" +
                    recursoFXML +
                    "\n\nPor favor, verifique se:\n" +
                    "1. O nome e o caminho do arquivo estão corretos.\n" +
                    "2. O arquivo existe no diretório 'src' do projeto (ex: src/gui/NomeDoArquivo.fxml).\n" +
                    "3. O arquivo está sendo copiado para o diretório de saída da compilação (ex: bin/gui/).");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent pane = loader.load();
            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(pane));
            stage.setTitle(titulo);

            if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                Stage owner = (Stage) rootPane.getScene().getWindow();
                stage.initOwner(owner);
                stage.initModality(Modality.APPLICATION_MODAL);
            }
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro ao Carregar Tela",
                "Ocorreu um erro ao tentar carregar a interface da tela:\n" + recursoFXML +
                "\n\nDetalhes do erro: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro Inesperado",
                "Um erro inesperado ocorreu ao tentar abrir a tela: " + titulo +
                "\n\nDetalhes: " + e.getMessage());
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