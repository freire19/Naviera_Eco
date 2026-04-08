package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/sistema_embarcacao";
    private static final String USUARIO = "postgres";
    private static final String SENHA = "123456"; // SENHA ATUALIZADA AQUI

    public static Connection conectar() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL nao encontrado. Verifique se postgresql.jar esta no classpath.", e);
        }
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }
}