package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/**
 * BorrowRequestsPanel
 * Shown to Admin and Librarian.
 * Lists all student borrow requests; allows Approve or Reject.
 *
 * Approve flow:
 *   1. Validate book still has available copies.
 *   2. Insert row into borrowed_books (status=BORROWED, due_date = today + 14 days).
 *   3. Decrement books.available_quantity by 1.
 *   4. Mark borrow_request as APPROVED.
 *
 * Reject flow:
 *   1. Prompt for optional reason.
 *   2. Mark borrow_request as REJECTED, store reason in notes.
 *   3. No book quantity change.
 */
public class BorrowRequestsPanel extends JPanel {

    // ── Column indices in the table model ──────────────────────────────────────
    private static final int COL_REQ_ID    = 0;
    private static final int COL_STUDENT   = 1;
    private static final int COL_BOOK      = 2;
    private static final int COL_AUTHOR    = 3;
    private static final int COL_COPIES    = 4;
    private static final int COL_DATE      = 5;
    private static final int COL_STATUS    = 6;
    // Hidden columns (not displayed but kept for SQL lookups)
    private static final int COL_MEMBER_ID = 7;
    private static final int COL_BOOK_ID   = 8;

    private JTable requestsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterCombo;
    private JLabel summaryLabel;

    // ── Default loan period ────────────────────────────────────────────────────
    private static final int LOAN_DAYS = 14;

    public BorrowRequestsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Title bar ─────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel titleLabel = new JLabel("📋 Borrow Requests");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        summaryLabel.setForeground(Color.GRAY);

        JPanel titleLeft = new JPanel();
        titleLeft.setLayout(new BoxLayout(titleLeft, BoxLayout.Y_AXIS));
        titleLeft.setOpaque(false);
        titleLeft.add(titleLabel);
        titleLeft.add(summaryLabel);

        // Filter combo
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Filter:"));
        filterCombo = new JComboBox<>(new String[]{"PENDING", "APPROVED", "REJECTED", "ALL"});
        filterCombo.addActionListener(e -> loadRequests());
        filterPanel.add(filterCombo);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadRequests());
        filterPanel.add(refreshBtn);

        titleBar.add(titleLeft, BorderLayout.WEST);
        titleBar.add(filterPanel, BorderLayout.EAST);
        add(titleBar, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(
                new String[]{"Req #", "Student", "Book Title", "Author",
                        "Copies Left", "Requested On", "Status",
                        "member_id", "book_id"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        requestsTable = new JTable(tableModel);
        requestsTable.setRowHeight(30);
        requestsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        requestsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Hide the internal ID columns from the user
        hideColumn(COL_MEMBER_ID);
        hideColumn(COL_BOOK_ID);

        // Column widths
        requestsTable.getColumnModel().getColumn(COL_REQ_ID).setMaxWidth(60);
        requestsTable.getColumnModel().getColumn(COL_STUDENT).setPreferredWidth(150);
        requestsTable.getColumnModel().getColumn(COL_BOOK).setPreferredWidth(250);
        requestsTable.getColumnModel().getColumn(COL_AUTHOR).setPreferredWidth(160);
        requestsTable.getColumnModel().getColumn(COL_COPIES).setMaxWidth(90);
        requestsTable.getColumnModel().getColumn(COL_DATE).setPreferredWidth(160);
        requestsTable.getColumnModel().getColumn(COL_STATUS).setMaxWidth(90);

        // Row colour by status
        requestsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    String status = String.valueOf(tableModel.getValueAt(row, COL_STATUS));
                    switch (status) {
                        case "PENDING"  -> c.setBackground(new Color(255, 255, 220));
                        case "APPROVED" -> c.setBackground(new Color(220, 255, 220));
                        case "REJECTED" -> c.setBackground(new Color(255, 220, 220));
                        default         -> c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(requestsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Student Book Requests"));
        add(scrollPane, BorderLayout.CENTER);

        // ── Action buttons ────────────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton approveBtn = new JButton("✅ Approve");
        approveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        approveBtn.setBackground(new Color(40, 167, 69));
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setOpaque(true);
        approveBtn.setToolTipText("Approve this request and create a borrow record");
        approveBtn.addActionListener(e -> handleApprove());

        JButton rejectBtn = new JButton("❌ Reject");
        rejectBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rejectBtn.setBackground(new Color(220, 53, 69));
        rejectBtn.setForeground(Color.WHITE);
        rejectBtn.setOpaque(true);
        rejectBtn.setToolTipText("Reject this request (optionally provide a reason)");
        rejectBtn.addActionListener(e -> handleReject());

        JButton detailsBtn = new JButton("🔍 Details");
        detailsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailsBtn.addActionListener(e -> showDetails());

        buttonPanel.add(approveBtn);
        buttonPanel.add(rejectBtn);
        buttonPanel.add(detailsBtn);

        // Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        legendPanel.add(makeLegendDot(new Color(255, 255, 180), "Pending"));
        legendPanel.add(makeLegendDot(new Color(180, 255, 180), "Approved"));
        legendPanel.add(makeLegendDot(new Color(255, 180, 180), "Rejected"));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonPanel, BorderLayout.CENTER);
        south.add(legendPanel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        loadRequests();
    }

    // ── Load / refresh ─────────────────────────────────────────────────────────
    private void loadRequests() {
        tableModel.setRowCount(0);
        String filter = (String) filterCombo.getSelectedItem();

        StringBuilder sql = new StringBuilder(
                "SELECT br.id, m.name AS student, b.title, b.author, " +
                        "       b.available_quantity, br.request_date, br.status, " +
                        "       br.member_id, br.book_id " +
                        "FROM borrow_requests br " +
                        "JOIN members m ON br.member_id = m.id " +
                        "JOIN books   b ON br.book_id   = b.id ");

        if (!"ALL".equals(filter)) {
            sql.append("WHERE br.status = '").append(filter).append("' ");
        }
        sql.append("ORDER BY br.request_date DESC");

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            int pending = 0;
            while (rs.next()) {
                String status = rs.getString("status");
                if ("PENDING".equals(status)) pending++;
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available_quantity"),
                        rs.getTimestamp("request_date"),
                        status,
                        rs.getInt("member_id"),
                        rs.getInt("book_id")
                });
            }

            summaryLabel.setText(tableModel.getRowCount() + " request(s) shown | "
                    + pending + " pending action");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading requests: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Approve ────────────────────────────────────────────────────────────────
    private void handleApprove() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String status = (String) tableModel.getValueAt(row, COL_STATUS);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only PENDING requests can be approved.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) tableModel.getValueAt(row, COL_REQ_ID);
        int memberId  = (int) tableModel.getValueAt(row, COL_MEMBER_ID);
        int bookId    = (int) tableModel.getValueAt(row, COL_BOOK_ID);
        String student = (String) tableModel.getValueAt(row, COL_STUDENT);
        String bookTitle = (String) tableModel.getValueAt(row, COL_BOOK);
        int availableCopies = (int) tableModel.getValueAt(row, COL_COPIES);

        // Guard: still copies available?
        if (availableCopies <= 0) {
            JOptionPane.showMessageDialog(this,
                    "❌ Cannot approve — no copies of \"" + bookTitle + "\" are currently available.",
                    "No Copies Available", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirm
        LocalDate today   = LocalDate.now();
        LocalDate dueDate = today.plusDays(LOAN_DAYS);

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Approve borrow request?</b><br><br>"
                        + "Student: " + student + "<br>"
                        + "Book: " + bookTitle + "<br>"
                        + "Borrow date: " + today + "<br>"
                        + "Due date: " + dueDate + " (" + LOAN_DAYS + " days)</html>",
                "Confirm Approval", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false); // transaction

            try {
                // 1. Insert into borrowed_books
                String insertSql =
                        "INSERT INTO borrowed_books (member_id, book_id, borrow_date, due_date, status) " +
                                "VALUES (?, ?, ?, ?, 'BORROWED')";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, memberId);
                insertStmt.setInt(2, bookId);
                insertStmt.setDate(3, java.sql.Date.valueOf(today));
                insertStmt.setDate(4, java.sql.Date.valueOf(dueDate));
                insertStmt.executeUpdate();

                // 2. Decrement available_quantity
                String decrementSql =
                        "UPDATE books SET available_quantity = available_quantity - 1 WHERE id = ? AND available_quantity > 0";
                PreparedStatement decrementStmt = conn.prepareStatement(decrementSql);
                decrementStmt.setInt(1, bookId);
                int updated = decrementStmt.executeUpdate();
                if (updated == 0) {
                    // Race condition — someone else borrowed the last copy
                    conn.rollback();
                    JOptionPane.showMessageDialog(this,
                            "❌ Failed: Book became unavailable. Please refresh.",
                            "Concurrent Update", JOptionPane.ERROR_MESSAGE);
                    loadRequests();
                    return;
                }

                // 3. Mark request as APPROVED
                String approveSql =
                        "UPDATE borrow_requests SET status='APPROVED', " +
                                "processed_by=?, processed_at=NOW() WHERE id=?";
                PreparedStatement approveStmt = conn.prepareStatement(approveSql);
                approveStmt.setString(1, com.librarysystem.LibrarySystemUI.getCurrentUsername());
                approveStmt.setInt(2, requestId);
                approveStmt.executeUpdate();

                conn.commit();

                JOptionPane.showMessageDialog(this,
                        "✅ Request approved!\n\n"
                                + student + " can now borrow \"" + bookTitle + "\".\n"
                                + "Due date: " + dueDate,
                        "Approved", JOptionPane.INFORMATION_MESSAGE);

                loadRequests();

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error approving request: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Reject ─────────────────────────────────────────────────────────────────
    private void handleReject() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String status = (String) tableModel.getValueAt(row, COL_STATUS);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only PENDING requests can be rejected.",
                    "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId  = (int) tableModel.getValueAt(row, COL_REQ_ID);
        String student  = (String) tableModel.getValueAt(row, COL_STUDENT);
        String bookTitle = (String) tableModel.getValueAt(row, COL_BOOK);

        // Optional rejection reason
        String reason = JOptionPane.showInputDialog(this,
                "<html>Reject borrow request for:<br><b>" + bookTitle + "</b> by " + student
                        + "<br><br>Reason (optional — shown to student):</html>",
                "Rejection Reason", JOptionPane.QUESTION_MESSAGE);

        if (reason == null) return; // user cancelled dialog

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE borrow_requests SET status='REJECTED', notes=?, " +
                    "processed_by=?, processed_at=NOW() WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, reason.trim().isEmpty() ? null : reason.trim());
            stmt.setString(2, com.librarysystem.LibrarySystemUI.getCurrentUsername());
            stmt.setInt(3, requestId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "❌ Request rejected.\n\nThe student will see the updated status in their portal.",
                    "Rejected", JOptionPane.INFORMATION_MESSAGE);

            loadRequests();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error rejecting request: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Details popup ──────────────────────────────────────────────────────────
    private void showDetails() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request first.");
            return;
        }

        // Fetch full details including notes from DB
        int requestId = (int) tableModel.getValueAt(row, COL_REQ_ID);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT br.*, m.name as student_name, m.email, " +
                    "b.title, b.author, b.available_quantity, " +
                    "br.processed_by, br.processed_at, br.notes " +
                    "FROM borrow_requests br " +
                    "JOIN members m ON br.member_id = m.id " +
                    "JOIN books   b ON br.book_id   = b.id " +
                    "WHERE br.id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String details = String.format(
                        "══════════════════════════════════════\n" +
                                "        BORROW REQUEST DETAILS\n" +
                                "══════════════════════════════════════\n\n" +
                                "Request #:    %d\n" +
                                "Status:       %s\n\n" +
                                "── Student ──────────────────────────\n" +
                                "Name:         %s\n" +
                                "Email:        %s\n\n" +
                                "── Book ─────────────────────────────\n" +
                                "Title:        %s\n" +
                                "Author:       %s\n" +
                                "Avail. Copies:%d\n\n" +
                                "── Timestamps ───────────────────────\n" +
                                "Requested:    %s\n" +
                                "Processed by: %s\n" +
                                "Processed at: %s\n\n" +
                                "── Notes ────────────────────────────\n" +
                                "%s\n",
                        rs.getInt("id"),
                        rs.getString("status"),
                        rs.getString("student_name"),
                        rs.getString("email"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("available_quantity"),
                        rs.getTimestamp("request_date"),
                        rs.getString("processed_by") != null ? rs.getString("processed_by") : "—",
                        rs.getTimestamp("processed_at") != null ? rs.getTimestamp("processed_at") : "—",
                        rs.getString("notes") != null ? rs.getString("notes") : "—"
                );

                JTextArea area = new JTextArea(details);
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane sp = new JScrollPane(area);
                sp.setPreferredSize(new Dimension(420, 380));
                JOptionPane.showMessageDialog(this, sp, "Request Details",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void hideColumn(int colIndex) {
        requestsTable.getColumnModel().getColumn(colIndex).setMinWidth(0);
        requestsTable.getColumnModel().getColumn(colIndex).setMaxWidth(0);
        requestsTable.getColumnModel().getColumn(colIndex).setWidth(0);
    }

    private JPanel makeLegendDot(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(color.darker());
        dot.setFont(new Font("Segoe UI", Font.BOLD, 16));
        p.add(dot);
        p.add(new JLabel(label));
        return p;
    }
}