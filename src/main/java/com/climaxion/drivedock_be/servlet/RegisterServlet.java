package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.climaxion.drivedock_be.util.PasswordUtil;
import com.climaxion.drivedock_be.util.ValidationUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fname = request.getParameter("fname");
        String lname = request.getParameter("lname");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone_number");
        String password = request.getParameter("password");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        Map<String, String> errors = new HashMap<>();

        if (fname == null || fname.trim().isEmpty()) {
            errors.put("fname", "First name is required");
        } else if (fname.length() > 50) {
            errors.put("fname", "First name cannot exceed 50 characters");
        }

        if (lname == null || lname.trim().isEmpty()) {
            errors.put("lname", "Last name is required");
        } else if (lname.length() > 50) {
            errors.put("lname", "Last name cannot exceed 50 characters");
        }

        if (!ValidationUtil.isValidEmail(email)) {
            errors.put("email", "Valid email address is required");
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            errors.put("phone", "Valid Sri Lankan phone number is required");
        }

        if (!ValidationUtil.isValidPassword(password)) {
            errors.put("password", "Password must be at least 8 characters and contain both letters and numbers");
        }

        if (!errors.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(errors));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Check if email already exists
            String checkSql = "SELECT id FROM user WHERE email = ?";
            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setString(1, email);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.print(gson.toJson(Collections.singletonMap("error", "Email already registered")));
                        return;
                    }
                }
            }

            // Hash password
            String hashedPassword = PasswordUtil.hashPassword(password);

            // Insert new user
            String sql = "INSERT INTO user (fname, lname, email, phone_number, password) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, ValidationUtil.sanitizeInput(fname));
                ps.setString(2, ValidationUtil.sanitizeInput(lname));
                ps.setString(3, email.toLowerCase()); // Store email in lowercase
                ps.setString(4, phone);
                ps.setString(5, hashedPassword);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    Map<String, String> success = new HashMap<>();
                    success.put("message", "User registered successfully");
                    out.print(gson.toJson(success));
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(gson.toJson(Collections.singletonMap("error", "Registration failed")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Server error: " + e.getMessage())));
        }
    }
}
