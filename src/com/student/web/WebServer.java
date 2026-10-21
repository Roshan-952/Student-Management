package com.student.web;

import com.student.dao.DatabaseConnection;
import com.student.dao.StudentDAO;
import com.student.model.Student;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP REST & Static File Server.
 * Uses standard Java SE com.sun.net.httpserver (zero external dependencies).
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8080;
    private static final StudentDAO studentDAO = new StudentDAO();
    private static HttpServer server;
    private static int activePort = DEFAULT_PORT;

    public static int start(int port) throws IOException {
        int tryPort = port;
        while (tryPort < port + 10) {
            try {
                server = HttpServer.create(new InetSocketAddress(tryPort), 0);
                activePort = tryPort;
                break;
            } catch (IOException e) {
                tryPort++;
            }
        }
        if (server == null) {
            throw new IOException("Unable to bind to any port between " + port + " and " + (port + 10));
        }

        server.setExecutor(Executors.newCachedThreadPool());

        // API Endpoints
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/db-status", new DbStatusHandler());
        server.createContext("/api/db-config", new DbConfigHandler());

        // Static File Handler
        server.createContext("/", new StaticFileHandler());

        server.start();
        return activePort;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static int getActivePort() {
        return activePort;
    }

    // ==========================================
    // API Handlers
    // ==========================================

    private static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            String path = uri.getPath(); // e.g. /api/students or /api/students/12
            String[] segments = path.split("/");
            Integer id = null;
            if (segments.length >= 4 && !segments[3].isEmpty()) {
                try {
                    id = Integer.parseInt(segments[3]);
                } catch (NumberFormatException ignored) {}
            }

            try {
                switch (method) {
                    case "GET":
                        if (id != null) {
                            Student s = studentDAO.getStudentById(id);
                            if (s == null) {
                                sendJsonResponse(exchange, 404, "{\"error\":\"Student not found\"}");
                            } else {
                                sendJsonResponse(exchange, 200, studentToJson(s));
                            }
                        } else {
                            List<Student> list = studentDAO.getAllStudents();
                            sendJsonResponse(exchange, 200, studentListToJson(list));
                        }
                        break;

                    case "POST":
                        String body = readBody(exchange);
                        Map<String, String> data = parseSimpleJson(body);
                        String name = data.getOrDefault("name", "").trim();
                        String email = data.getOrDefault("email", "").trim();
                        String course = data.getOrDefault("course", "").trim();
                        double marks = parseDoubleSafe(data.get("marks"), 0.0);

                        if (name.isEmpty() || email.isEmpty() || course.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Name, email, and course are required\"}");
                            return;
                        }

                        Student newStudent = new Student(name, email, course, marks);
                        boolean added = studentDAO.addStudent(newStudent);
                        if (added) {
                            sendJsonResponse(exchange, 201, studentToJson(newStudent));
                        } else {
                            sendJsonResponse(exchange, 500, "{\"error\":\"Failed to save student\"}");
                        }
                        break;

                    case "PUT":
                        if (id == null) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Student ID is required for update\"}");
                            return;
                        }
                        Student existing = studentDAO.getStudentById(id);
                        if (existing == null) {
                            sendJsonResponse(exchange, 404, "{\"error\":\"Student not found\"}");
                            return;
                        }

                        String updateBody = readBody(exchange);
                        Map<String, String> updateData = parseSimpleJson(updateBody);
                        if (updateData.containsKey("name")) existing.setName(updateData.get("name").trim());
                        if (updateData.containsKey("email")) existing.setEmail(updateData.get("email").trim());
                        if (updateData.containsKey("course")) existing.setCourse(updateData.get("course").trim());
                        if (updateData.containsKey("marks")) {
                            existing.setMarks(parseDoubleSafe(updateData.get("marks"), existing.getMarks()));
                        }

                        boolean updated = studentDAO.updateStudent(existing);
                        if (updated) {
                            sendJsonResponse(exchange, 200, studentToJson(existing));
                        } else {
                            sendJsonResponse(exchange, 500, "{\"error\":\"Failed to update student\"}");
                        }
                        break;

                    case "DELETE":
                        if (id == null) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Student ID required for delete\"}");
                            return;
                        }
                        boolean deleted = studentDAO.deleteStudent(id);
                        if (deleted) {
                            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Student deleted\"}");
                        } else {
                            sendJsonResponse(exchange, 404, "{\"error\":\"Student not found or already deleted\"}");
                        }
                        break;

                    default:
                        exchange.sendResponseHeaders(405, -1);
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                List<Student> students = studentDAO.getAllStudents();
                int total = students.size();
                double totalMarks = 0;
                Map<String, Integer> gradeCounts = new LinkedHashMap<>();
                gradeCounts.put("A+", 0);
                gradeCounts.put("A", 0);
                gradeCounts.put("B", 0);
                gradeCounts.put("C", 0);
                gradeCounts.put("D", 0);
                gradeCounts.put("F", 0);

                double highestMarks = -1;
                String topStudentName = "N/A";

                for (Student s : students) {
                    totalMarks += s.getMarks();
                    String g = s.getGrade();
                    gradeCounts.put(g, gradeCounts.getOrDefault(g, 0) + 1);
                    if (s.getMarks() > highestMarks) {
                        highestMarks = s.getMarks();
                        topStudentName = s.getName();
                    }
                }

                double avg = total > 0 ? (totalMarks / total) : 0.0;
                String avgGrade = total > 0 ? Student.calculateGrade(avg) : "-";

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"total\":").append(total).append(",");
                sb.append(String.format(Locale.US, "\"averageMarks\":%.2f,", avg));
                sb.append("\"averageGrade\":\"").append(avgGrade).append("\",");
                sb.append(String.format(Locale.US, "\"highestMarks\":%.2f,", Math.max(0, highestMarks)));
                sb.append("\"topStudent\":\"").append(escapeJson(topStudentName)).append("\",");
                sb.append("\"gradeDistribution\":{");
                int idx = 0;
                for (Map.Entry<String, Integer> e : gradeCounts.entrySet()) {
                    if (idx++ > 0) sb.append(",");
                    sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
                }
                sb.append("}}");

                sendJsonResponse(exchange, 200, sb.toString());
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static class DbStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String desc = DatabaseConnection.getActiveModeDescription();
            String activeType = DatabaseConnection.getActiveMode().name();
            boolean reachable = DatabaseConnection.testConnection();

            String json = String.format("{\"mode\":\"%s\",\"description\":\"%s\",\"reachable\":%b,\"host\":\"%s\",\"port\":%d,\"database\":\"%s\"}",
                    escapeJson(activeType),
                    escapeJson(desc),
                    reachable,
                    escapeJson(DatabaseConnection.getHost()),
                    DatabaseConnection.getPort(),
                    escapeJson(DatabaseConnection.getDatabase())
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    private static class DbConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            String mode = data.getOrDefault("mode", "").toUpperCase();

            try {
                if ("EMBEDDED".equals(mode)) {
                    DatabaseConnection.setMode(DatabaseConnection.DbMode.EMBEDDED);
                    studentDAO.ensureTableExists();
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Switched to Embedded Database (H2)\"}");
                } else if ("MYSQL".equals(mode)) {
                    String host = data.getOrDefault("host", DatabaseConnection.getHost()).trim();
                    int port = (int) parseDoubleSafe(data.get("port"), DatabaseConnection.getPort());
                    String db = data.getOrDefault("database", DatabaseConnection.getDatabase()).trim();
                    String user = data.getOrDefault("user", DatabaseConnection.getUser()).trim();
                    String pass = data.getOrDefault("password", "");

                    DatabaseConnection.setMySQLConfig(host, port, db, user, pass);
                    studentDAO.ensureTableExists();
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Connected to MySQL successfully\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Unknown mode. Use EMBEDDED or MYSQL\"}");
                }
            } catch (SQLException e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"Connection failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ==========================================
    // Static Files Handler
    // ==========================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Normalize path relative to project's web/ directory
            Path filePath = Paths.get("web", path.substring(1)).normalize();
            if (!filePath.startsWith(Paths.get("web"))) {
                // Prevent directory traversal
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            File file = filePath.toFile();
            if (!file.exists() || file.isDirectory()) {
                // If requesting html without extension or fallback to index.html
                File fallback = new File("web/index.html");
                if (fallback.exists()) {
                    file = fallback;
                } else {
                    byte[] notFound = "<h1>404 Not Found</h1>".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(404, notFound.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound);
                    }
                    return;
                }
            }

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private static String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = is.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    public static String studentToJson(Student s) {
        return String.format(Locale.US,
                "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"course\":\"%s\",\"marks\":%.2f,\"grade\":\"%s\"}",
                s.getId(),
                escapeJson(s.getName()),
                escapeJson(s.getEmail()),
                escapeJson(s.getCourse()),
                s.getMarks(),
                escapeJson(s.getGrade())
        );
    }

    public static String studentListToJson(List<Student> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(studentToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static double parseDoubleSafe(String val, double defaultVal) {
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /**
     * Minimal zero-dependency JSON string parser for flat JSON objects.
     */
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        // Tokenize by comma outside quotes
        List<String> pairs = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                String key = cleanJsonToken(pair.substring(0, colonIdx));
                String val = cleanJsonToken(pair.substring(colonIdx + 1));
                map.put(key, val);
            }
        }
        return map;
    }

    private static String cleanJsonToken(String token) {
        String t = token.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            t = t.substring(1, t.length() - 1);
        }
        return t.replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }
}
