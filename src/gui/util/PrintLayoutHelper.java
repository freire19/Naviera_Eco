package gui.util;

import java.io.File;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import util.AppLogger;

/**
 * DM004: Helpers reutilizaveis para construcao de layouts de impressao.
 *
 * Centraliza os padroes de layout que antes eram duplicados inline em cada
 * controller que implementava impressao:
 *   - Header de empresa (termico 270px e A4)
 *   - Pagina A4 base com header
 *   - Linhas de tabela com zebra
 *   - Header de tabela azul-escuro
 *   - Bloco de assinatura
 *   - Rodape com numero de pagina
 *   - Separadores
 *
 * Todos os metodos sao estaticos — basta importar a classe e chamar diretamente.
 */
public class PrintLayoutHelper {

    // -------------------------------------------------------------------------
    // CONSTANTES DE LAYOUT
    // -------------------------------------------------------------------------

    /** Largura padrao de recibos termicos (px). */
    public static final double THERMAL_WIDTH = 270;

    private static final String FONT_THERMAL = "Courier New";
    private static final String FONT_REPORT  = "Arial";

    /** Azul escuro padrao para headers de tabela e nome da empresa. */
    private static final String COR_HEADER     = "#059669";
    /** Fundo de linha par (zebra clara). */
    private static final String COR_ZEBRA_PAR  = "#f5f5f5";
    /** Fundo de linha impar (zebra branca). */
    private static final String COR_ZEBRA_IMPAR = "white";

    // -------------------------------------------------------------------------
    // HEADERS DE EMPRESA
    // -------------------------------------------------------------------------

    /**
     * Cria header de empresa para recibos termicos (largura {@value #THERMAL_WIDTH}px).
     * Inclui logo centralizado (se disponivel), nome em negrito, CNPJ e endereco.
     *
     * @param nomeEmpresa nome da embarcacao / empresa
     * @param cnpj        CNPJ formatado (pode ser null)
     * @param endereco    endereco (pode ser null)
     * @param pathLogo    caminho absoluto para imagem do logo (pode ser null)
     */
    public static VBox criarHeaderEmpresaTermico(String nomeEmpresa, String cnpj,
                                                  String endereco, String pathLogo) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(THERMAL_WIDTH);
        box.setPadding(new Insets(0, 0, 5, 0));

        adicionarLogo(box, pathLogo, 50);

        Label lblNome = new Label(nomeEmpresa != null ? nomeEmpresa.toUpperCase() : "EMPRESA");
        lblNome.setFont(Font.font(FONT_THERMAL, FontWeight.BOLD, 12));
        box.getChildren().add(lblNome);

        if (cnpj != null && !cnpj.isBlank()) {
            Label lblCnpj = new Label("CNPJ: " + cnpj);
            lblCnpj.setFont(Font.font(FONT_THERMAL, 9));
            box.getChildren().add(lblCnpj);
        }

        if (endereco != null && !endereco.isBlank()) {
            Label lblEnd = new Label(endereco);
            lblEnd.setFont(Font.font(FONT_THERMAL, 9));
            lblEnd.setWrapText(true);
            box.getChildren().add(lblEnd);
        }

        return box;
    }

    /**
     * Cria header de empresa para relatorios A4.
     * Inclui logo, nome em fonte grande negrito, CNPJ e endereco.
     *
     * @param nomeEmpresa nome da embarcacao / empresa
     * @param cnpj        CNPJ formatado (pode ser null)
     * @param endereco    endereco (pode ser null)
     * @param pathLogo    caminho absoluto para imagem do logo (pode ser null)
     */
    public static VBox criarHeaderEmpresaA4(String nomeEmpresa, String cnpj,
                                             String endereco, String pathLogo) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 0, 8, 0));
        box.setStyle("-fx-border-width: 0 0 1 0; -fx-border-color: #cccccc;");

        adicionarLogo(box, pathLogo, 60);

        Label lblNome = new Label(nomeEmpresa != null ? nomeEmpresa.toUpperCase() : "EMPRESA");
        lblNome.setFont(Font.font(FONT_REPORT, FontWeight.BOLD, 18));
        lblNome.setTextFill(Color.web(COR_HEADER));
        box.getChildren().add(lblNome);

        if (cnpj != null && !cnpj.isBlank()) {
            Label lblCnpj = new Label("CNPJ: " + cnpj);
            lblCnpj.setFont(Font.font(FONT_REPORT, 10));
            box.getChildren().add(lblCnpj);
        }

        if (endereco != null && !endereco.isBlank()) {
            Label lblEnd = new Label(endereco);
            lblEnd.setFont(Font.font(FONT_REPORT, 10));
            box.getChildren().add(lblEnd);
        }

        return box;
    }

    // -------------------------------------------------------------------------
    // PAGINA A4
    // -------------------------------------------------------------------------

    /**
     * Cria uma pagina A4 base com fundo branco, header de empresa e titulo.
     * Largura padrao de 555pt (area util A4 portrait com margens padrao).
     *
     * @param nomeEmpresa nome da embarcacao / empresa
     * @param cnpj        CNPJ (pode ser null)
     * @param endereco    endereco (pode ser null)
     * @param pathLogo    caminho para logo (pode ser null)
     * @param titulo      titulo do relatorio (exibido abaixo do header)
     */
    public static VBox criarPaginaA4(String nomeEmpresa, String cnpj,
                                      String endereco, String pathLogo, String titulo) {
        VBox page = new VBox(5);
        page.setPrefWidth(555);
        page.setStyle("-fx-background-color: white; -fx-padding: 15;");
        page.getChildren().add(criarHeaderEmpresaA4(nomeEmpresa, cnpj, endereco, pathLogo));
        if (titulo != null && !titulo.isBlank()) {
            page.getChildren().add(criarTitulo(titulo));
        }
        return page;
    }

    // -------------------------------------------------------------------------
    // TITULO
    // -------------------------------------------------------------------------

    /**
     * Cria label de titulo centralizado para relatorios.
     */
    public static Label criarTitulo(String titulo) {
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font(FONT_REPORT, FontWeight.BOLD, 14));
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        return lbl;
    }

    // -------------------------------------------------------------------------
    // TABELA
    // -------------------------------------------------------------------------

    /**
     * Cria header de tabela com fundo azul escuro e texto branco.
     *
     * @param colunas  nomes das colunas
     * @param larguras largura de cada coluna (deve ter mesmo comprimento que {@code colunas})
     */
    public static HBox criarHeaderTabela(String[] colunas, double[] larguras) {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: " + COR_HEADER + "; -fx-padding: 5;");
        for (int i = 0; i < colunas.length; i++) {
            Label lbl = new Label(colunas[i]);
            lbl.setTextFill(Color.WHITE);
            lbl.setFont(Font.font(FONT_REPORT, FontWeight.BOLD, 10));
            lbl.setPrefWidth(larguras[i]);
            header.getChildren().add(lbl);
        }
        return header;
    }

    /**
     * Cria linha de dados com cor de fundo alternada (zebra).
     *
     * @param valores valores das celulas (null e tratado como "")
     * @param larguras largura de cada coluna
     * @param par      true = fundo claro ({@value #COR_ZEBRA_PAR}), false = branco
     */
    public static HBox criarLinhaTabela(String[] valores, double[] larguras, boolean par) {
        HBox row = new HBox();
        row.setStyle("-fx-background-color: " + (par ? COR_ZEBRA_PAR : COR_ZEBRA_IMPAR)
                     + "; -fx-padding: 3 5;");
        for (int i = 0; i < valores.length; i++) {
            Label lbl = new Label(valores[i] != null ? valores[i] : "");
            lbl.setFont(Font.font(FONT_REPORT, 9));
            lbl.setPrefWidth(larguras[i]);
            row.getChildren().add(lbl);
        }
        return row;
    }

    // -------------------------------------------------------------------------
    // ASSINATURA
    // -------------------------------------------------------------------------

    /**
     * Cria bloco de assinatura (linha tracejada + texto descritivo abaixo).
     *
     * @param textoAbaixo legenda exibida sob a linha (ex: "Responsavel")
     */
    public static VBox criarBlocoAssinatura(String textoAbaixo) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 0, 5, 0));
        box.getChildren().addAll(
            new Text("__________________________________________"),
            criarTextoBold(textoAbaixo, 9)
        );
        return box;
    }

    // -------------------------------------------------------------------------
    // RODAPE
    // -------------------------------------------------------------------------

    /**
     * Cria rodape alinhado a direita com numero de pagina.
     */
    public static HBox criarRodapePagina(int pagina, int totalPaginas) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        Label lbl = new Label("Página " + pagina + " de " + totalPaginas);
        lbl.setFont(Font.font(FONT_REPORT, 8));
        lbl.setTextFill(Color.GRAY);
        footer.getChildren().add(lbl);
        return footer;
    }

    // -------------------------------------------------------------------------
    // HELPERS INTERNOS
    // -------------------------------------------------------------------------

    /**
     * Cria texto em negrito com tamanho especificado.
     */
    public static Text criarTextoBold(String texto, double tamanho) {
        Text t = new Text(texto);
        t.setFont(Font.font(FONT_REPORT, FontWeight.BOLD, tamanho));
        return t;
    }

    /**
     * Cria separador horizontal (solido ou pontilhado).
     *
     * @param pontilhado true para linha pontilhada, false para linha solida
     */
    public static HBox criarSeparador(boolean pontilhado) {
        HBox sep = new HBox();
        sep.setPadding(new Insets(5, 0, 5, 0));
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, THERMAL_WIDTH, 0);
        if (pontilhado) {
            line.getStrokeDashArray().addAll(5d, 5d);
        }
        line.setStroke(Color.GRAY);
        sep.getChildren().add(line);
        return sep;
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /**
     * Adiciona ImageView do logo ao container, se o arquivo existir.
     * Usa {@link ImageCache} para evitar leitura repetida do disco.
     */
    private static void adicionarLogo(VBox container, String pathLogo, double alturaMax) {
        if (pathLogo == null || pathLogo.isBlank()) return;
        try {
            Image img = ImageCache.get(pathLogo);
            if (img == null) {
                // fallback: carrega direto se ImageCache retornar null
                File f = new File(pathLogo);
                if (!f.exists()) return;
                img = new Image(f.toURI().toString());
            }
            ImageView iv = new ImageView(img);
            iv.setFitHeight(alturaMax);
            iv.setPreserveRatio(true);
            container.getChildren().add(iv);
        } catch (Exception e) {
            AppLogger.warn("PrintLayoutHelper", "PrintLayoutHelper: erro ao carregar logo '" + pathLogo + "': " + e.getMessage());
        }
    }
}
