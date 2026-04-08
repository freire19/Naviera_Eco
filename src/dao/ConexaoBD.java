package dao;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Pool de conexoes JDBC simples para PostgreSQL.
 * Configuracao lida de db.properties (URL, usuario, senha, tamanho do pool).
 * Conexoes retornam ao pool ao serem fechadas via try-with-resources.
 */
public class ConexaoBD {

    private static final String URL;
    private static final String USUARIO;
    private static final String SENHA;
    private static final int POOL_SIZE;
    private static final LinkedBlockingDeque<Connection> pool = new LinkedBlockingDeque<>();

    static {
        Properties props = new Properties();
        // Tenta carregar db.properties do diretorio de trabalho
        try (InputStream is = new FileInputStream("db.properties")) {
            props.load(is);
        } catch (Exception e) {
            System.err.println("db.properties nao encontrado, usando defaults.");
        }
        URL = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/sistema_embarcacao");
        USUARIO = props.getProperty("db.usuario", "postgres");
        SENHA = props.getProperty("db.senha", "123456");
        POOL_SIZE = Integer.parseInt(props.getProperty("db.pool.tamanho", "5"));

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: Driver JDBC do PostgreSQL nao encontrado.");
            throw new RuntimeException("Driver PostgreSQL nao encontrado", e);
        }
    }

    /**
     * Retorna uma conexao do pool. Se o pool estiver vazio, cria uma nova.
     * A conexao retornada e um wrapper — ao chamar close(), ela volta ao pool.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = pool.pollFirst();
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    return new PooledConnection(conn);
                }
                // Conexao invalida, descarta
                try { conn.close(); } catch (SQLException ignored) {}
            } catch (SQLException e) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        // Cria nova conexao real
        Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
        return new PooledConnection(real);
    }

    /**
     * Devolve uma conexao real ao pool (chamado pelo PooledConnection.close()).
     */
    static void devolver(Connection realConn) {
        if (realConn == null) return;
        try {
            if (realConn.isClosed()) return;
            if (!realConn.getAutoCommit()) {
                realConn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            try { realConn.close(); } catch (SQLException ignored) {}
            return;
        }
        if (pool.size() < POOL_SIZE) {
            pool.offerFirst(realConn);
        } else {
            try { realConn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Fecha todas as conexoes do pool (para shutdown).
     */
    public static void shutdown() {
        Connection conn;
        while ((conn = pool.pollFirst()) != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}
