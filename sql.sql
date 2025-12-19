
create table fines (
	id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id int not null,
    amount decimal(10,2) not null,
    paid boolean default false
);