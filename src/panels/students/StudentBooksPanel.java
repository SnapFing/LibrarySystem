// Panel for students to view listed books available

package panels.students;

import db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StudentBooksPanel extends JPanel {
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;

    public StudentBooksPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Title Panel =====
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("📚 Available Books");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // ===== Search Panel =====
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Books"));

        searchField = new JTextField(25);
        searchButton = new JButton("🔍 Search");
        JButton clearButton = new JButton("🔄 Show All");

        searchPanel.add(new JLabel("Title/Author:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);

        add(searchPanel, BorderLayout.AFTER_LAST_LINE);

        // ===== Table =====
        tableModel = new DefaultTableModel(
                new String[]{"Title", "Author", "Year Published", "Available Copies"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        booksTable = new JTable(tableModel);
        booksTable.setRowHeight(25);
        booksTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        booksTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        booksTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        booksTable.getColumnModel().getColumn(3).setPreferredWidth(120);

        add(new JScrollPane(booksTable), BorderLayout.CENTER);

        // ===== Info Panel =====
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("💡 To borrow a book, please visit the library or contact a librarian.");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);

        // ===== Actions =====
        searchButton.addActionListener(e -> searchBooks());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadBooks();
        });

        // Load initial data
        loadBooks();
    }

    private void loadBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            // ✅ FIXED: Use available_quantity instead of calculating (total_quantity - borrowed)
            String sql = "SELECT title, author, publish_year, available_quantity " +
                    "FROM books WHERE available_quantity > 0 ORDER BY title ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading books: " + ex.getMessage());
        }
    }

    private void searchBooks() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadBooks();
            return;
        }

        tableModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            // ✅ FIXED: Use available_quantity
            String sql = "SELECT title, author, publish_year, available_quantity " +
                    "FROM books WHERE available_quantity > 0 " +
                    "AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ?) " +
                    "ORDER BY title ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + keyword.toLowerCase() + "%");
            stmt.setString(2, "%" + keyword.toLowerCase() + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error searching books: " + ex.getMessage());
        }
    }
}