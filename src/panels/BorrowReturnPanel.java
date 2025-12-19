package panels;

import db.DBHelper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.ArrayList;

public class BorrowReturnPanel extends JPanel {
    private JTextField memberField, bookField, borrowDateField, returnDateField;
    private JButton borrowButton, returnButton, exportPDFButton;
    private JTable table;
    private DefaultTableModel model;

    public BorrowReturnPanel() {
        setLayout(new BorderLayout());

        // ===== Form Panel =====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Borrow / Return Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Member Field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Member Name:"), gbc);
        gbc.gridx = 1;
        memberField = new JTextField(15);
        formPanel.add(memberField, gbc);
        setupMemberAutocomplete();

        // Book Field
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Book (Title / Author / Edition):"), gbc);
        gbc.gridx = 1;
        bookField = new JTextField(15);
        formPanel.add(bookField, gbc);
        setupBookAutocomplete();

        // Borrow Date
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Borrow Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        borrowDateField = new JTextField(10);
        borrowDateField.setText(java.time.LocalDate.now().toString());
        formPanel.add(borrowDateField, gbc);

        // Return Date
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Return Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        returnDateField = new JTextField(10);
        formPanel.add(returnDateField, gbc);

        // Borrow Button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        borrowButton = new JButton("Borrow");
        borrowButton.addActionListener(e -> handleBorrow());
        formPanel.add(borrowButton, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ===== Table =====
        model = new DefaultTableModel(new String[]{
                "ID", "Member", "Book", "Edition", "Borrow Date", "Return Date", "Status"}, 0){
            @Override public boolean isCellEditable(int row,int col){ return false; }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===== Bottom Buttons =====
        JPanel bottomPanel = new JPanel();
        returnButton = new JButton("Mark as Returned");
        returnButton.addActionListener(e -> handleReturn());
        exportPDFButton = new JButton("📄 Export PDF");
        exportPDFButton.addActionListener(e -> exportToPDF());
        bottomPanel.add(returnButton);
        bottomPanel.add(exportPDFButton);
        add(bottomPanel, BorderLayout.SOUTH);

        loadBorrowRecords();
    }

    // ===== Load Borrow Records =====
    private void loadBorrowRecords() {
        model.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT * FROM borrow_records ORDER BY id DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int sn = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                        sn++,
                        rs.getString("member_name"),
                        rs.getString("book_title"),
                        rs.getString("edition"),
                        rs.getDate("borrow_date"),
                        rs.getDate("return_date"),
                        rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error loading records: "+ex.getMessage());
        }
    }

    // ===== Borrow Book =====
    private void handleBorrow() {
        String member = memberField.getText().trim();
        String bookTitle = bookField.getText().trim();
        String borrowDate = borrowDateField.getText().trim();
        String returnDate = returnDateField.getText().trim();

        if(member.isEmpty() || bookTitle.isEmpty() || borrowDate.isEmpty() || returnDate.isEmpty()) {
            JOptionPane.showMessageDialog(this,"Fill all fields!");
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            // Check member exists
            PreparedStatement mStmt = conn.prepareStatement("SELECT COUNT(*) FROM members WHERE name=?");
            mStmt.setString(1, member);
            ResultSet rs = mStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) == 0){
                JOptionPane.showMessageDialog(this,"Member does not exist!");
                return;
            }

            // Check book exists and available for that edition
            PreparedStatement bStmt = conn.prepareStatement(
                    "SELECT isbn, total_quantity, borrowed, edition FROM books WHERE title=? AND edition=?");
            // Here user must type in "Title Edition" or pick via autocomplete
            String[] parts = bookTitle.split("\\s+"); // crude split
            String edition = parts[parts.length-1]; // assume last part is edition
            String title = bookTitle.substring(0, bookTitle.length()-edition.length()).trim();
            bStmt.setString(1, title);
            bStmt.setString(2, edition);
            rs = bStmt.executeQuery();
            if(!rs.next()){
                JOptionPane.showMessageDialog(this,"Book with that edition not found!");
                return;
            }
            int total = rs.getInt("total_quantity");
            int borrowed = rs.getInt("borrowed");
            String isbn = rs.getString("isbn");
            if(borrowed >= total){
                JOptionPane.showMessageDialog(this,"No available copies of this edition!");
                return;
            }

            // Insert borrow record
            PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO borrow_records (member_name, book_title, edition, borrow_date, return_date, status) VALUES (?,?,?,?,?,?)");
            insertStmt.setString(1, member);
            insertStmt.setString(2, title);
            insertStmt.setString(3, edition);
            insertStmt.setDate(4, java.sql.Date.valueOf(borrowDate));
            insertStmt.setDate(5, java.sql.Date.valueOf(returnDate));
            insertStmt.setString(6, "Borrowed");
            insertStmt.executeUpdate();

            // Update borrowed count
            PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE books SET borrowed=borrowed+1 WHERE isbn=?");
            updateStmt.setString(1, isbn);
            updateStmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"Book borrowed successfully!");
            loadBorrowRecords();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error borrowing book: "+ex.getMessage());
        }
    }

    // ===== Return Book =====
    private void handleReturn() {
        int row = table.getSelectedRow();
        if(row==-1){ JOptionPane.showMessageDialog(this,"Select a record"); return; }

        String member = (String) model.getValueAt(row,1);
        String bookTitle = (String) model.getValueAt(row,2);
        String edition = (String) model.getValueAt(row,3);

        try(Connection conn = DBHelper.getConnection()){
            // Update borrow record
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE borrow_records SET status='Returned' WHERE member_name=? AND book_title=? AND edition=? AND status='Borrowed'");
            stmt.setString(1, member);
            stmt.setString(2, bookTitle);
            stmt.setString(3, edition);
            stmt.executeUpdate();

            // Update books table
            PreparedStatement uStmt = conn.prepareStatement(
                    "UPDATE books SET borrowed=borrowed-1 WHERE title=? AND edition=?");
            uStmt.setString(1, bookTitle);
            uStmt.setString(2, edition);
            uStmt.executeUpdate();

            JOptionPane.showMessageDialog(this,"Book marked as returned!");
            loadBorrowRecords();
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    // ===== Export PDF =====
    private void exportToPDF() {
        JFileChooser chooser = new JFileChooser();
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc,new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("Borrow Records\n\n"));
            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            for(int i=0;i<model.getColumnCount();i++){
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            }
            for(int row=0; row<model.getRowCount(); row++){
                for(int col=0; col<model.getColumnCount(); col++){
                    pdfTable.addCell(model.getValueAt(row,col).toString());
                }
            }
            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this,"Exported to PDF successfully!");
        } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage()); }
    }

    // ===== Autocomplete Member Names =====
    private void setupMemberAutocomplete() {
        memberField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { showSuggestions(); }
            public void removeUpdate(DocumentEvent e) { showSuggestions(); }
            public void changedUpdate(DocumentEvent e) { showSuggestions(); }
            private void showSuggestions(){
                String text = memberField.getText().trim();
                if(text.isEmpty()) return;
                try(Connection conn = DBHelper.getConnection()){
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT name FROM members WHERE name LIKE ? LIMIT 5");
                    stmt.setString(1,"%"+text+"%");
                    ResultSet rs = stmt.executeQuery();
                    ArrayList<String> suggestions = new ArrayList<>();
                    while(rs.next()) suggestions.add(rs.getString("name"));
                    if(!suggestions.isEmpty()){
                        JPopupMenu menu = new JPopupMenu();
                        for(String s : suggestions){
                            JMenuItem item = new JMenuItem(s);
                            item.addActionListener(ae -> memberField.setText(s));
                            menu.add(item);
                        }
                        menu.show(memberField,0,memberField.getHeight());
                    }
                }catch(Exception ex){ ex.printStackTrace(); }
            }
        });
    }

    // ===== Autocomplete Books =====
    private void setupBookAutocomplete() {
        bookField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { showSuggestions(); }
            public void removeUpdate(DocumentEvent e) { showSuggestions(); }
            public void changedUpdate(DocumentEvent e) { showSuggestions(); }
            private void showSuggestions(){
                String text = bookField.getText().trim();
                if(text.isEmpty()) return;
                try(Connection conn = DBHelper.getConnection()){
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT CONCAT(title,' ',edition) as titleedition FROM books WHERE (title LIKE ? OR author LIKE ?) AND total_quantity>borrowed LIMIT 5");
                    stmt.setString(1,"%"+text+"%");
                    stmt.setString(2,"%"+text+"%");
                    ResultSet rs = stmt.executeQuery();
                    ArrayList<String> suggestions = new ArrayList<>();
                    while(rs.next()) suggestions.add(rs.getString("titleedition"));
                    if(!suggestions.isEmpty()){
                        JPopupMenu menu = new JPopupMenu();
                        for(String s : suggestions){
                            JMenuItem item = new JMenuItem(s);
                            item.addActionListener(ae -> bookField.setText(s));
                            menu.add(item);
                        }
                        menu.show(bookField,0,bookField.getHeight());
                    }
                }catch(Exception ex){ ex.printStackTrace(); }
            }
        });
    }
}
