package panels;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import db.DBHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class MembersPanel extends JPanel {
    private JTextField fnameField, lnameField, emailField, phoneField, addressField, searchField;
    private JComboBox<String> searchByCombo;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JTable membersTable;
    private DefaultTableModel tableModel;

    public MembersPanel() {
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // ==== FORM PANEL ====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("📝 Register Member"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // First Name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        fnameField = new JTextField(15);
        formPanel.add(fnameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lnameField = new JTextField(15);
        formPanel.add(lnameField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(15);
        formPanel.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        formPanel.add(phoneField, gbc);

        // Address
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        addressField = new JTextField(15);
        formPanel.add(addressField, gbc);

        // Add Button
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        addButton = new JButton("➕ Add Member");
        addButton.setToolTipText("Add a new member to the database");
        addButton.addActionListener(e -> handleAddMember());
        formPanel.add(addButton, gbc);

        // ==== SEARCH PANEL ====
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("🔍 Search Member"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5,5,5,5);
        sgbc.fill = GridBagConstraints.HORIZONTAL;

        sgbc.gridx = 0; sgbc.gridy = 0;
        searchPanel.add(new JLabel("Search by:"), sgbc);

        sgbc.gridx = 1;
        searchByCombo = new JComboBox<>(new String[]{"fname","lname","email","phone"});
        searchPanel.add(searchByCombo, sgbc);

        sgbc.gridx = 0; sgbc.gridy = 1; sgbc.gridwidth = 2;
        searchField = new JTextField(15);
        searchPanel.add(searchField, sgbc);

        sgbc.gridy = 2;
        searchButton = new JButton("🔍 Search");
        searchButton.setToolTipText("Search members by selected column");
        searchButton.addActionListener(e -> handleSearch());
        searchPanel.add(searchButton, sgbc);

        // Top container: form + search side by side
        JPanel topPanel = new JPanel(new GridLayout(1,2,10,10));
        topPanel.add(formPanel);
        topPanel.add(searchPanel);
        add(topPanel, BorderLayout.NORTH);

        // ==== TABLE SECTION ====
        tableModel = new DefaultTableModel(new String[]{
                "ID","First Name","Last Name","Email","Phone","Address","Member Since"},0){
            @Override
            public boolean isCellEditable(int row,int col){ return false; }
        };
        membersTable = new JTable(tableModel);

        // Set column widths
        membersTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        membersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        membersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        membersTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        membersTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        membersTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        membersTable.getColumnModel().getColumn(6).setPreferredWidth(100);

        add(new JScrollPane(membersTable), BorderLayout.CENTER);

        // ==== FOOTER BUTTONS CENTERED ====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));

        editButton = new JButton("✏️ Edit Selected");
        editButton.setToolTipText("Edit selected member");
        deleteButton = new JButton("🗑️ Delete Selected");
        deleteButton.setToolTipText("Delete selected member");

        editButton.addActionListener(e -> handleEdit());
        deleteButton.addActionListener(e -> handleDelete());

        JButton viewHistoryButton = new JButton("📚 View Borrowing History");
        viewHistoryButton.setToolTipText("View member's borrowing history");
        viewHistoryButton.addActionListener(e -> viewBorrowingHistory());

        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(viewHistoryButton);

        JButton exportCSVButton = new JButton("📄 Export CSV");
        exportCSVButton.setToolTipText("Export members to CSV");
        exportCSVButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)
                exportToCSV(chooser.getSelectedFile());
        });

        JButton exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.setToolTipText("Export members to PDF");
        exportPDFButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)
                exportToPDF(chooser.getSelectedFile());
        });

        bottomPanel.add(exportCSVButton);
        bottomPanel.add(exportPDFButton);

        add(bottomPanel, BorderLayout.SOUTH);

        loadMembersFromDatabase();
    }

    // ===== Add Member =====
    private void handleAddMember(){
        String fname = fnameField.getText().trim();
        String lname = lnameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if(fname.isEmpty() || lname.isEmpty() || email.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this,"Please fill all required fields (First Name, Last Name, Email, Phone).");
            return;
        }

        if (!phone.matches("^(\\+260|0)?[0-9]{9,10}$")) {   // Zambian format
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.");
            return;
        }

        if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            JOptionPane.showMessageDialog(this, "Invalid email format.");
            return;
        }

        try(Connection conn = DBHelper.getConnection()){
            // Check if email already exists
            String checkSql = "SELECT COUNT(*) FROM members WHERE email=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Email already exists!");
                return;
            }

            String sql = "INSERT INTO members (fname, lname, email, phone, address) VALUES (?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, fname);
            stmt.setString(2, lname);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address.isEmpty() ? null : address);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Member added successfully.");
            clearForm();
            loadMembersFromDatabase();
        }catch(SQLException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
        }
    }

    // ===== Load Members =====
    private void loadMembersFromDatabase(){
        tableModel.setRowCount(0);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "SELECT id, fname, lname, email, phone, address, membership_date " +
                    "FROM members WHERE is_active=TRUE ORDER BY fname, lname";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("fname"),
                        rs.getString("lname"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getDate("membership_date")
                });
            }
        }catch(SQLException e){ e.printStackTrace(); }
    }

    // ===== Search =====
    private void handleSearch(){
        String keyword = searchField.getText().trim().toLowerCase();
        String column = searchByCombo.getSelectedItem().toString();
        if(keyword.isEmpty()){ loadMembersFromDatabase(); return; }

        try(Connection conn = DBHelper.getConnection()){
            String sql = "SELECT * FROM members WHERE LOWER("+column+") LIKE ? AND is_active=TRUE ORDER BY fname, lname";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%"+keyword+"%");
            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0);
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("fname"),
                        rs.getString("lname"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getDate("membership_date")
                });
            }
        }catch(SQLException e){ e.printStackTrace(); }
    }

    // ===== Edit =====
    private void handleEdit(){
        int row = membersTable.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a member to edit.");
            return;
        }

        int id = (int)tableModel.getValueAt(row, 0);
        String currentFname = (String)tableModel.getValueAt(row, 1);
        String currentLname = (String)tableModel.getValueAt(row, 2);
        String currentEmail = (String)tableModel.getValueAt(row, 3);
        String currentPhone = (String)tableModel.getValueAt(row, 4);
        String currentAddress = (String)tableModel.getValueAt(row, 5);

        String newFname = JOptionPane.showInputDialog(this, "New First Name:", currentFname);
        if(newFname == null || newFname.trim().isEmpty()) return;

        String newLname = JOptionPane.showInputDialog(this, "New Last Name:", currentLname);
        if(newLname == null || newLname.trim().isEmpty()) return;

        String newEmail = JOptionPane.showInputDialog(this, "New Email:", currentEmail);
        if(newEmail == null || !newEmail.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")){
            JOptionPane.showMessageDialog(this,"Invalid email.");
            return;
        }

        String newPhone = JOptionPane.showInputDialog(this, "New Phone:", currentPhone);
        if(newPhone == null || !newPhone.matches("\\d{10}")){
            JOptionPane.showMessageDialog(this,"Phone must be exactly 10 digits.");
            return;
        }

        String newAddress = JOptionPane.showInputDialog(this, "New Address:", currentAddress);
        if(newAddress == null) return;

        try(Connection conn = DBHelper.getConnection()){
            // Check if new email is already used by another member
            String checkSql = "SELECT COUNT(*) FROM members WHERE email=? AND id!=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, newEmail);
            checkStmt.setInt(2, id);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "❌ Email already exists!");
                return;
            }

            String sql = "UPDATE members SET fname=?, lname=?, email=?, phone=?, address=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newFname);
            stmt.setString(2, newLname);
            stmt.setString(3, newEmail);
            stmt.setString(4, newPhone);
            stmt.setString(5, newAddress.trim().isEmpty() ? null : newAddress);
            stmt.setInt(6, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Member updated successfully.");
            loadMembersFromDatabase();
        }catch(SQLException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
        }
    }

    // ===== Delete =====
    private void handleDelete(){
        int row = membersTable.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Select a member to delete.");
            return;
        }

        int id = (int)tableModel.getValueAt(row, 0);
        String name = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete member: " + name + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if(confirm != JOptionPane.YES_OPTION) return;

        try(Connection conn = DBHelper.getConnection()){
            // Soft delete - set is_active to FALSE
            String sql = "UPDATE members SET is_active=FALSE WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Member deactivated.");
            loadMembersFromDatabase();
        }catch(SQLException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());
        }
    }

    // ===== View Borrowing History =====
    private void viewBorrowingHistory() {
        int row = membersTable.getSelectedRow();
        if(row == -1) {
            JOptionPane.showMessageDialog(this, "Select a member to view history!");
            return;
        }

        int memberId = (int)tableModel.getValueAt(row, 0);
        String memberName = tableModel.getValueAt(row, 1) + " " + tableModel.getValueAt(row, 2);

        try(Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.title, b.author, bb.borrow_date, bb.due_date, bb.return_date, bb.status " +
                    "FROM borrowed_books bb " +
                    "JOIN books b ON bb.book_id = b.id " +
                    "WHERE bb.member_id = ? " +
                    "ORDER BY bb.borrow_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder history = new StringBuilder();
            history.append("═══════════════════════════════════\n");
            history.append("  BORROWING HISTORY: ").append(memberName).append("\n");
            history.append("═══════════════════════════════════\n\n");

            boolean hasHistory = false;
            while(rs.next()) {
                hasHistory = true;
                history.append("📚 ").append(rs.getString("title")).append("\n");
                history.append("   Author: ").append(rs.getString("author")).append("\n");
                history.append("   Borrowed: ").append(rs.getDate("borrow_date")).append("\n");
                history.append("   Due: ").append(rs.getDate("due_date")).append("\n");
                Date returnDate = rs.getDate("return_date");
                history.append("   Returned: ").append(returnDate != null ? returnDate : "Not yet").append("\n");
                history.append("   Status: ").append(rs.getString("status")).append("\n");
                history.append("───────────────────────────────────\n");
            }

            if(!hasHistory) {
                history.append("No borrowing history found.\n");
            }

            JTextArea textArea = new JTextArea(history.toString());
            textArea.setEditable(false);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(this, scrollPane,
                    "Borrowing History", JOptionPane.INFORMATION_MESSAGE);

        } catch(SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // ===== Export CSV =====
    private void exportToCSV(File file){
        try(PrintWriter pw = new PrintWriter(file)){
            for(int i=0; i<tableModel.getColumnCount(); i++){
                pw.print(tableModel.getColumnName(i));
                if(i < tableModel.getColumnCount()-1) pw.print(",");
            }
            pw.println();
            for(int row=0; row<tableModel.getRowCount(); row++){
                for(int col=0; col<tableModel.getColumnCount(); col++){
                    Object value = tableModel.getValueAt(row, col);
                    pw.print(value != null ? value : "");
                    if(col < tableModel.getColumnCount()-1) pw.print(",");
                }
                pw.println();
            }
            JOptionPane.showMessageDialog(this,"✅ Exported to CSV successfully!");
        }catch(IOException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    // ===== Export PDF =====
    private void exportToPDF(File file) {
        try {
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            document.add(new Paragraph("Members List\n\n"));

            PdfPTable pdfTable = new PdfPTable(tableModel.getColumnCount());
            float[] columnWidths = {1f, 2f, 2f, 3f, 2f, 3f, 2f};
            pdfTable.setWidths(columnWidths);
            pdfTable.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                PdfPCell headerCell = new PdfPCell(new Phrase(tableModel.getColumnName(i), headerFont));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                pdfTable.addCell(headerCell);
            }

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object value = tableModel.getValueAt(row, col);
                    pdfTable.addCell(value != null ? value.toString() : "");
                }
            }

            document.add(pdfTable);
            document.close();
            JOptionPane.showMessageDialog(this, "✅ Exported to PDF successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ===== Clear Form =====
    private void clearForm(){
        fnameField.setText("");
        lnameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressField.setText("");
    }
}