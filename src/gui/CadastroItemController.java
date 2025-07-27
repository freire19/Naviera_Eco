package gui;

import dao.ItemFreteDAO;
import model.ItemFrete; // Nova importação
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.math.BigDecimal; // Nova importação
import java.sql.SQLException; // Nova importação

public class CadastroItemController {
    @FXML private TextField txtDescricaoItem; // Este será o "nome_item"
    @FXML private TextField txtValorNormal;
    @FXML private TextField txtValorDesconto;

    private final ItemFreteDAO itemFreteDAO = new ItemFreteDAO();

    @FXML
    private void handleSalvarItem() {
        String nomeItemForm = txtDescricaoItem.getText().trim(); // Usaremos para nomeItem
        String valorNormalStr = txtValorNormal.getText().trim();
        String valorDescontoStr = txtValorDesconto.getText().trim();

        if (nomeItemForm.isEmpty() || valorNormalStr.isEmpty()) {
            // O valor com desconto pode ser opcional, dependendo da sua regra.
            // Se for obrigatório, adicione: || valorDescontoStr.isEmpty()
            showAlert("Erro de Validação", "O nome do item e o valor normal devem ser preenchidos.");
            return;
        }

        try {
            BigDecimal precoNormal = new BigDecimal(valorNormalStr.replace(",", "."));
            BigDecimal precoComDesconto = null;

            if (!valorDescontoStr.isEmpty()) {
                precoComDesconto = new BigDecimal(valorDescontoStr.replace(",", "."));
            }

            ItemFrete novoItem = new ItemFrete();
            novoItem.setNomeItem(nomeItemForm);
            // Como seu formulário não tem campos separados para descrição e unidade,
            // e você disse que eles estão no nome, podemos deixá-los vazios ou nulos.
            novoItem.setDescricao(""); // Ou null
            novoItem.setUnidadeMedida(""); // Ou null
            novoItem.setPrecoUnitarioPadrao(precoNormal);
            novoItem.setPrecoUnitarioDesconto(precoComDesconto); // Pode ser null se não informado
            novoItem.setAtivo(true); // Por padrão, um novo item é ativo

            itemFreteDAO.inserir(novoItem); // Chamada ao novo método do DAO
            showAlert("Sucesso", "Item cadastrado com sucesso! ID: " + novoItem.getIdItemFrete());

            limparCampos(); // Opcional: limpar campos após salvar
            // fecharJanela(); // Se quiser fechar após salvar

        } catch (NumberFormatException e) {
            showAlert("Erro de Formato", "Os valores de preço devem ser numéricos válidos (ex: 10.50).");
        } catch (SQLException e) {
            showAlert("Erro de Banco de Dados", "Não foi possível salvar o item: " + e.getMessage());
            e.printStackTrace(); // Importante para depuração no console
        } catch (Exception e) { // Captura genérica para outros erros inesperados
            showAlert("Erro Inesperado", "Ocorreu um erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limparCampos() {
        txtDescricaoItem.clear();
        txtValorNormal.clear();
        txtValorDesconto.clear();
        txtDescricaoItem.requestFocus(); // Foca no primeiro campo
    }

    @FXML
    private void handleCancelar() {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) txtDescricaoItem.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.NONE); // Tipo será definido abaixo

        if (titulo.toLowerCase().contains("erro") || titulo.toLowerCase().contains("falha")) {
            alerta.setAlertType(Alert.AlertType.ERROR);
        } else if (titulo.toLowerCase().contains("sucesso")) {
            alerta.setAlertType(Alert.AlertType.INFORMATION);
        } else if (titulo.toLowerCase().contains("aviso")) {
            alerta.setAlertType(Alert.AlertType.WARNING);
        } else {
            alerta.setAlertType(Alert.AlertType.INFORMATION); // Padrão
        }
        
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}