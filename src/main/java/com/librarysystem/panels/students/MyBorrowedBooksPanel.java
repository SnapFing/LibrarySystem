package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * MyBorrowedBooksPanel (enhanced)
 *
 * Three tabs:
 *   Tab 1 — 📚 Currently Borrowed   : books the student currently holds
 *   Tab 2 — 📤 Return Requests       : return requests the student has submitted
 *   Tab 3 — 📋 My Borrow Requests    : borrow requests with PENDING/APPROVED/REJECTED status
 */
public class MyBorrowedBooksPanel extends JPanel {

    private final String studentName;

    // Tab 1
    private JTable borrowedTable;
    private DefaultTableModel borrowedModel;

    // Tab 2
    private JTable returnRequestsTable;
    private DefaultTableModel returnRequestsModel;

    // Tab 3 (NEW)
    private JTable borrowRequestsTable;
    private DefaultTableModel borrowRequestsModel;

    private JTabbedPane tabbedPane;

    public MyBorrowedBooksPanel(String studentName) {
        this.studentName = studentName;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ── Title ─────────────────────────────────────────────────────────────
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📖 My Books & Requests");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel subtitleLabel = new JLabel("Track your borrowed books, return requests, and borrow requests");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitleLabel.setForeground(Color.GRAY);

        JPanel titleLeft = new JPanel();
        titleLeft.setLayout(new BoxLayout(titleLeft, BoxLayout.Y_AXIS));
        titleLeft.setOpaque(false);
        titleLeft.add(titleLabel);
        titleLeft.add(subtitleLabel);

        // Global refresh button
        JButton refreshAllBtn = new JButton("🔄 Refresh All");
        refreshAllBtn.addActionListener(e -> refreshAll());
        titlePanel.add(titleLeft, BorderLayout.WEST);
        titlePanel.add(refreshAllBtn, BorderLayout.EAST);
        add(titlePanel, BorderLayout.NORTH);

        // ── Tabs ──────────────────────────────────────────────────────────────
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("📚 Currently Borrowed", buildBorrowedTab());
        tabbedPane.addTab("📤 Return Requests",     buildReturnRequestsTab());
        tabbedPane.addTab("📋 My Borrow Requests",  buildBorrowRequestsTab());

        add(tabbedPane, BorderLayout.CENTER);

        // ── Footer tips ───────────────────────────────────────────────────────
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        addTip(footer, "💡 To request a return, go to the 'Currently Borrowed' tab and click 'Request Return'.",
                new Color(0, 102, 204));
        addTip(footer, "📋 To borrow a book, go to 'Browse Books' and click 'Request Borrow' on any card.",
                new Color(0, 153, 51));
        addTip(footer, "⏳ Pending requests are reviewed by a librarian — check back soon.",
                new Color(180, 100, 0));

        add(footer, BorderLayout.SOUTH);

        refreshAll();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 1: Currently Borrowed
    // ═══════════════════════════════════════════════════════════════════════════
    private JPanel buildBorrowedTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        borrowedModel = new DefaultTableModel(
                new String[]{"Book Title", "Author", "Borrow Date", "Due Date", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        borrowedTable = new JTable(borrowedModel);
        styleTable(borrowedTable);
        borrowedTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        borrowedTable.getColumnModel().getColumn(1).setPreferredWidth(180);

        // Row colour by due-date proximity
        borrowedTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    try {
                        Date due   = (Date) t.getValueAt(row, 3);
                        Date today = new Date(System.currentTimeMillis());
                        if (due != null && due.before(today)) {
                            c.setBackground(new Color(255, 220, 220)); // overdue
                        } else if (due != null) {
                            long days = (due.getTime() - today.getTime()) / 86_400_000L;
                            c.setBackground(days <= 3
                                    ? new Color(255, 255, 200)   // due soon
                                    : Color.WHITE);
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    } catch (Exception ignored) {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(borrowedTable);
        sp.setBorder(BorderFactory.createTitledBorder("Books you currently hold"));
        panel.add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton returnBtn = new JButton("📤 Request Return");
        returnBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        returnBtn.addActionListener(e -> handleRequestReturn());

        JButton detailsBtn = new JButton("🔍 View Details");
        detailsBtn.addActionListener(e -> handleViewDetails());

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadBorrowedBooks());

        btnRow.add(returnBtn);
        btnRow.add(detailsBtn);
        btnRow.add(refreshBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 2: Return Requests
    // ═══════════════════════════════════════════════════════════════════════════
    private JPanel buildReturnRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        returnRequestsModel = new DefaultTableModel(
                new String[]{"Request #", "Book Title", "Requested On", "Status", "Notes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        returnRequestsTable = new JTable(returnRequestsModel);
        styleTable(returnRequestsTable);
        returnRequestsTable.getColumnModel().getColumn(0).setMaxWidth(90);
        returnRequestsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        returnRequestsTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        applyStatusRenderer(returnRequestsTable, 3);

        JScrollPane sp = new JScrollPane(returnRequestsTable);
        sp.setBorder(BorderFactory.createTitledBorder("Your Return Requests"));
        panel.add(sp, BorderLayout.CENTER);
        panel.add(buildLegend(), BorderLayout.SOUTH);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 3: Borrow Requests (NEW)
    // ═══════════════════════════════════════════════════════════════════════════
    private JPanel buildBorrowRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        borrowRequestsModel = new DefaultTableModel(
                new String[]{"Request #", "Book Title", "Author", "Requested On",
                        "Status", "Notes (if rejected)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        borrowRequestsTable = new JTable(borrowRequestsModel);
        styleTable(borrowRequestsTable);
        borrowRequestsTable.getColumnModel().getColumn(0).setMaxWidth(90);
        borrowRequestsTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        borrowRequestsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        borrowRequestsTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        borrowRequestsTable.getColumnModel().getColumn(4).setMaxWidth(100);
        borrowRequestsTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        applyStatusRenderer(borrowRequestsTable, 4);

        JScrollPane sp = new JScrollPane(borrowRequestsTable);
        sp.setBorder(BorderFactory.createTitledBorder("Your Borrow Requests"));
        panel.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());

        // Cancel pending request button
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton cancelBtn = new JButton("🗑️ Cancel Pending Request");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.setToolTipText("Cancel a PENDING borrow request you no longer need");
        cancelBtn.addActionListener(e -> handleCancelBorrowRequest());

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadBorrowRequests());

        btnRow.add(cancelBtn);
        btnRow.add(refreshBtn);
        bottom.add(btnRow, BorderLayout.NORTH);
        bottom.add(buildLegend(), BorderLayout.SOUTH);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA LOADERS
    // ═══════════════════════════════════════════════════════════════════════════
    private void refreshAll() {
        loadBorrowedBooks();
        loadReturnRequests();
        loadBorrowRequests();
    }

    private void loadBorrowedBooks() {
        borrowedModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            int memberId = getMemberId(conn);
            if (memberId == -1) return;

            String sql = "SELECT b.title, b.author, bb.borrow_date, bb.due_date, bb.status, bb.id " +
                    "FROM borrowed_books bb " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id=? AND bb.status='BORROWED' " +
                    "ORDER BY bb.due_date ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Date dueDate = rs.getDate("due_date");
                Date today   = new Date(System.currentTimeMillis());
                String statusDisplay = rs.getString("status");

                if (dueDate != null && dueDate.before(today)) {
                    long overdue = (today.getTime() - dueDate.getTime()) / 86_400_000L;
                    statusDisplay = "⚠️ OVERDUE (" + overdue + "d)";
                } else if (dueDate != null) {
                    long remaining = (dueDate.getTime() - today.getTime()) / 86_400_000L;
                    if (remaining <= 3) statusDisplay = "⏰ DUE SOON (" + remaining + "d)";
                }

                borrowedModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDate("borrow_date"),
                        dueDate,
                        statusDisplay
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadReturnRequests() {
        returnRequestsModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            int memberId = getMemberId(conn);
            if (memberId == -1) return;

            String sql = "SELECT rr.id, b.title, rr.request_date, rr.status, rr.notes " +
                    "FROM return_requests rr " +
                    "JOIN borrowed_books bb ON rr.borrow_id = bb.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE rr.member_id=? ORDER BY rr.request_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                returnRequestsModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getTimestamp("request_date"),
                        rs.getString("status"),
                        rs.getString("notes") != null ? rs.getString("notes") : "—"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadBorrowRequests() {
        borrowRequestsModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            int memberId = getMemberId(conn);
            if (memberId == -1) return;

            String sql = "SELECT br.id, b.title, b.author, br.request_date, br.status, br.notes " +
                    "FROM borrow_requests br " +
                    "JOIN books b ON br.book_id = b.id " +
                    "WHERE br.member_id=? ORDER BY br.request_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                borrowRequestsModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getTimestamp("request_date"),
                        rs.getString("status"),
                        rs.getString("notes") != null ? rs.getString("notes") : "—"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    private void handleRequestReturn() {
        int row = borrowedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to return.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookTitle = (String) borrowedModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Request return for:</b><br><br>" + bookTitle +
                        "<br><br>The librarian will be notified.</html>",
                "Confirm Return Request",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            int memberId = getMemberId(conn);
            if (memberId == -1) return;

            // Get borrow record id
            String borrowSql = "SELECT bb.id FROM borrowed_books bb " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id=? AND b.title=? AND bb.status='BORROWED' LIMIT 1";
            PreparedStatement borrowStmt = conn.prepareStatement(borrowSql);
            borrowStmt.setInt(1, memberId);
            borrowStmt.setString(2, bookTitle);
            ResultSet borrowRs = borrowStmt.executeQuery();
            if (!borrowRs.next()) {
                JOptionPane.showMessageDialog(this, "Could not find borrow record.");
                return;
            }
            int borrowId = borrowRs.getInt("id");

            // Check duplicate
            String checkSql = "SELECT COUNT(*) FROM return_requests WHERE borrow_id=? AND status='PENDING'";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, borrowId);
            ResultSet checkRs = checkStmt.executeQuery();
            checkRs.next();
            if (checkRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "⏳ You already have a pending return request for this book.",
                        "Duplicate Request", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Insert
            String insertSql = "INSERT INTO return_requests (borrow_id, member_id, status) VALUES (?,?,'PENDING')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, borrowId);
            insertStmt.setInt(2, memberId);
            insertStmt.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "✅ Return request submitted!\nCheck the 'Return Requests' tab for status.",
                    "Submitted", JOptionPane.INFORMATION_MESSAGE);

            refreshAll();
            tabbedPane.setSelectedIndex(1);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelBorrowRequest() {
        int row = borrowRequestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to cancel.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String status    = (String) borrowRequestsModel.getValueAt(row, 4);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only PENDING requests can be cancelled.",
                    "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) borrowRequestsModel.getValueAt(row, 0);
        String bookTitle = (String) borrowRequestsModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel your request to borrow:\n\"" + bookTitle + "\"?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "DELETE FROM borrow_requests WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, requestId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Request cancelled.",
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            loadBorrowRequests();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleViewDetails() {
        int row = borrowedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book.");
            return;
        }

        String title  = (String) borrowedModel.getValueAt(row, 0);
        String author = (String) borrowedModel.getValueAt(row, 1);
        Date borrowDate = (Date) borrowedModel.getValueAt(row, 2);
        Date dueDate    = (Date) borrowedModel.getValueAt(row, 3);
        String status   = (String) borrowedModel.getValueAt(row, 4);

        Date today = new Date(System.currentTimeMillis());
        String dueLine;
        if (dueDate != null && dueDate.before(today)) {
            long days = (today.getTime() - dueDate.getTime()) / 86_400_000L;
            dueLine = "⚠️ OVERDUE by " + days + " day(s)";
        } else if (dueDate != null) {
            long days = (dueDate.getTime() - today.getTime()) / 86_400_000L;
            dueLine = "✅ " + days + " day(s) remaining";
        } else {
            dueLine = "—";
        }

        String details = String.format(
                "═════════════════════════════════\n" +
                        "       BOOK BORROW DETAILS\n" +
                        "═════════════════════════════════\n\n" +
                        "📚 Title:       %s\n" +
                        "✍️  Author:      %s\n" +
                        "📅 Borrowed:    %s\n" +
                        "⏰ Due:         %s\n" +
                        "📊 Status:      %s\n\n" +
                        "%s\n\n" +
                        "💡 Use 'Request Return' to initiate\n   the return process.",
                title, author, borrowDate, dueDate, status, dueLine);

        JTextArea area = new JTextArea(details);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(380, 300));
        JOptionPane.showMessageDialog(this, sp, "Book Details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    private int getMemberId(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM members WHERE name=?");
        stmt.setString(1, studentName);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /** Colour table rows by the value in statusCol. */
    private void applyStatusRenderer(JTable table, int statusCol) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    String s = String.valueOf(t.getValueAt(row, statusCol));
                    switch (s) {
                        case "PENDING"  -> c.setBackground(new Color(255, 255, 210));
                        case "APPROVED" -> c.setBackground(new Color(210, 255, 210));
                        case "REJECTED" -> c.setBackground(new Color(255, 210, 210));
                        default         -> c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        p.add(legendItem(new Color(255, 255, 150), "⏳ Pending"));
        p.add(legendItem(new Color(150, 255, 150), "✅ Approved"));
        p.add(legendItem(new Color(255, 150, 150), "❌ Rejected"));
        return p;
    }

    private JLabel legendItem(Color color, String text) {
        JLabel lbl = new JLabel("  " + text + "  ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setBackground(color);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        return lbl;
    }

    private void addTip(JPanel container, String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lbl.setForeground(color);
        container.add(lbl);
    }
}