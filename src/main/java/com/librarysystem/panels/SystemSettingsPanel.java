package com.librarysystem.panels;

import com.librarysystem.db.DBHelper;
import com.librarysystem.utils.FineCalculator;
import com.librarysystem.utils.RefreshManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.Properties;

public class SystemSettingsPanel extends JPanel {

    // ---- Institution profile fields ----
    private JTextField institutionNameField;
    private JComboBox<String> institutionTypeCombo;

    // ---- Borrowing / Fines fields ----
    private JSpinner loanPeriodSpinner, maxBooksSpinner;
    private JSpinner fineRateSpinner;

    // ---- Theme field ----
    private JComboBox<String> themeCombo;

    // ---- Status label ----
    private JLabel statusLabel;

    public SystemSettingsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(UIManager.getColor("Panel.background"));

        // Title
        JLabel title = new JLabel("⚙️ System Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        add(titlePanel, BorderLayout.NORTH);

        // Main content: two columns
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 20));
        mainPanel.setOpaque(false);

        // Left column: Institution + Borrowing/Fines
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);

        leftColumn.add(createInstitutionCard());
        leftColumn.add(Box.createVerticalStrut(20));
        leftColumn.add(createBorrowingCard());
        leftColumn.add(Box.createVerticalStrut(20));
        leftColumn.add(createFinesCard());

        // Right column: Theme + Backup
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);

        rightColumn.add(createThemeCard());
        rightColumn.add(Box.createVerticalStrut(20));
        rightColumn.add(createBackupCard());

        mainPanel.add(leftColumn);
        mainPanel.add(rightColumn);
        add(mainPanel, BorderLayout.CENTER);

        // Bottom: Save button and status
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);

        JButton saveBtn = new JButton("💾 Save All Settings");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setBackground(new Color(52, 152, 219));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> saveAllSettings());

        bottomPanel.add(saveBtn, BorderLayout.EAST);
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load current values
        loadSettings();
    }

    // ========== Institution Card ==========
    private JPanel createInstitutionCard() {
        JPanel card = new ShadowPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("🏫 Institution Profile");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        institutionNameField = new ModernTextField(20);
        institutionTypeCombo = new JComboBox<>(new String[]{"School", "Public Library", "Other"});

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Name:"), c);
        c.gridx = 1; form.add(institutionNameField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Type:"), c);
        c.gridx = 1; form.add(institutionTypeCombo, c);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ========== Borrowing Rules Card ==========
    private JPanel createBorrowingCard() {
        JPanel card = new ShadowPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("📅 Borrowing Rules");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        loanPeriodSpinner = new JSpinner(new SpinnerNumberModel(14, 1, 90, 1));
        maxBooksSpinner   = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Default loan period (days):"), c);
        c.gridx = 1; form.add(loanPeriodSpinner, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Max books per member:"), c);
        c.gridx = 1; form.add(maxBooksSpinner, c);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ========== Fines Card ==========
    private JPanel createFinesCard() {
        JPanel card = new ShadowPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("💰 Fine Settings");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        fineRateSpinner = new JSpinner(new SpinnerNumberModel(5.00, 0.00, 100.00, 0.50));

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Fine per day (K):"), c);
        c.gridx = 1; form.add(fineRateSpinner, c);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    // ========== Theme Card ==========
    private JPanel createThemeCard() {
        JPanel card = new ShadowPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("🎨 Theme");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.setOpaque(false);

        themeCombo = new JComboBox<>(new String[]{
                "Light (FlatLaf)", "Dark (FlatLaf Darcula)", "IntelliJ Light", "IntelliJ Dark"
        });
        form.add(themeCombo);
        card.add(form, BorderLayout.CENTER);

        return card;
    }

    // ========== Backup Card ==========
    private JPanel createBackupCard() {
        JPanel card = new ShadowPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel header = new JLabel("💾 Backup & Restore");
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        card.add(header, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 10, 10));
        buttons.setOpaque(false);

        JButton backupBtn = new JButton("📤 Export Database Backup");
        backupBtn.addActionListener(e -> performBackup());

        JButton restoreBtn = new JButton("📥 Restore from Backup");
        restoreBtn.addActionListener(e -> performRestore());

        buttons.add(backupBtn);
        buttons.add(restoreBtn);
        card.add(buttons, BorderLayout.CENTER);

        return card;
    }

    // ========== Load / Save ==========
    private void loadSettings() {
        try (Connection conn = DBHelper.getConnection()) {
            // Institution profile
            PreparedStatement ps = conn.prepareStatement("SELECT name, type FROM institution_profile WHERE id = 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                institutionNameField.setText(rs.getString("name"));
                institutionTypeCombo.setSelectedItem(rs.getString("type"));
            }

            // System settings
            String loanDays = getSetting(conn, "default_loan_period_days", "14");
            String maxBooks = getSetting(conn, "max_books_per_member", "5");
            String fineRate = getSetting(conn, "fine_per_day", "5.00");

            loanPeriodSpinner.setValue(Integer.parseInt(loanDays));
            maxBooksSpinner.setValue(Integer.parseInt(maxBooks));
            fineRateSpinner.setValue(Double.parseDouble(fineRate));

            // Theme (stored in settings or local config; for now we keep in memory)
            // We can also store in system_settings if desired
            String theme = getSetting(conn, "ui_theme", "Light (FlatLaf)");
            themeCombo.setSelectedItem(theme);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading settings.");
        }
    }

    private void saveAllSettings() {
        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Institution profile
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE institution_profile SET name=?, type=? WHERE id=1");
                ps.setString(1, institutionNameField.getText().trim());
                ps.setString(2, (String) institutionTypeCombo.getSelectedItem());
                ps.executeUpdate();

                // System settings
                saveSetting(conn, "default_loan_period_days", loanPeriodSpinner.getValue().toString());
                saveSetting(conn, "max_books_per_member", maxBooksSpinner.getValue().toString());
                saveSetting(conn, "fine_per_day", fineRateSpinner.getValue().toString());
                saveSetting(conn, "ui_theme", (String) themeCombo.getSelectedItem());

                conn.commit();

                // Reload FineCalculator cache
                FineCalculator.getFinePerDay(); // forces re-read

                // Notify other panels of changes
                RefreshManager.getInstance().notifyRefreshAll();

                statusLabel.setText("✅ Settings saved successfully.");
                JOptionPane.showMessageDialog(this, "Settings saved!\nSome changes may require a restart to take full effect.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error saving settings.");
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- Helpers ----
    private String getSetting(Connection conn, String key, String defaultVal) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT setting_value FROM system_settings WHERE setting_key = ?");
        ps.setString(1, key);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("setting_value");
        // If missing, insert default
        PreparedStatement ins = conn.prepareStatement("INSERT INTO system_settings (setting_key, setting_value) VALUES (?,?)");
        ins.setString(1, key);
        ins.setString(2, defaultVal);
        ins.executeUpdate();
        return defaultVal;
    }

    private void saveSetting(Connection conn, String key, String value) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO system_settings (setting_key, setting_value) VALUES (?,?) ON DUPLICATE KEY UPDATE setting_value=?");
        ps.setString(1, key);
        ps.setString(2, value);
        ps.setString(3, value);
        ps.executeUpdate();
    }

    // ========== Backup / Restore ==========
    private void performBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Database Backup");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".sql")) {
            file = new File(file.getAbsolutePath() + ".sql");
        }

        try {
            // Determine DB type
            Properties props = new Properties();
            File configFile = new File(System.getProperty("user.home"), ".LibrarySystem/librarysystem.properties");
            if (configFile.exists()) {
                try (InputStream in = new FileInputStream(configFile)) { props.load(in); }
            }
            String dbType = props.getProperty("db.type", "mysql");

            if ("embedded".equalsIgnoreCase(dbType)) {
                // H2: just copy the database file
                File dbFile = new File(System.getProperty("user.home"), ".LibrarySystem/database.mv.db");
                if (dbFile.exists()) {
                    Files.copy(dbFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(this, "✅ Backup saved successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Database file not found.");
                }
            } else {
                // MySQL: run mysqldump
                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");

                // Extract database name from URL
                String dbName = url.substring(url.lastIndexOf("/") + 1);
                if (dbName.contains("?")) dbName = dbName.substring(0, dbName.indexOf("?"));

                ProcessBuilder pb = new ProcessBuilder(
                        "mysqldump", "-u", user, "-p" + pass, dbName, "--result-file=" + file.getAbsolutePath());
                Process p = pb.start();
                int exitCode = p.waitFor();

                if (exitCode == 0) {
                    JOptionPane.showMessageDialog(this, "✅ Backup saved successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Backup failed (exit code " + exitCode + ").");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void performRestore() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Database Backup");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();

        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ Restoring will overwrite the current database!\nAre you sure?",
                "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Properties props = new Properties();
            File configFile = new File(System.getProperty("user.home"), ".LibrarySystem/librarysystem.properties");
            if (configFile.exists()) {
                try (InputStream in = new FileInputStream(configFile)) { props.load(in); }
            }
            String dbType = props.getProperty("db.type", "mysql");

            if ("embedded".equalsIgnoreCase(dbType)) {
                File dbFile = new File(System.getProperty("user.home"), ".LibrarySystem/database.mv.db");
                Files.copy(file.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "✅ Restore complete. Please restart the application.");
            } else {
                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");
                String dbName = url.substring(url.lastIndexOf("/") + 1);
                if (dbName.contains("?")) dbName = dbName.substring(0, dbName.indexOf("?"));

                ProcessBuilder pb = new ProcessBuilder(
                        "mysql", "-u", user, "-p" + pass, dbName);
                pb.redirectInput(file);
                Process p = pb.start();
                int exitCode = p.waitFor();

                if (exitCode == 0) {
                    JOptionPane.showMessageDialog(this, "✅ Restore complete. Please restart the application.");
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Restore failed (exit code " + exitCode + ").");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ---- Reuse ModernTextField from other panels ----
    private static class ModernTextField extends JTextField {
        public ModernTextField(int cols) {
            super(cols);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(8, UIManager.getColor("Component.borderColor")),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }

    private static class RoundedBorder extends javax.swing.border.LineBorder {
        private final int radius;
        public RoundedBorder(int radius, Color color) { super(color); this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lineColor);
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

    private static class ShadowPanel extends JPanel {
        public ShadowPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0,0,0,20));
            g2.fillRoundRect(2, 2, getWidth()-5, getHeight()-5, 16, 16);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth()-5, getHeight()-5, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}