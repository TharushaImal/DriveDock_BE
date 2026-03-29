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

@WebServlet("/api/admin/stats")
public class AdminStatsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Unauthorized. Please login.")));
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();


        try (Connection con = DBConnection.getConnection()) {
            // all users
            String userSql = "SELECT COUNT(*) AS total_users FROM user";
            int users = 0;
            try (Statement st = con.createStatement();
                 ResultSet rs1 = st.executeQuery(userSql)) {
                if (rs1.next()) {
                    users = rs1.getInt("total_users");
                }
            }

            // all reservations
            String resSql = "SELECT COUNT(*) AS total_res FROM reservation";
            int reservations = 0;
            try (Statement st = con.createStatement();
                 ResultSet rs2 = st.executeQuery(resSql)) {
                if (rs2.next()) {
                    reservations = rs2.getInt("total_res");
                }
            }

            Map<String, Integer> data = new HashMap<>();
            data.put("users", users);
            data.put("reservations", reservations);

            out.print(gson.toJson(data));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch stats: " + e.getMessage())));
        }
    }
}
