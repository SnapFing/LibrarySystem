package panels;

import db.DBHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Return Requests Management Panel
 * For Admin/Librarian to:
 * - View pending return requests from students
 * - Approve or reject return requests
 * - View request history
 */
public class ReturnRequestsPanel extends JPanel {
    private JTable requestsTable;
    private DefaultTableModel tableModel;
    private JButton approveButton, rejectButton, refreshButton;
    private JLabel pendingCountLabel, processedTodayLabel;
    private JComboBox<String> filterCombo;

    public ReturnRequestsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ===== TITLE PANEL =====
        JPanel titlePanel = new JPanel(new BorderLayout());

        JPanel leftTitle = new JPanel();
        leftTitle.setLayout(new BoxLayout(leftTitle, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("📤 Return Requests Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitleLabel = new JLabel("Process student book return requests");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        subtitleLabel.setForeground(Color.GRAY);

        leftTitle.add(titleLabel);
        leftTitle.add(subtitleLabel);

        titlePanel.add(leftTitle, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        // ===== STATS PANEL =====
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 15));
        statsPanel.setBorder(BorderFactory.createTitledBorder("📊 Request Statistics"));

        pendingCountLabel = new JLabel("Pending: 0", SwingConstants.CENTER);
        pendingCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pendingCountLabel.setForeground(new Color(255, 152, 0));

        processedTodayLabel = new JLabel("Processed Today: 0", SwingConstants.CENTER);
        processedTodayLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        processedTodayLabel.setForeground(new Color(76, 175, 80));

        JLabel totalLabel = new JLabel("Total Requests: 0", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setForeground(new Color(33, 150, 243));

        JPanel pendingCard = createStatCard(pendingCountLabel, new Color(255, 152, 0));
        JPanel processedCard = createStatCard(processedTodayLabel, new Color(76, 175, 80));
        JPanel totalCard = createStatCard(totalLabel, new Color(33, 150, 243));

        statsPanel.add(pendingCard);
        statsPanel.add(processedCard);
        statsPanel.add(totalCard);

        // ===== FILTER PANEL =====
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter Options"));

        filterPanel.add(new JLabel("Show:"));
        filterCombo = new JComboBox<>(new String[]{
                "All Requests",
                "Pending Only",
                "Approved Only",
                "Rejected Only",
                "Processed Today"
        });
        filterCombo.addActionListener(e -> loadRequests());
        filterPanel.add(filterCombo);

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> {
            loadRequests();
            updateStats();
        });
        filterPanel.add(refreshButton);

        // Combine stats and filter
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(statsPanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        // ===== TABLE =====
        tableModel = new DefaultTableModel(new String[]{
                "ID", "Student Name", "Email", "Book Title", "Request Date",
                "Due Date", "Days Late", "Status", "Processed By", "Notes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        requestsTable = new JTable(tableModel);
        requestsTable.setRowHeight(28);
        requestsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        requestsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        requestsTable.getColumnModel().getColumn(0).setMaxWidth(60);
        requestsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        requestsTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        requestsTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        requestsTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        requestsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        requestsTable.getColumnModel().getColumn(6).setMaxWidth(80);
        requestsTable.getColumnModel().getColumn(7).setMaxWidth(100);

        // Color code rows
        requestsTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 7);
                    switch (status) {
                        case "PENDING":
                            c.setBackground(new Color(255, 248, 220)); // Light yellow
                            break;
                        case "APPROVED":
                            c.setBackground(new Color(230, 255, 230)); // Light green
                            break;
                        case "REJECTED":
                            c.setBackground(new Color(255, 230, 230)); // Light red
                            break;
                        default:
                            c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(requestsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Return Requests"));

        // Main content panel
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.add(topPanel, BorderLayout.NORTH);
        mainContent.add(scrollPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // ===== BUTTONS PANEL =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        approveButton = new JButton("✅ Approve & Process Return");
        approveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        approveButton.setForeground(new Color(0, 128, 0));
        approveButton.setToolTipText("Approve request and mark book as returned");
        approveButton.addActionListener(e -> handleApprove());

        rejectButton = new JButton("❌ Reject Request");
        rejectButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rejectButton.setForeground(new Color(180, 0, 0));
        rejectButton.setToolTipText("Reject the return request");
        rejectButton.addActionListener(e -> handleReject());

        JButton viewDetailsButton = new JButton("🔍 View Details");
        viewDetailsButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(viewDetailsButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        loadRequests();
        updateStats();

        // Auto-refresh every 30 seconds
        Timer autoRefreshTimer = new Timer(30000, e -> {
            loadRequests();
            updateStats();
        });
        autoRefreshTimer.start();
    }

    // ===== STAT CARD =====
    private JPanel createStatCard(JLabel label, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Top accent bar
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 5, 15, 15);

                // Border
                g2.setColor(new Color(220, 220, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 10, 15, 10));
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    // ===== LOAD REQUESTS =====
    private void loadRequests() {
        tableModel.setRowCount(0);
        String filter = (String) filterCombo.getSelectedItem();

        try (Connection conn = DBHelper.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT rr.id, CONCAT(m.fname, ' ', m.lname) as member_name, m.email, " +
                            "b.title, rr.request_date, bb.due_date, " +
                            "CASE WHEN bb.due_date < CURRENT_DATE THEN DATEDIFF(CURRENT_DATE, bb.due_date) ELSE 0 END as days_late, " +
                            "rr.status, rr.processed_by, rr.notes " +
                            "FROM return_requests rr " +
                            "JOIN borrowed_books bb ON rr.borrow_id = bb.id " +
                            "JOIN members m ON rr.member_id = m.id " +
                            "JOIN books b ON bb.book_id = b.id "
            );

            // Apply filter
            if ("Pending Only".equals(filter)) {
                sql.append("WHERE rr.status = 'PENDING' ");
            } else if ("Approved Only".equals(filter)) {
                sql.append("WHERE rr.status = 'APPROVED' ");
            } else if ("Rejected Only".equals(filter)) {
                sql.append("WHERE rr.status = 'REJECTED' ");
            } else if ("Processed Today".equals(filter)) {
                sql.append("WHERE DATE(rr.processed_date) = CURRENT_DATE ");
            }

            sql.append("ORDER BY " +
                    "CASE rr.status WHEN 'PENDING' THEN 1 WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 END, " +
                    "rr.request_date DESC");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("member_name"),
                        rs.getString("email"),
                        rs.getString("title"),
                        rs.getTimestamp("request_date"),
                        rs.getDate("due_date"),
                        rs.getInt("days_late"),
                        rs.getString("status"),
                        rs.getString("processed_by") != null ? rs.getString("processed_by") : "-",
                        rs.getString("notes") != null ? rs.getString("notes") : "-"
                });
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading requests: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== UPDATE STATS =====
    private void updateStats() {
        try (Connection conn = DBHelper.getConnection()) {
            // Pending count
            String sql = "SELECT COUNT(*) FROM return_requests WHERE status='PENDING'";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt(1);
                pendingCountLabel.setText("Pending: " + count);
            }

            // Processed today
            sql = "SELECT COUNT(*) FROM return_requests WHERE DATE(processed_date)=CURRENT_DATE";
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt(1);
                processedTodayLabel.setText("Processed Today: " + count);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== APPROVE REQUEST =====
    private void handleApprove() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a request to approve!",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String status = (String) tableModel.getValueAt(row, 7);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This request has already been processed!",
                    "Already Processed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) tableModel.getValueAt(row, 0);
        String memberName = (String) tableModel.getValueAt(row, 1);
        String bookTitle = (String) tableModel.getValueAt(row, 3);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Approve return request and mark book as returned?\n\n" +
                        "Student: " + memberName + "\n" +
                        "Book: " + bookTitle + "\n\n" +
                        "This will:\n" +
                        "✓ Mark the book as returned\n" +
                        "✓ Update book availability\n" +
                        "✓ Approve the return request",
                "Confirm Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Get borrow_id from return request
                String getBorrowSql = "SELECT borrow_id FROM return_requests WHERE id=?";
                PreparedStatement getBorrowStmt = conn.prepareStatement(getBorrowSql);
                getBorrowStmt.setInt(1, requestId);
                ResultSet rs = getBorrowStmt.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Return request not found");
                }

                int borrowId = rs.getInt("borrow_id");

                // Get book_id from borrowed_books
                String getBookSql = "SELECT book_id FROM borrowed_books WHERE id=?";
                PreparedStatement getBookStmt = conn.prepareStatement(getBookSql);
                getBookStmt.setInt(1, borrowId);
                rs = getBookStmt.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Borrow record not found");
                }

                int bookId = rs.getInt("book_id");

                // 1. Update borrowed_books - mark as returned
                String updateBorrowSql = "UPDATE borrowed_books SET status='RETURNED', " +
                        "return_date=CURRENT_DATE WHERE id=?";
                PreparedStatement updateBorrowStmt = conn.prepareStatement(updateBorrowSql);
                updateBorrowStmt.setInt(1, borrowId);
                updateBorrowStmt.executeUpdate();

                // 2. Update book availability
                String updateBookSql = "UPDATE books SET available_quantity = available_quantity + 1 WHERE id=?";
                PreparedStatement updateBookStmt = conn.prepareStatement(updateBookSql);
                updateBookStmt.setInt(1, bookId);
                updateBookStmt.executeUpdate();

                // 3. Update return request status
                String currentUser = panels.LibrarySystemUI.getCurrentUsername();
                String updateRequestSql = "UPDATE return_requests SET status='APPROVED', " +
                        "processed_by=?, processed_date=CURRENT_TIMESTAMP WHERE id=?";
                PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql);
                updateRequestStmt.setString(1, currentUser);
                updateRequestStmt.setInt(2, requestId);
                updateRequestStmt.executeUpdate();

                conn.commit();

                JOptionPane.showMessageDialog(this,
                        "✅ Return request approved successfully!\n\n" +
                                "The book has been marked as returned.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                loadRequests();
                updateStats();

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error approving request: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== REJECT REQUEST =====
    private void handleReject() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request!");
            return;
        }

        String status = (String) tableModel.getValueAt(row, 7);
        if (!"PENDING".equals(status)) {
            JOptionPane.showMessageDialog(this, "This request has already been processed!");
            return;
        }

        int requestId = (int) tableModel.getValueAt(row, 0);
        String memberName = (String) tableModel.getValueAt(row, 1);

        String reason = JOptionPane.showInputDialog(this,
                "Reject return request for " + memberName + "\n\n" +
                        "Please provide a reason for rejection:",
                "Reject Request",
                JOptionPane.QUESTION_MESSAGE);

        if (reason == null || reason.trim().isEmpty()) {
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            String currentUser = panels.LibrarySystemUI.getCurrentUsername();
            String sql = "UPDATE return_requests SET status='REJECTED', " +
                    "processed_by=?, processed_date=CURRENT_TIMESTAMP, notes=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentUser);
            stmt.setString(2, reason);
            stmt.setInt(3, requestId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Request has been rejected.\n" +
                            "The student will be notified.",
                    "Request Rejected",
                    JOptionPane.INFORMATION_MESSAGE);

            loadRequests();
            updateStats();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== VIEW DETAILS =====
    private void handleViewDetails() {
        int row = requestsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request!");
            return;
        }

        int requestId = (int) tableModel.getValueAt(row, 0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT rr.*, CONCAT(m.fname, ' ', m.lname) as member_name, " +
                    "m.email, m.phone, b.title, b.author, bb.borrow_date, bb.due_date " +
                    "FROM return_requests rr " +
                    "JOIN borrowed_books bb ON rr.borrow_id = bb.id " +
                    "JOIN members m ON rr.member_id = m.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE rr.id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                StringBuilder details = new StringBuilder();
                details.append("═══════════════════════════════════\n");
                details.append("     RETURN REQUEST DETAILS\n");
                details.append("═══════════════════════════════════\n\n");

                details.append("📋 Request ID: ").append(rs.getInt("id")).append("\n");
                details.append("📊 Status: ").append(rs.getString("status")).append("\n");
                details.append("📅 Request Date: ").append(rs.getTimestamp("request_date")).append("\n");

                if (rs.getTimestamp("processed_date") != null) {
                    details.append("✅ Processed Date: ").append(rs.getTimestamp("processed_date")).append("\n");
                    details.append("👤 Processed By: ").append(rs.getString("processed_by")).append("\n");
                }

                details.append("\n👤 STUDENT INFORMATION\n");
                details.append("───────────────────────────────────\n");
                details.append("Name: ").append(rs.getString("member_name")).append("\n");
                details.append("Email: ").append(rs.getString("email")).append("\n");
                details.append("Phone: ").append(rs.getString("phone")).append("\n");

                details.append("\n📚 BOOK INFORMATION\n");
                details.append("───────────────────────────────────\n");
                details.append("Title: ").append(rs.getString("title")).append("\n");
                details.append("Author: ").append(rs.getString("author")).append("\n");

                details.append("\n📅 BORROW INFORMATION\n");
                details.append("───────────────────────────────────\n");
                details.append("Borrow Date: ").append(rs.getDate("borrow_date")).append("\n");
                details.append("Due Date: ").append(rs.getDate("due_date")).append("\n");

                Date dueDate = rs.getDate("due_date");
                if (dueDate != null) {
                    long diff = System.currentTimeMillis() - dueDate.getTime();
                    long days = diff / (1000 * 60 * 60 * 24);
                    if (days > 0) {
                        details.append("⚠️ Overdue by: ").append(days).append(" days\n");
                    }
                }

                if (rs.getString("notes") != null) {
                    details.append("\n📝 NOTES\n");
                    details.append("───────────────────────────────────\n");
                    details.append(rs.getString("notes")).append("\n");
                }

                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(450, 500));

                JOptionPane.showMessageDialog(this, scrollPane,
                        "Return Request Details", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}