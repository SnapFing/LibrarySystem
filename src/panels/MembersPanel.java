package panels;

import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
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
    private JTextField nameField, emailField, phoneField, searchField;
    private JComboBox<String> searchByCombo;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JTable membersTable;
    private DefaultTableModel tableModel;

    public MembersPanel() {
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // ==== FORM PANEL ====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Register Member"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(15);
        formPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        formPanel.add(phoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        addButton = new JButton("➕ Add Member");
        addButton.setToolTipText("Add a new member to the database");
        addButton.addActionListener(e -> handleAddMember());
        formPanel.add(addButton, gbc);

        // ==== SEARCH PANEL ====
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Member"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5,5,5,5);
        sgbc.fill = GridBagConstraints.HORIZONTAL;

        sgbc.gridx = 0; sgbc.gridy = 0;
        searchPanel.add(new JLabel("Search by:"), sgbc);

        sgbc.gridx = 1;
        searchByCombo = new JComboBox<>(new String[]{"name","phone"});
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
        tableModel = new DefaultTableModel(new String[]{"ID","Name","Email","Phone"},0){
            @Override
            public boolean isCellEditable(int row,int col){ return false; }
        };
        membersTable = new JTable(tableModel);
        add(new JScrollPane(membersTable), BorderLayout.CENTER);

        // Set column widths
        membersTable.getColumnModel().getColumn(0).setPreferredWidth(2);
        membersTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        membersTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        membersTable.getColumnModel().getColumn(3).setPreferredWidth(100);


        // ==== FOOTER BUTTONS CENTERED ====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));

        editButton = new JButton("✏️ Edit Selected");
        editButton.setToolTipText("Edit selected member");
        deleteButton = new JButton("🗑️ Delete Selected");
        deleteButton.setToolTipText("Delete selected member");

        editButton.addActionListener(e -> handleEdit());
        deleteButton.addActionListener(e -> handleDelete());

        bottomPanel.add(editButton);
        bottomPanel.add(deleteButton);

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

    // ===== Methods: Add, Load, Search, Edit, Delete, Export =====
    private void handleAddMember(){
        String name=nameField.getText().trim();
        String email=emailField.getText().trim();
        String phone=phoneField.getText().trim();

        if(name.isEmpty()||email.isEmpty()||phone.isEmpty()){
            JOptionPane.showMessageDialog(this,"Please fill all fields.");
            return;
        }

        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.");
            return;
        }


        try(Connection conn=DBHelper.getConnection()){
            String sql="INSERT INTO members (fname,lname,email,phone) VALUES (?,?,?)";
            PreparedStatement stmt=conn.prepareStatement(sql);
            stmt.setString(1,fname);
            stmt.setString(1,lname);
            stmt.setString(2,email);
            stmt.setString(3,phone);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Member added.");
            clearForm();
            loadMembersFromDatabase();
        }catch(SQLException e){ e.printStackTrace(); JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); }
    }

    private void loadMembersFromDatabase(){
        tableModel.setRowCount(0);
        try(Connection conn=DBHelper.getConnection()){
            String sql="SELECT id,fname,lname,email,phone FROM members";
            Statement stmt=conn.createStatement();
            ResultSet rs=stmt.executeQuery(sql);
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("fname"),
                        rs.getString("lname"),
                        rs.getString("email"),
                        rs.getString("phone")
                });
            }
        }catch(SQLException e){ e.printStackTrace(); }
    }

    private void handleSearch(){
        String keyword=searchField.getText().trim().toLowerCase();
        String column=searchByCombo.getSelectedItem().toString();
        if(keyword.isEmpty()){ loadMembersFromDatabase(); return; }

        try(Connection conn=DBHelper.getConnection()){
            String sql="SELECT * FROM members WHERE LOWER("+column+") LIKE ?";
            PreparedStatement stmt=conn.prepareStatement(sql);
            stmt.setString(1,"%"+keyword+"%");
            ResultSet rs=stmt.executeQuery();
            tableModel.setRowCount(0);
            while(rs.next()){
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")
                });
            }
        }catch(SQLException e){ e.printStackTrace(); }
    }

    private void handleEdit(){
        int row=membersTable.getSelectedRow();
        if(row==-1){ JOptionPane.showMessageDialog(this,"Select a member to edit."); return; }

        int id=(int)tableModel.getValueAt(row,0);
        String currentName=(String)tableModel.getValueAt(row,1);
        String currentEmail=(String)tableModel.getValueAt(row,2);
        String currentPhone=(String)tableModel.getValueAt(row,3);

        String newName=JOptionPane.showInputDialog(this,"New Name:",currentName);
        if(newName==null || newName.trim().isEmpty()){ JOptionPane.showMessageDialog(this,"Name cannot be empty."); return; }

        String newEmail=JOptionPane.showInputDialog(this,"New Email:",currentEmail);
        if(newEmail==null || !newEmail.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")){ JOptionPane.showMessageDialog(this,"Invalid email."); return; }

        String newPhone=JOptionPane.showInputDialog(this,"New Phone:",currentPhone);
        if(newPhone==null || !newPhone.matches("\\d{10}")){ JOptionPane.showMessageDialog(this,"Phone must be exactly 10 digits."); return; }

        try(Connection conn=DBHelper.getConnection()){
            String checkSql="SELECT COUNT(*) FROM members WHERE email=? AND id<>?";
            PreparedStatement checkStmt=conn.prepareStatement(checkSql);
            checkStmt.setString(1,newEmail);
            checkStmt.setInt(2,id);
            ResultSet rs=checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1)>0){ JOptionPane.showMessageDialog(this,"❌ Email already exists."); return; }

            String sql="UPDATE members SET name=?, email=?, phone=? WHERE id=?";
            PreparedStatement stmt=conn.prepareStatement(sql);
            stmt.setString(1,newName);
            stmt.setString(2,newEmail);
            stmt.setString(3,newPhone);
            stmt.setInt(4,id);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"✅ Member updated successfully.");
            loadMembersFromDatabase();
        }catch(SQLException e){ e.printStackTrace(); JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); }
    }

    private void handleDelete(){
        int row=membersTable.getSelectedRow();
        if(row==-1){ JOptionPane.showMessageDialog(this,"Select a member to delete."); return; }

        int id=(int)tableModel.getValueAt(row,0);
        int confirm=JOptionPane.showConfirmDialog(this,"Are you sure you want to delete this member?","Confirm",JOptionPane.YES_NO_OPTION);
        if(confirm!=JOptionPane.YES_OPTION) return;

        try(Connection conn=DBHelper.getConnection()){
            String sql="DELETE FROM members WHERE id=?";
            PreparedStatement stmt=conn.prepareStatement(sql);
            stmt.setInt(1,id);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this,"✅ Member deleted.");
            loadMembersFromDatabase();
        }catch(SQLException e){ e.printStackTrace(); JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); }
    }

    private void exportToCSV(File file){
        try(PrintWriter pw=new PrintWriter(file)){
            for(int i=0;i<tableModel.getColumnCount();i++){
                pw.print(tableModel.getColumnName(i));
                if(i<tableModel.getColumnCount()-1) pw.print(","); // CSV comma separator
            }
            pw.println();
            for(int row=0;row<tableModel.getRowCount();row++){
                for(int col=0;col<tableModel.getColumnCount();col++){
                    pw.print(tableModel.getValueAt(row,col));
                    if(col<tableModel.getColumnCount()-1) pw.print(",");
                }
                pw.println();
            }
            JOptionPane.showMessageDialog(this,"Exported to CSV successfully!");
        }catch(IOException ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
    }

    private void exportToPDF(File selectedFile) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Ensure .pdf extension
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                // Check if file already exists
                if (file.exists()) {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "The file \"" + file.getName() + "\" already exists.\nDo you want to replace it?",
                            "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (choice != JOptionPane.YES_OPTION) {
                        return; // user cancelled
                    }
                }

                // 👉 Start writing the PDF
                com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                document.add(new com.itextpdf.text.Paragraph("Members List\n\n"));

                com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(tableModel.getColumnCount());

                // Set custom widths (adjust to your table structure)
                float[] columnWidths = {1f, 3f, 4f, 2f};
                pdfTable.setWidths(columnWidths);
                pdfTable.setWidthPercentage(100);

                // Define header font (bold)
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

                // Add headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(tableModel.getColumnName(i), headerFont));
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setBorderWidth(1f);
                    pdfTable.addCell(headerCell);
                }

                // Add rows
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        pdfTable.addCell(String.valueOf(tableModel.getValueAt(row, col)));
                    }
                }

                document.add(pdfTable);
                document.close();

                JOptionPane.showMessageDialog(this, "Exported to PDF successfully!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }



    private void clearForm(){
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
    }
}
