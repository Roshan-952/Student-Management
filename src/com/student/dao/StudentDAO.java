package com.student.dao;

import com.student.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Student entity.
 * Handles all JDBC CRUD (Create, Read, Update, Delete) operations.
 */
public class StudentDAO {

    /**
     * Initializes the table if it doesn't already exist.
     */
    public void ensureTableExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS students ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(100) NOT NULL UNIQUE, "
                + "course VARCHAR(100) NOT NULL, "
                + "marks DOUBLE NOT NULL, "
                + "grade VARCHAR(5) NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    /**
     * Adds a new student record to the database.
     * 
     * @param student Student object to add
     * @return true if added successfully, false otherwise
     */
    public boolean addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (name, email, course, marks, grade) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getCourse());
            pstmt.setDouble(4, student.getMarks());
            pstmt.setString(5, student.getGrade());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        student.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Searches and returns a student by their ID.
     * 
     * @param id Student ID
     * @return Student object if found, null otherwise
     */
    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT id, name, email, course, marks, grade FROM students WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("course"),
                            rs.getDouble("marks"),
                            rs.getString("grade")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all student records ordered by ID.
     * 
     * @return List of all students
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, email, course, marks, grade FROM students ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("course"),
                        rs.getDouble("marks"),
                        rs.getString("grade")
                ));
            }
        }
        return students;
    }

    /**
     * Updates an existing student record in the database.
     * 
     * @param student Student object containing updated details
     * @return true if updated, false if no record found
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, email = ?, course = ?, marks = ?, grade = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getCourse());
            pstmt.setDouble(4, student.getMarks());
            pstmt.setString(5, student.getGrade());
            pstmt.setInt(6, student.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a student record by ID.
     * 
     * @param id Student ID to delete
     * @return true if deleted, false if record not found
     */
    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}
