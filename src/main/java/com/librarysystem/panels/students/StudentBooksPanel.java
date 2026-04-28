package com.librarysystem.panels.students;

import com.librarysystem.db.DBHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentBooksPanel — Card-based book browser for students.
 *
 * Layout:
 *   ┌────────────────────────────────────────────────────┐
 *   │  Search bar + Category filter buttons (top)        │
 *   ├────────────────────────────────────────────────────┤
 *   │  Scrollable grid of BookCard panels                │
 *   │  Each card: Title, Author, Category badge,         │
 *   │             Available copies, [Request Borrow] btn │
 *   └────────────────────────────────────────────────────┘
 *
 * Borrow-request flow:
 *   1. Student clicks "Request Borrow" on a card.
 *   2. Check for duplicate PENDING request → warn and stop.
 *   3. Check if student already has this book borrowed → warn and stop.
 *   4. Confirm dialog.
 *   5. INSERT into borrow_requests (status=PENDING).
 *   6. Success message.
 */
public class StudentBooksPanel extends JPanel {

    private final String studentName;

    // State
    private List<BookData> allBooks = new ArrayList<>();
    private String activeCategory = "All";
    private String searchKeyword  = "";

    // UI refs
    private JPanel cardsContainer;
    private JTextField searchField;
    private JPanel categoryButtonRow;
    private JLabel statusLabel;

    // ── Colour palette for category badges ───────────────────────────────────
    private static final Color[] BADGE_COLORS = {
            new Color(52,  152, 219),   // blue
            new Color(46,  204, 113),   // green
            new Color(231, 76,  60),    // red
            new Color(155, 89,  182),   // purple
            new Color(241, 196, 15),    // yellow
            new Color(26,  188, 156),   // teal
            new Color(230, 126, 34),    // orange
            new Color(149, 165, 166),   // grey
    };
    private java.util.Map<String, Color> categoryColorMap = new java.util.HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public StudentBooksPanel(String studentName) {
        this.studentName = studentName;
        setLayout(new BorderLayout(0, 0));

        add(buildTopPanel(), BorderLayout.NORTH);

        // Cards scroll area
        cardsContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        cardsContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(cardsContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(new EmptyBorder(4, 12, 6, 12));
        add(statusLabel, BorderLayout.SOUTH);

        loadData();
    }

    // ── Top panel: search + categories ───────────────────────────────────────
    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout(0, 0));
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        // Row 1: title + search
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setBorder(new EmptyBorder(10, 12, 6, 12));

        JLabel title = new JLabel("📚 Browse Books");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by title or author…");

        JButton searchBtn = new JButton("🔍");
        searchBtn.setToolTipText("Search");
        searchBtn.addActionListener(e -> {
            searchKeyword = searchField.getText().trim().toLowerCase();
            renderCards();
        });
        // Search on Enter
        searchField.addActionListener(e -> {
            searchKeyword = searchField.getText().trim().toLowerCase();
            renderCards();
        });
        JButton clearBtn = new JButton("✕");
        clearBtn.setToolTipText("Clear search");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            searchKeyword = "";
            renderCards();
        });

        searchBox.add(searchField, BorderLayout.CENTER);
        JPanel btnGroup = new JPanel(new GridLayout(1, 2, 2, 0));
        btnGroup.add(searchBtn);
        btnGroup.add(clearBtn);
        searchBox.add(btnGroup, BorderLayout.EAST);

        searchRow.add(title, BorderLayout.WEST);
        searchRow.add(searchBox, BorderLayout.CENTER);

        // Row 2: category filter buttons (populated after DB load)
        categoryButtonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        categoryButtonRow.setBorder(new EmptyBorder(0, 6, 4, 6));

        top.add(searchRow,      BorderLayout.NORTH);
        top.add(categoryButtonRow, BorderLayout.SOUTH);

        return top;
    }

    // ── Load everything from DB ───────────────────────────────────────────────
    private void loadData() {
        allBooks.clear();
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        categories.add("All");

        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT b.id, b.title, b.author, b.publish_year, " +
                    "b.available_quantity, c.name AS category " +
                    "FROM books b " +
                    "LEFT JOIN categories c ON b.category_id = c.id " +
                    "ORDER BY b.title ASC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            int colorIdx = 0;
            while (rs.next()) {
                String cat = rs.getString("category");
                if (cat == null) cat = "Uncategorized";
                BookData bd = new BookData(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        cat,
                        rs.getInt("publish_year"),
                        rs.getInt("available_quantity")
                );
                allBooks.add(bd);
                if (categories.add(cat)) {
                    categoryColorMap.put(cat,
                            BADGE_COLORS[colorIdx % BADGE_COLORS.length]);
                    colorIdx++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading books: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        buildCategoryButtons(new ArrayList<>(categories));
        renderCards();
    }

    // ── Category toggle buttons ───────────────────────────────────────────────
    private void buildCategoryButtons(List<String> categories) {
        categoryButtonRow.removeAll();
        ButtonGroup bg = new ButtonGroup();

        for (String cat : categories) {
            JToggleButton btn = new JToggleButton(cat);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if ("All".equals(cat)) {
                btn.setSelected(true);
                btn.setBackground(new Color(52, 73, 94));
                btn.setForeground(Color.WHITE);
            } else {
                Color c = categoryColorMap.getOrDefault(cat, Color.GRAY);
                btn.setBackground(c.brighter());
                btn.setForeground(Color.DARK_GRAY);
            }

            btn.addActionListener(e -> {
                activeCategory = cat;
                renderCards();
            });

            bg.add(btn);
            categoryButtonRow.add(btn);
        }

        categoryButtonRow.revalidate();
        categoryButtonRow.repaint();
    }

    // ── Filter + render cards ─────────────────────────────────────────────────
    private void renderCards() {
        cardsContainer.removeAll();

        List<BookData> filtered = new ArrayList<>();
        for (BookData bd : allBooks) {
            boolean catMatch = "All".equals(activeCategory)
                    || activeCategory.equals(bd.category);
            boolean kwMatch  = searchKeyword.isEmpty()
                    || bd.title.toLowerCase().contains(searchKeyword)
                    || bd.author.toLowerCase().contains(searchKeyword);
            if (catMatch && kwMatch) filtered.add(bd);
        }

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("😕 No books found matching your search.");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            empty.setForeground(Color.GRAY);
            empty.setBorder(new EmptyBorder(40, 40, 40, 40));
            cardsContainer.add(empty);
        } else {
            for (BookData bd : filtered) {
                cardsContainer.add(new BookCard(bd));
            }
        }

        int available = (int) filtered.stream()
                .filter(b -> b.availableCopies > 0).count();
        statusLabel.setText(filtered.size() + " book(s) shown  |  "
                + available + " available for borrowing");

        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    // ── BookCard component ────────────────────────────────────────────────────
    private class BookCard extends JPanel {
        BookCard(BookData bd) {
            setLayout(new BorderLayout(0, 6));
            setPreferredSize(new Dimension(230, 220));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(12, 14, 12, 14)));
            setBackground(Color.WHITE);

            // ── Top: category badge ──────────────────────────────
            Color badgeColor = categoryColorMap.getOrDefault(bd.category,
                    new Color(149, 165, 166));
            JLabel badge = new JLabel(bd.category, SwingConstants.CENTER);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            badge.setForeground(Color.WHITE);
            badge.setBackground(badgeColor);
            badge.setOpaque(true);
            badge.setBorder(new EmptyBorder(2, 8, 2, 8));

            JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            badgeRow.setOpaque(false);
            badgeRow.add(badge);

            // Availability indicator dot
            JLabel availDot = new JLabel(bd.availableCopies > 0 ? "●" : "●");
            availDot.setForeground(bd.availableCopies > 0
                    ? new Color(46, 204, 113) : new Color(231, 76, 60));
            availDot.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            availDot.setToolTipText(bd.availableCopies > 0 ? "Available" : "Not available");
            badgeRow.add(Box.createHorizontalStrut(6));
            badgeRow.add(availDot);

            add(badgeRow, BorderLayout.NORTH);

            // ── Centre: text info ────────────────────────────────
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setBorder(new EmptyBorder(6, 0, 6, 0));

            JLabel titleLbl = new JLabel("<html><b>" + bd.title + "</b></html>");
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLbl.setForeground(new Color(30, 30, 30));

            JLabel authorLbl = new JLabel("by " + bd.author);
            authorLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            authorLbl.setForeground(Color.GRAY);

            JLabel yearLbl = new JLabel("Year: " + bd.publishYear);
            yearLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            yearLbl.setForeground(Color.GRAY);

            JLabel copiesLbl = new JLabel(
                    bd.availableCopies > 0
                            ? bd.availableCopies + " cop" + (bd.availableCopies == 1 ? "y" : "ies") + " available"
                            : "No copies available");
            copiesLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            copiesLbl.setForeground(bd.availableCopies > 0
                    ? new Color(39, 174, 96) : new Color(192, 57, 43));

            infoPanel.add(titleLbl);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(authorLbl);
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(yearLbl);
            infoPanel.add(Box.createVerticalStrut(6));
            infoPanel.add(copiesLbl);

            add(infoPanel, BorderLayout.CENTER);

            // ── Bottom: request button ───────────────────────────
            JButton requestBtn = new JButton("📋 Request Borrow");
            requestBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            requestBtn.setFocusPainted(false);
            requestBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (bd.availableCopies > 0) {
                requestBtn.setBackground(new Color(52, 152, 219));
                requestBtn.setForeground(Color.WHITE);
                requestBtn.setOpaque(true);
                requestBtn.setToolTipText("Send a borrow request to the librarian");
                requestBtn.addActionListener(e -> handleBorrowRequest(bd));
            } else {
                requestBtn.setText("❌ Unavailable");
                requestBtn.setEnabled(false);
                requestBtn.setBackground(new Color(189, 195, 199));
                requestBtn.setForeground(Color.DARK_GRAY);
                requestBtn.setOpaque(true);
                requestBtn.setToolTipText("No copies available right now");
            }

            add(requestBtn, BorderLayout.SOUTH);

            // Hover effect
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(245, 248, 255));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true),
                            new EmptyBorder(12, 14, 12, 14)));
                }
                @Override public void mouseExited(MouseEvent e) {
                    setBackground(Color.WHITE);
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                            new EmptyBorder(12, 14, 12, 14)));
                }
            });
        }
    }

    // ── Borrow request logic ──────────────────────────────────────────────────
    private void handleBorrowRequest(BookData bd) {
        try (Connection conn = DBHelper.getConnection()) {

            // Resolve member ID
            int memberId = getMemberId(conn, studentName);
            if (memberId == -1) {
                JOptionPane.showMessageDialog(this,
                        "❌ Could not find your member record. Please contact the librarian.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Guard 1: Already has this book borrowed?
            String borrowedSql =
                    "SELECT COUNT(*) FROM borrowed_books " +
                            "WHERE member_id=? AND book_id=? AND status='BORROWED'";
            PreparedStatement borrowedStmt = conn.prepareStatement(borrowedSql);
            borrowedStmt.setInt(1, memberId);
            borrowedStmt.setInt(2, bd.bookId);
            ResultSet borrowedRs = borrowedStmt.executeQuery();
            borrowedRs.next();
            if (borrowedRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "⚠️ You already have \"" + bd.title + "\" borrowed.\n" +
                                "Please return it before requesting again.",
                        "Already Borrowed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Guard 2: Already has a PENDING request for this book?
            String pendingSql =
                    "SELECT COUNT(*) FROM borrow_requests " +
                            "WHERE member_id=? AND book_id=? AND status='PENDING'";
            PreparedStatement pendingStmt = conn.prepareStatement(pendingSql);
            pendingStmt.setInt(1, memberId);
            pendingStmt.setInt(2, bd.bookId);
            ResultSet pendingRs = pendingStmt.executeQuery();
            pendingRs.next();
            if (pendingRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "⏳ You already have a pending request for \"" + bd.title + "\".\n" +
                                "Check the 'My Requests' tab for the status.",
                        "Duplicate Request", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Confirm
            int confirm = JOptionPane.showConfirmDialog(this,
                    "<html><b>Request to borrow this book?</b><br><br>"
                            + "<b>Title:</b> " + bd.title + "<br>"
                            + "<b>Author:</b> " + bd.author + "<br><br>"
                            + "A librarian will review and confirm your request.</html>",
                    "Confirm Borrow Request",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) return;

            // Insert request
            String insertSql =
                    "INSERT INTO borrow_requests (member_id, book_id, status) VALUES (?, ?, 'PENDING')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, memberId);
            insertStmt.setInt(2, bd.bookId);
            insertStmt.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "✅ Borrow request submitted!\n\n" +
                            "\"" + bd.title + "\" has been requested.\n" +
                            "Check 'My Borrowed Books' → 'My Requests' tab to track the status.",
                    "Request Submitted",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error submitting request: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    private int getMemberId(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM members WHERE name=?");
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }

    // ── BookData record ───────────────────────────────────────────────────────
    private static class BookData {
        final int    bookId;
        final String title;
        final String author;
        final String category;
        final int    publishYear;
        final int    availableCopies;

        BookData(int bookId, String title, String author, String category,
                 int publishYear, int availableCopies) {
            this.bookId         = bookId;
            this.title          = title;
            this.author         = author;
            this.category       = category;
            this.publishYear    = publishYear;
            this.availableCopies = availableCopies;
        }
    }

    /**
     * WrapLayout — FlowLayout that wraps to next line instead of clipping.
     * Drop this inner class in or move it to a util package.
     */
    public static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width  = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth  += d.width + hgap;
                    rowHeight  = Math.max(rowHeight, d.height);
                }
                dim.width  = Math.max(dim.width, rowWidth);
                dim.height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }
}