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
    phone VARCHAR(25) NOT NULL,
    address TEXT,
    password VARCHAR(60), -- bcrypt hashed password for member login
    profile_completed BOOLEAN DEFAULT FALSE,
    profile_picture VARCHAR(255),
    grade_or_year VARCHAR(100),
    department VARCHAR(100),
    student_id VARCHAR(50),
    courses_or_subjects TEXT,
    interests TEXT,
    favorite_genres TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(25),
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

-- ================================================================
-- LIBRARY MANAGEMENT SYSTEM - DATABASE FIXES AND ENHANCEMENTS
-- ================================================================
-- This script fixes foreign key constraints, adds cascading deletes,
-- and implements the return request system
-- ================================================================

-- ==== DROP EXISTING FOREIGN KEYS (if any exist) ====
-- We need to remove old constraints before adding new ones

ALTER TABLE borrowed_books DROP FOREIGN KEY IF EXISTS borrowed_books_ibfk_1;
ALTER TABLE borrowed_books DROP FOREIGN KEY IF EXISTS borrowed_books_ibfk_2;
ALTER TABLE fines DROP FOREIGN KEY IF EXISTS fines_ibfk_1;

-- ==== FIX BORROWED_BOOKS TABLE CONSTRAINTS ====
-- Add proper foreign keys with CASCADE DELETE
-- When a member is deleted, their borrow records are automatically deleted
-- When a book is deleted, its borrow records are automatically deleted

ALTER TABLE borrowed_books
ADD CONSTRAINT fk_borrowed_books_member
    FOREIGN KEY (member_id) REFERENCES members(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

ALTER TABLE borrowed_books
ADD CONSTRAINT fk_borrowed_books_book
    FOREIGN KEY (book_id) REFERENCES books(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ==== FIX FINES TABLE CONSTRAINTS ====
-- When a borrow record is deleted, associated fines are also deleted
ALTER TABLE fines
ADD CONSTRAINT fk_fines_borrowed_books
    FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

-- ==== ADD RETURN REQUEST SYSTEM ====
-- Create table for return requests from students
CREATE TABLE IF NOT EXISTS return_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    member_id INT NOT NULL,
    request_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    processed_by VARCHAR(100),
    processed_date DATETIME,
    notes TEXT,

    -- Foreign keys with cascade
    FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,

    -- Indexes for performance
    INDEX idx_status (status),
    INDEX idx_member (member_id),
    INDEX idx_request_date (request_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== ADD ACTIVITY LOG TABLE (for dashboard) ====
-- Track all system activities for the dashboard
CREATE TABLE IF NOT EXISTS activity_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT,

    INDEX idx_action_time (action_time),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== ENSURE CATEGORIES TABLE EXISTS ====
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== ADD CATEGORY_ID TO BOOKS TABLE (if not exists) ====
ALTER TABLE books
ADD COLUMN IF NOT EXISTS category_id INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS publish_year INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS isbn VARCHAR(50) DEFAULT NULL;

-- Add foreign key for categories
ALTER TABLE books DROP FOREIGN KEY IF EXISTS fk_books_category;
ALTER TABLE books
ADD CONSTRAINT fk_books_category
    FOREIGN KEY (category_id) REFERENCES categories(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- ==== ADD INDICES FOR PERFORMANCE ====
-- These will speed up queries significantly
ALTER TABLE books ADD INDEX IF NOT EXISTS idx_title (title);
ALTER TABLE books ADD INDEX IF NOT EXISTS idx_author (author);
ALTER TABLE books ADD INDEX IF NOT EXISTS idx_category (category_id);

ALTER TABLE members ADD INDEX IF NOT EXISTS idx_name (name);
ALTER TABLE members ADD INDEX IF NOT EXISTS idx_email (email);

ALTER TABLE borrowed_books ADD INDEX IF NOT EXISTS idx_status (status);
ALTER TABLE borrowed_books ADD INDEX IF NOT EXISTS idx_due_date (due_date);

-- ==== INSERT DEFAULT CATEGORIES (if table is empty) ====
INSERT IGNORE INTO categories (name, description) VALUES
('Fiction', 'Novels, short stories, and other fictional works'),
('Non-Fiction', 'Factual books, biographies, and educational content'),
('Science', 'Scientific books covering various fields of science'),
('Technology', 'Books about technology, programming, and IT'),
('History', 'Historical books and documentaries'),
('Biography', 'Life stories and memoirs of notable people'),
('Self-Help', 'Personal development and self-improvement books'),
('Business', 'Business, economics, and entrepreneurship'),
('Art', 'Art, design, and creative works'),
('Children', 'Books suitable for children and young readers'),
('Reference', 'Dictionaries, encyclopedias, and reference materials'),
('Religion', 'Religious and spiritual texts'),
('Philosophy', 'Philosophical works and thought'),
('Poetry', 'Collections of poems and poetry'),
('Drama', 'Plays and dramatic works');

-- ==== ADD PAYMENT_DATE TO FINES TABLE (if not exists) ====
ALTER TABLE fines
ADD COLUMN IF NOT EXISTS payment_date DATE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reason VARCHAR(255) DEFAULT 'Late return';

-- ==== FIX MEMBERS TABLE STRUCTURE ====
-- Ensure members table has all needed columns
ALTER TABLE members
ADD COLUMN IF NOT EXISTS fname VARCHAR(100) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS lname VARCHAR(100) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS password VARCHAR(255) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- If members only have 'name' column, split it into fname/lname
-- (This is a one-time migration - run carefully)
-- UPDATE members SET fname = SUBSTRING_INDEX(name, ' ', 1),
--                    lname = SUBSTRING_INDEX(name, ' ', -1)
-- WHERE fname IS NULL AND name IS NOT NULL;

-- ==== STORED PROCEDURE: Calculate and Create Fines ====
-- This procedure automatically calculates fines for overdue books
DELIMITER //

DROP PROCEDURE IF EXISTS calculate_overdue_fines//

CREATE PROCEDURE calculate_overdue_fines()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_borrow_id INT;
    DECLARE v_due_date DATE;
    DECLARE v_days_late INT;
    DECLARE v_fine_amount DECIMAL(10,2);
    DECLARE fine_per_day DECIMAL(10,2) DEFAULT 5.00; -- K5.00 per day

    DECLARE cur CURSOR FOR
        SELECT bb.id, bb.due_date,
               DATEDIFF(CURRENT_DATE, bb.due_date) as days_late
        FROM borrowed_books bb
        LEFT JOIN fines f ON f.borrow_id = bb.id
        WHERE bb.status = 'BORROWED'
          AND bb.due_date < CURRENT_DATE
          AND f.id IS NULL; -- Only create fine if one doesn't exist

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_borrow_id, v_due_date, v_days_late;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Calculate fine amount
        SET v_fine_amount = v_days_late * fine_per_day;

        -- Insert fine record
        INSERT INTO fines (borrow_id, amount, paid, reason)
        VALUES (v_borrow_id, v_fine_amount, FALSE,
                CONCAT('Late return - ', v_days_late, ' days overdue'));
    END LOOP;

    CLOSE cur;
END//

DELIMITER ;

-- ==== TRIGGER: Log Activity on Book Borrow ====
DELIMITER //

DROP TRIGGER IF EXISTS after_book_borrow//

CREATE TRIGGER after_book_borrow
AFTER INSERT ON borrowed_books
FOR EACH ROW
BEGIN
    DECLARE book_title VARCHAR(255);
    DECLARE member_name VARCHAR(200);

    SELECT title INTO book_title FROM books WHERE id = NEW.book_id;
    SELECT CONCAT(fname, ' ', lname) INTO member_name FROM members WHERE id = NEW.member_id;

    INSERT INTO activity_log (action, username, details)
    VALUES ('Book Borrowed', member_name, CONCAT('Borrowed: ', book_title));
END//

DELIMITER ;

-- ==== TRIGGER: Log Activity on Book Return ====
DELIMITER //

DROP TRIGGER IF EXISTS after_book_return//

CREATE TRIGGER after_book_return
AFTER UPDATE ON borrowed_books
FOR EACH ROW
BEGIN
    DECLARE book_title VARCHAR(255);
    DECLARE member_name VARCHAR(200);

    IF OLD.status = 'BORROWED' AND NEW.status = 'RETURNED' THEN
        SELECT title INTO book_title FROM books WHERE id = NEW.book_id;
        SELECT CONCAT(fname, ' ', lname) INTO member_name FROM members WHERE id = NEW.member_id;

        INSERT INTO activity_log (action, username, details)
        VALUES ('Book Returned', member_name, CONCAT('Returned: ', book_title));
    END IF;
END//

DELIMITER ;

-- ==== VIEW: Overdue Books Summary ====
CREATE OR REPLACE VIEW overdue_books_view AS
SELECT
    bb.id as borrow_id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    b.title as book_title,
    bb.due_date,
    DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue,
    DATEDIFF(CURRENT_DATE, bb.due_date) * 5.00 as estimated_fine
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED'
  AND bb.due_date < CURRENT_DATE
ORDER BY days_overdue DESC;

-- ==== VIEW: Member Statistics ====
CREATE OR REPLACE VIEW member_statistics AS
SELECT
    m.id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    COUNT(bb.id) as total_borrowed,
    SUM(CASE WHEN bb.status = 'BORROWED' THEN 1 ELSE 0 END) as currently_borrowed,
    SUM(CASE WHEN bb.status = 'RETURNED' THEN 1 ELSE 0 END) as returned,
    COALESCE(SUM(f.amount), 0) as total_fines,
    COALESCE(SUM(CASE WHEN f.paid = FALSE THEN f.amount ELSE 0 END), 0) as unpaid_fines
FROM members m
LEFT JOIN borrowed_books bb ON m.id = bb.member_id
LEFT JOIN fines f ON bb.id = f.borrow_id
GROUP BY m.id, m.fname, m.lname, m.email;

-- ================================================================
-- VERIFICATION QUERIES
-- ================================================================
-- Run these to verify the changes worked correctly:

-- Check foreign keys are in place
-- SELECT * FROM information_schema.KEY_COLUMN_USAGE
-- WHERE TABLE_SCHEMA = 'library_db'
--   AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Check categories exist
-- SELECT COUNT(*) as category_count FROM categories;

-- Check for any overdue books
-- SELECT * FROM overdue_books_view;

-- Check member statistics
-- SELECT * FROM member_statistics LIMIT 10;

-- ================================================================
-- NOTES:
-- ================================================================
-- 1. CASCADE DELETE ensures referential integrity
-- 2. When you delete a member, all their borrows and fines are deleted
-- 3. When you delete a book, all its borrow records are deleted
-- 4. When you delete a borrow record, associated fines are deleted
-- 5. The return_requests table enables student-initiated returns
-- 6. Activity logging helps populate the dashboard
-- ================================================================

-- ================================================================
-- LIBRARY MANAGEMENT SYSTEM - QUICK REFERENCE SQL COMMANDS
-- ================================================================
-- Common queries and operations for daily management
-- ================================================================

-- ============================
-- 1. STATISTICS & REPORTING
-- ============================

-- Total books in library
SELECT COUNT(*) as total_books FROM books;

-- Available vs Borrowed books
SELECT
    SUM(total_quantity) as total_books,
    SUM(available_quantity) as available,
    SUM(total_quantity - available_quantity) as borrowed
FROM books;

-- Current overdue books count
SELECT COUNT(*) as overdue_count
FROM borrowed_books
WHERE status='BORROWED' AND due_date < CURRENT_DATE;

-- Total unpaid fines
SELECT COALESCE(SUM(amount), 0) as total_unpaid
FROM fines
WHERE paid = FALSE;

-- Active members (currently have borrowed books)
SELECT COUNT(DISTINCT member_id) as active_members
FROM borrowed_books
WHERE status='BORROWED';

-- Books borrowed today
SELECT COUNT(*) as borrowed_today
FROM borrowed_books
WHERE DATE(borrow_date) = CURRENT_DATE;

-- ============================
-- 2. OVERDUE MANAGEMENT
-- ============================

-- List all overdue books with member details
SELECT
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    m.phone,
    b.title,
    b.author,
    bb.due_date,
    DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue,
    DATEDIFF(CURRENT_DATE, bb.due_date) * 5.00 as estimated_fine
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED'
  AND bb.due_date < CURRENT_DATE
ORDER BY days_overdue DESC;

-- Overdue books by severity
SELECT
    CASE
        WHEN DATEDIFF(CURRENT_DATE, due_date) > 30 THEN 'Critical (30+ days)'
        WHEN DATEDIFF(CURRENT_DATE, due_date) > 14 THEN 'Severe (14-30 days)'
        WHEN DATEDIFF(CURRENT_DATE, due_date) > 7 THEN 'Moderate (7-14 days)'
        ELSE 'Mild (1-7 days)'
    END as severity,
    COUNT(*) as count
FROM borrowed_books
WHERE status = 'BORROWED' AND due_date < CURRENT_DATE
GROUP BY severity
ORDER BY DATEDIFF(CURRENT_DATE, due_date) DESC;

-- ============================
-- 3. FINE MANAGEMENT
-- ============================

-- Calculate fines for all overdue books
CALL calculate_overdue_fines();

-- Manually create a fine for a specific borrow record
INSERT INTO fines (borrow_id, amount, paid, reason)
VALUES (
    [borrow_id],
    [days_late] * 5.00,
    FALSE,
    CONCAT('Late return - ', [days_late], ' days overdue')
);

-- Mark fine as paid
UPDATE fines
SET paid = TRUE, payment_date = CURRENT_DATE
WHERE id = [fine_id];

-- List unpaid fines with member details
SELECT
    f.id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    b.title,
    f.amount,
    f.reason,
    DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue
FROM fines f
JOIN borrowed_books bb ON f.borrow_id = bb.id
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE f.paid = FALSE
ORDER BY f.amount DESC;

-- ============================
-- 4. RETURN REQUEST MANAGEMENT
-- ============================

-- List all pending return requests
SELECT
    rr.id,
    CONCAT(m.fname, ' ', m.lname) as student_name,
    m.email,
    b.title,
    rr.request_date,
    bb.due_date,
    CASE
        WHEN bb.due_date < CURRENT_DATE
        THEN DATEDIFF(CURRENT_DATE, bb.due_date)
        ELSE 0
    END as days_late
FROM return_requests rr
JOIN borrowed_books bb ON rr.borrow_id = bb.id
JOIN members m ON rr.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE rr.status = 'PENDING'
ORDER BY rr.request_date ASC;

-- Approve a return request (manually)
-- Step 1: Mark book as returned
UPDATE borrowed_books
SET status = 'RETURNED', return_date = CURRENT_DATE
WHERE id = (SELECT borrow_id FROM return_requests WHERE id = [request_id]);

-- Step 2: Update book availability
UPDATE books
SET available_quantity = available_quantity + 1
WHERE id = (SELECT book_id FROM borrowed_books
            WHERE id = (SELECT borrow_id FROM return_requests WHERE id = [request_id]));

-- Step 3: Mark request as approved
UPDATE return_requests
SET status = 'APPROVED',
    processed_by = '[username]',
    processed_date = CURRENT_TIMESTAMP
WHERE id = [request_id];

-- Reject a return request
UPDATE return_requests
SET status = 'REJECTED',
    processed_by = '[username]',
    processed_date = CURRENT_TIMESTAMP,
    notes = '[rejection reason]'
WHERE id = [request_id];

-- ============================
-- 5. MEMBER MANAGEMENT
-- ============================

-- Find member by name or email
SELECT * FROM members
WHERE LOWER(fname) LIKE '%[search]%'
   OR LOWER(lname) LIKE '%[search]%'
   OR LOWER(email) LIKE '%[search]%';

-- Member borrowing history
SELECT
    b.title,
    bb.borrow_date,
    bb.due_date,
    bb.return_date,
    bb.status,
    CASE
        WHEN bb.status = 'BORROWED' AND bb.due_date < CURRENT_DATE
        THEN 'OVERDUE'
        ELSE bb.status
    END as current_status
FROM borrowed_books bb
JOIN books b ON bb.book_id = b.id
WHERE bb.member_id = [member_id]
ORDER BY bb.borrow_date DESC;

-- Member with most borrowed books
SELECT
    m.id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    COUNT(bb.id) as total_borrowed
FROM members m
JOIN borrowed_books bb ON m.id = bb.member_id
GROUP BY m.id, m.fname, m.lname
ORDER BY total_borrowed DESC
LIMIT 10;

-- Members with unpaid fines
SELECT DISTINCT
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    m.phone,
    SUM(f.amount) as total_unpaid
FROM members m
JOIN borrowed_books bb ON m.id = bb.member_id
JOIN fines f ON bb.id = f.borrow_id
WHERE f.paid = FALSE
GROUP BY m.id, m.fname, m.lname, m.email, m.phone
ORDER BY total_unpaid DESC;

-- ============================
-- 6. BOOK MANAGEMENT
-- ============================

-- Books currently unavailable (all copies borrowed)
SELECT
    b.id,
    b.title,
    b.author,
    b.total_quantity,
    b.available_quantity
FROM books b
WHERE b.available_quantity = 0
ORDER BY b.title;

-- Most borrowed books (all time)
SELECT
    b.title,
    b.author,
    COUNT(bb.id) as times_borrowed
FROM books b
JOIN borrowed_books bb ON b.id = bb.book_id
GROUP BY b.id, b.title, b.author
ORDER BY times_borrowed DESC
LIMIT 20;

-- Books by category
SELECT
    c.name as category,
    COUNT(b.id) as book_count,
    SUM(b.available_quantity) as available_copies
FROM categories c
LEFT JOIN books b ON c.id = b.category_id
GROUP BY c.id, c.name
ORDER BY book_count DESC;

-- Books that have never been borrowed
SELECT
    b.title,
    b.author,
    b.publish_year,
    c.name as category
FROM books b
LEFT JOIN borrowed_books bb ON b.id = bb.book_id
LEFT JOIN categories c ON b.category_id = c.id
WHERE bb.id IS NULL
ORDER BY b.title;

-- ============================
-- 7. ACTIVITY MONITORING
-- ============================

-- Recent system activity (last 24 hours)
SELECT
    action,
    username,
    action_time,
    details
FROM activity_log
WHERE action_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY action_time DESC
LIMIT 50;

-- Activity summary by action type
SELECT
    action,
    COUNT(*) as count,
    MAX(action_time) as last_occurrence
FROM activity_log
WHERE action_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY action
ORDER BY count DESC;

-- Most active users (last 30 days)
SELECT
    username,
    COUNT(*) as activity_count
FROM activity_log
WHERE action_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY username
ORDER BY activity_count DESC
LIMIT 10;

-- ============================
-- 8. DATA CLEANUP & MAINTENANCE
-- ============================

-- Archive old activity logs (keep last 6 months)
-- First, create archive table if not exists
CREATE TABLE IF NOT EXISTS activity_log_archive LIKE activity_log;

-- Move old records to archive
INSERT INTO activity_log_archive
SELECT * FROM activity_log
WHERE action_time < DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- Delete archived records from main table
DELETE FROM activity_log
WHERE action_time < DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- Find and remove duplicate book entries (same ISBN)
SELECT isbn, COUNT(*) as count
FROM books
GROUP BY isbn
HAVING count > 1;

-- Clean up orphaned records (should not exist with CASCADE)
-- Check for borrowed_books without valid members
SELECT bb.*
FROM borrowed_books bb
LEFT JOIN members m ON bb.member_id = m.id
WHERE m.id IS NULL;

-- Check for borrowed_books without valid books
SELECT bb.*
FROM borrowed_books bb
LEFT JOIN books b ON bb.book_id = b.id
WHERE b.id IS NULL;

-- ============================
-- 9. PERFORMANCE MONITORING
-- ============================

-- Check database size
SELECT
    table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) as size_mb
FROM information_schema.tables
WHERE table_schema = 'library_db'
ORDER BY size_mb DESC;

-- Check index usage
SELECT
    table_name,
    index_name,
    seq_in_index,
    column_name
FROM information_schema.statistics
WHERE table_schema = 'library_db'
ORDER BY table_name, index_name, seq_in_index;

-- Slow queries audit
-- Enable slow query log in MySQL config
-- Then analyze: /var/log/mysql/slow-query.log

-- ============================
-- 10. BACKUP & RESTORE
-- ============================

-- Full database backup (run in terminal)
-- mysqldump -u root -p library_db > library_db_backup_$(date +%Y%m%d).sql

-- Backup specific tables
-- mysqldump -u root -p library_db members books borrowed_books > essential_tables.sql

-- Restore from backup
-- mysql -u root -p library_db < library_db_backup.sql

-- Export data to CSV (for Excel)
SELECT
    CONCAT(m.fname, ' ', m.lname) as 'Member Name',
    m.email as 'Email',
    b.title as 'Book Title',
    bb.borrow_date as 'Borrow Date',
    bb.due_date as 'Due Date',
    bb.status as 'Status'
INTO OUTFILE '/var/lib/mysql-files/borrowed_books_export.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED';

-- ============================
-- 11. USEFUL VIEWS
-- ============================

-- Create view for quick overdue lookup
CREATE OR REPLACE VIEW current_overdues AS
SELECT
    bb.id as borrow_id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    m.phone,
    b.title,
    b.author,
    bb.borrow_date,
    bb.due_date,
    DATEDIFF(CURRENT_DATE, bb.due_date) as days_overdue,
    DATEDIFF(CURRENT_DATE, bb.due_date) * 5.00 as estimated_fine,
    CASE
        WHEN EXISTS (SELECT 1 FROM fines f WHERE f.borrow_id = bb.id)
        THEN 'Fine Created'
        ELSE 'No Fine'
    END as fine_status
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED' AND bb.due_date < CURRENT_DATE
ORDER BY days_overdue DESC;

-- Use the view
SELECT * FROM current_overdues;

-- Create view for member dashboard
CREATE OR REPLACE VIEW member_dashboard AS
SELECT
    m.id,
    CONCAT(m.fname, ' ', m.lname) as member_name,
    m.email,
    COUNT(DISTINCT CASE WHEN bb.status = 'BORROWED' THEN bb.id END) as currently_borrowed,
    COUNT(DISTINCT CASE WHEN bb.status = 'RETURNED' THEN bb.id END) as total_returned,
    COALESCE(SUM(CASE WHEN f.paid = FALSE THEN f.amount ELSE 0 END), 0) as unpaid_fines,
    COUNT(DISTINCT CASE WHEN bb.status = 'BORROWED' AND bb.due_date < CURRENT_DATE THEN bb.id END) as overdue_count
FROM members m
LEFT JOIN borrowed_books bb ON m.id = bb.member_id
LEFT JOIN fines f ON bb.id = f.borrow_id
GROUP BY m.id, m.fname, m.lname, m.email;

-- Use the view
SELECT * FROM member_dashboard WHERE unpaid_fines > 0;

-- ============================
-- 12. SCHEDULED MAINTENANCE
-- ============================

-- Recommended daily tasks (set up as cron job)

-- 1. Calculate overdue fines
CALL calculate_overdue_fines();

-- 2. Send reminder emails (implement in application)
SELECT * FROM current_overdues WHERE days_overdue >= 7;

-- 3. Update statistics cache (if implemented)
-- Your custom procedure here

-- 4. Backup database (in cron)
-- 0 2 * * * mysqldump -u root -p[password] library_db > /backups/library_$(date +\%Y\%m\%d).sql

-- ================================================================
-- END OF QUICK REFERENCE
-- ================================================================
--
-- For more complex queries or procedures, refer to the main
-- database_fixes.sql file or the application code.
-- ================================================================

===================    Latest Migrations =========================

-- ============================================================
-- MIGRATION: Borrow Requests + Enhanced Member Profile
-- Run this against your library_db database
-- ============================================================

-- ------------------------------------------------------------
-- 1. Enhance members table with new profile fields
-- ------------------------------------------------------------
ALTER TABLE members
  ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(255)  DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS gender          VARCHAR(30)   DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS date_of_birth   DATE          DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS address         VARCHAR(255)  DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP;

-- ------------------------------------------------------------
-- 2. borrow_requests table
--    One row per student request. Librarian/admin then
--    APPROVES (creates a borrowed_books row) or REJECTS.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow_requests (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  member_id    INT          NOT NULL,
  book_id      INT          NOT NULL,
  request_date TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  status       ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  notes        VARCHAR(255) DEFAULT NULL,
  processed_by VARCHAR(100) DEFAULT NULL,
  processed_at TIMESTAMP    DEFAULT NULL,
  FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
  FOREIGN KEY (book_id)   REFERENCES books(id)   ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 3. borrowed_books (actual borrow records after approval)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrowed_books (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  member_id   INT  NOT NULL,
  book_id     INT  NOT NULL,
  borrow_date DATE NOT NULL,
  due_date    DATE NOT NULL,
  return_date DATE DEFAULT NULL,
  status      ENUM('BORROWED','RETURNED') DEFAULT 'BORROWED',
  FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
  FOREIGN KEY (book_id)   REFERENCES books(id)   ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 4. return_requests (student-initiated return workflow)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS return_requests (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  borrow_id    INT          NOT NULL,
  member_id    INT          NOT NULL,
  request_date TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  status       ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  notes        VARCHAR(255) DEFAULT NULL,
  FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id) ON DELETE CASCADE,
  FOREIGN KEY (member_id) REFERENCES members(id)        ON DELETE CASCADE
);