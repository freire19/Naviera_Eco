package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import model.Empresa;
import model.Embarcacao;
import dao.EmpresaDAO;
import dao.EmbarcacaoDAO;

import java.net.URL;
import java.io.File;
import java.util.ResourceBundle;
import javafx.application.Platform;
import gui.util.AlertHelper;
import util.AppLogger;

public class CadastroEmpresaController implements Initializable {

    @FXML private TextField txtCompanhia;
    @FXML private TextField txtEmbarcacao;
    @FXML private TextField txtComandante;
    @FXML private TextField txtProprietario;
    @FXML private TextField txtOrigem;
    @FXML private TextField txtGerente;
    @FXML private TextField txtLinhaDoRio;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtIe;
    @FXML private TextField txtEndereco;
    @FXML private TextField txtCep;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtFrase;
    @FXML private TextField txtCaminhoFoto;
    
    @FXML private TextArea txtRecomendacoes; // O TextArea já aceita Enter nativamente
    
    @FXML private Label lblMensagem;
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnSair;
    @FXML private Button btnEscolherFoto;

    private EmpresaDAO empresaDAO;
    private EmbarcacaoDAO embarcacaoDAO;
    private Empresa empresaAtual = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Configuracoes da Empresa"); return; }
        empresaDAO = new EmpresaDAO();
        embarcacaoDAO = new EmbarcacaoDAO();
        
        // Garante que o texto quebre linha visualmente se for muito longo, mas preserva os Enters
        txtRecomendacoes.setWrapText(true);

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                Empresa e = empresaDAO.buscarPorId(1);
                Platform.runLater(() -> {
                    if (e != null) {
                        empresaAtual = e;
                        txtCompanhia.setText(e.getCompanhia());
                        txtEmbarcacao.setText(e.getEmbarcacao());
                        txtComandante.setText(e.getComandante());
                        txtProprietario.setText(e.getProprietario());
                        txtOrigem.setText(e.getOrigem());
                        txtGerente.setText(e.getGerente());
                        txtLinhaDoRio.setText(e.getLinhaDoRio());
                        txtCnpj.setText(e.getCnpj());
                        txtIe.setText(e.getIe());
                        txtEndereco.setText(e.getEndereco());
                        txtCep.setText(e.getCep());
                        txtTelefone.setText(e.getTelefone());
                        txtFrase.setText(e.getFrase());
                        txtCaminhoFoto.setText(e.getCaminhoFoto());
                        try {
                            if (e.getRecomendacoesBilhete() != null) {
                                txtRecomendacoes.setText(e.getRecomendacoesBilhete());
                            }
                        } catch (Exception ex) { AppLogger.warn("CadastroEmpresaController", "CadastroEmpresaController.initialize: getRecomendacoesBilhete indisponivel — " + ex.getMessage()); }
                        lblMensagem.setText("Dados carregados.");
                    } else {
                        empresaAtual = null;
                        limparCampos();
                        lblMensagem.setText("Nenhuma configuração salva.");
                    }
                });
            } catch (Exception e) {
                AppLogger.warn("CadastroEmpresaController", "Erro ao carregar dados: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        lblMensagem.setText("");
    }

    private void carregarEmpresa(int id) {
        Empresa e = empresaDAO.buscarPorId(id);
        if (e != null) {
            empresaAtual = e;
            txtCompanhia.setText(e.getCompanhia());
            txtEmbarcacao.setText(e.getEmbarcacao());
            txtComandante.setText(e.getComandante());
            txtProprietario.setText(e.getProprietario());
            txtOrigem.setText(e.getOrigem());
            txtGerente.setText(e.getGerente());
            txtLinhaDoRio.setText(e.getLinhaDoRio());
            txtCnpj.setText(e.getCnpj());
            txtIe.setText(e.getIe());
            txtEndereco.setText(e.getEndereco());
            txtCep.setText(e.getCep());
            txtTelefone.setText(e.getTelefone());
            txtFrase.setText(e.getFrase());
            txtCaminhoFoto.setText(e.getCaminhoFoto());
            
            // Carrega as recomendações
            try {
                if(e.getRecomendacoesBilhete() != null) {
                    txtRecomendacoes.setText(e.getRecomendacoesBilhete());
                }
            } catch (Exception ex) {
                AppLogger.warn("CadastroEmpresaController", "CadastroEmpresaController.carregarEmpresa: getRecomendacoesBilhete indisponivel — " + ex.getMessage());
            }
            
            lblMensagem.setText("Dados carregados.");
        } else {
            empresaAtual = null;
            limparCampos();
            lblMensagem.setText("Nenhuma configuração salva.");
        }
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        empresaAtual = null;
        limparCampos();
        lblMensagem.setText("Campos limpos para novo cadastro.");
        txtCompanhia.requestFocus();
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        if (txtCompanhia.getText().isEmpty() || txtEmbarcacao.getText().isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Nome da Companhia e Embarcação são obrigatórios.");
            return;
        }

        if (empresaAtual == null) {
            empresaAtual = new Empresa();
            empresaAtual.setId(1);
        }

        empresaAtual.setCompanhia(txtCompanhia.getText().trim());
        String nomeEmbarcacaoPrincipal = txtEmbarcacao.getText().trim();
        empresaAtual.setEmbarcacao(nomeEmbarcacaoPrincipal);
        
        empresaAtual.setComandante(txtComandante.getText().trim());
        empresaAtual.setProprietario(txtProprietario.getText().trim());
        empresaAtual.setOrigem(txtOrigem.getText().trim());
        empresaAtual.setGerente(txtGerente.getText().trim());
        empresaAtual.setLinhaDoRio(txtLinhaDoRio.getText().trim());
        empresaAtual.setCnpj(txtCnpj.getText().trim());
        empresaAtual.setIe(txtIe.getText().trim());
        empresaAtual.setEndereco(txtEndereco.getText().trim());
        empresaAtual.setCep(txtCep.getText().trim());
        empresaAtual.setTelefone(txtTelefone.getText().trim());
        empresaAtual.setFrase(txtFrase.getText().trim());
        empresaAtual.setCaminhoFoto(txtCaminhoFoto.getText().trim());
        
        // Pega o texto exatamente como digitado (com quebras de linha)
        empresaAtual.setRecomendacoesBilhete(txtRecomendacoes.getText());

        boolean configOk = empresaDAO.salvarOuAtualizar(empresaAtual);

        if (configOk) {
            lblMensagem.setText("Salvo com sucesso!");
            
            if (!nomeEmbarcacaoPrincipal.isEmpty()) {
                Embarcacao embPrincipal = new Embarcacao(nomeEmbarcacaoPrincipal);
                embarcacaoDAO.inserirOuBuscar(embPrincipal); 
            }
            
            AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Configurações atualizadas!");
            
            // >>> MUDANÇA AQUI: NÃO LIMPA MAIS OS CAMPOS, APENAS RECARREGA PARA CONFIRMAR
            carregarEmpresa(1); 
            
        } else {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro", "Falha ao salvar no banco de dados.");
        }
    }

    @FXML
    private void handleSair(ActionEvent event) {
        Stage st = (Stage) ((Node) event.getSource()).getScene().getWindow();
        st.close();
    }

    @FXML
    private void onBtnEscolherFoto(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar Imagem da Logo");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
        );
        File arquivoSelecionado = fc.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (arquivoSelecionado != null) {
            txtCaminhoFoto.setText(arquivoSelecionado.getAbsolutePath());
        }
    }

    private void limparCampos() {
        txtCompanhia.clear();
        txtEmbarcacao.clear();
        txtComandante.clear();
        txtProprietario.clear();
        txtOrigem.clear();
        txtGerente.clear();
        txtLinhaDoRio.clear();
        txtCnpj.clear();
        txtIe.clear();
        txtEndereco.clear();
        txtCep.clear();
        txtTelefone.clear();
        txtFrase.clear();
        txtCaminhoFoto.clear();
        txtRecomendacoes.clear();
        lblMensagem.setText("");
    }

}