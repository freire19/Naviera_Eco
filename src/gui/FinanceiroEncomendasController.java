package gui;

import dao.ConexaoBD;
import dao.DAOUtils;
import dao.ViagemDAO;
import gui.util.AlertHelper;
import gui.util.PermissaoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import gui.util.StatusPagamentoView;
import model.EncomendaFinanceiro;
import model.OpcaoViagem;
import util.AppLogger;
public class FinanceiroEncomendasController {
    @FXML private ComboBox<OpcaoViagem> cmbViagem;
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkApenasDevedores;
    @FXML private Label lblTotalPendente;
    @FXML private Button btnSair;
    @FXML private TableView<EncomendaFinanceiro> tabela;
    @FXML private TableColumn<EncomendaFinanceiro, String> colNumero;
    @FXML private TableColumn<EncomendaFinanceiro, String> colData;
    @FXML private TableColumn<EncomendaFinanceiro, String> colRemetente;
    @FXML private TableColumn<EncomendaFinanceiro, String> colDestinatario;
    @FXML private TableColumn<EncomendaFinanceiro, String> colTotal;
    @FXML private TableColumn<EncomendaFinanceiro, String> colPago;
    @FXML private TableColumn<EncomendaFinanceiro, String> colRestante;
    @FXML private TableColumn<EncomendaFinanceiro, String> colStatus;
    @FXML
    public void initialize() {
        if (!PermissaoService.isFinanceiro()) { PermissaoService.exigirFinanceiro("Financeiro Encomendas"); return; }
        configurarTabela();
        cmbViagem.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        chkApenasDevedores.selectedProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        // DR010: carrega viagens em background
        Thread bg = new Thread(() -> {
            try {
                carregarComboViagens();
            } catch (Exception e) {
                AppLogger.warn("FinanceiroEncomendasController", "Erro em FinanceiroEncomendasController (bg init): " + e.getMessage());
                javafx.application.Platform.runLater(() -> gui.util.AlertHelper.errorSafe("FinanceiroEncomendasController", e));
            }
        });
        bg.setDaemon(true);
        bg.start();

        try {
            tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception e) { AppLogger.warn("FinanceiroEncomendasController", "Erro em FinanceiroEncomendasController.initialize (CSS): " + e.getMessage()); }
    }
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    }
    // --- Abre a nova tela de Histórico ---
    public void verHistoricoEstornos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/HistoricoEstornos.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Histórico de Auditoria - Estornos");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.showAndWait();
        } catch(Exception e) {
            AppLogger.error("FinanceiroEncomendasController", e.getMessage(), e);
            AlertHelper.info("Erro interno. Contate o administrador."); AppLogger.warn("FinanceiroEncomendasController", "Erro ao abrir histórico: " + e.getMessage());
        }
    }
    public void setViagemInicial(int idViagem) {
        for (OpcaoViagem op : cmbViagem.getItems()) {
            if (op.id == idViagem) {
                cmbViagem.setValue(op);
                break;
            }
        }
    }
    private void configurarTabela() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colData.setCellValueFactory(new PropertyValueFactory<>("dataLancamento"));
        colRemetente.setCellValueFactory(new PropertyValueFactory<>("remetente"));
        colDestinatario.setCellValueFactory(new PropertyValueFactory<>("destinatario"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatado"));
        colPago.setCellValueFactory(new PropertyValueFactory<>("pagoFormatado"));
        colRestante.setCellValueFactory(new PropertyValueFactory<>("restanteFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<EncomendaFinanceiro, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    setStyle(StatusPagamentoView.getEstiloCelula(model.StatusPagamento.fromString(item)));
                }
            }
        });
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
            }
            lista.add(new OpcaoViagem(v.getId().intValue(), label));
        }
        ObservableList<OpcaoViagem> finalLista = lista;
        javafx.application.Platform.runLater(() -> cmbViagem.setItems(finalLista));
    }
    // DR211: buscar dados em background thread para nao bloquear FX thread
    public void carregarDados() {
        if (cmbViagem.getValue() == null) return;
        int idViagem = cmbViagem.getValue().id;
        boolean apenasDevedores = chkApenasDevedores.isSelected();
        String busca = txtBusca.getText() != null ? txtBusca.getText().toLowerCase() : "";

        Thread bg = new Thread(() -> {
            try {
                ObservableList<EncomendaFinanceiro> lista = FXCollections.observableArrayList();
                java.math.BigDecimal somaPendente = java.math.BigDecimal.ZERO;
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT e.id_encomenda, e.numero_encomenda, v.data_viagem, e.remetente, e.destinatario, e.total_a_pagar, e.valor_pago ");
                sql.append("FROM encomendas e JOIN viagens v ON e.id_viagem = v.id_viagem WHERE e.empresa_id = ? ");
                // D003: parametriza idViagem em vez de concatenar
                java.util.List<Object> params = new java.util.ArrayList<>();
                params.add(dao.DAOUtils.empresaId());
                if (idViagem > 0) { sql.append(" AND e.id_viagem = ?"); params.add(idViagem); }
                if (apenasDevedores) sql.append(" AND (e.valor_pago < e.total_a_pagar OR e.valor_pago IS NULL) ");
                if (!busca.isEmpty()) {
                    // DS003: escape de wildcards LIKE para evitar exfiltracao
                    String buscaEscapada = busca.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                    sql.append(" AND (LOWER(e.remetente) LIKE ? ESCAPE '\\' OR LOWER(e.destinatario) LIKE ? ESCAPE '\\') ");
                    params.add("%" + buscaEscapada + "%");
                    params.add("%" + buscaEscapada + "%");
                }
                sql.append(" ORDER BY e.id_encomenda DESC");
                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        Object p = params.get(i);
                        if (p instanceof Integer) stmt.setInt(i + 1, (Integer) p);
                        else stmt.setString(i + 1, p.toString());
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        while (rs.next()) {
                            java.math.BigDecimal total = rs.getBigDecimal("total_a_pagar");
                            java.math.BigDecimal pago = rs.getBigDecimal("valor_pago");
                            if (total == null) total = java.math.BigDecimal.ZERO;
                            if (pago == null) pago = java.math.BigDecimal.ZERO;
                            java.math.BigDecimal devendo = total.subtract(pago);
                            String dataFmt = "";
                            if (rs.getDate("data_viagem") != null) dataFmt = sdf.format(rs.getDate("data_viagem"));
                            lista.add(new EncomendaFinanceiro(
                                rs.getInt("id_encomenda"),
                                rs.getString("numero_encomenda"),
                                dataFmt,
                                rs.getString("remetente"),
                                rs.getString("destinatario"),
                                total, pago
                            ));
                            if (devendo.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) somaPendente = somaPendente.add(devendo);
                        }
                    }
                }
                final java.math.BigDecimal totalPend = somaPendente;
                javafx.application.Platform.runLater(() -> {
                    tabela.setItems(lista);
                    lblTotalPendente.setText(String.format("R$ %,.2f", totalPend));
                });
            } catch (Exception e) {
                AppLogger.error("FinanceiroEncomendasController", e.getMessage(), e);
                javafx.application.Platform.runLater(() -> AlertHelper.errorSafe("carregar encomendas financeiro", e));
            }
        });
        bg.setDaemon(true);
        bg.start();
    }
    public void darBaixa() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertHelper.info("Selecione uma encomenda na tabela para dar baixa.");
            return;
        }
        if (selecionada.getRestante().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) {
            AlertHelper.info("Esta encomenda já está quitada!");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/BaixaPagamento.fxml"));
            Parent root = loader.load();
            BaixaPagamentoController controller = loader.getController();
            controller.setDadosIniciais(
                selecionada.getTotal(),
                selecionada.getPago(),
                selecionada.getRestante()
            );
            Stage stage = new Stage();
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setTitle("Realizar Pagamento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            if (controller.isConfirmado()) {
                salvarPagamento(selecionada.getId(), controller, selecionada.getPago());
            }
        } catch (Exception e) {
            AppLogger.error("FinanceiroEncomendasController", e.getMessage(), e);
            AlertHelper.info("Erro interno. Contate o administrador."); AppLogger.warn("FinanceiroEncomendasController", "Erro ao abrir tela de pagamento: " + e.getMessage());
        }
    }

    // DL003: operacao de pagamento em transacao atomica (SELECT + UPDATE na mesma conexao)
    private void salvarPagamento(int idEncomenda, BaixaPagamentoController dados, java.math.BigDecimal jaPago) {
        java.math.BigDecimal novoPago = jaPago.add(dados.getValorPago());
        try (Connection con = ConexaoBD.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Buscar desconto ja armazenado e acumular (DL010)
                java.math.BigDecimal descontoAnterior = java.math.BigDecimal.ZERO;
                try (PreparedStatement stmtQ = con.prepareStatement("SELECT COALESCE(desconto, 0) FROM encomendas WHERE id_encomenda = ? AND empresa_id = ?")) {
                    stmtQ.setInt(1, idEncomenda);
                    stmtQ.setInt(2, dao.DAOUtils.empresaId());
                    try (ResultSet rs = stmtQ.executeQuery()) {
                        if (rs.next()) descontoAnterior = rs.getBigDecimal(1);
                    }
                }
                java.math.BigDecimal descontoTotal = descontoAnterior.add(dados.getDesconto());
                java.math.BigDecimal totalComDesconto = dados.getValorTotalOriginal().subtract(descontoTotal);
                String novoStatus = (novoPago.compareTo(totalComDesconto.subtract(model.StatusPagamento.TOLERANCIA_PAGAMENTO)) >= 0) ? "PAGO" : "PARCIAL";
                // DL055: total_a_pagar permanece bruto no banco; saldo calculado como total - desconto - pago (via Encomenda.getSaldoDevedor)
                String sql = "UPDATE encomendas SET valor_pago = ?, desconto = ?, tipo_pagamento = ?, caixa = ?, status_pagamento = ? WHERE id_encomenda = ? AND empresa_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setBigDecimal(1, novoPago);
                    stmt.setBigDecimal(2, descontoTotal);
                    stmt.setString(3, dados.getFormaPagamento());
                    stmt.setString(4, dados.getCaixa());
                    stmt.setString(5, novoStatus);
                    stmt.setInt(6, idEncomenda);
                    stmt.setInt(7, dao.DAOUtils.empresaId());
                    stmt.executeUpdate();
                }
                con.commit();
                AlertHelper.info("Pagamento registrado com sucesso!");
                carregarDados();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            AlertHelper.info("Erro interno. Contate o administrador."); AppLogger.warn("FinanceiroEncomendasController", "Erro ao salvar no banco: " + e.getMessage());
        }
    }
    // --- MÉTODO ESTORNAR (AGORA PRESENTE E CORRIGIDO) ---
    public void estornarPagamento() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) { AlertHelper.info("Selecione um item para estornar."); return; }
        if (selecionada.getPago().compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) { AlertHelper.info("Este item não tem pagamento para estornar."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/EstornoPagamento.fxml"));
            Parent root = loader.load();
            EstornoPagamentoController controller = loader.getController();
            controller.setDados(selecionada.getTotal(), selecionada.getPago());
            Stage stage = new Stage();
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setTitle("Estornar Pagamento - Área Restrita");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            if (controller.isConfirmado()) {
                java.math.BigDecimal vEstorno = controller.getValorEstorno();
                String motivo = controller.getMotivo();
                String forma = controller.getFormaDevolucao();
                int idAutorizador = controller.getIdAutorizador();
                String nomeAutorizador = controller.getNomeAutorizador();
                java.math.BigDecimal novoPago = selecionada.getPago().subtract(vEstorno);
                String novoStatus = (novoPago.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) ? "PARCIAL" : "PENDENTE";

                try (Connection con = ConexaoBD.getConnection()) {
                    con.setAutoCommit(false);
                    try {
                        String sqlUp = "UPDATE encomendas SET valor_pago = ?, status_pagamento = ? WHERE id_encomenda = ? AND empresa_id = ?";
                        try (PreparedStatement stmt = con.prepareStatement(sqlUp)) {
                            stmt.setBigDecimal(1, novoPago);
                            stmt.setString(2, novoStatus);
                            stmt.setInt(3, selecionada.getId());
                            stmt.setInt(4, dao.DAOUtils.empresaId());
                            stmt.executeUpdate();
                        }
                        String sqlLog = "INSERT INTO log_estornos_encomendas (id_encomenda, valor_estornado, motivo, forma_devolucao, id_usuario_autorizou, nome_autorizador) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = con.prepareStatement(sqlLog)) {
                            stmt.setInt(1, selecionada.getId());
                            stmt.setBigDecimal(2, vEstorno);
                            stmt.setString(3, motivo);
                            stmt.setString(4, forma);
                            stmt.setInt(5, idAutorizador);
                            stmt.setString(6, nomeAutorizador);
                            stmt.executeUpdate();
                        }
                        con.commit();
                        AlertHelper.info("Estorno realizado com sucesso!\nAutorizado por: " + nomeAutorizador);
                        carregarDados();
                    } catch (Exception ex) {
                        try { con.rollback(); } catch (Exception re) { AppLogger.error("FinanceiroEncomendasController", re.getMessage(), re); }
                        AppLogger.error("FinanceiroEncomendasController", ex.getMessage(), ex);
                        AlertHelper.info("Erro ao gravar estorno: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            AlertHelper.info("Erro interno. Contate o administrador."); AppLogger.warn("FinanceiroEncomendasController", "Erro ao abrir tela de estorno: " + e.getMessage());
        }
    }
    // --- GERA EXTRATO (AGORA ABRE MESMO SEM SELEÇÃO) ---
    public void gerarRelatorioCliente() {
        EncomendaFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        String nomeCliente = "";
        if (selecionada != null) {
            nomeCliente = selecionada.getRemetente();
        } else {
            nomeCliente = txtBusca.getText();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ExtratoClienteEncomenda.fxml"));
            Parent root = loader.load();
            ExtratoClienteEncomendaController controller = loader.getController();
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                controller.carregarExtrato(nomeCliente);
            } else {
                controller.carregarExtrato(""); // Abre vazio para pesquisar lá
            }
            Stage stage = new Stage();
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.setTitle("Extrato Financeiro");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            AlertHelper.info("Erro interno. Contate o administrador."); AppLogger.warn("FinanceiroEncomendasController", "Erro ao abrir extrato: " + e.getMessage());
        }
    }
}
