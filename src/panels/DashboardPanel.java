package panels;

import db.DBHelper;
import utils.RefreshManager;
import utils.RefreshManager.RefreshListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

/**
 * DashboardPanel with Comprehensive RefreshManager Integration
 *
 * This panel listens to ALL panel types and automatically refreshes
 * when any data changes anywhere in the system.
 */
public class DashboardPanel extends JPanel implements RefreshListener {

    private JLabel totalBooksLabel, availableBooksLabel, borrowedBooksLabel;
    private JLabel totalMembersLabel, activeMembersLabel;
    private JLabel borrowedTodayLabel, overdueLabel, totalFinesLabel;
    private JTextArea recentActivityArea, topBooksArea, upcomingDuesArea;
    private Timer autoRefreshTimer;
    private JCheckBox autoRefreshCheckbox;

    public DashboardPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // ✅ REGISTER with RefreshManager - listen to ALL panel types for comprehensive updates
        registerWithRefreshManager();

        initializeUI();
        refreshDashboard();

        // Start auto-refresh timer (every 30 seconds)
        startAutoRefresh();
    }

    /**
     * Register this dashboard to listen to ALL data changes
     */
    private void registerWithRefreshManager() {
        RefreshManager rm = RefreshManager.getInstance();

        // Register for all panel types
        rm.addRefreshListener(RefreshManager.PANEL_DASHBOARD, this);
        rm.addRefreshListener(RefreshManager.PANEL_MEMBERS, this);
        rm.addRefreshListener(RefreshManager.PANEL_BOOKS, this);
        rm.addRefreshListener(RefreshManager.PANEL_BORROW, this);
        rm.addRefreshListener(RefreshManager.PANEL_FINES, this);
        rm.addRefreshListener(RefreshManager.PANEL_USERS, this);

        System.out.println("✅ DashboardPanel registered for ALL panel updates");
    }

    private void initializeUI() {
        // === TITLE ===
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("📊 Library Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        // Add manual refresh button and auto-refresh checkbox
        JPanel titleRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton manualRefreshBtn = new JButton("🔄 Refresh Now");
        manualRefreshBtn.setToolTipText("Manually refresh all dashboard data");
        manualRefreshBtn.addActionListener(e -> {
            refreshDashboard();
            JOptionPane.showMessageDialog(this, "✅ Dashboard refreshed!",
                    "Refresh Complete", JOptionPane.INFORMATION_MESSAGE);
        });

        autoRefreshCheckbox = new JCheckBox("Auto-refresh (30s)");
        autoRefreshCheckbox.setSelected(true); // On by default
        autoRefreshCheckbox.setToolTipText("Automatically refresh dashboard every 30 seconds");
        autoRefreshCheckbox.addActionListener(e -> {
            if (autoRefreshCheckbox.isSelected()) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
        });

        titleRightPanel.add(autoRefreshCheckbox);
        titleRightPanel.add(manualRefreshBtn);

        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(titleRightPanel, BorderLayout.EAST);
        add(titlePanel, BorderLayout.NORTH);

        // === MAIN CONTENT ===
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));

        // Top section: Statistics cards
        JPanel statsPanel = createStatsPanel();
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // Middle section: Activity panels
        JPanel activityPanel = createActivityPanel();
        mainPanel.add(activityPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    // ===== STATISTICS CARDS PANEL =====
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 15));

        // Books Statistics
        panel.add(createStatCard("📚 Total Books", "0", new Color(52, 152, 219)));
        panel.add(createStatCard("✅ Available", "0", new Color(46, 204, 113)));
        panel.add(createStatCard("📖 Borrowed", "0", new Color(241, 196, 15)));
        panel.add(createStatCard("⚠️ Overdue", "0", new Color(231, 76, 60)));

        // Members & Activity Statistics
        panel.add(createStatCard("👥 Total Members", "0", new Color(155, 89, 182)));
        panel.add(createStatCard("🟢 Active Members", "0", new Color(26, 188, 156)));
        panel.add(createStatCard("📅 Borrowed Today", "0", new Color(52, 73, 94)));
        panel.add(createStatCard("💰 Total Fines", "K 0.00", new Color(230, 126, 34)));

        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        RoundedPanel card = new RoundedPanel(color);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        // Store label references for updating
        if (title.contains("Total Books")) totalBooksLabel = valueLabel;
        else if (title.contains("Available")) availableBooksLabel = valueLabel;
        else if (title.contains("Borrowed") && !title.contains("Today")) borrowedBooksLabel = valueLabel;
        else if (title.contains("Overdue")) overdueLabel = valueLabel;
        else if (title.contains("Total Members")) totalMembersLabel = valueLabel;
        else if (title.contains("Active Members")) activeMembersLabel = valueLabel;
        else if (title.contains("Borrowed Today")) borrowedTodayLabel = valueLabel;
        else if (title.contains("Total Fines")) totalFinesLabel = valueLabel;

        return card;
    }

    // ===== ACTIVITY PANELS =====
    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 15));

        // Recent Activity
        panel.add(createActivityCard("📋 Recent Activity", createRecentActivityArea()));

        // Top 5 Most Borrowed Books
        panel.add(createActivityCard("⭐ Top Borrowed Books", createTopBooksArea()));

        // Upcoming Due Dates
        panel.add(createActivityCard("📅 Upcoming Due Dates", createUpcomingDuesArea()));

        return panel;
    }

    private JPanel createActivityCard(String title, JTextArea textArea) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createTitledBorder(title));

        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 250));
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JTextArea createRecentActivityArea() {
        recentActivityArea = new JTextArea();
        return recentActivityArea;
    }

    private JTextArea createTopBooksArea() {
        topBooksArea = new JTextArea();
        return topBooksArea;
    }

    private JTextArea createUpcomingDuesArea() {
        upcomingDuesArea = new JTextArea();
        return upcomingDuesArea;
    }

    // ===== DATA REFRESH =====
    private void refreshDashboard() {
        // Show visual feedback that refresh is happening
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private int totalBooks, availableBooks, borrowedBooks, overdueBooks;
            private int totalMembers, activeMembers, borrowedToday;
            private double totalFines;
            private String recentActivity, topBooks, upcomingDues;

            @Override
            protected Void doInBackground() {
                try (Connection conn = DBHelper.getConnection()) {
                    loadBookStatistics(conn);
                    loadMemberStatistics(conn);
                    loadActivityStatistics(conn);
                    loadFineStatistics(conn);
                    loadRecentActivity(conn);
                    loadTopBooks(conn);
                    loadUpcomingDues(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                updateUI();
                setCursor(Cursor.getDefaultCursor());
                System.out.println("🔄 Dashboard data refreshed at " +
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
            }

            private void loadBookStatistics(Connection conn) throws SQLException {
                String sql = "SELECT " +
                        "SUM(total_quantity) as total, " +
                        "SUM(available_quantity) as available, " +
                        "SUM(total_quantity - available_quantity) as borrowed " +
                        "FROM books";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    totalBooks = rs.getInt("total");
                    availableBooks = rs.getInt("available");
                    borrowedBooks = rs.getInt("borrowed");
                }

                // Overdue books
                String overdueSql = "SELECT COUNT(*) as count FROM borrowed_books " +
                        "WHERE status='BORROWED' AND due_date < CURRENT_DATE";
                rs = stmt.executeQuery(overdueSql);
                if (rs.next()) {
                    overdueBooks = rs.getInt("count");
                }
            }

            private void loadMemberStatistics(Connection conn) throws SQLException {
                String sql = "SELECT COUNT(*) as total, " +
                        "SUM(CASE WHEN is_active=TRUE THEN 1 ELSE 0 END) as active " +
                        "FROM members";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    totalMembers = rs.getInt("total");
                    activeMembers = rs.getInt("active");
                }
            }

            private void loadActivityStatistics(Connection conn) throws SQLException {
                String sql = "SELECT COUNT(*) as count FROM borrowed_books " +
                        "WHERE DATE(borrow_date) = CURRENT_DATE";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    borrowedToday = rs.getInt("count");
                }
            }

            private void loadFineStatistics(Connection conn) throws SQLException {
                String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM fines WHERE paid=FALSE";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    totalFines = rs.getDouble("total");
                }
            }

            private void loadRecentActivity(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT al.action, al.action_time, u.username " +
                        "FROM audit_logs al " +
                        "JOIN users u ON al.user_id = u.id " +
                        "ORDER BY al.action_time DESC LIMIT 10";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    String action = rs.getString("action");
                    String time = rs.getTimestamp("action_time").toString().substring(11, 16);
                    String user = rs.getString("username");
                    sb.append("• ").append(time).append(" - ").append(action)
                            .append(" (").append(user).append(")\n");
                }

                recentActivity = sb.length() > 0 ? sb.toString() : "No recent activity";
            }

            private void loadTopBooks(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT b.title, COUNT(*) as borrow_count " +
                        "FROM borrowed_books bb " +
                        "JOIN books b ON bb.book_id = b.id " +
                        "GROUP BY b.id, b.title " +
                        "ORDER BY borrow_count DESC LIMIT 5";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                int rank = 1;
                while (rs.next()) {
                    String title = rs.getString("title");
                    int count = rs.getInt("borrow_count");
                    if (title.length() > 30) {
                        title = title.substring(0, 27) + "...";
                    }
                    sb.append(rank++).append(". ").append(title)
                            .append(" (").append(count).append(" times)\n");
                }

                topBooks = sb.length() > 0 ? sb.toString() : "No borrowing data available";
            }

            private void loadUpcomingDues(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT CONCAT(m.fname, ' ', m.lname) as member, " +
                        "b.title, bb.due_date " +
                        "FROM borrowed_books bb " +
                        "JOIN members m ON bb.member_id = m.id " +
                        "JOIN books b ON bb.book_id = b.id " +
                        "WHERE bb.status='BORROWED' AND bb.due_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) " +
                        "ORDER BY bb.due_date ASC LIMIT 10";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    String member = rs.getString("member");
                    String title = rs.getString("title");
                    String dueDate = rs.getDate("due_date").toString();

                    if (title.length() > 25) {
                        title = title.substring(0, 22) + "...";
                    }

                    sb.append("• ").append(member).append("\n")
                            .append("  ").append(title).append("\n")
                            .append("  Due: ").append(dueDate).append("\n\n");
                }

                upcomingDues = sb.length() > 0 ? sb.toString() : "No books due in next 7 days";
            }

            private void updateUI() {
                totalBooksLabel.setText(String.valueOf(totalBooks));
                availableBooksLabel.setText(String.valueOf(availableBooks));
                borrowedBooksLabel.setText(String.valueOf(borrowedBooks));
                overdueLabel.setText(String.valueOf(overdueBooks));

                totalMembersLabel.setText(String.valueOf(totalMembers));
                activeMembersLabel.setText(String.valueOf(activeMembers));
                borrowedTodayLabel.setText(String.valueOf(borrowedToday));
                totalFinesLabel.setText("K " + String.format("%.2f", totalFines));

                recentActivityArea.setText(recentActivity);
                topBooksArea.setText(topBooks);
                upcomingDuesArea.setText(upcomingDues);

                // Color code overdue
                if (overdueBooks > 0) {
                    overdueLabel.setForeground(Color.RED);
                } else {
                    overdueLabel.setForeground(new Color(46, 204, 113));
                }
            }
        };

        worker.execute();
    }

    // ===== AUTO-REFRESH TIMER =====
    private void startAutoRefresh() {
        if (autoRefreshTimer == null) {
            autoRefreshTimer = new Timer(30000, e -> refreshDashboard()); // 30 seconds
        }
        if (!autoRefreshTimer.isRunning()) {
            autoRefreshTimer.start();
            System.out.println("▶️ Dashboard auto-refresh started (every 30 seconds)");
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
            System.out.println("⏸️ Dashboard auto-refresh stopped");
        }
    }

    // ✅ IMPLEMENT REFRESH LISTENER INTERFACE
    @Override
    public void onRefresh() {
        // Use SwingUtilities to ensure UI updates on EDT
        SwingUtilities.invokeLater(() -> {
            refreshDashboard();
            System.out.println("🔄 DashboardPanel auto-refreshed due to data change");
        });
    }

    // ✅ CLEANUP WHEN PANEL IS REMOVED
    @Override
    public void removeNotify() {
        super.removeNotify();

        // Stop auto-refresh timer
        stopAutoRefresh();

        // Unregister from ALL panel types
        RefreshManager rm = RefreshManager.getInstance();
        rm.removeRefreshListener(RefreshManager.PANEL_DASHBOARD, this);
        rm.removeRefreshListener(RefreshManager.PANEL_MEMBERS, this);
        rm.removeRefreshListener(RefreshManager.PANEL_BOOKS, this);
        rm.removeRefreshListener(RefreshManager.PANEL_BORROW, this);
        rm.removeRefreshListener(RefreshManager.PANEL_FINES, this);
        rm.removeRefreshListener(RefreshManager.PANEL_USERS, this);

        System.out.println("🧹 DashboardPanel cleanup complete");
    }

    /**
     * RoundedPanel - draws a rounded panel with hover effect.
     * Uses the provided color as the accent/border color.
     */
    private static class RoundedPanel extends JPanel {
        private final Color accent;
        private boolean hovered = false;
        private final Color baseFill = new Color(255, 255, 255, 230);
        private final int arc = 18;

        RoundedPanel(Color accent) {
            this.accent = accent;
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // fill (slightly brighter when hovered)
                Color fill = hovered ? new Color(255, 255, 255, 255) : baseFill;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                // border
                float strokeWidth = hovered ? 3f : 2f;
                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Color borderColor = hovered ? accent.brighter() : accent;
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}