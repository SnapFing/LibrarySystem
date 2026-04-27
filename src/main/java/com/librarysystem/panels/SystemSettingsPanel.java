package com.librarysystem.panels;

import javax.swing.*;
import java.awt.*;

public class SystemSettingsPanel extends JPanel {

    public SystemSettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("⚙️ System Settings", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Settings sections
        mainPanel.add(createSettingSection("📅 Borrowing Rules",
                "Configure default borrowing period, maximum books per member, etc."));
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(createSettingSection("💰 Fine Settings",
                "Set up late return fines and payment methods"));
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(createSettingSection("📧 Email Notifications",
                "Configure automated email reminders for due dates"));
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(createSettingSection("🔒 Security Settings",
                "Password policies, session timeout, and access controls"));
        mainPanel.add(Box.createVerticalStrut(15));

        mainPanel.add(createSettingSection("💾 Backup & Restore",
                "Database backup and restoration options"));

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        // Note
        JLabel noteLabel = new JLabel("💡 Changes require admin privileges", JLabel.CENTER);
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        noteLabel.setForeground(Color.GRAY);
        add(noteLabel, BorderLayout.SOUTH);
    }

    private JPanel createSettingSection(String title, String description) {
        JPanel section = new JPanel(new BorderLayout(10, 10));
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(Color.GRAY);

        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(descLabel);

        JButton configButton = new JButton("Configure");
        configButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Configuration panel coming soon!",
                        title,
                        JOptionPane.INFORMATION_MESSAGE)
        );

        section.add(leftPanel, BorderLayout.CENTER);
        section.add(configButton, BorderLayout.EAST);

        return section;
    }
}