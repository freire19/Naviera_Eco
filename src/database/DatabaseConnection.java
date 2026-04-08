package database;

import dao.ConexaoBD;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Delegador para ConexaoBD — mantido para compatibilidade.
 * Toda configuracao e pool estao centralizados em ConexaoBD.
 */
public class DatabaseConnection {

    public static Connection conectar() throws SQLException {
        return ConexaoBD.getConnection();
    }
}
