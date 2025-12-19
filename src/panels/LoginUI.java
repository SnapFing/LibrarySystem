package panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    public LoginUI() {
        setTitle("📚 Library Login");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // ===== Set Custom Icon =====
        ImageIcon icon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
        setIconImage(icon.getImage());

        // ===== Background Panel =====
        BackgroundPanel background = new BackgroundPanel("/panels/lib1.jpg");
        background.setLayout(new BorderLayout());
        setContentPane(background);

        // ===== Logo Panel =====
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 30));

        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
        Image scaledLogo = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoPanel.add(logoLabel);

        background.add(logoPanel, BorderLayout.NORTH);

        // ===== Login Form Panel =====
        RoundedPanel formPanel = new RoundedPanel(20); // radius = 20
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 180)); // semi-transparent black
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Library Management System", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        // Username
        JLabel userLabel = new JLabel("👤 Username:");
        userLabel.setForeground(Color.WHITE);
        usernameField = new JTextField(15);
        usernameField.setOpaque(false);
        usernameField.setForeground(Color.WHITE);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        // Password
        JLabel passLabel = new JLabel("🔑 Password:");
        passLabel.setForeground(Color.WHITE);
        passwordField = new JPasswordField(15);
        passwordField.setOpaque(false);
        passwordField.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setOpaque(false);
        loginButton.setForeground(Color.WHITE);
        loginButton.setBorder(BorderFactory.createEmptyBorder());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        // Status Label
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 4;
        formPanel.add(statusLabel, gbc);

        // Center the form panel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 50));
        centerPanel.setOpaque(false);
        centerPanel.add(formPanel);
        background.add(centerPanel, BorderLayout.CENTER);

        // ===== Actions =====
        loginButton.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin); // Press Enter to login

        setVisible(true);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username & password.");
            return;
        }

        login(username, password);
    }

    private void login(String username, String password) {
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                JOptionPane.showMessageDialog(this, "Login successful! Role: " + role);

                SwingUtilities.invokeLater(() -> new LibrarySystemUI(role));
                dispose();
            } else {
                statusLabel.setText("Invalid username or password.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Error connecting to database.");
        }
    }

    // ===== Background Panel Class =====
    static class BackgroundPanel extends JPanel {
        private final Image backgroundImage;

        public BackgroundPanel(String imagePath) {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            backgroundImage = icon.getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ===== Rounded Panel Class =====
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

            // Background with transparency
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarculaLaf()); }
        catch (Exception ex) { System.err.println("Failed to initialize LookAndFeel"); }
        SwingUtilities.invokeLater(LoginUI::new);
    }
}
