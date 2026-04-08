package dao;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Classe responsável por fornecer conexões JDBC ao banco de dados.
 * Usa um pool simples de conexões para evitar o custo de abrir/fechar
 * conexões TCP a cada operação (~100-300ms por conexão).
 */
public class ConexaoBD {

    private static final String URL     = "jdbc:postgresql://localhost:5432/sistema_embarcacao";
    private static final String USUARIO = "postgres";
    private static final String SENHA   = "123456";

    /** Pool de conexões reutilizáveis */
    private static final int POOL_MAX = 5;
    private static final LinkedBlockingDeque<Connection> pool = new LinkedBlockingDeque<>(POOL_MAX);

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC do PostgreSQL não encontrado: " + e.getMessage());
        }
    }

    /**
     * Retorna uma conexão do pool (ou cria uma nova se o pool estiver vazio).
     * IMPORTANTE: sempre feche a conexão com close() — o wrapper a devolve ao pool.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = pool.pollFirst();
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    return new PooledConnection(conn);
                }
                try { conn.close(); } catch (SQLException ignored) {}
            } catch (SQLException e) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        Connection real = DriverManager.getConnection(URL, USUARIO, SENHA);
        return new PooledConnection(real);
    }

    /** Devolve a conexão ao pool em vez de fechar de verdade */
    private static void devolver(Connection conn) {
        if (conn == null) return;
        try {
            if (conn.isClosed() || !conn.isValid(1)) {
                return;
            }
            if (!conn.getAutoCommit()) {
                conn.setAutoCommit(true);
            }
            if (!pool.offerFirst(conn)) {
                conn.close();
            }
        } catch (SQLException e) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Wrapper de Connection que devolve ao pool ao invés de fechar.
     */
    private static class PooledConnection implements Connection {

        private final Connection real;
        private boolean closed = false;

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
