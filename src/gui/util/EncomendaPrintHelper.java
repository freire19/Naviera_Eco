package gui.util;

import dao.EmpresaDAO;
import dao.EncomendaItemDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import model.Encomenda;
import model.EncomendaItem;
import model.Empresa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Helper para impressao de cupom termico de encomenda.
 * Extraido de InserirEncomendaController.
 */
public class EncomendaPrintHelper {

    /**
     * Imprime cupom termico para uma encomenda.
     * DP036: aceita empresa e itens pre-carregados para evitar DB no FX thread.
     */
    public static void imprimirCupomTermico(Encomenda encomenda) {
        // DP036: pre-carregar dados do banco em background antes de montar UI
        Thread bg = new Thread(() -> {
            EmpresaDAO empresaDAO = new EmpresaDAO();
            Empresa empresa = empresaDAO.buscarPorId(EmpresaDAO.ID_EMPRESA_PRINCIPAL);
            EncomendaItemDAO encomendaItemDAO = new EncomendaItemDAO();
            List<EncomendaItem> itens = encomendaItemDAO.listarPorIdEncomenda(encomenda.getId());
            javafx.application.Platform.runLater(() -> imprimirCupomTermicoInterno(encomenda, empresa, itens));
        });
        bg.setDaemon(true);
        bg.start();
    }

    private static void imprimirCupomTermicoInterno(Encomenda encomenda, Empresa empresa, List<EncomendaItem> itens) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            AlertHelper.show(AlertType.ERROR, "Erro", "Nenhuma impressora encontrada.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;

        javafx.print.PageLayout pageLayout = printer.createPageLayout(
                printer.getDefaultPageLayout().getPaper(),
                javafx.print.PageOrientation.PORTRAIT,
                Printer.MarginType.HARDWARE_MINIMUM);
        job.getJobSettings().setPageLayout(pageLayout);

        double larguraBase = 270;
        VBox root = new VBox(0);
        root.setPadding(new Insets(0, 0, 0, 2));
        root.setPrefWidth(larguraBase);
        root.setMaxWidth(larguraBase);
        root.setAlignment(Pos.TOP_LEFT);

        // --- HEADER ---
        VBox headerBox = new VBox(2);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPrefWidth(larguraBase);
        if (empresa != null) {
            if (empresa.getCaminhoFoto() != null && !empresa.getCaminhoFoto().isEmpty()) {
                try {
                    ImageView logo = new ImageView(ImageCache.get(empresa.getCaminhoFoto()));
                    logo.setFitWidth(50);
                    logo.setPreserveRatio(true);
                    headerBox.getChildren().add(logo);
                } catch (Exception e) { /* logo opcional */ }
            }
            Label lblEmpresa = new Label(empresa.getEmbarcacao() != null ? empresa.getEmbarcacao() : "EMBARCAÇÃO");
            lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Courier New'; -fx-text-fill: black;");
            String dados = "";
            if (empresa.getCnpj() != null) dados += "CNPJ: " + empresa.getCnpj() + "\n";
            if (empresa.getTelefone() != null) dados += "Tel: " + empresa.getTelefone() + "\n";
            if (empresa.getEndereco() != null && !empresa.getEndereco().isEmpty()) dados += empresa.getEndereco();
            Label lblDados = new Label(dados);
            lblDados.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
            lblDados.setTextAlignment(TextAlignment.CENTER);
            lblDados.setWrapText(true);
            headerBox.getChildren().addAll(lblEmpresa, lblDados);
        }
        root.getChildren().add(headerBox);

        // --- TITULO ---
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(10, 0, 5, 0));
        Label lblTitulo = new Label("RECIBO DE ENCOMENDA");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        Label lblNum = new Label("Nº " + encomenda.getNumeroEncomenda());
        lblNum.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-border-color: black; -fx-border-width: 2px; -fx-padding: 3px 15px 3px 15px; -fx-text-fill: black;");
        infoBox.getChildren().addAll(lblTitulo, lblNum);
        root.getChildren().add(infoBox);

        // --- CLIENTES ---
        VBox boxClientes = new VBox(2);
        boxClientes.setAlignment(Pos.CENTER_LEFT);
        Label lblRem = new Label("REM: " + encomenda.getRemetente());
        lblRem.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        Label lblDest = new Label("DEST: " + encomenda.getDestinatario());
        lblDest.setStyle("-fx-font-size: 11px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black;");
        Label lblRota = new Label("ROTA: " + (encomenda.getNomeRota() != null ? encomenda.getNomeRota() : "--"));
        lblRota.setStyle("-fx-font-size: 10px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black;");
        lblRem.setWrapText(true);
        lblDest.setWrapText(true);
        boxClientes.getChildren().addAll(lblRem, lblDest, lblRota);
        boxClientes.setPadding(new Insets(5, 0, 5, 0));
        root.getChildren().add(boxClientes);

        // --- TABELA ITENS ---
        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        String styleHeaderBase = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 1 1 0;";
        String styleHeaderLast = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: 900; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 1 0 1 0;";
        String styleCellNormal = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 1 1 0;";
        String styleCellLast = "-fx-padding: 2px; -fx-font-size: 9px; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 0 0 1 0;";
        double wQtd = 30, wDesc = 110, wUnit = 60, wTotal = 70;

        Label hQtd = new Label("QTD"); hQtd.setStyle(styleHeaderBase); hQtd.setPrefWidth(wQtd); hQtd.setAlignment(Pos.CENTER);
        Label hDesc = new Label("DESC."); hDesc.setStyle(styleHeaderBase); hDesc.setPrefWidth(wDesc); hDesc.setAlignment(Pos.CENTER_LEFT);
        Label hUnit = new Label("V.UN"); hUnit.setStyle(styleHeaderBase); hUnit.setPrefWidth(wUnit); hUnit.setAlignment(Pos.CENTER_RIGHT);
        Label hTotal = new Label("TOTAL"); hTotal.setStyle(styleHeaderLast); hTotal.setPrefWidth(wTotal); hTotal.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hQtd, 0, 0); grid.add(hDesc, 1, 0); grid.add(hUnit, 2, 0); grid.add(hTotal, 3, 0);

        // DP036: itens ja pre-carregados em background
        int l = 1;
        for (EncomendaItem i : itens) {
            Label q = new Label(String.valueOf(i.getQuantidade()));
            q.setStyle(styleCellNormal); q.setPrefWidth(wQtd); q.setAlignment(Pos.CENTER);
            Label d = new Label(i.getDescricao());
            d.setStyle(styleCellNormal); d.setPrefWidth(wDesc); d.setWrapText(true); d.setAlignment(Pos.CENTER_LEFT);
            Label vu = new Label(String.format("%,.2f", i.getValorUnitario()));
            vu.setStyle(styleCellNormal); vu.setPrefWidth(wUnit); vu.setAlignment(Pos.CENTER_RIGHT);
            Label vt = new Label(String.format("%,.2f", i.getValorTotal()));
            vt.setStyle(styleCellLast); vt.setPrefWidth(wTotal); vt.setAlignment(Pos.CENTER_RIGHT);
            grid.add(q, 0, l); grid.add(d, 1, l); grid.add(vu, 2, l); grid.add(vt, 3, l);
            l++;
        }
        root.getChildren().add(grid);

        // --- VOLUMES ---
        Label lblVol = new Label("VOLUMES: " + encomenda.getTotalVolumes());
        lblVol.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        Label espaco = new Label(" ");
        espaco.setMinHeight(25);
        root.getChildren().addAll(lblVol, espaco);

        // --- VALORES ---
        VBox boxValores = new VBox(3);
        boxValores.setAlignment(Pos.CENTER_RIGHT);
        boxValores.setPadding(new Insets(0, 5, 0, 0));
        Label lblTotal = new Label("TOTAL: R$ " + String.format("%,.2f", encomenda.getTotalAPagar()));
        lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: black;");
        BigDecimal pg = encomenda.getValorPago() != null ? encomenda.getValorPago() : BigDecimal.ZERO;
        Label lblPago = new Label("PAGO: R$ " + String.format("%,.2f", pg));
        lblPago.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: black;");
        String statusTxt = (pg.compareTo(encomenda.getTotalAPagar()) >= 0 ? "QUITADO" : "PENDENTE");
        Label lblStatus = new Label("STATUS: " + statusTxt);
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: black; -fx-border-width: 1px; -fx-padding: 2px 5px; -fx-text-fill: black;");
        boxValores.getChildren().addAll(lblTotal, lblPago, lblStatus);
        root.getChildren().add(boxValores);

        // --- FOOTER ---
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lblData = new Label("\nEmitido em: " + dataHora + "\n\n__________________________\nAssinatura");
        lblData.setStyle("-fx-font-size: 9px; -fx-text-fill: black;");
        lblData.setTextAlignment(TextAlignment.CENTER);
        VBox footer = new VBox(lblData);
        footer.setAlignment(Pos.CENTER);
        root.getChildren().add(footer);

        // --- ESCALAR E IMPRIMIR ---
        double largImp = pageLayout.getPrintableWidth();
        if (largImp > 0 && largImp < larguraBase) {
            double sc = largImp / larguraBase;
            root.getTransforms().add(new Scale(sc, sc));
        }
        if (job.printPage(root)) job.endJob();
        else AlertHelper.show(AlertType.ERROR, "Erro", "Falha na impressão.");
    }
}
