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
    private static final long CONNECTION_TIMEOUT_MS = 5000; // 5s para obter conexao
    private static final long MAX_LIFETIME_MS = 30 * 60 * 1000; // 30min — recicla conexoes velhas
    private static final LinkedBlockingDeque<Connection> pool = new LinkedBlockingDeque<>();
    private static final java.util.Map<Connection, Long> createdAt = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        Properties props = new Properties();
        // Carrega db.properties — obrigatorio (sem fallback de senha hardcoded)
        try (InputStream is = new FileInputStream("db.properties")) {
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException(
                "FATAL: db.properties nao encontrado. Copie db.properties.example para db.properties e preencha suas credenciais.", e);
        }
        String url = props.getProperty("db.url");
        String usuario = props.getProperty("db.usuario");
        String senha = props.getProperty("db.senha");
        if (url == null || usuario == null || senha == null
                || senha.isEmpty() || "SUA_SENHA_AQUI".equals(senha)) {
            throw new RuntimeException(
                "FATAL: db.properties incompleto. Preencha db.url, db.usuario e db.senha.");
        }
        URL = url;
        USUARIO = usuario;
        SENHA = senha;
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
        long deadline = System.currentTimeMillis() + CONNECTION_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Connection conn = pool.pollFirst();
            if (conn != null) {
                // Recicla conexoes velhas (max lifetime)
                Long created = createdAt.get(conn);
                if (created != null && (System.currentTimeMillis() - created) > MAX_LIFETIME_MS) {
                    createdAt.remove(conn);
                    try { conn.close(); } catch (SQLException ignored) {}
                    continue;
                }
                try {
                    if (!conn.isClosed() && conn.isValid(1)) {
                        return new PooledConnection(conn);
                    }
                    createdAt.remove(conn);
                    try { conn.close(); } catch (SQLException ignored) {}
                } catch (SQLException e) {
                    createdAt.remove(conn);
                    try { conn.close(); } catch (SQLException ignored) {}
                }
                continue;
            }
            // Pool vazio — cria nova conexao
            Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
            createdAt.put(real, System.currentTimeMillis());
            return new PooledConnection(real);
        }
        throw new SQLException("Timeout ao obter conexao do pool (" + CONNECTION_TIMEOUT_MS + "ms)");
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
