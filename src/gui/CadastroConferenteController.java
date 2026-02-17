package gui;



import dao.ConexaoBD;

import javafx.beans.property.LongProperty;

import javafx.beans.property.SimpleLongProperty;

import javafx.beans.property.SimpleStringProperty;

import javafx.beans.property.StringProperty;

import javafx.collections.FXCollections;

import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.fxml.Initializable;

import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;



import java.net.URL;

import java.sql.*;

import java.util.Optional; // Para o Alert de confirmação

import java.util.ResourceBundle;



public class CadastroConferenteController implements Initializable {



@FXML private TextField txtId;

@FXML private TextField txtNome;

@FXML private Button btnNova;

@FXML private Button btnSalvar;

@FXML private Button btnExcluir;

@FXML private TableView<Conferente> tabela; // Nome da tabela no FXML

@FXML private TableColumn<Conferente, Long> colId;

@FXML private TableColumn<Conferente, String> colNome;



private ObservableList<Conferente> lista = FXCollections.observableArrayList();

// private ConferenteDAO conferenteDAO; // Seria ideal ter um DAO



@Override

public void initialize(URL location, ResourceBundle resources) {

// conferenteDAO = new ConferenteDAO(); // Instanciar o DAO aqui

colId.setCellValueFactory(new PropertyValueFactory<>("id"));

colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

tabela.setItems(lista);



carregarDoBanco();



// As ações dos botões serão chamadas pelos onAction definidos no FXML

// Então, não precisamos de setOnAction aqui se o FXML estiver correto.



tabela.setOnMouseClicked(e -> {

if (e.getClickCount() == 2) { // Duplo clique para carregar para edição

Conferente sel = tabela.getSelectionModel().getSelectedItem();

if (sel != null) {

preencherCampos(sel);

}

}

});

}



private void showAlert(Alert.AlertType alertType, String title, String message) {

Alert alert = new Alert(alertType);

alert.setTitle(title);

alert.setHeaderText(null);

alert.setContentText(message);

alert.showAndWait();

}


private void preencherCampos(Conferente c) {

if (c == null) {

txtId.clear();

txtNome.clear();

return;

}

txtId.setText(String.valueOf(c.getId()));

txtNome.setText(c.getNome());

}



private void carregarDoBanco() {

lista.clear();

String sql = "SELECT id_conferente, nome_conferente FROM conferentes ORDER BY nome_conferente";

try (Connection conn = ConexaoBD.getConnection();

Statement st = conn.createStatement();

ResultSet rs = st.executeQuery(sql)) {

while (rs.next()) {

long i = rs.getLong("id_conferente");

String n = rs.getString("nome_conferente");

lista.add(new Conferente(i, n));

}

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao carregar conferentes: " + e.getMessage());

}

}



@FXML

private void novaConferente(ActionEvent event) {

long newId = gerarIdConferente();

if (newId <= 0) {

// showAlert já é chamado em gerarIdConferente se houver erro

return;

}

txtId.setText(String.valueOf(newId));

txtNome.clear();

txtNome.requestFocus();

tabela.getSelectionModel().clearSelection(); // Limpa seleção da tabela

}



private long gerarIdConferente() {

String sql = "SELECT nextval('seq_conferente') as prox_id";

try (Connection conn = ConexaoBD.getConnection();

Statement st = conn.createStatement();

ResultSet rs = st.executeQuery(sql)) {

if (rs.next()) {

return rs.getLong("prox_id");

}

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao gerar ID para conferente: " + e.getMessage());

}

return -1;

}



@FXML

private void salvarConferente(ActionEvent event) {

String idStr = txtId.getText().trim();

String nomeStr = txtNome.getText().trim();



if (idStr.isEmpty()) {

showAlert(Alert.AlertType.WARNING, "Atenção", "Clique em 'Novo' primeiro para gerar um ID.");

return;

}

if (nomeStr.isEmpty()) {

showAlert(Alert.AlertType.WARNING, "Atenção", "Digite o Nome do Conferente.");

return;

}



long idLong;

try {

idLong = Long.parseLong(idStr);

} catch (NumberFormatException e) {

showAlert(Alert.AlertType.ERROR, "Erro de Formato", "ID inválido!");

return;

}



boolean existe = false;

String sqlCheck = "SELECT 1 FROM conferentes WHERE id_conferente=?";

try (Connection conn = ConexaoBD.getConnection();

PreparedStatement pc = conn.prepareStatement(sqlCheck)) {

pc.setLong(1, idLong);

try (ResultSet rc = pc.executeQuery()) {

if (rc.next()) existe = true;

}

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao verificar conferente: " + e.getMessage());

return; // Não prosseguir se a verificação falhar

}



String sql;

boolean sucesso;



if (!existe) { // INSERT

sql = "INSERT INTO conferentes (id_conferente, nome_conferente) VALUES(?,?)";

try (Connection conn = ConexaoBD.getConnection();

PreparedStatement ps = conn.prepareStatement(sql)) {

ps.setLong(1, idLong);

ps.setString(2, nomeStr);

ps.executeUpdate();

sucesso = true;

showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Conferente cadastrado com sucesso!");

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao cadastrar conferente: " + e.getMessage());

sucesso = false;

}

} else { // UPDATE

sql = "UPDATE conferentes SET nome_conferente=? WHERE id_conferente=?";

try (Connection conn = ConexaoBD.getConnection();

PreparedStatement ps = conn.prepareStatement(sql)) {

ps.setString(1, nomeStr);

ps.setLong(2, idLong);

ps.executeUpdate();

sucesso = true;

showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Conferente atualizado com sucesso!");

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao atualizar conferente: " + e.getMessage());

sucesso = false;

}

}



if (sucesso) {

carregarDoBanco(); // Recarrega a lista da tabela

novaConferente(null); // Limpa os campos para o próximo

}

}



@FXML

private void excluirConferente(ActionEvent event) {

Conferente selecionado = tabela.getSelectionModel().getSelectedItem();

if (selecionado == null) {

// Se nada selecionado na tabela, tenta pegar do campo ID se estiver preenchido (após um "novo" ou "duplo clique")

if (!txtId.getText().isEmpty()) {

try {

long idParaExcluir = Long.parseLong(txtId.getText());

// Para ter o nome para o alerta, precisaríamos buscar do banco ou da lista

// Vamos simplificar pedindo para selecionar na tabela

showAlert(Alert.AlertType.WARNING, "Atenção", "Selecione um conferente na tabela para excluir.");

return;

} catch (NumberFormatException e) {

showAlert(Alert.AlertType.WARNING, "Atenção", "ID inválido nos campos. Selecione um conferente na tabela.");

return;

}

} else {

showAlert(Alert.AlertType.WARNING, "Atenção", "Selecione um conferente na tabela para excluir.");

return;

}

}


// Se chegou aqui, 'selecionado' tem o objeto da tabela

Alert conf = new Alert(Alert.AlertType.CONFIRMATION,

"Deseja realmente excluir o conferente: " + selecionado.getNome() + " (ID=" + selecionado.getId() + ")?",

ButtonType.YES, ButtonType.NO);

conf.setTitle("Confirmar Exclusão");

conf.setHeaderText(null);



Optional<ButtonType> resposta = conf.showAndWait();

if (resposta.isPresent() && resposta.get() == ButtonType.YES) {

String sqlDel = "DELETE FROM conferentes WHERE id_conferente=?";

try (Connection conn = ConexaoBD.getConnection();

PreparedStatement ps = conn.prepareStatement(sqlDel)) {

ps.setLong(1, selecionado.getId());

int affectedRows = ps.executeUpdate();

if (affectedRows > 0) {

showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Conferente excluído com sucesso!");

carregarDoBanco();

novaConferente(null); // Limpa os campos

} else {

showAlert(Alert.AlertType.WARNING, "Atenção", "Conferente não encontrado no banco (pode já ter sido excluído).");

}

} catch (SQLException e) {

e.printStackTrace();

showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao excluir conferente: " + e.getMessage());

}

}

}



// --- Classe interna Conferente ---

public static class Conferente {

private final LongProperty id;

private final StringProperty nome;



public Conferente(long i, String n) {

this.id = new SimpleLongProperty(i);

this.nome = new SimpleStringProperty(n);

}



public long getId() { return id.get(); }

public void setId(long i) { id.set(i); }

public LongProperty idProperty() { return id; }



public String getNome() { return nome.get(); }

public void setNome(String s) { nome.set(s); }

public StringProperty nomeProperty() { return nome; }



@Override

public boolean equals(Object o) {

if (this == o) return true;

if (o == null || getClass() != o.getClass()) return false;

Conferente that = (Conferente) o;

return getId() == that.getId();

}



@Override

public int hashCode() {

return Long.hashCode(getId());

}

}

}