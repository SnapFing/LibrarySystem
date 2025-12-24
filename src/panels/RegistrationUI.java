// java
package panels;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;
import utils.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Member Registration UI
 * - Creates user with role = MEMBER
 * - Inserts corresponding record into members table
 * - Redirects to LoginUI on success
 */
public class RegistrationUI extends JFrame {

    private JTextField fnameField, lnameField, usernameField, emailField, phoneField;
    private JPasswordField passwordField, confirmField;
    private JButton registerButton;
    private JLabel statusLabel;

    public RegistrationUI() {
        setTitle("📚 Member Registration");
        setSize(720, 640);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // ===== Background =====
        BackgroundPanel background = new BackgroundPanel("/panels/lib1.jpg");
        background.setLayout(new BorderLayout());
        setContentPane(background);

        // ===== Logo =====
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        logoPanel.setOpaque(false);
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = logoIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            logoPanel.add(new JLabel(new ImageIcon(scaled)));
        } catch (Exception ignored) {}
        background.add(logoPanel, BorderLayout.NORTH);

        // ===== Form =====
        RoundedPanel formPanel = new RoundedPanel(18);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(0, 0, 0, 180));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 14, 8, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create Member Account", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(title, gbc);

        gbc.gridwidth = 1;

        // First Name
        gbc.gridy = 1; gbc.gridx = 0;
        formPanel.add(label("First Name:"), gbc);
        fnameField = field();
        gbc.gridx = 1;
        formPanel.add(fnameField, gbc);

        // Last Name
        gbc.gridy = 2; gbc.gridx = 0;
        formPanel.add(label("Last Name:"), gbc);
        lnameField = field();
        gbc.gridx = 1;
        formPanel.add(lnameField, gbc);

        // Username
        gbc.gridy = 3; gbc.gridx = 0;
        formPanel.add(label("Username:"), gbc);
        usernameField = field();
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);

        // Email
        gbc.gridy = 4; gbc.gridx = 0;
        formPanel.add(label("Email:"), gbc);
        emailField = field();
        gbc.gridx = 1;
        formPanel.add(emailField, gbc);

        // Phone
        gbc.gridy = 5; gbc.gridx = 0;
        formPanel.add(label("Phone:"), gbc);
        phoneField = field();
        gbc.gridx = 1;
        formPanel.add(phoneField, gbc);

        // Password
        gbc.gridy = 6; gbc.gridx = 0;
        formPanel.add(label("Password:"), gbc);
        passwordField = new JPasswordField(16);
        style(passwordField);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        // Confirm
        gbc.gridy = 7; gbc.gridx = 0;
        formPanel.add(label("Confirm Password:"), gbc);
        confirmField = new JPasswordField(16);
        style(confirmField);
        gbc.gridx = 1;
        formPanel.add(confirmField, gbc);

        // Register Button
        registerButton = new JButton("Register");
        registerButton.setForeground(Color.WHITE);
        registerButton.setOpaque(false);
        gbc.gridy = 8; gbc.gridx = 0; gbc.gridwidth = 2;
        formPanel.add(registerButton, gbc);

        // Status
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.YELLOW);
        gbc.gridy = 9;
        formPanel.add(statusLabel, gbc);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 30));
        center.setOpaque(false);
        center.add(formPanel);
        background.add(center, BorderLayout.CENTER);

        registerButton.addActionListener(this::handleRegister);
        confirmField.addActionListener(this::handleRegister);

        setVisible(true);
    }

    // ===== Registration Logic =====
    private void handleRegister(ActionEvent e) {
        String fname = fnameField.getText().trim();
        String lname = lnameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (fname.isEmpty() || lname.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            statusLabel.setText("Please fill all required fields.");
            statusLabel.setForeground(Color.RED);
            return;
        }

        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            statusLabel.setForeground(Color.RED);
            return;
        }

        registerButton.setEnabled(false);
        statusLabel.setText("Registering...");
        statusLabel.setForeground(Color.YELLOW);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String message = "Registration failed.";

            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBHelper.getConnection()) {
                    conn.setAutoCommit(false);

                    // Check username
                    PreparedStatement check = conn.prepareStatement(
                            "SELECT id FROM users WHERE username=?");
                    check.setString(1, username);
                    if (check.executeQuery().next()) {
                        message = "Username already exists.";
                        conn.rollback();
                        return false;
                    }

                    // Insert user
                    String hashed = PasswordUtil.hashPassword(password);
                    PreparedStatement userStmt = conn.prepareStatement(
                            "INSERT INTO users (username, password, role, is_active) VALUES (?,?,?,?)",
                            PreparedStatement.RETURN_GENERATED_KEYS);

                    userStmt.setString(1, username);
                    userStmt.setString(2, hashed);
                    userStmt.setString(3, "MEMBER");
                    userStmt.setBoolean(4, true);
                    userStmt.executeUpdate();

                    ResultSet keys = userStmt.getGeneratedKeys();
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    int userId = keys.getInt(1);

                    // Insert member
                    PreparedStatement memStmt = conn.prepareStatement(
                            "INSERT INTO members (user_id, fname, lname, email, phone) VALUES (?,?,?,?,?)");
                    memStmt.setInt(1, userId);
                    memStmt.setString(2, fname);
                    memStmt.setString(3, lname);
                    memStmt.setString(4, email);
                    memStmt.setString(5, phone);
                    memStmt.executeUpdate();

                    conn.commit();
                    message = "Registration successful.";
                    return true;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    message = "Database error.";
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    statusLabel.setText(message);
                    statusLabel.setForeground(ok ? new Color(46,204,113) : Color.RED);
                    registerButton.setEnabled(true);

                    if (ok) {
                        JOptionPane.showMessageDialog(
                                RegistrationUI.this,
                                "Account created successfully. Please login.",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        dispose();
                        SwingUtilities.invokeLater(LoginUI::new);
                    }

                } catch (Exception ex) {
                    statusLabel.setText("Unexpected error.");
                    statusLabel.setForeground(Color.RED);
                    registerButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    // ===== Helpers =====
    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Color.WHITE);
        return l;
    }

    private JTextField field() {
        JTextField f = new JTextField(16);
        style(f);
        return f;
    }

    private void style(JComponent c) {
        c.setOpaque(false);
        c.setForeground(Color.WHITE);
    }

    // ===== Background =====
    static class BackgroundPanel extends JPanel {
        private Image bg;
        public BackgroundPanel(String path) {
            try { bg = new ImageIcon(getClass().getResource(path)).getImage(); }
            catch (Exception ignored) {}
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null)
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ===== Rounded Panel =====
    static class RoundedPanel extends JPanel {
        private final int r;
        public RoundedPanel(int radius) { r = radius; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarculaLaf()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(RegistrationUI::new);
    }
}
