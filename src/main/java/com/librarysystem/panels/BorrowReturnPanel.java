package com.librarysystem.panels;

import com.librarysystem.LibrarySystemUI;
import com.librarysystem.db.DBHelper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.librarysystem.utils.TransactionManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;

public class BorrowReturnPanel extends JPanel {
    private JTextField memberSearchField, bookSearchField;
    private JTextField borrowDateField, dueDateField;
    private JSpinner daysSpinner;
    private JButton borrowButton, returnButton, exportPDFButton, markOverdueButton;
    private JTable table;
    private DefaultTableModel model;

    // Store selected IDs
    private Integer selectedMemberId = null;
    private Integer selectedBookId = null;

    // Popup menus for autocomplete
    private JPopupMenu memberPopup;
    private JPopupMenu bookPopup;

    public BorrowReturnPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Form Panel =====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("📚 Borrow Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Member Search with Autocomplete
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel memberLabel = new JLabel("Member Name:");
        memberLabel.setToolTipText("Start typing to search members");
        formPanel.add(memberLabel, gbc);

        gbc.gridx = 1;
        memberSearchField = new JTextField(20);
        memberSearchField.setToolTipText("Type member's full name (e.g., Sarah Wilson)");
        setupMemberAutocomplete();
        formPanel.add(memberSearchField, gbc);

        // Book Search with Autocomplete
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel bookLabel = new JLabel("Book Title:");
        bookLabel.setToolTipText("Start typing to search books");
        formPanel.add(bookLabel, gbc);

        gbc.gridx = 1;
        bookSearchField = new JTextField(20);
        bookSearchField.setToolTipText("Type book title (e.g., To Kill a Mockingbird)");
        setupBookAutocomplete();
        formPanel.add(bookSearchField, gbc);

        // Borrow Date
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Borrow Date:"), gbc);
        gbc.gridx = 1;
        borrowDateField = new JTextField(10);
        borrowDateField.setText(LocalDate.now().toString());
        borrowDateField.setEditable(false);
        formPanel.add(borrowDateField, gbc);

        // Loan Period (Days)
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Loan Period (Days):"), gbc);
        gbc.gridx = 1;
        daysSpinner = new JSpinner(new SpinnerNumberModel(14, 1, 90, 1));
        daysSpinner.addChangeListener(e -> updateDueDate());
        formPanel.add(daysSpinner, gbc);

        // Due Date (Auto-calculated)
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1;
        dueDateField = new JTextField(10);
        dueDateField.setEditable(false);
        updateDueDate();
        formPanel.add(dueDateField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout());
        borrowButton = new JButton("📚 Borrow Book");
        borrowButton.setToolTipText("Borrow selected book for selected member");
        borrowButton.addActionListener(e -> handleBorrow());
        JButton refreshButton = new JButton("🔄 Clear Form");
        refreshButton.setToolTipText("Clear all fields");
        refreshButton.addActionListener(e -> clearForm());
        btnPanel.add(borrowButton);
        btnPanel.add(refreshButton);
        formPanel.add(btnPanel, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ===== Table =====
        model = new DefaultTableModel(new String[]{
                "ID", "Member", "Book", "Author", "Borrowed", "Due", "Returned", "Status"}, 0){
            @Override public boolean isCellEditable(int row,int col){ return false; }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== Bottom Buttons =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        returnButton = new JButton("✅ Mark as Returned");
        returnButton.setToolTipText("Mark selected borrow record as returned");
        returnButton.addActionListener(e -> handleReturn());

        markOverdueButton = new JButton("⚠️ Check Overdue");
        markOverdueButton.setToolTipText("Mark overdue books automatically");
        markOverdueButton.addActionListener(e -> markOverdueBooks());

        exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.setToolTipText("Export borrow records to PDF");
        exportPDFButton.addActionListener(e -> exportToPDF());

        JButton viewFinesButton = new JButton("💰 View Fines");
        viewFinesButton.setToolTipText("View unpaid fines summary");
        viewFinesButton.addActionListener(e -> viewFines());

        bottomPanel.add(returnButton);
        bottomPanel.add(markOverdueButton);
        bottomPanel.add(viewFinesButton);
        bottomPanel.add(exportPDFButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load initial data
        loadBorrowRecords();
    }

    // ===== Member Autocomplete =====
    private void setupMemberAutocomplete() {
        memberPopup = new JPopupMenu();
        memberPopup.setFocusable(false);

        memberSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchMembers(); }
            public void removeUpdate(DocumentEvent e) { searchMembers(); }
            public void changedUpdate(DocumentEvent e) { searchMembers(); }

            private void searchMembers() {
                String text = memberSearchField.getText().trim();

                // Clear selection if field is modified
                if (selectedMemberId != null) {
                    selectedMemberId = null;
                    memberSearchField.setForeground(Color.BLACK);
                }

                if (text.length() < 1) {
                    memberPopup.setVisible(false);
                    return;
                }

                SwingUtilities.invokeLater(() -> performMemberSearch(text));
            }
        });
    }

    private void performMemberSearch(String text) {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, fname, lname, email, phone FROM members " +
                    "WHERE (LOWER(fname) LIKE ? OR LOWER(lname) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ?) " +
                    "AND is_active = TRUE " +
                    "ORDER BY fname, lname LIMIT 15";
            PreparedStatement stmt = conn.prepareStatement(sql);
            String pattern = "%" + text.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            ResultSet rs = stmt.executeQuery();

            memberPopup.removeAll();
            boolean hasResults = false;

            while (rs.next()) {
                hasResults = true;
                final int id = rs.getInt("id");
                String fname = rs.getString("fname");
                String lname = rs.getString("lname");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                final String fullName = fname + " " + lname;
                String display = String.format("ID:%d | %s | %s | %s", id, fullName, email, phone);

                JMenuItem item = new JMenuItem(display);
                item.setFont(new Font("Monospaced", Font.PLAIN, 11));

                item.addActionListener(ae -> {
                    selectedMemberId = id;
                    memberSearchField.setText(fullName);
                    memberSearchField.setForeground(new Color(0, 150, 0));
                    memberPopup.setVisible(false);
                    checkMemberStatus(id);
                });

                memberPopup.add(item);
            }

            if (hasResults) {
                memberPopup.show(memberSearchField, 0, memberSearchField.getHeight());
            } else {
                memberPopup.setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ===== Book Autocomplete =====
    private void setupBookAutocomplete() {
        bookPopup = new JPopupMenu();
        bookPopup.setFocusable(false);

        bookSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchBooks(); }
            public void removeUpdate(DocumentEvent e) { searchBooks(); }
            public void changedUpdate(DocumentEvent e) { searchBooks(); }

            private void searchBooks() {
                String text = bookSearchField.getText().trim();

                // Clear selection if field is modified
                if (selectedBookId != null) {
                    selectedBookId = null;
                    bookSearchField.setForeground(Color.BLACK);
                }

                if (text.length() < 1) {
                    bookPopup.setVisible(false);
                    return;
                }

                SwingUtilities.invokeLater(() -> performBookSearch(text));
            }
        });
    }

    private void performBookSearch(String text) {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.id, b.title, b.author, b.isbn, b.available_quantity, c.name as category " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "WHERE (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(b.isbn) LIKE ?) " +
                    "AND b.available_quantity > 0 " +
                    "ORDER BY b.title LIMIT 15";
            PreparedStatement stmt = conn.prepareStatement(sql);
            String pattern = "%" + text.toLowerCase() + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            ResultSet rs = stmt.executeQuery();

            bookPopup.removeAll();
            boolean hasResults = false;

            while (rs.next()) {
                hasResults = true;
                final int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String isbn = rs.getString("isbn");
                int available = rs.getInt("available_quantity");
                String category = rs.getString("category");

                final String bookTitle = title;
                String display = String.format("ID:%d | %s by %s [%s] - %d available",
                        id, title, author,
                        (category != null ? category : "Uncategorized"),
                        available);

                JMenuItem item = new JMenuItem(display);
                item.setFont(new Font("Monospaced", Font.PLAIN, 11));

                item.addActionListener(ae -> {
                    selectedBookId = id;
                    bookSearchField.setText(bookTitle);
                    bookSearchField.setForeground(new Color(0, 150, 0));
                    bookPopup.setVisible(false);
                });

                bookPopup.add(item);
            }

            if (hasResults) {
                bookPopup.show(bookSearchField, 0, bookSearchField.getHeight());
            } else {
                bookPopup.setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ===== Helper: Find Member ID by Name =====
    private Integer findMemberIdByName(String fullName) {
        try (Connection conn = DBHelper.getConnection()) {
            // Try exact match first
            String sql = "SELECT id FROM members WHERE CONCAT(fname, ' ', lname) = ? AND is_active = TRUE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            // Try case-insensitive match
            sql = "SELECT id FROM members WHERE LOWER(CONCAT(fname, ' ', lname)) = LOWER(?) AND is_active = TRUE";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, fullName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            return null;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // ===== Helper: Find Book ID by Title =====
    private Integer findBookIdByTitle(String title) {
        try (Connection conn = DBHelper.getConnection()) {
            // Try exact match first
            String sql = "SELECT id FROM books WHERE title = ? AND available_quantity > 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            // Try case-insensitive match
            sql = "SELECT id FROM books WHERE LOWER(title) = LOWER(?) AND available_quantity > 0";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            return null;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // ===== Update Due Date =====
    private void updateDueDate() {
        try {
            LocalDate borrow = LocalDate.parse(borrowDateField.getText());
            int days = (int) daysSpinner.getValue();
            LocalDate due = borrow.plusDays(days);
            dueDateField.setText(due.toString());
        } catch (Exception ex) {
            dueDateField.setText("Invalid date");
        }
    }

    // ===== Check Member Status =====
    private void checkMemberStatus(int memberId) {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT COUNT(*) as borrowed FROM borrowed_books WHERE member_id=? AND status='BORROWED'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int borrowed = rs.getInt("borrowed");
                if (borrowed >= 5) {
                    JOptionPane.showMessageDialog(this,
                            "⚠️ This member has reached the borrowing limit (5 books).\nCurrent borrowed: " + borrowed,
                            "Limit Reached", JOptionPane.WARNING_MESSAGE);
                }
            }

            sql = "SELECT SUM(f.amount) as total_fines FROM fines f " +
                    "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                    "WHERE bb.member_id=? AND f.paid=FALSE";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                double fines = rs.getDouble("total_fines");
                if (fines > 0) {
                    JOptionPane.showMessageDialog(this,
                            "⚠️ This member has unpaid fines: K" + String.format("%.2f", fines),
                            "Unpaid Fines", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== Load Borrow Records =====
    private void loadBorrowRecords() {
        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT bb.id, CONCAT(m.fname, ' ', m.lname) AS member_name, b.title, b.author, " +
                    "bb.borrow_date, bb.due_date, bb.return_date, bb.status " +
                    "FROM borrowed_books bb " +
                    "JOIN members m ON bb.member_id = m.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "ORDER BY bb.id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("member_name"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDate("borrow_date"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading records: " + ex.getMessage());
        }
    }

    // ===== Borrow Book with Transaction Management =====
    private void handleBorrow() {
        String memberName = memberSearchField.getText().trim();
        String bookTitle = bookSearchField.getText().trim();

        if (memberName.isEmpty() || bookTitle.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "❌ Please enter both member name and book title!",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Auto-resolve member if not already selected
        if (selectedMemberId == null) {
            selectedMemberId = findMemberIdByName(memberName);
            if (selectedMemberId == null) {
                JOptionPane.showMessageDialog(this,
                        "❌ Member '" + memberName + "' not found!\n\nPlease check the spelling or select from the dropdown.",
                        "Member Not Found", JOptionPane.WARNING_MESSAGE);
                memberSearchField.requestFocus();
                memberSearchField.setForeground(Color.RED);
                return;
            }
            memberSearchField.setForeground(new Color(0, 150, 0));
        }

        // Auto-resolve book if not already selected
        if (selectedBookId == null) {
            selectedBookId = findBookIdByTitle(bookTitle);
            if (selectedBookId == null) {
                JOptionPane.showMessageDialog(this,
                        "❌ Book '" + bookTitle + "' not found or unavailable!\n\nPlease check the spelling or select from the dropdown.",
                        "Book Not Found", JOptionPane.WARNING_MESSAGE);
                bookSearchField.requestFocus();
                bookSearchField.setForeground(Color.RED);
                return;
            }
            bookSearchField.setForeground(new Color(0, 150, 0));
        }

        String borrowDate = borrowDateField.getText();
        String dueDate = dueDateField.getText();

        // Execute borrow operation as a transaction
        TransactionManager.TransactionResult<Integer> result = TransactionManager.executeSafe(conn -> {
            // Step 1: Check if book is still available (within transaction)
            String checkSql = "SELECT available_quantity FROM books WHERE id=? FOR UPDATE"; // Lock row
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, selectedBookId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next() || rs.getInt("available_quantity") <= 0) {
                throw new SQLException("Book is no longer available");
            }

            // Step 2: Check member's borrowing limit
            String limitSql = "SELECT COUNT(*) as borrowed FROM borrowed_books WHERE member_id=? AND status='BORROWED'";
            PreparedStatement limitStmt = conn.prepareStatement(limitSql);
            limitStmt.setInt(1, selectedMemberId);
            ResultSet limitRs = limitStmt.executeQuery();
            limitRs.next();
            if (limitRs.getInt("borrowed") >= 5) {
                throw new SQLException("Member has reached the borrowing limit (5 books)");
            }

            // Step 3: Check for unpaid fines
            String fineSql = "SELECT SUM(f.amount) as total_fines FROM fines f " +
                    "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                    "WHERE bb.member_id=? AND f.paid=FALSE";
            PreparedStatement fineStmt = conn.prepareStatement(fineSql);
            fineStmt.setInt(1, selectedMemberId);
            ResultSet fineRs = fineStmt.executeQuery();
            fineRs.next();
            double unpaidFines = fineRs.getDouble("total_fines");
            if (unpaidFines > 0) {
                throw new SQLException("Member has unpaid fines: K" + String.format("%.2f", unpaidFines));
            }

            // Step 4: Insert borrow record
            String insertSql = "INSERT INTO borrowed_books (member_id, book_id, borrow_date, due_date, status, issued_by) " +
                    "VALUES (?, ?, ?, ?, 'BORROWED', ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setInt(1, selectedMemberId);
            insertStmt.setInt(2, selectedBookId);
            insertStmt.setDate(3, Date.valueOf(borrowDate));
            insertStmt.setDate(4, Date.valueOf(dueDate));
            insertStmt.setInt(5, LibrarySystemUI.getCurrentUserId());
            insertStmt.executeUpdate();

            // Get generated borrow ID
            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            int borrowId = -1;
            if (generatedKeys.next()) {
                borrowId = generatedKeys.getInt(1);
            }

            // Step 5: Update book's available quantity
            String updateBookSql = "UPDATE books SET available_quantity = available_quantity - 1 WHERE id=?";
            PreparedStatement updateBookStmt = conn.prepareStatement(updateBookSql);
            updateBookStmt.setInt(1, selectedBookId);
            updateBookStmt.executeUpdate();

            // Step 6: Log the transaction in audit
            String auditSql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, 'BOOK_BORROWED', ?)";
            PreparedStatement auditStmt = conn.prepareStatement(auditSql);
            auditStmt.setInt(1, LibrarySystemUI.getCurrentUserId());
            auditStmt.setString(2, "Borrowed book ID " + selectedBookId + " to member ID " + selectedMemberId);
            auditStmt.executeUpdate();

            return borrowId; // Return the new borrow ID
        });

        // Handle result
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "✅ Book borrowed successfully!\n\nBorrow ID: " + result.getData(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            loadBorrowRecords();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Failed to borrow book:\n" + result.getErrorMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== Return Book =====
    private void handleReturn() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a borrow record from the table!");
            return;
        }

        int borrowId = (int) model.getValueAt(row, 0);
        String status = (String) model.getValueAt(row, 7);

        if ("RETURNED".equals(status)) {
            JOptionPane.showMessageDialog(this, "This book has already been returned!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark Borrow ID " + borrowId + " as returned?",
                "Confirm Return", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE borrowed_books SET status='RETURNED', return_date=CURRENT_DATE WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, borrowId);
            stmt.executeUpdate();

            String fineSql = "SELECT amount FROM fines WHERE borrow_id=? AND paid=FALSE";
            PreparedStatement fineStmt = conn.prepareStatement(fineSql);
            fineStmt.setInt(1, borrowId);
            ResultSet rs = fineStmt.executeQuery();

            if (rs.next()) {
                double fine = rs.getDouble("amount");
                JOptionPane.showMessageDialog(this,
                        "✅ Book returned!\n⚠️ Late return fine: K" + String.format("%.2f", fine),
                        "Returned with Fine", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "✅ Book returned successfully!");
            }

            loadBorrowRecords();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
        }
    }

    // ===== Mark Overdue =====
    private void markOverdueBooks() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE borrowed_books SET status='OVERDUE' WHERE status='BORROWED' AND due_date < CURRENT_DATE";
            Statement stmt = conn.createStatement();
            int count = stmt.executeUpdate(sql);
            JOptionPane.showMessageDialog(this, "✅ Marked " + count + " book(s) as overdue.", "Overdue Check", JOptionPane.INFORMATION_MESSAGE);
            loadBorrowRecords();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== View Fines =====
    private void viewFines() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT * FROM unpaid_fines_view ORDER BY member_name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            StringBuilder sb = new StringBuilder("═══════════════ UNPAID FINES ═══════════════\n\n");
            boolean hasFines = false;
            double totalFines = 0;

            while (rs.next()) {
                hasFines = true;
                double amount = rs.getDouble("amount");
                totalFines += amount;
                sb.append("Borrow ID: ").append(rs.getInt("borrow_id")).append("\n");
                sb.append("Member: ").append(rs.getString("member_name")).append("\n");
                sb.append("Book: ").append(rs.getString("book_title")).append("\n");
                sb.append("Amount: K").append(String.format("%.2f", amount)).append("\n");
                sb.append("Reason: ").append(rs.getString("reason")).append("\n");
                sb.append("─────────────────────────────────────────\n");
            }

            if (!hasFines) {
                sb.append("✅ No unpaid fines! All clear! 🎉");
            } else {
                sb.append("\n═══════════════════════════════════════════\n");
                sb.append("TOTAL UNPAID FINES: K").append(String.format("%.2f", totalFines)).append("\n");
                sb.append("═══════════════════════════════════════════");
            }

            JTextArea textArea = new JTextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(550, 450));
            JOptionPane.showMessageDialog(this, scrollPane, "Unpaid Fines Summary", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage());
        }
    }

    // ===== Clear Form =====
    private void clearForm() {
        memberSearchField.setText("");
        memberSearchField.setForeground(Color.BLACK);
        bookSearchField.setText("");
        bookSearchField.setForeground(Color.BLACK);
        selectedMemberId = null;
        selectedBookId = null;
        daysSpinner.setValue(14);
        borrowDateField.setText(LocalDate.now().toString());
        updateDueDate();
    }

    // ===== Export PDF =====
    private void exportToPDF() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Borrow Records as PDF");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("Library Borrow Records\n"));
            doc.add(new Paragraph("Generated: " + LocalDate.now() + "\n\n"));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);

            // Headers
            for (int i = 0; i < model.getColumnCount(); i++) {
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            }

            // Data
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    pdfTable.addCell(value != null ? value.toString() : "");
                }
            }

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this, "✅ Exported " + model.getRowCount() + " records to PDF successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage());
        }
    }
}