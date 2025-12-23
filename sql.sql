-- ============================================
-- HYBRID LIBRARY MANAGEMENT SYSTEM DATABASE
-- Combines best practices with practical features
-- ============================================

-- Drop existing tables in correct order (foreign keys first)
DROP TABLE IF EXISTS fines;
DROP TABLE IF EXISTS borrowed_books;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS users;

-- ============================================
-- 1. USERS TABLE (Authentication)
-- ============================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin', 'Librarian') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username)
);

-- BCrypt hashes are 60 characters long
ALTER TABLE users MODIFY COLUMN password VARCHAR(60) NOT NULL;

-- ============================================
-- 2. MEMBERS TABLE (Library patrons)
-- Split name into fname/lname for better queries
-- ============================================
CREATE TABLE members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15) NOT NULL,
    address TEXT,
    membership_date DATE DEFAULT (CURRENT_DATE),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (fname, lname),
    INDEX idx_email (email),
    INDEX idx_phone (phone)
);

-- ============================================
-- 3. CATEGORIES TABLE (Book classification)
-- ============================================
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

-- ============================================
-- 4. BOOKS TABLE (Library inventory)
-- Uses category_id for proper normalization
-- ============================================
CREATE TABLE books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(50) UNIQUE NOT NULL,
    category_id INT,
    publisher VARCHAR(100),
    publish_year INT,
    total_quantity INT NOT NULL DEFAULT 1,
    available_quantity INT NOT NULL DEFAULT 1,
    shelf_location VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CHECK (available_quantity >= 0),
    CHECK (available_quantity <= total_quantity),
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_category (category_id)
);

-- ============================================
-- 5. BORROWED_BOOKS TABLE (Borrowing records)
-- Uses foreign keys for data integrity
-- Tracks both borrowed and returned books
-- ============================================
CREATE TABLE borrowed_books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status ENUM('BORROWED', 'RETURNED', 'OVERDUE', 'LOST') DEFAULT 'BORROWED',
    issued_by INT,  -- Which librarian/admin issued it
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_member (member_id),
    INDEX idx_book (book_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date)
);

-- ============================================
-- 6. FINES TABLE (Late return penalties)
-- Links to borrow records
-- ============================================
CREATE TABLE fines (
    id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    reason VARCHAR(255) DEFAULT 'Late Return',
    paid BOOLEAN DEFAULT FALSE,
    payment_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id) ON DELETE CASCADE,
    INDEX idx_borrow (borrow_id),
    INDEX idx_paid (paid)
);

-- ============================================
-- 7. AUDIT_LOGS TABLE (Track all actions)
-- Records who did what and when
-- ============================================
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(255) NOT NULL,
    table_name VARCHAR(50),
    record_id INT,
    details TEXT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_action_time (action_time),
    INDEX idx_table (table_name)
);

-- ============================================
-- TRIGGERS FOR AUTOMATIC UPDATES
-- ============================================

-- Trigger: Update available_quantity when borrowing
DELIMITER $$
CREATE TRIGGER after_borrow_insert
AFTER INSERT ON borrowed_books
FOR EACH ROW
BEGIN
    IF NEW.status = 'BORROWED' THEN
        UPDATE books 
        SET available_quantity = available_quantity - 1 
        WHERE id = NEW.book_id;
    END IF;
END$$

-- Trigger: Update available_quantity when returning
CREATE TRIGGER after_borrow_update
AFTER UPDATE ON borrowed_books
FOR EACH ROW
BEGIN
    IF OLD.status = 'BORROWED' AND NEW.status = 'RETURNED' THEN
        UPDATE books 
        SET available_quantity = available_quantity + 1 
        WHERE id = NEW.book_id;
    END IF;
END$$

-- Trigger: Auto-calculate fines for overdue books
CREATE TRIGGER after_return_check_fine
AFTER UPDATE ON borrowed_books
FOR EACH ROW
BEGIN
    DECLARE days_overdue INT;
    DECLARE fine_amount DECIMAL(10,2);
    
    IF NEW.status = 'RETURNED' AND NEW.return_date > NEW.due_date THEN
        SET days_overdue = DATEDIFF(NEW.return_date, NEW.due_date);
        SET fine_amount = days_overdue * 2.00; -- $2 per day
        
        INSERT INTO fines (borrow_id, amount, reason)
        VALUES (NEW.id, fine_amount, CONCAT('Late return by ', days_overdue, ' days'));
    END IF;
END$$

DELIMITER ;

-- ============================================
-- INSERT DEFAULT DATA
-- ============================================

-- Default users
INSERT INTO users (username, password, role) VALUES 
('admin', 'admin123', 'Admin'),
('librarian', 'lib123', 'Librarian');

-- Sample categories
INSERT INTO categories (name, description) VALUES
('Fiction', 'Novels and short stories'),
('Non-Fiction', 'Educational and informational books'),
('Science', 'Scientific literature and research'),
('Technology', 'Computer science, IT, and engineering'),
('History', 'Historical accounts and biographies'),
('Business', 'Business, economics, and management'),
('Self-Help', 'Personal development and motivation'),
('Children', 'Books for children and young adults');

-- Sample members
INSERT INTO members (fname, lname, email, phone, address) VALUES
('John', 'Doe', 'john.doe@email.com', '0123456789', '123 Main St, Lusaka'),
('Jane', 'Smith', 'jane.smith@email.com', '0987654321', '456 Oak Ave, Lusaka'),
('Michael', 'Johnson', 'michael.j@email.com', '0111222333', '789 Pine Rd, Lusaka'),
('Sarah', 'Williams', 'sarah.w@email.com', '0444555666', '321 Elm St, Lusaka');

-- Sample books
INSERT INTO books (title, author, isbn, category_id, publisher, publish_year, total_quantity, available_quantity, shelf_location) VALUES
('Introduction to Algorithms', 'Thomas H. Cormen', '978-0262033848', 4, 'MIT Press', 2009, 5, 5, 'A1-001'),
('Clean Code', 'Robert C. Martin', '978-0132350884', 4, 'Prentice Hall', 2008, 3, 3, 'A1-002'),
('The Pragmatic Programmer', 'Andrew Hunt', '978-0201616224', 4, 'Addison-Wesley', 1999, 4, 4, 'A1-003'),
('Database System Concepts', 'Abraham Silberschatz', '978-0073523323', 4, 'McGraw-Hill', 2010, 4, 3, 'A1-004'),
('Think and Grow Rich', 'Napoleon Hill', '978-1585424337', 7, 'Tarcher', 2005, 6, 6, 'B2-001'),
('Sapiens', 'Yuval Noah Harari', '978-0062316097', 5, 'Harper', 2015, 5, 4, 'C3-001'),
('1984', 'George Orwell', '978-0451524935', 1, 'Signet Classic', 1950, 7, 7, 'D4-001'),
('To Kill a Mockingbird', 'Harper Lee', '978-0061120084', 1, 'Harper Perennial', 1960, 6, 5, 'D4-002');

-- Sample borrow records
INSERT INTO borrowed_books (member_id, book_id, borrow_date, due_date, status, issued_by) VALUES
(1, 4, '2024-12-01', '2024-12-15', 'BORROWED', 1),
(2, 6, '2024-12-05', '2024-12-19', 'BORROWED', 1),
(3, 8, '2024-11-20', '2024-12-04', 'OVERDUE', 2);

-- ============================================
-- USEFUL VIEWS FOR QUICK QUERIES
-- ============================================

-- View: Available books with full details
CREATE VIEW available_books_view AS
SELECT 
    b.id,
    b.title,
    b.author,
    b.isbn,
    c.name AS category,
    b.publisher,
    b.publish_year,
    b.total_quantity,
    b.available_quantity,
    b.shelf_location
FROM books b
LEFT JOIN categories c ON b.category_id = c.id
WHERE b.available_quantity > 0;

-- View: Currently borrowed books
CREATE VIEW currently_borrowed_view AS
SELECT 
    bb.id AS borrow_id,
    CONCAT(m.fname, ' ', m.lname) AS member_name,
    m.email,
    m.phone,
    b.title AS book_title,
    b.author,
    bb.borrow_date,
    bb.due_date,
    DATEDIFF(CURRENT_DATE, bb.due_date) AS days_overdue,
    bb.status
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status IN ('BORROWED', 'OVERDUE');

-- View: Overdue books
CREATE VIEW overdue_books_view AS
SELECT 
    bb.id AS borrow_id,
    CONCAT(m.fname, ' ', m.lname) AS member_name,
    m.email,
    m.phone,
    b.title AS book_title,
    bb.borrow_date,
    bb.due_date,
    DATEDIFF(CURRENT_DATE, bb.due_date) AS days_overdue,
    (DATEDIFF(CURRENT_DATE, bb.due_date) * 2.00) AS estimated_fine
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED' 
  AND bb.due_date < CURRENT_DATE;

-- View: Member borrowing history
CREATE VIEW member_history_view AS
SELECT 
    m.id AS member_id,
    CONCAT(m.fname, ' ', m.lname) AS member_name,
    COUNT(bb.id) AS total_borrowed,
    SUM(CASE WHEN bb.status = 'BORROWED' THEN 1 ELSE 0 END) AS currently_borrowed,
    SUM(CASE WHEN bb.status = 'RETURNED' THEN 1 ELSE 0 END) AS returned,
    SUM(CASE WHEN bb.status = 'OVERDUE' THEN 1 ELSE 0 END) AS overdue
FROM members m
LEFT JOIN borrowed_books bb ON m.id = bb.member_id
GROUP BY m.id;

-- View: Unpaid fines
CREATE VIEW unpaid_fines_view AS
SELECT 
    f.id AS fine_id,
    CONCAT(m.fname, ' ', m.lname) AS member_name,
    m.email,
    b.title AS book_title,
    bb.due_date,
    bb.return_date,
    f.amount,
    f.reason,
    f.created_at AS fine_date
FROM fines f
JOIN borrowed_books bb ON f.borrow_id = bb.id
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE f.paid = FALSE;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check all tables
SELECT 'Users' AS TableName, COUNT(*) AS RecordCount FROM users
UNION ALL
SELECT 'Members', COUNT(*) FROM members
UNION ALL
SELECT 'Categories', COUNT(*) FROM categories
UNION ALL
SELECT 'Books', COUNT(*) FROM books
UNION ALL
SELECT 'Borrowed Books', COUNT(*) FROM borrowed_books
UNION ALL
SELECT 'Fines', COUNT(*) FROM fines
UNION ALL
SELECT 'Audit Logs', COUNT(*) FROM audit_logs;