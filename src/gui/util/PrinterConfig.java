package gui.util;

import javafx.collections.ObservableSet;
import javafx.print.Printer;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.util.Optional;
import java.util.Properties;
import gui.util.AppLogger;

/**
 * ============================================================================
 * CONFIGURAÇÃO E GERENCIAMENTO DE IMPRESSORAS DO SISTEMA
 * ============================================================================
 *
 * Responsável por:
 * - Carregar/salvar configurações de impressoras do arquivo impressoras.config
 * - Selecionar impressoras por tipo (térmica ou A4)
 * - Caching em memória das impressoras configuradas
 * ============================================================================
 */
public class PrinterConfig {

    // Arquivo de configuração
    private static final String CONFIG_FILE = "impressoras.config";
    private static final String KEY_IMPRESSORA_TERMICA = "impressora.termica";
    private static final String KEY_IMPRESSORA_A4 = "impressora.a4";

    // Impressoras em cache (volatile para visibilidade entre threads — DR124)
    private static volatile String nomeImpressoraTermica = null;
    private static volatile String nomeImpressoraA4 = null;
    private static volatile boolean configCarregada = false;

    private PrinterConfig() { /* utilitária estática */ }

    // ========================================================================
    // CARREGAMENTO / PERSISTÊNCIA
    // ========================================================================

    /**
     * Carrega as configurações de impressoras do arquivo.
     * DR124: synchronized para evitar race em carregamento concorrente.
     */
    public static synchronized void carregarConfigImpressoras() {
        if (configCarregada) return;

        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                nomeImpressoraTermica = props.getProperty(KEY_IMPRESSORA_TERMICA);
                nomeImpressoraA4 = props.getProperty(KEY_IMPRESSORA_A4);
            } catch (IOException e) {
                AppLogger.error("PrinterConfig", e.getMessage(), e);
            }
        }
        configCarregada = true;
    }

    /**
     * Salva as configurações de impressoras no arquivo.
     */
    private static void salvarConfigImpressoras() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            if (nomeImpressoraTermica != null) props.setProperty(KEY_IMPRESSORA_TERMICA, nomeImpressoraTermica);
            if (nomeImpressoraA4 != null) props.setProperty(KEY_IMPRESSORA_A4, nomeImpressoraA4);
            props.store(fos, "Configuração de Impressoras do Sistema");
        } catch (IOException e) {
            AppLogger.error("PrinterConfig", e.getMessage(), e);
        }
    }

    // ========================================================================
    // SELEÇÃO DE IMPRESSORAS
    // ========================================================================

    /**
     * Busca uma impressora instalada pelo nome (case-insensitive).
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

        if (nomeImpressoraTermica != null) {
            Printer p = buscarImpressoraPorNome(nomeImpressoraTermica);
            if (p != null) return p;
        }

        return selecionarImpressora("TÉRMICA (Cupom/Recibo)", true);
    }

    /**
     * Obtém a impressora A4 configurada.
     * Se não houver configuração, abre diálogo para seleção.
     */
    public static Printer getImpressoraA4() {
        carregarConfigImpressoras();

        if (nomeImpressoraA4 != null) {
            Printer p = buscarImpressoraPorNome(nomeImpressoraA4);
            if (p != null) return p;
        }

        return selecionarImpressora("A4 (Relatórios)", false);
    }

    /**
     * Abre diálogo para o usuário selecionar uma impressora.
     */
    public static Printer selecionarImpressora(String tipo, boolean isTermica) {
        try {
            ObservableSet<Printer> impressoras = Printer.getAllPrinters();

            if (impressoras == null || impressoras.isEmpty()) {
                mostrarAlertaNoTopo(Alert.AlertType.ERROR, "Erro",
                    "Nenhuma impressora encontrada!", "Por favor, instale uma impressora no sistema.");
                return null;
            }

            Printer defaultPrinter = Printer.getDefaultPrinter();
            ChoiceDialog<Printer> dialog = new ChoiceDialog<>(defaultPrinter, impressoras);
            dialog.setTitle("Selecionar Impressora");
            dialog.setHeaderText("Selecione a impressora " + tipo);
            dialog.setContentText("Impressora:");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setOnShowing(e -> {
                Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                stage.setAlwaysOnTop(true);
                stage.toFront();
            });

            Optional<Printer> result = dialog.showAndWait();
            if (result.isPresent()) {
                Printer selecionada = result.get();

                if (isTermica) {
                    nomeImpressoraTermica = selecionada.getName();
                } else {
                    nomeImpressoraA4 = selecionada.getName();
                }
                salvarConfigImpressoras();

                return selecionada;
            }
        } catch (Exception e) {
            AppLogger.error("PrinterConfig", e.getMessage(), e);
            mostrarAlertaNoTopo(Alert.AlertType.ERROR, "Erro",
                "Erro ao buscar impressoras", "Detalhes: " + e.getMessage());
        }

        return null;
    }

    /**
     * Força a reconfiguração de ambas as impressoras abrindo diálogos de seleção.
     */
    public static void configurarImpressoras() {
        mostrarAlertaNoTopo(Alert.AlertType.INFORMATION, "Configurar Impressoras",
            "Você irá configurar as impressoras do sistema.",
            "Primeiro selecione a impressora TÉRMICA (para cupons/recibos),\ndepois selecione a impressora A4 (para relatórios).");

        // Resetar configurações
        nomeImpressoraTermica = null;
        nomeImpressoraA4 = null;

        Printer termica = selecionarImpressora("TÉRMICA (Cupom/Recibo)", true);
        if (termica != null) {
            mostrarAlertaNoTopo(Alert.AlertType.INFORMATION, "Sucesso",
                "Impressora térmica configurada!", "Impressora: " + termica.getName());
        }

        Printer a4 = selecionarImpressora("A4 (Relatórios)", false);
        if (a4 != null) {
            mostrarAlertaNoTopo(Alert.AlertType.INFORMATION, "Sucesso",
                "Impressora A4 configurada!", "Impressora: " + a4.getName());
        }
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    /**
     * Retorna o nome da impressora térmica configurada (pode ser null).
     */
    public static String getNomeImpressoraTermica() {
        carregarConfigImpressoras();
        return nomeImpressoraTermica;
    }

    /**
     * Retorna o nome da impressora A4 configurada (pode ser null).
     */
    public static String getNomeImpressoraA4() {
        carregarConfigImpressoras();
        return nomeImpressoraA4;
    }

    // ========================================================================
    // UTILITÁRIO DE UI
    // ========================================================================

    /**
     * Mostra um alerta que sempre fica na frente de todas as janelas.
     */
    public static void mostrarAlertaNoTopo(Alert.AlertType tipo, String titulo, String header, String conteudo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(conteudo);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setOnShowing(e -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.toFront();
        });
        alert.showAndWait();
    }
}
