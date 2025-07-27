package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/sistema_embarcacao";
    private static final String USUARIO = "postgres";
    private static final String SENHA = "123456"; // SENHA ATUALIZADA AQUI

    public static Connection conectar() {
        Connection conexao = null; // Inicializa como null
        try {
            // É uma boa prática carregar o driver explicitamente
            Class.forName("org.postgresql.Driver");
            conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
            System.out.println("Conexao com o PostgreSQL realizada com sucesso! (DatabaseConnection.java)");
            return conexao;
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL não encontrado (DatabaseConnection.java): " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erro ao conectar ao banco (DatabaseConnection.java): " + e.getMessage());
            // Você pode querer lançar a exceção ou tratar de forma diferente
            // dependendo de como o resto do seu sistema espera lidar com falhas de conexão
        }
        return null; // Retorna null se a conexão falhar
    }
}