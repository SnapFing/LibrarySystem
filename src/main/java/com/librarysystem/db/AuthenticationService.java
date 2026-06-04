package com.librarysystem.db;

import com.librarysystem.core.Session;
import com.librarysystem.utils.PasswordUtil;

import java.sql.*;

/**
 * Handles authentication against the database.
 * Staff are looked up in users table (username),
 * members in members table (email).
 */
public class AuthenticationService {

    /**
     * Attempt a login with the given identifier (username for staff, email for members).
     * @return a populated Session on success
     * @throws AuthenticationException if credentials are invalid
     * @throws SQLException on database errors
     */
    public static Session login(String login, String password)
            throws AuthenticationException, SQLException {

        try (Connection conn = DBHelper.getConnection()) {
            // 1. Try users table (staff)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, fname, mname, lname, password, role, is_active FROM users WHERE username = ?")) {
                stmt.setString(1, login);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String hashed = rs.getString("password");
                        boolean active = rs.getBoolean("is_active");
                        if (!active) {
                            throw new AuthenticationException("Account is deactivated.");
                        }
                        if (PasswordUtil.verifyPassword(password, hashed)) {
                            String fullName = buildFullName(
                                    rs.getString("fname"),
                                    rs.getString("mname"),
                                    rs.getString("lname"));
                            // Update last login
                            updateLastLogin(conn, rs.getInt("id"), "users");

                            Session.getInstance().login(
                                    rs.getInt("id"), 0, login,
                                    rs.getString("role"), fullName);
                            return Session.getInstance();
                        }
                    }
                }
            }

            // 2. Try members table (email)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, fname, mname, lname, password, is_active FROM members WHERE email = ?")) {
                stmt.setString(1, login);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String hashed = rs.getString("password");
                        boolean active = rs.getBoolean("is_active");
                        if (!active) {
                            throw new AuthenticationException("Account is deactivated.");
                        }
                        if (PasswordUtil.verifyPassword(password, hashed)) {
                            String fullName = buildFullName(
                                    rs.getString("fname"),
                                    rs.getString("mname"),
                                    rs.getString("lname"));
                            updateLastLogin(conn, rs.getInt("id"), "members");

                            Session.getInstance().login(
                                    0, rs.getInt("id"), login,
                                    "Student", fullName);
                            return Session.getInstance();
                        }
                    }
                }
            }

            // If we reach here, login failed
            throw new AuthenticationException("Invalid username/email or password.");
        }
    }

    /**
     * Register a new member (student) with bcrypt hashed password.
     * @throws SQLException on DB error, including duplicate email
     */
    public static void registerMember(String fname, String mname, String lname,
                                      String email, String phone, String password)
            throws SQLException {
        String hashed = PasswordUtil.hashPassword(password);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "INSERT INTO members (fname, mname, lname, email, phone, password, membership_date) "
                    + "VALUES (?, ?, ?, ?, ?, ?, CURDATE())";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fname);
                stmt.setString(2, mname);
                stmt.setString(3, lname);
                stmt.setString(4, email);
                stmt.setString(5, phone);
                stmt.setString(6, hashed);
                stmt.executeUpdate();
            }
        }
    }

    private static String buildFullName(String fname, String mname, String lname) {
        StringBuilder sb = new StringBuilder(fname);
        if (mname != null && !mname.isEmpty()) {
            sb.append(" ").append(mname);
        }
        sb.append(" ").append(lname);
        return sb.toString();
    }

    private static void updateLastLogin(Connection conn, int id, String table) {
        String col = table.equals("users") ? "id" : "id"; // both have 'id'
        String sql = "UPDATE " + table + " SET last_login = NOW() WHERE " + col + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // last_login column might not exist; ignore
        }
    }

    // Exception class
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}