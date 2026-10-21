package com.student.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC Database Connection.
 * Supports MySQL with automatic/graceful fallback to an Embedded Database (H2),
 * allowing the application to work out-of-the-box without requiring an external MySQL server.
 */
public class DatabaseConnection {

    public enum DbMode {
        AUTO,
        MYSQL,
        EMBEDDED
    }

    private static DbMode currentMode = DbMode.AUTO;
    private static DbMode activeMode = DbMode.AUTO;

    // MySQL configuration
    private static String host = "localhost";
    private static int port = 3306;
    private static String database = "student_db";
    private static String user = "root";
    private static String password = "";

    // Embedded H2 configuration
    private static final String H2_URL = "jdbc:h2:./student_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    private static boolean fallbackNoticePrinted = false;

    static {
        if (System.getProperty("db.host") != null) host = System.getProperty("db.host");
        if (System.getProperty("db.port") != null) port = Integer.parseInt(System.getProperty("db.port"));
        if (System.getProperty("db.database") != null) database = System.getProperty("db.database");
        if (System.getProperty("db.user") != null) user = System.getProperty("db.user");
        if (System.getProperty("db.password") != null) password = System.getProperty("db.password");
        if ("embedded".equalsIgnoreCase(System.getProperty("db.mode"))) currentMode = DbMode.EMBEDDED;
    }

    public static void setMySQLConfig(String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        host = dbHost;
        port = dbPort;
        database = dbName;
        user = dbUser;
        password = dbPassword;
        currentMode = DbMode.MYSQL;
    }

    public static void setCredentials(String dbUser, String dbPassword) {
        user = dbUser;
        password = dbPassword;
        currentMode = DbMode.MYSQL;
    }

    public static void setMode(DbMode mode) {
        currentMode = mode;
        if (mode == DbMode.EMBEDDED) {
            activeMode = DbMode.EMBEDDED;
        }
    }

    public static DbMode getActiveMode() {
        return activeMode;
    }

    public static String getActiveModeDescription() {
        if (activeMode == DbMode.EMBEDDED) {
            return "Embedded Local Database (H2 file: ./student_db.mv.db)";
        } else if (activeMode == DbMode.MYSQL) {
            return "MySQL Server (" + host + ":" + port + "/" + database + ")";
        } else {
            return "Auto-Detect (MySQL -> Embedded fallback)";
        }
    }

    public static String getMysqlUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /**
     * Establishes and returns a connection.
     * If AUTO or MYSQL fails to connect, automatically falls back to Embedded H2 database.
     */
    public static Connection getConnection() throws SQLException {
        if (currentMode == DbMode.EMBEDDED) {
            activeMode = DbMode.EMBEDDED;
            return getH2Connection();
        }

        // Try MySQL first
        try {
            Connection mysqlConn = getMysqlConnection();
            activeMode = DbMode.MYSQL;
            return mysqlConn;
        } catch (SQLException e) {
            if (currentMode == DbMode.MYSQL) {
                // If user explicitly forced MYSQL mode, throw the exception
                throw e;
            }

            // In AUTO mode: fall back to embedded database
            if (!fallbackNoticePrinted) {
                System.out.println("\n[i] Note: MySQL server is not reachable on " + host + ":" + port + ".");
                System.out.println("[i] Switching seamlessly to Embedded Local Database (H2).");
                System.out.println("[i] Your data is persistently stored in local file './student_db.mv.db'.");
                fallbackNoticePrinted = true;
            }

            activeMode = DbMode.EMBEDDED;
            return getH2Connection();
        }
    }

    private static Connection getMysqlConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in classpath.", e);
        }
        return DriverManager.getConnection(getMysqlUrl(), user, password);
    }

    private static Connection getH2Connection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 JDBC Driver not found in classpath. Ensure lib/h2-*.jar is loaded.", e);
        }
        return DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getUser() {
        return user;
    }

    public static String getDatabase() {
        return database;
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }
}
