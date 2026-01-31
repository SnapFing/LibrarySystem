package panels;

import db.DBHelper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BooksPanel extends JPanel {
    private JTextField titleField, authorField, isbnField, publisherField, shelfLocationField, searchField;
    private JSpinner quantitySpinner, publishYearSpinner;
    private JComboBox<String> categoryCombo, searchCategoryCombo;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton, exportPDFButton, manageCategoriesButton, clearSearchButton;
    private Map<String, Integer> categoryMap = new HashMap<>();

    public BooksPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ==== FORM PANEL ====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("📚 Add / Edit Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(20);
        formPanel.add(titleField, gbc);

        // Author
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        formPanel.add(authorField, gbc);

        // ISBN
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("ISBN:"), gbc);
        gbc.gridx = 1;
        isbnField = new JTextField(20);
        formPanel.add(isbnField, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryCombo = new JComboBox<>();
        formPanel.add(categoryCombo, gbc);

        // Publisher
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Publisher:"), gbc);
        gbc.gridx = 1;
        publisherField = new JTextField(20);
        formPanel.add(publisherField, gbc);

        // Publish Year
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Publish Year:"), gbc);
        gbc.gridx = 1;
        publishYearSpinner = new JSpinner(new SpinnerNumberModel(2024, 1800, 2100, 1));
        formPanel.add(publishYearSpinner, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        formPanel.add(quantitySpinner, gbc);

        // Shelf Location
        gbc.gridx = 0; gbc.gridy = 7;
        formPanel.add(new JLabel("Shelf Location:"), gbc);
        gbc.gridx = 1;
        shelfLocationField = new JTextField(20);
        shelfLocationField.setToolTipText("e.g., A1-001, B2-015");
        formPanel.add(shelfLocationField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonRow = new JPanel(new FlowLayout());
        addButton = new JButton("➕ Add Book");
        addButton.addActionListener(e -> handleAddBook());
        manageCategoriesButton = new JButton("📁 Manage Categories");
        manageCategoriesButton.addActionListener(e -> manageCategories());
        buttonRow.add(addButton);
        buttonRow.add(manageCategoriesButton);
        formPanel.add(buttonRow, gbc);

        // ==== SEARCH PANEL ====
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("🔍 Search Books"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5,5,5,5);
        sgbc.fill = GridBagConstraints.HORIZONTAL;

        sgbc.gridx = 0; sgbc.gridy = 0;
        searchPanel.add(new JLabel("Search:"), sgbc);
        sgbc.gridx = 1;
        searchField = new JTextField(20);
        searchField.setToolTipText("Search by title, author, or ISBN");
        searchPanel.add(searchField, sgbc);

        sgbc.gridx = 0; sgbc.gridy = 1;
        searchPanel.add(new JLabel("Category Filter:"), sgbc);
        sgbc.gridx = 1;
        searchCategoryCombo = new JComboBox<>();
        searchCategoryCombo.addItem("-- All Categories --");
        searchPanel.add(searchCategoryCombo, sgbc);

        sgbc.gridx = 0; sgbc.gridy = 2; sgbc.gridwidth = 2;
        JPanel searchButtonPanel = new JPanel(new FlowLayout());
        searchButton = new JButton("🔍 Search");
        searchButton.addActionListener(e -> handleSearch());
        clearSearchButton = new JButton("🔄 Show All");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            searchCategoryCombo.setSelectedIndex(0);
            loadBooksFromDatabase();
        });
        searchButtonPanel.add(searchButton);
        searchButtonPanel.add(clearSearchButton);
        searchPanel.add(searchButtonPanel, sgbc);

        // Top container
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.add(formPanel);
        topPanel.add(searchPanel);
        add(topPanel, BorderLayout.NORTH);

        // ==== TABLE ====
        tableModel = new DefaultTableModel(new String[]{
                "#","Title","Author","ISBN","Category","Publisher","Year","Total","Available","Shelf"},0){
            @Override
            public boolean isCellEditable(int row, int col){ return false; }
        };
        booksTable = new JTable(tableModel);
        booksTable.getColumnModel().getColumn(0).setMaxWidth(50);
        booksTable.getColumnModel().getColumn(6).setMaxWidth(60);
        booksTable.getColumnModel().getColumn(7).setMaxWidth(60);
        booksTable.getColumnModel().getColumn(8).setMaxWidth(80);
        booksTable.getColumnModel().getColumn(9).setMaxWidth(80);
        add(new JScrollPane(booksTable), BorderLayout.CENTER);

        // ==== BUTTONS PANEL ====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        editButton = new JButton("✏️ Edit");
        editButton.setToolTipText("Edit selected book");
        deleteButton = new JButton("🗑️ Delete");
        deleteButton.setToolTipText("Delete selected book");
        exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.setToolTipText("Export books to PDF");

        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(exportPDFButton);

        add(bottomPanel, BorderLayout.SOUTH);

        editButton.addActionListener(e -> handleEdit());
        deleteButton.addActionListener(e -> handleDelete());
        exportPDFButton.addActionListener(e -> exportToPDF());

        loadCategories();
        loadBooksFromDatabase();
    }

    // ===== Load Categories =====
    private void loadCategories() {
        categoryCombo.removeAllItems();
        searchCategoryCombo.removeAllItems();
        categoryMap.clear();

        categoryCombo.addItem("-- Select Category --");
        searchCategoryCombo.addItem("-- All Categories --");

        try(Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, name FROM categories ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                categoryCombo.addItem(name);
                searchCategoryCombo.addItem(name);
                categoryMap.put(name, id);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    // ===== Manage Categories =====
    private void manageCategories() {
        String[] options = {"Add Category", "Delete Category", "View All Categories", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Manage Book Categories", "Categories",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if(choice == 0) { // Add
            String categoryName = JOptionPane.showInputDialog(this, "Enter category name:");
            if(categoryName != null && !categoryName.trim().isEmpty()) {
                String description = JOptionPane.showInputDialog(this, "Enter category description (optional):");

                try(Connection conn = DBHelper.getConnection()) {
                    String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, categoryName.trim());
                    stmt.setString(2, description != null && !description.trim().isEmpty() ? description : null);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "✅ Category added!");
                    loadCategories();
                    loadBooksFromDatabase();
                } catch(Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        } else if(choice == 1) { // Delete
            String[] cats = categoryMap.keySet().toArray(new String[0]);
            if(cats.length == 0) {
                JOptionPane.showMessageDialog(this, "No categories to delete");
                return;
            }
            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select category to delete:", "Delete Category",
                    JOptionPane.QUESTION_MESSAGE, null, cats, cats[0]);

            if(selected != null) {
                try(Connection conn = DBHelper.getConnection()) {
                    // Check if category is in use
                    String checkSql = "SELECT COUNT(*) FROM books WHERE category_id=?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                    checkStmt.setInt(1, categoryMap.get(selected));
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    if(rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "❌ Cannot delete: Category is in use by " + rs.getInt(1) + " book(s)");
                        return;
                    }

                    String sql = "DELETE FROM categories WHERE id=?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, categoryMap.get(selected));
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "✅ Category deleted!");
                    loadCategories();
                    loadBooksFromDatabase();
                } catch(Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        } else if(choice == 2) { // View All
            try(Connection conn = DBHelper.getConnection()) {
                String sql = "SELECT name, description, " +
                        "(SELECT COUNT(*) FROM books WHERE category_id=categories.id) as book_count " +
                        "FROM categories ORDER BY name";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                StringBuilder sb = new StringBuilder("═══════════════════════════════\n");
                sb.append("        ALL CATEGORIES\n");
                sb.append("═══════════════════════════════\n\n");

                while(rs.next()) {
                    sb.append("📁 ").append(rs.getString("name")).append("\n");
                    String desc = rs.getString("description");
                    if(desc != null) {
                        sb.append("   ").append(desc).append("\n");
                    }
                    sb.append("   Books: ").append(rs.getInt("book_count")).append("\n");
                    sb.append("───────────────────────────────\n");
                }

                JTextArea textArea = new JTextArea(sb.toString());
                textArea.setEditable(false);
                textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 400));
                JOptionPane.showMessageDialog(this, scrollPane, "All Categories", JOptionPane.INFORMATION_MESSAGE);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ===== Add Book =====
    private void handleAddBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String publisher = publisherField.getText().trim();
        int publishYear = (int) publishYearSpinner.getValue();
        int quantity = (int) quantitySpinner.getValue();
        String shelfLocation = shelfLocationField.getText().trim();
        String categoryName = (String) categoryCombo.getSelectedItem();

        if(title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Fill all required fields (Title, Author, ISBN)!");
            return;
        }

        if(categoryName == null || categoryName.equals("-- Select Category --")) {
            JOptionPane.showMessageDialog(this,"Please select a category!");
            return;
        }

        Integer categoryId = categoryMap.get(categoryName);
        if(categoryId == null) {
            JOptionPane.showMessageDialog(this,"Invalid category!");
            return;
        }

        try(Connection conn = DBHelper.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM books WHERE isbn=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, isbn);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0){
                JOptionPane.showMessageDialog(this,"❌ Book with this ISBN already exists!");
                return;
            }

            String sql = "INSERT INTO books (title, author, isbn, category_id, publisher, publish_year, " +
                    "total_quantity, available_quantity, shelf_location) VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, isbn);
            stmt.setInt(4, categoryId);
            stmt.setString(5, publisher.isEmpty() ? null : publisher);
            stmt.setInt(6, publishYear);
            stmt.setInt(7, quantity);
            stmt.setInt(8, quantity); // Initially all available
            stmt.setString(9, shelfLocation.isEmpty() ? null : shelfLocation);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Book added successfully!");
            loadBooksFromDatabase();
            clearForm();
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    // ===== Load Books =====
    private void loadBooksFromDatabase(){
        tableModel.setRowCount(0);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "SELECT b.id, b.title, b.author, b.isbn, c.name as category, " +
                    "b.publisher, b.publish_year, b.total_quantity, b.available_quantity, b.shelf_location " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "ORDER BY b.title ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year"),
                        rs.getInt("total_quantity"),
                        rs.getInt("available_quantity"),
                        rs.getString("shelf_location")
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    // ===== Search =====
    private void handleSearch(){
        String keyword = searchField.getText().trim().toLowerCase();
        String selectedCategory = (String) searchCategoryCombo.getSelectedItem();

        // Whitelist validation
        if (selectedCategory !=null &&
            !selectedCategory.equals("--All Categories --") &&
            !categoryMap.containsKey(selectedCategory)) {
            JOptionPane.showMessageDialog(this, "Invalid category selected!");
            return;

        }
        if(keyword.isEmpty() && "-- All Categories --".equals(selectedCategory)) {
            loadBooksFromDatabase();
            return;
        }

        tableModel.setRowCount(0);
        try(Connection conn = DBHelper.getConnection()){
            StringBuilder sql = new StringBuilder(
                    "SELECT b.id, b.title, b.author, b.isbn, c.name as category, " +
                            "b.publisher, b.publish_year, b.total_quantity, b.available_quantity, b.shelf_location " +
                            "FROM books b " +
                            "LEFT JOIN categories c ON b.category_id = c.id WHERE 1=1 ");

            // Add keyword search
            if(!keyword.isEmpty()) {
                sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(b.isbn) LIKE ?) ");
            }

            // Add category filter
            if(selectedCategory != null && !"-- All Categories --".equals(selectedCategory)) {
                sql.append("AND c.name = ? ");
            }

            sql.append("ORDER BY b.title ASC");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            int paramIndex = 1;

            if(!keyword.isEmpty()) {
                String pattern = "%" + keyword + "%";
                stmt.setString(paramIndex++, pattern);
                stmt.setString(paramIndex++, pattern);
                stmt.setString(paramIndex++, pattern);
            }

            if(selectedCategory != null && !"-- All Categories --".equals(selectedCategory)) {
                stmt.setString(paramIndex, selectedCategory);
            }

            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("category"),
                        rs.getString("publisher"),
                        rs.getInt("publish_year"),
                        rs.getInt("total_quantity"),
                        rs.getInt("available_quantity"),
                        rs.getString("shelf_location")
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    // ===== Edit =====
    private void handleEdit(){
        int row = booksTable.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a book to edit");
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        String title = JOptionPane.showInputDialog("New Title:", tableModel.getValueAt(row, 1));
        if(title == null || title.trim().isEmpty()) return;

        String author = JOptionPane.showInputDialog("New Author:", tableModel.getValueAt(row, 2));
        if(author == null || author.trim().isEmpty()) return;

        String isbn = JOptionPane.showInputDialog("New ISBN:", tableModel.getValueAt(row, 3));
        if(isbn == null || isbn.trim().isEmpty()) return;

        String shelfLocation = JOptionPane.showInputDialog("New Shelf Location:", tableModel.getValueAt(row, 9));

        try(Connection conn = DBHelper.getConnection()){
            String sql = "UPDATE books SET title=?, author=?, isbn=?, shelf_location=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, isbn);
            stmt.setString(4, shelfLocation);
            stmt.setInt(5, bookId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Book updated!");
            loadBooksFromDatabase();
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== Delete =====
    private void handleDelete(){
        int row = booksTable.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a book to delete");
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete: " + title + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if(confirm != JOptionPane.YES_OPTION) return;

        try(Connection conn = DBHelper.getConnection()){
            // Check if book is currently borrowed
            String checkSql = "SELECT COUNT(*) FROM borrowed_books WHERE book_id=? AND status='BORROWED'";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, bookId);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Cannot delete: Book is currently borrowed!");
                return;
            }

            String sql = "DELETE FROM books WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bookId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Book deleted!");
            loadBooksFromDatabase();
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== Clear Form =====
    private void clearForm(){
        titleField.setText("");
        authorField.setText("");
        isbnField.setText("");
        publisherField.setText("");
        shelfLocationField.setText("");
        quantitySpinner.setValue(1);
        publishYearSpinner.setValue(2024);
        categoryCombo.setSelectedIndex(0);
    }

    // ===== Export PDF =====
    private void exportToPDF(){
        JFileChooser chooser = new JFileChooser();
        if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try{
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("Books List\n\n"));

            PdfPTable pdfTable = new PdfPTable(tableModel.getColumnCount());
            for(int i = 0; i < tableModel.getColumnCount(); i++) {
                pdfTable.addCell(tableModel.getColumnName(i));
            }

            for(int row = 0; row < tableModel.getRowCount(); row++){
                for(int col = 0; col < tableModel.getColumnCount(); col++){
                    Object value = tableModel.getValueAt(row, col);
                    pdfTable.addCell(value != null ? String.valueOf(value) : "");
                }
            }

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this,"✅ Exported to PDF!");
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error exporting PDF: " + ex.getMessage());
        }
    }
}