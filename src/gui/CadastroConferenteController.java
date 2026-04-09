package gui;

import dao.ConferenteDAO;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import gui.util.AlertHelper;

public class CadastroConferenteController implements Initializable {

    @FXML private TextField txtId;
    @FXML private TextField txtNome;
    @FXML private Button btnNova;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private TableView<Conferente> tabela;
    @FXML private TableColumn<Conferente, Long> colId;
    @FXML private TableColumn<Conferente, String> colNome;

    private ObservableList<Conferente> lista = FXCollections.observableArrayList();
    private final ConferenteDAO conferenteDAO = new ConferenteDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) {
            gui.util.PermissaoService.exigirAdmin("Cadastro de Conferentes");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        tabela.setItems(lista);

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<ConferenteDAO.ConferenteRow> dados = conferenteDAO.listarComId();
                Platform.runLater(() -> {
                    lista.clear();
                    for (ConferenteDAO.ConferenteRow row : dados) {
                        lista.add(new Conferente(row.id, row.nome));
                    }
                });
            } catch (Exception e) {
                System.err.println("Erro ao carregar conferentes: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Conferente sel = tabela.getSelectionModel().getSelectedItem();
                if (sel != null) preencherCampos(sel);
            }
        });
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
        try {
            List<ConferenteDAO.ConferenteRow> dados = conferenteDAO.listarComId();
            lista.clear();
            for (ConferenteDAO.ConferenteRow row : dados) {
                lista.add(new Conferente(row.id, row.nome));
            }
        } catch (Exception e) {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados",
                    "Falha ao carregar conferentes: " + e.getMessage());
        }
    }

    @FXML
    private void novaConferente(ActionEvent event) {
        try {
            long newId = conferenteDAO.gerarProximoId();
            txtId.setText(String.valueOf(newId));
            txtNome.clear();
            txtNome.requestFocus();
            tabela.getSelectionModel().clearSelection();
        } catch (Exception e) {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados",
                    "Falha ao gerar ID para conferente: " + e.getMessage());
        }
    }

    @FXML
    private void salvarConferente(ActionEvent event) {
        String idStr = txtId.getText().trim();
        String nomeStr = txtNome.getText().trim();

        if (idStr.isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Clique em 'Novo' primeiro para gerar um ID.");
            return;
        }
        if (nomeStr.isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Digite o Nome do Conferente.");
            return;
        }

        long idLong;
        try {
            idLong = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Formato", "ID inválido!");
            return;
        }

        boolean sucesso;
        if (!conferenteDAO.existe(idLong)) {
            sucesso = conferenteDAO.inserir(idLong, nomeStr);
            if (sucesso) AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Conferente cadastrado com sucesso!");
            else AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao cadastrar conferente.");
        } else {
            sucesso = conferenteDAO.atualizar(idLong, nomeStr);
            if (sucesso) AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Conferente atualizado com sucesso!");
            else AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao atualizar conferente.");
        }

        if (sucesso) {
            carregarDoBanco();
            novaConferente(null);
        }
    }

    @FXML
    private void excluirConferente(ActionEvent event) {
        Conferente selecionado = tabela.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Selecione um conferente na tabela para excluir.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir o conferente: " + selecionado.getNome() +
                " (ID=" + selecionado.getId() + ")?",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar Exclusão");
        conf.setHeaderText(null);

        Optional<ButtonType> resposta = conf.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.YES) {
            boolean ok = conferenteDAO.excluir(selecionado.getId());
            if (ok) {
                AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Conferente excluído com sucesso!");
                carregarDoBanco();
                novaConferente(null);
            } else {
                AlertHelper.show(Alert.AlertType.WARNING, "Atenção",
                        "Conferente não encontrado no banco (pode já ter sido excluído).");
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
