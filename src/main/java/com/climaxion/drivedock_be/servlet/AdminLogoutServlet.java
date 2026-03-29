package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
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

@WebServlet("/api/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        // Get session token from header
        String sessionToken = request.getHeader("X-Session-Token");

        // Invalidate database session
        if (sessionToken != null) {
            try (Connection con = DBConnection.getConnection()) {
                String sql = "DELETE FROM admin_session WHERE session_token = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, sessionToken);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Invalidate HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Map<String, String> responseData = new HashMap<>();
        responseData.put("success", "true");
        responseData.put("message", "Logged out successfully");
        out.print(gson.toJson(responseData));
    }
}
