package gui;

import gui.util.AlertHelper;
import gui.util.BffClient;
import gui.util.SessaoUsuario;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import util.AppLogger;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Fase 2 do OCR: apos upload + resposta do BFF, exibe os campos extraidos
 * num form editavel (remetente, destinatario, rota, itens, valor, obs).
 * Botao Salvar ainda nao cria encomenda/frete real — isso entra na Fase 3.
 */
public class LancarPorFotoController implements Initializable {

    private static final String TAG = "LancarPorFoto";

    @FXML private Label lblTitulo;
    @FXML private ComboBox<String> cbTipo;
    @FXML private Button btnEscolher;
    @FXML private Button btnProcessar;
    @FXML private Button btnSalvar;
    @FXML private Label lblArquivo;
    @FXML private Label lblStatus;
    @FXML private ImageView preview;
    @FXML private ProgressIndicator progress;
    @FXML private VBox boxRevisao;
    @FXML private VBox boxNumNota;
    @FXML private TextField txtRemetente;
    @FXML private TextField txtDestinatario;
    @FXML private TextField txtRota;
    @FXML private TextField txtNumNf;
    @FXML private TextArea txtObservacoes;
    @FXML private TableView<ItemOcr> tblItens;
    @FXML private TableColumn<ItemOcr, String> colNomeItem;
    @FXML private TableColumn<ItemOcr, Integer> colQuantidade;
    @FXML private TableColumn<ItemOcr, Double> colPrecoUnitario;
    @FXML private TableColumn<ItemOcr, Double> colSubtotal;
    @FXML private Label lblValorTotal;

    private File fotoSelecionada;
    private int idViagemAtiva = 0;
    private String tipoInicial = "encomenda";
    private Integer idLancamentoOcr;

    private final ObservableList<ItemOcr> itens = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbTipo.getItems().addAll("encomenda", "frete");
        cbTipo.getSelectionModel().select(tipoInicial);
        cbTipo.valueProperty().addListener((obs, oldV, newV) -> atualizarBoxNumNota());
        atualizarBoxNumNota();

        // Tabela de itens
        colNomeItem.setCellValueFactory(new PropertyValueFactory<>("nomeItem"));
        colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colPrecoUnitario.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colNomeItem.setCellFactory(TextFieldTableCell.forTableColumn());
        colNomeItem.setOnEditCommit(e -> { e.getRowValue().setNomeItem(e.getNewValue()); recalcTotal(); });

        colQuantidade.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colQuantidade.setOnEditCommit(e -> { e.getRowValue().setQuantidade(e.getNewValue()); recalcTotal(); });

        colPrecoUnitario.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colPrecoUnitario.setOnEditCommit(e -> { e.getRowValue().setPrecoUnitario(e.getNewValue()); recalcTotal(); });

        colSubtotal.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colSubtotal.setEditable(false); // calculado
        tblItens.setItems(itens);
    }

    private void atualizarBoxNumNota() {
        boolean mostra = "frete".equals(cbTipo.getValue());
        boxNumNota.setVisible(mostra);
        boxNumNota.setManaged(mostra);
    }

    public void setTipoInicial(String tipo) {
        this.tipoInicial = (tipo != null && ("encomenda".equals(tipo) || "frete".equals(tipo))) ? tipo : "encomenda";
        if (cbTipo != null) cbTipo.getSelectionModel().select(this.tipoInicial);
        if (lblTitulo != null) {
            lblTitulo.setText("Lançar " + ("frete".equals(this.tipoInicial) ? "Frete" : "Encomenda") + " por Foto (OCR)");
        }
    }

    public void setIdViagemAtiva(int idViagem) { this.idViagemAtiva = idViagem; }

    @FXML
    private void handleEscolherFoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar foto do documento");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        Stage stage = (Stage) btnEscolher.getScene().getWindow();
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        fotoSelecionada = f;
        lblArquivo.setText(f.getName() + " (" + (f.length() / 1024) + " KB)");
        try {
            preview.setImage(new Image(f.toURI().toString(), 260, 200, true, true, true));
        } catch (Exception e) {
            AppLogger.warn(TAG, "Preview falhou: " + e.getMessage());
        }
        btnProcessar.setDisable(false);
        lblStatus.setText("");
        esconderRevisao();
    }

    @FXML
    private void handleProcessar() {
        if (fotoSelecionada == null) { AlertHelper.warn("Selecione uma foto primeiro."); return; }
        if (!garantirAuth()) return;

        String tipo = cbTipo.getValue();
        String numNf = "frete".equals(tipo) ? (txtNumNf != null ? txtNumNf.getText() : null) : null;
        btnProcessar.setDisable(true);
        btnEscolher.setDisable(true);
        progress.setVisible(true);
        lblStatus.setText("Enviando foto e processando OCR... pode levar 10-30 segundos.");
        lblStatus.setStyle("-fx-text-fill: #F59E0B;");

        Thread bg = new Thread(() -> {
            try {
                Map<String, Object> resp = BffClient.getInstance().uploadOcr(
                        fotoSelecionada, tipo, idViagemAtiva, numNf);
                Platform.runLater(() -> preencherForm(resp, tipo));
            } catch (Exception ex) {
                AppLogger.error(TAG, "Erro no OCR: " + ex.getMessage(), ex);
                Platform.runLater(() -> {
                    lblStatus.setText("✗ Falha no OCR: " + ex.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                });
            } finally {
                Platform.runLater(() -> {
                    btnProcessar.setDisable(false);
                    btnEscolher.setDisable(false);
                    progress.setVisible(false);
                });
            }
        }, "OcrUpload-Worker");
        bg.setDaemon(true);
        bg.start();
    }

    @SuppressWarnings("unchecked")
    private void preencherForm(Map<String, Object> resp, String tipo) {
        lblStatus.setText("✓ OCR concluido! Revise os campos e clique Salvar.");
        lblStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");

        // Pega id do lancamento e dados_extraidos
        Object lanc = resp.get("lancamento");
        if (lanc instanceof Map) {
            Object id = ((Map<String, Object>) lanc).get("id");
            if (id instanceof Number) idLancamentoOcr = ((Number) id).intValue();
        }
        Object dadosObj = resp.get("dados_extraidos");
        Map<String, Object> dados = (dadosObj instanceof Map) ? (Map<String, Object>) dadosObj : new java.util.LinkedHashMap<>();

        txtRemetente.setText(str(dados.get("remetente")));
        txtDestinatario.setText(str(dados.get("destinatario")));
        txtRota.setText(str(dados.get("rota")));
        if ("frete".equals(tipo) && txtNumNf != null) {
            String nn = str(dados.get("numero_nota"));
            if (!nn.isEmpty()) txtNumNf.setText(nn);
        }
        txtObservacoes.setText(str(dados.get("observacoes")));

        // Itens
        itens.clear();
        Object itensObj = dados.get("itens");
        if (itensObj instanceof List) {
            for (Object it : (List<Object>) itensObj) {
                if (it instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) it;
                    ItemOcr io = new ItemOcr(
                            str(m.get("nome_item")),
                            num(m.get("quantidade"), 1).intValue(),
                            num(m.get("preco_unitario"), 0).doubleValue()
                    );
                    itens.add(io);
                }
            }
        }
        recalcTotal();
        mostrarRevisao();
    }

    private void recalcTotal() {
        double total = 0;
        for (ItemOcr i : itens) {
            i.atualizarSubtotal();
            total += i.getSubtotal();
        }
        tblItens.refresh();
        lblValorTotal.setText(String.format("R$ %.2f", total).replace('.', ','));
    }

    private void mostrarRevisao() {
        boxRevisao.setVisible(true);
        boxRevisao.setManaged(true);
        btnSalvar.setDisable(false);
    }

    private void esconderRevisao() {
        boxRevisao.setVisible(false);
        boxRevisao.setManaged(false);
        btnSalvar.setDisable(true);
        itens.clear();
    }

    @FXML
    private void handleAdicionarItem() {
        itens.add(new ItemOcr("", 1, 0.0));
        tblItens.getSelectionModel().select(itens.size() - 1);
        tblItens.scrollTo(itens.size() - 1);
        recalcTotal();
    }

    @FXML
    private void handleRemoverItem() {
        ItemOcr sel = tblItens.getSelectionModel().getSelectedItem();
        if (sel != null) {
            itens.remove(sel);
            recalcTotal();
        }
    }

    @FXML
    private void handleSalvar() {
        // Fase 3 implementara a criacao real de encomenda/frete
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Fase 3 em breve");
        a.setHeaderText("Dados revisados — salvamento pendente");
        a.setContentText("O lancamento OCR #" + idLancamentoOcr + " foi criado no servidor.\n"
                + "A criacao da " + cbTipo.getValue() + " real no banco local sera implementada na Fase 3.");
        a.showAndWait();
    }

    @FXML
    private void handleFechar() {
        Stage stage = (Stage) btnProcessar.getScene().getWindow();
        stage.close();
    }

    private boolean garantirAuth() {
        BffClient bff = BffClient.getInstance();
        if (bff.isAuthenticated()) return true;

        model.Usuario u = SessaoUsuario.getUsuarioLogado();
        String login = (u != null && u.getEmail() != null && !u.getEmail().isEmpty())
                ? u.getEmail() : (u != null ? u.getLoginUsuario() : "");

        Dialog<String> d = new Dialog<>();
        d.setTitle("Autenticacao OCR");
        d.setHeaderText("Digite sua senha para acessar o OCR");
        d.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        PasswordField pf = new PasswordField(); pf.setPromptText("senha");
        TextField loginField = new TextField(login); loginField.setPromptText("email / login");
        VBox box = new VBox(8, new Label("Login:"), loginField, new Label("Senha:"), pf);
        box.setPadding(new javafx.geometry.Insets(10));
        d.getDialogPane().setContent(box);
        Platform.runLater(pf::requestFocus);
        d.setResultConverter(bt -> bt == javafx.scene.control.ButtonType.OK ? pf.getText() : null);
        Optional<String> resp = d.showAndWait();
        if (!resp.isPresent()) return false;
        String senha = resp.get();
        if (senha == null || senha.isEmpty()) { AlertHelper.warn("Senha em branco."); return false; }
        String loginFinal = loginField.getText();
        if (loginFinal == null || loginFinal.isEmpty()) { AlertHelper.warn("Login em branco."); return false; }
        boolean ok = bff.login(loginFinal, senha);
        if (!ok) { AlertHelper.error("Falha no login OCR", "Detalhe: " + bff.getLastError()); return false; }
        return true;
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    private static Number num(Object o, Number def) {
        if (o instanceof Number) return (Number) o;
        if (o instanceof String) {
            try { return Double.parseDouble(((String) o).replace(',', '.')); }
            catch (NumberFormatException ignored) {}
        }
        return def;
    }

    // Modelo de linha editavel da tabela de itens
    public static class ItemOcr {
        private final SimpleStringProperty nomeItem = new SimpleStringProperty();
        private final SimpleIntegerProperty quantidade = new SimpleIntegerProperty();
        private final SimpleDoubleProperty precoUnitario = new SimpleDoubleProperty();
        private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty();

        public ItemOcr(String nome, int qtd, double preco) {
            this.nomeItem.set(nome); this.quantidade.set(qtd); this.precoUnitario.set(preco);
            atualizarSubtotal();
        }
        public void atualizarSubtotal() { this.subtotal.set(quantidade.get() * precoUnitario.get()); }

        public String getNomeItem() { return nomeItem.get(); }
        public void setNomeItem(String v) { nomeItem.set(v); }
        public int getQuantidade() { return quantidade.get(); }
        public void setQuantidade(int v) { quantidade.set(v); atualizarSubtotal(); }
        public double getPrecoUnitario() { return precoUnitario.get(); }
        public void setPrecoUnitario(double v) { precoUnitario.set(v); atualizarSubtotal(); }
        public double getSubtotal() { return subtotal.get(); }

        public SimpleStringProperty nomeItemProperty() { return nomeItem; }
        public SimpleIntegerProperty quantidadeProperty() { return quantidade; }
        public SimpleDoubleProperty precoUnitarioProperty() { return precoUnitario; }
        public SimpleDoubleProperty subtotalProperty() { return subtotal; }
    }
}
