package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;
import com.librarysystem.utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentProfilePanel extends JPanel {
    private String studentName;
    private JTextField nameField, emailField, phoneField;
    private JButton updateButton, changePasswordButton;
    private JLabel memberSinceLabel, booksCountLabel;

    public StudentProfilePanel(String studentName) {
        this.studentName = studentName;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ===== Title Panel =====
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("👤 My Profile");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // ===== Main Content =====
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Profile Picture Placeholder
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel avatarLabel = new JLabel("👤", JLabel.CENTER);
        avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 80));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        avatarLabel.setPreferredSize(new Dimension(120, 120));
        mainPanel.add(avatarLabel, gbc);

        gbc.gridwidth = 1;

        // Name
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(25);
        nameField.setEditable(true); // Name can be changed
        mainPanel.add(nameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(25);
        mainPanel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(25);
        mainPanel.add(phoneField, gbc);

        // Member Since
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("Member Since:"), gbc);
        gbc.gridx = 1;
        memberSinceLabel = new JLabel("-");
        mainPanel.add(memberSinceLabel, gbc);

        // Books Borrowed (Total)
        gbc.gridx = 0; gbc.gridy = 5;
        mainPanel.add(new JLabel("Total Books Borrowed:"), gbc);
        gbc.gridx = 1;
        booksCountLabel = new JLabel("-");
        mainPanel.add(booksCountLabel, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        updateButton = new JButton("💾 Update Profile");
        updateButton.addActionListener(e -> updateProfile());

        changePasswordButton = new JButton("🔑 Change Password");
        changePasswordButton.addActionListener(e -> changePassword());

        buttonPanel.add(updateButton);
        buttonPanel.add(changePasswordButton);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Load profile data
        loadProfile();
    }

    private void loadProfile() {
        try (Connection conn = DBHelper.getConnection()) {
            // First, get member info
            String sql = "SELECT id, name, email, phone, created_at FROM members WHERE name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, studentName);
            ResultSet rs = stmt.executeQuery();

            int memberId = -1;
            if (rs.next()) {
                memberId = rs.getInt("id");
                nameField.setText(rs.getString("name"));
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("phone"));

                try {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        memberSinceLabel.setText(new java.text.SimpleDateFormat("MMM dd, yyyy").format(createdAt));
                    }
                } catch (Exception e) {
                    memberSinceLabel.setText("N/A");
                }
            }

            // ✅ FIXED: Use member_id instead of member_name
            if (memberId != -1) {
                String countSql = "SELECT COUNT(*) as total FROM borrowed_books WHERE member_id=?";
                PreparedStatement countStmt = conn.prepareStatement(countSql);
                countStmt.setInt(1, memberId);
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    booksCountLabel.setText(String.valueOf(countRs.getInt("total")));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading profile: " + ex.getMessage());
        }
    }

    private void updateProfile() {
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validation
        if (email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Email and phone cannot be empty!");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            JOptionPane.showMessageDialog(this, "❌ Invalid email format!");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "❌ Phone must be 10 digits!");
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE members SET email=?, phone=? WHERE name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, phone);
            stmt.setString(3, studentName);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this,
                        "✅ Profile updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Error updating profile: " + ex.getMessage());
        }
    }

    private void changePassword() {
        JPasswordField currentPasswordField = new JPasswordField();
        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();

        Object[] message = {
                "Current Password:", currentPasswordField,
                "New Password:", newPasswordField,
                "Confirm New Password:", confirmPasswordField,
                " ",
                new JLabel("<html><small>Password must have: 8+ chars, letter, digit, special char</small></html>")
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String currentPassword = new String(currentPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Please fill all fields!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "❌ New passwords do not match!");
                return;
            }

            // Validate password strength
            String strengthError = PasswordUtil.validatePasswordStrength(newPassword);
            if (strengthError != null) {
                JOptionPane.showMessageDialog(this, "❌ " + strengthError);
                return;
            }

            try (Connection conn = DBHelper.getConnection()) {
                // Verify current password
                String verifySql = "SELECT password FROM members WHERE name=?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setString(1, studentName);
                ResultSet rs = verifyStmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    boolean currentPasswordCorrect = false;
                    if (storedPassword.contains(":")) {
                        currentPasswordCorrect = PasswordUtil.verifyPassword(currentPassword, storedPassword);
                    } else {
                        currentPasswordCorrect = currentPassword.equals(storedPassword);
                    }

                    if (!currentPasswordCorrect) {
                        JOptionPane.showMessageDialog(this, "❌ Current password is incorrect!");
                        return;
                    }

                    // Hash new password
                    String hashedPassword = PasswordUtil.hashPassword(newPassword);

                    // Update password
                    String updateSql = "UPDATE members SET password=? WHERE name=?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, studentName);
                    updateStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this,
                            "✅ Password changed successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "❌ Error changing password: " + ex.getMessage());
            }
        }
    }
}