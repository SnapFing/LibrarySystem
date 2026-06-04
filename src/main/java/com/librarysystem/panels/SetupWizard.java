package com.librarysystem.panels;

import com.librarysystem.db.DBHelper;
import com.librarysystem.db.DatabaseMigrator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * First‑run setup wizard shown when no configuration exists.
 * Steps: DB type → MySQL details / H2 → test connection → migrate → institution profile → finish.
 */
public class SetupWizard extends JDialog {

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JButton nextButton, backButton;

    // Step data
    private String dbType = "mysql"; // or embedded
    private JTextField hostField, portField, dbNameField, userField;
    private JPasswordField passField;
    private JTextField institutionNameField;
    private JComboBox<String> typeCombo;
    private JLabel statusLabel;

    private Connection testConnection = null;

    public SetupWizard(JFrame parent) {
        super(parent, "Library System Setup", true);
        setSize(550, 450);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Create steps
        cardPanel.add(stepWelcome(), "welcome");
        cardPanel.add(stepDatabaseType(), "dbtype");
        cardPanel.add(stepMySQLConfig(), "mysqlconfig");
        cardPanel.add(stepTestMigrate(), "testmigrate");
        cardPanel.add(stepInstitution(), "institution");
        cardPanel.add(stepFinish(), "finish");

        // Navigation buttons
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        backButton = new JButton("Back");
        nextButton = new JButton("Next");
        navPanel.add(backButton);
        navPanel.add(nextButton);

        backButton.addActionListener(e -> cardLayout.previous(cardPanel));
        nextButton.addActionListener(e -> handleNext());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cardPanel, BorderLayout.CENTER);
        getContentPane().add(navPanel, BorderLayout.SOUTH);

        cardLayout.show(cardPanel, "welcome");
        updateButtons();
    }

    private JPanel stepWelcome() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("<html><h2>Welcome to the Library Management System</h2>"
                + "This wizard will help you set up the database and institution profile.<br><br>"
                + "Click Next to begin.</html>", SwingConstants.CENTER);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private JPanel stepDatabaseType() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0;
        p.add(new JLabel("Select Database Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> combo = new JComboBox<>(new String[]{"MySQL (Server)", "Embedded (H2, local file)"});
        combo.addActionListener(e -> dbType = combo.getSelectedIndex() == 0 ? "mysql" : "embedded");
        p.add(combo, gbc);
        return p;
    }

    private JPanel stepMySQLConfig() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,5);

        hostField = new JTextField("localhost", 15);
        portField = new JTextField("3306", 5);
        dbNameField = new JTextField("library_db", 10);
        userField = new JTextField("root", 10);
        passField = new JPasswordField(10);

        gbc.gridx=0; gbc.gridy=0; p.add(new JLabel("Host:"), gbc);
        gbc.gridx=1; p.add(hostField, gbc);
        gbc.gridx=0; gbc.gridy=1; p.add(new JLabel("Port:"), gbc);
        gbc.gridx=1; p.add(portField, gbc);
        gbc.gridx=0; gbc.gridy=2; p.add(new JLabel("Database:"), gbc);
        gbc.gridx=1; p.add(dbNameField, gbc);
        gbc.gridx=0; gbc.gridy=3; p.add(new JLabel("User:"), gbc);
        gbc.gridx=1; p.add(userField, gbc);
        gbc.gridx=0; gbc.gridy=4; p.add(new JLabel("Password:"), gbc);
        gbc.gridx=1; p.add(passField, gbc);

        return p;
    }

    private JPanel stepTestMigrate() {
        JPanel p = new JPanel(new BorderLayout());
        JButton testBtn = new JButton("Test Connection & Create Tables");
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        testBtn.addActionListener(e -> {
            try {
                testConnection = createConnection();
                statusLabel.setText("Connection successful! Running schema...");
                DatabaseMigrator.runSchema(testConnection);
                statusLabel.setText("Tables created successfully.");
                updateButtons();
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                testConnection = null;
                updateButtons();
            }
        });
        p.add(testBtn, BorderLayout.NORTH);
        p.add(statusLabel, BorderLayout.CENTER);
        return p;
    }

    private JPanel stepInstitution() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=0; p.add(new JLabel("Institution Name:"), gbc);
        gbc.gridx=1;
        institutionNameField = new JTextField(20);
        p.add(institutionNameField, gbc);

        gbc.gridx=0; gbc.gridy=1; p.add(new JLabel("Type:"), gbc);
        gbc.gridx=1;
        typeCombo = new JComboBox<>(new String[]{"School", "Public Library", "Other"});
        p.add(typeCombo, gbc);

        return p;
    }

    private JPanel stepFinish() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("<html><h2>Setup Complete!</h2>"
                + "Click Finish to save configuration and start the application.</html>", SwingConstants.CENTER);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private void handleNext() {
        String currentCard = null;
        for (Component comp : cardPanel.getComponents()) {
            if (comp.isVisible()) {
                currentCard = comp.getName();
                break;
            }
        }
        // manual tracking since CardLayout doesn't give name easily
        // We'll use a simple sequence logic
        if (currentCard == null) return;

        switch (currentCard) {
            case "welcome":
                cardLayout.show(cardPanel, "dbtype");
                break;
            case "dbtype":
                if (dbType.equals("mysql")) {
                    cardLayout.show(cardPanel, "mysqlconfig");
                } else {
                    cardLayout.show(cardPanel, "testmigrate"); // skip config
                }
                break;
            case "mysqlconfig":
                cardLayout.show(cardPanel, "testmigrate");
                break;
            case "testmigrate":
                if (testConnection == null) {
                    JOptionPane.showMessageDialog(this,
                            "Please test the connection and run migrations first.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cardLayout.show(cardPanel, "institution");
                break;
            case "institution":
                cardLayout.show(cardPanel, "finish");
                nextButton.setText("Finish");
                break;
            case "finish":
                finishSetup();
                break;
        }
        updateButtons();
    }

    private void finishSetup() {
        try {
            // Save configuration properties
            Properties props = new Properties();
            props.setProperty("db.type", dbType);
            if (dbType.equals("mysql")) {
                String url = "jdbc:mysql://" + hostField.getText() + ":" + portField.getText()
                        + "/" + dbNameField.getText();
                props.setProperty("db.url", url);
                props.setProperty("db.user", userField.getText());
                props.setProperty("db.password", new String(passField.getPassword()));
            } else {
                // embedded H2 default
                String home = System.getProperty("user.home");
                props.setProperty("db.url", "jdbc:h2:file:" + home + "/.LibrarySystem/database;AUTO_SERVER=TRUE");
                props.setProperty("db.user", "sa");
                props.setProperty("db.password", "");
            }
            props.setProperty("pool.maximum.size", "10");

            // Write to file
            File dir = new File(System.getProperty("user.home"), ".LibrarySystem");
            if (!dir.exists()) dir.mkdirs();
            File configFile = new File(dir, "librarysystem.properties");
            try (OutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "Library System Configuration");
            }

            // Insert institution profile (if testConnection still open)
            if (testConnection != null && !testConnection.isClosed()) {
                try (Statement stmt = testConnection.createStatement()) {
                    stmt.executeUpdate("DELETE FROM institution_profile WHERE id=1");
                    String sql = "INSERT INTO institution_profile (id, name, type) VALUES (1, '"
                            + institutionNameField.getText().replace("'", "''") + "', '"
                            + typeCombo.getSelectedItem().toString() + "')";
                    stmt.executeUpdate(sql);
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Setup complete. The application will now close.\nPlease restart it.",
                    "Setup", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0); // The user will restart; the new config will be used.

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Connection createConnection() throws SQLException {
        if (dbType.equals("mysql")) {
            String url = "jdbc:mysql://" + hostField.getText() + ":" + portField.getText()
                    + "/" + dbNameField.getText();
            return DriverManager.getConnection(url, userField.getText(),
                    new String(passField.getPassword()));
        } else {
            // H2 embedded
            String home = System.getProperty("user.home");
            String url = "jdbc:h2:file:" + home + "/.LibrarySystem/database;AUTO_SERVER=TRUE";
            return DriverManager.getConnection(url, "sa", "");
        }
    }

    private void updateButtons() {
        // Determine which step is currently shown
        Component visibleComp = null;
        for (Component comp : cardPanel.getComponents()) {
            if (comp.isVisible()) {
                visibleComp = comp;
                break;
            }
        }
        if (visibleComp == null) return;
        String step = visibleComp.getName();

        // Back button is hidden on first step and finish step
        backButton.setVisible(!step.equals("welcome") && !step.equals("finish"));

        // Next button text changes on last step
        if (step.equals("finish")) {
            nextButton.setText("Finish");
        } else {
            nextButton.setText("Next");
        }

        // Disable Next on test/migrate step until test is successful
        if (step.equals("testmigrate")) {
            nextButton.setEnabled(testConnection != null);
        } else {
            nextButton.setEnabled(true);
        }
    }
}