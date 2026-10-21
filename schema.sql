-- ==========================================================
-- Student Management System Database Schema
-- Technologies: MySQL, JDBC
-- ==========================================================

CREATE DATABASE IF NOT EXISTS student_db;
USE student_db;

CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    course VARCHAR(100) NOT NULL,
    marks DOUBLE NOT NULL,
    grade VARCHAR(5) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optional sample test data
INSERT INTO students (name, email, course, marks, grade) VALUES
('Alice Johnson', 'alice.j@example.com', 'Computer Science', 92.5, 'A+'),
('Bob Smith', 'bob.smith@example.com', 'Information Technology', 78.0, 'B'),
('Charlie Davis', 'charlie.d@example.com', 'Data Science', 64.0, 'C')
ON DUPLICATE KEY UPDATE name=name;
