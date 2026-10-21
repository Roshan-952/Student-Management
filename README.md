# 🎓 Student Management System

A beginner-friendly console application built using **Core Java**, **JDBC (Java Database Connectivity)**, and **MySQL**.

---

## 📌 Features

1. **Add Student Records**: Captures name, email, course, and marks, automatically calculates letter grades and stores the record in MySQL.
2. **Display All Students**: Formats all registered students in a clean ASCII table with summary statistics (average score, overall grade).
3. **Search by Student ID**: Quickly lookup a student record using their unique ID.
4. **Update Student Details**: Interactively edit a student's name, email, course, or marks (grades are automatically recalculated).
5. **Delete Student Records**: Safely remove students with a confirmation prompt.
6. **Calculate Grades**: Automated grade grading scale:
   - `90% - 100%`: **A+**
   - `80% - 89%`: **A**
   - `70% - 79%`: **B**
   - `60% - 69%`: **C**
   - `50% - 59%`: **D**
   - `Below 50%`: **F**
7. **Modern Web UI Dashboard (EduTrack Pro)**: High-performance, responsive web frontend with glassmorphism, dark/light theme switch, real-time search, interactive grade slider calculator, and CSV export.
8. **Embedded REST API**: Lightweight zero-dependency HTTP server built with standard Java SE `com.sun.net.httpserver`.
9. **Dual-Database Support & Zero-Setup Mode**: Runs right out of the box using an embedded database (`H2`), with optional full connectivity to **MySQL/XAMPP**.
10. **Interactive Database Settings**: Easily switch between MySQL and Embedded mode or change host/credentials directly from the web UI or console menu.

---

## 📁 Project Structure

```text
student-management-system/
│
├── lib/
│   ├── h2-2.3.232.jar               # Embedded Database Driver (Zero-setup)
│   └── mysql-connector-j-8.4.0.jar  # JDBC MySQL Driver
│
├── src/
│   └── com/
│       └── student/
│           ├── model/
│           │   └── Student.java             # OOP Model (Encapsulation, Grade logic)
│           ├── dao/
│           │   ├── DatabaseConnection.java  # Dual JDBC Connection Manager (MySQL + H2)
│           │   └── StudentDAO.java          # CRUD Queries via PreparedStatement
│           ├── web/
│           │   └── WebServer.java           # Embedded HTTP REST & Static File Server
│           └── Main.java                    # Dual Console & Web UI Entry Point
│
├── web/                                     # Modern Web UI Frontend
│   ├── index.html                           # Semantic HTML5 Application Dashboard
│   ├── style.css                            # Glassmorphism Design System & Theme CSS
│   └── app.js                               # Reactive UI State, CRUD, & Live Grade Calc
│
├── schema.sql                               # MySQL database and table schema
├── run.bat                                  # 1-click Windows CMD launcher
├── run.ps1                                  # Windows PowerShell launcher
└── README.md
```

---

## 🚀 How to Set Up & Run

### 1. Database (Zero Configuration Required!)
- **Default (Embedded Mode)**: If MySQL is not running on your computer, the application automatically runs using an embedded persistent database (`student_db.mv.db`). No installation or server startup required!
- **MySQL / XAMPP Mode (Optional)**: If you start MySQL or XAMPP on port `3306`, you can switch to it anytime via Option 6 in the main menu or it connects automatically when present.

### 2. Run the Application

#### Option A: Double-Click
Double-click `run.bat` in Windows File Explorer.

#### Option B: Terminal / Command Prompt
```bash
cd "C:\Users\ELCOT\.gemini\antigravity-ide\scratch\student-management-system"
run.bat
```

#### Option C: PowerShell
```powershell
cd "C:\Users\ELCOT\.gemini\antigravity-ide\scratch\student-management-system"
.\run.ps1
```

---

## 🧠 Key Java Concepts Demonstrated

- **Classes & Objects**: `Student` represents a real-world entity with state (fields) and behavior (methods).
- **Encapsulation**: Private fields protected by public getters and setters.
- **Methods**: `calculateGrade(double marks)` encapsulating grading rules.
- **JDBC Driver & Connection**: Using `DriverManager.getConnection(...)` to communicate with MySQL.
- **PreparedStatements**: Parameterized queries `?` to safely execute SQL and prevent SQL injection.
- **Resource Management**: Using `try-with-resources` to prevent database connection leaks.
