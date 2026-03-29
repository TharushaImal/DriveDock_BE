package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/admin/users")
public class UsersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        List<Map<String, Object>> users = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery("SELECT id, fname, lname, email, phone_number, created_at FROM user");

            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("name", rs.getString("fname") + " " + rs.getString("lname"));
                user.put("email", rs.getString("email"));
                user.put("phone", rs.getString("phone_number"));
                user.put("created_at", rs.getString("created_at"));

                users.add(user);
            }

            out.print(gson.toJson(users));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userId = request.getParameter("user_id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        if (userId == null || userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson("Error: User ID is missing"));
            return;
        }

        // Try-with-resources automatically closes Connection and PreparedStatement
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM user WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(userId));
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.print(gson.toJson("User deleted successfully"));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson("User not found"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson("Server error: " + e.getMessage()));
        } finally {
            out.close();
        }
    }
}
