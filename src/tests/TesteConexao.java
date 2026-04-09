package tests;

import dao.ConexaoBD;
import java.sql.Connection;
import java.sql.SQLException;

public class TesteConexao {
    public static void main(String[] args) {
        try (Connection conexao = ConexaoBD.getConnection()) {
            if (conexao != null) {
                System.out.println("Teste bem-sucedido: Banco de dados conectado!");
            } else {
                System.out.println("Erro na conexao: getConnection() retornou null.");
            }
        } catch (SQLException e) {
            System.out.println("Erro na conexao: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
