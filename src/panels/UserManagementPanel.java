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
        boolean hasNewColumns = checkDatabaseColumns();

        if (hasNewColumns) {
            // ✅ FIX: Added "ID" column first
            model = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Created", "Last Login", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
        } else {
            model = new DefaultTableModel(new String[]{"ID", "Username", "Role"}, 0) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
        }

        usersTable = new JTable(model);
        usersTable.setFillsViewportHeight(true);

        // ✅ FIX: Proper column widths
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        if (hasNewColumns) {
            usersTable.getColumnModel().getColumn(3).setPreferredWidth(120);
            usersTable.getColumnModel().getColumn(4).setPreferredWidth(120);
            usersTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        }

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

        buttonPanel.add(editUserBtn);
        buttonPanel.add(resetPasswordBtn);
        buttonPanel.add(deleteUserBtn);

        if (hasNewColumns) {
            JButton toggleStatusBtn = new JButton("🔄 Toggle Status");
            toggleStatusBtn.setToolTipText("Activate/Deactivate user account");
            toggleStatusBtn.addActionListener(e -> toggleUserStatus());

            JButton viewLogsBtn = new JButton("📋 View Activity");
            viewLogsBtn.setToolTipText("View user's activity logs");
            viewLogsBtn.addActionListener(e -> viewUserActivity());

            buttonPanel.add(toggleStatusBtn);
            buttonPanel.add(viewLogsBtn);
        }

        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        loadUsers();
    }

    // ===== Check if database has new columns =====
    private boolean checkDatabaseColumns() {
        try (Connection conn = DBHelper.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "users", "is_active");
            return columns.next(); // Returns true if column exists
        } catch (Exception ex) {
            System.err.println("Could not check database columns: " + ex.getMessage());
            return false;
        }
    }

    // ===== Load Users (✅ FIXED) =====
    private void loadUsers() {
        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean hasNewColumns = (model.getColumnCount() > 3);

            String sql;
            if (hasNewColumns) {
                sql = "SELECT id, username, role, created_at, last_login, is_active FROM users ORDER BY id";
            } else {
                sql = "SELECT id, username, role FROM users ORDER BY id";
            }

            ResultSet rs = stmt.executeQuery(sql);

            int count = 0;
            while (rs.next()) {
                count++;
                if (hasNewColumns) {
                    String status = rs.getBoolean("is_active") ? "Active" : "Inactive";
                    // ✅ FIX: Now includes ID in first position
                    model.addRow(new Object[]{
                            rs.getInt("id"),              // Column 0: ID
                            rs.getString("username"),     // Column 1: Username
                            rs.getString("role"),         // Column 2: Role
                            rs.getTimestamp("created_at"),// Column 3: Created
                            rs.getTimestamp("last_login"),// Column 4: Last Login
                            status                        // Column 5: Status
                    });
                } else {
                    // ✅ FIX: Includes ID properly
                    model.addRow(new Object[]{
                            rs.getInt("id"),          // Column 0: ID
                            rs.getString("username"), // Column 1: Username
                            rs.getString("role")      // Column 2: Role
                    });
                }
            }
            statsLabel.setText("Total Users: " + count);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading users: " + ex.getMessage() + "\n\nCheck console for details.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== Search Users (✅ FIXED) =====
    private void searchUsers() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadUsers();
            return;
        }

        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            boolean hasNewColumns = (model.getColumnCount() > 3);
            String sql;

            if (hasNewColumns) {
                sql = "SELECT id, username, role, created_at, last_login, is_active " +
                        "FROM users WHERE username LIKE ? ORDER BY id";
            } else {
                sql = "SELECT id, username, role FROM users WHERE username LIKE ? ORDER BY id";
            }

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                if (hasNewColumns) {
                    String status = rs.getBoolean("is_active") ? "Active" : "Inactive";
                    // ✅ FIX: Includes ID
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("last_login"),
                            status
                    });
                } else {
                    // ✅ FIX: Includes ID
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role")
                    });
                }
            }
            statsLabel.setText("Found: " + count + " user(s)");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error searching: " + ex.getMessage());
        }
    }

    // ===== Add User =====
    private void addUser() {
        System.out.println("=== ADD USER DEBUG ===");

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleCombo.getSelectedItem();

        System.out.println("Username: " + username);
        System.out.println("Password length: " + password.length());
        System.out.println("Role: " + role);

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Please enter username and password!");
            return;
        }

        if (username.length() < 3) {
            JOptionPane.showMessageDialog(this, "❌ Username must be at least 3 characters!");
            return;
        }

        // ✅ FIX: Check if PasswordUtil exists, if not use plain password
        String hashedPassword;
        try {
            // Try to validate and hash
            String strengthError = PasswordUtil.validatePasswordStrength(password);
            if (strengthError != null) {
                System.out.println("Password validation failed: " + strengthError);
                JOptionPane.showMessageDialog(this,
                        "❌ " + strengthError,
                        "Weak Password",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            hashedPassword = PasswordUtil.hashPassword(password);
            System.out.println("Password hashed successfully");
        } catch (NoClassDefFoundError e) {
            // PasswordUtil not available, use plain password
            System.out.println("⚠️ PasswordUtil not found, using plain password");
            hashedPassword = password;
            JOptionPane.showMessageDialog(this,
                    "⚠️ Warning: Password hashing not available. Using plain password.",
                    "Security Warning",
                    JOptionPane.WARNING_MESSAGE);
        }

        try (Connection conn = DBHelper.getConnection()) {
            System.out.println("Database connection established");

            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                System.out.println("Username already exists");
                JOptionPane.showMessageDialog(this, "❌ Username already exists!");
                return;
            }

            // Insert new user
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            System.out.println("Executing SQL: " + sql);

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "✅ User added successfully!");
                clearForm();
                loadUsers();
                System.out.println("User added successfully!");
            } else {
                System.out.println("No rows were inserted");
                JOptionPane.showMessageDialog(this, "❌ Failed to add user (no rows affected)");
            }

        } catch (Exception ex) {
            System.err.println("=== ERROR ADDING USER ===");
            ex.printStackTrace();

            String errorMsg = "Error adding user:\n" + ex.getMessage();
            if (ex.getCause() != null) {
                errorMsg += "\n\nCause: " + ex.getCause().getMessage();
            }

            JOptionPane.showMessageDialog(this,
                    "❌ " + errorMsg,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("=== END ADD USER DEBUG ===\n");
    }

    // ===== Edit User =====
    private void editUser() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user to edit!");
            return;
        }

        // ✅ Now column 0 has the correct ID
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

    // ===== Reset Password =====
    private void resetPassword() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "❌ Please select a user!");
            return;
        }

        // ✅ Now column 0 has the correct ID
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

            // ✅ FIX: Handle missing PasswordUtil gracefully
            String hashedPassword;
            try {
                String strengthError = PasswordUtil.validatePasswordStrength(newPassword);
                if (strengthError != null) {
                    JOptionPane.showMessageDialog(this,
                            "❌ " + strengthError,
                            "Weak Password",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                hashedPassword = PasswordUtil.hashPassword(newPassword);
            } catch (NoClassDefFoundError e) {
                hashedPassword = newPassword;
                JOptionPane.showMessageDialog(this,
                        "⚠️ Warning: Password hashing not available.",
                        "Security Warning",
                        JOptionPane.WARNING_MESSAGE);
            }

            try (Connection conn = DBHelper.getConnection()) {
                String sql = "UPDATE users SET password=? WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, hashedPassword);
                stmt.setInt(2, id);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "✅ Password reset successfully!");
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

        if (model.getColumnCount() <= 3) {
            JOptionPane.showMessageDialog(this,
                    "❌ Status feature requires database update.\nRun migration_security.sql first.",
                    "Feature Unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ✅ Now column 0 has the correct ID
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

        // ✅ Now column 0 has the correct ID
        int id = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);
        String role = (String) model.getValueAt(row, 2);

        // Prevent deleting the last admin
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

        // ✅ Now column 0 has the correct ID
        int userId = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        try (Connection conn = DBHelper.getConnection()) {
            // Check if audit_logs table exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "audit_logs", null);
            if (!tables.next()) {
                JOptionPane.showMessageDialog(this,
                        "❌ Activity logging not set up.\nRun migration_security.sql first.",
                        "Feature Unavailable",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

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