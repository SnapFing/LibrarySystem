package panels;

import db.DBHelper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.ArrayList;

public class BooksPanel extends JPanel {
    private JTextField titleField, authorField, editionField, searchField;
    private JSpinner quantitySpinner;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton, exportPDFButton;

    public BooksPanel() {
        setLayout(new BorderLayout());

        // ==== FORM PANEL ====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add / Edit Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(15);
        formPanel.add(titleField, gbc);

        // Author
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(15);
        formPanel.add(authorField, gbc);

        // Edition
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Edition:"), gbc);
        gbc.gridx = 1;
        editionField = new JTextField(10);
        formPanel.add(editionField, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        formPanel.add(quantitySpinner, gbc);

        // Add Button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        addButton = new JButton("➕ Add Book");
        addButton.addActionListener(e -> handleAddBook());
        formPanel.add(addButton, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ==== SEARCH PANEL ====
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        searchButton = new JButton("🔍 Search");
        exportPDFButton = new JButton("📄 Export PDF");
        searchPanel.add(new JLabel("Search Book by Title/Author:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(exportPDFButton);

        add(searchPanel, BorderLayout.AFTER_LAST_LINE);

        searchButton.addActionListener(e -> handleSearch());
        exportPDFButton.addActionListener(e -> exportToPDF());

        // ==== TABLE ====
        tableModel = new DefaultTableModel(new String[]{
                "ID","Title","Edition","Author","ISBN","Total","Borrowed","Available"},0){
            @Override
            public boolean isCellEditable(int row, int col){ return false; }
        };
        booksTable = new JTable(tableModel);
        booksTable.getColumnModel().getColumn(0).setMaxWidth(40);
        add(new JScrollPane(booksTable), BorderLayout.CENTER);

        // ==== BUTTONS PANEL ====
        JPanel buttonPanel = new JPanel();
        editButton = new JButton("✏️ Edit");
        deleteButton = new JButton("🗑️ Delete");
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);

        editButton.addActionListener(e -> handleEdit());
        deleteButton.addActionListener(e -> handleDelete());

        loadBooksFromDatabase();
    }

    private void handleAddBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String edition = editionField.getText().trim();
        int quantity = (int) quantitySpinner.getValue();

        if(title.isEmpty() || author.isEmpty() || edition.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Fill all fields!");
            return;
        }

        try(Connection conn = DBHelper.getConnection()) {
            String isbn = title.replaceAll("\\s","") + "_" + edition + "_" + author.replaceAll("\\s","");

            String checkSql = "SELECT COUNT(*) FROM books WHERE isbn=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, isbn);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1)>0){
                JOptionPane.showMessageDialog(this,"Book already exists!");
                return;
            }

            String sql = "INSERT INTO books (title, edition, author, isbn, total_quantity, borrowed) VALUES (?,?,?,?,?,0)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, edition);
            stmt.setString(3, author);
            stmt.setString(4, isbn);
            stmt.setInt(5, quantity);
            stmt.executeUpdate();

            loadBooksFromDatabase();
            clearForm();
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
        }
    }

    private void loadBooksFromDatabase(){
        tableModel.setRowCount(0);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "SELECT * FROM books ORDER BY title ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int sn=1;
            while(rs.next()){
                int total = rs.getInt("total_quantity");
                int borrowed = rs.getInt("borrowed");
                tableModel.addRow(new Object[]{
                        sn++,
                        rs.getString("title"),
                        rs.getString("edition"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        total,
                        borrowed,
                        total-borrowed
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    private void handleSearch(){
        String keyword = searchField.getText().trim().toLowerCase();
        if(keyword.isEmpty()){ loadBooksFromDatabase(); return; }

        tableModel.setRowCount(0);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "SELECT * FROM books WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? ORDER BY title ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            int sn=1;
            while(rs.next()){
                int total = rs.getInt("total_quantity");
                int borrowed = rs.getInt("borrowed");
                tableModel.addRow(new Object[]{
                        sn++,
                        rs.getString("title"),
                        rs.getString("edition"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        total,
                        borrowed,
                        total-borrowed
                });
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    private void handleEdit(){
        int row = booksTable.getSelectedRow();
        if(row==-1){ JOptionPane.showMessageDialog(this,"Select a book"); return; }

        String title = JOptionPane.showInputDialog("New Title:", tableModel.getValueAt(row,1));
        String edition = JOptionPane.showInputDialog("New Edition:", tableModel.getValueAt(row,2));
        String author = JOptionPane.showInputDialog("New Author:", tableModel.getValueAt(row,3));
        if(title==null || edition==null || author==null) return;

        String isbn = (String) tableModel.getValueAt(row,4);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "UPDATE books SET title=?, edition=?, author=? WHERE isbn=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,title);
            stmt.setString(2,edition);
            stmt.setString(3,author);
            stmt.setString(4,isbn);
            stmt.executeUpdate();
            loadBooksFromDatabase();
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    private void handleDelete(){
        int row = booksTable.getSelectedRow();
        if(row==-1){ JOptionPane.showMessageDialog(this,"Select a book"); return; }
        String isbn = (String) tableModel.getValueAt(row,4);
        try(Connection conn = DBHelper.getConnection()){
            String sql = "DELETE FROM books WHERE isbn=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,isbn);
            stmt.executeUpdate();
            loadBooksFromDatabase();
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    private void clearForm(){
        titleField.setText("");
        authorField.setText("");
        editionField.setText("");
        quantitySpinner.setValue(1);
    }

    private void exportToPDF(){
        JFileChooser chooser = new JFileChooser();
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try{
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            PdfWriter.getInstance(doc,new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("Books List\n\n"));
            PdfPTable pdfTable = new PdfPTable(8);
            String[] headers = {"ID","Title","Edition","Author","ISBN","Total","Borrowed","Available"};
            for(String h: headers) pdfTable.addCell(h);
            for(int i=0;i<tableModel.getRowCount();i++){
                for(int j=0;j<8;j++){
                    pdfTable.addCell(String.valueOf(tableModel.getValueAt(i,j)));
                }
            }
            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this,"Exported to PDF!");
        } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Error exporting PDF"); }
    }
}
