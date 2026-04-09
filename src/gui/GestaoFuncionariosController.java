package gui;

import dao.ConexaoBD;
import gui.util.PermissaoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.print.PrinterJob;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public class GestaoFuncionariosController {

    @FXML private ListView<Funcionario> listaFuncionarios;
    @FXML private CheckBox chkMostrarInativos;
    
    @FXML private TextField txtNome, txtCpf, txtRg, txtCtps, txtTelefone, txtEndereco, txtCargo, txtSalario;
    
    @FXML private TextField txtValorInss; 
    @FXML private CheckBox chkDescontarInss; 
    @FXML private CheckBox chkClt;
    @FXML private CheckBox chkDecimo; 
    
    @FXML private Label lblProvisaoDecimo, lblStatusFuncionario;
    @FXML private DatePicker dpDataNascimento, dpAdmissao, dpInicioCalculo; 
    
    @FXML private Label lblAcumulado, lblPago, lblSaldo, lblDiasTrabalhados, lblValorDiaria, lblMesReferenciaSistema; 
    @FXML private TextField txtDescricaoPagamento, txtValorPagamento;
    @FXML private Button btnSair; 
    @FXML private VBox boxDemissao; 
    
    @FXML private ComboBox<String> cbMesAno; 
    
    @FXML private TableView<PagamentoHistorico> tabelaHistorico;
    @FXML private TableColumn<PagamentoHistorico, String> colDataHist;
    @FXML private TableColumn<PagamentoHistorico, String> colDescHist;
    @FXML private TableColumn<PagamentoHistorico, String> colValorHist;
    
    private Funcionario funcionarioSelecionado;
    
    private static final Locale BRASIL = new Locale("pt", "BR");
    private static final NumberFormat nf = NumberFormat.getCurrencyInstance(BRASIL);
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private static final DateTimeFormatter dtfMesExtenso = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM/yyyy")
            .toFormatter(BRASIL);

    @FXML
    public void initialize() {
        if (!PermissaoService.isAdmin()) { PermissaoService.exigirAdmin("Gestao de Funcionarios"); return; }
        configurarTabela();
        
        // --- REMOVIDO A LINHA QUE FORÇAVA O ESTILO DO BOTÃO SAIR ---
        // O estilo agora é controlado pela classe .botao-perigo no CSS
        
        if(cbMesAno != null) {
            carregarListaMeses();
            cbMesAno.setOnAction(e -> {
                if(funcionarioSelecionado != null) {
                    carregarHistoricoFinanceiro(funcionarioSelecionado);
                }
            });
        }

        listaFuncionarios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) selecionarFuncionario(newVal);
        });

        // DR010: carrega funcionarios em background
        Thread bg = new Thread(this::carregarFuncionarios);
        bg.setDaemon(true);
        bg.start();
    }

    private void configurarTabela() {
        colDataHist.setCellValueFactory(new PropertyValueFactory<>("data"));
        colDescHist.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        
        colDescHist.setCellFactory(column -> new TableCell<PagamentoHistorico, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); } 
                else {
                    String textoLimpo = item;
                    if (funcionarioSelecionado != null) {
                        textoLimpo = item.replace(funcionarioSelecionado.getNome(), "")
                                     .replace(funcionarioSelecionado.getNome().toUpperCase(), "")
                                     .replace(" - - ", " - ").trim();
                        if (textoLimpo.startsWith("- ")) textoLimpo = textoLimpo.substring(2);
                    }
                    setText(textoLimpo);
                }
            }
        });

        colValorHist.setCellValueFactory(new PropertyValueFactory<>("valorFormatado"));
        colValorHist.setCellFactory(column -> new TableCell<PagamentoHistorico, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); } 
                else {
                    setText(item);
                    PagamentoHistorico atual = getTableView().getItems().get(getIndex());
                    if (atual.tipo.equals("DESCONTO")) setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;"); 
                    else setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                }
            }
        });
    }
    
    private void carregarListaMeses() {
        ObservableList<String> meses = FXCollections.observableArrayList();
        LocalDate data = LocalDate.now().plusMonths(1); 
        
        for(int i=0; i<15; i++) {
            meses.add(data.format(dtfMesExtenso).toUpperCase());
            data = data.minusMonths(1);
        }
        
        String mesAtual = LocalDate.now().format(dtfMesExtenso).toUpperCase();
        
        cbMesAno.setItems(meses);
        
        if(meses.contains(mesAtual)) cbMesAno.getSelectionModel().select(mesAtual);
        else cbMesAno.getSelectionModel().select(0);
    }
    
    @FXML public void sair() { try { ((Stage) listaFuncionarios.getScene().getWindow()).close(); } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.sair: " + e.getMessage()); } }
    
    @FXML
    public void novoFuncionario() {
        listaFuncionarios.getSelectionModel().clearSelection();
        funcionarioSelecionado = null;
        limparCampos();
        dpAdmissao.setValue(LocalDate.now());
        dpInicioCalculo.setValue(null); 
        if(chkDecimo != null) chkDecimo.setSelected(false);
        if(chkClt != null) chkClt.setSelected(false);
        if(chkDescontarInss != null) chkDescontarInss.setSelected(false);
        tabelaHistorico.setItems(FXCollections.observableArrayList()); 
        if(boxDemissao != null) boxDemissao.setVisible(false);
    }

    @FXML
    public void carregarFuncionarios() {
        ObservableList<Funcionario> lista = FXCollections.observableArrayList();
        boolean mostrarInativos = (chkMostrarInativos != null && chkMostrarInativos.isSelected());
        String sql = mostrarInativos ? "SELECT * FROM funcionarios ORDER BY nome" : "SELECT * FROM funcionarios WHERE ativo = true ORDER BY nome";
        
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while(rs.next()) {
                Funcionario f = new Funcionario();
                f.setId(rs.getInt("id"));
                f.setNome(rs.getString("nome"));
                f.setCargo(rs.getString("cargo"));
                f.setSalario(rs.getDouble("salario"));
                if (rs.getDate("data_admissao") != null) f.setDataAdmissao(rs.getDate("data_admissao").toLocalDate());
                try { if (rs.getDate("data_inicio_calculo") != null) f.setDataInicioCalculo(rs.getDate("data_inicio_calculo").toLocalDate()); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna data_inicio_calculo ausente ou invalida — " + e.getMessage()); }
                try { f.setRecebe13(rs.getBoolean("recebe_decimo_terceiro")); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna recebe_decimo_terceiro indisponivel — " + e.getMessage()); f.setRecebe13(false); }

                try { f.setClt(rs.getBoolean("is_clt")); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna is_clt indisponivel — " + e.getMessage()); f.setClt(false); }
                try { f.setValorInss(rs.getDouble("valor_inss")); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna valor_inss indisponivel — " + e.getMessage()); f.setValorInss(0.0); }
                try { f.setDescontarInss(rs.getBoolean("descontar_inss")); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna descontar_inss indisponivel — " + e.getMessage()); f.setDescontarInss(false); }

                try { f.setAtivo(rs.getBoolean("ativo")); } catch (Exception e) { System.err.println("GestaoFuncionariosController.mapResultSet: coluna ativo indisponivel — " + e.getMessage()); f.setAtivo(true); }
                
                f.setCpf(rs.getString("cpf")); f.setRg(rs.getString("rg")); f.setCtps(rs.getString("ctps"));
                f.setTelefone(rs.getString("telefone")); f.setEndereco(rs.getString("endereco"));
                if (rs.getDate("data_nascimento") != null) f.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
                lista.add(f);
            }
        } catch (SQLException e) { e.printStackTrace(); javafx.application.Platform.runLater(() -> alert("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage())); }
        ObservableList<Funcionario> finalLista = lista;
        javafx.application.Platform.runLater(() -> listaFuncionarios.setItems(finalLista));
    }
    
    private void selecionarFuncionario(Funcionario f) {
        this.funcionarioSelecionado = f;
        txtNome.setText(f.getNome());
        txtCpf.setText(f.getCpf() != null ? f.getCpf() : "");
        txtRg.setText(f.getRg() != null ? f.getRg() : "");
        txtCtps.setText(f.getCtps() != null ? f.getCtps() : "");
        txtTelefone.setText(f.getTelefone() != null ? f.getTelefone() : "");
        txtEndereco.setText(f.getEndereco() != null ? f.getEndereco() : "");
        txtCargo.setText(f.getCargo());
        txtSalario.setText(String.format("%.2f", f.getSalario()));
        
        if(txtValorInss != null) txtValorInss.setText(String.format("%.2f", f.getValorInss()));
        if(chkDescontarInss != null) chkDescontarInss.setSelected(f.isDescontarInss());
        if(chkClt != null) chkClt.setSelected(f.isClt());
        
        dpAdmissao.setValue(f.getDataAdmissao());
        dpDataNascimento.setValue(f.getDataNascimento());
        dpInicioCalculo.setValue(f.getDataInicioCalculo()); 
        if(chkDecimo != null) chkDecimo.setSelected(f.isRecebe13());
        
        if (lblStatusFuncionario != null) {
            lblStatusFuncionario.setText(f.isAtivo() ? "ATIVO" : "DEMITIDO / INATIVO");
            lblStatusFuncionario.setStyle("-fx-text-fill: " + (f.isAtivo() ? "#2e7d32" : "#c62828") + "; -fx-font-weight: bold;");
            if(boxDemissao != null) boxDemissao.setDisable(!f.isAtivo());
        }
        
        String mesAtual = LocalDate.now().format(dtfMesExtenso).toUpperCase();
        if(cbMesAno.getItems().contains(mesAtual)) cbMesAno.getSelectionModel().select(mesAtual);

        calcularFinanceiro(f);
        carregarHistoricoFinanceiro(f); 
        if(boxDemissao != null) boxDemissao.setVisible(true);
    }
    
    private void limparCampos() {
        txtNome.clear(); txtCpf.clear(); txtRg.clear(); txtCtps.clear(); 
        txtTelefone.clear(); txtEndereco.clear(); txtCargo.clear(); txtSalario.clear();
        if(txtValorInss != null) txtValorInss.setText("");
        
        dpAdmissao.setValue(null); dpDataNascimento.setValue(null); dpInicioCalculo.setValue(null);
        if(lblProvisaoDecimo != null) lblProvisaoDecimo.setText("");
        lblAcumulado.setText("R$ 0,00"); lblPago.setText("R$ 0,00"); lblSaldo.setText("R$ 0,00"); lblDiasTrabalhados.setText("0"); 
        if(lblValorDiaria != null) lblValorDiaria.setText("");
        if(lblMesReferenciaSistema != null) lblMesReferenciaSistema.setText("Mês Atual");
    }

    @FXML
    public void salvarFuncionario() {
        if (txtNome.getText().isEmpty() || txtCargo.getText().isEmpty() || txtSalario.getText().isEmpty() || dpAdmissao.getValue() == null) {
            alert("Preencha: Nome, Cargo, Salário e Data de Admissão."); return;
        }
        try (Connection con = ConexaoBD.getConnection()) {
            String sql; PreparedStatement stmt;
            double salario = Double.parseDouble(txtSalario.getText().replace(".", "").replace(",", "."));
            
            double vInss = 0.0;
            if (txtValorInss != null && !txtValorInss.getText().isEmpty()) {
                vInss = Double.parseDouble(txtValorInss.getText().replace(".", "").replace(",", "."));
            }
            boolean descInss = (chkDescontarInss != null) && chkDescontarInss.isSelected();
            boolean isClt = (chkClt != null) && chkClt.isSelected();
            boolean tem13 = (chkDecimo != null) && chkDecimo.isSelected();
            
            if (funcionarioSelecionado == null) {
                sql = "INSERT INTO funcionarios (nome, cpf, rg, ctps, telefone, endereco, cargo, salario, data_admissao, data_nascimento, data_inicio_calculo, recebe_decimo_terceiro, is_clt, valor_inss, descontar_inss, ativo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, true)";
                stmt = con.prepareStatement(sql);
            } else {
                sql = "UPDATE funcionarios SET nome=?, cpf=?, rg=?, ctps=?, telefone=?, endereco=?, cargo=?, salario=?, data_admissao=?, data_nascimento=?, data_inicio_calculo=?, recebe_decimo_terceiro=?, is_clt=?, valor_inss=?, descontar_inss=? WHERE id=?";
                stmt = con.prepareStatement(sql);
            }
            stmt.setString(1, txtNome.getText().toUpperCase());
            stmt.setString(2, txtCpf.getText()); stmt.setString(3, txtRg.getText()); 
            stmt.setString(4, txtCtps.getText()); stmt.setString(5, txtTelefone.getText()); stmt.setString(6, txtEndereco.getText()); 
            stmt.setString(7, txtCargo.getText().toUpperCase());
            stmt.setDouble(8, salario);
            stmt.setDate(9, java.sql.Date.valueOf(dpAdmissao.getValue()));
            stmt.setDate(10, dpDataNascimento.getValue() != null ? java.sql.Date.valueOf(dpDataNascimento.getValue()) : null);
            stmt.setDate(11, dpInicioCalculo.getValue() != null ? java.sql.Date.valueOf(dpInicioCalculo.getValue()) : null);
            stmt.setBoolean(12, tem13);
            stmt.setBoolean(13, isClt);
            stmt.setDouble(14, vInss);
            stmt.setBoolean(15, descInss);
            
            if (funcionarioSelecionado != null) stmt.setInt(16, funcionarioSelecionado.getId());
            stmt.executeUpdate();
            alert("Salvo com sucesso!");
            
            int idSelecionado = funcionarioSelecionado != null ? funcionarioSelecionado.getId() : 0;
            carregarFuncionarios(); 
            for(Funcionario f : listaFuncionarios.getItems()) {
                if(f.setId(= idSelecionado) { listaFuncionarios.getSelectionModel().select(f)); break; }
            }
            
        } catch (Exception e) { e.printStackTrace(); alert("Erro interno. Contate o administrador."); System.err.println("Erro ao salvar: " + e.getMessage()); }
    }

    /**
     * Calcula dias comerciais (convencao 30/360 — mes comercial = 30 dias).
     * DL026: +1 inclui o dia de inicio (padrao trabalhista BR).
     * Normaliza dia 31→30 e fevereiro 28/29→30.
     */
    private double calcularDiasComerciais(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) return 0;
        int diaInicio = Math.min(inicio.getDayOfMonth(), 30);
        int diaFim = Math.min(fim.getDayOfMonth(), 30);
        if (fim.getMonthValue() == 2 && diaFim >= 28) diaFim = 30;
        if (inicio.getMonthValue() == 2 && diaInicio >= 28) diaInicio = 30;
        double dias = (fim.getYear() - inicio.getYear()) * 360
                    + (fim.getMonthValue() - inicio.getMonthValue()) * 30
                    + (diaFim - diaInicio);
        return dias + 1; // inclui dia de inicio
    }

    private void calcularFinanceiro(Funcionario f) {
        if (f.setDataAdmissao(= null) return);
        
        if (f.isRecebe13() && lblProvisaoDecimo != null) {
            // DL036: calcular meses trabalhados no ANO CORRENTE (nao total desde admissao)
            LocalDate inicioAno = LocalDate.of(LocalDate.now().getYear(), 1, 1);
            LocalDate base = f.getDataAdmissao().isAfter(inicioAno) ? f.getDataAdmissao() : inicioAno;
            long mesesNoAno = ChronoUnit.MONTHS.between(base, LocalDate.now()) + 1;
            mesesNoAno = Math.min(mesesNoAno, 12); // maximo 12 meses
            double provisao = (f.getSalario() / 12.0) * mesesNoAno;
            lblProvisaoDecimo.setText("Provisão 13º: " + nf.format(provisao));
        }

        LocalDate dataInicio = (f.getDataInicioCalculo() != null) ? f.getDataInicioCalculo() : f.getDataAdmissao();
        LocalDate dataHoje = LocalDate.now();
        
        if (dataInicio.isAfter(dataHoje)) {
             zerarTela();
             return;
        }
        
        boolean isCicloAtual = (dataInicio.getMonthValue() == dataHoje.getMonthValue() && dataInicio.getYear() == dataHoje.getYear());
        
        if(lblMesReferenciaSistema != null) {
            String mesRef = dataInicio.format(dtfMesExtenso).toUpperCase();
            if (!isCicloAtual) mesRef += " (EM ABERTO)";
            else mesRef += " (CICLO ATUAL)";
            lblMesReferenciaSistema.setText(mesRef);
        }
        
        double salarioDiario = f.getSalario() / 30.0;
        if(lblValorDiaria != null) lblValorDiaria.setText("Valor Diária: " + nf.format(salarioDiario));
        
        double totalDinheiroPago = buscarTotalPagamentosReais(f, dataInicio);
        double totalDescontosRH = buscarTotalEventosRH(f, dataInicio);
        totalDescontosRH += buscarTotalDescontosLegado(f, dataInicio);
        
        double diasTrabalhados = calcularDiasComerciais(dataInicio, dataHoje);
        if (diasTrabalhados > 30) diasTrabalhados = 30; 
        
        double descontoAutomatico = 0.0;
        
        boolean inssJaLancado = verificarSeExisteEventoRH(f, dataInicio, "INSS");
        if (!inssJaLancado) inssJaLancado = verificarSeExisteDescontoLegado(f, dataInicio, "ENCARGOS");
        
        if (!inssJaLancado && f.isDescontarInss() && f.getValorInss() > 0) {
            descontoAutomatico = f.getValorInss();
        }

        double salarioAcumulado = diasTrabalhados * salarioDiario;
        salarioAcumulado = Math.round(salarioAcumulado * 100.0) / 100.0;

        double saldo = salarioAcumulado - totalDinheiroPago - totalDescontosRH - descontoAutomatico;
        
        lblDiasTrabalhados.setText((int)diasTrabalhados + " dias");
        lblAcumulado.setText(nf.format(salarioAcumulado));
        lblPago.setText(nf.format(totalDinheiroPago)); 
        lblSaldo.setText(nf.format(saldo));
        
        if (saldo < 0) lblSaldo.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-font-size: 26px;");
        else lblSaldo.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 26px;");
    }
    
    private void zerarTela() {
        lblDiasTrabalhados.setText("0 dias");
        lblAcumulado.setText("R$ 0,00");
        lblPago.setText("R$ 0,00");
        lblSaldo.setText("R$ 0,00");
    }
    
    @FXML
    public void registrarDescontoOficial() {
        if (funcionarioSelecionado == null) { alert("Selecione um funcionário."); return; }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Lançar Desconto Oficial");
        dialog.setHeaderText("Desconto Manual (Extra) - RH");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Ex: Avaria, Uniforme");
        TextField txtVal = new TextField(); txtVal.setPromptText("0,00");
        grid.add(new Label("Descrição:"), 0, 0); grid.add(txtDesc, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1); grid.add(txtVal, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String descricao = "DESC. " + txtDesc.getText().toUpperCase();
                double valor = Double.parseDouble(txtVal.getText().replace(".", "").replace(",", "."));
                lancarEventoContabil(funcionarioSelecionado, "DESCONTO_MANUAL", descricao, valor, LocalDate.now(), funcionarioSelecionado.getDataInicioCalculo());
                alert("Desconto lançado no prontuário do funcionário!");
                calcularFinanceiro(funcionarioSelecionado);
                carregarHistoricoFinanceiro(funcionarioSelecionado);
            } catch(Exception e) { alert("Valor inválido."); }
        }
    }
    
    @FXML
    public void registrarFalta() {
        if (funcionarioSelecionado == null) { alert("Selecione um funcionário."); return; }
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Registrar Falta");
        dialog.setHeaderText("Lançar Falta no RH");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DatePicker dpFalta = new DatePicker(LocalDate.now());
        VBox content = new VBox(10, new Label("Data da Falta:"), dpFalta);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(bt -> (bt == ButtonType.OK) ? dpFalta.getValue() : null);
        Optional<LocalDate> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                LocalDate dataFalta = result.get();
                // DL061: verificar se ja existe falta para o mesmo dia
                String sqlCheck = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND data_referencia = ? AND tipo = 'FALTA'";
                try (Connection conCheck = dao.ConexaoBD.getConnection();
                     java.sql.PreparedStatement psCheck = conCheck.prepareStatement(sqlCheck)) {
                    psCheck.setInt(1, funcionarioSelecionado.getId());
                    psCheck.setDate(2, java.sql.Date.valueOf(dataFalta));
                    try (java.sql.ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                            alert("Ja existe falta registrada para " + funcionarioSelecionado.getNome() + " em " + dataFalta.format(dtf) + ".");
                            return;
                        }
                    }
                }
                double valorDesconto = funcionarioSelecionado.getSalario() / 30.0;
                String descricao = "FALTA - " + funcionarioSelecionado.getNome().toUpperCase() + " - " + dataFalta.format(dtf);
                lancarEventoContabil(funcionarioSelecionado, "FALTA", descricao, valorDesconto, dataFalta, funcionarioSelecionado.getDataInicioCalculo());
                alert("Falta registrada no prontuário! Valor: " + nf.format(valorDesconto));
                calcularFinanceiro(funcionarioSelecionado);
                carregarHistoricoFinanceiro(funcionarioSelecionado);
            } catch(Exception e) { e.printStackTrace(); alert("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage()); }
        }
    }
    
    @FXML
    public void fecharMes() {
        if (funcionarioSelecionado == null) { alert("Selecione um funcionário."); return; }
        
        LocalDate dataInicio = (funcionarioSelecionado.getDataInicioCalculo() != null) ? funcionarioSelecionado.getDataInicioCalculo() : funcionarioSelecionado.getDataAdmissao();
        LocalDate dataHoje = LocalDate.now();
        
        double totalPago = buscarTotalPagamentosReais(funcionarioSelecionado, dataInicio);
        double totalDescontosRH = buscarTotalEventosRH(funcionarioSelecionado, dataInicio);
        totalDescontosRH += buscarTotalDescontosLegado(funcionarioSelecionado, dataInicio);
        
        double descontoInss = 0.0;
        boolean inssJaLancado = verificarSeExisteEventoRH(funcionarioSelecionado, dataInicio, "INSS");
        if (!inssJaLancado) inssJaLancado = verificarSeExisteDescontoLegado(funcionarioSelecionado, dataInicio, "ENCARGOS");
        
        if (!inssJaLancado && funcionarioSelecionado.isDescontarInss() && funcionarioSelecionado.getValorInss() > 0) {
            descontoInss = funcionarioSelecionado.getValorInss();
        }

        double diasTrabalhados = calcularDiasComerciais(dataInicio, dataHoje);
        if (diasTrabalhados > 30) diasTrabalhados = 30;
        
        double salarioDiario = funcionarioSelecionado.getSalario() / 30.0;
        double acumulado = diasTrabalhados * salarioDiario;
        double saldoParaPagar = acumulado - totalPago - totalDescontosRH - descontoInss;
        saldoParaPagar = Math.round(saldoParaPagar * 100.0) / 100.0;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Fechar Ciclo");
        confirm.setHeaderText("Encerrar competência e iniciar novo mês?");
        
        StringBuilder msg = new StringBuilder();
        msg.append("Resumo do Fechamento:\n");
        msg.append("Acumulado Bruto: ").append(nf.format(acumulado)).append("\n");
        if(descontoInss > 0) msg.append("INSS/Encargos (Será registrado): ").append(nf.format(descontoInss)).append("\n");
        msg.append("Total Descontos (Faltas/Outros): ").append(nf.format(totalDescontosRH)).append("\n");
        msg.append("Já Pago em Dinheiro: ").append(nf.format(totalPago)).append("\n");
        msg.append("Saldo Líquido Restante: ").append(nf.format(saldoParaPagar)).append("\n\n");
        msg.append("Ao confirmar, o INSS será gravado no histórico (RH) e o saldo será pago.");
        confirm.setContentText(msg.toString());
        
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                // DL052: data de referencia e sempre o ultimo dia do periodo trabalhado (mes atual)
                LocalDate dataReferenciaFechamento = dataHoje;
                
                if (descontoInss > 0) {
                    lancarEventoContabil(funcionarioSelecionado, "INSS", "DESC. ENCARGOS (INSS/FOLHA)", descontoInss, dataReferenciaFechamento, dataInicio);
                }
                
                if (saldoParaPagar > 0) {
                    String mesExtenso = dataInicio.format(dtfMesExtenso).toUpperCase();
                    String desc = "FECHAMENTO MENSAL " + funcionarioSelecionado.getNome().toUpperCase() + " REF " + mesExtenso;
                    lancarDebitoAutomatico(funcionarioSelecionado, desc, saldoParaPagar, dataReferenciaFechamento, "DINHEIRO");
                }
                
                LocalDate novaDataInicio = LocalDate.of(dataHoje.getYear(), dataHoje.getMonth(), 1);
                if (dataHoje.getDayOfMonth() >= 28) {
                    novaDataInicio = dataHoje.withDayOfMonth(1).plusMonths(1);
                }
                
                atualizarDataInicioCalculo(funcionarioSelecionado.getId(), novaDataInicio);
                funcionarioSelecionado.setDataInicioCalculo(novaDataInicio);
                dpInicioCalculo.setValue(novaDataInicio); 
                
                alert("Ciclo Fechado! Novo ciclo iniciado em: " + novaDataInicio.format(dtf));
                calcularFinanceiro(funcionarioSelecionado);
                carregarHistoricoFinanceiro(funcionarioSelecionado);
                
            } catch(Exception e) { e.printStackTrace(); alert("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage()); }
        }
    }

    private void atualizarDataInicioCalculo(int idFunc, LocalDate novaData) {
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET data_inicio_calculo = ? WHERE id = ?")) {
            stmt.setDate(1, java.sql.Date.valueOf(novaData));
            stmt.setInt(2, idFunc);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- MÉTODOS DE BUSCA SEPARADOS ---
    
    // #DB014/#DB015: try-with-resources + getBigDecimal para valores financeiros
    private double buscarTotalPagamentosReais(Funcionario f, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas WHERE funcionario_id = ? AND is_excluido = false AND data_pagamento >= ? AND (forma_pagamento IS NULL OR forma_pagamento != 'DESCONTO' AND forma_pagamento != 'RETIDO')";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setDate(2, java.sql.Date.valueOf(inicio));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { java.math.BigDecimal v = rs.getBigDecimal("total"); return v != null ? v.doubleValue() : 0.0; }
            }
        } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.buscarTotalPagamentosReais: " + e.getMessage()); }
        return 0.0;
    }

    private double buscarTotalEventosRH(Funcionario f, LocalDate dataReferencia) {
        String sql = "SELECT COALESCE(SUM(valor), 0) as total FROM eventos_rh WHERE funcionario_id = ? AND data_referencia >= ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setDate(2, java.sql.Date.valueOf(dataReferencia));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { java.math.BigDecimal v = rs.getBigDecimal("total"); return v != null ? v.doubleValue() : 0.0; }
            }
        } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.buscarTotalEventosRH: " + e.getMessage()); }
        return 0.0;
    }

    private double buscarTotalDescontosLegado(Funcionario f, LocalDate inicio) {
        String sql = "SELECT COALESCE(SUM(valor_pago), 0) as total FROM financeiro_saidas WHERE funcionario_id = ? AND data_pagamento >= ? AND (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO')";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setDate(2, java.sql.Date.valueOf(inicio));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) { java.math.BigDecimal v = rs.getBigDecimal("total"); return v != null ? v.doubleValue() : 0.0; }
            }
        } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.buscarTotalDescontosLegado: " + e.getMessage()); }
        return 0.0;
    }

    private boolean verificarSeExisteEventoRH(Funcionario f, LocalDate dataReferencia, String tipo) {
        String sql = "SELECT COUNT(*) FROM eventos_rh WHERE funcionario_id = ? AND data_referencia >= ? AND tipo = ?";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setDate(2, java.sql.Date.valueOf(dataReferencia));
            stmt.setString(3, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.verificarSeExisteEventoRH: " + e.getMessage()); }
        return false;
    }

    private boolean verificarSeExisteDescontoLegado(Funcionario f, LocalDate dataReferencia, String termo) {
        String sql = "SELECT COUNT(*) FROM financeiro_saidas WHERE UPPER(descricao) LIKE ? AND data_pagamento >= ? AND forma_pagamento = 'DESCONTO'";
        try (Connection con = ConexaoBD.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "%" + termo.toUpperCase() + "%");
            stmt.setDate(2, java.sql.Date.valueOf(dataReferencia));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.verificarSeExisteDescontoLegado: " + e.getMessage()); }
        return false;
    }

    @FXML public void lancarPagamento() {
         if (!PermissaoService.exigirAdmin("Lancar Pagamento de Funcionario")) return;
         if (funcionarioSelecionado == null) return;
         try {
            double valor = Double.parseDouble(txtValorPagamento.getText().replace(".", "").replace(",", "."));
            if (txtDescricaoPagamento.getText().isEmpty()) { alert("Digite uma descrição."); return; }
            String desc = "PAGTO " + funcionarioSelecionado.getNome().toUpperCase() + " - " + txtDescricaoPagamento.getText().toUpperCase();
            
            lancarDebitoAutomatico(funcionarioSelecionado, desc, valor, LocalDate.now(), "DINHEIRO");
            
            txtValorPagamento.clear(); txtDescricaoPagamento.clear();
            calcularFinanceiro(funcionarioSelecionado); carregarHistoricoFinanceiro(funcionarioSelecionado);
         } catch(Exception e) { alert("Valor inválido."); }
    }
    
    private int buscarIdCategoriaFuncionarios(Connection con) {
        int id = 1; 
        try {
            String sql = "SELECT id FROM categorias WHERE UPPER(nome) LIKE '%FUNCIONARIO%' OR UPPER(nome) LIKE '%FOLHA%' OR UPPER(nome) LIKE '%RH%' OR UPPER(nome) LIKE '%PAGAMENTO%' LIMIT 1";
            try (PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("id");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return id;
    }
    
    private void lancarDebitoAutomatico(Funcionario f, String descricao, double valor, LocalDate dataRef, String formaPagamento) {
        try {
            int idViagem = 1;
            try (Connection con = ConexaoBD.getConnection();
                 PreparedStatement psViagem = con.prepareStatement("SELECT id_viagem FROM viagens WHERE is_atual = true LIMIT 1");
                 ResultSet rs = psViagem.executeQuery()) {
                if(rs.next()) idViagem = rs.getInt(1);
            }
            
            String sql = "INSERT INTO financeiro_saidas (descricao, valor_total, valor_pago, data_vencimento, data_pagamento, status, forma_pagamento, id_categoria, id_viagem, funcionario_id, is_excluido) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false)";
            try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, descricao.toUpperCase());
                stmt.setDouble(2, valor); stmt.setDouble(3, valor);
                stmt.setDate(4, java.sql.Date.valueOf(dataRef)); stmt.setDate(5, java.sql.Date.valueOf(dataRef));
                stmt.setString(6, "PAGO");
                stmt.setString(7, formaPagamento);

                int idCategoria = buscarIdCategoriaFuncionarios(con);
                stmt.setInt(8, idCategoria);

                stmt.setInt(9, idViagem);
                stmt.setInt(10, f.getId());
                stmt.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void lancarEventoContabil(Funcionario f, String tipo, String descricao, double valor, LocalDate dataEvento, LocalDate dataReferencia) {
        String sql = "INSERT INTO eventos_rh (funcionario_id, tipo, descricao, valor, data_evento, data_referencia) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setString(2, tipo); 
            stmt.setString(3, descricao);
            stmt.setDouble(4, valor);
            stmt.setDate(5, java.sql.Date.valueOf(dataEvento));
            stmt.setDate(6, java.sql.Date.valueOf(dataReferencia != null ? dataReferencia : dataEvento));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            alert("Erro interno. Contate o administrador."); System.err.println("Erro ao salvar evento RH: " + e.getMessage());
        }
    }
    
    private void carregarHistoricoFinanceiro(Funcionario f) {
        ObservableList<PagamentoHistorico> historico = FXCollections.observableArrayList();
        String selecionado = cbMesAno.getValue();
        int mes = LocalDate.now().getMonthValue();
        int ano = LocalDate.now().getYear();
        
        if (selecionado != null && !selecionado.isEmpty()) {
            try {
                java.time.temporal.TemporalAccessor ta = dtfMesExtenso.parse(selecionado);
                mes = ta.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
                ano = ta.get(java.time.temporal.ChronoField.YEAR);
            } catch (Exception e) { System.err.println("Erro em GestaoFuncionariosController.carregarHistoricoFinanceiro (parse mes/ano): " + e.getMessage()); }
        }

        String sqlFin = "SELECT data_pagamento, descricao, valor_pago, forma_pagamento FROM financeiro_saidas " +
                      "WHERE funcionario_id = ? " +
                      "AND ( (forma_pagamento = 'DESCONTO' OR forma_pagamento = 'RETIDO') OR is_excluido = false ) " +
                      "AND EXTRACT(MONTH FROM data_pagamento) = ? AND EXTRACT(YEAR FROM data_pagamento) = ?";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sqlFin)) {
            stmt.setInt(1, f.getId());
            stmt.setInt(2, mes);
            stmt.setInt(3, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    String forma = rs.getString("forma_pagamento");
                    if (forma != null && (forma.equals("DESCONTO") || forma.equals("RETIDO"))) {
                        historico.add(new PagamentoHistorico(rs.getDate("data_pagamento").toLocalDate(), rs.getString("descricao"), rs.getDouble("valor_pago"), "DESCONTO"));
                    } else {
                        historico.add(new PagamentoHistorico(rs.getDate("data_pagamento").toLocalDate(), rs.getString("descricao"), rs.getDouble("valor_pago"), "DINHEIRO"));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String sqlRH = "SELECT data_evento, descricao, valor, tipo FROM eventos_rh " +
                        "WHERE funcionario_id = ? " +
                        "AND EXTRACT(MONTH FROM data_evento) = ? AND EXTRACT(YEAR FROM data_evento) = ?";
        try (Connection con = ConexaoBD.getConnection(); PreparedStatement stmt = con.prepareStatement(sqlRH)) {
            stmt.setInt(1, f.getId());
            stmt.setInt(2, mes);
            stmt.setInt(3, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    historico.add(new PagamentoHistorico(rs.getDate("data_evento").toLocalDate(), rs.getString("descricao"), rs.getDouble("valor"), "DESCONTO"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        boolean temInssGravado = historico.stream().anyMatch(h -> h.getDescricao().contains("INSS") || h.getDescricao().contains("ENCARGOS"));
        
        if (!temInssGravado && f.isDescontarInss() && f.getValorInss() > 0) {
            LocalDate dataInss = LocalDate.of(ano, mes, 1).plusMonths(1).minusDays(1);
            if (dataInss.isAfter(LocalDate.now())) dataInss = LocalDate.now();
            
            if (f.getDataAdmissao().isBefore(dataInss) || f.getDataAdmissao().isEqual(dataInss)) {
                historico.add(new PagamentoHistorico(dataInss, "DESC. ENCARGOS (INSS/FOLHA) - PREVISÃO", f.getValorInss(), "DESCONTO"));
            }
        }
        
        historico.sort(Comparator.comparing(PagamentoHistorico::getDataLocal));
        tabelaHistorico.setItems(historico);
    }
    
    @FXML public void imprimirExtrato() { 
        if (funcionarioSelecionado == null) return;
        PrinterJob job = PrinterJob.createPrinterJob();
        
        if (job != null && job.showPrintDialog(tabelaHistorico.getScene().getWindow())) {
            VBox paginaDupla = new VBox(20);
            paginaDupla.setStyle("-fx-background-color: white; -fx-padding: 0;"); 
            paginaDupla.setAlignment(Pos.TOP_LEFT);
            
            VBox viaEmpresa = criarLayoutHolerite("VIA DO EMPREGADOR");
            VBox viaFuncionario = criarLayoutHolerite("VIA DO FUNCIONÁRIO");
            
            HBox linhaCorte = new HBox(10);
            linhaCorte.setAlignment(Pos.CENTER);
            linhaCorte.setPadding(new Insets(5, 0, 5, 0));
            Line pontilhado = new Line(0, 0, 480, 0); 
            pontilhado.getStrokeDashArray().addAll(10d, 10d);
            pontilhado.setStroke(Color.GRAY);
            linhaCorte.getChildren().addAll(new Text("✂ CORTE"), pontilhado);
            
            paginaDupla.getChildren().addAll(viaEmpresa, linhaCorte, viaFuncionario);
            
            Printer printer = job.getPrinter();
            PageLayout layout = job.getJobSettings().getPageLayout();
            paginaDupla.layout();
            double larguraBase = 500; 
            double scale = (layout.getPrintableWidth() / larguraBase) * 0.98;
            paginaDupla.getTransforms().add(new Scale(scale, scale));
            
            if(job.printPage(paginaDupla)) job.endJob();
        }
    }
    
    private VBox criarLayoutHolerite(String tituloVia) {
        VBox page = new VBox(0);
        page.setPrefWidth(500); page.setMaxWidth(500);
        page.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-background-color: white;");
        
        VBox header = new VBox(2); 
        header.setStyle("-fx-background-color: #1565c0; -fx-padding: 5;"); 
        header.setAlignment(Pos.CENTER_LEFT);
        
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblEmpresa = new Label("F/B DEUS DE ALIANÇA V");
        lblEmpresa.setTextFill(Color.WHITE);
        lblEmpresa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); 
        
        Pane spacer = new Pane(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblVia = new Label(tituloVia);
        lblVia.setTextFill(Color.WHITE);
        lblVia.setFont(Font.font("Arial", FontWeight.NORMAL, 9));
        
        topRow.getChildren().addAll(lblEmpresa, spacer, lblVia);
        Label lblTitulo = new Label("RECIBO DE PAGAMENTO DE SALÁRIO");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); 
        header.getChildren().addAll(topRow, lblTitulo);
        
        GridPane gridDados = new GridPane();
        gridDados.setStyle("-fx-padding: 5; -fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0;");
        gridDados.setHgap(15); gridDados.setVgap(2); 
        
        String mesCompetencia = cbMesAno.getValue();
        if(mesCompetencia == null) mesCompetencia = "";
        
        gridDados.add(textoBold("FUNCIONÁRIO:", 10), 0, 0);
        gridDados.add(new Text(funcionarioSelecionado.getNome()), 1, 0);
        gridDados.add(textoBold("CARGO:", 10), 0, 1);
        gridDados.add(new Text(funcionarioSelecionado.getCargo()), 1, 1);
        gridDados.add(textoBold("COMPETÊNCIA:", 10), 2, 0);
        gridDados.add(textoBold(mesCompetencia, 11), 3, 0);
        
        VBox corpoTabela = new VBox();
        
        HBox titulos = new HBox();
        titulos.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 2; -fx-border-color: #bbdefb; -fx-border-width: 0 0 1 0;");
        titulos.getChildren().addAll(
            criarCelulaTexto("DATA", 60, true, Pos.CENTER_LEFT),
            criarCelulaTexto("DESCRIÇÃO", 240, true, Pos.CENTER_LEFT), 
            criarCelulaTexto("REF.", 30, true, Pos.CENTER),
            criarCelulaTexto("VENC.", 75, true, Pos.CENTER_RIGHT),
            criarCelulaTexto("DESC.", 75, true, Pos.CENTER_RIGHT)
        );
        corpoTabela.getChildren().add(titulos);
        
        corpoTabela.getChildren().add(criarLinhaHolerite("", "SALÁRIO BASE MENSAL", "30d", nf.format(funcionarioSelecionado.getSalario()), ""));
        
        double totalVencimentos = funcionarioSelecionado.getSalario();
        double totalDescontos = 0;
        
        for(PagamentoHistorico p : tabelaHistorico.getItems()) {
            String desc = p.getDescricao().replace(funcionarioSelecionado.getNome(), "").replace(funcionarioSelecionado.getNome().toUpperCase(), "").replace(" - - ", " ").trim();
            if(desc.startsWith("- ")) desc = desc.substring(2);
            if(desc.length() > 32) desc = desc.substring(0, 32) + "..."; 
            
            if(p.tipo.equals("DESCONTO")) {
                corpoTabela.getChildren().add(criarLinhaHolerite(p.getData(), desc, "", "", nf.format(p.valor)));
                totalDescontos += p.valor;
            } else {
                corpoTabela.getChildren().add(criarLinhaHolerite(p.getData(), desc, "", "", nf.format(p.valor)));
                totalDescontos += p.valor;
            }
        }
        
        GridPane totais = new GridPane();
        totais.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        totais.setHgap(20);
        
        totais.add(textoBold("TOT. VENCIMENTOS", 10), 0, 0);
        totais.add(new Text(nf.format(totalVencimentos)), 0, 1);
        totais.add(textoBold("TOT. DESCONTOS", 10), 1, 0);
        Text tDesc = new Text(nf.format(totalDescontos)); tDesc.setFill(Color.RED);
        totais.add(tDesc, 1, 1);
        
        VBox boxLiq = new VBox(2);
        boxLiq.setStyle("-fx-background-color: white; -fx-border-color: #1565c0; -fx-border-width: 1; -fx-padding: 5;");
        boxLiq.setAlignment(Pos.CENTER_RIGHT);
        boxLiq.setPrefWidth(130);
        Label lLiq = new Label("LÍQUIDO A RECEBER"); lLiq.setFont(Font.font("Arial", FontWeight.BOLD, 9)); lLiq.setTextFill(Color.web("#1565c0"));
        Label lVal = new Label(nf.format(totalVencimentos - totalDescontos)); lVal.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        boxLiq.getChildren().addAll(lLiq, lVal);
        totais.add(boxLiq, 3, 0, 1, 2);
        
        VBox ass = new VBox(2);
        ass.setAlignment(Pos.CENTER);
        ass.setPadding(new Insets(15, 0, 5, 0)); 
        ass.getChildren().addAll(
            new Text("__________________________________________"), 
            textoBold("ASSINATURA DO FUNCIONÁRIO", 9)
        );
        
        page.getChildren().addAll(header, gridDados, corpoTabela, totais, ass);
        return page;
    }
    
    private Text textoBold(String s, int size) {
        Text t = new Text(s);
        t.setFont(Font.font("Arial", FontWeight.BOLD, size));
        return t;
    }
    
    private HBox criarLinhaHolerite(String data, String desc, String ref, String venc, String descVal) {
        HBox linha = new HBox();
        linha.setPadding(new Insets(2, 5, 2, 5)); 
        linha.getChildren().addAll(
            criarCelulaTexto(data, 60, false, Pos.CENTER_LEFT),
            criarCelulaTexto(desc, 240, false, Pos.CENTER_LEFT),
            criarCelulaTexto(ref, 30, false, Pos.CENTER),
            criarCelulaTexto(venc, 75, false, Pos.CENTER_RIGHT),
            criarCelulaTexto(descVal, 75, false, Pos.CENTER_RIGHT)
        );
        return linha;
    }
    
    private Label criarCelulaTexto(String texto, double largura, boolean bold, Pos alin) {
        Label l = new Label(texto);
        l.setPrefWidth(largura);
        l.setAlignment(alin);
        if(bold) l.setFont(Font.font("Arial", FontWeight.BOLD, 10)); 
        else l.setFont(Font.font("Arial", 10));
        return l;
    }

    @FXML public void abrirTelaDemissao() {
        if (!PermissaoService.exigirAdmin("Demissao de Funcionario")) return;
        if (funcionarioSelecionado == null) { alert("Selecione um funcionário."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Demissão");
        confirm.setHeaderText("Deseja realmente demitir " + funcionarioSelecionado.getNome() + "?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try (Connection con = ConexaoBD.getConnection(); 
                 PreparedStatement stmt = con.prepareStatement("UPDATE funcionarios SET ativo = false WHERE id = ?")) {
                stmt.setInt(1, funcionarioSelecionado.getId());
                stmt.executeUpdate();
                alert("Funcionário demitido com sucesso.");
                carregarFuncionarios(); 
            } catch (SQLException e) { alert("Erro interno. Contate o administrador."); System.err.println("Erro: " + e.getMessage()); }
        }
    }

    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).show(); }
    
    // D019: campos PII como private com getters
    public static class Funcionario {
        private int id; private String nome, cpf, rg, ctps, telefone, endereco, cargo;
        private double salario; private LocalDate dataAdmissao, dataNascimento, dataInicioCalculo;
        private boolean recebe13; private boolean ativo; private boolean isClt;
        private double valorInss; private boolean descontarInss;
        public int getId() { return id; } public void setId(int id) { this.id = id; }
        public String getNome() { return nome; } public void setNome(String nome) { this.nome = nome; }
        public String getCpf() { return cpf; } public void setCpf(String cpf) { this.cpf = cpf; }
        public String getRg() { return rg; } public void setRg(String rg) { this.rg = rg; }
        public String getCtps() { return ctps; } public void setCtps(String ctps) { this.ctps = ctps; }
        public String getTelefone() { return telefone; } public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getEndereco() { return endereco; } public void setEndereco(String endereco) { this.endereco = endereco; }
        public String getCargo() { return cargo; } public void setCargo(String cargo) { this.cargo = cargo; }
        public double getSalario() { return salario; } public void setSalario(double salario) { this.salario = salario; }
        public LocalDate getDataAdmissao() { return dataAdmissao; } public void setDataAdmissao(LocalDate d) { this.dataAdmissao = d; }
        public LocalDate getDataNascimento() { return dataNascimento; } public void setDataNascimento(LocalDate d) { this.dataNascimento = d; }
        public LocalDate getDataInicioCalculo() { return dataInicioCalculo; } public void setDataInicioCalculo(LocalDate d) { this.dataInicioCalculo = d; }
        public boolean isRecebe13() { return recebe13; } public void setRecebe13(boolean v) { this.recebe13 = v; }
        public boolean isAtivo() { return ativo; } public void setAtivo(boolean v) { this.ativo = v; }
        public boolean isClt() { return isClt; } public void setClt(boolean v) { this.isClt = v; }
        public double getValorInss() { return valorInss; } public void setValorInss(double v) { this.valorInss = v; }
        public boolean isDescontarInss() { return descontarInss; } public void setDescontarInss(boolean v) { this.descontarInss = v; }
        @Override public String toString() { return nome + (ativo ? "" : " (INATIVO)"); }
    }
    
    public static class PagamentoHistorico {
        LocalDate data; String descricao; double valor; String tipo;
        public PagamentoHistorico(LocalDate data, String descricao, double valor, String tipo) { 
            this.data = data; this.descricao = descricao; this.valor = valor; this.tipo = tipo;
        }
        public String getData() { return data.format(dtf); }
        public LocalDate getDataLocal() { return data; }
        public String getDescricao() { return descricao; }
        public String getValorFormatado() { 
            if (tipo.equals("DESCONTO")) return "(-) " + nf.format(valor);
            return nf.format(valor); 
        }
    }
}