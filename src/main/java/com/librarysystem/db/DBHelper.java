package com.librarysystem.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Helper class (fallback implementation without HikariCP).
 *
 * This class uses DriverManager to get connections when HikariCP is not
 * available in the development environment. It's intentionally simple and
 * safe for local testing.
 *
 * Database credentials are loaded from:
 * 1. db.properties file (preferred)
 * 2. Environment variables (fallback)
 */
public class DBHelper {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    private static boolean poolInitialized = false;

    static {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            poolInitialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found: " + e.getMessage());
            poolInitialized = false;
        }
    }

    /**
     * Load database configuration from properties file or environment variables
     */
    private static void loadDatabaseConfig() {
        System.out.println("\n========== DATABASE CONFIGURATION DIAGNOSTIC ==========");
        System.out.println("Current working directory: " + System.getProperty("user.dir"));
        System.out.println("User home directory: " + System.getProperty("user.home"));
        System.out.println("Java classpath: " + System.getProperty("java.class.path"));

        Properties props = new Properties();
        boolean loaded = false;

        // === METHOD 1: Try loading from classpath (RECOMMENDED) ===
        System.out.println("\n[Method 1] Trying to load from classpath...");
        try (InputStream input = DBHelper.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
                URL = props.getProperty("db.url");
                USER = props.getProperty("db.user");  // ← KEY CHANGE: was "db.username"
                PASSWORD = props.getProperty("db.password");

                if (URL != null && USER != null && PASSWORD != null) {
                    System.out.println("✅ SUCCESS: Loaded from classpath");
                    loaded = true;
                } else {
                    System.out.println("⚠️  File found but missing required properties");
                    System.out.println("   db.url: " + (URL != null ? "✓" : "✗"));
                    System.out.println("   db.user: " + (USER != null ? "✓" : "✗"));
                    System.out.println("   db.password: " + (PASSWORD != null ? "✓" : "✗"));
                }
            } else {
                System.out.println("❌ db.properties not found in classpath");
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading from classpath: " + e.getMessage());
        }

        // === METHOD 2: Try file system locations ===
        if (!loaded) {
            System.out.println("\n[Method 2] Trying file system locations...");
            String[] possiblePaths = {
                    "db.properties",                                    // Project root
                    "src/db.properties",                                // src folder
                    "resources/db.properties",                          // resources folder
                    "src/main/resources/db.properties",                 // Maven standard
                    "config/db.properties",                             // config folder
                    System.getProperty("user.dir") + "/db.properties",  // Working directory
                    System.getProperty("user.home") + "/.library/db.properties"  // User home
            };

            for (String path : possiblePaths) {
                System.out.println("  Checking: " + path);
                File file = new File(path);

                if (file.exists()) {
                    System.out.println("    ✓ File exists");
                    try (InputStream input = new FileInputStream(file)) {
                        props.load(input);
                        URL = props.getProperty("db.url");
                        USER = props.getProperty("db.user");
                        PASSWORD = props.getProperty("db.password");

                        if (URL != null && USER != null && PASSWORD != null) {
                            System.out.println("    ✅ SUCCESS: Loaded from " + path);
                            loaded = true;
                            break;
                        } else {
                            System.out.println("    ⚠️  File found but missing properties");
                        }
                    } catch (IOException e) {
                        System.out.println("    ❌ Error reading: " + e.getMessage());
                    }
                } else {
                    System.out.println("    ✗ File not found");
                }
            }
        }

        // === METHOD 3: Try to list classpath resources ===
        if (!loaded) {
            System.out.println("\n[Method 3] Listing available classpath resources:");
            try {
                java.util.Enumeration<URL> resources =
                        DBHelper.class.getClassLoader().getResources("");
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    System.out.println("  - " + resource);

                    // Try to list files in this resource location
                    if ("file".equals(resource.getProtocol())) {
                        File dir = new File(resource.toURI());
                        if (dir.exists() && dir.isDirectory()) {
                            File[] files = dir.listFiles();
                            if (files != null) {
                                for (File f : files) {
                                    System.out.println("    → " + f.getName());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  Error listing resources: " + e.getMessage());
            }
        }

        // === METHOD 4: Environment variables fallback ===
        if (!loaded || URL == null || USER == null || PASSWORD == null) {
            System.out.println("\n[Method 4] Trying environment variables...");
            URL = System.getenv("DB_URL");
            USER = System.getenv("DB_USER");
            PASSWORD = System.getenv("DB_PASSWORD");

            if (URL != null && USER != null && PASSWORD != null) {
                System.out.println("✅ SUCCESS: Loaded from environment variables");
                loaded = true;
            } else {
                System.out.println("❌ Environment variables not set");
                System.out.println("   DB_URL: " + (URL != null ? "✓" : "✗"));
                System.out.println("   DB_USER: " + (USER != null ? "✓" : "✗"));
                System.out.println("   DB_PASSWORD: " + (PASSWORD != null ? "✓" : "✗"));
            }
        }

        // === Final validation ===
        System.out.println("\n========== CONFIGURATION RESULT ==========");
        if (loaded && URL != null && USER != null && PASSWORD != null) {
            System.out.println("✅ Database configuration loaded successfully!");
            System.out.println("   URL: " + URL);
            System.out.println("   User: " + USER);
            System.out.println("   Password: " + (PASSWORD.length() > 0 ? "[SET]" : "[EMPTY]"));
        } else {
            System.err.println("❌ ERROR: Database configuration NOT found!");
            System.err.println("\n📋 TROUBLESHOOTING STEPS:");
            System.err.println("1. Create a file named 'db.properties' with:");
            System.err.println("   db.url=jdbc:mysql://localhost:3306/library_db");
            System.err.println("   db.user=root");
            System.err.println("   db.password=your_password_here");
            System.err.println("\n2. Place it in one of these locations:");
            System.err.println("   - src/main/resources/db.properties (Maven standard)");
            System.err.println("   - resources/db.properties (project root)");
            System.err.println("   - db.properties (project root)");
            System.err.println("\n3. If using IDE, rebuild/recompile the project");
            System.err.println("\n4. OR set environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.err.println("==========================================\n");

            throw new RuntimeException("Database configuration not found!");
        }
        System.out.println("==========================================\n");
    }

    /**
     * Gets a JDBC connection. Caller must close the connection when done.
     */
    public static Connection getConnection() throws SQLException {
        if (URL == null || USER == null || PASSWORD == null) {
            throw new SQLException("Database not configured. Check startup logs.");
        }
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
            e.printStackTrace();
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