package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import model.Caixa; // Importar a classe model.Caixa

import java.net.URL;
import java.util.ResourceBundle;
import gui.util.AlertHelper;
// import dao.CaixaDAO; // Descomente se for usar o DAO

public class CadastroCaixaController implements Initializable {

    // Campos do FXML
    @FXML private TextField txtId;
    @FXML private TextField txtNome;
    @FXML private Button btnNova;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private TableView<Caixa> tabela; // Usando model.Caixa
    @FXML private TableColumn<Caixa, Integer> colId;
    @FXML private TableColumn<Caixa, String> colNome;

    // Lista observável para os dados da TableView
    private ObservableList<Caixa> listaCaixas = FXCollections.observableArrayList();

    // private final CaixaDAO caixaDAO = new CaixaDAO(); // Descomente se for usar o DAO

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Caixas"); return; }
        // Configurar colunas da tabela
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        // Definir itens para a tabela
        tabela.setItems(listaCaixas);

        // Adicionar listener para seleção na tabela para popular os campos de texto
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtId.setText(String.valueOf(newSelection.getId()));
                txtNome.setText(newSelection.getNome());
            } else {
                limparCampos();
            }
        });

        // Carregar dados iniciais (placeholder por enquanto)
        carregarDadosCaixa();
    }

    private void carregarDadosCaixa() {
        listaCaixas.clear();
        // Placeholder: Em uma aplicação real, você carregaria do CaixaDAO
        // List<Caixa> caixasDoBanco = caixaDAO.listarTodos(); // Se caixaDAO.listarTodos() retornasse List<Caixa>
        // for (Caixa caixa : caixasDoBanco) {
        //     listaCaixas.add(caixa);
        // }
        
        // Dados de exemplo usando model.Caixa:
        Caixa caixa1 = new Caixa();
        caixa1.setId(1);
        caixa1.setNome("Caixa Principal");
        listaCaixas.add(caixa1);

        Caixa caixa2 = new Caixa();
        caixa2.setId(2);
        caixa2.setNome("Caixa Despesas");
        listaCaixas.add(caixa2);
    }

    private void limparCampos() {
        txtId.clear();
        txtNome.clear();
        txtNome.requestFocus();
    }

    @FXML
    private void novaCaixa(ActionEvent event) {
        limparCampos();
        tabela.getSelectionModel().clearSelection();
        // Placeholder: Gerar novo ID ou tratar conforme necessário
        // int novoId = caixaDAO.gerarNovoIdCaixa(); // Se caixaDAO estivesse sendo usado
        // txtId.setText(String.valueOf(novoId));
        txtId.setText("NOVO"); // Indica que é uma nova entrada
        System.out.println("Botão Novo Caixa clicado.");
    }

    @FXML
    private void salvarCaixa(ActionEvent event) {
        String nome = txtNome.getText();
        if (nome == null || nome.trim().isEmpty()) {
            AlertHelper.show(AlertType.WARNING, "Campo Obrigatório", "O nome do Tipo de Caixa não pode ser vazio.");
            txtNome.requestFocus();
            return;
        }

        String idTexto = txtId.getText();
        Caixa selecionado = tabela.getSelectionModel().getSelectedItem();

        try {
            if (selecionado != null && !idTexto.equals("NOVO") && !idTexto.isEmpty()) { // Atualizar
                int idAtual = selecionado.getId();
                // Caixa caixaParaAtualizar = new Caixa();
                // caixaParaAtualizar.setId(idAtual);
                // caixaParaAtualizar.setNome(nome.trim());
                // boolean sucesso = caixaDAO.atualizar(caixaParaAtualizar); // Se existisse caixaDAO.atualizar(Caixa c)
                // if (sucesso) {
                //    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Tipo de Caixa atualizado com sucesso!");
                // } else {
                //    AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao atualizar o Tipo de Caixa.");
                // }
                System.out.println("Tentando ATUALIZAR Caixa ID: " + idAtual + " para Nome: " + nome.trim());
                AlertHelper.show(AlertType.INFORMATION, "Operação (Placeholder)", "Atualizar Caixa ID: " + idAtual + " com nome: " + nome.trim());
            } else { // Inserir novo
                // Caixa novoCaixa = new Caixa();
                // novoCaixa.setNome(nome.trim()); 
                // boolean sucesso = caixaDAO.inserir(novoCaixa); // Se existisse caixaDAO.inserir(Caixa c)
                // if (sucesso) {
                //    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Tipo de Caixa salvo com sucesso!");
                //    txtId.setText(String.valueOf(novoCaixa.getId())); // Atualiza ID na tela se o DAO retornar
                // } else {
                //    AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao salvar o Tipo de Caixa.");
                // }
                 System.out.println("Tentando INSERIR Caixa com Nome: " + nome.trim());
                 AlertHelper.show(AlertType.INFORMATION, "Operação (Placeholder)", "Inserir Caixa com nome: " + nome.trim());
            }
            carregarDadosCaixa(); // Recarrega dados na tabela
            limparCampos();
            tabela.getSelectionModel().clearSelection();
        } catch (Exception e) {
            AlertHelper.show(AlertType.ERROR, "Erro Inesperado", "Ocorreu um erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void excluirCaixa(ActionEvent event) {
        Caixa selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertHelper.show(AlertType.WARNING, "Seleção Necessária", "Por favor, selecione um Tipo de Caixa para excluir.");
            return;
        }

        // boolean sucesso = caixaDAO.excluir(selecionado.getId()); // Se existisse caixaDAO.excluir(int id)
        // if (sucesso) {
        //    AlertHelper.show(AlertType.INFORMATION, "Sucesso", "Tipo de Caixa excluído com sucesso!");
        // } else {
        //    AlertHelper.show(AlertType.ERROR, "Erro", "Falha ao excluir o Tipo de Caixa. Verifique se não está em uso.");
        // }
        System.out.println("Tentando EXCLUIR Caixa ID: " + selecionado.getId());
        AlertHelper.show(AlertType.INFORMATION, "Operação (Placeholder)", "Excluir Caixa ID: " + selecionado.getId());
        
        carregarDadosCaixa(); // Recarrega dados
        limparCampos();
        tabela.getSelectionModel().clearSelection();
    }

}