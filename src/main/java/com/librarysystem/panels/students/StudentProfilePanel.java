package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;
import com.librarysystem.utils.PasswordUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * StudentProfilePanel — Enhanced self-service profile editor.
 *
 * Features:
 *   • Circular profile picture (upload from disk, stored as file path)
 *   • Read-only username/name display (only admins change names)
 *   • Editable: email, phone, address, gender, date-of-birth
 *   • Change password (with strength validation)
 *   • Stats: member since, total books borrowed
 *
 * Image storage strategy:
 *   File saved to  <workingDir>/profile_pics/member_<id>.jpg
 *   DB column      members.profile_picture stores that relative path.
 */
public class StudentProfilePanel extends JPanel {

    private static final String PROFILE_DIR   = "profile_pics";
    private static final int    AVATAR_SIZE    = 120;
    private static final int    MAX_IMAGE_BYTES = 2 * 1024 * 1024; // 2 MB

    // ── Identity (resolved once at construction time) ─────────────────────────
    private final String studentName;
    private int memberId = -1;

    // ── UI fields ─────────────────────────────────────────────────────────────
    private JLabel       avatarLabel;
    private JButton      changePhotoBtn;
    private JLabel       usernameDisplay;          // read-only
    private JTextField   emailField;
    private JTextField   phoneField;
    private JTextField   dobField;                 // YYYY-MM-DD
    private JComboBox<String> genderCombo;
    private JTextField   addressField;

    // Stats
    private JLabel memberSinceLabel;
    private JLabel booksCountLabel;
    private JLabel currentlyBorrowedLabel;

    // Current picture path (relative)
    private String currentPicturePath = null;

    // ── Constructor ───────────────────────────────────────────────────────────
    public StudentProfilePanel(String studentName) {
        this.studentName = studentName;
        ensureProfileDirExists();
        resolveCurrentMemberId();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("👤 My Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        add(titlePanel, BorderLayout.NORTH);

        // ── Main split: left = avatar+stats, right = form ─────────────────────
        JPanel mainPanel = new JPanel(new BorderLayout(20, 0));
        mainPanel.setOpaque(false);

        mainPanel.add(buildAvatarPanel(), BorderLayout.WEST);
        mainPanel.add(buildFormPanel(),   BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // ── Buttons (bottom) ──────────────────────────────────────────────────
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        JButton saveBtn = new JButton("💾 Save Changes");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(new Color(52, 152, 219));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.addActionListener(e -> saveProfile());

        JButton pwdBtn = new JButton("🔑 Change Password");
        pwdBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pwdBtn.addActionListener(e -> changePassword());

        buttonRow.add(saveBtn);
        buttonRow.add(pwdBtn);
        add(buttonRow, BorderLayout.SOUTH);

        loadProfile();
    }

    // ── Avatar panel (left side) ──────────────────────────────────────────────
    private JPanel buildAvatarPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBorder(new EmptyBorder(0, 0, 0, 10));

        // Circular avatar label
        avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Clip to circle
                g2.setClip(new Ellipse2D.Float(0, 0, AVATAR_SIZE, AVATAR_SIZE));
                super.paintComponent(g2);
                g2.dispose();
                // Draw circle border
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setColor(new Color(52, 152, 219));
                g3.setStroke(new BasicStroke(3));
                g3.drawOval(1, 1, AVATAR_SIZE - 3, AVATAR_SIZE - 3);
                g3.dispose();
            }
        };
        avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setMinimumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setMaximumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 60));
        avatarLabel.setText("👤"); // default until image loads

        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatarLabel);
        panel.add(avatarWrapper);

        panel.add(Box.createVerticalStrut(8));

        changePhotoBtn = new JButton("📷 Change Photo");
        changePhotoBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        changePhotoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePhotoBtn.addActionListener(e -> handleChangePhoto());
        panel.add(changePhotoBtn);

        panel.add(Box.createVerticalStrut(20));

        // Username (locked)
        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        userLbl.setForeground(Color.GRAY);
        userLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameDisplay = new JLabel(studentName, SwingConstants.CENTER);
        usernameDisplay.setFont(new Font("Segoe UI", Font.BOLD, 14));
        usernameDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lockedHint = new JLabel("🔒 cannot be changed", SwingConstants.CENTER);
        lockedHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lockedHint.setForeground(Color.GRAY);
        lockedHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(userLbl);
        panel.add(usernameDisplay);
        panel.add(lockedHint);

        panel.add(Box.createVerticalStrut(20));

        // Stats block
        panel.add(statRow("📅 Member since:", memberSinceLabel = new JLabel("—")));
        panel.add(Box.createVerticalStrut(6));
        panel.add(statRow("📚 Total borrowed:", booksCountLabel = new JLabel("—")));
        panel.add(Box.createVerticalStrut(6));
        panel.add(statRow("📖 Currently holding:", currentlyBorrowedLabel = new JLabel("—")));

        return panel;
    }

    // ── Form panel (right side) ───────────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("Personal Information"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Email
        addFormRow(panel, gbc, row++, "Email: *", emailField = new JTextField(25));

        // Phone
        addFormRow(panel, gbc, row++, "Phone: *", phoneField = new JTextField(25));
        addHint(panel, gbc, row++, "10 digits only");

        // Gender
        genderCombo = new JComboBox<>(new String[]{
                "— Select —", "Male", "Female", "Other", "Prefer not to say"});
        addFormRow(panel, gbc, row++, "Gender:", genderCombo);

        // Date of birth
        addFormRow(panel, gbc, row++, "Date of Birth:", dobField = new JTextField(25));
        addHint(panel, gbc, row++, "Format: YYYY-MM-DD  (e.g. 1999-04-15)");

        // Address
        addFormRow(panel, gbc, row++, "Address:", addressField = new JTextField(25));

        // Required note
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel req = new JLabel("* Required fields");
        req.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        req.setForeground(Color.GRAY);
        panel.add(req, gbc);

        return panel;
    }

    // ── Load from DB ──────────────────────────────────────────────────────────
    private void loadProfile() {
        try (Connection conn = DBHelper.getConnection()) {
            if (memberId == -1) { resolveCurrentMemberId(); if (memberId == -1) return; }

            String sql = "SELECT email, phone, gender, date_of_birth, address, " +
                    "profile_picture, created_at FROM members WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                emailField.setText(nvl(rs.getString("email")));
                phoneField.setText(nvl(rs.getString("phone")));

                String gender = rs.getString("gender");
                if (gender != null) genderCombo.setSelectedItem(gender);

                Date dob = rs.getDate("date_of_birth");
                if (dob != null) dobField.setText(dob.toLocalDate().toString());

                addressField.setText(nvl(rs.getString("address")));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    memberSinceLabel.setText(
                            new java.text.SimpleDateFormat("MMM dd, yyyy").format(createdAt));
                }

                currentPicturePath = rs.getString("profile_picture");
                loadAvatarImage(currentPicturePath);
            }

            // Stats
            String totalSql = "SELECT COUNT(*) FROM borrowed_books WHERE member_id=?";
            PreparedStatement totalStmt = conn.prepareStatement(totalSql);
            totalStmt.setInt(1, memberId);
            ResultSet totalRs = totalStmt.executeQuery();
            if (totalRs.next()) booksCountLabel.setText(String.valueOf(totalRs.getInt(1)));

            String currSql = "SELECT COUNT(*) FROM borrowed_books WHERE member_id=? AND status='BORROWED'";
            PreparedStatement currStmt = conn.prepareStatement(currSql);
            currStmt.setInt(1, memberId);
            ResultSet currRs = currStmt.executeQuery();
            if (currRs.next()) currentlyBorrowedLabel.setText(String.valueOf(currRs.getInt(1)));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading profile: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Save profile ──────────────────────────────────────────────────────────
    private void saveProfile() {
        String email   = emailField.getText().trim();
        String phone   = phoneField.getText().trim();
        String gender  = (String) genderCombo.getSelectedItem();
        String dobText = dobField.getText().trim();
        String address = addressField.getText().trim();

        // Validate required
        if (email.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "❌ Email and Phone are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate email
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            JOptionPane.showMessageDialog(this, "❌ Invalid email format.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate phone
        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "❌ Phone must be exactly 10 digits.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate DOB if provided
        LocalDate dob = null;
        if (!dobText.isEmpty()) {
            try {
                dob = LocalDate.parse(dobText, DateTimeFormatter.ISO_LOCAL_DATE);
                if (dob.isAfter(LocalDate.now().minusYears(5))) {
                    JOptionPane.showMessageDialog(this,
                            "❌ Date of birth does not seem valid.",
                            "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ Date of birth must be in YYYY-MM-DD format.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Check email uniqueness (exclude own record)
        try (Connection conn = DBHelper.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM members WHERE email=? AND id<>?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            checkStmt.setInt(2, memberId);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Email is already in use by another member.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Update
            String sql = "UPDATE members SET email=?, phone=?, gender=?, date_of_birth=?, address=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, phone);
            stmt.setString(3, "— Select —".equals(gender) ? null : gender);
            stmt.setDate(4, dob != null ? java.sql.Date.valueOf(dob) : null);
            stmt.setString(5, address.isEmpty() ? null : address);
            stmt.setInt(6, memberId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Profile saved successfully!",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving profile: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Change profile photo ──────────────────────────────────────────────────
    private void handleChangePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Photo");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();

        // Validate size
        if (selected.length() > MAX_IMAGE_BYTES) {
            JOptionPane.showMessageDialog(this,
                    "❌ Image is too large (max 2 MB). Please choose a smaller image.",
                    "File Too Large", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Copy to profile_pics/member_<id>.jpg
        String ext = getExtension(selected.getName());
        String destName = "member_" + memberId + "." + ext;
        Path destPath = Paths.get(PROFILE_DIR, destName);

        try {
            Files.createDirectories(Paths.get(PROFILE_DIR));
            Files.copy(selected.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = PROFILE_DIR + "/" + destName;

            // Save path to DB
            try (Connection conn = DBHelper.getConnection()) {
                String sql = "UPDATE members SET profile_picture=? WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, relativePath);
                stmt.setInt(2, memberId);
                stmt.executeUpdate();
            }

            currentPicturePath = relativePath;
            loadAvatarImage(relativePath);

            JOptionPane.showMessageDialog(this, "✅ Profile photo updated!",
                    "Photo Updated", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving photo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Render avatar image ───────────────────────────────────────────────────
    private void loadAvatarImage(String picturePath) {
        if (picturePath == null || picturePath.isEmpty()) {
            avatarLabel.setIcon(null);
            avatarLabel.setText("👤");
            avatarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 60));
            return;
        }

        try {
            File imgFile = new File(picturePath);
            if (!imgFile.exists()) {
                avatarLabel.setIcon(null);
                avatarLabel.setText("👤");
                return;
            }

            BufferedImage original = ImageIO.read(imgFile);
            if (original == null) return;

            // Scale to AVATAR_SIZE × AVATAR_SIZE keeping aspect ratio
            Image scaled = original.getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);

            avatarLabel.setIcon(new ImageIcon(scaled));
            avatarLabel.setText("");

            avatarLabel.revalidate();
            avatarLabel.repaint();

        } catch (Exception ex) {
            ex.printStackTrace();
            avatarLabel.setIcon(null);
            avatarLabel.setText("👤");
        }
    }

    // ── Change password ───────────────────────────────────────────────────────
    private void changePassword() {
        JPasswordField currentPwd = new JPasswordField();
        JPasswordField newPwd     = new JPasswordField();
        JPasswordField confirmPwd = new JPasswordField();

        Object[] msg = {
                "Current Password:", currentPwd,
                "New Password:",     newPwd,
                "Confirm New:",      confirmPwd,
                " ",
                new JLabel("<html><small>8+ chars · letter · digit · special char</small></html>")
        };

        int opt = JOptionPane.showConfirmDialog(this, msg,
                "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String current = new String(currentPwd.getPassword());
        String newPass  = new String(newPwd.getPassword());
        String confirm  = new String(confirmPwd.getPassword());

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ All fields are required.");
            return;
        }
        if (!newPass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "❌ New passwords do not match.");
            return;
        }

        String strengthError = PasswordUtil.validatePasswordStrength(newPass);
        if (strengthError != null) {
            JOptionPane.showMessageDialog(this, "❌ " + strengthError);
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            // Verify current
            String verifySql = "SELECT password FROM members WHERE id=?";
            PreparedStatement vs = conn.prepareStatement(verifySql);
            vs.setInt(1, memberId);
            ResultSet rs = vs.executeQuery();
            if (!rs.next()) { JOptionPane.showMessageDialog(this, "❌ Member not found."); return; }

            String stored = rs.getString("password");
            boolean correct = (stored != null && stored.contains(":"))
                    ? PasswordUtil.verifyPassword(current, stored)
                    : current.equals(stored);

            if (!correct) {
                JOptionPane.showMessageDialog(this, "❌ Current password is incorrect.");
                return;
            }

            String hashed = PasswordUtil.hashPassword(newPass);
            String updateSql = "UPDATE members SET password=? WHERE id=?";
            PreparedStatement us = conn.prepareStatement(updateSql);
            us.setString(1, hashed);
            us.setInt(2, memberId);
            us.executeUpdate();

            JOptionPane.showMessageDialog(this, "✅ Password changed successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private void resolveCurrentMemberId() {
        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM members WHERE name=?");
            stmt.setString(1, studentName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) memberId = rs.getInt("id");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void ensureProfileDirExists() {
        try { Files.createDirectories(Paths.get(PROFILE_DIR)); }
        catch (Exception ignored) {}
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? "jpg" : filename.substring(dot + 1).toLowerCase();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String labelText, JComponent field) {
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void addHint(JPanel panel, GridBagConstraints gbc, int row, String hint) {
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(hint);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lbl.setForeground(Color.GRAY);
        panel.add(lbl, gbc);
    }

    private JPanel statRow(String labelText, JLabel valueLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Color.GRAY);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        p.add(lbl);
        p.add(valueLabel);
        return p;
    }
}