-- ============================================================
-- LIBRARY MANAGEMENT SYSTEM - CANONICAL SCHEMA v2.0
-- Supports both MySQL 8.0+ and H2 (with minor adjustments)
-- ============================================================

-- Institution profile (single row, set during setup)
CREATE TABLE IF NOT EXISTS institution_profile (
    id INT PRIMARY KEY DEFAULT 1,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('School','Public Library','Other')),
    logo_path VARCHAR(255),
    address TEXT,
    phone VARCHAR(25),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
);

-- System settings (key‑value)
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Schema version (migration tracking)
CREATE TABLE IF NOT EXISTS schema_version (
    version INT PRIMARY KEY,
    description VARCHAR(255),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users (Admin, Librarian)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    mname VARCHAR(50) DEFAULT NULL,
    lname VARCHAR(50) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password CHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('Admin','Librarian')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_role (role)
);

-- Members (patrons/students)
CREATE TABLE IF NOT EXISTS members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    mname VARCHAR(50) DEFAULT NULL,
    lname VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(25) NOT NULL,
    password CHAR(60) NOT NULL,               -- bcrypt
    profile_picture VARCHAR(255),
    gender VARCHAR(30) DEFAULT NULL,
    date_of_birth DATE DEFAULT NULL,
    address TEXT,
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

-- Categories
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Books
CREATE TABLE IF NOT EXISTS books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(50) UNIQUE,
    category_id INT,
    publisher VARCHAR(100),
    publish_year INT,
    total_quantity INT NOT NULL DEFAULT 1,
    available_quantity INT NOT NULL DEFAULT 1,
    shelf_location VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CHECK (available_quantity >= 0 AND available_quantity <= total_quantity),
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_category (category_id)
);

-- Borrowed books
CREATE TABLE IF NOT EXISTS borrowed_books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BORROWED'
        CHECK (status IN ('BORROWED','RETURNED','OVERDUE','LOST')),
    issued_by INT,
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

-- Fines
CREATE TABLE IF NOT EXISTS fines (
    id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    reason VARCHAR(255) DEFAULT 'Late Return',
    paid BOOLEAN DEFAULT FALSE,
    payment_date DATE DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id) ON DELETE CASCADE,
    INDEX idx_borrow (borrow_id),
    INDEX idx_paid (paid)
);

-- Return requests (student self‑service)
CREATE TABLE IF NOT EXISTS return_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT NOT NULL,
    member_id INT NOT NULL,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    processed_by VARCHAR(100),
    processed_date TIMESTAMP NULL,
    notes TEXT,
    FOREIGN KEY (borrow_id) REFERENCES borrowed_books(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_status (status)
);

-- Borrow requests (student asks to borrow)
CREATE TABLE IF NOT EXISTS borrow_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    notes VARCHAR(255),
    processed_by VARCHAR(100),
    processed_at TIMESTAMP NULL,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Audit logs (central)
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    username VARCHAR(100) NOT NULL,
    action VARCHAR(255) NOT NULL,
    table_name VARCHAR(50),
    record_id INT,
    details TEXT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_action_time (action_time)
);

-- ============================================================
-- TRIGGERS
-- ============================================================
DELIMITER $$

CREATE TRIGGER after_borrow_insert
AFTER INSERT ON borrowed_books
FOR EACH ROW
BEGIN
    IF NEW.status = 'BORROWED' THEN
        UPDATE books SET available_quantity = available_quantity - 1
        WHERE id = NEW.book_id;
    END IF;
END$$

CREATE TRIGGER after_borrow_update
AFTER UPDATE ON borrowed_books
FOR EACH ROW
BEGIN
    -- Book returned
    IF OLD.status = 'BORROWED' AND NEW.status = 'RETURNED' THEN
        UPDATE books SET available_quantity = available_quantity + 1
        WHERE id = NEW.book_id;
    END IF;
    -- Book lost
    IF OLD.status = 'BORROWED' AND NEW.status = 'LOST' THEN
        UPDATE books SET total_quantity = total_quantity - 1,
                         available_quantity = available_quantity - 1
        WHERE id = NEW.book_id;
    END IF;
    -- Audit log for return
    IF OLD.status = 'BORROWED' AND NEW.status IN ('RETURNED','LOST') THEN
        INSERT INTO audit_logs (user_id, username, action, table_name, record_id, details)
        VALUES (NEW.issued_by, (SELECT username FROM users WHERE id = NEW.issued_by),
                'Book Returned', 'borrowed_books', NEW.id,
                CONCAT('Book ID: ', NEW.book_id, ' returned by Member ID: ', NEW.member_id));
    END IF;
END$$

CREATE TRIGGER after_borrow_audit
AFTER INSERT ON borrowed_books
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, username, action, table_name, record_id, details)
    VALUES (NEW.issued_by, (SELECT username FROM users WHERE id = NEW.issued_by),
            'Book Borrowed', 'borrowed_books', NEW.id,
            CONCAT('Book ID: ', NEW.book_id, ' borrowed by Member ID: ', NEW.member_id));
END$$

DELIMITER ;

-- ============================================================
-- STORED PROCEDURE: Calculate overdue fines (uses system_settings)
-- ============================================================
DELIMITER $$
CREATE PROCEDURE calculate_overdue_fines()
BEGIN
    DECLARE fine_rate DECIMAL(10,2);
    SELECT COALESCE(setting_value, '5.00') INTO fine_rate
    FROM system_settings WHERE setting_key = 'fine_per_day';

    INSERT INTO fines (borrow_id, amount, reason)
    SELECT bb.id,
           DATEDIFF(CURDATE(), bb.due_date) * CAST(fine_rate AS DECIMAL(10,2)),
           CONCAT('Late return - ', DATEDIFF(CURDATE(), bb.due_date), ' days overdue')
    FROM borrowed_books bb
    LEFT JOIN fines f ON f.borrow_id = bb.id
    WHERE bb.status = 'BORROWED'
      AND bb.due_date < CURDATE()
      AND f.id IS NULL;
END$$
DELIMITER ;

-- ============================================================
-- VIEWS
-- ============================================================
CREATE OR REPLACE VIEW available_books_view AS
SELECT b.id, b.title, b.author, b.isbn, c.name AS category,
       b.publisher, b.publish_year, b.total_quantity,
       b.available_quantity, b.shelf_location
FROM books b
LEFT JOIN categories c ON b.category_id = c.id
WHERE b.available_quantity > 0;

CREATE OR REPLACE VIEW overdue_books_view AS
SELECT bb.id AS borrow_id,
       CONCAT(m.fname, ' ', COALESCE(m.mname,''), ' ', m.lname) AS member_name,
       m.email, m.phone, b.title AS book_title,
       bb.due_date, DATEDIFF(CURDATE(), bb.due_date) AS days_overdue,
       DATEDIFF(CURDATE(), bb.due_date) * 5.00 AS estimated_fine
FROM borrowed_books bb
JOIN members m ON bb.member_id = m.id
JOIN books b ON bb.book_id = b.id
WHERE bb.status = 'BORROWED' AND bb.due_date < CURDATE();

-- ============================================================
-- DEFAULT DATA
-- ============================================================
-- Institution profile placeholder (will be filled by setup wizard)
INSERT INTO institution_profile (name, type) VALUES ('My Library', 'Public Library');

-- Default settings
INSERT INTO system_settings (setting_key, setting_value) VALUES
('fine_per_day', '5.00'),
('default_loan_period_days', '14'),
('max_books_per_member', '5'),
('enable_borrow_requests', 'true'),
('enable_return_requests', 'true'),
('require_member_approval', 'false'),
('member_fields_visible', 'fname,lname,email,phone,address,student_id,grade_or_year');

-- Insert schema version
INSERT INTO schema_version (version, description) VALUES (2, 'Canonical consolidated schema with user names');

-- Admin user (password: admin123 bcrypt)
INSERT INTO users (fname, lname, username, password, role)
VALUES ('System', 'Admin', 'admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Admin');

-- Librarian (password: lib123)
INSERT INTO users (fname, lname, username, password, role)
VALUES ('Head', 'Librarian', 'librarian',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Librarian');

-- Categories
INSERT INTO categories (name, description) VALUES
('Fiction', 'Novels and short stories'),
('Non-Fiction', 'Educational and informational'),
('Science', 'Scientific literature'),
('Technology', 'Computing and engineering'),
('History', 'Historical books');

-- Sample members (password: member123)
INSERT INTO members (fname, lname, email, phone, password)
VALUES ('John', 'Doe', 'john@example.com', '0123456789',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- Sample books
INSERT INTO books (title, author, isbn, category_id, publisher, publish_year, total_quantity, available_quantity)
VALUES ('Introduction to Algorithms', 'Thomas Cormen', '978-0262033848', 4, 'MIT Press', 2009, 5, 5);