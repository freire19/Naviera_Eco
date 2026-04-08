package gui;

import dao.ConexaoBD;
import gui.util.PermissaoService;
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
        carregarComboViagens();

        cmbViagem.valueProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        txtBusca.textProperty().addListener((obs, oldVal, newVal) -> carregarDados());
        chkApenasDevedores.selectedProperty().addListener((obs, oldVal, newVal) -> carregarDados());
    }

    @FXML
    public void sair() {
        Stage stage = (Stage) btnSair.getScene().getWindow();
        stage.close();
    }

    @FXML
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
            alert("Histórico de estornos de fretes ainda não implementado.");
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
                    setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                }
            }
        });
    }

    private void carregarComboViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        String sql = "SELECT id_viagem, descricao, data_viagem, data_chegada FROM viagens ORDER BY id_viagem DESC LIMIT 20";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            lista.add(new OpcaoViagem(0, "Todas as Viagens"));

            while (rs.next()) {
                String label = rs.getString("descricao");
                java.sql.Date dtSaida = rs.getDate("data_viagem");
                java.sql.Date dtChegada = rs.getDate("data_chegada");

                if (dtSaida != null) {
                    label += " (" + sdf.format(dtSaida);
                    if (dtChegada != null) label += " - " + sdf.format(dtChegada);
                    label += ")";
                }
                lista.add(new OpcaoViagem(rs.getInt("id_viagem"), label));
            }
            cmbViagem.setItems(lista);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void carregarDados() {
        if (cmbViagem.getValue() == null) return;
        int idViagem = cmbViagem.getValue().id;

        ObservableList<FreteFinanceiro> lista = FXCollections.observableArrayList();
        double somaPendente = 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.id_frete, f.numero_frete, v.data_viagem, ");
        sql.append("f.remetente_nome_temp AS remetente, f.destinatario_nome_temp AS destinatario, ");
        sql.append("f.valor_total_itens AS valor_nominal, f.valor_pago, f.valor_devedor, ");
        sql.append("(SELECT COALESCE(SUM(fi.quantidade), 0) FROM frete_itens fi WHERE fi.id_frete = f.id_frete) AS total_volumes ");
        sql.append("FROM fretes f ");
        sql.append("LEFT JOIN viagens v ON f.id_viagem = v.id_viagem ");
        sql.append("WHERE f.status_frete != 'CANCELADO' ");

        if (idViagem > 0) sql.append(" AND f.id_viagem = ").append(idViagem);
        if (chkApenasDevedores.isSelected()) sql.append(" AND (f.valor_devedor > 0.01 OR f.valor_pago IS NULL OR f.valor_pago < f.valor_total_itens) ");

        String busca = txtBusca.getText().toLowerCase();
        if (!busca.isEmpty()) {
            sql.append(" AND (LOWER(f.remetente_nome_temp) LIKE ? OR LOWER(f.destinatario_nome_temp) LIKE ?) ");
        }

        sql.append(" ORDER BY f.id_frete DESC");

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            if (!busca.isEmpty()) {
                stmt.setString(1, "%" + busca + "%");
                stmt.setString(2, "%" + busca + "%");
            }

            ResultSet rs = stmt.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            java.math.BigDecimal somaPendenteBD = java.math.BigDecimal.ZERO;
            while (rs.next()) {
                double total = rs.getDouble("valor_nominal");
                double pago = rs.getDouble("valor_pago");
                double devendo = rs.getDouble("valor_devedor");
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
                        total, pago
                ));
                if (devendo > 0.01) somaPendenteBD = somaPendenteBD.add(java.math.BigDecimal.valueOf(devendo));
            }
            tabela.setItems(lista);
            lblTotalPendente.setText(String.format("R$ %,.2f", somaPendenteBD));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void darBaixa() {
        FreteFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alert("Selecione um frete na tabela para dar baixa.");
            return;
        }
        if (selecionada.getRestante() <= 0.01) {
            alert("Este frete já está quitado!");
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
            stage.setTitle("Realizar Pagamento - Frete");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                salvarPagamento(selecionada.getId(), controller, selecionada.getPago());
            }

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro ao abrir tela de pagamento: " + e.getMessage());
        }
    }

    private void salvarPagamento(long idFrete, BaixaPagamentoController dados, double jaPago) {
        java.math.BigDecimal bdJaPago = java.math.BigDecimal.valueOf(jaPago);
        java.math.BigDecimal novoPago = bdJaPago.add(dados.getValorPago());
        // Buscar desconto ja armazenado no banco e somar com o novo (DL012)
        java.math.BigDecimal descontoAnterior = java.math.BigDecimal.ZERO;
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmtQ = con.prepareStatement("SELECT COALESCE(desconto, 0) FROM fretes WHERE id_frete = ?")) {
            stmtQ.setLong(1, idFrete);
            try (ResultSet rs = stmtQ.executeQuery()) {
                if (rs.next()) descontoAnterior = rs.getBigDecimal(1);
            }
        } catch (SQLException e) { System.err.println("Erro ao buscar desconto anterior: " + e.getMessage()); }

        java.math.BigDecimal descontoTotal = descontoAnterior.add(dados.getDesconto());
        java.math.BigDecimal totalComDesconto = dados.getValorTotalOriginal().subtract(descontoTotal);
        java.math.BigDecimal novoDevedor = totalComDesconto.subtract(novoPago).max(java.math.BigDecimal.ZERO);
        String novoStatus = (novoDevedor.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) <= 0) ? "PAGO" : "PENDENTE";

        // DL011: gravar tipo_pagamento e nome_caixa junto
        String sql = "UPDATE fretes SET valor_pago = ?, valor_devedor = ?, desconto = ?, tipo_pagamento = ?, nome_caixa = ?, status_frete = ? WHERE id_frete = ?";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setBigDecimal(1, novoPago);
            stmt.setBigDecimal(2, novoDevedor);
            stmt.setBigDecimal(3, descontoTotal);
            stmt.setString(4, dados.getFormaPagamento());
            stmt.setString(5, dados.getCaixa());
            stmt.setString(6, novoStatus);
            stmt.setLong(7, idFrete);
            stmt.executeUpdate();

            alert("Pagamento registrado com sucesso!");
            carregarDados();

        } catch (SQLException e) {
            alert("Erro ao salvar no banco: " + e.getMessage());
        }
    }

    @FXML
    public void estornarPagamento() {
        FreteFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alert("Selecione um frete para estornar.");
            return;
        }

        if (selecionada.getPago() <= 0.01) {
            alert("Este frete não tem pagamento para estornar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/EstornoPagamento.fxml"));
            Parent root = loader.load();

            EstornoPagamentoController controller = loader.getController();
            controller.setDados(selecionada.getTotal(), selecionada.getPago());

            Stage stage = new Stage();
            stage.setTitle("Estornar Pagamento - Área Restrita");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isConfirmado()) {
                java.math.BigDecimal vEstorno = controller.getValorEstorno();
                String motivo = controller.getMotivo();
                int idAutorizador = controller.getIdAutorizador();
                String nomeAutorizador = controller.getNomeAutorizador();

                java.math.BigDecimal bdPago = java.math.BigDecimal.valueOf(selecionada.getPago());
                java.math.BigDecimal bdTotal = java.math.BigDecimal.valueOf(selecionada.getTotal());
                java.math.BigDecimal novoPago = bdPago.subtract(vEstorno);
                java.math.BigDecimal novoDevedor = bdTotal.subtract(novoPago);
                String novoStatus = (novoPago.compareTo(model.StatusPagamento.TOLERANCIA_PAGAMENTO) > 0) ? "PENDENTE" : "NAO_PAGO";

                Connection con = null;
                try {
                    con = ConexaoBD.getConnection();
                    con.setAutoCommit(false);

                    // Criar tabela de log se não existir
                    String sqlCriarTabela = "CREATE TABLE IF NOT EXISTS log_estornos_fretes (" +
                            "id_log SERIAL PRIMARY KEY, " +
                            "id_frete INTEGER NOT NULL, " +
                            "valor_estornado DECIMAL(10,2) NOT NULL, " +
                            "motivo TEXT, " +
                            "forma_devolucao VARCHAR(50), " +
                            "id_usuario_autorizou INTEGER, " +
                            "nome_autorizador VARCHAR(100), " +
                            "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                    try (PreparedStatement stmtCriar = con.prepareStatement(sqlCriarTabela)) {
                        stmtCriar.executeUpdate();
                    }

                    String sqlUp = "UPDATE fretes SET valor_pago = ?, valor_devedor = ?, status_frete = ? WHERE id_frete = ?";
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
                        stmt.executeUpdate();
                    }

                    con.commit();
                    alert("Estorno realizado com sucesso!\nAutorizado por: " + nomeAutorizador);
                    carregarDados();

                } catch (Exception ex) {
                    if (con != null) con.rollback();
                    ex.printStackTrace();
                    alert("Erro ao gravar estorno: " + ex.getMessage());
                } finally {
                    if (con != null) con.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro ao abrir tela de estorno: " + e.getMessage());
        }
    }

    @FXML
    public void abrirNotaFrete() {
        FreteFinanceiro selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            alert("Selecione um frete para visualizar a nota.");
            return;
        }

        try {
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
                    }
                }
            }
            
            // Buscar itens do frete
            String sqlItens = "SELECT fi.quantidade, fi.descricao, fi.valor_unitario " +
                              "FROM frete_itens fi " +
                              "WHERE fi.id_frete = ?";
            try (Connection con = ConexaoBD.getConnection();
                 PreparedStatement stmt = con.prepareStatement(sqlItens)) {
                stmt.setLong(1, selecionada.getId());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    itens.add(new NotaFretePersonalizadaController.ItemNota(
                        rs.getInt("quantidade"),
                        rs.getString("descricao"),
                        rs.getDouble("valor_unitario")
                    ));
                }
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NotaFretePersonalizada.fxml"));
            Parent root = loader.load();

            NotaFretePersonalizadaController controller = loader.getController();
            controller.setDadosNotaFrete(remetente, destinatario, conferente, rota, dataHora, pagamento, total, itens);

            Stage stage = new Stage();
            stage.setTitle("Nota do Frete - " + selecionada.getNumero());
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            alert("Erro ao abrir nota do frete: " + e.getMessage());
        }
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    public static class OpcaoViagem {
        int id;
        String label;

        public OpcaoViagem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class FreteFinanceiro {
        private long id;
        private String numero, dataViagem, remetente, destinatario;
        private int volumes;
        private Double total, pago;

        public FreteFinanceiro(long id, String num, String data, String rem, String dest, int volumes, Double total, Double pago) {
            this.id = id;
            this.numero = num;
            this.dataViagem = data;
            this.remetente = rem;
            this.destinatario = dest;
            this.volumes = volumes;
            this.total = total;
            this.pago = (pago != null) ? pago : 0.0;
        }

        public long getId() { return id; }
        public String getNumero() { return numero; }
        public String getDataViagem() { return dataViagem; }
        public String getRemetente() { return remetente; }
        public String getDestinatario() { return destinatario; }
        public int getVolumes() { return volumes; }
        public Double getTotal() { return total; }
        public Double getPago() { return pago; }
        public Double getRestante() { return Math.max(0, total - pago); }
        public String getTotalFormatado() { return String.format("R$ %.2f", total); }
        public String getPagoFormatado() { return String.format("R$ %.2f", pago); }
        public String getRestanteFormatado() { return String.format("R$ %.2f", getRestante()); }

        public String getStatus() {
            return model.StatusPagamento.calcularPorSaldo(getRestante(), getPago()).name();
        }
    }
}
