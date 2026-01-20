package panels.members;

import com.formdev.flatlaf.FlatDarculaLaf;
import db.DBHelper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.imageio.ImageIO;

/**
 * Complete Profile Form - Required after first login
 * Collects academic and personal information from members
 *
 * NOTE: This class is implemented as a modal JDialog (not a JFrame) so it
 * can be shown over a parent window and return control properly.
 */
public class CompleteProfileUI extends JDialog {
    private final int memberId;
    private final String memberName;

    private JLabel profilePictureLabel;
    private File selectedImageFile;
    private JTextField gradeYearField, departmentField, studentIdField;
    private JTextArea coursesArea, interestsArea, genresArea;
    private JTextField emergencyNameField, emergencyPhoneField;
    private JButton uploadPhotoButton, completeButton, skipButton;
    private JProgressBar progressBar;

    public CompleteProfileUI(Window owner, int memberId, String memberName) {
        super(owner, "📝 Complete Your Profile", ModalityType.APPLICATION_MODAL);
        this.memberId = memberId;
        this.memberName = memberName;

        setSize(800, 750);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Force profile completion or explicit skip
        setLocationRelativeTo(owner);
        setResizable(false);

        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    public static void showDialog(Window owner, int memberId, String memberName) {
        CompleteProfileUI dlg = new CompleteProfileUI(owner, memberId, memberName);
        dlg.setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(52, 152, 219));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        JLabel logo = new JLabel();
        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/panels/SNAPFING-LOGO.png"));
            Image scaled = raw.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception ignored) {}

        JLabel titleLabel = new JLabel("Welcome, " + memberName + "!");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        left.add(logo);
        left.add(titleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        JLabel subtitleLabel = new JLabel("Please complete your profile to continue");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(240, 240, 240));
        right.add(subtitleLabel);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(createFormContent());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFormContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // === PROFILE PICTURE SECTION ===
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel sectionLabel = new JLabel("📸 Profile Picture");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(sectionLabel, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        profilePictureLabel = new JLabel();
        profilePictureLabel.setPreferredSize(new Dimension(150, 150));
        profilePictureLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        profilePictureLabel.setHorizontalAlignment(JLabel.CENTER);
        profilePictureLabel.setText("No Photo");
        panel.add(profilePictureLabel, gbc);

        gbc.gridx = 1;
        uploadPhotoButton = new JButton("📁 Upload Photo");
        uploadPhotoButton.setToolTipText("Upload profile picture (JPG, PNG)");
        uploadPhotoButton.addActionListener(e -> uploadPhoto());
        panel.add(uploadPhotoButton, gbc);
        row++;

        // === ACADEMIC INFORMATION SECTION ===
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(20), gbc);
        row++;

        gbc.gridy = row;
        JLabel academicLabel = new JLabel("🎓 Academic Information");
        academicLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(academicLabel, gbc);
        row++;

        // Grade/Year
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Grade/Year of Study:"), gbc);
        gbc.gridx = 1;
        gradeYearField = new JTextField(20);
        gradeYearField.setToolTipText("e.g., Grade 10, Year 3, 3rd Year");
        panel.add(gradeYearField, gbc);
        row++;

        // Department
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Department/Faculty:"), gbc);
        gbc.gridx = 1;
        departmentField = new JTextField(20);
        departmentField.setToolTipText("e.g., Computer Science, Engineering");
        panel.add(departmentField, gbc);
        row++;

        // Student ID
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        studentIdField = new JTextField(20);
        studentIdField.setToolTipText("Your student identification number");
        panel.add(studentIdField, gbc);
        row++;

        // Courses/Subjects
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Courses/Subjects:"), gbc);
        gbc.gridx = 1;
        coursesArea = new JTextArea(3, 20);
        coursesArea.setLineWrap(true);
        coursesArea.setWrapStyleWord(true);
        coursesArea.setToolTipText("Enter your courses, separated by commas");
        JScrollPane coursesScroll = new JScrollPane(coursesArea);
        panel.add(coursesScroll, gbc);
        row++;

        // === INTERESTS SECTION ===
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(20), gbc);
        row++;

        gbc.gridy = row;
        JLabel interestsLabel = new JLabel("📚 Reading Interests");
        interestsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(interestsLabel, gbc);
        row++;

        // Interests
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Your Interests:"), gbc);
        gbc.gridx = 1;
        interestsArea = new JTextArea(3, 20);
        interestsArea.setLineWrap(true);
        interestsArea.setWrapStyleWord(true);
        interestsArea.setToolTipText("e.g., Programming, History, Science Fiction");
        JScrollPane interestsScroll = new JScrollPane(interestsArea);
        panel.add(interestsScroll, gbc);
        row++;

        // Favorite Genres
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Favorite Genres:"), gbc);
        gbc.gridx = 1;
        genresArea = new JTextArea(3, 20);
        genresArea.setLineWrap(true);
        genresArea.setWrapStyleWord(true);
        genresArea.setToolTipText("e.g., Mystery, Romance, Non-fiction, Biography");
        JScrollPane genresScroll = new JScrollPane(genresArea);
        panel.add(genresScroll, gbc);
        row++;

        // === EMERGENCY CONTACT SECTION ===
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(20), gbc);
        row++;

        gbc.gridy = row;
        JLabel emergencyLabel = new JLabel("🚨 Emergency Contact");
        emergencyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(emergencyLabel, gbc);
        row++;

        // Emergency Contact Name
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Contact Name: *"), gbc);
        gbc.gridx = 1;
        emergencyNameField = new JTextField(20);
        emergencyNameField.setToolTipText("Full name of emergency contact");
        panel.add(emergencyNameField, gbc);
        row++;

        // Emergency Contact Phone
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Contact Phone: *"), gbc);
        gbc.gridx = 1;
        emergencyPhoneField = new JTextField(20);
        emergencyPhoneField.setToolTipText("Phone number of emergency contact");
        panel.add(emergencyPhoneField, gbc);
        row++;

        // Note
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel noteLabel = new JLabel("* = Required fields");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        noteLabel.setForeground(Color.GRAY);
        panel.add(noteLabel, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.NORTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        completeButton = new JButton("✅ Complete Profile");
        completeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        completeButton.setBackground(new Color(40, 167, 69));
        completeButton.setForeground(Color.WHITE);
        completeButton.setFocusPainted(false);
        completeButton.addActionListener(e -> handleComplete());

        skipButton = new JButton("⏭️ Skip for Now");
        skipButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        skipButton.setToolTipText("You can complete your profile later from settings");
        skipButton.addActionListener(e -> handleSkip());

        buttonPanel.add(completeButton);
        buttonPanel.add(skipButton);

        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private void uploadPhoto() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Profile Picture");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();

            try {
                // Display preview
                BufferedImage img = ImageIO.read(selectedImageFile);
                Image scaledImg = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                profilePictureLabel.setIcon(new ImageIcon(scaledImg));
                profilePictureLabel.setText("");

                JOptionPane.showMessageDialog(this,
                        "✅ Photo selected successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "❌ Error loading image: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleComplete() {
        // Validate required fields
        String emergencyName = emergencyNameField.getText().trim();
        String emergencyPhone = emergencyPhoneField.getText().trim();

        if (emergencyName.isEmpty() || emergencyPhone.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "❌ Please fill in all required fields (Emergency Contact)",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        completeButton.setEnabled(false);
        skipButton.setEnabled(false);
        progressBar.setVisible(true);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try (Connection conn = DBHelper.getConnection()) {
                    // Save profile picture if selected
                    String picturePath = null;
                    if (selectedImageFile != null) {
                        picturePath = saveProfilePicture();
                    }

                    // Update member profile
                    String sql = "UPDATE members SET " +
                            "profile_completed = TRUE, " +
                            "profile_picture = ?, " +
                            "grade_or_year = ?, " +
                            "department = ?, " +
                            "student_id = ?, " +
                            "courses_or_subjects = ?, " +
                            "interests = ?, " +
                            "favorite_genres = ?, " +
                            "emergency_contact_name = ?, " +
                            "emergency_contact_phone = ? " +
                            "WHERE id = ?";

                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, picturePath);
                    stmt.setString(2, gradeYearField.getText().trim());
                    stmt.setString(3, departmentField.getText().trim());
                    stmt.setString(4, studentIdField.getText().trim());
                    stmt.setString(5, coursesArea.getText().trim());
                    stmt.setString(6, interestsArea.getText().trim());
                    stmt.setString(7, genresArea.getText().trim());
                    stmt.setString(8, emergencyName);
                    stmt.setString(9, emergencyPhone);
                    stmt.setInt(10, memberId);

                    stmt.executeUpdate();
                    return true;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Error saving profile: " + ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                completeButton.setEnabled(true);
                skipButton.setEnabled(true);

                try {
                    Boolean success = get();
                    if (success) {
                        JOptionPane.showMessageDialog(CompleteProfileUI.this,
                                "✅ Profile completed successfully!\n\nWelcome to the library portal!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        // close dialog and hand control back to owner — caller should open portal if required
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(CompleteProfileUI.this,
                                "❌ " + errorMessage,
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private String saveProfilePicture() {
        try {
            // Create profile_pictures directory if it doesn't exist
            File uploadDir = new File("profile_pictures");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate unique filename
            String extension = selectedImageFile.getName().substring(
                    selectedImageFile.getName().lastIndexOf("."));
            String filename = "member_" + memberId + "_" + System.currentTimeMillis() + extension;
            File destFile = new File(uploadDir, filename);

            // Copy file
            Files.copy(selectedImageFile.toPath(), destFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            return destFile.getPath();

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void handleSkip() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to skip profile completion?\n\n" +
                        "You can complete your profile later from settings.",
                "Skip Profile",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    // legacy main for testing
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LookAndFeel");
        }

        SwingUtilities.invokeLater(() -> showDialog(null, 1, "Test User"));
    }
}

