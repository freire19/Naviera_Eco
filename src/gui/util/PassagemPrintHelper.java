package gui.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Passagem;

import util.AppLogger;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * Helper para impressao de bilhetes de passagem (termica).
 * Extraido de VenderPassagemController para reduzir tamanho do controller.
 */
public class PassagemPrintHelper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Dados da empresa necessarios para imprimir o bilhete. */
    public static class EmpresaInfo {
        public final String embarcacao;
        public final String proprietario;
        public final String cnpj;
        public final String telefone;
        public final String frase;
        public final String recomendacoes;
        public final String pathLogo;

        public EmpresaInfo(String embarcacao, String proprietario, String cnpj,
                           String telefone, String frase, String recomendacoes, String pathLogo) {
            this.embarcacao = embarcacao;
            this.proprietario = proprietario;
            this.cnpj = cnpj;
            this.telefone = telefone;
            this.frase = frase;
            this.recomendacoes = recomendacoes;
            this.pathLogo = pathLogo;
        }
    }

    /**
     * Mostra preview do bilhete com botao de imprimir.
     */
    public static void mostrarPreviewImpressao(Passagem p, EmpresaInfo emp, LocalDate dataChegada, Window owner) {
        Stage previewStage = new Stage();
        previewStage.setTitle("Visualizacao do Bilhete (Impressora Termica)");
        previewStage.initModality(Modality.WINDOW_MODAL);
        previewStage.initOwner(owner);

        Node bilheteNode = criarLayoutBilheteVisual(p, emp, dataChegada);

        Button btnConfirmar = new Button("IMPRIMIR AGORA");
        btnConfirmar.setStyle("-fx-font-size: 16px; -fx-background-color: #059669; -fx-text-fill: white; -fx-padding: 10px 20px;");
        btnConfirmar.setOnAction(e -> {
            Node nodeParaImpressora = criarLayoutBilheteVisual(p, emp, dataChegada);
            imprimirNodeReal(nodeParaImpressora);
            previewStage.close();
        });

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-font-size: 14px;");
        btnCancelar.setOnAction(e -> previewStage.close());

        HBox buttonBox = new HBox(20, btnConfirmar, btnCancelar);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f0f0;");
        root.getChildren().addAll(new Text("Confira os dados abaixo antes de imprimir:"), bilheteNode, buttonBox);

        Scene scene = new Scene(root, 700, 400);
        previewStage.setScene(scene);
        previewStage.show();
    }

    /**
     * Cria o layout visual do bilhete para preview e impressao.
     */
    public static Node criarLayoutBilheteVisual(Passagem p, EmpresaInfo emp, LocalDate dataChegada) {
        double logicHeight = 230;
        double logicWidth = 600;

        Font fontHeaderBarco = Font.font("Arial", FontWeight.BOLD, 22);
        Font fontHeaderDono = Font.font("Arial", FontWeight.NORMAL, 14);
        Font fontHeaderCNPJ = Font.font("Arial", FontWeight.NORMAL, 12);
        Font fontHeaderPhrase = Font.font("Arial", FontWeight.NORMAL, 11);
        Font fontHeaderSmall = Font.font("Arial", FontWeight.NORMAL, 10);
        Font fontSectionTitle = Font.font("Arial", FontWeight.BOLD, 11);
        Font fontData = Font.font("Arial", FontWeight.BOLD, 12);
        Font fontLabel = Font.font("Arial", FontWeight.NORMAL, 11);
        Font fontValores = Font.font("Courier New", FontWeight.BOLD, 12);
        Font fontBilheteNum = Font.font("Arial", FontWeight.BOLD, 26);
        Font fontSituacao = Font.font("Arial", FontWeight.BOLD, 20);
        Font fontTotalBig = Font.font("Arial", FontWeight.BOLD, 16);

        HBox mainLayout = new HBox(5);
        mainLayout.setPadding(new Insets(5));
        mainLayout.setPrefSize(logicWidth, logicHeight);
        mainLayout.setMaxSize(logicWidth, logicHeight);
        mainLayout.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2;");

        VBox leftPanel = new VBox(2);
        leftPanel.setPrefWidth(logicWidth * 0.65);

        // --- CABECALHO ---
        VBox headerContent = new VBox(0);
        headerContent.setAlignment(Pos.CENTER_LEFT);

        Text txtBarco = new Text(emp.embarcacao);
        txtBarco.setFont(fontHeaderBarco);
        Text txtDono = new Text(emp.proprietario);
        txtDono.setFont(fontHeaderDono);
        txtDono.setFill(Color.BLACK);
        Text txtCnpjTel = new Text("CNPJ: " + emp.cnpj + " | " + emp.telefone);
        txtCnpjTel.setFont(fontHeaderCNPJ);
        Text txtFrase = new Text(emp.frase);
        txtFrase.setFont(fontHeaderPhrase);
        txtFrase.setFill(Color.BLACK);
        headerContent.getChildren().addAll(txtBarco, txtDono, txtCnpjTel, txtFrase);

        HBox headerBox = new HBox(5);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        if (emp.pathLogo != null && !emp.pathLogo.isEmpty()) {
            try {
                File f = new File(emp.pathLogo);
                if (f.exists()) {
                    ImageView logo = new ImageView(ImageCache.get(emp.pathLogo)); // DP049: usa cache
                    logo.setFitWidth(70);
                    logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                }
            } catch (Exception e) {
                AppLogger.warn("PassagemPrintHelper", "Erro ao carregar logo: " + e.getMessage());
            }
        }
        headerBox.getChildren().add(headerContent);

        VBox headerContainer = new VBox(headerBox);
        headerContainer.setBorder(new Border(new BorderStroke(
                Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 0, 2.5, 0))));
        headerContainer.setPadding(new Insets(0, 0, 4, 0));
        leftPanel.getChildren().add(headerContainer);

        // --- VIAGEM ---
        VBox tripBox = createSectionBox("VIAGEM", fontSectionTitle, leftPanel.getPrefWidth());
        String prevChegada = (dataChegada != null) ? DATE_FMT.format(dataChegada) : "??";
        tripBox.getChildren().addAll(
                criarLinhaDupla("De:", p.getOrigem(), fontLabel, fontData),
                criarLinhaDupla("Para:", p.getDestino(), fontLabel, fontData),
                new HBox(10,
                        criarLinhaDupla("Data:", DATE_FMT.format(p.getDataViagem()), fontLabel, fontData),
                        criarLinhaDupla("Prev:", prevChegada, fontLabel, fontData)),
                new HBox(10,
                        criarLinhaDupla("Acom.:", p.getAcomodacao(), fontLabel, fontData),
                        criarLinhaDupla("Agente:", (p.getAgenteAux() != null ? p.getAgenteAux() : "--"), fontLabel, fontData))
        );
        leftPanel.getChildren().add(tripBox);

        // --- PASSAGEIRO ---
        VBox passBox = createSectionBox("PASSAGEIRO", fontSectionTitle, leftPanel.getPrefWidth());
        String idade = (p.getIdade() > 0 ? p.getIdade() + "a" : "--");
        String nascimento = (p.getDataNascimento() != null) ? DATE_FMT.format(p.getDataNascimento()) : "--";
        String nacionalidade = (p.getNacionalidade() != null) ? p.getNacionalidade() : "BR";
        passBox.getChildren().addAll(
                criarLinhaDupla("Nome:", p.getNomePassageiro(), fontLabel, fontData),
                new HBox(10,
                        criarLinhaDupla("Doc:", p.getNumeroDoc(), fontLabel, fontData),
                        criarLinhaDupla("Nac:", nacionalidade, fontLabel, fontData)),
                new HBox(10,
                        criarLinhaDupla("DN:", nascimento, fontLabel, fontData),
                        criarLinhaDupla("Id:", idade, fontLabel, fontData),
                        criarLinhaDupla("Sx:", (p.getSexo() != null ? p.getSexo() : "--"), fontLabel, fontData))
        );
        leftPanel.getChildren().add(passBox);

        // --- PAGAMENTO ---
        HBox finBox = new HBox(5);
        VBox tarBox = createSectionBox("TARIFAS", fontSectionTitle, leftPanel.getPrefWidth() / 2);
        tarBox.getChildren().addAll(
                criarLinhaValor("Alim:", p.getValorAlimentacao(), fontValores),
                criarLinhaValor("Transp:", p.getValorTransporte(), fontValores),
                criarLinhaValor("Carga:", p.getValorCargas(), fontValores)
        );

        VBox payBox = createSectionBox("PAGAMENTO", fontSectionTitle, leftPanel.getPrefWidth() / 2);
        BigDecimal total = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        BigDecimal desconto = p.getValorDesconto() != null ? p.getValorDesconto() : BigDecimal.ZERO;
        BigDecimal pago = p.getValorPago() != null ? p.getValorPago() : BigDecimal.ZERO;
        BigDecimal troco = BigDecimal.ZERO;
        BigDecimal falta = BigDecimal.ZERO;
        BigDecimal aPagar = total.subtract(desconto);
        if (pago.compareTo(aPagar) > 0) troco = pago.subtract(aPagar);
        else if (pago.compareTo(aPagar) < 0) falta = aPagar.subtract(pago);

        payBox.getChildren().add(criarLinhaValor("TOTAL:", total, fontTotalBig));
        if (desconto.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Desc.:", desconto, fontValores));
        payBox.getChildren().add(criarLinhaValor("Pago:", pago, fontValores));
        if (troco.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Troco:", troco, fontValores));
        if (falta.compareTo(BigDecimal.ZERO) > 0) payBox.getChildren().add(criarLinhaValor("Falta:", falta, fontValores));

        String formaPagtoTexto = (falta.compareTo(BigDecimal.ZERO) <= 0) ? "A VISTA" : "PENDENTE";
        payBox.getChildren().add(criarLinhaDupla("Situacao:", formaPagtoTexto, fontLabel, fontData));

        finBox.getChildren().addAll(tarBox, payBox);
        leftPanel.getChildren().add(finBox);

        leftPanel.getChildren().add(new Text("Emissao: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))) {{
            setFont(fontHeaderSmall);
        }});

        // --- RIGHT PANEL ---
        VBox rightPanel = new VBox(5);
        rightPanel.setPrefWidth(logicWidth * 0.35 - 10);

        VBox bilBox = new VBox(2);
        bilBox.setAlignment(Pos.CENTER);
        bilBox.setStyle("-fx-border-color: black; -fx-background-color: #eeeeee; -fx-padding: 5; -fx-border-width: 2;");
        Text txtBil = new Text("BILHETE\n" + p.getNumBilhete());
        txtBil.setFont(fontBilheteNum);
        txtBil.setTextAlignment(TextAlignment.CENTER);

        String situacao = "PENDENTE";
        Color cor = Color.web("#DC2626");
        if (p.getDevedor() != null && p.getDevedor().compareTo(BigDecimal.ZERO) <= 0) {
            situacao = "PAGO";
            cor = Color.web("#059669");
        }
        Text txtSit = new Text(situacao);
        txtSit.setFont(fontSituacao);
        txtSit.setFill(cor);
        bilBox.getChildren().addAll(txtBil, txtSit);
        rightPanel.getChildren().add(bilBox);

        VBox recBox = createSectionBox("AVISOS", Font.font("Arial", FontWeight.BOLD, 11), rightPanel.getPrefWidth());
        VBox.setVgrow(recBox, Priority.ALWAYS);
        Text txtRec = new Text(emp.recomendacoes != null ? emp.recomendacoes : "");
        txtRec.setFont(Font.font("Arial", 10));
        txtRec.setWrappingWidth(rightPanel.getPrefWidth() - 5);
        recBox.getChildren().add(txtRec);
        rightPanel.getChildren().add(recBox);

        mainLayout.getChildren().addAll(leftPanel, rightPanel);
        return mainLayout;
    }

    /**
     * Imprime um Node JavaFX na impressora termica (background thread).
     */
    public static void imprimirNodeReal(Node nodeParaImpressora) {
        try {
            nodeParaImpressora.setStyle("-fx-background-color: white; -fx-font-family: 'Arial'; -fx-font-size: 11px;");
            Group root = new Group(nodeParaImpressora);
            Scene tempScene = new Scene(root);
            root.applyCss();
            root.layout();

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            WritableImage fxImage = nodeParaImpressora.snapshot(params, null);

            BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);

            if (bufferedImage.getWidth() > bufferedImage.getHeight()) {
                bufferedImage = rotateImage90(bufferedImage);
            }

            java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
            PrintService service = PrintServiceLookup.lookupDefaultPrintService();
            if (service != null) job.setPrintService(service);

            java.awt.print.PageFormat pf = job.defaultPage();
            java.awt.print.Paper paper = pf.getPaper();
            double finalWidth = bufferedImage.getWidth();
            double finalHeight = bufferedImage.getHeight();
            paper.setSize(finalWidth + 10, finalHeight + 50);
            paper.setImageableArea(0, 0, finalWidth + 10, finalHeight + 50);
            pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);
            pf.setPaper(paper);

            final BufferedImage imageToPrint = bufferedImage;
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                double pageWidth = pageFormat.getImageableWidth();
                double scale = 1.0;
                if (imageToPrint.getWidth() > pageWidth) scale = pageWidth / imageToPrint.getWidth();
                g2d.scale(scale, scale);
                g2d.drawImage(imageToPrint, 0, 0, null);
                return Printable.PAGE_EXISTS;
            }, pf);

            // DP016: impressao em background para nao bloquear UI
            final java.awt.print.PrinterJob finalJob = job;
            Thread printThread = new Thread(() -> {
                try {
                    finalJob.print();
                } catch (Exception ex) {
                    AppLogger.error("PassagemPrintHelper", ex.getMessage(), ex);
                    javafx.application.Platform.runLater(() ->
                            AlertHelper.show(AlertType.ERROR, "Erro de Impressão", "Falha ao imprimir."));
                }
            });
            printThread.setDaemon(true);
            printThread.start();

        } catch (Exception e) {
            AppLogger.error("PassagemPrintHelper", e.getMessage(), e);
            AlertHelper.show(AlertType.ERROR, "Erro de Impressao", "Falha ao imprimir: " + e.getMessage());
        }
    }

    // --- Helpers de layout ---

    static VBox createSectionBox(String title, Font fontTitle, double width) {
        VBox box = new VBox(1);
        box.setPrefWidth(width);
        box.setBorder(new Border(new BorderStroke(
                Color.web("#999999"), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 0, 0.5, 0))));
        box.setPadding(new Insets(2));
        Text txtTitle = new Text(title);
        txtTitle.setFont(fontTitle);
        box.getChildren().add(txtTitle);
        return box;
    }

    static HBox criarLinhaDupla(String label, String valor, Font fontLbl, Font fontVal) {
        HBox line = new HBox(5);
        Text txtLbl = new Text(label);
        txtLbl.setFont(fontLbl);
        Text txtVal = new Text(valor != null ? valor : "");
        txtVal.setFont(fontVal);
        line.getChildren().addAll(txtLbl, txtVal);
        return line;
    }

    static HBox criarLinhaValor(String label, BigDecimal val, Font fontVal) {
        HBox line = new HBox(5);
        Text txtLbl = new Text(label);
        txtLbl.setFont(Font.font("Arial", 12));
        Text txtVal = new Text(val != null ? String.format("R$ %,.2f", val) : "R$ 0,00");
        txtVal.setFont(fontVal);
        line.getChildren().addAll(txtLbl, txtVal);
        return line;
    }

    private static BufferedImage rotateImage90(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage newImage = new BufferedImage(height, width, img.getType());
        Graphics2D g = newImage.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((height - width) / 2.0, (width - height) / 2.0);
        at.rotate(Math.toRadians(90), width / 2.0, height / 2.0);
        g.setTransform(at);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return newImage;
    }
}
