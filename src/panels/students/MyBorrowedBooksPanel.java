package panels.students;

import db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MyBorrowedBooksPanel extends JPanel {
    private JTable borrowedTable;
    private DefaultTableModel tableModel;
    private String studentName;

    public MyBorrowedBooksPanel(String studentName) {
        this.studentName = studentName;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== Title Panel =====
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("📖 My Borrowed Books");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // ===== Table =====
        tableModel = new DefaultTableModel(
                new String[]{"Book Title", "publish_year", "Borrow Date", "Return Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        borrowedTable = new JTable(tableModel);
        borrowedTable.setRowHeight(25);
        borrowedTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        borrowedTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        borrowedTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        borrowedTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        borrowedTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        add(new JScrollPane(borrowedTable), BorderLayout.CENTER);

        // ===== Info Panel =====
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel infoLabel1 = new JLabel("💡 To return a book, please visit the library.");
        JLabel infoLabel2 = new JLabel("⚠️ Overdue books may incur fines.");
        infoLabel1.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel2.setForeground(new Color(200, 100, 0));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(infoLabel1);
        row2.add(infoLabel2);

        infoPanel.add(row1);
        infoPanel.add(row2);
        add(infoPanel, BorderLayout.SOUTH);

        // Load borrowed books
        loadBorrowedBooks();
    }

    private void loadBorrowedBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            // First get member ID
            String memberSql = "SELECT id FROM members WHERE name=?";
            PreparedStatement memberStmt = conn.prepareStatement(memberSql);
            memberStmt.setString(1, studentName);
            ResultSet memberRs = memberStmt.executeQuery();

            if (!memberRs.next()) {
                JOptionPane.showMessageDialog(this, "Member not found!");
                return;
            }

            int memberId = memberRs.getInt("id");

            // JOIN with books to get book details
            String sql = "SELECT b.title, b.publish_year, bb.borrow_date, bb.return_date, bb.status " +
                    "FROM borrowed_books bb " +
                    "INNER JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id=? " +
                    "ORDER BY bb.borrow_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status");
                Date returnDate = rs.getDate("return_date");
                Date today = new Date(System.currentTimeMillis());

                // Check if overdue
                if ("Borrowed".equals(status) && returnDate != null && returnDate.before(today)) {
                    status = "⚠️ OVERDUE";
                }

                tableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getInt("publish_year"),
                        rs.getDate("borrow_date"),
                        returnDate,
                        status
                });
            }

            /**
            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "You haven't borrowed any books yet.\nVisit the library to borrow books!",
                        "No Borrowed Books",
                        JOptionPane.INFORMATION_MESSAGE);
            }
             */
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading borrowed books: " + ex.getMessage());
        }
    }
}
