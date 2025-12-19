package panels;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.*;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class LibrarySystemUI extends JFrame {

    private JTabbedPane tabbedPane;
    private final String userRole;
    private JLabel dateTimeLabel;

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

        setTitle("Library Management System — Logged in as " + role);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
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

        startClock();
        setVisible(true);
    }

    /* ================= TOP BAR ================= */
    private JPanel createTopBar() {
        RoundedPanel topBar = new RoundedPanel(60);
        topBar.setLayout(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        topBar.setBackground(new Color(30, 30, 30));

        /* LEFT — LOGO + WELCOME */
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = raw.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {}

        JLabel welcome = new JLabel("Welcome, " + userRole);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 15));
        welcome.setForeground(Color.WHITE);

        left.add(logo);
        left.add(welcome);

        /* RIGHT — THEME, LOGOUT, TIME */
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row1.setOpaque(false);

        JComboBox<String> themeBox =
                new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        themeBox.setSelectedItem("Flat Darcula");
        themeBox.addActionListener(e ->
                switchTheme((String) themeBox.getSelectedItem()));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());

        row1.add(new JLabel("🎨 Theme"));
        row1.add(themeBox);
        row1.add(logoutBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row2.setOpaque(false);
        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateTimeLabel.setForeground(Color.WHITE);
        row2.add(dateTimeLabel);

        right.add(row1);
        right.add(Box.createVerticalStrut(6));
        right.add(row2);

        topBar.add(left, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        return topBar;
    }

    /* ================= TABS ================= */
    private JTabbedPane createTabs() {
        tabbedPane = new JTabbedPane();

        // Core Tabs
        tabbedPane.addTab("👥 Members", new MembersPanel());
        tabbedPane.addTab("📚 Borrow / Return", new BorrowReturnPanel());
        tabbedPane.addTab("💰 Fines", new FinesPanel());

        // Admin Only Tabs
        if ("Admin".equalsIgnoreCase(userRole)) {
            tabbedPane.addTab("📖 Books", new BooksPanel());
            tabbedPane.addTab("👤 Users", new UserManagementPanel());
        }

        return tabbedPane;
    }

    /* ================= HELPERS ================= */
    private void switchTheme(String themeName) {
        try {
            UIManager.setLookAndFeel(THEMES.get(themeName));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void logout() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }

    private void startClock() {
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE, dd MMM yyyy | HH:mm:ss");
        Timer timer = new Timer(1000,
                e -> dateTimeLabel.setText(sdf.format(new Date())));
        timer.start();
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
        SwingUtilities.invokeLater(() ->
                new LibrarySystemUI("Admin"));
    }
}
