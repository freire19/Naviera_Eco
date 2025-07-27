// src/gui/util/SessaoUsuario.java  (ou src/util/SessaoUsuario.java, dependendo do seu pacote)
package gui.util; // Adapte o nome do pacote para onde você criou o arquivo!

import model.Usuario; // Adapte para o pacote onde sua classe Usuario.java está!

public class SessaoUsuario {
    private static Usuario usuarioLogado;

    public static void setUsuarioLogado(Usuario usuario) {
        SessaoUsuario.usuarioLogado = usuario;
    }

    public static Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public static boolean isUsuarioLogado() {
        return usuarioLogado != null;
    }

    public static void clearSession() {
        SessaoUsuario.usuarioLogado = null;
    }
}