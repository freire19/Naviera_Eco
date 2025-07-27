package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe responsável por fornecer conexões JDBC ao banco de dados.
 * Agora está configurada para PostgreSQL. É muito importante que você:
 *   1) Preencha URL, USUARIO e SENHA com os dados corretos do seu pgAdmin4.
 *   2) Adicione a dependência do driver PostgreSQL ao classpath do projeto.
 */
public class ConexaoBD {

    // ====== 1) CONFIGURAÇÃO: preencha com os dados do seu PostgreSQL ======
    // Exemplo real (ajuste host, porta, nome_do_banco, usuário e senha):
    //    private static final String URL     = "jdbc:postgresql://localhost:5432/sistema_embarcacoes";
    //    private static final String USUARIO = "postgres";
    //    private static final String SENHA   = "123456";

    private static final String URL     = "jdbc:postgresql://localhost:5432/sistema_embarcacao"; 
    private static final String USUARIO = "postgres";    // ex: "postgres"
    private static final String SENHA   = "123456";      // ex: "123456"
    // =======================================================================

    static {
        try {
            // Carrega o driver do PostgreSQL (certifique-se de que o .jar esteja no classpath):
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC do PostgreSQL não encontrado: " + e.getMessage());
        }
    }

    /**
     * Retorna uma conexão ativa ao PostgreSQL. 
     * @throws SQLException se falhar ao conectar.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }
}
