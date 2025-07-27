package gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node; // Para fechar janela
import model.Empresa;
import model.Embarcacao; // Adicionado
import dao.EmpresaDAO;
import dao.EmbarcacaoDAO; // Adicionado

import java.net.URL;
import java.io.File;
import java.util.ResourceBundle;

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
    @FXML private Label lblMensagem; // Adicionado para feedback

    // Botões fx:id devem corresponder ao FXML
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnSair;
    @FXML private Button btnEscolherFoto;

    private EmpresaDAO empresaDAO;
    private EmbarcacaoDAO embarcacaoDAO; // DAO para Embarcação
    private Empresa empresaAtual = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        empresaDAO = new EmpresaDAO();
        embarcacaoDAO = new EmbarcacaoDAO();
        // Ao abrir, tentamos carregar o registro com ID=1
        carregarEmpresa(1); 
        lblMensagem.setText(""); // Limpa mensagem inicial
    }

    private void carregarEmpresa(int id) {
        Empresa e = empresaDAO.buscarPorId(id); // Presume que ID=1 é o registro de configuração
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
            lblMensagem.setText("Dados carregados. Modifique e clique em Salvar.");
        } else {
            empresaAtual = null;
            limparCampos();
            lblMensagem.setText("Nenhuma configuração salva. Preencha os dados.");
        }
    }

    @FXML
    private void handleNovo(ActionEvent event) {
        empresaAtual = null; // Indica que será uma nova inserção (embora sempre atualizaremos ID=1)
        limparCampos();
        lblMensagem.setText("Campos limpos. Preencha os dados para salvar a configuração.");
        txtCompanhia.requestFocus();
    }

    @FXML
    private void handleSalvar(ActionEvent event) {
        if (txtCompanhia.getText().isEmpty() || txtEmbarcacao.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Campos Obrigatórios", "Nome da Companhia e Nome da Embarcação são obrigatórios.");
            return;
        }

        if (empresaAtual == null) {
            empresaAtual = new Empresa();
            empresaAtual.setId(1); // ID fixo para a tabela configuracao_empresa
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

        boolean configOk = empresaDAO.salvarOuAtualizar(empresaAtual);

        if (configOk) {
            lblMensagem.setText("Configurações da empresa salvas com sucesso!");
            // Agora, salva/atualiza a embarcação principal
            if (!nomeEmbarcacaoPrincipal.isEmpty()) {
                Embarcacao embPrincipal = new Embarcacao(nomeEmbarcacaoPrincipal);
                // Se você coletar mais dados da embarcação nesta tela, sete-os aqui.
                // Ex: embPrincipal.setRegistroCapitania(...);
                
                Embarcacao embSalva = embarcacaoDAO.inserirOuBuscar(embPrincipal); 
                
                if (embSalva != null && embSalva.getId() > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Configurações da Empresa e dados da Embarcação principal salvos!");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Aviso", "Configurações da empresa salvas, mas houve um problema ao registrar a embarcação principal na tabela 'embarcacoes'.");
                }
            } else {
                 showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Configurações da Empresa salvas!");
            }
            carregarEmpresa(1); // Recarrega os dados salvos nos campos
        } else {
            showAlert(Alert.AlertType.ERROR, "Erro", "Falha ao salvar as configurações da Empresa.");
            lblMensagem.setText("Erro ao salvar. Verifique o console.");
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
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File arquivoSelecionado = fc.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (arquivoSelecionado != null) {
            txtCaminhoFoto.setText(arquivoSelecionado.getAbsolutePath());
            lblMensagem.setText("Caminho da foto selecionado. Clique em Salvar.");
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
        lblMensagem.setText("");
    }

    private void showAlert(Alert.AlertType alertType, String titulo, String msg) {
        Alert a = new Alert(alertType);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}