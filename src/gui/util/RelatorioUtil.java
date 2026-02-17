package gui.util;

import dao.ConexaoBD;
import dao.EmpresaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import model.Empresa;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * ============================================================================
 * CLASSE UTILITÁRIA PARA RELATÓRIOS - PADRÃO DO SISTEMA
 * ============================================================================
 * 
 * Esta classe centraliza todos os estilos, layouts e configurações de 
 * impressão do sistema. Use esta classe para criar qualquer novo relatório.
 * 
 * COMO USAR:
 * ----------
 * 
 * 1. RELATÓRIO TÉRMICO:
 *    RelatorioUtil util = new RelatorioUtil();
 *    VBox recibo = util.criarReciboTermico("RECIBO DE FRETE", "Nº 123");
 *    // Adicionar conteúdo...
 *    util.imprimirTermico(recibo);
 * 
 * 2. RELATÓRIO A4:
 *    RelatorioUtil util = new RelatorioUtil();
 *    List<VBox> paginas = new ArrayList<>();
 *    VBox pagina = util.criarPaginaA4("RELATÓRIO GERAL", "MANAUS - JUTAÍ", viagem, true);
 *    // Adicionar conteúdo...
 *    util.imprimirA4(paginas);
 * 
 * ============================================================================
 */
public class RelatorioUtil {

    // ========================================================================
    // CONFIGURAÇÕES GLOBAIS - ALTERE AQUI PARA MUDAR TODO O SISTEMA
    // ========================================================================
    
    // CORES
    public static final String COR_AZUL_ESCURO = "#0d47a1";
    public static final String COR_VERDE_ESCURO = "#2e7d32";
    public static final String COR_VERMELHO = "#c62828";
    public static final String COR_CINZA_CLARO = "#f5f5f5";
    public static final String COR_CINZA_MEDIO = "#e0e0e0";
    
    public static final Color COLOR_AZUL = Color.web(COR_AZUL_ESCURO);
    public static final Color COLOR_VERDE = Color.web(COR_VERDE_ESCURO);
    public static final Color COLOR_VERMELHO = Color.web(COR_VERMELHO);
    
    // FONTES - A4
    public static final Font FONT_EMPRESA_A4 = Font.font("Arial", FontWeight.BLACK, 16);
    public static final Font FONT_ROTA_A4 = Font.font("Arial", FontWeight.BLACK, 14);
    public static final Font FONT_DATAS_A4 = Font.font("Arial", FontWeight.BOLD, 11);
    public static final Font FONT_TITULO_BLOCO_A4 = Font.font("Arial", FontWeight.BLACK, 14);
    public static final Font FONT_NORMAL_A4 = Font.font("Arial", FontWeight.NORMAL, 10);
    public static final Font FONT_NEGRITO_A4 = Font.font("Arial", FontWeight.BOLD, 10);
    public static final Font FONT_HEADER_TABELA_A4 = Font.font("Arial", FontWeight.BOLD, 9);
    public static final Font FONT_TOTAIS_TITULO_A4 = Font.font("Arial", FontWeight.BOLD, 12);
    public static final Font FONT_TOTAIS_VALOR_A4 = Font.font("Arial", FontWeight.BOLD, 13);
    
    // FONTES - TÉRMICO
    public static final String FONT_FAMILY_TERMICO = "Courier New";
    public static final int FONT_SIZE_EMPRESA_TERMICO = 13;
    public static final int FONT_SIZE_TITULO_TERMICO = 12;
    public static final int FONT_SIZE_NUMERO_TERMICO = 16;
    public static final int FONT_SIZE_DADOS_TERMICO = 10;
    public static final int FONT_SIZE_TABELA_TERMICO = 9;
    public static final int FONT_SIZE_TOTAL_TERMICO = 13;
    public static final int FONT_SIZE_STATUS_TERMICO = 11;
    public static final int FONT_SIZE_RODAPE_TERMICO = 9;
    
    // LARGURAS - TÉRMICO
    public static final double LARGURA_TERMICA = 270;
    public static final double LARGURA_LOGO_TERMICO = 50;
    
    // ESTILOS CSS - TÉRMICO (para reutilização)
    public static final String STYLE_HEADER_CELL = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
    public static final String STYLE_HEADER_CELL_FIRST = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 1;";
    public static final String STYLE_DATA_CELL = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
    public static final String STYLE_DATA_CELL_FIRST = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 1;";
    
    // FORMATADORES DE DATA
    public static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ARQUIVO DE CONFIGURAÇÃO DE IMPRESSORAS
    private static final String CONFIG_FILE = "impressoras.config";
    private static final String KEY_IMPRESSORA_TERMICA = "impressora.termica";
    private static final String KEY_IMPRESSORA_A4 = "impressora.a4";
    
    // Impressoras salvas (cache em memória)
    private static String nomeImpressoraTermica = null;
    private static String nomeImpressoraA4 = null;
    private static boolean configCarregada = false;
    
    // ========================================================================
    // DADOS DA EMPRESA - CARREGADOS AUTOMATICAMENTE
    // ========================================================================
    
    private Empresa empresa;
    private String nomeEmpresa;
    private String cnpj;
    private String telefone;
    private String endereco;
    private String caminhoLogo;
    
    /**
     * Construtor - Carrega automaticamente os dados da empresa
     */
    public RelatorioUtil() {
        carregarDadosEmpresa();
        carregarConfigImpressoras();
    }
    
    private void carregarDadosEmpresa() {
        try {
            EmpresaDAO empresaDAO = new EmpresaDAO();
            empresa = empresaDAO.buscarPorId(1);
            
            if (empresa != null) {
                nomeEmpresa = empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "SISTEMA";
                cnpj = empresa.getCnpj();
                telefone = empresa.getTelefone();
                endereco = empresa.getEndereco();
                caminhoLogo = empresa.getCaminhoFoto();
            } else {
                // Tentar buscar direto da tabela de configuração
                try (Connection con = ConexaoBD.getConnection();
                     PreparedStatement stmt = con.prepareStatement(
                         "SELECT nome_embarcacao, cnpj, telefone, endereco, path_logo FROM configuracao_empresa LIMIT 1")) {
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        nomeEmpresa = rs.getString("nome_embarcacao");
                        cnpj = rs.getString("cnpj");
                        telefone = rs.getString("telefone");
                        endereco = rs.getString("endereco");
                        caminhoLogo = rs.getString("path_logo");
                    }
                }
            }
        } catch (Exception e) {
            nomeEmpresa = "SISTEMA";
        }
        
        if (nomeEmpresa == null || nomeEmpresa.isEmpty()) {
            nomeEmpresa = "SISTEMA";
        }
    }
    
    // ========================================================================
    // GERENCIAMENTO DE IMPRESSORAS
    // ========================================================================
    
    /**
     * Carrega as configurações de impressoras do arquivo
     */
    private static void carregarConfigImpressoras() {
        if (configCarregada) return;
        
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                nomeImpressoraTermica = props.getProperty(KEY_IMPRESSORA_TERMICA);
                nomeImpressoraA4 = props.getProperty(KEY_IMPRESSORA_A4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        configCarregada = true;
    }
    
    /**
     * Salva as configurações de impressoras no arquivo
     */
    private static void salvarConfigImpressoras() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            if (nomeImpressoraTermica != null) props.setProperty(KEY_IMPRESSORA_TERMICA, nomeImpressoraTermica);
            if (nomeImpressoraA4 != null) props.setProperty(KEY_IMPRESSORA_A4, nomeImpressoraA4);
            props.store(fos, "Configuração de Impressoras do Sistema");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Busca uma impressora pelo nome
     */
    public static Printer buscarImpressoraPorNome(String nome) {
        if (nome == null || nome.isEmpty()) return null;
        
        ObservableSet<Printer> impressoras = Printer.getAllPrinters();
        for (Printer p : impressoras) {
            if (p.getName().equalsIgnoreCase(nome)) {
                return p;
            }
        }
        return null;
    }
    
    /**
     * Obtém a impressora térmica configurada.
     * Se não houver configuração, abre diálogo para seleção.
     */
    public static Printer getImpressoraTermica() {
        carregarConfigImpressoras();
        
        // Tentar usar a impressora salva
        if (nomeImpressoraTermica != null) {
            Printer p = buscarImpressoraPorNome(nomeImpressoraTermica);
            if (p != null) return p;
        }
        
        // Se não encontrou, pedir para o usuário selecionar
        return selecionarImpressora("TÉRMICA (Cupom/Recibo)", true);
    }
    
    /**
     * Obtém a impressora A4 configurada.
     * Se não houver configuração, abre diálogo para seleção.
     */
    public static Printer getImpressoraA4() {
        carregarConfigImpressoras();
        
        // Tentar usar a impressora salva
        if (nomeImpressoraA4 != null) {
            Printer p = buscarImpressoraPorNome(nomeImpressoraA4);
            if (p != null) return p;
        }
        
        // Se não encontrou, pedir para o usuário selecionar
        return selecionarImpressora("A4 (Relatórios)", false);
    }
    
    /**
     * Abre diálogo para selecionar uma impressora
     */
    public static Printer selecionarImpressora(String tipo, boolean isTermica) {
        ObservableSet<Printer> impressoras = Printer.getAllPrinters();
        
        if (impressoras.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Nenhuma impressora encontrada!");
            alert.setContentText("Por favor, instale uma impressora no sistema.");
            alert.showAndWait();
            return null;
        }
        
        // Criar diálogo de seleção
        ChoiceDialog<Printer> dialog = new ChoiceDialog<>(Printer.getDefaultPrinter(), impressoras);
        dialog.setTitle("Selecionar Impressora");
        dialog.setHeaderText("Selecione a impressora " + tipo);
        dialog.setContentText("Impressora:");
        
        Optional<Printer> result = dialog.showAndWait();
        if (result.isPresent()) {
            Printer selecionada = result.get();
            
            // Salvar a escolha
            if (isTermica) {
                nomeImpressoraTermica = selecionada.getName();
            } else {
                nomeImpressoraA4 = selecionada.getName();
            }
            salvarConfigImpressoras();
            
            return selecionada;
        }
        
        return null;
    }
    
    /**
     * Força a reconfiguração das impressoras (abre diálogo para ambas)
     */
    public static void configurarImpressoras() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configurar Impressoras");
        alert.setHeaderText("Você irá configurar as impressoras do sistema.");
        alert.setContentText("Primeiro selecione a impressora TÉRMICA (para cupons/recibos),\ndepois selecione a impressora A4 (para relatórios).");
        alert.showAndWait();
        
        // Resetar configurações
        nomeImpressoraTermica = null;
        nomeImpressoraA4 = null;
        
        // Selecionar impressora térmica
        Printer termica = selecionarImpressora("TÉRMICA (Cupom/Recibo)", true);
        if (termica != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Sucesso");
            ok.setHeaderText("Impressora térmica configurada!");
            ok.setContentText("Impressora: " + termica.getName());
            ok.showAndWait();
        }
        
        // Selecionar impressora A4
        Printer a4 = selecionarImpressora("A4 (Relatórios)", false);
        if (a4 != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Sucesso");
            ok.setHeaderText("Impressora A4 configurada!");
            ok.setContentText("Impressora: " + a4.getName());
            ok.showAndWait();
        }
    }
    
    /**
     * Retorna o nome da impressora térmica configurada
     */
    public static String getNomeImpressoraTermica() {
        carregarConfigImpressoras();
        return nomeImpressoraTermica;
    }
    
    /**
     * Retorna o nome da impressora A4 configurada
     */
    public static String getNomeImpressoraA4() {
        carregarConfigImpressoras();
        return nomeImpressoraA4;
    }
    
    // ========================================================================
    // GETTERS - DADOS DA EMPRESA
    // ========================================================================
    
    public String getNomeEmpresa() { return nomeEmpresa; }
    public String getCnpj() { return cnpj; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
    public String getCaminhoLogo() { return caminhoLogo; }
    public Empresa getEmpresa() { return empresa; }
    
    // ========================================================================
    // MÉTODOS PARA IMPRESSÃO TÉRMICA
    // ========================================================================
    
    /**
     * Cria um recibo térmico completo com cabeçalho padrão
     * 
     * @param titulo Ex: "RECIBO DE FRETE", "RECIBO DE ENCOMENDA"
     * @param numero Ex: "Nº 123" (pode ser null para não exibir)
     * @return VBox com o cabeçalho pronto para adicionar conteúdo
     */
    public VBox criarReciboTermico(String titulo, String numero) {
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(LARGURA_TERMICA);
        root.setMaxWidth(LARGURA_TERMICA);
        root.setAlignment(Pos.TOP_LEFT);
        
        // Adicionar cabeçalho da empresa
        root.getChildren().add(criarCabecalhoTermico());
        
        // Adicionar título e número
        root.getChildren().add(criarTituloTermico(titulo, numero));
        
        return root;
    }
    
    /**
     * Cria o cabeçalho com logo e dados da empresa para impressão térmica
     */
    public VBox criarCabecalhoTermico() {
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(LARGURA_TERMICA);
        
        // Logo
        if (caminhoLogo != null && !caminhoLogo.isEmpty()) {
            try {
                ImageView logo = new ImageView(new Image("file:" + caminhoLogo));
                logo.setFitWidth(LARGURA_LOGO_TERMICO);
                logo.setPreserveRatio(true);
                headerBox.getChildren().add(logo);
            } catch (Exception e) { /* Logo não encontrada */ }
        }
        
        // Nome da empresa
        Label lblEmpresa = new Label(nomeEmpresa);
        lblEmpresa.setStyle(String.format(
            "-fx-font-weight: bold; -fx-font-size: %dpx; -fx-font-family: '%s'; -fx-text-fill: black;",
            FONT_SIZE_EMPRESA_TERMICO, FONT_FAMILY_TERMICO
        ));
        headerBox.getChildren().add(lblEmpresa);
        
        // Dados (CNPJ, Telefone, Endereço)
        StringBuilder dados = new StringBuilder();
        if (cnpj != null && !cnpj.isEmpty()) dados.append("CNPJ: ").append(cnpj).append("\n");
        if (telefone != null && !telefone.isEmpty()) dados.append("Tel: ").append(telefone).append("\n");
        if (endereco != null && !endereco.isEmpty()) dados.append(endereco);
        
        if (dados.length() > 0) {
            Label lblDados = new Label(dados.toString().trim());
            lblDados.setStyle(String.format(
                "-fx-font-size: %dpx; -fx-font-family: '%s'; -fx-font-weight: bold; -fx-text-fill: black;",
                FONT_SIZE_DADOS_TERMICO, FONT_FAMILY_TERMICO
            ));
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().add(lblDados);
        }
        
        return headerBox;
    }
    
    /**
     * Cria o bloco de título e número para impressão térmica
     */
    public VBox criarTituloTermico(String titulo, String numero) {
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(10, 0, 5, 0));
        
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle(String.format(
            "-fx-font-weight: bold; -fx-font-size: %dpx; -fx-text-fill: black;",
            FONT_SIZE_TITULO_TERMICO
        ));
        infoBox.getChildren().add(lblTitulo);
        
        if (numero != null && !numero.isEmpty()) {
            Label lblNum = new Label(numero);
            lblNum.setStyle(String.format(
                "-fx-font-weight: bold; -fx-font-size: %dpx; -fx-border-color: black; -fx-border-width: 2px; -fx-padding: 3px 15px 3px 15px; -fx-text-fill: black;",
                FONT_SIZE_NUMERO_TERMICO
            ));
            infoBox.getChildren().add(lblNum);
        }
        
        return infoBox;
    }
    
    /**
     * Cria um bloco de informações (Remetente, Destinatário, Rota)
     */
    public VBox criarBlocoDadosTermico(String remetente, String destinatario, String rota) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5, 0, 5, 0));
        
        if (remetente != null && !remetente.isEmpty()) {
            Label lbl = new Label("REM: " + remetente);
            lbl.setStyle(String.format(
                "-fx-font-size: %dpx; -fx-font-family: '%s'; -fx-font-weight: bold; -fx-text-fill: black;",
                FONT_SIZE_DADOS_TERMICO, FONT_FAMILY_TERMICO
            ));
            lbl.setWrapText(true);
            box.getChildren().add(lbl);
        }
        
        if (destinatario != null && !destinatario.isEmpty()) {
            Label lbl = new Label("DEST: " + destinatario);
            lbl.setStyle(String.format(
                "-fx-font-size: 11px; -fx-font-family: '%s'; -fx-font-weight: 900; -fx-text-fill: black;",
                FONT_FAMILY_TERMICO
            ));
            lbl.setWrapText(true);
            box.getChildren().add(lbl);
        }
        
        if (rota != null && !rota.isEmpty()) {
            Label lbl = new Label("ROTA: " + rota);
            lbl.setStyle(String.format(
                "-fx-font-size: %dpx; -fx-font-family: '%s'; -fx-font-weight: bold; -fx-text-fill: black;",
                FONT_SIZE_DADOS_TERMICO, FONT_FAMILY_TERMICO
            ));
            box.getChildren().add(lbl);
        }
        
        return box;
    }
    
    /**
     * Cria uma tabela com bordas para impressão térmica
     * 
     * @param headers Array com os títulos das colunas
     * @param larguras Array com as larguras das colunas
     * @return GridPane configurado para adicionar linhas
     */
    public GridPane criarTabelaTermica(String[] headers, double[] larguras) {
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        
        for (int i = 0; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.setStyle(i == 0 ? STYLE_HEADER_CELL_FIRST : STYLE_HEADER_CELL);
            h.setPrefWidth(larguras[i]);
            h.setAlignment(i == 0 ? Pos.CENTER : (i == headers.length - 1 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT));
            grid.add(h, i, 0);
        }
        
        return grid;
    }
    
    /**
     * Adiciona uma linha de dados à tabela térmica
     */
    public void adicionarLinhaTabela(GridPane grid, int linha, String[] valores, double[] larguras) {
        for (int i = 0; i < valores.length; i++) {
            Label cell = new Label(valores[i]);
            cell.setStyle(i == 0 ? STYLE_DATA_CELL_FIRST : STYLE_DATA_CELL);
            cell.setPrefWidth(larguras[i]);
            cell.setWrapText(true);
            cell.setAlignment(i == 0 ? Pos.CENTER : (i == valores.length - 1 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT));
            grid.add(cell, i, linha);
        }
    }
    
    /**
     * Cria o bloco de valores (Total, Pago, Status) para impressão térmica
     */
    public VBox criarBlocoValoresTermico(double total, double pago, String status) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(10, 5, 0, 0));
        
        Label lblTotal = new Label("TOTAL: R$ " + String.format("%,.2f", total));
        lblTotal.setStyle(String.format(
            "-fx-font-size: %dpx; -fx-font-weight: 900; -fx-text-fill: black;",
            FONT_SIZE_TOTAL_TERMICO
        ));
        box.getChildren().add(lblTotal);
        
        Label lblPago = new Label("PAGO: R$ " + String.format("%,.2f", pago));
        lblPago.setStyle(String.format(
            "-fx-font-weight: bold; -fx-font-size: %dpx; -fx-text-fill: black;",
            FONT_SIZE_STATUS_TERMICO
        ));
        box.getChildren().add(lblPago);
        
        if (status != null) {
            Label lblStatus = new Label("STATUS: " + status);
            lblStatus.setStyle(String.format(
                "-fx-font-weight: bold; -fx-font-size: %dpx; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;",
                FONT_SIZE_STATUS_TERMICO
            ));
            box.getChildren().add(lblStatus);
        }
        
        return box;
    }
    
    /**
     * Cria o rodapé com data/hora e assinatura para impressão térmica
     */
    public VBox criarRodapeTermico() {
        String dataHora = LocalDateTime.now().format(FMT_DATA_HORA);
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: black;", FONT_SIZE_RODAPE_TERMICO));
        lblData.setTextAlignment(TextAlignment.CENTER);
        
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        return footer;
    }
    
    /**
     * Imprime um recibo térmico usando a impressora térmica configurada
     */
    public boolean imprimirTermico(VBox conteudo) {
        // Usar a impressora térmica configurada
        Printer printer = getImpressoraTermica();
        if (printer == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Impressão Cancelada");
            alert.setHeaderText("Nenhuma impressora térmica selecionada.");
            alert.setContentText("A impressão foi cancelada.");
            alert.showAndWait();
            return false;
        }
        
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return false;
        
        PageLayout pageLayout = printer.createPageLayout(
            printer.getDefaultPageLayout().getPaper(),
            PageOrientation.PORTRAIT,
            Printer.MarginType.HARDWARE_MINIMUM
        );
        job.getJobSettings().setPageLayout(pageLayout);
        
        // Ajustar escala se necessário
        double largImp = pageLayout.getPrintableWidth();
        if (largImp > 0 && largImp < LARGURA_TERMICA) {
            double sc = largImp / LARGURA_TERMICA;
            conteudo.getTransforms().add(new Scale(sc, sc));
        }
        
        if (job.printPage(conteudo)) {
            job.endJob();
            return true;
        }
        return false;
    }
    
    // ========================================================================
    // MÉTODOS PARA IMPRESSÃO A4
    // ========================================================================
    
    /**
     * Cria uma página A4 com cabeçalho padrão
     * 
     * @param titulo Ex: "RELATÓRIO GERAL", "CONFERÊNCIA DE VIAGEM"
     * @param rota Ex: "MANAUS - JUTAÍ"
     * @param dataSaida Data de saída da viagem
     * @param dataChegada Data de chegada (pode ser null)
     * @param isPrimeiraPagina true para primeira página com cabeçalho completo
     * @param numPagina Número da página
     * @param largura Largura útil da página
     * @param altura Altura útil da página
     * @return VBox com o cabeçalho pronto
     */
    public VBox criarPaginaA4(String titulo, String rota, LocalDate dataSaida, LocalDate dataChegada,
                               boolean isPrimeiraPagina, int numPagina, double largura, double altura) {
        VBox p = new VBox(5);
        p.setPadding(new Insets(5));
        p.setPrefSize(largura, altura);
        p.setMaxWidth(largura);
        p.setStyle("-fx-background-color: white;");
        
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPrefWidth(largura);
        header.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
        
        if (isPrimeiraPagina) {
            // Nome da empresa
            Label lEmp = new Label(nomeEmpresa.toUpperCase());
            lEmp.setFont(FONT_EMPRESA_A4);
            header.getChildren().add(lEmp);
            
            // Rota com destaque
            if (rota != null && !rota.isEmpty()) {
                Label lRota = new Label("ROTA: " + rota);
                lRota.setFont(FONT_ROTA_A4);
                lRota.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 3 10 3 10; -fx-background-radius: 3;");
                header.getChildren().add(lRota);
            }
            
            // Datas
            String dSaida = dataSaida != null ? dataSaida.format(FMT_DATA) : "N/D";
            String dChegada = dataChegada != null ? dataChegada.format(FMT_DATA) : "EM ABERTO";
            Label lDatas = new Label("SAÍDA: " + dSaida + "  |  PREV. CHEGADA: " + dChegada);
            lDatas.setFont(FONT_DATAS_A4);
            header.getChildren().add(lDatas);
            
            // Título do relatório
            Label lTit = new Label(titulo);
            lTit.setFont(FONT_NORMAL_A4);
            lTit.setTextFill(Color.DARKGRAY);
            header.getChildren().add(lTit);
        } else {
            Label lSimples = new Label(titulo + " (Continuação) - Página " + numPagina);
            lSimples.setFont(FONT_NEGRITO_A4);
            header.getChildren().add(lSimples);
        }
        
        p.getChildren().add(header);
        return p;
    }
    
    /**
     * Cria o cabeçalho de um bloco (ex: FRETE 1, ENC. 1)
     */
    public HBox criarHeaderBlocoA4(String numero, String linha1, String linha2, String status, double largura) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(3, 5, 3, 5));
        header.setStyle("-fx-background-color: " + COR_AZUL_ESCURO + ";");
        header.setPrefWidth(largura);
        
        // Número
        Label lNum = new Label(numero);
        lNum.setFont(FONT_TITULO_BLOCO_A4);
        lNum.setTextFill(Color.WHITE);
        lNum.setMinWidth(80);
        header.getChildren().add(lNum);
        
        // Linhas de informação
        VBox boxInfo = new VBox(2);
        boxInfo.setAlignment(Pos.CENTER_LEFT);
        
        if (linha1 != null) {
            Label l1 = new Label(linha1);
            l1.setFont(FONT_NEGRITO_A4);
            l1.setTextFill(Color.WHITE);
            boxInfo.getChildren().add(l1);
        }
        if (linha2 != null) {
            Label l2 = new Label(linha2);
            l2.setFont(FONT_NEGRITO_A4);
            l2.setTextFill(Color.WHITE);
            boxInfo.getChildren().add(l2);
        }
        header.getChildren().add(boxInfo);
        
        // Espaçador
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);
        
        // Status
        if (status != null) {
            Label lSt = new Label(status);
            lSt.setFont(FONT_NEGRITO_A4);
            lSt.setTextFill(status.contains("PAGO") ? Color.LIGHTGREEN : Color.ORANGE);
            header.getChildren().add(lSt);
        }
        
        return header;
    }
    
    /**
     * Cria um grid de itens para A4
     */
    public GridPane criarGridItensA4(double largura) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12, 5, 5, 5));
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPrefWidth(largura);
        
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(8);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(57);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(17);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(18);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);
        
        // Headers
        Label hQtd = new Label("QTD"); hQtd.setFont(FONT_HEADER_TABELA_A4); hQtd.setTextFill(Color.DARKGRAY);
        Label hDesc = new Label("DESCRIÇÃO"); hDesc.setFont(FONT_HEADER_TABELA_A4); hDesc.setTextFill(Color.DARKGRAY);
        Label hUnit = new Label("VL UNIT"); hUnit.setFont(FONT_HEADER_TABELA_A4); hUnit.setTextFill(Color.DARKGRAY); 
        hUnit.setMaxWidth(Double.MAX_VALUE); hUnit.setAlignment(Pos.CENTER_RIGHT);
        Label hTotal = new Label("VL TOTAL"); hTotal.setFont(FONT_HEADER_TABELA_A4); hTotal.setTextFill(Color.DARKGRAY); 
        hTotal.setMaxWidth(Double.MAX_VALUE); hTotal.setAlignment(Pos.CENTER_RIGHT);
        
        grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);
        
        return grid;
    }
    
    /**
     * Adiciona uma linha de item ao grid A4
     */
    public void adicionarItemGridA4(GridPane grid, int linha, String qtd, String desc, String valorUnit, String valorTotal) {
        Label q = new Label(qtd); q.setFont(FONT_NORMAL_A4);
        Label d = new Label(desc); d.setFont(FONT_NORMAL_A4); d.setWrapText(true);
        Label u = new Label(valorUnit); u.setFont(FONT_NORMAL_A4); u.setAlignment(Pos.CENTER_RIGHT); u.setMaxWidth(Double.MAX_VALUE);
        Label t = new Label(valorTotal); t.setFont(FONT_NORMAL_A4); t.setAlignment(Pos.CENTER_RIGHT); t.setMaxWidth(Double.MAX_VALUE);
        
        grid.add(q, 0, linha); grid.add(d, 1, linha); grid.add(u, 2, linha); grid.add(t, 3, linha);
    }
    
    /**
     * Cria o rodapé de um bloco com assinatura e valores
     */
    public HBox criarRodapeBlocoA4(double total, double pago, double devedor) {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(5, 5, 5, 5));
        footer.setStyle("-fx-background-color: " + COR_CINZA_CLARO + ";");
        
        // Assinatura
        Label lAss = new Label("Assinatura: __________________________");
        lAss.setFont(FONT_NORMAL_A4);
        footer.getChildren().add(lAss);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().add(spacer);
        
        // Valores
        VBox boxValores = new VBox(2);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        
        if (devedor > 0.01) {
            Label lTot = new Label("Total: " + formatarMoeda(total));
            lTot.setFont(FONT_NEGRITO_A4);
            boxValores.getChildren().add(lTot);
            
            if (pago > 0.01) {
                Label lPago = new Label("Pago: " + formatarMoeda(pago));
                lPago.setFont(FONT_NEGRITO_A4);
                lPago.setTextFill(Color.FORESTGREEN);
                boxValores.getChildren().add(lPago);
            }
            
            Label lFalta = new Label("A Pagar: " + formatarMoeda(devedor));
            lFalta.setFont(FONT_NEGRITO_A4);
            lFalta.setTextFill(Color.RED);
            boxValores.getChildren().add(lFalta);
        } else {
            Label lTot = new Label("TOTAL: " + formatarMoeda(total) + " (PAGO)");
            lTot.setFont(FONT_NEGRITO_A4);
            lTot.setTextFill(Color.FORESTGREEN);
            boxValores.getChildren().add(lTot);
        }
        
        footer.getChildren().add(boxValores);
        return footer;
    }
    
    /**
     * Cria o bloco de totais gerais
     */
    public VBox criarBlocoTotaisA4(double total, double pago, double devedor, double largura) {
        VBox box = new VBox(8);
        box.setPrefWidth(largura);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 5, 0, 0));
        box.setStyle("-fx-background-color: white;");
        VBox.setMargin(box, new Insets(40, 0, 10, 0));
        
        Label lblTit = new Label("RESUMO FINANCEIRO GERAL");
        lblTit.setFont(FONT_TITULO_BLOCO_A4);
        box.getChildren().add(lblTit);
        
        box.getChildren().add(criarLinhaTotalA4("TOTAL LANÇADO:", total, COLOR_AZUL));
        box.getChildren().add(criarLinhaTotalA4("TOTAL RECEBIDO (PAGO):", pago, COLOR_VERDE));
        box.getChildren().add(criarLinhaTotalA4("TOTAL A RECEBER (FIADO):", devedor, devedor > 0.01 ? COLOR_VERMELHO : Color.BLACK));
        
        return box;
    }
    
    private HBox criarLinhaTotalA4(String titulo, double valor, Color cor) {
        HBox linha = new HBox(10);
        linha.setAlignment(Pos.CENTER_RIGHT);
        
        Label lTit = new Label(titulo);
        lTit.setFont(FONT_TOTAIS_TITULO_A4);
        if (cor.equals(COLOR_VERMELHO)) lTit.setTextFill(COLOR_VERMELHO);
        
        Label lVal = new Label(formatarMoeda(valor));
        lVal.setFont(FONT_TOTAIS_VALOR_A4);
        lVal.setTextFill(cor);
        
        linha.getChildren().addAll(lTit, lVal);
        return linha;
    }
    
    /**
     * Adiciona rodapé à página A4
     */
    public void adicionarRodapeA4(VBox pagina, double largura) {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        pagina.getChildren().add(spacer);
        
        Label l = new Label("Impresso em: " + LocalDateTime.now().format(FMT_DATA_HORA));
        l.setFont(FONT_NORMAL_A4);
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setPrefWidth(largura);
        l.setStyle("-fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
        pagina.getChildren().add(l);
    }
    
    /**
     * Imprime páginas A4 usando a impressora A4 configurada
     * @param showDialog Se true, mostra diálogo para confirmar impressora
     */
    public boolean imprimirA4(List<VBox> paginas, Node parent, boolean showDialog) {
        Printer printer = getImpressoraA4();
        if (printer == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Impressão Cancelada");
            alert.setHeaderText("Nenhuma impressora A4 selecionada.");
            alert.setContentText("A impressão foi cancelada.");
            alert.showAndWait();
            return false;
        }
        
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return false;
        
        // Mostrar diálogo apenas se solicitado
        if (showDialog && !job.showPrintDialog(parent.getScene().getWindow())) {
            return false;
        }
        
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        
        for (VBox p : paginas) {
            job.printPage(pageLayout, p);
        }
        job.endJob();
        return true;
    }
    
    /**
     * Imprime páginas A4 (com diálogo de confirmação)
     */
    public boolean imprimirA4(List<VBox> paginas, Node parent) {
        return imprimirA4(paginas, parent, true);
    }
    
    /**
     * Imprime páginas A4 diretamente (sem diálogo)
     */
    public boolean imprimirA4Direto(List<VBox> paginas) {
        Printer printer = getImpressoraA4();
        if (printer == null) return false;
        
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return false;
        
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        
        for (VBox p : paginas) {
            job.printPage(pageLayout, p);
        }
        job.endJob();
        return true;
    }
    
    /**
     * Obtém as dimensões úteis de uma página A4
     */
    public double[] getDimensoesA4() {
        Printer printer = getImpressoraA4();
        if (printer == null) {
            printer = Printer.getDefaultPrinter();
        }
        if (printer == null) return new double[]{500, 700};
        
        PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        return new double[]{pageLayout.getPrintableWidth() - 40, pageLayout.getPrintableHeight()};
    }
    
    // ========================================================================
    // UTILITÁRIOS
    // ========================================================================
    
    /**
     * Formata valor para moeda brasileira
     */
    public static String formatarMoeda(double valor) {
        return "R$ " + String.format("%,.2f", valor);
    }
    
    /**
     * Formata data
     */
    public static String formatarData(LocalDate data) {
        return data != null ? data.format(FMT_DATA) : "N/D";
    }
    
    /**
     * Formata data e hora
     */
    public static String formatarDataHora(LocalDateTime dataHora) {
        return dataHora != null ? dataHora.format(FMT_DATA_HORA) : "N/D";
    }
    
    /**
     * Retorna o status baseado no valor devedor
     */
    public static String getStatus(double devedor) {
        return devedor <= 0.01 ? "QUITADO" : "PENDENTE";
    }
    
    /**
     * Retorna a cor do status
     */
    public static Color getCorStatus(double devedor) {
        return devedor <= 0.01 ? COLOR_VERDE : COLOR_VERMELHO;
    }
}
