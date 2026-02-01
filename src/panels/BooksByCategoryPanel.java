// Panel for students to browse books organized by categories

package panels.students;

import db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class BooksByCategoryPanel extends JPanel {
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> categoryComboBox;
    private JTextField searchField;
    private JButton searchButton, showAllButton;
    private JLabel categoryInfoLabel, statsLabel;

    public BooksByCategoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Title Panel =====
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("📂 Browse Books by Category");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // ===== Filter Panel =====
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter Options"));

        // Category Selection Row
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        categoryPanel.add(new JLabel("📂 Select Category:"));

        categoryComboBox = new JComboBox<>();
        categoryComboBox.setPreferredSize(new Dimension(250, 30));
        categoryComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loadCategories();
        categoryComboBox.addActionListener(e -> filterByCategory());
        categoryPanel.add(categoryComboBox);

        // Stats Label
        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsLabel.setForeground(new Color(0, 120, 215));
        categoryPanel.add(Box.createHorizontalStrut(20));
        categoryPanel.add(statsLabel);

        filterPanel.add(categoryPanel);

        // Category Info Label
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        categoryInfoLabel = new JLabel(" ");
        categoryInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        categoryInfoLabel.setForeground(Color.GRAY);
        infoPanel.add(categoryInfoLabel);
        filterPanel.add(infoPanel);

        // Search Row
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("🔍 Search in Category:"));
        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchButton = new JButton("Search");
        showAllButton = new JButton("🔄 Show All Categories");

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);

        filterPanel.add(searchPanel);

        add(filterPanel, BorderLayout.NORTH);

        // ===== Table =====
        tableModel = new DefaultTableModel(
                new String[]{"Title", "Author", "Category", "Year", "Available Copies"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        booksTable = new JTable(tableModel);
        booksTable.setRowHeight(28);
        booksTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        booksTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        booksTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        booksTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        booksTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        booksTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        booksTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available Books"));
        add(scrollPane, BorderLayout.CENTER);

        // ===== Bottom Info Panel =====
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel infoBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("💡 Select a category to browse books. To borrow a book, please visit the library or contact a librarian.");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoBottomPanel.add(infoLabel);

        bottomPanel.add(infoBottomPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ===== Actions =====
        searchButton.addActionListener(e -> searchInCategory());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            categoryComboBox.setSelectedIndex(0);
            loadAllBooks();
        });

        // Add Enter key listener for search field
        searchField.addActionListener(e -> searchInCategory());

        // Load initial data (all books)
        loadAllBooks();
    }

    /**
     * Load all available categories from the database
     */
    private void loadCategories() {
        categoryComboBox.removeAllItems();
        categoryComboBox.addItem("-- All Categories --");

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, name, description FROM categories ORDER BY name ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String categoryName = rs.getString("name");
                categoryComboBox.addItem(categoryName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading categories: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load all available books (no category filter)
     */
    private void loadAllBooks() {
        tableModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.title, b.author, c.name as category_name, " +
                    "b.publish_year, b.available_quantity " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "WHERE b.available_quantity > 0 " +
                    "ORDER BY b.title ASC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category_name") != null ? rs.getString("category_name") : "Uncategorized",
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                });
                count++;
            }

            statsLabel.setText("📊 Showing " + count + " available books");
            categoryInfoLabel.setText("All categories displayed");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading books: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Filter books by selected category
     */
    private void filterByCategory() {
        String selectedCategory = (String) categoryComboBox.getSelectedItem();

        if (selectedCategory == null || selectedCategory.equals("-- All Categories --")) {
            loadAllBooks();
            return;
        }

        tableModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            // First, get category description for info display
            String descSql = "SELECT description FROM categories WHERE name = ?";
            PreparedStatement descStmt = conn.prepareStatement(descSql);
            descStmt.setString(1, selectedCategory);
            ResultSet descRs = descStmt.executeQuery();

            String description = "";
            if (descRs.next()) {
                description = descRs.getString("description");
                categoryInfoLabel.setText("ℹ️ " + description);
            }

            // Then, get books in that category
            String sql = "SELECT b.title, b.author, c.name as category_name, " +
                    "b.publish_year, b.available_quantity " +
                    "FROM books b " +
                    "INNER JOIN categories c ON b.category_id = c.id " +
                    "WHERE c.name = ? AND b.available_quantity > 0 " +
                    "ORDER BY b.title ASC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, selectedCategory);
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category_name"),
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                });
                count++;
            }

            if (count == 0) {
                statsLabel.setText("❌ No available books in this category");
                JOptionPane.showMessageDialog(this,
                        "No books available in the '" + selectedCategory + "' category at the moment.",
                        "No Books Found",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                statsLabel.setText("📊 Found " + count + " available book(s) in " + selectedCategory);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error filtering books: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Search for books within the selected category (or all if no category selected)
     */
    private void searchInCategory() {
        String keyword = searchField.getText().trim();
        String selectedCategory = (String) categoryComboBox.getSelectedItem();

        if (keyword.isEmpty()) {
            if (selectedCategory == null || selectedCategory.equals("-- All Categories --")) {
                loadAllBooks();
            } else {
                filterByCategory();
            }
            return;
        }

        tableModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql;
            PreparedStatement stmt;

            if (selectedCategory == null || selectedCategory.equals("-- All Categories --")) {
                // Search in all categories
                sql = "SELECT b.title, b.author, c.name as category_name, " +
                        "b.publish_year, b.available_quantity " +
                        "FROM books b " +
                        "LEFT JOIN categories c ON b.category_id = c.id " +
                        "WHERE b.available_quantity > 0 " +
                        "AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ?) " +
                        "ORDER BY b.title ASC";

                stmt = conn.prepareStatement(sql);
                stmt.setString(1, "%" + keyword.toLowerCase() + "%");
                stmt.setString(2, "%" + keyword.toLowerCase() + "%");
            } else {
                // Search within selected category
                sql = "SELECT b.title, b.author, c.name as category_name, " +
                        "b.publish_year, b.available_quantity " +
                        "FROM books b " +
                        "INNER JOIN categories c ON b.category_id = c.id " +
                        "WHERE c.name = ? AND b.available_quantity > 0 " +
                        "AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ?) " +
                        "ORDER BY b.title ASC";

                stmt = conn.prepareStatement(sql);
                stmt.setString(1, selectedCategory);
                stmt.setString(2, "%" + keyword.toLowerCase() + "%");
                stmt.setString(3, "%" + keyword.toLowerCase() + "%");
            }

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category_name") != null ? rs.getString("category_name") : "Uncategorized",
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                });
                count++;
            }

            if (count == 0) {
                statsLabel.setText("❌ No results found for \"" + keyword + "\"");
                String message = selectedCategory != null && !selectedCategory.equals("-- All Categories --")
                        ? "No books found matching \"" + keyword + "\" in the '" + selectedCategory + "' category."
                        : "No books found matching \"" + keyword + "\".";
                JOptionPane.showMessageDialog(this,
                        message,
                        "No Results",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                String categoryText = selectedCategory != null && !selectedCategory.equals("-- All Categories --")
                        ? " in " + selectedCategory
                        : "";
                statsLabel.setText("🔍 Found " + count + " result(s) for \"" + keyword + "\"" + categoryText);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error searching books: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}