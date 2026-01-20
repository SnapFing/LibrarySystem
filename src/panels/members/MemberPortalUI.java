package panels.members;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;
import panels.LoginUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Member Portal - Self-service interface for library members
 * Features: Browse books, view borrowed books, check fines
 *
 * NOTE: This class is now a JPanel (not a JFrame) and provides a helper
 * showInFrame(...) for legacy/testing usage. The panel is designed to be
 * embedded into a parent window (for example after login).
 */
public class MemberPortalUI extends JPanel {
    private final int memberId;
    private final String memberName;
    private JTabbedPane tabbedPane;
    private JLabel dateTimeLabel, statsLabel;

    // Browse Books Tab
    private JTable booksTable;
    private DefaultTableModel booksModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;

    // My Books Tab
    private JTable borrowedTable;
    private DefaultTableModel borrowedModel;

    // My Fines Tab
    private JTable finesTable;
    private DefaultTableModel finesModel;
    private JLabel totalFinesLabel;

    public MemberPortalUI(int memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;

        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        startClock();
        loadAllData();
    }

    // Helper to show this panel inside a JFrame (keeps compatibility with older call-sites)
    public static void showInFrame(int memberId, String memberName) {
        JFrame frame = new JFrame("📚 Library Member Portal - " + memberName);
        frame.setSize(1100, 700);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            ImageIcon icon = new ImageIcon(MemberPortalUI.class.getResource("/panels/SNAPFING-LOGO.png"));
            frame.setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        MemberPortalUI panel = new MemberPortalUI(memberId, memberName);
        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    // ===== TOP BAR =====
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 30, 30));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Left - Logo + Welcome
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = raw.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {}

        JLabel welcome = new JLabel("👤 Welcome, " + memberName);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcome.setForeground(Color.WHITE);

        left.add(logo);
        left.add(welcome);

        // Right - Stats and Logout (keeps previous layout)
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        row1.setOpaque(false);

        statsLabel = new JLabel("Loading...");
        statsLabel.setForeground(new Color(200, 200, 200));
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setToolTipText("Logout");
        logoutBtn.addActionListener(e -> logout());

        row1.add(statsLabel);
        row1.add(Box.createHorizontalStrut(15));
        row1.add(logoutBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        row2.setOpaque(false);
        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateTimeLabel.setForeground(new Color(200, 200, 200));
        row2.add(dateTimeLabel);

        right.add(row1);
        right.add(Box.createVerticalStrut(8));
        right.add(row2);

        topBar.add(left, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        return topBar;
    }

    // ===== TABS =====
    private JTabbedPane createTabs() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tabbedPane.addTab("📚 Browse Books", createBrowseBooksPanel());
        tabbedPane.addTab("📖 My Borrowed Books", createMyBooksPanel());
        tabbedPane.addTab("💰 My Fines", createMyFinesPanel());
        tabbedPane.addTab("👤 My Profile", createProfilePanel());

        return tabbedPane;
    }

    // ===== BROWSE BOOKS PANEL =====
    private JPanel createBrowseBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.add(new JLabel("🔍 Search:"));
        searchField = new JTextField(20);
        searchField.setToolTipText("Search by title, author, or ISBN");
        searchPanel.add(searchField);

        searchPanel.add(new JLabel("Category:"));
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("-- All Categories --");
        loadCategories();
        searchPanel.add(categoryFilter);

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> searchBooks());
        searchPanel.add(searchBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            categoryFilter.setSelectedIndex(0);
            loadAvailableBooks();
        });
        searchPanel.add(clearBtn);

        panel.add(searchPanel, BorderLayout.NORTH);

        // Books Table
        booksModel = new DefaultTableModel(
                new String[]{"ID", "Title", "Author", "ISBN", "Category", "Available", "Shelf Location"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        booksTable = new JTable(booksModel);
        booksTable.getColumnModel().getColumn(0).setMaxWidth(50);
        booksTable.getColumnModel().getColumn(5).setMaxWidth(80);

        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton viewDetailsBtn = new JButton("📖 View Details");
        viewDetailsBtn.addActionListener(e -> viewBookDetails());
        bottomPanel.add(viewDetailsBtn);

        JLabel noteLabel = new JLabel("ℹ️ To borrow books, please visit the library counter");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        bottomPanel.add(noteLabel);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== MY BORROWED BOOKS PANEL =====
    private JPanel createMyBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        borrowedModel = new DefaultTableModel(
                new String[]{"Borrow ID", "Book Title", "Author", "Borrowed Date", "Due Date", "Return Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        borrowedTable = new JTable(borrowedModel);
        borrowedTable.getColumnModel().getColumn(0).setMaxWidth(80);

        // Color code rows
        borrowedTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 6);
                    if ("OVERDUE".equals(status)) {
                        c.setBackground(new Color(255, 230, 230)); // Light red
                    } else if ("RETURNED".equals(status)) {
                        c.setBackground(new Color(230, 255, 230)); // Light green
                    } else {
                        c.setBackground(new Color(230, 240, 255)); // Light blue
                    }
                }
                return c;
            }
        });

        panel.add(new JScrollPane(borrowedTable), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadBorrowedBooks());
        bottomPanel.add(refreshBtn);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== MY FINES PANEL =====
    private JPanel createMyFinesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Fine Summary"));
        totalFinesLabel = new JLabel("Total Unpaid Fines: K0.00");
        totalFinesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalFinesLabel.setForeground(new Color(220, 53, 69));
        statsPanel.add(totalFinesLabel);

        panel.add(statsPanel, BorderLayout.NORTH);

        // Fines Table
        finesModel = new DefaultTableModel(
                new String[]{"Fine ID", "Book Title", "Due Date", "Return Date", "Days Late", "Amount (K)", "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        finesTable = new JTable(finesModel);
        finesTable.getColumnModel().getColumn(0).setMaxWidth(70);
        finesTable.getColumnModel().getColumn(4).setMaxWidth(80);
        finesTable.getColumnModel().getColumn(5).setMaxWidth(80);
        finesTable.getColumnModel().getColumn(7).setMaxWidth(80);

        // Color code rows
        finesTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 7);
                    if ("UNPAID".equals(status)) {
                        c.setBackground(new Color(255, 230, 230));
                    } else {
                        c.setBackground(new Color(230, 255, 230));
                    }
                }
                return c;
            }
        });

        panel.add(new JScrollPane(finesTable), BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadFines());
        bottomPanel.add(refreshBtn);

        JLabel noteLabel = new JLabel("ℹ️ To pay fines, please visit the library counter");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        bottomPanel.add(noteLabel);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ===== PROFILE PANEL =====
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT * FROM members WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                gbc.gridx = 0; gbc.gridy = 0;
                centerPanel.add(new JLabel("Member ID:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(String.valueOf(rs.getInt("id"))), gbc);

                gbc.gridx = 0; gbc.gridy = 1;
                centerPanel.add(new JLabel("First Name:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getString("fname")), gbc);

                gbc.gridx = 0; gbc.gridy = 2;
                centerPanel.add(new JLabel("Last Name:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getString("lname")), gbc);

                gbc.gridx = 0; gbc.gridy = 3;
                centerPanel.add(new JLabel("Email:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getString("email")), gbc);

                gbc.gridx = 0; gbc.gridy = 4;
                centerPanel.add(new JLabel("Phone:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getString("phone")), gbc);

                gbc.gridx = 0; gbc.gridy = 5;
                centerPanel.add(new JLabel("Address:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getString("address")), gbc);

                gbc.gridx = 0; gbc.gridy = 6;
                centerPanel.add(new JLabel("Member Since:"), gbc);
                gbc.gridx = 1;
                centerPanel.add(new JLabel(rs.getDate("membership_date").toString()), gbc);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    // ===== STATUS BAR =====
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(40, 40, 40));

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 180, 180));

        statusBar.add(statusLabel, BorderLayout.WEST);

        return statusBar;
    }

    // ===== DATA LOADING =====
    private void loadAllData() {
        loadAvailableBooks();
        loadBorrowedBooks();
        loadFines();
        updateStats();
    }

    private void loadCategories() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT name FROM categories ORDER BY name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                categoryFilter.addItem(rs.getString("name"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadAvailableBooks() {
        booksModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.id, b.title, b.author, b.isbn, c.name as category, " +
                    "b.available_quantity, b.shelf_location " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "WHERE b.available_quantity > 0 " +
                    "ORDER BY b.title";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                booksModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("category"),
                        rs.getInt("available_quantity"),
                        rs.getString("shelf_location")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void searchBooks() {
        String keyword = searchField.getText().trim().toLowerCase();
        String category = (String) categoryFilter.getSelectedItem();

        booksModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT b.id, b.title, b.author, b.isbn, c.name as category, " +
                            "b.available_quantity, b.shelf_location " +
                            "FROM books b " +
                            "LEFT JOIN categories c ON b.category_id = c.id " +
                            "WHERE b.available_quantity > 0 ");

            if (!keyword.isEmpty()) {
                sql.append("AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR LOWER(b.isbn) LIKE ?) ");
            }

            if (category != null && !"-- All Categories --".equals(category)) {
                sql.append("AND c.name = ? ");
            }

            sql.append("ORDER BY b.title");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            int paramIndex = 1;

            if (!keyword.isEmpty()) {
                String pattern = "%" + keyword + "%";
                stmt.setString(paramIndex++, pattern);
                stmt.setString(paramIndex++, pattern);
                stmt.setString(paramIndex++, pattern);
            }

            if (category != null && !"-- All Categories --".equals(category)) {
                stmt.setString(paramIndex, category);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                booksModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getString("category"),
                        rs.getInt("available_quantity"),
                        rs.getString("shelf_location")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadBorrowedBooks() {
        borrowedModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT bb.id, b.title, b.author, bb.borrow_date, bb.due_date, " +
                    "bb.return_date, bb.status " +
                    "FROM borrowed_books bb " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id = ? " +
                    "ORDER BY bb.borrow_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                borrowedModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getDate("borrow_date"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getString("status")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadFines() {
        finesModel.setRowCount(0);
        double totalUnpaid = 0;

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT f.id, b.title, bb.due_date, bb.return_date, " +
                    "DATEDIFF(bb.return_date, bb.due_date) as days_late, " +
                    "f.amount, f.reason, " +
                    "CASE WHEN f.paid = TRUE THEN 'PAID' ELSE 'UNPAID' END as status " +
                    "FROM fines f " +
                    "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id = ? " +
                    "ORDER BY f.id DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getString("status");
                double amount = rs.getDouble("amount");

                if ("UNPAID".equals(status)) {
                    totalUnpaid += amount;
                }

                finesModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getDate("due_date"),
                        rs.getDate("return_date"),
                        rs.getInt("days_late"),
                        String.format("%.2f", amount),
                        rs.getString("reason"),
                        status
                });
            }

            totalFinesLabel.setText("Total Unpaid Fines: K" + String.format("%.2f", totalUnpaid));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void viewBookDetails() {
        int row = booksTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book!");
            return;
        }

        int bookId = (int) booksModel.getValueAt(row, 0);

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.*, c.name as category FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id WHERE b.id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                StringBuilder details = new StringBuilder();
                details.append("═══════════════════════════════════\n");
                details.append("          BOOK DETAILS\n");
                details.append("═══════════════════════════════════\n\n");
                details.append("Title: ").append(rs.getString("title")).append("\n");
                details.append("Author: ").append(rs.getString("author")).append("\n");
                details.append("ISBN: ").append(rs.getString("isbn")).append("\n");
                details.append("Category: ").append(rs.getString("category")).append("\n");
                details.append("Publisher: ").append(rs.getString("publisher")).append("\n");
                details.append("Publish Year: ").append(rs.getInt("publish_year")).append("\n");
                details.append("Available: ").append(rs.getInt("available_quantity")).append(" copies\n");
                details.append("Shelf Location: ").append(rs.getString("shelf_location")).append("\n");

                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));

                JOptionPane.showMessageDialog(this, scrollPane, "Book Details", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateStats() {
        try (Connection conn = DBHelper.getConnection()) {
            // Count borrowed books
            String sql = "SELECT COUNT(*) FROM borrowed_books WHERE member_id=? AND status='BORROWED'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int borrowed = rs.getInt(1);

            // Count unpaid fines
            sql = "SELECT COALESCE(SUM(f.amount), 0) FROM fines f " +
                    "JOIN borrowed_books bb ON f.borrow_id = bb.id " +
                    "WHERE bb.member_id=? AND f.paid=FALSE";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            rs = stmt.executeQuery();
            rs.next();
            double fines = rs.getDouble(1);

            statsLabel.setText(String.format("📖 Borrowed: %d | 💰 Fines: K%.2f", borrowed, fines));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy | HH:mm:ss");
        Timer timer = new Timer(1000, e -> dateTimeLabel.setText(sdf.format(new Date())));
        timer.start();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this),
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) {
                w.dispose();
            }
            SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LookAndFeel");
        }
        SwingUtilities.invokeLater(() -> MemberPortalUI.showInFrame(1, "Test User"));
    }
}

