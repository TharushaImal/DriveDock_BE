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

@WebServlet("/api/user/fcm-token")
public class SaveFcmTokenServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String userId = request.getParameter("user_id");
        String token = request.getParameter("fcm_token");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        if (userId == null || token == null) {
            response.setStatus(400);
            out.print(gson.toJson(Collections.singletonMap("error", "Missing parameters")));
            return;
        }

        // You need to add an fcm_token column to your user table first:
        // ALTER TABLE user ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL;

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE user SET fcm_token = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, token);
                ps.setInt(2, Integer.parseInt(userId));
                ps.executeUpdate();
                out.print(gson.toJson(Collections.singletonMap("message", "Token saved")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print(gson.toJson(Collections.singletonMap("error", e.getMessage())));
        }
    }
}
