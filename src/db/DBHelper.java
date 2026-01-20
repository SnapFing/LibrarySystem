package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Helper class (fallback implementation without HikariCP).
 *
 * This class uses DriverManager to get connections when HikariCP is not
 * available in the development environment. It's intentionally simple and
 * safe for local testing. In production, replace with HikariCP-backed
 * implementation and provide the Hikari dependency.
 */
public class DBHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASSWORD = "202304247edward_jr/2%%";

    // No connection pool in this fallback implementation
    private static boolean poolInitialized = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            poolInitialized = true; // mark that driver loaded
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            poolInitialized = false;
        }
    }

    /**
     * Gets a JDBC connection. Caller must close the connection when done.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Placeholder for advanced use-cases: returns null when no pool present.
     */
    public static Object getDataSource() {
        return null;
    }

    /**
     * Close pool - no-op for DriverManager fallback.
     */
    public static void closePool() {
        // No pool to close in this fallback implementation
        System.out.println("DBHelper: closePool() called - no pool to close in fallback mode.");
    }

    public static String getPoolStats() {
        return "No connection pool available (DriverManager fallback).";
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public static int getActiveConnections() {
        return 0; // Not available without a pool
    }

    public static int getIdleConnections() {
        return 0; // Not available without a pool
    }
}