package tests; // IMPORTANTE: Declaração do pacote onde o arquivo está agora

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TesteConexaoPostgreSQL {

    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/sistema_embarcacao";
        String usuario = "postgres";
        String senha = "123456"; // Sua senha correta

        Connection conexao = null; // Declare fora do try para poder fechar no finally

        try {
            // 1. Carregar o driver JDBC (Opcional para JDBC 4.0+, mas boa prática)
            System.out.println("Tentando carregar o driver PostgreSQL...");
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver PostgreSQL carregado com sucesso.");

            // 2. Estabelecer a conexão
            System.out.println("Tentando conectar ao banco de dados: " + url);
            conexao = DriverManager.getConnection(url, usuario, senha);
            System.out.println("----------------------------------------------------------");
            System.out.println("CONEXÃO COM O BANCO DE DADOS ESTABELECIDA COM SUCESSO!");
            System.out.println("----------------------------------------------------------");

            // Se chegou até aqui, a conexão foi bem-sucedida.
            // Você poderia adicionar testes adicionais aqui, como executar uma query simples,
            // mas para um teste inicial de conexão, isso é suficiente.

        } catch (ClassNotFoundException e) {
            System.err.println("**********************************************************");
            System.err.println("ERRO: O driver JDBC do PostgreSQL não foi encontrado.");
            System.err.println("Verifique se o arquivo JAR do driver PostgreSQL está no Build Path do projeto.");
            System.err.println("**********************************************************");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("**********************************************************");
            System.err.println("ERRO AO CONECTAR AO BANCO DE DADOS:");
            System.err.println("URL: " + url);
            System.err.println("Usuário: " + usuario);
            System.err.println("Verifique se o servidor PostgreSQL está rodando,");
            System.err.println("se o nome do banco de dados, usuário e senha estão corretos,");
            System.err.println("e se a porta (5432) não está bloqueada por um firewall.");
            System.err.println("----------------------------------------------------------");
            System.err.println("Detalhes da Exceção SQL:");
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            System.err.println("**********************************************************");
            e.printStackTrace();
        } finally {
            // 3. Fechar a conexão
            if (conexao != null) {
                try {
                    conexao.close();
                    System.out.println("Conexão com o banco de dados fechada com sucesso.");
                } catch (SQLException e) {
                    System.err.println("Erro ao fechar a conexão:");
                    e.printStackTrace();
                }
            }
        }
    }
}