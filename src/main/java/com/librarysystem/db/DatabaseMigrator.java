package com.librarysystem.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Simple migrator that reads schema.sql from classpath and executes it.
 * In production, you'd add version checks, but for now this ensures all
 * tables are created on first run.
 */
public class DatabaseMigrator {

    /**
     * Run the full schema SQL file against the given connection.
     * @param conn an open JDBC connection (auto-commit can be on or off)
     * @throws SQLException if any SQL statement fails
     */
    public static void runSchema(Connection conn) throws SQLException {
        // Load the SQL file as a stream
        try (InputStream in = DatabaseMigrator.class
                .getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql not found in classpath");
            }

            Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())
                    .useDelimiter("\\s*;\\s*");

            Statement stmt = conn.createStatement();

            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (sql.isEmpty() || sql.startsWith("--")) {
                    continue; // skip comments
                }
                // Replace DELIMITER commands (not needed with JDBC)
                if (sql.toUpperCase().startsWith("DELIMITER")) {
                    continue;
                }
                stmt.execute(sql);
            }

            stmt.close();
            scanner.close();

        } catch (Exception e) {
            throw new SQLException("Schema migration failed", e);
        }
    }
}