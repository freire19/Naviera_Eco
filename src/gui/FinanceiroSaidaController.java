package gui;

import dao.ConexaoBD;
import dao.DespesaDAO;
import dao.ViagemDAO;
import gui.util.AlertHelper;
import gui.util.PermissaoService;
import gui.util.SessaoUsuario; 
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import model.OpcaoViagem;

public class FinanceiroSaidaController {

    @FXML private TextField txtDescricao;
    @FXML private TextField txtValor;
    @FXML private ComboBox<String> cmbCategoria; 
    @FXML private DatePicker dpDataGasto;
    @FXML private DatePicker dpDataPrimeiroPagamento; 
    @FXML private ComboBox<String> cmbFormaPagamento;
    
    @FXML private Label lblTotalGasto;
    @FXML private ComboBox<String> cmbFiltroCategoria;
    @FXML private ComboBox<String> cmbFiltroPagamento; 
    @FXML private ComboBox<OpcaoViagem> cmbFiltroViagem; 
    @FXML private DatePicker dpFiltroData;
    @FXML private Button btnSair;

    @FXML private TableView<Despesa> tabela;
    @FXML private TableColumn<Despesa, String> colData;
    @FXML private TableColumn<Despesa, String> colDescricao;
    @FXML private TableColumn<Despesa, String> colCategoria;
    @FXML private TableColumn<Despesa, String> colForma;
    @FXML private TableColumn<Despesa, String> colValor;
    @FXML private TableColumn<Despesa, String> colStatus;

    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final DespesaDAO despesaDAO = new DespesaDAO();
    private final ViagemDAO viagemDAO = new ViagemDAO();

    private ObservableList<String> listaCategoriasOriginal = FXCollections.observableArrayList();
    private boolean ignoreFilter = false;

    @FXML
    public void initialize() {
        if (!PermissaoService.isFinanceiro()) { PermissaoService.exigirFinanceiro("Lancamento de Despesas"); return; }
        // 1. Configurações visuais iniciais
        carregarCategorias();
        configurarTabela();
        
        dpDataGasto.setValue(LocalDate.now());
        if (dpDataPrimeiroPagamento != null) {
            dpDataPrimeiroPagamento.setValue(LocalDate.now());
        }
        
        cmbFormaPagamento.setItems(FXCollections.observableArrayList("DINHEIRO", "PIX", "CARTAO", "TRANSFERENCIA", "BOLETO"));
        cmbFormaPagamento.getSelectionModel().selectFirst();
        
        ObservableList<String> formasFiltro = FXCollections.observableArrayList("Todas");
        formasFiltro.addAll(cmbFormaPagamento.getItems());
        cmbFiltroPagamento.setItems(formasFiltro);
        cmbFiltroPagamento.getSelectionModel().selectFirst();
        
        dpFiltroData.setValue(null);
        cmbFiltroCategoria.getSelectionModel().selectFirst();
        
        // DR010: carrega viagens em background e configura listeners na FX thread
        Thread bg = new Thread(() -> {
            carregarViagens();
            javafx.application.Platform.runLater(() -> {
                cmbFiltroViagem.valueProperty().addListener((obs, oldVal, newVal) -> filtrar());
                cmbFiltroCategoria.valueProperty().addListener((obs, oldVal, newVal) -> filtrar());
                cmbFiltroPagamento.valueProperty().addListener((obs, oldVal, newVal) -> filtrar());
                dpFiltroData.valueProperty().addListener((obs, oldVal, newVal) -> filtrar());
                filtrar();
            });
        });
        bg.setDaemon(true);
        bg.start();
    }
    
    // --- LÓGICA CORRIGIDA DE CARREGAMENTO DE VIAGENS ---
    private void carregarViagens() {
        ObservableList<OpcaoViagem> lista = FXCollections.observableArrayList();
        OpcaoViagem opcaoTodas = new OpcaoViagem(0, "TODAS AS VIAGENS");
        lista.add(opcaoTodas);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        OpcaoViagem opcaoAtivaEncontrada = null;

        try {
            java.util.List<model.Viagem> viagens = viagemDAO.listarViagensRecentes(50);
            for (model.Viagem v : viagens) {
                int id = Long.valueOf(v.getId()).intValue();
                String desc = v.getDescricao() != null ? v.getDescricao() : "";
                if (v.getDataViagem() != null) {
                    desc += " (" + sdf.format(java.sql.Date.valueOf(v.getDataViagem())) + ")";
                }
                boolean isAtual = v.getIsAtual();
                if (isAtual) {
                    desc += " (ATUAL)";
                }
                OpcaoViagem op = new OpcaoViagem(id, desc);
                lista.add(op);
                if (isAtual) {
                    opcaoAtivaEncontrada = op;
                }
            }
            
            // DR010: atualiza UI na FX thread
            OpcaoViagem finalOpcaoAtiva = opcaoAtivaEncontrada;
            ObservableList<OpcaoViagem> finalLista = lista;
            javafx.application.Platform.runLater(() -> {
                cmbFiltroViagem.setItems(finalLista);
                if (finalOpcaoAtiva != null) {
                    cmbFiltroViagem.getSelectionModel().select(finalOpcaoAtiva);
                } else if (finalLista.size() > 1) {
                    cmbFiltroViagem.getSelectionModel().select(1);
                } else {
                    cmbFiltroViagem.getSelectionModel().selectFirst();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> AlertHelper.info("Erro interno. Contate o administrador."));
            System.err.println("Erro ao carregar viagens: " + e.getMessage());
        }
    }

    @FXML
    public void filtrar() {
        // Se o combo estiver nulo (ainda carregando), não faz nada
        if(cmbFiltroViagem.getValue() == null) return;

        int idFiltro = cmbFiltroViagem.getValue().id;
        ObservableList<Despesa> lista = FXCollections.observableArrayList();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        String categoriaFiltro = cmbFiltroCategoria.getValue();
        String formaFiltro = cmbFiltroPagamento.getValue();
        java.time.LocalDate dataFiltro = dpFiltroData.getValue();

        java.util.List<java.util.Map<String, Object>> rows = despesaDAO.buscarDespesas(
                idFiltro, categoriaFiltro, formaFiltro, dataFiltro, true);

        for (java.util.Map<String, Object> row : rows) {
            java.sql.Date dtVenc = (java.sql.Date) row.get("data_vencimento");
            Despesa d = new Despesa(
                (int) row.get("id"),
                dtVenc != null ? sdf.format(dtVenc) : "",
                (String) row.get("descricao"),
                (String) row.get("cat_nome"),
                (String) row.get("forma_pagamento"),
                (java.math.BigDecimal) row.get("valor_total"),
                (String) row.get("status"),
                (boolean) row.get("is_excluido")
            );
            lista.add(d);
            if (!d.isExcluido() && d.getValor() != null) {
                total = total.add(d.getValor());
            }
        }
        tabela.setItems(lista);
        lblTotalGasto.setText(nf.format(total));
    }
    
    @FXML
    public void sair() {
        Node nodeRef = (btnSair != null) ? btnSair : txtDescricao;
        TelaPrincipalController.fecharTelaAtual(nodeRef);
    }

    @FXML
    public void abrirGestaoFuncionarios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GestaoFuncionarios.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Gestão Completa de Funcionários e Pagamentos");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL); 
            stage.setMaximized(true);
            stage.showAndWait();
            
            filtrar(); 
        } catch (Exception e) {
            System.err.println("FinanceiroSaidaController.abrirGestaoFuncionarios: erro ao abrir tela — " + e.getMessage());
            e.printStackTrace();
            AlertHelper.info("Erro interno. Contate o administrador.");
        }
    }

    private void configurarAutoComplete(ComboBox<String> cmb) {
        cmb.setEditable(true);
        cmb.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ignoreFilter = true; 
                cmb.getEditor().setText(newVal);
                Platform.runLater(() -> ignoreFilter = false); 
            }
        });
        cmb.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (ignoreFilter) return; 
            Platform.runLater(() -> {
                if (newText == null || newText.isEmpty()) {
                    cmb.setItems(listaCategoriasOriginal);
                    return;
                }
                ObservableList<String> sublista = FXCollections.observableArrayList();
                for (String item : listaCategoriasOriginal) {
                    if (item.toUpperCase().contains(newText.toUpperCase())) {
                        sublista.add(item);
                    }
                }
                cmb.setItems(sublista);
                if (!cmb.isShowing() && !sublista.isEmpty()) cmb.show();
                if (sublista.isEmpty()) cmb.hide();
            });
        });
    }

    @FXML
    public void salvarLancamento() {
        if (txtDescricao.getText().isEmpty() || txtValor.getText().isEmpty() || cmbCategoria.getValue() == null) {
            AlertHelper.info("Preencha os campos obrigatórios."); return;
        }
        
        if (dpDataPrimeiroPagamento.getValue() == null) {
            AlertHelper.info("Por favor, informe a Data do 1º Pagamento.");
            return;
        }
        
        int idParaSalvar = 0;
        
        if (cmbFiltroViagem.getValue() != null && cmbFiltroViagem.getValue().id != 0) {
            idParaSalvar = cmbFiltroViagem.getValue().id;
        } 

        if (idParaSalvar == 0) {
            AlertHelper.info("ERRO CRÍTICO: Não foi possível identificar a viagem selecionada.\nVerifique o filtro de viagens no topo da tela.");
            return;
        }

        try {
            String valorTexto = txtValor.getText().replace(",", ".");
            java.math.BigDecimal valor = new java.math.BigDecimal(valorTexto);
            if (valor.signum() <= 0) { AlertHelper.info("O valor deve ser maior que zero."); return; }

            String forma = cmbFormaPagamento.getValue();
            String status = forma.equals("BOLETO") ? "PENDENTE" : "PAGO";
            int idCat = buscarIdCategoria(cmbCategoria.getValue());

            String sql = "INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, data_pagamento, status, forma_pagamento, id_categoria, id_viagem) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection con = ConexaoBD.getConnection();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                
                stmt.setString(1, txtDescricao.getText().toUpperCase());
                stmt.setBigDecimal(2, valor);
                stmt.setBigDecimal(3, status.equals("PAGO") ? valor : java.math.BigDecimal.ZERO);
                
                java.sql.Date dataFinanceira = java.sql.Date.valueOf(dpDataPrimeiroPagamento.getValue());
                
                stmt.setDate(4, dataFinanceira); // data_vencimento
                stmt.setDate(5, status.equals("PAGO") ? dataFinanceira : null); // data_pagamento
                
                stmt.setString(6, status);
                stmt.setString(7, forma);
                stmt.setInt(8, idCat);
                stmt.setInt(9, idParaSalvar);
                
                stmt.executeUpdate();
                AlertHelper.info("Despesa salva com sucesso!");
                
                txtDescricao.clear();
                txtValor.clear();
                cmbCategoria.setValue(null);
                cmbCategoria.getEditor().clear(); 
                cmbCategoria.setItems(listaCategoriasOriginal);
                
                dpDataGasto.setValue(LocalDate.now());
                dpDataPrimeiroPagamento.setValue(LocalDate.now());
                
                filtrar();
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluir() {
        Despesa sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        if (sel.isExcluido()) {
            AlertHelper.info("Este item já está excluído.");
            return;
        }

        String nomeOperador = "DESCONHECIDO";
        if (SessaoUsuario.isUsuarioLogado()) {
            nomeOperador = SessaoUsuario.getUsuarioLogado().getNomeCompleto(); 
        }
        final String solicitanteFinal = nomeOperador;

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Exclusão Administrativa");
        dialog.setHeaderText("Solicitante Identificado: " + solicitanteFinal);

        ButtonType loginButtonType = new ButtonType("Confirmar Exclusão", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label lblOperador = new Label(solicitanteFinal);
        lblOperador.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565c0;");

        PasswordField password = new PasswordField();
        password.setPromptText("Senha do Gerente/Admin");
        
        TextArea motivo = new TextArea();
        motivo.setPromptText("Motivo detalhado...");
        motivo.setPrefHeight(60);

        grid.add(new Label("Solicitante:"), 0, 0);
        grid.add(lblOperador, 1, 0);
        grid.add(new Label("Senha Admin:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Motivo:"), 0, 2);
        grid.add(motivo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> password.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(password.getText(), motivo.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(dados -> {
            String pass = dados.getKey();
            String reason = dados.getValue();
            
            if (reason.isEmpty()) {
                AlertHelper.info("É obrigatório informar o motivo da exclusão.");
                return;
            }

            String nomeAdmin = validarPermissaoGerente(pass);
            if (nomeAdmin == null) {
                AlertHelper.info("Senha incorreta ou usuário sem permissão de Gerente/Administrador.");
                return;
            }

            try (Connection con = ConexaoBD.getConnection()) {
                con.setAutoCommit(false); 
                
                int idViagemDaDespesa = buscarIdViagemDaDespesa(sel.getId(), con);
                String infoViagem = buscarInfoViagem(sel.getId(), con);
                String responsaveis = solicitanteFinal.toUpperCase() + " / " + nomeAdmin.toUpperCase();

                String sqlUpdate = "UPDATE financeiro_saidas SET is_excluido = true, motivo_exclusao = ? WHERE id = ?";
                try (PreparedStatement stmt = con.prepareStatement(sqlUpdate)) {
                    stmt.setString(1, reason.toUpperCase());
                    stmt.setInt(2, sel.getId());
                    stmt.executeUpdate();
                }

                String sqlAudit = "INSERT INTO auditoria_financeiro (acao, usuario, motivo, detalhe_valor, id_viagem) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(sqlAudit)) {
                    stmt.setString(1, "EXCLUSAO_DESPESA");
                    stmt.setString(2, responsaveis); 
                    stmt.setString(3, reason.toUpperCase());
                    stmt.setString(4, sel.getDescricao() + " | " + sel.getValorFormatado() + " | " + infoViagem);
                    stmt.setInt(5, idViagemDaDespesa);
                    stmt.executeUpdate();
                }
                
                con.commit();
                AlertHelper.info("Registro marcado como excluído com sucesso!");
                filtrar();

            } catch (Exception e) {
                e.printStackTrace();
                AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro ao excluir: " + e.getMessage());
            }
        });
    }
    
    private String validarPermissaoGerente(String senha) {
        String sql = "SELECT login_usuario, senha_hash FROM usuarios WHERE (funcao = 'Gerente' OR funcao = 'Administrador') AND ativo = true";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String hashDoBanco = rs.getString("senha_hash");
                String login = rs.getString("login_usuario");
                try {
                    if (hashDoBanco != null) {
                        // Sempre usa BCrypt — sem fallback plaintext
                        if (org.mindrot.jbcrypt.BCrypt.checkpw(senha, hashDoBanco)) {
                            return login;
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    // Hash nao e BCrypt valido — ignora este usuario
                    System.err.println("Hash invalido para usuario " + login + ": formato nao-BCrypt");
                }
            }
        } catch (Exception e) { System.err.println("FinanceiroSaidaController.validarPermissaoGerente: erro ao consultar usuarios — " + e.getMessage()); e.printStackTrace(); }
        return null;
    }

    private String buscarInfoViagem(int idDespesa, Connection con) {
        String info = "VIAGEM N/D";
        String sql = "SELECT v.data_viagem, v.data_chegada FROM financeiro_saidas s JOIN viagens v ON s.id_viagem = v.id_viagem WHERE s.id = ?";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idDespesa);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.sql.Date dtIda = rs.getDate("data_viagem");
                java.sql.Date dtVolta = rs.getDate("data_chegada");
                String sIda = (dtIda != null) ? sdf.format(dtIda) : "?";
                String sVolta = (dtVolta != null) ? sdf.format(dtVolta) : "?";
                info = "REF. VIAGEM: " + sIda + " A " + sVolta;
            }
        } catch (Exception e) { System.err.println("Erro em FinanceiroSaidaController.buscarInfoViagem: " + e.getMessage()); }
        return info;
    }
    
    private int buscarIdViagemDaDespesa(int idDespesa, Connection con) {
        String sql = "SELECT id_viagem FROM financeiro_saidas WHERE id = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idDespesa);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id_viagem");
        } catch (Exception e) { System.err.println("Erro em FinanceiroSaidaController.buscarIdViagemDaDespesa: " + e.getMessage()); }
        return 0;
    }

    @FXML
    public void imprimirRelatorio() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(tabela.getScene().getWindow())) {
            
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(javafx.print.Paper.A4, javafx.print.PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            double alturaUtilPagina = pageLayout.getPrintableHeight() - 80; 
            double larguraPagina = pageLayout.getPrintableWidth();

            List<VBox> paginas = new ArrayList<>();
            List<Label> labelsNumeracao = new ArrayList<>(); 

            VBox paginaAtual = new VBox(5);
            paginaAtual.setPrefWidth(larguraPagina);
            paginaAtual.setStyle("-fx-background-color: white; -fx-padding: 0;");
            paginaAtual.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            
            double alturaAtual = 0;

            VBox cabecalho = criarCabecalhoEmpresa();
            paginaAtual.getChildren().add(cabecalho);
            alturaAtual += 140; 

            VBox filtros = criarCabecalhoFiltros();
            paginaAtual.getChildren().add(filtros);
            alturaAtual += 50;

            GridPane gridTabela = new GridPane();
            configurarGridTabela(gridTabela, larguraPagina);
            adicionarCabecalhoColunas(gridTabela);
            paginaAtual.getChildren().add(gridTabela);
            alturaAtual += 30; 

            ObservableList<Despesa> itens = tabela.getItems();
            Map<String, java.math.BigDecimal> totaisPorTipo = new HashMap<>();
            java.math.BigDecimal totalGeral = java.math.BigDecimal.ZERO;
            int rowGrid = 1; 

            for (Despesa d : itens) {
                double alturaLinha = 30; 
                if (d.getDescricao().length() > 55) alturaLinha = 50; 

                if (alturaAtual + alturaLinha > alturaUtilPagina) {
                    paginas.add(paginaAtual); 
                    paginaAtual = new VBox(5);
                    paginaAtual.setPrefWidth(larguraPagina);
                    paginaAtual.setStyle("-fx-background-color: white; -fx-padding: 20 0 0 0;");
                    paginaAtual.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                    Label lblNumPag = new Label("Página ?/?");
                    lblNumPag.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                    labelsNumeracao.add(lblNumPag);
                    paginaAtual.getChildren().add(lblNumPag);
                    gridTabela = new GridPane();
                    configurarGridTabela(gridTabela, larguraPagina);
                    adicionarCabecalhoColunas(gridTabela);
                    paginaAtual.getChildren().add(gridTabela);
                    alturaAtual += 70; 
                    rowGrid = 1; 
                }

                String corFundo = (rowGrid % 2 == 0) ? "#f2f2f2" : "#ffffff";
                adicionarLinhaGrid(gridTabela, d, rowGrid, corFundo);
                
                if (!d.isExcluido()) {
                    totaisPorTipo.merge(d.getForma(), d.getValor(), java.math.BigDecimal::add);
                    totalGeral = totalGeral.add(d.getValor());
                }
                
                alturaAtual += alturaLinha;
                rowGrid++;
            }

            if (alturaAtual + 150 > alturaUtilPagina) {
                 paginas.add(paginaAtual);
                 paginaAtual = new VBox(5);
                 paginaAtual.setPrefWidth(larguraPagina);
                 paginaAtual.setStyle("-fx-background-color: white; -fx-padding: 20 0 0 0;");
                 paginaAtual.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                 Label lblNumPag = new Label("Página ?/?");
                 labelsNumeracao.add(lblNumPag);
                 paginaAtual.getChildren().add(lblNumPag);
            }
            
            paginaAtual.getChildren().add(criarRodape(totaisPorTipo, totalGeral));
            paginas.add(paginaAtual);

            int totalPaginas = paginas.size();
            for (int i = 0; i < labelsNumeracao.size(); i++) {
                labelsNumeracao.get(i).setText("Página " + (i + 2) + "/" + totalPaginas);
            }
            for (VBox p : paginas) { job.printPage(pageLayout, p); }
            job.endJob();
        }
    }
    
    private VBox criarRodape(Map<String, java.math.BigDecimal> totais, java.math.BigDecimal geral) {
        VBox box = new VBox(5);
        box.setStyle("-fx-padding: 20 0 0 0; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        int col = 0;
        int row = 0;

        for (Map.Entry<String, java.math.BigDecimal> entry : totais.entrySet()) {
            Label lbl = new Label(entry.getKey() + ": " + nf.format(entry.getValue()));
            lbl.setStyle("-fx-font-size: 10px;");
            grid.add(lbl, col, row);
            col++;
            if (col > 3) { col = 0; row++; }
        }

        Label lblTotal = new Label("TOTAL GERAL: " + nf.format(geral));
        lblTotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");
        
        box.getChildren().addAll(grid, lblTotal);
        return box;
    }

    private VBox criarCabecalhoEmpresa() {
        VBox cabecalho = new VBox(5);
        cabecalho.setAlignment(javafx.geometry.Pos.CENTER);
        DadosEmpresa dados = buscarDadosEmpresa();
        if (dados.pathLogo != null && !dados.pathLogo.isEmpty()) {
            try {
                File file = new File(dados.pathLogo);
                if (file.exists()) {
                    ImageView imgLogo = new ImageView(new Image(file.toURI().toString()));
                    imgLogo.setFitHeight(60); 
                    imgLogo.setPreserveRatio(true);
                    cabecalho.getChildren().add(imgLogo);
                }
            } catch (Exception e) { System.err.println("Erro em FinanceiroSaidaController.criarCabecalhoEmpresa (logo): " + e.getMessage()); }
        }
        String nomeEmpresa = (dados.nome != null && !dados.nome.isEmpty()) ? dados.nome : "F/B DEUS DE ALIANÇA V";
        Label lblEmpresa = new Label(nomeEmpresa.toUpperCase());
        lblEmpresa.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1565c0;"); 
        Label lblTitulo = new Label("RELATÓRIO FINANCEIRO - SAÍDAS");
        lblTitulo.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");
        cabecalho.getChildren().addAll(lblEmpresa, lblTitulo);
        return cabecalho;
    }

    private VBox criarCabecalhoFiltros() {
        VBox box = new VBox(2);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 10 0 10 0;");
        String nomeViagem = cmbFiltroViagem.getValue() != null ? cmbFiltroViagem.getValue().label : "Todas";
        Label lblInfo = new Label("VIAGEM: " + nomeViagem);
        Label lblData = new Label("EMISSÃO: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " | FILTROS: " + (cmbFiltroCategoria.getValue() != null ? cmbFiltroCategoria.getValue() : "Geral"));
        lblInfo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        lblData.setStyle("-fx-font-size: 9px;");
        box.getChildren().addAll(lblInfo, lblData);
        return box;
    }

    private void configurarGridTabela(GridPane grid, double largura) {
        grid.setPrefWidth(largura);
        grid.setMaxWidth(largura);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(12); // Data
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(42); // Descrição
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(18); // Categoria
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(14); // Forma
        ColumnConstraints col5 = new ColumnConstraints(); col5.setPercentWidth(14); // Valor
        grid.getColumnConstraints().addAll(col1, col2, col3, col4, col5);
    }

    private void adicionarCabecalhoColunas(GridPane grid) {
        adicionarCelulaCabecalho(grid, "DATA", 0);
        adicionarCelulaCabecalho(grid, "DESCRIÇÃO / FORNECEDOR", 1);
        adicionarCelulaCabecalho(grid, "CATEGORIA", 2);
        adicionarCelulaCabecalho(grid, "FORMA", 3);
        adicionarCelulaCabecalho(grid, "VALOR", 4);
    }
    
    private void adicionarCelulaCabecalho(GridPane grid, String texto, int col) {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: #1565c0; -fx-padding: 6;"); 
        Label label = new Label(texto);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        pane.getChildren().add(label);
        grid.add(pane, col, 0);
    }

    private void adicionarLinhaGrid(GridPane grid, Despesa d, int row, String corFundo) {
        boolean riscado = d.isExcluido();
        String corPadrao = riscado ? "#999999" : "black";
        String corForma = riscado ? "#999999" : getCorPorForma(d.getForma());
        adicionarCelulaDado(grid, d.getData(), 0, row, corFundo, false, corPadrao, riscado);
        String desc = d.getDescricao();
        if (riscado) desc += " (EXCLUÍDO)";
        adicionarCelulaDado(grid, desc, 1, row, corFundo, true, corPadrao, riscado);
        adicionarCelulaDado(grid, d.getCategoria(), 2, row, corFundo, true, corPadrao, riscado);
        adicionarCelulaDado(grid, d.getForma(), 3, row, corFundo, false, corForma, riscado);
        adicionarCelulaDado(grid, d.getValorFormatado(), 4, row, corFundo, false, corPadrao, riscado);
    }

    private void adicionarCelulaDado(GridPane grid, String texto, int col, int row, String corFundo, boolean wrap, String corTexto, boolean riscado) {
        StackPane pane = new StackPane();
        pane.setStyle("-fx-background-color: " + corFundo + "; -fx-padding: 5;");
        Label label = new Label(texto);
        String style = "-fx-text-fill: " + corTexto + "; -fx-font-size: 9px;";
        if (riscado) { style += "-fx-strikethrough: true;"; } else { if (col == 3) style += "-fx-font-weight: bold;"; }
        label.setStyle(style);
        label.setWrapText(wrap);
        if(!wrap) label.setTextOverrun(OverrunStyle.CLIP);
        if (col == 4) pane.setAlignment(javafx.geometry.Pos.CENTER_RIGHT); else pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT); 
        pane.getChildren().add(label);
        grid.add(pane, col, row);
    }
    
    private String getCorPorForma(String forma) {
        if (forma == null) return "black";
        switch (forma.toUpperCase()) {
            case "DINHEIRO": return "#2e7d32"; case "PIX": return "#1565c0"; case "CARTAO": return "#e65100"; case "TRANSFERENCIA": return "#6a1b9a"; case "BOLETO": return "#c62828"; default: return "black";
        }
    }

    @FXML
    public void abrirCadastroBoleto() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CadastroBoleto.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Gestão de Boletos e Prazos");
            stage.setScene(TemaManager.criarSceneComTema(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.showAndWait();
            
            filtrar(); 
        } catch (Exception e) {
            System.err.println("FinanceiroSaidaController.abrirBoletos: erro ao abrir tela de boletos — " + e.getMessage());
            e.printStackTrace();
            AlertHelper.info("Erro interno. Contate o administrador.");
        }
    }
    
    @FXML
    public void novaCategoria() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Categoria");
        dialog.setHeaderText("Cadastrar Categoria de Despesa");
        dialog.setContentText("Nome:");
        Optional<String> res = dialog.showAndWait();
        if(res.isPresent() && !res.get().isEmpty()) {
            try(Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement("INSERT INTO categorias_despesa (nome) VALUES (?)")) {
                stmt.setString(1, res.get().toUpperCase());
                stmt.executeUpdate();
                carregarCategorias(); 
            } catch(Exception e) { AlertHelper.info("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage()); }
        }
    }
    
    @FXML public void limparFiltros() {
        dpFiltroData.setValue(null);
        dpFiltroData.getEditor().clear(); 
        cmbFiltroCategoria.getSelectionModel().clearSelection();
        cmbFiltroPagamento.getSelectionModel().clearSelection();
        cmbFiltroPagamento.getSelectionModel().select("Todas");
        cmbFiltroCategoria.getSelectionModel().select("Todas");
        filtrar();
    }

    @FXML
    public void abrirAuditoria() {
        if (cmbFiltroViagem.getValue() == null || cmbFiltroViagem.getValue().id == 0) {
            AlertHelper.info("Selecione uma viagem específica no filtro para ver a auditoria.\nO sistema precisa saber de qual viagem você quer ver o histórico.");
            return;
        }
        
        AuditoriaExclusoesSaida tela = new AuditoriaExclusoesSaida();
        tela.abrir(cmbFiltroViagem.getValue().id, cmbFiltroViagem.getValue().label);
    }
    
    private void carregarCategorias() {
        listaCategoriasOriginal.clear();
        listaCategoriasOriginal.add("Todas"); 
        ObservableList<String> catsParaCadastro = FXCollections.observableArrayList();
        try (Connection con = ConexaoBD.getConnection(); ResultSet rs = con.prepareStatement("SELECT nome FROM categorias_despesa ORDER BY nome").executeQuery()) {
            while(rs.next()) {
                String nome = rs.getString(1);
                listaCategoriasOriginal.add(nome);
                catsParaCadastro.add(nome);
            }
        } catch(Exception e) { System.err.println("Erro em FinanceiroSaidaController.carregarCategorias: " + e.getMessage()); }
        cmbCategoria.setItems(catsParaCadastro);
        this.listaCategoriasOriginal = catsParaCadastro; 
        configurarAutoComplete(cmbCategoria); 
        ObservableList<String> catsFiltro = FXCollections.observableArrayList("Todas");
        catsFiltro.addAll(catsParaCadastro);
        cmbFiltroCategoria.setItems(catsFiltro);
    }
    
    private int buscarIdCategoria(String nome) throws SQLException {
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement("SELECT id FROM categorias_despesa WHERE nome = ?")) {
            stmt.setString(1, nome);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) return rs.getInt(1);
        }
        return 1; 
    }

    private void configuringTabela() {
        colData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colForma.setCellValueFactory(new PropertyValueFactory<>("forma"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(col -> new TableCell<Despesa, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { 
                    setText(null); setStyle(""); 
                } else {
                    Despesa d = getTableView().getItems().get(getIndex());
                    if (d.isExcluido()) {
                        setText("EXCLUÍDO");
                        setStyle("-fx-text-fill: #999; -fx-strikethrough: true;");
                    } else {
                        setText(item);
                        setStyle(model.StatusPagamento.fromString(item).getEstiloCelula());
                    }
                }
            }
        });

        try { tabela.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm()); } catch(Exception e){ System.err.println("Erro em FinanceiroSaidaController.configuringTabela (CSS): " + e.getMessage()); }
    }
    private void configurarTabela() { configuringTabela(); }


    private static class DadosEmpresa {
        String nome;
        String pathLogo;
    }

    private DadosEmpresa buscarDadosEmpresa() {
        DadosEmpresa d = new DadosEmpresa();
        try {
            dao.EmpresaDAO empresaDAO = new dao.EmpresaDAO();
            model.Empresa empresa = empresaDAO.buscarPorId(dao.EmpresaDAO.ID_EMPRESA_PRINCIPAL);
            if (empresa != null) {
                d.nome = empresa.getEmbarcacao();
                d.pathLogo = empresa.getCaminhoFoto();
            }
        } catch (Exception e) {
            System.err.println("FinanceiroSaidaController.buscarDadosEmpresa: erro ao buscar dados empresa — " + e.getMessage());
            e.printStackTrace();
        }
        return d;
    }

    public static class Despesa {
        private int id;
        private String data, descricao, categoria, forma, status;
        private java.math.BigDecimal valor;
        private boolean excluido;

        public Despesa(int id, String d, String desc, String cat, String forma, java.math.BigDecimal val, String st, boolean excluido) {
            this.id = id; this.data = d; this.descricao = desc; this.categoria = cat;
            this.forma = forma; this.valor = val != null ? val : java.math.BigDecimal.ZERO; this.status = st;
            this.excluido = excluido;
        }
        public int getId() { return id; }
        public String getData() { return data; }
        public String getDescricao() { return descricao; }
        public String getCategoria() { return categoria; }
        public String getForma() { return forma; }
        public java.math.BigDecimal getValor() { return valor; }
        public String getStatus() { return status; }
        public boolean isExcluido() { return excluido; }
        public String getValorFormatado() { return nf.format(valor); }
    }
    
}