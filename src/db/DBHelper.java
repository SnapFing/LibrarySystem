package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Helper class using HikariCP connection pooling.
 *
 * HikariCP provides:
 * - Connection pooling (reuse connections instead of creating new ones)
 * - Better performance (significantly faster than DriverManager)
 * - Automatic connection health checks
 * - Connection leak detection
 * - Proper resource management
 */

public class DBHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASSWORD = "202304247edward_jr/2%%";

    // HikariCP DataSource - thread-safe, manages connection pool
    private static HikariDataSource dataSource;

    // Static initialization block - runs once when class is loaded
    static {
        try {
            // Explicitly load MySQL JDBC driver (though HikariCP can auto-detect)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Initialize HikariCP connection pool
            initializePool();

            System.out.println("✅ Database connection pool initialized successfully");
            System.out.println("📊 Pool Info - Max Pool Size: " + dataSource.getMaximumPoolSize());

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL JDBC Driver not found!", e);
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to initialize database connection pool!", e);
        }
    }

    /**
     * Initializes the HikariCP connection pool with optimal settings.
     */
    private static void initializePool() {
        HikariConfig config = new HikariConfig();

        // === Basic Connection Settings ===
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        // === Pool Size Configuration ===
        // Maximum number of connections in the pool
        config.setMaximumPoolSize(10);

        // Minimum number of idle connections to maintain
        config.setMinimumIdle(3);

        // === Connection Timeout Settings ===
        // Maximum time (ms) to wait for a connection from the pool
        config.setConnectionTimeout(30000); // 30 seconds

        // Maximum time (ms) a connection can sit idle in the pool
        config.setIdleTimeout(600000); // 10 minutes

        // Maximum lifetime (ms) of a connection in the pool
        config.setMaxLifetime(1800000); // 30 minutes

        // === Performance Optimization ===
        // Enable prepared statement caching for better performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // Use SSL for secure connection (optional - disable if not using SSL)
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");

        // === Connection Testing ===
        // Test query to validate connections are alive
        config.setConnectionTestQuery("SELECT 1");

        // === Pool Name (for monitoring/debugging) ===
        config.setPoolName("LibrarySystemPool");

        // === Leak Detection (helpful for debugging) ===
        // Logs a warning if connection is held longer than this (ms)
        config.setLeakDetectionThreshold(60000); // 1 minute

        // === Auto-commit ===
        // Set to false if you want to manually manage transactions
        config.setAutoCommit(true);

        // Create the DataSource
        dataSource = new HikariDataSource(config);
    }

    /**
     * Gets a connection from the pool.
    **/
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Gets the HikariCP DataSource instance.
     * Useful for advanced monitoring and configuration
     */
    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Closes the connection pool and releases all resources.
     * Should be called when the application is shutting down.
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("🔄 Closing database connection pool...");
            dataSource.close();
            System.out.println("✅ Database connection pool closed successfully");
        }
    }
    /**
     * Gets pool statistics for monitoring.
     *
     * @return String containing pool statistics
     */
    public static String getPoolStats() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }

        StringBuilder stats = new StringBuilder();
        stats.append("═══════════════════════════════════\n");
        stats.append("    DATABASE POOL STATISTICS\n");
        stats.append("═══════════════════════════════════\n");
        stats.append("Pool Name: ").append(dataSource.getPoolName()).append("\n");
        stats.append("Active Connections: ").append(dataSource.getHikariPoolMXBean().getActiveConnections()).append("\n");
        stats.append("Idle Connections: ").append(dataSource.getHikariPoolMXBean().getIdleConnections()).append("\n");
        stats.append("Total Connections: ").append(dataSource.getHikariPoolMXBean().getTotalConnections()).append("\n");
        stats.append("Threads Awaiting: ").append(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).append("\n");
        stats.append("Max Pool Size: ").append(dataSource.getMaximumPoolSize()).append("\n");
        stats.append("Min Idle: ").append(dataSource.getMinimumIdle()).append("\n");
        stats.append("═══════════════════════════════════");

        return stats.toString();
    }

    /**
     * Tests the database connection.
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current number of active connections in the pool.
     * @return Number of active connections
     */
    public static int getActiveConnections() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    /**
     * Gets the current number of idle connections in the pool.
     * @return Number of idle connections
     */
    public static int getIdleConnections() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
}