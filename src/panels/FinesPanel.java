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

/**
 * Enhanced Fines Panel
 * Comprehensive view of:
 * - All fines (paid and unpaid)
 * - Overdue books
 * - Outstanding dues
 * - Payment history
 */
public class FinesPanel extends JPanel {
    private JTabbedPane tabbedPane;
    private JTable finesTable, overdueTable;
    private DefaultTableModel finesModel, overdueModel;
    private JLabel totalUnpaidLabel, totalPaidLabel, overdueCountLabel, totalDuesLabel;
    private JComboBox<String> fineFilterCombo;

    public FinesPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ===== TITLE PANEL =====
        JPanel titlePanel = createTitlePanel();
        add(titlePanel, BorderLayout.NORTH);

        // ===== STATS PANEL =====
        JPanel statsPanel = createStatsPanel();

        // ===== TABBED PANE =====
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: All Fines
        JPanel finesPanel = createFinesPanel();
        tabbedPane.addTab("💰 Fines Management", finesPanel);

        // Tab 2: Overdue Books
        JPanel overduePanel = createOverduePanel();
        tabbedPane.addTab("⚠️ Overdue Books", overduePanel);

        // Main content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.add(statsPanel, BorderLayout.NORTH);
        mainContent.add(tabbedPane, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        // Load initial data
        loadFines();
        loadOverdueBooks();
        updateStats();

        // Auto-refresh every 60 seconds
        Timer autoRefresh = new Timer(60000, e -> {
            loadFines();
            loadOverdueBooks();
            updateStats();
        });
        autoRefresh.start();
    }

    // ===== TITLE PANEL =====
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("💰 Fines & Dues Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel subtitleLabel = new JLabel("Manage all library fines, overdue books, and outstanding payments");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        subtitleLabel.setForeground(Color.GRAY);

        leftPanel.add(titleLabel);
        leftPanel.add(subtitleLabel);

        panel.add(leftPanel, BorderLayout.WEST);

        return panel;
    }

    // ===== STATS PANEL =====
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 15));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("📊 Financial Overview"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        totalUnpaidLabel = new JLabel("K 0.00", SwingConstants.CENTER);
        totalPaidLabel = new JLabel("K 0.00", SwingConstants.CENTER);
        totalDuesLabel = new JLabel("K 0.00", SwingConstants.CENTER);
        overdueCountLabel = new JLabel("0 Books", SwingConstants.CENTER);

        panel.add(createStatCard("Total Unpaid Fines", totalUnpaidLabel, new Color(220, 53, 69)));
        panel.add(createStatCard("Total Paid Fines", totalPaidLabel, new Color(40, 167, 69)));
        panel.add(createStatCard("Estimated Dues", totalDuesLabel, new Color(255, 193, 7)));
        panel.add(createStatCard("Overdue Books", overdueCountLabel, new Color(255, 87, 34)));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), 4, 12, 12);

                g2.setColor(new Color(220, 220, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLbl.setForeground(Color.GRAY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // ===== FINES PANEL =====
    private JPanel createFinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter & Actions"));

        filterPanel.add(new JLabel("Show:"));
        fineFilterCombo = new JComboBox<>(new String[]{
                "All Fines", "Unpaid Only", "Paid Only", "This Month"
        });
        fineFilterCombo.addActionListener(e -> loadFines());
        filterPanel.add(fineFilterCombo);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            loadFines();
            updateStats();
        });
        filterPanel.add(refreshBtn);

        JButton calculateFinesBtn = new JButton("💵 Calculate Overdue Fines");
        calculateFinesBtn.setToolTipText("Automatically calculate fines for overdue books");
        calculateFinesBtn.addActionListener(e -> handleCalculateFines());
        filterPanel.add(calculateFinesBtn);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Table
        finesModel = new DefaultTableModel(new String[]{
                "ID", "Member", "Email", "Book", "Due Date", "Return Date",
                "Days Late", "Amount (K)", "Reason", "Status", "Payment Date"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        finesTable = new JTable(finesModel);
        finesTable.setRowHeight(28);
        finesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        finesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        finesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        finesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        finesTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        finesTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        finesTable.getColumnModel().getColumn(6).setMaxWidth(80);
        finesTable.getColumnModel().getColumn(7).setMaxWidth(100);
        finesTable.getColumnModel().getColumn(9).setMaxWidth(80);

        // Color coding
        finesTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 9);
                    if ("UNPAID".equals(status)) {
                        c.setBackground(new Color(255, 230, 230));
                    } else {
                        c.setBackground(new Color(230, 255, 230));
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(finesTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Fine Records"));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton markPaidBtn = new JButton("✅ Mark as Paid");
        markPaidBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        markPaidBtn.addActionListener(e -> handleMarkPaid());

        JButton viewDetailsBtn = new JButton("🔍 View Details");
        viewDetailsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        viewDetailsBtn.addActionListener(e -> handleViewFineDetails());

        JButton exportPdfBtn = new JButton("📄 Export PDF");
        exportPdfBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exportPdfBtn.addActionListener(e -> exportFinesToPDF());

        JButton sendReminderBtn = new JButton("📧 Send Reminder");
        sendReminderBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sendReminderBtn.addActionListener(e -> handleSendReminder());

        buttonPanel.add(markPaidBtn);
        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(sendReminderBtn);
        buttonPanel.add(exportPdfBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== OVERDUE PANEL =====
    private JPanel createOverduePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));

        JLabel infoLabel = new JLabel("<html><b>Note:</b> These books are currently overdue. " +
                "Fines accrue at K5.00 per day. Use 'Calculate Overdue Fines' to create fine records.</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoPanel.add(infoLabel);

        panel.add(infoPanel, BorderLayout.NORTH);

        // Table
        overdueModel = new DefaultTableModel(new String[]{
                "Member", "Email", "Phone", "Book", "Author", "Due Date",
                "Days Overdue", "Estimated Fine (K)", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        overdueTable = new JTable(overdueModel);
        overdueTable.setRowHeight(28);
        overdueTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        overdueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        overdueTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        overdueTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        overdueTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        overdueTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        overdueTable.getColumnModel().getColumn(6).setMaxWidth(100);
        overdueTable.getColumnModel().getColumn(7).setMaxWidth(120);

        // Color coding - gradient from yellow to red based on days overdue
        overdueTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    try {
                        int daysOverdue = (int) table.getValueAt(row, 6);
                        if (daysOverdue > 14) {
                            c.setBackground(new Color(255, 200, 200)); // Dark red
                        } else if (daysOverdue > 7) {
                            c.setBackground(new Color(255, 230, 200)); // Orange
                        } else {
                            c.setBackground(new Color(255, 255, 200)); // Yellow
                        }
                    } catch (Exception e) {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(overdueTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Currently Overdue Books"));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton createFineBtn = new JButton("💵 Create Fine for Selected");
        createFineBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        createFineBtn.setToolTipText("Create a fine record for the selected overdue book");
        createFineBtn.addActionListener(e -> handleCreateFine());

        JButton contactMemberBtn = new JButton("📧 Contact Member");
        contactMemberBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contactMemberBtn.addActionListener(e -> handleContactMember());

        JButton refreshOverdueBtn = new JButton("🔄 Refresh");
        refreshOverdueBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshOverdueBtn.addActionListener(e -> {
            loadOverdueBooks();
            updateStats();
        });

        buttonPanel.add(createFineBtn);
        buttonPanel.add(contactMemberBtn);
        buttonPanel.add(refreshOverdueBtn);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== LOAD DATA =====
    private void loadFines() {
        finesModel.setRowCount(0);
        String filter = (String) fineFilterCombo.getSelectedItem();

        try (Connection conn = DBHelper.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT f.id, CONCAT(m.fname, ' ', m.lname) as member_name, m.email, " +
                            "b.title, bb.due_date, bb.return_date, " +
                            "DATEDIFF(COALESCE(bb.return_date, CURRENT_DATE), bb.due_date) as days_late, " +
                            "f.amount, f.reason, " +
                            "CASE WHEN f.paid = TRUE THEN 'PAID' ELSE 'UNPAID' END as status, " +
                            "f.payment_date " +
                            "FROM fines f " +
                            "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                            "JOIN members m ON bb.member_id = m.id " +
                            "JOIN books b ON bb.book_id = b.id "
            );

            if ("Unpaid Only".equals(filter)) {
                sql.append("WHERE f.paid = FALSE ");
            } else if ("Paid Only".equals(filter)) {
                sql.append("WHERE f.paid = TRUE ");
            } else if ("This Month".equals(filter)) {
                sql.append("WHERE MONTH(f.payment_date) = MONTH(CURRENT_DATE) " +
                        "AND YEAR(f.payment_date) = YEAR(CURRENT_DATE) ");
            }

            sql.append("ORDER BY f.paid ASC, f.id DESC");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());

            while (rs.next()) {
                finesModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("member_name"),
                        rs.getString("email"),
                        rs.getString("title"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getInt("days_late"),
                        String.format("%.2f", rs.getDouble("amount")),
                        rs.getString("reason"),
                        rs.getString("status"),
                        rs.getDate("payment_date")
                });
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage());
        }
    }

    private void loadOverdueBooks() {
        overdueModel.setRowCount(0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT CONCAT(m.fname, ' ', m.lname) as member_name, m.email, m.phone, " +
                    "b.title, b.author, bb.due_date, bb.id, " +
                    "DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue " +
                    "FROM borrowed_books bb " +
                    "JOIN members m ON bb.member_id = m.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.status='BORROWED' AND bb.due_date < CURRENT_DATE " +
                    "ORDER BY days_overdue DESC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int daysOverdue = rs.getInt("days_overdue");
                double estimatedFine = daysOverdue * 5.00; // K5 per day

                // Check if fine already exists
                int borrowId = rs.getInt("id");
                String checkSql = "SELECT COUNT(*) FROM fines WHERE borrow_id=?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, borrowId);
                ResultSet checkRs = checkStmt.executeQuery();
                checkRs.next();
                String status = checkRs.getInt(1) > 0 ? "Fine Created" : "No Fine Yet";

                overdueModel.addRow(new Object[]{
                        rs.getString("member_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDate("due_date"),
                        daysOverdue,
                        String.format("%.2f", estimatedFine),
                        status
                });
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading overdue books: " + ex.getMessage());
        }
    }

    private void updateStats() {
        try (Connection conn = DBHelper.getConnection()) {
            // Total unpaid
            String sql = "SELECT COALESCE(SUM(amount), 0) FROM fines WHERE paid=FALSE";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                totalUnpaidLabel.setText("K " + String.format("%.2f", rs.getDouble(1)));
            }

            // Total paid
            sql = "SELECT COALESCE(SUM(amount), 0) FROM fines WHERE paid=TRUE";
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                totalPaidLabel.setText("K " + String.format("%.2f", rs.getDouble(1)));
            }

            // Estimated dues (from overdue books without fines)
            sql = "SELECT COUNT(*) * 5.00 * AVG(DATEDIFF(CURRENT_DATE, bb.due_date)) as estimated " +
                    "FROM borrowed_books bb " +
                    "LEFT JOIN fines f ON bb.id = f.borrow_id " +
                    "WHERE bb.status='BORROWED' AND bb.due_date < CURRENT_DATE AND f.id IS NULL";
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                totalDuesLabel.setText("K " + String.format("%.2f", rs.getDouble("estimated")));
            }

            // Overdue count
            sql = "SELECT COUNT(*) FROM borrowed_books WHERE status='BORROWED' AND due_date < CURRENT_DATE";
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt(1);
                overdueCountLabel.setText(count + " Book" + (count != 1 ? "s" : ""));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== HANDLERS =====

    private void handleCalculateFines() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will automatically calculate and create fine records for all overdue books.\n\n" +
                        "Fine rate: K5.00 per day\n\n" +
                        "Do you want to continue?",
                "Calculate Fines",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL calculate_overdue_fines()}");
            stmt.execute();

            JOptionPane.showMessageDialog(this,
                    "✅ Fines calculated successfully!\n\nRefreshing data...",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            loadFines();
            loadOverdueBooks();
            updateStats();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error calculating fines: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleMarkPaid() {
        int row = finesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a fine!");
            return;
        }

        String status = (String) finesModel.getValueAt(row, 9);
        if ("PAID".equals(status)) {
            JOptionPane.showMessageDialog(this, "This fine is already paid!");
            return;
        }

        int fineId = (int) finesModel.getValueAt(row, 0);
        String member = (String) finesModel.getValueAt(row, 1);
        String amount = (String) finesModel.getValueAt(row, 7);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark fine as PAID?\n\n" +
                        "Member: " + member + "\n" +
                        "Amount: K" + amount,
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE fines SET paid=TRUE, payment_date=CURRENT_DATE WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fineId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Fine marked as PAID!");
            loadFines();
            updateStats();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void handleCreateFine() {
        int row = overdueTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an overdue book!");
            return;
        }

        String status = (String) overdueModel.getValueAt(row, 8);
        if ("Fine Created".equals(status)) {
            JOptionPane.showMessageDialog(this, "Fine already exists for this book!");
            return;
        }

        String member = (String) overdueModel.getValueAt(row, 0);
        String book = (String) overdueModel.getValueAt(row, 3);
        int daysOverdue = (int) overdueModel.getValueAt(row, 6);
        String estimatedFine = (String) overdueModel.getValueAt(row, 7);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Create fine record?\n\n" +
                        "Member: " + member + "\n" +
                        "Book: " + book + "\n" +
                        "Days Overdue: " + daysOverdue + "\n" +
                        "Fine Amount: K" + estimatedFine,
                "Create Fine",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Implementation similar to handleCalculateFines but for single record
        JOptionPane.showMessageDialog(this,
                "Fine creation would happen here.\n" +
                        "Use 'Calculate Overdue Fines' for bulk processing.");
    }

    private void handleContactMember() {
        int row = overdueTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a member!");
            return;
        }

        String member = (String) overdueModel.getValueAt(row, 0);
        String email = (String) overdueModel.getValueAt(row, 1);
        String phone = (String) overdueModel.getValueAt(row, 2);
        String book = (String) overdueModel.getValueAt(row, 3);

        String message = "Contact Member\n\n" +
                "Name: " + member + "\n" +
                "Email: " + email + "\n" +
                "Phone: " + phone + "\n\n" +
                "Overdue Book: " + book + "\n\n" +
                "(In a real system, this would send an email/SMS)";

        JOptionPane.showMessageDialog(this, message, "Contact Member", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleViewFineDetails() {
        // Implementation similar to FinesPanel viewDetails
        JOptionPane.showMessageDialog(this, "Fine details view...");
    }

    private void handleSendReminder() {
        // Implementation similar to FinesPanel sendReminder
        JOptionPane.showMessageDialog(this, "Sending reminder...");
    }

    private void exportFinesToPDF() {
        // Implementation similar to FinesPanel exportPDF
        JOptionPane.showMessageDialog(this, "Exporting to PDF...");
    }
}