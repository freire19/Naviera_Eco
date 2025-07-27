package gui;

import dao.ConexaoBD; // Importa sua classe de conexão centralizada
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent; // Importar ActionEvent
import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Importar Initializable
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL; // Necessário para Initializable
import java.sql.*;
import java.util.ResourceBundle; // Necessário para Initializable

public class RotasController implements Initializable {

    @FXML private TextField txtId, txtOrigem, txtDestino;
    @FXML private Button btnNova, btnSalvar, btnEditar, btnExcluir;

    @FXML private TableView<Rota> tabelaRotas;
    @FXML private TableColumn<Rota, Long> colId;
    @FXML private TableColumn<Rota, String> colOrigem;
    @FXML private TableColumn<Rota, String> colDestino;

    private ObservableList<Rota> listaRotas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOrigem.setCellValueFactory(new PropertyValueFactory<>("origem"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));

        tabelaRotas.setItems(listaRotas);

        carregarRotasDoBanco();

        // Configurando as ações dos botões programaticamente
        btnNova.setOnAction(e -> novaRota(e));
        btnSalvar.setOnAction(e -> salvarRota(e));
        btnEditar.setOnAction(e -> editarRota(e));
        btnExcluir.setOnAction(e -> excluirRota(e));
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Não precisa de @FXML se não for chamado diretamente pelo FXML onAction
    private void carregarRotasDoBanco() {
        listaRotas.clear();
        String sql = "SELECT id, origem, destino FROM rotas ORDER BY id";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                long i = rs.getLong("id");
                String o = rs.getString("origem");
                String d = rs.getString("destino");
                listaRotas.add(new Rota(i, o, d));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao carregar rotas: " + e.getMessage());
        }
    }

    // Chamado pelo btnNova.setOnAction
    private void novaRota(ActionEvent event) {
        long novoId = gerarIdRota();
        if (novoId <= 0) {
            // A mensagem de erro já é exibida por gerarIdRota()
            return;
        }
        txtId.setText(String.valueOf(novoId));
        txtOrigem.clear();
        txtDestino.clear();
        txtOrigem.requestFocus();
    }

    private long gerarIdRota() {
        String sql = "SELECT nextval('seq_rotas') as prox";
        try (Connection conn = ConexaoBD.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("prox");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao gerar novo ID para rota: " + e.getMessage());
        }
        return -1; // Indica falha
    }

    // Chamado pelo btnSalvar.setOnAction
    private void salvarRota(ActionEvent event) {
        String idStr = txtId.getText().trim();
        String ori = txtOrigem.getText().trim();
        String des = txtDestino.getText().trim();

        if (idStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Atenção", "Clique em 'Nova Rota' primeiro para gerar um ID.");
            return;
        }
        if (ori.isEmpty() || des.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Atenção", "Preencha os campos Origem e Destino.");
            return;
        }

        long idLong;
        try {
            idLong = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erro de Formato", "O ID da rota é inválido!");
            return;
        }

        boolean existe = false;
        String sqlCheck = "SELECT 1 FROM rotas WHERE id=?";
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement psc = conn.prepareStatement(sqlCheck)) {
            psc.setLong(1, idLong);
            try (ResultSet rsc = psc.executeQuery()) {
                if (rsc.next()) {
                    existe = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao verificar existência da rota: " + e.getMessage());
            return; 
        }

        if (!existe) {
            // INSERT
            String sqlIns = "INSERT INTO rotas (id, origem, destino) VALUES(?,?,?)";
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlIns)) {
                ps.setLong(1, idLong);
                ps.setString(2, ori);
                ps.setString(3, des);
                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Rota cadastrada com sucesso!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao inserir nova rota: " + e.getMessage());
            }
        } else {
            // UPDATE
            String sqlUp = "UPDATE rotas SET origem=?, destino=? WHERE id=?";
            try (Connection conn = ConexaoBD.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlUp)) {
                ps.setString(1, ori);
                ps.setString(2, des);
                ps.setLong(3, idLong);
                ps.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Rota atualizada com sucesso!");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao atualizar rota: " + e.getMessage());
            }
        }
        carregarRotasDoBanco(); 
        novaRota(null); // Passando null pois o evento não é usado aqui, apenas para limpar campos
    }

    // Chamado pelo btnEditar.setOnAction
    private void editarRota(ActionEvent event) {
        Rota sel = tabelaRotas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "Seleção", "Nenhuma rota selecionada para editar.");
            return;
        }
        txtId.setText(String.valueOf(sel.getId()));
        txtOrigem.setText(sel.getOrigem());
        txtDestino.setText(sel.getDestino());
    }

    // Chamado pelo btnExcluir.setOnAction
    private void excluirRota(ActionEvent event) {
        Rota sel = tabelaRotas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "Seleção", "Nenhuma rota selecionada para excluir.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseja realmente excluir a rota ID=" + sel.getId() + " (" + sel.getOrigem() + " - " + sel.getDestino() + ") ?",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar Exclusão");
        conf.setHeaderText(null); 

        conf.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) { 
                String sqlDel = "DELETE FROM rotas WHERE id=?";
                try (Connection conn = ConexaoBD.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sqlDel)) {
                    ps.setLong(1, sel.getId());
                    int affectedRows = ps.executeUpdate();
                    if (affectedRows > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Rota excluída com sucesso!");
                        carregarRotasDoBanco(); 
                        novaRota(null); // Limpa campos após exclusão
                    } else {
                         showAlert(Alert.AlertType.WARNING, "Atenção", "Rota não encontrada no banco para exclusão.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erro de Banco de Dados", "Falha ao excluir rota: " + e.getMessage());
                }
            }
        });
    }

    // --- Classe interna Rota ---
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