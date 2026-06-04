package com.librarysystem.panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.librarysystem.core.Session;
import com.librarysystem.db.AuthenticationService;
import com.librarysystem.db.DBHelper;
import com.librarysystem.utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class LoginUI extends JFrame {
    private JTextField loginField;     // username or email
    private JPasswordField passwordField;
    private JButton loginButton, signupButton;
    private JLabel statusLabel;

    public LoginUI() {
        // Check if setup is needed
        if (!DBHelper.isAvailable()) {
            showSetupWizard();
            return; // will exit after wizard
        }

        setTitle("📚 Library Management System - Login");
        setSize(700, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        BackgroundPanel background = new BackgroundPanel("/panels/lib1.jpg");
        background.setLayout(new BorderLayout());
        setContentPane(background);

        // Logo
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 30));
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaledLogo = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            logoPanel.add(new JLabel(new ImageIcon(scaledLogo)));
        } catch (Exception ignored) {}
        background.add(logoPanel, BorderLayout.NORTH);

        // Form panel
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Library Management System", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Sign in to continue", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        formPanel.add(subtitleLabel, gbc);

        // Login identifier (username or email)
        JLabel userLabel = new JLabel("👤 Username or Email:");
        userLabel.setForeground(Color.WHITE);
        loginField = new JTextField(15);
        loginField.setOpaque(false);
        loginField.setForeground(Color.WHITE);

        gbc.gridwidth = 1; gbc.gridy = 2; gbc.gridx = 0;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(loginField, gbc);

        JLabel passLabel = new JLabel("🔑 Password:");
        passLabel.setForeground(Color.WHITE);
        passwordField = new JPasswordField(15);
        passwordField.setOpaque(false);
        passwordField.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        loginButton = new JButton("🔓 Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        formPanel.add(statusLabel, gbc);

        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        gbc.gridy = 6;
        formPanel.add(separator, gbc);

        JLabel newUserLabel = new JLabel("Don't have an account?", JLabel.CENTER);
        newUserLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newUserLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 7;
        formPanel.add(newUserLabel, gbc);

        signupButton = new JButton("📝 Sign Up as Student");
        signupButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 8;
        formPanel.add(signupButton, gbc);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        centerPanel.setOpaque(false);
        centerPanel.add(formPanel);
        background.add(centerPanel, BorderLayout.CENTER);

        loginButton.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin);
        signupButton.addActionListener(this::handleSignup);

        setVisible(true);
    }

    private void showSetupWizard() {
        SetupWizard wizard = new SetupWizard(this);
        wizard.setVisible(true);
    }

    private void handleLogin(ActionEvent e) {
        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username/email and password.");
            return;
        }

        try {
            Session session = AuthenticationService.login(login, password);
            // Audit log (simplified)
            try (var conn = DBHelper.getConnection()) {
                String action = "Login";
                String details = "User '" + login + "' logged in as " + session.getRole();
                conn.prepareStatement(
                        "INSERT INTO audit_logs (user_id, username, action, table_name, record_id, details) "
                                + "VALUES (?, ?, ?, NULL, NULL, ?)"
                ).executeUpdate();
            } catch (Exception ignored) {}

            JOptionPane.showMessageDialog(this,
                    "Welcome, " + session.getFullName() + "!\nRole: " + session.getRole(),
                    "Login Successful", JOptionPane.INFORMATION_MESSAGE);
            SwingUtilities.invokeLater(() -> new com.librarysystem.LibrarySystemUI(session.getRole(), session.getUsername()));
            dispose();

        } catch (AuthenticationService.AuthenticationException ex) {
            statusLabel.setText(ex.getMessage());
        } catch (SQLException ex) {
            statusLabel.setText("Database error. Please try again.");
            ex.printStackTrace();
        }
    }

    private void handleSignup(ActionEvent e) {
        SignupDialog signupDialog = new SignupDialog(this);
        signupDialog.setVisible(true);
    }

    // ===== Signup Dialog (adapted for new schema) =====
    class SignupDialog extends JDialog {
        private JTextField fnameField, mnameField, lnameField, emailField, phoneField;
        private JPasswordField passwordField, confirmPasswordField;
        private JButton registerButton, cancelButton;

        public SignupDialog(JFrame parent) {
            super(parent, "Student Registration", true);
            setSize(450, 550);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel titleLabel = new JLabel("📝 Student Registration", JLabel.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            formPanel.add(titleLabel, gbc);
            gbc.gridwidth = 1;

            // First name
            gbc.gridx = 0; gbc.gridy = 1;
            formPanel.add(new JLabel("First Name:"), gbc);
            gbc.gridx = 1;
            fnameField = new JTextField(20);
            formPanel.add(fnameField, gbc);

            // Middle name (optional)
            gbc.gridx = 0; gbc.gridy = 2;
            formPanel.add(new JLabel("Middle Name:"), gbc);
            gbc.gridx = 1;
            mnameField = new JTextField(20);
            formPanel.add(mnameField, gbc);

            // Last name
            gbc.gridx = 0; gbc.gridy = 3;
            formPanel.add(new JLabel("Last Name:"), gbc);
            gbc.gridx = 1;
            lnameField = new JTextField(20);
            formPanel.add(lnameField, gbc);

            // Email
            gbc.gridx = 0; gbc.gridy = 4;
            formPanel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            emailField = new JTextField(20);
            formPanel.add(emailField, gbc);

            // Phone
            gbc.gridx = 0; gbc.gridy = 5;
            formPanel.add(new JLabel("Phone:"), gbc);
            gbc.gridx = 1;
            phoneField = new JTextField(20);
            formPanel.add(phoneField, gbc);

            // Password
            gbc.gridx = 0; gbc.gridy = 6;
            formPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            formPanel.add(passwordField, gbc);

            // Confirm password
            gbc.gridx = 0; gbc.gridy = 7;
            formPanel.add(new JLabel("Confirm Password:"), gbc);
            gbc.gridx = 1;
            confirmPasswordField = new JPasswordField(20);
            formPanel.add(confirmPasswordField, gbc);

            JLabel reqLabel = new JLabel("<html><small>Password must have: 8+ chars, letter, digit, special char</small></html>");
            reqLabel.setForeground(Color.GRAY);
            gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
            formPanel.add(reqLabel, gbc);

            add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            registerButton = new JButton("✅ Register");
            cancelButton = new JButton("❌ Cancel");
            registerButton.addActionListener(ev -> handleRegister());
            cancelButton.addActionListener(ev -> dispose());
            buttonPanel.add(registerButton);
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }

        private void handleRegister() {
            String fname = fnameField.getText().trim();
            String mname = mnameField.getText().trim();
            String lname = lnameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

            if (fname.isEmpty() || lname.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Please fill all required fields!");
                return;
            }
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
                JOptionPane.showMessageDialog(this, "❌ Invalid email format!");
                return;
            }
            if (!phone.matches("\\d{10}")) {
                JOptionPane.showMessageDialog(this, "❌ Phone must be 10 digits!");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "❌ Passwords do not match!");
                return;
            }
            String strengthError = PasswordUtil.validatePasswordStrength(password);
            if (strengthError != null) {
                JOptionPane.showMessageDialog(this, "❌ " + strengthError);
                return;
            }

            try {
                AuthenticationService.registerMember(fname, mname.isEmpty() ? null : mname, lname, email, phone, password);
                JOptionPane.showMessageDialog(this,
                        "✅ Registration successful!\nYou can now log in using your email and password.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062 || ex.getMessage().contains("Duplicate")) {
                    JOptionPane.showMessageDialog(this, "❌ This email is already registered.");
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Registration error: " + ex.getMessage());
                }
            }
        }
    }

    // ===== Background Panel (unchanged) =====
    static class BackgroundPanel extends JPanel {
        private Image backgroundImage;
        public BackgroundPanel(String imagePath) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
                backgroundImage = icon.getImage();
            } catch (Exception e) {
                backgroundImage = null;
            }
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    // ===== Rounded Panel (unchanged) =====
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
        try { UIManager.setLookAndFeel(new FlatDarculaLaf()); }
        catch (Exception ex) { System.err.println("Failed to initialize LookAndFeel"); }
        SwingUtilities.invokeLater(LoginUI::new);
    }
}