package panels;

import db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementPanel extends JPanel {
    private JTable usersTable;
    private DefaultTableModel model;

    public UserManagementPanel() {
        setLayout(new BorderLayout());

        // Table model
        model = new DefaultTableModel(new String[]{"ID", "Username", "Role"}, 0);
        usersTable = new JTable(model);
        usersTable.setFillsViewportHeight(true);
        usersTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS); // Keep other columns filling space

        add(new JScrollPane(usersTable), BorderLayout.CENTER);

        // Buttons
        JButton addUserBtn = new JButton("➕ Add User");
        JButton deleteUserBtn = new JButton("🗑️ Delete User");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addUserBtn);
        buttonPanel.add(deleteUserBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load users
        loadUsers();

        // Adjust only ID column width
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        // Add user action
        addUserBtn.addActionListener(e -> addUser());

        // Delete user action
        deleteUserBtn.addActionListener(e -> deleteUser());
    }

    private void loadUsers() {
        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM users")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
        }
    }

    private void addUser() {
        String username = JOptionPane.showInputDialog(this, "Enter username:");
        String password = JOptionPane.showInputDialog(this, "Enter password:");
        String[] roles = {"Admin", "Librarian"};
        String role = (String) JOptionPane.showInputDialog(this, "Select role:", "Role",
                JOptionPane.QUESTION_MESSAGE, null, roles, roles[1]);

        if (username != null && password != null && role != null) {
            try (Connection conn = DBHelper.getConnection()) {
                String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, role);
                stmt.executeUpdate();
                loadUsers();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding user: " + ex.getMessage());
            }
        }
    }

    private void deleteUser() {
        int row = usersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to delete.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "DELETE FROM users WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            loadUsers();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage());
        }
    }
}
