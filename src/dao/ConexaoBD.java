package dao;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import util.AppLogger;

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
    private static final LinkedBlockingDeque<Connection> pool;
    private static final java.util.concurrent.ConcurrentHashMap<Connection, Long> createdAt = new java.util.concurrent.ConcurrentHashMap<>();
    // #001: Semaphore limita total de conexoes abertas (pool + em uso)
    private static final Semaphore semaphore;

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
        if (url == null || usuario == null || senha == null || senha.isEmpty()) {
            throw new RuntimeException(
                "FATAL: db.properties incompleto. Preencha db.url, db.usuario e db.senha.");
        }
        // Garantir sslmode=disable para evitar SSL handshake_failure em PG local com SSL ativado
        if (!url.contains("sslmode=")) {
            url = url + (url.contains("?") ? "&" : "?") + "sslmode=disable";
        }
        URL = url;
        USUARIO = usuario;
        SENHA = senha;
        POOL_SIZE = Integer.parseInt(props.getProperty("db.pool.tamanho", "5"));
        // #001/#003: pool com capacidade fixa + semaphore para limitar total de conexoes
        pool = new LinkedBlockingDeque<>(POOL_SIZE);
        semaphore = new Semaphore(POOL_SIZE * 3);

        // DS006: aviso de senha fraca
        if (senha.length() < 8 || "123456".equals(senha) || "password".equals(senha)) {
            AppLogger.warn("ConexaoBD", "AVISO SEGURANCA: senha do banco e fraca. Altere em db.properties.");
        }

        // Multi-tenant: carrega empresa_id do db.properties (default = 1)
        int empresaId = Integer.parseInt(props.getProperty("empresa.id", "1"));
        TenantContext.setDefaultEmpresaId(empresaId);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            AppLogger.warn("ConexaoBD", "FATAL: Driver JDBC do PostgreSQL nao encontrado.");
            throw new RuntimeException("Driver PostgreSQL nao encontrado", e);
        }

        // Shutdown hook: fecha conexoes ao encerrar a JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[ConexaoBD] Shutdown hook: fechando pool de conexoes...");
            shutdown();
        }));
    }

    /**
     * Retorna uma conexao do pool. Se o pool estiver vazio, cria uma nova.
     * A conexao retornada e um wrapper — ao chamar close(), ela volta ao pool.
     * Inclui timeout e reciclagem de conexoes velhas (max lifetime).
     */
    public static Connection getConnection() throws SQLException {
        // #001: Semaphore limita total de conexoes (pool + em uso)
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrompido ao aguardar conexao", e);
        }
        if (!acquired) {
            throw new SQLException("Pool esgotado — limite de " + (POOL_SIZE * 3) + " conexoes atingido");
        }

        try {
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
                // DR106: timeout no DriverManager para evitar bloqueio indefinido se BD inacessivel
                DriverManager.setLoginTimeout(5);
                Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
                createdAt.put(real, System.currentTimeMillis());
                return new PooledConnection(real);
            }
            throw new SQLException("Timeout ao obter conexao do pool (" + CONNECTION_TIMEOUT_MS + "ms)");
        } catch (SQLException e) {
            semaphore.release(); // libera permit se nao conseguiu conexao
            throw e;
        }
    }

    /**
     * Devolve uma conexao real ao pool (chamado pelo PooledConnection.close()).
     */
    static void devolver(Connection realConn) {
        if (realConn == null) {
            semaphore.release();
            return;
        }
        try {
            if (realConn.isClosed()) {
                createdAt.remove(realConn);
                semaphore.release();
                return;
            }
            if (!realConn.getAutoCommit()) {
                realConn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            createdAt.remove(realConn);
            try { realConn.close(); } catch (SQLException ignored) {}
            semaphore.release();
            return;
        }
        // #003: offerFirst retorna false se pool cheio (capacidade fixa) — atomico
        if (!pool.offerFirst(realConn)) {
            createdAt.remove(realConn);
            try { realConn.close(); } catch (SQLException ignored) {}
        }
        semaphore.release();
    }

    /**
     * Fecha todas as conexoes do pool (para shutdown).
     */
    public static void shutdown() {
        Connection conn;
        while ((conn = pool.pollFirst()) != null) {
            createdAt.remove(conn);
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Wrapper de Connection que devolve ao pool ao inves de fechar.
     */
    private static class PooledConnection implements Connection {

        private final Connection real;
        // DR222: volatile para visibilidade entre threads
        private volatile boolean closed = false;

        PooledConnection(Connection real) {
            this.real = real;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                ConexaoBD.devolver(real);
            }
        }

        @Override public boolean isClosed() throws SQLException { return closed || real.isClosed(); }
        @Override public Statement createStatement() throws SQLException { return real.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return real.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return real.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return real.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { real.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return real.getAutoCommit(); }
        @Override public void commit() throws SQLException { real.commit(); }
        @Override public void rollback() throws SQLException { real.rollback(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return real.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { real.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return real.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { real.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return real.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { real.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return real.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return real.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { real.clearWarnings(); }
        @Override public Statement createStatement(int t, int c) throws SQLException { return real.createStatement(t, c); }
        @Override public PreparedStatement prepareStatement(String sql, int t, int c) throws SQLException { return real.prepareStatement(sql, t, c); }
        @Override public CallableStatement prepareCall(String sql, int t, int c) throws SQLException { return real.prepareCall(sql, t, c); }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return real.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { real.setTypeMap(map); }
        @Override public void setHoldability(int h) throws SQLException { real.setHoldability(h); }
        @Override public int getHoldability() throws SQLException { return real.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return real.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return real.setSavepoint(name); }
        @Override public void rollback(Savepoint sp) throws SQLException { real.rollback(sp); }
        @Override public void releaseSavepoint(Savepoint sp) throws SQLException { real.releaseSavepoint(sp); }
        @Override public Statement createStatement(int t, int c, int h) throws SQLException { return real.createStatement(t, c, h); }
        @Override public PreparedStatement prepareStatement(String sql, int t, int c, int h) throws SQLException { return real.prepareStatement(sql, t, c, h); }
        @Override public CallableStatement prepareCall(String sql, int t, int c, int h) throws SQLException { return real.prepareCall(sql, t, c, h); }
        @Override public PreparedStatement prepareStatement(String sql, int k) throws SQLException { return real.prepareStatement(sql, k); }
        @Override public PreparedStatement prepareStatement(String sql, int[] idx) throws SQLException { return real.prepareStatement(sql, idx); }
        @Override public PreparedStatement prepareStatement(String sql, String[] cols) throws SQLException { return real.prepareStatement(sql, cols); }
        @Override public Clob createClob() throws SQLException { return real.createClob(); }
        @Override public Blob createBlob() throws SQLException { return real.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return real.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return real.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return real.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { real.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties p) throws SQLClientInfoException { real.setClientInfo(p); }
        @Override public String getClientInfo(String name) throws SQLException { return real.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return real.getClientInfo(); }
        @Override public Array createArrayOf(String t, Object[] e) throws SQLException { return real.createArrayOf(t, e); }
        @Override public Struct createStruct(String t, Object[] a) throws SQLException { return real.createStruct(t, a); }
        @Override public void setSchema(String schema) throws SQLException { real.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return real.getSchema(); }
        @Override public void abort(Executor executor) throws SQLException { real.abort(executor); }
        @Override public void setNetworkTimeout(Executor e, int ms) throws SQLException { real.setNetworkTimeout(e, ms); }
        @Override public int getNetworkTimeout() throws SQLException { return real.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return real.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return real.isWrapperFor(iface); }
    }
}
