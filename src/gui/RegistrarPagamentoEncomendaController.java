package gui;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import dao.CaixaDAO;
import dao.EncomendaDAO;
import dao.EncomendaItemDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import model.Caixa; 
import model.Encomenda;
import model.EncomendaItem;

public class RegistrarPagamentoEncomendaController implements Initializable {

    @FXML private Button btnCancelar;
    @FXML private Button btnConfirmar;
    
    @FXML private ComboBox<Caixa> cmbCaixa; 

    @FXML private Label lblFalta;
    @FXML private Label lblTituloFalta;
    @FXML private Label lblTituloTroco;
    @FXML private Label lblTotalRecebido;
    @FXML private Label lblTroco;
    @FXML private Label lblValorTotal;
    
    @FXML private TextField txtCartao;
    @FXML private TextField txtDinheiro;
    @FXML private TextField txtPix;

    private Encomenda encomendaAtual;
    private ObservableList<EncomendaItem> itensParaSalvar;
    private InserirEncomendaController parentController;
    
    private EncomendaDAO encomendaDAO;
    private EncomendaItemDAO encomendaItemDAO;
    private CaixaDAO caixaDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        encomendaDAO = new EncomendaDAO();
        encomendaItemDAO = new EncomendaItemDAO();
        caixaDAO = new CaixaDAO(); 
        
        // --- CARREGAMENTO DOS CAIXAS ---
        try {
            List<Caixa> listaCaixas = caixaDAO.listarTodos();
            if (listaCaixas.isEmpty()) {
                cmbCaixa.setPromptText("Nenhum caixa cadastrado");
            } else {
                cmbCaixa.setItems(FXCollections.observableArrayList(listaCaixas));
                cmbCaixa.getSelectionModel().selectFirst(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        configurarListener(txtDinheiro);
        configurarListener(txtPix);
        configurarListener(txtCartao);
        
        Platform.runLater(() -> txtDinheiro.requestFocus());
        
        // --- CORREÇÃO DO F3 E ESC ---
        // Espera o botão entrar na cena para adicionar o atalho global
        btnConfirmar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.F3) {
                        handleConfirmar(null);
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        handleCancelar(null);
                        event.consume();
                    }
                });
            }
        });
    }

    public void setDados(Encomenda encomenda, ObservableList<EncomendaItem> itens, InserirEncomendaController parent) {
        this.encomendaAtual = encomenda;
        this.itensParaSalvar = itens;
        this.parentController = parent;
        
        BigDecimal total = encomenda.getTotalAPagar();
        lblValorTotal.setText("R$ " + String.format("%,.2f", total));
        
        calcularTotais();
    }
    
    private void configurarListener(TextField txt) {
        txt.textProperty().addListener((obs, old, novo) -> {
            calcularTotais();
        });
    }

    private void calcularTotais() {
        BigDecimal dinheiro = parse(txtDinheiro.getText());
        BigDecimal pix = parse(txtPix.getText());
        BigDecimal cartao = parse(txtCartao.getText());
        
        BigDecimal totalPagoAgora = dinheiro.add(pix).add(cartao);
        
        BigDecimal jaPagoAnteriormente = (encomendaAtual.getValorPago() != null) ? encomendaAtual.getValorPago() : BigDecimal.ZERO;
        BigDecimal totalRecebidoGlobal = totalPagoAgora; 
        
        if (encomendaAtual.getId() != null && jaPagoAnteriormente.compareTo(BigDecimal.ZERO) > 0) {
             totalRecebidoGlobal = totalPagoAgora.add(jaPagoAnteriormente);
        }

        BigDecimal totalDevido = encomendaAtual.getTotalAPagar();
        lblTotalRecebido.setText("R$ " + String.format("%,.2f", totalRecebidoGlobal));
        
        BigDecimal diferenca = totalRecebidoGlobal.subtract(totalDevido);
        
        if (diferenca.compareTo(BigDecimal.ZERO) >= 0) {
            mostrarPainelTroco(true);
            lblTroco.setText("R$ " + String.format("%,.2f", diferenca));
            
            if(diferenca.compareTo(BigDecimal.ZERO) == 0) {
                lblTroco.setStyle("-fx-text-fill: #1a3c7d; -fx-font-weight: bold; -fx-font-size: 16px;");
                lblTituloTroco.setText("Quitado:");
            } else {
                lblTroco.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 18px;");
                lblTituloTroco.setText("Troco:");
            }
        } else {
            mostrarPainelTroco(false);
            lblFalta.setText("R$ " + String.format("%,.2f", diferenca.abs()));
        }
    }
    
    private void mostrarPainelTroco(boolean mostrarTroco) {
        lblTituloTroco.setVisible(mostrarTroco);
        lblTroco.setVisible(mostrarTroco);
        lblTituloTroco.setManaged(mostrarTroco); 
        lblTroco.setManaged(mostrarTroco);

        lblTituloFalta.setVisible(!mostrarTroco);
        lblFalta.setVisible(!mostrarTroco);
        lblTituloFalta.setManaged(!mostrarTroco);
        lblFalta.setManaged(!mostrarTroco);
    }

    @FXML
    void handleConfirmar(ActionEvent event) {
        Caixa caixaSelecionado = cmbCaixa.getValue();

        if (caixaSelecionado == null) {
            showAlert(AlertType.WARNING, "Por favor, selecione em qual CAIXA o dinheiro vai entrar.");
            cmbCaixa.requestFocus();
            return;
        }

        BigDecimal dinheiro = parse(txtDinheiro.getText());
        BigDecimal pix = parse(txtPix.getText());
        BigDecimal cartao = parse(txtCartao.getText());
        
        BigDecimal pagoAgora = dinheiro.add(pix).add(cartao);
        BigDecimal jaPago = (encomendaAtual.getValorPago() != null) ? encomendaAtual.getValorPago() : BigDecimal.ZERO;
        
        BigDecimal valorFinalPago = (encomendaAtual.getId() != null) ? jaPago.add(pagoAgora) : pagoAgora;
        
        encomendaAtual.setValorPago(valorFinalPago);
        
        if (valorFinalPago.compareTo(encomendaAtual.getTotalAPagar()) >= 0) {
            encomendaAtual.setStatusPagamento("PAGO");
        } else if (valorFinalPago.compareTo(BigDecimal.ZERO) > 0) {
            encomendaAtual.setStatusPagamento("PARCIAL");
        } else {
            encomendaAtual.setStatusPagamento("PENDENTE");
        }
        
        StringBuilder forma = new StringBuilder();
        if (dinheiro.compareTo(BigDecimal.ZERO) > 0) forma.append("DINHEIRO ");
        if (pix.compareTo(BigDecimal.ZERO) > 0) forma.append("PIX ");
        if (cartao.compareTo(BigDecimal.ZERO) > 0) forma.append("CARTAO");
        
        String formaStr = forma.toString().trim();
        if (formaStr.isEmpty()) formaStr = "PENDENTE";
        encomendaAtual.setFormaPagamento(formaStr);
        
        try {
            encomendaAtual.setLocalPagamento(caixaSelecionado.getNome()); 
            encomendaAtual.setIdCaixa(caixaSelecionado.getId());          
        } catch (Exception e) {
            System.out.println("Erro ao setar dados do caixa na encomenda: " + e.getMessage());
        }
        
        salvarNoBanco();
    }

    private void salvarNoBanco() {
        try {
            boolean sucessoEncomenda = false;
            
            if (encomendaAtual.getId() == null) {
                Encomenda nova = encomendaDAO.inserir(encomendaAtual);
                if (nova != null) {
                    encomendaAtual = nova; 
                    sucessoEncomenda = true;
                }
            } else {
                sucessoEncomenda = encomendaDAO.atualizar(encomendaAtual);
            }
            
            if (sucessoEncomenda) {
                if (encomendaAtual.getId() != null) {
                    encomendaItemDAO.excluirPorEncomenda(encomendaAtual.getId());
                }
                
                for (EncomendaItem item : itensParaSalvar) {
                    item.setIdEncomenda(encomendaAtual.getId());
                    encomendaItemDAO.inserir(item);
                }
                
                // showAlert(AlertType.INFORMATION, "Pagamento registrado e encomenda salva!"); // Removido para ser mais rápido
                
                if (parentController != null) {
                    parentController.finalizarSalvamento(encomendaAtual);
                }
                
                Stage stage = (Stage) btnConfirmar.getScene().getWindow();
                stage.close();
                
            } else {
                showAlert(AlertType.ERROR, "Erro ao salvar encomenda no banco de dados.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Erro técnico: " + e.getMessage());
        }
    }

    @FXML
    void handleCancelar(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
    
    private BigDecimal parse(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.replace(".", "").replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void showAlert(AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == AlertType.ERROR ? "Erro" : "Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}