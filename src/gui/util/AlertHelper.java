package gui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import util.AppLogger;

/**
 * Utilitario centralizado para exibicao de alertas JavaFX.
 * Substitui as 26+ copias de showAlert() espalhadas pelos controllers.
 */
public class AlertHelper {

    /**
     * Exibe um alerta com tipo, titulo e mensagem.
     * Seguro para chamar de qualquer thread (usa Platform.runLater se necessario).
     */
    public static void show(AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            exibir(type, title, message);
        } else {
            Platform.runLater(() -> exibir(type, title, message));
        }
    }

    /**
     * Exibe um alerta de INFORMATION com mensagem simples.
     */
    public static void info(String message) {
        show(AlertType.INFORMATION, "Informacao", message);
    }

    /**
     * Exibe um alerta de WARNING com mensagem simples.
     */
    public static void warn(String message) {
        show(AlertType.WARNING, "Aviso", message);
    }

    /**
     * Exibe um alerta de ERROR com mensagem simples.
     */
    public static void error(String message) {
        show(AlertType.ERROR, "Erro", message);
    }

    /**
     * Exibe um alerta de ERROR com titulo customizado.
     */
    public static void error(String title, String message) {
        show(AlertType.ERROR, title, message);
    }

    /**
     * D017: Exibe erro sem expor detalhes internos (SQL, stack traces) ao usuario.
     * Loga o erro real em stderr e mostra mensagem generica ao usuario.
     */
    public static void errorSafe(String contexto, Exception e) {
        // DR131: registrar no log de erros alem de imprimir em stderr
        LogService.registrarErro(contexto, e);
        AppLogger.warn("AlertHelper", "Erro em " + contexto + ": " + e.getMessage());
        show(AlertType.ERROR, "Erro", "Ocorreu um erro ao " + contexto.toLowerCase() + ". Tente novamente ou contate o suporte.");
    }

    // Aliases compat usados em controllers mais antigos.
    public static void showInfo(String message) { info(message); }
    public static void showWarning(String message) { warn(message); }

    /**
     * Exibe um alerta de confirmacao (OK/Cancelar). Retorna true se usuario confirmou.
     */
    public static boolean showConfirmation(String message) {
        if (!Platform.isFxApplicationThread()) {
            final boolean[] ret = new boolean[]{false};
            final Object lock = new Object();
            Platform.runLater(() -> {
                synchronized (lock) {
                    ret[0] = exibirConfirmacao(message);
                    lock.notifyAll();
                }
            });
            synchronized (lock) {
                try { lock.wait(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            return ret[0];
        }
        return exibirConfirmacao(message);
    }

    private static boolean exibirConfirmacao(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmacao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        java.util.Optional<javafx.scene.control.ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == javafx.scene.control.ButtonType.OK;
    }

    private static void exibir(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
