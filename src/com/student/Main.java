package com.student;

import com.student.dao.DatabaseConnection;
import com.student.dao.StudentDAO;
import com.student.model.Student;
import com.student.web.WebServer;

import java.awt.Desktop;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Main application console & web user interface for Student Management System.
 */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static int webPort = 8080;

    public static void main(String[] args) {
        printBanner();
        checkAndInitializeDatabase();
        startWebServer();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter your choice (1-8): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addStudentRecord();
                    break;
                case "2":
                    displayAllStudents();
                    break;
                case "3":
                    searchStudentById();
                    break;
                case "4":
                    updateStudentDetails();
                    break;
                case "5":
                    deleteStudentRecord();
                    break;
                case "6":
                    configureDatabaseSettings();
                    break;
                case "7":
                    openWebBrowser();
                    break;
                case "8":
                    running = false;
                    WebServer.stop();
                    System.out.println("\nThank you for using the Student Management System. Goodbye!");
                    break;
                default:
                    System.out.println("\n[!] Invalid option. Please enter a number between 1 and 8.");
            }
        }
    }

    private static void startWebServer() {
        try {
            webPort = WebServer.start(8080);
            System.out.println("==============================================================");
            System.out.println("🌐 WEB DASHBOARD READY: http://localhost:" + webPort);
            System.out.println("==============================================================\n");
            openWebBrowser();
        } catch (Exception e) {
            System.out.println("[!] Web server could not start on port 8080: " + e.getMessage());
        }
    }

    private static void openWebBrowser() {
        String url = "http://localhost:" + webPort;
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
                System.out.println("[i] Opened web dashboard in default browser: " + url);
                return;
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                System.out.println("[i] Opened web dashboard in default browser: " + url);
                return;
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
                System.out.println("[i] Opened web dashboard in default browser: " + url);
                return;
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("[i] Opened web dashboard in default browser: " + url);
                return;
            }
        } catch (Exception ignored) {}
        System.out.println("[i] Open " + url + " in your browser to view the Web UI.");
    }

    private static void printBanner() {
        System.out.println("==============================================================");
        System.out.println("             STUDENT MANAGEMENT SYSTEM (Core Java + Web UI)   ");
        System.out.println("==============================================================");
    }

    private static void printMenu() {
        System.out.println("\n----------------------- MAIN MENU ----------------------------");
        System.out.println(" 1. Add Student Record");
        System.out.println(" 2. Display All Students");
        System.out.println(" 3. Search Student by ID");
        System.out.println(" 4. Update Student Details");
        System.out.println(" 5. Delete Student Record");
        System.out.println(" 6. Database Connection Settings / Reconnect");
        System.out.println(" 7. Open Web UI Dashboard in Browser (http://localhost:" + webPort + ")");
        System.out.println(" 8. Exit");
        System.out.println("--------------------------------------------------------------");
    }

    private static void checkAndInitializeDatabase() {
        System.out.println("Checking database connection...");
        try {
            studentDAO.ensureTableExists();
            System.out.println("[OK] Active database: " + DatabaseConnection.getActiveModeDescription() + " (Ready)\n");
        } catch (SQLException e) {
            System.out.println("\n[!] Database connection failed: " + e.getMessage());
            System.out.println("[i] You can configure database settings via Option 6 in the menu.\n");
        }
    }

    private static void addStudentRecord() {
        System.out.println("\n--- [ Add Student Record ] ---");

        System.out.print("Enter Student Full Name: ");
        String name = scanner.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("Name cannot be empty. Please enter name: ");
            name = scanner.nextLine().trim();
        }

        System.out.print("Enter Student Email: ");
        String email = scanner.nextLine().trim();
        while (email.isEmpty() || !email.contains("@")) {
            System.out.print("Please enter a valid email address: ");
            email = scanner.nextLine().trim();
        }

        System.out.print("Enter Course/Department: ");
        String course = scanner.nextLine().trim();
        while (course.isEmpty()) {
            System.out.print("Course cannot be empty. Please enter course: ");
            course = scanner.nextLine().trim();
        }

        double marks = readValidMarks();

        Student student = new Student(name, email, course, marks);

        try {
            boolean success = studentDAO.addStudent(student);
            if (success) {
                System.out.println("\n[SUCCESS] Student added successfully!");
                System.out.println("Assigned Student ID: " + student.getId());
                System.out.println("Calculated Grade:    " + student.getGrade());
            } else {
                System.out.println("\n[FAIL] Could not insert student record.");
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database Error: " + e.getMessage());
        }
    }

    private static void displayAllStudents() {
        System.out.println("\n--- [ All Registered Students ] ---");
        try {
            List<Student> students = studentDAO.getAllStudents();
            if (students.isEmpty()) {
                System.out.println("No student records found in the database.");
                return;
            }

            printStudentTable(students);

            // Display Grade Scale and Class Statistics
            double totalMarks = 0;
            for (Student s : students) {
                totalMarks += s.getMarks();
            }
            double avg = totalMarks / students.size();
            System.out.printf("Total Students: %d | Class Average Marks: %.2f | Average Grade: %s\n",
                    students.size(), avg, Student.calculateGrade(avg));

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Could not fetch records: " + e.getMessage());
        }
    }

    private static void searchStudentById() {
        System.out.println("\n--- [ Search Student by ID ] ---");
        int id = readValidInt("Enter Student ID: ");

        try {
            Student student = studentDAO.getStudentById(id);
            if (student != null) {
                System.out.println("\nStudent Record Found:");
                System.out.println("+------------------------------------------------------------+");
                System.out.printf("| ID:      %-50d |\n", student.getId());
                System.out.printf("| Name:    %-50s |\n", student.getName());
                System.out.printf("| Email:   %-50s |\n", student.getEmail());
                System.out.printf("| Course:  %-50s |\n", student.getCourse());
                System.out.printf("| Marks:   %-50.2f |\n", student.getMarks());
                System.out.printf("| Grade:   %-50s |\n", student.getGrade());
                System.out.println("+------------------------------------------------------------+");
            } else {
                System.out.println("\n[!] No student found with ID: " + id);
            }
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during search: " + e.getMessage());
        }
    }

    private static void updateStudentDetails() {
        System.out.println("\n--- [ Update Student Details ] ---");
        int id = readValidInt("Enter Student ID to update: ");

        try {
            Student student = studentDAO.getStudentById(id);
            if (student == null) {
                System.out.println("\n[!] Student not found with ID: " + id);
                return;
            }

            System.out.println("\nCurrent details: " + student);
            System.out.println("(Press ENTER without typing to keep current value)\n");

            System.out.print("New Name [" + student.getName() + "]: ");
            String name = scanner.nextLine().trim();
            if (!name.isEmpty()) {
                student.setName(name);
            }

            System.out.print("New Email [" + student.getEmail() + "]: ");
            String email = scanner.nextLine().trim();
            if (!email.isEmpty()) {
                student.setEmail(email);
            }

            System.out.print("New Course [" + student.getCourse() + "]: ");
            String course = scanner.nextLine().trim();
            if (!course.isEmpty()) {
                student.setCourse(course);
            }

            System.out.print("New Marks [" + student.getMarks() + "]: ");
            String marksInput = scanner.nextLine().trim();
            if (!marksInput.isEmpty()) {
                try {
                    double marks = Double.parseDouble(marksInput);
                    if (marks >= 0 && marks <= 100) {
                        student.setMarks(marks); // recalculates grade automatically
                    } else {
                        System.out.println("[!] Invalid range (0-100). Marks kept as previous.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[!] Invalid number. Marks kept as previous.");
                }
            }

            boolean success = studentDAO.updateStudent(student);
            if (success) {
                System.out.println("\n[SUCCESS] Student record updated successfully!");
                System.out.println("Updated Grade: " + student.getGrade());
            } else {
                System.out.println("\n[FAIL] Update operation failed.");
            }

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during update: " + e.getMessage());
        }
    }

    private static void deleteStudentRecord() {
        System.out.println("\n--- [ Delete Student Record ] ---");
        int id = readValidInt("Enter Student ID to delete: ");

        try {
            Student student = studentDAO.getStudentById(id);
            if (student == null) {
                System.out.println("\n[!] Student not found with ID: " + id);
                return;
            }

            System.out.printf("Are you sure you want to delete '%s' (ID: %d)? (yes/no): ", student.getName(), student.getId());
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("y") || confirm.equals("yes")) {
                boolean deleted = studentDAO.deleteStudent(id);
                if (deleted) {
                    System.out.println("\n[SUCCESS] Student record deleted.");
                } else {
                    System.out.println("\n[FAIL] Record could not be deleted.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }

        } catch (SQLException e) {
            System.out.println("\n[ERROR] Database error during delete: " + e.getMessage());
        }
    }

    private static void configureDatabaseSettings() {
        System.out.println("\n--- [ Database Settings & Connection ] ---");
        System.out.println("Current Active Database: " + DatabaseConnection.getActiveModeDescription());
        System.out.println("1. Connect to MySQL Server (Localhost, XAMPP, or Remote)");
        System.out.println("2. Switch to Embedded Database (H2 - Zero setup required)");
        System.out.println("3. Back to Main Menu");
        System.out.print("Select an option (1-3): ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                System.out.print("Enter MySQL Host [default " + DatabaseConnection.getHost() + "]: ");
                String host = scanner.nextLine().trim();
                if (host.isEmpty()) host = DatabaseConnection.getHost();

                int port = readValidInt("Enter MySQL Port [default " + DatabaseConnection.getPort() + "]: ");

                System.out.print("Enter Database Name [default " + DatabaseConnection.getDatabase() + "]: ");
                String dbName = scanner.nextLine().trim();
                if (dbName.isEmpty()) dbName = DatabaseConnection.getDatabase();

                System.out.print("Enter MySQL Username [default " + DatabaseConnection.getUser() + "]: ");
                String user = scanner.nextLine().trim();
                if (user.isEmpty()) user = DatabaseConnection.getUser();

                System.out.print("Enter MySQL Password: ");
                String pass = scanner.nextLine().trim();

                DatabaseConnection.setMySQLConfig(host, port, dbName, user, pass);
                System.out.println("Testing connection to MySQL (" + host + ":" + port + ")...");
                try {
                    studentDAO.ensureTableExists();
                    System.out.println("[SUCCESS] Connected successfully to MySQL database '" + dbName + "'!");
                } catch (SQLException e) {
                    System.out.println("[FAILED] Could not connect to MySQL: " + e.getMessage());
                    System.out.println("[i] Reverting back to previous database mode.");
                    DatabaseConnection.setMode(DatabaseConnection.DbMode.AUTO);
                }
                break;

            case "2":
                DatabaseConnection.setMode(DatabaseConnection.DbMode.EMBEDDED);
                try {
                    studentDAO.ensureTableExists();
                    System.out.println("[SUCCESS] Switched to Embedded Local Database (H2).");
                } catch (SQLException e) {
                    System.out.println("[FAILED] Error initializing embedded database: " + e.getMessage());
                }
                break;

            case "3":
                break;

            default:
                System.out.println("[!] Invalid option.");
        }
    }

    // Helper methods
    private static double readValidMarks() {
        while (true) {
            System.out.print("Enter Marks (0 - 100): ");
            String input = scanner.nextLine().trim();
            try {
                double marks = Double.parseDouble(input);
                if (marks >= 0.0 && marks <= 100.0) {
                    return marks;
                }
                System.out.println("[!] Marks must be between 0 and 100.");
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter numeric marks.");
            }
        }
    }

    private static int readValidInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[!] Please enter a valid whole number.");
            }
        }
    }

    private static void printStudentTable(List<Student> students) {
        String line = "+------+----------------------+-------------------------------+----------------------+-------+-------+";
        System.out.println(line);
        System.out.printf("| %-4s | %-20s | %-29s | %-20s | %-5s | %-5s |\n",
                "ID", "Name", "Email", "Course", "Marks", "Grade");
        System.out.println(line);
        for (Student s : students) {
            System.out.printf("| %-4d | %-20s | %-29s | %-20s | %-5.1f | %-5s |\n",
                    s.getId(),
                    truncate(s.getName(), 20),
                    truncate(s.getEmail(), 29),
                    truncate(s.getCourse(), 20),
                    s.getMarks(),
                    s.getGrade());
        }
        System.out.println(line);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
