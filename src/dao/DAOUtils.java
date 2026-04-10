package dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class DAOUtils {
    private DAOUtils() {}

    public static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Retorna o empresa_id atual do TenantContext.
     * Atalho para uso nos DAOs.
     */
    public static int empresaId() {
        return TenantContext.getEmpresaId();
    }

    /**
     * Seta o empresa_id no PreparedStatement na posicao indicada.
     * Retorna a proxima posicao disponivel.
     * 
     * Uso: int pos = DAOUtils.setEmpresa(stmt, 3); // seta na posicao 3, retorna 4
     */
    public static int setEmpresa(PreparedStatement stmt, int pos) throws SQLException {
        stmt.setInt(pos, TenantContext.getEmpresaId());
        return pos + 1;
    }

    /**
     * Fragmento SQL para filtro de tenant.
     * Uso: "SELECT * FROM viagens WHERE " + DAOUtils.TENANT_FILTER + " AND data_viagem = ?"
     */
    public static final String TENANT_FILTER = "empresa_id = ?";

    /**
     * Fragmento SQL para INSERT incluindo empresa_id.
     * Uso: adicionar "empresa_id" na lista de colunas e DAOUtils.empresaId() nos valores.
     */
    public static final String TENANT_COLUMN = "empresa_id";
}
