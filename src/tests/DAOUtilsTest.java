package tests;

import dao.DAOUtils;
import dao.TenantContext;
import org.junit.After;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

/**
 * DR028: Testes unitarios para DAOUtils.
 * Verifica helpers de tenant e null-value handling.
 */
public class DAOUtilsTest {

    @After
    public void cleanup() {
        TenantContext.clear();
        TenantContext.setDefaultEmpresaId(1);
    }

    @Test
    public void nvl_comValor_retornaValor() {
        BigDecimal val = new BigDecimal("123.45");
        assertEquals(val, DAOUtils.nvl(val));
    }

    @Test
    public void nvl_comNull_retornaZero() {
        assertEquals(BigDecimal.ZERO, DAOUtils.nvl(null));
    }

    @Test
    public void nvl_comZero_retornaMesmoZero() {
        assertSame(BigDecimal.ZERO, DAOUtils.nvl(BigDecimal.ZERO));
    }

    @Test
    public void nvl_comValor_retornaMesmaInstancia() {
        BigDecimal val = new BigDecimal("99.99");
        assertSame(val, DAOUtils.nvl(val));
    }

    @Test
    public void empresaId_retornaDoTenantContext() {
        TenantContext.setEmpresaId(42);
        assertEquals(42, DAOUtils.empresaId());
    }

    @Test
    public void empresaId_semSet_retornaDefault() {
        TenantContext.setDefaultEmpresaId(7);
        TenantContext.clear();
        assertEquals(7, DAOUtils.empresaId());
    }

    @Test
    public void empresaId_setEClear_voltaAoDefault() {
        TenantContext.setDefaultEmpresaId(5);
        TenantContext.setEmpresaId(99);
        assertEquals(99, DAOUtils.empresaId());
        TenantContext.clear();
        assertEquals(5, DAOUtils.empresaId());
    }

    @Test
    public void TENANT_FILTER_constante() {
        assertEquals("empresa_id = ?", DAOUtils.TENANT_FILTER);
    }

    @Test
    public void TENANT_COLUMN_constante() {
        assertEquals("empresa_id", DAOUtils.TENANT_COLUMN);
    }
}
