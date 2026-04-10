package gui;

import dao.ConexaoBD;
import dao.DAOUtils;
import dao.ViagemDAO;
import gui.util.AlertHelper;
import gui.util.PermissaoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import gui.util.StatusPagamentoView;
import model.FreteFinanceiro;
import model.OpcaoViagem;
public class FinanceiroFretesController {
    @FXML private ComboBox<OpcaoViagem> cmbViagem;
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkApenasDevedores;
    @FXML private Label lblTotalPendente;
    @FXML private Button btnSair;
    @FXML private TableView<FreteFinanceiro> tabela;
    @FXML private TableColumn<FreteFinanceiro, String> colNumero;
    @FXML private TableColumn<FreteFinanceiro, String> colData;
    @FXML private TableColumn<FreteFinanceiro, String> colRemetente;
    @FXML private TableColumn<FreteFinanceiro, String> colDestinatario;
    @FXML private TableColumn<FreteFinanceiro, Integer> colVolumes;
    @FXML private TableColumn<FreteFinanceiro, String> colTotal;
    @FXML private TableColumn<FreteFinanceiro, String> colPago;
    @FXML private TableColumn<FreteFinanceiro, String> colRestante;
    @FXML private TableColumn<FreteFinanceiro, String> colStatus;
    @FXML
    public void initialize() {
        if (!PermissaoService.isFinanceiro()) { PermissaoService.exigirFinanceiro("Financeiro Fretes"); return; }
        configurarTabela();
        cmbViagem.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        chkApenasDevedores.selectedProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        // DR010: carrega viagens em background
        Thread bg = new Thread(() -> {
            try {
                carregarComboViagens();
            } catch (Exception e) {
                System.err.println("Erro em FinanceiroFretesController (bg init): " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("FinanceiroFretesController", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    public void verHistoricoEstornos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HistoricoEstornosFretes.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Histórico de Auditoria - Estornos de Fretes");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.info("Histórico de estornos de fretes ainda não implementado.");
        }
    public void setViagemInicial(int idViagem) {
        for (OpcaoViagem op : cmbViagem.getItems()) {
            if (op.id == idViagem) {
                cmbViagem.setValue(op);
                break;
    private void configurarTabela() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataViagem"));
        colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        colDestinatario.setCellValueFactory(new PropertyValueFactory<>("destinatario"));
        colVolumes.setCellValueFactory(new PropertyValueFactory<>("volumes"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatado"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("pagoFormatado"));
        colRestante.setCellValueFactory(new PropertyValueFactory<>("restanteFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<FreteFinanceiro, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(StatusPagamentoView.getEstiloCelula(model.StatusPagamento.fromString(item)));
                }
    private void carregarComboViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        lista.add(new OpcaoViagem(0, "Todas as Viagens"));
        for (model.Viagem v : new ViagemDAO().listarViagensRecentes(20)) {
            String label = v.getDescricao() != null ? v.getDescricao() : "";
            if (v.getDataViagem() != null) {
                label += " (" + sdf.format(java.sql.Date.valueOf(v.getDataViagem()));
                if (v.getDataChegada() != null) label += " - " + sdf.format(java.sql.Date.valueOf(v.getDataChegada()));
                label += ")";
            lista.add(new OpcaoViagem(v.getId().intValue(), label));
        ObservableList<OpcaoViagem> finalLista = lista;
        Platform.runLater(() -> cmbViagem.setItems(finalLista));
    public void carregarDados() {
        if (cmbViagem.getValue() == null) return;
        int idViagem = cmbViagem.getValue().id;
        ObservableList<FreteFinanceiro> lista = FXCollections.observableArrayList();
        java.math.BigDecimal somaPendente = java.math.BigDecimal.ZERO;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.id_frete, f.numero_frete, v.data_viagem, ");
        sql.append("f.remetente_nome_temp AS remetente, f.destinatario_nome_temp AS destinatario, ");
        sql.append("f.valor_total_itens AS valor_nominal, f.valor_pago, f.valor_devedor, ");
        sql.append("(SELECT COALESCE(SUM(fi.quantidade), 0) FROM frete_itens fi WHERE fi.id_frete = f.id_frete) AS total_volumes ");
        sql.append("FROM fretes f ");
        sql.append("LEFT JOIN viagens v ON f.id_viagem = v.id_viagem ");
        sql.append("WHERE f.status_frete != 'CANCELADO' ");
        // D003: parametriza idViagem
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (idViagem > 0) { sql.append(" AND f.id_viagem = ?"); params.add(idViagem); }
        if (chkApenasDevedores.isSelected()) sql.append(" AND (f.valor_devedor > 0.01 OR f.valor_pago IS NULL OR f.valor_pago < f.valor_total_itens) ");
        String busca = txtBusca.getText().toLowerCase();
        if (!busca.isEmpty()) {
            String buscaEscapada = busca.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            sql.append(" AND (LOWER(f.remetente_nome_temp) LIKE ? ESCAPE '\\' OR LOWER(f.destinatario_nome_temp) LIKE ? ESCAPE '\\') ");
            params.add("%" + buscaEscapada + "%");
        sql.append(" ORDER BY f.id_frete DESC");
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                else stmt.setString(i + 1, p.toString());
            ResultSet rs = stmt.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            while (rs.next()) {
                java.math.BigDecimal totalBD = rs.getBigDecimal("valor_nominal");
                java.math.BigDecimal pagoBD = rs.getBigDecimal("valor_pago");
                java.math.BigDecimal devendoBD = rs.getBigDecimal("valor_devedor");
                if (totalBD == null) totalBD = java.math.BigDecimal.ZERO;
                if (pagoBD == null) pagoBD = java.math.BigDecimal.ZERO;
                if (devendoBD == null) devendoBD = java.math.BigDecimal.ZERO;
                int volumes = rs.getInt("total_volumes");
                String dataFmt = "";
                if (rs.getDate("data_viagem") != null) dataFmt = sdf.format(rs.getDate("data_viagem"));
                lista.add(new FreteFinanceiro(
                        rs.getLong("id_frete"),
                        rs.getString("numero_frete"),
                        dataFmt,
                        rs.getString("remetente"),
                        rs.getString("destinatario"),
                        volumes,
                        totalBD, pagoBD
                ));
                if (devendoBD.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) somaPendente = somaPendente.add(devendoBD);
            tabela.setItems(lista);
            lblTotalPendente.setText(String.format("R$ %,.2f", somaPendente));
        } catch (SQLException e) {
    public void darBaixa() {
        FreteFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.info("Selecione um frete na tabela para dar baixa.");
            return;
        if (selecionada.getRestante().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) {
            AlertHelper.info("Este frete já está quitado!");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BaixaPagamento.fxml"));
            BaixaPagamentoController controller = loader.getController();
            controller.setDadosIniciais(
                    selecionada.getTotal(),
                    selecionada.getPago(),
                    selecionada.getRestante()
            );
            stage.setTitle("Realizar Pagamento - Frete");
            stage.setResizable(false);
            if (controller.isConfirmado()) {
                salvarPagamento(selecionada.getId(), controller, selecionada.getPago());
            AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao abrir tela de pagamento: " + e.getMessage());
    private void salvarPagamento(long idFrete, BaixaPagamentoController dados, java.math.BigDecimal jaPago) {
        // DL040: usar transacao unica para buscar desconto + atualizar (evita race condition)
        try (Connection con = ConexaoBD.getConnection()) {
            con.setAutoCommit(false);
                // Buscar desconto ja armazenado no banco (DL012)
                java.math.BigDecimal descontoAnterior = java.math.BigDecimal.ZERO;
                try (PreparedStatement stmtQ = con.prepareStatement("SELECT COALESCE(desconto, 0) FROM fretes WHERE id_frete = ? FOR UPDATE")) {
                    stmtQ.setLong(1, idFrete);
                    try (ResultSet rs = stmtQ.executeQuery()) {
                        if (rs.next()) descontoAnterior = rs.getBigDecimal(1);
                    }
                java.math.BigDecimal novoPago = jaPago.add(dados.getValorPago());
                java.math.BigDecimal descontoTotal = descontoAnterior.add(dados.getDesconto());
                java.math.BigDecimal totalComDesconto = dados.getValorTotalOriginal().subtract(descontoTotal);
                java.math.BigDecimal novoDevedor = totalComDesconto.subtract(novoPago).max(java.math.BigDecimal.ZERO);
                String novoStatus = (novoDevedor.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) ? "PAGO" : "PENDENTE";
                String sql = "UPDATE fretes SET valor_pago = ?, valor_devedor = ?, desconto = ?, tipo_pagamento = ?, nome_caixa = ?, status_frete = ? WHERE id_frete = ? AND empresa_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setBigDecimal(1, novoPago);
                    stmt.setBigDecimal(2, novoDevedor);
                    stmt.setBigDecimal(3, descontoTotal);
                    stmt.setString(4, dados.getFormaPagamento());
                    stmt.setString(5, dados.getCaixa());
                    stmt.setString(6, novoStatus);
                    stmt.setLong(7, idFrete);
                    stmt.executeUpdate();
                con.commit();
                AlertHelper.info("Pagamento registrado com sucesso!");
                carregarDados();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao salvar no banco: " + e.getMessage());
    public void estornarPagamento() {
            AlertHelper.info("Selecione um frete para estornar.");
        if (selecionada.getPago().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) {
            AlertHelper.info("Este frete não tem pagamento para estornar.");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/EstornoPagamento.fxml"));
            EstornoPagamentoController controller = loader.getController();
            controller.setDados(selecionada.getTotal(), selecionada.getPago());
            stage.setTitle("Estornar Pagamento - Área Restrita");
                java.math.BigDecimal vEstorno = controller.getValorEstorno();
                String motivo = controller.getMotivo();
                int idAutorizador = controller.getIdAutorizador();
                String nomeAutorizador = controller.getNomeAutorizador();
                java.math.BigDecimal novoPago = selecionada.getPago().subtract(vEstorno);
                java.math.BigDecimal novoDevedor = selecionada.getTotal().subtract(novoPago);
                // DL041: usar StatusPagamento.calcular para consistencia com resto do sistema
                String novoStatus = model.StatusPagamento.calcular(novoPago, selecionada.getTotal()).name();
                // #DB009: try-with-resources para fechar conexao automaticamente
                try (Connection con = ConexaoBD.getConnection()) {
                    con.setAutoCommit(false);
                    try {
                        // D021: DDL movido para database_scripts/ — tabela log_estornos_fretes deve existir no banco
                        String sqlUp = "UPDATE fretes SET valor_pago = ?, valor_devedor = ?, status_frete = ? WHERE id_frete = ? AND empresa_id = ?";
                        try (PreparedStatement stmt = con.prepareStatement(sqlUp)) {
                            stmt.setBigDecimal(1, novoPago);
                            stmt.setBigDecimal(2, novoDevedor);
                            stmt.setString(3, novoStatus);
                            stmt.setLong(4, selecionada.getId());
                            stmt.executeUpdate();
                        }
                        // Log do estorno
                        String sqlLog = "INSERT INTO log_estornos_fretes (id_frete, valor_estornado, motivo, id_usuario_autorizou, nome_autorizador) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = con.prepareStatement(sqlLog)) {
                            stmt.setLong(1, selecionada.getId());
                            stmt.setBigDecimal(2, vEstorno);
                            stmt.setString(3, motivo);
                            stmt.setInt(4, idAutorizador);
                            stmt.setString(5, nomeAutorizador);
                        con.commit();
                        AlertHelper.info("Estorno realizado com sucesso!\nAutorizado por: " + nomeAutorizador);
                        carregarDados();
                    } catch (Exception ex) {
                        try { con.rollback(); } catch (Exception re) { re.printStackTrace(); }
                        ex.printStackTrace();
                        AlertHelper.info("Erro ao gravar estorno: " + ex.getMessage());
            AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao abrir tela de estorno: " + e.getMessage());
    public void abrirNotaFrete() {
            AlertHelper.info("Selecione um frete para visualizar a nota.");
            // Buscar dados completos do frete no banco
            String sql = "SELECT f.numero_frete, f.remetente_nome_temp, f.destinatario_nome_temp, " +
                         "f.valor_total_itens, f.valor_pago, f.valor_devedor, f.data_emissao, " +
                         "v.data_viagem, f.conferente_temp, f.rota_temp " +
                         "FROM fretes f " +
                         "LEFT JOIN viagens v ON f.id_viagem = v.id_viagem " +
                         "WHERE f.id_frete = ?";
            
            String remetente = "", destinatario = "", conferente = "", rota = "", dataHora = "", pagamento = "";
            String numeroFrete = selecionada.getNumero();
            double total = 0;
            java.util.List<NotaFretePersonalizadaController.ItemNota> itens = new java.util.ArrayList<>();
            try (Connection con = ConexaoBD.getConnection();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, selecionada.getId());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    remetente = rs.getString("remetente_nome_temp") != null ? rs.getString("remetente_nome_temp") : "";
                    destinatario = rs.getString("destinatario_nome_temp") != null ? rs.getString("destinatario_nome_temp") : "";
                    conferente = rs.getString("conferente_temp") != null ? rs.getString("conferente_temp") : "";
                    rota = rs.getString("rota_temp") != null ? rs.getString("rota_temp") : "";
                    total = rs.getDouble("valor_total_itens");
                    double pago = rs.getDouble("valor_pago");
                    double devedor = rs.getDouble("valor_devedor");
                    pagamento = String.format("Pago: R$ %.2f | Devedor: R$ %.2f", pago, devedor);
                    
                    java.sql.Date dataEmissao = rs.getDate("data_emissao");
                    if (dataEmissao != null) {
                        dataHora = new java.text.SimpleDateFormat("dd/MM/yyyy").format(dataEmissao);
            // Buscar itens do frete
            String sqlItens = "SELECT fi.quantidade, fi.descricao, fi.valor_unitario " +
                              "FROM frete_itens fi " +
                              "WHERE fi.id_frete = ?";
                 PreparedStatement stmt = con.prepareStatement(sqlItens)) {
                while (rs.next()) {
                    itens.add(new NotaFretePersonalizadaController.ItemNota(
                        rs.getInt("quantidade"),
                        rs.getString("descricao"),
                        rs.getDouble("valor_unitario")
                    ));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NotaFretePersonalizada.fxml"));
            NotaFretePersonalizadaController controller = loader.getController();
            controller.setDadosNotaFrete(remetente, destinatario, conferente, rota, dataHora, pagamento, total, itens);
            stage.setTitle("Nota do Frete - " + selecionada.getNumero());
            stage.setResizable(true);
            AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao abrir nota do frete: " + e.getMessage());
}
