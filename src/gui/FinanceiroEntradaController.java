package gui;

import dao.ConexaoBD;
import dao.ViagemDAO;
import gui.util.PermissaoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import model.OpcaoViagem;
import gui.util.AppLogger;

public class FinanceiroEntradaController {

    @FXML private Label lblTituloViagem;
    @FXML private Label lblDataAtual;
    @FXML private Label lblTotalGeral;
    @FXML private Label lblRecebido;
    @FXML private Label lblPendente;
    @FXML private Button btnSair;

    @FXML private ComboBox<OpcaoViagem> cmbFiltroViagem;
    @FXML private ComboBox<String> cmbFiltroCategoria;
    @FXML private ComboBox<String> cmbFiltroPagamento;
    @FXML private ComboBox<String> cmbFiltroCaixa;

    @FXML private PieChart graficoPizza;
    @FXML private BarChart<String, Number> graficoBarra;

    private int idViagemSelecionada = 0; 

    @FXML
    public void initialize() {
        if (!PermissaoService.isFinanceiro()) { PermissaoService.exigirFinanceiro("Lancamento de Entradas"); return; }
        lblDataAtual.setText("Data de Hoje: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        carregarCombosEstaticos();

        cmbFiltroViagem.valueProperty().addListener((obs, oldVal, newVal) -> atualizarDashboard());
        cmbFiltroCategoria.valueProperty().addListener((obs, oldVal, newVal) -> atualizarDashboard());
        cmbFiltroPagamento.valueProperty().addListener((obs, oldVal, newVal) -> atualizarDashboard());
        cmbFiltroCaixa.valueProperty().addListener((obs, oldVal, newVal) -> atualizarDashboard());

        // DR010: carrega dados pesados em background
        Thread bg = new Thread(() -> {
            carregarUsuariosNoCombo();
            carregarComboViagens();
            javafx.application.Platform.runLater(this::atualizarDashboard);
        });
        bg.setDaemon(true);
        bg.start();
    }

    @FXML
    public void sair() {
        TelaPrincipalController.fecharTelaAtual(lblTituloViagem);
    }

    private void carregarCombosEstaticos() {
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList("Todas", "PASSAGEM", "ENCOMENDA", "FRETE"));
        cmbFiltroCategoria.getSelectionModel().selectFirst();

        cmbFiltroPagamento.setItems(FXCollections.observableArrayList("Todas", "DINHEIRO", "PIX", "CARTAO", "BOLETO"));
        cmbFiltroPagamento.getSelectionModel().selectFirst();
    }
    
    private void carregarUsuariosNoCombo() {
        ObservableList<String> usuarios = FXCollections.observableArrayList("Todos");
        
        String sql = "SELECT nome FROM usuarios WHERE empresa_id = ? ORDER BY nome";

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
             stmt.setInt(1, dao.DAOUtils.empresaId());
             ResultSet rs = stmt.executeQuery();
             
             while(rs.next()){
                 usuarios.add(rs.getString("nome"));
             }
             
        } catch (SQLException e) {
            System.out.println("Erro ao carregar usuários: " + e.getMessage());
        }
        
        javafx.application.Platform.runLater(() -> {
            cmbFiltroCaixa.setItems(usuarios);
            cmbFiltroCaixa.getSelectionModel().selectFirst();
        });
    }

    private void carregarComboViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        lista.add(new OpcaoViagem(0, "Todas as Viagens"));

        OpcaoViagem viagemAtiva = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (model.Viagem v : new ViagemDAO().listarViagensRecentes(20)) {
            String desc = v.getDescricao() != null ? v.getDescricao() : "";
            if (v.getDataViagem() != null) {
                desc += " (" + sdf.format(java.sql.Date.valueOf(v.getDataViagem()));
                if (v.getDataChegada() != null) desc += " - " + sdf.format(java.sql.Date.valueOf(v.getDataChegada()));
                desc += ")";
            }
            OpcaoViagem op = new OpcaoViagem(v.getId().intValue(), desc);
            lista.add(op);
            if (v.getIsAtual()) viagemAtiva = op;
        }

        OpcaoViagem finalViagemAtiva = viagemAtiva;
        ObservableList<OpcaoViagem> finalLista = lista;
        javafx.application.Platform.runLater(() -> {
            cmbFiltroViagem.setItems(finalLista);
            if (finalViagemAtiva != null) {
                cmbFiltroViagem.setValue(finalViagemAtiva);
            } else if (finalLista.size() > 1) {
                cmbFiltroViagem.getSelectionModel().select(1);
            } else {
                cmbFiltroViagem.getSelectionModel().selectFirst();
            }
        });
    }

    @FXML
    public void atualizarDashboard() {
        OpcaoViagem viagemSel = cmbFiltroViagem.getValue();
        if(viagemSel != null) {
            lblTituloViagem.setText(viagemSel.id == 0 ? "Visão Geral (Todas)" : "Resumo: " + viagemSel.label);
            this.idViagemSelecionada = viagemSel.id;
        }

        String catSel = cmbFiltroCategoria.getValue();
        String pagSel = cmbFiltroPagamento.getValue();
        String usrSel = cmbFiltroCaixa.getValue();

        double total = 0, recebido = 0, pendente = 0;
        double somaPassagem = 0, somaEncomenda = 0, somaFrete = 0;
        double somaPix = 0, somaDinheiro = 0, somaCartao = 0;

        StringBuilder sql = new StringBuilder();
        java.util.List<Object> params = new java.util.ArrayList<>();

        // 1. ENCOMENDAS — #012: parametrizado
        sql.append("SELECT 'ENCOMENDA' as origem, total_a_pagar as total, valor_pago as pago, COALESCE(tipo_pagamento, 'PENDENTE') as pgto, caixa as usuario ");
        sql.append("FROM encomendas WHERE empresa_id = ? ");
        params.add(dao.DAOUtils.empresaId());
        if (idViagemSelecionada > 0) { sql.append(" AND id_viagem = ?"); params.add(idViagemSelecionada); }

        sql.append(" UNION ALL ");

        // 2. FRETES
        sql.append("SELECT 'FRETE' as origem, valor_frete_calculado as total, valor_pago as pago, COALESCE(tipo_pagamento, 'PENDENTE') as pgto, nome_caixa as usuario ");
        sql.append("FROM fretes WHERE empresa_id = ? ");
        params.add(dao.DAOUtils.empresaId());
        if (idViagemSelecionada > 0) { sql.append(" AND id_viagem = ?"); params.add(idViagemSelecionada); }

        sql.append(" UNION ALL ");

        // 3. PASSAGENS
        sql.append(" SELECT 'PASSAGEM' as origem, p.valor_total as total, p.valor_pago as pago, ");
        sql.append(" COALESCE(afp.nome_forma_pagamento, 'DINHEIRO') as pgto, 'SISTEMA' as usuario ");
        sql.append(" FROM passagens p ");
        sql.append(" LEFT JOIN aux_formas_pagamento afp ON p.id_forma_pagamento = afp.id_forma_pagamento ");
        sql.append(" WHERE p.empresa_id = ? ");
        params.add(dao.DAOUtils.empresaId());
        if (idViagemSelecionada > 0) { sql.append(" AND p.id_viagem = ?"); params.add(idViagemSelecionada); }

        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String tipo = rs.getString("origem"); 
                double vTot = rs.getDouble("total");
                double vPag = rs.getDouble("pago");
                String pgto = rs.getString("pgto");   
                String user = rs.getString("usuario");

                if (catSel != null && !catSel.equals("Todas")) {
                    if (!tipo.equalsIgnoreCase(catSel)) continue;
                }
                if (pagSel != null && !pagSel.equals("Todas")) {
                    if (pgto == null || !pgto.toUpperCase().contains(pagSel.toUpperCase())) continue;
                }
                if (usrSel != null && !usrSel.equals("Todos")) {
                    if (user == null || !user.equalsIgnoreCase(usrSel)) continue;
                }

                total += vTot;
                recebido += vPag;
                pendente += (vTot - vPag);

                if (tipo.equals("PASSAGEM")) somaPassagem += vTot;
                else if (tipo.equals("ENCOMENDA")) somaEncomenda += vTot;
                else if (tipo.equals("FRETE")) somaFrete += vTot;

                if (vPag > 0 && pgto != null) {
                    String pgtoUpper = pgto.toUpperCase();
                    if (pgtoUpper.contains("PIX")) somaPix += vPag;
                    else if (pgtoUpper.contains("CART") || pgtoUpper.contains("CREDITO") || pgtoUpper.contains("DEBITO")) somaCartao += vPag;
                    else somaDinheiro += vPag;
                }
            }

            lblTotalGeral.setText(String.format("R$ %.2f", total));
            lblRecebido.setText(String.format("R$ %.2f", recebido));
            lblPendente.setText(String.format("R$ %.2f", pendente));

            ObservableList<PieChart.Data> dadosPizza = FXCollections.observableArrayList();
            if (somaPassagem > 0) dadosPizza.add(new PieChart.Data("Passagens", somaPassagem));
            if (somaEncomenda > 0) dadosPizza.add(new PieChart.Data("Encomendas", somaEncomenda));
            if (somaFrete > 0) dadosPizza.add(new PieChart.Data("Fretes", somaFrete));
            graficoPizza.setData(dadosPizza);

            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName("Recebido");
            if (somaDinheiro > 0) serie.getData().add(new XYChart.Data<>("Dinheiro", somaDinheiro));
            if (somaPix > 0) serie.getData().add(new XYChart.Data<>("Pix", somaPix));
            if (somaCartao > 0) serie.getData().add(new XYChart.Data<>("Cartão", somaCartao));
            
            graficoBarra.getData().clear();
            graficoBarra.getData().add(serie);

            }
        } catch (SQLException e) {
            AppLogger.error("FinanceiroEntradaController", e.getMessage(), e);
            AppLogger.warn("FinanceiroEntradaController", "Erro SQL Dashboard: " + e.getMessage());
        }
    }

    @FXML void abrirTelaEncomendas() { abrirTelaDetalhes("ENCOMENDA"); }
    @FXML void abrirTelaPassagens() { abrirTelaDetalhes("PASSAGEM"); }
    @FXML void abrirTelaFretes() { abrirTelaDetalhes("FRETE"); }
    @FXML void abrirTelaGeral() { abrirTelaDetalhes("GERAL"); }

    private void abrirTelaDetalhes(String tipo) {
        if (idViagemSelecionada == 0 && !tipo.equals("GERAL")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selecione uma Viagem");
            alert.setHeaderText("Modo 'Todas as Viagens'");
            alert.setContentText("Para ver detalhes, selecione uma viagem específica.");
            alert.showAndWait();
            return;
        }
        
        try {
            String fxmlFile = "";
            String titulo = "";
            
            if (tipo.equals("ENCOMENDA")) {
                fxmlFile = "/gui/FinanceiroEncomendas.fxml";
                titulo = "Financeiro - Encomendas";
            } else if (tipo.equals("PASSAGEM")) {
                fxmlFile = "/gui/FinanceiroPassagens.fxml";
                titulo = "Financeiro - Passagens";
            } else if (tipo.equals("FRETE")) {
                fxmlFile = "/gui/FinanceiroFretes.fxml";
                titulo = "Financeiro - Fretes";
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Em construção");
                alert.setHeaderText("Abrindo: " + tipo);
                alert.setContentText("Esta tela será implementada na próxima etapa.");
                alert.showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            if (tipo.equals("ENCOMENDA")) {
                FinanceiroEncomendasController controller = loader.getController();
                controller.setViagemInicial(idViagemSelecionada);
                controller.carregarDados(); 
            } else if (tipo.equals("PASSAGEM")) {
                FinanceiroPassagensController controller = loader.getController();
                controller.setViagemInicial(idViagemSelecionada);
                controller.carregarDados();
            } else if (tipo.equals("FRETE")) {
                FinanceiroFretesController controller = loader.getController();
                controller.setViagemInicial(idViagemSelecionada);
                controller.carregarDados();
            }
            
            Stage stage = new Stage();
            stage.setTitle(titulo);
            Scene scene = new Scene(root);
            TemaManager.aplicarTema(scene); // Aplicar tema correto
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL); 
            stage.setMaximized(true);
            stage.showAndWait();
            
            atualizarDashboard(); 

        } catch (Exception e) {
            AppLogger.error("FinanceiroEntradaController", e.getMessage(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erro ao abrir tela: " + e.getMessage());
            alert.showAndWait();
        }
    }

}