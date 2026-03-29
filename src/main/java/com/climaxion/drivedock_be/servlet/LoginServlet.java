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

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        if (email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Email and password are required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id, fname, lname, email, password FROM user WHERE email = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email.toLowerCase());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");

                        if (PasswordUtil.verifyPassword(password, hashedPassword)) {
                            // Create session
                            HttpSession session = request.getSession(true);
                            session.setAttribute("userId", rs.getInt("id"));
                            session.setAttribute("userName", rs.getString("fname") + " " + rs.getString("lname"));
                            session.setAttribute("userEmail", rs.getString("email"));
                            session.setMaxInactiveInterval(24 * 60 * 60); // 24 hours

                            Map<String, Object> responseData = new HashMap<>();
                            responseData.put("success", true);
                            responseData.put("message", "Login successful");
                            responseData.put("userId", rs.getInt("id"));
                            responseData.put("userName", rs.getString("fname") + " " + rs.getString("lname"));

                            out.print(gson.toJson(responseData));
                        } else {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            out.print(gson.toJson(Collections.singletonMap("error", "Invalid credentials")));
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        out.print(gson.toJson(Collections.singletonMap("error", "User not found")));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Login error: " + e.getMessage())));
        }
    }
}
