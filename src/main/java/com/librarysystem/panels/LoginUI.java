package com.librarysystem.panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.librarysystem.LibrarySystemUI;
import com.librarysystem.db.DBHelper;
import com.librarysystem.utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, signupButton;
    private JLabel statusLabel;

    public LoginUI() {
        setTitle("📚 Library Management System - Login");
        setSize(700, 650);
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
        logoPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 30));

        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaledLogo = logoIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoPanel.add(logoLabel);
        } catch (Exception e) {
            // Logo not found, continue without it
        }

        background.add(logoPanel, BorderLayout.NORTH);

        // ===== Login Form Panel =====
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 180));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Library Management System", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Sign in to continue", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        formPanel.add(subtitleLabel, gbc);

        // Username
        JLabel userLabel = new JLabel("👤 Username:");
        userLabel.setForeground(Color.WHITE);
        usernameField = new JTextField(15);
        usernameField.setOpaque(false);
        usernameField.setForeground(Color.WHITE);

        gbc.gridwidth = 1; gbc.gridy = 2; gbc.gridx = 0;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        // Password
        JLabel passLabel = new JLabel("🔑 Password:");
        passLabel.setForeground(Color.WHITE);
        passwordField = new JPasswordField(15);
        passwordField.setOpaque(false);
        passwordField.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        // Login Button
        loginButton = new JButton("🔓 Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        // Status Label
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 5;
        formPanel.add(statusLabel, gbc);

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        gbc.gridy = 6;
        formPanel.add(separator, gbc);

        // New User Label
        JLabel newUserLabel = new JLabel("Don't have an account?", JLabel.CENTER);
        newUserLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newUserLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 7;
        formPanel.add(newUserLabel, gbc);

        // Signup Button
        signupButton = new JButton("📝 Sign Up as Student");
        signupButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 8;
        formPanel.add(signupButton, gbc);

        // Center the form panel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        centerPanel.setOpaque(false);
        centerPanel.add(formPanel);
        background.add(centerPanel, BorderLayout.CENTER);

        // ===== Actions =====
        loginButton.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin);
        signupButton.addActionListener(this::handleSignup);

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
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Username: " + username);

        try (Connection conn = DBHelper.getConnection()) {
            System.out.println("Database connection established");

            // Try to find user in both users and members tables
            String role = null;
            boolean isActive = true;
            boolean found = false;
            String source = null;

            // 1. Check users table (Admin/Librarian)
            String userSql = "SELECT password, role, is_active FROM users WHERE username=?";
            PreparedStatement userStmt = conn.prepareStatement(userSql);
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();

            if (userRs.next()) {
                String storedPassword = userRs.getString("password");
                role = userRs.getString("role");

                // Check if is_active column exists
                try {
                    isActive = userRs.getBoolean("is_active");
                } catch (Exception ex) {
                    isActive = true; // Default to active if column doesn't exist
                }

                System.out.println("Found in users table - Role: " + role);

                // Verify password
                boolean passwordMatches = false;
                if (storedPassword.contains(":")) {
                    // PBKDF2 hashed
                    passwordMatches = PasswordUtil.verifyPassword(password, storedPassword);
                } else {
                    // Plain text (backwards compatibility)
                    passwordMatches = password.equals(storedPassword);
                }

                if (passwordMatches && isActive) {
                    found = true;
                    source = "users";

                    // Update last login
                    try {
                        String updateSql = "UPDATE users SET last_login=NOW() WHERE username=?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                        updateStmt.setString(1, username);
                        updateStmt.executeUpdate();
                    } catch (Exception ex) {
                        // last_login column might not exist
                    }
                } else if (!isActive) {
                    statusLabel.setText("Account is deactivated. Contact admin.");
                    return;
                } else {
                    statusLabel.setText("Invalid username or password.");
                    return;
                }
            }

            // 2. If not found in users, check members table (Students)
            if (!found) {
                String memberSql = "SELECT password, role FROM members WHERE name=?";
                PreparedStatement memberStmt = conn.prepareStatement(memberSql);
                memberStmt.setString(1, username);
                ResultSet memberRs = memberStmt.executeQuery();

                if (memberRs.next()) {
                    String storedPassword = memberRs.getString("password");

                    // Check if password column exists and has value
                    if (storedPassword == null || storedPassword.isEmpty()) {
                        statusLabel.setText("Please sign up first to set your password.");
                        return;
                    }

                    role = "Student"; // Default role for members
                    try {
                        String dbRole = memberRs.getString("role");
                        if (dbRole != null && !dbRole.isEmpty()) {
                            role = dbRole;
                        }
                    } catch (Exception ex) {
                        // role column might not exist
                    }

                    System.out.println("Found in members table - Role: " + role);

                    // Verify password
                    boolean passwordMatches = false;
                    if (storedPassword.contains(":")) {
                        // PBKDF2 hashed
                        passwordMatches = PasswordUtil.verifyPassword(password, storedPassword);
                    } else {
                        // Plain text (backwards compatibility)
                        passwordMatches = password.equals(storedPassword);
                    }

                    if (passwordMatches) {
                        found = true;
                        source = "members";

                        // Update last login
                        try {
                            String updateSql = "UPDATE members SET last_login=NOW() WHERE name=?";
                            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                            updateStmt.setString(1, username);
                            updateStmt.executeUpdate();
                        } catch (Exception ex) {
                            // last_login column might not exist
                        }
                    } else {
                        statusLabel.setText("Invalid username or password.");
                        return;
                    }
                } else {
                    statusLabel.setText("Invalid username or password.");
                    return;
                }
            }

            if (found && role != null) {
                System.out.println("Login successful! Role: " + role + " (from " + source + ")");
                JOptionPane.showMessageDialog(this,
                        "Welcome, " + username + "!\nRole: " + role,
                        "Login Successful",
                        JOptionPane.INFORMATION_MESSAGE);

                // Pass both role and username to the LibrarySystemUI
                final String finalRole = role;
                final String finalUsername = username;

                SwingUtilities.invokeLater(() -> new LibrarySystemUI(finalRole, finalUsername));
                dispose();
            }

        } catch (Exception ex) {
            System.err.println("=== LOGIN ERROR ===");
            ex.printStackTrace();
            statusLabel.setText("Error connecting to database.");
        }

        System.out.println("=== END LOGIN ATTEMPT ===\n");
    }

    private void handleSignup(ActionEvent e) {
        // Show signup dialog
        SignupDialog signupDialog = new SignupDialog(this);
        signupDialog.setVisible(true);
    }

    // ===== Signup Dialog =====
    // ===== Signup Dialog =====
    class SignupDialog extends JDialog {
        private JTextField fnameField, lnameField, emailField, phoneField;
        private JPasswordField passwordField, confirmPasswordField;
        private JButton registerButton, cancelButton;

        public SignupDialog(JFrame parent) {
            super(parent, "Student Registration", true);
            setSize(450, 550);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            // Form Panel
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Title
            JLabel titleLabel = new JLabel("📝 Student Registration", JLabel.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            formPanel.add(titleLabel, gbc);

            gbc.gridwidth = 1;

            // First Name
            gbc.gridx = 0; gbc.gridy = 1;
            formPanel.add(new JLabel("First Name:"), gbc);
            gbc.gridx = 1;
            fnameField = new JTextField(20);
            formPanel.add(fnameField, gbc);

            // Last Name
            gbc.gridx = 0; gbc.gridy = 2;
            formPanel.add(new JLabel("Last Name:"), gbc);
            gbc.gridx = 1;
            lnameField = new JTextField(20);
            formPanel.add(lnameField, gbc);

            // Email
            gbc.gridx = 0; gbc.gridy = 3;
            formPanel.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            emailField = new JTextField(20);
            formPanel.add(emailField, gbc);

            // Phone
            gbc.gridx = 0; gbc.gridy = 4;
            formPanel.add(new JLabel("Phone:"), gbc);
            gbc.gridx = 1;
            phoneField = new JTextField(20);
            formPanel.add(phoneField, gbc);

            // Password
            gbc.gridx = 0; gbc.gridy = 5;
            formPanel.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            formPanel.add(passwordField, gbc);

            // Confirm Password
            gbc.gridx = 0; gbc.gridy = 6;
            formPanel.add(new JLabel("Confirm Password:"), gbc);
            gbc.gridx = 1;
            confirmPasswordField = new JPasswordField(20);
            formPanel.add(confirmPasswordField, gbc);

            // Password requirements
            JLabel reqLabel = new JLabel("<html><small>Password must have: 8+ chars, letter, digit, special char</small></html>");
            reqLabel.setForeground(Color.GRAY);
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
            formPanel.add(reqLabel, gbc);

            // Username info
            JLabel usernameInfoLabel = new JLabel("<html><small><i>Username will be auto-generated from your name</i></small></html>");
            usernameInfoLabel.setForeground(Color.GRAY);
            gbc.gridy = 8;
            formPanel.add(usernameInfoLabel, gbc);

            add(formPanel, BorderLayout.CENTER);

            // Button Panel
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
            String lname = lnameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

            // Validation
            if (fname.isEmpty() || lname.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Please fill all fields!");
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

            // Validate password strength
            String strengthError = PasswordUtil.validatePasswordStrength(password);
            if (strengthError != null) {
                JOptionPane.showMessageDialog(this, "❌ " + strengthError);
                return;
            }

            // Generate username: firstname.lastname or firstname.lastname2 if duplicate
            String baseUsername = (fname + "." + lname).toLowerCase().replaceAll("\\s+", "");
            String username = baseUsername;

            // Register in database
            try (Connection conn = DBHelper.getConnection()) {
                // Check if email already exists
                String checkEmailSql = "SELECT COUNT(*) FROM members WHERE email=?";
                PreparedStatement checkEmailStmt = conn.prepareStatement(checkEmailSql);
                checkEmailStmt.setString(1, email);
                ResultSet emailRs = checkEmailStmt.executeQuery();
                emailRs.next();
                if (emailRs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "❌ Email already exists!");
                    return;
                }

                // Generate unique username
                String checkUsernameSql = "SELECT COUNT(*) FROM members WHERE name=?";
                PreparedStatement checkUsernameStmt = conn.prepareStatement(checkUsernameSql);
                int counter = 1;
                while (true) {
                    checkUsernameStmt.setString(1, username);
                    ResultSet usernameRs = checkUsernameStmt.executeQuery();
                    usernameRs.next();
                    if (usernameRs.getInt(1) == 0) {
                        break; // Username is unique
                    }
                    username = baseUsername + counter;
                    counter++;
                }

                // Hash password
                String hashedPassword = PasswordUtil.hashPassword(password);

                // Insert into members table
                String insertSql = "INSERT INTO members (name, fname, lname, email, phone, password, role) VALUES (?, ?, ?, ?, ?, ?, 'Student')";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, username);  // name column = username
                insertStmt.setString(2, fname);     // fname column
                insertStmt.setString(3, lname);     // lname column
                insertStmt.setString(4, email);
                insertStmt.setString(5, phone);
                insertStmt.setString(6, hashedPassword);
                insertStmt.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "✅ Registration successful!\n\n" +
                                "Your login credentials:\n" +
                                "Username: " + username + "\n" +
                                "Password: (the password you entered)\n\n" +
                                "Please remember your username!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "❌ Error during registration:\n" + ex.getMessage());
            }
        }
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
        try { UIManager.setLookAndFeel(new FlatDarculaLaf()); }
        catch (Exception ex) { System.err.println("Failed to initialize LookAndFeel"); }
        SwingUtilities.invokeLater(LoginUI::new);
    }
}