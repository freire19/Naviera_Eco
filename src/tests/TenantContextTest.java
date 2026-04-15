package tests;

import dao.TenantContext;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DR028: Testes unitarios para TenantContext.
 * Verifica isolamento de tenant entre threads e fallback para default.
 */
public class TenantContextTest {

    @After
    public void cleanup() {
        TenantContext.clear();
    }

    @Test
    public void defaultEmpresaId_deveRetornarValorConfigurado() {
        TenantContext.setDefaultEmpresaId(42);
        TenantContext.clear();
        assertEquals(42, TenantContext.getEmpresaId());
        // Restaurar default
        TenantContext.setDefaultEmpresaId(1);
    }

    @Test
    public void setEmpresaId_deveOverrideDefault() {
        TenantContext.setDefaultEmpresaId(1);
        TenantContext.setEmpresaId(99);
        assertEquals(99, TenantContext.getEmpresaId());
    }

    @Test
    public void clear_deveVoltarParaDefault() {
        TenantContext.setDefaultEmpresaId(1);
        TenantContext.setEmpresaId(99);
        TenantContext.clear();
        assertEquals(1, TenantContext.getEmpresaId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setEmpresaId_zero_deveLancarExcecao() {
        TenantContext.setEmpresaId(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setEmpresaId_negativo_deveLancarExcecao() {
        TenantContext.setEmpresaId(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultEmpresaId_zero_deveLancarExcecao() {
        TenantContext.setDefaultEmpresaId(0);
    }

    @Test
    public void threadIsolation_threadsDiferentesNaoCompartilham() throws Exception {
        TenantContext.setDefaultEmpresaId(1);
        TenantContext.setEmpresaId(10);

        final int[] outraThreadId = {0};
        Thread t = new Thread(() -> {
            // Outra thread sem setEmpresaId deve usar default
            outraThreadId[0] = TenantContext.getEmpresaId();
        });
        t.start();
        t.join();

        assertEquals("Thread principal deve ter 10", 10, TenantContext.getEmpresaId());
        assertEquals("Outra thread deve ter default (1)", 1, outraThreadId[0]);
    }

    @Test
    public void threadIsolation_setEmUmaThreadNaoAfetaOutra() throws Exception {
        TenantContext.setDefaultEmpresaId(1);
        TenantContext.setEmpresaId(10);

        final int[] outraThreadId = {0};
        Thread t = new Thread(() -> {
            TenantContext.setEmpresaId(20);
            outraThreadId[0] = TenantContext.getEmpresaId();
        });
        t.start();
        t.join();

        assertEquals("Thread principal permanece 10", 10, TenantContext.getEmpresaId());
        assertEquals("Outra thread setou 20", 20, outraThreadId[0]);
    }
}
