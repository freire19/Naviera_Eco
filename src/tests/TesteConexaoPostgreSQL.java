package tests; // IMPORTANTE: Declaração do pacote onde o arquivo está agora

import java.sql.Connection;
import java.sql.SQLException;

public class TesteConexaoPostgreSQL {

    public static void main(String[] args) {
        // Usa ConexaoBD centralizado (config em db.properties)
        Connection conexao = null;

        try {
            System.out.println("Tentando conectar via ConexaoBD (pool)...");
            conexao = dao.ConexaoBD.getConnection();
            System.out.println("----------------------------------------------------------");
            System.out.println("CONEXÃO COM O BANCO DE DADOS ESTABELECIDA COM SUCESSO!");
            System.out.println("----------------------------------------------------------");

            // Se chegou até aqui, a conexão foi bem-sucedida.
            // Você poderia adicionar testes adicionais aqui, como executar uma query simples,
            // mas para um teste inicial de conexão, isso é suficiente.

        } catch (SQLException e) {
            System.err.println("**********************************************************");
            System.err.println("ERRO AO CONECTAR AO BANCO DE DADOS:");
            System.err.println("Verifique se o servidor PostgreSQL está rodando,");
            System.err.println("se o nome do banco de dados, usuário e senha em db.properties estão corretos,");
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