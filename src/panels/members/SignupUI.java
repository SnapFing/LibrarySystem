package panels.members;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;
import panels.LoginUI;
import utils.ValidationUtils;
import utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Member Self-Registration Form
 * Allows users to register as library members without admin intervention
 */
public class SignupUI extends JFrame {
    private JTextField fnameField, lnameField, emailField, phoneField, addressField;
    private JPasswordField passwordField, confirmPasswordField;
    private JButton signupButton, backToLoginButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public SignupUI() {
        setTitle("📚 Library Member Registration");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // ===== Set Custom Icon =====
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        // ===== Background Panel =====
        BackgroundPanel background = new BackgroundPanel("/panels/lib1.jpg");
        background.setLayout(new BorderLayout());
        setContentPane(background);

        // ===== Logo Panel =====
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 20));

        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaledLogo = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoPanel.add(logoLabel);
        } catch (Exception e) {
            // Logo not found, continue without it
        }

        background.add(logoPanel, BorderLayout.NORTH);

        // ===== Registration Form Panel =====
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 15, 8, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Create Member Account", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Join our library community", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        formPanel.add(subtitleLabel, gbc);

        gbc.gridwidth = 1;

        // First Name
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel fnameLabel = new JLabel("First Name *");
        fnameLabel.setForeground(Color.WHITE);
        formPanel.add(fnameLabel, gbc);

        gbc.gridx = 1;
        fnameField = new JTextField(18);
        fnameField.setToolTipText("Enter your first name (2-50 characters)");
        formPanel.add(fnameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lnameLabel = new JLabel("Last Name *");
        lnameLabel.setForeground(Color.WHITE);
        formPanel.add(lnameLabel, gbc);

        gbc.gridx = 1;
        lnameField = new JTextField(18);
        lnameField.setToolTipText("Enter your last name (2-50 characters)");
        formPanel.add(lnameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel emailLabel = new JLabel("Email *");
        emailLabel.setForeground(Color.WHITE);
        formPanel.add(emailLabel, gbc);

        gbc.gridx = 1;
        emailField = new JTextField(18);
        emailField.setToolTipText("Enter a valid email address");
        formPanel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel phoneLabel = new JLabel("Phone *");
        phoneLabel.setForeground(Color.WHITE);
        formPanel.add(phoneLabel, gbc);

        gbc.gridx = 1;
        phoneField = new JTextField(18);
        phoneField.setToolTipText("Zambian format: 0770000000 or +260977123456");
        formPanel.add(phoneField, gbc);

        // Address
        gbc.gridx = 0; gbc.gridy = 6;
        JLabel addressLabel = new JLabel("Address");
        addressLabel.setForeground(Color.WHITE);
        formPanel.add(addressLabel, gbc);

        gbc.gridx = 1;
        addressField = new JTextField(18);
        addressField.setToolTipText("Optional: Enter your address");
        formPanel.add(addressField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 7;
        JLabel passLabel = new JLabel("Password *");
        passLabel.setForeground(Color.WHITE);
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(18);
        passwordField.setToolTipText("Min 8 characters, must include letter, digit & special char");
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridx = 0; gbc.gridy = 8;
        JLabel confirmLabel = new JLabel("Confirm Password *");
        confirmLabel.setForeground(Color.WHITE);
        formPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(18);
        confirmPasswordField.setToolTipText("Re-enter your password");
        formPanel.add(confirmPasswordField, gbc);

        // Progress Bar
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        formPanel.add(progressBar, gbc);

        // Signup Button
        gbc.gridy = 10;
        signupButton = new JButton("Create Account");
        signupButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        signupButton.setBackground(new Color(40, 167, 69));
        signupButton.setForeground(Color.WHITE);
        signupButton.setFocusPainted(false);
        signupButton.addActionListener(this::handleSignup);
        formPanel.add(signupButton, gbc);

        // Back to Login Button
        gbc.gridy = 11;
        backToLoginButton = new JButton("Already have an account? Login");
        backToLoginButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backToLoginButton.setForeground(new Color(100, 181, 246));
        backToLoginButton.setBorderPainted(false);
        backToLoginButton.setContentAreaFilled(false);
        backToLoginButton.setFocusPainted(false);
        backToLoginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backToLoginButton.addActionListener(e -> backToLogin());
        formPanel.add(backToLoginButton, gbc);

        // Status Label
        gbc.gridy = 12;
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        formPanel.add(statusLabel, gbc);

        // Center the form panel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        centerPanel.setOpaque(false);
        centerPanel.add(formPanel);
        background.add(centerPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void handleSignup(ActionEvent e) {
        // Get all field values
        String fname = fnameField.getText().trim();
        String lname = lnameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // ===== VALIDATION =====

        // Validate first name
        ValidationUtils.ValidationResult fnameResult = ValidationUtils.validateName(fname, "First name");
        if (!fnameResult.isValid()) {
            showError(fnameResult.getErrorMessage());
            fnameField.requestFocus();
            return;
        }

        // Validate last name
        ValidationUtils.ValidationResult lnameResult = ValidationUtils.validateName(lname, "Last name");
        if (!lnameResult.isValid()) {
            showError(lnameResult.getErrorMessage());
            lnameField.requestFocus();
            return;
        }

        // Validate email
        ValidationUtils.ValidationResult emailResult = ValidationUtils.validateEmail(email);
        if (!emailResult.isValid()) {
            showError(emailResult.getErrorMessage());
            emailField.requestFocus();
            return;
        }

        // Validate phone
        ValidationUtils.ValidationResult phoneResult = ValidationUtils.validatePhone(phone);
        if (!phoneResult.isValid()) {
            showError(phoneResult.getErrorMessage());
            phoneField.requestFocus();
            return;
        }

        // Validate address (optional, but if provided must be valid)
        if (!address.isEmpty()) {
            ValidationUtils.ValidationResult addressResult = ValidationUtils.validateAddress(address);
            if (!addressResult.isValid()) {
                showError(addressResult.getErrorMessage());
                addressField.requestFocus();
                return;
            }
        }

        // Validate password strength
        String passwordError = PasswordUtil.validatePasswordStrength(password);
        if (passwordError != null) {
            showError(passwordError);
            passwordField.requestFocus();
            return;
        }

        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match!");
            confirmPasswordField.requestFocus();
            return;
        }

        // ===== ALL VALIDATIONS PASSED - PROCEED WITH REGISTRATION =====

        // Disable button and show progress
        signupButton.setEnabled(false);
        progressBar.setVisible(true);
        statusLabel.setText("Creating account...");
        statusLabel.setForeground(Color.YELLOW);

        // Perform registration in background thread
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBHelper.getConnection()) {
                    // Check if email already exists
                    String checkEmailSql = "SELECT COUNT(*) FROM members WHERE email = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql);
                    checkStmt.setString(1, email);
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        errorMessage = "Email address is already registered!";
                        return false;
                    }

                    // Hash the password
                    String hashedPassword = PasswordUtil.hashPassword(password);

                    // Insert member with password
                    String insertSql = "INSERT INTO members (fname, lname, email, phone, address, password) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(insertSql);
                    stmt.setString(1, fname);
                    stmt.setString(2, lname);
                    stmt.setString(3, email);
                    stmt.setString(4, phone);
                    stmt.setString(5, address.isEmpty() ? null : address);
                    stmt.setString(6, hashedPassword);
                    stmt.executeUpdate();

                    return true;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Database error: " + ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                signupButton.setEnabled(true);

                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(SignupUI.this,
                                "✅ Registration Successful!\n\n" +
                                        "Your account has been created.\n" +
                                        "You can now login using your email and password.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        backToLogin();
                    } else {
                        showError(errorMessage);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("An error occurred during registration.");
                }
            }
        };

        worker.execute();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void backToLogin() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }

    // ===== Background Panel Class =====
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
        SwingUtilities.invokeLater(SignupUI::new);
    }
}