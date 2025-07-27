package tests;

import database.DatabaseConnection;
import java.sql.Connection;

public class TesteConexao {
    public static void main(String[] args) {
        Connection conexao = DatabaseConnection.conectar();
        if (conexao != null) {
            System.out.println("🎉 Teste bem-sucedido: Banco de dados conectado!");
        } else {
            System.out.println("⚠ Erro na conexão!");
        }
    }
}
