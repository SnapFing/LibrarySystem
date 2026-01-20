package panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;
import panels.members.CompleteProfileUI;
import panels.members.MemberPortalUI;
import panels.members.SignupUI;
import utils.PasswordUtil;

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
    private JButton signupButton;
    private JLabel statusLabel;
    private JComboBox<String> loginTypeCombo;

    public LoginUI() {
        setTitle("📚 Library Login");
        setSize(700, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Add shutdown hook to close connection pool gracefully
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowAdapter e) {
                DBHelper.closePool();
            }
        });

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

        // Login Type Selector
        JLabel typeLabel = new JLabel("Login as:");
        typeLabel.setForeground(Color.WHITE);
        loginTypeCombo = new JComboBox<>(new String[]{"Staff (Admin/Librarian)", "Member"});
        loginTypeCombo.setToolTipText("Select your account type");

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        formPanel.add(typeLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(loginTypeCombo, gbc);

        // Username/Email
        JLabel userLabel = new JLabel("👤 Email/Username:");
        userLabel.setForeground(Color.WHITE);
        usernameField = new JTextField(15);
        usernameField.setOpaque(false);
        usernameField.setForeground(Color.WHITE);
        usernameField.setToolTipText("Enter your email (members) or username (staff)");

        gbc.gridx = 0; gbc.gridy = 2;
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
        loginButton = new JButton("🔐 Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(52, 152, 219));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        // Signup Button
        signupButton = new JButton("📝 New Member? Sign Up");
        signupButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signupButton.setForeground(new Color(100, 181, 246));
        signupButton.setBorderPainted(false);
        signupButton.setContentAreaFilled(false);
        signupButton.setFocusPainted(false);
        signupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupButton.addActionListener(e -> openSignup());
        gbc.gridy = 5;
        formPanel.add(signupButton, gbc);

        // Status Label
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 6;
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
        String loginType = (String) loginTypeCombo.getSelectedItem();
        boolean isMember = loginType.contains("Member");

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter credentials.");
            return;
        }

        // Disable login button to prevent multiple clicks
        loginButton.setEnabled(false);
        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(Color.YELLOW);

        // Perform login in background thread to keep UI responsive
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String role;
            private int userId;
            private String displayName;
            private boolean profileCompleted = true; // default true for non-members
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                if (isMember) {
                    return loginMember(username, password);
                } else {
                    return loginStaff(username, password);
                }
            }

            @Override
            protected void done() {
                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(LoginUI.this,
                                "✅ Login successful!\nWelcome, " + displayName,
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);

                        if (isMember) {
                            // If profile not completed, show modal dialog first
                            Window owner = LoginUI.this;
                            if (!profileCompleted) {
                                CompleteProfileUI.showDialog(owner, userId, displayName);
                            }
                            // After dialog closes (or if profile already completed), open portal
                            SwingUtilities.invokeLater(() -> MemberPortalUI.showInFrame(userId, displayName));
                        } else {
                            SwingUtilities.invokeLater(() -> new LibrarySystemUI(role));
                        }
                        dispose();
                    } else {
                        statusLabel.setText(errorMessage);
                        statusLabel.setForeground(Color.RED);
                        loginButton.setEnabled(true);
                        passwordField.setText("");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusLabel.setText("An error occurred during login.");
                    statusLabel.setForeground(Color.RED);
                    loginButton.setEnabled(true);
                }
            }

            private boolean loginMember(String email, String password) {
                try (Connection conn = DBHelper.getConnection()) {
                    String sql = "SELECT id, fname, lname, password, is_active, COALESCE(profile_completed, FALSE) as profile_completed FROM members WHERE email=?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, email);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        // Check if account is active
                        boolean isActive = rs.getBoolean("is_active");
                        if (!isActive) {
                            errorMessage = "Account is deactivated. Contact library staff.";
                            return false;
                        }

                        String storedHash = rs.getString("password");

                        // Check if password field exists and has a value
                        if (storedHash == null || storedHash.isEmpty()) {
                            errorMessage = "Account not set up for member login. Please contact library staff.";
                            return false;
                        }

                        // Verify password
                        if (PasswordUtil.verifyPassword(password, storedHash)) {
                            userId = rs.getInt("id");
                            displayName = rs.getString("fname") + " " + rs.getString("lname");
                            role = "Member";
                            profileCompleted = rs.getBoolean("profile_completed");

                            return true;
                        } else {
                            errorMessage = "Invalid email or password.";
                            return false;
                        }
                    } else {
                        errorMessage = "Invalid email or password.";
                        return false;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Database connection error.";
                    return false;
                }
            }

            private boolean loginStaff(String username, String password) {
                try (Connection conn = DBHelper.getConnection()) {
                    String sql = "SELECT id, username, password, role, is_active FROM users WHERE username=?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, username);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        // Check if account is active
                        boolean isActive = rs.getBoolean("is_active");
                        if (!isActive) {
                            errorMessage = "Account is deactivated. Contact administrator.";
                            return false;
                        }

                        String storedHash = rs.getString("password");
                        role = rs.getString("role");

                        // Verify password using BCrypt
                        if (PasswordUtil.verifyPassword(password, storedHash)) {
                            userId = rs.getInt("id");
                            displayName = rs.getString("username");

                            // Check if password needs rehashing
                            if (PasswordUtil.needsRehash(storedHash)) {
                                String newHash = PasswordUtil.hashPassword(password);
                                String updateSql = "UPDATE users SET password=? WHERE id=?";
                                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                                updateStmt.setString(1, newHash);
                                updateStmt.setInt(2, userId);
                                updateStmt.executeUpdate();
                            }

                            // Update last login timestamp
                            String updateLoginSql = "UPDATE users SET last_login=CURRENT_TIMESTAMP WHERE id=?";
                            PreparedStatement loginStmt = conn.prepareStatement(updateLoginSql);
                            loginStmt.setInt(1, userId);
                            loginStmt.executeUpdate();

                            return true;
                        } else {
                            errorMessage = "Invalid username or password.";
                            return false;
                        }
                    } else {
                        errorMessage = "Invalid username or password.";
                        return false;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Database connection error.";
                    return false;
                }
            }
        };

        worker.execute();
    }

    private void openSignup() {
        dispose();
        SwingUtilities.invokeLater(() -> new SignupUI().setVisible(true));
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
        SwingUtilities.invokeLater(LoginUI::new);
    }
}

