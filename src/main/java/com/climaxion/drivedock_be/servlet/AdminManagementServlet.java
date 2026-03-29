package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.climaxion.drivedock_be.util.PasswordUtil;
import com.climaxion.drivedock_be.util.ValidationUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/admin/admins")
public class AdminManagementServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"SUPER_ADMIN".equals(session.getAttribute("adminRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Only Super Admin can manage administrators")));
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id, username, email, full_name, role, is_active, last_login, created_at FROM admin ORDER BY id";
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                List<Map<String, Object>> admins = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> admin = new HashMap<>();
                    admin.put("id", rs.getInt("id"));
                    admin.put("username", rs.getString("username"));
                    admin.put("email", rs.getString("email"));
                    admin.put("fullName", rs.getString("full_name"));
                    admin.put("role", rs.getString("role"));
                    admin.put("isActive", rs.getBoolean("is_active"));
                    admin.put("lastLogin", rs.getTimestamp("last_login"));
                    admin.put("createdAt", rs.getTimestamp("created_at"));
                    admins.add(admin);
                }
                out.print(gson.toJson(admins));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch admins: " + e.getMessage())));
        }

    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"SUPER_ADMIN".equals(session.getAttribute("adminRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Only Super Admin can create administrators")));
            return;
        }

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");
        String isActive = request.getParameter("isActive");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Validate inputs
        Map<String, String> errors = new HashMap<>();

        if (!ValidationUtil.isValidUsername(username)) {
            errors.put("username", "Username must be 3-50 characters (letters, numbers, underscore only)");
        }

        if (!ValidationUtil.isValidEmail(email)) {
            errors.put("email", "Valid email address is required");
        }

        if (!ValidationUtil.isValidPassword(password)) {
            errors.put("password", "Password must be at least 8 characters with at least one letter and one number");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            errors.put("fullName", "Full name is required");
        }

        if (role == null || (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"VIEWER".equals(role))) {
            errors.put("role", "Valid role is required (SUPER_ADMIN, ADMIN, VIEWER)");
        }

        if (!errors.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(errors));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Check if username or email already exists
            String checkSql = "SELECT id FROM admin WHERE username = ? OR email = ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, username);
                ps.setString(2, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.print(gson.toJson(Collections.singletonMap("error", "Username or email already exists")));
                        return;
                    }
                }
            }

            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Insert new admin
            String insertSql = "INSERT INTO admin (username, email, password, full_name, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, email.toLowerCase());
                ps.setString(3, hashedPassword);
                ps.setString(4, fullName);
                ps.setString(5, role);
                ps.setBoolean(6, isActive == null || "true".equals(isActive));

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    Map<String, String> success = new HashMap<>();
                    success.put("message", "Admin created successfully");
                    out.print(gson.toJson(success));
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(gson.toJson(Collections.singletonMap("error", "Failed to create admin")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }

    }


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"SUPER_ADMIN".equals(session.getAttribute("adminRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Only Super Admin can update administrators")));
            return;
        }

        String id = request.getParameter("id");
        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");
        String isActive = request.getParameter("isActive");
        String password = request.getParameter("password"); // Optional

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (id == null || id.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Admin ID is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            StringBuilder updateSql = new StringBuilder("UPDATE admin SET ");
            List<Object> params = new ArrayList<>();

            if (fullName != null && !fullName.trim().isEmpty()) {
                updateSql.append("full_name = ?, ");
                params.add(fullName);
            }

            if (role != null && (role.equals("SUPER_ADMIN") || role.equals("ADMIN") || role.equals("VIEWER"))) {
                updateSql.append("role = ?, ");
                params.add(role);
            }

            if (isActive != null) {
                updateSql.append("is_active = ?, ");
                params.add(Boolean.parseBoolean(isActive));
            }

            if (password != null && !password.trim().isEmpty()) {
                if (!ValidationUtil.isValidPassword(password)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(Collections.singletonMap("error", "Password must be at least 8 characters with at least one letter and one number")));
                    return;
                }
                String hashedPassword = PasswordUtil.hashPassword(password);
                updateSql.append("password = ?, ");
                params.add(hashedPassword);
            }

            if (params.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(Collections.singletonMap("error", "No fields to update")));
                return;
            }

            updateSql.append("updated_at = NOW() WHERE id = ?");
            params.add(Integer.parseInt(id));

            try (PreparedStatement ps = con.prepareStatement(updateSql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    Map<String, String> success = new HashMap<>();
                    success.put("message", "Admin updated successfully");
                    out.print(gson.toJson(success));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(Collections.singletonMap("error", "Admin not found")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }

    }


    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"SUPER_ADMIN".equals(session.getAttribute("adminRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Only Super Admin can delete administrators")));
            return;
        }

        String id = request.getParameter("id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (id == null || id.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Admin ID is required")));
            return;
        }

        // Prevent deleting yourself
        Integer currentAdminId = (Integer) session.getAttribute("adminId");
        if (currentAdminId != null && currentAdminId == Integer.parseInt(id)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "You cannot delete your own account")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // First, delete all sessions for this admin
            String deleteSessionsSql = "DELETE FROM admin_session WHERE admin_id = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteSessionsSql)) {
                ps.setInt(1, Integer.parseInt(id));
                ps.executeUpdate();
            }

            // Then delete the admin
            String deleteSql = "DELETE FROM admin WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                ps.setInt(1, Integer.parseInt(id));
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    Map<String, String> success = new HashMap<>();
                    success.put("message", "Admin deleted successfully");
                    out.print(gson.toJson(success));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(Collections.singletonMap("error", "Admin not found")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }

    }
}
