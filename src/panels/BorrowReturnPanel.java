package panels;

import db.DBHelper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class BorrowReturnPanel extends JPanel {
    private JComboBox<String> memberCombo, bookCombo;
    private JTextField borrowDateField, dueDateField;
    private JSpinner daysSpinner;
    private JButton borrowButton, returnButton, exportPDFButton, markOverdueButton;
    private JTable table;
    private DefaultTableModel model;

    // Maps to store IDs
    private Map<String, Integer> memberMap = new HashMap<>();
    private Map<String, Integer> bookMap = new HashMap<>();
    private int currentUserId = 1; // TODO: Get from session

    public BorrowReturnPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Form Panel =====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Borrow Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Member Selection
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Select Member:"), gbc);
        gbc.gridx = 1;
        memberCombo = new JComboBox<>();
        memberCombo.addActionListener(e -> checkMemberStatus());
        formPanel.add(memberCombo, gbc);

        // Book Selection
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Select Book:"), gbc);
        gbc.gridx = 1;
        bookCombo = new JComboBox<>();
        formPanel.add(bookCombo, gbc);

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
        borrowButton.addActionListener(e -> handleBorrow());
        JButton refreshButton = new JButton("🔄 Refresh Lists");
        refreshButton.addActionListener(e -> {
            loadMembers();
            loadAvailableBooks();
        });
        btnPanel.add(borrowButton);
        btnPanel.add(refreshButton);
        formPanel.add(btnPanel, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ===== Table =====
        model = new DefaultTableModel(new String[]{
                "Borrow ID", "Member", "Book", "Author", "Borrowed", "Due", "Returned", "Status"}, 0){
            @Override public boolean isCellEditable(int row,int col){ return false; }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== Bottom Buttons =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        returnButton = new JButton("✅ Mark as Returned");
        returnButton.addActionListener(e -> handleReturn());

        markOverdueButton = new JButton("⚠️ Check Overdue");
        markOverdueButton.addActionListener(e -> markOverdueBooks());

        exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.addActionListener(e -> exportToPDF());

        JButton viewFinesButton = new JButton("💰 View Fines");
        viewFinesButton.addActionListener(e -> viewFines());

        bottomPanel.add(returnButton);
        bottomPanel.add(markOverdueButton);
        bottomPanel.add(viewFinesButton);
        bottomPanel.add(exportPDFButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load initial data
        loadMembers();
        loadAvailableBooks();
        loadBorrowRecords();
    }

    // ===== Update Due Date Based on Loan Period =====
    private void updateDueDate() {
        LocalDate borrow = LocalDate.parse(borrowDateField.getText());
        int days = (int) daysSpinner.getValue();
        LocalDate due = borrow.plusDays(days);
        dueDateField.setText(due.toString());
    }

    // ===== Load Members into Dropdown =====
    private void loadMembers() {
        memberCombo.removeAllItems();
        memberMap.clear();

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, fname, lname, email FROM members WHERE is_active = TRUE ORDER BY fname, lname";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String display = rs.getString("fname") + " " + rs.getString("lname") +
                        " (" + rs.getString("email") + ")";
                memberCombo.addItem(display);
                memberMap.put(display, id);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading members: " + ex.getMessage());
        }
    }

    // ===== Load Available Books into Dropdown =====
    private void loadAvailableBooks() {
        bookCombo.removeAllItems();
        bookMap.clear();

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.id, b.title, b.author, b.available_quantity, c.name as category " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "WHERE b.available_quantity > 0 " +
                    "ORDER BY b.title";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                int available = rs.getInt("available_quantity");
                String category = rs.getString("category");

                String display = title + " by " + author +
                        " [" + (category != null ? category : "No Category") +
                        "] (" + available + " available)";
                bookCombo.addItem(display);
                bookMap.put(display, id);
            }

            if (bookCombo.getItemCount() == 0) {
                bookCombo.addItem("-- No books available --");
                borrowButton.setEnabled(false);
            } else {
                borrowButton.setEnabled(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
        }
    }

    // ===== Check Member's Current Borrowing Status =====
    private void checkMemberStatus() {
        String selected = (String) memberCombo.getSelectedItem();
        if (selected == null || !memberMap.containsKey(selected)) return;

        int memberId = memberMap.get(selected);

        try (Connection conn = DBHelper.getConnection()) {
            // Check current borrowed count
            String sql = "SELECT COUNT(*) as borrowed FROM borrowed_books WHERE member_id=? AND status='BORROWED'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int borrowed = rs.getInt("borrowed");
                if (borrowed >= 5) { // Max 5 books per member
                    JOptionPane.showMessageDialog(this,
                            "⚠️ This member has reached the borrowing limit (5 books).",
                            "Limit Reached", JOptionPane.WARNING_MESSAGE);
                }
            }

            // Check for unpaid fines
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
            String sql = "SELECT " +
                    "bb.id, " +
                    "CONCAT(m.fname, ' ', m.lname) AS member_name, " +
                    "b.title, " +
                    "b.author, " +
                    "bb.borrow_date, " +
                    "bb.due_date, " +
                    "bb.return_date, " +
                    "bb.status " +
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

    // ===== Borrow Book =====
    private void handleBorrow() {
        String selectedMember = (String) memberCombo.getSelectedItem();
        String selectedBook = (String) bookCombo.getSelectedItem();

        if (selectedMember == null || selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Please select both member and book!");
            return;
        }

        if (!memberMap.containsKey(selectedMember) || !bookMap.containsKey(selectedBook)) {
            JOptionPane.showMessageDialog(this, "Invalid selection!");
            return;
        }

        int memberId = memberMap.get(selectedMember);
        int bookId = bookMap.get(selectedBook);
        String borrowDate = borrowDateField.getText();
        String dueDate = dueDateField.getText();

        try (Connection conn = DBHelper.getConnection()) {
            // Double-check book availability
            String checkSql = "SELECT available_quantity FROM books WHERE id=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, bookId);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next() || rs.getInt("available_quantity") <= 0) {
                JOptionPane.showMessageDialog(this, "❌ Book is no longer available!");
                loadAvailableBooks();
                return;
            }

            // Insert borrow record (trigger will update available_quantity)
            String sql = "INSERT INTO borrowed_books (member_id, book_id, borrow_date, due_date, status, issued_by) " +
                    "VALUES (?, ?, ?, ?, 'BORROWED', ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            stmt.setInt(2, bookId);
            stmt.setDate(3, Date.valueOf(borrowDate));
            stmt.setDate(4, Date.valueOf(dueDate));
            stmt.setInt(5, currentUserId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Book borrowed successfully!");
            loadBorrowRecords();
            loadAvailableBooks();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error borrowing book: " + ex.getMessage());
        }
    }

    // ===== Return Book =====
    private void handleReturn() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a borrow record!");
            return;
        }

        int borrowId = (int) model.getValueAt(row, 0);
        String status = (String) model.getValueAt(row, 7);

        if ("RETURNED".equals(status)) {
            JOptionPane.showMessageDialog(this, "This book has already been returned!");
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            // Update status (trigger will update available_quantity and calculate fines)
            String sql = "UPDATE borrowed_books SET status='RETURNED', return_date=CURRENT_DATE WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, borrowId);
            stmt.executeUpdate();

            // Check if fine was created
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
            loadAvailableBooks();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
        }
    }

    // ===== Mark Overdue Books =====
    private void markOverdueBooks() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE borrowed_books SET status='OVERDUE' " +
                    "WHERE status='BORROWED' AND due_date < CURRENT_DATE";
            Statement stmt = conn.createStatement();
            int count = stmt.executeUpdate(sql);

            JOptionPane.showMessageDialog(this,
                    "Marked " + count + " book(s) as overdue.",
                    "Overdue Check", JOptionPane.INFORMATION_MESSAGE);
            loadBorrowRecords();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== View Fines =====
    private void viewFines() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT * FROM unpaid_fines_view";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            StringBuilder sb = new StringBuilder("=== UNPAID FINES ===\n\n");
            boolean hasFines = false;

            while (rs.next()) {
                hasFines = true;
                sb.append("Member: ").append(rs.getString("member_name")).append("\n");
                sb.append("Book: ").append(rs.getString("book_title")).append("\n");
                sb.append("Amount: K").append(String.format("%.2f", rs.getDouble("amount"))).append("\n");
                sb.append("Reason: ").append(rs.getString("reason")).append("\n");
                sb.append("---\n");
            }

            if (!hasFines) {
                sb.append("No unpaid fines! 🎉");
            }

            JTextArea textArea = new JTextArea(sb.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(this, scrollPane, "Unpaid Fines", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== Export PDF =====
    private void exportToPDF() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("Borrow Records\n\n"));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            for (int i = 0; i < model.getColumnCount(); i++) {
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            }

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object value = model.getValueAt(row, col);
                    pdfTable.addCell(value != null ? value.toString() : "");
                }
            }

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this, "✅ Exported to PDF successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}