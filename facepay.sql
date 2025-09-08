
CREATE DATABASE facepay;
USE facepay;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) UNIQUE,
  balance DOUBLE
);

-- Insert your test user
INSERT INTO users (name, balance) VALUES ("Arjun Kumar", 50000);

SELECT * FROM users;
