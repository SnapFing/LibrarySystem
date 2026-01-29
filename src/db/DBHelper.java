package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Helper class (fallback implementation without HikariCP).
 *
 * This class uses DriverManager to get connections when HikariCP is not
 * available in the development environment. It's intentionally simple and
 * safe for local testing. In production, replace with HikariCP-backed
 * implementation and provide the Hikari dependency.
 *
 * Database credentials are loaded from:
 * 1. db.properties file (preferred)
 * 2. Environment variables (fallback)
 */
public class DBHelper {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // No connection pool in this fallback implementation
    private static boolean poolInitialized = false;

    static {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            poolInitialized = true; // mark that driver loaded
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            poolInitialized = false;
        }
    }

    /**
     * Load database configuration from properties file or environment variables
     */
    private static void loadDatabaseConfig() {
        Properties props = new Properties();

        // Try multiple locations for the properties file
        String[] possiblePaths = {
                "db.properties",                    // Project root
                "src/db.properties",                // src folder
                "config/db.properties",             // config folder
                "src/config/db.properties",         // src/config folder
                System.getProperty("user.dir") + "/db.properties",  // Current working directory
                System.getProperty("user.home") + "/.library/db.properties"  // User home directory
        };

        boolean loaded = false;

        // Try to load from file system
        for (String path : possiblePaths) {
            try (InputStream input = new FileInputStream(path)) {
                props.load(input);

                URL = props.getProperty("db.url");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                if (URL != null && USER != null && PASSWORD != null) {
                    System.out.println("✅ Database configuration loaded from: " + path);
                    loaded = true;
                    break;
                }
            } catch (IOException e) {
                // Try next path
                continue;
            }
        }

        // Try loading from classpath
        if (!loaded) {
            try (InputStream input = DBHelper.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (input != null) {
                    props.load(input);
                    URL = props.getProperty("db.url");
                    USER = props.getProperty("db.user");
                    PASSWORD = props.getProperty("db.password");

                    if (URL != null && USER != null && PASSWORD != null) {
                        System.out.println("✅ Database configuration loaded from classpath");
                        loaded = true;
                    }
                }
            } catch (IOException e) {
                // Continue to fallback
            }
        }

        // Fallback to environment variables
        if (!loaded || URL == null || USER == null || PASSWORD == null) {
            System.out.println("⚠️  db.properties not found, checking environment variables...");
            URL = System.getenv("DB_URL");
            USER = System.getenv("DB_USER");
            PASSWORD = System.getenv("DB_PASSWORD");

            if (URL != null && USER != null && PASSWORD != null) {
                System.out.println("✅ Database configuration loaded from environment variables");
                loaded = true;
            }
        }

        // Final validation
        if (!loaded || URL == null || USER == null || PASSWORD == null) {
            System.err.println("❌ ERROR: Database configuration not found!");
            System.err.println("");
            System.err.println("Please create a 'db.properties' file in your project root with:");
            System.err.println("  db.url=jdbc:mysql://localhost:3306/library_db");
            System.err.println("  db.user=root");
            System.err.println("  db.password=your_password_here");
            System.err.println("");
            System.err.println("OR set environment variables:");
            System.err.println("  DB_URL, DB_USER, DB_PASSWORD");
            System.err.println("");
            throw new RuntimeException("Database configuration not found!");
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
            boolean isValid = conn != null && !conn.isClosed();
            if (isValid) {
                System.out.println("✅ Database connection test successful!");
                System.out.println("   Connected to: " + conn.getCatalog());
                System.out.println("   User: " + USER);
            }
            return isValid;
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
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