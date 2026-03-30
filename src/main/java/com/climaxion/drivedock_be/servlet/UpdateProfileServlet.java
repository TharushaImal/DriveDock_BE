package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.climaxion.drivedock_be.util.ValidationUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.*;
import java.util.*;


@WebServlet("/api/user/update")
public class UpdateProfileServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String userId  = request.getParameter("user_id");
        String fname   = request.getParameter("fname");
        String lname   = request.getParameter("lname");
        String email   = request.getParameter("email");
        String phone   = request.getParameter("phone_number");

        if (userId == null || userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "User ID is required")));
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Valid email is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Check email not taken by another user
            String checkSql = "SELECT id FROM user WHERE email = ? AND id != ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, email.toLowerCase());
                ps.setInt(2, Integer.parseInt(userId));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        out.print(gson.toJson(Collections.singletonMap("error", "Email already in use")));
                        return;
                    }
                }
            }

            String sql = "UPDATE user SET fname=?, lname=?, email=?, phone_number=? WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fname);
                ps.setString(2, lname);
                ps.setString(3, email.toLowerCase());
                ps.setString(4, phone);
                ps.setInt(5, Integer.parseInt(userId));

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    out.print(gson.toJson(Collections.singletonMap("message", "Profile updated successfully")));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(Collections.singletonMap("error", "User not found")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Server error: " + e.getMessage())));
        }
    }
}
