package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.climaxion.drivedock_be.util.PasswordUtil;
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

@WebServlet("/api/admin/login")
public class AdminLoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        // Input validation
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Username and password are required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Get admin from database
            String sql = "SELECT id, username, email, password, full_name, role, is_active FROM admin WHERE username = ? OR email = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Check if account is active
                        if (!rs.getBoolean("is_active")) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            out.print(gson.toJson(Collections.singletonMap("error", "Account is deactivated")));
                            return;
                        }

                        String hashedPassword = rs.getString("password");

                        // Verify password
                        if (PasswordUtil.verifyPassword(password, hashedPassword)) {
                            // Generate session token
                            String sessionToken = generateSessionToken();

                            // Store session in database
                            String insertSessionSql = "INSERT INTO admin_session (admin_id, session_token, ip_address, user_agent, expires_at) " +
                                    "VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL 8 HOUR))";
                            try (PreparedStatement psSession = con.prepareStatement(insertSessionSql)) {
                                psSession.setInt(1, rs.getInt("id"));
                                psSession.setString(2, sessionToken);
                                psSession.setString(3, request.getRemoteAddr());
                                psSession.setString(4, request.getHeader("User-Agent"));
                                psSession.executeUpdate();
                            }

                            // Update last login time
                            String updateLoginSql = "UPDATE admin SET last_login = NOW() WHERE id = ?";
                            try (PreparedStatement psUpdate = con.prepareStatement(updateLoginSql)) {
                                psUpdate.setInt(1, rs.getInt("id"));
                                psUpdate.executeUpdate();
                            }

                            // Create HTTP session
                            HttpSession session = request.getSession(true);
                            session.setAttribute("adminId", rs.getInt("id"));
                            session.setAttribute("adminUsername", rs.getString("username"));
                            session.setAttribute("adminRole", rs.getString("role"));
                            session.setAttribute("adminFullName", rs.getString("full_name"));
                            session.setMaxInactiveInterval(30 * 60); // 30 minutes

                            // Prepare response
                            Map<String, Object> responseData = new HashMap<>();
                            responseData.put("success", true);
                            responseData.put("message", "Login successful");
                            responseData.put("sessionToken", sessionToken);
                            responseData.put("admin", Map.of(
                                    "id", rs.getInt("id"),
                                    "username", rs.getString("username"),
                                    "fullName", rs.getString("full_name"),
                                    "role", rs.getString("role")
                            ));

                            out.print(gson.toJson(responseData));
                        } else {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            out.print(gson.toJson(Collections.singletonMap("error", "Invalid credentials")));
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print(gson.toJson(Collections.singletonMap("error", "Admin not found")));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Login error: " + e.getMessage())));
        }
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString() + System.currentTimeMillis();
    }
}
