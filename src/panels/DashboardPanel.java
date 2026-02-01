package panels;

import db.DBHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;

/**
 * Enhanced Dashboard Panel with Modern UI/UX
 * Features:
 * - Animated stat cards with hover effects
 * - Real-time data updates
 * - Color-coded statistics
 * - Smooth animations
 * - Responsive design
 */
public class DashboardPanel extends JPanel {

    private JLabel totalBooksLabel, availableBooksLabel, borrowedBooksLabel;
    private JLabel totalMembersLabel, activeMembersLabel;
    private JLabel borrowedTodayLabel, overdueLabel, totalFinesLabel;
    private JTextArea recentActivityArea, topBooksArea, upcomingDuesArea;
    private Timer autoRefreshTimer;
    private JCheckBox autoRefreshCheckbox;

    // Stat card panels for animation
    private StatCard[] statCards;

    public DashboardPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(25, 25, 25, 25));
        setBackground(new Color(240, 242, 245));

        initializeUI();
        refreshDashboard();
        startAutoRefresh();
    }

    private void initializeUI() {
        // === HEADER SECTION ===
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // === MAIN CONTENT ===
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setOpaque(false);

        // Stats Cards Grid
        JPanel statsPanel = createModernStatsPanel();
        mainPanel.add(statsPanel, BorderLayout.NORTH);

        // Activity Panels
        JPanel activityPanel = createActivityPanel();
        mainPanel.add(activityPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    // ===== HEADER PANEL =====
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 15));
        header.setOpaque(false);

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("📊");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JLabel titleLabel = new JLabel("Library Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(33, 37, 41));

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);

        autoRefreshCheckbox = new JCheckBox("Auto-refresh");
        autoRefreshCheckbox.setSelected(true);
        autoRefreshCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        autoRefreshCheckbox.setOpaque(false);
        autoRefreshCheckbox.addActionListener(e -> {
            if (autoRefreshCheckbox.isSelected()) {
                startAutoRefresh();
            } else {
                stopAutoRefresh();
            }
        });

        JButton refreshBtn = createModernButton("🔄 Refresh", new Color(0, 123, 255));
        refreshBtn.addActionListener(e -> {
            refreshDashboard();
            animateRefresh();
        });

        JButton settingsBtn = createModernButton("⚙️ Settings", new Color(108, 117, 125));
        settingsBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Dashboard settings coming soon!"));

        controlPanel.add(autoRefreshCheckbox);
        controlPanel.add(refreshBtn);
        controlPanel.add(settingsBtn);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(controlPanel, BorderLayout.EAST);

        return header;
    }

    // ===== MODERN STATS PANEL =====
    private JPanel createModernStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 20, 20));
        panel.setOpaque(false);

        statCards = new StatCard[8];

        // Row 1: Books Statistics
        statCards[0] = createModernStatCard("📚", "Total Books", "0",
                new Color(52, 152, 219), new Color(41, 128, 185));
        statCards[1] = createModernStatCard("✅", "Available", "0",
                new Color(46, 204, 113), new Color(39, 174, 96));
        statCards[2] = createModernStatCard("📖", "Borrowed", "0",
                new Color(241, 196, 15), new Color(243, 156, 18));
        statCards[3] = createModernStatCard("⚠️", "Overdue", "0",
                new Color(231, 76, 60), new Color(192, 57, 43));

        // Row 2: Members & Activity Statistics
        statCards[4] = createModernStatCard("👥", "Total Members", "0",
                new Color(155, 89, 182), new Color(142, 68, 173));
        statCards[5] = createModernStatCard("🟢", "Active Members", "0",
                new Color(26, 188, 156), new Color(22, 160, 133));
        statCards[6] = createModernStatCard("📅", "Today's Borrows", "0",
                new Color(52, 73, 94), new Color(44, 62, 80));
        statCards[7] = createModernStatCard("💰", "Total Fines", "K 0.00",
                new Color(230, 126, 34), new Color(211, 84, 0));

        for (StatCard card : statCards) {
            panel.add(card);
        }

        return panel;
    }

    // ===== MODERN STAT CARD =====
    private StatCard createModernStatCard(String icon, String title, String value,
                                          Color primaryColor, Color hoverColor) {
        StatCard card = new StatCard(icon, title, value, primaryColor, hoverColor);

        // Store reference for updates
        if (title.contains("Total Books")) totalBooksLabel = card.valueLabel;
        else if (title.contains("Available")) availableBooksLabel = card.valueLabel;
        else if (title.contains("Borrowed") && !title.contains("Today")) borrowedBooksLabel = card.valueLabel;
        else if (title.contains("Overdue")) overdueLabel = card.valueLabel;
        else if (title.contains("Total Members")) totalMembersLabel = card.valueLabel;
        else if (title.contains("Active")) activeMembersLabel = card.valueLabel;
        else if (title.contains("Today")) borrowedTodayLabel = card.valueLabel;
        else if (title.contains("Fines")) totalFinesLabel = card.valueLabel;

        return card;
    }

    // ===== STAT CARD CLASS =====
    private static class StatCard extends JPanel {
        private final Color primaryColor;
        private final Color hoverColor;
        private Color currentColor;
        private boolean isHovered = false;
        private final JLabel iconLabel;
        private final JLabel titleLabel;
        private final JLabel valueLabel;
        private float elevation = 0f;
        private Timer animationTimer;

        public StatCard(String icon, String title, String value, Color primaryColor, Color hoverColor) {
            this.primaryColor = primaryColor;
            this.hoverColor = hoverColor;
            this.currentColor = primaryColor;

            setLayout(new BorderLayout(10, 10));
            setOpaque(false);
            setBorder(new EmptyBorder(20, 20, 20, 20));
            setPreferredSize(new Dimension(200, 130));

            // Icon at top
            iconLabel = new JLabel(icon, SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));

            // Title
            titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            titleLabel.setForeground(new Color(108, 117, 125));

            // Value
            valueLabel = new JLabel(value, SwingConstants.CENTER);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
            valueLabel.setForeground(primaryColor);

            // Layout
            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            centerPanel.setOpaque(false);

            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            centerPanel.add(iconLabel);
            centerPanel.add(Box.createVerticalStrut(5));
            centerPanel.add(valueLabel);
            centerPanel.add(Box.createVerticalStrut(5));
            centerPanel.add(titleLabel);

            add(centerPanel, BorderLayout.CENTER);

            // Mouse listeners for hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    animateElevation(true);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    animateElevation(false);
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        }

        private void animateElevation(boolean up) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }

            final float targetElevation = up ? 8f : 0f;
            final float step = up ? 0.5f : -0.5f;

            animationTimer = new Timer(10, e -> {
                elevation += step;
                if ((up && elevation >= targetElevation) || (!up && elevation <= targetElevation)) {
                    elevation = targetElevation;
                    ((Timer) e.getSource()).stop();
                }
                currentColor = isHovered ? hoverColor : primaryColor;
                valueLabel.setForeground(currentColor);
                repaint();
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Shadow
            if (elevation > 0) {
                g2.setColor(new Color(0, 0, 0, (int)(30 + elevation * 2)));
                g2.fillRoundRect(
                        (int)elevation, (int)elevation,
                        getWidth() - (int)(elevation * 2),
                        getHeight() - (int)(elevation * 2),
                        20, 20
                );
            }

            // Background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            // Top accent bar
            g2.setColor(currentColor);
            g2.fillRoundRect(0, 0, getWidth(), 6, 20, 20);

            // Border
            g2.setColor(new Color(233, 236, 239));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== ACTIVITY PANELS =====
    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 20));
        panel.setOpaque(false);

        panel.add(createInfoCard("📋 Recent Activity", createRecentActivityArea(),
                new Color(52, 152, 219)));
        panel.add(createInfoCard("⭐ Top Borrowed Books", createTopBooksArea(),
                new Color(46, 204, 113)));
        panel.add(createInfoCard("📅 Upcoming Due Dates", createUpcomingDuesArea(),
                new Color(230, 126, 34)));

        return panel;
    }

    private JPanel createInfoCard(String title, JTextArea textArea, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Border
                g2.setColor(new Color(233, 236, 239));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(accentColor);
        header.add(titleLabel);

        // Text area
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(new Color(248, 249, 250));
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(300, 280));

        card.add(header, BorderLayout.NORTH);
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

    // ===== MODERN BUTTON =====
    private JButton createModernButton(String text, Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(120, 35));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }

    // ===== DATA LOADING =====
    private void refreshDashboard() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int totalBooks, availableBooks, borrowedBooks, overdueBooks;
            int totalMembers, activeMembers, borrowedToday;
            double totalFines;
            String recentActivity, topBooks, upcomingDues;

            @Override
            protected Void doInBackground() {
                try (Connection conn = DBHelper.getConnection()) {
                    loadBookStats(conn);
                    loadMemberStats(conn);
                    loadFineStats(conn);
                    loadRecentActivity(conn);
                    loadTopBooks(conn);
                    loadUpcomingDues(conn);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    recentActivity = "Error loading data";
                    topBooks = "Error loading data";
                    upcomingDues = "Error loading data";
                }
                return null;
            }

            private void loadBookStats(Connection conn) throws SQLException {
                String sql = "SELECT " +
                        "COUNT(*) as total, " +
                        "SUM(available_quantity) as available, " +
                        "SUM(total_quantity - available_quantity) as borrowed " +
                        "FROM books";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    totalBooks = rs.getInt("total");
                    availableBooks = rs.getInt("available");
                    borrowedBooks = rs.getInt("borrowed");
                }

                // Count overdue
                sql = "SELECT COUNT(*) as overdue FROM borrowed_books " +
                        "WHERE status='BORROWED' AND due_date < CURRENT_DATE";
                rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    overdueBooks = rs.getInt("overdue");
                }
            }

            private void loadMemberStats(Connection conn) throws SQLException {
                String sql = "SELECT COUNT(*) as total FROM members";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    totalMembers = rs.getInt("total");
                }

                sql = "SELECT COUNT(DISTINCT member_id) as active FROM borrowed_books " +
                        "WHERE status='BORROWED'";
                rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    activeMembers = rs.getInt("active");
                }

                sql = "SELECT COUNT(*) as today FROM borrowed_books " +
                        "WHERE DATE(borrow_date) = CURRENT_DATE";
                rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    borrowedToday = rs.getInt("today");
                }
            }

            private void loadFineStats(Connection conn) throws SQLException {
                String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM fines WHERE paid = FALSE";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                if (rs.next()) {
                    totalFines = rs.getDouble("total");
                }
            }

            private void loadRecentActivity(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT action, username, DATE_FORMAT(action_time, '%H:%i') as time " +
                        "FROM activity_log ORDER BY action_time DESC LIMIT 10";
                ResultSet rs = conn.createStatement().executeQuery(sql);

                while (rs.next()) {
                    sb.append("• ").append(rs.getString("time")).append(" - ")
                            .append(rs.getString("action")).append(" (")
                            .append(rs.getString("username")).append(")\n");
                }

                recentActivity = sb.length() > 0 ? sb.toString() : "No recent activity";
            }

            private void loadTopBooks(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT b.title, COUNT(*) as count " +
                        "FROM borrowed_books bb " +
                        "JOIN books b ON bb.book_id = b.id " +
                        "GROUP BY b.id, b.title " +
                        "ORDER BY count DESC LIMIT 5";
                ResultSet rs = conn.createStatement().executeQuery(sql);

                int rank = 1;
                while (rs.next()) {
                    String title = rs.getString("title");
                    if (title.length() > 35) title = title.substring(0, 32) + "...";
                    sb.append(rank++).append(". ").append(title)
                            .append(" (").append(rs.getInt("count")).append(" times)\n");
                }

                topBooks = sb.length() > 0 ? sb.toString() : "No borrowing data";
            }

            private void loadUpcomingDues(Connection conn) throws SQLException {
                StringBuilder sb = new StringBuilder();
                String sql = "SELECT CONCAT(m.fname, ' ', m.lname) as member, " +
                        "b.title, bb.due_date " +
                        "FROM borrowed_books bb " +
                        "JOIN members m ON bb.member_id = m.id " +
                        "JOIN books b ON bb.book_id = b.id " +
                        "WHERE bb.status='BORROWED' AND bb.due_date BETWEEN CURRENT_DATE " +
                        "AND DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) " +
                        "ORDER BY bb.due_date ASC LIMIT 8";
                ResultSet rs = conn.createStatement().executeQuery(sql);

                while (rs.next()) {
                    String member = rs.getString("member");
                    String title = rs.getString("title");
                    if (title.length() > 30) title = title.substring(0, 27) + "...";

                    sb.append("• ").append(member).append("\n  ")
                            .append(title).append("\n  Due: ")
                            .append(rs.getDate("due_date")).append("\n\n");
                }

                upcomingDues = sb.length() > 0 ? sb.toString() : "No books due soon";
            }

            @Override
            protected void done() {
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
            }
        };

        worker.execute();
    }

    // ===== ANIMATION =====
    private void animateRefresh() {
        for (StatCard card : statCards) {
            card.animateElevation(true);
            Timer timer = new Timer(300, e -> card.animateElevation(false));
            timer.setRepeats(false);
            timer.start();
        }
    }

    // ===== AUTO-REFRESH =====
    private void startAutoRefresh() {
        if (autoRefreshTimer == null) {
            autoRefreshTimer = new Timer(30000, e -> refreshDashboard());
        }
        if (!autoRefreshTimer.isRunning()) {
            autoRefreshTimer.start();
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        stopAutoRefresh();
    }
}