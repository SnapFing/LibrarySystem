package com.librarysystem.utils;

import com.librarysystem.db.DBHelper;
import java.sql.*;
import java.util.logging.*;

/**
 * Fine calculator that reads fine‑per‑day from system_settings
 * and uses TransactionManager for safe database writes.
 */
public class FineCalculator {

    private static final Logger LOG = Logger.getLogger(FineCalculator.class.getName());

    /**
     * Get the current fine rate from the system_settings table.
     * @return fine rate per day (default 5.00 if not set)
     */
    public static double getFinePerDay() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'fine_per_day'";
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return Double.parseDouble(rs.getString("setting_value"));
            }
        } catch (SQLException | NumberFormatException e) {
            LOG.log(Level.WARNING, "Could not read fine rate, using default 5.00", e);
        }
        return 5.00; // default fallback
    }

    /**
     * Calculate overdue fines for all currently overdue books that have no fine yet.
     * This method is safe to call multiple times – it only creates fines where none exist.
     */
    public static void calculateOverdueFines() {
        try {
            double rate = getFinePerDay();
            TransactionManager.executeTransactionVoid(conn -> {
                String sql = "SELECT bb.id, DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue " +
                        "FROM borrowed_books bb " +
                        "LEFT JOIN fines f ON bb.id = f.borrow_id " +
                        "WHERE bb.status = 'BORROWED' " +
                        "AND bb.due_date < CURRENT_DATE " +
                        "AND f.id IS NULL";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    while (rs.next()) {
                        int borrowId = rs.getInt("id");
                        int daysOverdue = rs.getInt("days_overdue");
                        double fineAmount = daysOverdue * rate;

                        try (PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO fines (borrow_id, amount, reason) VALUES (?, ?, ?)")) {
                            insertStmt.setInt(1, borrowId);
                            insertStmt.setDouble(2, fineAmount);
                            insertStmt.setString(3,
                                    "Late return - " + daysOverdue + " days overdue");
                            insertStmt.executeUpdate();
                        }
                    }
                }
            });
            LOG.info("Overdue fines calculated successfully.");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to calculate overdue fines", e);
        }
    }

    /**
     * Calculate the fine for a single borrow record that has just been returned.
     * This should be called at return time if the book was overdue.
     * @param borrowId the borrowed_books id
     * @param dueDate the original due date
     * @param returnDate the actual return date
     * @return the fine amount (0 if not overdue)
     * @throws SQLException if insert fails
     */
    public static double calculateAndCreateFineForReturn(int borrowId,
                                                         java.time.LocalDate dueDate,
                                                         java.time.LocalDate returnDate)
            throws SQLException {
        if (returnDate == null || !returnDate.isAfter(dueDate)) {
            return 0.0; // not overdue, no fine
        }

        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, returnDate);
        double rate = getFinePerDay();
        double amount = daysOverdue * rate;

        String insertSql = "INSERT INTO fines (borrow_id, amount, reason) VALUES (?, ?, ?)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setInt(1, borrowId);
            stmt.setDouble(2, amount);
            stmt.setString(3, "Late return - " + daysOverdue + " days overdue");
            stmt.executeUpdate();
        }

        return amount;
    }
}