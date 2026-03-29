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

@WebServlet("/api/admin/services")
public class AdminServicesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        List<Map<String, Object>> services = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id, name, description, price FROM service";
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    Map<String, Object> service = new HashMap<>();
                    service.put("id", rs.getInt("id"));
                    service.put("name", rs.getString("name"));
                    service.put("description", rs.getString("description"));
                    service.put("price", rs.getDouble("price"));
                    services.add(service);
                }
            }
            out.print(gson.toJson(services));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch services: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        // Read parameters (can be from form-urlencoded or JSON)
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String priceStr = request.getParameter("price");
        // Optional: for update
        String idStr = request.getParameter("id");

        if (name == null || name.trim().isEmpty() || priceStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Name and price are required")));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Invalid price format")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql;
            PreparedStatement ps;

            if (idStr != null && !idStr.trim().isEmpty()) {
                // Update existing service
                sql = "UPDATE service SET name=?, description=?, price=? WHERE id=?";
                ps = con.prepareStatement(sql);
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDouble(3, price);
                ps.setInt(4, Integer.parseInt(idStr));
            } else {
                // Insert new service
                sql = "INSERT INTO service (name, description, price) VALUES (?, ?, ?)";
                ps = con.prepareStatement(sql);
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDouble(3, price);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                out.print(gson.toJson(Collections.singletonMap("message", "Service saved successfully")));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(gson.toJson(Collections.singletonMap("error", "Failed to save service")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String idStr = request.getParameter("id");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        if (idStr == null || idStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Service ID is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM service WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(idStr));
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    out.print(gson.toJson(Collections.singletonMap("message", "Service deleted successfully")));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(Collections.singletonMap("error", "Service not found")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to delete service: " + e.getMessage())));
        }

    }
}
