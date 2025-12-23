package panels;

import db.DBHelper;
import utils.PasswordUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementPanel extends JPanel {
    private JTable usersTable;
    private DefaultTableModel model;
    private JTextField usernameField, searchField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JButton addUserBtn, editUserBtn, deleteUserBtn, resetPasswordBtn, searchBtn, clearSearchBtn;
    private JLabel statsLabel;

    public UserManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== TOP PANEL - Add User Form =====
        JPanel topPanel = new JPanel(new BorderLayout());

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("👤 Add New User"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);

        // Role
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>(new String[]{"Admin", "Librarian"});
        roleCombo.setSelectedIndex(1); // Default to Librarian
        formPanel.add(roleCombo, gbc);

        // Add Button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        addUserBtn = new JButton("➕ Add User");
        addUserBtn.setToolTipText("Create a new user account");
        addUserBtn.addActionListener(e -> addUser());
        formPanel.add(addUserBtn, gbc);

        topPanel.add(formPanel, BorderLayout.WEST);

        // Search Panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("🔍 Search Users"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5, 5, 5, 5);
        sgbc.fill = GridBagConstraints.HORIZONTAL;

        sgbc.gridx = 0; sgbc.gridy = 0;
        searchPanel.add(new JLabel("Search Username:"), sgbc);
        sgbc.gridx = 1;
        searchField = new JTextField(15);
        searchPanel.add(searchField, sgbc);

        sgbc.gridx = 0; sgbc.gridy = 1; sgbc.gridwidth = 2;
        JPanel searchButtonPanel = new JPanel(new FlowLayout());
        searchBtn = new JButton("🔍 Search");
        searchBtn.addActionListener(e -> searchUsers());
        clearSearchBtn = new JButton("🔄 Show All");
        clearSearchBtn.addActionListener(e -> {
            searchField.setText("");
            loadUsers();
        });
        searchButtonPanel.add(searchBtn);
        searchButtonPanel.add(clearSearchBtn);
        searchPanel.add(searchButtonPanel, sgbc);

        // Stats Label
        sgbc.gridy = 2;
        statsLabel = new JLabel("Total Users: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchPanel.add(statsLabel, sgbc);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ===== TABLE =====
        model = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Created", "Last Login", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        usersTable = new JTable(model);
        usersTable.setFillsViewportHeight(true);
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        add(new JScrollPane(usersTable), BorderLayout.CENTER);

        // ===== BOTTOM BUTTONS =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        editUserBtn = new JButton("✏️ Edit User");
        editUserBtn.setToolTipText("Edit selected user details");
        editUserBtn.addActionListener(e -> editUser());

        deleteUserBtn = new JButton("🗑️ Delete User");
        deleteUserBtn.setToolTipText("Delete selected user");
        deleteUserBtn.addActionListener(e -> deleteUser());

        resetPasswordBtn = new JButton("🔑 Reset Password");
        resetPasswordBtn.setToolTipText("Reset password for selected user");
        resetPasswordBtn.addActionListener(e -> resetPassword());

        JButton toggleStatusBtn = new JButton("🔄 Toggle Status");
        toggleStatusBtn.setToolTipText("Activate/Deactivate user account");
        toggleStatusBtn.addActionListener(e -> toggleUserStatus());

        JButton viewLogsBtn = new JButton("📋 View Activity");
        viewLogsBtn.setToolTipText("View user's activity logs");
        viewLogsBtn.addActionListener(e -> viewUserActivity());

        buttonPanel.add(editUserBtn);
        buttonPanel.add(resetPasswordBtn);
        buttonPanel.add(toggleStatusBtn);
        buttonPanel.add(deleteUserBtn);
        buttonPanel.add(viewLogsBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        loadUsers();
    }

    // ===== Load Users =====
    private void loadUsers() {
        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, username, role, created_at, last_login, is_active FROM users ORDER BY id")) {

            int count = 0;
            while (rs.next()) {
                count++;
                String status = rs.getBoolean("is_active") ? "Active" : "Inactive";
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_login"),
                        status
                });
            }
            statsLabel.setText("Total Users: " + count);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
        }
    }

    // ===== Search Users =====
    private void searchUsers() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadUsers();
            return;
        }

        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, username, role, created_at, last_login, is_active " +
                    "FROM users WHERE username LIKE ? ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                String status = rs.getBoolean("is_active") ? "Active" : "Inactive";
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_login"),
                        status
                });
            }
            statsLabel.setText("Found: " + count + " user(s)");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error searching: " + ex.getMessage());
        }
    }

    // ===== Add User (WITH PASSWORD HASHING) =====
    private void addUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Please enter username and password!");
            return;
        }

        if (username.length() < 3) {
            JOptionPane.showMessageDialog(this, "❌ Username must be at least 3 characters!");
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "❌ Password must be at least 6 characters!");
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Username already exists!");
                return;
            }

            // Hash the password using BCrypt
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Insert new user with hashed password
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ User added successfully with secure password!");
            clearForm();
            loadUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error adding user: " + ex.getMessage());
        }
    }

    // ===== Edit User =====
    private void editUser() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user to edit!");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String currentUsername = (String) model.getValueAt(row, 1);
        String currentRole = (String) model.getValueAt(row, 2);

        String newUsername = JOptionPane.showInputDialog(this, "New Username:", currentUsername);
        if (newUsername == null || newUsername.trim().isEmpty()) return;

        String[] roles = {"Admin", "Librarian"};
        String newRole = (String) JOptionPane.showInputDialog(this, "New Role:", "Edit User",
                JOptionPane.QUESTION_MESSAGE, null, roles, currentRole);
        if (newRole == null) return;

        try (Connection conn = DBHelper.getConnection()) {
            // Check if new username is already taken by another user
            String checkSql = "SELECT COUNT(*) FROM users WHERE username=? AND id!=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, newUsername);
            checkStmt.setInt(2, id);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Username already exists!");
                return;
            }

            String sql = "UPDATE users SET username=?, role=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newUsername);
            stmt.setString(2, newRole);
            stmt.setInt(3, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ User updated successfully!");
            loadUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error updating user: " + ex.getMessage());
        }
    }

    // ===== Reset Password (WITH PASSWORD HASHING) =====
    private void resetPassword() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user!");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();

        Object[] message = {
                "Username: " + username,
                "New Password:", newPasswordField,
                "Confirm Password:", confirmPasswordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Reset Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Please fill all fields!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "❌ Passwords do not match!");
                return;
            }

            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(this, "❌ Password must be at least 6 characters!");
                return;
            }

            try (Connection conn = DBHelper.getConnection()) {
                // Hash the new password using BCrypt
                String hashedPassword = PasswordUtil.hashPassword(newPassword);

                String sql = "UPDATE users SET password=? WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, hashedPassword);
                stmt.setInt(2, id);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "✅ Password reset successfully with secure encryption!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
            }
        }
    }

    // ===== Toggle User Status =====
    private void toggleUserStatus() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user!");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);
        String currentStatus = (String) model.getValueAt(row, 5);

        String newStatus = currentStatus.equals("Active") ? "Inactive" : "Active";
        boolean isActive = newStatus.equals("Active");

        int confirm = JOptionPane.showConfirmDialog(this,
                "Change status of '" + username + "' to " + newStatus + "?",
                "Confirm Status Change",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE users SET is_active=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ User status updated!");
            loadUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
        }
    }

    // ===== Delete User =====
    private void deleteUser() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user to delete!");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        // Prevent deleting the last admin
        String role = (String) model.getValueAt(row, 2);
        if ("Admin".equals(role)) {
            try (Connection conn = DBHelper.getConnection()) {
                String sql = "SELECT COUNT(*) FROM users WHERE role='Admin'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                rs.next();
                if (rs.getInt(1) <= 1) {
                    JOptionPane.showMessageDialog(this,
                            "❌ Cannot delete the last admin user!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete user '" + username + "'?\nThis action cannot be undone!",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "DELETE FROM users WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ User deleted successfully!");
            loadUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error deleting user: " + ex.getMessage());
        }
    }

    // ===== View User Activity =====
    private void viewUserActivity() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user!");
            return;
        }

        int userId = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT action, action_time FROM audit_logs " +
                    "WHERE user_id=? ORDER BY action_time DESC LIMIT 50";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder activity = new StringBuilder();
            activity.append("═══════════════════════════════════\n");
            activity.append("  ACTIVITY LOG: ").append(username).append("\n");
            activity.append("═══════════════════════════════════\n\n");

            boolean hasActivity = false;
            while (rs.next()) {
                hasActivity = true;
                activity.append("• ").append(rs.getString("action")).append("\n");
                activity.append("  ").append(rs.getTimestamp("action_time")).append("\n");
                activity.append("───────────────────────────────────\n");
            }

            if (!hasActivity) {
                activity.append("No activity recorded yet.\n");
            }

            JTextArea textArea = new JTextArea(activity.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(this, scrollPane,
                    "User Activity", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
        }
    }

    // ===== Clear Form =====
    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        roleCombo.setSelectedIndex(1);
    }
}