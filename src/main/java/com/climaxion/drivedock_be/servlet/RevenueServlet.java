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

@WebServlet("/api/admin/revenue")
public class RevenueServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT DATE(payment_date) as day, SUM(amount) as total " +
                    "FROM payment WHERE payment_status='SUCCESS' " +
                    "GROUP BY DATE(payment_date)";

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("day", rs.getString("day"));
                row.put("total", rs.getDouble("total"));
                data.add(row);
            }

            out.print(gson.toJson(data));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
