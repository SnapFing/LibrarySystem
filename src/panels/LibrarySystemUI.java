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

    public LibrarySystemUI(String role) {
        this.userRole = role;

        setTitle("Library Management - Logged in as " + role);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       // setResizable(false);

        // ===== Set Custom Icon =====
        ImageIcon icon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
        setIconImage(icon.getImage());

        // ===== Top Bar =====
        RoundedPanel topBar = new RoundedPanel(80);
       // topBar.setBackground(new Color(0, 0, 0, 180)); // semi-transparent
        topBar.setLayout(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // LEFT: Logo + Welcome
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) { }

        JLabel welcomeLabel = new JLabel("Welcome, " + role);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        welcomeLabel.setForeground(Color.WHITE);

        leftPanel.add(logo);
        leftPanel.add(welcomeLabel);
        topBar.add(leftPanel, BorderLayout.WEST);

        // RIGHT: Theme + Logout + DateTime
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        // Row 1: Theme + Logout
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row1.setOpaque(false);
        JComboBox<String> themeSelector = new JComboBox<>(THEMES.keySet().toArray(new String[0]));
        themeSelector.setSelectedItem("Flat Darcula");
        themeSelector.addActionListener(e -> switchTheme((String) themeSelector.getSelectedItem()));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
        });

        row1.add(new JLabel("🎨 Theme:"));
        row1.add(themeSelector);
        row1.add(logoutBtn);

        // Row 2: Date/Time
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row2.setOpaque(false);
        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateTimeLabel.setForeground(Color.WHITE);
        row2.add(dateTimeLabel);

        rightPanel.add(row1);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(row2);

        topBar.add(rightPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ===== Tabs =====
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Members", new MembersPanel());
        tabbedPane.addTab("Borrow/Return", new BorrowReturnPanel());
        if ("Admin".equalsIgnoreCase(userRole)) {
            tabbedPane.addTab("Books", new BooksPanel());
            tabbedPane.addTab("User Management", new UserManagementPanel());
        }
        add(tabbedPane, BorderLayout.CENTER);

        // Start clock
        startClock();

        setVisible(true);
    }

    private void switchTheme(String themeName) {
        try {
            UIManager.setLookAndFeel(THEMES.get(themeName));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy | HH:mm:ss");
            dateTimeLabel.setText(sdf.format(new Date()));
        });
        timer.start();
    }

    // ===== Rounded Panel =====
    static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        public RoundedPanel(int radius) {
            super();
            this.cornerRadius = radius;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(new FlatDarculaLaf()); 
        } catch (Exception ex) { 
            System.err.println("Failed to initialize LookAndFeel"); 
        }
        SwingUtilities.invokeLater(() -> new LibrarySystemUI("Admin"));
    }
}
