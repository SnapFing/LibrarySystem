package com.librarysystem.utils;

import com.librarysystem.db.DBHelper;

import java.sql.*;

public class FineCalculator {
    private static final double FINE_PER_DAY = 2.00;    // K2 per day

    public static void calculateOverdueFines() {
        try (Connection conn = DBHelper.getConnection()) {
            // Find all overdue books without fines
            String sql = "SELECT bb.id, DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue " +
                    "FROM borrowed_books bb " +
                    "LEFT JOIN fines f ON bb.id = f.borrow_id " +
                    "WHERE bb.status = 'BORROWED' " +
                    "AND bb.due_date < CURRENT_DATE " +
                    "AND f.id IS NULL";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int borrowId = rs.getInt("id");
                int daysOverdue = rs.getInt("days_overdue");
                double fineAmount = daysOverdue *  FINE_PER_DAY;

                // Insert fine
                String insertSql = "INSERT INTO fines (borrow_id, amount, reason) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, borrowId);
                insertStmt.setDouble(2, fineAmount);
                insertStmt.setString(3, "Late return - " + daysOverdue + " days overdue");
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
