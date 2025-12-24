package panels;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.*;
import db.DBHelper;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class LibrarySystemUI extends JFrame {

    private JTabbedPane tabbedPane;
    private final String userRole;
    private JLabel dateTimeLabel;
    private static int currentUserId;
    private static String currentUsername;

    /* ================= THEME REGISTRY ================= */
    private static final Map<String, LookAndFeel> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("Flat Light", new FlatLightLaf());
        THEMES.put("Flat Dark", new FlatDarkLaf());
        THEMES.put("Flat IntelliJ", new FlatIntelliJLaf());
        THEMES.put("Flat Darcula", new FlatDarculaLaf());
        THEMES.put("Arc Orange", new FlatArcOrangeIJTheme());
        THEMES.put("Carbon", new FlatCarbonIJTheme());
        THEMES.put("Dracula", new FlatDraculaIJTheme());
        THEMES.put("Monokai Pro", new FlatMonokaiProIJTheme());
        THEMES.put("One Dark", new FlatOneDarkIJTheme());
        THEMES.put("Solarized Dark", new FlatSolarizedDarkIJTheme());
        THEMES.put("Solarized Light", new FlatSolarizedLightIJTheme());
    }

    /* ================= CONSTRUCTOR ================= */
    public LibrarySystemUI(String role) {
        this.userRole = role;

        setTitle("📚 Library Management System — Logged in as " + role);
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // App Icon
        try {
            setIconImage(new ImageIcon(
                    getClass().getResource("/panels/SNAPFING-LOGO.png")).getImage());
        } catch (Exception ignored) {}

        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        startClock();
        loadUserSession();
        setVisible(true);
    }

    /* ================= TOP BAR ================= */
    private JPanel createTopBar() {
        RoundedPanel topBar = new RoundedPanel(10);
        topBar.setLayout(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        topBar.setBackground(new Color(30, 30, 30));

        /* LEFT — LOGO + WELCOME */
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = raw.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {}

        JLabel welcome = new JLabel("Welcome, " + userRole);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcome.setForeground(Color.WHITE);

        left.add(logo);
        left.add(welcome);

        /* RIGHT — THEME, LOGOUT, TIME */
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row1.setOpaque(false);

        JLabel themeLabel = new JLabel("🎨");
        themeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JComboBox<String> themeBox = new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        themeBox.setSelectedItem("Flat Darcula");
        themeBox.setToolTipText("Select theme");
        themeBox.addActionListener(e -> switchTheme((String) themeBox.getSelectedItem()));

        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setToolTipText("Logout and return to login screen");
        logoutBtn.addActionListener(e -> logout());

        row1.add(themeLabel);
        row1.add(themeBox);
        row1.add(logoutBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
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

    /* ================= TABS ================= */
    private JTabbedPane createTabs() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Dashboard - First tab for quick overview
        tabbedPane.addTab("📊 Dashboard", null, new DashboardPanel(), "Library overview and statistics");

        // Core Tabs - Available to all users
        tabbedPane.addTab("👥 Members", null, new MembersPanel(), "Manage library members");
        tabbedPane.addTab("📚 Borrow / Return", null, new BorrowReturnPanel(), "Borrow and return books");
        tabbedPane.addTab("💰 Fines", null, new FinesPanel(), "Manage fines and payments");

        // Admin Only Tabs
        if ("Admin".equalsIgnoreCase(userRole)) {
            tabbedPane.addTab("📖 Books", null, new BooksPanel(), "Manage book inventory");
            tabbedPane.addTab("👤 Users", null, new UserManagementPanel(), "Manage system users");
            tabbedPane.addTab("📊 DB Monitor", null, new DatabaseMonitorPanel(), "Monitor database connection pool");
        }

        return tabbedPane;
    }

    /* ================= STATUS BAR ================= */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusBar.setBackground(new Color(40, 40, 40));

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 180, 180));

        JLabel versionLabel = new JLabel("Version 2.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(150, 150, 150));

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(versionLabel, BorderLayout.EAST);

        return statusBar;
    }

    /* ================= HELPERS ================= */
    private void switchTheme(String themeName) {
        try {
            UIManager.setLookAndFeel(THEMES.get(themeName));
            SwingUtilities.updateComponentTreeUI(this);

            // Log theme change
            logActivity("THEME_CHANGED", "Changed theme to: " + themeName);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to apply theme: " + themeName,
                    "Theme Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            logActivity("LOGOUT", "User logged out");
            updateLastLogin();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
        }
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy | HH:mm:ss");
        Timer timer = new Timer(1000, e -> dateTimeLabel.setText(sdf.format(new Date())));
        timer.start();
    }

    private void loadUserSession() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT id, username FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userRole.toLowerCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id");
                currentUsername = rs.getString("username");

                // Update last_login
                updateLastLogin();

                // Log login
                logActivity("LOGIN", "User logged in successfully");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateLastLogin() {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentUserId);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void logActivity(String action, String details) {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "INSERT INTO audit_logs (user_id, action, details) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentUserId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();
        } catch (Exception ex) {
            // Silently fail - logging shouldn't interrupt main operations
            ex.printStackTrace();
        }
    }

    /* ================= STATIC ACCESSORS ================= */
    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    /* ================= ROUNDED PANEL ================= */
    static class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(int r) {
            radius = r;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /* ================= MAIN ================= */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new LibrarySystemUI("Admin"));
    }
}