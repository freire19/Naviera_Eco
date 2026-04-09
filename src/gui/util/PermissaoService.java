package gui.util;

import model.Usuario;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.util.Arrays;
import java.util.List;

/**
 * Servico centralizado de verificacao de permissoes.
 * Usa o campo 'funcao' do Usuario logado em SessaoUsuario.
 */
public class PermissaoService {

    // Funcoes com acesso total
    private static final List<String> FUNCOES_ADMIN = Arrays.asList("Administrador", "Gerente");

    // Funcoes com acesso a operacoes financeiras
    private static final List<String> FUNCOES_FINANCEIRO = Arrays.asList("Administrador", "Gerente", "Operador de Caixa");

    // Funcoes com acesso a operacoes de venda/atendimento
    private static final List<String> FUNCOES_OPERACIONAL = Arrays.asList("Administrador", "Gerente", "Operador de Caixa", "Atendente", "Conferente");

    private static String getFuncaoAtual() {
        Usuario u = SessaoUsuario.getUsuarioLogado();
        if (u == null || u.getFuncao() == null) return "";
        return u.getFuncao();
    }

    public static boolean isAdmin() {
        return FUNCOES_ADMIN.contains(getFuncaoAtual());
    }

    public static boolean isFinanceiro() {
        return FUNCOES_FINANCEIRO.contains(getFuncaoAtual());
    }

    public static boolean isOperacional() {
        return FUNCOES_OPERACIONAL.contains(getFuncaoAtual());
    }

    /**
     * Verifica se o usuario tem permissao de admin. Se nao, exibe alerta e retorna false.
     */
    public static boolean exigirAdmin(String operacao) {
        if (isAdmin()) return true;
        negarAcesso(operacao);
        return false;
    }

    /**
     * Verifica se o usuario tem permissao financeira. Se nao, exibe alerta e retorna false.
     */
    public static boolean exigirFinanceiro(String operacao) {
        if (isFinanceiro()) return true;
        negarAcesso(operacao);
        return false;
    }

    /**
     * Verifica se o usuario tem permissao operacional. Se nao, exibe alerta e retorna false.
     */
    public static boolean exigirOperacional(String operacao) {
        if (isOperacional()) return true;
        negarAcesso(operacao);
        return false;
    }

    // DR123: guard de FX thread para evitar IllegalStateException se chamado de bg thread
    private static void negarAcesso(String operacao) {
        Runnable showAlert = () -> {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Acesso Negado");
            alert.setHeaderText("Permissao insuficiente");
            alert.setContentText("Voce nao tem permissao para: " + operacao + "\nContate o administrador do sistema.");
            alert.showAndWait();
        };
        if (javafx.application.Platform.isFxApplicationThread()) {
            showAlert.run();
        } else {
            javafx.application.Platform.runLater(showAlert);
        }
    }
}
