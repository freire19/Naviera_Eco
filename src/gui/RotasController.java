package gui;

import dao.RotaDAO;
import javafx.application.Platform;
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
import java.util.List;
import java.util.ResourceBundle;
import gui.util.AlertHelper;
import gui.util.AppLogger;

public class RotasController implements Initializable {

    @FXML private TextField txtId, txtOrigem, txtDestino;
    @FXML private Button btnNova, btnSalvar, btnEditar, btnExcluir;

    @FXML private TableView<Rota> tabelaRotas;
    @FXML private TableColumn<Rota, Long> colId;
    @FXML private TableColumn<Rota, String> colOrigem;
    @FXML private TableColumn<Rota, String> colDestino;

    private final ObservableList<Rota> listaRotas = FXCollections.observableArrayList();
    private final RotaDAO rotaDAO = new RotaDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!gui.util.PermissaoService.isAdmin()) { gui.util.PermissaoService.exigirAdmin("Cadastro de Rotas"); return; }
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrigem.setCellValueFactory(new PropertyValueFactory<>("origem"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));

        tabelaRotas.setItems(listaRotas);

        // DR117: background thread para nao bloquear FX thread
        Thread bg = new Thread(() -> {
            try {
                List<model.Rota> dados = rotaDAO.listarTodasAsRotasComoObjects();
                List<Rota> vms = new java.util.ArrayList<>();
                for (model.Rota r : dados) {
                    vms.add(toViewModel(r));
                }
                Platform.runLater(() -> listaRotas.setAll(vms));
            } catch (Exception e) {
                AppLogger.warn("RotasController", "Erro ao carregar rotas: " + e.getMessage());
            }
        });
        bg.setDaemon(true);
        bg.start();

        btnNova.setOnAction(e -> novaRota(e));
        btnSalvar.setOnAction(e -> salvarRota(e));
        btnEditar.setOnAction(e -> editarRota(e));
        btnExcluir.setOnAction(e -> excluirRota(e));
    }

    private void carregarRotasDoBanco() {
        try {
            List<model.Rota> dados = rotaDAO.listarTodasAsRotasComoObjects();
            listaRotas.clear();
            for (model.Rota r : dados) {
                listaRotas.add(toViewModel(r));
            }
        } catch (Exception e) {
            AppLogger.error("RotasController", e.getMessage(), e);
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao carregar rotas: " + e.getMessage());
        }
    }

    private void novaRota(ActionEvent event) {
        long novoId = rotaDAO.gerarProximoIdRota();
        if (novoId <= 0) {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao gerar novo ID para rota.");
            return;
        }
        txtId.setText(String.valueOf(novoId));
        txtOrigem.clear();
        txtDestino.clear();
        txtOrigem.requestFocus();
    }

    private void salvarRota(ActionEvent event) {
        String idStr = txtId.getText().trim();
        String ori = txtOrigem.getText().trim();
        String des = txtDestino.getText().trim();

        if (idStr.isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Clique em 'Nova Rota' primeiro para gerar um ID.");
            return;
        }
        if (ori.isEmpty() || des.isEmpty()) {
            AlertHelper.show(Alert.AlertType.WARNING, "Atenção", "Preencha os campos Origem e Destino.");
            return;
        }

        long idLong;
        try {
            idLong = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Formato", "O ID da rota é inválido!");
            return;
        }

        model.Rota rota = new model.Rota();
        rota.setId(idLong);
        rota.setOrigem(ori);
        rota.setDestino(des);

        boolean existe = rotaDAO.buscarPorId(idLong) != null;
        boolean ok = existe ? rotaDAO.atualizar(rota) : rotaDAO.inserir(rota);

        if (ok) {
            AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", existe ? "Rota atualizada com sucesso!" : "Rota cadastrada com sucesso!");
        } else {
            AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados", existe ? "Falha ao atualizar rota." : "Falha ao inserir nova rota.");
        }
        carregarRotasDoBanco();
        novaRota(null);
    }

    private void editarRota(ActionEvent event) {
        Rota sel = tabelaRotas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertHelper.show(Alert.AlertType.INFORMATION, "Seleção", "Nenhuma rota selecionada para editar.");
            return;
        }
        txtId.setText(String.valueOf(sel.getId()));
        txtOrigem.setText(sel.getOrigem());
        txtDestino.setText(sel.getDestino());
    }

    private void excluirRota(ActionEvent event) {
        Rota sel = tabelaRotas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertHelper.show(Alert.AlertType.INFORMATION, "Seleção", "Nenhuma rota selecionada para excluir.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir a rota ID=" + sel.getId() + " (" + sel.getOrigem() + " - " + sel.getDestino() + ") ?",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar Exclusão");
        conf.setHeaderText(null);

        conf.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                boolean ok = rotaDAO.excluir(sel.getId());
                if (ok) {
                    AlertHelper.show(Alert.AlertType.INFORMATION, "Sucesso", "Rota excluída com sucesso!");
                    carregarRotasDoBanco();
                    novaRota(null);
                } else {
                    AlertHelper.show(Alert.AlertType.ERROR, "Erro de Banco de Dados",
                            "Falha ao excluir rota. Verifique se existem viagens vinculadas a esta rota.");
                }
            }
        });
    }

    /** Converte model.Rota (DAO) para Rota (ViewModel com JavaFX Properties). */
    private static Rota toViewModel(model.Rota r) {
        long id = r.getId() != null ? r.getId() : 0L;
        return new Rota(id, r.getOrigem(), r.getDestino());
    }

    // --- Classe interna Rota (ViewModel com JavaFX Properties para TableView) ---
    public static class Rota {
        private final LongProperty id;
        private final StringProperty origem;
        private final StringProperty destino;

        public Rota(long i, String o, String d) {
            this.id = new SimpleLongProperty(i);
            this.origem = new SimpleStringProperty(o);
            this.destino = new SimpleStringProperty(d);
        }

        public long getId() { return id.get(); }
        public void setId(long i) { id.set(i); }
        public LongProperty idProperty() { return id; }

        public String getOrigem() { return origem.get(); }
        public void setOrigem(String o) { origem.set(o); }
        public StringProperty origemProperty() { return origem; }

        public String getDestino() { return destino.get(); }
        public void setDestino(String d) { destino.set(d); }
        public StringProperty destinoProperty() { return destino; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rota rota = (Rota) o;
            return getId() == rota.getId();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(getId());
        }
    }
}
