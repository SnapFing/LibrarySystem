package com.librarysystem;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.*;
import com.librarysystem.core.Session;
import com.librarysystem.db.DBHelper;
import com.librarysystem.panels.*;
import com.librarysystem.panels.students.*;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LibrarySystemUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JLabel dateTimeLabel;

    // Theme definitions
    private static final Map<String, LookAndFeel> THEMES = new LinkedHashMap<>();
    static {
        THEMES.put("Flat Light", new FlatLightLaf());
        THEMES.put("Flat Dark", new FlatDarkLaf());
        THEMES.put("Flat IntelliJ", new FlatIntelliJLaf());
        THEMES.put("Flat Darcula", new FlatDarculaLaf());
        THEMES.put("Arc Orange", new FlatArcOrangeIJTheme());
        THEMES.put("Carbon", new FlatCarbonIJTheme());
        THEMES.put("Cobalt 2", new FlatCobalt2IJTheme());
        THEMES.put("Dracula", new FlatDraculaIJTheme());
        THEMES.put("Gradianto Deep Ocean", new FlatGradiantoDeepOceanIJTheme());
        THEMES.put("Gradianto Midnight Blue", new FlatGradiantoMidnightBlueIJTheme());
        THEMES.put("Gradianto Nature Green", new FlatGradiantoNatureGreenIJTheme());
        THEMES.put("High Contrast", new FlatHighContrastIJTheme());
        THEMES.put("Monokai Pro", new FlatMonokaiProIJTheme());
        THEMES.put("One Dark", new FlatOneDarkIJTheme());
        THEMES.put("Solarized Dark", new FlatSolarizedDarkIJTheme());
        THEMES.put("Solarized Light", new FlatSolarizedLightIJTheme());
    }

    public LibrarySystemUI() {
        Session session = Session.getInstance();
        if (!session.isLoggedIn()) {
            dispose();
            SwingUtilities.invokeLater(LoginUI::new);
            return;
        }

        String role = session.getRole();
        String fullName = session.getFullName();

        setTitle("Library Management - " + fullName + " (" + role + ")");
        setSize(1200, 700);   // slightly larger for modern look
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // App icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        // ── Top Bar ─────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 30, 40));   // dark header for any theme
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Left: logo + welcome
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image logoImg = logoIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(logoImg));
            leftPanel.add(logoLabel);
        } catch (Exception ignored) {}

        JLabel welcomeLbl = new JLabel("Welcome, " + fullName + " (" + role + ")");
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        welcomeLbl.setForeground(Color.WHITE);
        leftPanel.add(welcomeLbl);
        topBar.add(leftPanel, BorderLayout.WEST);

        // Right: theme selector + logout + clock
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row1.setOpaque(false);

        JComboBox<String> themeSelector = new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        // Load saved theme
        String savedTheme = getSavedTheme();
        themeSelector.setSelectedItem(savedTheme != null ? savedTheme : "Flat Darcula");
        themeSelector.addActionListener(e -> {
            String themeName = (String) themeSelector.getSelectedItem();
            if (themeName != null) {
                switchTheme(themeName);
                saveThemeSetting(themeName);
            }
        });

        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.addActionListener(e -> {
            Session.getInstance().logout();
            dispose();
            SwingUtilities.invokeLater(LoginUI::new);
        });

        row1.add(new JLabel("🎨 Theme:"));
        row1.add(themeSelector);
        row1.add(logoutBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row2.setOpaque(false);
        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateTimeLabel.setForeground(Color.LIGHT_GRAY);
        row2.add(dateTimeLabel);

        rightPanel.add(row1);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(row2);
        topBar.add(rightPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ── Tabs according to role ──────────────────────────────────────────
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        switch (role) {
            case "Student":
                tabbedPane.addTab("📚 Browse Books", new StudentBooksPanel());
                tabbedPane.addTab("📖 My Books & Requests", new MyBorrowedBooksPanel());
                tabbedPane.addTab("👤 My Profile", new StudentProfilePanel());
                break;

            case "Librarian":
                tabbedPane.addTab("👥 Members", new MembersPanel());
                tabbedPane.addTab("📚 Books", new BooksPanel());
                tabbedPane.addTab("🔄 Borrow/Return", new BorrowReturnPanel());
                tabbedPane.addTab("📋 Borrow Requests", new BorrowRequestsPanel());
                tabbedPane.addTab("📤 Return Requests", new ReturnRequestsPanel());
                tabbedPane.addTab("📊 Reports", new ReportsPanel());
                break;

            case "Admin":
                tabbedPane.addTab("📊 Dashboard", new DashboardPanel());
                tabbedPane.addTab("👥 Members", new MembersPanel());
                tabbedPane.addTab("📚 Books", new BooksPanel());
                tabbedPane.addTab("🔄 Borrow/Return", new BorrowReturnPanel());
                tabbedPane.addTab("📋 Borrow Requests", new BorrowRequestsPanel());
                tabbedPane.addTab("📤 Return Requests", new ReturnRequestsPanel());
                tabbedPane.addTab("👨‍💼 User Management", new UserManagementPanel());
                tabbedPane.addTab("💰 Fines", new FinesPanel());
                tabbedPane.addTab("📊 Reports", new ReportsPanel());
                tabbedPane.addTab("⚙️ System Settings", new SystemSettingsPanel());
                tabbedPane.addTab("📡 Database Monitor", new DatabaseMonitorPanel());
                break;

            default:
                JOptionPane.showMessageDialog(this, "Unknown role: " + role);
                System.exit(1);
        }

        add(tabbedPane, BorderLayout.CENTER);

        // Start clock
        new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy  HH:mm:ss");
            dateTimeLabel.setText(sdf.format(new Date()));
        }).start();

        setVisible(true);
    }

    // ── Theme helpers ─────────────────────────────────────────────────────────
    private String getSavedTheme() {
        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT setting_value FROM system_settings WHERE setting_key = 'ui_theme'");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("setting_value");
        } catch (Exception ignored) {}
        return null;
    }

    private void saveThemeSetting(String themeName) {
        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO system_settings (setting_key, setting_value) VALUES ('ui_theme', ?) " +
                            "ON DUPLICATE KEY UPDATE setting_value = ?");
            stmt.setString(1, themeName);
            stmt.setString(2, themeName);
            stmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void switchTheme(String themeName) {
        try {
            UIManager.setLookAndFeel(THEMES.get(themeName));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Start with the login UI – it handles setup wizard, DB check, etc.
        SwingUtilities.invokeLater(LoginUI::new);
    }
}