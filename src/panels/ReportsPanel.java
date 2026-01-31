package panels;

import javax.swing.*;
import java.awt.*;

public class ReportsPanel extends JPanel {

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("📊 Reports & Analytics", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new GridLayout(2, 2, 20, 20));

        // Card 1: Most Borrowed Books
        JPanel card1 = createReportCard("📚 Most Borrowed Books",
                "View the most popular books in the library");

        // Card 2: Active Members
        JPanel card2 = createReportCard("👥 Active Members",
                "See member activity and statistics");

        // Card 3: Overdue Books
        JPanel card3 = createReportCard("⚠️ Overdue Books",
                "List of books that are past due date");

        // Card 4: Monthly Statistics
        JPanel card4 = createReportCard("📈 Monthly Statistics",
                "Borrowing trends and monthly reports");

        mainPanel.add(card1);
        mainPanel.add(card2);
        mainPanel.add(card3);
        mainPanel.add(card4);

        add(mainPanel, BorderLayout.CENTER);

        // Note
        JLabel noteLabel = new JLabel("💡 Click on a card to view detailed reports", JLabel.CENTER);
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        noteLabel.setForeground(Color.GRAY);
        add(noteLabel, BorderLayout.SOUTH);
    }

    private JPanel createReportCard(String title, String description) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel descLabel = new JLabel("<html>" + description + "</html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(Color.GRAY);

        JButton viewButton = new JButton("View Report");
        viewButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Report feature coming soon!",
                        title,
                        JOptionPane.INFORMATION_MESSAGE)
        );

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        card.add(viewButton, BorderLayout.SOUTH);

        return card;
    }
}