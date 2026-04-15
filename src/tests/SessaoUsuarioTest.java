package tests;

import gui.util.SessaoUsuario;
import model.Usuario;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DR028: Testes unitarios para SessaoUsuario.
 * Verifica login, logout, expiracao e thread safety.
 */
public class SessaoUsuarioTest {

    @After
    public void cleanup() {
        SessaoUsuario.clearSession();
    }

    @Test
    public void semLogin_retornaNull() {
        assertNull(SessaoUsuario.getUsuarioLogado());
    }

    @Test
    public void semLogin_isUsuarioLogado_false() {
        assertFalse(SessaoUsuario.isUsuarioLogado());
    }

    @Test
    public void comLogin_retornaUsuario() {
        Usuario u = new Usuario();
        u.setId(1);
        u.setNomeCompleto("Teste");
        SessaoUsuario.setUsuarioLogado(u);

        assertNotNull(SessaoUsuario.getUsuarioLogado());
        assertEquals(1, SessaoUsuario.getUsuarioLogado().getId());
        assertTrue(SessaoUsuario.isUsuarioLogado());
    }

    @Test
    public void clearSession_limpaUsuario() {
        Usuario u = new Usuario();
        u.setId(1);
        SessaoUsuario.setUsuarioLogado(u);
        SessaoUsuario.clearSession();

        assertNull(SessaoUsuario.getUsuarioLogado());
        assertFalse(SessaoUsuario.isUsuarioLogado());
    }

    @Test
    public void setNull_limpaUsuario() {
        Usuario u = new Usuario();
        u.setId(1);
        SessaoUsuario.setUsuarioLogado(u);
        SessaoUsuario.setUsuarioLogado(null);

        assertNull(SessaoUsuario.getUsuarioLogado());
    }

    @Test
    public void touch_renovaAtividade() {
        Usuario u = new Usuario();
        u.setId(1);
        SessaoUsuario.setUsuarioLogado(u);
        SessaoUsuario.touch();

        assertFalse(SessaoUsuario.isSessionExpired());
    }

    @Test
    public void threadSafety_volatileVisibilidade() throws Exception {
        Usuario u = new Usuario();
        u.setId(42);
        SessaoUsuario.setUsuarioLogado(u);

        final Usuario[] lido = {null};
        Thread t = new Thread(() -> {
            lido[0] = SessaoUsuario.getUsuarioLogado();
        });
        t.start();
        t.join();

        // volatile garante que a outra thread ve o valor
        assertNotNull("Outra thread deve ver o usuario logado (volatile)", lido[0]);
        assertEquals(42, lido[0].getId());
    }
}
