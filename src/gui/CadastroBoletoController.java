package gui;

import dao.ConexaoBD;
import gui.util.PermissaoService;
import gui.util.SessaoUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CadastroBoletoController {

    @FXML private TextField txtDescricao;
    @FXML private TextField txtValor;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private Spinner<Integer> spParcelas;
    @FXML private VBox boxDatas; 
    @FXML private TextArea txtObs;
    
    // Lado Direito (Lista)
    @FXML private TableView<Boleto> tabela;
    @FXML private TableColumn<Boleto, String> colVencimento;
    @FXML private TableColumn<Boleto, String> colDescricao;
    @FXML private TableColumn<Boleto, String> colParcela;
    @FXML private TableColumn<Boleto, String> colValor;
    @FXML private TableColumn<Boleto, String> colStatus;
    @FXML private DatePicker dpFiltroData;
    
    @FXML private Button btnSair; // Botão novo
    
    private List<DatePicker> pickersDatas = new ArrayList<>();
    private int idViagemAtual = 0;
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @FXML
    public void initialize() {
        configurarTabela();
        spParcelas.valueProperty().addListener((obs, oldVal, newVal) -> gerarCamposData(newVal));
        gerarCamposData(1);

        // DR010+DR104: carrega dados em background, atualiza UI via Platform.runLater
        Thread bg = new Thread(() -> {
            try {
                buscarViagemAtual();
                // DR104: buscar categorias em bg, atualizar ComboBox na FX thread
                ObservableList<String> cats = FXCollections.observableArrayList();
                try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT nome FROM categorias_despesa ORDER BY nome").executeQuery()){
                    while(rs.next()) cats.add(rs.getString(1));
                } catch(Exception e) { System.err.println("Erro em CadastroBoletoController.carregarCategorias: " + e.getMessage()); }
                final ObservableList<String> finalCats = cats;
                javafx.application.Platform.runLater(() -> {
                    cmbCategoria.setItems(finalCats);
                    configurarAutocomplete(cmbCategoria, finalCats);
                    filtrar();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        bg.setDaemon(true);
        bg.start();
    }
    
    @FXML
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    }
    
    private void buscarViagemAtual() {
        String sql = "SELECT id_viagem FROM viagens WHERE is_atual = true ORDER BY id_viagem DESC LIMIT 1";
        try (Connection con = ConexaoBD.getConnection();
             ResultSet rs = con.prepareStatement(sql).executeQuery()) {
            if(rs.next()) idViagemAtual = rs.getInt("id_viagem");
            else buscarUltimaViagem();
        } catch (Exception e) { buscarUltimaViagem(); }
    }
    
    private void buscarUltimaViagem() {
        try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT id_viagem FROM viagens ORDER BY id_viagem DESC LIMIT 1").executeQuery()){
            if(rs.next()) idViagemAtual = rs.getInt("id_viagem");
        } catch(Exception e){ System.err.println("Erro em CadastroBoletoController.buscarUltimaViagem: " + e.getMessage()); }
    }

    private void gerarCamposData(int qtd) {
        boxDatas.getChildren().clear();
        pickersDatas.clear();
        LocalDate base = LocalDate.now();
        for(int i = 0; i < qtd; i++) {
            DatePicker dp = new DatePicker();
            dp.setValue(base.plusMonths(i + 1)); 
            dp.setPromptText("Vencimento Parcela " + (i+1));
            dp.setMaxWidth(Double.MAX_VALUE);
            boxDatas.getChildren().add(dp);
            pickersDatas.add(dp);
        }
    }

    private void carregarCategorias() {
        ObservableList<String> cats = FXCollections.observableArrayList();
        try(Connection c = ConexaoBD.getConnection(); ResultSet rs = c.prepareStatement("SELECT nome FROM categorias_despesa ORDER BY nome").executeQuery()){
            while(rs.next()) cats.add(rs.getString(1));
        } catch(Exception e) { System.err.println("Erro em CadastroBoletoController.carregarCategorias: " + e.getMessage()); }
        cmbCategoria.setItems(cats);
        configurarAutocomplete(cmbCategoria, cats);
    }
    
    private void configurarAutocomplete(ComboBox<String> comboBox, ObservableList<String> dados) {
        comboBox.setEditable(true);
        comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ObservableList<String> filtrados = FXCollections.observableArrayList();
            for (String item : dados) {
                if (item.toUpperCase().contains(newVal.toUpperCase())) filtrados.add(item);
            }
            if(newVal.isEmpty()) comboBox.setItems(dados);
            else comboBox.setItems(filtrados);
            if(!comboBox.isShowing()) comboBox.show();
        });
    }

    @FXML
    public void salvar() {
        if(txtDescricao.getText().isEmpty() || txtValor.getText().isEmpty()) {
            alert("Preencha descrição e valor."); return;
        }
        
        try {
            java.math.BigDecimal total = new java.math.BigDecimal(txtValor.getText().replace(",", "."));
            int parcelas = spParcelas.getValue();
            // DL014: distribui centavos da divisao na ultima parcela
            java.math.BigDecimal valorParcela = total.divide(java.math.BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.DOWN);
            java.math.BigDecimal valorUltimaParcela = total.subtract(valorParcela.multiply(java.math.BigDecimal.valueOf(parcelas - 1)));
            String cat = cmbCategoria.getValue();
            if(cat == null) cat = cmbCategoria.getEditor().getText();
            int idCat = buscarOuCriarCategoria(cat);
            
            try (Connection con = ConexaoBD.getConnection()) {
                con.setAutoCommit(false);

                String sqlFinanceiro = "INSERT INTO financeiro_saidas (descricao, valor_total, data_vencimento, status, forma_pagamento, id_categoria, numero_parcela, total_parcelas, observacoes, id_viagem) VALUES (?, ?, ?, 'PENDENTE', 'BOLETO', ?, ?, ?, ?, ?)";
                String sqlAgenda = "INSERT INTO agenda_anotacoes (data_evento, descricao, concluida) VALUES (?, ?, false)";

                try (PreparedStatement stmtFin = con.prepareStatement(sqlFinanceiro);
                     PreparedStatement stmtAgenda = con.prepareStatement(sqlAgenda)) {

                    for (int i = 0; i < parcelas; i++) {
                        LocalDate vencimento = pickersDatas.get(i).getValue();
                        if (vencimento == null) vencimento = LocalDate.now().plusMonths(i + 1);

                        // DL014: ultima parcela absorve centavos restantes
                        java.math.BigDecimal valorDestaParcela = (i == parcelas - 1) ? valorUltimaParcela : valorParcela;

                        stmtFin.setString(1, txtDescricao.getText());
                        stmtFin.setBigDecimal(2, valorDestaParcela);
                        stmtFin.setDate(3, Date.valueOf(vencimento));
                        stmtFin.setInt(4, idCat);
                        stmtFin.setInt(5, i + 1);
                        stmtFin.setInt(6, parcelas);
                        stmtFin.setString(7, txtObs.getText());
                        stmtFin.setInt(8, idViagemAtual);
                        stmtFin.addBatch();

                        stmtAgenda.setDate(1, Date.valueOf(vencimento));
                        stmtAgenda.setString(2, "VENCIMENTO BOLETO: " + txtDescricao.getText() + " (" + (i + 1) + "/" + parcelas + ") - R$ " + String.format("%.2f", valorDestaParcela));
                        stmtAgenda.addBatch();
                    }
                    stmtFin.executeBatch();
                    stmtAgenda.executeBatch();

                    con.commit();

                    alert("Boletos gerados e adicionados à Agenda!");
                    txtDescricao.clear();
                    txtValor.clear();
                    filtrar();

                } catch (Exception ex) {
                    con.rollback();
                    ex.printStackTrace();
                }
            }
            
        } catch (Exception e) { e.printStackTrace(); alert("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage()); }
    }
    
    private int buscarOuCriarCategoria(String nome) throws SQLException {
        if(nome == null || nome.isEmpty()) return 1;
        // #DB008: Connection em try-with-resources (antes vazava em cada early return)
        try (Connection con = ConexaoBD.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("SELECT id FROM categorias_despesa WHERE nome = ?")) {
                stmt.setString(1, nome.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if(rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement stmt = con.prepareStatement("INSERT INTO categorias_despesa (nome) VALUES (?) RETURNING id")) {
                stmt.setString(1, nome.toUpperCase());
                try (ResultSet rs = stmt.executeQuery()) {
                    if(rs.next()) return rs.getInt(1);
                }
            }
        }
        return 1;
    }
    
    private void configurarTabela() {
        colVencimento.setCellValueFactory(new PropertyValueFactory<>("vencimento"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colParcela.setCellValueFactory(new PropertyValueFactory<>("parcelaStr"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(col -> new TableCell<Boleto, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                }
            }
        });
        
        // Tenta carregar o CSS
        try { tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); } catch(Exception e){ System.err.println("Erro em CadastroBoletoController.configurarTabela (CSS): " + e.getMessage()); }
    }
    
    @FXML
    public void filtrar() {
        ObservableList<Boleto> lista = FXCollections.observableArrayList();
        // DL049: filtrar boletos pela viagem ativa
        StringBuilder sql = new StringBuilder("SELECT * FROM financeiro_saidas WHERE forma_pagamento = 'BOLETO' ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (idViagemAtual > 0) {
            sql.append(" AND id_viagem = ?");
            params.add(idViagemAtual);
        }
        if(dpFiltroData.getValue() != null) {
            sql.append(" AND data_vencimento = ?");
            params.add(java.sql.Date.valueOf(dpFiltroData.getValue()));
        }
        sql.append(" ORDER BY data_vencimento ASC");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try(Connection c = ConexaoBD.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())){
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) ps.setInt(i + 1, (Integer) param);
                else if (param instanceof java.sql.Date) ps.setDate(i + 1, (java.sql.Date) param);
            }
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                lista.add(new Boleto(
                    rs.getInt("id"),
                    sdf.format(rs.getDate("data_vencimento")),
                    rs.getString("descricao"),
                    rs.getInt("numero_parcela") + "/" + rs.getInt("total_parcelas"),
                    rs.getDouble("valor_total"),
                    rs.getString("status")
                ));
            }
            tabela.setItems(lista);
        } catch(Exception e){e.printStackTrace();}
    }
    
    @FXML public void limparFiltros() { dpFiltroData.setValue(null); filtrar(); }
    
    @FXML
    public void darBaixa() {
        Boleto sel = tabela.getSelectionModel().getSelectedItem();
        if(sel == null) { alert("Selecione um boleto."); return; }
        if(sel.getStatus().equals("PAGO")) { alert("Já está pago."); return; }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("DINHEIRO", "DINHEIRO", "PIX", "CARTAO", "TRANSFERENCIA");
        dialog.setTitle("Pagar Boleto");
        dialog.setHeaderText("Confirmar pagamento de " + sel.getValorFormatado());
        dialog.setContentText("Como foi pago?");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String formaEscolhida = result.get();
            try(Connection c = ConexaoBD.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE financeiro_saidas SET status='PAGO', forma_pagamento=?, data_pagamento=CURRENT_DATE, valor_pago=valor_total WHERE id=?")) {
                s.setString(1, formaEscolhida); 
                s.setInt(2, sel.getId());
                s.executeUpdate();
                filtrar();
            } catch(Exception e) { e.printStackTrace(); }
        }
    }
    
    @FXML
    public void excluir() {
        if (!PermissaoService.exigirAdmin("Excluir registro financeiro")) return;
        Boleto sel = tabela.getSelectionModel().getSelectedItem();
        if(sel == null) return;
        if(new Alert(Alert.AlertType.CONFIRMATION, "Excluir boleto: " + sel.getDescricao() + "?").showAndWait().get() == ButtonType.OK) {
            try(Connection c = ConexaoBD.getConnection()) {
                // Registrar em auditoria antes de deletar
                String usuario = SessaoUsuario.isUsuarioLogado() ? SessaoUsuario.getUsuarioLogado().getNomeCompleto() : "DESCONHECIDO";
                try (PreparedStatement audit = c.prepareStatement(
                        "INSERT INTO auditoria_financeiro (tipo_operacao, descricao, usuario_solicitante, data_hora, detalhe_valor) VALUES (?, ?, ?, NOW(), ?)")) {
                    audit.setString(1, "EXCLUSAO_BOLETO");
                    audit.setString(2, "Exclusao de boleto: " + sel.getDescricao());
                    audit.setString(3, usuario);
                    audit.setString(4, "Valor: " + sel.getValorFormatado() + " | Vencimento: " + sel.getVencimento());
                    audit.executeUpdate();
                }
                // Agora deleta
                try (PreparedStatement s = c.prepareStatement("DELETE FROM financeiro_saidas WHERE id=?")) {
                    s.setInt(1, sel.getId());
                    s.executeUpdate();
                }
                filtrar();
            } catch(Exception e) { e.printStackTrace(); }
        }
    }

    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).show(); }

    public static class Boleto {
        private int id;
        private String vencimento, descricao, parcelaStr, status;
        private Double valor;
        public Boleto(int id, String v, String d, String p, Double val, String s) {
            this.id=id; this.vencimento=v; this.descricao=d; this.parcelaStr=p; this.valor=val; this.status=s;
        }
        public int getId() { return id; }
        public String getVencimento() { return vencimento; }
        public String getDescricao() { return descricao; }
        public String getParcelaStr() { return parcelaStr; }
        public Double getValor() { return valor; }
        public String getStatus() { return status; }
        public String getValorFormatado() { return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor); }
    }
}