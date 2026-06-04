package com.librarysystem.db;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * Database connection manager. Supports MySQL (HikariCP) and H2 embedded.
 * Configuration loaded from {user.home}/.LibrarySystem/librarysystem.properties.
 */
public class DBHelper {
    private static final Logger LOGGER = Logger.getLogger(DBHelper.class.getName());
    private static ConnectionProvider provider;
    public static boolean dbAvailable = false;
    public static String errorMessage = null;

    static {
        try {
            loadConfiguration();
        } catch (Exception e) {
            dbAvailable = false;
            errorMessage = e.getMessage();
            LOGGER.log(Level.SEVERE, "Database initialization failed", e);
        }
    }

    private static void loadConfiguration() throws Exception {
        Properties props = new Properties();
        File configFile = new File(System.getProperty("user.home"),
                ".LibrarySystem/librarysystem.properties");

        if (!configFile.exists()) {
            // First run: no config, will be handled by setup wizard later.
            dbAvailable = false;
            errorMessage = "Database not configured. Please run setup.";
            return;
        }

        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        }

        String dbType = props.getProperty("db.type", "mysql");
        if ("embedded".equalsIgnoreCase(dbType)) {
            provider = new H2EmbeddedProvider(props);
        } else {
            provider = new MySQLPoolProvider(props);
        }

        // Test connection
        try (Connection conn = provider.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                dbAvailable = true;
                LOGGER.info("Database connection established.");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!dbAvailable) throw new SQLException("Database not available.");
        return provider.getConnection();
    }

    public static DataSource getDataSource() {
        return provider.getDataSource();
    }

    public static String getPoolStats() {
        return provider != null ? provider.getStats() : "No pool";
    }

    public static boolean isAvailable() {
        return dbAvailable;
    }

    public static void shutdown() {
        if (provider != null) {
            provider.close();
            provider = null;
        }
    }

    // ========== Connection Providers ==========
    interface ConnectionProvider {
        Connection getConnection() throws SQLException;
        DataSource getDataSource();
        void close();
        String getStats();
    }

    static class MySQLPoolProvider implements ConnectionProvider {
        private HikariDataSource ds;
        public MySQLPoolProvider(Properties props) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.user"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("pool.maximum.size", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("pool.minimum.idle", "3")));
            ds = new HikariDataSource(config);
        }
        public Connection getConnection() throws SQLException { return ds.getConnection(); }
        public DataSource getDataSource() { return ds; }
        public void close() { ds.close(); }
        public String getStats() { return "HikariCP active: " + ds.getHikariPoolMXBean().getActiveConnections(); }
    }

    static class H2EmbeddedProvider implements ConnectionProvider {
        private DataSource ds;
        public H2EmbeddedProvider(Properties props) {
            String url = props.getProperty("db.url", "jdbc:h2:file:~/.LibrarySystem/database;AUTO_SERVER=TRUE");
            ds = org.h2.jdbcx.JdbcConnectionPool.create(url, "sa", "");
        }
        public Connection getConnection() throws SQLException { return ds.getConnection(); }
        public DataSource getDataSource() { return ds; }
        public void close() { if (ds instanceof org.h2.jdbcx.JdbcConnectionPool) ((org.h2.jdbcx.JdbcConnectionPool)ds).dispose(); }
        public String getStats() { return "Embedded H2 active"; }
    }

    public static synchronized void reinitialize() {
        shutdown();
        dbAvailable = false;
        errorMessage = null;
        try {
            loadConfiguration();
        } catch (Exception e) {
            dbAvailable = false;
            errorMessage = e.getMessage();
            LOGGER.log(Level.SEVERE, "Reinitialization failed", e);
        }
    }

}