
create table fines (
	id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id int not null,
    amount decimal(10,2) not null,
    paid boolean default false
);

CREATE TABLE members (
	id int auto_increment primary key,
    fname varchar(20) not null,
    lname varchar(20) not null,
    email varchar(100) unique not null,
    phone varchar(15) not null,
    created_at timestamp default current_timestamp
);

CREATE TABLE users (
	id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE books (
	id int auto_increment primary key,
    title varchar(200) not null,
    author varchar(100) not null,
    category_id int,
    isbn varchar(50) unique,
    quantity int not null,
    created_at timestamp default current_timestamp

    );

    create table audit_logs (
    	id int auto_increment primary key,
        user_id int,
        action varchar(255),
        action_time timestamp default current_timestamp

        );

    create table borrowed_books (
    	id int auto_increment primary key,
        member_id int not null,
        book_id int not null,
        borrow_date date not null,
        due_date date not null,
        return_date date,
        status varchar(20) default 'BORROWED'
        );

    create table categories (
    	id int auto_increment primary key,
        name varchar(100) unique not null
    );


