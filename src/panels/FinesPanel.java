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

public class FinesPanel extends JPanel {
    private JTable finesTable;
    private DefaultTableModel tableModel;
    private JButton markPaidButton, viewDetailsButton, exportPDFButton, refreshButton;
    private JLabel totalUnpaidLabel, totalPaidLabel, statsLabel;
    private JComboBox<String> filterCombo;

    public FinesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== TOP PANEL - Stats and Filter =====
        JPanel topPanel = new JPanel(new BorderLayout());

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("📊 Fine Statistics"));

        totalUnpaidLabel = new JLabel("Total Unpaid: K0.00");
        totalUnpaidLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalUnpaidLabel.setForeground(new Color(220, 53, 69));

        totalPaidLabel = new JLabel("Total Paid: K0.00");
        totalPaidLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalPaidLabel.setForeground(new Color(40, 167, 69));

        statsLabel = new JLabel("Total Fines: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        statsPanel.add(totalUnpaidLabel);
        statsPanel.add(totalPaidLabel);
        statsPanel.add(statsLabel);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Filter:"));
        filterCombo = new JComboBox<>(new String[]{"All Fines", "Unpaid Only", "Paid Only"});
        filterCombo.addActionListener(e -> loadFines());
        refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> {
            loadFines();
            updateStats();
        });
        filterPanel.add(filterCombo);
        filterPanel.add(refreshButton);

        topPanel.add(statsPanel, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ===== TABLE PANEL =====
        tableModel = new DefaultTableModel(new String[]{
                "Fine ID", "Member Name", "Email", "Book Title", "Due Date", "Return Date",
                "Days Late", "Amount (K)", "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        finesTable = new JTable(tableModel);
        finesTable.getColumnModel().getColumn(0).setMaxWidth(60);
        finesTable.getColumnModel().getColumn(6).setMaxWidth(80);
        finesTable.getColumnModel().getColumn(7).setMaxWidth(100);
        finesTable.getColumnModel().getColumn(9).setMaxWidth(80);

        // Color code rows based on payment status
        finesTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 9);
                    if ("UNPAID".equals(status)) {
                        c.setBackground(new Color(255, 230, 230)); // Light red
                    } else {
                        c.setBackground(new Color(230, 255, 230)); // Light green
                    }
                }
                return c;
            }
        });

        add(new JScrollPane(finesTable), BorderLayout.CENTER);

        // ===== BOTTOM PANEL - Action Buttons =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        markPaidButton = new JButton("✅ Mark as Paid");
        markPaidButton.setToolTipText("Mark selected fine as paid");
        markPaidButton.addActionListener(e -> handleMarkPaid());

        viewDetailsButton = new JButton("🔍 View Details");
        viewDetailsButton.setToolTipText("View detailed information about selected fine");
        viewDetailsButton.addActionListener(e -> handleViewDetails());

        exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.setToolTipText("Export fines to PDF");
        exportPDFButton.addActionListener(e -> exportToPDF());

        JButton sendReminderButton = new JButton("📧 Send Reminder");
        sendReminderButton.setToolTipText("Send payment reminder to member");
        sendReminderButton.addActionListener(e -> handleSendReminder());

        bottomPanel.add(markPaidButton);
        bottomPanel.add(viewDetailsButton);
        bottomPanel.add(sendReminderButton);
        bottomPanel.add(exportPDFButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Load initial data
        loadFines();
        updateStats();
    }

    // ===== Load Fines Based on Filter =====
    private void loadFines() {
        tableModel.setRowCount(0);
        String filter = (String) filterCombo.getSelectedItem();

        try (Connection conn = DBHelper.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                            "f.id AS fine_id, " +
                            "CONCAT(m.fname, ' ', m.lname) AS member_name, " +
                            "m.email, " +
                            "b.title AS book_title, " +
                            "bb.due_date, " +
                            "bb.return_date, " +
                            "DATEDIFF(bb.return_date, bb.due_date) AS days_late, " +
                            "f.amount, " +
                            "f.reason, " +
                            "CASE WHEN f.paid = TRUE THEN 'PAID' ELSE 'UNPAID' END AS status " +
                            "FROM fines f " +
                            "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                            "JOIN members m ON bb.member_id = m.id " +
                            "JOIN books b ON bb.book_id = b.id "
            );

            // Apply filter
            if ("Unpaid Only".equals(filter)) {
                sql.append("WHERE f.paid = FALSE ");
            } else if ("Paid Only".equals(filter)) {
                sql.append("WHERE f.paid = TRUE ");
            }

            sql.append("ORDER BY f.id DESC");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql.toString());

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("fine_id"),
                        rs.getString("member_name"),
                        rs.getString("email"),
                        rs.getString("book_title"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getInt("days_late"),
                        String.format("%.2f", rs.getDouble("amount")),
                        rs.getString("reason"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage());
        }
    }

    // ===== Update Statistics =====
    private void updateStats() {
        try (Connection conn = DBHelper.getConnection()) {
            // Total unpaid
            String unpaidSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM fines WHERE paid = FALSE";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(unpaidSql);
            if (rs.next()) {
                double unpaid = rs.getDouble("total");
                totalUnpaidLabel.setText("Total Unpaid: K" + String.format("%.2f", unpaid));
            }

            // Total paid
            String paidSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM fines WHERE paid = TRUE";
            rs = stmt.executeQuery(paidSql);
            if (rs.next()) {
                double paid = rs.getDouble("total");
                totalPaidLabel.setText("Total Paid: K" + String.format("%.2f", paid));
            }

            // Total count
            String countSql = "SELECT COUNT(*) AS total FROM fines";
            rs = stmt.executeQuery(countSql);
            if (rs.next()) {
                int count = rs.getInt("total");
                statsLabel.setText("Total Fines: " + count);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ===== Mark Fine as Paid =====
    private void handleMarkPaid() {
        int row = finesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a fine to mark as paid!");
            return;
        }

        int fineId = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 9);

        if ("PAID".equals(status)) {
            JOptionPane.showMessageDialog(this, "This fine has already been paid!");
            return;
        }

        String amount = (String) tableModel.getValueAt(row, 7);
        String member = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark fine as PAID?\n\n" +
                        "Member: " + member + "\n" +
                        "Amount: K" + amount,
                "Confirm Payment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE fines SET paid = TRUE, payment_date = CURRENT_DATE WHERE id = ?";
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

    // ===== View Fine Details =====
    private void handleViewDetails() {
        int row = finesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a fine to view details!");
            return;
        }

        int fineId = (int) tableModel.getValueAt(row, 0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT " +
                    "f.*, " +
                    "CONCAT(m.fname, ' ', m.lname) AS member_name, " +
                    "m.email, m.phone, " +
                    "b.title, b.author, b.isbn, " +
                    "bb.borrow_date, bb.due_date, bb.return_date " +
                    "FROM fines f " +
                    "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                    "JOIN members m ON bb.member_id = m.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE f.id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fineId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                StringBuilder details = new StringBuilder();
                details.append("═══════════════════════════════════\n");
                details.append("            FINE DETAILS\n");
                details.append("═══════════════════════════════════\n\n");

                details.append("📋 Fine ID: ").append(rs.getInt("id")).append("\n");
                details.append("💰 Amount: K").append(String.format("%.2f", rs.getDouble("amount"))).append("\n");
                details.append("📝 Reason: ").append(rs.getString("reason")).append("\n");
                details.append("✅ Status: ").append(rs.getBoolean("paid") ? "PAID" : "UNPAID").append("\n");
                if (rs.getDate("payment_date") != null) {
                    details.append("📅 Payment Date: ").append(rs.getDate("payment_date")).append("\n");
                }

                details.append("\n👤 MEMBER INFORMATION\n");
                details.append("───────────────────────────────────\n");
                details.append("Name: ").append(rs.getString("member_name")).append("\n");
                details.append("Email: ").append(rs.getString("email")).append("\n");
                details.append("Phone: ").append(rs.getString("phone")).append("\n");

                details.append("\n📚 BOOK INFORMATION\n");
                details.append("───────────────────────────────────\n");
                details.append("Title: ").append(rs.getString("title")).append("\n");
                details.append("Author: ").append(rs.getString("author")).append("\n");
                details.append("ISBN: ").append(rs.getString("isbn")).append("\n");

                details.append("\n📅 BORROWING DETAILS\n");
                details.append("───────────────────────────────────\n");
                details.append("Borrow Date: ").append(rs.getDate("borrow_date")).append("\n");
                details.append("Due Date: ").append(rs.getDate("due_date")).append("\n");
                details.append("Return Date: ").append(rs.getDate("return_date")).append("\n");

                java.util.Date dueDate = rs.getDate("due_date");
                java.util.Date returnDate = rs.getDate("return_date");
                if (returnDate != null && dueDate != null) {
                    long diff = returnDate.getTime() - dueDate.getTime();
                    long days = diff / (1000 * 60 * 60 * 24);
                    details.append("Days Overdue: ").append(days).append("\n");
                }

                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(450, 500));

                JOptionPane.showMessageDialog(this, scrollPane, "Fine Details", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== Send Payment Reminder =====
    private void handleSendReminder() {
        int row = finesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a fine!");
            return;
        }

        String status = (String) tableModel.getValueAt(row, 9);
        if ("PAID".equals(status)) {
            JOptionPane.showMessageDialog(this, "This fine has already been paid!");
            return;
        }

        String member = (String) tableModel.getValueAt(row, 1);
        String email = (String) tableModel.getValueAt(row, 2);
        String amount = (String) tableModel.getValueAt(row, 7);

        String message = "Payment Reminder\n\n" +
                "Dear " + member + ",\n\n" +
                "You have an outstanding library fine of K" + amount + ".\n" +
                "Please visit the library to settle this payment.\n\n" +
                "Email: " + email + "\n\n" +
                "(In a real system, this would send an email)";

        JOptionPane.showMessageDialog(this, message, "Reminder", JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== Export to PDF =====
    private void exportToPDF() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            doc.add(new Paragraph("Library Fines Report\n\n"));
            doc.add(new Paragraph(totalUnpaidLabel.getText() + "\n"));
            doc.add(new Paragraph(totalPaidLabel.getText() + "\n"));
            doc.add(new Paragraph(statsLabel.getText() + "\n\n"));

            PdfPTable pdfTable = new PdfPTable(tableModel.getColumnCount());

            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                pdfTable.addCell(new Phrase(tableModel.getColumnName(i)));
            }

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object value = tableModel.getValueAt(row, col);
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