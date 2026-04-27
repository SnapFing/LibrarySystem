package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Enhanced MyBorrowedBooksPanel
 * Features:
 * - View currently borrowed books
 * - Request return for borrowed books
 * - View return request status
 * - Separate tab for fines (redirects to fines panel)
 */
public class MyBorrowedBooksPanel extends JPanel {
    private JTable borrowedTable, requestsTable;
    private DefaultTableModel borrowedModel, requestsModel;
    private String studentName;
    private JTabbedPane tabbedPane;

    public MyBorrowedBooksPanel(String studentName) {
        this.studentName = studentName;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ===== Title Panel =====
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📖 My Borrowed Books");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel subtitleLabel = new JLabel("View your borrowed books and request returns");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitleLabel.setForeground(Color.GRAY);

        JPanel titleLeft = new JPanel();
        titleLeft.setLayout(new BoxLayout(titleLeft, BoxLayout.Y_AXIS));
        titleLeft.setOpaque(false);
        titleLeft.add(titleLabel);
        titleLeft.add(subtitleLabel);

        titlePanel.add(titleLeft, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        // ===== Tabbed Pane =====
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Currently Borrowed Books
        JPanel borrowedPanel = createBorrowedBooksPanel();
        tabbedPane.addTab("📚 Currently Borrowed", borrowedPanel);

        // Tab 2: Return Requests
        JPanel requestsPanel = createReturnRequestsPanel();
        tabbedPane.addTab("📤 Return Requests", requestsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // ===== Info Panel (Footer) =====
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel infoLabel1 = new JLabel("💡 To return a book, select it and click 'Request Return'");
        JLabel infoLabel2 = new JLabel("📧 You'll be notified when your request is processed");
        JLabel infoLabel3 = new JLabel("💰 View your fines in the 'Fines' tab");

        infoLabel1.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel3.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        infoLabel1.setForeground(new Color(0, 102, 204));
        infoLabel2.setForeground(new Color(0, 153, 51));
        infoLabel3.setForeground(new Color(204, 102, 0));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        row1.setOpaque(false);
        row2.setOpaque(false);
        row3.setOpaque(false);

        row1.add(infoLabel1);
        row2.add(infoLabel2);
        row3.add(infoLabel3);

        infoPanel.add(row1);
        infoPanel.add(row2);
        infoPanel.add(row3);

        add(infoPanel, BorderLayout.SOUTH);

        // Load data
        loadBorrowedBooks();
        loadReturnRequests();
    }

    // ===== BORROWED BOOKS PANEL =====
    private JPanel createBorrowedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Table
        borrowedModel = new DefaultTableModel(
                new String[]{"Book Title", "Author", "Borrow Date", "Due Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        borrowedTable = new JTable(borrowedModel);
        borrowedTable.setRowHeight(30);
        borrowedTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        borrowedTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        borrowedTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        borrowedTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        borrowedTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        borrowedTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        borrowedTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Color code rows based on due date
        borrowedTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    try {
                        String status = (String) table.getValueAt(row, 4);
                        Date dueDate = (Date) table.getValueAt(row, 3);
                        Date today = new Date(System.currentTimeMillis());

                        if ("BORROWED".equals(status) && dueDate != null && dueDate.before(today)) {
                            // Overdue - red
                            c.setBackground(new Color(255, 230, 230));
                        } else if ("BORROWED".equals(status) && dueDate != null) {
                            // Calculate days until due
                            long diff = dueDate.getTime() - today.getTime();
                            long days = diff / (1000 * 60 * 60 * 24);

                            if (days <= 3) {
                                // Due soon - yellow
                                c.setBackground(new Color(255, 255, 230));
                            } else {
                                // Normal - white
                                c.setBackground(Color.WHITE);
                            }
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    } catch (Exception e) {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(borrowedTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Currently Borrowed Books"));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton requestReturnBtn = new JButton("📤 Request Return");
        requestReturnBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        requestReturnBtn.setToolTipText("Send a return request to the librarian");
        requestReturnBtn.addActionListener(e -> handleRequestReturn());

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshBtn.addActionListener(e -> {
            loadBorrowedBooks();
            loadReturnRequests();
        });

        JButton viewDetailsBtn = new JButton("🔍 View Details");
        viewDetailsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        viewDetailsBtn.addActionListener(e -> handleViewDetails());

        buttonPanel.add(requestReturnBtn);
        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(refreshBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== RETURN REQUESTS PANEL =====
    private JPanel createReturnRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Table
        requestsModel = new DefaultTableModel(
                new String[]{"Request ID", "Book Title", "Request Date", "Status", "Notes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        requestsTable = new JTable(requestsModel);
        requestsTable.setRowHeight(30);
        requestsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        requestsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        requestsTable.getColumnModel().getColumn(0).setMaxWidth(100);
        requestsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        requestsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        requestsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        requestsTable.getColumnModel().getColumn(4).setPreferredWidth(200);

        // Color code by status
        requestsTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 3);
                    switch (status) {
                        case "PENDING":
                            c.setBackground(new Color(255, 255, 230)); // Yellow
                            break;
                        case "APPROVED":
                            c.setBackground(new Color(230, 255, 230)); // Green
                            break;
                        case "REJECTED":
                            c.setBackground(new Color(255, 230, 230)); // Red
                            break;
                        default:
                            c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(requestsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Your Return Requests"));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Info label
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("<html><b>Status Legend:</b> " +
                "<span style='background-color:#ffffcc; padding:2px 8px;'>⏳ PENDING</span> " +
                "<span style='background-color:#ccffcc; padding:2px 8px;'>✅ APPROVED</span> " +
                "<span style='background-color:#ffcccc; padding:2px 8px;'>❌ REJECTED</span></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoPanel.add(infoLabel);
        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== LOAD DATA =====
    private void loadBorrowedBooks() {
        borrowedModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            // Get member ID
            String memberSql = "SELECT id FROM members WHERE name=?";
            PreparedStatement memberStmt = conn.prepareStatement(memberSql);
            memberStmt.setString(1, studentName);
            ResultSet memberRs = memberStmt.executeQuery();

            if (!memberRs.next()) {
                JOptionPane.showMessageDialog(this, "Member not found!");
                return;
            }

            int memberId = memberRs.getInt("id");

            // Get borrowed books
            String sql = "SELECT b.title, b.author, bb.borrow_date, bb.due_date, bb.status, bb.id " +
                    "FROM borrowed_books bb " +
                    "INNER JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id=? AND bb.status='BORROWED' " +
                    "ORDER BY bb.due_date ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Date dueDate = rs.getDate("due_date");
                Date today = new Date(System.currentTimeMillis());

                String statusDisplay = rs.getString("status");

                // Check if overdue
                if (dueDate != null && dueDate.before(today)) {
                    long diff = today.getTime() - dueDate.getTime();
                    long daysOverdue = diff / (1000 * 60 * 60 * 24);
                    statusDisplay = "⚠️ OVERDUE (" + daysOverdue + " days)";
                } else if (dueDate != null) {
                    // Check if due soon
                    long diff = dueDate.getTime() - today.getTime();
                    long daysToDue = diff / (1000 * 60 * 60 * 24);
                    if (daysToDue <= 3) {
                        statusDisplay = "⏰ DUE SOON (" + daysToDue + " days)";
                    }
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
            JOptionPane.showMessageDialog(this,
                    "Error loading borrowed books: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadReturnRequests() {
        requestsModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            // Get member ID
            String memberSql = "SELECT id FROM members WHERE name=?";
            PreparedStatement memberStmt = conn.prepareStatement(memberSql);
            memberStmt.setString(1, studentName);
            ResultSet memberRs = memberStmt.executeQuery();

            if (!memberRs.next()) {
                return;
            }

            int memberId = memberRs.getInt("id");

            // Get return requests
            String sql = "SELECT rr.id, b.title, rr.request_date, rr.status, rr.notes " +
                    "FROM return_requests rr " +
                    "INNER JOIN borrowed_books bb ON rr.borrow_id = bb.id " +
                    "INNER JOIN books b ON bb.book_id = b.id " +
                    "WHERE rr.member_id=? " +
                    "ORDER BY rr.request_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                requestsModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getTimestamp("request_date"),
                        rs.getString("status"),
                        rs.getString("notes") != null ? rs.getString("notes") : "-"
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ===== HANDLE REQUEST RETURN =====
    private void handleRequestReturn() {
        int row = borrowedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a book to request return!",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookTitle = (String) borrowedModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Request return for:\n\n" + bookTitle + "\n\n" +
                        "The librarian will be notified and will process your request.",
                "Confirm Return Request",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            // Get member ID
            String memberSql = "SELECT id FROM members WHERE name=?";
            PreparedStatement memberStmt = conn.prepareStatement(memberSql);
            memberStmt.setString(1, studentName);
            ResultSet memberRs = memberStmt.executeQuery();

            if (!memberRs.next()) {
                JOptionPane.showMessageDialog(this, "Member not found!");
                return;
            }

            int memberId = memberRs.getInt("id");

            // Get borrow ID for this book
            String borrowSql = "SELECT bb.id FROM borrowed_books bb " +
                    "INNER JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id=? AND b.title=? AND bb.status='BORROWED' " +
                    "LIMIT 1";
            PreparedStatement borrowStmt = conn.prepareStatement(borrowSql);
            borrowStmt.setInt(1, memberId);
            borrowStmt.setString(2, bookTitle);
            ResultSet borrowRs = borrowStmt.executeQuery();

            if (!borrowRs.next()) {
                JOptionPane.showMessageDialog(this, "Borrow record not found!");
                return;
            }

            int borrowId = borrowRs.getInt("id");

            // Check if request already exists
            String checkSql = "SELECT COUNT(*) FROM return_requests " +
                    "WHERE borrow_id=? AND status='PENDING'";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, borrowId);
            ResultSet checkRs = checkStmt.executeQuery();
            checkRs.next();

            if (checkRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "You already have a pending return request for this book!",
                        "Duplicate Request",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Create return request
            String insertSql = "INSERT INTO return_requests (borrow_id, member_id, status) " +
                    "VALUES (?, ?, 'PENDING')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, borrowId);
            insertStmt.setInt(2, memberId);
            insertStmt.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "✅ Return request submitted successfully!\n\n" +
                            "The librarian will process your request soon.",
                    "Request Submitted",
                    JOptionPane.INFORMATION_MESSAGE);

            // Refresh tables
            loadBorrowedBooks();
            loadReturnRequests();

            // Switch to requests tab to show the new request
            tabbedPane.setSelectedIndex(1);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error submitting return request: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== VIEW DETAILS =====
    private void handleViewDetails() {
        int row = borrowedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book!");
            return;
        }

        String title = (String) borrowedModel.getValueAt(row, 0);
        String author = (String) borrowedModel.getValueAt(row, 1);
        Date borrowDate = (Date) borrowedModel.getValueAt(row, 2);
        Date dueDate = (Date) borrowedModel.getValueAt(row, 3);
        String status = (String) borrowedModel.getValueAt(row, 4);

        String details = String.format(
                "═══════════════════════════════════\n" +
                        "        BOOK BORROW DETAILS\n" +
                        "═══════════════════════════════════\n\n" +
                        "📚 Title: %s\n" +
                        "✍️ Author: %s\n" +
                        "📅 Borrow Date: %s\n" +
                        "⏰ Due Date: %s\n" +
                        "📊 Status: %s\n\n",
                title, author, borrowDate, dueDate, status
        );

        // Calculate days remaining or overdue
        Date today = new Date(System.currentTimeMillis());
        if (dueDate != null) {
            long diff = dueDate.getTime() - today.getTime();
            long days = Math.abs(diff / (1000 * 60 * 60 * 24));

            if (dueDate.before(today)) {
                details += "⚠️ OVERDUE by " + days + " day(s)\n";
                details += "💰 Potential fine: K" + (days * 5.00) + "\n";
            } else {
                details += "✅ " + days + " day(s) remaining\n";
            }
        }

        details += "\n💡 To return this book, use the\n   'Request Return' button.";

        JTextArea textArea = new JTextArea(details);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 350));

        JOptionPane.showMessageDialog(this, scrollPane, "Book Details",
                JOptionPane.INFORMATION_MESSAGE);
    }
}