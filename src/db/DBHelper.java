package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASSWORD = "202304247edward_jr/2%%";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  // ✅ Load driver
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL JDBC Driver not found!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
