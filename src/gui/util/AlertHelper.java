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

    private static void exibir(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
