package gui;

import dao.PassagemDAO;
import dao.ViagemDAO;
import dao.ConexaoBD;
import model.Passagem;
import model.Viagem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.TableCell;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.List;

// Imports para Impressão A4 e Imagens
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import javax.imageio.ImageIO;

public class ListarPassageirosViagemController implements Initializable {

    @FXML private Label lblViagemAtivaInfo;
    @FXML private TableView<Passagem> tablePassageirosViagem;
    @FXML private TableColumn<Passagem, Integer> colOrdem;
    @FXML private TableColumn<Passagem, String> colNomeCompleto;
    @FXML private TableColumn<Passagem, LocalDate> colDataNascimento;
    @FXML private TableColumn<Passagem, Integer> colIdade;    
    @FXML private TableColumn<Passagem, String> colRG;
    @FXML private TableColumn<Passagem, String> colOrigem;
    @FXML private TableColumn<Passagem, String> colDestino;
    @FXML private Button btnImprimirLista;
    @FXML private Button btnSair;

    private ViagemDAO viagemDAO;
    private PassagemDAO passagemDAO;
    private ObservableList<Passagem> obsListPassageiros;
    
    // Dados da empresa para o cabeçalho do relatório
    private String empNome = "EMPRESA DE NAVEGAÇÃO";
    private String empCnpj = "";
    private String empEndereco = "";
    private String empTelefone = "";
    private String empPathLogo = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        viagemDAO = new ViagemDAO();
        passagemDAO = new PassagemDAO();

        carregarDadosEmpresaCompleto(); 
        configurarTabela();
        carregarListaPassageirosDaViagemAtiva();
    }
    
    private void carregarDadosEmpresaCompleto() {
        try (Connection conn = ConexaoBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT nome_embarcacao, cnpj, endereco, telefone, path_logo FROM configuracao_empresa LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                if(rs.getString("nome_embarcacao") != null) empNome = rs.getString("nome_embarcacao");
                if(rs.getString("cnpj") != null) empCnpj = rs.getString("cnpj");
                if(rs.getString("endereco") != null) empEndereco = rs.getString("endereco");
                if(rs.getString("telefone") != null) empTelefone = rs.getString("telefone");
                if(rs.getString("path_logo") != null) empPathLogo = rs.getString("path_logo");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void configurarTabela() {
        colOrdem.setCellValueFactory(new PropertyValueFactory<>("ordem"));
        colNomeCompleto.setCellValueFactory(new PropertyValueFactory<>("nomePassageiro"));
        colDataNascimento.setCellValueFactory(new PropertyValueFactory<>("dataNascimento"));    
        colIdade.setCellValueFactory(new PropertyValueFactory<>("idade"));    
        colRG.setCellValueFactory(new PropertyValueFactory<>("numeroDoc"));
        colOrigem.setCellValueFactory(new PropertyValueFactory<>("origem"));
        colDestino.setCellValueFactory(new PropertyValueFactory<>("destino"));

        colDataNascimento.setCellFactory(column -> new TableCell<Passagem, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        obsListPassageiros = FXCollections.observableArrayList();
        tablePassageirosViagem.setItems(obsListPassageiros);
    }

    private void carregarListaPassageirosDaViagemAtiva() {
        Viagem viagemAtiva = viagemDAO.buscarViagemAtiva();

        if (viagemAtiva == null) {
            showAlert(AlertType.INFORMATION, "Nenhuma Viagem Ativa", "Não há uma viagem ativa definida.");
            lblViagemAtivaInfo.setText("Nenhuma viagem ativa.");
            obsListPassageiros.clear();
            return;
        }

        String dtSaida = (viagemAtiva.getDataViagem() != null) ? viagemAtiva.getDataViagem().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "--";
        String prevChegada = (viagemAtiva.getDataChegada() != null) ? viagemAtiva.getDataChegada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "--";

        lblViagemAtivaInfo.setText(
            "Embarcação: " + viagemAtiva.getNomeEmbarcacao() +
            " | Saída: " + dtSaida +
            " | Prev. Chegada: " + prevChegada +
            " | Hora: " + viagemAtiva.getHorarioSaidaStr()
        );

        try {
            List<Passagem> passageirosDaViagem = passagemDAO.listarPorViagem(viagemAtiva.getId());
            
            int ordem = 1;
            for (Passagem p : passageirosDaViagem) {
                p.setOrdem(ordem++);
                if (p.getDataNascimento() != null) {
                    p.setIdade(Period.between(p.getDataNascimento(), LocalDate.now()).getYears());
                } else {
                    p.setIdade(0);
                }
            }
            
            obsListPassageiros.setAll(passageirosDaViagem);

        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Erro ao Carregar Lista", "Ocorreu um erro ao carregar a lista de passageiros: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleImprimirLista(ActionEvent event) {
        if (obsListPassageiros.isEmpty()) {
            showAlert(AlertType.WARNING, "Lista Vazia", "Não há passageiros para imprimir.");
            return;
        }
        imprimirRelatorioA4();
    }

    private void imprimirRelatorioA4() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Lista de Passageiros - " + empNome);

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                // --- Configurações Visuais ---
                double width = pageFormat.getImageableWidth();
                double height = pageFormat.getImageableHeight();
                
                Color azulSistema = new Color(0, 51, 102); // RGB para Azul Navy
                Color corFundoZebrado = new Color(235, 240, 250); // Azul bem clarinho para o zebrado

                int margin = 30;
                int y = 30; // Margem topo inicial
                
                // --- Fontes ---
                Font fontEmpresa = new Font("Arial", Font.BOLD, 18);
                Font fontSubTitulo = new Font("Arial", Font.PLAIN, 10);
                Font fontTituloRelatorio = new Font("Arial", Font.BOLD, 16);
                Font fontTableHeader = new Font("Arial", Font.BOLD, 9);
                Font fontRow = new Font("Arial", Font.PLAIN, 9);
                Font fontFooter = new Font("Arial", Font.ITALIC, 8);

                // --- CABEÇALHO ---
                
                // 1. LOGO CENTRALIZADA NO TOPO
                if (empPathLogo != null && !empPathLogo.isEmpty()) {
                    try {
                        File f = new File(empPathLogo);
                        if (f.exists()) {
                            Image logo = ImageIO.read(f);
                            
                            int originalW = logo.getWidth(null);
                            int originalH = logo.getHeight(null);
                            
                            // Altura alvo da logo (60px)
                            int targetH = 60; 
                            // Calcula largura proporcional
                            int targetW = (int) ((double)originalW / originalH * targetH);
                            
                            // Centraliza a imagem no papel
                            int logoX = (int) ((width - targetW) / 2);
                            
                            g2d.drawImage(logo, logoX, y, targetW, targetH, null); 
                            
                            // AQUI FOI ALTERADO: Mais espaço entre a logo e o nome (+25)
                            y += targetH + 25; 
                        }
                    } catch (Exception e) {
                        // Se der erro na logo, apenas ignora
                    }
                } else {
                    y += 10; 
                }

                // 2. DADOS DA EMPRESA (Tudo Centralizado)
                g2d.setColor(azulSistema);
                g2d.setFont(fontEmpresa);
                drawCenteredString(g2d, empNome, width, y);
                y += 18;
                
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(fontSubTitulo);
                
                if (!empEndereco.isEmpty()) {
                    drawCenteredString(g2d, empEndereco, width, y);
                    y += 12;
                }
                
                drawCenteredString(g2d, "CNPJ: " + empCnpj, width, y);
                y += 12;
                
                if (!empTelefone.isEmpty()) {
                    drawCenteredString(g2d, "Tel: " + empTelefone, width, y);
                    y += 12;
                }
                
                y += 20; // Espaço para o título

                // 3. TÍTULO DO RELATÓRIO
                g2d.setColor(Color.BLACK);
                g2d.setFont(fontTituloRelatorio);
                drawCenteredString(g2d, "LISTA DE PASSAGEIROS", width, y);
                y += 20;

                // 4. DADOS DA VIAGEM (Alinhado à Esquerda)
                g2d.setFont(fontSubTitulo);
                String infoViagem = lblViagemAtivaInfo.getText();
                g2d.drawString(infoViagem, margin, y);
                y += 20;

                // --- TABELA ---
                int rowHeight = 18; 
                
                // Definição das colunas
                int[] colX = {
                    margin,             // ORD
                    margin + 35,        // NOME
                    margin + 260,       // DOC/RG
                    margin + 350,       // NASC
                    margin + 420,       // ORIGEM
                    margin + 490        // DESTINO
                };
                
                String[] colNames = {"ORD", "NOME DO PASSAGEIRO", "DOC/RG", "NASC.", "ORIGEM", "DESTINO"};
                
                // Fundo AZUL do Cabeçalho
                g2d.setColor(azulSistema);
                g2d.fillRect(margin, y - 12, (int)width - (margin*2), 16); 
                
                // Texto BRANCO do Cabeçalho
                g2d.setColor(Color.WHITE);
                g2d.setFont(fontTableHeader);
                for(int i=0; i<colNames.length; i++) {
                    g2d.drawString(colNames[i], colX[i], y);
                }
                
                y += 5;

                // --- PAGINAÇÃO E LINHAS ---
                int itemsPerPage = (int) ((height - y - 40) / rowHeight); 
                int startIndex = pageIndex * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, obsListPassageiros.size());

                if (startIndex >= obsListPassageiros.size()) {
                    return NO_SUCH_PAGE;
                }

                g2d.setFont(fontRow);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                for (int i = startIndex; i < endIndex; i++) {
                    y += rowHeight; 
                    
                    Passagem p = obsListPassageiros.get(i);
                    
                    // ZEBRADO (Sem linhas verticais/horizontais desenhadas, apenas o fundo)
                    if (i % 2 != 0) {
                        g2d.setColor(corFundoZebrado);
                        g2d.fillRect(margin, y - 12, (int)width - (margin*2), rowHeight);
                    }
                    
                    g2d.setColor(Color.BLACK); // Texto preto

                    // Preenche colunas
                    g2d.drawString(String.valueOf(p.getOrdem()), colX[0], y);
                    
                    String nome = p.getNomePassageiro() != null ? p.getNomePassageiro().toUpperCase() : "";
                    if (nome.length() > 38) nome = nome.substring(0, 38) + "..."; 
                    g2d.drawString(nome, colX[1], y);
                    
                    g2d.drawString(p.getNumeroDoc() != null ? p.getNumeroDoc() : "", colX[2], y);
                    
                    g2d.drawString(p.getDataNascimento() != null ? dtf.format(p.getDataNascimento()) : "", colX[3], y);
                    
                    String origem = p.getOrigem() != null ? p.getOrigem() : "";
                    if(origem.length() > 12) origem = origem.substring(0, 12);
                    g2d.drawString(origem, colX[4], y);
                    
                    String destino = p.getDestino() != null ? p.getDestino() : "";
                    if(destino.length() > 12) destino = destino.substring(0, 12);
                    g2d.drawString(destino, colX[5], y);

                    // REMOVIDO: g2d.drawLine(...) -> Fica apenas o zebrado limpo
                }

                // --- RODAPÉ ---
                y = (int) height - 30;
                g2d.setFont(fontFooter);
                g2d.setColor(azulSistema);
                // Mantive apenas essa linha grossa do rodapé para fechar a página
                g2d.drawLine(margin, y-15, (int)width-margin, y-15);
                
                g2d.drawString("Impresso por: " + System.getProperty("user.name"), margin, y);
                g2d.drawString("Data: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), margin + 200, y);
                g2d.drawString("Página " + (pageIndex + 1), (int)width - margin - 50, y);

                return PAGE_EXISTS;
            }
        });

        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
            } catch (PrinterException e) {
                showAlert(AlertType.ERROR, "Erro de Impressão", "Falha ao imprimir: " + e.getMessage());
            }
        }
    }
    
    private void drawCenteredString(Graphics2D g, String text, double pageWidth, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = (int) ((pageWidth - metrics.stringWidth(text)) / 2);
        g.drawString(text, x, y);
    }

    @FXML
    private void handleSair(ActionEvent event) {
        TelaPrincipalController.fecharTelaAtual(btnSair);
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}