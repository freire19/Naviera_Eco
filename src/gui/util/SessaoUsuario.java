// src/gui/util/SessaoUsuario.java  (ou src/util/SessaoUsuario.java, dependendo do seu pacote)
package gui.util; // Adapte o nome do pacote para onde você criou o arquivo!

import model.Usuario; // Adapte para o pacote onde sua classe Usuario.java está!
import util.AppLogger;

public class SessaoUsuario {
    // #DB018: volatile para visibilidade entre threads (SyncClient roda em background)
    private static volatile Usuario usuarioLogado;
    private static volatile long ultimaAtividade = 0;
    private static final long TIMEOUT_MS = 8 * 60 * 60 * 1000; // 8 horas (jornada de trabalho)

    public static void setUsuarioLogado(Usuario usuario) {
        SessaoUsuario.usuarioLogado = usuario;
        SessaoUsuario.ultimaAtividade = System.currentTimeMillis();
    }

    public static Usuario getUsuarioLogado() {
        if (usuarioLogado != null && isSessionExpired()) {
            AppLogger.warn("SessaoUsuario", "Sessao expirada apos " + (TIMEOUT_MS / 3600000) + "h de inatividade.");
            clearSession();
            return null;
        }
        // #024: renovar atividade em cada acesso (garante que uso ativo nao expira)
        if (usuarioLogado != null) touch();
        return usuarioLogado;
    }

    public static boolean isUsuarioLogado() {
        return getUsuarioLogado() != null;
    }

    /** Registra atividade do usuario (chamar em operacoes importantes). */
    public static void touch() {
        if (usuarioLogado != null) ultimaAtividade = System.currentTimeMillis();
    }

    public static boolean isSessionExpired() {
        return ultimaAtividade > 0 && (System.currentTimeMillis() - ultimaAtividade) > TIMEOUT_MS;
    }

    public static void clearSession() {
        SessaoUsuario.usuarioLogado = null;
        SessaoUsuario.ultimaAtividade = 0;
    }
}